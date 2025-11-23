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
import java.util.UUID;
import java.util.regex.Pattern; // Necesario para regex

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final String UPLOAD_DIR = "src/main/resources/static/Profiles/";

    // Método helper para validar contraseña (NUEVO)
    private void validarPasswordSeguro(String password) {
        if (password.length() < 8) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres.");
        }
        // Regex: Al menos una mayúscula, al menos un número
        if (!Pattern.matches(".*[A-Z].*", password)) {
            throw new IllegalArgumentException("La contraseña debe contener al menos una letra mayúscula.");
        }
        if (!Pattern.matches(".*\\d.*", password)) {
            throw new IllegalArgumentException("La contraseña debe contener al menos un número.");
        }
    }

    public void registrarUsuario(Usuario usuario, Perfil perfil) {
        // Validamos también al registrar (Opcional, pero recomendado)
        validarPasswordSeguro(usuario.getPassword());

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
            perfilBD.setFechaNacimiento(datosFormulario.getFechaNacimiento());
        }

        // --- VALIDACIÓN DE CONTRASEÑA MEJORADA ---
        if (nuevoPassword != null && !nuevoPassword.isEmpty()) {
            // 1. Validamos reglas de seguridad
            validarPasswordSeguro(nuevoPassword);
            
            // 2. Si pasa, encriptamos y guardamos
            usuarioBD.setPassword(passwordEncoder.encode(nuevoPassword));
        }
        // ------------------------------------------

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