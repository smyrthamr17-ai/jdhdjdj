package com.example.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class ExampleModClient implements ClientModInitializer {
    private static KeyBinding swapKey;

    @Override
    public void onInitializeClient() {
        swapKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.automace.swap",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                KeyBinding.Category.create(Identifier.of("automace", "main"))
        ));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (swapKey.wasPressed()) {
                attack(mc);
            }
        });
    }

    private void attack(MinecraftClient mc) {
        if (mc.player == null || mc.interactionManager == null) return;

        Entity target = mc.targetedEntity;
        if (!(target instanceof LivingEntity)) return;

        PlayerInventory inv = mc.player.getInventory();

        int mace = -1;
        for (int i = 0; i < 9; i++) {
            if (inv.getStack(i).isOf(Items.MACE)) {
                mace = i;
                break;
            }
        }
        if (mace == -1) return;

        int prev = inv.getSelectedSlot();

        inv.setSelectedSlot(mace);
        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);
        inv.setSelectedSlot(prev);
    }
	}
