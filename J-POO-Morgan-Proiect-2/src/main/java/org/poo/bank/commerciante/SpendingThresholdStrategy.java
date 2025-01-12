package org.poo.bank.commerciante;

import lombok.Getter;
import org.poo.bank.account.Account;

import java.util.HashMap;
import java.util.Map;

public class SpendingThresholdStrategy implements CashbackStrategy {

    @Getter
    static private final Map<String, Map<String, Double>> spendingInfo = new HashMap<>();

    @Override
    public double getCashback(final Account account, final String planType,
                              final Commerciante commerciante, final double amount) {
        spendingInfo.putIfAbsent(account.getIban(), new HashMap<>());
        final Map<String,Double> spentPerType = spendingInfo.get(account.getIban());
        spentPerType.put(commerciante.getType(), spentPerType.getOrDefault(commerciante.getType(), 0.0) + amount);
        final double totalSpent = spentPerType.get(commerciante.getType());

        if (totalSpent >= 500) {
            return switch(planType) {
                case "standard", "student" -> 0.0025 * totalSpent;
                case "silver" -> 0.005 * totalSpent;
                case "gold" -> 0.007 * totalSpent;
                default -> throw new IllegalStateException("Unexpected value: " + planType);
            };
        }

        if (totalSpent >= 300) {
            return switch(planType) {
                case "standard", "student" -> 0.002 * totalSpent;
                case "silver" -> 0.004 * totalSpent;
                case "gold" -> 0.0055 * totalSpent;
                default -> throw new IllegalStateException("Unexpected value: " + planType);
            };
        }

        if (totalSpent >= 100) {
            return switch(planType) {
                case "standard", "student" -> 0.001 * totalSpent;
                case "silver" -> 0.003 * totalSpent;
                case "gold" -> 0.005 * totalSpent;
                default -> throw new IllegalStateException("Unexpected value: " + planType);
            };
        }

        return 0.0;
    }
}
