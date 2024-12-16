package no.ngu.httpfile;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;

/**
 * Provides input streams for resources.
 */
public interface InputStreamProvider {

  /**
   * Gets an input stream for the given resource.
   *
   * @param resource the resource
   * @return the input stream, or null if not found
   */
  public InputStream getInputStream(String resource);

  /**
   * Provides input streams for URIs.
   */
  public record Uri(URI baseUri) implements InputStreamProvider {

    /**
     * Gets an input stream for the given resource.
     *
     * @param baseUri the base URI
     * @param resource the resource
     * @return the input stream, or null if not found
     */
    public static InputStream getInputStream(URI baseUri, String resource) {
      try {
        return (baseUri != null ? baseUri.resolve(resource) : URI.create(resource)).toURL()
            .openStream();
      } catch (IOException ex) {
        return null;
      }
    }

    @Override
    public InputStream getInputStream(String resource) {
      return getInputStream(baseUri, resource);
    }
  }

  /**
   * Provides input streams for resources in the classpath.
   */
  public record Resource(Class<?> context, String basePath) implements InputStreamProvider {

    /**
     * Convenience constructor for resources in the classpath.
     *
     * @param context the class context
     */
    public Resource(Class<?> context) {
      this(context, null);
    }

    /**
     * Gets an input stream for the given resource.
     *
     * @param context the class context
     * @param resource the resource
     * @return the input stream, or null if not found
     */
    public static InputStream getInputStream(Class<?> context, String basePath, String resource) {
      if (basePath != null) {
        resource = Path.of(basePath).resolve(resource).toString();
      }
      return context.getResourceAsStream(resource);
    }

    @Override
    public InputStream getInputStream(String resource) {
      return getInputStream(context, basePath, resource);
    }
  }

  /**
   * Provides input streams for files.
   */
  public record File(Path basePath) implements InputStreamProvider {

    /**
     * Gets an input stream for the given file.
     *
     * @param basePath the base path
     * @param file the file
     * @return the input stream, or null if not found
     */
    public static InputStream getInputStream(Path basePath, String file) {
      try {
        return new FileInputStream(basePath.resolve(file).toFile());
      } catch (IOException ex) {
        return null;
      }
    }

    @Override
    public InputStream getInputStream(String resource) {
      try {
        return new FileInputStream(basePath.resolve(resource).toFile());
      } catch (FileNotFoundException e) {
        return null;
      }
    }
  }

  /**
   * The default input stream provider,
   * supporting URIs, classpath resources, and files.
   */
  public class Default implements InputStreamProvider {

    @Override
    public InputStream getInputStream(String resource) {
      if (resource.indexOf(':') >= 4) {
        return Uri.getInputStream(null, resource);
      } else if (resource.startsWith("/")) {
        return Resource.getInputStream(getClass(), null, resource);
      } else {
        return File.getInputStream(Path.of("./"), resource);
      }
    }
  }
}
