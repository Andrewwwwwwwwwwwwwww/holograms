# Holograms

Server-side holograms for Minecraft **26.1.2** (Fabric). Vanilla clients need nothing installed —
holograms are real native **display entities** (`text_display`, `item_display`, `block_display`),
so they render natively and persist with their chunks.

Inspired by *MasuHolograms* (a Forge 1.18.2 mod that faked holograms with packet-level armor stands).
This is a clean reimplementation for modern Minecraft using the display entities added in 1.19.4.

## Features
- **Multi-line** holograms stacking text, item icons, and block models.
- **Text formatting**: legacy `&` colour/format codes and `&#rrggbb` 24-bit hex colours.
- **Click actions** (right-click): run command(s) (elevated, as the clicking player), send a message,
  and/or play a sound. Backed by an invisible `interaction` entity covering the hologram.
- Definitions persist in `<world>/holograms/holograms.dat`.

## Commands
`/holograms` (aliases `/holo`, `/hd`) — requires op level 2.

| Command | Description |
|---|---|
| `create <name> <text…>` | Create a hologram at your position |
| `remove <name>` | Delete a hologram |
| `list` / `info <name>` | List all / show details of one |
| `movehere <name>` | Move a hologram to your position |
| `teleport <name>` | Teleport yourself to a hologram |
| `addline <name> <text…>` | Append a text line |
| `setline <name> <i> <text…>` | Replace line *i* |
| `insertline <name> <i> <text…>` | Insert a text line at *i* |
| `removeline <name> <i>` | Remove line *i* |
| `additem <name> <itemId>` | Append a floating item icon |
| `addblock <name> <blockId>` | Append a floating block model |
| `setclickcommand <name> <cmd…>` | Add a command run on click |
| `setclickmessage <name> <msg…>` | Set a message shown on click |
| `setclicksound <name> <soundId>` | Set a sound played on click |
| `clearclick <name>` | Remove all click actions |
| `reload` | Clear and respawn all hologram entities |

Lines are 1-indexed. Use the `reload` command if entities ever get out of sync with definitions
(e.g. after editing while the hologram's chunk was unloaded).

## Build
`$env:JAVA_HOME="C:\Program Files\Microsoft\jdk-25.0.3.9-hotspot"; .\gradlew.bat build`
