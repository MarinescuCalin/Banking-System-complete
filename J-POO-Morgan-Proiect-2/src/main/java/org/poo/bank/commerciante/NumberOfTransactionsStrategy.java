package org.poo.bank.commerciante;

import org.poo.bank.account.Account;

public final class NumberOfTransactionsStrategy implements CashbackStrategy {
    private static final int TRANSACTIONS_THRESHOLD_10 = 10;
    private static final int TRANSACTIONS_THRESHOLD_5 = 5;
    private static final int TRANSACTIONS_THRESHOLD_2 = 2;
    private static final double PERCENTAGE_MULTIPLIER = 0.01;

    @Override
    public double getCashback(final Account account, final String planType,
                              final Commerciante commerciante, final double amount) {
        final int accountTransactions = account.getNoTransactions() + 1;
        account.setNoTransactions(accountTransactions);

        if (accountTransactions >= TRANSACTIONS_THRESHOLD_10
                && !account.getReceivedCashbacks().contains("Tech")) {
            account.getReceivedCashbacks().add("Tech");
            account.addCashback("Tech", new Cashback(TRANSACTIONS_THRESHOLD_10));
            return 0.0;
        }

        if (accountTransactions >= TRANSACTIONS_THRESHOLD_5
                && !account.getReceivedCashbacks().contains("Clothes")) {
            account.getReceivedCashbacks().add("Clothes");
            account.addCashback("Clothes", new Cashback(TRANSACTIONS_THRESHOLD_5));
            return 0.0;
        }

        if (accountTransactions >= TRANSACTIONS_THRESHOLD_2
                && !account.getReceivedCashbacks().contains("Food")) {
            account.getReceivedCashbacks().add("Food");
            account.addCashback("Food", new Cashback(TRANSACTIONS_THRESHOLD_2));
            return 0.0;
        }

        return 0.0;
    }
}
