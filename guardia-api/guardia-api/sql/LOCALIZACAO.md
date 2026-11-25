# Coluna de atualização da localização

O SQL 2026-11-22-localizacao-teste.sql é uma proposta aditiva para guardia_teste. Não foi executado. Revisar permissões e estrutura com acesso somente ao banco de testes; obter autorização específica antes do DDL. Não executar no banco original, não habilitar update/create. Com validate, a API nova exigirá a coluna antes de iniciar.

A coluna é nullable, sem backfill. Posições anteriores ficam sem validade até a próxima atualização autenticada. Instantes novos são gravados pelo servidor em UTC. A configuração guardia.localizacao.validade aceita duração ISO-8601 positiva; padrão PT15M. Exatamente 15 minutos é vencido; horário futuro também é inválido. Cadastro e formatos HTTP anteriores preservados.

A regra vale para novas destinatárias, polling de não participantes, novos aceites e preparação de novas tentativas na fila. Ajudante já confirmada continua autorizada a consultar o alerta ativo e usar o chat, independentemente da validade posterior da posição. A posição do SOS permanece aquela registrada na criação. Atualização da conta não movimenta o SOS.

A fila revalida a elegibilidade imediatamente antes de despachar, usando AlertaService. Uma entrega já enviada ou em voo não pode ser desfeita. Usuária que se aproxima depois da criação pode encontrar o SOS pelo polling; a mudança não cria retroativamente novas entregas push.

No Android, atualizações ocorrem somente em primeiro plano. Após 15 minutos sem atualização aceita, a posição deixa de habilitar novas ajudas/recebimentos por proximidade. Coordenadas aproximadas dependem da precisão disponível no dispositivo e não garantem distância física exata. Cliente informa posição, portanto esta melhoria não atesta presença física nem impede falsificação de GPS.
