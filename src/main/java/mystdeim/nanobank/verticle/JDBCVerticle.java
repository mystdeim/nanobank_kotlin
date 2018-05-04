package mystdeim.nanobank.verticle;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLClient;
import io.vertx.ext.sql.SQLConnection;

import java.util.concurrent.CompletableFuture;

public abstract class JDBCVerticle extends AbstractVerticle {

    protected SQLClient sqlClient() {
        JsonObject config = new JsonObject().put("url", config().getString("db.url"));
        return JDBCClient.createShared(vertx, config);
    }

    protected CompletableFuture<SQLConnection> connection() {
        CompletableFuture<SQLConnection> cf = new CompletableFuture();
        sqlClient().getConnection(h -> {
            wrap(cf, h);
        });
        return cf;
    }

    protected CompletableFuture<SQLConnection> transaction() {
        CompletableFuture<SQLConnection> cf = new CompletableFuture();
        connection().thenAccept(con -> {
           con.setAutoCommit(false, res -> {
               if (res.succeeded()) {
                   cf.complete(con);
               } else {
                   cf.completeExceptionally(res.cause());
               }
           });
        });
        return cf;
    }

    protected CompletableFuture<ResultSet> query(String sql) {
        CompletableFuture<ResultSet> cf = new CompletableFuture();
        sqlClient().query(sql, h -> {
            wrap(cf, h);
        });
        return cf;
    }

    protected CompletableFuture<ResultSet> query(String sql, JsonArray params) {
        CompletableFuture<ResultSet> cf = new CompletableFuture();
        sqlClient().queryWithParams(sql, params, h -> wrap(cf, h));
        return cf;
    }

    protected CompletableFuture<SQLConnection> query(SQLConnection con, String sql, JsonArray params) {
        CompletableFuture<SQLConnection> cf = new CompletableFuture();
        con.queryWithParams(sql, params, res -> {
            if (res.succeeded()) {
                cf.complete(con);
            } else {
                cf.completeExceptionally(res.cause());
            }
        });
        return cf;
    }

    protected CompletableFuture<Void> connectionClose(SQLConnection con) {
        CompletableFuture<Void> cf = new CompletableFuture();
        con.close(h -> wrap(cf, h));
        return cf;
    }

    protected CompletableFuture<Void> commit(SQLConnection con) {
        CompletableFuture<Void> cf = new CompletableFuture();
        con.commit(h -> wrap(cf, h));
        return cf;
    }

    <T> void wrap(CompletableFuture<T> cf, AsyncResult<T> res) {
        if (res.succeeded()) {
            cf.complete(res.result());
        } else {
            cf.completeExceptionally(res.cause());
        }
    }
}
