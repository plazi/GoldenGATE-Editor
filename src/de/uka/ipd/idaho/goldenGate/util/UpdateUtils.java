/*
 * Copyright (c) 2006-, IPD Boehm, Universitaet Karlsruhe (TH) / KIT, by Guido Sautter
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Universit�t Karlsruhe (TH) nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY UNIVERSIT�T KARLSRUHE (TH) / KIT AND CONTRIBUTORS 
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package de.uka.ipd.idaho.goldenGate.util;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.swing.JButton;
import javax.swing.JPanel;

import de.uka.ipd.idaho.goldenGate.GoldenGateConstants;
import de.uka.ipd.idaho.goldenGate.GoldenGateConstants.StatusDialog.StatusDialogButton;

/**
 * Utility library for GoldenGATE core updates.
 * 
 * @author sautter
 */
public class UpdateUtils implements GoldenGateConstants {
	
	/**
	 * Read the update hosts from the respective file.
	 * @param ggRootPath the root folder of the GoldenGATE installation to
	 *            update
	 * @return an array holding the update hosts
	 */
	public static String[] readUpdateHosts(File ggRootPath) {
		
		File updateHostsFile = new File(ggRootPath, UPDATE_HOST_FILE_NAME);
		String[] updateHosts = {};
		if (updateHostsFile.exists()) {
			ArrayList updateHostList = new ArrayList();
			try {
				BufferedReader uhfReader = new BufferedReader(new FileReader(updateHostsFile));
				String updateHost;
				while ((updateHost = uhfReader.readLine()) != null) {
					updateHost = updateHost.trim();
					if (updateHost.length() != 0)
						updateHostList.add(updateHost);
				}
				updateHosts = ((String[]) updateHostList.toArray(new String[updateHostList.size()]));
				uhfReader.close();
			}
			catch (FileNotFoundException fnfe) {
				System.out.println("GoldenGateStarter: " + fnfe.getClass().getName() + " (" + fnfe.getMessage() + ") while reading updates.");
			}
			catch (IOException ioe) {
				System.out.println("GoldenGateStarter: " + ioe.getClass().getName() + " (" + ioe.getMessage() + ") while reading updates.");
			}
		}
		
		return updateHosts;
	}
	
	/**
	 * Produce the file name for an update generated at a given timestamp. This
	 * method produces file names that are recognized by the downloadUpdates()
	 * and installUpdates() methods.
	 * @param timestamp the timestamp for the update file
	 * @return the file name for an update generated at the specified timestamp
	 */
	public static final String getUpdateName(long timestamp) {
		return (updateName + timestamper.format(new Date(timestamp)) + ".zip");
	}
	
	private static final SimpleDateFormat timestamper = new SimpleDateFormat("yyyyMMdd-HHmm");
	
	private static final String updateNamePrefix = "GgUpdate";
//	private static final String updateVersionString = "V3";
//	private static final String updateName = (updateNamePrefix + "." + updateVersionString + ".");
//	private static final String updateNameRegEx = (updateNamePrefix + "\\." + updateVersionString + "\\.");
	private static final String updateName = (updateNamePrefix + "." + VERSION_STRING + ".");
	private static final String updateNameRegEx = (updateNamePrefix + "\\." + VERSION_STRING + "\\.");
	
	// matches href="/GgUpdate.-.zip"
//	private static final Pattern updateHrefPattern = Pattern.compile("(a href\\=\\\"((\\/[^\\/]++)*\\/GgUpdate\\.[0-9\\-]++\\.zip))\\\"", Pattern.CASE_INSENSITIVE);
	private static final Pattern updateHrefPattern = Pattern.compile("(a href\\=\\\"((\\/[^\\/]++)*\\/" + updateNameRegEx + "[0-9\\-]++\\.zip))\\\"", Pattern.CASE_INSENSITIVE);
	
	// matches GgUpdate.-.zip
//	private static final Pattern updateFileNamePattern = Pattern.compile("GgUpdate\\.[0-9\\-]++\\.zip", Pattern.CASE_INSENSITIVE);
	private static final Pattern updateFileNamePattern = Pattern.compile(updateNameRegEx + "[0-9\\-]++\\.zip", Pattern.CASE_INSENSITIVE);
	
	// matches GgUpdate.-.zip.done
//	private static final Pattern installedUpdateFileNamePattern = Pattern.compile("GgUpdate\\.[0-9\\-]++\\.zip\\.done", Pattern.CASE_INSENSITIVE);
	private static final Pattern installedUpdateFileNamePattern = Pattern.compile(updateNameRegEx + "[0-9\\-]++\\.zip\\.done", Pattern.CASE_INSENSITIVE);
	
	//	produces eg '20090511-1300'
	private static final DateFormat updateTimestampFormatter = new SimpleDateFormat("yyyyMMdd-HHmm");
	
	/**
	 * Download the updates deposited at a set of GoldenGATE update hosts.
	 * @param ggRootPath the root folder of the GoldenGATE installation
	 * @param ggTimestamp the timestamp of the local GoldenGATE installation
	 * @param updateHosts an array holding the URLs to download updates from
	 * @param sd the status dialog for displaying the installation status to
	 *            users (may be null)
	 */
	public static void downloadUpdates(File ggRootPath, long ggTimestamp, String[] updateHosts, UpdateStatusDialog sd) {
		String ggTimestampString = updateTimestampFormatter.format(new Date(ggTimestamp));
		
		File updateFolder = new File(ggRootPath, UPDATE_FOLDER_NAME);
		if (!updateFolder.exists())
			updateFolder.mkdir();
		
		//	create index of updates available locally or already installed
		File[] updateFiles = updateFolder.listFiles(new FileFilter() {
			public boolean accept(File f) {
				return (installedUpdateFileNamePattern.matcher(f.getName()).matches() || updateFileNamePattern.matcher(f.getName()).matches());
			}
		});
		
		Set updateFileNames = new HashSet();
		for (int u = 0; u < updateFiles.length; u++) {
			String updateFileName = updateFiles[u].getName().toLowerCase();
			if (updateFileName.endsWith(".done"))
				updateFileName = updateFileName.substring(0, (updateFileName.length() - ".done".length()));
			updateFileNames.add(updateFileName);
		}
		
		
		//	get available configurations from configuration hosts
		for (int h = 0; h < updateHosts.length; h++) {
			final String updateHost = updateHosts[h].trim();
			if ((updateHost.length() == 0) || updateHost.startsWith("//"))
				continue;
			
			//	show activity
			if (sd != null)
				sd.setStatusLabel("- from " + updateHosts[h]);
			
			//	create control objects
			final Object updateFetcherLock = new Object();
			final boolean[] addHostUpdates = {true};
			
			//	build UI elements only if dialog present
			if (sd != null) {
				
				//	build buttons
				JButton skipButton = new StatusDialogButton("Skip", 5, 2);
				skipButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						synchronized (updateFetcherLock) {
							addHostUpdates[0] = false;
							updateFetcherLock.notify();
						}
					}
				});
				
				//	line up button panel
				JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
				buttonPanel.setBackground(Color.WHITE);
				buttonPanel.add(skipButton);
				
				//	show buttons
				sd.setCustomComponent(buttonPanel);
			}
			
			//	load configs in extra thread
			final ArrayList updateUrls = new ArrayList();
			Thread updateFetcher = new Thread() {
				public void run() {
					try {
						
						//	download update index from update host
						URL updateHostUrl = new URL(updateHost + (updateHost.endsWith("/") ? "" : "/"));
						BufferedReader updateIndexReader = new BufferedReader(new InputStreamReader(updateHostUrl.openStream()));
						String updateIndexLine;
						while ((updateIndexLine = updateIndexReader.readLine()) != null) {
							if (!addHostUpdates[0])
								return;
							
							Matcher updateLinkMatcher = updateHrefPattern.matcher(updateIndexLine);
							if (updateLinkMatcher.find()) {
								String updateLink = updateLinkMatcher.group(1);
								System.out.println(" - got update link: '" + updateLink + "'");
								updateUrls.add(updateLink);
							}
						}
					}
					catch (IOException ioe) {
						System.out.println("UpdateUtils: " + ioe.getClass().getName() + " (" + ioe.getMessage() + ") while downloading update index from '" + updateHost + "'");
						ioe.printStackTrace(System.out);
					}
					
					//	wake up main thread
					synchronized (updateFetcherLock) {
						updateFetcherLock.notify();
					}
					
					//	log for debugging
					System.out.println("Update fetcher terminated for " + updateHost);
				}
			};
			
			//	start index download and block main thread
			synchronized (updateFetcherLock) {
				updateFetcher.start();
				System.out.println("Update fetcher started for " + updateHost);
				try {
					updateFetcherLock.wait();
				}
				catch (InterruptedException ie) {
					System.out.println("Interrupted waiting for update fetcher from " + updateHost);
					addHostUpdates[0] = false;
				}
			}
			
			//	skipped, ignore this
			if (!addHostUpdates[0])
				continue;
			
			//	download actual updates
			for (int u = 0; u < updateUrls.size(); u++) {
				String updateFileName = updateUrls.get(u).toString();
				updateFileName = updateFileName.substring(updateFileName.lastIndexOf("/") + 1);
				
				//	update already installed
				if (updateFileNames.contains(updateFileName.toLowerCase())) {
					System.out.println(" - update '" + updateFileName + "' is available locally.");
					if (sd != null) sd.setStatusLabel(" - update '" + updateFileName + "' is available locally.");
					continue;
				}
				
				//	check if update is recent, download if so
				String updateTimestamp = updateFileName.substring(updateName.length(), updateFileName.length() - 4);
				if (updateTimestamp.compareTo(ggTimestampString) > 0) {
					System.out.println(" - update '" + updateFileName + "' is out of date.");
					if (sd != null) sd.setStatusLabel(" - update '" + updateFileName + "' is out of date.");
					continue;
				}
				
				//	extract update timestamp & compare to timestamp of GoldenGATE.jar
				try {
					System.out.println(" - downloading update '" + updateFileName + "' ...");
					if (sd != null) sd.setStatusLabel(" - downloading update '" + updateFileName + "' ...");
					
					File destFile = new File(updateFolder, updateFileName);
					System.out.println(" - destination file is '" + destFile.getAbsolutePath() + "'");
					if (sd != null) sd.setStatusLabel(" - destination file is '" + destFile.getAbsolutePath() + "'");
					destFile.createNewFile();
					
					FileOutputStream dest = new FileOutputStream(destFile);
					System.out.println("   - got destination file writer");
					if (sd != null) sd.setStatusLabel("   - got destination file writer");
					
					BufferedInputStream bis = new BufferedInputStream(new URL(updateHosts[h] + (updateHosts[h].endsWith("/") ? "" : "/") + updateFileName).openStream());
					System.out.println("   - got reader");
					if (sd != null) sd.setStatusLabel("   - got reader");
					
					int count;
					byte data[] = new byte[1024];
					while ((count = bis.read(data, 0, 1024)) != -1)
						dest.write(data, 0, count);
					dest.flush();
					dest.close();
					bis.close();
					System.out.println("   - download completed");
					if (sd != null) sd.setStatusLabel("   - download completed");
				}
				catch (IOException ioe) {
					System.out.println("UpdateUtils: " + ioe.getClass().getName() + " (" + ioe.getMessage() + ") while downloading update '" + updateFileName + "' from '" + updateHosts[h] + "'");
					ioe.printStackTrace(System.out);
				}
			}
		}
//
//		for (int h = 0; h < updateHosts.length; h++) try {
//			if ((updateHosts[h].trim().length() == 0) || updateHosts[h].startsWith("//"))
//				continue;
//			
//			//	keep user posted
//			System.out.println("Start downloading update links from '" + updateHosts[h] + "' ...");
//			if (sd != null) sd.setHost(updateHosts[h]);
//			
//			//	download update index from update host
//			URL updateHostUrl = new URL(updateHosts[h] + (updateHosts[h].endsWith("/") ? "" : "/"));
//			ArrayList updateUrls = new ArrayList();
//			BufferedReader updateIndexReader = new BufferedReader(new InputStreamReader(updateHostUrl.openStream()));
//			String updateIndexLine;
//			while ((updateIndexLine = updateIndexReader.readLine()) != null) {
//				Matcher updateLinkMatcher = updateHrefPattern.matcher(updateIndexLine);
//				if (updateLinkMatcher.find()) {
//					String updateLink = updateLinkMatcher.group(1);
//					System.out.println(" - got update link: '" + updateLink + "'");
//					updateUrls.add(updateLink);
//				}
//			}
//			
//			//	download updates
//			for (int u = 0; u < updateUrls.size(); u++) {
//				String updateFileName = updateUrls.get(u).toString();
//				updateFileName = updateFileName.substring(updateFileName.lastIndexOf("/") + 1);
//				
//				//	update already installed
//				if (updateFileNames.contains(updateFileName.toLowerCase())) {
//					System.out.println(" - update '" + updateFileName + "' is available locally.");
//					continue;
//				}
//				
//				//	check if update is recent, download if so
//				String updateTimestamp = updateFileName.substring(updateName.length(), updateFileName.length() - 4);
//				if (updateTimestamp.compareTo(ggTimestampString) > 0) {
//					System.out.println(" - update '" + updateFileName + "' is out of date.");
//					continue;
//				}
//				
//				//	extract update timestamp & compare to timestamp of GoldenGATE.jar
//				try {
//					System.out.println(" - downloading update '" + updateFileName + "' ...");
//					if (sd != null) sd.setLabel("Downloading " + updateFileName);
//					
//					File destFile = new File(updateFolder, updateFileName);
//					System.out.println(" - destination file is '" + destFile.getAbsolutePath() + "'");
//					destFile.createNewFile();
//					
//					FileOutputStream dest = new FileOutputStream(destFile);
//					System.out.println("   - got destination file writer");
//					
//					BufferedInputStream bis = new BufferedInputStream(new URL(updateHosts[h] + (updateHosts[h].endsWith("/") ? "" : "/") + updateFileName).openStream());
//					System.out.println("   - got reader");
//					
//					int count;
//					byte data[] = new byte[1024];
//					while ((count = bis.read(data, 0, 1024)) != -1)
//						dest.write(data, 0, count);
//					dest.flush();
//					dest.close();
//					bis.close();
//					System.out.println("   - download completed");
//				}
//				catch (IOException ioe) {
//					System.out.println("GoldenGateStarter: " + ioe.getClass().getName() + " (" + ioe.getMessage() + ") while downloading update '" + updateFileName + "' from '" + updateHosts[h] + "'");
//					ioe.printStackTrace(System.out);
//				}
//			}
//		}
//		catch (IOException ioe) {
//			System.out.println("GoldenGateStarter: " + ioe.getClass().getName() + " (" + ioe.getMessage() + ") while downloading update index from '" + updateHosts[h] + "'");
//			ioe.printStackTrace(System.out);
//		}
	}
	
//	
//	//	get available configurations from configuration hosts
//	for (int h = 0; h < CONFIG_HOSTS.size(); h++) {
//		final int ch = h;
//		final String configHost = CONFIG_HOSTS.get(ch).trim();
//		if (configHost.startsWith("//"))
//			continue;
//		
//		//	show activity
//		sd.setStatusLabel("- from " + configHost);
//		
//		//	create control objects
//		final Object configFetcherLock = new Object();
//		final boolean[] addHostConfigs = {true};
//		
//		//	build buttons
//		JButton skipButton = new StatusDialogButton("Skip", 5, 2);
//		skipButton.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent ae) {
//				synchronized (configFetcherLock) {
//					addHostConfigs[0] = false;
//					configFetcherLock.notify();
//				}
//			}
//		});
//		
//		JButton alwaysSkipButton = new StatusDialogButton("Always Skip", 5, 2);
//		alwaysSkipButton.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent ae) {
//				synchronized (configFetcherLock) {
//					CONFIG_HOSTS.setElementAt(("//" + configHost), ch);
//					configHostsModified[0] = true;
//					addHostConfigs[0] = false;
//					configFetcherLock.notify();
//				}
//			}
//		});
//		
//		//	line up button panel
//		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
//		buttonPanel.setBackground(Color.WHITE);
//		buttonPanel.add(skipButton);
//		buttonPanel.add(alwaysSkipButton);
//		
//		//	show buttons
//		sd.setCustomComponent(buttonPanel);
//		
//		//	load configs in extra thread
//		Thread configFetcher = new Thread() {
//			public void run() {
//				ConfigurationDescriptor[] hostConfigs = ConfigurationUtils.getRemoteConfigurations(configHost);
//				synchronized (configFetcherLock) {
//					if (!addHostConfigs[0])
//						return;
//					for (int c = 0; c < hostConfigs.length; c++)
//						configList.add(hostConfigs[c]);
//					configFetcherLock.notify();
//				}
//			}
//		};
//		
//		//	start download and block main thread
//		synchronized (configFetcherLock) {
//			configFetcher.start();
//			System.out.println("Config fetcher started for " + configHost);
//			try {
//				configFetcherLock.wait();
//			}
//			catch (InterruptedException ie) {
//				System.out.println("Interrupted waiting for config fetcher from " + configHost);
//				addHostConfigs[0] = false;
//			}
//		}
//	}
	/**
	 * Install the updates deposited in the update folder of a GoldenGATE
	 * installation.
	 * @param ggRootPath the root folder of the GoldenGATE installation to
	 *            update
	 * @param sd the status dialog for displaying the installation status to
	 *            users (may be null)
	 */
	public static void installUpdates(File ggRootPath, UpdateStatusDialog sd) {
		File updatePath = new File(ggRootPath, UPDATE_FOLDER_NAME);
		if (!updatePath.exists()) updatePath.mkdir();
		File[] updates = updatePath.listFiles();
		for (int u = 0; u < updates.length; u++) {
			if (!updates[u].isFile() || !updates[u].getName().endsWith(".zip"))
				continue;
			
			try {
				System.out.println("Installing update from '" + updates[u] + "' ...");
				if (sd != null) sd.setStepLabel(updates[u].getName());
				
				ZipFile zip = new ZipFile(updates[u]);
				Enumeration entries = zip.entries();
				while (entries.hasMoreElements()) {
					ZipEntry ze = ((ZipEntry) entries.nextElement());
					String name = ze.getName();
					System.out.println(" - unzipping '" + name + "' ...");
					if (sd != null) sd.setStatusLabel(" - unzipping '" + name + "' ...");
					
					//	unzip folder
					if (ze.isDirectory()) {
						System.out.println("   - it's a folder");
						File destFile = new File(ggRootPath, name);
						System.out.println("   - destination folder is '" + destFile.getAbsolutePath() + "'");
						System.out.println("   - checking destination folder:");
						if (!destFile.exists()) {
							System.out.println("   --> destination folder doesn't exist ...");
							destFile.mkdirs();
							System.out.println("     - destination folder created");
						} else System.out.println("   --> destination folder exists");
					}
					
					//	unzip file
					else {
						System.out.println("   - it's a file");
						File destFile = new File(ggRootPath, name);
						System.out.println("   - destination file is '" + destFile.getAbsolutePath() + "'");
						System.out.println("   - checking destination file:");
						
						if (!destFile.exists()) {
							System.out.println("     --> destination file doesn't exist ...");
							File destFolder = destFile.getParentFile();
							if (!destFolder.exists()) {
								destFolder.mkdirs();
								System.out.println("     - destination folder created");
							}
							destFile.createNewFile();
							System.out.println("       - destination file created");
						}
						else if ((ze.getTime() != -1) && (destFile.lastModified() < ze.getTime())) {
							System.out.println("     --> destination file exists");
							String destFileName = destFile.toString();
							File oldDestFile = new File(destFileName + ".old");
							if (oldDestFile.exists() && oldDestFile.delete())
								oldDestFile = new File(destFileName + ".old");
							destFile.renameTo(oldDestFile);
							System.out.println("       - old destination file renamed");
							
							destFile = new File(destFileName);
							destFile.createNewFile();
							System.out.println("       - destination file created");
						}
						else {
							System.out.println("     --> destination file exists");
							System.out.println("       - destination file (" + destFile.lastModified() + ") is newer than update file (" + ze.getTime() + "), skipping update file");
							destFile = null;
						}
						
						if (destFile != null) {
							if (sd != null) sd.setStatusLabel(" - updating " + name);
							
							FileOutputStream dest = new FileOutputStream(destFile);
							System.out.println("   - got destination file writer");
							if (sd != null) sd.setStatusLabel("   - got destination file writer");
							
							BufferedInputStream bis = new BufferedInputStream(zip.getInputStream(ze));
							System.out.println("   - got reader");
							if (sd != null) sd.setStatusLabel("   - got reader");
							
							int count;
							byte data[] = new byte[1024];
							while ((count = bis.read(data, 0, 1024)) != -1)
								dest.write(data, 0, count);
							dest.flush();
							dest.close();
							
							if (ze.getTime() != -1) destFile.setLastModified(ze.getTime());
						}
					}
					
					System.out.println("   - '" + name + "' unzipped");
					if (sd != null) sd.setStatusLabel("   - '" + name + "' unzipped");
				}
				
				zip.close();
				
				File oldUpdateFile = new File(updates[u].getAbsoluteFile() + ".done");
				System.out.println(" - renaming update file to '" + oldUpdateFile + "'");
				updates[u].renameTo(oldUpdateFile);
				
				System.out.println(" - update from '" + updates[u] + "' installed");
			}
			catch (Exception e) {
				System.out.println("UpdateUtils: " + e.getClass().getName() + " (" + e.getMessage() + ") while installing updates from '" + updates[u] + "'");
				e.printStackTrace(System.out);
			}
		}
	}
	
	/**
	 * A status dialog for monitoring update downloads
	 * 
	 * @author sautter
	 */
	public static class UpdateStatusDialog extends StatusDialog {
		
		/**
		 * Constructor
		 * @param icon the icon to show
		 */
		public UpdateStatusDialog(Image icon) {
			this(icon, null);
		}
		
		/**
		 * Constructor
		 * @param icon the icon to show
		 * @param title the status dialog title
		 */
		public UpdateStatusDialog(Image icon, String title) {
			super(icon, ((title == null) ? "Updating GoldenGATE ..." : title));
		}
	}
//	/**
//	 * A status dialog for monitoring update downloads
//	 * 
//	 * @author sautter
//	 */
//	public static class UpdateStatusDialog extends JFrame implements Runnable {
//		JLabel hostLabel = new JLabel("", JLabel.LEFT);
//		
//		JLabel label = new JLabel("", JLabel.CENTER);
//		ArrayList labelLines = new ArrayList();
//		
//		Thread thread = null;
//		TitledBorder tb;
//		
//		/**
//		 * Constructor
//		 * @param icon the icon to show
//		 */
//		public UpdateStatusDialog(Image icon) {
//			this(icon, null);
//		}
//		
//		/**
//		 * Constructor
//		 * @param icon the icon to show
//		 * @param title the status dialog title
//		 */
//		public UpdateStatusDialog(Image icon, String title) {
//			super((title == null) ? "Updating GoldenGATE ..." : title);
//			this.setIconImage(icon);
//			this.setUndecorated(true);
//			
//			JPanel contentPanel = new JPanel(new BorderLayout());
//			contentPanel.add(this.hostLabel, BorderLayout.NORTH);
//			contentPanel.add(this.label, BorderLayout.CENTER);
//			
//			Font labelFont = UIManager.getFont("Label.font");
//			Border outer = BorderFactory.createMatteBorder(2, 3, 2, 3, Color.LIGHT_GRAY);
//			Border inner = BorderFactory.createEtchedBorder(Color.RED, Color.DARK_GRAY);
//			Border compound = BorderFactory.createCompoundBorder(outer, inner);
//			this.tb = BorderFactory.createTitledBorder(compound, this.getTitle(), TitledBorder.LEFT, TitledBorder.TOP, labelFont.deriveFont(Font.BOLD));
//			contentPanel.setBorder(this.tb);
//			contentPanel.setBackground(Color.WHITE);
//			
//			this.getContentPane().setLayout(new BorderLayout());
//			this.getContentPane().add(contentPanel, BorderLayout.CENTER);
//			
//			this.setSize(400, 120);
//			this.setLocationRelativeTo(null);
//			this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
//		}
//		
//		/* (non-Javadoc)
//		 * @see java.awt.Frame#setTitle(java.lang.String)
//		 */
//		public void setTitle(String title) {
//			super.setTitle(title);
//			this.tb.setTitle(title);
//		}
//
//		void setHost(String host) {
//			this.hostLabel.setText(host);
//			this.hostLabel.validate();
//		}
//		
//		void setLabel(String text) {
//			this.labelLines.add(text);
//			while (this.labelLines.size() > 3)
//				this.labelLines.remove(0);
//			
//			StringBuffer labelText = new StringBuffer("<HTML>" + this.labelLines.get(0));
//			for (int l = 1; l < this.labelLines.size(); l++)
//				labelText.append("<BR>" + this.labelLines.get(l));
//			labelText.append("</HTML>");
//			
//			this.label.setText(labelText.toString());
//			this.label.validate();
//		}
//		
//		/**
//		 * Open the status dialog.
//		 */
//		public void popUp() {
//			if (this.thread == null) {
//				this.thread = new Thread(this);
//				this.thread.start();
//				while (!this.isVisible()) try {
//					Thread.sleep(50);
//				} catch (InterruptedException ie) {}
//			}
//		}
//		
//		/* (non-Javadoc)
//		 * @see java.lang.Runnable#run()
//		 */
//		public void run() {
//			this.setVisible(true);
//			this.thread = null;
//		}
//	}
}
