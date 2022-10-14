package com.noenv.vertx434issues;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.logging.ByteBufFormat;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Vertx;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.http.*;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
class UploadTest {

  private Vertx vertx;

  @BeforeEach
  void before() {
    vertx = Vertx.vertx();
  }

  @AfterEach
  void after(final VertxTestContext context) {
    vertx.close().onComplete(context.succeedingThenComplete());
  }

  @Test
  void shouldUploadHttp1(final VertxTestContext context) {
    final var router = Router.router(vertx);

    router.route().handler(BodyHandler.create());
    router.route().handler(routingContext -> {
      context.verify(() -> Assertions.assertEquals(new JsonArray("""
        [
          {"a": "value"},
          {"b": "value"},
          {"c": "value"},
          {"d": "value"},
          {"e": "value"}
        ]
        """), routingContext.body().asJsonArray()));
      routingContext.response().end();
    });

    final var server = vertx.createHttpServer(new HttpServerOptions().setLogActivity(true).setActivityLogDataFormat(ByteBufFormat.HEX_DUMP))
      .requestHandler(router)
      .listen(8080)
      .mapEmpty();

    final var client = vertx.createHttpClient(new HttpClientOptions())
      .request(new RequestOptions().setURI("/file").setPort(8080))
      .compose(
        request ->
          vertx.fileSystem().open("src/test/resources/test.json", new OpenOptions())
            .map(file -> file.setReadBufferSize(2))
            .compose(request::send)
      )
      .onSuccess(response -> context.verify(() -> Assertions.assertEquals(200, response.statusCode())))
      .mapEmpty();

    CompositeFuture.all(server, client)
      .onComplete(context.succeedingThenComplete());
  }

  // FIXME BodyHandler expects TRANSFER-ENCODING and does not react because it is missing
  // FIXME also fails with VertX 4.3.3
  @Test
  void shouldUploadHttp2(final VertxTestContext context) {
    final var router = Router.router(vertx);

    router.route().handler(BodyHandler.create());
    router.route().handler(routingContext -> {
      context.verify(
        () -> {
          Assertions.assertEquals("application/json", routingContext.parsedHeaders().contentType().value());
          Assertions.assertEquals(new JsonArray("""
            [
              {"a": "value"},
              {"b": "value"},
              {"c": "value"},
              {"d": "value"},
              {"e": "value"}
            ]
            """), routingContext.body().asJsonArray());
        }
      );
      routingContext.response().end();
    });

    final var server = vertx.createHttpServer(new HttpServerOptions().setLogActivity(true).setActivityLogDataFormat(ByteBufFormat.HEX_DUMP))
      .requestHandler(router)
      .listen(8080)
      .mapEmpty();

    final var client = vertx.createHttpClient(new HttpClientOptions()
        .setProtocolVersion(HttpVersion.HTTP_2)
        .setHttp2ClearTextUpgrade(false)
      )
      .request(new RequestOptions().setURI("/file").setPort(8080)
        .setHeaders(HttpHeaders.headers().add(HttpHeaderNames.CONTENT_TYPE, "application/json"))
      )
      .compose(
        request ->
          vertx.fileSystem().open("src/test/resources/test.json", new OpenOptions())
            .map(file -> file.setReadBufferSize(2))
            .compose(request::send)
      )
      .onSuccess(response -> context.verify(() -> Assertions.assertEquals(200, response.statusCode())))
      .mapEmpty();

    CompositeFuture.all(server, client)
      .onComplete(context.succeedingThenComplete());
  }

  @Test
  void shouldUploadHttp2_BodyFutureWithoutBodyHandler(final VertxTestContext context) {
    final var router = Router.router(vertx);

    router.route().handler(routingContext -> {
      routingContext.request().body()
        .onSuccess(body -> {
          context.verify(() -> Assertions.assertEquals(new JsonArray("""
            [
              {"a": "value"},
              {"b": "value"},
              {"c": "value"},
              {"d": "value"},
              {"e": "value"}
            ]
            """), new JsonArray(body)));
          routingContext.response().end();
        });
    });

    final var server = vertx.createHttpServer(new HttpServerOptions().setLogActivity(true).setActivityLogDataFormat(ByteBufFormat.HEX_DUMP))
      .requestHandler(router)
      .listen(8080)
      .mapEmpty();

    final var client = vertx.createHttpClient(new HttpClientOptions()
        .setProtocolVersion(HttpVersion.HTTP_2)
        .setHttp2ClearTextUpgrade(false)
      )
      .request(new RequestOptions().setURI("/file").setPort(8080))
      .compose(
        request ->
          vertx.fileSystem().open("src/test/resources/test.json", new OpenOptions())
            .map(file -> file.setReadBufferSize(2))
            .compose(request::send)
      )
      .onSuccess(response -> context.verify(() -> Assertions.assertEquals(200, response.statusCode())))
      .mapEmpty();

    CompositeFuture.all(server, client)
      .onComplete(context.succeedingThenComplete());
  }
}
