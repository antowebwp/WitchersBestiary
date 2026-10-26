package com.witchersbestiary.artifact;
/** Player-wide persisted cooldowns; changing rings cannot bypass them. */
public interface ArtifactCooldowns {
 long bestiary$rootsReady();
 void bestiary$rootsReady(long tick);
 long bestiary$wolfReady();
 void bestiary$wolfReady(long tick);
}
