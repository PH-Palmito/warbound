package com.warbound.ui.fx;

import com.warbound.ui.audio.AmbienteSom;
import javafx.animation.AnimationTimer;
import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.VertexFormat;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;

import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Cena de batalha do Menu: três guerreiros (cavaleiro, viking e samurai) se
 * encarando num campo de batalha sob tempestade.
 *
 *   - cavaleiro olha para o viking, viking olha para o samurai e samurai olha
 *     para o cavaleiro (triângulo de olhares);
 *   - cada um tem um idle de combate animado (arquivos *_cena.wba, gerados por
 *     tools/gerar_cena_*.py: poses com as armas nas mãos + animação em laço);
 *   - o campo vem de assets/models/cena/campo.wbm (battlefield.glb sem o robô);
 *   - chuva (Canvas por cima da cena 3D), neblina, relâmpagos que clareiam tudo
 *     e trovões (som), mais o som ambiente de chuva e vento.
 *
 * O mundo é montado com Y para cima (como os modelos) e girado 180° em X para o
 * sistema do JavaFX (Y para baixo), igual ao CavaleiroEspera.
 *
 * Se não houver 3D, ou se faltar arquivo, {@link #criar()} devolve null e o
 * Menu segue com o fundo de sempre. Nada aqui faz parte de regra de jogo.
 */
public final class CenaBatalha extends Pane {

    // ------------------------------------------------------------------
    // Ajustes (tudo que costuma ser mexido fica aqui)
    // ------------------------------------------------------------------

    private static final String PASTA = "/assets/models/cena/";

    /** Câmera (mundo com Y para cima; a câmera olha para -Z). */
    private static final double CAMERA_X = 0.9;
    private static final double CAMERA_Y = 1.2;
    private static final double CAMERA_Z = 5.4;
    /** Inclinação para cima, em graus (câmera baixa, olhando de baixo, como na foto). */
    private static final double CAMERA_INCLINACAO = 4.0;
    private static final double CAMERA_FOV = 38.0;

    private static final int N_GOTAS = 320;
    /** Intervalo (s) entre relâmpagos. */
    private static final double RAIO_MIN = 6.0;
    private static final double RAIO_MAX = 15.0;

    private static final Color AMBIENTE_BASE = Color.web("#4b5663");
    private static final Color AMBIENTE_RAIO = Color.web("#c3cee6");
    private static final Color LUZ_FRIA_BASE = Color.web("#8aa0c0");
    private static final Color LUZ_FRIA_RAIO = Color.web("#ffffff");
    private static final Color LUZ_QUENTE_BASE = Color.web("#6a4528");
    private static final Color LUZ_QUENTE_RAIO = Color.web("#b9b0a8");

    /** Personagem da cena: id no ModeloAnimadoFx, posição, tamanho e para quem olha. */
    private static final class Config {
        final String id;
        final double x, z;
        final double altura;        // altura final no mundo (metros)
        final double pesY;          // Y dos pés no modelo
        final double alturaModelo;  // do pé ao topo da cabeça, em unidades do modelo
        final double centroX, centroZ;
        final int alvo;             // índice de quem ele encara

        Config(String id, double x, double z, double altura, double pesY, double alturaModelo,
               double centroX, double centroZ, int alvo) {
            this.id = id; this.x = x; this.z = z; this.altura = altura; this.pesY = pesY;
            this.alturaModelo = alturaModelo; this.centroX = centroX; this.centroZ = centroZ;
            this.alvo = alvo;
        }
    }

    // 0 = cavaleiro (olha o viking), 1 = viking (olha o samurai), 2 = samurai (olha o cavaleiro)
    private static final Config[] ELENCO = {
            new Config("cena_cavaleiro", -2.8, -3.0, 1.95, -3.31, 7.26, 0.0, 0.0, 1),
            new Config("cena_viking",     0.2,  1.0, 2.05,  0.02, 2.04, 0.0, -0.04, 2),
            new Config("cena_samurai",    1.4, -3.0, 1.85,  1.60, 188.9, 0.0, 0.0, 0)
    };

    // ------------------------------------------------------------------
    // Instância
    // ------------------------------------------------------------------

    private final List<ModeloAnimadoFx> modelos = new ArrayList<>();
    private final Random acaso = new Random();
    private final AmbientLight ambiente = new AmbientLight(AMBIENTE_BASE);
    private final PointLight luzFria = new PointLight(LUZ_FRIA_BASE);
    private final PointLight luzQuente = new PointLight(LUZ_QUENTE_BASE);
    private final Translate posCamera = new Translate(CAMERA_X, -CAMERA_Y, -CAMERA_Z);
    private final Region clarao = new Region();
    private final Canvas chuva = new Canvas();
    private final AnimationTimer laco;
    private boolean ligada;

    // gotas: x, y, velocidade (px/s), comprimento, camada (0 longe .. 2 perto)
    private final double[][] gotas = new double[N_GOTAS][5];

    // relâmpago
    private long inicioRaio = -1;       // nanoTime do início do relâmpago em curso (ou -1)
    private long proximoRaio = -1;
    private long trovaoEm = -1;         // nanoTime em que o trovão deve tocar (ou -1)

    /** Cria a cena, ou devolve null se não for possível. Chamar na thread do JavaFX. */
    public static CenaBatalha criar() {
        try {
            if (!Platform.isSupported(ConditionalFeature.SCENE3D)) return null;
            return new CenaBatalha();
        } catch (Throwable t) {
            System.err.println("[CenaBatalha] Não foi possível criar a cena 3D: " + t);
            return null;
        }
    }

    private CenaBatalha() throws Exception {
        setMouseTransparent(true);
        setMinSize(0, 0);
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        // ---------- mundo (Y para cima) ----------
        Group mundo = new Group();
        Group campo = montarCampo();
        if (campo == null) throw new IllegalStateException("campo.wbm indisponível");
        mundo.getChildren().add(campo);

        for (int i = 0; i < ELENCO.length; i++) {
            Config c = ELENCO[i];
            ModeloAnimadoFx fx = ModeloAnimadoFx.obter(c.id, "completo");
            if (fx == null) throw new IllegalStateException("modelo indisponível: " + c.id);
            Config alvo = ELENCO[c.alvo];
            double giro = Math.toDegrees(Math.atan2(alvo.x - c.x, alvo.z - c.z));
            double escala = c.altura / c.alturaModelo;
            Group malhas = fx.criarMalhas();
            malhas.getTransforms().addAll(
                    new Translate(c.x, 0, c.z),
                    new Rotate(giro, Rotate.Y_AXIS),
                    new Scale(escala, escala, escala),
                    new Translate(-c.centroX, -c.pesY, -c.centroZ));
            mundo.getChildren().add(criarSombra(c.x, c.z, 1.1));
            mundo.getChildren().add(malhas);
            modelos.add(fx);
        }

        luzFria.setTranslateX(-4);
        luzFria.setTranslateY(7);
        luzFria.setTranslateZ(-5);
        luzQuente.setTranslateX(4);
        luzQuente.setTranslateY(2.5);
        luzQuente.setTranslateZ(4);
        mundo.getChildren().addAll(luzFria, luzQuente);
        mundo.getTransforms().add(new Rotate(180, Rotate.X_AXIS));

        PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.setFieldOfView(CAMERA_FOV);
        camera.setNearClip(0.1);
        camera.setFarClip(400);
        camera.getTransforms().addAll(posCamera, new Rotate(CAMERA_INCLINACAO, Rotate.X_AXIS));

        Group raiz3d = new Group(ambiente, mundo, camera);
        SubScene subScene = new SubScene(raiz3d, 10, 10, true, SceneAntialiasing.BALANCED);
        subScene.setCamera(camera);
        subScene.setFill(Color.TRANSPARENT);
        subScene.setManaged(false);
        subScene.setPickOnBounds(false);
        subScene.widthProperty().bind(widthProperty());
        subScene.heightProperty().bind(heightProperty());

        // ---------- camadas 2D ----------
        Region ceu = camada("-fx-background-color: linear-gradient(to bottom, #11161b 0%, #262e36 36%, "
                + "#505b65 56%, #7b858c 60%, #3a3b37 64%, #1c1c1a 100%);");
        Region neblina = camada("-fx-background-color: linear-gradient(to bottom, rgba(125,135,142,0) 46%, "
                + "rgba(128,138,145,0.50) 59%, rgba(125,135,142,0) 74%);");
        Region vinheta = camada("-fx-background-color: radial-gradient(center 50% 50%, radius 78%, "
                + "rgba(0,0,0,0) 50%, rgba(0,0,0,0.60) 100%);");
        clarao.setMouseTransparent(true);
        clarao.prefWidthProperty().bind(widthProperty());
        clarao.prefHeightProperty().bind(heightProperty());
        clarao.setStyle("-fx-background-color: #e8eeff;");
        clarao.setOpacity(0);
        chuva.setMouseTransparent(true);
        chuva.widthProperty().bind(widthProperty());
        chuva.heightProperty().bind(heightProperty());

        getChildren().addAll(ceu, subScene, neblina, chuva, vinheta, clarao);

        iniciarGotas();
        laco = new AnimationTimer() {
            private long ultimo = -1;

            @Override
            public void handle(long agora) {
                if (ultimo < 0) {
                    ultimo = agora;
                    proximoRaio = agora + 2_500_000_000L;
                }
                double dt = (agora - ultimo) / 1e9;
                ultimo = agora;
                if (dt > 0.1) dt = 0.1;
                desenharChuva(dt);
                atualizarRelampago(agora);
                // a câmera "respira" de leve, como se estivesse na mão
                double t = agora / 1e9;
                posCamera.setX(CAMERA_X + 0.10 * Math.sin(t * 0.37));
                posCamera.setY(-CAMERA_Y + 0.03 * Math.sin(t * 0.53 + 1.0));
            }
        };
    }

    private Region camada(String estilo) {
        Region r = new Region();
        r.setMouseTransparent(true);
        r.prefWidthProperty().bind(widthProperty());
        r.prefHeightProperty().bind(heightProperty());
        r.setStyle(estilo);
        return r;
    }

    // ------------------------------------------------------------------
    // Ciclo de vida
    // ------------------------------------------------------------------

    /** Liga animações, chuva e sons. */
    public void iniciar() {
        if (ligada) return;
        ligada = true;
        for (ModeloAnimadoFx m : modelos) m.ligar();
        laco.start();
        AmbienteSom.iniciar();
    }

    /** Desliga tudo. Necessário ao sair do Menu: timers e sons não param sozinhos. */
    public void parar() {
        if (!ligada) return;
        ligada = false;
        laco.stop();
        for (ModeloAnimadoFx m : modelos) m.desligar();
        AmbienteSom.parar();
    }

    // ------------------------------------------------------------------
    // Chuva
    // ------------------------------------------------------------------

    private void iniciarGotas() {
        for (int i = 0; i < N_GOTAS; i++) {
            double[] g = gotas[i];
            int camada = i % 3;
            g[0] = acaso.nextDouble() * 1600;
            g[1] = acaso.nextDouble() * 900;
            g[4] = camada;
            g[2] = 700 + camada * 380 + acaso.nextDouble() * 220;   // px/s
            g[3] = 10 + camada * 9 + acaso.nextDouble() * 8;        // comprimento
        }
    }

    private void desenharChuva(double dt) {
        double w = chuva.getWidth();
        double h = chuva.getHeight();
        if (w <= 0 || h <= 0) return;
        GraphicsContext gc = chuva.getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);
        final double vento = 0.20; // inclinação: px horizontais por px de queda
        for (int camada = 0; camada < 3; camada++) {
            gc.setStroke(Color.rgb(200, 214, 235, 0.18 + camada * 0.12));
            gc.setLineWidth(0.8 + camada * 0.45);
            gc.beginPath();
            for (int i = camada; i < N_GOTAS; i += 3) {
                double[] g = gotas[i];
                g[1] += g[2] * dt;
                g[0] += g[2] * dt * vento;
                if (g[1] - g[3] > h || g[0] > w + 40) {
                    g[1] = -g[3] - acaso.nextDouble() * 60;
                    g[0] = acaso.nextDouble() * (w + 120) - 120;
                }
                gc.moveTo(g[0], g[1]);
                gc.lineTo(g[0] - g[3] * vento, g[1] - g[3]);
            }
            gc.stroke();
        }
    }

    // ------------------------------------------------------------------
    // Relâmpago e trovão
    // ------------------------------------------------------------------

    private void atualizarRelampago(long agora) {
        if (inicioRaio < 0 && proximoRaio > 0 && agora >= proximoRaio) {
            inicioRaio = agora;
            trovaoEm = agora + (long) ((0.5 + acaso.nextDouble() * 1.4) * 1e9);
        }
        double f = 0;
        if (inicioRaio >= 0) {
            double t = (agora - inicioRaio) / 1e9;
            double a = Math.min(1.0, t / 0.02) * 0.85 * Math.exp(-t * 13.0);
            double t2 = t - 0.22;
            double b = t2 >= 0 ? Math.min(1.0, t2 / 0.03) * Math.exp(-t2 * 5.0) : 0.0;
            f = Math.max(a, b);
            if (t > 2.6) {
                inicioRaio = -1;
                f = 0;
                proximoRaio = agora + (long) ((RAIO_MIN + acaso.nextDouble() * (RAIO_MAX - RAIO_MIN)) * 1e9);
            }
        }
        if (trovaoEm > 0 && agora >= trovaoEm) {
            trovaoEm = -1;
            AmbienteSom.trovao();
        }
        f = Math.max(0.0, Math.min(1.0, f));
        clarao.setOpacity(f * 0.42);
        ambiente.setColor(AMBIENTE_BASE.interpolate(AMBIENTE_RAIO, f));
        luzFria.setColor(LUZ_FRIA_BASE.interpolate(LUZ_FRIA_RAIO, f));
        luzQuente.setColor(LUZ_QUENTE_BASE.interpolate(LUZ_QUENTE_RAIO, f));
    }

    // ------------------------------------------------------------------
    // Campo de batalha (campo.wbm)
    // ------------------------------------------------------------------

    private static Group montarCampo() {
        try (InputStream in = CenaBatalha.class.getResourceAsStream(PASTA + "campo.wbm")) {
            if (in == null) {
                System.err.println("[CenaBatalha] Campo não encontrado: " + PASTA + "campo.wbm");
                return null;
            }
            ByteBuffer bb = ByteBuffer.wrap(in.readAllBytes());   // big-endian
            if (bb.getInt() != 0x57424D31) {
                System.err.println("[CenaBatalha] campo.wbm inválido.");
                return null;
            }
            bb.position(bb.position() + 20);   // cabeçalho de 5 floats (não usado aqui)
            int quantidade = bb.getInt();
            Group campo = new Group();
            for (int i = 0; i < quantidade; i++) {
                int tamanhoNome = bb.getShort() & 0xFFFF;
                byte[] bytesNome = new byte[tamanhoNome];
                bb.get(bytesNome);
                String nome = new String(bytesNome, StandardCharsets.UTF_8);
                int cerca = nome.indexOf('#');
                String base = cerca >= 0 ? nome.substring(0, cerca) : nome;

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

                MeshView vista = new MeshView(montarMalha(vertices, indices));
                vista.setMaterial(materialDoCampo(base));
                vista.setCullFace(CullFace.NONE);
                campo.getChildren().add(vista);
            }
            return campo;
        } catch (Throwable t) {
            System.err.println("[CenaBatalha] Erro ao ler o campo: " + t);
            return null;
        }
    }

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
            faces[i * 3] = indices[i];
            faces[i * 3 + 1] = indices[i];
            faces[i * 3 + 2] = indices[i];
        }
        TriangleMesh malha = new TriangleMesh(VertexFormat.POINT_NORMAL_TEXCOORD);
        malha.getPoints().setAll(pontos);
        malha.getNormals().setAll(normais);
        malha.getTexCoords().setAll(uvs);
        malha.getFaces().setAll(faces);
        return malha;
    }

    private static PhongMaterial materialDoCampo(String base) {
        PhongMaterial m = new PhongMaterial();
        switch (base) {
            case "chao_perto":
            case "chao_longe": {
                m.setDiffuseColor(Color.WHITE);
                URL url = CenaBatalha.class.getResource(PASTA + base + ".jpg");
                if (url != null) m.setDiffuseMap(new Image(url.toExternalForm(), true));
                m.setSpecularColor(Color.gray(0.06));
                m.setSpecularPower(6);
                break;
            }
            case "galhos":
                m.setDiffuseColor(Color.web("#6a5543"));
                m.setSpecularColor(Color.gray(0.05));
                m.setSpecularPower(8);
                break;
            default:   // árvores secas
                m.setDiffuseColor(Color.web("#615c4f"));
                m.setSpecularColor(Color.gray(0.05));
                m.setSpecularPower(8);
                break;
        }
        return m;
    }

    /** Mancha escura no chão sob o personagem (disco com borda suave). Mundo com Y para cima. */
    private static MeshView criarSombra(double cx, double cz, double raio) {
        float y = 0.03f;
        float r = (float) raio;
        TriangleMesh malha = new TriangleMesh();
        malha.getPoints().setAll(
                (float) cx - r, y, (float) cz - r,
                (float) cx + r, y, (float) cz - r,
                (float) cx + r, y, (float) cz + r,
                (float) cx - r, y, (float) cz + r);
        malha.getTexCoords().setAll(0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f);
        malha.getFaces().setAll(
                0, 0, 1, 1, 2, 2,
                0, 0, 2, 2, 3, 3);

        int n = 96;
        WritableImage textura = new WritableImage(n, n);
        PixelWriter pw = textura.getPixelWriter();
        for (int py = 0; py < n; py++) {
            for (int px = 0; px < n; px++) {
                double dx = (px + 0.5) / n * 2.0 - 1.0;
                double dy = (py + 0.5) / n * 2.0 - 1.0;
                double dist = Math.sqrt(dx * dx + dy * dy);
                double alfa = dist >= 1.0 ? 0.0 : Math.pow(1.0 - dist, 1.6) * 0.75;
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
}
