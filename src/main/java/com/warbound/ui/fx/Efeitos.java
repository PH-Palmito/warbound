package com.warbound.ui.fx;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/**
 * Efeitos visuais reutilizáveis. A intensidade segue a importância da ação
 * (guia de frontend): seleção e hover são discretos e rápidos; ataque, dano
 * e habilidades usam movimento maior.
 *
 * Nada aqui conhece regra de jogo: cada método só anima um nó que a tela
 * entrega.
 */
public final class Efeitos {

    private Efeitos() { }

    // ------------------------------------------------------------------
    // Discretos (hover, seleção)
    // ------------------------------------------------------------------

    /** Sobe e aumenta levemente o nó enquanto o mouse está sobre ele. */
    public static void levantarAoPassarOMouse(Node no, double escala, double subir) {
        no.setOnMouseEntered(e -> {
            no.setViewOrder(-1);
            animarTransformacao(no, escala, -subir, 130);
        });
        no.setOnMouseExited(e -> {
            no.setViewOrder(0);
            animarTransformacao(no, 1.0, 0.0, 160);
        });
    }

    private static void animarTransformacao(Node no, double escala, double deslocamentoY, double ms) {
        Timeline t = new Timeline(new KeyFrame(Duration.millis(ms),
                new KeyValue(no.scaleXProperty(), escala, Interpolator.EASE_OUT),
                new KeyValue(no.scaleYProperty(), escala, Interpolator.EASE_OUT),
                new KeyValue(no.translateYProperty(), deslocamentoY, Interpolator.EASE_OUT)));
        t.play();
    }

    /** Pulso curto, usado ao selecionar algo. */
    public static void pulso(Node no) {
        Timeline t = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(no.scaleXProperty(), 1.0), new KeyValue(no.scaleYProperty(), 1.0)),
                new KeyFrame(Duration.millis(90),
                        new KeyValue(no.scaleXProperty(), 1.06, Interpolator.EASE_OUT),
                        new KeyValue(no.scaleYProperty(), 1.06, Interpolator.EASE_OUT)),
                new KeyFrame(Duration.millis(200),
                        new KeyValue(no.scaleXProperty(), 1.0, Interpolator.EASE_IN),
                        new KeyValue(no.scaleYProperty(), 1.0, Interpolator.EASE_IN)));
        t.play();
    }

    // ------------------------------------------------------------------
    // Entradas
    // ------------------------------------------------------------------

    /** Aparece com fade e uma pequena subida. Usado uma única vez por tela. */
    public static void entrarSuave(Node no, double atrasoMs, double duracaoMs) {
        no.setOpacity(0);
        no.setTranslateY(16);
        Timeline t = new Timeline(
                new KeyFrame(Duration.millis(atrasoMs),
                        new KeyValue(no.opacityProperty(), 0.0),
                        new KeyValue(no.translateYProperty(), 16.0)),
                new KeyFrame(Duration.millis(atrasoMs + duracaoMs),
                        new KeyValue(no.opacityProperty(), 1.0, Interpolator.EASE_OUT),
                        new KeyValue(no.translateYProperty(), 0.0, Interpolator.EASE_OUT)));
        t.play();
    }

    /** Fade simples, sem deslocamento (troca de tela, janela modal). */
    public static void aparecer(Node no, double duracaoMs) {
        no.setOpacity(0);
        Timeline t = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(no.opacityProperty(), 0.0)),
                new KeyFrame(Duration.millis(duracaoMs), new KeyValue(no.opacityProperty(), 1.0, Interpolator.EASE_OUT)));
        t.play();
    }

    /** Janela modal: cresce de 92% a 100% enquanto aparece. */
    public static void abrirModal(Node caixa) {
        caixa.setOpacity(0);
        caixa.setScaleX(0.92);
        caixa.setScaleY(0.92);
        Timeline t = new Timeline(
                new KeyFrame(Duration.millis(180),
                        new KeyValue(caixa.opacityProperty(), 1.0, Interpolator.EASE_OUT),
                        new KeyValue(caixa.scaleXProperty(), 1.0, Interpolator.EASE_OUT),
                        new KeyValue(caixa.scaleYProperty(), 1.0, Interpolator.EASE_OUT)));
        t.play();
    }

    /** Pulsa indefinidamente a opacidade (ex.: texto de espera). */
    public static Timeline respirar(Node no, double minimo, double duracaoMs) {
        Timeline t = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(no.opacityProperty(), 1.0)),
                new KeyFrame(Duration.millis(duracaoMs), new KeyValue(no.opacityProperty(), minimo, Interpolator.EASE_BOTH)));
        t.setAutoReverse(true);
        t.setCycleCount(Timeline.INDEFINITE);
        t.play();
        return t;
    }

    // ------------------------------------------------------------------
    // Combate (intensos)
    // ------------------------------------------------------------------

    /** Tremor horizontal. {@code amplitude} em pixels. */
    public static void tremer(Node no, double amplitude) {
        Timeline t = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(no.translateXProperty(), 0.0)),
                new KeyFrame(Duration.millis(50), new KeyValue(no.translateXProperty(), amplitude)),
                new KeyFrame(Duration.millis(110), new KeyValue(no.translateXProperty(), -amplitude)),
                new KeyFrame(Duration.millis(170), new KeyValue(no.translateXProperty(), amplitude * 0.6)),
                new KeyFrame(Duration.millis(230), new KeyValue(no.translateXProperty(), -amplitude * 0.4)),
                new KeyFrame(Duration.millis(290), new KeyValue(no.translateXProperty(), 0.0)));
        t.play();
    }

    /** Avanço rápido na direção do alvo e retorno (ataque). */
    public static void investida(Node no, double deslocamentoY) {
        Timeline t = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(no.translateYProperty(), 0.0)),
                new KeyFrame(Duration.millis(110), new KeyValue(no.translateYProperty(), deslocamentoY, Interpolator.EASE_OUT)),
                new KeyFrame(Duration.millis(300), new KeyValue(no.translateYProperty(), 0.0, Interpolator.EASE_BOTH)));
        t.play();
    }

    /** Camada colorida que acende e some sobre o nó (dano, cura, proteção). */
    public static void clarao(StackPane host, String classeCss, double duracaoMs) {
        StackPane camada = new StackPane();
        camada.getStyleClass().add(classeCss);
        camada.setMouseTransparent(true);
        camada.setOpacity(0);
        host.getChildren().add(camada);
        Timeline t = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(camada.opacityProperty(), 0.0)),
                new KeyFrame(Duration.millis(duracaoMs * 0.25), new KeyValue(camada.opacityProperty(), 0.9)),
                new KeyFrame(Duration.millis(duracaoMs), new KeyValue(camada.opacityProperty(), 0.0, Interpolator.EASE_IN)));
        t.setOnFinished(e -> host.getChildren().remove(camada));
        t.play();
    }

    /** Risco de corte atravessando o nó (golpes). */
    public static void corte(StackPane host) {
        StackPane risco = new StackPane();
        risco.getStyleClass().add("fx-slash");
        risco.setMouseTransparent(true);
        risco.setMaxSize(5, 150);
        risco.setRotate(38);
        risco.setOpacity(0);
        risco.setScaleY(0.2);
        host.getChildren().add(risco);
        StackPane.setAlignment(risco, Pos.CENTER);
        Timeline t = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(risco.opacityProperty(), 0.0), new KeyValue(risco.scaleYProperty(), 0.2)),
                new KeyFrame(Duration.millis(90),
                        new KeyValue(risco.opacityProperty(), 1.0), new KeyValue(risco.scaleYProperty(), 1.1, Interpolator.EASE_OUT)),
                new KeyFrame(Duration.millis(260),
                        new KeyValue(risco.opacityProperty(), 0.0, Interpolator.EASE_IN), new KeyValue(risco.scaleYProperty(), 1.25)));
        t.setOnFinished(e -> host.getChildren().remove(risco));
        t.play();
    }

    /** Número que sobe e desaparece (dano, cura). */
    public static void textoFlutuante(StackPane host, String texto, String classeCss) {
        Label rotulo = new Label(texto);
        rotulo.getStyleClass().addAll("fx-floating-text", classeCss);
        rotulo.setMouseTransparent(true);
        rotulo.setOpacity(0);
        rotulo.setScaleX(0.6);
        rotulo.setScaleY(0.6);
        host.getChildren().add(rotulo);
        StackPane.setAlignment(rotulo, Pos.CENTER);
        Timeline t = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(rotulo.opacityProperty(), 0.0),
                        new KeyValue(rotulo.translateYProperty(), 0.0),
                        new KeyValue(rotulo.scaleXProperty(), 0.6),
                        new KeyValue(rotulo.scaleYProperty(), 0.6)),
                new KeyFrame(Duration.millis(140),
                        new KeyValue(rotulo.opacityProperty(), 1.0),
                        new KeyValue(rotulo.scaleXProperty(), 1.25, Interpolator.EASE_OUT),
                        new KeyValue(rotulo.scaleYProperty(), 1.25, Interpolator.EASE_OUT)),
                new KeyFrame(Duration.millis(420),
                        new KeyValue(rotulo.opacityProperty(), 1.0),
                        new KeyValue(rotulo.scaleXProperty(), 1.0),
                        new KeyValue(rotulo.scaleYProperty(), 1.0)),
                new KeyFrame(Duration.millis(1000),
                        new KeyValue(rotulo.opacityProperty(), 0.0, Interpolator.EASE_IN),
                        new KeyValue(rotulo.translateYProperty(), -54.0, Interpolator.EASE_OUT)));
        t.setOnFinished(e -> host.getChildren().remove(rotulo));
        t.play();
    }

    /** Executa uma ação depois de um atraso (encadear efeitos). */
    public static void depoisDe(double ms, Runnable acao) {
        PauseTransition p = new PauseTransition(Duration.millis(ms));
        p.setOnFinished(e -> acao.run());
        p.play();
    }

    /** Remove o nó do pai depois de um atraso. */
    public static void removerDepoisDe(Pane pai, Node no, double ms) {
        depoisDe(ms, () -> pai.getChildren().remove(no));
    }
}
