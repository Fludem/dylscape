import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openrs2.cache.Cache;
import org.openrs2.cache.Store;
import org.openrs2.crypto.SymmetricKey;

/**
 * Dumps every loc placement in the game cache as `id  x  z  level  shape  angle`.
 *
 * Reimplements the `l{x}_{z}` shortSmart walk by hand: MapLocListDecoder's parameters are Kotlin
 * value classes and are erased, so they are unreachable from Java.
 *
 * args: <cache dir> <xteas.json> <out file>
 */
public class DumpLocPlacements {
    private static final int MAPS_ARCHIVE = 5;

    public static void main(String[] args) throws Exception {
        String xteaJson = Files.readString(Path.of(args[1]));
        Matcher m =
                Pattern.compile(
                                "\"name\"\\s*:\\s*\"l(\\d+)_(\\d+)\".*?\"key\"\\s*:\\s*\\["
                                        + "\\s*(-?\\d+),\\s*(-?\\d+),\\s*(-?\\d+),\\s*(-?\\d+)\\s*\\]",
                                Pattern.DOTALL)
                        .matcher(xteaJson);

        int squares = 0;
        int failed = 0;
        long rows = 0;
        try (Store store = Store.open(Path.of(args[0]), ByteBufAllocator.DEFAULT);
                Cache cache = Cache.open(store, ByteBufAllocator.DEFAULT);
                PrintWriter out = new PrintWriter(args[2])) {
            while (m.find()) {
                int sqX = Integer.parseInt(m.group(1));
                int sqZ = Integer.parseInt(m.group(2));
                SymmetricKey key =
                        SymmetricKey.fromIntArray(
                                new int[] {
                                    Integer.parseInt(m.group(3)),
                                    Integer.parseInt(m.group(4)),
                                    Integer.parseInt(m.group(5)),
                                    Integer.parseInt(m.group(6))
                                });
                List<String> buffered = new ArrayList<>();
                try {
                    ByteBuf buf = cache.read(MAPS_ARCHIVE, "l" + sqX + "_" + sqZ, 0, key);
                    try {
                        walk(buf, sqX, sqZ, buffered);
                        // Zero trailing bytes is what proves the walk is correct.
                        if (buf.isReadable()) {
                            throw new IllegalStateException(
                                    buf.readableBytes() + " trailing bytes");
                        }
                    } finally {
                        buf.release();
                    }
                } catch (Exception e) {
                    failed++;
                    continue;
                }
                squares++;
                for (String line : buffered) {
                    out.println(line);
                    rows++;
                }
            }
        }
        System.out.println("squares=" + squares + " failed=" + failed + " rows=" + rows);
    }

    private static void walk(ByteBuf buf, int sqX, int sqZ, List<String> out) {
        int id = -1;
        while (true) {
            int idDelta = shortSmart(buf);
            if (idDelta == 0) {
                return;
            }
            id += idDelta;
            int pos = 0;
            while (true) {
                int posDelta = shortSmart(buf);
                if (posDelta == 0) {
                    break;
                }
                pos += posDelta - 1;
                int localZ = pos & 0x3F;
                int localX = (pos >> 6) & 0x3F;
                int level = (pos >> 12) & 0x3;
                int attr = buf.readUnsignedByte();
                int shape = attr >> 2;
                int angle = attr & 0x3;
                out.add(
                        id
                                + "\t"
                                + (sqX * 64 + localX)
                                + "\t"
                                + (sqZ * 64 + localZ)
                                + "\t"
                                + level
                                + "\t"
                                + shape
                                + "\t"
                                + angle);
            }
        }
    }

    private static int shortSmart(ByteBuf buf) {
        int peek = buf.getUnsignedByte(buf.readerIndex());
        return peek < 128 ? buf.readUnsignedByte() : buf.readUnsignedShort() - 32768;
    }
}
