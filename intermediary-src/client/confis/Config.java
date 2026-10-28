package client.confis;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Config {
    /** Per-mod settings. Colors are ARGB ints. Position is a fraction of the screen. */
    public static class ModuleCfg {
        public boolean enabled = true;
        public double fx, fy;
        public int bg = 0x80000000;
        public int text = 0xFFFFFFFF;
        public int accent = 0xFFFFFFFF;
        /** Panel border color (ARGB). The alpha byte is the border opacity: 0 = invisible, 255 = fully opaque. */
        public int border = 0x73FFFFFF;
        public boolean showBg = true;
        public boolean favorite = false;
        /** Size multiplier for this HUD element (0.5 - 3.0). */
        public float scale = 1.0f;
        /** Inventory Hub: show the Quick Access Bar (the hotbar row at the bottom). */
        public boolean quickBar = true;
        /** Armor Status: durability text NUMBER / PERCENT / BOTH, and which side of the item it sits on. */
        public String armorMode = "NUMBER";
        public String armorSide = "RIGHT";
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("confis0oooooo.json");
    private static Config instance;

    public Map<String, ModuleCfg> modules = new LinkedHashMap<>();
    public int accent = 0xFFFFFFFF;
    public int panelBg = 0xF2050505;
    public boolean clock24h = true;
    public boolean nameBadge = true;
    public List<Waypoint> waypointList = new ArrayList<>();
    /** Friends list (player names) shown in the Friends menu. */
    public List<String> friends = new ArrayList<>();
    /** Dropped-item size: default for all items + per-item overrides keyed by item id (e.g. "diamond"). */
    public float dropScale = 1.0f;
    /** Hitbox switches by type key (crystal, exp, friendly, hostile, firework, pearl, windcharge) and nametags. */
    public boolean hitboxMaster = true;
    public Map<String, Boolean> hitboxes = new LinkedHashMap<>();
    public boolean nametags = true;
    /** Zoom key (GLFW key code, default C) and zoom strength. */
    public int zoomKey = org.lwjgl.glfw.GLFW.GLFW_KEY_C;
    public float zoomFactor = 4.0f;
    /** Freecam toggle key (GLFW key code, default F6) and fly speed multiplier. */
    public int freecamKey = org.lwjgl.glfw.GLFW.GLFW_KEY_F6;
    public float freecamSpeed = 1.0f;
    /** Brightness the player had before Fullbright switched it up (-1 = none stored). */
    public double savedGamma = -1;
    /** Tier tagger source: PVP (PvPTiers), MC (MCTiers) or BOTH. URL templates: %s = player name. */
    public String tierMode = "BOTH";
    public String tierUrlMc = "https://mctiers.com/api/search_profile/%s";
    public String tierUrlPvp = "https://pvptiers.com/api/search_profile/%s";
    /** Optional text file URL (one player name per line) listing other players that use this mod. */
    public String userListUrl = "";
    public Map<String, Float> itemScales = new LinkedHashMap<>();

    // ---- Custom Item Glint (colors + opacity live in the "glint" module: bg = items, text = armor)
    /** true = use the custom glint, false = vanilla glint (the module switch must be on for either to apply). */
    public boolean glintCustom = true;
    /** Glint brightness in percent: 50 = vanilla strength, 100 = twice as bright, 0 = invisible. */
    public int glintIntensity = 50;
    /** Which kinds of enchanted items show a glint while the custom glint is active. */
    public boolean glintWeapons = true, glintArmor = true, glintTools = true, glintOther = true;

    // ---- No Darkness
    /** How much of the Darkness effect is removed, in percent (100 = completely). */
    public int darknessReduction = 100;
    /** Vanilla "Darkness Pulsing" option value stored while No Darkness changes it (-1 = none stored). */
    public double savedDarknessScale = -1;

    // ---- Free Look
    /** Free Look key (GLFW key code, default Left Alt) and whether it is hold-to-look (false = press to toggle). */
    public int freeLookKey = org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_ALT;
    public boolean freeLookHold = false;

    // ---- Hit Color (the module switch is the master toggle)
    /** ARMOR = armor only, BODY = body / skin only, BOTH. */
    public String hitMode = "BOTH";
    /** Separate switch for armor hit coloring (used by ARMOR and BOTH). */
    public boolean hitArmor = true;
    /** Hit color as RGB (alpha ignored). */
    public int hitColor = HitColor.DEF_COLOR;
    /** How long the effect lasts, in milliseconds (100 - 2000). */
    public int hitDuration = HitColor.DEF_DURATION;
    /** Strength in percent (0 - 100). */
    public int hitIntensity = HitColor.DEF_INTENSITY;

    // ---- menu appearance (see Theme): defaults = Blue Glass, the original look
    /** ThemeColor name: BLACK WHITE YELLOW RED BLUE PURPLE BROWN GREY PINK GREEN LIGHT_BLUE ORANGE. */
    public String themeColor = "LIGHT_BLUE";
    /** true = Glass mode, false = Solid mode. */
    public boolean themeGlass = true;
    /** Glass body opacity in percent (15-90). */
    public int glassOpacity = Theme.DEF_OPACITY;
    /** Border intensity in percent (0-100). */
    public int borderIntensity = Theme.DEF_BORDER;
    /** Highlight (sheen) intensity in percent (0-100). */
    public int highlightIntensity = Theme.DEF_HIGHLIGHT;
    /** Settings > GENERAL > "Module Icons": false hides every module icon in the Edit Hub (default on; missing in old configs = on). */
    public boolean showModuleIcons = true;
    /** Short hover / scroll / pop-up animations in the menu. */
    public boolean menuAnimations = true;
    /** Sky-style main menu replaces the vanilla title screen / sky-blue glass look for HUD modules. */
    public boolean skyTitle = true;
    public boolean skyHud = true;
    /** Sky photo behind Multiplayer and Mod Menu. */
    public boolean skyScreens = true;
    /** One-time switch of the menu theme to Sky Blue glass. */
    public boolean skyThemeApplied = false;

    public static Config get() {
        if (instance == null) load();
        return instance;
    }

    public static void load() {
        try (Reader r = Files.newBufferedReader(FILE)) {
            instance = GSON.fromJson(r, Config.class);
        } catch (Exception ignored) {
        }
        if (instance == null) instance = new Config();
        if (instance.modules == null) instance.modules = new LinkedHashMap<>();
        if (instance.friends == null) instance.friends = new ArrayList<>();
        if (instance.waypointList == null) instance.waypointList = new ArrayList<>();
        if (instance.hitboxes == null) instance.hitboxes = new LinkedHashMap<>();
        if (instance.itemScales == null) instance.itemScales = new LinkedHashMap<>();
        if (instance.dropScale <= 0f) instance.dropScale = 1.0f;
        if (instance.freecamSpeed <= 0f) instance.freecamSpeed = 1.0f;
        instance.glintIntensity = Math.max(0, Math.min(100, instance.glintIntensity));
        instance.darknessReduction = Math.max(0, Math.min(100, instance.darknessReduction));
        if (instance.freeLookKey <= 0) instance.freeLookKey = org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_ALT;
        if (!instance.skyThemeApplied) { instance.themeColor = "LIGHT_BLUE"; instance.themeGlass = true; instance.skyThemeApplied = true; }
        HitColor.sanitize(instance);
        Theme.sanitize(instance);
        for (ModuleCfg m : instance.modules.values()) {
            if (m.scale <= 0f) m.scale = 1.0f;
            if (m.armorMode == null) m.armorMode = "NUMBER";
            if (m.armorSide == null) m.armorSide = "RIGHT";
        }
    }

    public static void save() {
        try (Writer w = Files.newBufferedWriter(FILE)) {
            GSON.toJson(get(), w);
        } catch (Exception ignored) {
        }
    }
}
