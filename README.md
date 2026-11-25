# Warbound

Jogo de cartas multiplayer para dois jogadores, com JavaFX no cliente e Java RMI no servidor.

Cada jogador usa o mesmo baralho de 27 cartas de personagens e feitiços, embaralhado de forma independente. A partida possui três vagas no campo, Reserva, descarte reciclável, grupos de três personagens da mesma facção e três vidas. Ambos os jogadores começam seu primeiro turno com 2 Recursos e com ao menos uma personagem básica de custo 2 na mão inicial.

## Requisitos

- JDK 17 ou superior;
- conexão com a internet na primeira execução, para o Maven Wrapper baixar suas dependências.

## Executar no Windows

1. Execute `iniciar-servidor.bat` e pressione Enter para jogar neste computador. Para jogar pela rede local, informe o IP da máquina que está executando o servidor.
2. Execute `executar-front.bat` duas vezes, uma para cada jogador.
3. Na primeira janela, informe o nome e clique em **Criar partida**.
4. Na segunda, use **Atualizar salas**, selecione a sala criada e clique em **Entrar na sala**.

Também é possível informar o código da sala manualmente como alternativa.

## Executar pelo terminal

Inicie o servidor:

```powershell
.\mvnw.cmd compile exec:java "-Dexec.mainClass=com.warbound.server.ServerMain"
```

Em outros dois terminais, inicie um cliente em cada um:

```powershell
.\mvnw.cmd javafx:run
```

Para validar a compilação sem abrir o jogo:

```powershell
.\mvnw.cmd compile
```
