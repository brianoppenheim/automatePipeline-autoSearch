package autoSearch;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.ProcessBuilder.Redirect;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

import static java.nio.file.StandardCopyOption.*;

import java.nio.charset.CharsetDecoder;
import java.nio.file.AccessDeniedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.LayoutStyle;
import javax.swing.table.DefaultTableModel;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.xml.sax.SAXException;

import modules.Form;
import modules.Params;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
//import autoLoad.Form;
//import autoLoad.Params;

/**
 * @authors jbelmont, njooma
 *
 */
@SuppressWarnings("serial")
public class MainForm extends JFrame implements Form {

	private boolean _isBusy, _isPaused;
	private JTextArea _statusArea;
	private JPanel _clientPanel;
	private JTable _clientList;
	private String _appPath, _paramFile, _userName, _sample, _samplePath, _sampleLocalPath, _searchEngine, _sep,
			_searchParam, _instrumentType, _fileType, _fileExt, _srBackupPath, _extractParam;
	private Params _params;
	private Client[] _clients;
	private Vector<Path> _fileList = new Vector<Path>();

	private String pathToCSV;

	public MainForm() {
		// GUI Basics
		super("AutoSearch");
		this.setPreferredSize(new Dimension(950, 250));
		this.setSize(this.getPreferredSize());
		this.setVisible(true);

		// Handles window closing events
		// Warns user if a process is running at time of shut-down
		WindowListener exitListener = new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				if (_isBusy) {
					String message = "Are you sure you want to close the application?\n"
							+ "<html><b>There is currently a process running.</b></html>";
					int confirm = JOptionPane.showOptionDialog((MainForm) e.getSource(), message, "Exit Confirmation",
							JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, null, null);
					if (confirm == 0) {
						setDefaultCloseOperation(EXIT_ON_CLOSE);
					} else {
						setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
					}
				} else {
					setDefaultCloseOperation(EXIT_ON_CLOSE);
				}

			}
		};
		this.addWindowListener(exitListener);

		// Main Panel
		JPanel mainPanel = new JPanel();
		mainPanel.setPreferredSize(this.getPreferredSize());
		GroupLayout mainLayout = new GroupLayout(mainPanel);
		mainLayout.setAutoCreateGaps(true);
		mainLayout.setAutoCreateContainerGaps(true);
		mainPanel.setLayout(mainLayout);

		// Status Area
		_statusArea = new JTextArea("[" + new Date().toString() + "] AutoSearch initializing...");
		// _statusArea.setPreferredSize(new Dimension(930, 200));
		// _statusArea.setSize(_statusArea.getPreferredSize());
		_statusArea.setFont(new java.awt.Font("MONOSPACED", java.awt.Font.PLAIN, 12));
		_statusArea.setEditable(false);
		JScrollPane statusScroll = new JScrollPane(_statusArea);
		statusScroll.setAutoscrolls(true);

		// Client Panel
		_clientPanel = new JPanel();
		GroupLayout cpLayout = new GroupLayout(_clientPanel);
		cpLayout.setAutoCreateGaps(true);
		cpLayout.setAutoCreateContainerGaps(true);
		_clientPanel.setLayout(cpLayout);
		_clientPanel.setPreferredSize(new Dimension(700, 150));
		_clientPanel.setBorder(BorderFactory.createTitledBorder("Clients"));

		// Client List
		_clientList = new JTable();
		_clientList.setBorder(BorderFactory.createLineBorder(java.awt.Color.BLACK));
		JScrollPane clientScroll = new JScrollPane(_clientList);
		_clientList.setFillsViewportHeight(true);

		cpLayout.setHorizontalGroup(
				cpLayout.createSequentialGroup().addGroup(cpLayout.createParallelGroup().addComponent(clientScroll)));
		cpLayout.setVerticalGroup(
				cpLayout.createParallelGroup().addGroup(cpLayout.createSequentialGroup().addComponent(clientScroll)));

		_clientPanel.setVisible(false);

		// Buttons for pausing and setting priority
		JButton pauseButton = new JButton("Pause");
		pauseButton.addActionListener(new PauseListener());
		JButton setPriorityButton = new JButton("Set Priority");
		setPriorityButton.addActionListener(new PriorityButtonListener());

		// Add everything to the main panel
		mainLayout.setHorizontalGroup(mainLayout.createParallelGroup()
				.addGroup(mainLayout.createSequentialGroup().addComponent(statusScroll).addComponent(_clientPanel))
				.addGroup(mainLayout.createSequentialGroup().addComponent(pauseButton)
						.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE,
								Short.MAX_VALUE)
						.addComponent(setPriorityButton)));
		mainLayout.setVerticalGroup(mainLayout.createSequentialGroup()
				.addGroup(mainLayout.createParallelGroup().addComponent(statusScroll).addComponent(_clientPanel))
				.addGroup(mainLayout.createParallelGroup().addComponent(pauseButton).addComponent(setPriorityButton)));

		this.add(mainPanel);
		this.pack();

		// Get & Set the _appPath
		_sep = System.getProperty("file.separator");
		this.log("_sep: " + _sep.toString()); // delete
		_appPath = System.getProperty("java.class.path");
		_appPath = _appPath.substring(0, _appPath.lastIndexOf(_sep));

		// Get everything else going...
		this.loadForm();
	}

	// ==================== ACCESSORS AND MUTATORS ====================\\
	public void setParamFile(String path) {
		_paramFile = path;
	}

	public String getParamFile() {
		return _paramFile;
	}

	public Params getParams() {
		return _params;
	}

	public String getAppPath() {
		return _appPath;
	}

	public String getUserName() {
		return _userName;
	}

	public String getCurrentClient() {
		return _params.getClientParam("CLIENT_TAG", "");
	}

	public boolean isBusy() {
		return _isBusy;
	}

	private void setBusy(boolean busy) {
		_isBusy = busy;
	}

	public boolean isPaused() {
		return _isPaused;
	}

	// ==================== METHODS ====================\\

	/**
	 * This method logs a message onto the status panel. It prepends the date
	 * and time to the message.
	 * 
	 * @param String
	 *            message
	 */
	public void log(String message) {
		_statusArea.append("\n[" + new Date().toString() + "] " + message);
		try {
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(
					Paths.get(_appPath, "AutoSearchLog.txt"), StandardOpenOption.CREATE, StandardOpenOption.APPEND)));
			out.newLine();
			out.write("[" + new Date().toString() + "] " + message);
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * This method logs a message, appending it to the previous message without
	 * adding the date or a new line.
	 * 
	 * @param message
	 */
	public void logNNL(String message) {
		_statusArea.append(message);
		try {
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(
					Paths.get(_appPath, "AutoSearchLog.txt"), StandardOpenOption.CREATE, StandardOpenOption.APPEND)));
			out.write("[" + new Date().toString() + "] " + message);
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * This method loads the necessary info into the application.
	 */
	private void loadForm() {
		// Load parameters
		this.setParamFile("HTAPP.Configuration.xml");
		System.out.println(this.getParamFile());
		_params = new Params(this);
		_params.loadParam(this.getParamFile());

		// Initialize clients
		this.initClients();
		this.logNNL("done!");

		new AUServerSocket(this, _params).start();
	}

	/**
	 * Initializes the clients and builds the client list
	 */
	public void initClients() {
		String[] clientList = _params.getAllClients();
		_clients = new Client[clientList.length];
		String[][] clientData = new String[clientList.length][2];
		for (int i = 0; i < clientList.length; i++) {
			_clients[i] = new Client(clientList[i], 2);
			clientData[i][0] = _clients[i].getName();
			clientData[i][1] = "2-Normal";
		}
		// Build the table
		String[] columnNames = { "Client", "Priority" };
		_clientList.setModel(new DefaultTableModel(clientData, columnNames));
		JComboBox<String> priorities = new JComboBox<String>();
		priorities.addActionListener(new PriorityListener());
		priorities.addItem("1-High");
		priorities.addItem("2-Normal");
		priorities.addItem("3-Low");
		priorities.addItem("0-Disabled");
		javax.swing.table.TableColumn priority = _clientList.getColumnModel().getColumn(1);
		priority.setCellEditor(new DefaultCellEditor(priorities));
	}

	/**
	 * This method parses the string coming from AutoUpdate. The string contains
	 * all the information for the current sample, including client ID, sample
	 * location, user, etc.
	 * 
	 * It also calls the other methods required for actually processing the
	 * sample, such as downloading the files, running the search, and making
	 * DTAs.
	 * 
	 * @param String
	 *            sample
	 * @throws InterruptedException
	 * @throws SAXException
	 */
	// jmb changes 08/27/13: msconvert command line args are no longer passed in
	// from the finished sample
	// list string--so length and parsing order of the parts String[] is
	// slightly different now.
	// Also the peakpicking filters for msconvert are now hardcoded in when that
	// program is called.
	public void processSample(String sample) throws InterruptedException, SAXException {
		this.setBusy(true);
		this.pause("Sample Processing");

		try {
			String[] parts = sample.split("\\+");
			_params.setCurrentClientIdentity(parts[0]);
			_samplePath = parts[1];
			System.out.println("Sample Path: " + _samplePath);
			_sample = _samplePath.substring(_samplePath.lastIndexOf(_sep) + 1);

			// Get file type and extension
			if (_samplePath.indexOf(":") > 0) {
				String ext = _samplePath.substring(_samplePath.lastIndexOf(":"));
				_fileType = ext.substring(1, ext.indexOf("."));
				_fileExt = ext.substring(ext.indexOf("."));
				_samplePath = _samplePath.substring(0, _samplePath.lastIndexOf(":"));
				_sample = _sample.substring(0, _sample.lastIndexOf(":"));
			} else {
				_fileType = "THMRAW";
				_fileExt = ".raw";
			}

			System.out.println("Current sample: " + _sample);
			this.log("Processing sample: " + _sample);
			this.log("          File Type: " + _fileType + " (" + _fileExt + ")");
			// Set up the local path for the sample
			Path clientPath = Paths.get(_params.getClientParam("LOCAL_PATH", "F:\\Xcalibur\\data"));// CHANGE
																									// TO
																									// F:
																									// REMOVE
																									// QEBR
			String pathToRel = _samplePath;
			System.out.println("Path to relativize: " + pathToRel);
			System.out.println("Client Path: " + clientPath);
			Path subPathPath = clientPath.relativize(Paths.get(pathToRel)); // Restore
																			// this.
			String subPath = subPathPath.toString();
			_sampleLocalPath = _params.getGlobalParam("SEQUEST.BaseFolder", "C:\\sequest") + _sep
					+ _params.getClientParam("CLIENT_TAG", "") + _sep + subPath;// +
																				// _sep
																				// +
																				// _sample;
			this.log("          Local Path: " + _sampleLocalPath);
			// Set backup paths
			_srBackupPath = _params.getGlobalParam("SR_BACKUP_PATH", "") + _sep
					+ _params.getClientParam("CLIENT_TAG", "") + _sep + subPath;
			String rawBackupPath = _params.getGlobalParam("RAW_BACKUP_PATH", "") + _sep
					+ _params.getClientParam("CLIENT_TAG", "") + _sep + subPath;
			System.out.println("Backup path for .raw file: " + rawBackupPath);
			// Create directory
			Files.createDirectories(Paths.get(_sampleLocalPath));
			// Get the remaining information from the message
			_instrumentType = parts[2];
			_searchParam = parts[3];
			_extractParam = _params.getGlobalParam("SEQUEST.msconv_params", "");
			if (_searchParam.indexOf(":") > 0) {
				_searchEngine = _searchParam.substring(0, _searchParam.indexOf(":"));
				_searchParam = _searchParam.substring(_searchParam.indexOf(":") + 1);
			} else {
				_searchEngine = "S";
			}
			_userName = parts[4];
			String msMethod = parts[5];
			String sampleQueueID, expFolder, storeLocation, protocolID, species;
			Vector<String> toWrite = new Vector<String>();
			System.out.println(".raw file path: " + rawBackupPath + _fileExt);
			System.out.println(".zip file path: " + _srBackupPath + ".zip");
			toWrite.add("filename = " + _sample);
			// toWrite.add(".raw file path = " + rawBackupPath + "\\" + _sample
			// + _fileExt); jmb changed 130917: filenames already encoded in
			// variables, just need to specify paths
			toWrite.add(".raw file path = " + rawBackupPath + _fileExt);
			// toWrite.add(".zip file path = " + _srBackupPath + "\\" + _sample
			// + ".zip"); jmb changed 130917
			toWrite.add(".zip file path = " + _srBackupPath + ".zip");
			toWrite.add("username = " + _userName);
			toWrite.add("MS Method = " + msMethod);
			if (parts[6].startsWith("SQ:")) {
				sampleQueueID = parts[6].substring(3);
				toWrite.add("Sample Queue ID = " + sampleQueueID);
			} else {
				expFolder = parts[6];
				storeLocation = parts[7];
				protocolID = parts[8];
				species = parts[9] + "";
				if (species.equals("")) {
					species = "N/A";
				}
				toWrite.add("Experiment Folder = " + expFolder);
				toWrite.add("Storage Location = " + storeLocation);
				toWrite.add("Protocol ID = " + protocolID);
				toWrite.add("Species = " + species);
			}
			toWrite.add("Instrument = " + _instrumentType);
			toWrite.add("SearchEngine = " + _searchEngine);
			Files.write(Paths.get(_sampleLocalPath + ".txt"), toWrite, StandardCharsets.UTF_8);
		} catch (NullPointerException | IndexOutOfBoundsException | IOException e) {
			log("ERROR: COULD NOT PARSE FILE");
			e.printStackTrace();
		}

		String directory = _sampleLocalPath.substring(0, _sampleLocalPath.lastIndexOf(_sep));
		/*
		 * Call the other methods that are needed to process the sample.
		 */
		this.search(_searchEngine);
		// Copy all files into directory _samplePath before zipping
		System.out.println("_sampleLocalPath: " + _sampleLocalPath);
		String[] extns = { ".p2p", ".p2c", ".p1p", ".p1c", ".raw", ".met", ".seq", ".cfg", ".csv" };
		for (String extn : extns) {
			try {
				Files.copy(Paths.get(_sampleLocalPath + extn), Paths.get(_sampleLocalPath), REPLACE_EXISTING);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				System.out.println("Couldn't move " + _sampleLocalPath + extn + " or file was not found.");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		// move mzXML up one directory
		// try {
		// this.log(Paths.get(_sampleLocalPath,_sample+".mzXML").toString());
		// this.log(Paths.get(_sampleLocalPath).toString());
		// Files.move(Paths.get(_sampleLocalPath,_sample+".mzXML"),
		// Paths.get(_sampleLocalPath), REPLACE_EXISTING);
		// } catch (IOException e) {
		// e.printStackTrace();
		// this.log("Error: Couldn't move .mzXML file to leave it exposed
		// outside zip");
		// }
		this.make7Zip();
		this.sendToLoader();
		this.sendToProteome();
		this.deleteFiles();
		// move raw file before deleting everything else
		System.out.println("Moving .raw file to ");
		try {
			Files.copy(Paths.get(_sampleLocalPath + ".raw"), Paths.get(_sampleLocalPath), REPLACE_EXISTING);
		} catch (IOException e) {
			e.printStackTrace();
		}

		this.log("The sample " + _sample + " has finished processing.");
		this.setBusy(false);
	}

	/**
	 * This method downloads the raw file and other associated files from the
	 * AutoUpdate computer. Deprecated 09/01/17
	 * 
	 * @return boolean: download
	 */
	// private boolean downloadFiles() {
	// this.pause("File Download");
	// this.log(" Copying files from AutoUpdate...");
	// String dataPath =
	// _samplePath.substring(_params.getClientParam("LOCAL_PATH", "").length());
	// dataPath = _params.getClientParam("REMOTE_PATH", "") + dataPath;
	// String[] extns = {".p2p", ".p2c", ".p1p", ".p1c", ".raw", ".met", ".seq",
	// ".cfg", ".tmt", ".max"};
	// for (String extn: extns) {
	// try {
	// Files.copy(Paths.get(dataPath+extn), Paths.get(_sampleLocalPath+extn),
	// REPLACE_EXISTING);
	// } catch (AccessDeniedException e) {
	// e.printStackTrace();
	// JOptionPane.showMessageDialog(this, "AutoSearch does not have access to
	// the data.",
	// "Access Denied", JOptionPane.WARNING_MESSAGE);
	// return false;
	// } catch (FileNotFoundException e) {
	// if (extn.equals(".cfg")) {
	// continue;
	// }
	// this.log(" ERROR: Could not find the specified file: " + _sample+extn);
	// System.err.println("Could not find the specified file: " + _sample+extn);
	// return false;
	// } catch (IOException e) {
	// if (extn.equals(".cfg")) {
	// continue;
	// }
	// this.log(" ERROR: Could not download file: " + _sample+extn);
	// System.err.println("Error downloading file: " + _sample+extn);
	// e.printStackTrace();
	// return false;
	// }
	// }
	// this.logNNL("done!");
	// return true;
	// }

	/**
	 * Calls the appropriate search method depending on which database is to be
	 * used after creating the DTA files.
	 * 
	 * @param database
	 * @throws InterruptedException
	 */
	private void search(String database) throws InterruptedException {
		this.makeDTA(); // Make the DTA files
		// We need to pass MQ the directory to be ran
		String directory = _sampleLocalPath.substring(0, _sampleLocalPath.lastIndexOf(_sep));
		if (database.equals("S")) {
			this.sequestSearch();
		} else if (database.equals("M")) {
			this.mascotSearch();
		} else if (database.equals("X")) {
			this.MaxQuantPipeline(directory);
		}
	}

	private void makeDTA() {
		this.pause("Making DTA Files");
		this.log("          Making DTA files...");
		this.log("msconvert params: " + _extractParam);
		String cmd = null;
		/*
		 * Use the new msconvert.exe to create the mgf file
		 */
		ProcessBuilder builder = null;
		if (_fileType.equalsIgnoreCase("THMRAW")) {
			cmd = "\"" + _params.getGlobalParam("SEQUEST.mgf_maker", "") + "\" " + _sampleLocalPath + ".raw" + " --mgf "
					+ _extractParam + " --outdir " + _sampleLocalPath + " --outfile merged";
			List<String> processCall = new ArrayList<>(
					Arrays.asList(_params.getGlobalParam("SEQUEST.mgf_maker", ""), _sampleLocalPath + ".raw", "--mgf"));
			processCall.addAll(Arrays.asList(_extractParam.split(" ")));
			processCall.addAll(Arrays.asList("--outdir", _sampleLocalPath, "--outfile", "merged"));
			builder = new ProcessBuilder(processCall);
			System.out.println("Process sent: " + builder.toString());
		} else if (_fileType.equalsIgnoreCase("mzData")) {
			cmd = "java -Xmx512m -jar" + _appPath + _sep + "ExtractMSMS.jar mzData " + _sampleLocalPath + _fileExt;
			builder = new ProcessBuilder("java", "-Xmx512m", "-jar", Paths.get(_appPath, "ExtractMSMS.jar").toString(),
					"mzData", _sampleLocalPath + _fileExt);
		} else if (_fileType.equalsIgnoreCase("mzML099")) {
			cmd = "java -Xmx512m -jar" + _appPath + _sep + "ExtractMSMS.jar mzML099 " + _sampleLocalPath + _fileExt;
			builder = new ProcessBuilder("java", "-Xmx512m", "-jar", Paths.get(_appPath, "ExtractMSMS.jar").toString(),
					"mzML099", _sampleLocalPath + _fileExt);
		} else if (_fileType.equalsIgnoreCase("mzML100")) {
			cmd = "java -Xmx512m -jar" + _appPath + _sep + "ExtractMSMS.jar mzML100 " + _sampleLocalPath + _fileExt;
			builder = new ProcessBuilder("java", "-Xmx512m", "-jar", Paths.get(_appPath, "ExtractMSMS.jar").toString(),
					"mzML100", _sampleLocalPath + _fileExt);
		} else if (_fileType.equalsIgnoreCase("mzDXML")) {
			cmd = "java -Xmx512m -jar" + _appPath + _sep + "ExtractMSMS.jar mzXML " + _sampleLocalPath + _fileExt;
			builder = new ProcessBuilder("java", "-Xmx512m", "-jar", Paths.get(_appPath, "ExtractMSMS.jar").toString(),
					"mzXML", _sampleLocalPath + _fileExt);
		}
		System.out.println(cmd);
		try {
			Process p = builder.redirectOutput(Redirect.INHERIT).start();
			// Print any print lines from standard out and error out
			BufferedReader bri = new BufferedReader(new InputStreamReader(p.getInputStream()));
			BufferedReader bre = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			String line;
			while ((line = bri.readLine()) != null) {
				System.out.println(line);
			}
			bri.close();
			while ((line = bre.readLine()) != null) {
				System.err.println(line);
			}
			bre.close();
			p.waitFor(); // Maintain thread synchrony

		} catch (IOException e) {
			this.log("          ERROR: Could not create MGF file");
			System.err.println("Could not create MGF file");
			e.printStackTrace();
		} catch (InterruptedException e) {
			this.log("          ERROR: Process creating MGF file was interrupted");
			System.err.println("Process creating MGF file was interrupted");
			e.printStackTrace();
		}
		/*
		 * Split the MGF file into DTAs using perl script
		 */
		cmd = "cmd /C perl C:\\autoSearch\\SplitMGF2DTA.pl --indir " + _sampleLocalPath + " --outdir "
				+ _sampleLocalPath;
		builder = new ProcessBuilder("cmd", "/C", "perl", Paths.get(_appPath, "SplitMGF2DTA.pl").toString(), "--indir",
				_sampleLocalPath, "--outdir", _sampleLocalPath);
		System.out.println(cmd);
		try {
			Process p = builder.redirectOutput(Redirect.INHERIT).start();
			// Print any print lines from standard out and error out
			BufferedReader bri = new BufferedReader(new InputStreamReader(p.getInputStream()));
			BufferedReader bre = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			String line;
			while ((line = bri.readLine()) != null) {
				System.out.println(line);
			}
			bri.close();
			while ((line = bre.readLine()) != null) {
				System.err.println(line);
			}
			bre.close();
			p.waitFor(); // Maintain thread synchrony
		} catch (IOException e) {
			this.log("          ERROR: Could not extract DTA files");
			System.err.println("Could not extract DTA files");
			e.printStackTrace();
		} catch (InterruptedException e) {
			this.log("          ERROR: Process extracting DTA files was interrupted");
			System.err.println("Process extracting DTA files was interrupted");
			e.printStackTrace();
		}
		/*
		 * Create mzXML using msconvert.exe
		 */

		Path rawpath = Paths.get(_sampleLocalPath + ".raw");
		cmd = "\"" + _params.getGlobalParam("SEQUEST.mgf_maker", "") + "\" " + rawpath.toString() + " --mzXML "
				+ _extractParam + " --outdir " + rawpath.getParent().toString();
		// builder = new
		// ProcessBuilder(_params.getGlobalParam("SEQUEST.mgf_maker",""),
		// rawpath.toString(), "--mzXML", "--filter","\"peakPicking", "true",
		// "[1,2]\"", "--outdir", rawpath.getParent().toString());
		List<String> processCall = new ArrayList<>(
				Arrays.asList(_params.getGlobalParam("SEQUEST.mgf_maker", ""), rawpath.toString(), "--mzXML"));
		processCall.addAll(Arrays.asList(_extractParam.split(" ")));
		processCall.addAll(Arrays.asList("--outdir", rawpath.getParent().toString()));
		builder = new ProcessBuilder(processCall);
		this.log("     " + cmd);
		System.out.println(cmd);
		System.out.println(builder.toString());

		try {
			Process p = builder.redirectOutput(Redirect.INHERIT).start();
			// Print any print lines from standard out and error out
			BufferedReader bri = new BufferedReader(new InputStreamReader(p.getInputStream()));
			BufferedReader bre = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			String line;
			while ((line = bri.readLine()) != null) {
				System.out.println(line);
			}
			bri.close();
			while ((line = bre.readLine()) != null) {
				System.err.println(line);
			}
			bre.close();
			p.waitFor(); // Maintain thread synchrony

			// //start 2nd mzXML conversion
			// Process p2 = builder2.redirectOutput(Redirect.INHERIT).start();
			// //Print any print lines from standard out and error out
			// BufferedReader bri2 = new BufferedReader(new
			// InputStreamReader(p2.getInputStream()));
			// BufferedReader bre2 = new BufferedReader(new
			// InputStreamReader(p2.getErrorStream()));
			// String line2;
			// while ((line2 = bri2.readLine()) != null) {
			// System.out.println(line2);
			// }
			// bri2.close();
			// while ((line2 = bre2.readLine()) != null) {
			// System.err.println(line2);
			// }
			// bre2.close();
			// p2.waitFor(); //Maintain thread synchrony

		} catch (IOException e) {
			this.log("          ERROR: Could not create mzXML file");
			System.err.println("Could not create mzXML file");
			e.printStackTrace();
		} catch (InterruptedException e) {
			this.log("          ERROR: Process creating mzXML file was interrupted");
			System.err.println("Process creating mzXML file was interrupted");
			e.printStackTrace();
		}
		this.logNNL("done!");
	}

	private void sequestSearch() {
		// TODO finish this method
	}

	/**
	 * This method runs the Mascot search. It obtains the DTA files, merges them
	 * into MGF, copies the necessary files to the watch folders for the Mascot
	 * Daemon. Once the search is complete, it obtains the XML file and creates
	 * the OUT files.
	 */
	private void mascotSearch() {
		this.pause("MASCOT Search");
		this.log("          Starting MASCOT search...");
		// Delete previous output file if it exists
		try {
			Files.deleteIfExists(Paths.get(this.getAppPath(), "mascotdownloaderoutput.txt"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		/*
		 * ********** DEPRECATED: MGF IS BEING CREATED IN THE DTA STEP
		 *
		 * //Merge DTAs to MGF by running external JAR try { String cmd =
		 * "java -Xmx256m -jar "+_appPath+_sep+"MergeDTA2MGF.jar "
		 * +_sampleLocalPath; if
		 * (System.getProperty("os.name").startsWith("Windows")) { cmd =
		 * "cmd /C "+cmd; } else { cmd = "xterm -c "+cmd; } Process p = new
		 * ProcessBuilder(cmd.split(" ")).start(); //Print any print lines from
		 * standard out and error out BufferedReader bri = new
		 * BufferedReader(new InputStreamReader(p.getInputStream()));
		 * BufferedReader bre = new BufferedReader(new
		 * InputStreamReader(p.getErrorStream())); String line; while ((line =
		 * bri.readLine()) != null) { System.out.println(line); } bri.close();
		 * while ((line = bre.readLine()) != null) { System.err.println(line); }
		 * bre.close(); p.waitFor(); //Maintain thread synchrony } catch
		 * (IOException e) {
		 * this.log("          ERROR: Could not execute JAR MergeDTA2MGF");
		 * System.err.println("Could not execute JAR MergeDTA2MGF");
		 * e.printStackTrace(); } catch (InterruptedException e) { this.
		 * log("          ERROR: Process running JAR MergeDTA2MGF was interrupted"
		 * );
		 * System.err.println("Process running JAR MergeDTA2MGF was interrupted"
		 * ); e.printStackTrace(); }
		 */
		// Create folder in Mascot
		DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
		String mgf = _sample + "_" + dateFormat.format(new Date()) + ".mgf";
		Path mgfFolder = Paths.get(_params.getGlobalParam("SEQUEST.MascotDaemonFolder", ""),
				_searchParam.substring(0, _searchParam.lastIndexOf(".")), mgf);
		this.log("Moving mgf file to " + mgfFolder);
		try {
			Files.move(Paths.get(_sampleLocalPath, "merged.mgf"), mgfFolder);
		} catch (IOException e) {
			this.log("          ERROR: Could not copy merged MGF file");
			System.err.println("Could not copy merged MGF file");
			e.printStackTrace();
		}
		// Mascot is running...
		this.log("          Mascot is running. Waiting for results...");
		Path resultFile = Paths.get(this.getAppPath(), "mascotdownloaderoutput.txt");
		while (!Files.exists(resultFile)) {
			// While results are not available, put the thread to sleep
			try {
				Thread.sleep(60000);
			} catch (InterruptedException e) {
				this.log("          ERROR: Error while waiting for Mascot results");
				System.err.println("Error while waiting for Mascot results");
				e.printStackTrace();
			}
		}
		// Mascot has finished. Obtain results
		try {
			Files.move(mgfFolder, Paths.get(_sampleLocalPath, mgf));
		} catch (IOException e) {
			this.log("          ERROR: Could not copy results");
			System.err.println("Could not copy results");
			e.printStackTrace();
		}
		this.log("          Mascot has finished");
		// Find results XML
		Path resultXML = null;
		Path resultCSV = null;
		pathToCSV = null;
		try {
			List<String> lines = Files.readAllLines(resultFile, StandardCharsets.UTF_8);
			String line = lines.get(0);
			if (line.startsWith("OK")) {
				resultXML = Paths.get(_sampleLocalPath, _sample + ".xml");
				resultCSV = Paths.get(_sampleLocalPath, _sample + ".csv");

				pathToCSV = Paths.get(line.substring(4, line.lastIndexOf(".") - 4) + ".dat.csv").toString();

				Files.move(Paths.get(line.substring(4)), resultXML, REPLACE_EXISTING);
			} else {
				this.log("          ERROR: XML is in error state - " + line);
				this.log("			Trying to move .csv again");

			}

			//
			// .dat.csv file is generated after .xml but not before the status
			// text file (mascotdownloaderoutput.txt) is
			// created. Below is a checker that maxes out after 60 tries (10
			// minutes), before looking for the file
			// If the file is found, then it will wait for 1 minute before
			// moving the file, just in case the csv is still
			// being generated (generation of csv should only take around an
			// extra 10 seconds after the file appears in file
			// system).

			int tmpCount = 0;
			while (Files.notExists(Paths.get(line.substring(4, line.lastIndexOf(".") - 4) + ".dat.csv"))) {
				if (tmpCount == 0) {
					this.log("Waiting for CSV file to be generated");
				}
				try {
					Thread.sleep(10000); // 1000 milliseconds is one second.
				} catch (InterruptedException ex) {
					Thread.currentThread().interrupt();
				}
				if (tmpCount == 60) {
					break;
				}
				tmpCount++;
			}
			if (Files.exists(Paths.get(line.substring(4, line.lastIndexOf(".") - 4) + ".dat.csv"))) {
				this.log("CSV file found. Delaying by 1 minute so CSV can be completely generated.");
				try {
					Thread.sleep(60000); // 1000 milliseconds is one second.
				} catch (InterruptedException ex) {
					Thread.currentThread().interrupt();
				}
			}

			this.log("Moving the csv file at" + Paths.get(line.substring(4, line.lastIndexOf(".") - 4) + ".dat.csv"));

			Files.move(Paths.get(line.substring(4, line.lastIndexOf(".") - 4) + ".dat.csv"), resultCSV,
					REPLACE_EXISTING);

		} catch (IOException e) {
			this.log("          ERROR: Could not copy XML file");
			System.err.println("Could not copy XML file");
			e.printStackTrace();
		}
		// Make OUT files from XML. If the first attempt fails, reformat the XML
		// to remove illegal chars and try again
		this.log("          Making OUT files from Macot Results...");
		for (int attempt = 0; attempt <= 1; attempt++) {
			try {
				Process p = Runtime.getRuntime()
						.exec("java -Xmx1536m -jar " + _appPath + _sep + "pepXML2OUT_jmb.jar " + resultXML.toString());
				// Print any print lines from standard out and error out
				BufferedReader bri = new BufferedReader(new InputStreamReader(p.getInputStream()));
				BufferedReader bre = new BufferedReader(new InputStreamReader(p.getErrorStream()));
				String line;
				while ((line = bri.readLine()) != null) {
					System.out.println(line);
				}
				bri.close();
				while ((line = bre.readLine()) != null) {
					System.err.println(line);
				}
				bre.close();
				p.waitFor(); // Maintain thread synchrony
				int successCheck = p.exitValue();
				try {
					if (successCheck != 0) {
						throw new InterruptedException();
					} else {
						break;
					} // if pepXML2OUT is successful, no need for second attempt
				} catch (InterruptedException e) {
					this.log(
							"          ERROR: pepXML2OUT_jmb encountered an error and terminated prematurely. Attempting to correct XML formatting.");
					System.err.println(
							"pepXML2OUT_jmb encountered an error and terminated prematurely. Attempting to correct XML formatting.");
					reformatXML(resultXML.toString());
				}

			} catch (IOException e) {
				this.log("          ERROR: Could not execute JAR pepXML2OUT_jmb");
				System.err.println("Could not execute JAR pepXML2OUT_jmb");
				e.printStackTrace();
				// FEATURE REQUEST: Catch errors and break the program out if
				// OUT file generation unsuccessful FLAG.
				// Catch an error in PepXML2OUT and let it know that if reformat
				// XMl fails then that was its last chance, and should die
			} catch (InterruptedException e) {
				this.log("          ERROR: Process running JAR pepXML2OUT_jmb was interrupted");
				System.err.println("Process running JAR pepXML2OUT_jmb was interrupted");
				e.printStackTrace();
			}
		}
		this.logNNL("done!");
	}

	/**
	 * This method attempts to rewrite malformed XMLs by stripping away all
	 * non-UTF8 chars
	 */
	private void reformatXML(String xmlin) {
		CharsetDecoder utf8Decoder = Charset.forName("UTF-8").newDecoder();
		utf8Decoder.onMalformedInput(CodingErrorAction.IGNORE);
		utf8Decoder.onUnmappableCharacter(CodingErrorAction.IGNORE);

		String xmltemp = xmlin.substring(0, xmlin.lastIndexOf(".")) + "_temp.xml";
		String xmlold = xmlin.substring(0, xmlin.lastIndexOf(".")) + "_old.xml";

		try (PrintWriter writer = new PrintWriter(xmltemp, "UTF-8")) {
			try (BufferedReader br = new BufferedReader(new FileReader(xmlin))) {
				for (String line; (line = br.readLine()) != null;) {
					byte[] b = line.getBytes();
					ByteBuffer buf = ByteBuffer.wrap(b);
					CharBuffer parsed = utf8Decoder.decode(buf);
					writer.println(parsed);
				}
				br.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			writer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		File fileIn = new File(xmlin);
		File fileTemp = new File(xmltemp);

		fileIn.renameTo(new File(xmlold));
		fileTemp.renameTo(new File(xmlin));
	}

	/**
	 * This method adds all the files related to this sample to a compressed zip
	 * file for transfer to AutoLoad
	 */
	// private void zipFiles() { // deprecated--use make7Zip for zipfiles now
	// this.pause("Zipping files");
	// this.log(" Zipping files...");
	// File zip = new File(_sampleLocalPath+".zip");
	// byte[] buffer = new byte[4096];
	// Path baseDir = Paths.get(_sampleLocalPath.substring(0,
	// _sampleLocalPath.lastIndexOf(_sep)));
	// System.out.println("baseDir: " + baseDir);
	// this.getAllFiles(baseDir);
	// try{
	// ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zip));
	// for (Path path:_fileList){
	// Path relPath = baseDir.relativize(path);
	// System.out.println("Adding "+relPath+" to zip folder");
	// if (Files.isDirectory(path)) {
	// out.putNextEntry(new ZipEntry(relPath.toString()));
	// }
	// else {
	// InputStream in = Files.newInputStream(path);
	// out.putNextEntry(new ZipEntry(relPath.toString()));
	// int len;
	// while((len = in.read(buffer)) > 0){
	// out.write(buffer, 0, len);
	// }
	// out.closeEntry();
	// in.close();
	// }
	// }
	// out.close();
	// } catch (IOException e) {
	// this.log(" ERROR: Could not zip files");
	// System.err.println("Error zipping files");
	// e.printStackTrace();
	// }
	// this.logNNL("done!");
	// }
	private void make7Zip() {
		this.pause("Zipping files");
		this.log("          Zipping files...");
		String cmd = null;
		Path zipPath = Paths.get(_sampleLocalPath + ".zip");
		cmd = "\"" + _params.getGlobalParam("SEQUEST.7ziplocation", "C:\\Program Files\\7zip\\7za.exe") + "\" a"
				+ " -tzip " + zipPath.toString() + " \"" + _sampleLocalPath + "*\"";
		ProcessBuilder builder = null;
		builder = new ProcessBuilder(_params.getGlobalParam("SEQUEST.7ziplocation", "C:\\Program Files\\7zip\\7za.exe"),
				"a", "-tzip", "\"" + zipPath.toString() + "\"", "\"" + _sampleLocalPath + "*\"");
		System.out.println("7zip command: " + builder.command());
		this.log("7zip command: " + builder.command());
		List<String> cmdList = builder.command();
		String cmdPB = "";
		this.log("        " + cmdPB);
		try {
			Process p = builder.redirectOutput(Redirect.INHERIT).start();
			// Print any print lines from standard out and error out
			BufferedReader bri = new BufferedReader(new InputStreamReader(p.getInputStream()));
			BufferedReader bre = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			String line;
			while ((line = bri.readLine()) != null) {
				System.out.println(line);
			}
			bri.close();
			while ((line = bre.readLine()) != null) {
				System.err.println(line);
			}
			bre.close();
			p.waitFor(); // Maintain thread synchrony

		} catch (IOException e) {
			this.log("          ERROR: 7Zip Could not create zip file");
			System.err.println("Could not create zip file");
			e.printStackTrace();
		} catch (InterruptedException e) {
			this.log("          ERROR: Process creating zip file was interrupted");
			System.err.println("Process creating zip file was interrupted");
			e.printStackTrace();
		}

		this.logNNL("done!");
	}

	// Deprecated 09/1/17
	// private void getAllFiles(Path dir) {
	// _fileList.clear();
	// try {
	// DirectoryStream<Path> files = Files.newDirectoryStream(dir);
	// for (Path file:files) {
	// if (file.toString().contains(_sample) &&
	// !file.toString().contains(".zip")) {
	// _fileList.add(file);
	// if (Files.isDirectory(file)) {
	// this.getAllFiles(file);
	// }
	// }
	// }
	// } catch (IOException e) {
	// e.printStackTrace();
	// }
	//
	// }
	/**
	 * This method sends the zip file to AutoLoad, as well as the filename so
	 * that AutoLoad can put it in its sample queue list. The filename is sent
	 * over a Socket which is only connected when AutoSearch has a file to send.
	 */
	private void sendToLoader() {
		this.pause("Send to Loader");
		this.log("          Beginning transfer of files to AutoLoad...");
		// Copy zip file to AutoLoad
		try {
			// Note: Does not use a socket. Uses Java Transfer system.
			// Feature Request: Use Java Files.transfer() for Proteome backup
			// method.
			Files.copy(Paths.get(_sampleLocalPath + ".zip"),
					Paths.get(_params.getGlobalParam("LOADER.Root", ""), _sample + ".zip"), REPLACE_EXISTING);
			Files.copy(Paths.get(_sampleLocalPath + ".txt"),
					Paths.get(_params.getGlobalParam("LOADER.Root", ""), _sample + ".txt"), REPLACE_EXISTING);

		} catch (IOException e) {
			this.log("          ERROR: Could not send .zip file to AutoLoad");
			System.err.println("Could not send .zip file to AutoLoad");
			e.printStackTrace();
		}
		// Send filename to AutoLoad over Socket connection
		try {
			Socket toLoaderSocket = new Socket(_params.getGlobalParam("LOADER", ""),
					Integer.parseInt(_params.getGlobalParam("LOADER.Port", "1003")));
			PrintWriter out = new PrintWriter(toLoaderSocket.getOutputStream(), true);
			out.println(_sample);
			out.close();
			toLoaderSocket.close();
		} catch (NumberFormatException | IOException e) {
			this.log("          ERROR: Could not send file to AutoLoad");
			System.err.println("Could not send filename over Socket connection to AutoLoad");
			e.printStackTrace();
		}
	}

	/**
	 * Sends all the files associated with this sample to the backup server
	 */
	private void sendToProteome() {
		this.pause("Sending to Backup Computer");
		this.log("          Sending to backup computer...");
		try {
			DirectoryStream<Path> files = Files.newDirectoryStream(
					Paths.get(_sampleLocalPath.substring(0, _sampleLocalPath.lastIndexOf(_sep))), _sample + ".*");
			for (Path file : files) {
				String extn = file.toString().substring(file.toString().lastIndexOf("."));
				// Feature Request: Change _srBackupPath to be an open folder on
				// Proteome.
				Files.copy(file, Paths.get(_srBackupPath, _sample + extn), REPLACE_EXISTING);
			}
		} catch (IOException e) {
			System.err.println("Could not back up " + _sample);
			e.printStackTrace();
		}
		this.logNNL("done!");
	}

	/**
	 * This method deletes all the local files
	 */
	private void deleteFiles() {
		this.pause("File Deletion");
		this.log("          Deleting files...");
		try {
			DirectoryStream<Path> files = Files.newDirectoryStream(
					Paths.get(_sampleLocalPath.substring(0, _sampleLocalPath.lastIndexOf(_sep)), _sample)); // jmb
																											// substitute
																											// this
																											// for
																											// the
																											// call
																											// above
			this.log("			Deleting "
					+ Paths.get(_sampleLocalPath.substring(0, _sampleLocalPath.lastIndexOf(_sep)), _sample));
			for (Path file : files) {
				FullClean(file.toFile());
			}
			Paths.get(_sampleLocalPath.substring(0, _sampleLocalPath.lastIndexOf(_sep)), _sample).toFile().delete();
			files.close();
		} catch (IOException e1) {
			System.err.println("Could not delete " + _sample);
			e1.printStackTrace();
		}
		this.logNNL("done!");
	}

	// ==================== HELPER METHODS & CLASSES ====================\\
	/**
	 * Convenience method to put the thread to sleep if the application is
	 * paused.
	 * 
	 * @param String
	 *            method - method name for debugging purposes
	 */
	private void pause(String method) {
		int min = 0;
		while (this.isPaused()) {
			try {
				System.out.println("AutoSearch has been paused at " + method + " for " + min + " minutes");
				min++;
				Thread.sleep(60000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * This class implements the action method for the priority button. It
	 * brings up the panel for setting the priority of clients.
	 * 
	 * @author njooma
	 *
	 */
	private class PriorityButtonListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			JButton source = (JButton) e.getSource();
			if (source.getText().equals("Set Priority")) {
				source.setText("Close Client List");
				_clientPanel.setVisible(true);
			} else {
				source.setText("Set Priority");
				_clientPanel.setVisible(false);
			}
		}
	}

	/**
	 * This class sets the _isPaused boolean to either true or false depending
	 * on the paused state.
	 * 
	 * @author njooma
	 *
	 */
	private class PauseListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			JButton source = (JButton) e.getSource();
			if (source.getText().equals("Pause")) {
				source.setText("Resume");
				log("AutoSearch is paused...");
				_isPaused = true;
			} else {
				source.setText("Pause");
				log("AutoSearch has been resumed");
				_isPaused = false;
			}
		}
	}

	/**
	 * This method changes the priority of the clients when the user changes
	 * them in the table using the dropdown menus.
	 * 
	 * @author njooma
	 *
	 */
	private class PriorityListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			@SuppressWarnings("unchecked")
			JComboBox<String> jcb = (JComboBox<String>) e.getSource();
			int i = _clientList.getSelectedRow();
			if (i > -1) {
				String str = (String) jcb.getSelectedItem();
				str = str.split("-")[0];
				int priority = Integer.parseInt(str);
				_clients[i].setPriority(priority);
				System.out.println(_clients[i].getName() + ": " + _clients[i].getPriority());
			}
		}
	}

	/**
	 * Main method for running the MaxQuant Pipeline. This method runs the
	 * program itself (including preparing the mqpar), and then calls other
	 * methods to finish processing. BrianO
	 * 
	 * @throws InterruptedException
	 */
	private void MaxQuantPipeline(String directory) throws InterruptedException {
		this.log("Entering MaxQuant Pipeline");
		try {
			// Getting the directory in which all the relevant
			// files live on the computer running autoSearch;
			// Copy the specified mqpar file into our direcotry
			Files.copy(Paths.get(_params.getGlobalParam("SEQUEST.MaxQuantParams", "C:" + _sep + "maxquant parameter")
					+ _sep + _searchParam), Paths.get(directory + _sep + "mqpar.xml"), REPLACE_EXISTING);
			// Update the given mqpar with the raw file
			MqparEditor(directory, _sample);
			// Opening the command line and running MaxQuantCommand with the
			// path to
			// the parameters file (always of the name 'mqpar.xml', else
			// MaxQuant software breaks)
			this.log("MaxQuant running...");
			String pathToMaxQuantProgram = _params.getGlobalParam("SEQUEST.MaxQuantPath",
					"C:" + _sep + "autoSearch" + _sep + "MaxQuant" + _sep + "bin" + _sep + "MaxQuantCmd.exe");
			Runtime rt = Runtime.getRuntime();
			final Process pr = rt.exec(pathToMaxQuantProgram + " " + directory + _sep + "mqpar.xml");
			BufferedReader bri = new BufferedReader(new InputStreamReader(pr.getInputStream()));
			BufferedReader bre = new BufferedReader(new InputStreamReader(pr.getErrorStream()));
			BufferedWriter inputStreamOut = new BufferedWriter(new FileWriter(new File("C:\\Desktop\\inputStreamOut.txt")));
			BufferedWriter outputStreamOut = new BufferedWriter(new FileWriter(new File("C:\\Desktop\\outputStreamOut.txt")));
			String line;
			while ((line = bri.readLine()) != null) {
				inputStreamOut.write(line);
			}
			bri.close();
			inputStreamOut.close();
			while ((line = bre.readLine()) != null) {
				System.err.println(line);
			}
			bre.close();
			outputStreamOut.close();
			// FEATURE REQUEST Take output of MaxQuantCmd and relay to text
			// file.
			// We will run the thread and wait for completion
			new Thread(new Runnable() { // Get rid of this and just have the
										// process later
				public void run() {
					BufferedReader input = new BufferedReader(new InputStreamReader(pr.getInputStream()));
					String line = null;
					try {
						while ((line = input.readLine()) != null)
							System.out.println(line);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}).start();

			pr.waitFor();

			Files.move(Paths.get(directory + _sep + "mqpar.xml", ""),
					Paths.get(directory + _sep + _sample + _sep + "mqpar.xml", ""), REPLACE_EXISTING);
			this.log("MaxQuant processing finished");
			this.log("Extracting data from output files");
			// //Now we will extract the relevant info from the output files.
			// We want to import four important files from the MQ run, but first
			// we must standardize them (done below) by splicing out any field
			// that
			// could be different depending on a given run.
			MsmsEditor(directory);
			ProteinGroupsEditor(directory);
			ParameterEditor(directory);
			SummaryEditor(directory);
		} catch (IOException e) {
			e.printStackTrace();
		}
		// } catch (InterruptedException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }

	}

	/**
	 * Edits a base mqpar fuile to include the raw file path
	 * 
	 * The mqpar.xml file is the sole input to running MaxQuant from the command
	 * line. It contains all of the necessary information to run quantitation,
	 * such as the path to the raw and fasta files, what enzymes/mods/isobaric
	 * labels are used, information about peaks, calibration, and much more. If
	 * something goes wrong or MaxQuantCmd stops working, something must be
	 * wrong with the mqpar file.
	 * 
	 * Template MQPAR.xml files generated manually by the MaxQuant GUI and a
	 * user, then the user removes the raw file section under filepaths,
	 * including the string tags. It is then placed in maxquant parameters and
	 * it is copied to the maxquant directory to be run.
	 * 
	 * @param directory-Directory
	 *            that the raw file lives in
	 * @param fileName-Unique
	 *            name of the file that's being processed BrianO
	 */

	private void MqparEditor(String directory, String fileName) {

		try {
			// Open mqpar.xml and prepare it for alteration
			String filePath = directory + _sep + "mqpar.xml"; // later replace
																// with
																// directory +
																// mqpar.xml
			File xmlFile = new File(filePath);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder;
			dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(xmlFile);

			// This sets the path to the raw file as a text node
			Element toAppendRawFile = doc.createElement("string");
			toAppendRawFile.appendChild(doc.createTextNode(directory + _sep + fileName + ".raw"));

			// append node
			org.w3c.dom.Node parentNodeRaw = doc.getElementsByTagName("filePaths").item(0);
			parentNodeRaw.appendChild(toAppendRawFile);

			// write the content into xml file
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(new File(filePath));
			transformer.transform(source, result);

			System.out.println("Done");

		} catch (ParserConfigurationException | TransformerException | SAXException | IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Depending on what the specs of our experiment are, we don't know how many
	 * mods or reporter ions we will have in msms.txt. This function takes the
	 * msms file, removes all columns related to mods, and standardizes the
	 * number of reporter ions to 10 so that the FMImport is not thrown off (if
	 * tmt).
	 * 
	 * Modified Sequence is also wrong coming directly out of maxquant, so we
	 * change it to correct laboratory format and replace all modification
	 * mentions (like (ox) for oxidation) to be some random special character
	 * which is assigned upon first seeing it. (eg. (ox) is assigned to $, (ca)
	 * assigned to ^, etc. going forward). Only exception is Phospho, which is
	 * alway sthe asterisk
	 * 
	 * FEATURE REQUEST- Communicate PLEX level and TMT as TMT6, TMT8
	 * 
	 * @param directory-
	 *            The directory that MQ occurs in BrianO
	 */
	private void MsmsEditor(String directory) {
		try {

			boolean tmt = false; // Flag fix this

			String filePath = directory + _sep + "combined" + _sep + "txt" + _sep + "msms.txt";
			BufferedReader pepData = new BufferedReader(new FileReader(new File(filePath)));
			BufferedWriter summaryWriter = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(directory + _sep + _sample + _sep + "FMImportmsms.txt"), "utf-8"));
			// Take the first line, which is a header, and
			// find the indices of relevant info to display in layout
			// We want to eliminate anything with mods (a variable number of
			// fields) so
			// that we can have a standard number of fields.
			String line = pepData.readLine();
			String[] titles = line.split("\\t");
			List<String> sequenceList = Arrays.asList(titles);
			int modifiedSequenceIndex = sequenceList.indexOf("Modified sequence"); // Mod
																					// fields
																					// comes
																					// after
																					// this
			int proteinsIndex = sequenceList.indexOf("Proteins"); // mod fields
																	// comes
																	// before
																	// this
			int reverseIndex = sequenceList.indexOf("Reverse"); // Need to alter
																// this to R/F
			int allModifiedSequenceIndex = sequenceList.indexOf("All modified sequences"); // reporter
																							// ion
																							// data
																							// comes
																							// after
																							// this
			int idIndex = sequenceList.indexOf("id");
			int evidenceIndex = sequenceList.indexOf("Evidence ID"); // Site IDs
																		// of
																		// mods
																		// come
																		// after
																		// this.
			// Now we will loop through to find the indices (if they exist) of
			// the intensities of
			// the up to 10 reporter ions.

			int count = 0;
			int[] reporterIonCorrectedIndices = new int[10]; // FEATURE REQUEST
																// parametrize
			int[] reporterIonIndices = new int[10];
			// We will now find the indices of the reporter ion intensities. In
			// the event of less than 10, we add
			// NaN up to 10 later on so we can have a standard number of fields.
			// The program knows this is the case when
			// reporter ion indices in the arrays above are equal to 0 (when you
			// instantiate an array, it's default values
			// are 0, meaning they havent been altered.)
			while (count < 10) {
				// If Reporter intensity exists, so will corrected
				if (sequenceList.contains("Reporter intensity " + count)) {
					int reporterIonCorrectedIndex = sequenceList.indexOf("Reporter intensity corrected " + count);
					reporterIonCorrectedIndices[count] = reporterIonCorrectedIndex;
					int reporterIonIndex = sequenceList.indexOf("Reporter intensity " + count);
					reporterIonIndices[count] = reporterIonIndex;
					count++;
				} else {
					count++;
				}
			}

			Stack<String> specialChars = new Stack<String>();
			// Collection of characters that we will take from when assigning to
			// a
			// specific mod in the modified sequence
			specialChars.push("&");
			specialChars.push("^");
			specialChars.push("%");
			specialChars.push("$");
			specialChars.push("#");
			specialChars.push("@");
			specialChars.push("!");
			HashMap<String, String> modToCharacter = new HashMap<String, String>(); // Map
																					// for
																					// modifications
																					// to
																					// special
																					// characters.
			modToCharacter.put("(ph)", "*");

			// Now that we have all the indices of our information, we loop
			// through each line
			// in the output file and snag all of them, and combine them
			// together in a single line
			while ((line = pepData.readLine()) != null) {
				String[] sequenceInfo = line.split("\t");

				for (int i = 0; i < modifiedSequenceIndex; i++) { // writes
																	// beginning
																	// of msms,
					summaryWriter.write(sequenceInfo[i] + "\t"); // up to
																	// modified
																	// sequence
																	// field
				}

				// -----------------------Alter modified
				// sequence--------------------------------------
				String modSequence = sequenceInfo[modifiedSequenceIndex];
				int modSequencelength = modSequence.length();
				modSequence = modSequence.substring(0, 1) + "." // adds _. ._ on
																// both sides,
																// instead of _
						+ modSequence.substring(1, modSequencelength - 1) + "._";
				// Now we have to search through the modified sequence for
				// anything
				// of the form (xx) with a regular expression, which implies a
				// mod. We save all of our
				// matches, align each type of mod with a special character in
				// HashMap if not done
				// already, and then replace all them in the string. Might seem
				// laborious
				// but because we have no idea what mods might come up, this is
				// unavoidable.
				List<String> modsList = new ArrayList<String>();
				Matcher parens = Pattern.compile("\\((.*?)\\)").matcher(modSequence);
				while (parens.find()) {
					modsList.add(parens.group());
				}

				for (String mod : modsList) { // if mod not assigned already,
												// assign it.
					if (!modToCharacter.containsKey(mod)) {
						modToCharacter.put(mod, specialChars.pop());
					}

				}
				for (String mod : modsList) { // Replace all the mods in
												// expression with their chars
					modSequence = modSequence.replaceAll("\\(" + mod + "\\)", modToCharacter.get(mod));
				}
				summaryWriter.write(modSequence + "\t"); // Write the newly
															// modded sequence

				// ------------------------- Modified sequence end
				// -------------------------------
				for (int i = proteinsIndex; i < reverseIndex; i++) {
					summaryWriter.write(sequenceInfo[i] + "\t");
				}
				// If there happens to be a reverse hit, it will show up as +
				// instead of R, so we fix
				if (sequenceInfo[reverseIndex].equals("+")) {
					summaryWriter.write("R" + "\t");
				} else {
					summaryWriter.write("F" + "\t");
				}
				// writes between reverse and the reporter ions
				for (int i = reverseIndex + 1; i <= allModifiedSequenceIndex; i++) {
					summaryWriter.write(sequenceInfo[i] + "\t");
				}

				// Now adding all the values for reporter ion intensities,
				// corrected and uncorrected
				if (tmt) {
					for (int ionIndex : reporterIonCorrectedIndices) {
						if (ionIndex == 0) { // only occurs if no value for a
												// given reporter ion in array
							summaryWriter.write("NaN" + "\t");
						} else {
							summaryWriter.write(sequenceInfo[ionIndex] + "\t");
						}
					}
					for (int ionIndex : reporterIonIndices) {
						if (ionIndex == 0) {
							summaryWriter.write("NaN" + "\t");
						} else {
							summaryWriter.write(sequenceInfo[ionIndex] + "\t");
						}
					}
					// In the case that there are no reporter ions, the fields
					// Reporter PIF and
					// Reporter fraction won't exist either, throwing our import
					// off. so we add these in as Nan
					if (reporterIonIndices[0] == 0) {
						summaryWriter.write("NaN" + "\t");
						summaryWriter.write("NaN" + "\t");
					} else {
						summaryWriter.write(sequenceInfo[sequenceList.indexOf("Reporter PIF")] + "\t");
						summaryWriter.write(sequenceInfo[sequenceList.indexOf("Reporter fraction")] + "\t");
					}
				}
				// writes from id (after reporter ions) to evidence, the final
				// column we need
				for (int i = idIndex; i <= evidenceIndex; i++) {
					summaryWriter.write(sequenceInfo[i] + "\t");
				}

				summaryWriter.write("\r\n");

			}
			summaryWriter.flush();
			summaryWriter.close();
			pepData.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	/**
	 * Writes the important section of the Summary output of MaxQuant, ie the
	 * first 2 lines as single fields for junk drawer import
	 * 
	 * @param directory
	 *            - The directory that MaxQuant ran in BrianO
	 */
	private void SummaryEditor(String directory) {
		try {
			String filePath = directory + _sep + "combined" + _sep + "txt" + _sep + "summary.txt";
			BufferedReader globalData = new BufferedReader(new FileReader(new File(filePath)));
			BufferedWriter globalSummaryWriter = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(directory + _sep + _sample + _sep + "FMImportsummary.txt"), "utf-8"));
			String[] headers = globalData.readLine().split("\t");
			for (String header : headers) {
				globalSummaryWriter.write(header + " ");
			}
			globalSummaryWriter.write("\t");
			String[] values = globalData.readLine().split("\t");
			for (String value : values) {
				if (value.equals("")) {
					globalSummaryWriter.write("blank "); // Placeholder to keep
															// alignment
				} else {
					globalSummaryWriter.write(value + " ");
				}
			}
			globalSummaryWriter.flush();
			globalSummaryWriter.close();
			globalData.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Parameters as an output of MQ is formatted as 2 columns, not rows, so we
	 * transpose it by saving all of both columns and writing them each as one
	 * field
	 * 
	 * @param directory
	 *            - directory that MQ occurs
	 */
	private void ParameterEditor(String directory) {
		try {
			String filePath = directory + _sep + "combined" + _sep + "txt" + _sep + "parameters.txt";
			BufferedReader globalData = new BufferedReader(new FileReader(new File(filePath)));
			BufferedWriter globalParameterWriter = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(directory + _sep + _sample + _sep + "FMImportparameters.txt"), "utf-8"));
			String line = globalData.readLine(); // Get past the header
			StringBuilder headers = new StringBuilder();
			StringBuilder values = new StringBuilder();
			while ((line = globalData.readLine()) != null) {
				String[] parameters = line.split("\t");
				headers.append(parameters[0] + " ");
				if (parameters.length == 2) {
					values.append(parameters[1] + " ");
				} else {
					values.append("blank "); // placeholder for alignment
				}

			}
			globalParameterWriter.write(headers.toString() + "\t");
			globalParameterWriter.write(values.toString());
			globalData.close();
			globalParameterWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Depending on what the specs of our experiment are, we don't know how many
	 * mods or reporter ions we will have in proteingroups.txt. This function
	 * takes that file, removes all columns related to mods, and standardizes
	 * the number of reporter ions to 10.
	 * 
	 * @param directory
	 *            - Directory that MQ runs in BrianO
	 */
	private void ProteinGroupsEditor(String directory) {
		try {
			boolean tmt = false; // Flag fix this
			String filePath = directory + _sep + "combined" + _sep + "txt" + _sep + "proteinGroups.txt";
			BufferedReader proteinData = new BufferedReader(new FileReader(new File(filePath)));
			BufferedWriter summaryWriter = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(directory + _sep + _sample + _sep + "FMImportProteinGroups.txt"), "utf-8"));
			// Take the first line, which is a header, and
			// find the indices of relevant info to display in layout
			// We want to eliminate anything with mods (a variable number of
			// fields) so
			// that we can have a standard number of fields.
			String line = proteinData.readLine();
			String[] titles = line.split("\\t");
			List<String> sequenceList = Arrays.asList(titles);
			int scoreIndex = sequenceList.indexOf("Score"); // reporter info
															// comes after this
			int msmsCountIndex = sequenceList.indexOf("MS/MS count"); // reporter
																		// info
																		// comes
																		// before
																		// this
			int bestMSMSIndex = sequenceList.indexOf("Best MS/MS");// Last index
																	// we want
			int count = 0;
			int[] reporterIonCorrectedIndices = new int[10];
			int[] reporterIonIndices = new int[10];
			int[] reporterIonCountIndices = new int[10];
			// We will now find the indices of the reporter ion intensities.
			// In the event of less than 10, we add NaN up to 10 later on so we
			// can have a standard number of fields. The program knows this is
			// the case
			// when reporter ion indices in the arrays above are equal to 0
			// (when you instantiate
			// an array, it's default values are 0, meaning they havent been
			// altered.)
			while (count < 10) {
				// If Reporter intensity exists, so will corrected and their
				// counts
				if (sequenceList.contains("Reporter intensity " + count)) {
					int reporterIonCorrectedIndex = sequenceList.indexOf("Reporter intensity corrected " + count);
					reporterIonCorrectedIndices[count] = reporterIonCorrectedIndex;
					int reporterIonIndex = sequenceList.indexOf("Reporter intensity " + count);
					reporterIonIndices[count] = reporterIonIndex;
					int reporterIonCountIndex = sequenceList.indexOf("Reporter intensity count " + count);
					reporterIonCountIndices[count] = reporterIonCountIndex;
					count++;
				} else {
					count++;
				}
			}
			// Now that we have all the indices of our information, we loop
			// through each line
			// in the output file and snag all of them, and combine them
			// together in a single line
			while ((line = proteinData.readLine()) != null) {
				String[] sequenceInfo = line.split("\t");

				if (!tmt) { // If lfq or silac, we take all the data except for
							// the mod info at the end
					for (int i = 0; i <= bestMSMSIndex; i++) {
						if (sequenceInfo.length > i) {
							summaryWriter.write(sequenceInfo[i] + "\t");
						}
					}
					summaryWriter.write("\r\n");
				}

				else {// If tmt, we have to standardize number of reporter ions
						// for FM table
					for (int i = 0; i <= scoreIndex; i++) { // writes beginning
															// of proteing
															// Groups
						summaryWriter.write(sequenceInfo[i] + "\t"); // up to
																		// modified
																		// sequence
																		// field
					}
					// Now adding all the values for reporter ion intensities,
					// corrected, uncorrected, and counts, in correct order.
					for (int ionIndex : reporterIonCorrectedIndices) {
						if (ionIndex == 0) { // only occur if no value for a
												// reporter ion in the array.
							summaryWriter.write("NaN" + "\t");
						} else {
							summaryWriter.write(sequenceInfo[ionIndex] + "\t");
						}
					}
					for (int ionIndex : reporterIonIndices) {
						if (ionIndex == 0) {
							summaryWriter.write("NaN" + "\t");
						} else {
							summaryWriter.write(sequenceInfo[ionIndex] + "\t");
						}
					}
					for (int ionIndex : reporterIonCountIndices) {
						if (ionIndex == 0) {
							summaryWriter.write("NaN" + "\t");
						} else {
							summaryWriter.write(sequenceInfo[ionIndex] + "\t");
						}
					}
					if (reporterIonIndices[0] == 0) { // If no reporter ions,
														// Intensity field won't
						summaryWriter.write("NaN" + "\t"); // exist, throwing
															// our import off.
															// so we Nan
					} else {
						summaryWriter.write(sequenceInfo[sequenceList.indexOf("Intensity")] + "\t");

						for (int i = msmsCountIndex; i <= bestMSMSIndex; i++) { // writes
																				// from
																				// Proteins
																				// (directly
							summaryWriter.write(sequenceInfo[i] + "\t"); // after
																			// the
																			// mods)
																			// to
																			// reverse
																			// index
						}
					}

					summaryWriter.write("\r\n");
				}
			}
			summaryWriter.flush();
			summaryWriter.close();
			proteinData.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void FullClean(File directory) {
		if (directory.isDirectory()) {
			for (File f : directory.listFiles()) {
				FullClean(f);
			}
		}
		directory.delete();
	}
}
