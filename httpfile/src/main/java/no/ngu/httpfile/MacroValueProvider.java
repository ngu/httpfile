package no.ngu.httpfile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.function.Function;

public class MacroValueProvider {

  private InputStreamProvider inputStreamProvider;

  private Properties envProps = null;

  public MacroValueProvider(InputStreamProvider inputStreamProvider) {
    this.inputStreamProvider = inputStreamProvider;
  }

  private Properties getEnvProps() {
    if (envProps == null) {
      envProps = new Properties();
      try (var inputStream = inputStreamProvider.getInputStream(".env")) {
        envProps.load(inputStream);
      } catch (IOException e) {
        // ignore
      }
    }
    return envProps;
  }

  public String applyMacro(Macro macro, String... args) {
    return applyMacro(macro, List.of(args));
  }

  public String applyMacro(Macro macro, List<String> args) {
    return switch (macro) {
      // {{$guid}}
      case guid -> UUID.randomUUID().toString();
      // {{$randomInt min max}}
      case randomInt -> {
        int min = Integer.parseInt(args.get(0));
        int max = Integer.parseInt(args.get(1));
        yield Integer.toString(min + (int) (Math.random() * (max - min)));
      }
      // {{$timestamp [offset option]}}
      case timestamp -> {
        int offset = arg(0, args, Integer::parseInt, 0);
        TemporalUnit unit = getTemporalUnit(arg(1, args, "s"));
        var datetime = LocalDateTime.now().plus(offset, unit);
        var timestamp = datetime.toEpochSecond(ZoneOffset.ofTotalSeconds(0));
        yield String.valueOf(timestamp);
      }
      // {{$datetime rfc1123|iso8601 [offset option]}}
      case datetime -> {
        DateTimeFormatter formatter = getDateTimeFormatter(arg(0, args), DateTimeFormatter.ISO_DATE_TIME);
        int offset = arg(1, args, Integer::parseInt, 0);
        TemporalUnit unit = getTemporalUnit(arg(2, args, "s"));
        var datetime = ZonedDateTime.now().plus(offset, unit);
        yield formatter.format(datetime.withNano(0));
      }
      // {{$localDatetime rfc1123|iso8601 [offset option]}}
      case localDatetime -> {
        DateTimeFormatter formatter = getDateTimeFormatter(arg(0, args), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        int offset = arg(1, args, Integer::parseInt, 0);
        TemporalUnit unit = getTemporalUnit(arg(2, args, "s"));
        var datetime = LocalDateTime.now().plus(offset, unit);
        yield formatter.format(datetime.withNano(0));
      }
      // {{$processEnv [%]envVarName}}
      case processEnv -> {
        var envVarName = args.get(0);
        if (envVarName.startsWith("%")) {
          envVarName = envVarName.substring(1);
        }
        var envVarValue = System.getenv(envVarName);
        yield envVarValue != null ? envVarValue : envVarName;
      }
      // {{$dotenv [%]variableName}}
      case dotenv -> {
        var envVarName = args.get(0);
        if (envVarName.startsWith("%")) {
          envVarName = envVarName.substring(1);
        }
        yield getEnvProps().getProperty(envVarName, envVarName);
      }
    };
  }

  private static String arg(int n, List<String> args, String def) {
    return args.size() > n ? args.get(n) : def;
  }
  private static String arg(int n, List<String> args) {
    return arg(n, args, null);
  }

  private static <T> T arg(int n, List<String> args, Function<String, T> mapper, T def) {
    return args.size() > n ? mapper.apply(args.get(n)) : def;
  }

  private static DateTimeFormatter getDateTimeFormatter(String format, DateTimeFormatter isoFormatter) {
    return switch (format) {
      case null -> isoFormatter;
      case "rfc1123" -> DateTimeFormatter.RFC_1123_DATE_TIME;
      case "iso8601" -> isoFormatter;
      default -> DateTimeFormatter.ofPattern(format);
    };
  }

  private static TemporalUnit getTemporalUnit(String unit) {
    return switch (unit) {
      case null -> ChronoUnit.SECONDS;
      case "s" -> ChronoUnit.SECONDS;
      case "m" -> ChronoUnit.MINUTES;
      case "h" -> ChronoUnit.HOURS;
      case "d" -> ChronoUnit.DAYS;
      case "w" -> ChronoUnit.WEEKS;
      case "M" -> ChronoUnit.MONTHS;
      case "ms" -> ChronoUnit.MILLIS;
      default -> throw new IllegalArgumentException("Unknown temporal unit: " + unit);
    };
  }
}
