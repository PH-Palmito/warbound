package com.warbound.ui.fx;

import javafx.animation.AnimationTimer;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Cenário comum a todas as telas, desenhado inteiramente por código:
 * céu azul com sol, nuvens à deriva, montanhas, um castelo sobre a colina,
 * floresta verde vibrante em três camadas (com paralaxe suave) e campo florido.
 *
 * Por cima da paisagem rodam os efeitos: raios de sol pulsando, bandeiras
 * ao vento, pássaros, folhas caindo, borboletas e brilhos de pólen.
 *
 * Fica numa única instância dentro do ScreenManager, então a animação não
 * reinicia quando o jogador troca de tela. Nada aqui recebe cliques.
 */
public class FundoFloresta extends Pane {

    /** Sobra lateral das camadas de paisagem, para o balanço da paralaxe. */
    private static final double MARGEM = 60;

    private static final int N_NUVENS = 9;
    private static final int N_FOLHAS = 40;
    private static final int N_BRILHOS = 24;
    private static final int N_PASSAROS = 5;
    private static final int N_BORBOLETAS = 0;
    private static final int N_RAIOS = 6;

    private static final Color[] CORES_FOLHA = {
            Color.web("#5f6844"), Color.web("#556b32"), Color.web("#6a6946"),
            Color.web("#756446"), Color.web("#46602f") };
    private static final Color[] CORES_BORBOLETA = { Color.web("#8a8570") };
    private static final Color[] CORES_BANDEIRA = {
            Color.web("#be6c63"), Color.web("#cdb768"), Color.web("#658dbe") };

    private static final double SOL_X = 0.80;   // posição do sol, em fração da tela
    private static final double SOL_Y = 0.15;

    private static final class Nuvem {
        double x, y, escala, vel, alpha;
        double[][] bolhas;   // dx, dy, raio (em unidades de "escala")
    }

    private final Canvas ceu = new Canvas();
    private final Canvas nuvensCanvas = new Canvas();
    private final Canvas longe = new Canvas();   // montanhas, castelo, mata distante
    private final Canvas meio = new Canvas();    // floresta do meio
    private final Canvas perto = new Canvas();   // floresta da frente e campo
    private final Canvas efeitos = new Canvas(); // raios, bandeiras, bichos, folhas
    private final Region escurecimento = new Region();
    private final Region vinheta = new Region();

    private final Random acaso = new Random(7);
    private final List<Nuvem> nuvens = new ArrayList<>();
    private final List<double[]> bandeiras = new ArrayList<>();   // x, y (em tela), cor

    private final double[][] folhas = new double[N_FOLHAS][8];        // nx, ny, vx, vy, ang, vang, tam, cor
    private final double[][] brilhos = new double[N_BRILHOS][6];      // nx, ny, fase, vel, raio, deriva
    private final double[][] passaros = new double[N_PASSAROS][5];    // nx, ny, vel, fase, escala
    private final double[][] borboletas = new double[N_BORBOLETAS][6];// nx, ny, alvoX, alvoY, fase, cor

    private double larguraDesenhada;
    private double alturaDesenhada;
    private double tempo;
    private long ultimoQuadro;

    private final AnimationTimer relogio = new AnimationTimer() {
        @Override
        public void handle(long agora) {
            if (ultimoQuadro == 0) {
                ultimoQuadro = agora;
                return;
            }
            double dt = (agora - ultimoQuadro) / 1_000_000_000.0;
            ultimoQuadro = agora;
            if (dt > 0.1) dt = 0.1;
            tempo += dt;
            quadro(dt);
        }
    };

    public FundoFloresta() {
        getStyleClass().add("forest-base");
        setMouseTransparent(true);

        escurecimento.getStyleClass().add("forest-dim");
        escurecimento.setOpacity(0.0);
        vinheta.getStyleClass().add("forest-vignette");

        for (javafx.scene.Node n : new javafx.scene.Node[] {
                ceu, nuvensCanvas, longe, meio, perto, efeitos, escurecimento, vinheta }) {
            n.setManaged(false);
            n.setMouseTransparent(true);
        }
        getChildren().addAll(ceu, nuvensCanvas, longe, meio, perto, efeitos, escurecimento, vinheta);

        criarNuvens();
        criarParticulas();

        sceneProperty().addListener((obs, antiga, nova) -> {
            if (nova != null) {
                relogio.start();
            } else {
                relogio.stop();
                ultimoQuadro = 0;
            }
        });
    }

    /** 0 = cenário claro (Menu); valores maiores escurecem para dar leitura às telas de jogo. */
    public void setEscurecimento(double valor) {
        Timeline t = new Timeline(new KeyFrame(Duration.millis(450),
                new KeyValue(escurecimento.opacityProperty(), valor, Interpolator.EASE_BOTH)));
        t.play();
    }

    // ------------------------------------------------------------------ layout

    @Override
    protected void layoutChildren() {
        double w = getWidth();
        double h = getHeight();
        if (w <= 0 || h <= 0) return;

        if (w != larguraDesenhada || h != alturaDesenhada) {
            larguraDesenhada = w;
            alturaDesenhada = h;
            double largura = w + 2 * MARGEM;

            dimensionar(ceu, 0, w, h);
            dimensionar(nuvensCanvas, 0, w, h);
            dimensionar(efeitos, 0, w, h);
            dimensionar(longe, -MARGEM, largura, h);
            dimensionar(meio, -MARGEM, largura, h);
            dimensionar(perto, -MARGEM, largura, h);

            desenharCeu(w, h);
            desenharLonge(largura, h);
            desenharMeio(largura, h);
            desenharPerto(largura, h);
        }
        escurecimento.resizeRelocate(0, 0, w, h);
        vinheta.resizeRelocate(0, 0, w, h);
    }

    private static void dimensionar(Canvas c, double x, double w, double h) {
        c.setLayoutX(x);
        c.setLayoutY(0);
        c.setWidth(w);
        c.setHeight(h);
    }

    // ------------------------------------------------------------ céu e paisagem

    private void desenharCeu(double w, double h) {
        GraphicsContext g = ceu.getGraphicsContext2D();
        g.clearRect(0, 0, w, h);
        g.setFill(new LinearGradient(0, 0, 0, h * 0.72, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.web("#262e33")),
                new Stop(0.40, Color.web("#46535a")),
                new Stop(0.78, Color.web("#7b8483")),
                new Stop(1.0, Color.web("#a29f90"))));
        g.fillRect(0, 0, w, h);

        double sx = w * SOL_X;
        double sy = h * SOL_Y;
        g.setFill(new RadialGradient(0, 0, sx, sy, h * 0.60, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.rgb(205, 196, 170, 0.42)),
                new Stop(0.07, Color.rgb(195, 186, 162, 0.30)),
                new Stop(0.25, Color.rgb(180, 172, 150, 0.10)),
                new Stop(1.0, Color.rgb(180, 172, 150, 0.0))));
        g.fillRect(0, 0, w, h);
        g.setFill(Color.rgb(214, 208, 190, 0.55));
        g.fillOval(sx - h * 0.034, sy - h * 0.034, h * 0.068, h * 0.068);
    }

    private void desenharLonge(double largura, double h) {
        GraphicsContext g = longe.getGraphicsContext2D();
        g.clearRect(0, 0, largura, h);
        double u = h / 720.0;

        cordilheira(g, largura, h, h * 0.56, h * 0.20, 1.3, 0.0040,
                Color.web("#56646b"), Color.web("#78858a"));
        cordilheira(g, largura, h, h * 0.61, h * 0.14, 4.1, 0.0062,
                Color.web("#47565a"), Color.web("#65726f"));

        desenharCastelo(g, largura * 0.66, h * 0.625, u * 0.9);

        floresta(g, largura, h, h * 0.71, h * 0.13, 26 * u,
                Color.web("#3b5340"), Color.web("#2b4234"), 11L);
        g.setFill(new LinearGradient(0, h * 0.58, 0, h * 0.74, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.rgb(140, 148, 142, 0.0)),
                new Stop(1.0, Color.rgb(140, 148, 142, 0.55))));
        g.fillRect(0, h * 0.58, largura, h * 0.16);
    }

    private void desenharMeio(double largura, double h) {
        GraphicsContext g = meio.getGraphicsContext2D();
        g.clearRect(0, 0, largura, h);
        double u = h / 720.0;
        floresta(g, largura, h, h * 0.81, h * 0.19, 44 * u,
                Color.web("#2f4a30"), Color.web("#203726"), 23L);
    }

    private void desenharPerto(double largura, double h) {
        GraphicsContext g = perto.getGraphicsContext2D();
        g.clearRect(0, 0, largura, h);
        double u = h / 720.0;
        Random r = new Random(37L);

        floresta(g, largura, h, h * 0.945, h * 0.27, 78 * u,
                Color.web("#283f27"), Color.web("#162619"), 37L);

        // campo
        g.setFill(new LinearGradient(0, h * 0.94, 0, h, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.web("#3f4f2c")),
                new Stop(1.0, Color.web("#18241a"))));
        g.fillRect(0, h * 0.94, largura, h * 0.06);

        // tufos de grama na borda do campo
        g.setStroke(Color.web("#59693f"));
        g.setLineWidth(Math.max(1.0, 1.4 * u));
        for (double x = 0; x < largura; x += 5 * u + r.nextDouble() * 6 * u) {
            double alt = (6 + r.nextDouble() * 10) * u;
            g.strokeLine(x, h * 0.942, x + (r.nextDouble() - 0.5) * 5 * u, h * 0.942 - alt);
        }

        // flores
        Color[] flores = { Color.web("#8a8570"), Color.web("#6e6a58"), Color.web("#7a6a4a"), Color.web("#5e5a48") };
        for (int i = 0; i < 90; i++) {
            double x = r.nextDouble() * largura;
            double y = h * (0.955 + r.nextDouble() * 0.04);
            double tam = (1.6 + r.nextDouble() * 2.2) * u;
            g.setFill(flores[r.nextInt(flores.length)]);
            g.fillOval(x - tam, y - tam, tam * 2, tam * 2);
            g.setFill(Color.web("#5a4a2a"));
            g.fillOval(x - tam * 0.35, y - tam * 0.35, tam * 0.7, tam * 0.7);
        }

        // moitas nos cantos
        moita(g, largura * 0.03, h * 0.985, 70 * u, r);
        moita(g, largura * 0.97, h * 0.985, 80 * u, r);
        moita(g, largura * 0.50, h * 1.00, 50 * u, r);
    }

    private void moita(GraphicsContext g, double x, double y, double tam, Random r) {
        for (int i = 0; i < 6; i++) {
            double ox = (r.nextDouble() - 0.5) * tam * 1.3;
            double oy = -r.nextDouble() * tam * 0.35;
            double raio = tam * (0.30 + r.nextDouble() * 0.22);
            Color c = Color.web("#60a55a").interpolate(Color.web("#376c3d"), r.nextDouble());
            g.setFill(new RadialGradient(0, 0, x + ox - raio * 0.3, y + oy - raio * 0.35, raio * 1.3, false,
                    CycleMethod.NO_CYCLE, new Stop(0, c.brighter()), new Stop(1, c)));
            g.fillOval(x + ox - raio, y + oy - raio, raio * 2, raio * 2);
        }
    }

    private void cordilheira(GraphicsContext g, double largura, double h, double baseY, double amp,
                             double fase, double freq, Color topo, Color base) {
        int n = 70;
        double[] xs = new double[n + 3];
        double[] ys = new double[n + 3];
        for (int i = 0; i <= n; i++) {
            double x = largura * i / n;
            xs[i] = x;
            ys[i] = baseY - amp * (0.55 + 0.30 * Math.sin(x * freq + fase)
                    + 0.15 * Math.sin(x * freq * 2.7 + fase * 1.7));
        }
        xs[n + 1] = largura;
        ys[n + 1] = h;
        xs[n + 2] = 0;
        ys[n + 2] = h;
        g.setFill(new LinearGradient(0, baseY - amp, 0, baseY, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, topo), new Stop(1.0, base)));
        g.fillPolygon(xs, ys, n + 3);
    }

    /** Faixa de floresta: copas redondas e alguns pinheiros, sobre um chão sólido. */
    private void floresta(GraphicsContext g, double largura, double h, double baseY, double alt,
                          double passo, Color topo, Color base, long semente) {
        Random r = new Random(semente);
        g.setFill(new LinearGradient(0, baseY - alt * 0.2, 0, h, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, base), new Stop(1.0, base.darker())));
        g.fillRect(0, baseY - alt * 0.2, largura, h - baseY + alt * 0.2);

        for (double x = -passo; x < largura + passo; x += passo * (0.6 + r.nextDouble() * 0.8)) {
            double ht = alt * (0.75 + r.nextDouble() * 0.5);
            if (r.nextDouble() < 0.25) {
                // pinheiro
                g.setFill(Color.web("#5b3b20"));
                g.fillRect(x - ht * 0.03, baseY - ht * 0.18, ht * 0.06, ht * 0.2);
                for (int t = 0; t < 3; t++) {
                    double apice = baseY - ht * (0.98 - 0.24 * t);
                    double fundo = apice + ht * 0.36;
                    double meia = ht * (0.20 + 0.06 * t);
                    g.setFill(base.interpolate(topo, 0.30 + 0.20 * t));
                    g.fillPolygon(new double[] { x, x - meia, x + meia },
                            new double[] { apice, fundo, fundo }, 3);
                }
            } else {
                // árvore de copa redonda
                double cy = baseY - ht * 0.58;
                g.setFill(Color.web("#5b3b20"));
                g.fillRect(x - ht * 0.035, baseY - ht * 0.42, ht * 0.07, ht * 0.46);
                for (int k = 0; k < 4; k++) {
                    double ox = (r.nextDouble() - 0.5) * ht * 0.5;
                    double oy = (r.nextDouble() - 0.5) * ht * 0.35;
                    double raio = ht * (0.20 + r.nextDouble() * 0.10);
                    Color c = topo.interpolate(base, r.nextDouble() * 0.7);
                    g.setFill(new RadialGradient(0, 0, x + ox - raio * 0.3, cy + oy - raio * 0.35,
                            raio * 1.25, false, CycleMethod.NO_CYCLE,
                            new Stop(0.0, c.brighter()), new Stop(1.0, c)));
                    g.fillOval(x + ox - raio, cy + oy - raio, raio * 2, raio * 2);
                }
            }
        }
    }

    // ------------------------------------------------------------------ castelo

    private void desenharCastelo(GraphicsContext g, double cx, double gy, double u) {
        bandeiras.clear();
        Color vermelho = Color.web("#9a3128");
        Color ouro = Color.web("#b8942a");
        Color azul = Color.web("#3f6a9a");

        // colina
        g.setFill(new LinearGradient(0, gy - 70 * u, 0, gy + 90 * u, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.web("#8db87a")), new Stop(1.0, Color.web("#528553"))));
        g.fillOval(cx - 340 * u, gy - 70 * u, 680 * u, 170 * u);

        // torres de trás
        torre(g, cx, gy - 10 * u, 104 * u, 240 * u, u, ouro, 86 * u, 1);
        torre(g, cx - 120 * u, gy - 10 * u, 46 * u, 160 * u, u, azul, 56 * u, 2);
        torre(g, cx + 120 * u, gy - 10 * u, 46 * u, 160 * u, u, vermelho, 56 * u, 0);

        // muralha
        double topoMuro = gy - 96 * u;
        g.setFill(new LinearGradient(0, topoMuro, 0, gy + 4 * u, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.web("#dcd7c7")), new Stop(1.0, Color.web("#a19d8d"))));
        g.fillRect(cx - 215 * u, topoMuro, 430 * u, 100 * u);
        g.setFill(Color.web("#d4cfbf"));
        for (double x = cx - 215 * u; x < cx + 203 * u; x += 24 * u) {
            g.fillRect(x, topoMuro - 10 * u, 13 * u, 11 * u);
        }
        g.setStroke(Color.rgb(90, 84, 70, 0.22));
        g.setLineWidth(1);
        for (double y = topoMuro + 12 * u; y < gy; y += 13 * u) {
            g.strokeLine(cx - 215 * u, y, cx + 215 * u, y);
        }

        // portão
        g.setFill(Color.web("#2a2118"));
        g.fillOval(cx - 22 * u, gy - 78 * u, 44 * u, 44 * u);
        g.fillRect(cx - 22 * u, gy - 56 * u, 44 * u, 60 * u);
        g.setStroke(Color.web("#6b5230"));
        g.setLineWidth(Math.max(1.0, 1.5 * u));
        for (int i = -1; i <= 1; i++) {
            g.strokeLine(cx + i * 8 * u, gy - 70 * u, cx + i * 8 * u, gy + 4 * u);
        }

        // torres das pontas, na frente da muralha
        torre(g, cx - 215 * u, gy + 2 * u, 60 * u, 150 * u, u, vermelho, 62 * u, 0);
        torre(g, cx + 215 * u, gy + 2 * u, 60 * u, 150 * u, u, azul, 62 * u, 2);
    }

    private void torre(GraphicsContext g, double x, double baseY, double larg, double alt, double u,
                       Color telhado, double altTelhado, int corBandeira) {
        double topo = baseY - alt;

        g.setFill(new LinearGradient(x - larg / 2, 0, x + larg / 2, 0, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.web("#e8e3d4")),
                new Stop(0.6, Color.web("#c5c0af")),
                new Stop(1.0, Color.web("#8f8b7d"))));
        g.fillRect(x - larg / 2, topo, larg, alt);

        g.setStroke(Color.rgb(90, 84, 70, 0.22));
        g.setLineWidth(1);
        for (double y = topo + 12 * u; y < baseY; y += 13 * u) {
            g.strokeLine(x - larg / 2, y, x + larg / 2, y);
        }

        g.setFill(Color.web("#2a2118"));
        g.fillRect(x - 2.5 * u, topo + alt * 0.22, 5 * u, 18 * u);
        if (alt > 140 * u) {
            g.fillRect(x - 2.5 * u, topo + alt * 0.50, 5 * u, 18 * u);
        }

        g.setFill(Color.web("#a9a493"));
        g.fillRect(x - larg / 2 - 4 * u, topo - 5 * u, larg + 8 * u, 8 * u);

        g.setFill(new LinearGradient(x - larg / 2, 0, x + larg / 2, 0, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, telhado.brighter()), new Stop(1.0, telhado.darker())));
        double baseTelhado = topo - 4 * u;
        g.fillPolygon(new double[] { x - larg / 2 - 6 * u, x + larg / 2 + 6 * u, x },
                new double[] { baseTelhado, baseTelhado, baseTelhado - altTelhado }, 3);

        double ponta = baseTelhado - altTelhado;
        g.setStroke(Color.web("#3b2c1c"));
        g.setLineWidth(Math.max(1.0, 2 * u));
        g.strokeLine(x, ponta, x, ponta - 34 * u);
        bandeiras.add(new double[] { x - MARGEM, ponta - 34 * u, corBandeira, u });
    }

    // ------------------------------------------------------------------ nuvens

    private void criarNuvens() {
        for (int i = 0; i < N_NUVENS; i++) {
            Nuvem c = new Nuvem();
            c.x = acaso.nextDouble();
            c.y = 0.04 + acaso.nextDouble() * 0.34;
            c.escala = 45 + acaso.nextDouble() * 40;
            c.vel = 0.004 + acaso.nextDouble() * 0.009;
            c.alpha = 0.55 + acaso.nextDouble() * 0.40;
            int nb = 6 + acaso.nextInt(4);
            c.bolhas = new double[nb][3];
            for (int j = 0; j < nb; j++) {
                double f = (j / (double) (nb - 1)) * 2 - 1;
                c.bolhas[j][0] = f * 2.6;
                c.bolhas[j][1] = -(1 - f * f) * 0.7 * acaso.nextDouble() + acaso.nextDouble() * 0.15;
                c.bolhas[j][2] = 0.55 + (1 - Math.abs(f)) * 0.5 + acaso.nextDouble() * 0.2;
            }
            nuvens.add(c);
        }
    }

    private void desenharNuvens(double w, double h, double u) {
        GraphicsContext g = nuvensCanvas.getGraphicsContext2D();
        g.clearRect(0, 0, w, h);
        double margem = 300 * u;
        for (Nuvem c : nuvens) {
            double pos = (c.x + tempo * c.vel) % 1.0;
            double cx = pos * (w + 2 * margem) - margem;
            double cy = c.y * h;
            double e = c.escala * u;

            g.setFill(Color.rgb(140, 175, 215, c.alpha * 0.30));
            for (double[] b : c.bolhas) {
                double r = b[2] * e;
                g.fillOval(cx + b[0] * e - r * 1.05, cy + b[1] * e - r * 0.85 + r * 0.30, r * 2.1, r * 1.9);
            }
            g.setFill(Color.rgb(255, 255, 255, c.alpha * 0.30));
            for (double[] b : c.bolhas) {
                double r = b[2] * e * 1.2;
                g.fillOval(cx + b[0] * e - r, cy + b[1] * e - r, r * 2, r * 2);
            }
            g.setFill(Color.rgb(255, 255, 255, c.alpha * 0.75));
            for (double[] b : c.bolhas) {
                double r = b[2] * e * 0.9;
                g.fillOval(cx + b[0] * e - r, cy + b[1] * e - r, r * 2, r * 2);
            }
        }
    }

    // --------------------------------------------------------------- partículas

    private void criarParticulas() {
        for (int i = 0; i < N_FOLHAS; i++) reiniciarFolha(i, true);
        for (int i = 0; i < N_BRILHOS; i++) {
            double[] p = brilhos[i];
            p[0] = acaso.nextDouble();
            p[1] = 0.45 + acaso.nextDouble() * 0.55;
            p[2] = acaso.nextDouble() * Math.PI * 2;
            p[3] = 0.6 + acaso.nextDouble() * 1.4;
            p[4] = 1.0 + acaso.nextDouble() * 1.6;
            p[5] = 0.5 + acaso.nextDouble();
        }
        for (int i = 0; i < N_PASSAROS; i++) {
            double[] p = passaros[i];
            p[0] = acaso.nextDouble();
            p[1] = 0.10 + acaso.nextDouble() * 0.25;
            p[2] = 0.015 + acaso.nextDouble() * 0.02;
            p[3] = acaso.nextDouble() * 6.0;
            p[4] = 0.7 + acaso.nextDouble() * 0.6;
        }
        for (int i = 0; i < N_BORBOLETAS; i++) {
            double[] p = borboletas[i];
            p[0] = 0.1 + acaso.nextDouble() * 0.8;
            p[1] = 0.55 + acaso.nextDouble() * 0.35;
            p[2] = 0.1 + acaso.nextDouble() * 0.8;
            p[3] = 0.55 + acaso.nextDouble() * 0.35;
            p[4] = acaso.nextDouble() * 6.0;
            p[5] = acaso.nextInt(CORES_BORBOLETA.length);
        }
    }

    private void reiniciarFolha(int i, boolean inicial) {
        double[] p = folhas[i];
        p[0] = inicial ? acaso.nextDouble() : -0.05 - acaso.nextDouble() * 0.2;
        p[1] = inicial ? acaso.nextDouble() : -0.05 - acaso.nextDouble() * 0.2;
        if (!inicial && acaso.nextBoolean()) {
            p[0] = acaso.nextDouble();
            p[1] = -0.06;
        }
        p[2] = 22 + acaso.nextDouble() * 30;      // deriva para a direita (px/s)
        p[3] = 26 + acaso.nextDouble() * 32;      // queda (px/s)
        p[4] = acaso.nextDouble() * 360;
        p[5] = (acaso.nextDouble() - 0.5) * 160;
        p[6] = 3.5 + acaso.nextDouble() * 3.5;
        p[7] = acaso.nextInt(CORES_FOLHA.length);
    }

    // ------------------------------------------------------------------- quadro

    private void quadro(double dt) {
        double w = efeitos.getWidth();
        double h = efeitos.getHeight();
        if (w <= 0 || h <= 0) return;
        double u = h / 720.0;

        // paralaxe: as camadas de perto balançam mais que as de longe
        double sobra = MARGEM - 2;
        meio.setTranslateX(Math.max(-sobra, Math.min(sobra, Math.sin(tempo * 0.35) * 5 * u)));
        perto.setTranslateX(Math.max(-sobra, Math.min(sobra, Math.sin(tempo * 0.50 + 1.0) * 11 * u)));

        desenharNuvens(w, h, u);

        GraphicsContext g = efeitos.getGraphicsContext2D();
        g.clearRect(0, 0, w, h);

        desenharRaios(g, w, h);
        desenharBandeiras(g);
        desenharPassaros(g, w, h, u, dt);
        desenharBrilhos(g, w, h, u, dt);
        desenharBorboletas(g, w, h, u, dt);
        desenharFolhas(g, w, h, u, dt);
    }

    private void desenharRaios(GraphicsContext g, double w, double h) {
        double sx = w * SOL_X;
        double sy = h * SOL_Y;
        double comp = h * 1.5;
        for (int i = 0; i < N_RAIOS; i++) {
            double ang = Math.toRadians(104 + i * 11.5 + Math.sin(tempo * 0.25 + i) * 2.0);
            double larg = Math.toRadians(2.2 + (i % 3) * 1.1);
            double alfa = 0.05 + 0.05 * (0.5 + 0.5 * Math.sin(tempo * 0.6 + i * 1.3));
            double ex = sx + Math.cos(ang) * comp;
            double ey = sy + Math.sin(ang) * comp;
            g.setFill(new LinearGradient(sx, sy, ex, ey, false, CycleMethod.NO_CYCLE,
                    new Stop(0.0, Color.rgb(255, 250, 210, alfa * 1.6)),
                    new Stop(1.0, Color.rgb(255, 250, 210, 0.0))));
            g.fillPolygon(
                    new double[] { sx, sx + Math.cos(ang - larg) * comp, sx + Math.cos(ang + larg) * comp },
                    new double[] { sy, sy + Math.sin(ang - larg) * comp, sy + Math.sin(ang + larg) * comp },
                    3);
        }
    }

    private void desenharBandeiras(GraphicsContext g) {
        for (double[] b : bandeiras) {
            double bx = b[0];
            double by = b[1];
            double u = b[3];
            double comp = 28 * u;
            double alt = 16 * u;
            int seg = 6;
            double[] xs = new double[seg * 2 + 2];
            double[] ys = new double[seg * 2 + 2];
            for (int i = 0; i <= seg; i++) {
                double f = (double) i / seg;
                double onda = Math.sin(tempo * 5.0 - f * 4.0 + bx * 0.01) * 3.2 * u * f;
                xs[i] = bx + f * comp;
                ys[i] = by + onda;
                xs[seg * 2 + 1 - i] = bx + f * comp;
                ys[seg * 2 + 1 - i] = by + alt * (1 - f * 0.25) + onda;
            }
            g.setFill(CORES_BANDEIRA[(int) b[2]]);
            g.fillPolygon(xs, ys, xs.length);
        }
    }

    private void desenharPassaros(GraphicsContext g, double w, double h, double u, double dt) {
        g.setStroke(Color.rgb(35, 55, 85, 0.75));
        g.setLineWidth(Math.max(1.0, 1.8 * u));
        for (double[] p : passaros) {
            p[0] += p[2] * dt;
            if (p[0] > 1.05) {
                p[0] = -0.05;
                p[1] = 0.10 + acaso.nextDouble() * 0.25;
            }
            double x = p[0] * w;
            double y = p[1] * h + Math.sin(tempo * 0.8 + p[3]) * 6 * u;
            double s = 9 * u * p[4];
            double bater = Math.sin(tempo * 9 + p[3]) * 4 * u * p[4];
            g.strokeLine(x - s, y - bater, x, y);
            g.strokeLine(x, y, x + s, y - bater);
        }
    }

    private void desenharBrilhos(GraphicsContext g, double w, double h, double u, double dt) {
        for (double[] p : brilhos) {
            p[2] += dt * p[3];
            p[0] += Math.sin(p[2] * 0.7) * 8 * u * p[5] * dt / w;
            p[1] -= 6 * u * p[5] * dt / h;
            if (p[1] < 0.40) {
                p[1] = 1.0;
                p[0] = acaso.nextDouble();
            }
            double a = 0.25 + 0.75 * Math.max(0.0, Math.sin(p[2]));
            double x = p[0] * w;
            double y = p[1] * h;
            double r = p[4] * u;
            g.setFill(Color.rgb(255, 244, 190, a * 0.22));
            g.fillOval(x - r * 4, y - r * 4, r * 8, r * 8);
            g.setFill(Color.rgb(255, 250, 215, a * 0.95));
            g.fillOval(x - r, y - r, r * 2, r * 2);
        }
    }

    private void desenharBorboletas(GraphicsContext g, double w, double h, double u, double dt) {
        for (double[] p : borboletas) {
            double bx = p[0] * w;
            double by = p[1] * h;
            double dx = p[2] * w - bx;
            double dy = p[3] * h - by;
            double d = Math.hypot(dx, dy);
            if (d < 12 * u) {
                p[2] = 0.05 + acaso.nextDouble() * 0.9;
                p[3] = 0.50 + acaso.nextDouble() * 0.42;
            } else {
                bx += dx / d * 48 * u * dt;
                by += dy / d * 48 * u * dt + Math.sin(tempo * 3 + p[4]) * 14 * u * dt;
                p[0] = bx / w;
                p[1] = by / h;
            }
            p[4] += dt * 14;
            double bater = Math.abs(Math.sin(p[4]));
            double asa = 5.5 * u;
            g.setFill(CORES_BORBOLETA[(int) p[5]]);
            g.fillOval(bx - asa * bater - 0.5 * u, by - asa * 0.9, asa * bater, asa * 1.6);
            g.fillOval(bx + 0.5 * u, by - asa * 0.9, asa * bater, asa * 1.6);
            g.setFill(Color.rgb(40, 30, 20));
            g.fillOval(bx - 0.9 * u, by - 3 * u, 1.8 * u, 6 * u);
        }
    }

    private void desenharFolhas(GraphicsContext g, double w, double h, double u, double dt) {
        for (int i = 0; i < N_FOLHAS; i++) {
            double[] p = folhas[i];
            p[0] += (p[2] + Math.sin(tempo * 1.4 + i) * 14) * u * dt / w;
            p[1] += p[3] * u * dt / h;
            p[4] += p[5] * dt;
            if (p[1] > 1.05 || p[0] > 1.08) {
                reiniciarFolha(i, false);
                continue;
            }
            double t = p[6] * u;
            g.save();
            g.translate(p[0] * w, p[1] * h);
            g.rotate(p[4]);
            g.setFill(CORES_FOLHA[(int) p[7]]);
            g.fillOval(-t, -t * 0.5, t * 2, t);
            g.restore();
        }
    }
}
