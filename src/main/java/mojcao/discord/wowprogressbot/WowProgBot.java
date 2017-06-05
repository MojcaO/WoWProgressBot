package mojcao.discord.wowprogressbot;

import com.google.common.util.concurrent.FutureCallback;
import de.btobastian.javacord.DiscordAPI;
import de.btobastian.javacord.Javacord;
import de.btobastian.javacord.entities.Server;
import de.btobastian.javacord.entities.User;
import de.btobastian.javacord.entities.message.Message;
import de.btobastian.javacord.entities.message.embed.EmbedBuilder;
import de.btobastian.javacord.entities.permissions.Role;
import de.btobastian.javacord.listener.message.MessageCreateListener;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Created by Mojca on 31. 05. 2017.
 */
public class WowProgBot {

    private static final long STARTTIME = System.currentTimeMillis();

    //public WowProgBot() {
    public static void main(String[] args) {

        DiscordAPI api = Javacord.getApi("token", true);
        //connect
        api.setWaitForServersOnStartup(false);
        api.connect(new FutureCallback<DiscordAPI>() {


            public void onSuccess(DiscordAPI api) {
                //register listener
                api.registerListener(new MessageCreateListener() {

                    public void onMessageCreate(DiscordAPI api, Message message) {

                        //check the content of the message
                        String content = message.getContent().toLowerCase();

                        //Display bot information
                        if (content.equalsIgnoreCase("!wp bot") || content.equalsIgnoreCase("!wpbot") ||
                                 content.equalsIgnoreCase("!wp info") ||content.startsWith("!wowprogressbot")) {

                            displayBotInfo(api, message);

                        //Commands to update roles
                        } else if (content.startsWith("!score ") || content.startsWith("!roles ") || content.startsWith("!wp ")) {

                            setRoles(message, content);

                        //Commands to look-up and display info
                        } else if (content.startsWith("!who ") || content.startsWith("!lookup ") || content.startsWith("!info ")) {

                            lookup(message, content);

                        }
                    }
                });
            }

            public void onFailure(Throwable t) {
                t.printStackTrace();
            }
        });
    }

    private static void setRoles(Message message, String content) {
        content = content.substring(content.indexOf(" ")).trim();
        String url = getUrl(message, content);

        //Get HTML
        Document doc = null;
        try {
            doc = Jsoup.connect(url).get();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (doc != null) {
            if (!doc.getElementsContainingText("Item Level:").isEmpty()) {

                String score = getScoreGroup(doc);
                String faction = getFaction(doc);
                String clas = getClass(doc);
                String role = getRole(doc);  //Tank/Healer/DPS

                User user = message.getAuthor();
                Server server = message.getChannelReceiver().getServer();
                Collection<Role> rolesServer = server.getRoles();

                Collection<Role> rolesUser = user.getRoles(server);
                ArrayList<Role> rolesAdd = new ArrayList<Role>();
                String rolesString = "";

                int numRoles = 0;
                for (Role r : rolesServer) {
                    String rName = r.getName();
                    if (rName.equalsIgnoreCase(role) || rName.equalsIgnoreCase(clas)
                            || rName.equalsIgnoreCase(faction) || rName.equalsIgnoreCase(score) ||
                            (rName.equalsIgnoreCase("rbg") && rolesUser.contains(r))) {

                        rolesAdd.add(r);
                        rolesString += rName + ", ";
                        numRoles++;
                        if (numRoles == 5) { //Roles for: faction, role, class, score, rbg
                            break;
                        }
                    }
                }
                if (numRoles>0) {
                    rolesString = rolesString.substring(0, rolesString.length()-2);
                }

                Role[] rolesArray = rolesAdd.toArray(new Role[numRoles]);

                server.updateRoles(user, rolesArray);



                displaySuccess(message, "Updated roles for **"+message.getAuthor().getName()+"**: "+rolesString);


            } else {
                displayError(message, "Character not found.");
            }
        } else {
            displayError(message, "Can't access https://www.wowprogress.com");
        }
    }

    private static void displayBotInfo(DiscordAPI api, Message message) {
        EmbedBuilder eb = new EmbedBuilder();

        eb.setTitle("WoWProgressBot\n");
        eb.setColor(new Color(78, 142, 32));
        eb.addField("Author: ", "Shana#3957", true);
        eb.addField("Library: ", "Javacord", true);
        //eb.addField("Servers: ", String.valueOf(api.getServers().size()), true);
        eb.addField("Uptime: ", getUptime(), true);
        eb.addField("Commands: ", "• **Lookup:** !who/!lookup/!info [name] [realm] [region] " +
                        "\n• **Update roles:** !score/!roles/!wp [name] [realm] [region]" +
                        "\n• **Bot info:** !wp info ", false);
        //eb.setUrl("@Shana#3957");
        message.reply("", eb);
    }

    private static void lookup(Message message, String content) {
        content = content.substring(content.indexOf(" ")).trim();

        String url = "https://www.wowprogress.com/character/";
        String character = "";
        String realm = "";
        String region = "eu";

        //Get WoWProgress link to character
        if (content.startsWith(url)) {
            url = content;
            String[] split = url.split("/");
            if (split[split.length-1].isEmpty()) {
                character = split[split.length-2];
                realm = split[split.length-3];
            } else {
                character = split[split.length-1];
                realm = split[split.length-2];
            }
            character = character.substring(0, 1).toUpperCase()+character.substring(1);
        } else {
            if (content.endsWith(" eu")) {
                region = "eu";
                content = content.substring(0, content.lastIndexOf(" "));
            } else if (content.endsWith(" us") || content.endsWith(" na")) {
                region = "us";
                content = content.substring(0, content.lastIndexOf(" "));
            } else if (content.endsWith(" tw")) { //Taiwan
                region = "tw";
                content = content.substring(0, content.lastIndexOf(" "));
            } else if (content.endsWith(" kr")) { //Korea
                region = "kr";
                content = content.substring(0, content.lastIndexOf(" "));
            } else {
                String regionServer = message.getChannelReceiver().getServer().getRegion().getKey();
                if (regionServer.startsWith("us")) {
                    region = "us";
                } else if (regionServer.startsWith("eu")) {
                    region = "eu";
                }
            }

            character = content.substring(0, 1).toUpperCase() + content.substring(1, content.indexOf(" "));
            realm = content.substring(content.indexOf(" ")).trim().replaceAll(" |'", "-");

            url = url + region + "/" + realm + "/" + character;
        }

        //Get HTML
        Document doc = null;
        try {
            doc = Jsoup.connect(url).get();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (doc != null) {
            if (!doc.getElementsContainingText("Item level:").isEmpty()) {

                String scoreExact = getScoreExact(doc).toString();
                String faction = getFaction(doc);
                String factionIcon;
                if (faction.equalsIgnoreCase("horde")) {
                    factionIcon = "https://worldofwarcraft.akamaized.net/static/components/Logo/Logo-horde-2a80e0466e.png";
                } else {
                    factionIcon = "https://worldofwarcraft.akamaized.net/static/components/Logo/Logo-alliance-bb36e70f5f.png";
                }

                String clas = getClass(doc);
                String role = getRole(doc);  //Tank/Healer/DPS
                String ilvl = getItemLevel(doc).toString();
                String armory = "https://" + region + ".battle.net/wow/en/character/" + realm + "/" + character;
                String logs = "https://www.warcraftlogs.com/character/" + region + "/" + realm + "/" + character;
                String guild = getGuild(doc);
                realm = getRegionRealm(doc).substring(3);
                String spec = getSpec(doc);
                String classIcon = getClassIcon(clas);
                Color classColor = getClassColor(clas);
                String progress = getRaidProgress(doc);
                String traits = getArtifactTraits(doc);

                EmbedBuilder eb = new EmbedBuilder();
                eb.setAuthor(character + " - " + realm + " (" + region.toUpperCase() + ")", url, classIcon);
                eb.setColor(classColor);
                eb.setDescription(spec + " " + clas + " (" + role + ") " + " <" + guild + ">");
                eb.addField("Item level: ", ilvl, true);
                eb.addField("Artifact traits: ", traits, true);
                eb.addField("M+ score: ", scoreExact, true);
                eb.addField("Raid progress: ", progress, true);
                /*eb.addField("WoWProgress: ", url, false);
                eb.addField("Armory: ", armory, false);
                eb.addField("WarcraftLogs: ", logs, false);*/
                eb.addField("Links: ", "[Armory]("+armory+") • [WoWProgress]("+url+") • [WarcraftLogs]("+logs+")", false);
                eb.setThumbnail(factionIcon);

                message.reply("", eb);
            } else {
                    displayError(message, "Character not found.");
            }
        } else {
            displayError(message, "Can't access https://www.wowprogress.com");
        }
    }

    private static String getRegionRealm(Document doc) {
        return doc.select("a.nav_link").text();
    }

    private static String getGuild(Document doc) {
        return doc.select("div.nav_block>a.guild>nobr").text();
    }

    private static String getRaidProgress(Document doc) {
        String progress = doc.select("#tier_300>table:last-child span").text();
        if (progress.contains("Mythic")) {
            progress = (progress.length() - progress.replace("Mythic", "").length()) / "Mythic".length() + "/10M";
        } else if (progress.contains("Heroic")) {
            progress = (progress.length() - progress.replace("Heroic", "").length()) / "Heroic".length() + "/10H";
        } else if (progress.contains("Normal")) {
            progress = (progress.length() - progress.replace("Normal", "").length()) / "Normal".length() + "/10N";
        } else {
            progress = "0/0N";
        }
        return progress;
    }

    private static String getArtifactTraits(Document doc) {
        String traits = doc.select("h2").text();
        int from = traits.indexOf("(Level")+6;
        int to = traits.indexOf(")");
        if (from>0 && to>0) {
            traits = traits.substring(from, to);
        } else {
            traits = "0";
        }
        return traits;
    }

    private static String getSpec(Document doc) {
        String spec = doc.select("table.rating td[style*='font-weight:bold']").text().trim();
        int from = spec.indexOf("(")+1;
        int to = spec.indexOf(")");
        return spec.substring(from, to);
    }

    private static String getRole(Document doc) {
        String role = doc.select("table.rating td[style*='font-weight:bold']").text().substring(0, 4).trim();
        if (role.equalsIgnoreCase("heal")) {
            role += "er";
        }
        return role;
    }

    private static String getClass(Document doc) {
        String clas = doc.select("i>span").text();
        clas = clas.substring(0, 1).toUpperCase()+clas.substring(1);
        if (clas.equalsIgnoreCase("deathknight")) {
            clas = "Death Knight";
        } else if (clas.equalsIgnoreCase("demon_hunter")) {
            clas = "Demon Hunter";
        }
        return clas;
    }

    private static String getClassIcon(String clas) {
        clas = clas.toLowerCase().replace(" ", "");
        return "http://wow.zamimg.com/images/wow/icons/medium/class_"+clas+".jpg";
    }

    private static Color getClassColor(String clas) {
        Color color;
        String s = clas.toLowerCase();
        if (s.equals("death knight")) {
            color = new Color(196, 31, 59);

        } else if (s.equals("demon hunter")) {
            color = new Color(163, 48, 201);

        } else if (s.equals("druid")) {
            color = new Color(255, 125, 10);

        } else if (s.equals("hunter")) {
            color = new Color(171, 212, 115);

        } else if (s.equals("mage")) {
            color = new Color(105, 204, 240);

        } else if (s.equals("monk")) {
            color = new Color(0, 255, 150);

        } else if (s.equals("paladin")) {
            color = new Color(245, 140, 186);

        } else if (s.equals("priest")) {
            color = new Color(255, 255, 255);

        } else if (s.equals("rogue")) {
            color = new Color(255, 245, 105);

        } else if (s.equals("shaman")) {
            color = new Color(0, 112, 222);

        } else if (s.equals("warlock")) {
            color = new Color(148, 130, 201);

        } else if (s.equals("warrior")) {
            color = new Color(199, 156, 110);

        } else {
            color = new Color(129, 123, 125);

        }
        return color;
    }

    private static String getClassCrest(String clas) {
        String crest;
        String s = clas.toLowerCase();
        if (s.equals("death knight")) {
            crest = "https://hydra-media.cursecdn.com/wow.gamepedia.com/5/5d/Death_Knight_Crest.png";

        } else if (s.equals("demon hunter")) {
            crest = "https://hydra-media.cursecdn.com/wow.gamepedia.com/thumb/0/00/Demon_Hunter_Crest.png/547px-Demon_Hunter_Crest.png";
            //https://hydra-media.cursecdn.com/wow.gamepedia.com/0/00/Demon_Hunter_Crest.png full size

        } else if (s.equals("druid")) {
            crest = "https://hydra-media.cursecdn.com/wow.gamepedia.com/e/ec/Druid_Crest.png";

        } else if (s.equals("hunter")) {
            crest = "https://hydra-media.cursecdn.com/wow.gamepedia.com/3/3e/Hunter_Crest.png";

        } else if (s.equals("mage")) {
            crest = "https://hydra-media.cursecdn.com/wow.gamepedia.com/0/04/Mage_Crest.png";

        } else if (s.equals("monk")) {
            crest = "https://hydra-media.cursecdn.com/wow.gamepedia.com/a/a4/Pandaren_Crest.png";

        } else if (s.equals("paladin")) {
            crest = "https://hydra-media.cursecdn.com/wow.gamepedia.com/8/82/Paladin_Crest.png";

        } else if (s.equals("priest")) {
            crest = "https://hydra-media.cursecdn.com/wow.gamepedia.com/5/50/Priest_Crest.png";

        } else if (s.equals("rogue")) {
            crest = "https://hydra-media.cursecdn.com/wow.gamepedia.com/0/02/Rogue_Crest.png";

        } else if (s.equals("shaman")) {
            crest = "https://hydra-media.cursecdn.com/wow.gamepedia.com/d/d2/Shaman_Crest.png";

        } else if (s.equals("warlock")) {
            crest = "https://hydra-media.cursecdn.com/wow.gamepedia.com/5/5e/Warlock_Crest.png";

        } else if (s.equals("warrior")) {
            crest = "https://hydra-media.cursecdn.com/wow.gamepedia.com/4/4f/Warrior_Crest.png";

        } else {
            crest = "";

        }
        return crest;
    }

    private static String getFaction(Document doc) {
        String faction = "horde";
        if (doc.select(".guild").hasClass("alliance")) {
            faction = "alliance";
        }
        return faction;
    }

    private static String getScoreGroup(Document doc) {
        double points = getScoreExact(doc);
        String score;

        if (points >= 2800) {
            score = "2700+"; //highest rank
        } else if (points >= 1800) {
            score = String.valueOf((Math.round(points)/100)*100)+"+";
        } else {
            score = "1800-"; //lowest rank
        }

        return score;
    }

    private static Double getScoreExact(Document doc) {
        String score = doc.select(".gearscore").text();
        int from = score.indexOf("Mythic+ Score: ");
        int to = score.indexOf("Ach. Points:");
        if (score.length() < 1 || from < 1) {
            score = "";
        } else {
            score = score.substring(from+"Mythic+ Score: ".length(), to).trim();
        }

        if (score.length()<1 || score.length()>8) {
            return 0.;
        } else {
            return Double.valueOf(score);
        }
    }

    private static Double getItemLevel(Document doc) {
        String ilvl = doc.select(".gearscore").text();
        int from = ilvl.indexOf("Item Level: ");
        int to = ilvl.indexOf("Artifact Power:");
        ilvl = ilvl.substring(from+"Item Level: ".length(), to).trim();
        return Double.valueOf(ilvl);
    }

    private static String getUrl(Message message, String content) {
        String url = "https://www.wowprogress.com/character/";
        //Get WoWProgress link to character
        if (content.startsWith(url)) {
            url = content;
        } else {
            String region = "eu"; //default
            if (content.endsWith(" eu")) {
                content = content.substring(0, content.lastIndexOf(" "));
            } else if (content.endsWith(" us") || content.endsWith(" na")) {
                region = "us";
                content = content.substring(0, content.lastIndexOf(" "));
            } else if (content.endsWith(" tw")) { //Taiwan
                region = "tw";
                content = content.substring(0, content.lastIndexOf(" "));
            } else if (content.endsWith(" kr")) { //Korea
                region = "kr";
                content = content.substring(0, content.lastIndexOf(" "));
            } else {
                String regionServer = message.getChannelReceiver().getServer().getRegion().getKey();
                if (regionServer.startsWith("us") || regionServer.equalsIgnoreCase("brazil")) {
                    region = "us";
                } else if (regionServer.startsWith("eu") || regionServer.equalsIgnoreCase("frankfurt")
                        || regionServer.equalsIgnoreCase("london") || regionServer.equalsIgnoreCase("amsterdam")) {
                    region = "eu";
                }
            }

            String character = content.substring(0, content.indexOf(" "));
            String realm = content.substring(content.indexOf(" ")).trim().replaceAll(" |'", "-");

            url = url+region+"/"+realm+"/"+character;
        }
        return url;
    }

    private static String getUptime() {
        long millis = System.currentTimeMillis() - STARTTIME;

        long days = TimeUnit.MILLISECONDS.toDays(millis);
        millis -= TimeUnit.DAYS.toMillis(days);
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        millis -= TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        millis -= TimeUnit.MINUTES.toMillis(minutes);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);

        return days+"d "+hours+"h "+minutes+"m "+seconds+"s";
    }

    private static void displayError(Message message, String description) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle(":x: Error");
        eb.setColor(new Color(221, 46, 68));
        eb.setDescription(description);

        message.reply("", eb);
    }

    private static void displaySuccess(Message message, String description) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle(":white_check_mark: Success");
        eb.setColor(new Color(119, 178, 85));
        eb.setDescription(description);

        message.reply("", eb);
    }
}