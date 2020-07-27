package wtf.lua.rockblock.calendarserver;

import org.apache.commons.cli.*;
import org.slf4j.*;

/**
 * LongName provides an enumeration of all long-name option strings
 */
enum LongName {
	/** Long name for option --help */
	HELP("help"),
	/** Long name for option --version */
	VERSION("version"),

	/** Long name for option --port */
	API_PORT("api-port"),
	/** Long name for option --cache */
	API_CACHE("api-cache"),

	/** Long name for option --calculate-duration */
	INTERPRET_CALCULATE_DURATION("calculate-duration"),
	/** Long name for option --timetable-regex */
	INTERPRET_TIMETABLE_REGEX("timetable-regex"),

	/** Long name for option --download-timeout */
	DOWNLOAD_TIMEOUT("download-timeout"),

	/** Long name for option --template-url */
	TEMPLATE_URL("template-url"),
	/** Long name for option --template-agent */
	TEMPLATE_AGENT("template-agent");

	/** Entry's string value */
	private final String value;

	/**
	 * Create a new LongName instance
	 * @param value String value, used as the long name for this option
	 */
	private LongName(String value) {
		this.value = value;
	}

	/**
	 * Get this entry's value
	 * @return String value of this enumeration entry
	 */
	public String toString() {
		return value;
	}
}

/**
 * Config provides various application configuration settings and command-line parsing tools for setting these options from the command line
 */
public final class Config {
	private static final Logger log = LoggerFactory.getLogger(Config.class);

	/** Port number to listen on */
	public int apiPort = 2000;
	/** API cache time-to-live (in milliseconds) */
	public long apiCacheTTL = 3600000; // 30 minutes

	/** Automatically fill in Event.duration? */
	public boolean interpretCalculateDuration = true;
	/** Regular expression used for matching and flagging events as timetable events (leave empty for no flagging) */
	public String interpretTimetableRegex = "[12](\s*-\s*|\s+)[1234]{4}[xX]?";

	/** Timeout before retrying a download */
	public long downloadTimeout = 30000; // 30 seconds

	/** URL template to download iCalendar files from, '$' will be replaced with the requested month (ex "2020-12") */
	public String templateURL = "https://westvancouverschools.ca/rockridge-secondary/events/$/?ical=1";
	/** UserAgent template, sent as the User-Agent header when downloading iCalendar files, '$' will be replaced with the current CalendarServer version */
	public String templateUserAgent = "RockBlock-CalendarServer/$ (https://github.com/luawtf/rockblock-calendarserver)";

	/**
	 * Format templateURL with a month expression string
	 * @param val Month expression string to format template with
	 * @return Formatted URL string with provided month
	 */
	public String formatURL(String val) {
		return templateURL.replace("$", val);
	}
	/**
	 * Format templateUserAgent with the app's version
	 * @param val Version string to format template with
	 * @return Formatted User-Agent with provided version
	 */
	public String formatUserAgent(String val) { return templateUserAgent.replace("$", val); }

	/**
	 * Create a new Config instance with default field values
	 */
	public Config() {}

	/**
	 * Create a new Config instance using command-line arguments
	 * NOTE: This will call System.exit(0) if --help/--version are passed
	 * @param args Array of argument strings passed to this program
	 */
	public Config(String[] args) {
		try {
			Options opts = makeOptions();
			CommandLineParser parser = new DefaultParser();
			CommandLine cmd = parser.parse(opts, args);

			// Handle --help and --version
			if (getBoolean(cmd, LongName.HELP, false))
				printHelp(opts);
			if (getBoolean(cmd, LongName.VERSION, false))
				printVersion();

			// Update all config fields
			apiPort				= getInt(cmd,		LongName.API_PORT,			apiPort);
			apiCacheTTL			= getLong(cmd,		LongName.API_CACHE,			apiCacheTTL);
			interpretCalculateDuration 	= getBoolean(cmd, 	LongName.INTERPRET_CALCULATE_DURATION,	interpretCalculateDuration);
			interpretTimetableRegex		= getString(cmd,	LongName.INTERPRET_TIMETABLE_REGEX,	interpretTimetableRegex);
			downloadTimeout			= getLong(cmd,		LongName.DOWNLOAD_TIMEOUT,		downloadTimeout);
			templateURL			= getString(cmd,	LongName.TEMPLATE_URL,			templateURL);
			templateUserAgent		= getString(cmd,	LongName.TEMPLATE_AGENT,		templateUserAgent);
		} catch (ParseException e) {
			log.warn("Failed to parse command-line arguments:", e);
		}
	}

	private void addOption(Options opts, String shortName, LongName longName, String description) {
		opts.addOption(new Option(shortName, longName.toString(), true, description));
	}
	private Options makeOptions() {
		Options opts = new Options();
		addOption(opts, "h", LongName.HELP,				"display program help page");
		addOption(opts, "v", LongName.VERSION,				"display program version");
		addOption(opts, "p", LongName.API_PORT,				"port number to listen on");
		addOption(opts, "c", LongName.API_CACHE,			"api cache time-to-live (ms)");
		addOption(opts, "d", LongName.INTERPRET_CALCULATE_DURATION,	"automatically fill in duration field of events");
		addOption(opts, "r", LongName.INTERPRET_TIMETABLE_REGEX,	"regex that matches timetable events (can be blank)");
		addOption(opts, "t", LongName.DOWNLOAD_TIMEOUT,			"timeout before retrying a download (ms)");
		addOption(opts, "u", LongName.TEMPLATE_URL,			"template URL for downloading iCalendar data");
		addOption(opts, "a", LongName.TEMPLATE_AGENT,			"template for the UserAgent header");
		return opts;
	}

	private String getVersion() {
		return String.format("%s v%s", App.getTitle(), App.getVersion());
	}
	private void printVersion() {
		System.out.println(getVersion());
		System.exit(0);
	}
	private void printHelp(Options opts) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.setOptionComparator(null);
		formatter.printHelp(
			App.getTitle(),
			"Parses events from an iCalendar source and serves them via HTTP as JSON objects",
			opts,
			getVersion(),
			false
		);

		System.exit(0);
	}

	private boolean getBoolean(CommandLine cmd, LongName optionName, boolean defaultValue) {
		return cmd.hasOption(optionName.toString()) || defaultValue;
	}
	private String getString(CommandLine cmd, LongName optionName, String defaultValue) {
		if (!getBoolean(cmd, optionName, false)) return defaultValue;

		String value = cmd.getOptionValue(optionName.toString());
		if (value == null || value.isEmpty()) return defaultValue;

		return value;
	}
	private long getLong(CommandLine cmd, LongName optionName, long defaultValue) {
		String value = getString(cmd, optionName, "");
		try { return Long.parseLong(value); }
		catch (NumberFormatException e) { return defaultValue; }
	}
	private int getInt(CommandLine cmd, LongName optionName, int defaultValue) {
		return (int)getLong(cmd, optionName, (long)defaultValue);
	}
}
