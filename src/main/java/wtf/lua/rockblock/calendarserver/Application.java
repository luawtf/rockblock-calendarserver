package wtf.lua.rockblock.calendarserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link Application} provides the entrypoint for RockBlock CalendarServer that
 * loads the config file and starts the {@link Collater} + {@link Server}. It
 * also provides the {@link getVersion} method to retrieve the current package
 * version.
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
public final class Application {
  private static Logger log = LoggerFactory.getLogger(Application.class);

  /**
   * Application entrypoint function, called by the JVM, don't touch this!
   * @param args Command-line argument string array
   */
  public static void main(String[] args) {
    log.info("Starting RockBlock CalendarServer version v{}...", getVersion());
  }

  /**
   * Retrieve the version string (without "v" prefix) for our package
   * {@link wtf.lua.rockblock.calendarserver}.
   * @return Package's version string
   */
  public static String getVersion() {
    var version = Application.class.getPackage().getImplementationVersion();
    return version != null ? version : "undefined";
  }
}