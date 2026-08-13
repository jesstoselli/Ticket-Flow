# Roteiro de apresentação (10–15 min)

## Preparação (antes de começar)

- [ ] AVD **Android 10 (API 29)** iniciado.
- [ ] **Emulador Cielo** instalado (`adb install -r lio-emulator.apk`).
- [ ] App instalado: `./gradlew installDebug` ou `adb install -r app/build/outputs/apk/debug/app-debug.apk`.
- [ ] Dados limpos, se quiser começar do zero: `adb shell pm clear com.jesstoselli.ticketflow`.
- [ ] Emulador Cielo pronto para retornar **Sucesso**, **Cancelado** e **Erro**.

## Roteiro

1. **Problema e escopo (1 min)** — venda presencial de ingressos com pagamento Cielo,
   foco em confiabilidade: anti-duplicidade, estados explícitos, persistência.

2. **Arquitetura (2 min)** — monomódulo por feature, fluxo unidirecional, dependências
   externas atrás de interfaces. Mostrar os diagramas do README e os três ADRs.

3. **Demo — caminho feliz (3 min)** — evento → checkout (quantidade/total) →
   *Pagar com Cielo* → **Sucesso** → comprovante → **QR Code**. Fechar e reabrir o app,
   abrir o histórico: a compra e o ingresso persistem.

4. **Demo — resiliência (3 min)**
   - **Duplo toque** em *Pagar com Cielo*: uma única tentativa/cobrança.
   - **Cancelado**/**Erro** no emulador: estado explícito + *Tentar novamente*.
   - Repetir o mesmo callback: **um** ingresso (idempotência).

5. **Room + máquina de estados (2 min)** — gate atômico (`markInProgressIfPayable`),
   `applyResultIfActive`, ingresso `INSERT OR IGNORE`, recuperação conservadora → `PENDING`.

6. **Testes (2 min)** — `./gradlew testDebugUnitTest` (lógica crítica) e
   `./gradlew connectedDebugAndroidTest` (Room, telas e jornada com `FakePaymentGateway`).

7. **Uso de IA (1 min)** — TDD por tarefa, decisões versionadas, e o que foi/ não foi
   validado (ver `docs/ai/usage-log.md`).

8. **Trade-offs e próximos passos (1 min)** — sem backend por escopo; reconciliação de
   `PENDING`, assinatura do QR e multimódulo como evoluções.

## Se algo falhar na demo

- Deep Link não abre → confirmar o Emulador Cielo instalado (o app mostra *handler
  indisponível*).
- Estado preso em processando → é o comportamento conservador; abrir o histórico mostra
  `PENDING`, sem reenvio automático.
