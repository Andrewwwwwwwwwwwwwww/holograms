# Changelog

## 0.2.1 - 2026-06-07
- **Text background toggle** — text lines no longer force the translucent dark panel (which looked bad
  in front of glass/builds). Background now defaults to **off**; toggle per hologram with
  `/holo background <name> on|off` or the "Text background" button in the editor. Run `/holo reload`
  (or re-edit) to apply to holograms made before this update.
- Editor now shows 4 lines per page with a roomier two-row control bar (added the background button).

## 0.2.0 - 2026-06-07
- **Chest-GUI editor** — `/holo edit <name>` opens a visual editor: one row per line with move
  up/down, edit, and delete buttons; a control bar to add text/item/block/row lines, manage click
  actions, page through lines, tune line spacing, and finish. Sub-menus for editing a row and for
  click actions.
- **Rows (multiple items/blocks on one line)** — new "row" line type laying several item/block icons
  side-by-side, ideal for showing crafting recipes. Build rows from items in your hand via the GUI.
- **Add from hand** — `/holo additemhand` and GUI buttons add the item/block you're holding.
- **Tab-completion** for `/holo additem` and `/holo addblock` (now real item/block arguments), and for
  `/holo setclicksound`.
- **Manage click actions** — `/holo addclickcommand`, `/holo removeclickcommand <i>`,
  `/holo clearclickmessage`; `/holo info` now lists click commands with indices.
- **Move/space lines** — `/holo moveline <i> up|down` and `/holo spacing <name> <value>` (also in GUI).
- **Fixes**: line overlap (text ending up inside item/block lines) — each line now gets its own
  vertical slot sized to its tallest element, with a tunable gap; click sound now sent directly to the
  clicking player so it reliably plays; block displays face the viewer (CENTER billboard) instead of
  the odd default angle.

## 0.1.0 - 2026-06-07
- Initial release. Server-side holograms built on native display entities (text_display,
  item_display, block_display) — vanilla clients render them with nothing installed.
- Multi-line holograms: text lines (with `&` colour codes and `&#rrggbb` hex), item icons, block models.
- Click actions via an interaction entity: run command(s) (elevated, as the clicking player),
  send a message, and/or play a sound on right-click.
- Commands `/holograms` (aliases `/holo`, `/hd`), op level 2:
  `create`, `remove`, `list`, `info`, `movehere`, `teleport`, `addline`, `setline`, `insertline`,
  `removeline`, `additem`, `addblock`, `setclickcommand`, `setclickmessage`, `setclicksound`,
  `clearclick`, `reload`.
- Definitions persist in `<world>/holograms/holograms.dat`; the display entities themselves persist
  with their chunks.
