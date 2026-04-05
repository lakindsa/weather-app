# Weather App (Java)

This project is a Java-based application that allows users to search for a city and view the current weather conditions. It demonstrates how to integrate multiple RESTful APIs to retrieve and display real-time data.

## Features
- Search for a city by name
- Automatically converts the city into geographic coordinates
- Retrieves current weather data for that location
- Supports both Imperial and Metric units

## How It Works
The application combines two APIs:

- **OpenWeatherMap Geocoding API**  
  Converts a city name into latitude and longitude coordinates

- **Open-Meteo API**  
  Uses those coordinates to fetch current weather data

The user only inputs a city name, and the application handles all API interactions internally.

## Technologies Used
- Java
- Maven
- REST APIs
- JSON parsing (Gson)
- Java HttpClient

## What I Learned
- How to work with RESTful APIs in Java
- Sending HTTP requests using `HttpClient`
- Parsing JSON data into Java objects using Gson
- Structuring a multi-step data pipeline (geocoding → weather retrieval)

## How to Run
1. Clone the repository
2. Run the application:
   ```bash
   ./run.sh
