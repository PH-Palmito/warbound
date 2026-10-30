package com.warbound.server.model;

import com.warbound.shared.Faction;

import java.util.ArrayList;
import java.util.List;

final class DeckFactory {
    private DeckFactory() {
    }

    static List<CardInstance> createDeck(Faction faction) {
        List<CardDefinition> definitions = switch (faction) {
            case ROMANOS -> List.of(
                    new CardDefinition("Legionário", 1, 2, 2),
                    new CardDefinition("Centurião", 2, 3, 3),
                    new CardDefinition("Cavalaria Romana", 3, 4, 3)
            );
            case SAMURAIS -> List.of(
                    new CardDefinition("Ashigaru", 1, 1, 3),
                    new CardDefinition("Samurai", 2, 3, 2),
                    new CardDefinition("Ronin", 3, 4, 4)
            );
            case VIKINGS -> List.of(
                    new CardDefinition("Saqueador", 1, 2, 1),
                    new CardDefinition("Berserker", 2, 4, 2),
                    new CardDefinition("Jarl", 3, 3, 5)
            );
        };
        List<CardInstance> deck = new ArrayList<>();
        for (CardDefinition definition : definitions) {
            for (int copy = 0; copy < 4; copy++) {
                deck.add(new CardInstance(definition));
            }
        }
        return deck;
    }
}
