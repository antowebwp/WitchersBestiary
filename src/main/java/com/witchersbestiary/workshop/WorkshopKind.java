package com.witchersbestiary.workshop;
public enum WorkshopKind {
 ALCHEMY_SHELF("alchemy_shelf",Mount.WALL,0), HANGING_HERBS("hanging_herbs",Mount.CEILING,0),
 ALCHEMY_WORKBENCH("alchemy_workbench",Mount.FLOOR,4), RITUAL_CANDLES("ritual_candles",Mount.FLOOR,0),
 WALL_SWORD_RACK("wall_sword_rack",Mount.WALL,2), WEAPON_STAND("weapon_stand",Mount.FLOOR,3),
 WITCHER_EQUIPMENT_RACK("witcher_equipment_rack",Mount.FLOOR,8), WITCHER_CHEST("witcher_chest",Mount.FLOOR,45),
 SKULL_CANDLES("skull_candles",Mount.FLOOR,0), RITUAL_URN("ritual_urn",Mount.FLOOR,6),
 ANCIENT_GRAVESTONE("ancient_gravestone",Mount.FLOOR,0), GRAVEYARD_FENCE("graveyard_fence",Mount.FLOOR,0),
 GHOST_LANTERN("ghost_lantern",Mount.BOTH,0), HUNTING_TABLE("hunting_table",Mount.FLOOR,0), HUNTERS_TOOL_RACK("hunters_tool_rack",Mount.WALL,0);
 public enum Mount { FLOOR,WALL,CEILING,BOTH }
 public final String id;public final Mount mount;public final int slots;
 WorkshopKind(String id,Mount mount,int slots){this.id=id;this.mount=mount;this.slots=slots;}
 public boolean rack(){return this==WALL_SWORD_RACK||this==WEAPON_STAND;}
 public int light(){return this==GHOST_LANTERN?10:this==SKULL_CANDLES||this==RITUAL_CANDLES?12:0;}
}
