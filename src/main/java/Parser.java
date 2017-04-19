import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;

/**
 * Author: Oleksandr Korniienko
 * Date: 4/5/17
 */
public class Parser {

    private static final Logger LOGGER = LoggerFactory.getLogger(Parser.class);

    private static final String BASE_URL = "http://dota2.gamepedia.com";
    private static final String DOTA_2_WIKI = "/Dota_2_Wiki";
    private static final String STRATEGY_TAB_PATH = "/Guide";
    private static final String GAME_PLAY_SECTION_ID = "Gameplay";
    private static final String TIPS_TACTICS_SECTION_ID = "Tips_.26_Tactics";
    private static final String TIPS_SECTION_ID = "Tips";
    private static final String ITEMS_SECTION_ID = "Items";
    private static final String HERO_ENTRY = ".heroentry";
    private static final String H2 = "<h2>%s</h2>";
    private static final String H3 = "<h3>%s</h3>";
    private static final String BR = "<br/>";
    private static final String HEADER = "header";
    private static final String A = "a";
    private static final String HREF = "href";
    private static final String B = "b";
    private static final String UL = "ul";
    private static final String GENERAL = "General";

    public static void main(String[] args) {
        String heroAlias = "";
        try {
            Document homePage = Jsoup.connect(BASE_URL + DOTA_2_WIKI).get();
            Elements heroEntryElements = homePage.select(HERO_ENTRY);
            StringBuilder sb = new StringBuilder();

            sb.append("<!doctype html>");
            sb.append("\n<html lang=\"en\">");
            sb.append("\n<head>");
            sb.append("\n<meta charset=\"UTF-8\">");
            sb.append("\n<meta name=\"viewport\" content=\"width=device-width, user-scalable=no, initial-scale=1.0, maximum-scale=1.0, minimum-scale=1.0\">");
            sb.append("\n<meta http-equiv=\"X-UA-Compatible\" content=\"ie=edge\">");
            sb.append("\n<title>GamePedia</title>");
            sb.append("\n</head>");
            sb.append("\n<body>");

            for (Element element : heroEntryElements) {
                heroAlias = element.select(A).attr(HREF);
                String heroTips = parseHeroLink(BASE_URL + heroAlias + STRATEGY_TAB_PATH);
                sb.append(heroTips);
                sb.append(BR);
                sb.append(BR);
            }

            sb.append("\n</body>");
            sb.append("\n</html>");
            FileUtils.writeStringToFile(new File("src/main/resources/heroTips.html"), sb.toString(), "UTF-8", false);
        } catch (IOException e) {
            LOGGER.error(String.format("Error parsing hero: %s", heroAlias));
        }
    }

    private static String parseHeroLink(String heroLink) {
        StringBuilder sb = new StringBuilder();
        try {
            Document heroPage = Jsoup.connect(URLDecoder.decode(heroLink, "UTF-8")).get();
            sb.append(parseGamePlaySection(heroPage));
            sb.append(parseTipsSection(heroPage));
            sb.append(parseItemsSection(heroPage));
        } catch (IOException e) {
            LOGGER.error(String.format("Error parsing heroLink: %s", heroLink));
        }

        return sb.toString();
    }

    private static String parseGamePlaySection(Document heroPage) {
        StringBuilder sb = new StringBuilder();
        try {
            Element gamePlaySection = heroPage.getElementById(GAME_PLAY_SECTION_ID);
            sb.append(String.format(H2, gamePlaySection.text()));

            Element gamePlayTable = gamePlaySection.parent().nextElementSibling();
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
                if (i > 0) {
                    sb.append(BR);
                }
                sb.append(prosConsLabels.get(i).parent().html());
                Element list = prosConsTexts.get(i);
                sb.append(parseListToTextWithLineBreaks(list));
            }
        } catch (Exception e) {
            LOGGER.error(String.format("Error parsing game play page %s", heroPage.baseUri()));
        }
        return sb.toString();
    }

    private static String parseTipsSection(Document heroPage) {
        StringBuilder sb = new StringBuilder();
        Element tipsSection = heroPage.getElementById(TIPS_TACTICS_SECTION_ID) != null ? heroPage.getElementById(TIPS_TACTICS_SECTION_ID) : heroPage.getElementById(TIPS_SECTION_ID);
        try {
            sb.append(String.format(H2, tipsSection.text()));

            Element generalLabel = tipsSection.parent().nextElementSibling().getElementById(GENERAL);
            if (generalLabel == null) {
                Element unnamedList = getNextListElement(tipsSection.parent());
                sb.append(parseListToTextWithLineBreaks(unnamedList));
                sb.append(BR);
                generalLabel = unnamedList.nextElementSibling().getElementById(GENERAL);
            }
            if (generalLabel != null) {
                sb.append(String.format(H3, generalLabel.text()));

                Element generalList = generalLabel.parent().nextElementSibling();
                sb.append(parseListToTextWithLineBreaks(generalList));
            }
        } catch (Exception e) {
            LOGGER.error(String.format("Error parsing tips for page: %s", heroPage.baseUri()));
        }
        return sb.toString();
    }

    private static Element getNextListElement(Element element) {
        if (element.nextElementSibling().tag().getName().equals("ul")) {
            return element.nextElementSibling();
        }
        return getNextListElement(element.nextElementSibling());
    }

    private static String parseItemsSection(Document heroPage) {
        StringBuilder sb = new StringBuilder();
        try {
            Element itemsSection = heroPage.getElementById(ITEMS_SECTION_ID);
            sb.append(String.format(H2, itemsSection.text()));
        } catch (Exception e) {
            LOGGER.error(String.format("Error parsing items for page: %s", heroPage.baseUri()));
        }
        return sb.toString();
    }

    private static String parseListToTextWithLineBreaks(Element list) {
        StringBuilder content = new StringBuilder();
        for (Element child : list.children()) {
            if (child.getElementsByTag("ul").size() > 0) {
                content.append(BR);
                for (int i = 0; i < child.textNodes().size(); i++) {
                    content.append(child.textNodes().get(i).getWholeText());
                }
                Element innerList = child.getElementsByTag("ul").get(0);
                content.append(parseListToTextWithLineBreaks(innerList));
            } else {
                content.append(BR);
                content.append(child.text());
            }
        }
        return content.toString();
    }
}
