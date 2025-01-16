package org.poo.fileio;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public interface JSONWritable {
    /**
     * Converts the implementing object to an {@link ObjectNode} using the provided
     * {@link ObjectMapper}. This method should return a JSON-compatible representation
     * of the current state of the object.
     *
     * @param objectMapper the {@link ObjectMapper} instance used to create the JSON node.
     *                     It provides methods for constructing {@link ObjectNode} and handling
     *                     JSON serialization.
     * @return an {@link ObjectNode} representing the object's state in JSON format.
     */
    ObjectNode toObjectNode(ObjectMapper objectMapper);
}
