package org.poo.bank.transaction;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;

public final class SplitPaymentTransaction extends Transaction {
    private final double amount;
    private final String currency;
    private final List<String> involvedAccounts;
    private final String error;

    public SplitPaymentTransaction(final int timestamp, final String description,
                                   final double amount, final String currency,
                                   final List<String> accounts, final String error,
                                   final String IBAN) {
        super(timestamp, description, IBAN);

        this.amount = amount;
        this.currency = currency;
        this.involvedAccounts = accounts;
        this.error = error;
    }

    @Override
    public ObjectNode toObjectNode(final ObjectMapper objectMapper) {
        ObjectNode result = super.toObjectNode(objectMapper);

        result.put("currency", currency);
        result.put("amount", amount);
        if (error != null) {
            result.put("error", error);
        }

        ArrayNode array = objectMapper.createArrayNode();
        for (final String account : involvedAccounts) {
            array.add(account);
        }

        result.set("involvedAccounts", array);

        return result;
    }
}
