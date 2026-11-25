# Tela de alerta ativo

A tela da autora reúne mapa da posição registrada no SOS, raio ilustrativo de 1 km, início, tempo restante estimado, estado da ajuda, chat após confirmação e encerramento. Não exibe número do SOS nem pessoas fictícias. O MVP mantém uma ajudante confirmada. Voltar retorna à Home; Ver meu alerta reabre o detalhe. Polling autenticado existente permanece ativo.

## Contrato aditivo

GET /api/v2/alertas/me/status mantém os campos anteriores e acrescenta latitude, longitude, criadaEm, expiraEm e segundosRestantes. Identidade vem da sessão; parâmetros de usuário não escolhem a autora. Resposta protegida/no-store, 401 sem sessão válida, 404 sem SOS vigente. Nenhuma coluna nova ou migração. Aplicativos anteriores ignoram os campos adicionais; aplicativo novo sem esses campos mostra informações indisponíveis, sem inventar ponto ou prazo.

Os horários LocalDateTime mantêm a interpretação já usada pelo servidor e banco, sem converter os registros existentes para UTC. ExpiraEm corresponde à criação mais uma hora. SegundosRestantes é calculado pelo relógio do servidor; o Android desconta tempo monotônico a partir do início da requisição, sem depender do horário civil do aparelho. É uma estimativa conservadora, podendo reduzir pelo tempo da requisição. Chegar a zero não encerra localmente nem permite novo SOS: a tela aguarda o status do servidor. Falha de rede mantém último estado confirmado e aviso. Sessão e prazo continuam autorizados no backend.

## Mapa e privacidade

Reutiliza MapLibre/OpenFreeMap já instalado. Anel geodésico de 1 km é ilustrativo; elegibilidade continua calculada no servidor. Nenhuma nova aquisição de GPS para o mapa do SOS: usa a posição gravada naquele alerta. Atribuições e aviso ao provedor permanecem visíveis. Sem novas dependências, trajetos, mapa de pessoas ou notificações reais nesta implementação.

## Validação manual

1. Reiniciar a API local com perfil teste e ddl-auto=validate para carregar os campos novos, mantendo guardia_teste/guardia_teste_app.
2. Executar o Android atualizado. Criar SOS com conta e coordenadas fictícias; conferir detalhe, marcador, raio, início e contador.
3. Voltar à Home e reabrir por Ver meu alerta. Reabrir o aplicativo com SOS vigente e conferir recuperação pelo status.
4. Confirmar ajuda por outra conta fictícia e conferir nome e chat da autora. Sem ajuda confirmada, chat não deve aparecer.
5. Simular falha de rede: manter último estado/aviso sem confirmar encerramento. Restaurar conexão e conferir atualização.
6. Encerrar como autora e confirmar saída do detalhe; confirmar expiração com horário controlado em teste isolado e retomada após reinício. Não usar banco original nem enviar push sem autorização específica.

Testes automatizados e resultados de cada rodada ficam registrados em PROGRESSO_GUARDIA.md. Testes de tela com mapa local/fictício não substituem esta validação integrada.
