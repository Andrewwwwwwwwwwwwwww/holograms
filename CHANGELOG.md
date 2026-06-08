# Changelog

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
