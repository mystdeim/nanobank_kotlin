package mystdeim.nanobank.util;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;

import java.util.concurrent.CompletableFuture;

public class DeployHelper {

    private DeployHelper() { }

    public static CompletableFuture<String> deploy(Vertx vertx, Class clazz, DeploymentOptions dopts) {
        CompletableFuture<String> cf = new CompletableFuture<>();
        vertx.deployVerticle(clazz, dopts, handler -> {
            if (handler.succeeded()) {
                cf.complete(handler.result());
            } else {
                cf.completeExceptionally(handler.cause());
            }
        });
        return cf;
    }
}
