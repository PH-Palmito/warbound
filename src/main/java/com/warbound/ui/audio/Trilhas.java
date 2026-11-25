package com.warbound.ui.audio;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;

import java.net.URL;

/**
 * Trilhas sonoras de fundo, em loop. Há dois espaços reservados para as suas
 * trilhas, um para cada tela:
 *
 *   MENU     -> Menu, Biblioteca de Cartas e Sala de Partida
 *   PARTIDA  -> Tela de Partida
 *
 * Para usar a sua trilha: coloque um arquivo WAV em src/main/resources/sons/
 * com o nome indicado no espaço. Se o arquivo não existir, a tela fica em
 * silêncio, sem erro. Para trocar o nome do arquivo, altere o caminho do
 * espaço desejado abaixo.
 *
 * Formatos suportados pelo Java nativamente: WAV, AIFF e AU (PCM).
 */
public final class Trilhas {

    /** Os dois espaços reservados para trilhas. */
    public enum Trilha {
        /** ESPAÇO 1: trilha do menu e das telas de preparação. */
        MENU("/sons/trilha_menu.wav"),
        /** ESPAÇO 2: trilha da partida. */
        PARTIDA("/sons/trilha_partida.wav");

        private final String caminho;

        Trilha(String caminho) {
            this.caminho = caminho;
        }

        public String getCaminho() {
            return caminho;
        }
    }

    /** Volume da trilha em decibéis (0 = volume original do arquivo). */
    private static final float GANHO_DB = -10f;

    private static Clip atual;
    private static Trilha tocando;

    private Trilhas() { }

    /** Troca para a trilha indicada. Se já estiver tocando, não reinicia. */
    public static synchronized void tocar(Trilha trilha) {
        if (trilha == null || trilha == tocando) return;
        parar();
        tocando = trilha;

        URL url = Trilhas.class.getResource(trilha.getCaminho());
        if (url == null) {
            System.err.println("[Trilhas] arquivo não encontrado: " + trilha.getCaminho());
            return; // espaço ainda vazio: silêncio
        }

        try (AudioInputStream entrada = AudioSystem.getAudioInputStream(url)) {
            Clip clip = AudioSystem.getClip();
            clip.open(entrada);
            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl ganho = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                ganho.setValue(GANHO_DB);
            }
            clip.loop(Clip.LOOP_CONTINUOUSLY);
            atual = clip;
        } catch (Throwable e) {
            // formato não suportado ou sem áudio: segue em silêncio, mas diz o motivo
            System.err.println("[Trilhas] não consegui tocar " + trilha.getCaminho() + " -> " + e);
            atual = null;
        }
    }

    /** Para a trilha atual, se houver. */
    public static synchronized void parar() {
        if (atual != null) {
            atual.stop();
            atual.close();
            atual = null;
        }
        tocando = null;
    }
}
