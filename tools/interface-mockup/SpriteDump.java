import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import org.openrs2.cache.Cache;
import org.openrs2.cache.Store;

/**
 * Renders cache sprites (js5 archive 8) to PNG.
 *
 * Usage: SpriteDump <cacheDir> <outDir> sheet <from> <to> <name>  -- labelled contact sheet
 *        SpriteDump <cacheDir> <outDir> each <id> [id...]           -- one PNG per frame, 4x scale
 */
public class SpriteDump {
    record Frame(int w, int h, int[] argb) {}

    public static void main(String[] args) throws Exception {
        Path cacheDir = Path.of(args[0]);
        File out = new File(args[1]);
        out.mkdirs();
        try (Store store = Store.open(cacheDir, ByteBufAllocator.DEFAULT);
                Cache cache = Cache.open(store, ByteBufAllocator.DEFAULT)) {
            if (args[2].equals("sheet")) {
                sheet(cache, out, Integer.parseInt(args[3]), Integer.parseInt(args[4]), args[5]);
            } else {
                for (int i = 3; i < args.length; i++) {
                    int id = Integer.parseInt(args[i]);
                    List<Frame> frames = decode(cache, id);
                    if (frames == null) continue;
                    for (int f = 0; f < frames.size(); f++) {
                        Frame fr = frames.get(f);
                        int s = 4;
                        BufferedImage img = new BufferedImage(Math.max(1, fr.w * s), Math.max(1, fr.h * s), BufferedImage.TYPE_INT_ARGB);
                        for (int y = 0; y < fr.h * s; y++)
                            for (int x = 0; x < fr.w * s; x++) img.setRGB(x, y, fr.argb[(y / s) * fr.w + (x / s)]);
                        ImageIO.write(img, "png", new File(out, id + "_" + f + ".png"));
                        System.out.println(id + "_" + f + " " + fr.w + "x" + fr.h);
                    }
                }
            }
        }
    }

    static void sheet(Cache cache, File out, int from, int to, String name) throws Exception {
        int cell = 72, cols = 12;
        List<Integer> ids = new ArrayList<>();
        List<Frame> firsts = new ArrayList<>();
        List<Integer> counts = new ArrayList<>();
        for (int id = from; id <= to; id++) {
            List<Frame> frames = decode(cache, id);
            if (frames == null || frames.isEmpty()) continue;
            ids.add(id);
            firsts.add(frames.get(0));
            counts.add(frames.size());
        }
        int rows = (ids.size() + cols - 1) / cols;
        BufferedImage img = new BufferedImage(cols * cell, Math.max(1, rows * (cell + 14)), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(0x40, 0x40, 0x48));
        g.fillRect(0, 0, img.getWidth(), img.getHeight());
        g.setFont(new Font("SansSerif", Font.PLAIN, 10));
        for (int i = 0; i < ids.size(); i++) {
            int cx = (i % cols) * cell, cy = (i / cols) * (cell + 14);
            Frame fr = firsts.get(i);
            double scale = Math.min(1.0, Math.min((cell - 4) / (double) fr.w, (cell - 4) / (double) fr.h));
            if (fr.w <= 24 && fr.h <= 24) scale = 2.0;
            int dw = (int) Math.max(1, fr.w * scale), dh = (int) Math.max(1, fr.h * scale);
            BufferedImage s = new BufferedImage(Math.max(1, fr.w), Math.max(1, fr.h), BufferedImage.TYPE_INT_ARGB);
            s.setRGB(0, 0, fr.w, fr.h, fr.argb, 0, fr.w);
            g.drawImage(s, cx + (cell - dw) / 2, cy + (cell - dh) / 2, dw, dh, null);
            g.setColor(Color.WHITE);
            String label = ids.get(i) + (counts.get(i) > 1 ? "x" + counts.get(i) : "") + " " + fr.w + "x" + fr.h;
            g.drawString(label, cx + 2, cy + cell + 11);
        }
        ImageIO.write(img, "png", new File(out, name + ".png"));
        System.out.println("sheet " + name + " sprites=" + ids.size());
    }

    static List<Frame> decode(Cache cache, int id) {
        ByteBuf buf;
        try {
            buf = cache.read(8, id, 0);
        } catch (Exception e) {
            return null;
        }
        try {
            byte[] d = new byte[buf.readableBytes()];
            buf.readBytes(d);
            int len = d.length;
            int count = u16(d, len - 2);
            int hdr = len - 7 - count * 8;
            int maxW = u16(d, hdr);
            int maxH = u16(d, hdr + 2);
            int palSize = (d[hdr + 4] & 0xFF) + 1;
            int[] offX = new int[count], offY = new int[count], ws = new int[count], hs = new int[count];
            int p = hdr + 5;
            for (int i = 0; i < count; i++) { offX[i] = u16(d, p); p += 2; }
            for (int i = 0; i < count; i++) { offY[i] = u16(d, p); p += 2; }
            for (int i = 0; i < count; i++) { ws[i] = u16(d, p); p += 2; }
            for (int i = 0; i < count; i++) { hs[i] = u16(d, p); p += 2; }
            int palStart = hdr - (palSize - 1) * 3;
            int[] pal = new int[palSize];
            for (int i = 1; i < palSize; i++) {
                int q = palStart + (i - 1) * 3;
                int rgb = ((d[q] & 0xFF) << 16) | ((d[q + 1] & 0xFF) << 8) | (d[q + 2] & 0xFF);
                pal[i] = rgb == 0 ? 1 : rgb;
            }
            List<Frame> frames = new ArrayList<>();
            int pos = 0;
            for (int i = 0; i < count; i++) {
                int w = ws[i], h = hs[i], n = w * h;
                int[] idx = new int[n];
                int[] alpha = new int[n];
                int flags = d[pos++] & 0xFF;
                boolean colMajor = (flags & 1) != 0;
                boolean hasAlpha = (flags & 2) != 0;
                if (colMajor) {
                    for (int x = 0; x < w; x++) for (int y = 0; y < h; y++) idx[y * w + x] = d[pos++] & 0xFF;
                } else {
                    for (int k = 0; k < n; k++) idx[k] = d[pos++] & 0xFF;
                }
                if (hasAlpha) {
                    if (colMajor) {
                        for (int x = 0; x < w; x++) for (int y = 0; y < h; y++) alpha[y * w + x] = d[pos++] & 0xFF;
                    } else {
                        for (int k = 0; k < n; k++) alpha[k] = d[pos++] & 0xFF;
                    }
                }
                // Place into a maxW x maxH canvas so offsets are honoured, like the client does.
                int[] argb = new int[Math.max(1, maxW * maxH)];
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        int k = y * w + x;
                        int c = idx[k];
                        int a = hasAlpha ? alpha[k] : (c == 0 ? 0 : 255);
                        if (c == 0 && !hasAlpha) continue;
                        int tx = x + offX[i], ty = y + offY[i];
                        if (tx < maxW && ty < maxH) argb[ty * maxW + tx] = (a << 24) | pal[c];
                    }
                }
                frames.add(new Frame(maxW, maxH, argb));
            }
            return frames;
        } catch (Exception e) {
            System.out.println("decode fail " + id + ": " + e);
            return null;
        } finally {
            buf.release();
        }
    }

    static int u16(byte[] d, int p) {
        return ((d[p] & 0xFF) << 8) | (d[p + 1] & 0xFF);
    }
}
