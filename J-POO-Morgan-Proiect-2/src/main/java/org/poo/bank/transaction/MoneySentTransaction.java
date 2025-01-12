package org.poo.bank.transaction;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.HashSet;
import java.util.Set;

public final class MoneySentTransaction extends Transaction {
    private final double amount;
    private final String receiverIBAN;

    private final String currency;
    private final String type;

    public MoneySentTransaction(final int timestamp, final String description,
                                final double amount, final String receiverIBAN,
                                final String senderIBAN, final String currency,
                                final String type) {
        super(timestamp, description, senderIBAN);

        this.amount = amount;
        this.receiverIBAN = receiverIBAN;
        this.currency = currency;
        this.type = type;
    }

    @Override
    public ObjectNode toObjectNode(final ObjectMapper objectMapper) {
        ObjectNode result = super.toObjectNode(objectMapper);

        result.put("amount", amount + " " + currency);
        result.put("receiverIBAN", receiverIBAN);
        result.put("senderIBAN", iban);
        result.put("transferType", type);

        return result;
    }


    @Override
    public Set<String> getIBAN() {
        final Set<String> set = new HashSet<>();
        set.add(receiverIBAN);
        set.add(iban);
        return set;
    }
}
