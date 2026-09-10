import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.rsmod.game.type.comp.UnpackedComponentType;

/**
 * Converts a hand-written {@code ComponentBuilder} object into a design file, so a panel authored in
 * Kotlin can be opened in the designer. Prints the design as JSON on stdout; {@code serve.py
 * --import} gives it the canonical layout and writes it to {@code designs/}.
 *
 * <p>The design format is deliberately smaller than the component format, so this is strict: any
 * field set to something the design cannot express is a hard error rather than a silent drop.
 * Known hooks are turned back into their semantic form - {@code onLoad=[227, event_com, title]}
 * into {@code frame}, and matching {@code onMouseOver}/{@code onMouseLeave} pairs on scripts 45,
 * 273 and 44 into {@code hover}.
 *
 * <p>Names come from the interface's block in {@code component.sym} rather than the builder, so
 * this works for builders that do not expose {@code componentNames}. Each component must sit at the
 * child index its symbol names, which is also what {@code ComponentBuilderResolver} checks.
 *
 * <p>Usage: DesignImport &lt;builderClass&gt; &lt;component.sym&gt;
 */
public class DesignImport {
    static final int EVENT_COM = -2147483645;
    static final String[] POS = {"start", "centre", "end"};
    static final String[] SIZE = {"fixed", "minus"};
    static final String[] ALIGN_H = {"left", "centre", "right"};
    static final String[] ALIGN_V = {"top", "centre", "bottom"};

    public static void main(String[] args) throws Exception {
        Class<?> builderClass = Class.forName(args[0]);
        Object builder = builderClass.getField("INSTANCE").get(null);
        Field cacheField = findField(builderClass, "cache");
        cacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<UnpackedComponentType> comps = (List<UnpackedComponentType>) cacheField.get(builder);
        if (comps.isEmpty()) throw new IllegalStateException(args[0] + " authors no components.");

        String iface = comps.get(0).getInternalName().split(":", 2)[0];
        Map<Integer, String> symbolNames = symbolNames(Path.of(args[1]), iface);

        List<String> names = new ArrayList<>();
        for (int i = 0; i < comps.size(); i++) {
            String[] parts = comps.get(i).getInternalName().split(":", 2);
            if (!parts[0].equals(iface)) fail(comps.get(i), "belongs to another interface");
            if (!parts[1].equals(symbolNames.get(i))) {
                fail(comps.get(i), "is declared at index " + i + " but component.sym names that index `"
                        + symbolNames.get(i) + "`");
            }
            names.add(parts[1]);
        }

        List<Map<String, Object>> out = new ArrayList<>();
        for (int i = 0; i < comps.size(); i++) out.add(convert(comps.get(i), i, names));

        Map<String, Object> design = new LinkedHashMap<>();
        design.put("format", 1);
        design.put("interface", iface);
        design.put("notes", "Imported from " + builderClass.getName() + ".");
        design.put("components", out);
        System.out.println(new ObjectMapper().writeValueAsString(design));
    }

    static Map<String, Object> convert(UnpackedComponentType c, int index, List<String> names) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", names.get(index));
        if (c.getLayer() == -1) {
            if (index != 0) fail(c, "is a second root");
        } else {
            // Before the resolver runs, `layer` still holds the plain child index the builder wrote.
            m.put("parent", names.get(c.getLayer() & 0xFFFF));
        }
        String type = switch (c.getType()) {
            case 0 -> "layer";
            case 3 -> "rect";
            case 4 -> "text";
            case 5 -> "graphic";
            // Only an empty model component, the kind ifSetObj fills; requireUnused rejects any
            // authored model.
            case 6 -> "item";
            default -> throw fail(c, "has type " + c.getType() + ", which designs cannot express");
        };
        m.put("type", type);
        m.put("x", c.getX());
        m.put("y", c.getY());
        m.put("w", c.getWidth());
        m.put("h", c.getHeight());
        m.put("xMode", word(c, POS, c.getXMode(), "xMode"));
        m.put("yMode", word(c, POS, c.getYMode(), "yMode"));
        m.put("wMode", word(c, SIZE, c.getWidthMode(), "widthMode"));
        m.put("hMode", word(c, SIZE, c.getHeightMode(), "heightMode"));
        m.put("hidden", c.getHide());
        m.put("clickThrough", !c.getNoClickThrough());

        Object[] load = c.getOnLoad();
        if (load != null) {
            if (load.length != 3 || !Objects.equals(load[0], 227) || !Objects.equals(load[1], EVENT_COM)
                    || !(load[2] instanceof String title) || !type.equals("layer")) {
                throw fail(c, "has an onLoad hook other than a steelborder frame: " + Arrays.toString(load));
            }
            m.put("frame", title);
        }

        boolean text = type.equals("text"), rect = type.equals("rect"), graphic = type.equals("graphic");
        if (rect || text) m.put("colour", String.format("%06x", c.getColour1()));
        else requireDefault(c, "colour1", c.getColour1(), 0);
        if (rect) m.put("filled", c.getFill());
        else requireDefault(c, "fill", c.getFill(), false);
        if (rect || graphic) m.put("trans", c.getTrans1());
        else requireDefault(c, "trans1", c.getTrans1(), 0);
        if (graphic) {
            m.put("sprite", c.getGraphic());
            m.put("tiling", c.getTiling());
        } else {
            requireDefault(c, "graphic", c.getGraphic(), -1);
            requireDefault(c, "tiling", c.getTiling(), false);
        }
        if (text) {
            m.put("text", c.getText());
            m.put("font", c.getTextFont());
            m.put("alignH", word(c, ALIGN_H, c.getTextAlignH(), "textAlignH"));
            m.put("alignV", word(c, ALIGN_V, c.getTextAlignV(), "textAlignV"));
            m.put("lineHeight", c.getTextLineHeight());
            m.put("shadow", c.getTextShadow());
        } else {
            requireDefault(c, "text", c.getText(), "");
            requireDefault(c, "textFont", c.getTextFont(), -1);
            requireDefault(c, "textAlignH", c.getTextAlignH(), 0);
            requireDefault(c, "textAlignV", c.getTextAlignV(), 0);
            requireDefault(c, "textLineHeight", c.getTextLineHeight(), 0);
            requireDefault(c, "textShadow", c.getTextShadow(), false);
        }

        Object hover = hover(c, type);
        if (hover != null) m.put("hover", hover);

        List<String> ops = Arrays.asList(c.getOp());
        int mask = 0;
        for (int i = 0; i < ops.size(); i++) if (!ops.get(i).isEmpty()) mask |= 1 << (i + 1);
        if (mask != c.getEvents()) {
            throw fail(c, "has events " + c.getEvents() + " but its ops " + ops + " imply " + mask);
        }
        m.put("ops", ops);
        m.put("opBase", c.getOpBase());

        requireUnused(c);
        return m;
    }

    /** Undoes the onMouseOver/onMouseLeave pair `DesignedComponentBuilder` writes for `hover`. */
    static Object hover(UnpackedComponentType c, String type) {
        Object[] over = c.getOnMouseOver(), leave = c.getOnMouseLeave();
        if (over == null && leave == null) return null;
        if (over == null || leave == null || over.length != 3 || leave.length != 3
                || !Objects.equals(over[0], leave[0]) || !Objects.equals(over[1], EVENT_COM)
                || !Objects.equals(leave[1], EVENT_COM)) {
            throw fail(c, "has hover hooks designs cannot express: " + Arrays.toString(over) + " / "
                    + Arrays.toString(leave));
        }
        int script = (Integer) over[0];
        Map<String, Object> h = new LinkedHashMap<>();
        if (script == 45 && type.equals("text") && Objects.equals(leave[2], c.getColour1())) {
            h.put("colour", String.format("%06x", (Integer) over[2]));
        } else if (script == 273 && (type.equals("rect") || type.equals("graphic"))
                && Objects.equals(leave[2], c.getTrans1())) {
            h.put("trans", over[2]);
        } else if (script == 44 && type.equals("graphic") && Objects.equals(leave[2], c.getGraphic())) {
            h.put("sprite", over[2]);
        } else {
            throw fail(c, "has a hover pair whose leave hook does not restore its resting value: "
                    + Arrays.toString(over) + " / " + Arrays.toString(leave));
        }
        return h;
    }

    /** Every field the design format has no word for must be at the builder default. */
    static void requireUnused(UnpackedComponentType c) {
        requireDefault(c, "buttonType", c.getButtonType(), 0);
        requireDefault(c, "clientCode", c.getClientCode(), 0);
        requireDefault(c, "mouseOverRedirect", c.getMouseOverRedirect(), -1);
        requireDefault(c, "scrollHeight", c.getScrollHeight(), 0);
        requireDefault(c, "scrollWidth", c.getScrollWidth(), 0);
        requireDefault(c, "secondaryText", c.getSecondaryText(), "");
        requireDefault(c, "colour2", c.getColour2(), 0);
        requireDefault(c, "mouseOverColour1", c.getMouseOverColour1(), 0);
        requireDefault(c, "mouseOverColour2", c.getMouseOverColour2(), 0);
        requireDefault(c, "secondaryGraphic", c.getSecondaryGraphic(), -1);
        requireDefault(c, "modelKind", c.getModelKind(), 1);
        requireDefault(c, "model", c.getModel(), -1);
        requireDefault(c, "modelAnim", c.getModelAnim(), -1);
        requireDefault(c, "modelZoom", c.getModelZoom(), 100);
        requireDefault(c, "targetVerb", c.getTargetVerb(), "");
        requireDefault(c, "targetBase", c.getTargetBase(), "");
        requireDefault(c, "buttonText", c.getButtonText(), "Ok");
        requireDefault(c, "angle2d", c.getAngle2d(), 0);
        requireDefault(c, "outline", c.getOutline(), 0);
        requireDefault(c, "graphicShadow", c.getGraphicShadow(), 0);
        requireDefault(c, "vFlip", c.getVFlip(), false);
        requireDefault(c, "hFlip", c.getHFlip(), false);
        requireDefault(c, "lineWid", c.getLineWid(), 1);
        requireDefault(c, "dragDeadZone", c.getDragDeadZone(), 0);
        requireDefault(c, "dragDeadTime", c.getDragDeadTime(), 0);
        Object[][] hooks = {
            c.getOnTargetLeave(), c.getOnTargetEnter(), c.getOnVarTransmit(), c.getOnInvTransmit(),
            c.getOnStatTransmit(), c.getOnTimer(), c.getOnOp(), c.getOnMouseRepeat(), c.getOnClick(),
            c.getOnClickRepeat(), c.getOnRelease(), c.getOnHold(), c.getOnDrag(),
            c.getOnDragComplete(), c.getOnScrollWheel(),
        };
        for (Object[] hook : hooks) if (hook != null) throw fail(c, "has a hook designs cannot express");
        if (c.getCs1Instructions() != null) throw fail(c, "has cs1 instructions");
    }

    static void requireDefault(UnpackedComponentType c, String field, Object value, Object expected) {
        if (!Objects.equals(value, expected)) {
            throw fail(c, "sets " + field + " = " + value + ", which a " + c.getType()
                    + "-type component in a design cannot express");
        }
    }

    static String word(UnpackedComponentType c, String[] words, int value, String field) {
        if (value < 0 || value >= words.length) throw fail(c, "has " + field + " " + value);
        return words[value];
    }

    static Map<Integer, String> symbolNames(Path sym, String iface) throws Exception {
        Map<Integer, String> names = new HashMap<>();
        String prefix = iface + ":";
        for (String line : Files.readAllLines(sym)) {
            if (!line.startsWith(prefix)) continue;
            String[] parts = line.split("\t");
            int index = Integer.parseInt(parts[0].substring(prefix.length()));
            names.put(index, parts[1].substring(prefix.length()));
        }
        return names;
    }

    static IllegalStateException fail(UnpackedComponentType c, String why) {
        throw new IllegalStateException("`" + c.getInternalName() + "` " + why + ".");
    }

    static Field findField(Class<?> c, String name) throws NoSuchFieldException {
        for (Class<?> k = c; k != null; k = k.getSuperclass()) {
            try {
                return k.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
            }
        }
        throw new NoSuchFieldException(name);
    }
}
