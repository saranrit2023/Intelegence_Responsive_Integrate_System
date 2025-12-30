package com.jarvis.services;

import com.jarvis.config.Config;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;

/**
 * Weather information service using OpenWeatherMap API
 */
public class WeatherService {
    private final Config config;
    private final OkHttpClient httpClient;
    private final String apiKey;
    private static final String WEATHER_API_URL = "https://api.openweathermap.org/data/2.5/weather";
    
    public WeatherService() {
        this.config = Config.getInstance();
        this.httpClient = new OkHttpClient();
        this.apiKey = config.getOpenWeatherApiKey();
    }
    
    /**
     * Get weather for a city
     */
    public String getWeather(String city) {
        if (apiKey == null || apiKey.isEmpty()) {
            return "I need an OpenWeatherMap API key to check the weather. Please configure it in your .env file.";
        }
        
        try {
            String url = WEATHER_API_URL + "?q=" + city + "&appid=" + apiKey + "&units=metric";
            
            Request request = new Request.Builder()
                .url(url)
                .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return "I couldn't find weather information for " + city;
                }
                
                String responseBody = response.body().string();
                return parseWeatherResponse(responseBody, city);
            }
            
        } catch (IOException e) {
            return "I encountered an error while fetching weather data: " + e.getMessage();
        }
    }
    
    /**
     * Get weather for default location (can be configured)
     */
    public String getCurrentWeather() {
        String defaultCity = config.getProperty("weather.default.city", "London");
        return getWeather(defaultCity);
    }
    
    /**
     * Parse weather API response
     */
    private String parseWeatherResponse(String jsonResponse, String city) {
        try {
            JsonObject responseObj = JsonParser.parseString(jsonResponse).getAsJsonObject();
            
            JsonObject main = responseObj.getAsJsonObject("main");
            double temp = main.get("temp").getAsDouble();
            double feelsLike = main.get("feels_like").getAsDouble();
            int humidity = main.get("humidity").getAsInt();
            
            JsonObject weather = responseObj.getAsJsonArray("weather").get(0).getAsJsonObject();
            String description = weather.get("description").getAsString();
            
            return String.format(
                "The weather in %s is %s with a temperature of %.1f degrees Celsius. " +
                "It feels like %.1f degrees with %d percent humidity.",
                city, description, temp, feelsLike, humidity
            );
            
        } catch (Exception e) {
            return "I had trouble parsing the weather data.";
        }
    }
}
