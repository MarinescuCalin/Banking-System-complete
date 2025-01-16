package org.poo.bank.transaction;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;

public final class SplitPaymentTransaction extends Transaction {
    private final List<Double> amounts;
    private final String type;
    private final String currency;
    private final List<String> involvedAccounts;
    private final String error;

    public SplitPaymentTransaction(final int timestamp, final String description,
                                   final List<Double> amounts, final String type,
                                   final String currency, final List<String> accounts,
                                   final String error, final String iban) {
        super(timestamp, description, iban);

        this.amounts = amounts;
        this.type = type;
        this.currency = currency;
        this.involvedAccounts = accounts;
        this.error = error;
    }

    @Override
    public ObjectNode toObjectNode(final ObjectMapper objectMapper) {
        final ObjectNode result = super.toObjectNode(objectMapper);

        result.put("currency", currency);


        if (amounts.size() == 1) {
            result.put("amount", amounts.getFirst() );
        } else {
            final ArrayNode amountArr = objectMapper.createArrayNode();
            for (final Double amount : amounts) {
                amountArr.add(amount);
            }

            result.set("amountForUsers", amountArr);
        }

        if (error != null) {
            result.put("error", error);
        }

        result.put("splitPaymentType", type);

        ArrayNode array = objectMapper.createArrayNode();
        for (final String account : involvedAccounts) {
            array.add(account);
        }

        result.set("involvedAccounts", array);

        return result;
    }
}
