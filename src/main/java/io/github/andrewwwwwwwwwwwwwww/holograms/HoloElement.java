package io.github.andrewwwwwwwwwwwwwww.holograms;

import com.mojang.math.Transformation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * One renderable piece: a text string, an item icon, or a block model. A {@link HoloLine} is made of
 * one or more elements laid out horizontally (multiple elements = a "row", e.g. a crafting recipe).
 */
public final class HoloElement {
    public enum Kind { TEXT, ITEM, BLOCK }

    public Kind kind;
    /** TEXT: raw string with {@code &} colour codes. */
    public String text = "";
    /** ITEM/BLOCK: registry id, e.g. {@code minecraft:diamond}. */
    public String resource = "";

    public HoloElement() {}

    public static HoloElement text(String text) {
        HoloElement e = new HoloElement();
        e.kind = Kind.TEXT;
        e.text = text;
        return e;
    }

    public static HoloElement item(String itemId) {
        HoloElement e = new HoloElement();
        e.kind = Kind.ITEM;
        e.resource = itemId;
        return e;
    }

    public static HoloElement block(String blockId) {
        HoloElement e = new HoloElement();
        e.kind = Kind.BLOCK;
        e.resource = blockId;
        return e;
    }

    /** Vertical space this element occupies, in blocks. */
    public double height() {
        return switch (kind) {
            case TEXT -> 0.30;
            case ITEM, BLOCK -> 0.55;
        };
    }

    public String describe() {
        return switch (kind) {
            case TEXT -> "\"" + text + "\"";
            case ITEM -> "item " + resource;
            case BLOCK -> "block " + resource;
        };
    }

    /** An ItemStack used to preview this element inside the editor GUI. */
    public ItemStack icon() {
        return switch (kind) {
            case TEXT -> new ItemStack(Items.PAPER);
            case ITEM -> {
                Item i = lookupItem();
                yield i == null ? new ItemStack(Items.BARRIER) : new ItemStack(i);
            }
            case BLOCK -> {
                Block b = lookupBlock();
                yield b == null ? new ItemStack(Items.BARRIER) : new ItemStack(b.asItem());
            }
        };
    }

    public Item lookupItem() {
        Identifier id = Identifier.tryParse(resource);
        return id == null ? null : BuiltInRegistries.ITEM.getOptional(id).orElse(null);
    }

    public Block lookupBlock() {
        Identifier id = Identifier.tryParse(resource);
        return id == null ? null : BuiltInRegistries.BLOCK.getOptional(id).orElse(null);
    }

    /**
     * Create the display entity for this element. The entity is positioned by the renderer; the
     * vertical-centring offset for the visual is returned to the renderer via {@link #yCenterOffset()}.
     */
    public Entity createEntity(ServerLevel level) {
        return switch (kind) {
            case TEXT -> {
                Display.TextDisplay td = new Display.TextDisplay(EntityType.TEXT_DISPLAY, level);
                td.setText(Colors.parse(text));
                td.setBillboardConstraints(Display.BillboardConstraints.CENTER);
                yield td;
            }
            case ITEM -> {
                Item item = lookupItem();
                if (item == null) yield null;
                Display.ItemDisplay disp = new Display.ItemDisplay(EntityType.ITEM_DISPLAY, level);
                disp.setItemStack(new ItemStack(item));
                disp.setItemTransform(ItemDisplayContext.GROUND);
                disp.setBillboardConstraints(Display.BillboardConstraints.CENTER);
                disp.setTransformation(scale(0.5f, 0f));
                yield disp;
            }
            case BLOCK -> {
                Block block = lookupBlock();
                if (block == null) yield null;
                Display.BlockDisplay disp = new Display.BlockDisplay(EntityType.BLOCK_DISPLAY, level);
                disp.setBlockState(block.defaultBlockState());
                // CENTER so the block always presents a readable face to the viewer.
                disp.setBillboardConstraints(Display.BillboardConstraints.CENTER);
                disp.setTransformation(scale(0.5f, -0.25f));
                yield disp;
            }
        };
    }

    /** Y offset to add to the line centre so the visual sits centred (text renders upward from anchor). */
    public double yCenterOffset() {
        return kind == Kind.TEXT ? -0.13 : 0.0;
    }

    private static Transformation scale(float s, float offset) {
        return new Transformation(
                new Vector3f(offset, offset, offset),
                new Quaternionf(),
                new Vector3f(s, s, s),
                new Quaternionf());
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Kind", kind.name());
        tag.putString("Text", text);
        tag.putString("Resource", resource);
        return tag;
    }

    public static HoloElement load(CompoundTag tag) {
        HoloElement e = new HoloElement();
        e.kind = switch (tag.getStringOr("Kind", "TEXT")) {
            case "ITEM" -> Kind.ITEM;
            case "BLOCK" -> Kind.BLOCK;
            default -> Kind.TEXT;
        };
        e.text = tag.getStringOr("Text", "");
        e.resource = tag.getStringOr("Resource", "");
        return e;
    }
}
