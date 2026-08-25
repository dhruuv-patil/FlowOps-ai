package com.flowops.execution;

import org.springframework.stereotype.Component;

/**
 * Evaluates the boolean expression on a Condition node, after variables have been
 * interpolated into it.
 *
 * <p>Intentionally small and side-effect free — this is routing logic, not a
 * scripting engine, so it can never call out, read the filesystem, or mutate
 * state (contract §7: no arbitrary execution). Supported shapes:
 *
 * <ul>
 *   <li>{@code left OP right} where OP is {@code == != > < >= <=}; numeric when both
 *       sides parse as numbers, otherwise a string compare;</li>
 *   <li>{@code left contains right} — substring test;</li>
 *   <li>a bare value, which is truthy unless empty, {@code false}, {@code 0}, or
 *       {@code null}.</li>
 * </ul>
 */
@Component
public class ExpressionEvaluator {

    public boolean evaluate(String expression) {
        if (expression == null) {
            return false;
        }
        String expr = expression.trim();
        if (expr.isEmpty()) {
            return false;
        }

        for (String op : new String[] {"==", "!=", ">=", "<=", ">", "<"}) {
            int at = expr.indexOf(op);
            if (at > 0) {
                String left = unquote(expr.substring(0, at).trim());
                String right = unquote(expr.substring(at + op.length()).trim());
                return compare(left, right, op);
            }
        }

        int containsAt = indexOfWord(expr, "contains");
        if (containsAt >= 0) {
            String left = unquote(expr.substring(0, containsAt).trim());
            String right = unquote(expr.substring(containsAt + "contains".length()).trim());
            return left.contains(right);
        }

        return truthy(expr);
    }

    private boolean compare(String left, String right, String op) {
        Double ln = asNumber(left);
        Double rn = asNumber(right);
        if (ln != null && rn != null) {
            int c = Double.compare(ln, rn);
            return switch (op) {
                case "==" -> c == 0;
                case "!=" -> c != 0;
                case ">" -> c > 0;
                case "<" -> c < 0;
                case ">=" -> c >= 0;
                case "<=" -> c <= 0;
                default -> false;
            };
        }
        int c = left.compareTo(right);
        return switch (op) {
            case "==" -> left.equals(right);
            case "!=" -> !left.equals(right);
            case ">" -> c > 0;
            case "<" -> c < 0;
            case ">=" -> c >= 0;
            case "<=" -> c <= 0;
            default -> false;
        };
    }

    private boolean truthy(String value) {
        String v = value.trim();
        return !(v.isEmpty()
                || v.equalsIgnoreCase("false")
                || v.equals("0")
                || v.equalsIgnoreCase("null"));
    }

    private Double asNumber(String value) {
        try {
            return Double.valueOf(value);
        } catch (NumberFormatException notANumber) {
            return null;
        }
    }

    private String unquote(String value) {
        if (value.length() >= 2
                && ((value.startsWith("\"") && value.endsWith("\""))
                        || (value.startsWith("'") && value.endsWith("'")))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    /** Finds {@code contains} only as a standalone word bounded by whitespace. */
    private int indexOfWord(String expr, String word) {
        int from = 0;
        while (true) {
            int at = expr.indexOf(word, from);
            if (at < 0) {
                return -1;
            }
            boolean leftOk = at > 0 && Character.isWhitespace(expr.charAt(at - 1));
            int after = at + word.length();
            boolean rightOk = after < expr.length() && Character.isWhitespace(expr.charAt(after));
            if (leftOk && rightOk) {
                return at;
            }
            from = at + word.length();
        }
    }
}
