package com.example.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

public class ExampleModClient implements ClientModInitializer {
    private static KeyMapping swapKey;

    @Override
    public void onInitializeClient() {
        swapKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.automace.swap",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                KeyMapping.Category.register(Identifier.fromNamespaceAndPath("automace", "main"))
        ));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (swapKey.consumeClick()) {
                attack(mc);
            }
        });
    }

    private void attack(Minecraft mc) {
        if (mc.player == null || mc.gameMode == null) return;

        Entity target = mc.crosshairPickEntity;
        if (!(target instanceof LivingEntity)) return;

        Inventory inv = mc.player.getInventory();

        int mace = -1;
        for (int i = 0; i < 9; i++) {
            if (inv.getItem(i).is(Items.MACE)) {
                mace = i;
                break;
            }
        }
        if (mace == -1) return;

        int prev = inv.getSelectedSlot();

        inv.setSelectedSlot(mace);
        mc.gameMode.attack(mc.player, target);
        mc.player.swing(InteractionHand.MAIN_HAND);
        inv.setSelectedSlot(prev);
    }
	}
