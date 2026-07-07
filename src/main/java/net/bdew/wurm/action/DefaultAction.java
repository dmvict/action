package net.bdew.wurm.action;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeSet;

import com.wurmonline.client.game.inventory.InventoryMetaItem;
import com.wurmonline.client.renderer.PickableUnit;
import com.wurmonline.client.renderer.gui.HeadsUpDisplay;

@SuppressWarnings("unchecked")
public class DefaultAction {
    // TODO: probably there are constants that could be used instead of literals
    static final Path CONFIG_PATH = Paths.get("mods/action", "act_default.properties");
    static final String DEFAULT_OPTION_NAME = "default";

    public static short[] defaultEntry = {(short) 1, (short) 1};
    public Map<String, short[]> activatedDefaultProps = new HashMap<String, short[]>();
    public Map<String, short[]> areaDefaultProps = new HashMap<String, short[]>();
    public Map<String, short[]> bodyDefaultProps = new HashMap<String, short[]>();
    public Map<String, short[]> eqDefaultProps = new HashMap<String, short[]>();
    public Map<String, short[]> hoverDefaultProps = new HashMap<String, short[]>();
    public Map<String, short[]> nearbyDefaultProps = new HashMap<String, short[]>();
    public Map<String, short[]> selectedDefaultProps = new HashMap<String, short[]>();
    public Map<String, short[]> tbDefaultProps = new HashMap<String, short[]>();
    public Map<String, short[]> tileDefaultProps = new HashMap<String, short[]>();
    public Map<String, short[]> tileEDefaultProps = new HashMap<String, short[]>();
    public Map<String, short[]> tileNDefaultProps = new HashMap<String, short[]>();
    public Map<String, short[]> tileNEDefaultProps = new HashMap<String, short[]>();
    public Map<String, short[]> tileNWDefaultProps = new HashMap<String, short[]>();
    public Map<String, short[]> tileSDefaultProps = new HashMap<String, short[]>();
    public Map<String, short[]> tileSEDefaultProps = new HashMap<String, short[]>();
    public Map<String, short[]> tileSWDefaultProps = new HashMap<String, short[]>();
    public Map<String, short[]> tileWDefaultProps = new HashMap<String, short[]>();
    public Map<String, short[]> toolbeltDefaultProps = new HashMap<String, short[]>();

    private static Map<String, Patterns> patterns = new HashMap<String, Patterns>();

    public static enum Action {
        DEFAULT(0),
        ALT(1);

        private final int value;

        private Action(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    public static DefaultAction setup() throws Throwable {
        DefaultAction defaultAction = new DefaultAction();

        // Get config for default actions and parse options
        Properties props = new Properties();
        if (Files.exists(CONFIG_PATH)) {
            try (InputStream in = Files.newInputStream(CONFIG_PATH)) {
                props.load(in);
                System.out.println("Loaded config for default actions from " + CONFIG_PATH);          
            } catch (IOException e) {
                throw new Throwable(e.toString());
            }
        }
        List<String> keys = new ArrayList<>(props.stringPropertyNames());
        Collections.sort(keys);

        // order of adding of props is alphabetical since the keys are sorted alphabetically
        // items handled related to the Target enum
        // ACTIVATED 
        // AREA
        // BODY
        // EQ
        // HOVER
        // NEARBY
        // SELECTED
        // TB
        // TILE
        // TILE_E
        // TILE_N
        // TILE_NE
        // TILE_NW
        // TILE_S
        // TILE_SE
        // TILE_SW
        // TILE_W
        // TOOLBELT

        int counter = 0;
        counter = defaultAction.fillProps(props, keys, Target.ACTIVATED.name().toLowerCase(), counter, defaultAction.activatedDefaultProps);
        counter = defaultAction.fillProps(props, keys, Target.AREA.name().toLowerCase(), counter, defaultAction.areaDefaultProps);
        counter = defaultAction.fillProps(props, keys, Target.BODY.name().toLowerCase(), counter, defaultAction.bodyDefaultProps);
        counter = defaultAction.fillProps(props, keys, Target.EQ.name().toLowerCase(), counter, defaultAction.eqDefaultProps);
        counter = defaultAction.fillProps(props, keys, Target.HOVER.name().toLowerCase(), counter, defaultAction.hoverDefaultProps);
        counter = defaultAction.fillProps(props, keys, Target.NEARBY.name().toLowerCase(), counter, defaultAction.nearbyDefaultProps);
        counter = defaultAction.fillProps(props, keys, Target.SELECTED.name().toLowerCase(), counter, defaultAction.selectedDefaultProps);
        counter = defaultAction.fillProps(props, keys, Target.TB.name().toLowerCase(), counter, defaultAction.tbDefaultProps);
        counter = defaultAction.fillProps(props, keys, Target.TILE.name().toLowerCase(), counter, defaultAction.tileDefaultProps);
        counter = defaultAction.fillProps(props, keys, Target.TILE_E.name().toLowerCase(), counter, defaultAction.tileEDefaultProps);
        counter = defaultAction.fillProps(props, keys, Target.TILE_N.name().toLowerCase(), counter, defaultAction.tileNDefaultProps);
        counter = defaultAction.fillProps(props, keys, Target.TILE_NE.name().toLowerCase(), counter, defaultAction.tileNEDefaultProps);
        counter = defaultAction.fillProps(props, keys, Target.TILE_NW.name().toLowerCase(), counter, defaultAction.tileNWDefaultProps);
        counter = defaultAction.fillProps(props, keys, Target.TILE_S.name().toLowerCase(), counter, defaultAction.tileSDefaultProps);
        counter = defaultAction.fillProps(props, keys, Target.TILE_SE.name().toLowerCase(), counter, defaultAction.tileSEDefaultProps);
        counter = defaultAction.fillProps(props, keys, Target.TILE_SW.name().toLowerCase(), counter, defaultAction.tileSWDefaultProps);
        counter = defaultAction.fillProps(props, keys, Target.TILE_W.name().toLowerCase(), counter, defaultAction.tileWDefaultProps);
        defaultAction.fillProps(props, keys, Target.TOOLBELT.name().toLowerCase(), counter, defaultAction.toolbeltDefaultProps);

        //

        return defaultAction;
    }

    private int fillProps(Properties props, List<String> keys, final String sectionName, int startPos, Map<String, short[]> dst) {
        final int keysSize = keys.size();
        if (startPos >= keysSize) {
            return startPos;
        }

        String key = keys.get(startPos);
        while (key.startsWith(sectionName) && startPos < keysSize) {
            Optional<String> defaultActionNameE = DefaultAction.getPropertyName(key, sectionName);
            if (!defaultActionNameE.isPresent()) {
                System.out.println("Failed to read section property. Format is [section_name].[section_value]");
                startPos += 1;
                key = keys.get(startPos);
                continue;
            }

            Optional<short[]> actionsE = DefaultAction.getActions(props.getProperty(key), (short) 1);
            if (actionsE.isPresent()) {
                String defaultActionName = defaultActionNameE.get();
                short[] actions = actionsE.get();

                boolean startsWithAsterisk = defaultActionName.startsWith("*");
                boolean endsWithAsterisk = defaultActionName.endsWith("*");
                if (startsWithAsterisk || endsWithAsterisk) {
                    Patterns sectionPatterns = patterns.get(sectionName);
                    String patternName = null;
                    if (sectionPatterns == null) {
                        sectionPatterns = new Patterns();
                        patterns.put(sectionName, sectionPatterns);
                    }
                    if (startsWithAsterisk && endsWithAsterisk) {
                        patternName = defaultActionName.substring(1, defaultActionName.length() - 1);
                        sectionPatterns.containsList.add(patternName);
                    } else if (startsWithAsterisk) {
                        patternName = defaultActionName.substring(1);
                        sectionPatterns.endsWithList.add(patternName);
                    } else {
                        patternName = defaultActionName.substring(0, defaultActionName.length() - 1);
                        sectionPatterns.startsWithList.add(patternName);
                    }
                    sectionPatterns.patterns.put(patternName, actions);
                } else {
                    dst.put(defaultActionName, actions);
                }
            }

            startPos += 1;
            if (startPos < keysSize) {
                key = keys.get(startPos);
            }
        }

        return startPos;
    }

    private static Optional<String> getPropertyName(final String key, final String sectionName) {
        String[] keyParts = key.split("\\.", 2);
        if (keyParts.length == 1) {
            System.out.println("Failed to read section property. Format is [section_name].[section_value]");
            return Optional.empty();
        } else {
            return Optional.of(keyParts[1]);
        }
    }

    private static Optional<short[]> getActions(final String actionsStr, final short defaultAct) {
        String[] actionParts = actionsStr.split("\\|");

        try {
            Short alt_act = null; 
            if (actionParts.length == 1) {
                alt_act = defaultAct;
            } else {
                alt_act = Short.parseShort(actionParts[1]);
            }
            short[] entry = {Short.parseShort(actionParts[0]), alt_act};
            return Optional.of(entry);
        } catch (NumberFormatException nfe) {
            System.out.println("Failed to read property values. Should be numbers divided by bar: [n1]|[n2] or single number.");
            return Optional.empty();
        }
    }

    public Optional<Short> getAction(final Target target, final Action actionE, final HeadsUpDisplay hud) {
        Short act_id = null;
        int action = actionE.getValue();
        Patterns pats = patterns.get(target.name().toLowerCase());
        // Used if-else pattern instead of switch.
        // In comparison to Rust, Java doesn't track all enum members
        // and it is more safe to use this statements
        if (target == Target.HOVER) {
            PickableUnit obj = hud.getWorld().getCurrentHoveredObject();
            String obj_name = null; 
            if (obj == null) {
                Optional<InventoryMetaItem> t = Reflect.getActiveToolItem(hud);
                if (t.isPresent()) {
                    InventoryMetaItem item = t.get();
                    obj_name = item.getBaseName();
                } else {
                    obj_name = DEFAULT_OPTION_NAME;
                }
            } else {
                obj_name = obj.getHoverName();
            }
            act_id = DefaultAction.getActionIdOrUpdateFromPatterns(this.hoverDefaultProps, obj_name, pats, action);
        } else if (target == Target.BODY) {
            Optional<InventoryMetaItem> itemOpt = Reflect.getBodyItem(hud.getPaperDollInventory());
            if (itemOpt.isPresent()) {
                InventoryMetaItem item = itemOpt.get();
                act_id = DefaultAction.getActionIdOrUpdateFromPatterns(this.bodyDefaultProps, item.getBaseName(), pats, action);
            }
        } else if (target == Target.TILE) {
            act_id = DefaultAction.getActionIdOrUpdateFromPatterns(this.tileDefaultProps, DEFAULT_OPTION_NAME, pats, action);
        } else if (target == Target.TILE_N) {
            act_id = DefaultAction.getActionIdOrUpdateFromPatterns(this.tileNDefaultProps, DEFAULT_OPTION_NAME, pats, action);
        } else if (target == Target.TILE_W) {
            act_id = DefaultAction.getActionIdOrUpdateFromPatterns(this.tileWDefaultProps, DEFAULT_OPTION_NAME, pats, action);
        } else if (target == Target.TILE_NW) {
            act_id = DefaultAction.getActionIdOrUpdateFromPatterns(this.tileNWDefaultProps, DEFAULT_OPTION_NAME, pats, action);
        } else if (target == Target.TILE_NE) {
            act_id = DefaultAction.getActionIdOrUpdateFromPatterns(this.tileNEDefaultProps, DEFAULT_OPTION_NAME, pats, action);
        } else if (target == Target.TILE_S) {
            act_id = DefaultAction.getActionIdOrUpdateFromPatterns(this.tileSDefaultProps, DEFAULT_OPTION_NAME, pats, action);
        } else if (target == Target.TILE_E) {
            act_id = DefaultAction.getActionIdOrUpdateFromPatterns(this.tileEDefaultProps, DEFAULT_OPTION_NAME, pats, action);
        } else if (target == Target.TILE_SE) {
            act_id = DefaultAction.getActionIdOrUpdateFromPatterns(this.tileSEDefaultProps, DEFAULT_OPTION_NAME, pats, action);
        } else if (target == Target.TILE_SW) {
            act_id = DefaultAction.getActionIdOrUpdateFromPatterns(this.tileSWDefaultProps, DEFAULT_OPTION_NAME, pats, action);
        } else if (target == Target.AREA) {
            act_id = DefaultAction.getActionIdOrUpdateFromPatterns(this.areaDefaultProps, DEFAULT_OPTION_NAME, pats, action);
        } else if (target == Target.ACTIVATED) {
            Optional<InventoryMetaItem> t = Reflect.getActiveToolItem(hud);
            if (t.isPresent()) {
                InventoryMetaItem item = t.get();
                act_id = DefaultAction.getActionIdOrUpdateFromPatterns(this.activatedDefaultProps, item.getBaseName(), pats, action);
            }
        } else if (target == Target.SELECTED) {
            Optional<PickableUnit> p = Reflect.getSelectedUnit(hud.getSelectBar());
            if (p.isPresent()) {
                act_id = DefaultAction.getActionIdOrUpdateFromPatterns(this.selectedDefaultProps, p.get().getHoverName(), pats, action);
            }
        } else if (target == Target.TOOLBELT) {
            String toolbeltItemSelector = null;

            PickableUnit obj = hud.getWorld().getCurrentHoveredObject();
            if (obj != null) {
                toolbeltItemSelector = obj.getHoverName();
            } else {

                Optional<PickableUnit> p = Reflect.getSelectedUnit(hud.getSelectBar());
                if (p.isPresent()) {
                    toolbeltItemSelector = p.get().getHoverName();
                } else {
                    toolbeltItemSelector = DEFAULT_OPTION_NAME;
                }
            }
        
            act_id = DefaultAction.getActionIdOrUpdateFromPatterns(this.toolbeltDefaultProps, toolbeltItemSelector, pats, action);
        } else if (target == Target.TB) {
            act_id = DefaultAction.getActionIdOrUpdateFromPatterns(this.tbDefaultProps, Integer.toString(target.getId()), pats, action);
        } else if (target == Target.EQ) {
            act_id = DefaultAction.getActionIdOrUpdateFromPatterns(this.eqDefaultProps, Integer.toString(target.getId()), pats, action);
        } else if (target == Target.NEARBY) {
            act_id = DefaultAction.getActionIdOrUpdateFromPatterns(this.nearbyDefaultProps, Integer.toString(target.getId()), pats, action);
        }

        //
        
        if (act_id != null) {
            return Optional.of(act_id);
        }
        return Optional.empty();
    }

    private static short getActionIdOrUpdateFromPatterns(Map<String, short[]> container, final String item_name, final Patterns patterns, final int action) {
        short act_id = defaultEntry[action];
        short[] act_ids = container.get(item_name);
        if (act_ids == null) {
            if (patterns != null) {
                short[] entry = patterns.get(item_name);
                if (entry != null) {
                    container.put(item_name, entry);
                    act_id = entry[action];
                } 
            }
            // double check
            if (act_id == defaultEntry[action]) {
                short[] default_ids = container.get(DEFAULT_OPTION_NAME);
                if (default_ids != null) {
                    act_id = default_ids[action];
                }
            }
        } else {
            act_id = act_ids[action];
        }
        return act_id;
    }

    /* utilitary */

    private static class Patterns {
        public List<String> startsWithList = new ArrayList<>();
        public List<String> endsWithList = new ArrayList<>();
        public List<String> containsList = new ArrayList<>();

        public Map<String, short[]> patterns = new HashMap<String, short[]>();

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("Patterns {");
            sb.append("startsWithList=").append(startsWithList.toString()).append('\'');
            sb.append(", endsWithList=").append(endsWithList.toString());
            sb.append(", containsList=").append(containsList.toString());
            sb.append(", patterns=").append(patterns.toString());
            sb.append('}');
            return sb.toString(); 
        }

        public short[] get(final String src) {
            for (String p: this.startsWithList) {
                if (src.startsWith(p)) {
                    return this.patterns.get(p);
                }
            }
            for (String p: this.endsWithList) {
                if (src.endsWith(p)) {
                    return this.patterns.get(p);
                }
            }
            for (String p: this.containsList) {
                if (src.contains(p)) {
                    return this.patterns.get(p);
                }
            }
            return null;
        }
    }

    private static String shortArrayToString(short[] arr) {
        StringJoiner joiner = new StringJoiner("|");
        for (short v : arr) {
            joiner.add(String.valueOf(v));
        }
        return joiner.toString();
    }

    private class SortedProperties extends Properties {

        @Override
        public Set<Object> keySet() {
            return new TreeSet<>(super.keySet());
        }

        @Override
        public Set<Map.Entry<Object, Object>> entrySet() {
            Set<Map.Entry<Object, Object>> sortedSet = new TreeSet<>(
                Comparator.comparing(e -> String.valueOf(e.getKey()))
            );
            sortedSet.addAll(super.entrySet());
            return sortedSet;
        }

        @Override
        public synchronized Enumeration<Object> keys() {
            return Collections.enumeration(new TreeSet<>(super.keySet()));
        }
    }

    private SortedProperties toProperties() {
        SortedProperties props = new SortedProperties();

        for(Target k: Target.values()) {
            try {
                String keyName = k.name().toLowerCase();
                String[] prefixParts = keyName.split("_");
                if (prefixParts.length == 2) {
                    prefixParts[1] = prefixParts[1].toUpperCase();
                }
                String prefix = String.join("", prefixParts);
                Field field = this.getClass().getDeclaredField(prefix + "DefaultProps");
                field.setAccessible(true);
                HashMap<String, short[]> fieldValue = (HashMap<String, short[]>) field.get(this);
                if (fieldValue != null) {
                    for (Map.Entry<String, short[]> entry : fieldValue.entrySet()) {
                        props.setProperty(prefix + "." + entry.getKey(), shortArrayToString(entry.getValue()));
                    }
                }
                
                Patterns currentPatterns = patterns.get(keyName);
                if (currentPatterns != null) {
                    currentPatterns.startsWithList.stream().forEach(p -> props.setProperty(prefix + "." + p + "*", shortArrayToString(currentPatterns.patterns.get(p))));
                    currentPatterns.endsWithList.stream().forEach(p -> props.setProperty(prefix + ".*" + p, shortArrayToString(currentPatterns.patterns.get(p))));
                    currentPatterns.containsList.stream().forEach(p -> props.setProperty(prefix + ".*" + p + "*", shortArrayToString(currentPatterns.patterns.get(p))));
                }
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new RuntimeException("Could not access field:", e);
            }            
        }

        return props;
    }

    public void saveProperties(final String path) throws IOException {
        FileOutputStream out = new FileOutputStream(path);
        this.toProperties().store(out, "Default config updated at:");
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("DefaultAction {");
        sb.append("activatedDefaultProps=").append(activatedDefaultProps.toString()).append('\'');
        sb.append(", areaDefaultProps=").append(areaDefaultProps.toString());
        sb.append(", bodyDefaultProps=").append(bodyDefaultProps.toString());
        sb.append(", eqDefaultProps=").append(eqDefaultProps.toString());
        sb.append(", hoverDefaultProps=").append(hoverDefaultProps.toString());
        sb.append(", nearbyDefaultProps=").append(nearbyDefaultProps.toString());
        sb.append(", selectedDefaultProps=").append(selectedDefaultProps.toString());
        sb.append(", tbDefaultProps=").append(tbDefaultProps.toString());
        sb.append(", tileDefaultProps=").append(tileDefaultProps.toString());
        sb.append(", tileEDefaultProps=").append(tileEDefaultProps.toString());
        sb.append(", tileNDefaultProps=").append(tileNDefaultProps.toString());
        sb.append(", tileNEDefaultProps=").append(tileNEDefaultProps.toString());
        sb.append(", tileNWDefaultProps=").append(tileNWDefaultProps.toString());
        sb.append(", tileSDefaultProps=").append(tileSDefaultProps.toString());
        sb.append(", tileSEDefaultProps=").append(tileSEDefaultProps.toString());
        sb.append(", tileSWDefaultProps=").append(tileSWDefaultProps.toString());
        sb.append(", tileWDefaultProps=").append(tileWDefaultProps.toString());
        sb.append(", toolbeltDefaultProps=").append(toolbeltDefaultProps.toString());
        sb.append(", patterns=");
        patterns.keySet().stream().forEach(key -> sb.append(", " + key + "=").append(patterns.get(key)));
        sb.append('}');
        return sb.toString(); 
    }
}
