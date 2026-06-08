package io.github.andrewwwwwwwwwwwwwww.holograms;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

/**
 * Parses hologram text with legacy {@code &} colour/format codes and {@code &#rrggbb} hex codes into
 * a rich {@link Component}. Vanilla clients render the resulting component natively on a text_display.
 *
 * <p>Supported: {@code &0}-{@code &9}/{@code &a}-{@code &f} colours, {@code &k}-{@code &o} formats,
 * {@code &r} reset, and {@code &#rrggbb} 24-bit hex colours. Both {@code &} and {@code §} are accepted
 * as the escape character.
 */
public final class Colors {
    private Colors() {}

    public static Component parse(String raw) {
        MutableComponent root = Component.empty();
        StringBuilder buf = new StringBuilder();
        Style style = Style.EMPTY;
        int n = raw.length();
        for (int i = 0; i < n; i++) {
            char c = raw.charAt(i);
            if ((c == '&' || c == '§') && i + 1 < n) {
                char next = raw.charAt(i + 1);
                if (next == '#' && i + 7 < n) {
                    Integer rgb = parseHex(raw, i + 2);
                    if (rgb != null) {
                        flush(root, buf, style);
                        style = style.withColor(TextColor.fromRgb(rgb));
                        i += 7; // skip '#rrggbb'
                        continue;
                    }
                }
                ChatFormatting fmt = ChatFormatting.getByCode(Character.toLowerCase(next));
                if (fmt != null) {
                    flush(root, buf, style);
                    if (fmt == ChatFormatting.RESET) {
                        style = Style.EMPTY;
                    } else if (fmt.isColor()) {
                        style = Style.EMPTY.withColor(fmt); // a colour code resets active formats
                    } else {
                        style = style.applyFormat(fmt);
                    }
                    i++; // skip the code char
                    continue;
                }
            }
            buf.append(c);
        }
        flush(root, buf, style);
        return root;
    }

    private static Integer parseHex(String s, int start) {
        int v = 0;
        for (int k = 0; k < 6; k++) {
            int d = Character.digit(s.charAt(start + k), 16);
            if (d < 0) return null;
            v = (v << 4) | d;
        }
        return v;
    }

    private static void flush(MutableComponent root, StringBuilder buf, Style style) {
        if (buf.isEmpty()) return;
        root.append(Component.literal(buf.toString()).setStyle(style));
        buf.setLength(0);
    }
}
