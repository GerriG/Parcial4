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

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // CORRECCIÓN: Apuntar a 'Profiles' para coincidir con WebConfig
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

        // Actualizar fecha solo si el usuario seleccionó una nueva
        if (datosFormulario.getFechaNacimiento() != null) {
            perfilBD.setFechaNacimiento(datosFormulario.getFechaNacimiento());
        }

        // 3. Actualizar contraseña (solo si se escribió una nueva)
        if (nuevoPassword != null && !nuevoPassword.isEmpty()) {
            usuarioBD.setPassword(passwordEncoder.encode(nuevoPassword));
        }

        // 4. Manejo de Imagen
        if (!archivoAvatar.isEmpty()) {
            Path rutaDirectorio = Paths.get(UPLOAD_DIR);
            if (!Files.exists(rutaDirectorio)) {
                Files.createDirectories(rutaDirectorio);
            }

            String nombreArchivo = UUID.randomUUID().toString() + "_" + archivoAvatar.getOriginalFilename();
            Path rutaArchivo = rutaDirectorio.resolve(nombreArchivo);
            
            // Guardar físicamente en la carpeta 'Profiles'
            Files.write(rutaArchivo, archivoAvatar.getBytes());
            
            // Guardar la ruta URL en la BD (WebConfig mapea /uploads/** -> carpeta Profiles)
            perfilBD.setAvatar("/uploads/" + nombreArchivo);
        }

        usuarioBD.setPerfil(perfilBD);
        usuarioRepository.save(usuarioBD);
    }
}