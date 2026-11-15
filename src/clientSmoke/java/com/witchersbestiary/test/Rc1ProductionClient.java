package com.witchersbestiary.test;

import net.fabricmc.api.ClientModInitializer;

/** Entrypoint of the isolated remapped review harness. */
public final class Rc1ProductionClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        new ArcaneV2Review().start();
    }
}
