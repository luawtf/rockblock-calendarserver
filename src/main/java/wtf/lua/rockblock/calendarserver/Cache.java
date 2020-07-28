package wtf.lua.rockblock.calendarserver;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;

import org.slf4j.*;

/**
 * CacheEntry represents an entry in the cacheMap of a Cache instance
 */
final class CacheEntry {
	/** Future JSON string result */
	public CompletableFuture<String> result;
	/** When this entry was created and added */
	public long creationTime;

	/**
	 * Create a new CacheEntry instance
	 * @param result CompletableFuture that results in the calendar JSON
	 * @param creationTime Current time
	 */
	public CacheEntry(CompletableFuture<String> result, long creationTime) {
		this.result = result;
		this.creationTime = creationTime;
	}
}

/**
 * Cache provides a wrapper around Downloader and Interpreter that adds caching and simplifies downloading calendars
 */
public final class Cache {
	private static final Logger log = LoggerFactory.getLogger(Cache.class);

	/** Time-to-live of a cache entry in milliseconds */
	public final long timeToLive;

	/** Configuration that is used for URL generation and passed to internal downloader/interpreter instances */
	public final Config config;

	/** Downloader instance used for downloading iCalendar data */
	public final Downloader downloader;
	/** Interpreter instance used for generating resulting JSON from iCalendar data */
	public final Interpreter interpreter;

	private final Map<String, CacheEntry> cacheMap;

	/**
	 * Create a new Cache instance using the passed configuration
	 * @param config Passed to the Downloader and Interpreter, also used for config.apiCacheTTL
	 */
	public Cache(Config config) {
		this.config = config;

		timeToLive = config.apiCacheTTL;

		downloader = new Downloader(config);
		interpreter = new Interpreter(config);

		cacheMap = new HashMap<String, CacheEntry>();
	}

	/**
	 * Get the JSON data for a month from the cache or by downloading and interpreting it
	 * @param month Month to retrieve
	 * @return Future that results in the JSON string
	 */
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
