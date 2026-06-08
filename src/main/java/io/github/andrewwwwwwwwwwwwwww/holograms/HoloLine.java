package io.github.andrewwwwwwwwwwwwwww.holograms;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import java.util.ArrayList;
import java.util.List;

/**
 * A horizontal line of a hologram, made of one or more {@link HoloElement}s. A single text element is
 * an ordinary text line; multiple elements are laid out side-by-side (a "row"), which is handy for
 * showing crafting recipes (e.g. several item/block icons in a row).
 */
public final class HoloLine {
    public final List<HoloElement> elements = new ArrayList<>();

    public HoloLine() {}

    public static HoloLine text(String text) {
        HoloLine l = new HoloLine();
        l.elements.add(HoloElement.text(text));
        return l;
    }

    public static HoloLine of(HoloElement element) {
        HoloLine l = new HoloLine();
        l.elements.add(element);
        return l;
    }

    public boolean isRow() {
        return elements.size() > 1;
    }

    /** True if this line is a single text element (the common case). */
    public boolean isSingleText() {
        return elements.size() == 1 && elements.get(0).kind == HoloElement.Kind.TEXT;
    }

    public double height() {
        double h = 0;
        for (HoloElement e : elements) h = Math.max(h, e.height());
        return h <= 0 ? 0.30 : h;
    }

    public String describe() {
        if (elements.isEmpty()) return "(empty)";
        if (elements.size() == 1) return elements.get(0).describe();
        StringBuilder sb = new StringBuilder("row[");
        for (int i = 0; i < elements.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(elements.get(i).describe());
        }
        return sb.append("]").toString();
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (HoloElement e : elements) list.add(list.size(), e.save());
        tag.put("Elements", list);
        return tag;
    }

    public static HoloLine load(CompoundTag tag) {
        HoloLine l = new HoloLine();
        ListTag list = tag.getListOrEmpty("Elements");
        for (int i = 0; i < list.size(); i++) l.elements.add(HoloElement.load(list.getCompoundOrEmpty(i)));
        // Backwards-compat: a 0.1.0 line stored its fields directly (no Elements list).
        if (l.elements.isEmpty()) l.elements.add(HoloElement.load(tag));
        return l;
    }
}
