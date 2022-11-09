package nl.engeserver.pronounmeta;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.MetaNode;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class EventListener implements Listener {
    private String responseContent;
    private LuckPerms api;
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        try {
            URL url = new URL("https://pronoundb.org/api/v1/lookup?platform=minecraft&id=" + event.getPlayer().getUniqueId());
            HttpURLConnection http = (HttpURLConnection) url.openConnection();
            http.setRequestMethod("GET");

            if (http.getResponseCode() == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(http.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = in .readLine()) != null) {
                    response.append(inputLine);
                } in .close();

                this.responseContent = response.toString();
            } else {
                System.out.println("GET request did not work");
            }
            http.disconnect();

            String[] array = responseContent.split("\"");
            String pronoun = switch (array[3]) {
                case "unspecified" -> "Unspecified";
                case "hh" -> "He/Him";
                case "hi" -> "He/It";
                case "hs" -> "He/She";
                case "ht" -> "He/They";
                case "ih" -> "It/Him";
                case "ii" -> "It/Its";
                case "is" -> "It/She";
                case "it" -> "It/They";
                case "shh" -> "She/He";
                case "sh" -> "She/Het";
                case "si" -> "She/It";
                case "st" -> "She/They";
                case "th" -> "They/He";
                case "ti" -> "They/It";
                case "ts" -> "They/She";
                case "tt" -> "They/Them";
                case "any" -> "Any Pronouns";
                case "other" -> "Other Pronouns";
                case "ask" -> "Ask Pronouns";
                case "avoid" -> "Avoid Pronouns";
                default -> "Error";
            };
            //TODO only edit meta if different
            RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
            if (provider != null) {
                this.api = provider.getProvider();
            }
            User user = api.getUserManager().getUser(event.getPlayer().getUniqueId());
            MetaNode node = MetaNode.builder("pronouns", pronoun).build();
            if (user != null) {
                user.data().clear(NodeType.META.predicate(mn -> mn.getMetaKey().equals("pronouns")));
                user.data().add(node);
                api.getUserManager().saveUser(user);
            }

        } catch (IOException e) {
            e.printStackTrace();
            Bukkit.broadcastMessage(event.getPlayer().getName() + "error");
        }
    }
}
