package com.warbound.ui.fx;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.InflaterInputStream;

/**
 * Modelo 3D com animação por esqueleto "assada" (arquivo .wba), lido sem JavaFX.
 *
 * O JavaFX não faz skinning. Por isso o conversor (tools/gerar_monge.py) calcula, para
 * cada quadro da animação, a matriz de pele 3x4 de cada osso; aqui deformamos os vértices
 * na CPU a cada quadro com {@link #deformar}. Cada vértice tem até 4 ossos com pesos.
 *
 * Formato (big-endian, depois de "WBA1" + fluxo zlib) documentado em tools/gerar_monge.py.
 * Um arquivo traz duas variantes da malha ("busto" e "completo") que compartilham o
 * esqueleto e a animação; {@link #ler} carrega só a variante pedida.
 */
public final class ModeloAnimado {

    /** Arquivos de textura de um material (nome relativo à pasta; "" se não houver). */
    public static final class Material {
        public final String chave, difusa, normal, especular;
        Material(String chave, String difusa, String normal, String especular) {
            this.chave = chave; this.difusa = difusa; this.normal = normal; this.especular = especular;
        }
    }

    /** Uma peça da malha, com os dados de repouso e de skinning. */
    public static final class Parte {
        public final String nome;
        public final String material;
        public final int nVertices;
        /** x,y,z, nx,ny,nz, u,v por vértice (pose de repouso, Y para cima). */
        final float[] repouso;
        /** 4 índices de osso (0..255) por vértice. */
        final byte[] ossos;
        /** 4 pesos por vértice (somam 1). */
        final float[] pesos;
        /** 3 índices por triângulo. */
        public final int[] triangulos;
        /** u,v por vértice (para o TriangleMesh). */
        public final float[] uvs;

        Parte(String nome, String material, int n, float[] repouso, byte[] ossos, float[] pesos, int[] tris) {
            this.nome = nome; this.material = material; this.nVertices = n;
            this.repouso = repouso; this.ossos = ossos; this.pesos = pesos; this.triangulos = tris;
            this.uvs = new float[n * 2];
            for (int i = 0; i < n; i++) {
                uvs[i * 2]     = repouso[i * 8 + 6];
                uvs[i * 2 + 1] = repouso[i * 8 + 7];
            }
        }
    }

    /** Conjunto de peças + parâmetros de enquadramento (unidades do modelo, Y para cima). */
    public static final class Variante {
        public final String nome;
        /** Ponto para onde a câmera olha: centro do quadro (x, y) e z do rosto/centro. */
        public final float centroX, centroY, centroZ;
        /** Altura (em unidades do modelo) que deve preencher o quadro da câmera. */
        public final float alturaQuadro;
        public final float minY, maxY;
        public final List<Parte> partes;

        Variante(String nome, float cx, float cy, float cz, float altura, float minY, float maxY,
                 List<Parte> partes) {
            this.nome = nome; this.centroX = cx; this.centroY = cy; this.centroZ = cz;
            this.alturaQuadro = altura; this.minY = minY; this.maxY = maxY;
            this.partes = Collections.unmodifiableList(partes);
        }
    }

    public final int fps;
    public final int nQuadros;
    public final int nOssos;
    public final List<Material> materiais;
    public final Variante variante;
    /** [quadro][osso*12 + 0..11]: matriz 3x4 linha a linha. */
    private final float[][] pele;

    private ModeloAnimado(int fps, int nQuadros, int nOssos, float[][] pele,
                          List<Material> materiais, Variante variante) {
        this.fps = fps; this.nQuadros = nQuadros; this.nOssos = nOssos; this.pele = pele;
        this.materiais = Collections.unmodifiableList(materiais);
        this.variante = variante;
    }

    // ------------------------------------------------------------------
    // Leitura
    // ------------------------------------------------------------------

    /** Lê o arquivo e carrega a variante pedida ("busto" ou "completo"). */
    public static ModeloAnimado ler(InputStream entrada, String nomeVariante) throws IOException {
        byte[] todo = entrada.readAllBytes();
        if (todo.length < 5 || todo[0] != 'W' || todo[1] != 'B' || todo[2] != 'A' || todo[3] != '1') {
            throw new IOException("Arquivo .wba inválido");
        }
        byte[] dados;
        try (InflaterInputStream z = new InflaterInputStream(
                new ByteArrayInputStream(todo, 4, todo.length - 4))) {
            dados = z.readAllBytes();
        }
        ByteBuffer bb = ByteBuffer.wrap(dados);   // big-endian (padrão)

        int fps = bb.getInt();
        int nQuadros = bb.getInt();
        int nOssos = bb.getInt();

        float[][] pele = new float[nQuadros][nOssos * 12];
        for (int q = 0; q < nQuadros; q++) lerFloats(bb, pele[q]);

        int nMat = bb.getInt();
        List<Material> materiais = new ArrayList<>();
        for (int i = 0; i < nMat; i++) {
            materiais.add(new Material(lerUtf(bb), lerUtf(bb), lerUtf(bb), lerUtf(bb)));
        }

        int nVar = bb.getInt();
        Variante escolhida = null;
        for (int i = 0; i < nVar; i++) {
            String nome = lerUtf(bb);
            int tamanho = bb.getInt();
            if (!nome.equals(nomeVariante)) {
                bb.position(bb.position() + tamanho);
                continue;
            }
            float cx = bb.getFloat(), cy = bb.getFloat(), cz = bb.getFloat();
            float altura = bb.getFloat(), minY = bb.getFloat(), maxY = bb.getFloat();
            int nPartes = bb.getInt();
            List<Parte> partes = new ArrayList<>();
            for (int p = 0; p < nPartes; p++) partes.add(lerParte(bb));
            escolhida = new Variante(nome, cx, cy, cz, altura, minY, maxY, partes);
        }
        if (escolhida == null) throw new IOException("Variante não encontrada: " + nomeVariante);
        return new ModeloAnimado(fps, nQuadros, nOssos, pele, materiais, escolhida);
    }

    private static Parte lerParte(ByteBuffer bb) {
        String nome = lerUtf(bb);
        String material = lerUtf(bb);
        int n = bb.getInt();
        float[] repouso = new float[n * 8];
        lerFloats(bb, repouso);
        byte[] ossos = new byte[n * 4];
        bb.get(ossos);
        float[] pesos = new float[n * 4];
        lerFloats(bb, pesos);
        int nTris = bb.getInt();
        short[] curtos = new short[nTris * 3];
        ShortBuffer sb = bb.asShortBuffer();
        sb.get(curtos);
        bb.position(bb.position() + curtos.length * 2);
        int[] tris = new int[curtos.length];
        for (int i = 0; i < tris.length; i++) tris[i] = curtos[i] & 0xFFFF;
        return new Parte(nome, material, n, repouso, ossos, pesos, tris);
    }

    private static void lerFloats(ByteBuffer bb, float[] destino) {
        FloatBuffer fb = bb.asFloatBuffer();
        fb.get(destino);
        bb.position(bb.position() + destino.length * 4);
    }

    private static String lerUtf(ByteBuffer bb) {
        int tam = bb.getShort() & 0xFFFF;
        byte[] b = new byte[tam];
        bb.get(b);
        return new String(b, StandardCharsets.UTF_8);
    }

    // ------------------------------------------------------------------
    // Skinning
    // ------------------------------------------------------------------

    /**
     * Calcula posições e normais deformadas de uma peça no quadro dado.
     *
     * @param pontos  saída, 3 floats por vértice (x,y,z)
     * @param normais saída, 3 floats por vértice (nx,ny,nz), normalizadas
     */
    public void deformar(Parte p, int quadro, float[] pontos, float[] normais) {
        final float[] m = pele[quadro];
        final float[] r = p.repouso;
        final float[] w = p.pesos;
        final byte[] os = p.ossos;
        for (int i = 0; i < p.nVertices; i++) {
            final int o = i * 8;
            final float x = r[o], y = r[o + 1], z = r[o + 2];
            final float nx = r[o + 3], ny = r[o + 4], nz = r[o + 5];
            float px = 0, py = 0, pz = 0, qx = 0, qy = 0, qz = 0;
            for (int k = 0; k < 4; k++) {
                final float peso = w[i * 4 + k];
                if (peso == 0f) continue;
                final int b = (os[i * 4 + k] & 0xFF) * 12;
                px += peso * (m[b]     * x + m[b + 1] * y + m[b + 2]  * z + m[b + 3]);
                py += peso * (m[b + 4] * x + m[b + 5] * y + m[b + 6]  * z + m[b + 7]);
                pz += peso * (m[b + 8] * x + m[b + 9] * y + m[b + 10] * z + m[b + 11]);
                qx += peso * (m[b]     * nx + m[b + 1] * ny + m[b + 2]  * nz);
                qy += peso * (m[b + 4] * nx + m[b + 5] * ny + m[b + 6]  * nz);
                qz += peso * (m[b + 8] * nx + m[b + 9] * ny + m[b + 10] * nz);
            }
            pontos[i * 3] = px; pontos[i * 3 + 1] = py; pontos[i * 3 + 2] = pz;
            final float len = (float) Math.sqrt(qx * qx + qy * qy + qz * qz);
            final float inv = len > 1e-12f ? 1f / len : 0f;
            normais[i * 3] = qx * inv; normais[i * 3 + 1] = qy * inv; normais[i * 3 + 2] = qz * inv;
        }
    }
}
