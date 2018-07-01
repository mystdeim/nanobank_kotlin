package mystdeim.nanobank.verticle;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import mystdeim.nanobank.model.Account;

import java.util.UUID;

public class AccountVerticle extends JDBCVerticle {

    public static final String GET_ACCOUNTS = "get_accounts";
    public static final String GET_ACCOUNT = "get_account";
    public static final String CREATE_ACCOUNT = "create_account";
    public static final String DELETE_ACCOUNT = "delete_account";

    @Override
    public void start() {
        EventBus eventBus = vertx.eventBus();
        eventBus.consumer(GET_ACCOUNT, this::getAccount);
        eventBus.consumer(GET_ACCOUNTS, this::getAccounts);
        eventBus.consumer(CREATE_ACCOUNT, this::createAccount);
        eventBus.consumer(DELETE_ACCOUNT, this::deleteAccount);
    }

    @Override
    public void stop() {
    }

    void getAccount(Message<String> msg) {
        query("select * from account where id = ?", new JsonArray().add(msg.body()))
                .thenAccept(rows -> {
                    if (rows.getRows().size() > 0) {
                        msg.reply(Json.decodeValue(rows.getRows().get(0).toString(), Account.class));
                    } else {
                        msg.reply(null);
                    }
                })
                .exceptionally(ex -> {
                    return null;
                });
    }

    void getAccounts(Message msg) {
        query("select * from account")
                .thenAccept(rows -> {
                    msg.reply(new JsonArray(rows.getRows()));
                })
                .exceptionally(ex -> {
                    return null;
                });
    }

    void createAccount(Message<Buffer> msg) {
        Account account = Json.decodeValue(msg.body(), Account.class);
        if (null == account.getId()) {
            UUID uuid = UUID.randomUUID();
            account.setId(uuid);
        }
        query("insert into account (id, person, balance) values (?, ?, ?)",
                new JsonArray()
                        .add(account.getId().toString())
                        .add(account.getPerson())
                        .add(account.getBalance().toString()))
                .thenRun(() -> msg.reply(new JsonObject().put("id", account.getId().toString())))
                .exceptionally(ex -> {
                    msg.reply(null);
                    return null;
                });
    }

    void deleteAccount(Message<String> msg) {
        query("delete from account where id = ? and balance = 0",
                new JsonArray().add(msg.body()))
                .thenRun(() -> {
                    msg.reply(true);
                })
                .exceptionally(ex -> {
                    msg.reply(false);
                    return null;
                });
    }
}
