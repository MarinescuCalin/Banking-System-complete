package org.poo.bank.plan;

public class StandardStrategy implements PlanStrategy {
    @Override
    public String name() {
        return "standard";
    }

    @Override
    public double getComision(double amount) {
        return amount * 0.002;
    }
}
