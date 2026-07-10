package com.bat.uberlandia.dashboard.controller;

import com.bat.uberlandia.dashboard.model.*;
import com.bat.uberlandia.dashboard.repository.TendenciaDiaria;
import com.bat.uberlandia.dashboard.repository.TendenciaMotivoDiaria;
import com.bat.uberlandia.dashboard.repository.*;
import com.bat.uberlandia.dashboard.service.AlertaService;
import com.bat.uberlandia.dashboard.service.ChamadoService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Controller
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final ChamadoRepository chamadoRepository;
    private final MaquinaRepository maquinaRepository;
    private final SetorRepository setorRepository;
    private final ChamadoService chamadoService;
    private final AlertaService alertaService;
    private final NotificacaoRepository notificacaoRepository;

    @GetMapping
    public String dashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataFim,
            @RequestParam(required = false) Long setorId,
            Model model) {

        Setor setorSelecionado = null;
        if (setorId != null) {
            setorSelecionado = setorRepository.findById(setorId).orElse(null);
        }

        List<Chamado> abertos = filtrarPorSetor(chamadoRepository.findByStatus(Chamado.Status.ABERTO), setorId);
        List<Chamado> emAndamento = filtrarPorSetor(chamadoRepository.findByStatus(Chamado.Status.EM_ANDAMENTO), setorId);
        List<Chamado> pausados = filtrarPorSetor(chamadoRepository.findByStatus(Chamado.Status.PAUSADO), setorId);
        List<Chamado> escalados = filtrarPorSetor(chamadoRepository.findByStatus(Chamado.Status.ESCALADO), setorId);

        List<Chamado> concluidos;
        if (dataInicio != null && dataFim != null) {
            concluidos = filtrarPorSetor(chamadoRepository.findByDataAberturaBetween(dataInicio, dataFim), setorId);
        } else {
            concluidos = filtrarPorSetor(chamadoRepository.findByStatus(Chamado.Status.CONCLUIDO), setorId);
        }

        model.addAttribute("abertos", abertos);
        model.addAttribute("emAndamento", emAndamento);
        model.addAttribute("pausados", pausados);
        model.addAttribute("escalados", escalados);
        model.addAttribute("concluidos", concluidos);

        double tempoMedio = (setorId != null)
                ? chamadoService.getTempoMedioReparoPorSetor(setorId)
                : chamadoService.getTempoMedioReparoGeral();
        model.addAttribute("tempoMedioReparo", tempoMedio);

        model.addAttribute("totalAlertas", alertaService.contarAlertasNaoLidos());
        model.addAttribute("ultimasNotificacoes", alertaService.listarUltimasNotificacoes());

        List<Setor> setores = setorRepository.findAll();
        model.addAttribute("setores", setores);
        model.addAttribute("setorSelecionadoId", setorId);
        model.addAttribute("setorSelecionado", setorSelecionado);

        Map<String, Double> mtbfPorSetor = new LinkedHashMap<>();
        for (Setor s : setores) {
            mtbfPorSetor.put(s.getNome(), chamadoService.calcularMtbfPorSetor(s.getId()));
        }
        model.addAttribute("mtbfPorSetor", mtbfPorSetor);

        List<Maquina> maquinas = (setorId != null)
                ? maquinaRepository.findBySetorId(setorId)
                : maquinaRepository.findAll();
        Map<String, Double> mtbfPorMaquina = new LinkedHashMap<>();
        for (Maquina m : maquinas) {
            mtbfPorMaquina.put(m.getNome(), chamadoService.calcularMtbfPorMaquina(m.getId()));
        }
        model.addAttribute("mtbfPorMaquina", mtbfPorMaquina);

        model.addAttribute("distribuicaoMotivos", chamadoService.getDistribuicaoMotivosFalha(setorId));

        long totalMotivos = chamadoService.getDistribuicaoMotivosFalha(setorId).values().stream()
                .mapToLong(Long::longValue).sum();
        model.addAttribute("totalChamadosComMotivo", totalMotivos);

        model.addAttribute("dataInicio", dataInicio);
        model.addAttribute("dataFim", dataFim);

        List<TendenciaDiaria> tendencia = chamadoRepository
                .findTendenciaDiaria(LocalDateTime.now().minusDays(90));
        Map<String, Long> tendenciaMap = new LinkedHashMap<>();
        for (TendenciaDiaria t : tendencia) {
            tendenciaMap.put(t.getDia().toString(), t.getTotal());
        }
        model.addAttribute("tendenciaDiaria", tendenciaMap);

        List<TendenciaMotivoDiaria> tendenciaMotivo = chamadoRepository
                .findTendenciaPorMotivo(LocalDateTime.now().minusDays(90));
        Map<String, Map<String, Long>> tendenciaMotivoMap = new LinkedHashMap<>();
        for (TendenciaMotivoDiaria t : tendenciaMotivo) {
            tendenciaMotivoMap
                    .computeIfAbsent(t.getMotivo().name(), k -> new LinkedHashMap<>())
                    .put(t.getDia().toString(), t.getTotal());
        }
        model.addAttribute("tendenciaMotivo", tendenciaMotivoMap);

        return "dashboard";
    }

    private List<Chamado> filtrarPorSetor(List<Chamado> chamados, Long setorId) {
        if (setorId == null) return chamados;
        List<Chamado> filtrados = new ArrayList<>();
        for (Chamado c : chamados) {
            if (c.getMaquina() != null && c.getMaquina().getSetor() != null
                    && setorId.equals(c.getMaquina().getSetor().getId())) {
                filtrados.add(c);
            }
        }
        return filtrados;
    }
}
