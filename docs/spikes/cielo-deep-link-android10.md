# Spike: Cielo Smart Deep Link no Android 10

Validado em 13/08/2026 em um AVD Android 10 (API 29), com o Emulador Cielo
oficial `br.com.cielosmart.orderservice`, versão `1.61.8` (`versionCode 1610800`).

## Ambiente

A página de instalação indicada no case fornece o arquivo
`lio-emulator.apk`. O APK `LIO-SDK-Sample-Integracao-Local` é somente um
aplicativo cliente de exemplo e não substitui o emulador: sem o APK oficial,
nenhuma activity resolve o Deep Link de pagamento.

O teste foi feito com credenciais placeholder aceitas pelo emulador. Nenhuma
credencial real, dado de cartão ou payload bruto é registrado pelo Ticket Flow.

## Request observado

O fluxo de checkout que funcionou usa:

```text
lio://payment?request=<BASE64_MIME_JSON>&urlCallback=ticketflow%3A%2F%2Fpayment-result
```

Shape sanitizado do JSON:

```json
{
  "accessToken": "<configurável>",
  "clientID": "<configurável>",
  "email": "ticketflow@sample.local",
  "installments": 1,
  "items": [
    {
      "name": "Festival Aurora",
      "quantity": 1,
      "sku": "<referência-estável>",
      "unitOfMeasure": "unidade",
      "unitPrice": 1000
    }
  ],
  "reference": "<referência-estável>",
  "value": 1000
}
```

O emulador abriu o checkout exibindo exatamente `R$ 10,00`.

## Callbacks observados

Todos os callbacks usam `ticketflow://payment-result?response=<BASE64>`.
O Base64 emitido pelo emulador contém quebras de linha cruas; o parser precisa
aceitar MIME Base64 sem depender de uma `java.net.URI` estrita.

Sucesso retorna o pedido completo, com shape mínimo relevante:

```json
{
  "reference": "<mesma-referência>",
  "status": "PAID",
  "payments": [
    {
      "id": "<transaction-id>",
      "authCode": "<authorization-code>"
    }
  ]
}
```

O retorno real também contém campos de cartão e metadados mock. Eles são
ignorados e nunca devem ser persistidos. O resultado normalizado é `Approved`.

Cancelamento:

```json
{"code":1,"reason":"CANCELADO PELO USUÁRIO"}
```

O resultado normalizado é `Cancelled`. O callback não traz a referência.

Erro:

```json
{"code":2,"reason":"Falha no processo de pagamento"}
```

O resultado normalizado é `Failed`. O callback não traz a referência.

## Divergências encontradas e decisão

- O checkout é `lio://payment`, não `lio://order`.
- `value` é um número inteiro em centavos, não uma string.
- `email`, `installments` e `unitOfMeasure` precisam estar presentes no JSON.
- Valores default do Kotlin Serialization devem ser codificados explicitamente.
- O Base64 dos callbacks pode conter quebras de linha não escapadas.
- Cancelamento e erro não incluem a referência; a tentativa ativa persistida é
  usada para correlacionar esses resultados na integração final.

O adapter definitivo mantém o request e o parser atrás de `PaymentGateway`,
valida a existência do handler antes do handoff e persiste somente referência,
transaction ID, authorization code e erro sanitizado.
