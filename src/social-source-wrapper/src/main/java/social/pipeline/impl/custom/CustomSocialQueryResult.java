package social.pipeline.impl.custom;

import social.pipeline.source.SocialQueryResult;
import social.pipeline.source.SocialMessage;
import social.pipeline.source.SocialQuery;

import java.util.ArrayList;
import java.util.List;

public class CustomSocialQueryResult implements SocialQueryResult {

  List<SocialMessage> messages = new ArrayList<SocialMessage>();

  public CustomSocialQueryResult(){

  }

  public void setMessages(List<SocialMessage> messages){
    this.messages = messages;
  }

  @Override
  public List<SocialMessage> getMessages() {
    return messages;
  }

  @Override
  public String getQuery() {
    return null;
  }

  @Override
  public SocialQuery nextQuery() {
    return null;
  }

  @Override
  public boolean hasNext() {
    return false;
  }

}
