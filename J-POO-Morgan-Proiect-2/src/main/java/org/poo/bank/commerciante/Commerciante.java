package org.poo.bank.commerciante;

import lombok.Getter;
import org.poo.bank.account.Account;
import org.poo.fileio.CommerciantInput;


public class Commerciante {
    @Getter
    private final String name;
    private final int id;
    private final String iban;

    @Getter
    private final String type;
    private final CashbackStrategy cashback;

    public Commerciante(final String name, final int id, final String iban,
                        final String type, final String cashbackType) {
        this.name = name;
        this.id = id;
        this.iban = iban;
        this.type = type;
        if (cashbackType.equals("nrOfTransactions"))
            this.cashback = new NumberOfTransactionsStrategy();
        else
            this.cashback = new SpendingThresholdStrategy();
    }

    public Commerciante(final CommerciantInput commerciantInput) {
        this(commerciantInput.getCommerciant(), commerciantInput.getId(),
                commerciantInput.getAccount(), commerciantInput.getType(),
                commerciantInput.getCashbackStrategy());
    }

    public double getCashback(final Account account, final String planType, final double amount) {
        return cashback.getCashback(account, planType, this, amount);
    }
}
