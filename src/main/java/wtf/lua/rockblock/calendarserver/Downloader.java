package wtf.lua.rockblock.calendarserver;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Downloader is used to download files/resources from the internet over HTTP or HTTPS.
 * CalendarServer uses it to download the iCalendar .ICS files from whatever server the user specifies.
 *
 * <p>
 * Copyright (C) 2020 Lua MacDougall
 * <br/><br/>
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <br/><br/>
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 * <br/><br/>
 * You should have received a copy of the GNU General Public License along with
 * this program.  If not, see &lt;https://www.gnu.org/licenses/&gt;.
 * </p>
 *
 * @author Lua MacDougall &lt;luawhat@gmail.com&gt;
 */
public final class Downloader {
  private static final Logger log = LoggerFactory.getLogger(Downloader.class);

  /** HTTP User-Agent header sent with every request. */
  public static final String userAgent = String.format(
    "RockBlock-CalendarServer / %s (https://github.com/luawtf/rockblock-calendarserver)",
    Application.getVersion()
  );

  private final Executor executor;

  private final HttpClient httpClient;

  /**
   * Create a new Downloader instance.
   * @param executor Executor instance to use for executing request tasks.
   * @param connectTimeout How long (in milliseconds) to wait before timing out when connecting to a server.
   */
  public Downloader(Executor executor, long connectTimeout) {
    this.executor = executor;
    httpClient = HttpClient
      .newBuilder()
      .version(Version.HTTP_1_1)
      .followRedirects(Redirect.NORMAL)
      .executor(executor)
      .connectTimeout(Duration.ofMillis(connectTimeout))
      .build();
  }

  /**
   * Download a resource from the internet over HTTP (or HTTPS) with the GET method.
   * @param uri URI/URL pointing to the resource to download.
   * @param retrieveTimeout How long (in milliseconds) to wait before timing out while downloading the resource.
   * @return CompletableFuture that completes with an InputStream containing the requested resource.
   */
  public CompletableFuture<InputStream> download(URI uri, long retrieveTimeout) {
    var httpRequest = HttpRequest
      .newBuilder()
      .GET()
      .uri(uri)
      .header("User-Agent", userAgent)
      .timeout(Duration.ofMillis(retrieveTimeout))
      .build();

    log.info("Download started for {}", uri);

    return httpClient
      .sendAsync(httpRequest, BodyHandlers.ofInputStream())
      .thenApplyAsync(response -> {
        var status = response.statusCode();
        if (status / 100 != 2) {
          log.error("Download failed (non-2XX status code) for {}", uri);
          throw new CompletionException(new BadStatusException(String.format(
            "Recieved error status code %d", status
          )));
        } else {
          log.info("Download completed for {}", uri);
          return response.body();
        }
      }, executor);
  }
}
