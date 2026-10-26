package com.witchersbestiary.compat;

import com.witchersbestiary.WitchersBestiary;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/** Data-only bridge: no SS classes or components may enter the core item registry. */
public final class SimplySwordsCompat {
    private SimplySwordsCompat() {}

    public static void initialize() {
        var loader = FabricLoader.getInstance();
        if (!loader.isModLoaded("simplyswords") || !loader.isModLoaded("bettercombat")) return;
        String version = loader.getModContainer("simplyswords").orElseThrow().getMetadata().getVersion().getFriendlyString();
        if (!version.equals("1.63.0-1.21.1")) {
            WitchersBestiary.LOGGER.warn("Simply Swords {} is unverified; keeping the base hammer profile", version);
            return;
        }
        boolean registered = ResourceManagerHelper.registerBuiltinResourcePack(
                Identifier.of(WitchersBestiary.MOD_ID, "simplyswords_163"),
                loader.getModContainer(WitchersBestiary.MOD_ID).orElseThrow(),
                Text.literal("Witcher's Bestiary / Simply Swords 1.63"), ResourcePackActivationType.ALWAYS_ENABLED);
        if (!registered) throw new IllegalStateException("Missing bundled Simply Swords hammer profile");
        WitchersBestiary.LOGGER.info("Optional Simply Swords 1.63 greathammer profile enabled; stable WB items/components");
    }
}
