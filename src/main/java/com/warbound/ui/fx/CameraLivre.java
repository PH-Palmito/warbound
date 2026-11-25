package com.warbound.ui.fx;

import javafx.animation.AnimationTimer;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SubScene;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.util.Duration;

/**
 * Câmera livre para as SubScenes 3D: orbitar, aproximar e mover.
 *
 *   arrastar (botão esquerdo)        gira em volta do personagem (com inércia)
 *   roda do mouse                    aproxima / afasta
 *   botão direito, meio ou Shift     move a câmera para os lados / para cima e baixo
 *   duplo clique                     volta ao enquadramento original
 *
 * Todos os movimentos são suavizados por um relógio próprio, que só roda
 * enquanto o painel estiver dentro de uma cena.
 *
 * Uso: crie com a câmera já configurada (FOV, clip) e coloque
 * {@link #getRig()} no grafo 3D no lugar da câmera; depois chame
 * {@link #instalar(Pane, SubScene)} com o painel que contém a SubScene.
 */
public final class CameraLivre {

    private static final double GIRO_POR_PIXEL = 0.45;
    private static final double ANGULO_X_MIN = -70;   // negativo = câmera acima
    private static final double ANGULO_X_MAX = 35;
    private static final double ZOOM_MIN = 0.35;
    private static final double ZOOM_MAX = 1.9;

    private final PerspectiveCamera camera;
    private final Group rig = new Group();
    private final Rotate giroY = new Rotate(0, Rotate.Y_AXIS);
    private final Rotate giroX = new Rotate(0, Rotate.X_AXIS);
    private final double distBase;

    // para onde o usuário quer ir (alvo) e onde a câmera está agora (suavizado)
    private double alvoY, alvoX, alvoDist, alvoPanX, alvoPanY;
    private double curY, curX, curDist, curPanX, curPanY;
    private double velY;   // inércia do giro, em graus por segundo

    private boolean arrastando;
    private double ultimoX, ultimoY;
    private long ultimoEvento;
    private long ultimoQuadro;

    private Pane painel;
    private Label dica;
    private Timeline fadeDica;

    private final AnimationTimer relogio = new AnimationTimer() {
        @Override
        public void handle(long agora) {
            if (ultimoQuadro == 0) {
                ultimoQuadro = agora;
                return;
            }
            double dt = (agora - ultimoQuadro) / 1_000_000_000.0;
            ultimoQuadro = agora;
            if (dt > 0.05) dt = 0.05;
            atualizar(dt);
        }
    };

    /**
     * @param camera    câmera já configurada (FOV, clip); será colocada dentro do rig
     * @param focoY     altura (eixo Y da cena) do ponto em volta do qual a câmera orbita
     * @param distancia distância inicial da câmera a esse ponto
     */
    public CameraLivre(PerspectiveCamera camera, double focoY, double distancia) {
        this.camera = camera;
        this.distBase = distancia;
        camera.setTranslateX(0);
        camera.setTranslateY(0);
        camera.setTranslateZ(-distancia);
        camera.setFarClip(Math.max(camera.getFarClip(), distancia * ZOOM_MAX * 4));
        rig.getTransforms().addAll(new Translate(0, focoY, 0), giroY, giroX);
        rig.getChildren().add(camera);
        alvoDist = distancia;
        curDist = distancia;
    }

    /** Grupo que contém a câmera; vai no grafo 3D no lugar da câmera. */
    public Group getRig() {
        return rig;
    }

    public void resetar() {
        alvoY = 0;
        alvoX = 0;
        alvoDist = distBase;
        alvoPanX = 0;
        alvoPanY = 0;
        velY = 0;
    }

    /** Liga o mouse do painel à câmera. A SubScene deixa de receber mouse (o painel recebe tudo). */
    public void instalar(Pane painel, SubScene sub) {
        this.painel = painel;
        sub.setMouseTransparent(true);
        painel.setCursor(Cursor.OPEN_HAND);

        painel.setOnMousePressed(e -> {
            arrastando = true;
            ultimoX = e.getSceneX();
            ultimoY = e.getSceneY();
            ultimoEvento = System.nanoTime();
            velY = 0;
            painel.setCursor(e.getButton() == MouseButton.PRIMARY && !e.isShiftDown()
                    ? Cursor.CLOSED_HAND : Cursor.MOVE);
            esconderDica();
        });

        painel.setOnMouseDragged(e -> {
            if (!arrastando) return;
            double dx = e.getSceneX() - ultimoX;
            double dy = e.getSceneY() - ultimoY;
            ultimoX = e.getSceneX();
            ultimoY = e.getSceneY();
            long agora = System.nanoTime();
            double dtEvento = (agora - ultimoEvento) / 1_000_000_000.0;
            ultimoEvento = agora;

            if (e.getButton() == MouseButton.PRIMARY && !e.isShiftDown()) {
                alvoY += dx * GIRO_POR_PIXEL;
                alvoX = limitar(alvoX - dy * 0.3, ANGULO_X_MIN, ANGULO_X_MAX);
                if (dtEvento > 0.001 && dtEvento < 0.1) {
                    velY = limitar(dx * GIRO_POR_PIXEL / dtEvento, -360, 360);
                }
            } else {
                double altura = Math.max(1.0, painel.getHeight());
                double porPixel = 2.0 * curDist * Math.tan(Math.toRadians(camera.getFieldOfView() / 2.0)) / altura;
                double lim = distBase * 0.9;
                alvoPanX = limitar(alvoPanX - dx * porPixel, -lim, lim);
                alvoPanY = limitar(alvoPanY - dy * porPixel, -lim, lim);
            }
        });

        painel.setOnMouseReleased(e -> {
            arrastando = false;
            painel.setCursor(Cursor.OPEN_HAND);
            if ((System.nanoTime() - ultimoEvento) / 1_000_000_000.0 > 0.08) {
                velY = 0;   // parou antes de soltar: sem inércia
            }
        });

        painel.setOnScroll(e -> {
            double fator = Math.exp(-e.getDeltaY() * 0.003);
            alvoDist = limitar(alvoDist * fator, distBase * ZOOM_MIN, distBase * ZOOM_MAX);
            esconderDica();
            e.consume();
        });

        painel.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) resetar();
        });

        criarDica(painel);

        painel.sceneProperty().addListener((obs, antiga, nova) -> {
            if (nova != null) {
                relogio.start();
            } else {
                relogio.stop();
                ultimoQuadro = 0;
            }
        });
        if (painel.getScene() != null) relogio.start();
    }

    // ------------------------------------------------------------------ interno

    private void atualizar(double dt) {
        if (!arrastando) {
            if (Math.abs(velY) > 0.5) {
                alvoY += velY * dt;
                velY *= Math.exp(-dt * 3.5);
            } else {
                velY = 0;
            }
        }
        double k = 1.0 - Math.exp(-dt * 14.0);
        curY += (alvoY - curY) * k;
        curX += (alvoX - curX) * k;
        curDist += (alvoDist - curDist) * k;
        curPanX += (alvoPanX - curPanX) * k;
        curPanY += (alvoPanY - curPanY) * k;

        giroY.setAngle(curY);
        giroX.setAngle(curX);
        camera.setTranslateZ(-curDist);
        camera.setTranslateX(curPanX);
        camera.setTranslateY(curPanY);
    }

    private void criarDica(Pane painel) {
        dica = new Label("Arraste: girar   ·   Roda: zoom   ·   Botão direito: mover   ·   Duplo clique: centralizar");
        dica.setMouseTransparent(true);
        dica.setStyle("-fx-background-color: rgba(8, 18, 30, 0.66); -fx-background-radius: 14;"
                + " -fx-padding: 6 16 6 16; -fx-text-fill: #f3e7c6; -fx-font-size: 12px;");
        painel.getChildren().add(dica);
        dica.layoutXProperty().bind(painel.widthProperty().subtract(dica.widthProperty()).divide(2));
        dica.layoutYProperty().bind(painel.heightProperty().subtract(dica.heightProperty()).subtract(22));

        fadeDica = new Timeline(
                new KeyFrame(Duration.seconds(8), new KeyValue(dica.opacityProperty(), 1.0)),
                new KeyFrame(Duration.seconds(9.2), new KeyValue(dica.opacityProperty(), 0.0, Interpolator.EASE_IN)));
        fadeDica.play();
    }

    private void esconderDica() {
        if (dica == null || dica.getOpacity() <= 0.0) return;
        if (fadeDica != null) fadeDica.stop();
        fadeDica = new Timeline(new KeyFrame(Duration.millis(350),
                new KeyValue(dica.opacityProperty(), 0.0, Interpolator.EASE_BOTH)));
        fadeDica.play();
    }

    private static double limitar(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}
