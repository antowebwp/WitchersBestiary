package com.witchersbestiary.test;

import com.witchersbestiary.alchemy.WitcherBombItem;
import com.witchersbestiary.registry.ModItems;
import com.witchersbestiary.signs.SignManager;
import com.witchersbestiary.signs.SignType;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.world.GameMode;

public final class ArcaneRc1NetworkServer implements ModInitializer {
    private int together;
    @Override public void onInitialize() {
        if (!Boolean.getBoolean("witchersbestiary.rc1NetworkServer")) return;
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            var one = server.getPlayerManager().getPlayer("Rc1One");
            var two = server.getPlayerManager().getPlayer("Rc1Two");
            if (one == null || two == null) return;
            var world = server.getOverworld();
            if (++together == 5) {
                var spawn = world.getSpawnPos();
                one.changeGameMode(GameMode.CREATIVE);
                two.changeGameMode(GameMode.CREATIVE);
                one.teleport(world, spawn.getX() + .5, spawn.getY(), spawn.getZ(), 0, 4);
                two.teleport(world, spawn.getX() + 2.0, spawn.getY(), spawn.getZ(), 0, 4);
            }
            if (together == 40) {
                for (var player : java.util.List.of(one, two)) {
                    var state = SignManager.state(player);
                    state.learn(SignType.IGNI);
                    state.select(SignType.IGNI);
                    if (!SignManager.cast(player, server.getTicks()))
                        throw new IllegalStateException("Network RC1 cast rejected");
                }
                ((WitcherBombItem)ModItems.BOMBS.getFirst()).detonate(world, one, one.getPos().add(.8,.2,4));
            }
            if (together > 120 && Files.isRegularFile(Path.of("../Rc1One.csv"))
                    && Files.isRegularFile(Path.of("../Rc1Two.csv"))) server.stop(false);
            if (together > 400) throw new IllegalStateException("Two-client FX observation timed out");
        });
    }
}
