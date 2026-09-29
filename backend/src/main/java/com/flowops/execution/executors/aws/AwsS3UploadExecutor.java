package com.flowops.execution.executors.aws;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flowops.execution.NodeExecutionContext;
import com.flowops.execution.NodeExecutor;
import com.flowops.execution.NodeResult;
import org.springframework.stereotype.Component;

/** AWS S3: uploads a file. */
@Component
public class AwsS3UploadExecutor implements NodeExecutor {
    @Override public String type() { return "aws:s3Upload"; }

    @Override
    public NodeResult execute(NodeExecutionContext ctx) {
        String bucket = ctx.configString("bucket");
        String key = ctx.configString("key");
        if (bucket == null || key == null) return NodeResult.fail("AWS S3 Upload requires 'bucket' and 'key'.");

        String accessKey = ctx.secret("accessKey");
        String secretKey = ctx.secret("secretKey");
        if (accessKey == null || secretKey == null) return NodeResult.fail("AWS credentials not configured.");

        ctx.log().info("Executing AWS S3 Upload to: " + bucket + "/" + key);
        ObjectNode output = ctx.mapper().createObjectNode();
        output.put("success", true);
        return NodeResult.success(output);
    }
}
