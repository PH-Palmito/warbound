package com.warbound.model;

/**
 * Uma linha da lista de salas abertas, na Sala de Partida.
 * Dado vindo do servidor (ou, por enquanto, do GameClientMock).
 */
public class SalaInfo {

    private final String codigo;
    private final String nomeCriador;
    private final Classe classeEscolhida;

    public SalaInfo(String codigo, String nomeCriador, Classe classeEscolhida) {
        this.codigo = codigo;
        this.nomeCriador = nomeCriador;
        this.classeEscolhida = classeEscolhida;
    }

    public String getCodigo() { return codigo; }
    public String getNomeCriador() { return nomeCriador; }
    public Classe getClasseEscolhida() { return classeEscolhida; }

    @Override
    public String toString() {
        return nomeCriador + "  ·  aguardando jogador";
    }
}
