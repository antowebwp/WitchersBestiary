package com.witchersbestiary.contracts;

import com.google.gson.JsonParser;
import com.witchersbestiary.WitchersBestiary;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import java.util.*;

/** Datapack reload publishes a complete immutable definition map. Invalid entries are excluded. */
public final class ContractDefinitions implements SimpleSynchronousResourceReloadListener {
    private volatile Map<Identifier, ContractDefinition> definitions = Map.of();
    public Map<Identifier, ContractDefinition> all() { return definitions; }
    public ContractDefinition find(Identifier id) { return definitions.get(id); }
    @Override public Identifier getFabricId() { return Identifier.of("witchersbestiary:contracts"); }
    @Override public void reload(ResourceManager manager) {
        var next = new HashMap<Identifier, ContractDefinition>();
        for (var entry : manager.findResources("contracts", id -> id.getPath().endsWith(".json")).entrySet()) {
            var resource = entry.getKey();
            var id = Identifier.of(resource.getNamespace(), resource.getPath().substring("contracts/".length(), resource.getPath().length() - 5));
            try (var reader = entry.getValue().getReader()) { next.put(id, ContractDefinition.parse(id, JsonParser.parseReader(reader).getAsJsonObject())); }
            catch (Exception e) { WitchersBestiary.LOGGER.error("Invalid contract definition {} from {}: {}", id, entry.getValue().getPackId(), e.getMessage()); }
        }
        definitions = Map.copyOf(next);
        WitchersBestiary.LOGGER.info("Loaded {} Witcher contract definitions", next.size());
    }
}
