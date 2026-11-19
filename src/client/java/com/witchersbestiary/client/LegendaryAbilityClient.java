package com.witchersbestiary.client;

import com.witchersbestiary.equipment.armory.LegendaryAbilities;
import com.witchersbestiary.equipment.armory.LegendaryAbilities.Activate;
import com.witchersbestiary.equipment.armory.LegendaryAbilities.View;
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
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public final class LegendaryAbilityClient {
    private static View view;

    public static void initialize() {
        var binding = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.witchersbestiary.legendary_ability",
                InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_B, "category.witchersbestiary"));
        ClientPlayNetworking.registerGlobalReceiver(View.ID, (packet, context) ->
                context.client().execute(() -> view = packet));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> view = null);
        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, world) -> view = null);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            boolean pressed = false;
            while (binding.wasPressed()) pressed = true;
            if (pressed && !binding.isUnbound() && client.isWindowFocused() && client.currentScreen == null && client.player != null
                    && ClientPlayNetworking.canSend(Activate.ID)) {
                var stack = client.player.getMainHandStack();
                if (stack.isOf(ModItems.BLOOD_LEGACY) || stack.isOf(ModItems.GRAVE_BLADE))
                    ClientPlayNetworking.send(Activate.INSTANCE);
            }
        });
        HudRenderCallback.EVENT.register((draw, tick) -> render(draw, MinecraftClient.getInstance(), binding));
    }

    private static void render(DrawContext draw, MinecraftClient client, KeyBinding binding) {
        if (view == null || client.player == null || client.player.isSpectator() || client.currentScreen != null
                || client.options.hudHidden || com.witchersbestiary.client.fist.FistClient.status() != null) return;
        var stack = client.player.getMainHandStack();
        boolean blood = stack.isOf(ModItems.BLOOD_LEGACY);
        if (!blood && !stack.isOf(ModItems.GRAVE_BLADE)) return;

        int remaining = blood ? view.blood() : view.grave();
        int duration = blood ? LegendaryAbilities.BLOOD_DURATION : LegendaryAbilities.GRAVE_DURATION;
        String ability = blood ? "bloodlust" : "spectral_summon";
        int color = blood ? 0xffea5258 : 0xff65dff2;
        Text title = Text.translatable("ability.witchersbestiary." + ability);
        Text label;
        float fraction;
        if (remaining > LegendaryAbilities.COOLDOWN) {
            int active = remaining - LegendaryAbilities.COOLDOWN;
            label = Text.translatable("ability.witchersbestiary.active", title, (active + 19) / 20);
            fraction = active / (float) duration;
        } else if (remaining != 0) {
            remaining = Math.abs(remaining);
            label = Text.translatable("ability.witchersbestiary.cooldown", title, (remaining + 19) / 20);
            fraction = Math.max(0, 1 - remaining / (float) LegendaryAbilities.COOLDOWN);
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
