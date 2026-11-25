package com.warbound.remote;

import com.warbound.model.Classe;
import com.warbound.model.EstadoPartidaDTO;
import com.warbound.model.SalaInfo;

import java.util.List;

/**
 * Borda do frontend com o backend — NÃO é a interface remota RMI.
 *
 * A interface remota de verdade (a que estende java.rmi.Remote, lança
 * RemoteException, e é registrada no RMI Registry) deve ser projetada e
 * implementada pela equipe, sem IA, como parte do backend. Esta interface
 * Java comum serve só para a UI não depender diretamente de "como" a
 * comunicação é feita: hoje aponta para GameClientMock (dados simulados);
 * quando o backend existir, a equipe cria uma implementação que por baixo
 * dos panos chama o stub RMI e faz o "de-para" para estes mesmos métodos.
 *
 * Nenhum método aqui decide o jogo — cada um só pede uma ação ou um estado
 * e devolve o que a "resposta do servidor" traria, conforme o guia de
 * frontend: "O frontend não calcula [...]. Ele pede a ação e renderiza a
 * resposta do servidor."
 */
public interface GameClient {

    /** Lista as salas abertas no lobby (equivalente a uma consulta remota ao coordenador). */
    List<SalaInfo> listarSalasAbertas(String enderecoServidor);

    /** Cria uma sala nova e retorna o código gerado para compartilhar com o adversário. */
    String criarSala(String enderecoServidor, String nomeJogador, Classe classeEscolhida);

    /** Entra em uma sala já existente, pelo código ou pela seleção na lista. */
    void entrarNaSala(String enderecoServidor, String nomeJogador, Classe classeEscolhida, String codigoSala);

    /** Estado completo e atual da partida (equivalente a obterEstadoDoJogo()). */
    EstadoPartidaDTO obterEstadoDaPartida();

    /** Joga uma carta da mão; alvoSlotCampo é usado só por cartas de personagem (0..2), pode ser null. */
    EstadoPartidaDTO jogarCarta(String cartaId, Integer alvoSlotCampo);

    /** Ataque de uma personagem própria contra uma personagem adversária. */
    EstadoPartidaDTO atacar(int slotAtacante, int slotAlvoAdversario);

    /** Retira uma personagem viva do campo para a Reserva, sem perder vida. */
    EstadoPartidaDTO retiradaTatica(int slotCampo);

    /** Devolve uma personagem da Reserva para uma vaga vazia do campo. */
    EstadoPartidaDTO retornarDaReserva(String personagemId, int slotCampo);

    /** Ativa a habilidade da personagem escolhida sobre uma aliada. */
    EstadoPartidaDTO ativarHabilidade(int slotOrigem, int slotAlvo);

    /** Conclui uma escolha privada solicitada pelo servidor. */
    EstadoPartidaDTO resolverEscolha(String cartaId);

    /** Encerra o turno do jogador local. */
    EstadoPartidaDTO encerrarTurno();
}
