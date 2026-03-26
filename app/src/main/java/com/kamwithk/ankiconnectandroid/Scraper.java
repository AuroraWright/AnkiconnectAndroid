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
            case "ab": return Arrays.asList("georgia"); // Abkhazian
            case "af": return Arrays.asList("south africa", "namibia");
            case "sq": return Arrays.asList("albania", "north macedonia", "montenegro");
            case "am": return Arrays.asList("ethiopia");
            case "ar": return Arrays.asList("saudi arabia", "egypt", "united arab emirates", "iraq", "morocco", "algeria", "syrian arab republic", "jordan", "lebanon");
            case "hy": return Arrays.asList("armenia");
            case "as": return Arrays.asList("india");
            case "ast": return Arrays.asList("spain");
            case "az": return Arrays.asList("azerbaijan", "iran (islamic republic of)");
            case "ba": return Arrays.asList("russian federation"); // Bashkir
            case "eu": return Arrays.asList("spain", "france"); // Basque
            case "be": return Arrays.asList("belarus");
            case "bn": return Arrays.asList("bangladesh", "india");
            case "bs": return Arrays.asList("bosnia and herzegovina");
            case "br": return Arrays.asList("france"); // Breton
            case "bg": return Arrays.asList("bulgaria");
            case "my": return Arrays.asList("myanmar");
            case "yue": return Arrays.asList("hong kong", "macao", "china");
            case "ca": return Arrays.asList("spain", "andorra", "france", "italy");
            case "ceb": return Arrays.asList("philippines"); // Cebuano
            case "ny": return Arrays.asList("malawi", "zambia", "mozambique"); // Chichewa
            case "kw": return Arrays.asList("united kingdom of great britain and northern ireland"); // Cornish
            case "co": return Arrays.asList("france"); // Corsican
            case "hr": return Arrays.asList("croatia", "bosnia and herzegovina");
            case "cs": return Arrays.asList("czechia");
            case "da": return Arrays.asList("denmark");
            case "prs": return Arrays.asList("afghanistan"); // Dari
            case "dv": return Arrays.asList("maldives"); // Divehi
            case "nl": return Arrays.asList("netherlands", "belgium", "suriname");
            case "dz": return Arrays.asList("bhutan"); // Dzongkha
            case "arz": return Arrays.asList("egypt"); // Egyptian Arabic
            case "en": return Arrays.asList("united states of america", "united kingdom of great britain and northern ireland", "canada", "australia", "ireland", "new zealand", "south africa");
            case "eo": return Arrays.asList("any (pick best rated pronunciation)"); // Esperanto
            case "et": return Arrays.asList("estonia");
            case "ee": return Arrays.asList("ghana", "togo"); // Ewe
            case "fo": return Arrays.asList("faroe islands", "denmark");
            case "fj": return Arrays.asList("fiji");
            case "fi": return Arrays.asList("finland");
            case "fr": return Arrays.asList("france", "canada", "belgium", "switzerland", "côte d'ivoire", "senegal", "cameroon");
            case "gl": return Arrays.asList("spain"); // Galician
            case "ka": return Arrays.asList("georgia");
            case "de": return Arrays.asList("germany", "austria", "switzerland", "liechtenstein", "belgium");
            case "el": return Arrays.asList("greece", "cyprus");
            case "gn": return Arrays.asList("paraguay", "bolivia (plurinational state of)", "argentina"); // Guarani
            case "gu": return Arrays.asList("india"); // Gujarati
            case "ht": return Arrays.asList("haiti"); // Haitian Creole
            case "ha": return Arrays.asList("nigeria", "niger", "ghana"); // Hausa
            case "haw": return Arrays.asList("united states of america");
            case "he": return Arrays.asList("israel");
            case "hi": return Arrays.asList("india", "fiji", "nepal");
            case "hu": return Arrays.asList("hungary", "romania", "slovakia");
            case "is": return Arrays.asList("iceland");
            case "ig": return Arrays.asList("nigeria"); // Igbo
            case "ind": return Arrays.asList("indonesia"); // Indonesian
            case "ga": return Arrays.asList("ireland", "united kingdom of great britain and northern ireland");
            case "it": return Arrays.asList("italy", "switzerland", "san marino", "holy see");
            case "ja": return Arrays.asList("japan");
            case "jv": return Arrays.asList("indonesia"); // Javanese
            case "kn": return Arrays.asList("india"); // Kannada
            case "kk": return Arrays.asList("kazakhstan");
            case "km": return Arrays.asList("cambodia"); // Khmer
            case "rw": return Arrays.asList("rwanda"); // Kinyarwanda
            case "rn": return Arrays.asList("burundi"); // Kirundi
            case "ko": return Arrays.asList("korea, republic of", "korea (democratic people's republic of)");
            case "ku": return Arrays.asList("iraq", "turkey", "iran (islamic republic of)", "syrian arab republic"); // Kurdish
            case "ky": return Arrays.asList("kyrgyzstan");
            case "lo": return Arrays.asList("lao people's democratic republic");
            case "la": return Arrays.asList("holy see", "italy"); // Latin
            case "lv": return Arrays.asList("latvia");
            case "ln": return Arrays.asList("congo, democratic republic of the", "congo"); // Lingala
            case "lt": return Arrays.asList("lithuania");
            case "lb": return Arrays.asList("luxembourg");
            case "mk": return Arrays.asList("north macedonia"); // Macedonian
            case "mg": return Arrays.asList("madagascar"); // Malagasy
            case "ms": return Arrays.asList("malaysia", "singapore", "brunei darussalam", "indonesia"); // Malay
            case "ml": return Arrays.asList("india"); // Malayalam
            case "mt": return Arrays.asList("malta");
            case "zh": return Arrays.asList("china", "taiwan", "hong kong", "singapore"); // Mandarin Chinese
            case "mi": return Arrays.asList("new zealand"); // Māori
            case "mr": return Arrays.asList("india"); // Marathi
            case "mn": return Arrays.asList("mongolia", "china");
            case "ne": return Arrays.asList("nepal", "india");
            case "no": return Arrays.asList("norway"); // Norwegian Bokmål
            case "nn": return Arrays.asList("norway"); // Norwegian Nynorsk
            case "or": return Arrays.asList("india"); // Oriya
            case "om": return Arrays.asList("ethiopia", "kenya"); // Oromo
            case "ps": return Arrays.asList("afghanistan", "pakistan"); // Pashto
            case "fa": return Arrays.asList("iran (islamic republic of)", "afghanistan", "tajikistan"); // Persian
            case "pl": return Arrays.asList("poland");
            case "pt": return Arrays.asList("brazil", "portugal", "angola", "mozambique", "cabo verde");
            case "pa": return Arrays.asList("india", "pakistan"); // Punjabi
            case "qu": return Arrays.asList("peru", "bolivia (plurinational state of)", "ecuador"); // Quechua
            case "ro": return Arrays.asList("romania", "moldova, republic of");
            case "ru": return Arrays.asList("russian federation", "belarus", "kazakhstan", "kyrgyzstan");
            case "sm": return Arrays.asList("samoa", "american samoa");
            case "sa": return Arrays.asList("india"); // Sanskrit
            case "gd": return Arrays.asList("united kingdom of great britain and northern ireland"); // Scottish Gaelic
            case "sr": return Arrays.asList("serbia", "bosnia and herzegovina", "montenegro");
            case "sn": return Arrays.asList("zimbabwe"); // Shona
            case "sd": return Arrays.asList("pakistan", "india"); // Sindhi
            case "si": return Arrays.asList("sri lanka"); // Sinhalese
            case "sk": return Arrays.asList("slovakia");
            case "sl": return Arrays.asList("slovenia");
            case "so": return Arrays.asList("somalia", "djibouti", "ethiopia", "kenya");
            case "st": return Arrays.asList("lesotho", "south africa"); // Sotho
            case "es": return Arrays.asList("spain", "mexico", "argentina", "colombia", "peru", "chile", "venezuela (bolivarian republic of)", "ecuador", "guatemala", "cuba");
            case "su": return Arrays.asList("indonesia"); // Sundanese
            case "sw": return Arrays.asList("tanzania, united republic of", "kenya", "uganda", "rwanda", "congo, democratic republic of the"); // Swahili
            case "sv": return Arrays.asList("sweden", "finland");
            case "tl": return Arrays.asList("philippines"); // Tagalog
            case "tg": return Arrays.asList("tajikistan");
            case "ta": return Arrays.asList("india", "sri lanka", "singapore", "malaysia"); // Tamil
            case "tt": return Arrays.asList("russian federation"); // Tatar
            case "te": return Arrays.asList("india"); // Telugu
            case "th": return Arrays.asList("thailand");
            case "bo": return Arrays.asList("china", "india", "nepal"); // Tibetan
            case "ti": return Arrays.asList("eritrea", "ethiopia"); // Tigrinya
            case "tr": return Arrays.asList("turkey", "cyprus");
            case "tk": return Arrays.asList("turkmenistan");
            case "uk": return Arrays.asList("ukraine");
            case "ur": return Arrays.asList("pakistan", "india");
            case "ug": return Arrays.asList("china"); // Uyghur
            case "uz": return Arrays.asList("uzbekistan");
            case "vi": return Arrays.asList("viet nam");
            case "cy": return Arrays.asList("united kingdom of great britain and northern ireland"); // Welsh
            case "wo": return Arrays.asList("senegal", "gambia", "mauritania"); // Wolof
            case "xh": return Arrays.asList("south africa"); // Xhosa
            case "yi": return Arrays.asList("israel", "united states of america"); // Yiddish
            case "yo": return Arrays.asList("nigeria", "benin", "togo"); // Yoruba
            case "zu": return Arrays.asList("south africa");
            default: return new ArrayList<>();
        }
    }

    // Helper method to get rid of leading/trailing spaces
    private String strip(String input) {
        return input.replaceAll("^[ \t]+|[ \t]+$", "");
    }
}
