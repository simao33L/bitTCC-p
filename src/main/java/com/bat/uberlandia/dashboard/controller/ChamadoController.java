package com.bat.uberlandia.dashboard.controller;

import com.bat.uberlandia.dashboard.model.*;
import com.bat.uberlandia.dashboard.repository.*;
import com.bat.uberlandia.dashboard.service.ChamadoService;
import com.bat.uberlandia.dashboard.service.RoteamentoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/chamados")
@RequiredArgsConstructor
public class ChamadoController {

    private final ChamadoRepository chamadoRepository;
    private final MaquinaRepository maquinaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ChamadoService chamadoService;
    private final RoteamentoService roteamentoService;
    private final NotificacaoRepository notificacaoRepository;

    @Value("${app.upload.dir}")
    private String uploadDir;

    @GetMapping("/novo")
    public String formNovoChamado(Model model) {
        model.addAttribute("chamado", new Chamado());
        model.addAttribute("maquinas", maquinaRepository.findAll());
        model.addAttribute("motivosFalha", Chamado.MotivoFalha.values());
        return "chamado-form";
    }

    @PostMapping("/novo")
    public String criarChamado(@Valid @ModelAttribute Chamado chamado,
                                BindingResult result,
                                @RequestParam("maquinaId") Long maquinaId,
                                @RequestParam(value = "motivoFalha", required = false) String motivoFalha,
                                @RequestParam(value = "foto", required = false) MultipartFile foto,
                                RedirectAttributes redirect,
                                Model model) throws IOException {

        if (result.hasErrors()) {
            model.addAttribute("maquinas", maquinaRepository.findAll());
            model.addAttribute("motivosFalha", Chamado.MotivoFalha.values());
            return "chamado-form";
        }

        Maquina maquina = maquinaRepository.findById(maquinaId)
                .orElseThrow(() -> new RuntimeException("Maquina nao encontrada"));

        if (motivoFalha != null && !motivoFalha.isEmpty()) {
            chamado.setMotivoFalha(Chamado.MotivoFalha.valueOf(motivoFalha));
        }

        if (foto != null && !foto.isEmpty()) {
            String nomeArquivo = UUID.randomUUID() + "_" + foto.getOriginalFilename();
            Path caminho = Paths.get(uploadDir, nomeArquivo);
            Files.createDirectories(caminho.getParent());
            Files.write(caminho, foto.getBytes());
            chamado.setCaminhoFoto(caminho.toString());
        }

        chamadoService.abrirChamado(chamado, maquina);
        redirect.addFlashAttribute("sucesso", "Chamado #" + chamado.getId() + " aberto!");
        return "redirect:/chamados";
    }

    @GetMapping
    public String listarChamados(@RequestParam(required = false) String status,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataInicio,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataFim,
                                  Model model) {
        List<Chamado> chamados;
        if (dataInicio != null && dataFim != null) {
            chamados = chamadoRepository.findByDataAberturaBetween(dataInicio, dataFim);
        } else if (status != null && !status.isEmpty()) {
            chamados = chamadoRepository.findByStatus(Chamado.Status.valueOf(status));
        } else {
            chamados = chamadoRepository.findAll();
        }
        model.addAttribute("chamados", chamados);
        model.addAttribute("statusFiltro", status);
        return "chamado-lista";
    }

    @GetMapping("/{id}")
    public String detalheChamado(@PathVariable Long id, Model model) {
        Chamado chamado = chamadoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Chamado nao encontrado"));
        model.addAttribute("chamado", chamado);
        model.addAttribute("tempoReparoAtual", chamado.getTempoReparoSegundos());
        model.addAttribute("tecnicos", usuarioRepository.findByTipo(Usuario.Tipo.TECNICO));
        model.addAttribute("especialistas", usuarioRepository.findByTipo(Usuario.Tipo.ESPECIALISTA));
        model.addAttribute("notificacoes", notificacaoRepository.findByChamadoIdOrderByDataEnvioDesc(id));
        model.addAttribute("atrasado", chamado.isAtrasado());

        if (chamado.getStatus() == Chamado.Status.ABERTO && chamado.getMaquina() != null) {
            RoteamentoService.SugestaoTecnico sugestao =
                    roteamentoService.sugerirTecnico(chamado.getMaquina(), chamado.getMotivoFalha());
            model.addAttribute("sugestaoTecnico", sugestao);
        }

        return "chamado-detalhe";
    }

    @PostMapping("/{id}/assumir")
    public String assumirChamado(@PathVariable Long id,
                                  @RequestParam Long tecnicoId,
                                  RedirectAttributes redirect) {
        Usuario tecnico = usuarioRepository.findById(tecnicoId)
                .orElseThrow(() -> new RuntimeException("Tecnico nao encontrado"));
        chamadoService.iniciarAtendimento(id, tecnico);
        redirect.addFlashAttribute("sucesso", "Chamado assumido. Reparo iniciado!");
        return "redirect:/chamados/" + id;
    }

    @PostMapping("/{id}/auto-assumir")
    public String autoAssumirChamado(@PathVariable Long id, RedirectAttributes redirect) {
        Chamado chamado = chamadoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Chamado nao encontrado"));
        if (chamado.getMaquina() == null) {
            redirect.addFlashAttribute("erro", "Maquina nao vinculada ao chamado.");
            return "redirect:/chamados/" + id;
        }
        RoteamentoService.SugestaoTecnico sugestao =
                roteamentoService.sugerirTecnico(chamado.getMaquina(), chamado.getMotivoFalha());
        if (sugestao == null) {
            redirect.addFlashAttribute("erro", "Nenhum tecnico disponivel para roteamento automatico.");
            return "redirect:/chamados/" + id;
        }
        Usuario tecnico = usuarioRepository.findById(sugestao.getTecnicoId())
                .orElseThrow(() -> new RuntimeException("Tecnico sugerido nao encontrado"));
        chamadoService.iniciarAtendimento(id, tecnico);
        redirect.addFlashAttribute("sucesso",
                "Chamado atribuido automaticamente a " + tecnico.getNome() + " (" + sugestao.getJustificativa() + ")");
        return "redirect:/chamados/" + id;
    }

    @PostMapping("/{id}/pausar")
    public String pausarChamado(@PathVariable Long id, RedirectAttributes redirect) {
        chamadoService.pausarReparo(id);
        redirect.addFlashAttribute("sucesso", "Cronometro pausado.");
        return "redirect:/chamados/" + id;
    }

    @PostMapping("/{id}/retomar")
    public String retomarChamado(@PathVariable Long id, RedirectAttributes redirect) {
        chamadoService.retomarReparo(id);
        redirect.addFlashAttribute("sucesso", "Reparo retomado.");
        return "redirect:/chamados/" + id;
    }

    @PostMapping("/{id}/escalar")
    public String escalarChamado(@PathVariable Long id,
                                  @RequestParam Long especialistaId,
                                  RedirectAttributes redirect) {
        Usuario especialista = usuarioRepository.findById(especialistaId)
                .orElseThrow(() -> new RuntimeException("Especialista nao encontrado"));
        chamadoService.escalarParaEspecialista(id, especialista);
        redirect.addFlashAttribute("sucesso", "Chamado escalado para especialista!");
        return "redirect:/chamados/" + id;
    }

    @PostMapping("/{id}/retomar-especialista")
    public String retomarEspecialista(@PathVariable Long id, RedirectAttributes redirect) {
        Chamado c = chamadoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Chamado nao encontrado"));
        c.setInicioReparo(LocalDateTime.now());
        c.setInicioPausa(null);
        c.setAlerta30MinEnviado(false);
        chamadoRepository.save(c);
        redirect.addFlashAttribute("sucesso", "Reparo retomado pelo especialista.");
        return "redirect:/chamados/" + id;
    }

    @PostMapping("/{id}/concluir")
    public String concluirChamado(@PathVariable Long id, RedirectAttributes redirect) {
        chamadoService.concluirChamado(id);
        redirect.addFlashAttribute("sucesso", "Chamado concluido!");
        return "redirect:/dashboard";
    }

    @PostMapping("/{id}/iniciar-locomocao")
    public String iniciarLocomocao(@PathVariable Long id, RedirectAttributes redirect) {
        chamadoService.iniciarLocomocao(id);
        redirect.addFlashAttribute("sucesso", "Locomocao iniciada.");
        return "redirect:/chamados/" + id;
    }

    @PostMapping("/{id}/finalizar-locomocao")
    public String finalizarLocomocao(@PathVariable Long id, RedirectAttributes redirect) {
        chamadoService.finalizarLocomocao(id);
        redirect.addFlashAttribute("sucesso", "Locomocao finalizada.");
        return "redirect:/chamados/" + id;
    }
}
