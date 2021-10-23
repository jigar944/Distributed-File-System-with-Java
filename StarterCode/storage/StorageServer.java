package storage;

import common.Path;
import naming.Registration;
import rmi.RMIException;
import rmi.Skeleton;
import rmi.Stub;

import java.io.*;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.util.Comparator;

/** Storage server.

    <p>
    Storage servers respond to client file access requests. The files accessible
    through a storage server are those accessible under a given directory of the
    local filesystem.
 */
public class StorageServer implements Storage, Command
{
    private File rootDir;
    Skeleton<Command> commandSkeleton;
    Skeleton<Storage> storageSkeleton;
    Command commandStub;
    Storage storageStub;

    /** Creates a storage server, given a directory on the local filesystem.

        @param root Directory on the local filesystem. The contents of this
                    directory will be accessible through the storage server.
        @throws NullPointerException If <code>root</code> is <code>null</code>.
    */
    public StorageServer(File root)
    {
       if (root.equals(null))
           throw new NullPointerException();

        this.rootDir = root;
        commandSkeleton = new Skeleton<Command>(Command.class,this);
        storageSkeleton = new Skeleton<Storage>(Storage.class,this);


    }

    /** Starts the storage server and registers it with the given naming
        server.

        @param hostname The externally-routable hostname of the local host on
                        which the storage server is running. This is used to
                        ensure that the stub which is provided to the naming
                        server by the <code>start</code> method carries the
                        externally visible hostname or address of this storage
                        server.
        @param naming_server Remote interface for the naming server with which
                             the storage server is to register.
        @throws UnknownHostException If a stub cannot be created for the storage
                                     server because a valid address has not been
                                     assigned.
        @throws FileNotFoundException If the directory with which the server was
                                      created does not exist or is in fact a
                                      file.
        @throws RMIException If the storage server cannot be started, or if it
                             cannot be registered.
     */
    public synchronized void start(String hostname, Registration naming_server)
            throws RMIException, UnknownHostException, FileNotFoundException {
        if(!rootDir.exists() || rootDir.isFile() )
            throw new FileNotFoundException("Root directory is not found or is a file ");

        commandSkeleton.start();;
        storageSkeleton.start();

        createStub(hostname);

        Path[] CopyFile = naming_server.register(storageStub,commandStub,Path.list(rootDir));


        // Delete those duplicate files
        for(Path path: CopyFile) {
            File currentFile = path.toFile(rootDir);
            File parentFile = new File(currentFile.getParent());
            currentFile.delete();

            // Delete the parent file if empty
            while(!parentFile.equals(rootDir)) {
                if (parentFile.list().length == 0) {
                    parentFile.delete();
                    parentFile =  new File(parentFile.getParent());
                } else {
                    break;
                }
            }
        }


    }

    private void createStub(String host) {
        commandStub =  Stub.create(Command.class,commandSkeleton,host);
        storageStub =  Stub.create(Storage.class,storageSkeleton,host);
    }

    /** Stops the storage server.

        <p>
        The server should not be restarted.
     */
    public void stop()
    {
        storageSkeleton.stop();
        commandSkeleton.stop();
    }

    /** Called when the storage server has shut down.

        @param cause The cause for the shutdown, if any, or <code>null</code> if
                     the server was shut down by the user's request.
     */
    protected void stopped(Throwable cause)
    {
    }

    // The following methods are documented in Storage.java.
    @Override
    public synchronized long size(Path file) throws FileNotFoundException
    {
        File PathToFile = file.toFile(rootDir);
        if (PathToFile.isDirectory() || !PathToFile.exists())
            throw new FileNotFoundException("File not found or it is a directory");
        return PathToFile.length();
    }

    @Override
    public synchronized byte[] read(Path file, long offset, int length)
        throws FileNotFoundException, IOException
    {
        File PathToFile = file.toFile(rootDir);

        if (PathToFile.isDirectory() || !PathToFile.exists())
            throw new FileNotFoundException("File not found or it is a directory");

        if (length<0 || (offset+length)>PathToFile.length())
            throw new IndexOutOfBoundsException("Indexes of reading file is out of bound");

        byte[] readContent = new byte[length];

        FileInputStream readFile = new FileInputStream(PathToFile);

        readFile.read(readContent,(int)offset,length);

        try{
            readFile.close();
        }catch (Exception e){}

        return readContent;
    }

    @Override
    public synchronized void write(Path file, long offset, byte[] data)
        throws FileNotFoundException, IOException
    {
        File PathToFile = file.toFile(rootDir);

        if (PathToFile.isDirectory() || !PathToFile.exists())
            throw new FileNotFoundException("File not found or it is a directory");

        if (offset<0)
            throw new IndexOutOfBoundsException("Offset is negative ");

        FileOutputStream writeFile = new FileOutputStream(PathToFile);

        FileChannel c1 = writeFile.getChannel();
        c1.position(offset);
        c1.write(ByteBuffer.wrap(data));

        try {
            writeFile.close();
        }catch (Exception e){}

    }

    // The following methods are documented in Command.java.
    @Override
    public synchronized boolean create(Path file)
    {
        if (file.isRoot())
            return false;

       File createFile = file.toFile(rootDir);
       File parentFile = file.parent().toFile(rootDir);

       if (file.parent().isRoot()){
           if (!createFile.exists()) {
               try {
                   createFile.createNewFile();
                   return true;
               } catch (IOException e) {
                   e.printStackTrace();
               }
           }
       }


        if (!parentFile.exists()){
            parentFile.mkdirs();
        }else {
            return false;
        }

        if (!createFile.exists()){
            try {
                createFile.createNewFile();
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return true;

    }

    @Override
    public synchronized boolean delete(Path path)
    {
            File deleteFile = path.toFile(rootDir);

            if (path.isRoot())
                return false;

            if(!deleteFile.exists()){
                return false;
            }

            if(deleteFile.isFile()){
                deleteFile.delete();
                return true;
            }else {
                for (File f:deleteFile.listFiles()) {
                    try {
                                Files.walk(deleteFile.toPath())
                                .sorted(Comparator.reverseOrder())
                                .map(java.nio.file.Path :: toFile)
                                .forEach(File::delete);
                                return true;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            return false;
    }
}
