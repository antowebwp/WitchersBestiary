package com.witchersbestiary.test;

import com.witchersbestiary.alchemy.WitcherBombItem;
import com.witchersbestiary.client.signs.SignsClient;
import com.witchersbestiary.client.vfx.VfxManager;
import com.witchersbestiary.registry.ModItems;
import com.witchersbestiary.signs.SignManager;
import com.witchersbestiary.signs.SignType;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import net.minecraft.client.MinecraftClient;

final class Rc1Stress {
    private enum Scenario { BASELINE, BREATH, THREE_GRAPESHOT, MIXED, GAS }
    private static final Scenario[] SCENARIOS = Scenario.values();
    private final StringBuilder report = new StringBuilder(
            "Scenario,frames,mean_ms,max_ms,sign_instances,bomb_instances,cascade_emitters,cascade_particles,ticks_to_clear\n");
    private int index, age = -1, frames, signs, bombs, emitters, particles;
    private long previous, sum, maximum;
    private boolean seen;

    boolean tick(MinecraftClient client) throws IOException {
        if (index == SCENARIOS.length) {
            Files.writeString(Path.of("../performance.csv"), report);
            return true;
        }
        var scenario = SCENARIOS[index];
        age++;
        if (age == 0) {
            begin(client, scenario);
            return false;
        }
        if (scenario == Scenario.BREATH) {
            if (age == 5) SignsClient.send(10, SignType.IGNI.ordinal());
            if (age == 11 || age == 17 || age == 23) SignsClient.send(8, SignType.IGNI.ordinal());
            if (age == 27) SignsClient.send(7, SignType.IGNI.ordinal());
        }
        var count = VfxManager.counts();
        signs = Math.max(signs, count.signs());
        bombs = Math.max(bombs, count.bombs());
        emitters = Math.max(emitters, count.cascadeEmitters());
        particles = Math.max(particles, count.cascadeParticles());
        boolean active = count.signs() + count.bombs() + count.cascadeEmitters() > 0;
        seen |= active;
        int settle = switch (scenario) {
            case BASELINE -> 30;
            case BREATH -> 29;
            case THREE_GRAPESHOT, MIXED, GAS -> 8;
        };
        if (age >= settle && !active && (seen || scenario == Scenario.BASELINE)) {
            report.append(String.format(Locale.ROOT,"%s,%d,%.3f,%.3f,%d,%d,%d,%d,%d%n", scenario, frames,
                    sum/1e6/Math.max(1,frames), maximum/1e6, signs, bombs, emitters, particles,
                    scenario == Scenario.BREATH ? age-27 : scenario == Scenario.BASELINE ? 0 : age));
            next();
        }
        if (age > 185) throw new IllegalStateException("FX did not run and clean up: " + scenario);
        return false;
    }

    void frame() {
        if (index >= SCENARIOS.length || age < 3) return;
        var scenario = SCENARIOS[index];
        if (age > (scenario == Scenario.GAS ? 70 : 28)) return;
        long now = System.nanoTime();
        if (previous != 0) {
            long elapsed = now-previous;
            sum += elapsed;
            maximum = Math.max(maximum,elapsed);
            frames++;
        }
        previous = now;
    }

    private void begin(MinecraftClient client, Scenario scenario) {
        client.player.setYaw(0);
        client.player.setPitch(5);
        client.getServer().execute(() -> {
            var server = client.getServer();
            var player = server.getPlayerManager().getPlayerList().getFirst();
            var world = player.getServerWorld();
            if (scenario == Scenario.BASELINE) server.getTickManager().setTickRate(20);
            if (scenario == Scenario.BREATH) {
                SignManager.state(player).select(SignType.IGNI);
                SignManager.sync(player);
            }
            int n = switch (scenario) {
                case THREE_GRAPESHOT, GAS -> 3;
                case MIXED -> 5;
                default -> 0;
            };
            for (int i = 0; i < n; i++) {
                int item = scenario == Scenario.GAS ? 3 : scenario == Scenario.MIXED ? i % 4 : 0;
                var point = player.getPos().add((i - 1.5) * 1.25, .10, 3.6 + i*.32);
                ((WitcherBombItem)ModItems.BOMBS.get(item)).detonate(world, player, point);
            }
        });
    }

    private void next() {
        index++;
        age = -1;
        frames = signs = bombs = emitters = particles = 0;
        previous = sum = maximum = 0;
        seen = false;
    }
}
