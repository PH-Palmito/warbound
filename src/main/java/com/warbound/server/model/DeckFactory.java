package com.warbound.server.model;

import com.warbound.shared.CardType;
import com.warbound.shared.Faction;

import java.util.ArrayList;
import java.util.List;

/** Cada jogador recebe as mesmas 27 cartas, em uma ordem independente. */
final class DeckFactory {
    private DeckFactory() {
    }

    static List<CardInstance> createDeck() {
        List<CardDefinition> definitions = List.of(
                character("guardiao", "Guardião do Bastião", Faction.ROMANOS, 4, 15, 160, CardEffect.GUARDIAN_WALL, "Muralha a cada 2 turnos."),
                character("lanceiro", "Lanceiro Real", Faction.ROMANOS, 3, 28, 125, CardEffect.LANCER_FIRST_STRIKE, "Primeiro ataque causa +10 de dano."),
                character("espadachim", "Espadachim da Coroa", Faction.ROMANOS, 2, 25, 135, CardEffect.NONE, ""),
                character("cavaleira", "Cavaleira de Prata", Faction.ROMANOS, 3, 20, 145, CardEffect.NONE, ""),
                spell("bencao", "Bênção da Coroa", Faction.ROMANOS, 2, CardEffect.BLESSING, "Cura 25 HP."),
                spell("ordem", "Ordem Defensiva", Faction.ROMANOS, 2, CardEffect.DEFENSIVE_ORDER, "Reduz o próximo dano recebido em 20."),
                spell("suprimentos", "Suprimentos Reais", Faction.ROMANOS, 2, CardEffect.SUPPLIES, "Compre 2 cartas e descarte 1."),
                spell("recrutamento-1", "Recrutamento Real", Faction.ROMANOS, 2, CardEffect.RECRUITMENT, "Puxa uma carta Cavaleiro aleatória."),
                spell("recrutamento-2", "Recrutamento Real", Faction.ROMANOS, 2, CardEffect.RECRUITMENT, "Puxa uma carta Cavaleiro aleatória."),

                character("ronin", "Ronin", Faction.SAMURAIS, 2, 30, 100, CardEffect.NONE, ""),
                character("trovao", "Samurai do Trovão", Faction.SAMURAIS, 3, 40, 100, CardEffect.THUNDER_CHARGE, "Entra carregando por 2 turnos."),
                character("katana", "Mestre da Katana", Faction.SAMURAIS, 4, 35, 115, CardEffect.NONE, ""),
                character("monge", "Monge Guerreiro", Faction.SAMURAIS, 3, 22, 120, CardEffect.MONK_LOOK_AHEAD, "Escolha uma das 2 cartas do topo."),
                spell("iaijutsu", "Golpe Iaijutsu", Faction.SAMURAIS, 2, CardEffect.IAIJUTSU, "Um Samurai causa +15 no próximo ataque."),
                spell("meditacao", "Meditação", Faction.SAMURAIS, 2, CardEffect.MEDITATION, "Escolha uma das 3 cartas do topo."),
                spell("nevoa", "Névoa do Dojo", Faction.SAMURAIS, 2, CardEffect.MIST, "Um Samurai não recebe dano do próximo ataque."),
                spell("dojo-1", "Chamado do Dojo", Faction.SAMURAIS, 2, CardEffect.DOJO_CALL, "Puxa uma carta Samurai aleatória."),
                spell("dojo-2", "Chamado do Dojo", Faction.SAMURAIS, 2, CardEffect.DOJO_CALL, "Puxa uma carta Samurai aleatória."),

                character("berserker", "Berserker", Faction.VIKINGS, 3, 28, 120, CardEffect.BERSERKER_RAGE, "Recebe +5 ATQ ao sofrer dano, até +15."),
                character("jarl", "Jarl da Guerra", Faction.VIKINGS, 4, 25, 140, CardEffect.NONE, ""),
                character("machado", "Lançador de Machado", Faction.VIKINGS, 2, 30, 110, CardEffect.NONE, ""),
                character("navegadora", "Navegadora do Norte", Faction.VIKINGS, 3, 22, 120, CardEffect.NAVIGATOR_DRAW, "Ao entrar, compre 1 carta."),
                spell("hidromel", "Hidromel", Faction.VIKINGS, 2, CardEffect.HYDROMEL, "Cura 20 HP ou 35 HP em Viking."),
                spell("saque", "Saque", Faction.VIKINGS, 2, CardEffect.PLUNDER, "Compre 2 cartas e descarte 1."),
                spell("grito", "Grito de Guerra", Faction.VIKINGS, 2, CardEffect.WAR_CRY, "Um Viking causa +15 no próximo ataque."),
                spell("valhalla-1", "Chamado de Valhalla", Faction.VIKINGS, 2, CardEffect.VALHALLA_CALL, "Puxa uma carta Viking aleatória."),
                spell("valhalla-2", "Chamado de Valhalla", Faction.VIKINGS, 2, CardEffect.VALHALLA_CALL, "Puxa uma carta Viking aleatória.")
        );
        return definitions.stream().map(CardInstance::new).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    private static CardDefinition character(String key, String name, Faction group, int cost, int attack, int health, CardEffect effect, String description) {
        return new CardDefinition(key, name, CardType.CHARACTER, group, cost, attack, health, effect, description);
    }

    private static CardDefinition spell(String key, String name, Faction group, int cost, CardEffect effect, String description) {
        return new CardDefinition(key, name, CardType.SPELL, group, cost, 0, 0, effect, description);
    }
}
