package com.warbound.ui.screens;

import com.warbound.model.Carta;
import com.warbound.model.EstadoPartidaDTO;
import com.warbound.model.JogadorView;
import com.warbound.model.PersonagemCampoView;
import com.warbound.model.StatusPartida;
import com.warbound.ui.ScreenManager;
import com.warbound.ui.components.AppHeader;
import com.warbound.ui.components.CardView;
import com.warbound.ui.components.CharacterSlot;
import com.warbound.ui.components.FeedbackMessage;
import com.warbound.ui.components.ModalConfirmacao;
import com.warbound.ui.components.PainelJogador;
import com.warbound.ui.components.PrimaryButton;
import com.warbound.ui.components.SecondaryButton;
import com.warbound.ui.audio.Sons;
import com.warbound.ui.components.StatusBadge;
import com.warbound.ui.fx.Efeitos;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Button;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Tela de Partida. Mostra o estado atual (vindo do GameClient) e dispara as
 * ações válidas do turno. Não calcula regra nenhuma: cada botão pede a ação
 * ao GameClient e redesenha com o estado devolvido.
 *
 * Os efeitos de combate nascem da diferença entre o estado anterior e o
 * novo (vida que caiu, personagem que sumiu). A tela só mostra o que o
 * servidor informou; não decide o que aconteceu.
 *
 * Layout pensado para 1280 x 720 sem rolagem:
 *   esquerda: adversário, histórico, jogador local
 *   centro:   campo adversário, mensagens, campo próprio, mão
 *   direita:  turno, reserva, ações
 */
public class PartidaScreen extends StackPane {

    private static final double LARGURA_CARTA_MAO = 104;
    private static final double ALTURA_CARTA_MAO = 144;

    private final ScreenManager screenManager;

    private final Label statusTurno = new Label();
    private final HBox selosGerais = new HBox(6);

    private final VBox espacoAdversario = new VBox();
    private final VBox espacoLocal = new VBox();
    private final ListView<String> historico = new ListView<>();

    private final HBox campoAdversario = new HBox(10);
    private final HBox campoLocal = new HBox(10);
    private final FlowPane reservaLocal = new FlowPane(6, 6);
    private final HBox maoLocal = new HBox(8);
    private final VBox painelEscolha = new VBox(6);

    private final Label dica = new Label();
    private final VBox areaFeedback = new VBox();

    private final PrimaryButton botaoJogarCarta = new PrimaryButton("Jogar carta", this::aoJogarCarta, false);
    private final PrimaryButton botaoAtacar = new PrimaryButton("Atacar", this::aoAtacar, false);
    private final SecondaryButton botaoMuralha = new SecondaryButton("Usar Muralha", this::aoAtivarMuralha, false);
    private final SecondaryButton botaoRetirada = new SecondaryButton("Retirada Tática", this::aoRetiradaTatica, false);
    private final SecondaryButton botaoVoltarReserva = new SecondaryButton("Voltar da Reserva", this::aoVoltarDaReserva, false);
    private final PrimaryButton botaoEncerrarTurno = new PrimaryButton("Encerrar turno", this::aoEncerrarTurno, false);

    private final List<CharacterSlot> slotsLocais = new ArrayList<>();
    private final List<CharacterSlot> slotsAdversarios = new ArrayList<>();

    private EstadoPartidaDTO estado;
    private Carta cartaDaMaoSelecionada;
    private Integer slotProprioSelecionado;
    private Integer slotInimigoSelecionado;
    private String personagemDaReservaSelecionada;
    private Integer origemDaMuralha;
    private final Timeline sincronizacao = new Timeline(new KeyFrame(Duration.seconds(1), e -> atualizarDoServidor()));

    // brilho pulsante do botão "Encerrar turno" enquanto for a vez do jogador
    private final DropShadow brilhoTurno = new DropShadow(16, Color.web("#cdbb93"));
    private final DropShadow sombraBotao = new DropShadow(6, 0, 2, Color.rgb(0, 0, 0, 0.6));
    private final Timeline pulsoTurno = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(brilhoTurno.radiusProperty(), 8.0)),
            new KeyFrame(Duration.millis(900),
                    new KeyValue(brilhoTurno.radiusProperty(), 26.0, Interpolator.EASE_BOTH)));

    public PartidaScreen(ScreenManager screenManager) {
        this.screenManager = screenManager;

        BorderPane conteudo = new BorderPane();
        conteudo.getStyleClass().add("screen-root");
        conteudo.setTop(new AppHeader("Partida", this::aoClicarVoltar));

        // ---------------- coluna esquerda ----------------
        historico.getStyleClass().add("list-view-flat");
        historico.setFocusTraversable(false);
        Label tituloHistorico = new Label("Histórico de ações");
        tituloHistorico.getStyleClass().add("section-label");
        VBox painelHistorico = new VBox(6, tituloHistorico, historico);
        painelHistorico.getStyleClass().add("panel");
        VBox.setVgrow(historico, Priority.ALWAYS);
        VBox.setVgrow(painelHistorico, Priority.ALWAYS);

        VBox esquerda = new VBox(10, espacoAdversario, painelHistorico, espacoLocal);
        esquerda.setPrefWidth(272);
        esquerda.setMinWidth(272);
        esquerda.setMaxWidth(272);

        // ---------------- coluna direita ----------------
        statusTurno.getStyleClass().add("turn-status");
        statusTurno.setWrapText(true);
        selosGerais.setAlignment(Pos.CENTER_LEFT);
        VBox painelTurno = new VBox(8, statusTurno, selosGerais);
        painelTurno.getStyleClass().add("panel");

        botaoEncerrarTurno.getStyleClass().add("end-turn-button");
        for (javafx.scene.control.Control c : new javafx.scene.control.Control[] {
                botaoJogarCarta, botaoAtacar, botaoMuralha, botaoRetirada, botaoVoltarReserva, botaoEncerrarTurno }) {
            c.setMaxWidth(Double.MAX_VALUE);
        }
        painelEscolha.getStyleClass().add("choice-panel");
        VBox painelAcoes = new VBox(8, botaoJogarCarta, botaoAtacar, botaoMuralha, botaoRetirada, botaoVoltarReserva,
                painelEscolha, botaoEncerrarTurno);
        painelAcoes.getStyleClass().add("panel");

        VBox direita = new VBox(10, painelTurno, painelAcoes);
        direita.setPrefWidth(232);
        direita.setMinWidth(232);
        direita.setMaxWidth(232);

        // ---------------- centro: tabuleiro ----------------
        campoAdversario.setAlignment(Pos.CENTER);
        campoLocal.setAlignment(Pos.CENTER);
        maoLocal.setAlignment(Pos.TOP_CENTER);
        maoLocal.setPadding(new Insets(18, 0, 0, 0));
        maoLocal.setMinHeight(160);
        maoLocal.setPrefHeight(160);

        dica.getStyleClass().add("hint-text");
        VBox banda = new VBox(4, dica, areaFeedback);
        banda.setAlignment(Pos.CENTER);
        banda.setMinHeight(50);
        banda.setPrefHeight(50);
        banda.setMaxHeight(50);

        VBox centro = new VBox(8, campoAdversario, banda, campoLocal, maoLocal);
        centro.setAlignment(Pos.TOP_CENTER);
        centro.getStyleClass().add("board-mat");

        conteudo.setLeft(esquerda);
        conteudo.setCenter(centro);
        conteudo.setRight(direita);
        BorderPane.setMargin(centro, new Insets(0, 12, 0, 12));
        conteudo.setPadding(new Insets(10, 14, 10, 14));

        brilhoTurno.setSpread(0.3);
        pulsoTurno.setCycleCount(Timeline.INDEFINITE);
        pulsoTurno.setAutoReverse(true);
        sceneProperty().addListener((obs, antiga, nova) -> {
            if (nova == null) pulsoTurno.stop();
        });

        Efeitos.entrarSuave(esquerda, 60, 450);
        Efeitos.entrarSuave(centro, 180, 500);
        Efeitos.entrarSuave(direita, 300, 450);

        getChildren().add(conteudo);

        estado = screenManager.getGameClient().obterEstadoDaPartida();
        renderizar();

        sincronizacao.setCycleCount(Timeline.INDEFINITE);
        sincronizacao.play();
        sceneProperty().addListener((obs, antiga, nova) -> {
            if (nova == null) {
                sincronizacao.stop();
                pulsoTurno.stop();
            } else {
                sincronizacao.play();
            }
        });

        if (estado.getStatus() == StatusPartida.EM_ANDAMENTO && estado.isTurnoDoJogadorLocal()) {
            Efeitos.depoisDe(350, () -> mostrarFaixaDeTurno(true));
        }
    }

    // ---------------- ações ----------------

    private void aoJogarCarta() {
        if (cartaDaMaoSelecionada == null) return;
        Sons.tocar(Sons.Som.JOGAR_CARTA);
        Integer slotAlvo = slotProprioSelecionado;
        executar(() -> screenManager.getGameClient().jogarCarta(cartaDaMaoSelecionada.getId(), slotAlvo));
    }

    private void aoAtacar() {
        if (slotProprioSelecionado == null || slotInimigoSelecionado == null) return;
        executar(() -> screenManager.getGameClient().atacar(slotProprioSelecionado, slotInimigoSelecionado));
    }

    private void aoRetiradaTatica() {
        if (slotProprioSelecionado == null) return;
        Sons.tocar(Sons.Som.RETIRADA);
        executar(() -> screenManager.getGameClient().retiradaTatica(slotProprioSelecionado));
    }

    private void aoVoltarDaReserva() {
        if (personagemDaReservaSelecionada == null || slotProprioSelecionado == null) return;
        executar(() -> screenManager.getGameClient().retornarDaReserva(personagemDaReservaSelecionada, slotProprioSelecionado));
    }

    private void aoAtivarMuralha() {
        if (slotProprioSelecionado == null) return;
        origemDaMuralha = slotProprioSelecionado;
        personagemDaReservaSelecionada = null;
        cartaDaMaoSelecionada = null;
        renderizar();
    }

    private void aoEncerrarTurno() {
        Sons.tocar(Sons.Som.ENCERRAR_TURNO);
        executar(() -> screenManager.getGameClient().encerrarTurno());
    }

    private void executar(Supplier<EstadoPartidaDTO> acao) {
        try {
            aplicar(acao.get());
        } catch (RuntimeException exception) {
            String mensagem = exception.getMessage() == null ? "Falha ao comunicar com o servidor." : exception.getMessage();
            areaFeedback.getChildren().setAll(new FeedbackMessage(mensagem, FeedbackMessage.Tipo.ERRO));
        }
    }

    private void atualizarDoServidor() {
        try {
            EstadoPartidaDTO atualizado = screenManager.getGameClient().obterEstadoDaPartida();
            if (atualizado.getVersao() != estado.getVersao()
                    || !java.util.Objects.equals(atualizado.getMensagem(), estado.getMensagem())) {
                aplicar(atualizado);
            }
        } catch (RuntimeException exception) {
            // Erros transitórios de rede são tentados novamente na próxima atualização.
        }
    }

    /** Guarda o novo estado, limpa a seleção, redesenha e toca os efeitos da diferença. */
    private void aplicar(EstadoPartidaDTO novo) {
        EstadoPartidaDTO anterior = estado;
        estado = novo;
        limparSelecao();
        renderizar();
        tocarEfeitos(anterior, novo);
    }

    private void aoClicarVoltar() {
        ModalConfirmacao modal = new ModalConfirmacao(
                "Sair da partida?",
                "Você vai voltar para a Sala de Partida. A partida continua no servidor, mas esta tela não retoma a sessão automaticamente.",
                () -> { getChildren().removeIf(n -> n instanceof ModalConfirmacao); screenManager.mostrarSalaDePartida(); },
                () -> getChildren().removeIf(n -> n instanceof ModalConfirmacao));
        getChildren().add(modal);
    }

    private void limparSelecao() {
        cartaDaMaoSelecionada = null;
        slotProprioSelecionado = null;
        slotInimigoSelecionado = null;
        personagemDaReservaSelecionada = null;
        origemDaMuralha = null;
    }

    // ---------------- renderização ----------------

    private void renderizar() {
        boolean meuTurno = estado.isTurnoDoJogadorLocal();
        boolean emAndamento = estado.getStatus() == StatusPartida.EM_ANDAMENTO;

        if (emAndamento) {
            statusTurno.setText(meuTurno ? "Seu turno" : estado.getAdversario().getNome() + " está jogando");
        } else if (estado.getStatus() == StatusPartida.FINALIZADA) {
            statusTurno.setText("Partida finalizada");
        } else {
            statusTurno.setText("Aguardando segundo jogador");
        }

        selosGerais.getChildren().clear();
        if (emAndamento) {
            StatusBadge selo = new StatusBadge(StatusBadge.Tipo.TURNO, meuTurno ? "Seu turno" : "Turno do adversário");
            if (!meuTurno) selo.getStyleClass().add("status-badge-wait");
            selosGerais.getChildren().add(selo);
        }

        espacoAdversario.getChildren().setAll(new PainelJogador(estado.getAdversario(), true));
        espacoLocal.getChildren().setAll(new PainelJogador(estado.getJogadorLocal(), false));

        renderizarCampo(campoAdversario, estado.getAdversario().getCampo(), false, true, slotsAdversarios);
        renderizarCampo(campoLocal, estado.getJogadorLocal().getCampo(), false, false, slotsLocais);
        renderizarCampo(reservaLocal, estado.getJogadorLocal().getReserva(), true, false, new ArrayList<>());

        historico.getItems().setAll(estado.getHistoricoDeAcoesPublicas());
        if (!historico.getItems().isEmpty()) {
            historico.scrollTo(historico.getItems().size() - 1);
        }

        renderizarMao(meuTurno);
        renderizarEscolhaPendente(meuTurno);

        areaFeedback.getChildren().setAll(new FeedbackMessage(estado.getMensagem(), FeedbackMessage.Tipo.INFO));
        dica.setText(textoDaDica(meuTurno, emAndamento));

        PersonagemCampoView proprioSelecionado = personagemNoSlot(estado.getJogadorLocal(), slotProprioSelecionado);
        boolean temProprio = slotProprioSelecionado != null && proprioSelecionado != null;
        boolean vagaLocalSelecionada = slotProprioSelecionado != null && proprioSelecionado == null;
        boolean escolhaPendente = estado.getEscolhaPendente() != null;
        boolean temMao = estado.getJogadorLocal().getMao() != null && !estado.getJogadorLocal().getMao().isEmpty();
        boolean temReserva = estado.getJogadorLocal().getReserva() != null && !estado.getJogadorLocal().getReserva().isEmpty();
        boolean guardiaoSelecionado = temProprio && "Guardião do Bastião".equals(proprioSelecionado.getCarta().getNome());

        boolean podeAtacarAgora = meuTurno && !escolhaPendente && temProprio && proprioSelecionado.isPodeAtacar()
                && slotInimigoSelecionado != null;
        boolean podeJogarAgora = meuTurno && cartaDaMaoSelecionada != null
                && (!cartaDaMaoSelecionada.isPersonagem()
                    || vagaLocalSelecionada);

        botaoJogarCarta.setDisable(escolhaPendente || !podeJogarAgora);
        botaoAtacar.setDisable(!podeAtacarAgora);
        botaoMuralha.setDisable(!meuTurno || escolhaPendente || !temProprio || origemDaMuralha != null);
        botaoMuralha.setText(origemDaMuralha == null ? "Usar Muralha" : "Escolha a aliada");
        botaoRetirada.setDisable(!meuTurno || escolhaPendente || !temProprio);
        botaoVoltarReserva.setDisable(!meuTurno || escolhaPendente || personagemDaReservaSelecionada == null || !vagaLocalSelecionada);
        botaoEncerrarTurno.setDisable(!meuTurno || escolhaPendente);
        mostrarAcao(botaoJogarCarta, emAndamento && meuTurno && temMao);
        mostrarAcao(botaoAtacar, emAndamento && meuTurno && temProprio && proprioSelecionado.isPodeAtacar());
        mostrarAcao(botaoMuralha, emAndamento && meuTurno && (guardiaoSelecionado || origemDaMuralha != null));
        mostrarAcao(botaoRetirada, emAndamento && meuTurno && temProprio);
        mostrarAcao(botaoVoltarReserva, emAndamento && meuTurno && temReserva);
        mostrarAcao(botaoEncerrarTurno, emAndamento && meuTurno);
        if (meuTurno && emAndamento) {
            botaoEncerrarTurno.setEffect(brilhoTurno);
            pulsoTurno.play();
        } else {
            pulsoTurno.stop();
            botaoEncerrarTurno.setEffect(sombraBotao);
        }
    }

    private void renderizarMao(boolean meuTurno) {
        maoLocal.getChildren().clear();
        List<Carta> mao = estado.getJogadorLocal().getMao();
        if (mao == null || mao.isEmpty()) {
            Label vazia = new Label("Sem cartas na mão");
            vazia.getStyleClass().add("hint-text");
            maoLocal.getChildren().add(vazia);
            return;
        }

        int n = mao.size();
        double espacamento = n <= 6 ? 8 : Math.max(-34.0, (680.0 - n * LARGURA_CARTA_MAO) / (n - 1));
        maoLocal.setSpacing(espacamento);

        for (Carta carta : mao) {
            CardView cv = new CardView(carta, LARGURA_CARTA_MAO, ALTURA_CARTA_MAO, true);
            cv.setSelecionada(carta.equals(cartaDaMaoSelecionada), false);
            cv.setOnMouseClicked(e -> {
                boolean escolheu = !carta.equals(cartaDaMaoSelecionada);
                cartaDaMaoSelecionada = escolheu ? carta : null;
                if (escolheu) Sons.tocar(Sons.Som.ESCOLHA_CARTA);
                renderizar();
            });
            cv.setDisable(!meuTurno || estado.getEscolhaPendente() != null);
            cv.setOnDragDetected(event -> iniciarArrasteDaCarta(event, carta, meuTurno));
            maoLocal.getChildren().add(cv);
        }
    }

    private void renderizarEscolhaPendente(boolean meuTurno) {
        painelEscolha.getChildren().clear();
        if (estado.getEscolhaPendente() == null) return;

        Label titulo = new Label(estado.getEscolhaPendente().instrucao());
        titulo.setWrapText(true);
        titulo.getStyleClass().add("hint-text");
        painelEscolha.getChildren().add(titulo);

        List<Carta> opcoes = estado.getEscolhaPendente().descartarDaMao()
                ? estado.getJogadorLocal().getMao()
                : estado.getEscolhaPendente().opcoes();
        if (opcoes == null || opcoes.isEmpty()) return;

        for (Carta carta : opcoes) {
            Button botao = new Button(carta.getNome());
            botao.setMaxWidth(Double.MAX_VALUE);
            botao.setDisable(!meuTurno);
            botao.setOnAction(event -> executar(() -> screenManager.getGameClient().resolverEscolha(carta.getId())));
            painelEscolha.getChildren().add(botao);
        }
    }

    private String textoDaDica(boolean meuTurno, boolean emAndamento) {
        if (!emAndamento) return "";
        if (!meuTurno) return "Aguardando o adversário…";
        if (origemDaMuralha != null) return "Clique na personagem aliada que receberá a Muralha.";
        PersonagemCampoView proprio = personagemNoSlot(estado.getJogadorLocal(), slotProprioSelecionado);
        if (cartaDaMaoSelecionada != null) {
            if (cartaDaMaoSelecionada.isPersonagem()) {
                return (slotProprioSelecionado != null && proprio == null)
                        ? "Tudo pronto: use Jogar carta."
                        : "Clique em uma vaga livre do seu campo para posicionar.";
            }
            return "Use Jogar carta. Feitiços que tiverem alvo usam a personagem selecionada.";
        }
        if (proprio != null) {
            return slotInimigoSelecionado != null
                    ? "Tudo pronto: use Atacar."
                    : "Escolha uma personagem adversária.";
        }
        return "Escolha uma carta da mão ou uma personagem do seu campo.";
    }

    private PersonagemCampoView personagemNoSlot(JogadorView jogador, Integer slot) {
        if (slot == null || jogador.getCampo() == null) return null;
        if (slot < 0 || slot >= jogador.getCampo().size()) return null;
        return jogador.getCampo().get(slot);
    }

    private void renderizarCampo(Pane container, List<PersonagemCampoView> personagens, boolean naReserva,
                                 boolean ehAdversario, List<CharacterSlot> registro) {
        container.getChildren().clear();
        registro.clear();
        List<PersonagemCampoView> lista = personagens != null ? personagens : new ArrayList<>();
        int total = naReserva ? lista.size() : 3;

        for (int i = 0; i < total; i++) {
            final int slot = i;
            final PersonagemCampoView p = i < lista.size() ? lista.get(i) : null;
            if (p != null) {
                Integer escolhido = ehAdversario ? slotInimigoSelecionado : slotProprioSelecionado;
                boolean selectedReserve = naReserva && p.getCarta().getId().equals(personagemDaReservaSelecionada);
                p.setSelecionada(selectedReserve || (!naReserva && escolhido != null && escolhido == slot));
            }
            CharacterSlot slotView = new CharacterSlot(p, naReserva, () -> aoClicarSlot(slot, naReserva, ehAdversario, p));

            if (!naReserva && !ehAdversario) {
                configurarDestinoDeArraste(slotView, slot, p);
            }

            if (p == null && !naReserva && !ehAdversario
                    && ((cartaDaMaoSelecionada != null && cartaDaMaoSelecionada.isPersonagem())
                    || personagemDaReservaSelecionada != null)) {
                slotView.permitirPosicionar(slotProprioSelecionado != null && slotProprioSelecionado == slot);
            }
            container.getChildren().add(slotView);
            registro.add(slotView);
        }
    }

    private void iniciarArrasteDaCarta(javafx.scene.input.MouseEvent event, Carta carta, boolean meuTurno) {
        if (!meuTurno || estado.getEscolhaPendente() != null) return;
        Dragboard dragboard = ((CardView) event.getSource()).startDragAndDrop(TransferMode.MOVE);
        ClipboardContent content = new ClipboardContent();
        content.putString(carta.getId());
        dragboard.setContent(content);
        event.consume();
    }

    private void configurarDestinoDeArraste(CharacterSlot slotView, int slot, PersonagemCampoView personagem) {
        slotView.setOnDragOver(event -> {
            Carta carta = cartaArrastada(event.getDragboard());
            if (carta != null && destinoAceitaCarta(carta, personagem)) {
                event.acceptTransferModes(TransferMode.MOVE);
            }
            event.consume();
        });
        slotView.setOnDragDropped(event -> {
            Carta carta = cartaArrastada(event.getDragboard());
            boolean played = carta != null && destinoAceitaCarta(carta, personagem);
            if (played) {
                cartaDaMaoSelecionada = carta;
                slotProprioSelecionado = slot;
                aoJogarCarta();
            }
            event.setDropCompleted(played);
            event.consume();
        });
    }

    private Carta cartaArrastada(Dragboard dragboard) {
        if (!estado.isTurnoDoJogadorLocal() || !dragboard.hasString()) return null;
        return estado.getJogadorLocal().getMao().stream()
                .filter(carta -> carta.getId().equals(dragboard.getString()))
                .findFirst()
                .orElse(null);
    }

    private static boolean destinoAceitaCarta(Carta carta, PersonagemCampoView personagem) {
        return carta.isPersonagem() ? personagem == null : personagem != null;
    }

    private static void mostrarAcao(javafx.scene.Node acao, boolean mostrar) {
        acao.setVisible(mostrar);
        acao.setManaged(mostrar);
    }

    private void aoClicarSlot(int slot, boolean naReserva, boolean ehAdversario, PersonagemCampoView personagem) {
        if (!estado.isTurnoDoJogadorLocal()) return;

        if (naReserva) {
            if (personagem == null) return;
            String id = personagem.getCarta().getId();
            personagemDaReservaSelecionada = id.equals(personagemDaReservaSelecionada) ? null : id;
            cartaDaMaoSelecionada = null;
            renderizar();
            return;
        }

        if (!ehAdversario && personagem != null && origemDaMuralha != null) {
            int source = origemDaMuralha;
            origemDaMuralha = null;
            executar(() -> screenManager.getGameClient().ativarHabilidade(source, slot));
            return;
        }

        if (personagem == null) {
            boolean podePosicionar = !ehAdversario && cartaDaMaoSelecionada != null
                    && cartaDaMaoSelecionada.isPersonagem();
            boolean podeRetornar = !ehAdversario && personagemDaReservaSelecionada != null;
            if (!podePosicionar && !podeRetornar) return;
            slotProprioSelecionado = (slotProprioSelecionado != null && slotProprioSelecionado == slot) ? null : slot;
            if (slotProprioSelecionado != null) Sons.tocar(Sons.Som.SELECIONAR_ALVO);
            renderizar();
            return;
        }

        if (ehAdversario) {
            slotInimigoSelecionado = (slotInimigoSelecionado != null && slotInimigoSelecionado == slot) ? null : slot;
            if (slotInimigoSelecionado != null) Sons.tocar(Sons.Som.SELECIONAR_ALVO);
        } else {
            slotProprioSelecionado = (slotProprioSelecionado != null && slotProprioSelecionado == slot) ? null : slot;
            if (slotProprioSelecionado != null) Sons.tocar(Sons.Som.SELECIONAR_ALVO);
        }
        renderizar();
    }

    // ---------------- efeitos ----------------

    private void tocarEfeitos(EstadoPartidaDTO antes, EstadoPartidaDTO depois) {
        if (antes == null || depois == null) return;
        efeitosDoCampo(antes.getJogadorLocal(), depois.getJogadorLocal(), slotsLocais, true);
        efeitosDoCampo(antes.getAdversario(), depois.getAdversario(), slotsAdversarios, false);
        if (antes.isTurnoDoJogadorLocal() != depois.isTurnoDoJogadorLocal()
                && depois.getStatus() == StatusPartida.EM_ANDAMENTO) {
            mostrarFaixaDeTurno(depois.isTurnoDoJogadorLocal());
        }
    }

    private void efeitosDoCampo(JogadorView antes, JogadorView depois, List<CharacterSlot> slots, boolean ehLocal) {
        List<PersonagemCampoView> a = antes.getCampo();
        List<PersonagemCampoView> d = depois.getCampo();
        if (a == null || d == null) return;

        for (int i = 0; i < slots.size() && i < a.size() && i < d.size(); i++) {
            PersonagemCampoView pa = a.get(i);
            PersonagemCampoView pd = d.get(i);
            final CharacterSlot slot = slots.get(i);

            if (pa != null && pd != null) {
                if (!pa.isJaAtacou() && pd.isJaAtacou()) {
                    slot.tocarAtaque(ehLocal);
                }
                int variacao = pd.getHpAtual() - pa.getHpAtual();
                if (variacao < 0) {
                    final int dano = -variacao;
                    Efeitos.depoisDe(150, () -> slot.tocarDano(dano));
                } else if (variacao > 0) {
                    slot.tocarCura(variacao);
                }
                if (!pa.isProtegida() && pd.isProtegida()) {
                    slot.tocarProtecao();
                }
            } else if (pa != null) {
                Efeitos.depoisDe(150, () -> slot.tocarDerrota());
            } else if (pd != null) {
                slot.tocarEntrada();
            }
        }
    }

    /** Faixa que cruza a tela quando o turno muda. */
    private void mostrarFaixaDeTurno(boolean meuTurno) {
        Sons.tocar(Sons.Som.TURNO);
        Label faixa = new Label(meuTurno ? "Seu turno" : "Turno do adversário");
        faixa.getStyleClass().addAll("turn-banner", meuTurno ? "turn-banner-mine" : "turn-banner-theirs");
        faixa.setMouseTransparent(true);
        faixa.setOpacity(0);
        getChildren().add(faixa);

        Timeline t = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(faixa.opacityProperty(), 0.0),
                        new KeyValue(faixa.translateXProperty(), -90.0)),
                new KeyFrame(Duration.millis(300),
                        new KeyValue(faixa.opacityProperty(), 1.0),
                        new KeyValue(faixa.translateXProperty(), 0.0, Interpolator.EASE_OUT)),
                new KeyFrame(Duration.millis(1300),
                        new KeyValue(faixa.opacityProperty(), 1.0),
                        new KeyValue(faixa.translateXProperty(), 0.0)),
                new KeyFrame(Duration.millis(1700),
                        new KeyValue(faixa.opacityProperty(), 0.0, Interpolator.EASE_IN),
                        new KeyValue(faixa.translateXProperty(), 90.0, Interpolator.EASE_IN)));
        t.setOnFinished(e -> getChildren().remove(faixa));
        t.play();
    }
}
