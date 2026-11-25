package com.warbound.data;

import com.warbound.model.Carta;
import com.warbound.model.Classe;
import com.warbound.model.TipoCarta;

import java.util.List;

/** Catálogo visual do mesmo conjunto de 27 cartas criado pelo servidor. */
public final class CatalogoLocal {
    private CatalogoLocal() {
    }

    public static List<Carta> todasAsCartas() {
        return List.of(
                personagem("guardiao", "Guardião do Bastião", Classe.TEMPLARIOS, 4, 15, 160,
                        "Muralha: a cada 2 turnos, protege uma personagem aliada de todo dano de ataques até o próximo turno."),
                personagem("lanceiro", "Lanceiro Real", Classe.TEMPLARIOS, 3, 28, 125,
                        "O primeiro ataque após entrar em campo causa mais 10 de dano."),
                personagem("espadachim", "Espadachim da Coroa", Classe.TEMPLARIOS, 2, 25, 135,
                        "Personagem de combate sem efeito adicional."),
                personagem("cavaleira", "Cavaleira de Prata", Classe.TEMPLARIOS, 3, 20, 145,
                        "Personagem de combate sem efeito adicional."),
                efeito("bencao", "Bênção da Coroa", Classe.TEMPLARIOS, 2, "Cura 25 HP de uma personagem aliada."),
                efeito("ordem", "Ordem Defensiva", Classe.TEMPLARIOS, 2, "Uma personagem aliada reduz o próximo dano recebido em 20."),
                efeito("suprimentos", "Suprimentos Reais", Classe.TEMPLARIOS, 2, "Compre 2 cartas e descarte 1."),
                busca("recrutamento-1", "Recrutamento Real", Classe.TEMPLARIOS, "Puxa aleatoriamente uma carta Cavaleiro."),
                busca("recrutamento-2", "Recrutamento Real", Classe.TEMPLARIOS, "Puxa aleatoriamente uma carta Cavaleiro."),

                personagem("ronin", "Ronin", Classe.ORIENTAIS, 2, 30, 100, "Personagem de combate sem efeito adicional."),
                personagem("trovao", "Samurai do Trovão", Classe.ORIENTAIS, 3, 40, 100,
                        "Entra carregando por 2 turnos antes de poder atacar."),
                personagem("katana", "Mestre da Katana", Classe.ORIENTAIS, 4, 35, 115,
                        "Personagem de combate sem efeito adicional."),
                personagem("monge", "Monge Guerreiro", Classe.ORIENTAIS, 3, 22, 120,
                        "Ao entrar, olha as 2 cartas do topo e escolhe 1 para a mão."),
                efeito("iaijutsu", "Golpe Iaijutsu", Classe.ORIENTAIS, 2, "Um Samurai causa mais 15 de dano no próximo ataque."),
                efeito("meditacao", "Meditação", Classe.ORIENTAIS, 2, "Veja as 3 cartas do topo, escolha 1 e devolva as outras ao fundo."),
                efeito("nevoa", "Névoa do Dojo", Classe.ORIENTAIS, 2, "Um Samurai não recebe dano do próximo ataque."),
                busca("dojo-1", "Chamado do Dojo", Classe.ORIENTAIS, "Puxa aleatoriamente uma carta Samurai."),
                busca("dojo-2", "Chamado do Dojo", Classe.ORIENTAIS, "Puxa aleatoriamente uma carta Samurai."),

                personagem("berserker", "Berserker", Classe.NORDICOS, 3, 28, 120,
                        "Ao sofrer dano, ganha +5 ATQ até o próximo turno, com máximo de +15."),
                personagem("jarl", "Jarl da Guerra", Classe.NORDICOS, 4, 25, 140,
                        "Personagem de combate sem efeito adicional."),
                personagem("machado", "Lançador de Machado", Classe.NORDICOS, 2, 30, 110,
                        "Personagem de combate sem efeito adicional."),
                personagem("navegadora", "Navegadora do Norte", Classe.NORDICOS, 3, 22, 120,
                        "Ao entrar, compre 1 carta."),
                efeito("hidromel", "Hidromel", Classe.NORDICOS, 2, "Cura 20 HP. Cura 35 HP em um Viking."),
                efeito("saque", "Saque", Classe.NORDICOS, 2, "Compre 2 cartas e descarte 1."),
                efeito("grito", "Grito de Guerra", Classe.NORDICOS, 2, "Um Viking causa mais 15 de dano no próximo ataque."),
                busca("valhalla-1", "Chamado de Valhalla", Classe.NORDICOS, "Puxa aleatoriamente uma carta Viking."),
                busca("valhalla-2", "Chamado de Valhalla", Classe.NORDICOS, "Puxa aleatoriamente uma carta Viking.")
        );
    }

    private static Carta personagem(String id, String name, Classe classe, int cost, int attack, int health, String description) {
        return new Carta(id, name, classe, TipoCarta.PERSONAGEM, cost, attack, health, description, 1, null);
    }

    private static Carta efeito(String id, String name, Classe classe, int cost, String description) {
        return new Carta(id, name, classe, TipoCarta.EFEITO, cost, null, null, description, 1, null);
    }

    private static Carta busca(String id, String name, Classe classe, String description) {
        return new Carta(id, name, classe, TipoCarta.BUSCA, 2, null, null, description, 1, null);
    }
}
