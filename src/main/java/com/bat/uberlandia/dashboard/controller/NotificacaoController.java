package com.bat.uberlandia.dashboard.controller;

import com.bat.uberlandia.dashboard.repository.NotificacaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/notificacoes")
@RequiredArgsConstructor
public class NotificacaoController {

    private final NotificacaoRepository notificacaoRepository;

    @PostMapping("/marcar-todas-lidas")
    public String marcarTodasLidas(RedirectAttributes redirect) {
        int atualizadas = notificacaoRepository.marcarTodasComoLidas();
        redirect.addFlashAttribute("sucesso", atualizadas + " notificacoes marcadas como lidas.");
        return "redirect:/dashboard";
    }

    @PostMapping("/{id}/ler")
    public String marcarComoLida(@PathVariable Long id, RedirectAttributes redirect) {
        notificacaoRepository.marcarComoLida(id);
        redirect.addFlashAttribute("sucesso", "Notificacao marcada como lida.");
        return "redirect:/dashboard";
    }
}
