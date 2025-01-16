package org.poo.bank;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import org.poo.bank.account.Account;
import org.poo.bank.account.AccountFactory;
import org.poo.bank.account.TransactionInfo;
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
import java.util.stream.Collectors;

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
    private final Map<String, Commerciante> commerciantesByIBAN;

    private Bank() {
        usersByEmail = new LinkedHashMap<>();

        exchangeRates = new HashMap<>();
        aliasesToIBAN = new HashMap<>();
        usersByIBAN = new HashMap<>();
        accountsByIBAN = new HashMap<>();
        commerciantesByName = new HashMap<>();
        commerciantesByIBAN = new HashMap<>();
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
    public void initializeBank(final UserInput[] users, final ExchangeInput[] rates,
                               final CommerciantInput[] commerciants) {
        this.usersByEmail.clear();
        this.exchangeRates.clear();
        this.usersByIBAN.clear();
        this.accountsByIBAN.clear();
        this.aliasesToIBAN.clear();
        this.commerciantesByName.clear();
        this.commerciantesByIBAN.clear();

        SpendingThresholdStrategy.getSpendingInfo().clear();
        NumberOfTransactionsStrategy.getNoTransactions().clear();

        for (final UserInput user : users) {
            this.usersByEmail.put(user.getEmail(), new User(user));
        }

        for (final CommerciantInput commerciant : commerciants) {
            final Commerciante c = new Commerciante(commerciant);
            this.commerciantesByName.put(commerciant.getCommerciant(), c);
            this.commerciantesByIBAN.put(commerciant.getAccount(), c);
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
        final Account account = AccountFactory.createAccount(currency, accountType, interestRate,
                email);
        final User user = usersByEmail.get(email);

        accountsByIBAN.put(account.getIban(), account);
        usersByIBAN.put(account.getIban(), user);
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

    public void addFunds(final String iban, final double amount, final String email, final int timestamp) {
        final User user = usersByEmail.get(email);
        Account account = accountsByIBAN.get(iban);
        final boolean can = account.addFunds(amount, user, timestamp);
        if (can) {
            account.increaseBalance(amount);
        }
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
        if (amount == 0.0) {
            return null;
        }

        final User user = usersByEmail.get(email);

        try {
            user.payOnline(cardNumber, amount, currency, timestamp,
                    commerciantesByName.get(commerciante));
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
        if (receiverAccount == null && commerciantesByIBAN.get(receiverIBAN) == null) {
            return "User not found";
        }

        final User user = usersByIBAN.get(senderIBAN);
        user.sendMoney(senderIBAN, usersByIBAN.get(receiverIBAN),
                receiverAccount, commerciantesByIBAN.get(receiverIBAN), receiverIBAN,
                amount, timestamp, description);
        return null;
    }


    public void splitPayment(final List<String> ibans, final String splitPaymentType,
                             final List<Double> amounts, final double amount,
                             final String currency, final int timestamp) {
        final SplitPayment.SplitPaymentBuilder splitBuilder
                = new SplitPayment.SplitPaymentBuilder();
        splitBuilder.setTotalAmount(amount);
        splitBuilder.setCurrency(currency);
        splitBuilder.setType(splitPaymentType);
        splitBuilder.setTimestamp(timestamp);

        double splitAmount = 0.0;
        if (splitPaymentType.equals("equal")) {
            splitAmount = amount / ibans.size();
            splitBuilder.addAmount(splitAmount);
        }

        for (int i = 0; i < ibans.size(); i++) {
            final String iban = ibans.get(i);
            final User user = usersByIBAN.get(ibans.get(i));
            if (splitAmount == 0.0) {
                splitBuilder.addAmount(amounts.get(i));
            }

            splitBuilder.addIBAN(iban);
            splitBuilder.addUser(user);
        }

        final SplitPayment splitPayment = splitBuilder.build();
        for (final String iban : ibans) {
            final User user = usersByIBAN.get(iban);
            user.addSplitPayment(splitPayment);
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
            user.addTransaction(new InterestRateTransaction(timestamp, iban,
                    amount, account.getCurrency()));
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
                final Transaction lastT = filteredTransactions.getLast();
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
            user.addTransaction(new Transaction(timestamp,
                    "You don't have the minimum age required.", iban));
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
            user.addTransaction(new Transaction(timestamp,
                    "You do not have a classic account.", iban));
            return;
        }

        final double withdrawAmount = Bank.getInstance().convertCurrency(amount,
                currency, account.getCurrency());

        if (account.getBalance() < withdrawAmount) {
            return;
        }

        destAccount.increaseBalance(amount);
        account.decreaseBalance(withdrawAmount);

        user.addTransaction(new SavingsWithdrawTransaction(timestamp,
                destAccount.getIban(), iban, amount));
        user.addTransaction(new SavingsWithdrawTransaction(timestamp,
                destAccount.getIban(), iban, amount));
    }

    public String upgradePlan(final String iban, final String planType, final int timestamp) {
        final User user = usersByIBAN.get(iban);

        if (user == null) {
            return "Account not found";
        }

        user.upgradePlan(iban, planType, timestamp);

        return null;
    }

    /**
     * Handles a cash withdrawal operation for a user.
     * It retrieves the user by email, performs the withdrawal operation on the user's account,
     * and handles any potential errors such as a card not being found or being frozen.
     *
     * @param email     The email of the user performing the withdrawal.
     * @param card      The card used for the withdrawal.
     * @param amount    The amount to withdraw.
     * @param timestamp The timestamp of the withdrawal operation.
     * @return A message indicating the result of the operation, or {@code null} if successful.
     * Possible messages include "User not found" or the exception message
     * in case of errors.
     */
    public String cashWithdrawal(final String email, final String card, final double amount,
                                 final int timestamp) {
        final User user = usersByEmail.get(email);

        if (user == null) {
            return "User not found";
        }

        try {
            user.cashWithdrawal(card, amount, timestamp);
        } catch (CardNotFoundException | CardFrozenException e) {
            return e.getMessage();
        }

        return null;
    }

    public String acceptSplitPayment(final String email) {
        final User user = usersByEmail.get(email);

        if (user == null) {
            return "User not found";
        }

        user.acceptSplitPayment();

        return null;
    }

    public String rejectSplitPayment(final String email) {
        final User user = usersByEmail.get(email);

        if (user == null) {
            return "User not found";
        }

        user.rejectSplitPayment();

        return null;
    }

    public void addNewBusinessAssociate(final String iban, final String role, final String email) {
        final Account account = accountsByIBAN.get(iban);
        final User user = usersByEmail.get(email);

        switch (role) {
            case "employee":
                account.addEmployee(email);
                break;
            case "manager":
                account.addManager(email);
                break;
            default:
                break;
        }

        user.addAccount(account, -1);
    }

    public String changeSpendingLimit(final String iban, final String email, final double limit) {
        final Account account = accountsByIBAN.get(iban);
        try {
            account.changeSpendingLimit(email, limit);
        } catch (final UnsupportedOperationException e) {
            return "This is not a business account";
        } catch (final NotAuthorizedException e) {
            return "You must be owner in order to change spending limit.";
        }

        return null;
    }

    public String changeDepositLimit(final String iban, final String email, final double limit) {
        final Account account = accountsByIBAN.get(iban);
        try {
            account.changeDepositLimit(email, limit);
        } catch (final UnsupportedOperationException e) {
            return "This is not a business account";
        } catch (final NotAuthorizedException e) {
            return "You must be owner in order to change spending limit.";
        }

        return null;
    }

    public ObjectNode businessReport(final ObjectMapper objectMapper, final String type,
                                     final int startTimestamp, final int endTimestamp,
                                     final String iban) {
        final Account account = accountsByIBAN.get(iban);

        final ObjectNode resultNode = objectMapper.createObjectNode();

        resultNode.put("IBAN", account.getIban());
        resultNode.put("balance", account.getBalance());
        resultNode.put("currency", account.getCurrency());
        resultNode.put("spending limit", account.getSpendingLimit());
        resultNode.put("deposit limit", account.getDepostLimit());

        double totalDeposited = 0.0;
        double totalSpent = 0.0;
        final Map<String, Double> deposited = new HashMap<>();
        final Map<String, Double> spent = new HashMap<>();
        for (final TransactionInfo transactionInfo : account.getTransasctionInfo()) {
            final int timestamp = transactionInfo.getTimestamp();
            if (timestamp < startTimestamp || timestamp > endTimestamp) {
                continue;
            }

            final String username = transactionInfo.getUsername();
            final double amount = transactionInfo.getAmount();

            if (amount < 0) {
                totalSpent -= amount;
                spent.put(username, spent.getOrDefault(username, 0.0) - amount);
            } else {
                totalDeposited += amount;
                deposited.put(username, deposited.getOrDefault(username, 0.0) + amount);
            }
        }

        final ArrayNode employeesArr = objectMapper.createArrayNode();
        for (final String employee : account.getEmployees().stream().sorted().toList()) {
            final User user = usersByEmail.get(employee);
            final String username = user.getLastName() + " " + user.getFirstName();

            final ObjectNode node = objectMapper.createObjectNode();
            node.put("deposited", deposited.getOrDefault(username, 0.0));
            node.put("spent", spent.getOrDefault(username, 0.0));
            node.put("username", username);

            employeesArr.add(node);
        }

        final ArrayNode managersArr = objectMapper.createArrayNode();
        for (final String manager : account.getManagers().stream().sorted().toList()) {
            final User user = usersByEmail.get(manager);
            final String username = user.getLastName() + " " + user.getFirstName();

            final ObjectNode node = objectMapper.createObjectNode();
            node.put("deposited", deposited.getOrDefault(username, 0.0));
            node.put("spent", spent.getOrDefault(username, 0.0));
            node.put("username", username);

            managersArr.add(node);
        }

        resultNode.set("employees", employeesArr);
        resultNode.set("managers", managersArr);
        resultNode.put("statistics type", type);
        resultNode.put("total deposited", totalDeposited);
        resultNode.put("total spent", totalSpent);

        return resultNode;
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

    public User getUserByEmail(final String email) {
        return usersByEmail.get(email);
    }

    /**
     * Retrieves the transaction history for a specific user.
     *
     * @param email the email of the user.
     * @return the list of transactions.
     */
    public List<Transaction> getTransactions(final String email) {
        final User user = usersByEmail.get(email);

        final List<Transaction> transactions = new ArrayList<>(user.getTransactions());
        transactions.sort(Comparator.comparingInt(Transaction::getTimestamp));

        return transactions;
    }
}
