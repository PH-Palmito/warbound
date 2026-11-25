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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Busto 3D de um personagem usado como arte dentro de uma CardView.
 *
 * A câmera enquadra do ombro para cima (busto), centralizado no espaço da
 * ilustração da carta. A animação idle (respiração + inclinação suave) roda
 * automaticamente enquanto o componente estiver na cena.
 *
 * O modelo é lido de assets/models/cavaleiro/cavaleiro_pose2.wbm.
 * Use {@link #criar(String, double, double)} para instanciar; devolve null
 * se 3D não estiver disponível, o arquivo não existir ou houver erro de leitura.
 *
 * Personagens sem modelo 3D (sem arquivo de modelo cadastrado) devem usar ImagePlaceholder
 * normalmente — {@link #temModelo(String)} retorna false para eles.
 */
public final class PersonagemView extends Pane {

    // ------------------------------------------------------------------
    // Configuração de modelo por cartaId
    // ------------------------------------------------------------------

    /** Pasta raiz dos modelos. */
    private static final String PASTA_MODELOS  = "/assets/models/";
    /** Pasta de texturas do cavaleiro (modelo compartilhado). */
    private static final String PASTA_CAVALEIRO = "/assets/models/cavaleiro/";

    /**
     * IDs que possuem modelo 3D e qual arquivo .wbm usam.
     * Personagens não listados aqui ficam com PNG (ImagePlaceholder).
     */
    private static final Map<String, String> CAMINHO_WBM = new HashMap<>();
    private static final Map<String, String> PASTA_TEXTURAS = new HashMap<>();

    /**
     * Fração da altura do modelo onde fica o centro da face, por cartaId.
     * Permite enquadrar o busto corretamente para cada personagem.
     * Se não estiver mapeado, usa CENTRO_FACE (0.88) como padrão.
     */
    private static final Map<String, Double> CENTRO_FACE_ID = new HashMap<>();

    /**
     * Personagens estáticos: exibem o modelo na pose original do arquivo,
     * sem animações de respiração, inclinação ou olhar (apenas chama de luz).
     */
    private static final java.util.Set<String> SEM_ANIMACAO_IDLE = new java.util.HashSet<>();

    /** Personagens cujo .wbm tem UVs fora de 0..1 e V invertido (corrigidos em montarMalha). */
    private static final java.util.Set<String> CORRIGE_UV_UDIM = new java.util.HashSet<>();

    /** Centro X (em unidades do modelo) usado para centralizar o busto; padrão = pivotX do .wbm. */
    private static final Map<String, Double> CENTRO_X_ID = new HashMap<>();

    static {
        // Cavaleiro: malha estática (.wbm), pose única.
        CAMINHO_WBM.put("tpl_cavaleiro", PASTA_CAVALEIRO + "cavaleiro_pose2.wbm");
        PASTA_TEXTURAS.put("tpl_cavaleiro", PASTA_CAVALEIRO);
        // Arqueira: malha estática (.wbm) convertida de GLB — pose de descanso do arquivo original.
        CAMINHO_WBM.put("tpl_arqueira", PASTA_MODELOS + "arqueira/arqueira.wbm");
        PASTA_TEXTURAS.put("tpl_arqueira", PASTA_MODELOS + "arqueira/");
        // Face da arqueira fica a ~90% da altura total (Y=1.31 de 1.46 m de altura).
        CENTRO_FACE_ID.put("tpl_arqueira", 0.90);
        // A arqueira fica parada — sem respiração/inclinação/olhar por código.
        SEM_ANIMACAO_IDLE.add("tpl_arqueira");
        // Arqueira: UVs em blocos UDIM (U até 4, V negativo) e V invertido no .wbm — ver montarMalha.
        CORRIGE_UV_UDIM.add("tpl_arqueira");
        // Arqueira: centraliza o busto pela cabeça (X=0.004), não pelo pivô do corpo (X=0.124,
        // puxado pelo arco e pela aljava), que empurrava o rosto para a esquerda da carta.
        CENTRO_X_ID.put("tpl_arqueira", 0.0042);

        // Lanceiro (id tpl_besteiro): GLB convertido por tools/gerar_lanceiro.py, pose única do arquivo.
        // Sem o arquivo, temModelo() é false e a carta volta a usar templarios_besteiro.png.
        CAMINHO_WBM.put("tpl_besteiro", PASTA_MODELOS + "lanceiro/lanceiro.wbm");
        PASTA_TEXTURAS.put("tpl_besteiro", PASTA_MODELOS + "lanceiro/");
        CENTRO_FACE_ID.put("tpl_besteiro", 0.90);      // rosto a ~90% da altura (Y=1,70 de 1,89)
        SEM_ANIMACAO_IDLE.add("tpl_besteiro");
        // Centraliza o busto pela cabeça (X=0); o pivô do .wbm (X=0,29) é puxado pelo escudo e pela lança.
        CENTRO_X_ID.put("tpl_besteiro", 0.0);
        // UDIM já resolvido no conversor: NÃO entra em CORRIGE_UV_UDIM.

        // Huscarl (id nor_hurcarl): modelo do antigo Berserker (viking.glb, tools/gerar_berserker.py),
        // pose de repouso, parado. Rosto a ~94% da altura e centro X=-0,105 (medidos nesse modelo).
        // (lança e escudo nas costas). Sem o arquivo, temModelo() é false e a carta volta ao PNG.
        CAMINHO_WBM.put("nor_hurcarl", PASTA_MODELOS + "berserker/berserker.wbm");
        PASTA_TEXTURAS.put("nor_hurcarl", PASTA_MODELOS + "berserker/");
        CENTRO_FACE_ID.put("nor_hurcarl", 0.94);
        SEM_ANIMACAO_IDLE.add("nor_hurcarl");
        CENTRO_X_ID.put("nor_hurcarl", -0.105);

        // Jarl (id nor_jarl): GLB mastercraft_mithril_armor_set convertido por tools/gerar_jarl.py,
        // pose de repouso do arquivo. Rosto a ~88% da altura (padrão). UVs já normalizadas no conversor.
        CAMINHO_WBM.put("nor_jarl", PASTA_MODELOS + "jarl/jarl.wbm");
        PASTA_TEXTURAS.put("nor_jarl", PASTA_MODELOS + "jarl/");
        SEM_ANIMACAO_IDLE.add("nor_jarl");
        CENTRO_X_ID.put("nor_jarl", -0.0085);   // após girar 180° em Y (tools/gerar_jarl.py), a cabeça fica em X=-0,0085

        // Viking (id nor_viking): modelo do antigo Huscarl (hurcarl.wbm), pose de repouso, parado.
        CAMINHO_WBM.put("nor_viking", PASTA_MODELOS + "hurcarl/hurcarl.wbm");
        PASTA_TEXTURAS.put("nor_viking", PASTA_MODELOS + "hurcarl/");
        CENTRO_FACE_ID.put("nor_viking", 0.758);      // rosto a ~76% da altura total (ponta da lança conta)
        SEM_ANIMACAO_IDLE.add("nor_viking");
        CENTRO_X_ID.put("nor_viking", 0.0);           // cabeça centrada em X=0
    }

    // ------------------------------------------------------------------
    // Câmera — enquadramento de busto (ombro para cima)
    // ------------------------------------------------------------------

    /** Campo de visão vertical estreito = zoom no busto. */
    private static final double FOV           = 18.0;
    /** Distância da câmera ao centro da cena. */
    private static final double DISTANCIA_CAM = 10.0;
    /**
     * Fração da altura do modelo onde fica o centro da face (0 = base, 1 = topo).
     * Com o modelo do cavaleiro a face está a ~88% da altura total.
     */
    private static final double CENTRO_FACE   = 0.88;

    /** Rotação Y de repouso (graus) e amplitude do olhar idle. */
    private static final double OLHAR_BASE      = 6.0;
    private static final double OLHAR_AMPLITUDE = 4.0;

    // ------------------------------------------------------------------
    // Cache — o mesmo .wbm não é relido a cada CartaView
    // ------------------------------------------------------------------

    private static final class Parte {
        final String nome;
        final TriangleMesh malha;
        final PhongMaterial material;
        Parte(String nome, TriangleMesh malha, PhongMaterial material) {
            this.nome = nome; this.malha = malha; this.material = material;
        }
    }

    private static final class Modelo {
        float minY, maxY, pivotX, pivotZ, quadrilY;
        final List<Parte> partes = new ArrayList<>();
    }

    /** Cache por cartaId para não reler o .wbm a cada instância. */
    private static final Map<String, Modelo>  CACHE   = new HashMap<>();
    private static final Map<String, Boolean> TENTOU  = new HashMap<>();

    private final List<Timeline> animacoes = new ArrayList<>();

    // ------------------------------------------------------------------
    // API pública
    // ------------------------------------------------------------------

    /**
     * Verifica se existe modelo 3D cadastrado para este cartaId.
     * Resultado em cache: seguro chamar várias vezes.
     */
    public static boolean temModelo(String cartaId) {
        if (ModeloAnimadoFx.temModelo(cartaId)) return true;   // ex.: Monge (animação própria)
        if (!CAMINHO_WBM.containsKey(cartaId)) return false;
        String caminho = CAMINHO_WBM.get(cartaId);
        return PersonagemView.class.getResource(caminho) != null;
    }

    /**
     * Cria o busto 3D, ou devolve null se 3D não estiver disponível, o
     * cartaId não tiver modelo ou o arquivo não puder ser carregado.
     * Deve ser chamado na thread do JavaFX.
     */
    public static PersonagemView criar(String cartaId, double largura, double altura) {
        try {
            if (!Platform.isSupported(ConditionalFeature.SCENE3D)) return null;
            if (ModeloAnimadoFx.temModelo(cartaId)) {
                ModeloAnimadoFx animado = ModeloAnimadoFx.obter(cartaId, "busto");
                return animado == null ? null : new PersonagemView(animado, largura, altura);
            }
            if (!CAMINHO_WBM.containsKey(cartaId)) return null;
            Modelo modelo = carregarModelo(cartaId);
            if (modelo == null) return null;
            return new PersonagemView(modelo, cartaId, largura, altura);
        } catch (Throwable t) {
            System.err.println("[PersonagemView] Erro ao criar busto de " + cartaId + ": " + t);
            return null;
        }
    }

    // ------------------------------------------------------------------
    // Construtor
    // ------------------------------------------------------------------

    private PersonagemView(Modelo modelo, String cartaId, double largura, double altura) {
        setPrefSize(largura, altura);
        setMinSize(largura, altura);
        setMaxSize(largura, altura);
        setMouseTransparent(true);

        double alturaModelo = modelo.maxY - modelo.minY;

        // Escala: o busto (30% superior do modelo) deve preencher a altura
        // visível pela câmera. alturaVisível = 2 * dist * tan(FOV/2).
        double alturaVisivel = 2.0 * DISTANCIA_CAM * Math.tan(Math.toRadians(FOV / 2.0));
        double escala = alturaVisivel / (alturaModelo * 0.30);

        // Posição Y da face — fração por personagem, 0.88 como padrão (cavaleiro).
        double centroFace = CENTRO_FACE_ID.getOrDefault(cartaId, CENTRO_FACE);
        double yFaceModelo = modelo.minY + alturaModelo * centroFace;
        double focoY = -(yFaceModelo * escala);

        boolean staticIdle = SEM_ANIMACAO_IDLE.contains(cartaId);

        Rotate    giroDoOlhar = new Rotate(OLHAR_BASE, Rotate.Y_AXIS);
        Translate bobTronco   = new Translate(0, 0, 0);
        Rotate    inclinacao  = new Rotate(0, modelo.pivotX, modelo.quadrilY, modelo.pivotZ, Rotate.Z_AXIS);
        Scale     respiracao  = new Scale(1, 1, 1, modelo.pivotX, modelo.quadrilY, modelo.pivotZ);

        Group pernas = new Group();
        Group tronco = new Group();
        for (Parte p : modelo.partes) {
            MeshView mv = new MeshView(p.malha);
            // Para o cavaleiro, "Head" é o interior vazio do capacete — fica preto.
            // Para outros personagens (ex.: arqueira) a textura do rosto é usada.
            if ("Head".equals(p.nome) && "tpl_cavaleiro".equals(cartaId)) {
                PhongMaterial preto = new PhongMaterial();
                preto.setDiffuseColor(Color.BLACK);
                preto.setSpecularColor(Color.BLACK);
                preto.setSpecularPower(1);
                mv.setMaterial(preto);
            } else {
                mv.setMaterial(p.material);
            }
            mv.setCullFace(CullFace.NONE);
            if ("Legs".equals(p.nome)) pernas.getChildren().add(mv);
            else                        tronco.getChildren().add(mv);
        }

        // Personagens estáticos usam um grupo simples; animados empilham transforms idle.
        Group raizPersonagem;
        if (staticIdle) {
            // Sem respiração/inclinação — modelo na pose original do arquivo.
            Group centrar = new Group(pernas, tronco);
            double centroX = CENTRO_X_ID.getOrDefault(cartaId, (double) modelo.pivotX);
            centrar.getTransforms().add(new Translate(-centroX, 0, -modelo.pivotZ));
            Group escalar = new Group(centrar); escalar.getTransforms().add(new Scale(escala, escala, escala));
            raizPersonagem = new Group(escalar); raizPersonagem.getTransforms().add(new Rotate(180, Rotate.X_AXIS));
        } else {
            Group respira = new Group(tronco);  respira.getTransforms().add(respiracao);
            Group inclina = new Group(respira); inclina.getTransforms().add(inclinacao);
            Group sobe    = new Group(inclina); sobe.getTransforms().add(bobTronco);
            Group centrar = new Group(pernas, sobe);
            centrar.getTransforms().add(new Translate(-modelo.pivotX, 0, -modelo.pivotZ));
            Group olhar   = new Group(centrar); olhar.getTransforms().add(giroDoOlhar);
            Group escalar = new Group(olhar);   escalar.getTransforms().add(new Scale(escala, escala, escala));
            raizPersonagem = new Group(escalar); raizPersonagem.getTransforms().add(new Rotate(180, Rotate.X_AXIS));
            animacoes.add(criarRespiracao(respiracao, bobTronco));
            animacoes.add(criarInclinacao(inclinacao));
            animacoes.add(criarOlhar(giroDoOlhar));
        }

        Group cena = new Group(raizPersonagem);

        // Luzes: ambiente frio + chama quente lateral + contraluz azul
        AmbientLight ambiente = new AmbientLight(Color.web("#4a5560"));
        PointLight chama = new PointLight(Color.web("#ffbf7a"));
        chama.setTranslateX(-8); chama.setTranslateY(-6); chama.setTranslateZ(-8);
        PointLight contraluz = new PointLight(Color.web("#5f8fdc"));
        contraluz.setTranslateX(2); contraluz.setTranslateY(-5); contraluz.setTranslateZ(10);

        // Câmera posicionada para enquadrar o busto
        PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.setFieldOfView(FOV);
        camera.setNearClip(0.1);
        camera.setFarClip(80);
        camera.setTranslateY(focoY);
        camera.setTranslateZ(-DISTANCIA_CAM);

        // Chama de luz animada para todos os personagens (efeito de tocha ambiente)
        animacoes.add(criarChama(chama));

        Group raiz3d = new Group(ambiente, chama, contraluz, cena, camera);
        SubScene sub = new SubScene(raiz3d, largura, altura, true, SceneAntialiasing.BALANCED);
        sub.setCamera(camera);
        sub.setFill(Color.TRANSPARENT);
        sub.setManaged(false);
        sub.setPickOnBounds(false);
        sub.widthProperty().bind(widthProperty());
        sub.heightProperty().bind(heightProperty());
        getChildren().add(sub);

        // Liga/desliga automaticamente com a cena
        sceneProperty().addListener((obs, antiga, nova) -> {
            if (nova != null) animacoes.forEach(Timeline::play);
            else              animacoes.forEach(Timeline::stop);
        });
    }

    /**
     * Busto de um personagem com animação idle própria (esqueleto assado, ex.: Monge).
     *
     * Mesmo enquadramento do cavaleiro: câmera fixa (FOV e distância iguais), rosto no centro
     * da arte e cabeça ocupando a mesma fração do quadro. Os parâmetros (centro e altura do
     * quadro) vêm do próprio modelo, calculados pelo conversor a partir da posição média do
     * rosto durante a animação. A pose vem do FBX; não há respiração/inclinação por código.
     */
    private PersonagemView(ModeloAnimadoFx animado, double largura, double altura) {
        setPrefSize(largura, altura);
        setMinSize(largura, altura);
        setMaxSize(largura, altura);
        setMouseTransparent(true);

        ModeloAnimado.Variante v = animado.variante();

        // alturaVisível = 2 * dist * tan(FOV/2); o quadro do modelo preenche essa altura.
        double alturaVisivel = 2.0 * DISTANCIA_CAM * Math.tan(Math.toRadians(FOV / 2.0));
        double escala = alturaVisivel / v.alturaQuadro;
        double focoY = -(v.centroY * escala);

        Group centrar = new Group(animado.criarMalhas());
        centrar.getTransforms().add(new Translate(-v.centroX, 0, -v.centroZ));
        Group escalar = new Group(centrar); escalar.getTransforms().add(new Scale(escala, escala, escala));
        Group virar   = new Group(escalar); virar.getTransforms().add(new Rotate(180, Rotate.X_AXIS));
        Group cena = new Group(virar);

        AmbientLight ambiente = new AmbientLight(Color.web("#4a5560"));
        PointLight chama = new PointLight(Color.web("#ffbf7a"));
        chama.setTranslateX(-8); chama.setTranslateY(-6); chama.setTranslateZ(-8);
        PointLight contraluz = new PointLight(Color.web("#5f8fdc"));
        contraluz.setTranslateX(2); contraluz.setTranslateY(-5); contraluz.setTranslateZ(10);

        PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.setFieldOfView(FOV);
        camera.setNearClip(0.1);
        camera.setFarClip(80);
        camera.setTranslateY(focoY);
        camera.setTranslateZ(-DISTANCIA_CAM);

        Group raiz3d = new Group(ambiente, chama, contraluz, cena, camera);
        SubScene sub = new SubScene(raiz3d, largura, altura, true, SceneAntialiasing.BALANCED);
        sub.setCamera(camera);
        sub.setFill(Color.TRANSPARENT);
        sub.setManaged(false);
        sub.setPickOnBounds(false);
        sub.widthProperty().bind(widthProperty());
        sub.heightProperty().bind(heightProperty());
        getChildren().add(sub);

        animacoes.add(criarChama(chama));

        // Liga/desliga automaticamente com a cena (o relógio da animação é compartilhado)
        sceneProperty().addListener((obs, antiga, nova) -> {
            if (nova != null) { animacoes.forEach(Timeline::play); animado.ligar(); }
            else              { animacoes.forEach(Timeline::stop); animado.desligar(); }
        });
    }

    // ------------------------------------------------------------------
    // Animações idle
    // ------------------------------------------------------------------

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
                new KeyFrame(Duration.ZERO,          new KeyValue(r.angleProperty(), OLHAR_BASE - OLHAR_AMPLITUDE)),
                new KeyFrame(Duration.seconds(4.2),  new KeyValue(r.angleProperty(), OLHAR_BASE + OLHAR_AMPLITUDE, Interpolator.EASE_BOTH)));
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

    // ------------------------------------------------------------------
    // Leitura do .wbm
    // ------------------------------------------------------------------

    private static Modelo carregarModelo(String cartaId) {
        if (CACHE.containsKey(cartaId)) return CACHE.get(cartaId);
        if (TENTOU.getOrDefault(cartaId, false)) return null;
        TENTOU.put(cartaId, true);

        String caminho      = CAMINHO_WBM.get(cartaId);
        String pastaTextura = PASTA_TEXTURAS.getOrDefault(cartaId,
                PASTA_MODELOS + cartaId + "/");

        try (InputStream in = PersonagemView.class.getResourceAsStream(caminho)) {
            if (in == null) {
                System.err.println("[PersonagemView] Modelo não encontrado: " + caminho);
                return null;
            }
            ByteBuffer bb = ByteBuffer.wrap(in.readAllBytes());
            if (bb.getInt() != 0x57424D31) {
                System.err.println("[PersonagemView] Arquivo inválido: " + caminho);
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

                modelo.partes.add(new Parte(nome,
                        montarMalha(verts, tris, CORRIGE_UV_UDIM.contains(cartaId)),
                        criarMaterial(nome, pastaTextura)));
            }
            CACHE.put(cartaId, modelo);
            return modelo;
        } catch (Throwable t) {
            System.err.println("[PersonagemView] Erro ao ler " + caminho + ": " + t);
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

    private static PhongMaterial criarMaterial(String nome, String pastaTextura) {
        PhongMaterial m = new PhongMaterial();
        // Cores de fallback por peça (usadas quando não há textura)
        switch (nome) {
            case "Armor":  m.setDiffuseColor(Color.web("#aab1bb")); m.setSpecularColor(Color.gray(0.75)); m.setSpecularPower(54); break;
            case "Head":   m.setDiffuseColor(Color.web("#9aa1ab")); m.setSpecularColor(Color.gray(0.75)); m.setSpecularPower(54); break;
            case "Weapon": m.setDiffuseColor(Color.web("#b4bcc8")); m.setSpecularColor(Color.gray(0.85)); m.setSpecularPower(64); break;
            case "Wear":   m.setDiffuseColor(Color.web("#c49a3c")); m.setSpecularColor(Color.gray(0.10)); m.setSpecularPower(12); break;
            case "Hands":  m.setDiffuseColor(Color.web("#7a5232")); m.setSpecularColor(Color.gray(0.18)); m.setSpecularPower(18); break;
            case "Legs":   m.setDiffuseColor(Color.web("#4d3626")); m.setSpecularColor(Color.gray(0.12)); m.setSpecularPower(14); break;
            default:       m.setDiffuseColor(Color.web("#999999")); m.setSpecularColor(Color.gray(0.30)); break;
        }
        String chave = nome.toLowerCase();
        URL difusa = PersonagemView.class.getResource(pastaTextura + "diffuse_" + chave + ".jpg");
        if (difusa == null) difusa = PersonagemView.class.getResource(pastaTextura + "diffuse_" + chave + ".png"); // com alpha (cabelo)
        if (difusa != null) {
            m.setDiffuseColor(Color.WHITE);
            m.setDiffuseMap(new Image(difusa.toExternalForm(), true));
            URL brilho = PersonagemView.class.getResource(pastaTextura + "specular_" + chave + ".png");
            if (brilho != null) { m.setSpecularColor(Color.gray(0.9)); m.setSpecularMap(new Image(brilho.toExternalForm(), true)); }
        }
        URL normal = PersonagemView.class.getResource(pastaTextura + "normal_" + chave + ".png");
        if (normal != null) m.setBumpMap(new Image(normal.toExternalForm(), true));
        return m;
    }
}
