package com.flowops.common.security;

import com.flowops.common.error.ApiException;
import com.flowops.common.error.ErrorCode;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Locale;

/**
 * Destination policy for every server-side HTTP call a workflow can cause: the
 * {@code http_request} node, the {@code outbound_webhook} node, and webhook-style
 * delivery. Blocks loopback, private/link-local ranges, unique-local IPv6, the
 * cloud-metadata range, and internal-looking hostnames before a socket is opened.
 *
 * <p>The check resolves every address the host maps to — a name that resolves to
 * any blocked address is rejected, not just the first. IP-literal URLs are checked
 * without touching DNS. Known limitation (documented, not hidden): the connection
 * re-resolves DNS at connect time, so a canonical defense would also pin the
 * validated address; a private-network allowlist for self-hosted callers is the
 * natural follow-up and deliberately does not exist yet.
 *
 * <p>Failures are {@link ApiException} with {@link ErrorCode#INTEGRATION_INVALID}
 * and never echo the rejected URL — the query string may carry secrets.
 */
public final class SsrfGuard {

    private SsrfGuard() {
    }

    /** Parses and validates {@code url}; returns the parsed URI on success. */
    public static URI validate(String url) {
        if (url == null || url.isBlank()) {
            throw new ApiException(ErrorCode.INTEGRATION_INVALID, "An HTTP target URL is required.");
        }

        URI uri;
        try {
            uri = new URI(url.trim());
        } catch (Exception malformed) {
            throw new ApiException(ErrorCode.INTEGRATION_INVALID, "The HTTP target URL is not a valid URI.");
        }

        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!scheme.equals("http") && !scheme.equals("https")) {
            throw new ApiException(ErrorCode.INTEGRATION_INVALID, "Only http and https targets are allowed.");
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new ApiException(ErrorCode.INTEGRATION_INVALID, "The HTTP target URL has no host.");
        }

        String lower = host.toLowerCase(Locale.ROOT);
        if (lower.equals("localhost") || lower.endsWith(".localhost")
                || lower.endsWith(".local") || lower.endsWith(".internal")) {
            throw new ApiException(ErrorCode.INTEGRATION_INVALID,
                    "Internal hostnames are not allowed as HTTP targets.");
        }

        try {
            for (InetAddress address : InetAddress.getAllByName(host)) {
                if (isBlocked(address)) {
                    throw new ApiException(ErrorCode.INTEGRATION_INVALID,
                            "The target host resolves to a forbidden (private or local) address.");
                }
            }
        } catch (UnknownHostException unresolvable) {
            throw new ApiException(ErrorCode.INTEGRATION_INVALID, "The target host could not be resolved.");
        }

        return uri;
    }

    static boolean isBlocked(InetAddress address) {
        if (address.isAnyLocalAddress() || address.isLoopbackAddress()
                || address.isLinkLocalAddress() || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            return true;
        }

        byte[] octets = address.getAddress();
        if (octets.length == 4) {
            int first = octets[0] & 0xFF;
            int second = octets[1] & 0xFF;
            return first == 0                          // "this network"
                    || first == 10                     // 10.0.0.0/8
                    || (first == 172 && second >= 16 && second <= 31)   // 172.16.0.0/12
                    || (first == 192 && second == 168)                  // 192.168.0.0/16
                    || (first == 169 && second == 254);                 // link-local / cloud metadata
        }
        if (octets.length == 16) {
            int first = octets[0] & 0xFF;
            // fc00::/7 unique-local; the reserved isSiteLocalAddress() covers fec0::/10
            // for IPv4 only, so ULA is checked by hand.
            return (first & 0xFE) == 0xFC;
        }
        return false;
    }
}
