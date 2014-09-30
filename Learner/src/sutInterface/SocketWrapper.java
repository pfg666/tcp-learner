package sutInterface;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

// Wrapper around the java Socket so we have clear segmentation of inputs and outputs
public class SocketWrapper {
	protected Socket sock;
	protected PrintWriter sockout;
	protected BufferedReader sockin;
	

	public SocketWrapper(String sutIP, int sutPort) {
		try {
			sock = new Socket(sutIP, sutPort);
			sockout = new PrintWriter(sock.getOutputStream(), true);
			sockin = new BufferedReader(new InputStreamReader(
					sock.getInputStream()));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public SocketWrapper(int sutPort) {
		this("localhost", sutPort);
	}

	public void writeInput(String input) {
		System.out.println("IN: "+ input);
		sockout.println(input);
		sockout.flush();
	}

	public String readOutput() {
		String output = null;
		try {
			output = sockin.readLine();
			System.out.println("OUT: "+ output);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return output;
	}

	public void close() {
		try {
			sockin.close();
			sockout.close();
			sock.close();
		} catch (IOException ex) {

		}
	}
}
