# GUIA DE IMPLEMENTACAO - Dashboard BAT Brasil

## Introducao

Este documento descreve passo a passo como as FEATURES AVANCADAS foram
implementadas neste projeto Spring Boot + Thymeleaf, para que alunos do SENAI
possam entender, reproduzir e adaptar para seus proprios projetos.

---

## Sumario

1. [Autenticacao e Controle de Acesso (Spring Security)](#1-autenticacao-e-controle-de-acesso)
2. [Roteamento Automatico de Chamados](#2-roteamento-automatico-de-chamados)
3. [Atualizacao em Tempo Real com SSE (Server-Sent Events)](#3-atualizacao-em-tempo-real-com-sse)
4. [Status Operacional Explicito das Maquinas](#4-status-operacional-explicito-das-maquinas)
5. [Relatorios Exportaveis (Excel com Apache POI)](#5-relatorios-exportaveis)
6. [Analise de Tendencias Temporais (Line Charts)](#6-analise-de-tendencias-temporais)
7. [ricao para o seu projeto](#7-adaptacao-para-o-seu-projeto)
8. [Comandos Uteis](#8-comandos-uteis)

---

## 1. Autenticacao e Controle de Acesso

### O que foi implementado

- Tela de login corporativa (logo BAT, tema azul)
- Spring Security com autenticacao baseada na tabela `usuarios`
- Cada tipo de usuario (OPERADOR, TECNICO, LIDER, ESPECIALISTA) vira uma ROLE
- Botao "Sair" (logout) no topbar
- Nome do usuario logado exibido no topo (substitui o nome fixo "Ricardo Barbosa")

### Passo a passo

#### Passo 1: Adicionar dependencia no `pom.xml`

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

Isso baixa o Spring Security e configura automaticamente a_seguranca da aplicacao.

#### Passo 2: Criar o UserDetailsService

O `UserDetailsService` e a ponte entre o Spring Security e o seu banco de dados.
Ele recebe o login digitado e devolve um objeto `UserDetails` (com senha e role).

Arquivo: `config/CustomUserDetailsService.java`

```java
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByLogin(login)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario nao encontrado"));

        String role = "ROLE_" + usuario.getTipo().name(); // ex: ROLE_TECNICO

        return User.builder()
                .username(usuario.getLogin())
                .password(usuario.getSenha())
                .authorities(List.of(new SimpleGrantedAuthority(role)))
                .build();
    }
}
```

**Pontos-chave para entender:**
- `ROLE_` e o prefixo padrao do Spring Security para roles
- `Usuario.Tipo` (enum) vira a role automaticamente
- O metodo `findByLogin` ja existia no `UsuarioRepository`

#### Passo 3: Criar o SecurityConfig

Arquivo: `config/SecurityConfig.java`

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/css/**", "/js/**", "/bat_logo.png").permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/dashboard", true)
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
            );
        return http.build();
    }
}
```

**Pontos-chave:**
- `permitAll()` libera CSS, JS e a tela de login sem autenticacao
- `anyRequest().authenticated()` exige login em todas as outras paginas
- `PasswordEncoderFactories.createDelegatingPasswordEncoder()` permite multiplos
  formatos de senha. O prefixo `{noop}` indica senha em texto puro (para o seed).

#### Passo 4: Senhas no data.sql

As senhas no `data.sql` usam o prefixo `{noop}` para funcionar com o
DelegatingPasswordEncoder:

```sql
MERGE INTO usuarios (id, login, senha, ...) VALUES (1, 'rbarbosa', '{noop}123', ...);
```

`{noop}` = "NoOpPasswordEncoder", nao criptografa. Em producao, use `{bcrypt}`.

#### Passo 5: Controller de login

```java
@Controller
@RequestMapping("/login")
public class AuthController {
    @GetMapping
    public String login() { return "login"; }
}
```

#### Passo 6: Template de login (`templates/login.html`)

Formulario HTML padrao com `th:action="@{/login}" method="post"` e campos
`username` e `password`. Mensagens de erro/sucesso via `${param.error}` e
`${param.logout}`.

#### Passo 7: Exibir usuario logado

No `GlobalModelAttributes` (ControllerAdvice):

```java
@ModelAttribute("usuarioLogadoNome")
public String usuarioLogadoNome() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    return (auth != null && auth.isAuthenticated()) ? auth.getName() : "";
}
```

No template: `th:text="${usuarioLogadoNome}"`.

---

## 2. Roteamento Automatico de Chamados

### O que foi implementado

Quando um chamado esta ABERTO, o sistema calcula automaticamente qual tecnico e o
mais adequado para atende-lo, considerando:
- **Carga de trabalho**: tecnico com menos chamados ativos ganha prioridade
- **Experiencia**: tecnico com mais resolucoes do mesmo motivo de falha ganha pontos
- **Setor**: se o tecnico for do mesmo setor da maquina, ganha bonus

O sistema mostra a sugestao na tela de detalhe do chamado com a justificativa
e um botao "Atribuir automaticamente".

### Passo a passo

#### Passo 1: Criar o RoteamentoService

Arquivo: `service/RoteamentoService.java`

```java
@Service
public class RoteamentoService {

    public SugestaoTecnico sugerirTecnico(Maquina maquina, MotivoFalha motivo) {
        List<Usuario> tecnicos = usuarioRepository.findByTipo(Usuario.Tipo.TECNICO);

        for (Usuario tec : tecnicos) {
            int ativos = chamadoRepository
                .findByTecnicoIdAndStatusIn(tec.getId(), statusAtivos).size();
            int experiencia = chamadoRepository
                .countByTecnicoIdAndMotivoFalhaAndStatus(tec.getId(), motivo, CONCLUIDO);

            double score = 0;
            score -= ativos * 10;      // menos chamados ativos = melhor
            score += experiencia * 5;  // mais experiencia = melhor
            if (mesmoSetor) score += 15; // bonus de setor
        }
        // retorna o tecnico com maior score
    }
}
```

**Logica de pontuacao (algoritmo de scoring):**
- Cada chamado ativo: -10 pontos (carga de trabalho)
- Cada resolucao do mesmo motivo: +5 pontos (experiencia)
- Mesmo setor da maquina: +15 pontos (proximidade)

A classe interna `SugestaoTecnico` e um DTO que carrega:
`tecnicoId`, `tecnicoNome`, `chamadosAtivos`, `experienciaMotivo`, `score`, `justificativa`

#### Passo 2: Adicionar queries no repository

```java
List<Chamado> findByTecnicoIdAndStatusIn(Long tecnicoId, List<Status> statusList);
long countByTecnicoIdAndMotivoFalhaAndStatus(Long tecnicoId, MotivoFalha motivo, Status status);
```

#### Passo 3: Integrar no ChamadoController

No `detalheChamado()`, se o chamado estiver ABERTO, calcula a sugestao:

```java
if (chamado.getStatus() == Status.ABERTO) {
    SugestaoTecnico sugestao = roteamentoService.sugerirTecnico(maquina, motivo);
    model.addAttribute("sugestaoTecnico", sugestao);
}
```

E um novo endpoint `POST /{id}/auto-assumir` que atribui automaticamente:
```java
SugestaoTecnico sugestao = roteamentoService.sugerirTecnico(...)
chamadoService.iniciarAtendimento(id, tecnicoSugerido);
```

#### Passo 4: Template

No `chamado-detalhe.html`, quando ABERTO, mostra:
```html
<div class="sugestao-rota">
    <h3>Tecnico recomendado pelo sistema</h3>
    <span th:text="${sugestaoTecnico.tecnicoNome}"></span>
    <div th:text="${sugestaoTecnico.justificativa}"></div>
    <form th:action="@{/chamados/{id}/auto-assumir(id=${chamado.id})}">
        <button>Atribuir automaticamente</button>
    </form>
</div>
```

### Como adaptar para seu projeto

1. Defina os criterios de pontuacao que fazem sentido para o seu dominio
2. Pode adicionar criterios como: horario de trabalho, distancia, avaliacao
3. A classe `SugestaoTecnico` pode ter mais campos conforme necessidade

---

## 3. Atualizacao em Tempo Real com SSE

### O que foi implementado

O cronometro de reparo na tela de detalhe do chamado agora atualiza em **tempo
real**, a cada segundo, sem recarregar a pagina. Usa **Server-Sent Events (SSE)**
- uma tecnologia nativa dos navegadores que permite o servidor "empurrar" dados
para o cliente sem WebSocket.

### Passo a passo

#### Passo 1: Criar o SseController

Arquivo: `controller/SseController.java`

```java
@RestController
@RequestMapping("/api/sse")
public class SseController {

    @GetMapping("/chamado/{id}/tempo")
    public SseEmitter streamTempo(@PathVariable Long id) {
        SseEmitter emitter = new SseEmitter(0L); // 0L = sem timeout

        // Agenda envio a cada 1 segundo
        scheduler.scheduleAtFixedRate(() -> {
            Chamado c = chamadoRepository.findById(id);
            long segundos = c.getTempoReparoSegundos();
            emitter.send(SseEmitter.event()
                .name("tempo")
                .data("{\"segundos\":" + segundos + ",\"atrasado\":" + c.isAtrasado() + "}"));
        }, 0, 1, TimeUnit.SECONDS);

        return emitter;
    }
}
```

**Pontos-chave:**
- `SseEmitter` e a classe do Spring MVC para SSE
- `0L` = sem timeout (conexao fica aberta enquanto o usuario estiver na pagina)
- `SseEmitter.event().name("tempo").data(...)` cria o evento nomeado
- Um `ScheduledExecutorService` envia atualizacoes periodicas
- Mapa `ConcurrentHashMap` guarda os emitters ativos

#### Passo 2: JavaScript no template (EventSource)

No `chamado-detalhe.html`:

```javascript
var evtSource = new EventSource('/api/sse/chamado/' + chamadoId + '/tempo');

evtSource.addEventListener('tempo', function (e) {
    var data = JSON.parse(e.data);
    document.getElementById('cronoVal').textContent = data.formatado;
    if (data.atrasado) {
        cronometro.classList.add('atrasado');
    }
});

evtSource.onerror = function () { evtSource.close(); };
```

**Como SSE funciona:**
1. Browser abre conexao HTTP GET com o servidor
2. Servidor mantem a conexao aberta e envia "eventos"
3. Browser dispara `addEventListener` para cada evento
4. Quando a pagina fecha, a conexao e encerrada automaticamente

### SSE vs WebSocket

| Aspecto | SSE | WebSocket |
|---------|-----|-----------|
| Direcao | Servidor -> Cliente | Bidirecional |
| Protocolo | HTTP | WS |
| Complexidade | Baixa | Media |
| Ideal para | Atualizacoes de status | Chat, jogos |

O SSE foi escolhido porque so precisamos que o servidor envie dados de tempo — nao
precisamos bidirecionalidade. E mais simples e funciona com HTTP normal.

---

## 4. Status Operacional Explicito das Maquinas

### O que foi implementado

- Campo `status` na entidade `Maquina` com os valores: OPERANDO, PARADA, MANUTENCAO
- Pagina de monitoramento (`/maquinas`) com cards visuais de todas as maquinas
- Pagina de detalhe por maquina (`/maquinas/{id}`) com:
  - Historico completo de manutencao
  - Downtime total acumulado
  - MTBF calculado
  - Chamados ativos
  - Botao para alterar status manualmente

### Passo a passo

#### Passo 1: Adicionar enum e campo na entidade

```java
public enum StatusMaquina { OPERANDO, PARADA, MANUTENCAO }

@Enumerated(EnumType.STRING)
@Column(nullable = false)
@Builder.Default
private StatusMaquina status = StatusMaquina.OPERANDO;
```

Como `ddl-auto=update`, o Hibernate adiciona a coluna automaticamente.

#### Passo 2: Atualizar data.sql

```sql
MERGE INTO maquinas (..., status, ...) KEY(id) VALUES (..., 'PARADA', ...);
```

#### Passo 3: Queries no repository

```java
List<Chamado> findByMaquinaIdOrderByDataAberturaDesc(Long maquinaId);
Long sumTempoReparoByMaquinaId(Long maquinaId); // soma todo o downtime
```

#### Passo 4: Controller e templates

O `MaquinaController` monta um Map para cada maquina com:
`maquina`, `chamadosAtivos`, `tempoDowntimeSegundos`, `totalChamados`, `mtbfHoras`

### Calcular downtime

O "downtime" e a soma de todos os `tempoReparoAcumulado` dos chamados CONCLUIDOS
daquela maquina. Isso representa quanto tempo a maquina ficou parada.

---

## 5. Relatorios Exportaveis

### O que foi implementado

- Pagina `/relatorios` com cards de metricas e botoes de download
- Exportacao para Excel (.xlsx) usando Apache POI
- Aba 1: Lista de todos os chamados com 11 colunas
- Aba 2: Metricas gerais (total, MTTR, distribuicao por motivo)
- Filtros: todos, concluidos, abertos, em andamento

### Passo a passo

#### Passo 1: Dependencia Apache POI no pom.xml

```xml
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.3.0</version>
</dependency>
```

`poi-ooxml` trabalha com arquivos `.xlsx` (formato Office Open XML).
`poi` (sem ooxml) trabalha com `.xls` (formato antigo).

#### Passo 2: Controller de exportacao

```java
@GetMapping("/excel")
public void exportarExcel(HttpServletResponse response) throws IOException {
    response.setContentType(
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    response.setHeader("Content-Disposition",
        "attachment; filename=\"relatorio.xlsx\"");

    Workbook wb = new XSSFWorkbook();
    Sheet sheet = wb.createSheet("Chamados");

    // Cabecalho estilizado
    Row headerRow = sheet.createRow(3);
    for (int i = 0; i < headers.length; i++) {
        Cell cell = headerRow.createCell(i);
        cell.setCellValue(headers[i]);
        cell.setCellStyle(headerStyle);
    }

    // Dados
    int rowNum = 4;
    for (Chamado c : chamados) {
        Row row = sheet.createRow(rowNum++);
        row.createCell(0).setCellValue(c.getId());
        row.createCell(1).setCellValue(c.getTitulo());
        // ...
    }

    // Autoajustar colunas
    for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);

    wb.write(response.getOutputStream());
    wb.close();
}
```

**Pontos-chave:**
- `HttpServletResponse` recebe o arquivo diretamente no browser (download)
- `Content-Disposition: attachment` fora o download em vez de exibir na tela
- `XSSFWorkbook` = formato `.xlsx` (planilha moderna do Excel)
- `autoSizeColumn` ajusta a largura automaticamente

#### Passo 3: Estilos de celula

```java
CellStyle headerStyle = wb.createCellStyle();
Font headerFont = wb.createFont();
headerFont.setBold(true);
headerFont.setColor(IndexedColors.WHITE.getIndex());
headerStyle.setFont(headerFont);
headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
```

Isso cria cabecalhos com fundo azul e texto branco (cores da BAT).

### Como gerar PDF

Para PDF, seria necessario adicionalmente:
- Biblioteca OpenPDF: `com.github.librepdf:openpdf:2.0.3`
- Ou usar a funcao de impressao do navegador (window.print() com CSS print)

O Excel foi priorizado por ser o formato mais natural do ecossistema Microsoft
e mais util para analise de dados industriais.

---

## 6. Analise de Tendencias Temporais

### O que foi implementado

- Grafico de linha no dashboard mostrando evolucao de chamados concluidos
  nos ultimos 90 dias
- Linha principal: total diario de chamados
- Linhas secundarias (toggle): por motivo de falha
- Dados agrupados por dia via query JPQL com `DATE()` function

### Passo a passo

#### Passo 1: Queries de agrupamento por data

```java
@Query("SELECT DATE(c.dataConclusao) AS dia, COUNT(c) AS total " +
       "FROM Chamado c WHERE c.status = 'CONCLUIDO' AND c.dataConclusao >= :inicio " +
       "GROUP BY DATE(c.dataConclusao) ORDER BY dia ASC")
List<TendenciaDiaria> findTendenciaDiaria(@Param("inicio") LocalDateTime inicio);
```

A projection `TendenciaDiaria` e uma interface:
```java
public interface TendenciaDiaria {
    LocalDate getDia();
    Long getTotal();
}
```

Spring Data JPA preenche a interface automaticamente a partir da query.

#### Passo 2: Controller envia dados para o template

```java
List<TendenciaDiaria> tendencia = chamadoRepository.findTendenciaDiaria(...);
Map<String, Long> tendenciaMap = new LinkedHashMap<>();
for (TendenciaDiaria t : tendencia) {
    tendenciaMap.put(t.getDia().toString(), t.getTotal());
}
model.addAttribute("tendenciaDiaria", tendenciaMap);
```

#### Passo 3: JavaScript no template com Chart.js

```javascript
var tendenciaDiaria = /*[[${tendenciaDiaria}]]*/ {};

var ctx = document.getElementById('graficoTendencia');
new Chart(ctx, {
    type: 'line',
    data: {
        labels: sortedDates,
        datasets: [{
            label: 'Total de chamados',
            data: [...],
            borderColor: '#000245',
            fill: true,
            tension: 0.3
        }]
    },
    options: {
        responsive: true,
        scales: { y: { beginAtZero: true } }
    }
});
```

**Pontos-chave:**
- `th:inline="javascript"` permite passar dados Java para JS como JSON
- Chart.js tipo `line` cria o grafico de linha
- `tension: 0.3` suaviza as linhas
- Linhas secundarias com `hidden: true` comecam recolhidas (clique para mostrar)

---

## 7. Adaptacao para o seu projeto

### Estrutura geral para replicar

```
config/         <- Spring Security, PasswordEncoder, etc.
controller/     <- Rotas web (Controller) e API (RestController)
service/        <- Regras de negocio (services)
repository/     <- Interfaces JPA + projections
model/          <- Entidades JPA (tabelas)
templates/      <- HTML Thymeleaf
static/css/     <- CSS
static/js/      <- JavaScript
resources/data.sql <- Seed de dados
```

### Checklist para adaptar

1. **Autenticacao**: Mude seu `Usuario` para ter `login`, `senha` e `tipo`
2. **Roteamento**: Defina seus criterios de pontuacao no service
3. **SSE**: So mudar a query de dados no scheduler
4. **Status de maquina**: Adicione o enum de status na sua entidade
5. **Excel**: Ajuste as colunas do relatorio para suas entidades
6. **Grafico de linha**: Ajuste a query de agrupamento por data

### Bibliotecas usadas

| Biblioteca | Versao | Funcao |
|-----------|--------|--------|
| Spring Boot | 4.1.0 | Framework web |
| Spring Security | (gerenciado) | Autenticacao |
| Apache POI | 5.3.0 | Exportacao Excel |
| Chart.js | 4.4.4 (CDN) | Graficos |
| H2 Database | (gerenciado) | Banco de dados |
| Lombok | (gerenciado) | Reduz boilerplate |

### Restricoes do projeto BAT

- **Microsoft ecosystem only**: Apache POI gera `.xlsx` (formato Microsoft)
- **No Azure/SQL Server**: Usamos H2 Database (compativel)
- **Cloud-only**: Em producao, o JAR pode ser hospedado em qualquer nuvem
- **Sem APIs externas**: Tudo e self-contained no JAR

---

## 8. Comandos Uteis

### Rodar o projeto

```bash
mvn spring-boot:run
# Acesse: http://localhost:8080/dashboard
```

### Compilar (sem rodar)

```bash
mvn compile
```

### Gerar JAR

```bash
mvn package -DskipTests
# JAR em: target/dashboard-0.0.1-SNAPSHOT.jar
# Rode com: java -jar target/dashboard-0.0.1-SNAPSHOT.jar
```

### Resetar o banco de dados

```bash
# Delete o arquivo do banco
rm data/dashboard_bat.mv.db
# Reinicie a aplicacao - o data.sql recria tudo
```

### Logins de teste

| Login | Senha | Tipo |
|-------|-------|------|
| rbarbosa | 123 | LIDER |
| paulino | 123 | TECNICO |
| frocha | 123 | ESPECIALISTA |
| vsilva | 123 | OPERADOR |

### Estrutura de arquivos novos

```
config/
  SecurityConfig.java            [NOVO] Spring Security
  CustomUserDetailsService.java  [NOVO] Autenticacao por banco
controller/
  AuthController.java            [NOVO] Tela de login
  SseController.java             [NOVO] Server-Sent Events
  MaquinaController.java         [NOVO] Monitoramento de maquinas
  RelatorioController.java       [NOVO] Relatorios Excel
service/
  RoteamentoService.java         [NOVO] Sugestao de tecnico
repository/
  TendenciaDiaria.java           [NOVO] Projection para grafico de linha
  TendenciaMotivoDiaria.java     [NOVO] Projection por motivo
templates/
  login.html                     [NOVO] Tela de login
  maquinas.html                  [NOVO] Lista de maquinas
  maquina-detalhe.html           [NOVO] Detalhe da maquina
  relatorios.html                [NOVO] Pagina de relatorios
```

### Endpoints novos

| Rota | Metodo | Funcao |
|------|--------|--------|
| `/login` | GET | Tela de login |
| `/logout` | POST | Encerrar sessao |
| `/maquinas` | GET | Monitoramento de maquinas |
| `/maquinas/{id}` | GET | Detalhe da maquina |
| `/maquinas/{id}/status` | POST | Alterar status da maquina |
| `/chamados/{id}/auto-assumir` | POST | Roteamento automatico |
| `/relatorios` | GET | Pagina de relatorios |
| `/relatorios/excel` | GET | Download Excel |
| `/api/sse/chamado/{id}/tempo` | GET (SSE) | Timer ao vivo |

---

## Resumo do que foi entregue

| Requisito da BAT | Status | Onde |
|------------------|--------|------|
| Autenticacao e controle de acesso | Pronto | Spring Security + login.html |
| Roteamento automatico de chamados | Pronto | RoteamentoService + chamado-detalhe.html |
| Timer ao vivo em tempo real | Pronto | SseController + EventSource |
| Status operacional das maquinas | Pronto | MaquinaController + maquinas.html |
| Relatorios exportaveis | Pronto | RelatorioController + Apache POI |
| Analise de tendencias temporais | Pronto | Grafico de linha no dashboard |
|vincula equipamentos a profissionais | Pronto | Maquina + RoteamentoService |
| Controle de tempo de 30 min com alertas | Pronto | (ja existia) AlertaService |
| Registro de dados essenciais | Pronto | (ja existia) Chamado entity |
| MTBF e MTTR por maquina/setor/periodo | Pronto | (ja existia) ChamadoService |

---

_Documentacao gerada para o projeto SAGA SENAI de Inovacao - BAT Brasil_
_SeNAI Uberlandia - CFP Dr. Celso Charuri_