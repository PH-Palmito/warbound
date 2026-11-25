package com.warbound.ui.components;

import com.warbound.model.JogadorView;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Placa de informações de um jogador: nome, facção inicial, vidas,
 * recursos e quantidade de cartas na mão. Vidas e recursos aparecem como
 * marcadores e também como texto, então a informação não depende de cor.
 */
public class PainelJogador extends VBox {

    private static final int RECURSOS_MAXIMOS = 10;

    public PainelJogador(JogadorView jogador, boolean ehAdversario) {
        super(6);
        getStyleClass().addAll("panel", "player-plaque");

        Label nome = new Label(jogador.getNome());
        nome.getStyleClass().add("plaque-name");

        Label faccao = new Label("Baralho compartilhado");
        faccao.getStyleClass().add("plaque-faction");

        Region espaco = new Region();
        HBox.setHgrow(espaco, javafx.scene.layout.Priority.ALWAYS);
        HBox cabecalho = new HBox(8, nome, espaco, faccao);
        cabecalho.setAlignment(Pos.CENTER_LEFT);

        Label textoVidas = new Label("Vida  " + jogador.getVidasRestantes());
        textoVidas.getStyleClass().add("plaque-stat");

        HBox segmentos = new HBox(2);
        segmentos.setAlignment(Pos.CENTER_LEFT);
        for (int i = 0; i < RECURSOS_MAXIMOS; i++) {
            Region seg = new Region();
            seg.getStyleClass().add("res-seg");
            if (i < jogador.getRecursos()) seg.getStyleClass().add("res-seg-on");
            segmentos.getChildren().add(seg);
        }
        Label textoRecursos = new Label("Mana  " + jogador.getRecursos() + "/" + RECURSOS_MAXIMOS);
        textoRecursos.getStyleClass().add("plaque-stat");
        VBox linhaRecursos = new VBox(3, textoRecursos, segmentos);

        Label mao = new Label("Cartas na mão  " + jogador.getQuantidadeCartasNaMao());
        mao.getStyleClass().add("plaque-stat");

        getChildren().addAll(cabecalho, textoVidas, linhaRecursos, mao);

        if (jogador.getBuffDeGrupoAtivo() != null) {
            Label buff = new StatusBadge(StatusBadge.Tipo.BUFF, jogador.getBuffDeGrupoAtivo());
            buff.setWrapText(true);
            getChildren().add(buff);
        }
    }
}
