package com.github.ykele233gdf.autofishing;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;

import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import org.lwjgl.glfw.GLFW;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.Identifier;


public class AutoFishing implements ClientModInitializer {
	public static final String MOD_ID = "autofishing";

	private int castCooldown = 0;
	private boolean enabled = false;
	private KeyMapping toggleKeyBinding;

	private static final int CAST_COOLDOWN_RESET = 10;
	private static final int HOOK_FIRST_IN_WATER_RESET = 10;
	private static final float HOOK_GET_FISH_VELOCITY_Y = -0.1f;

	private int hookFirstInWater = 10;

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitializeClient() {

		toggleKeyBinding = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.autofishing.toggle",
				GLFW.GLFW_KEY_K,
				KeyMapping.Category.register(Identifier.fromNamespaceAndPath(
						"autofishing", "category")
				)
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {

			while(toggleKeyBinding.consumeClick()){
				enabled = !enabled;
				if(client.player != null){
					client.player.sendOverlayMessage(
							Component.translatable(
									"message.autofishing.toggle",
									Component.translatable(
											enabled ? "message.autofishing.on" : "message.autofishing.off"
									)
							)
					);
				}
			}
			if(!enabled) return;

			if(client.player == null) return; //检查玩家对象是否被创建
			if(client.getConnection() == null) return; //确保网络连接存在

			ItemStack mainHand = client.player.getMainHandItem();

			if(mainHand.getItem() != Items.FISHING_ROD) return;

			FishingHook hook = client.player.fishing;

			if (hook != null) {
				if (hook.isInWater()){

					hookFirstInWater--;
					if(hookFirstInWater < 0) {

						Vec3 hookVelocity = hook.getDeltaMovement();

						if(hookVelocity.y < HOOK_GET_FISH_VELOCITY_Y) {
							sendRightClick(client);
							castCooldown = CAST_COOLDOWN_RESET;
							hookFirstInWater = HOOK_FIRST_IN_WATER_RESET;
						}
					}
				}
			} else {
				if (castCooldown > 0) {
					castCooldown--;
					return;
				}
				sendRightClick(client);
				castCooldown = CAST_COOLDOWN_RESET;
				hookFirstInWater = HOOK_FIRST_IN_WATER_RESET;
			}
		} );
	}
	private void sendRightClick(Minecraft client) {
		ServerboundUseItemPacket packet = new ServerboundUseItemPacket(
			InteractionHand.MAIN_HAND,
			0,
			client.player.getYRot(),
			client.player.getXRot()
		);
		if (client.getConnection() != null) {
			client.getConnection().send(packet);
		}
	}
}