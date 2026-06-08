package io.github.andrewwwwwwwwwwwwwww.holograms;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import java.util.ArrayList;
import java.util.List;

/**
 * A named hologram definition: where it is, what lines it shows, and what happens when it's clicked.
 * The definition is the source of truth; the actual display entities in the world are (re)built from
 * it by {@link HologramRenderer}.
 */
public final class Hologram {
    public final String name;
    /** Dimension id, e.g. {@code minecraft:overworld}. */
    public String dimension;
    public double x, y, z;
    public float yaw;
    /** Extra vertical gap between lines, in blocks. Tunable in the editor. */
    public double lineSpacing = 0.08;

    public final List<HoloLine> lines = new ArrayList<>();

    // ---- click actions (optional) ----
    /** Commands run (with elevated permission, as the clicking player) on right-click. */
    public final List<String> clickCommands = new ArrayList<>();
    /** Message sent to the player on right-click (supports {@code &} colour codes). May be null. */
    public String clickMessage;
    /** Sound id played to the player on right-click, e.g. {@code minecraft:ui.button.click}. May be null. */
    public String clickSound;

    public Hologram(String name) {
        this.name = name;
    }

    public boolean hasClickAction() {
        return !clickCommands.isEmpty() || clickMessage != null || clickSound != null;
    }

    /** Total height of all stacked lines (including the gaps between them), in blocks. */
    public double totalHeight() {
        double h = 0;
        for (int i = 0; i < lines.size(); i++) {
            h += lines.get(i).height();
            if (i < lines.size() - 1) h += lineSpacing;
        }
        return h;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Name", name);
        tag.putString("Dimension", dimension);
        tag.putDouble("X", x);
        tag.putDouble("Y", y);
        tag.putDouble("Z", z);
        tag.putFloat("Yaw", yaw);
        tag.putDouble("LineSpacing", lineSpacing);

        ListTag lineList = new ListTag();
        for (HoloLine l : lines) lineList.add(lineList.size(), l.save());
        tag.put("Lines", lineList);

        ListTag cmds = new ListTag();
        for (String c : clickCommands) {
            CompoundTag e = new CompoundTag();
            e.putString("Cmd", c);
            cmds.add(cmds.size(), e);
        }
        tag.put("ClickCommands", cmds);
        if (clickMessage != null) tag.putString("ClickMessage", clickMessage);
        if (clickSound != null) tag.putString("ClickSound", clickSound);
        return tag;
    }

    public static Hologram load(CompoundTag tag) {
        Hologram h = new Hologram(tag.getStringOr("Name", "unnamed"));
        h.dimension = tag.getStringOr("Dimension", "minecraft:overworld");
        h.x = tag.getDoubleOr("X", 0);
        h.y = tag.getDoubleOr("Y", 0);
        h.z = tag.getDoubleOr("Z", 0);
        h.yaw = tag.getFloatOr("Yaw", 0);
        h.lineSpacing = tag.getDoubleOr("LineSpacing", 0.08);

        ListTag lineList = tag.getListOrEmpty("Lines");
        for (int i = 0; i < lineList.size(); i++) h.lines.add(HoloLine.load(lineList.getCompoundOrEmpty(i)));

        ListTag cmds = tag.getListOrEmpty("ClickCommands");
        for (int i = 0; i < cmds.size(); i++) {
            String c = cmds.getCompoundOrEmpty(i).getStringOr("Cmd", "");
            if (!c.isBlank()) h.clickCommands.add(c);
        }
        String msg = tag.getStringOr("ClickMessage", "");
        h.clickMessage = msg.isEmpty() ? null : msg;
        String snd = tag.getStringOr("ClickSound", "");
        h.clickSound = snd.isEmpty() ? null : snd;
        return h;
    }
}
