package org.example;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.util.Scanner;

public class App {
    private static final String API_KEY = "978d5bb38bbe4b199bd37d034c666fd5";
    private static final OkHttpClient httpClient = new OkHttpClient();
    private static final int RECIPES_PER_PAGE = 10;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Welcome to RecipeGO!");
        System.out.println("Type 'bye' to exit.");

        String lastQuery = null;
        int currentPage = 0;
        boolean hasNextPage = true;

        while (hasNextPage) {
            if (lastQuery == null) {
                System.out.println("Enter your recipe query:");
                lastQuery = scanner.nextLine().trim();
            }

            hasNextPage = searchRecipes(lastQuery, currentPage * RECIPES_PER_PAGE);

            if (hasNextPage) {
                System.out.println("Type 'next' to see the next " + RECIPES_PER_PAGE + " recipes, or 'bye' to exit:");
                String input = scanner.nextLine().trim().toLowerCase();

                if ("next".equals(input)) {
                    currentPage++;
                } else if ("bye".equals(input)) {
                    System.out.println("Goodbye!");
                    break;
                } else {
                    try {
                        int recipeNumber = Integer.parseInt(input);
                        if (recipeNumber >= 0 && recipeNumber < RECIPES_PER_PAGE) {
                            fetchRecipeDetails(recipeNumber, lastQuery, currentPage * RECIPES_PER_PAGE, scanner);
                        } else {
                            System.out.println("Invalid recipe number. Please try again.");
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid input. Please type 'next', 'bye', or a recipe number.");
                    }
                }
            } else {
                System.out.println("No more recipes to display. Goodbye!");
            }
        }

        scanner.close();
    }

    private static boolean searchRecipes(String query, int offset) {
        HttpUrl.Builder urlBuilder = HttpUrl.parse("https://api.spoonacular.com/recipes/search").newBuilder();
        urlBuilder.addQueryParameter("query", query);
        urlBuilder.addQueryParameter("apiKey", API_KEY);
        urlBuilder.addQueryParameter("offset", String.valueOf(offset));

        String url = urlBuilder.build().toString();

        Request request = new Request.Builder().url(url).build();

        try {
            Response response = httpClient.newCall(request).execute();
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }

            String responseBody = response.body().string();
            JSONTokener tokener = new JSONTokener(responseBody);
            JSONObject jsonResponse = new JSONObject(tokener);

            JSONArray results;
            try {
                results = jsonResponse.getJSONArray("results");
            } catch (JSONException e) {
                // Handle the JSONException
                System.err.println("Error while parsing JSON: " + e.getMessage());
                return false; // Exit the method if an exception occurs
            }

            for (int i = 0; i < results.length(); i++) {
                JSONObject recipe = results.getJSONObject(i);
                String title = recipe.getString("title");
                long id = recipe.getLong("id");
                System.out.println("[" + i + "] Title: " + title + ", ID: " + id);
            }

            return results.length() == RECIPES_PER_PAGE; // Return true if the number of recipes fetched is equal to RECIPES_PER_PAGE
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void fetchRecipeDetails(int recipeNumber, String query, int offset, Scanner scanner) {
        HttpUrl.Builder urlBuilder = HttpUrl.parse("https://api.spoonacular.com/recipes/search").newBuilder();
        urlBuilder.addQueryParameter("query", query);
        urlBuilder.addQueryParameter("apiKey", API_KEY);
        urlBuilder.addQueryParameter("offset", String.valueOf(offset));

        String url = urlBuilder.build().toString();

        Request request = new Request.Builder().url(url).build();

        try {
            Response response = httpClient.newCall(request).execute();
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }

            String responseBody = response.body().string();
            JSONTokener tokener = new JSONTokener(responseBody);
            JSONObject jsonResponse = new JSONObject(tokener);

            JSONArray results = jsonResponse.getJSONArray("results");

            if (recipeNumber >= results.length()) {
                System.out.println("Invalid recipe number. Please try again.");
                return;
            }

            JSONObject recipe = results.getJSONObject(recipeNumber);
            long id = recipe.getLong("id");

            url = "https://api.spoonacular.com/recipes/" + id + "/information?apiKey=" + API_KEY;

            request = new Request.Builder().url(url).build();

            response = httpClient.newCall(request).execute();
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }

            responseBody = response.body().string();
            tokener = new JSONTokener(responseBody);
            jsonResponse = new JSONObject(tokener);

            // Parse and display recipe details
            String title = jsonResponse.getString("title");
            int readyInMinutes = jsonResponse.getInt("readyInMinutes");
            int servings = jsonResponse.getInt("servings");

            // Display ingredients
            JSONArray ingredientsArray = jsonResponse.getJSONArray("extendedIngredients");
            System.out.println("Ingredients for " + title + ":");
            for (int i = 0; i < ingredientsArray.length(); i++) {
                JSONObject ingredient = ingredientsArray.getJSONObject(i);
                String ingredientName = ingredient.getString("original");
                System.out.println("- " + ingredientName);
            }

            String instructions = jsonResponse.getString("instructions")
                    .replaceAll("<[^>]*>", "")  // Remove HTML tags
                    .replaceAll("\\.\\s+", ".\n")  // Add a line break after every sentence
                    .replaceAll("\\s*\\n\\s*", "\n");  // Replace multiple newlines with a single newline

            System.out.println("\nInstructions for " + title + ":");
            System.out.println("Ready in: " + readyInMinutes + " minutes");
            System.out.println("Servings: " + servings);
            System.out.println(instructions);

            // Prompt user to go back or exit
            System.out.println("\nType 'back' to go back to the recipe list, or 'bye' to exit:");
            String input = scanner.nextLine().trim().toLowerCase();

            if ("back".equals(input)) {
                return;
            } else if ("bye".equals(input)) {
                System.out.println("Goodbye!");
                System.exit(0);
            } else {
                System.out.println("Invalid input. Exiting...");
                System.exit(0);
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }
}
