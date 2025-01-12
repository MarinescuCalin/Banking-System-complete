package org.poo.bank.plan;

public class PlanFactory {
    private PlanFactory() {

    }

    public static PlanStrategy createPlan(final String planType) {
        return switch (planType) {
            case "student" -> new StudentStrategy();
            case "standard" -> new StandardStrategy();
            case "silver" -> new SilverStrategy();
            case "gold" -> new GoldStrategy();
            default -> throw new IllegalStateException("Unexpected value: " + planType);
        };
    }
}
