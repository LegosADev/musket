package com.example.musketmod;

import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(modid = MusketMod.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class ClientEvents {

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(MusketMod.MUSKET_BALL_ENTITY.get(), ThrownItemRenderer::new);
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // Cocked-hammer model once a ball is rammed home.
            ItemProperties.register(MusketMod.MUSKET.get(),
                    ResourceLocation.fromNamespaceAndPath(MusketMod.MODID, "loaded"),
                    (stack, level, entity, seed) -> MusketItem.isLoaded(stack) ? 1.0F : 0.0F);

            // Half-cock model while mid-reload.
            ItemProperties.register(MusketMod.MUSKET.get(),
                    ResourceLocation.fromNamespaceAndPath(MusketMod.MODID, "loading"),
                    (stack, level, entity, seed) -> entity != null && entity.isUsingItem()
                            && entity.getUseItem() == stack && !MusketItem.isLoaded(stack) ? 1.0F : 0.0F);

            // Sights-up model while aiming.
            ItemProperties.register(MusketMod.MUSKET.get(),
                    ResourceLocation.fromNamespaceAndPath(MusketMod.MODID, "aiming"),
                    (stack, level, entity, seed) -> MusketItem.isAiming(entity, stack) ? 1.0F : 0.0F);
        });
    }

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR,
                ResourceLocation.fromNamespaceAndPath(MusketMod.MODID, "ammo_counter"),
                new AmmoHudLayer());
    }
}
