-- PROPOSTA REVISAVEL: instalacao limpa, ainda nao executada/validada no MySQL.
-- PRE-REQUISITOS: schema guardia_teste JA criado e VAZIO; usuario
-- guardia_teste_app com privilegios exclusivos nesse schema.
-- Antes, seguir INSTALACAO_LIMPA.md e verificar banco/usuario/ausencia de tabelas.
-- NUNCA usar como migracao de banco existente. Sem IF NOT EXISTS intencionalmente.
-- Sem contas, senhas, dados de exemplo, DROP, ALTER ou criacao de usuarios/schema.
-- Todas as tabelas sao qualificadas: a selecao de outro schema nao muda o destino.
-- DDL MySQL pode ter commits implicitos; nao presumir rollback do arquivo completo.

CREATE TABLE guardia_teste.usuarios (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nome VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    senha VARCHAR(255) NOT NULL,
    latitude DOUBLE NULL,
    longitude DOUBLE NULL,
    localizacao_atualizada_em DATETIME(6) NULL,
    token_fcm VARCHAR(500) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_usuarios_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE guardia_teste.sessoes (
    token_hash VARCHAR(64) NOT NULL,
    usuario_id BIGINT NOT NULL,
    instalacao_id BINARY(16) NOT NULL,
    expira_em DATETIME(6) NOT NULL,
    revogada_em DATETIME(6) NULL,
    revogacao_hash VARCHAR(64) NULL,
    token_fcm VARCHAR(500) NULL,
    versao BIGINT NULL,
    PRIMARY KEY (token_hash),
    CONSTRAINT uk_sessoes_revogacao_hash UNIQUE (revogacao_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE guardia_teste.alertas (
    id BIGINT NOT NULL AUTO_INCREMENT,
    usuario_id BIGINT NOT NULL,
    latitude DOUBLE NOT NULL,
    longitude DOUBLE NOT NULL,
    ativo BIT(1) NOT NULL,
    data_hora_criacao DATETIME(6) NOT NULL,
    data_hora_encerramento DATETIME(6) NULL,
    usuario_ajudando_id BIGINT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE guardia_teste.vinculos_fcm (
    instalacao_hash VARCHAR(64) NOT NULL,
    sessao_hash VARCHAR(64) NOT NULL,
    token VARCHAR(500) NULL,
    token_hash VARCHAR(64) NULL,
    PRIMARY KEY (instalacao_hash),
    CONSTRAINT uk_vinculos_fcm_sessao UNIQUE (sessao_hash),
    CONSTRAINT uk_vinculos_fcm_token UNIQUE (token_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE guardia_teste.mensagens_alerta (
    id BIGINT NOT NULL AUTO_INCREMENT,
    alerta_id BIGINT NOT NULL,
    remetente_id BIGINT NOT NULL,
    texto VARCHAR(500) NOT NULL,
    enviada_em DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_mensagens_alerta_id (alerta_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE guardia_teste.eventos_push (
    id BIGINT NOT NULL AUTO_INCREMENT,
    alerta_id BIGINT NOT NULL,
    modo VARCHAR(16) NOT NULL,
    criado_em DATETIME(6) NOT NULL,
    expira_em DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_evento_push_alerta UNIQUE (alerta_id),
    KEY idx_evento_push_modo (modo, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE guardia_teste.entregas_push (
    id BIGINT NOT NULL AUTO_INCREMENT,
    evento_id BIGINT NOT NULL,
    usuario_id BIGINT NOT NULL,
    instalacao_hash VARCHAR(64) NOT NULL,
    sessao_hash VARCHAR(64) NOT NULL,
    estado VARCHAR(24) NOT NULL,
    tentativas INT NOT NULL,
    proxima_tentativa DATETIME(6) NOT NULL,
    reservada_ate DATETIME(6) NULL,
    reserva VARCHAR(36) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_entrega_push_destino UNIQUE (evento_id, instalacao_hash, sessao_hash),
    KEY idx_entrega_push_pendente (estado, proxima_tentativa, reservada_ate),
    CONSTRAINT fk_entrega_push_evento FOREIGN KEY (evento_id)
        REFERENCES guardia_teste.eventos_push (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
