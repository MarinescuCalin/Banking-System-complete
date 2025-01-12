package org.poo.fileio;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public interface JSONWritable {

    ObjectNode toObjectNode(ObjectMapper objectMapper);
}
