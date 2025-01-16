package org.poo.bank.commerciante;

import lombok.Getter;
import org.poo.bank.account.Account;

import java.util.HashMap;
import java.util.Map;

public final class NumberOfTransactionsStrategy implements CashbackStrategy {
    private static final int TRANSACTIONS_THRESHOLD_10 = 10;
    private static final int TRANSACTIONS_THRESHOLD_5 = 5;
    private static final int TRANSACTIONS_THRESHOLD_2 = 2;
    private static final double PERCENTAGE_MULTIPLIER = 0.01;

    @Getter
    private static final Map<String, Integer> noTransactions = new HashMap<>();

    @Override
    public double getCashback(final Account account, final String planType,
                              final Commerciante commerciante, final double amount) {
        if (account.isCashbackReceived()) {
            return 0.0;
        }

        final Cashback cashback = account.getCashbacks().get(commerciante.getType());
        if (cashback != null) {
            account.setCashbackReceived(true);
            return cashback.getPercentage() * PERCENTAGE_MULTIPLIER * amount;
        }

        final int accountTransactions = noTransactions.getOrDefault(account.getIban(), 0);
        noTransactions.put(account.getIban(), accountTransactions + 1);
        if (accountTransactions >= TRANSACTIONS_THRESHOLD_10) {
            account.addCashback("Tech", new Cashback(TRANSACTIONS_THRESHOLD_10));
            return 0.0;
        }

        if (accountTransactions >= TRANSACTIONS_THRESHOLD_5) {
            account.addCashback("Clothes", new Cashback(TRANSACTIONS_THRESHOLD_5));
            return 0.0;
        }

        if (accountTransactions >= TRANSACTIONS_THRESHOLD_2) {
            account.addCashback("Food", new Cashback(TRANSACTIONS_THRESHOLD_2));
            return 0.0;
        }

        return 0.0;
    }
}
