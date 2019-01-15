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
# Description: Configure ~/.databrickscfg with relevant configuration, omitting the need to configure it manually
#
# Usage: 
#
# Requirments:  
#

echo "configuring databrick-cli authentication"

if [[ -f .env ]]; then
  export $(grep -v '^#' .env | xargs -d '\n')
  declare DATABRICKS_URL="https://$DBRICKS_DOMAIN"
  declare DATABRICKS_ACCESS_TOKEN="$DBRICKS_TOKEN"
fi

declare dbconfig=""
if [[ -f ~/.databrickscfg ]]; then
  dbconfig=$(<~/.databrickscfg)
fi

if [[ $dbconfig = *"host = "* && $dbconfig = *"token = "* ]]; then
  echo "file [~/.databrickscfg] is already configured"
else
  if [[ -z "$DATABRICKS_URL" || -z "$DATABRICKS_ACCESS_TOKEN" ]]; then
    echo "file [~/.databrickscfg] is not configured, but [DATABRICKS_URL],[DATABRICKS_ACCESS_TOKEN] env vars are not set"
  else
    echo "populating [~/.databrickscfg]"
    > ~/.databrickscfg
    echo "[DEFAULT]" >> ~/.databrickscfg
    echo "host = $DATABRICKS_URL" >> ~/.databrickscfg
    echo "token = $DATABRICKS_ACCESS_TOKEN" >> ~/.databrickscfg
    echo "" >> ~/.databrickscfg
  fi
fi