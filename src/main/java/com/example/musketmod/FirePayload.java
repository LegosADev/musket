package com.example.musketmod;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Sent client -> server when the player left-clicks while holding a musket.
 * Minecraft does not tell the server about left-clicks on empty air, so the
 * trigger pull has to be its own packet.
 */
public record FirePayload() implements CustomPacketPayload {

    public static final FirePayload INSTANCE = new FirePayload();

    public static final CustomPacketPayload.Type<FirePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MusketMod.MODID, "fire"));

    public static final StreamCodec<ByteBuf, FirePayload> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(FirePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player == null) {
                return;
            }
            ItemStack stack = player.getMainHandItem();
            // Re-check server-side: never trust the client that it may shoot.
            if (!stack.is(MusketMod.MUSKET.get()) || !MusketItem.isLoaded(stack)) {
                return;
            }
            if (player.getCooldowns().isOnCooldown(stack.getItem())) {
                return;
            }
            MusketItem.fire(player.level(), player, stack, InteractionHand.MAIN_HAND);
        });
    }
}
