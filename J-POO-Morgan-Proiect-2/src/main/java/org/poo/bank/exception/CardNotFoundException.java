package org.poo.bank.exception;

public class CardNotFoundException extends Exception {
    public CardNotFoundException() {
        super("Card not found");
    }
}
