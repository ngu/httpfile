package no.ngu.httpfile;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HttpTestClient implements AutoCloseable {

  private InputStreamProvider inputStreamProvider;
  private HttpClient httpClient;

  public HttpTestClient() {
    this.inputStreamProvider = new InputStreamProvider.Default();
    var builder = HttpClient.newBuilder();
    this.httpClient = builder.build();
  }

  @Override
  public void close() throws Exception {
    if (httpClient != null && !httpClient.isTerminated()) {
      try {
        httpClient.close();
      } finally {
        httpClient = null;
      }
    }
  }

  public Map<String, Object> performRequests(HttpFile.Model model, List<String> requestNames) {
    Map<String, Object> results = new HashMap<>();
    var stringTemplateResolver = new StringTemplateResolver();
    stringTemplateResolver.setInputStreamProvider(inputStreamProvider);
    var fileVariableValuesProvider = new StringValueProvider.Variables(model.fileVariables(), 
        stringTemplateResolver);
    for (var request : model.requests()) {
      StringValueProvider stringValueProvider = new StringValueProvider.Providers(
          fileVariableValuesProvider,
          new StringValueProvider.MapEntries(results));
      stringTemplateResolver.setStringValueProvider(stringValueProvider);
      try {
        var requestName = request.getRequestPropertyValue("name");
        if (requestNames == null || requestNames.contains(requestName.orElse(null))) {
          var result = performRequest(request, stringTemplateResolver);
          if (requestName.isPresent()) {
            results.put(requestName.get(), result);
          }
        }
      } catch (Exception ex) {
        System.err.println("Aborting, due to exception when performing %s %s"
            .formatted(request.method(), request.target()));
        break;
      }
    }
    return results;
  }

  public Map<String, Object> performRequests(HttpFile.Model model, String... requestNames) {
    return performRequests(model, List.of(requestNames));
  }

  private Map<String, Object> performRequest(HttpFile.Request request,
      StringTemplateResolver templateResolver) {
    var builder = HttpRequest.newBuilder(URI.create(templateResolver.toString(request.target())));
    for (var header : request.headers()) {
      builder.header(header.name(), templateResolver.toString(header.value()));
    }
    builder.method(request.method().name(),
        BodyPublishers.ofString(templateResolver.toString(request.body().content())));
    var httpRequest = builder.build();

    var requestMap = Map.of("uri", httpRequest.uri(), "headers", httpRequest.headers().map());
    try {
      HttpResponse<String> httpResponse = httpClient.send(httpRequest, BodyHandlers.ofString());
      var responseMap = Map.of("status", httpResponse.statusCode(), "headers",
          httpResponse.headers(), "body", httpResponse.body());
      return Map.of("request", requestMap, "response", responseMap);
    } catch (IOException | InterruptedException ex) {
      throw new RuntimeException(ex);
    }
  }

  private static String sample = """
      @host=www.vg.no
      GET https://{{host}}/
      Accept: text/html

      """;

  public static void main(String[] args) {
    HttpFileParser parser = new HttpFileParser();
    var requests = parser.parse(List.of(sample.split("\n")).iterator());
    try (var testClient = new HttpTestClient()) {
      System.out.println(requests.fileVariables());
      testClient.performRequests(requests);
    } catch (Exception ioe) {
      System.err.println(ioe);
    }
  }
}
