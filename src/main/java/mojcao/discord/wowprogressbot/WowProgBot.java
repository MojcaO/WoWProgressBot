package mojcao.discord.wowprogressbot;

import com.google.common.util.concurrent.FutureCallback;
import de.btobastian.javacord.DiscordAPI;
import de.btobastian.javacord.Javacord;
import de.btobastian.javacord.entities.Server;
import de.btobastian.javacord.entities.User;
import de.btobastian.javacord.entities.message.Message;
import de.btobastian.javacord.entities.permissions.Role;
import de.btobastian.javacord.listener.message.MessageCreateListener;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;


/**
 * Created by Mojca on 31. 05. 2017.
 */
public class WowProgBot {

    //public WowProgBot() {
    public static void main(String[] args) {

        DiscordAPI api = Javacord.getApi("MzE1NTM3OTQyMzY4MDkyMTYx.DA-UEA.K6ZT-fU17_S_8QPklJcXynNruYM", true);
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

                            //Get HTML
                            Document doc = null;
                            try {
                                doc = Jsoup.connect(url).get();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            if (doc != null) {

                                //Mythic+ score
                                String score = doc.select(".gearscore").text();
                                int from = score.indexOf("Mythic+ Score: ");
                                int to = score.indexOf("Ach. Points");
                                score = score.substring(from+"Mythic+ Score: ".length(), to).trim();
                                //float points = Float.valueOf(score);
                                int scoreTrunc = (Double.valueOf(score).intValue()/100)*100;

                                message.reply(score);
                                message.reply(String.valueOf(scoreTrunc));


                                //Faction
                                String faction = "horde";
                                if (doc.select(".guild").hasClass("alliance")) {
                                    faction = "alliance";
                                }
                                message.reply(faction);

                                //Class
                                String clas = doc.select("i>span").text();
                                message.reply(clas);

                                //Role (Tank/Healer/DPS)
                                String role = "";
                                if (doc.select("table.rating td").text().contains("*")) {
                                    role = doc.select("table.rating td").text().substring(0, 4).trim();
                                    if (role.equalsIgnoreCase("heal")) {
                                        role += "er";
                                    }
                                }
                                message.reply(role);

                                User user = message.getAuthor();
                                //TODO: Set role, reply confirmation
                                Server server = message.getChannelReceiver().getServer();
                                Collection<Role> roles = server.getRoles();

                                for (Role r : roles) {
                                    String rName = r.getName();
                                    if (rName.equalsIgnoreCase(role) || rName.equalsIgnoreCase(clas) || rName.equalsIgnoreCase(faction)) {
                                        r.addUser(user);
                                        break;
                                    }
                                }

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
}