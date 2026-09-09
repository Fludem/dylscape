import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import java.io.PrintWriter;
import java.nio.file.Path;
import org.openrs2.cache.Cache;
import org.openrs2.cache.Store;

/**
 * Writes `x z level settings` for every tile whose settings byte is non-zero.
 *
 * `m{x}_{z}` is not XTEA-encrypted. Bit 1 of settings is "blocked terrain"; bit 2 is the
 * link-below flag that makes a bridge tile render one level down.
 */
public class DumpMapTiles {
    public static void main(String[] args) throws Exception {
        try (Store store = Store.open(Path.of(args[0]), ByteBufAllocator.DEFAULT);
                Cache cache = Cache.open(store, ByteBufAllocator.DEFAULT);
                PrintWriter out = new PrintWriter(args[1])) {
            int squares = 0;
            java.util.Map<Integer, Integer> trailing = new java.util.TreeMap<>();
            for (int sx = 0; sx < 128; sx++) {
                for (int sz = 0; sz < 256; sz++) {
                    if (!cache.exists(5, "m" + sx + "_" + sz)) continue;
                    ByteBuf buf = cache.read(5, "m" + sx + "_" + sz, 0);
                    try {
                        for (int level = 0; level < 4; level++) {
                            for (int x = 0; x < 64; x++) {
                                for (int z = 0; z < 64; z++) {
                                    int settings = 0;
                                    while (buf.isReadable()) {
                                        int op = buf.readUnsignedShort();
                                        if (op == 0) break;
                                        if (op == 1) {
                                            buf.readUnsignedByte();
                                            break;
                                        }
                                        if (op <= 49) buf.readShort();
                                        else if (op <= 81) settings = (byte) (op - 49);
                                    }
                                    if (settings != 0) {
                                        out.println(
                                                (sx * 64 + x)
                                                        + "\t" + (sz * 64 + z)
                                                        + "\t" + level
                                                        + "\t" + settings);
                                    }
                                }
                            }
                        }
                        trailing.merge(buf.readableBytes(), 1, Integer::sum);
                        squares++;
                    } finally {
                        buf.release();
                    }
                }
            }
            System.out.println("squares=" + squares + " trailingBytes=" + trailing);
        }
    }
}
