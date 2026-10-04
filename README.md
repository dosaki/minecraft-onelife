# One Life

A Paper plugin that replaces death drops with a gravestone.

- When you die, a gravestone appears where you died. It holds your items, your XP, how you died and the last
  thing you said in chat, shown as floating text above it. The grave is named after you and your death count
  ("Steve, the Third").
- Each life you choose **One Life** or not (asked when you first join and every time you respawn):
  - **One Life:** your grave is sealed forever. Anyone can look inside; nobody can take anything out.
  - **Not One Life:** anyone can loot the grave like a chest. Emptied, it stays as a memorial.
- `/onelife` shows your mode. `/onelife on` turns One Life on at any time; only death turns it off.
- Gravestones break after 3 hits and drop as an item that keeps everything inside, like a shulker box. The first
  time a grave is ever broken, whoever breaks it gets half the stored XP.
- If a gravestone item burns or falls in lava, it's gone for good.
- Void deaths and `keep_inventory` deaths don't make graves.

## Requirements

- Paper 26.3
- [CraftEngine](https://modrinth.com/plugin/craftengine) 26.9 or newer, with its resource pack delivered to
  players (the plugin adds the gravestone model to CraftEngine's pack and runs `/ce reload pack` on startup).

## Configuration (`plugins/OneLife/config.yml`)

| Key | Default | Meaning |
|---|---|---|
| `size-cap-bytes` | `524288` | Largest encoded size of a grave's items; over it, shulker boxes spill on the ground first |
| `search-radius` | `3` | How far to look for a free space for a grave |
| `xp-fraction` | `0.5` | Share of the stored XP given on the first break |
| `one-life.required` | `false` | `true`: every player is always in One Life and is never asked (read at startup; stored choices are kept) |
| `regenerate-pack-on-start` | `true` | Run `/ce reload pack` once after startup |

## Building

`./gradlew build` (downloads JDK 25 if needed). `./gradlew runServer` starts a local test server with CraftEngine.

## Releasing

Push a tag `vX.Y.Z`. The release workflow attaches `onelife-X.Y.Z.jar` to a GitHub Release with its sha256.

## Licence

GPL-3.0.
