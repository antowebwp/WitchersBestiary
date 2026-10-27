package com.witchersbestiary.contracts;

import com.google.gson.*;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import java.util.*;

/** Validated immutable definition, also snapshotted into accepted contracts. */
public record ContractDefinition(Identifier id, String name, String description, Identifier target,
        int required, int difficulty, Reward reward, int minimumLevel, String dimension, long durationMillis) {
    public record ItemReward(Identifier item, int count) {}
    public record Reward(int experience, List<ItemReward> items) {
        public Reward { items = List.copyOf(items); }
    }
    public ContractDefinition {
        Objects.requireNonNull(id); Objects.requireNonNull(name); Objects.requireNonNull(description);
        Objects.requireNonNull(target); Objects.requireNonNull(reward); Objects.requireNonNull(dimension);
        if (!target.getNamespace().equals("witchersbestiary") || !Registries.ENTITY_TYPE.containsId(target)
                || Registries.ENTITY_TYPE.get(target).getSpawnGroup() != net.minecraft.entity.SpawnGroup.MONSTER)
            throw new IllegalArgumentException("Unknown Witcher's Bestiary target: " + target);
        if (required < 1 || required > 10000 || difficulty < 1 || difficulty > 5 || minimumLevel < 0
                || minimumLevel > 1000 || durationMillis < 0 || durationMillis > 31_536_000_000L || reward.experience() < 0 || reward.experience() > 100000)
            throw new IllegalArgumentException("Contract numbers outside supported bounds: " + id);
        if (name.isBlank() || description.isBlank() || reward.items().size() > 32)
            throw new IllegalArgumentException("Invalid display/reward: " + id);
        for (var item : reward.items()) {
            if (item.count() < 1 || item.count() > 2304 || !Registries.ITEM.containsId(item.item())
                    || !(item.item().toString().equals("minecraft:emerald") || item.item().getNamespace().equals("witchersbestiary")))
                throw new IllegalArgumentException("Invalid reward material: " + item.item());
        }
        if (!dimension.isEmpty() && Identifier.tryParse(dimension) == null)
            throw new IllegalArgumentException("Invalid eligibility dimension");
    }
    public static ContractDefinition parse(Identifier id, JsonObject json) {
        if (json.has("id") && !id.toString().equals(json.get("id").getAsString()))
            throw new IllegalArgumentException("Definition id differs from resource id: " + id);
        var reward = json.getAsJsonObject("reward");
        var items = new ArrayList<ItemReward>();
        if (reward.has("emeralds")) {
            int count = integer(reward, "emeralds", 0);
            if (count < 0) throw new IllegalArgumentException("Negative emerald reward");
            if (count > 0) items.add(new ItemReward(Identifier.of("minecraft:emerald"), count));
        }
        if (reward.has("items")) for (var element : reward.getAsJsonArray("items")) {
            var item = element.getAsJsonObject();
            items.add(new ItemReward(Identifier.of(item.get("item").getAsString()), integer(item, "count", 0)));
        }
        var eligibility = json.has("eligibility") ? json.getAsJsonObject("eligibility") : new JsonObject();
        return new ContractDefinition(id, json.get("name").getAsString(), json.get("description").getAsString(),
                Identifier.of(json.get("target").getAsString()), integer(json, "required", 0), integer(json, "difficulty", 0),
                new Reward(integer(reward, "experience", 0), items), integer(eligibility, "minimum_level", 0),
                eligibility.has("dimension") ? eligibility.get("dimension").getAsString() : "",
                json.has("duration_millis") ? json.get("duration_millis").getAsBigDecimal().longValueExact() : 0);
    }
    private static int integer(JsonObject o, String key, int fallback) { try { return o.has(key) ? o.get(key).getAsBigDecimal().intValueExact() : fallback; } catch (ArithmeticException e) { throw new IllegalArgumentException("Expected bounded integer: " + key, e); } }
    public JsonObject toJson() {
        var j = new JsonObject(); j.addProperty("id", id.toString()); j.addProperty("name", name);
        j.addProperty("description", description); j.addProperty("target", target.toString());
        j.addProperty("required", required); j.addProperty("difficulty", difficulty); j.addProperty("duration_millis", durationMillis);
        var r = new JsonObject(); r.addProperty("experience", reward.experience()); var a = new JsonArray();
        for (var item : reward.items()) { var i = new JsonObject(); i.addProperty("item", item.item().toString()); i.addProperty("count", item.count()); a.add(i); }
        r.add("items", a); j.add("reward", r); var e = new JsonObject(); e.addProperty("minimum_level", minimumLevel);
        if (!dimension.isEmpty()) e.addProperty("dimension", dimension); j.add("eligibility", e); return j;
    }
}
