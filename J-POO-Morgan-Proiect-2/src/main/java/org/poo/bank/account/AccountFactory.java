package org.poo.bank.account;

public final class AccountFactory {

    private AccountFactory() {

    }

    /**
     * Factory method to create an account based on the specified account type.
     *
     * @param currency     The currency of the account. Must not be null or empty.
     * @param accountType  The type of the account to create. Must be one of "classic",
     *                    "savings", or "business".
     * @param interestRate The interest rate applicable to the account. Only used for
     *                    "savings" accounts.
     *                     Ignored for other account types.
     * @param owner        The owner of the account. Must not be null or empty.
     * @return A newly created {@code Account} object of the specified type.
     * @throws IllegalStateException If the {@code accountType} is not recognized.
     */
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
