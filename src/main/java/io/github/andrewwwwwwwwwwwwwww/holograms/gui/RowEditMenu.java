package io.github.andrewwwwwwwwwwwwwww.holograms.gui;

import io.github.andrewwwwwwwwwwwwwww.holograms.HoloElement;
import io.github.andrewwwwwwwwwwwwwww.holograms.HoloLine;
import io.github.andrewwwwwwwwwwwwwww.holograms.Hologram;
import io.github.andrewwwwwwwwwwwwwww.holograms.HologramRenderer;
import io.github.andrewwwwwwwwwwwwwww.holograms.Holograms;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
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

/** Edits a single "row" line: a horizontal strip of item/block icons (e.g. a crafting recipe). */
public final class RowEditMenu extends ChestMenu {
    private static final int SIZE = 27;
    private static final int MAX_ELEMENTS = 9;
    private static final int ADD_ITEM = 18;
    private static final int ADD_BLOCK = 19;
    private static final int BACK = 26;

    private final ServerPlayer player;
    private final Hologram holo;
    private final int lineIndex;
    private final SimpleContainer container;

    public static void open(ServerPlayer player, Hologram holo, int lineIndex) {
        player.openMenu(new SimpleMenuProvider(
                (syncId, inv, p) -> new RowEditMenu(syncId, inv, (ServerPlayer) p, holo, lineIndex),
                Gui.styled("Row editor", ChatFormatting.DARK_AQUA)));
    }

    private RowEditMenu(int syncId, Inventory inv, ServerPlayer player, Hologram holo, int lineIndex) {
        super(MenuType.GENERIC_9x3, syncId, inv, new SimpleContainer(SIZE), 3);
        this.player = player;
        this.holo = holo;
        this.lineIndex = lineIndex;
        this.container = (SimpleContainer) getContainer();
        populate();
    }

    private HoloLine line() {
        return holo.lines.get(lineIndex);
    }

    private void populate() {
        for (int i = 0; i < SIZE; i++) container.setItem(i, ItemStack.EMPTY);
        HoloLine line = line();
        for (int i = 0; i < line.elements.size() && i < MAX_ELEMENTS; i++) {
            HoloElement el = line.elements.get(i);
            ItemStack icon = el.icon();
            icon.set(DataComponents.CUSTOM_NAME, Gui.styled((i + 1) + ". " + el.describe(), ChatFormatting.AQUA));
            icon.set(DataComponents.LORE, new ItemLore(List.of(Gui.styled("Right-click to remove", ChatFormatting.RED))));
            container.setItem(i, icon);
        }
        container.setItem(ADD_ITEM, Gui.button(Items.ITEM_FRAME, "Add item", "Adds the item in your hand"));
        container.setItem(ADD_BLOCK, Gui.button(Items.GRASS_BLOCK, "Add block", "Adds the block in your hand"));
        container.setItem(BACK, Gui.button(Items.OAK_DOOR, "Back to editor"));
    }

    @Override
    public void clicked(int slotId, int button, ContainerInput input, Player clicker) {
        if (!(clicker instanceof ServerPlayer)) return;
        HoloLine line = line();

        if (slotId >= 0 && slotId < MAX_ELEMENTS && slotId < line.elements.size()) {
            if (button == 1) { // right-click removes
                line.elements.remove(slotId);
                apply();
                refresh();
            }
            return;
        }

        switch (slotId) {
            case ADD_ITEM -> addFromHand(false);
            case ADD_BLOCK -> addFromHand(true);
            case BACK -> HologramEditMenu.open(player, holo);
            default -> { }
        }
    }

    private void addFromHand(boolean block) {
        if (line().elements.size() >= MAX_ELEMENTS) {
            player.sendSystemMessage(Gui.styled("This row is full (max " + MAX_ELEMENTS + ").", ChatFormatting.RED));
            return;
        }
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
            line().elements.add(HoloElement.block(id.toString()));
        } else {
            Identifier id = BuiltInRegistries.ITEM.getKey(hand.getItem());
            line().elements.add(HoloElement.item(id.toString()));
        }
        apply();
        refresh();
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
