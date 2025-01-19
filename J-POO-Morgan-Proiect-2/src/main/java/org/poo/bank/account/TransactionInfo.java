package org.poo.bank.account;

import lombok.Getter;

@Getter
public class TransactionInfo {
    private final double amount;
    private final String email;
    private final int timestamp;
    private final String commerciante;

    public TransactionInfo(final double amount, final String email, final int timestamp,
                           final String commerciante) {
        this.amount = amount;
        this.email = email;
        this.timestamp = timestamp;
        this.commerciante = commerciante;
    }

    public TransactionInfo(final double amount, final String email, final int timestamp) {
        this(amount, email, timestamp, null);
    }
}
