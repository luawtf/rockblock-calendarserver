![](./logo.png)
# RockBlock CalendarServer
RockBlock CalendarServer is an application that scrapes sites using [The Event Calendar](https://theeventscalendar.com/) WordPress plugin and serves the calendar data as JSON through a HTTP API.
It was originally designed only for use with the RockBlock project (an app I am building for my school), but I'm releasing it under the [GNU/GPL](LICENSE) for use by anyone who needs a nice proxy that makes accessing calendar websites using [The Event Calendar](https://theeventscalendar.com/) via programmatic methods easy.
It's also super fast! My school's website can take up to 20 seconds to generate iCalendar data but with the power of caching and the infrequentness of calendar updates, that time can usually be decreased to maybe half-a-millisecond (unless the cached data expires 😞).

## Installation and Building
RockBlock CalendarServer only requires a Java-11 compatible JRE, or the JDK + Maven if you intend to build from source.

To use RockBlock CalendarServer's precompiled JAR files:
 - Download the latest release from [https://github.com/luawtf/rockblock-calendarserver/releases/latest](https://github.com/luawtf/rockblock-calendarserver/releases/latest)
 - Invoke it using your JRE (ex: `java -jar calendarserver-2.0.java`)

Alternatively, if you're a build-from-source kind of person:
```sh
# Clone the Github repository to your computer
git clone https://github.com/luawtf/rockblock-calendarserver
# Enter the directory
cd rockblock-calendarserver
# Invoke Maven
mvn package
# Grab your JAR and edit the directory
mv target/calendarserver-*.jar ../
cd ../
# Invoke the JAR
java -jar calendarserver-*.jar
```

## Configuration
Configuring RockBlock CalendarServer is easy.
When starting up, RockBlock CalendarServer automatically attempts to load a file named `config.json` from the current working directory.
This JSON file can contain a variety of configuration options, which are described in the TypeScript interface definition below:
```typescript
interface Config {
  /* Port number to start the HTTP API server on. */
  port?: number;
  /* Send HTTP Cross-Origin Resource Sharing headers with each response? */
  cors?: boolean;
  /* How long should completed API responses be stored in the cache (in
     milliseconds)? */
  cacheTTL?: number;
  /* How long to wait before failing with a timeout exception while waiting to
     connect to the calendar source (in milliseconds)? */
  downloadConnectTimeout?: number;
  /* How long to wait before failing with a timeout exception while waiting for
     the download from the calendar source to complete (in milliseconds)? */
  downloadRetrieveTimeout?: number;
  /* Template for URL that points to the calendar source. "$$" (double dollar
     sign) will be replaced with the requested month (YYYY-MM). */
  urlTemplate?: string;
  /* If an event's summary matches this regular expression, it will be marked as
     hidden. This field can also be "null". */
  hiddenRegex?: string | null;
  /* Minimum acceptable year value (inclusive). If "yearMin" or "yearMax" are
     less than 0, all years from 0000 to 9999 are acceptable. */
  yearMin?: number;
  /* Maximum acceptable year value (inclusive). If "yearMin" or "yearMax" are
     less than 0, all years from 0000 to 9999 are acceptable. */
  yearMax?: number;
}
```

The default configuration looks like this (`config.json` will be merged with this object):
```json
{
  "port":                    2000,
  "cors":                    true,
  "cacheTTL":                1800000,
  "downloadConnectTimeout":  30000,
  "downloadRetrieveTimeout": 30000,
  "urlTemplate":             "https://demo.theeventscalendar.com/events/$$/?ical=1",
  "hiddenRegex":             null,
  "yearMin":                 -1,
  "yearMax":                 -1
}
```

## Usage
Once you're up-and-running, the HTTP API will be served on whatever port you specified (or 2000 by default).
You can access the list of events for a month by visiting `http://<your server>/YYYY-MM` where YYYY-MM is the month you wish to retrieve.
Event data is returned in a JSON array, with each entry in the array being an object that implements the following interface:
```typescript
interface Event {
  /* Should this event be hidden / seperate from other events? This field will
     be "true" if "summary" matches the event hiding regular expression
     ("hiddenRegex" in the application configuration). */
  hidden: boolean;
  /* VEVENT UID field, may be "null" if not present. */
  uid: string | null;
  /* VEVENT URL field, may be "null" if not present. */
  url: string | null;
  /* VEVENT CREATED field as a milliseconds-since-1970 (UTC) timestamp. May be
     "null" if not present. */
  created: number | null;
  /* VEVENT LAST-MODIFIED field as a milliseconds-since-1970 (UTC) timestamp.
     May be "null" if not present. */
  modified: number | null;
  /* VEVENT DTSTART field as a milliseconds-since-1970 (UTC) timestamp. May be
     "null" if not present. */
  start: number | null;
  /* VEVENT DTEND field as a milliseconds-since-1970 (UTC) timestamp. May be
     "null" if not present. */
  end: number | null;
  /* Difference between "start" and "end" in milliseconds. May be "null" if
     "start" or "end" are not present. */
  duration: number | null;
  /* VEVENT SUMMARY field, may be "null" if not present. */
  summary: string | null;
  /* VEVENT DESCRIPTION field, may be "null" if not present. */
  description: string | null;
  /* VEVENT CATEGORIES field as an array of category string. Will never be
     "null". */
  categories: string[];
  /* VEVENT LOCATION field, may be "null" if not present. */
  location: string | null;
}
```

## Authors
Made with ❤ by Lua MacDougall ([lua.wtf](https://lua.wtf/))

## License
This project is licensed under [GNU/GPL v3](LICENSE).
More info in the [LICENSE](LICENSE) file.

*"You may copy, distribute and modify the software as long as you track changes/dates in source files. Any modifications to or software including (via compiler) GPL-licensed code must also be made available under the GPL along with build & install instructions."* - [tl;drLegal](https://tldrlegal.com/license/gnu-general-public-license-v3-(gpl-3))
