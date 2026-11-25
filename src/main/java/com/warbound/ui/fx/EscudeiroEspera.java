package com.warbound.ui.fx;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Pane;
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
import java.util.List;

/**
 * Escudeiro templário 3D em posição de espera para o combate, usado no Menu.
 *
 * O modelo (Pose_2) é uma malha estática, sem esqueleto. Por isso a "espera"
 * é feita com movimentos pequenos e independentes, que se somam sem repetir
 * de forma óbvia:
 *
 *   - respiração: o tronco (tudo, menos as pernas) sobe e desce, com o peito
 *     se expandindo levemente (ciclo de 3,4 s);
 *   - inclinação: o corpo transfere o peso de um lado para o outro (5,3 s);
 *   - olhar: o corpo gira devagar, como quem vigia o adversário (8,4 s);
 *   - luz: a chama que ilumina o cavaleiro oscila (2,6 s).
 *
 * A malha vem de assets/models/cavaleiro/cavaleiro_pose2.wbm, gerada por
 * tools/gerar_cavaleiro.py a partir do OBJ original.
 *
 * Se o 3D não for suportado, ou se o arquivo não existir ou estiver
 * corrompido, {@link #criar()} devolve null e o Menu segue sem o escudeiro.
 * O componente não faz parte de nenhuma regra de jogo.
 */
public final class EscudeiroEspera extends Pane {

    // ------------------------------------------------------------------
    // Ajustes (tudo que costuma ser mexido fica aqui)
    // ------------------------------------------------------------------

    private static final String ARQUIVO_MALHA = "/assets/models/cavaleiro/cavaleiro_pose2.wbm";
    private static final String PASTA_NORMAIS = "/assets/models/cavaleiro/";

    /** Altura do cavaleiro em unidades da cena (a câmera é calculada em cima disso). */
    private static final double ALTURA_ALVO = 7.4;
    /** Distância da câmera. Com FOV vertical de 30°, a tela mostra ~9 unidades de altura. */
    private static final double DISTANCIA_CAMERA = 17.0;
    /** Quanto o cavaleiro fica à esquerda do centro (unidades da cena, ~70 px cada). */
    private static final double DESLOCAMENTO_X = 5.8;

    /** Para onde o cavaleiro olha em repouso (graus; positivo = para a direita da tela, rumo ao Menu). */
    private static final double OLHAR_BASE = 10.0;
    private static final double OLHAR_AMPLITUDE = 8.0;

    /**
     * Usa os mapas de normal (relevo das armaduras). Se o relevo parecer
     * "afundado" em vez de saliente, é a convenção do mapa: coloque false.
     */
    private static final boolean USAR_MAPA_DE_NORMAL = true;

    /** Usa as texturas de cor e de brilho. Com false, volta às cores sólidas por material. */
    private static final boolean USAR_TEXTURAS = true;

    // ------------------------------------------------------------------
    // Dados do modelo (lidos uma vez e reaproveitados a cada abertura do Menu)
    // ------------------------------------------------------------------

    private static final class Parte {
        final String nome;
        final TriangleMesh malha;
        final PhongMaterial material;

        Parte(String nome, TriangleMesh malha, PhongMaterial material) {
            this.nome = nome;
            this.malha = malha;
            this.material = material;
        }
    }

    private static final class Modelo {
        float minY, maxY, pivotX, pivotZ, quadrilY;
        final List<Parte> partes = new ArrayList<>();
    }

    private static Modelo modeloEmCache;
    private static boolean jaTentouCarregar;

    // ------------------------------------------------------------------
    // Instância
    // ------------------------------------------------------------------

    private final List<Timeline> animacoes = new ArrayList<>();

    /**
     * Cria o cavaleiro, ou devolve null se não for possível (sem suporte a
     * 3D, arquivo ausente ou erro de leitura). Chamar na thread do JavaFX.
     */
    public static EscudeiroEspera criar() {
        try {
            if (!Platform.isSupported(ConditionalFeature.SCENE3D)) return null;
            Modelo modelo = carregarModelo();
            if (modelo == null) return null;
            return new EscudeiroEspera(modelo);
        } catch (Throwable t) {
            System.err.println("[EscudeiroEspera] Não foi possível criar o escudeiro 3D: " + t);
            return null;
        }
    }

    private EscudeiroEspera(Modelo modelo) {
        setMouseTransparent(true);
        setMinSize(0, 0);
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        double altura = modelo.maxY - modelo.minY;
        double escala = ALTURA_ALVO / altura;
        double meioY = (modelo.minY + modelo.maxY) / 2.0;

        // ---------- hierarquia do modelo ----------
        // cena -> virar (modelo Y-para-cima vira Y-para-baixo do JavaFX) -> escalar
        //      -> olhar (giro em torno do corpo) -> centrar (corpo na origem) -> peças
        Rotate giroDoOlhar = new Rotate(OLHAR_BASE, Rotate.Y_AXIS);
        Translate bobTronco = new Translate(0, 0, 0);
        Rotate inclinacao = new Rotate(0, modelo.pivotX, modelo.quadrilY, modelo.pivotZ, Rotate.Z_AXIS);
        Scale respiracao = new Scale(1, 1, 1, modelo.pivotX, modelo.quadrilY, modelo.pivotZ);

        Group pernas = new Group();
        Group tronco = new Group();
        for (Parte parte : modelo.partes) {
            MeshView vista = new MeshView(parte.malha);
            vista.setMaterial(parte.material);
            vista.setCullFace(CullFace.NONE);
            if ("Legs".equals(parte.nome)) {
                pernas.getChildren().add(vista);
            } else {
                tronco.getChildren().add(vista);
            }
        }

        Group respira = new Group(tronco);
        respira.getTransforms().add(respiracao);
        Group inclina = new Group(respira);
        inclina.getTransforms().add(inclinacao);
        Group sobe = new Group(inclina);
        sobe.getTransforms().add(bobTronco);

        Group centrar = new Group(pernas, sobe);
        centrar.getTransforms().add(new Translate(-modelo.pivotX, -meioY, -modelo.pivotZ));

        Group olhar = new Group(centrar);
        olhar.getTransforms().add(giroDoOlhar);

        Group escalar = new Group(olhar);
        escalar.getTransforms().add(new Scale(escala, escala, escala));

        Group virar = new Group(escalar);
        virar.getTransforms().add(new Rotate(180, Rotate.X_AXIS));

        // Sombra no chão, desenhada antes do cavaleiro.
        double chaoY = altura * escala / 2.0 + 0.02;
        MeshView sombra = criarSombra(chaoY, 2.5);

        Group cena = new Group(sombra, virar);
        cena.setTranslateX(-DESLOCAMENTO_X);

        // ---------- luzes ----------
        AmbientLight ambiente = new AmbientLight(Color.web("#59646f"));
        PointLight chama = new PointLight(Color.web("#d9b58f"));
        chama.setTranslateX(-13);
        chama.setTranslateY(-8);
        chama.setTranslateZ(-11);
        PointLight contraluz = new PointLight(Color.web("#7690bb"));
        contraluz.setTranslateX(2);
        contraluz.setTranslateY(-7);
        contraluz.setTranslateZ(12);

        // ---------- câmera e SubScene ----------
        PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.setNearClip(0.1);
        camera.setFarClip(100);
        camera.setTranslateZ(-DISTANCIA_CAMERA);

        Group raiz3d = new Group(ambiente, chama, contraluz, cena, camera);
        SubScene subScene = new SubScene(raiz3d, 10, 10, true, SceneAntialiasing.BALANCED);
        subScene.setCamera(camera);
        subScene.setFill(Color.TRANSPARENT);
        subScene.setManaged(false);
        subScene.setPickOnBounds(false);
        subScene.widthProperty().bind(widthProperty());
        subScene.heightProperty().bind(heightProperty());
        getChildren().add(subScene);

        // ---------- animações (só começam em iniciar()) ----------
        animacoes.add(criarRespiracao(respiracao, bobTronco));
        animacoes.add(criarInclinacao(inclinacao));
        animacoes.add(criarOlhar(giroDoOlhar));
        animacoes.add(criarChama(chama));
    }

    /** Começa (ou retoma) os movimentos de espera. */
    public void iniciar() {
        for (Timeline t : animacoes) t.play();
    }

    /** Para os movimentos. Necessário ao sair do Menu: Timeline infinita não para sozinha. */
    public void parar() {
        for (Timeline t : animacoes) t.stop();
    }

    // ------------------------------------------------------------------
    // Animações
    // ------------------------------------------------------------------

    private static Timeline criarRespiracao(Scale respiracao, Translate bob) {
        Timeline t = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(respiracao.xProperty(), 1.0),
                        new KeyValue(respiracao.yProperty(), 1.0),
                        new KeyValue(respiracao.zProperty(), 1.0),
                        new KeyValue(bob.yProperty(), 0.0)),
                new KeyFrame(Duration.seconds(1.7),
                        new KeyValue(respiracao.xProperty(), 1.012, Interpolator.EASE_BOTH),
                        new KeyValue(respiracao.yProperty(), 1.022, Interpolator.EASE_BOTH),
                        new KeyValue(respiracao.zProperty(), 1.012, Interpolator.EASE_BOTH),
                        new KeyValue(bob.yProperty(), 0.03, Interpolator.EASE_BOTH)));
        t.setAutoReverse(true);
        t.setCycleCount(Timeline.INDEFINITE);
        return t;
    }

    private static Timeline criarInclinacao(Rotate inclinacao) {
        Timeline t = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(inclinacao.angleProperty(), -0.9)),
                new KeyFrame(Duration.seconds(2.65),
                        new KeyValue(inclinacao.angleProperty(), 0.9, Interpolator.EASE_BOTH)));
        t.setAutoReverse(true);
        t.setCycleCount(Timeline.INDEFINITE);
        return t;
    }

    private static Timeline criarOlhar(Rotate giro) {
        Timeline t = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(giro.angleProperty(), OLHAR_BASE - OLHAR_AMPLITUDE)),
                new KeyFrame(Duration.seconds(4.2),
                        new KeyValue(giro.angleProperty(), OLHAR_BASE + OLHAR_AMPLITUDE, Interpolator.EASE_BOTH)));
        t.setAutoReverse(true);
        t.setCycleCount(Timeline.INDEFINITE);
        return t;
    }

    /** Oscilação da chama. O último quadro repete o primeiro, então o ciclo emenda sem salto. */
    private static Timeline criarChama(PointLight luz) {
        Color a = Color.web("#d9b58f");
        Color b = Color.web("#ffd49c");
        Color c = Color.web("#cea57d");
        Color d = Color.web("#d9ba97");
        Timeline t = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(luz.colorProperty(), a)),
                new KeyFrame(Duration.seconds(0.35), new KeyValue(luz.colorProperty(), b)),
                new KeyFrame(Duration.seconds(0.7), new KeyValue(luz.colorProperty(), c)),
                new KeyFrame(Duration.seconds(1.2), new KeyValue(luz.colorProperty(), d)),
                new KeyFrame(Duration.seconds(1.55), new KeyValue(luz.colorProperty(), c)),
                new KeyFrame(Duration.seconds(2.0), new KeyValue(luz.colorProperty(), b)),
                new KeyFrame(Duration.seconds(2.6), new KeyValue(luz.colorProperty(), a)));
        t.setCycleCount(Timeline.INDEFINITE);
        return t;
    }

    // ------------------------------------------------------------------
    // Sombra no chão: um quadrado plano com textura radial transparente
    // ------------------------------------------------------------------

    private static MeshView criarSombra(double y, double raio) {
        TriangleMesh malha = new TriangleMesh();
        malha.getPoints().setAll(
                -(float) raio, (float) y, -(float) raio,
                (float) raio, (float) y, -(float) raio,
                (float) raio, (float) y, (float) raio,
                -(float) raio, (float) y, (float) raio);
        malha.getTexCoords().setAll(0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f);
        malha.getFaces().setAll(
                0, 0, 1, 1, 2, 2,
                0, 0, 2, 2, 3, 3);

        int n = 128;
        WritableImage textura = new WritableImage(n, n);
        PixelWriter pw = textura.getPixelWriter();
        for (int py = 0; py < n; py++) {
            for (int px = 0; px < n; px++) {
                double dx = (px + 0.5) / n * 2.0 - 1.0;
                double dy = (py + 0.5) / n * 2.0 - 1.0;
                double dist = Math.sqrt(dx * dx + dy * dy);
                double alfa = dist >= 1.0 ? 0.0 : Math.pow(1.0 - dist, 1.7) * 0.8;
                pw.setColor(px, py, Color.color(0, 0, 0, alfa));
            }
        }
        PhongMaterial material = new PhongMaterial(Color.BLACK);
        material.setDiffuseMap(textura);

        MeshView vista = new MeshView(malha);
        vista.setMaterial(material);
        vista.setCullFace(CullFace.NONE);
        return vista;
    }

    // ------------------------------------------------------------------
    // Leitura do modelo (.wbm) e materiais
    // ------------------------------------------------------------------

    private static Modelo carregarModelo() {
        if (jaTentouCarregar) return modeloEmCache;
        jaTentouCarregar = true;

        try (InputStream in = EscudeiroEspera.class.getResourceAsStream(ARQUIVO_MALHA)) {
            if (in == null) {
                System.err.println("[EscudeiroEspera] Modelo não encontrado: " + ARQUIVO_MALHA);
                return null;
            }
            ByteBuffer bb = ByteBuffer.wrap(in.readAllBytes()); // big-endian, como o gerador grava
            if (bb.getInt() != 0x57424D31) {
                System.err.println("[EscudeiroEspera] Arquivo de modelo inválido.");
                return null;
            }

            Modelo modelo = new Modelo();
            modelo.minY = bb.getFloat();
            modelo.maxY = bb.getFloat();
            modelo.pivotX = bb.getFloat();
            modelo.pivotZ = bb.getFloat();
            modelo.quadrilY = bb.getFloat();

            int quantidade = bb.getInt();
            for (int i = 0; i < quantidade; i++) {
                int tamanhoNome = bb.getShort() & 0xFFFF;
                byte[] bytesNome = new byte[tamanhoNome];
                bb.get(bytesNome);
                String nome = new String(bytesNome, StandardCharsets.UTF_8);

                int nVertices = bb.getInt();
                float[] vertices = new float[nVertices * 8];
                FloatBuffer fb = bb.asFloatBuffer();
                fb.get(vertices);
                bb.position(bb.position() + vertices.length * 4);

                int nTriangulos = bb.getInt();
                int[] indices = new int[nTriangulos * 3];
                IntBuffer ib = bb.asIntBuffer();
                ib.get(indices);
                bb.position(bb.position() + indices.length * 4);

                modelo.partes.add(new Parte(nome, montarMalha(vertices, indices), criarMaterial(nome)));
            }
            modeloEmCache = modelo;
            return modelo;
        } catch (Throwable t) {
            System.err.println("[EscudeiroEspera] Erro ao ler o modelo: " + t);
            return null;
        }
    }

    /** Cada vértice no arquivo: x y z nx ny nz u v. Ponto, normal e UV compartilham o mesmo índice. */
    private static TriangleMesh montarMalha(float[] v, int[] indices) {
        int n = v.length / 8;
        float[] pontos = new float[n * 3];
        float[] normais = new float[n * 3];
        float[] uvs = new float[n * 2];
        for (int i = 0; i < n; i++) {
            int o = i * 8;
            pontos[i * 3] = v[o];
            pontos[i * 3 + 1] = v[o + 1];
            pontos[i * 3 + 2] = v[o + 2];
            normais[i * 3] = v[o + 3];
            normais[i * 3 + 1] = v[o + 4];
            normais[i * 3 + 2] = v[o + 5];
            uvs[i * 2] = v[o + 6];
            uvs[i * 2 + 1] = v[o + 7];
        }
        int[] faces = new int[indices.length * 3];
        for (int i = 0; i < indices.length; i++) {
            faces[i * 3] = indices[i];      // ponto
            faces[i * 3 + 1] = indices[i];  // normal
            faces[i * 3 + 2] = indices[i];  // texcoord
        }
        TriangleMesh malha = new TriangleMesh(VertexFormat.POINT_NORMAL_TEXCOORD);
        malha.getPoints().setAll(pontos);
        malha.getNormals().setAll(normais);
        malha.getTexCoords().setAll(uvs);
        malha.getFaces().setAll(faces);
        return malha;
    }

    /**
     * Material de cada peça. Se existirem as texturas de cor e de brilho
     * (diffuse_/specular_<material>, geradas por tools/gerar_texturas_cavaleiro.py),
     * elas pintam a peça. As cores abaixo (aço, túnica dourada de Templário,
     * couro) ficam só como reserva para um material sem textura.
     */
    private static PhongMaterial criarMaterial(String nome) {
        PhongMaterial m = new PhongMaterial();
        switch (nome) {
            case "Armor":
                m.setDiffuseColor(Color.web("#aab1bb"));
                m.setSpecularColor(Color.gray(0.75));
                m.setSpecularPower(54);
                break;
            case "Head":
                m.setDiffuseColor(Color.web("#9aa1ab"));
                m.setSpecularColor(Color.gray(0.75));
                m.setSpecularPower(54);
                break;
            case "Weapon":
                m.setDiffuseColor(Color.web("#b4bcc8"));
                m.setSpecularColor(Color.gray(0.85));
                m.setSpecularPower(64);
                break;
            case "Wear":
                m.setDiffuseColor(Color.web("#a78f5b"));
                m.setSpecularColor(Color.gray(0.10));
                m.setSpecularPower(12);
                break;
            case "Hands":
                m.setDiffuseColor(Color.web("#685240"));
                m.setSpecularColor(Color.gray(0.18));
                m.setSpecularPower(18);
                break;
            case "Legs":
                m.setDiffuseColor(Color.web("#4d3626"));
                m.setSpecularColor(Color.gray(0.12));
                m.setSpecularPower(14);
                break;
            default:
                m.setDiffuseColor(Color.web("#999999"));
                m.setSpecularColor(Color.gray(0.3));
                break;
        }

        String chave = nome.toLowerCase();

        // Cor: com a textura, a difusa vira branca para não tingir o mapa.
        URL difusa = EscudeiroEspera.class.getResource(PASTA_NORMAIS + "diffuse_" + chave + ".jpg");
        if (USAR_TEXTURAS && difusa != null) {
            m.setDiffuseColor(Color.WHITE);
            m.setDiffuseMap(new Image(difusa.toExternalForm(), true)); // carrega em segundo plano

            // Brilho: o mapa já traz metal x couro/tecido; a cor só dosa a intensidade geral.
            URL brilho = EscudeiroEspera.class.getResource(PASTA_NORMAIS + "specular_" + chave + ".png");
            if (brilho != null) {
                m.setSpecularColor(Color.gray(0.9));
                m.setSpecularMap(new Image(brilho.toExternalForm(), true));
            }
        }

        if (USAR_MAPA_DE_NORMAL) {
            URL url = EscudeiroEspera.class.getResource(PASTA_NORMAIS + "normal_" + chave + ".png");
            if (url != null) {
                m.setBumpMap(new Image(url.toExternalForm(), true)); // carrega em segundo plano
            }
        }
        return m;
    }
}
