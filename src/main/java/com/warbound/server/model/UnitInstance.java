package com.warbound.server.model;

import com.warbound.shared.dto.UnitView;

import java.util.UUID;

final class UnitInstance {
    private final UUID id;
    private final String name;
    private final int attack;
    private int health;
    private boolean readyToAttack;

    UnitInstance(UUID id, String name, int attack, int health) {
        this.id = id;
        this.name = name;
        this.attack = attack;
        this.health = health;
        this.readyToAttack = false;
    }

    UUID id() {
        return id;
    }

    int attack() {
        return attack;
    }

    boolean canAttack() {
        return readyToAttack && health > 0;
    }

    void useAttack() {
        readyToAttack = false;
    }

    void ready() {
        readyToAttack = true;
    }

    void receiveDamage(int damage) {
        health -= damage;
    }

    boolean isDefeated() {
        return health <= 0;
    }

    UnitView toView() {
        return new UnitView(id.toString(), name, attack, health, canAttack());
    }
}
