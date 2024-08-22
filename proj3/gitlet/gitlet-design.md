# Gitlet Design Document
author: Daniel Ngo

## 1. Classes and Data Structures

### Commands

This class stores of all the commands that gitlet can perform.

### Staging

This class keeps track of all files that have
been staged.

#### Fields
1. HashMap<String, String> stagedstages: A map of staged files and their hash id.
2. HashMap<String, String> removedstages: A map of removed files and their hash id.

### Blobs

This class creates blob objects that contain a file's name, contents, 
and hash id.

#### Fields

1. String name: The name of the file.
2. String content: The contents of the file.
3. String id: The hash id of the file.

### Commits

This class that captures all of the previous commits being made. It also keeps track of the branches, including head branch.

#### Fields
1. String logmessage: The log message given by the user.
2. String time: The timestamp of the commit.
3. String hashid: The hashcode of the commit.
4. String parentid: The id of the parent commit.
5. Blobs blob: A reference to the blobs in the commit.
6. TreeMap<String, String> branches: A map representing the branches and their respective commits.
7. String headCommit: The hash of the most recent commit file.
8. String headName: The name of the head branch.
9. String CWD: The current working directory.


## 2. Algorithms

### Commands Class
1. innit(): Creates a new Gitlet version-control system in the current directory that starts with an initial commit.
2. add(String filename): Adds a copy of filename as it currently exists to the staging area.
3. commit(String message): Saves a snapshot of tracked files in the current commit and staging area so they can be restored at a later time, creating a new commit. The staging area is cleared after a commit.
4. rm(String filename): Unstage the file if it is currently staged for addition. If the file is tracked in the current commit, stage it for removal and remove the file from the working directory if the user has not already done so (do not remove it unless it is tracked in the current commit).
5. log(): Starting at the current head commit, display information about each commit backwards along the commit tree until the initial commit, following the first parent commit links, ignoring any second parents found in merge commits.
6. global-log(): Like log, except displays information about all commits ever made. The order of the commits does not matter.
7. find(String message): Prints out the ids of all commits that have the given commit message, one per line. If there are multiple such commits, it prints the ids out on separate lines. The commit message is a single operand; to indicate a multiword message, put the operand in quotation marks, as for the commit command above.
8. status():  Displays what branches currently exist, and marks the current branch with a *. Also displays what files have been staged for addition or removal. An example of the exact format it should follow is as follows.
9. checkout(String[] args):
* if args (String filename): Takes the version of the file as it exists in the head commit, the front of the current branch, and puts it in the working directory, overwriting the version of the file that's already there if there is one. The new version of the file is not staged.
* if args (String commit id, String filename): Takes the version of the file as it exists in the commit with the given id, and puts it in the working directory, overwriting the version of the file that's already there if there is one. The new version of the file is not staged.
* if args (String branchname): Takes all files in the commit at the head of the given branch, and puts them in the working directory, overwriting the versions of the files that are already there if they exist. Also, at the end of this command, the given branch will now be considered the current branch (HEAD). Any files that are tracked in the current branch but are not present in the checked-out branch are deleted.
10. branch(String branchname): Creates a new branch with the given name, and points it at the current head node. A branch is nothing more than a name for a reference (a SHA-1 identifier) to a commit node. This command does NOT immediately switch to the newly created branch (just as in real Git). Before you ever call branch, your code should be running with a default branch called "master".
11. rm-branch(String branchname): Deletes the branch with the given name. This only means to delete the pointer associated with the branch; it does not mean to delete all commits that were created under the branch, or anything like that.
12. reset(String commitid): Checks out all the files tracked by the given commit. Removes tracked files that are not present in that commit. Also moves the current branch's head to that commit node. See the intro for an example of what happens to the head pointer after using reset. The [commit id] may be abbreviated as for checkout. The staging area is cleared. The command is essentially checkout of an arbitrary commit that also changes the current branch head.
13. merge(String branchname): Merges files from the given branch into the current branch.

### Staging Class
1. getStaged(): Returns the map of staged files.
2. getRemoved(): Returns the map of removed files.


### Blobs Class
1. getName(): Returns the name of the file.
2. getContents(): Returns the content of the file.
3. getHash(): Returns the hash id of file.

### Commits Class
1. getParentId(): Returns the id of the parent commit.
2. getMessage(): Returns the message of the commit.
3. getTime(): Returns the time of the commit.
4. getBlob(): Returns the blob of the commit.
5. getHashId(): Returns the id of the commit.

## 3. Persistence

We save the state of the commit tree after each commit by serializing the commits using their hash id's and saving them to files named after their hashid on disk. We also serialize the blobs using their hash id's and saving them to a directory in gitlet. This can be done with writeObject method from the Utils class.

## 4. Design Diagram



