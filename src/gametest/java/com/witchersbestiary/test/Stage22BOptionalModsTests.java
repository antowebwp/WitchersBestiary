package com.witchersbestiary.test;

import com.witchersbestiary.sorcery.SpellPowerBridge;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.EntityType;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Identifier;

public final class Stage22BOptionalModsTests implements FabricGameTest {
    @GameTest(templateName=FabricGameTest.EMPTY_STRUCTURE)
    public void dedicatedOptionalStackAndFrostApi(TestContext c) throws java.io.IOException {
        var loader=FabricLoader.getInstance();
        var expected=java.util.List.of("bettercombat","simplyswords","spell_engine","spell_power");
        var log=org.slf4j.LoggerFactory.getLogger("witchersbestiary");
        if (!loader.isModLoaded("simplyswords")) {
            log.info("Optional stack scenario skipped because Simply Swords is absent");c.complete();return;
        }
        for(String mod:expected){
            var container=loader.getModContainer(mod).orElseThrow();
            log.info("Stage 22B optional runtime: {} {}",mod,container.getMetadata().getVersion().getFriendlyString());
        }
        var attributes=c.getWorld().getServer().getResourceManager()
            .getResource(Identifier.of("witchersbestiary","weapon_attributes/bonecrusher_club.json")).orElseThrow();
        try(var reader=attributes.getReader()){
            c.assertTrue(com.google.gson.JsonParser.parseReader(reader).getAsJsonObject().get("parent").getAsString()
                .equals("simplyswords:iron_greathammer"),"Active Simply Swords profile selects the Better Combat greathammer");
        }
        var caster=EntityType.ZOMBIE.create(c.getWorld());
        var fallback=c.getWorld().getDamageSources().magic();
        c.assertTrue(SpellPowerBridge.available()&&SpellPowerBridge.source(caster,fallback)!=fallback&&SpellPowerBridge.amount(caster)>=6,"Spell Power frost source works with Spell Engine loaded");
        caster.discard();c.complete();
    }
}
