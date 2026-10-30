package com.warbound.client;

import com.warbound.shared.ActionType;
import com.warbound.shared.Faction;
import com.warbound.shared.dto.CardView;
import com.warbound.shared.dto.GameCommand;
import com.warbound.shared.dto.GameResult;
import com.warbound.shared.dto.MatchView;
import com.warbound.shared.dto.PlayerSession;
import com.warbound.shared.dto.UnitView;
import com.warbound.shared.remote.WarboundRemote;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.UUID;

public final class WarboundClientApp extends Application {
    private final TextField hostField = new TextField("127.0.0.1");
    private final TextField nameField = new TextField();
    private final TextField matchIdField = new TextField();
    private final ChoiceBox<Faction> factionChoice = new ChoiceBox<>(FXCollections.observableArrayList(Faction.values()));
    private final Label statusLabel = new Label("Conecte-se ao servidor e crie ou entre em uma partida.");
    private final Label selfLabel = new Label("Você: -");
    private final Label opponentLabel = new Label("Adversário: aguardando jogador");
    private final ListView<CardView> handList = new ListView<>();
    private final ListView<UnitView> ownBoardList = new ListView<>();
    private final ListView<UnitView> opponentBoardList = new ListView<>();

    private WarboundRemote service;
    private PlayerSession session;
    private MatchView match;

    @Override
    public void start(Stage stage) {
        factionChoice.setValue(Faction.ROMANOS);
        configureCardCell(handList);
        configureUnitCell(ownBoardList);
        configureUnitCell(opponentBoardList);

        GridPane connection = new GridPane();
        connection.setHgap(8);
        connection.setVgap(8);
        connection.setPadding(new Insets(12));
        connection.addRow(0, new Label("Servidor"), hostField, new Label("Nome"), nameField, new Label("Povo"), factionChoice);
        connection.addRow(1, new Label("Partida"), matchIdField, createButton(), joinButton(), refreshButton());

        VBox playerColumn = new VBox(6, selfLabel, new Label("Sua mão"), handList, new Label("Seu campo"), ownBoardList);
        playerColumn.setPrefWidth(340);
        VBox opponentColumn = new VBox(6, opponentLabel, new Label("Campo adversário"), opponentBoardList);
        opponentColumn.setPrefWidth(340);

        HBox boards = new HBox(14, playerColumn, opponentColumn);
        boards.setPadding(new Insets(12));
        HBox actions = new HBox(8, playButton(), attackCommanderButton(), attackUnitButton(), endTurnButton());
        actions.setPadding(new Insets(12));

        BorderPane root = new BorderPane();
        root.setTop(connection);
        root.setCenter(boards);
        root.setBottom(new VBox(actions, statusLabel));
        BorderPane.setMargin(statusLabel, new Insets(0, 12, 12, 12));

        stage.setTitle("Warbound");
        stage.setScene(new Scene(root, 760, 620));
        stage.show();
    }

    private Button createButton() {
        Button button = new Button("Criar partida");
        button.setOnAction(event -> runSafely(() -> {
            PlayerSession created = remote().createMatch(requiredName(), factionChoice.getValue());
            session = created;
            matchIdField.setText(created.matchId().toString());
            refresh();
        }));
        return button;
    }

    private Button joinButton() {
        Button button = new Button("Entrar na partida");
        button.setOnAction(event -> runSafely(() -> {
            UUID matchId = UUID.fromString(matchIdField.getText().trim());
            session = remote().joinMatch(matchId, requiredName(), factionChoice.getValue());
            refresh();
        }));
        return button;
    }

    private Button refreshButton() {
        Button button = new Button("Atualizar");
        button.setOnAction(event -> runSafely(this::refresh));
        return button;
    }

    private Button playButton() {
        Button button = new Button("Jogar carta selecionada");
        button.setOnAction(event -> runSafely(() -> {
            CardView card = requireSelection(handList, "Selecione uma carta da mão.");
            send(ActionType.PLAY_CARD, card.instanceId(), null);
        }));
        return button;
    }

    private Button attackCommanderButton() {
        Button button = new Button("Atacar comandante");
        button.setOnAction(event -> runSafely(() -> {
            UnitView unit = requireSelection(ownBoardList, "Selecione uma unidade do seu campo.");
            send(ActionType.ATTACK, unit.instanceId(), null);
        }));
        return button;
    }

    private Button attackUnitButton() {
        Button button = new Button("Atacar unidade");
        button.setOnAction(event -> runSafely(() -> {
            UnitView attacker = requireSelection(ownBoardList, "Selecione uma unidade do seu campo.");
            UnitView target = requireSelection(opponentBoardList, "Selecione uma unidade adversária.");
            send(ActionType.ATTACK, attacker.instanceId(), target.instanceId());
        }));
        return button;
    }

    private Button endTurnButton() {
        Button button = new Button("Encerrar turno");
        button.setOnAction(event -> runSafely(() -> send(ActionType.END_TURN, null, null)));
        return button;
    }

    private void send(ActionType action, String sourceCardId, String targetUnitId) throws Exception {
        requireSession();
        GameCommand command = new GameCommand(
                session.matchId(), session.playerId(), session.playerToken(), action,
                sourceCardId, targetUnitId, match.version()
        );
        GameResult result = remote().performAction(command);
        statusLabel.setText(result.message());
        if (result.match() != null) {
            showMatch(result.match());
        }
    }

    private void refresh() throws Exception {
        requireSession();
        showMatch(remote().getMatch(session.matchId(), session.playerId(), session.playerToken()));
    }

    private WarboundRemote remote() throws Exception {
        if (service == null) {
            Registry registry = LocateRegistry.getRegistry(hostField.getText().trim(), 1099);
            service = (WarboundRemote) registry.lookup(WarboundRemote.SERVICE_NAME);
        }
        return service;
    }

    private void showMatch(MatchView view) {
        match = view;
        handList.setItems(FXCollections.observableArrayList(view.ownHand()));
        ownBoardList.setItems(FXCollections.observableArrayList(view.self().board()));
        selfLabel.setText(String.format("Você: %s | Vida %d | Mana %d/%d", view.self().name(), view.self().commanderHealth(), view.self().mana(), view.self().maxMana()));
        if (view.opponent() == null) {
            opponentLabel.setText("Adversário: aguardando jogador");
            opponentBoardList.setItems(FXCollections.emptyObservableList());
        } else {
            opponentBoardList.setItems(FXCollections.observableArrayList(view.opponent().board()));
            opponentLabel.setText(String.format("Adversário: %s | Vida %d", view.opponent().name(), view.opponent().commanderHealth()));
        }
        statusLabel.setText(view.message() + " Versão: " + view.version());
    }

    private String requiredName() {
        String name = nameField.getText().trim();
        if (name.isBlank()) {
            throw new IllegalArgumentException("Informe um nome de jogador.");
        }
        return name;
    }

    private void requireSession() {
        if (session == null || match == null) {
            throw new IllegalArgumentException("Crie uma partida ou entre em uma partida antes de jogar.");
        }
    }

    private static <T> T requireSelection(ListView<T> list, String message) {
        T item = list.getSelectionModel().getSelectedItem();
        if (item == null) {
            throw new IllegalArgumentException(message);
        }
        return item;
    }

    private void runSafely(ThrowingAction action) {
        try {
            action.run();
        } catch (Exception exception) {
            statusLabel.setText(exception.getMessage() == null ? "Falha na comunicação com o servidor." : exception.getMessage());
        }
    }

    private static void configureCardCell(ListView<CardView> list) {
        list.setCellFactory(ignored -> new ListCell<>() {
            @Override
            protected void updateItem(CardView card, boolean empty) {
                super.updateItem(card, empty);
                setText(empty || card == null ? null : String.format("%s | custo %d | %d/%d", card.name(), card.cost(), card.attack(), card.health()));
            }
        });
    }

    private static void configureUnitCell(ListView<UnitView> list) {
        list.setCellFactory(ignored -> new ListCell<>() {
            @Override
            protected void updateItem(UnitView unit, boolean empty) {
                super.updateItem(unit, empty);
                setText(empty || unit == null ? null : String.format("%s | %d/%d | %s", unit.name(), unit.attack(), unit.health(), unit.readyToAttack() ? "pronta" : "sem ataque"));
            }
        });
    }

    @FunctionalInterface
    private interface ThrowingAction {
        void run() throws Exception;
    }
}
