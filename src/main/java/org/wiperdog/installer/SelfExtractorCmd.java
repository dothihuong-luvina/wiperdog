package org.wiperdog.installer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.wiperdog.installer.internal.InstallerUtil;
import org.wiperdog.installer.internal.InstallerXML;
import org.wiperdog.installer.internal.XMLErrorHandler;


/**
 * Self-extractor main class
 * @author nguyenvannghia
 *
 */
public class SelfExtractorCmd {	 
	
	private static FileHandler fh = null;
	public static String OUTPUT_FOLDER = "";		
	public static Logger logger = Logger.getLogger(SelfExtractorCmd.class.getName());
	static Logger rootLogger = Logger.getLogger("");
	public static final String LOG_FILE_NAME = "WiperdogInstaller.log";  
	public static void main(String args[]) throws Exception{	
		try {
		 fh=new FileHandler(LOG_FILE_NAME, false);
		} catch (Exception e) {
		 e.printStackTrace();
		}
		
		//- Remove console handler
		Handler[] handlers = rootLogger.getHandlers();
		if (handlers[0] instanceof ConsoleHandler) {
		   rootLogger.removeHandler(handlers[0]);
		}
		fh.setFormatter(new SimpleFormatter());
		rootLogger.addHandler(fh);
		rootLogger.setLevel(Level.ALL);
		//Argurments : -d (wiperdog home) ,-j(jetty port),-m(mongodb host),-p(mongodb port),-n(database name),-u(user database),-pw(password database),-mp(mail policy),-s(install as OS service)
		//              -jd (job directory ) , -id (instances directory) , -cd (jobclass directory) 
		List<String> listParams = new ArrayList<String>();
		listParams.add("-d");
		listParams.add("-j");
		listParams.add("-m");
		listParams.add("-jd");
		listParams.add("-id");
		listParams.add("-td");
		listParams.add("-cd");
		listParams.add("-p");
		listParams.add("-n");
		listParams.add("-u");
		listParams.add("-pw");
		listParams.add("-mp");
		listParams.add("-s");
		List<String> listArgs = Arrays.asList(args);
		try {
			// check command syntax
			if (args.length == 0) {
				//Get current dir
				String currentDir = System.getProperty("user.dir");
				
				//Get jar file name, create install directory name
				String jarFileName = new java.io.File(SelfExtractorCmd.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getName();
				String wiperdogDirName = "";
				if (jarFileName.endsWith(".jar")) {
					wiperdogDirName = jarFileName.substring(0, jarFileName.length() - 4);
				}
				if (wiperdogDirName.endsWith("-unix")) {
					wiperdogDirName = wiperdogDirName.substring(0, wiperdogDirName.length() - 5);
				}
				if (wiperdogDirName.endsWith("-win")) {
					wiperdogDirName = wiperdogDirName.substring(0, wiperdogDirName.length() - 4);
				}
				if (wiperdogDirName == "") {
					wiperdogDirName = "wiperdogHome";
				}
				//wiperdog home path
				String wiperdogPath = currentDir + File.separator + wiperdogDirName;
				
				//Check install or not
				logger.info("You omitted to specify WIPERDOG HOME.");
				//System.out.println("You omitted to specify WIPERDOG HOME.");
				InputStreamReader converter = new InputStreamReader(System.in);
	            BufferedReader inp = new BufferedReader(converter, 512);
	            String confirmStr = "";
	            
	            while ((!confirmStr.toLowerCase().equalsIgnoreCase("y")) && (!confirmStr.toLowerCase().equalsIgnoreCase("n"))) {
	            	logger.info("Are you sure to install wiperdog at " + wiperdogPath + " ? [y/n] :");
	            	System.out.println("Are you sure to install wiperdog at " + wiperdogPath + " ? [y/n] :");
	            	confirmStr = inp.readLine().trim();
	            	if (confirmStr.toLowerCase().equalsIgnoreCase("y")) {
	            		OUTPUT_FOLDER = wiperdogPath;
	            	} else if (confirmStr.toLowerCase().equalsIgnoreCase("n")) {
	            		System.exit(0);
	            	}
	            }
			} else if ((args.length < 2) || (!args[0].trim().equals("-d")) ) {
				logger.warning("Wrong parameter. Usage:\n \t\t java -jar [Installer Jar] -d [INSTALL_PATH>] \n \t\t or \n \t\t java -jar [Installer Jar] -d [INSTALL_PATH] -j [jettyport] -m [mongodb host] -p [mongodb port] -n [mongodb database name] -u [mongodb user name] -pw [mongodb password] -mp [mail policy] -s [yes/no install as OS service]");
				System.out.println("Wrong parameter. Usage:\n \t\t java -jar [Installer Jar] -d [INSTALL_PATH>] \n \t\t or \n \t\t java -jar [Installer Jar] -d [INSTALL_PATH] -j [jettyport] -m [mongodb host] -p [mongodb port] -n [mongodb database name] -u [mongodb user name] -pw [mongodb password] -mp [mail policy] -s [yes/no install as OS service]");
				System.exit(0);
			} else {
				OUTPUT_FOLDER = (String) args[1];
			}
			//Get default params not in arguments
			List<String> defaultParams = new ArrayList<String>();
			for(int i = 1 ; i < listParams.size() ; i++){
				if(!listArgs.contains(listParams.get(i))){
					defaultParams.add(listParams.get(i));
				}
			}
			String strArgs = "";
			//Valid arguments
			if(args.length > 2) {
				for(int i = 1; i< args.length; i++ ){
				   //Get jetty port from argurment
					if(args[i].equals("-j")) {
						if( ( args.length > i+1) &&  (args[i+1].trim() != "") && (!listParams.contains(args[i+1].trim()))){
							if( isNumeric(args[i+1])){
								strArgs += "-j " + args[i+1] + " ";
								i++;
							} else {
								logger.warning( "Jetty port must be number: " + args[i]);
								System.out.println( "Jetty port must be number: " + args[i]);
								return;
							}
						} else {
							logger.warning("Incorrect value of params: " + args[i]);
							System.out.println("Incorrect value of params: " + args[i]);
							return;
						}
					}
					// Get job directory from argurment
					if (args[i].equals("-jd")) {
						if ((args.length > i + 1) && (args[i + 1].trim() != "") && (!listParams.contains(args[i + 1].trim()))) {
							strArgs += "-jd " + args[i + 1] + " ";
							i++;
						} else {
							logger.warning("Incorrect value of params: " + args[i]);
							System.out.println("Incorrect value of params: " + args[i]);
							return;
						}
					}

					// Get intances directory from argurment
					if (args[i].equals("-id")) {
						if ((args.length > i + 1) && (args[i + 1].trim() != "") && (!listParams.contains(args[i + 1].trim()))) {
							strArgs += "-id " + args[i + 1] + " ";
							i++;
						} else {
							logger.warning("Incorrect value of params: " + args[i]);
							System.out.println("Incorrect value of params: " + args[i]);
							return;
						}
					}
					
					// Get job class directory from argurment
					if (args[i].equals("-cd")) {
						if ((args.length > i + 1) && (args[i + 1].trim() != "") && (!listParams.contains(args[i + 1].trim()))) {
							strArgs += "-cd " + args[i + 1] + " ";
							i++;
						} else {
							logger.warning("Incorrect value of params: " + args[i]);
							System.out.println("Incorrect value of params: " + args[i]);
							return;
						}
					}
					
					// Get trigger directory from argurment
					if (args[i].equals("-td")) {
						if ((args.length > i + 1) && (args[i + 1].trim() != "") && (!listParams.contains(args[i + 1].trim()))) {
							strArgs += "-td " + args[i + 1] + " ";
							i++;
						} else {
							logger.warning("Incorrect value of params: " + args[i]);
							System.out.println("Incorrect value of params: " + args[i]);
							return;
						}
					}
					//Get Mongodb Host from argurment
					if(args[i].equals("-m")) {
						if(( args.length > i+1) && (args[i+1].trim() != "") && (!listParams.contains(args[i+1].trim()))){
							strArgs += "-m " + args[i+1] + " ";
							i++;
						} else {
							logger.warning("Incorrect value of params: " + args[i]);
							System.out.println( "Incorrect value of params: " + args[i]);
							return;
						}
					}
					//Get Mongodb Port from argurment
					if(args[i].equals("-p")) {
						if( ( args.length > i+1) && (args[i+1].trim() != "") && (!listParams.contains(args[i+1].trim()))){
							if(isNumeric(args[i+1])){
								strArgs += "-p " + args[i+1] + " ";
								i++;
							} else {
								logger.warning("Mongodb port must be number: " + args[i]);
								System.out.println("Mongodb port must be number: " + args[i]);
								return;
							}
						} else {
							logger.warning("Incorrect value of params: " + args[i]);
							System.out.println("Incorrect value of params: " + args[i]);
							return;
						}
					}
					//Get Mongodb database name from argurment
					if(args[i].equals("-n")) {
						if( ( args.length > i+1) && (args[i+1].trim() != "") && (!listParams.contains(args[i+1].trim()))){
							strArgs += "-n " + args[i+1] + " ";
							i++;
						} else {
							logger.warning("Incorrect value of params: " + args[i]);
							System.out.println("Incorrect value of params: " + args[i]);
							return;
						}
					}
					//Get user connect to database
					if(args[i].equals("-u")) {
						if( ( args.length > i+1) && (args[i+1].trim() != "") && (!listParams.contains(args[i+1].trim()))){
							strArgs += "-u " + args[i+1] + " ";
							i++;
						} else {
							logger.warning("Incorrect value of params: " + args[i]);
							System.out.println("Incorrect value of params: " + args[i]);
							return;
						}
					}
					//Get password connect to database
					String pattern = "[a-zA-Z0-9]+";
					if(args[i].equals("-pw")) {
						if( ( args.length > i+1) && (args[i+1].trim() != "") && (!listParams.contains(args[i+1].trim())) && args[i+1].matches(pattern)){
							strArgs += "-pw " + args[i+1] + " ";
							i++;
						} else {
							logger.warning("Incorrect value of params: " + args[i]);
							System.out.println("Incorrect value of params: " + args[i]);
							return;
						}
					}
					//Get mail send to policy
					if(args[i].equals("-mp")) {
						if( ( args.length > i+1) && (args[i+1].trim() != "") && (!listParams.contains(args[i+1].trim()))){
							strArgs += "-mp " + args[i+1] + " ";
							i++;
						} else {
							logger.warning("Incorrect value of params: " + args[i]);
							System.out.println("Incorrect value of params: " + args[i]);
							return;
						}
					}
					//Get install wiperdog as service
					if(args[i].equals("-s")) {
						if( ( args.length > i+1) && (args[i+1].trim() != "") && (!listParams.contains(args[i+1].trim()))){
							strArgs += "-s " + args[i+1] + " ";
							i++;
						} else {
							logger.warning("Incorrect value of params: " + args[i]);
							System.out.println("Incorrect value of params: " + args[i]);
							return;
						}
					}
				}
				// Set default params
				for(int i = 0 ; i < defaultParams.size() ; i++ ){
					if(defaultParams.get(i) == "-j"){
						strArgs += "-j 13110 ";
					}
					if(defaultParams.get(i) == "-m"){
						strArgs += "-m 127.0.0.1 ";
					}
					if (defaultParams.get(i) == "-jd") {
						strArgs += "-jd ${felix.home}/var/job ";
					}
					if (defaultParams.get(i) == "-id") {
						strArgs += "-id ${felix.home}/var/job ";
					}
					if (defaultParams.get(i) == "-td") {
						strArgs += "-td ${felix.home}/var/job ";
					}
					if (defaultParams.get(i) == "-cd") {
						strArgs += "-cd ${felix.home}/var/job ";
					}
					if(defaultParams.get(i) == "-p"){
						strArgs += "-p 27017 ";
					}
					if(defaultParams.get(i) == "-n"){
						strArgs += "-n wiperdog ";
					}
					if(defaultParams.get(i) == "-mp"){
						strArgs += "-mp testmail@gmail.com ";
					}
					if(defaultParams.get(i) == "-s"){
						strArgs += "-s no ";
					}
				}
			} 
			
			File outputDir = new File(OUTPUT_FOLDER);
			//check if wiperdog home params is not an absolute path
		    if(!outputDir.isAbsolute()) {
       			String userDir = System.getProperty("user.dir");
				OUTPUT_FOLDER = new File (userDir, OUTPUT_FOLDER).getAbsolutePath();
			}			
			System.out.println("Wiperdog will be install to directory: "
					+ OUTPUT_FOLDER);
			String jarPath = new URI(SelfExtractorCmd.class.getProtectionDomain()
					.getCodeSource().getLocation().getFile()).getPath();
			//-- Stopping service 						
			if(System.getProperty("os.name").toLowerCase().indexOf("win") != -1){
				System.out.println("");				
				logger.info("Stop wiperdog service: Start");
				System.out.println("Stop wiperdog service: Start");
				stopService();
				logger.info("Stop wiperdog service: End");
				System.out.println("Stop wiperdog service: End");
			}
			
			unZip(jarPath, OUTPUT_FOLDER);			
			String newJarPath = (System.getProperty("os.name").toLowerCase()
					.indexOf("win") != -1) ? jarPath.substring(1, jarPath
					.length()) : jarPath;			
			runGroovyInstaller(newJarPath,strArgs);
			System.exit(0);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	/**
	 * Run the installer script written in Groovy
	 * @param jarPath path to this Jar file, used in classpath for the java process
	 * @throws Exception any exception
	 */
	static void runGroovyInstaller(String jarPath,String strArgs)throws Exception{		
		//- risk when user choose the output directory in another volume, which is different from current volume
		String currentInstallerDir = System.getProperty("user.dir");
		try
        {
	        File file = new File(OUTPUT_FOLDER + "/extractor.xml");
	        BufferedReader reader = new BufferedReader(new FileReader(file));
	        String line = "", oldtext = "";
	        while((line = reader.readLine()) != null)
	            {
	            oldtext += line + "\n";
	        }
	        reader.close();	        
	        String newtext = oldtext.replaceAll("INSTALLER_LOG_PATH", currentInstallerDir + "/" + LOG_FILE_NAME);
	        //System.out.println("new text " + newtext);
	        FileWriter writer = new FileWriter(OUTPUT_FOLDER + "/extractor.xml");
	        writer.write(newtext);
	        writer.close();
	    }
	    catch (IOException ioe)
	        {
	        ioe.printStackTrace();
	    }
		
		File workDir = new File(OUTPUT_FOLDER);		
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(true);
		factory.setIgnoringElementContentWhitespace(true);
		DocumentBuilder docBuilder = factory.newDocumentBuilder();
		docBuilder.setErrorHandler(new XMLErrorHandler());
		Document doc = docBuilder.parse(InstallerUtil.class.getResourceAsStream("/extractor.xml"));
		InstallerUtil.parseXml(doc.getDocumentElement());
				
		
		if(InstallerXML.getInstance().getRunInstallerSyntax() == null || InstallerXML.getInstance().getRunInstallerSyntax().equals(""))
			throw new Exception("Cannot run configuration for newly installed Wiperdog");
		
		//Run java process, e.g: java -jar lib/java/bundle/groovy-all-2.2.1.jar installer/installer.groovy
		String runInstallerSyntax =  InstallerXML.getInstance().getRunInstallerSyntax();
		
		// runInstallerSyntax += " "+OUTPUT_FOLDER  + " " + strArgs;
			if (runInstallerSyntax != null && !runInstallerSyntax.equals("")) {
				String[] cmdArray = runInstallerSyntax.split(" ");
				List<String> listCmd = new LinkedList<String>();
				
				if (cmdArray.length > 0) {					
					if (cmdArray[0].equals("java")) {
						cmdArray[0] = System.getProperty("java.home") 
							+ File.separator + "bin" + File.separator + "java";
					}
					for(int i=0; i<cmdArray.length;i++){						
						if(i==2){
							String claspathSeparator =  (System.getProperty("os.name").toLowerCase().indexOf("win")!=-1)?";":":";
							String newCmd = (System.getProperty("os.name").toLowerCase().indexOf("win")==-1)?
									(OUTPUT_FOLDER +File.separator+ cmdArray[i] + claspathSeparator + jarPath)
									:(cmdArray[i] + claspathSeparator + jarPath);
							listCmd.add(newCmd);
						} else {
							listCmd.add(cmdArray[i]);
						}
					}
					listCmd.add(OUTPUT_FOLDER);
					if (strArgs != "") {
					    cmdArray = strArgs.split(" ");
					    for(int i = 0; i < cmdArray.length; i++){
							listCmd.add(cmdArray[i]);
						}
					}
					ProcessBuilder builder = new ProcessBuilder(listCmd);
                    builder.directory(workDir);
					builder.redirectErrorStream(true);					
					Process p = builder.start();
					InputStream procOut  = p.getInputStream();
		            OutputStream procIn = p.getOutputStream();

		            new Thread(new Redirector("Output", procOut, System.out)).start();		            
		            new Thread(new Redirector("Input",System.in, procIn)).start();
		            p.waitFor();					
				}
				
			}		
	}
	/**
	 * Extract the jar file
	 * 
	 * @param zipFile input zip file
	 * @param outputFolder zip file output folder
	 */
    public static void unZip(String zipFile, String outputFolder){    
     byte[] buffer = new byte[1024];
 
     try{
 
    	// create output directory is not exists
    	File folder = new File(outputFolder);
    	if(!folder.exists()){
    		folder.mkdir();
    	}   	
    	
    	//------------------------------------ 
    	ZipInputStream zis2 = new ZipInputStream(new FileInputStream(zipFile));
    	// get the zipped file list entry
    	ZipEntry ze2 = zis2.getNextEntry();
    	while(ze2!=null ){
    		String fileName = ze2.getName();
    		if(!ze2.isDirectory() 
    				&& ! fileName.endsWith(".java") 
    				&& !fileName.endsWith(".class") 
    				&& !fileName.toLowerCase().endsWith(".mf")
    				&& !fileName.toLowerCase().endsWith("pom.xml")
    				&& !fileName.toLowerCase().endsWith("pom.properties")
    				){
	    	   File newFile = new File(outputFolder + System.getProperty("file.separator") + fileName);	 
	    	   logger.info("Wiperdog installer, unzip to file : "+ newFile.getAbsolutePath());
	           System.out.println("Wiperdog installer, unzip to file : "+ newFile.getAbsolutePath());	 
	            // create all non exists folders
	            // else you will hit FileNotFoundException for compressed folder
	           String parentPath = newFile.getParent();
	           File parentFolder = new File(parentPath);           
	           if(! parentFolder.exists())
	        	   parentFolder.mkdirs();
	           FileOutputStream fos = new FileOutputStream(newFile);
	           int len;
	           while ((len = zis2.read(buffer)) > 0) {
	            	fos.write(buffer, 0, len);
	           }
	            fos.flush();
	            fos.close();
	            if (fileName.startsWith("bin")) {
	            	newFile.setExecutable(true);
	            }
    		}
            ze2 = zis2.getNextEntry();
    	}
 
        zis2.closeEntry();
    	zis2.close();
    	logger.info("Self-extracting done!");
    	System.out.println("Self-extracting done!");
    }catch(IOException ex){
       ex.printStackTrace(); 
    }
   }
   	public static boolean isNumeric(String string){
	  return string.matches("-?\\d+(\\.\\d+)?");  
	}

    public static void stopService() throws Exception{
    	
    	File workDir = new File(System.getProperty("user.dir"));
    	List<String> listCmd = new LinkedList<String>();
    	listCmd.add("net");
    	listCmd.add("stop");
    	listCmd.add("wiperdog");
    	ProcessBuilder builder = new ProcessBuilder(listCmd);
		builder.redirectErrorStream(true);
		builder.directory(workDir);
		Process p = builder.start();	
		InputStream procOut  = p.getInputStream();
        OutputStream procIn = p.getOutputStream();

        new Thread(new Redirector(procOut, System.out)).start();
        new Thread(new Redirector(System.in, procIn)).start();
		p.waitFor();
		
		//-- kill process
		listCmd = new LinkedList<String>();
		listCmd.add("taskkill");
    	listCmd.add("/F");
    	listCmd.add("/IM");
    	listCmd.add("wiperdog_service.exe");
    	
    	builder = new ProcessBuilder(listCmd);
		builder.redirectErrorStream(true);
		builder.directory(workDir);
		p = builder.start();	
		procOut  = p.getInputStream();
        procIn = p.getOutputStream();

        new Thread(new Redirector(procOut, System.out)).start();
        new Thread(new Redirector(System.in, procIn)).start();
		p.waitFor();

		//-- Wait
		listCmd = new LinkedList<String>();
		listCmd.add("cmd.exe");    	    	
		listCmd.add("/c");
		listCmd.add("sleep");
		listCmd.add("3");
		builder = new ProcessBuilder(listCmd);
		builder.redirectErrorStream(true);
		builder.directory(workDir);
		p = builder.start();	
		procOut  = p.getInputStream();
        procIn = p.getOutputStream();

        new Thread(new Redirector(procOut, System.out)).start();
        new Thread(new Redirector(System.in, procIn)).start();
		p.waitFor();
    }  
}

/**
 * Standard input/output redirector thread
 * @author nguyenvannghia
 *
 */
class Redirector implements Runnable {
    InputStream in;
    OutputStream out;
    String name = "";
    public Redirector(String name, InputStream in, OutputStream out) {
    	this.name = name;
        this.in = in;
        this.out = out;
    }
    public Redirector(InputStream in, OutputStream out) {
        this.in = in;
        this.out = out;
    }    
    public void run() {
    	synchronized(in){
		try {
			byte[] buf = new byte[1];
			while ( in.read(buf) >= 0) {			
				out.write(buf);				
				out.flush();				
			}
		} catch (IOException e) {
            	//e.printStackTrace();
		}     
	}//- end sync
    }
}

	 