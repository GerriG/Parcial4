SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;

-- ZONA HORARIA DE EL SALVADOR (UTC-6)
SET time_zone = "-06:00";

-- 1. Configuración de la Base de Datos
CREATE DATABASE IF NOT EXISTS `preparcial_security_db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `preparcial_security_db`;

-- 2. Limpieza previa
-- Mantenemos esto para asegurar que si existe el trigger viejo, se elimine.
DROP TRIGGER IF EXISTS `borrar_usuario_al_borrar_perfil`;
DROP TABLE IF EXISTS `perfiles`;
DROP TABLE IF EXISTS `usuarios`;

-- 3. Crear tabla USUARIOS
CREATE TABLE `usuarios` (
  `id_usuario` bigint(20) NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL,
  `password` varchar(255) NOT NULL,
  `rol` varchar(20) NOT NULL,
  `estado` tinyint(1) DEFAULT 1,
  `created_at` datetime DEFAULT current_timestamp(),
  PRIMARY KEY (`id_usuario`),
  UNIQUE KEY `username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- 4. Crear tabla PERFILES
CREATE TABLE `perfiles` (
  `id_perfil` bigint(20) NOT NULL AUTO_INCREMENT,
  `nombre` varchar(100) NOT NULL,
  `apellido` varchar(100) NOT NULL,
  `email` varchar(150) DEFAULT NULL,
  `fecha_nacimiento` date DEFAULT NULL,
  `avatar` varchar(255) DEFAULT NULL,
  `usuario_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id_perfil`),
  UNIQUE KEY `usuario_id` (`usuario_id`),
  -- Esta línea es la que hace la magia: Si borras Usuario, se borra Perfil automáticamente.
  CONSTRAINT `fk_perfil_usuario` FOREIGN KEY (`usuario_id`) REFERENCES `usuarios` (`id_usuario`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- 5. Insertar datos en USUARIOS (Admins y usuarios base)
INSERT INTO `usuarios` (`id_usuario`, `username`, `password`, `rol`, `estado`, `created_at`) VALUES
(1, 'Admin', '$2a$10$00VDLk4S0MogOwyHWecWnONCmkK8IcUlIALzieImjxP6KJ6ksGRB.', 'ADMINISTRADOR', 1, NOW()),
(2, 'Usuario', '$2a$10$sFgww4eFUui2FVzshfQd5uL/JFM65OO91O1Ea8lynMa350GrO1n9a', 'USUARIO', 1, NOW());

-- 6. Insertar datos en PERFILES
INSERT INTO `perfiles` (`id_perfil`, `nombre`, `apellido`, `email`, `fecha_nacimiento`, `avatar`, `usuario_id`) VALUES
(1, 'Admin', 'Admin', 'admin@admin.com', '2005-04-17', '/uploads/5ba7a251-99eb-497c-bf95-42e1f976b401_133969316527262984.jpg', 1),
(2, 'Usuario', 'Usuario', 'Usuario@Usuario.com', '2012-12-12', '/uploads/6e43e288-6b77-48dc-a3bc-75eb8433ca58_133817347908392708_1366_768.jpg', 2);

COMMIT;