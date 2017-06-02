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


/**
 * Created by Mojca on 31. 05. 2017.
 */
public class WowProgBot {

    //public WowProgBot() {
    public static void main(String[] args) {

        DiscordAPI api = Javacord.getApi("token", true);
        // connect
        api.setWaitForServersOnStartup(false);
        api.connect(new FutureCallback<DiscordAPI>() {


            public void onSuccess(DiscordAPI api) {
                // register listener
                api.registerListener(new MessageCreateListener() {

                    public void onMessageCreate(DiscordAPI api, Message message) {

                        // check the content of the message
                        String content = message.getContent().toLowerCase();

                        if (content.startsWith("!score") || content.startsWith("!roles") || content.startsWith("!wp")) {

                            content = content.substring(content.indexOf(" ")).trim();
                            String url = getUrl(content);

                            //Get HTML
                            Document doc = null;
                            try {
                                doc = Jsoup.connect(url).get();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            if (doc != null) {

                                String score = getScoreGroup(doc);
                                String faction = getFaction(doc);
                                String clas = WowProgBot.getClass(doc);
                                String role = getRole(doc);  //Tank/Healer/DPS

                                setRoles(message, score, faction, clas, role);

                            }

                            //TODO: catching exceptions, reply "Couldn't find"

                        } else if (content.startsWith("!who") || content.startsWith("!lookup")) {
                            content = content.substring(content.indexOf(" ")).trim();
                            String url = getUrl(content);

                            //Get HTML
                            Document doc = null;
                            try {
                                doc = Jsoup.connect(url).get();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            if (doc != null) {

                                Double scoreExact = getScoreExact(doc);
                                String faction = getFaction(doc);
                                String clas = WowProgBot.getClass(doc);
                                String role = getRole(doc);  //Tank/Healer/DPS

                                //TODO: display
                                EmbedBuilder eb = new EmbedBuilder();


                                /*
                                EmbedBuilder e1 = new EmbedBuilder();
                                e1.setAuthor("Mr Smith");
                                e1.setTitle("Title");
                                e1.addField("name", "value", true);
                                e1.addField("name2", "value2", true);
                                e1.setFooter("Footer text ... BLA BLA");
                                e1.setColor(new Color(26, 116, 47));
                                e1.setImage("https://discordapp.com/assets/dc0a6320d907631d34e6655dff176295.svg");
                                e1.setThumbnail("https://discordapp.com/assets/c6b26ba81f44b0c43697852e1e1d1420.svg");
                                e1.setDescription("This is a cool description");
                                */

                                message.reply("", eb);


                            }

                            //TODO: catching exceptions, reply "Couldn't find"
                        }
                    }
                });
            }

            public void onFailure(Throwable t) {
                t.printStackTrace();
            }
        });
    }

    private static String getRole(Document doc) {
        String role = "";
        if (doc.select("table.rating td").text().contains("*")) {
            role = doc.select("table.rating td").text().substring(0, 4).trim();
            if (role.equalsIgnoreCase("heal")) {
                role += "er";
            }
        }
        return role;
    }

    private static String getClass(Document doc) {
        return doc.select("i>span").text();
    }

    private static String getFaction(Document doc) {
        String faction = "horde";
        if (doc.select(".guild").hasClass("alliance")) {
            faction = "alliance";
        }
        return faction;
    }

    private static String getScoreGroup(Document doc) {
        //Mythic+ score
        double points = getScoreExact(doc);
        String score;

        if (points >= 2800) {
            score = "2700+"; //highest rank
        } else if (points >= 1800) {
            score = String.valueOf((Math.round(points)/100)*100)+"+";
        } else {
            score = "1800-"; //lowest rank
        }

        //message.reply(score+", exact: "+points);
        return score;
    }

    private static Double getScoreExact(Document doc) {
        //Mythic+ exact score
        String score = doc.select(".gearscore").text();
        if (score.length() < 1) {
            //TODO: message.reply("error");
        }
        int from = score.indexOf("Mythic+ Score: ");
        int to = score.indexOf("Ach. Points");
        score = score.substring(from+"Mythic+ Score: ".length(), to).trim();
        return Double.valueOf(score);
    }

    private static String getUrl(String content) {
        String url = "https://www.wowprogress.com/character/";
        //Get WoWProgress link to character
        if (content.startsWith(url)) {
            url = content;
        } else {
            String region = "eu";  //default
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
            }

            String character = content.substring(0, content.indexOf(" "));
            String realm = content.substring(content.indexOf(" ")).trim().replaceAll(" |'", "-");

            url = url+region+"/"+realm+"/"+character;
        }
        return url;
    }

    private static void setRoles(Message message, String score, String faction, String clas, String role) {
        //TODO: confirmation
        User user = message.getAuthor();
        Server server = message.getChannelReceiver().getServer();
        Collection<Role> rolesServer = server.getRoles();

        Collection<Role> rolesUser = user.getRoles(server);
        ArrayList<Role> rolesAdd = new ArrayList<Role>();

        int numRoles = 0;
        for (Role r : rolesServer) {
            String rName = r.getName();
            if (rName.equalsIgnoreCase(role) || rName.equalsIgnoreCase(clas)
                    || rName.equalsIgnoreCase(faction) || rName.equalsIgnoreCase(score) ||
                    (rName.equalsIgnoreCase("rbg") && rolesUser.contains(r))) {

                rolesAdd.add(r);
                numRoles++;
                if (numRoles == 5) { //Roles for: faction, role, class, score, rbg
                    break;
                }
            }
        }
        Role[] rolesArray = rolesAdd.toArray(new Role[numRoles]);

        server.updateRoles(user, rolesArray);
    }
}