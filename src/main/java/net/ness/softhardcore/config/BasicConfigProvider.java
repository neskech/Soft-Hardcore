package net.ness.softhardcore.config;

import com.mojang.datafixers.util.Pair;

import java.util.ArrayList;
import java.util.List;

public class BasicConfigProvider implements SimpleConfig.DefaultConfig {
    private String configContents = "";
    private final List<Pair<String, Object>> configsList = new ArrayList<>();

    public final List<Pair<String, Object>> getConfigList() {
        return this.configsList;
    }

    public void addKeyValuePair(Pair<String, Object> keyValuePair, String comment) {
        configsList.add(keyValuePair);
        configContents += "# " + comment + " | default: " + keyValuePair.getSecond() + "\n";
        configContents += keyValuePair.getFirst() + "=" + keyValuePair.getSecond() + "\n\n";
    }

    @Override
    public String get(String namespace) {
        return configContents;
    }
}
