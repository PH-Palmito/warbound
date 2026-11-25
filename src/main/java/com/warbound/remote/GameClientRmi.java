package com.warbound.remote;

import com.warbound.client.network.RmiGameClient;
import com.warbound.model.Carta;
import com.warbound.model.Classe;
import com.warbound.model.EscolhaPendenteDTO;
import com.warbound.model.EstadoPartidaDTO;
import com.warbound.model.JogadorView;
import com.warbound.model.PersonagemCampoView;
import com.warbound.model.SalaInfo;
import com.warbound.model.StatusPartida;
import com.warbound.model.TipoCarta;
import com.warbound.shared.ActionType;
import com.warbound.shared.CardType;
import com.warbound.shared.Faction;
import com.warbound.shared.dto.CardView;
import com.warbound.shared.dto.GameCommand;
import com.warbound.shared.dto.GameResult;
import com.warbound.shared.dto.MatchView;
import com.warbound.shared.dto.PendingChoiceView;
import com.warbound.shared.dto.PlayerSession;
import com.warbound.shared.dto.OpenMatchView;
import com.warbound.shared.dto.PlayerView;
import com.warbound.shared.dto.UnitView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Adapta o contrato RMI autoritativo ao modelo de apresentação JavaFX. */
public final class GameClientRmi implements GameClient {
    private final RmiGameClient remote = new RmiGameClient();
    private PlayerSession session;
    private MatchView lastView;
    private List<String> history = List.of();
    private Set<String> unitsReadyInPreviousView = Set.of();

    @Override
    public List<SalaInfo> listarSalasAbertas(String enderecoServidor) {
        try {
            connect(enderecoServidor);
            return remote.listOpenMatches().stream().map(this::sala).toList();
        } catch (Exception exception) {
            throw failure("Não foi possível listar as salas", exception);
        }
    }

    @Override
    public String criarSala(String enderecoServidor, String nomeJogador, Classe classeEscolhida) {
        try {
            connect(enderecoServidor);
            session = remote.createMatch(nomeJogador, faction(classeEscolhida));
            history = List.of("Partida criada. Compartilhe o código com o segundo jogador.");
            refreshView();
            return session.matchId().toString();
        } catch (Exception exception) {
            throw failure("Não foi possível criar a partida", exception);
        }
    }

    @Override
    public void entrarNaSala(String enderecoServidor, String nomeJogador, Classe classeEscolhida, String codigoSala) {
        try {
            connect(enderecoServidor);
            session = remote.joinMatch(UUID.fromString(codigoSala.trim()), nomeJogador, faction(classeEscolhida));
            history = List.of("Você entrou na partida.");
            refreshView();
        } catch (Exception exception) {
            throw failure("Não foi possível entrar na partida", exception);
        }
    }

    @Override
    public EstadoPartidaDTO obterEstadoDaPartida() {
        requireSession();
        try {
            return convert(refreshView(), null);
        } catch (Exception exception) {
            throw failure("Não foi possível atualizar a partida", exception);
        }
    }

    @Override
    public EstadoPartidaDTO jogarCarta(String cartaId, Integer alvoSlotCampo) {
        requireSession();
        try {
            if (lastView == null) refreshView();
            CardView card = lastView.ownHand().stream().filter(item -> item.instanceId().equals(cartaId)).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("A carta selecionada não está na sua mão."));
            String target = null;
            if (card.type() == CardType.CHARACTER) {
                if (alvoSlotCampo == null) throw new IllegalArgumentException("Escolha uma vaga vazia do seu campo.");
                target = alvoSlotCampo.toString();
            } else if (alvoSlotCampo != null) {
                target = unitAt(lastView.self().board(), alvoSlotCampo, "Escolha uma personagem sua.").instanceId();
            }
            return send(ActionType.PLAY_CARD, cartaId, target, null);
        } catch (Exception exception) {
            throw failure("Não foi possível jogar a carta", exception);
        }
    }

    @Override
    public EstadoPartidaDTO atacar(int slotAtacante, int slotAlvoAdversario) {
        UnitView attacker = unitAt(currentView().self().board(), slotAtacante, "Selecione uma personagem sua.");
        UnitView target = unitAt(currentView().opponent().board(), slotAlvoAdversario, "Selecione uma personagem adversária.");
        return send(ActionType.ATTACK, attacker.instanceId(), target.instanceId(), null);
    }

    @Override
    public EstadoPartidaDTO retiradaTatica(int slotCampo) {
        UnitView unit = unitAt(currentView().self().board(), slotCampo, "Selecione uma personagem sua.");
        return send(ActionType.TACTICAL_RETREAT, unit.instanceId(), null, null);
    }

    @Override
    public EstadoPartidaDTO retornarDaReserva(String personagemId, int slotCampo) {
        return send(ActionType.RETURN_FROM_RESERVE, personagemId, Integer.toString(slotCampo), null);
    }

    @Override
    public EstadoPartidaDTO ativarHabilidade(int slotOrigem, int slotAlvo) {
        UnitView source = unitAt(currentView().self().board(), slotOrigem, "Selecione a personagem com habilidade.");
        UnitView target = unitAt(currentView().self().board(), slotAlvo, "Selecione uma personagem aliada.");
        return send(ActionType.ACTIVATE_ABILITY, source.instanceId(), target.instanceId(), null);
    }

    @Override
    public EstadoPartidaDTO resolverEscolha(String cartaId) {
        return send(ActionType.RESOLVE_CHOICE, null, null, cartaId);
    }

    @Override
    public EstadoPartidaDTO encerrarTurno() {
        return send(ActionType.END_TURN, null, null, null);
    }

    private EstadoPartidaDTO send(ActionType action, String sourceId, String targetId, String choiceId) {
        requireSession();
        try {
            if (lastView == null) refreshView();
            GameCommand command = new GameCommand(session.matchId(), session.playerId(), session.playerToken(),
                    action, sourceId, targetId, lastView.version(), choiceId);
            GameResult result = remote.performAction(command);
            history = append(history, result.message());
            Set<String> previouslyReady = readyUnitIds(lastView);
            MatchView view = result.match() == null
                    ? remote.getMatch(session.matchId(), session.playerId(), session.playerToken())
                    : result.match();
            unitsReadyInPreviousView = previouslyReady;
            lastView = view;
            return convert(view, result.message());
        } catch (Exception exception) {
            throw failure("Não foi possível enviar a jogada", exception);
        }
    }

    private MatchView currentView() {
        try {
            if (lastView == null) refreshView();
            return lastView;
        } catch (Exception exception) {
            throw failure("Não foi possível carregar a partida", exception);
        }
    }

    private MatchView refreshView() throws Exception {
        requireSession();
        Set<String> previouslyReady = readyUnitIds(lastView);
        MatchView view = remote.getMatch(session.matchId(), session.playerId(), session.playerToken());
        unitsReadyInPreviousView = previouslyReady;
        lastView = view;
        return view;
    }

    private void connect(String address) throws Exception {
        remote.connect(address == null || address.isBlank() ? "localhost" : address.trim());
    }

    private void requireSession() {
        if (session == null) throw new IllegalStateException("Crie uma partida ou entre com um código primeiro.");
    }

    private static UnitView unitAt(List<UnitView> units, int index, String error) {
        if (units == null || index < 0 || index >= units.size() || units.get(index) == null) {
            throw new IllegalArgumentException(error);
        }
        return units.get(index);
    }

    private static Faction faction(Classe classe) {
        if (classe == null) return Faction.ROMANOS;
        return switch (classe) {
            case TEMPLARIOS -> Faction.ROMANOS;
            case ORIENTAIS -> Faction.SAMURAIS;
            case NORDICOS -> Faction.VIKINGS;
        };
    }

    private static Classe classe(Faction faction) {
        if (faction == null) return Classe.TEMPLARIOS;
        return switch (faction) {
            case ROMANOS -> Classe.TEMPLARIOS;
            case SAMURAIS -> Classe.ORIENTAIS;
            case VIKINGS -> Classe.NORDICOS;
        };
    }

    private SalaInfo sala(OpenMatchView match) {
        return new SalaInfo(match.matchId().toString(), match.ownerName(), Classe.TEMPLARIOS);
    }

    private EstadoPartidaDTO convert(MatchView view, String message) {
        if (view == null) throw new IllegalStateException("A partida ainda não foi carregada.");
        StatusPartida status = switch (view.status()) {
            case WAITING_FOR_PLAYERS -> StatusPartida.AGUARDANDO_SEGUNDO_JOGADOR;
            case IN_PROGRESS -> StatusPartida.EM_ANDAMENTO;
            case FINISHED -> StatusPartida.FINALIZADA;
        };
        boolean myTurn = view.activePlayerId() != null && view.activePlayerId().equals(view.self().playerId());
        JogadorView self = player(view.self(), view.ownHand(), true);
        JogadorView opponent = view.opponent() == null ? null : player(view.opponent(), null, false);
        String shownMessage = message == null || message.isBlank() ? view.message() : message;
        return new EstadoPartidaDTO(status, myTurn, self, opponent, history, shownMessage, view.version(),
                choice(view.pendingChoice()));
    }

    private JogadorView player(PlayerView player, List<CardView> hand, boolean own) {
        List<Carta> cards = own && hand != null ? hand.stream().map(GameClientRmi::card).toList() : null;
        List<PersonagemCampoView> board = units(player.board());
        List<PersonagemCampoView> reserve = units(player.reserve());
        return new JogadorView(player.name(), classe(player.faction()), 3 - player.livesLost(), player.mana(),
                cards, own ? cards.size() : player.handSize(), board, reserve, player.activeGroupBuff());
    }

    private List<PersonagemCampoView> units(List<UnitView> units) {
        List<PersonagemCampoView> converted = new ArrayList<>();
        if (units == null) return converted;
        for (UnitView unit : units) {
            if (unit == null) {
                converted.add(null);
                continue;
            }
            Carta card = new Carta(unit.instanceId(), unit.name(), classeDaDescricao(unit.group()), TipoCarta.PERSONAGEM,
                    0, unit.attack(), unit.maxHealth(), "Personagem do campo de batalha.", 1, null);
            boolean jaAtacou = unitsReadyInPreviousView.contains(unit.instanceId()) && !unit.readyToAttack();
            converted.add(new PersonagemCampoView(card, unit.health(), unit.readyToAttack(), jaAtacou,
                    unit.chargeTurnsRemaining() == 0 ? null : unit.chargeTurnsRemaining(),
                    unit.protectedFromAttacks(), unit.furyActive()));
        }
        return converted;
    }

    private static Carta card(CardView card) {
        TipoCarta type = card.type() == CardType.CHARACTER ? TipoCarta.PERSONAGEM : TipoCarta.EFEITO;
        Integer attack = type == TipoCarta.PERSONAGEM ? card.attack() : null;
        Integer health = type == TipoCarta.PERSONAGEM ? card.health() : null;
        return new Carta(card.instanceId(), card.name(), classe(card.group()), type, card.cost(), attack, health,
                card.description(), 1, null);
    }

    private static Classe classeDaDescricao(String group) {
        return switch (group) {
            case "Samurai" -> Classe.ORIENTAIS;
            case "Viking" -> Classe.NORDICOS;
            default -> Classe.TEMPLARIOS;
        };
    }

    private static EscolhaPendenteDTO choice(PendingChoiceView choice) {
        if (choice == null) return null;
        return new EscolhaPendenteDTO(choice.prompt(), choice.options().stream().map(GameClientRmi::card).toList(),
                choice.chooseCardFromHand());
    }

    private static List<String> append(List<String> existing, String message) {
        if (message == null || message.isBlank()) return existing;
        List<String> updated = new ArrayList<>(existing);
        updated.add(message);
        if (updated.size() > 40) updated.remove(0);
        return List.copyOf(updated);
    }

    private static Set<String> readyUnitIds(MatchView view) {
        if (view == null) return Set.of();
        Set<String> ready = new HashSet<>();
        addReadyUnitIds(ready, view.self().board());
        if (view.opponent() != null) addReadyUnitIds(ready, view.opponent().board());
        return Set.copyOf(ready);
    }

    private static void addReadyUnitIds(Set<String> ready, List<UnitView> units) {
        if (units == null) return;
        for (UnitView unit : units) if (unit != null && unit.readyToAttack()) ready.add(unit.instanceId());
    }

    private static IllegalStateException failure(String operation, Exception error) {
        String detail = error.getMessage();
        return new IllegalStateException(operation + (detail == null ? "." : ": " + detail), error);
    }
}
