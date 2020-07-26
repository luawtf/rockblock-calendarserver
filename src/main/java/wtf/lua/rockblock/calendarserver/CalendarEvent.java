package wtf.lua.rockblock.calendarserver;

/**
 * CalendarEvent is a POJO that represents an event object returned by CalendarServer
 */
public final class CalendarEvent {
	/** Event's unique identifier */
	public String uid;
	/** Event's URL */
	public String url;

	/** When this event was created (ms since 1970) */
	public Long created;
	/** When this event was last modified (ms since 1970) */
	public Long lastModified;

	/** When this event starts (ms since 1970) */
	public Long start;
	/** When this event ends (ms since 1970) */
	public Long end;
	/** Event's duration */
	public Long duration;

	/** Event's summary string */
	public String summary;
	/** Event's full description */
	public String description;
	/** List of keywords/tags given to this event */
	public String[] categories;

	/** Custom location string that this event is tagged with */
	public String location;

	/**
	 * Instantiate a new CalendarEvent, see CalendarEvent.java for parameter descriptions
	 */
	public CalendarEvent(
		String uid, String url,
		Long created, Long lastModified,
		Long start, Long end, Long duration,
		String summary, String description, String[] categories,
		String location
	) {
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
