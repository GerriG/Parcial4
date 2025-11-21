package com.preparcial.service;

import com.preparcial.model.Usuario;
import com.preparcial.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. Buscar usuario en la Base de Datos
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));

        // 2. Convertir Rol (Enum -> Spring Authority)
        // Spring Security espera el prefijo "ROLE_" para las validaciones hasRole()
        String rolSpring = "ROLE_" + usuario.getRol().name();

        // 3. Retornar objeto User de Spring Security con la contraseña HASHED de la BD
        return User.builder()
                .username(usuario.getUsername())
                .password(usuario.getPassword()) 
                .authorities(rolSpring)
                .disabled(!usuario.getEstado()) // Bloquea si estado es false
                .build();
    }
}
