package sut.interfacing;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages socket wrappers, so that only one wrapper is used for each port. Also adds a shutdown hook which closes all sockets
 * System.exit
 */
public class SocketManager {
	
	private static Map<Integer, SocketWrapper> socketMap = new HashMap<Integer, SocketWrapper>();
	
	public static SocketWrapper newSocket(int portNumber) {
		SocketWrapper connection = socketMap.get(portNumber);
		if (connection == null) {
			connection = setupSocket(portNumber);
		}
		return connection;
	}
	
	private static SocketWrapper setupSocket(int portNumber) {
		System.out.println("Setting socket");
		System.out.println(portNumber);
		final SocketWrapper connection = new SocketWrapper(InetAddress.getLoopbackAddress()
				.getHostAddress(), portNumber);
		socketMap.put(portNumber, connection);
		
		/* Adding runtime hook to close the socket in case we terminate execution */
		Runtime.getRuntime().addShutdownHook(new Thread() {
		    public void run() { 
		    	System.out.println("Shutting down socket ");
		    	connection.close();
		    }
		 });
		return connection;
	}
}
