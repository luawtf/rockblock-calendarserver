package wtf.lua.rockblock.calendarserver;

import java.io.*;
import java.net.*;
import java.net.http.*;
import java.net.http.HttpClient.*;
import java.net.http.HttpResponse.*;
import java.time.*;
import java.util.concurrent.*;

/**
 * Downloader provides methods for downloading resources (iCalendar data) over the internet via HTTP
 */
public final class Downloader {
	/** User-Agent header sent with every HTTP request, set from config.templateUserAgent */
	public final String userAgent;
	/** Time to wait before timing out and declaring a request dead if connection/download takes too long */
	public final Duration timeoutDuration;

	private final HttpClient client;

	/**
	 * Create a new Downloader instance using configuration values from the passed config
	 * @param config Config object to get userAgent / timeoutDuration from
	 */
	public Downloader(Config config) {
		userAgent = config.formatUserAgent(App.getVersion());
		timeoutDuration = Duration.ofMillis(config.downloadTimeout);

		client = HttpClient.newBuilder()
			.version(Version.HTTP_1_1)
			.followRedirects(Redirect.NORMAL)
			.connectTimeout(timeoutDuration)
			.build();
	}

	/**
	 * Download a resource from a URL over HTTP and get it's body as an InputStream
	 * @param url URL that points to the resource to download
	 * @return CompletableFuture that results in the InputStream with the content of the resource
	 */
	public CompletableFuture<InputStream> download(String url) {
		HttpRequest request = HttpRequest.newBuilder()
			.GET()
			.uri(URI.create(url))
			.header("User-Agent", userAgent)
			.timeout(timeoutDuration)
			.build();

		return client.sendAsync(request, BodyHandlers.ofInputStream())
			.thenApply(HttpResponse::body);
	}
}
