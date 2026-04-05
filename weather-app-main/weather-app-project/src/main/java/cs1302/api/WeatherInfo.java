package cs1302.api;

import com.google.gson.annotations.SerializedName;

/**
 * Represents weather forecast information for a given location,
 * as returned by the Open-Meteo API. This class is used to deserialize
 * JSON responses into usable Java objects.
 */
public class WeatherInfo {

    /** The latitude of the location for which the weather data applies. */
    public double latitude;

    /** The longitude of the location for which the weather data applies. */
    public double longitude;

    /** The current weather information at the specified location. */
    @SerializedName("current_weather")
    public CurrentWeather currentWeather;

    /**
     * Represents current weather conditions at a specific location.
     */
    public static class CurrentWeather {

        /** The air temperature in degrees Celsius. */
        public double temperature;

        /** The wind speed in kilometers per hour. */
        public double windspeed;

        /** The wind direction in degrees. */
        public int winddirection;

        /** The Open-Meteo weather code for icon/condition mapping. */
        public int weathercode;

        /** The ISO 8601 timestamp for the current weather reading. */
        public String time;
    }
}
