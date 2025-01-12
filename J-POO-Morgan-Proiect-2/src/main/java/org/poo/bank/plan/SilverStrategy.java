package org.poo.bank.plan;

public class SilverStrategy implements PlanStrategy {
    @Override
    public String name() {
        return "silver";
    }

    @Override
    public double getComision(double amount) {
        if (amount < 500)
            return 0;

        return amount * 0.001;
    }
}
