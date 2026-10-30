# Warbound

Card game multiplayer de povos históricos, como Romanos, Samurais e Vikings.

## Tecnologia definida

- Linguagem: Java 17+
- Interface: JavaFX
- Comunicação distribuída: Java RMI
- Arquitetura: cliente-servidor com estado autoritativo no servidor

## Protótipo inicial

O projeto já contém um servidor RMI, uma interface remota, estado de partida para dois jogadores e uma tela JavaFX inicial. A primeira versão permite criar ou entrar em uma partida, baixar unidades, atacar unidades ou o comandante adversário e encerrar o turno.

### Executar localmente

Pré-requisitos: JDK 17+ e Maven 3.9+.

Em um terminal, inicie o servidor:

```bash
mvn -q compile exec:java -Dexec.mainClass=com.warbound.server.ServerMain
```

Em dois outros terminais, abra os clientes:

```bash
mvn javafx:run
```

No primeiro cliente, informe o nome e o povo, clique em **Criar partida** e copie o identificador exibido no campo Partida. No segundo, informe nome, povo, cole esse identificador e clique em **Entrar na partida**.

Para partidas entre duas máquinas da mesma rede, execute o servidor com o IP local como argumento e informe esse mesmo IP no campo Servidor de cada cliente:

```bash
mvn -q compile exec:java -Dexec.mainClass=com.warbound.server.ServerMain -Dexec.args="192.168.0.10"
```

## Documentação

- [Entrega 1 - Introdução e arquitetura](docs/entrega-1-arquitetura.md)
- [Relatório Word da Entrega 1](docs/relatorio/Warbound_Entrega_1.docx)
- [Estrutura de backend e frontend](docs/estrutura-do-projeto.md)
