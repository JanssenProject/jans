package io.jans.configapi.plugin.facade.convert;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.jans.configapi.plugin.facade.convert.FacadeRows.Action;
import io.jans.configapi.plugin.facade.convert.FacadeRows.Context;
import io.jans.configapi.plugin.facade.convert.FacadeRows.Peer;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.zip.CRC32C;

public final class FacadeFiles {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
            .withZone(ZoneOffset.UTC);
    private static final int MASK_DELTA = 0xa282ead8;

    private FacadeFiles() {
    }

    public static String iso(long epochSeconds) {
        return ISO.format(Instant.ofEpochSecond(epochSeconds));
    }

    public static void writeActionsJsonl(Path file, List<Action> actions) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file)) {
            for (Action action : actions) {
                ObjectNode row = MAPPER.createObjectNode();
                row.put("type", action.type());
                row.put("id", action.id());
                row.put("resource_id", action.resourceId());
                row.put("principal", action.principal());
                row.put("occurred_at", iso(action.occurredAt()));
                row.put("history_key", action.historyKey());
                writer.write(MAPPER.writeValueAsString(row));
                writer.newLine();
            }
        }
    }

    public static void writeContextsJsonl(Path file, List<Context> contexts) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file)) {
            for (Context context : contexts) {
                ObjectNode row = MAPPER.createObjectNode();
                row.put("type", context.type());
                row.put("principal", context.principal());
                row.put("valid_from", iso(context.validFrom()));
                ArrayNode peers = row.putArray("peer_attributes");
                for (Peer peer : context.peers()) {
                    peers.addObject().put("name", peer.name()).put("value", peer.value())
                            .put("direction", peer.direction()).put("weight", peer.weight());
                }
                writer.write(MAPPER.writeValueAsString(row));
                writer.newLine();
            }
        }
    }

    public static void writeActionTfRecord(Path file, List<Action> actions) throws IOException {
        try (OutputStream out = Files.newOutputStream(file)) {
            for (Action action : actions) {
                ProtoWire message = new ProtoWire()
                        .string(1, action.type())
                        .bytes(2, action.id().getBytes(StandardCharsets.UTF_8))
                        .string(3, action.resourceId())
                        .string(4, action.principal())
                        .message(5, timestamp(action.occurredAt()))
                        .bytes(6, action.historyKey().getBytes(StandardCharsets.UTF_8));
                record(out, message.toByteArray());
            }
        }
    }

    public static void writeContextTfRecord(Path file, List<Context> contexts) throws IOException {
        try (OutputStream out = Files.newOutputStream(file)) {
            for (Context context : contexts) {
                ProtoWire message = new ProtoWire()
                        .string(1, context.type())
                        .string(2, context.principal())
                        .message(3, timestamp(context.validFrom()));
                for (Peer peer : context.peers()) {
                    message.message(4, new ProtoWire()
                            .string(1, peer.name())
                            .bytes(2, peer.value().getBytes(StandardCharsets.UTF_8))
                            .enumValue(3, direction(peer.direction()))
                            .floatValue(4, (float) peer.weight()));
                }
                record(out, message.toByteArray());
            }
        }
    }

    private static ProtoWire timestamp(long seconds) {
        return new ProtoWire().int64(1, seconds);
    }

    private static int direction(String name) {
        return switch (name) {
            case "FORWARD" -> 1;
            case "BACKWARD" -> 2;
            default -> 0;
        };
    }

    private static void record(OutputStream out, byte[] data) throws IOException {
        byte[] length = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(data.length).array();
        out.write(length);
        out.write(maskedCrc(length));
        out.write(data);
        out.write(maskedCrc(data));
    }

    private static byte[] maskedCrc(byte[] data) {
        CRC32C crc = new CRC32C();
        crc.update(data, 0, data.length);
        int value = (int) crc.getValue();
        int masked = ((value >>> 15) | (value << 17)) + MASK_DELTA;
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(masked).array();
    }
}
