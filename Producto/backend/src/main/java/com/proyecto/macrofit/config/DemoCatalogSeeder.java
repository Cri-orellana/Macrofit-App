package com.proyecto.macrofit.config;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.proyecto.macrofit.rutinaejercicio.model.Entity.EjercicioEntity;
import com.proyecto.macrofit.rutinaejercicio.model.Entity.RutinaEjercicioEntity;
import com.proyecto.macrofit.rutinaejercicio.model.Entity.RutinaEntity;
import com.proyecto.macrofit.rutinaejercicio.repository.EjercicioRepository;
import com.proyecto.macrofit.rutinaejercicio.repository.RutinaEjercicioRepository;
import com.proyecto.macrofit.rutinaejercicio.repository.RutinaRepository;

import tools.jackson.databind.ObjectMapper;

/**
 * Carga el catálogo DEMO de ejercicios y rutinas base cuando las tablas están vacías.
 *
 * Archivos esperados en src/main/resources/demo/:
 *  - ejercicios-demo.json
 *  - rutinas-demo.json
 *
 * Se puede desactivar con app.demo.seed-catalog=false
 */
@Component
public class DemoCatalogSeeder implements CommandLineRunner {

    public record EjercicioSeed(String nombreEjercicio, String descripcion, String videoEjercicio,
            String zonaMuscular, String implemento, String nivelDificultad, String musculoObjetivo,
            Boolean activoCatalogo) {
    }

    public record RutinaEjercicioSeed(String ejercicio, Integer dia, Integer orden, Integer series,
            Integer repeticiones, Integer tiempoSeg, Float pesoReferencia) {
    }

    public record RutinaSeed(String nombreRutina, String descripcion, Integer cantidadDias,
            List<RutinaEjercicioSeed> ejercicios) {
    }

    @Autowired
    private EjercicioRepository ejercicioRepo;

    @Autowired
    private RutinaRepository rutinaRepo;

    @Autowired
    private RutinaEjercicioRepository rutinaEjercicioRepo;

    @Value("${app.demo.seed-catalog:true}")
    private boolean habilitado;

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void run(String... args) throws Exception {
        if (!habilitado) {
            return;
        }

        if (ejercicioRepo.count() == 0) {
            EjercicioSeed[] datos = leer("demo/ejercicios-demo.json", EjercicioSeed[].class);
            for (EjercicioSeed s : datos) {
                EjercicioEntity e = new EjercicioEntity();
                e.setNombreEjercicio(s.nombreEjercicio());
                e.setDescripcion(s.descripcion());
                e.setVideoEjercicio(s.videoEjercicio());
                e.setZonaMuscular(s.zonaMuscular());
                e.setImplemento(s.implemento());
                e.setNivelDificultad(s.nivelDificultad());
                e.setMusculoObjetivo(s.musculoObjetivo());
                e.setActivoCatalogo(s.activoCatalogo() == null ? Boolean.TRUE : s.activoCatalogo());
                ejercicioRepo.save(e);
            }
            System.out.println("Ejercicios demo cargados: " + datos.length);
        }

        if (rutinaRepo.count() == 0) {
            Map<String, Integer> idPorNombre = new HashMap<>();
            for (EjercicioEntity e : ejercicioRepo.findAll()) {
                idPorNombre.put(e.getNombreEjercicio(), e.getIdEjercicio());
            }

            RutinaSeed[] rutinas = leer("demo/rutinas-demo.json", RutinaSeed[].class);
            for (RutinaSeed r : rutinas) {
                RutinaEntity rutina = new RutinaEntity();
                rutina.setIdUsuarioCreador(null);
                rutina.setNombreRutina(r.nombreRutina());
                rutina.setDescripcion(r.descripcion());
                rutina.setCantidadDias(r.cantidadDias());
                rutina.setEsBase(true);
                rutina.setActivoCatalogo(true);
                RutinaEntity guardada = rutinaRepo.save(rutina);

                for (RutinaEjercicioSeed re : r.ejercicios()) {
                    Integer idEjercicio = idPorNombre.get(re.ejercicio());
                    if (idEjercicio == null) {
                        throw new IllegalStateException(
                                "La rutina '" + r.nombreRutina() + "' usa un ejercicio que no existe: " + re.ejercicio());
                    }
                    RutinaEjercicioEntity fila = new RutinaEjercicioEntity();
                    fila.setIdRutina(guardada.getIdRutina());
                    fila.setIdEjercicio(idEjercicio);
                    fila.setDia(re.dia());
                    fila.setOrden(re.orden());
                    fila.setSeries(re.series());
                    fila.setRepeticiones(re.repeticiones());
                    fila.setTiempoSeg(re.tiempoSeg());
                    fila.setPesoReferencia(re.pesoReferencia());
                    rutinaEjercicioRepo.save(fila);
                }
            }
            System.out.println("Rutinas base demo cargadas: " + rutinas.length);
        }
    }

    private <T> T leer(String ruta, Class<T> tipo) throws Exception {
        try (InputStream in = new ClassPathResource(ruta).getInputStream()) {
            return mapper.readValue(in, tipo);
        }
    }
}
