package org.poo.bank.transaction;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import org.poo.fileio.JSONWritable;

import java.util.HashSet;
import java.util.Set;


/**
 * Represents a financial transaction associated with a bank account.
 * This class implements the {@code JSONWritable} interface to allow
 * transactions to be serialized into a JSON format.
 */
public class Transaction implements JSONWritable {
    @Getter
    protected final int timestamp;
    protected String description;
    protected final String iban;

    public Transaction(final int timestamp, final String description, final String iban) {
        this.timestamp = timestamp;
        this.description = description;

        this.iban = iban;
    }


    /**
     * Converts this transaction into a JSON object.
     *
     * @param objectMapper the {@code ObjectMapper} instance to use for creating the JSON object
     * @return an {@code ObjectNode} representing the transaction as JSON
     */
    @Override
    public ObjectNode toObjectNode(final ObjectMapper objectMapper) {
        ObjectNode result = objectMapper.createObjectNode();

        result.put("timestamp", timestamp);
        result.put("description", description);

        return result;
    }


    /**
     * Returns the commerciant (merchant) associated with this transaction.
     * By default, this method returns {@code null} and is intended to be overridden by subclasses.
     *
     * @return the name of the commerciant, or {@code null} if not applicable
     */
    public String getCommerciant() {
        return null;
    }


    /**
     * Returns the monetary amount involved in this transaction.
     * By default, this method returns {@code null} and is intended to be overridden by subclasses.
     *
     * @return the amount of the transaction, or {@code null} if not applicable
     */
    public Double getAmount() {
        return null;
    }


    /**
     * Returns a set of IBANs associated with this transaction.
     * By default, this set contains only the primary IBAN of this transaction.
     *
     * @return a {@code Set<String>} containing the IBAN(s) associated with this transaction
     */
    public Set<String> getIBAN() {
        final Set<String> set = new HashSet<>();
        set.add(iban);
        return set;
    }
}
