package com.proyecto.macrofit.usuarios.service;

import com.proyecto.macrofit.usuarios.model.ComidaRecomendada;
import com.proyecto.macrofit.usuarios.model.RecetaCache;
import com.proyecto.macrofit.usuarios.repository.RecetaCacheRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SpoonacularService {

    @Autowired
    private RecetaCacheRepository cacheRepo;

    @Autowired
    private LibreTranslateService libreTranslate;

    @Value("${spoonacular.api.key}")
    private String claveApi;

    @Value("${spoonacular.api.url}")
    private String urlBase;

    @Value("${spoonacular.cache.ttl-hours:24}")
    private long ttlHoras;

    private final RestTemplate restTemplate;

    public SpoonacularService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(15000);
        this.restTemplate = new RestTemplate(factory);
    }

    private String generarCacheKey(String tipoDieta, String ingredientes,
            Float maxCarbs, Float minProt, Float maxGrasa) {
        return String.format("%s|%s|%.1f|%.1f|%.1f",
                tipoDieta.toLowerCase().trim(),
                ingredientes.toLowerCase().trim(),
                maxCarbs, minProt, maxGrasa);
    }

    private ComidaRecomendada cacheAComida(RecetaCache cache) {
        ComidaRecomendada c = new ComidaRecomendada();
        c.setId_comida(cache.getSpoonacularId());
        c.setNombre_comida(cache.getNombre_comida());
        c.setDescripcion_comida(cache.getDescripcion_comida());
        c.setCantidad_porcion(cache.getCantidad_porcion());
        c.setCalorias_porcion(cache.getCalorias_porcion());
        c.setProteina_porcion(cache.getProteina_porcion());
        c.setCarbohidratos_porcion(cache.getCarbohidratos_porcion());
        c.setGrasa_porcion(cache.getGrasa_porcion());
        c.setFoto_comida(cache.getFoto_comida());
        c.setIngredientes_lista(cache.getIngredientes());
        c.setPreparacion_lista(cache.getPreparacion());
        return c;
    }

    private String traducirFiltroDieta(String dietaEspanol) {
        if (dietaEspanol == null || dietaEspanol.trim().isEmpty())
            return "";
        String dietaLimpia = dietaEspanol.toLowerCase();
        if (dietaLimpia.contains("vegetariana"))
            return "vegetarian";
        if (dietaLimpia.contains("vegana"))
            return "vegan";
        if (dietaLimpia.contains("keto") || dietaLimpia.contains("cetog"))
            return "ketogenic";
        if (dietaLimpia.contains("paleo"))
            return "paleo";
        return "";
    }

    private String traducirTituloAlEspanol(String textoIngles) {
        return libreTranslate.traducirEnEsp(textoIngles);
    }

    public int retraducirRecetasEnIngles() {
        List<RecetaCache> todas = cacheRepo.findAll();
        int traducidas = 0;

        for (RecetaCache receta : todas) {
            String nombreOriginal = receta.getNombre_comida();

            receta.setNombre_comida(traducirTituloAlEspanol(nombreOriginal));

            List<String> ings = receta.getIngredientes();
            if (ings != null && !ings.isEmpty()) {
                List<String> ingsTraducidos = new ArrayList<>();
                for (String ing : ings) {
                    ingsTraducidos.add(traducirTituloAlEspanol(ing));
                }
                receta.setIngredientes(ingsTraducidos);
            }

            List<String> pasos = receta.getPreparacion();
            if (pasos != null && !pasos.isEmpty()) {
                List<String> pasosTraducidos = new ArrayList<>();
                for (String paso : pasos) {
                    pasosTraducidos.add(traducirTituloAlEspanol(paso));
                }
                receta.setPreparacion(pasosTraducidos);
            }

            cacheRepo.save(receta);
            traducidas++;
            System.out.println("Traducida: " + nombreOriginal + " → " + receta.getNombre_comida());

            try {
                Thread.sleep(200);
            } catch (InterruptedException ignored) {
            }
        }

        return traducidas;
    }

    @SuppressWarnings("unchecked")
    public List<ComidaRecomendada> buscarRecetasPersonalizadas(
            String tipoDieta,
            String ingredientes,
            Float maxCarbohidratos,
            Float minProteina,
            Float maxGrasa) {

        String cacheKey = generarCacheKey(tipoDieta, ingredientes,
                maxCarbohidratos, minProteina, maxGrasa);

        LocalDateTime limiteExpiracion = LocalDateTime.now().minusHours(ttlHoras);

        List<RecetaCache> enCache = cacheRepo.findByCacheKeyAndCreadoEnAfter(cacheKey, limiteExpiracion);
        if (!enCache.isEmpty()) {
            System.out.println("Cache HIT (vigente): " + cacheKey + " (" + enCache.size() + " recetas)");
            return enCache.stream().map(this::cacheAComida).toList();
        }

        if (claveApi == null || claveApi.isBlank()) {
            System.out.println("Sin API key de Spoonacular → usando recetas de respaldo");
            return recetasDeRespaldo(tipoDieta, maxCarbohidratos, minProteina, maxGrasa);
        }

        if ("mock_key_temporal_123".equals(claveApi)) {
            System.out.println("⚠️ Modo Sandbox activado: Interceptando llamada a Spoonacular y devolviendo mock.");
            return generarListaMock(ingredientes);
        }

        System.out.println("🌐 Cache MISS o expirado: " + cacheKey + " → consultando Spoonacular");

        List<ComidaRecomendada> listaRecomendaciones = new ArrayList<>();

        try {
            String dietaTraducida = traducirFiltroDieta(tipoDieta);

            String urlFinal = UriComponentsBuilder.fromUriString(urlBase)
                    .queryParam("apiKey", claveApi)
                    .queryParam("diet", dietaTraducida)
                    .queryParam("includeIngredients", ingredientes)
                    .queryParam("maxCarbs", maxCarbohidratos)
                    .queryParam("minProtein", minProteina)
                    .queryParam("maxFat", maxGrasa)
                    .queryParam("number", 5)
                    .queryParam("addRecipeNutrition", true)
                    .queryParam("fillIngredients", true)
                    .queryParam("addRecipeInformation", true)
                    .queryParam("instructionsRequired", true)
                    .toUriString();

            Map<String, Object> respuestaSpoonacular = restTemplate.getForObject(urlFinal, Map.class);

            if (respuestaSpoonacular != null && respuestaSpoonacular.containsKey("results")) {
                List<Map<String, Object>> resultados = (List<Map<String, Object>>) respuestaSpoonacular.get("results");

                for (Map<String, Object> recetaIngles : resultados) {
                    ComidaRecomendada nuevaComida = new ComidaRecomendada();

                    nuevaComida.setId_comida((Integer) recetaIngles.get("id"));
                    nuevaComida.setFoto_comida((String) recetaIngles.get("image"));
                    nuevaComida.setDescripcion_comida("Recomendación personalizada.");

                    String tituloIngles = (String) recetaIngles.get("title");
                    nuevaComida.setNombre_comida(traducirTituloAlEspanol(tituloIngles));

                    List<String> listaIng = new ArrayList<>();
                    if (recetaIngles.containsKey("extendedIngredients")) {
                        // Corrección: Tipos genéricos explícitos restaurados
                        List<Map<String, Object>> extIng = (List<Map<String, Object>>) recetaIngles
                                .get("extendedIngredients");
                        if (extIng != null) {
                            for (Map<String, Object> ing : extIng) {
                                String ingredienteOriginal = (String) ing.get("original");
                                listaIng.add(traducirTituloAlEspanol(ingredienteOriginal));
                            }
                        }
                    }
                    nuevaComida.setIngredientes_lista(listaIng);

                    List<String> listaPasos = new ArrayList<>();
                    if (recetaIngles.containsKey("analyzedInstructions")) {
                        List<Map<String, Object>> instructions = (List<Map<String, Object>>) recetaIngles
                                .get("analyzedInstructions");
                        if (instructions != null && !instructions.isEmpty()) {
                            List<Map<String, Object>> steps = (List<Map<String, Object>>) instructions.get(0)
                                    .get("steps");
                            if (steps != null) {
                                for (Map<String, Object> step : steps) {
                                    String pasoOriginal = (String) step.get("step");
                                    if (pasoOriginal != null && !pasoOriginal.trim().isEmpty()) {
                                        listaPasos.add(traducirTituloAlEspanol(pasoOriginal));
                                    }
                                }
                            }
                        }
                    }

                    if (listaPasos.isEmpty() && recetaIngles.containsKey("instructions")) {
                        String instruccionesGenerales = (String) recetaIngles.get("instructions");
                        if (instruccionesGenerales != null && !instruccionesGenerales.trim().isEmpty()) {
                            listaPasos.add(traducirTituloAlEspanol(instruccionesGenerales));
                        }
                    }

                    if (listaPasos.isEmpty()) {
                        listaPasos.add("Mezclar los ingredientes y preparar según preferencia.");
                    }

                    nuevaComida.setPreparacion_lista(listaPasos);

                    Map<String, Object> nutricion = (Map<String, Object>) recetaIngles.get("nutrition");
                    if (nutricion != null) {
                        if (nutricion.containsKey("weightPerServing")) {
                            Map<String, Object> pesoPorcion = (Map<String, Object>) nutricion.get("weightPerServing");
                            if (pesoPorcion != null) {
                                Number cantidadPeso = (Number) pesoPorcion.get("amount");
                                String unidadMedida = (String) pesoPorcion.get("unit");
                                if (cantidadPeso != null && unidadMedida != null) {
                                    nuevaComida.setCantidad_porcion(cantidadPeso.intValue() + unidadMedida);
                                }
                            }
                        }

                        if (nutricion.containsKey("nutrients")) {
                            List<Map<String, Object>> nutrientes = (List<Map<String, Object>>) nutricion
                                    .get("nutrients");
                            if (nutrientes != null) {
                                for (Map<String, Object> nutriente : nutrientes) {
                                    String nombreNutriente = (String) nutriente.get("name");
                                    Float cantidad = ((Number) nutriente.get("amount")).floatValue();
                                    switch (nombreNutriente) {
                                        case "Calories":
                                            nuevaComida.setCalorias_porcion(cantidad);
                                            break;
                                        case "Protein":
                                            nuevaComida.setProteina_porcion(cantidad);
                                            break;
                                        case "Carbohydrates":
                                            nuevaComida.setCarbohidratos_porcion(cantidad);
                                            break;
                                        case "Fat":
                                            nuevaComida.setGrasa_porcion(cantidad);
                                            break;
                                    }
                                }
                            }
                        }
                    }

                    listaRecomendaciones.add(nuevaComida);
                }
            }

            for (ComidaRecomendada comida : listaRecomendaciones) {
                if (cacheRepo.existsBySpoonacularIdAndCacheKey(comida.getId_comida(), cacheKey)) {
                    System.out.println("⏭️ Ya existe en cache para esta búsqueda: " + comida.getNombre_comida());
                    continue;
                }

                RecetaCache cache = new RecetaCache();
                cache.setCacheKey(cacheKey);
                cache.setSpoonacularId(comida.getId_comida());
                cache.setNombre_comida(comida.getNombre_comida());
                cache.setDescripcion_comida(comida.getDescripcion_comida());
                cache.setCantidad_porcion(comida.getCantidad_porcion());
                cache.setCalorias_porcion(comida.getCalorias_porcion());
                cache.setProteina_porcion(comida.getProteina_porcion());
                cache.setCarbohidratos_porcion(comida.getCarbohidratos_porcion());
                cache.setGrasa_porcion(comida.getGrasa_porcion());
                cache.setFoto_comida(comida.getFoto_comida());
                cache.setIngredientes(comida.getIngredientes_lista());
                cache.setPreparacion(comida.getPreparacion_lista());
                cacheRepo.save(cache);
                System.out.println("💾 Guardada en español: " + comida.getNombre_comida());
            }

        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
        }

        if (listaRecomendaciones.isEmpty()) {
            System.out.println("↩️ Spoonacular no devolvió recetas → usando recetas de respaldo");
            return recetasDeRespaldo(tipoDieta, maxCarbohidratos, minProteina, maxGrasa);
        }

        return listaRecomendaciones;
    }

    private List<ComidaRecomendada> generarListaMock(String ingredientes) {
        List<ComidaRecomendada> mocks = new ArrayList<>();
        ComidaRecomendada mock = new ComidaRecomendada();

        String sufijo = (ingredientes != null && !ingredientes.isBlank()) ? " con " + ingredientes : " Saludable";

        mock.setId_comida(999999);
        mock.setNombre_comida("Receta Demo" + sufijo);
        mock.setDescripcion_comida("Recomendación generada en modo Sandbox para portafolio (Sin consumo de API).");
        mock.setCantidad_porcion("1 porción");
        mock.setCalorias_porcion(450.0f);
        mock.setProteina_porcion(35.0f);
        mock.setCarbohidratos_porcion(45.0f);
        mock.setGrasa_porcion(12.0f);
        mock.setFoto_comida("https://via.placeholder.com/300x200.png?text=Demo+Spoonacular");
        mock.setIngredientes_lista(List.of("Ingrediente de prueba 1", "Ingrediente de prueba 2"));
        mock.setPreparacion_lista(
                List.of("Mezclar los ingredientes.", "Cocinar según preferencia.", "Servir y disfrutar."));

        mocks.add(mock);
        return mocks;
    }

    private static String normalizar(String texto) {
        if (texto == null)
            return "";
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .trim();
    }

    private boolean cumpleMacros(RecetaCache r, Float maxCarbs, Float minProt, Float maxGrasa) {
        boolean carbs = maxCarbs == null || r.getCarbohidratos_porcion() == null
                || r.getCarbohidratos_porcion() <= maxCarbs;
        boolean prot = minProt == null || r.getProteina_porcion() == null
                || r.getProteina_porcion() >= minProt;
        boolean grasa = maxGrasa == null || r.getGrasa_porcion() == null
                || r.getGrasa_porcion() <= maxGrasa;
        return carbs && prot && grasa;
    }

    private List<ComidaRecomendada> recetasDeRespaldo(String tipoDieta, Float maxCarbs,
            Float minProt, Float maxGrasa) {

        String dieta = normalizar(tipoDieta);
        List<RecetaCache> todas = cacheRepo.findAll();

        List<RecetaCache> deLaDieta = todas.stream().filter(r -> {
            if (dieta.isEmpty())
                return true;
            String clave = normalizar(r.getCacheKey());
            return clave.startsWith(dieta + "|") || clave.startsWith("demo|" + dieta);
        }).toList();
        List<RecetaCache> base = deLaDieta.isEmpty() ? todas : deLaDieta;

        List<RecetaCache> estrictas = base.stream()
                .filter(r -> cumpleMacros(r, maxCarbs, minProt, maxGrasa)).toList();
        List<RecetaCache> elegidas = estrictas.isEmpty() ? base : estrictas;

        Map<Integer, RecetaCache> unicas = new LinkedHashMap<>();
        for (RecetaCache r : elegidas) {
            unicas.putIfAbsent(r.getSpoonacularId(), r);
        }
        List<RecetaCache> lista = new ArrayList<>(unicas.values());
        Collections.shuffle(lista);

        return lista.stream().limit(5).map(this::cacheAComida).toList();
    }
}