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

    @Getter
    @Setter
    protected boolean cashbackReceived;

    public Account(final String currency, final String owner) {
        this.iban = Utils.generateIBAN();
        this.currency = currency;
        this.owner = owner;

        this.balance = 0.0;
        this.cashbacks = new HashMap<>();

        this.cardsByNumber = new LinkedHashMap<>();

        this.minBalance = Optional.empty();

        this.cashbackReceived = false;
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


    public boolean addFunds(final double amount, final User user, final int timestamp) {
        return true;
    }

    public boolean removeFunds(final double amount, final User user, final int timestamp) {
        return true;
    }

    public void increaseBalance(final double amount) {
        balance += amount;
    }

    public void decreaseBalance(final double amount) {
        balance -= amount;
    }

    public void sendFunds(final User user,
                          final Account receiver,
                          final Commerciante commerciante,
                          final double amount)
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

        double amountToSubstract = amount + Bank.getInstance()
                .convertCurrency(user.getCommission(amountInRON), "RON", currency);
        if (balance - amountToSubstract < 0.0) {
            throw new InsufficientFundsException();
        }

        if (commerciante != null) {
            final Cashback cashback = cashbacks.get(commerciante.getType());
            double amountToCashback = 0.0;
            if (cashback != null) {
                amountToCashback = cashback.getPercentage() * 0.01 * amountInRON;
                cashbacks.remove(commerciante.getType());
            }

            amountToCashback += commerciante.getCashback(this, user.getPlanName(), amountInRON);
            final double amountToAdd = Bank.getInstance().convertCurrency(amountToCashback,
                    "RON", currency);
            balance += amountToAdd;
        }

        balance -= amountToSubstract;
        if (receiver != null) {
            receiver.balance += amountToGet;
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

        final boolean can = removeFunds(amountToPay, user, timestamp);
        if (!can) {
            return;
        }

        balance -= amountToSubstract;

        final Cashback cashback = cashbacks.get(commerciante.getType());
        double amountToCashback = 0.0;
        if (cashback != null) {
            amountToCashback = cashback.getPercentage() * 0.01 * amountInRON;
            cashbacks.remove(commerciante.getType());
        }

        amountToCashback += commerciante.getCashback(this, user.getPlanName(), amountInRON);
        final double amountToAdd = Bank.getInstance().convertCurrency(amountToCashback,
                "RON", currency);
        balance += amountToAdd;

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
        double withdrawAmount = Bank.getInstance().convertCurrency(amount, "RON", currency);
        withdrawAmount += Bank.getInstance().convertCurrency(user.getCommission(amount),
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
     * Abstract method to retrieve the interest rate for this account.
     *
     * @return the interest rate.
     */
    public abstract Double getInterestRate();

    public abstract List<String> getEmployees();

    public abstract List<String> getManagers();

    public abstract Double getSpendingLimit();

    public abstract Double getDepostLimit();

    public abstract List<TransactionInfo> getTransasctionInfo();

    public abstract void addTransactionInfo(double amount, String username, int timestamp);

    public abstract void addManager(String email);

    public abstract void addEmployee(String email);

    public abstract void changeSpendingLimit(String email, double limit) throws NotAuthorizedException;

    public abstract void changeDepositLimit(String email, double limit) throws NotAuthorizedException;
}
