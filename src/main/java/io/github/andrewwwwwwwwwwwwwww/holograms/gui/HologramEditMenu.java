package io.github.andrewwwwwwwwwwwwwww.holograms.gui;

import io.github.andrewwwwwwwwwwwwwww.holograms.HoloElement;
import io.github.andrewwwwwwwwwwwwwww.holograms.HoloLine;
import io.github.andrewwwwwwwwwwwwwww.holograms.Hologram;
import io.github.andrewwwwwwwwwwwwwww.holograms.HologramRenderer;
import io.github.andrewwwwwwwwwwwwwww.holograms.Holograms;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

import java.util.List;

/** The main hologram editor: one row per line (reorder / edit / delete) plus a control bar. */
public final class HologramEditMenu extends ChestMenu {
    private static final int SIZE = 54;
    private static final int LINES_PER_PAGE = 4;

    // control-bar slots (rows 4-5)
    private static final int ADD_TEXT = 36;
    private static final int ADD_ITEM = 37;
    private static final int ADD_BLOCK = 38;
    private static final int ADD_ROW = 39;
    private static final int CLICK_ACTIONS = 40;
    private static final int BACKGROUND = 41;
    private static final int PREV = 43;
    private static final int NEXT = 44;
    private static final int SPACING = 49;
    private static final int DONE = 53;

    private final ServerPlayer player;
    private final Hologram holo;
    private final SimpleContainer container;
    private int page;

    public static void open(ServerPlayer player, Hologram holo) {
        player.openMenu(new SimpleMenuProvider(
                (syncId, inv, p) -> new HologramEditMenu(syncId, inv, (ServerPlayer) p, holo),
                Gui.styled("Editing: " + holo.name, ChatFormatting.DARK_AQUA)));
    }

    public static void openRow(ServerPlayer player, Hologram holo, int lineIndex) {
        RowEditMenu.open(player, holo, lineIndex);
    }

    private HologramEditMenu(int syncId, Inventory inv, ServerPlayer player, Hologram holo) {
        super(MenuType.GENERIC_9x6, syncId, inv, new SimpleContainer(SIZE), 6);
        this.player = player;
        this.holo = holo;
        this.container = (SimpleContainer) getContainer();
        populate();
    }

    private void populate() {
        for (int i = 0; i < SIZE; i++) container.setItem(i, ItemStack.EMPTY);

        int start = page * LINES_PER_PAGE;
        for (int r = 0; r < LINES_PER_PAGE; r++) {
            int idx = start + r;
            if (idx >= holo.lines.size()) break;
            HoloLine line = holo.lines.get(idx);
            int base = r * 9;
            if (idx > 0) container.setItem(base, Gui.button(Items.SPECTRAL_ARROW, "Move up",
                    "Line " + (idx + 1) + " ↑"));
            if (idx < holo.lines.size() - 1) container.setItem(base + 1, Gui.button(Items.ARROW, "Move down",
                    "Line " + (idx + 1) + " ↓"));
            container.setItem(base + 3, preview(line, idx));
            container.setItem(base + 5, Gui.button(Items.WRITABLE_BOOK, "Edit",
                    line.isSingleText() ? "Click to retype this line" : "Click to edit this row's items"));
            container.setItem(base + 7, Gui.button(Items.BARRIER, "Delete line",
                    "Removes line " + (idx + 1)));
        }

        container.setItem(ADD_TEXT, Gui.button(Items.PAPER, "Add text line", "Type the text in chat"));
        container.setItem(ADD_ITEM, Gui.button(Items.ITEM_FRAME, "Add item line", "Adds the item in your hand"));
        container.setItem(ADD_BLOCK, Gui.button(Items.GRASS_BLOCK, "Add block line", "Adds the block in your hand"));
        container.setItem(ADD_ROW, Gui.button(Items.CRAFTING_TABLE, "Add row",
                "A horizontal row of items/blocks", "Great for showing recipes"));
        container.setItem(CLICK_ACTIONS, Gui.button(Items.LEVER, "Click actions",
                "Commands / message / sound", "run when players right-click"));
        container.setItem(BACKGROUND, Gui.button(
                holo.textBackground ? Items.STAINED_GLASS.black() : Items.GLASS,
                "Text background: " + (holo.textBackground ? "ON" : "OFF"),
                "Click to toggle the dark", "panel behind text lines"));
        container.setItem(PREV, page > 0 ? Gui.button(Items.STAINED_GLASS_PANE.red(), "Previous page") : ItemStack.EMPTY);
        container.setItem(NEXT, (start + LINES_PER_PAGE) < holo.lines.size()
                ? Gui.button(Items.STAINED_GLASS_PANE.green(), "Next page") : ItemStack.EMPTY);
        container.setItem(SPACING, Gui.button(Items.STRING, String.format("Line spacing: %.2f", holo.lineSpacing),
                "Left-click: +0.04", "Right-click: -0.04"));
        container.setItem(DONE, Gui.button(Items.CONCRETE.lime(), "Done"));
    }

    private ItemStack preview(HoloLine line, int idx) {
        ItemStack icon = line.elements.isEmpty() ? new ItemStack(Items.DYE.lightGray()) : line.elements.get(0).icon();
        icon.set(DataComponents.CUSTOM_NAME, Gui.styled("Line " + (idx + 1) + ": " + line.describe(), ChatFormatting.AQUA));
        icon.set(DataComponents.LORE, new ItemLore(List.of(Gui.styled("Use Edit → to change", ChatFormatting.GRAY))));
        return icon;
    }

    @Override
    public void clicked(int slotId, int button, ContainerInput input, Player clicker) {
        if (!(clicker instanceof ServerPlayer)) return;

        // line rows
        if (slotId >= 0 && slotId < LINES_PER_PAGE * 9) {
            int r = slotId / 9, col = slotId % 9, idx = page * LINES_PER_PAGE + r;
            if (idx >= holo.lines.size()) return;
            switch (col) {
                case 0 -> moveLine(idx, -1);
                case 1 -> moveLine(idx, 1);
                case 5, 3 -> editLine(idx);
                case 7 -> deleteLine(idx);
                default -> { }
            }
            return;
        }

        switch (slotId) {
            case ADD_TEXT -> {
                player.closeContainer();
                suggest("/holo addline " + holo.name + " ");
            }
            case ADD_ITEM -> addFromHand(false);
            case ADD_BLOCK -> addFromHand(true);
            case ADD_ROW -> {
                HoloLine row = new HoloLine();
                holo.lines.add(row);
                apply();
                RowEditMenu.open(player, holo, holo.lines.size() - 1);
            }
            case CLICK_ACTIONS -> ClickActionsMenu.open(player, holo);
            case BACKGROUND -> {
                holo.textBackground = !holo.textBackground;
                apply();
                refresh();
            }
            case PREV -> { if (page > 0) { page--; refresh(); } }
            case NEXT -> { if ((page + 1) * LINES_PER_PAGE < holo.lines.size()) { page++; refresh(); } }
            case SPACING -> {
                holo.lineSpacing = clamp(holo.lineSpacing + (button == 1 ? -0.04 : 0.04));
                apply();
                refresh();
            }
            case DONE -> player.closeContainer();
            default -> { }
        }
    }

    private void editLine(int idx) {
        HoloLine line = holo.lines.get(idx);
        if (line.isSingleText()) {
            player.closeContainer();
            suggest("/holo setline " + holo.name + " " + (idx + 1) + " " + line.elements.get(0).text);
        } else {
            RowEditMenu.open(player, holo, idx);
        }
    }

    private void moveLine(int idx, int dir) {
        int to = idx + dir;
        if (to < 0 || to >= holo.lines.size()) return;
        HoloLine l = holo.lines.remove(idx);
        holo.lines.add(to, l);
        apply();
        refresh();
    }

    private void deleteLine(int idx) {
        if (holo.lines.size() <= 1) {
            player.sendSystemMessage(Gui.styled("A hologram must keep at least one line.", ChatFormatting.RED));
            return;
        }
        holo.lines.remove(idx);
        apply();
        refresh();
    }

    private void addFromHand(boolean block) {
        ItemStack hand = player.getMainHandItem();
        if (hand.isEmpty()) {
            player.sendSystemMessage(Gui.styled("Hold an item in your main hand first.", ChatFormatting.RED));
            return;
        }
        if (block) {
            if (!(hand.getItem() instanceof BlockItem bi)) {
                player.sendSystemMessage(Gui.styled("That item is not a placeable block.", ChatFormatting.RED));
                return;
            }
            Identifier id = BuiltInRegistries.BLOCK.getKey(bi.getBlock());
            holo.lines.add(HoloLine.of(HoloElement.block(id.toString())));
        } else {
            Identifier id = BuiltInRegistries.ITEM.getKey(hand.getItem());
            holo.lines.add(HoloLine.of(HoloElement.item(id.toString())));
        }
        apply();
        refresh();
    }

    private static double clamp(double v) {
        return Math.max(0.0, Math.min(2.0, Math.round(v * 100.0) / 100.0));
    }

    private void suggest(String command) {
        player.sendSystemMessage(Component.literal("✎ Click here to enter text")
                .withStyle(s -> s.withColor(0x55FF55).withItalic(false)
                        .withClickEvent(new ClickEvent.SuggestCommand(command))));
    }

    private void apply() {
        HologramRenderer.render(Holograms.server, holo);
        Holograms.MANAGER.save(Holograms.server);
    }

    private void refresh() {
        populate();
        sendAllDataToRemote();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
