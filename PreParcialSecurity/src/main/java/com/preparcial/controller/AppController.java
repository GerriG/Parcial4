package com.preparcial.controller;

import com.preparcial.model.Perfil;
import com.preparcial.model.Rol;
import com.preparcial.model.Usuario;
import com.preparcial.repository.UsuarioRepository;
import com.preparcial.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
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

    // --- MÉTODOS DE PERFIL MODIFICADOS ---

    // 1. VISTA DE LECTURA (Muestra perfil.html)
    @GetMapping("/perfil")
    public String verPerfil(Authentication auth, Model model) {
        String username = auth.getName();
        Usuario usuario = usuarioRepository.findByUsername(username).orElseThrow();
        
        model.addAttribute("usuario", usuario);
        // Pasamos "perfil" también por comodidad en la vista
        model.addAttribute("perfil", usuario.getPerfil());
        
        return "perfil"; // Retorna la vista de solo lectura
    }

    // 2. VISTA DE EDICIÓN (Muestra edit_perfil.html - antiguo perfil.html renombrado)
    @GetMapping("/perfil/editar")
    public String editarPerfil(Authentication auth, Model model) {
        String username = auth.getName();
        Usuario usuario = usuarioRepository.findByUsername(username).orElseThrow();
        
        model.addAttribute("usuario", usuario);
        model.addAttribute("perfil", usuario.getPerfil());
        
        return "edit_perfil"; // Retorna el formulario de edición
    }

    // 3. GUARDAR CAMBIOS (Redirige a lectura si sale bien, o vuelve a editar si falla)
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
            return "redirect:/perfil"; // Éxito: vamos a la vista de lectura

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al actualizar.");
            return "redirect:/perfil/editar"; // Error: volvemos al formulario
        }
    }

    // --- ADMINISTRACIÓN DE USUARIOS ---
    @GetMapping("/admin/usuarios")
    public String administrarUsuarios(Model model) {
        model.addAttribute("listaUsuarios", usuarioRepository.findAll());
        return "admin_usuarios";
    }

    @GetMapping("/admin/eliminar/{id}")
    public String eliminarUsuario(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            // 1. Verificamos si el usuario existe antes de intentar borrarlo
            if (!usuarioRepository.existsById(id)) {
                redirectAttributes.addFlashAttribute("error", "El usuario con ID " + id + " no existe.");
                return "redirect:/admin/usuarios";
            }

            // 2. Intentamos eliminar (Gracias al ON DELETE CASCADE de la BD, el perfil se irá solo)
            usuarioRepository.deleteById(id);

            redirectAttributes.addFlashAttribute("success", "Usuario y sus datos asociados eliminados correctamente.");

        } catch (Exception e) {
            // 3. IMPORTANTE: Imprimir el error real en la consola del servidor para que tú lo veas
            System.err.println("ERROR AL ELIMINAR USUARIO: " + e.getMessage());
            e.printStackTrace();

            // 4. Mostrar un mensaje más descriptivo al usuario (opcional, o dejar el genérico)
            redirectAttributes.addFlashAttribute("error", "Error crítico al eliminar: " + e.getMessage());
        }

        return "redirect:/admin/usuarios";
    }
}
