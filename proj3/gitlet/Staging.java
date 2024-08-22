package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;

public class Staging implements Serializable {
    /** Directory for the stage. */
    static final File STAGING_FOLDER = Utils.join(Main.GITLET_FOLDER,
            "staging");
    /** Hashmap of all staged files. */
    private HashMap<String, String> stagedstages;
    /** Hashmap of all removed files. */
    private HashMap<String, String> removedstages;

    public Staging() {
        stagedstages = new HashMap<String, String>();
        removedstages = new HashMap<String, String>();
    }

    public HashMap<String, String> getStaged() {
        return stagedstages;
    }

    public HashMap<String, String> getRemoved() {
        return removedstages;
    }

    public void addstaged(String filename, String hashid) {
        stagedstages.put(filename, hashid);
    }

    public void addremoved(String filename, String hashid) {
        removedstages.put(filename, hashid);
    }

    public void clearstages() {
        stagedstages.clear();
        removedstages.clear();
    }
}
