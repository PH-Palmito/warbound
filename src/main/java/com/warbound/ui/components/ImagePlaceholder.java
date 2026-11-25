package com.warbound.ui.components;

import com.warbound.ui.fx.Recursos;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.TextAlignment;

/**
 * Encaixe de imagem do projeto (logo, ilustração de personagem). Se o
 * arquivo existir, aparece a imagem; se não, aparece um marcador tracejado
 * com o nome do que deveria estar ali.
 *
 * Dois modos de ajuste:
 *  - COBRIR: a imagem preenche o espaço inteiro, cortando o excesso sem
 *    deformar (usado nas ilustrações de personagem). {@code focoY} diz de
 *    onde cortar na vertical: 0 = mantém o topo, 1 = mantém a base.
 *  - CONTER: a imagem aparece inteira dentro do espaço (usado na logo).
 *
 * Com {@code esmaecer}, as bordas da ilustração se dissolvem no fundo
 * escuro da moldura, em vez de aparecer como um retângulo solto.
 *
 * Convenção de arquivos (dentro de src/main/resources):
 *   /assets/logo/warbound_logo.png
 *   /assets/characters/&lt;facção&gt;_&lt;carta&gt;.png
 */
public class ImagePlaceholder extends StackPane {

    public enum Modo { COBRIR, CONTER }

    private final double largura;
    private final double altura;
    private final Modo modo;
    private final double focoY;
    private final boolean esmaecer;
    private final String textoMarcador;
    private final ImageView imageView = new ImageView();

    /** Ilustração de personagem: cobre o espaço, foco no alto (rostos) e bordas esmaecidas. */
    public ImagePlaceholder(String resourcePath, double width, double height, String placeholderText) {
        this(resourcePath, width, height, placeholderText, Modo.COBRIR, 0.28, true);
    }

    public ImagePlaceholder(String resourcePath, double width, double height, String placeholderText,
                            Modo modo, double focoY, boolean esmaecer) {
        this.largura = width;
        this.altura = height;
        this.modo = modo;
        this.focoY = focoY;
        this.esmaecer = esmaecer;
        this.textoMarcador = placeholderText;

        setPrefSize(width, height);
        setMinSize(width, height);
        setMaxSize(width, height);
        setAlignment(Pos.CENTER);

        if (!carregar(resourcePath)) {
            getChildren().setAll(construirMarcador());
        }
    }

    /**
     * Tenta recarregar a imagem a partir do caminho, útil se a arte for
     * adicionada depois sem reiniciar a tela.
     *
     * @return true se a imagem foi encontrada e passou a ser exibida.
     */
    public boolean reload(String resourcePath) {
        return carregar(resourcePath);
    }

    private boolean carregar(String resourcePath) {
        Image imagem = Recursos.imagem(resourcePath);
        if (imagem == null) {
            return false;
        }
        imageView.setImage(imagem);
        imageView.setSmooth(true);

        if (modo == Modo.COBRIR) {
            double iw = imagem.getWidth();
            double ih = imagem.getHeight();
            double proporcaoAlvo = largura / altura;
            double vw;
            double vh;
            if (iw / ih > proporcaoAlvo) {
                vh = ih;
                vw = ih * proporcaoAlvo;
            } else {
                vw = iw;
                vh = iw / proporcaoAlvo;
            }
            double vx = (iw - vw) / 2.0;
            double vy = (ih - vh) * Math.max(0.0, Math.min(1.0, focoY));
            imageView.setViewport(new Rectangle2D(vx, vy, vw, vh));
            imageView.setPreserveRatio(false);
            imageView.setFitWidth(largura);
            imageView.setFitHeight(altura);
        } else {
            imageView.setViewport(null);
            imageView.setPreserveRatio(true);
            imageView.setFitWidth(largura);
            imageView.setFitHeight(altura);
        }

        getChildren().setAll(imageView);
        if (esmaecer) {
            Region vinheta = new Region();
            vinheta.getStyleClass().add("art-vignette");
            vinheta.setMouseTransparent(true);
            vinheta.setMinSize(largura, altura);
            vinheta.setPrefSize(largura, altura);
            vinheta.setMaxSize(largura, altura);
            getChildren().add(vinheta);
        }
        return true;
    }

    private StackPane construirMarcador() {
        Rectangle fundo = new Rectangle(largura, altura);
        fundo.setFill(Color.web("#1b1712"));
        fundo.setStroke(Color.web("#8a6a3a"));
        fundo.getStrokeDashArray().addAll(6.0, 4.0);
        fundo.setStrokeWidth(1.5);
        fundo.setArcWidth(6);
        fundo.setArcHeight(6);

        Label rotulo = new Label(textoMarcador);
        rotulo.setStyle("-fx-text-fill: #b9ab8e; -fx-font-size: 12px;");
        rotulo.setWrapText(true);
        rotulo.setTextAlignment(TextAlignment.CENTER);
        rotulo.setMaxWidth(largura - 16);

        StackPane grupo = new StackPane(fundo, rotulo);
        grupo.setAlignment(Pos.CENTER);
        return grupo;
    }
}
