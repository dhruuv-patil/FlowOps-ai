package com.flowops.execution.executors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flowops.execution.NodeExecutionContext;
import com.flowops.execution.NodeExecutor;
import com.flowops.execution.NodeResult;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.util.Properties;

/** PostgreSQL: runs SQL against a PostgreSQL database via its JDBC driver. */
@Component
public class PostgreSqlExecutor implements NodeExecutor {

    @Override
    public String type() {
        return "postgresql";
    }

    @Override
    public NodeResult execute(NodeExecutionContext ctx) {
        String host = ctx.configString("host");
        String portStr = ctx.configString("port");
        String database = ctx.configString("database");
        String username = ctx.configString("username");
        String password = ctx.configString("password");
        String sql = ctx.configString("sql");
        String limitStr = ctx.configString("limit");

        if (host == null || host.isBlank()) return NodeResult.fail("PostgreSQL requires 'host'.");
        if (database == null || database.isBlank()) return NodeResult.fail("PostgreSQL requires 'database'.");
        if (username == null || username.isBlank()) return NodeResult.fail("PostgreSQL requires 'username'.");
        if (sql == null || sql.isBlank()) return NodeResult.fail("PostgreSQL requires 'sql'.");

        int port = 5432;
        if (portStr != null && !portStr.isBlank()) {
            try { port = Integer.parseInt(portStr); } catch (NumberFormatException ignored) {}
        }

        int limit = 100;
        if (limitStr != null && !limitStr.isBlank()) {
            try { limit = Integer.parseInt(limitStr); } catch (NumberFormatException ignored) {}
        }

        String interpolatedSql = ctx.interpolate(sql);
        String url = "jdbc:postgresql://" + host + ":" + port + "/" + database;

        Properties props = new Properties();
        props.setProperty("user", username);
        if (password != null) props.setProperty("password", password);
        props.setProperty("ssl", "false");

        try (Connection conn = DriverManager.getConnection(url, props)) {
            boolean isSelect = interpolatedSql.trim().toUpperCase().startsWith("SELECT");
            if (isSelect) {
                try (PreparedStatement stmt = conn.prepareStatement(interpolatedSql);
                     ResultSet rs = stmt.executeQuery()) {
                    ArrayNode rows = ctx.mapper().createArrayNode();
                    ResultSetMetaData meta = rs.getMetaData();
                    int colCount = meta.getColumnCount();
                    int count = 0;
                    while (rs.next() && count < limit) {
                        ObjectNode row = ctx.mapper().createObjectNode();
                        for (int i = 1; i <= colCount; i++) {
                            String colName = meta.getColumnLabel(i);
                            Object val = rs.getObject(i);
                            row.set(colName, toJsonNode(ctx, val));
                        }
                        rows.add(row);
                        count++;
                    }
                    ObjectNode output = ctx.mapper().createObjectNode();
                    output.set("rows", rows);
                    output.put("count", count);
                    ctx.log().info("PostgreSQL SELECT returned " + count + " rows.");
                    return NodeResult.success(output);
                }
            } else {
                try (Statement stmt = conn.createStatement()) {
                    int affected = stmt.executeUpdate(interpolatedSql);
                    ObjectNode output = ctx.mapper().createObjectNode();
                    output.put("affectedRows", affected);
                    ctx.log().info("PostgreSQL executed, " + affected + " rows affected.");
                    return NodeResult.success(output);
                }
            }
                } catch (SQLException e) {
            return NodeResult.fail("PostgreSQL error: " + e.getMessage());
        } catch (Exception e) {
            return NodeResult.fail("PostgreSQL driver not available: " + e.getMessage());
        }
    }

    private JsonNode toJsonNode(NodeExecutionContext ctx, Object val) {
        if (val == null) return ctx.mapper().nullNode();
        if (val instanceof Number) return ctx.mapper().valueToTree(val);
        if (val instanceof Boolean) return ctx.mapper().valueToTree(val);
        if (val instanceof java.sql.Timestamp) return ctx.mapper().getNodeFactory().textNode(val.toString());
        if (val instanceof java.sql.Date) return ctx.mapper().getNodeFactory().textNode(val.toString());
        if (val instanceof java.sql.Time) return ctx.mapper().getNodeFactory().textNode(val.toString());
        return ctx.mapper().getNodeFactory().textNode(val.toString());
    }
}