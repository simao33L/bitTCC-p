package com.bat.uberlandia.dashboard.controller;

import com.bat.uberlandia.dashboard.model.*;
import com.bat.uberlandia.dashboard.repository.*;
import com.bat.uberlandia.dashboard.service.ChamadoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

@Controller
@RequestMapping("/maquinas")
@RequiredArgsConstructor
public class MaquinaController {

    private final MaquinaRepository maquinaRepository;
    private final ChamadoRepository chamadoRepository;
    private final ChamadoService chamadoService;

    @GetMapping
    public String listarMaquinas(Model model) {
        List<Maquina> maquinas = maquinaRepository.findAll();

        List<Map<String, Object>> maquinaInfos = new ArrayList<>();
        for (Maquina m : maquinas) {
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("maquina", m);

            List<Chamado> chamadosAtivos = chamadoRepository.findByMaquinaIdAndStatusIn(m.getId(),
                    Arrays.asList(Chamado.Status.ABERTO, Chamado.Status.EM_ANDAMENTO,
                            Chamado.Status.PAUSADO, Chamado.Status.ESCALADO));
            info.put("chamadosAtivos", chamadosAtivos.size());

            Long tempoDowntime = chamadoRepository.sumTempoReparoByMaquinaId(m.getId());
            info.put("tempoDowntimeSegundos", tempoDowntime != null ? tempoDowntime : 0L);

            List<Chamado> historico = chamadoRepository.findByMaquinaIdOrderByDataAberturaDesc(m.getId());
            info.put("totalChamados", historico.size());

            double mtbf = chamadoService.calcularMtbfPorMaquina(m.getId());
            info.put("mtbfHoras", mtbf);

            maquinaInfos.add(info);
        }

        model.addAttribute("maquinaInfos", maquinaInfos);

        long operand = maquinas.stream().filter(m -> m.getStatus() == Maquina.StatusMaquina.OPERANDO).count();
        long paradas = maquinas.stream().filter(m -> m.getStatus() == Maquina.StatusMaquina.PARADA).count();
        long manutencao = maquinas.stream().filter(m -> m.getStatus() == Maquina.StatusMaquina.MANUTENCAO).count();
        model.addAttribute("totalOperando", operand);
        model.addAttribute("totalParadas", paradas);
        model.addAttribute("totalManutencao", manutencao);

        return "maquinas";
    }

    @GetMapping("/{id}")
    public String detalheMaquina(@PathVariable Long id, Model model) {
        Maquina maquina = maquinaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Maquina nao encontrada"));
        model.addAttribute("maquina", maquina);

        List<Chamado> historico = chamadoRepository.findByMaquinaIdOrderByDataAberturaDesc(id);
        model.addAttribute("historico", historico);

        Long tempoDowntime = chamadoRepository.sumTempoReparoByMaquinaId(id);
        model.addAttribute("tempoDowntimeSegundos", tempoDowntime != null ? tempoDowntime : 0L);

        double mtbf = chamadoService.calcularMtbfPorMaquina(id);
        model.addAttribute("mtbfHoras", mtbf);

        List<Chamado> chamadosAtivos = chamadoRepository.findByMaquinaIdAndStatusIn(id,
                Arrays.asList(Chamado.Status.ABERTO, Chamado.Status.EM_ANDAMENTO,
                        Chamado.Status.PAUSADO, Chamado.Status.ESCALADO));
        model.addAttribute("chamadosAtivos", chamadosAtivos);

        return "maquina-detalhe";
    }

    @PostMapping("/{id}/status")
    public String alterarStatus(@PathVariable Long id,
                                @RequestParam String status,
                                RedirectAttributes redirect) {
        Maquina maquina = maquinaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Maquina nao encontrada"));
        maquina.setStatus(Maquina.StatusMaquina.valueOf(status));
        maquinaRepository.save(maquina);
        redirect.addFlashAttribute("sucesso", "Status da maquina atualizado para " + status);
        return "redirect:/maquinas/" + id;
    }
}