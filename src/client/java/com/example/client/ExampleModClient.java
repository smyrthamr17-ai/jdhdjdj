package com.example.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class ExampleModClient implements ClientModInitializer {
    private static final double MIN_FALL = 5.0;   // أقل مسافة سقوط (بلوكات)
    private static final float MAX_RANGE = 3.0f;  // أقصى مسافة للهدف (بلوكات)

    private static KeyMapping toggleKey;

    private boolean enabled = false;
    private int stage = 0;      // 0 = خامل، 1 = ينتظر ضربة الفأس، 2 = ينتظر ضربة المبس
    private int timer = 0;      // ⏱ عداد التكات من بداية المحاولة
    private int pause = 0;      // ⏱ عداد الراحة بين المحاولات
    private int prevSlot = 0;
    private int maceSlot = -1;
    private Entity target;

    @Override
    public void onInitializeClient() {
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.automace.swap",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                KeyMapping.Category.register(Identifier.fromNamespaceAndPath("automace", "main"))
        ));

        // ⏱ هذا السطر يخلي دالة tick تشتغل مرة كل تك (20 مرة بالثانية)
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    private void tick(Minecraft mc) {
        if (mc.player == null || mc.gameMode == null || mc.level == null) {
            stage = 0;
            return;
        }

        while (toggleKey.consumeClick()) {
            enabled = !enabled;
            if (!enabled && stage != 0) finish(mc);
            mc.player.displayClientMessage(
                    Component.literal("AutoMace: " + (enabled ? "ON" : "OFF")), true);
        }

        if (!enabled) return;

        if (pause > 0) pause--;   // ⏱ ينقص الراحة تك بعد تك

        if (stage == 0) {
            if (pause == 0 && mc.screen == null) tryStart(mc);
            return;
        }

        timer++;                  // ⏱ يزيد العداد تك بعد تك
        if (timer > 60            // ⏱ المهلة القصوى: 60 تك = 3 ثواني
                || target == null
                || !target.isAlive()
                || mc.player.distanceTo(target) > MAX_RANGE
                || mc.player.onGround()) {
            finish(mc);
            return;
        }

        Inventory inv = mc.player.getInventory();

        faceTarget(mc, target);

        if (stage == 1) {
            // ⏱ ينتظر اكتمال الـ cooldown (ما فيه رقم تك ثابت)
            if (mc.player.getAttackStrengthScale(0f) < 1f) return;
            mc.gameMode.attack(mc.player, target);
            mc.player.swing(InteractionHand.MAIN_HAND);
            inv.setSelectedSlot(maceSlot);
            stage = 2;
        } else if (stage == 2) {
            // ⏱ ينتظر اكتمال الـ cooldown مرة ثانية قبل ضربة المبس
            if (mc.player.getAttackStrengthScale(0f) < 1f) return;
            mc.gameMode.attack(mc.player, target);
            mc.player.swing(InteractionHand.MAIN_HAND);
            finish(mc);
        }
    }

    private void tryStart(Minecraft mc) {
        if (mc.player.onGround()) return;
        if (mc.player.getDeltaMovement().y >= 0) return;
        if (mc.player.fallDistance < MIN_FALL) return;

        List<LivingEntity> list = mc.level.getEntitiesOfClass(
                LivingEntity.class,
                mc.player.getBoundingBox().inflate(MAX_RANGE),
                e -> e != mc.player && e.isAlive());

        Entity best = null;
        float bestDist = MAX_RANGE;
        for (LivingEntity e : list) {
            float d = mc.player.distanceTo(e);
            if (d <= bestDist) {
                best = e;
                bestDist = d;
            }
        }
        if (best == null) return;

        Inventory inv = mc.player.getInventory();
        int axe = -1;
        int mace = -1;
        for (int i = 0; i < 9; i++) {
            if (axe == -1 && inv.getItem(i).is(ItemTags.AXES)) axe = i;
            if (mace == -1 && inv.getItem(i).is(Items.MACE)) mace = i;
        }
        if (axe == -1 || mace == -1) return;

        target = best;
        maceSlot = mace;
        prevSlot = inv.getSelectedSlot();
        timer = 0;                // ⏱ يصفر العداد عند بداية محاولة جديدة
        stage = 1;
        inv.setSelectedSlot(axe);
    }

    private void faceTarget(Minecraft mc, Entity t) {
        Vec3 from = mc.player.getEyePosition();
        Vec3 to = t.getEyePosition();
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double dz = to.z - from.z;
        double horiz = Math.sqrt(dx * dx + dz * dz);

        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, horiz));

        mc.player.setYRot(yaw);
        mc.player.setXRot(pitch);
    }

    private void finish(Minecraft mc) {
        if (mc.player != null) {
            mc.player.getInventory().setSelectedSlot(prevSlot);
        }
        stage = 0;
        target = null;
        pause = 20;               // ⏱ راحة 20 تك = ثانية بعد كل محاولة
    }
	}
