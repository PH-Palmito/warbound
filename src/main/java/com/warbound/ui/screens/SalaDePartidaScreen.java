package com.warbound.ui.screens;

import com.warbound.model.Classe;
import com.warbound.model.SalaInfo;
import com.warbound.model.StatusPartida;
import com.warbound.ui.ScreenManager;
import com.warbound.ui.components.AppHeader;
import com.warbound.ui.components.FeedbackMessage;
import com.warbound.ui.components.PrimaryButton;
import com.warbound.ui.fx.Efeitos;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/** Sala de criação/entrada RMI; a partida começa quando o segundo jogador entra. */
public class SalaDePartidaScreen extends BorderPane {
    private final ScreenManager screenManager;
    private final TextField campoEndereco = new TextField("localhost");
    private final TextField campoNome = new TextField();
    private final TextField campoCodigo = new TextField();
    private final ListView<SalaInfo> salasAbertas = new ListView<>();
    private final VBox areaEspera = new VBox(14);
    private final HBox conteudoPrincipal;
    private final Label estadoEspera = new Label();
    private final Timeline aguardarPartida = new Timeline(new KeyFrame(Duration.seconds(1), e -> verificarPartida()));
    private final Timeline atualizacaoLobby = new Timeline(new KeyFrame(Duration.seconds(2), e -> atualizarSalasSilenciosamente()));

    public SalaDePartidaScreen(ScreenManager screenManager) {
        this.screenManager = screenManager;
        sceneProperty().addListener((obs, anterior, atual) -> {
            if (atual == null) {
                aguardarPartida.stop();
                atualizacaoLobby.stop();
            } else {
                atualizacaoLobby.play();
            }
        });
        getStyleClass().add("screen-root");
        setTop(new AppHeader("Sala de Partida", screenManager::mostrarMenu));

        campoEndereco.setPrefWidth(230);
        campoNome.setPromptText("Seu nome");
        campoNome.setPrefWidth(230);
        campoCodigo.setPromptText("Código UUID da partida");
        HBox.setHgrow(campoCodigo, Priority.ALWAYS);

        GridPane formulario = new GridPane();
        formulario.setHgap(12);
        formulario.setVgap(12);
        formulario.addRow(0, label("Endereço do servidor"), campoEndereco);
        formulario.addRow(1, label("Nome do jogador"), campoNome);
        formulario.addRow(2, label("Baralho"), label("27 cartas compartilhadas, embaralhadas para cada jogador"));

        PrimaryButton criarPartida = new PrimaryButton("Criar partida", this::aoCriarPartida);
        PrimaryButton entrarComCodigo = new PrimaryButton("Entrar com código", this::aoEntrarComCodigo);
        PrimaryButton atualizarSalas = new PrimaryButton("Atualizar salas", this::atualizarSalas);
        PrimaryButton entrarNaSalaSelecionada = new PrimaryButton("Entrar na sala", this::aoEntrarNaSalaSelecionada);
        HBox linhaCodigo = new HBox(10, campoCodigo, entrarComCodigo);
        linhaCodigo.setAlignment(Pos.CENTER_LEFT);

        areaEspera.setAlignment(Pos.CENTER);
        areaEspera.setVisible(false);
        areaEspera.setManaged(false);

        Label tituloCriar = new Label("Criar partida");
        tituloCriar.getStyleClass().add("section-title");
        VBox painelCriar = new VBox(16, tituloCriar, formulario, criarPartida);
        painelCriar.getStyleClass().add("panel");
        painelCriar.setPrefWidth(430);
        painelCriar.setMinWidth(430);

        Label tituloEntrar = new Label("Salas abertas");
        tituloEntrar.getStyleClass().add("section-title");
        Label dicaSalas = label("Escolha uma partida aguardando o segundo jogador.");
        salasAbertas.setPrefHeight(150);
        salasAbertas.getStyleClass().add("list-view-flat");
        HBox acoesSalas = new HBox(10, atualizarSalas, entrarNaSalaSelecionada);
        Label dicaCodigo = label("Ou cole o código UUID compartilhado por quem criou a partida.");
        dicaCodigo.setWrapText(true);
        VBox painelEntrar = new VBox(12, tituloEntrar, dicaSalas, salasAbertas, acoesSalas, dicaCodigo, linhaCodigo);
        painelEntrar.getStyleClass().add("panel");
        HBox.setHgrow(painelEntrar, Priority.ALWAYS);

        conteudoPrincipal = new HBox(20, painelCriar, painelEntrar);
        conteudoPrincipal.setAlignment(Pos.TOP_CENTER);

        VBox corpo = new VBox(16, conteudoPrincipal, areaEspera);
        corpo.setPadding(new Insets(24, 28, 20, 28));
        setCenter(corpo);
        campoEndereco.focusedProperty().addListener((obs, anterior, atual) -> {
            if (!atual) atualizarSalas();
        });
        atualizacaoLobby.setCycleCount(Timeline.INDEFINITE);
        atualizacaoLobby.play();
    }

    private Label label(String texto) {
        Label l = new Label(texto);
        l.getStyleClass().add("text-secondary");
        return l;
    }

    private void salvarDadosDoJogador() {
        screenManager.setEnderecoServidor(campoEndereco.getText());
        screenManager.setNomeJogador(campoNome.getText());
        screenManager.setClasseEscolhida(Classe.TEMPLARIOS);
    }

    private void aoCriarPartida() {
        salvarDadosDoJogador();
        try {
            String codigo = screenManager.getGameClient().criarSala(
                    campoEndereco.getText(), campoNome.getText(), Classe.TEMPLARIOS);
            mostrarEspera(codigo);
        } catch (RuntimeException exception) {
            mostrarErro(exception);
        }
    }

    private void aoEntrarComCodigo() {
        if (!campoCodigo.getText().isBlank()) entrar(campoCodigo.getText());
    }

    private void aoEntrarNaSalaSelecionada() {
        SalaInfo sala = salasAbertas.getSelectionModel().getSelectedItem();
        if (sala == null) {
            mostrarErro(new IllegalArgumentException("Selecione uma sala aberta."));
            return;
        }
        entrar(sala.getCodigo());
    }

    private void atualizarSalas() {
        try {
            salasAbertas.getItems().setAll(screenManager.getGameClient().listarSalasAbertas(campoEndereco.getText()));
        } catch (RuntimeException exception) {
            mostrarErro(exception);
        }
    }

    private void atualizarSalasSilenciosamente() {
        try {
            salasAbertas.getItems().setAll(screenManager.getGameClient().listarSalasAbertas(campoEndereco.getText()));
        } catch (RuntimeException ignored) {
            // O lobby será atualizado assim que o servidor estiver disponível.
        }
    }

    private void entrar(String codigo) {
        salvarDadosDoJogador();
        try {
            screenManager.getGameClient().entrarNaSala(
                    campoEndereco.getText(), campoNome.getText(), Classe.TEMPLARIOS, codigo);
            screenManager.mostrarPartida();
        } catch (RuntimeException exception) {
            mostrarErro(exception);
        }
    }

    private void mostrarEspera(String codigo) {
        conteudoPrincipal.setVisible(false);
        conteudoPrincipal.setManaged(false);
        areaEspera.setVisible(true);
        areaEspera.setManaged(true);

        Label titulo = new Label("Aguardando o segundo jogador…");
        titulo.getStyleClass().add("wait-title");
        Efeitos.respirar(titulo, 0.45, 900);
        Label dica = label("Compartilhe o código da partida:");
        Label placaCodigo = new Label(codigo);
        placaCodigo.setWrapText(true);
        placaCodigo.getStyleClass().add("code-plate");
        estadoEspera.setText("A partida começa quando o adversário entrar.");
        FeedbackMessage aviso = new FeedbackMessage(
                "O código identifica a partida no servidor.", FeedbackMessage.Tipo.INFO);
        aviso.setMaxWidth(420);

        areaEspera.getChildren().setAll(titulo, dica, placaCodigo, aviso, estadoEspera);
        areaEspera.setPadding(new Insets(60, 0, 0, 0));
        aguardarPartida.setCycleCount(Timeline.INDEFINITE);
        aguardarPartida.play();
        verificarPartida();
    }

    private void verificarPartida() {
        try {
            if (screenManager.getGameClient().obterEstadoDaPartida().getStatus() == StatusPartida.EM_ANDAMENTO) {
                aguardarPartida.stop();
                screenManager.mostrarPartida();
            }
        } catch (RuntimeException exception) {
            estadoEspera.setText(exception.getMessage());
        }
    }

    private void mostrarErro(RuntimeException exception) {
        areaEspera.setVisible(true);
        areaEspera.setManaged(true);
        areaEspera.getChildren().setAll(new FeedbackMessage(exception.getMessage(), FeedbackMessage.Tipo.ERRO));
    }
}
