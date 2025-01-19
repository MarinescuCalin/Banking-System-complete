package org.poo.bank.account;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.poo.bank.Bank;
import org.poo.bank.User;
import org.poo.bank.exception.NotAuthorizedException;
import org.poo.bank.exception.NotSavingsAccountException;

import java.util.ArrayList;
import java.util.List;

public final class BusinessAccount extends Account {
    private double spendingLimit;
    private double depositLimit;
    private final List<String> managers;
    private final List<String> employees;
    private final List<TransactionInfo> transactionInfo;

    public BusinessAccount(final String currency, final String owner) {
        super(currency, owner);

        spendingLimit = Bank.getInstance().convertCurrency(500, "RON", currency);
        depositLimit = spendingLimit;

        this.managers = new ArrayList<>();
        this.employees = new ArrayList<>();
        this.transactionInfo = new ArrayList<>();
    }

    @Override
    public boolean addFunds(final double amount, final User user,
                            final int timestamp) {
        final String email = user.getEmail();

        if (owner.equals(email)) {
            return true;
        }

        if (!employees.contains(email) && !managers.contains(email)) {
            return false;
        }

        if (employees.contains(email)) {
            if (amount > depositLimit) {
                return false;
            }
        }

        addTransactionInfo(amount, email, timestamp, null);
        return true;
    }

    @Override
    public boolean removeFunds(final double amount, final User user,
                               final int timestamp, final String commerciante) {
        final String email = user.getEmail();

        if (owner.equals(email)) {
            return true;
        }

        if (!employees.contains(email) && !managers.contains(email)) {
            return false;
        }

        if (employees.contains(email)) {
            if (amount > spendingLimit) {
                return false;
            }
        }

        addTransactionInfo(-amount, email, timestamp, commerciante);
        return true;
    }

    @Override
    public void splitPay(final double amount, final String currency) {
        throw new UnsupportedOperationException();
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
    public List<String> getEmployees() {
        return employees;
    }

    @Override
    public List<String> getManagers() {
        return managers;
    }

    @Override
    public Double getSpendingLimit() {
        return spendingLimit;
    }

    @Override
    public Double getDepostLimit() {
        return depositLimit;
    }

    @Override
    public List<TransactionInfo> getTransasctionInfo() {
        return transactionInfo;
    }

    @Override
    public void addTransactionInfo(final double amount, final String email,
                                   final int timestamp, final String commerciante) {
        transactionInfo.add(new TransactionInfo(amount, email, timestamp, commerciante));
    }

    @Override
    public void addManager(final String email) {
        if (owner.equals(email)) {
            return;
        }

        if (employees.contains(email)) {
            return;
        }

        managers.add(email);
    }

    @Override
    public void addEmployee(final String email) {
        if (owner.equals(email)) {
            return;
        }

        if (managers.contains(email)) {
            return;
        }

        employees.add(email);
    }

    @Override
    public void changeSpendingLimit(final String email, final double limit)
            throws NotAuthorizedException {
        if (!email.equals(owner)) {
            throw new NotAuthorizedException();
        }

        spendingLimit = limit;
    }

    @Override
    public void changeDepositLimit(final String email, final double limit)
            throws NotAuthorizedException {
        if (!email.equals(owner)) {
            throw new NotAuthorizedException();
        }

        depositLimit = limit;
    }

    @Override
    public String getType() {
        return "business";
    }

    @Override
    public ObjectNode toObjectNode(final ObjectMapper objectMapper) {
        ObjectNode result = super.toObjectNode(objectMapper);

        result.put("type", "business");

        return result;
    }
}
