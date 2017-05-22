package com.jasontoradler.moviesearch.data;

import android.util.Pair;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

/**

 Corresponds to the movie/series details queried by IMDB ID. All String-String key-value pairs are
 added to the 'data' list except the "Response", "Poster", and "Title" fields. The non-String values
 are ignored (e.g. the Ratings) for now.

 For example:

 "Title": "Red Dragon",
 "Year": "2002",
 "Rated": "R",
 "Released": "04 Oct 2002",
 "Runtime": "124 min",
 "Genre": "Crime, Drama, Thriller",
 "Director": "Brett Ratner",
 "Writer": "Thomas Harris (novel), Ted Tally (screenplay)",
 "Actors": "Anthony Hopkins, Edward Norton, Ralph Fiennes, Harvey Keitel",
 "Plot": "A retired FBI agent with psychological gifts is assigned to help track down \"The Tooth Fairy\", a mysterious serial killer; aiding him is imprisoned forensic psychiatrist Hannibal \"The Cannibal\" Lecter.",
 "Language": "English, French",
 "Country": "Germany, USA",
 "Awards": "4 wins & 10 nominations.",
 "Poster": "https://images-na.ssl-images-amazon.com/images/M/MV5BMTQ4MDgzNjM5MF5BMl5BanBnXkFtZTYwMjUwMzY2._V1_SX300.jpg",
 "Ratings": [],
 "Metascore": "60",
 "imdbRating": "7.2",
 "imdbVotes": "211,530",
 "imdbID": "tt0289765",
 "Type": "movie",
 "DVD": "01 Apr 2003",
 "BoxOffice": "$92,930,005.00",
 "Production": "Universal Pictures",
 "Website": "http://www.reddragonmovie.com/",
 "Response": "True"

 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MovieDetails {
    public String response;
    public String poster;
    public String title;
    public List<Pair<String, String>> data = new ArrayList<>();
}
