# One Life gravestones — design

Date: 2026-10-03
Status: Draft, awaiting review

## 1. Goal

A Paper plugin for the family server ([`dosaki/minecraft-server`](https://github.com/dosaki/minecraft-server)) that
replaces death drops with a **gravestone**: a small 3D memorial block that holds the dead player's items, XP,
cause of death and last chat message.

Every player chooses, per life, whether they are in **One Life** mode:

- **One Life on** — the grave is sealed forever. Anyone can look inside; nobody can take anything out.
- **One Life off** — the grave is lootable by anyone, like a chest. Emptied, it stays as a memorial.

In both modes the gravestone can be broken and carried like a shulker box, keeping everything inside, and the
first time it is ever broken, whoever breaks it gets half of the stored XP.

### Success criteria

- Dying (anywhere except the void) never scatters items on the ground (except the size-cap spill, §6.3); a gravestone appears instead.
- A One Life grave's items can never leave it, by any click, drag, hotkey or hopper.
- A grave carried as an item keeps every stored detail through any number of break → place cycles.
- Stored XP is paid out exactly once per grave, ever.
- Players see a proper gravestone model with no client mods (server resource pack only).

### Fixed decisions

| Decision | Choice |
|---|---|
| Platform | Paper 26.3, Java 25, vanilla clients |
| Custom content | [CraftEngine](https://modrinth.com/plugin/craftengine) **furniture** (not a custom block) |
| Model | Simple JSON model made for this project (headstone on a base), 16×16 stone texture |
| Resource pack | Built by CraftEngine and served by the server itself on the game port (§7), **required** |
| Licence | GPL-3.0 (CraftEngine is GPL-3.0 and we link against its API) |
| Build | Gradle (Kotlin DSL), JUnit 5, MockBukkit where it supports 26.3 |
| Distribution | Jar attached to a GitHub Release, pinned by URL + sha256 in the server repo's `versions.json` |

## 2. Behaviour

### 2.1 Choosing One Life

- A player's mode is stored on the player (persistent data), so it survives logouts and restarts.
- A **dialog** (Paper's native dialog screen) asks "Play this life as One Life?" with **Yes** / **No**:
  - on first join, if the player has never chosen;
  - on every respawn after death.
- Yes → One Life on. No, or closing the dialog → One Life off.
- `/onelife` shows the current mode. `/onelife on` turns it on at any time. There is no `off`: once on, only death
  turns it off.
- Death clears the mode (back to "not chosen"), so the next respawn asks again.
- With `one-life.required: true` (§5), none of the above applies: no dialog is shown, every grave is One Life
  whatever the stored choice, and `/onelife` and `/onelife on` only reply "One Life is required on this server."
  Stored choices are left alone, so setting it back to `false` resumes normal behaviour.

### 2.2 Death

On every player death the One Life choice is cleared (§2.1). Then, unless the death was in the void or the `keep_inventory` gamerule is true:

1. Take the drops vanilla is about to scatter (curse-of-vanishing items are already gone) and the player's
   **total** XP points (not vanilla's capped drop).
2. Pull out any gravestone items the player was carrying; they become separate graves (step 6).
3. Apply the size cap (§6.3); anything spilled drops on the ground as normal items.
4. Build the grave: owner UUID + name, mode (One Life or lootable, from §2.1), cause of death, last chat message,
   items, XP, `mined = false`, a new grave id.
5. Clear vanilla's drops and dropped XP.
6. Place the new grave at the nearest free space (§6.1), then each carried gravestone at the next nearest.

Void deaths behave as vanilla (items lost to the void, no grave).

### 2.3 Looking inside (right-click)

A 5-row window shows the 41 stored slots in the player's layout (hotbar, main, armour,
offhand). The title is `One Life Grave - unable to loot` for a One Life grave and `Grave of <title>` (§2.6) for a
lootable one; a grave whose contents can't be read keeps the title `<owner>'s Grave`.

- **One Life:** every click, drag, number-key swap, double-click collect and shift-click is cancelled.
- **Lootable:** items can be taken out; nothing can be put in (actions that would place into the grave are
  cancelled). The window is shared by everyone viewing the same grave, like a chest, and changes are written back
  to the grave straight away.

### 2.4 Breaking

Gravestones break after **3 hits** by anyone. On break:

1. If `mined` is false: set `mined = true`, then give the breaker half the stored XP (rounded down).
2. Close any open windows on this grave.
3. Remove the floating text.
4. Drop **our** gravestone item carrying the up-to-date grave data (CraftEngine's own drop is cancelled); if the grave stands in lava or water, the item goes straight into the breaker's inventory (overflow drops at their feet) so it can't burn or drift away.

Explosions, fire and projectiles do not damage a placed gravestone.

### 2.5 Placing a carried gravestone

A gravestone item places like any furniture. The grave data moves from the item onto the placed furniture and the
floating text appears. A gravestone item with no grave data (e.g. from `/ce give`) cannot be placed.

### 2.6 Text

Shown as floating text above the placed grave and as the item's tooltip:

- `☠ <title>`, where `<title>` is `<owner>, the <ordinal>` (e.g. "Steve, the Third"; just `<owner>` when the death
  number is unknown). The item name is `Grave of <title>`.
- the cause of death in English without the player's name, e.g. "Was slain by Zombie"
- the last chat message, in quotes, italic; omitted if the player never chatted
- tooltip only: `One Life` or `Lootable` (floating text omits it; the window title carries the mode)

Long text wraps (floating text) or is split into lines (tooltip). The cause is rendered to plain English on the
server at death time (vanilla translations, server language), the owner's name prefix is removed and the first letter
capitalised; only the player's plain name is stripped, so a team prefix or nickname plugin leaves the name in the
cause. It is English for everyone. Graves made before this change keep a translatable cause, which
still displays.

The death number is `DEATHS statistic + 1` at death time (vanilla awards the statistic after the death event).
Ordinals are words up to Ninety-Ninth, then digits ("100th", "101st").

### 2.7 If the item is destroyed

A gravestone item that burns, falls in lava, or despawns is gone along with its contents. The plugin keeps no
records outside the item and the placed furniture, so there is nothing to clean up.

## 3. Components

The plugin lives in `src/main/java/net/dosaki/onelife/`. Units marked **pure** don't use the server or CraftEngine,
so they can be unit tested on their own.

| Unit | Job | Depends on |
|---|---|---|
| `GraveData` (pure) | Immutable record: grave id, owner UUID + name, mode, cause (component JSON), last message, items (41 slots), XP, `mined`. Encode/decode to a byte array. | Paper API types only |
| `SizeCap` (pure) | Given the 41 slots and a byte limit, returns what's stored and what spills. | — |
| `GraveText` (pure) | Builds floating text lines and tooltip lines from `GraveData`. | Adventure |
| `OneLifeState` | Read/write a player's mode (`chosen`, `enabled`) in their persistent data; enforce on → off only by death. | Paper |
| `LastMessageTracker` | Saves each player's last chat message in their persistent data. | Chat event |
| `OneLifePrompt` | Shows the dialog on first join and on respawn; applies the answer. | Paper dialog API |
| `OneLifeCommand` | `/onelife`, `/onelife on`. | Paper commands |
| `GravePlacer` | Finds free spaces (§6.1); places CraftEngine furniture with grave data; spawns the floating text. | CraftEngine API |
| `GraveStore` | Reads/writes `GraveData` on a placed grave's furniture entity; finds a grave's floating text. | CraftEngine API |
| `DeathListener` | §2.2. | `SizeCap`, `GravePlacer`, `OneLifeState`, `LastMessageTracker` |
| `GraveViewer` | §2.3: opens and polices the window; one shared inventory per open grave. | `GraveStore` |
| `GraveBreakListener` | §2.4. | CraftEngine events, `GraveStore` |
| `GravePlaceListener` | §2.5. | CraftEngine events, `GraveStore` |

### Bundled CraftEngine content

The jar contains the CraftEngine resources and copies them into CraftEngine's resources folder on startup, so the
model always matches the plugin version:

- furniture `onelife:gravestone`: item display element with the gravestone model; one solid 1×1 `shulker` hitbox;
  `hit-times: 3`; not hittable by projectiles; blocks building in its space;
- item `onelife:gravestone`: max stack size 1, places the furniture;
- model JSON and 16×16 texture.

## 4. Data

### 4.1 Where grave data lives

| State | Location |
|---|---|
| Placed | The furniture's meta entity persistent data, key `onelife:grave` (bytes). This is the authoritative copy; lootable graves change it when looted. |
| Carried | The gravestone item's persistent data, key `onelife:grave` (bytes). |
| Floating text | A persistent `TextDisplay` entity above the grave tagged `onelife:grave_id = <grave id>`. If a placed grave loads without its text, the text is recreated. |

The furniture also keeps CraftEngine's own copy of the placing item, but we never rely on it: break always drops a
fresh item built from the meta entity's current data.

### 4.2 Encoding

- Items: Paper's `ItemStack.serializeItemsAsBytes` / `deserializeItemsFromBytes` over a fixed 41-slot array (empty
  slots included). These bytes carry the Minecraft data version, so items upgrade when the server upgrades.
- Cause: Adventure component as JSON (a plain text component from format 2 on; older graves may hold a translatable one).
- Everything else: plain fields. A leading format version byte allows future changes. Format 2 adds the death
  number (int, 0 = unknown); format 1 still decodes, with death number 0.

### 4.3 Player data

| Key | Meaning |
|---|---|
| `onelife:chosen` | Has chosen for this life |
| `onelife:enabled` | One Life on |
| `onelife:last_message` | Last chat message (plain text, ≤ 256 chars) |

## 5. Configuration (`config.yml`)

| Key | Default | Meaning |
|---|---|---|
| `size-cap-bytes` | `524288` (512 KiB) | Max encoded size of a grave's items |
| `search-radius` | `3` | How far to look for a free space |
| `xp-fraction` | `0.5` | Share of stored XP given on the first break |
| `one-life.required` | `false` | Every player is always One Life and is never asked (read at startup only) |

## 6. Edge cases and error handling

### 6.1 Finding a free space

- A free space is air, water or lava (cave air and void air count as air).
- Look at the death block first, then outward in rings up to `search-radius`, nearest first.
- Nothing free → the first block above the death spot that isn't bedrock, replacing it.
- Keep within the world's height range.
- Mid-air or in liquid: the grave floats there. No support is needed.

### 6.2 Bad data

- If a grave's items can't be decoded (e.g. a format change after a Minecraft upgrade), the grave still shows its
  text, the window shows a single "contents unreadable" item, and a warning is logged with the grave id. Breaking it
  drops an item with the original bytes untouched.

### 6.3 Size cap

- Encode all 41 slots. If over `size-cap-bytes`, remove shulker boxes (largest encoded size first) until under.
  Removed boxes drop on the ground at the death spot as normal items.
- If still over with no shulker boxes left, remove other items largest first in the same way.

### 6.4 Concurrency and duplication

- `mined` is set before XP is given.
- Lootable windows share one inventory per grave and write back on every change, so two players can't take the
  same item.
- Breaking a grave closes its windows before dropping the item.
- Hoppers and other automation can't reach furniture, so no extra protection is needed.

### 6.5 Startup

- If CraftEngine isn't present, the plugin doesn't load (hard dependency in `paper-plugin.yml`). Deaths then use
  vanilla drops, so items are never lost to a half-working plugin.

## 7. Server integration (in `dosaki/minecraft-server`)

The server repo needs:

- `versions.json`: pin CraftEngine and the `onelife` jar (release URL + sha256).
- CraftEngine config: the server serves the pack itself on the game port (`self` host, `port: auto`, URL
  `http://<server DNS name>:25565/`); pack sent on join and required.

S3 + CloudFront was the original plan, but CraftEngine 26.9's S3 host only accepts static access keys (not the EC2
instance role), the map sync job deletes unknown keys in the map bucket, and CloudFront caching could serve a stale
pack after a reboot. Self-hosting needs no keys, no Terraform and always serves the pack that matches its hash.

## 8. Testing

- **Unit (JUnit):**
  - `GraveData` round trip, including shulker boxes with contents and empty slots;
  - `SizeCap` under the limit, over the limit, spill order;
  - `GraveText` with long and missing last messages;
  - `OneLifeState` transitions (off → on; on → off refused; death clears).
- **MockBukkit** (if it supports Paper 26.3; otherwise these move to the manual list):
  - death replaced by a grave;
  - void death and `keep_inventory` ignored;
  - One Life window blocks every action; lootable window blocks putting items in;
  - XP paid once.
- **Manual smoke test** on a local Paper + CraftEngine server:
  - resource pack prompt and kick on decline;
  - model and floating text look right;
  - break → carry → place round trip;
  - lava destroys the item;
  - dialog on first join and on respawn.
- **CI:** GitHub Actions runs the Gradle build and tests on every PR. Pushing a `v*` tag builds the jar and attaches
  it to a GitHub Release.

## 9. Out of scope

- Real block mining (crack animation, break time) — furniture breaks by hits.
- Admin commands, grave listings, maps or compasses pointing to graves.
- Protecting graves from other players: anyone can view, break and (for lootable graves) loot.
- Bedrock (Geyser) players.
