package com.flowops.integration.provider;

import com.flowops.common.error.ApiException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class IntegrationRegistryTest {

    private IntegrationProvider provider(IntegrationType type) {
        return new IntegrationProvider() {
            public IntegrationType type() { return type; }
            public String displayName() { return type.wire(); }
            public String description() { return "Test provider"; }
            public List<CredentialField> credentialFields() {
                return List.of(new CredentialField("token", "Token", true, "", "", ""));
            }
        };
    }

    @Test
    void onlyInstalledProvidersAreListedInStableOrder() {
        var slack = provider(IntegrationType.SLACK);
        var github = provider(IntegrationType.GITHUB);
        var registry = new IntegrationRegistry(List.of(slack, github));
        assertThat(registry.all()).containsExactly(github, slack);
        assertThat(registry.require("SLACK")).isSameAs(slack);
        assertThatThrownBy(() -> registry.require("temporal")).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> registry.require(null)).isInstanceOf(ApiException.class);
    }

    @Test
    void duplicateIdentifiersFailFast() {
        assertThatThrownBy(() -> new IntegrationRegistry(List.of(
                provider(IntegrationType.SLACK), provider(IntegrationType.SLACK))))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsUnknownFieldsWithoutEchoingTheirNamesOrValues() {
        var provider = provider(IntegrationType.SLACK);
        provider.validateCredentialFields(Map.of("token", "accepted"));
        assertThatThrownBy(() -> provider.validateCredentialFields(Map.of("secret-value-as-key", "secret-value")))
                .isInstanceOf(ApiException.class)
                .hasMessageNotContaining("secret-value");
        assertThatThrownBy(() -> provider.validateCredentialFields(null)).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> provider.validateCredentialFields(Map.of())).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> provider.validateCredentialFields(Map.of("token", "x".repeat(16385))))
                .isInstanceOf(ApiException.class);
        var nullable = new HashMap<String, String>();
        nullable.put("token", null);
        assertThatThrownBy(() -> provider.validateCredentialFields(nullable)).isInstanceOf(ApiException.class);
    }

    @Test
    void credentialsAreImmutableAndToStringIsRedacted() {
        var source = new HashMap<>(Map.of("token", "sensitive-canary"));
        var credentials = new DecryptedCredentials(source);
        source.put("token", "changed");
        assertThat(credentials.get("token")).isEqualTo("sensitive-canary");
        assertThat(credentials.toString()).isEqualTo("DecryptedCredentials[redacted]");
        assertThatThrownBy(() -> credentials.secrets().put("token", "changed"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
