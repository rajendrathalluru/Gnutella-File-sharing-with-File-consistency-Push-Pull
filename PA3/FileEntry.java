public class FileEntry {
    private String fileName;
    private int version;
    private String originServer;
    private long lastModifiedTime;
    private int ttr; // Time-to-refresh
    private boolean valid;

    public FileEntry(String fileName, int version, String originServer, int ttr) {
        this.fileName = fileName;
        this.version = version;
        this.originServer = originServer;
        this.ttr = ttr;
        this.valid = true;
        this.lastModifiedTime = System.currentTimeMillis(); // Set default to current time
    }

    public String getFileName() {
        return fileName;
    }

    public int getVersion() {
        return version;
    }

    public String getOriginServer() {
        return originServer;
    }

    public long getLastModifiedTime() {
        return lastModifiedTime;
    }

    public void setLastModifiedTime(long lastModifiedTime) {
        this.lastModifiedTime = lastModifiedTime;
    }

    public void incrementVersion() {
        this.version++;
        this.lastModifiedTime = System.currentTimeMillis(); // Update last modified time
    }

    public boolean isValid() {
        return valid;
    }

    public void markAsInvalid() {
        this.valid = false;
    }

    public void refreshTTR(int newTTR) {
        this.ttr = newTTR;
        this.valid = true;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() - lastModifiedTime > ttr;
    }
}
