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

    UnitInstance summon() {
        return new UnitInstance(id, definition.name(), definition.attack(), definition.health());
    }

    CardView toView() {
        return new CardView(id.toString(), definition.name(), definition.cost(), definition.attack(), definition.health());
    }
}
