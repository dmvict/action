package net.bdew.wurm.action;

import com.wurmonline.client.comm.ServerConnectionListenerClass;
import com.wurmonline.client.game.inventory.InventoryMetaItem;
import com.wurmonline.client.renderer.PickableUnit;
import com.wurmonline.client.renderer.cell.GroundItemCellRenderable;
import com.wurmonline.client.renderer.gui.HeadsUpDisplay;
import com.wurmonline.client.renderer.gui.PaperDollInventory;
import com.wurmonline.client.renderer.gui.PaperDollSlot;
import com.wurmonline.client.renderer.gui.SelectBar;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;

@SuppressWarnings("unchecked")
public class Reflect {
    // PaperDollInventory
    static private Field fldBodyItem;
    static private Method mGetFrameFromSlotnumber;

    // HeadsUpDisplay
    static private Field fldActiveToolItem;

    // SelectBar
    static private Field fldSelectedUnit;

    // ServerConnectionListenerClass
    private static Field fldGroundItems;

    static public void setup() throws ReflectiveOperationException {
        fldBodyItem = PaperDollInventory.class.getDeclaredField("bodyItem");
        fldActiveToolItem = HeadsUpDisplay.class.getDeclaredField("activeToolItem");
        fldSelectedUnit = SelectBar.class.getDeclaredField("selectedUnit");
        mGetFrameFromSlotnumber = ReflectionUtil.getMethod(PaperDollInventory.class, "getFrameFromSlotnumber", new Class[]{byte.class});
        fldGroundItems = ReflectionUtil.getField(ServerConnectionListenerClass.class, "groundItems");
    }

    public static Optional<InventoryMetaItem> getBodyItem(PaperDollInventory pd) {
        try {
            InventoryMetaItem item = ((PaperDollSlot) ReflectionUtil.getPrivateField(pd, fldBodyItem)).getItem();
            return (item == null ? Optional.empty() : Optional.of(item));
        } catch (ReflectiveOperationException e) {
            return Optional.empty();
        }
    }

    public static Optional<InventoryMetaItem> getActiveToolItem(HeadsUpDisplay hud) {
        try {
            InventoryMetaItem item = ReflectionUtil.getPrivateField(hud, fldActiveToolItem);
            return (item == null ? Optional.empty() : Optional.of(item));
        } catch (ReflectiveOperationException e) {
            return Optional.empty();
        }
    }

    public static Optional<PickableUnit> getSelectedUnit(SelectBar s) {
        try {
            PickableUnit unit = ReflectionUtil.getPrivateField(s, fldSelectedUnit);
            return (unit == null ? Optional.empty() : Optional.of(unit));
        } catch (ReflectiveOperationException e) {
            return Optional.empty();
        }
    }

    public static Optional<PaperDollSlot> getFrameFromSlotnumber(PaperDollInventory pd, byte slot) {
        try {
            PaperDollSlot slotNumber = ReflectionUtil.callPrivateMethod(pd, mGetFrameFromSlotnumber, slot);
            return (slotNumber == null ? Optional.empty() : Optional.of(slotNumber));
        } catch (ReflectiveOperationException e) {
            return Optional.empty();
        }
    }

    public static Optional<Map<Long, GroundItemCellRenderable>> getGroundItems(ServerConnectionListenerClass conn) {
        try {
            Map<Long, GroundItemCellRenderable> items = ReflectionUtil.getPrivateField(conn, fldGroundItems);
            return (items == null ? Optional.empty() : Optional.of(items));
        } catch (ReflectiveOperationException e) {
            return Optional.empty();
        }
    }
}
