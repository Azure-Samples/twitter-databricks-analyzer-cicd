
set -o errexit
set -o pipefail
set -o nounset
# set -o xtrace

# Set path
parent_path=$( cd "$(dirname "${BASH_SOURCE[0]}")" ; pwd -P )
cd "$parent_path"

# Constants
RED='\033[0;31m'
ORANGE='\033[0;33m'
NC='\033[0m'

wait_for_terminate () {
    # See here: https://docs.azuredatabricks.net/api/latest/jobs.html#jobsrunresultstate
    declare cluster_id=$1
    while : ; do
        result_state=$(databricks clusters get --cluster-id $cluster_id | jq -r ".state") 
        if [[ $result_state == "TERMINATED" ]]; then
            echo -e "Job ${cluster_id} has terminated successfully"
            break;
        else 
            echo "Waiting for cluster ${cluster_id} to terminate..."
            sleep 2m
        fi
    done
}

_main() {
  cancelledJobs=()

  echo "Going over jobs to terminate..."
  for filePath in $(ls -v $PWD/config/run.*.config.json); do

        if [[ -z $filePath || "$filePath" == " " ]]; then
            echo "No files found to run"
            continue
        fi

        filename=$(basename "$filePath")
        type="$(cut -d'.' -f1 <<<"$filename")"
        serial="$(cut -d'.' -f2 <<<"$filename")"
        runType="$(cut -d'.' -f3 <<<"$filename")"

        for jn in "$(cat "$filePath" | jq -r ".run_name")"; do

            # Search for active running jobs and stop them
            declare runids=$(databricks runs list --active-only --output JSON | jq -c ".runs // []" | jq -c "[.[] | select(.run_name == \"$jn\")]" | jq .[].run_id)
            for id in $runids; do
              echo "Stopping job id $id..."
              databricks runs cancel --run-id $id
              cancelledJobs+=($id)
            done
        done
    done

    echo "Waiting for job runs to terminate..."
    for jobId in ${cancelledJobs[@]}; do
        wait_for_terminate $jobId
    done
}

_main