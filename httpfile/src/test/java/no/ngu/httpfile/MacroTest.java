package no.ngu.httpfile;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link Macro}.
 */
public class MacroTest {
  
  private MacroValueProvider macroValueProvider =
      new MacroValueProvider(new InputStreamProvider.Default());

  @Test
  public void testLocalDatetime() {
    var now = LocalDateTime.now().withNano(0);
    assertEquals(
        DateTimeFormatter.ISO_DATE_TIME.format(now),
        macroValueProvider.applyMacro(Macro.localDatetime, "iso8601")
    );
    assertEquals(
        DateTimeFormatter.ISO_DATE_TIME.format(now.plusSeconds(1)),
        macroValueProvider.applyMacro(Macro.localDatetime, "iso8601", "1")
    );
    assertEquals(
        DateTimeFormatter.ISO_DATE_TIME.format(now.plusMinutes(1)),
        macroValueProvider.applyMacro(Macro.localDatetime, "iso8601", "1", "m")
    );
  }
}
