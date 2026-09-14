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

/** Database Transaction: runs a list of statements in one transaction. */
@Component
public class DatabaseTransactionExecutor implements NodeExecutor {

    @Override
    public String type() {
        return "database_transaction";
    }

    @Override
    public NodeResult execute(NodeExecutionContext ctx) {
        String dbType = ctx.configString("databaseType");
        String host = ctx.configString("host");
        String portStr = ctx.configString("port");
        String database = ctx.configString("database");
        String username = ctx.configString("username");
        String password = ctx.configString("password");
        Object rawStatements = ctx.config().get("statements");
        JsonNode statementsNode = rawStatements instanceof JsonNode ? (JsonNode) rawStatements : null;

        if (dbType == null || dbType.isBlank()) return NodeResult.fail("Database Transaction requires 'databaseType'.");
        if (host == null || host.isBlank()) return NodeResult.fail("Database Transaction requires 'host'.");
        if (database == null || database.isBlank()) return NodeResult.fail("Database Transaction requires 'database'.");
        if (username == null || username.isBlank()) return NodeResult.fail("Database Transaction requires 'username'.");
        if (statementsNode == null || !statementsNode.isArray()) return NodeResult.fail("Database Transaction requires 'statements' array.");

        int port = defaultPort(dbType);
        if (portStr != null && !portStr.isBlank()) {
            try { port = Integer.parseInt(portStr); } catch (NumberFormatException ignored) {}
        }

        String url = buildUrl(dbType, host, port, database);

        Properties props = new Properties();
        props.setProperty("user", username);
        if (password != null) props.setProperty("password", password);
        if ("postgresql".equalsIgnoreCase(dbType)) props.setProperty("ssl", "false");
        if ("mysql".equalsIgnoreCase(dbType)) props.setProperty("useSSL", "false");

        try (Connection conn = DriverManager.getConnection(url, props)) {
            conn.setAutoCommit(false);
            try {
                ArrayNode results = ctx.mapper().createArrayNode();
                for (JsonNode stmtNode : statementsNode) {
                    String sql = stmtNode.asText();
                    String interpolated = ctx.interpolate(sql);
                    try (Statement stmt = conn.createStatement()) {
                        boolean hasResults = stmt.execute(interpolated);
                        ObjectNode result = ctx.mapper().createObjectNode();
                        result.put("sql", interpolated);
                        if (hasResults) {
                            try (ResultSet rs = stmt.getResultSet()) {
                                ArrayNode rows = ctx.mapper().createArrayNode();
                                ResultSetMetaData meta = rs.getMetaData();
                                int colCount = meta.getColumnCount();
                                while (rs.next()) {
                                    ObjectNode row = ctx.mapper().createObjectNode();
                                    for (int i = 1; i <= colCount; i++) {
                                        String colName = meta.getColumnLabel(i);
                                        Object val = rs.getObject(i);
                                        row.set(colName, toJsonNode(ctx, val));
                                    }
                                    rows.add(row);
                                }
                                result.set("rows", rows);
                                result.put("rowCount", rows.size());
                            }
                        } else {
                            result.put("affectedRows", stmt.getUpdateCount());
                        }
                        results.add(result);
                    }
                }
                conn.commit();
                ObjectNode output = ctx.mapper().createObjectNode();
                output.set("results", results);
                output.put("committed", true);
                ctx.log().info("Database Transaction (" + dbType + ") committed " + statementsNode.size() + " statements.");
                return NodeResult.success(output);
            } catch (Exception e) {
                conn.rollback();
                ctx.log().error("Database Transaction rolled back: " + e.getMessage());
                return NodeResult.fail("Transaction rolled back: " + e.getMessage());
            }
        } catch (SQLException e) {
            return NodeResult.fail(dbType + " error: " + e.getMessage());
        } catch (Exception e) {
            return NodeResult.fail(dbType + " driver not available: " + e.getMessage());
        }
    }

    private int defaultPort(String dbType) {
        return "postgresql".equalsIgnoreCase(dbType) ? 5432 : 3306;
    }

    private String buildUrl(String dbType, String host, int port, String database) {
        if ("postgresql".equalsIgnoreCase(dbType)) {
            return "jdbc:postgresql://" + host + ":" + port + "/" + database;
        } else {
            return "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&allowPublicKeyRetrieval=true";
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