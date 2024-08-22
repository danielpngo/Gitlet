package gitlet;


import java.io.File;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Daniel Ngo
 */
public class Main {
    /** Main directory. */
    static final File CWD = new File(".");
    /** Directory of the gitlet folder. */
    static final File GITLET_FOLDER = Utils.join(CWD, ".gitlet");

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) {
        Commands commands = new Commands();
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            System.exit(0);
        }
        switch (args[0]) {
        case "init":
            checkinput(1, args);
            commands.init();
            break;
        case "add":
            if (commands.getStage() == null) {
                System.out.println("Not in an initialized Gitlet directory.");
                System.exit(0);
            }
            checkinput(2, args);
            commands.add(args[1]);
            break;
        case "commit":
            if (commands.getStage() == null) {
                System.out.println("Not in an initialized Gitlet directory.");
                System.exit(0);
            }
            checkinput(2, args);
            commands.commit(args[1]);
            break;
        case "rm":
            if (commands.getStage() == null) {
                System.out.println("Not in an initialized Gitlet directory.");
                System.exit(0);
            }
            checkinput(2, args);
            commands.rm(args[1]);
            break;
        case "log":
            checkinput(1, args);
            commands.log();
            break;
        case "global-log":
            checkinput(1, args);
            commands.globallog();
            break;
        case "find":
            checkinput(2, args);
            commands.find(args[1]);
            break;
        case "status":
            if (commands.getStage() == null) {
                System.out.println("Not in an initialized Gitlet directory.");
                System.exit(0);
            }
            checkinput(1, args);
            commands.status();
            break;
        default:
            mainhelper(commands, args);
        }
    }

    public static void mainhelper(Commands command, String... args) {
        switch (args[0]) {
        case "checkout":
            if (command.getStage() == null) {
                System.out.println("Not in an initialized Gitlet directory.");
                System.exit(0);
            }
            if (args.length != 2 && args.length != 3 && args.length != 4) {
                System.out.println("Incorrect operands.");
                System.exit(0);
            }
            if (args.length == 4 && !args[2].equals("--") || args.length == 3
                    && !args[1].equals("--")) {
                System.out.println("Incorrect operands.");
                System.exit(0);
            }
            command.checkout(args);
            break;
        case "branch":
            checkinput(2, args);
            command.branch(args[1]);
            break;
        case "rm-branch":
            checkinput(2, args);
            command.rmbranch(args[1]);
            break;
        case "reset":
            if (command.getStage() == null) {
                System.out.println("Not in an initialized Gitlet directory.");
                System.exit(0);
            }
            checkinput(2, args);
            command.reset(args[1]);
            break;
        case "merge":
            checkinput(2, args);
            command.merge(args[1]);
            break;
        default:
            System.out.println("No command with that name exists.");
            System.exit(0);
        }
    }

    public static void checkinput(int target, String... args) {
        if (args.length == target) {
            if (target == 2) {
                if (!(args[1] instanceof String)) {
                    System.out.println("Incorrect operands.");
                    System.exit(0);
                }
            }
            return;
        } else {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
    }

}
