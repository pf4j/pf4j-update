package ro.fortsoft.pf4j.update;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * @author Decebal Suiu
 */
public class PluginsTest {

    private static final String pluginsJson = "plugins.json";

    public static void main(String[] args) {
        FileWriter writer;
        try {
            writer = new FileWriter("downloads/" + pluginsJson);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        List<UpdateRepository.PluginInfo> plugins = new ArrayList<UpdateRepository.PluginInfo>();

        // plugin 1
        UpdateRepository.PluginInfo p1 = new UpdateRepository.PluginInfo();
        p1.id = "welcome-plugin";
        p1.description = "Welcome plugin";
        plugins.add(p1);
        // releases for plugin 1
        UpdateRepository.PluginRelease p1r1 = new UpdateRepository.PluginRelease();
        p1r1.version = "0.9.0";
        p1r1.date = new Date();
        p1r1.url = "pf4j-demo-plugin1/0.9.0/pf4j-demo-plugin1-0.9.0-SNAPSHOT.zip";
        p1.releases = Arrays.asList(p1r1);

        // plugin 2
        UpdateRepository.PluginInfo p2 = new UpdateRepository.PluginInfo();
        p2.id = "hello-plugin";
        p2.description = "Hello plugin";
        plugins.add(p2);
        // releases for plugin 2
        UpdateRepository.PluginRelease p2r1 = new UpdateRepository.PluginRelease();
        p2r1.version = "0.9.0";
        p2r1.date = new Date();
        p2r1.url = "pf4j-demo-plugin2/0.9.0/pf4j-demo-plugin2-0.9.0-SNAPSHOT.zip";
        p2.releases = Arrays.asList(p2r1);

        String json = gson.toJson(plugins);
        try {
            writer.write(json);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
