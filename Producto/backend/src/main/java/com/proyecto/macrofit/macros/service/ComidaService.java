package com.proyecto.macrofit.macros.service;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.proyecto.macrofit.macros.model.ComidaDto;

import tools.jackson.databind.ObjectMapper;

@Service
public class ComidaService {

    private final RestTemplate restTemplate;
    private final String BASE_URL = "https://world.openfoodfacts.org";
    private final ObjectMapper mapper = new ObjectMapper();

    // OpenFoodFacts pide identificar la app con un User-Agent real
    @Value("${openfoodfacts.user-agent}")
    private String userAgent;

    public ComidaService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public ComidaDto getComidaByBarra(String barra) {
        // El código de barras solo puede tener dígitos (evita meter rutas o parámetros raros en la URL)
        String codigo = barra == null ? "" : barra.replaceAll("\\D", "");
        if (codigo.isEmpty()) {
            return null;
        }
        String url = String.format("%s/api/v0/product/%s.json", BASE_URL, codigo);

        try {

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", userAgent);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            Map<String, Object> resp = response.getBody();

            if (resp == null)
                return null;

            Object comidaData = resp.get("product");
            if (comidaData == null)
                return null;

            return mapper.convertValue(comidaData, ComidaDto.class);
        } catch (Exception e) {
            System.err.println("OpenFoodFacts (barra) falló: " + e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public List<ComidaDto> getComidasByNombre(String nombre) {
        // Se codifica la URL: nombres con espacios o tildes ("leche descremada", "plátano") fallaban antes
        URI url = UriComponentsBuilder.fromUriString(BASE_URL + "/cgi/search.pl")
                .queryParam("search_terms", nombre)
                .queryParam("search_simple", 1)
                .queryParam("action", "process")
                .queryParam("json", 1)
                .queryParam("page_size", 20)
                .build()
                .encode()
                .toUri();
        try {
            // También le ponemos las cabeceras a la búsqueda por nombre
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", userAgent);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            Map<String, Object> resp = response.getBody();

            if (resp == null)
                return Collections.emptyList();

            List<Map<String, Object>> comidaList = (List<Map<String, Object>>) resp.get("products");

            if (comidaList == null || comidaList.isEmpty()) {
                return Collections.emptyList();
            }

            // Mantiene tu mapeo con streams para devolver la lista de DTOs
            return comidaList.stream()
                    .map(comidaData -> mapper.convertValue(comidaData, ComidaDto.class))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("OpenFoodFacts (búsqueda) falló: " + e.getMessage());
            return Collections.emptyList();
        }
    }

}
