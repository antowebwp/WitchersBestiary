package com.witchersbestiary.client.book;

import com.witchersbestiary.client.signs.SignMasteryScreen;
import com.witchersbestiary.client.signs.SignsClient;
import com.witchersbestiary.signs.SignPlayerState;
import com.witchersbestiary.signs.SignPlayerState.Upgrade;
import com.witchersbestiary.signs.SignType;
import java.util.Locale;
import java.util.function.UnaryOperator;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Text;
import vazkii.patchouli.api.IComponentRenderContext;
import vazkii.patchouli.api.ICustomComponent;
import vazkii.patchouli.api.IVariable;

public final class SignBalanceComponent implements ICustomComponent {
    private static final net.minecraft.util.Identifier ART = net.minecraft.util.Identifier.of(
            "witchersbestiary", "textures/gui/bestiary/sign_abilities.png");
    public String sign = "igni";
    public int form;
    private int x, y;

    @Override public void build(int x, int y, int page) {
        this.x = x;
        this.y = y;
    }

    @Override public void onVariablesAvailable(UnaryOperator<IVariable> lookup, RegistryWrapper.WrapperLookup registries) {
        sign = lookup.apply(IVariable.wrap("#sign")).asString();
        form = lookup.apply(IVariable.wrap("#form")).asNumber().intValue();
    }

    @Override public void render(DrawContext draw, IComponentRenderContext context, float delta, int mouseX, int mouseY) {
        var font = MinecraftClient.getInstance().textRenderer;
        var type = SignType.valueOf(sign.toUpperCase(Locale.ROOT));
        var mastery = SignsClient.mastery;
        int discount = mastery == null ? 0 : 2 * mastery.level(type.ordinal(), Upgrade.COST.ordinal());
        var cost = type == SignType.IGNI && form == 2
                ? Text.translatable("book.combat.concentration_rate", (int)(20 * SignPlayerState.BREATH_COST_PER_TICK))
                : Text.translatable("book.combat.concentration", type.cost + (form == 0 ? 0 : 4) - discount);
        draw.drawTexture(ART, x + 38, y - 3, form * 32, type.ordinal() * 32, 32, 32, 128, 160);
        var lines = Text.empty().append(SignMasteryScreen.formName(type, form)).append("\n\n")
                .append(Text.translatable("sign.mastery.form." + sign + "." + form + ".tip")).append("\n\n")
                .append(cost).append("\n")
                .append(SignMasteryScreen.damageSummary(type, form))
                .append("\n\n").append(Text.translatable("book.combat.visual." + sign + "." + form));
        int row = y + 30;
        for (var line : font.wrapLines(lines.setStyle(context.getFont()), 110)) {
            draw.drawText(font, line, x, row, context.getTextColor(), false);
            row += 10;
        }
    }
}
