package com.warbound.remote;

import com.warbound.data.CatalogoLocal;
import com.warbound.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementação FALSA de GameClient, usada enquanto o backend (RMI) não
 * existe ou não cobre ainda determinado estado.
 *
 * IMPORTANTE: as "regras" aplicadas aqui (quanto de dano um ataque causa,
 * quando o turno muda etc.) são simplificadas e só servem para a interface
 * ter algo para mostrar. Elas não são a especificação do jogo — isso é
 * definido e implementado exclusivamente pelo backend, pela equipe, sem IA.
 * Quando o contrato RMI estiver pronto, esta classe inteira é substituída
 * por uma implementação que chama o stub remoto; nenhuma tela deve precisar
 * mudar por causa dessa troca.
 */
public class GameClientMock implements GameClient {

    private final Map<String, Carta> cartasPorId = CatalogoLocal.todasAsCartas().stream()
            .collect(Collectors.toMap(Carta::getId, c -> c));

    private EstadoPartidaDTO estadoAtual = estadoInicialDeDemonstracao();

    @Override
    public List<SalaInfo> listarSalasAbertas(String enderecoServidor) {
        List<SalaInfo> salas = new ArrayList<>();
        salas.add(new SalaInfo("A1B2", "Lucas", Classe.TEMPLARIOS));
        salas.add(new SalaInfo("K9ZQ", "Emanoel", Classe.NORDICOS));
        salas.add(new SalaInfo("7P3X", "Mariana", Classe.ORIENTAIS));
        return salas;
    }

    @Override
    public String criarSala(String enderecoServidor, String nomeJogador, Classe classeEscolhida) {
        // TODO: substituir pela chamada remota real (ex.: coordenador.criarSala(...)).
        return "NOVA" + (int) (Math.random() * 9000 + 1000);
    }

    @Override
    public void entrarNaSala(String enderecoServidor, String nomeJogador, Classe classeEscolhida, String codigoSala) {
        // TODO: substituir pela chamada remota real (ex.: coordenador.entrar(...)).
        estadoAtual = estadoInicialDeDemonstracao();
    }

    @Override
    public EstadoPartidaDTO obterEstadoDaPartida() {
        return estadoAtual;
    }

    @Override
    public EstadoPartidaDTO jogarCarta(String cartaId, Integer alvoSlotCampo) {
        List<Carta> novaMao = new ArrayList<>(estadoAtual.getJogadorLocal().getMao());
        Carta carta = cartasPorId.get(cartaId);
        novaMao.removeIf(c -> c.getId().equals(cartaId));

        List<PersonagemCampoView> novoCampo = new ArrayList<>(estadoAtual.getJogadorLocal().getCampo());
        String mensagem;
        if (carta != null && carta.isPersonagem() && alvoSlotCampo != null
                && alvoSlotCampo >= 0 && alvoSlotCampo < novoCampo.size()
                && novoCampo.get(alvoSlotCampo) == null) {
            novoCampo.set(alvoSlotCampo, new PersonagemCampoView(carta, carta.getHp(),
                    true, false, null, false, false));
            mensagem = carta.getNome() + " entrou em campo (simulado).";
        } else {
            mensagem = carta != null
                    ? "Efeito de \"" + carta.getNome() + "\" aplicado (simulado)."
                    : "Ação recebida pelo servidor (simulado).";
        }

        JogadorView novoLocal = comNovaMaoECampo(estadoAtual.getJogadorLocal(), novaMao, novoCampo);
        estadoAtual = comHistoricoEMensagem(novoLocal, estadoAtual.getAdversario(), mensagem);
        return estadoAtual;
    }

    @Override
    public EstadoPartidaDTO atacar(int slotAtacante, int slotAlvoAdversario) {
        List<PersonagemCampoView> campoLocal = new ArrayList<>(estadoAtual.getJogadorLocal().getCampo());
        List<PersonagemCampoView> campoAdversario = new ArrayList<>(estadoAtual.getAdversario().getCampo());

        PersonagemCampoView atacante = campoLocal.get(slotAtacante);
        PersonagemCampoView alvo = campoAdversario.get(slotAlvoAdversario);
        String mensagem;

        if (atacante != null && atacante.isPodeAtacar() && alvo != null) {
            int dano = atacante.getCarta().getAtaque() != null ? atacante.getCarta().getAtaque() : 0;
            int hpRestante = Math.max(0, alvo.getHpAtual() - dano);
            campoAdversario.set(slotAlvoAdversario, hpRestante == 0 ? null
                    : new PersonagemCampoView(alvo.getCarta(), hpRestante, alvo.isPodeAtacar(),
                        alvo.isJaAtacou(), alvo.getTurnosDeCarregamentoRestantes(), alvo.isProtegida(), alvo.isFuriaAtiva()));
            campoLocal.set(slotAtacante, new PersonagemCampoView(atacante.getCarta(), atacante.getHpAtual(),
                    false, true, null, atacante.isProtegida(), atacante.isFuriaAtiva()));
            mensagem = atacante.getCarta().getNome() + " atacou e causou " + dano + " de dano (simulado).";
        } else {
            mensagem = "Ataque inválido (simulado).";
        }

        JogadorView novoLocal = comNovaMaoECampo(estadoAtual.getJogadorLocal(), estadoAtual.getJogadorLocal().getMao(), campoLocal);
        JogadorView novoAdversario = comNovaMaoECampo(estadoAtual.getAdversario(), null, campoAdversario);
        estadoAtual = comHistoricoEMensagem(novoLocal, novoAdversario, mensagem);
        return estadoAtual;
    }

    @Override
    public EstadoPartidaDTO retiradaTatica(int slotCampo) {
        List<PersonagemCampoView> campo = new ArrayList<>(estadoAtual.getJogadorLocal().getCampo());
        List<PersonagemCampoView> reserva = new ArrayList<>(estadoAtual.getJogadorLocal().getReserva());
        PersonagemCampoView personagem = campo.get(slotCampo);
        String mensagem;
        if (personagem != null) {
            campo.set(slotCampo, null);
            reserva.add(new PersonagemCampoView(personagem.getCarta(), personagem.getHpAtual(),
                    false, false, null, false, false));
            mensagem = personagem.getCarta().getNome() + " recuou para a Reserva (simulado).";
        } else {
            mensagem = "Não há personagem nessa vaga (simulado).";
        }
        JogadorView novoLocal = new JogadorView(estadoAtual.getJogadorLocal().getNome(),
                estadoAtual.getJogadorLocal().getClasseInicial(), estadoAtual.getJogadorLocal().getVidasRestantes(),
                estadoAtual.getJogadorLocal().getRecursos(), estadoAtual.getJogadorLocal().getMao(),
                estadoAtual.getJogadorLocal().getQuantidadeCartasNaMao(), campo, reserva,
                estadoAtual.getJogadorLocal().getBuffDeGrupoAtivo());
        estadoAtual = comHistoricoEMensagem(novoLocal, estadoAtual.getAdversario(), mensagem);
        return estadoAtual;
    }

    @Override
    public EstadoPartidaDTO retornarDaReserva(String personagemId, int slotCampo) {
        return estadoAtual;
    }

    @Override
    public EstadoPartidaDTO ativarHabilidade(int slotOrigem, int slotAlvo) {
        return estadoAtual;
    }

    @Override
    public EstadoPartidaDTO resolverEscolha(String cartaId) {
        return estadoAtual;
    }

    @Override
    public EstadoPartidaDTO encerrarTurno() {
        boolean proximoTurnoLocal = !estadoAtual.isTurnoDoJogadorLocal();
        String mensagem = proximoTurnoLocal ? "Seu turno começou (simulado)." : "Turno encerrado (simulado).";
        estadoAtual = new EstadoPartidaDTO(estadoAtual.getStatus(), proximoTurnoLocal,
                estadoAtual.getJogadorLocal(), estadoAtual.getAdversario(),
                adicionarAoHistorico(estadoAtual.getHistoricoDeAcoesPublicas(), mensagem), mensagem);
        return estadoAtual;
    }

    // ---------------- helpers internos (sem regra de jogo, só montagem de DTO) ----------------

    private JogadorView comNovaMaoECampo(JogadorView base, List<Carta> novaMao, List<PersonagemCampoView> novoCampo) {
        return new JogadorView(base.getNome(), base.getClasseInicial(), base.getVidasRestantes(),
                base.getRecursos(), novaMao != null ? novaMao : base.getMao(), base.getQuantidadeCartasNaMao(),
                novoCampo, base.getReserva(), base.getBuffDeGrupoAtivo());
    }

    private EstadoPartidaDTO comHistoricoEMensagem(JogadorView local, JogadorView adversario, String mensagem) {
        return new EstadoPartidaDTO(estadoAtual.getStatus(), estadoAtual.isTurnoDoJogadorLocal(),
                local, adversario, adicionarAoHistorico(estadoAtual.getHistoricoDeAcoesPublicas(), mensagem), mensagem);
    }

    private List<String> adicionarAoHistorico(List<String> atual, String novaLinha) {
        List<String> copia = new ArrayList<>(atual);
        copia.add(novaLinha);
        return copia;
    }

    /** Estado de exemplo mostrando vários estados visuais ao mesmo tempo (carregando, protegida, fúria, buff de grupo...). */
    private static EstadoPartidaDTO estadoInicialDeDemonstracao() {
        Map<String, Carta> catalogo = CatalogoLocal.todasAsCartas().stream()
                .collect(Collectors.toMap(Carta::getId, c -> c));

        List<Carta> maoLocal = List.of(
                catalogo.get("tpl_bencao"), catalogo.get("tpl_suprimentos"),
                catalogo.get("ori_iai"), catalogo.get("nor_chamado"));

        List<PersonagemCampoView> campoLocal = new ArrayList<>();
        campoLocal.add(new PersonagemCampoView(catalogo.get("tpl_cavaleiro"), 135, true, false, null, false, false));
        campoLocal.add(new PersonagemCampoView(catalogo.get("tpl_escudeiro"), 160, true, false, null, true, false));
        campoLocal.add(new PersonagemCampoView(catalogo.get("nor_viking"), 40, false, true, null, false, true)); // fúria (HP baixo)

        List<PersonagemCampoView> reservaLocal = List.of(
                new PersonagemCampoView(catalogo.get("nor_berserker"), 90, false, false, null, false, false));

        JogadorView jogadorLocal = new JogadorView("Você", Classe.TEMPLARIOS, 3, 4,
                maoLocal, maoLocal.size(), campoLocal, reservaLocal, null);

        List<PersonagemCampoView> campoAdversario = new ArrayList<>();
        campoAdversario.add(new PersonagemCampoView(catalogo.get("ori_samurai"), 100, false, true, null, false, false));
        campoAdversario.add(new PersonagemCampoView(catalogo.get("ori_ronin"), 100, false, false, 1, false, false)); // carregando
        campoAdversario.add(new PersonagemCampoView(catalogo.get("ori_monge"), 120, true, false, null, false, false));

        JogadorView adversario = new JogadorView("CPU de teste", Classe.ORIENTAIS, 3, 5,
                null, 3, campoAdversario, List.of(), "Bushido: +5 de ataque em cada Oriental");

        List<String> historico = List.of(
                "Você jogou Cavaleiro.",
                "CPU de teste jogou Samurai, Ronin e Monge.",
                "Grupo adversário completou Bushido.");

        return new EstadoPartidaDTO(StatusPartida.EM_ANDAMENTO, true, jogadorLocal, adversario,
                historico, "Partida iniciada (dados simulados).");
    }
}
