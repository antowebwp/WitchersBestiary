package com.witchersbestiary.enchantment;
/** Entity-owned, saved cooldowns. No global collection and no per-tick entity scan. */
public interface SwordEnchantmentState {
 long bestiary$frostReady();void bestiary$frostReady(long value);
 long bestiary$harvestReady();void bestiary$harvestReady(long value);
 long bestiary$pulseReady();void bestiary$pulseReady(long value);
}
