package com.warbound.ui.audio;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;

import java.net.URL;
import java.util.Random;

/**
 * Sons da cena de batalha do Menu: chuva e vento em laço contínuo e trovões
 * avulsos, disparados junto com os relâmpagos. Os arquivos ficam em
 * /sons/ambiente/ (gerados por tools/gerar_sons_ambiente.py). Se um arquivo
 * faltar ou o áudio não abrir, a cena segue em silêncio.
 */
public final class AmbienteSom {

    private static final float GANHO_CHUVA_DB = -9f;
    private static final float GANHO_VENTO_DB = -12f;
    private static final float GANHO_TROVAO_DB = -3f;
    private static final String[] TROVOES = {
            "/sons/ambiente/trovao1.wav", "/sons/ambiente/trovao2.wav", "/sons/ambiente/trovao3.wav" };

    private static final Random ACASO = new Random();
    private static Clip chuva;
    private static Clip vento;

    private AmbienteSom() { }

    /** Liga a chuva e o vento (se já estiverem ligados, não faz nada). */
    public static synchronized void iniciar() {
        if (chuva == null) chuva = abrir("/sons/ambiente/chuva.wav", GANHO_CHUVA_DB, true);
        if (vento == null) vento = abrir("/sons/ambiente/vento.wav", GANHO_VENTO_DB, true);
    }

    /** Um trovão sorteado, tocado uma vez. */
    public static synchronized void trovao() {
        final Clip clip = abrir(TROVOES[ACASO.nextInt(TROVOES.length)], GANHO_TROVAO_DB, false);
        if (clip == null) return;
        clip.addLineListener(evento -> {
            if (evento.getType() == LineEvent.Type.STOP) clip.close();
        });
        clip.start();
    }

    /** Desliga tudo. Chamar ao sair do Menu. */
    public static synchronized void parar() {
        if (chuva != null) { chuva.stop(); chuva.close(); chuva = null; }
        if (vento != null) { vento.stop(); vento.close(); vento = null; }
    }

    private static Clip abrir(String caminho, float ganhoDb, boolean laco) {
        URL url = AmbienteSom.class.getResource(caminho);
        if (url == null) {
            System.err.println("[AmbienteSom] arquivo não encontrado: " + caminho);
            return null;
        }
        try (AudioInputStream entrada = AudioSystem.getAudioInputStream(url)) {
            Clip clip = AudioSystem.getClip();
            clip.open(entrada);
            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl ganho = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                ganho.setValue(ganhoDb);
            }
            if (laco) clip.loop(Clip.LOOP_CONTINUOUSLY);
            return clip;
        } catch (Throwable e) {
            System.err.println("[AmbienteSom] não consegui tocar " + caminho + " -> " + e);
            return null;
        }
    }
}
