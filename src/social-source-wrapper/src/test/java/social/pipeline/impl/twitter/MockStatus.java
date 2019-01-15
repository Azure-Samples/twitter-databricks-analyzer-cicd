package social.pipeline.impl.twitter;

import twitter4j.*;

import java.util.Date;

public class MockStatus implements Status {

  private final String text;
  private final boolean isRetweet
    ;
  private final Date date;
  private final int id;
  private final int retweetCount;
  private String lang;

  public MockStatus(String text, boolean isRetweet, Date date, int id, int retweetCount, String lang){
    this.text = text;
    this.isRetweet = isRetweet;
    this.date = date;
    this.id = id;
    this.retweetCount = retweetCount;
    this.lang = lang;
  }



  @Override
  public Date getCreatedAt() {
    return date;
  }

  @Override
  public long getId() {
    return id;
  }

  @Override
  public String getText() {
    return text;
  }

  @Override
  public int getDisplayTextRangeStart() {
    return 0;
  }

  @Override
  public int getDisplayTextRangeEnd() {
    return 0;
  }

  @Override
  public String getSource() {
    return null;
  }

  @Override
  public boolean isTruncated() {
    return false;
  }

  @Override
  public long getInReplyToStatusId() {
    return 0;
  }

  @Override
  public long getInReplyToUserId() {
    return 0;
  }

  @Override
  public String getInReplyToScreenName() {
    return null;
  }

  @Override
  public GeoLocation getGeoLocation() {
    return null;
  }

  @Override
  public Place getPlace() {
    return null;
  }

  @Override
  public boolean isFavorited() {
    return false;
  }

  @Override
  public boolean isRetweeted() {
    return isRetweet;
  }

  @Override
  public int getFavoriteCount() {
    return 0;
  }

  @Override
  public User getUser() {
    return null;
  }

  @Override
  public boolean isRetweet() {
    return isRetweet;
  }

  @Override
  public Status getRetweetedStatus() {
    return null;
  }

  @Override
  public long[] getContributors() {
    return new long[0];
  }

  @Override
  public int getRetweetCount() {
    return retweetCount;
  }

  @Override
  public boolean isRetweetedByMe() {
    return false;
  }

  @Override
  public long getCurrentUserRetweetId() {
    return 0;
  }

  @Override
  public boolean isPossiblySensitive() {
    return false;
  }

  @Override
  public String getLang() {
    return lang;
  }

  @Override
  public Scopes getScopes() {
    return null;
  }

  @Override
  public String[] getWithheldInCountries() {
    return new String[0];
  }

  @Override
  public long getQuotedStatusId() {
    return 0;
  }

  @Override
  public Status getQuotedStatus() {
    return null;
  }

  @Override
  public URLEntity getQuotedStatusPermalink() {
    return null;
  }

  @Override
  public int compareTo(Status o) {
    return 0;
  }

  @Override
  public UserMentionEntity[] getUserMentionEntities() {
    return new UserMentionEntity[0];
  }

  @Override
  public URLEntity[] getURLEntities() {
    return new URLEntity[0];
  }

  @Override
  public HashtagEntity[] getHashtagEntities() {
    return new HashtagEntity[0];
  }

  @Override
  public MediaEntity[] getMediaEntities() {
    return new MediaEntity[0];
  }

  @Override
  public SymbolEntity[] getSymbolEntities() {
    return new SymbolEntity[0];
  }

  @Override
  public RateLimitStatus getRateLimitStatus() {
    return null;
  }

  @Override
  public int getAccessLevel() {
    return 0;
  }
}
