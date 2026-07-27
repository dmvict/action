package net.bdew.wurm.action;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashSet;
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
    public Map<Target, Map<String, short[]>> defaultProps = new HashMap<>();

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
        return loadDefaultActios(defaultAction);
    }

    public static DefaultAction loadDefaultActios(DefaultAction defaultAction) throws Throwable {

        // Get config for default actions and parse options
        OrderedProperties props = new OrderedProperties();
        if (Files.exists(CONFIG_PATH)) {
            try (InputStream in = Files.newInputStream(CONFIG_PATH)) {
                props.load(in);
                System.out.println("Loaded config for default actions from " + CONFIG_PATH);          
            } catch (IOException e) {
                throw new Throwable(e.toString());
            }
        }

        defaultAction.fillProps(props);

        return defaultAction;
    }

    private void fillProps(OrderedProperties props) {

        List<String> keys = new ArrayList<>(props.stringPropertyNames());

        for (String key: keys) {

            Optional<String[]> actionSplitsE = DefaultAction.getPropertyName(key);
            if (!actionSplitsE.isPresent()) {
                System.out.println("Failed to read section property. Format is [section_name].[section_value]");
                System.out.println("Skip and continue");
                continue;
            }

            Optional<short[]> actionsE = DefaultAction.getActions(props.getProperty(key), (short) 1);

            if (actionsE.isPresent()) {
                Optional<Target> targetSectionNameE = Target.parseTargetSafe(actionSplitsE.get()[0].trim());
                if (!targetSectionNameE.isPresent()) {
                    System.out.println("Invalid section name: " + actionSplitsE.get()[0]);
                    System.out.println("Skip and continue");
                    continue;
                }
                Target targetSection = targetSectionNameE.get();
                String targetSectionName = targetSection.name().toLowerCase();
                String defaultActionName = actionSplitsE.get()[1].trim();
                short[] actions = actionsE.get();

                boolean startsWithAsterisk = defaultActionName.startsWith("*");
                boolean endsWithAsterisk = defaultActionName.endsWith("*");
                if (startsWithAsterisk || endsWithAsterisk) {
                    Patterns sectionPatterns = patterns.get(targetSectionName);
                    String patternName = null;
                    if (sectionPatterns == null) {
                        sectionPatterns = new Patterns();
                        patterns.put(targetSectionName, sectionPatterns);
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
                    Map<String, short[]> dst = this.defaultProps.get(targetSection);
                    if (dst == null) {
                        dst = new HashMap<>();
                        this.defaultProps.put(targetSection, dst);
                    }
                    dst.put(defaultActionName, actions);
                }
            }
        }
    }

    private static Optional<String[]> getPropertyName(final String key) {
        String[] keyParts = key.split("\\.", 2);
        if (keyParts.length == 1) {
            System.out.println("Failed to read section property. Format is [section_name].[section_value]");
            return Optional.empty();
        } else {
            return Optional.of(keyParts);
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
            act_id = DefaultAction.getActionIdOrUpdateFromPatterns(this.defaultProps.getOrDefault(target, new HashMap<>()), obj_name, pats, action);
        } else if (target == Target.BODY) {
            Optional<InventoryMetaItem> itemOpt = Reflect.getBodyItem(hud.getPaperDollInventory());
            if (itemOpt.isPresent()) {
                InventoryMetaItem item = itemOpt.get();
                act_id = DefaultAction.getActionIdOrUpdateFromPatterns(this.defaultProps.getOrDefault(target, new HashMap<>()), item.getBaseName(), pats, action);
            }
        } else if (target == Target.ACTIVATED) {
            Optional<InventoryMetaItem> t = Reflect.getActiveToolItem(hud);
            if (t.isPresent()) {
                InventoryMetaItem item = t.get();
                act_id = DefaultAction.getActionIdOrUpdateFromPatterns(this.defaultProps.getOrDefault(target, new HashMap<>()), item.getBaseName(), pats, action);
            }
        } else if (target == Target.SELECTED) {
            Optional<PickableUnit> p = Reflect.getSelectedUnit(hud.getSelectBar());
            if (p.isPresent()) {
                act_id = DefaultAction.getActionIdOrUpdateFromPatterns(this.defaultProps.getOrDefault(target, new HashMap<>()), p.get().getHoverName(), pats, action);
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
        
            act_id = DefaultAction.getActionIdOrUpdateFromPatterns(this.defaultProps.getOrDefault(target, new HashMap<>()), toolbeltItemSelector, pats, action);
        } else if (target == Target.TB || target == Target.EQ || target == Target.NEARBY) {
            act_id = DefaultAction.getActionIdOrUpdateFromPatterns(this.defaultProps.getOrDefault(target, new HashMap<>()), Integer.toString(target.getId()), pats, action);
        } else {
            act_id = DefaultAction.getActionIdOrUpdateFromPatterns(this.defaultProps.getOrDefault(target, new HashMap<>()), DEFAULT_OPTION_NAME, pats, action);
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

    private static class OrderedProperties extends Properties {
        private final LinkedHashSet<Object> insertionOrder = new LinkedHashSet<>();

        @Override
        public synchronized Object put(Object key, Object value) {
            insertionOrder.add(key);
            return super.put(key, value);
        }

        @Override
        public Set<Object> keySet() {
            return insertionOrder;
        }

        @Override
        public Set<String> stringPropertyNames() {
            LinkedHashSet<String> ordered = new LinkedHashSet<>();
            for (Object key : insertionOrder) {
                if (key instanceof String && get(key) instanceof String) {
                    ordered.add((String) key);
                }
            }
            return ordered;
        }

        @Override
        public Set<Map.Entry<Object, Object>> entrySet() {
            Set<Map.Entry<Object, Object>> ordered = new LinkedHashSet<>();
            for (Object key : insertionOrder) {
                ordered.add(new AbstractMap.SimpleEntry<>(key, get(key)));
            }
            return ordered;
        }

        @Override
        public synchronized Enumeration<Object> keys() {
            return Collections.enumeration(insertionOrder);
        }
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
            String keyName = k.name().toLowerCase();
            String[] prefixParts = keyName.split("_");
            if (prefixParts.length == 2) {
                prefixParts[1] = prefixParts[1].toUpperCase();
            }
            String prefix = String.join("", prefixParts);
            HashMap<String, short[]> fieldValue = (HashMap<String, short[]>) this.defaultProps.get(k);
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
        sb.append("activatedDefaultProps=").append(defaultProps.get(Target.ACTIVATED).toString()).append('\'');
        sb.append(", areaDefaultProps=").append(defaultProps.get(Target.AREA).toString());
        sb.append(", bodyDefaultProps=").append(defaultProps.get(Target.BODY).toString());
        sb.append(", eqDefaultProps=").append(defaultProps.get(Target.EQ).toString());
        sb.append(", hoverDefaultProps=").append(defaultProps.get(Target.HOVER).toString());
        sb.append(", nearbyDefaultProps=").append(defaultProps.get(Target.NEARBY).toString());
        sb.append(", selectedDefaultProps=").append(defaultProps.get(Target.SELECTED).toString());
        sb.append(", tbDefaultProps=").append(defaultProps.get(Target.TB).toString());
        sb.append(", tileDefaultProps=").append(defaultProps.get(Target.TILE).toString());
        sb.append(", tileEDefaultProps=").append(defaultProps.get(Target.TILE_E).toString());
        sb.append(", tileNDefaultProps=").append(defaultProps.get(Target.TILE_N).toString());
        sb.append(", tileNEDefaultProps=").append(defaultProps.get(Target.TILE_NE).toString());
        sb.append(", tileNWDefaultProps=").append(defaultProps.get(Target.TILE_NW).toString());
        sb.append(", tileSDefaultProps=").append(defaultProps.get(Target.TILE_S).toString());
        sb.append(", tileSEDefaultProps=").append(defaultProps.get(Target.TILE_SE).toString());
        sb.append(", tileSWDefaultProps=").append(defaultProps.get(Target.TILE_SW).toString());
        sb.append(", tileWDefaultProps=").append(defaultProps.get(Target.TILE_W).toString());
        sb.append(", toolbeltDefaultProps=").append(defaultProps.get(Target.TOOLBELT).toString());
        sb.append(", patterns=");
        patterns.keySet().stream().forEach(key -> sb.append(", " + key + "=").append(patterns.get(key)));
        sb.append('}');
        return sb.toString(); 
    }
}
