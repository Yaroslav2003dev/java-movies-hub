package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.model.MovieRequest;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class MoviesHandler extends BaseHttpHandler {

    private static final Gson gson = new Gson();
    private static final AtomicLong idGenerator = new AtomicLong();
    private static final MoviesStore store = new MoviesStore();

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        String query = ex.getRequestURI().getQuery();
        if (query != null && query.startsWith("year=")) {
            handleFilter(ex, query.substring(5));
        } else {
            String[] parts = path.split("/");
            boolean hasId = parts.length > 2 && !parts[2].isBlank();
            String id = hasId ? parts[2] : null;
            switch (ex.getRequestMethod()) {
                case "GET":
                    if (hasId) {
                        handleGetById(ex, id);
                    } else {
                        handleGetAll(ex);
                    }
                    break;
                case "POST":
                    handlePost(ex);
                    break;
                case "DELETE":
                    handleDeleteById(ex, id);
                    break;
                default:
                    sendStatus(ex, 405);
            }
        }

    }

    private void handlePost(HttpExchange ex) throws IOException {
        String contentType = ex.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null || !contentType.equals(CT_JSON)) {
            sendStatus(ex, 415);
            return;
        }
        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        MovieRequest req = gson.fromJson(body, MovieRequest.class);

        List<String> errors = validate(req);
        if (!errors.isEmpty()) {
            ErrorResponse err = new ErrorResponse("Ошибка валидации", errors);
            sendJson(ex, 422, gson.toJson(err));
            return;
        }

        Movie movie = new Movie(
                idGenerator.incrementAndGet(),
                req.title,
                req.year
        );
        store.add(movie.getYear(), movie);
        sendJson(ex, 201, gson.toJson(movie));
    }


    private List<String> validate(MovieRequest req) {
        List<String> errors = new ArrayList<>();

        if (req.title == null || req.title.isBlank()) {
            errors.add("название не должно быть пустым");
        } else if (req.title.length() > 100) {
            errors.add("название не должно превышать 100 символов");
        }

        int maxYear = Year.now().getValue() + 1;
        if (req.year < 1888 || req.year > maxYear) {
            errors.add("год должен быть между 1888 и " + maxYear);
        }

        return errors;
    }


    public void handleGetById(HttpExchange ex, String id) throws IOException {

        int movieId;

        // Проверка, что id — число
        try {
            movieId = Integer.parseInt(id);
        } catch (NumberFormatException e) {
            sendJson(ex, 400, gson.toJson(
                    new ErrorResponse("Ошибка валидации", List.of("ID фильма должен быть числом"))
            ));
            return;
        }

        Movie movie = store.getMovieById(movieId);
        if (movie == null) {
            sendJson(ex, 404, gson.toJson(
                    new ErrorResponse("Ошибка валидации", List.of("Фильм не найден"))
            ));
            return;
        }

        sendJson(ex, 200, gson.toJson(movie));

    }


    public void handleGetAll(HttpExchange ex) throws IOException {
        String jsonString = gson.toJson(store.getAll());
        sendJson(ex, 200, jsonString);
    }


    public void handleDeleteById(HttpExchange ex, String id) throws IOException {

        int movieId;

        // Проверка, что id — число
        try {
            movieId = Integer.parseInt(id);
        } catch (NumberFormatException e) {
            sendJson(ex, 400, gson.toJson(
                    new ErrorResponse("Ошибка валидации", List.of("ID фильма должен быть числом"))
            ));
            return;
        }

        Movie movie = store.getMovieById(movieId);
        if (movie == null) {
            sendJson(ex, 404, gson.toJson(
                    new ErrorResponse("Ошибка валидации", List.of("Фильм не найден"))
            ));
            return;
        }
        store.deleteById(movieId);

        sendStatus(ex, 204);

    }

    public void handleFilter(HttpExchange ex, String year) throws IOException {


        try {
            int yearMovies = Integer.parseInt(year);
            List<Movie> listMovies = store.getMoviesByYear(yearMovies);

            if (listMovies == null) {
                listMovies = List.of();
            }

            sendJson(ex, 200, gson.toJson(listMovies));


        } catch (NumberFormatException e) {
            sendJson(ex, 400, gson.toJson(
                    new ErrorResponse("Ошибка валидации", List.of("Год фильма должен быть числом"))
            ));

        }

    }


}
