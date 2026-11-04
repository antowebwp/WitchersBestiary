package com.witchersbestiary.fist;

/** Server-owned virtual combat. Never writes Minecraft health or inventory. */
public final class FistFightSession {
 public enum Action { IDLE, QUICK, HEAVY, BLOCK, DODGE, COUNTER, STAGGER, KNOCKOUT, VICTORY }
 public enum Stage { COUNTDOWN, FIGHTING, FINISHED, CANCELLED }
 public static final class Fighter {
  public double health=100,stamina=100,maxStamina=100,heavyDamageMultiplier=1;public Action action=Action.IDLE;
  public boolean hitResolved,recovering;public int actionTicks,cooldown,counterTicks,regenDelay,combo;
 }
 private final java.util.Random random=new java.util.Random();
 public final Fighter player=new Fighter(),opponent=new Fighter();
 public Stage stage=Stage.COUNTDOWN;public int ticks;public boolean playerWon;
 public boolean act(boolean owner,Action action){
  if(stage!=Stage.FIGHTING)return false;var f=owner?player:opponent;
  if(action==Action.BLOCK&&f.action==Action.BLOCK)return false;
  if(f.cooldown>0||f.action==Action.STAGGER||f.action==Action.QUICK||f.action==Action.HEAVY||f.action==Action.COUNTER)return false;
  double cost=switch(action){case QUICK->8;case HEAVY->24;case DODGE->20;case COUNTER->12;case BLOCK->2;default->0;};
  if(action==Action.COUNTER&&f.counterTicks<=0||f.stamina<cost)return false;
  f.stamina-=cost;f.action=action;f.actionTicks=0;f.hitResolved=false;f.regenDelay=action==Action.HEAVY||action==Action.DODGE?35:18;
  if(action==Action.DODGE)f.cooldown=10;return true;
 }
 public boolean releaseBlock(boolean owner){var f=owner?player:opponent;if(f.action!=Action.BLOCK)return false;f.action=Action.IDLE;f.cooldown=4;return true;}
 public void tick(){
  if(stage==Stage.FINISHED||stage==Stage.CANCELLED)return;
  if(stage==Stage.COUNTDOWN){if(++ticks>=60){stage=Stage.FIGHTING;ticks=0;}return;}
  ticks++;step(player);step(opponent);
  if(ticks>=6000)stage=Stage.CANCELLED;
 }
 private void step(Fighter f){
  f.actionTicks++;if(f.cooldown>0)f.cooldown--;if(f.counterTicks>0)f.counterTicks--;if(f.regenDelay>0)f.regenDelay--;
  if(f.action==Action.BLOCK){f.stamina=Math.max(0,f.stamina-.65);if(f.stamina==0){f.action=Action.STAGGER;f.actionTicks=0;f.cooldown=18;}}
  else if(f.regenDelay==0)f.stamina=Math.min(f.maxStamina,f.stamina+.65);
  int length=switch(f.action){case QUICK->10;case HEAVY->28;case COUNTER->14;case DODGE->10;case STAGGER->18;default->Integer.MAX_VALUE;};
  if(f.actionTicks>=length){f.action=Action.IDLE;f.actionTicks=0;}
 }
 public boolean contact(boolean owner){var f=owner?player:opponent;return stage==Stage.FIGHTING&&f.actionTicks==switch(f.action){case QUICK->4;case HEAVY->14;case COUNTER->5;default->-1;};}
 public boolean hit(boolean owner,boolean reachable,boolean defenderFacing){
  if(!contact(owner)||!reachable)return false;var attack=owner?player:opponent;var target=owner?opponent:player;
  if(attack.hitResolved)return false;attack.hitResolved=true;
  if(target.action==Action.DODGE&&target.actionTicks<=6){target.counterTicks=12;return false;}
  double damage=switch(attack.action){case HEAVY->22*attack.heavyDamageMultiplier;case COUNTER->16;default->9;};
  if(target.action==Action.BLOCK&&defenderFacing){boolean timely=target.actionTicks<=6;double drain=attack.action==Action.HEAVY?28:10;
   if(target.stamina>=drain){target.stamina-=drain;damage*=.25;if(timely)target.counterTicks=14;}
   else{target.stamina=0;target.action=Action.STAGGER;target.actionTicks=0;target.cooldown=18;damage*=.8;}}
  if(attack.action==Action.QUICK){attack.combo=Math.min(2,attack.combo+1);if(attack.combo==2){damage+=2;attack.combo=0;}}else attack.combo=0;
  if(attack.action==Action.COUNTER||attack.action==Action.QUICK&&target.action==Action.HEAVY&&target.actionTicks<14&&random.nextInt(5)==0){target.action=Action.STAGGER;target.actionTicks=0;target.cooldown=18;}
  target.health=Math.max(0,target.health-damage);target.regenDelay=25;
  if(target.health==0){stage=Stage.FINISHED;playerWon=owner;target.action=Action.KNOCKOUT;attack.action=Action.VICTORY;}
  return true;
 }
 public void cancel(){if(stage!=Stage.FINISHED)stage=Stage.CANCELLED;}
}
