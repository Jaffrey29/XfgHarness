package cn.xiaofuge.deepseek.harness.infrastructure.adapter.sdk;

import cn.xiaofuge.deepseek.harness.domain.sdk.adapter.port.ISdkTransportPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Newline-delimited JSON-RPC 2.0 transport over stdin/stdout.
 *
 * <p>Frames with id and method are requests, id alone is a response, and
 * method alone is a notification. Malformed lines are ignored.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public class StdioSdkTransportPort implements ISdkTransportPort {

    private final ObjectMapper mapper;
    private final BufferedReader reader;
    private final BufferedWriter writer;
    private final Map<String, CompletableFuture<Object>> pending = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong(1);
    private final List<Consumer<NotificationFrame>> notificationHandlers = new CopyOnWriteArrayList<>();
    private final ExecutorService readLoop = Executors.newSingleThreadExecutor();
    private volatile boolean closed = false;

    public StdioSdkTransportPort(InputStream in, OutputStream out, ObjectMapper mapper) {
        this.mapper = mapper;
        this.reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        this.writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
        readLoop.submit(this::readLoop);
    }

    @Override
    public Object request(String method, Object params) {
        String id = String.valueOf(nextId.getAndIncrement());
        CompletableFuture<Object> future = new CompletableFuture<>();
        pending.put(id, future);

        Map<String, Object> frame = new LinkedHashMap<>();
        frame.put("jsonrpc", "2.0");
        frame.put("id", id);
        frame.put("method", method);
        if (params != null) frame.put("params", params);

        try {
            writeFrame(frame);
        } catch (Exception e) {
            pending.remove(id);
            throw new RuntimeException("failed to send request: " + e.getMessage(), e);
        }

        try {
            return future.get(60, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            pending.remove(id);
            throw new RuntimeException("request timed out: " + method, e);
        } catch (Exception e) {
            throw new RuntimeException("request failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void notify(String method, Object params) {
        Map<String, Object> frame = new LinkedHashMap<>();
        frame.put("jsonrpc", "2.0");
        frame.put("method", method);
        if (params != null) frame.put("params", params);
        try {
            writeFrame(frame);
        } catch (Exception e) {
            throw new RuntimeException("failed to send notification: " + e.getMessage(), e);
        }
    }

    @Override
    public void onNotification(Consumer<NotificationFrame> handler) {
        notificationHandlers.add(handler);
    }

    @Override
    public void close() {
        closed = true;
        readLoop.shutdownNow();
        failPending(new IOException("transport closed"));
    }

    private void readLoop() {
        while (!closed) {
            try {
                String line = reader.readLine();
                if (line == null) {
                    failPending(new IOException("JSON-RPC input closed"));
                    break;
                }
                line = line.trim();
                if (line.isEmpty()) continue;
                handleLine(line);
            } catch (IOException e) {
                if (!closed) failPending(e);
                break;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void handleLine(String line) {
        Map<String, Object> frame;
        try {
            frame = mapper.readValue(line, Map.class);
        } catch (Exception e) {
            return;
        }

        Object id = frame.get("id");
        String method = (String) frame.get("method");

        if (id != null && method != null) {
            // Incoming request — server side, not handled here
            return;
        }
        if (id != null) {
            // Response to our request
            CompletableFuture<Object> future = pending.remove(String.valueOf(id));
            if (future == null) return;
            if (frame.containsKey("error")) {
                future.completeExceptionally(
                        new RuntimeException("JSON-RPC error: " + frame.get("error")));
            } else {
                future.complete(frame.get("result"));
            }
            return;
        }
        if (method != null) {
            // Notification
            NotificationFrame nf = new NotificationFrame(method, frame.get("params"));
            for (Consumer<NotificationFrame> handler : notificationHandlers) {
                try { handler.accept(nf); } catch (Exception ignored) {}
            }
        }
    }

    private void writeFrame(Map<String, Object> frame) throws IOException {
        synchronized (writer) {
            writer.write(mapper.writeValueAsString(frame));
            writer.write("\n");
            writer.flush();
        }
    }

    private void failPending(Exception e) {
        for (CompletableFuture<Object> f : pending.values()) {
            f.completeExceptionally(e);
        }
        pending.clear();
    }
}
