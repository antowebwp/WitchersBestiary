package com.witchersbestiary.combat;

/** Новые монстры подключают общие правила боя через категорию. */
public interface CategorizedMonster {
    MonsterCategory getMonsterCategory();
}
