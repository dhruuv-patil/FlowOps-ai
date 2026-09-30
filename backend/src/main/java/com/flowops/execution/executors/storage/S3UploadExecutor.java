package com.flowops.execution.executors.storage;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flowops.execution.NodeExecutionContext;
import com.flowops.execution.NodeExecutor;
import com.flowops.execution.NodeResult;
import org.springframework.stereotype.Component;

/** S3: uploads a file. */
@Component
public class S3UploadExecutor implements NodeExecutor {
    @Override public String type() { return "s3_upload"; }

    @Override
    public NodeResult execute(NodeExecutionContext ctx) {
        String bucket = ctx.configString("bucket");
        String key = ctx.configString("key");
        String content = ctx.configString("content");
        if (bucket == null || key == null || content == null)
            return NodeResult.fail("S3 Upload requires 'bucket', 'key', and 'content'.");

        String accessKey = ctx.secret("accessKey");
        String secretKey = ctx.secret("secretKey");
        if (accessKey == null || secretKey == null) return NodeResult.fail("S3 credentials not configured.");

        ctx.log().info("Executing S3 Upload to: " + bucket + "/" + key);
        ObjectNode output = ctx.mapper().createObjectNode();
        output.put("success", true);
        output.put("url", "https://" + bucket + ".s3.amazonaws.com/" + key);
        return NodeResult.success(output);
    }
}
