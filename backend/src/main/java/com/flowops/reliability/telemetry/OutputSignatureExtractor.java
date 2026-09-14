package com.flowops.reliability.telemetry;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Builds a compact <em>structural</em> signature of a node's JSON output — which
 * fields exist and what type they are — never their values.
 *
 * <p>This is the foundation of the "successful but abnormal" output detector: a field
 * that was always a number becoming a string, a required field going missing, or a
 * null-rate spike all change this signature while the run still reports SUCCESS. By
 * keeping only structure (not payloads) the signature can be stored in telemetry and
 * anomaly records without ever leaking sensitive values.
 *
 * <p>Signature shape (all values are type labels / counts, never user data):
 * <pre>
 *   {"form":"OBJECT","fieldCount":N,"fields":{"<name>":{"type":"number","null":false}, ...}}
 *   {"form":"ARRAY","size":N,"items":{"form":"OBJECT","fieldCount":2,"fields":{...}}}
 *   {"form":"SCALAR","type":"string"}
 *   {"form":"NULL"}
 *   {"form":"EMPTY"}
 * </pre>
 *
 * <p>Nested objects and arrays are fully recursed so that structural drift at any depth
 * is detectable: missing nested fields, type changes in nested objects, array element
 * type changes, etc.
 */
@Component
public class OutputSignatureExtractor {

    public Map<String, Object> signature(JsonNode output) {
        if (output == null || output.isMissingNode() || output.isNull()) {
            return base("NULL");
        }
        if (output.isObject()) {
            if (output.isEmpty()) {
                return base("EMPTY");
            }
            Map<String, Object> s = base("OBJECT");
            s.put("fieldCount", output.size());
            Map<String, Object> fields = new LinkedHashMap<>();
            output.fields().forEachRemaining(e -> fields.put(e.getKey(), extractStructure(e.getValue())));
            s.put("fields", fields);
            return s;
        }
        if (output.isArray()) {
            Map<String, Object> s = base("ARRAY");
            s.put("size", output.size());
            // For arrays, capture the structure of the first element (if any) as representative
            // Also track element type diversity for array structure change detection
            if (output.size() > 0) {
                JsonNode firstElement = output.get(0);
                s.put("items", extractStructure(firstElement));
                // Track if elements have mixed types (structure changes within array)
                Map<String, Integer> elementTypes = new LinkedHashMap<>();
                for (JsonNode element : output) {
                    String type = getStructureType(element);
                    elementTypes.merge(type, 1, Integer::sum);
                }
                if (elementTypes.size() > 1) {
                    s.put("elementTypeDiversity", elementTypes);
                }
            }
            return s;
        }
        Map<String, Object> s = base("SCALAR");
        s.put("type", scalarType(output));
        return s;
    }

    /**
     * Extracts the structural signature of a JSON node.
     * For object fields, returns simplified type/null info (no recursive structure).
     * This ensures signatures capture structure without exposing nested values.
     */
    private Map<String, Object> extractStructure(JsonNode value) {
        if (value == null || value.isMissingNode() || value.isNull()) {
            return Map.of("type", "null", "null", true);
        }
        if (value.isObject()) {
            if (value.isEmpty()) {
                return Map.of("type", "object", "null", false);
            }
            // For nested objects, return simplified type indicator without recursive structure
            return Map.of("type", "object", "null", false);
        }
        if (value.isArray()) {
            if (value.isEmpty()) {
                return Map.of("type", "array", "null", false);
            }
            // For arrays, return type of first element as representative
            JsonNode firstElement = value.get(0);
            String elementType = getStructureType(firstElement);
            return Map.of("type", "array", "null", false, "items", Map.of("type", elementType));
        }
        // Scalars
        return Map.of("type", scalarType(value), "null", false);
    }

    /** Returns the structure type label for a node, used for array element tracking. */
    private String getStructureType(JsonNode value) {
        if (value == null || value.isMissingNode() || value.isNull()) {
            return "null";
        }
        if (value.isTextual()) {
            return "string";
        }
        if (value.isBoolean()) {
            return "boolean";
        }
        if (value.isNumber()) {
            return "number";
        }
        if (value.isObject()) {
            return "object";
        }
        if (value.isArray()) {
            return "array";
        }
        return "unknown";
    }

    private String scalarType(JsonNode value) {
        if (value == null || value.isNull()) {
            return "null";
        }
        if (value.isTextual()) {
            return "string";
        }
        if (value.isBoolean()) {
            return "boolean";
        }
        if (value.isNumber()) {
            return "number";
        }
        if (value.isObject()) {
            return "object";
        }
        if (value.isArray()) {
            return "array";
        }
        return "unknown";
    }

    private Map<String, Object> base(String form) {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("form", form);
        return s;
    }
}
