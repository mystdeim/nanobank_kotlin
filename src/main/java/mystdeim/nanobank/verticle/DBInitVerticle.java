package mystdeim.nanobank.verticle;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.sql.SQLClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class DBInitVerticle extends JDBCVerticle {

    private static final Logger log = LoggerFactory.getLogger(DBInitVerticle.class);

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        SQLClient client = sqlClient();
        // It's fast don't care about event loop
        String creation = vertx.fileSystem().readFileBlocking("db.sql").toString();
        query(creation).thenAccept(r -> {

            log.info("DB was created");
            startFuture.complete();
//            sqlClient().queryWithParams(
//                    "insert into account (id, person) values (?, ?)",
//                    new JsonArray().add(UUID.randomUUID().toString()).add("aaa"),h -> {
//                if (h.succeeded()) {
//
//                }
//            });
//            sqlClient().queryWithParams(
//                    "insert into account (id, person) values ('acaa4b02-0c8c-4157-a871-2da8f6bf5fe6', 'a')",
//                    new JsonArray(),h -> {
//                        if (h.succeeded()) {
//
//                        }
//                    });
//            sqlClient().queryWithParams(
//                    "select * from account",
//                    new JsonArray(),h -> {
//                        if (h.succeeded()) {
//
//                        }
//                    });
        });
        log.info("DB was deployed");
        System.nanoTime();
    }

    @Override
    public void stop() throws Exception {
        log.info("DB was undeployed");
    }
}
