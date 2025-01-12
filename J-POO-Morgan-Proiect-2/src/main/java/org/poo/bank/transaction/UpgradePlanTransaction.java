package org.poo.bank.transaction;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class UpgradePlanTransaction extends Transaction {
    final String planType;

    public UpgradePlanTransaction(final int timestamp, final String iban,
                                  final String planType) {
        super(timestamp, "Upgrade plan", iban);

        this.planType = planType;
    }


    @Override
    public ObjectNode toObjectNode(final ObjectMapper objectMapper) {
        ObjectNode result = super.toObjectNode(objectMapper);

        result.put("accountIBAN", iban);
        result.put("newPlanType", planType);

        return result;
    }
}
