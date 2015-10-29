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
package de.uka.ipd.idaho.goldenGate.plugin.documentFormats;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Properties;
import java.util.Stack;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import de.uka.ipd.idaho.gamta.DocumentRoot;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.Token;
import de.uka.ipd.idaho.gamta.TokenSequence;
import de.uka.ipd.idaho.gamta.Tokenizer;
import de.uka.ipd.idaho.gamta.util.constants.TableConstants;
import de.uka.ipd.idaho.goldenGate.DocumentEditor;
import de.uka.ipd.idaho.goldenGate.plugins.AbstractDocumentFormatProvider;
import de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat;
import de.uka.ipd.idaho.goldenGate.plugins.SettingsPanel;
import de.uka.ipd.idaho.htmlXmlUtil.Parser;
import de.uka.ipd.idaho.htmlXmlUtil.TokenReceiver;
import de.uka.ipd.idaho.htmlXmlUtil.TreeNodeAttributeSet;
import de.uka.ipd.idaho.htmlXmlUtil.accessories.IoTools;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.Grammar;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.Html;
import de.uka.ipd.idaho.stringUtils.StringUtils;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * This plugin provides a document format that specially deals with empty
 * elements in HTML documents, especially empty table cells. Otherwise similar
 * to an appropriately configures SgmlDocumentReader.
 * 
 * @author sautter
 */
public class HtmlDocumentFormatter extends AbstractDocumentFormatProvider implements TableConstants {

	private static final String[] FILE_EXTENSIONS = {"html", "htm"};
	private static final String HTML_FORMAT_NAME = "<HTML Document Format>";
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#init()
	 */
	public void init() {
		
		//	load config file
		try {
			StringVector config = StringVector.loadList(new InputStreamReader(this.dataProvider.getInputStream("config.cnfg"), "UTF-8"));
			for (int c = 0; c < config.size(); c++) {
				String line = config.get(c).trim();
				if (line.startsWith("//"))
					continue;
				
				//	mapping
				if (line.startsWith("M:")) {
					line = line.substring("M:".length()).trim();
					String[] lineParts = line.split("\\s+");
					if (lineParts.length == 2)
						this.tagMapping.setProperty(lineParts[0], lineParts[1]);
				}
				
				//	paragraph tag
				else if (line.startsWith("P:")) {
					line = line.substring("P:".length()).trim();
					this.paragraphTags.addElementIgnoreDuplicates(line);
				}
				
				//	ignore tag
				else if (line.startsWith("I:")) {
					line = line.substring("I:".length()).trim();
					this.ignoreTags.addElementIgnoreDuplicates(line);
				}
				
				//	ignore content tag
				else if (line.startsWith("C:")) {
					line = line.substring("C:".length()).trim();
					this.ignoreContentTags.addElementIgnoreDuplicates(line);
				}
			}
		}
		
		//	fall back to defaults
		catch (IOException ioe) {
			this.tagMapping.setProperty("p", MutableAnnotation.PARAGRAPH_TYPE);
			this.tagMapping.setProperty("h1", MutableAnnotation.PARAGRAPH_TYPE);
			this.tagMapping.setProperty("h2", MutableAnnotation.PARAGRAPH_TYPE);
			this.tagMapping.setProperty("h3", MutableAnnotation.PARAGRAPH_TYPE);
			this.tagMapping.setProperty("h4", MutableAnnotation.PARAGRAPH_TYPE);
			this.tagMapping.setProperty("h5", MutableAnnotation.PARAGRAPH_TYPE);
			this.tagMapping.setProperty("h6", MutableAnnotation.PARAGRAPH_TYPE);
			
			this.paragraphTags.addElementIgnoreDuplicates(MutableAnnotation.SECTION_TYPE);
			this.paragraphTags.addElementIgnoreDuplicates(MutableAnnotation.SUB_SECTION_TYPE);
			this.paragraphTags.addElementIgnoreDuplicates(MutableAnnotation.SUB_SUB_SECTION_TYPE);
			this.paragraphTags.addElementIgnoreDuplicates(MutableAnnotation.PARAGRAPH_TYPE);
			this.paragraphTags.parseAndAddElements("tr;p;br;hr", ";");
			
			this.ignoreTags.addElementIgnoreDuplicates(DocumentRoot.DOCUMENT_TYPE);
			this.ignoreTags.parseAndAddElements("b;i;u;a;span;font;html;body", ";");
			
			this.ignoreContentTags.parseAndAddElements("script;style;head", ";");
		}
	}
	
	/* 
	 * @see de.uka.ipd.idaho.goldenGate.plugins.GoldenGatePlugin#getPluginName()
	 */
	public String getPluginName() {
		return "HTML Document Format";
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
		String extension = fileExtension.toLowerCase();
		if (extension.startsWith("."))
			extension = extension.substring(1);
		if ("html".equalsIgnoreCase(extension) || "htm".equalsIgnoreCase(extension))
			return new HtmlDocumentFormat(this.parent.getTokenizer());
		else return null;
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormatProvider#getLoadFormatNames()
	 */
	public String[] getLoadFormatNames() {
		String[] formatNames = {HTML_FORMAT_NAME};
		return formatNames;
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormatProvider#getSaveFormatNames()
	 */
	public String[] getSaveFormatNames() {
		String[] formatNames = {};
		return formatNames;
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormatProvider#getFormatForName(java.lang.String)
	 */
	public DocumentFormat getFormatForName(String formatName) {
		if (HTML_FORMAT_NAME.equalsIgnoreCase(formatName))
			return new HtmlDocumentFormat(this.parent.getTokenizer());
		else return null;
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormatProvider#getLoadFileFilters()
	 */
	public DocumentFormat[] getLoadFileFilters() {
		DocumentFormat[] formats = {new HtmlDocumentFormat(this.parent.getTokenizer())};
		return formats;
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormatProvider#getSaveFileFilters()
	 */
	public DocumentFormat[] getSaveFileFilters() {
		DocumentFormat[] formats = {};
		return formats;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractDocumentFormatProvider#getDataNamesForResource(java.lang.String)
	 */
	public String[] getDataNamesForResource(String name) {
		String[] dataNames = {"config.cnfg"};
		return dataNames;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#getSettingsPanel()
	 */
	public SettingsPanel getSettingsPanel() {
		return new HtmlDocumentFormatSettingsPanel();
	}
	
	private class HtmlDocumentFormatSettingsPanel extends SettingsPanel {
		private TagListEditorPanel paragraphTagListEditor = new TagListEditorPanel();
		private TagListEditorPanel ignoreTagListEditor = new TagListEditorPanel();
		private TagListEditorPanel ignoreContentTagListEditor = new TagListEditorPanel();
		
		private JTable tagMappingTable = new JTable();
		
		private boolean dirty = false;
		HtmlDocumentFormatSettingsPanel() {
			super("HTML Document Format", "Document format for loading HTML and XHTML documents");
			this.setLayout(new BorderLayout());
			
			this.paragraphTagListEditor.setContent(paragraphTags);
			this.ignoreTagListEditor.setContent(ignoreTags);
			this.ignoreContentTagListEditor.setContent(ignoreContentTags);
			this.tagMappingTable.setModel(new TagMappingTableModel(tagMapping));
			
			JTabbedPane tabs = new JTabbedPane();
			tabs.addTab("Paragraph Tags", null, this.paragraphTagListEditor, "List of tags at the end of which the document should have a line break");
			tabs.addTab("Ignore Tags", null, this.ignoreTagListEditor, "List of tags to ignore, i.e. not to represent as annotations");
			tabs.addTab("Ignore Content Tags", null, this.ignoreContentTagListEditor, "List of tags to ignore including their content, i.e. whose content not to add to the document at all");
			
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
					String tag = JOptionPane.showInputDialog(HtmlDocumentFormatSettingsPanel.this, "Enter the tag to map. The mapping can be changed in the table.", "Add Tag Mapping", JOptionPane.PLAIN_MESSAGE);
					if (tag == null)
						return;
					if (!tagMapping.contains(tag) || (JOptionPane.showConfirmDialog(HtmlDocumentFormatSettingsPanel.this, ("The specified tag is already mapped to " + tagMapping.getProperty(tag, "") + ". Change the mapping?"), "Tag Already Mapped", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION)) {
						tagMapping.setProperty(tag, tag);
						TagMappingTableModel model = new TagMappingTableModel(tagMapping);
						tagMappingTable.setModel(model);
						tagMappingTable.setColumnSelectionAllowed(false);
						tagMappingTable.setRowSelectionAllowed(true);
						dirty = true;
						validate();
						int index = Arrays.binarySearch(model.tags, tag);
						tagMappingTable.setRowSelectionInterval(index, index);
						tagMappingTable.editCellAt(index, 1);
					}
				}
			});
			buttonPanel.add(button);
			button = new JButton("Remove Mapping");
			button.setBorder(BorderFactory.createRaisedBevelBorder());
			button.setSize(new Dimension(120, 21));
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					int row = tagMappingTable.getSelectedRow();
					if ((row > -1) && (row < tagMappingTable.getRowCount())) {
						tagMapping.remove(tagMappingTable.getModel().getValueAt(row, 0));
						tagMappingTable.setModel(new TagMappingTableModel(tagMapping));
						tagMappingTable.setColumnSelectionAllowed(false);
						tagMappingTable.setRowSelectionAllowed(true);
						dirty = true;
						validate();
					}
				}
			});
			buttonPanel.add(button);
			tagMappingEditor.add(buttonPanel, BorderLayout.SOUTH);
			
			tabs.addTab("Tag Mappings", null, tagMappingEditor, "Mapping of tags");
			this.add(tabs, BorderLayout.CENTER);
		}
		
		void notifyMappingChanged() {
			this.dirty = true;
		}
		
		boolean isDirty() {
			return (this.dirty || this.paragraphTagListEditor.isDirty() || this.ignoreTagListEditor.isDirty() || this.ignoreContentTagListEditor.isDirty());
		}
		
		private class TagMappingTableModel implements TableModel {
			Properties mappings;
			String[] tags;
			TagMappingTableModel(Properties mappings) {
				this.mappings = mappings;
				this.tags = ((String[]) this.mappings.keySet().toArray(new String[this.mappings.size()]));
				Arrays.sort(this.tags);
			}
			public Class getColumnClass(int columnIndex) {
				return String.class;
			}
			public int getColumnCount() {
				return 2;
			}
			public String getColumnName(int columnIndex) {
				if (columnIndex == 0)
					return "Mapped Tag";
				else if (columnIndex == 1)
					return "Mapping Target";
				else return null;
			}
			public int getRowCount() {
				return this.tags.length;
			}
			public Object getValueAt(int rowIndex, int columnIndex) {
				if (columnIndex == 0)
					return this.tags[rowIndex];
				else if (columnIndex == 1)
					return this.mappings.getProperty(this.tags[rowIndex]);
				else return null;
			}
			public void setValueAt(Object newValue, int rowIndex, int columnIndex) {
				if ((columnIndex == 1) && (newValue != null) && (newValue instanceof String)) {
					this.mappings.setProperty(this.tags[rowIndex], newValue.toString());
					notifyMappingChanged();
				}
			}
			public boolean isCellEditable(int rowIndex, int columnIndex) {
				return (columnIndex == 1);
			}
			public void addTableModelListener(TableModelListener l) {}
			public void removeTableModelListener(TableModelListener l) {}
		}
		
		private class TagListEditorPanel extends JPanel implements DocumentListener {
			private JTextArea editor;
			private JScrollPane editorBox;
			private StringVector content;
			private boolean dirty = false;
			TagListEditorPanel() {
				super(new BorderLayout(), true);
				this.editor = new JTextArea();
				this.editor.setEditable(true);
				this.editorBox = new JScrollPane(this.editor);
				this.add(this.editorBox, BorderLayout.CENTER);
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
				this.editor.setText(this.content.concatStrings("\n"));
				this.editor.getDocument().addDocumentListener(this);
			}
			public void changedUpdate(DocumentEvent de) {}
			public void insertUpdate(DocumentEvent de) {
				this.dirty = true;
			}
			public void removeUpdate(DocumentEvent de) {
				this.dirty = true;
			}
		}
		public void commitChanges() {
			if (!this.isDirty())
				return;
			StringVector config = new StringVector();
			
			//	mappings
			for (Iterator tit = tagMapping.keySet().iterator(); tit.hasNext();) {
				String tag = ((String) tit.next());
				config.addElement("M:" + tag + " " + tagMapping.getProperty(tag));
			}
			
			//	paragraph tag
			this.paragraphTagListEditor.getContent();
			for (int t = 0; t < paragraphTags.size(); t++)
				config.addElement("P:" + paragraphTags.get(t));
			
			//	ignore tag
			this.ignoreTagListEditor.getContent();
			for (int t = 0; t < ignoreTags.size(); t++)
				config.addElement("I:" + ignoreTags.get(t));
			
			//	ignore content tag
			this.ignoreContentTagListEditor.getContent();
			for (int t = 0; t < ignoreContentTags.size(); t++)
				config.addElement("C:" + ignoreContentTags.get(t));
			
			//	store it
			try {
				Writer w = new OutputStreamWriter(dataProvider.getOutputStream("config.cnfg"), "UTF-8");
				config.storeContent(w);
				w.flush();
				w.close();
			}
			catch (IOException ioe) {
				ioe.printStackTrace(System.out);
			}
		}
	}
	
	private static final Grammar html = new Html();
	private static final Parser parser = new Parser(html);
	
	private class HtmlDocumentFormat extends DocumentFormat {
		
		private Tokenizer tokenizer;
		
		HtmlDocumentFormat(Tokenizer tokenizer) {
			this.tokenizer = tokenizer;
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat#isExportFormat()
		 */
		public boolean isExportFormat() {
			return false;
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat#getDefaultSaveFileExtension()
		 */
		public String getDefaultSaveFileExtension() {
			return "html";
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat#loadDocument(java.io.Reader)
		 */
		public MutableAnnotation loadDocument(Reader source) throws IOException {
			DocumentRoot document = Gamta.newDocument(this.tokenizer);
			HtmlDocumentReader hdr = new HtmlDocumentReader(document);
			parser.stream(source, hdr);
			hdr.close();
			return document;
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat#saveDocument(de.uka.ipd.idaho.gamta.MutableAnnotation, java.io.Writer)
		 */
		public boolean saveDocument(QueriableAnnotation data, Writer out) throws IOException {
			return false; // we are not saving as HTML
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat#saveDocument(de.goldenGate.DocumentEditor, java.io.Writer)
		 */
		public boolean saveDocument(DocumentEditor data, Writer out) throws IOException {
			return false; // we are not saving as HTML
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat#saveDocument(de.goldenGate.DocumentEditor, de.uka.ipd.idaho.gamta.MutableAnnotation, java.io.Writer)
		 */
		public boolean saveDocument(DocumentEditor data, QueriableAnnotation doc, Writer out) throws IOException {
			return false; // we are not saving as HTML
		}
		
		/* 
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat#accept(java.lang.String)
		 */
		public boolean accept(String fileName) {
			fileName = fileName.toLowerCase();
			return (fileName.endsWith(".html") || fileName.endsWith(".htm"));
		}
		
		/*
		 * @see javax.swing.filechooser.FileFilter#getDescription()
		 */
		public String getDescription() {
			return "HTML and HTM files";
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat#equals(de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat)
		 */
		public boolean equals(DocumentFormat format) {
			return ((format != null) && (format instanceof HtmlDocumentFormat));
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGate.plugins.Resource#getName()
		 */
		public String getName() {
			return HTML_FORMAT_NAME;
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGate.plugins.Resource#getProviderClassName()
		 */
		public String getProviderClassName() {
			return HtmlDocumentFormatter.class.getName();
		}
	}
	
	private Properties tagMapping = new Properties();
	private StringVector ignoreTags = new StringVector();
	private StringVector paragraphTags = new StringVector();
	
	private StringVector ignoreContentTags = new StringVector();
	private String ignoreContentTag = null;
	
	private class HtmlDocumentReader extends TokenReceiver {
		
		private static final boolean DEBUG = false;
		
		private MutableAnnotation document;
		
		private char lastChar = StringUtils.NULLCHAR;
		
		private Stack stack = new Stack();
		private ArrayList annotations = new ArrayList();
		
		HtmlDocumentReader(MutableAnnotation document) throws IOException {
			this.document = document;
		}
		
		/** @see de.uka.ipd.idaho.htmlXmlUtil.TokenReceiver#close()
		 */
		public void close() throws IOException {
			
			//	close all remaining Annotations (if any)
			while (!this.stack.isEmpty()) {
				AnnotationContainer ac = ((AnnotationContainer) this.stack.pop());
				ac.size = (this.document.size() - ac.start);
			}
			
			//	write Annotations
			for (int a = 0; a < this.annotations.size(); a++) {
				AnnotationContainer ac = ((AnnotationContainer) this.annotations.get(a));
				if (ac.size != 0) {
					String aType = tagMapping.getProperty(ac.type, ac.type);
					MutableAnnotation annotation = this.document.addAnnotation(aType, ac.start, ac.size);
					
					//	mark paragraph end if necessary
					if (paragraphTags.containsIgnoreCase(aType))
						annotation.lastToken().setAttribute(Token.PARAGRAPH_END_ATTRIBUTE, Token.PARAGRAPH_END_ATTRIBUTE);
					
					//	transfer attributes
					String[] attributeNames = ac.attributes.getAttributeNames();
					for (int n = 0; n < attributeNames.length; n++)
						annotation.setAttribute(attributeNames[n], ac.attributes.getAttribute(attributeNames[n]));
					
					//	mark header paragraphs
					if (MutableAnnotation.PARAGRAPH_TYPE.equals(aType) && ac.type.matches("[hH][1-6]"))
						annotation.setAttribute("type", ac.type.toLowerCase());
				}
			}
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.htmlXmlUtil.TokenReceiver#storeToken(java.lang.String, int)
		 */
		public void storeToken(String token, int treeDepth) throws IOException {
			if (DEBUG) System.out.println("HtmlDocumentReader: got token - " + token);
			
			//	tagging information, ignore processing instructions and !DOCTYPE, etc.
			if (html.isTag(token) && !token.startsWith("<?") && !token.startsWith("<!")) {
				String type = html.getType(token);
				if (DEBUG) System.out.println("   it's a tag - type is '" + type + "'");
				
				//	content ignorable tag
				if (ignoreContentTags.containsIgnoreCase(type)) {
					if (html.isEndTag(token) && type.equalsIgnoreCase(ignoreContentTag)) {
						if (DEBUG) System.out.println("   done ignoring content of '" + type + "' tag");
						ignoreContentTag = null;
						return;
					}
					else if (!html.isSingularTag(token) && (ignoreContentTag == null)) {
						if (DEBUG) System.out.println("   start ignoring content of '" + type + "' tag");
						ignoreContentTag = type;
						return;
					}
				}
				
				//	currently ignoting content
				if (ignoreContentTag != null)
					return;
				
				//	ignorable tag
				if (ignoreTags.containsIgnoreCase(type)) {
					if (DEBUG) System.out.println("   ignored");
					return;
				}
				
				if (DEBUG) System.out.println("   processing tag ...");
				
				//	append line of dashes for HR tag
				if ("hr".equalsIgnoreCase(type)) {
					if (this.document.size() != 0) {
						if (DEBUG) System.out.println("   it's a horizontal row");
						this.document.lastToken().setAttribute(Token.PARAGRAPH_END_ATTRIBUTE, Token.PARAGRAPH_END_ATTRIBUTE);
						
						if (this.lastChar != '\n')
							this.document.addChar('\n');
						
						AnnotationContainer ac = new AnnotationContainer(MutableAnnotation.PARAGRAPH_TYPE, this.document.size(), TreeNodeAttributeSet.getTagAttributes(token, html));
						
						int oldSize = this.document.size();
						this.document.addTokens("----------------------------------------------------------------");
						this.document.lastToken().setAttribute(Token.PARAGRAPH_END_ATTRIBUTE, Token.PARAGRAPH_END_ATTRIBUTE);
						
						this.document.addChar('\n');
						this.lastChar = '\n';
						
						ac.size = (this.document.size() - oldSize);
						this.annotations.add(ac);
					}
				}
				
				//	end of Annotation
				else if (html.isEndTag(token)) {
					if (DEBUG) System.out.println("   it's an end tag");
					
					//	any Annotation open?
					if (!this.stack.isEmpty()) {
						AnnotationContainer ac = ((AnnotationContainer) this.stack.pop());
						
						//	catch empty table cells
						if (TABLE_CELL_ANNOTATION_TYPE.equalsIgnoreCase(ac.type) && (this.document.size() == ac.start)) {
							if (DEBUG) System.out.println("   filling empty table cell");
							ac.attributes.setAttribute(EMPTY_CELL_MARKER_ATTRIBUTE, "true");
							if (this.lastChar > 32)
								this.document.addChar('\n');
							this.document.addTokens(" " + EMPTY_CELL_FILLER + " ");
							this.lastChar = ' ';
						}
						
						ac.size = (this.document.size() - ac.start);
						if (DEBUG) System.out.println("   closed Annotation - type '" + ac.type + "'");
						
						//	paragraph end
						if (paragraphTags.containsIgnoreCase(type) && (this.document.size() != 0)) {
							this.document.lastToken().setAttribute(Token.PARAGRAPH_END_ATTRIBUTE, Token.PARAGRAPH_END_ATTRIBUTE);
							if (this.lastChar > 32)
								this.document.addChar('\n');
							this.lastChar = '\n';
						}
					}
				}
				
				//	start of Annotation
				else if (!html.isSingularTag(token)) {
					if (DEBUG) System.out.println("   it's a start tag");
					AnnotationContainer ac = new AnnotationContainer(type, this.document.size(), TreeNodeAttributeSet.getTagAttributes(token, html));
					this.stack.push(ac);
					this.annotations.add(ac);
					if (DEBUG) System.out.println("   opened Annotation - type '" + ac.type + "'");
				}
				
				//	singular tag, probable line break
				else if (paragraphTags.containsIgnoreCase(type) && (this.document.size() != 0)) {
					if (DEBUG) System.out.println("   it's a singular tag");
					this.document.lastToken().setAttribute(Token.PARAGRAPH_END_ATTRIBUTE, Token.PARAGRAPH_END_ATTRIBUTE);
					
					if (this.lastChar > 32)
						this.document.addChar('\n');
					
					this.lastChar = '\n';
				}
			}
			
			//	textual content
			else if (ignoreContentTag == null) {
				int oldSize = this.document.size();
				
				String plain;
				if (html.isStrictXML())
					plain = html.unescape(token).trim();
				else plain = IoTools.prepareForPlainText(token).trim();
				
				if (plain.length() != 0) {// ignore whitespace between tags and text
					TokenSequence ts = this.document.getTokenizer().tokenize(plain);//
					
					//	make sure tokens before and after tags do not cling together
					if ((this.document.size() != 0) && (ts.size() != 0) && (this.lastChar > 32) && Gamta.insertSpace(this.document.lastToken(), ts.firstToken()))
						this.document.addChar(' ');
					
					this.document.addTokens(ts);
					this.lastChar = plain.charAt(plain.length() - 1);
				}
				
				for (int t = oldSize; t < this.document.size(); t++) 
					this.document.tokenAt(t).removeAttribute(Token.PARAGRAPH_END_ATTRIBUTE);
			}
		}
		
		/**	representation for Annotations that are not yet complete
		 */
		private class AnnotationContainer {
			private String type;
			private int start;
			private int size = 1;
			private TreeNodeAttributeSet attributes;
			private AnnotationContainer(String type, int start, TreeNodeAttributeSet attributes) {
				this.type = type;
				this.start = start;
				this.attributes = attributes;
			}
		}
	}
//	
//	/**
//	 * @param args
//	 */
//	public static void main(String[] args) throws Exception {
//		Reader r;
//		File file = new File("E:/Testdaten/EcologyTestbed/spelda2001.htm");
//		r = new FileReader(file);
////		URL url = new URL("http://www.google.com");
////		r = new InputStreamReader(url.openStream(), "UTF-8");
//		HtmlDocumentFormat hdf = new HtmlDocumentFormat(Gamta.INNER_PUNCTUATION_TOKENIZER);
//		MutableAnnotation dr = hdf.loadDocument(r);
//		AnnotationUtils.writeXML(dr, new PrintWriter(System.out));
//	}
}
