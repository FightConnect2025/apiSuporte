CREATE TABLE IF NOT EXISTS tickets (
    id                   UUID         NOT NULL,
    usuario_id           UUID         NOT NULL,
    polano_id            UUID,
    equipe_id            UUID         NOT NULL,
    titulo               VARCHAR(200) NOT NULL,
    descricao            TEXT,
    status               VARCHAR(40)  NOT NULL,
    categoria            VARCHAR(30),
    prioridade           VARCHAR(10),
    atendente_id         UUID,
    atendente_nome       VARCHAR(200),
    nome_usuario         VARCHAR(200) NOT NULL,
    nome_equipe          VARCHAR(200) NOT NULL,
    nome_plano           VARCHAR(200) NOT NULL,
    numero_ticket        BIGINT       NOT NULL,
    reabertura_seq       INTEGER      NOT NULL DEFAULT 0,
    numero_exibicao      VARCHAR(20),
    criado_em            TIMESTAMP WITH TIME ZONE NOT NULL,
    atualizado_em        TIMESTAMP WITH TIME ZONE NOT NULL,
    fechado_em           TIMESTAMP WITH TIME ZONE,
    primeira_resposta_em TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (id)
);

ALTER TABLE tickets ADD COLUMN IF NOT EXISTS atendente_id UUID;
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS atendente_nome VARCHAR(200);
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS reabertura_seq INTEGER NOT NULL DEFAULT 0;
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS numero_exibicao VARCHAR(20);
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS primeira_resposta_em TIMESTAMP WITH TIME ZONE;

CREATE UNIQUE INDEX IF NOT EXISTS uk_ticket_numero ON tickets(numero_ticket);
CREATE INDEX IF NOT EXISTS ix_ticket_numero ON tickets(numero_ticket);
CREATE INDEX IF NOT EXISTS ix_ticket_usuario ON tickets(usuario_id);
CREATE INDEX IF NOT EXISTS ix_ticket_status ON tickets(status);
CREATE INDEX IF NOT EXISTS ix_ticket_equipe ON tickets(equipe_id);
CREATE INDEX IF NOT EXISTS ix_ticket_atendente ON tickets(atendente_id);

CREATE TABLE IF NOT EXISTS ticket_messages (
    id               UUID         NOT NULL,
    ticket_id        UUID         NOT NULL,
    autor_usuario_id UUID         NOT NULL,
    autor_tipo       VARCHAR(20)  NOT NULL,
    texto            TEXT         NOT NULL,
    criado_em        TIMESTAMP WITH TIME ZONE NOT NULL,
    autor_nome       VARCHAR(200),
    equipe_nome      VARCHAR(200),
    PRIMARY KEY (id),
    CONSTRAINT fk_ticket_message_ticket FOREIGN KEY (ticket_id) REFERENCES tickets(id)
);

CREATE TABLE IF NOT EXISTS ticket_message_attachments (
    id           UUID         NOT NULL,
    message_id   UUID         NOT NULL,
    storage_key  VARCHAR(255) NOT NULL,
    url          VARCHAR(255) NOT NULL,
    content_type VARCHAR(100),
    file_name    VARCHAR(255),
    criado_em    TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_attachment_message FOREIGN KEY (message_id) REFERENCES ticket_messages(id)
);

CREATE TABLE IF NOT EXISTS ticket_feedback (
    id        UUID    NOT NULL,
    ticket_id UUID    NOT NULL UNIQUE,
    nota      INTEGER NOT NULL,
    comentario TEXT,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_feedback_ticket FOREIGN KEY (ticket_id) REFERENCES tickets(id)
);

CREATE TABLE IF NOT EXISTS ticket_reads (
    ticket_id    UUID NOT NULL,
    usuario_id   UUID NOT NULL,
    last_read_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (ticket_id, usuario_id)
);

CREATE TABLE IF NOT EXISTS ticket_fotos (
    id           UUID          NOT NULL,
    ticket_id    UUID          NOT NULL,
    file_name    VARCHAR(255)  NOT NULL,
    content_type VARCHAR(120)  NOT NULL,
    size_bytes   BIGINT        NOT NULL,
    storage_key  VARCHAR(500)  NOT NULL,
    url          VARCHAR(800)  NOT NULL,
    criado_em    TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS webhook_configs (
    id           UUID         NOT NULL,
    equipe_id    UUID         NOT NULL,
    url          VARCHAR(500) NOT NULL,
    eventos      VARCHAR(500) NOT NULL,
    segredo      VARCHAR(255),
    ativo        BOOLEAN      NOT NULL DEFAULT TRUE,
    criado_em    TIMESTAMP WITH TIME ZONE NOT NULL,
    atualizado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS ix_webhook_equipe ON webhook_configs(equipe_id);

CREATE TABLE IF NOT EXISTS respostas_rapidas (
    id           UUID         NOT NULL,
    equipe_id    UUID         NOT NULL,
    titulo       VARCHAR(200) NOT NULL,
    conteudo     TEXT         NOT NULL,
    atalho       VARCHAR(50),
    criado_em    TIMESTAMP WITH TIME ZONE NOT NULL,
    atualizado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS ix_resposta_equipe ON respostas_rapidas(equipe_id);

CREATE TABLE IF NOT EXISTS artigos (
    id           UUID         NOT NULL,
    equipe_id    UUID,
    titulo       VARCHAR(300) NOT NULL,
    conteudo     TEXT         NOT NULL,
    tags         VARCHAR(500),
    categoria    VARCHAR(50),
    publicado    BOOLEAN      NOT NULL DEFAULT TRUE,
    ordem        INTEGER      DEFAULT 0,
    criado_em    TIMESTAMP WITH TIME ZONE NOT NULL,
    atualizado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS ix_artigo_publicado ON artigos(publicado);
CREATE INDEX IF NOT EXISTS ix_artigo_equipe ON artigos(equipe_id);
