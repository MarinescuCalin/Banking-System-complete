package org.poo.bank.commerciante;

import org.poo.bank.account.Account;

public interface CashbackStrategy {
    double getCashback(final Account account, final String planType,
                       final Commerciante commerciante, final double amount);

}
