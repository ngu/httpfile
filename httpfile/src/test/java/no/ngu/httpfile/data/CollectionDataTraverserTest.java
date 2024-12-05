package no.ngu.httpfile.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link CollectionDataTraverser}.
 */
public class CollectionDataTraverserTest {
  
  private Iterable<DataTraverser> traversers = List.of(new CollectionDataTraverser());

  @Test
  public void testTraverse() {
    var data = Map.<String, Object>of(
        "first", Map.of("second", List.of(3, 4, 5)),
        "fourth", Map.of("fifth", new Integer[]{6, 7, 8}),
        "seventh", Map.of("eighth", "nineth")
    );
    
    assertEquals(List.of(3, 4, 5), DataTraverser.traversePath(data, "first.second", traversers));
    assertEquals(3, DataTraverser.traversePath(data, "first.second.0", traversers));
    assertEquals(7, DataTraverser.traversePath(data, "fourth.fifth.1", traversers));
    assertThrows(IllegalArgumentException.class,
        () -> DataTraverser.traversePath(data, "first.second.third", traversers));
  }
}
