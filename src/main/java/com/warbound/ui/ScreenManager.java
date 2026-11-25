package com.warbound.ui;

import com.warbound.model.Carta;
import com.warbound.model.Classe;
import com.warbound.remote.GameClient;
import com.warbound.remote.GameClientRmi;
import com.warbound.ui.screens.BibliotecaCartasScreen;
import com.warbound.ui.screens.DetalheCartaScreen;
import com.warbound.ui.screens.MenuScreen;
import com.warbound.ui.screens.PartidaScreen;
import com.warbound.ui.screens.SalaDePartidaScreen;
import com.warbound.ui.audio.Trilhas;
import com.warbound.ui.fx.CenaBatalha;
import com.warbound.ui.fx.Efeitos;
import com.warbound.ui.fx.FundoFloresta;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

/**
 * Navegação entre as quatro telas (Menu, Biblioteca, Sala de Partida,
 * Partida) e ponto único que guarda a sessão atual (servidor, jogador,
 * classe escolhida) e a instância de GameClient usada por todas as telas.
 *
 * As telas usam um adaptador que comunica com o servidor RMI.
 */
public class ScreenManager {

    private final Stage stage;
    private final Scene scene;
    private final FundoFloresta fundo = new FundoFloresta();
    private final StackPane raiz = new StackPane(fundo);
    private final GameClient gameClient = new GameClientRmi();
    private CenaBatalha cenaAtual;

    // dados da sessão atual, preenchidos na Sala de Partida
    private String enderecoServidor = "localhost";
    private String nomeJogador = "";
    private Classe classeEscolhida = Classe.TEMPLARIOS;

    public ScreenManager(Stage stage) {
        this.stage = stage;
        this.scene = new Scene(raiz, 1280, 720);
        this.scene.setFill(Color.web("#070b0b"));
        this.scene.getStylesheets().add(
                getClass().getResource("/css/styles.css").toExternalForm());
        stage.setScene(scene);
        stage.setMinWidth(1280);
        stage.setMinHeight(720);
    }

    public void mostrarMenu() {
        Trilhas.tocar(Trilhas.Trilha.MENU);
        pararCena();
        MenuScreen menu = new MenuScreen(this);
        CenaBatalha cena = CenaBatalha.criar();
        if (cena == null) {
            // sem 3D (ou arquivo faltando): Menu com o fundo de floresta de sempre
            setRoot(menu, 0.0);
            return;
        }
        cenaAtual = cena;
        raiz.getChildren().setAll(cena, menu);
        cena.iniciar();
        Efeitos.aparecer(menu, 220);
    }

    /** Desliga a cena de batalha do Menu (animações, chuva e sons), se estiver ligada. */
    private void pararCena() {
        if (cenaAtual != null) {
            cenaAtual.parar();
            cenaAtual = null;
        }
    }

    public void mostrarBiblioteca() {
        Trilhas.tocar(Trilhas.Trilha.MENU);
        setRoot(new BibliotecaCartasScreen(this), 0.30);
    }

    public void mostrarSalaDePartida() {
        Trilhas.tocar(Trilhas.Trilha.MENU);
        setRoot(new SalaDePartidaScreen(this), 0.26);
    }

    public void mostrarDetalheCarta(Carta carta) {
        Trilhas.tocar(Trilhas.Trilha.MENU);
        setRoot(new DetalheCartaScreen(this, carta), 0.18);
    }

    public void mostrarPartida() {
        Trilhas.tocar(Trilhas.Trilha.PARTIDA);
        setRoot(new PartidaScreen(this), 0.30);
    }

    /** Troca a tela mantendo o mesmo fundo animado por baixo; escurecimento maior = mais leitura. */
    private void setRoot(Parent tela, double escurecimentoDoFundo) {
        pararCena();
        raiz.getChildren().setAll(fundo, tela);
        fundo.setEscurecimento(escurecimentoDoFundo);
        Efeitos.aparecer(tela, 220);
    }

    public GameClient getGameClient() { return gameClient; }

    public String getEnderecoServidor() { return enderecoServidor; }
    public void setEnderecoServidor(String enderecoServidor) { this.enderecoServidor = enderecoServidor; }

    public String getNomeJogador() { return nomeJogador; }
    public void setNomeJogador(String nomeJogador) { this.nomeJogador = nomeJogador; }

    public Classe getClasseEscolhida() { return classeEscolhida; }
    public void setClasseEscolhida(Classe classeEscolhida) { this.classeEscolhida = classeEscolhida; }
}
