package nl.engeserver.pronounmeta;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.MetaNode;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Objects;

public class EventListener implements Listener {
    private String responseContent;
    private LuckPerms api;
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // grab pronoun from pronoundb API
        try {
            URL url = new URL("https://pronoundb.org/api/v2/lookup?platform=minecraft&ids=" + event.getPlayer().getUniqueId());
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

            // translate grabbed data to string
            if (responseContent.equals("{}")) {
                metaSet(event.getPlayer(), "Undefined");
                return;
            }
            String[] array1 = responseContent.split("\"sets\"");
            if (array1[1].equals(":{}}}")) {
                metaSet(event.getPlayer(), "Undefined");
                return;
            }
            String[] array2 = array1[1].split(":");
            String string1 = array2[2].replaceAll("[\\[\\]}]","");
            String[] array3 = string1.split("\"");
            StringBuilder pronoun = new StringBuilder();
            for (String a:array3) {
                if (a.equals(",")) {
                    pronoun.append("/");
                } else if (!a.isEmpty()) {
                    String b = a.substring(0, 1).toUpperCase() + a.substring(1);
                    pronoun.append(b);
                }
            }

            metaSet(event.getPlayer(), pronoun.toString());


        } catch (IOException e) {
            e.printStackTrace();
            Bukkit.broadcastMessage(event.getPlayer().getName() + "error");
        }
    }

    private void metaSet(Player player, String pronoun) {
        RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (provider != null) {
            this.api = provider.getProvider();
        }
        User user = api.getUserManager().getUser(player.getUniqueId());
        String pronounOld = user.getCachedData().getMetaData().getMetaValue("pronouns");

        // update pronoun if different
        if (Objects.equals(pronounOld, pronoun)) {
            System.out.println(player.getName() + " already has \"" + pronounOld + "\".");
        } else {
            MetaNode node = MetaNode.builder("pronouns", pronoun).build();
            user.data().clear(NodeType.META.predicate(mn -> mn.getMetaKey().equals("pronouns")));
            user.data().add(node);
            api.getUserManager().saveUser(user);
            System.out.println("Changed " + player.getName() + "'s pronouns from \"" + pronounOld + "\" to \"" + user.getCachedData().getMetaData().getMetaValue("pronouns") + "\"!");
        }
    }
}
