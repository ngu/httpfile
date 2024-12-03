package no.ngu.httpfile;

import java.io.FileInputStream;
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

public enum Functions implements Function<List<String>, String> {

  // {{$guid}}
  guid {
    @Override
    public String apply(List<String> args) {
      return UUID.randomUUID().toString();
    }
  },
  // {{$randomInt min max}}
  randomInt {
    @Override
    public String apply(List<String> args) {
      int min = Integer.parseInt(args.get(0));
      int max = Integer.parseInt(args.get(1));
      return Integer.toString(min + (int) (Math.random() * (max - min)));
    }
  },
  // {{$timestamp [offset option]}}
  timestamp {
    @Override
    public String apply(List<String> args) {
      int offset = args.size() >= 1 ? Integer.parseInt(args.get(0)) : 0;
      TemporalUnit unit = getTemporalUnit(args.size() >= 3 ? args.get(2) : null);
      var datetime = LocalDateTime.now().plus(offset, unit);
      var timestamp = datetime.toEpochSecond(ZoneOffset.ofTotalSeconds(0));
      return String.valueOf(timestamp);
    }
  },
  // {{$datetime rfc1123|iso8601 [offset option]}}
  datetime {
    @Override
    public String apply(List<String> args) {
      DateTimeFormatter formatter = getDateTimeFormatter(args.size() >= 1 ? args.get(0) : null);
      int offset = args.size() >= 2 ? Integer.parseInt(args.get(1)) : 0;
      TemporalUnit unit = getTemporalUnit(args.size() >= 3 ? args.get(2) : null);
      var datetime = ZonedDateTime.now().plus(offset, unit);
      return formatter.format(datetime.withNano(0));
    }
  },
  // {{$localDatetime rfc1123|iso8601 [offset option]}}
  localDatetime {
    @Override
    public String apply(List<String> args) {
      DateTimeFormatter formatter = getDateTimeFormatter(args.size() >= 1 ? args.get(0) : null);
      int offset = args.size() >= 2 ? Integer.parseInt(args.get(1)) : 0;
      TemporalUnit unit = getTemporalUnit(args.size() >= 3 ? args.get(2) : null);
      var datetime = LocalDateTime.now().plus(offset, unit);
      return formatter.format(datetime.withNano(0));
    }
  },
  // {{$processEnv [%]envVarName}}
  processEnv {
    @Override
    public String apply(List<String> args) {
      var envVarName = args.get(0);
      if (envVarName.startsWith("%")) {
        envVarName = envVarName.substring(1);
      }
      var envVarValue = System.getenv(envVarName);
      return envVarValue != null ? envVarValue : "";
    }
  },
  // {{$dotenv [%]variableName}}
  dotenv {
    @Override
    public String apply(List<String> args) {
      var envProps = new Properties();
      try {
        envProps.load(new FileInputStream(".env"));
      } catch (IOException e) {
        // ignore
      }
      var envVarName = args.get(0);
      if (envVarName.startsWith("%")) {
        envVarName = envVarName.substring(1);
      }
      var envVarValue = envProps.getProperty(envVarName);
      return envVarValue != null ? envVarValue : "";
    }
  }
  // {{$aadToken [new] [public|cn|de|us|ppe] [<domain|tenantId>] [aud:<domain|tenantId>]}}
  // NYI
  ;

  private static DateTimeFormatter getDateTimeFormatter(String format) {
    return switch (format) {
      case null -> DateTimeFormatter.ISO_DATE_TIME;
      case "rfc1123" -> DateTimeFormatter.RFC_1123_DATE_TIME;
      case "iso8601" -> DateTimeFormatter.ISO_DATE_TIME;
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
