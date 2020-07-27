package wtf.lua.rockblock.calendarserver;

import java.io.*;
import java.net.*;
import java.net.http.*;
import java.net.http.HttpClient.*;
import java.net.http.HttpResponse.*;
import java.time.*;
import java.util.concurrent.*;

public final class Downloader {
	public final String userAgent;
	public final Duration timeoutDuration;

	private final HttpClient client;

	public Downloader(Config config) {
		userAgent = config.formatUserAgent(App.getVersion());
		timeoutDuration = Duration.ofMillis(config.downloadTimeout);

		client = HttpClient.newBuilder()
			.version(Version.HTTP_1_1)
			.followRedirects(Redirect.NORMAL)
			.connectTimeout(timeoutDuration)
			.build();
	}

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
