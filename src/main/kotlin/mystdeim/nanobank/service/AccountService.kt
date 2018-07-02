package mystdeim.nanobank.service

import io.vertx.codegen.annotations.ProxyGen
import io.vertx.codegen.annotations.VertxGen
import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import mystdeim.nanobank.data.Account

@ProxyGen
@VertxGen
interface AccountService {
    fun create(account: Account, resultHandler: Handler<AsyncResult<String>>)

}