package mystdeim.nanobank.verticle;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import mystdeim.nanobank.model.Transaction;

import java.util.UUID;

public class TransactionVerticle extends JDBCVerticle {

    public static final String GET_TRANSACTIONS = "get_transactions";
    public static final String CREATE_TRANSACTION = "create_transaction";

    @Override
    public void start() {
        EventBus eventBus = vertx.eventBus();
        eventBus.consumer(GET_TRANSACTIONS, this::getTransactions);
        eventBus.consumer(CREATE_TRANSACTION, this::createTransaction);
    }

    void getTransactions(Message<String> msg) {
        query("select * from transaction where src = ? OR dst = ?",
                new JsonArray().add(msg.body()).add(msg.body()))
                .thenAccept(rows -> {
                    msg.reply(new JsonArray(rows.getRows()));
                })
                .exceptionally(ex -> {
                    msg.reply(null);
                    return null;
                });
    }

    void createTransaction(Message<Buffer> msg) {
        UUID id = UUID.randomUUID();
        Transaction tx = Json.decodeValue(msg.body(), Transaction.class);
        transaction().thenCompose(con -> {
            JsonArray params = new JsonArray()
                    .add(id.toString())
                    .add(tx.getSrc().toString())
                    .add(tx.getDst().toString())
                    .add(tx.getVol().toString());
            return query(con, "insert into transaction (id,src,dst,vol) values (?,?,?,?)", params);
        }).thenCompose(con -> {
            JsonArray params = new JsonArray()
                    .add(tx.getVol().toString())
                    .add(tx.getSrc().toString())
            ;
            return query(con, "update account set balance=balance-? where id=?", params);
        }).thenCompose(con -> {
            JsonArray params = new JsonArray()
                    .add(tx.getVol().toString())
                    .add(tx.getDst().toString())
            ;
            return query(con, "update account set balance=balance+? where id=?", params);
        }).thenCompose(con -> commit(con))
          .thenRun(() -> msg.reply(new JsonObject().put("id", id.toString())))
          .exceptionally(ex -> {
            msg.reply(null);
            return null;
        });
    }
}
