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
 *     * Neither the name of the Universität Karlsruhe (TH) nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY UNIVERSITÄT KARLSRUHE (TH) / KIT AND CONTRIBUTORS 
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
package de.uka.ipd.idaho.goldenGate.plugin.documentIO;


import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;
import javax.swing.plaf.basic.BasicFileChooserUI;

import de.uka.ipd.idaho.easyIO.settings.Settings;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.goldenGate.DocumentEditor;
import de.uka.ipd.idaho.goldenGate.plugins.AbstractDocumentIO;
import de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat;
import de.uka.ipd.idaho.goldenGate.plugins.DocumentFormatProvider;
import de.uka.ipd.idaho.goldenGate.plugins.DocumentSaveOperation;
import de.uka.ipd.idaho.goldenGate.util.DialogPanel;
import de.uka.ipd.idaho.stringUtils.accessories.StringSelector;

/**
 * Document IO implementation enabling the GoldenGATE Editor to read and write
 * documents from and to files.
 * 
 * @author sautter
 */
public class FileDocumentIO extends AbstractDocumentIO {
	
	private JFileChooser fileChooser;
	private FileNameGuard fileKeeper = new FileNameGuard();
	private File lastLoadFile = null;
	private File lastSaveFile = null;
	
	public FileDocumentIO() {
		this.fileChooser = new JFileChooser();
		this.fileChooser.setAcceptAllFileFilterUsed(true);
		this.fileChooser.addPropertyChangeListener(this.fileKeeper);
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#init()
	 */
	public void init() {
		super.init();
		try {
			Reader setIn = new BufferedReader(new InputStreamReader(this.dataProvider.getInputStream("FileDocIO.cnfg")));
			Settings set = Settings.loadSettings(setIn);
			String lastLoadFile = set.getSetting("lastLoadedFrom");
			if (lastLoadFile != null)
				this.lastLoadFile = new File(lastLoadFile);
			String lastSaveFile = set.getSetting("lastSavedTo");
			if (lastSaveFile != null)
				this.lastSaveFile = new File(lastSaveFile);
			setIn.close();
		}
		catch (IOException ioe) {
			System.out.println("FileDocIO: exception loading settings - " + ioe.getMessage());
			ioe.printStackTrace(System.out);
		}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#exit()
	 */
	public void exit() {
		super.exit();
		if (this.dataProvider.isDataEditable("FileDocIO.cnfg")) try {
			Writer setOut = new BufferedWriter(new OutputStreamWriter(this.dataProvider.getOutputStream("FileDocIO.cnfg")));
			Settings set = new Settings();
			if (this.lastLoadFile != null)
				set.setSetting("lastLoadedFrom", this.lastLoadFile.getAbsolutePath());
			if (this.lastSaveFile != null)
				set.setSetting("lastSavedTo", this.lastSaveFile.getAbsolutePath());
			set.storeAsText(setOut);
			setOut.flush();
			setOut.close();
		}
		catch (IOException ioe) {
			System.out.println("FileDocIO: exception saving settings - " + ioe.getMessage());
			ioe.printStackTrace(System.out);
		}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#isOperational()
	 */
	public boolean isOperational() {
		return (super.isOperational() && (this.fileChooser != null));
	}
	
	/* 
	 * @see de.uka.ipd.idaho.goldenGate.plugins.GoldenGatePlugin#getPluginName()
	 */
	public String getPluginName() {
		return "File Document IO";
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentSaver#getSaveDocumentMenuItem()
	 */
	public JMenuItem getSaveDocumentMenuItem() {
		return new JMenuItem("Save Document to File");
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentSaver#getSaveDocumentPartsMenuItem()
	 */
	public JMenuItem getSaveDocumentPartsMenuItem() {
		return new JMenuItem("Save Document Parts to File");
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentLoader#getLoadDocumentMenuItem()
	 */
	public JMenuItem getLoadDocumentMenuItem() {
		return new JMenuItem("Load Document from File");
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentLoader#loadDocument()
	 */
	public DocumentData loadDocument() throws Exception {
		if (this.lastLoadFile != null)
			this.fileChooser.setSelectedFile(this.lastLoadFile);
		
		FileFilter chosenFileFilter = this.fileChooser.getFileFilter();
		FileFilter[] fileFilters = this.fileChooser.getChoosableFileFilters();
		for (int f = 0; f < fileFilters.length; f++)
			this.fileChooser.removeChoosableFileFilter(fileFilters[f]);
		
		DocumentFormatProvider[] formatters = this.parent.getDocumentFormatProviders();
		String defaultLoadFormatName = DocumentFormat.getDefaultLoadFormatName();
		DocumentFormat chosenFormat = null;
		for (int d = 0; d < formatters.length; d++) {
			DocumentFormat[] formats = formatters[d].getLoadFileFilters();
			for (int f = 0; f < formats.length; f++) {
				this.fileChooser.addChoosableFileFilter(formats[f]);
				if (formats[f].equals(chosenFileFilter))
					chosenFormat = formats[f];
				else if (defaultLoadFormatName != null) {
					if (defaultLoadFormatName.equals(formats[f].getDescription()))
						chosenFormat = formats[f];
				}
				else if ((chosenFormat == null) && formats[f].accept(this.lastLoadFile)) 
					chosenFormat = formats[f];
			}
		}
		
		if (chosenFormat != null)
			this.fileChooser.setFileFilter(chosenFormat);
		else this.fileChooser.setFileFilter(this.fileChooser.getAcceptAllFileFilter());
		
		this.fileKeeper.setFile(this.lastLoadFile);
		this.fileKeeper.setLoading(true);
		if (this.fileChooser.showOpenDialog(DialogPanel.getTopWindow()) == JFileChooser.APPROVE_OPTION) {
			File file = this.fileChooser.getSelectedFile();
			
			FileFilter selectedFilter = this.fileChooser.getFileFilter();
			if ((file != null) && !(selectedFilter instanceof DocumentFormat)) {
				ArrayList formatList = new ArrayList();
				for (int d = 0; d < formatters.length; d++) {
					DocumentFormat[] formats = formatters[d].getLoadFileFilters();
					for (int f = 0; f < formats.length; f++)
						formatList.add(formats[f]);
				}
				SelectFormatDialog sfd = new SelectFormatDialog(true, true, "Load", ("'" + this.fileChooser.getAcceptAllFileFilter().getDescription() + "' is not a valid file format, please select a format."), ((DocumentFormat[]) formatList.toArray(new DocumentFormat[formatList.size()])));
				sfd.setVisible(true);
				if (sfd.isCommitted)
					selectedFilter = sfd.getFormat();
				else file = null;
			}
			if ((file != null) && file.isFile()) {
				this.lastLoadFile = file;
				FileInputStream fis = null;
				try {
					System.out.println("FileDocumentIO: opening file as '" + ((DocumentFormat) selectedFilter).getDefaultSaveFileExtension() + "' (" + selectedFilter.getDescription() + ") via " + selectedFilter.getClass().getName());
					fis = new FileInputStream(file);
					return new DocumentData(((DocumentFormat) selectedFilter).loadDocument(fis), file.getName(), ((DocumentFormat) selectedFilter), new FileSaveOperation(file, ((DocumentFormat) selectedFilter), this));
				}
				catch (IOException ioe) {
					throw new Exception((ioe.getClass().getName() + " (" + ioe.getMessage() + ")\nwhile trying to load " + file.toString()), ioe);
				}
				finally {
					if (fis != null)
						fis.close();
				}
			}
		}
		return null;
	}
	
	private class FileNameGuard implements PropertyChangeListener {
		private String fileName;
		private boolean isLoading = false;
		private void setFile(File file) {
			if (file == null) this.fileName = null;
			else this.fileName = file.getName();
		}
		private void setLoading(boolean loading) {
			this.isLoading = loading;
		}
		public void propertyChange(PropertyChangeEvent pe) {
			if (JFileChooser.FILE_FILTER_CHANGED_PROPERTY.equals(pe.getPropertyName())) try {
				final BasicFileChooserUI ui = (BasicFileChooserUI) fileChooser.getUI();
				if (this.isLoading) {
					final String fileName = ui.getFileName().trim();
					if ((fileName != null) && (fileName.length() > 0)) {
						Object newFilter = pe.getNewValue();
						if ((newFilter instanceof FileFilter) && ((FileFilter) newFilter).accept(new File(fileName)))
							SwingUtilities.invokeLater(new Runnable() {
								public void run() {
									String currentName = ui.getFileName();
									if ((currentName == null) || (currentName.length() == 0)) ui.setFileName(fileName);
								}
							});
					}
				}
				else {
					String fileName = ui.getFileName().trim();
					if ((fileName != null) && (fileName.length() > 0)) {
						Object oldFilter = pe.getOldValue();
						if (oldFilter instanceof DocumentFormat) {
							String extension = ((DocumentFormat) oldFilter).getDefaultSaveFileExtension();
							if ((extension != null) && fileName.endsWith("." + extension))
								fileName = fileName.substring(0, (fileName.length() - 1 - extension.length()));
						}
					}
					if ((fileName == null) || (fileName.length() == 0)) fileName = this.fileName;
					if ((fileName == null) || (fileName.length() == 0)) return;
					
					Object newFilter = pe.getNewValue();
					if (newFilter instanceof DocumentFormat) {
						String extension = ((DocumentFormat) newFilter).getDefaultSaveFileExtension();
						if ((extension != null) && !fileName.endsWith(extension))
							fileName = (fileName + "." + extension);
					}
					
					final String name = fileName;
					if ((name == null) || (name.length() == 0)) return;
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							String currentName = ui.getFileName();
							if ((currentName == null) || (currentName.length() == 0)) ui.setFileName(name);
						}
					});
				}
			}
			catch (ClassCastException cce) {
				System.out.println(cce.getMessage() + " while reacting to file filter change.");
				cce.printStackTrace(System.out);
			}
			catch (RuntimeException re) {
				System.out.println(re.getMessage() + " while reacting to file filter change.");
				re.printStackTrace(System.out);
			}
		}
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentSaver#getSaveOperation(de.uka.ipd.idaho.goldenGate.plugins.DocumentSaveOperation)
	 */
	public DocumentSaveOperation getSaveOperation(DocumentSaveOperation model) {
		
		//	clear document formats
		FileFilter chosenFileFilter = this.fileChooser.getFileFilter();
		FileFilter[] fileFilters = this.fileChooser.getChoosableFileFilters();
		for (int f = 0; f < fileFilters.length; f++)
			this.fileChooser.removeChoosableFileFilter(fileFilters[f]);
		
		//	set document formats
		DocumentFormatProvider[] formatters = this.parent.getDocumentFormatProviders();
		String defaultFormatName = DocumentFormat.getDefaultSaveFormatName();
		boolean enforceDefaultFormat = DocumentFormat.enforceDefaultSaveFormat();
		DocumentFormat defaultFormat = null;
		DocumentFormat modelFormat = ((model == null) ? null : model.getDocumentFormat());
		DocumentFormat chosenFormat = null;
		DocumentFormat selectedFormat = null;
		for (int d = 0; d < formatters.length; d++) {
			DocumentFormat[] formats = formatters[d].getSaveFileFilters();
			for (int f = 0; f < formats.length; f++) {
				this.fileChooser.addChoosableFileFilter(formats[f]);
				if ((defaultFormatName != null) && defaultFormatName.equals(formats[f].getDescription()))
					defaultFormat = formats[f]; 
				if ((modelFormat != null) && (modelFormat.equals(formats[f])))
					selectedFormat = formats[f];
				if (formats[f].equals(chosenFileFilter))
					chosenFormat = formats[f];
				if ((selectedFormat == null) && formats[f].accept(this.lastSaveFile))
					selectedFormat = formats[f];
			}
		}
		
		//	pre-select document format
		if ((defaultFormat != null) && ((selectedFormat == null) || enforceDefaultFormat)) {
			this.fileChooser.setFileFilter(defaultFormat);
			selectedFormat = defaultFormat;
		}
		else if (selectedFormat != null)
			this.fileChooser.setFileFilter(selectedFormat);
		else if (chosenFormat != null) {
			this.fileChooser.setFileFilter(chosenFormat);
			selectedFormat = chosenFormat;
		}
		else this.fileChooser.setFileFilter(this.fileChooser.getAcceptAllFileFilter());
		
		//	determine target file
		File target;
		if (model == null)
			target = new File(((this.lastSaveFile == null) ? new File("") : this.lastSaveFile.getParentFile()), "document.xml"); 
		else if (model instanceof FileSaveOperation)
			target = new File(((FileSaveOperation) model).file.getParentFile(), model.getDocumentName()); 
		else target = new File(((this.lastSaveFile == null) ? new File("") : this.lastSaveFile.getParentFile()), model.getDocumentName()); 
		
		//	make sure target file has appropriate extension for pre-selected format
		if (selectedFormat != null) {
			String extension = selectedFormat.getDefaultSaveFileExtension();
			if ((extension != null) && !target.getName().endsWith(extension))
				target = new File(target.getParentFile(), (target.getName() + "." + extension));
		}
		
		//	prepare dialog
		this.fileChooser.setSelectedFile(target);
		this.fileKeeper.setFile(target);
		this.fileKeeper.setLoading(false);
		
		//	show dialog
		if (this.fileChooser.showSaveDialog(DialogPanel.getTopWindow()) == JFileChooser.APPROVE_OPTION) {
			target = this.fileChooser.getSelectedFile();
			
			FileFilter selectedFilter = this.fileChooser.getFileFilter();
			if ((target != null) && !(selectedFilter instanceof DocumentFormat)) {
				ArrayList formatList = new ArrayList();
				for (int d = 0; d < formatters.length; d++) {
					DocumentFormat[] formats = formatters[d].getSaveFileFilters();
					for (int f = 0; f < formats.length; f++) formatList.add(formats[f]);
				}
				SelectFormatDialog sfd = new SelectFormatDialog(false, true, "Save", ("'" + this.fileChooser.getAcceptAllFileFilter().getDescription() + "' is not a valid storage format, please select a format."), ((DocumentFormat[]) formatList.toArray(new DocumentFormat[formatList.size()])));
				sfd.setVisible(true);
				if (sfd.isCommitted) {
					selectedFilter = sfd.getFormat();
					String extension = ((DocumentFormat) selectedFilter).getDefaultSaveFileExtension();
					if ((extension != null) && !target.getName().endsWith(extension))
						target = new File(target.getParentFile(), (target.getName() + "." + extension));
				}
				else target = null;
			}
			
			if (target != null)
				return new FileSaveOperation(target, ((DocumentFormat) selectedFilter), this);
			
			else return null;
		}
		else return null;
	}
	
	/** @see de.uka.ipd.idaho.goldenGate.plugins.DocumentSaver#getSaveOperation(java.lang.String, de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat)
	 */
	public DocumentSaveOperation getSaveOperation(String documentName, DocumentFormat format) {
		
		//	clear document formats
		FileFilter chosenFileFilter = this.fileChooser.getFileFilter();
		FileFilter[] fileFilters = this.fileChooser.getChoosableFileFilters();
		for (int f = 0; f < fileFilters.length; f++)
			this.fileChooser.removeChoosableFileFilter(fileFilters[f]);
		
		//	set document formats
		DocumentFormatProvider[] formatters = this.parent.getDocumentFormatProviders();
		String defaultFormatName = DocumentFormat.getDefaultSaveFormatName();
		boolean enforceDefaultFormat = DocumentFormat.enforceDefaultSaveFormat();
		DocumentFormat defaultFormat = null;
		DocumentFormat chosenFormat = null;
		DocumentFormat selectedFormat = null;
		for (int d = 0; d < formatters.length; d++) {
			DocumentFormat[] formats = formatters[d].getSaveFileFilters();
			for (int f = 0; f < formats.length; f++) {
				this.fileChooser.addChoosableFileFilter(formats[f]);
				if ((defaultFormatName != null) && defaultFormatName.equals(formats[f].getDescription()))
					defaultFormat = formats[f]; 
				if ((format != null) && (format.equals(formats[f])))
					selectedFormat = formats[f];
				if (formats[f].equals(chosenFileFilter))
					chosenFormat = formats[f];
				if ((selectedFormat == null) && formats[f].accept(this.lastSaveFile))
					selectedFormat = formats[f];
			}
		}
		
		//	pre-select document format
		if ((defaultFormat != null) && ((selectedFormat == null) || enforceDefaultFormat)) {
			this.fileChooser.setFileFilter(defaultFormat);
			selectedFormat = defaultFormat;
		}
		else if (selectedFormat != null)
			this.fileChooser.setFileFilter(selectedFormat);
		else if (chosenFormat != null) {
			this.fileChooser.setFileFilter(chosenFormat);
			selectedFormat = chosenFormat;
		}
		else this.fileChooser.setFileFilter(this.fileChooser.getAcceptAllFileFilter());
		
		//	determine target file
		File target = new File(((this.lastSaveFile == null) ? new File("") : this.lastSaveFile.getParentFile()), ((documentName == null) ? "document.xml" : documentName));
		
		//	make sure target file has appropriate extension for pre-sellected format
		if (selectedFormat != null) {
			String extension = selectedFormat.getDefaultSaveFileExtension();
			if ((extension != null) && !target.getName().endsWith(extension))
				target = new File(target.getParentFile(), (target.getName() + "." + extension));
		}
		
		//	prepare dialog
		this.fileChooser.setSelectedFile(target);
		this.fileKeeper.setFile(target);
		this.fileKeeper.setLoading(false);
		
		//	show dialog
		if (this.fileChooser.showSaveDialog(DialogPanel.getTopWindow()) == JFileChooser.APPROVE_OPTION) {
			target = this.fileChooser.getSelectedFile();
			this.lastSaveFile = target;
			
			FileFilter selectedFilter = this.fileChooser.getFileFilter();
			if ((target != null) && !(selectedFilter instanceof DocumentFormat)) {
				ArrayList formatList = new ArrayList();
				for (int d = 0; d < formatters.length; d++) {
					DocumentFormat[] formats = formatters[d].getSaveFileFilters();
					for (int f = 0; f < formats.length; f++) formatList.add(formats[f]);
				}
				SelectFormatDialog sfd = new SelectFormatDialog(false, true, "Save", ("'" + this.fileChooser.getAcceptAllFileFilter().getDescription() + "' is not a valid storage format, please select a format."), ((DocumentFormat[]) formatList.toArray(new DocumentFormat[formatList.size()])));
				sfd.setVisible(true);
				if (sfd.isCommitted) {
					selectedFilter = sfd.getFormat();
					String extension = ((DocumentFormat) selectedFilter).getDefaultSaveFileExtension();
					if ((extension != null) && !target.getName().endsWith(extension))
						target = new File(target.getParentFile(), (target.getName() + "." + extension));
				} else target = null;
			}
			
			if (target != null)
				return new FileSaveOperation(target, ((DocumentFormat) selectedFilter), this);
			
			else return null;
		}
		else return null;
	}
	
	/** @see de.uka.ipd.idaho.goldenGate.plugins.DocumentSaver#saveDocumentParts(de.uka.ipd.idaho.goldenGate.DocumentEditor, de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat, java.lang.String)
	 */
	public String saveDocumentParts(DocumentEditor data, DocumentFormat modelFormat, String modelType) {
		QueriableAnnotation doc = data.getContent();
		
		StringSelector typeSelector = new StringSelector("Parts to Save", doc.getAnnotationTypes(), false);
		if (modelType != null) typeSelector.setSelectedString(modelType);
		
		File target = new File(((this.lastSaveFile == null) ? new File("") : this.lastSaveFile.getParentFile()), (data.getContentName() + "." + ((modelType == null) ? "part" : modelType) + "[XYZ]")); 
		this.fileChooser.setSelectedFile(target);
		
		FileFilter chosenFileFilter = this.fileChooser.getFileFilter();
		FileFilter[] fileFilters = this.fileChooser.getChoosableFileFilters();
		for (int f = 0; f < fileFilters.length; f++)
			this.fileChooser.removeChoosableFileFilter(fileFilters[f]);
		
		DocumentFormatProvider[] formatters = this.parent.getDocumentFormatProviders();
		String defaultFormatName = DocumentFormat.getDefaultSaveFormatName();
		boolean enforceDefaultFormat = DocumentFormat.enforceDefaultSaveFormat();
		DocumentFormat defaultFormat = null;
		DocumentFormat chosenFormat = null;
		DocumentFormat selectedFormat = null;
		for (int d = 0; d < formatters.length; d++) {
			DocumentFormat[] formats = formatters[d].getSaveFileFilters();
			for (int f = 0; f < formats.length; f++) {
				this.fileChooser.addChoosableFileFilter(formats[f]);
				if ((defaultFormatName != null) && defaultFormatName.equals(formats[f].getDescription()))
					defaultFormat = formats[f]; 
				if ((modelFormat != null) && (modelFormat.equals(formats[f])))
					selectedFormat = formats[f];
				if (formats[f].equals(chosenFileFilter))
					chosenFormat = formats[f];
				if ((selectedFormat == null) && formats[f].accept(this.lastSaveFile))
					selectedFormat = formats[f];
			}
		}
		
		if ((defaultFormat != null) && ((selectedFormat == null) || enforceDefaultFormat))
			this.fileChooser.setFileFilter(defaultFormat);
		else if (selectedFormat != null)
			this.fileChooser.setFileFilter(selectedFormat);
		else if (chosenFormat != null)
			this.fileChooser.setFileFilter(chosenFormat);
		else this.fileChooser.setFileFilter(this.fileChooser.getAcceptAllFileFilter());
		
		this.fileChooser.setAccessory(typeSelector);
		
		this.fileKeeper.setFile(target);
		this.fileKeeper.setLoading(false);
		if (this.fileChooser.showSaveDialog(DialogPanel.getTopWindow()) == JFileChooser.APPROVE_OPTION) {
			this.fileChooser.setAccessory(null);
			target = this.fileChooser.getSelectedFile();
			this.lastSaveFile = target;
			
			FileFilter selectedFilter = this.fileChooser.getFileFilter();
			if ((target != null) && !(selectedFilter instanceof DocumentFormat)) {
				ArrayList formatList = new ArrayList();
				for (int d = 0; d < formatters.length; d++) {
					DocumentFormat[] formats = formatters[d].getSaveFileFilters();
					for (int f = 0; f < formats.length; f++) formatList.add(formats[f]);
				}
				SelectFormatDialog sfd = new SelectFormatDialog(false, true, "Save", ("'" + this.fileChooser.getAcceptAllFileFilter().getDescription() + "' is not a valid storage format, please select a format."), ((DocumentFormat[]) formatList.toArray(new DocumentFormat[formatList.size()])));
				sfd.setVisible(true);
				if (sfd.isCommitted) {
					selectedFilter = sfd.getFormat();
					String extension = ((DocumentFormat) selectedFilter).getDefaultSaveFileExtension();
					if ((extension != null) && !target.getName().endsWith(extension))
						target = new File(target.getParentFile(), (target.getName() + "." + extension));
				}
				else target = null;
			}
			
			if (target != null) {
				
				//	obtain main name
				String mainName = target.getAbsolutePath();
				mainName = mainName.substring(0, mainName.lastIndexOf("."));
				String fileExtension = target.getAbsolutePath().substring(mainName.length());
				
				String partType = typeSelector.getSelectedString();
				QueriableAnnotation[] parts = doc.getAnnotations(partType);
				
				try {
					for (int p = 0; p < parts.length; p++) {
						
						//	create part file
						target = new File(mainName + "." + partType.replaceAll("\\:", "\\-") + "[" + p + "]" + fileExtension);
						
						//	write document
						OutputStream out = new FileOutputStream(target);
						((DocumentFormat) selectedFilter).saveDocument(data, parts[p], out);
						out.flush();
						out.close();
					}
					
					return partType;
				}
				catch (IOException ioe) {
					JOptionPane.showMessageDialog(data, (ioe.getClass().getName() + " (" + ioe.getMessage() + ")\nwhile saving document parts to " + mainName), "Could Not Save To File", JOptionPane.ERROR_MESSAGE);
					return null;
				}
			}
			else return null;
		}
		else {
			this.fileChooser.setAccessory(null);
			return null;
		}
	}
	
	private class FileSaveOperation implements DocumentSaveOperation {

		private File file;
		private DocumentFormat format;

		private FileSaveOperation(File file, DocumentFormat format, FileDocumentIO parent) {
			this.file = file;
			this.format = format;
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentSaveOperation#keepAsDefault()
		 */
		public boolean keepAsDefault() {
			return false;
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentSaveOperation#saveDocument(de.uka.ipd.idaho.gamta.QueriableAnnotation)
		 */
		public String saveDocument(QueriableAnnotation data) {
			try {
				
				//	use specialized output stream that creates/renames file only on first write
				FormattedFileOutputStream ffos = new FormattedFileOutputStream();
				
				//	save document
				if (!this.format.saveDocument(data, ffos)) {
					
					//	if format could not save, open dialog to allow for selecting a different format
					this.format = this.selectDocumentFormat(this.format);
					
					//	reset output stream and try again
					ffos.resetBuffer();
					this.format.saveDocument(data, ffos);
				}
				
				//	finish saving
				ffos.flush();
				ffos.close();
				
				//	return eventual file name
				return this.file.getName();
			}
			catch (IOException ioe) {
				JOptionPane.showMessageDialog(DialogPanel.getTopWindow(), (ioe.getClass().getName() + " (" + ioe.getMessage() + ")\nwhile saving document to " + this.file.toString()), "Could Not Save To File", JOptionPane.ERROR_MESSAGE);
				return null;
			}
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentSaveOperation#saveDocument(de.uka.ipd.idaho.goldenGate.DocumentEditor)
		 */
		public String saveDocument(DocumentEditor data) {
			try {
				
				//	use specialized output stream that creates/renames file only on first write
				FormattedFileOutputStream ffos = new FormattedFileOutputStream();
				
				//	save document
				if (!this.format.saveDocument(data, ffos)) {
					
					//	if format could not save, open dialog to allow for selecting a different format
					this.format = this.selectDocumentFormat(this.format);
					
					//	reset output stream and try again
					ffos.resetBuffer();
					this.format.saveDocument(data, ffos);
				}
				
				//	finish saving
				ffos.flush();
				ffos.close();
				
				//	return eventual file name
				return this.file.getName();
			}
			catch (IOException ioe) {
				JOptionPane.showMessageDialog(data, (ioe.getClass().getName() + " (" + ioe.getMessage() + ")\nwhile saving document to " + this.file.toString()), "Could Not Save To File", JOptionPane.ERROR_MESSAGE);
				return null;
			}
		}
		
		private class FormattedFileOutputStream extends OutputStream {
			private OutputStream out;
			private int[] buffer = new int[16]; // buffer for byte order mark and other prefixes, which DocumentFormat writes before calling saveDocument() and possibly returns false
			private int bufferLevel = 0;
			FormattedFileOutputStream() {}
			public void write(int b) throws IOException {
				if ((this.out == null) && (this.bufferLevel < this.buffer.length))
					this.buffer[this.bufferLevel++] = b;
				else {
					this.checkOutputStream();
					this.out.write(b);
				}
			}
			public void write(byte[] b) throws IOException {
				this.write(b, 0, b.length);
			}
			public void write(byte[] b, int off, int len) throws IOException {
				if ((this.out == null) && ((this.bufferLevel + len) < this.buffer.length)) {
					for (int i = off; len > 0; len--)
						this.buffer[this.bufferLevel++] = b[i++];
				}
				else {
					this.checkOutputStream();
					this.out.write(b, off, len);
				}
			}
			public void flush() throws IOException {
				if (this.out != null)
					this.out.flush();
			}
			public void close() throws IOException {
				if (0 < this.bufferLevel)
					this.checkOutputStream();
				if (this.out != null)
					this.out.close();
			}
			private void checkOutputStream() throws IOException {
				if (this.out != null)
					return;
				if ((format.getDefaultSaveFileExtension() != null) && !file.getName().toLowerCase().endsWith(format.getDefaultSaveFileExtension().toLowerCase()))
					file = new File(file.getAbsolutePath() + "." + format.getDefaultSaveFileExtension());
				file = ensureNewFile(file);
				file.createNewFile();
				this.out = new BufferedOutputStream(new FileOutputStream(file));
				if (0 < this.bufferLevel) {
					for (int i = 0; i < this.bufferLevel; i++)
						this.out.write(this.buffer[i]);
					this.out.flush();
				}
			}
			void resetBuffer() {
				if (this.out == null)
					this.bufferLevel = 0;
			}
		}
		
		private File ensureNewFile(File file) {
			if (file.exists()) {
				String targetName = file.toString();
				File oldTarget = new File(targetName + "." + System.currentTimeMillis() + ".old");
				file.renameTo(oldTarget);
				return new File(targetName);
			}
			else return file;
		}
		
		private DocumentFormat selectDocumentFormat(DocumentFormat format) {
			DocumentFormatProvider[] formatters = parent.getDocumentFormatProviders();
			ArrayList formatList = new ArrayList();
			for (int d = 0; d < formatters.length; d++) {
				DocumentFormat[] formats = formatters[d].getSaveFileFilters();
				for (int f = 0; f < formats.length; f++) formatList.add(formats[f]);
			}
			SelectFormatDialog sfd = new SelectFormatDialog(false, false, "Save", ("'" + format.getDescription() + "' cannot save the document, please select a different format."), ((DocumentFormat[]) formatList.toArray(new DocumentFormat[formatList.size()])));
			sfd.setVisible(true);
			return sfd.getFormat();
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentSaveOperation#getDocumentFormat()
		 */
		public DocumentFormat getDocumentFormat() {
			return this.format;
		}
		
//		/*
//		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentSaveOperation#getDocumentSaver()
//		 */
//		public DocumentSaver getDocumentSaver() {
//			return FileDocumentIO.this;
//		}
//		
		/*
		 * @return the title of a document after it has been saved through this SaveOperation
		 */
		public String getDocumentName() {
			return this.file.getName();
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentSaveOperation#documentClosed()
		 */
		public void documentClosed() {
			// nothing to do here
		}
	}
	
	private class SelectFormatDialog extends DialogPanel {
		
		private JComboBox formatChooser;
		private boolean isCommitted = false;
		
		SelectFormatDialog(boolean isLoadDialog, boolean isCancellable, String commitButtonText, String text, DocumentFormat[] formats) {
			super("Select Document Format", true);
			
			this.formatChooser = new JComboBox(formats);
			this.formatChooser.setBorder(BorderFactory.createLoweredBevelBorder());
			this.formatChooser.setEditable(false);
			String defaultFormatName = (isLoadDialog ? DocumentFormat.getDefaultLoadFormatName() : DocumentFormat.getDefaultSaveFormatName());
			if (defaultFormatName != null)
				for (int f = 0; f < formats.length; f++)
					if (defaultFormatName.equals(formats[f].getDescription()))
						this.formatChooser.setSelectedItem(formats[f]);
			
			JPanel selectorPanel = new JPanel(new GridBagLayout());
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.weighty = 0;
			gbc.gridwidth = 1;
			gbc.gridheight = 1;
			gbc.insets.top = 5;
			gbc.insets.left = 5;
			gbc.insets.right = 5;
			gbc.insets.bottom = 5;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			
			gbc.gridy = 0;
			gbc.gridx = 0;
			gbc.gridwidth = 2;
			gbc.weightx = 2;
			selectorPanel.add(new JLabel(text), gbc.clone());
			
			gbc.gridy = 1;
			gbc.gridx = 0;
			gbc.gridwidth = 1;
			gbc.weightx = 0;
			selectorPanel.add(new JLabel("Format"), gbc.clone());
			gbc.gridx = 1;
			gbc.weightx = 1;
			selectorPanel.add(this.formatChooser, gbc.clone());
			
			//	initialize main buttons
			JPanel mainButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
			JButton commitButton = new JButton(commitButtonText);
			commitButton.setBorder(BorderFactory.createRaisedBevelBorder());
			commitButton.setPreferredSize(new Dimension(100, 21));
			commitButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					isCommitted = true;
					dispose();
				}
			});
			mainButtonPanel.add(commitButton);
			
			if (isCancellable) {
				JButton abortButton = new JButton("Cancel");
				abortButton.setBorder(BorderFactory.createRaisedBevelBorder());
				abortButton.setPreferredSize(new Dimension(100, 21));
				abortButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						dispose();
					}
				});
				mainButtonPanel.add(abortButton);
			}
			
			//	put the whole stuff together
			this.add(selectorPanel, BorderLayout.CENTER);
			this.add(mainButtonPanel, BorderLayout.SOUTH);
			
			this.setResizable(true);
			this.setSize(new Dimension(500, 120));
			this.setLocationRelativeTo(DialogPanel.getTopWindow());
		}
		
		DocumentFormat getFormat() {
			return ((DocumentFormat) this.formatChooser.getSelectedItem());
		}
	}
}
