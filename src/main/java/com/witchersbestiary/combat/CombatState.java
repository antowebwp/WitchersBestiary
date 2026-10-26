package com.witchersbestiary.combat;

/** Server-observable phases; attack damage is applied only in ATTACK_ACTIVE. */
public enum CombatState {
    IDLE, CHASE, POSITIONING, ATTACK_WINDUP, ATTACK_ACTIVE, ATTACK_RECOVERY,
    SPECIAL_ABILITY, STUNNED, PHASE_TRANSITION
}
