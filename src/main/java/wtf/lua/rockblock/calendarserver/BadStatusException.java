package wtf.lua.rockblock.calendarserver;

/**
 * BadStatusException is thrown (inside of the CompletableFuture) when {@link Downloader} recieves a bad HTTP status in the response from the HTTP server (non-2XX status code).
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
public final class BadStatusException extends Exception {
  private static final long serialVersionUID = -1509745833318517920L;

  /**
   * Create a new BadStatusException instance.
   * @param message Error message to attach to this exception.
   */
  public BadStatusException(String message) {
    super(message);
  }
}
