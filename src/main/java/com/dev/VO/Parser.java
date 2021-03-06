package com.dev.VO;

import com.dev.VO.Exception.InvalidFlagValueException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

import java.io.IOException;
import java.util.HashMap;

public abstract class Parser {

    // +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    // 상품 이름과 검색 옵션에 맞는 url을 설정 및
    // 쇼핑물에 접속한 후 상속받은 Parser에게 html 문서를 전달
    // +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

    private static final String WEB_DRIVE_ID = "webdriver.chrome.driver";
    private static final String WEB_DRIVER_PATH = "C:/Dev/chromedriver.exe";
    private static final int SCROLL_LIMIT = 20;
    private static final HashMap<String, String> URLS = new HashMap<>();

    public static void setUrl(String siteName, String url) {
        URLS.put(siteName, url);
    }

    public static HashMap<String, String> getUrlsHashMap() {
        return URLS;
    }

    protected Document getDocument(String keyword, String sorter, String flag) throws InvalidFlagValueException {
        Document document;
        String url;

        switch (flag) {
            case "coupang":
                url = String.format("https://www.coupang.com/np/search?component=&q=%s&channel=user%s", keyword, sorter);
                document = getPageSourceByJsoup(url);
                setUrl("coupang", url);
                break;
            case "gmarket":
                url = String.format("https://browse.gmarket.co.kr/search?keyword=%s%s", keyword, sorter);
                document = getPageSourceBySelenium(url);
                setUrl("gmarket", url);
                break;
            case "street":
                url = String.format("https://search.11st.co.kr/Search.tmall?kwd=%s%s", keyword, sorter);
                document = getPageSourceBySelenium(url);
                setUrl("street", url);
                break;
            default:
                throw new InvalidFlagValueException();
        }

        return document;
     }

    private Document getPageSourceByJsoup(String url) {
        Document document = null;

        try {
            document = Jsoup.connect(url).get();
        } catch (IOException ERO) {
            System.out.println("[TermProjectPrototype.Crawler.Parser::getPageSourceByJsoup] " + ERO.getMessage());
            ERO.printStackTrace();
        }

        return document;
    }

    private Document getPageSourceBySelenium(String url) {

        // +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
        // Jsoup은 Javascript를 핸들링할 수 없기 떄문에 Javascript로 구현된 이미지 Lazy loading을
        // Selenium이 직접 쇼핑몰에 접속한 후 직접 아래로 스크롤(이미지 로드)한 후 Selenium이 html문서를 받아옴
        // 받아온 html은 Jsoup이 파싱
        // +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

        System.setProperty(WEB_DRIVE_ID, WEB_DRIVER_PATH);

        WebDriver driver = new ChromeDriver();

        driver.get(url);

        for (int i = 10; i < SCROLL_LIMIT + 1; i++) {
            ((JavascriptExecutor) driver).executeScript(String.format("window.scrollTo(0, %d)", i * 100));

            try {
                Thread.sleep(1);
            } catch (InterruptedException ERO) {
                System.out.println("[TermProjectPrototype.Crawler.Parser::getPageSourceBySelenium] " + ERO.getMessage());
                ERO.printStackTrace();
            }
        }

        Document document = Jsoup.parse(driver.getPageSource());

        driver.close();

        return document;
    }
}
