package org.poo.bank;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import org.poo.bank.account.Account;
import org.poo.bank.card.Card;
import org.poo.bank.commerciante.Commerciante;
import org.poo.bank.exception.*;
import org.poo.bank.plan.PlanFactory;
import org.poo.bank.plan.PlanStrategy;
import org.poo.bank.transaction.*;
import org.poo.fileio.JSONWritable;
import org.poo.fileio.UserInput;

import java.util.*;

/**
 * Represents a user in the banking system. Each user has personal information,
 * a collection of bank accounts, and a transaction history. This class provides
 * functionality for managing accounts, cards, and transactions.
 */
public final class User implements JSONWritable {
    @Getter
    private final String firstName;
    @Getter
    private final String lastName;
    @Getter
    private final String email;
    @Getter
    private final String birthDate;
    private final String occupation;

    private PlanStrategy plan;
    private final Map<String, Account> accountsByIBAN;

    @Getter
    private final List<Transaction> transactions;

    public User(final String firstName, final String lastName, final String email,
                final String birthDate, final String occupation) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.birthDate = birthDate;
        this.occupation = occupation;

        this.accountsByIBAN = new LinkedHashMap<>();
        this.transactions = new ArrayList<>();

        if (occupation.equals("student"))
            plan = PlanFactory.createPlan("student");
        else
            plan = PlanFactory.createPlan("standard");
    }

    public User(final UserInput userInput) {
        this(userInput.getFirstName(), userInput.getLastName(), userInput.getEmail(),
                userInput.getBirthDate(), userInput.getOccupation());
    }

    /**
     * Adds a new account to the user and records the event as a transaction.
     *
     * @param account   the account to be added
     * @param timestamp the timestamp of the account creation
     */
    public void addAccount(final Account account, final int timestamp) {
        accountsByIBAN.put(account.getIban(), account);

        transactions.add(new Transaction(timestamp, "New account created", account.getIban()));
    }

    /**
     * Returns a list of accounts owned by the user.
     *
     * @return a list of accounts
     */
    public List<Account> getAccounts() {
        return new ArrayList<>(accountsByIBAN.values());
    }

    /**
     * Retrieves an account by its IBAN.
     *
     * @param iban the IBAN of the account
     * @return the account associated with the IBAN, or {@code null} if not found
     */
    public Account getAccount(final String iban) {
        return accountsByIBAN.get(iban);
    }

    /**
     * Removes an account by its IBAN if its balance is zero.
     * Throws an exception if the balance is not empty.
     *
     * @param iban the IBAN of the account to be removed
     * @throws BalanceNotEmptyException if the account has a non-zero balance
     */
    public void removeAccount(final String iban) throws BalanceNotEmptyException {
        if (accountsByIBAN.get(iban).getBalance() != 0) {
            throw new BalanceNotEmptyException();
        }

        accountsByIBAN.remove(iban);
    }

    /**
     * Creates a new card for the specified account and records the event as a transaction.
     *
     * @param iban      the IBAN of the account
     * @param timestamp the timestamp of card creation
     */
    public void createCard(final String iban, final int timestamp) {
        final Account account = accountsByIBAN.get(iban);
        if (account == null) {
            return;
        }

        final Card card = account.createCard();

        transactions.add(new CardOperationTransaction(timestamp, "New card created",
                email, iban, card.getCardNumber()));
    }

    /**
     * Creates a one-time card for the specified account and records the event as a transaction.
     *
     * @param iban      the IBAN of the account
     * @param timestamp the timestamp of card creation
     */
    public void createOneTimeCard(final String iban, final int timestamp) {
        final Account account = accountsByIBAN.get(iban);
        if (account == null) {
            return;
        }

        final Card card = account.createOneTimeCard();

        transactions.add(new CardOperationTransaction(timestamp, "New card created",
                email, iban, card.getCardNumber()));
    }

    /**
     * Removes a card by its number and records the event as a transaction.
     *
     * @param cardNumber the card number to be removed
     * @param timestamp  the timestamp of card removal
     */
    public void removeCard(final String cardNumber, final int timestamp) {
        for (final Account account : accountsByIBAN.values()) {
            Card card = account.getCard(cardNumber);
            if (card != null) {
                account.removeCard(cardNumber);
                transactions.add(new CardOperationTransaction(timestamp,
                        "The card has been destroyed", email, account.getIban(), cardNumber));
                return;
            }
        }
    }

    /**
     * Handles an online payment using a specified card and records the transaction.
     * Throws an exception if the card is not found.
     *
     * @param cardNumber  the card number used for payment
     * @param amount      the amount to be paid
     * @param currency    the currency of the payment
     * @param timestamp   the timestamp of the payment
     * @param commerciante the merchant receiving the payment
     * @throws CardNotFoundException if the card cannot be found
     */
    public void payOnline(final String cardNumber, final double amount, final String currency,
                          final int timestamp, final Commerciante commerciante)
            throws CardNotFoundException {
        for (final Account account : accountsByIBAN.values()) {
            final Card card = account.getCard(cardNumber);

            if (card != null) {
                try {
                    account.pay(this, card, amount, currency, timestamp, commerciante);
                } catch (InsufficientFundsException | CardFrozenException e) {
                    addTransaction(new Transaction(timestamp, e.getMessage(), account.getIban()));
                    return;
                }
                return;
            }
        }

        throw new CardNotFoundException();
    }


    /**
     * Transfers money from the sender's account to the receiver's account.
     * Creates and records transactions for both the sender and the receiver.
     *
     * @param senderIBAN      the IBAN of the sender's account
     * @param receiverUser    the user receiving the funds
     * @param receiverAccount the account of the receiver
     * @param receiverIBAN    the IBAN of the receiver's account
     * @param amount          the amount to be transferred
     * @param timestamp       the timestamp of the transaction
     * @param description     the description of the transaction
     */
    public void sendMoney(final String senderIBAN, final User receiverUser,
                          final Account receiverAccount, final String receiverIBAN,
                          final double amount, final int timestamp, final String description) {
        final Account senderAccount = accountsByIBAN.get(senderIBAN);

        try {
            senderAccount.sendFunds(this, receiverUser.getAccount(receiverIBAN), amount);
        } catch (InsufficientFundsException e) {
            addTransaction(new Transaction(timestamp, e.getMessage(), senderIBAN));
            return;
        }

        double receiverAmount = amount;
        if (!senderAccount.getCurrency().equals(receiverAccount.getCurrency())) {
            receiverAmount = Bank.getInstance().convertCurrency(amount,
                    senderAccount.getCurrency(),
                    receiverAccount.getCurrency());
        }

        addTransaction(new MoneySentTransaction(timestamp, description,
                amount, receiverIBAN, senderIBAN, senderAccount.getCurrency(), "sent"));
        receiverUser.addTransaction(new MoneySentTransaction(timestamp, description,
                receiverAmount, receiverIBAN, senderIBAN, receiverAccount.getCurrency(),
                "received"));
    }

    /**
     * Checks if a card should be frozen based on the account's balance.
     * If the balance is below the minimum required, the card is frozen,
     * and a transaction is recorded.
     *
     * @param account   the account associated with the card
     * @param card      the card to be checked
     * @param timestamp the timestamp of the check
     */
    public void checkFreezeCard(final Account account, final Card card, final int timestamp) {
        double balance = account.getBalance();

        if (balance <= 30) {
            addTransaction(new Transaction(timestamp,
                    "You have reached the minimum amount of funds, the card will be frozen",
                    account.getIban()));
            card.setStatus("frozen");
            return;
        }

        Optional<Double> minBalance = account.getMinBalance();

        if (minBalance.isEmpty()) {
            return;
        }

        double min = minBalance.get();
        if (balance <= min) {
            addTransaction(new Transaction(timestamp, "Card is frozen", account.getIban()));
            card.setStatus("frozen");
        }
    }

    /**
     * Changes the interest rate for a specified account.
     * This operation is only applicable to savings accounts and records the change
     * as a transaction.
     *
     * @param iban         the IBAN of the account
     * @param interestRate the new interest rate to be applied
     * @param timestamp    the timestamp of the operation
     * @throws NotSavingsAccountException if the account is not a savings account
     */
    public void changeInterestRate(final String iban, final double interestRate,
                                   final int timestamp)
            throws NotSavingsAccountException {
        final Account account = accountsByIBAN.get(iban);
        account.setInterestRate(interestRate);

        addTransaction(new Transaction(timestamp, "Interest rate of the account changed to "
                + interestRate, account.getIban()));
    }


    /**
     * Splits a payment from the specified account among multiple accounts or individuals.
     * Records the split payment transaction with details of the original payment and participants.
     *
     * @param iban             the IBAN of the payer's account
     * @param amount           the portion of the original payment to be deducted from the account
     * @param originalAmount   the total amount of the original payment
     * @param originalCurrency the currency of the original payment
     * @param involvedAccounts a list of IBANs of accounts involved in the payment
     * @param description      the description of the split payment
     * @param timestamp        the timestamp of the transaction
     */
    public void splitPay(final String iban, final double amount, final double originalAmount,
                         final String originalCurrency, final List<String> involvedAccounts,
                         final String description, final int timestamp) {
        final Account account = accountsByIBAN.get(iban);

        account.splitPay(amount);
        addTransaction(new SplitPaymentTransaction(timestamp, description, originalAmount,
                originalCurrency, involvedAccounts, null, account.getIban()));
    }

    public void upgradePlan(final String iban, final String planType, final int timestamp) throws InsufficientFundsException {
        final Account account = accountsByIBAN.get(iban);
        final String currentPlan = getPlanName();

        double amount = 0;
        if (currentPlan.equals("standard") || currentPlan.equals("student")) {
            if (planType.equals("silver"))
                amount = 100;
            else if (planType.equals("gold"))
                amount = 350;
        } else if (currentPlan.equals("silver") && planType.equals("gold"))
            amount = 250;

        final String currency = account.getCurrency();
        if (!currency.equals("RON")) {
            amount = Bank.getInstance().convertCurrency(amount, "RON", currency);
        }

        if (account.getBalance() < amount) {
            throw new InsufficientFundsException();
        }

        account.removeFunds(amount);
        plan = PlanFactory.createPlan(planType);
        addTransaction(new UpgradePlanTransaction(timestamp, iban, planType));
    }

    public void cashWithdrawal(final String cardNumber, final double amount, final int timestamp)
            throws CardNotFoundException, InsufficientFundsException, CardFrozenException {
        for (final Account account : accountsByIBAN.values()) {
            final Card card = account.getCard(cardNumber);
            if (card != null) {
                account.cashWithdrawal(this, card, amount, timestamp);
                return;
            }
        }

        throw new CardNotFoundException();
    }

    public String getPlanName() {
        return plan.name();
    }

    public double getCommission(final double amount) {
        return plan.getComision(amount);
    }

    /**
     * Adds a transaction to the user's transaction history.
     *
     * @param transaction the transaction to be added
     */
    public void addTransaction(final Transaction transaction) {
        transactions.add(transaction);
    }

    @Override
    public ObjectNode toObjectNode(final ObjectMapper objectMapper) {
        ObjectNode result = objectMapper.createObjectNode();

        result.put("firstName", firstName);
        result.put("lastName", lastName);
        result.put("email", email);

        ArrayNode accountArray = result.putArray("accounts");
        for (final Account account : accountsByIBAN.values())
            accountArray.add(account.toObjectNode(objectMapper));

        return result;
    }
}
