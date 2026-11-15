package com.witchersbestiary.combat;

import com.witchersbestiary.signs.SignType;
import net.minecraft.entity.LivingEntity;

public final class StageCombatBalance {
    public enum Sword {
        STEEL(8, .35), SILVER(9, .5), GRAVE(12, .6), BLOOD_LEGACY(16, .8);

        public final int base;
        public final double percent;

        Sword(int base, double percent) {
            this.base = base;
            this.percent = percent;
        }
    }

    public enum Bomb {
        GRAPESHOT(6, 3), MOON_DUST(2, 1), SAMUM(2, 0), DRAGONS_DREAM(8, 4);

        public final int base;
        public final double percent;

        Bomb(int base, double percent) {
            this.base = base;
            this.percent = percent;
        }

        public float damage(LivingEntity target) {
            return base + MonsterHealthScaling.component(target, percent / 100, 24);
        }
    }

    private StageCombatBalance() {}

    public static double signPercent(SignType sign, int form, int rank) {
        return switch (sign) {
            case IGNI -> form == 2 ? .4 + .1 * rank : 2 + .5 * rank;
            case AARD -> 1.5 + .25 * rank;
            case YRDEN -> (form == 3 ? .8 : .5) + .15 * rank;
            case QUEN -> switch (form) {
                case 1 -> 1.5 + .25 * rank;
                case 2 -> .6 + .15 * rank;
                default -> 0;
            };
            case AXII -> 0;
        };
    }

    public static float signDamage(LivingEntity target, SignType sign, int form, int rank, float multiplier) {
        return MonsterHealthScaling.component(target, signPercent(sign, form, rank) * multiplier / 100, 16);
    }

    public static float burnDamage(LivingEntity target, int rank) {
        return MonsterHealthScaling.component(target, (.2 + .05 * rank) / 100, 4);
    }
}
