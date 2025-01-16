package org.poo.bank.transaction;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class SavingsWithdrawTransaction extends Transaction {
    private final String savingsAccountIBAN;
    private final double amount;

    public SavingsWithdrawTransaction(final int timestamp, final String classicAccountIBAN,
                                      final String savingsAccountIBAN, final double amount) {
        super(timestamp, "Savings withdrawal", classicAccountIBAN);

        this.savingsAccountIBAN = savingsAccountIBAN;
        this.amount = amount;
    }

    @Override
    public ObjectNode toObjectNode(final ObjectMapper objectMapper) {
        final ObjectNode result = super.toObjectNode(objectMapper);

        result.put("amount", amount);
        result.put("classicAccountIBAN", iban);
        result.put("savingsAccountIBAN", savingsAccountIBAN);

        return result;
    }
}
