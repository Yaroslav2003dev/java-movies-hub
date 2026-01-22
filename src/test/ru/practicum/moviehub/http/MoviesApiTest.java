package ru.practicum.moviehub.http;

import com.google.gson.*;
import org.junit.jupiter.api.*;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.model.MovieRequest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class MoviesApiTest {
    private static final String BASE = "http://localhost:8080";
    private static MoviesServer server;
    private static HttpClient client;

    @BeforeAll
    static void beforeAll() {
        server = new MoviesServer();
        server.start();
        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
    }

    @BeforeEach
    void beforeEach() {

    }

    @AfterAll
    static void afterAll() {
        server.stop();
    }

    @Test
    @DisplayName("Получение всех фильмов")
    void test_getMovies_whenStorageIsEmpty_thenReturnsEmptyArray() throws Exception {
        // Given
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .GET()
                .build();
        // When
        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        String body = resp.body().trim();
        // Then
        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");

        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");
        assertTrue(body.startsWith("[") && body.endsWith("]"),
                "Ожидается JSON-массив");
        assertEquals("[]", resp.body().trim());
    }

    @Test
    @DisplayName("Добавление фильма")
    void test_getMovies_afterMovieIsCreated_thenReturnsMovieInArray() throws Exception {
        // Given
        Gson gson = new Gson();
        MovieRequest body = new MovieRequest("Шрек", 2000);
        String json = gson.toJson(body);

        HttpRequest postReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        // When
        HttpResponse<String> postResp =
                client.send(postReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        HttpRequest getReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(getReq, HttpResponse.BodyHandlers.ofString());
        JsonElement jsonElement = JsonParser.parseString(response.body());
        JsonObject jsonObject = jsonElement.getAsJsonArray().get(0).getAsJsonObject();
        String title = jsonObject.get("title").getAsString();
        int year = jsonObject.get("year").getAsInt();
        // Then
        assertEquals(201, postResp.statusCode());
        assertTrue(title.equals(body.title));
        assertTrue(year == body.year);

    }

    @Test
    @DisplayName("Получение фильма по id")
    void test_getMovieById_afterMovieIsCreated_thenReturnsMovieObject() throws Exception {
        // Given
        Gson gson = new Gson();
        MovieRequest body = new MovieRequest("Шрек", 2000);
        int id = 1;
        String json = gson.toJson(body);
        HttpRequest postReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();
        client.send(postReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        // When
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/" + id))
                .GET()
                .build();
        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");
        JsonElement jsonElement = JsonParser.parseString(resp.body());
        JsonObject jsonObject = jsonElement.getAsJsonObject();

        String title = jsonObject.get("title").getAsString();
        int year = jsonObject.get("year").getAsInt();
        int respId = jsonObject.get("id").getAsInt();

        // Then
        Assertions.assertTrue(title.equals(body.title));
        Assertions.assertTrue(year == body.year);
        Assertions.assertTrue(id == respId);
        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");

    }


    @Test
    @DisplayName("Удаление фильма по id")
    void test_deleteMovieById_afterMovieIsCreated_thenReturns204() throws Exception {
        // Given
        Gson gson = new Gson();
        MovieRequest body = new MovieRequest("Шрек", 2000);
        int id = 1;
        String json = gson.toJson(body);
        HttpRequest postReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();
        client.send(postReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        // When
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/" + id))
                .DELETE()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        //Then
        assertEquals(204, resp.statusCode(), "DELETE /movies должен вернуть 200");

    }

    @Test
    @DisplayName("Получение фильмов по году")
    void test_whenFilteredByYear_thenReturnsMoviesWithThatYear() throws Exception {
        // Given
        Gson gson = new Gson();
        MovieRequest body = new MovieRequest("Шрек", 2000);
        int year = 2000;
        String json = gson.toJson(body);
        HttpRequest postReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();
        client.send(postReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        // When
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=" + year))
                .GET()
                .build();
        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        JsonElement jsonElement = JsonParser.parseString(resp.body());
        String moviesStr = jsonElement.getAsJsonArray().toString();
        List<Movie> moviesList = gson.fromJson(moviesStr, new ListOfMoviesTypeToken().getType());
        //Then
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");
        assertEquals(200, resp.statusCode(), "200");
        System.out.println("Список фильмов: ");
        for (Movie movie : moviesList) {
            System.out.println(movie.getTitle());
        }

    }


}