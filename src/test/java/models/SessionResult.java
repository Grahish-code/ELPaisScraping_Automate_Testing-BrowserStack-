package models;
import java.util.List;
import java.util.Map;

public class SessionResult {
    public String sessionName;
    public String homePageScreenshot;
    public List<ArticleData> articles;
    public Map<String, Integer> wordFreq;
}