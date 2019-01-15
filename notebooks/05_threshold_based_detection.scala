// Databricks notebook source
import org.apache.spark.eventhubs._

// Set up the Event Hub config dictionary with default settings
val eventhub_namespace = dbutils.preview.secret.get(scope = "storage_scope", key = "eventhub_namespace")
val eventhub_enriched = dbutils.preview.secret.get(scope = "storage_scope", key = "eventhub_enriched")
val eventhub_alerts = dbutils.preview.secret.get(scope = "storage_scope", key = "eventhub_alerts")
val eventhub_key = dbutils.preview.secret.get(scope = "storage_scope", key = "eventhub_key")
val readConnectionString = s"Endpoint=sb://${eventhub_namespace}.servicebus.windows.net/;" +
                           s"EntityPath=${eventhub_enriched};" +
                           s"SharedAccessKeyName=RootManageSharedAccessKey;" +
                           s"SharedAccessKey=${eventhub_key}"
val writeConnectionString = s"Endpoint=sb://${eventhub_namespace}.servicebus.windows.net/;" +
                           s"EntityPath=${eventhub_alerts};" +
                           s"SharedAccessKeyName=RootManageSharedAccessKey;" +
                           s"SharedAccessKey=${eventhub_key}"

// This is the threshold above which, alerts will be sent to the alerts eventhub
val alertThreshold = 5

val ehReadConf = EventHubsConf(readConnectionString)
val ehWriteConf = EventHubsConf(writeConnectionString)

// COMMAND ----------

import org.apache.spark.sql.types._
import org.apache.spark.sql.functions._

// Define the schema to apply to the data...
val schema = StructType(List(
  StructField("content", StringType),
  StructField("timestamp", StringType),
  StructField("language", StringType),
  StructField("entities", ArrayType(StringType))
))

// COMMAND ----------

// Connect to the IoT Hub...
val enrichedStream = spark
  .readStream
  .format("eventhubs")
  .options(ehReadConf.toMap)
  .load()

// Cast the data as string (it comes in as binary by default)
val streamDF = enrichedStream
  .selectExpr("CAST(body as STRING)")
  .select(from_json($"body", schema) as "data")
  .withColumn("timestamp", col("data.timestamp").cast(TimestampType))
  .withColumn("entities", col("data.entities"))
  .select($"timestamp", explode($"entities").alias("topic"))
  .withWatermark("timestamp", "10 minute")
  .groupBy(window($"timestamp", "10 minutes", "1 minute"), $"topic")
  .count()
  .selectExpr("cast (window.start as timestamp) AS windowStart", "cast (window.end as timestamp) AS windowEnd", "topic", "count")

display(streamDF)

// COMMAND ----------

import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._

def toStringJsonFunc(windowStart: String, windowEnd: String, topic: String, count: Long): String = {

  val json = 
     ("windowStart" -> windowStart) ~ 
     ("windowEnd" -> windowEnd) ~
     ("topic" -> topic) ~
     ("count" -> count)
  
  return compact(render(json))
}
val toStringJson = udf(toStringJsonFunc _, StringType)

// COMMAND ----------

val alertStream = streamDF
  .where(s"count >= ${alertThreshold}")
  .selectExpr("cast(windowStart as string)", "cast(windowEnd as string)", "topic", "count")
  .withColumn("body", toStringJson($"windowStart", $"windowEnd", $"topic", $"count"))
  .select("body")

display(alertStream)

val writeAlertsStream = alertStream
  .writeStream
  .format("eventhubs")
  .options(ehWriteConf.toMap)
  .option("checkpointLocation", "/mnt/blob/05.chk.threshold.tmp")
  .start()


// COMMAND ----------


