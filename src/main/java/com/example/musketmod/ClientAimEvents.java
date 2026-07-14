package com.example.musketmod;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;

/** Zooms the camera in when the player looks down the musket's iron sights. */
@EventBusSubscriber(modid = MusketMod.MODID, value = Dist.CLIENT)
public class ClientAimEvents {

    /** How far the FOV closes in at full aim (0.45 = a bit over 2x zoom). */
    private static final float ZOOM_AMOUNT = 0.45F;

    @SubscribeEvent
    public static void onComputeFov(ComputeFovModifierEvent event) {
        Player player = event.getPlayer();
        ItemStack stack = player.getUseItem();
        if (!MusketItem.isAiming(player, stack)) {
            return;
        }
        // Ease the zoom in over the first few ticks so it isn't a hard snap.
        float progress = Math.min(1.0F, (float) player.getTicksUsingItem() / (float) MusketItem.ZOOM_TICKS);
        event.setNewFovModifier(event.getNewFovModifier() * (1.0F - ZOOM_AMOUNT * progress));
    }
}
