package wtf.lua.rockblock.calendarserver;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CalendarJsonProvider is the core of this application.
 * Using {@link Downloader} and {@link Deserializer}, this class provides methods to automatically download, deserialize, and JSON-ify calendar data.
 * All results from CalendarJsonProvider are cached and cache settings + resource download location is provided by the {@link Config}.
 * Additionally, all public methods of this class are entirely thread safe, call from anywhere at any time!
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
public final class CalendarJsonProvider {
  private static final Logger log = LoggerFactory.getLogger(CalendarJsonProvider.class);

  /**
   * CacheEntry represents an entry in "cacheMap" that will resolve with a calendar month data JSON body.
   */
  private static interface CacheEntry {
    /**
     * Is this entry still valid?
     * @return Boolean indicating if this CacheEntry is valid or should be regenerated.
     */
    public boolean isValid();
    /**
     * Get the body of this entry.
     * @return CompletableFuture that completes with a calendar month data JSON body (as a byte array).
     */
    public CompletableFuture<byte[]> getBody();
  }
  /**
   * PendingCacheEntry represents a pending operation to generate a calendar month data JSON body.
   */
  private static final class PendingCacheEntry implements CacheEntry {
    private final CompletableFuture<byte[]> promise;

    /**
     * Create a new PendingCacheEntry instance.
     * @param promise CompletableFuture that completes with a calendar month data JSON body (as a byte array).
     */
    public PendingCacheEntry(CompletableFuture<byte[]> promise) {
      this.promise = promise;
    }

    @Override
    public boolean isValid() {
      return true;
    }

    @Override
    public CompletableFuture<byte[]> getBody() {
      return promise;
    }
  }
  /**
   * CompletedCacheEntry represents a cached calendar month data JSON body that will eventually expire.
   */
  private static final class CompletedCacheEntry implements CacheEntry {
    private final long expires;
    private final byte[] body;

    /**
     * Create a new CompletedCacheEntry instance.
     * @param body Calendar month data JSON body (as a byte array).
     * @param ttl Time in milliseconds that this cache entry will be valid for.
     */
    public CompletedCacheEntry(byte[] body, long ttl) {
      this.body = body;
      expires = System.currentTimeMillis() + ttl;
    }

    @Override
    public boolean isValid() {
      return expires >= System.currentTimeMillis();
    }

    @Override
    public CompletableFuture<byte[]> getBody() {
      return CompletableFuture.completedFuture(body);
    }
  }
  /**
   * UpdatingCacheEntry is a container that contains a cache entry that will be replaced and the new replacement cache entry.
   * It is used to implement non-blocking cache updates, until the new entry has completed, the old entry's body will be returned.
   */
  private static final class UpdatingCacheEntry implements CacheEntry {
    private final CacheEntry oldEntry, newEntry;

    /**
     * Create a new UpdatingCacheEntry instance.
     * @param oldEntry The original cache entry that is being replaced.
     * @param newEntry The new cache entry.
     */
    public UpdatingCacheEntry(CacheEntry oldEntry, CacheEntry newEntry) {
      this.oldEntry = oldEntry;
      this.newEntry = newEntry;
    }

    @Override
    public boolean isValid() {
      return oldEntry.isValid() || newEntry.isValid();
    }

    @Override
    public CompletableFuture<byte[]> getBody() {
      var newBody = newEntry.getBody();
      return newBody.isDone() ? newBody : oldEntry.getBody();
    }
  }

  // Cache + cache's lock
  private final Map<Month, CacheEntry> cacheMap;
  private final ReadWriteLock cacheLock;

  // ObjectMapper for turning Event[] -> JSON byte[]
  private final ObjectMapper objectMapper;

  private final Executor executor;
  private final Config config;

  /** Downloader instance used to download calendar data. */
  public final Downloader downloader;
  /** Deserializer instance used to deserialize calendar data. */
  public final Deserializer deserializer;

  /**
   * Create a new CalendarJsonProvider instance.
   * @param executor Executor instance to run all calendar request/update tasks on.
   * @param config Application configuration.
   */
  public CalendarJsonProvider(Executor executor, Config config) {
    this.executor = executor;
    this.config = config;

    downloader = new Downloader(executor, config.downloadConnectTimeout);
    deserializer = new Deserializer(executor, config.hiddenRegex);

    objectMapper = new ObjectMapper();

    cacheMap = new HashMap<>();
    cacheLock = new ReentrantReadWriteLock();
  }

  /**
   * Retrieve JSON data for a month from the cache, redownloading and regenerating it if it is out-of-date or has yet to be cached.
   * @param month Month to retrieve.
   * @return CompletableFuture that completes with a calendar month data JSON body (as a byte array).
   */
  public CompletableFuture<byte[]> request(Month month) {
    CacheEntry entry;

    // Safely read entry from the cache
    cacheLock.readLock().lock(); try {
      entry = cacheMap.get(month);
    } finally { cacheLock.readLock().unlock(); }

    if (entry != null && entry.isValid()) {
      // Cached value is valid! Return it.
      return entry.getBody();
    } else {
      // No value in cache, or cached value is invalid, run update to generate a new one.
      log.info("Cache miss for {}", month);
      return update(month);
    }
  }

  /**
   * Attempt to redownload and regenerate the JSON data for a month.
   * If an update operation is already in progress then the CompletableFuture for that operation is returned instead.
   * @param month Month to update.
   * @return CompletableFuture that completes with a calendar month data JSON body (as a byte array).
   */
  public CompletableFuture<byte[]> update(Month month) {
    // Lock the cache exclusively for us while we work
    cacheLock.writeLock().lock(); try {

      // Check if there is already an pending update
      var oldEntry = cacheMap.get(month);
      if (
        oldEntry instanceof PendingCacheEntry ||
        oldEntry instanceof UpdatingCacheEntry
      ) {
        // Return the pending update promise
        return oldEntry.getBody();
      }

      log.info("Update for {} started", month);

      // We're good to start working! Begin generating the body
      var promise = generateBody(month);

      // Once we're done generating the body ...
      promise.handleAsync((body, error) -> {
        // Lock the cache exclusively again
        cacheLock.writeLock().lock(); try {
          // And update the cache entry to either "null" (if generateBody failed) or a CompletedCacheEntry instance
          if (promise.isCompletedExceptionally()) {
            log.error("Update for {} failed", month, error);
            cacheMap.remove(month);
          } else {
            log.info("Update for {} completed", month);
            cacheMap.put(month, new CompletedCacheEntry(body, config.cacheTTL));
          }
        } finally { cacheLock.writeLock().unlock(); }
        return null;
      }, executor);

      // Create new cache entries
      var newEntry = new PendingCacheEntry(promise);
      // If we are creating a new cache entry, then use the PendingCacheEntry directly
      // However, if we are replacing an existing cache entry then use UpdatingCacheEntry
      var updatingEntry =
        oldEntry != null
          ? new UpdatingCacheEntry(oldEntry, newEntry)
          : newEntry;

      // Insert the new entry into the cache
      cacheMap.put(month, updatingEntry);

      // Return the updated entry's body
      return updatingEntry.getBody();
    } finally { cacheLock.writeLock().unlock(); }
  }

  private CompletableFuture<byte[]> generateBody(Month month) {
    return CompletableFuture
      // Download the iCalendar data
      .supplyAsync(() -> {
        var url = config.urlTemplate.replace("$$", month.expression);
        var uri = URI.create(url);
        return downloader.download(uri, config.downloadRetrieveTimeout);
      }, executor)
      .thenComposeAsync(v -> v, executor)
      // Deserialize the data
      .thenComposeAsync(deserializer::deserialize, executor)
      // Convert the data to JSON and return it
      .thenApplyAsync(events -> {
        try {
          return objectMapper.writeValueAsBytes(events);
        } catch (JsonProcessingException error) {
          throw new CompletionException(error);
        }
      }, executor);
  }
}
