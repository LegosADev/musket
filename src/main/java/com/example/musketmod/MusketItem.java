package com.example.musketmod;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * A muzzle-loading rifled musket.
 *
 * Controls:
 *   - Right-click (empty)  : hold 5s to ram a ball home. The charge is stored on the
 *                            item and kept until fired.
 *   - Right-click (loaded) : aim down the iron sights - zooms in and steadies the shot.
 *   - Left-click           : pull the trigger. Fires from the hip or while aiming.
 */
public class MusketItem extends Item {
    /** 5 seconds * 20 ticks = 100 ticks to reload. */
    public static final int RELOAD_TICKS = 100;
    /** 7 hearts = 14 damage. */
    public static final float DAMAGE = 14.0F;
    /** Ticks of aiming needed for a fully steady shot. */
    public static final int STEADY_TICKS = 14;
    /** Ticks the sights take to settle, used for the zoom ramp. */
    public static final int ZOOM_TICKS = 5;

    private static final float SPREAD_HIP = 5.0F;
    private static final float SPREAD_AIMED = 0.15F;
    private static final int FIRE_COOLDOWN_TICKS = 10;

    public MusketItem(Properties properties) {
        super(properties);
    }

    public static boolean isLoaded(ItemStack stack) {
        return Boolean.TRUE.equals(stack.get(MusketMod.LOADED.get()));
    }

    public static void setLoaded(ItemStack stack, boolean loaded) {
        stack.set(MusketMod.LOADED.get(), loaded);
    }

    /** True while the player is holding right-click on an already-loaded musket. */
    public static boolean isAiming(LivingEntity entity, ItemStack stack) {
        return entity != null && entity.isUsingItem()
                && entity.getUseItem() == stack && isLoaded(stack);
    }

    /** 0.0 -> 1.0 how steady the shot currently is. */
    public static float steadiness(int aimTicks) {
        return Math.min(1.0F, (float) aimTicks / (float) STEADY_TICKS);
    }

    // ------------------------------------------------------------------
    // Left-click: the trigger. Handled through FirePayload so it works on air.
    // ------------------------------------------------------------------

    /** A musket can't mine. */
    @Override
    public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
        return false;
    }

    /** A musket isn't a club either - left-clicking a mob shouldn't melee it. */
    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
        return true; // cancels the vanilla attack
    }

    // ------------------------------------------------------------------
    // Right-click: reload when empty, aim when loaded.
    // ------------------------------------------------------------------

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Loaded: look down the sights. Left-click fires.
        if (isLoaded(stack)) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }

        // Empty: begin the 5 second reload if there's a ball to hand.
        if (player.getAbilities().instabuild || !findAmmo(player).isEmpty()) {
            player.startUsingItem(hand);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.CROSSBOW_LOADING_START, SoundSource.PLAYERS, 0.9F, 0.7F);
            return InteractionResultHolder.consume(stack);
        }

        // No ammo at all: dry click.
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.CROSSBOW_LOADING_END, SoundSource.PLAYERS, 0.5F, 1.8F);
        return InteractionResultHolder.fail(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        // Loaded: hold the sights as long as you like. Empty: 5 second reload.
        return isLoaded(stack) ? 72000 : RELOAD_TICKS;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return isLoaded(stack) ? UseAnim.BOW : UseAnim.CROSSBOW;
    }

    /** Letting go of right-click just lowers the sights / cancels the reload. */
    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        // no-op: the trigger is left-click now
    }

    /** Held right-click for the full 5 seconds on an empty musket: ball goes in. */
    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!(entity instanceof Player player) || isLoaded(stack)) {
            return stack;
        }

        boolean creative = player.getAbilities().instabuild;
        ItemStack ammo = findAmmo(player);
        if (!creative && ammo.isEmpty()) {
            return stack;
        }
        if (!creative) {
            ammo.shrink(1);
        }

        setLoaded(stack, true);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.CROSSBOW_LOADING_END, SoundSource.PLAYERS, 1.0F, 1.0F);
        return stack;
    }

    /** The actual shot. Called from the fire packet once the server has vetted it. */
    public static void fire(Level level, Player player, ItemStack stack, InteractionHand hand) {
        // How long were the sights up? Hip-firing gives 0 and sprays.
        int aimTicks = isAiming(player, stack) ? player.getTicksUsingItem() : 0;
        float spread = SPREAD_HIP - (SPREAD_HIP - SPREAD_AIMED) * steadiness(aimTicks);

        setLoaded(stack, false);
        player.stopUsingItem();

        if (!level.isClientSide) {
            MusketBallEntity ball = new MusketBallEntity(level, player);
            ball.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 5.0F, spread);
            level.addFreshEntity(ball);

            stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));

            if (level instanceof ServerLevel serverLevel) {
                Vec3 look = player.getLookAngle();
                Vec3 muzzle = player.getEyePosition().add(look.scale(1.4)).add(0.0, -0.15, 0.0);
                serverLevel.sendParticles(ParticleTypes.CLOUD,
                        muzzle.x, muzzle.y, muzzle.z, 10, 0.06, 0.06, 0.06, 0.02);
                serverLevel.sendParticles(ParticleTypes.FLAME,
                        muzzle.x, muzzle.y, muzzle.z, 4, 0.02, 0.02, 0.02, 0.01);
            }
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 2.0F, 0.6F);
        player.getCooldowns().addCooldown(stack.getItem(), FIRE_COOLDOWN_TICKS);
        player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
    }

    private static ItemStack findAmmo(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(MusketMod.MUSKET_BALL.get())) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    /** Counts every musket ball the player is carrying, for the HUD. */
    public static int countAmmo(Player player) {
        int total = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(MusketMod.MUSKET_BALL.get())) {
                total += stack.getCount();
            }
        }
        return total;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.musketmod.musket.tooltip").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.musketmod.musket.tooltip2").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("item.musketmod.musket.tooltip3").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable(isLoaded(stack)
                        ? "item.musketmod.musket.state_loaded"
                        : "item.musketmod.musket.state_empty")
                .withStyle(isLoaded(stack) ? ChatFormatting.GREEN : ChatFormatting.RED));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
