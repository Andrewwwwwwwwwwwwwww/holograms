package io.github.andrewwwwwwwwwwwwwww.holograms.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;

/** Small shared helpers for the editor menus (button icons + styled text). */
final class Gui {
    private Gui() {}

    static ItemStack button(Item item, String name, String... lore) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME, styled(name, ChatFormatting.YELLOW));
        if (lore.length > 0) {
            List<Component> lines = new ArrayList<>();
            for (String l : lore) lines.add(styled(l, ChatFormatting.GRAY));
            stack.set(DataComponents.LORE, new ItemLore(lines));
        }
        return stack;
    }

    static ItemStack labelled(ItemStack stack, String name, String... lore) {
        stack.set(DataComponents.CUSTOM_NAME, styled(name, ChatFormatting.AQUA));
        if (lore.length > 0) {
            List<Component> lines = new ArrayList<>();
            for (String l : lore) lines.add(styled(l, ChatFormatting.GRAY));
            stack.set(DataComponents.LORE, new ItemLore(lines));
        }
        return stack;
    }

    static MutableComponent styled(String text, ChatFormatting color) {
        return Component.literal(text).withStyle(color).withStyle(s -> s.withItalic(false));
    }
}
