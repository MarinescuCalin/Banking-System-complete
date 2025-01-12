package org.poo.bank.transaction;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public final class OnlinePaymentTransaction extends Transaction {
    private final double amount;
    private final String commerciant;

    public OnlinePaymentTransaction(final int timestamp, final double amount, final String commerciant, final String iban) {
        super(timestamp, "Card payment", iban);

        this.amount = amount;
        this.commerciant = commerciant;
    }

    @Override
    public ObjectNode toObjectNode(final ObjectMapper objectMapper) {
        ObjectNode result = super.toObjectNode(objectMapper);

        result.put("amount", amount);
        result.put("commerciant", commerciant);

        return result;
    }

    @Override
    public String getCommerciant() {
        return commerciant;
    }

    @Override
    public Double getAmount() {
        return amount;
    }
}
