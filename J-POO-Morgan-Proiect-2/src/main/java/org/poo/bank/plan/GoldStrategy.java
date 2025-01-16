package org.poo.bank.plan;

public final class GoldStrategy implements PlanStrategy {
    @Override
    public String name() {
        return "gold";
    }

    @Override
    public double getComision(final double amount) {
        return 0;
    }
}
