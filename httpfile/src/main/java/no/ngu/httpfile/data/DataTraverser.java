package no.ngu.httpfile.data;

public interface DataTraverser {

  /**
   * Tells whether this traverser can traverse the data.
   *
   * @param data the data to test
   * @param step the step to take
   * @return true if this traverser can traverse the data, otherwise false
   */
  public boolean traverses(Object data, String step);

  /**
   * Traverse data one step.
   *
   * @param data the data to traverse
   * @param step the step to take
   * @return the data after traversing
   * @throws IllegalArgumentException if step is not valid for the data
   */
  public Object traverse(Object data, String step) throws IllegalArgumentException;

  /**
   * Helper method to find the correct traverser for the data.
   *
   * @param data the data to find traverser for
   * @param traversers the traversers to select among
   * @return the first traverser for the data, or null if none
   */
  public static DataTraverser traverserFor(Object data, String step, Iterable<DataTraverser> traversers) {
    for (var traverser : traversers) {
      if (traverser.traverses(data, step)) {
        return traverser;
      }
    }
    return null;
  }

  /**
   * Helper method to report that a step is illegal.
   *
   * @param data the data to traverse
   * @param step the step to take
   * @return som object, never reached
   * @throws IllegalArgumentException since the step is illegal
   */
  public static <T> T throwIllegalStep(Object data, String step) throws IllegalArgumentException {
    throw new IllegalArgumentException("Step " + step + " is illegal for data of " + data.getClass());
  }

  /**
   * Helper method to check that a step is an integer and within bounds.
   *
   * @param indexStep the index step to check
   * @param size the size of the collection
   * @return the integer value of the step
   * @throws IllegalArgumentException if the step is not an integer or it is out of bounds
   */
  public static int checkIndexStep(String indexStep, int size) throws IllegalArgumentException {
    try {
      int index = Integer.valueOf(indexStep);
      checkIndexBounds(index, size);
      return index;
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Not an integer step: " + indexStep);
    }
  }

  /**
   * Helper method to check that an index is within bounds.
   *
   * @param index the index to check
   * @param size the size of the collection
   * @throws IllegalArgumentException if the index is out of bounds
   */
  public static void checkIndexBounds(int index, int size) throws IllegalArgumentException {
    if (index < 0 || index >= size) {
      throw new IllegalArgumentException("Step out of bounds, should be >= 0 and < " + size);
    }
  }

  /**
   * Traverse a path in the data, selecting among the traversers for each step.
   *
   * @param data the initial data to traverse
   * @param path the path to traverse
   * @param traversers the traversers to select among
   * @return the data after traversing the path
   */
  public static Object traversePath(Object data, String path, Iterable<DataTraverser> traversers) {
    int pos = 0;
    while (pos < path.length()) {
      if (data == null) {
        throw new NullPointerException("Nothing to traverse for (rest of) path: " + path.substring(pos));
      }
      int dotPos = path.indexOf('.', pos);
      if (dotPos < 0) {
        dotPos = path.length();
      }
      var step = path.substring(pos, dotPos);
      var traverser = traverserFor(data, step, traversers);
      if (traverser == null) {
        traverser = traverserFor(data, null, traversers);
      }
      if (traverser == null) {
        throw new IllegalArgumentException("No traverser for " + data);
      }
      data = traverser.traverse(data, step);
      pos = dotPos + 1;
    }
    return data;
  }

  /**
   * Tells whether this traverser can traverse the data.
   *
   * @param data the data to test
   * @param step the step to take
   * @return true if this traverser can traverse the data, otherwise false
   */
  public default boolean converts(Object data) {
    return data instanceof String || data instanceof Number || data instanceof Boolean;
  }

  /**
   * Helper method to find the correct converter for the data.
   *
   * @param data the data to find converter for
   * @param dataTraversers the converters to select among
   * @return the first converter for the data, or null if none
   */
  public static DataTraverser converterFor(Object data, Iterable<DataTraverser> converters) {
    for (var converter : converters) {
      if (converter.converts(data)) {
        return converter;
      }
    }
    return null;
  }

  /**
   * Convert final step to string.
   *
   * @param data the data to convert
   * @return the string representation of the data
   */
  default String asString(Object data) {
    return String.valueOf(data);
  }

  /**
   * Convert data to integer, using the String conversion first.
   *
   * @param data the data to convert
   * @return the integer representation of the data
   */
  public default int asInt(Object data) {
    return Integer.valueOf(asString(data));
  }

  /**
   * Convert data to double, using the String conversion first.
   *
   * @param data the data to convert
   * @return the double representation of the data
   */
  public default double asDouble(Object data) {
    return Double.valueOf(asString(data));
  }

  /**
   * Convert data to boolean, using the String conversion first.
   *
   * @param data the data to convert
   * @return the boolean representation of the data
   */
  public default boolean asBoolean(Object data) {
    return switch (asString(data).toLowerCase()) {
      case "true" -> true;
      case "false" -> false;
      default -> throw new IllegalArgumentException("Not a boolean: " + data);
    };
  }
}
