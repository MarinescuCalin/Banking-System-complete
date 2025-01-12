package org.poo.bank.plan;

public class GoldStrategy implements PlanStrategy {
    @Override
    public String name() {
        return "gold";
    }

    @Override
    public double getComision(double amount) {
        return 0;
    }
}
