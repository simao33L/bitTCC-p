package com.bat.uberlandia.dashboard.service;

import com.bat.uberlandia.dashboard.model.Chamado;
import com.bat.uberlandia.dashboard.model.Maquina;
import com.bat.uberlandia.dashboard.model.Setor;
import com.bat.uberlandia.dashboard.model.Usuario;
import com.bat.uberlandia.dashboard.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatbotService {

    private final ChamadoRepository chamadoRepository;
    private final MaquinaRepository maquinaRepository;
    private final SetorRepository setorRepository;
    private final UsuarioRepository usuarioRepository;
    private final ChamadoService chamadoService;

    public Map<String, Object> processMessage(String message, String username) {
        String msg = message.toLowerCase().trim();

        if (matchesAny(msg, "oi", "olá", "ola", "bom dia", "boa tarde", "boa noite", "e aí", "e ai", "hey", "hello", "fala"))
            return greeting(username);

        if (matchesAny(msg, "ajuda", "help", "o que você faz", "como funciona", "comandos", "o que voce faz"))
            return help();

        if (matchesAny(msg, "resumo", "situação", "situacao", "visão geral", "visao geral", "status geral", "como estão os chamados", "como estao"))
            return ticketsSummary();

        if (containsAny(msg, "abertos", "em aberto", "aguardando"))
            return ticketsByStatus(Chamado.Status.ABERTO, "Chamados Abertos");

        if (containsAny(msg, "em andamento", "andamento", "atendendo", "em atendimento"))
            return ticketsByStatus(Chamado.Status.EM_ANDAMENTO, "Chamados em Andamento");

        if (containsAny(msg, "pausados", "pausado"))
            return ticketsByStatus(Chamado.Status.PAUSADO, "Chamados Pausados");

        if (containsAny(msg, "escalados", "escalado"))
            return ticketsByStatus(Chamado.Status.ESCALADO, "Chamados Escalados");

        if (containsAny(msg, "concluídos", "concluidos", "finalizados", "resolvidos"))
            return ticketsByStatus(Chamado.Status.CONCLUIDO, "Chamados Concluídos");

        if (containsAny(msg, "máquina", "maquina", "máquinas", "maquinas")) {
            if (containsAny(msg, "parada", "paradas"))
                return machinesByStatus(Maquina.StatusMaquina.PARADA);
            if (containsAny(msg, "operando", "ligada", "ativas"))
                return machinesByStatus(Maquina.StatusMaquina.OPERANDO);
            if (containsAny(msg, "manutenção", "manutencao"))
                return machinesByStatus(Maquina.StatusMaquina.MANUTENCAO);
            return allMachinesStatus();
        }

        if (containsAny(msg, "mtbf", "tempo médio entre falhas", "tempo medio entre falhas", "confiabilidade"))
            return mtbfInfo();

        if (containsAny(msg, "ranking", "melhor", "top", "classificação", "classificacao", "pódio", "podio"))
            return ranking();

        if (containsAny(msg, "tempo médio", "tempo medio", "mttr", "reparo médio", "reparo medio"))
            return averageRepairTime();

        if (containsAny(msg, "quem sou", "meu perfil", "minha conta", "meu login"))
            return whoami(username);

        if (containsAny(msg, "meus chamados", "minhas tarefas", "meus tickets"))
            return myTickets(username);

        if (containsAny(msg, "dashboard", "painel", "principal", "início", "inicio"))
            return navigateLink("Dashboard", "/dashboard");

        if (containsAny(msg, "abrir chamado", "novo chamado", "criar chamado", "reportar falha", "reportar problema"))
            return navigateLink("Abrir Chamado", "/chamados/novo");

        if (containsAny(msg, "listar chamados", "todos chamados", "lista de chamados", "ver chamados"))
            return navigateLink("Lista de Chamados", "/chamados");

        if (containsAny(msg, "relatório", "relatorio", "excel", "exportar"))
            return navigateLink("Relatórios", "/relatorios");

        return fallback();
    }

    private Map<String, Object> greeting(String username) {
        String nome = resolveUserName(username);
        String[] saudacoes = {
            "Olá, " + nome + "! Sou o assistente virtual do BAT Manutenção. Pergunte sobre chamados, máquinas, MTBF, ranking ou digite **ajuda** para ver o que posso fazer.",
            "Oi, " + nome + "! Estou aqui para ajudar. Pergunte sobre o status dos chamados, máquinas ou qualquer indicador do sistema.",
            "Bem-vindo, " + nome + "! Em que posso ajudar? Posso consultar chamados, máquinas, MTBF e mais."
        };
        int idx = (int) (Math.random() * saudacoes.length);
        return Map.of("text", saudacoes[idx]);
    }

    private Map<String, Object> help() {
        String sb = "**Comandos disponíveis:**\n\n" +
            "**Chamados:**\n" +
            "- \"chamados abertos\" — lista os abertos\n" +
            "- \"chamados em andamento\" — em atendimento\n" +
            "- \"chamados pausados\" / \"escalados\" / \"concluídos\"\n" +
            "- \"resumo\" — visão geral de todos os status\n\n" +
            "**Máquinas:**\n" +
            "- \"máquinas\" — status de todas\n" +
            "- \"máquinas paradas\" — apenas as paradas\n\n" +
            "**Indicadores:**\n" +
            "- \"mtbf\" — tempo médio entre falhas por setor\n" +
            "- \"ranking\" — melhores técnicos\n" +
            "- \"tempo médio de reparo\" — MTTR\n\n" +
            "**Navegação:**\n" +
            "- \"dashboard\", \"abrir chamado\", \"relatórios\"\n\n" +
            "**Pessoal:**\n" +
            "- \"meus chamados\", \"quem sou eu\"";
        return Map.of("text", sb);
    }

    private Map<String, Object> ticketsSummary() {
        long abertos = chamadoRepository.countByStatus(Chamado.Status.ABERTO);
        long andamento = chamadoRepository.countByStatus(Chamado.Status.EM_ANDAMENTO);
        long pausados = chamadoRepository.countByStatus(Chamado.Status.PAUSADO);
        long escalados = chamadoRepository.countByStatus(Chamado.Status.ESCALADO);
        long concluidos = chamadoRepository.countByStatus(Chamado.Status.CONCLUIDO);

        String sb = "**Resumo dos Chamados:**\n\n" +
            "Abertos: **" + abertos + "**\n" +
            "Em andamento: **" + andamento + "**\n" +
            "Pausados: **" + pausados + "**\n" +
            "Escalados: **" + escalados + "**\n" +
            "Concluídos: **" + concluidos + "**\n\n" +
            "Total ativo: **" + (abertos + andamento + pausados + escalados) + "**";

        return Map.of("text", sb, "data", Map.of(
            "abertos", abertos,
            "emAndamento", andamento,
            "pausados", pausados,
            "escalados", escalados,
            "concluidos", concluidos
        ));
    }

    private Map<String, Object> ticketsByStatus(Chamado.Status status, String label) {
        List<Chamado> chamados = chamadoRepository.findByStatus(status);
        long count = chamadoRepository.countByStatus(status);

        if (chamados.isEmpty()) {
            return Map.of("text", "Não há chamados **" + label.toLowerCase() + "** no momento.");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("**").append(label).append(" (").append(count).append("):**\n\n");
        int max = Math.min(chamados.size(), 5);
        for (int i = 0; i < max; i++) {
            Chamado c = chamados.get(i);
            String maq = c.getMaquina() != null ? c.getMaquina().getNome() : "N/D";
            sb.append("**#").append(c.getId()).append("** ").append(c.getTitulo());
            sb.append(" — Máquina: ").append(maq).append("\n");
        }
        if (chamados.size() > 5) {
            sb.append("\n... e mais **").append(chamados.size() - 5).append("** chamados.");
        }

        return Map.of("text", sb.toString());
    }

    private Map<String, Object> allMachinesStatus() {
        List<Maquina> maquinas = maquinaRepository.findAll();
        long operando = maquinas.stream().filter(m -> m.getStatus() == Maquina.StatusMaquina.OPERANDO).count();
        long paradas = maquinas.stream().filter(m -> m.getStatus() == Maquina.StatusMaquina.PARADA).count();
        long manutencao = maquinas.stream().filter(m -> m.getStatus() == Maquina.StatusMaquina.MANUTENCAO).count();

        StringBuilder sb = new StringBuilder();
        sb.append("**Status das Máquinas:**\n\n");
        sb.append("Operando: **").append(operando).append("**\n");
        sb.append("Paradas: **").append(paradas).append("**\n");
        sb.append("Em manutenção: **").append(manutencao).append("**\n\n");

        for (Maquina m : maquinas) {
            String statusIcon = m.getStatus() == Maquina.StatusMaquina.OPERANDO ? "🟢" :
                    m.getStatus() == Maquina.StatusMaquina.PARADA ? "🔴" : "🟡";
            sb.append(statusIcon).append(" **").append(m.getNome()).append("**");
            if (m.getSetor() != null) {
                sb.append(" (").append(m.getSetor().getNome()).append(")");
            }
            sb.append(" — ").append(traduzirStatus(m.getStatus())).append("\n");
        }

        return Map.of("text", sb.toString());
    }

    private Map<String, Object> machinesByStatus(Maquina.StatusMaquina status) {
        List<Maquina> maquinas = maquinaRepository.findAll().stream()
                .filter(m -> m.getStatus() == status)
                .collect(Collectors.toList());

        if (maquinas.isEmpty()) {
            return Map.of("text", "Nenhuma máquina está **" + traduzirStatus(status).toLowerCase() + "** no momento.");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("**Máquinas ").append(traduzirStatus(status)).append(" (").append(maquinas.size()).append("):**\n\n");
        for (Maquina m : maquinas) {
            sb.append("- **").append(m.getNome()).append("**");
            if (m.getSetor() != null) {
                sb.append(" (").append(m.getSetor().getNome()).append(")");
            }
            sb.append("\n");
        }
        return Map.of("text", sb.toString());
    }

    private Map<String, Object> mtbfInfo() {
        List<Setor> setores = setorRepository.findAll();
        StringBuilder sb = new StringBuilder();
        sb.append("**MTBF por Setor (últimos 30 dias):**\n\n");

        for (Setor s : setores) {
            double mtbf = chamadoService.calcularMtbfPorSetor(s.getId());
            sb.append("**").append(s.getNome()).append("**: ");
            if (mtbf > 0) {
                sb.append(String.format("%.1f horas", mtbf));
            } else {
                sb.append("dados insuficientes");
            }
            sb.append("\n");
        }

        return Map.of("text", sb.toString());
    }

    private Map<String, Object> ranking() {
        List<RankingTecnico> ranking = chamadoService.getRankingTecnicos();

        if (ranking.isEmpty()) {
            return Map.of("text", "Não há dados suficientes para o ranking de técnicos ainda.");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("**Ranking de Técnicos:**\n\n");

        String[] medals = {"🥇", "🥈", "🥉"};
        int max = Math.min(ranking.size(), 5);
        for (int i = 0; i < max; i++) {
            RankingTecnico r = ranking.get(i);
            String prefix = i < 3 ? medals[i] + " " : (i + 1) + ". ";
            double tempoMin = r.getTempoMedioSegundos() != null ? r.getTempoMedioSegundos() / 60.0 : 0;
            sb.append(prefix).append("**").append(r.getNome()).append("**");
            sb.append(" — ").append(r.getTotal()).append(" chamados");
            if (tempoMin > 0) {
                sb.append(String.format(", média %.0f min", tempoMin));
            }
            sb.append("\n");
        }

        return Map.of("text", sb.toString());
    }

    private Map<String, Object> averageRepairTime() {
        double geral = chamadoService.getTempoMedioReparoGeral();
        StringBuilder sb = new StringBuilder();
        sb.append("**Tempo Médio de Reparo (MTTR):**\n\n");
        sb.append(String.format("Geral: **%.1f minutos**\n\n", geral));

        List<Setor> setores = setorRepository.findAll();
        sb.append("Por setor:\n");
        for (Setor s : setores) {
            double porSetor = chamadoService.getTempoMedioReparoPorSetor(s.getId());
            sb.append("- **").append(s.getNome()).append("**: ");
            sb.append(String.format("%.1f min\n", porSetor));
        }

        return Map.of("text", sb.toString());
    }

    private Map<String, Object> whoami(String username) {
        Optional<Usuario> opt = usuarioRepository.findByLogin(username);
        if (opt.isEmpty()) {
            return Map.of("text", "Você está logado como **" + username + "**.");
        }
        Usuario u = opt.get();
        String sb = "**Seu Perfil:**\n\n" +
            "Nome: **" + u.getNome() + "**\n" +
            "Login: **" + u.getLogin() + "**\n" +
            "Cargo: **" + traduzirTipo(u.getTipo()) + "**\n";
        if (u.getSetor() != null) {
            sb += "Setor: **" + u.getSetor().getNome() + "**\n";
        }
        return Map.of("text", sb);
    }

    private Map<String, Object> myTickets(String username) {
        Optional<Usuario> opt = usuarioRepository.findByLogin(username);
        if (opt.isEmpty()) {
            return Map.of("text", "Não foi possível encontrar seus dados.");
        }
        Usuario u = opt.get();

        List<Chamado.Status> ativos = List.of(
                Chamado.Status.ABERTO, Chamado.Status.EM_ANDAMENTO,
                Chamado.Status.PAUSADO, Chamado.Status.ESCALADO);
        List<Chamado> meusChamados = chamadoRepository.findByTecnicoIdAndStatusIn(u.getId(), ativos);

        if (meusChamados.isEmpty()) {
            return Map.of("text", "Você não tem chamados ativos no momento, **" + u.getNome() + "**.");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("**Seus chamados ativos (").append(meusChamados.size()).append("):**\n\n");
        for (Chamado c : meusChamados) {
            sb.append("**#").append(c.getId()).append("** ");
            sb.append(c.getTitulo()).append(" — ");
            sb.append(traduzirStatus(c.getStatus())).append("\n");
        }
        return Map.of("text", sb.toString());
    }

    private Map<String, Object> navigateLink(String label, String url) {
        return Map.of("text", "Clique para acessar: **" + label + "**",
                "link", url, "linkLabel", label);
    }

    private Map<String, Object> fallback() {
        String[] respostas = {
            "Desculpe, não entendi. Tente perguntar sobre **chamados**, **máquinas**, **MTBF** ou digite **ajuda**.",
            "Não consegui interpretar sua mensagem. Que tal perguntar \"quantos chamados abertos?\" ou digitar **ajuda**?",
            "Hmm, não reconheci esse comando. Digite **ajuda** para ver o que posso fazer."
        };
        int idx = (int) (Math.random() * respostas.length);
        return Map.of("text", respostas[idx]);
    }

    private boolean matchesAny(String msg, String... palavras) {
        return Arrays.stream(palavras).anyMatch(p -> msg.equals(p) || msg.startsWith(p + " ") || msg.endsWith(" " + p) || msg.contains(" " + p + " "));
    }

    private boolean containsAny(String msg, String... frases) {
        return Arrays.stream(frases).anyMatch(msg::contains);
    }

    private String resolveUserName(String username) {
        return usuarioRepository.findByLogin(username)
                .map(Usuario::getNome)
                .orElse(username);
    }

    private String traduzirStatus(Chamado.Status status) {
        return switch (status) {
            case ABERTO -> "Aberto";
            case EM_ANDAMENTO -> "Em andamento";
            case PAUSADO -> "Pausado";
            case ESCALADO -> "Escalado";
            case CONCLUIDO -> "Concluído";
        };
    }

    private String traduzirStatus(Maquina.StatusMaquina status) {
        return switch (status) {
            case OPERANDO -> "Operando";
            case PARADA -> "Parada";
            case MANUTENCAO -> "Manutenção";
        };
    }

    private String traduzirTipo(Usuario.Tipo tipo) {
        return switch (tipo) {
            case OPERADOR -> "Operador";
            case TECNICO -> "Técnico";
            case LIDER -> "Líder";
            case ESPECIALISTA -> "Especialista";
            case VISUALIZADOR -> "Visualizador";
        };
    }
}
