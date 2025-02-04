package no.ngu.httpfile.data;

import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * {@link DataTraverser} implementation for traversing HTTP requests, headers and responses.
 */
public class HttpDataTraverser implements DataTraverser {

  @Override
  public boolean traverses(Object data, String step) {
    return data instanceof HttpRequest || data instanceof HttpResponse
        || data instanceof HttpHeaders
        || (data instanceof String && step.equals("*"));
  }

  @Override
  public Object traverse(Object data, String step) throws IllegalArgumentException {
    return switch (data) {
      case HttpRequest request -> switch (step) {
        case "uri" -> request.uri();
        case "headers" -> request.headers();
        case "body" -> request.bodyPublisher().orElse(null);
        default -> DataTraverser.throwIllegalStep(data, step);
      };
      case HttpResponse<?> response -> switch (step) {
        case "uri" -> response.uri();
        case "statusCode" -> response.statusCode();
        case "body" -> response.body();
        case "headers" -> response.headers();
        default -> DataTraverser.throwIllegalStep(data, step);
      };
      case HttpHeaders headers -> {
        var values = headers.map().get(step);
        if (values != null && values.size() == 1) {
          yield values.get(0);
        }
        yield values;
      }
      case String string -> switch (step) {
        case "*" -> string;
        default -> DataTraverser.throwIllegalStep(data, step);
      };
      default -> DataTraverser.throwIllegalStep(data, step);
    };
  }
}
