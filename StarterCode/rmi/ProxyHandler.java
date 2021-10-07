package rmi;

import java.io.*;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.net.Socket;

public class ProxyHandler implements InvocationHandler {

    private InetSocketAddress address;
    private Class<?> c;

    public ProxyHandler(InetSocketAddress address, Class<? > c){
        this.address = address;
        this.c = c;
    }


    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        String functionName = method.getName();
        Class[] tyesOfParamates = method.getParameterTypes();
        Object MarshalledPacket = null;
        boolean result = false;
        ObjectOutputStream write = null;
        ObjectInputStream read = null;
        Socket new_Socket = new Socket();
        //Local methods - Equal(),hashcode(),toString()

        if (functionName.equals("equals")){
            java.lang.reflect.Proxy argument = (java.lang.reflect.Proxy) args[0];

            if (argument==null){
                return false;
            }

            InetSocketAddress address1 = ((ProxyHandler)Proxy.getInvocationHandler(argument)).address;
            Class<?> c1 = ((ProxyHandler)Proxy.getInvocationHandler(argument)).c;

            if (address.equals(address1) && c.equals(c1))
                return true;
            else
                return false;

        }else if (functionName.equals("hashCode")){
            return address.hashCode()+c.hashCode();
        }else if (functionName.equals("toString")){
            return "InetAddress of Stub is :" + address + " Interface : "+c;
        }else {


            try{

                new_Socket.connect(address);

                write = new ObjectOutputStream(new_Socket.getOutputStream());
                read = new ObjectInputStream(new_Socket.getInputStream());

                write.writeObject(functionName);
                write.writeObject(tyesOfParamates);
                write.writeObject(args);

                result = (boolean)read.readObject();
                MarshalledPacket = read.readObject();

                new_Socket.close();

            }catch (FileNotFoundException e){
                throw new FileNotFoundException();
            }
            catch (Exception e){
                new_Socket.close();
                throw new RMIException("RMI");
            }
            if (result == false) {
                throw   ((Throwable) MarshalledPacket);
            }
        }
        return MarshalledPacket;
    }
}