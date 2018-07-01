package mystdeim.nanobank.model;

import java.math.BigDecimal;
import java.util.UUID;

public class Account {
    public UUID id;
    public String person;
    public BigDecimal balance;

    public Account(UUID accountId, String tom, BigDecimal bigDecimal) {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getPerson() {
        return person;
    }

    public void setPerson(String person) {
        this.person = person;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
}
