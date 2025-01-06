package no.ngu.httpfile.client;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import no.ngu.httpfile.HttpFile;
import no.ngu.httpfile.InputStreamProvider;
import no.ngu.httpfile.StringTemplateResolver;
import no.ngu.httpfile.StringValueProvider;
import no.ngu.httpfile.StringValueProvider.Properties;
import no.ngu.httpfile.data.CollectionDataTraverser;
import no.ngu.httpfile.data.DataTraverser;
import no.ngu.httpfile.data.HttpDataTraverser;
import no.ngu.httpfile.data.JsonbDataTraverser;

/**
 * A client for performing HTTP requests as specified in an {@link HttpFile.Model}.
 */
public class HttpFileClient implements AutoCloseable {

  private InputStreamProvider inputStreamProvider;
  private HttpClient httpClient;

  /**
   * Initializes with the provided {@link InputStreamProvider}.
   */
  public HttpFileClient(InputStreamProvider inputStreamProvider) {
    this.inputStreamProvider = inputStreamProvider;
    var builder = HttpClient.newBuilder();
    this.httpClient = builder.build();
  }

  /**
   * Initializes with the default {@link InputStreamProvider}.
   */
  public HttpFileClient() {
    this(new InputStreamProvider.Default());
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

  /**
   * The data traversers used by this client.
   */
  public final Iterable<DataTraverser> dataTraversers = List.of(
      new CollectionDataTraverser(),
      new JsonbDataTraverser(),
      new HttpDataTraverser()
  );

  /**
   * Performs the requests in the provided {@link HttpFile.Model}.
   *
   * @param model the model containing the requests to perform
   * @param variableOverrides variable overrides
   * @param requestTransform a function returning the actual request to perform, or null to skip
   * @param resultConsumer a consumer for processing or valildating the result after each request
   * @return a map of the results, with the request name as key
   */
  public Map<String, Object> performRequests(
      HttpFile.Model model,
      Properties variableOverrides,
      BiFunction<HttpFile.Request, String, HttpFile.Request> requestTransform,
      BiConsumer<HttpFile.Request, Map<String, Object>> resultConsumer
  ) {
    Map<String, Object> results = new HashMap<>();
    var stringTemplateResolver = new StringTemplateResolver();
    stringTemplateResolver.setInputStreamProvider(inputStreamProvider);
    var fileVariableValuesProvider = new StringValueProvider.Variables(model.fileVariables(), 
        stringTemplateResolver);
    StringValueProvider stringValueProvider = new StringValueProvider.Providers(
        variableOverrides,
        fileVariableValuesProvider,
        new StringValueProvider.Traversable(results, dataTraversers)
    );
    for (var request : model.requests()) {
      stringTemplateResolver.setStringValueProvider(stringValueProvider);
      try {
        var requestName = request.getRequestPropertyValue("name");
        var actualRequest = request;
        try {
          actualRequest = requestTransform.apply(request, requestName.orElse(null));
        } catch (Exception ex) {
          break;
        }
        if (actualRequest != null) {
          var result = performRequest(actualRequest, stringTemplateResolver);
          if (requestName.isPresent()) {
            results.put(requestName.get(), result);
          }
          if (resultConsumer != null) {
            resultConsumer.accept(actualRequest, result);
          }
        }
        System.err.println("Results, after performing\n%s %s:\n%s"
            .formatted(request.method(), stringTemplateResolver.toString(request.target()),
                results));
      } catch (Exception ex) {
        System.err.println("Aborting, due to exception when performing\n%s %s:\n%s"
            .formatted(request.method(), request.target(), ex));
        break;
      }
    }
    return results;
  }

  /**
   * Performs the requests in the provided {@link HttpFile.Model}.
   *
   * @param model the model containing the requests to perform
   * @param variableOverrides variable overrides
   * @param requestTransform a function returning the actual request to perform, or null to skip
   * @param resultConsumer a consumer for processing or valildating the result after each request
   * @return a map of the results, with the request name as key
   */
  public Map<String, Object> performRequests(
      HttpFile.Model model,
      Map<String, String> variableOverrides,
      BiFunction<HttpFile.Request, String, HttpFile.Request> requestTransform,
      BiConsumer<HttpFile.Request, Map<String, Object>> resultConsumer
  ) {
    return performRequests(model, StringValueProvider.Properties.of(variableOverrides),
        requestTransform, resultConsumer);
  }

  /**
   * Performs the requests in the provided {@link HttpFile.Model}.
   *
   * @param model the model containing the requests to perform
   * @param variableOverrides variable overrides
   * @param requestNames the names of the requests to perform, or empty to perform all
   * @param resultConsumer a consumer for processing or valildating the result after each request
   * @return a map of the results, with the request name as key
   */
  public Map<String, Object> performRequests(
      HttpFile.Model model,
      Map<String, String> variableOverrides,
      List<String> requestNames,
      BiConsumer<HttpFile.Request, Map<String, Object>> resultConsumer
  ) {
    return performRequests(
        model,
        variableOverrides,
        (request, name) -> requestNames.isEmpty() || requestNames.contains(name) ? request : null,
        resultConsumer
    );
  }

  /**
   * Performs the requests in the provided {@link HttpFile.Model}.
   *
   * @param model the model containing the requests to perform
   * @param requestNames the names of the requests to perform, or empty to perform all
   * @param resultConsumer a consumer for processing or valildating the result after each request
   * @return a map of the results, with the request name as key
   */
  public Map<String, Object> performRequests(
      HttpFile.Model model,
      List<String> requestNames,
      BiConsumer<HttpFile.Request, Map<String, Object>> resultConsumer
  ) {
    return performRequests(
        model,
        Map.of(),
        (request, name) -> requestNames.isEmpty() || requestNames.contains(name) ? request : null,
        resultConsumer
    );
  }

  /**
   * Performs the requests in the provided {@link HttpFile.Model}.
   *
   * @param model the model containing the requests to perform
   * @param requestNames the names of the requests to perform, or empty to perform all
   * @return a map of the results, with the request name as key
   */
  public Map<String, Object> performRequests(HttpFile.Model model, String... requestNames) {
    return performRequests(model, List.of(requestNames), null);
  }

  private Map<String, Object> performRequest(HttpFile.Request request,
      StringTemplateResolver templateResolver) {
    var builder = HttpRequest.newBuilder(URI.create(templateResolver.toString(request.target())));
    if (request.version() != null) {
      var versionString = templateResolver.toString(request.version());
      // turn HTTP/1.1 into HTTP_1_1 and HTTP/2 into HTTP_2
      builder.version(Version.valueOf(versionString.replaceAll("\\W", "_")));
    }
    for (var header : request.headers()) {
      builder.header(
          templateResolver.toString(header.name()),
          templateResolver.toString(header.value())
      );
    }
    var bodyContent = (request.body() != null
        ? templateResolver.toString(request.body().content())
        : "");
    builder.method(request.method().name(), BodyPublishers.ofString(bodyContent));
    var httpRequest = builder.build();

    try {
      HttpResponse<String> httpResponse = httpClient.send(httpRequest, BodyHandlers.ofString());
      return Map.of("request", httpRequest, "response", httpResponse);
    } catch (IOException | InterruptedException ex) {
      throw new RuntimeException(ex);
    }
  }

  /**
   * Traverses the provided data using the client's data traversers.
   *
   * @param data the data to traverse
   * @param path the path to traverse
   * @return the result of the traversal
   */
  public Object traversePath(Object data, String path) {
    return DataTraverser.traversePath(data, path, dataTraversers);
  }
}
