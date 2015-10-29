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
package de.uka.ipd.idaho.goldenGate.plugin.documentWriters;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Stack;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.html.HTMLDocument;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.Token;
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
import de.uka.ipd.idaho.htmlXmlUtil.TreeNode;
import de.uka.ipd.idaho.htmlXmlUtil.accessories.TreeTools;
import de.uka.ipd.idaho.stringUtils.StringUtils;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * Document format provider managing document writer definitions (as a resource
 * manager does with resources). A document writer basically is a set of
 * annotation types to be considered on output, while the types not in the set
 * are ignored. This is useful for removing experimental or helper markup on
 * saving a document.
 * 
 * @author sautter
 */
public class DocumentWriterManager extends AbstractDocumentFormatProvider {
	
	private static final String FILE_EXTENSION = ".documentWriter";
	
	private static final String[] FILE_EXTENSIONS = {"xml", "html"};
	
	public static final String ALL_TAGS_WRITER_NAME = "<All Tags>";
	public static final String ALL_TAGS_WITH_ID_WRITER_NAME = "<All Tags, with IDs>";
	public static final String SELECTION_WRITER_NAME = "<Current Selection>";
	public static final String SELECTION_WITH_ID_WRITER_NAME = "<Current Selection, with IDs>";
	
	private static final String[] FIX_DOCUMENT_WRITER_NAMES = {ALL_TAGS_WRITER_NAME, ALL_TAGS_WITH_ID_WRITER_NAME, SELECTION_WRITER_NAME, SELECTION_WITH_ID_WRITER_NAME};
	
	private JFileChooser fileChooser = null;
	
	private FileFilter xsdFilter = new FileFilter() {
		public boolean accept(File f) {
			return ((f != null) && (f.isFile()) && f.getName().endsWith(".xsd"));
		}
		public String getDescription() {
			return "XML Schema Files";
		}
	};
	
	public DocumentWriterManager() {}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager#getFileExtension()
	 */
	protected String getFileExtension() {
		return FILE_EXTENSION;
	}
	
	/* 
	 * @see de.uka.ipd.idaho.goldenGate.plugins.GoldenGatePlugin#getPluginName()
	 */
	public String getPluginName() {
		return "Document Writer Manager";
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.ResourceManager#getResourceTypeLabel()
	 */
	public String getResourceTypeLabel() {
		return "SGML Document Writer";
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.ResourceManager#init()
	 */
	public void init() {
		try {
			this.fileChooser = new JFileChooser();
			this.fileChooser.addChoosableFileFilter(this.fileChooser.getAcceptAllFileFilter());
			this.fileChooser.addChoosableFileFilter(this.xsdFilter);
		} catch (SecurityException se) {}
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.GoldenGatePlugin#getSettingsPanel()
	 */
	public SettingsPanel getSettingsPanel() {
		return new DocumentWriterSettingsPanel();
	}
	
	private class DocumentWriterSettingsPanel extends SettingsPanel {
		private DocumentWriterEditorPanel editor;
		DocumentWriterSettingsPanel() {
			super("Document Writers", "Tag filters for writing SGML formatted documents.");
			
			this.setLayout(new BorderLayout());
			this.setDoubleBuffered(true);
			
			JPanel editButtons = new JPanel(new FlowLayout(FlowLayout.CENTER));
			JButton button;
			button = new JButton("Create");
			button.setBorder(BorderFactory.createRaisedBevelBorder());
			button.setPreferredSize(new Dimension(100, 21));
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (createDocumentWriter())
						resourceNameList.refresh();
				}
			});
			editButtons.add(button);
			button = new JButton("Clone");
			button.setBorder(BorderFactory.createRaisedBevelBorder());
			button.setPreferredSize(new Dimension(100, 21));
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (cloneDocumentWriter())
						resourceNameList.refresh();
				}
			});
			editButtons.add(button);
			button = new JButton("Load");
			button.setBorder(BorderFactory.createRaisedBevelBorder());
			button.setPreferredSize(new Dimension(100, 21));
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (loadDocumentWriter())
						resourceNameList.refresh();
				}
			});
			editButtons.add(button);
			button = new JButton("Load URL");
			button.setBorder(BorderFactory.createRaisedBevelBorder());
			button.setPreferredSize(new Dimension(100, 21));
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (loadDocumentWriterFromURL())
						resourceNameList.refresh();
				}
			});
			editButtons.add(button);
			button = new JButton("Delete");
			button.setBorder(BorderFactory.createRaisedBevelBorder());
			button.setPreferredSize(new Dimension(100, 21));
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (deleteDocumentWriter())
						resourceNameList.refresh();
				}
			});
			editButtons.add(button);
			
			final JPanel editorPanel = new JPanel(new BorderLayout());
			String selectedName = resourceNameList.getSelectedName();
			if (selectedName == null)
				editorPanel.add(getExplanationLabel(), BorderLayout.CENTER);
			else {
				StringVector documentWriter = readDocumentWriter(selectedName);
				if (documentWriter == null)
					editorPanel.add(getExplanationLabel(), BorderLayout.CENTER);
				else {
					this.editor = new DocumentWriterEditorPanel(selectedName, documentWriter);
					editorPanel.add(this.editor, BorderLayout.CENTER);
				}
			}
			
			this.add(editButtons, BorderLayout.NORTH);
			this.add(editorPanel, BorderLayout.CENTER);
			this.add(resourceNameList, BorderLayout.EAST);
			
			resourceNameList.addDataListListener(new DataListListener() {
				public void selected(String dataName) {
					if ((editor != null) && editor.isDirty()) {
						try {
							storeDocumentWriter(editor.documentWriterName, editor.getContent());
							parent.notifyResourcesChanged(DocumentWriterManager.class.getName());
						}
						catch (IOException ioe) {
							if (JOptionPane.showConfirmDialog(DocumentWriterSettingsPanel.this, (ioe.getClass().getName() + " (" + ioe.getMessage() + ")\nwhile saving file to " + editor.documentWriterName + "\nProceed?"), "Could Not Save DocumentWriter", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
								resourceNameList.setSelectedName(editor.documentWriterName);
								return;
							}
						}
					}
					
					if (dataName != null) {
						StringVector documentWriter = readDocumentWriter(dataName);
						if (documentWriter == null) documentWriter = new StringVector();
						editorPanel.removeAll();
						editor = new DocumentWriterEditorPanel(dataName, documentWriter);
						editorPanel.add(editor, BorderLayout.CENTER);
						editorPanel.validate();
					}
				}
			});
			
			this.addComponentListener(new ComponentAdapter() {
				public void componentHidden(ComponentEvent ce) {
					if ((editor != null) && editor.isDirty()) {
						try {
							storeDocumentWriter(editor.documentWriterName, editor.getContent());
							parent.notifyResourcesChanged(DocumentWriterManager.class.getName());
						}
						catch (IOException ioe) {}
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
		
		boolean createDocumentWriter() {
			return this.createDocumentWriter(null, null);
		}
		
		boolean loadDocumentWriter() {
			if (fileChooser == null) try {
				fileChooser = new JFileChooser();
			} catch (SecurityException se) {}
			
			if ((fileChooser != null) && (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)) {
				File file = fileChooser.getSelectedFile();
				if ((file != null) && file.isFile()) {
					try {
						String fileName = file.toString();
						fileName = fileName.substring(fileName.lastIndexOf(File.separatorChar) + 1);
						FileFilter ff = fileChooser.getFileFilter();
						return this.loadDocumentWriter(new FileInputStream(file), ((ff == xsdFilter) ? "xsd" : null), fileName);
					}
					catch (IOException ioe) {
						JOptionPane.showMessageDialog(this, (ioe.getClass().getName() + " (" + ioe.getMessage() + ")\nwhile trying to load " + file.toString()), "Could Not Read File", JOptionPane.ERROR_MESSAGE);
					}
				}
			}
			return false;
		}
		
		boolean loadDocumentWriterFromURL() {
			if (!dataProvider.allowWebAccess() && (JOptionPane.showConfirmDialog(this, "You are working in offline mode, allow loading document writer from URL anyway?", "Allow Web Access", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) != JOptionPane.YES_OPTION))
				return false;
			
			Object o = JOptionPane.showInputDialog(this, "Please enter URL to load", "Enter URL", JOptionPane.QUESTION_MESSAGE, null, null, "http://");
			if ((o != null) && (o instanceof String)) {
				try {
					String urlString = o.toString();
					URL url = new URL(urlString);
					String fileName = StringUtils.replaceAll(url.getHost() + url.getPath(), "/", "_");
					return this.loadDocumentWriter(url.openStream(), (urlString.toLowerCase().endsWith(".xsd") ? "xsd" : null), fileName);
				}
				catch (IOException ioe) {
					JOptionPane.showMessageDialog(this, (ioe.getClass().getName() + " (" + ioe.getMessage() + ")\nwhile trying to load " + o.toString()), "Could Not Load URL", JOptionPane.ERROR_MESSAGE);
				}
			}
			return false;
		}
		
		boolean loadDocumentWriter(InputStream source, String sourceType, String writerName) {
			StringVector documentWriter = new StringVector();
			if ("xsd".equals(sourceType)) {
				try {
					Parser parser = new Parser();
					TreeNode root = parser.parse(source);
					TreeNode[] nodes = TreeTools.treeToBreadthFirstOrder(root);
					for (int n = 0; n < nodes.length; n++) {
						TreeNode node = nodes[n];
						if (node.hasAttribute("name")) {
							String type = node.getNodeType();
							String name = node.getAttribute("name");
							if ("xs:element".equals(type) || "element".equals(type)) documentWriter.addElementIgnoreDuplicates(name);
							if ("xs:simpleType".equals(type) || "simpleType".equals(type)) documentWriter.addElementIgnoreDuplicates(name);
							if ("xs:complexType".equals(type) || "complexType".equals(type)) documentWriter.addElementIgnoreDuplicates(name);
							if ("xs:group".equals(type) || "group".equals(type)) documentWriter.addElementIgnoreDuplicates(name);
							if ("xs:key".equals(type) || "key".equals(type)) documentWriter.addElementIgnoreDuplicates(name);
							if ("xs:keyref".equals(type) || "keyref".equals(type)) documentWriter.addElementIgnoreDuplicates(name);
							if ("xs:unique".equals(type) || "unique".equals(type)) documentWriter.addElementIgnoreDuplicates(name);
						}
					}
				}
				catch (IOException pe) {
					if (source != null) documentWriter = readDocumentWriter(source);
				}
			} else if (source != null) documentWriter = readDocumentWriter(source);
			return this.createDocumentWriter(writerName, documentWriter);
		}
		
		boolean cloneDocumentWriter() {
			String selectedName = resourceNameList.getSelectedName();
			if (selectedName == null)
				return this.createDocumentWriter();
			else {
				String name = "New " + selectedName;
				StringVector content = ((this.editor == null) ? readDocumentWriter(selectedName) : this.editor.getContent());
				return this.createDocumentWriter(name, content);
			}
		}
		
		boolean createDocumentWriter(String name, StringVector documentWriter) {
			CreateDocumentWriterDialog cld = new CreateDocumentWriterDialog(name, documentWriter);
			cld.setVisible(true);
			
			if (cld.isCommitted()) {
				StringVector newDocumentWriter = cld.getDocumentWriter();
				String documentWriterName = cld.getDocumentWriterName();
				if (!documentWriterName.endsWith(FILE_EXTENSION)) documentWriterName += FILE_EXTENSION;
				try {
					if (storeDocumentWriter(documentWriterName, newDocumentWriter)) {
						parent.notifyResourcesChanged(DocumentWriterManager.class.getName());
						return true;
					}
				} catch (IOException e) {}
			}
			return false;
		}
		
		boolean deleteDocumentWriter() {
			String name = resourceNameList.getSelectedName();
			if ((name != null) && (JOptionPane.showConfirmDialog(this, ("Really delete " + name), "Confirm Delete DocumentWriter", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION)) {
				try {
					if (dataProvider.deleteData(name)) {
						resourceNameList.refresh();
						parent.notifyResourcesChanged(DocumentWriterManager.class.getName());
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
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormatProvider#getFileExtensions()
	 */
	public String[] getFileExtensions() {
		return FILE_EXTENSIONS;
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormatProvider#getFormatForFileExtension(java.lang.String)
	 */
	public DocumentFormat getFormatForFileExtension(String fileExtension) {
		return null;
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormatProvider#getLoadFormatNames()
	 */
	public String[] getLoadFormatNames() {
		return new String[0];
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormatProvider#getSaveFormatNames()
	 */
	public String[] getSaveFormatNames() {
		StringVector documentWriters = new StringVector();
		documentWriters.addContentIgnoreDuplicates(FIX_DOCUMENT_WRITER_NAMES);
		documentWriters.addContentIgnoreDuplicates(this.resourceNameList.getNames());
		return documentWriters.toStringArray();
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormatProvider#getFormatForName(java.lang.String)
	 */
	public DocumentFormat getFormatForName(String formatName) {
		if (ALL_TAGS_WRITER_NAME.equals(formatName))
			return new AllTagsDocumentFormat(false);
		else if (ALL_TAGS_WITH_ID_WRITER_NAME.equals(formatName))
			return new AllTagsDocumentFormat(true);
		else if (SELECTION_WRITER_NAME.equals(formatName))
			return new SelectedTagsDocumentFormat(false);
		else if (SELECTION_WITH_ID_WRITER_NAME.equals(formatName))
			return new SelectedTagsDocumentFormat(true);
		Set writer = this.getDocumentWriter(formatName);
		if (writer != null)
			return new FixedWriterDocumentFormat(formatName, writer);
		else return null;
	}
	
//	/* (non-Javadoc)
//	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractDocumentFormatProvider#getDataNamesForResource(java.lang.String)
//	 */
//	public String[] getDataNamesForResource(String name) {
//		String[] names = {name + "@" + this.getClass().getName()};
//		return names;
//	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormatProvider#getLoadFileFilters()
	 */
	public DocumentFormat[] getLoadFileFilters() {
		return new DocumentFormat[0];
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormatProvider#getSaveFileFilters()
	 */
	public DocumentFormat[] getSaveFileFilters() {
		DocumentFormat[] formats = {new AllTagsDocumentFormat(false), new AllTagsDocumentFormat(true), new SelectedTagsDocumentFormat(false), new SelectedTagsDocumentFormat(true), new DocWriterDocumentFormat()};
		return formats;
	}
	
	private class AllTagsDocumentFormat extends DocumentFormat {
		private boolean writeAnnotationIDs = false;
		AllTagsDocumentFormat(boolean writeAnnotationIDs) {
			this.writeAnnotationIDs = writeAnnotationIDs;
		}
		
		public String getDefaultSaveFileExtension() {
			return "xml";
		}
		
		public MutableAnnotation loadDocument(Reader source) throws IOException {
			return null;
		}
		
		public boolean isExportFormat() {
			return false;
		}
		
		public boolean saveDocument(QueriableAnnotation data, Writer out) throws IOException {
			return this.saveDocument(null, data, out);
		}
		
		public boolean saveDocument(DocumentEditor data, Writer out) throws IOException {
			return this.saveDocument(data, data.getContent(), out);
		}
		
		public boolean saveDocument(DocumentEditor data, QueriableAnnotation doc, Writer out) throws IOException {
			DocumentWriterManager.writeDocument(doc, out, null, this.writeAnnotationIDs);
			out.flush();
			return true;
		}
		
		public boolean accept(String fileName) {
			return fileName.toLowerCase().endsWith(".xml");
		}
		
		public String getDescription() {
			return (this.writeAnnotationIDs ? "XML file (all tags, with IDs)" : "XML file (all tags)");
		}
		
		public boolean equals(DocumentFormat format) {
			return ((format != null) && (format instanceof AllTagsDocumentFormat) && (this.writeAnnotationIDs == ((AllTagsDocumentFormat) format).writeAnnotationIDs));
		}
		
		public String getName() {
			return (this.writeAnnotationIDs ? ALL_TAGS_WITH_ID_WRITER_NAME : ALL_TAGS_WRITER_NAME);
		}
		
		public String getProviderClassName() {
			return DocumentWriterManager.class.getName();
		}
	}

	private class SelectedTagsDocumentFormat extends DocumentFormat {
		private boolean writeAnnotationIDs = false;
		SelectedTagsDocumentFormat(boolean writeAnnotationIDs) {
			this.writeAnnotationIDs = writeAnnotationIDs;
		}
		
		public String getDefaultSaveFileExtension() {
			return "xml";
		}
		
		public boolean isExportFormat() {
			return true;
		}
		
		public MutableAnnotation loadDocument(Reader source) throws IOException {
			return null;
		}
		
		public boolean saveDocument(QueriableAnnotation data, Writer out) throws IOException {
			return this.saveDocument(null, data, out);
		}
		
		public boolean saveDocument(DocumentEditor data, Writer out) throws IOException {
			return this.saveDocument(data, data.getContent(), out);
		}
		
		public boolean saveDocument(DocumentEditor data, QueriableAnnotation doc, Writer out) throws IOException {
			Set docWriter = ((data == null) ? null : data.getSelectedTags());
			if (docWriter != null)
				docWriter.add("br");
			DocumentWriterManager.writeDocument(doc, out, docWriter, this.writeAnnotationIDs);
			out.flush();
			return true;
		}
		
		public boolean accept(String fileName) {
			return fileName.toLowerCase().endsWith(".xml");
		}
		
		public String getDescription() {
			return (this.writeAnnotationIDs ? "XML file (selected tags, with IDs)" : "XML file (selected tags)");
		}
		
		public boolean equals(DocumentFormat format) {
			return ((format != null) && (format instanceof SelectedTagsDocumentFormat) && (this.writeAnnotationIDs == ((SelectedTagsDocumentFormat) format).writeAnnotationIDs));
		}
		
		public String getName() {
			return (this.writeAnnotationIDs ? SELECTION_WITH_ID_WRITER_NAME : SELECTION_WRITER_NAME);
		}
		
		public String getProviderClassName() {
			return DocumentWriterManager.class.getName();
		}
	}

	private class DocWriterDocumentFormat extends DocumentFormat {
		
		private String name;
		private Set docWriter = null;
		
		DocWriterDocumentFormat() {}
		
		public String getDefaultSaveFileExtension() {
			return "xml";
		}
		
		public boolean isExportFormat() {
			return true;
		}
		
		public MutableAnnotation loadDocument(Reader source) throws IOException {
			return null;
		}
		
		public boolean saveDocument(QueriableAnnotation data, Writer out) throws IOException {
			return this.saveDocument(null, data, out);
		}
		
		public boolean saveDocument(DocumentEditor data, Writer out) throws IOException {
			return this.saveDocument(data, data.getContent(), out);
		}
		
		public boolean saveDocument(DocumentEditor data, QueriableAnnotation doc, Writer out) throws IOException {
			if (this.docWriter == null) {
				ResourceDialog rd = ResourceDialog.getResourceDialog(getSaveFormatNames(), "Select Document Writer", "Select");
				rd.setVisible(true);
				if (rd.isCommitted()) {
					this.name = rd.getSelectedResourceName();
					this.docWriter = getDocumentWriter(this.name);
				}
			}
			DocumentWriterManager.writeDocument(doc, out, this.docWriter, false);
			out.flush();
			return true;
		}
		
		public boolean accept(String fileName) {
			return fileName.toLowerCase().endsWith(".xml");
		}
		
		public String getDescription() {
			return "XML file (custom writer)";
		}
		
		public boolean equals(DocumentFormat format) {
			if (format == null)
				return false;
			if (!(format instanceof DocWriterDocumentFormat))
				return false;
			DocWriterDocumentFormat dwdf = ((DocWriterDocumentFormat) format);
			if (this.name == null)
				return true; // allow an undetermined instance to be pre-selected if the previous selection was another instance that has been determined in the meantime 
			else return this.name.equals(dwdf.name);
		}
		
		public String getName() {
			return this.name;
		}
		
		public String getProviderClassName() {
			return DocumentWriterManager.class.getName();
		}
	}

	private class FixedWriterDocumentFormat extends DocumentFormat {
		
		private String name;
		private Set docWriter = null;
		
		FixedWriterDocumentFormat(String name, Set docWriter) {
			this.name = name;
			this.docWriter = docWriter;
		}
		
		public String getDefaultSaveFileExtension() {
			return "xml";
		}
		
		public boolean isExportFormat() {
			return true;
		}
		
		public MutableAnnotation loadDocument(Reader source) throws IOException {
			return null;
		}
		
		public boolean saveDocument(QueriableAnnotation data, Writer out) throws IOException {
			return this.saveDocument(null, data, out);
		}
		
		public boolean saveDocument(DocumentEditor data, Writer out) throws IOException {
			return this.saveDocument(data, data.getContent(), out);
		}
		
		public boolean saveDocument(DocumentEditor data, QueriableAnnotation doc, Writer out) throws IOException {
			DocumentWriterManager.writeDocument(doc, out, this.docWriter, false);
			out.flush();
			return true;
		}
		
		public boolean accept(String fileName) {
			return fileName.toLowerCase().endsWith(".xml");
		}
		
		public String getDescription() {
			return "XML file (" + this.name + ")";
		}
		
		public boolean equals(DocumentFormat format) {
			if (format == null)
				return false;
			if (!(format instanceof FixedWriterDocumentFormat))
				return false;
			FixedWriterDocumentFormat fwdf = ((FixedWriterDocumentFormat) format);
			if (this.name == null)
				return (fwdf.name == null);
			else return this.name.equals(fwdf.name);
		}
		
		public String getName() {
			return this.name;
		}
		
		public String getProviderClassName() {
			return DocumentWriterManager.class.getName();
		}
	}
	
	/* retrieve a plain documentWriter by its name
	 * @param	name	the name of the required documentWriter
	 * @return the documentWriter with the required name, or null, if there is no such documentWriter
	 */
	private Set getDocumentWriter(String name) {
		if (name == null) return null;
		return new TagFilterSet(this.readDocumentWriter(name));
	}
	
	/*
	 * regular expression based set, containing all matches of contained regular expressions
	 * 
	 * @author sautter
	 */
	private class TagFilterSet implements Set {
		
		private static final String TAG_CHAR = "[a-zA-Z0-9\\\\\\_\\\\\\.\\\\\\-]"; 
		private static final String TAG_REGEX = "([a-zA-Z0-9\\_\\.\\-]++\\:)?[a-zA-Z0-9\\_\\.\\-]++";
		
		private StringVector tags = null;
		
		private Set cacheSet = new HashSet();
		private Set negCacheSet = new HashSet();
		
		TagFilterSet(StringVector tags) {
			if (tags != null) {
				this.tags = new StringVector();
				
				//	convert regular expressions
				for (int t = 0; t < tags.size(); t++) {
					String tag = tags.get(t);
					
					//	simple tag
					if (tag.matches(TAG_REGEX)) {
						System.out.println("Simple tag: " + tag);
						this.tags.addElementIgnoreDuplicates(tag);
						
					//	simple pattern
					} else if (tag.replaceAll("[\\*\\+\\?\\#]", "_").matches(TAG_REGEX)) {
						System.out.println("Simple tag expression: " + tag);
						
						//	optimize
						int l = tag.length() + 1;
						while (l > tag.length()) {
							l = tag.length();
							
							//	remove ? next to *
							tag = tag.replaceAll("[\\?][\\*]", "*");
							tag = tag.replaceAll("[\\*][\\?]", "*");
							
							//	convert + next to * into #
							tag = tag.replaceAll("[\\+][\\*]", "#");
							tag = tag.replaceAll("[\\*][\\+]", "#");
							
							//	remove * next to #
							tag = tag.replaceAll("[\\*][\\#]", "#");
							tag = tag.replaceAll("[\\#][\\*]", "#");
							
							//	remove * next to *
							tag = tag.replaceAll("[\\*][\\*]", "*");
							
							//	remove # next to #
							tag = tag.replaceAll("[\\#][\\#]", "#");
						}
						System.out.println("Optimized simple tag expression: " + tag);
						
						//	convert to JAVA pattern
						//	convert ? (none or one)
						tag = tag.replaceAll("[\\?]", (TAG_CHAR + "?"));
						
						//	convert * (none or more)
						tag = tag.replaceAll("[\\*]", (TAG_CHAR + "*"));
						
						//	convert + (exactly one)
						tag = tag.replaceAll("[\\+]", TAG_CHAR);
						
						//	convert # (one or more)
						tag = tag.replaceAll("[\\#]", (TAG_CHAR + "+"));
						
						System.out.println("Converted simple tag expression: " + tag);
						this.tags.addElementIgnoreDuplicates(tag);
						
					//	JAVA pattern
					} else {
						System.out.println("JAVA tag expression: " + tag);
						this.tags.addElementIgnoreDuplicates(tag);
					}
				}
			}
		}
		
		public boolean add(Object o) {
			return false;
		}
		
		public boolean addAll(Collection c) {
			return false;
		}
		
		public void clear() {}
		
		public boolean contains(Object o) {
			
			//	check arguments
			if (o == null) return false;
			if (!(o instanceof String)) return false;
			if (this.tags == null) return true;
			
			//	do cache lookup
			if (this.cacheSet.contains(o)) return true;
			if (this.negCacheSet.contains(o)) return false;
			
			//	do list lookup
			String s = o.toString();
			for (int t = 0; t < this.tags.size(); t++) {
				try {
					//	try regular expression match
					if (s.matches(this.tags.get(t))) {
						this.cacheSet.add(s);
						return true;
					}
				} catch (Exception e) {
					//	try plain comparison
					if (s.equalsIgnoreCase(this.tags.get(t))) {
						this.cacheSet.add(s);
						return true;
					}
				}
			}
			
			//	remember tag which is not contained
			this.negCacheSet.add(s);
			return false;
		}
		
		public boolean containsAll(Collection c) {
			if (c == null) return true;
			Iterator i = c.iterator();
			while (i.hasNext())
				if (!this.contains(i.next())) return false;
			return true;
		}
		
		public boolean isEmpty() {
			return ((this.tags == null) ? true : (this.tags.size() == 0));
		}
		
		public Iterator iterator() {
			return null;
		}
		
		public boolean remove(Object o) {
			return false;
		}
		
		public boolean removeAll(Collection c) {
			return false;
		}
		
		public boolean retainAll(Collection c) {
			return false;
		}
		
		public int size() {
			return ((this.tags == null) ? 0 : this.tags.size());
		}
		
		public Object[] toArray(Object[] t) {
			return null;
		}
		
		public Object[] toArray() {
			return ((this.tags == null) ? new String[0] : this.tags.toStringArray());
		}
	}
	
	private StringVector readDocumentWriter(String name) {
		try {
			InputStream is = this.dataProvider.getInputStream(name);
			StringVector sv = this.readDocumentWriter(is);
			is.close();
			return sv;
		}
		catch (IOException ioe) {
			return null;
		}
	}
	
	private StringVector readDocumentWriter(InputStream source) {
		try {
			InputStreamReader reader = new InputStreamReader(source);
			StringVector documentWriter = StringVector.loadList(reader);
			reader.close();
			return documentWriter;
		}
		catch (IOException ioe) {
			return null;
		}
	}
	
	private boolean storeDocumentWriter(String name, StringVector documentWriter) throws IOException {
		if (this.dataProvider.isDataEditable(name)) {
			OutputStream os = this.dataProvider.getOutputStream(name);
			documentWriter.storeContent(os);
			os.flush();
			os.close();
			return true;
		}
		else return false;
	}
	
	private static void writeDocument(QueriableAnnotation data, Writer output, Set annotationTypes, boolean writeAnnotationIDs) throws IOException {
		BufferedWriter buf = new BufferedWriter(output);
		
		//	write XML declaration (output stream will correct encoding name on the fly if it's not UTF-8)
		buf.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
		buf.newLine();
		
		//	get Annotations
		Annotation[] nestedAnnotations = data.getAnnotations();
		
		//	filter annotations
		if (annotationTypes != null) {
			ArrayList annotationList = new ArrayList();
			for (int a = 0; a < nestedAnnotations.length; a++)
				if (annotationTypes.contains(nestedAnnotations[a].getType()))
					annotationList.add(nestedAnnotations[a]);
			nestedAnnotations = ((Annotation[]) annotationList.toArray(new Annotation[annotationList.size()]));
		}
		
		//	make sure there is a root element
		if ((nestedAnnotations.length == 0) || (nestedAnnotations[0].size() < data.size())) {
			Annotation[] newNestedAnnotations = new Annotation[nestedAnnotations.length + 1];
			newNestedAnnotations[0] = data;
			System.arraycopy(nestedAnnotations, 0, newNestedAnnotations, 1, nestedAnnotations.length);
			nestedAnnotations = newNestedAnnotations;
		}
		
		Stack stack = new Stack();
		int annotationPointer = 0;
		
		Token token = null;
		Token lastToken;
		
		boolean lastWasTag = false;
		boolean lastWasLineBreak = true;
		
		HashSet lineBroken = new HashSet();
		
		for (int t = 0; t < data.size(); t++) {
			
			//	switch to next Token
			lastToken = token;
			token = data.tokenAt(t);
			
			//	write end tags for Annotations ending before current Token
			while ((stack.size() > 0) && ((((Annotation) stack.peek()).getStartIndex() + ((Annotation) stack.peek()).size()) <= t)) {
				Annotation annotation = ((Annotation) stack.pop());
				
				//	line break only if nested Annotations
				if (!lastWasLineBreak && lineBroken.contains(annotation.getAnnotationID())) 
					buf.newLine();
				
				//	write tag and line break
				buf.write(AnnotationUtils.produceEndTag(annotation));
				lastWasTag = true;
				buf.newLine();
				lastWasLineBreak = true;
			}
			
			//	add line break if required
			if (!lastWasLineBreak && (lastToken != null) && lastToken.hasAttribute(Token.PARAGRAPH_END_ATTRIBUTE)) {
				if ((annotationTypes == null) || annotationTypes.contains("br")) buf.write("<br/>");
				buf.newLine();
				lastWasLineBreak = true;
			}
			
			//	skip space character before unspaced punctuation (e.g. ','), after line breaks and tags, and if there is no whitespace in the token sequence
			if (!lastWasTag && !lastWasLineBreak && 
				(lastToken != null) && !lastToken.hasAttribute(Token.PARAGRAPH_END_ATTRIBUTE) && 
				 Gamta.insertSpace(lastToken, token) && (t != 0) && (data.getWhitespaceAfter(t-1).length() != 0)
				 ) buf.write(" ");
			
			//	write start tags for Annotations beginning at actual Token
			while ((annotationPointer < nestedAnnotations.length) && (nestedAnnotations[annotationPointer].getStartIndex() == t)) {
				Annotation annotation = nestedAnnotations[annotationPointer];
				stack.push(nestedAnnotations[annotationPointer]);
				annotationPointer++;
				
				//	line break
				if (!lastWasLineBreak) buf.newLine();
				
				//	add start tag
				buf.write(AnnotationUtils.produceStartTag(annotation, writeAnnotationIDs));
				lastWasTag = true;
				lastWasLineBreak = false;
				
				//	line break only if nested Annotations
				if ((annotationPointer < nestedAnnotations.length) && AnnotationUtils.contains(annotation, nestedAnnotations[annotationPointer])) {
					buf.newLine();
					lastWasLineBreak = true;
					lineBroken.add(annotation.getAnnotationID());
				}
			}
			
			//	append current Token
			String tokenValue = token.getValue();
			buf.write(AnnotationUtils.escapeForXml(tokenValue));
			lastWasTag = false;
			lastWasLineBreak = false;
		}
		
		//	write end tags for Annotations not closed so far
		while (stack.size() > 0) {
			Annotation annotation = ((Annotation) stack.pop());
			if (!lastWasLineBreak)
				buf.newLine();
			buf.write("</" + ((annotation.getType() == null) ? "annotation" : annotation.getType()) + ">");
			lastWasLineBreak = false;
		}
		buf.flush();
	}
	
	private class CreateDocumentWriterDialog extends DialogPanel {
		
		private JTextField nameField;
		
		private DocumentWriterEditorPanel editor;
		private StringVector documentWriter = null;
		private String documentWriterName = null;
		
		CreateDocumentWriterDialog(String name, StringVector documentWriter) {
			super("Create DocumentWriter", true);
			
			this.nameField = new JTextField((name == null) ? "New DocumentWriter" : name);
			this.nameField.setBorder(BorderFactory.createLoweredBevelBorder());
			
			//	initialize main buttons
			JButton commitButton = new JButton("Create");
			commitButton.setBorder(BorderFactory.createRaisedBevelBorder());
			commitButton.setPreferredSize(new Dimension(100, 21));
			commitButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					commit();
				}
			});
			
			JButton abortButton = new JButton("Cancel");
			abortButton.setBorder(BorderFactory.createRaisedBevelBorder());
			abortButton.setPreferredSize(new Dimension(100, 21));
			abortButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					abort();
				}
			});
			
			JPanel mainButtonPanel = new JPanel();
			mainButtonPanel.setLayout(new FlowLayout());
			mainButtonPanel.add(commitButton);
			mainButtonPanel.add(abortButton);
			
			//	initialize editor
			this.editor = new DocumentWriterEditorPanel(name, documentWriter);
			
			//	put the whole stuff together
			this.add(this.nameField, BorderLayout.NORTH);
			this.add(this.editor, BorderLayout.CENTER);
			this.add(mainButtonPanel, BorderLayout.SOUTH);
			
			this.setResizable(true);
			this.setSize(new Dimension(600, 400));
			this.setLocationRelativeTo(this.getOwner());
		}
		
		boolean isCommitted() {
			return (this.documentWriterName != null);
		}
		
		StringVector getDocumentWriter() {
			return this.documentWriter;
		}
		
		String getDocumentWriterName() {
			return this.documentWriterName;
		}
		
		void abort() {
			this.documentWriter = null;
			this.documentWriterName = null;
			this.dispose();
		}
		
		void commit() {
			this.documentWriter = this.editor.getContent();
			this.documentWriterName = this.nameField.getText();
			this.dispose();
		}
	}
	
	private class DocumentWriterEditorPanel extends JPanel implements FontEditable, DocumentListener {
		
		private JTextArea editor;
		private JScrollPane editorBox;
		
		private StringVector content = new StringVector();
		
		private String fontName = "Verdana";
		private int fontSize = 12;
		private Color fontColor = Color.BLACK;
		
		private boolean dirty = false;
		private String documentWriterName;
		
		DocumentWriterEditorPanel(String name, StringVector dw) {
			super(new BorderLayout(), true);
			this.documentWriterName = name;
			
			//	initialize editor
			this.editor = new JTextArea();
			this.editor.setEditable(true);
			
			//	wrap editor in scroll pane
			this.editorBox = new JScrollPane(this.editor);
			
			//	put the whole stuff together
			this.add(this.editorBox, BorderLayout.CENTER);
			this.setContent((dw == null) ? new StringVector() : dw);
		}
		
		StringVector getContent() {
			if (this.isDirty()) {
				this.content.clear();
				this.content.parseAndAddElements(this.editor.getText(), "\n");
			}
			return this.content;
		}
		
		void setContent(StringVector documentWriter) {
			this.content = documentWriter;
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
