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

import java.time.LocalDate;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Directorio físico donde se guardan las imágenes
    private final String UPLOAD_DIR = "src/main/resources/static/Profiles/";

    public void registrarUsuario(Usuario usuario, Perfil perfil) {
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        usuario.setEstado(true);
        
        if (usuario.getRol() == null) {
            usuario.setRol(Rol.USUARIO); 
        }

        usuario.setPerfil(perfil);
        usuarioRepository.save(usuario);
    }

    public void actualizarPerfil(Usuario usuarioSesion, Perfil datosFormulario, MultipartFile archivoAvatar, String nuevoPassword) throws IOException {
        // 1. Recuperar datos actuales de la BD
        Usuario usuarioBD = usuarioRepository.findById(usuarioSesion.getIdUsuario()).orElseThrow();
        Perfil perfilBD = usuarioBD.getPerfil();

        // 2. Actualizar campos SOLO si no vienen nulos o vacíos
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

        // 3. Actualizar contraseña
        if (nuevoPassword != null && !nuevoPassword.isEmpty()) {
            usuarioBD.setPassword(passwordEncoder.encode(nuevoPassword));
        }

        // 4. Manejo de Imagen (AQUÍ ESTÁ LA LÓGICA DE BORRADO)
        if (!archivoAvatar.isEmpty()) {
            
            // --- NUEVO BLOQUE: BORRAR IMAGEN ANTERIOR ---
            String avatarAnteriorUrl = perfilBD.getAvatar();
            
            // Solo intentamos borrar si hay algo guardado y no es nulo
            if (avatarAnteriorUrl != null && !avatarAnteriorUrl.isEmpty()) {
                try {
                    // La BD guarda la ruta web: "/uploads/nombre_archivo.jpg"
                    // Necesitamos quitar "/uploads/" para obtener solo "nombre_archivo.jpg"
                    String nombreArchivoAnterior = avatarAnteriorUrl.replace("/uploads/", "");
                    
                    // Construimos la ruta física completa
                    Path rutaArchivoAnterior = Paths.get(UPLOAD_DIR).resolve(nombreArchivoAnterior);
                    
                    // Borramos el archivo si existe en la carpeta
                    Files.deleteIfExists(rutaArchivoAnterior);
                    
                } catch (IOException e) {
                    // Imprimimos el error en consola para saber si falló, pero dejamos que el flujo continúe
                    System.err.println("No se pudo borrar la imagen anterior: " + e.getMessage());
                }
            }
            // --------------------------------------------

            // Lógica de guardado de la NUEVA imagen
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
    // Restablecer contraseña
    public boolean restablecerContrasena(String email, LocalDate fechaNacimiento, String nuevaPassword) {
        // 1. Buscar usuario que coincida con el email y la fecha
        var usuarioOpt = usuarioRepository.findByEmailAndFechaNacimiento(email, fechaNacimiento);

        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            // 2. Encriptar y guardar la nueva contraseña
            usuario.setPassword(passwordEncoder.encode(nuevaPassword));
            usuarioRepository.save(usuario);
            return true;
        }
        return false;
    }
}