package com.witchersbestiary.combat;

/** Discard an interrupted action, never resume a delayed damage window after stun. */
public interface Stunnable {
    void cancelCombatForStun();
    CombatState getCombatState();
    boolean trackedStun();
    void setTrackedStun(boolean value);
}
