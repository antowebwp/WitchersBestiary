package com.witchersbestiary.test;

import com.google.gson.JsonParser;
import com.witchersbestiary.contracts.*;
import com.witchersbestiary.entity.ModEntities;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.entity.EntityType;
import net.minecraft.item.*;
import net.minecraft.nbt.*;
import net.minecraft.test.*;
import net.minecraft.util.Identifier;
import java.util.*;

public final class ContractFoundationTests implements FabricGameTest {
    private ContractDefinition definition() { return Contracts.DEFINITIONS.find(Identifier.of("witchersbestiary:drowner_cleanup")); }
    private ContractInstance instance(UUID owner) { return new ContractInstance(UUID.randomUUID(), owner, definition(), 1000); }
    private void rejects(TestContext c, Runnable action) {
        boolean rejected = false; try { action.run(); } catch (IllegalArgumentException | IllegalStateException e) { rejected = true; }
        c.assertTrue(rejected, "Invalid operation must be rejected");
    }
    private void finish(ContractInstance i) { i.transition(ContractInstance.Status.ACCEPTED); for(int k=0;k<i.definition().required();k++) i.credit(UUID.randomUUID(),i.owner(),i.definition().target(),1001); }

    @GameTest(templateName=FabricGameTest.EMPTY_STRUCTURE, batchId="stage20a", tickLimit=20)
    public void lifecycleAndDuplicateCompletion(TestContext c) {
        var i=instance(UUID.randomUUID()); rejects(c,()->i.transition(ContractInstance.Status.REWARDED));
        i.transition(ContractInstance.Status.ACCEPTED); rejects(c,()->i.transition(ContractInstance.Status.COMPLETED));
        for(int n=0;n<3;n++) c.assertTrue(i.credit(UUID.randomUUID(),i.owner(),i.definition().target(),1001),"Valid kill accepted");
        c.assertTrue(i.status()==ContractInstance.Status.COMPLETED,"Completed at exact count");
        c.assertTrue(!i.credit(UUID.randomUUID(),i.owner(),i.definition().target(),1001),"No progress after completion");
        i.transition(ContractInstance.Status.REWARDED); rejects(c,()->i.transition(ContractInstance.Status.REWARDED)); rejects(c,()->i.transition(ContractInstance.Status.CANCELLED));
        var cancelled=instance(i.owner());cancelled.transition(ContractInstance.Status.CANCELLED);rejects(c,()->cancelled.transition(ContractInstance.Status.ACCEPTED));c.complete();
    }
    @GameTest(templateName=FabricGameTest.EMPTY_STRUCTURE, batchId="stage20a", tickLimit=20)
    public void serializationOwnershipAndKillLedger(TestContext c) {
        var i=instance(UUID.randomUUID());i.transition(ContractInstance.Status.ACCEPTED);var death=UUID.randomUUID();
        c.assertTrue(!i.credit(death,UUID.randomUUID(),i.definition().target(),1001),"Other player ignored");
        c.assertTrue(!i.credit(death,i.owner(),Identifier.of("witchersbestiary:ghoul"),1001),"Other type ignored");
        c.assertTrue(i.credit(death,i.owner(),i.definition().target(),1001),"First kill");
        var store=new ContractState();store.add(i);var loaded=ContractState.read(store.writeNbt(new NbtCompound(),c.getWorld().getRegistryManager()),c.getWorld().getRegistryManager());
        var restored=loaded.find(i.id());c.assertTrue(loaded.ownedBy(i.owner()).size()==1&&loaded.ownedBy(UUID.randomUUID()).isEmpty(),"Owner index restored");
        c.assertTrue(restored.owner().equals(i.owner())&&restored.progress()==1&&restored.created()==1000,"Identity/progress/time roundtrip");
        c.assertTrue(!restored.credit(death,restored.owner(),restored.definition().target(),1002),"Duplicate rejected after reload");c.complete();
    }
    @GameTest(templateName=FabricGameTest.EMPTY_STRUCTURE, batchId="stage20a", tickLimit=20)
    public void diskPersistenceAndClaimedState(TestContext c) {
        try {
            var i=instance(UUID.randomUUID());finish(i);i.transition(ContractInstance.Status.REWARDED);var state=new ContractState();state.add(i);
            var path=java.nio.file.Files.createTempFile(java.nio.file.Path.of("."),"contract-state-",".nbt");
            NbtIo.writeCompressed(state.writeNbt(new NbtCompound(),c.getWorld().getRegistryManager()),path);
            var loaded=ContractState.read(NbtIo.readCompressed(path,NbtSizeTracker.ofUnlimitedBytes()),c.getWorld().getRegistryManager());
            c.assertTrue(loaded.find(i.id()).status()==ContractInstance.Status.REWARDED,"Disk preserves claimed state");
            rejects(c,()->loaded.find(i.id()).transition(ContractInstance.Status.REWARDED));java.nio.file.Files.delete(path);c.complete();
        } catch(java.io.IOException e) {throw new RuntimeException(e);}
    }
    @GameTest(templateName=FabricGameTest.EMPTY_STRUCTURE, batchId="stage20a", tickLimit=40)
    public void realDeathAttributionAndIsolation(TestContext c) {
        var p=c.createMockCreativeServerPlayerInWorld();var other=c.createMockCreativeServerPlayerInWorld();
        c.assertTrue(!p.getUuid().equals(other.getUuid()),"Separate owners");var a=Contracts.assign(p,definition().id());var b=Contracts.assign(other,definition().id());
        var victim=c.spawnEntity(ModEntities.DROWNER,1,2,1);victim.setAiDisabled(true);victim.setNoGravity(true);
        var direct=c.getWorld().getDamageSources().playerAttack(p);c.assertTrue(Contracts.killer(victim,direct)==p,"Direct owner");
        victim.damage(direct,100000);c.assertTrue(!victim.isAlive()&&a.progress()==1&&b.progress()==0,"Actual death callback credits only killer");
        Contracts.onDeath(victim,direct);c.assertTrue(a.progress()==1,"Duplicate callback ignored");
        var arrow=EntityType.ARROW.create(c.getWorld());arrow.setOwner(p);
        var second=c.spawnEntity(ModEntities.DROWNER,2,2,1);second.setAiDisabled(true);second.setNoGravity(true);var indirect=c.getWorld().getDamageSources().arrow(arrow,p);
        c.assertTrue(Contracts.killer(second,indirect)==p,"Projectile owner");second.damage(indirect,100000);c.assertTrue(a.progress()==2,"Projectile death counts");
        var environment=c.spawnEntity(ModEntities.DROWNER,3,2,1);environment.setAiDisabled(true);environment.setNoGravity(true);environment.damage(c.getWorld().getDamageSources().genericKill(),100000);
        c.assertTrue(a.progress()==2&&b.progress()==0,"Environmental death ignored");
        var unrelated=c.spawnEntity(EntityType.COW,4,2,1);unrelated.damage(direct,100000);c.assertTrue(a.progress()==2,"Unrelated type ignored");
        p.discard();other.discard();arrow.discard();c.complete();
    }
    @GameTest(templateName=FabricGameTest.EMPTY_STRUCTURE, batchId="stage20a", tickLimit=20)
    public void rewardOnceAndInventoryCapacity(TestContext c) {
        var p=c.createMockCreativeServerPlayerInWorld();p.getInventory().clear();var i=Contracts.assign(p,definition().id());
        c.assertTrue(!p.getServer().getCommandManager().getDispatcher().getRoot().getChild("wbcontracts").canUse(p.getCommandSource().withLevel(0)),"Non-operator cannot use developer commands");
        var stranger=c.createMockCreativeServerPlayerInWorld();rejects(c,()->Contracts.claim(stranger,i.id()));rejects(c,()->Contracts.cancel(stranger,i.id()));stranger.discard();
        rejects(c,()->Contracts.claim(p,i.id()));for(int n=0;n<3;n++)i.credit(UUID.randomUUID(),p.getUuid(),definition().target(),System.currentTimeMillis());
        for(int n=0;n<36;n++)p.getInventory().main.set(n,new ItemStack(Items.STONE,64));
        rejects(c,()->Contracts.claim(p,i.id()));c.assertTrue(i.status()==ContractInstance.Status.COMPLETED,"Full inventory leaves claim pending");
        p.getInventory().clear();int xp=p.totalExperience;Contracts.claim(p,i.id());c.assertTrue(p.totalExperience==xp+30&&p.getInventory().count(Items.EMERALD)==2,"XP and emerald reward");
        c.assertTrue(p.getInventory().count(com.witchersbestiary.registry.ModItems.DROWNER_ESSENCE)==1,"WB material reward");
        rejects(c,()->Contracts.claim(p,i.id()));c.assertTrue(p.totalExperience==xp+30&&p.getInventory().count(Items.EMERALD)==2,"No repeated delivery");
        var saved=ContractState.get(p.getServer()).writeNbt(new NbtCompound(),c.getWorld().getRegistryManager());var loaded=ContractState.read(saved,c.getWorld().getRegistryManager());
        rejects(c,()->ContractRewards.claim(loaded,loaded.find(i.id()),p));p.discard();c.complete();
    }
    @GameTest(templateName=FabricGameTest.EMPTY_STRUCTURE, batchId="stage20a", tickLimit=20)
    public void invalidDefinitionsAndSchemaPreservation(TestContext c) {
        var json=definition().toJson();json.addProperty("required",0);rejects(c,()->ContractDefinition.parse(definition().id(),json));
        json.addProperty("required",1.5);rejects(c,()->ContractDefinition.parse(definition().id(),json));
        json.addProperty("required",3);json.addProperty("target","witchersbestiary:missing_monster");rejects(c,()->ContractDefinition.parse(definition().id(),json));
        json.addProperty("target","witchersbestiary:leshen_wolf");rejects(c,()->ContractDefinition.parse(definition().id(),json));
        json.addProperty("target","witchersbestiary:drowner");json.addProperty("duration_millis",Long.MAX_VALUE);rejects(c,()->ContractDefinition.parse(definition().id(),json));
        var p=c.createMockCreativeServerPlayerInWorld();rejects(c,()->Contracts.assign(p,Identifier.of("witchersbestiary:missing")));p.discard();
        var future=new NbtCompound();future.putInt("Schema",999);future.putString("Unrecognized","keep");var loaded=ContractState.read(future,c.getWorld().getRegistryManager());
        rejects(c,()->loaded.add(instance(UUID.randomUUID())));c.assertTrue(loaded.writeNbt(new NbtCompound(),c.getWorld().getRegistryManager()).equals(future),"Future schema retained");
        var damaged=new NbtCompound();damaged.putInt("Schema",1);var entries=new NbtList();var bad=new NbtCompound();bad.putString("Definition","invalid");entries.add(bad);damaged.put("Contracts",entries);
        var quarantine=ContractState.read(damaged,c.getWorld().getRegistryManager());c.assertTrue(quarantine.writeNbt(new NbtCompound(),c.getWorld().getRegistryManager()).getList("Contracts",10).size()==1,"Unreadable record preserved");c.complete();
    }
    @GameTest(templateName=FabricGameTest.EMPTY_STRUCTURE, batchId="stage20a", tickLimit=20)
    public void missingDefinitionPausesWithoutLosingSnapshot(TestContext c) {
        var p=c.createMockCreativeServerPlayerInWorld();var i=Contracts.assign(p,definition().id());
        var manager=c.getWorld().getServer().getResourceManager();
        var empty=(net.minecraft.resource.ResourceManager)java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{net.minecraft.resource.ResourceManager.class},(proxy,method,args)->method.getName().equals("findResources")?Map.of():method.invoke(manager,args));
        try {
            Contracts.DEFINITIONS.reload(empty);rejects(c,()->Contracts.claim(p,i.id()));
            var victim=c.spawnEntity(ModEntities.DROWNER,1,2,1);victim.setAiDisabled(true);victim.damage(c.getWorld().getDamageSources().playerAttack(p),100000);
            c.assertTrue(i.progress()==0&&i.status()==ContractInstance.Status.ACCEPTED,"Removed definition pauses active record");
            var saved=ContractState.get(p.getServer()).writeNbt(new NbtCompound(),c.getWorld().getRegistryManager());var restored=ContractState.read(saved,c.getWorld().getRegistryManager());
            c.assertTrue(restored.find(i.id()).definition().id().equals(i.definition().id()),"Snapshot persists while registry entry is missing");
        } finally {Contracts.DEFINITIONS.reload(manager);p.discard();}
        c.assertTrue(Contracts.DEFINITIONS.find(i.definition().id())!=null,"Definition restored on next reload");c.complete();
    }
    @GameTest(templateName=FabricGameTest.EMPTY_STRUCTURE, batchId="stage20a", tickLimit=20)
    public void reloadSnapshotMissingAndExpiration(TestContext c) {
        var i=instance(UUID.randomUUID());var snapshot=i.definition();Contracts.DEFINITIONS.reload(c.getWorld().getServer().getResourceManager());
        c.assertTrue(Contracts.DEFINITIONS.all().size()==5&&i.definition()==snapshot,"Reload publishes definitions without mutating instances");
        var json=definition().toJson();json.addProperty("duration_millis",10);var expiring=new ContractInstance(UUID.randomUUID(),i.owner(),ContractDefinition.parse(definition().id(),json),1000);
        expiring.transition(ContractInstance.Status.ACCEPTED);c.assertTrue(expiring.expire(1010)&&expiring.status()==ContractInstance.Status.EXPIRED,"Deadline inclusive");
        c.assertTrue(!expiring.credit(UUID.randomUUID(),expiring.owner(),expiring.definition().target(),1011),"Expired blocks kills");
        rejects(c,()->expiring.transition(ContractInstance.Status.ACCEPTED));c.complete();
    }
}
