// Databricks notebook source
import org.apache.spark.eventhubs._

// Set up the Event Hub config dictionary with default settings
val eventhub_namespace = dbutils.preview.secret.get(scope = "storage_scope", key = "eventhub_namespace")
val eventhub_input = dbutils.preview.secret.get(scope = "storage_scope", key = "eventhub_input")
val eventhub_enriched = dbutils.preview.secret.get(scope = "storage_scope", key = "eventhub_enriched")
val eventhub_key = dbutils.preview.secret.get(scope = "storage_scope", key = "eventhub_key")
val readConnectionString = s"Endpoint=sb://${eventhub_namespace}.servicebus.windows.net/;" +
                           s"EntityPath=${eventhub_input};" +
                           s"SharedAccessKeyName=RootManageSharedAccessKey;" +
                           s"SharedAccessKey=${eventhub_key}"
val writeConnectionString = s"Endpoint=sb://${eventhub_namespace}.servicebus.windows.net/;" +
                           s"EntityPath=${eventhub_enriched};" +
                           s"SharedAccessKeyName=RootManageSharedAccessKey;" +
                           s"SharedAccessKey=${eventhub_key}"

val ehReadConf = EventHubsConf(readConnectionString)
val ehWriteConf = EventHubsConf(writeConnectionString)

// COMMAND ----------

import org.apache.spark.sql._
import org.apache.spark.sql.functions.{col, from_json, to_json, udf, struct, array}
import org.apache.spark.sql.types.{ArrayType, StringType, TimestampType}
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClientBuilder;

// Cognitive Services API connection settings
// ===============================================================================================
// Use the following lines instead of the function app stub to get results from cognitive services
// real API - take under consideration that cognitive services uses throttling to limit the amount
// of requests you can perform.
// -----------------------------------------------------------------------------------------------
// val accessKey = dbutils.preview.secret.get(scope = "storage_scope", key = "textanalytics_key1")
// val host = dbutils.preview.secret.get(scope = "storage_scope", key = "textanalytics_endpoint")
// val languagesPath = "/languages"
// val sentimentPath = "/sentiment"
// val entitiesPath = "/entities"
// val languagesUrl = host + languagesPath
// val sentimenUrl = host + sentimentPath
// val entitiesUrl = host + entitiesPath

// Using function app stub to get responses for language and topic extraction
val accessKey = ""
val host = dbutils.preview.secret.get(scope = "storage_scope", key = "textanalytics_url")
val languagesPath = "languages"
val sentimentPath = "sentiment"
val entitiesPath = "entities"
val languagesUrl = host.replace("{requestType}", languagesPath)
val sentimenUrl = host.replace("{requestType}", sentimentPath)
val entitiesUrl = host.replace("{requestType}", entitiesPath)

// Handles the call to Cognitive Services API.
// Expects Documents as parameters and the address of the API to call.
// Returns an instance of Documents in response.
def processUsingApi(url: String, params: String) = {
  
  // A helper function to make the DBFS API request, request/response is encoded/decoded as JSON
  val client = HttpClientBuilder.create().build();
  val request = new HttpPost(url);

  val postParams = new StringEntity(params);
  request.addHeader("Content-Type", "application/json");
  request.addHeader("Ocp-Apim-Subscription-Key", accessKey);
  request.setEntity(postParams);
  val response = client.execute(request);
  val handler = new org.apache.http.impl.client.BasicResponseHandler()
  handler.handleResponse(response).trim.toString
}

// COMMAND ----------

import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._
import org.apache.commons.lang.StringEscapeUtils.escapeJava

def getLanguage (text: String): String = {
  val jpayload = 
     ("documents" -> 
       List(
         ("id" -> "1") ~
         ("text" -> escapeJava(text))
       ))
  
  val response = processUsingApi(languagesUrl, compact(render(jpayload)))

  val jresult = parse(response, useBigDecimalForDouble = true);
  var language = "en"

  val jdocs = (jresult \ "documents").extract[List[JObject]]
  if (jdocs.length > 0) {
    val detectedLanguages = (jdocs(0) \ "detectedLanguages").extract[List[JObject]]
    if (detectedLanguages.length > 0) {
      val extractedLanguage = (detectedLanguages(0) \ "iso6391Name").extract[String]
      if (extractedLanguage != null && !extractedLanguage.isEmpty()) {
        language = extractedLanguage
      }
    }
  }

  return language
}

def getEntities (text: String): List[String] = {
  val language = getLanguage(text)
  val jpayload = 
     ("documents" -> 
       List(
         ("id" -> "1") ~
         ("language" -> language) ~
         ("text" -> escapeJava(text))
       ))

  val response = processUsingApi(entitiesUrl, compact(render(jpayload)))
  
  var entities = List("None")
  val jresult = parse(response, useBigDecimalForDouble = true);
  val jdocs = (jresult \ "documents").extract[List[JObject]]
  if (jdocs.length > 0) {
    val jentities = (jdocs(0) \ "entities" \ "name")
    if (jentities != JNothing) {
      if (jentities.isInstanceOf[JString]) {
        entities = List(jentities.extract[String])
      } else {
        entities = jentities.extract[List[String]]
      }
    }
  }

  return entities
}

// getEntities("I had a wonderful trip to Seattle and enjoyed seeing the Space Needle!")
val extractLanguage = udf(getLanguage _)
val extractEntities = udf(getEntities _, ArrayType(StringType))

// COMMAND ----------

import scala.collection.mutable.WrappedArray

def toStringJsonFunc(content: String, timestamp: String, language: String, entities: WrappedArray[String]): String = {
  val json = 
     ("content" -> content) ~ 
     ("timestamp" -> timestamp) ~
     ("language" -> language) ~
     ("entities" -> entities)
  
  return compact(render(json))
}
val toStringJson = udf(toStringJsonFunc _, StringType)

// COMMAND ----------

val reader = spark
  .readStream
  .format("eventhubs")
  .options(ehReadConf.toMap)
  .load()

val enriched = reader
  .select($"body" cast "string", $"enqueuedTime" cast "String" as "timestamp")
  .withColumn("Language", extractLanguage($"body"))
  .withColumn("Entities", extractEntities($"body"))
  .withColumn("body", toStringJson($"body", $"timestamp", $"Language", $"Entities"))

display(enriched)

// COMMAND ----------

// Write body data from a DataFrame to EventHubs. Events are distributed across partitions using round-robin model.
val enrichedStream = enriched
  .writeStream
  .format("eventhubs")
  .options(ehWriteConf.toMap)
  .option("checkpointLocation", "/mnt/blob/03.chk.enrichment.tmp")
  .start()

// COMMAND ----------


