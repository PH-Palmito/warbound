package com.warbound.model;

import java.util.List;

/**
 * Retrato completo do estado da partida, como a tela de Partida espera
 * receber do servidor (hoje, do GameClientMock). Equivale ao retorno de um
 * método remoto como obterEstadoDoJogo() no contrato RMI real.
 */
public class EstadoPartidaDTO {

    private final StatusPartida status;
    private final boolean turnoDoJogadorLocal;
    private final JogadorView jogadorLocal;
    private final JogadorView adversario;
    private final List<String> historicoDeAcoesPublicas;
    private final String mensagem; // resultado da última ação ou erro
    private final long versao;
    private final EscolhaPendenteDTO escolhaPendente;

    public EstadoPartidaDTO(StatusPartida status, boolean turnoDoJogadorLocal,
                             JogadorView jogadorLocal, JogadorView adversario,
                             List<String> historicoDeAcoesPublicas, String mensagem) {
        this(status, turnoDoJogadorLocal, jogadorLocal, adversario, historicoDeAcoesPublicas, mensagem, -1);
    }

    public EstadoPartidaDTO(StatusPartida status, boolean turnoDoJogadorLocal,
                             JogadorView jogadorLocal, JogadorView adversario,
                             List<String> historicoDeAcoesPublicas, String mensagem, long versao) {
        this(status, turnoDoJogadorLocal, jogadorLocal, adversario, historicoDeAcoesPublicas, mensagem, versao, null);
    }

    public EstadoPartidaDTO(StatusPartida status, boolean turnoDoJogadorLocal,
                             JogadorView jogadorLocal, JogadorView adversario,
                             List<String> historicoDeAcoesPublicas, String mensagem, long versao,
                             EscolhaPendenteDTO escolhaPendente) {
        this.status = status;
        this.turnoDoJogadorLocal = turnoDoJogadorLocal;
        this.jogadorLocal = jogadorLocal;
        this.adversario = adversario;
        this.historicoDeAcoesPublicas = historicoDeAcoesPublicas;
        this.mensagem = mensagem;
        this.versao = versao;
        this.escolhaPendente = escolhaPendente;
    }

    public StatusPartida getStatus() { return status; }
    public boolean isTurnoDoJogadorLocal() { return turnoDoJogadorLocal; }
    public JogadorView getJogadorLocal() { return jogadorLocal; }
    public JogadorView getAdversario() { return adversario; }
    public List<String> getHistoricoDeAcoesPublicas() { return historicoDeAcoesPublicas; }
    public String getMensagem() { return mensagem; }
    public long getVersao() { return versao; }
    public EscolhaPendenteDTO getEscolhaPendente() { return escolhaPendente; }
}
