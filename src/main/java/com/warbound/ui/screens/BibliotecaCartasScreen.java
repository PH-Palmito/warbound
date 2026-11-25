package com.warbound.ui.screens;

import com.warbound.ui.audio.Sons;
import com.warbound.data.CatalogoLocal;
import com.warbound.model.Carta;
import com.warbound.model.Classe;
import com.warbound.model.TipoCarta;
import com.warbound.ui.ScreenManager;
import com.warbound.ui.components.AppHeader;
import com.warbound.ui.components.CardView;
import com.warbound.ui.fx.Efeitos;
import com.warbound.ui.fx.PersonagemView;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Biblioteca de Cartas: catálogo local TEMPORÁRIO (não é a mão do jogador
 * nem cartas privadas de uma partida), filtrável por classe e por tipo.
 */
public class BibliotecaCartasScreen extends BorderPane {

    private final List<Carta> catalogo = CatalogoLocal.todasAsCartas();
    private final FlowPane grade = new FlowPane(18, 22);
    private final Label contador = new Label();
    private final ScreenManager screenManager;
    private final ScrollPane rolagem = new ScrollPane(grade);

    private Classe filtroClasse; // null = "Todas"
    private TipoCarta filtroTipo; // null = "Todas"

    public BibliotecaCartasScreen(ScreenManager screenManager) {
        this.screenManager = screenManager;
        getStyleClass().add("screen-root");
        setTop(new AppHeader("Biblioteca de Cartas", screenManager::mostrarMenu));

        grade.setPadding(new Insets(20, 20, 24, 20));
        grade.setAlignment(Pos.TOP_CENTER);

        rolagem.setFitToWidth(true);
        rolagem.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        rolagem.getStyleClass().add("list-view-flat");
        VBox.setVgrow(rolagem, Priority.ALWAYS);

        // Modelos 3D só nas cartas visíveis: reavalia ao rolar, redimensionar e entrar na cena.
        rolagem.vvalueProperty().addListener((o, a, n) -> atualizarVisiveis());
        rolagem.heightProperty().addListener((o, a, n) -> Platform.runLater(this::atualizarVisiveis));
        grade.heightProperty().addListener((o, a, n) -> Platform.runLater(this::atualizarVisiveis));
        sceneProperty().addListener((o, a, n) -> { if (n != null) Platform.runLater(this::atualizarVisiveis); });

        contador.getStyleClass().add("text-secondary");

        VBox corpo = new VBox(12, construirFiltros(), rolagem);
        corpo.setPadding(new Insets(14, 20, 0, 20));
        setCenter(corpo);
        atualizarGrade();
    }

    private HBox construirFiltros() {
        ToggleGroup grupoClasse = new ToggleGroup();
        HBox filtrosClasse = new HBox(8,
                rotulo("Facção"),
                botaoFiltro("Todas", grupoClasse, true, null, () -> filtroClasse = null),
                botaoFiltro("Templários", grupoClasse, false, "filter-toggle-templarios", () -> filtroClasse = Classe.TEMPLARIOS),
                botaoFiltro("Orientais", grupoClasse, false, "filter-toggle-orientais", () -> filtroClasse = Classe.ORIENTAIS),
                botaoFiltro("Nórdicos", grupoClasse, false, "filter-toggle-nordicos", () -> filtroClasse = Classe.NORDICOS));
        filtrosClasse.setAlignment(Pos.CENTER_LEFT);

        ToggleGroup grupoTipo = new ToggleGroup();
        HBox filtrosTipo = new HBox(8,
                rotulo("Tipo"),
                botaoFiltro("Todas", grupoTipo, true, null, () -> filtroTipo = null),
                botaoFiltro("Personagens", grupoTipo, false, null, () -> filtroTipo = TipoCarta.PERSONAGEM),
                botaoFiltro("Efeitos", grupoTipo, false, null, () -> filtroTipo = TipoCarta.EFEITO),
                botaoFiltro("Busca", grupoTipo, false, null, () -> filtroTipo = TipoCarta.BUSCA));
        filtrosTipo.setAlignment(Pos.CENTER_LEFT);

        Region espaco = new Region();
        HBox.setHgrow(espaco, Priority.ALWAYS);

        VBox colunaFiltros = new VBox(8, filtrosClasse, filtrosTipo);
        HBox barra = new HBox(16, colunaFiltros, espaco, contador);
        barra.setAlignment(Pos.CENTER_LEFT);
        barra.getStyleClass().addAll("panel", "filter-bar");
        return barra;
    }

    private Label rotulo(String texto) {
        Label l = new Label(texto);
        l.getStyleClass().add("filter-label");
        l.setMinWidth(62);
        return l;
    }

    private ToggleButton botaoFiltro(String texto, ToggleGroup grupo, boolean selecionadoInicialmente,
                                     String classeExtra, Runnable aoSelecionar) {
        ToggleButton botao = new ToggleButton(texto);
        botao.getStyleClass().add("filter-toggle");
        if (classeExtra != null) botao.getStyleClass().add(classeExtra);
        botao.setToggleGroup(grupo);
        botao.setSelected(selecionadoInicialmente);
        botao.setOnAction(e -> {
            Sons.tocar(Sons.Som.CLIQUE);
            if (botao.isSelected()) {
                aoSelecionar.run();
                atualizarGrade();
            } else {
                botao.setSelected(true); // pelo menos um filtro sempre fica ativo
            }
        });
        return botao;
    }

    private void atualizarGrade() {
        grade.getChildren().clear();
        int total = 0;
        for (Carta carta : catalogo) {
            boolean passaClasse = filtroClasse == null || carta.getClasse() == filtroClasse;
            boolean passaTipo = filtroTipo == null || carta.getTipo() == filtroTipo;
            if (passaClasse && passaTipo) {
                // Carta com modelo 3D (Cavaleiro): passa o screenManager para abrir tela de detalhe ao clicar.
                // Demais cartas: comportamento padrão sem detalhe.
                CardView cv = PersonagemView.temModelo(carta.getId())
                        ? new CardView(carta, screenManager, true)
                        : new CardView(carta);
                grade.getChildren().add(cv);
                total++;
            }
        }
        contador.setText(total + (total == 1 ? " carta" : " cartas"));
        Efeitos.aparecer(grade, 180);
        Platform.runLater(this::atualizarVisiveis);
    }

    /** Mostra o modelo 3D das cartas na área visível (com folga de uma carta) e remove o das demais. */
    private void atualizarVisiveis() {
        if (getScene() == null) return;
        Bounds vp = rolagem.localToScene(rolagem.getLayoutBounds());
        double folga = CardView.ALTURA_PADRAO;
        for (Node n : grade.getChildren()) {
            if (!(n instanceof CardView)) continue;
            CardView cv = (CardView) n;
            Bounds b = cv.localToScene(cv.getBoundsInLocal());
            boolean visivel = b.getMaxY() > vp.getMinY() - folga && b.getMinY() < vp.getMaxY() + folga;
            if (visivel) cv.mostrarModelo(); else cv.ocultarModelo();
        }
    }
}
