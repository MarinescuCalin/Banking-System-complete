package org.poo.bank.account;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.poo.bank.exception.NotSavingsAccountException;

public final class SavingsAccount extends Account {
    private double interestRate;

    public SavingsAccount(final String currency, final String owner, final double interestRate) {
        super(currency, owner);
        this.interestRate = interestRate;
    }

    @Override
    public ObjectNode toObjectNode(final ObjectMapper objectMapper) {
        ObjectNode result = super.toObjectNode(objectMapper);

        result.put("type", "savings");

        return result;
    }

    @Override
    public void setInterestRate(final double interestRate) {
        this.interestRate = interestRate;
    }

    @Override
    public double addInterest() throws NotSavingsAccountException {
        final double interest = interestRate * getBalance();
        balance += interest;
        return interest;
    }

    @Override
    public String getType() {
        return "savings";
    }
}
