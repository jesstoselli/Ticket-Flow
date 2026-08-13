# Ticket Flow Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Entregar um aplicativo Android funcional de venda de ingressos, com pagamento local via Cielo Smart, persistencia idempotente, comprovante e QR Code para compras aprovadas.

**Architecture:** Aplicativo monomodulo organizado por feature, com fluxo unidirecional Compose -> ViewModel -> repositorio/caso de uso. Room sera a fonte de verdade das compras; a integracao Cielo ficara atras de `PaymentGateway`, com callback Deep Link normalizado para o dominio e implementacao fake nos testes.

**Tech Stack:** Kotlin, Jetpack Compose/Material 3, Navigation Compose, Coroutines/Flow, Room, Hilt, Kotlin Serialization, ZXing, JUnit, Turbine, MockK e GitHub Actions.

**Spec:** `docs/superpowers/specs/2026-08-13-ticket-flow-design.md`

## Global Constraints

- Aplicacao Android em Kotlin e Jetpack Compose; layouts XML nao serao usados para telas.
- Projeto com um unico modulo Gradle `:app`, organizado por feature.
- `applicationId` e `namespace`: `com.jesstoselli.ticketflow`.
- `minSdk = 24`, cobrindo Android 7 e Android 10; `compileSdk = 36` e `targetSdk = 36`.
- Toolchain Java 17 para Android/Gradle, mesmo que a JDK hospedeira seja mais nova.
- Valores monetarios sempre em `Long` e centavos; nunca `Double`.
- Sem backend, login, analytics ou dependencia de rede para o catalogo.
- Nenhuma credencial, token ou payload sensivel pode ser commitado ou escrito em logs.
- A Cielo deve ser aberta somente depois de a tentativa estar persistida como `PaymentInProgress`.
- `PaymentInProgress`, `Pending` e `Approved` nao permitem novo pagamento.
- Callback duplicado e idempotente; resultado conflitante nao substitui resultado terminal.
- QR Code e ingresso existem somente para compra aprovada e sao gerados uma unica vez.
- O primeiro risco validado sera o Deep Link real no Emulador Cielo em Android 10.
- Antes de adotar uma versao diferente das fixadas no catalogo, consultar documentacao atual e registrar o motivo.

## Dependency Baseline

Usar inicialmente AGP `8.8.2`, Kotlin/Compose plugin `2.1.0`, Gradle `8.10.2`, Hilt `2.56.2`, Room `2.7.1`, Navigation Compose `2.9.0`, Activity Compose `1.10.1`, Lifecycle `2.9.0`, Kotlin Serialization `1.8.1`, ZXing Core `3.5.3`, Turbine `1.2.0` e MockK `1.14.2`. A Compose BOM deve ser a release estavel mais recente resolvida pelo Google Maven durante Task 1. Se a matriz nao resolver com o SDK local, manter AGP/Kotlin e escolher a BOM estavel mais nova que conclua `assembleDebug` e `testDebugUnitTest`; registrar a versao efetiva em `gradle/libs.versions.toml` e no README.

---

## File Map

Arquivos de configuracao:

- `settings.gradle.kts`: repositorios e modulo `:app`.
- `build.gradle.kts`: plugins raiz.
- `gradle/libs.versions.toml`: unica fonte de versoes e aliases.
- `gradle/wrapper/gradle-wrapper.properties`: Gradle Wrapper.
- `app/build.gradle.kts`: Android, BuildConfig, Compose, Hilt, Room e testes.
- `app/src/main/AndroidManifest.xml`: app, callback activity, foreground service e queries do Deep Link.
- `local.properties.example`: nomes das propriedades da Cielo sem valores reais.

Entrada e composicao:

- `TicketFlowApplication.kt`: inicializacao Hilt.
- `MainActivity.kt`: host Compose e recebimento de intents.
- `TicketFlowApp.kt`: tema, scaffold e navegacao principal.
- `di/AppModule.kt`: bindings de relogio/IDs, catalogo, gateway e repositorios.
- `navigation/TicketFlowDestination.kt`: rotas tipadas.
- `navigation/TicketFlowNavHost.kt`: grafo e callbacks de navegacao.

Dominio e dados:

- `model/Event.kt`, `Purchase.kt`, `PurchaseStatus.kt`, `PaymentAttempt.kt`, `Ticket.kt`: modelos imutaveis.
- `model/MoneyFormatter.kt`: formatacao BRL sem alterar o valor em centavos.
- `events/data/LocalEventRepository.kt`: catalogo deterministico.
- `events/domain/EventRepository.kt`: contrato do catalogo.
- `database/*`: banco, entidades, DAOs, converters e mappers.
- `purchase/domain/PurchaseRepository.kt`: operacoes atomicas do dominio.
- `purchase/data/OfflinePurchaseRepository.kt`: implementacao Room.
- `checkout/domain/StartPaymentUseCase.kt`: gate idempotente e lancamento do gateway.
- `payment/domain/PaymentGateway.kt`: porta da Cielo.
- `payment/cielo/CieloPaymentRequest.kt`: DTO serializavel.
- `payment/cielo/CieloDeepLinkFactory.kt`: JSON, Base64 e URI.
- `payment/cielo/CieloCallbackParser.kt`: URI de retorno para resultado normalizado.
- `payment/cielo/CieloDeepLinkPaymentGateway.kt`: resolve/abre intent.
- `payment/cielo/PaymentForegroundService.kt`: mantem processo vivo durante handoff.
- `payment/cielo/CieloCallbackActivity.kt`: recebe callback e delega persistencia.
- `payment/cielo/HandleCieloCallbackUseCase.kt`: idempotencia do retorno.

Features Compose:

- `events/ui/EventListScreen.kt`, `EventListViewModel.kt`: catalogo.
- `checkout/ui/CheckoutScreen.kt`, `CheckoutViewModel.kt`: quantidade, total e pagamento.
- `purchase/ui/PurchaseResultScreen.kt`, `PurchaseHistoryScreen.kt` e ViewModels: estados e comprovantes.
- `ticket/ui/TicketScreen.kt`, `TicketViewModel.kt`: ingresso aprovado.
- `ticket/domain/QrCodeEncoder.kt`: contrato de QR.
- `ticket/data/ZxingQrCodeEncoder.kt`: bitmap local.
- `designsystem/*`: tema, componentes, formatadores visuais e estados vazios.

Documentacao e automacao:

- `README.md`: entrega principal.
- `docs/adr/*.md`: decisoes arquiteturais.
- `docs/ai/usage-log.md`: harness e uso da IA.
- `.github/workflows/android.yml`: build, lint e testes.

---

### Task 1: Scaffold Android compilavel e CI minima

**Files:**
- Create: `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml`
- Create: `gradle/wrapper/gradle-wrapper.properties`, `gradlew`, `gradlew.bat`
- Create: `app/build.gradle.kts`, `app/proguard-rules.pro`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/java/com/jesstoselli/ticketflow/TicketFlowApplication.kt`
- Create: `app/src/main/java/com/jesstoselli/ticketflow/MainActivity.kt`
- Create: `app/src/main/java/com/jesstoselli/ticketflow/TicketFlowApp.kt`
- Create: `app/src/main/java/com/jesstoselli/ticketflow/designsystem/TicketFlowTheme.kt`
- Create: `app/src/main/res/values/strings.xml`, `themes.xml`
- Create: `app/src/test/java/com/jesstoselli/ticketflow/SmokeTest.kt`
- Create: `.gitignore`, `local.properties.example`, `.github/workflows/android.yml`

**Interfaces:**
- Consumes: nenhuma interface do projeto.
- Produces: app Hilt compilavel, `TicketFlowApp()` como raiz Compose e comandos Gradle reproduziveis.

- [ ] **Step 1: Gerar o wrapper e catalogo com a baseline fixada**

Criar aliases para Android application, Kotlin Android, Kotlin Compose, Kotlin Serialization, Hilt e KSP. Configurar `google()`, `mavenCentral()` e `gradlePluginPortal()`. Em `app/build.gradle.kts`, usar:

```kotlin
android {
    namespace = "com.jesstoselli.ticketflow"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.jesstoselli.ticketflow"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "com.jesstoselli.ticketflow.HiltTestRunner"
        buildConfigField("String", "CIELO_CLIENT_ID", cieloProperty("CIELO_CLIENT_ID"))
        buildConfigField("String", "CIELO_ACCESS_TOKEN", cieloProperty("CIELO_ACCESS_TOKEN"))
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true; buildConfig = true }
}
kotlin { jvmToolchain(17) }
```

`cieloProperty` deve ler `local.properties` e produzir um literal vazio escapado quando ausente, permitindo CI sem segredo.

- [ ] **Step 2: Criar smoke test antes da implementacao Compose**

```kotlin
class SmokeTest {
    @Test fun projectTestRuntimeIsConfigured() {
        assertEquals(4, 2 + 2)
    }
}
```

- [ ] **Step 3: Criar a raiz minima do aplicativo**

```kotlin
@HiltAndroidApp
class TicketFlowApplication : Application()

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { TicketFlowTheme { TicketFlowApp() } }
    }
}

@Composable
fun TicketFlowApp() {
    Surface { Text(stringResource(R.string.app_name)) }
}
```

- [ ] **Step 4: Resolver dependencias e verificar a baseline**

Run: `./gradlew testDebugUnitTest assembleDebug lintDebug --stacktrace`

Expected: `BUILD SUCCESSFUL`, `SmokeTest` PASS e APK em `app/build/outputs/apk/debug/app-debug.apk`. Se a Compose BOM proposta nao existir, consultar o metadata do Google Maven, fixar a release estavel resolvida e repetir exatamente os tres checks.

- [ ] **Step 5: Configurar CI minima**

Workflow deve usar JDK 17, cache Gradle e executar `./gradlew testDebugUnitTest lintDebug assembleDebug`. Nao instalar nem iniciar emulador nesta etapa.

- [ ] **Step 6: Commit**

```bash
git add .gitignore .github build.gradle.kts settings.gradle.kts gradle gradle.properties gradlew gradlew.bat app local.properties.example
git commit -m "build: scaffold Ticket Flow Android app"
```

### Task 2: Modelos de dominio, dinheiro e maquina de estados

**Files:**
- Create: `app/src/main/java/com/jesstoselli/ticketflow/model/Event.kt`
- Create: `app/src/main/java/com/jesstoselli/ticketflow/model/Purchase.kt`
- Create: `app/src/main/java/com/jesstoselli/ticketflow/model/PurchaseStatus.kt`
- Create: `app/src/main/java/com/jesstoselli/ticketflow/model/PaymentAttempt.kt`
- Create: `app/src/main/java/com/jesstoselli/ticketflow/model/PaymentResult.kt`
- Create: `app/src/main/java/com/jesstoselli/ticketflow/model/Ticket.kt`
- Create: `app/src/main/java/com/jesstoselli/ticketflow/model/MoneyFormatter.kt`
- Test: `app/src/test/java/com/jesstoselli/ticketflow/model/PurchaseTest.kt`
- Test: `app/src/test/java/com/jesstoselli/ticketflow/model/MoneyFormatterTest.kt`

**Interfaces:**
- Consumes: Kotlin/JVM apenas.
- Produces: `Purchase.canStartPayment: Boolean`, `Purchase.totalInCents: Long`, `PurchaseStatus`, `PaymentResult` e `formatBrl(Long): String`.

- [ ] **Step 1: Escrever testes falhando da regra monetaria**

```kotlin
@Test fun totalUsesIntegerCents() {
    val purchase = purchase(unitPriceInCents = 12_345, quantity = 3)
    assertEquals(37_035, purchase.totalInCents)
}

@Test fun brlFormattingIsDeterministic() {
    assertEquals("R$ 123,45", formatBrl(12_345, Locale("pt", "BR")))
}
```

Run: `./gradlew testDebugUnitTest --tests '*PurchaseTest' --tests '*MoneyFormatterTest'`

Expected: FAIL por tipos/funcoes ausentes.

- [ ] **Step 2: Implementar modelos imutaveis e dinheiro**

```kotlin
enum class PurchaseStatus { DRAFT, PAYMENT_IN_PROGRESS, APPROVED, DENIED, CANCELLED, FAILED, PENDING }

data class Purchase(
    val id: String,
    val eventId: String,
    val eventName: String,
    val quantity: Int,
    val unitPriceInCents: Long,
    val status: PurchaseStatus,
) {
    init { require(quantity > 0); require(unitPriceInCents >= 0) }
    val totalInCents: Long get() = Math.multiplyExact(unitPriceInCents, quantity.toLong())
    val canStartPayment: Boolean get() = status in setOf(DRAFT, DENIED, CANCELLED, FAILED)
}

sealed interface PaymentResult {
    data class Approved(val transactionId: String?, val authorizationCode: String?) : PaymentResult
    data class Denied(val reason: String?) : PaymentResult
    data class Cancelled(val reason: String?) : PaymentResult
    data class Failed(val code: String?, val reason: String?) : PaymentResult
}
```

- [ ] **Step 3: Escrever e passar testes de transicao**

Testar que somente `DRAFT`, `DENIED`, `CANCELLED` e `FAILED` possuem `canStartPayment == true`, e que quantidade zero/valor negativo lancam `IllegalArgumentException`.

Run: `./gradlew testDebugUnitTest --tests 'com.jesstoselli.ticketflow.model.*'`

Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/jesstoselli/ticketflow/model app/src/test/java/com/jesstoselli/ticketflow/model
git commit -m "feat: define purchase domain model"
```

### Task 3: Contrato Cielo e spike vertical no Emulador Android 10

**Files:**
- Create: `app/src/main/java/com/jesstoselli/ticketflow/payment/domain/PaymentGateway.kt`
- Create: `app/src/main/java/com/jesstoselli/ticketflow/payment/cielo/CieloPaymentRequest.kt`
- Create: `app/src/main/java/com/jesstoselli/ticketflow/payment/cielo/CieloDeepLinkFactory.kt`
- Create: `app/src/main/java/com/jesstoselli/ticketflow/payment/cielo/CieloCallbackParser.kt`
- Create: `app/src/main/java/com/jesstoselli/ticketflow/payment/cielo/CieloDeepLinkPaymentGateway.kt`
- Create: `app/src/main/java/com/jesstoselli/ticketflow/payment/cielo/CieloCallbackActivity.kt`
- Create: `app/src/main/java/com/jesstoselli/ticketflow/payment/cielo/PaymentForegroundService.kt`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/java/com/jesstoselli/ticketflow/TicketFlowApp.kt`
- Test: `app/src/test/java/com/jesstoselli/ticketflow/payment/cielo/CieloDeepLinkFactoryTest.kt`
- Test: `app/src/test/java/com/jesstoselli/ticketflow/payment/cielo/CieloCallbackParserTest.kt`
- Create: `docs/spikes/cielo-deep-link-android10.md`

**Interfaces:**
- Consumes: `Purchase`, `PaymentResult`, `BuildConfig.CIELO_CLIENT_ID`, `BuildConfig.CIELO_ACCESS_TOKEN`.
- Produces: `PaymentGateway.launch(PaymentLaunchRequest): PaymentLaunchResult`, `CieloDeepLinkFactory.create(...) : Uri`, `CieloCallbackParser.parse(Uri): CallbackParseResult`.

- [ ] **Step 1: Escrever testes falhando do request e callback**

```kotlin
@Test fun requestUsesTotalInCentsAndStableReference() {
    val uri = factory.create(request(reference = "attempt-123", total = 25_000))
    val decoded = decodeRequestQuery(uri)
    assertEquals("attempt-123", decoded.reference)
    assertEquals("25000", decoded.value)
    assertEquals("ticketflow://payment-result", uri.getQueryParameter("urlCallback"))
}

@Test fun cancelledCallbackIsNormalized() {
    val uri = callbackUri(json = """{"code":1,"reason":"CANCELADO PELO USUARIO"}""")
    val parsed = assertIs<CallbackParseResult.Valid>(parser.parse(uri))
    assertEquals(PaymentResult.Cancelled("CANCELADO PELO USUARIO"), parsed.result)
}
```

Run: `./gradlew testDebugUnitTest --tests '*Cielo*Test'`

Expected: FAIL por factory/parser ausentes.

- [ ] **Step 2: Implementar DTO, Base64 e parser defensivo**

```kotlin
@Serializable
data class CieloPaymentRequest(
    val accessToken: String,
    val clientID: String,
    val reference: String,
    val items: List<CieloItem>,
    val value: String,
)

sealed interface CallbackParseResult {
    data class Valid(val reference: String?, val result: PaymentResult) : CallbackParseResult
    data class Invalid(val reason: String) : CallbackParseResult
}
```

Usar `Base64.NO_WRAP`, UTF-8 e `Json { ignoreUnknownKeys = true; isLenient = false }`. Validar scheme `ticketflow`, host `payment-result`, existencia de `response` e Base64/JSON. Nao logar o payload bruto.

- [ ] **Step 3: Implementar gateway e manifest**

```kotlin
interface PaymentGateway {
    suspend fun launch(request: PaymentLaunchRequest): PaymentLaunchResult
}

sealed interface PaymentLaunchResult {
    data object Launched : PaymentLaunchResult
    data object HandlerUnavailable : PaymentLaunchResult
    data class ConfigurationError(val missingKeys: Set<String>) : PaymentLaunchResult
}
```

Declarar `<queries>` para o scheme Cielo, callback activity `exported="true"` com `ticketflow://payment-result`, foreground service `exported="false"` e permissao `FOREGROUND_SERVICE`. Antes de `startActivity`, usar `resolveActivity`; iniciar o service com `ContextCompat.startForegroundService`.

- [ ] **Step 4: Passar testes e instalar APK**

Run: `./gradlew testDebugUnitTest --tests '*Cielo*Test' assembleDebug`

Expected: PASS e APK gerado.

Run: `adb devices`

Expected: o AVD Android 10 aparece como `device`.

Run: `adb install -r app/build/outputs/apk/debug/app-debug.apk`

Expected: `Success`.

- [ ] **Step 5: Executar o spike real**

Adicionar temporariamente a raiz `TicketFlowApp` com campos fixos de evento/quantidade e um botao que chama o gateway. Validar manualmente no Emulador Cielo:

1. Deep Link abre o fluxo com R$ 10,00.
2. `Sucesso` retorna ao callback activity.
3. `Cancelado` retorna e normaliza cancelamento.
4. `Erro` retorna e normaliza falha.
5. Reabrir o app nao dispara o pagamento sozinho.

Registrar em `docs/spikes/cielo-deep-link-android10.md`: AVD/API, versao do Emulador Cielo, URI sem credenciais, shapes reais dos tres callbacks sanitizados, divergencias da documentacao e decisao final de contrato.

- [ ] **Step 6: Remover UI descartavel, manter contrato validado e rodar checks**

Run: `./gradlew testDebugUnitTest lintDebug assembleDebug`

Expected: PASS; nenhum token aparece em `git diff --cached` ou `rg -n 'accessToken|clientID' docs/spikes` fora de nomes de campo/placeholders.

- [ ] **Step 7: Commit**

```bash
git add app/src/main app/src/test docs/spikes
git commit -m "feat: validate Cielo deep link payment flow"
```

### Task 4: Room e repositorio idempotente de compras

**Files:**
- Create: `app/src/main/java/com/jesstoselli/ticketflow/database/TicketFlowDatabase.kt`
- Create: `app/src/main/java/com/jesstoselli/ticketflow/database/PurchaseEntity.kt`
- Create: `app/src/main/java/com/jesstoselli/ticketflow/database/PaymentAttemptEntity.kt`
- Create: `app/src/main/java/com/jesstoselli/ticketflow/database/TicketEntity.kt`
- Create: `app/src/main/java/com/jesstoselli/ticketflow/database/PurchaseDao.kt`
- Create: `app/src/main/java/com/jesstoselli/ticketflow/database/DatabaseMappers.kt`
- Create: `app/src/main/java/com/jesstoselli/ticketflow/purchase/domain/PurchaseRepository.kt`
- Create: `app/src/main/java/com/jesstoselli/ticketflow/purchase/data/OfflinePurchaseRepository.kt`
- Create: `app/src/androidTest/java/com/jesstoselli/ticketflow/database/PurchaseDaoTest.kt`

**Interfaces:**
- Consumes: modelos da Task 2.
- Produces: `createDraft`, `startAttempt`, `applyPaymentResult`, `observePurchase`, `observePurchases`, com resultado selado idempotente.

- [ ] **Step 1: Escrever testes instrumentados falhando**

```kotlin
@Test fun startingAttemptAtomicallyLocksPurchase() = runTest {
    dao.insertPurchase(draftEntity())
    assertEquals(1, dao.markInProgressIfPayable("purchase-1", "attempt-1", now = 100))
    assertEquals(0, dao.markInProgressIfPayable("purchase-1", "attempt-2", now = 101))
}

@Test fun duplicateApprovalCreatesOneTicket() = runTest {
    repository.applyPaymentResult("attempt-1", approved)
    repository.applyPaymentResult("attempt-1", approved)
    assertEquals(1, dao.countTicketsForPurchase("purchase-1"))
}
```

Run: `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.jesstoselli.ticketflow.database.PurchaseDaoTest`

Expected: FAIL por banco/DAO ausentes.

- [ ] **Step 2: Criar schema e indices**

Usar foreign keys e indices unicos:

```kotlin
@Entity(indices = [Index(value = ["reference"], unique = true)])
data class PaymentAttemptEntity(...)

@Entity(indices = [Index(value = ["purchaseId"], unique = true)])
data class TicketEntity(...)
```

O snapshot do evento vive em `PurchaseEntity`, evitando que alteracoes no catalogo mudem comprovantes antigos.

- [ ] **Step 3: Implementar operacoes transacionais condicionais**

```kotlin
interface PurchaseRepository {
    suspend fun createDraft(selection: PurchaseSelection): Purchase
    suspend fun startAttempt(purchaseId: String): StartAttemptResult
    suspend fun applyPaymentResult(reference: String, result: PaymentResult): ApplyResult
    fun observePurchase(id: String): Flow<Purchase?>
    fun observePurchases(): Flow<List<Purchase>>
}
```

`startAttempt` deve atualizar somente estados pagaveis e inserir a tentativa na mesma `withTransaction`. `applyPaymentResult` deve retornar `AlreadyApplied` para duplicata e `ConflictIgnored` para resultado terminal divergente; aprovacao insere ingresso com `INSERT OR IGNORE` na mesma transacao.

- [ ] **Step 4: Passar testes Room**

Cobrir: primeiro lock, segundo lock rejeitado, retry explicito apos denied/cancelled/failed, bloqueio pending/approved, callback duplicado, conflito e criacao unica de ticket.

Run: `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.jesstoselli.ticketflow.database.PurchaseDaoTest`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/jesstoselli/ticketflow/database app/src/main/java/com/jesstoselli/ticketflow/purchase app/src/androidTest
git commit -m "feat: persist purchases idempotently"
```

### Task 5: Catalogo local, design system e navegacao

**Files:**
- Create: `app/src/main/java/com/jesstoselli/ticketflow/events/domain/EventRepository.kt`
- Create: `app/src/main/java/com/jesstoselli/ticketflow/events/data/LocalEventRepository.kt`
- Create: `app/src/main/java/com/jesstoselli/ticketflow/events/ui/EventListViewModel.kt`
- Create: `app/src/main/java/com/jesstoselli/ticketflow/events/ui/EventListScreen.kt`
- Create: `app/src/main/java/com/jesstoselli/ticketflow/navigation/TicketFlowDestination.kt`
- Create: `app/src/main/java/com/jesstoselli/ticketflow/navigation/TicketFlowNavHost.kt`
- Create: `app/src/main/java/com/jesstoselli/ticketflow/designsystem/EventCard.kt`
- Modify: `TicketFlowApp.kt`, `TicketFlowTheme.kt`, `di/AppModule.kt`
- Test: `app/src/test/java/com/jesstoselli/ticketflow/events/ui/EventListViewModelTest.kt`
- Test: `app/src/androidTest/java/com/jesstoselli/ticketflow/events/ui/EventListScreenTest.kt`

**Interfaces:**
- Consumes: `Event`, `formatBrl`.
- Produces: `EventRepository.observeEvents(): Flow<List<Event>>`, `EventListUiState`, rota `checkout/{eventId}`.

- [ ] **Step 1: Escrever testes de ViewModel e UI falhando**

```kotlin
@Test fun eventsAreExposedAsContent() = runTest {
    viewModel.uiState.test {
        assertEquals(EventListUiState.Loading, awaitItem())
        assertEquals(events, (awaitItem() as EventListUiState.Content).events)
    }
}

@Test fun clickingEventOpensCheckout() {
    composeRule.onNodeWithText("Festival Aurora").performClick()
    assertEquals("event-aurora", openedEventId)
}
```

- [ ] **Step 2: Implementar catalogo deterministico e UI**

Fornecer 3 eventos com IDs estaveis e datas futuras fixadas no contexto do case. `EventCard` mostra nome, data, local, preco e disponibilidade. Incluir content descriptions e touch targets de no minimo 48dp.

- [ ] **Step 3: Implementar navegacao principal**

Usar destinos serializaveis ou rotas centralizadas, bottom navigation com `Eventos` e `Ingressos`, e ocultar a barra durante checkout/resultado/detalhe do ingresso.

- [ ] **Step 4: Rodar testes**

Run: `./gradlew testDebugUnitTest --tests '*EventListViewModelTest' connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.jesstoselli.ticketflow.events.ui.EventListScreenTest`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main app/src/test app/src/androidTest
git commit -m "feat: add local event catalog"
```

### Task 6: Checkout, gate de pagamento e estados de lancamento

**Files:**
- Create: `app/src/main/java/com/jesstoselli/ticketflow/checkout/domain/StartPaymentUseCase.kt`
- Create: `app/src/main/java/com/jesstoselli/ticketflow/checkout/ui/CheckoutViewModel.kt`
- Create: `app/src/main/java/com/jesstoselli/ticketflow/checkout/ui/CheckoutScreen.kt`
- Modify: `TicketFlowNavHost.kt`, `di/AppModule.kt`
- Test: `app/src/test/java/com/jesstoselli/ticketflow/checkout/domain/StartPaymentUseCaseTest.kt`
- Test: `app/src/test/java/com/jesstoselli/ticketflow/checkout/ui/CheckoutViewModelTest.kt`
- Test: `app/src/androidTest/java/com/jesstoselli/ticketflow/checkout/ui/CheckoutScreenTest.kt`

**Interfaces:**
- Consumes: `EventRepository`, `PurchaseRepository.startAttempt`, `PaymentGateway.launch`.
- Produces: `StartPaymentOutcome`, `CheckoutUiState`, navegacao para `purchase/{purchaseId}`.

- [ ] **Step 1: Escrever testes falhando do gate**

```kotlin
@Test fun attemptIsPersistedBeforeGatewayLaunch() = runTest {
    val calls = mutableListOf<String>()
    repository.onStart = { calls += "persist" }
    gateway.onLaunch = { calls += "launch"; PaymentLaunchResult.Launched }
    useCase(purchaseId)
    assertEquals(listOf("persist", "launch"), calls)
}

@Test fun secondTapDoesNotLaunchAgain() = runTest {
    coEvery { repository.startAttempt(any()) } returns StartAttemptResult.Blocked(PAYMENT_IN_PROGRESS)
    useCase(purchaseId)
    coVerify(exactly = 0) { gateway.launch(any()) }
}
```

- [ ] **Step 2: Implementar `StartPaymentUseCase`**

Se o gateway retornar handler/config ausente depois do lock, aplicar `PaymentResult.Failed` para liberar retry explicito. Se lancar com sucesso, manter `PAYMENT_IN_PROGRESS` ate callback ou recuperacao.

- [ ] **Step 3: Implementar checkout Compose**

Quantidade de 1 ate `min(event.availableTickets, 10)`, total reativo, resumo e CTA `Pagar com Cielo`. Desabilitar CTA quando `isSubmitting`; ignorar eventos adicionais enquanto a coroutine esta ativa.

- [ ] **Step 4: Passar testes unitarios e UI**

Cobrir calculo, limites, duplo toque, handler indisponivel, configuracao ausente e lancamento bem-sucedido.

Run: `./gradlew testDebugUnitTest --tests '*Checkout*Test' --tests '*StartPaymentUseCaseTest' connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.jesstoselli.ticketflow.checkout.ui.CheckoutScreenTest`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main app/src/test app/src/androidTest
git commit -m "feat: add idempotent checkout flow"
```

### Task 7: Processamento de callback, resultado e recuperacao pendente

**Files:**
- Create: `app/src/main/java/com/jesstoselli/ticketflow/payment/cielo/HandleCieloCallbackUseCase.kt`
- Modify: `CieloCallbackActivity.kt`, `PaymentForegroundService.kt`
- Create: `app/src/main/java/com/jesstoselli/ticketflow/purchase/ui/PurchaseResultViewModel.kt`
- Create: `app/src/main/java/com/jesstoselli/ticketflow/purchase/ui/PurchaseResultScreen.kt`
- Create: `app/src/main/java/com/jesstoselli/ticketflow/purchase/domain/MarkInterruptedPaymentsPendingUseCase.kt`
- Modify: `MainActivity.kt`, `TicketFlowNavHost.kt`
- Test: `app/src/test/java/com/jesstoselli/ticketflow/payment/cielo/HandleCieloCallbackUseCaseTest.kt`
- Test: `app/src/test/java/com/jesstoselli/ticketflow/purchase/domain/MarkInterruptedPaymentsPendingUseCaseTest.kt`
- Test: `app/src/androidTest/java/com/jesstoselli/ticketflow/payment/cielo/CieloCallbackActivityTest.kt`

**Interfaces:**
- Consumes: `CieloCallbackParser`, `PurchaseRepository.applyPaymentResult`.
- Produces: callback idempotente, `PurchaseResultUiState` e promocao conservadora de tentativas interrompidas para `PENDING`.

- [ ] **Step 1: Escrever testes falhando do callback**

```kotlin
@Test fun validCallbackIsAppliedOnceAndStopsService() = runTest {
    useCase(validApprovedUri)
    useCase(validApprovedUri)
    coVerify(exactly = 2) { repository.applyPaymentResult("attempt-1", approved) }
    verify(exactly = 1) { serviceController.stop() }
}

@Test fun malformedCallbackNeverApprovesPurchase() = runTest {
    assertIs<HandleCallbackOutcome.Invalid>(useCase(malformedUri))
    coVerify(exactly = 0) { repository.applyPaymentResult(any(), any()) }
}
```

- [ ] **Step 2: Implementar callback activity minima**

A activity nao renderiza UI propria. Em `onCreate` e `onNewIntent`, delega a URI ao use case, encerra o service e abre `MainActivity` com `purchaseId` resolvido. Callback invalido abre resultado tecnico sem fabricar aprovacao.

- [ ] **Step 3: Implementar resultado e retry explicito**

Cada estado possui titulo, explicacao e acoes seguras. `DENIED`, `CANCELLED` e `FAILED` exibem `Tentar novamente`, que chama o mesmo gate e cria nova tentativa. `PENDING` nao exibe retry; `APPROVED` oferece comprovante/ingresso.

- [ ] **Step 4: Implementar recuperacao conservadora**

No startup, marcar como `PENDING` apenas tentativas `PAYMENT_IN_PROGRESS` criadas antes do inicio atual do processo e sem callback terminal. Nunca usar timeout curto enquanto o foreground service da execucao atual esta ativo.

- [ ] **Step 5: Rodar testes**

Run: `./gradlew testDebugUnitTest --tests '*HandleCieloCallbackUseCaseTest' --tests '*MarkInterruptedPaymentsPendingUseCaseTest' connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.jesstoselli.ticketflow.payment.cielo.CieloCallbackActivityTest`

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add app/src/main app/src/test app/src/androidTest
git commit -m "feat: handle payment results safely"
```

### Task 8: Historico, comprovante e ingresso com QR Code

**Files:**
- Create: `app/src/main/java/com/jesstoselli/ticketflow/purchase/ui/PurchaseHistoryViewModel.kt`
- Create: `app/src/main/java/com/jesstoselli/ticketflow/purchase/ui/PurchaseHistoryScreen.kt`
- Create: `app/src/main/java/com/jesstoselli/ticketflow/purchase/ui/ReceiptContent.kt`
- Create: `app/src/main/java/com/jesstoselli/ticketflow/ticket/domain/QrCodeEncoder.kt`
- Create: `app/src/main/java/com/jesstoselli/ticketflow/ticket/data/ZxingQrCodeEncoder.kt`
- Create: `app/src/main/java/com/jesstoselli/ticketflow/ticket/ui/TicketViewModel.kt`
- Create: `app/src/main/java/com/jesstoselli/ticketflow/ticket/ui/TicketScreen.kt`
- Modify: `TicketFlowNavHost.kt`, `di/AppModule.kt`
- Test: `app/src/test/java/com/jesstoselli/ticketflow/ticket/data/ZxingQrCodeEncoderTest.kt`
- Test: `app/src/test/java/com/jesstoselli/ticketflow/ticket/ui/TicketViewModelTest.kt`
- Test: `app/src/androidTest/java/com/jesstoselli/ticketflow/purchase/ui/PurchaseHistoryScreenTest.kt`

**Interfaces:**
- Consumes: `PurchaseRepository.observePurchases`, ticket criado atomicamente na Task 4.
- Produces: historico persistente, comprovante e `QrCodeEncoder.encode(payload, sizePx): ImageBitmap`.

- [ ] **Step 1: Escrever testes falhando do acesso ao ingresso**

```kotlin
@Test fun approvedPurchaseExposesTicket() = runTest {
    repository.emit(approvedPurchaseWithTicket())
    assertIs<TicketUiState.Content>(viewModel.uiState.first { it !is Loading })
}

@Test fun nonApprovedPurchaseCannotRenderTicket() = runTest {
    repository.emit(deniedPurchase())
    assertEquals(TicketUiState.Unavailable, viewModel.uiState.first { it !is Loading })
}
```

- [ ] **Step 2: Implementar payload versionado e encoder ZXing**

```kotlin
@Serializable
data class TicketQrPayload(
    val version: Int = 1,
    val ticketId: String,
    val purchaseId: String,
    val eventId: String,
)
```

Serializar JSON compacto, gerar `BarcodeFormat.QR_CODE`, fundo branco e pixels pretos. Nao incluir preco, token, transactionId ou authorizationCode.

- [ ] **Step 3: Implementar historico, comprovante e ticket**

Historico agrupa compras pela data mais recente, mostra status e permite abrir resultado/comprovante. Comprovante mostra referencia, evento, quantidade, total e identificadores seguros. Ticket mostra QR, ID, evento, local e data.

- [ ] **Step 4: Rodar testes**

Run: `./gradlew testDebugUnitTest --tests '*Ticket*Test' --tests '*ZxingQrCodeEncoderTest' connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.jesstoselli.ticketflow.purchase.ui.PurchaseHistoryScreenTest`

Expected: PASS; teste do encoder confirma matriz quadrada com pixels claros e escuros e payload decodificavel.

- [ ] **Step 5: Commit**

```bash
git add app/src/main app/src/test app/src/androidTest
git commit -m "feat: add receipts and QR tickets"
```

### Task 9: Testes end-to-end, acessibilidade e hardening visual

**Files:**
- Create: `app/src/androidTest/java/com/jesstoselli/ticketflow/di/TestPaymentModule.kt`
- Create: `app/src/androidTest/java/com/jesstoselli/ticketflow/FakePaymentGateway.kt`
- Create: `app/src/androidTest/java/com/jesstoselli/ticketflow/PurchaseJourneyTest.kt`
- Create: `app/src/androidTest/java/com/jesstoselli/ticketflow/HiltTestRunner.kt`
- Modify: telas Compose e recursos afetados por acessibilidade/estado.

**Interfaces:**
- Consumes: fluxo completo das Tasks 4-8.
- Produces: jornadas instrumentadas deterministicas e app visualmente pronto para demonstracao.

- [ ] **Step 1: Escrever jornada feliz falhando**

```kotlin
@Test fun eventToApprovedQrTicket() {
    onNodeWithText("Festival Aurora").performClick()
    onNodeWithContentDescription("Aumentar quantidade").performClick()
    onNodeWithText("Pagar com Cielo").performClick()
    fakeGateway.completeApproved(reference = latestReference)
    onNodeWithText("Pagamento aprovado").assertIsDisplayed()
    onNodeWithText("Ver ingresso").performClick()
    onNodeWithContentDescription("QR Code do ingresso").assertIsDisplayed()
}
```

- [ ] **Step 2: Adicionar jornadas de risco**

Testar: duplo toque produz uma tentativa; cancelamento permite retry explicito; negativa mostra motivo; handler ausente mostra instrucao; callback duplicado gera um ticket; pending nao exibe pagar novamente; historico restaura depois de recriar activity.

- [ ] **Step 3: Auditar acessibilidade e layouts**

Verificar em Compose tests: content descriptions, headings, estados nao comunicados apenas por cor, touch target 48dp, fonte em escala 1.3 e ausencia de texto cortado. Fazer revisao manual em 360x640 e no AVD Android 10 alvo.

- [ ] **Step 4: Rodar suite completa**

Run: `./gradlew testDebugUnitTest connectedDebugAndroidTest lintDebug assembleDebug`

Expected: `BUILD SUCCESSFUL`, sem erro de lint e todas as jornadas PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main app/src/androidTest app/src/test
git commit -m "test: cover critical purchase journeys"
```

### Task 10: Documentacao, ADRs, harness de IA e entrega

**Files:**
- Create: `README.md`
- Create: `docs/adr/0001-cielo-deep-link.md`
- Create: `docs/adr/0002-monolith-by-feature.md`
- Create: `docs/adr/0003-idempotent-payment-state.md`
- Create: `docs/ai/usage-log.md`
- Create: `docs/presentation/demo-script.md`
- Modify: `.github/workflows/android.yml`

**Interfaces:**
- Consumes: comportamento e comandos finais de todas as tarefas.
- Produces: entrega publica reproduzivel e roteiro de code review.

- [ ] **Step 1: Escrever README executavel**

Incluir: visao geral/GIF ou screenshots, requisitos (Android Studio, JDK 17, SDK 36, AVD Android 10), instalacao do Emulador Cielo, copia de `local.properties.example`, propriedades opcionais do emulador, comandos de build/teste, fluxo de demonstracao, arquitetura, bibliotecas e justificativas, integracao Cielo, idempotencia, tratamento de erros, testes, trade-offs e evolucoes.

Validar cada comando do README em checkout limpo ou clone temporario sem credenciais reais.

- [ ] **Step 2: Registrar ADRs**

Cada ADR contem Contexto, Decisao, Consequencias positivas, Consequencias negativas e Alternativas rejeitadas. Explicar que multimodulo e evolucao condicionada a escala/equipes, nao melhoria automatica.

- [ ] **Step 3: Registrar uso de IA**

`docs/ai/usage-log.md` deve conter:

```markdown
## Registro
### Objetivo
### Contexto e restricoes fornecidos
### Prompt ou resumo fiel da interacao
### Resultado sugerido
### Decisao humana: aceito, alterado ou rejeitado
### Evidencia de validacao: teste, comando ou verificacao manual
```

Incluir planejamento, consulta da Cielo, geracao assistida, revisoes e correcoes. Remover segredos e dados pessoais.

- [ ] **Step 4: Criar roteiro de apresentacao**

Roteiro de 10-15 minutos: problema, arquitetura, demo feliz, cancelamento/duplicidade, Room/state machine, testes, uso da IA, trade-offs e proximos passos. Adicionar checklist para resetar dados do Emulador Cielo e deixar o AVD preparado.

- [ ] **Step 5: Verificacao final de entrega**

Run: `./gradlew clean testDebugUnitTest lintDebug assembleDebug`

Expected: PASS.

Run: `git grep -n -E '(access[_-]?token|client[_-]?id).*[=:][[:space:]]*[A-Za-z0-9]{12,}' -- ':!docs/superpowers' ':!local.properties.example'`

Expected: nenhuma credencial real.

Run: `git status --short && git log --oneline --decorate -12`

Expected: apenas arquivos intencionais antes do commit; historico incremental e legivel.

Executar manualmente no Android 10: aprovado, cancelado, erro, callback duplicado/reabertura, historico e QR Code.

- [ ] **Step 6: Commit**

```bash
git add README.md docs .github
git commit -m "docs: prepare Ticket Flow case delivery"
```

---

## Final Verification Gate

Antes de publicar o repositorio:

- [ ] `./gradlew clean testDebugUnitTest lintDebug assembleDebug` passa.
- [ ] `./gradlew connectedDebugAndroidTest` passa no AVD Android 10.
- [ ] Deep Link real foi validado no Emulador Cielo para sucesso, cancelamento e erro.
- [ ] Dois toques nao criam duas tentativas nem duas chamadas externas.
- [ ] Callback duplicado nao cria segundo ingresso.
- [ ] Processo interrompido aparece como pendente e nao reenvia cobranca.
- [ ] Compra aprovada persiste, reabre comprovante e exibe QR Code.
- [ ] README foi seguido do zero e nao depende de conhecimento oral.
- [ ] Nenhum segredo ou payload sensivel esta versionado.
- [ ] CI verde no repositorio publico.
- [ ] Demo e explicacao de trade-offs foram ensaiadas.
