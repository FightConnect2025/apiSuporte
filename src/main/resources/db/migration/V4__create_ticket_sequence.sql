-- Sequence para geração automática de número de ticket
-- Idempotente: funciona tanto em bancos novos (homolog) quanto existentes (prod)
CREATE SEQUENCE IF NOT EXISTS ticket_num_seq START WITH 1;
