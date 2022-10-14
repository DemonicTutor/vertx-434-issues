package com.noenv.vertx434issues;

import io.vertx.core.Vertx;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.parsetools.RecordParser;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.rxjava3.CompletableHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
class RecordParserTest {

  private Vertx vertx;

  @BeforeEach
  void before() {
    vertx = Vertx.vertx();
  }

  @Test
  void shouldParseRecordsFromReadStreamFuture(final VertxTestContext context) {
    final var source = vertx.fileSystem().open("src/test/resources/test.json", new OpenOptions());

    final var checkpoint = context.checkpoint(8);
    source.map(file -> RecordParser.newDelimited("\n", file))
      .onSuccess(
        parser -> parser.handler(buffer -> checkpoint.flag())
          .endHandler(nothing -> checkpoint.flag())
      )
      .mapEmpty()
      .onComplete(context.succeedingThenComplete());
  }

  @Test
  void shouldParseRecordsFromReadStreamRxJava(final VertxTestContext context) {
    final var source = io.vertx.rxjava3.core.Vertx.newInstance(vertx).fileSystem().open("src/test/resources/test.json", new OpenOptions());

    final var checkpoint = context.checkpoint(8);
    source.map(file -> io.vertx.rxjava3.core.parsetools.RecordParser.newDelimited("\n", file.toFlowable()))
      .flatMapObservable(io.vertx.rxjava3.core.parsetools.RecordParser::toObservable)
      .doOnNext(buffer -> checkpoint.flag())
      .doOnComplete(checkpoint::flag)
      .ignoreElements()
      .subscribe(CompletableHelper.toObserver(context.succeedingThenComplete()));
  }
}
