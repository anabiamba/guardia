# Guardiã°

Aplicativo Android de apoio emergencial entre mulheres, desenvolvido como Trabalho de Conclusão de Curso. Uma usuária pode solicitar ajuda por SOS; outras usuárias elegíveis próximas podem visualizar o alerta, confirmar ajuda e conversar com a solicitante.

O projeto é um MVP acadêmico. Não substitui serviços oficiais de emergência e não deve ser apresentado como serviço de emergência com disponibilidade ou entrega garantidas.

## Funcionalidades

- Cadastro, login por sessão, perfil e troca de senha com BCrypt.
- Sessão cifrada no Android; logout local e fila persistente de revogação para recuperar conectividade.
- SOS persistido e elegibilidade calculada no servidor em raio de 1 km.
- Validade de uma hora, encerramento pela autora e recuperação de alertas expirados no servidor.
- Atualização de localização em primeiro plano; posições antigas não habilitam novas destinatárias ou novos aceites.
- Recebimento por polling e Firebase Cloud Messaging, com vínculos por instalação/sessão e fila persistente de despacho.
- Confirmação de ajuda e chat restrito à autora e a uma ajudante confirmada por alerta.
- Histórico dos próprios alertas, mapa integrado e alternativa de mapa externo.
- Primeiro nome da solicitante e endereço aproximado, quando disponível no serviço do aparelho.

## Estrutura

```text
APP_Guardia/                  Aplicativo Kotlin / Jetpack Compose
  CONFIGURACAO_LOCAL.md        Configuração Android e Firebase
  MAPA_INTEGRADO.md            Mapa, endereço e privacidade
  ALERTA_ATIVO.md              Tela e contrato do SOS ativo
guardia-api/guardia-api/       API Java / Spring Boot
  CONFIGURACAO_LOCAL.md        Perfis e variáveis locais
  sql/                        Proposta de instalação limpa e scripts aditivos
POLITICA_SENHA_GUARDIA.md      Regra de cadastro e troca de senha
VALIDACAO_DO_PROJETO.md       Resultados e condições de demonstração
```

## Tecnologias e requisitos

Android nativo com Kotlin, Compose, Gradle Wrapper, SDK 37 e minSdk 24; AGP 9.3.1 e Kotlin 2.2.10 configurados no projeto. MapLibre Native OpenGL 13.5.4/OpenFreeMap para mapas; Geocoder Android para endereço complementar. Backend Spring Boot 4.1.1, alvo Java 17, Maven Wrapper, Spring Data JPA, MySQL e Firebase Admin. O ambiente de desenvolvimento foi verificado com JBR do Android Studio; preserve as configurações Kotlin/AGP funcionais do projeto.

Instale Android Studio com SDK/plataforma 37 e ferramentas necessárias, JDK compatível, MySQL local e acesso à internet para baixar dependências. O primeiro build precisa de rede. O endereço `10.0.2.2:8080` é usado pelo aplicativo para acessar o computador pelo emulador e não é configuração para aparelho físico ou produção.

## Configuração segura

Arquivos reais de Firebase, credenciais, `local.properties` e `application-teste.properties` não acompanham o repositório. Os arquivos `.example` contêm somente modelos; não são credenciais funcionais. Não sobrescreva configurações locais já existentes ao seguir estas instruções.

### Banco de teste — pré-requisito

Use somente o schema `guardia_teste` e o usuário `guardia_teste_app`, com privilégios limitados a esse schema. Nunca use o banco original ou uma conta administrativa para executar a demonstração. A senha será informada localmente, sem incluí-la em comandos, arquivos versionados ou logs.

Para preparar o banco de demonstração, consulte o [roteiro de configuração](guardia-api/guardia-api/sql/INSTALACAO_LIMPA.md). O SQL proposto exige `guardia_teste` vazio e usuário restrito; não deve ser aplicado ao banco local já preenchido nem combinado com os scripts aditivos. Sua execução e validação no MySQL permanecem pendentes.

A API inicia com `ddl-auto=validate`: valida a estrutura, sem usar `update` ou `create` para corrigir divergências. Se faltarem tabelas/colunas, interrompa e prepare o esquema com revisão e autorização próprias. Não há migração destrutiva ou criação automática de schema nesta configuração.

### API local com notificações simuladas

Abra um PowerShell na raiz do projeto. Configure `JAVA_HOME` para o JDK instalado na sua máquina. Em seguida:

```powershell
Set-Location '.\guardia-api\guardia-api'

$modelo = 'src\main\resources\application-teste.properties.example'
$local = 'src\main\resources\application-teste.properties'
if (-not (Test-Path -LiteralPath $local)) {
    Copy-Item -LiteralPath $modelo -Destination $local
}

$env:GUARDIA_TEST_DB_PASSWORD = Read-Host 'Senha de guardia_teste_app' -MaskInput
if ([string]::IsNullOrWhiteSpace($env:GUARDIA_TEST_DB_PASSWORD)) {
    throw 'Senha ausente. Inicialização cancelada.'
}

.\mvnw.cmd '-Dspring-boot.run.profiles=teste' '-Dspring-boot.run.arguments=--server.port=8080 --spring.jpa.hibernate.ddl-auto=validate --guardia.sessao.duracao=PT24H' spring-boot:run
```

O perfil `teste` possui proteções de schema/usuário, desativa Firebase Admin real e usa notificações simuladas. Não habilite `teste-push` para essa demonstração comum: ele faz parte de ensaio separado de envio real restrito.

Em outro PowerShell, uma verificação básica de resposta é:

```powershell
(Invoke-WebRequest 'http://localhost:8080/api/usuarios/teste' -TimeoutSec 10).StatusCode
```

O esperado é HTTP 200. Isso confirma a resposta do endpoint, não é um diagnóstico completo do banco ou da saúde de todas as funcionalidades. A raiz `/` pode retornar 404.

### Android

1. Abra `APP_Guardia` no Android Studio e sincronize o Gradle.
2. Configure seu próprio aplicativo Firebase Android com package `br.com.guardia` e obtenha `google-services.json`, colocando-o em `APP_Guardia/app/`. O modelo `google-services.json.example` não permite um build Firebase funcional por si só.
3. Não coloque credenciais Firebase Admin no Android. Elas não são necessárias para executar a API no perfil simulado.
4. Inicie o emulador com serviços compatíveis, execute pelo Run e conceda as permissões pertinentes.
5. Use exclusivamente contas, senhas e posições fictícias. O GPS do emulador usa posição simulada, que pode ser configurada em seus controles.

Com notificações simuladas, o polling permite a demonstração integrada; a API não envia push real. A validação de recebimento FCM real exige ambiente e autorização próprios. Mapas/endereço dependem de serviços externos e disponibilidade de rede; os respectivos provedores podem receber região visualizada e, para geocodificação, coordenadas do SOS. Endereço é aproximado e não substitui a posição registrada.

## Testes automatizados

No diretório da API, estes testes selecionados não exigem conexão MySQL nem inicialização completa da aplicação:

```powershell
.\mvnw.cmd '-Dtest=UsuarioAutenticacaoTest,SessaoAutorizacaoTest,IsolamentoPerfilTesteTest,AlertaPrazoTest,AlterarSenhaTest,ChatAlertaTest,DespachoSosTest,FilaPushTest,HistoricoAlertaTest,LocalizacaoValidadeTest,NotificacaoTestePushTest,AlertaStatusVisualTest,AlertaProximoNomeTest,PoliticaSenhaTest' test
```

Não substituir esse comando por execução indiscriminada de todos os contextos em ambiente sem isolamento configurado. `SosMysqlIT` usa banco real e requer preparação/autorização próprias, somente em `guardia_teste`; não faz parte do comando acima.

No diretório Android, com JDK/SDK/Firebase local configurados:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --console=plain
```

Com emulador disponível, os testes de cartão/endereço podem ser executados sem consulta real ao Geocoder ou API:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=br.com.guardia.EnderecoAlertaInstrumentadoTest,br.com.guardia.AlertaRecebidoInstrumentadoTest' --console=plain
```

A configuração existente mantém APKs após os testes, importante para não confundir desinstalação com falha de persistência de sessão. Não use limpeza de dados durante um teste de restauração de sessão. Com dependências já baixadas, os comandos aceitam modo offline. Consulte [validação do projeto](VALIDACAO_DO_PROJETO.md) para os resultados realmente executados.

## Demonstração manual

Com API `teste`/`validate` e posições fictícias próximas e recentes: criar contas de teste; Ana aciona SOS; outra conta recebe por polling, visualiza solicitante/mapa/endereço aproximado, confirma ajuda; ambas trocam mensagens; autora encerra; alerta e chat deixam de estar disponíveis. A regra de uma hora e demais cenários negativos precisam ser avaliados separadamente, conforme suas evidências.

## Limitações e escopo futuro

Localização não é contínua com o aplicativo fechado. Há restrições de entrega FCM, especialmente após Forçar parada até reabrir o app. SOS requer conexão: não se promete entrega offline. Endereço pode não existir ou estar incorreto; mapa/serviços externos não têm disponibilidade garantida pelo projeto. Não há chat em grupo, anexos, fotos de perfil ou recuperação de senha por e-mail no MVP.

Permanecem validações residuais de concorrência/rollback MySQL, dispositivos/versões e falhas de rede. HTTPS fora do desenvolvimento, operação, backups, retenção de dados e revisão de privacidade exigem preparação específica antes de uso real. Esta publicação não equivale a implantação em produção nem à conclusão integral da T02.9.

Comunidade de amigas, disponibilidade em segundo plano, rotas, perfil avançado e offline ampliado são evoluções separadas. Não incluem controles mínimos já necessários de autenticação e autorização.

## Publicação e credenciais

Não versionar tokens, senhas, contas de serviço, chaves de assinatura, arquivos locais ou dumps do banco. `.gitignore` ajuda a prevenir inclusão acidental, mas não remove segredos de histórico já existente. Em caso de exposição, revogar/substituir credenciais. O repositório não inclui contas de demonstração prontas com senha pública.

## Autoria

**Ana Beatriz** — FASF — Análise e Desenvolvimento de Sistemas.

Repositório: https://github.com/anabiamba/guardia

## Direitos autorais e condições de consulta

**© 2026 Ana Beatriz — Todos os direitos reservados.** Material original disponibilizado para consulta e avaliação acadêmica, sem licença geral de reutilização, modificação ou distribuição. Consulte o [aviso de direitos autorais](LICENSE.md).

Permanecem válidas as permissões previstas na legislação aplicável e nos termos do GitHub, inclusive visualização e forks na plataforma. Este projeto não é apresentado como código aberto de livre reutilização. Componentes de terceiros mantêm suas licenças e atribuições próprias.

