package com.warbound.ui.screens;

import com.warbound.ui.ScreenManager;
import com.warbound.ui.components.AppHeader;
import com.warbound.ui.components.ImagePlaceholder;
import com.warbound.ui.components.PrimaryButton;
import com.warbound.ui.components.SecondaryButton;
import com.warbound.ui.fx.Efeitos;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.util.Duration;

/**
 * Tela de Menu. Só direciona a navegação: não inicia partida, não faz
 * polling e não conecta no servidor (guia de frontend).
 *
 *   Jogar -> Sala de Partida
 *   Cartas -> Biblioteca de Cartas
 *   Sair
 *
 * O momento de destaque do jogo é a abertura deste Menu: a logo surge sobre
 * a floresta e os botões entram em sequência, uma única vez.
 */
public class MenuScreen extends BorderPane {

    public MenuScreen(ScreenManager screenManager) {
        getStyleClass().add("screen-root");
        setTop(new AppHeader("Menu", null, false));

        ImagePlaceholder logo = new ImagePlaceholder("/assets/logo/warbound_logo.png", 440, 330,
                "Logo\nWarbound", ImagePlaceholder.Modo.CONTER, 0.5, false);

        Label subtitulo = new Label("Um universo medieval épico e realista");
        subtitulo.getStyleClass().add("tagline");

        HBox faixa = new HBox(6,
                barra("tri-bar-templarios"), barra("tri-bar-orientais"), barra("tri-bar-nordicos"));
        faixa.setAlignment(Pos.CENTER);

        PrimaryButton jogar = new PrimaryButton("Jogar", screenManager::mostrarSalaDePartida);
        SecondaryButton cartas = new SecondaryButton("Cartas", screenManager::mostrarBiblioteca);
        SecondaryButton sair = new SecondaryButton("Sair", Platform::exit);
        for (Control c : new Control[] { jogar, cartas, sair }) {
            c.getStyleClass().add("menu-button");
            c.setMaxWidth(Double.MAX_VALUE);
        }

        VBox botoes = new VBox(10, jogar, cartas, sair);
        botoes.setAlignment(Pos.CENTER);
        botoes.setMaxWidth(280);

        HBox faccoes = new HBox(22,
                rotuloFaccao("Templários", "faction-text-templarios"),
                rotuloFaccao("Orientais", "faction-text-orientais"),
                rotuloFaccao("Nórdicos", "faction-text-nordicos"));
        faccoes.setAlignment(Pos.CENTER);

        VBox centro = new VBox(14, logo, subtitulo, faixa, botoes, faccoes);
        centro.setAlignment(Pos.CENTER);
        centro.setPadding(new Insets(12, 40, 12, 40));
        centro.getStyleClass().add("menu-plate");

        // a logo respira: brilho dourado e escala pulsando devagar
        DropShadow brilho = new DropShadow(26, Color.rgb(255, 232, 160, 0.55));
        brilho.setSpread(0.1);
        logo.setEffect(brilho);
        Timeline respiro = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(brilho.radiusProperty(), 22.0),
                        new KeyValue(logo.scaleXProperty(), 1.0),
                        new KeyValue(logo.scaleYProperty(), 1.0)),
                new KeyFrame(Duration.seconds(3.2),
                        new KeyValue(brilho.radiusProperty(), 44.0, Interpolator.EASE_BOTH),
                        new KeyValue(logo.scaleXProperty(), 1.025, Interpolator.EASE_BOTH),
                        new KeyValue(logo.scaleYProperty(), 1.025, Interpolator.EASE_BOTH)));
        respiro.setCycleCount(Timeline.INDEFINITE);
        respiro.setAutoReverse(true);
        respiro.play();
        logo.sceneProperty().addListener((obs, antiga, nova) -> {
            if (nova == null) respiro.stop();
        });

        // O centro da tela é da cena de batalha (3D, atrás): o painel do Menu fica à direita.
        setRight(centro);
        BorderPane.setAlignment(centro, Pos.CENTER_RIGHT);
        BorderPane.setMargin(centro, new Insets(0, 48, 0, 0));

        Efeitos.entrarSuave(logo, 0, 800);
        Efeitos.entrarSuave(subtitulo, 400, 500);
        Efeitos.entrarSuave(faixa, 500, 500);
        Efeitos.entrarSuave(jogar, 600, 450);
        Efeitos.entrarSuave(cartas, 720, 450);
        Efeitos.entrarSuave(sair, 840, 450);
        Efeitos.entrarSuave(faccoes, 980, 500);
    }

    private Region barra(String classe) {
        Region r = new Region();
        r.getStyleClass().addAll("tri-bar", classe);
        r.setMinSize(56, 3);
        r.setPrefSize(56, 3);
        r.setMaxSize(56, 3);
        return r;
    }

    private Label rotuloFaccao(String nome, String classe) {
        Label l = new Label(nome);
        l.getStyleClass().addAll("menu-faction", classe);
        return l;
    }
}
