# ADR 0003 — Idempotência e persistência da máquina de estados de pagamento

- Status: aceito
- Data: 2026-08-13

## Contexto

O requisito não-funcional central do case é **evitar cobrança duplicada** em reenvio de
ação, e registrar resultados (aprovado, negado, cancelado) de forma confiável. O
pagamento sai do app para a Cielo e volta por callback — um ponto clássico para toques
repetidos, callbacks duplicados e processos interrompidos.

## Decisão

O Room é a fonte de verdade. A máquina de estados
(`DRAFT → PAYMENT_IN_PROGRESS → APPROVED | DENIED | CANCELLED | FAILED | PENDING`) é
protegida por operações **atômicas e condicionais**:

- **Gate:** `markInProgressIfPayable` só move para `PAYMENT_IN_PROGRESS` a partir de um
  estado pagável, na mesma transação em que a tentativa é inserida. A Cielo só abre
  **depois** desse lock — garantido pelo `StartPaymentUseCase`.
- **Guarda de UI:** o `CheckoutViewModel` ignora toques enquanto uma submissão está em
  andamento e reutiliza o mesmo rascunho, então dois toques não criam duas compras.
- **Callback idempotente:** `applyResultIfActive` só aplica o resultado à tentativa ativa;
  callback repetido retorna `AlreadyApplied` e um resultado divergente para tentativa
  terminal retorna `ConflictIgnored` (registrado só para diagnóstico).
- **Ingresso único:** criado com `INSERT OR IGNORE` na mesma transação da aprovação.
- **Recuperação conservadora:** tentativas `PAYMENT_IN_PROGRESS` de um processo anterior
  viram `PENDING` no start; o app nunca assume aprovação nem reenvia cobrança.

Valores monetários são sempre `Long` em centavos — nunca `Double`.

## Consequências positivas

- Duplo toque, callback duplicado e reinício de processo não geram segunda cobrança,
  segunda aprovação nem segundo ingresso.
- Regras testadas de forma determinística (unit + Room instrumentado + jornada com fake).
- `PENDING` conservador prioriza segurança financeira quando o resultado é ambíguo.

## Consequências negativas

- `PENDING` exige ação do usuário (consultar histórico); sem reconciliação automática.
- Idempotência mora em SQL condicional, que precisa de leitura cuidadosa.

## Alternativas rejeitadas

- **Só guarda de UI (flag `isSubmitting`):** não protege contra callback duplicado nem
  processo morto/reiniciado. A atomicidade no banco é a real linha de defesa.
- **Dinheiro em `Double`/`BigDecimal` de ponto flutuante:** risco de arredondamento;
  centavos em `Long` são exatos.
