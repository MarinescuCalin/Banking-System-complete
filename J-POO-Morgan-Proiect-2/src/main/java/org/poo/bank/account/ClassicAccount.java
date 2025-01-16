package org.poo.bank.account;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.poo.bank.exception.NotAuthorizedException;
import org.poo.bank.exception.NotSavingsAccountException;

import java.util.List;

public final class ClassicAccount extends Account {
    public ClassicAccount(final String currency, final String owner) {
        super(currency, owner);
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

    @Override
    public List<String> getEmployees() {
        return null;
    }

    @Override
    public List<String> getManagers() {
        return null;
    }

    @Override
    public Double getSpendingLimit() {
        return null;
    }

    @Override
    public Double getDepostLimit() {
        return null;
    }

    @Override
    public List<TransactionInfo> getTransasctionInfo() {
        return null;
    }

    @Override
    public void addTransactionInfo(final double amount, final String username, final int timestamp) {

    }

    @Override
    public void addManager(final String email) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addEmployee(final String email) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void changeSpendingLimit(final String email, final double limit)
            throws NotAuthorizedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void changeDepositLimit(final String email, final double limit)
            throws NotAuthorizedException {
        throw new UnsupportedOperationException();
    }
}
