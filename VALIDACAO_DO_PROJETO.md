# Validação do projeto

Referência: 25/11/2026. O Guardiã° foi avaliado como MVP acadêmico em ambiente local, com contas fictícias, API Spring Boot, banco isolado `guardia_teste` e emulador Android. Os resultados representam as versões e os cenários avaliados, sem equivaler à homologação para uso em emergências reais.

## Resultados registrados

| Área | Evidência |
|---|---|
| Backend | 102 testes isolados aprovados em rodada anterior da política de senha; oito testes de troca de senha reaprovados em 24/11, sem MySQL ou FCM real. Os oito não são somados aos 102 como novos casos. |
| Android | Regressão de 25/11: 42 testes JVM aprovados, APK debug e lint aprovados; zero erros lint, 38 avisos e uma sugestão. |
| Interface e localização | Regressão de 25/11: 25 testes instrumentados selecionados aprovados no Pixel 7 emulado/Android 17, sem falhas, erros ou ignorados. Incluem endereço, mapas locais, sessão e localização atrasada; captura e envio simulados. |
| Banco e fila de notificações | Suíte ampliada com 23 testes MySQL aprovada em 24/11, sem falhas, erros ou ignorados, em guardia_teste. Inclui revogação multissessão/limpeza FCM, rollback controlado e concorrência de troca de senha com login/localização. |
| Fluxo integrado | Cadastro/login, SOS, recebimento, confirmação de ajuda, chat e encerramento observados com contas fictícias. Histórico, perfil e mapa também foram conferidos manualmente. |
| Segurança de sessão e acesso | Revogação, logout offline/troca de conta, preservação do vínculo FCM e bloqueio de acesso ao chat por terceira conta ou após encerramento tiveram cenários aprovados. |
| Push restrito | Recebimento da notificação e abertura do alerta confirmados em rodada anterior no emulador, separadamente da aceitação do envio pelo FCM; sem novo envio real na regressão de 25/11. |
| Verificações manuais recentes | Negativos de cadastro, senha atual incorreta com sessão preservada, negativa/restauração da permissão, localização desligada/reativada e texto do chat preservado sem rede com retomada após reconectar, conforme capturas e relatos da responsável. |

As rodadas não são somadas como testes únicos. Resultados anteriores não substituem a revalidação após alterações.

## Condições de demonstração

- SOS e chat dependem de conexão. A localização é atualizada com o aplicativo em primeiro plano; posições vencidas deixam de habilitar novas destinatárias e novos aceites.
- Notificações dependem do Android, das permissões e do FCM. Após “Forçar parada”, é necessário reabrir o aplicativo; não há garantia de entrega.
- Mapas e endereços dependem de serviços externos. O endereço é aproximado, e esses serviços podem receber a região visualizada ou as coordenadas consultadas.
- Os testes foram concentrados em emulador. O aplicativo é um protótipo acadêmico e não substitui serviços oficiais de emergência.

## Continuidade da validação

**A rodada selecionada foi aprovada nos cenários executados. A T02.9 global permanece parcialmente concluída pelos limites de cobertura abaixo.** Os testes MySQL ampliados, instrumentados de endereço e integração de componentes para descarte da localização entre contas deixaram de ser pendentes.

Não foram comprovados: instalação completa em ambiente separado; aparelhos físicos e múltiplas versões; acessibilidade ampliada; entrega exatamente uma vez após confirmação HTTP perdida; e o percurso Home + GPS + HTTP reais com atraso forçado na troca de conta. O teste de componentes não substitui esse percurso completo. Testes de notificações locais e o exemplo de contexto não foram selecionados na regressão final; exclusão não é aprovação nem teste ignorado pelo runner.

Permissões e rede possuem os cenários específicos relatados, sem cobertura de todas as combinações. O diálogo inicial de recusa de notificações não deve ser confundido com o teste anterior de notificações desativadas nas configurações. A estrutura SQL preparada ainda requer execução/validação em ambiente isolado vazio. A última captura de recuperação da posição mostrava o mapa carregando; carregamento de tiles daquela tentativa não foi confirmado, embora existam validações anteriores independentes do mapa.

Não há teste reprovado nas execuções finais selecionadas. Isso não equivale a ausência de defeitos fora do escopo ou homologação para operação real.

## Evoluções previstas

Localização em segundo plano com consentimento, sincronização offline ampliada, comunidade de amigas, grupos/anexos e rotas são evoluções de produto. HTTPS fora do desenvolvimento, backups e política de retenção de dados integram a preparação necessária antes de uma operação real.
