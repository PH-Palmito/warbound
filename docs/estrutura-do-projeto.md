# Estrutura do projeto

O código é organizado em três áreas para que a interface gráfica, a comunicação distribuída e as regras do jogo possam evoluir sem dependências desnecessárias.

```text
com.warbound
├── shared
│   ├── remote       # contrato Java RMI
│   └── dto          # objetos serializáveis entre cliente e servidor
├── server
│   ├── config       # portas e configuração de publicação RMI
│   ├── service      # casos de uso e gerência das partidas
│   └── model        # estado interno, baralho, cartas e regras
└── client
    ├── network      # cliente RMI e conexão com o Registry
    └──              # aplicação e interface JavaFX
```

## Backend

`ServerMain` publica o objeto remoto no RMI Registry. `WarboundService` é apenas o adaptador da interface remota e delega as operações a `MatchService`. A regra de negócio e o estado interno ficam em `MatchState` e nas entidades de `model`.

## Frontend

`WarboundClientApp` concentra a experiência JavaFX. Ela não consulta o Registry diretamente: utiliza `RmiGameClient`, que encapsula a obtenção do Stub RMI e as chamadas remotas. Assim, uma mudança futura de RMI para outro protocolo fica limitada à camada `client.network` e ao contrato compartilhado.
