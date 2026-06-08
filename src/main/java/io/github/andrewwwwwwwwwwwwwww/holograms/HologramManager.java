package io.github.andrewwwwwwwwwwwwwww.holograms;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Owns every hologram definition, keyed by name. All definitions persist in a single compressed NBT
 * file under the world folder ({@code <world>/holograms/holograms.dat}). The display entities
 * themselves live in the world (they save with their chunks); this file only stores the definitions
 * so holograms can be edited/moved/removed by name later.
 */
public final class HologramManager {
    private final Map<String, Hologram> holograms = new LinkedHashMap<>();

    public Hologram get(String name) {
        return holograms.get(name.toLowerCase());
    }

    public boolean exists(String name) {
        return holograms.containsKey(name.toLowerCase());
    }

    public Hologram create(String name) {
        Hologram h = new Hologram(name);
        holograms.put(name.toLowerCase(), h);
        return h;
    }

    public void remove(String name) {
        holograms.remove(name.toLowerCase());
    }

    public Collection<Hologram> all() {
        return holograms.values();
    }

    public List<String> names() {
        return new ArrayList<>(holograms.values().stream().map(h -> h.name).toList());
    }

    // ---- persistence ----

    private Path file(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("holograms").resolve("holograms.dat");
    }

    public void save(MinecraftServer server) {
        if (server == null) return;
        try {
            Path file = file(server);
            Files.createDirectories(file.getParent());
            CompoundTag root = new CompoundTag();
            ListTag list = new ListTag();
            for (Hologram h : holograms.values()) list.add(list.size(), h.save());
            root.put("Holograms", list);
            NbtIo.writeCompressed(root, file);
        } catch (Exception e) {
            Holograms.LOGGER.error("Failed to save holograms", e);
        }
    }

    public void load(MinecraftServer server) {
        holograms.clear();
        if (server == null) return;
        Path file = file(server);
        if (!Files.exists(file)) return;
        try {
            CompoundTag root = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
            ListTag list = root.getListOrEmpty("Holograms");
            for (int i = 0; i < list.size(); i++) {
                Hologram h = Hologram.load(list.getCompoundOrEmpty(i));
                holograms.put(h.name.toLowerCase(), h);
            }
            Holograms.LOGGER.info("Loaded {} hologram(s)", holograms.size());
        } catch (Exception e) {
            Holograms.LOGGER.error("Failed to load holograms", e);
        }
    }
}
