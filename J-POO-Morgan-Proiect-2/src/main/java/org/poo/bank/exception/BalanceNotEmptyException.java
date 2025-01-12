package org.poo.bank.exception;

public class BalanceNotEmptyException extends Exception {
    public BalanceNotEmptyException() {
        super("Account couldn't be deleted - see org.poo.transactions for details");
    }

}
