package no.ngu.httpfile.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import jakarta.json.Json;
import no.ngu.httpfile.HttpFileParser;
import no.ngu.httpfile.data.DataTraverser;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link HttpFileClient}.
 */
public class HttpFileClientTest {

  @Test
  public void testParseTest1Http() {
    HttpFileParser parser = new HttpFileParser();
    try (var input = this.getClass().getResourceAsStream("/test1.http")) {
      String content = new String(input.readAllBytes());
      var requests = parser.parse(content);
      try (var testClient = new HttpFileClient()) {
        var result = testClient.performRequests(requests);
        var traversed = DataTraverser.traversePath(result, "test2.response.body.$.data",
            testClient.dataTraversers);
        assertEquals(
            Json.createValue("hello"),
            traversed
        );
      } catch (Exception ioe) {
        fail(ioe.getMessage());
      }
    } catch (Exception ioe) {
      fail(ioe.getMessage());
    }
  }
}
