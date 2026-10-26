package com.witchersbestiary.accessory;

/** Persistent, shared victim budget. Switching players or reconnecting cannot refill it. */
public interface VampireBloodReserve {
 float bestiary$bloodDrawn();
 void bestiary$bloodDrawn(float amount);
}
