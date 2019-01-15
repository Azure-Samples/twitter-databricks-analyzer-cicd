package social.pipeline.source;

public class SocialQuery {

  private String query;
  private String lang;
  private int count;
  private long maxId = -1L;

  public SocialQuery() {
  }

  public SocialQuery(String query) {
    this.query = query;
  }

  public SocialQuery(String query, int numOfMessages) {
    this.query = query;
    this.count = numOfMessages;
  }

  public String getLang() {
    return this.lang;
  }

  public void setLang(String lang) {
    this.lang = lang;
  }


  public String getQuery() {
    return query;
  }

  public void setQuery(String query) {
    this.query = query;
  }

  public int getCount() {
    return count;
  }

  public void setCount(int count) {
    this.count = count;
  }

  public void setMaxId(long maxId) {
    this.maxId = maxId;
  }
}
