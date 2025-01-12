package org.poo.bank.account;

public final class AccountFactory {

    private AccountFactory() {

    }

    public static Account createAccount(final String currency,
                                        final String accountType, final double interestRate) {
        return switch (accountType) {
            case "classic" -> new ClassicAccount(currency);
            case "savings" -> new SavingsAccount(currency, interestRate);
            case "business" -> null;
            default -> throw new IllegalStateException("Unexpected value: " + accountType);
        };
    }
}
