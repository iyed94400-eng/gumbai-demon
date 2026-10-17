package com.susano;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;
import net.minecraft.world.Heightmap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Attaque du Gunbai (touche L) : une bete a queues sort de terre devant toi, charge droit devant en ecrasant tout,
 * puis rugit et frappe de ses neuf queues avant de disparaitre.
 */
public final class BeastManager {
    private static final int COOLDOWN = 900;       // 45 s
    private static final int RISE = 25, CHARGE = 70, STAND = 70;
    private static final double SPEED = 0.75;       // blocs / tick
    private static final float CHARGE_DMG = 60f, SLAM_DMG = 150f, TAIL_DMG = 70f;
    private static final double HIT_RADIUS = 5.0;

    private static final Map<UUID, Long> CDS = new HashMap<>();
    private static final List<Run> RUNS = new ArrayList<>();

    private static final class Run {
        final ServerWorld w;
        final UUID owner;
        final TailedBeastEntity beast;
        Vec3d dir;
        float yaw;
        int age;

        Run(ServerWorld w, UUID owner, TailedBeastEntity beast, Vec3d dir, float yaw) {
            this.w = w;
            this.owner = owner;
            this.beast = beast;
            this.dir = dir;
            this.yaw = yaw;
        }
    }

    private BeastManager() { }

    public static void clear() {
        for (Run r : RUNS) r.beast.discard();
        RUNS.clear();
        CDS.clear();
    }

    public static void summon(ServerPlayerEntity p) {
        ItemStack main = p.getMainHandStack(), off = p.getOffHandStack();
        if (!(main.getItem() instanceof GunbaiItem) && !(off.getItem() instanceof GunbaiItem)) {
            p.sendMessage(Text.translatable("message.susanoo.need_gunbai"), true);
            return;
        }
        ServerWorld w = p.getServerWorld();
        long now = w.getTime();
        Long until = CDS.get(p.getUuid());
        if (until != null && now < until) {
            p.sendMessage(Text.translatable("message.susanoo.beast_cd", (until - now + 19) / 20), true);
            return;
        }
        Vec3d dir = Vec3d.fromPolar(0f, p.getYaw());
        Vec3d start = p.getPos().add(dir.multiply(10));
        double gy = ground(w, start.x, start.z, p.getY());
        TailedBeastEntity b = SusanooMod.BEAST.create(w);
        if (b == null) return;
        b.refreshPositionAndAngles(start.x, gy - 5, start.z, p.getYaw(), 0f);
        w.spawnEntity(b);
        RUNS.add(new Run(w, p.getUuid(), b, dir, p.getYaw()));
        CDS.put(p.getUuid(), now + COOLDOWN);
        w.playSound(null, start.x, gy, start.z, SoundEvents.ENTITY_WARDEN_EMERGE, SoundCategory.PLAYERS, 8f, 0.5f);
        w.playSound(null, start.x, gy, start.z, SoundEvents.ENTITY_RAVAGER_ROAR, SoundCategory.PLAYERS, 8f, 0.4f);
        p.sendMessage(Text.translatable("message.susanoo.beast"), true);
    }

    public static void tick(MinecraftServer server) {
        Iterator<Run> it = RUNS.iterator();
        while (it.hasNext()) {
            Run r = it.next();
            ServerPlayerEntity p = server.getPlayerManager().getPlayer(r.owner);
            if (p == null || r.beast.isRemoved()) {
                r.beast.discard();
                it.remove();
                continue;
            }
            if (!step(p, r)) {
                it.remove();
            }
        }
    }

    private static boolean step(ServerPlayerEntity p, Run r) {
        ServerWorld w = r.w;
        TailedBeastEntity b = r.beast;
        r.age++;
        Vec3d pos = b.getPos();
        if (r.age <= RISE) {
            // sort de terre
            double gy = ground(w, pos.x, pos.z, pos.y + 5);
            double y = gy - 5 + 5.0 * r.age / RISE;
            b.setPosition(pos.x, y, pos.z);
            if (r.age % 3 == 0) {
                SusanooManager.fx(w, ParticleTypes.CAMPFIRE_COSY_SMOKE, new Vec3d(pos.x, gy + 0.5, pos.z), 6, 3.5, 0.3, 3.5, 0.02);
                SusanooManager.fx(w, ParticleTypes.POOF, new Vec3d(pos.x, gy + 0.3, pos.z), 12, 4, 0.2, 4, 0.1);
            }
            if (r.age == RISE) {
                b.setPhase(1);
                setMotion(b, r.dir.multiply(SPEED));
            }
            return true;
        }
        if (r.age <= RISE + CHARGE) {
            // charge : suit lentement l'ennemi le plus proche devant
            LivingEntity target = nearestAhead(p, w, pos, r.dir);
            if (target != null) {
                Vec3d to = new Vec3d(target.getX() - pos.x, 0, target.getZ() - pos.z).normalize();
                r.dir = r.dir.multiply(0.92).add(to.multiply(0.08)).normalize();
                r.yaw = (float) (MathHelper.atan2(-r.dir.x, r.dir.z) * 180.0 / Math.PI);
                b.setYaw(r.yaw);
                setMotion(b, r.dir.multiply(SPEED));
            }
            Vec3d next = pos.add(r.dir.multiply(SPEED));
            double gy = ground(w, next.x, next.z, pos.y);
            double y = MathHelper.clamp(gy, pos.y - 0.7, pos.y + 0.7);
            b.setPosition(next.x, y, next.z);
            crush(p, r, b.getPos());
            if (r.age % 6 == 0) {
                w.playSound(null, next.x, y, next.z, SoundEvents.ENTITY_IRON_GOLEM_STEP, SoundCategory.PLAYERS, 6f, 0.4f);
                SusanooManager.fx(w, ParticleTypes.POOF, new Vec3d(next.x, y + 0.3, next.z), 14, 2.5, 0.2, 2.5, 0.08);
            }
            if (r.age == RISE + CHARGE) {
                setMotion(b, Vec3d.ZERO);
                b.setPhase(2);
                slam(p, r, b.getPos());
            }
            return true;
        }
        if (r.age <= RISE + CHARGE + STAND) {
            int t = r.age - RISE - CHARGE;
            if (t % 20 == 0) {
                tailWhip(p, r, b.getPos());
            }
            return true;
        }
        SusanooManager.fx(w, ParticleTypes.POOF, b.getPos().add(0, 3, 0), 80, 4, 3, 4, 0.1);
        SusanooManager.fx(w, ParticleTypes.LARGE_SMOKE, b.getPos().add(0, 4, 0), 60, 4, 3, 4, 0.05);
        w.playSound(null, b.getX(), b.getY(), b.getZ(), SoundEvents.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.PLAYERS, 6f, 0.5f);
        b.discard();
        return false;
    }

    private static void setMotion(TailedBeastEntity b, Vec3d v) {
        b.setVelocity(v);
        b.velocityDirty = true;
    }

    /** Ecrase tout sur le passage : blocs (sauf le sol) et creatures. */
    private static void crush(ServerPlayerEntity p, Run r, Vec3d pos) {
        ServerWorld w = r.w;
        Vec3d c = pos.add(r.dir.multiply(2.2));
        if (w.getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING)) {
            int cx = (int) Math.floor(c.x), cy = (int) Math.floor(pos.y), cz = (int) Math.floor(c.z);
            for (int dx = -4; dx <= 4; dx++) {
                for (int dy = 0; dy <= 8; dy++) {
                    for (int dz = -4; dz <= 4; dz++) {
                        double ex = dx / 4.0, ey = (dy - 3.5) / 4.5, ez = dz / 4.0;
                        if (ex * ex + ey * ey + ez * ez > 1.0) continue;
                        SusanooManager.wipe(w, p, new BlockPos(cx + dx, cy + dy, cz + dz));
                    }
                }
            }
        }
        Box box = new Box(c, c).expand(HIT_RADIUS, 4.5, HIT_RADIUS);
        for (LivingEntity t : w.getEntitiesByClass(LivingEntity.class, box, x -> SusanooManager.canTarget(p, x))) {
            t.damage(w.getDamageSources().playerAttack(p), CHARGE_DMG);
            t.addVelocity(r.dir.x * 1.6, 0.7, r.dir.z * 1.6);
            t.velocityModified = true;
        }
    }

    /** Fin de la charge : rugissement et grosse onde de choc. */
    private static void slam(ServerPlayerEntity p, Run r, Vec3d pos) {
        ServerWorld w = r.w;
        w.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ENTITY_RAVAGER_ROAR, SoundCategory.PLAYERS, 10f, 0.35f);
        w.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 8f, 0.5f);
        shock(p, w, pos, 16.0, SLAM_DMG, 3.0);
        if (w.getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING)) {
            int cx = (int) Math.floor(pos.x), cy = (int) Math.floor(pos.y), cz = (int) Math.floor(pos.z);
            for (int dx = -8; dx <= 8; dx++) {
                for (int dy = 0; dy <= 8; dy++) {
                    for (int dz = -8; dz <= 8; dz++) {
                        if (dx * dx + dy * dy + dz * dz > 64) continue;
                        SusanooManager.wipe(w, p, new BlockPos(cx + dx, cy + dy, cz + dz));
                    }
                }
            }
        }
    }

    private static void tailWhip(ServerPlayerEntity p, Run r, Vec3d pos) {
        ServerWorld w = r.w;
        w.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ENTITY_PHANTOM_FLAP, SoundCategory.PLAYERS, 6f, 0.3f);
        w.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 5f, 0.6f);
        shock(p, w, pos, 12.0, TAIL_DMG, 2.2);
    }

    private static void shock(ServerPlayerEntity p, ServerWorld w, Vec3d c, double radius, float dmg, double push) {
        Box box = new Box(c, c).expand(radius, radius * 0.6, radius);
        for (LivingEntity t : w.getEntitiesByClass(LivingEntity.class, box, x -> SusanooManager.canTarget(p, x))) {
            double d = t.getPos().distanceTo(c);
            if (d > radius) continue;
            float k = (float) (1.0 - d / radius);
            t.damage(w.getDamageSources().playerAttack(p), dmg * (0.4f + 0.6f * k));
            Vec3d out = new Vec3d(t.getX() - c.x, 0, t.getZ() - c.z);
            out = out.lengthSquared() < 1.0e-4 ? new Vec3d(0, 0, 1) : out.normalize();
            t.addVelocity(out.x * push, 0.8, out.z * push);
            t.velocityModified = true;
        }
        for (double ring = 3; ring <= radius; ring += 3) {
            int n = (int) (8 + ring * 2);
            for (int i = 0; i < n; i++) {
                double a = i * Math.PI * 2 / n;
                SusanooManager.fx(w, i % 2 == 0 ? ParticleTypes.CLOUD : ParticleTypes.LARGE_SMOKE,
                        new Vec3d(c.x + Math.cos(a) * ring, c.y + 0.5, c.z + Math.sin(a) * ring), 1, 0.2, 0.3, 0.2, 0.05);
            }
        }
    }

    private static LivingEntity nearestAhead(ServerPlayerEntity p, ServerWorld w, Vec3d pos, Vec3d dir) {
        LivingEntity best = null;
        double bestD = 40.0;
        Box box = new Box(pos, pos).expand(40.0);
        for (LivingEntity t : w.getEntitiesByClass(LivingEntity.class, box, x -> SusanooManager.canTarget(p, x))) {
            Vec3d to = new Vec3d(t.getX() - pos.x, 0, t.getZ() - pos.z);
            double d = to.length();
            if (d > bestD || d < 3.0 || to.normalize().dotProduct(dir) < 0.6) continue;
            best = t;
            bestD = d;
        }
        return best;
    }

    private static double ground(ServerWorld w, double x, double z, double fallback) {
        int bx = (int) Math.floor(x), bz = (int) Math.floor(z);
        if (!w.getChunkManager().isChunkLoaded(bx >> 4, bz >> 4)) return fallback;
        return w.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, bx, bz);
    }
}
