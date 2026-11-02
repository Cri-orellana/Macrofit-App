package com.proyecto.macrofit.usuarios.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.proyecto.macrofit.usuarios.repository.UsuarioRepository;
import com.proyecto.macrofit.usuarios.model.Usuario;
import com.proyecto.macrofit.usuarios.model.Entity.UsuarioEntity;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository repositorioUsuario;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Listar todos los usuarios
    public List<Usuario> obtenerTodosLosUsuarios() {
        return repositorioUsuario.findAll().stream()
                .map(this::convertirAUsuario)
                .collect(Collectors.toList());
    }

    // Buscar por ID
    public Usuario obtenerUsuarioPorId(Integer id) {
        Optional<UsuarioEntity> entidad = repositorioUsuario.findById(id);
        return entidad.map(this::convertirAUsuario).orElse(null);
    }

    // Crear nuevo usuario
    public Usuario crearUsuario(Usuario usuario) {
        // VALIDACIÓN: Correo único
        if (repositorioUsuario.findByCorreo(usuario.getCorreo()).isPresent()) {
            throw new IllegalArgumentException("El correo ya se encuentra registrado.");
        }

        // VALIDACIÓN: Contraseña alfanumérica de mín 8 caracteres
        if (usuario.getContrasena() == null || !usuario.getContrasena().matches("^(?=.*[A-Za-z])(?=.*\\d).{8,}$")) {
            throw new IllegalArgumentException(
                    "La contraseña debe tener mínimo 8 caracteres, incluyendo solo letras y números.");
        }

        // El rol NUNCA se toma de la petición: un registro público siempre crea un usuario normal
        return guardarNuevo(usuario, "USER");
    }

    // Solo para uso interno (seeders de la demo). Ningún controlador expone este método.
    public Usuario crearUsuarioConRol(Usuario usuario, String rol) {
        if (repositorioUsuario.findByCorreo(usuario.getCorreo()).isPresent()) {
            throw new IllegalArgumentException("El correo ya se encuentra registrado.");
        }
        return guardarNuevo(usuario, rol);
    }

    private Usuario guardarNuevo(Usuario usuario, String rol) {
        usuario.setContrasena(passwordEncoder.encode(usuario.getContrasena()));
        usuario.setRol(rol);

        UsuarioEntity entidadNueva = convertirAEntidad(usuario);

        calcularRequerimientos(entidadNueva);

        UsuarioEntity entidadGuardada = repositorioUsuario.save(entidadNueva);
        return convertirAUsuario(entidadGuardada);
    }

    // Buscamos si existe alguien con este correo
    public Usuario loginUsuario(String correo, String contrasena) {
        Optional<UsuarioEntity> usuarioEncontrado = repositorioUsuario.findByCorreo(correo);

        if (usuarioEncontrado.isPresent() && contrasena != null) {
            // BCrypt compara la contraseña ingresada contra el hash guardado
            if (passwordEncoder.matches(contrasena, usuarioEncontrado.get().getContrasena())) {
                return convertirAUsuario(usuarioEncontrado.get());
            }
        }

        // Login fallido
        return null;
    }

    // Actualizar perfil parcial
    public Usuario actualizarPerfil(Integer id, Usuario datosActualizados) {
        return repositorioUsuario.findById(id).map(entidad -> {

            // Verificamos cada campo: si no es nulo en la petición, lo actualizamos
            if (datosActualizados.getNom_usuario() != null)
                entidad.setNom_usuario(datosActualizados.getNom_usuario());
            if (datosActualizados.getCorreo() != null)
                entidad.setCorreo(datosActualizados.getCorreo());
            if (datosActualizados.getEdad() != null)
                entidad.setEdad(datosActualizados.getEdad());
            if (datosActualizados.getPeso() > 0)
                entidad.setPeso(datosActualizados.getPeso());
            if (datosActualizados.getAltura() != null)
                entidad.setAltura(datosActualizados.getAltura());
            if (datosActualizados.getId_objetivo() != null)
                entidad.setId_objetivo(datosActualizados.getId_objetivo());
            if (datosActualizados.getId_nv_act() != null)
                entidad.setId_nv_act(datosActualizados.getId_nv_act());
            if (datosActualizados.getSexo() != null) {
                entidad.setSexo(datosActualizados.getSexo());
            }

            calcularRequerimientos(entidad);

            UsuarioEntity guardado = repositorioUsuario.save(entidad);
            return convertirAUsuario(guardado);
        }).orElse(null);
    }

    private void calcularRequerimientos(UsuarioEntity entidad) {
        if (entidad.getPeso() <= 0 || entidad.getAltura() == null || entidad.getSexo() == null)
            return;

        float tmbBase = (10f * entidad.getPeso()) + (6.25f * entidad.getAltura());
        String sexo = entidad.getSexo();

        if ("Masculino".equalsIgnoreCase(sexo) || "M".equalsIgnoreCase(sexo)) {
            if (entidad.getEdad() != null && entidad.getEdad() > 0) {
                tmbBase = tmbBase - (5f * entidad.getEdad()) + 5f;
            }
        } else if ("Femenino".equalsIgnoreCase(sexo) || "F".equalsIgnoreCase(sexo)) {
            if (entidad.getEdad() != null && entidad.getEdad() > 0) {
                tmbBase = tmbBase - (5f * entidad.getEdad()) - 161f;
            }
        } else {
            return;
        }

        entidad.setTmb_objetivo(tmbBase);

        float factor = 1.2f;
        if (entidad.getId_nv_act() != null) {
            switch (entidad.getId_nv_act()) {
                case 1:
                    factor = 1.2f;
                    break;
                case 2:
                    factor = 1.375f;
                    break;
                case 3:
                    factor = 1.55f;
                    break;
                case 4:
                    factor = 1.725f;
                    break;
                case 5:
                    factor = 1.9f;
                    break;
            }
        }

        float ajuste = 0f;
        if (entidad.getId_objetivo() != null) {
            switch (entidad.getId_objetivo()) {
                case 1:
                    ajuste = -500f;
                    break;
                case 2:
                    ajuste = 0f;
                    break;
                case 3:
                    ajuste = 500f;
                    break;
            }
        }

        entidad.setCal_diaria((tmbBase * factor) + ajuste);
    }

    // Modificar usuario existente
    public Usuario modificarUsuario(Integer id, Usuario usuarioActualizado) {
        return repositorioUsuario.findById(id).map(entidadExistente -> {
            entidadExistente.setNom_usuario(usuarioActualizado.getNom_usuario());
            entidadExistente.setCorreo(usuarioActualizado.getCorreo());
            // Solo se cambia la contraseña si viene una nueva (y siempre hasheada)
            if (usuarioActualizado.getContrasena() != null && !usuarioActualizado.getContrasena().isBlank()) {
                entidadExistente.setContrasena(passwordEncoder.encode(usuarioActualizado.getContrasena()));
            }
            entidadExistente.setEdad(usuarioActualizado.getEdad());
            entidadExistente.setPeso(usuarioActualizado.getPeso());
            entidadExistente.setAltura(usuarioActualizado.getAltura());
            entidadExistente.setSexo(usuarioActualizado.getSexo());
            entidadExistente.setId_objetivo(usuarioActualizado.getId_objetivo());
            entidadExistente.setId_nv_act(usuarioActualizado.getId_nv_act());
            entidadExistente.setTmb_objetivo(usuarioActualizado.getTmb_objetivo());
            entidadExistente.setCal_diaria(usuarioActualizado.getCal_diaria());

            UsuarioEntity guardado = repositorioUsuario.save(entidadExistente);
            return convertirAUsuario(guardado);
        }).orElse(null);
    }

    // Eliminar usuario por ID
    public boolean eliminarUsuario(Integer id) {
        if (repositorioUsuario.existsById(id)) {
            repositorioUsuario.deleteById(id);
            return true;
        }
        return false;
    }

    // Conversión entre Usuario y UsuarioEntity

    private Usuario convertirAUsuario(UsuarioEntity entidad) {
        if (entidad == null)
            return null;
        Usuario usuario = new Usuario();
        usuario.setId_usuario(entidad.getId_usuario());
        usuario.setNom_usuario(entidad.getNom_usuario());
        usuario.setCorreo(entidad.getCorreo());
        // El hash nunca se devuelve al cliente
        usuario.setContrasena(null);
        usuario.setRol(entidad.getRol());
        usuario.setEdad(entidad.getEdad());
        usuario.setAltura(entidad.getAltura());
        usuario.setSexo(entidad.getSexo());
        usuario.setPeso(entidad.getPeso());
        usuario.setId_objetivo(entidad.getId_objetivo());
        usuario.setId_nv_act(entidad.getId_nv_act());
        usuario.setTmb_objetivo(entidad.getTmb_objetivo());
        usuario.setCal_diaria(entidad.getCal_diaria());
        return usuario;
    }

    private UsuarioEntity convertirAEntidad(Usuario usuario) {
        if (usuario == null)
            return null;
        UsuarioEntity entidad = new UsuarioEntity();
        entidad.setId_usuario(usuario.getId_usuario());
        entidad.setNom_usuario(usuario.getNom_usuario());
        entidad.setCorreo(usuario.getCorreo());
        entidad.setContrasena(usuario.getContrasena());
        entidad.setRol(usuario.getRol());
        entidad.setEdad(usuario.getEdad());
        entidad.setAltura(usuario.getAltura());
        entidad.setSexo(usuario.getSexo());
        entidad.setPeso(usuario.getPeso());
        entidad.setId_objetivo(usuario.getId_objetivo());
        entidad.setId_nv_act(usuario.getId_nv_act());
        entidad.setTmb_objetivo(usuario.getTmb_objetivo());
        entidad.setCal_diaria(usuario.getCal_diaria());
        return entidad;
    }
}