package no.ngu.httpfile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import no.ngu.httpfile.HttpFile.Body;
import no.ngu.httpfile.HttpFile.Header;
import no.ngu.httpfile.HttpFile.HttpMethod;
import no.ngu.httpfile.HttpFile.Model;
import no.ngu.httpfile.HttpFile.Property;
import no.ngu.httpfile.HttpFile.Request;
import no.ngu.httpfile.HttpFile.StringTemplate.Part;
import no.ngu.httpfile.HttpFile.Variable;
import no.ngu.httpfile.HttpFileParser.Token.HeaderLine;
import no.ngu.httpfile.HttpFileParser.Token.PropertyLine;
import no.ngu.httpfile.HttpFileParser.Token.RequestLine;

/**
 * Parses an http file into a {@link Model}.
 */
public class HttpFileParser {

  sealed interface Token {

    static boolean matchesEnd(String line) {
      return line == null;
    }

    static boolean matchesBlank(String line) {
      return line.length() == 0;
    }

    static boolean matchesRequestSeparator(String line) {
      return line.startsWith("###");
    }

    static boolean matchesComment(String line) {
      return line.startsWith("#") || line.startsWith("//");
    }

    record VariableLine(String name, String value) implements Token {
      static boolean matches(String line) {
        return line.startsWith("@");
      }

      static VariableLine of(String line) {
        int pos = line.indexOf("=");
        return new VariableLine(line.substring(1, pos).trim(), line.substring(pos + 1).trim());
      }
    }

    record PropertyLine(String name, String value) implements Token {
      static boolean matches(String line) {
        if (!line.startsWith("#")) {
          return false;
        }
        line = line.substring(1).trim();
        return line.startsWith("@");
      }

      static PropertyLine of(String line) {
        line = line.substring(1).trim();
        int pos = line.indexOf(" ");
        if (pos < 0) {
          pos = line.indexOf("=");
        }
        return new PropertyLine(line.substring(1, pos).trim(), line.substring(pos + 1).trim());
      }
    }

    record ContinuationLine(String line) implements Token {
      static boolean matches(String line) {
        return line.startsWith(" ") || line.startsWith("\t");
      }
    }

    record RequestLine(HttpMethod verb, String target, String version) implements Token {

      static boolean matches(String line) {
        int pos1 = line.indexOf(" ");
        if (pos1 > 0) {
          String first = line.substring(0, pos1);
          if (HttpMethod.is(first)) {
            return true;
          }
        }
        // URI must start with scheme or /
        return line.startsWith("/") || line.indexOf(":") >= 3;
      }

      static RequestLine of(String line) {
        int pos1 = line.indexOf(" ");
        HttpMethod verb = HttpMethod.GET;
        String rest = line;
        String version = null;
        if (pos1 < 0) {
          pos1 = rest.length();
        } else {
          String first = line.substring(0, pos1);
          rest = line.substring(pos1 + 1).trim();
          if (HttpMethod.is(first)) {
            verb = HttpMethod.valueOf(first);
            pos1 = rest.indexOf(" ");
          } else {
            rest = line;
          }
          if (pos1 > 0) {
            version = rest.substring(pos1 + 1).trim();
          } else {
            pos1 = rest.length();
          }
        }
        return new RequestLine(verb, rest.substring(0, pos1), version);
      }
    }

    record HeaderLine(String name, String value) implements Token {

      static boolean matches(String line) {
        int pos = line.indexOf(":");
        return pos > 0 && line.indexOf(":", pos + 1) < 0;
      }

      static HeaderLine of(String line) {
        int pos = line.indexOf(":");
        return new HeaderLine(line.substring(0, pos).trim(), line.substring(pos + 1).trim());
      }
    }

    record ResourceRefLine(String path) implements Token {

      static boolean matches(String line) {
        return line.startsWith("< ");
      }

      static ResourceRefLine of(String line) {
        int pos = line.indexOf(" ");
        return new ResourceRefLine(line.substring(pos + 1).trim());
      }
    }
  }

  class Builder {
    List<Variable> fileVariables = new ArrayList<>();
    List<Request> requests = new ArrayList<>();
    List<Property> properties;
    RequestLine requestLine;
    List<Header> headers;
    Body body;

    public void acceptRequest() {
      Request request = new Request(
          properties,
          requestLine.verb(),
          HttpFile.StringTemplate.of(requestLine.target()),
          HttpFile.StringTemplate.of(requestLine.version()),
          headers,
          body
      );
      requests.add(request);
      properties = null;
      requestLine = null;
      headers = null;
      body = null;
    }
  }

  record Next(String line, State state) {
  }

  sealed interface State {
    Next next(String line, Builder builder);

    record RequestSeparator(boolean expectSeparator) implements State {
      @Override
      public Next next(String line, Builder builder) {
        if (Token.matchesEnd(line)) {
          return null;
        } else if (Token.matchesBlank(line)) {
          return new Next(null, this);
        } else if (Token.matchesRequestSeparator(line)) {
          return new Next(null, new RequestSeparator(false));
        }
        if (expectSeparator) {
          throw new IllegalStateException("Expected RequestSeparator, was '" + line + "'");
        } else {
          return new Next(line, new RequestFeature());
        }
      }
    }

    record RequestOrSeparator() implements State {
      @Override
      public Next next(String line, Builder builder) {
        if (Token.matchesBlank(line)) {
          return new Next(null, this);
        } else if (Token.matchesRequestSeparator(line)) {
          return new Next(null, new RequestFeature());
        }
        return new Next(line, new RequestFeature());
      }
    }

    record RequestFeature(List<PropertyLine> properties) implements State {
      public RequestFeature() {
        this(new ArrayList<>());
      }

      @Override
      public Next next(String line, Builder builder) {
        if (Token.matchesBlank(line)) {
          return new Next(null, this);
        } else if (Token.PropertyLine.matches(line)) {
          properties.add(Token.PropertyLine.of(line));
          return new Next(null, this);
        } else if (Token.VariableLine.matches(line)) {
          var variableLine = Token.VariableLine.of(line);
          builder.fileVariables.add(
              new Variable(variableLine.name(), HttpFile.StringTemplate.of(variableLine.value())));
          return new Next(null, this);
        } else if (Token.matchesComment(line)) {
          return new Next(null, this);
        }
        builder.properties = properties.stream()
            .map(propLine -> new HttpFile.Property(propLine.name(), propLine.value())).toList();
        return new Next(line, new RequestLine());
      }
    }

    record RequestLine() implements State {
      @Override
      public Next next(String line, Builder builder) {
        if (Token.RequestLine.matches(line)) {
          builder.requestLine = Token.RequestLine.of(line);
          return new Next(null, new HeaderLines());
        }
        throw new IllegalStateException("Expected RequestLine, was '" + line + "'");
      }
    }

    record HeaderLines(List<HeaderLine> headers, HeaderLine current) implements State {
      public HeaderLines() {
        this(new ArrayList<>(), null);
      }

      @Override
      public Next next(String line, Builder builder) {
        if (Token.matchesEnd(line) || Token.matchesBlank(line)) {
          // fall through
        } else if (Token.ContinuationLine.matches(line)) {
          if (current == null) {
            throw new IllegalStateException("No current HeaderLine for ContinuationLine");
          }
          return new Next(null, new HeaderLines(headers,
              new HeaderLine(current.name(), current.value() + line.trim())));
        } else if (Token.HeaderLine.matches(line)) {
          if (current != null) {
            headers.add(current);
          }
          return new Next(null, new HeaderLines(headers, Token.HeaderLine.of(line)));
        }
        if (current != null) {
          headers.add(current);
        }
        builder.headers = headers.stream().map(headerLine -> new Header(
            HttpFile.StringTemplate.of(headerLine.name()),
            HttpFile.StringTemplate.of(headerLine.value()
          ))).toList();
        return new Next(null, new BodyLines(new ArrayList<>(), new StringBuilder()));
      }
    }

    record BodyLines(List<Part> allParts, StringBuilder bodyLines) implements State {
      private void consumeBodyLines() {
        if (bodyLines != null && !bodyLines.isEmpty()) {
          allParts.addAll(HttpFile.StringTemplate.of(bodyLines.toString()).parts());
          bodyLines.setLength(0);
        }
      }

      @Override
      public Next next(String line, Builder builder) {
        if (Token.matchesEnd(line) || Token.matchesBlank(line)) {
          consumeBodyLines();
          if (!allParts.isEmpty()) {
            builder.body = new Body(null, new HttpFile.StringTemplate(allParts));
          }
          builder.acceptRequest();
          return new Next(null, new RequestSeparator(true));
        } else if (Token.matchesRequestSeparator(line) && allParts.isEmpty()
            && bodyLines.isEmpty()) {
          builder.acceptRequest();
          return new Next(line, new RequestSeparator(true));
        } else if (Token.ResourceRefLine.matches(line)) {
          consumeBodyLines();
          var resource = HttpFile.StringTemplate.of(Token.ResourceRefLine.of(line).path());
          allParts.add(new Part.ResourceRef(resource));
          return new Next(null, this);
        } else {
          if (bodyLines.length() > 0) {
            bodyLines.append("\n");
          }
          bodyLines.append(line);
          return new Next(null, this);
        }
      }
    }
  }

  /**
   * Parses the given lines into a {@link Model}.
   *
   * @param lines the lines to parse
   * @return the resulting model
   */
  public Model parse(Iterator<String> lines) {
    Builder builder = new Builder();
    Next next = new Next(null, new State.RequestOrSeparator());
    while (true) {
      String line = next.line();
      if (line == null && lines.hasNext()) {
        line = lines.next();
      }
      // System.out.println(next + ": " + line);
      next = next.state().next(line, builder);
      if (next == null) {
        break;
      }
    }
    return new Model(builder.fileVariables, builder.requests);
  }

  /**
   * Parses the given input into a {@link Model}.
   *
   * @param input the input to parse
   * @return the resulting model
   */
  public Model parse(String input) {
    return parse(List.of(input.split("\n")).iterator());
  }

  /**
   * Parses the given input into a {@link Model}.
   *
   * @param input the input to parse
   * @return the resulting model
   */
  public Model parse(InputStream input) throws IOException {
    return parse(new String(input.readAllBytes()));
  }

  private static String sample = """
      @baseUrl = https://httpbin.org/post

      POST {{baseUrl}}?q=hello
      Accept: application/json

      """;

  /**
   * Main method for testing.
   *
   * @param args the command line arguments
   */
  public static void main(String[] args) {
    HttpFileParser parser = new HttpFileParser();
    System.out.println(parser.parse(List.of(sample.split("\n")).iterator()));
  }
}
