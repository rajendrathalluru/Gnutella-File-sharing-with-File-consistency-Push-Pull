import java.io.*;
import java.net.*;
import java.util.*;

public class SuperPeer {
    private String nodeName;
    private List<String> neighbors;
    private Map<String, List<FileEntry>> leafNodeFiles = new HashMap<>();
    private Map<String, String> messageIdBuffer = new HashMap<>();
    private boolean pushEnabled;

    public SuperPeer(String name, List<String> neighbors) {
        this.nodeName = name;
        this.neighbors = neighbors;
    }

    public void setPushEnabled(boolean enabled) {
        this.pushEnabled = enabled;
    }

    public void startServer() throws IOException {
        ServerSocket serverSocket = new ServerSocket(getPort(nodeName));
        System.out.println(nodeName + " is running...");

        while (true) {
            Socket clientSocket = serverSocket.accept();
            new Thread(() -> handleClient(clientSocket)).start();
        }
    }

    private void handleClient(Socket socket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            String message = in.readLine();
            if (message.startsWith("REGISTER")) {
                handleRegisterRequest(message);
            } else if (message.startsWith("POLL")) {
                handlePollRequest(message, out);
            } else if (message.startsWith("INVALIDATION") && pushEnabled) {
                handleInvalidation(message);
            } else if (message.startsWith("QUERY")) {
                handleQuery(message, socket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleRegisterRequest(String message) {
        String[] parts = message.split(":");
        String leafNode = parts[1];
        String fileName = parts[2];

        leafNodeFiles.computeIfAbsent(leafNode, k -> new ArrayList<>())
                .add(new FileEntry(fileName, 1, leafNode, 30000)); // Default version = 1
        System.out.println(nodeName + ": Registered file " + fileName + " from leaf node " + leafNode);
    }

    private void handlePollRequest(String message, PrintWriter out) {
        String[] parts = message.split(":");
        String requester = parts[1];
        String fileName = parts[2];
        int clientVersion = Integer.parseInt(parts[3]);

        System.out.println(nodeName + ": Received POLL for file: " + fileName + " from " + requester);

        for (List<FileEntry> files : leafNodeFiles.values()) {
            for (FileEntry file : files) {
                if (file.getFileName().equals(fileName)) {
                    if (file.getVersion() == clientVersion) {
                        out.println("VALID:" + file.getFileName() + ":30000");
                    } else {
                        out.println("INVALID:" + file.getFileName() + ":" + file.getVersion());
                    }
                    return;
                }
            }
        }
        out.println("MISSING:" + fileName);
    }

    private void handleInvalidation(String message) {
        String[] parts = message.split(":");
        String msgId = parts[1];
        String originServer = parts[2];
        String fileName = parts[3];
        int newVersion = Integer.parseInt(parts[4]);

        if (messageIdBuffer.containsKey(msgId)) return;

        messageIdBuffer.put(msgId, originServer);

        for (Map.Entry<String, List<FileEntry>> entry : leafNodeFiles.entrySet()) {
            for (FileEntry file : entry.getValue()) {
                if (file.getFileName().equals(fileName) && file.getVersion() < newVersion) {
                    file.markAsInvalid();
                    System.out.println(nodeName + ": Invalidated " + fileName + " for leaf node " + entry.getKey());
                }
            }
        }

        for (String neighbor : neighbors) {
            propagateMessage(message, neighbor);
        }
    }

    private void handleQuery(String message, Socket socket) {
        try {
            String[] parts = message.split(":");
            String messageId = parts[1];
            String fileName = parts[2];

            // Avoid duplicate queries
            if (messageIdBuffer.containsKey(messageId)) return;
            messageIdBuffer.put(messageId, socket.getInetAddress().getHostAddress());

            // Search for the file in connected leaf nodes
            boolean found = false;
            for (Map.Entry<String, List<FileEntry>> entry : leafNodeFiles.entrySet()) {
                for (FileEntry file : entry.getValue()) {
                    if (file.getFileName().equals(fileName)) {
                        sendQueryHit(socket, file, file.getLastModifiedTime());
                        found = true;
                        break;
                    }
                }
                if (found) break;
            }

            // If not found, propagate the query to neighbors
            if (!found) {
                for (String neighbor : neighbors) {
                    propagateMessage(message, neighbor);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendQueryHit(Socket socket, FileEntry file, long originLastModifiedTime) {
        try (PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            String queryHitMessage = "QUERYHIT:" + file.getFileName() + ":" +
                    originLastModifiedTime + ":" + file.getLastModifiedTime();
            out.println(queryHitMessage);
            System.out.println(nodeName + ": Sent QUERYHIT for file " + file.getFileName());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void propagateMessage(String message, String neighbor) {
        try (Socket socket = new Socket("localhost", getPort(neighbor));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            out.println(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int getPort(String peerName) {
        return 8000 + Integer.parseInt(peerName.replaceAll("\\D+", ""));
    }
}
