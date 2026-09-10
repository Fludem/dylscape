import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import org.openrs2.cache.Cache;
import org.openrs2.cache.Store;
import org.rsmod.game.type.comp.UnpackedComponentType;

/**
 * Renders the components authored by a ComponentBuilder the way the OSRS client would lay them
 * out, using real cache sprites and fonts, and emulating [proc,steelborder] for onLoad=227.
 *
 * Usage: PanelMockup <cacheDir> <out.png> <builderClass> <overridesFile> [hoverName...]
 * overrides file lines: "text <name> <text...>" | "show <name>" | "hide <name>"
 */
public class PanelMockup {
    static Cache cache;
    static final Map<Integer, SpriteDump.Frame> SPRITES = new HashMap<>();
    static final Map<Integer, List<SpriteDump.Frame>> FONTS = new HashMap<>();
    static final Map<Integer, byte[]> METRICS = new HashMap<>();
    static int[] px;
    static int W, H;

    public static void main(String[] args) throws Exception {
        Class<?> builderClass = Class.forName(args[2]);
        Object builder = builderClass.getField("INSTANCE").get(null);
        Field f = findField(builderClass, "cache");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<UnpackedComponentType> comps = (List<UnpackedComponentType>) f.get(builder);
        @SuppressWarnings("unchecked")
        List<String> names = (List<String>) builderClass.getMethod("getComponentNames").invoke(builder);

        // Builder emits components in declaration order, which is also child-index order.
        Map<String, Integer> index = new HashMap<>();
        for (int i = 0; i < names.size(); i++) index.put(names.get(i), i);
        Map<Integer, String> textOverride = new HashMap<>();
        Map<Integer, Boolean> hideOverride = new HashMap<>();
        for (String line : java.nio.file.Files.readAllLines(Path.of(args[3]))) {
            if (line.isBlank()) continue;
            String[] p = line.split(" ", 3);
            int idx = index.get(p[1]);
            switch (p[0]) {
                case "text" -> textOverride.put(idx, p.length > 2 ? p[2] : "");
                case "show" -> hideOverride.put(idx, false);
                case "hide" -> hideOverride.put(idx, true);
            }
        }
        java.util.Set<Integer> hovered = new java.util.HashSet<>();
        for (int i = 4; i < args.length; i++) hovered.add(index.get(args[i]));

        try (Store store = Store.open(Path.of(args[0]), ByteBufAllocator.DEFAULT)) {
            cache = Cache.open(store, ByteBufAllocator.DEFAULT);
            W = 512 + 40;
            H = 334 + 40;
            px = new int[W * H];
            // A game-world-ish backdrop so translucency reads correctly.
            for (int i = 0; i < px.length; i++) px[i] = 0x4A5A3A;
            UnpackedComponentType root = comps.get(0);
            int rw = root.getWidth(), rh = root.getHeight();
            int rx = (W - rw) / 2, ry = (H - rh) / 2;
            draw(comps, 0, rx, ry, rw, rh, textOverride, hideOverride, hovered);
            BufferedImage img = new BufferedImage(W * 2, H * 2, BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < H * 2; y++) for (int x = 0; x < W * 2; x++) img.setRGB(x, y, px[(y / 2) * W + x / 2]);
            ImageIO.write(img, "png", new File(args[1]));
            System.out.println("wrote " + args[1]);
        }
    }

    static Field findField(Class<?> c, String name) throws NoSuchFieldException {
        for (Class<?> k = c; k != null; k = k.getSuperclass()) {
            try { return k.getDeclaredField(name); } catch (NoSuchFieldException ignored) {}
        }
        throw new NoSuchFieldException(name);
    }

    static void draw(List<UnpackedComponentType> comps, int idx, int x, int y, int w, int h,
                     Map<Integer, String> texts, Map<Integer, Boolean> hides, java.util.Set<Integer> hovered) {
        UnpackedComponentType c = comps.get(idx);
        boolean hidden = hides.getOrDefault(idx, c.getHide());
        if (hidden) return;
        boolean hov = false;
        for (int hv : hovered) if (isAncestorOrSelf(comps, hv, idx)) hov = true;
        switch (c.getType()) {
            case 0 -> {
                Object[] load = c.getOnLoad();
                if (load != null && load.length > 0 && ((Integer) load[0]) == 227) steelborder(x, y, w, h, (String) load[2]);
            }
            case 3 -> {
                int trans = c.getTrans1();
                if (hov && c.getOnMouseOver() != null) trans = (Integer) c.getOnMouseOver()[2];
                int alpha = 255 - trans;
                if (c.getFill()) fillRect(x, y, w, h, c.getColour1(), alpha);
                else outline(x, y, w, h, c.getColour1(), alpha);
            }
            case 4 -> {
                int colour = c.getColour1();
                if (hov && c.getOnMouseOver() != null) colour = (Integer) c.getOnMouseOver()[2];
                String t = texts.getOrDefault(idx, c.getText());
                text(c.getTextFont(), t, x, y, w, h, colour, c.getTextShadow(), c.getTextAlignH(), c.getTextAlignV());
            }
            case 5 -> sprite(c.getGraphic(), x, y, w, h, c.getTiling());
        }
        // Children, depth-first in child-index order, as the client draws them.
        for (int i = 0; i < comps.size(); i++) {
            UnpackedComponentType k = comps.get(i);
            if (i == idx || k.getLayer() != idx || i == 0) continue;
            int cw = size(k.getWidth(), k.getWidthMode(), w);
            int ch = size(k.getHeight(), k.getHeightMode(), h);
            int cx = x + pos(k.getX(), k.getXMode(), w, cw);
            int cy = y + pos(k.getY(), k.getYMode(), h, ch);
            draw(comps, i, cx, cy, cw, ch, texts, hides, hovered);
        }
    }

    static boolean isAncestorOrSelf(List<UnpackedComponentType> comps, int anc, int idx) {
        int cur = idx;
        while (cur > 0) {
            if (cur == anc) return true;
            cur = comps.get(cur).getLayer();
        }
        return cur == anc;
    }

    static int size(int v, int mode, int parent) {
        return switch (mode) { case 0 -> v; case 1 -> parent - v; case 2 -> (parent * v) >> 14; default -> v; };
    }

    static int pos(int v, int mode, int parent, int self) {
        return switch (mode) { case 0 -> v; case 1 -> (parent - self) / 2 + v; case 2 -> parent - self - v; default -> v; };
    }

    /** [proc,steelborder] as decoded from its bytecode. */
    static void steelborder(int x, int y, int w, int h, String title) {
        sprite(297, x + 1, y + 1, w - 2, h - 2, true);
        text(496, title, x + 6, y + 6, w - 12, 24, 0xFF981F, true, 1, 1);
        sprite(310, x, y, 25, 30, false);
        sprite(311, x + w - 25, y, 25, 30, false);
        sprite(312, x, y + h - 30, 25, 30, false);
        sprite(313, x + w - 25, y + h - 30, 25, 30, false);
        sprite(172, x - 15, y + 30, 36, h - 60, true);
        sprite(315, x + w - 36 + 15, y + 30, 36, h - 60, true);
        sprite(314, x + 25, y - 15, w - 50, 36, true);
        sprite(173, x + 25, y + h - 36 + 15, w - 50, 36, true);
        sprite(535, x + w - 26 - 3, y + 6, 26, 23, false);
    }

    static SpriteDump.Frame spr(int id) {
        return SPRITES.computeIfAbsent(id, k -> {
            List<SpriteDump.Frame> fr = SpriteDump.decode(cache, k);
            return fr == null || fr.isEmpty() ? null : fr.get(0);
        });
    }

    static void sprite(int id, int x, int y, int w, int h, boolean tiling) {
        SpriteDump.Frame s = spr(id);
        if (s == null) return;
        if (tiling) {
            for (int ty = 0; ty < h; ty += s.h()) for (int tx = 0; tx < w; tx += s.w())
                blit(s, x + tx, y + ty, Math.min(s.w(), w - tx), Math.min(s.h(), h - ty));
        } else {
            blit(s, x, y, s.w(), s.h());
        }
    }

    static void blit(SpriteDump.Frame s, int x, int y, int cw, int ch) {
        for (int j = 0; j < ch; j++) for (int i = 0; i < cw; i++) {
            int argb = s.argb()[j * s.w() + i];
            int a = argb >>> 24;
            if (a == 0) continue;
            plot(x + i, y + j, argb & 0xFFFFFF, a);
        }
    }

    static void plot(int x, int y, int rgb, int a) {
        if (x < 0 || y < 0 || x >= W || y >= H) return;
        int d = px[y * W + x];
        int r = (((rgb >> 16) & 255) * a + ((d >> 16) & 255) * (255 - a)) / 255;
        int g = (((rgb >> 8) & 255) * a + ((d >> 8) & 255) * (255 - a)) / 255;
        int b = ((rgb & 255) * a + (d & 255) * (255 - a)) / 255;
        px[y * W + x] = (r << 16) | (g << 8) | b;
    }

    static void fillRect(int x, int y, int w, int h, int rgb, int a) {
        for (int j = 0; j < h; j++) for (int i = 0; i < w; i++) plot(x + i, y + j, rgb, a);
    }

    static void outline(int x, int y, int w, int h, int rgb, int a) {
        for (int i = 0; i < w; i++) { plot(x + i, y, rgb, a); plot(x + i, y + h - 1, rgb, a); }
        for (int j = 1; j < h - 1; j++) { plot(x, y + j, rgb, a); plot(x + w - 1, y + j, rgb, a); }
    }

    static byte[] metrics(int font) {
        return METRICS.computeIfAbsent(font, k -> {
            ByteBuf b = cache.read(13, k, 0);
            byte[] d = new byte[b.readableBytes()];
            b.readBytes(d);
            b.release();
            return d;
        });
    }

    static List<SpriteDump.Frame> glyphs(int font) {
        return FONTS.computeIfAbsent(font, k -> SpriteDump.decode(cache, k));
    }

    static int width(int font, String s) {
        byte[] m = metrics(font);
        int w = 0;
        for (char ch : s.toCharArray()) w += m[ch & 0xFF] & 0xFF;
        return w;
    }

    static void text(int font, String s, int x, int y, int w, int h, int colour, boolean shadow, int alignH, int alignV) {
        if (s == null || s.isEmpty()) return;
        byte[] m = metrics(font);
        int ascent = m[256] & 0xFF;
        int tw = width(font, s);
        int penX = alignH == 1 ? x + (w - tw) / 2 : alignH == 2 ? x + w - tw : x;
        int top = alignV == 1 ? y + (h - ascent) / 2 : alignV == 2 ? y + h - ascent : y;
        if (tw > w) System.out.println("OVERFLOW font=" + font + " '" + s + "' " + tw + " > " + w);
        List<SpriteDump.Frame> gl = glyphs(font);
        for (int pass = shadow ? 0 : 1; pass < 2; pass++) {
            int pxp = penX + (pass == 0 ? 1 : 0), ty = top + (pass == 0 ? 1 : 0);
            int col = pass == 0 ? 0x000000 : colour;
            for (char ch : s.toCharArray()) {
                SpriteDump.Frame g = gl.get(ch & 0xFF);
                for (int j = 0; j < g.h(); j++) for (int i = 0; i < g.w(); i++)
                    if ((g.argb()[j * g.w() + i] >>> 24) != 0) plot(pxp + i, ty + j, col, 255);
                pxp += m[ch & 0xFF] & 0xFF;
            }
        }
    }
}
