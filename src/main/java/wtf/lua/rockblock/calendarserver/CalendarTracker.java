package wtf.lua.rockblock.calendarserver;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;

final class CalendarData {
	public CompletableFuture<String> jsonResult;

	public final long creationTime;

	public CalendarData(CompletableFuture<String> jsonResult, long creationTime) {
		this.jsonResult = jsonResult;
		this.creationTime = creationTime;
	}
}

public final class CalendarTracker {
	/** Config instance that provides URL generation */
	public final Config config;

	/** Downloader instance which is used to download and parse iCalendar files */
	public final CalendarDownloader downloader;

	/** ObjectMapper instance which is used to JSON-ify the resulting CalendarEvent array */
	public final ObjectMapper objectMapper;

	/** Time-to-live of CalendarData in cache (ms) */
	public final long timeToLive;

	/** Map of MonthExpression values to CalendarData instances, used as the cache */
	public final Map<String, CalendarData> calendarJSONResultMap;

	public CalendarTracker(Config config, CalendarDownloader downloader) {
		this.config = config;
		this.downloader = downloader;

		objectMapper = new ObjectMapper();

		timeToLive = config.apiCacheTTL;

		calendarJSONResultMap = new HashMap<String, CalendarData>();
	}

	public CompletableFuture<String> getCalendarJSON(MonthExpression month) {
		var monthValue = month.value;

		var currentTime = Instant.now().toEpochMilli();

		if (calendarJSONResultMap.containsKey(monthValue)) {
			var lastResult = calendarJSONResultMap.get(monthValue);

			if (currentTime - lastResult.creationTime > timeToLive) {
				calendarJSONResultMap.remove(monthValue);
			} else {
				return lastResult.jsonResult;
			}
		}

		var data = new CalendarData(null, currentTime);

		var url = config.formatURL(monthValue);
		CompletableFuture<String> jsonResult = downloader
			.downloadCalendar(url)
			.thenApply(CalendarInterpreter::interpret)
			.thenApply((events) -> {
				try { return objectMapper.writeValueAsString(events); }
				catch (JsonProcessingException e) {
					throw new CompletionException(e);
				}
			})
			.handle((json, e) -> {
				if (e != null) {
					calendarJSONResultMap.values().remove(data);
					throw new CompletionException(e);
				} else return json;
			});

		data.jsonResult = jsonResult;
		calendarJSONResultMap.put(monthValue, data);

		return jsonResult;
	}
}
