package org.poo.bank.plan;

public final class StudentStrategy implements PlanStrategy {
    @Override
    public String name() {
        return "student";
    }

    @Override
    public double getComision(final double amount) {
        return 0;
    }
}
