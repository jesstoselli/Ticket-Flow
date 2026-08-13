# Ticket Flow - Design da Solucao

## 1. Contexto e objetivo

Ticket Flow e um aplicativo Android para venda presencial de ingressos de eventos locais, desenvolvido como case tecnico para uma vaga Android Senior. O aplicativo deve demonstrar um fluxo de compra simples, integracao real com o Emulador Cielo Smart, tratamento explicito de falhas, prevencao de cobranca duplicada, persistencia local, testes automatizados e documentacao das decisoes tecnicas e do uso de IA.

O projeto sera implementado do zero em Kotlin e Jetpack Compose. Nao havera backend: o catalogo de eventos sera local e deterministico, pois a construcao de um servidor nao faz parte dos criterios de avaliacao nem do foco da vaga.

O QR Code faz parte do escopo da entrega. Ele somente sera gerado para uma compra confirmada como aprovada.

## 2. Escopo

### Incluido

- Catalogo local de eventos disponiveis.
- Detalhes do evento e selecao da quantidade de ingressos.
- Revisao da compra e calculo do total em centavos.
- Pagamento local pelo Emulador Cielo Smart via Deep Link.
- Registro dos resultados aprovado, negado, cancelado, falha tecnica e pendente.
- Prevencao de reenvio e cobranca duplicada.
- Comprovante da compra.
- Ingresso com QR Code vinculado a uma compra aprovada.
- Historico local de compras e ingressos.
- Testes automatizados dos cenarios criticos.
- CI para build, lint e testes unitarios.
- README, ADRs, spec, plano de implementacao e registro do uso de IA.

### Fora de escopo

- Backend ou sincronizacao remota do catalogo.
- Login, cadastro e perfis de usuario.
- Reserva distribuida de estoque.
- Favoritos, busca e filtros avancados.
- Compra de eventos diferentes no mesmo carrinho.
- Validacao remota ou leitura do QR Code na entrada do evento.
- Publicacao na Cielo Store ou Google Play.
- Impressao fisica do comprovante.

## 3. Direcao arquitetural

O projeto tera um unico modulo Gradle, `:app`, organizado por feature. Essa estrutura reduz configuracao e tempo de build para o tamanho atual do produto, mas preserva limites claros que permitam extrair modulos no futuro.

Features principais:

- `events`: catalogo e detalhes do evento.
- `checkout`: quantidade, resumo e inicio do pagamento.
- `purchase`: historico, comprovante e estados da compra.
- `ticket`: ingresso aprovado e QR Code.

Nucleos compartilhados:

- `model`: modelos de dominio e estados.
- `database`: Room, entidades, DAOs e mapeamentos.
- `payment`: contrato do gateway, implementacao Cielo e parser de callbacks.
- `navigation`: destinos e coordenacao da navegacao.
- `designsystem`: tema e componentes reutilizados.

A interface seguira fluxo unidirecional de dados:

```text
Compose Screen -> ViewModel -> Use Case/Repository -> Data Source
       ^              |
       +--- UI State -+
```

Casos de uso serao criados apenas para regras de negocio ou operacoes que coordenem mais de um repositorio. Leitura simples nao recebera camadas artificiais.

Dependencias externas ficarao atras de interfaces. Em especial, `PaymentGateway` permitira substituir a Cielo por uma implementacao fake nos testes.

## 4. Modelo de dados e fonte de verdade

O catalogo de eventos sera fornecido por uma implementacao local de `EventRepository`. Eventos terao identificador estavel, nome, descricao, data, local, preco unitario em centavos e quantidade disponivel para exibicao.

Room sera a fonte de verdade para compras, tentativas de pagamento e ingressos. Os valores monetarios serao representados por `Long` em centavos; `Double` nao sera usado para dinheiro.

Uma compra armazenara, no minimo:

- Identificador UUID imutavel.
- Identificador e snapshot dos dados relevantes do evento.
- Quantidade de ingressos.
- Preco unitario e valor total em centavos.
- Estado atual.
- Referencia enviada para a Cielo.
- Datas de criacao e ultima atualizacao.
- Dados nao sensiveis retornados pela transacao, quando disponiveis.

Uma tentativa de pagamento armazenara:

- Identificador proprio.
- Identificador da compra.
- Referencia idempotente.
- Estado e timestamps.
- Resultado normalizado ou falha sanitizada.

Um ingresso armazenara:

- Identificador UUID imutavel.
- Identificador da compra aprovada.
- Conteudo versionado usado no QR Code.
- Data de emissao.

O QR Code nao conterá credenciais nem dados de pagamento. Seu payload tera versao, identificador do ingresso, identificador da compra e uma representacao minima do evento. Como nao existe backend no escopo, o QR Code demonstra o vinculo e a emissao; autenticidade e validacao online sao evolucoes futuras.

## 5. Maquina de estados da compra

Estados do dominio:

```text
Draft -> PaymentInProgress -> Approved
                           -> Denied
                           -> Cancelled
                           -> Failed
                           -> Pending
```

Regras:

- Apenas uma compra `Draft`, `Denied`, `Cancelled` ou `Failed` pode iniciar uma tentativa mediante acao explicita do usuario.
- Antes de abrir a Cielo, a persistencia deve registrar atomicamente a tentativa e mover a compra para `PaymentInProgress`.
- `PaymentInProgress`, `Pending` e `Approved` nunca iniciam automaticamente uma nova cobranca.
- Uma nova tentativa apos negativa, cancelamento ou falha exige confirmacao explicita e recebe nova referencia de tentativa, permanecendo vinculada a mesma compra.
- Uma compra `Approved` e terminal para pagamento.
- Callback repetido da mesma tentativa e idempotente.
- Resultados conflitantes para uma tentativa terminal nao sobrescrevem o primeiro resultado confirmado e sao registrados apenas para diagnostico sanitizado.
- Ingresso e criado uma unica vez, na mesma operacao logica que confirma a aprovacao.

`Pending` representa resultado ambiguo: o pagamento foi iniciado, mas nao existe confirmacao confiavel. A interface nao oferece repetir silenciosamente. Para o case, o usuario podera voltar ao historico e consultar o estado; reconciliacao automatica com a Cielo fica documentada como evolucao.

## 6. Integracao com Cielo Smart

O aplicativo usara integracao local via Deep Link. Essa escolha segue a recomendacao da documentacao do Emulador Cielo, elimina a necessidade de backend e mantem o foco no desenvolvimento Android.

Fluxo:

1. Criar e persistir uma compra em `Draft`.
2. Criar atomicamente uma tentativa e marcar a compra como `PaymentInProgress`.
3. Montar o JSON da Cielo com credenciais configuraveis, referencia, itens e total em centavos.
4. Codificar o request em Base64 e montar a URI de pagamento.
5. Confirmar que existe um aplicativo capaz de resolver o Deep Link.
6. Iniciar um foreground service e abrir a Cielo por `Intent.ACTION_VIEW`.
7. Receber o callback em uma activity dedicada, registrada com scheme e host proprios.
8. Decodificar e validar defensivamente o retorno.
9. Normalizar o resultado para o dominio e persisti-lo de forma idempotente.
10. Encerrar o foreground service quando houver resultado terminal ou cancelamento controlado da espera.

Credenciais do emulador serao lidas de `local.properties` e expostas ao app via `BuildConfig`. O repositorio tera apenas placeholders e instrucoes. Nenhum token sera versionado ou escrito em logs.

A primeira atividade da implementacao sera um spike vertical no Android 10 ja preparado: enviar uma compra minima ao emulador e receber os retornos de sucesso, cancelamento e erro. O contrato observado sera registrado antes de expandir o restante do app.

### Interrupcao e recuperacao

O foreground service reduz o risco de o processo ser encerrado durante a troca de aplicativos. A persistencia acontece antes do Deep Link, portanto a tentativa permanece conhecida mesmo se houver encerramento.

Ao abrir o app, tentativas ainda `PaymentInProgress` sem callback confiavel serao apresentadas como `Pending`. O app nao assumira aprovacao, falha nem repetira a cobranca. Caso a documentacao e o spike confirmem uma consulta local segura por referencia, ela podera ser incorporada sem alterar o contrato do dominio; ela nao e requisito para a primeira entrega.

## 7. Experiencia e navegacao

O app tera duas areas principais:

- `Eventos`: catalogo e inicio de uma nova compra.
- `Ingressos`: compras aprovadas, com acesso ao comprovante e QR Code.

Fluxo principal:

1. Catalogo de eventos em cards com nome, data, local, preco e disponibilidade.
2. Detalhes do evento com seletor limitado de quantidade e total atualizado.
3. Revisao com itens, quantidade, total e botao `Pagar com Cielo`.
4. Estado de processamento com novos envios bloqueados.
5. Resultado distinto para aprovado, negado, cancelado, falha e pendencia.
6. Comprovante com referencia, evento, quantidade, total e dados seguros da transacao.
7. Ingresso com QR Code, identificador e dados do evento, apenas apos aprovacao.

O botao de pagamento sera desabilitado assim que a acao for aceita. Eventos indisponiveis ou quantidade invalida nao avancam para o checkout. Mensagens de erro terao causa compreensivel e proxima acao segura.

O historico permitira reabrir comprovantes e ingressos depois de reiniciar o aplicativo, demonstrando persistencia real.

## 8. Tratamento de erros e seguranca

Erros serao separados em categorias:

- Emulador ou handler de Deep Link indisponivel.
- Configuracao/credencial ausente.
- Request invalido.
- Pagamento negado.
- Cancelamento pelo usuario.
- Falha retornada pela Cielo.
- Callback ausente, malformado ou desconhecido.
- Falha local de persistencia.

Callbacks serao validados quanto ao scheme, host, Base64 e estrutura esperada. Payloads invalidos nao avancam o estado para aprovado.

Logs e dados persistidos nao incluirao tokens, credenciais ou informacoes sensiveis de pagamento. Mensagens tecnicas persistidas para diagnostico serao sanitizadas.

Se a gravacao local falhar antes de abrir a Cielo, o pagamento nao sera iniciado. Se falhar ao registrar um callback, o retorno sera mantido em memoria apenas durante aquela execucao e a interface informara resultado pendente; o sistema nao iniciara nova cobranca automaticamente.

## 9. Estrategia de testes

### Testes unitarios

- Calculo de quantidade e total.
- Limites de quantidade.
- Todas as transicoes validas e invalidas da maquina de estados.
- Criacao atomica de tentativa antes do gateway.
- Bloqueio de segundo pagamento em progresso ou pendente.
- Bloqueio definitivo para compra aprovada.
- Nova tentativa explicita apos negativa, cancelamento ou falha.
- Montagem do request e valores em centavos.
- Decodificacao de aprovacao, negativa, cancelamento e erro.
- Callback duplicado e callback conflitante.
- Callback malformado ou desconhecido.
- Geracao unica de ingresso apenas para compra aprovada.
- ViewModels e reducao de eventos para estados de UI.

### Testes instrumentados

- DAOs, transacoes e mapeamentos do Room.
- Persistencia e restauracao apos recriacao.
- Navegacao do caminho feliz com gateway fake.
- Estados principais de erro em Compose.
- Recepcao da URI de callback em teste controlado.

### Validacao manual

- Android 10 com o Emulador Cielo instalado.
- Sucesso, cancelamento e erro configurados no emulador.
- Toques repetidos no botao de pagamento.
- Volta ao app, rotacao/recriacao e reabertura apos encerramento.
- Historico, comprovante e QR Code depois da aprovacao.

O gateway real sera exercitado manualmente no emulador. Testes automatizados usarao um fake deterministico para nao depender de outro APK ou de interacao humana.

## 10. Stack proposta

- Kotlin.
- Jetpack Compose e Material 3.
- Navigation Compose.
- ViewModel, Coroutines, Flow e StateFlow.
- Room.
- Hilt.
- Kotlin Serialization.
- ZXing para geracao local do QR Code.
- JUnit, Turbine, MockK e bibliotecas oficiais de teste Compose/Room.
- GitHub Actions para build, lint e testes unitarios.

As versoes exatas serao definidas no plano de implementacao depois de consulta a documentacao atual e compatibilidade com o Android 10 e o Emulador Cielo.

## 11. Documentacao e uso de IA

O repositorio entregara:

- `README.md` com requisitos, configuracao, execucao, teste e demonstracao no emulador.
- Diagrama da arquitetura e do fluxo de pagamento.
- ADRs para Deep Link, monomodulo e estrategia de idempotencia/persistencia.
- Esta spec e o plano de implementacao.
- Registro de uso de IA com objetivos, prompts relevantes, restricoes, resultados, decisoes aceitas/rejeitadas e verificacoes humanas.
- Bibliotecas e justificativas.
- Trade-offs e evolucoes.

O registro de IA nao alegara que resultados foram validados quando nao foram. Comandos, testes e verificacoes manuais relevantes acompanharao as decisoes produzidas com assistencia de IA.

## 12. Trade-offs e evolucoes

Escolhas conscientes para a entrega:

- Catalogo local em vez de backend: concentra o investimento na experiencia e engenharia Android.
- Um modulo em vez de multimodulo: proporcional ao escopo e ao prazo de tres dias.
- Deep Link em vez de integracao remota: recomendado para o emulador e independente de backend.
- QR Code local sem validacao remota: demonstra emissao e vinculo, mas nao combate fraude sozinho.
- Estado pendente conservador: privilegia evitar cobranca duplicada quando o resultado e ambiguo.

Com mais tempo e crescimento real do produto:

- Extrair modulos de feature quando houver equipes, reutilizacao ou ciclos de release independentes.
- Adicionar backend para catalogo, estoque, reserva, reconciliacao e validacao de ingressos.
- Assinar o payload do QR Code e criar um app/processo de leitura.
- Consultar/reconciliar tentativas pendentes com a Cielo.
- Adicionar observabilidade, analytics e testes de processo interrompido em dispositivos fisicos.
- Ampliar acessibilidade, internacionalizacao e testes de screenshots.

## 13. Criterios de aceite

- O projeto compila seguindo apenas as instrucoes do README.
- O app roda no ambiente Android 10 informado.
- Eventos locais podem ser visualizados e comprados em quantidade valida.
- O valor enviado a Cielo corresponde exatamente ao resumo da compra.
- O emulador consegue retornar sucesso, cancelamento e erro ao Ticket Flow.
- Resultados sao persistidos e reaparecem apos reabrir o app.
- Toques ou callbacks repetidos nao criam segunda cobranca, segunda aprovacao nem segundo ingresso.
- Estados negado, cancelado, falha e pendente sao explicitos e recuperaveis sem reenvio automatico.
- Uma compra aprovada exibe comprovante e ingresso com QR Code vinculado.
- Os cenarios criticos possuem testes automatizados.
- CI executa build, lint e testes unitarios.
- README, ADRs, spec, plano e registro de IA atendem aos materiais solicitados no case.

## 14. Sequencia de entrega

- Dia 1: spike da Cielo, fundacao do projeto, catalogo e checkout.
- Dia 2: persistencia, fluxo completo de pagamento, recuperacao e QR Code.
- Dia 3: testes, acabamento visual, CI, documentacao e ensaio da apresentacao.

O objetivo operacional e obter uma primeira versao vertical funcional no primeiro dia. Os dias seguintes aumentam confiabilidade e qualidade da apresentacao, em vez de adiar a integracao de maior risco para o fim.
