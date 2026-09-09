import io.netty.buffer.ByteBufAllocator;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import org.openrs2.cache.Cache;
import org.openrs2.cache.Store;
import org.rsmod.api.cache.types.npc.NpcTypeDecoder;
import org.rsmod.game.type.npc.NpcTypeList;
import org.rsmod.game.type.npc.UnpackedNpcType;

/**
 * Writes every npc type in the game cache as `id, name, wanderRange, defaultMode, moveRestrict,
 * ops` so `generate.py` can bridge void's npc names onto this revision.
 *
 * Run against the installed server jars rather than through Gradle, so it works while the build
 * is busy and never touches the running server. See README.md.
 */
public class DumpNpcTypes {
    public static void main(String[] args) throws Exception {
        try (Store store = Store.open(Path.of(args[0]), ByteBufAllocator.DEFAULT);
                Cache cache = Cache.open(store, ByteBufAllocator.DEFAULT);
                PrintWriter out = new PrintWriter(args[1])) {
            NpcTypeList types = NpcTypeDecoder.INSTANCE.decodeAll(cache);
            for (Map.Entry<Integer, UnpackedNpcType> entry : types.entrySet()) {
                UnpackedNpcType type = entry.getValue();
                String ops =
                        String.join(
                                "|",
                                Arrays.stream(type.getOp())
                                        .map(op -> op == null ? "" : op)
                                        .toArray(String[]::new));
                out.println(
                        entry.getKey()
                                + "\t" + type.getName()
                                + "\t" + type.getWanderRange()
                                + "\t" + type.getDefaultMode()
                                + "\t" + type.getMoveRestrict()
                                + "\t" + ops);
            }
        }
    }
}
