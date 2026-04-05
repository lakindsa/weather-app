package cs1302.api;

/**
 * Represents the location information for a city, typically returned by a
 * geocoding API like OpenWeatherMap. This class is used to store relevant
 * data parsed from JSON, such as the city's name, latitude, longitude,
 * and country code.
 */
public class CityLocation {

    /** The name of the city (e.g., "Athens"). */
    public String name;

    /** The latitude coordinate of the city. */
    public double lat;

    /** The longitude coordinate of the city. */
    public double lon;

    /** The ISO 3166 country code (e.g., "US", "GR"). */
    public String country;
}
