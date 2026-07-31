package net.bdew.wurm.action;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.StringJoiner;

import com.wurmonline.client.game.inventory.InventoryMetaItem;
import com.wurmonline.client.renderer.PickableUnit;
import com.wurmonline.client.renderer.gui.HeadsUpDisplay;

@SuppressWarnings("unchecked")
public class DefaultAction {
    // TODO: probably there are constants that could be used instead of literals
    static final Path CONFIG_PATH = Paths.get("mods/action", "act_default.properties");
    static final String DEFAULT_OPTION_NAME = "default";
    static final String RUNTIME_UPDATE_KEY = "runtime_update";

    public static short[] defaultEntry = {(short) 1, (short) 1};
    public Map<Target, Map<String, short[]>> defaultProps = new HashMap<>();
    public Map<String, Patterns> patterns = new HashMap<>();
    private boolean runtimeUpdate = false;

    public static enum ActionKind {
        DEFAULT(0),
        ALT(1);

        private final int value;

        private ActionKind(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    public static enum ActionIds {
        REGULAR(defaultEntry.clone()),
        ACTIVATED(defaultEntry.clone());

        private short[] value;

        private ActionIds(short[] value) {
            this.value = value;
        }

        public short[] getValue() {
            return value;
        }

        public void setValue(final short[] value) {
            this.value = value;
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

        defaultAction.runtimeUpdate = Boolean.parseBoolean(props.getProperty(RUNTIME_UPDATE_KEY, "false"));
        props.remove(RUNTIME_UPDATE_KEY);
        defaultAction.fillProps(props);

        return defaultAction;
    }

    private void fillProps(OrderedProperties props) {

        List<String> keys = new ArrayList<>(props.stringPropertyNames());

        for (String key: keys) {

            Optional<String[]> actionPartsOpt = DefaultAction.getPropertyParts(key);
            if (!actionPartsOpt.isPresent()) {
                System.out.println("Failed to read section property. Format is [section_name].[section_value]");
                System.out.println("Skip and continue");
                continue;
            }

            Optional<short[]> actionsE = DefaultAction.getActions(props.getProperty(key), (short) 1);

            if (actionsE.isPresent()) {
                Optional<Target> targetSectionNameOpt = Target.parseTargetSafe(actionPartsOpt.get()[0].trim());
                if (!targetSectionNameOpt.isPresent()) {
                    System.out.println("Invalid section name: " + actionPartsOpt.get()[0]);
                    System.out.println("Skip and continue");
                    continue;
                }
                Target targetSection = targetSectionNameOpt.get();
                String targetSectionName = targetSection.name().toLowerCase();
                String defaultActionKey = actionPartsOpt.get()[1].trim();
                short[] actions = actionsE.get();

                if (defaultActionKey.contains("*") || defaultActionKey.contains("&&")) {
                    Patterns sectionPatterns = patterns.get(targetSectionName);
                    if (sectionPatterns == null) {
                        sectionPatterns = new Patterns();
                        patterns.put(targetSectionName, sectionPatterns);
                    }
                    sectionPatterns.add(new Pattern(defaultActionKey), actions);
                } else {
                    Map<String, short[]> dst = this.defaultProps.get(targetSection);
                    if (dst == null) {
                        dst = new LinkedHashMap<>();
                        this.defaultProps.put(targetSection, dst);
                    }
                    dst.put(defaultActionKey, actions);
                }
            }
        }
    }

    private static Optional<String[]> getPropertyParts(final String key) {
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
            Short altAct = null; 
            if (actionParts.length == 1) {
                altAct = defaultAct;
            } else {
                altAct = Short.parseShort(actionParts[1]);
            }
            short[] entry = {Short.parseShort(actionParts[0]), altAct};
            return Optional.of(entry);
        } catch (NumberFormatException nfe) {
            System.out.println("Failed to read property values. Should be numbers divided by bar: [n1]|[n2] or single number.");
            return Optional.empty();
        }
    }

    public Short getAction(final Target target, final ActionKind actionE, final HeadsUpDisplay hud) {
        int action = actionE.getValue();
        Patterns pats = patterns.get(target.name().toLowerCase());
        Optional<InventoryMetaItem> activated = Reflect.getActiveToolItem(hud);
        final Optional<String> activatedName = activated.map(item -> item.getBaseName());
        // Used if-else pattern instead of switch.
        // In comparison to Rust, Java doesn't track all enum members
        // and it is more safe to use this statements
        String baseName = null;
        if (target == Target.HOVER) {
            PickableUnit obj = hud.getWorld().getCurrentHoveredObject();
            if (obj == null) {
                if (activated.isPresent()) {
                    InventoryMetaItem item = activated.get();
                    baseName = item.getBaseName();
                } else {
                    baseName = DEFAULT_OPTION_NAME;
                }
            } else {
                baseName = obj.getHoverName();
            }
        } else if (target == Target.BODY) {
            Optional<InventoryMetaItem> itemOpt = Reflect.getBodyItem(hud.getPaperDollInventory());
            if (itemOpt.isPresent()) {
                baseName = itemOpt.get().getBaseName();
            }
        } else if (target == Target.ACTIVATED) {
            Optional<InventoryMetaItem> t = Reflect.getActiveToolItem(hud);
            if (activated.isPresent()) {
                baseName = activated.get().getBaseName();
            }
        } else if (target == Target.SELECTED) {
            Optional<PickableUnit> p = Reflect.getSelectedUnit(hud.getSelectBar());
            if (p.isPresent()) {
                baseName =  p.get().getHoverName();
            }
        } else if (target == Target.TOOLBELT) {
            PickableUnit obj = hud.getWorld().getCurrentHoveredObject();
            if (obj != null) {
                baseName = obj.getHoverName();
            } else {
                Optional<PickableUnit> p = Reflect.getSelectedUnit(hud.getSelectBar());
                if (p.isPresent()) {
                    baseName = p.get().getHoverName();
                } else {
                    baseName = DEFAULT_OPTION_NAME;
                }
            }
        } else if (target == Target.TB || target == Target.EQ || target == Target.NEARBY) {
            baseName = Integer.toString(target.getId());
        } else {
            baseName = DEFAULT_OPTION_NAME;
        }

        if (baseName == null) {
            baseName = DEFAULT_OPTION_NAME;
        }

        Short actId = DefaultAction.getActionIdOrUpdateFromPatterns(
            this.defaultProps.getOrDefault(target, new HashMap<>()), 
            this.runtimeUpdate,
            baseName, 
            pats, 
            activatedName,
            action
        );

        if (target == Target.TOOLBELT) {
            int arrangement = actId / 100;
            if (arrangement != 0) {
                hud.getToolBelt().loadArrangement(arrangement-1);
                actId = (short) (actId - (arrangement * 100));
            }
        }

        //
        
        return actId;
    }

    private static short getActionIdOrUpdateFromPatterns(
        Map<String, short[]> container, 
        boolean updateContainer,
        final String itemName, 
        final Patterns patterns, 
        final Optional<String> activated,
        final int action
    ) {
        short actId = defaultEntry[action];
        short[] actIds = container.get(itemName);
        if (actIds == null || itemName.equals(DEFAULT_OPTION_NAME)) {
            if (patterns != null) {
                final ActionIds entry = patterns.get(itemName, activated);
                if (updateContainer && entry == ActionIds.REGULAR && entry != null) {
                    container.put(itemName, entry.getValue());
                    actId = entry.getValue()[action];
                } else if (entry != null) {
                    actId = entry.getValue()[action];
                }
            }

            // double check that the value is changed otherwise it's making fallback to default
            if (actId == defaultEntry[action] && patterns != null) {
                final ActionIds entry = patterns.get(DEFAULT_OPTION_NAME, activated);
                if (entry != null) {
                    actId = entry.getValue()[action];
                }
            } 
            if (actId == defaultEntry[action]) {
                short[] defaultIds = container.get(DEFAULT_OPTION_NAME);
                if (defaultIds != null) {
                    actId = defaultIds[action];
                }
            }
        } else {
            actId = actIds[action];
        }
        return actId;
    }

    /* utilitary */

    private static enum Cmp {
        STARTS,
        CONTAINS,
        ENDS,
        EQ;
    }

    private static class PatternEntry {
        public String value;
        public Cmp cmp;

        PatternEntry(final String pattern) {
            boolean startsWithAsterisk = pattern.startsWith("*");
            boolean endsWithAsterisk = pattern.endsWith("*");
            if (startsWithAsterisk && endsWithAsterisk) {
                this.value = pattern.substring(1, pattern.length() - 1);
                this.cmp = Cmp.CONTAINS;
            } else if (startsWithAsterisk) {
                this.value = pattern.substring(1);
                this.cmp = Cmp.ENDS;
            } else if (endsWithAsterisk) {
                this.value = pattern.substring(0, pattern.length() - 1);
                this.cmp = Cmp.STARTS;
            } else {
                this.value = pattern;
                this.cmp = Cmp.EQ;
            }
        }


        public boolean matches(final String src) {
            if (this.cmp == Cmp.STARTS) {
                return src.startsWith(this.value);
            } else if (this.cmp == Cmp.ENDS) {
                return src.endsWith(this.value);
            } else if (this.cmp == Cmp.CONTAINS) {
                return src.contains(this.value);
            } else if (this.cmp == Cmp.EQ) {
                return src.equals(this.value); 
            } else {
                return false;
            }
        }

        @Override
        public String toString() {
            if (this.cmp == Cmp.STARTS) {
                return this.value + "*";
            } else if (this.cmp == Cmp.ENDS) {
                return "*" + this.value;
            } else if (this.cmp == Cmp.CONTAINS) {
                return "*" + this.value + "*";
            } else if (this.cmp == Cmp.EQ) {
                return this.value;
            } else {
                return "";
            }
        }
    }

    private static class Pattern {
        public PatternEntry firstEntry;
        public Optional<PatternEntry> secondEntry;
        private boolean secondEntryMatchActivated = false;

        Pattern(final String pattern) {
            String[] patternSplits = pattern.split("&&");
            if (patternSplits.length == 2) {
                this.firstEntry = new PatternEntry(patternSplits[0]);
                if (patternSplits[1].startsWith("@activated")) {
                    this.secondEntryMatchActivated = true;
                    this.secondEntry = Optional.of(new PatternEntry(patternSplits[1].substring(10)));
                } else {
                    this.secondEntry = Optional.of(new PatternEntry(patternSplits[1]));
                }
            } else {
                this.firstEntry = new PatternEntry(patternSplits[0]);
                this.secondEntry = Optional.empty();
            }
        }

        public boolean matches(final String src, final Optional<String> activated) {
            boolean matches = firstEntry.matches(src);
            if (matches && this.secondEntry.isPresent()) {
                if (this.secondEntryMatchActivated && activated.isPresent()) {
                    matches = secondEntry.get().matches(activated.get());
                } else {
                    matches = secondEntry.get().matches(src);
                }
            } 
            return matches;
        }

        @Override
        public String toString() {
            if (this.secondEntry.isPresent()) {
                if (this.secondEntryMatchActivated) {
                    return this.firstEntry.toString() + "&&@activated" + this.secondEntry.get().toString();
                } else {
                    return this.firstEntry.toString() + "&&" + this.secondEntry.get().toString();
                }
            } else {
                return this.firstEntry.toString();
            } 
        }
    }

    private static class Patterns {

        public List<Pattern> patternsList = new ArrayList<>();
        public Map<Pattern, short[]> patternsMap = new HashMap<>();

        public void add(final Pattern pattern, final short[] value) {
            this.patternsList.add(pattern);
            this.patternsMap.put(pattern, value);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("Patterns {");
            sb.append("patternsList=[");
            for (Pattern p: patternsList) {
                sb.append(p.firstEntry.toString() + 
                (p.secondEntry.isPresent() ? "&&" + p.secondEntry.get().toString() : "") + ", ");
            }
            sb.delete(sb.length() - 2, sb.length());
            sb.append("]}\n");
            return sb.toString(); 
        }

        public ActionIds get(final String src, final Optional<String> activated) {
            for (Pattern p: patternsList) {
                if (p.matches(src, activated)) {
                    final short[] actions = patternsMap.get(p);
                    if (p.secondEntryMatchActivated) {
                        ActionIds result = ActionIds.ACTIVATED;
                        result.setValue(actions);
                        return result;
                    } else {
                        ActionIds result = ActionIds.REGULAR;
                        result.setValue(actions);
                        return result;
                    }
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

    private OrderedProperties toProperties() {
        OrderedProperties props = new OrderedProperties();
        props.setProperty(RUNTIME_UPDATE_KEY, String.valueOf(runtimeUpdate));

        Target[] values = Target.values();
        Arrays.sort(values, Comparator.comparing(Enum::name));
        
        for(Target k: values) {
            String prefix;            
            if (k == Target.EQ || k == Target.NEARBY || k == Target.TB) {
                prefix = "@" + k.name().toLowerCase() + k.getId();
            } else {
                prefix = k.name().toLowerCase();
            }

            HashMap<String, short[]> fieldValue = (HashMap<String, short[]>) this.defaultProps.get(k);
            if (fieldValue != null) {
                for (Map.Entry<String, short[]> entry : fieldValue.entrySet()) {
                    props.setProperty(prefix + "." + entry.getKey(), shortArrayToString(entry.getValue()));
                }
            }
            
            Patterns currentPatterns = patterns.get(prefix);
            if (currentPatterns != null) {
                currentPatterns.patternsList.stream().forEach(p -> 
                    props.setProperty(prefix + "." + p.toString(), shortArrayToString(currentPatterns.patternsMap.get(p)))
                );
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
        sb.append("runtime_update=").append(String.valueOf(runtimeUpdate)).append("\n");
        sb.append("activatedDefaultProps=").append(defaultProps.getOrDefault(Target.ACTIVATED, new LinkedHashMap<>()).keySet().toString()).append(',');
        sb.append("\n");
        sb.append("areaDefaultProps=").append(defaultProps.getOrDefault(Target.AREA, new LinkedHashMap<>()).keySet().toString()).append(',');
        sb.append("\n");
        sb.append("bodyDefaultProps=").append(defaultProps.getOrDefault(Target.BODY, new LinkedHashMap<>()).keySet().toString()).append(',');
        sb.append("\n");
        sb.append("eqDefaultProps=").append(defaultProps.getOrDefault(Target.EQ, new LinkedHashMap<>()).keySet().toString()).append(',');
        sb.append("\n");
        sb.append("hoverDefaultProps=").append(defaultProps.getOrDefault(Target.HOVER, new LinkedHashMap<>()).keySet().toString()).append(',');
        sb.append("\n");
        sb.append("nearbyDefaultProps=").append(defaultProps.getOrDefault(Target.NEARBY, new LinkedHashMap<>()).keySet().toString()).append(',');
        sb.append("\n");
        sb.append("selectedDefaultProps=").append(defaultProps.getOrDefault(Target.SELECTED, new LinkedHashMap<>()).keySet().toString()).append(',');
        sb.append("\n");
        sb.append("tbDefaultProps=").append(defaultProps.getOrDefault(Target.TB, new LinkedHashMap<>()).keySet().toString()).append(',');
        sb.append("\n");
        sb.append("tileDefaultProps=").append(defaultProps.getOrDefault(Target.TILE, new LinkedHashMap<>()).keySet().toString()).append(',');
        sb.append("\n");
        sb.append("tileEDefaultProps=").append(defaultProps.getOrDefault(Target.TILE_E, new LinkedHashMap<>()).keySet().toString()).append(',');
        sb.append("\n");
        sb.append("tileNDefaultProps=").append(defaultProps.getOrDefault(Target.TILE_N, new LinkedHashMap<>()).keySet().toString()).append(',');
        sb.append("\n");
        sb.append("tileNEDefaultProps=").append(defaultProps.getOrDefault(Target.TILE_NE, new LinkedHashMap<>()).keySet().toString()).append(',');
        sb.append("\n");
        sb.append("tileNWDefaultProps=").append(defaultProps.getOrDefault(Target.TILE_NW, new LinkedHashMap<>()).keySet().toString()).append(',');
        sb.append("\n");
        sb.append("tileSDefaultProps=").append(defaultProps.getOrDefault(Target.TILE_S, new LinkedHashMap<>()).keySet().toString()).append(',');
        sb.append("\n");
        sb.append("tileSEDefaultProps=").append(defaultProps.getOrDefault(Target.TILE_SE, new LinkedHashMap<>()).keySet().toString()).append(',');
        sb.append("\n");
        sb.append("tileSWDefaultProps=").append(defaultProps.getOrDefault(Target.TILE_SW, new LinkedHashMap<>()).keySet().toString()).append(',');
        sb.append("\n");
        sb.append("tileWDefaultProps=").append(defaultProps.getOrDefault(Target.TILE_W, new LinkedHashMap<>()).keySet().toString()).append(',');
        sb.append("\n");
        sb.append("toolbeltDefaultProps=").append(defaultProps.getOrDefault(Target.TOOLBELT, new LinkedHashMap<>()).keySet().toString()).append(',');
        sb.append("\n");
        sb.append("patterns=").append(patterns.toString());
        sb.append("}");
        return sb.toString(); 
    }
}
