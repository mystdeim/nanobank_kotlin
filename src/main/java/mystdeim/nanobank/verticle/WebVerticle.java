package mystdeim.nanobank.verticle;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;
import mystdeim.nanobank.model.Account;
import mystdeim.nanobank.model.Transaction;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class WebVerticle extends AbstractVerticle {

    public static final String CONTENT_TYPE = "content-type";
    public static final String CONTENT_JSON = "application/json; charset=utf-8";

    @Override
    public void start() {

        Router router = Router.router(vertx);
        router.get("/api/accounts").handler(this::accounts);
        router.get("/api/account/:id").handler(this::account);
        router.get("/api/account/:id/transactions").handler(this::accountTransactions);
        router.delete("/api/account/:id").handler(this::deleteAccount);
        router.post("/api/account").handler(this::accountCreate);

        router.post("/api/transaction").handler(this::transactionCreate);

        int port = config().getInteger("http.port");
        HttpServerOptions serverOptions = new HttpServerOptions();
        serverOptions.setCompressionSupported(true);

        HttpServer httpServer = vertx.createHttpServer(serverOptions);
        httpServer.requestHandler(router::accept).listen(port);
        log.info("REST-API was exposed: http://localhost:{}", port);
    }

    void accounts(RoutingContext ctx) {
        sendMsg(AccountVerticle.GET_ACCOUNTS, null).thenAccept(res -> {
            ctx.response().putHeader(CONTENT_TYPE, CONTENT_JSON).end(res.toString());
        });

    }
    void account(RoutingContext ctx) {
        this.<Account>sendMsg(AccountVerticle.GET_ACCOUNT, ctx.pathParam("id")).thenAccept(res -> {
            ctx.response().putHeader(CONTENT_TYPE, CONTENT_JSON)
                    .end(Json.encode(res));
        });
    }
    void accountCreate(RoutingContext ctx) {
        bodyHandler(ctx).thenAccept(body -> {
            this.<JsonObject>sendMsg(AccountVerticle.CREATE_ACCOUNT, body).thenAccept(res -> {
                if (null != res) {
                    ctx.response().putHeader(CONTENT_TYPE, CONTENT_JSON)
                            .end(Json.encode(res));
                } else {
                    ctx.response().setStatusCode(500).end();
                }
                    });
        });
    }
    void deleteAccount(RoutingContext ctx) {
        this.<Account>sendMsg(AccountVerticle.GET_ACCOUNT, ctx.pathParam("id")).thenCompose(res -> {
            if (res.getBalance().equals(new BigDecimal("0.0"))) {
                return this.<Boolean>sendMsg(AccountVerticle.DELETE_ACCOUNT, ctx.pathParam("id"));
            } else {
                CompletableFuture<Boolean> cf = new CompletableFuture<Boolean>();
                cf.complete(false);
                return cf;
            }
        }).thenAccept(res -> ctx.response().setStatusCode(res ? 204 : 409).end());
    }

    void accountTransactions(RoutingContext ctx) {
        this.<JsonArray>sendMsg(TransactionVerticle.GET_TRANSACTIONS, ctx.pathParam("id"))
                .thenAccept(res -> ctx.response().putHeader(CONTENT_TYPE, CONTENT_JSON)
                .end(Json.encode(res)));
    }

    void transactionCreate(RoutingContext ctx) {
        bodyHandler(ctx)
                .thenCompose(body -> {
                    Transaction tx = Json.decodeValue(body, Transaction.class);
                    CompletableFuture<Void> src = this.<Account>sendMsg(AccountVerticle.GET_ACCOUNT, tx.getSrc().toString())
                            .thenCompose(account -> {
                                CompletableFuture<Void> cf = new CompletableFuture<>();
                                if (null == account) {
                                    cf.completeExceptionally(new RuntimeException("404"));
                                } else if (account.getBalance().compareTo(tx.getVol()) < 0) {
                                    cf.completeExceptionally(new RuntimeException("409"));
                                } else {
                                    cf.complete(null);
                                }
                                return cf;
                            });
                    CompletableFuture<Void> dst =
                            this.<Account>sendMsg(AccountVerticle.GET_ACCOUNT, tx.getDst().toString())
                                    .thenCompose(account -> {
                                        CompletableFuture<Void> cf = new CompletableFuture<>();
                                        if (null == account) {
                                            cf.completeExceptionally(new RuntimeException("404"));
                                        } else {
                                            cf.complete(null);
                                        }
                                        return cf;
                                    });
                    return src
                            .thenCompose(o -> dst)
                            .thenCompose(o -> sendMsg(TransactionVerticle.CREATE_TRANSACTION, body));
                })

                .thenAccept(o -> {
                    ctx.response().setStatusCode(201).end(o.toString());
                }).exceptionally(ex -> {
                    ctx.response().setStatusCode(Integer.parseInt(ex.getCause().getMessage())).end();
                    log.error("!", ex);
                    return null;
        });
    }

    // Helpers
    // -----------------------------------------------------------------------------------------------------------------

    <T> CompletableFuture<T> sendMsg(String address, Object body) {
        CompletableFuture<T> cf = new CompletableFuture<>();
        vertx.eventBus().<T>send(address, body, handler -> {
           if (handler.succeeded()) {
               cf.complete(handler.result().body());
           } else {
               cf.completeExceptionally(handler.cause());
           }
        });
        return cf;
    }

    CompletableFuture<Buffer> bodyHandler(RoutingContext ctx) {
        CompletableFuture<Buffer> cf = new CompletableFuture<>();
        ctx.request().bodyHandler(body -> cf.complete(body));
        return cf;
    }
}
