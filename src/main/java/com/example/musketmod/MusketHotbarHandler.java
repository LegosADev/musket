package com.example.musketmod;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Enforces the "one musket per hotbar" rule. Every server tick the player's
 * hotbar (slots 0-8) and offhand slot are scanned; the first musket found may
 * stay, and any additional muskets are moved to the main inventory (or dropped
 * if it's full).
 */
@EventBusSubscriber(modid = MusketMod.MODID)
public class MusketHotbarHandler {

    /** Hotbar slots 0-8, plus 40 which is the offhand slot. */
    private static final int[] CHECKED_SLOTS = {0, 1, 2, 3, 4, 5, 6, 7, 8, Inventory.SLOT_OFFHAND};

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) {
            return;
        }

        boolean foundOne = false;
        for (int slot : CHECKED_SLOTS) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.is(MusketMod.MUSKET.get())) {
                continue;
            }
            if (!foundOne) {
                foundOne = true;
                continue;
            }

            // Extra musket in the hotbar/offhand: remove it and relocate it
            player.getInventory().setItem(slot, ItemStack.EMPTY);
            int free = findFreeMainInventorySlot(player);
            if (free != -1) {
                player.getInventory().setItem(free, stack);
            } else {
                player.drop(stack, false);
            }
            player.displayClientMessage(
                    Component.translatable("item.musketmod.musket.one_per_hotbar"), true);
        }
    }

    /** Finds an empty main-inventory slot (9-35), or -1 if none is free. */
    private static int findFreeMainInventorySlot(Player player) {
        for (int slot = 9; slot <= 35; slot++) {
            if (player.getInventory().getItem(slot).isEmpty()) {
                return slot;
            }
        }
        return -1;
    }
}
