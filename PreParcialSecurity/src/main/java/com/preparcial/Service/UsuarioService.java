package com.preparcial.service;

import com.preparcial.model.Perfil;
import com.preparcial.model.Rol;
import com.preparcial.model.Usuario;
import com.preparcial.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
// 1. AGREGAR ESTOS IMPORTS
import java.time.LocalDate;
import java.time.Period;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final String UPLOAD_DIR = "src/main/resources/static/Profiles/";

    public void validarPasswordSeguro(String password) {
        if (password.length() < 8) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres.");
        }
        if (!Pattern.matches(".*[A-Z].*", password)) {
            throw new IllegalArgumentException("La contraseña debe contener al menos una letra mayúscula.");
        }
        if (!Pattern.matches(".*\\d.*", password)) {
            throw new IllegalArgumentException("La contraseña debe contener al menos un número.");
        }
    }

    // 2. NUEVO MÉTODO DE VALIDACIÓN DE FECHA
    public void validarFechaNacimiento(LocalDate fechaNacimiento) {
        if (fechaNacimiento == null) {
            throw new IllegalArgumentException("La fecha de nacimiento es obligatoria.");
        }

        LocalDate fechaActual = LocalDate.now();

        // Validar que no sea fecha futura
        if (fechaNacimiento.isAfter(fechaActual)) {
            throw new IllegalArgumentException("La fecha de nacimiento no puede ser futura.");
        }

        // Validar edad mínima (12 años)
        int edad = Period.between(fechaNacimiento, fechaActual).getYears();
        if (edad < 12) {
            throw new IllegalArgumentException("Debes tener al menos 12 años para registrarte.");
        }
    }

    public void registrarUsuario(Usuario usuario, Perfil perfil) {
        // Validamos contraseña
        validarPasswordSeguro(usuario.getPassword());
        
        // 3. LLAMAR A LA VALIDACIÓN AQUÍ
        validarFechaNacimiento(perfil.getFechaNacimiento());

        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        usuario.setEstado(true);
        
        if (usuario.getRol() == null) {
            usuario.setRol(Rol.USUARIO); 
        }

        usuario.setPerfil(perfil);
        usuarioRepository.save(usuario);
    }

    public void actualizarPerfil(Usuario usuarioSesion, Perfil datosFormulario, MultipartFile archivoAvatar, String nuevoPassword) throws IOException {
        Usuario usuarioBD = usuarioRepository.findById(usuarioSesion.getIdUsuario()).orElseThrow();
        Perfil perfilBD = usuarioBD.getPerfil();

        if (datosFormulario.getNombre() != null && !datosFormulario.getNombre().isEmpty()) {
            perfilBD.setNombre(datosFormulario.getNombre());
        }
        
        if (datosFormulario.getApellido() != null && !datosFormulario.getApellido().isEmpty()) {
            perfilBD.setApellido(datosFormulario.getApellido());
        }
        
        if (datosFormulario.getEmail() != null && !datosFormulario.getEmail().isEmpty()) {
            perfilBD.setEmail(datosFormulario.getEmail());
        }

        if (datosFormulario.getFechaNacimiento() != null) {
            // Opcional: Validar también al editar perfil
            validarFechaNacimiento(datosFormulario.getFechaNacimiento());
            perfilBD.setFechaNacimiento(datosFormulario.getFechaNacimiento());
        }

        if (nuevoPassword != null && !nuevoPassword.isEmpty()) {
            validarPasswordSeguro(nuevoPassword);
            usuarioBD.setPassword(passwordEncoder.encode(nuevoPassword));
        }

        if (!archivoAvatar.isEmpty()) {
            Path rutaDirectorio = Paths.get(UPLOAD_DIR);
            if (!Files.exists(rutaDirectorio)) {
                Files.createDirectories(rutaDirectorio);
            }

            String nombreArchivo = UUID.randomUUID().toString() + "_" + archivoAvatar.getOriginalFilename();
            Path rutaArchivo = rutaDirectorio.resolve(nombreArchivo);
            
            Files.write(rutaArchivo, archivoAvatar.getBytes());
            perfilBD.setAvatar("/uploads/" + nombreArchivo);
        }

        usuarioBD.setPerfil(perfilBD);
        usuarioRepository.save(usuarioBD);
    }
}