import java.util._
import java.util.concurrent._
import com.azure.messaging.eventhubs._
import scala.collection.JavaConverters._
import collection.JavaConversions._
import social.pipeline.impl.custom._
import social.pipeline.impl.twitter._
import social.pipeline.source._
import twitter4j.conf.ConfigurationBuilder

// Set notebook parameters:
dbutils.widgets.text("queryTwitterTopic", "russia")
dbutils.widgets.text("socialSource", "TWITTER")


// Get notebook parameters:
val queryTwitterTopic = dbutils.widgets.get("queryTwitterTopic")
val socialSourceParam = dbutils.widgets.get("socialSource")

println("Source used = " + socialSourceParam)

// Define Event hub configuration
val eventhub_namespace = dbutils.preview.secret.get("storage_scope", "eventhub_namespace")
val eventhub_input = dbutils.preview.secret.get("storage_scope", "eventhub_input")
val eventhub_key = dbutils.preview.secret.get("storage_scope", "eventhub_key")
val eventhub_keyname = "RootManageSharedAccessKey"
val connStr = s"Endpoint=sb://${eventhub_namespace}.servicebus.windows.net/;" +
                           s"EntityPath=${eventhub_input};" +
                           s"SharedAccessKeyName=RootManageSharedAccessKey;" +
                           s"SharedAccessKey=${eventhub_key}"

val  producer = new EventHubClientBuilder()
    .connectionString(connStr)
    .buildProducerClient();


// Send data to event hubs
def sendEvent(message: String) = {
  val messageData = new EventData(message)
  val batch = producer.createBatch();
  if(!batch.tryAdd(messageData)) {
    producer.get().send(batch);
    batch = producer.createBatch();
    batch.tryAdd(messageData);
  }
  if (eventDataBatch.getCount() > 0) {
    producer.get().send(batch);
  }
  System.out.println("Sent event: " + message + "\n")
}

// Twitter configuration builder
def getTwitterConfigurationBuilder(): ConfigurationBuilder = {
  val twitterConsumerKey = dbutils.preview.secret.get("storage_scope", "DBENV_TWITTER_CONSUMER_KEY")
  val twitterConsumerSecret = dbutils.preview.secret.get("storage_scope", "DBENV_TWITTER_CONSUMER_SECRET")
  val twitterOauthAccessToken = dbutils.preview.secret.get("storage_scope", "DBENV_TWITTER_OAUTH_ACCESS_TOKEN")
  val twitterOauthTokenSecret = dbutils.preview.secret.get("storage_scope", "DBENV_TWITTER_OAUTH_TOKEN_SECRET")

  var cb = new ConfigurationBuilder()

  cb.setDebugEnabled(true)
    .setOAuthConsumerKey(twitterConsumerKey)
    .setOAuthConsumerSecret(twitterConsumerSecret)
    .setOAuthAccessToken(twitterOauthAccessToken)
    .setOAuthAccessTokenSecret(twitterOauthTokenSecret)

  return cb

}


// Define which source is used to fetch messages (Twitter, Predefined, others)
var socialSource: SocialSource = null

if(socialSourceParam.equals("TWITTER")) {
  var cb = getTwitterConfigurationBuilder()
  socialSource = new TwitterSource(cb)
} else if(socialSourceParam.equals("CUSTOM")){
  socialSource = new PredefinedSource()
}

println("Source id =" + socialSource)

// Getting messages from source and sending them to EventHubs:
val query = new SocialQuery(queryTwitterTopic)
query.setCount(100)
query.setLang("en")
val finished = false
while (!finished) {

  val lowestStatusId = socialSource.search(query)
    .getMessages()
    .foldLeft(Long.MaxValue) {(currLowestStatusId, message) =>
      if(!message.isShareOrRetweet()) {
        sendEvent(message.getText())
      }
      Thread.sleep(2000)
      Math.min(message.getId(), currLowestStatusId)

    }
  query.setMaxId(lowestStatusId - 1)
}

// In case you forcibly want to close the connection to the Event Hub, use the following command:
// eventHubClient.get().close()

