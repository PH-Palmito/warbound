package com.warbound.server.model;

import com.warbound.shared.dto.CardView;

import java.util.UUID;

final class CardInstance {
    private final UUID id = UUID.randomUUID();
    private final CardDefinition definition;

    CardInstance(CardDefinition definition) {
        this.definition = definition;
    }

    UUID id() {
        return id;
    }

    int cost() {
        return definition.cost();
    }

    com.warbound.shared.CardType type() {
        return definition.type();
    }

    CardEffect effect() {
        return definition.effect();
    }

    boolean belongsTo(com.warbound.shared.Faction group) {
        return definition.group() == group;
    }

    boolean isLowCost() {
        return definition.isLowCost();
    }

    boolean isBasicAttacker() {
        return definition.type() == com.warbound.shared.CardType.CHARACTER && definition.cost() == 2;
    }

    UnitInstance summon() {
        return new UnitInstance(this);
    }

    CardView toView() {
        return new CardView(id.toString(), definition.name(), definition.cost(), definition.attack(), definition.health(),
                definition.type(), definition.group(), definition.description());
    }

    CardDefinition definition() {
        return definition;
    }
}
