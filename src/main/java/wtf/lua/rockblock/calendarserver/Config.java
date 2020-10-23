package wtf.lua.rockblock.calendarserver;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Config represents the application configuration. Instances of Config are
 * usually created by reading the "config.json" file.
 */
public final class Config {
  /** Port number to start the HTTP API server on. */
  public final int port;
  /** Send HTTP Cross-Origin Resource Sharing headers with each response? */
  public final boolean cors;
  /** How long should completed API responses be stored in the cache (in milliseconds)? */
  public final long cacheTTL;
  /** How long to wait before failing with a timeout exception while waiting to connect to the calendar source (in milliseconds)? */
  public final long downloadConnectTimeout;
  /** How long to wait before failing with a timeout exception while waiting for the download from the calendar source to complete (in milliseconds)? */
  public final long downloadRetrieveTimeout;
  /** Template for URL that points to the calendar source. "$$" (double dollar sign) will be replaced with the requested month (YYYY-MM). */
  public final String urlTemplate;
  /** If an event's summary matches this regular expression, it will be marked as hidden. This field can also be "null". */
  public final String hiddenRegex;

  /** Config instance with default values. */
  public static final Config defaultConfig = new Config(
    /* port                    */ 2000,
    /* cors                    */ true,
    /* cacheTTL                */ 1_800_000, // 30 minutes
    /* downloadConnectTimeout  */ 30_000,    // 30 seconds
    /* downloadRetrieveTimeout */ 30_000,    // 30 seconds
    /* urlTemplate             */ "https://demo.theeventscalendar.com/events/$$/?ical=1",
    /* hiddenRegex             */ null
  );

  /**
   * Create a new Config instance.
   * @param port                    {@link Config#port}
   * @param cors                    {@link Config#cors}
   * @param cacheTTL                {@link Config#cacheTTL}
   * @param downloadConnectTimeout  {@link Config#downloadConnectTimeout}
   * @param downloadRetrieveTimeout {@link Config#downloadRetrieveTimeout}
   * @param urlTemplate             {@link Config#urlTemplate}
   * @param hiddenRegex             {@link Config#hiddenRegex}
   */
  public Config(
    int port,
    boolean cors,
    long cacheTTL,
    long downloadConnectTimeout,
    long downloadRetrieveTimeout,
    String urlTemplate,
    String hiddenRegex
  ) {
    this.port = port;
    this.cors = cors;
    this.cacheTTL = cacheTTL;
    this.downloadConnectTimeout = downloadConnectTimeout;
    this.downloadRetrieveTimeout = downloadRetrieveTimeout;
    this.urlTemplate = urlTemplate;
    this.hiddenRegex = hiddenRegex;
  }

  private static final ObjectMapper objectMapper = new ObjectMapper();

  /**
   * Create a new instance of Config using data from a JSON file (usually "config.json").
   * @param path Path to JSON file.
   * @return Config instance.
   * @throws IOException If an error occurs while reading from the file.
   * @throws JsonProcessingException If an error occurs while deserializing the JSON.
   */
  public static Config readConfig(String path) throws IOException, JsonProcessingException {
    var object = objectMapper.readTree(new File(path));

    var object$port                    = object.get("port");
    var object$cors                    = object.get("cors");
    var object$cacheTTL                = object.get("cacheTTL");
    var object$downloadConnectTimeout  = object.get("downloadConnectTimeout");
    var object$downloadRetrieveTimeout = object.get("downloadRetrieveTimeout");
    var object$urlTemplate             = object.get("urlTemplate");
    var object$hiddenRegex             = object.get("hiddenRegex");

    return new Config(
      // "port"
      object$port.isInt()
        ? object$port.asInt()
        : defaultConfig.port,
      // "cors"
      object$cors.isBoolean()
        ? object$cors.asBoolean()
        : defaultConfig.cors,
      // "cacheTTL"
      object$cacheTTL.isLong()
        ? object$cacheTTL.asLong()
        : defaultConfig.cacheTTL,
      // "downloadConnectTimeout"
      object$downloadConnectTimeout.isLong()
        ? object$downloadConnectTimeout.asLong()
        : defaultConfig.downloadConnectTimeout,
      // "downloadRetrieveTimeout"
      object$downloadRetrieveTimeout.isLong()
        ? object$downloadRetrieveTimeout.asLong()
        : defaultConfig.downloadRetrieveTimeout,
      // "urlTemplate"
      object$urlTemplate.isTextual()
        ? object$urlTemplate.asText()
        : defaultConfig.urlTemplate,
      // "hiddenRegex"
      object$hiddenRegex.isTextual()
        ? object$hiddenRegex.asText()
        : object$hiddenRegex.isNull()
          ? null
          : defaultConfig.hiddenRegex
    );
  }
}
