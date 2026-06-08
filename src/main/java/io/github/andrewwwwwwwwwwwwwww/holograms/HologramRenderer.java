package io.github.andrewwwwwwwwwwwwwww.holograms;

import com.mojang.math.Transformation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.entity.EntityTypeTest;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

/**
 * Turns a {@link Hologram} definition into real display entities in the world, and finds/removes them
 * again by tag. Each spawned entity carries the {@link #GENERIC_TAG} plus a per-hologram tag so they
 * can be located later without keeping entity references (which go stale across chunk loads).
 */
public final class HologramRenderer {
    public static final String GENERIC_TAG = "holograms";

    private HologramRenderer() {}

    public static String nameTag(String name) {
        return "holo_" + name;
    }

    public static ServerLevel levelOf(MinecraftServer server, Hologram h) {
        Identifier id = Identifier.tryParse(h.dimension);
        if (id != null) {
            for (ServerLevel level : server.getAllLevels()) {
                if (level.dimension().identifier().equals(id)) return level;
            }
        }
        return server.overworld();
    }

    /** Rebuild a hologram's entities: remove any existing ones, then spawn fresh from the definition. */
    public static void render(MinecraftServer server, Hologram h) {
        ServerLevel level = levelOf(server, h);
        despawn(server, h.name);
        spawn(level, h);
    }

    private static void spawn(ServerLevel level, Hologram h) {
        double cursorY = h.y;
        for (HoloLine line : h.lines) {
            Entity e = createLineEntity(level, line);
            if (e != null) {
                configure(e, h.name);
                e.snapTo(h.x, cursorY - line.height(), h.z, h.yaw, 0.0f);
                level.addFreshEntity(e);
            }
            cursorY -= line.height();
        }

        if (h.hasClickAction()) {
            double total = h.totalHeight();
            Interaction interaction = new Interaction(EntityType.INTERACTION, level);
            interaction.setWidth(1.2f);
            interaction.setHeight((float) Math.max(0.5, total));
            interaction.setResponse(true);
            configure(interaction, h.name);
            // Interaction anchor is its bottom centre; lines span from h.y down to h.y - total.
            interaction.snapTo(h.x, h.y - total, h.z, h.yaw, 0.0f);
            level.addFreshEntity(interaction);
        }
    }

    private static Entity createLineEntity(ServerLevel level, HoloLine line) {
        return switch (line.kind) {
            case TEXT -> {
                Display.TextDisplay td = new Display.TextDisplay(EntityType.TEXT_DISPLAY, level);
                td.setText(Colors.parse(line.text));
                td.setBillboardConstraints(Display.BillboardConstraints.CENTER);
                yield td;
            }
            case ITEM -> {
                Identifier id = Identifier.tryParse(line.resource);
                Item item = id == null ? null : BuiltInRegistries.ITEM.getOptional(id).orElse(null);
                if (item == null) yield null;
                Display.ItemDisplay disp = new Display.ItemDisplay(EntityType.ITEM_DISPLAY, level);
                disp.setItemStack(new ItemStack(item));
                disp.setItemTransform(ItemDisplayContext.FIXED);
                disp.setBillboardConstraints(Display.BillboardConstraints.CENTER);
                disp.setTransformation(scale(0.5f, 0f));
                yield disp;
            }
            case BLOCK -> {
                Identifier id = Identifier.tryParse(line.resource);
                Block block = id == null ? null : BuiltInRegistries.BLOCK.getOptional(id).orElse(null);
                if (block == null) yield null;
                Display.BlockDisplay disp = new Display.BlockDisplay(EntityType.BLOCK_DISPLAY, level);
                disp.setBlockState(block.defaultBlockState());
                disp.setBillboardConstraints(Display.BillboardConstraints.FIXED);
                // Block models render from their corner; centre a half-scale block on the anchor.
                disp.setTransformation(scale(0.5f, -0.25f));
                yield disp;
            }
        };
    }

    private static Transformation scale(float s, float offset) {
        return new Transformation(
                new Vector3f(offset, offset, offset),
                new Quaternionf(),
                new Vector3f(s, s, s),
                new Quaternionf());
    }

    private static void configure(Entity e, String name) {
        e.setNoGravity(true);
        e.setInvulnerable(true);
        e.addTag(GENERIC_TAG);
        e.addTag(nameTag(name));
    }

    /** Discard every entity belonging to the named hologram, across all loaded dimensions. */
    public static void despawn(MinecraftServer server, String name) {
        String tag = nameTag(name);
        for (ServerLevel level : server.getAllLevels()) {
            List<? extends Entity> found = level.getEntities(
                    EntityTypeTest.forClass(Entity.class), e -> e.entityTags().contains(tag));
            for (Entity e : found) e.discard();
        }
    }

    /** Discard EVERY hologram-tagged entity (used to clear orphans before a full re-render). */
    public static int despawnAll(MinecraftServer server) {
        int count = 0;
        for (ServerLevel level : server.getAllLevels()) {
            List<? extends Entity> found = level.getEntities(
                    EntityTypeTest.forClass(Entity.class), e -> e.entityTags().contains(GENERIC_TAG));
            for (Entity e : found) { e.discard(); count++; }
        }
        return count;
    }
}
