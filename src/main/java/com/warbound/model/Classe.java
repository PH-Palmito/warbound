package com.warbound.model;

/**
 * As três facções jogáveis. O nome da classe (ex.: "Cavaleiro", "Samurai")
 * é o tipo de personagem; a facção (Templários/Orientais/Nórdicos) é o que
 * aparece no catálogo e na moldura da carta.
 *
 * Diferenciação apenas por cor, conforme decisão registrada no guia de
 * frontend (sem símbolo extra por enquanto).
 */
public enum Classe {
    TEMPLARIOS("Templários", "-color-templarios"),
    ORIENTAIS("Orientais", "-color-orientais"),
    NORDICOS("Nórdicos", "-color-nordicos");

    private final String nomeExibicao;
    private final String corCssVar;

    Classe(String nomeExibicao, String corCssVar) {
        this.nomeExibicao = nomeExibicao;
        this.corCssVar = corCssVar;
    }

    public String getNomeExibicao() {
        return nomeExibicao;
    }

    /** Nome da variável de cor definida em styles.css (-color-templarios etc.). */
    public String getCorCssVar() {
        return corCssVar;
    }

    /** Classe de estilo CSS usada em CardView para a borda/moldura da facção. */
    public String getCardStyleClass() {
        switch (this) {
            case TEMPLARIOS: return "card-view-templarios";
            case ORIENTAIS: return "card-view-orientais";
            default: return "card-view-nordicos";
        }
    }

    /** Nome em português, usado em qualquer lugar da UI que exiba o enum diretamente (ex.: ComboBox). */
    @Override
    public String toString() {
        return nomeExibicao;
    }
}
