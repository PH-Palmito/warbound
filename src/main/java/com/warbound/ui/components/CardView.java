package com.warbound.ui.components;

import com.warbound.model.Carta;
import com.warbound.model.TipoCarta;
import com.warbound.ui.ScreenManager;
import com.warbound.ui.audio.Sons;
import com.warbound.ui.fx.Efeitos;
import com.warbound.ui.fx.Particulas;
import com.warbound.ui.fx.PersonagemView;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

/**
 * Carta reutilizável (Biblioteca, mão do jogador). A moldura muda por
 * facção (MolduraFacao). Cartas de personagem mostram a ilustração; Efeito e
 * Busca mostram um emblema. O custo fica no disco do canto superior
 * esquerdo; ATQ e HP, só em personagens, na base.
 *
 * Existem dois tamanhos: completo (Biblioteca, com texto do efeito) e
 * compacto (mão), em que o texto do efeito vai para uma dica ao passar o mouse.
 */
public class CardView extends StackPane {

    public static final double LARGURA_PADRAO = 190;
    public static final double ALTURA_PADRAO = 290;

    private final Carta carta;
    private final boolean compacta;
    private boolean selecionada;
    private ScreenManager screenManager; // não nulo apenas quando o detalhe está disponível

    // Modelo 3D sob demanda (Biblioteca): o busto só existe enquanto a carta está visível.
    private StackPane slotArte;
    private Node placeholderArte;
    private PersonagemView bustoAtivo;
    private double larguraArte, alturaArteBusto;

    public CardView(Carta carta) {
        this(carta, LARGURA_PADRAO, ALTURA_PADRAO, false);
    }

    public CardView(Carta carta, double largura, double altura) {
        this(carta, largura, altura, largura < 150);
    }

    /** Construtor com ScreenManager: cartas com modelo 3D (ex.: Cavaleiro) abrem tela de detalhe ao clicar. */
    public CardView(Carta carta, ScreenManager screenManager) {
        this(carta, LARGURA_PADRAO, ALTURA_PADRAO, false);
        this.screenManager = screenManager;
    }

    /** Como acima, mas com o busto 3D criado só quando pedido (mostrarModelo) e removido em ocultarModelo. */
    public CardView(Carta carta, ScreenManager screenManager, boolean modeloSobDemanda) {
        this(carta, LARGURA_PADRAO, ALTURA_PADRAO, false, modeloSobDemanda);
        this.screenManager = screenManager;
    }

    public CardView(Carta carta, double largura, double altura, boolean compacta) {
        this(carta, largura, altura, compacta, false);
    }

    private CardView(Carta carta, double largura, double altura, boolean compacta, boolean modeloSobDemanda) {
        this.carta = carta;
        this.compacta = compacta;

        setPrefSize(largura, altura);
        setMinSize(largura, altura);
        setMaxSize(largura, altura);
        getStyleClass().add("card-view");
        if (compacta) getStyleClass().add("card-compact");
        MolduraFacao.aplicar(this, carta.getClasse());

        double borda = compacta ? 9 : 11;
        double larguraInterna = largura - borda * 2;

        VBox corpo = new VBox(compacta ? 3 : 5);
        corpo.setPadding(new Insets(borda));
        corpo.setAlignment(Pos.TOP_CENTER);

        Label nome = new Label(carta.getNome());
        nome.getStyleClass().add("card-title");
        nome.setMaxWidth(larguraInterna - 6);
        HBox placa = new HBox(nome);
        placa.setAlignment(Pos.CENTER);
        placa.getStyleClass().add("card-nameplate");
        corpo.getChildren().add(placa);

        double alturaArte = compacta ? altura * 0.46 : altura * 0.38;
        Node arte;
        if (carta.isPersonagem()) {
            // Cavaleiro: busto 3D animado. Fallback para PNG se 3D não estiver disponível.
            // Personagens sem modelo 3D: sempre PNG (ImagePlaceholder).
            PersonagemView busto = null;
            if (modeloSobDemanda && PersonagemView.temModelo(carta.getId())) {
                larguraArte = larguraInterna;
                alturaArteBusto = alturaArte;
                placeholderArte = new ImagePlaceholder(carta.getImagePath(), larguraInterna, alturaArte,
                        "Ilustração\n" + carta.getNome());
                slotArte = new StackPane(placeholderArte);
                slotArte.setPrefSize(larguraInterna, alturaArte);
                slotArte.setMinSize(larguraInterna, alturaArte);
                slotArte.setMaxSize(larguraInterna, alturaArte);
            } else {
                busto = PersonagemView.criar(carta.getId(), larguraInterna, alturaArte);
            }
            arte = slotArte != null ? slotArte : busto != null
                    ? busto
                    : new ImagePlaceholder(carta.getImagePath(), larguraInterna, alturaArte,
                            "Ilustração\n" + carta.getNome());
        } else {
            arte = construirEmblema(larguraInterna, alturaArte);
        }
        corpo.getChildren().add(arte);

        if (!compacta) {
            Label meta = new Label(carta.getClasse().getNomeExibicao() + " · " + nomeDoTipo());
            meta.getStyleClass().add("card-meta");
            corpo.getChildren().add(meta);

            Label descricao = new Label(carta.getDescricao());
            descricao.getStyleClass().add("card-desc");
            descricao.setWrapText(true);
            descricao.setTextAlignment(TextAlignment.CENTER);
            descricao.setMaxWidth(larguraInterna - 4);
            descricao.setMaxHeight(carta.isPersonagem() ? 62 : 110);
            corpo.getChildren().add(descricao);
        }

        if (carta.isPersonagem()) {
            Region espaco = new Region();
            VBox.setVgrow(espaco, Priority.ALWAYS);
            HBox status = new HBox(chip("ATQ", carta.getAtaque(), "orb-atq"));
            Region meio = new Region();
            HBox.setHgrow(meio, Priority.ALWAYS);
            status.getChildren().addAll(meio, chip("HP", carta.getHp(), "orb-hp"));
            status.setAlignment(Pos.CENTER);
            corpo.getChildren().addAll(espaco, status);
        }

        getChildren().add(corpo);

        Label custo = new Label(String.valueOf(carta.getCusto()));
        custo.getStyleClass().addAll("orb", "orb-custo");
        if (compacta) custo.getStyleClass().add("orb-small");
        custo.setMouseTransparent(true);
        StackPane.setAlignment(custo, Pos.TOP_LEFT);
        StackPane.setMargin(custo, new Insets(compacta ? -5 : -7, 0, 0, compacta ? -5 : -7));
        getChildren().add(custo);

        getChildren().addAll(MolduraFacao.ornamentos(carta.getClasse()));

        if (compacta) {
            Tooltip dica = new Tooltip(textoDaDica());
            dica.setWrapText(true);
            dica.setMaxWidth(260);
            dica.setShowDelay(Duration.millis(250));
            Tooltip.install(this, dica);
            Efeitos.levantarAoPassarOMouse(this, 1.16, 14);
        } else {
            Efeitos.levantarAoPassarOMouse(this, 1.04, 5);
        }

        setOnMouseClicked(e -> setSelecionada(!selecionada));
    }

    private String nomeDoTipo() {
        switch (carta.getTipo()) {
            case PERSONAGEM: return "Personagem";
            case EFEITO: return "Efeito";
            case BUSCA: return "Busca";
            default: return "Equipamento";
        }
    }

    private String textoDaDica() {
        StringBuilder sb = new StringBuilder();
        sb.append(carta.getNome()).append(" — ")
          .append(carta.getClasse().getNomeExibicao()).append(" · ").append(nomeDoTipo());
        sb.append("\nCusto: ").append(carta.getCusto()).append(" Recursos");
        if (carta.isPersonagem()) {
            sb.append("\nATQ ").append(carta.getAtaque()).append(" / HP ").append(carta.getHp());
        }
        sb.append("\n").append(carta.getDescricao());
        return sb.toString();
    }

    private HBox chip(String rotulo, Integer valor, String classeOrb) {
        Label orb = new Label(valor == null ? "-" : String.valueOf(valor));
        orb.getStyleClass().addAll("orb", classeOrb);
        if (compacta) orb.getStyleClass().add("orb-small");
        Label legenda = new Label(rotulo);
        legenda.getStyleClass().add("orb-caption");
        HBox caixa = new HBox(compacta ? 2 : 4, orb, legenda);
        caixa.setAlignment(Pos.CENTER_LEFT);
        return caixa;
    }

    private Node construirEmblema(double largura, double altura) {
        StackPane caixa = new StackPane();
        caixa.getStyleClass().add("card-emblem");
        caixa.setPrefSize(largura, altura);
        caixa.setMinSize(largura, altura);
        caixa.setMaxSize(largura, altura);

        Node forma;
        if (carta.getTipo() == TipoCarta.BUSCA) {
            Circle lente = new Circle(14);
            lente.getStyleClass().add("emblem-ring");
            Line cabo = new Line(10, 10, 27, 27);
            cabo.getStyleClass().add("emblem-handle");
            forma = new Group(lente, cabo);
        } else {
            SVGPath estrela = new SVGPath();
            estrela.setContent("M 0,-30 L 8,-8 L 30,0 L 8,8 L 0,30 L -8,8 L -30,0 L -8,-8 Z");
            estrela.getStyleClass().add("emblem-shape");
            forma = estrela;
        }
        double escala = compacta ? 0.75 : 1.2;
        forma.setScaleX(escala);
        forma.setScaleY(escala);
        caixa.getChildren().add(forma);
        return caixa;
    }

    public void setSelecionada(boolean selecionada) {
        setSelecionada(selecionada, true);
    }

    /** Com animar = false, só troca o estilo (usado quando a tela redesenha tudo). */
    public void setSelecionada(boolean selecionada, boolean animar) {
        boolean mudou = this.selecionada != selecionada;
        this.selecionada = selecionada;
        if (selecionada) {
            if (!getStyleClass().contains("card-view-selected")) getStyleClass().add("card-view-selected");
            if (mudou && animar) {
                Efeitos.pulso(this);
                Particulas.selecao(this, carta.getClasse());
                if (carta.isPersonagem()) {
                    Sons.tocarPersonagem(carta.getId());
                } else {
                    Sons.tocar(Sons.Som.ESCOLHA_CARTA);
                }
                // Carta do Cavaleiro com ScreenManager disponível: abre tela de detalhe
                if (screenManager != null && PersonagemView.temModelo(carta.getId())) {
                    screenManager.mostrarDetalheCarta(carta);
                }
            }
        } else {
            getStyleClass().remove("card-view-selected");
        }
    }

    /** Cria o busto 3D (se ainda não existe). Só faz algo em cartas criadas com modeloSobDemanda. */
    public void mostrarModelo() {
        if (slotArte == null || bustoAtivo != null) return;
        PersonagemView b = PersonagemView.criar(carta.getId(), larguraArte, alturaArteBusto);
        if (b == null) return;
        bustoAtivo = b;
        slotArte.getChildren().setAll(b);
    }

    /** Remove o busto 3D (as animações param sozinhas ao sair da cena) e volta ao placeholder. */
    public void ocultarModelo() {
        if (slotArte == null || bustoAtivo == null) return;
        bustoAtivo = null;
        slotArte.getChildren().setAll(placeholderArte);
    }

    public boolean isSelecionada() { return selecionada; }
    public Carta getCarta() { return carta; }
}
