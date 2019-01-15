package social.pipeline.source;

import java.util.List;
import java.util.Map;

public class SocialMessage {

  private String text;
  private long timestamp;
  private String source;
  private int numOfShares;
  private int numOfLikes;
  private boolean isShareOrRetweet;
  private String lang;
  private long id;
  private Map<String,Integer> reactions;
  private List<String> topics;
  private double sentiment;
  public SocialMessage(){

  }
  public SocialMessage(String text, long timestamp){
    this.text = text;
    this.timestamp = timestamp;
  }

  public SocialMessage(String text){
    this.text = text;
    this.timestamp = System.currentTimeMillis();
  }

  public boolean isShareOrRetweet() {
    return isShareOrRetweet;
  }

  public void setShareOrRetweet(boolean shareOrRetweet) {
    isShareOrRetweet = shareOrRetweet;
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public int getNumOfShares() {
    return numOfShares;
  }

  public void setNumOfShares(int numShared) {
    numOfShares = numShared;
  }

  public int getNumOfLikes() {
    return numOfLikes;
  }

  public void setNumOfLikes(int numOfLikes) {
    this.numOfLikes = numOfLikes;
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public Map<String, Integer> getReactions() {
    return reactions;
  }

  public void setReactions(Map<String, Integer> reactions) {
    this.reactions = reactions;
  }

  public List<String> getTopics() {
    return topics;
  }

  public void setTopics(List<String> topics) {
    this.topics = topics;
  }

  public double getSentiment() {
    return sentiment;
  }

  public void setSentiment(double sentiment) {
    this.sentiment = sentiment;
  }

  @Override
  public String toString() {
    return "SocialMessage{" +
      "text='" + text + '\'' +
      ", timestamp=" + timestamp +
      '}';
  }

  public String getLang() {
    return lang;
  }

  public void setLang(String lang) {
    this.lang = lang;
  }
}
