-- PROPOSTA PARA REVISAO. Nao executar antes de autorizar o DDL.
-- Executar apenas como guardia_teste_app, com privilegios exclusivos sobre guardia_teste.
-- Nao usar ddl-auto=update. As instrucoes abaixo sao apenas aditivas.
CREATE TABLE guardia_teste.eventos_push (
    id BIGINT NOT NULL AUTO_INCREMENT,
    alerta_id BIGINT NOT NULL,
    modo VARCHAR(16) NOT NULL,
    criado_em DATETIME(6) NOT NULL,
    expira_em DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_evento_push_alerta UNIQUE (alerta_id),
    KEY idx_evento_push_modo (modo, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
