# Registro de uso de IA

Este documento descreve **como** a IA (Claude Code) foi usada na construção do Ticket
Flow, com objetivos, contexto/restrições fornecidos, o que foi sugerido, a decisão humana
e a evidência de validação. Princípio adotado: **não alegar validação que não aconteceu**.

## Guardrails aplicados a toda a colaboração

- Fonte de verdade das decisões: spec e plano de implementação versionados
  (`docs/superpowers/`). A IA implementou dentro desses limites.
- TDD por tarefa: teste primeiro, implementação depois, verificação com Gradle.
- Nenhum segredo, token ou payload sensível em código, log ou histórico.
- A IA rodou `testDebugUnitTest`, `lintDebug`, `assembleDebug` e `assembleDebugAndroidTest`.
  **Não** executou `connectedDebugAndroidTest` (sem AVD no ambiente) — os testes
  instrumentados foram escritos e compilados, mas a execução fica para validação manual.

---

## Registro

### Objetivo
Entender o case, o estado do projeto e planejar a continuidade.

**Contexto e restrições fornecidos:** PDF do case, spec, plano e spike da Cielo já no
repositório; projeto pessoal Android (trabalho direto, sem cerimônia corporativa).

**Prompt / resumo fiel:** "Leia a proposta e os docs; o que já foi feito e o que falta?"
Também: "consegue ler a doc da Cielo?" (link `conheca-a-cielo-smart`).

**Resultado sugerido:** mapa das Tasks 1–5 concluídas (commits) vs. 6–10 pendentes;
leitura da doc da Cielo (visão institucional; detalhe técnico já capturado no spike).

**Decisão humana:** aceito. Seguir pela Task 6 após confirmar o baseline verde.

**Evidência de validação:** `./gradlew testDebugUnitTest assembleDebug lintDebug` →
`BUILD SUCCESSFUL`, 20 testes unitários, 0 falhas.

---

### Objetivo
Task 6 — checkout e **gate idempotente** de pagamento.

**Contexto e restrições:** Cielo só pode abrir após tentativa persistida como
`PAYMENT_IN_PROGRESS`; dois toques não podem gerar duas cobranças.

**Prompt / resumo fiel:** "Dando verde, pode seguir pra Task 6."

**Resultado sugerido:** `StartPaymentUseCase` (gate + normalização dos desfechos do
gateway), `CheckoutViewModel` (guarda de UI + rascunho reutilizado), `CheckoutScreen`, e o
`InfrastructureModule` de Hilt que faltava.

**Decisão humana:** aceito e commitado (`feat: add idempotent checkout flow`).
Ajuste feito pela IA: o plano assumia `StartAttemptResult.Blocked(status)`, mas a API real
é `Rejected(purchase?)` — o código seguiu a API real.

**Evidência de validação:** `StartPaymentUseCaseTest` (5) e `CheckoutViewModelTest` (5)
verdes; build/lint OK.

---

### Objetivo
Task 7 — callback, tela de resultado e recuperação de pendentes.

**Contexto e restrições:** callback duplicado é idempotente; callback malformado nunca
aprova; cancelamento/erro **não trazem referência** (observado no spike).

**Prompt / resumo fiel:** "Pode commitar e seguimos para a Task 7."

**Resultado sugerido:** `HandleCieloCallbackUseCase` (idempotência delegada ao repositório;
correlação com a tentativa ativa quando falta referência), `MarkInterruptedPaymentsPendingUseCase`
+ `ProcessClock`, `PurchaseResultViewModel/Screen` e fiação da `CieloCallbackActivity`.

**Decisão humana:** aceito e commitado (`feat: handle payment results safely`).
Correção durante o build: faltava prover `CieloCallbackParser` no Hilt — a IA detectou pelo
erro do Dagger e adicionou o provider.

**Evidência de validação:** `HandleCieloCallbackUseCaseTest` (6),
`MarkInterruptedPaymentsPendingUseCaseTest` (1), `PurchaseResultViewModelTest` (4) verdes.

---

### Objetivo
Task 8 — histórico, comprovante e ingresso com QR Code.

**Contexto e restrições:** QR só para compra aprovada; sem preço/token no payload;
`ImageBitmap` exige Android (não testável em JVM puro).

**Prompt / resumo fiel:** "Pode commitar e seguir. Só adianta eu testar no final mesmo."

**Resultado sugerido:** `QrCodeEncoder`/`ZxingQrCodeEncoder` com a **matriz ZXing isolada**
em função pura (`encodeToMatrix`) para teste em JVM; `TicketViewModel/Screen`,
`PurchaseHistoryViewModel/Screen` e `ReceiptContent` reutilizável.

**Decisão humana:** aceito e commitado (`feat: add receipts and QR tickets`).

**Evidência de validação:** `ZxingQrCodeEncoderTest` (2 — matriz quadrada e **payload
decodificável** via encode→decode ZXing) e `TicketViewModelTest` (4) verdes.

---

### Objetivo
Task 9 — harness de testes instrumentados e jornada ponta a ponta.

**Resultado sugerido:** `HiltTestRunner`, `FakePaymentGateway`, `TestPaymentModule`
(`@TestInstallIn` substituindo o gateway real) e `PurchaseJourneyTest`. Extração do
`PaymentGatewayModule` para permitir a troca por fake.

**Decisão humana:** aceito e commitado (`test: cover critical purchase journeys`).

**Evidência de validação:** `assembleDebugAndroidTest` compila o harness; 47 testes
unitários verdes. **Não validado pela IA:** execução de `connectedDebugAndroidTest`
(requer AVD Android 10) — pendente de validação manual.

---

### Objetivo
Task 10 — documentação (README, ADRs, este registro, roteiro de demo, CI).

**Resultado sugerido:** README executável, três ADRs (Deep Link, monomódulo, idempotência),
roteiro de apresentação e ajuste da CI para compilar também o androidTest.

**Decisão humana:** aceito e commitado (`docs: prepare Ticket Flow case delivery`).

**Evidência de validação:** comandos do README conferidos contra o `build.gradle.kts` e a
CI reais; `./gradlew clean testDebugUnitTest lintDebug assembleDebug` como verificação final.
