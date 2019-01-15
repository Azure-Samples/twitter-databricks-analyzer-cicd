package social.pipeline.impl.twitter;

import org.junit.Assert;
import org.junit.Test;
import social.pipeline.source.SocialMessage;
import social.pipeline.source.SocialQuery;
import twitter4j.Query;
import twitter4j.Status;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class TwitterSocialQueryResultTest {

  @Test
  public void test_message_translation1() {
    String text = "Hello";
    Date date = new Date();
    int retweetCount = 5;
    String language = "EN";
    boolean isRetweeted = false;
    int id = 8;

    Status someStatus = new MockStatus(text, isRetweeted, date, id, retweetCount, language);
    List<SocialMessage> translate = TwitterSocialQueryResult.translate(Arrays.asList(someStatus));
    SocialMessage response = translate.get(0);
    Assert.assertEquals(response.getId(), id);
    Assert.assertEquals(response.getText(), text);
    Assert.assertEquals(response.getSource(), TwitterSocialQueryResult.SOURCE_NAME);
    Assert.assertEquals(response.isShareOrRetweet(), isRetweeted);
    Assert.assertEquals(response.getNumOfShares(),retweetCount);
  }

  @Test
  public void test_message_translation2() {
    String text = "Hello";
    Date date = new Date();
    int retweetCount = 5;
    String language = "EN";
    boolean isRetweeted = true;
    int id = 8;

    Status someStatus = new MockStatus(text, isRetweeted, date, id, retweetCount, language);
    List<SocialMessage> translate = TwitterSocialQueryResult.translate(Arrays.asList(someStatus));
    SocialMessage response = translate.get(0);
    Assert.assertEquals(response.getId(), id);
    Assert.assertEquals(response.getText(), text);
    Assert.assertEquals(response.getSource(), TwitterSocialQueryResult.SOURCE_NAME);
    Assert.assertEquals(response.isShareOrRetweet(), isRetweeted);
    Assert.assertEquals(response.getNumOfShares(),retweetCount);
  }

  @Test
  public void test_message_translation3() {
    String text = "Hello";
    Date date = new Date();
    int retweetCount = 5;
    String language = "EN";
    boolean isRetweeted = false;
    int id = 8;

    Status someStatus = new MockStatus(text, isRetweeted, date, id, retweetCount, language);
    List<SocialMessage> translate = TwitterSocialQueryResult.translate(Arrays.asList(someStatus));
    SocialMessage response = translate.get(0);
    Assert.assertEquals(response.getId(), id);
    Assert.assertEquals(response.getText(), text);
    Assert.assertEquals(response.getSource(), TwitterSocialQueryResult.SOURCE_NAME);
    Assert.assertEquals(response.isShareOrRetweet(), isRetweeted);
    Assert.assertEquals(response.getNumOfShares(),retweetCount);
    Assert.assertEquals(response.getTimestamp(),date.getTime());
    Assert.assertEquals(response.getSentiment(),0,0.001);
  }

  @Test
  public void test_query_translation(){
    String queryText = "russia";
    SocialQuery russia = TwitterSocialQueryResult.translate(new Query(queryText));
    Assert.assertEquals(russia.getQuery(), queryText);

  }

  @Test
  public void test_query_translation2(){
    String queryText = "russia";
    Query query = new Query(queryText);
    query.setLang("Armenian");
    query.setCount(713);
    SocialQuery russia = TwitterSocialQueryResult.translate(query);
    Assert.assertEquals(russia.getQuery(), queryText);
    Assert.assertEquals(russia.getLang(), "Armenian");
    Assert.assertEquals(russia.getCount(), 713);

  }


}
