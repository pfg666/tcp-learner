package sut.interfacing;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

public class ConnectionWrapper {
    private Socket socket;
    private InputStream inputStream;
    private OutputStream outputStream;
   
    public ConnectionWrapper(Socket socket) {
        this.socket = socket;
        
        try {
            this.inputStream = socket.getInputStream();
            this.outputStream = socket.getOutputStream();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
   
    /**
     * sets up a new socket
     * @param ipAddress
     * @param portNumber
     */
    public ConnectionWrapper (String ipAddress, int portNumber) {
        try {
        	
            socket = new Socket(ipAddress, portNumber);
            //socket.setSoTimeout(2000);
            socket.setTcpNoDelay(false);
            this.inputStream = socket.getInputStream();
            this.outputStream = socket.getOutputStream();
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
   
    /**
     * sends the string as output over the socket, and adds a newline to denote the end of the string
     * @param output the message to send
     */
    public void sendOutput(String output) {
        output += "\n";
        try {
			outputStream.write(output.getBytes());
			outputStream.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
   
    /**
     * receives an input, and reads up until a newline is received
     * @return the message received
     */
    public String receiveInput() {
    	StringBuilder sb = new StringBuilder();
    	try {
	        int receivedByte;
	        do {
				receivedByte = inputStream.read();
	            if (receivedByte != '\n') {
	                sb.append((char) receivedByte);
	            }
	        } while (receivedByte != '\n');
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	return sb.toString();
    }
    
    /**
     * closes the connection
     */
    public void close() {
    	try {
    		socket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
}