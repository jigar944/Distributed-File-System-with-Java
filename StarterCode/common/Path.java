package common;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/** Distributed filesystem paths.

 <p>
 Objects of type <code>Path</code> are used by all filesystem interfaces.
 Path objects are immutable.

 <p>
 The string representation of paths is a forward-slash-delimeted sequence of
 path components. The root directory is represented as a single forward
 slash.

 <p>
 The colon (<code>:</code>) and forward slash (<code>/</code>) characters are
 not permitted within path components. The forward slash is the delimeter,
 and the colon is reserved as a delimeter for application use.
 */
public class Path implements Iterable<String>, Serializable
{
    /** Creates a new path which represents the root directory. */

    String p;
    String root= "/";

    public Path()
    {
        //assign root directory
        p = root;
    }

    /** Creates a new path by appending the given component to an existing path.

     @param path The existing path.
     @param component The new component.
     @throws IllegalArgumentException If <code>component</code> includes the
     separator, a colon, or
     <code>component</code> is the empty
     string.
     */
    public Path(Path path, String component)
    {
        if(component==null){
            throw new IllegalArgumentException("Component string is null.");
        }else if(component.isEmpty()){
            throw new IllegalArgumentException("Component String is empty.");
        }else if (component.contains("/")){
            throw new IllegalArgumentException("Component string contains Separator");
        }else if (component.contains(":")){
            throw new IllegalArgumentException("Component string contains colon");
        }else {
            if (path.isRoot()){
                p = path.p + component;
            }else {
                p = path.p +"/"+ component;
            }
        }
    }

    /** Creates a new path from a path string.

     <p>
     The string is a sequence of components delimited with forward slashes.
     Empty components are dropped. The string must begin with a forward
     slash.

     @param path The path string.
     @throws IllegalArgumentException If the path string does not begin with
     a forward slash, or if the path
     contains a colon character.
     */
    public Path(String path)
    {
        //replacing last index forward slash in path string
        String temp = path.replaceAll("/+$","");
        String temp2 = temp.replaceFirst("/+","");

        if (!path.startsWith("/") || path.contains(":")){
            throw new IllegalArgumentException("Path string contains illegal expressions");
        }else {
            this.p = root+temp2;
        }
    }

    /** Returns an iterator over the components of the path.

     <p>
     The iterator cannot be used to modify the path object - the
     <code>remove</code> method is not supported.

     @return The iterator.
     */
    @Override
    public Iterator<String> iterator()
    {
        String[] ar = this.p.replaceFirst("/","").split("/");
        //split string path to arraylist
        List<String> arrayList = Arrays.asList(ar);
        //iterating arraylist
        Iterator<String> itr = arrayList.iterator();
        return itr;
    }

    /** Lists the paths of all files in a directory tree on the local
     filesystem.

     @param directory The root directory of the directory tree.
     @return An array of relative paths, one for each file in the directory
     tree.
     @throws FileNotFoundException If the root directory does not exist.
     @throws IllegalArgumentException If <code>directory</code> exists but
     does not refer to a directory.
     */
    public static Path[] list(File directory) throws FileNotFoundException
    {

        File tempFile = new File(directory.getAbsolutePath());

        if (!tempFile.exists()){
            throw new FileNotFoundException("Root directory does not exist");
        }

        if (!directory.isDirectory()){
            throw new IllegalArgumentException("Parameter directory does not a directory.");
        }

        //fetching list of files in particular directory
        ArrayList<Path> newPath =getArrayOfAllPath(new Path(),directory,new ArrayList<>());

        return newPath.toArray(new Path[0]);
    }

    private static ArrayList<Path> getArrayOfAllPath(Path p,File directory, ArrayList<Path> paths) {

        File[] file = directory.listFiles();

        for (File f:file) {
            if (f.isFile()){
                paths.add(new Path(p,f.getName()));
            }else{
                getArrayOfAllPath(new Path(p,f.getName()),f,paths);
            }
        }
        return paths;

    }

    /** Determines whether the path represents the root directory.

     @return <code>true</code> if the path does represent the root directory,
     and <code>false</code> if it does not.
     */
    public boolean isRoot()
    {
        if (p.equals("/")){
            return true;
        }else {
            return false;
        }
    }

    /** Returns the path to the parent of this path.

     @throws IllegalArgumentException If the path represents the root
     directory, and therefore has no parent.
     */
    public Path parent()
    {
        if (isRoot()){
            throw new IllegalArgumentException("path represents root directory and has no parent.");
        }else {
            List<String> a = new ArrayList<String>(Arrays.asList(p.split("/")));
            a.remove(a.size()-1);
            String finallist = String.join("/",a);
            if (a.size()==1)
                return new Path("/");
            else
                return new Path(finallist);
        }
    }

    /** Returns the last component in the path.

     @throws IllegalArgumentException If the path represents the root
     directory, and therefore has no last
     component.
     */
    public String last()
    {
        if (isRoot()){
            throw new IllegalArgumentException("path represents root directory or null.");
        }else {
            return new File(this.p).getName();
        }
    }

    /** Determines if the given path is a subpath of this path.

     <p>
     The other path is a subpath of this path if is a prefix of this path.
     Note that by this definition, each path is a subpath of itself.

     @param other The path to be tested.
     @return <code>true</code> If and only if the other path is a subpath of
     this path.
     */
    public boolean isSubpath(Path other)
    {

        if (toString().contains(other.toString())){
            return true;
        }
        return false;
    }

    /** Converts the path to <code>File</code> object.

     @param root The resulting <code>File</code> object is created relative
     to this directory.
     @return The <code>File</code> object.
     */
    public File toFile(File root)
    {
       return new File(root.getPath()+(this));
    }

    /** Compares two paths for equality.

     <p>
     Two paths are equal if they share all the same components.

     @param other The other path.
     @return <code>true</code> if and only if the two paths are equal.
     */
    @Override
    public boolean equals(Object other)
    {
        return this.p.equals(other.toString());
    }

    /** Returns the hash code of the path. */
    @Override
    public int hashCode()
    {
        return p.hashCode();
    }

    /** Converts the path to a string.

     <p>
     The string may later be used as an argument to the
     <code>Path(String)</code> constructor.

     @return The string representation of the path.
     */
    @Override
    public String toString()
    {
        return p;
    }
}
