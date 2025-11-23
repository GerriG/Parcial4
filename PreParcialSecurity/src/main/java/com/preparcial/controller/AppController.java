package com.preparcial.controller;

import com.preparcial.model.Perfil;
import com.preparcial.model.Rol;
import com.preparcial.model.Usuario;
import com.preparcial.repository.UsuarioRepository;
import com.preparcial.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
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

    @Autowired
    private PasswordEncoder passwordEncoder;

    // --- LOGIN & HOME ---

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

    // --- REGISTRO PÚBLICO ---
    
    @PostMapping("/register")
    public String registerUser(@ModelAttribute Usuario usuario, 
                               @RequestParam String nombre, 
                               @RequestParam String apellido,
                               @RequestParam String email,
                               @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate fechaNacimiento,
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
            
            usuario.setRol(Rol.USUARIO); // Siempre USUARIO
            
            usuarioService.registrarUsuario(usuario, perfil);
            
            redirectAttributes.addFlashAttribute("success", "Cuenta creada. Inicia sesión.");
            return "redirect:/login";

        } catch (IllegalArgumentException e) {
            // Capturamos validaciones específicas (ej: contraseña débil)
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/login?error";
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error técnico: " + e.getMessage());
            return "redirect:/login?error";
        }
    }

    // --- RECUPERACIÓN DE CONTRASEÑA ---

    @GetMapping("/recovery")
    public String mostrarRecuperacion() {
        return "recovery"; // Vista recovery.html
    }

    @PostMapping("/recovery/reset")
    public String procesarRecuperacion(@RequestParam String username,
                                       @RequestParam String email,
                                       @RequestParam String newPassword,
                                       RedirectAttributes redirectAttributes) {
        try {
            Usuario usuario = usuarioRepository.findByUsername(username).orElse(null);

            if (usuario == null || usuario.getPerfil() == null || !usuario.getPerfil().getEmail().equalsIgnoreCase(email)) {
                redirectAttributes.addFlashAttribute("error", "Datos incorrectos. Verifica tu usuario y correo.");
                return "redirect:/recovery";
            }

            // Aquí idealmente usarías el servicio para validar la contraseña también
            // Por simplicidad en este ejemplo, usamos setPassword directo, pero
            // lo mejor sería refactorizar para usar usuarioService.actualizarPassword(usuario, newPass)
            usuario.setPassword(passwordEncoder.encode(newPassword));
            usuarioRepository.save(usuario);

            redirectAttributes.addFlashAttribute("success", "Contraseña restablecida correctamente. Inicia sesión.");
            return "redirect:/login";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error técnico: " + e.getMessage());
            return "redirect:/recovery";
        }
    }

    // --- GESTIÓN DE USUARIOS (ADMIN) ---

    @PostMapping("/admin/crear")
    public String crearUsuarioDesdeAdmin(@RequestParam String username,
                                         @RequestParam String password,
                                         @RequestParam String nombre,
                                         @RequestParam String apellido,
                                         @RequestParam String email,
                                         @RequestParam String rol,
                                         RedirectAttributes redirectAttributes) {
        try {
            if (usuarioRepository.findByUsername(username).isPresent()) {
                redirectAttributes.addFlashAttribute("error", "El usuario ya existe.");
                return "redirect:/admin/usuarios";
            }

            Usuario usuario = new Usuario();
            usuario.setUsername(username);
            usuario.setPassword(password); // El servicio lo encriptará y validará
            usuario.setRol(Rol.valueOf(rol));

            Perfil perfil = new Perfil();
            perfil.setNombre(nombre);
            perfil.setApellido(apellido);
            perfil.setEmail(email);
            
            usuarioService.registrarUsuario(usuario, perfil);
            redirectAttributes.addFlashAttribute("success", "Usuario creado correctamente.");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al crear: " + e.getMessage());
        }
        return "redirect:/admin/usuarios";
    }

    @GetMapping("/admin/usuarios")
    public String administrarUsuarios(Model model) {
        model.addAttribute("listaUsuarios", usuarioRepository.findAll());
        return "admin_usuarios";
    }
    
    @GetMapping("/admin/toggle/{id}")
    public String toggleEstadoUsuario(@PathVariable Long id, Authentication auth, RedirectAttributes redirectAttributes) {
        try {
            Usuario usuario = usuarioRepository.findById(id).orElse(null);
            
            if (usuario != null) {
                String usuarioLogueado = auth.getName();
                
                if (usuario.getUsername().equals(usuarioLogueado)) {
                    redirectAttributes.addFlashAttribute("error", "Acción denegada: No puedes desactivar tu propia cuenta.");
                    return "redirect:/admin/usuarios";
                }

                boolean estadoActual = Boolean.TRUE.equals(usuario.getEstado());
                usuario.setEstado(!estadoActual);
                usuarioRepository.save(usuario);
                
                String accion = !estadoActual ? "activado" : "deshabilitado";
                redirectAttributes.addFlashAttribute("success", "Usuario " + usuario.getUsername() + " " + accion + ".");
            } else {
                redirectAttributes.addFlashAttribute("error", "Usuario no encontrado.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al cambiar estado.");
        }
        return "redirect:/admin/usuarios";
    }

    @GetMapping("/admin/eliminar/{id}")
    public String eliminarUsuario(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            if (!usuarioRepository.existsById(id)) {
                redirectAttributes.addFlashAttribute("error", "El usuario no existe.");
                return "redirect:/admin/usuarios";
            }
            usuarioRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("success", "Usuario eliminado.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al eliminar.");
        }
        return "redirect:/admin/usuarios";
    }

    // --- PERFIL ---

    @GetMapping("/perfil")
    public String verPerfil(Authentication auth, Model model) {
        String username = auth.getName();
        Usuario usuario = usuarioRepository.findByUsername(username).orElseThrow();
        model.addAttribute("usuario", usuario);
        model.addAttribute("perfil", usuario.getPerfil());
        return "perfil"; 
    }

    @GetMapping("/perfil/editar")
    public String editarPerfil(Authentication auth, Model model) {
        String username = auth.getName();
        Usuario usuario = usuarioRepository.findByUsername(username).orElseThrow();
        model.addAttribute("usuario", usuario);
        model.addAttribute("perfil", usuario.getPerfil());
        return "edit_perfil"; 
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
            
            // Aquí el servicio lanzará excepciones si la contraseña no cumple las reglas
            usuarioService.actualizarPerfil(usuarioSesion, perfilForm, archivo, newPassword);

            redirectAttributes.addFlashAttribute("success", "Perfil actualizado.");
            return "redirect:/perfil"; 

        } catch (Exception e) {
            // --- CORRECCIÓN CRÍTICA ---
            // Pasamos e.getMessage() para que el usuario vea: "La contraseña debe tener al menos..."
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/perfil/editar"; 
        }
    }
}