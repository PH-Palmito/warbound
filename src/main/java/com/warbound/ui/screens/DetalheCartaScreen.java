package com.warbound.ui.screens;

import com.warbound.model.Carta;
import com.warbound.ui.ScreenManager;
import com.warbound.ui.components.AppHeader;
import com.warbound.ui.components.MolduraFacao;
import com.warbound.ui.fx.CameraLivre;
import com.warbound.ui.fx.Efeitos;
import com.warbound.ui.fx.ModeloAnimado;
import com.warbound.ui.fx.ModeloAnimadoFx;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.VertexFormat;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import javafx.util.Duration;

import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tela de detalhe de uma carta de personagem com modelo 3D.
 *
 * Layout: painel de atributos na lateral esquerda | modelo 3D no centro.
 * Suporta qualquer personagem registrado em {@link #CADASTRO}: Cavaleiro,
 * Arqueira, e futuros personagens com modelo .wbm.
 */
public class DetalheCartaScreen extends BorderPane {

    // -----------------------------------------------------------------------
    // Cadastro: cartaId → { caminho .wbm, pasta de texturas }
    // -----------------------------------------------------------------------

    private static final Map<String, String[]> CADASTRO = new HashMap<>();
    static {
        CADASTRO.put("tpl_cavaleiro", new String[]{
            "/assets/models/cavaleiro/cavaleiro_pose2.wbm",
            "/assets/models/cavaleiro/"
        });
        CADASTRO.put("tpl_arqueira", new String[]{
            "/assets/models/arqueira/arqueira.wbm",
            "/assets/models/arqueira/"
        });
        // Lanceiro (id tpl_besteiro): pose única do arquivo, com lança e escudo soltos como no GLB.
        CADASTRO.put("tpl_besteiro", new String[]{
            "/assets/models/lanceiro/lanceiro.wbm",
            "/assets/models/lanceiro/"
        });
        // Huscarl (id nor_hurcarl): modelo do antigo Berserker (viking.glb), pose de repouso.
        CADASTRO.put("nor_hurcarl", new String[]{
            "/assets/models/berserker/berserker.wbm",
            "/assets/models/berserker/"
        });
        // Jarl (id nor_jarl): GLB mastercraft_mithril_armor_set convertido por tools/gerar_jarl.py.
        CADASTRO.put("nor_jarl", new String[]{
            "/assets/models/jarl/jarl.wbm",
            "/assets/models/jarl/"
        });
        // Viking (id nor_viking): modelo do antigo Huscarl (hurcarl.wbm), pose de repouso.
        CADASTRO.put("nor_viking", new String[]{
            "/assets/models/hurcarl/hurcarl.wbm",
            "/assets/models/hurcarl/"
        });
    }

    /**
     * Personagens estáticos: exibidos na pose original do arquivo,
     * sem animações de respiração, inclinação ou olhar (apenas chama de luz).
     */
    private static final java.util.Set<String> SEM_ANIMACAO_IDLE = new java.util.HashSet<>(
            java.util.Arrays.asList("tpl_arqueira", "tpl_besteiro", "nor_hurcarl", "nor_jarl", "nor_viking")
    );

    /** FOV mais amplo para caber o personagem inteiro na tela. */
    private static final double FOV            = 32.0;
    /** Distância suficiente para ver dos pés à cabeça. */
    private static final double DISTANCIA_CAM  = 14.0;

    private static final double OLHAR_BASE      = 6.0;
    private static final double OLHAR_AMPLITUDE = 4.0;

    // -----------------------------------------------------------------------
    // Cache de modelos por cartaId
    // -----------------------------------------------------------------------

    private static final class Parte {
        final String nome;
        final TriangleMesh malha;
        Parte(String nome, TriangleMesh malha) { this.nome = nome; this.malha = malha; }
    }

    private static final class Modelo {
        float minY, maxY, pivotX, pivotZ, quadrilY;
        final List<Parte> partes = new ArrayList<>();
    }

    /** Personagens cujo .wbm tem UVs fora de 0..1 e V invertido (corrigidos em montarMalha). */
    private static final Set<String> CORRIGE_UV_UDIM = new HashSet<>(java.util.Arrays.asList("tpl_arqueira"));

    private static final Map<String, Modelo> CACHE   = new HashMap<>();
    private static final Set<String>         TENTOU  = new HashSet<>();

    // -----------------------------------------------------------------------
    // Construtor
    // -----------------------------------------------------------------------

    public DetalheCartaScreen(ScreenManager screenManager, Carta carta) {
        getStyleClass().add("screen-root");
        setTop(new AppHeader(carta.getNome() + " · Detalhes", screenManager::mostrarBiblioteca));

        // ---- painel lateral de atributos ----
        VBox painelAtributos = construirPainelAtributos(carta);

        // ---- centro: modelo 3D ou fallback ----
        Region centro = construirCentro3D(carta);

        // ---- layout geral ----
        HBox corpo = new HBox(0, painelAtributos, centro);
        HBox.setHgrow(centro, Priority.ALWAYS);
        setCenter(corpo);

        Efeitos.aparecer(painelAtributos, 220);
        Efeitos.entrarSuave(centro, 80, 400);
    }

    // -----------------------------------------------------------------------
    // Painel lateral de atributos
    // -----------------------------------------------------------------------

    private VBox construirPainelAtributos(Carta carta) {
        // cabeçalho com nome e moldura da facção
        Label nome = new Label(carta.getNome());
        nome.getStyleClass().add("modal-title");
        nome.setWrapText(true);

        Label faccaoTipo = new Label(
                carta.getClasse().getNomeExibicao() + " · " + tipoLabel(carta));
        faccaoTipo.getStyleClass().add("card-meta");
        faccaoTipo.setStyle("-fx-font-size: 13px;");

        // separador de cor da facção
        Region sep = new Region();
        sep.setPrefHeight(2);
        sep.setMaxWidth(Double.MAX_VALUE);
        MolduraFacao.aplicarCorLinha(sep, carta.getClasse());

        // atributos
        VBox atribs = new VBox(10,
                linhaAtributo("Custo",    String.valueOf(carta.getCusto()) + " Recursos"),
                linhaAtributo("ATQ",      carta.getAtaque() != null ? String.valueOf(carta.getAtaque()) : "—"),
                linhaAtributo("HP",       carta.getHp()     != null ? String.valueOf(carta.getHp())     : "—"),
                linhaAtributo("Cópias",   String.valueOf(carta.getCopias()))
        );
        atribs.getStyleClass().add("panel");
        atribs.setPadding(new Insets(14));

        // descrição / efeito
        Label tituloEfeito = new Label("Efeito");
        tituloEfeito.getStyleClass().add("section-label");

        Label descricao = new Label(carta.getDescricao());
        descricao.getStyleClass().add("card-desc");
        descricao.setWrapText(true);
        descricao.setStyle("-fx-font-size: 13px; -fx-line-spacing: 2;");
        descricao.setMaxWidth(Double.MAX_VALUE);

        VBox boxEfeito = new VBox(6, tituloEfeito, descricao);
        boxEfeito.getStyleClass().add("panel");
        boxEfeito.setPadding(new Insets(14));

        VBox painel = new VBox(16, nome, faccaoTipo, sep, atribs, boxEfeito);
        painel.setAlignment(Pos.TOP_LEFT);
        painel.setPadding(new Insets(24, 20, 24, 24));
        painel.setPrefWidth(280);
        painel.setMaxWidth(280);
        painel.setStyle(
            "-fx-background-color: rgba(16,13,10,0.88);" +
            "-fx-border-color: #6b5230;" +
            "-fx-border-width: 0 1 0 0;");

        return painel;
    }

    private HBox linhaAtributo(String rotulo, String valor) {
        Label lRotulo = new Label(rotulo);
        lRotulo.getStyleClass().add("text-secondary");
        lRotulo.setMinWidth(60);

        Label lValor = new Label(valor);
        lValor.getStyleClass().add("text-body");
        lValor.setStyle("-fx-font-size: 14px; -fx-font-family: 'MedievalSharp';");

        Region espaco = new Region();
        HBox.setHgrow(espaco, Priority.ALWAYS);

        HBox linha = new HBox(8, lRotulo, espaco, lValor);
        linha.setAlignment(Pos.CENTER_LEFT);
        return linha;
    }

    private String tipoLabel(Carta carta) {
        switch (carta.getTipo()) {
            case PERSONAGEM: return "Personagem";
            case EFEITO:     return "Efeito";
            case BUSCA:      return "Busca";
            default:         return "Equipamento";
        }
    }

    // -----------------------------------------------------------------------
    // Centro: SubScene 3D com modelo completo dos pés à cabeça
    // -----------------------------------------------------------------------

    private Region construirCentro3D(Carta carta) {
        try {
            if (!Platform.isSupported(ConditionalFeature.SCENE3D)) return fallback();
            if (ModeloAnimadoFx.temModelo(carta.getId())) {
                // Personagem com animação própria (Monge, Samurai): corpo inteiro, animado.
                ModeloAnimadoFx animado = ModeloAnimadoFx.obter(carta.getId(), "completo");
                return animado == null ? fallback() : construirSubCenaAnimada(animado);
            }
            if (!CADASTRO.containsKey(carta.getId())) return fallback();
            String pastaTexturas = CADASTRO.get(carta.getId())[1];
            Modelo modelo = carregarModelo(carta.getId());
            if (modelo == null) return fallback();
            return construirSubCena(modelo, pastaTexturas, carta.getId());
        } catch (Throwable t) {
            System.err.println("[DetalheCartaScreen] Erro ao criar 3D: " + t);
            return fallback();
        }
    }

    private Pane construirSubCena(Modelo modelo, String pastaTexturas, String cartaId) {
        double alturaModelo = modelo.maxY - modelo.minY;
        double meioY = (modelo.minY + modelo.maxY) / 2.0;

        // Escala: o personagem inteiro deve caber com folga (80% da altura visível).
        // alturaVisivel = 2 * dist * tan(FOV/2)
        double alturaVisivel = 2.0 * DISTANCIA_CAM * Math.tan(Math.toRadians(FOV / 2.0));
        double escala = (alturaVisivel * 0.80) / alturaModelo;

        // O centrar já move o centro do modelo para Y=0 via Translate(-pivotX, -meioY, -pivotZ).
        // A câmera fica em Y=0 para enquadrar o personagem inteiro centralizado.
        double focoY = 0;

        boolean staticIdle = SEM_ANIMACAO_IDLE.contains(cartaId);

        Group pernas = new Group();
        Group tronco = new Group();

        for (Parte p : modelo.partes) {
            MeshView mv = new MeshView(p.malha);
            mv.setMaterial(criarMaterial(p.nome, pastaTexturas));
            mv.setCullFace(CullFace.NONE);
            // "Legs" separado para que a animação de tronco não afete as pernas (cavaleiro).
            // Personagens sem parte "Legs" (ex.: Arqueira) colocam tudo no tronco.
            if ("Legs".equals(p.nome)) pernas.getChildren().add(mv);
            else                        tronco.getChildren().add(mv);
        }

        // Personagens estáticos: sem respiração/inclinação/olhar.
        Group raizPersonagem;
        List<Timeline> animacoes = new ArrayList<>();

        if (staticIdle) {
            Group centrar = new Group(pernas, tronco);
            centrar.getTransforms().add(new Translate(-modelo.pivotX, -meioY, -modelo.pivotZ));
            Group escalar = new Group(centrar); escalar.getTransforms().add(new Scale(escala, escala, escala));
            raizPersonagem = new Group(escalar); raizPersonagem.getTransforms().add(new Rotate(180, Rotate.X_AXIS));
        } else {
            Rotate    giroDoOlhar = new Rotate(OLHAR_BASE, Rotate.Y_AXIS);
            Translate bobTronco   = new Translate(0, 0, 0);
            Rotate    inclinacao  = new Rotate(0, modelo.pivotX, modelo.quadrilY, modelo.pivotZ, Rotate.Z_AXIS);
            Scale     respiracao  = new Scale(1, 1, 1, modelo.pivotX, modelo.quadrilY, modelo.pivotZ);

            Group respira  = new Group(tronco);  respira.getTransforms().add(respiracao);
            Group inclina  = new Group(respira); inclina.getTransforms().add(inclinacao);
            Group sobe     = new Group(inclina); sobe.getTransforms().add(bobTronco);
            Group centrar  = new Group(pernas, sobe);
            centrar.getTransforms().add(new Translate(-modelo.pivotX, -meioY, -modelo.pivotZ));
            Group olhar    = new Group(centrar); olhar.getTransforms().add(giroDoOlhar);
            Group escalar  = new Group(olhar);   escalar.getTransforms().add(new Scale(escala, escala, escala));
            raizPersonagem = new Group(escalar); raizPersonagem.getTransforms().add(new Rotate(180, Rotate.X_AXIS));

            animacoes.add(criarRespiracao(respiracao, bobTronco));
            animacoes.add(criarInclinacao(inclinacao));
            animacoes.add(criarOlhar(giroDoOlhar));
        }

        Group cena = new Group(raizPersonagem);

        // Luzes: ambiente frio + chama quente lateral + contraluz azul
        AmbientLight ambiente = new AmbientLight(Color.web("#4a5560"));
        PointLight chama = new PointLight(Color.web("#ffbf7a"));
        chama.setTranslateX(-10); chama.setTranslateY(-8); chama.setTranslateZ(-10);
        PointLight contraluz = new PointLight(Color.web("#5f8fdc"));
        contraluz.setTranslateX(3); contraluz.setTranslateY(-6); contraluz.setTranslateZ(12);

        // Chama de luz animada para todos os personagens (efeito de tocha ambiente)
        animacoes.add(criarChama(chama));

        // Câmera posicionada para ver o personagem inteiro
        PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.setFieldOfView(FOV);
        camera.setNearClip(0.1);
        camera.setFarClip(100);
        // Câmera livre: girar, zoom e mover com o mouse
        CameraLivre livre = new CameraLivre(camera, focoY, DISTANCIA_CAM);

        Group raiz3d = new Group(ambiente, chama, contraluz, cena, livre.getRig());

        Pane pane = new Pane();
        pane.setMinSize(0, 0);
        pane.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        SubScene sub = new SubScene(raiz3d, 600, 600, true, SceneAntialiasing.BALANCED);
        sub.setCamera(camera);
        sub.setFill(Color.TRANSPARENT);
        sub.setManaged(false);
        sub.setPickOnBounds(false);

        // SubScene acompanha o tamanho do Pane
        sub.widthProperty().bind(pane.widthProperty());
        sub.heightProperty().bind(pane.heightProperty());
        pane.getChildren().add(sub);
        livre.instalar(pane, sub);

        pane.sceneProperty().addListener((obs, antiga, nova) -> {
            if (nova != null) animacoes.forEach(Timeline::play);
            else              animacoes.forEach(Timeline::stop);
        });

        return pane;
    }

    /**
     * Corpo inteiro de um personagem com animação idle própria (esqueleto assado).
     * Mesma câmera e iluminação do cavaleiro; o movimento vem do FBX (sem animação por código).
     */
    private Pane construirSubCenaAnimada(ModeloAnimadoFx animado) {
        ModeloAnimado.Variante v = animado.variante();
        double alturaModelo = v.maxY - v.minY;

        double alturaVisivel = 2.0 * DISTANCIA_CAM * Math.tan(Math.toRadians(FOV / 2.0));
        double escala = (alturaVisivel * 0.80) / alturaModelo;
        double focoY = -(v.centroY * escala);

        Group centrar = new Group(animado.criarMalhas());
        centrar.getTransforms().add(new Translate(-v.centroX, 0, -v.centroZ));
        Group escalar = new Group(centrar); escalar.getTransforms().add(new Scale(escala, escala, escala));
        Group virar   = new Group(escalar); virar.getTransforms().add(new Rotate(180, Rotate.X_AXIS));
        Group cena = new Group(virar);

        AmbientLight ambiente = new AmbientLight(Color.web("#4a5560"));
        PointLight chama = new PointLight(Color.web("#ffbf7a"));
        chama.setTranslateX(-10); chama.setTranslateY(-8); chama.setTranslateZ(-10);
        PointLight contraluz = new PointLight(Color.web("#5f8fdc"));
        contraluz.setTranslateX(3); contraluz.setTranslateY(-6); contraluz.setTranslateZ(12);

        PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.setFieldOfView(FOV);
        camera.setNearClip(0.1);
        camera.setFarClip(100);
        // Câmera livre: girar, zoom e mover com o mouse
        CameraLivre livre = new CameraLivre(camera, focoY, DISTANCIA_CAM);

        Group raiz3d = new Group(ambiente, chama, contraluz, cena, livre.getRig());

        Pane pane = new Pane();
        pane.setMinSize(0, 0);
        pane.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        SubScene sub = new SubScene(raiz3d, 600, 600, true, SceneAntialiasing.BALANCED);
        sub.setCamera(camera);
        sub.setFill(Color.TRANSPARENT);
        sub.setManaged(false);
        sub.setPickOnBounds(false);
        sub.widthProperty().bind(pane.widthProperty());
        sub.heightProperty().bind(pane.heightProperty());
        pane.getChildren().add(sub);
        livre.instalar(pane, sub);

        Timeline luzChama = criarChama(chama);
        pane.sceneProperty().addListener((obs, antiga, nova) -> {
            if (nova != null) { luzChama.play(); animado.ligar(); }
            else              { luzChama.stop(); animado.desligar(); }
        });

        return pane;
    }

    // -----------------------------------------------------------------------
    // Material com textura — genérico para qualquer personagem
    // -----------------------------------------------------------------------

    private static PhongMaterial criarMaterial(String nome, String pastaTexturas) {
        PhongMaterial m = new PhongMaterial();
        // Cores de fallback por nome de parte (cobre cavaleiro + tons neutros)
        switch (nome) {
            case "Armor":  m.setDiffuseColor(Color.web("#aab1bb")); m.setSpecularColor(Color.gray(0.75)); m.setSpecularPower(54); break;
            case "Weapon": m.setDiffuseColor(Color.web("#b4bcc8")); m.setSpecularColor(Color.gray(0.85)); m.setSpecularPower(64); break;
            case "Wear":   m.setDiffuseColor(Color.web("#c49a3c")); m.setSpecularColor(Color.gray(0.10)); m.setSpecularPower(12); break;
            case "Hands":  m.setDiffuseColor(Color.web("#7a5232")); m.setSpecularColor(Color.gray(0.18)); m.setSpecularPower(18); break;
            case "Legs":   m.setDiffuseColor(Color.web("#4d3626")); m.setSpecularColor(Color.gray(0.12)); m.setSpecularPower(14); break;
            default:       m.setDiffuseColor(Color.web("#b0b0a8")); m.setSpecularColor(Color.gray(0.25)); m.setSpecularPower(20); break;
        }
        String chave = nome.toLowerCase();
        URL difusa = DetalheCartaScreen.class.getResource(pastaTexturas + "diffuse_" + chave + ".jpg");
        if (difusa == null) difusa = DetalheCartaScreen.class.getResource(pastaTexturas + "diffuse_" + chave + ".png"); // com alpha (cabelo)
        if (difusa != null) {
            m.setDiffuseColor(Color.WHITE);
            m.setDiffuseMap(new Image(difusa.toExternalForm(), true));
            URL brilho = DetalheCartaScreen.class.getResource(pastaTexturas + "specular_" + chave + ".png");
            if (brilho != null) { m.setSpecularColor(Color.gray(0.9)); m.setSpecularMap(new Image(brilho.toExternalForm(), true)); }
        }
        URL normal = DetalheCartaScreen.class.getResource(pastaTexturas + "normal_" + chave + ".png");
        if (normal != null) m.setBumpMap(new Image(normal.toExternalForm(), true));
        return m;
    }

    // -----------------------------------------------------------------------
    // Fallback quando 3D não está disponível
    // -----------------------------------------------------------------------

    private Region fallback() {
        Label aviso = new Label("Visualização 3D\nnão disponível");
        aviso.getStyleClass().add("text-secondary");
        aviso.setStyle("-fx-font-size: 15px;");
        aviso.setWrapText(true);
        StackPane sp = new StackPane(aviso);
        sp.setAlignment(Pos.CENTER);
        sp.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        return sp;
    }

    // -----------------------------------------------------------------------
    // Animações idle (mesmas do PersonagemView)
    // -----------------------------------------------------------------------

    private static Timeline criarRespiracao(Scale s, Translate bob) {
        Timeline t = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(s.xProperty(), 1.0),
                        new KeyValue(s.yProperty(), 1.0),
                        new KeyValue(s.zProperty(), 1.0),
                        new KeyValue(bob.yProperty(), 0.0)),
                new KeyFrame(Duration.seconds(1.7),
                        new KeyValue(s.xProperty(), 1.010, Interpolator.EASE_BOTH),
                        new KeyValue(s.yProperty(), 1.018, Interpolator.EASE_BOTH),
                        new KeyValue(s.zProperty(), 1.010, Interpolator.EASE_BOTH),
                        new KeyValue(bob.yProperty(), 0.025, Interpolator.EASE_BOTH)));
        t.setAutoReverse(true); t.setCycleCount(Timeline.INDEFINITE);
        return t;
    }

    private static Timeline criarInclinacao(Rotate r) {
        Timeline t = new Timeline(
                new KeyFrame(Duration.ZERO,          new KeyValue(r.angleProperty(), -0.7)),
                new KeyFrame(Duration.seconds(2.65), new KeyValue(r.angleProperty(),  0.7, Interpolator.EASE_BOTH)));
        t.setAutoReverse(true); t.setCycleCount(Timeline.INDEFINITE);
        return t;
    }

    private static Timeline criarOlhar(Rotate r) {
        Timeline t = new Timeline(
                new KeyFrame(Duration.ZERO,         new KeyValue(r.angleProperty(), OLHAR_BASE - OLHAR_AMPLITUDE)),
                new KeyFrame(Duration.seconds(4.2), new KeyValue(r.angleProperty(), OLHAR_BASE + OLHAR_AMPLITUDE, Interpolator.EASE_BOTH)));
        t.setAutoReverse(true); t.setCycleCount(Timeline.INDEFINITE);
        return t;
    }

    private static Timeline criarChama(PointLight luz) {
        Color a = Color.web("#ffbf7a"), b = Color.web("#ffd49c"),
              c = Color.web("#f2a860"), d = Color.web("#ffc888");
        Timeline t = new Timeline(
                new KeyFrame(Duration.ZERO,          new KeyValue(luz.colorProperty(), a)),
                new KeyFrame(Duration.seconds(0.35), new KeyValue(luz.colorProperty(), b)),
                new KeyFrame(Duration.seconds(0.70), new KeyValue(luz.colorProperty(), c)),
                new KeyFrame(Duration.seconds(1.20), new KeyValue(luz.colorProperty(), d)),
                new KeyFrame(Duration.seconds(1.55), new KeyValue(luz.colorProperty(), c)),
                new KeyFrame(Duration.seconds(2.00), new KeyValue(luz.colorProperty(), b)),
                new KeyFrame(Duration.seconds(2.60), new KeyValue(luz.colorProperty(), a)));
        t.setCycleCount(Timeline.INDEFINITE);
        return t;
    }

    // -----------------------------------------------------------------------
    // Leitura do .wbm (mesmo formato do PersonagemView / CavaleiroEspera)
    // -----------------------------------------------------------------------

    private static Modelo carregarModelo(String cartaId) {
        if (CACHE.containsKey(cartaId)) return CACHE.get(cartaId);
        if (TENTOU.contains(cartaId))   return null;
        TENTOU.add(cartaId);

        String wbmPath = CADASTRO.get(cartaId)[0];
        try (InputStream in = DetalheCartaScreen.class.getResourceAsStream(wbmPath)) {
            if (in == null) {
                System.err.println("[DetalheCartaScreen] Modelo não encontrado: " + wbmPath);
                return null;
            }
            ByteBuffer bb = ByteBuffer.wrap(in.readAllBytes());
            if (bb.getInt() != 0x57424D31) {
                System.err.println("[DetalheCartaScreen] Arquivo de modelo inválido.");
                return null;
            }

            Modelo modelo = new Modelo();
            modelo.minY     = bb.getFloat();
            modelo.maxY     = bb.getFloat();
            modelo.pivotX   = bb.getFloat();
            modelo.pivotZ   = bb.getFloat();
            modelo.quadrilY = bb.getFloat();

            int qtd = bb.getInt();
            for (int i = 0; i < qtd; i++) {
                int tamNome = bb.getShort() & 0xFFFF;
                byte[] bytesNome = new byte[tamNome];
                bb.get(bytesNome);
                String nome = new String(bytesNome, StandardCharsets.UTF_8);

                int nVerts = bb.getInt();
                float[] verts = new float[nVerts * 8];
                FloatBuffer fb = bb.asFloatBuffer(); fb.get(verts);
                bb.position(bb.position() + verts.length * 4);

                int nTris = bb.getInt();
                int[] tris = new int[nTris * 3];
                IntBuffer ib = bb.asIntBuffer(); ib.get(tris);
                bb.position(bb.position() + tris.length * 4);

                modelo.partes.add(new Parte(nome, montarMalha(verts, tris, CORRIGE_UV_UDIM.contains(cartaId))));
            }
            CACHE.put(cartaId, modelo);
            return modelo;
        } catch (Throwable t) {
            System.err.println("[DetalheCartaScreen] Erro ao ler modelo '" + cartaId + "': " + t);
            return null;
        }
    }

    private static TriangleMesh montarMalha(float[] v, int[] indices) {
        return montarMalha(v, indices, false);
    }

    /**
     * @param corrigirUvUdim true para modelos exportados com UVs fora de 0..1 (blocos UDIM) e V
     *                       invertido pelo conversor (ex.: arqueira). O JavaFX não repete a textura
     *                       fora de 0..1 (ele estica a borda), então cada parte é trazida para o
     *                       bloco 0..1 e o V é desinvertido. Cada parte do arquivo cabe em um único
     *                       bloco, então o deslocamento é o mesmo para todos os vértices dela.
     */
    private static TriangleMesh montarMalha(float[] v, int[] indices, boolean corrigirUvUdim) {
        int n = v.length / 8;
        float[] pontos  = new float[n * 3];
        float[] normais = new float[n * 3];
        float[] uvs     = new float[n * 2];
        for (int i = 0; i < n; i++) {
            int o = i * 8;
            pontos[i*3]=v[o];   pontos[i*3+1]=v[o+1]; pontos[i*3+2]=v[o+2];
            normais[i*3]=v[o+3]; normais[i*3+1]=v[o+4]; normais[i*3+2]=v[o+5];
            uvs[i*2]=v[o+6];    uvs[i*2+1]=v[o+7];
        }
        if (corrigirUvUdim && n > 0) {
            float minU = Float.MAX_VALUE, minV = Float.MAX_VALUE;
            for (int i = 0; i < n; i++) {
                minU = Math.min(minU, uvs[i*2]);
                minV = Math.min(minV, uvs[i*2+1]);
            }
            float blocoU = (float) Math.floor(minU + 1e-4f);
            float blocoV = (float) Math.floor(minV + 1e-4f);
            for (int i = 0; i < n; i++) {
                uvs[i*2]   = uvs[i*2]   - blocoU;
                uvs[i*2+1] = 1f - (uvs[i*2+1] - blocoV);
            }
        }
        int[] faces = new int[indices.length * 3];
        for (int i = 0; i < indices.length; i++) {
            faces[i*3]=indices[i]; faces[i*3+1]=indices[i]; faces[i*3+2]=indices[i];
        }
        TriangleMesh malha = new TriangleMesh(VertexFormat.POINT_NORMAL_TEXCOORD);
        malha.getPoints().setAll(pontos);
        malha.getNormals().setAll(normais);
        malha.getTexCoords().setAll(uvs);
        malha.getFaces().setAll(faces);
        return malha;
    }
}
