package com.witchersbestiary.client.book;

import com.witchersbestiary.combat.StageCombatBalance;
import java.text.DecimalFormat;
import java.util.function.UnaryOperator;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Text;
import vazkii.patchouli.api.IComponentRenderContext;
import vazkii.patchouli.api.ICustomComponent;
import vazkii.patchouli.api.IVariable;

public final class WeaponBalanceComponent implements ICustomComponent {
    private int x, y, base;
    private double percent;
    private String id;

    @Override public void build(int x, int y, int page) {
        this.x = x;
        this.y = y;
    }

    @Override public void onVariablesAvailable(UnaryOperator<IVariable> lookup, RegistryWrapper.WrapperLookup registries) {
        id = lookup.apply(IVariable.wrap("#id")).asString();
        var profile = lookup.apply(IVariable.wrap("#profile")).asString();
        if (lookup.apply(IVariable.wrap("#bomb")).asBoolean()) {
            var bomb = StageCombatBalance.Bomb.valueOf(profile);
            base = bomb.base;
            percent = bomb.percent;
        } else {
            var sword = StageCombatBalance.Sword.valueOf(profile);
            base = sword.base;
            percent = sword.percent;
        }
    }

    @Override public void render(DrawContext draw, IComponentRenderContext context, float delta, int mouseX, int mouseY) {
        var font = MinecraftClient.getInstance().textRenderer;
        var text = Text.empty().append(Text.translatable("item.witchersbestiary." + id)).append("\n\n")
                .append(Text.translatable("book.combat.hybrid", base, new DecimalFormat("0.##").format(percent) + "%"))
                .append("\n\n").append(Text.translatable("book.combat." + id));
        int row = y;
        for (var line : font.wrapLines(text.setStyle(context.getFont()), 110)) {
            draw.drawText(font, line, x, row, context.getTextColor(), false);
            row += 10;
        }
    }
}
