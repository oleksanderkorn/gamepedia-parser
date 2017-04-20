import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URLDecoder;

/**
 * Author: Oleksandr Korniienko
 * Date: 4/5/17
 */
public class Parser {

    private static final Logger LOGGER = LoggerFactory.getLogger(Parser.class);

    private static final String BASE_URL_EN = "http://dota2.gamepedia.com";
    private static final String BASE_URL_RU = "http://dota2-ru.gamepedia.com";
    private static final String DOTA_2_WIKI = "/Dota_2_Wiki";
    private static final String STRATEGY_TAB_PATH = "/Guide";

    private static final String GAME_PLAY_SECTION_ID_EN = "Gameplay";
    private static final String GAME_PLAY_SECTION_ID_RU = ".D0.98.D0.B3.D1.80.D0.BE.D0.B2.D0.BE.D0.B9_.D0.BF.D1.80.D0.BE.D1.86.D0.B5.D1.81.D1.81";

    private static final String TIPS_TACTICS_SECTION_ID_EN = "Tips_.26_Tactics";
    private static final String TIPS_TACTICS_SECTION_ID_RU = ".D0.97.D0.B0.D0.BC.D0.B5.D1.82.D0.BA.D0.B8_.D0.B8_.D1.82.D0.B0.D0.BA.D1.82.D0.B8.D0.BA.D0.B8";

    private static final String TIPS_SECTION_ID_EN = "Tips";
    private static final String TIPS_SECTION_ID_RU = ".D0.97.D0.B0.D0.BC.D0.B5.D1.82.D0.BA.D0.B8_.D0.B8_.D1.82.D0.B0.D0.BA.D1.82.D0.B8.D0.BA.D0.B8";

    private static final String ITEMS_SECTION_ID_EN = "Items";
    private static final String ITEMS_SECTION_ID_RU = ".D0.9F.D1.80.D0.B5.D0.B4.D0.BC.D0.B5.D1.82.D1.8B";

    private static final String ABILITIES_SECTION_ID_EN = "Abilities";
    private static final String ABILITIES_SECTION_ID_RU = ".D0.A1.D0.BF.D0.BE.D1.81.D0.BE.D0.B1.D0.BD.D0.BE.D1.81.D1.82.D0.B8";

    private static final String GENERAL_SECTION_ID_EN = "General";
    private static final String GENERAL_SECTION_ID_RU = ".D0.9E.D1.81.D0.BD.D0.BE.D0.B2.D0.BD.D0.BE.D0.B5";

    private static final String HERO_ENTRY = ".heroentry";
    private static final String HEADER = "header";
    private static final String H2 = "<h2>%s</h2>";
    private static final String H3 = "<h3>%s</h3>";
    private static final String H4 = "<h4>%s</h4>";
    private static final String B = "<b>%s</b>";
    private static final String BR = "<br/>";
    private static final String H4_TAG = "h4";
    private static final String H2_TAG = "h2";
    private static final String A_TAG = "a";
    private static final String B_TAG = "b";
    private static final String P_TAG = "p";
    private static final String UL_TAG = "ul";
    private static final String HREF = "href";

    private static final String HEROES_JSON_EN = "src/main/resources/heroes-en.json";
    private static final String HEROES_JSON_RU = "src/main/resources/heroes-ru.json";

    private static final String HEROES_HTML_EN = "src/main/resources/heroes-en.html";
    private static final String HEROES_HTML_RU = "src/main/resources/heroes-ru.html";

    public enum Lang {
        EN,
        RU
    }

    private static Lang currentLanguage;

    public static void main(String[] args) {
        try {
            currentLanguage = Lang.EN;
            LOGGER.info("Started parsing tips for {} language.", currentLanguage);
            parseTips();
            LOGGER.info("Finished parsing tips for {} language.", currentLanguage);

            currentLanguage = Lang.RU;
            LOGGER.info("Started parsing tips for {} language.", currentLanguage);
            parseTips();
            LOGGER.info("Finished parsing tips for {} language.", currentLanguage);
        } catch (Exception e) {
            LOGGER.error("Error parsing tips for.");
        }
    }

    private static void parseTips() {
        String heroAliasUrlPath = "";
        String heroAlias;
        try {
            String baseUrl = currentLanguage.equals(Lang.EN) ? BASE_URL_EN : BASE_URL_RU;
            Document homePage = Jsoup.connect(baseUrl + DOTA_2_WIKI).get();
            Elements heroEntryElements = homePage.select(HERO_ENTRY);
            StringBuilder fullHeroTips = new StringBuilder();
            Gson gson = new Gson();
            String jsonFilePath = currentLanguage.equals(Lang.EN) ? HEROES_JSON_EN : HEROES_JSON_RU;
            JsonReader reader = new JsonReader(new FileReader(jsonFilePath));
            JsonObject heroesJson = gson.fromJson(reader, JsonObject.class);
            for (Element element : heroEntryElements) {
                heroAliasUrlPath = element.select(A_TAG).attr(HREF);
                String heroTips = parseHeroLink(baseUrl + heroAliasUrlPath + STRATEGY_TAB_PATH);
                fullHeroTips.append(heroTips).append(BR).append(BR);
                heroAlias = URLDecoder.decode(heroAliasUrlPath, "UTF-8").replace("'", "").replace("-", "").substring(1).toLowerCase();
                updateHeroTipsInJson(heroAlias, heroTips, heroesJson);
                LOGGER.info("Finished parsing hero: {}", heroAlias);
            }
            writeUpdatedJsonToFile(heroesJson);
            writeTipsToHtml(fullHeroTips.toString());
        } catch (Exception e) {
            LOGGER.error("Error parsing hero: {}", heroAliasUrlPath);
        }
    }


    private static void writeUpdatedJsonToFile(JsonObject heroesJson) {
        String jsonFilePath = currentLanguage.equals(Lang.EN) ? HEROES_JSON_EN : HEROES_JSON_RU;
        try (Writer writer = new FileWriter(jsonFilePath)) {
            Gson gson = new GsonBuilder()
                    .setPrettyPrinting()
                    .disableHtmlEscaping()
                    .create();
            gson.toJson(heroesJson, writer);
        } catch (IOException e) {
            LOGGER.error("Error writing json file.");
        }
    }

    private static void updateHeroTipsInJson(String heroAlias, String heroTips, JsonObject heroesJson) {
        try {
            if (heroesJson.get(heroAlias) != null) {
                heroesJson.get(heroAlias).getAsJsonObject().addProperty("tips", heroTips);
            } else {
                LOGGER.warn("Cannot find hero {} in the file", heroAlias);
            }
        } catch (Exception e) {
            LOGGER.error("Error updating json for hero: {}", heroAlias);
        }
    }

    private static void writeTipsToHtml(String heroTips) throws IOException {
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
        sb.append(heroTips);
        sb.append("\n</body>");
        sb.append("\n</html>");
        String htmlFilePath = currentLanguage.equals(Lang.EN) ? HEROES_HTML_EN : HEROES_HTML_RU;
        FileUtils.writeStringToFile(new File(htmlFilePath), sb.toString(), "UTF-8", false);
    }

    private static String parseHeroLink(String heroLink) {
        StringBuilder sb = new StringBuilder();
        try {
            Document heroPage = Jsoup.connect(URLDecoder.decode(heroLink, "UTF-8")).get();
            sb.append(parseGamePlaySection(heroPage));
            sb.append(parseTipsSection(heroPage));
            sb.append(parseAbilitiesSection(heroPage));
            sb.append(parseItemsSection(heroPage));
        } catch (Exception e) {
            LOGGER.error("Error parsing heroLink: {}", heroLink);
        }

        return sb.toString();
    }

    private static String parseAbilitiesSection(Document heroPage) {
        StringBuilder sb = new StringBuilder();
        try {
            String abilitiesSectionId = currentLanguage.equals(Lang.EN) ? ABILITIES_SECTION_ID_EN : ABILITIES_SECTION_ID_RU;
            Element abilitiesSection = heroPage.getElementById(abilitiesSectionId);
            sb.append(String.format(H2, abilitiesSection.text()));

            Element firstAbility = abilitiesSection.parent().nextElementSibling();
            sb.append(parseAbility(firstAbility));
            Element secondAbility = firstAbility.nextElementSibling().nextElementSibling();
            sb.append(parseAbility(secondAbility.tagName().equals(H4_TAG) ? secondAbility : secondAbility.nextElementSibling()));
            Element thirdAbility = secondAbility.nextElementSibling().nextElementSibling();
            sb.append(parseAbility(thirdAbility.tagName().equals(H4_TAG) ? thirdAbility : thirdAbility.nextElementSibling()));
            Element fourthAbility = thirdAbility.nextElementSibling().nextElementSibling();
            sb.append(parseAbility(fourthAbility.tagName().equals(H4_TAG) ? fourthAbility : fourthAbility.nextElementSibling()));
        } catch (Exception e) {
            LOGGER.error("Error parsing abilities section on page {}", heroPage.baseUri());
        }
        return sb.toString();
    }

    private static String parseAbility(Element ability) {
        StringBuilder sb = new StringBuilder();
        try {
            sb.append(String.format(H4, ability.child(0).text()));
            if (ability.nextElementSibling().tag().getName().equals(P_TAG)) {
                Element paragraph = ability.nextElementSibling();
                sb.append(paragraph.text());
                sb.append(parseListToTextWithLineBreaks(paragraph.nextElementSibling()).substring(5));
            } else {
                Element abilityList = ability.nextElementSibling();
                sb.append(parseListToTextWithLineBreaks(abilityList).substring(5));
            }
        } catch (Exception e) {
            LOGGER.error("Error parsing ability for page {}", ability.baseUri());
        }
        return sb.toString();
    }

    private static String parseGamePlaySection(Document heroPage) {
        StringBuilder sb = new StringBuilder();
        try {

            Element gamePlaySection = heroPage.getElementById(currentLanguage.equals(Lang.EN) ? GAME_PLAY_SECTION_ID_EN : GAME_PLAY_SECTION_ID_RU);
            sb.append(String.format(H2, gamePlaySection.text()));

            Element gamePlayTable = gamePlaySection.parent().nextElementSibling();
            Element playStyle = gamePlayTable.getElementsByClass(HEADER).get(0);
            sb.append(String.format(H3, playStyle.text()));

            Element playStyleInfo = playStyle.parent().nextElementSibling();
            sb.append(playStyleInfo.text());

            Element prosConsTable = playStyleInfo.nextElementSibling();
            Elements prosConsLabels = prosConsTable.getElementsByTag(B_TAG);
            Elements prosConsTexts = prosConsTable.nextElementSibling().getElementsByTag(UL_TAG);
            sb.append(BR);
            for (int i = 0; i < 2; i++) {
                sb.append(BR);
                sb.append(prosConsLabels.get(i).parent().html());
                sb.append(BR);
                Element list = prosConsTexts.get(i);
                sb.append(parseListToTextWithLineBreaks(list));
            }
        } catch (Exception e) {
            LOGGER.error("Error parsing game play page {}", heroPage.baseUri());
        }
        return sb.toString();
    }

    private static String parseTipsSection(Document heroPage) {
        StringBuilder sb = new StringBuilder();
        String tipsTacticsId = currentLanguage.equals(Lang.EN) ? TIPS_TACTICS_SECTION_ID_EN : TIPS_TACTICS_SECTION_ID_RU;
        String tipsId = currentLanguage.equals(Lang.EN) ? TIPS_SECTION_ID_EN : TIPS_SECTION_ID_RU;

        Element tipsSection = heroPage.getElementById(tipsTacticsId) != null ? heroPage.getElementById(tipsTacticsId) : heroPage.getElementById(tipsId);
        try {
            sb.append(String.format(H2, tipsSection.text()));

            String generalSectionId = currentLanguage.equals(Lang.EN) ? GENERAL_SECTION_ID_EN : GENERAL_SECTION_ID_RU;

            Element generalLabel = tipsSection.parent().nextElementSibling().getElementById(generalSectionId);
            if (generalLabel == null) {
                Element unnamedList = getNextListElement(tipsSection.parent());
                sb.append(parseListToTextWithLineBreaks(unnamedList));
                sb.append(BR);
                generalLabel = unnamedList.nextElementSibling().getElementById(generalSectionId);
            }
            if (generalLabel != null) {
                sb.append(String.format(H3, generalLabel.text()));

                Element generalList = generalLabel.parent().nextElementSibling();
                if (generalList.tagName().equals(UL_TAG)) {
                    sb.append(parseListToTextWithLineBreaks(generalList).substring(5));
                } else {
                    sb.append(generalList.text());
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error parsing tips for page: {}", heroPage.baseUri());
        }
        return sb.toString();
    }

    private static Element getNextListElement(Element element) {
        if (element.nextElementSibling().tag().getName().equals(UL_TAG)) {
            return element.nextElementSibling();
        }
        return getNextListElement(element.nextElementSibling());
    }

    private static String parseItemsSection(Document heroPage) {
        StringBuilder sb = new StringBuilder();
        try {
            String itemsSectionId = currentLanguage.equals(Lang.EN) ? ITEMS_SECTION_ID_EN : ITEMS_SECTION_ID_RU;
            Element itemsSection = heroPage.getElementById(itemsSectionId);
            sb.append(String.format(H2, itemsSection.text()));
            sb.append(parseAvailableItems(itemsSection.parent().nextElementSibling()));
        } catch (Exception e) {
            LOGGER.error("Error parsing items for page: {}", heroPage.baseUri());
        }
        return sb.toString();
    }

    private static String parseAvailableItems(Element section) {
        StringBuilder sb = new StringBuilder();
        try {
            if (section != null) {
                if (section.tagName().equals(P_TAG)) {
                    if (!section.previousElementSibling().tagName().equals(H2_TAG)) {
                        sb.append(BR);
                    }
                    sb.append(String.format(B,section.text()));
                } else if (section.tagName().equals(UL_TAG)) {
                    sb.append(BR);
                    sb.append(parseListToTextWithLineBreaks(section));
                    sb.append(BR);
                }
                if (section.nextElementSibling() != null) {
                    sb.append(parseAvailableItems(section.nextElementSibling()));
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error parsing items for page: {}", section.baseUri());
        }
        return sb.toString();
    }

    private static String parseListToTextWithLineBreaks(Element list) {
        StringBuilder content = new StringBuilder();
        for (int i = 0; i < list.children().size(); i++) {
            Element child = list.child(i);
            if (child.getElementsByTag(UL_TAG).size() > 0) {
                content.append(BR);
                Element innerList = child.getElementsByTag(UL_TAG).get(0);
                child.getElementsByTag(UL_TAG).get(0).remove();
                content.append(child.text());
                content.append(parseListToTextWithLineBreaks(innerList));
            } else {
                content.append(BR);
                content.append(child.text());
            }
            content.append(BR);
        }
        return content.toString();
    }
}
