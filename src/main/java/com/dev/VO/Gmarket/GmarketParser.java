package com.dev.VO.Gmarket;

import com.dev.VO.Exception.InvalidFlagValueException;
import com.dev.VO.Parser;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.HashMap;

public class GmarketParser extends Parser {
    private static final GmarketParser GMARKET_PARSER = new GmarketParser();
    private static final int PRODUCT_NEEDS = 10;

    private GmarketParser() { }

    public static GmarketParser getInstance() { return GMARKET_PARSER; }

    @Override
    protected Document getDocument(String keyword, String sorter, String webPage) {
        Document document = null;

        try {
            document = super.getDocument(keyword, sorter, webPage);
        } catch (InvalidFlagValueException ERO) {
            System.out.println("[com.dev.VO.Gmarket.GmarketParser::getDocument]: " + ERO.getMessage());
            ERO.printStackTrace();
        }

        return document;
    }

    protected ArrayList<Element> getSearchProduct(Document document) {
        Elements elements = document.getElementsByClass("box__component-itemcard");
        ArrayList<Element> products = new ArrayList<>();

        for (int i = 0; i < PRODUCT_NEEDS; i++)
            products.add(elements.get(i));

        return products;
    }

    protected HashMap<String, String> getProductInfo(Element element) {
        HashMap<String, String> productInfo = new HashMap<>();
        Elements metaS1 = element.getElementsByClass("list-item list-item__awards");
        Elements metaS2 = element.getElementsByClass("list-item list-item__feedback-count");
        String productRatingStar = "null%";
        String productRatingCount = "(0)";

        if (metaS1.size() != 0) {
            productRatingStar = metaS1.get(0).getElementsByClass("for-a11y").get(0).text().split(" ")[1];
            productRatingStar = productRatingStar.substring(0, productRatingStar.length() - 1) + "%";
        }

        if (metaS2.size() != 0)
            productRatingCount = metaS2.get(0).getElementsByClass("text").get(0).text();

        productInfo.put("productName", element.getElementsByClass("text__item").get(0).text());
        productInfo.put("productHref", element.getElementsByClass("link__item").get(0).attr("href"));
        productInfo.put("productPrice", element.getElementsByClass("box__price-seller").get(0).getElementsByClass("text text__value").get(0).text());
        productInfo.put("productImageSrc", element.getElementsByTag("img").get(0).attr("src"));
        productInfo.put("productRatingStar", productRatingStar);
        productInfo.put("productRatingCount", productRatingCount);

        return productInfo;
    }
}
