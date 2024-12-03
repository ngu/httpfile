package no.ngu.httpfile;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.Test;

public class FunctionsTest {
  
  private MacroValueProvider macroValueProvider = new MacroValueProvider(new InputStreamProvider.Default());

  @Test
  public void testLocalDatetime() {
    assertEquals(
      DateTimeFormatter.ISO_DATE_TIME.format(LocalDateTime.now().withNano(0)),
      macroValueProvider.applyMacro(Macro.localDatetime, "iso8601")
    );
    assertEquals(
      DateTimeFormatter.ISO_DATE_TIME.format(LocalDateTime.now().plusSeconds(1).withNano(0)),
      macroValueProvider.applyMacro(Macro.localDatetime, "iso8601", "1")
    );
    assertEquals(
      DateTimeFormatter.ISO_DATE_TIME.format(LocalDateTime.now().plusMinutes(1).withNano(0)),
      macroValueProvider.applyMacro(Macro.localDatetime,"iso8601", "1", "m")
    );
  }
}
