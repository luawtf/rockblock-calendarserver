package wtf.lua.rockblock.calendarserver;

/**
 * Event represents an immutable calendar event generated from input iCalendar data.
 * Note that any of Event's fields could possibly be "null" (except {@link Event#hidden} and {@link Event#categories}).
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
public final class Event {
  /**
   * Should this event be hidden / seperate from other events?
   * This field will be "true" if {@link Event#summary summary} matches the event hiding regular expression.
   */
  public final boolean hidden;

  /** VEVENT UID field, may be "null" if not present. */
  public final String uid;
  /** VEVENT URL field, may be "null" if not present. */
  public final String url;

  /** VEVENT CREATED field as a milliseconds-since-1970 (UTC) timestamp. May be "null" if not present. */
  public final Long created;
  /**
   * VEVENT LAST-MODIFIED field as a milliseconds-since-1970 (UTC) timestamp.
   * May be "null" if not present.
   */
  public final Long modified;

  /** VEVENT DTSTART field as a milliseconds-since-1970 (UTC) timestamp. May be "null" if not present. */
  public final Long start;
  /** VEVENT DTEND field as a milliseconds-since-1970 (UTC) timestamp. May be "null" if not present. */
  public final Long end;
  /** Difference between {@link Event#start start} and {@link Event#end end} in milliseconds. May be "null" if not present. */
  public final Long duration;

  /** VEVENT SUMMARY field, may be "null" if not present. */
  public final String summary;
  /** VEVENT DESCRIPTION field, may be "null" if not present. */
  public final String description;

  /** VEVENT CATEGORIES field as an array of category string. Will never be "null". */
  public final String[] categories;

  /** VEVENT LOCATION field, may be "null" if not present. */
  public final String location;

  /**
   * Create a new Event instance.
   * @param hidden      {@link Event#hidden}
   * @param uid         {@link Event#uid}
   * @param url         {@link Event#url}
   * @param created     {@link Event#created}
   * @param modified    {@link Event#modified}
   * @param start       {@link Event#start}
   * @param end         {@link Event#end}
   * @param duration    {@link Event#duration}
   * @param summary     {@link Event#summary}
   * @param description {@link Event#description}
   * @param categories  {@link Event#categories}
   * @param location    {@link Event#location}
   */
  public Event(
    boolean  hidden,
    String   uid,
    String   url,
    Long     created,
    Long     modified,
    Long     start,
    Long     end,
    Long     duration,
    String   summary,
    String   description,
    String[] categories,
    String   location
  ) {
    this.hidden      = hidden;
    this.uid         = uid;
    this.url         = url;
    this.created     = created;
    this.modified    = modified;
    this.start       = start;
    this.end         = end;
    this.duration    = duration;
    this.summary     = summary;
    this.description = description;
    this.categories  = categories;
    this.location    = location;
  }
}
