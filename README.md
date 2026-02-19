El Pais Web Scraping Automation

This is an automation framework for scraping opinion articles from the El Pais website, translating them to English, 
and generating a consolidated PDF report. It runs across multiple browsers and devices in parallel using BrowserStack.

Project Structure
To keep the code clean and maintainable, I used the Page Object Model structure. The code is organized into four main packages:

models: Contains simple data classes like ArticleData and SessionResult to hold the scraped information and test results.
pages: Contains the Selenium locators and actions for the HomePage and OpinionPage. This keeps the UI logic separate from the tests.
utils: Contains helper functions for the translation API calls, saving images, and generating the final PDF report.
tests: Contains the main TestNG test script. Because of the POM structure, this file just handles the test flow and assertions.

Gemini said
Challenges and Solutions

iPhone Safari lazy loading issue
During testing, desktop and Android browsers scraped the text perfectly, but the iPhone 14 Safari browser returned blank
text for the later articles. I searched about this online and learned that Safari uses lazy loading. It doesn't actually
render the text in the DOM until you scroll down to it, so Selenium reads it as blank. To fix this, I injected a 
javascript command to scroll each article into view before extracting the text, which solved the problem.

BrowserStack dashboard showing unknown status
My tests were passing in my IDE, but the BrowserStack dashboard was marking the sessions as Unmarked or Completed 
instead of Pass or Fail. I researched this and found out that since TestNG assertions happen locally, BrowserStack 
doesn't automatically get the final test result. I looked up how to solve this and found that I needed to send the 
status manually. I added an AfterMethod teardown that checks the test result and uses a javascript executor script to 
update the dashboard status.

How to Run
(For running the Script empowered with BrowserStack Automate)
You can run the testng.xml file, which is configured with parallel="methods" and thread-count="5" to execute
the cross-browser scraping simultaneously.

(For running the Script locally)
You can run the ElpaisScraper_locally in the src/main/java file  
