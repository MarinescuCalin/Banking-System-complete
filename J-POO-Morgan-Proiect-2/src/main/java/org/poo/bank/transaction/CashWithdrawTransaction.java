package org.poo.bank.transaction;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public final class CashWithdrawTransaction extends Transaction {
    private final double amount;

    public CashWithdrawTransaction(final int timestamp, final String iban, final double amount) {
        super(timestamp, null, iban);

        this.description = "Cash withdrawal of " + amount;
        this.amount = amount;
    }

    @Override
    public ObjectNode toObjectNode(final ObjectMapper objectMapper) {
        final ObjectNode result = super.toObjectNode(objectMapper);

        result.put("amount", amount);

        return result;
    }
}
