package com.susano;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.world.World;

/** La bete a queues invoquee par le Gunbai. Deplacee par le serveur (BeastManager), dessinee par BeastRenderer. */
public class TailedBeastEntity extends Entity {
    /** 0 = sort de terre, 1 = charge, 2 = rugit et frappe de ses queues. */
    private static final TrackedData<Integer> PHASE =
            DataTracker.registerData(TailedBeastEntity.class, TrackedDataHandlerRegistry.INTEGER);

    public TailedBeastEntity(EntityType<? extends TailedBeastEntity> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setNoGravity(true);
    }

    @Override
    protected void initDataTracker() {
        this.dataTracker.startTracking(PHASE, 0);
    }

    public int getPhase() { return this.dataTracker.get(PHASE); }
    public void setPhase(int p) { this.dataTracker.set(PHASE, p); }

    @Override
    public void tick() {
        super.tick();
        if (this.getWorld().isClient) {
            // prediction cote client pendant la charge ; le serveur envoie les corrections
            this.setPosition(this.getX() + this.getVelocity().x, this.getY() + this.getVelocity().y, this.getZ() + this.getVelocity().z);
        } else if (this.age > 600) {
            this.discard();
        }
    }

    @Override public boolean canHit() { return false; }
    @Override public boolean isPushable() { return false; }
    @Override public boolean shouldSave() { return false; }
    @Override public boolean isAttackable() { return false; }
    @Override public boolean isInvulnerableTo(DamageSource source) { return true; }
    @Override public boolean damage(DamageSource source, float amount) { return false; }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) { }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) { }

    @Override
    public Packet<ClientPlayPacketListener> createSpawnPacket() {
        return new EntitySpawnS2CPacket(this);
    }
}
