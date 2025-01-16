package org.poo.bank.account;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.poo.bank.exception.NotAuthorizedException;
import org.poo.bank.exception.NotSavingsAccountException;

import java.util.List;

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
    public Double getInterestRate() {
        return interestRate;
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
    public void changeSpendingLimit(String email, double limit) throws NotAuthorizedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void changeDepositLimit(String email, double limit) throws NotAuthorizedException {
        throw new UnsupportedOperationException();
    }
}
