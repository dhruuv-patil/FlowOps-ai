package com.flowops.api;

import java.util.List;

/** Object envelope for the template gallery (never a bare array). */
public final class TemplateEnvelopes {

    private TemplateEnvelopes() {
    }

    public record Templates(List<TemplateSummaryResponse> templates) {
    }
}
