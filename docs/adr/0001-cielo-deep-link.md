# ADR 0001 — Integração com a Cielo Smart via Deep Link local

- Status: aceito
- Data: 2026-08-13

## Contexto

O case exige integração de pagamento com o ecossistema Cielo Smart e disponibiliza
um **Emulador Cielo** para uso local. A avaliação explicitamente **não** considera a
construção de um backend. O ambiente-alvo é um dispositivo/AVD Android 10 com o
emulador instalado.

A Cielo Smart oferece diferentes modelos de integração (apps públicos na Cielo Store,
apps privados, automação comercial e integração local no dispositivo). Para um app que
roda no próprio terminal e não pode depender de servidor, a integração **local via
Deep Link** é a recomendada.

## Decisão

Integrar pagamento abrindo a Cielo por `Intent.ACTION_VIEW` com uma URI
`lio://payment?request=<BASE64>&urlCallback=ticketflow://payment-result`, e receber o
retorno numa activity dedicada registrada com scheme/host próprios.

O contrato real foi validado num spike no Android 10 e registrado em
[`docs/spikes/cielo-deep-link-android10.md`](../spikes/cielo-deep-link-android10.md):
`value` inteiro em centavos, campos obrigatórios (`email`, `installments`,
`unitOfMeasure`), Base64 MIME nos callbacks e ausência de referência nos retornos de
cancelamento/erro.

Toda a integração fica atrás da porta `PaymentGateway`, permitindo um fake determinístico
nos testes. Um foreground service mantém o processo vivo durante a troca de apps.

## Consequências positivas

- Sem backend: foco total na engenharia Android, como pede o case.
- Contrato observado e documentado antes de expandir o app (menor risco).
- `PaymentGateway` isola a Cielo — testável sem depender de outro APK.

## Consequências negativas

- O callback de cancelamento/erro não traz referência; é preciso correlacionar com a
  tentativa `PAYMENT_IN_PROGRESS` ativa.
- Deep Link depende de o emulador estar instalado; tratamos `HandlerUnavailable`
  explicitamente.
- Sem reconciliação online, resultado ambíguo vira `PENDING` conservador.

## Alternativas rejeitadas

- **Integração remota/API na nuvem:** exigiria backend, fora do escopo e do foco.
- **SDK cliente de exemplo (LIO-SDK-Sample):** é app de exemplo, não resolve o Deep Link
  de pagamento sem o emulador oficial.
