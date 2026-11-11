
package dev.async;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public final class TickRate {

    private final AsyncConfig config;

    private boolean skipNextAttack;
    private boolean criticalAttackPerformed;

    public TickRate(AsyncConfig config) {
        this.config = config;
    }

    public boolean isEnabled() {
        return config.isEnabled();
    }

    public TargetMode getMode() {
        return config.getTargetMode();
    }

    public TickMode getAttackMode() {
        return config.getAttackMode();
    }

    public TickWeapon getWeaponMode() {
        return config.getWeaponMode();
    }

    public boolean isKeyToggleEnabled() {
        return config.isKeyToggleEnabled();
    }

    public int getToggleKey() {
        return config.getToggleKey();
    }

    public void toggleEnabled() {
        config.setEnabled(!config.isEnabled());
        criticalAttackPerformed = false;
    }

    public void toggleTargetMode() {
        config.setTargetMode(config.getTargetMode().next());
    }

    public void toggleAttackMode() {
        config.setAttackMode(config.getAttackMode().next());
        criticalAttackPerformed = false;
    }

    public void toggleWeaponMode() {
        config.setWeaponMode(config.getWeaponMode().next());
    }

    public void toggleKeyToggleEnabled() {
        config.setKeyToggleEnabled(!config.isKeyToggleEnabled());
    }

    public void setToggleKey(int key) {
        config.setToggleKey(key);
    }

    public void suppressNextAttack() {
        skipNextAttack = true;
    }

    public void onEndTick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;

        if (client.world == null || player == null) {
            skipNextAttack = false;
            criticalAttackPerformed = false;
            return;
        }

        if (player.isOnGround()) {
            criticalAttackPerformed = false;
        }

        if (client.currentScreen != null) {
            skipNextAttack = true;
            return;
        }

        if (skipNextAttack) {
            skipNextAttack = false;
            return;
        }

        if (!config.isEnabled()
                || client.interactionManager == null
                || !player.isAlive()
                || player.isSpectator()) {
            return;
        }

        if (!(client.crosshairTarget instanceof EntityHitResult hit)) {
            return;
        }

        Entity target = hit.getEntity();

        if (!TickDown.isValidTarget(
                client,
                target,
                config.getTargetMode()
        )) {
            return;
        }

        ItemStack weapon = player.getMainHandStack();

        String itemId = weapon.isEmpty()
                ? ""
                : Registries.ITEM.getId(weapon.getItem()).getPath();

        boolean isMace = itemId.equals("mace");
        boolean isAxe = itemId.endsWith("_axe");
        boolean isSword = itemId.endsWith("_sword");

        if (config.getWeaponMode() == TickWeapon.WEAPONS_ONLY
                && !isAllowedWeapon(weapon)) {
            return;
        }

        double maxDistance = config.getAttackDistance();
        Vec3d eyePos = player.getEyePos();
        Box targetBox = target.getBoundingBox();

        Vec3d closestPoint = new Vec3d(
                clamp(eyePos.x, targetBox.minX, targetBox.maxX),
                clamp(eyePos.y, targetBox.minY, targetBox.maxY),
                clamp(eyePos.z, targetBox.minZ, targetBox.maxZ)
        );

        if (eyePos.squaredDistanceTo(closestPoint)
                > maxDistance * maxDistance) {
            return;
        }

        if (!isMace && player.getAttackCooldownProgress(1.0F) < 1.0F) {
            return;
        }

        if (config.getAttackMode() == TickMode.FAST) {
            if (criticalAttackPerformed || !isCriticalTiming(player)) {
                return;
            }

            if (isAxe && player.getVelocity().y >= -0.08) {
                return;
            }
        }

        if (!isMace && !isSword && !isAxe) {
            return;
        }

        client.interactionManager.attackEntity(player, target);
        player.swingHand(Hand.MAIN_HAND);

        if (config.getAttackMode() == TickMode.FAST) {
            criticalAttackPerformed = true;
        }
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static boolean isAllowedWeapon(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        String itemId = Registries.ITEM.getId(stack.getItem()).getPath();

        return itemId.endsWith("_sword")
                || itemId.endsWith("_axe")
                || itemId.equals("mace");
    }

    private static boolean isCriticalTiming(ClientPlayerEntity player) {
        return !player.isOnGround()
                && player.getVelocity().y < -0.08
                && player.fallDistance > 0.1F
                && !player.isSprinting()
                && !player.isClimbing()
                && !player.isTouchingWater()
                && !player.hasVehicle()
                && !player.hasStatusEffect(StatusEffects.BLINDNESS);
    }
}
