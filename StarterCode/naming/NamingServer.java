package naming;

import common.Path;
import rmi.RMIException;
import rmi.Skeleton;
import rmi.Stub;
import storage.Command;
import storage.Storage;

import java.io.FileNotFoundException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/** Naming server.

    <p>
    Each instance of the filesystem is centered on a single naming server. The
    naming server maintains the filesystem directory tree. It does not store any
    file data - this is done by separate storage servers. The primary purpose of
    the naming server is to map each file name (path) to the storage server
    which hosts the file's contents.

    <p>
    The naming server provides two interfaces, <code>Service</code> and
    <code>Registration</code>, which are accessible through RMI. Storage servers
    use the <code>Registration</code> interface to inform the naming server of
    their existence. Clients use the <code>Service</code> interface to perform
    most filesystem operations. The documentation accompanying these interfaces
    provides details on the methods supported.

    <p>
    Stubs for accessing the naming server must typically be created by directly
    specifying the remote network address. To make this possible, the client and
    registration interfaces are available at well-known ports defined in
    <code>NamingStubs</code>.
 */
public class NamingServer implements Service, Registration
{

    Skeleton<Registration> registrationSkeleton;
    Skeleton<Service> serviceSkeleton;
    Stub registrationStub;
    Directory_tree tree;
    Stub serviceStub;
    HashSet<Command> RegisteredServer = new HashSet<>();
    ArrayList<Node> allNodes;

    /** Creates the naming server object.

        <p>
        The naming server is not started.
     */
    public NamingServer()
    {

        InetSocketAddress portOfRegistration = new InetSocketAddress(NamingStubs.REGISTRATION_PORT);
        registrationSkeleton = new Skeleton<Registration>(Registration.class,this,portOfRegistration);

        InetSocketAddress portOfService = new InetSocketAddress(NamingStubs.SERVICE_PORT);
        serviceSkeleton = new Skeleton<Service>(Service.class,this,portOfService);

        tree = new Directory_tree();

    }

    /** Starts the naming server.

        <p>
        After this method is called, it is possible to access the client and
        registration interfaces of the naming server remotely.

        @throws RMIException If either of the two skeletons, for the client or
                             registration server interfaces, could not be
                             started. The user should not attempt to start the
                             server again if an exception occurs.
     */
    public synchronized void start() throws RMIException
    {
        this.serviceSkeleton.start();
        this.registrationSkeleton.start();

    }

    /** Stops the naming server.

        <p>
        This method waits for both the client and registration interface
        skeletons to stop. It attempts to interrupt as many of the threads that
        are executing naming server code as possible. After this method is
        called, the naming server is no longer accessible remotely. The naming
        server should not be restarted.
     */
    public void stop()
    {
        this.serviceSkeleton.stop();
        this.registrationSkeleton.stop();
        stopped(null);
    }

    /** Indicates that the server has completely shut down.

        <p>
        This method should be overridden for error reporting and application
        exit purposes. The default implementation does nothing.

        @param cause The cause for the shutdown, or <code>null</code> if the
                     shutdown was by explicit user request.
     */
    protected void stopped(Throwable cause)
    {
    }

    // The following methods are documented in Service.java.
    @Override
    public boolean isDirectory(Path path) throws FileNotFoundException
    {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public String[] list(Path directory) throws FileNotFoundException
    {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public boolean createFile(Path file)
        throws RMIException, FileNotFoundException
    {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public boolean createDirectory(Path directory) throws FileNotFoundException
    {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public boolean delete(Path path) throws FileNotFoundException
    {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public Storage getStorage(Path file) throws FileNotFoundException
    {
        throw new UnsupportedOperationException("not implemented");
    }

    // The method register is documented in Registration.java.
    @Override
    public Path[] register(Storage client_stub, Command command_stub,
                           Path[] files)
    {

        if (RegisteredServer.contains(command_stub))
            throw new IllegalStateException("Storage server is already registered.");

        if (client_stub.equals(null) || command_stub.equals(null) || files.equals(null))
            throw new NullPointerException("Anyone of the parameters null while registering storage server in naming server.");

        RegisteredServer.add(command_stub);

        ArrayList<Path> copyfiles = checkduplicaefiles(files,client_stub,command_stub);


        throw new UnsupportedOperationException("not implemented");
    }

    private ArrayList<Path> checkduplicaefiles(Path[] files,Storage storageStub,Command commandStub) {

        ArrayList<Path> temp = new ArrayList<>();
        for (Path p:files) {
            if (exist(p,storageStub,commandStub)){

            }else {
                temp.add(p);
            }
        }
        return temp;
    }

    private boolean exist(Path p,Storage storageStub,Command commandStub) {

        if (p.isRoot()){
            return true;
        }

        String[] ArrayPath = p.toString().split("/");
        List<String> ArrayPath1 = Arrays.asList(ArrayPath);

        //if there is 1 component in the list

        if (ArrayPath.length==1){
            for (Node dT: allNodes) {
                if (dT.getFilename().equals(ArrayPath[0])){
                    return false;
                }
            }
            allNodes.add(new Directory_tree(p,ArrayPath[0],commandStub,storageStub));
            return true;
        }



        for (Node dT:allNodes) {

            if (dT.filename.equals(ArrayPath1.get(0)) && dT.isDirectory()){
                ArrayPath1.remove(0);
                String[] newPath = p.toString().split("/",2);
                Path p1 = new Path(newPath[1]);
                return exist(p1,storageStub,commandStub);
            }else if (dT.getFilename().equals(ArrayPath1.get(0)) && !(dT.isDirectory())){
                return false;
            }

        }

        Node newNode = new Node(p,ArrayPath1.get(0));
        allNodes.add(newNode);

        return true;


    }
}
