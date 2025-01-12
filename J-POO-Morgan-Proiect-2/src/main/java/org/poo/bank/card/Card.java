package org.poo.bank.card;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.Setter;
import org.poo.fileio.JSONWritable;
import org.poo.utils.Utils;

@Getter
public abstract class Card implements JSONWritable {
    protected final String cardNumber;
    protected final boolean oneTime;

    @Setter
    protected String status;

    public Card(final boolean oneTime) {
        this.cardNumber = Utils.generateCardNumber();
        this.status = "active";

        this.oneTime = oneTime;
    }

    @Override
    public ObjectNode toObjectNode(final ObjectMapper objectMapper) {
        ObjectNode result = objectMapper.createObjectNode();

        result.put("cardNumber", cardNumber);
        result.put("status", status);

        return result;
    }
}
