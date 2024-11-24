import java.io.*;
import java.net.*;
import java.util.*;

public class LeafNode {
    private String nodeName;
    private String superPeer;
    private Map<String, FileEntry> downloadedFiles = new HashMap<>();
    private boolean pullEnabled;
    private int defaultTTR;

    private int totalQueries = 0;      // Total queries sent
    private int invalidResults = 0;   // Count of invalid query results

    public LeafNode(String nodeName, String superPeer, List<FileEntry> files) {
        this.nodeName = nodeName;
        this.superPeer = superPeer;
        for (FileEntry file : files) {
            downloadedFiles.put(file.getFileName(), file);
        }
    }

    public void initializeOwnedFiles() {
        File sharedDir = new File(nodeName + "/shared");
        if (!sharedDir.exists() || !sharedDir.isDirectory()) {
            System.out.println(nodeName + ": Shared directory not found. Creating...");
            sharedDir.mkdirs();
        }

        File[] files = sharedDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    FileEntry fileEntry = new FileEntry(file.getName(), 1, nodeName, 30000); // Default version = 1
                    downloadedFiles.put(file.getName(), fileEntry);
                    System.out.println(nodeName + ": Loaded owned file " + file.getName());
                }
            }
        }
    }

    public void registerFilesWithSuperPeer() throws IOException {
        try (Socket socket = new Socket("localhost", getPort(superPeer));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            for (String fileName : downloadedFiles.keySet()) {
                out.println("REGISTER:" + nodeName + ":" + fileName);
                System.out.println(nodeName + ": Registered file " + fileName + " with super-peer " + superPeer);
            }
        }
    }

    public List<String> getOwnedFiles() {
        return new ArrayList<>(downloadedFiles.keySet());
    }

    public void simulateQueriesAndDownloads(Random random, List<String> fileNames) {
        new Thread(() -> {
            while (true) {
                try {
                    String fileName = fileNames.get(random.nextInt(fileNames.size()));
                    searchFile(fileName);

                    Thread.sleep(1000 + random.nextInt(2000)); // Delay between queries
                    downloadFile(fileName, superPeer);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void simulateFileModifications(Random random) {
        new Thread(() -> {
            while (true) {
                try {
                    List<String> files = new ArrayList<>(downloadedFiles.keySet());
                    String fileName = files.get(random.nextInt(files.size()));

                    FileEntry file = downloadedFiles.get(fileName);
                    file.incrementVersion();
                    file.setLastModifiedTime(System.currentTimeMillis());

                    broadcastInvalidation(fileName, file.getVersion());

                    Thread.sleep(2000 + random.nextInt(5000)); // Delay between modifications
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void broadcastInvalidation(String fileName, int version) throws IOException {
        try (Socket socket = new Socket("localhost", getPort(superPeer));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            String message = "INVALIDATION:" + UUID.randomUUID().toString() + ":" + nodeName + ":" + fileName + ":" + version;
            out.println(message);
            System.out.println(nodeName + ": Broadcast invalidation for file " + fileName);
        }
    }

    public void searchFile(String fileName) throws IOException {
        totalQueries++; // Increment total queries
        try (Socket socket = new Socket("localhost", getPort(superPeer));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            String queryMessage = "QUERY:" + UUID.randomUUID().toString() + ":" + fileName;
            out.println(queryMessage);

            String response = in.readLine(); // Read the query response
            if (response != null && response.startsWith("QUERYHIT")) {
                // Response format: QUERYHIT:fileName:originLastModified:resultLastModified
                String[] parts = response.split(":");
                long originLastModified = Long.parseLong(parts[2]); // From origin server
                long resultLastModified = Long.parseLong(parts[3]); // From query result

                if (resultLastModified < originLastModified) {
                    invalidResults++; // Increment invalid results
                    System.out.println(nodeName + ": Invalid result for file " + fileName);
                } else {
                    System.out.println(nodeName + ": Valid result for file " + fileName);
                }
            }
        }
    }

    public void downloadFile(String fileName, String server) throws IOException {
        try (Socket socket = new Socket("localhost", getPort(server));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            out.println("DOWNLOAD:" + nodeName + ":" + fileName);
        }
    }

    public void printStatistics() {
        double invalidPercentage = (totalQueries == 0) ? 0 : (invalidResults * 100.0 / totalQueries);
        System.out.println(nodeName + ": Total Queries: " + totalQueries + ", Invalid Results: " + invalidResults +
                ", Invalid Percentage: " + invalidPercentage + "%");
    }

    private int getPort(String peerName) {
        return 8000 + Integer.parseInt(peerName.replaceAll("\\D+", ""));
    }

    public void setPullEnabled(boolean enabled) {
        this.pullEnabled = enabled;
    }

    public void setDefaultTTR(int ttr) {
        this.defaultTTR = ttr;
    }

    public void startPolling() {
        if (!pullEnabled) return;

        new Thread(() -> {
            while (true) {
                try {
                    pollForUpdates(); // Poll for updates
                    Thread.sleep(1000); // Polling interval: 1 second
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void pollForUpdates() throws IOException {
        for (FileEntry file : downloadedFiles.values()) {
            if (file.isExpired()) { // Check if TTR has expired
                try (Socket socket = new Socket("localhost", getPort(file.getOriginServer()))) {
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    out.println("POLL:" + nodeName + ":" + file.getFileName() + ":" + file.getVersion());

                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String response = in.readLine();

                    if (response.startsWith("VALID")) {
                        String[] parts = response.split(":");
                        int newTTR = Integer.parseInt(parts[2]);
                        file.refreshTTR(newTTR); // Update TTR
                    } else if (response.startsWith("INVALID")) {
                        file.markAsInvalid(); // Mark file as invalid
                    }
                }
            }
        }
    }
}
