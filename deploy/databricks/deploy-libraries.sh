#!/bin/bash

declare cluster_id=$1

if [[ -z $cluster_id ]]; then
  echo "No cluster id was provided for deploying libraries"
  exit
fi

echo "Installing libraries..."
databricks libraries install --maven-coordinates com.microsoft.azure:azure-eventhubs-spark_2.11:2.3.1 --cluster-id $cluster_id
databricks libraries install --maven-coordinates org.apache.bahir:spark-streaming-twitter_2.11:2.2.0 --cluster-id $cluster_id
databricks libraries install --maven-coordinates org.json4s:json4s-native_2.11:3.5.4 --cluster-id $cluster_id
