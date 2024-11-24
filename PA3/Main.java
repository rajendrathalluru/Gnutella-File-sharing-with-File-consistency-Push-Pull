import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {
    public static void main(String[] args) throws Exception {
        ConfigParser parser = new ConfigParser();

        // Parse system configuration
        parser.parseSystemConfig("system_config.txt");

        // Parse network configuration
        parser.parseNetworkConfig("network_config.txt");

        // Retrieve configuration values
        boolean pullEnabled = parser.isPullEnabled();
        List<Integer> ttrValues = Arrays.asList(30000, 60000, 120000); // 30 sec, 1 min, 2 min TTRs

        // Get super-peer and leaf-node configurations
        Map<String, List<String>> superPeers = parser.getSuperPeers();
        Map<String, List<String>> leafNodes = parser.getLeafNodes();

        // Start super-peers
        startSuperPeers(superPeers);

        Thread.sleep(3000); // Allow super-peers to initialize

        // Test for different TTR values
        for (int ttr : ttrValues) {
            System.out.println("\n=== Testing with TTR: " + ttr / 1000 + " seconds ===");
            startLeafNodes(leafNodes, pullEnabled, ttr);

            Thread.sleep(60000); // Run each test for 1 minute
        }
    }

    private static void startSuperPeers(Map<String, List<String>> superPeers) {
        for (String superPeer : superPeers.keySet()) {
            List<String> neighbors = superPeers.get(superPeer);
            new Thread(() -> {
                try {
                    SuperPeer sp = new SuperPeer(superPeer, neighbors);
                    sp.startServer();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    private static void startLeafNodes(Map<String, List<String>> leafNodes, boolean pullEnabled, int defaultTTR) {
        Random random = new Random();
        AtomicInteger queryingNodesCount = new AtomicInteger(0);

        for (String superPeer : leafNodes.keySet()) {
            List<String> leaves = leafNodes.get(superPeer);
            for (String leaf : leaves) {
                new Thread(() -> {
                    try {
                        LeafNode ln = new LeafNode(leaf, superPeer, new ArrayList<>());
                        ln.initializeOwnedFiles();
                        ln.registerFilesWithSuperPeer();

                        if (queryingNodesCount.getAndIncrement() < 3) { // Limit to 2-3 querying nodes
                            ln.setPullEnabled(pullEnabled);
                            ln.setDefaultTTR(defaultTTR);
                            ln.startPolling(); // Start polling for updates
                            ln.simulateQueriesAndDownloads(random, ln.getOwnedFiles());

                            new Timer().schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    ln.printStatistics(); // Print statistics every 30 seconds
                                }
                            }, 30000, 30000);
                        } else {
                            ln.simulateFileModifications(random); // Remaining nodes modify files
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
            }
        }
    }
}
