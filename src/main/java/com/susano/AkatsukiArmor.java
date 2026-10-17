package com.susano;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

/**
 * La tenue de l'Akatsuki : chapeau de paille, manteau noir aux nuages rouges, pantalon bande, sandales.
 * Meilleure que le Netherite (24 points d'armure contre 20, 5 de robustesse, 15 % de resistance au recul, durabilite x60).
 * Tenue complete : Vitesse I, Force I et Resistance au feu.
 */
public final class AkatsukiArmor {
    private AkatsukiArmor() { }

    public static final ArmorMaterial MATERIAL = new ArmorMaterial() {
        @Override
        public int getDurability(ArmorItem.Type type) {
            int base = switch (type) {
                case BOOTS -> 13;
                case LEGGINGS -> 15;
                case CHESTPLATE -> 16;
                case HELMET -> 11;
            };
            return base * 60;
        }

        @Override
        public int getProtection(ArmorItem.Type type) {
            return switch (type) {
                case BOOTS -> 4;
                case LEGGINGS -> 7;
                case CHESTPLATE -> 9;
                case HELMET -> 4;
            };
        }

        @Override public int getEnchantability() { return 25; }
        @Override public SoundEvent getEquipSound() { return SoundEvents.ITEM_ARMOR_EQUIP_NETHERITE; }
        @Override public Ingredient getRepairIngredient() { return Ingredient.ofItems(Items.NETHERITE_INGOT); }
        /** Les textures sont dans assets/minecraft/textures/models/armor/akatsuki_layer_1.png et _2.png */
        @Override public String getName() { return "akatsuki"; }
        @Override public float getToughness() { return 5.0f; }
        @Override public float getKnockbackResistance() { return 0.15f; }
    };

    private static boolean wearsFullSet(ServerPlayerEntity p) {
        return p.getEquippedStack(EquipmentSlot.HEAD).getItem() == SusanooMod.AKATSUKI_HAT
                && p.getEquippedStack(EquipmentSlot.CHEST).getItem() == SusanooMod.AKATSUKI_CLOAK
                && p.getEquippedStack(EquipmentSlot.LEGS).getItem() == SusanooMod.AKATSUKI_PANTS
                && p.getEquippedStack(EquipmentSlot.FEET).getItem() == SusanooMod.AKATSUKI_SANDALS;
    }

    /** Bonus de tenue complete (rafraichi toutes les 2 secondes, sans particules). */
    public static void tick(MinecraftServer server) {
        if (server.getTicks() % 40 != 0) return;
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            if (!wearsFullSet(p)) continue;
            p.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 100, 0, true, false, true));
            p.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 100, 0, true, false, true));
            p.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 100, 0, true, false, true));
        }
    }
}
