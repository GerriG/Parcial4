package com.preparcial.controller;

import com.preparcial.model.Perfil;
import com.preparcial.model.Rol;
import com.preparcial.model.Usuario;
import com.preparcial.repository.UsuarioRepository;
import com.preparcial.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat; // IMPORTANTE
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
public class AppController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private UsuarioService usuarioService;

    @GetMapping("/login")
    public String login() {
        return "login"; 
    }

    @GetMapping("/redirectByRole")
    public String defaultAfterLogin() {
        return "redirect:/home";
    }

    @GetMapping({"/", "/home"})
    public String home(Authentication auth, Model model) {
        String username = auth.getName();
        Usuario usuario = usuarioRepository.findByUsername(username).orElse(null);
        model.addAttribute("usuario", usuario);
        return "home";
    }

    // --- REGISTRO (CON FORMATO DE FECHA ASEGURADO) ---
    @PostMapping("/register")
    public String registerUser(@ModelAttribute Usuario usuario, 
                               @RequestParam String nombre, 
                               @RequestParam String apellido,
                               @RequestParam String email,
                               // Esta anotación asegura que la fecha se lea correctamente (yyyy-MM-dd)
                               @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate fechaNacimiento,
                               @RequestParam(required = false) String rolSeleccionado,
                               RedirectAttributes redirectAttributes) {
        try {
            if (usuarioRepository.findByUsername(usuario.getUsername()).isPresent()) {
                redirectAttributes.addFlashAttribute("error", "El usuario ya existe.");
                return "redirect:/login?error";
            }

            Perfil perfil = new Perfil();
            perfil.setNombre(nombre);
            perfil.setApellido(apellido);
            perfil.setEmail(email);
            perfil.setFechaNacimiento(fechaNacimiento);
            
            if(rolSeleccionado != null && !rolSeleccionado.isEmpty()) {
                usuario.setRol(Rol.valueOf(rolSeleccionado));
            } else {
                usuario.setRol(Rol.USUARIO);
            }
            
            usuarioService.registrarUsuario(usuario, perfil);
            
            redirectAttributes.addFlashAttribute("success", "Cuenta creada. Inicia sesión.");
            return "redirect:/login";

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
            return "redirect:/login?error";
        }
    }

    @GetMapping("/perfil")
    public String editarPerfil(Authentication auth, Model model) {
        String username = auth.getName();
        Usuario usuario = usuarioRepository.findByUsername(username).orElseThrow();
        model.addAttribute("usuario", usuario);
        model.addAttribute("perfil", usuario.getPerfil());
        return "perfil";
    }

    @PostMapping("/perfil/guardar")
    public String guardarPerfil(Authentication auth, 
                                @ModelAttribute Perfil perfilForm,
                                @RequestParam("file") MultipartFile archivo,
                                @RequestParam(required = false) String newPassword,
                                RedirectAttributes redirectAttributes) {
        try {
            String username = auth.getName();
            Usuario usuarioSesion = usuarioRepository.findByUsername(username).orElseThrow();
            usuarioService.actualizarPerfil(usuarioSesion, perfilForm, archivo, newPassword);
            redirectAttributes.addFlashAttribute("success", "Perfil actualizado correctamente.");
            return "redirect:/perfil";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al actualizar.");
            return "redirect:/perfil";
        }
    }

    @GetMapping("/admin/usuarios")
    public String administrarUsuarios(Model model) {
        model.addAttribute("listaUsuarios", usuarioRepository.findAll());
        return "admin_usuarios";
    }

    @GetMapping("/admin/eliminar/{id}")
    public String eliminarUsuario(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            usuarioRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("success", "Usuario eliminado correctamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "No se pudo eliminar el usuario.");
        }
        return "redirect:/admin/usuarios";
    }
}