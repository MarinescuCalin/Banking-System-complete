package org.poo.bank;

import lombok.Setter;
import org.poo.bank.account.Account;
import org.poo.bank.transaction.SplitPaymentTransaction;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a split payment operation that can be accepted or rejected by users.
 * The payment is distributed among multiple users, each contributing a portion of the total
 * amount.
 * Implements the {@link PaymentObserver} interface to handle payment status updates.
 */
public final class SplitPayment implements PaymentObserver {
    private final List<Double> amounts;
    private final List<Double> convertedAmounts;
    private final List<User> users;
    private final List<String> ibans;
    private final double totalAmount;
    private final String currency;
    private final String description;
    private final String type;
    private final int timestamp;
    private int noAccepted;

    /**
     * Private constructor to initialize a SplitPayment instance.
     * This constructor is called through the {@link SplitPaymentBuilder}.
     *
     * @param splitBuilder the builder object containing the necessary split payment details.
     */
    private SplitPayment(final SplitPaymentBuilder splitBuilder) {
        this.amounts = splitBuilder.amounts;
        this.totalAmount = splitBuilder.totalAmount;
        this.currency = splitBuilder.currency;
        this.type = splitBuilder.type;
        this.users = splitBuilder.users;
        this.ibans = splitBuilder.ibans;
        this.timestamp = splitBuilder.timestamp;
        this.noAccepted = 0;

        this.convertedAmounts = new ArrayList<>();
        for (int i = 0; i < ibans.size(); i++) {
            final User user = users.get(i);
            final String iban = ibans.get(i);
            final Account account = user.getAccount(iban);
            final double amount = getPayAmountFor(i);

            this.convertedAmounts.add(Bank.getInstance()
                    .convertCurrency(amount, currency, account.getCurrency()));
        }

        this.description = "Split payment of " + String.format("%.2f", totalAmount)
                + " " + currency;
    }

    /**
     * Updates the split payment status when a user accepts or rejects the payment request.
     * If all users accept the payment, the funds are deducted from their accounts.
     * If any user rejects the payment or has insufficient funds, the split payment is canceled.
     *
     * @param accepted true if the user accepted the payment, false otherwise.
     * @param email    the email of the user responding to the payment request.
     */
    @Override
    public void update(final boolean accepted, final String email) {
        if (!accepted) {
            cancelSplitPayment(null);
            return;
        }

        noAccepted++;
        if (noAccepted == ibans.size()) {
            for (int i = 0; i < ibans.size(); i++) {
                final User user = users.get(i);
                final String iban = ibans.get(i);
                final Account account = user.getAccount(iban);

                if (account.getBalance() < convertedAmounts.get(i)) {
                    cancelSplitPayment(iban);
                    return;
                }
            }

            for (int i = 0; i < ibans.size(); i++) {
                final String iban = ibans.get(i);
                final User user = users.get(i);
                final double amount = getPayAmountFor(i);

                user.splitPay(iban, amount, currency);
                user.addTransaction(new SplitPaymentTransaction(timestamp, description, amounts,
                        type, currency, ibans, null, iban));
            }
        }
    }

    /**
     * Cancels the split payment and records the reason for the cancellation.
     * This method is triggered if a user rejects the payment or has insufficient funds.
     *
     * @param cantAffordAccount the IBAN of the account that could not afford the payment,
     *                          or null if rejected.
     */
    private void cancelSplitPayment(final String cantAffordAccount) {
        String error = "One user rejected the payment.";
        if (cantAffordAccount != null) {
            error = "Account " + cantAffordAccount
                    + " has insufficient funds for a split payment.";
        }

        for (int i = 0; i < ibans.size(); i++) {
            final String iban = ibans.get(i);
            final User user = users.get(i);

            user.addTransaction(new SplitPaymentTransaction(timestamp, description, amounts, type,
                    currency, ibans, error, iban));
        }
    }


    /**
     * Retrieves the payment amount for a specific user based on the payment type.
     * If the type is "equal", the first amount is returned for all users.
     *
     * @param index the index of the user for whom the amount is being calculated.
     * @return the amount the user needs to pay.
     */
    private double getPayAmountFor(final int index) {
        if (type.equals("equal")) {
            return amounts.getFirst();
        }

        return amounts.get(index);
    }

    /**
     * Builder class to construct {@link SplitPayment} instances.
     * This class follows the builder pattern to ensure immutable creation of SplitPayment objects.
     */
    public static final class SplitPaymentBuilder {
        private final List<Double> amounts;
        private final List<User> users;
        private final List<String> ibans;
        @Setter
        private double totalAmount;
        @Setter
        private String currency;
        @Setter
        private String type;
        @Setter
        private int timestamp;

        /**
         * Initializes an empty SplitPaymentBuilder instance.
         * Lists for amounts, users, and IBANs are initialized to empty collections.
         */
        public SplitPaymentBuilder() {
            amounts = new ArrayList<>();
            users = new ArrayList<>();
            ibans = new ArrayList<>();
        }

        /**
         * Adds an amount to the list of split payments.
         *
         * @param amount the amount to be added.
         */
        public void addAmount(final double amount) {
            amounts.add(amount);
        }

        /**
         * Adds a user to the split payment.
         *
         * @param user the user to be added to the payment.
         */
        public void addUser(final User user) {
            users.add(user);
        }

        /**
         * Adds an IBAN to the list of participating accounts.
         *
         * @param iban the IBAN of the user's account.
         */
        public void addIBAN(final String iban) {
            ibans.add(iban);
        }

        /**
         * Builds and returns a new {@link SplitPayment} instance using the provided data.
         *
         * @return a fully constructed SplitPayment object.
         */
        public SplitPayment build() {
            return new SplitPayment(this);
        }
    }
}
