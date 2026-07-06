package net.bdew.wurm.action;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.interfaces.Initable;
import org.gotti.wurmunlimited.modloader.interfaces.PreInitable;
import org.gotti.wurmunlimited.modloader.interfaces.WurmClientMod;

import com.wurmonline.client.comm.ServerConnectionListenerClass;
import com.wurmonline.client.game.inventory.InventoryMetaItem;
import com.wurmonline.client.renderer.PickableUnit;
import com.wurmonline.client.renderer.cell.CellRenderable;
import com.wurmonline.client.renderer.cell.CreatureCellRenderable;
import com.wurmonline.client.renderer.cell.GroundItemCellRenderable;
import com.wurmonline.client.renderer.gui.HeadsUpDisplay;
import com.wurmonline.client.renderer.gui.PaperDollSlot;
import com.wurmonline.mesh.Tiles;
import com.wurmonline.shared.constants.PlayerAction;

import javassist.ClassPool;
import javassist.CtClass;

public class ActionMod implements WurmClientMod, Initable, PreInitable {
    private static final Logger logger = Logger.getLogger("ActionMod");

    public static boolean showActionNums = false;
    public static HeadsUpDisplay hud;
    public static DefaultAction defaultAction;

    public static void logException(String msg, Throwable e) {
            if (logger != null)
            logger.log(Level.SEVERE, msg, e);
    }

    public static boolean handleInput(final String cmd, final String[] data) {
        if (cmd.equals("act_show")) {
            if (data.length == 2) {
                if (data[1].equals("on")) {
                    hud.consoleOutput("Action numbers on");
                    showActionNums = true;
                    return true;
                } else if (data[1].equals("off")) {
                    hud.consoleOutput("Action numbers off");
                    showActionNums = false;
                    return true;
                }
            }
            hud.consoleOutput("Usage: act_show {on|off}");
            return true;
        } else if (cmd.equals("act")) {
            // Stitch it back together with spaces, without the leading 'act' and get a list of strings split by |
            final String[] commands = String.join(" ", Arrays.copyOfRange(data, 1, data.length)).split("\\|");
            for (String nextCmd : commands) {
                // Remove leading/trailing whitespace, then split it apart and parse it
                final String[] nextCmdSplit = nextCmd.trim().split(" ");
                try {
                    if (nextCmdSplit.length == 2)
                        parseAct(nextCmdSplit[0], nextCmdSplit[1]);
                    else
                        hud.consoleOutput("Usage: act <id> <modifier>[|<id> <modifier>|...]");
                } catch (Throwable roe) {
                    throw new RuntimeException(roe);
                } 
            }
            return true;
        }
        return false;
    }

    @Override
    public void init() {
        logger.fine("Initializing");
        try {
            ClassPool classPool = HookManager.getInstance().getClassPool();

            CtClass ctPlayerAction = classPool.getCtClass("com.wurmonline.shared.constants.PlayerAction");
            ctPlayerAction.getMethod("getName", "()Ljava/lang/String;").insertBefore("if (net.bdew.wurm.action.ActionMod.showActionNums) return this.name + \" (\"+this.id+\")\";");

            CtClass ctWurmConsole = classPool.getCtClass("com.wurmonline.client.console.WurmConsole");
            ctWurmConsole.getMethod("handleDevInput", "(Ljava/lang/String;[Ljava/lang/String;)Z").insertBefore(
                    "if (net.bdew.wurm.action.ActionMod.handleInput($1,$2)) return true;"
            );

            defaultAction = DefaultAction.setup();

            // Hook HUD init to setup our stuff
            HookManager.getInstance().registerHook("com.wurmonline.client.renderer.gui.HeadsUpDisplay", "init", "(II)V", () -> (proxy, method, args) -> {
                method.invoke(proxy, args);
                hud = (HeadsUpDisplay) proxy;
                Reflect.setup();
                return null;
            });

            logger.fine("Loaded");
        } catch (Throwable e) {
            logException("Error loading mod", e);
        }
    }

    @Override
    public void preInit() {

    }

    private static void sendAreaAction(final PlayerAction action) {
        sendLocalAction(action, +1, +1);
        sendLocalAction(action, +1, +0);
        sendLocalAction(action, +1, -1);

        sendLocalAction(action, +0, +1);
        sendLocalAction(action, +0, +0);
        sendLocalAction(action, +0, -1);

        sendLocalAction(action, -1, +1);
        sendLocalAction(action, -1, +0);
        sendLocalAction(action, -1, -1);
    }

    private static void sendLocalAction(final PlayerAction action, int xo, int yo) {
        int x = hud.getWorld().getPlayerCurrentTileX();
        int y = hud.getWorld().getPlayerCurrentTileY();
        hud.sendAction(action, Tiles.getTileId(x + xo, y + yo, 0));
    }

    private static void parseAct(final String strId, final String target) throws ReflectiveOperationException {
        final Optional<Target> targetOptE = Target.parseTargetSafe(target);

        if (targetOptE.isPresent()) { 
            Target targetE = targetOptE.get();

            if (strId.equals(DefaultAction.Action.DEFAULT.name().toLowerCase())) {
                Optional<Short> default_id = defaultAction.getAction(targetE, DefaultAction.Action.DEFAULT, hud);
                if (default_id.isPresent()) {
                    act(default_id.get(), targetE, target);
                } else {
                    hud.consoleOutput("act: it doesn't have default action for target '" + target + "'");
                }
            } else if (strId.equals(DefaultAction.Action.ALT.name().toLowerCase())) {
                Optional<Short> alt_id = defaultAction.getAction(targetE, DefaultAction.Action.ALT, hud);
                if (alt_id.isPresent()) {
                    act(alt_id.get(), targetE, target);
                } else {
                    hud.consoleOutput("act: it doesn't have alternative action for target '" + target + "'");
                }
            } else {
                try {
                    final short id = Short.parseShort(strId);
                    act(id, targetE, target);
                } catch (NumberFormatException nfe) {
                    hud.consoleOutput("act: Error parsing id '" + strId + "'");
                    return;
                } 
            }
        }
    }

    private static void act(final short id, final Target targetE, final String targetStr) {
        final PlayerAction act = new PlayerAction(id, PlayerAction.ANYTHING, "", false);

        switch (targetE) {
            case HOVER:
                hud.getWorld().sendHoveredAction(act);
                break;
            case BODY:
                // Original logic also unwrapped the fetched value
                hud.sendAction(act, Reflect.getBodyItem(hud.getPaperDollInventory()).get().getId());
                break;
            case TILE:
                hud.getWorld().sendLocalAction(act);
                break;
            case TILE_N:
                sendLocalAction(act, 0, -1);
                break;
            case TILE_W:
                sendLocalAction(act, -1, 0);
                break;
            case TILE_NW:
                sendLocalAction(act, -1, -1);
                break;
            case TILE_NE:
                sendLocalAction(act, 1, -1);
                break;
            case TILE_S:
                sendLocalAction(act, 0, 1);
                break;
            case TILE_E:
                sendLocalAction(act, 1, 0);
                break;
            case TILE_SE:
                sendLocalAction(act, 1, 1);
                break;
            case TILE_SW:
                sendLocalAction(act, -1, 1);
                break;
            case TOOL:
                Optional<InventoryMetaItem> t = Reflect.getActiveToolItem(hud);
                if (t.isPresent())
                    hud.sendAction(act, t.get().getId());
                else
                    hud.consoleOutput("act: tool modifier requires an active tool selected");
                break;
            case SELECTED:
                Optional<PickableUnit> p = Reflect.getSelectedUnit(hud.getSelectBar());
                if (p.isPresent())
                    hud.sendAction(act, p.get().getId());
                break;
            case AREA:
                sendAreaAction(act);
                break;
            case TOOLBELT:
                if (id >= 1 && id <= 10)
                    hud.setActiveTool(id - 1);
                else
                    hud.consoleOutput("act: Invalid toolbelt slot '" + id + "'");
                break;
            case TB:
                int slotT = Integer.parseInt(targetStr.substring(3));
                if (slotT >= 1 && slotT <= 10 && hud.getToolBelt().getItemInSlot(slotT - 1) != null)
                    hud.sendAction(act, hud.getToolBelt().getItemInSlot(slotT - 1).getId());
                else
                    hud.consoleOutput("act: Invalid toolbelt slot '" + slotT + "'");
                break;
            case EQ:
                byte slotE = Byte.parseByte(targetStr.substring(3));
                Optional<PaperDollSlot> obj = Reflect.getFrameFromSlotnumber(hud.getPaperDollInventory(), slotE);
                if (!obj.isPresent()) {
                    hud.consoleOutput("act: Invalid equipment slot " + slotE);
                } else if (obj.get().getEquippedItem() == null) {
                    hud.consoleOutput("act: No item in equipment slot " + slotE);
                } else {
                    hud.sendAction(act, obj.get().getEquippedItem().getId());
                }
                break;
            case NEARBY:
                float range = Float.parseFloat(targetStr.substring(7));
                final float rangeSq = range * range;
                ServerConnectionListenerClass conn = hud.getWorld().getServerConnection().getServerConnectionListener();
                // Original logic also unwrapped the fetched value
                Collection<GroundItemCellRenderable> items = Reflect.getGroundItems(conn).get().values();
                Collection<CreatureCellRenderable> creatures = conn.getCreatures().values();
                Stream.concat(items.stream(), creatures.stream())
                        .filter(x -> x.getSquaredLengthFromPlayer() < rangeSq)
                        .mapToLong(CellRenderable::getId)
                        .forEach(tid -> hud.sendAction(act, tid));
                break;
            default:
                hud.consoleOutput("act: Invalid target keyword '" + targetStr + "'");
        } 
    }
}
