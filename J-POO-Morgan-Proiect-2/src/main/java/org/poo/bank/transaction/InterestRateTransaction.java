package org.poo.bank.transaction;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public final class InterestRateTransaction extends Transaction {
    private final double amount;
    private final String currency;

    public InterestRateTransaction(final int timestamp, final String iban, final double amount,
                                   final String currency) {
        super(timestamp, "Interest rate income", iban);

        this.amount = amount;
        this.currency = currency;
    }

    @Override
    public ObjectNode toObjectNode(final ObjectMapper objectMapper) {
        final ObjectNode objectNode = super.toObjectNode(objectMapper);

        objectNode.put("amount", amount);
        objectNode.put("currency", currency);

        return objectNode;
    }
}
