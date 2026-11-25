# Testes e limitações da revisão pública

Referência: 24/11/2026. Implementação, compilação, testes isolados e validação integrada são evidências diferentes. Os resultados abaixo não devem ser somados entre rodadas como testes únicos.

| Evidência | Resultado conhecido | Limite |
|---|---|---|
| Backend isolado, revisão da política de senha | 102 testes, zero falhas/erros/pulados | Repositórios simulados/contextos selecionados; sem MySQL ou FCM real |
| Android, revisão do chat | 42 JVM aprovados; build aprovado | A última alteração literal do rótulo Participante teve build/lint repetidos, não nova rodada JVM |
| Lint final da revisão do chat | Zero erros e 38 avisos | Avisos existentes não foram suprimidos globalmente |
| Endereço aproximado | Nove unitários novos incluídos na suíte; interface compilada | Três instrumentados novos ainda não executados; Geocoder legado e falhas reais pendentes |
| Mapas/alerta ativo | Cinco instrumentados específicos aprovados em revisão anterior | Estilo local/fictício; não equivale a rede/tiles reais em todos os aparelhos |
| Cartão recebido com primeiro nome | Quatro instrumentados aprovados após ajustar sequência do teste | Primeira rodada interrompida não é aprovada; integrações reais possuem evidência própria |
| Ciclo de localização | Dois instrumentados aprovados no Pixel 7/Android 17 | Operação fictícia/lifecycle; não prova todos os comportamentos de GPS |
| MySQL | Suíte anterior de 16 casos aprovada, incluindo concorrência/fila | Anterior ao mapeamento/revisões atuais; precisa reexecução controlada |
| Fluxo manual atual, informado pela responsável | Perfil, nome/endereço/mapa, ajuda, chat nos dois sentidos, teclado e encerramento aprovados | Contas fictícias e um emulador; sem prova de todos os cenários de segurança |
| FCM real restrito, revisão anterior | Recebimento único, abertura do alerta e encerramento confirmados | Aceitação FCM e recebimento foram observados separadamente; não é garantia de entrega futura |

## Pendências que não podem ser consideradas aprovadas

- Instrumentados novos de endereço e falhas reais de serviço/rede/cancelamento em diferentes versões.
- Suíte MySQL da revisão atual, somente com perfil teste, guardia_teste, usuário restrito e notificações simuladas.
- Concorrência/rollback da troca de senha com login e atualização da posição; revogação de múltiplas sessões/limpeza FCM com transações reais.
- Troca de conta com localização automática e respostas atrasadas na revisão atual.
- Registro individual de todos os testes manuais negativos da política de senha; relato geral de sucesso não comprova cada caso.
- Fontes ampliadas, acessibilidade, dispositivo físico e múltiplas versões. Dispositivo físico indisponível nesta rodada.
- Validação real da proposta completa de instalação limpa (sql/INSTALACAO_LIMPA.md e instalacao-limpa-guardia-teste.sql), preparada a partir do JPA, ainda sem execução MySQL. Não aplicar scripts aditivos por cima dela.

T02.9 permanece parcialmente concluída. Não reutilizar o banco original para suprir ausência do ambiente isolado. Testes MySQL com escrita e ensaios de push real exigem autorização específica.

## Comportamentos e evoluções conhecidos

Endereço é referência aproximada, sem garantia de número ou precisão. Consulta pode enviar coordenadas ao serviço do aparelho; mapa pode consultar provedores da região. Geocoder tem cancelamento local: operação física do provedor pode continuar, e uma chamada legada bloqueada pode indisponibilizar temporariamente consultas seguintes. Não há gravação própria de histórico de endereços.

Localização atualiza em primeiro plano, não durante todo o tempo. Após a validade configurada (padrão 15 minutos), posição antiga não habilita novas destinatárias/aceites; participação já confirmada mantém autorização própria. Forçar parada restringe push até reabrir o aplicativo. A causa histórica de uma ocorrência inicial de sessão ausente não foi demonstrada; repetições posteriores passaram, e uma rodada de desinstalação por testes foi identificada e protegida por configuração.

Comunidade, localização em segundo plano com consentimento, grupos/anexos, perfil avançado, rotas e sincronização offline ampliada ficam fora do escopo atual. Preparação operacional/HTTPS, backups e política de dados não são substituídos pelos testes acadêmicos.

