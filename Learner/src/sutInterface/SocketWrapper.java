package sutInterface;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import util.Log;

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
			System.exit(0);
		}
	}
	
	public SocketWrapper(int sutPort) {
		this("localhost", sutPort);
	}

	public void writeInput(String input) {
		Log.info("IN: "+ input);
		sockout.println(input);
		sockout.flush();
	}

	public String readOutput() {
		String output = null;
		try {
			output = sockin.readLine();
			Log.info("OUT: "+ output);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return output;
	}

	public void close() {
		sockout.write("exit");
		sockout.close();
		try {
			sockin.close();
		} catch (IOException ex) {

		}
		try {
			sock.close();
		} catch (IOException ex) {

		}
	}
}
