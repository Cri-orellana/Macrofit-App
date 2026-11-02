# MacroFit · Backend (API REST)

API REST de **MacroFit**, una app para llevar el control de macronutrientes y rutinas de ejercicio.
Este repositorio es parte de un proyecto de portafolio compuesto por:

| Parte | Tecnología |
|---|---|
| **Backend (este repo)** | Java 21 · Spring Boot 4 · Spring Security + JWT · JPA/Hibernate · MySQL |
| App móvil | Android nativo |
| Panel de administración | React + Vite |

> 🧪 **Proyecto de demostración.** Todos los datos (usuarios, ejercicios, rutinas, recetas) son ficticios.

## Qué hace

- Registro y login con **JWT** (roles `USER` y `ADMIN`).
- Cálculo de **TMB y calorías diarias** según edad, peso, altura, sexo, objetivo y nivel de actividad.
- Diario de comidas, historial y cierre de día.
- Búsqueda de alimentos y **código de barras** con la API de [Open Food Facts](https://world.openfoodfacts.org).
- **Recomendaciones de recetas** según dieta y macros con [Spoonacular](https://spoonacular.com/food-api), con caché en base de datos y respaldo local.
- Catálogo de **ejercicios y rutinas** (rutinas base y personalizadas), con importación desde Excel.

## Probarlo en local (con Docker)

Requisitos: Docker y Docker Compose.

```bash
cp .env.example .env      # en Windows: copy .env.example .env
docker compose up --build
```

- API: http://localhost:8080
- Documentación interactiva (Swagger): http://localhost:8080/swagger-ui.html
- Estado: http://localhost:8080/health

Al iniciar se crean las tablas y se cargan **datos de demostración** automáticamente.

### Cuentas de demostración

| Rol | Correo | Contraseña |
|---|---|---|
| Administrador | `admin@macrofit.demo` | `Admin1234` |
| Usuario | `demo@macrofit.demo` | `Demo1234` |

Para entrar: `POST /api/usuarios/login` con `{"correo": "...", "contrasena": "..."}`. La respuesta incluye el token; en Swagger pulsa **Authorize** y pégalo.

## Configuración

Todo se configura con variables de entorno (ver `.env.example`).

| Variable | Descripción | Por defecto |
|---|---|---|
| `DB_HOST` `DB_PORT` `DB_NAME` `DB_USER` `DB_PASSWORD` | Conexión a MySQL | `localhost` · `3306` · `macrofit_db` · `macrofit` · — |
| `DB_SSL_MODE` | Modo SSL de MySQL (`DISABLED`, `PREFERRED`, `REQUIRED`) | `PREFERRED` |
| `JWT_SECRET` | Secreto para firmar tokens (**obligatorio**, mín. 32 caracteres) | — |
| `CORS_ALLOWED_ORIGINS` | Orígenes permitidos, separados por coma | localhost + panel admin |
| `SPOONACULAR_API_KEY` | Opcional. Sin ella se usan recetas de respaldo | vacío |
| `DEMO_SEED_CATALOG` / `DEMO_SEED_USERS` | Cargar ejercicios, rutinas, recetas y usuarios demo | `true` |
| `PORT` | Puerto HTTP | `8080` |

### Modo demo y APIs externas

- **Sin API key de Spoonacular**, o si se agota la cuota o la API falla, las recomendaciones salen de las recetas de ejemplo guardadas en la base de datos.
- **Open Food Facts** no necesita key. Si no responde, el endpoint devuelve 404 sin romper la app.

## Seguridad

- Contraseñas con **BCrypt**; el hash nunca se devuelve en las respuestas.
- Un registro público **siempre crea un usuario `USER`**; el rol `ADMIN` solo existe vía seeder.
- Crear, editar o borrar los catálogos compartidos (ejercicios, comidas, recetas en caché) requiere rol `ADMIN`.
- Sin secretos en el repositorio: todo viene de variables de entorno.

## Estructura

```
src/main/java/com/proyecto/macrofit
├── config/             Seeders de datos demo, health check, Swagger
├── macros/             Diario, historial, alimentos (Open Food Facts)
├── rutinaejercicio/    Ejercicios, rutinas, rutinas de usuario, importación Excel
└── usuarios/           Auth (JWT), usuarios, recomendaciones (Spoonacular), catálogos
src/main/resources/demo/   Datos ficticios de ejercicios, rutinas y recetas (JSON)
demo-data/                 Excel de ejercicios compatible con POST /ejercicios/importar-excel
```

## Importar ejercicios desde Excel

`POST /ejercicios/importar-excel` (rol `ADMIN`) acepta un `.xlsx` con la hoja **Ejercicios** y los encabezados en la **fila 16**.
Hay un ejemplo listo en `demo-data/ejercicios_demo.xlsx`.
