package com.proyecto.macrofit.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.proyecto.macrofit.usuarios.model.Usuario;
import com.proyecto.macrofit.usuarios.repository.UsuarioRepository;
import com.proyecto.macrofit.usuarios.service.UsuarioService;

/**
 * Crea las cuentas de demostración (un ADMIN y un USER) si no existen.
 * Las contraseñas se guardan con BCrypt, igual que cualquier usuario real.
 *
 * Se puede desactivar con app.demo.seed-users=false
 */
@Component
public class DemoUserSeeder implements CommandLineRunner {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private UsuarioRepository usuarioRepo;

    @Value("${app.demo.seed-users:true}")
    private boolean habilitado;

    @Value("${app.demo.admin-email}")
    private String adminEmail;

    @Value("${app.demo.admin-password}")
    private String adminPassword;

    @Value("${app.demo.user-email}")
    private String userEmail;

    @Value("${app.demo.user-password}")
    private String userPassword;

    @Override
    public void run(String... args) {
        if (!habilitado) {
            return;
        }

        // Objetivo: 1 = bajar, 2 = mantener, 3 = subir | Actividad: 1 = sedentario ... 5 = muy intenso
        crearSiNoExiste(adminEmail, adminPassword, "ADMIN", "Admin Demo", 30, 75f, 175, "Masculino", 2, 3);
        crearSiNoExiste(userEmail, userPassword, "USER", "Usuario Demo", 28, 68f, 168, "Femenino", 1, 2);
    }

    private void crearSiNoExiste(String correo, String contrasena, String rol, String nombre,
            int edad, float peso, int altura, String sexo, int idObjetivo, int idNvAct) {

        if (usuarioRepo.findByCorreo(correo).isPresent()) {
            return;
        }

        Usuario u = new Usuario();
        u.setNom_usuario(nombre);
        u.setCorreo(correo);
        u.setContrasena(contrasena);
        u.setEdad(edad);
        u.setPeso(peso);
        u.setAltura(altura);
        u.setSexo(sexo);
        u.setId_objetivo(idObjetivo);
        u.setId_nv_act(idNvAct);

        usuarioService.crearUsuarioConRol(u, rol);
        System.out.println("Usuario demo creado: " + correo + " (" + rol + ")");
    }
}
