package social.pipeline.impl.twitter;

import social.pipeline.source.SocialMessage;
import social.pipeline.source.SocialQuery;
import social.pipeline.source.SocialQueryResult;
import twitter4j.conf.ConfigurationBuilder;

import java.io.InputStream;
import java.util.List;
import java.util.Properties;

public class TwitterSourceTest {


  /**
   * This test calls the TwitterAPI
   */
  //@Test
  //@Ignore
  public void test_query() throws Exception {

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
    SocialQueryResult result = source.search(new SocialQuery("russia"));
    List<SocialMessage> messages = result.getMessages();
    messages.stream().forEach(e -> System.out.println(e));
  }

}
