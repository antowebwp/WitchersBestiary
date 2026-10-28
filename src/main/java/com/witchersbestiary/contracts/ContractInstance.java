package com.witchersbestiary.contracts;

import net.minecraft.nbt.*;
import java.util.*;

/** State can only change through validated transitions. The kill ledger survives reloads. */
public final class ContractInstance {
    public enum Status { AVAILABLE, ACCEPTED, COMPLETED, REWARDED, CANCELLED, EXPIRED }
    public com.witchersbestiary.contracts.hunt.SwampHunt hunt;
    public com.witchersbestiary.contracts.procedural.ProceduralProfile procedural;
    private final UUID id, owner;
    private final ContractDefinition definition;
    private final long created, expires;
    private Status status = Status.AVAILABLE;
    private final Set<UUID> kills = new HashSet<>();
    public ContractInstance(UUID id, UUID owner, ContractDefinition definition, long created) {
        this.id = Objects.requireNonNull(id); this.owner = Objects.requireNonNull(owner);
        this.definition = Objects.requireNonNull(definition); this.created = created;
        this.expires = definition.durationMillis() == 0 ? 0 : Math.addExact(created, definition.durationMillis());
    }
    public UUID id() { return id; }
    public UUID owner() { return owner; }
    public ContractDefinition definition() { return definition; }
    public Status status() { return status; }
    public int progress() { return kills.size(); }
    public long created() { return created; }
    public long expires() { return expires; }
    public boolean expire(long now) {
        if (expires != 0 && now >= expires && (status == Status.AVAILABLE || status == Status.ACCEPTED)) { status = Status.EXPIRED; return true; }
        return false;
    }
    public void transition(Status next) {
        boolean valid = switch (status) {
            case AVAILABLE -> next == Status.ACCEPTED || next == Status.CANCELLED || next == Status.EXPIRED;
            case ACCEPTED -> next == Status.CANCELLED || next == Status.EXPIRED || (next == Status.COMPLETED && progress() == definition.required());
            case COMPLETED -> next == Status.REWARDED;
            default -> false;
        };
        if (!valid) throw new IllegalStateException("Invalid contract transition " + status + " -> " + next);
        status = next;
    }
    public boolean credit(UUID death, UUID player, net.minecraft.util.Identifier target, long now) {
        expire(now);
        if (!owner.equals(player) || status != Status.ACCEPTED || !definition.target().equals(target) || !kills.add(death)) return false;
        if (progress() == definition.required()) transition(Status.COMPLETED);
        return true;
    }
    public NbtCompound toNbt() {
        var n = new NbtCompound(); n.putUuid("Id", id); n.putUuid("Owner", owner);
        n.putString("Definition", definition.toJson().toString()); n.putString("Status", status.name()); n.putLong("Created", created);
        var list = new NbtList(); kills.stream().sorted().forEach(k -> list.add(NbtString.of(k.toString()))); n.put("Kills", list); if(hunt!=null)n.put("Hunt",hunt.write());if(procedural!=null)n.put("Procedural",procedural.write()); return n;
    }
    public static ContractInstance fromNbt(NbtCompound n) {
        var j = com.google.gson.JsonParser.parseString(n.getString("Definition")).getAsJsonObject();
        var d = ContractDefinition.parse(net.minecraft.util.Identifier.of(j.get("id").getAsString()), j);
        var c = new ContractInstance(n.getUuid("Id"), n.getUuid("Owner"), d, n.getLong("Created"));
        var list = n.getList("Kills", NbtElement.STRING_TYPE);
        for (int i = 0; i < list.size(); i++) if (!c.kills.add(UUID.fromString(list.getString(i)))) throw new IllegalArgumentException("Duplicate persisted kill");
        c.status = Status.valueOf(n.getString("Status"));
        if (c.status == Status.AVAILABLE && c.progress() != 0) throw new IllegalArgumentException("Available contract has credited kills");
        if (c.progress() > d.required() || ((c.status == Status.COMPLETED || c.status == Status.REWARDED) && c.progress() != d.required())
                || ((c.status == Status.AVAILABLE || c.status == Status.ACCEPTED) && c.progress() == d.required()))
            throw new IllegalArgumentException("Inconsistent contract progress/state");
        if(n.contains("Hunt"))c.hunt=com.witchersbestiary.contracts.hunt.SwampHunt.read(n.getCompound("Hunt"));
        if(n.contains("Procedural")){c.procedural=com.witchersbestiary.contracts.procedural.ProceduralProfile.read(n.getCompound("Procedural"));if(!com.witchersbestiary.contracts.procedural.ProceduralHunts.isDefinition(c.definition.id())||!c.definition.target().getPath().equals(c.procedural.species()))throw new IllegalArgumentException("Procedural species mismatch");}
        if(com.witchersbestiary.contracts.procedural.ProceduralHunts.isDefinition(d.id())&&c.procedural==null)throw new IllegalArgumentException("Missing saved procedural profile; refusing reroll");
        return c;
    }
}
