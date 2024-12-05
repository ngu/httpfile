package no.ngu.httpfile;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

/**
 * The contents of an http file.
 */
public interface HttpFile {

  /**
   * All contents in an http file.
   */
  public record Model(List<Variable> fileVariables, List<Request> requests) {

    /**
     * Initializes with the given file variables and requests.
     *
     * @param fileVariables the file variables
     * @param requests the requests
     */
    public Model(List<Variable> fileVariables, Request... requests) {
      this(fileVariables, List.of(requests));
    }

    /**
     * Gets the value of the file variable with the given name.
     *
     * @param name the name
     * @return the resulting value, or empty if not found
     */
    public Optional<StringTemplate> getFileVariableValue(String name) {
      return HttpFile.getValue(name, fileVariables);
    }
  }

  /**
   * A string value split into alternating static and variable part. Starts with string contents.
   */
  public record StringTemplate(List<Part> parts) {

    /**
     * Initializes with the given parts.
     *
     * @param parts the parts
     */
    public StringTemplate(Part... parts) {
      this(List.of(parts));
    }

    /**
     * A part of a StringTemplate.
     */
    public sealed interface Part {
      
      /**
       * A constant part.
       */
      public record Constant(String value) implements Part {
      }

      /**
       * A variable reference part.
       */
      public record VariableRef(String name) implements Part {
      }

      /**
       * A macro call part.
       */
      public record MacroCall(Macro macro, List<String> args) implements Part {

        /**
         * Initializes with the given macro and arguments.
         *
         * @param macro the macro
         * @param args the arguments
         */
        public MacroCall(Macro macro, String... args) {
          this(macro, List.of(args));
        }
      }

      /**
       * A resource reference part. Only used in body content.
       */
      public record ResourceRef(String resource) implements Part {
      }
    }

    /**
     * Creates a StringTemplate from the given string.
     * Dynamic parts are enclosed in {{ and }}.
     *
     * @param s the string
     * @return the resulting StringTemplate
     */
    public static StringTemplate of(String s) {
      List<Part> parts = new ArrayList<>();
      int pos = 0;
      while (pos < s.length()) {
        // works even if varStart = -1
        int varStart = s.indexOf("{{", pos);
        int varEnd = s.indexOf("}}", varStart + 1);
        if (varStart < 0) {
          // add final static part
          parts.add(new Part.Constant(s.substring(pos)));
          break;
        } else {
          if (varEnd < 0) {
            throw new IllegalArgumentException("{{ without }}");
          }
          // add intermediate static part
          parts.add(new Part.Constant(s.substring(pos, varStart)));
          // add function or variable part
          if (s.charAt(varStart + 2) == '$') {
            var nameEnd = s.indexOf(' ', varStart + 3);
            if (nameEnd < 0) {
              nameEnd = varEnd;
            }
            String name = s.substring(varStart + 3, nameEnd);
            String[] args = (nameEnd < varEnd ? s.substring(nameEnd, varEnd).trim().split(" +")
                : new String[0]);
            parts.add(new Part.MacroCall(Macro.valueOf(name), args));
          } else {
            parts.add(new Part.VariableRef(s.substring(varStart + 2, varEnd)));
          }
        }
        pos = varEnd + 2;
      }
      return new StringTemplate(parts);
    }
  }

  /**
   * A named value.
   */
  public interface Named<T> {

    /**
     * The name of the value.
     *
     * @return the name
     */
    public String name();

    /**
     * The value.
     *
     * @return the value
     */
    public T value();
  }

  /**
   * Gets the value with the given name from the list of named values.
   *
   * @param <T> the type of the Named value
   * @param name the name
   * @param nameds the named values
   * @return the result of lookup up the name
   */
  public static <T> Optional<T> getValue(String name, List<? extends Named<T>> nameds) {
    if (nameds != null) {
      for (var named : nameds) {
        if (name.equals(named.name())) {
          return Optional.ofNullable(named.value());
        }
      }
    }
    return Optional.empty();
  }

  /**
   * A property declaration.
   */
  public record Property(String name, String value) implements Named<String> {
  }

  /**
   * A variable declaration.
   */
  public record Variable(String name, StringTemplate value) implements Named<StringTemplate> {

    /**
     * Initializes with the given name and value.
     *
     * @param name the name
     * @param value the value
     */
    public Variable(String name, String value) {
      this(name, StringTemplate.of(value));
    }
  }

  /**
   * The HTTP verbs.
   */
  public enum HttpMethod {
    GET, HEAD, POST, PUT, PATCH, DELETE;

    /**
     * Checks if the given string is a valid HTTP method.
     *
     * @param s the string
     * @return true if the string is a valid HTTP method
     */
    public static boolean is(String s) {
      try {
        return HttpMethod.valueOf(s) != null;
      } catch (IllegalArgumentException iae) {
        return false;
      }
    }
  }

  /**
   * A request.
   */
  public record Request(List<Property> requestProperties, HttpMethod method, StringTemplate target,
      String version, List<Header> headers, Body body) {
    
    /**
     * Convenience constructor for a request with a name property.
     *
     * @param name the request name
     * @param method the HTTP method
     * @param target the target URL
     * @param headers the HTTP headers
     * @param body the body
     */
    public Request(String name, HttpMethod method, String target,
        List<Header> headers, Body body) {
      this(List.of(new Property("name", name)), method, StringTemplate.of(target),
          null, headers, body);
    }

    /**
     * Convenience constructor for a request without properties or name.
     *
     * @param method the HTTP method
     * @param target the target URL
     * @param headers the HTTP headers
     * @param body the body
     */
    public Request(HttpMethod method, String target,
        List<Header> headers, Body body) {
      this(List.of(), method, StringTemplate.of(target), null, headers, body);
    }

    /**
     * Gets the value of the request property with the given name.
     *
     * @param name the name
     * @return the resulting value, or empty if not found
     */
    public Optional<String> getRequestPropertyValue(String name) {
      return HttpFile.getValue(name, requestProperties);
    }
  }

  /**
   * An http header.
   */
  public record Header(String name, StringTemplate value) {

    /**
     * Initializes with the given name and value.
     *
     * @param name the name
     * @param value the value
     */
    public Header(String name, String value) {
      this(name, StringTemplate.of(value));
    }
  }

  /**
   * Body contents.
   */
  public record Body(String contentType, StringTemplate content) {

    /**
     * Initializes with the given content type and content.
     *
     * @param contentType the content type
     * @param value the content
     */
    public Body(String contentType, String value) {
      this(contentType, StringTemplate.of(value));
    }
  }

  //

  /**
   * Convenience constructor using the http file parser.
   *
   * @param lines the lines of the http file
   * @return the resulting model
   */
  public static Model of(Iterator<String> lines) {
    return new HttpFileParser().parse(lines);
  }

  /**
   * Convenience constructor using the http file parser.
   *
   * @param input the http file contents
   * @return the resulting model
   */
  public static Model of(String input) {
    return new HttpFileParser().parse(input);
  }
}
