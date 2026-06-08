package io.github.andrewwwwwwwwwwwwwww.holograms;

import net.minecraft.nbt.CompoundTag;

/**
 * A single line of a hologram. A line is one of:
 * <ul>
 *   <li>{@link Kind#TEXT} — a string with {@code &} colour codes (rendered as a text_display),</li>
 *   <li>{@link Kind#ITEM} — a floating item icon (item_display), {@code resource} = item id,</li>
 *   <li>{@link Kind#BLOCK} — a floating block model (block_display), {@code resource} = block id.</li>
 * </ul>
 */
public final class HoloLine {
    public enum Kind { TEXT, ITEM, BLOCK }

    public Kind kind;
    /** For TEXT lines: the raw text with {@code &} colour codes. */
    public String text = "";
    /** For ITEM/BLOCK lines: the registry id (e.g. {@code minecraft:diamond}). */
    public String resource = "";

    public HoloLine() {}

    public static HoloLine text(String text) {
        HoloLine l = new HoloLine();
        l.kind = Kind.TEXT;
        l.text = text;
        return l;
    }

    public static HoloLine item(String itemId) {
        HoloLine l = new HoloLine();
        l.kind = Kind.ITEM;
        l.resource = itemId;
        return l;
    }

    public static HoloLine block(String blockId) {
        HoloLine l = new HoloLine();
        l.kind = Kind.BLOCK;
        l.resource = blockId;
        return l;
    }

    /** Vertical space this line occupies, in blocks (used to stack lines downward). */
    public double height() {
        return switch (kind) {
            case TEXT -> 0.27;
            case ITEM -> 0.45;
            case BLOCK -> 0.60;
        };
    }

    public String describe() {
        return switch (kind) {
            case TEXT -> "\"" + text + "\"";
            case ITEM -> "item " + resource;
            case BLOCK -> "block " + resource;
        };
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Kind", kind.name());
        tag.putString("Text", text);
        tag.putString("Resource", resource);
        return tag;
    }

    public static HoloLine load(CompoundTag tag) {
        HoloLine l = new HoloLine();
        l.kind = switch (tag.getStringOr("Kind", "TEXT")) {
            case "ITEM" -> Kind.ITEM;
            case "BLOCK" -> Kind.BLOCK;
            default -> Kind.TEXT;
        };
        l.text = tag.getStringOr("Text", "");
        l.resource = tag.getStringOr("Resource", "");
        return l;
    }
}
