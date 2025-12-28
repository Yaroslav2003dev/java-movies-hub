package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public class MoviesStore {
    HashMap<Integer, List<Movie>> movies= new HashMap<>();

    public List<Movie> getMoviesByYear(int year){
        return movies.get(year);
    }

    public List<Movie> getAll(){
        return movies.values()
                .stream()
                .flatMap(List::stream)
                .toList();
    }

    public void add(int year, Movie movie){
        List<Movie> copyMovies = Optional
                .ofNullable(movies.get(year))
                .orElseGet(ArrayList::new);
        copyMovies.add(movie);
        movies.put(year,copyMovies);


    }

    public void clear(){
        movies.clear();
    }

    public void deleteById(int id) {
        for (Movie movie : getAll()) {
            if (movie.getId() == id) {
                int year = movie.getYear();
                List<Movie> copyMovies = movies.get(year);
                copyMovies.remove(movie);
                movies.put(year,copyMovies);
            }
        }

    }

    public Movie getMovieById(int id){
    for(Movie movie : getAll()){
        if(movie.getId()==id){
            return movie;
        }
    }
    return null;
    }

}