package io.github.andrewwwwwwwwwwwwwww.holograms;

import com.mojang.math.Transformation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
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

    /** Horizontal spacing between elements within a row, in blocks. */
    private static final double ROW_DX = 0.5;

    private static void spawn(ServerLevel level, Hologram h) {
        double topY = h.y; // top of the whole stack
        for (HoloLine line : h.lines) {
            double lineH = line.height();
            double centreY = topY - lineH / 2.0;
            int n = line.elements.size();
            double startOffset = -(n - 1) * ROW_DX / 2.0;
            for (int i = 0; i < n; i++) {
                HoloElement el = line.elements.get(i);
                Entity e = el.createEntity(level);
                if (e != null) {
                    if (e instanceof Display.TextDisplay td) {
                        td.setBackgroundColor(h.textBackground ? Display.TextDisplay.INITIAL_BACKGROUND : 0);
                    }
                    configure(e, h.name);
                    double dx = startOffset + i * ROW_DX;
                    e.snapTo(h.x + dx, centreY + el.yCenterOffset(), h.z, h.yaw, 0.0f);
                    level.addFreshEntity(e);
                }
            }
            topY -= (lineH + h.lineSpacing);
        }

        if (h.hasClickAction()) {
            double total = h.totalHeight();
            Interaction interaction = new Interaction(EntityTypes.INTERACTION, level);
            interaction.setWidth(1.4f);
            interaction.setHeight((float) Math.max(0.5, total));
            interaction.setResponse(true);
            configure(interaction, h.name);
            // Interaction anchor is its bottom centre; lines span from h.y down to h.y - total.
            interaction.snapTo(h.x, h.y - total, h.z, h.yaw, 0.0f);
            level.addFreshEntity(interaction);
        }
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
