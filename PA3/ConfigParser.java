import java.io.*;
import java.util.*;

public class ConfigParser {
    private boolean pushEnabled;
    private boolean pullEnabled;
    private int defaultTTR;
    private Map<String, List<String>> superPeers = new HashMap<>();
    private Map<String, List<String>> leafNodes = new HashMap<>();

    public void parseSystemConfig(String configPath) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(configPath));
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.split("#")[0].trim(); // Remove comments
            if (line.isEmpty()) continue;

            if (line.startsWith("PUSH_ENABLED")) {
                pushEnabled = Boolean.parseBoolean(line.split("=")[1].trim());
            } else if (line.startsWith("PULL_ENABLED")) {
                pullEnabled = Boolean.parseBoolean(line.split("=")[1].trim());
            } else if (line.startsWith("TTR")) {
                defaultTTR = Integer.parseInt(line.split("=")[1].trim());
            }
        }
        reader.close();
    }

    public void parseNetworkConfig(String configPath) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(configPath));
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.split("#")[0].trim(); // Remove comments
            if (line.isEmpty()) continue;

            String[] parts = line.split(":");
            if (parts.length != 2) {
                System.out.println("Skipping invalid line in network configuration: " + line);
                continue;
            }

            String key = parts[0].trim();
            List<String> values = Arrays.asList(parts[1].trim().split(","));

            if (key.endsWith("-leaves")) {
                leafNodes.put(key.replace("-leaves", ""), values);
            } else {
                superPeers.put(key, values);
            }
        }
        reader.close();
    }

    public boolean isPushEnabled() {
        return pushEnabled;
    }

    public boolean isPullEnabled() {
        return pullEnabled;
    }

    public int getDefaultTTR() {
        return defaultTTR;
    }

    public Map<String, List<String>> getSuperPeers() {
        return superPeers;
    }

    public Map<String, List<String>> getLeafNodes() {
        return leafNodes;
    }
}
