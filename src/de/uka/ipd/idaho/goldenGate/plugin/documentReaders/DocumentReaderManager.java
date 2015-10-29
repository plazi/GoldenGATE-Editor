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
package de.uka.ipd.idaho.goldenGate.plugin.documentReaders;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import javax.swing.text.html.HTMLDocument;

import de.uka.ipd.idaho.easyIO.settings.Settings;
import de.uka.ipd.idaho.gamta.DocumentRoot;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.util.SgmlDocumentReader;
import de.uka.ipd.idaho.goldenGate.DocumentEditor;
import de.uka.ipd.idaho.goldenGate.plugins.AbstractDocumentFormatProvider;
import de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat;
import de.uka.ipd.idaho.goldenGate.plugins.SettingsPanel;
import de.uka.ipd.idaho.goldenGate.util.DataListListener;
import de.uka.ipd.idaho.goldenGate.util.DialogPanel;
import de.uka.ipd.idaho.goldenGate.util.EditFontsButton;
import de.uka.ipd.idaho.goldenGate.util.FontEditable;
import de.uka.ipd.idaho.goldenGate.util.FontEditorDialog;
import de.uka.ipd.idaho.goldenGate.util.FontEditorPanel;
import de.uka.ipd.idaho.goldenGate.util.ResourceDialog;
import de.uka.ipd.idaho.htmlXmlUtil.Parser;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.Grammar;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.Html;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.StandardGrammar;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * Document format provider managing document reader definitions (as a resource
 * manager does with resources). A document reader is a set of rules that can
 * modify and filter the markup of an HTML or XML document on reading. This is
 * useful for both filtering useless layout markup from HTML documents, and for
 * mapping the markup of XML documents into GoldenGATE's generic scheme of
 * sections, subSections, subSubSections, and paragraphs.
 * 
 * @author sautter
 */
public class DocumentReaderManager extends AbstractDocumentFormatProvider {
	
	private static final String EXTENSIONS_TO_READERS_FILE_NAME = "ExtensionsToReaders.cnfg";
	private static final String FILE_EXTENSION = ".documentReader";
	
	private static final String[] DEFAULT_FILE_EXTENSIONS = {"xml", "htm", "html", "sgm", "sgml"};
	
	private Settings fileExtensionsToDocumentReaders = new Settings();
	
	public DocumentReaderManager() {}
	
	/* 
	 * @see de.uka.ipd.idaho.goldenGate.plugins.GoldenGatePlugin#getPluginName()
	 */
	public String getPluginName() {
		return "Document Reader Manager";
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.ResourceManager#getResourceTypeLabel()
	 */
	public String getResourceTypeLabel() {
		return "SGML Document Reader";
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.ResourceManager#init()
	 */
	public void init() {
		try {
			InputStream is = this.dataProvider.getInputStream(EXTENSIONS_TO_READERS_FILE_NAME);
			this.fileExtensionsToDocumentReaders = Settings.loadSettings(is);
			is.close();
		} catch (IOException e1) {
			this.fileExtensionsToDocumentReaders = new Settings();
		}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager#getFileExtension()
	 */
	protected String getFileExtension() {
		return FILE_EXTENSION;
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.GoldenGatePlugin#getSettingsPanel()
	 */
	public SettingsPanel getSettingsPanel() {
		return new DocumentReaderSettingsPanel();
	}
	
	private class DocumentReaderSettingsPanel extends SettingsPanel {
		
		private DocumentReaderEditorPanel editor;
		
		DocumentReaderSettingsPanel() {
			super("Document Readers", "Custom readers for SGML formatted documents.");
			this.setLayout(new BorderLayout());
			this.setDoubleBuffered(true);
			
			JPanel editButtons = new JPanel(new FlowLayout(FlowLayout.CENTER));
			JButton button;
			button = new JButton("Create");
			button.setBorder(BorderFactory.createRaisedBevelBorder());
			button.setPreferredSize(new Dimension(100, 21));
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					if (createDocumentReader())
						resourceNameList.refresh();
				}
			});
			editButtons.add(button);
			button = new JButton("Clone");
			button.setBorder(BorderFactory.createRaisedBevelBorder());
			button.setPreferredSize(new Dimension(100, 21));
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					if (cloneDocumentReader())
						resourceNameList.refresh();
				}
			});
			editButtons.add(button);
			button = new JButton("Delete");
			button.setBorder(BorderFactory.createRaisedBevelBorder());
			button.setPreferredSize(new Dimension(100, 21));
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					if (deleteDocumentReader())
						resourceNameList.refresh();
				}
			});
			editButtons.add(button);
			
			final JPanel editorPanel = new JPanel(new BorderLayout());
			String selectedName = resourceNameList.getSelectedName();
			if (selectedName == null)
				editorPanel.add(getExplanationLabel(), BorderLayout.CENTER);
			else {
				Settings set = readFile(selectedName);
				if (set == null)
					editorPanel.add(getExplanationLabel(), BorderLayout.CENTER);
				else {
					this.editor = new DocumentReaderEditorPanel(selectedName, set, getFileExtensionsForDocumentReader(selectedName));
					editorPanel.add(this.editor, BorderLayout.CENTER);
				}
			}
			
			this.add(editButtons, BorderLayout.NORTH);
			this.add(editorPanel, BorderLayout.CENTER);
			this.add(resourceNameList, BorderLayout.EAST);
			
			resourceNameList.addDataListListener(new DataListListener() {
				public void selected(String dataName) {
					if ((editor != null)) {
						try {
							boolean changed = false;
							if (editor.isDirty()) {
								storeDocumentReader(editor.documentReaderName, editor.getSettings());
								changed = true;
							}
							if (editor.fileTypesChanged()) {
								StringVector fileTypes = editor.getFileTypes();
								for (int t = 0; t < fileTypes.size(); t++)
									fileExtensionsToDocumentReaders.setSetting(fileTypes.get(t).toLowerCase(), editor.documentReaderName);
								
								OutputStream os = dataProvider.getOutputStream(EXTENSIONS_TO_READERS_FILE_NAME);
								fileExtensionsToDocumentReaders.storeAsText(os);
								os.flush();
								os.close();
								changed = true;
							}
							if (changed) parent.notifyResourcesChanged(DocumentReaderManager.class.getName());
						}
						catch (IOException ioe) {
							if (JOptionPane.showConfirmDialog(DocumentReaderSettingsPanel.this, (ioe.getClass().getName() + " (" + ioe.getMessage() + ")\nwhile saving file to " + editor.documentReaderName + "\nProceed?"), "Could Not Save DocumentReader", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
								resourceNameList.setSelectedName(editor.documentReaderName);
								validate();
								return;
							}
						}
					}
					
					if (dataName != null) {
						Settings set = readFile(dataName);
						if (set != null) {
							editorPanel.removeAll();
							editor = new DocumentReaderEditorPanel(dataName, set, getFileExtensionsForDocumentReader(dataName));
							editorPanel.add(editor, BorderLayout.CENTER);
							editorPanel.validate();
						}
					}
				}
			});
			
			this.addComponentListener(new ComponentAdapter() {
				public void componentHidden(ComponentEvent ce) {
					if ((editor != null) && editor.isDirty()) {
						try {
							storeDocumentReader(editor.documentReaderName, editor.getSettings());
							StringVector fileTypes = editor.getFileTypes();
							String fileName = editor.documentReaderName;
							fileName = fileName.substring(fileName.lastIndexOf('/') + 1);
							for (int t = 0; t < fileTypes.size(); t++)
								fileExtensionsToDocumentReaders.setSetting(fileTypes.get(t).toLowerCase(), fileName);
							
							OutputStream os = dataProvider.getOutputStream(EXTENSIONS_TO_READERS_FILE_NAME);
							fileExtensionsToDocumentReaders.storeAsText(os);
							os.flush();
							os.close();
							
							parent.notifyResourcesChanged(DocumentReaderManager.class.getName());
						} catch (IOException ioe) {}
					}
				}
			});
		}
		
		JScrollPane getExplanationLabel() {
			JEditorPane text = new JEditorPane();
			text.setContentType("text/html");
			try {
				text.read(getHelp().getTextReader(), new HTMLDocument());
			}
			catch (IOException e) {
				text.setText("<html><body>" +
				"<h3><font face=\"Verdana\">The SGML Readers</font></h3>" +
				"<font face=\"Verdana\" size=\"2\">Help on SGML Readers is not available, please contact your administrator.</font>" +
				"</body></html>");
			}
			return new JScrollPane(text);
		}
		
		public void commitChanges() {
			//	do nothing here, since changes are written as they are made
		}
		
		boolean createDocumentReader() {
			return this.createDocumentReader(new Settings(), new StringVector(), null);
		}
		
		boolean cloneDocumentReader() {
			String selectedName = resourceNameList.getSelectedName();
			if (selectedName == null)
				return this.createDocumentReader();
			else {
				String name = "New " + selectedName;
				StringVector fileTypes = getFileExtensionsForDocumentReader(selectedName);
				Settings set = ((this.editor == null) ? readFile(selectedName) : this.editor.getSettings());
				return this.createDocumentReader(set, fileTypes, name);
			}
		}
		
		boolean createDocumentReader(Settings modelDocumentReader, StringVector fileTypes, String name) {
			CreateDocumentReaderDialog cpd = new CreateDocumentReaderDialog(name, modelDocumentReader, fileTypes);
			cpd.setVisible(true);
			
			if (cpd.isCommitted()) {
				Settings documentReader = cpd.getDocumentReader();
				String documentReaderName = cpd.getDocumentReaderName();
				if (!documentReaderName.endsWith(FILE_EXTENSION)) documentReaderName += FILE_EXTENSION;
				try {
					if (storeDocumentReader(documentReaderName, documentReader)) {
						StringVector fileExtensions = cpd.getFileExtensions();
						for (int e = 0; e < fileExtensions.size(); e++)
							fileExtensionsToDocumentReaders.setSetting(fileExtensions.get(e).toLowerCase(), documentReaderName);
						try {
							OutputStream os = dataProvider.getOutputStream(EXTENSIONS_TO_READERS_FILE_NAME);
							fileExtensionsToDocumentReaders.storeAsText(os);
							os.flush();
							os.close();
							parent.notifyResourcesChanged(DocumentReaderManager.class.getName());
							return true;
						}
						catch (IOException e) {}
					}
				}
				catch (IOException e) {}
			}
			return false;
		}
		
		boolean deleteDocumentReader() {
			String name = resourceNameList.getSelectedName();
			if ((name != null) && (JOptionPane.showConfirmDialog(this, ("Really delete " + name), "Confirm Delete DocumentReader", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION)) {
				try {
					if (dataProvider.deleteData(name)) {
						resourceNameList.refresh();
						StringVector fileExtensions = getFileExtensionsForDocumentReader(resourceNameList.getSelectedName());
						for (int e = 0; e < fileExtensions.size(); e++)
							fileExtensionsToDocumentReaders.removeSetting(fileExtensions.get(e).toLowerCase());
						
						OutputStream os = dataProvider.getOutputStream(EXTENSIONS_TO_READERS_FILE_NAME);
						fileExtensionsToDocumentReaders.storeAsText(os);
						os.flush();
						os.close();
						
						parent.notifyResourcesChanged(DocumentReaderManager.class.getName());
						return true;
					}
					else {
						JOptionPane.showMessageDialog(this, ("Could not delete " + name), "Delete Failed", JOptionPane.INFORMATION_MESSAGE);
						return false;
					}
				}
				catch (Exception ioe) {
					JOptionPane.showMessageDialog(this, ("Could not delete " + name), "Delete Failed", JOptionPane.INFORMATION_MESSAGE);
					return false;
				}
			}
			else return false;
		}
	}
	
	/* retrieve a DocumentReader by its name
	 * @param	name	the name of the reqired DocumentReader
	 * @return the Document with the required name, or null, if there is no such DocumentReader
	 */
	private HtmlXmlDocumentReader getDocumentReader(String name) {
		if (name == null) return null;
		Settings set = this.readFile(name);
		return ((set == null) ? null : new HtmlXmlDocumentReader(set));
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormatProvider#getFileExtensions()
	 */
	public String[] getFileExtensions() {
		StringVector extensions = new StringVector(false);
		extensions.addContentIgnoreDuplicates(DEFAULT_FILE_EXTENSIONS);
		String[] customExtensions = this.fileExtensionsToDocumentReaders.getKeys();
		for (int e = 0; e < customExtensions.length; e++)
			customExtensions[e] = customExtensions[e].toLowerCase();
		extensions.addContentIgnoreDuplicates(customExtensions);
		extensions.sortLexicographically(false);
		return extensions.toStringArray();
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormatProvider#getFormatForFileExtension(java.lang.String)
	 */
	public DocumentFormat getFormatForFileExtension(String fileExtension) {
		String extension = fileExtension.toLowerCase();
		if (extension.startsWith(".")) extension = extension.substring(1);
		String readerName = this.fileExtensionsToDocumentReaders.getSetting(extension);
		if (readerName == null)
			return null; 
		else {
			HtmlXmlDocumentReader reader = this.getDocumentReader(readerName);
			if (reader == null)
				return null;
			else {
				String[] extensions = {extension};
				return new FixedReaderDocumentFormat(readerName, reader, extensions);
			}
		}
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormatProvider#getLoadFormatNames()
	 */
	public String[] getLoadFormatNames() {
		return this.resourceNameList.getNames();
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormatProvider#getSaveFormatNames()
	 */
	public String[] getSaveFormatNames() {
		return new String[0];
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormatProvider#getFormatForName(java.lang.String)
	 */
	public DocumentFormat getFormatForName(String formatName) {
		HtmlXmlDocumentReader reader = this.getDocumentReader(formatName);
		if (reader == null)
			return null;
		else return new FixedReaderDocumentFormat(formatName, reader, DEFAULT_FILE_EXTENSIONS);
	}
	
//	/* (non-Javadoc)
//	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractDocumentFormatProvider#getDataNamesForResource(java.lang.String)
//	 */
//	public String[] getDataNamesForResource(String name) {
//		String[] names = {name + "@" + this.getClass().getName()};
//		return names;
//	}
//	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormatProvider#getLoadFileFilters()
	 */
	public DocumentFormat[] getLoadFileFilters() {
		ArrayList formats = new ArrayList();
		
		//	map document readers to file extensions
		String[] extensions = this.fileExtensionsToDocumentReaders.getKeys();
		Arrays.sort(extensions);
		StringVector readerNames = new StringVector(false);
		Properties readerNamesToFileExtensions = new Properties();
		for (int e = 0; e < extensions.length; e++) {
			String readerName = this.fileExtensionsToDocumentReaders.getSetting(extensions[e]).toLowerCase();
			readerNames.addElementIgnoreDuplicates(readerName);
			readerNamesToFileExtensions.setProperty(readerName, (readerNamesToFileExtensions.getProperty(readerName, "") + " " + extensions[e]));
		}
		
		//	produce formats for custom configured types
		StringVector extensionParser = new StringVector();
		for (int r = 0; r < readerNames.size(); r++) {
			String readerName = readerNames.get(r);
			extensionParser.parseAndAddElements(readerNamesToFileExtensions.getProperty(readerName, "").trim(), " ");
			HtmlXmlDocumentReader reader = this.getDocumentReader(readerName);
			if (reader != null)
				formats.add(new FixedReaderDocumentFormat(readerName, reader, extensionParser.toStringArray()));
			extensionParser.clear();
		}
		
		//	produce standard readers
		formats.add(new SgmlCustomReaderDocumentFormat());
		formats.add(new CustomReaderDocumentFormat());
		
		//	return formats
		return ((DocumentFormat[]) formats.toArray(new DocumentFormat[formats.size()]));
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormatProvider#getSaveFileFilters()
	 */
	public DocumentFormat[] getSaveFileFilters() {
		return new DocumentFormat[0];
	}
	
	private class FixedReaderDocumentFormat extends DocumentFormat {
		
		private String name;
		private HtmlXmlDocumentReader docReader;
		private String[] fileExtensions;
		private String description;
		
		FixedReaderDocumentFormat(String name, HtmlXmlDocumentReader docReader, String[] fileExtensions) {
			this.name = name;
			this.docReader = docReader;
			
			//	store and unify file extensions
			this.fileExtensions = fileExtensions;
			for (int e = 0; e < this.fileExtensions.length; e++) {
				this.fileExtensions[e] = this.fileExtensions[e].toLowerCase();
				if (this.fileExtensions[e].startsWith("."))
					this.fileExtensions[e] = this.fileExtensions[e].substring(1);
			}
			
			//	build description
			if (this.fileExtensions.length == 0)
				this.description = name;
			else {
				for (int e = 0; e < this.fileExtensions.length; e++) {
					if (e == 0) this.description = this.fileExtensions[e].toUpperCase();
					else if (e == (this.fileExtensions.length - 1)) this.description += (" and " + this.fileExtensions[e].toUpperCase());
					else this.description += (", " + this.fileExtensions[e].toUpperCase());
				}
				this.description += " files (" + ((fileExtensions == DEFAULT_FILE_EXTENSIONS) ? this.name : "default reader") + ")";
			}
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat#getDefaultSaveFileExtension()
		 */
		public String getDefaultSaveFileExtension() {
			return "xml";
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat#isExportFormat()
		 */
		public boolean isExportFormat() {
			return false;
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat#loadDocument(java.io.Reader)
		 */
		public MutableAnnotation loadDocument(Reader source) throws IOException {
			return this.docReader.readDocument(source);
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat#saveDocument(de.uka.ipd.idaho.gamta.MutableAnnotation, java.io.Writer)
		 */
		public boolean saveDocument(QueriableAnnotation data, Writer out) throws IOException {
			return false;
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat#saveDocument(de.uka.ipd.idaho.gamta.MutableAnnotation, java.io.Writer)
		 */
		public boolean saveDocument(DocumentEditor data, Writer out) throws IOException {
			return false;
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat#saveDocument(de.goldenGate.DocumentEditor, de.uka.ipd.idaho.gamta.MutableAnnotation, java.io.Writer)
		 */
		public boolean saveDocument(DocumentEditor data, QueriableAnnotation doc, Writer out) throws IOException {
			return false;
		}
		
		/* 
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat#accept(java.lang.String)
		 */
		public boolean accept(String fileName) {
			for (int e = 0; e < this.fileExtensions.length; e++)
				if (fileName.endsWith("." + this.fileExtensions[e])) return true;
			return false;
		}
		
		/*
		 * @see javax.swing.filechooser.FileFilter#getDescription()
		 */
		public String getDescription() {
			return this.description;
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat#equals(de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat)
		 */
		public boolean equals(DocumentFormat format) {
			if (format == null) return false;
			if (!(format instanceof FixedReaderDocumentFormat)) return false;
			FixedReaderDocumentFormat frdf = ((FixedReaderDocumentFormat) format);
			if (this.name == null) return (frdf.name == null);
			else return this.name.equals(frdf.name);
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGate.plugins.Resource#getName()
		 */
		public String getName() {
			return this.name;
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGate.plugins.Resource#getProviderClassName()
		 */
		public String getProviderClassName() {
			return DocumentReaderManager.class.getName();
		}
	}

	private class SgmlCustomReaderDocumentFormat extends DocumentFormat {
		
		private String name;
		private HtmlXmlDocumentReader docReader = null;
		
		SgmlCustomReaderDocumentFormat() {}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat#getDefaultSaveFileExtension()
		 */
		public String getDefaultSaveFileExtension() {
			return "xml";
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat#isExportFormat()
		 */
		public boolean isExportFormat() {
			return false;
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat#loadDocument(java.io.Reader)
		 */
		public MutableAnnotation loadDocument(Reader source) throws IOException {
			if (this.docReader == null) {
				ResourceDialog rd = ResourceDialog.getResourceDialog(getLoadFormatNames(), "Select Document Reader", "Select");
				rd.setVisible(true);
				if (rd.isCommitted()) {
					this.name = rd.getSelectedResourceName();
					this.docReader = getDocumentReader(this.name);
				}
			}
			if (this.docReader == null)
				return null;
			else return this.docReader.readDocument(source);
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat#saveDocument(de.uka.ipd.idaho.gamta.MutableAnnotation, java.io.Writer)
		 */
		public boolean saveDocument(QueriableAnnotation data, Writer out) throws IOException {
			return false;
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat#saveDocument(de.uka.ipd.idaho.gamta.MutableAnnotation, java.io.Writer)
		 */
		public boolean saveDocument(DocumentEditor data, Writer out) throws IOException {
			return false;
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat#saveDocument(de.goldenGate.DocumentEditor, de.uka.ipd.idaho.gamta.MutableAnnotation, java.io.Writer)
		 */
		public boolean saveDocument(DocumentEditor data, QueriableAnnotation doc, Writer out) throws IOException {
			return false;
		}
		
		/* 
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat#accept(java.lang.String)
		 */
		public boolean accept(String fileName) {
			fileName = fileName.toLowerCase();
			return (fileName.endsWith(".xml") || fileName.endsWith(".html") || fileName.endsWith(".htm") || fileName.endsWith(".sgml") || fileName.endsWith(".sgm"));
		}
		
		/*
		 * @see javax.swing.filechooser.FileFilter#getDescription()
		 */
		public String getDescription() {
			return "HTML, XML and SGML files (custom reader)";
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat#equals(de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat)
		 */
		public boolean equals(DocumentFormat format) {
			if (format == null) return false;
			if (!(format instanceof SgmlCustomReaderDocumentFormat)) return false;
			SgmlCustomReaderDocumentFormat scrdf = ((SgmlCustomReaderDocumentFormat) format);
			if (this.name == null) return true;	//	allow an undetermined instance to be pre-selected if the previous selection was another instance that has been determined in the meantime 
			else return this.name.equals(scrdf.name);
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGate.plugins.Resource#getName()
		 */
		public String getName() {
			return this.name;
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGate.plugins.Resource#getProviderClassName()
		 */
		public String getProviderClassName() {
			return DocumentReaderManager.class.getName();
		}
	}

	private class CustomReaderDocumentFormat extends DocumentFormat {
		
		private String name;
		private HtmlXmlDocumentReader docReader = null;
		
		CustomReaderDocumentFormat() {}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat#getDefaultSaveFileExtension()
		 */
		public String getDefaultSaveFileExtension() {
			return "xml";
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat#isExportFormat()
		 */
		public boolean isExportFormat() {
			return false;
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat#loadDocument(java.io.Reader)
		 */
		public MutableAnnotation loadDocument(Reader source) throws IOException {
			if (this.docReader == null) {
				ResourceDialog rd = ResourceDialog.getResourceDialog(getLoadFormatNames(), "Select Document Reader", "Select");
				rd.setVisible(true);
				if (rd.isCommitted()) {
					this.name = rd.getSelectedResourceName();
					this.docReader = getDocumentReader(this.name);
				}
			}
			if (this.docReader == null)
				return null;
			else return this.docReader.readDocument(source);
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat#saveDocument(de.uka.ipd.idaho.gamta.MutableAnnotation, java.io.Writer)
		 */
		public boolean saveDocument(QueriableAnnotation data, Writer out) throws IOException {
			return false;
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat#saveDocument(de.uka.ipd.idaho.gamta.MutableAnnotation, java.io.Writer)
		 */
		public boolean saveDocument(DocumentEditor data, Writer out) throws IOException {
			return false;
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat#saveDocument(de.goldenGate.DocumentEditor, de.uka.ipd.idaho.gamta.MutableAnnotation, java.io.Writer)
		 */
		public boolean saveDocument(DocumentEditor data, QueriableAnnotation doc, Writer out) throws IOException {
			return false;
		}
		
		/* 
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat#accept(java.lang.String)
		 */
		public boolean accept(String fileName) {
			return true;
		}
		
		/*
		 * @see javax.swing.filechooser.FileFilter#getDescription()
		 */
		public String getDescription() {
			return "All files (custom SGML reader)";
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat#equals(de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat)
		 */
		public boolean equals(DocumentFormat format) {
			if (format == null) return false;
			if (!(format instanceof CustomReaderDocumentFormat)) return false;
			CustomReaderDocumentFormat crdf = ((CustomReaderDocumentFormat) format);
			if (this.name == null) return true;	//	allow an undetermined instance to be pre-selected if the previous selection was another instance that has been determined in the meantime 
			else return this.name.equals(crdf.name);
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGate.plugins.Resource#getName()
		 */
		public String getName() {
			return this.name;
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGate.plugins.Resource#getProviderClassName()
		 */
		public String getProviderClassName() {
			return DocumentReaderManager.class.getName();
		}
	}
	
	private Settings readFile(String name) {
		try {
			InputStream is = this.dataProvider.getInputStream(name);
			Settings set = Settings.loadSettings(is);
			is.close();
			return set;
		}
		catch (IOException e) {
			return null;
		}
	}
	
	private boolean storeDocumentReader(String name, Settings documentReader) throws IOException {
		if (this.dataProvider.isDataEditable(name)) {
			OutputStream os = this.dataProvider.getOutputStream(name);
			documentReader.storeAsText(os);
			os.flush();
			os.close();
			return true;
		}
		else return false;
	}
	
	private StringVector getFileExtensionsForDocumentReader(String documentReaderName) {
		StringVector types = new StringVector();
		String[] fileExtensions = this.fileExtensionsToDocumentReaders.getKeys();
		for (int e = 0; e < fileExtensions.length; e++)
			if (this.fileExtensionsToDocumentReaders.getSetting(fileExtensions[e].toLowerCase(), "").equalsIgnoreCase(documentReaderName))
				types.addElementIgnoreDuplicates(fileExtensions[e]);
		return types;
	}
	
	private class CreateDocumentReaderDialog extends DialogPanel {
		
		private JTextField nameField;
		
		private DocumentReaderEditorPanel editor;
		private String documentReaderName = null;
		
		CreateDocumentReaderDialog(String name, Settings documentReader, StringVector fileTypes) throws HeadlessException {
			super("Create DocumentReader", true);
			
			this.nameField = new JTextField((name == null) ? "New DocumentReader" : name);
			this.nameField.setBorder(BorderFactory.createLoweredBevelBorder());
			
			//	initialize main buttons
			JButton commitButton = new JButton("Create");
			commitButton.setBorder(BorderFactory.createRaisedBevelBorder());
			commitButton.setPreferredSize(new Dimension(100, 21));
			commitButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					documentReaderName = nameField.getText();
					dispose();
				}
			});
			
			JButton abortButton = new JButton("Cancel");
			abortButton.setBorder(BorderFactory.createRaisedBevelBorder());
			abortButton.setPreferredSize(new Dimension(100, 21));
			abortButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					documentReaderName = null;
					dispose();
				}
			});
			
			JPanel mainButtonPanel = new JPanel();
			mainButtonPanel.setLayout(new FlowLayout());
			mainButtonPanel.add(commitButton);
			mainButtonPanel.add(abortButton);
			
			//	initialize editor
			this.editor = new DocumentReaderEditorPanel(name, documentReader, fileTypes);
			
			//	put the whole stuff together
			this.add(this.nameField, BorderLayout.NORTH);
			this.add(this.editor, BorderLayout.CENTER);
			this.add(mainButtonPanel, BorderLayout.SOUTH);
			
			this.setResizable(true);
			this.setSize(new Dimension(600, 400));
			this.setLocationRelativeTo(DialogPanel.getTopWindow());
		}
		
		boolean isCommitted() {
			return (this.documentReaderName != null);
		}
		
		Settings getDocumentReader() {
			return this.editor.getSettings();
		}
		
		String getDocumentReaderName() {
			return this.documentReaderName;
		}
		
		StringVector getFileExtensions() {
			return this.editor.getFileTypes();
		}
	}

	private class DocumentReaderEditorPanel extends JPanel {
		
		private TagListEditorPanel paragraphTagListEditor = new TagListEditorPanel();
		private TagListEditorPanel ignoreTagListEditor = new TagListEditorPanel();
		private JTable tagMappingTable = new JTable();
		private TagListEditorPanel fileTypeListEditor = new TagListEditorPanel();
		
		private StringVector paragraphTags = new StringVector();
		private StringVector ignoreTags = new StringVector();
		
		private StringVector mappedTags = new StringVector();
		private Properties tagMappings = new Properties();
		
		private StringVector fileTypes = new StringVector();
		
		private JCheckBox parseStyleSelector = new JCheckBox("Use HTML parse style", true);
		
		private boolean dirty = false;
		private String documentReaderName;
		
		DocumentReaderEditorPanel(String name, Settings set, StringVector fileTypes) {
			super(new BorderLayout(), true);
			this.documentReaderName = name;
			
			JTabbedPane tabs = new JTabbedPane();
			tabs.addTab("File Types", null, this.fileTypeListEditor, "List of file extensions which this Document Reader is applicabel to");
			tabs.addTab("Paragraph Tags", null, this.paragraphTagListEditor, "List of tags at the end of which the Document should have a line break");
			tabs.addTab("Ignore Tags", null, this.ignoreTagListEditor, "List of Tags to ignore, i.e. not to represent as Annotations od MutableAnnotations");
			
			JPanel tagMappingEditor = new JPanel(new BorderLayout(), true);
			JScrollPane tabMappingTableBox = new JScrollPane(this.tagMappingTable);
			tagMappingEditor.add(tabMappingTableBox, BorderLayout.CENTER);
			
			JButton button;
			JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER), true);
			button = new JButton("Add Mapping");
			button.setBorder(BorderFactory.createRaisedBevelBorder());
			button.setSize(new Dimension(120, 21));
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					addTagMapping();
				}
			});
			buttonPanel.add(button);
			button = new JButton("Remove Mapping");
			button.setBorder(BorderFactory.createRaisedBevelBorder());
			button.setSize(new Dimension(120, 21));
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					removeTagMapping();
				}
			});
			buttonPanel.add(button);
			this.parseStyleSelector.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent e) {
					dirty = true;
				}
			});
			buttonPanel.add(this.parseStyleSelector);
			
			tagMappingEditor.add(buttonPanel, BorderLayout.SOUTH);
			tabs.addTab("Tag Mappings", null, tagMappingEditor, "Mapping of tags");
			this.add(tabs, BorderLayout.CENTER);
			this.setContent(set, fileTypes);
		}
		
		void addTagMapping() {
			String tag = JOptionPane.showInputDialog(this, "Enter the tag to map. The mapping can be changed in the table.", "Add Tag Mapping", JOptionPane.PLAIN_MESSAGE);
			if (tag != null) {
				if (!this.mappedTags.contains(tag) || (JOptionPane.showConfirmDialog(this, ("The specified Tag is already mapped to " + this.tagMappings.getProperty(tag, "") + ". Change the mapping?"), "Tag Already Mapped", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION)) {
					this.mappedTags.addElementIgnoreDuplicates(tag);
					this.tagMappings.setProperty(tag, tag);
					this.tagMappingTable.setModel(new TagMappingTableModel(this.mappedTags, this.tagMappings));
					this.tagMappingTable.setColumnSelectionAllowed(false);
					this.tagMappingTable.setRowSelectionAllowed(true);
					this.dirty = true;
					this.validate();
					int index = this.mappedTags.indexOf(tag);
					this.tagMappingTable.setRowSelectionInterval(index, index);
					this.tagMappingTable.editCellAt(index, 1);
				}
			}
		}
		
		void removeTagMapping() {
			int row = this.tagMappingTable.getSelectedRow();
			if ((row > -1) && (row < this.mappedTags.size())) {
				String tag = this.mappedTags.remove(row);
				this.tagMappings.remove(tag);
				this.tagMappingTable.setModel(new TagMappingTableModel(this.mappedTags, this.tagMappings));
				this.tagMappingTable.setColumnSelectionAllowed(false);
				this.tagMappingTable.setRowSelectionAllowed(true);
				this.dirty = true;
				this.validate();
			}
		}
		
		void notifyMappingChanged() {
			this.dirty = true;
		}
		
		void setContent(Settings set, StringVector fileTypes) {
			StringVector parser = new StringVector();
			
			this.fileTypes.clear();
			this.fileTypes.addContentIgnoreDuplicates(fileTypes);
			this.fileTypeListEditor.setContent(this.fileTypes);
			
			this.paragraphTags.clear();
			parser.parseAndAddElements(set.getSetting(HtmlXmlDocumentReader.PARAGRAPH_TAGS_ATTRIBUTE_NAME, ""), ";");
			this.paragraphTags.addContentIgnoreDuplicates(parser);
			this.paragraphTagListEditor.setContent(this.paragraphTags);
			parser.clear();
			
			this.ignoreTags.clear();
			parser.parseAndAddElements(set.getSetting(HtmlXmlDocumentReader.IGNORE_TAGS_ATTRIBUTE_NAME, ""), ";");
			this.ignoreTags.addContentIgnoreDuplicates(parser);
			this.ignoreTagListEditor.setContent(this.ignoreTags);
			parser.clear();
			
			this.mappedTags.clear();
			this.tagMappings.clear();
			for (int m = 0; m < set.size(); m++) {
				Settings mapping = set.getSubset(HtmlXmlDocumentReader.MAPPING_ATTRIBUTE_PREFIX + "_" + m);
				parser.parseAndAddElements(mapping.getSetting(HtmlXmlDocumentReader.MAPPING_START_ATTRIBUTE_NAME, ""), ";");
				String target = mapping.getSetting(HtmlXmlDocumentReader.MAPPING_TARGET_ATTRIBUTE_NAME);
				if (target != null)
					for (int s = 0; s < parser.size(); s++) {
						this.mappedTags.addElementIgnoreDuplicates(parser.get(s));
						this.tagMappings.setProperty(parser.get(s), target);
					}
				parser.clear();
			}
			this.tagMappingTable.setModel(new TagMappingTableModel(this.mappedTags, this.tagMappings));
			this.tagMappingTable.setColumnSelectionAllowed(false);
			this.tagMappingTable.setRowSelectionAllowed(true);
			
			this.parseStyleSelector.setSelected(HtmlXmlDocumentReader.HTML_PARSE_STYLE.equals(set.getSetting(HtmlXmlDocumentReader.PARSE_STYLE_ATTRIBUTE_NAME, HtmlXmlDocumentReader.XML_PARSE_STYLE)));
			
			this.validate();
			
			this.dirty = false;
		}
		
		boolean isDirty() {
			return (this.dirty || this.paragraphTagListEditor.isDirty() || this.ignoreTagListEditor.isDirty());
		}
		
		boolean fileTypesChanged() {
			return this.fileTypeListEditor.isDirty();
		}
		
		StringVector getFileTypes() {
			if (this.fileTypeListEditor.isDirty()) this.fileTypes = this.fileTypeListEditor.getContent();
			return this.fileTypes;
		}
		
		Settings getSettings() {
			Settings set = new Settings();
			
			if (this.paragraphTagListEditor.isDirty()) this.paragraphTags = this.paragraphTagListEditor.getContent();
			set.setSetting(HtmlXmlDocumentReader.PARAGRAPH_TAGS_ATTRIBUTE_NAME, this.paragraphTags.concatStrings(";"));
			
			if (this.ignoreTagListEditor.isDirty()) this.ignoreTags = this.ignoreTagListEditor.getContent();
			set.setSetting(HtmlXmlDocumentReader.IGNORE_TAGS_ATTRIBUTE_NAME, this.ignoreTags.concatStrings(";"));
			
			StringVector mappingTargets = new StringVector();
			HashMap mappingStarts = new HashMap();
			for (int m = 0; m < this.mappedTags.size(); m++) {
				String target = this.tagMappings.getProperty(this.mappedTags.get(m));
				mappingTargets.addElementIgnoreDuplicates(target);
				StringVector starts = ((StringVector) mappingStarts.get(target));
				if (starts == null) {
					starts = new StringVector();
					mappingStarts.put(target, starts);
				}
				starts.addElementIgnoreDuplicates(this.mappedTags.get(m));
			}
			for (int m = 0; m < mappingTargets.size(); m++) {
				String target = mappingTargets.get(m);
				Settings mapping = set.getSubset(HtmlXmlDocumentReader.MAPPING_ATTRIBUTE_PREFIX + "_" + m);
				mapping.setSetting(HtmlXmlDocumentReader.MAPPING_START_ATTRIBUTE_NAME, ((StringVector) mappingStarts.get(target)).concatStrings(";"));
				mapping.setSetting(HtmlXmlDocumentReader.MAPPING_TARGET_ATTRIBUTE_NAME, target);
			}
			
			set.setSetting(HtmlXmlDocumentReader.PARSE_STYLE_ATTRIBUTE_NAME, (this.parseStyleSelector.isSelected() ? HtmlXmlDocumentReader.HTML_PARSE_STYLE : HtmlXmlDocumentReader.XML_PARSE_STYLE));
			
			return set;
		}

		private class TagMappingTableModel implements TableModel {
			
			private StringVector mappedTags;
			private Properties mappings;
			
			TagMappingTableModel(StringVector mappedTags, Properties mappings) {
				this.mappedTags = mappedTags;
				this.mappings = mappings;
			}
			
			/*
			 * @see javax.swing.table.TableModel#getColumnClass(int)
			 */
			public Class getColumnClass(int columnIndex) {
				return String.class;
			}
			
			/*
			 * @see javax.swing.table.TableModel#getColumnCount()
			 */
			public int getColumnCount() {
				return 2;
			}
			
			/*
			 * @see javax.swing.table.TableModel#getColumnName(int)
			 */
			public String getColumnName(int columnIndex) {
				if (columnIndex == 0) return "Mapped Tag";
				else if (columnIndex == 1) return "Mapping Target";
				return null;
			}
			
			/*
			 * @see javax.swing.table.TableModel#getRowCount()
			 */
			public int getRowCount() {
				return this.mappedTags.size();
			}
			
			/*
			 * @see javax.swing.table.TableModel#getValueAt(int, int)
			 */
			public Object getValueAt(int rowIndex, int columnIndex) {
				if (columnIndex == 0) return this.mappedTags.get(rowIndex);
				else if (columnIndex == 1) return this.mappings.getProperty(this.mappedTags.get(rowIndex));
				return null;
			}
			
			/*
			 * @see javax.swing.table.TableModel#setValueAt(java.lang.Object, int, int)
			 */
			public void setValueAt(Object newValue, int rowIndex, int columnIndex) {
				if ((columnIndex == 1) && (newValue != null) && (newValue instanceof String)) {
					this.mappings.setProperty(this.mappedTags.get(rowIndex), newValue.toString());
					notifyMappingChanged();
				}
			}
			
			/*
			 * @see javax.swing.table.TableModel#isCellEditable(int, int)
			 */
			public boolean isCellEditable(int rowIndex, int columnIndex) {
				return (columnIndex == 1);
			}
			
			/*
			 * @see javax.swing.table.TableModel#addTableModelListener(javax.swing.event.TableModelListener)
			 */
			public void addTableModelListener(TableModelListener l) {}
			
			/*
			 * @see javax.swing.table.TableModel#removeTableModelListener(javax.swing.event.TableModelListener)
			 */
			public void removeTableModelListener(TableModelListener l) {}
		}
		
		private class TagListEditorPanel extends JPanel implements FontEditable, DocumentListener {
			
			private JTextArea editor;
			private JScrollPane editorBox;
			
			private StringVector content = new StringVector();
			
			private String fontName = "Verdana";
			private int fontSize = 12;
			private Color fontColor = Color.BLACK;
			
			private boolean dirty = false;
			
			TagListEditorPanel() {
				super(new BorderLayout(), true);
				
				//	initialize editor
				this.editor = new JTextArea();
				this.editor.setEditable(true);
				
				//	wrap editor in scroll pane
				this.editorBox = new JScrollPane(this.editor);
				
				//	initialize buttons
				JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
				buttonPanel.add(this.getEditFontsButton());
				
				//	put the whole stuff together
				this.add(this.editorBox, BorderLayout.CENTER);
				this.add(buttonPanel, BorderLayout.SOUTH);
				this.refreshDisplay();
			}
			
			StringVector getContent() {
				if (this.isDirty()) {
					this.content.clear();
					this.content.parseAndAddElements(this.editor.getText(), "\n");
				}
				return this.content;
			}
			
			void setContent(StringVector list) {
				this.content = list;
				this.refreshDisplay();
				this.dirty = false;
			}
			
			boolean isDirty() {
				return this.dirty;
			}
			
			void refreshDisplay() {
				this.editor.setFont(new Font(this.fontName, Font.PLAIN, this.fontSize));
				this.editor.setText(this.content.concatStrings("\n"));
				this.editor.getDocument().addDocumentListener(this);
			}
			
			/*
			 * @see javax.swing.event.DocumentListener#changedUpdate(javax.swing.event.DocumentEvent)
			 */
			public void changedUpdate(DocumentEvent e) {
				//	attribute changes are not of interest for now
			}
			
			/*
			 * @see javax.swing.event.DocumentListener#insertUpdate(javax.swing.event.DocumentEvent)
			 */
			public void insertUpdate(DocumentEvent e) {
				this.dirty = true;
			}
			
			/*
			 * @see javax.swing.event.DocumentListener#removeUpdate(javax.swing.event.DocumentEvent)
			 */
			public void removeUpdate(DocumentEvent e) {
				this.dirty = true;
			}
			
			/*
			 * @see de.goldenGate.util.FontEditable#getEditFontsButton()
			 */
			public JButton getEditFontsButton() {
				return this.getEditFontsButton(null, null, null);
			}
			
			/*
			 * @see de.goldenGate.util.FontEditable#getEditFontsButton(String)
			 */
			public JButton getEditFontsButton(String text) {
				return this.getEditFontsButton(text, null, null);
			}
			
			/*
			 * @see de.goldenGate.util.FontEditable#getEditFontsButton(Dimension)
			 */
			public JButton getEditFontsButton(Dimension dimension) {
				return this.getEditFontsButton(null, dimension, null);
			}
			
			/*
			 * @see de.goldenGate.util.FontEditable#getEditFontsButton(Border)
			 */
			public JButton getEditFontsButton(Border border) {
				return this.getEditFontsButton(null, null, border);
			}
			
			/*
			 * @see de.goldenGate.util.FontEditable#getEditFontsButton(String, Dimension, Border)
			 */
			public JButton getEditFontsButton(String text, Dimension dimension, Border border) {
				return new EditFontsButton(this, text, dimension, border);
			}
			
			/*
			 * @see de.goldenGate.util.FontEditable#editFonts()
			 */
			public boolean editFonts() {
				FontEditorDialog fed = new FontEditorDialog(((JFrame) null), this.fontName, this.fontSize, this.fontColor);
				fed.setVisible(true);
				if (fed.isCommitted()) {
					FontEditorPanel font = fed.getFontEditor();
					if (font.isDirty()) {
						this.fontName = font.getFontName();
						this.fontSize = font.getFontSize();
						this.fontColor = font.getFontColor();
						dirty = true;
					}
					this.refreshDisplay();
					return true;
				}
				return false;
			}
		}
	}
	
	private class HtmlXmlDocumentReader {
		
		private static final String PARAGRAPH_TAGS_ATTRIBUTE_NAME = "PARAGRAPH_TAGS";
		private static final String IGNORE_TAGS_ATTRIBUTE_NAME = "IGNORE_TAGS";
		
		private static final String PARSE_STYLE_ATTRIBUTE_NAME = "PARSE_STYLE";
		private static final String XML_PARSE_STYLE = "XML";
		private static final String HTML_PARSE_STYLE = "HTML";
		
		private static final String MAPPING_ATTRIBUTE_PREFIX = "MAPPING";
		private static final String MAPPING_START_ATTRIBUTE_NAME = "START";
		private static final String MAPPING_TARGET_ATTRIBUTE_NAME = "TARGET";
		
		private StringVector paragraphTags = new StringVector();
		private StringVector ignoreTags = new StringVector();
		private Properties tagMappings = new Properties();
		
		private Grammar grammar;
		private Parser parser;
		
		HtmlXmlDocumentReader(Settings set) {
			this.grammar = ((HTML_PARSE_STYLE.equals(set.getSetting(PARSE_STYLE_ATTRIBUTE_NAME)) ? new Html() : new StandardGrammar()));
			this.parser = new Parser(this.grammar);
			
			StringVector parser = new StringVector();
			
			parser.parseAndAddElements(set.getSetting(PARAGRAPH_TAGS_ATTRIBUTE_NAME, ""), ";");
			this.paragraphTags.addContentIgnoreDuplicates(parser);
			parser.clear();
			
			parser.parseAndAddElements(set.getSetting(IGNORE_TAGS_ATTRIBUTE_NAME, ""), ";");
			this.ignoreTags.addContentIgnoreDuplicates(parser);
			parser.clear();
			
			for (int m = 0; m < set.size(); m++) {
				Settings mapping = set.getSubset(MAPPING_ATTRIBUTE_PREFIX + "_" + m);
				parser.parseAndAddElements(mapping.getSetting(MAPPING_START_ATTRIBUTE_NAME, ""), ";");
				String target = mapping.getSetting(MAPPING_TARGET_ATTRIBUTE_NAME);
				if (target != null)
					for (int s = 0; s < parser.size(); s++)
						this.tagMappings.setProperty(parser.get(s), target);
				parser.clear();
			}
		}
		
		MutableAnnotation readDocument(Reader source) throws IOException {
			try {
				DocumentRoot document = Gamta.newDocument(parent.getTokenizer());
				SgmlDocumentReader dc = new SgmlDocumentReader(document, this.grammar, this.tagMappings, this.ignoreTags, this.paragraphTags);
				this.parser.stream(source, dc);
				dc.close();
				return document;
			}
			catch (IOException pe) {
				throw new IOException("Could not parse data provided by specified Reader.");
			}
		}
	}
}
