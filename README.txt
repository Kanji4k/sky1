HOW TO BUILD (GitHub, no installs)
1. Create a free account at github.com and click New repository (name: confis-client, Public or Private).
2. On the new repo page click "uploading an existing file", drag in EVERYTHING from this folder
   (including the hidden .github folder) and click Commit changes.
   Tip: easiest is to drag the folders themselves: .github, intermediary-src, src, tools plus the
   loose files build.gradle, gradle.properties, settings.gradle.
3. Click the Actions tab -> "Build mod" -> wait ~3-6 minutes for a green check.
4. Open the finished run -> scroll to Artifacts -> download confis-mod-jar -> unzip -> put the .jar
   in .minecraft/mods (Fabric Loader + Fabric API required).
If the run is red, open it, copy the red error text, and send it to Claude.

WHAT'S NEW IN THIS VERSION (all show up as modules when you press Right Shift)
- Inventory Hub, Totem Count, Potion Count, Saturation, Zoom (key changeable in its options), Tier Tagger
  (PvPTiers / MCTiers / both), Shulker Tooltip (hold Shift over a shulker box), Crosshair Indicator.
- Effect Timer now shows the effect icon instead of the effect name.
- Item Guide: click an item to see its crafting recipe and where to find it.
- Name badge: "C" before your own name; other mod users get it too if you set "userListUrl" in
  .minecraft/config/confis0oooooo.json to a text file (one player name per line).
- Tier tagger web addresses are in the same config file (tierUrlMc / tierUrlPvp) in case a site changes its API.

UPDATE (HUD module changes)
- Freecam (new HUD module): press F6 in game to fly the camera around while your player stays put.
  Change the key and speed in Freecam options. Fly with W A S D / Space / Shift (Ctrl = faster).
- Armor Status: options now let you show durability as a Number, a Percentage, or Both,
  and put that text on the Left or Right of the item.
- Potion Count: already in the module list (counts potions, splash and lingering).
- Minimap removed (module, settings button and code). Waypoints still work.
- Fullbright (new): switch on in the module list; brightness goes to max and is restored when you switch it off.
- No Blindness (new): removes the Blindness effect view. Both start switched OFF.

UPDATE 3
- Item Drop Scale / Online Players: the search box keeps focus while you type.
- Hitboxes & Nametags: switches now work on 1.21.11 (names were using the wrong mapping before).
- Messenger replaced by Friends (Tools > Friends): type a name + ADD, click a friend to chat (uses /msg).
  Friends listed in userListUrl show a blue C. Green dot = online.
- Right Shift menu now pops up with a short animation.
- Edit HUD has no blur.
- Server TPS fixed (hooks the server time packet).

UPDATE 4 (Liquid Glass HUD)
- Every HUD module (FPS, CPS, ping, keystrokes, armor, inventory hub, effects, scoreboard, counters ...) now uses
  an iOS-style liquid glass look: rounded frosted panel, bright sheen on top, glowing rim that fades down,
  soft shadow. Inventory slots are rounded glass wells.
- The module's "background" color and the background switch still work: the color's transparency sets how frosted
  the glass is, and turning the background off hides the glass completely.
- New file: intermediary-src/client/confis/Glass.java (upload it with the other folders).

UPDATE 5 (Liquid Glass menu)
- The Right Shift menu (All Modules, tabs, Options, Waypoints, Settings) now has the same liquid glass style:
  see-through rounded window, glass module cards that glow brighter when you hover them or switch them on,
  pill buttons (the active tab and "Enabled" are light glass), inset glass search box, rounded sliders and scrollbar.
- Changed files: Glass.java, ConfigScreen.java (both in intermediary-src/client/confis).
- Also includes the fixes for ItemGuideScreen.java and ShulkerPreview.java.

UPDATE 6 (Item Guide search)
- Typing in the Item Guide search box now works: the box keeps focus while the list refreshes after each letter.
- Shows "No items found." when nothing matches. Changed file: ItemGuideScreen.java.

UPDATE 7
- Inventory Hub options: new "Quick Access Bar" ON/OFF button (hides the bottom hotbar row and shrinks the box).
- Big FPS fix for the Right Shift menu: the liquid glass now merges rows into far fewer draw calls
  (about 4x fewer), and the key-rebind scan only runs while you are rebinding a key.
- Category tabs (ALL / INFO / HUD ...) play the vanilla button click sound.
- Edit HUD: click an element and use the - / + buttons under it, or scroll the mouse wheel over it, to change its size.
- Changed files: Glass.java, ConfigScreen.java, NewModules.java, Config.java.

UPDATE 8 (Neon blue glass menu, no blur)
- Menu restyled after the reference image: translucent blue glass window, bright cyan-white rims and sheen,
  blue glass cards/tabs/buttons, green "Enabled" pills, white hearts, colored pixel icons.
- The menu no longer blurs or dims the game (no full-screen effects). Glass is flat fills + banded gradients only.
- Changed files: Glass.java, Icons.java, ConfigScreen.java.

UPDATE 9 (Themes, pixel icons, lighter menu rendering)
- Settings > APPEARANCE: pick Solid or Glass, one of 12 colors (Black, White, Yellow, Red, Blue, Purple, Brown, Grey,
  Pink, Green, Light Blue, Orange), and set glass opacity / border / highlight. A live preview shows the result.
  Saved in confis0oooooo.json (themeColor, themeGlass, glassOpacity, borderIntensity, highlightIntensity,
  menuAnimations). Old configs open as Blue Glass (the previous look). "Reset appearance" restores that.
- The old "Menu background" color editor is replaced by the Appearance page (the panelBg value is kept in the file).
- New 16x16 shaded pixel icons for every module (generated by tools/gen_icons.py -> IconData.java). Disabled modules
  show a darker icon, hovered cards a brighter one.
- Cards: titles wrap to two lines (never touch the heart), the grid uses fewer columns on narrow windows, the tab bar
  wraps to two rows when labels would overlap, the scrollbar can be dragged, scrolling eases (Menu animations switch).
- Rendering: no blur, shaders or framebuffer work anywhere. Only visible rows are drawn, the filtered module list and
  title wrapping are cached, glass banding is coarser (fewer fill calls).
- New files: Theme.java, IconData.java, tools/gen_icons.py. Changed: Glass.java, Icons.java, Config.java, ConfigScreen.java,
  ConfisScreen.java, .github/workflows/build.yml (file count check). Upload the whole folder again.

----------------------------------------------------------------------------------------------------
UPDATE: HUD transparency, Item Glint, No Darkness
----------------------------------------------------------------------------------------------------
* Transparency: every HUD module has Background / Text / (Accent) / Border colors, each with an
  "Opacity: N%" slider (0% = invisible, 100% = solid). Glass.draw now uses the exact color + opacity (shadow,
  sheen and slot wells fade with the background). Border is a new color + opacity (default white 45%).
  Text/accent opacity is their own slider. Everything is saved in the config (ARGB ints).
* Item Glint (module "Item Glint", off by default): items color + armor color (with opacity), intensity slider,
  Custom/Vanilla switch, Weapons/Armor/Tools/Other switches. Files: Glint.java, mixin/ItemGlintMixin.java.
* No Darkness (module, off by default): removes the Darkness effect; slider for partial reduction.
  File: NoDarkness.java.
* New files: Glint.java, NoDarkness.java, mixin/ItemGlintMixin.java (the workflow now expects 52 Java files).
  Changed: Glass, ConfigScreen, Config, Module, Modules, NewModules, ExtraModules, Waypoints, Hud, ConfisClient,
  Icons, confis0oooooo.mixins.json, build.yml.

----------------------------------------------------------------------------------------------------
UPDATE: monochrome icons + "Module Icons" setting
----------------------------------------------------------------------------------------------------
* tools/gen_icons.py redraws all 26 module icons in a strict grayscale palette (k E D G M L w, transparent
  background). Run `python3 tools/gen_icons.py preview.png` to regenerate IconData.java and a contact sheet.
  Icons are still pre-baked into rectangles at class load (no per-frame cost). "No Darkness" and "Item Glint"
  now have their own icons (the borrowed aliases were removed from Icons.java).
* Settings > GENERAL > "Module Icons: ON/OFF" (Config.showModuleIcons, default ON, missing in old configs = ON).
  OFF hides the icon in every module card (all categories use the same card renderer) and in the Appearance
  preview; cards shrink to a compact height so there is no empty icon area. Applies instantly, saved in config.

----------------------------------------------------------------------------------------------------
UPDATE: Free Look + Hit Color (new HUD modules) and the missing-icon fix
----------------------------------------------------------------------------------------------------
* Icon fix: Icons.java no longer looks for PNG sprites that were never in the project (they showed as pink/black
  squares). Every module now draws its built-in pixel icon from IconData.java.
* FREE LOOK (module "Free Look", HUD box "Free Look [LEFT ALT] ON/OFF"): look around with the mouse while your player
  keeps moving and aiming the same way. Options: key (click, press a key) and Mode: "Press to toggle" or "Hold the key".
  Works in first person and third person (back / front). It turns itself off when a menu (except chat) opens, the window
  loses focus, you die / disconnect / change world, Freecam starts, or the module is switched off. Your own rotation is
  never changed, so there is nothing to restore. Files: FreeLook.java, mixin/FreeLookInputMixin.java,
  mixin/FreeLookCameraMixin.java, confis0oooooo.freelook.mixins.json (own mixin config, like Freecam).
* HIT COLOR (module "Hit Color", starts OFF; no HUD box). Options: Enable Hit Color (= the module switch), Mode
  (Armor Only / Body-Skin Only / Both), Enable Armor Hit Color, color picker (square + hue bar + RGB sliders + editable
  HEX field + presets), Hit Duration (100-2000 ms), Hit Intensity (0-100%), Preview, Reset to Default.
  A player counts as hit when their hurtTime counter rises (the same signal vanilla uses for its red flash, which is
  left untouched). The color is multiplied onto the model's tint for the duration (full strength for the first quarter,
  then fading out), so skins, armor patterns, trims, dye and transparency stay visible; nothing is written to textures,
  no shader, no full-screen effect. Players only; your own player is tinted in third person (not the first-person hand).
  Files: HitColor.java, mixin/HitColorQueueMixin.java, mixin/HitColorBatchMixin.java, confis0oooooo.hitcolor.mixins.json.
  Saved in config: hitMode, hitArmor, hitColor, hitDuration, hitIntensity, freeLookKey, freeLookHold.
* Changed: Config, ConfigScreen, Modules, Hud, ConfisClient, fabric.mod.json, tools/gen_icons.py + IconData.java (two new
  icons), build.yml (now expects 58 Java files).
* NOT test-built on Minecraft here: the render hooks were written against the Yarn 1.21.11 API docs. If the GitHub build
  is red, send the error text. If a hook does not apply in game, the module simply does nothing (Free Look also
  says "not supported on this version") and the rest of the mod is unaffected.

----------------------------------------------------------------------------------------------------
UPDATE: Sky main menu + sky HUD style
----------------------------------------------------------------------------------------------------
* New SkyTitleScreen.java replaces the vanilla title screen: sky gradient + drifting clouds, frosted glass buttons
  (single player, multiplayer, mod menu, sky mods, options) and quit at the bottom. "mod menu" opens Mod Menu if
  installed, otherwise the Confis menu; "sky mods" opens the Confis menu.
* Every HUD module panel now uses a sky-blue glass gradient with a white rim (opacity sliders still apply).
* Switch either off in .minecraft/config/confis0oooooo.json: "skyTitle": false / "skyHud": false.
* Changed: Glass.java, Config.java, ConfisClient.java, build.yml (expects 59 Java files). New: SkyTitleScreen.java.
* NOT test-built here (no Minecraft toolchain). If the GitHub build is red, send the error text.

UPDATE: real sky photo backgrounds + sky menu theme
* SkyBg.java draws src/main/resources/assets/confis0oooooo/textures/gui/sky_bg.png behind the sky title screen, Multiplayer and Mod Menu (mixin/ScreenSkyMixin.java; switch: "skyScreens": false).
* The Confis menu switches once to the "Sky Blue" glass theme (change it any time in Settings > Appearance).
* build.yml expects 61 Java files.

UPDATE: sky background on every screen, sky photo inside menu windows, Feather-style flat light icons.
* SkyBg.wanted: all screens except inventories, chat and Confis screens. Switch: "skyScreens": false.
* Glass.panel / panelDeep draw the sky photo inside the menu window.
* Icons: outer outline removed, flat light colors, disabled = translucent.
