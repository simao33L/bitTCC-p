package com.bat.uberlandia.dashboard.controller;

import com.bat.uberlandia.dashboard.repository.RankingTecnico;
import com.bat.uberlandia.dashboard.service.ChamadoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/ranking")
@RequiredArgsConstructor
public class RankingController {

    private final ChamadoService chamadoService;

    @GetMapping
    public String ranking(Model model) {
        List<RankingTecnico> ranking = chamadoService.getRankingTecnicos();
        model.addAttribute("ranking", ranking);

        long maxTotal = ranking.stream().mapToLong(r -> r.getTotal()).max().orElse(1L);
        model.addAttribute("maxTotal", maxTotal);

        return "ranking";
    }
}
