# Configuração local do Android

O projeto usa Kotlin/Jetpack Compose e Firebase Cloud Messaging. Gradle, AGP, Kotlin e SDKs existentes foram preservados.

## Firebase

O arquivo app/google-services.json é local e não deve ser publicado neste repositório. O arquivo app/google-services.json.example apenas descreve a estrutura com valores fictícios: não é uma configuração Firebase funcional.

Para compilar uma cópia nova, criar/configurar seu próprio aplicativo Android no Firebase com package br.com.guardia e baixar o google-services.json real para app/. Não substituir uma configuração local existente pelo exemplo. Não há fallback silencioso que desative autenticação ou FCM; sem configuração válida a etapa Google Services pode impedir a compilação.

A configuração do cliente identifica o projeto Firebase e é diferente da chave privada do Firebase Admin. Contas de serviço nunca pertencem ao aplicativo Android. Restrições e permissões devem ser configuradas no projeto Firebase; exclusão do arquivo no Git não substitui esses controles.

## SDK e API local

local.properties contém caminhos locais do Android SDK e deve permanecer fora do Git. O Android Studio pode criá-lo na máquina de quem abrir o projeto.

A API local é acessada pelo emulador via 10.0.2.2:8080. Para testar, utilizar o backend com perfil teste, schema guardia_teste e notificações simuladas. Esse endereço é específico do emulador; HTTP local não é uma configuração de produção.

## Verificação

~~~powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --console=plain
~~~

connectedDebugAndroidTest requer emulador disponível. Não desinstalar/limpar os dados para atualizar uma versão de teste quando o objetivo é verificar persistência de sessão. Chaves de assinatura, arquivos privados, resultados de build e credenciais não devem ser enviados ao GitHub.

## Atualização de localização em primeiro plano

A versão atual tenta atualizar a posição ao entrar/retornar ao aplicativo autenticado e a cada 60 segundos enquanto está em primeiro plano (estado RESUMED). Sem permissão, a consulta Minha localização oferece um pedido específico que não cria SOS. Cada captura tem timeout de 20 segundos, rejeita medidas anteriores a 30 segundos e é cancelada ao sair/invalidar a sessão. Não há rastreamento com app fechado nem permissão de localização em segundo plano.

O backend exige a nova coluna localizacao_atualizada_em, cujo SQL aditivo de teste está em sql/2026-11-22-localizacao-teste.sql na API. Não iniciar a nova API sob validate antes de preparar a estrutura com autorização. A posição da conta não movimenta um SOS já registrado. A validade padrão no servidor é 15 minutos; depois disso a conta não habilita novas destinatárias/aceites por proximidade até atualizar a posição. Participação já confirmada e chat seguem suas autorizações próprias.

Atualização, pausa/retomada e validade tiveram ensaios manuais com contas fictícias; dois testes instrumentados do ciclo foram aprovados. Troca de conta com respostas atrasadas e concorrência real ainda têm validações residuais. Use somente contas/posições fictícias e perfil teste com notificações simuladas.

## Mapa integrado

A dependência MapLibre OpenGL 13.5.4 foi adicionada sem alterar as versões existentes. O mapa usa OpenFreeMap e não requer chave/cartão na configuração atual. Consulte MAPA_INTEGRADO.md para créditos, privacidade, cache, limites e testes pendentes. Abertura geográfica e controles receberam confirmação manual em um emulador; falhas de rede e outros aparelhos permanecem pendentes. O botão externo foi preservado.

