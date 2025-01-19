package org.poo.bank.commerciante;

import lombok.Getter;
import org.poo.bank.account.Account;
import org.poo.fileio.CommerciantInput;


public final class Commerciante {
    @Getter
    private final String name;
    private final int id;
    private final String iban;

    @Getter
    private final String type;
    private final CashbackStrategy cashback;
    private double totalReceived;

    public Commerciante(final String name, final int id, final String iban,
                        final String type, final String cashbackType) {
        this.name = name;
        this.id = id;
        this.iban = iban;
        this.type = type;
        if (cashbackType.equals("nrOfTransactions")) {
            this.cashback = new NumberOfTransactionsStrategy();
        } else {
            this.cashback = new SpendingThresholdStrategy();
        }

        this.totalReceived = 0;
    }

    public Commerciante(final CommerciantInput commerciantInput) {
        this(commerciantInput.getCommerciant(), commerciantInput.getId(),
                commerciantInput.getAccount(), commerciantInput.getType(),
                commerciantInput.getCashbackStrategy());
    }

    /**
     * Calculates the cashback amount for a transaction based on the account, plan type,
     * and transaction amount.
     *
     * @param account  The account associated with the transaction. Must not be null.
     * @param planType The plan type of the account (e.g., "silver", "gold"). Must not be
     *                null or empty.
     * @param amount   The transaction amount. Must be greater than 0.
     * @return The calculated cashback amount as a {@code double}.
     */
    public double getCashback(final Account account, final String planType, final double amount) {
        return cashback.getCashback(account, planType, this, amount);
    }
}
