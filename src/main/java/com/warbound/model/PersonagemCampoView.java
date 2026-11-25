package com.warbound.model;

/**
 * View-model de uma personagem em campo (ou na reserva). Guarda só o que a
 * interface precisa exibir — estados visuais que o servidor informa
 * (protegida, com Fúria, carregando etc.), nunca a regra que os produz.
 *
 * Uma vaga de campo vazia é representada por "null" nas listas de
 * PersonagemCampoView (ver List&lt;PersonagemCampoView&gt; em JogadorView).
 */
public class PersonagemCampoView {

    private final Carta carta;
    private final int hpAtual;
    private final boolean podeAtacar;
    private final boolean jaAtacou;
    private final Integer turnosDeCarregamentoRestantes; // null = não está carregando
    private final boolean protegida;
    private final boolean furiaAtiva;
    private boolean selecionada;

    public PersonagemCampoView(Carta carta, int hpAtual, boolean podeAtacar, boolean jaAtacou,
                                Integer turnosDeCarregamentoRestantes, boolean protegida,
                                boolean furiaAtiva) {
        this.carta = carta;
        this.hpAtual = hpAtual;
        this.podeAtacar = podeAtacar;
        this.jaAtacou = jaAtacou;
        this.turnosDeCarregamentoRestantes = turnosDeCarregamentoRestantes;
        this.protegida = protegida;
        this.furiaAtiva = furiaAtiva;
    }

    public Carta getCarta() { return carta; }
    public int getHpAtual() { return hpAtual; }
    public boolean isPodeAtacar() { return podeAtacar; }
    public boolean isJaAtacou() { return jaAtacou; }
    public Integer getTurnosDeCarregamentoRestantes() { return turnosDeCarregamentoRestantes; }
    public boolean isCarregando() { return turnosDeCarregamentoRestantes != null && turnosDeCarregamentoRestantes > 0; }
    public boolean isProtegida() { return protegida; }
    public boolean isFuriaAtiva() { return furiaAtiva; }
    public boolean isSelecionada() { return selecionada; }
    public void setSelecionada(boolean selecionada) { this.selecionada = selecionada; }
}
