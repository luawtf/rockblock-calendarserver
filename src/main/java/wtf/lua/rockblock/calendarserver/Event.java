package wtf.lua.rockblock.calendarserver;

/**
 * Event is a POJO that represents a calendar event
 */
public final class Event {
	/** Has this event been flagged as (possibly) being part of the timetable? */
	public boolean timetable;

	/** Unique identifier, possibly null */
	public String uid;
	/** URL link, possibly null */
	public String url;

	/** Creation timestamp (milliseconds since 1970), possibly null */
	public Long created;
	/** Last modified timestamp (milliseconds since 1970), possibly null */
	public Long lastModified;

	/** Event start timestamp (milliseconds since 1970), possibly null */
	public Long start;
	/** Event end timestamp (milliseconds since 1970), possibly null */
	public Long end;
	/** Event duration time (milliseconds), possibly null */
	public Long duration;

	/** Short summary string, possibly null */
	public String summary;
	/** Full description string, possibly null */
	public String description;

	/** List of categories/tags given to this event, not null and does not contain null elements */
	public String[] categories;

	/** Location description string, possibly null */
	public String location;

	/**
	 * Create a new Event instance
	 * @see Event Event class for parameter descriptions
	 */
	public Event(
		boolean timetable,
		String uid, String url,
		Long created, Long lastModified,
		Long start, Long end, Long duration,
		String summary, String description,
		String[] categories,
		String location
	) {
		this.timetable = timetable;
		this.uid = uid;
		this.url = url;
		this.created = created;
		this.lastModified = lastModified;
		this.start = start;
		this.end = end;
		this.duration = duration;
		this.summary = summary;
		this.description = description;
		this.categories = categories;
		this.location = location;
	}
}
