-- ============================================================
-- Seed BAT Uberlandia - dados de exemplo do dashboard
-- Idempotente (MERGE por id) - roda a cada inicio sem duplicar
-- ============================================================

-- SETORES
MERGE INTO setores (id, nome, descricao) KEY(id) VALUES (1, 'Producao', 'Linha de producao de cigarros');
MERGE INTO setores (id, nome, descricao) KEY(id) VALUES (2, 'Embalagem', 'Encaixotamento e embalagem secundaria');
MERGE INTO setores (id, nome, descricao) KEY(id) VALUES (3, 'Logistica', 'Esteiras e armazenagem de PA');
MERGE INTO setores (id, nome, descricao) KEY(id) VALUES (4, 'Utilidades', 'Vapor, ar comprimido e climatizacao');
MERGE INTO setores (id, nome, descricao) KEY(id) VALUES (5, 'Manutencao', 'Oficina e almoxarifado tecnico');

-- USUARIOS
MERGE INTO usuarios (id, login, senha, nome, tipo, setor_id) KEY(id) VALUES (1, 'rbarbosa', '{noop}123', 'Ricardo Barbosa', 'LIDER', 1);
MERGE INTO usuarios (id, login, senha, nome, tipo, setor_id) KEY(id) VALUES (2, 'paulino', '{noop}123', 'Paulo Almeida', 'TECNICO', 5);
MERGE INTO usuarios (id, login, senha, nome, tipo, setor_id) KEY(id) VALUES (3, 'asantos', '{noop}123', 'Andre Santos', 'TECNICO', 5);
MERGE INTO usuarios (id, login, senha, nome, tipo, setor_id) KEY(id) VALUES (4, 'mferreira', '{noop}123', 'Marcos Ferreira', 'TECNICO', 5);
MERGE INTO usuarios (id, login, senha, nome, tipo, setor_id) KEY(id) VALUES (5, 'clima', '{noop}123', 'Carlos Lima', 'TECNICO', 5);
MERGE INTO usuarios (id, login, senha, nome, tipo, setor_id) KEY(id) VALUES (6, 'frocha', '{noop}123', 'Felipe Rocha', 'ESPECIALISTA', 5);
MERGE INTO usuarios (id, login, senha, nome, tipo, setor_id) KEY(id) VALUES (7, 'jcosta', '{noop}123', 'Juliana Costa', 'ESPECIALISTA', 5);
MERGE INTO usuarios (id, login, senha, nome, tipo, setor_id) KEY(id) VALUES (8, 'vsilva', '{noop}123', 'Vanessa Silva', 'OPERADOR', 1);
MERGE INTO usuarios (id, login, senha, nome, tipo, setor_id) KEY(id) VALUES (9, 'ldias', '{noop}123', 'Lucas Dias', 'OPERADOR', 2);

-- MAQUINAS
MERGE INTO maquinas (id, nome, modelo, numero_serie, status, setor_id) KEY(id) VALUES (1, 'Embaladora Primaria 01', 'GD-XP', 'EP-001', 'PARADA', 1);
MERGE INTO maquinas (id, nome, modelo, numero_serie, status, setor_id) KEY(id) VALUES (2, 'Maquina de Cortar 02', 'MCP-9', 'MC-002', 'PARADA', 1);
MERGE INTO maquinas (id, nome, modelo, numero_serie, status, setor_id) KEY(id) VALUES (3, 'Encaixotadora 01', 'EC-2000', 'EX-003', 'PARADA', 2);
MERGE INTO maquinas (id, nome, modelo, numero_serie, status, setor_id) KEY(id) VALUES (4, 'Esteira Transportadora 04', 'ET-50', 'ES-004', 'PARADA', 3);
MERGE INTO maquinas (id, nome, modelo, numero_serie, status, setor_id) KEY(id) VALUES (5, 'Caldeira de Vapor', 'CV-1000', 'CA-005', 'MANUTENCAO', 4);
MERGE INTO maquinas (id, nome, modelo, numero_serie, status, setor_id) KEY(id) VALUES (6, 'Compressor de Ar', 'CA-75', 'CP-006', 'PARADA', 4);
MERGE INTO maquinas (id, nome, modelo, numero_serie, status, setor_id) KEY(id) VALUES (7, 'Paletizadora 01', 'PL-800', 'PA-007', 'MANUTENCAO', 2);

-- CHAMADOS
-- 1 - concluido (ELETRICA) - Paulo
MERGE INTO chamados (id, titulo, descricao, status, motivo_falha, maquina_id, tecnico_id, inicio_reparo, tempo_reparo_acumulado, data_abertura, data_conclusao) KEY(id) VALUES (1, 'Falha no sensor de temperatura', 'Sensor nao respondendo na embaladora', 'CONCLUIDO', 'ELETRICA', 1, 2, '2026-06-10 08:00:00', 1500, '2026-06-10 07:50:00', '2026-06-10 08:25:00');
-- 2 - concluido (MECANICA) - Andre
MERGE INTO chamados (id, titulo, descricao, status, motivo_falha, maquina_id, tecnico_id, inicio_reparo, tempo_reparo_acumulado, data_abertura, data_conclusao) KEY(id) VALUES (2, 'Desalinhamento da correia', 'Correia transportadora desalinhada gerando paradas', 'CONCLUIDO', 'MECANICA', 2, 3, '2026-06-11 09:00:00', 2100, '2026-06-11 08:50:00', '2026-06-11 09:35:00');
-- 3 - concluido (PNEUMATICA) - Marcos
MERGE INTO chamados (id, titulo, descricao, status, motivo_falha, maquina_id, tecnico_id, inicio_reparo, tempo_reparo_acumulado, data_abertura, data_conclusao) KEY(id) VALUES (3, 'Vazamento no cilindro pneumatico', 'Cilindro com vazamento de ar na encaixotadora', 'CONCLUIDO', 'PNEUMATICA', 3, 4, '2026-06-12 10:00:00', 1800, '2026-06-12 09:50:00', '2026-06-12 10:30:00');
-- 4 - concluido (ELETRICA) - Paulo
MERGE INTO chamados (id, titulo, descricao, status, motivo_falha, maquina_id, tecnico_id, inicio_reparo, tempo_reparo_acumulado, data_abertura, data_conclusao) KEY(id) VALUES (4, 'Disjuntor desarmando', 'Disjuntor caindo repetidamente na caldeira', 'CONCLUIDO', 'ELETRICA', 5, 2, '2026-06-13 11:00:00', 900, '2026-06-13 10:50:00', '2026-06-13 11:15:00');
-- 5 - concluido (HIDRAULICA) - Carlos
MERGE INTO chamados (id, titulo, descricao, status, motivo_falha, maquina_id, tecnico_id, inicio_reparo, tempo_reparo_acumulado, data_abertura, data_conclusao) KEY(id) VALUES (5, 'Baixa pressao hidraulica', 'Pressao abaixo do ideal no sistema da paletizadora', 'CONCLUIDO', 'HIDRAULICA', 7, 5, '2026-06-14 12:00:00', 2700, '2026-06-14 11:50:00', '2026-06-14 12:35:00');
-- 6 - concluido (ELETRONICA) - Andre
MERGE INTO chamados (id, titulo, descricao, status, motivo_falha, maquina_id, tecnico_id, inicio_reparo, tempo_reparo_acumulado, data_abertura, data_conclusao) KEY(id) VALUES (6, 'Falha no CLP da esteira', 'CLP apresentando erro de comunicacao', 'CONCLUIDO', 'ELETRONICA', 4, 3, '2026-06-15 13:00:00', 1200, '2026-06-15 12:50:00', '2026-06-15 13:10:00');
-- 7 - concluido (SOFTWARE) - Paulo
MERGE INTO chamados (id, titulo, descricao, status, motivo_falha, maquina_id, tecnico_id, inicio_reparo, tempo_reparo_acumulado, data_abertura, data_conclusao) KEY(id) VALUES (7, 'Erro de parametrizacao', 'Receita incorreta carregada na embaladora', 'CONCLUIDO', 'SOFTWARE', 1, 2, '2026-06-16 14:00:00', 600, '2026-06-16 13:50:00', '2026-06-16 14:05:00');
-- 8 - concluido (DESGASTE) - Marcos
MERGE INTO chamados (id, titulo, descricao, status, motivo_falha, maquina_id, tecnico_id, inicio_reparo, tempo_reparo_acumulado, data_abertura, data_conclusao) KEY(id) VALUES (8, 'Desgaste de faca rotativa', 'Faca da maquina de cortar sem fio', 'CONCLUIDO', 'DESGASTE', 2, 4, '2026-06-17 15:00:00', 3600, '2026-06-17 14:50:00', '2026-06-17 15:50:00');
-- 9 - concluido (OPERACIONAL) - Carlos
MERGE INTO chamados (id, titulo, descricao, status, motivo_falha, maquina_id, tecnico_id, inicio_reparo, tempo_reparo_acumulado, data_abertura, data_conclusao) KEY(id) VALUES (9, 'Erro de operacao - parada indevida', 'Operador acionou parada incorreta na encaixotadora', 'CONCLUIDO', 'OPERACIONAL', 3, 5, '2026-06-18 16:00:00', 1500, '2026-06-18 15:50:00', '2026-06-18 16:15:00');
-- 10 - concluido (MECANICA) - Andre
MERGE INTO chamados (id, titulo, descricao, status, motivo_falha, maquina_id, tecnico_id, inicio_reparo, tempo_reparo_acumulado, data_abertura, data_conclusao) KEY(id) VALUES (10, 'Ruido excessivo no compressor', 'Mancal do compressor com ruido anormal', 'CONCLUIDO', 'MECANICA', 6, 3, '2026-06-19 17:00:00', 2400, '2026-06-19 16:50:00', '2026-06-19 17:30:00');
-- 11 - concluido (OUTROS) - Paulo
MERGE INTO chamados (id, titulo, descricao, status, motivo_falha, maquina_id, tecnico_id, inicio_reparo, tempo_reparo_acumulado, data_abertura, data_conclusao) KEY(id) VALUES (11, 'Limpeza de rotina nao programada', 'Necessidade de limpeza profunda nao prevista', 'CONCLUIDO', 'OUTROS', 5, 2, '2026-06-20 08:00:00', 3000, '2026-06-20 07:50:00', '2026-06-20 08:40:00');
-- 12 - concluido (ELETRICA) - Marcos
MERGE INTO chamados (id, titulo, descricao, status, motivo_falha, maquina_id, tecnico_id, inicio_reparo, tempo_reparo_acumulado, data_abertura, data_conclusao) KEY(id) VALUES (12, 'Curto no painel da paletizadora', 'Curto circuito no painel eletrico principal', 'CONCLUIDO', 'ELETRICA', 7, 4, '2026-06-21 09:00:00', 2000, '2026-06-21 08:50:00', '2026-06-21 09:33:00');

-- CHAMADOS EM OUTROS STATUS
-- 13 - aberto (HIDRAULICA)
MERGE INTO chamados (id, titulo, descricao, status, motivo_falha, maquina_id, data_abertura) KEY(id) VALUES (13, 'Vazamento de oleo hidraulico', 'Poco de oleo sob a maquina de cortar', 'ABERTO', 'HIDRAULICA', 2, '2026-07-06 07:00:00');
-- 14 - aberto (MECANICA)
MERGE INTO chamados (id, titulo, descricao, status, motivo_falha, maquina_id, data_abertura) KEY(id) VALUES (14, 'Esteira travando', 'Esteira de logistica com travamento intermitente', 'ABERTO', 'MECANICA', 4, '2026-07-06 07:30:00');
-- 15 - aberto (sem motivo)
MERGE INTO chamados (id, titulo, descricao, status, motivo_falha, maquina_id, data_abertura) KEY(id) VALUES (15, 'Ruido estranho na embaladora', 'Operador relatou ruido atipico', 'ABERTO', NULL, 1, '2026-07-06 07:45:00');
-- 16 - em andamento (ELETRONICA) - Andre
MERGE INTO chamados (id, titulo, descricao, status, motivo_falha, maquina_id, tecnico_id, inicio_reparo, tempo_reparo_acumulado, data_abertura) KEY(id) VALUES (16, 'Falha inversor frequencia', 'Inversor da embaladora apresentando erro E04', 'EM_ANDAMENTO', 'ELETRONICA', 1, 3, '2026-07-06 06:00:00', 2400, '2026-07-06 05:50:00');
-- 17 - em andamento (PNEUMATICA) - Carlos
MERGE INTO chamados (id, titulo, descricao, status, motivo_falha, maquina_id, tecnico_id, inicio_reparo, tempo_reparo_acumulado, data_abertura) KEY(id) VALUES (17, 'Pressao de ar baixa', 'Pressao do sistema pneumatico abaixo de 6 bar', 'EM_ANDAMENTO', 'PNEUMATICA', 6, 5, '2026-07-06 06:30:00', 900, '2026-07-06 06:20:00');
-- 18 - pausado (MECANICA) - Paulo
MERGE INTO chamados (id, titulo, descricao, status, motivo_falha, maquina_id, tecnico_id, inicio_reparo, inicio_pausa, tempo_reparo_acumulado, data_abertura) KEY(id) VALUES (18, 'Troca de rolamento', 'Rolamento da maquina de cortar com folga', 'PAUSADO', 'MECANICA', 2, 2, '2026-07-06 05:00:00', '2026-07-06 06:00:00', 1800, '2026-07-06 04:50:00');
-- 19 - escalado (SOFTWARE) - especialista Felipe
MERGE INTO chamados (id, titulo, descricao, status, motivo_falha, maquina_id, tecnico_id, especialista_id, inicio_reparo, inicio_pausa, tempo_reparo_acumulado, data_abertura, data_escalacao) KEY(id) VALUES (19, 'Falha na integracao MES', 'Comunicacao com MES intermitente na paletizadora', 'ESCALADO', 'SOFTWARE', 7, 4, 6, '2026-07-06 04:00:00', '2026-07-06 05:00:00', 3000, '2026-07-06 03:50:00', '2026-07-06 05:00:00');
-- 20 - aberto (ELETRICA)
MERGE INTO chamados (id, titulo, descricao, status, motivo_falha, maquina_id, data_abertura) KEY(id) VALUES (20, 'Lampada de sinalizacao queimada', 'Torre de sinalizacao sem luz verde na encaixotadora', 'ABERTO', 'ELETRICA', 3, '2026-07-06 08:00:00');

-- NOTIFICACOES
MERGE INTO notificacoes (id, tipo, mensagem, data_envio, lida, chamado_id, usuario_id) KEY(id) VALUES (1, 'ALERTA_30MIN', 'Chamado #16 (Falha inversor frequencia) ultrapassou 30 minutos de reparo na maquina Embaladora Primaria 01', '2026-07-06 06:35:00', false, 16, 1);
MERGE INTO notificacoes (id, tipo, mensagem, data_envio, lida, chamado_id, usuario_id) KEY(id) VALUES (2, 'ESCALACAO', 'Chamado #19 escalado para voce. Maquina: Paletizadora 01. Titulo: Falha na integracao MES', '2026-07-06 05:00:00', false, 19, 6);
MERGE INTO notificacoes (id, tipo, mensagem, data_envio, lida, chamado_id, usuario_id) KEY(id) VALUES (3, 'CONCLUSAO', 'Chamado #12 concluido. Tempo total: 33 min', '2026-06-21 09:33:00', false, 12, 4);
