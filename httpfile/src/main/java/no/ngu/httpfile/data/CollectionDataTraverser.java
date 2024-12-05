package no.ngu.httpfile.data;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * {@link DataTraverser} implementation for traversing maps, collections and arrays.
 */
public class CollectionDataTraverser implements DataTraverser {

  @Override
  public boolean traverses(Object data, String step) {
    return data instanceof Map || data instanceof Collection || data.getClass().isArray();
  }

  @Override
  public Object traverse(Object data, String step) throws IllegalArgumentException {
    return switch (data) {
      case Map<?, ?> map -> map.get(step);
      case List<?> list -> list.get(DataTraverser.checkIndexStep(step, list.size()));
      case Collection<?> col -> {
        int num = DataTraverser.checkIndexStep(step, col.size());
        var it = col.iterator();
        while (it.hasNext()) {
          var item = it.next();
          if (num == 0) {
            yield item;
          }
          num--;
        }
        // should never happen
        yield DataTraverser.throwIllegalStep(data, step);
      }
      default -> {
        if (data.getClass().isArray()) {
          int num = DataTraverser.checkIndexStep(step, Array.getLength(data));
          yield Array.get(data, num);
        }
        yield DataTraverser.throwIllegalStep(data, step);
      }
    };
  }
}
