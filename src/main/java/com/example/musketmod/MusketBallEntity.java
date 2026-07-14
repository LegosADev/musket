package com.example.musketmod;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * The musket ball. Punches straight through terrain and only stops when it hits
 * something living (or runs out of life).
 */
public class MusketBallEntity extends ThrowableItemProjectile {

    /** Ticks the ball stays alive before despawning (it never hits the ground now). */
    private static final int LIFETIME = 100;

    private int life;

    public MusketBallEntity(EntityType<? extends MusketBallEntity> type, Level level) {
        super(type, level);
    }

    public MusketBallEntity(Level level, LivingEntity shooter) {
        super(MusketMod.MUSKET_BALL_ENTITY.get(), shooter, level);
    }

    @Override
    protected Item getDefaultItem() {
        return MusketMod.MUSKET_BALL.get();
    }

    @Override
    protected double getDefaultGravity() {
        // Almost flat trajectory over normal fighting range, like a bullet
        return 0.005;
    }

    @Override
    public void tick() {
        super.tick();
        // Without a block collision to end it, the ball needs its own lifespan.
        if (!this.level().isClientSide && ++this.life > LIFETIME) {
            this.discard();
        }
    }

    /** Terrain does not stop the ball. */
    @Override
    protected void onHitBlock(BlockHitResult result) {
        // deliberately empty - fly straight through
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        result.getEntity().hurt(this.damageSources().thrown(this, this.getOwner()), MusketItem.DAMAGE);
    }

    @Override
    protected void onHit(HitResult result) {
        // Ignore block hits entirely; only an entity stops the ball.
        if (result.getType() != HitResult.Type.ENTITY) {
            return;
        }
        super.onHit(result);
        if (!this.level().isClientSide) {
            this.level().broadcastEntityEvent(this, (byte) 3);
            this.discard();
        }
    }
}
