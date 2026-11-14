package com.witchersbestiary.test;

import com.witchersbestiary.signs.*;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.*;

public final class SignFormsTests implements FabricGameTest {
    private static final String ARENA="witchersbestiary:village_board_arena";
    private final SignTests fixtures=new SignTests();

    ServerPlayerEntity equipped(TestContext c,SignType sign,int form) {
        var p=player(c);var state=SignManager.state(p);state.learn(sign);state.award(100);
        for(var node:SignPlayerState.Upgrade.values())state.upgrade(sign,node);
        c.assertTrue(state.unlock(sign,form)&&state.equip(sign,1,form)&&state.select(sign),"Alternate unlocked and equipped");
        return p;
    }

    private static ServerPlayerEntity player(TestContext context) {
        // The embedded login fixture cannot negotiate Cardinal Components payloads.
        var player = context.createMockCreativeServerPlayerInWorld();
        player.changeGameMode(net.minecraft.world.GameMode.SURVIVAL);
        player.setNoGravity(true);
        var position = context.getAbsolutePos(new BlockPos(6, 2, 6));
        player.refreshPositionAndAngles(position.getX() + .5, position.getY(), position.getZ() + .5, 0, 0);
        return player;
    }

    @GameTest(templateName=ARENA)
    public void igniFormsAndAardForces(TestContext c) {
        for(int form:new int[]{1,3}){
            var p=equipped(c,SignType.IGNI,form);var foe=fixtures.target(c,p);
            if(form==1)foe.setPosition(p.getX(),p.getY(),p.getZ()-3);
            c.assertTrue(SignManager.cast(p,1,1)&&foe.isOnFire()&&foe.getHealth()<20,"Igni form damages and burns: "+form);
            foe.discard();p.discard();
        }
        for(int form=2;form<=3;form++){
            var p=equipped(c,SignType.AARD,form);var foe=fixtures.target(c,p);
            c.assertTrue(SignManager.cast(p,1,1)&&foe.getHealth()<20,"Aard alternative hits");
            c.assertTrue(form==2?foe.getVelocity().z>.8:foe.getVelocity().z<0,"Thrust pushes and vortex pulls");
            foe.discard();p.discard();
        }
        c.complete();
    }

    @GameTest(templateName=ARENA)
    public void quenBurstReflectionAndSharedAura(TestContext c) {
        for(int form=1;form<=2;form++){
            var p=equipped(c,SignType.QUEN,form);var foe=fixtures.target(c,p);
            c.assertTrue(SignManager.cast(p,1,1),"Quen alternative casts");
            float hit=form==1?25:6;
            c.assertTrue(SignManager.protect(p,c.getWorld().getDamageSources().mobAttack(foe),hit)<hit&&foe.getHealth()<20,"Quen burst or reflection damages hostile attacker");
            foe.discard();p.discard();
        }
        var p=equipped(c,SignType.QUEN,3);var ally=player(c);
        var scoreboard=c.getWorld().getScoreboard();var team=scoreboard.addTeam("wb_aura_test");
        scoreboard.addScoreHolderToTeam(p.getNameForScoreboard(),team);scoreboard.addScoreHolderToTeam(ally.getNameForScoreboard(),team);
        ally.setPosition(p.getX()+2,p.getY(),p.getZ());
        c.assertTrue(SignManager.cast(p,1,1)&&SignManager.protect(ally,c.getWorld().getDamageSources().fall(),5)==0,"Aura shares finite shield with teammate");
        scoreboard.removeTeam(team);p.discard();ally.discard();c.complete();
    }

    @GameTest(templateName=ARENA)
    public void axiiCrowdControlAndRedirect(TestContext c) {
        for(int form=1;form<=3;form++){
            var p=equipped(c,SignType.AXII,form);var first=fixtures.target(c,p);var second=fixtures.target(c,p);
            second.setPosition(p.getX(),p.getY(),p.getZ()+4);
            for(var mob:java.util.List.of(first,second)){
                mob.setAiDisabled(false);mob.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(0);
            }
            c.assertTrue(SignManager.cast(p,1,1),"Axii form casts");
            if(form==1)c.assertTrue(SignManager.calmed(first)&&SignManager.calmed(second),"Mass confusion calms both");
            else c.assertTrue(first.getTarget()==second&&!SignManager.permitsRedirectedAttack(first,p)&&SignManager.permitsRedirectedAttack(first,second),"Redirected attacker can hit only designated foe");
            first.discard();second.discard();p.discard();
        }
        c.complete();
    }

}
