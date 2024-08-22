package gitlet;


import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.Map;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;

public class Commands implements Serializable {
    /** A list of all commits. */
    private ArrayList<String> commits;
    /** A tree map of all branches. */
    private TreeMap<String, String> branches;
    /** A string referring to the id of the head commit. */
    private String headcommit;
    /** A string referring to the id of the head branch. */
    private String headbranch;
    /** An instance of the staging area. */
    private Staging stage;

    public Commands() {
        File pathtoprev = Utils.join(Main.GITLET_FOLDER, "variables");
        if (pathtoprev.exists()) {
            Commands prev = Utils.readObject(pathtoprev, Commands.class);
            commits = prev.commits;
            branches = prev.branches;
            headcommit = prev.headcommit;
            headbranch = prev.headbranch;
            stage = prev.stage;
        } else {
            commits = new ArrayList<String>();
            branches = new TreeMap<String, String>();
        }
    }

    public void savevariables() {
        File variables = Utils.join(Main.GITLET_FOLDER, "variables");
        Utils.writeObject(variables, this);
    }

    public Staging getStage() {
        return stage;
    }

    /** Creates a new Gitlet version-control system in the current directory.
     * This system will automatically start with one commit:
     * a commit that contains no files and has the commit message initial commit
     * (just like that, with no punctuation).
     * It will have a single branch: master,
     * which initially points to this initial commit,
     * and master will be the current branch.
     * The timestamp for this initial commit will be
     * 00:00:00 UTC, Thursday, 1 January 1970
     * in whatever format you choose for dates
     * (this is called "The (Unix) Epoch",
     * represented internally by the time 0.) Since the initial
     * commit in all repositories created by Gitlet
     * will have exactly the same content, it follows that
     * all repositories will automatically share this commit
     * (they will all have the same UID) and all
     * commits in all repositories will trace back to it. */
    public void init() {
        File git = new File(".gitlet");
        if (git.exists()) {
            System.out.println(
                    "A Gitlet version-control system "
                            + "already exists in the current directory.");
            System.exit(0);
        }
        Main.GITLET_FOLDER.mkdirs();
        Commit.COMMIT_FOLDER.mkdirs();
        Blobs.BLOB_FOLDER.mkdirs();
        Staging.STAGING_FOLDER.mkdirs();
        Commit initcommit = new Commit("initial commit",
                null, null, new HashMap<>());
        stage = new Staging();
        Utils.writeObject(Utils.join(Staging.STAGING_FOLDER, "stage"), stage);
        String commitid = initcommit.getHashid();
        headcommit = commitid;
        commits.add(commitid);
        headbranch = "master";
        branches.put("master", commitid);
        File newcommit = Utils.join(Commit.COMMIT_FOLDER, headcommit);
        Utils.writeObject(newcommit, initcommit);
        savevariables();
    }

    /** Adds a copy of the file as it currently exists to the staging area
     * (see the description of the commit command).
     * For this reason,
     * adding a file is also called staging the file for addition.
     * Staging an already-staged file overwrites the previous entry
     * in the staging area with the new contents.
     * The staging area should be somewhere in .gitlet.
     * If the current working version of the file is
     * identical to the version in the current commit,
     * do not stage it to be added,
     * and remove it from the staging area if it is already there
     * (as can happen when a file is changed, added, and then changed back).
     * The file will no longer be staged for removal (see gitlet rm),
     * if it was at the time of the command.
     * @param filename the file to be added */
    public void add(String filename) {
        File newfile = new File(filename);
        if (newfile.exists()) {
            Blobs newblob = new Blobs(filename,
                    Utils.readContentsAsString(newfile));
            String blobid = newblob.getId();
            File pathtoprevcommit = Utils.join(Commit.COMMIT_FOLDER,
                    headcommit);
            Commit prevcommit = Utils.readObject(pathtoprevcommit,
                    Commit.class);
            headcommit = prevcommit.getHashid();
            if (!prevcommit.getBlob().isEmpty()
                    && prevcommit.getBlob().get(filename) != null
                    && prevcommit.getBlob().get(filename).equals(blobid)) {
                if (stage.getRemoved().containsKey(filename)) {
                    stage.getRemoved().remove(filename);
                }
                if (stage.getStaged().containsKey(filename)) {
                    stage.getStaged().remove(filename);
                }
                Utils.writeObject(Utils.join(Staging.STAGING_FOLDER,
                        "stage"), stage);
                savevariables();
            } else {
                if (stage.getRemoved().containsKey(filename)) {
                    stage.getRemoved().remove(filename);
                }
                File pathtoblob = Utils.join(Blobs.BLOB_FOLDER,
                        newblob.getId());
                Utils.writeContents(pathtoblob, newblob.getContent());
                stage.addstaged(filename, newblob.getId());
                Utils.writeObject(Utils.join(Staging.STAGING_FOLDER,
                        "stage"), stage);
                savevariables();
            }
        } else {
            System.out.println("File does not exist.");
            System.exit(0);
        }
    }

    /** Saves a snapshot of tracked files in the current commit
     * and staging area so they can be restored at a later time,
     * creating a new commit. The commit is
     * said to be tracking the saved files.
     * By default, each commit's snapshot of files will
     * be exactly the same as its parent commit's snapshot of files;
     * it will keep versions of files exactly as they are, and not update them.
     * A commit will only update the contents of files it is
     * tracking that have been staged for addition at
     * the time of commit, in which case the commit will now
     * include the version of the file that was staged
     * instead of the version it got from its parent.
     * A commit will save and start tracking any files that
     * were staged for addition but weren't tracked by its parent.
     * Finally, files tracked in the current commit may
     * be untracked in the new commit as a result
     * being staged for removal by the rm command (below).
     *
     * Some other key points:
     * The staging area is cleared after a commit.
     * The commit command never adds, changes,
     * or removes files in the working directory
     *      (other than those in the .gitlet directory).
     *      The rm command will remove such files, as well as staging
     *      them for removal, so that they will be untracked after a commit.
     * Any changes made to files after staging for addition
     *      or removal are ignored by the commit command,
     *      which only modifies the contents of the .gitlet directory.
     *      For example, if you remove a tracked file
     *      using the Unix rm command
     *      (rather than Gitlet's command of the same name),
     *      it has no effect on the next commit,
     *      which will still contain the deleted version of the file.
     * After the commit command, the new commit
     *      is added as a new node in the commit tree.
     * The commit just made becomes the "current commit",
     *      and the head pointer now points to it.
     *      The previous head commit is this commit's parent commit.
     * Each commit should contain the date and time it was made.
     * Each commit has a log message associated with it that
     *      describes the changes to the files in the commit.
     *      This is specified by the user. The entire message
     *      should take up only one entry in the array args
     *      that is passed to main. To include multiword messages,
     *      you'll have to surround them in quotes.
     * Each commit is identified by its SHA-1 id,
     *      which must include the file (blob) references of its files,
     *      parent reference, log message, and commit time.
     * @param message message to commit */
    public void commit(String message) {
        if (stage.getRemoved().isEmpty() && stage.getStaged().isEmpty()) {
            System.out.println("No changes added to the commit.");
            System.exit(0);
        }
        if (message.equals("")) {
            System.out.println("Please enter a commit message.");
            System.exit(0);
        }
        File pathtoprevcommit = Utils.join(Commit.COMMIT_FOLDER, headcommit);
        Commit prevcommit = Utils.readObject(pathtoprevcommit, Commit.class);
        Map<String, String> blobs = prevcommit.getBlob();
        ArrayList<String> addfiles = new ArrayList<>(
                stage.getStaged().keySet());
        for (String filename: addfiles) {
            blobs.put(filename, stage.getStaged().get(filename));
        }
        for (String filename: stage.getRemoved().keySet()) {
            blobs.remove(filename);
        }
        Commit newcommit = new Commit(message, prevcommit.getHashid(),
                null, blobs);
        headcommit = newcommit.getHashid();
        File pathtocommit = Utils.join(Commit.COMMIT_FOLDER, headcommit);
        Utils.writeObject(pathtocommit, newcommit);
        commits.add(newcommit.getHashid());
        branches.put(headbranch, newcommit.getHashid());
        stage.clearstages();
        savevariables();
    }

    /** Unstage the file if it is currently staged for addition.
     * If the file is tracked in the current commit,
     * stage it for removal and remove the file
     * from the working directory if the user has not already done so
     * (do not remove it unless it is tracked in the current commit).
     * @param filename file to remove */
    public void rm(String filename) {
        File pathtoprevcommit = Utils.join(Commit.COMMIT_FOLDER, headcommit);
        Commit prevcommit = Utils.readObject(pathtoprevcommit, Commit.class);
        if (stage.getStaged().containsKey(filename)) {
            stage.getStaged().remove(filename);
            Utils.writeObject(Utils.join(Staging.STAGING_FOLDER,
                    "stage"), stage);
            savevariables();
        } else if (prevcommit.getBlob().containsKey(filename)) {
            stage.addremoved(filename, "randomhash");
            if (stage.getStaged().containsKey(filename)) {
                stage.getStaged().remove(filename);
            }
            File checkfile = Utils.join(Main.CWD, filename);
            if (checkfile.exists()) {
                Utils.restrictedDelete(checkfile);
            }
            Utils.writeObject(Utils.join(Staging.STAGING_FOLDER,
                    "stage"), stage);
            savevariables();
        } else {
            System.out.println("No reason to remove the file.");
            System.exit(0);
        }
    }

    /** Starting at the current head commit,
     * display information about each commit backwards along the commit tree
     * until the initial commit, following the first parent commit links,
     * ignoring any second parents found in merge
     * commits. (In regular Git,
     * this is what you get with git log --first-parent).
     * This set of commit nodes is called the commit's history.
     * For every node in this history,
     * the information it should display is the commit id,
     * the time the commit was made, and the commit message.
     *
     * There is a === before each commit and an empty line after it.
     * As in real Git, each entry displays the unique
     * SHA-1 id of the commit object. The timestamps displayed in
     * the commits reflect the current timezone, not UTC;
     * as a result, the timestamp for the initial commit
     * does not read Thursday, January 1st, 1970, 00:00:00,
     * but rather the equivalent Pacific Standard Time.
     * Display commits with the most recent at the top. By the way,
     * you'll find that the Java classes java.util.Date
     * and java.util.Formatter are useful for getting
     * and formatting times. Look into them instead of
     * trying to construct it manually yourself!
     *
     * For merge commits (those that have two parent commits),
     * add a line just below the first, as in
     * === commit 3e8bf1d794ca2e9ef8a4007275acf3751c7170ff
     * Merge: 4975af1 2c1ead1 Date:
     * Sat Nov 11 12:30:00 2017 -0800 Merged development into master.
     *      where the two hexadecimal numerals following "Merge:"
     *      consist of the first seven digits of the first
     *      and second parents' commit ids, in that order.
     *      The first parent is the branch you were on when you did the
     *      merge; the second is that of the merged-in branch.
     *      This is as in regular Git.*/
    public void log() {
        File pathtoprevcommit = Utils.join(Commit.COMMIT_FOLDER, headcommit);
        Commit prevcommit = Utils.readObject(pathtoprevcommit, Commit.class);
        while (prevcommit != null) {
            System.out.println("===");
            System.out.println("commit " + prevcommit.getHashid());
            if (prevcommit.getSecondparent() != null) {
                System.out.println("Merge: "
                        + prevcommit.getParent().substring(0, 7)
                        + " " + prevcommit.getSecondparent().substring(0, 7));
            }
            System.out.println("Date: " + prevcommit.getTimestamp());
            System.out.println(prevcommit.getMessage() + "\n");
            if (prevcommit.getParent() != null) {
                File pathtoparent = Utils.join(Commit.COMMIT_FOLDER,
                        prevcommit.getParent());
                Commit parent = Utils.readObject(pathtoparent, Commit.class);
                prevcommit = parent;
            } else {
                prevcommit = null;
            }
        }
    }

    /** Like log, except displays information about all commits ever made.
     * The order of the commits does not matter. */
    public void globallog() {
        for (String commitid: commits) {
            File pathtoprevcommit = Utils.join(Commit.COMMIT_FOLDER, commitid);
            Commit prevcommit = Utils.readObject(pathtoprevcommit,
                    Commit.class);
            System.out.println("===");
            System.out.println("commit " + prevcommit.getHashid());
            System.out.println("Date: " + prevcommit.getTimestamp());
            System.out.println(prevcommit.getMessage() + "\n");
        }
    }

    /** Prints out the ids of all commits that have the given commit message,
     * one per line.
     * If there are multiple such commits,
     * it prints the ids out on separate lines.
     * The commit message is a single operand;
     * to indicate a multiword message,
     * put the operand in quotation marks,
     * as for the commit command above.
     * @param  message message to find*/
    public void find(String message) {
        int counter = 0;
        for (String commitid: commits) {
            File pathtoprevcommit = Utils.join(Commit.COMMIT_FOLDER, commitid);
            Commit prevcommit = Utils.readObject(pathtoprevcommit,
                    Commit.class);
            if (prevcommit.getMessage().equals(message)) {
                System.out.println(prevcommit.getHashid());
                counter += 1;
            }
        }
        if (counter == 0) {
            System.out.println("Found no commit with that message.");
            System.exit(0);
        }
    }

    /** Displays what branches currently exist,
     * and marks the current branch with a *.
     * Also displays what files have been staged for addition or removal. */
    public void status() {
        System.out.println("=== Branches ===");
        for (String branchname: branches.keySet()) {
            if (branchname.equals(headbranch)) {
                System.out.println("*" + branchname);
            } else {
                System.out.println(branchname);
            }
        }
        System.out.println("\n" + "=== Staged Files ===");
        for (String stagedname: stage.getStaged().keySet()) {
            System.out.println(stagedname);
        }
        System.out.println("\n" + "=== Removed Files ===");
        for (String removedname: stage.getRemoved().keySet()) {
            System.out.println(removedname);
        }
        System.out.println("\n"
                + "=== Modifications Not Staged For Commit ===");
        System.out.println("\n" + "=== Untracked Files ===");
    }

    /**
     * For: java gitlet.Main checkout -- [file name]
     *      Takes the version of the file as it exists in the head commit,
     *      the front of the current branch,
     *      and puts it in the working directory,
     *      overwriting the version of the file that's already there
     *      if there is one. The new version of the file is not staged.
     * For: java gitlet.Main checkout [commit id] -- [file name]
     *      Takes the version of the file as it exists in the commit
     *      with the given id, and puts it in the working
     *      directory, overwriting the version of the file that's
     *      already there if there is one.
     *      The new version of the file is not staged.
     * For: java gitlet.Main checkout [branch name]
     *      Takes all files in the commit at the head of the given branch,
     *      and puts them in the working directory,
     *      overwriting the versions of the files that are already there
     *      if they exist. Also, at the end of this command,
     *      the given branch will now be considered the current branch (HEAD).
     *      Any files that are tracked in the current branch but are
     *      not present in the checked-out branch are deleted.
     *      The staging area is cleared, unless the checked-out branch
     *      is the current branch
     *
     * Failure Cases:
     * 1. If the file does not exist in the previous commit, abort,
     *      printing the error message File does not exist in that commit.
     * 2. If no commit with the given id exists, print
     * No commit with that id exists.
     *      Otherwise, if the file does not exist in the given commit,
     *      print the same message as for failure case 1.
     * 3. If no branch with that name exists, print No such branch exists.
     * If that branch is the current branch,
     *      print No need to checkout the current branch.
     *      If a working file is untracked in the current branch
     *      and would be overwritten by the checkout,
     *      print There is an untracked file in the way; delete it,
     *      or add and commit it first. and exit;
     *      perform this check before doing anything else.
     * @param args arguments to consider when doing checkout
     * */
    public void checkout(String[] args) {
        if (args.length == 2) {
            branchcheckout(args);
        }
        if (args.length == 3) {
            String[] newargs = new String[4];
            newargs[0] = args[0];
            newargs[1] = headcommit;
            newargs[2] = args[1];
            newargs[3] = args[2];
            checkout(newargs);
        }
        if (args.length == 4) {
            String commitid = args[1];
            String filename = args[3];
            int check = 0;
            for (String id: commits) {
                if (id.equals(commitid)) {
                    check += 1;
                }
                if (id.startsWith(commitid)) {
                    check += 1;
                    commitid = id;
                }
            }
            if (check == 0) {
                System.out.println("No commit with that id exists.");
                System.exit(0);
            }
            File pathtoprevcommit = Utils.join(Commit.COMMIT_FOLDER, commitid);
            Commit prevcommit = Utils.readObject(pathtoprevcommit,
                    Commit.class);
            Map<String, String> blobs = prevcommit.getBlob();
            if (!blobs.containsKey(filename)) {
                System.out.println("File does not exist in that commit.");
                System.exit(0);
            }
            File pathtoblob = Utils.join(Blobs.BLOB_FOLDER,
                    prevcommit.getBlob().get(filename));
            String content = Utils.readContentsAsString(pathtoblob);
            File newblob = Utils.join(Main.CWD, filename);
            Utils.writeContents(newblob, content);
            savevariables();
        }
    }

    /** Helper function for checkout of branch.
     * @param args arguments to checkout */
    public void branchcheckout(String[] args) {
        String branchname = args[1];
        if (!branches.containsKey(branchname)) {
            System.out.println("No such branch exists.");
            System.exit(0);
        } else if (branchname.equals(headbranch)) {
            System.out.println("No need to checkout the current branch.");
            System.exit(0);
        }
        Commit branchcommit = Utils.readObject(Utils.join(
                Commit.COMMIT_FOLDER,
                branches.get(branchname)), Commit.class);
        Map<String, String> branchblobs = branchcommit.getBlob();
        Commit currcommit = Utils.readObject(Utils.join(
                Commit.COMMIT_FOLDER, headcommit), Commit.class);
        Map<String, String> currblobs = currcommit.getBlob();
        List<String> cwdfiles = Utils.plainFilenamesIn(Main.CWD);
        for (String name: cwdfiles) {
            if (!currblobs.containsKey(name)
                    && !stage.getStaged().containsKey(name)
                    && branchblobs.containsKey(name)) {
                System.out.println(
                        "There is an untracked file in the way; delete it,"
                                + " or add and commit it first.");
                System.exit(0);
            }
        }
        for (String name: cwdfiles) {
            if (!branchblobs.containsKey(name)
                    && currblobs.containsKey(name)) {
                Utils.restrictedDelete(name);
            }
        }
        for (String file: branchblobs.keySet()) {
            String content = Utils.readContentsAsString(Utils.join(
                    Blobs.BLOB_FOLDER, branchblobs.get(file)));
            Utils.writeContents(Utils.join(Main.CWD, file), content);
        }
        stage.clearstages();
        headcommit = branches.get(branchname);
        headbranch = branchname;
        savevariables();
    }

    /** Creates a new branch with the given name,
     * and points it at the current head node.
     * A branch is nothing more than a name for a reference
     * (a SHA-1 identifier) to a commit node.
     * This command does NOT immediately switch to the newly created branch
     * (just as in real Git).
     * Before you ever call branch,
     * your code should be running with a default branch called "master".
     * @param branchname branch to create */
    public void branch(String branchname) {
        if (branches.containsKey(branchname)) {
            System.out.println("A branch with that name already exists.");
            System.exit(0);
        } else {
            branches.put(branchname, headcommit);
            savevariables();
        }
    }

    /** Deletes the branch with the given name.
     * This only means to delete the pointer associated with the branch;
     * it does not mean to delete all commits that were created under
     * the branch, or anything like that.
     * @param branchname branch to remove */
    public void rmbranch(String branchname) {
        if (!branches.containsKey(branchname)) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        } else if (branchname.equals(headbranch)) {
            System.out.println("Cannot remove the current branch.");
            System.exit(0);
        } else {
            branches.remove(branchname);
            savevariables();
        }
    }

    /** Checks out all the files tracked by the given commit.
     * Removes tracked files that are not present in that commit.
     * Also moves the current branch's head to that commit node.
     * See the intro for an example of what happens
     * to the head pointer after using reset.
     * The [commit id] may be abbreviated as for checkout.
     * The staging area is cleared. The command is essentially
     * checkout of an arbitrary commit that
     * also changes the current branch head.
     * @param commitid id of commit to reset to */
    public void reset(String commitid) {
        if (!commits.contains(commitid)) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }
        Commit currcommit = Utils.readObject(Utils.join(
                Commit.COMMIT_FOLDER, headcommit), Commit.class);
        Map<String, String> currblobs = currcommit.getBlob();
        Commit prevcommit = Utils.readObject(Utils.join(
                Commit.COMMIT_FOLDER, commitid), Commit.class);
        Map<String, String> prevblobs = prevcommit.getBlob();
        List<String> cwdfiles = Utils.plainFilenamesIn(Main.CWD);
        for (String file: cwdfiles) {
            if (!currblobs.containsKey(file)
                    && !stage.getStaged().containsKey(file)
                    && prevblobs.containsKey(file)) {
                System.out.println("There is an untracked file in the way; "
                        + "delete it, or add and commit it first.");
                System.exit(0);
            }
        }
        for (String file: prevblobs.keySet()) {
            String content = Utils.readContentsAsString(
                    Utils.join(Blobs.BLOB_FOLDER, prevblobs.get(file)));
            Utils.writeContents(Utils.join(Main.CWD, file), content);
        }
        for (String file: currblobs.keySet()) {
            if (!prevblobs.containsKey(file)) {
                File pathtofile = Utils.join(Main.CWD, file);
                Utils.restrictedDelete(pathtofile);
            }
        }
        branches.put(headbranch, commitid);
        headcommit = commitid;
        stage.clearstages();
        savevariables();
    }

    /** Merges files from the given branch into the current branch.
     * @param branchname branch to merge with current branch */
    public void merge(String branchname) {
        boolean isconflict = false;
        mergecheck(branchname);
        Commit currcommit = Utils.readObject(Utils.join(
                Commit.COMMIT_FOLDER, headcommit), Commit.class);
        Map<String, String> currblobs = currcommit.getBlob();
        Commit branchcommit = Utils.readObject(Utils.join(
                Commit.COMMIT_FOLDER, branches.get(branchname)), Commit.class);
        Map<String, String> branchblobs = branchcommit.getBlob();
        Commit splitpoint = getsplitpoint(currcommit, branchcommit);
        Map<String, String> splitblobs = splitpoint.getBlob();
        for (String file: currblobs.keySet()) {
            if (splitblobs.containsKey(file)
                    && branchblobs.containsKey(file)) {
                if (!splitblobs.get(file).equals(branchblobs.get(file))
                        && splitblobs.get(file).equals(currblobs.get(file))) {
                    checkout(new String[]{"checkout",
                            branches.get(branchname), "--", file});
                    add(file);
                }
                if (!splitblobs.get(file).equals(branchblobs.get(file))
                        && !splitblobs.get(file).equals(currblobs.get(file))
                        && !currblobs.get(file).
                        equals(branchblobs.get(file))) {
                    writeConflict(currblobs, file, branchblobs);
                    isconflict = true;
                }
            } else if (!branchblobs.containsKey(file)
                    && splitblobs.containsKey(file)
                    && !splitblobs.get(file).equals(currblobs.get(file))) {
                writeConflict(currblobs, file, branchblobs);
                isconflict = true;
            }
        }
        for (String file: branchblobs.keySet()) {
            if (!splitblobs.containsKey(file)
                    && !currblobs.containsKey(file)) {
                checkout(new String[]{"checkout",
                        branches.get(branchname), "--", file});
                add(file);
            }
            if (!splitblobs.containsKey(file) && currblobs.containsKey(file)
                    && !currblobs.get(file).equals(branchblobs.get(file))) {
                writeConflict(currblobs, file, branchblobs);
                isconflict = true;
            }
            if (!currblobs.containsKey(file) && splitblobs.containsKey(file)
                    && !splitblobs.get(file).equals(branchblobs.get(file))) {
                writeConflict(currblobs, file, branchblobs);
                isconflict = true;
            }
        }
        mergeremove(splitblobs, currblobs, branchblobs);
        if (isconflict) {
            System.out.println("Encountered a merge conflict.");
        }
        mergecommit(currcommit, branchcommit, branchname);
        savevariables();
    }

    public void mergeremove(Map<String, String> splitblobs, Map<String,
            String> currblobs, Map<String, String> branchblobs) {
        for (String file: splitblobs.keySet()) {
            if (currblobs.containsKey(file)) {
                if (splitblobs.get(file).equals(currblobs.get(file))
                        && !branchblobs.containsKey(file)) {
                    rm(file);
                }
            }
        }
    }

    public void mergecommit(Commit currcommit,
                            Commit branchcommit, String branchname) {
        HashMap<String, String> currblobs = new HashMap<>();
        if (currcommit.getBlob() != null) {
            currblobs.putAll(currcommit.getBlob());
        }
        currblobs.putAll(stage.getStaged());
        for (String rmfile: stage.getRemoved().keySet()) {
            currblobs.remove(rmfile);
        }
        Commit newcommit = new Commit("Merged " + branchname + " into "
                + headbranch + ".", currcommit.getHashid(),
                branchcommit.getHashid(), currblobs);
        File commitpath = Utils.join(Commit.COMMIT_FOLDER,
                newcommit.getHashid());
        Utils.writeObject(commitpath, newcommit);
        branches.put(headbranch, newcommit.getHashid());
        headcommit = branches.get(headbranch);
        commits.add(newcommit.getHashid());
        stage.clearstages();
        savevariables();
    }

    public void mergecheck(String branchname) {
        if (!stage.getStaged().isEmpty() || !stage.getRemoved().isEmpty()) {
            System.out.println("You have uncommited changes.");
            System.exit(0);
        }
        if (!branches.containsKey(branchname)) {
            System.out.println("A branch with that name does not exist");
            System.exit(0);
        }
        if (branchname.equals(headbranch)) {
            System.out.println("Cannot merge a branch with itself.");
            System.exit(0);
        }
        Commit currcommit = Utils.readObject(Utils.join(
                Commit.COMMIT_FOLDER, headcommit), Commit.class);
        Map<String, String> currblobs = currcommit.getBlob();
        Commit branchcommit = Utils.readObject(Utils.join(
                Commit.COMMIT_FOLDER, branches.get(branchname)), Commit.class);
        Map<String, String> branchblobs = branchcommit.getBlob();
        List<String> cwdfiles = Utils.plainFilenamesIn(Main.CWD);
        for (String file: cwdfiles) {
            if (!currblobs.containsKey(file)
                    && !stage.getStaged().containsKey(file)
                    && branchblobs.containsKey(file)) {
                System.out.println("There is an untracked file in the way; "
                        + "delete it, or add and commit it first.");
                System.exit(0);
            }
        }
        Commit splitpoint = getsplitpoint(currcommit, branchcommit);
        if (splitpoint.getHashid().equals(currcommit.getHashid())) {
            checkout(new String[]{"checkout", branchname});
            System.out.println("Current branch fast-forwarded.");
            System.exit(0);
        } else if (splitpoint.getHashid().equals(branchcommit.getHashid())) {
            System.out.println("Given branch is an "
                    + "ancestor of the current branch.");
            System.exit(0);
        }
    }

    public Commit getsplitpoint(Commit head, Commit branch) {
        ArrayDeque<Commit> commitsArray = new ArrayDeque<>();
        HashSet<String> commitset = new HashSet<>();
        String splitid;
        commitsArray.add(branch);
        while (!commitsArray.isEmpty()) {
            ArrayList<String> branchparents = new ArrayList<>();
            Commit newcommit = commitsArray.remove();
            commitset.add(newcommit.getHashid());
            if (branch.getParent() != null) {
                branchparents.add(branch.getParent());
                if (branch.getSecondparent() != null) {
                    branchparents.add(branch.getSecondparent());
                }
            }
            while (branchparents.size() > 0) {
                File parentpath = Utils.join(Commit.COMMIT_FOLDER,
                        branchparents.get(0));
                Commit parent = Utils.readObject(parentpath, Commit.class);
                commitsArray.add(parent);
                branch = parent;
                branchparents.remove(0);
            }
        }
        ArrayDeque<Commit> bfs = new ArrayDeque<>();
        bfs.add(head);
        while (!bfs.isEmpty()) {
            Commit newcommit = bfs.remove();
            if (commitset.contains(newcommit.getHashid())) {
                splitid = newcommit.getHashid();
                File pathtosplit = Utils.join(Commit.COMMIT_FOLDER, splitid);
                return Utils.readObject(pathtosplit, Commit.class);
            } else {
                ArrayList<String> headparents = new ArrayList<>();
                if (head.getParent() != null) {
                    headparents.add(head.getParent());
                    if (head.getSecondparent() != null) {
                        headparents.add(head.getSecondparent());
                    }
                }
                while (headparents.size() > 0) {
                    File parentpath = Utils.join(Commit.COMMIT_FOLDER,
                            headparents.get(0));
                    Commit parent = Utils.readObject(parentpath, Commit.class);
                    bfs.add(parent);
                    head = parent;
                    headparents.remove(0);
                }
            }
        }
        return null;
    }

    public void writeConflict(Map<String, String> currentblob, String filename,
                              Map<String, String> branchblob) {
        String headcontent = "";
        String branchcontent = "";
        String topline = "<<<<<<< HEAD\n";
        String middleline = "=======\n";
        String endline = ">>>>>>>\n";
        File current = Utils.join(Blobs.BLOB_FOLDER,
                currentblob.get(filename));
        if (current.exists()) {
            headcontent = Utils.readContentsAsString(current);
        }
        if (branchblob.containsKey(filename)) {
            File branch = Utils.join(Blobs.BLOB_FOLDER,
                    branchblob.get(filename));
            if (branch.exists()) {
                branchcontent = Utils.readContentsAsString(branch);
            }
        }
        String finalstring = topline + headcontent
                + middleline + branchcontent + endline;
        File pathtofile = Utils.join(Main.CWD, filename);
        Utils.writeContents(pathtofile, finalstring);
        Blobs blobfile = new Blobs(filename, finalstring);
        stage.addstaged(filename, blobfile.getId());
    }

}
