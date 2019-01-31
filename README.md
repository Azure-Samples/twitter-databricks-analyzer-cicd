# Social Posts Pipeline

The following is a Social Post Sentiment processing pipeline implemented within [Azure Databricks](https://azure.microsoft.com/en-au/services/databricks/). 
This repo contains a generalized solution for running a social post processor on Azure Data Bricks.

For the description behind the architecture and investigation behind this solution, follow the [Code Story](https://www.microsoft.com/developerblog/2018/12/12/databricks-ci-cd-pipeline-using-travis/).

The Data Pipeline consists of:

- Ingesting tweets from Twitter
- Enriching tweets with *Language* and *Associated Entities*
- Identifying recent trends (last 15 minutes)
- Identifying long term trends (over the span of a week or a month)
- Saving historical data in an SQL database
- Sending an email (or triggering an Azure Function event) on new alerts

This repo also integrates a **CI/CD Pipeline** as part of the generalized solution with an e-2-e testing.
The CI/CD Pipeline consists of:

- TravisCI based process(See [.travis.yml](.travis.yml))
- A Build Status Tag (To see if the last build/PR is successful or faulty)
- Building of artifacts
- Deploying notebooks and artifacts into Azure Databricks test environment (using databricks-cli)
- Executing the pipeline on the test environment
- Observing the generated alerts to determine success/fail
- Cleanup solution

## Data Pipeline Architecture

![Pipelin Architecture](/docs/ci-cd-pipeline-cloud-architecture.png)

## CI/CD Pipeline Architecture

![CI/CD Pipeline Architecture](/docs/ci-cd-pipeline-ci-cd-diagram.png)

# Deployment

Ensure you are in the root of the repository and logged in to the Azure cli by running `az login`.

## Requirements

- Scripts and installations must be run from **bash** (wether on Windows or other platforms)
- [Azure CLI 2.0](https://azure.github.io/projects/clis/)
- [Maven 3.0](https://maven.apache.org/download.cgi) or higher
- [Python virtualenv](http://docs.python-guide.org/en/latest/dev/virtualenvs/) 
- [jq tool](https://stedolan.github.io/jq/download/)
- Check the requirements.txt for list of necessary Python packages. (will be installed by `make requirements`)

## Deployment Machine

The deployment is done using [Python Virtual Environment](https://docs.python-guide.org/dev/virtualenvs/).

- The following works with [Windows Subsystem for Linux](https://docs.microsoft.com/en-us/windows/wsl/install-win10)
- `virtualenv .`  This creates a python virtual environment to work in.
- `source bin/activate`  This activates the virtual environment.
- TODO: Add _ext.env
- `make requirements`. This installs python dependencies in the virtual environment.
- WARNING: The line endings of the two shell scripts `deploy.sh` and `databricks/configure.sh` may cause errors in your interpreter. You can change the line endings by opening the files in VS Code, and changing in the bottom right of the editor.

## Deploy Entire Solution

- Make sure to create the following file `databricks.env` in the root of the project:

```
# ------ Constant environment variables to update Databricks -----------
DBENV_SQL_TABLE_NAME=ItemHistory
DBENV_SQL_JDBC_PORT=1433
DBENV_TWITTER_CONSUMER_KEY={FROM_TWITTER}
DBENV_TWITTER_CONSUMER_SECRET={FROM_TWITTER}
DBENV_TWITTER_OAUTH_ACCESS_TOKEN={FROM_TWITTER}
DBENV_TWITTER_OAUTH_TOKEN_SECRET={FROM_TWITTER}
# --------------------------------------------------------------
```

- To deploy the solution, simply run `make deploy` and fill in the prompts.
- When prompted for a Databricks Host, enter the full name of your databricks workspace host, e.g. `https://westeurope.azuredatabricks.net`  (Or change the zone to the one closest to you)
- When prompted for a token, you can [generate a new token](https://docs.databricks.com/api/latest/authentication.html) in the databricks workspace.
- To view additional make commands run `make`

# Make Options

- `make test_environment`: Test python environment is setup correctly
- `make requirements`: Install Python Dependencies
- `make deploy_resources`: Deploy infrastructure (Just ARM template)
- `make create_secrets`: Create secrets in Databricks
- `make configure_databricks`: Configure Databricks
- `make deploy`: Deploys entire solution
- `make clean`: Delete all compiled Python files
- `make lint`: Lint using flake8
- `make create_environment`: Set up python interpreter environment

# Integration Tests

Main Assumption: The current design of the integration test pipeline, enables only one test to run e-2-e at any given moment, because of shared resources.
That said, in case the integration tests can spin-up/down an entire environment, that would not be an issue since each test runs on an encapsulated environment. The ingest notebook allows you to input a custom source and run the pipeline on this source.

## Deploying a Test environment

To create a new secondary environment that's ready for integration testing, you need to deploy a new environment, but there's no need to configure it.
For that purpose you can run the following commands:

```sh
make deploy_resources resource-group-name=test-social-rg region=westeurope subscription-id=5b86ec85-0709-4021-b73c-7a089d413ff0
make create_secrets
```

Those two commands will deploy a new environment to Azure, then configure the Databricks environment with the appropriate secrets.
You will also need to create a local file `databricks.env` in the root of the project, containing:

```
# ------ Constant environment variables to update Databricks -----------
DBENV_SQL_TABLE_NAME=ItemHistory
DBENV_SQL_JDBC_PORT=1433
# --------------------------------------------------------------
```

(You can use the full file with the twitter production configuration as well. Those keys will simply be ignored in the test environment).

## Connect to Travis-CI

This project displays how to connect [Travis-CI](https://travis-ci.org) to enable continuous integration and e2e validation.
To achieve that you need to perform the following tasks:

- Make sure to deploy a test environment using the make script
- Create a new Service Principal on Azure using [azure cli](https://docs.microsoft.com/en-us/cli/azure/create-an-azure-service-principal-azure-cli?toc=%2Fen-us%2Fazure%2Fazure-resource-manager%2Ftoc.json&bc=%2Fen-us%2Fazure%2Fbread%2Ftoc.json&view=azure-cli-latest) or [azure portal](https://docs.microsoft.com/en-us/azure/azure-resource-manager/resource-group-create-service-principal-portal?view=azure-cli-latest)
- Make sure to give the service principals permission on you azure subscription
- Deploy the test environment to generate an `.env` file
- Set the following environment variables
  - `DATABRICKS_ACCESS_TOKEN` - Access Token you created on the databricks portal
  - `DATABRICKS_URL` - Regional address of databrick, i.e. https://westeurope.azuredatabricks.net
  - `SERVICE_PRINCIPAL_APP_ID` - Service Principal Application ID
  - `SERVICE_PRINCIPAL_SUBSCRIPTION_ID` - Service Principal Subscription ID
  - `SERVICE_PRINCIPAL_PASSWORD` - Service Principal Password/Key
  - `SERVICE_PRINCIPAL_TENANT_ID` - Tenant ID your resources exist
  - `EVENTHUB_NAMESPACE` - The namespace of the event hubs service
  - `EVENTHUB_KEY_NAME` - The name of the key for authentication (Usually `RootManageSharedAccessKey`)
  - `EVENTHUB_KEY` - The key value for authentication
  - `EVENTHUB_ALERTS` - The name of the alerts event hub instance
- Connect travis ci to your github repo

The [test.sh](/.travis/test.sh) script, run by Travis, activate the make command `configure_databricks` with an extra parameter of `test=true` which causes the script to execute each notebook with an extra parameter which indicates an e-2-e validation test and enables the environment to execute accordingly.

# Potential Issues

> org.apache.spark.SparkException: Job aborted due to stage failure: Task 0 in stage 145.0 failed 4 times, most recent failure: Lost task 0.3 in stage 145.0 (TID 1958, 10.139.64.4, executor 0): org.apache.spark.SparkException: Failed to execute user defined function($anonfun$9: (string) => string)

This issue may be `Caused by: org.apache.http.client.HttpResponseException: Too Many Requests` due to cognitive services throttling limit on API requests.

> java.util.NoSuchElementException: An error occurred while enumerating the result, check the original exception for details.

> ERROR PoolWatchThread: Error in trying to obtain a connection. Retrying in 7000ms 
> java.security.AccessControlException: access denied org.apache.derby.security.SystemPermission( 'engine', 'usederbyinternals' )

These issue may be cause by DBR 4+ versions. You get rid of those issues by using the initialization notebook to run the script:

```scala
// Fix derby permissions
dbutils.fs.put("/databricks/init/fix-derby-permissions.sh", s"""
#!/bin/bash
cat <<EOF > ${System.getProperty("user.home")}/.java.policy
grant {
     permission org.apache.derby.security.SystemPermission "engine", "usederbyinternals";
};
EOF
""", true)
```

# Additional Links

Code base for REST function app: [https://github.com/morsh/social-rest-webapp](https://github.com/morsh/social-rest-webapp)

# Contributing

This project welcomes contributions and suggestions.  Most contributions require you to agree to a
Contributor License Agreement (CLA) declaring that you have the right to, and actually do, grant us
the rights to use your contribution. For details, visit https://cla.microsoft.com.

When you submit a pull request, a CLA-bot will automatically determine whether you need to provide
a CLA and decorate the PR appropriately (e.g., label, comment). Simply follow the instructions
provided by the bot. You will only need to do this once across all repos using our CLA.

This project has adopted the [Microsoft Open Source Code of Conduct](https://opensource.microsoft.com/codeofconduct/).
For more information see the [Code of Conduct FAQ](https://opensource.microsoft.com/codeofconduct/faq/) or
contact [opencode@microsoft.com](mailto:opencode@microsoft.com) with any additional questions or comments.
