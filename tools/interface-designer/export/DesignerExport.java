import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.imageio.ImageIO;
import org.openrs2.cache.Cache;
import org.openrs2.cache.Js5Index;
import org.openrs2.cache.Store;

/**
 * Exports the art the interface designer draws with, straight from the game cache:
 *
 * <ul>
 *   <li>{@code sprites/<id>.png} - frame 0 of every sprite group in js5 archive 8, placed on its
 *       full canvas exactly as {@code SpriteDump.decode} does, plus {@code sprites.json} listing
 *       {@code [id, width, height, frames]} for the sprite picker.
 *   <li>{@code fonts/<id>.png} - a 16x16 grid of the font's 256 glyphs as white masks, plus
 *       {@code fonts.json} with each font's name, cell size, ascent and the 256 advances from js5
 *       archive 13, read the same way {@code PanelMockup.metrics} reads them.
 * </ul>
 *
 * <p>Usage: DesignerExport &lt;cacheDir&gt; &lt;font.sym&gt; &lt;outDir&gt;
 */
public class DesignerExport {
    public static void main(String[] args) throws Exception {
        Path cacheDir = Path.of(args[0]);
        List<String> fontSym = Files.readAllLines(Path.of(args[1]));
        File out = new File(args[2]);
        File sprites = new File(out, "sprites");
        File fonts = new File(out, "fonts");
        sprites.mkdirs();
        fonts.mkdirs();

        try (Store store = Store.open(cacheDir, ByteBufAllocator.DEFAULT);
                Cache cache = Cache.open(store, ByteBufAllocator.DEFAULT)) {
            StringBuilder index = new StringBuilder("{\"sprites\":[\n");
            int count = 0;
            for (Iterator<Js5Index.Group<?>> it = cache.list(8); it.hasNext(); ) {
                int id = it.next().getId();
                List<SpriteDump.Frame> frames = SpriteDump.decode(cache, id);
                if (frames == null || frames.isEmpty()) continue;
                SpriteDump.Frame first = frames.get(0);
                ImageIO.write(image(first), "png", new File(sprites, id + ".png"));
                if (count++ > 0) index.append(",\n");
                index.append('[').append(id).append(',').append(first.w()).append(',')
                        .append(first.h()).append(',').append(frames.size()).append(']');
            }
            index.append("\n]}\n");
            Files.writeString(new File(out, "sprites.json").toPath(), index.toString());
            System.out.println("sprites: " + count);

            StringBuilder fontIndex = new StringBuilder("{\"fonts\":[\n");
            int fontCount = 0;
            for (String line : fontSym) {
                String[] parts = line.split("\t");
                if (parts.length != 2) continue;
                int id = Integer.parseInt(parts[0].trim());
                List<SpriteDump.Frame> glyphs = SpriteDump.decode(cache, id);
                if (glyphs == null || glyphs.size() < 256) {
                    System.out.println("skipping font " + id + ": no 256-glyph sprite group");
                    continue;
                }
                byte[] metrics = read(cache, 13, id);
                int cellW = glyphs.get(0).w(), cellH = glyphs.get(0).h();
                BufferedImage atlas = new BufferedImage(cellW * 16, cellH * 16, BufferedImage.TYPE_INT_ARGB);
                for (int ch = 0; ch < 256; ch++) {
                    SpriteDump.Frame g = glyphs.get(ch);
                    int ox = (ch % 16) * cellW, oy = (ch / 16) * cellH;
                    for (int y = 0; y < g.h(); y++)
                        for (int x = 0; x < g.w(); x++)
                            // The client draws every opaque glyph pixel in the text colour.
                            if ((g.argb()[y * g.w() + x] >>> 24) != 0) atlas.setRGB(ox + x, oy + y, 0xFFFFFFFF);
                }
                ImageIO.write(atlas, "png", new File(fonts, id + ".png"));
                if (fontCount++ > 0) fontIndex.append(",\n");
                fontIndex.append("{\"id\":").append(id)
                        .append(",\"name\":\"").append(parts[1].trim()).append('"')
                        .append(",\"cellW\":").append(cellW)
                        .append(",\"cellH\":").append(cellH)
                        .append(",\"ascent\":").append(metrics[256] & 0xFF)
                        .append(",\"advances\":[");
                for (int ch = 0; ch < 256; ch++) fontIndex.append(ch == 0 ? "" : ",").append(metrics[ch] & 0xFF);
                fontIndex.append("]}");
            }
            fontIndex.append("\n]}\n");
            Files.writeString(new File(out, "fonts.json").toPath(), fontIndex.toString());
            System.out.println("fonts: " + fontCount);
        }
    }

    static BufferedImage image(SpriteDump.Frame f) {
        BufferedImage img = new BufferedImage(Math.max(1, f.w()), Math.max(1, f.h()), BufferedImage.TYPE_INT_ARGB);
        img.setRGB(0, 0, f.w(), f.h(), f.argb(), 0, f.w());
        return img;
    }

    static byte[] read(Cache cache, int archive, int group) {
        ByteBuf buf = cache.read(archive, group, 0);
        try {
            byte[] data = new byte[buf.readableBytes()];
            buf.readBytes(data);
            return data;
        } finally {
            buf.release();
        }
    }
}
