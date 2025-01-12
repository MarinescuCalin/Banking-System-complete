package org.poo.bank.commerciante;

import lombok.Getter;
import org.poo.bank.account.Account;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class NumberOfTransactionsStrategy implements CashbackStrategy {
    @Getter
    private static final Set<String> cashbackReceived = new HashSet<>();
    @Getter
    private static final Map<String, Integer> noTransactions = new HashMap<>();

    @Override
    public double getCashback(final Account account, final String planType,
                              final Commerciante commerciante, final double amount) {
        if (cashbackReceived.contains(account.getIban())) {
            return 0.0;
        }

        final Cashback cashback = account.getCashbacks().get(commerciante.getType());
        if (cashback != null) {
            cashbackReceived.add(account.getIban());
            return cashback.getPercentage() * 0.01 * amount;
        }

        final int accountTransactions = noTransactions.getOrDefault(account.getIban(), 0);
        noTransactions.put(account.getIban(), accountTransactions + 1);
        if (accountTransactions >= 10) {
            account.addCashback("Tech", new Cashback(10));
            return 0.0;
        }

        if (accountTransactions >= 5) {
            account.addCashback("Clothes", new Cashback(5));
            return 0.0;
        }

        if (accountTransactions >= 2) {
            account.addCashback("Food", new Cashback(1));
            return 0.0;
        }

        return 0.0;
    }
}
