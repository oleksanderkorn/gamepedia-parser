import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;

/**
 * Author: Oleksandr Korniienko
 * Date: 4/5/17
 */
public class Parser {

    private final static String BASE_URL = "http://dota2.gamepedia.com";
    private static final String DOTA_2_WIKI = "/Dota_2_Wiki";
    private final static String STRATEGY_TAB_PATH = "/Guide";
    private final static String GAMEPLAY_SECTION_ID = "Gameplay";
    private final static String TIPS_SECTION_ID = "Tips_.26_Tactics";
    private final static String ITEMS_SECTION_ID = "Items";
    private static final String HERO_ENTRY = ".heroentry";
    private final static String H2 = "<h2>%s</h2>";
    private final static String H3 = "<h3>%s</h3>";
    private static final String BR = "<br/>";
    private static final String HEADER = "header";
    private static final String A = "a";
    private static final String HREF = "href";
    private static final String B = "b";
    private static final String UL = "ul";


    public static void main(String[] args) {
        String heroAlias = "";
        try {
            Document homePage = Jsoup.connect(BASE_URL + DOTA_2_WIKI).get();
            Elements heroEntryElements = homePage.select(HERO_ENTRY);
            StringBuilder sb = new StringBuilder();
            for (Element element : heroEntryElements) {
                heroAlias = element.select(A).attr(HREF);
                String heroTips = parseHeroLink(BASE_URL + heroAlias + STRATEGY_TAB_PATH);
                sb.append(heroTips);
                sb.append(BR);
                sb.append(BR);
                sb.append(BR);
                sb.append(BR);
            }
            System.out.println(sb.toString());
        } catch (IOException e) {
            System.out.println(String.format("Error parsing hero: %s", heroAlias));
        }
    }

    private static String parseHeroLink(String heroLink) {
        StringBuilder sb = new StringBuilder();
        try {
            Document heroPage = Jsoup.connect(heroLink).get();
            sb.append(parseGameplay(heroPage));
            sb.append(parseTips(heroPage));
            sb.append(parseItems(heroPage));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return sb.toString();
    }

    private static String parseGameplay(Document heroPage) {
        Element gameplaySection = heroPage.getElementById(GAMEPLAY_SECTION_ID);
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(H2, gameplaySection.text()));

        Element gamePlayTable = gameplaySection.parent().nextElementSibling();
        Element playStyle = gamePlayTable.getElementsByClass(HEADER).get(0);
        sb.append(String.format(H3, playStyle.text()));

        Element playStyleInfo = playStyle.parent().nextElementSibling();
        sb.append(playStyleInfo.text());

        Element prosConsTable = playStyleInfo.nextElementSibling();
        Elements prosConsLabels = prosConsTable.getElementsByTag(B);
        Elements prosConsTexts = prosConsTable.nextElementSibling().getElementsByTag(UL);
        sb.append(BR);
        for (int i = 0; i < 2; i++) {
            sb.append(BR);
            sb.append(prosConsLabels.get(i).parent().html());
            sb.append(BR);
            Element list = prosConsTexts.get(i);
            sb.append(parseList(list));
        }
        return sb.toString();
    }

    private static String parseTips(Document heroPage) {
        StringBuilder sb = new StringBuilder();
        Element tipsSection = heroPage.getElementById(TIPS_SECTION_ID);
        sb.append(String.format(H2, tipsSection.text()));
        return sb.toString();
    }

    private static String parseItems(Document heroPage) {
        StringBuilder sb = new StringBuilder();
        Element itemsSection = heroPage.getElementById(ITEMS_SECTION_ID);
        sb.append(String.format(H2, itemsSection.text()));
        return sb.toString();
    }

    private static String parseList(Element list) {
        StringBuilder content = new StringBuilder();
        for (Element child : list.children()) {
            content.append(child.text());
            content.append(BR);
        }
        return content.toString();
    }
}
