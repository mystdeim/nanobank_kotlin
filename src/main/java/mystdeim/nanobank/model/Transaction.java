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

    UUID id;
    UUID src;
    UUID dst;
    BigDecimal vol;

}
