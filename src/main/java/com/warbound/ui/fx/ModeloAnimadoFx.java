package com.warbound.ui.fx;

import javafx.animation.AnimationTimer;
import javafx.scene.Group;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.VertexFormat;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Personagem 3D com animação idle própria (esqueleto assado em .wba), pronto para o JavaFX.
 *
 * Cada variante ("busto" para a carta, "completo" para a tela de detalhes) vira um conjunto
 * de TriangleMesh que é atualizado quadro a quadro por um único relógio. O relógio só roda
 * enquanto houver pelo menos uma tela usando o modelo ({@link #ligar()} / {@link #desligar()}).
 *
 * A mesma instância (e suas malhas) é compartilhada por todas as views da mesma variante,
 * como o cache de modelos estáticos do PersonagemView.
 */
public final class ModeloAnimadoFx {

    // ------------------------------------------------------------------
    // Cadastro: cartaId -> { arquivo .wba, pasta das texturas }
    // ------------------------------------------------------------------

    private static final Map<String, String[]> CADASTRO = new HashMap<>();

    static {
        CADASTRO.put("ori_monge", new String[] {
                "/assets/models/monge/monge_idle.wba",
                "/assets/models/monge/" });
        CADASTRO.put("ori_samurai", new String[] {
                "/assets/models/samurai/samurai_idle.wba",
                "/assets/models/samurai/" });
        // Ninja: GLB do Sketchfab convertido por tools/gerar_ninja.py (idle de 3,3 s, de frente).
        CADASTRO.put("ori_ninja", new String[] {
                "/assets/models/ninja/ninja_idle.wba",
                "/assets/models/ninja/" });
        // Ronin: GLB estático do Sketchfab convertido por tools/gerar_ronin.py (pose fixa, 1 quadro, parado).
        // Se o arquivo faltar ou falhar, temModelo() é false e a carta volta a usar orientais_ronin.png.
        CADASTRO.put("ori_ronin", new String[] {
                "/assets/models/ronin/ronin_idle.wba",
                "/assets/models/ronin/" });
        // Martelador (id tpl_escudeiro): GLB do Sketchfab convertido por tools/gerar_martelador.py
        // (idle de 2,0 s em loop; martelo e adaga acompanham os ossos). Sem o arquivo, usa o PNG.
        CADASTRO.put("tpl_escudeiro", new String[] {
                "/assets/models/martelador/martelador_idle.wba",
                "/assets/models/martelador/" });
        // Modelo do Viking ("XP Viking Ragnar", tools/gerar_viking.py), pose fixa, parado.
        // Trocado: agora pertence ao Berserker (id nor_berserker). Sem o arquivo, a carta volta ao PNG.
        CADASTRO.put("nor_berserker", new String[] {
                "/assets/models/viking/viking_idle.wba",
                "/assets/models/viking/" });
        // Cena de batalha do Menu (CenaBatalha): mesmos modelos de cavaleiro, viking e samurai, mas com
        // esqueleto próprio (tools/gerar_cena_*.py): armas nas mãos e idle de combate em laço de 3 s.
        CADASTRO.put("cena_cavaleiro", new String[] {
                "/assets/models/cavaleiro/cavaleiro_cena.wba",
                "/assets/models/cavaleiro/" });
        CADASTRO.put("cena_viking", new String[] {
                "/assets/models/hurcarl/hurcarl_cena.wba",
                "/assets/models/hurcarl/" });
        CADASTRO.put("cena_samurai", new String[] {
                "/assets/models/samurai/samurai_cena.wba",
                "/assets/models/samurai/" });
    }

    /** Mapas de normal (relevo). Desligue se o desenho ficar pesado em máquinas fracas. */
    private static final boolean USAR_BUMP = true;

    private static final Map<String, ModeloAnimadoFx> CACHE = new HashMap<>();
    private static final Set<String> TENTOU = new HashSet<>();
    /** Texturas compartilhadas entre variantes (a mesma imagem não é carregada duas vezes). */
    private static final Map<String, Image> IMAGENS = new HashMap<>();

    // ------------------------------------------------------------------
    // Estado
    // ------------------------------------------------------------------

    private static final class ParteFx {
        final ModeloAnimado.Parte parte;
        final TriangleMesh malha;
        final PhongMaterial material;
        final float[] pontos;
        final float[] normais;

        ParteFx(ModeloAnimado.Parte parte, TriangleMesh malha, PhongMaterial material) {
            this.parte = parte;
            this.malha = malha;
            this.material = material;
            this.pontos = new float[parte.nVertices * 3];
            this.normais = new float[parte.nVertices * 3];
        }
    }

    private final ModeloAnimado modelo;
    private final List<ParteFx> partes = new ArrayList<>();
    private final AnimationTimer relogio;
    private int usuarios = 0;

    private ModeloAnimadoFx(ModeloAnimado modelo, String pastaTexturas) {
        this.modelo = modelo;

        Map<String, PhongMaterial> materiais = new HashMap<>();
        for (ModeloAnimado.Material m : modelo.materiais) {
            materiais.put(m.chave, criarMaterial(m, pastaTexturas));
        }
        for (ModeloAnimado.Parte p : modelo.variante.partes) {
            PhongMaterial mat = materiais.get(p.material);
            if (mat == null) mat = materiais.values().iterator().next();
            ParteFx f = new ParteFx(p, montarMalha(p), mat);
            // pose do quadro 0 já na criação (senão apareceria a pose de repouso por um instante)
            modelo.deformar(p, 0, f.pontos, f.normais);
            f.malha.getPoints().setAll(f.pontos);
            f.malha.getNormals().setAll(f.normais);
            partes.add(f);
        }

        this.relogio = new AnimationTimer() {
            private long inicio = -1;
            private int ultimo = -1;

            @Override
            public void start() {
                inicio = -1;
                ultimo = -1;
                super.start();
            }

            @Override
            public void handle(long agora) {
                if (inicio < 0) inicio = agora;
                int quadro = (int) (((agora - inicio) * ModeloAnimadoFx.this.modelo.fps / 1_000_000_000L)
                        % ModeloAnimadoFx.this.modelo.nQuadros);
                if (quadro == ultimo) return;
                ultimo = quadro;
                atualizar(quadro);
            }
        };
    }

    // ------------------------------------------------------------------
    // API pública
    // ------------------------------------------------------------------

    /** Existe modelo animado cadastrado (e o arquivo está nos recursos) para este cartaId? */
    public static boolean temModelo(String cartaId) {
        String[] c = CADASTRO.get(cartaId);
        return c != null && ModeloAnimadoFx.class.getResource(c[0]) != null;
    }

    /**
     * Carrega (uma vez) a variante pedida: "busto" ou "completo".
     * Devolve null se não houver modelo ou se a leitura falhar. Chamar na thread do JavaFX.
     */
    public static ModeloAnimadoFx obter(String cartaId, String variante) {
        String chave = cartaId + "#" + variante;
        if (CACHE.containsKey(chave)) return CACHE.get(chave);
        if (!TENTOU.add(chave)) return null;
        String[] c = CADASTRO.get(cartaId);
        if (c == null) return null;
        try (InputStream in = ModeloAnimadoFx.class.getResourceAsStream(c[0])) {
            if (in == null) {
                System.err.println("[ModeloAnimadoFx] Modelo não encontrado: " + c[0]);
                return null;
            }
            ModeloAnimadoFx fx = new ModeloAnimadoFx(ModeloAnimado.ler(in, variante), c[1]);
            CACHE.put(chave, fx);
            return fx;
        } catch (Throwable t) {
            System.err.println("[ModeloAnimadoFx] Erro ao ler " + c[0] + " (" + variante + "): " + t);
            return null;
        }
    }

    /** Parâmetros de enquadramento da variante (centro, altura do quadro, limites). */
    public ModeloAnimado.Variante variante() {
        return modelo.variante;
    }

    /** Novo grupo com uma MeshView por peça; as malhas (e o material) são compartilhadas. */
    public Group criarMalhas() {
        Group g = new Group();
        for (ParteFx f : partes) {
            MeshView mv = new MeshView(f.malha);
            mv.setMaterial(f.material);
            mv.setCullFace(CullFace.NONE);
            g.getChildren().add(mv);
        }
        return g;
    }

    /** Uma view passou a exibir o modelo: liga o relógio se for a primeira. */
    public void ligar() {
        if (usuarios++ == 0) relogio.start();
    }

    /** Uma view saiu da cena: para o relógio quando não sobra ninguém. */
    public void desligar() {
        if (usuarios > 0 && --usuarios == 0) relogio.stop();
    }

    // ------------------------------------------------------------------
    // Internos
    // ------------------------------------------------------------------

    private void atualizar(int quadro) {
        for (ParteFx f : partes) {
            modelo.deformar(f.parte, quadro, f.pontos, f.normais);
            f.malha.getPoints().set(0, f.pontos, 0, f.pontos.length);
            f.malha.getNormals().set(0, f.normais, 0, f.normais.length);
        }
    }

    private static TriangleMesh montarMalha(ModeloAnimado.Parte p) {
        int[] tris = p.triangulos;
        int[] faces = new int[tris.length * 3];
        for (int i = 0; i < tris.length; i++) {
            faces[i * 3] = tris[i];
            faces[i * 3 + 1] = tris[i];
            faces[i * 3 + 2] = tris[i];
        }
        TriangleMesh malha = new TriangleMesh(VertexFormat.POINT_NORMAL_TEXCOORD);
        malha.getTexCoords().setAll(p.uvs);
        malha.getFaces().setAll(faces);
        return malha;
    }

    private static PhongMaterial criarMaterial(ModeloAnimado.Material m, String pasta) {
        PhongMaterial pm = new PhongMaterial();
        pm.setDiffuseColor(Color.web("#999999"));
        pm.setSpecularColor(Color.gray(0.30));
        pm.setSpecularPower("metal".equals(m.chave) || "lanca".equals(m.chave) ? 48 : 28);

        Image difusa = imagem(pasta, m.difusa);
        if (difusa != null) {
            pm.setDiffuseColor(Color.WHITE);
            pm.setDiffuseMap(difusa);
        }
        Image brilho = imagem(pasta, m.especular);
        if (brilho != null) {
            pm.setSpecularColor(Color.gray(0.9));
            pm.setSpecularMap(brilho);
        }
        if (USAR_BUMP) {
            Image normal = imagem(pasta, m.normal);
            if (normal != null) pm.setBumpMap(normal);
        }
        return pm;
    }

    private static Image imagem(String pasta, String arquivo) {
        if (arquivo == null || arquivo.isEmpty()) return null;
        String caminho = pasta + arquivo;
        Image img = IMAGENS.get(caminho);
        if (img == null) {
            URL url = ModeloAnimadoFx.class.getResource(caminho);
            if (url == null) return null;
            img = new Image(url.toExternalForm(), true);
            IMAGENS.put(caminho, img);
        }
        return img;
    }
}
