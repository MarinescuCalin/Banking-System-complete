package org.poo.bank.plan;

public final class StandardStrategy implements PlanStrategy {
    @Override
    public String name() {
        return "standard";
    }

    @Override
    public double getComision(final double amount) {
        return amount * 0.002;
    }
}
