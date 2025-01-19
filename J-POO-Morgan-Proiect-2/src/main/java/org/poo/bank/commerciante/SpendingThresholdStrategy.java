package org.poo.bank.commerciante;

import org.poo.bank.account.Account;

public final class SpendingThresholdStrategy implements CashbackStrategy {
    private static final double SPENDING_THRESHOLD_500 = 500.0;
    private static final double SPENDING_THRESHOLD_300 = 300.0;
    private static final double SPENDING_THRESHOLD_100 = 100.0;

    private static final double CASHBACK_STANDARD_STUDENT_500 = 0.0025;
    private static final double CASHBACK_SILVER_500 = 0.005;
    private static final double CASHBACK_GOLD_500 = 0.007;

    private static final double CASHBACK_STANDARD_STUDENT_300 = 0.002;
    private static final double CASHBACK_SILVER_300 = 0.004;
    private static final double CASHBACK_GOLD_300 = 0.0055;

    private static final double CASHBACK_STANDARD_STUDENT_100 = 0.001;
    private static final double CASHBACK_SILVER_100 = 0.003;
    private static final double CASHBACK_GOLD_100 = 0.005;

    @Override
    public double getCashback(final Account account, final String planType,
                              final Commerciante commerciante, final double amount) {
        account.setSpending(account.getSpending() + amount);
        final double totalSpent = account.getSpending();

        if (totalSpent >= SPENDING_THRESHOLD_500) {
            return switch (planType) {
                case "standard", "student" -> CASHBACK_STANDARD_STUDENT_500 * amount;
                case "silver" -> CASHBACK_SILVER_500 * amount;
                case "gold" -> CASHBACK_GOLD_500 * amount;
                default -> throw new IllegalStateException("Unexpected value: " + planType);
            };
        }

        if (totalSpent >= SPENDING_THRESHOLD_300) {
            return switch (planType) {
                case "standard", "student" -> CASHBACK_STANDARD_STUDENT_300 * amount;
                case "silver" -> CASHBACK_SILVER_300 * amount;
                case "gold" -> CASHBACK_GOLD_300 * amount;
                default -> throw new IllegalStateException("Unexpected value: " + planType);
            };
        }

        if (totalSpent >= SPENDING_THRESHOLD_100) {
            return switch (planType) {
                case "standard", "student" -> CASHBACK_STANDARD_STUDENT_100 * amount;
                case "silver" -> CASHBACK_SILVER_100 * amount;
                case "gold" -> CASHBACK_GOLD_100 * amount;
                default -> throw new IllegalStateException("Unexpected value: " + planType);
            };
        }

        return 0.0;
    }
}
