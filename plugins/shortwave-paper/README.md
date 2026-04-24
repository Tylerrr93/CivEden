# Shortwave

A physical radio tower broadcasting system for Paper 1.21. Players build structures that read from a Book & Quill and broadcast messages on a frequency. Other players can receive those messages using a tuned Recovery Compass or a speaker block placed anywhere in the world.

---

## Player Guide

### Building a Tower

Towers are a 4-block vertical structure. Stack them in this order from the ground up:

```
Lightning Rod       <- top
Chiseled Copper
Lectern
Chiseled Copper     <- base (this is what you interact with)
```

Once built, shift + right-click the base chiseled copper with an empty hand to set a frequency and register the tower. Frequencies are in the format `104.5` and must be between 88.0 and 108.0 MHz.

After that, shift + right-clicking the base copper opens the tower management GUI where you handle everything else.

### Writing Your Broadcast

Write a Book & Quill and place it in the Lectern. Each page becomes one broadcast message. The tower cycles through pages sequentially and loops back to the start. How many lines of each page get broadcast depends on your tower's upgrade tier — lower tiers only send the first line or two, higher tiers send more.

Keep pages short. Broadcasts show up in players' chat, so walls of text aren't ideal.

### Fuel

Towers need fuel to broadcast. You add it through the GUI. Different fuels last different amounts of time:

| Fuel | Duration |
|------|----------|
| Coal | 10 minutes |
| Charcoal | 10 minutes |
| Blaze Rod | 30 minutes |
| Lava Bucket | 2 hours |

A tower with no fuel stops broadcasting but keeps all its settings.

### Range and Upgrades

All towers start with a 500-block range. You upgrade through the GUI by spending materials. Upgrades are sequential — you can't skip tiers.

| Tier | Cost | Range | Broadcast Lines |
|------|------|-------|-----------------|
| Basic | free | 500 blocks | 1 |
| Iron | 3x Iron Block | 1,000 blocks | 2 |
| Gold | 5x Gold Block | 2,500 blocks | 3 |
| Diamond | 7x Diamond Block | 5,000 blocks | 4 |
| Netherite | 9x Netherite Block | 10,000 blocks | 5 |
| Emerald | 12x Emerald Block | 20,000 blocks | 6 |

### Jingles

You can pick a jingle that plays before each broadcast for players holding a tuned receiver. Set it in the tower GUI. It's optional.

### Oxidation and Garbling

Tower copper oxidizes very slowly over time (much slower than vanilla — about 2% of the normal rate by default). As it oxidizes, the signal gets garbled. Scrape it with an axe to reset it.

| State | Garble Amount |
|-------|--------------|
| Unaffected | 0% |
| Exposed | 10% |
| Weathered | 30% |
| Oxidized | 100% (`### #### ####`) |

### Receiving Broadcasts

**Handheld receiver (Recovery Compass)**

Right-click a Recovery Compass to tune it. You'll type a frequency in chat. Once tuned, hold it in your main hand or off-hand and broadcasts from towers on that frequency will appear in your chat, as long as you're within range of the tower.

**Speaker blocks**

Place any copper block on the ground and a Decorated Pot on top. Right-click the pot and type a frequency to tune it. When a tower on that frequency broadcasts and you're nearby, a hologram text display appears above the speaker for 5 seconds.

Speakers only work when their chunk is loaded. If nobody is around, they don't fire. That's intentional.

### Reinforcement and Access

If a tower is reinforced to a NameLayer group via Citadel, only members of that group (with the `USE_RADIO` permission) can open the GUI or tune speakers near it. Unreinforced towers are open to anyone.

---

## Admin and Dev Reference

### Commands and Permissions

| Command | Permission | Description |
|---------|------------|-------------|
| `/shortwave clearholograms` | `shortwave.admin` (op default) | Removes all stuck speaker holograms across all loaded worlds |

The hologram cleanup also runs automatically 1 tick after the plugin enables to clear any leftover displays from a previous session.

### Configuration

`config.yml` covers everything: broadcast interval, frequency range and decimal format, fuel items and durations, receiver item type, range upgrade tiers, jingle sounds, oxidation cancel chance, GUI titles, and the auto-save interval. Everything is commented in the file.

A few non-obvious ones:

- `tower-oxidation-cancel-chance` — probability (0.0–1.0) that a natural oxidation tick on tower copper is suppressed. Default `0.98` means towers oxidize at roughly 2% of vanilla speed.
- `broadcast-lines` on each tier — how many lines of a book page get included in the broadcast. Hard-capped at 6 regardless of config.
- `auto-save-interval-ticks` — how often the tower and speaker data is auto-saved to disk. Default 6000 ticks (5 minutes). Saves also happen immediately on any state change.

### Data Persistence

Tower data is saved to `plugins/Shortwave/towers.json` and speaker tuning is saved to `plugins/Shortwave/speakers.json`. Both use Gson and follow the same structure. Towers save: location, frequency, fuel end timestamp, current page index, jingle, range, cached book pages, and cached oxidation level. Speakers save: location and tuned frequency.

Fuel uses a Unix timestamp (`fuelEndTime = System.currentTimeMillis() + duration`), so it ticks down correctly whether the server is running or not.

### Code Structure

```
net.edenciv.shortwave/
├── ShortwavePlugin.java          main plugin class, command handler, hologram cleanup
├── models/
│   └── RadioTower.java           tower data model, holds all cached state
├── managers/
│   ├── TowerManager.java         tower registry and JSON persistence
│   ├── SpeakerManager.java       speaker registry and JSON persistence
│   ├── ConfigManager.java        typed config access with result caching
│   └── GUIManager.java           all tower GUI screens (CivModCore ClickableInventory)
├── listeners/
│   ├── InteractionListener.java  right-click handling, chat input, block breaks
│   └── OxidationListener.java    BlockFadeEvent — updates cached oxidation on tower copper
├── tasks/
│   └── BroadcastTask.java        BukkitRunnable that fires every broadcast interval
└── utils/
    └── MessageUtils.java         MiniMessage parse helper
```

### Caching Architecture

`RadioTower` holds three cached values that the broadcast task reads without touching any blocks:

- `cachedPages` — the book pages, updated when a book is placed in the lectern and on GUI open. Saved to disk so it survives restarts.
- `cachedOxidation` — the copper's oxidation level, updated by `OxidationListener` when a fade event goes through, and refreshed on GUI open. Saved to disk.
- `structureIntact` — boolean, defaults to `true` on load. Set to `false` when any of the four structure blocks are broken via `BlockBreakEvent`. Refreshed to the real block state when a player opens the tower GUI.

This means `BroadcastTask` does zero block reads. Towers broadcast correctly even when their chunk isn't loaded, since all the data it needs is in the model. Fuel also works for unloaded towers since it's timestamp-based.

Speakers work the opposite way — they intentionally only fire when their chunk is loaded. `SpeakerManager.getLoadedSpeakersOnFrequency()` checks `world.isChunkLoaded()` and skips anything unloaded.

### GUI

Uses CivModCore's `ClickableInventory` / `LClickable` / `DecorationStack` framework. Tower closures capture the `RadioTower` reference directly so no metadata lookup is needed between screens. The only player metadata used is `shortwave_pending_frequency` for the chat-based frequency change flow (since the GUI has to close before the player types).

### Dependencies

- **CivModCore** — GUI framework, required
- **Citadel** — reinforcement access checks, required
- **NameLayer** — group permission type registration (`USE_RADIO`), required

Built with Gradle (`build.gradle.kts`). Follows the same structure as other Eden plugins — `compileOnly(project(":plugins:civmodcore-paper"))` etc.
