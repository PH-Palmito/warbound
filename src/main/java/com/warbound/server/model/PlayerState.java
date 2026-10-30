package com.warbound.server.model;

import com.warbound.shared.Faction;
import com.warbound.shared.dto.CardView;
import com.warbound.shared.dto.PlayerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

final class PlayerState {
    private static final int COMMANDER_HEALTH = 20;

    private final UUID id = UUID.randomUUID();
    private final String token = UUID.randomUUID().toString();
    private final String name;
    private final Faction faction;
    private final List<CardInstance> deck;
    private final List<CardInstance> hand = new ArrayList<>();
    private final List<UnitInstance> board = new ArrayList<>();
    private int commanderHealth = COMMANDER_HEALTH;
    private int mana;
    private int maxMana;

    PlayerState(String name, Faction faction) {
        this.name = name;
        this.faction = faction;
        this.deck = new ArrayList<>(DeckFactory.createDeck(faction));
        Collections.shuffle(deck);
    }

    UUID id() { return id; }
    String token() { return token; }
    String name() { return name; }
    Faction faction() { return faction; }
    int mana() { return mana; }
    int maxMana() { return maxMana; }
    List<CardInstance> hand() { return hand; }
    List<UnitInstance> board() { return board; }

    void drawCard() {
        if (!deck.isEmpty()) {
            hand.add(deck.remove(0));
        }
    }

    void beginTurn() {
        maxMana = Math.min(10, maxMana + 1);
        mana = maxMana;
        board.forEach(UnitInstance::ready);
        drawCard();
    }

    boolean spendMana(int cost) {
        if (mana < cost) {
            return false;
        }
        mana -= cost;
        return true;
    }

    void receiveDamage(int damage) { commanderHealth -= damage; }
    boolean isDefeated() { return commanderHealth <= 0; }

    CardInstance findHandCard(String id) {
        return hand.stream().filter(card -> card.id().toString().equals(id)).findFirst().orElse(null);
    }

    UnitInstance findUnit(String id) {
        return board.stream().filter(unit -> unit.id().toString().equals(id)).findFirst().orElse(null);
    }

    void removeDefeatedUnits() {
        board.removeIf(UnitInstance::isDefeated);
    }

    PlayerView toView() {
        return new PlayerView(
                id, name, faction, commanderHealth, mana, maxMana, deck.size(), hand.size(),
                board.stream().map(UnitInstance::toView).toList()
        );
    }

    List<CardView> handView() {
        return hand.stream().map(CardInstance::toView).toList();
    }
}
