package no.ngu.httpfile;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import no.ngu.httpfile.HttpFile.StringTemplate.Part;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link StringTemplateResolver}.
 */
public class StringTemplateResolverTest {

  private HttpFileParser parser;
  private InputStreamProvider inputStreamProvider;

  /**
   * Sets up the parser and input stream provider.
   */
  @BeforeEach
  public void setupParser() {
    this.parser = new HttpFileParser();
    this.inputStreamProvider = new InputStreamProvider.Default();
  }

  @Test
  public void testVariableRequestLine() {
    var model = parser.parse("""
        @section=sport
        GET http://vg.no/{{section}}
        """);
    assertEquals(1, model.requests().size());

    var stringTemplateResolver = new StringTemplateResolver();
    stringTemplateResolver.setInputStreamProvider(inputStreamProvider);
    var stringValueProvider =
        new StringValueProvider.Variables(model.fileVariables(), stringTemplateResolver);
    stringTemplateResolver.setStringValueProvider(stringValueProvider);
    stringTemplateResolver.resolve(model);

    assertEquals(new HttpFile.Model(List.of(new HttpFile.Variable("section", "sport")),
        new HttpFile.Request(List.of(), HttpFile.HttpMethod.GET,
            new HttpFile.StringTemplate(new Part.Constant("http://vg.no/"),
                new Part.Constant("sport")),
            null, List.of(), null)),
        model);
  }
}
