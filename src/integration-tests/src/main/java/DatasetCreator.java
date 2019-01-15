import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import social.pipeline.impl.twitter.TwitterSource;
import social.pipeline.source.SocialMessage;
import social.pipeline.source.SocialQuery;
import social.pipeline.source.SocialQueryResult;
import twitter4j.conf.ConfigurationBuilder;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.List;
import java.util.Properties;

/**
 * Creates a dataset of tweets to be used for tests
 */
public class DatasetCreator {

  public List<SocialMessage> getMessagesFromTwitter(String topic, int numOfTweets) throws Exception {
    Properties prop = new Properties();
    InputStream input = this.getClass().getResourceAsStream("/keys.properties");
    prop.load(input);

    String twitterConsumerKey = prop.getProperty("consumer_key");
    String twitterConsumerSecret = prop.getProperty("consumer_secret");
    String twitterOauthAccessToken = prop.getProperty("access_token");
    String twitterOauthTokenSecret = prop.getProperty("access_token_secret");


    ConfigurationBuilder cb = new ConfigurationBuilder();
    cb.setDebugEnabled(true)
      .setOAuthConsumerKey(twitterConsumerKey)
      .setOAuthConsumerSecret(twitterConsumerSecret)
      .setOAuthAccessToken(twitterOauthAccessToken)
      .setOAuthAccessTokenSecret(twitterOauthTokenSecret);


    TwitterSource source = new TwitterSource(cb);
    SocialQuery query = new SocialQuery(topic);
    query.setCount(numOfTweets);
    SocialQueryResult result = source.search(query);
    List<SocialMessage> messages = result.getMessages();
    return messages;
  }

  public void writeMessagesToFile(List<SocialMessage> messages){
    try (Writer writer = new FileWriter("messages.json")) {
      Gson gson = new GsonBuilder().create();
      gson.toJson(messages, writer);
    } catch (IOException e) {
      e.printStackTrace();
    }

  }


  public static void main(String[] args) throws Exception {
    DatasetCreator creator = new DatasetCreator();
    List<SocialMessage> tweets = creator.getMessagesFromTwitter("tel_aviv",2000);
    creator.writeMessagesToFile(tweets);

  }
}
