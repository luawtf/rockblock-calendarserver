package wtf.lua.rockblock.calendarserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;

/**
 * Server provides the HTTP API that is used to access data returned from {@link CalendarJsonProvider}.
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
public final class Server {
  private static final Logger log = LoggerFactory.getLogger(Server.class);

  private final Config config;

  private final ServerBootstrap bootstrap;

  /**
   * Create a new Server instance.
   * @param groupAccept Event loop group to use for accepting connections.
   * @param groupServe Event loop group to use for processing clients.
   * @param config Application configuration.
   * @param calendarJsonProvider Instance of CalendarJsonProvider to serve data from.
   */
  public Server(
    EventLoopGroup groupAccept,
    EventLoopGroup groupServe,
    Config config,
    CalendarJsonProvider calendarJsonProvider
  ) {
    this.config = config;

    bootstrap = new ServerBootstrap();
    bootstrap.group(groupAccept, groupServe);

    bootstrap.channel(NioServerSocketChannel.class);
    bootstrap.option(ChannelOption.SO_BACKLOG, 1024);

    bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
      @Override
      protected void initChannel(SocketChannel channel) throws Exception {
        var pipeline = channel.pipeline();
        pipeline.addLast(new HttpRequestDecoder());
        pipeline.addLast(new HttpResponseEncoder());
        pipeline.addLast(new HttpObjectAggregator(65535));
        pipeline.addLast(new ServerChannelHandler(groupServe, config, calendarJsonProvider));
      }
    });
    bootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);
  }

  /**
   * Start the server and wait for it to close/crash.
   * This method stops the executing thread until the server has completed.
   */
  public void start() {
    try {
      log.info("Binding to port {}", config.port);
      var channel = bootstrap.bind(config.port).sync().channel();
      log.info("Listening on port {}", config.port);
      try {
        channel.closeFuture().sync();
        log.info("Server closed");
      } catch (Throwable error) {
        log.error("Exception occurred that caused the server to close", error);
      }
    } catch (Throwable error) {
      log.error("Failed to bind to port {}", config.port, error);
    }
  }
}
