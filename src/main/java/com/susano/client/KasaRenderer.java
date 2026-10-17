package com.susano.client;

import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** Le chapeau de paille conique de l'Akatsuki, avec sa frange de papiers blancs, pose sur la tete. */
public class KasaRenderer implements ArmorRenderer {
    private static final Identifier PLAIN = new Identifier("susanoo", "textures/armor/plain.png");
    private static final int N = 24;
    private static final float APEX = -1.02f, RIM = -0.38f, R = 0.62f;
    private static final float[] RING = {0f, 0.30f, 0.58f, 0.82f, 1f};

    @Override
    public void render(MatrixStack ms, VertexConsumerProvider vcp, ItemStack stack, LivingEntity entity,
                       EquipmentSlot slot, int light, BipedEntityModel<LivingEntity> model) {
        ms.push();
        model.head.rotate(ms);
        VertexConsumer vc = vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(PLAIN));
        MatrixStack.Entry en = ms.peek();
        Matrix4f pm = en.getPositionMatrix();
        Matrix3f nm = en.getNormalMatrix();

        for (int k = 0; k < RING.length - 1; k++) {
            float r0 = R * RING[k], r1 = R * RING[k + 1];
            float y0 = APEX + (RIM - APEX) * RING[k], y1 = APEX + (RIM - APEX) * RING[k + 1];
            for (int i = 0; i < N; i++) {
                double a0 = Math.PI * 2 * i / N, a1 = Math.PI * 2 * (i + 1) / N;
                boolean alt = ((i + k) & 1) == 0;
                int cr = alt ? 214 : 192, cg = alt ? 184 : 160, cb = alt ? 114 : 92;
                float nx = (float) Math.cos((a0 + a1) / 2) * 0.5f, nz = (float) Math.sin((a0 + a1) / 2) * 0.5f;
                // dessus
                quad(vc, pm, nm, light, r0, a0, y0, r0, a1, y0, r1, a1, y1, r1, a0, y1, cr, cg, cb, nx, -0.85f, nz);
                // dessous (plus sombre, legerement plus bas)
                quad(vc, pm, nm, light, r0, a0, y0 + 0.012f, r0, a1, y0 + 0.012f, r1, a1, y1 + 0.012f, r1, a0, y1 + 0.012f,
                        (int) (cr * 0.55), (int) (cg * 0.55), (int) (cb * 0.55), 0f, 0.9f, 0f);
            }
        }
        // bande blanche au bord du chapeau
        for (int i = 0; i < N; i++) {
            double a0 = Math.PI * 2 * i / N, a1 = Math.PI * 2 * (i + 1) / N;
            quad(vc, pm, nm, light, R, a0, RIM - 0.01f, R, a1, RIM - 0.01f, R, a1, RIM + 0.06f, R, a0, RIM + 0.06f,
                    238, 232, 216, (float) Math.cos((a0 + a1) / 2), 0f, (float) Math.sin((a0 + a1) / 2));
        }
        // frange de papiers blancs qui pend tout autour
        for (int i = 0; i < 20; i++) {
            double a = Math.PI * 2 * i / 20;
            float cx = (float) Math.cos(a) * (R - 0.005f), cz = (float) Math.sin(a) * (R - 0.005f);
            float tx = (float) -Math.sin(a) * 0.035f, tz = (float) Math.cos(a) * 0.035f;
            float len = (i % 2 == 0) ? 0.30f : 0.22f;
            vertex(vc, pm, nm, light, cx - tx, RIM + 0.06f, cz - tz, 246, 244, 238, (float) Math.cos(a), 0f, (float) Math.sin(a));
            vertex(vc, pm, nm, light, cx + tx, RIM + 0.06f, cz + tz, 246, 244, 238, (float) Math.cos(a), 0f, (float) Math.sin(a));
            vertex(vc, pm, nm, light, cx + tx, RIM + 0.06f + len, cz + tz, 226, 222, 212, (float) Math.cos(a), 0f, (float) Math.sin(a));
            vertex(vc, pm, nm, light, cx - tx, RIM + 0.06f + len, cz - tz, 226, 222, 212, (float) Math.cos(a), 0f, (float) Math.sin(a));
        }
        ms.pop();
    }

    private static void quad(VertexConsumer vc, Matrix4f pm, Matrix3f nm, int light,
                             float ra, double aa, float ya, float rb, double ab, float yb,
                             float rc, double ac, float yc, float rd, double ad, float yd,
                             int r, int g, int b, float nx, float ny, float nz) {
        vertex(vc, pm, nm, light, ra * (float) Math.cos(aa), ya, ra * (float) Math.sin(aa), r, g, b, nx, ny, nz);
        vertex(vc, pm, nm, light, rb * (float) Math.cos(ab), yb, rb * (float) Math.sin(ab), r, g, b, nx, ny, nz);
        vertex(vc, pm, nm, light, rc * (float) Math.cos(ac), yc, rc * (float) Math.sin(ac), r, g, b, nx, ny, nz);
        vertex(vc, pm, nm, light, rd * (float) Math.cos(ad), yd, rd * (float) Math.sin(ad), r, g, b, nx, ny, nz);
    }

    private static void vertex(VertexConsumer vc, Matrix4f pm, Matrix3f nm, int light,
                               float x, float y, float z, int r, int g, int b, float nx, float ny, float nz) {
        vc.vertex(pm, x, y, z).color(r, g, b, 255).texture(0.5f, 0.5f).overlay(OverlayTexture.DEFAULT_UV)
                .light(light).normal(nm, nx, ny, nz).next();
    }
}
