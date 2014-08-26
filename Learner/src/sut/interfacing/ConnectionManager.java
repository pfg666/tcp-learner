package sut.interfacing;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

public class ConnectionManager {
	
	private static Map<Integer, ConnectionWrapper> socketMap = new HashMap<Integer, ConnectionWrapper>();
	
	public static ConnectionWrapper getConnection(int portNumber) {
		ConnectionWrapper connection = socketMap.get(portNumber);
		if (connection == null) {
			connection = setupConnection(portNumber);
		}
		return connection;
	}
	
	private static ConnectionWrapper setupConnection(int portNumber) {
		System.out.println("Setting socket");
		System.out.println(portNumber);
		final ConnectionWrapper connection = new ConnectionWrapper(InetAddress.getLoopbackAddress()
				.getHostAddress(), portNumber);
		socketMap.put(portNumber, connection);
		
		/* Adding runtime hook to close the socket in case we terminate execution */
		Runtime.getRuntime().addShutdownHook(new Thread() {
		    public void run() { 
		    	System.out.println("Shutting down socket ");
		    	connection.sendOutput("exit");
		    	connection.close();
		    }
		 });
		return connection;
	}
}
