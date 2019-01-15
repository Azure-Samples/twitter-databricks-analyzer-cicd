package social.pipeline.impl.twitter;

import social.pipeline.source.SocialQueryResult;
import social.pipeline.source.SocialMessage;
import social.pipeline.source.SocialQuery;
import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Status;

import java.util.List;
import java.util.stream.Collectors;

public class TwitterSocialQueryResult implements SocialQueryResult {

  public static final String SOURCE_NAME = "Twitter";
  private final QueryResult queryResult;

  public TwitterSocialQueryResult(QueryResult queryResult) {
    this.queryResult = queryResult;
  }

  @Override
  public List<SocialMessage> getMessages() {
    return translate(queryResult.getTweets());
  }


  @Override
  public String getQuery() {
    return queryResult.getQuery();
  }

  @Override
  public SocialQuery nextQuery() {
    return translate(queryResult.nextQuery());
  }

  @Override
  public boolean hasNext() {
    return queryResult.hasNext();
  }

  public static List<SocialMessage> translate(List<Status> statuses) {
    if (statuses == null) {
      return null;
    }

    List<SocialMessage> socialMessages = statuses.stream().map(status -> {
      SocialMessage message = new SocialMessage(status.getText());
      message.setId(status.getId());
      message.setNumOfLikes(status.getFavoriteCount());
      message.setNumOfShares(status.getRetweetCount());
      message.setSource(SOURCE_NAME);
      message.setLang(status.getLang());
      message.setShareOrRetweet(status.isRetweet());
      message.setTimestamp(status.getCreatedAt().getTime());
      return message;
    }).collect(Collectors.toList());

    return socialMessages;
  }

  public static SocialQuery translate(Query query){
    SocialQuery socialQuery = new SocialQuery();
    socialQuery.setLang(query.getLang());
    socialQuery.setQuery(query.getQuery());
    socialQuery.setCount(query.getCount());

    return socialQuery;
  }
}
