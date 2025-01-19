package org.poo.bank.account;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.Setter;
import org.poo.bank.Bank;
import org.poo.bank.User;
import org.poo.bank.card.Card;
import org.poo.bank.card.ClassicCard;
import org.poo.bank.card.OneTimePayCard;
import org.poo.bank.commerciante.Cashback;
import org.poo.bank.commerciante.Commerciante;
import org.poo.bank.exception.CardFrozenException;
import org.poo.bank.exception.InsufficientFundsException;
import org.poo.bank.exception.NotAuthorizedException;
import org.poo.bank.exception.NotSavingsAccountException;
import org.poo.bank.transaction.CardOperationTransaction;
import org.poo.bank.transaction.CashWithdrawTransaction;
import org.poo.bank.transaction.OnlinePaymentTransaction;
import org.poo.fileio.JSONWritable;
import org.poo.utils.Utils;

import java.util.*;


/**
 * Abstract class representing a bank account.
 * Manages funds, cards, and operations such as payments, transfers, and balance adjustments.
 */
@Getter
public abstract class Account implements JSONWritable {
    protected final String iban;
    protected final String currency;
    protected final String owner;

    protected Double balance;
    protected Optional<Double> minBalance;

    @Setter
    protected Map<String, Card> cardsByNumber;

    protected final Map<String, Cashback> cashbacks;
    protected final Set<String> receivedCashbacks;

    @Setter
    protected double spending;

    @Setter
    protected int noTransactions;

    public Account(final String currency, final String owner) {
        this.iban = Utils.generateIBAN();
        this.currency = currency;
        this.owner = owner;

        this.balance = 0.0;
        this.cardsByNumber = new LinkedHashMap<>();
        this.minBalance = Optional.empty();

        this.cashbacks = new HashMap<>();
        this.receivedCashbacks = new HashSet<>();
        this.noTransactions = 0;
        this.spending = 0;
    }

    /**
     * Creates a new {@link ClassicCard} and associates it with this account.
     *
     * @return the newly created card.
     */
    public Card createCard() {
        final Card card = new ClassicCard();
        cardsByNumber.put(card.getCardNumber(), card);

        return card;
    }

    /**
     * Retrieves the card associated with the specified card number.
     *
     * @param cardNumber the card number.
     * @return the {@link Card} associated with the card number, or {@code null} if not found.
     */
    public Card getCard(final String cardNumber) {
        return cardsByNumber.get(cardNumber);
    }


    /**
     * Removes the card associated with the specified card number from this account.
     *
     * @param cardNumber the card number to remove.
     */
    public void removeCard(final String cardNumber) {
        cardsByNumber.remove(cardNumber);
    }

    /**
     * Creates a new {@link OneTimePayCard} and associates it with this account.
     *
     * @return the newly created one-time payment card.
     */
    public Card createOneTimeCard() {
        final Card card = new OneTimePayCard();
        cardsByNumber.put(card.getCardNumber(), card);

        return card;
    }


    /**
     * Adds funds to the specified user's account.
     *
     * @param amount    The amount to be added. Must be greater than 0.
     * @param user      The user whose account will receive the funds. Must not be null.
     * @param timestamp The timestamp of the transaction, represented as an integer.
     * @return {@code true} if the funds were successfully added; {@code false} otherwise.
     */
    public boolean addFunds(final double amount, final User user,
                            final int timestamp) {
        return true;
    }

    /**
     * Removes funds from the specified user's account.
     *
     * @param amount    The amount to be removed. Must be greater than 0.
     * @param user      The user whose account will be debited. Must not be null.
     * @param timestamp The timestamp of the transaction, represented as an integer.
     * @return {@code true} if the funds were successfully removed; {@code false} otherwise.
     */
    public boolean removeFunds(final double amount, final User user,
                               final int timestamp, final String commerciante) {
        return true;
    }

    /**
     * Increases the balance by the specified amount.
     *
     * @param amount The amount to add to the balance. Must be greater than 0.
     */
    public void increaseBalance(final double amount) {
        balance += amount;
    }

    /**
     * Decreases the balance by the specified amount.
     *
     * @param amount The amount to subtract from the balance. Must be greater than 0.
     *               Ensure the resulting balance does not go below allowed limits.
     */
    public void decreaseBalance(final double amount) {
        balance -= amount;
    }

    /**
     * Transfers funds from the current account to a specified receiver account or
     * updates the cashback
     * for a merchant.
     * <p>
     * The method performs currency conversion if necessary, applies any applicable cashback,
     * deducts the required balance including commission fees, and ensures sufficient funds are
     * available
     * before completing the transaction. If the transferred amount (in RON) is 300 or greater,
     * it tracks such transactions for the account owner.
     * </p>
     *
     * @param user          the {@link User} initiating the transfer.
     * @param receiver      the {@link Account} receiving the funds. If null, no funds are
     *                     transferred to another account.
     * @param commerciante  the {@link Commerciante} involved in the transaction. If null,
     *                     no cashback is applied.
     * @param amount        the amount to transfer, in the current account's currency.
     * @param timestamp     the timestamp of the transaction.
     * @throws InsufficientFundsException if the account balance is insufficient to
     * cover the amount,
     *                                    including commission fees.
     */
    public void sendFunds(final User user,
                          final Account receiver,
                          final Commerciante commerciante,
                          final double amount,
                          final int timestamp)
            throws InsufficientFundsException {
        double amountToGet = amount;
        if (receiver != null) {
            if (!currency.equals(receiver.getCurrency())) {
                amountToGet = Bank.getInstance()
                        .convertCurrency(amount,
                                this.currency,
                                receiver.getCurrency());
            }
        }

        final double amountInRON = Bank.getInstance().convertCurrency(amount, currency, "RON");

        final User ownerUser = Bank.getInstance().getUserByEmail(getOwner());

        double amountToSubstract = amount + Bank.getInstance()
                .convertCurrency(ownerUser.getCommission(amountInRON), "RON", currency);
        if (balance - amountToSubstract < 0.0) {
            throw new InsufficientFundsException();
        }

        if (commerciante != null) {
            final Cashback cashback = cashbacks.get(commerciante.getType());
            double amountToCashback = 0.0;
            if (cashback != null) {
                amountToCashback += cashback.getPercentage() * 0.01 * amountInRON;
                cashbacks.remove(commerciante.getType());
            }

            amountToCashback += commerciante.getCashback(this, ownerUser.getPlanName(),
                    amountInRON);
            final double amountToAdd = Bank.getInstance().convertCurrency(amountToCashback,
                    "RON", currency);
            balance += amountToAdd;
        }

        balance -= amountToSubstract;
        if (receiver != null) {
            receiver.balance += amountToGet;
        }

        if (amountInRON >= 300) {
            ownerUser.increaseTransactionsOver300(iban, timestamp);
        }
    }


    /**
     * Performs a payment operation using the specified card.
     * Adjusts balance, handles currency conversion, and supports one-time payment cards.
     *
     * @param user          the user making the payment.
     * @param card          the card used for payment.
     * @param amount        the amount to pay.
     * @param otherCurrency the currency of the payment.
     * @param timestamp     the timestamp of the payment.
     * @param commerciante  the merchant receiving the payment.
     * @throws InsufficientFundsException if there are insufficient funds.
     * @throws CardFrozenException        if the card is frozen.
     */
    public void pay(final User user, final Card card,
                    final double amount, final String otherCurrency,
                    final int timestamp, final Commerciante commerciante)
            throws InsufficientFundsException, CardFrozenException {
        if (card.getStatus().equals("frozen")) {
            throw new CardFrozenException("The card is frozen");
        }

        double amountToPay = Bank.getInstance().convertCurrency(amount, otherCurrency, currency);
        final double amountInRON = Bank.getInstance().convertCurrency(amountToPay, currency, "RON");

        final User owner = Bank.getInstance().getUserByEmail(getOwner());

        final double amountToSubstract = amountToPay + Bank.getInstance()
                .convertCurrency(owner.getCommission(amountInRON), "RON", currency);

        if (balance - amountToSubstract < 0) {
            throw new InsufficientFundsException();
        }

        if (minBalance.isPresent()) {
            final double afterBalance = balance - amountToSubstract;

            if (Math.abs(afterBalance - minBalance.get()) <= 30) {
                card.setStatus("frozen");
                throw new CardFrozenException("You have reached the minimum amount of funds,"
                        + " the card will be frozen");
            } else if (afterBalance <= minBalance.get()) {
                card.setStatus("frozen");
                throw new CardFrozenException("Card is frozen");
            }
        }

        final boolean can = removeFunds(amountToPay, user, timestamp, commerciante.getName());
        if (!can) {
            return;
        }

        balance -= amountToSubstract;

        if (commerciante != null) {
            final Cashback cashback = cashbacks.get(commerciante.getType());
            double amountToCashback = 0.0;
            if (cashback != null) {
                amountToCashback = cashback.getPercentage() * 0.01 * amountInRON;
                cashbacks.remove(commerciante.getType());
            }

            amountToCashback += commerciante.getCashback(this, owner.getPlanName(), amountInRON);
            final double amountToAdd = Bank.getInstance().convertCurrency(amountToCashback,
                    "RON", currency);
            balance += amountToAdd;
        }

        user.addTransaction(new OnlinePaymentTransaction(timestamp,
                amountToPay, commerciante.getName(), iban));

        if (card.isOneTime()) {
            user.addTransaction(new CardOperationTransaction(timestamp,
                    "The card has been destroyed",
                    user.getEmail(), iban, card.getCardNumber()));
            removeCard(card.getCardNumber());
            final Card newCard = createOneTimeCard();

            user.addTransaction(new CardOperationTransaction(timestamp,
                    "New card created",
                    user.getEmail(), iban, newCard.getCardNumber()));
        }

        if (amountInRON >= 300) {
            owner.increaseTransactionsOver300(iban, timestamp);
        }
    }

    /**
     * Performs a cash withdrawal from the account using the specified card.
     * Converts the withdrawal amount to the account's currency (if needed),
     * checks if the funds are sufficient,
     * and ensures the card is not frozen before processing the withdrawal.
     * Adds a transaction record for the withdrawal after completing the operation.
     *
     * @param user      The user performing the withdrawal.
     * @param card      The card used for the withdrawal.
     * @param amount    The amount to withdraw in the card's currency.
     * @param timestamp The timestamp of the withdrawal operation.
     * @throws InsufficientFundsException if the account balance is insufficient
     *                                    to cover the withdrawal.
     * @throws CardFrozenException        if the card is frozen and cannot
     *                                    be used for the withdrawal.
     */
    public void cashWithdrawal(final User user, final Card card, final double amount,
                               final int timestamp) throws InsufficientFundsException,
            CardFrozenException {

        final User ownerUser = Bank.getInstance().getUserByEmail(getOwner());

        double withdrawAmount = Bank.getInstance().convertCurrency(amount, "RON", currency);
        withdrawAmount += Bank.getInstance().convertCurrency(ownerUser.getCommission(amount),
                "RON", currency);

        if (balance < withdrawAmount) {
            throw new InsufficientFundsException();
        }

        if (card.getStatus().equals("frozen")) {
            throw new CardFrozenException("The card is frozen");
        }
        balance -= withdrawAmount;

        user.addTransaction(new CashWithdrawTransaction(timestamp, iban, amount));
    }

    /**
     * Adds a cashback rule for a specific merchant type to the account.
     * The cashback rule determines the percentage of cashback the account holder will receive
     * when making transactions with merchants of the specified type.
     *
     * @param commerciantType The type of the merchant (e.g., retail, online store).
     * @param cashback        The cashback object containing the percentage value.
     */
    public void addCashback(final String commerciantType, final Cashback cashback) {
        cashbacks.put(commerciantType, cashback);
    }

    /**
     * Performs a split payment by converting the specified amount to the account's currency and
     * deducting it from the balance. This method is typically used when paying in installments
     * or in multiple parts.
     *
     * @param amount   The total amount to pay, in the given currency.
     * @param currency The currency in which the amount is provided.
     */
    public void splitPay(final double amount, final String currency) {
        double payAmount = Bank.getInstance().convertCurrency(amount, currency, this.currency);
        balance -= payAmount;
    }

    /**
     * Sets a minimum balance for the account.
     *
     * @param amount the minimum balance.
     */
    public void setMinBalance(final double amount) {
        minBalance = Optional.of(amount);
    }

    /**
     * Converts this account's data to a JSON object.
     *
     * @param objectMapper the Jackson {@link ObjectMapper} for JSON conversion.
     * @return the {@link ObjectNode} representing the account.
     */
    @Override
    public ObjectNode toObjectNode(final ObjectMapper objectMapper) {
        ObjectNode result = objectMapper.createObjectNode();

        result.put("IBAN", iban);
        result.put("balance", balance);
        result.put("currency", currency);

        ArrayNode cardArray = result.putArray("cards");
        for (final Card card : cardsByNumber.values()) {
            cardArray.add(card.toObjectNode(objectMapper));
        }

        return result;
    }


    /**
     * Abstract method to set the interest rate for this account.
     *
     * @param interestRate the interest rate to set.
     * @throws NotSavingsAccountException if this is not a savings account.
     */
    public abstract void setInterestRate(double interestRate)
            throws NotSavingsAccountException;

    /**
     * Abstract method to add interest to the account balance.
     *
     * @throws NotSavingsAccountException if this is not a savings account.
     */
    public abstract double addInterest() throws NotSavingsAccountException;

    /**
     * Retrieves the list of employees.
     *
     * @return A list of employee names, or {@code null} if no employees are available.
     */
    public List<String> getEmployees() {
        return null;
    }

    /**
     * Retrieves the list of managers.
     *
     * @return A list of manager names, or {@code null} if no managers are available.
     */
    public List<String> getManagers() {
        return null;
    }

    /**
     * Retrieves the spending limit for the account.
     *
     * @return The spending limit as a {@code Double}, or {@code null} if not set.
     */
    public Double getSpendingLimit() {
        return null;
    }

    /**
     * Retrieves the deposit limit for the account.
     *
     * @return The deposit limit as a {@code Double}, or {@code null} if not set.
     */
    public Double getDepostLimit() {
        return null;
    }

    /**
     * Retrieves the transaction information associated with the account.
     *
     * @return A list of {@code TransactionInfo} objects, or {@code null}
     * if no transactions are available.
     */
    public List<TransactionInfo> getTransasctionInfo() {
        return null;
    }

    /**
     * Adds a transaction record to the account's transaction history.
     *
     * @param amount   The amount of the transaction. Must be greater than 0.
     * @param username The username associated with the transaction. Must not be null or empty.
     * @param timestamp The timestamp of the transaction.
     * @param commerciante The name of the commerciante associated with the transaction, or null.
     */
    public void addTransactionInfo(final double amount, final String username,
                                   final int timestamp, final String commerciante) {

    }

    /**
     * Adds a manager to the account.
     *
     * @param email The email of the manager to add. Must not be null or empty.
     */
    public void addManager(final String email) {
    }

    /**
     * Adds an employee to the account.
     *
     * @param email The email of the employee to add. Must not be null or empty.
     */
    public void addEmployee(final String email) {

    }

    /**
     * Changes the spending limit for the account.
     *
     * @param email The email of the user requesting the change. Must not be null or empty.
     * @param limit The new spending limit. Must be greater than 0.
     * @throws NotAuthorizedException If the user is not authorized to change the spending limit.
     * @throws UnsupportedOperationException If the operation is not supported.
     */
    public void changeSpendingLimit(final String email, final double limit)
            throws NotAuthorizedException, UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Changes the deposit limit for the account.
     *
     * @param email The email of the user requesting the change. Must not be null or empty.
     * @param limit The new deposit limit. Must be greater than 0.
     * @throws NotAuthorizedException If the user is not authorized to change the deposit limit.
     * @throws UnsupportedOperationException If the operation is not supported.
     */
    public void changeDepositLimit(final String email, final double limit)
            throws NotAuthorizedException, UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Retrieves the type of the account.
     *
     * @return The type of the account as a {@code String}.
     */
    public abstract String getType();
}
