package social.pipeline.impl.custom;

import org.junit.Assert;
import org.junit.Test;
import social.pipeline.source.SocialMessage;
import social.pipeline.source.SocialQuery;
import social.pipeline.source.SocialQueryResult;

public class PredefinedSourceTest {

  @Test
  public void test_predefinedSource() throws Exception {
    PredefinedSource source = new PredefinedSource("/test_messages.json");
    SocialQueryResult my_query = source.search(new SocialQuery("my query"));
    SocialMessage socialMessage = my_query.getMessages().get(0);

    Assert.assertEquals(socialMessage.getText(),"Hello");

  }

  @Test
  public void test_customSource_dataset() throws Exception {
    PredefinedSource source = new PredefinedSource();
    SocialQueryResult my_query = source.search(new SocialQuery("my query"));
    SocialMessage socialMessage = my_query.getMessages().get(0);

    Assert.assertTrue(my_query.getMessages().size()>0);

  }

}
