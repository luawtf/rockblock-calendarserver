package wtf.lua.rockblock.calendarserver;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;

import org.slf4j.*;

final class CacheEntry {
	public CompletableFuture<String> result;
	public long creationTime;

	public CacheEntry(CompletableFuture<String> result, long creationTime) {
		this.result = result;
		this.creationTime = creationTime;
	}
}

public final class Cache {
	private static final Logger log = LoggerFactory.getLogger(Cache.class);

	public final long timeToLive;

	public final Config config;

	public final Downloader downloader;
	public final Interpreter interpreter;

	private final Map<String, CacheEntry> cacheMap;

	public Cache(Config config) {
		this.config = config;

		timeToLive = config.apiCacheTTL;

		downloader = new Downloader(config);
		interpreter = new Interpreter(config);

		cacheMap = new HashMap<String, CacheEntry>();
	}

	public CompletableFuture<String> request(Month month) {
		var monthValue = month.value;
		var currentTime = Instant.now().toEpochMilli();

		var lastEntry = cacheMap.get(monthValue);
		if (lastEntry == null) {
			// Do nothing
		} else if (currentTime - lastEntry.creationTime > timeToLive) {
			cacheMap.remove(monthValue);
		} else {
			return lastEntry.result;
		}

		log.info("Cache miss for month {}, downloading...", monthValue);

		var entry = new CacheEntry(null, currentTime);

		var url = config.formatURL(monthValue);
		CompletableFuture<String> result = downloader
			.download(url)
			.handle((stream, downloadException) -> {
				if (downloadException != null) {
					throw wrapThrowable(downloadException);
				} else {
					try {
						return interpreter.interpret(stream);
					} catch (Exception interpretException) {
						throw wrapThrowable(interpretException);
					}
				}
			})
			.handle((json, e) -> {
				if (e != null) {
					log.error("Download of {} failed: {}", monthValue, e.getMessage());
					cacheMap.values().remove(entry);
					throw wrapThrowable(e);
				} else {
					log.info("Download of {} completed, result added to cache", monthValue);
					return json;
				}
			});

		entry.result = result;
		cacheMap.put(monthValue, entry);

		return result;
	}

	private CompletionException wrapThrowable(Throwable e) {
		if (e instanceof CompletionException)
			return (CompletionException)e;
		else
			return new CompletionException(e);
	}
}
