package com.flowops.execution.executors;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flowops.execution.NodeExecutionContext;
import com.flowops.execution.NodeExecutor;
import com.flowops.execution.NodeResult;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

/** Date Formatter: formats, adds, or subtracts time from a date. */
@Component
public class DateFormatterExecutor implements NodeExecutor {

    @Override
    public String type() {
        return "date_formatter";
    }

    @Override
    public NodeResult execute(NodeExecutionContext ctx) {
        String input = ctx.configString("input");
        String fromFormat = ctx.configString("fromFormat");
        String toFormat = ctx.configString("toFormat");
        String operation = ctx.configString("operation");
        String daysStr = ctx.configString("days");

        if (toFormat == null || toFormat.isBlank()) {
            return NodeResult.fail("Date Formatter requires a 'toFormat' (java.time pattern).");
        }

        Instant instant;
        if (input == null || input.isBlank()) {
            instant = Instant.now();
        } else {
            String interpolated = ctx.interpolate(input);
            try {
                if (fromFormat != null && !fromFormat.isBlank()) {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(fromFormat);
                    LocalDateTime ldt = LocalDateTime.parse(interpolated, formatter);
                    instant = ldt.atZone(ZoneId.systemDefault()).toInstant();
                } else {
                    instant = Instant.parse(interpolated);
                }
            } catch (Exception e) {
                return NodeResult.fail("Date Formatter: invalid input date - " + e.getMessage());
            }
        }

        ZonedDateTime zdt = instant.atZone(ZoneId.systemDefault());
        if ("addDays".equals(operation)) {
            int days = parseDays(daysStr);
            zdt = zdt.plusDays(days);
        } else if ("subtractDays".equals(operation)) {
            int days = parseDays(daysStr);
            zdt = zdt.minusDays(days);
        }

        String formatted;
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(toFormat);
            formatted = zdt.format(formatter);
        } catch (Exception e) {
            return NodeResult.fail("Date Formatter: invalid output pattern - " + e.getMessage());
        }

        ObjectNode output = ctx.mapper().createObjectNode();
        output.put("result", formatted);
        output.put("iso", zdt.toInstant().toString());
        ctx.log().info("Date Formatter: formatted to " + formatted);
        return NodeResult.success(output);
    }

    private int parseDays(String daysStr) {
        if (daysStr == null || daysStr.isBlank()) return 0;
        try {
            return Integer.parseInt(daysStr);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }
}