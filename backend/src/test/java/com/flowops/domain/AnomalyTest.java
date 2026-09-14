package com.flowops.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Pins the Anomaly record's lifecycle semantics that the recorder relies on: repeats
 * within a dedup window aggregate (affected_executions bumps, severity only ever
 * escalates, ACKNOWLEDGED reopens to OPEN), and a post-cooldown repeat starts a fresh
 * episode on the same record instead of a new row.
 */
class AnomalyTest {

    private static final UUID ORG = UUID.randomUUID();
    private static final UUID WF = UUID.randomUUID();
    private static final UUID EXEC = UUID.randomUUID();

    private Anomaly open() {
        return Anomaly.open(ORG, WF, "n1", EXEC, AnomalyType.LATENCY,
                AnomalySeverity.MEDIUM, "LATENCY", "p95 ≈ 1s", "8s", 4.0, 0.9,
                Map.of("kind", "latency-spike"), "LATENCY:n1");
    }

    @Test
    void openStartsAtOneAffectedAndOpen() {
        Anomaly a = open();
        assertThat(a.getStatus()).isEqualTo(AnomalyStatus.OPEN);
        assertThat(a.getAffectedExecutions()).isEqualTo(1);
        assertThat(a.getSeverity()).isEqualTo(AnomalySeverity.MEDIUM);
    }

    @Test
    void aggregateBumpsAffectedAndNeverLowersSeverity() {
        Anomaly a = open();
        a.aggregate(AnomalySeverity.HIGH, "9s", 6.0, 0.95, EXEC, Map.of("kind", "latency-spike"));
        assertThat(a.getAffectedExecutions()).isEqualTo(2);
        assertThat(a.getSeverity()).isEqualTo(AnomalySeverity.HIGH); // escalated
        a.aggregate(AnomalySeverity.LOW, "9s", 6.0, 0.95, EXEC, Map.of("kind", "latency-spike"));
        assertThat(a.getSeverity()).isEqualTo(AnomalySeverity.HIGH); // never downgraded
        assertThat(a.getAffectedExecutions()).isEqualTo(3);
    }

    @Test
    void acknowledgedReopensOnNewEvidence() {
        Anomaly a = open();
        a.acknowledge();
        assertThat(a.getStatus()).isEqualTo(AnomalyStatus.ACKNOWLEDGED);
        a.aggregate(AnomalySeverity.HIGH, "9s", 6.0, 0.95, EXEC, Map.of("kind", "latency-spike"));
        assertThat(a.getStatus()).isEqualTo(AnomalyStatus.OPEN);
    }

    /**
     * NOTE: the "resolved/false-positive → open a fresh row" policy lives in the recorder
     * (AnomalyRecorder.recordOne), not the entity — the entity's aggregate() is only ever
     * invoked on OPEN/ACKNOWLEDGED records. That gate is covered by a Spring/Mockito test.
     */
    @Test
    void newEpisodeResetsEpisodeAndReopens() {
        Anomaly a = open();
        a.aggregate(AnomalySeverity.HIGH, "9s", 6.0, 0.95, EXEC, Map.of("kind", "latency-spike"));
        assertThat(a.getAffectedExecutions()).isEqualTo(2);
        a.newEpisode(AnomalySeverity.CRITICAL, "12s", 9.0, 1.0, EXEC, Map.of("kind", "latency-spike"));
        assertThat(a.getAffectedExecutions()).isEqualTo(1);          // fresh episode
        assertThat(a.getStatus()).isEqualTo(AnomalyStatus.OPEN);
        assertThat(a.getSeverity()).isEqualTo(AnomalySeverity.CRITICAL);
    }

    @Test
    void lifecycleTransitions() {
        Anomaly a = open();
        a.acknowledge();
        assertThat(a.getStatus()).isEqualTo(AnomalyStatus.ACKNOWLEDGED);
        a.markFalsePositive();
        assertThat(a.getStatus()).isEqualTo(AnomalyStatus.FALSE_POSITIVE);
        Anomaly b = open();
        b.resolve();
        assertThat(b.getStatus()).isEqualTo(AnomalyStatus.RESOLVED);
    }
}