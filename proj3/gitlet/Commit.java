package gitlet;

import java.io.File;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class Commit implements Serializable {
    /** Directory for the commits. */
    static final File COMMIT_FOLDER = Utils.join(Main.GITLET_FOLDER, "commits");
    /** Message of the commit. */
    private String message;
    /** Timestamp of the commit. */
    private String timestamp;
    /** Hash ID of the commit. */
    private String hashid;
    /** Parent of the commit. */
    private String parent;
    /** Second parent of the merge commit. */
    private String secondparent;
    /** Map of blobs of the commit. */
    private Map<String, String> blob;


    public Commit(String themessage, String theparent,
                  String thesecondparent, Map<String, String> theblob) {
        this.message = themessage;
        SimpleDateFormat timeformat = new SimpleDateFormat(
                "EEE MMM d HH:mm:ss yyyy Z");
        Date currenttime = new Date();
        this.timestamp = timeformat.format(currenttime);
        this.parent = theparent;
        this.secondparent = thesecondparent;
        this.blob = theblob;
        this.hashid = Utils.sha1((Object) Utils.serialize(this));
    }

    public String getMessage() {
        return this.message;
    }

    public String getTimestamp() {
        return this.timestamp;
    }

    public String getHashid() {
        return this.hashid;
    }

    public String getParent() {
        return this.parent;
    }

    public String getSecondparent() {
        return this.secondparent;
    }

    public Map<String, String> getBlob() {
        return this.blob;
    }

}
