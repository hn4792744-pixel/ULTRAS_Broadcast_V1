# ULTRAS_Broadcast_V1

Premium administrative announcement system for Paper. Developer: **UC_Hussein**.
Announcements can be published in **chat**, in the **middle of the screen** or **above the hearts**, with a live 0.1 s countdown.

> Status: written without network access, so it has **not been compiled or run yet**. Build it once (`gradle build`) and test on a server before using it in production. See *Compatibility*.

## Features
- `/bc <type> <text> <seconds> [player|all]`, same handler for `/uc_bc`.
- Types `chat`, `screen`, `hearts` (names and aliases editable in `config.yml`).
- Colors: `&0-&9 &a-&f &k &l &m &n &o &r` and `&#RRGGBB`. Admin text is never parsed as MiniMessage.
- Duration in seconds (decimals allowed), limits in config. Countdown `5.2s, 5.1s ...` computed from one central `endAt` timestamp, so every player sees the same time.
- Only one global announcement at a time (`broadcast.only-one-global-active`). Personal announcements (to one player) are independent, never listed, and can be paused while a global one runs (`personal-announcements.allowed-while-global-active`).
- `/bc cancel [player]`, `/bc list`, `/bc reload`, `/bc help`, `/bc_setting`.
- Player settings (SQLite, by UUID): language, sounds, chat, screen, hearts, reset. Applied to global and personal announcements. Changing a setting while an announcement is running takes effect immediately.
- List GUI with pages, live status refresh, details, active-announcement page (Cancel Publication), Republish with a location picker, Delete, Delete All with confirmation, optional Preview.
- Every GUI is a YAML file in `gui/`; all texts come from `messages_en.yml` / `messages_ar.yml`.
- Audit log (`logs/YYYY-MM-DD.log` + console), events, small developer API.

## Requirements
Paper (not Spigot, not Folia). Java 25 for Minecraft 26.x, Java 21 for 1.21.x. SQLite driver is bundled with Paper.

## Installation
1. `gradle build` (see *Building*), put `build/libs/ULTRAS_Broadcast_V1-1.0.0.jar` in `plugins/`.
2. Start the server. Files are created in `plugins/ULTRAS_Broadcast_V1/`: `config.yml`, `messages_en.yml`, `messages_ar.yml`, `gui/*.yml`, `data.db`, `logs/`.

## Commands
| Command | Permission | Description |
|---|---|---|
| `/bc <type> <text> <seconds> [player\|all]` | publish | Publish. `all` or no player = everyone |
| `/bc cancel [player]` | cancel | Cancel the global announcement (or a player's personal one) |
| `/bc list` | list | Open the list (players only) |
| `/bc reload` | reload | Reload config, languages, GUI files, sounds, timer |
| `/bc help` | use | Help, only the lines you may use |
| `/bc_setting` | settings | Your settings (players only) |

Examples
```
/bc chat Welcome to ULTRAS CLUB 10
/bc screen &cServer restart &fin 5 minutes 10 all
/bc hearts You have a new message 5 Hussein
/bc chat &cWelcome &#ff0000to &fULTRAS 10
```
The seconds value is the last number; an optional player name may follow it. `/bc chat 10` (no text), `/bc chat hi abc`, `-5`, `0` are rejected.

### Where each type is shown
- **chat**: one framed block (gradient lines, heading, text). Sent once per player.
- **screen**: fixed title `ULTRAS CLUB` + the text as subtitle; the **timer is on the action bar just below** them. Minecraft titles cannot show a third updating line, so this is the closest possible layout.
- **hearts**: one action-bar line `text   5.2s`, the vanilla spot directly above the hearts/hotbar. A boss bar is not used because it appears at the top of the screen. Other plugins that also write to the action bar will compete with it.

## Permissions (names editable in `config.yml -> permissions`)
`ultrasbroadcast.use` `publish` `cancel` `list` `delete` `reload` (OP) · `ultrasbroadcast.settings` (everyone) · `ultrasbroadcast.bypass.cooldown` (OP) · `ultrasbroadcast.admin` (all of them, OP). Renamed nodes are registered automatically.
Suggestions and the command list only show what the player may use. The plugin never modifies other plugins' commands.

## Configuration highlights
`language`, `available-languages`, `branding.*` (name, title, separator, gradient), `colors`, `symbols`, `broadcast.*` (limits, `update-interval-ms`, sound), `types.*`, `chat.*`, `screen.*`, `timer.*`, `cooldown.seconds`, `preview.enabled`, `active-announcement.restore-after-restart`, `delete-all.cancel-active`, `storage.*` (max, auto-delete, purge), `player-defaults`, `gui.*`, `gui-sounds`, `format.*`, `logging.*`, `permissions.*`.
Sounds accept `BLOCK_NOTE_BLOCK_PLING` or `block.note_block.pling` style names.
Command aliases beyond `/bc`, `/uc_bc`, `/bc_setting` are not created at runtime (that would require editing the server command map); use Paper's `commands.yml` aliases if you need more.

## Storage
SQLite (`data.db`): `announcements` (id, message, type, location, creator uuid/name, target, created/started/ended/end timestamps, duration, status, republish count, views, deleted) and `player_settings` (uuid, name, language, sounds, chat, screen, hearts, updated_at). Deleted announcements stay as `deleted=1` records (audit) until purged (`storage.purge-deleted-after-days`). A single database thread does all writes; player settings are read on the async pre-login thread.
On restart an active announcement ends, or is restored when `restore-after-restart: true` and time remains.

## Delete All
Deletes every finished announcement. The active one is kept by default (`delete-all.cancel-active: false`) so the system never reaches an illogical state.

## GUI files
`gui/settings.yml announcement-list.yml announcement-details.yml publish-location.yml active-announcement.yml delete-confirmation.yml preview.yml`: title, rows, filler, slots (numbers or `"10-16"` ranges), material, name, lore, glow, sound, `visible`, and variants (`enabled/disabled`, `active/finished`, `selected/unselected`). Text can be `lang:some.key` or literal MiniMessage.

## Developer API
```java
BroadcastAPI api = BroadcastAPI.get();          // main thread only
api.publish(new PublishRequest(uuid, "MyPlugin", AnnouncementType.SCREEN, "&aHello", 10_000L, null, null, true));
api.cancel("MyPlugin"); api.isActive(); api.getActiveAnnouncement();
```
Events (main thread): `AnnouncementPublishEvent`, `AnnouncementCancelEvent`, `AnnouncementDeleteEvent` (cancellable), `AnnouncementFinishEvent`.

## Compatibility
- `build.gradle` defaults to `io.papermc.paper:paper-api:26.2.build.+` with Java 25 (taken from Paper's published instructions; not verified here).
- For 1.21.x: `gradle build -PpaperVersion=1.21.4-R0.1-SNAPSHOT -PjavaRelease=21`.
- Only stable Paper/Adventure APIs are used, but API differences between versions could still require small fixes; this was not compiled against any Paper jar.
- No `gradlew` files are included (the wrapper jar cannot be generated offline). Run `gradle wrapper` once if you want them.

## Building
`gradle build` with JDK 25 (or 21 for the 1.21 build) and access to `repo.papermc.io`.

## Troubleshooting
- `/bc` opens another plugin: the first registered plugin owns the short label. Use `/ultras_broadcast_v1:bc` (the console prints a warning at start-up).
- Nothing shows on the action bar: another plugin overwrites it.
- Missing texts appear as red key names: copy the key from the bundled `messages_en.yml` into your edited file.
- Plugin disables itself at start: check the console for `[STORAGE]` (database file not writable).
