package org.poo.bank;

/**
 * Interface for observers that wish to be notified about payment acceptance or rejection.
 * Implementing classes can register to be notified when a payment transaction
 * is accepted or rejected.
 */
public interface PaymentObserver {
    /**
     * This method is called to update the observer about the payment status.
     *
     * @param accepted {@code true} if the payment was accepted, {@code false} if rejected.
     * @param email    the email of the user associated with the payment.
     */
    void update(boolean accepted, String email);
}
