package com.witchersbestiary.combat;

/** Implemented by monsters with a limited, vulnerable preparation window. */
public interface InterruptibleAttack {
    boolean canInterruptAttack();
    void interruptAttack();
}
