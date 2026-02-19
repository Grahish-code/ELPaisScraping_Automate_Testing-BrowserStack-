package utils;

import models.ArticleData;
import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.util.*;
import java.util.logging.Logger;

public class ScraperUtils {
    private static final Logger LOGGER = Logger.getLogger(ScraperUtils.class.getName());

    public static String translateText(String text, String apiUrl, String apiKey, String apiHost) {
        try {
            String jsonBody = String.format("{\"from\":\"es\",\"to\":\"en\",\"json\":{\"title\":\"%s\"}}", escapeJson(text));
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .header("x-rapidapi-key", apiKey)
                    .header("x-rapidapi-host", apiHost)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                String body = response.body();
                int start = body.indexOf("\"title\":\"") + 9;
                int end = body.indexOf("\"", start);
                if (start > 9 && end > start) return body.substring(start, end);
            }
        } catch (Exception e) {
            LOGGER.warning("Translation failed: " + e.getMessage());
        }
        return text;
    }

    public static String saveImage(String imageUrl, String title, int index, String imageDir) throws IOException {
        String safeTitle = title.replaceAll("[^a-zA-Z0-9áéíóúÁÉÍÓÚñÑ ]", "").trim();
        String filename = String.format("%s/article_%02d_%s.jpg", imageDir, index, safeTitle);
        try (InputStream is = URI.create(imageUrl).toURL().openStream()) {
            Files.copy(is, Paths.get(filename), StandardCopyOption.REPLACE_EXISTING);
        }
        return filename;
    }

    public static Map<String, Integer> analyzeWordFrequency(List<ArticleData> articles) {
        Map<String, Integer> wordCount = new HashMap<>();
        for (ArticleData article : articles) {
            if (article.translatedTitle != null) {
                for (String word : article.translatedTitle.toLowerCase().split("\\s+")) {
                    word = word.replaceAll("[^a-zA-Z]", "");
                    if (word.length() > 2) wordCount.merge(word, 1, Integer::sum);
                }
            }
        }
        return wordCount;
    }

    private static String escapeJson(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}