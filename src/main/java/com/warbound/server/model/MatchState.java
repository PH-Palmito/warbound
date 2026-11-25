package com.warbound.server.model;

import com.warbound.shared.ActionType;
import com.warbound.shared.CardType;
import com.warbound.shared.Faction;
import com.warbound.shared.GameStatus;
import com.warbound.shared.dto.GameCommand;
import com.warbound.shared.dto.GameResult;
import com.warbound.shared.dto.MatchView;
import com.warbound.shared.dto.PlayerSession;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Estado autoritativo de uma partida para dois jogadores. */
public final class MatchState {
    private final UUID id = UUID.randomUUID();
    private final List<PlayerState> players = new ArrayList<>(2);
    private GameStatus status = GameStatus.WAITING_FOR_PLAYERS;
    private int activePlayerIndex;
    private long version;
    private UUID winnerId;

    public MatchState(String ownerName, Faction faction) {
        players.add(new PlayerState(validateName(ownerName), faction));
    }

    public UUID id() { return id; }

    public synchronized boolean isWaitingForPlayers() {
        return status == GameStatus.WAITING_FOR_PLAYERS;
    }

    public synchronized String ownerName() {
        return players.get(0).name();
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
        return new MatchView(id, status, version, status == GameStatus.IN_PROGRESS ? currentPlayer().id() : null,
                winnerId, self.toView(), opponent == null ? null : opponent.toView(), self.handView(), message,
                self.pendingChoiceView());
    }

    public synchronized GameResult perform(GameCommand command) {
        if (command == null || command.matchId() == null || command.action() == null) {
            return new GameResult(false, "Comando de jogada inválido.", null);
        }
        PlayerState actor;
        try {
            actor = authenticate(command.playerId(), command.playerToken());
        } catch (IllegalArgumentException exception) {
            return new GameResult(false, exception.getMessage(), null);
        }
        if (status != GameStatus.IN_PROGRESS) return reject(actor, "A partida ainda não está em andamento ou já foi encerrada.");
        if (!currentPlayer().id().equals(actor.id())) return reject(actor, "Não é o turno deste jogador.");
        if (command.expectedVersion() != version) return reject(actor, "O estado local está desatualizado. Atualize a partida e tente novamente.");
        if (actor.hasPendingChoice() && command.action() != ActionType.RESOLVE_CHOICE) {
            return reject(actor, "Conclua primeiro a escolha pendente.");
        }
        if (!actor.hasPendingChoice() && command.action() == ActionType.RESOLVE_CHOICE) {
            return reject(actor, "Não há uma escolha pendente.");
        }

        try {
            String message = switch (command.action()) {
                case PLAY_CARD -> playCard(actor, command.sourceCardId(), command.targetUnitId());
                case ATTACK -> attack(actor, command.sourceCardId(), command.targetUnitId());
                case END_TURN -> endTurn(actor);
                case TACTICAL_RETREAT -> tacticalRetreat(actor, command.sourceCardId());
                case RETURN_FROM_RESERVE -> returnFromReserve(actor, command.sourceCardId(), command.targetUnitId());
                case ACTIVATE_ABILITY -> activateAbility(actor, command.sourceCardId(), command.targetUnitId());
                case RESOLVE_CHOICE -> actor.resolveChoice(command.choiceCardId() == null ? command.sourceCardId() : command.choiceCardId());
            };
            version++;
            return new GameResult(true, message, viewFor(actor.id(), actor.token()));
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return reject(actor, exception.getMessage());
        }
    }

    private String playCard(PlayerState actor, String cardId, String targetId) {
        CardInstance card = actor.findHandCard(cardId);
        if (card == null) throw new IllegalArgumentException("A carta selecionada não está na mão do jogador.");
        if (!actor.spendMana(card.cost())) {
            throw new IllegalArgumentException(card.toView().name() + " custa " + card.cost()
                    + " Recursos, mas você possui apenas " + actor.mana() + ".");
        }

        if (card.type() == CardType.CHARACTER) return playCharacter(actor, card, targetId);
        actor.discardPlayedSpell(card);
        return castSpell(actor, card, targetId);
    }

    private String playCharacter(PlayerState actor, CardInstance card, String targetId) {
        int slot = boardSlot(targetId, actor.firstEmptyBoardSlot());
        if (slot < 0 || slot >= PlayerState.BOARD_SIZE) throw new IllegalArgumentException("Escolha uma vaga vazia do campo.");
        if (actor.firstEmptyBoardSlot() < 0) throw new IllegalArgumentException("O campo já possui três personagens.");
        if (targetId != null && !targetId.isBlank() && actor.toView().board().get(slot) != null) {
            throw new IllegalArgumentException("A vaga escolhida já está ocupada.");
        }
        actor.placeFromHand(card, slot);
        return switch (card.effect()) {
            case NAVIGATOR_DRAW -> {
                actor.drawCards(1);
                yield card.toView().name() + " entrou em campo e comprou uma carta.";
            }
            case MONK_LOOK_AHEAD -> {
                actor.startTopDeckChoice("Escolha uma das duas cartas vistas pelo Monge Guerreiro.", actor.takeTopCards(2));
                yield card.toView().name() + " entrou em campo. Escolha uma carta do topo.";
            }
            default -> card.toView().name() + " entrou em campo na vaga " + (slot + 1) + ".";
        };
    }

    private String castSpell(PlayerState actor, CardInstance card, String targetId) {
        return switch (card.effect()) {
            case BLESSING -> {
                UnitInstance target = requireOwnUnit(actor, targetId);
                target.heal(25);
                yield "Bênção da Coroa curou " + target.card().toView().name() + ".";
            }
            case DEFENSIVE_ORDER -> {
                UnitInstance target = requireOwnUnit(actor, targetId);
                target.reduceNextDamageBy(20);
                yield "Ordem Defensiva foi aplicada a " + target.card().toView().name() + ".";
            }
            case SUPPLIES, PLUNDER -> {
                actor.drawCards(2);
                actor.startHandDiscardChoice("Escolha uma carta da mão para descartar.");
                yield card.toView().name() + " comprou duas cartas. Escolha uma para descartar.";
            }
            case RECRUITMENT -> search(actor, Faction.ROMANOS, "Cavaleiro");
            case IAIJUTSU -> {
                UnitInstance target = requireOwnUnitOfGroup(actor, targetId, Faction.SAMURAIS);
                target.addNextAttackBonus(15);
                yield "Golpe Iaijutsu foi preparado para " + target.card().toView().name() + ".";
            }
            case MEDITATION -> {
                actor.startTopDeckChoice("Escolha uma das três cartas vistas pela Meditação.", actor.takeTopCards(3));
                yield "Meditação revelou cartas do topo. Escolha uma para a mão.";
            }
            case MIST -> {
                UnitInstance target = requireOwnUnitOfGroup(actor, targetId, Faction.SAMURAIS);
                target.protectFromNextAttack();
                yield "Névoa do Dojo protegeu " + target.card().toView().name() + ".";
            }
            case DOJO_CALL -> search(actor, Faction.SAMURAIS, "Samurai");
            case HYDROMEL -> {
                UnitInstance target = requireOwnUnit(actor, targetId);
                target.heal(target.group() == Faction.VIKINGS ? 35 : 20);
                yield "Hidromel restaurou a saúde de " + target.card().toView().name() + ".";
            }
            case WAR_CRY -> {
                UnitInstance target = requireOwnUnitOfGroup(actor, targetId, Faction.VIKINGS);
                target.addNextAttackBonus(15);
                yield "Grito de Guerra foi preparado para " + target.card().toView().name() + ".";
            }
            case VALHALLA_CALL -> search(actor, Faction.VIKINGS, "Viking");
            default -> throw new IllegalArgumentException("Este feitiço não possui efeito jogável.");
        };
    }

    private String search(PlayerState actor, Faction group, String groupName) {
        CardInstance found = actor.pullRandomCard(group);
        return found == null ? "A busca não encontrou uma carta " + groupName + "." : found.toView().name() + " foi adicionada à mão.";
    }

    private String attack(PlayerState actor, String attackerId, String targetId) {
        UnitInstance attacker = requireOwnUnit(actor, attackerId);
        if (!attacker.canAttack()) throw new IllegalArgumentException("Esta personagem não pode atacar neste turno.");
        PlayerState opponent = opponentOf(actor);
        UnitInstance target = opponent.findUnit(targetId);
        if (target == null) throw new IllegalArgumentException("Escolha uma personagem adversária como alvo.");

        int attackDamage = attacker.attack(actor.receivesBushido(attacker), actor.receivesClanFury(attacker)) + attacker.consumeFirstStrikeBonus();
        int counterDamage = target.attack(opponent.receivesBushido(target), opponent.receivesClanFury(target));
        attacker.consumeAttackBonus();
        int dealt = target.receiveAttackDamage(attackDamage, opponent.receivesFormationReduction());
        int received = attacker.receiveAttackDamage(counterDamage, actor.receivesFormationReduction());
        attacker.useAttack();
        if (actor.removeDefeatedUnits()) actor.markBoardEmptiedThisTurn();
        opponent.removeDefeatedUnits();
        return attacker.card().toView().name() + " causou " + dealt + " de dano e recebeu " + received + " no contra-ataque.";
    }

    private String tacticalRetreat(PlayerState actor, String unitId) {
        UnitInstance unit = requireOwnUnit(actor, unitId);
        actor.moveToReserve(unit);
        return unit.card().toView().name() + " foi para a Reserva.";
    }

    private String returnFromReserve(PlayerState actor, String unitId, String targetId) {
        UnitInstance unit = actor.findReserveUnit(unitId);
        if (unit == null) throw new IllegalArgumentException("A personagem selecionada não está na Reserva.");
        int slot = boardSlot(targetId, actor.firstEmptyBoardSlot());
        if (slot < 0 || slot >= PlayerState.BOARD_SIZE || actor.toView().board().get(slot) != null) {
            throw new IllegalArgumentException("Escolha uma vaga vazia do campo.");
        }
        if (!actor.spendMana(1)) throw new IllegalArgumentException("Recursos insuficientes para trazer a personagem da Reserva.");
        actor.returnFromReserve(unit, slot);
        return unit.card().toView().name() + " voltou da Reserva e não pode atacar neste turno.";
    }

    private String activateAbility(PlayerState actor, String sourceId, String targetId) {
        UnitInstance source = requireOwnUnit(actor, sourceId);
        if (!source.canUseGuardianWall()) throw new IllegalArgumentException("Esta habilidade ainda não está disponível.");
        UnitInstance target = requireOwnUnit(actor, targetId);
        source.useGuardianWall();
        target.protectUntilNextTurn();
        return source.card().toView().name() + " protegeu " + target.card().toView().name() + " do próximo ataque.";
    }

    private String endTurn(PlayerState actor) {
        if (actor.hasPendingChoice()) throw new IllegalStateException("Conclua primeiro a escolha pendente.");
        boolean loseLife = actor.hasCompletedInitialTurn() && actor.isBoardEmpty() && !actor.boardEmptiedThisTurn();
        actor.endTurn();
        if (loseLife) {
            actor.loseLife();
            if (actor.isDefeated()) {
                status = GameStatus.FINISHED;
                winnerId = opponentOf(actor).id();
                return actor.name() + " perdeu a terceira vida. A partida terminou.";
            }
        }
        activePlayerIndex = (activePlayerIndex + 1) % players.size();
        currentPlayer().beginTurn();
        return loseLife ? actor.name() + " encerrou o turno sem personagem em campo e perdeu uma vida." : actor.name() + " encerrou o turno.";
    }

    private GameResult reject(PlayerState player, String message) {
        return new GameResult(false, message, viewFor(player.id(), player.token()));
    }

    private PlayerState authenticate(UUID playerId, String token) {
        if (playerId == null || token == null) throw new IllegalArgumentException("Sessão inválida.");
        return players.stream().filter(player -> player.id().equals(playerId) && player.token().equals(token)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Sessão inválida."));
    }

    private PlayerState currentPlayer() { return players.get(activePlayerIndex); }
    private PlayerState opponentOf(PlayerState player) { return players.stream().filter(candidate -> !candidate.id().equals(player.id())).findFirst().orElse(null); }
    private PlayerSession sessionFor(PlayerState player) { return new PlayerSession(id, player.id(), player.token(), players.indexOf(player)); }

    private static UnitInstance requireOwnUnit(PlayerState actor, String unitId) {
        UnitInstance unit = actor.findUnit(unitId);
        if (unit == null) throw new IllegalArgumentException("A personagem selecionada não está no seu campo.");
        return unit;
    }

    private static UnitInstance requireOwnUnitOfGroup(PlayerState actor, String unitId, Faction group) {
        UnitInstance unit = requireOwnUnit(actor, unitId);
        if (unit.group() != group) throw new IllegalArgumentException("Escolha uma personagem " + groupName(group) + ".");
        return unit;
    }

    private static int boardSlot(String targetId, int fallback) {
        if (targetId == null || targetId.isBlank()) return fallback;
        try {
            return Integer.parseInt(targetId);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("A vaga do campo deve ser 0, 1 ou 2.");
        }
    }

    private static String groupName(Faction group) {
        return switch (group) {
            case ROMANOS -> "Cavaleiro";
            case SAMURAIS -> "Samurai";
            case VIKINGS -> "Viking";
        };
    }

    private static String validateName(String playerName) {
        if (playerName == null || playerName.isBlank() || playerName.length() > 24) {
            throw new IllegalArgumentException("O nome do jogador deve ter entre 1 e 24 caracteres.");
        }
        return playerName.trim();
    }
}
