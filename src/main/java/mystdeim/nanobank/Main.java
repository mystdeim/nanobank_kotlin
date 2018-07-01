package mystdeim.nanobank;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import mystdeim.nanobank.util.MessageCodecs;
import mystdeim.nanobank.verticle.AccountVerticle;
import mystdeim.nanobank.verticle.DBInitVerticle;
import mystdeim.nanobank.verticle.TransactionVerticle;
//import mystdeim.nanobank.verticle.WebVerticle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

import static mystdeim.nanobank.util.DeployHelper.deploy;


public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        new Main();
    }

    private Main() {
        Vertx vertx = Vertx.vertx();
        MessageCodecs.add(vertx.eventBus());
        getConfig(vertx)
                .thenApply(DeploymentOptions::new)
                .thenCompose(dopts ->
                    deploy(vertx, DBInitVerticle.class, dopts)
//                            .thenAccept(vertx::undeploy)
//                            .thenCompose(a -> deploy(vertx, WebVerticle.class, dopts))
                            .thenCompose(a -> deploy(vertx, AccountVerticle.class, dopts))
                            .thenCompose(a -> deploy(vertx, TransactionVerticle.class, dopts))
                ).exceptionally(ex -> {
                    log.error("Deploy failed: ", ex);
                    return null;
                });
    }

    CompletableFuture<JsonObject> getConfig(Vertx vertx) {
        CompletableFuture<JsonObject> cf = new CompletableFuture<>();
        ConfigStoreOptions fileStore = new ConfigStoreOptions()
                .setType("file")
                .setConfig(new JsonObject().put("path", "conf.json"));
        ConfigRetrieverOptions coptions = new ConfigRetrieverOptions()
                .addStore(fileStore);
        ConfigRetriever retriever = ConfigRetriever.create(vertx, coptions);
        retriever.getConfig(handler -> {
            if (handler.succeeded()) {
                cf.complete(new JsonObject().put("config", handler.result()));
            } else {
                cf.completeExceptionally(handler.cause());
            }
        });
        return cf;
    }
}
