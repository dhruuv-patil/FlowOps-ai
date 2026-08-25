package com.flowops.execution;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * The live fan-out hub for execution monitoring. A browser opens an SSE stream for
 * one execution; the engine (running on a worker thread) pushes {@code node},
 * {@code log}, and {@code execution} events as state changes, and a final
 * {@code done} event when the run reaches a terminal or waiting state.
 *
 * <p>Emitters are keyed by execution id and are safe to touch from multiple
 * threads. A dead or slow client simply drops off — its emitter is removed on the
 * first failed send and never blocks the engine.
 */
@Component
public class ExecutionEvents {

    /** Event names on the SSE stream — mirrored by the frontend stream reader. */
    public static final String NODE = "node";
    public static final String LOG = "log";
    public static final String EXECUTION = "execution";
    public static final String DONE = "done";

    private final Map<java.util.UUID, CopyOnWriteArrayList<SseEmitter>> streams = new ConcurrentHashMap<>();

    public SseEmitter subscribe(java.util.UUID executionId, long timeoutMillis) {
        SseEmitter emitter = new SseEmitter(timeoutMillis);
        CopyOnWriteArrayList<SseEmitter> list =
                streams.computeIfAbsent(executionId, id -> new CopyOnWriteArrayList<>());
        list.add(emitter);

        emitter.onCompletion(() -> remove(executionId, emitter));
        emitter.onTimeout(() -> {
            emitter.complete();
            remove(executionId, emitter);
        });
        emitter.onError(error -> remove(executionId, emitter));
        return emitter;
    }

    public void emit(java.util.UUID executionId, String event, Object data) {
        List<SseEmitter> list = streams.get(executionId);
        if (list == null) {
            return;
        }
        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event().name(event).data(data));
            } catch (IOException | IllegalStateException disconnected) {
                remove(executionId, emitter);
            }
        }
    }

    /** Signals the run is done and closes every open stream for it. */
    public void complete(java.util.UUID executionId) {
        List<SseEmitter> list = streams.remove(executionId);
        if (list == null) {
            return;
        }
        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event().name(DONE).data("{}"));
                emitter.complete();
            } catch (IOException | IllegalStateException ignored) {
                // client already gone; nothing to close
            }
        }
    }

    private void remove(java.util.UUID executionId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> list = streams.get(executionId);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) {
                streams.remove(executionId, list);
            }
        }
    }
}
