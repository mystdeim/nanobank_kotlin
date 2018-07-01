package mystdeim.nanobank.verticle

import io.vertx.core.Handler
import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.Message
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.http.HttpServerRequest
import io.vertx.core.json.Json
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Route
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.awaitEvent
import io.vertx.kotlin.coroutines.awaitResult
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.experimental.launch
import mystdeim.nanobank.model.Account
import mystdeim.nanobank.model.Transaction
import org.slf4j.LoggerFactory
import java.math.BigDecimal

import java.util.concurrent.CompletableFuture


class WebVerticle : CoroutineVerticle() {

    val log = LoggerFactory.getLogger(WebVerticle.javaClass.name)

    suspend override fun start() {

        val router = Router.router(vertx)
        router.get("/api/accounts").handler(::accounts)
        router.get("/api/account/:id").coroutine { account(it) }
        router.get("/api/account/:id/transactions").handler(::accountTransactions)
        router.delete("/api/account/:id").coroutine { deleteAccount(it) }
//        router.post("/api/account").handler(::accountCreate)
        router.post("/api/account").coroutine { accountCreate(it) }

        router.post("/api/transaction").coroutine { transactionCreate(it) }

        val port = config.getInteger("http.port")!!
        val serverOptions = HttpServerOptions()
        serverOptions.isCompressionSupported = true

        val httpServer = vertx.createHttpServer(serverOptions)
        httpServer.requestHandler(Handler<HttpServerRequest> { router.accept(it) }).listen(port)
        log.info("REST-API was exposed: http://localhost:{}", port)
    }

    internal fun accounts(ctx: RoutingContext) {
        sendMsg<Any>(AccountVerticle.GET_ACCOUNTS, null).thenAccept { res -> ctx.response().putHeader(CONTENT_TYPE, CONTENT_JSON).end(res.toString()) }

    }

    suspend internal fun account(ctx: RoutingContext) {
        val account = awaitResult<Message<Account>> {
            vertx.eventBus().send(AccountVerticle.GET_ACCOUNT, ctx.pathParam("id"), it)
        }.body()
        ctx.response().putHeader(CONTENT_TYPE, CONTENT_JSON).end(Json.encode(account))
    }

    suspend internal fun accountCreate(ctx: RoutingContext) {
        val body = body(ctx)
        val res = awaitResult<Message<JsonObject>> {
            vertx.eventBus().send(AccountVerticle.CREATE_ACCOUNT, body, it)
        }.body()
        if (null != res) {
            ctx.response().putHeader(CONTENT_TYPE, CONTENT_JSON).end(Json.encode(res))
        } else {
            ctx.response().setStatusCode(500).end()
        }
    }

    suspend internal fun deleteAccount(ctx: RoutingContext) {
//        val account = awaitResult<Message<Account>> {
//            vertx.eventBus().send(AccountVerticle.GET_ACCOUNT, ctx.pathParam("id"), it)
//        }.body()

        val account = send<Account, String>(AccountVerticle.GET_ACCOUNT, ctx.pathParam("id")).body()

        if (account.balance.equals(BigDecimal("0.0"))) {
            val del = awaitResult<Message<Boolean>> {
                vertx.eventBus().send(AccountVerticle.DELETE_ACCOUNT, ctx.pathParam("id"), it) }
            ctx.response().setStatusCode(204).end()
        }  else {
            ctx.response().setStatusCode(409).end()
        }

    }

    internal fun accountTransactions(ctx: RoutingContext) {
        this.sendMsg<JsonArray>(TransactionVerticle.GET_TRANSACTIONS, ctx.pathParam("id"))
                .thenAccept { res ->
                    ctx.response().putHeader(CONTENT_TYPE, CONTENT_JSON)
                            .end(Json.encode(res))
                }
    }

    suspend internal fun transactionCreate(ctx: RoutingContext) {
        val body = body(ctx)
        val tx = Json.decodeValue(body, Transaction::class.java)
        val src = awaitResult<Message<Account>> {
            vertx.eventBus().send(AccountVerticle.GET_ACCOUNT, tx.src.toString(), it)
        }.body()
        if (null == src) {
            ctx.response().setStatusCode(404).end()
            return
        }
        if (src.balance.compareTo(tx.vol) < 0) {
            ctx.response().setStatusCode(409).end()
            return
        }
        val dst = awaitResult<Message<Account>> {
            vertx.eventBus().send(AccountVerticle.GET_ACCOUNT, tx.dst.toString(), it)
        }.body()
        if (null == dst) {
            ctx.response().setStatusCode(404).end()
            return
        }

        val txNew = awaitResult<Message<JsonObject>> {
            vertx.eventBus().send(TransactionVerticle.CREATE_TRANSACTION, body, it)
        }.body()
//        val txNew = send<JsonObject, String>(TransactionVerticle.CREATE_TRANSACTION, body).body()
        ctx.response().setStatusCode(201).end(txNew.toString())

    }

    // Helpers

    internal fun <T> sendMsg(address: String, body: Any?): CompletableFuture<T> {
        val cf = CompletableFuture<T>()
        vertx.eventBus().send<T>(address, body) { handler ->
            if (handler.succeeded()) {
                cf.complete(handler.result().body())
            } else {
                cf.completeExceptionally(handler.cause())
            }
        }
        return cf
    }

    suspend internal fun <T, R> send(address : String, msg : R): Message<T> {
        return awaitResult {vertx.eventBus().send(AccountVerticle.DELETE_ACCOUNT, msg, it) }
    }

    suspend internal fun body(ctx: RoutingContext): Buffer {
        return awaitEvent<Buffer> { ctx.request().bodyHandler(it) }
    }

    companion object {

        val CONTENT_TYPE = "content-type"

        val CONTENT_JSON = "application/json; charset=utf-8"
    }
    // -----------------------------------------------------------------------------------------------------------------

    fun Route.coroutine(coroutineHandler: suspend (RoutingContext) -> Unit): Route {
        handler {
            launch(vertx.dispatcher()) {
                coroutineHandler.invoke(it)
            }
        }
        return this
    }
}

