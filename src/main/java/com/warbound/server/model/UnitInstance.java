package com.warbound.server.model;

import com.warbound.shared.Faction;
import com.warbound.shared.dto.UnitView;

final class UnitInstance {
    private final CardInstance card;
    private int health;
    private boolean readyToAttack;
    private int chargeTurnsRemaining;
    private int nextAttackBonus;
    private int berserkerBonus;
    private int nextDamageReduction;
    private boolean immuneToNextAttack;
    private boolean protectedUntilNextTurn;
    private int guardianCooldown;
    private boolean firstStrikeUsed;

    UnitInstance(CardInstance card) {
        this.card = card;
        this.health = card.definition().health();
        this.chargeTurnsRemaining = card.effect() == CardEffect.THUNDER_CHARGE ? 2 : 0;
        this.guardianCooldown = card.effect() == CardEffect.GUARDIAN_WALL ? 2 : 0;
    }

    CardInstance card() { return card; }
    String id() { return card.id().toString(); }
    Faction group() { return card.definition().group(); }
    CardEffect effect() { return card.effect(); }
    boolean canAttack() { return readyToAttack && health > 0 && chargeTurnsRemaining == 0; }
    boolean isDefeated() { return health <= 0; }
    boolean isAtOrBelowHalfHealth() { return health * 2 <= maxHealth(); }
    int maxHealth() { return card.definition().health(); }

    void beginOwnerTurn() {
        berserkerBonus = 0;
        immuneToNextAttack = false;
        protectedUntilNextTurn = false;
        if (chargeTurnsRemaining > 0) chargeTurnsRemaining--;
        if (guardianCooldown > 0) guardianCooldown--;
        readyToAttack = chargeTurnsRemaining == 0 && health > 0;
    }

    int attack(boolean bushido, boolean clanFury) {
        return card.definition().attack() + berserkerBonus + nextAttackBonus + (bushido ? 5 : 0) + (clanFury ? 10 : 0);
    }

    int consumeFirstStrikeBonus() {
        if (effect() != CardEffect.LANCER_FIRST_STRIKE || firstStrikeUsed) return 0;
        firstStrikeUsed = true;
        return 10;
    }

    void consumeAttackBonus() { nextAttackBonus = 0; }
    void addNextAttackBonus(int bonus) { nextAttackBonus += bonus; }
    void protectFromNextAttack() { immuneToNextAttack = true; }
    void protectUntilNextTurn() { protectedUntilNextTurn = true; }
    void reduceNextDamageBy(int reduction) { nextDamageReduction = Math.max(nextDamageReduction, reduction); }
    boolean canUseGuardianWall() { return effect() == CardEffect.GUARDIAN_WALL && guardianCooldown == 0; }
    void useGuardianWall() { guardianCooldown = 2; }

    int receiveAttackDamage(int rawDamage, boolean formation) {
        if (protectedUntilNextTurn) {
            return 0;
        }
        if (immuneToNextAttack) {
            immuneToNextAttack = false;
            return 0;
        }
        int reduction = (formation ? 5 : 0) + nextDamageReduction;
        nextDamageReduction = 0;
        int damage = Math.max(0, rawDamage - reduction);
        health -= damage;
        if (damage > 0 && effect() == CardEffect.BERSERKER_RAGE) berserkerBonus = Math.min(15, berserkerBonus + 5);
        return damage;
    }

    void heal(int amount) { health = Math.min(maxHealth(), health + amount); }
    void useAttack() { readyToAttack = false; }

    UnitView toView(int slot, boolean bushido, boolean clanFury) {
        return new UnitView(id(), card.definition().name(), attack(bushido, clanFury), health, canAttack(),
                maxHealth(), slot, groupLabel(group()), protectedUntilNextTurn || immuneToNextAttack,
                chargeTurnsRemaining, clanFury || berserkerBonus > 0);
    }

    private static String groupLabel(Faction group) {
        return switch (group) {
            case ROMANOS -> "Cavaleiro";
            case SAMURAIS -> "Samurai";
            case VIKINGS -> "Viking";
        };
    }
}
