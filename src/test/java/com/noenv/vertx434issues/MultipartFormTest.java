package com.noenv.vertx434issues;

import io.netty.handler.logging.ByteBufFormat;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.http.RequestOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.multipart.MultipartForm;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
class MultipartFormTest {

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
      context.verify(() -> Assertions.assertEquals("value", routingContext.request().getFormAttribute("attr")));
      routingContext.response().end();
    });

    final var server = vertx.createHttpServer(new HttpServerOptions().setLogActivity(true).setActivityLogDataFormat(ByteBufFormat.HEX_DUMP))
      .requestHandler(router)
      .listen(8080)
      .mapEmpty();
    final var client = WebClient.create(vertx, new WebClientOptions())
      .request(HttpMethod.POST, new RequestOptions().setURI("/file").setPort(8080))
      .sendMultipartForm(
        MultipartForm.create().attribute("attr", "value")
          .textFileUpload("name", "test.json", "src/test/resources/test.json", "text/plain")
      )
      .onSuccess(response -> context.verify(() -> Assertions.assertEquals(200, response.statusCode())))
      .mapEmpty();

    CompositeFuture.all(server, client)
      .onComplete(context.succeedingThenComplete());
  }

  // FIXME MultipartFormUpload  creates DefaultFullHttpRequest with HttpVersion.HTTP_1_1 which adds HttpHeaderNames.TRANSFER_ENCODING = chunked
  // fails reading request at netty HpackDecoder line 412
  @Test
  void shouldUploadHttp2(final VertxTestContext context) {
    final var router = Router.router(vertx);

    router.route().handler(BodyHandler.create());
    router.route().handler(routingContext -> {
      context.verify(() -> Assertions.assertEquals("value", routingContext.request().getFormAttribute("attr")));
      routingContext.response().end();
    });

    final var server = vertx.createHttpServer(new HttpServerOptions().setLogActivity(true).setActivityLogDataFormat(ByteBufFormat.HEX_DUMP))
      .requestHandler(router)
      .listen(8080)
      .mapEmpty();
    final var client = WebClient.create(vertx, new WebClientOptions()
        .setProtocolVersion(HttpVersion.HTTP_2)
        .setHttp2ClearTextUpgrade(false)
      )
      .request(HttpMethod.POST, new RequestOptions().setURI("/file").setPort(8080))
      .sendMultipartForm(
        MultipartForm.create().attribute("attr", "value")
          .textFileUpload("name", "test.json", "src/test/resources/test.json", "text/plain")
      )
      .onSuccess(response -> context.verify(() -> Assertions.assertEquals(200, response.statusCode())))
      .mapEmpty();

    CompositeFuture.all(server, client)
      .onComplete(context.succeedingThenComplete());
  }
}
