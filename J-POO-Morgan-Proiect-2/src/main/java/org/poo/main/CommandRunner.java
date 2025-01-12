package org.poo.main;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.poo.bank.Bank;
import org.poo.bank.transaction.Transaction;
import org.poo.bank.User;
import org.poo.fileio.CommandInput;

import java.util.List;


public final class CommandRunner {
    private final ObjectMapper objectMapper;

    public CommandRunner(final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ObjectNode executeCommand(final CommandInput commandInput) {
        return switch (commandInput.getCommand()) {
            case "printUsers" -> printUsers(commandInput);
            case "printTransactions" -> printTransactions(commandInput);
            case "addAccount" -> addAccount(commandInput);
            case "addFunds" -> addFunds(commandInput);
            case "createCard" -> createCard(commandInput);
            case "createOneTimeCard" -> createOneTimeCard(commandInput);
            case "deleteAccount" -> deleteAccount(commandInput);
            case "deleteCard" -> deleteCard(commandInput);
            case "setMinimumBalance" -> setMinimumBalance(commandInput);
            case "checkCardStatus" -> checkCardStatus(commandInput);
            case "payOnline" -> payOnline(commandInput);
            case "sendMoney" -> sendMoney(commandInput);
            case "setAlias" -> setAlias(commandInput);
            case "addInterest" -> addInterest(commandInput);
            case "changeInterestRate" -> changeInterestRate(commandInput);
            case "splitPayment" -> splitPayment(commandInput);
            case "report" -> report(commandInput);
            case "spendingsReport" -> spendingsReport(commandInput);
            case "withdrawSavings" -> withdrawSavings(commandInput);
            case "upgradePlan" -> upgradePlan(commandInput);
            case "cashWithdrawal" -> cashWithdrawal(commandInput);
            case "acceptSplitPayment" -> acceptSplitPayment(commandInput);
            default -> throw new IllegalStateException("Unexpected value: "
                    + commandInput.getCommand());
        };
    }

    private ObjectNode printUsers(final CommandInput commandInput) {
        ObjectNode resultNode = objectMapper.createObjectNode();

        resultNode.put("command", "printUsers");

        ArrayNode userArray = resultNode.putArray("output");
        final List<User> users = Bank.getInstance().getUsers();
        for (final User user : users) {
            userArray.add(user.toObjectNode(objectMapper));
        }

        resultNode.put("timestamp", commandInput.getTimestamp());

        return resultNode;
    }

    private ObjectNode printTransactions(final CommandInput commandInput) {
        final String email = commandInput.getEmail();

        ObjectNode resultNode = objectMapper.createObjectNode();

        resultNode.put("command", "printTransactions");

        ArrayNode output = objectMapper.createArrayNode();
        final List<Transaction> transactions = Bank.getInstance().getTransactions(email);
        for (final Transaction transaction : transactions) {
            output.add(transaction.toObjectNode(objectMapper));
        }

        resultNode.set("output", output);
        resultNode.put("timestamp", commandInput.getTimestamp());

        return resultNode;
    }

    private ObjectNode addAccount(final CommandInput commandInput) {
        final String email = commandInput.getEmail();
        final String currency = commandInput.getCurrency();
        final String accountType = commandInput.getAccountType();
        final double interestRate = commandInput.getInterestRate();
        final int timestamp = commandInput.getTimestamp();

        Bank.getInstance().addAccount(email, currency, accountType, interestRate, timestamp);

        return null;
    }

    private ObjectNode addFunds(final CommandInput commandInput) {
        final String iban = commandInput.getAccount();
        final double amount = commandInput.getAmount();

        Bank.getInstance().addFunds(iban, amount);

        return null;
    }

    private ObjectNode createCard(final CommandInput commandInput) {
        final String iban = commandInput.getAccount();
        final String email = commandInput.getEmail();
        final int timestamp = commandInput.getTimestamp();

        Bank.getInstance().createCard(iban, email, timestamp);

        return null;
    }

    private ObjectNode createOneTimeCard(final CommandInput commandInput) {
        final String iban = commandInput.getAccount();
        final String email = commandInput.getEmail();
        final int timestamp = commandInput.getTimestamp();

        Bank.getInstance().createOneTimeCard(iban, email, timestamp);

        return null;
    }


    private ObjectNode deleteAccount(final CommandInput commandInput) {
        final String iban = commandInput.getAccount();
        final String email = commandInput.getEmail();
        final int timestamp = commandInput.getTimestamp();

        final ObjectNode resultNode = objectMapper.createObjectNode();

        resultNode.put("command", "deleteAccount");
        final ObjectNode output = objectMapper.createObjectNode();

        final String result = Bank.getInstance().deleteAccount(iban, email, timestamp);
        if (result == null) {
            output.put("success", "Account deleted");
        } else {
            output.put("error", result);
        }

        output.put("timestamp", timestamp);
        resultNode.set("output", output);

        resultNode.put("timestamp", timestamp);

        return resultNode;
    }

    private ObjectNode deleteCard(final CommandInput commandInput) {
        final String cardNumber = commandInput.getCardNumber();
        final int timestamp = commandInput.getTimestamp();

        Bank.getInstance().deleteCard(cardNumber, timestamp);

        return null;
    }

    private ObjectNode setMinimumBalance(final CommandInput commandInput) {
        final double amount = commandInput.getAmount();
        final String iban = commandInput.getAccount();

        Bank.getInstance().setMinBalance(iban, amount);

        return null;
    }

    private ObjectNode checkCardStatus(final CommandInput commandInput) {
        final String cardNumber = commandInput.getCardNumber();
        final int timestamp = commandInput.getTimestamp();

        final String status = Bank.getInstance().checkCardStatus(cardNumber, timestamp);
        if (status != null) {
            final ObjectNode resultNode = objectMapper.createObjectNode();
            resultNode.put("command", "checkCardStatus");

            final ObjectNode outputNode = objectMapper.createObjectNode();
            outputNode.put("timestamp", timestamp);
            outputNode.put("description", status);

            resultNode.set("output", outputNode);
            resultNode.put("timestamp", timestamp);

            return resultNode;
        }

        return null;
    }

    private ObjectNode payOnline(final CommandInput commandInput) {
        final String cardNumber = commandInput.getCardNumber();
        final double amount = commandInput.getAmount();
        final String currency = commandInput.getCurrency();
        final String description = commandInput.getDescription();
        final String commerciant = commandInput.getCommerciant();
        final String email = commandInput.getEmail();
        final int timestamp = commandInput.getTimestamp();

        final String result = Bank.getInstance().payOnline(email, cardNumber, amount,
                currency, description, commerciant, timestamp);

        if (result == null) {
            return null;
        }

        final ObjectNode resultNode = objectMapper.createObjectNode();
        resultNode.put("command", "payOnline");

        final ObjectNode outputNode = objectMapper.createObjectNode();
        outputNode.put("timestamp", timestamp);
        outputNode.put("description", result);
        resultNode.set("output", outputNode);

        resultNode.put("timestamp", timestamp);

        return resultNode;
    }

    private ObjectNode sendMoney(final CommandInput commandInput) {
        final String iban = commandInput.getAccount();
        final double amount = commandInput.getAmount();
        final String receiver = commandInput.getReceiver();
        final String description = commandInput.getDescription();
        final int timestamp = commandInput.getTimestamp();

        final String result = Bank.getInstance().sendMoney(iban, receiver, amount, description, timestamp);
        if (result == null)
            return null;

        final ObjectNode resultNode = objectMapper.createObjectNode();
        resultNode.put("command", "sendMoney");

        final ObjectNode outputNode = objectMapper.createObjectNode();
        outputNode.put("timestamp", timestamp);
        outputNode.put("description", result);

        resultNode.set("output", outputNode);

        resultNode.put("timestamp", timestamp);

        return resultNode;
    }

    private ObjectNode setAlias(final CommandInput commandInput) {
        final String iban = commandInput.getAccount();
        final String email = commandInput.getEmail();
        final String alias = commandInput.getAlias();

        Bank.getInstance().setAlias(iban, email, alias);

        return null;
    }

    private ObjectNode addInterest(final CommandInput commandInput) {
        final String iban = commandInput.getAccount();
        final int timestamp = commandInput.getTimestamp();

        final String result = Bank.getInstance().addInterest(iban, timestamp);
        if (result == null) {
            return null;
        }

        final ObjectNode resultNode = objectMapper.createObjectNode();
        resultNode.put("command", "addInterest");
        final ObjectNode node = objectMapper.createObjectNode();

        node.put("description", result);
        node.put("timestamp", timestamp);
        resultNode.set("output", node);
        resultNode.put("timestamp", timestamp);

        return resultNode;
    }

    private ObjectNode changeInterestRate(final CommandInput commandInput) {
        final String iban = commandInput.getAccount();
        final double interestRate = commandInput.getInterestRate();
        final int timestamp = commandInput.getTimestamp();

        final String result = Bank.getInstance().changeInterestRate(iban, interestRate, timestamp);
        if (result != null) {
            final ObjectNode resultNode = objectMapper.createObjectNode();
            resultNode.put("command", "changeInterestRate");
            final ObjectNode node = objectMapper.createObjectNode();

            node.put("description", result);
            node.put("timestamp", timestamp);
            resultNode.set("output", node);
            resultNode.put("timestamp", timestamp);

            return resultNode;
        }

        return null;
    }

    private ObjectNode splitPayment(final CommandInput commandInput) {
        final List<String> accounts = commandInput.getAccounts();
        final int timestamp = commandInput.getTimestamp();
        final String currency = commandInput.getCurrency();
        final double amount = commandInput.getAmount();

        Bank.getInstance().splitPayment(accounts, amount, currency, timestamp);

        return null;
    }

    private ObjectNode report(final CommandInput commandInput) {
        final int startTimestamp = commandInput.getStartTimestamp();
        final int endTimestamp = commandInput.getEndTimestamp();
        final String iban = commandInput.getAccount();
        final int timestamp = commandInput.getTimestamp();

        final ObjectNode resultNode = objectMapper.createObjectNode();
        resultNode.put("command", "report");

        ObjectNode result = Bank.getInstance()
                .getReport(objectMapper, iban, startTimestamp, endTimestamp);
        if (result == null) {
            result = objectMapper.createObjectNode();
            result.put("description", "Account not found");
            result.put("timestamp", timestamp);
        }
        resultNode.set("output", result);

        resultNode.put("timestamp", timestamp);

        return resultNode;
    }

    private ObjectNode spendingsReport(final CommandInput commandInput) {
        final int startTimestamp = commandInput.getStartTimestamp();
        final int endTimestamp = commandInput.getEndTimestamp();
        final String iban = commandInput.getAccount();
        final int timestamp = commandInput.getTimestamp();

        final ObjectNode resultNode = objectMapper.createObjectNode();
        resultNode.put("command", "spendingsReport");

        ObjectNode result = Bank.getInstance().getSpendingsReport(objectMapper,
                iban, startTimestamp, endTimestamp);
        if (result == null) {
            result = objectMapper.createObjectNode();
            result.put("description", "Account not found");
            result.put("timestamp", timestamp);
        }
        resultNode.set("output", result);

        resultNode.put("timestamp", timestamp);

        return resultNode;
    }

    private ObjectNode withdrawSavings(final CommandInput commandInput) {
        final String account = commandInput.getAccount();
        final double amount = commandInput.getAmount();
        final String currency = commandInput.getCurrency();
        final int timestamp = commandInput.getTimestamp();

        Bank.getInstance().withdrawSavings(account, amount, currency, timestamp);

        return null;
    }

    private ObjectNode upgradePlan(final CommandInput commandInput) {
        final String iban = commandInput.getAccount();
        final String planType = commandInput.getNewPlanType();
        final int timestamp = commandInput.getTimestamp();

        final String result = Bank.getInstance().upgradePlan(iban, planType, timestamp);

        if (result != null) {
            final ObjectNode resultNode = objectMapper.createObjectNode();
            resultNode.put("command", "upgradePlan");

            final ObjectNode outputNode = objectMapper.createObjectNode();
            outputNode.put("timestamp", timestamp);
            outputNode.put("description", result);

            resultNode.set("output", outputNode);

            resultNode.put("timestamp", timestamp);

            return resultNode;
        }

        return null;
    }

    private ObjectNode cashWithdrawal(final CommandInput commandInput) {
        final String card = commandInput.getCardNumber();
        final double amount = commandInput.getAmount();
        final String email = commandInput.getEmail();
        final int timestamp = commandInput.getTimestamp();

        final String result = Bank.getInstance().cashWithdrawal(email, card, amount, timestamp);
        if (result == null) {
            return null;
        }

        final ObjectNode resultNode = objectMapper.createObjectNode();
        resultNode.put("command", "cashWithdrawal");

        final ObjectNode outputNode = objectMapper.createObjectNode();
        outputNode.put("timestamp", timestamp);
        outputNode.put("description", result);

        resultNode.set("output", outputNode);

        resultNode.put("timestamp", timestamp);

        return resultNode;
    }

    private ObjectNode acceptSplitPayment(final CommandInput commandInput) {
        return null;
    }
}
