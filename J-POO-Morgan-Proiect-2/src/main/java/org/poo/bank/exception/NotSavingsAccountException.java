package org.poo.bank.exception;

public class NotSavingsAccountException extends Exception {
    public NotSavingsAccountException() {
        super("This is not a savings account");
    }

}
