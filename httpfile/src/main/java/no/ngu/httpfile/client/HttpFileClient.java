package no.ngu.httpfile.client;

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
import no.ngu.httpfile.HttpFile;
import no.ngu.httpfile.HttpFileParser;
import no.ngu.httpfile.InputStreamProvider;
import no.ngu.httpfile.StringTemplateResolver;
import no.ngu.httpfile.StringValueProvider;
import no.ngu.httpfile.data.CollectionDataTraverser;
import no.ngu.httpfile.data.DataTraverser;
import no.ngu.httpfile.data.HttpDataTraverser;
import no.ngu.httpfile.data.JsonbDataTraverser;

public class HttpFileClient implements AutoCloseable {

  private InputStreamProvider inputStreamProvider;
  private HttpClient httpClient;

  public HttpFileClient() {
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

  public final Iterable<DataTraverser> dataTraversers = List.of(
      new CollectionDataTraverser(),
      new JsonbDataTraverser(),
      new HttpDataTraverser()
  );

  public Map<String, Object> performRequests(HttpFile.Model model, List<String> requestNames) {
    Map<String, Object> results = new HashMap<>();
    var stringTemplateResolver = new StringTemplateResolver();
    stringTemplateResolver.setInputStreamProvider(inputStreamProvider);
    var fileVariableValuesProvider = new StringValueProvider.Variables(model.fileVariables(), 
        stringTemplateResolver);
    StringValueProvider stringValueProvider = new StringValueProvider.Providers(
        fileVariableValuesProvider,
        new StringValueProvider.Traversable(results, dataTraversers));
    for (var request : model.requests()) {
      stringTemplateResolver.setStringValueProvider(stringValueProvider);
      try {
        var requestName = request.getRequestPropertyValue("name");
        if (requestNames == null || requestNames.isEmpty() || requestNames.contains(requestName.orElse(null))) {
          var result = performRequest(request, stringTemplateResolver);
          if (requestName.isPresent()) {
            results.put(requestName.get(), result);
          }
        }
        System.err.println("Results, after performing %s %s: %s"
            .formatted(request.method(), request.target(), results));
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

    try {
      HttpResponse<String> httpResponse = httpClient.send(httpRequest, BodyHandlers.ofString());
      return Map.of("request", httpRequest, "response", httpResponse);
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
    var httpFile = parser.parse(List.of(sample.split("\n")).iterator());
    try (var testClient = new HttpFileClient()) {
      System.out.println(httpFile.fileVariables());
      testClient.performRequests(httpFile);
    } catch (Exception ioe) {
      System.err.println(ioe);
    }
  }
}
