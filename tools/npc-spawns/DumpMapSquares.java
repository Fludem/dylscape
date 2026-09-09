import io.netty.buffer.ByteBufAllocator;
import java.io.PrintWriter;
import java.nio.file.Path;
import org.openrs2.cache.Cache;
import org.openrs2.cache.Store;

/**
 * Writes the `x, z` of every mapsquare the game cache actually has terrain for.
 *
 * Void is a 2011 RuneScape server and walks regions this revision never shipped, so
 * `generate.py` drops any spawn whose mapsquare is missing from this list.
 */
public class DumpMapSquares {
    private static final int MAPS_ARCHIVE = 5;

    public static void main(String[] args) throws Exception {
        try (Store store = Store.open(Path.of(args[0]), ByteBufAllocator.DEFAULT);
                Cache cache = Cache.open(store, ByteBufAllocator.DEFAULT);
                PrintWriter out = new PrintWriter(args[1])) {
            for (int x = 0; x < 128; x++) {
                for (int z = 0; z < 256; z++) {
                    if (cache.exists(MAPS_ARCHIVE, "m" + x + "_" + z)) {
                        out.println(x + "\t" + z);
                    }
                }
            }
        }
    }
}
