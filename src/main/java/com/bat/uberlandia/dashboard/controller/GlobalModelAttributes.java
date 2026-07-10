package com.bat.uberlandia.dashboard.controller;

import com.bat.uberlandia.dashboard.model.Setor;
import com.bat.uberlandia.dashboard.repository.ChamadoRepository;
import com.bat.uberlandia.dashboard.repository.NotificacaoRepository;
import com.bat.uberlandia.dashboard.repository.SetorRepository;
import com.bat.uberlandia.dashboard.service.AlertaService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAttributes {

    private final SetorRepository setorRepository;
    private final AlertaService alertaService;
    private final NotificacaoRepository notificacaoRepository;
    private final ChamadoRepository chamadoRepository;

    @ModelAttribute("setores")
    public List<Setor> setores() {
        return setorRepository.findAll();
    }

    @ModelAttribute("totalAlertas")
    public long totalAlertas() {
        return alertaService.contarAlertasNaoLidos();
    }

    @ModelAttribute("ultimasNotificacoes")
    public List<?> ultimasNotificacoes() {
        return notificacaoRepository.findTop10ByOrderByDataEnvioDesc();
    }

    @ModelAttribute("totalAbertos")
    public long totalAbertos() {
        return chamadoRepository.findByStatus(
                com.bat.uberlandia.dashboard.model.Chamado.Status.ABERTO).size();
    }

    @ModelAttribute("usuarioLogadoNome")
    public String usuarioLogadoNome() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName().equals("anonymousUser")) {
            return "";
        }
        return auth.getName();
    }
}