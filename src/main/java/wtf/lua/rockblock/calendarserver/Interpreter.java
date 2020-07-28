package wtf.lua.rockblock.calendarserver;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;

import biweekly.*;
import biweekly.component.*;
import biweekly.property.*;
import biweekly.util.*;

/**
 * Interpreter provides methods for parsing raw iCalendar data (from InputStreams) into JSON strings
 */
public final class Interpreter {
	/** Should event durations be calculated if they are not provided? Set from config.interpretCalculateDuration */
	public final boolean calculateDuration;
	/** Pattern that matches events that are timetable events, possibly null, set from config.interpretTimetableRegex */
	public final Pattern timetablePattern;

	private final ObjectMapper mapper;

	/**
	 * Create a new Interpreter instance using configuration values from the passed config
	 * @param config Config object to get calculateDuration / timetablePattern from
	 */
	public Interpreter(Config config) {
		calculateDuration = config.interpretCalculateDuration;

		if (!config.interpretTimetableRegex.isBlank()) {
			timetablePattern = Pattern.compile(config.interpretTimetableRegex);
		} else timetablePattern = null;

		mapper = new ObjectMapper();
	}

	/**
	 * Parse the data in an InputStream as iCalendar data and generate a JSON representation
	 * @param stream InputStream with iCalendar data
	 * @return Event[] as JSON
	 * @throws IOException If parsing iCalendar data failed or JSON serialization failed
	 */
	public String interpret(InputStream stream) throws IOException {
		var cal = parse(stream);
		var json = serialize(cal);
		return json;
	}

	private ICalendar parse(InputStream stream) throws IOException {
		return Biweekly.parse(stream).first();
	}

	private String serialize(ICalendar cal) throws JsonProcessingException {
		var vevents = cal.getEvents();
		var events = new Event[vevents.size()];

		for (int i = 0; i < events.length; i++) {
			events[i] = serializeEvent(vevents.get(i));
		}

		return mapper.writeValueAsString(events);
	}

	private Event serializeEvent(VEvent vevent) {
		var uid = getString(vevent.getUid());
		var url = getString(vevent.getUrl());

		var created = getTimestamp(vevent.getCreated());
		var lastModified = getTimestamp(vevent.getLastModified());

		var start = getTimestampICalDate(vevent.getDateStart());
		var end = getTimestampICalDate(vevent.getDateEnd());
		var duration = getDuration(vevent.getDuration());

		if (
			calculateDuration &&
			duration == null &&
			start != null &&
			end != null
		) {
			duration = Math.max(end - start, 0);
		}

		var summary = getString(vevent.getSummary());
		var description = getString(vevent.getDescription());

		var categories = getCategories(vevent.getCategories());

		var location = getString(vevent.getLocation());

		var timetable = summary != null &&
			timetablePattern != null &&
			timetablePattern.matcher(summary).find();

		return new Event(
			timetable,
			uid, url,
			created, lastModified,
			start, end, duration,
			summary, description,
			categories,
			location
		);
	}

	private String getString(ValuedProperty<String> prop) {
		if (prop == null) return null;
		return prop.getValue();
	}

	private Long getTimestamp(Date date) {
		if (date == null) return null;
		return date.getTime();
	}
	private Long getTimestamp(ValuedProperty<Date> prop) {
		if (prop == null) return null;
		return getTimestamp(prop.getValue());
	}
	private Long getTimestampICalDate(ValuedProperty<ICalDate> prop) {
		if (prop == null) return null;
		return getTimestamp((Date)prop.getValue());
	}

	private Long getDuration(DurationProperty prop) {
		if (prop == null) return null;

		var duration = prop.getValue();
		if (duration == null) return null;

		return duration.toMillis();
	}

	private String[] getCategories(List<Categories> prop) {
		if (prop == null) return null;

		List<String> strings = new ArrayList<String>();

		for (var category : prop) {
			if (category == null) continue;

			var values = category.getValues();
			for (var value : values) {
				if (value == null) continue;

				strings.add(value);
			}
		}

		var stringsArray = new String[strings.size()];
		return strings.toArray(stringsArray);
	}
}
