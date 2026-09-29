package com.flowops.execution.executors.crm.salesforce;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flowops.execution.NodeExecutionContext;
import com.flowops.execution.NodeExecutor;
import com.flowops.execution.NodeResult;
import org.springframework.stereotype.Component;

/** Salesforce: creates a lead. */
@Component
public class SalesforceCreateLeadExecutor implements NodeExecutor {
    @Override public String type() { return "salesforce:createLead"; }

    @Override
    public NodeResult execute(NodeExecutionContext ctx) {
        String lastName = ctx.configString("lastName");
        String company = ctx.configString("company");
        if (lastName == null || company == null) return NodeResult.fail("Salesforce createLead requires 'lastName' and 'company'.");

        // Credentials would be fetched from integration context in a real implementation
        ctx.log().info("Executing Salesforce createLead for: " + lastName);
        ObjectNode output = ctx.mapper().createObjectNode();
        output.put("success", true);
        output.put("id", "00Q0x0000000000"); // Mock ID
        return NodeResult.success(output);
    }
}
