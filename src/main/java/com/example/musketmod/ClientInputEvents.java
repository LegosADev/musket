package com.example.musketmod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Turns a left-click into a trigger pull instead of a swing/mine/attack.
 */
@EventBusSubscriber(modid = MusketMod.MODID, value = Dist.CLIENT)
public class ClientInputEvents {

    @SubscribeEvent
    public static void onAttackKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isAttack()) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        if (!stack.is(MusketMod.MUSKET.get())) {
            return;
        }

        // Don't punch, mine or swing - pull the trigger.
        event.setSwingHand(false);
        event.setCanceled(true);

        if (MusketItem.isLoaded(stack) && !player.getCooldowns().isOnCooldown(stack.getItem())) {
            PacketDistributor.sendToServer(FirePayload.INSTANCE);
        }
    }
}
