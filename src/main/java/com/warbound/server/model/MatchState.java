package com.warbound.server.model;

import com.warbound.shared.ActionType;
import com.warbound.shared.Faction;
import com.warbound.shared.GameStatus;
import com.warbound.shared.dto.GameCommand;
import com.warbound.shared.dto.GameResult;
import com.warbound.shared.dto.MatchView;
import com.warbound.shared.dto.PlayerSession;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class MatchState {
    private static final int MAX_BOARD_SIZE = 5;

    private final UUID id = UUID.randomUUID();
    private final List<PlayerState> players = new ArrayList<>(2);
    private GameStatus status = GameStatus.WAITING_FOR_PLAYERS;
    private int activePlayerIndex;
    private long version;
    private UUID winnerId;

    public MatchState(String ownerName, Faction faction) {
        players.add(new PlayerState(validateName(ownerName), faction));
    }

    public UUID id() {
        return id;
    }

    public synchronized PlayerSession ownerSession() {
        return sessionFor(players.get(0));
    }

    public synchronized PlayerSession join(String playerName, Faction faction) {
        if (status != GameStatus.WAITING_FOR_PLAYERS || players.size() >= 2) {
            throw new IllegalStateException("A partida já possui dois jogadores.");
        }
        PlayerState joined = new PlayerState(validateName(playerName), faction);
        players.add(joined);
        players.forEach(player -> {
            player.drawCard();
            player.drawCard();
            player.drawCard();
        });
        activePlayerIndex = 0;
        currentPlayer().beginTurn();
        status = GameStatus.IN_PROGRESS;
        version++;
        return sessionFor(joined);
    }

    public synchronized MatchView viewFor(UUID playerId, String token) {
        PlayerState self = authenticate(playerId, token);
        PlayerState opponent = opponentOf(self);
        String message = switch (status) {
            case WAITING_FOR_PLAYERS -> "Aguardando o segundo jogador entrar na partida.";
            case IN_PROGRESS -> currentPlayer().id().equals(self.id()) ? "Seu turno." : "Aguardando o turno adversário.";
            case FINISHED -> winnerId != null && winnerId.equals(self.id()) ? "Você venceu a partida." : "A partida terminou.";
        };
        return new MatchView(
                id, status, version, status == GameStatus.IN_PROGRESS ? currentPlayer().id() : null,
                winnerId, self.toView(), opponent == null ? null : opponent.toView(), self.handView(), message
        );
    }

    public synchronized GameResult perform(GameCommand command) {
        PlayerState actor;
        try {
            actor = authenticate(command.playerId(), command.playerToken());
        } catch (IllegalArgumentException exception) {
            return new GameResult(false, exception.getMessage(), null);
        }
        if (status != GameStatus.IN_PROGRESS) {
            return reject(actor, "A partida ainda não está em andamento ou já foi encerrada.");
        }
        if (!currentPlayer().id().equals(actor.id())) {
            return reject(actor, "Não é o turno deste jogador.");
        }
        if (command.expectedVersion() != version) {
            return reject(actor, "O estado local está desatualizado. Atualize a partida e tente novamente.");
        }

        String message;
        try {
            message = switch (command.action()) {
                case PLAY_CARD -> playCard(actor, command.sourceCardId());
                case ATTACK -> attack(actor, command.sourceCardId(), command.targetUnitId());
                case END_TURN -> endTurn(actor);
            };
        } catch (IllegalArgumentException exception) {
            return reject(actor, exception.getMessage());
        }
        version++;
        return new GameResult(true, message, viewFor(actor.id(), actor.token()));
    }

    private String playCard(PlayerState actor, String cardId) {
        if (actor.board().size() >= MAX_BOARD_SIZE) {
            throw new IllegalArgumentException("O campo de batalha já possui cinco unidades.");
        }
        CardInstance card = actor.findHandCard(cardId);
        if (card == null) {
            throw new IllegalArgumentException("A carta selecionada não está na mão do jogador.");
        }
        if (!actor.spendMana(card.cost())) {
            throw new IllegalArgumentException("Recursos insuficientes para jogar esta carta.");
        }
        actor.hand().remove(card);
        actor.board().add(card.summon());
        return actor.name() + " colocou " + card.toView().name() + " em campo.";
    }

    private String attack(PlayerState actor, String attackerId, String targetUnitId) {
        UnitInstance attacker = actor.findUnit(attackerId);
        if (attacker == null) {
            throw new IllegalArgumentException("A unidade atacante não foi encontrada.");
        }
        if (!attacker.canAttack()) {
            throw new IllegalArgumentException("Esta unidade não pode atacar neste turno.");
        }
        PlayerState opponent = opponentOf(actor);
        if (targetUnitId == null || targetUnitId.isBlank()) {
            opponent.receiveDamage(attacker.attack());
            attacker.useAttack();
            if (opponent.isDefeated()) {
                status = GameStatus.FINISHED;
                winnerId = actor.id();
                return actor.name() + " derrotou o comandante adversário e venceu a partida.";
            }
            return attacker.toView().name() + " atacou o comandante adversário.";
        }
        UnitInstance target = opponent.findUnit(targetUnitId);
        if (target == null) {
            throw new IllegalArgumentException("A unidade alvo não pertence ao adversário ou não existe.");
        }
        target.receiveDamage(attacker.attack());
        attacker.receiveDamage(target.attack());
        attacker.useAttack();
        actor.removeDefeatedUnits();
        opponent.removeDefeatedUnits();
        return attacker.toView().name() + " atacou uma unidade adversária.";
    }

    private String endTurn(PlayerState actor) {
        if (!currentPlayer().id().equals(actor.id())) {
            throw new IllegalArgumentException("Somente o jogador ativo pode encerrar o turno.");
        }
        activePlayerIndex = (activePlayerIndex + 1) % players.size();
        currentPlayer().beginTurn();
        return actor.name() + " encerrou o turno.";
    }

    private GameResult reject(PlayerState player, String message) {
        return new GameResult(false, message, viewFor(player.id(), player.token()));
    }

    private PlayerState authenticate(UUID playerId, String token) {
        if (playerId == null || token == null) {
            throw new IllegalArgumentException("Sessão inválida.");
        }
        return players.stream()
                .filter(player -> player.id().equals(playerId) && player.token().equals(token))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Sessão inválida."));
    }

    private PlayerState currentPlayer() {
        return players.get(activePlayerIndex);
    }

    private PlayerState opponentOf(PlayerState player) {
        return players.stream().filter(candidate -> !candidate.id().equals(player.id())).findFirst().orElse(null);
    }

    private PlayerSession sessionFor(PlayerState player) {
        return new PlayerSession(id, player.id(), player.token(), players.indexOf(player));
    }

    private static String validateName(String playerName) {
        if (playerName == null || playerName.isBlank() || playerName.length() > 24) {
            throw new IllegalArgumentException("O nome do jogador deve ter entre 1 e 24 caracteres.");
        }
        return playerName.trim();
    }
}
