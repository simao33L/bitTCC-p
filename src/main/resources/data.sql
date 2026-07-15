-- Usuario administrador inicial
-- Senha: admin
MERGE INTO setores (id, nome, descricao) KEY(id) VALUES (1, 'Manutencao', 'Setor de manutencao');
MERGE INTO usuarios (id, login, senha, nome, tipo, setor_id) KEY(id) VALUES (1, 'admin', '{noop}admin', 'Administrador', 'LIDER', 1);
