package org.poo.bank.transaction;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.HashSet;
import java.util.Set;

public final class CardOperationTransaction extends Transaction {
    private final String email;
    private final String account;
    private final String cardNumber;

    public CardOperationTransaction(final int timestamp, final String description,
                                    final String email, final String account,
                                    final String cardNumber) {
        super(timestamp, description, account);

        this.email = email;
        this.account = account;
        this.cardNumber = cardNumber;
    }

    @Override
    public ObjectNode toObjectNode(final ObjectMapper objectMapper) {
        ObjectNode result = super.toObjectNode(objectMapper);

        result.put("account", account);
        result.put("card", cardNumber);
        result.put("cardHolder", email);

        return result;
    }

    @Override
    public Set<String> getIBAN() {
        final Set<String> set =  new HashSet<>();
        set.add(account);
        return set;
    }
}
