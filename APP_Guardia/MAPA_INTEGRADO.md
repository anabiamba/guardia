# Mapa integrado do alerta

## Escopo

No alerta recebido, VER MAPA NO APP abre uma tela interna com o ponto registrado no SOS, zoom por gestos/botões, centralização e retorno. ABRIR NO MAPA e a alternativa externa na tela interna são preservados. Sem rotas, rastreamento em segundo plano, posição de outras contas ou movimento retroativo do SOS.

## Dependência e provedor

- MapLibre Native Android OpenGL: org.maplibre.gl:android-sdk-opengl:13.5.4, versão fixa no catálogo.
- POM publicado consultado: Kotlin stdlib 2.2.10 e OkHttp 4.12.0. Resolução e build aprovados com o ambiente existente. Gradle, AGP 9.3.1, plugin Compose/Kotlin 2.2.10, minSdk 24 e compile/target SDK 37 preservados.
- Estilo: https://tiles.openfreemap.org/styles/liberty. OpenFreeMap informa serviço público gratuito sem conta/chave e documenta uso com MapLibre Native; disponibilidade/termos dependem do provedor. Não há garantia SLA.
- Créditos visíveis OpenFreeMap, OpenMapTiles e OpenStreetMap contributors, além do controle de atribuição do SDK. Não ocultar esses créditos.

Fontes: https://openfreemap.org/quick_start/ ; https://openfreemap.org/ ; https://maplibre.org/maplibre-native/android/examples/artifacts/ ; https://repo.maven.apache.org/maven2/org/maplibre/gl/android-sdk-opengl/13.5.4/android-sdk-opengl-13.5.4.pom .

## Privacidade e limites

O mapa é inicializado após toque explícito e recebe apenas o ponto já exibido pelo fluxo autorizado do alerta. Não recebe senha, Bearer, credencial de revogação, nome ou e-mail. Não usa o cliente HTTP autenticado da API. A região visualizada e o IP podem ser conhecidos pelos servidores de mapa através das requisições de tiles/estilo; SDK pode manter tiles em cache. O marcador é construído em memória, sem novo histórico de trajetos implementado pelo aplicativo. Não se trata de presença física verificada.

A localização automática existente continua obedecendo seu próprio ciclo de primeiro plano; o mapa não ativa LocationComponent nem novas permissões de localização. Coordenadas inválidas não viram ponto 0,0. A projeção Mercator do mapa interno limita latitude a aproximadamente +/-85,0511 graus; pontos fora da projeção recebem mensagem e alternativa externa, sem modificar os dados do backend.

MapView é pausado/parado junto do lifecycle e destruído ao sair/invalidar a composição do alerta, com remoção de listeners e tratamento de baixa memória. Callbacks antigos são ignorados após descarte. Nova tentativa recria o recurso. Erro/inicialização nativa indisponível e carregamento sem concluir em 20 segundos de primeiro plano exibem mensagem/repetição; falhas não bloqueiam voltar ou usar o mapa externo. A rotação/reabertura pode recentralizar no SOS, sem persistir uma posição de câmera.

## Testes e validação

Build, 21 testes JVM e lint aprovados no ambiente Codex; três testes novos verificam ponto/projeção e sequência/idempotência de lifecycle. Dois testes instrumentados preparados e compilados: renderizar marcador/controles/retorno com estilo local fictício e alternativa com ponto inválido. O estilo de teste fica em src/debug/assets/mapa_teste.json, não contém tiles/fontes/sprites externos e nunca é fallback de produção. Esses testes não foram executados por ausência de emulador conectado na implementação.

Para executar somente os novos testes, com emulador ligado e sem iniciar API para este ensaio isolado:

~~~powershell
.\gradlew.bat :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=br.com.guardia.MapaAlertaInstrumentadoTest' --console=plain
~~~

Depois validar manualmente no perfil teste/guardia_teste com contas e posições fictícias: abrir um alerta autorizado, VER MAPA NO APP, conferir região e ponto, zoom/arraste/centralização, retorno, segundo plano/retomada, encerramento e sessão invalidada; testar falta de internet e alternativa externa. Falta de internet pode ainda permitir tiles em cache e não prova falha de tratamento. Não enviar FCM real para testar o mapa.

Estilo remoto retornou HTTP 200 com JSON versão 8 na máquina de desenvolvimento. Isso não comprova renderização, conectividade ou carregamento de tiles no emulador. SDK nativo aumenta o APK; o APK debug desta revisão mede aproximadamente 53,8 MiB. Empacotamento exibiu aviso de bibliotecas nativas não despojadas de símbolos, sem impedir build; testes físicos e release permanecem pendentes.
## Minha localização — 23/11/2026

O cartão Minha localização da Home abre uma consulta local da posição do aparelho. Usa LocationHelper com amostra recente (até 30 segundos) e timeout de 20 segundos; não usa coordenadas de um alerta ou posição fictícia como fallback. Permissão solicitada quando ausente. GPS desativado/falha de consulta apresentam tentativa e acesso às configurações. Voltar cancela a consulta; pausa cancela a aquisição pelo lifecycle e retornar inicia nova busca. A tela tem instância própria do helper para não cancelar operações existentes da Home.

O ponto é uma fotografia da consulta, não rastreamento ao vivo. Atualizar localização busca outra amostra; o controle MINHA POSIÇÃO apenas recentraliza o ponto exibido. Mapa do SOS continua com os textos e a posição registrada no alerta. A nova consulta não cria SOS nem envia posição pela API; sincronização em primeiro plano já existente da Home permanece independente. Provedor do mapa recebe requisições da região/IP e SDK pode manter cache de tiles, como no mapa do alerta.

Validação manual: entrar com conta fictícia; Minha localização; conceder permissão; conferir posição simulada do emulador e mapa; zoom/centralizar/voltar; mudar posição simulada e atualizar; negar permissão/desligar GPS e testar recuperação. Confirmar separação do mapa do SOS e ausência de criação de alerta. Aquisição real, ciclo de permissão e integração visual não são comprovados pelos testes de estado de tela.

## Alerta recebido: primeiro nome e mini mapa — 23/11/2026

O cartão do alerta recebido exibe o primeiro nome da solicitante e um mini mapa da posição gravada no SOS. Não usa a posição atual da ajudante nem acrescenta rastreamento. Ver detalhes mantém o mapa maior e sua alternativa externa. Créditos e aviso sobre requisições ao provedor aparecem também no cartão. A área de 1 km do componente reutilizado é ilustrativa; autorização e elegibilidade continuam no servidor.

GET /api/v2/alertas/proximos acrescenta primeiroNome, mantendo os campos anteriores do alerta em AlertaProximoResponse. O serviço aplica o filtro de acesso existente antes de buscar nomes, e reaproveita o nome da mesma autora dentro da consulta. Nome completo, e-mail, senha e objeto da conta não entram na resposta. Sem primeiro nome, aplicativo mantém Solicitante do alerta. Sem coordenadas válidas, não abre mini mapa e mantém mensagem de indisponibilidade. Sem alterações de banco, dependências ou geocodificação/endereço de rua.

Para validar com dados fictícios, reiniciar API no perfil teste com ddl-auto=validate, executar o Android atualizado, abrir um alerta permitido para outra conta e conferir primeiro nome, marcador, Ver detalhes/retorno, confirmação e chat. Sem reiniciar a API antiga, o mini mapa usa as coordenadas existentes, mas o nome permanece genérico. Testes de cartão usam um mapa fictício para evitar requisições externas; carregamento de tiles reais nesta composição exige conferência manual.

## Endereço aproximado do SOS — 24/11/2026

Alerta recebido consulta o Geocoder do Android com a posição registrada no SOS. Resultado aparece como Endereço aproximado; rua é necessária, número/bairro/cidade/estado somente quando fornecidos. Não se usa a posição da ajudante nem se substitui o marcador pelo endereço inferido. Endereço não é utilizado na autorização, aceite, raio ou prazo. Consulta pode enviar coordenadas ao provedor do aparelho, conforme aviso visível no cartão; aplicativo não adiciona persistência ou logs de endereço/coordenadas.

API 33+ usa listener assíncrono; API 24–32 usa chamada legada fora da interface. Registro/consulta fora da thread principal, executor de uma thread sem fila ilimitada. Prazo de espera local de 8s e cancelamento local ignoram respostas posteriores. Android não oferece garantia de interromper fisicamente a consulta ao provedor; uma chamada legada bloqueada pode ocupar a única thread e consultas seguintes falharem imediatamente, mantendo mapa e ajuda disponíveis. Retentativa manual; não repetir a cada polling. Pausa cancela espera em andamento e retomada pode tentar de novo; resultado completo fica em memória da composição daquele alerta.

Geocoder pode estar ausente, falhar ou devolver estimativas incorretas. Recurso é complementar: sem rua/resultado aparece Endereço indisponível. Consulte o mapa. Sem dependência, chave, API ou banco novos. Fonte técnica: https://developer.android.com/reference/android/location/Geocoder .

Validação manual pendente: abrir alerta fictício no emulador, conferir endereço e número somente se retornados, ausência de serviço/rede, tentativa, retorno/troca de alerta e ajuda sem bloqueio. Para testes sem consultas externas: .\gradlew.bat :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=br.com.guardia.EnderecoAlertaInstrumentadoTest,br.com.guardia.AlertaRecebidoInstrumentadoTest' --offline --console=plain . Casos usam consultor e mapa fictícios; não equivalem a validação do Geocoder real ou da versão antiga Android.
