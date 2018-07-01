package mystdeim.nanobank.model;

import java.math.BigDecimal;
import java.util.UUID;

public class Transaction {

    public UUID id;
    public UUID src;
    public UUID dst;
    public BigDecimal vol;

    public Transaction(Object o, UUID account1, UUID account2, BigDecimal bigDecimal) {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getSrc() {
        return src;
    }

    public void setSrc(UUID src) {
        this.src = src;
    }

    public UUID getDst() {
        return dst;
    }

    public void setDst(UUID dst) {
        this.dst = dst;
    }

    public BigDecimal getVol() {
        return vol;
    }

    public void setVol(BigDecimal vol) {
        this.vol = vol;
    }
}
