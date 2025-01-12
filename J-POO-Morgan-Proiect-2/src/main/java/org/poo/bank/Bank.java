package org.poo.bank;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import org.poo.bank.account.Account;
import org.poo.bank.account.AccountFactory;
import org.poo.bank.card.Card;
import org.poo.bank.commerciante.Commerciante;
import org.poo.bank.commerciante.NumberOfTransactionsStrategy;
import org.poo.bank.commerciante.SpendingThresholdStrategy;
import org.poo.bank.exception.*;
import org.poo.bank.transaction.*;
import org.poo.fileio.CommerciantInput;
import org.poo.fileio.ExchangeInput;
import org.poo.fileio.UserInput;

import java.time.LocalDate;
import java.time.Period;
import java.util.*;

/**
 * Represents a singleton class that models a bank, managing users, accounts,
 * exchange rates, transactions, and various banking operations.
 */
public final class Bank {
    private static Bank instance = null;

    private final Map<String, User> usersByEmail;
    private final Map<String, Account> accountsByIBAN;

    @Getter
    private final Map<String, Map<String, Double>> exchangeRates;

    private final Map<String, String> aliasesToIBAN;
    private final Map<String, User> usersByIBAN;
    private final Map<String, Commerciante> commerciantesByName;

    private Bank() {
        usersByEmail = new LinkedHashMap<>();

        exchangeRates = new HashMap<>();
        aliasesToIBAN = new HashMap<>();
        usersByIBAN = new HashMap<>();
        accountsByIBAN = new HashMap<>();
        commerciantesByName = new HashMap<>();
    }


    /**
     * Retrieves the single instance of the bank.
     *
     * @return the singleton instance of the bank.
     */
    public static Bank getInstance() {
        if (instance == null) {
            instance = new Bank();
        }

        return instance;
    }

    /**
     * Initializes the bank's data, including users and exchange rates.
     *
     * @param users        the array of user inputs to initialize.
     * @param rates        the array of exchange rates to configure.
     * @param commerciants the array of commerciant inputs to initialize.
     */
    public void initializeBank(final UserInput[] users, final ExchangeInput[] rates, final CommerciantInput[] commerciants) {
        this.usersByEmail.clear();
        this.exchangeRates.clear();
        this.usersByIBAN.clear();
        this.accountsByIBAN.clear();
        this.aliasesToIBAN.clear();
        this.commerciantesByName.clear();
        SpendingThresholdStrategy.getSpendingInfo().clear();
        NumberOfTransactionsStrategy.getCashbackReceived().clear();
        NumberOfTransactionsStrategy.getNoTransactions().clear();

        for (final UserInput user : users) {
            this.usersByEmail.put(user.getEmail(), new User(user));
        }

        for (final CommerciantInput commerciant : commerciants) {
            this.commerciantesByName.put(commerciant.getCommerciant(), new Commerciante(commerciant));
        }

        // add direct rates
        for (final ExchangeInput rate : rates) {
            exchangeRates.putIfAbsent(rate.getFrom(), new HashMap<>());

            Map<String, Double> to = exchangeRates.get(rate.getFrom());
            to.putIfAbsent(rate.getTo(), rate.getRate());

            exchangeRates.putIfAbsent(rate.getTo(), new HashMap<>());
            Map<String, Double> from = exchangeRates.get(rate.getTo());
            if (!from.containsKey(rate.getFrom())) {
                double inverseRate = 1.0 / rate.getRate();
                from.put(rate.getFrom(), inverseRate);
            }
        }

        // add indirect rates
        for (final String from : exchangeRates.keySet()) {
            for (final String to : exchangeRates.keySet()) {
                if (!from.equals(to) && !exchangeRates.get(from).containsKey(to)) {
                    for (String intermediate : exchangeRates.get(from).keySet()) {
                        if (exchangeRates.get(intermediate).containsKey(to)) {
                            double rateFromTo = exchangeRates.get(from).get(intermediate);
                            double rateIntermediateTo = exchangeRates.get(intermediate).get(to);
                            double indirectRate = rateFromTo * rateIntermediateTo;

                            exchangeRates.get(from).put(to, indirectRate);
                            break;
                        }
                    }
                }
            }
        }

        // add self rates
        for (final String from : exchangeRates.keySet()) {
            Map<String, Double> map = exchangeRates.get(from);
            map.put(from, 1.0);
        }
    }


    /**
     * Adds a new account for the specified user.
     *
     * @param email        the email of the user.
     * @param currency     the currency of the account.
     * @param accountType  the type of the account (e.g., savings, current).
     * @param interestRate the initial interest rate for the account.
     * @param timestamp    the timestamp of the account creation.
     */
    public void addAccount(final String email, final String currency,
                           final String accountType, final double interestRate,
                           final int timestamp) {
        final Account account = AccountFactory.createAccount(currency, accountType, interestRate);

        final User user = usersByEmail.get(email);
        usersByIBAN.put(account.getIban(), user);
        accountsByIBAN.put(account.getIban(), account);
        user.addAccount(account, timestamp);
    }


    /**
     * Deletes an account with the specified IBAN.
     *
     * @param iban      the IBAN of the account to delete.
     * @param email     the email of the account owner.
     * @param timestamp the timestamp of the operation.
     * @return a message if the account could not be deleted, otherwise null.
     */
    public String deleteAccount(final String iban, final String email, final int timestamp) {
        final User user = usersByEmail.get(email);

        try {
            user.removeAccount(iban);
        } catch (BalanceNotEmptyException e) {
            user.addTransaction(new Transaction(timestamp,
                    "Account couldn't be deleted - there are funds remaining", iban));
            return e.getMessage();

        }

        accountsByIBAN.remove(iban);
        usersByIBAN.remove(iban);

        return null;
    }


    /**
     * Creates a new card for the specified account.
     *
     * @param iban      the IBAN of the account.
     * @param email     the email of the account owner.
     * @param timestamp the timestamp of the operation.
     */
    public void createCard(final String iban, final String email, final int timestamp) {
        final User user = usersByEmail.get(email);

        if (user == null) {
            return;
        }

        user.createCard(iban, timestamp);
    }


    /**
     * Deletes a card identified by its card number.
     *
     * @param cardNumber the number of the card to delete.
     * @param timestamp  the timestamp of the operation.
     */
    public void deleteCard(final String cardNumber, final int timestamp) {
        for (final User user : usersByEmail.values()) {
            user.removeCard(cardNumber, timestamp);
        }
    }


    /**
     * Creates a one-time-use card for the specified account.
     *
     * @param iban      the IBAN of the account.
     * @param email     the email of the account owner.
     * @param timestamp the timestamp of the operation.
     */
    public void createOneTimeCard(final String iban, final String email, final int timestamp) {
        final User user = usersByEmail.get(email);

        user.createOneTimeCard(iban, timestamp);
    }


    /**
     * Adds funds to the specified account.
     *
     * @param iban   the IBAN of the account.
     * @param amount the amount to add.
     */
    public void addFunds(final String iban, final double amount) {
        Account account = accountsByIBAN.get(iban);
        account.addFunds(amount);
    }


    /**
     * Sets the minimum balance for an account.
     *
     * @param iban   the IBAN of the account.
     * @param amount the minimum balance to set.
     */
    public void setMinBalance(final String iban, final double amount) {
        Account account = accountsByIBAN.get(iban);
        account.setMinBalance(amount);
    }


    /**
     * Checks the status of a card and freezes it if necessary.
     *
     * @param cardNumber the card number to check.
     * @param timestamp  the timestamp of the operation.
     * @return an error message if the card is not found, otherwise null.
     */
    public String checkCardStatus(final String cardNumber, final int timestamp) {
        for (final User user : usersByEmail.values()) {
            for (final Account account : user.getAccounts()) {
                final Card card = account.getCard(cardNumber);
                if (card != null) {
                    user.checkFreezeCard(account, card, timestamp);
                    return null;
                }

            }
        }

        return "Card not found";
    }


    /**
     * Processes an online payment using a specific card.
     *
     * @param email        the email of the card owner.
     * @param cardNumber   the card number.
     * @param amount       the payment amount.
     * @param currency     the payment currency.
     * @param description  the payment description.
     * @param commerciante the commerciant name.
     * @param timestamp    the timestamp of the transaction.
     * @return an error message if the payment fails, otherwise null.
     */
    public String payOnline(final String email, final String cardNumber,
                            final double amount, final String currency,
                            final String description, final String commerciante,
                            final int timestamp) {
        final User user = usersByEmail.get(email);

        try {
            user.payOnline(cardNumber, amount, currency, timestamp, commerciantesByName.get(commerciante));
        } catch (CardNotFoundException e) {
            return e.getMessage();
        }

        return null;
    }


    /**
     * Transfers money between two accounts.
     *
     * @param senderIBAN   the IBAN of the sender account.
     * @param receiverIBAN the IBAN of the receiver account.
     * @param amount       the amount to transfer.
     * @param description  the transfer description.
     * @param timestamp    the timestamp of the transaction.
     * @return an error message if the payment fails, otherwise null.
     */
    public String sendMoney(final String senderIBAN, final String receiverIBAN,
                            final double amount, final String description, final int timestamp) {
        final Account senderAccount = accountsByIBAN.get(senderIBAN);

        if (senderAccount == null) {
            return "User not found";
        }

        final Account receiverAccount = accountsByIBAN.get(aliasesToIBAN
                .getOrDefault(receiverIBAN, receiverIBAN));
        if (receiverAccount == null) {
            return "User not found";
        }

        final User user = usersByIBAN.get(senderIBAN);
        user.sendMoney(senderIBAN, usersByIBAN.get(receiverIBAN),
                receiverAccount, receiverIBAN, amount, timestamp, description);
        return null;
    }


    /**
     * Processes a split payment across multiple accounts.
     *
     * @param ibans     the list of IBANs to split the payment across.
     * @param amount    the total amount to split.
     * @param currency  the currency of the payment.
     * @param timestamp the timestamp of the transaction.
     */
    public void splitPayment(final List<String> ibans, final double amount,
                             final String currency, final int timestamp) {
        final List<Double> sumsToPay = new ArrayList<>();
        double splitAmount = amount / ibans.size();

        boolean canPay = true;
        String cantPayAccount = null;
        for (final String iban : ibans) {
            final Account account = accountsByIBAN.get(iban);
            final String accountCurrency = account.getCurrency();
            double individualSplitAmount = splitAmount;
            if (!accountCurrency.equals(currency)) {
                individualSplitAmount = convertCurrency(individualSplitAmount,
                        currency, accountCurrency);
            }

            if (individualSplitAmount > account.getBalance()) {
                canPay = false;
                cantPayAccount = iban;
            }
            sumsToPay.add(individualSplitAmount);
        }

        final String description = "Split payment of " + String.format("%.2f", amount)
                + " " + currency;
        if (canPay) {
            for (int i = 0; i < sumsToPay.size(); i++) {
                final User user = usersByIBAN.get(ibans.get(i));

                double individualSplitAmount = sumsToPay.get(i);
                user.splitPay(ibans.get(i), individualSplitAmount,
                        splitAmount, currency, ibans, description, timestamp);
            }
        } else {
            final String error = "Account " + cantPayAccount
                    + " has insufficient funds for a split payment.";
            for (int i = 0; i < sumsToPay.size(); i++) {
                final User user = usersByIBAN.get(ibans.get(i));

                user.addTransaction(new SplitPaymentTransaction(timestamp, description, splitAmount,
                        currency, ibans, error, ibans.get(i)));
            }
        }
    }


    /**
     * Converts an amount between two currencies.
     *
     * @param amount       the amount to convert.
     * @param fromCurrency the source currency.
     * @param toCurrency   the target currency.
     * @return the converted amount.
     */
    public double convertCurrency(final double amount,
                                  final String fromCurrency,
                                  final String toCurrency) {
        final Map<String, Map<String, Double>> rates = Bank.getInstance().getExchangeRates();
        return amount * rates.get(fromCurrency).get(toCurrency);
    }


    /**
     * Changes the interest rate of an account.
     *
     * @param iban         the IBAN of the account.
     * @param interestRate the new interest rate.
     * @param timestamp    the timestamp of the operation.
     * @return an error message if the operation fails, otherwise null.
     */
    public String changeInterestRate(final String iban,
                                     final double interestRate,
                                     final int timestamp) {
        final User user = usersByIBAN.get(iban);
        try {
            user.changeInterestRate(iban, interestRate, timestamp);
            return null;
        } catch (NotSavingsAccountException e) {
            return e.getMessage();
        }
    }


    /**
     * Adds interest to an account.
     *
     * @param iban      the IBAN of the account.
     * @param timestamp the timestamp of the operation.
     * @return an error message if the operation fails, otherwise null.
     */
    public String addInterest(final String iban, final int timestamp) {
        final Account account = accountsByIBAN.get(iban);
        final User user = usersByIBAN.get(iban);

        try {
            final double amount = account.addInterest();
            user.addTransaction(new InterestRateTransaction(timestamp, iban, amount, account.getCurrency()));
            return null;
        } catch (NotSavingsAccountException e) {
            return e.getMessage();
        }
    }


    /**
     * Generates a report for an account's transactions.
     *
     * @param objectMapper   the JSON object mapper.
     * @param iban           the IBAN of the account.
     * @param startTimestamp the start timestamp for filtering transactions.
     * @param endTimestamp   the end timestamp for filtering transactions.
     * @return the report as a JSON object node.
     */
    public ObjectNode getReport(final ObjectMapper objectMapper,
                                final String iban, final int startTimestamp,
                                final int endTimestamp) {
        final User user = usersByIBAN.get(iban);
        if (user == null) {
            return null;
        }

        final List<Transaction> transactions = user.getTransactions();
        final Account account = accountsByIBAN.get(iban);

        final List<Transaction> filteredTransactions = new ArrayList<>();
        for (final Transaction t : transactions) {
            if (!filteredTransactions.isEmpty()) {
                final Transaction lastT = filteredTransactions.get(filteredTransactions.size() - 1);
                if (t == lastT) {
                    continue;
                }
            }
            if (t.getIBAN() == null) {
                continue;
            }
            if (t.getTimestamp() >= startTimestamp
                    && t.getTimestamp() <= endTimestamp && t.getIBAN().contains(iban)) {
                filteredTransactions.add(t);
            }
        }

        final ObjectNode result = objectMapper.createObjectNode();
        result.put("balance", account.getBalance());
        result.put("currency", account.getCurrency());
        result.put("IBAN", iban);
        ArrayNode array = objectMapper.createArrayNode();
        for (final Transaction t : filteredTransactions) {
            array.add(t.toObjectNode(objectMapper));
        }

        result.set("transactions", array);

        return result;
    }


    /**
     * Generates a spending report for an account.
     *
     * @param objectMapper   the JSON object mapper.
     * @param iban           the IBAN of the account.
     * @param startTimestamp the start timestamp for filtering transactions.
     * @param endTimestamp   the end timestamp for filtering transactions.
     * @return the spending report as a JSON object node.
     */
    public ObjectNode getSpendingsReport(final ObjectMapper objectMapper, final String iban,
                                         final int startTimestamp, final int endTimestamp) {
        final User user = usersByIBAN.get(iban);
        if (user == null) {
            return null;
        }

        final List<Transaction> transactions = user.getTransactions();
        final Account account = accountsByIBAN.get(iban);
        if (account.getInterestRate() != null) {
            final ObjectNode c = objectMapper.createObjectNode();
            c.put("error", "This kind of report is not supported for a saving account");
            return c;
        }

        final List<Transaction> filteredTransactions = new ArrayList<>();
        for (final Transaction t : transactions) {
            if (t.getIBAN() == null) {
                continue;
            }
            if (t.getTimestamp() >= startTimestamp
                    && t.getTimestamp() <= endTimestamp && t.getCommerciant() != null
                    && t.getIBAN().contains(iban)) {
                filteredTransactions.add(t);
            }
        }

        final ObjectNode result = objectMapper.createObjectNode();
        result.put("balance", account.getBalance());
        result.put("currency", account.getCurrency());
        result.put("IBAN", iban);

        final Map<String, Double> spendings = new TreeMap<>();
        final ArrayNode array = objectMapper.createArrayNode();
        for (final Transaction t : filteredTransactions) {
            array.add(t.toObjectNode(objectMapper));
            final String commerciant = t.getCommerciant();
            if (commerciant != null) {
                spendings.put(commerciant, spendings.getOrDefault(commerciant, 0.0)
                        + t.getAmount());
            }
        }

        result.set("transactions", array);

        final ArrayNode commerciants = objectMapper.createArrayNode();
        for (final Map.Entry<String, Double> entry : spendings.entrySet()) {
            final ObjectNode c = objectMapper.createObjectNode();

            c.put("commerciant", entry.getKey());
            c.put("total", entry.getValue());

            commerciants.add(c);
        }

        result.set("commerciants", commerciants);

        return result;
    }

    public void withdrawSavings(final String iban, final double amount, final String currency,
                                final int timestamp) {
        final Account account = accountsByIBAN.get(iban);
        final User user = usersByIBAN.get(iban);

        final LocalDate birthDate = LocalDate.parse(user.getBirthDate());
        final int age = Period.between(birthDate, LocalDate.now()).getYears();
        if (age < 21) {
            user.addTransaction(new Transaction(timestamp, "You don't have the minimum age required.", iban));
            return;
        }

        Account destAccount = null;
        for (final Account acc : user.getAccounts()) {
            if (acc.getInterestRate() == null && acc.getCurrency().equals(currency)) {
                destAccount = acc;
                break;
            }
        }

        if (destAccount == null) {
            user.addTransaction(new Transaction(timestamp, "You do not have a classic account.", iban));
            return;
        }

        destAccount.addFunds(amount);

        final double withdrawAmount = Bank.getInstance().convertCurrency(amount, currency, account.getCurrency());
        account.removeFunds(withdrawAmount);
    }

    public String upgradePlan(final String iban, final String planType, final int timestamp) {
        final User user = usersByIBAN.get(iban);

        if (user == null) {
            return "Account not found";
        }

        final String currentPlan = user.getPlanName();

        if (planType.equals(currentPlan)) {
            return "The user already has the " + planType + " plan.";
        }

        switch (currentPlan) {
            case "silver":
                if (planType.equals("standard") || planType.equals("student"))
                    return "You cannot downgrade your plan.";
            case "gold":
                if (planType.equals("standard") || planType.equals("student") || planType.equals("silver"))
                    return "You cannot downgrade your plan.";
        }

        try {
            user.upgradePlan(iban, planType, timestamp);
        } catch (InsufficientFundsException e) {
            return e.getMessage();
        }

        return null;
    }

    public String cashWithdrawal(final String email, final String card, final double amount, final int timestamp) {
        final User user = usersByEmail.get(email);

        if (user == null)
            return "User not found";

        try {
            user.cashWithdrawal(card, amount, timestamp);
        } catch (CardNotFoundException | CardFrozenException e) {
            return e.getMessage();
        } catch (InsufficientFundsException e) {
            return null;
        }

        return null;
    }

    /**
     * Sets an alias for a specified IBAN.
     *
     * @param iban  the IBAN to alias.
     * @param email the email of the user creating the alias.
     * @param alias the alias name.
     */
    public void setAlias(final String iban, final String email, final String alias) {
        aliasesToIBAN.putIfAbsent(alias, iban);
    }


    /**
     * Retrieves the list of all users.
     *
     * @return the list of users.
     */
    public List<User> getUsers() {
        return new ArrayList<>(usersByEmail.values());
    }


    /**
     * Retrieves the transaction history for a specific user.
     *
     * @param email the email of the user.
     * @return the list of transactions.
     */
    public List<Transaction> getTransactions(final String email) {
        final User user = usersByEmail.get(email);

        return user.getTransactions();
    }
}
