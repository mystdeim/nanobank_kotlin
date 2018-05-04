package mystdeim.nanobank.verticle;

import com.fasterxml.jackson.core.type.TypeReference;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import mystdeim.nanobank.model.Account;
import mystdeim.nanobank.model.Transaction;
import mystdeim.nanobank.util.MessageCodecs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.math.BigDecimal;
import java.net.ServerSocket;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(VertxExtension.class)
public class TransactionsTest {

    Integer port;
    WebClient client;

    @BeforeEach
    public void setUp(Vertx vertx, VertxTestContext context) throws Exception {
        client = WebClient.create(vertx);
        MessageCodecs.add(vertx.eventBus());
        try (ServerSocket socket = new ServerSocket(0)) {
            port = socket.getLocalPort();
        }
        DeploymentOptions options = new DeploymentOptions().setConfig(
                new JsonObject()
                        .put("http.port", port)
                        .put("db.url", "jdbc:h2:mem:test;database_to_upper=false")
        );
        CountDownLatch latch = new CountDownLatch(4);
        vertx.deployVerticle(DBInitVerticle.class, options,
                context.succeeding(id -> latch.countDown()));
        vertx.deployVerticle(WebVerticle.class, options,
                context.succeeding(id -> latch.countDown()));
        vertx.deployVerticle(AccountVerticle.class, options,
                context.succeeding(id -> latch.countDown()));
        vertx.deployVerticle(TransactionVerticle.class, options,
                context.succeeding(id -> latch.countDown()));
        latch.await();
        context.completeNow();
    }

    @Nested
    @Timeout(value = 1000)
    class AfterCreationAccounts {
        UUID account1;
        UUID account2;

        @BeforeEach
        void createAccount(Vertx vertx, VertxTestContext context) throws Exception {
            CountDownLatch latch = new CountDownLatch(2);
            client
                    .post(port, "localhost", "/api/account")
                    .sendJson(new JsonObject().put("person", "Tom").put("balance", "100.0"), ar -> {
                        account1 = UUID.fromString(ar.result().body().toJsonObject().getString("id"));
                        latch.countDown();
                    });
            client
                    .post(port, "localhost", "/api/account")
                    .sendJson(new JsonObject().put("person", "Sam").put("balance", "100.0"), ar -> {
                        account2 = UUID.fromString(ar.result().body().toJsonObject().getString("id"));
                        latch.countDown();
                    });
            latch.await();
            context.completeNow();
        }

        @Test
        void transfer(Vertx vertx, VertxTestContext context) {
            JsonObject jo = new JsonObject()
                    .put("src", account1.toString())
                    .put("dst", account2.toString())
                    .put("vol", "50.0");
            client
                    .post(port, "localhost", "/api/transaction")
                    .sendJson(jo, ar -> {
                        assertEquals(201, ar.result().statusCode());
                        context.completeNow();
                    });
        }

        @Test
        void testTransferFakeAccount(Vertx vertx, VertxTestContext context) {
            JsonObject jo = new JsonObject()
                    .put("src", UUID.randomUUID().toString())
                    .put("dst", account2.toString())
                    .put("vol", "50.0");
            client
                    .post(port, "localhost", "/api/transaction")
                    .sendJson(jo, ar -> {
                        assertEquals(404, ar.result().statusCode());
                        context.completeNow();
                    });
        }

        @Test
        void testTransferNonEnoughMoney(Vertx vertx, VertxTestContext context) {
            JsonObject jo = new JsonObject()
                    .put("src", account1.toString())
                    .put("dst", account2.toString())
                    .put("vol", "150.0");
            client
                    .post(port, "localhost", "/api/transaction")
                    .sendJson(jo, ar -> {
                        assertEquals(409, ar.result().statusCode());
                        context.completeNow();
                    });
        }

        @Nested
        class AfterTransaction {
            @BeforeEach
            void transfer(Vertx vertx, VertxTestContext context) {
                Transaction tx = new Transaction(null, account1, account2, new BigDecimal("50.0"));
                client
                        .post(port, "localhost", "/api/transaction")
                        .sendJson(tx, ar -> context.completeNow());
            }

            @Test
            void testSrcBalance(Vertx vertx, VertxTestContext context) {
                client.get(port, "localhost", "/api/account/" + account1).send(response -> {
                    Account account = response.result().bodyAsJson(Account.class);
                    assertEquals(new BigDecimal("50.0"), account.getBalance());
                    context.completeNow();
                });
            }

            @Test
            void testDstBalance(Vertx vertx, VertxTestContext context) {
                client.get(port, "localhost", "/api/account/" + account2).send(response -> {
                    Account account = response.result().bodyAsJson(Account.class);
                    assertEquals(new BigDecimal("150.0"), account.getBalance());
                    context.completeNow();
                });
            }

            @Test
            void testSrcTransaction(Vertx vertx, VertxTestContext context) {
                client.get(port, "localhost", String.format("/api/account/%s/transactions", account1)).send(response -> {
                    List<Transaction> txs = Json.decodeValue(response.result().body(), new TypeReference<List<Transaction>>() {});
                    assertEquals(200, response.result().statusCode());
                    assertEquals(1, txs.size());
                    context.completeNow();
                });
            }

            @Test
            void testDstTransaction(Vertx vertx, VertxTestContext context) {
                client.get(port, "localhost", String.format("/api/account/%s/transactions", account2)).send(response -> {
                    List<Transaction> txs = Json.decodeValue(response.result().body(), new TypeReference<List<Transaction>>() {});
                    assertEquals(200, response.result().statusCode());
                    assertEquals(1, txs.size());
                    context.completeNow();
                });
            }
        }
    }
}
