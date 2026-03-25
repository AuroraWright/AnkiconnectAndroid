package com.kamwithk.ankiconnectandroid;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class Scraper {
    private final Context context;
    private final String API_URL_CORPORATE = "https://apicorporate.forvo.com/api2/v1.1/";
    private final String DEFAULT_FORVO_LANGUAGE = "ja";
    private static final String TAG = "ForvoScraper";

    public Scraper(Context context) {
        this.context = context;
    }

    public ArrayList<HashMap<String, String>> scrape(String word, String reading) throws IOException {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String language = preferences.getString("forvo_language", DEFAULT_FORVO_LANGUAGE);
        String apiKey = preferences.getString("forvo_api_key", "");

        ArrayList<HashMap<String, String>> audio_sources = new ArrayList<>();

        if (apiKey == null || apiKey.isEmpty()) {
            Log.e(TAG, "Forvo API key is missing. Set 'forvo_api_key' in SharedPreferences.");
            return audio_sources;
        }

        // 1. Try fetching exact word
        if (word != null && !word.trim().isEmpty()) {
            audio_sources = fetchFromApi(word, language, apiKey);
        }

        // 2. Try fetching reading if exact word has no audio
        if (audio_sources.isEmpty() && reading != null && !reading.trim().isEmpty()) {
            audio_sources = fetchFromApi(reading, language, apiKey);
        }

        return audio_sources;
    }

    private ArrayList<HashMap<String, String>> fetchFromApi(String term, String language, String apiKey) {
        ArrayList<HashMap<String, String>> audioSources = new ArrayList<>();

        // Get preferred countries based on the selected Forvo language
        List<String> preferredCountries = getPreferredCountries(language);

        try {
            // Encode the term to handle special characters properly
            String encodedTerm = URLEncoder.encode(strip(term), "UTF-8");
            String urlString = API_URL_CORPORATE + apiKey + "/word-pronunciations/word/" + encodedTerm + "/language/" + language + "/order/rate-desc";
            
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:85.0) Gecko/20100101 Firefox/85.0");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                JSONObject jsonResponse = new JSONObject(response.toString());
                if (jsonResponse.has("data") && jsonResponse.getJSONObject("data").has("items")) {
                    JSONArray items = jsonResponse.getJSONObject("data").getJSONArray("items");
                    List<JSONObject> pronunciations = new ArrayList<>();
                    
                    for (int i = 0; i < items.length(); i++) {
                        pronunciations.add(items.getJSONObject(i));
                    }

                    // Sort based on preferred_countries for the specific language
                    Collections.sort(pronunciations, new Comparator<JSONObject>() {
                        @Override
                        public int compare(JSONObject o1, JSONObject o2) {
                            return getIndex(o1, preferredCountries) - getIndex(o2, preferredCountries);
                        }
                    });

                    // Format output similar to Yomichan / Python schema
                    for (JSONObject pronunciation : pronunciations) {
                        String username = pronunciation.optString("username", "");
                        String sex = pronunciation.optString("sex", "");
                        String country = pronunciation.optString("country", "");
                        String pathmp3 = pronunciation.optString("pathmp3", "");

                        String genderSymbol = "";
                        if ("m".equals(sex)) genderSymbol = "♂";
                        else if ("f".equals(sex)) genderSymbol = "♀";

                        // Build name string (e.g., "Forvo (♂user, japan)")
                        String name = "Forvo (" + genderSymbol + username;
                        if (!country.isEmpty()) {
                            name += ", " + country;
                        }
                        name += ")";

                        HashMap<String, String> source = new HashMap<>();
                        source.put("name", name);
                        source.put("url", pathmp3);
                        audioSources.add(source);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error fetching data from Forvo API", e);
        }

        return audioSources;
    }

    // Returns a rank index based only on preferred countries
    private int getIndex(JSONObject pronunciation, List<String> preferredCountries) {
        String country = pronunciation.optString("country", "unknown").toLowerCase();

        int countryIndex = preferredCountries.indexOf(country);
        if (countryIndex != -1) {
            return countryIndex;
        }

        // Fallback for countries not on the preferred list
        return preferredCountries.size();
    }

    // Maps Forvo language codes to their native/primary English country names (lowercase for sorting)
    private List<String> getPreferredCountries(String language) {
        switch (language.toLowerCase()) {
            case "ar": return Arrays.asList("saudi arabia", "egypt", "united arab emirates", "iraq", "morocco");
            case "de": return Arrays.asList("germany", "austria", "switzerland");
            case "en": return Arrays.asList("united states", "united kingdom", "canada", "australia", "ireland", "new zealand");
            case "es": return Arrays.asList("spain", "mexico", "argentina", "colombia", "chile", "peru");
            case "fr": return Arrays.asList("france", "canada", "belgium", "switzerland");
            case "grc": return Arrays.asList("greece");
            case "hu": return Arrays.asList("hungary");
            case "it": return Arrays.asList("italy");
            case "ja": return Arrays.asList("japan");
            case "ko": return Arrays.asList("south korea");
            case "nl": return Arrays.asList("netherlands", "belgium");
            case "pl": return Arrays.asList("poland");
            case "pt": return Arrays.asList("portugal", "brazil");
            case "ru": return Arrays.asList("russia");
            case "sv": return Arrays.asList("sweden");
            case "tr": return Arrays.asList("turkey");
            case "tt": return Arrays.asList("russia"); // Tatarstan is primarily in Russia
            case "uk": return Arrays.asList("ukraine");
            case "yue": return Arrays.asList("hong kong", "china");
            case "zh": return Arrays.asList("china", "taiwan", "hong kong");
            default: return new ArrayList<>();
        }
    }

    // Helper method to get rid of leading/trailing spaces
    private String strip(String input) {
        return input.replaceAll("^[ \t]+|[ \t]+$", "");
    }
}
