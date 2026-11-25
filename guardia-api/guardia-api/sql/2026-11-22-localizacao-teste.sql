-- PROPOSTA: não executada pelo Codex. Somente guardia_teste.
-- Confirmar DATABASE(), CURRENT_USER(), SHOW GRANTS e estrutura antes de autorizar.
-- Não usar ddl-auto=update; não executar se a coluna já existir.
ALTER TABLE guardia_teste.usuarios
    ADD COLUMN localizacao_atualizada_em DATETIME(6) NULL;
-- Sem backfill: posições antigas sem horário não são consideradas recentes.
-- Horários novos são UTC (Instant no backend).
-- Reversão operacional: retornar versão anterior da API preservando coluna/dados.
-- Não remover coluna nem atualizar registros automaticamente.
