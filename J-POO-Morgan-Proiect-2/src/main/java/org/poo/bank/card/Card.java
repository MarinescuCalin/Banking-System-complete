package org.poo.bank.card;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.Setter;
import org.poo.fileio.JSONWritable;
import org.poo.utils.Utils;

/**
 * Abstract class representing a bank card.
 * Manages basic card attributes such as card number, status, and whether it is a one-time card.
 * This class implements the {@link JSONWritable} interface, allowing for JSON serialization.
 */
@Getter
public abstract class Card implements JSONWritable {
    /**
     * The card number, generated when the card is created.
     */
    protected final String cardNumber;

    /**
     * Indicates whether the card is a one-time use card.
     */
    protected final boolean oneTime;

    @Setter
    protected String status;

    /**
     * Constructs a new {@link Card} instance with the specified one-time use status.
     * The card number is generated automatically, and the card's status
     * is initially set to "active".
     *
     * @param oneTime A boolean indicating whether this is a one-time use card.
     */

    public Card(final boolean oneTime) {
        this.cardNumber = Utils.generateCardNumber();
        this.status = "active";

        this.oneTime = oneTime;
    }

    /**
     * Converts this {@link Card} instance to a JSON object.
     * The JSON object includes the card number and status as properties.
     *
     * @param objectMapper the {@link ObjectMapper} used to create the JSON representation.
     * @return an {@link ObjectNode} representing this card.
     */
    @Override
    public ObjectNode toObjectNode(final ObjectMapper objectMapper) {
        ObjectNode result = objectMapper.createObjectNode();

        result.put("cardNumber", cardNumber);
        result.put("status", status);

        return result;
    }
}
