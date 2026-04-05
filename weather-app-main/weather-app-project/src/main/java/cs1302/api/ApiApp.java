package cs1302.api;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import javafx.scene.Scene;
import javafx.stage.Stage;

import javafx.application.Application;
import javafx.application.Platform;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import com.google.gson.Gson;


/**
 * A JavaFX application that allows users to look up the current weather
 * for any city using two RESTful JSON APIs. It uses the OpenWeatherMap
 * Geocoding API to convert city names into latitude and longitude, and then
 * queries the Open-Meteo API to retrieve current weather information such as
 * temperature, wind speed, and wind direction.
 * <p>
 * Users can toggle between imperial and metric units and view matching weather
 * icons and brief textual descriptions of the current weather.
 */
public class ApiApp extends Application {
    Stage stage;
    Scene scene;
    VBox root;

    private HttpClient client = HttpClient.newHttpClient();
    private Gson gson = new Gson();
    private String openWeatherApiKey;

    private WeatherInfo.CurrentWeather lastWeather = null;
    private boolean lastWeatherIsValid = false;
    private boolean useMetric = false; // Default to imperial units

    private Label descriptionLabel;

    /**
     * Constructs an {@code ApiApp} object. This default (i.e., no argument)
     * constructor is executed in Step 2 of the JavaFX Application Life-Cycle.
     */
    public ApiApp() {
        root = new VBox();

        System.out.println("Loading config...");

        // Load API key from config
        try (FileInputStream fis = new FileInputStream("resources/config.properties")) {
            Properties props = new Properties();
            props.load(fis);
            openWeatherApiKey = props.getProperty("openweather.key");
        } catch (IOException e) {
            System.err.println("Could not load API key from config.properties");
            e.printStackTrace();
        }
    } // ApiApp


    /**
     * Initializes and displays the JavaFX application window.
     * Sets up the layout, event handlers, and connects the UI components.
     *
     * @param stage the primary stage for this application
     */
    @Override
    public void start(Stage stage) {
        this.stage = stage;
        root.setSpacing(15);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-padding: 20; -fx-font-size: 14;");
        descriptionLabel = new Label();

        setupUI(stage);
        setupEvents();

        stage.setScene(scene = new Scene(root));
        stage.setTitle("ApiApp!");
        stage.setOnCloseRequest(e -> Platform.exit());
        stage.sizeToScene();
        stage.show();
        Platform.runLater(() -> root.requestFocus());
    } // start


    private TextField cityInput;
    private Button searchButton;
    private ComboBox<String> unitSelector;
    private Label cityLabel, weatherLabel;
    private ImageView iconView;

    /**
     * Sets up the user interface layout for the application.
     * Initializes all JavaFX UI components and adds them to the root container.
     *
     * @param stage the primary stage used by the application
     */
    private void setupUI(Stage stage) {
        // Load and configure the banner image
        ImageView banner = new ImageView(new Image("file:resources/weather-banner.png"));
        banner.setPreserveRatio(true);
        banner.setFitWidth(640);

        // Text field for city input
        cityInput = new TextField();
        cityInput.setPromptText("Enter city (e.g. Athens,US or Paris,FR)");

        // Button to trigger the weather search
        searchButton = new Button("Get Weather");

        // Dropdown menu for unit selection (Imperial vs Metric)
        unitSelector = new ComboBox<>();
        unitSelector.getItems().addAll("Imperial (°F, mph)", "Metric (°C, km/h)");
        unitSelector.setValue("Imperial (°F, mph)");

        // Horizontal container to place the search button and unit selector side-by-side
        HBox controls = new HBox(10, searchButton, unitSelector);
        controls.setAlignment(Pos.CENTER);

        // Labels to display city and weather information
        cityLabel = new Label();
        weatherLabel = new Label();

        // Weather icon view
        iconView = new ImageView();
        iconView.setFitWidth(128);
        iconView.setFitHeight(128);

        // Add all components to the root layout
        root.getChildren().addAll(
            banner,
            cityInput,
            controls,
            cityLabel,
            weatherLabel,
            iconView,
            descriptionLabel
        );
    } // setupUI

    /**
     * Sets up event handlers for UI interactions.
     * Handles unit selection changes and search button clicks,
     * including input validation and initiating the city search.
     */
    private void setupEvents() {

        // Handle unit selection from ComboBox
        unitSelector.setOnAction(e -> {
            // Update internal state based on selected units
            useMetric = unitSelector.getValue().contains("Metric");

            // If previous weather data is valid, update the display using the new units
            if (lastWeatherIsValid) {
                weatherLabel.setText(formatWeather(lastWeather, useMetric));
            }
        });

        // Handle weather search button clicks
        searchButton.setOnAction(e -> {
            searchButton.setDisable(true); // Prevent spamming
            String city = cityInput.getText().trim(); // Get and sanitize input

            // Validate input:
            // must be at least 3 characters and only contain letters, spaces, or commas
            if (city.isEmpty() || city.length() < 3 || !city.matches("^[a-zA-Z\\s,]+$")) {
                cityLabel.setText("Enter valid city name.");
                weatherLabel.setText("");
                searchButton.setDisable(false); // Re-enable if validation fails
                return;
            }

            // Update UI and begin search
            cityLabel.setText("Searching for " + city + "...");
            weatherLabel.setText("");
            searchCity(city, cityLabel, weatherLabel, iconView, searchButton, useMetric);
        });
    } // setupEvents

    /**
     * Searches for the geographic coordinates of the specified city using
     * the OpenWeatherMap Geocoding API. If successful, updates the UI with
     * the location details and initiates a weather data fetch for that location.
     *
     * @param city           the name of the city to search for
     * @param cityLabel      label to display city location information
     * @param weatherLabel   label to display weather data
     * @param iconView       image view for weather icons
     * @param searchButton   the search button to re-enable on completion
     * @param useMetric      whether to display weather info in metric units
     */
    private void searchCity(String city, Label cityLabel,
                            Label weatherLabel, ImageView iconView,
                            Button searchButton, boolean useMetric) {

        // Safely encode city for use in URL
        String encodedCity = URLEncoder.encode(city, StandardCharsets.UTF_8);

        // Construct request URL for OpenWeatherMap geocoding
        String url = String.format(
            "https://api.openweathermap.org/geo/1.0/direct?q=%s&limit=1&appid=%s",
            encodedCity, openWeatherApiKey
        );

        // Build HTTP request
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .build();

        // Send request asynchronously and process the response
        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(HttpResponse::body)
            .thenAccept(json -> {
                // Parse JSON response into array of CityLocation objects
                CityLocation[] results = gson.fromJson(json, CityLocation[].class);

                if (results.length > 0) {
                    CityLocation location = results[0];

                    // Update UI with found location
                    String message = String.format("Found: %s, %s (%.2f, %.2f)",
                                                   location.name, location.country,
                                                   location.lat, location.lon);

                    Platform.runLater(() -> {
                        cityLabel.setText(message);
                        searchButton.setDisable(false); // Re-enable button
                    });

                    // Fetch weather data for that location
                    fetchWeather(location.lat, location.lon,
                                 weatherLabel, iconView,
                                 descriptionLabel, useMetric);
                } else {
                    // Handle case where no matching location was found
                    Platform.runLater(() -> {
                        cityLabel.setText("City not found.");
                        weatherLabel.setText("");
                        searchButton.setDisable(false);
                    });
                }
            })
            .exceptionally(e -> {
                // Handle request failure (e.g., network error)
                e.printStackTrace();
                Platform.runLater(() -> {
                    cityLabel.setText("Error fetching city data.");
                    weatherLabel.setText("");
                    searchButton.setDisable(false);
                });
                return null;
            });
    } // searchCity

    /**
     * Fetches current weather data for the specified latitude and longitude using
     * the Open-Meteo API. Updates the UI with the temperature, wind information,
     * a weather icon, and a human-readable weather description.
     *
     * @param lat               the latitude of the location
     * @param lon               the longitude of the location
     * @param outputLabel       label to display formatted weather summary
     * @param iconView          image view to display the appropriate weather icon
     * @param descriptionLabel  label to display a textual weather description
     * @param useMetric         whether to use metric units for temperature and wind
     */
    private void fetchWeather(
        double lat, double lon,
        Label outputLabel,
        ImageView iconView,
        Label descriptionLabel,
        boolean useMetric) {

        // Construct Open-Meteo API URL with coordinates
        String url = String.format(
            "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f&current_weather=true",
            lat, lon
        );

        // Build HTTP request
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .build();

        // Send async request and handle the response
        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(HttpResponse::body)
            .thenAccept(json -> {
                // Parse JSON into WeatherInfo object
                WeatherInfo info = gson.fromJson(json, WeatherInfo.class);
                WeatherInfo.CurrentWeather cw = info.currentWeather;

                // Null check
                if (cw == null) {
                    Platform.runLater(() -> outputLabel.setText("Weather data unavailable."));
                    return;
                }

                // Store latest weather for unit toggling
                lastWeather = cw;
                lastWeatherIsValid = true;

                // Format weather string based on unit preference
                String weatherMsg = formatWeather(cw, useMetric);

                // Get weather icon path and description from code
                int code = cw.weathercode;
                String iconPath = "file:resources/weather-icons/" + code + ".png";
                Image icon = new Image(iconPath, true);
                String description = getWeatherDescription(code);

                // Update UI components on JavaFX thread
                Platform.runLater(() -> {
                    outputLabel.setText(weatherMsg);
                    iconView.setImage(icon);
                    descriptionLabel.setText(description);
                });
            })
            .exceptionally(e -> {
                // Handle errors gracefully and notify user
                e.printStackTrace();
                Platform.runLater(() -> outputLabel.setText("Error fetching weather data."));
                return null;
            });
    } // fetchWeather

    /**
     * Formats weather data into a human-readable string based on the selected unit system.
     *
     * <p>The temperature and wind speed are converted to either metric or imperial
     * units, depending on the {@code useMetric} flag. The resulting string includes
     * temperature, wind speed, wind direction, and the timestamp.
     *
     * @param cw         the {@code CurrentWeather} object containing raw weather data
     * @param useMetric  {@code true} for metric units (°C, km/h);
     * {@code false} for imperial (°F, mph)
     * @return a formatted string representing the current weather conditions
     */
    private String formatWeather(WeatherInfo.CurrentWeather cw, boolean useMetric) {
        double temp = cw.temperature;
        double wind = cw.windspeed;

        // Convert units if using imperial
        if (!useMetric) {
            temp = temp * 9 / 5 + 32; // Celsius to Fahrenheit
            wind = wind / 1.609;      // km/h to mph
        }

        // Return formatted string
        return String.format(
            "Temp: %.1f %s, Wind: %.1f %s, Dir: %d° @ %s",
            temp,
            useMetric ? "°C" : "°F",
            wind,
            useMetric ? "km/h" : "mph",
            cw.winddirection,
            cw.time
        );
    } // formatWeather

    /**
     * Returns a human-readable description of the weather condition based on
     * the Open-Meteo weather code.
     *
     * <p>This method translates numerical weather codes into simple English
     * descriptions such as "Clear", "Rain", or "Snow". These descriptions
     * are shown alongside the weather icon in the UI.
     *
     * @param code the Open-Meteo weather code representing current conditions
     * @return a description of the weather condition (e.g., "Clear", "Rain", "Snow")
     */
    private String getWeatherDescription(int code) {
        return switch (code) {
        case 0 -> "Clear sky";
        case 1, 2, 3 -> "Partly cloudy";
        case 45, 48 -> "Foggy";
        case 51, 53, 55 -> "Drizzle";
        case 61, 63, 65 -> "Rainy";
        case 71, 73, 75 -> "Snowy";
        case 80, 81, 82 -> "Showers";
        case 95 -> "Thunderstorm";
        default -> "Weather: " + code;
        };
    }

} // ApiApp
