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
import mystdeim.nanobank.util.MessageCodecs;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.math.BigDecimal;
import java.net.ServerSocket;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(VertxExtension.class)
@Timeout(value = 2, timeUnit = TimeUnit.SECONDS)
public class AccountsTest {

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
        CountDownLatch latch = new CountDownLatch(3);
        vertx.deployVerticle(DBInitVerticle.class, options,
                context.succeeding(id -> latch.countDown()));
        vertx.deployVerticle(WebVerticle.class, options,
                context.succeeding(id -> latch.countDown()));
        vertx.deployVerticle(AccountVerticle.class, options,
                context.succeeding(id -> latch.countDown()));
        latch.await();
        context.completeNow();
    }

    @Test
    public void testEmptyResponse(Vertx vertx, VertxTestContext context) {
        WebClient client = WebClient.create(vertx);
        client.get(port, "localhost", "/api/accounts").send(response -> {
            assertEquals("[]", response.result().body().toString());
            context.completeNow();
        });
    }

    @Nested
    class AfterCreationAccount {
        UUID accountId;
        @BeforeEach
        void createAccount(Vertx vertx, VertxTestContext context) {
            client
                    .post(port, "localhost", "/api/account")
                    .sendJson(new JsonObject().put("person", "Tom").put("balance", "100.0"), ar -> {
                        accountId = UUID.fromString(ar.result().body().toJsonObject().getString("id"));
                        context.completeNow();
                    });
        }

        @Test
        void testGetAccount(Vertx vertx, VertxTestContext context) {
            client.get(port, "localhost", "/api/account/" + accountId).send(response -> {
                assertEquals(200, response.result().statusCode());
                Account expected = new Account(accountId, "Tom", new BigDecimal("100.0"));
                Account account = response.result().bodyAsJson(Account.class);
                assertEquals(expected, account);
                context.completeNow();
            });
        }

        @Test
        void testNonEmptyList(Vertx vertx, VertxTestContext context) {
            client.get(port, "localhost", "/api/accounts").send(response -> {
                assertEquals(200, response.result().statusCode());
                List<Account> accounts = Json.decodeValue(response.result().body(), new TypeReference<List<Account>>() {});
                assertEquals(1, accounts.size());

                context.completeNow();
            });
        }

        @Test
        void test404(Vertx vertx, VertxTestContext context) {
            UUID uuid = UUID.randomUUID();
            client.get(port, "localhost", "/api/accounts/" + uuid.toString()).send(response -> {
                assertEquals(404, response.result().statusCode());
                context.completeNow();
            });
        }

        @Test
        void testDeletionNonEmpty(Vertx vertx, VertxTestContext context) {
            client.delete(port, "localhost", "/api/account/" + accountId).send(response -> {
                assertEquals(409, response.result().statusCode());
                context.completeNow();
            });
        }
    }

    @Nested
    class AfterCreationEmptyAccount {
        UUID accountId;

        @BeforeEach
        void createAccount(Vertx vertx, VertxTestContext context) {
            client
                    .post(port, "localhost", "/api/account")
                    .sendJson(new JsonObject().put("person", "Tom").put("balance", "0.0"), ar -> {
                        accountId = UUID.fromString(ar.result().body().toJsonObject().getString("id"));
                        context.completeNow();
                    });
        }

        @Test
        void testDeletionEmpty(Vertx vertx, VertxTestContext context) {
            client.delete(port, "localhost", "/api/account/" + accountId).send(response -> {
                assertEquals(204, response.result().statusCode());
                context.completeNow();
            });
        }
    }
}
