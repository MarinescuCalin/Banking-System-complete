package org.poo.bank.exception;

public class CardFrozenException extends Exception {
    public CardFrozenException(final String message) {
        super(message);
    }
}
