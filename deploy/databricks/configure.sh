#!/bin/bash

# Access granted under MIT Open Source License: https://en.wikipedia.org/wiki/MIT_License
#
# Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated 
# documentation files (the "Software"), to deal in the Software without restriction, including without limitation 
# the rights to use, copy, modify, merge, publish, distribute, sublicense, # and/or sell copies of the Software, 
# and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in all copies or substantial portions 
# of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED 
# TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL 
# THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
# CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER 
# DEALINGS IN THE SOFTWARE.
#
#
# Description: Deploy Databricks cluster
#
# Usage: 
#
# Requirments:  
#

set -o errexit
set -o pipefail
set -o nounset
# set -o xtrace

# Set path
parent_path=$( cd "$(dirname "${BASH_SOURCE[0]}")" ; pwd -P )
cd "$parent_path"
declare run_test=${1:-""}

# Constants
RED='\033[0;31m'
ORANGE='\033[0;33m'
NC='\033[0m'

wait_for_run () {
    # See here: https://docs.azuredatabricks.net/api/latest/jobs.html#jobsrunresultstate
    declare mount_run_id=$1
    declare wait_for_state=${2:-""}

    if [[ -z $wait_for_state ]]; then
        printf "Waiting for job $mount_run_id to complete..."
    else 
        printf "Waiting for job $mount_run_id to have status <$wait_for_state>..."
    fi

    while : ; do
        life_cycle_status=$(databricks runs get --run-id $mount_run_id | jq -r ".state.life_cycle_state")
        result_state=$(databricks runs get --run-id $mount_run_id | jq -r ".state.result_state")
        if [[ $result_state == "SUCCESS" || $result_state == "SKIPPED" ]]; then
            echo ""
            break;
        elif [[ $life_cycle_status == "INTERNAL_ERROR" || $result_state == "FAILED" ]]; then
            echo ""
            echo -e "${RED} error running job, details ahead"
            run_status=$(databricks runs get --run-id $mount_run_id)
            err_msg=$(echo $run_status | jq -r ".state.state_message")
            echo -e "${RED}Error while running ${mount_run_id}: ${err_msg} ${NC}"
            exit 1
        elif [[ $wait_for_state && $life_cycle_status == $wait_for_state ]]; then
            echo ""
            echo "The job ${mount_run_id} has reached status ${wait_for_state}"
            break;
        else 
            printf "."
            sleep 30s
        fi
    done
}

wait_for_start () {
    # See here: https://docs.azuredatabricks.net/api/latest/jobs.html#jobsrunresultstate
    declare cluster_id=$1
    while : ; do
        result_state=$(databricks clusters get --cluster-id $cluster_id | jq -r ".state") 
        if [[ $result_state == "RUNNING" ]]; then
            break;
        elif [[ $result_state == "TERMINATED" ]]; then
            echo -e "${RED}Error while starting ${cluster_id} ${NC}"
            exit 1
        else 
            echo "Waiting for cluster ${cluster_id} to start..."
            sleep 2m
        fi
    done
}

cluster_exists () {
    declare cluster_name="$1"
    declare cluster=$(databricks clusters list | tr -s " " | cut -d" " -f2 | grep ^${cluster_name}$)
    if [[ -n $cluster ]]; then
        return 0; # cluster exists
    else
        return 1; # cluster does not exists
    fi
}

yes_or_no () {
    while true; do
        read -p "$(echo -e ${ORANGE}"$* [y/n]: "${NC})" yn
        case $yn in
            [Yy]*) return 0  ;;
            [Nn]*) echo -e "${RED}Aborted${NC}" ; return  1 ;;
        esac
    done
}

_main() {
    declare run_test=${1:-""}
    if [[ -z $run_test ]]; then
        echo -e "${ORANGE}"
        echo -e "!! -- WARNING --!!"
        echo -e "If this is the second time you are running this, this will re-upload and overwrite existing notebooks with the same names in the 'notebooks' folder. "
        echo -e "This will also drop and reload data in rating and recommendation Tables."
        echo -e "${NC}"
        yes_or_no "Are you sure you want to continue (Y/N)?" || { exit 1; }  
    else
        echo "Running configuration in Test environment"
    fi

    # Check if databricks is configured and populate the configuration file if not
    # This file also loads the environment variables (from an .env file)
    bash ./configure-cli-auth.sh

    # Create initial cluster, if not yet exists
    # TODO: Currently should be removed because jobs creates cluster - or remove from jobs
    cluster_config="./config/cluster.config.json"
    cluster_name=$(cat $cluster_config | jq -r ".cluster_name")
    if cluster_exists $cluster_name; then
        echo "Cluster ${cluster_name} already exists!"
    else
        echo "Creating cluster ${cluster_name}..."
        databricks clusters create --json-file $cluster_config
    fi

    declare cluster_id=$(databricks clusters list | awk '/'$cluster_name'/ {print $1}')
    declare cluster_state=$(databricks clusters get --cluster-id ${cluster_id} | jq -r ".state")
    if [[ $cluster_state == "TERMINATED" ]]; then
        databricks clusters start --cluster-id $cluster_id
        wait_for_start $cluster_id
    fi

    # Attach library
    bash ./deploy-libraries.sh $cluster_id
    # TODO: python library
    # TODO: generalize to dependency file

    # Upload artifact to dbfs
    echo "Uploading artifacts..."
    blob_file_name="social-source-wrapper-1.0-SNAPSHOT.jar"
    blob_local_path="../../src/social-source-wrapper/target/$blob_file_name"
    blob_jars_path="dbfs:/mnt/jars"
    blob_dbfs_path="$blob_jars_path/$blob_file_name"

    echo "Ensuring directory $blob_jars_path"
    databricks fs mkdirs "$blob_jars_path"
    echo "Uploading [$blob_local_path] to [$blob_jars_path]"
    databricks fs cp --overwrite "$blob_local_path" "$blob_dbfs_path"
    echo "Installing library [$blob_dbfs_path]"
    databricks libraries install --cluster-id $cluster_id --jar "$blob_dbfs_path"

    # Upload notebooks and dashboards
    echo "Uploading notebooks..."
    databricks workspace import_dir "../../notebooks" "/notebooks" --overwrite

    continuousJobs=()

    # For each job in the config folder, stop any existing running jobs and start the new job
    echo "Going over jobs to execute..."
    # TODO: check if assured order if not add sort
    #for f in ./config/run.*.config.json; do
    for filePath in $(ls -v $PWD/config/run.*.config.json); do

        if [[ -z $filePath || "$filePath" == " " ]]; then
            echo "No files found to run"
            continue
        fi

        filename=$(basename "$filePath")
        type="$(cut -d'.' -f1 <<<"$filename")"
        serial="$(cut -d'.' -f2 <<<"$filename")"
        runType="$(cut -d'.' -f3 <<<"$filename")"

        # Naming check
        if [[ $runType != 'continuous' && $runType != 'once' ]]; then
            echo "$filename is not in the format of run.<serial>.<continuous|once>.<name>.config.json"
            continue
        fi

        for jn in "$(cat "$filePath" | jq -r ".run_name")"; do

            # Search for active running jobs and stop them
            declare runids=$(databricks runs list --active-only --output JSON | jq -c ".runs // []" | jq -c "[.[] | select(.run_name == \"$jn\")]" | jq .[].run_id)
            for id in $runids; do
                echo "Stopping job id $id..."
                databricks runs cancel --run-id $id
            done

            declare jobjson=$(cat "$filePath")
            if [[ ! -z $run_test && $run_test == "true" ]]; then
                # TODO: socialSource is specific to this project, this parameter should change to test
                # Adding this paramter to the notebooks, enables each notebook/environment to understand 
                # its running in a test environment and execute accordingly
                jobjson=$(echo "$jobjson" | jq '.notebook_task.base_parameters |= { "socialSource": "CUSTOM" }')
            fi

            # Descern if the next execution should be continuous or a one time execution and execute accordingly
            if [[ $runType == 'continuous' ]]; then
                echo "Running job $jn..."
                continuousJobs+=($(databricks runs submit --json "$jobjson" | jq -r ".run_id" ))
            else 
                echo "Running job $jn and waiting for completion..."
                wait_for_run $(databricks runs submit --json "$jobjson" | jq -r ".run_id" )
            fi
        done
    done

    echo "Waiting for job runs to run..."
    for jobId in ${continuousJobs[@]}; do
        wait_for_run $jobId "RUNNING"
    done
}

_main $run_test

# Use a new name and the token you created manually: 
# databricks configure --token