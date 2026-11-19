package com.witchersbestiary.client;

import com.witchersbestiary.equipment.armory.LegendaryAbilities;
import com.witchersbestiary.equipment.armory.LegendaryAbilities.Activate;
import com.witchersbestiary.equipment.armory.LegendaryAbilities.View;
import com.witchersbestiary.equipment.armory.LegendaryAbilities.HammerInput;
import com.witchersbestiary.registry.ModItems;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public final class LegendaryAbilityClient {
    private static View view;
    private static HammerHold hammerHold;

    private record HammerHold(ClientPlayerEntity player, ItemStack stack, int slot) {}

    public static void initialize() {
        var binding = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.witchersbestiary.legendary_ability",
                InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_B, "category.witchersbestiary"));
        ClientPlayNetworking.registerGlobalReceiver(View.ID, (packet, context) ->
                context.client().execute(() -> view = packet));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> clear());
        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, world) -> clear());
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            boolean pressed = false;
            while (binding.wasPressed()) pressed = true;
            var player = client.player;
            boolean usable = !binding.isUnbound() && client.isWindowFocused() && client.currentScreen == null
                    && player != null && player.isAlive() && !player.isSpectator();
            if (hammerHold != null) {
                boolean valid = usable && hammerHold.player() == player && hammerHold.stack() == player.getMainHandStack()
                        && hammerHold.slot() == player.getInventory().selectedSlot;
                if (!valid || !binding.isPressed()) {
                    hammerHold = null;
                    if (ClientPlayNetworking.canSend(HammerInput.ID))
                        ClientPlayNetworking.send(valid ? HammerInput.RELEASE : HammerInput.CANCEL);
                }
                return;
            }
            if (!pressed || !usable) return;
            var stack = player.getMainHandStack();
            if (stack.isOf(ModItems.BONECRUSHER_CLUB)) {
                if (binding.isPressed() && ClientPlayNetworking.canSend(HammerInput.ID)) {
                    hammerHold = new HammerHold(player, stack, player.getInventory().selectedSlot);
                    ClientPlayNetworking.send(HammerInput.START);
                }
            } else if ((stack.isOf(ModItems.BLOOD_LEGACY) || stack.isOf(ModItems.GRAVE_BLADE))
                    && ClientPlayNetworking.canSend(Activate.ID)) {
                ClientPlayNetworking.send(Activate.INSTANCE);
            }
        });
        HudRenderCallback.EVENT.register((draw, tick) -> render(draw, MinecraftClient.getInstance(), binding));
    }

    private static void clear() {
        view = null;
        hammerHold = null;
    }

    private static void render(DrawContext draw, MinecraftClient client, KeyBinding binding) {
        if (view == null || client.player == null || client.player.isSpectator() || client.currentScreen != null
                || client.options.hudHidden || com.witchersbestiary.client.fist.FistClient.status() != null) return;
        var stack = client.player.getMainHandStack();
        boolean blood = stack.isOf(ModItems.BLOOD_LEGACY);
        if (!blood && !stack.isOf(ModItems.GRAVE_BLADE)) return;

        int remaining = blood ? view.blood() : view.grave();
        int duration = blood ? LegendaryAbilities.BLOOD_DURATION : LegendaryAbilities.GRAVE_DURATION;
        int cooldown = blood ? LegendaryAbilities.BLOOD_COOLDOWN : LegendaryAbilities.GRAVE_COOLDOWN;
        String ability = blood ? "bloodlust" : "spectral_summon";
        int color = blood ? 0xffea5258 : 0xff65dff2;
        Text title = Text.translatable("ability.witchersbestiary." + ability);
        Text label;
        float fraction;
        if (remaining > cooldown) {
            int active = remaining - cooldown;
            label = Text.translatable("ability.witchersbestiary.active", title, (active + 19) / 20);
            fraction = active / (float) duration;
        } else if (remaining != 0) {
            remaining = Math.abs(remaining);
            label = Text.translatable("ability.witchersbestiary.cooldown", title, (remaining + 19) / 20);
            fraction = Math.max(0, 1 - remaining / (float) cooldown);
            color = 0xffa9a7a4;
        } else if (blood && view.charge() < LegendaryAbilities.FULL_CHARGE) {
            label = Text.translatable("ability.witchersbestiary.charge", title, view.charge(), LegendaryAbilities.FULL_CHARGE);
            fraction = view.charge() / (float) LegendaryAbilities.FULL_CHARGE;
        } else {
            label = Text.translatable("ability.witchersbestiary.ready", title, binding.getBoundKeyLocalizedText());
            fraction = 1;
        }
        int x = draw.getScaledWindowWidth() / 2, y = draw.getScaledWindowHeight() - (blood ? 80 : 67);
        draw.drawCenteredTextWithShadow(client.textRenderer, label, x, y, color);
        draw.fill(x - 41, y + 11, x + 41, y + 16, 0xe0201c1d);
        draw.fill(x - 40, y + 12, x - 40 + Math.round(80 * fraction), y + 15, color);
    }
}
