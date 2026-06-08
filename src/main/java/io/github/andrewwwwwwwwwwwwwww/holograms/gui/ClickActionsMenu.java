package io.github.andrewwwwwwwwwwwwwww.holograms.gui;

import io.github.andrewwwwwwwwwwwwwww.holograms.Hologram;
import io.github.andrewwwwwwwwwwwwwww.holograms.HologramRenderer;
import io.github.andrewwwwwwwwwwwwwww.holograms.Holograms;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

import java.util.List;

/** Manage a hologram's right-click actions: command list, message, and sound. */
public final class ClickActionsMenu extends ChestMenu {
    private static final int SIZE = 27;
    private static final int MAX_CMDS = 9;
    private static final int ADD_CMD = 18;
    private static final int MESSAGE = 19;
    private static final int SOUND = 20;
    private static final int BACK = 26;

    private final ServerPlayer player;
    private final Hologram holo;
    private final SimpleContainer container;

    public static void open(ServerPlayer player, Hologram holo) {
        player.openMenu(new SimpleMenuProvider(
                (syncId, inv, p) -> new ClickActionsMenu(syncId, inv, (ServerPlayer) p, holo),
                Gui.styled("Click actions", ChatFormatting.DARK_AQUA)));
    }

    private ClickActionsMenu(int syncId, Inventory inv, ServerPlayer player, Hologram holo) {
        super(MenuType.GENERIC_9x3, syncId, inv, new SimpleContainer(SIZE), 3);
        this.player = player;
        this.holo = holo;
        this.container = (SimpleContainer) getContainer();
        populate();
    }

    private void populate() {
        for (int i = 0; i < SIZE; i++) container.setItem(i, ItemStack.EMPTY);
        for (int i = 0; i < holo.clickCommands.size() && i < MAX_CMDS; i++) {
            ItemStack icon = new ItemStack(Items.COMMAND_BLOCK);
            icon.set(DataComponents.CUSTOM_NAME, Gui.styled("/" + strip(holo.clickCommands.get(i)), ChatFormatting.AQUA));
            icon.set(DataComponents.LORE, new ItemLore(List.of(Gui.styled("Right-click to remove", ChatFormatting.RED))));
            container.setItem(i, icon);
        }
        container.setItem(ADD_CMD, Gui.button(Items.COMMAND_BLOCK_MINECART, "Add command",
                "Runs on right-click (as the player,", "with op permission)"));
        container.setItem(MESSAGE, Gui.button(Items.PAPER, "Message: " + (holo.clickMessage == null ? "none" : "set"),
                holo.clickMessage == null ? "" : holo.clickMessage,
                "Left-click: set / change", "Right-click: clear"));
        container.setItem(SOUND, Gui.button(Items.NOTE_BLOCK, "Sound: " + (holo.clickSound == null ? "none" : holo.clickSound),
                "Left-click: set / change", "Right-click: clear"));
        container.setItem(BACK, Gui.button(Items.OAK_DOOR, "Back to editor"));
    }

    private static String strip(String c) {
        return c.startsWith("/") ? c.substring(1) : c;
    }

    @Override
    public void clicked(int slotId, int button, ContainerInput input, Player clicker) {
        if (!(clicker instanceof ServerPlayer)) return;

        if (slotId >= 0 && slotId < MAX_CMDS && slotId < holo.clickCommands.size()) {
            if (button == 1) {
                holo.clickCommands.remove(slotId);
                apply();
                refresh();
            }
            return;
        }

        switch (slotId) {
            case ADD_CMD -> {
                player.closeContainer();
                suggest("/holo addclickcommand " + holo.name + " ");
            }
            case MESSAGE -> {
                if (button == 1) { holo.clickMessage = null; apply(); refresh(); }
                else { player.closeContainer(); suggest("/holo setclickmessage " + holo.name + " "); }
            }
            case SOUND -> {
                if (button == 1) { holo.clickSound = null; apply(); refresh(); }
                else { player.closeContainer(); suggest("/holo setclicksound " + holo.name + " "); }
            }
            case BACK -> HologramEditMenu.open(player, holo);
            default -> { }
        }
    }

    private void suggest(String command) {
        player.sendSystemMessage(Component.literal("✎ Click here to enter it in chat")
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
