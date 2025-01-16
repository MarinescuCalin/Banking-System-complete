package org.poo.bank.account;

public final class AccountFactory {

    private AccountFactory() {

    }

    public static Account createAccount(final String currency,
                                        final String accountType,
                                        final double interestRate,
                                        final String owner) {
        return switch (accountType) {
            case "classic" -> new ClassicAccount(currency, owner);
            case "savings" -> new SavingsAccount(currency, owner, interestRate);
            case "business" -> new BusinessAccount(currency, owner);
            default -> throw new IllegalStateException("Unexpected value: " + accountType);
        };
    }
}
