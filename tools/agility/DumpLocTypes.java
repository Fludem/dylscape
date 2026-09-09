import io.netty.buffer.ByteBufAllocator;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import org.openrs2.cache.Cache;
import org.openrs2.cache.Store;
import org.rsmod.api.cache.types.loc.LocTypeDecoder;
import org.rsmod.game.type.loc.LocTypeList;
import org.rsmod.game.type.loc.UnpackedLocType;

/** Writes every loc type as `id, name, width, length, blockWalk, multiLoc, ops`. */
public class DumpLocTypes {
    public static void main(String[] args) throws Exception {
        try (Store store = Store.open(Path.of(args[0]), ByteBufAllocator.DEFAULT);
                Cache cache = Cache.open(store, ByteBufAllocator.DEFAULT);
                PrintWriter out = new PrintWriter(args[1])) {
            LocTypeList types = LocTypeDecoder.INSTANCE.decodeAll(cache);
            for (Map.Entry<Integer, UnpackedLocType> entry : types.entrySet()) {
                UnpackedLocType t = entry.getValue();
                String ops =
                        String.join(
                                "|",
                                Arrays.stream(t.getOp())
                                        .map(op -> op == null ? "" : op)
                                        .toArray(String[]::new));
                String multi = Arrays.toString(t.getMultiLoc());
                out.println(
                        entry.getKey()
                                + "\t" + t.getName()
                                + "\t" + t.getWidth()
                                + "\t" + t.getLength()
                                + "\t" + t.getBlockWalk()
                                + "\t" + t.getMultiVarBit()
                                + "\t" + multi
                                + "\t" + ops);
            }
        }
    }
}
