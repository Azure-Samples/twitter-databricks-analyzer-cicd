#!/bin/bash

# Taken from:
# http://www.dharmendrakeshari.com/sql-server-linux-installation-part4-install-sql-server-tools-ubuntu-updated/

sudo curl https://packages.microsoft.com/keys/microsoft.asc | sudo apt-key add
sudo curl https://packages.microsoft.com/config/ubuntu/16.04/prod.list | sudo tee /etc/apt/sources.list.d/msprod.list
sudo apt-get update
sudo apt-get install mssql-tools unixodbc-dev
echo 'export PATH="$PATH:/opt/mssql-tools/bin"' >> ~/.bashrc
source ~/.bashrc