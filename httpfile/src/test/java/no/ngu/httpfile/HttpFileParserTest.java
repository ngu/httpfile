package no.ngu.httpfile;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import no.ngu.httpfile.HttpFile.StringTemplate.Part;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link HttpFileParser}.
 */
public class HttpFileParserTest {

  private HttpFileParser parser;

  /**
   * Sets up the parser.
   */
  @BeforeEach
  public void setupParser() {
    this.parser = new HttpFileParser();
  }

  @Test
  public void testRequestLine() {
    assertEquals(
        new HttpFile.Model(List.of(),
            new HttpFile.Request(HttpFile.HttpMethod.GET, "http://vg.no/", List.of(), null)),
        parser.parse("""
            GET http://vg.no/
            """));
  }

  @Test
  public void testRequestLines() {
    assertEquals(
        new HttpFile.Model(List.of(),
            new HttpFile.Request(HttpFile.HttpMethod.GET, "http://vg.no/", List.of(), null),
            new HttpFile.Request(HttpFile.HttpMethod.GET, "http://yr.no/", List.of(), null)),
        parser.parse("""
            GET http://vg.no/

            ###
            GET http://yr.no/
            """));
  }

  @Test
  public void testVariableRequestLine1() {
    assertEquals(new HttpFile.Model(List.of(new HttpFile.Variable("section", "sport")),
        new HttpFile.Request(List.of(), HttpFile.HttpMethod.GET,
            new HttpFile.StringTemplate(new Part.Constant("http://vg.no/"),
                new Part.VariableRef("section"), new Part.Constant("/"),
                new Part.MacroCall(Macro.guid)),
            null, List.of(), null)),
        parser.parse("""
            @section=sport
            GET http://vg.no/{{section}}/{{$guid}}
            """));
  }

  @Test
  public void testVariableRequestLine2() {
    assertEquals(
        new HttpFile.Model(List.of(
            new HttpFile.Variable("section", "sport"),
            new HttpFile.Variable("url",
                new HttpFile.StringTemplate(new Part.Constant("http://vg.no/"),
                    new Part.VariableRef("section"), new Part.Constant("/"),
                    new Part.MacroCall(Macro.guid))
            )),
            new HttpFile.Request(List.of(), HttpFile.HttpMethod.GET,
                new HttpFile.StringTemplate(new Part.VariableRef("url")),
                null, List.of(), null)
        ),
        parser.parse("""
            @section=sport
            @url=http://vg.no/{{section}}/{{$guid}}
            GET {{url}}
            """));
  }

  @Test
  public void testVariableRequestLineHeaders() {
    assertEquals(
        new HttpFile.Model(List.of(
            new HttpFile.Variable("x-header", "X-Header"),
            new HttpFile.Variable("x-value", "X-Value")
        ), new HttpFile.Request(List.of(), HttpFile.HttpMethod.POST,
            new HttpFile.StringTemplate(new Part.Constant("http://vg.no/")), null,
            List.of(new HttpFile.Header(
                new HttpFile.StringTemplate(new Part.VariableRef("x-header")),
                new HttpFile.StringTemplate(new Part.VariableRef("x-value")))
            ),
            null
        )),
        parser.parse("""
            @x-header=X-Header
            @x-value=X-Value
            POST http://vg.no/
            {{x-header}}: {{x-value}}

            """));
  }

  @Test
  public void testRequestLineHeadersBody() {
    assertEquals(
        new HttpFile.Model(List.of(), new HttpFile.Request(List.of(), HttpFile.HttpMethod.POST,
            new HttpFile.StringTemplate(new Part.Constant("http://vg.no/")), null,
            List.of(new HttpFile.Header("Content-Type", "text/plain")),
            new HttpFile.Body(null,
                new HttpFile.StringTemplate(new Part.Constant("Here's some content"))))),
        parser.parse("""
            POST http://vg.no/
            Content-Type: text/plain

            Here's some content
            """));
  }

  @Test
  public void testResourceBody1() {
    assertEquals(
        new HttpFile.Model(List.of(), new HttpFile.Request(List.of(), HttpFile.HttpMethod.POST,
            new HttpFile.StringTemplate(new Part.Constant("http://vg.no/")), null,
            List.of(new HttpFile.Header("Content-Type", "text/plain")),
            new HttpFile.Body(null,
                new HttpFile.StringTemplate(new Part.ResourceRef("content.txt"))))),
        parser.parse("""
            POST http://vg.no/
            Content-Type: text/plain

            < content.txt

            """));
  }

  @Test
  public void testResourceBody2() {
    assertEquals(
        new HttpFile.Model(List.of(new HttpFile.Variable("file", "content.txt")),
            new HttpFile.Request(List.of(), HttpFile.HttpMethod.POST,
                new HttpFile.StringTemplate(new Part.Constant("http://vg.no/")), null,
                List.of(new HttpFile.Header("Content-Type", "text/plain")),
                new HttpFile.Body(null,
                    new HttpFile.StringTemplate(new Part.ResourceRef(
                        new HttpFile.StringTemplate(new Part.VariableRef("file"))))
            ))
        ),
        parser.parse("""
            @file=content.txt
            POST http://vg.no/
            Content-Type: text/plain

            < {{file}}

            """));
  }
}
