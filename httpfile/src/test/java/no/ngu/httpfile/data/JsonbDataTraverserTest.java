package no.ngu.httpfile.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.json.Json;
import java.util.List;
import org.junit.jupiter.api.Test;

public class JsonbDataTraverserTest {
  
  private Iterable<DataTraverser> traversers = List.of(new JsonbDataTraverser());

  @Test
  public void testTraverseJsonStructure() {
    var data = Json.createObjectBuilder()
        .add("first", Json.createObjectBuilder().add("second", Json.createArrayBuilder().add(3).add(4).add(5).build()).build())
        .add("fourth", Json.createObjectBuilder().add("fifth", Json.createArrayBuilder().add(6).add(7).add(8).build()).build())
        .build();
    assertEquals(Json.createValue(3), DataTraverser.traversePath(data, "first.second.0", traversers));
    assertEquals(Json.createValue(7), DataTraverser.traversePath(data, "fourth.fifth.1", traversers));
    assertThrows(IllegalArgumentException.class, () -> DataTraverser.traversePath(data, "first.second.third", traversers));
  }

  @Test
  public void testTraverseJsonString() {
    var data = """
    {
      "first": {
        "second": [3, 4, 5]
      },
      "fourth": {
        "fifth": [6, 7, 8]
      }
    }
    """;
    assertEquals(Json.createValue(3), DataTraverser.traversePath(data, "$.first.second.0", traversers));
    assertEquals(Json.createValue(7), DataTraverser.traversePath(data, "$.fourth.fifth.1", traversers));
    assertThrows(IllegalArgumentException.class, () -> DataTraverser.traversePath(data, "$.first.second.third", traversers));
  }
}
