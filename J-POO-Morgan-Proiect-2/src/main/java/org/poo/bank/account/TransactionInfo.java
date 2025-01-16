package org.poo.bank.account;

import lombok.Getter;

@Getter
public class TransactionInfo {
    private final double amount;
    private final String username;
    private final int timestamp;

    public TransactionInfo(final double amount, final String username, final int timestamp) {
        this.amount = amount;
        this.username = username;
        this.timestamp = timestamp;
    }
}
