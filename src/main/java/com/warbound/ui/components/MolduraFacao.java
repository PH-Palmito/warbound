package com.warbound.ui.components;

import com.warbound.model.Classe;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

import java.util.ArrayList;
import java.util.List;

/**
 * Moldura de cada facção, aplicada igualmente a cartas e vagas de campo.
 *
 *  - Templários: ouro com losangos nos cantos (nobreza, realeza).
 *  - Orientais: madeira escura com vigas de topo e base e filete vermelho,
 *    como a estrutura de um portal japonês.
 *  - Nórdicos: aço sobre pedra com rebites.
 *
 * A moldura em si é desenhada pelo CSS (classes moldura e moldura-&lt;facção&gt;);
 * esta classe só aplica as classes e cria os enfeites que o CSS não alcança.
 * O nó hospedeiro precisa ser um StackPane sem padding próprio.
 */
public final class MolduraFacao {

    private MolduraFacao() { }

    /** Sufixo usado nas classes de estilo: templarios, orientais ou nordicos. */
    public static String sufixo(Classe classe) {
        switch (classe) {
            case TEMPLARIOS: return "templarios";
            case ORIENTAIS: return "orientais";
            default: return "nordicos";
        }
    }

    public static void aplicar(Region hospedeiro, Classe classe) {
        hospedeiro.getStyleClass().addAll("moldura", "moldura-" + sufixo(classe));
    }

    /**
     * Aplica apenas a cor da facção como background de uma Region (ex.: separador horizontal).
     * Não aplica a moldura completa — útil para linhas decorativas em outras telas.
     */
    public static void aplicarCorLinha(Region linha, Classe classe) {
        switch (classe) {
            case TEMPLARIOS: linha.setStyle("-fx-background-color: #e3b92f;"); break;
            case ORIENTAIS:  linha.setStyle("-fx-background-color: #c8372d;"); break;
            default:         linha.setStyle("-fx-background-color: #3d82d0;"); break;
        }
    }

    /** Enfeites sobrepostos à moldura. Devem ser adicionados depois do conteúdo. */
    public static List<Node> ornamentos(Classe classe) {
        List<Node> lista = new ArrayList<>();
        switch (classe) {
            case TEMPLARIOS:
                lista.add(canto("orn-losango", Pos.TOP_LEFT));
                lista.add(canto("orn-losango", Pos.TOP_RIGHT));
                lista.add(canto("orn-losango", Pos.BOTTOM_LEFT));
                lista.add(canto("orn-losango", Pos.BOTTOM_RIGHT));
                break;
            case ORIENTAIS:
                lista.add(viga("orn-viga-topo", Pos.TOP_CENTER));
                lista.add(viga("orn-viga-base", Pos.BOTTOM_CENTER));
                break;
            default:
                lista.add(canto("orn-rebite", Pos.TOP_LEFT));
                lista.add(canto("orn-rebite", Pos.TOP_RIGHT));
                lista.add(canto("orn-rebite", Pos.BOTTOM_LEFT));
                lista.add(canto("orn-rebite", Pos.BOTTOM_RIGHT));
                break;
        }
        return lista;
    }

    private static Node canto(String classeCss, Pos posicao) {
        Region r = new Region();
        r.getStyleClass().add(classeCss);
        r.setMouseTransparent(true);
        StackPane.setAlignment(r, posicao);
        StackPane.setMargin(r, new Insets(3));
        return r;
    }

    private static Node viga(String classeCss, Pos posicao) {
        Region r = new Region();
        r.getStyleClass().add(classeCss);
        r.setMouseTransparent(true);
        r.setMaxWidth(Double.MAX_VALUE);
        StackPane.setAlignment(r, posicao);
        return r;
    }
}
