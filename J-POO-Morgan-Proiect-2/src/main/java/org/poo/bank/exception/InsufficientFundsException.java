package org.poo.bank.exception;

public class InsufficientFundsException extends Exception {
    public InsufficientFundsException() {
        super("Insufficient funds");
    }
}
