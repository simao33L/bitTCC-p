package com.bat.uberlandia.dashboard.controller;

import com.bat.uberlandia.dashboard.model.Setor;
import com.bat.uberlandia.dashboard.repository.SetorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/setores")
@RequiredArgsConstructor
public class SetorController {

    private final SetorRepository setorRepository;

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("setores", setorRepository.findAll());
        return "setores";
    }

    @PostMapping
    public String salvar(@RequestParam String nome,
                         @RequestParam(required = false) String descricao,
                         RedirectAttributes redirect) {
        Setor setor = Setor.builder()
                .nome(nome)
                .descricao(descricao)
                .build();
        setorRepository.save(setor);
        redirect.addFlashAttribute("sucesso", "Setor cadastrado com sucesso.");
        return "redirect:/setores";
    }

    @PostMapping("/{id}/excluir")
    public String excluir(@PathVariable Long id, RedirectAttributes redirect) {
        setorRepository.deleteById(id);
        redirect.addFlashAttribute("sucesso", "Setor removido.");
        return "redirect:/setores";
    }
}
