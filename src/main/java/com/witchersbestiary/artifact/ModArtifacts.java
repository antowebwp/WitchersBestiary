package com.witchersbestiary.artifact;
import net.minecraft.item.Item;
import net.minecraft.registry.*;
import net.minecraft.util.Identifier;
import java.util.List;
public final class ModArtifacts {
 private ModArtifacts(){}
 private static Item item(String id){return Registry.register(Registries.ITEM,Identifier.of("witchersbestiary",id),new ArtifactItem(id));}
 public static final Item LESHEN_RING=item("leshen_ring"),WEREWOLF_RING=item("werewolf_ring"),DROWNER_CHARM=item("drowner_charm"),GRIFFIN_CHARM=item("griffin_charm");
 public static final List<Item> ALL=List.of(LESHEN_RING,WEREWOLF_RING,DROWNER_CHARM,GRIFFIN_CHARM);
 public static void initialize(){}
}
