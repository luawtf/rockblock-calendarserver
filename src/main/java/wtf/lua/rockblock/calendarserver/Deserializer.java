package wtf.lua.rockblock.calendarserver;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Pattern;
import java.util.concurrent.Executor;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import biweekly.Biweekly;
import biweekly.ICalendar;
import biweekly.component.VEvent;
import biweekly.property.ValuedProperty;

/**
 * Deserializer is used to deserialize iCalendar data (VCALENDAR objects) to retrieve arrays of {@link Event}s (VEVENT objects).
 * Additionally, it can automatically set the {@link Event#hidden} field by checking if the event summary matches a regular expression.
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
public final class Deserializer {
  private final Executor executor;

  private final Pattern hiddenPattern;

  /**
   * Create a new Deserializer instance.
   * @param executor Executor to execute the long-running blocking deserialization operation with.
   * @param hiddenRegex Optional (can be "null") regular expression that is used to set {@link Event#hidden} if said regular expression matches the event summary.
   */
  public Deserializer(Executor executor, String hiddenRegex) {
    this.executor = executor;

    hiddenPattern =
      hiddenRegex != null && !hiddenRegex.isEmpty()
        ? Pattern.compile(hiddenRegex)
        : null;
  }

  /**
   * Deserialize iCalendar data from an {@link InputStream}.
   * @param input Stream with iCalendar data.
   * @return Array of events present in the first VCALENDAR object.
   */
  public CompletableFuture<Event[]> deserialize(InputStream input) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        return interpret(parse(input));
      } catch (IOException error) {
        throw new CompletionException(error);
      }
    }, executor);
  }

  /**
   * Use Biweekly to parse a stream of iCalendar data, retrieving the first available VCALENDAR object.
   * @param input
   * @return VCALENDAR object as a Biweekly {@link ICalendar}.
   * @throws IOException If reading from the stream fails.
   */
  private ICalendar parse(InputStream input) throws IOException {
    return Biweekly.parse(input).first();
  }

  private Event[] interpret(ICalendar calendar) {
    var vevents = calendar.getEvents();
    var events = new Event[vevents.size()];

    for (int i = 0; i < events.length; i++) {
      events[i] = interpretEvent(vevents.get(i));
    }

    return events;
  }

  private <T> T getValue(ValuedProperty<T> property) {
    return
      property != null
        ? property.getValue()
        : null;
  }
  private <T> Long getValueTimestamp(ValuedProperty<T> property) {
    var date = (Date)getValue(property);
    return
      date != null
        ? date.getTime()
        : null;
  }

  private Event interpretEvent(VEvent vevent) {
    var uid = getValue(vevent.getUid());
    var url = getValue(vevent.getUrl());

    var created  = getValueTimestamp(vevent.getCreated());
    var modified = getValueTimestamp(vevent.getLastModified());

    var start    = getValueTimestamp(vevent.getDateStart());
    var end      = getValueTimestamp(vevent.getDateEnd());
    var duration = start != null && end != null ? Math.max(end - start, 0) : null;

    var summary     = getValue(vevent.getSummary());
    var description = getValue(vevent.getDescription());

    var location = getValue(vevent.getLocation());

    boolean hidden;
    if (hiddenPattern != null && summary != null)
      hidden = hiddenPattern.matcher(summary).matches();
    else
      hidden = false;

    var veventCategoriesList = vevent.getCategories();
    var categoryList = new ArrayList<String>();

    for (var veventCategories : veventCategoriesList) {
      if (veventCategories == null) continue;

      var categoryValues = veventCategories.getValues();
      for (var categoryValue : categoryValues) {
        if (categoryValue == null) continue;
        categoryList.add(categoryValue);
      }
    }

    var categories = categoryList.toArray(new String[categoryList.size()]);

    return new Event(
      hidden,
      uid,
      url,
      created,
      modified,
      start,
      end,
      duration,
      summary,
      description,
      categories,
      location
    );
  }
}
