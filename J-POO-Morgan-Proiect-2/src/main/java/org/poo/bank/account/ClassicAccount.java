package org.poo.bank.account;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.poo.bank.exception.NotSavingsAccountException;

public final class ClassicAccount extends Account {
    public ClassicAccount(final String currency) {
        super(currency);
    }

    @Override
    public ObjectNode toObjectNode(final ObjectMapper objectMapper) {
        ObjectNode result = super.toObjectNode(objectMapper);

        result.put("type", "classic");

        return result;
    }

    @Override
    public void setInterestRate(final double interestRate) throws NotSavingsAccountException {
        throw new NotSavingsAccountException();
    }

    @Override
    public double addInterest() throws NotSavingsAccountException {
        throw new NotSavingsAccountException();
    }

    @Override
    public Double getInterestRate() {
        return null;
    }
}
