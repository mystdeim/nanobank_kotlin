package mystdeim.nanobank.util;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.MessageCodec;
import mystdeim.nanobank.model.Account;

public class MessageCodecs {
    private MessageCodecs() { }

    public static void add(EventBus eventBus) {
        eventBus.registerDefaultCodec(Account.class, new AccountCodec());
    }

    static class AccountCodec implements MessageCodec<Account, Account> {
        @Override
        public void encodeToWire(Buffer buffer, Account account) { }
        @Override
        public Account decodeFromWire(int pos, Buffer buffer) {
            return null;
        }
        @Override
        public Account transform(Account account) {
            return account;
        }
        @Override
        public String name() {
            return this.getClass().getSimpleName();
        }
        @Override
        public byte systemCodecID() {
            return -1;
        }
    }
}
