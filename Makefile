.PHONY: test_environment requirements deploy_resources data configure_databricks clean lint create_environment
.INTERMEDIATE: deploy_resources

#################################################################################
# GLOBALS                                                                       #
#################################################################################

SHELL = /bin/bash
PROJECT_DIR := $(shell dirname $(realpath $(lastword $(MAKEFILE_LIST))))
PROFILE = default
PROJECT_NAME = socialDatabricks
PYTHON_INTERPRETER = python3
STARTDATETIME = $(shell date +"%Y-%m-%d %H:%M:%S")

ifeq (,$(shell which conda))
HAS_CONDA=False
else
HAS_CONDA=True
endif

#################################################################################
# COMMANDS                                                                      #
#################################################################################

## Test python environment is setup correctly 
test_environment:
	$(PYTHON_INTERPRETER) test_environment.py

## Install Python Dependencies 
requirements: test_environment
	pip install -U pip setuptools wheel
	pip install -r requirements.txt

## Deploy infrastructure (Just ARM template)
deploy_resources: 
	deploy/deploy.sh "$(resource-group-name)" "$(region)" "$(subscription-id)"

## Create secrets in Databricks
create_secrets:
	$(PYTHON_INTERPRETER) deploy/databricks/create_secrets.py

## Configure Databricks
configure_databricks:
	deploy/databricks/configure.sh "$(test)"

## Deploys entire solution
deploy: deploy_resources create_secrets configure_databricks

## Build jar files
build:
	cd src
	mvn clean install

## Run test alerts check
test_alerts:
	java -jar src/integration-tests/target/integration-tests-1.0-SNAPSHOT-jar-with-dependencies.jar "$(STARTDATETIME)"

## Load .env file
load_env_file:
	echo "Run the following command in the console:"
	echo "export $$(grep -v '^#' .env | xargs -d '\n')"

## Unload values from env file
unload_env_file:
	echo "Run the following command in the console:"
	echo "unset $$(grep -v '^#' .env | sed -E 's/=.*//' | xargs)"

## Test Cleanup
test_cleanup:
	deploy/databricks/cleanup.sh

## Delete all compiled Python files 
clean:
	find . -type f -name "*.py[co]" -delete
	find . -type d -name "__pycache__" -delete

## Lint using flake8 
lint:
	flake8 src

## Set up python interpreter environment
create_environment:
ifeq (True,$(HAS_CONDA))
		@echo ">>> Detected conda, creating conda environment."
ifeq (3,$(findstring 3,$(PYTHON_INTERPRETER)))
	conda create --name $(PROJECT_NAME) python=3
else
	conda create --name $(PROJECT_NAME) python=2.7
endif
		@echo ">>> New conda env created. Activate with:\nsource activate $(PROJECT_NAME)"
else
	@pip install -q virtualenv virtualenvwrapper
	@echo ">>> Installing virtualenvwrapper if not already intalled.\nMake sure the following lines are in shell startup file\n\
	export WORKON_HOME=$$HOME/.virtualenvs\nexport PROJECT_HOME=$$HOME/Devel\nsource /usr/local/bin/virtualenvwrapper.sh\n"
	@bash -c "source `which virtualenvwrapper.sh`;mkvirtualenv $(PROJECT_NAME) --python=$(PYTHON_INTERPRETER)"
	@echo ">>> New virtualenv created. Activate with:\nworkon $(PROJECT_NAME)"
endif


#################################################################################
# PROJECT RULES                                                                 #
#################################################################################



#################################################################################
# Self Documenting Commands                                                     #
#################################################################################

.DEFAULT_GOAL := show-help

# Inspired by <http://marmelab.com/blog/2016/02/29/auto-documented-makefile.html>
# sed script explained:
# /^##/:
# 	* save line in hold space
# 	* purge line
# 	* Loop:
# 		* append newline + line to hold space
# 		* go to next line
# 		* if line starts with doc comment, strip comment character off and loop
# 	* remove target prerequisites
# 	* append hold space (+ newline) to line
# 	* replace newline plus comments by `---`
# 	* print line
# Separate expressions are necessary because labels cannot be delimited by
# semicolon; see <http://stackoverflow.com/a/11799865/1968>
.PHONY: show-help
show-help:
	@echo "$$(tput bold)Available rules:$$(tput sgr0)"
	@echo
	@sed -n -e "/^## / { \
		h; \
		s/.*//; \
		:doc" \
		-e "H; \
		n; \
		s/^## //; \
		t doc" \
		-e "s/:.*//; \
		G; \
		s/\\n## /---/; \
		s/\\n/ /g; \
		p; \
	}" ${MAKEFILE_LIST} \
	| LC_ALL='C' sort --ignore-case \
	| awk -F '---' \
		-v ncol=$$(tput cols) \
		-v indent=19 \
		-v col_on="$$(tput setaf 6)" \
		-v col_off="$$(tput sgr0)" \
	'{ \
		printf "%s%*s%s ", col_on, -indent, $$1, col_off; \
		n = split($$2, words, " "); \
		line_length = ncol - indent; \
		for (i = 1; i <= n; i++) { \
			line_length -= length(words[i]) + 1; \
			if (line_length <= 0) { \
				line_length = ncol - indent - length(words[i]) - 1; \
				printf "\n%*s ", -indent, " "; \
			} \
			printf "%s ", words[i]; \
		} \
		printf "\n"; \
	}' \
	| more $(shell test $(shell uname) = Darwin && echo '--no-init --raw-control-chars')