package autoSearch;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import javax.swing.ProgressMonitor;

import org.xml.sax.SAXException;

import modules.Params;

/**
 * This class takes handles clients. Once
 * a client is passed to it, it determines
 * what to do with incoming messages.
 * 
 * @authors jbelmont, njooma
 */
public class AUClientThread extends Thread {

	private Socket _clientSocket;
	private MainForm _mainForm;
	private ProgressMonitor _progressMonitor;
	private Params _params;
	private boolean tempBusy = false;
	
	public AUClientThread(Socket clientSocket, MainForm mainForm) {
		super("AUClientThread");
		_clientSocket = clientSocket;
		_mainForm = mainForm;
		_params = _mainForm.getParams();
		System.out.println("New connection at "+_clientSocket.getRemoteSocketAddress());
	}
	
	public void run() {
		try {
			PrintWriter out = new PrintWriter(_clientSocket.getOutputStream(), true);
			ObjectInputStream in = new ObjectInputStream(_clientSocket.getInputStream());
	        //Start reading inputs
			//First input is always the "Ready" query
			String ready = in.readUTF();
	        System.out.println("Incoming query: "+ready);
	        //If system is busy...
	        if (_mainForm.isBusy() || tempBusy == true) {
	        	out.println("busy");
	        	System.out.println("System is busy.");
	        }
	        //If system is ready for a sample
	        else {
	        	System.out.println("Ready to receive.");
	        	//Send "ready"
	        	out.println("ready");
	        	tempBusy = true;
	        	//Read path where files should be stored
	        	String sequestPathString = in.readUTF();
	        	System.out.println("In path: "+sequestPathString);
	        	Path sequestPath = Paths.get(_params.getGlobalParam("SEQUEST.BaseFolder", "C:\\sequest"), sequestPathString);
	        	System.out.println("sequestPath: " + sequestPath.toString());
	        	Files.createDirectories(sequestPath);
	        	//Read number of files
		        int numFiles = in.readInt();
		        System.out.println("Number of files incoming: "+numFiles);
		        //Create the read buffer
		        byte[] buffer = new byte[4092];
		        int read = 0;
		        int totalRead = 0;
		        //For each incoming file...
		        for (int i=0; i<numFiles; i++) {
		        	System.out.println("Receiving file "+i+" of "+numFiles);
		        	//Read file name
		        	String file = in.readUTF();
		        	System.out.println(file);
		        	Path filePath = Paths.get(sequestPath.toString()+file.substring(file.lastIndexOf(".")));
		        	Files.deleteIfExists(filePath);
		        	//Read file size
		        	long fileSize = in.readLong();
		        	System.out.println("File size: "+fileSize);
		        	_progressMonitor = new ProgressMonitor(_mainForm, "Receiving file: "+file, "File size: "+fileSize+" bytes", 0, (int) fileSize);
		        	//Read and write to file
		        	totalRead = 0;
		        	OutputStream fos = Files.newOutputStream(filePath, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
		        	System.out.println("File path: " + filePath);
		        	while (fileSize>0 && (read = in.read(buffer))>0) {
		        		fos.write(buffer, 0, read);
		        		fos.flush();
		        		fileSize -= read;
		        		totalRead += read;
		        		_progressMonitor.setProgress(totalRead);
		        	}
		        	fos.close();
		        	_progressMonitor.close();
		        }
		        //Read in complete sample name
		        String sample = in.readUTF();
		        System.out.println("Processing sample: "+sample);
		        out.println("received");
		        _mainForm.processSample(sample);
		        tempBusy = false;
	        }
	        //Close all resources
	        in.close();
	        out.close();
	        _clientSocket.close();
		} catch (IOException | NullPointerException | InterruptedException e) {
			e.printStackTrace();
			tempBusy = false;
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
