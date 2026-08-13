# ADR 0002 — Aplicativo monomódulo organizado por feature

- Status: aceito
- Data: 2026-08-13

## Contexto

O produto é um case com prazo de três dias e escopo enxuto (catálogo local, checkout,
pagamento, comprovante e ingresso). Precisamos de uma estrutura que reduza atrito de
build e configuração sem impedir evolução futura.

## Decisão

Manter um único módulo Gradle `:app`, organizado **por feature** (`events`, `checkout`,
`purchase`, `ticket`) com núcleos compartilhados (`model`, `database`, `payment`,
`navigation`, `designsystem`, `di`). O fluxo é unidirecional: `Compose → ViewModel →
UseCase/Repository → DataSource`. Casos de uso existem só quando há regra de negócio ou
coordenação de mais de um repositório (ex.: `StartPaymentUseCase`), evitando camadas
artificiais para leitura simples.

## Consequências positivas

- Build e configuração mais simples, proporcionais ao escopo e ao prazo.
- Limites por feature preservam coesão e permitem extrair módulos depois.
- Dependências externas atrás de interfaces (`PaymentGateway`, `EventRepository`,
  `PurchaseRepository`, `QrCodeEncoder`) — fáceis de fakear em teste.

## Consequências negativas

- Sem fronteiras de compilação fortes entre features (disciplina fica por convenção).
- Um único módulo recompila mais do que módulos independentes em projetos grandes.

## Alternativas rejeitadas

- **Multimódulo desde o início:** ganho real aparece com equipes, reuso e ciclos de
  release independentes — nenhum presente neste case. Seria cerimônia sem retorno.
