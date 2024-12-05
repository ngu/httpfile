package no.ngu.httpfile;

import java.io.FileInputStream;
import java.nio.file.Path;
import java.util.List;
import no.ngu.httpfile.HttpFile.Variable;
import no.ngu.httpfile.data.DataTraverser;

/**
 * Provides values for string template values,
 * e.g. variable lookup and data traversal in result objects.
 */
public interface StringValueProvider {

  /**
   * Gets the string value for the given name.
   *
   * @param name the name
   * @return the string value, or null if not found
   */
  public String getStringValue(String name);

  /**
   * StringValueProvider that looks up name in a list of variables.
   * Uses a template value provider to expand variable values.
   */
  public record Variables(
      Iterable<Variable> variables,
      StringTemplateResolver templateValueResolver
  ) implements StringValueProvider {

    @Override
    public String getStringValue(String name) {
      for (var variable : variables) {
        if (name.equals(variable.name())) {
          return templateValueResolver.toString(variable.value());
        }
      }
      return null;
    }
  }

  /**
   * StringValueProvider that traverses data objects.
   */
  public record Traversable(Object data, Iterable<DataTraverser> traversers)
      implements StringValueProvider {

    @Override
    public String getStringValue(String path) {
      Object traversed = DataTraverser.traversePath(data, path, traversers);
      if (traversed != null) {
        DataTraverser converter = DataTraverser.converterFor(traversed, traversers);
        return (converter != null ? converter.asString(traversed) : String.valueOf(traversed));
      }
      return null;
    }
  }

  /**
   * StringValueProvider that looks up name in a java.util.Properties object.
   */
  public record Properties(java.util.Properties properties) implements StringValueProvider {

    /**
     * StringValueProvider for properties loaded from the given path.
     *
     * @param path the path
     * @return the properties
     */
    public static Properties of(Path path) {
      var props = new java.util.Properties();
      try (var inputStream = new FileInputStream(path.toFile())) {
        props.load(inputStream);
        return new Properties(props);
      } catch (Exception e) {
        throw new IllegalArgumentException("Exception when loading properties from "
            + path + "; " + e, e);
      }
    }

    @Override
    public String getStringValue(String name) {
      return properties.getProperty(name);
    }
  }

  /**
   * Composite StringValueProvider.
   */
  public record Providers(Iterable<StringValueProvider> providers) implements StringValueProvider {

    /**
     * Composite StringValueProvider for the provided providers.
     *
     * @param providers the providers
     */
    public Providers(StringValueProvider... providers) {
      this(List.of(providers));
    }

    @Override
    public String getStringValue(String name) {
      for (var provider : providers) {
        var value = provider.getStringValue(name);
        if (value != null) {
          return value.toString();
        }
      }
      return null;
    }
  }
}
