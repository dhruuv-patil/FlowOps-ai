package com.flowops.common.security;

import com.flowops.common.error.ApiException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Hermetic tests for the SSRF destination policy. No Mockito, no network: IP-literal
 * targets are checked without DNS, so every blocked case is provable in-process.
 */
class SsrfGuardTest {

    @Test
    void acceptsPublicIpLiteralTargets() {
        // Public literals need no DNS, so this is hermetic even in a sandboxed CI.
        // (93.184.216.34 is example.com's historic address; any public v4 works.)
        assertThat(SsrfGuard.validate("https://93.184.216.34/get").getHost())
                .isEqualTo("93.184.216.34");
        assertThat(SsrfGuard.validate("http://1.1.1.1").getScheme()).isEqualTo("http");
        // Port and path must survive validation untouched.
        assertThat(SsrfGuard.validate("https://93.184.216.34:8443/a/b").getPort()).isEqualTo(8443);
    }

    @Test
    void blocksLoopbackAndLocalhostNames() {
        for (String target : new String[] {
                "http://localhost/x", "http://localhost:8080/admin", "http://127.0.0.1/",
                "http://[::1]/", "http://metadata.localhost/"}) {
            assertThatThrownBy(() -> SsrfGuard.validate(target))
                    .as("target " + target)
                    .isInstanceOf(ApiException.class)
                    .hasMessageNotContaining("127.0.0.1"); // never echo the rejected URL
        }
    }

    @Test
    void blocksPrivateAndLinkLocalRanges() {
        for (String target : new String[] {
                "http://10.0.0.5/", "http://172.16.0.1/", "http://172.31.255.255/",
                "http://192.168.1.10/", "http://169.254.169.254/latest/meta-data/",
                "http://0.0.0.0/"}) {
            assertThatThrownBy(() -> SsrfGuard.validate(target))
                    .as("target " + target)
                    .isInstanceOf(ApiException.class);
        }
    }

    @Test
    void blocksIPv6UniqueLocalAndNonHttpSchemes() {
        assertThatThrownBy(() -> SsrfGuard.validate("http://[fc00::1]/")).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> SsrfGuard.validate("http://[fd12::1]/")).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> SsrfGuard.validate("file:///etc/passwd")).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> SsrfGuard.validate("ftp://example.com/")).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> SsrfGuard.validate("gopher://example.com/")).isInstanceOf(ApiException.class);
    }

    @Test
    void rejectsInternalLookingNamesAndMalformedUrls() {
        assertThatThrownBy(() -> SsrfGuard.validate("http://my-service.internal/")).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> SsrfGuard.validate("http://printer.local/")).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> SsrfGuard.validate("not a url")).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> SsrfGuard.validate("http://")).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> SsrfGuard.validate(null)).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> SsrfGuard.validate("   ")).isInstanceOf(ApiException.class);
    }
}
