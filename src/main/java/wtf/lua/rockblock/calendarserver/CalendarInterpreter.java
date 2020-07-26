package wtf.lua.rockblock.calendarserver;

import java.util.*;

import org.slf4j.*;

import biweekly.*;
import biweekly.component.*;
import biweekly.property.*;
import biweekly.util.*;

/**
 * CalendarInterpreter provides the interpret() function which will convert an ICalendar object from Biweekly into an array of CalendarEvents
 */
public final class CalendarInterpreter {
	private static final Logger log = LoggerFactory.getLogger(CalendarInterpreter.class);

	private CalendarInterpreter() {}

	private static String getString(ValuedProperty<String> prop) {
		if (prop == null) return null;
		return prop.getValue();
	}

	private static Long getDateTimestamp(Date date) {
		if (date == null) return null;
		return date.getTime();
	}
	private static Long getTimestamp(ValuedProperty<Date> prop) {
		if (prop == null) return null;
		return getDateTimestamp(prop.getValue());
	}
	private static Long getTimestampICalDate(ValuedProperty<ICalDate> prop) {
		if (prop == null) return null;
		return getDateTimestamp(prop.getValue());
	}

	private static Long getDuration(DurationProperty prop) {
		if (prop == null) return null;

		var duration = prop.getValue();
		if (duration == null) return null;

		return duration.toMillis();
	}

	private static String[] getCategories(List<Categories> prop) {
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

	private static CalendarEvent interpretEvent(VEvent vevent) {
		var uid = getString(vevent.getUid());
		var url = getString(vevent.getUrl());

		var created = getTimestamp(vevent.getCreated());
		var lastModified = getTimestamp(vevent.getLastModified());

		var start = getTimestampICalDate(vevent.getDateStart());
		var end = getTimestampICalDate(vevent.getDateEnd());
		var duration = getDuration(vevent.getDuration());

		var summary = getString(vevent.getSummary());
		var description = getString(vevent.getDescription());
		var categories = getCategories(vevent.getCategories());

		var location = getString(vevent.getLocation());

		return new CalendarEvent(
			uid, url,
			created, lastModified,
			start, end, duration,
			summary, description, categories,
			location
		);
	}

	/**
	 * Generate an array of CalendarEvents from an input ICalendar
	 * @param cal ICalendar object that contains VEvents
	 * @return Array of parsed CalendarEvents
	 */
	public static CalendarEvent[] interpret(ICalendar cal) {
		var product = cal.getProductId();
		log.info("Interpreting calendar \"{}\"", product != null ? product.getValue() : "unknown");

		var vevents = cal.getEvents(); var veventsSize = vevents.size();
		var events = new CalendarEvent[veventsSize];

		for (int i = 0; i < veventsSize; i++) {
			events[i] = interpretEvent(vevents.get(i));
		}

		log.info("Generated {} new events", veventsSize);

		return events;
	}
}
