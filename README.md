# Holograms

Server-side holograms for Minecraft **26.1.2** (Fabric). Vanilla clients need nothing installed —
holograms are real native **display entities** (`text_display`, `item_display`, `block_display`),
so they render natively and persist with their chunks.

Inspired by *MasuHolograms* (a Forge 1.18.2 mod that faked holograms with packet-level armor stands).
This is a clean reimplementation for modern Minecraft using the display entities added in 1.19.4.

## Features
- **Multi-line** holograms stacking text, item icons, and block models.
- **Rows** — multiple item/block icons on a single line, laid out side-by-side. Great for showing
  crafting recipes / required materials.
- **Chest-GUI editor** (`/holo edit <name>`) — reorder, edit, and delete lines visually; add lines
  from the item/block in your hand; manage click actions; tune spacing. No command-typing for layout.
- **Text formatting**: legacy `&` colour/format codes and `&#rrggbb` 24-bit hex colours.
- **Click actions** (right-click): run command(s) (elevated, as the clicking player), send a message,
  and/or play a sound. Backed by an invisible `interaction` entity covering the hologram.
- Definitions persist in `<world>/holograms/holograms.dat`.

## The editor (recommended)
`/holo edit <name>` opens a chest menu:
- **One row per line** with ▲/▼ to reorder, an Edit button (retype text, or open the row editor), and
  a Delete button.
- **Control bar**: Add Text / Add Item (from hand) / Add Block (from hand) / Add Row / Click Actions /
  page ◀▶ / Line spacing (left-click +, right-click −) / Done.
- **Row editor**: add the item or block in your hand to build a horizontal strip; right-click an icon
  to remove it. Build a recipe by holding each ingredient and clicking "Add".
- **Click actions menu**: add/remove commands, set/clear the message, set/clear the sound.

Text is still typed in chat (a clickable prompt pre-fills the command for you).

## Commands
`/holograms` (aliases `/holo`, `/hd`) — requires op level 2.

| Command | Description |
|---|---|
| `create <name> <text…>` | Create a hologram at your position |
| `edit <name>` | Open the chest-GUI editor |
| `remove <name>` | Delete a hologram |
| `list` / `info <name>` | List all / show details (lines + click actions, indexed) |
| `movehere <name>` | Move a hologram to your position |
| `teleport <name>` | Teleport yourself to a hologram |
| `spacing <name> <value>` | Set the gap between lines (0–2) |
| `addline <name> <text…>` | Append a text line |
| `setline <name> <i> <text…>` | Replace line *i* |
| `insertline <name> <i> <text…>` | Insert a text line at *i* |
| `removeline <name> <i>` | Remove line *i* |
| `moveline <name> <i> up\|down` | Reorder a line |
| `additem <name> <item>` | Append a floating item icon (tab-completes) |
| `addblock <name> <block>` | Append a floating block model (tab-completes) |
| `additemhand <name>` | Append the item in your hand |
| `addrow <name>` | Add a row and open the row editor |
| `addclickcommand <name> <cmd…>` | Add a command run on click |
| `removeclickcommand <name> <i>` | Remove click command *i* |
| `setclickmessage <name> <msg…>` / `clearclickmessage <name>` | Set / clear the click message |
| `setclicksound <name> <soundId>` | Set a sound played on click (tab-completes) |
| `clearclick <name>` | Remove all click actions |
| `reload` | Clear and respawn all hologram entities |

Lines are 1-indexed. Use `reload` if entities ever get out of sync with definitions
(e.g. after editing while the hologram's chunk was unloaded).

## Build
`$env:JAVA_HOME="C:\Program Files\Microsoft\jdk-25.0.3.9-hotspot"; .\gradlew.bat build`
