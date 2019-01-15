// Databricks notebook source
import org.apache.spark.eventhubs._

// Set up the Event Hub config dictionary with default settings
val eventhub_namespace = dbutils.preview.secret.get(scope = "storage_scope", key = "eventhub_namespace")
val eventhub_enriched = dbutils.preview.secret.get(scope = "storage_scope", key = "eventhub_enriched")
val eventhub_key = dbutils.preview.secret.get(scope = "storage_scope", key = "eventhub_key")
val readConnectionString = s"Endpoint=sb://${eventhub_namespace}.servicebus.windows.net/;" +
                           s"EntityPath=${eventhub_enriched};" +
                           s"SharedAccessKeyName=RootManageSharedAccessKey;" +
                           s"SharedAccessKey=${eventhub_key}"

val ehReadConf = EventHubsConf(readConnectionString)

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

val generateUuid = udf(() => java.util.UUID.randomUUID.toString())

// Cast the data as string (it comes in as binary by default)
val streamDF = enrichedStream
  .selectExpr("CAST(body as STRING)")
  .select(from_json($"body", schema) as "data")
  .withColumn("uniqueId", generateUuid())
  .withColumn("content", col("data.content"))
  .withColumn("timestamp", col("data.timestamp"))
  .withColumn("language", col("data.language"))
  .withColumn("entities", col("data.entities"))
  .select($"uniqueId", $"content", $"timestamp", $"language", explode($"entities").alias("topic"))

display(streamDF)

// COMMAND ----------

import java.sql.DriverManager
import org.apache.spark.sql.ForeachWriter

val sqlWriter = streamDF.writeStream.foreach(new ForeachWriter[Row] {
  var connection:java.sql.Connection = _
  var statement:java.sql.Statement = _
   
  // Load database configuation from environment secrets
  val serverName = dbutils.preview.secret.get("storage_scope", "sql_server_name") + ".database.windows.net"
  val database = dbutils.preview.secret.get("storage_scope", "sql_server_database")
  val writeuser = dbutils.preview.secret.get("storage_scope", "sql_admin_login")
  val writepwd = dbutils.preview.secret.get("storage_scope", "sql_admin_password")
  val tableName = dbutils.preview.secret.get("storage_scope", "DBENV_SQL_TABLE_NAME")
  val jdbcPort = dbutils.preview.secret.get("storage_scope", "DBENV_SQL_JDBC_PORT").toInt

  val driver = "com.microsoft.sqlserver.jdbc.SQLServerDriver"
  val jdbc_url = s"jdbc:sqlserver://${serverName}:${jdbcPort};database=${database};encrypt=true;trustServerCertificate=false;hostNameInCertificate=*.database.windows.net;loginTimeout=30;"
  
  def open(partitionId: Long, version: Long):Boolean = {
    Class.forName(driver)
    connection = DriverManager.getConnection(jdbc_url, writeuser, writepwd)
    statement = connection.createStatement
    true
  }
  
  def process(value: Row): Unit = {
    val uniqueId = value(0)
    val content = (value(1) + "").replaceAll("'", "''")
    val timestamp = value(2)
    val language = value(3)
    val topic = (value(4) + "").replaceAll("'", "''")
    
    val valueStr = s"'${uniqueId}', '${content}', '${timestamp}', '${language}', '${topic}'"
    val statementStr = s"INSERT INTO ${tableName} (UniqueId, Content, TweetTime, Language, Topic) VALUES (${valueStr})"
    statement.execute(statementStr)
  }

  def close(errorOrNull: Throwable): Unit = {
    connection.close
  }
})

val streamingWriter = sqlWriter.start()

// COMMAND ----------


