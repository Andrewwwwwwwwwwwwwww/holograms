package io.github.andrewwwwwwwwwwwwwww.holograms.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.github.andrewwwwwwwwwwwwwww.holograms.HoloLine;
import io.github.andrewwwwwwwwwwwwwww.holograms.Hologram;
import io.github.andrewwwwwwwwwwwwwww.holograms.HologramManager;
import io.github.andrewwwwwwwwwwwwwww.holograms.HologramRenderer;
import io.github.andrewwwwwwwwwwwwwww.holograms.Holograms;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import java.util.regex.Pattern;

/** Registers {@code /holograms} (aliases {@code /holo}, {@code /hd}). All editing requires op level 2. */
public final class HologramCommands {
    private static final Pattern VALID_NAME = Pattern.compile("[A-Za-z0-9_-]{1,32}");

    private static final SuggestionProvider<CommandSourceStack> HOLO_NAMES = (ctx, b) ->
            SharedSuggestionProvider.suggest(Holograms.MANAGER.names(), b);

    private HologramCommands() {}

    private static HologramManager mgr() {
        return Holograms.MANAGER;
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("holograms")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));

        root.then(Commands.literal("create")
                .then(Commands.argument("name", StringArgumentType.word())
                        .then(Commands.argument("text", StringArgumentType.greedyString())
                                .executes(HologramCommands::create))));

        root.then(Commands.literal("remove")
                .then(named().executes(HologramCommands::remove)));

        root.then(Commands.literal("list").executes(HologramCommands::list));

        root.then(Commands.literal("info")
                .then(named().executes(HologramCommands::info)));

        root.then(Commands.literal("movehere")
                .then(named().executes(HologramCommands::moveHere)));

        root.then(Commands.literal("teleport")
                .then(named().executes(HologramCommands::teleport)));

        root.then(Commands.literal("addline")
                .then(named().then(Commands.argument("text", StringArgumentType.greedyString())
                        .executes(HologramCommands::addLine))));

        root.then(Commands.literal("setline")
                .then(named().then(Commands.argument("index", IntegerArgumentType.integer(1))
                        .then(Commands.argument("text", StringArgumentType.greedyString())
                                .executes(HologramCommands::setLine)))));

        root.then(Commands.literal("insertline")
                .then(named().then(Commands.argument("index", IntegerArgumentType.integer(1))
                        .then(Commands.argument("text", StringArgumentType.greedyString())
                                .executes(HologramCommands::insertLine)))));

        root.then(Commands.literal("removeline")
                .then(named().then(Commands.argument("index", IntegerArgumentType.integer(1))
                        .executes(HologramCommands::removeLine))));

        root.then(Commands.literal("additem")
                .then(named().then(Commands.argument("item", StringArgumentType.string())
                        .executes(HologramCommands::addItem))));

        root.then(Commands.literal("addblock")
                .then(named().then(Commands.argument("block", StringArgumentType.string())
                        .executes(HologramCommands::addBlock))));

        root.then(Commands.literal("setclickcommand")
                .then(named().then(Commands.argument("command", StringArgumentType.greedyString())
                        .executes(HologramCommands::addClickCommand))));

        root.then(Commands.literal("setclickmessage")
                .then(named().then(Commands.argument("message", StringArgumentType.greedyString())
                        .executes(HologramCommands::setClickMessage))));

        root.then(Commands.literal("setclicksound")
                .then(named().then(Commands.argument("sound", StringArgumentType.string())
                        .executes(HologramCommands::setClickSound))));

        root.then(Commands.literal("clearclick")
                .then(named().executes(HologramCommands::clearClick)));

        root.then(Commands.literal("reload").executes(HologramCommands::reload));

        LiteralCommandNode<CommandSourceStack> node = dispatcher.register(root);
        dispatcher.register(Commands.literal("holo")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).redirect(node));
        dispatcher.register(Commands.literal("hd")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).redirect(node));
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> named() {
        return Commands.argument("name", StringArgumentType.word()).suggests(HOLO_NAMES);
    }

    // ---- handlers ----

    private static int create(CommandContext<CommandSourceStack> ctx) {
        String name = StringArgumentType.getString(ctx, "name");
        if (!VALID_NAME.matcher(name).matches()) {
            return fail(ctx, "Invalid name. Use letters, digits, _ or - (max 32).");
        }
        if (mgr().exists(name)) return fail(ctx, "A hologram named '" + name + "' already exists.");

        CommandSourceStack src = ctx.getSource();
        ServerLevel level = src.getLevel();
        Vec3 pos = src.getPosition();
        Hologram h = mgr().create(name);
        h.dimension = level.dimension().identifier().toString();
        h.x = pos.x;
        h.y = pos.y + 1.8; // float above the creator's feet
        h.z = pos.z;
        h.yaw = src.getRotation().y;
        h.lines.add(HoloLine.text(StringArgumentType.getString(ctx, "text")));
        return afterEdit(ctx, h, "Created hologram '" + name + "'.");
    }

    private static int remove(CommandContext<CommandSourceStack> ctx) {
        Hologram h = holo(ctx);
        if (h == null) return 0;
        HologramRenderer.despawn(Holograms.server, h.name);
        mgr().remove(h.name);
        mgr().save(Holograms.server);
        return success(ctx, "Removed hologram '" + h.name + "'.");
    }

    private static int list(CommandContext<CommandSourceStack> ctx) {
        var names = mgr().names();
        if (names.isEmpty()) return success(ctx, "No holograms exist.");
        return success(ctx, "Holograms (" + names.size() + "): " + String.join(", ", names));
    }

    private static int info(CommandContext<CommandSourceStack> ctx) {
        Hologram h = holo(ctx);
        if (h == null) return 0;
        StringBuilder sb = new StringBuilder();
        sb.append(h.name).append(" @ ").append(h.dimension)
                .append(String.format(" (%.1f, %.1f, %.1f)", h.x, h.y, h.z));
        for (int i = 0; i < h.lines.size(); i++) {
            sb.append("\n  ").append(i + 1).append(". ").append(h.lines.get(i).describe());
        }
        if (h.hasClickAction()) {
            sb.append("\n  click:");
            if (h.clickMessage != null) sb.append(" message");
            if (h.clickSound != null) sb.append(" sound=").append(h.clickSound);
            if (!h.clickCommands.isEmpty()) sb.append(" commands=").append(h.clickCommands.size());
        }
        return success(ctx, sb.toString());
    }

    private static int moveHere(CommandContext<CommandSourceStack> ctx) {
        Hologram h = holo(ctx);
        if (h == null) return 0;
        CommandSourceStack src = ctx.getSource();
        Vec3 pos = src.getPosition();
        h.dimension = src.getLevel().dimension().identifier().toString();
        h.x = pos.x;
        h.y = pos.y + 1.8;
        h.z = pos.z;
        h.yaw = src.getRotation().y;
        return afterEdit(ctx, h, "Moved hologram '" + h.name + "' here.");
    }

    private static int teleport(CommandContext<CommandSourceStack> ctx) {
        Hologram h = holo(ctx);
        if (h == null) return 0;
        try {
            ctx.getSource().getPlayerOrException().teleportTo(h.x, h.y, h.z);
            return success(ctx, "Teleported to '" + h.name + "'.");
        } catch (Exception e) {
            return fail(ctx, "Must be run by a player.");
        }
    }

    private static int addLine(CommandContext<CommandSourceStack> ctx) {
        Hologram h = holo(ctx);
        if (h == null) return 0;
        h.lines.add(HoloLine.text(StringArgumentType.getString(ctx, "text")));
        return afterEdit(ctx, h, "Added line to '" + h.name + "'.");
    }

    private static int setLine(CommandContext<CommandSourceStack> ctx) {
        Hologram h = holo(ctx);
        if (h == null) return 0;
        int idx = IntegerArgumentType.getInteger(ctx, "index") - 1;
        if (idx < 0 || idx >= h.lines.size()) return fail(ctx, "No line " + (idx + 1) + ".");
        h.lines.set(idx, HoloLine.text(StringArgumentType.getString(ctx, "text")));
        return afterEdit(ctx, h, "Updated line " + (idx + 1) + ".");
    }

    private static int insertLine(CommandContext<CommandSourceStack> ctx) {
        Hologram h = holo(ctx);
        if (h == null) return 0;
        int idx = IntegerArgumentType.getInteger(ctx, "index") - 1;
        if (idx < 0 || idx > h.lines.size()) return fail(ctx, "Index out of range.");
        h.lines.add(idx, HoloLine.text(StringArgumentType.getString(ctx, "text")));
        return afterEdit(ctx, h, "Inserted line at " + (idx + 1) + ".");
    }

    private static int removeLine(CommandContext<CommandSourceStack> ctx) {
        Hologram h = holo(ctx);
        if (h == null) return 0;
        int idx = IntegerArgumentType.getInteger(ctx, "index") - 1;
        if (idx < 0 || idx >= h.lines.size()) return fail(ctx, "No line " + (idx + 1) + ".");
        if (h.lines.size() == 1) return fail(ctx, "A hologram must keep at least one line; use remove.");
        h.lines.remove(idx);
        return afterEdit(ctx, h, "Removed line " + (idx + 1) + ".");
    }

    private static int addItem(CommandContext<CommandSourceStack> ctx) {
        Hologram h = holo(ctx);
        if (h == null) return 0;
        String id = StringArgumentType.getString(ctx, "item");
        Identifier rl = Identifier.tryParse(id);
        if (rl == null || BuiltInRegistries.ITEM.getOptional(rl).isEmpty()) {
            return fail(ctx, "Unknown item '" + id + "'.");
        }
        h.lines.add(HoloLine.item(rl.toString()));
        return afterEdit(ctx, h, "Added item line to '" + h.name + "'.");
    }

    private static int addBlock(CommandContext<CommandSourceStack> ctx) {
        Hologram h = holo(ctx);
        if (h == null) return 0;
        String id = StringArgumentType.getString(ctx, "block");
        Identifier rl = Identifier.tryParse(id);
        if (rl == null || BuiltInRegistries.BLOCK.getOptional(rl).isEmpty()) {
            return fail(ctx, "Unknown block '" + id + "'.");
        }
        h.lines.add(HoloLine.block(rl.toString()));
        return afterEdit(ctx, h, "Added block line to '" + h.name + "'.");
    }

    private static int addClickCommand(CommandContext<CommandSourceStack> ctx) {
        Hologram h = holo(ctx);
        if (h == null) return 0;
        h.clickCommands.add(StringArgumentType.getString(ctx, "command"));
        return afterEdit(ctx, h, "Added click command to '" + h.name + "'.");
    }

    private static int setClickMessage(CommandContext<CommandSourceStack> ctx) {
        Hologram h = holo(ctx);
        if (h == null) return 0;
        h.clickMessage = StringArgumentType.getString(ctx, "message");
        return afterEdit(ctx, h, "Set click message on '" + h.name + "'.");
    }

    private static int setClickSound(CommandContext<CommandSourceStack> ctx) {
        Hologram h = holo(ctx);
        if (h == null) return 0;
        String id = StringArgumentType.getString(ctx, "sound");
        Identifier rl = Identifier.tryParse(id);
        if (rl == null || BuiltInRegistries.SOUND_EVENT.getOptional(rl).isEmpty()) {
            return fail(ctx, "Unknown sound '" + id + "'.");
        }
        h.clickSound = rl.toString();
        return afterEdit(ctx, h, "Set click sound on '" + h.name + "'.");
    }

    private static int clearClick(CommandContext<CommandSourceStack> ctx) {
        Hologram h = holo(ctx);
        if (h == null) return 0;
        h.clickCommands.clear();
        h.clickMessage = null;
        h.clickSound = null;
        return afterEdit(ctx, h, "Cleared click actions on '" + h.name + "'.");
    }

    private static int reload(CommandContext<CommandSourceStack> ctx) {
        int removed = HologramRenderer.despawnAll(Holograms.server);
        for (Hologram h : mgr().all()) HologramRenderer.render(Holograms.server, h);
        return success(ctx, "Reloaded " + mgr().all().size() + " hologram(s) (cleared " + removed + " entities).");
    }

    // ---- helpers ----

    private static Hologram holo(CommandContext<CommandSourceStack> ctx) {
        String name = StringArgumentType.getString(ctx, "name");
        Hologram h = mgr().get(name);
        if (h == null) {
            fail(ctx, "No hologram named '" + name + "'.");
            return null;
        }
        return h;
    }

    private static int afterEdit(CommandContext<CommandSourceStack> ctx, Hologram h, String msg) {
        HologramRenderer.render(Holograms.server, h);
        mgr().save(Holograms.server);
        return success(ctx, msg);
    }

    private static int success(CommandContext<CommandSourceStack> ctx, String msg) {
        ctx.getSource().sendSuccess(() -> Component.literal(msg), false);
        return 1;
    }

    private static int fail(CommandContext<CommandSourceStack> ctx, String msg) {
        ctx.getSource().sendFailure(Component.literal(msg));
        return 0;
    }
}
