package wtf.lua.rockblock.calendarserver;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOutboundInvoker;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;

/**
 * ServerChannelHandler handles requests to the HTTP API and is used by {@link Server} for its Netty channel.
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
public final class ServerChannelHandler extends ChannelInboundHandlerAdapter {
  private static final Logger log = LoggerFactory.getLogger(ServerChannelHandler.class);

  private static final class HttpContentTypes {
    public static final String text_plain = "text/plain; charset=utf-8";
    public static final String application_json = "application/json";
  }

  private static final class HttpHeaderKeys {
    public static final String Access_Control_Allow_Origin = "Access-Control-Allow-Origin";
    public static final String Access_Control_Allow_Methods = "Access-Control-Allow-Methods";
    public static final String Access_Control_Allow_Headers = "Access-Control-Allow-Headers";
    public static final String Allow = "Allow";
    public static final String Connection = "Connection";
    public static final String Content_Length = "Content-Length";
    public static final String Content_Type = "Content-Type";
  }

  private final class HttpResponseWriter {
    private final HttpMessage request;

    public HttpResponseWriter() {
      this.request = null;
    }
    public HttpResponseWriter(HttpMessage request) {
      this.request = request;
    }

    private HttpResponseStatus status;

    public HttpResponseWriter setStatus(int status) {
      this.status = HttpResponseStatus.valueOf(status);
      return this;
    }

    private ByteBuf content;

    public HttpResponseWriter setContent(byte[] content) {
      this.content = Unpooled.wrappedBuffer(content);
      return this;
    }
    public HttpResponseWriter setContent(String content) {
      this.content = Unpooled.copiedBuffer(content, StandardCharsets.UTF_8);
      return this;
    }
    public HttpResponseWriter setContent(Object content) {
      return setContent(content.toString());
    }

    private String contentType;

    public HttpResponseWriter setContentType(String contentType) {
      this.contentType = contentType;
      return this;
    }

    private List<String> headerKeys;
    private List<Object> headerValues;

    public HttpResponseWriter setHeader(String key, Object value) {
      if (headerKeys == null) {
        headerKeys = new ArrayList<>();
        headerValues = new ArrayList<>();
      }
      headerKeys.add(key);
      headerValues.add(value);
      return this;
    }

    public void writeResponse(ChannelOutboundInvoker invoker) {
      if (status == null)
        status = HttpResponseStatus.OK;
      if (content == null)
        content = Unpooled.EMPTY_BUFFER;
      if (contentType == null)
        contentType = HttpContentTypes.text_plain;

      var response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, content);
      var headers = response.headers();

      if (headerKeys != null) {
        var size = headerKeys.size(); for (int i = 0; i < size; i++) {
          headers.set(headerKeys.get(i), headerValues.get(i));
        }
      }

      if (config.cors) {
        headers.set(HttpHeaderKeys.Access_Control_Allow_Origin, "*");
        headers.set(HttpHeaderKeys.Access_Control_Allow_Methods, "*");
        headers.set(HttpHeaderKeys.Access_Control_Allow_Headers, "*");
      }

      if (request != null && HttpUtil.isKeepAlive(request)) {
        headers.set(HttpHeaderKeys.Connection, "keep-alive");
      }

      headers.set(HttpHeaderKeys.Content_Length, content.readableBytes());
      headers.set(HttpHeaderKeys.Content_Type, contentType);

      invoker.writeAndFlush(response).addListener(future -> {
        if (!future.isSuccess())
          log.error("Failed to send HTTP response", future.cause());
      });
    }
  }

  private final Executor executor;
  private final Config config;

  private final CalendarJsonProvider calendarJsonProvider;

  public ServerChannelHandler(Executor executor, Config config, CalendarJsonProvider calendarJsonProvider) {
    this.executor = executor;
    this.config = config;
    this.calendarJsonProvider = calendarJsonProvider;
  }

  private static final Pattern pathTrimPattern = Pattern.compile("^\\/+|\\/+$|[?#].*$");

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (msg instanceof FullHttpRequest) {

      var request = (FullHttpRequest)msg;
      var method = request.method();
      var path = request.uri();
      request.release();

      if (method != HttpMethod.GET) {
        new HttpResponseWriter(request)
          .setStatus(405)
          .setContent("This server only accepts GET requests")
          .setHeader(HttpHeaderKeys.Allow, "GET")
          .writeResponse(ctx);
        return;
      }

      Month month;
      try {
        var expression = pathTrimPattern.matcher(path).replaceAll("");
        month = Month.parse(expression);
      } catch (InvalidMonthException error) {
        new HttpResponseWriter(request)
          .setStatus(400)
          .setContent(error.getMessage())
          .writeResponse(ctx);
        return;
      }

      if (
        (config.yearMin >= 0 && config.yearMax >= 0)
        && (month.year < config.yearMin || month.year > config.yearMax)
      ) {
        new HttpResponseWriter(request)
          .setStatus(400)
          .setContent("Year out of range")
          .writeResponse(ctx);
        return;
      }

      var promise = calendarJsonProvider.request(month);
      if (promise.isDone() && !promise.isCompletedExceptionally()) {
        new HttpResponseWriter(request)
          .setContent(promise.get())
          .setContentType(HttpContentTypes.application_json)
          .writeResponse(ctx);
      } else {
        promise.handleAsync((body, error) -> {
          if (!promise.isCompletedExceptionally()) {
            new HttpResponseWriter(request)
              .setContent(body)
              .setContentType(HttpContentTypes.application_json)
              .writeResponse(ctx);
          } else {
            new HttpResponseWriter(request)
              .setStatus(500)
              .setContent(error)
              .writeResponse(ctx);
          }
          return null;
        }, executor);
      }

    } else super.channelRead(ctx, msg);
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
    ctx.flush();
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable error) throws Exception {
    log.error("Exception occurred while handling request", error);
    new HttpResponseWriter()
      .setStatus(500)
      .setContent(error)
      .writeResponse(ctx);
  }
}
