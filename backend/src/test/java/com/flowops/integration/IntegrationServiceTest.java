package com.flowops.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.flowops.api.IntegrationResponse;
import com.flowops.common.crypto.CredentialCipher;
import com.flowops.common.error.ApiException;
import com.flowops.common.error.ErrorCode;
import com.flowops.config.CryptoProperties;
import com.flowops.domain.Integration;
import com.flowops.domain.IntegrationCredential;
import com.flowops.integration.provider.WorkflowProviderRegistry;
import com.flowops.repository.IntegrationCredentialRepository;
import com.flowops.repository.IntegrationRepository;
import com.flowops.security.FlowOpsPrincipal;
import com.flowops.domain.Role;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Slice tests for {@link IntegrationService}, the connect/disconnect choke
 * point.
 *
 * <p>
 * Repositories are mocked; a real {@link CredentialCipher} (known test key)
 * proves the encryption actually happens end-to-end.
 *
 * <p>
 * The invariants under test are the security-critical ones: the plaintext
 * webhook URL is encrypted before it reaches the DB and never appears in a
 * client response; a malformed or non-Slack URL is rejected before anything
 * is stored; another tenant's integration is indistinguishable from absent
 * (404); and disconnecting destroys the stored secret.
 */
class IntegrationServiceTest {

        private static final String WEBHOOK_URL = "https://hooks.slack.com/services/T00000000/B00000000/abcdefghijklmnopqrstuvwxyz1234567890";
        /*
         * Mirrors the app's Spring-Boot-configured ObjectMapper.
         */
        private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

        /**
         * The audit trail is exercised by its own test; here it only needs to
         * accept calls.
         */
        private static com.flowops.audit.AuditService audit() {
                return mock(com.flowops.audit.AuditService.class);
        }

        /**
         * The provider registry is only consulted for provider-type connects;
         * Slack tests never hit it.
         */
        private static WorkflowProviderRegistry registry() {
                return mock(WorkflowProviderRegistry.class);
        }

        private static CredentialCipher cipher() {
                byte[] raw = new byte[CryptoProperties.KEY_BYTES];

                for (int i = 0; i < raw.length; i++) {
                        raw[i] = (byte) (i * 3 + 5);
                }

                return new CredentialCipher(
                                new CryptoProperties(
                                                Base64.getEncoder().encodeToString(raw)));
        }

        private FlowOpsPrincipal admin(UUID orgId) {
                return new FlowOpsPrincipal(
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                orgId,
                                Role.ADMIN,
                                "admin@example.com");
        }

        @Test
        void connectEncryptsTheUrlAndNeverReturnsPlaintext() throws Exception {
                UUID orgId = UUID.randomUUID();

                IntegrationRepository integrations = mock(IntegrationRepository.class);

                IntegrationCredentialRepository credentials = mock(IntegrationCredentialRepository.class);

                CredentialCipher cipher = cipher();

                when(
                                integrations.findByOrganizationIdAndType(
                                                orgId,
                                                Integration.TYPE_SLACK))
                                .thenReturn(Optional.empty());

                IntegrationService service = new IntegrationService(
                                integrations,
                                credentials,
                                cipher,
                                audit(),
                                registry());

                IntegrationResponse response = service.connect(
                                admin(orgId),
                                new ConnectIntegrationRequest(
                                                "slack",
                                                WEBHOOK_URL));

                /*
                 * The stored credential is ciphertext — not the URL —
                 * and decrypts back to the expected credential map.
                 */
                ArgumentCaptor<IntegrationCredential> saved = ArgumentCaptor.forClass(
                                IntegrationCredential.class);

                verify(credentials).save(saved.capture());

                String ciphertext = saved.getValue().getCiphertext();

                assertThat(ciphertext)
                                .doesNotContain(WEBHOOK_URL);

                Map<String, String> decrypted = cipher.decryptToMap(ciphertext);

                assertThat(decrypted)
                                .containsEntry(
                                                "webhookUrl",
                                                WEBHOOK_URL);

                /*
                 * The response carries only the masked last-4,
                 * and its full JSON never leaks the URL.
                 */
                assertThat(response.status())
                                .isEqualTo(
                                                Integration.STATUS_CONNECTED);

                assertThat(response.hint()).isEqualTo("7890");

                String json = MAPPER.writeValueAsString(response);

                assertThat(json)
                                .doesNotContain(WEBHOOK_URL);

                assertThat(json)
                                .doesNotContain("hooks.slack.com");
        }

        @Test
        void connectRejectsANonSlackUrlBeforeStoringAnything() {
                UUID orgId = UUID.randomUUID();

                IntegrationRepository integrations = mock(IntegrationRepository.class);

                IntegrationCredentialRepository credentials = mock(IntegrationCredentialRepository.class);

                IntegrationService service = new IntegrationService(
                                integrations,
                                credentials,
                                cipher(),
                                audit(),
                                registry());

                assertThatThrownBy(
                                () -> service.connect(
                                                admin(orgId),
                                                new ConnectIntegrationRequest(
                                                                "slack",
                                                                "https://evil.example.com/x")))
                                .isInstanceOf(ApiException.class)
                                .extracting(
                                                ex -> ((ApiException) ex).code())
                                .isEqualTo(
                                                ErrorCode.INTEGRATION_INVALID);

                verify(integrations, never()).save(any());

                verify(credentials, never()).save(any());
        }

        @Test
        void connectRejectsAMalformedUrl() {
                UUID orgId = UUID.randomUUID();

                IntegrationService service = new IntegrationService(
                                mock(IntegrationRepository.class),
                                mock(IntegrationCredentialRepository.class),
                                cipher(),
                                audit(),
                                registry());

                assertThatThrownBy(
                                () -> service.connect(
                                                admin(orgId),
                                                new ConnectIntegrationRequest(
                                                                "slack",
                                                                "h ttp://not a url")))
                                .isInstanceOf(ApiException.class)
                                .extracting(
                                                ex -> ((ApiException) ex).code())
                                .isEqualTo(
                                                ErrorCode.INTEGRATION_INVALID);
        }

        @Test
        void connectRejectsAnUnsupportedType() {
                UUID orgId = UUID.randomUUID();

                IntegrationService service = new IntegrationService(
                                mock(IntegrationRepository.class),
                                mock(IntegrationCredentialRepository.class),
                                cipher(),
                                audit(),
                                registry());

                assertThatThrownBy(
                                () -> service.connect(
                                                admin(orgId),
                                                new ConnectIntegrationRequest(
                                                                "email",
                                                                WEBHOOK_URL)))
                                .isInstanceOf(ApiException.class)
                                .extracting(
                                                ex -> ((ApiException) ex).code())
                                .isEqualTo(
                                                ErrorCode.VALIDATION_ERROR);
        }

        @Test
        void getUnknownOrCrossTenantIntegrationIsNotFound() {
                UUID orgId = UUID.randomUUID();

                IntegrationRepository integrations = mock(IntegrationRepository.class);

                when(
                                integrations.findByIdAndOrganizationId(
                                                any(),
                                                any()))
                                .thenReturn(Optional.empty());

                IntegrationService service = new IntegrationService(
                                integrations,
                                mock(IntegrationCredentialRepository.class),
                                cipher(),
                                audit(),
                                registry());

                assertThatThrownBy(
                                () -> service.get(
                                                admin(orgId),
                                                UUID.randomUUID()))
                                .isInstanceOf(ApiException.class)
                                .extracting(
                                                ex -> ((ApiException) ex).code())
                                .isEqualTo(
                                                ErrorCode.INTEGRATION_NOT_FOUND);
        }

        @Test
        void disconnectFlipsStatusAndDestroysTheStoredSecret() {
                UUID orgId = UUID.randomUUID();

                Integration integration = Integration.createConnected(
                                orgId,
                                UUID.randomUUID(),
                                Integration.TYPE_SLACK,
                                "Slack",
                                "6789");

                IntegrationRepository integrations = mock(IntegrationRepository.class);

                IntegrationCredentialRepository credentials = mock(IntegrationCredentialRepository.class);

                when(
                                integrations.findByIdAndOrganizationId(
                                                integration.getId(),
                                                orgId))
                                .thenReturn(Optional.of(integration));

                IntegrationService service = new IntegrationService(
                                integrations,
                                credentials,
                                cipher(),
                                audit(),
                                registry());

                service.disconnect(
                                admin(orgId),
                                integration.getId());

                assertThat(integration.getStatus())
                                .isEqualTo(
                                                Integration.STATUS_DISCONNECTED);

                assertThat(integration.getHint())
                                .isNull();

                verify(credentials)
                                .deleteByIntegrationId(
                                                integration.getId());
        }
}