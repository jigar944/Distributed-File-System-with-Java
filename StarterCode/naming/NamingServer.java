package naming;

import common.Path;
import rmi.RMIException;
import rmi.Skeleton;
import storage.Command;
import storage.Storage;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Naming server.
 *
 * <p>
 * Each instance of the filesystem is centered on a single naming server. The
 * naming server maintains the filesystem directory tree. It does not store any
 * file data - this is done by separate storage servers. The primary purpose of
 * the naming server is to map each file name (path) to the storage server
 * which hosts the file's contents.
 *
 * <p>
 * The naming server provides two interfaces, <code>Service</code> and
 * <code>Registration</code>, which are accessible through RMI. Storage servers
 * use the <code>Registration</code> interface to inform the naming server of
 * their existence. Clients use the <code>Service</code> interface to perform
 * most filesystem operations. The documentation accompanying these interfaces
 * provides details on the methods supported.
 *
 * <p>
 * Stubs for accessing the naming server must typically be created by directly
 * specifying the remote network address. To make this possible, the client and
 * registration interfaces are available at well-known ports defined in
 * <code>NamingStubs</code>.
 */
public class NamingServer implements Service, Registration {

    Skeleton<Registration> registrationSkeleton;
    Skeleton<Service> serviceSkeleton;
    HashSet<Command> RegisteredServer = new HashSet<>();
    ArrayList<Directory_tree> allNodes = new ArrayList<>();
    List<File> listOffiles = new ArrayList<>();
    Hashtable<Storage,Command> stubs = new Hashtable<>();

    /**
     * Creates the naming server object.
     *
     * <p>
     * The naming server is not started.
     */
    public NamingServer() {

        InetSocketAddress portOfRegistration = new InetSocketAddress(NamingStubs.REGISTRATION_PORT);
        registrationSkeleton = new Skeleton<Registration>(Registration.class, this, portOfRegistration);

        InetSocketAddress portOfService = new InetSocketAddress(NamingStubs.SERVICE_PORT);
        serviceSkeleton = new Skeleton<Service>(Service.class, this, portOfService);

    }

    /**
     * Starts the naming server.
     *
     * <p>
     * After this method is called, it is possible to access the client and
     * registration interfaces of the naming server remotely.
     *
     * @throws RMIException If either of the two skeletons, for the client or
     *                      registration server interfaces, could not be
     *                      started. The user should not attempt to start the
     *                      server again if an exception occurs.
     */
    public synchronized void start() throws RMIException {
        try {
            this.serviceSkeleton.start();
            this.registrationSkeleton.start();
        } catch (Exception e) {
            throw new RMIException("Error while starting Registration ans Service skeletons in void start method");
        }


    }

    /**
     * Stops the naming server.
     *
     * <p>
     * This method waits for both the client and registration interface
     * skeletons to stop. It attempts to interrupt as many of the threads that
     * are executing naming server code as possible. After this method is
     * called, the naming server is no longer accessible remotely. The naming
     * server should not be restarted.
     */
    public void stop() {
        this.serviceSkeleton.stop();
        this.registrationSkeleton.stop();
        stopped(null);
    }

    /**
     * Indicates that the server has completely shut down.
     *
     * <p>
     * This method should be overridden for error reporting and application
     * exit purposes. The default implementation does nothing.
     *
     * @param cause The cause for the shutdown, or <code>null</code> if the
     *              shutdown was by explicit user request.
     */
    protected void stopped(Throwable cause) {
    }

    // The following methods are documented in Service.java.
    @Override
    public boolean isDirectory(Path path) throws FileNotFoundException {

        if (path == null)
            throw new NullPointerException("Null path provided");

        if (path.isRoot())
            return true;

        for (Directory_tree dT:allNodes) {

            //if list contains the same path
            if (dT.getP().toString().equals(path.toString()))
                return dT.isDirectory();
            String a = dT.getP().toString().replaceAll("\\\\","/");

            //if path is a substring of any list of paths then it is a directory
            if (a.contains(path+"/")){
                return true;
            }
        }
        throw new FileNotFoundException("File not found");
    }

    @Override
    public String[] list(Path directory) throws FileNotFoundException {

        Set<String> list = new HashSet<>();

        if (!isDirectory(directory))
            throw new FileNotFoundException("File not found");

        if (directory.toString() == null){
            throw new NullPointerException("Null path provided");
        }

        if (isDirectory(directory)) {

           Stream<Directory_tree> s = allNodes.stream().filter(p -> p.getP().toString().startsWith(directory.toString()));

            for (Directory_tree dt:s.collect(Collectors.toList())) {
                if (directory.isRoot()){
                    list.add(dt.getP().toString().replaceFirst(directory.toString(),"").split("^*/")[0]);
                }else {
                    list.add(dt.getP().toString().replaceFirst(directory.toString()+"/","").split("/")[0]);
                }
            }
        }

        return list.toArray(new String[0]);
    }

    @Override
    public boolean createFile(Path file)
            throws RMIException, FileNotFoundException {

        if (file.isRoot())
            return false;

        if (file==null)
            throw new NullPointerException("file path is null");

        if (!isDirectory(file.parent())){
            throw new FileNotFoundException("parent directory is not exist");
        }

        for (Directory_tree dT:allNodes) {
            if (dT.getP().toString().equals(file.toString())){
                return false;
            }
            String a = dT.getP().toString().replaceAll("\\\\","/");
            if (a.contains(file+"/")){
                return false;
            }
        }


        Storage tempStorage = stubs.keys().nextElement();
        Command tempCommand = stubs.get(tempStorage);

        //adding new file in directory tree
        allNodes.add(new Directory_tree(new Path(file.toString()),file.last(),tempCommand,tempStorage,false));

        //creating file in server storage file
        tempCommand.create(new Path(file.toString()));

        //adding file in to list
        listOffiles.add(new File(file.toString()));

        return true;
    }

    @Override
    public boolean createDirectory(Path directory) throws FileNotFoundException {


        if (directory.isRoot())
            return false;

        if (!isDirectory(directory.parent())){
            throw new FileNotFoundException("parent directory is not exist");
        }

        for (Directory_tree dT:allNodes) {
            if (Arrays.asList(dT.getP().toString().split("/")).containsAll(Arrays.asList(directory.toString().split("/")))){
                    return false;
            }
        }

            Storage tempStorage = stubs.keys().nextElement();
            Command tempCommand = stubs.get(tempStorage);

            //adding new directory in directory tree
            allNodes.add(new Directory_tree(new Path(directory.toString()),directory.last(),tempCommand,tempStorage,true));

            //adding directory in list
            listOffiles.add(new File(directory+"/"));

        return true;
    }

    @Override
    public boolean delete(Path path) throws FileNotFoundException {

        if (path==null)
            throw new NullPointerException("Null is provided");

        if (path.isRoot())
            return false;

        if (!isDirectory(path.parent())){
            throw new FileNotFoundException("parent directory is not exist");
        }

        try {
            for (Directory_tree dT:allNodes) {
                if (dT.getP().toString().startsWith(path.toString()))
                    allNodes.remove(dT);
            }
            stubs.get(getStorage(path)).delete(path);
        } catch (RMIException e) {
            e.printStackTrace();
        }

        return true;
    }

    @Override
    public Storage getStorage(Path file) throws FileNotFoundException {

        if (file==null)
            throw new NullPointerException("Null is provided");

        if (!isDirectory(file.parent()))
            throw new FileNotFoundException("Directories not exist");

        for (Directory_tree dT:allNodes) {
            if (dT.getP().toString().equals(file.toString())){
                return dT.getStorageStub();
            }
        }
        throw new FileNotFoundException("File not found");
    }

    // The method register is documented in Registration.java.
    @Override
    public Path[] register(Storage client_stub, Command command_stub,
                           Path[] files) {

        stubs.put(client_stub,command_stub);
        if (RegisteredServer.contains(command_stub))
            throw new IllegalStateException("Storage server is already registered.");

        if (client_stub.equals(null) || command_stub.equals(null) || files.equals(null))
            throw new NullPointerException("Anyone of the parameters null while registering storage server in naming server.");

        RegisteredServer.add(command_stub);

        //checkduplicatefiles returns the list of files that are already exist in naming server
        ArrayList<Path> copyfiles = checkduplicaefiles(files, client_stub, command_stub);
        Path[] toArray = new Path[0];

        return copyfiles.toArray(toArray);
    }

    private ArrayList<Path> checkduplicaefiles(Path[] files, Storage storageStub, Command commandStub) {

        ArrayList<Path> copyFiles = new ArrayList<>();
        for (Path p : files) {
            //exist function returns boolean value that if path is exit or not in naming server
            if (exist(p, storageStub, commandStub, copyFiles)) {
                continue;
            }
        }
          return copyFiles;
    }

    private boolean exist(Path p, Storage storageStub, Command commandStub, ArrayList<Path> list) {

        if (p.isRoot()) {
            return true;
        }

        if (allNodes.stream().map(Directory_tree::getP).collect(Collectors.toList()).contains(p) ||
                    allNodes.stream().filter(g -> g.getP().toString().contains(p+"/")).collect(Collectors.toList()).size() > 0) {
            list.add(p);
            return false;
        }
        List<String> ArrayPath = new LinkedList<>(Arrays.asList(p.toString().split("/")));

        String tempPath = "";

        while (ArrayPath.size() > 1) {
            boolean exist = false;
            tempPath += "/" + ArrayPath.get(1);
            if (allNodes.isEmpty()) {
                allNodes.add(new Directory_tree(new Path(p.toString()), ArrayPath.get(1), commandStub, storageStub,false));
                listOffiles.add( new File(p.toString()));
            } else {
                for (Object obj : allNodes.stream().map(Directory_tree::getP).collect(Collectors.toList())) {

                    if (obj.equals(p.toString())) {
                        exist = true;
                        break;
                    }
                }

                if (!exist) {
                    allNodes.add(new Directory_tree(new Path(p.toString()), ArrayPath.get(1), commandStub, storageStub,false));
                    listOffiles.add(new File(p.toString()));
                }
            }
            ArrayPath.remove(1);
        }

       return true;
    }
}
