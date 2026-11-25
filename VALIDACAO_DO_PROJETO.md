# Validação do projeto

Referência: 24/11/2026. O Guardiã° foi avaliado como MVP acadêmico em ambiente local, com contas fictícias, API Spring Boot, banco isolado `guardia_teste` e emulador Android. Os resultados representam as versões e os cenários avaliados, sem equivaler à homologação para uso em emergências reais.

## Resultados registrados

| Área | Evidência |
|---|---|
| Backend | 102 testes isolados aprovados na revisão da política de senha, sem MySQL ou FCM real. |
| Android | 42 testes JVM aprovados na revisão do chat; compilação e lint posteriores aprovados, com zero erros e 38 avisos. |
| Interface e localização | Testes instrumentados específicos de mapas, alertas e ciclo de localização aprovados em rodadas anteriores. |
| Banco e fila de notificações | 16 testes MySQL aprovados em revisão anterior; recuperação da fila após reinício observada em teste integrado. |
| Fluxo integrado | Cadastro/login, SOS, recebimento, confirmação de ajuda, chat e encerramento observados com contas fictícias. Histórico, perfil e mapa também foram conferidos manualmente. |
| Segurança de sessão e acesso | Revogação, logout offline/troca de conta, preservação do vínculo FCM e bloqueio de acesso ao chat por terceira conta ou após encerramento tiveram cenários aprovados. |
| Push restrito | Recebimento da notificação e abertura do alerta confirmados no emulador, separadamente da aceitação do envio pelo FCM. |

As rodadas não são somadas como testes únicos. Resultados anteriores não substituem a revalidação após alterações.

## Condições de demonstração

- SOS e chat dependem de conexão. A localização é atualizada com o aplicativo em primeiro plano; posições vencidas deixam de habilitar novas destinatárias e novos aceites.
- Notificações dependem do Android, das permissões e do FCM. Após “Forçar parada”, é necessário reabrir o aplicativo; não há garantia de entrega.
- Mapas e endereços dependem de serviços externos. O endereço é aproximado, e esses serviços podem receber a região visualizada ou as coordenadas consultadas.
- Os testes foram concentrados em emulador. O aplicativo é um protótipo acadêmico e não substitui serviços oficiais de emergência.

## Continuidade da validação

A T02.9 permanece parcialmente concluída. Restam revalidações da revisão atual no MySQL, cenários de concorrência e respostas atrasadas na troca de conta, testes instrumentados novos de endereço e ampliação de testes de rede, acessibilidade e dispositivos. A estrutura SQL de configuração ainda requer validação de execução em ambiente isolado vazio.

## Evoluções previstas

Localização em segundo plano com consentimento, sincronização offline ampliada, comunidade de amigas, grupos/anexos e rotas são evoluções de produto. HTTPS fora do desenvolvimento, backups e política de retenção de dados integram a preparação necessária antes de uma operação real.
