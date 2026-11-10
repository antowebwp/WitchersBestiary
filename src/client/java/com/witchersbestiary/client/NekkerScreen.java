package com.witchersbestiary.client;

import com.witchersbestiary.nekker.NekkerHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

public final class NekkerScreen extends HandledScreen<NekkerHandler> {
    private ButtonWidget pickup;

    public NekkerScreen(NekkerHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        backgroundHeight = 154;
        playerInventoryTitleY = 37;
    }

    @Override protected void init() {
        super.init();
        pickup = addDrawableChild(ButtonWidget.builder(pickupLabel(), button -> {
            if (client.interactionManager != null) client.interactionManager.clickButton(handler.syncId, 0);
        }).dimensions(x + 8, y + 130, 160, 18).build());
    }

    private Text pickupLabel() {
        return Text.translatable(handler.collecting() ? "nekker.witchersbestiary.pickup_on" : "nekker.witchersbestiary.pickup_off");
    }

    @Override protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        context.fill(x, y, x + backgroundWidth, y + backgroundHeight, 0xff28231e);
        context.fill(x + 2, y + 2, x + backgroundWidth - 2, y + backgroundHeight - 2, 0xffcfc0a5);
        for (var slot : handler.slots) {
            int sx = x + slot.x, sy = y + slot.y;
            context.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xff4c4033);
            context.fill(sx, sy, sx + 16, sy + 16, 0xff93846e);
        }
    }

    @Override protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        super.drawForeground(context, mouseX, mouseY);
        var mode = com.witchersbestiary.entity.ai.CompanionDuty.label(handler.mode());
        context.drawText(textRenderer, mode, backgroundWidth - 8 - textRenderer.getWidth(mode), titleY, 0x40352b, false);
    }

    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        pickup.setMessage(pickupLabel());
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
    }
}
