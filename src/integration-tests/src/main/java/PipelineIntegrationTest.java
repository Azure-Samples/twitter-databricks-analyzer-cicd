import java.io.IOException;

public class PipelineIntegrationTest {

  private void test() throws IOException, InterruptedException {
    // Run pipeline
    runTestPipeline();
    // Wait for pipeline to finish by sleeping or by listening to the output
    // eventhubs
    Thread.sleep(1000 * 60 * 10);
    // Grab results from eventhubs and compare to expected
    evalutePipelineResult();
  }

  private void runTestPipeline() {
    System.out.println("Not implemented yet");
  }

  private void evalutePipelineResult() {
    System.out.println("Not implemented yet");
  }

  public static void main(String[] args) throws IOException, InterruptedException {
    PipelineIntegrationTest test = new PipelineIntegrationTest();
    test.test();
  }

}
