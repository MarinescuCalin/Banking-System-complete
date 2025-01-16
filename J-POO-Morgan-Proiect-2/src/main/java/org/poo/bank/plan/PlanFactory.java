package org.poo.bank.plan;

/**
 * A factory class for creating instances of {@link PlanStrategy} based on the given plan type.
 * This class is designed to provide a centralized way to instantiate different strategies
 * for various plans.
 * It uses the Factory Method design pattern to create and return the appropriate
 * {@link PlanStrategy} object.
 *
 * <p>The supported plan types are:
 * <ul>
 *     <li>"student" - {@link StudentStrategy}</li>
 *     <li>"standard" - {@link StandardStrategy}</li>
 *     <li>"silver" - {@link SilverStrategy}</li>
 *     <li>"gold" - {@link GoldStrategy}</li>
 * </ul>
 * </p>
 *
 * <p>If an unsupported plan type is provided, an {@link IllegalStateException} is thrown.</p>
 */
public final class PlanFactory {
    private PlanFactory() {

    }

    /**
     * Creates and returns an appropriate {@link PlanStrategy} based on the provided plan type.
     *
     * @param planType the type of the plan (e.g., "student", "standard", "silver", "gold").
     * @return the corresponding {@link PlanStrategy} instance for the given plan type.
     * @throws IllegalStateException if an unsupported plan type is provided.
     */
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
