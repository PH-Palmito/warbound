package com.warbound.ui.audio;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Efeitos sonoros contextuais: um som para cada ação do jogo.
 *
 * Os sons são gerados pelo próprio código, em PCM 16 bits, então o projeto
 * não depende de arquivos de áudio nem de licenças. Para trocar um efeito
 * por um arquivo seu, altere o ramo correspondente em {@link #gerar(Som)}.
 *
 * Se não houver dispositivo de áudio disponível, os sons ficam mudos sem
 * lançar erro. As trilhas de fundo ficam em {@link Trilhas}.
 */
public final class Sons {

    /** Efeitos disponíveis. Para adicionar um novo, crie a constante e um ramo em gerar(). */
    public enum Som {
        /** Clique genérico de botão (menu, sala, modais, filtros). */
        CLIQUE,
        /** Estalo leve de papel: uma carta foi selecionada. */
        ESCOLHA_CARTA,
        /** Tique curto: uma vaga do campo foi escolhida como alvo ou origem. */
        SELECIONAR_ALVO,
        /** Papel e sino grave: uma carta foi jogada. */
        JOGAR_CARTA,
        /** Silvo seguido de impacto: uma personagem atacou. */
        ATAQUE,
        /** Baque grave com ruído: uma personagem recebeu dano. */
        DANO,
        /** Arpejo ascendente suave: uma personagem foi curada. */
        CURA,
        /** Tom metálico crescente: uma personagem ganhou proteção. */
        PROTECAO,
        /** Varredura ascendente: uma personagem entrou em campo. */
        ENTRADA,
        /** Queda de tom longa: uma personagem foi derrotada. */
        DERROTA,
        /** Silvo descendente: retirada tática. */
        RETIRADA,
        /** Duas notas descendentes: o turno foi encerrado. */
        ENCERRAR_TURNO,
        /** Sino: começou o turno de um dos jogadores. */
        TURNO
    }

    private static final float TAXA = 44100f;
    private static final AudioFormat FORMATO = new AudioFormat(TAXA, 16, 1, true, false);
    private static final double PICO = 0.8;

    private static final Map<Som, byte[]> AMOSTRAS = new EnumMap<>(Som.class);
    private static final Map<Som, Clip> CLIPS = new EnumMap<>(Som.class);
    private static volatile boolean ativo = true;

    static {
        for (Som som : Som.values()) {
            AMOSTRAS.put(som, converter(normalizar(gerar(som))));
        }
    }

    private Sons() { }

    /** Liga ou desliga todos os efeitos sonoros. */
    public static void setAtivo(boolean valor) {
        ativo = valor;
    }

    /** Toca o efeito a partir do início. Nunca lança exceção. */
    public static synchronized void tocar(Som som) {
        if (!ativo || som == null) return;
        try {
            Clip clip = CLIPS.get(som);
            if (clip == null) {
                clip = abrir(som);
                if (clip == null) return;
                CLIPS.put(som, clip);
            }
            clip.stop();
            clip.setFramePosition(0);
            clip.start();
        } catch (Throwable e) {
            // sem áudio disponível: segue em silêncio (mas avisa uma vez no console)
            avisarUmaVez("não consegui tocar " + som + " -> " + e);
        }
    }

    private static boolean avisou;

    /** Escreve o problema de áudio no console uma única vez, para não inundar o log a cada clique. */
    private static void avisarUmaVez(String mensagem) {
        if (avisou) return;
        avisou = true;
        System.err.println("[Sons] " + mensagem);
        System.err.println("[Sons] Rode Sons.diagnosticar() (já é chamado em Main) e veja as linhas [Audio] acima.");
    }

    /**
     * Imprime no console o que o Java Sound enxerga neste computador: os
     * dispositivos (mixers) e se um Clip 44,1 kHz/16 bits/mono é suportado.
     * Sem nenhum mixer, o Java não tem para onde mandar o som.
     */
    public static void diagnosticar() {
        try {
            javax.sound.sampled.Mixer.Info[] mixers = AudioSystem.getMixerInfo();
            System.out.println("[Audio] Dispositivos do Java Sound: " + mixers.length);
            for (javax.sound.sampled.Mixer.Info info : mixers) {
                System.out.println("[Audio]   - " + info.getName() + " | " + info.getDescription());
            }
            javax.sound.sampled.DataLine.Info linha = new javax.sound.sampled.DataLine.Info(Clip.class, FORMATO);
            System.out.println("[Audio] Clip suportado: " + AudioSystem.isLineSupported(linha));
        } catch (Throwable t) {
            System.out.println("[Audio] Falha ao consultar o Java Sound: " + t);
        }
    }

    private static final Map<String, Clip> CLIPS_PERSONAGEM = new HashMap<>();

    /**
     * Toca o som próprio de uma personagem, pelo id da carta (ex.: "ori_ninja").
     * Ids sem som definido tocam um clique genérico. Nunca lança exceção.
     */
    public static synchronized void tocarPersonagem(String id) {
        if (!ativo || id == null) return;
        try {
            Clip clip = CLIPS_PERSONAGEM.get(id);
            if (clip == null) {
                clip = carregarArquivoPersonagem(id);
                if (clip == null) {
                    byte[] dados = converter(normalizar(gerarPersonagem(id)));
                    clip = AudioSystem.getClip();
                    clip.open(FORMATO, dados, 0, dados.length);
                }
                CLIPS_PERSONAGEM.put(id, clip);
            }
            clip.stop();
            clip.setFramePosition(0);
            clip.start();
        } catch (Throwable e) {
            System.out.println("[Sons] " + id + ": sem saida de audio -> " + e);
        }
    }

    /**
     * Carrega o WAV próprio da personagem em /sons/personagens/{id}.wav.
     * Retorna null se o arquivo não existir ou não puder ser lido (então o som sintetizado é usado).
     */
    private static Clip carregarArquivoPersonagem(String id) {
        String caminho = "/sons/personagens/" + id + ".wav";
        java.net.URL url = Sons.class.getResource(caminho);
        if (url == null) {
            System.out.println("[Sons] " + id + ": arquivo nao encontrado em resources" + caminho + " (usando som gerado)");
            return null;
        }
        try (javax.sound.sampled.AudioInputStream entrada = AudioSystem.getAudioInputStream(url)) {
            Clip clip = AudioSystem.getClip();
            clip.open(entrada);
            System.out.println("[Sons] " + id + ": usando " + caminho + " (" + entrada.getFormat() + ")");
            return clip;
        } catch (Exception e) {
            System.out.println("[Sons] " + id + ": nao consegui ler " + caminho + " -> " + e
                    + " (usando som gerado). Precisa ser WAV PCM.");
            return null;
        }
    }

    private static Clip abrir(Som som) {
        try {
            byte[] dados = AMOSTRAS.get(som);
            Clip clip = AudioSystem.getClip();
            clip.open(FORMATO, dados, 0, dados.length);
            return clip;
        } catch (Throwable e) {
            avisarUmaVez("não consegui abrir o dispositivo de áudio -> " + e);
            return null;
        }
    }

    // ------------------------------------------------------------------
    // Síntese
    // ------------------------------------------------------------------

    private static double[] gerar(Som som) {
        switch (som) {
            case CLIQUE:          return batida(0.09, 900, 0.9);
            case ESCOLHA_CARTA:   return mesclar(couro(0.22, 0.55), 0.0,
                                                 batida(0.03, 1800, 0.2), 0.0);
            case SELECIONAR_ALVO: return metal(0.25, 1400, 0.35);
            case JOGAR_CARTA:     return mesclar(couro(0.20, 0.5), 0.0,
                                                 batida(0.07, 520, 0.7), 0.18);
            case ATAQUE:          return mesclar(vento(0.28, 500, 2400, 0.7), 0.0,
                                                 metal(0.55, 1250, 0.6), 0.22,
                                                 baque(0.12, 160, 50, 0.6), 0.22);
            case DANO:            return mesclar(baque(0.30, 120, 38, 1.0), 0.0,
                                                 correntes(0.26, 0.45), 0.02);
            case CURA:            return sino(1.4, 523, 0.55);
            case PROTECAO:        return metal(0.9, 880, 0.6);
            case ENTRADA:         return mesclar(tambor(0.55, 95, 45, 0.9), 0.0,
                                                 batida(0.04, 240, 0.3), 0.05);
            case DERROTA:         return mesclar(baque(0.25, 150, 45, 0.7), 0.0,
                                                 trompa(0.8, 220, 110, 0.45), 0.05);
            case RETIRADA:        return trompa(0.45, 330, 220, 0.45);
            case ENCERRAR_TURNO:  return mesclar(trompa(0.35, 262, 262, 0.4), 0.0,
                                                 trompa(0.5, 196, 196, 0.4), 0.33);
            case TURNO:           return mesclar(tambor(0.32, 85, 48, 0.9), 0.0,
                                                 tambor(0.32, 85, 48, 0.9), 0.40,
                                                 tambor(0.45, 85, 48, 1.0), 0.80);
            default:              return new double[0];
        }
    }

    /** Som de cada personagem, pelo id da carta. */
    private static double[] gerarPersonagem(String id) {
        switch (id) {
            // Templários: aço, escudo e cavalaria
            case "tpl_escudeiro": return mesclar(batida(0.12, 700, 0.8), 0.0,
                                                 metal(0.35, 620, 0.6), 0.04);
            case "tpl_besteiro":  return mesclar(corda(0.25, 320, 110, 0.8), 0.0,
                                                 vento(0.20, 3000, 1200, 0.4), 0.03);
            case "tpl_cavaleiro": return mesclar(baque(0.12, 110, 45, 0.6), 0.0,
                                                 baque(0.12, 110, 45, 0.6), 0.32,
                                                 correntes(0.20, 0.35), 0.05);
            case "tpl_arqueira":  // arqueira usa o mesmo perfil sonoro do lanceiro
            case "tpl_lanceiro":  return mesclar(vento(0.25, 300, 2200, 0.6), 0.0,
                                                 metal(0.60, 1500, 0.5), 0.22);
            // Orientais: lâmina rápida, passos leves e templo
            case "ori_samurai":   return mesclar(vento(0.22, 2500, 6000, 0.5), 0.0,
                                                 metal(0.50, 2100, 0.55), 0.20);
            case "ori_ronin":     return mesclar(vento(0.20, 1800, 4500, 0.6), 0.0,
                                                 metal(0.35, 1200, 0.5), 0.18,
                                                 baque(0.06, 200, 80, 0.3), 0.18);
            case "ori_ninja":     return mesclar(couro(0.05, 0.25), 0.0,
                                                 couro(0.05, 0.25), 0.22,
                                                 batida(0.03, 1500, 0.1), 0.0);
            case "ori_monge":     return mesclar(batida(0.08, 650, 0.9), 0.0,
                                                 sino(1.2, 330, 0.35), 0.05);
            // Nórdicos: machado, chifre e fúria
            case "nor_berserker": return mesclar(rugido(0.9, 90, 0.9), 0.0,
                                                 tambor(0.5, 110, 50, 0.6), 0.0);
            case "nor_hurcarl":   return mesclar(vento(0.12, 800, 2000, 0.3), 0.0,
                                                 baque(0.12, 160, 60, 0.9), 0.10,
                                                 batida(0.08, 600, 0.7), 0.10);
            case "nor_viking":    return trompa(0.6, 196, 294, 0.5);
            case "nor_jarl":      return mesclar(trompa(0.9, 130, 130, 0.45), 0.0,
                                                 tambor(0.5, 90, 45, 0.7), 0.2);
            default:              return batida(0.09, 900, 0.9);
        }
    }

    /** Corda de arco se soltando: tom que cai rápido, com um estalo inicial. */
    private static double[] corda(double dur, double fIni, double fFim, double vol) {
        int n = (int) (dur * TAXA);
        double[] v = new double[n];
        Random r = new Random(19);
        double fase = 0;
        for (int i = 0; i < n; i++) {
            double t = i / TAXA;
            double f = fFim + (fIni - fFim) * Math.exp(-t * 25);
            fase += 2 * Math.PI * f / TAXA;
            double estalo = (r.nextDouble() * 2 - 1) * Math.exp(-t * 300) * 0.4;
            v[i] = vol * Math.min(1.0, t / 0.001) * (Math.sin(fase) * Math.exp(-t * 9) + estalo);
        }
        return v;
    }

    /** Rugido de guerreiro: dente-de-serra grave com vibrato e rouquidão de ruído filtrado. */
    private static double[] rugido(double dur, double f0, double vol) {
        int n = (int) (dur * TAXA);
        double[] v = new double[n];
        Random r = new Random(29);
        double fase = 0;
        for (int i = 0; i < n; i++) {
            double t = i / TAXA;
            double x = t / dur;
            double f = f0 * (1 + 0.01 * Math.sin(2 * Math.PI * 6 * t));
            fase += 2 * Math.PI * f / TAXA;
            double dente = 0;
            for (int k = 1; k <= 10; k++) dente += Math.sin(k * fase) / k;
            double rouquidao = (r.nextDouble() * 2 - 1);
            double env = Math.min(1.0, t / 0.05) * Math.min(1.0, (dur - t) / 0.2);
            v[i] = vol * env * (0.35 * dente * (0.7 + 0.3 * Math.sin(2 * Math.PI * 9 * t)) + 0.12 * rouquidao) * (1 - 0.2 * x);
        }
        return filtroPassaBaixas(v, 900);
    }

    // ---------- blocos de construção: materiais do mundo medieval ----------

    /** Madeira batida: impulso curto passado por um ressoador (mesa, caneca, escudo de madeira). */
    private static double[] batida(double dur, double freq, double vol) {
        int n = (int) (dur * TAXA);
        double[] v = new double[n];
        Random r = new Random(3);
        for (int i = 0; i < n; i++) {
            double t = i / TAXA;
            v[i] = vol * (r.nextDouble() * 2 - 1) * Math.exp(-t * 180) * 0.6;
        }
        return filtroPassaFaixa(v, freq, 6.0);
    }

    /** Couro e pergaminho: ruído em pequenos grãos de amplitude irregular, passa-faixa de médios. */
    private static double[] couro(double dur, double vol) {
        int n = (int) (dur * TAXA);
        double[] v = new double[n];
        Random r = new Random(11);
        int grao = 0, duracaoGrao = 0;
        double amp = 0;
        for (int i = 0; i < n; i++) {
            double t = i / TAXA;
            if (duracaoGrao <= 0) {
                duracaoGrao = (int) ((0.004 + r.nextDouble() * 0.012) * TAXA);
                amp = r.nextDouble();
            }
            duracaoGrao--;
            double env = Math.min(1.0, t / 0.003) * Math.exp(-t * 9) * (1 - Math.sin(Math.PI * t / dur) * 0.4);
            v[i] = vol * env * amp * (r.nextDouble() * 2 - 1);
        }
        return filtroPassaFaixa(v, 2600, 0.9);
    }

    /** Vento / silvo de lâmina: ruído passa-faixa cuja frequência central sobe ou desce no tempo. */
    private static double[] vento(double dur, double fIni, double fFim, double vol) {
        int n = (int) (dur * TAXA);
        double[] v = new double[n];
        Random r = new Random(23);
        double x1 = 0, x2 = 0, y1 = 0, y2 = 0;
        for (int i = 0; i < n; i++) {
            double x = i / (double) n;
            double fc = fIni * Math.pow(fFim / fIni, x);
            double w0 = 2 * Math.PI * fc / TAXA;
            double alfa = Math.sin(w0) / (2 * 1.4);
            double a0 = 1 + alfa;
            double b0 = alfa / a0, b2 = -alfa / a0;
            double a1 = -2 * Math.cos(w0) / a0, a2 = (1 - alfa) / a0;
            double entrada = r.nextDouble() * 2 - 1;
            double y = b0 * entrada + b2 * x2 - a1 * y1 - a2 * y2;
            x2 = x1; x1 = entrada;
            y2 = y1; y1 = y;
            v[i] = vol * 3.0 * y * Math.sin(Math.PI * x);
        }
        return v;
    }

    /** Metal: parciais não harmônicos com decaimento, como uma espada ou escudo batendo. */
    private static double[] metal(double dur, double base, double vol) {
        double[] razoes = {1.0, 2.76, 5.40, 8.93};
        double[] decaimentos = {7, 10, 15, 22};
        double[] pesos = {1.0, 0.55, 0.28, 0.12};
        int n = (int) (dur * TAXA);
        double[] v = new double[n];
        for (int p = 0; p < razoes.length; p++) {
            double f = base * razoes[p];
            for (int i = 0; i < n; i++) {
                double t = i / TAXA;
                v[i] += vol * pesos[p] * Math.exp(-t * decaimentos[p]) * Math.sin(2 * Math.PI * f * t)
                        * Math.min(1.0, t / 0.001);
            }
        }
        return v;
    }

    /** Sino de igreja: parciais de sino (1, 2, 2.4, 3.0) com decaimento longo. */
    private static double[] sino(double dur, double base, double vol) {
        double[] razoes = {1.0, 2.0, 2.4, 3.0};
        double[] decaimentos = {2.2, 3.0, 4.0, 5.5};
        double[] pesos = {1.0, 0.45, 0.3, 0.18};
        int n = (int) (dur * TAXA);
        double[] v = new double[n];
        for (int p = 0; p < razoes.length; p++) {
            double f = base * razoes[p];
            for (int i = 0; i < n; i++) {
                double t = i / TAXA;
                v[i] += vol * pesos[p] * Math.exp(-t * decaimentos[p]) * Math.sin(2 * Math.PI * f * t)
                        * Math.min(1.0, t / 0.002);
            }
        }
        return v;
    }

    /** Baque corporal: queda rápida de tom grave com ruído de impacto. */
    private static double[] baque(double dur, double fIni, double fFim, double vol) {
        int n = (int) (dur * TAXA);
        double[] v = new double[n];
        Random r = new Random(7);
        double fase = 0;
        for (int i = 0; i < n; i++) {
            double t = i / TAXA;
            double f = fFim + (fIni - fFim) * Math.exp(-t * 22);
            fase += 2 * Math.PI * f / TAXA;
            double corpo = Math.sin(fase) * Math.exp(-t * 11);
            double impacto = (r.nextDouble() * 2 - 1) * Math.exp(-t * 90) * 0.35;
            v[i] = vol * Math.min(1.0, t / 0.002) * (corpo + impacto);
        }
        return v;
    }

    /** Correntes de cota de malha: muitos estalos metálicos curtos e irregulares. */
    private static double[] correntes(double dur, double vol) {
        int n = (int) (dur * TAXA);
        double[] v = new double[n];
        Random r = new Random(41);
        for (int c = 0; c < 26; c++) {
            int inicio = (int) (r.nextDouble() * (n - 0.02 * TAXA));
            double base = 2200 + r.nextDouble() * 2500;
            double amp = vol * (0.3 + r.nextDouble() * 0.7);
            for (int i = 0; i < 0.02 * TAXA && inicio + i < n; i++) {
                double t = i / TAXA;
                v[inicio + i] += amp * Math.exp(-t * 260) * Math.sin(2 * Math.PI * base * t);
            }
        }
        return v;
    }

    /** Tambor de guerra: sweep grave com toque de pele. */
    private static double[] tambor(double dur, double fIni, double fFim, double vol) {
        int n = (int) (dur * TAXA);
        double[] v = new double[n];
        Random r = new Random(5);
        double fase = 0;
        for (int i = 0; i < n; i++) {
            double t = i / TAXA;
            double f = fFim + (fIni - fFim) * Math.exp(-t * 14);
            fase += 2 * Math.PI * f / TAXA;
            double corpo = Math.sin(fase) * Math.exp(-t * 7);
            double pele = (r.nextDouble() * 2 - 1) * Math.exp(-t * 120) * 0.25;
            v[i] = vol * Math.min(1.0, t / 0.002) * (corpo + pele);
        }
        return v;
    }

    /** Trompa: ondas dente-de-serra filtradas, com glissando e vibrato suave. */
    private static double[] trompa(double dur, double fIni, double fFim, double vol) {
        int n = (int) (dur * TAXA);
        double[] v = new double[n];
        double fase = 0;
        for (int i = 0; i < n; i++) {
            double t = i / TAXA;
            double x = t / dur;
            double f = fIni * Math.pow(fFim / fIni, x) * (1 + 0.006 * Math.sin(2 * Math.PI * 5.5 * t));
            fase += 2 * Math.PI * f / TAXA;
            double soma = 0;
            for (int k = 1; k <= 8; k++) {
                soma += Math.sin(k * fase) / Math.pow(k, 1.3);
            }
            double env = Math.min(1.0, t / 0.05) * Math.min(1.0, (dur - t) / 0.08);
            v[i] = vol * env * soma * 0.5;
        }
        return filtroPassaBaixas(v, 1500);
    }

    // ---------- filtros (RBJ biquad) ----------

    private static double[] filtroPassaFaixa(double[] x, double fc, double q) {
        double w0 = 2 * Math.PI * fc / TAXA;
        double alfa = Math.sin(w0) / (2 * q);
        double a0 = 1 + alfa;
        double b0 = alfa / a0, b2 = -alfa / a0;
        double a1 = -2 * Math.cos(w0) / a0, a2 = (1 - alfa) / a0;
        double x1 = 0, x2 = 0, y1 = 0, y2 = 0;
        double[] y = new double[x.length];
        for (int i = 0; i < x.length; i++) {
            double out = b0 * x[i] + b2 * x2 - a1 * y1 - a2 * y2;
            x2 = x1; x1 = x[i];
            y2 = y1; y1 = out;
            y[i] = out;
        }
        return y;
    }

    private static double[] filtroPassaBaixas(double[] x, double fc) {
        double w0 = 2 * Math.PI * fc / TAXA;
        double alfa = Math.sin(w0) / (2 * 0.707);
        double cw = Math.cos(w0);
        double a0 = 1 + alfa;
        double b0 = (1 - cw) / 2 / a0, b1 = (1 - cw) / a0, b2 = b0;
        double a1 = -2 * cw / a0, a2 = (1 - alfa) / a0;
        double x1 = 0, x2 = 0, y1 = 0, y2 = 0;
        double[] y = new double[x.length];
        for (int i = 0; i < x.length; i++) {
            double out = b0 * x[i] + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2;
            x2 = x1; x1 = x[i];
            y2 = y1; y1 = out;
            y[i] = out;
        }
        return y;
    }

    /** Soma vários trechos, cada um começando no seu atraso (em segundos). Recebe pares (trecho, atraso). */
    private static double[] mesclar(Object... partes) {
        int total = 0;
        for (int p = 0; p < partes.length; p += 2) {
            double[] trecho = (double[]) partes[p];
            int inicio = (int) (((Double) partes[p + 1]) * TAXA);
            total = Math.max(total, inicio + trecho.length);
        }
        double[] saida = new double[total];
        for (int p = 0; p < partes.length; p += 2) {
            double[] trecho = (double[]) partes[p];
            int inicio = (int) (((Double) partes[p + 1]) * TAXA);
            for (int i = 0; i < trecho.length; i++) {
                saida[inicio + i] += trecho[i];
            }
        }
        return saida;
    }

    /** Ajusta o pico de cada som para o mesmo nível, evitando distorção. */
    private static double[] normalizar(double[] v) {
        double pico = 0;
        for (double x : v) pico = Math.max(pico, Math.abs(x));
        if (pico == 0) return v;
        double fator = PICO / pico;
        for (int i = 0; i < v.length; i++) v[i] *= fator;
        return v;
    }

    private static byte[] converter(double[] amostras) {
        byte[] bytes = new byte[amostras.length * 2];
        for (int i = 0; i < amostras.length; i++) {
            double x = Math.max(-1.0, Math.min(1.0, amostras[i]));
            short s = (short) Math.round(x * 32767);
            bytes[2 * i] = (byte) (s & 0xff);
            bytes[2 * i + 1] = (byte) ((s >> 8) & 0xff);
        }
        return bytes;
    }
}
