package naming;

import common.Path;

import storage.Command;
import storage.Storage;


class Node {

    Path p;
    String filename;
    boolean directory;

    public Node(Path p, String filename) {
        this.p = p;
        this.filename = filename;
        this.directory = false;
    }

    public Path getP() {
        return p;
    }

    public String getFilename() {
        return filename;
    }

    public boolean isDirectory() {
        return directory;
    }

    public void setDirectory(boolean directory) {
        this.directory = directory;
    }
}
public class Directory_tree extends Node{

    private Command commandStub;
    private Storage storageStub;

    public Command getCommandStub() {
        return commandStub;
    }

    public Storage getStorageStub() {
        return storageStub;
    }

    public Directory_tree(){
        super(new Path(),"/");
        this.directory = false;
    }
    public Directory_tree(Path p,String component,Command c,Storage s,boolean d) {
        super(p,component);
        this.commandStub = c;
        this.storageStub = s;
        directory = d;
    }





}
