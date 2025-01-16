package org.poo.bank.commerciante;

import org.poo.bank.account.Account;

/**
 * Interface representing a strategy for calculating cashback.
 * Different strategies may apply different rules for calculating
 * the cashback amount based on various factors such as account, plan type,
 * merchant, and transaction amount.
 */
public interface CashbackStrategy {
    /**
     * Calculates the cashback amount based on the provided account, plan type, merchant,
     * and transaction amount.
     *
     * @param account      the {@link Account} that is making the transaction.
     * @param planType     the type of plan the user is subscribed to (e.g., premium, standard).
     * @param commerciante the {@link Commerciante} (merchant) associated with the transaction.
     * @param amount       the transaction amount (before cashback).
     * @return the cashback amount to be applied to the transaction.
     */
    double getCashback(Account account, String planType, Commerciante commerciante, double amount);

}
