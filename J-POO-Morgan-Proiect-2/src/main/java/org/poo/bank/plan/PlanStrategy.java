package org.poo.bank.plan;

public interface PlanStrategy {
    String name();

    double getComision(double amount);
}
