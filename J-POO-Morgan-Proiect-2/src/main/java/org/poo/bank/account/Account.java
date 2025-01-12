package org.poo.bank.account;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import org.poo.bank.Bank;
import org.poo.bank.User;
import org.poo.bank.card.Card;
import org.poo.bank.card.ClassicCard;
import org.poo.bank.card.OneTimePayCard;
import org.poo.bank.commerciante.Cashback;
import org.poo.bank.commerciante.Commerciante;
import org.poo.bank.exception.CardFrozenException;
import org.poo.bank.exception.InsufficientFundsException;
import org.poo.bank.exception.NotSavingsAccountException;
import org.poo.bank.transaction.CardOperationTransaction;
import org.poo.bank.transaction.CashWithdrawTransaction;
import org.poo.bank.transaction.OnlinePaymentTransaction;
import org.poo.fileio.JSONWritable;
import org.poo.utils.Utils;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;


/**
 * Abstract class representing a bank account.
 * Manages funds, cards, and operations such as payments, transfers, and balance adjustments.
 */
@Getter
public abstract class Account implements JSONWritable {
    protected final String iban;
    protected final String currency;

    protected Double balance;
    protected Optional<Double> minBalance;

    protected Map<String, Card> cardsByNumber;

    private final Map<String, Cashback> cashbacks;

    public Account(final String currency) {
        this.iban = Utils.generateIBAN();
        this.currency = currency;

        this.balance = 0.0;
        this.cashbacks = new HashMap<>();

        this.cardsByNumber = new LinkedHashMap<>();

        this.minBalance = Optional.empty();
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
     * Adds funds to the account.
     *
     * @param amount the amount to add.
     */
    public void addFunds(final double amount) {
        balance += amount;
        balance = Math.round(balance * 100.0) / 100.0;
    }

    public void removeFunds(final double amount) {
        balance -= amount;
        balance = Math.round(balance * 100.0) / 100.0;
    }

    /**
     * Sends funds to another account.
     * Converts currency if needed and ensures sufficient funds are available.
     *
     * @param user     the user sending money.
     * @param receiver the receiving {@link Account}.
     * @param amount   the amount to send.
     * @throws InsufficientFundsException if the account has insufficient funds.
     */
    public void sendFunds(final User user,
                          final Account receiver,
                          final double amount)
            throws InsufficientFundsException {
        double amountToGet = amount;
        if (!currency.equals(receiver.getCurrency())) {
            amountToGet = Bank.getInstance()
                    .convertCurrency(amount,
                            this.currency,
                            receiver.getCurrency());
        }

        final double amountInRON = Bank.getInstance().convertCurrency(amount, currency, "RON");

        double amountToSubstract = amount + Bank.getInstance().convertCurrency(user.getCommission(amountInRON), "RON", currency);
        if (balance - amountToSubstract < 0.0) {
            throw new InsufficientFundsException();
        }

        balance -= amountToSubstract;
        balance = Math.round(balance * 100.0) / 100.0;

        receiver.addFunds(amountToGet);
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

        final double amountToPay = Bank.getInstance().convertCurrency(amount, otherCurrency, currency);
        final double amountInRON = Bank.getInstance().convertCurrency(amountToPay, currency, "RON");

        double amountToSubstract = amountToPay + Bank.getInstance().convertCurrency(user.getCommission(amountInRON), "RON", currency);

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

        balance -= amountToSubstract;

        final Cashback cashback = cashbacks.get(commerciante.getType());
        double amountToCashback = 0.0;
        if (cashback != null) {
            amountToCashback = cashback.getPercentage() * 0.01 * amountInRON;
        }

        amountToCashback += commerciante.getCashback(this, user.getPlanName(), amountInRON);
        balance += amountToCashback;
        balance = Math.round(balance * 100.0) / 100.0;

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

    public void cashWithdrawal(final User user, final Card card, final double amount,
                               final int timestamp) throws InsufficientFundsException, CardFrozenException {
        double withdrawAmount = Bank.getInstance().convertCurrency(amount, "RON", currency);
        withdrawAmount += Bank.getInstance().convertCurrency(user.getCommission(amount), "RON", currency);

        if (balance < withdrawAmount)
            throw new InsufficientFundsException();

        if (card.getStatus().equals("frozen")) {
            throw new CardFrozenException("The card is frozen");
        }

        balance -= withdrawAmount;
        balance = Math.round(balance * 100.0) / 100.0;
        user.addTransaction(new CashWithdrawTransaction(timestamp, iban, amount));
    }

    public void addCashback(final String commerciantType, final Cashback cashback) {
        cashbacks.put(commerciantType, cashback);
    }

    /**
     * Deducts the specified amount from the account balance.
     *
     * @param amount the amount to deduct.
     */
    public void splitPay(final double amount) {
        balance -= amount;
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
        result.put("balance", Math.round(balance * 100.0) / 100.0);
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
}
