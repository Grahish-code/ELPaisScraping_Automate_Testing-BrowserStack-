package utils;

import models.ArticleData;
import models.SessionResult;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;

import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class PdfReportUtil {
    private static final Logger LOGGER = Logger.getLogger(PdfReportUtil.class.getName());

    public static void generateMasterPdfReport(List<SessionResult> globalResults) {
        LOGGER.info("=== Generating Master PDF Report ===");

        try (Document document = new Document()) {
            PdfWriter.getInstance(document, new FileOutputStream("ElPais_Master_Report.pdf"));
            document.open();

            // Add Title
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            document.add(new Paragraph("El País Scraping Automation Report", titleFont));
            document.add(new Paragraph("Generated: " + LocalDateTime.now().toString()));
            document.add(new Paragraph("Total Sessions: " + globalResults.size()));
            document.add(new Paragraph("\n\n"));

            // Loop through each session result
            for (SessionResult session : globalResults) {
                // Section Header
                Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
                Paragraph p = new Paragraph("Session: " + session.sessionName, sectionFont);
                p.setSpacingBefore(20);
                document.add(p);
                document.add(new Paragraph("--------------------------------------------------"));

                // Add Home Page Screenshot
                if (session.homePageScreenshot != null) {
                    try {
                        Image img = Image.getInstance(session.homePageScreenshot);
                        img.scaleToFit(500, 400);
                        img.setAlignment(Element.ALIGN_CENTER);
                        document.add(img);
                        document.add(new Paragraph("(Home Page Screenshot)", FontFactory.getFont(FontFactory.HELVETICA, 8)));
                    } catch (Exception e) {
                        document.add(new Paragraph("[Error loading screenshot]"));
                    }
                }

                // Add Articles
                for (ArticleData article : session.articles) {
                    Paragraph a = new Paragraph("Article " + article.index + ": " + article.title);
                    a.setSpacingBefore(10);
                    document.add(a);
                    document.add(new Paragraph("Spanish: " + article.title, FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 10)));
                    document.add(new Paragraph("English Title: " + article.translatedTitle, FontFactory.getFont(FontFactory.HELVETICA, 11)));

                    Paragraph content = new Paragraph("Content: " + article.content, FontFactory.getFont(FontFactory.HELVETICA, 10));
                    content.setSpacingBefore(5);
                    document.add(content);

                    // Image Availability Check
                    String imageText = (article.imagePath != null)
                            ? "Image Source/Path: " + article.imagePath
                            : "Image not available";

                    Paragraph imagePara = new Paragraph(imageText, FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9));
                    imagePara.setSpacingBefore(5);
                    document.add(imagePara);
                }

                // Add Word Frequency Analysis
                Paragraph freqHeader = new Paragraph("Word Frequency Analysis (Repeated > 2 times):", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12));
                freqHeader.setSpacingBefore(20);
                document.add(freqHeader);

                List<Map.Entry<String, Integer>> repeated = session.wordFreq.entrySet().stream()
                        .filter(e -> e.getValue() > 2)
                        .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                        .toList();

                if (repeated.isEmpty()) {
                    document.add(new Paragraph("  No words repeated more than twice.", FontFactory.getFont(FontFactory.HELVETICA, 10)));
                } else {
                    for (Map.Entry<String, Integer> entry : repeated) {
                        document.add(new Paragraph(String.format("  • '%s': %d times", entry.getKey(), entry.getValue()), FontFactory.getFont(FontFactory.HELVETICA, 10)));
                    }
                }

                // Page break between sessions
                document.newPage();
            }

        } catch (Exception e) {
            LOGGER.severe("Failed to generate PDF: " + e.getMessage());
        } finally {
            LOGGER.info("PDF Report Generated Successfully: ElPais_Master_Report.pdf");
        }
    }
}