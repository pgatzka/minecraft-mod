package com.philgatzka.minecraftmod.menu;

import com.philgatzka.minecraftmod.MinecraftMod;
import com.philgatzka.minecraftmod.entity.SettlerEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

// A settler's inventory: a 4x10 grid whose left column is the armor the settler wears (head to feet) and whose other
// 9 columns are its 36 storage slots, above the player's inventory. Unlike the other menus, this one has real slots.
public class SettlerInventoryMenu extends AbstractContainerMenu
{
    public static final int ROWS = 4;
    public static final int STORAGE_COLUMNS = 9;
    // The armor column, top to bottom
    public static final EquipmentSlot[] ARMOR_SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    // Vanilla's empty armor slot icons from the player inventory, in the same order
    private static final ResourceLocation[] ARMOR_ICONS = {InventoryMenu.EMPTY_ARMOR_SLOT_HELMET, InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE,
            InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS, InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS};

    // Where the slots' items are drawn, relative to the screen's panel (SettlerInventoryScreen draws the slots around them)
    public static final int SLOT_SIZE = 18;
    public static final int ARMOR_X = 9;
    public static final int STORAGE_X = 31;
    public static final int GRID_Y = 21;
    public static final int PLAYER_INVENTORY_Y = 108;
    public static final int HOTBAR_Y = 166;

    // Menu slot indices
    public static final int ARMOR_START = 0;
    public static final int STORAGE_START = ARMOR_START + ROWS;
    public static final int PLAYER_START = STORAGE_START + SettlerEntity.STORAGE_SIZE;
    public static final int PLAYER_END = PLAYER_START + 36;

    // Server side only: the settler whose inventory is open
    @Nullable
    private final SettlerEntity settler;

    // Client side: the server fills these empty containers through the menu's slot sync
    public SettlerInventoryMenu(int containerId, Inventory inventory, FriendlyByteBuf buf)
    {
        this(containerId, inventory, new SimpleContainer(ROWS), new SimpleContainer(SettlerEntity.STORAGE_SIZE), null);
    }

    public SettlerInventoryMenu(int containerId, Inventory inventory, Container armor, Container storage, @Nullable SettlerEntity settler)
    {
        super(MinecraftMod.SETTLER_INVENTORY_MENU.get(), containerId);
        checkContainerSize(armor, ROWS);
        checkContainerSize(storage, SettlerEntity.STORAGE_SIZE);
        this.settler = settler;

        for (int row = 0; row < ROWS; row++)
            addSlot(new ArmorSlot(armor, row, ARMOR_X, GRID_Y + row * SLOT_SIZE, ARMOR_SLOTS[row])
                    .setBackground(InventoryMenu.BLOCK_ATLAS, ARMOR_ICONS[row]));
        for (int row = 0; row < ROWS; row++)
            for (int column = 0; column < STORAGE_COLUMNS; column++)
                addSlot(new Slot(storage, row * STORAGE_COLUMNS + column, STORAGE_X + column * SLOT_SIZE, GRID_Y + row * SLOT_SIZE));
        for (int row = 0; row < 3; row++)
            for (int column = 0; column < 9; column++)
                addSlot(new Slot(inventory, 9 + row * 9 + column, STORAGE_X + column * SLOT_SIZE, PLAYER_INVENTORY_Y + row * SLOT_SIZE));
        for (int column = 0; column < 9; column++)
            addSlot(new Slot(inventory, column, STORAGE_X + column * SLOT_SIZE, HOTBAR_Y));
    }

    // The menu as the server builds it for a settler; open() sends it to the player
    public static SettlerInventoryMenu forSettler(int containerId, Inventory inventory, SettlerEntity settler)
    {
        return new SettlerInventoryMenu(containerId, inventory, new SettlerArmor(settler), settler.getStorage(), settler);
    }

    public static void open(ServerPlayer player, SettlerEntity settler)
    {
        NetworkHooks.openScreen(player, new SimpleMenuProvider(
                (containerId, inventory, p) -> forSettler(containerId, inventory, settler), settler.getName()));
    }

    // Closes when the settler dies or the player walks away, like a chest
    @Override
    public boolean stillValid(Player player)
    {
        return settler == null || settler.isAlive() && player.distanceToSqr(settler) <= 64.0;
    }

    // Shift-click: the settler's items go to the player; the player's items go to the fitting armor slot, else to storage
    @Override
    public ItemStack quickMoveStack(Player player, int index)
    {
        Slot slot = slots.get(index);
        if (!slot.hasItem())
            return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < PLAYER_START)
        {
            moveItemStackTo(stack, PLAYER_START, PLAYER_END, true);
        }
        else
        {
            EquipmentSlot equipmentSlot = LivingEntity.getEquipmentSlotForItem(stack);
            if (equipmentSlot.getType() == EquipmentSlot.Type.ARMOR)
            {
                int armorIndex = ARMOR_START + armorRow(equipmentSlot);
                moveItemStackTo(stack, armorIndex, armorIndex + 1, false);
            }
            if (!stack.isEmpty())
                moveItemStackTo(stack, STORAGE_START, PLAYER_START, false);
        }

        if (stack.getCount() == original.getCount())
            return ItemStack.EMPTY;
        if (stack.isEmpty())
            slot.setByPlayer(ItemStack.EMPTY);
        else
            slot.setChanged();
        slot.onTake(player, stack);
        return original;
    }

    private static int armorRow(EquipmentSlot equipmentSlot)
    {
        for (int row = 0; row < ARMOR_SLOTS.length; row++)
            if (ARMOR_SLOTS[row] == equipmentSlot)
                return row;
        throw new IllegalArgumentException("Not an armor slot: " + equipmentSlot);
    }

    // Takes one piece of armor that belongs in its equipment slot
    private static class ArmorSlot extends Slot
    {
        private final EquipmentSlot equipmentSlot;

        ArmorSlot(Container container, int index, int x, int y, EquipmentSlot equipmentSlot)
        {
            super(container, index, x, y);
            this.equipmentSlot = equipmentSlot;
        }

        @Override
        public boolean mayPlace(ItemStack stack)
        {
            return LivingEntity.getEquipmentSlotForItem(stack) == equipmentSlot;
        }

        @Override
        public int getMaxStackSize()
        {
            return 1;
        }
    }

    // The armor the settler wears, as a Container, so the slots change its equipment directly (and it shows on the settler)
    private record SettlerArmor(SettlerEntity settler) implements Container
    {
        @Override
        public int getContainerSize()
        {
            return ROWS;
        }

        @Override
        public boolean isEmpty()
        {
            for (EquipmentSlot slot : ARMOR_SLOTS)
                if (!settler.getItemBySlot(slot).isEmpty())
                    return false;
            return true;
        }

        @Override
        public ItemStack getItem(int index)
        {
            return settler.getItemBySlot(ARMOR_SLOTS[index]);
        }

        @Override
        public ItemStack removeItem(int index, int count)
        {
            ItemStack worn = getItem(index);
            if (worn.isEmpty() || count <= 0)
                return ItemStack.EMPTY;
            ItemStack taken = worn.split(count);
            setItem(index, worn);
            return taken;
        }

        @Override
        public ItemStack removeItemNoUpdate(int index)
        {
            ItemStack worn = getItem(index);
            setItem(index, ItemStack.EMPTY);
            return worn;
        }

        @Override
        public void setItem(int index, ItemStack stack)
        {
            settler.setItemSlot(ARMOR_SLOTS[index], stack);
        }

        @Override
        public void setChanged()
        {
        }

        @Override
        public boolean stillValid(Player player)
        {
            return settler.isAlive();
        }

        @Override
        public void clearContent()
        {
            for (EquipmentSlot slot : ARMOR_SLOTS)
                settler.setItemSlot(slot, ItemStack.EMPTY);
        }
    }
}
