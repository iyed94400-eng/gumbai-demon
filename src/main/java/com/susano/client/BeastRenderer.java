package com.susano.client;

import com.susano.TailedBeastEntity;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/**
 * La bete a queues, d'apres la photo de reference : accroupie, crane cubique avec un oeil rouge a anneaux dans une tache noire fissuree,
 * machoire en peigne (des dizaines de crocs fins), bras massifs a quatre griffes carrees, neuf grosses queues ecailleuses en eventail.
 * Les donnees (PARTS, TAILS) sont generees depuis le meme modele que l'apercu de dev ; repere : +Z devant, +Y haut, origine au sol.
 */
public class BeastRenderer extends EntityRenderer<TailedBeastEntity> {
    private static final Identifier STONE = new Identifier("susanoo", "textures/entity/beast_stone.png");
    private static final Identifier SCALES = new Identifier("susanoo", "textures/entity/beast_scales.png");
    private static final Identifier EYE = new Identifier("susanoo", "textures/entity/beast_eye.png");

    /** {groupe, texture, teinte, cx, cy, cz, rx, ry, rz, sx, sy, sz} ; groupes : 0 fixe, 1 bras -X, 2 bras +X, 3 machoire. */
    private static final float[][] PARTS = {
        {0f, 0f, 1f, 0f, 3.5f, -0.5f, -8f, 0f, 0f, 5.8f, 4.8f, 6.8f},
        {0f, 0f, 0.95f, 0f, 4.2f, 2.4f, -4f, 0f, 0f, 6.6f, 5f, 3.6f},
        {0f, 0f, 0.85f, 0f, 1.9f, 0.6f, 0f, 0f, 0f, 5f, 2.6f, 6.4f},
        {0f, 0f, 1f, 0f, 3.4f, -4f, 0f, 0f, 0f, 5.2f, 4.4f, 3.2f},
        {0f, 0f, 0.9f, 0f, 6.3f, -0.4f, -6f, 0f, 0f, 4f, 1.6f, 5.2f},
        {0f, 0f, 1f, 0f, 5.8f, 3.7f, -20f, 0f, 0f, 3.6f, 3.6f, 3.2f},
        {0f, 0f, 0.95f, 0f, 7f, 4.5f, -28f, 0f, 0f, 3.2f, 2.8f, 2.6f},
        {0f, 0f, 1.05f, 0f, 8f, 5.4f, -8f, 0f, 0f, 2.8f, 2.7f, 3.4f},
        {3f, 0f, 0.9f, 0f, 6.45f, 6.5f, 26f, 0f, 0f, 3f, 1.4f, 4.2f},
        {2f, 0f, 1f, 5f, 4.9f, 3f, 0f, 0f, 0f, 3.6f, 2.6f, 2.6f},
        {2f, 0f, 0.95f, 6.3f, 2.8f, 4f, 8f, 0f, 0f, 2.7f, 4.8f, 2.9f},
        {2f, 0f, 0.9f, 6.3f, 0.55f, 5.5f, 0f, 0f, 0f, 3f, 1.1f, 3.3f},
        {2f, 3f, 1f, 5.1f, 0.45f, 7.3f, 0f, 0f, 0f, 0.55f, 0.8f, 1.2f},
        {2f, 3f, 1f, 5.8f, 0.45f, 7.3f, 0f, 0f, 0f, 0.55f, 0.8f, 1.2f},
        {2f, 3f, 1f, 6.5f, 0.45f, 7.3f, 0f, 0f, 0f, 0.55f, 0.8f, 1.2f},
        {2f, 3f, 1f, 7.2f, 0.45f, 7.3f, 0f, 0f, 0f, 0.55f, 0.8f, 1.2f},
        {1f, 0f, 1f, -4.9f, 3.6f, 3.4f, 32f, -12f, 0f, 2.4f, 2.4f, 5.8f},
        {1f, 0f, 0.9f, -6.3f, 0.55f, 7.1f, 0f, 0f, 0f, 3f, 1.1f, 3.3f},
        {1f, 3f, 1f, -7.6f, 0.45f, 8.9f, 0f, 0f, 0f, 0.55f, 0.8f, 1.2f},
        {1f, 3f, 1f, -6.9f, 0.45f, 8.9f, 0f, 0f, 0f, 0.55f, 0.8f, 1.2f},
        {1f, 3f, 1f, -6.2f, 0.45f, 8.9f, 0f, 0f, 0f, 0.55f, 0.8f, 1.2f},
        {1f, 3f, 1f, -5.5f, 0.45f, 8.9f, 0f, 0f, 0f, 0.55f, 0.8f, 1.2f},
        {0f, 0f, 0.95f, -3.5f, 2.3f, -4f, 10f, 0f, 0f, 2.7f, 4.2f, 3.8f},
        {0f, 0f, 0.9f, -3.5f, 0.45f, -2.5f, 0f, 0f, 0f, 2.7f, 0.9f, 3.3f},
        {0f, 3f, 1f, -4.55f, 0.4f, -0.7f, 0f, 0f, 0f, 0.55f, 0.8f, 1.1f},
        {0f, 3f, 1f, -3.85f, 0.4f, -0.7f, 0f, 0f, 0f, 0.55f, 0.8f, 1.1f},
        {0f, 3f, 1f, -3.15f, 0.4f, -0.7f, 0f, 0f, 0f, 0.55f, 0.8f, 1.1f},
        {0f, 3f, 1f, -2.45f, 0.4f, -0.7f, 0f, 0f, 0f, 0.55f, 0.8f, 1.1f},
        {0f, 0f, 0.95f, 3.5f, 2.3f, -4f, 10f, 0f, 0f, 2.7f, 4.2f, 3.8f},
        {0f, 0f, 0.9f, 3.5f, 0.45f, -2.5f, 0f, 0f, 0f, 2.7f, 0.9f, 3.3f},
        {0f, 3f, 1f, 2.45f, 0.4f, -0.7f, 0f, 0f, 0f, 0.55f, 0.8f, 1.1f},
        {0f, 3f, 1f, 3.15f, 0.4f, -0.7f, 0f, 0f, 0f, 0.55f, 0.8f, 1.1f},
        {0f, 3f, 1f, 3.85f, 0.4f, -0.7f, 0f, 0f, 0f, 0.55f, 0.8f, 1.1f},
        {0f, 3f, 1f, 4.55f, 0.4f, -0.7f, 0f, 0f, 0f, 0.55f, 0.8f, 1.1f}
    };
    private static final int HEAD_IDX = 7, JAW_IDX = 8;
    /** Pivots d'animation (epaules gauche/droite, charniere de la machoire). */
    private static final float[][] PIVOT = {{0, 0, 0}, {-3.4f, 4.7f, 2.6f}, {3.4f, 4.7f, 2.6f}, {0f, 7.3f, 4.7f}};

    /** {yaw depuis l'arriere (deg), portee, hauteur, epaisseur, teinte}. */
    private static final float[][] TAILS = {
        {-138f, 8.5f, 12f, 2.7f, 0.95f},
        {-102f, 8f, 15f, 2.9f, 1f},
        {-66f, 6.5f, 17f, 2.9f, 0.92f},
        {-31f, 5f, 18.5f, 2.9f, 1f},
        {0f, 3.5f, 19.5f, 3.1f, 0.96f},
        {31f, 5f, 18.5f, 2.9f, 1f},
        {66f, 6.5f, 17f, 2.9f, 0.92f},
        {102f, 8f, 15f, 2.9f, 1f},
        {138f, 8.5f, 12f, 2.7f, 0.95f}
    };
    private static final int TAIL_SEG = 10;

    public BeastRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
        this.shadowRadius = 0f;
    }

    @Override
    public Identifier getTexture(TailedBeastEntity entity) {
        return STONE;
    }

    @Override
    public boolean shouldRender(TailedBeastEntity entity, Frustum frustum, double x, double y, double z) {
        return true;
    }

    // ------------------------------------------------------------------ outils de dessin

    private static final class G {
        final VertexConsumer vc;
        final MatrixStack ms;
        final int light;
        float tint = 1f;
        MatrixStack.Entry fixed;

        G(VertexConsumer vc, MatrixStack ms, int light) {
            this.vc = vc;
            this.ms = ms;
            this.light = light;
        }

        /** Boite centree sur l'origine courante. */
        void box(float sx, float sy, float sz) {
            float x0 = -sx / 2, x1 = sx / 2, y0 = -sy / 2, y1 = sy / 2, z0 = -sz / 2, z1 = sz / 2;
            quad(x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, 0, 1, 0, 1.00f);
            quad(x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, 0, -1, 0, 0.55f);
            quad(x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, 0, 0, 1, 0.82f);
            quad(x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, 0, 0, -1, 0.78f);
            quad(x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, 1, 0, 0, 0.68f);
            quad(x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, -1, 0, 0, 0.88f);
        }

        void quad(float ax, float ay, float az, float bx, float by, float bz,
                  float cx, float cy, float cz, float dx, float dy, float dz,
                  float nx, float ny, float nz, float shade) {
            MatrixStack.Entry en = fixed != null ? fixed : ms.peek();
            Matrix4f pm = en.getPositionMatrix();
            Matrix3f nm = en.getNormalMatrix();
            int c = (int) (255 * Math.min(1f, shade * tint));
            v(pm, nm, ax, ay, az, 0f, 1f, c, nx, ny, nz);
            v(pm, nm, bx, by, bz, 1f, 1f, c, nx, ny, nz);
            v(pm, nm, cx, cy, cz, 1f, 0f, c, nx, ny, nz);
            v(pm, nm, dx, dy, dz, 0f, 0f, c, nx, ny, nz);
        }

        private void v(Matrix4f pm, Matrix3f nm, float x, float y, float z, float u, float vv, int c,
                       float nx, float ny, float nz) {
            vc.vertex(pm, x, y, z).color(c, c, c, 255).texture(u, vv).overlay(OverlayTexture.DEFAULT_UV)
                    .light(light).normal(nm, nx, ny, nz).next();
        }
    }

    /** Place l'origine sur la piece (translation puis rotations Y, X, Z : meme ordre que l'apercu). */
    private static void place(MatrixStack ms, float[] p) {
        ms.translate(p[3], p[4], p[5]);
        if (p[7] != 0f) ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(p[7]));
        if (p[6] != 0f) ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(p[6]));
        if (p[8] != 0f) ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(p[8]));
    }

    private static void spike(G g, float x, float y, float z, float len) {
        g.ms.push();
        g.ms.translate(x, y, z);
        g.box(0.12f, len, 0.12f);
        g.ms.pop();
    }

    // ------------------------------------------------------------------ rendu

    @Override
    public void render(TailedBeastEntity e, float yaw, float td, MatrixStack ms, VertexConsumerProvider vcp, int light) {
        float age = e.age + td;
        int phase = e.getPhase();
        float walk = phase == 1 ? MathHelper.sin(age * 0.55f) : 0f;
        float breathe = MathHelper.sin(age * 0.12f) * 0.05f;
        float[] swing = new float[4];
        swing[1] = walk * 16f;
        swing[2] = -walk * 16f;
        swing[3] = phase == 2 ? 6f + 5f * MathHelper.sin(age * 0.3f) : (phase == 1 ? 2f : 0f);
        int bl = Math.max(LightmapTextureManager.getBlockLightCoordinates(light), 6);
        int lit = LightmapTextureManager.pack(bl, LightmapTextureManager.getSkyLightCoordinates(light));

        ms.push();
        ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-MathHelper.lerpAngleDegrees(td, e.prevYaw, e.getYaw())));
        ms.translate(0, breathe + 0.15f * Math.abs(walk), 0);
        MatrixStack.Entry headEntry = null;

        // ---- passe 1 : corps de pierre, machoire, crocs, griffes
        G g = new G(vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(STONE)), ms, lit);
        for (int i = 0; i < PARTS.length; i++) {
            float[] p = PARTS[i];
            int grp = (int) p[0];
            ms.push();
            if (grp != 0) {
                float[] pv = PIVOT[grp];
                ms.translate(pv[0], pv[1], pv[2]);
                ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(swing[grp]));
                ms.translate(-pv[0], -pv[1], -pv[2]);
            }
            ms.push();
            place(ms, p);
            g.tint = p[1] == 3f ? Math.min(1.6f, 1.3f * p[2]) : p[2];
            if (i == HEAD_IDX) headEntry = ms.peek();
            g.box(p[9], p[10], p[11]);
            g.tint = 0.55f;
            if (i == HEAD_IDX) {
                // crocs du haut : pendent sous le crane (devant et sur les cotes)
                for (int k = -9; k <= 9; k++) spike(g, k * 0.14f, -1.7f, 1.6f, 0.75f);
                for (int s = -1; s <= 1; s += 2) {
                    for (int k = -8; k < 8; k++) spike(g, s * 1.3f, -1.7f, k * 0.2f, 0.65f);
                }
            } else if (i == JAW_IDX) {
                // crocs du bas : dressent comme un peigne sur la machoire
                for (int k = -9; k <= 9; k++) spike(g, k * 0.15f, 1.0f, 2.0f, 0.8f);
                for (int s = -1; s <= 1; s += 2) {
                    for (int k = -9; k < 9; k++) spike(g, s * 1.4f, 1.0f, k * 0.21f, 0.7f);
                }
            }
            ms.pop();
            ms.pop();
        }

        // ---- passe 2 : neuf queues ecailleuses, segments qui s'enroulent vers le haut
        G tg = new G(vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(SCALES)), ms, lit);
        for (int i = 0; i < TAILS.length; i++) {
            float[] t = TAILS[i];
            float phi = (float) Math.toRadians(t[0] + 4f * MathHelper.sin(age * 0.07f + i * 1.3f));
            float hx = MathHelper.sin(phi), hz = -MathHelper.cos(phi);
            float reach = t[1] * (1f + 0.04f * MathHelper.sin(age * 0.09f + i));
            float hgt = t[2] * (1f + 0.03f * MathHelper.sin(age * 0.11f + i * 0.7f));
            tg.tint = t[4];
            float lx = hx * 1.4f, ly = 4.2f, lz = -5.0f + hz * 0.4f;
            for (int k = 1; k <= TAIL_SEG; k++) {
                float a = (k / (float) TAIL_SEG) * (float) (Math.PI / 2);
                float nx = hx * 1.4f + hx * reach * MathHelper.sin(a);
                float nz = -5.0f + hz * 0.4f + hz * reach * MathHelper.sin(a);
                float ny = 4.2f + hgt * (1f - MathHelper.cos(a));
                float dx = nx - lx, dy = ny - ly, dz = nz - lz;
                float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
                float w = t[3] * (1f - 0.62f * ((k - 0.5f) / TAIL_SEG));
                ms.push();
                ms.translate((nx + lx) / 2f, (ny + ly) / 2f, (nz + lz) / 2f);
                ms.multiply(new Quaternionf().rotationTo(0f, 1f, 0f, dx / len, dy / len, dz / len));
                tg.box(w, len * 1.06f, w);
                ms.pop();
                lx = nx;
                ly = ny;
                lz = nz;
            }
        }

        // ---- passe 3 : le gros oeil rouge (face avant + joues), decolle de la tete pour ne jamais disparaitre de loin
        if (headEntry != null) {
            G eg = new G(vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(EYE)), ms, LightmapTextureManager.MAX_LIGHT_COORDINATE);
            eg.fixed = headEntry;
            float hx = PARTS[HEAD_IDX][9] / 2f + 0.09f;     // demi-largeur du crane + decalage
            float hz = PARTS[HEAD_IDX][11] / 2f + 0.09f;    // demi-profondeur du crane + decalage
            // devant
            eg.quad(-1.38f, -1.3f, hz, 1.38f, -1.3f, hz, 1.38f, 1.35f, hz, -1.38f, 1.35f, hz, 0, 0, 1, 1f);
            // joue droite (+X) et joue gauche (-X)
            eg.quad(hx, -1.3f, 1.6f, hx, -1.3f, -1.0f, hx, 1.3f, -1.0f, hx, 1.3f, 1.6f, 1, 0, 0, 1f);
            eg.quad(-hx, -1.3f, -1.0f, -hx, -1.3f, 1.6f, -hx, 1.3f, 1.6f, -hx, 1.3f, -1.0f, -1, 0, 0, 1f);
        }
        ms.pop();
    }
}
