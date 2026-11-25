package com.warbound.ui.fx;

import com.warbound.model.Classe;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.effect.Glow;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Partículas e ondas de impacto (v3).
 *
 * Cada método descreve uma ação visual, nunca uma regra de jogo: a tela
 * chama {@code Particulas.dano(slot)} quando recebe um dano, sem calcular nada.
 * As cores das facções são as mesmas de styles.css.
 */
public final class Particulas {

    private static final Random RNG = new Random();

    private static final Color COR_TEMPLARIOS = Color.web("#d9c165");
    private static final Color COR_ORIENTAIS = Color.web("#d9756c");
    private static final Color COR_NORDICOS = Color.web("#78a9d9");
    private static final Color COR_DANO = Color.web("#d97e71");
    private static final Color COR_CURA = Color.web("#b0d98f");
    private static final Color COR_PROTECAO = Color.web("#9bbad9");
    private static final Color COR_OURO = Color.web("#fff0b0");
    private static final Color COR_CINZA = Color.web("#9a9286");

    private Particulas() { }

    public static Color corDaFacao(Classe classe) {
        switch (classe) {
            case TEMPLARIOS: return COR_TEMPLARIOS;
            case ORIENTAIS: return COR_ORIENTAIS;
            default: return COR_NORDICOS;
        }
    }

    // ------------------------------------------------------------------
    // Uma entrada por ação visível
    // ------------------------------------------------------------------

    /** Seleção de carta ou personagem: faíscas discretas. */
    public static void selecao(StackPane host, Classe classe) {
        faiscas(host, corDaFacao(classe), 8, 46, 320);
    }

    /** Ataque: faíscas da facção e um anel que se expande. */
    public static void ataque(StackPane host, Classe classe) {
        faiscas(host, corDaFacao(classe), 14, 90, 420);
        onda(host, corDaFacao(classe), 120, 380);
    }

    /** Dano: impacto vermelho forte. */
    public static void dano(StackPane host) {
        onda(host, COR_DANO, 150, 420);
        faiscas(host, COR_DANO, 16, 110, 460);
    }

    /** Cura: brasas verdes subindo. */
    public static void cura(StackPane host) {
        brasas(host, COR_CURA, 12, 900);
    }

    /** Proteção: anel azul e brilho que sobe. */
    public static void protecao(StackPane host) {
        onda(host, COR_PROTECAO, 140, 600);
        brasas(host, COR_PROTECAO, 8, 800);
    }

    /** Entrada em campo: anel da facção e brasas douradas. */
    public static void entrada(StackPane host, Classe classe) {
        onda(host, corDaFacao(classe), 170, 600);
        brasas(host, COR_OURO, 14, 1000);
    }

    /** Derrota: estilhaços cinzas e um toque de vermelho. */
    public static void derrota(StackPane host) {
        faiscas(host, COR_CINZA, 18, 130, 700);
        faiscas(host, COR_DANO, 8, 80, 500);
    }

    // ------------------------------------------------------------------
    // Primitivas
    // ------------------------------------------------------------------

    /** Pontos que saem do centro em direções aleatórias e se apagam. */
    public static void faiscas(StackPane host, Color cor, int quantidade, double alcance, double duracaoMs) {
        ParallelTransition grupo = new ParallelTransition();
        List<Circle> pontos = new ArrayList<>();
        for (int i = 0; i < quantidade; i++) {
            double angulo = RNG.nextDouble() * Math.PI * 2;
            double distancia = alcance * (0.5 + RNG.nextDouble() * 0.6);
            Circle p = novoPonto(host, cor, 1.5 + RNG.nextDouble() * 2.2);
            pontos.add(p);
            Timeline t = new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(p.translateXProperty(), 0.0),
                            new KeyValue(p.translateYProperty(), 0.0),
                            new KeyValue(p.opacityProperty(), 1.0)),
                    new KeyFrame(Duration.millis(duracaoMs),
                            new KeyValue(p.translateXProperty(), Math.cos(angulo) * distancia, Interpolator.EASE_OUT),
                            new KeyValue(p.translateYProperty(), Math.sin(angulo) * distancia, Interpolator.EASE_OUT),
                            new KeyValue(p.opacityProperty(), 0.0, Interpolator.EASE_IN)));
            grupo.getChildren().add(t);
        }
        grupo.setOnFinished(e -> host.getChildren().removeAll(pontos));
        grupo.play();
    }

    /** Pontos que nascem embaixo do centro e sobem, como brasas de fogueira. */
    public static void brasas(StackPane host, Color cor, int quantidade, double duracaoMs) {
        ParallelTransition grupo = new ParallelTransition();
        List<Circle> pontos = new ArrayList<>();
        for (int i = 0; i < quantidade; i++) {
            double x0 = (RNG.nextDouble() - 0.5) * 140;
            double y0 = 40 + RNG.nextDouble() * 30;
            double x1 = x0 + (RNG.nextDouble() - 0.5) * 50;
            double y1 = -(70 + RNG.nextDouble() * 70);
            Circle p = novoPonto(host, cor, 1.6 + RNG.nextDouble() * 2.2);
            pontos.add(p);
            Timeline t = new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(p.translateXProperty(), x0),
                            new KeyValue(p.translateYProperty(), y0),
                            new KeyValue(p.opacityProperty(), 0.0)),
                    new KeyFrame(Duration.millis(duracaoMs * 0.3),
                            new KeyValue(p.opacityProperty(), 1.0)),
                    new KeyFrame(Duration.millis(duracaoMs),
                            new KeyValue(p.translateXProperty(), x1, Interpolator.EASE_OUT),
                            new KeyValue(p.translateYProperty(), y1, Interpolator.EASE_OUT),
                            new KeyValue(p.opacityProperty(), 0.0, Interpolator.EASE_IN)));
            grupo.getChildren().add(t);
        }
        grupo.setOnFinished(e -> host.getChildren().removeAll(pontos));
        grupo.play();
    }

    /** Anel que cresce a partir do centro e some. */
    public static void onda(StackPane host, Color cor, double raioFinal, double duracaoMs) {
        Circle anel = new Circle(8, Color.TRANSPARENT);
        anel.setStroke(cor);
        anel.setStrokeWidth(3);
        anel.setMouseTransparent(true);
        anel.setEffect(new Glow(0.7));
        StackPane.setAlignment(anel, Pos.CENTER);
        host.getChildren().add(anel);
        Timeline t = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(anel.radiusProperty(), 8.0),
                        new KeyValue(anel.opacityProperty(), 1.0)),
                new KeyFrame(Duration.millis(duracaoMs),
                        new KeyValue(anel.radiusProperty(), raioFinal, Interpolator.EASE_OUT),
                        new KeyValue(anel.opacityProperty(), 0.0, Interpolator.EASE_IN)));
        t.setOnFinished(e -> host.getChildren().remove(anel));
        t.play();
    }

    private static Circle novoPonto(StackPane host, Color cor, double raio) {
        Circle p = new Circle(raio, cor);
        p.setMouseTransparent(true);
        p.setEffect(new Glow(0.8));
        StackPane.setAlignment(p, Pos.CENTER);
        host.getChildren().add(p);
        return p;
    }
}
