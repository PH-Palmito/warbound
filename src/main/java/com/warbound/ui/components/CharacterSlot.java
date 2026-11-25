package com.warbound.ui.components;

import com.warbound.model.Carta;
import com.warbound.model.PersonagemCampoView;
import com.warbound.ui.audio.Sons;
import com.warbound.ui.fx.Efeitos;
import com.warbound.ui.fx.Particulas;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

/**
 * Vaga de personagem em campo ou na Reserva. Reflete, com selos de texto e
 * moldura, os estados visuais do guia: vazia, selecionada, pronta para
 * atacar, já atacou, carregando, protegida, com Fúria, na Reserva.
 *
 * Os métodos tocar* são efeitos de combate: a tela compara o estado antigo
 * com o novo e chama o efeito correspondente. Não há regra de jogo aqui.
 */
public class CharacterSlot extends StackPane {

    private static final double LARGURA = 118;
    private static final double ALTURA = 174;
    private static final double LARGURA_RESERVA = 98;
    private static final double ALTURA_RESERVA = 138;

    private final boolean vazio;
    private Label textoVazio;
    private Carta cartaDoSlot;

    public CharacterSlot(PersonagemCampoView personagem, boolean naReserva, Runnable aoClicar) {
        double w = naReserva ? LARGURA_RESERVA : LARGURA;
        double h = naReserva ? ALTURA_RESERVA : ALTURA;
        setPrefSize(w, h);
        setMinSize(w, h);
        setMaxSize(w, h);
        getStyleClass().add("character-slot");
        this.vazio = personagem == null;

        if (personagem == null) {
            getStyleClass().add("character-slot-empty");
            textoVazio = new Label("Vaga vazia");
            textoVazio.getStyleClass().add("text-secondary");
            getChildren().add(textoVazio);
            setOnMouseClicked(e -> { if (aoClicar != null) aoClicar.run(); });
            return;
        }

        Carta carta = personagem.getCarta();
        this.cartaDoSlot = carta;
        MolduraFacao.aplicar(this, carta.getClasse());

        if (naReserva) getStyleClass().add("character-slot-reserve");
        if (personagem.isSelecionada()) getStyleClass().add("character-slot-selected");
        if (personagem.isProtegida()) getStyleClass().add("character-slot-protected");
        if (personagem.isFuriaAtiva()) getStyleClass().add("character-slot-fury");
        if (personagem.isCarregando()) getStyleClass().add("character-slot-charging");
        if (personagem.isPodeAtacar() && !naReserva) getStyleClass().add("character-slot-can-attack");
        if (personagem.isJaAtacou()) getStyleClass().add("character-slot-attacked");

        double borda = naReserva ? 7 : 9;
        double larguraInterna = w - borda * 2;

        VBox corpo = new VBox(naReserva ? 2 : 3);
        corpo.setPadding(new Insets(borda));
        corpo.setAlignment(Pos.TOP_CENTER);

        ImagePlaceholder ilustracao = new ImagePlaceholder(
                carta.getImagePath(), larguraInterna, naReserva ? 54 : 64, carta.getNome());

        Label nome = new Label(carta.getNome());
        nome.getStyleClass().add("slot-name");
        nome.setMaxWidth(larguraInterna);
        nome.setTextAlignment(TextAlignment.CENTER);

        corpo.getChildren().addAll(ilustracao, nome, construirBarraDeVida(personagem, larguraInterna));

        FlowPane selos = new FlowPane(3, 3);
        selos.setAlignment(Pos.TOP_CENTER);
        selos.setPrefWrapLength(larguraInterna);
        selos.setMaxWidth(larguraInterna);
        if (naReserva) selos.getChildren().add(new StatusBadge(StatusBadge.Tipo.RESERVA));
        if (personagem.isSelecionada()) selos.getChildren().add(new StatusBadge(StatusBadge.Tipo.SELECIONADA));
        if (personagem.isPodeAtacar() && !naReserva && !personagem.isJaAtacou()) {
            selos.getChildren().add(new StatusBadge(StatusBadge.Tipo.PODE_ATACAR));
        }
        if (personagem.isJaAtacou()) selos.getChildren().add(new StatusBadge(StatusBadge.Tipo.JA_ATACOU));
        if (personagem.isProtegida()) selos.getChildren().add(new StatusBadge(StatusBadge.Tipo.PROTEGIDA));
        if (personagem.isFuriaAtiva()) selos.getChildren().add(new StatusBadge(StatusBadge.Tipo.FURIA));
        if (personagem.isCarregando()) {
            selos.getChildren().add(new StatusBadge(StatusBadge.Tipo.CARREGANDO,
                    "Carregando (" + personagem.getTurnosDeCarregamentoRestantes() + ")"));
        }
        corpo.getChildren().add(selos);

        getChildren().add(corpo);
        getChildren().addAll(MolduraFacao.ornamentos(carta.getClasse()));

        if (!naReserva) Efeitos.levantarAoPassarOMouse(this, 1.035, 3);
        setOnMouseClicked(e -> { if (aoClicar != null) aoClicar.run(); });
    }

    private Node construirBarraDeVida(PersonagemCampoView p, double larguraInterna) {
        if ("Unidade do campo de batalha.".equals(p.getCarta().getDescricao())) {
            Label vidaAtual = new Label("HP " + p.getHpAtual());
            vidaAtual.getStyleClass().add("hp-text");
            return vidaAtual;
        }
        Integer max = p.getCarta().getHp();
        int hpMax = (max == null || max <= 0) ? Math.max(1, p.getHpAtual()) : max;
        double razao = Math.max(0.0, Math.min(1.0, (double) p.getHpAtual() / hpMax));
        double larguraBarra = larguraInterna - 2;

        Region trilha = new Region();
        trilha.getStyleClass().add("hp-track");
        trilha.setMinSize(larguraBarra, 15);
        trilha.setPrefSize(larguraBarra, 15);
        trilha.setMaxSize(larguraBarra, 15);

        Region preenchimento = new Region();
        preenchimento.getStyleClass().add("hp-fill");
        preenchimento.getStyleClass().add(razao > 0.6 ? "hp-fill-alto" : (razao > 0.3 ? "hp-fill-medio" : "hp-fill-baixo"));
        double larguraFill = Math.max(0.0, larguraBarra * razao);
        preenchimento.setMinSize(larguraFill, 15);
        preenchimento.setPrefSize(larguraFill, 15);
        preenchimento.setMaxSize(larguraFill, 15);

        Label texto = new Label("HP " + p.getHpAtual() + "/" + hpMax);
        texto.getStyleClass().add("hp-text");

        StackPane barra = new StackPane(trilha, preenchimento, texto);
        StackPane.setAlignment(preenchimento, Pos.CENTER_LEFT);
        barra.setMaxSize(larguraBarra, 15);
        return barra;
    }

    // ------------------------------------------------------------------
    // Vaga vazia que pode receber uma personagem
    // ------------------------------------------------------------------

    /** Destaca a vaga vazia como destino possível de uma carta de personagem. */
    public void permitirPosicionar(boolean escolhida) {
        if (!vazio) return;
        getStyleClass().add("character-slot-placeable");
        if (escolhida) getStyleClass().add("character-slot-selected");
        textoVazio.setText(escolhida ? "Posicionar aqui" : "Vaga livre");
    }

    // ------------------------------------------------------------------
    // Efeitos de combate
    // ------------------------------------------------------------------

    public void tocarAtaque(boolean paraCima) {
        Sons.tocar(Sons.Som.ATAQUE);
        Efeitos.investida(this, paraCima ? -30 : 30);
        if (cartaDoSlot != null) Particulas.ataque(this, cartaDoSlot.getClasse());
    }

    public void tocarDano(int valor) {
        Sons.tocar(Sons.Som.DANO);
        double amplitude = Math.min(11.0, 3.0 + valor / 5.0);
        Efeitos.corte(this);
        Efeitos.clarao(this, "fx-flash-dano", 400);
        Efeitos.tremer(this, amplitude);
        Particulas.dano(this);
        Efeitos.textoFlutuante(this, "-" + valor, "fx-dano");
    }

    public void tocarCura(int valor) {
        Sons.tocar(Sons.Som.CURA);
        Efeitos.clarao(this, "fx-flash-cura", 650);
        Particulas.cura(this);
        Efeitos.textoFlutuante(this, "+" + valor, "fx-cura");
    }

    public void tocarProtecao() {
        Sons.tocar(Sons.Som.PROTECAO);
        Efeitos.clarao(this, "fx-flash-protecao", 750);
        Particulas.protecao(this);
        Efeitos.textoFlutuante(this, "Protegida", "fx-protecao");
    }

    public void tocarEntrada() {
        Sons.tocar(Sons.Som.ENTRADA);
        Efeitos.clarao(this, "fx-flash-entrada", 650);
        if (cartaDoSlot != null) Particulas.entrada(this, cartaDoSlot.getClasse());
        Efeitos.pulso(this);
    }

    public void tocarDerrota() {
        Sons.tocar(Sons.Som.DERROTA);
        Efeitos.clarao(this, "fx-flash-dano", 500);
        Particulas.derrota(this);
        Efeitos.textoFlutuante(this, "Derrotada", "fx-dano");
    }
}
