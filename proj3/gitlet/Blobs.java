package gitlet;

import java.io.File;
import java.io.Serializable;

public class Blobs implements Serializable {
    /** Directory for all blobs. */
    static final File BLOB_FOLDER = Utils.join(Main.GITLET_FOLDER, ".blobs");
    /** Name of the blob. */
    private String name;
    /** Contents of the blob. */
    private String content;
    /** Hash ID of the blob. */
    private String id;

    public Blobs(String thename, String thecontent) {
        this.name = thename;
        this.content = thecontent;
        this.id = Utils.sha1(name, content);
    }

    public String getName() {
        return this.name;
    }

    public String getContent() {
        return this.content;
    }

    public String getId() {
        return this.id;
    }

}
