package com.twobassdrum;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerResponse;

public class Http2Server extends AbstractVerticle {
  @Override
  public void start() {
      HttpServerOptions options = new HttpServerOptions();
      // my client module knows about this option
      options.setUseAlpn(false);
      options.setSsl(false);

      HttpServer server = vertx.createHttpServer(options);
      server.requestHandler(req -> {
          final LatencyTimer myTimer = new LatencyTimer();
          System.out.println(this);
          String path = req.path();
          HttpServerResponse resp = req.response();
          // fyi : chunked response has no effect for an HTTP/2 stream
          resp.setChunked(true);

          if ("/".equals(path)) {
              // simulate 100ms delayed push
              vertx.setTimer(100, id -> {
                  push("push1",resp,myTimer);
              });
              // immediate server push
              push("push2",resp,myTimer);

              // simulate 450ms delay for main response
              vertx.setTimer(450, id -> {
                  resp.putHeader("content-type", "text/html");
                  System.out.println("sending main " + myTimer.latency());
                  resp.end("<html><body>" +
                          "<h1>main</h1>" +
                          "<p>version = " + req.version() + "</p>" +
                          "</body></html>");
              });
          } else {
              System.out.println("Not found " + path);
              resp.setStatusCode(404).end();
          }
      });

      server.listen(config().getInteger("http.port", 8888), ar -> {
          if (ar.succeeded()) {
              System.out.println("Server started");
          } else {
              ar.cause().printStackTrace();
          }
      });
    }
    public void push(String name, HttpServerResponse resp, LatencyTimer myTimer) {
        System.out.println("prepare " + name + " " + myTimer.latency());
        resp.push(HttpMethod.GET, "/" + name, ar -> {
            if (ar.succeeded()) {
                System.out.println("sending " + name + " " + myTimer.latency());
                HttpServerResponse pushedResp = ar.result();
                pushedResp.putHeader("content-type", "text/html").end("<html><body>" +
                        "<h1>" + name + "</h1>" +
                        "</body></html>");
            } else {
                // cannot push, delegate to main response
                System.out.println("cannot " + name);
            }
        });
    }
}
class LatencyTimer {
    long start;
    public LatencyTimer() {
        this.start = System.currentTimeMillis();
    }
    public long latency() {
        return System.currentTimeMillis() - this.start;
    }
}

