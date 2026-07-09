# Dashboard BAT Uberlandia - Gestao de Chamados de Manutencao

Projeto Spring Boot + Thymeleaf de dashboard industrial para gerenciamento de
chamados de manutencao, seguindo o padrao visual corporativo da BAT Brasil
(azul #003DA5 + branco, fonte Arial).

## Sumario

- [Como rodar](#como-rodar)
- [Estrutura do projeto](#estrutura-do-projeto)
- [Entidades (modelo de negocio)](#entidades-modelo-de-negocio)
- [Features implementadas](#features-implementadas)
- [Fluxo das paginas](#fluxo-das-paginas)
- [Como os dados de exemplo funcionam](#como-os-dados-de-exemplo-funcionam)
- [Checklist para alunos](#checklist-para-alunos)

## Como rodar

```bash
# 1. Clone o repositorio
git clone <url>

# 2. Entre na pasta
cd projetoFinal_BAT

# 3. Compile e rode (requer Java 17+)
./mvnw spring-boot:run

# 4. Acesse no navegador
http://localhost:8080/dashboard
```

O banco H2 em arquivo fica em `data/dashboard_bat.mv.db`.
Para resetar os dados, delete esse arquivo e reinicie a aplicacao.
O arquivo `src/main/resources/data.sql` contem os dados de exemplo e roda
automaticamente a cada inicio (MERGE por id, nao duplica registros existentes).

## Estrutura do projeto

```
projetoFinal_BAT/
  pom.xml                          # Maven, Spring Boot 4.1.0, Java 17
  src/main/java/com/bat/uberlandia/dashboard/
    DashboardApplication.java      # @SpringBootApplication + @EnableScheduling
    model/
      Chamado.java                 # Entidade principal (status, motivo, tempos)
      Maquina.java                 # Maquinas industriais por setor
      Notificacao.java             # Alertas e notificacoes
      Setor.java                   # Setores da fabrica
      Usuario.java                 # Tecnicos, especialistas, lideres, operadores
    repository/
      ChamadoRepository.java       # Queries JPQL customizadas
      MaquinaRepository.java
      NotificacaoRepository.java   # Marcar lidas + listagens
      SetorRepository.java
      UsuarioRepository.java
      RankingTecnico.java          # Projecao para ranking (id, nome, total, tempoMedio)
      MotivoFalhaCount.java        # Projecao para grafico (motivo, total)
    service/
      ChamadoService.java          # Regras de negocio (abrir, pausar, MTBF, etc.)
      AlertaService.java           # @Scheduled verificando 30min + notificacoes
    controller/
      DashboardController.java     # /dashboard com filtro por setor + grafico
      ChamadoController.java       # /chamados CRUD
      RankingController.java       # /ranking tecnicos mais resolvidos
      NotificacaoController.java   # /notificacoes marcar lidas
      HomeController.java          # /
      ApiController.java           # /api metricas e MTBF REST
      GlobalModelAttributes.java   # @ControllerAdvice (setores, alertas, etc.)
  src/main/resources/
    application.properties         # H2 file database
    data.sql                       # Seed idempotente (MERGE por id)
    static/
      css/dashboard.css            # Tema azul/branco BAT + layout bento
      css/style.css                # CSS da pagina hello (legado)
      js/app.js                    # Toggle do painel de notificacoes
    templates/
      dashboard.html               # Pagina principal com as 4 features
      ranking.html                 # Ranking de tecnicos
      chamado-lista.html           # Tabela de chamados
      chamado-form.html            # Formulario novo chamado
      chamado-detalhe.html         # Detalhe + acoes + cronometro
      hello.html                   # Pagina de teste (legado)
      fragments/layout.html        # Sidebar + topbar compartilhados
```

## Entidades (modelo de negocio)

```
Setor (Producao, Embalagem, Logistica, Utilidades, Manutencao)
  |-- Maquina (ex: Embaladora Primaria 01, Caldeira de Vapor...)
  |-- maquinas tem chamados

Usuario (OPERADOR | TECNICO | LIDER | ESPECIALISTA | VISUALIZADOR)
  |-- Tecnico assume e resolve chamados
  |-- Especialista recebe escalacoes
  |-- Lider recebe alertas de 30min

Chamado
  |-- MotivoFalha: ELETRICA, MECANICA, PNEUMATICA, HIDRAULICA,
  |               ELETRONICA, SOFTWARE, OPERACIONAL, DESGASTE, OUTROS
  |-- Status: ABERTO -> EM_ANDAMENTO -> CONCLUIDO
  |           ou ABERTO -> EM_ANDAMENTO -> PAUSADO -> EM_ANDAMENTO -> CONCLUIDO
  |           ou ABERTO -> EM_ANDAMENTO -> ESCALADO -> CONCLUIDO
  |-- Tempo de reparo (inicioReparo + pausas = tempoAcumulado)
  |-- Tempo de locomocao (opcional)
```

## Features implementadas

### 1. Grafico de pizza (Chart.js) - % de cada MotivoFalha

**Onde**: `dashboard.html` (lado esquerdo do bento grid)

**Backend**:
- `ChamadoRepository.countByMotivoFalha()` - JPQL agrupando por motivo
- `ChamadoService.getDistribuicaoMotivosFalha(setorId)` - retorna Map<MotivoFalha, Long>
- `DashboardController` coloca `distribuicaoMotivos` e `totalChamadosComMotivo` no model

**Frontend**:
- Chart.js carregado via CDN no `<head>`
- Dados passados via `th:inline="javascript"` (Map<Enum, Long> serializado como JSON)
- Grafico tipo `doughnut` com 9 cores, legenda customizada em grid
- Quando filtro de setor ativo, o grafico reflete apenas
  chamados daquele setor
- 9 motivos de falha com cores fixas (azul BAT, verde, amarelo, laranja,
  vermelho, roxo, cinza)

### 2. Ranking de tecnicos com mais chamados resolvidos

**Onde**: `ranking.html` (acessivel pelo menu lateral)

**Backend**:
- `ChamadoRepository.findRankingTecnicosConcluidos()` - JPQL:
  ```sql
  SELECT tecnico.id, tecnico.nome, COUNT(*), AVG(tempoReparoAcumulado)
  FROM Chamado WHERE status = 'CONCLUIDO' AND tecnico IS NOT NULL
  GROUP BY tecnico.id, tecnico.nome ORDER BY COUNT(*) DESC
  ```
- `RankingTecnico` (projecao interface): getId(), getNome(), getTotal(), getTempoMedioSegundos()
- `RankingController` expoe `/ranking` com lista + maxTotal

**Frontend**:
- Banner do lider (1 lugar) com nome, total e tempo medio
- Lista numerada com posicao, nome, barra de progresso proporcional
- Top 3 com posicao dourada/prata/bronze
- Tempo medio de reparo em minutos por tecnico

### 3. Filtro por setor no dashboard

**Onde**: `dashboard.html` (barra superior de filtros)

**Backend**:
- `DashboardController.dashboard(setorId, dataInicio, dataFim)`
- Se `setorId` informado, todos os cards e graficos usam apenas chamados
  daquele setor (filtro em memoria via streams)
- `distribuicaoMotivos` usa `countByMotivoFalhaAndSetor(setorId)`
- `mtbfPorMaquina` filtra maquinas: `maquinaRepository.findBySetorId(setorId)`

**Frontend**:
- `<select>` com todos os setores (carregados do `GlobalModelAttributes`)
- `onchange="this.form.submit()"` para submit automatico ao selecionar
- Botao "Limpar" remove o filtro
- Indicador visual "Filtrando por setor: Produção" quando ativo

### 4. Painel de notificacoes (sino no navbar com badge)

**Onde**: topo direito em todas as paginas (via fragmento `layout.html`)

**Backend**:
- `AlertaService` (@Scheduled a cada 30s) verifica chamados >30min
  e cria notificacoes para lideres e especialistas
- `GlobalModelAttributes` (@ControllerAdvice) injeta `totalAlertas`
  e `ultimasNotificacoes` em todas as paginas
- `NotificacaoController` expoe `/notificacoes/marcar-todas-lidas`
  e `/notificacoes/{id}/ler`
- Nao usa sessao/login - as notificacoes sao globais

**Frontend**:
- SVG sino inline (unico icone no sistema)
- Badge vermelho com numero de notificacoes nao lidas
- Tooltip/painel dropdown com lista das ultimas 10 notificacoes
- Cada notificacao mostra: tipo (Alerta/Escalacao/Concluido),
  mensagem e data
- Botao "Marcar todas como lidas" com formulario POST
- Fecha ao clicar fora (JS no `app.js`)
- Tags coloridas: alerta = vermelho, escalacao = laranja,
  conclusao = verde

## Fluxo das paginas

```
/ (redirect) -> /dashboard  (pagina principal)
                  |-- cards de status (abertos, em andamento, pausados...)
                  |-- grafico pizza (MotivoFalha %)
                  |-- MTBF por setor / maquina
                  |-- chamados abertos recentes
                  |
                  +-- filtro de setor (atualiza tudo acima)
                  +-- sino notificacoes (badge + dropdown)
                  +-- sidebar -> /chamados, /ranking, /chamados/novo

/chamados           tabela com filtro de status

/chamados/novo      formulario de abertura (maquina + motivo + foto)

/chamados/{id}      detalhe + cronometro + acoes
                       |-- assumir (tecnico)
                       |-- pausar / retomar
                       |-- escalar (especialista)
                       |-- concluir

/ranking            tecnicos ordenados por chamados concluidos
```

## Como os dados de exemplo funcionam

O `data.sql` usa `MERGE INTO ... KEY(id)` em vez de `INSERT`.
Isso significa que e **idempotente**: nao duplica registros se o
banco ja tem dados.

### Setores e maquinas (5 setores, 7 maquinas)

| Setor | Maquinas |
|---|---|
| Producao | Embaladora Primaria 01, Maquina de Cortar 02 |
| Embalagem | Encaixotadora 01, Paletizadora 01 |
| Logistica | Esteira Transportadora 04 |
| Utilidades | Caldeira de Vapor, Compressor de Ar |

### Usuarios (9)

| Login | Nome | Tipo |
|---|---|---|
| rbarbosa | Ricardo Barbosa | LIDER (Producao) |
| paulino | Paulo Almeida | TECNICO (Manutencao) |
| asantos | Andre Santos | TECNICO (Manutencao) |
| mferreira | Marcos Ferreira | TECNICO (Manutencao) |
| clima | Carlos Lima | TECNICO (Manutencao) |
| frocha | Felipe Rocha | ESPECIALISTA |
| jcosta | Juliana Costa | ESPECIALISTA |
| vsilva | Vanessa Silva | OPERADOR |
| ldias | Lucas Dias | OPERADOR |

### Chamados (20)

- 12 CONCLUIDOS com variados motivos e tempos de reparo,
  distribuidos entre 4 tecnicos (Paulo=4, Andre=3, Marcos=3, Carlos=2)
- 4 ABERTOS aguardando atendimento
- 2 EM_ANDAMENTO (um deles com >30min de reparo acumulado)
- 1 PAUSADO
- 1 ESCALADO para Felipe Rocha

### Notificacoes pre-criadas (3)

- Alerta de 30min no chamado #16
- Escalacao do chamado #19
- Conclusao do chamado #12

## Checklist para alunos

Para entender e reproduzir este projeto:

- [ ] **Model:** as 5 entidades em `model/` com JPA annotations
  - Relacionamentos: Setor -> Maquina -> Chamado, Usuario -> Chamado (tecnico/especialista)
  - Enums: Chamado.Status, Chamado.MotivoFalha, Usuario.Tipo
  - Lombok (@Data, @Builder) para reduzir boilerplate
- [ ] **Repository:** `ChamadoRepository` com queries JPQL
  - `@Query` com `GROUP BY` para grafico de pizza (`countByMotivoFalha`)
  - `@Query` com `GROUP BY ... ORDER BY COUNT DESC` para ranking
  - Spring Data projections (interfaces) para queries customizadas
  - `@Modifying @Transactional` para updates em `NotificacaoRepository`
- [ ] **Service:** `ChamadoService` com regras de negocio
  - Maquina de estados do chamado (aberto -> em_andamento -> concluido)
  - Calculo de MTBF (tempo entre falhas consecutivas)
  - Calculo de tempo acumulado (pausas nao contam)
- [ ] **Controller:** `DashboardController` com filtro por setor
  - Filtro via query param, aplicado em memoria com streams
  - Dados para Chart.js passados via `th:inline="javascript"`
- [ ] **Scheduler:** `@EnableScheduling` + `@Scheduled(fixedDelay=30000)`
  - Verificacao periodica de chamados atrasados
  - Criacao de notificacoes automaticas
- [ ] **Template:** Thymeleaf + CSS customizado
  - Layout bento com CSS Grid (inspirado em dashboards modernos)
  - Tema corporativo (variaveis CSS no `:root`)
  - Chart.js CDN com grafico doughnut
  - Fragmentos (`fragments/layout.html`) para sidebar/topbar reutilizaveis
  - `@ControllerAdvice` (`GlobalModelAttributes`) para atributos globais
- [ ] **Seed:** `data.sql` idempotente com `MERGE INTO ... KEY(id)`
  - Dados de exemplo realistas (fabrica BAT)
  - Respeita a estrutura de FK

## Extensoes possiveis (para alunos avancarem)

- Login/autenticacao (Spring Security) com perfis reais
- Exportacao de relatorios PDF/Excel
- Graficos de linha mostrando evolucao temporal (Chart.js)
- API REST paginada com Spring Data REST
- Testes unitarios e de integracao (JaCoCo, MockMvc)
- Deploy em Docker / nuvem (Railway, Render)
