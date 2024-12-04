package no.ngu.httpfile;

import java.io.FileInputStream;
import java.nio.file.Path;
import java.util.List;
import no.ngu.httpfile.HttpFile.Variable;
import no.ngu.httpfile.data.DataTraverser;

public interface StringValueProvider {

  public String getStringValue(String name);

  public record Variables(
      Iterable<Variable> variables,
      StringTemplateResolver templateValueProvider
  ) implements StringValueProvider {

    @Override
    public String getStringValue(String name) {
      for (var variable : variables) {
        if (name.equals(variable.name())) {
          return templateValueProvider.toString(variable.value());
        }
      }
      return null;
    }
  }

  public record Traversable(Object data, Iterable<DataTraverser> traversers) implements StringValueProvider {

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

  public record Properties(java.util.Properties properties) implements StringValueProvider {

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

  public record Providers(Iterable<StringValueProvider> providers) implements StringValueProvider {

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
