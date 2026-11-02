package com.proyecto.macrofit.config;

import java.io.InputStream;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.proyecto.macrofit.usuarios.model.RecetaCache;
import com.proyecto.macrofit.usuarios.repository.RecetaCacheRepository;

import tools.jackson.databind.ObjectMapper;

/**
 * Carga recetas de ejemplo en receta_cache. SpoonacularService las usa como respaldo
 * cuando no hay API key, se agotó la cuota o la API no responde.
 *
 * Se puede desactivar con app.demo.seed-catalog=false
 */
@Component
public class DemoRecetasSeeder implements CommandLineRunner {

    public record RecetaSeed(String dieta, String nombreComida, String descripcion, String cantidadPorcion,
            Float calorias, Float proteina, Float carbohidratos, Float grasa,
            List<String> ingredientes, List<String> preparacion) {
    }

    // Ids falsos y altos para no chocar nunca con ids reales de Spoonacular
    private static final int ID_BASE = 9_000_000;

    @Autowired
    private RecetaCacheRepository cacheRepo;

    @Value("${app.demo.seed-catalog:true}")
    private boolean habilitado;

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void run(String... args) throws Exception {
        if (!habilitado) {
            return;
        }

        try (InputStream in = new ClassPathResource("demo/recetas-demo.json").getInputStream()) {
            RecetaSeed[] recetas = mapper.readValue(in, RecetaSeed[].class);

            // Si la primera ya existe, asumimos que todas fueron cargadas
            if (recetas.length == 0 || cacheRepo.existsBySpoonacularId(ID_BASE + 1)) {
                return;
            }

            int i = 1;
            for (RecetaSeed r : recetas) {
                RecetaCache c = new RecetaCache();
                c.setCacheKey("demo|" + r.dieta().toLowerCase());
                c.setSpoonacularId(ID_BASE + i++);
                c.setNombre_comida(r.nombreComida());
                c.setDescripcion_comida(r.descripcion());
                c.setCantidad_porcion(r.cantidadPorcion());
                c.setCalorias_porcion(r.calorias());
                c.setProteina_porcion(r.proteina());
                c.setCarbohidratos_porcion(r.carbohidratos());
                c.setGrasa_porcion(r.grasa());
                c.setFoto_comida(null);
                c.setIngredientes(r.ingredientes());
                c.setPreparacion(r.preparacion());
                cacheRepo.save(c);
            }
            System.out.println("Recetas demo cargadas: " + recetas.length);
        }
    }
}
