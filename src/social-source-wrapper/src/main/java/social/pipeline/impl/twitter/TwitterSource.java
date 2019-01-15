package social.pipeline.impl.twitter;

import social.pipeline.source.SocialQuery;
import social.pipeline.source.SocialQueryResult;
import social.pipeline.source.SocialSource;
import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.conf.ConfigurationBuilder;

public class TwitterSource implements SocialSource {

  private Twitter twitterApi;

  public TwitterSource(Twitter twitterApi){
    this.twitterApi = twitterApi;
  }

  public TwitterSource(TwitterFactory twitterFactory){
    this.twitterApi = twitterFactory.getInstance();
  }

  public TwitterSource(ConfigurationBuilder configurationBuilder){
    TwitterFactory factory = new TwitterFactory(configurationBuilder.build());
    this.twitterApi = factory.getInstance();

  }

  @Override
  public SocialQueryResult search(SocialQuery query) throws Exception {
    Query twitterQuery = new Query(query.getQuery());
    twitterQuery.setCount(query.getCount());
    twitterQuery.setLang(query.getLang());
    QueryResult twitterResult = twitterApi.search(twitterQuery);
    SocialQueryResult res = new TwitterSocialQueryResult(twitterResult);
    return res;
  }

  @Override
  public void setOAuthConsumer(String key, String secret) {
    twitterApi.setOAuthConsumer(key,secret);
  }

  @Override
  public void setOAuthAccessToken(String accessToken, String tokenSecret) {
    twitterApi.setOAuthAccessToken(new AccessToken(accessToken,tokenSecret));
  }

}
