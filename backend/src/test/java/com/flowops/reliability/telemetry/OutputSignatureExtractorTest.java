package com.flowops.reliability.telemetry;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link OutputSignatureExtractor} — the structural signature that
 * powers "successful but abnormal" output detection. No Spring context.
 *
 * <p>These pin the two security-relevant guarantees: the signature captures structure
 * (field names + types + null-presence) but <em>never any value</em>, so it is safe to
 * store alongside telemetry; and it distinguishes the shapes the output detector cares
 * about (object/array/scalar/null/empty and per-field type changes).
 */
class OutputSignatureExtractorTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final OutputSignatureExtractor extractor = new OutputSignatureExtractor();

    @Test
    void objectSignatureListsFieldTypesButNeverValues() throws Exception {
        Map<String, Object> s = extractor.signature(mapper.readTree(
                """
                {"company_size": 1200, "name": "Acme", "active": true, "note": null}
                """));

        assertThat(s.get("form")).isEqualTo("OBJECT");
        assertThat(s.get("fieldCount")).isEqualTo(4);
        @SuppressWarnings("unchecked")
        Map<String, Object> fields = (Map<String, Object>) s.get("fields");
        assertThat(fields).containsEntry("company_size", Map.of("type", "number", "null", false));
        assertThat(fields).containsEntry("name", Map.of("type", "string", "null", false));
        assertThat(fields).containsEntry("active", Map.of("type", "boolean", "null", false));
        assertThat(fields).containsEntry("note", Map.of("type", "null", "null", true));
        // The signature must never contain the actual values.
        assertThat(s.toString())
                .doesNotContain("Acme")
                .doesNotContain("1200")
                .doesNotContain("company_size=1200");
    }

    @Test
    void aFieldChangingTypeIsReflected() throws Exception {
        Map<String, Object> asNumber = extractor.signature(
                mapper.readTree("{\"score\": 92}"));
        Map<String, Object> asString = extractor.signature(
                mapper.readTree("{\"score\": \"unknown\"}"));

        @SuppressWarnings("unchecked")
        Map<String, Object> number = (Map<String, Object>) ((Map<String, Object>) asNumber.get("fields")).get("score");
        @SuppressWarnings("unchecked")
        Map<String, Object> string = (Map<String, Object>) ((Map<String, Object>) asString.get("fields")).get("score");

        assertThat(number.get("type")).isEqualTo("number");
        assertThat(string.get("type")).isEqualTo("string");
        assertThat(number).isNotEqualTo(string);
    }

    @Test
    void missingFieldChangesFieldCountAndFieldPresence() throws Exception {
        Map<String, Object> full = extractor.signature(
                mapper.readTree("{\"a\": 1, \"b\": 2}"));
        Map<String, Object> partial = extractor.signature(
                mapper.readTree("{\"a\": 1}"));

        assertThat(full.get("fieldCount")).isEqualTo(2);
        assertThat(partial.get("fieldCount")).isEqualTo(1);
        @SuppressWarnings("unchecked")
        Map<String, Object> partialFields = (Map<String, Object>) partial.get("fields");
        assertThat(partialFields).doesNotContainKey("b");
    }

    @Test
    void arrayScalarNullAndEmptyAreDistinguished() {
        assertThat(extractor.signature(mapper.createArrayNode()).get("form")).isEqualTo("ARRAY");
        assertThat(extractor.signature(mapper.createObjectNode()).get("form")).isEqualTo("EMPTY");
        assertThat(extractor.signature(mapper.nullNode()).get("form")).isEqualTo("NULL");
        assertThat(extractor.signature(mapper.getNodeFactory().textNode("hello")).get("form"))
                .isEqualTo("SCALAR");
        assertThat(extractor.signature(mapper.createArrayNode().add(1)).get("size")).isEqualTo(1);
    }

    @Test
    void anArraySignatureCapturesSize() throws Exception {
        Map<String, Object> s = extractor.signature(mapper.readTree("[1,2,3,4,5]"));
        assertThat(s.get("form")).isEqualTo("ARRAY");
        assertThat(s.get("size")).isEqualTo(5);
    }

    @Test
    void nullAndMissingInputsYieldNullForm() {
        assertThat(extractor.signature(null).get("form")).isEqualTo("NULL");
    }

    @Test
    void nestedObjectsAreCapturedAsObjectTypeWithoutRecursiveValues() throws Exception {
        Map<String, Object> s = extractor.signature(
                mapper.readTree("{\"nested\": {\"secret\": \"sensitive-value\"}}"));
        @SuppressWarnings("unchecked")
        Map<String, Object> fields = (Map<String, Object>) s.get("fields");
        assertThat(fields.get("nested")).isEqualTo(Map.of("type", "object", "null", false));
        assertThat(s.toString()).doesNotContain("sensitive-value");
        List.of("ACCEPTED", "from INTEGRATION_CREDENTIALS it is never read", "x-api-key").forEach(
                secret -> assertThat(s.toString()).doesNotContain(secret));
    }
}