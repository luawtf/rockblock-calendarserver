package wtf.lua.rockblock.calendarserver;

import java.io.*;
import java.net.*;
import java.net.http.*;
import java.net.http.HttpClient.*;
import java.net.http.HttpResponse.*;
import java.time.*;
import java.util.concurrent.*;

import org.slf4j.*;

import biweekly.*;

/**
 * CalendarDownloader provides functions for downloading iCalendar data (using Biweekly) from the internet
 */
public final class CalendarDownloader {
	private static final Logger log = LoggerFactory.getLogger(CalendarDownloader.class);

	/** Config instance that provides userAgent and timeout */
	public final Config config;

	/** UserAgent string to use when making requests */
	public final String userAgent;

	/** Request timeout in milliseconds, used for connect and download */
	public final long timeout;
	/** Timeout as a duration */
	public final Duration timeoutDuration;

	/** The HttpClient instance used by this downloader */
	public final HttpClient client;

	/**
	 * Instantiate a new CalendarDownloader using settings from a Config
	 * @param config Config that provides this CalendarDownloader's userAgent and timeout
	 */
	public CalendarDownloader(Config config) {
		this.config = config;

		userAgent = config.formatUserAgent(App.getVersion());

		timeout = config.downloadTimeout;
		timeoutDuration = Duration.ofMillis(timeout);

		client = HttpClient.newBuilder()
			.version(Version.HTTP_1_1)
			.followRedirects(Redirect.NORMAL)
			.connectTimeout(timeoutDuration)
			.build();
	}

	/**
	 * Download a resource from the internet
	 * @param url URL of resource to GET
	 * @return CompletableFuture that returns an InputStream with data
	 */
	public CompletableFuture<InputStream> downloadStream(String url) {
		log.info("Downloading resource \"{}\"", url);

		HttpRequest request = HttpRequest.newBuilder()
			.GET()
			.uri(URI.create(url))
			.header("User-Agent", userAgent)
			.header("X-RockBlock-CalendarServer", App.getTitle())
			.header("X-RockBlock-CalendarServerVersion", App.getVersion())
			.timeout(timeoutDuration)
			.build();

		return client.sendAsync(request, BodyHandlers.ofInputStream())
			.handle((response, e) -> {
				if (e != null) {
					Throwable cause = e.getCause();
					String causeName = cause != null ? cause.getClass().getName() : e.getClass().getName();

					log.error("Downloading resource \"{}\" failed with {}, throwing", url, causeName);

					throw new CompletionException(e);
				} else {
					log.info("Downloaded resource \"{}\", got status {}", url, response.statusCode());

					return response.body();
				}
			});
	}

	/**
	 * Download a resource from the internet and parse it as iCalendar data
	 * @param url URL of resource to GET
	 * @return CompletableFuture that returns an ICalendar instance
	 */
	public CompletableFuture<ICalendar> downloadCalendar(String url) {
		return downloadStream(url)
			.thenApply((stream) -> {
				log.info("Parsing resource \"{}\" into ICalendar", url);
				try {
					return Biweekly.parse(stream).first();
				} catch (IOException e) {
					throw new CompletionException(e);
				}
			});
	}
}
