package org.poo.bank;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import org.poo.bank.account.Account;
import org.poo.bank.card.Card;
import org.poo.bank.commerciante.Commerciante;
import org.poo.bank.exception.*;
import org.poo.bank.plan.GoldStrategy;
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
    private static final double MINIMUM_BALANCE = 30;
    private static final int STUDENT_SILVER_PLAN_COST = 100;
    private static final int STUDENT_GOLD_PLAN_COST = 350;
    private static final int SILVER_GOLD_PLAN_COST = 250;

    @Getter
    private final String firstName;
    @Getter
    private final String lastName;
    @Getter
    private final String email;
    @Getter
    private final String birthDate;
    private final String occupation;
    private int transactionsOver300;

    private PlanStrategy plan;
    private final Map<String, Account> accountsByIBAN;

    @Getter
    private final List<Transaction> transactions;

    private final Queue<SplitPayment> splitPayments;

    public User(final String firstName, final String lastName, final String email,
                final String birthDate, final String occupation) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.birthDate = birthDate;
        this.occupation = occupation;

        this.accountsByIBAN = new LinkedHashMap<>();
        this.transactions = new ArrayList<>();

        if (occupation.equals("student")) {
            plan = PlanFactory.createPlan("student");
        } else {
            plan = PlanFactory.createPlan("standard");
        }

        splitPayments = new LinkedList<>();
        this.transactionsOver300 = 0;
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

        if (timestamp == -1) {
            return;
        }

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
        createCardHelper(iban, timestamp, false);
    }

    /**
     * Creates a one-time card for the specified account and records the event as a transaction.
     *
     * @param iban      the IBAN of the account
     * @param timestamp the timestamp of card creation
     */
    public void createOneTimeCard(final String iban, final int timestamp) {
        createCardHelper(iban, timestamp, true);
    }

    private void createCardHelper(final String iban, final int timestamp,
                                  final boolean oneTime) {
        final Account account = accountsByIBAN.get(iban);
        if (account == null) {
            return;
        }

        final Card card = oneTime ? account.createOneTimeCard() : account.createCard();
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
     * @param cardNumber   the card number used for payment
     * @param amount       the amount to be paid
     * @param currency     the currency of the payment
     * @param timestamp    the timestamp of the payment
     * @param commerciante the merchant receiving the payment
     * @throws CardNotFoundException if the card cannot be found
     */
    public void payOnline(final String cardNumber, final double amount, final String currency,
                          final int timestamp, final Commerciante commerciante)
            throws CardNotFoundException {
        final Account account = findAccountWithCard(cardNumber);
        if (account == null) {
            throw new CardNotFoundException();
        }

        try {
            account.pay(this, account.getCard(cardNumber), amount,
                    currency, timestamp, commerciante);
        } catch (InsufficientFundsException | CardFrozenException e) {
            addTransaction(new Transaction(timestamp, e.getMessage(), account.getIban()));
        }
    }

    /**
     * Handles the process of sending money from one account to another.
     * The method performs fund transfer, handles currency conversion (if necessary),
     * and logs the transaction details for both the sender and receiver.
     *
     * @param senderIBAN      The IBAN of the sender's account.
     * @param receiverUser    The recipient user (if applicable).
     * @param receiverAccount The recipient's account (can be null for external transfers).
     * @param commerciante    The merchant involved in the transaction (if applicable).
     * @param receiverIBAN    The IBAN of the recipient's account.
     * @param amount          The amount of money to be sent.
     * @param timestamp       The timestamp of the transaction.
     * @param description     A description of the transaction.
     */
    public void sendMoney(final String senderIBAN, final User receiverUser,
                          final Account receiverAccount, final Commerciante commerciante,
                          final String receiverIBAN, final double amount, final int timestamp,
                          final String description) {
        final Account senderAccount = accountsByIBAN.get(senderIBAN);

        try {
            senderAccount.sendFunds(this, receiverAccount, commerciante, amount, timestamp);
        } catch (InsufficientFundsException e) {
            addTransaction(new Transaction(timestamp, e.getMessage(), senderIBAN));
            return;
        }

        double receiverAmount = amount;
        String receiverCurrency = senderAccount.getCurrency();
        if (receiverAccount != null) {
            if (!senderAccount.getCurrency().equals(receiverAccount.getCurrency())) {
                receiverAmount = Bank.getInstance().convertCurrency(amount,
                        senderAccount.getCurrency(),
                        receiverAccount.getCurrency());
            }

            receiverCurrency = receiverAccount.getCurrency();
        }

        addTransaction(new MoneySentTransaction(timestamp, description,
                amount, receiverIBAN, senderIBAN, senderAccount.getCurrency(), "sent"));

        if (receiverUser != null) {
            receiverUser.addTransaction(new MoneySentTransaction(timestamp, description,
                    receiverAmount, receiverIBAN, senderIBAN, receiverCurrency,
                    "received"));
        }
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

        if (balance <= MINIMUM_BALANCE) {
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
     * Initiates a split payment for a specified account.
     * This method retrieves the account by IBAN and triggers a split payment process.
     *
     * @param iban     the IBAN of the account initiating the split payment.
     * @param amount   the amount to be split.
     * @param currency the currency in which the payment will be made.
     */
    public void splitPay(final String iban, final double amount, final String currency) {
        final Account account = accountsByIBAN.get(iban);
        account.splitPay(amount, currency);
    }

    /**
     * Upgrades the plan of an account to a specified plan type.
     * The method checks if the account has sufficient funds to cover the upgrade cost.
     * If successful, the plan is upgraded and a transaction is recorded.
     *
     * @param iban      the IBAN of the account whose plan is to be upgraded.
     * @param planType  the type of plan to upgrade to (e.g., "silver", "gold").
     * @param timestamp the timestamp of the upgrade transaction.
     */
    public void upgradePlan(final String iban, final String planType, final int timestamp) {
        final String currentPlan = getPlanName();
        if (planType.equals(currentPlan)) {
            addTransaction(new Transaction(timestamp,
                    "The user already has the " + planType + " plan.", iban));
            return;
        }

        switch (currentPlan) {
            case "silver":
                if (planType.equals("standard") || planType.equals("student")) {
                    return;
                }
            case "gold":
                if (planType.equals("standard") || planType.equals("student")
                        || planType.equals("silver")) {
                    return;
                }
            default:
                break;
        }
        final Account account = accountsByIBAN.get(iban);
        double amount = calculateUpgradeCost(planType);

        amount = Bank.getInstance().convertCurrency(amount, "RON", account.getCurrency());

        if (account.getBalance() < amount) {
            addTransaction(new Transaction(timestamp, "Insufficient funds", iban));
            return;
        }

        account.decreaseBalance(amount);
        plan = PlanFactory.createPlan(planType);
        addTransaction(new UpgradePlanTransaction(timestamp, iban, planType));

        for (final Account acc : accountsByIBAN.values()) {
            acc.setSpending(0.0);
        }
    }

    /**
     * Calculates the cost of upgrading to a specified plan type.
     * The cost depends on the user's current plan and the target upgrade plan.
     *
     * @param planType the target plan type (e.g., "silver", "gold").
     * @return the cost of upgrading as a {@code double}. Returns 0 if no upgrade is applicable.
     */
    private double calculateUpgradeCost(final String planType) {
        return switch (getPlanName()) {
            case "standard", "student" -> planType.equals("silver") ? STUDENT_SILVER_PLAN_COST
                    : planType.equals("gold") ? STUDENT_GOLD_PLAN_COST : 0;
            case "silver" -> planType.equals("gold") ? SILVER_GOLD_PLAN_COST : 0;
            default -> 0;
        };
    }

    /**
     * Processes a cash withdrawal using a specified card.
     * This method checks if the card exists and if the associated account has sufficient funds.
     * If the card is frozen or not found, an exception is thrown.
     *
     * @param cardNumber the number of the card to be used for the withdrawal.
     * @param amount     the amount of cash to withdraw.
     * @param timestamp  the timestamp of the withdrawal transaction.
     * @throws CardNotFoundException if the card cannot be found in any user account.
     * @throws CardFrozenException   if the card is frozen and cannot be used for withdrawals.
     */
    public void cashWithdrawal(final String cardNumber, final double amount, final int timestamp)
            throws CardNotFoundException, CardFrozenException {
        final Account account = findAccountWithCard(cardNumber);
        if (account == null) {
            throw new CardNotFoundException();
        }

        try {
            account.cashWithdrawal(this, account.getCard(cardNumber), amount, timestamp);
        } catch (final InsufficientFundsException e) {
            addTransaction(new Transaction(timestamp, e.getMessage(), account.getIban()));
        }
    }

    /**
     * Finds and returns the account associated with a specified card number.
     * Iterates through the user's accounts to locate the account holding the card.
     *
     * @param cardNumber the number of the card to locate.
     * @return the {@link Account} that contains the card, or {@code null} if
     * no account holds the card.
     */
    private Account findAccountWithCard(final String cardNumber) {
        for (final Account account : accountsByIBAN.values()) {
            final Card card = account.getCard(cardNumber);
            if (card != null) {
                return account;
            }
        }

        return null;
    }

    /**
     * Adds a split payment to the queue of pending split payments for the user.
     * This allows the user to participate in shared transactions where costs are divided.
     *
     * @param splitPayment the {@link SplitPayment} to be added.
     */
    public void addSplitPayment(final SplitPayment splitPayment) {
        splitPayments.add(splitPayment);
    }

    /**
     * Accepts the current split payment at the front of the queue.
     * If no split payment exists, the method returns without making changes.
     * Accepting a split payment confirms the user's agreement to pay their share.
     */
    public void acceptSplitPayment() {
        final SplitPayment splitPayment = splitPayments.poll();

        if (splitPayment == null) {
            return;
        }

        splitPayment.update(true, email);
    }

    /**
     * Rejects the current split payment at the front of the queue.
     * If no split payment exists, the method returns without making changes.
     * Rejecting a split payment indicates the user does not agree to their share of the cost.
     */
    public void rejectSplitPayment() {
        final SplitPayment splitPayment = splitPayments.poll();

        if (splitPayment == null) {
            return;
        }

        splitPayment.update(false, email);
    }

    /**
     * Retrieves the name of the current plan associated with the user.
     * This plan determines the user's banking benefits, fees, and features.
     *
     * @return a {@link String} representing the name of the current plan
     * (e.g., "standard", "student", "gold").
     */
    public String getPlanName() {
        return plan.name();
    }

    /**
     * Calculates the commission fee for a specified transaction amount
     * on the user's current plan.
     * Different plans may have varying commission rates, which affect the final transaction cost.
     *
     * @param amount the transaction amount for which the commission is to be calculated.
     * @return a {@code double} representing the commission fee for the given amount.
     */
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


    public void increaseTransactionsOver300(final String iban, final int timestamp) {
        if (getPlanName().equals("gold")) {
            return;
        }

        transactionsOver300++;
        if (transactionsOver300 >= 5 && getPlanName().equals("silver")) {
            plan = new GoldStrategy();
            for (final Account acc : accountsByIBAN.values()) {
                if (acc.getOwner().equals(email)) {
                    acc.setSpending(0.0);
                }
            }

            addTransaction(new UpgradePlanTransaction(timestamp, iban, "gold"));
        }
    }

    @Override
    public ObjectNode toObjectNode(final ObjectMapper objectMapper) {
        ObjectNode result = objectMapper.createObjectNode();

        result.put("firstName", firstName);
        result.put("lastName", lastName);
        result.put("email", email);

        ArrayNode accountArray = result.putArray("accounts");
        for (final Account account : accountsByIBAN.values()) {
            if (!account.getOwner().equals(email)) {
                continue;
            }

            accountArray.add(account.toObjectNode(objectMapper));
        }

        return result;
    }
}
