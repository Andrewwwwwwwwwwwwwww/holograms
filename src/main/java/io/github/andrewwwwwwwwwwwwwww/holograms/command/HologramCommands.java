package io.github.andrewwwwwwwwwwwwwww.holograms.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.github.andrewwwwwwwwwwwwwww.holograms.HoloElement;
import io.github.andrewwwwwwwwwwwwwww.holograms.HoloLine;
import io.github.andrewwwwwwwwwwwwwww.holograms.Hologram;
import io.github.andrewwwwwwwwwwwwwww.holograms.HologramManager;
import io.github.andrewwwwwwwwwwwwwww.holograms.HologramRenderer;
import io.github.andrewwwwwwwwwwwwwww.holograms.Holograms;
import io.github.andrewwwwwwwwwwwwwww.holograms.gui.HologramEditMenu;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.blocks.BlockStateArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

import java.util.regex.Pattern;

/** Registers {@code /holograms} (aliases {@code /holo}, {@code /hd}). All editing requires op level 2. */
public final class HologramCommands {
    private static final Pattern VALID_NAME = Pattern.compile("[A-Za-z0-9_-]{1,32}");

    private static final SuggestionProvider<CommandSourceStack> HOLO_NAMES = (ctx, b) ->
            SharedSuggestionProvider.suggest(Holograms.MANAGER.names(), b);
    private static final SuggestionProvider<CommandSourceStack> SOUNDS = (ctx, b) ->
            SharedSuggestionProvider.suggest(
                    BuiltInRegistries.SOUND_EVENT.keySet().stream().map(Identifier::toString), b);

    private HologramCommands() {}

    private static HologramManager mgr() {
        return Holograms.MANAGER;
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext ctx) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("holograms")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));

        root.then(Commands.literal("create")
                .then(Commands.argument("name", StringArgumentType.word())
                        .then(Commands.argument("text", StringArgumentType.greedyString())
                                .executes(HologramCommands::create))));

        root.then(Commands.literal("edit").then(named().executes(HologramCommands::edit)));
        root.then(Commands.literal("remove").then(named().executes(HologramCommands::remove)));
        root.then(Commands.literal("list").executes(HologramCommands::list));
        root.then(Commands.literal("info").then(named().executes(HologramCommands::info)));
        root.then(Commands.literal("movehere").then(named().executes(HologramCommands::moveHere)));
        root.then(Commands.literal("teleport").then(named().executes(HologramCommands::teleport)));

        root.then(Commands.literal("spacing").then(named()
                .then(Commands.argument("value", com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg(0.0, 2.0))
                        .executes(HologramCommands::spacing))));

        root.then(Commands.literal("addline").then(named()
                .then(Commands.argument("text", StringArgumentType.greedyString()).executes(HologramCommands::addLine))));
        root.then(Commands.literal("setline").then(named()
                .then(Commands.argument("index", IntegerArgumentType.integer(1))
                        .then(Commands.argument("text", StringArgumentType.greedyString()).executes(HologramCommands::setLine)))));
        root.then(Commands.literal("insertline").then(named()
                .then(Commands.argument("index", IntegerArgumentType.integer(1))
                        .then(Commands.argument("text", StringArgumentType.greedyString()).executes(HologramCommands::insertLine)))));
        root.then(Commands.literal("removeline").then(named()
                .then(Commands.argument("index", IntegerArgumentType.integer(1)).executes(HologramCommands::removeLine))));
        root.then(Commands.literal("moveline").then(named()
                .then(Commands.argument("index", IntegerArgumentType.integer(1))
                        .then(Commands.literal("up").executes(c -> moveLine(c, -1)))
                        .then(Commands.literal("down").executes(c -> moveLine(c, 1))))));

        root.then(Commands.literal("additem").then(named()
                .then(Commands.argument("item", ItemArgument.item(ctx)).executes(HologramCommands::addItem))));
        root.then(Commands.literal("addblock").then(named()
                .then(Commands.argument("block", BlockStateArgument.block(ctx)).executes(HologramCommands::addBlock))));
        root.then(Commands.literal("additemhand").then(named().executes(HologramCommands::addItemHand)));
        root.then(Commands.literal("addrow").then(named().executes(HologramCommands::addRow)));

        root.then(Commands.literal("addclickcommand").then(named()
                .then(Commands.argument("command", StringArgumentType.greedyString()).executes(HologramCommands::addClickCommand))));
        root.then(Commands.literal("removeclickcommand").then(named()
                .then(Commands.argument("index", IntegerArgumentType.integer(1)).executes(HologramCommands::removeClickCommand))));
        root.then(Commands.literal("setclickmessage").then(named()
                .then(Commands.argument("message", StringArgumentType.greedyString()).executes(HologramCommands::setClickMessage))));
        root.then(Commands.literal("clearclickmessage").then(named().executes(HologramCommands::clearClickMessage)));
        root.then(Commands.literal("setclicksound").then(named()
                .then(Commands.argument("sound", StringArgumentType.string()).suggests(SOUNDS).executes(HologramCommands::setClickSound))));
        root.then(Commands.literal("clearclick").then(named().executes(HologramCommands::clearClick)));
        root.then(Commands.literal("reload").executes(HologramCommands::reload));

        LiteralCommandNode<CommandSourceStack> node = dispatcher.register(root);
        dispatcher.register(Commands.literal("holo")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).redirect(node));
        dispatcher.register(Commands.literal("hd")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).redirect(node));
    }

    private static RequiredArgumentBuilder<CommandSourceStack, String> named() {
        return Commands.argument("name", StringArgumentType.word()).suggests(HOLO_NAMES);
    }

    // ---- handlers ----

    private static int create(CommandContext<CommandSourceStack> ctx) {
        String name = StringArgumentType.getString(ctx, "name");
        if (!VALID_NAME.matcher(name).matches()) return fail(ctx, "Invalid name. Use letters, digits, _ or - (max 32).");
        if (mgr().exists(name)) return fail(ctx, "A hologram named '" + name + "' already exists.");
        CommandSourceStack src = ctx.getSource();
        ServerLevel level = src.getLevel();
        Vec3 pos = src.getPosition();
        Hologram h = mgr().create(name);
        h.dimension = level.dimension().identifier().toString();
        h.x = pos.x;
        h.y = pos.y + 1.8;
        h.z = pos.z;
        h.yaw = src.getRotation().y;
        h.lines.add(HoloLine.text(StringArgumentType.getString(ctx, "text")));
        return afterEdit(ctx, h, "Created hologram '" + name + "'. Use /holo edit " + name + " for the editor.");
    }

    private static int edit(CommandContext<CommandSourceStack> ctx) {
        Hologram h = holo(ctx);
        if (h == null) return 0;
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer sp)) return fail(ctx, "Must be run by a player.");
        HologramEditMenu.open(sp, h);
        return 1;
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
                .append(String.format(" (%.1f, %.1f, %.1f) spacing=%.2f", h.x, h.y, h.z, h.lineSpacing));
        for (int i = 0; i < h.lines.size(); i++) sb.append("\n  ").append(i + 1).append(". ").append(h.lines.get(i).describe());
        if (h.hasClickAction()) {
            sb.append("\n  click:");
            if (h.clickMessage != null) sb.append("\n    message: ").append(h.clickMessage);
            if (h.clickSound != null) sb.append("\n    sound: ").append(h.clickSound);
            for (int i = 0; i < h.clickCommands.size(); i++) sb.append("\n    cmd ").append(i + 1).append(": ").append(h.clickCommands.get(i));
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
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer sp)) return fail(ctx, "Must be run by a player.");
        sp.teleportTo(h.x, h.y, h.z);
        return success(ctx, "Teleported to '" + h.name + "'.");
    }

    private static int spacing(CommandContext<CommandSourceStack> ctx) {
        Hologram h = holo(ctx);
        if (h == null) return 0;
        h.lineSpacing = com.mojang.brigadier.arguments.DoubleArgumentType.getDouble(ctx, "value");
        return afterEdit(ctx, h, String.format("Set line spacing to %.2f.", h.lineSpacing));
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

    private static int moveLine(CommandContext<CommandSourceStack> ctx, int dir) {
        Hologram h = holo(ctx);
        if (h == null) return 0;
        int idx = IntegerArgumentType.getInteger(ctx, "index") - 1;
        int to = idx + dir;
        if (idx < 0 || idx >= h.lines.size() || to < 0 || to >= h.lines.size()) return fail(ctx, "Can't move that line.");
        HoloLine l = h.lines.remove(idx);
        h.lines.add(to, l);
        return afterEdit(ctx, h, "Moved line " + (idx + 1) + " to " + (to + 1) + ".");
    }

    private static int addItem(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Hologram h = holo(ctx);
        if (h == null) return 0;
        Identifier id = BuiltInRegistries.ITEM.getKey(ItemArgument.getItem(ctx, "item").item().value());
        h.lines.add(HoloLine.of(HoloElement.item(id.toString())));
        return afterEdit(ctx, h, "Added item line to '" + h.name + "'.");
    }

    private static int addBlock(CommandContext<CommandSourceStack> ctx) {
        Hologram h = holo(ctx);
        if (h == null) return 0;
        Block block = BlockStateArgument.getBlock(ctx, "block").getState().getBlock();
        Identifier id = BuiltInRegistries.BLOCK.getKey(block);
        h.lines.add(HoloLine.of(HoloElement.block(id.toString())));
        return afterEdit(ctx, h, "Added block line to '" + h.name + "'.");
    }

    private static int addItemHand(CommandContext<CommandSourceStack> ctx) {
        Hologram h = holo(ctx);
        if (h == null) return 0;
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer sp)) return fail(ctx, "Must be run by a player.");
        ItemStack stack = sp.getMainHandItem();
        if (stack.isEmpty()) return fail(ctx, "Hold an item in your main hand first.");
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        h.lines.add(HoloLine.of(HoloElement.item(id.toString())));
        return afterEdit(ctx, h, "Added held item to '" + h.name + "'.");
    }

    private static int addRow(CommandContext<CommandSourceStack> ctx) {
        Hologram h = holo(ctx);
        if (h == null) return 0;
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer sp)) return fail(ctx, "Must be run by a player.");
        HoloLine row = new HoloLine();
        h.lines.add(row);
        HologramRenderer.render(Holograms.server, h);
        mgr().save(Holograms.server);
        HologramEditMenu.openRow(sp, h, h.lines.size() - 1);
        return 1;
    }

    private static int addClickCommand(CommandContext<CommandSourceStack> ctx) {
        Hologram h = holo(ctx);
        if (h == null) return 0;
        h.clickCommands.add(StringArgumentType.getString(ctx, "command"));
        return afterEdit(ctx, h, "Added click command to '" + h.name + "'.");
    }

    private static int removeClickCommand(CommandContext<CommandSourceStack> ctx) {
        Hologram h = holo(ctx);
        if (h == null) return 0;
        int idx = IntegerArgumentType.getInteger(ctx, "index") - 1;
        if (idx < 0 || idx >= h.clickCommands.size()) return fail(ctx, "No click command " + (idx + 1) + ".");
        h.clickCommands.remove(idx);
        return afterEdit(ctx, h, "Removed click command " + (idx + 1) + ".");
    }

    private static int setClickMessage(CommandContext<CommandSourceStack> ctx) {
        Hologram h = holo(ctx);
        if (h == null) return 0;
        h.clickMessage = StringArgumentType.getString(ctx, "message");
        return afterEdit(ctx, h, "Set click message on '" + h.name + "'.");
    }

    private static int clearClickMessage(CommandContext<CommandSourceStack> ctx) {
        Hologram h = holo(ctx);
        if (h == null) return 0;
        h.clickMessage = null;
        return afterEdit(ctx, h, "Cleared click message on '" + h.name + "'.");
    }

    private static int setClickSound(CommandContext<CommandSourceStack> ctx) {
        Hologram h = holo(ctx);
        if (h == null) return 0;
        String id = StringArgumentType.getString(ctx, "sound");
        Identifier rl = Identifier.tryParse(id);
        if (rl == null || BuiltInRegistries.SOUND_EVENT.getOptional(rl).isEmpty()) return fail(ctx, "Unknown sound '" + id + "'.");
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
