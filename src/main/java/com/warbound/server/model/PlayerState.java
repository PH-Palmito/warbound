package com.warbound.server.model;

import com.warbound.shared.Faction;
import com.warbound.shared.dto.CardView;
import com.warbound.shared.dto.PendingChoiceView;
import com.warbound.shared.dto.PlayerView;
import com.warbound.shared.dto.UnitView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

final class PlayerState {
    static final int BOARD_SIZE = 3;
    static final int MAX_LIVES_LOST = 3;

    private final UUID id = UUID.randomUUID();
    private final String token = UUID.randomUUID().toString();
    private final String name;
    private final Faction faction;
    private final List<CardInstance> deck = new ArrayList<>(DeckFactory.createDeck());
    private final List<CardInstance> hand = new ArrayList<>();
    private final List<CardInstance> discard = new ArrayList<>();
    private final List<UnitInstance> board = new ArrayList<>(Collections.nCopies(BOARD_SIZE, null));
    private final List<UnitInstance> reserve = new ArrayList<>();
    private int mana;
    // O primeiro beginTurn incrementa este valor. Começamos em 1 para que o
    // primeiro turno de cada jogador tenha 2 Recursos utilizáveis.
    private int maxMana = 1;
    private int livesLost;
    private boolean completedInitialTurn;
    private boolean boardEmptiedThisTurn;
    private PendingChoice pendingChoice;

    PlayerState(String name, Faction faction) {
        this.name = name;
        this.faction = faction == null ? Faction.ROMANOS : faction;
        Collections.shuffle(deck);
        drawInitialHand();
    }

    UUID id() { return id; }
    String token() { return token; }
    String name() { return name; }
    Faction faction() { return faction; }
    int mana() { return mana; }
    int maxMana() { return maxMana; }
    List<CardInstance> hand() { return hand; }
    int livesLost() { return livesLost; }
    boolean hasCompletedInitialTurn() { return completedInitialTurn; }
    boolean hasPendingChoice() { return pendingChoice != null; }
    boolean isDefeated() { return livesLost >= MAX_LIVES_LOST; }

    void beginTurn() {
        boardEmptiedThisTurn = false;
        maxMana = Math.min(10, maxMana + 1);
        mana = maxMana;
        board.stream().filter(unit -> unit != null).forEach(UnitInstance::beginOwnerTurn);
        drawCards(1);
    }

    void endTurn() {
        completedInitialTurn = true;
    }

    boolean spendMana(int cost) {
        if (mana < cost) return false;
        mana -= cost;
        return true;
    }

    boolean isBoardEmpty() {
        return board.stream().allMatch(unit -> unit == null);
    }

    int firstEmptyBoardSlot() {
        for (int slot = 0; slot < BOARD_SIZE; slot++) if (board.get(slot) == null) return slot;
        return -1;
    }

    void placeFromHand(CardInstance card, int slot) {
        hand.remove(card);
        board.set(slot, card.summon());
    }

    void returnFromReserve(UnitInstance unit, int slot) {
        reserve.remove(unit);
        board.set(slot, unit);
        unit.useAttack();
    }

    UnitInstance findUnit(String unitId) {
        return board.stream().filter(unit -> unit != null && unit.id().equals(unitId)).findFirst().orElse(null);
    }

    UnitInstance findReserveUnit(String unitId) {
        return reserve.stream().filter(unit -> unit.id().equals(unitId)).findFirst().orElse(null);
    }

    void moveToReserve(UnitInstance unit) {
        int slot = board.indexOf(unit);
        if (slot >= 0) {
            board.set(slot, null);
            unit.useAttack();
            reserve.add(unit);
        }
    }

    boolean removeDefeatedUnits() {
        boolean removedUnit = false;
        for (int slot = 0; slot < BOARD_SIZE; slot++) {
            UnitInstance unit = board.get(slot);
            if (unit != null && unit.isDefeated()) {
                board.set(slot, null);
                discard.add(unit.card());
                removedUnit = true;
            }
        }
        return removedUnit && isBoardEmpty();
    }

    void drawCards(int count) {
        for (int drawn = 0; drawn < count; drawn++) {
            CardInstance card = drawOne();
            if (card == null) return;
            hand.add(card);
        }
    }

    CardInstance pullRandomCard(Faction group) {
        recycleDiscardIfNeeded();
        List<CardInstance> matches = deck.stream().filter(card -> card.belongsTo(group)).toList();
        if (matches.isEmpty()) return null;
        CardInstance card = matches.get((int) (Math.random() * matches.size()));
        deck.remove(card);
        hand.add(card);
        return card;
    }

    List<CardInstance> takeTopCards(int count) {
        List<CardInstance> cards = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            CardInstance card = drawOne();
            if (card == null) break;
            cards.add(card);
        }
        return cards;
    }

    void addToBottom(List<CardInstance> cards) {
        deck.addAll(cards);
    }

    void discardFromHand(String cardId) {
        CardInstance card = findHandCard(cardId);
        if (card == null) throw new IllegalArgumentException("Escolha uma carta válida da própria mão para descartar.");
        hand.remove(card);
        discard.add(card);
    }

    CardInstance findHandCard(String cardId) {
        return hand.stream().filter(card -> card.id().toString().equals(cardId)).findFirst().orElse(null);
    }

    void discardPlayedSpell(CardInstance card) {
        hand.remove(card);
        discard.add(card);
    }

    void startHandDiscardChoice(String prompt) {
        pendingChoice = PendingChoice.discardFromHand(prompt);
    }

    void startTopDeckChoice(String prompt, List<CardInstance> options) {
        if (options.isEmpty()) return;
        pendingChoice = PendingChoice.topDeck(prompt, options);
    }

    String resolveChoice(String cardId) {
        if (pendingChoice == null) throw new IllegalArgumentException("Não há uma escolha pendente.");
        String message = pendingChoice.resolve(this, cardId);
        pendingChoice = null;
        return message;
    }

    PendingChoiceView pendingChoiceView() {
        return pendingChoice == null ? null : pendingChoice.toView();
    }

    PlayerView toView() {
        boolean formation = hasFullGroup(Faction.ROMANOS);
        boolean bushido = hasFullGroup(Faction.SAMURAIS);
        boolean fury = hasFullGroup(Faction.VIKINGS);
        List<UnitView> boardView = new ArrayList<>(BOARD_SIZE);
        for (int slot = 0; slot < BOARD_SIZE; slot++) {
            UnitInstance unit = board.get(slot);
            boardView.add(unit == null ? null : unit.toView(slot, bushido && unit.group() == Faction.SAMURAIS,
                    fury && unit.group() == Faction.VIKINGS && unit.isAtOrBelowHalfHealth()));
        }
        List<UnitView> reserveView = reserve.stream().map(unit -> unit.toView(-1, false, false)).toList();
        return new PlayerView(id, name, faction, MAX_LIVES_LOST - livesLost, mana, maxMana, deck.size(), hand.size(),
                Collections.unmodifiableList(boardView), reserveView, discard.size(), livesLost, activeGroupBuff());
    }

    List<CardView> handView() { return hand.stream().map(CardInstance::toView).toList(); }
    boolean hasFullGroup(Faction group) {
        return board.stream().allMatch(unit -> unit != null && unit.group() == group);
    }
    boolean receivesFormationReduction() { return hasFullGroup(Faction.ROMANOS); }
    boolean receivesBushido(UnitInstance unit) { return hasFullGroup(Faction.SAMURAIS) && unit.group() == Faction.SAMURAIS; }
    boolean receivesClanFury(UnitInstance unit) { return hasFullGroup(Faction.VIKINGS) && unit.group() == Faction.VIKINGS && unit.isAtOrBelowHalfHealth(); }
    void loseLife() { livesLost++; }
    void markBoardEmptiedThisTurn() { boardEmptiedThisTurn = true; }
    boolean boardEmptiedThisTurn() { return boardEmptiedThisTurn; }

    private void drawInitialHand() {
        drawCards(3);
        if (hand.stream().anyMatch(CardInstance::isBasicAttacker)) return;
        int basicAttackerIndex = -1;
        for (int index = 0; index < deck.size(); index++) {
            if (deck.get(index).isBasicAttacker()) {
                basicAttackerIndex = index;
                break;
            }
        }
        if (basicAttackerIndex >= 0) {
            CardInstance basicAttacker = deck.set(basicAttackerIndex, hand.get(2));
            hand.set(2, basicAttacker);
        }
    }

    private String activeGroupBuff() {
        if (hasFullGroup(Faction.ROMANOS)) return "Formação: cada Cavaleiro reduz 5 de dano de ataques.";
        if (hasFullGroup(Faction.SAMURAIS)) return "Bushido: cada Samurai causa +5 de dano.";
        if (hasFullGroup(Faction.VIKINGS)) return "Fúria do Clã: Vikings com até metade do HP recebem +10 ATQ.";
        return null;
    }

    private CardInstance drawOne() {
        recycleDiscardIfNeeded();
        return deck.isEmpty() ? null : deck.remove(0);
    }

    private void recycleDiscardIfNeeded() {
        if (!deck.isEmpty() || discard.isEmpty()) return;
        deck.addAll(discard);
        discard.clear();
        Collections.shuffle(deck);
    }

    private static final class PendingChoice {
        private final String prompt;
        private final boolean discardFromHand;
        private final List<CardInstance> options;

        private PendingChoice(String prompt, boolean discardFromHand, List<CardInstance> options) {
            this.prompt = prompt;
            this.discardFromHand = discardFromHand;
            this.options = options;
        }

        static PendingChoice discardFromHand(String prompt) {
            return new PendingChoice(prompt, true, List.of());
        }

        static PendingChoice topDeck(String prompt, List<CardInstance> options) {
            return new PendingChoice(prompt, false, List.copyOf(options));
        }

        String resolve(PlayerState player, String cardId) {
            if (discardFromHand) {
                player.discardFromHand(cardId);
                return "Carta descartada.";
            }
            CardInstance selected = options.stream().filter(card -> card.id().toString().equals(cardId)).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Escolha uma carta exibida pelo servidor."));
            player.hand.add(selected);
            List<CardInstance> remaining = new ArrayList<>(options);
            remaining.remove(selected);
            player.addToBottom(remaining);
            return selected.toView().name() + " foi adicionada à mão.";
        }

        PendingChoiceView toView() {
            return new PendingChoiceView(prompt, options.stream().map(CardInstance::toView).toList(), discardFromHand);
        }
    }
}
