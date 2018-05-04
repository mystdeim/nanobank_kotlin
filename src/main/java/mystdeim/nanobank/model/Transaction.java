package mystdeim.nanobank.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    public UUID id;
    public UUID src;
    public UUID dst;
    public BigDecimal vol;

}
