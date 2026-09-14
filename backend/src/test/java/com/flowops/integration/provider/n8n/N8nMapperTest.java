package com.flowops.integration.provider.n8n;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowops.integration.provider.ExternalExecution;
import com.flowops.integration.provider.ExternalNodeExecution;
import com.flowops.integration.provider.ExternalWorkflow;
import com.flowops.integration.provider.ExecutionStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Unit tests for n8n mappers. Pure JSON → normalized records, no HTTP. */
class N8nMapperTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void mapsWorkflowList() throws Exception {
        String json = """
                {
                  "data": [
                    { "id": "wf-1", "name": "My Workflow", "active": true, "updatedAt": "2024-01-15T10:30:00.000Z" },
                    { "id": "wf-2", "name": "Inactive", "active": false, "updatedAt": "2024-01-10T08:00:00.000Z" }
                  ]
                }
                """;

        JsonNode node = MAPPER.readTree(json);
        List<ExternalWorkflow> workflows = N8nWorkflowMapper.toExternal(node);

        assertThat(workflows).hasSize(2);
        assertThat(workflows.get(0).externalId()).isEqualTo("wf-1");
        assertThat(workflows.get(0).name()).isEqualTo("My Workflow");
        assertThat(workflows.get(0).status()).isEqualTo("active");
        assertThat(workflows.get(1).status()).isEqualTo("inactive");
    }

    @Test
    void mapsExecutionList() throws Exception {
        String json = """
                {
                  "data": [
                    { "id": "exec-1", "workflowId": "wf-1", "status": "success", "startedAt": "2024-01-15T10:00:00.000Z", "stoppedAt": "2024-01-15T10:01:30.000Z", "mode": "manual" },
                    { "id": "exec-2", "workflowId": "wf-1", "status": "error", "startedAt": "2024-01-15T11:00:00.000Z", "stoppedAt": "2024-01-15T11:00:05.000Z", "mode": "manual" }
                  ]
                }
                """;

        JsonNode node = MAPPER.readTree(json);
        List<ExternalExecution> executions = N8nExecutionMapper.toExternal(node);

        assertThat(executions).hasSize(2);
        assertThat(executions.get(0).externalId()).isEqualTo("exec-1");
        assertThat(executions.get(0).workflowExternalId()).isEqualTo("wf-1");
        assertThat(executions.get(0).status()).isEqualTo(ExecutionStatus.SUCCEEDED);
        assertThat(executions.get(1).status()).isEqualTo(ExecutionStatus.FAILED);
    }

    @Test
    void mapsExecutionStatuses() throws Exception {
        String[] successStatuses = {"success"};
        String[] failedStatuses = {"error"};
        String[] runningStatuses = {"running", "new"};
        String[] waitingStatuses = {"waiting"};
        String[] canceledStatuses = {"cancelled"};

        for (String s : successStatuses) {
            JsonNode node = MAPPER.readTree("{\"data\":[{\"id\":\"x\",\"workflowId\":\"w\",\"status\":\"" + s + "\"}]}");
            assertThat(N8nExecutionMapper.toExternal(node).get(0).status()).isEqualTo(ExecutionStatus.SUCCEEDED);
        }
        for (String s : failedStatuses) {
            JsonNode node = MAPPER.readTree("{\"data\":[{\"id\":\"x\",\"workflowId\":\"w\",\"status\":\"" + s + "\"}]}");
            assertThat(N8nExecutionMapper.toExternal(node).get(0).status()).isEqualTo(ExecutionStatus.FAILED);
        }
        for (String s : runningStatuses) {
            JsonNode node = MAPPER.readTree("{\"data\":[{\"id\":\"x\",\"workflowId\":\"w\",\"status\":\"" + s + "\"}]}");
            assertThat(N8nExecutionMapper.toExternal(node).get(0).status()).isEqualTo(ExecutionStatus.RUNNING);
        }
        for (String s : waitingStatuses) {
            JsonNode node = MAPPER.readTree("{\"data\":[{\"id\":\"x\",\"workflowId\":\"w\",\"status\":\"" + s + "\"}]}");
            assertThat(N8nExecutionMapper.toExternal(node).get(0).status()).isEqualTo(ExecutionStatus.WAITING);
        }
        for (String s : canceledStatuses) {
            JsonNode node = MAPPER.readTree("{\"data\":[{\"id\":\"x\",\"workflowId\":\"w\",\"status\":\"" + s + "\"}]}");
            assertThat(N8nExecutionMapper.toExternal(node).get(0).status()).isEqualTo(ExecutionStatus.CANCELED);
        }
        // Unknown defaults to FAILED
        JsonNode unknown = MAPPER.readTree("{\"data\":[{\"id\":\"x\",\"workflowId\":\"w\",\"status\":\"weird\"}]}");
        assertThat(N8nExecutionMapper.toExternal(unknown).get(0).status()).isEqualTo(ExecutionStatus.FAILED);
    }

    @Test
    void mapsNodeExecutions() throws Exception {
        String json = """
                {
                  "data": {
                    "resultData": {
                      "runData": {
                        "HTTP Request": [
                          {
                            "startTime": 1705312800000,
                            "executionTime": 1500,
                            "executionStatus": "success",
                            "error": null,
                            "outputSize": 2048
                          }
                        ],
                        "Transform": [
                          {
                            "startTime": 1705312802000,
                            "executionTime": 50,
                            "executionStatus": "success",
                            "error": null,
                            "outputSize": 1024
                          }
                        ]
                      }
                    },
                    "status": "success"
                  }
                }
                """;

        JsonNode node = MAPPER.readTree(json);
        List<ExternalNodeExecution> nodes = N8nNodeExecutionMapper.toExternal(node, "exec-1", "wf-1");

        assertThat(nodes).hasSize(2);
        assertThat(nodes.get(0).externalId()).isEqualTo("HTTP Request");
        assertThat(nodes.get(0).name()).isEqualTo("HTTP Request");
        assertThat(nodes.get(0).status()).isEqualTo(ExecutionStatus.SUCCEEDED);
        assertThat(nodes.get(0).startedAt()).isEqualTo(Instant.ofEpochMilli(1705312800000L));
        assertThat(nodes.get(0).finishedAt()).isEqualTo(Instant.ofEpochMilli(1705312801500L));
        assertThat(nodes.get(0).outputSize()).isEqualTo(2048L);

        assertThat(nodes.get(1).externalId()).isEqualTo("Transform");
        assertThat(nodes.get(1).outputSize()).isEqualTo(1024L);
    }

    @Test
    void fallsBackToPseudoNodeWhenNoRunData() throws Exception {
        String json = """
                {
                  "data": {
                    "resultData": { "runData": {} },
                    "status": "error"
                  }
                }
                """;

        JsonNode node = MAPPER.readTree(json);
        List<ExternalNodeExecution> nodes = N8nNodeExecutionMapper.toExternal(node, "exec-1", "wf-1");

        assertThat(nodes).hasSize(1);
        assertThat(nodes.get(0).externalId()).isEqualTo("execution");
        assertThat(nodes.get(0).name()).isEqualTo("Execution");
        assertThat(nodes.get(0).status()).isEqualTo(ExecutionStatus.FAILED);
    }

    @Test
    void nodeStatusMapping() throws Exception {
        String[] success = {"success"};
        String[] failed = {"error"};
        String[] waiting = {"waiting"};
        String[] running = {"running"};
        String[] canceled = {"cancelled"};

        for (String s : success) {
            JsonNode run = MAPPER.readTree("{\"executionStatus\":\"" + s + "\",\"startTime\":1000,\"executionTime\":100}");
            ExternalNodeExecution ne = N8nNodeExecutionMapper.toExternal(run, "n", "exec-1", "wf-1");
            assertThat(ne.status()).isEqualTo(ExecutionStatus.SUCCEEDED);
        }
        for (String s : failed) {
            JsonNode run = MAPPER.readTree("{\"executionStatus\":\"" + s + "\",\"startTime\":1000,\"executionTime\":100}");
            ExternalNodeExecution ne = N8nNodeExecutionMapper.toExternal(run, "n", "exec-1", "wf-1");
            assertThat(ne.status()).isEqualTo(ExecutionStatus.FAILED);
        }
        for (String s : waiting) {
            JsonNode run = MAPPER.readTree("{\"executionStatus\":\"" + s + "\",\"startTime\":1000,\"executionTime\":100}");
            ExternalNodeExecution ne = N8nNodeExecutionMapper.toExternal(run, "n", "exec-1", "wf-1");
            assertThat(ne.status()).isEqualTo(ExecutionStatus.WAITING);
        }
        for (String s : running) {
            JsonNode run = MAPPER.readTree("{\"executionStatus\":\"" + s + "\",\"startTime\":1000,\"executionTime\":100}");
            ExternalNodeExecution ne = N8nNodeExecutionMapper.toExternal(run, "n", "exec-1", "wf-1");
            assertThat(ne.status()).isEqualTo(ExecutionStatus.RUNNING);
        }
        for (String s : canceled) {
            JsonNode run = MAPPER.readTree("{\"executionStatus\":\"" + s + "\",\"startTime\":1000,\"executionTime\":100}");
            ExternalNodeExecution ne = N8nNodeExecutionMapper.toExternal(run, "n", "exec-1", "wf-1");
            assertThat(ne.status()).isEqualTo(ExecutionStatus.CANCELED);
        }
        // Unknown defaults to FAILED
        JsonNode run = MAPPER.readTree("{\"executionStatus\":\"weird\",\"startTime\":1000,\"executionTime\":100}");
        ExternalNodeExecution ne = N8nNodeExecutionMapper.toExternal(run, "n", "exec-1", "wf-1");
        assertThat(ne.status()).isEqualTo(ExecutionStatus.FAILED);
    }
}