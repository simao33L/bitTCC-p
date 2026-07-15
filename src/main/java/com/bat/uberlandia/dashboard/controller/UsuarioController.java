package com.bat.uberlandia.dashboard.controller;

import com.bat.uberlandia.dashboard.model.Usuario;
import com.bat.uberlandia.dashboard.repository.SetorRepository;
import com.bat.uberlandia.dashboard.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioRepository usuarioRepository;
    private final SetorRepository setorRepository;

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("usuarios", usuarioRepository.findAll());
        model.addAttribute("setores", setorRepository.findAll());
        return "usuarios";
    }

    @PostMapping
    public String salvar(@RequestParam String login,
                         @RequestParam String senha,
                         @RequestParam String nome,
                         @RequestParam Usuario.Tipo tipo,
                         @RequestParam(required = false) Long setorId,
                         RedirectAttributes redirect) {
        if (usuarioRepository.findByLogin(login).isPresent()) {
            redirect.addFlashAttribute("erro", "Login já existe.");
            return "redirect:/usuarios";
        }
        Usuario.UsuarioBuilder builder = Usuario.builder()
                .login(login)
                .senha("{noop}" + senha)
                .nome(nome)
                .tipo(tipo);
        if (setorId != null) {
            setorRepository.findById(setorId).ifPresent(builder::setor);
        }
        usuarioRepository.save(builder.build());
        redirect.addFlashAttribute("sucesso", "Usuário cadastrado com sucesso.");
        return "redirect:/usuarios";
    }

    @PostMapping("/{id}/excluir")
    public String excluir(@PathVariable Long id, RedirectAttributes redirect) {
        usuarioRepository.deleteById(id);
        redirect.addFlashAttribute("sucesso", "Usuário removido.");
        return "redirect:/usuarios";
    }
}
