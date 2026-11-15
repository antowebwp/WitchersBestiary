package com.witchersbestiary.test;

import com.witchersbestiary.client.vfx.VfxManager;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;

/** Localhost-only observer of real, distinct server players and packet recipients. */
final class Rc1NetworkClient {
    private boolean connecting, sampled, saved;
    private int ticks, signs, bombs, emitters, particles, frames, lastActive, firstActive;
    private long previous, sum, maximum;

    void start(String address) {
        WorldRenderEvents.END.register(context -> {
            if (!sampled || saved) return;
            long now = System.nanoTime();
            if (previous != 0) {
                long time = now - previous;
                sum += time;
                maximum = Math.max(maximum, time);
                frames++;
            }
            previous = now;
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!connecting && client.currentScreen instanceof TitleScreen && client.getOverlay() == null) {
                connecting = true;
                client.options.pauseOnLostFocus = false;
                client.options.getViewDistance().setValue(4);
                client.options.getMaxFps().setValue(120);
                ConnectScreen.connect(client.currentScreen, client, ServerAddress.parse(address),
                        new ServerInfo("RC1 localhost", address, ServerInfo.ServerType.OTHER), false, null);
            }
            if (saved && client.world == null) client.scheduleStop();
            if (++ticks > 1800) throw new IllegalStateException("Localhost RC1 client timed out");
            if (client.world == null || client.player == null || saved) return;
            var counts = VfxManager.counts();
            if (counts.signs() + counts.bombs() + counts.cascadeEmitters() > 0) {
                if (!sampled) firstActive = ticks;
                sampled = true;
                lastActive = ticks;
                signs = Math.max(signs, counts.signs());
                bombs = Math.max(bombs, counts.bombs());
                emitters = Math.max(emitters, counts.cascadeEmitters());
                particles = Math.max(particles, counts.cascadeParticles());
            }
            if (!sampled || saved || ticks - lastActive < 4) return;
            if (signs != 2 || bombs != 1
                    || emitters != 4 || particles == 0 || frames == 0)
                throw new IllegalStateException("Unexpected two-client FX counts " + signs + "/" + bombs + "/" + emitters);
            try {
                var name = client.getSession().getUsername();
                Files.writeString(Path.of("../" + name + ".csv"), String.format(Locale.ROOT,
                        "scenario,frames,mean_ms,max_ms,signs,bombs,emitters,particles,ticks_to_clear%n" +
                        "two_igni,%d,%.3f,%.3f,%d,%d,%d,%d,%d%n", frames,
                        sum/1e6/frames, maximum/1e6, signs, bombs, emitters, particles, lastActive-firstActive+1));
                saved = true;
            } catch (java.io.IOException error) {
                throw new java.io.UncheckedIOException(error);
            }
        });
    }
}
