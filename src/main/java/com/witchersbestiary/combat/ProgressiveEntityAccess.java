package com.witchersbestiary.combat;
/** Server-owned spawn snapshot; natural monsters continue using the existing live world phase. */
public interface ProgressiveEntityAccess {
 long witcher$progressivePhase();
 void witcher$progressivePhase(long phase);
}
