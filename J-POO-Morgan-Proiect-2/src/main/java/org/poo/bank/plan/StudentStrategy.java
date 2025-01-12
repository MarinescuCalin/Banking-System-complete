package org.poo.bank.plan;

public class StudentStrategy implements PlanStrategy {
    @Override
    public String name() {
        return "student";
    }

    @Override
    public double getComision(double amount) {
        return 0;
    }
}
