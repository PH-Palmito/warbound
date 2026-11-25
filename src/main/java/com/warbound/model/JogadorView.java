package com.warbound.model;

import java.util.List;

/**
 * View-model de um jogador dentro da partida (local ou adversário).
 * Para o adversário, a mão vem só com a quantidade de cartas — nunca o
 * conteúdo (o guia de frontend proíbe mostrar as cartas do adversário).
 */
public class JogadorView {

    private final String nome;
    private final Classe classeInicial;
    private final int vidasRestantes;
    private final int recursos;
    private final List<Carta> mao;              // null para o adversário
    private final int quantidadeCartasNaMao;     // usado quando mao == null
    private final List<PersonagemCampoView> campo;   // tamanho fixo 3 (posições vazias = null)
    private final List<PersonagemCampoView> reserva;
    private final String buffDeGrupoAtivo;       // texto do buff, ou null

    public JogadorView(String nome, Classe classeInicial, int vidasRestantes, int recursos,
                        List<Carta> mao, int quantidadeCartasNaMao,
                        List<PersonagemCampoView> campo, List<PersonagemCampoView> reserva,
                        String buffDeGrupoAtivo) {
        this.nome = nome;
        this.classeInicial = classeInicial;
        this.vidasRestantes = vidasRestantes;
        this.recursos = recursos;
        this.mao = mao;
        this.quantidadeCartasNaMao = quantidadeCartasNaMao;
        this.campo = campo;
        this.reserva = reserva;
        this.buffDeGrupoAtivo = buffDeGrupoAtivo;
    }

    public String getNome() { return nome; }
    public Classe getClasseInicial() { return classeInicial; }
    public int getVidasRestantes() { return vidasRestantes; }
    public int getRecursos() { return recursos; }
    public List<Carta> getMao() { return mao; }
    public int getQuantidadeCartasNaMao() { return mao != null ? mao.size() : quantidadeCartasNaMao; }
    public List<PersonagemCampoView> getCampo() { return campo; }
    public List<PersonagemCampoView> getReserva() { return reserva; }
    public String getBuffDeGrupoAtivo() { return buffDeGrupoAtivo; }
}
