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
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.HashSet;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.util.AnnotationInputStream;
import de.uka.ipd.idaho.goldenGate.DocumentEditor;
import de.uka.ipd.idaho.goldenGate.plugins.AbstractDocumentFormatProvider;
import de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat;
import de.uka.ipd.idaho.goldenGate.plugins.SettingsPanel;
import de.uka.ipd.idaho.goldenGate.util.ResourceDialog;
import de.uka.ipd.idaho.htmlXmlUtil.accessories.XsltUtils;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * Document format provider enabling GoldenGATE to transform documents through
 * an arbitrary XSLT stylesheet on output, writing the transformation result to
 * the specified character stream instead of the plain XML representation of the
 * document being stored. This document format provider manages the XSLT
 * stylesheets deposited in its data path in the same way as a resource manager
 * does with resources. With JRE's older than version 1.5.x, it requires the
 * Apache Xalan XML/XSLT engine on the class path, most sensibly in its '...Bin'
 * folder.
 * 
 * @author sautter
 */
public class XsltOutputFormatter extends AbstractDocumentFormatProvider {
	
	private static final String[] FILE_EXTENSIONS = {"xml"};
	
	private StringVector availableXsltNames = new StringVector();
	private boolean availableXsltsModified = false;
	private HashSet validXslts = new HashSet();
	
	public XsltOutputFormatter() {}
	
	/* 
	 * @see de.uka.ipd.idaho.goldenGate.plugins.GoldenGatePlugin#getPluginName()
	 */
	public String getPluginName() {
		return "XSLT Dynamic Document Format";
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#init()
	 */
	public void init() {
		StringVector availableXsltNames = this.loadListResource("availableXsltNames.cnfg");
		if (availableXsltNames != null)
			this.availableXsltNames.addContentIgnoreDuplicates(availableXsltNames);
		this.availableXsltNames.sortLexicographically(false, false);
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#exit()
	 */
	public void exit() {
		this.storeAvailableXsltNames();
	}
	
	private void storeAvailableXsltNames() {
		if (this.availableXsltsModified) try {
			this.availableXsltNames.sortLexicographically(false, false);
			this.storeListResource("availableXsltNames.cnfg", this.availableXsltNames);
			this.availableXsltsModified = false;
		} catch (IOException ioe) {}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#getSettingsPanel()
	 */
	public SettingsPanel getSettingsPanel() {
		return new XsltDocumentFormatSettingsPanel();
	}
	
	private class XsltDocumentFormatSettingsPanel extends SettingsPanel {
		private JTable xsltTable = new JTable();
		XsltDocumentFormatSettingsPanel() {
			super("XSLT Document Format", "Document format for saving XML documents with an intermediate XSL transformation");
			this.setLayout(new BorderLayout());
			this.setDoubleBuffered(true);
			
			JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
			JButton button;
			button = new JButton("Update XSLT List");
			button.setBorder(BorderFactory.createRaisedBevelBorder());
			button.setPreferredSize(new Dimension(120, 21));
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					refreshXsltTable();
				}
			});
			buttonPanel.add(button);
			button = new JButton("Clear XSLT Cache");
			button.setBorder(BorderFactory.createRaisedBevelBorder());
			button.setPreferredSize(new Dimension(120, 21));
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					validXslts.clear();
				}
			});
			buttonPanel.add(button);
			
			JScrollPane xsltTableBox = new JScrollPane(this.xsltTable);
			xsltTableBox.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			
			this.add(buttonPanel, BorderLayout.NORTH);
			this.add(xsltTableBox, BorderLayout.CENTER);
			
			this.xsltTable.setColumnModel(new DefaultTableColumnModel() {
				public TableColumn getColumn(int columnIndex) {
					TableColumn tc = super.getColumn(columnIndex);
					if (columnIndex == 0) {
						tc.setPreferredWidth(70);
						tc.setMinWidth(70);
						tc.setMaxWidth(100);
					}
					tc.setResizable(true);
					return tc;
				}
			});
			
			this.refreshXsltTable();
		}
		
		private void refreshXsltTable() {
			final StringVector xsltNames = new StringVector();
			String[] dataNames = dataProvider.getDataNames();
			System.out.println("XSLT data list: got " + dataNames.length + " data items");
			for (int n = 0; n < dataNames.length; n++) {
				System.out.println(" - " + dataNames[n]);
				if (dataNames[n].toLowerCase().endsWith(".xslt") || dataNames[n].toLowerCase().endsWith(".xsl"))
					xsltNames.addElement(dataNames[n]);
			}
			
			System.out.println(" ==> " + xsltNames.size() + " stylesheets");
			this.xsltTable.setModel(new TableModel() {
				public int getRowCount() {
					return xsltNames.size();
				}
				public int getColumnCount() {
					return 2;
				}
				public String getColumnName(int columnIndex) {
					if (columnIndex == 0)
						return "Available";
					else if (columnIndex == 1)
						return "XSLT Name";
					else return null;
				}
				public Class getColumnClass(int columnIndex) {
					if (columnIndex == 0)
						return Boolean.class;
					else if (columnIndex == 1)
						return String.class;
					else return null;
				}
				public boolean isCellEditable(int rowIndex, int columnIndex) {
					return (columnIndex == 0);
				}
				public Object getValueAt(int rowIndex, int columnIndex) {
					if (columnIndex == 0)
						return new Boolean(availableXsltNames.contains(xsltNames.get(rowIndex)));
					else if (columnIndex == 1)
						return xsltNames.get(rowIndex);
					else return null;
				}
				public void setValueAt(Object newValue, int rowIndex, int columnIndex) {
					if ((columnIndex == 0) && (newValue instanceof Boolean)) {
						if (((Boolean) newValue).booleanValue())
							availableXsltNames.addElementIgnoreDuplicates(xsltNames.get(rowIndex));
						else availableXsltNames.removeAll(xsltNames.get(rowIndex));
						availableXsltsModified = true;
					}
				}
				public void addTableModelListener(TableModelListener tml) {}
				public void removeTableModelListener(TableModelListener tml) {}
			});
			
			this.xsltTable.validate();
			this.xsltTable.repaint();
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGate.plugins.SettingsPanel#commitChanges()
		 */
		public void commitChanges() {
			storeAvailableXsltNames();
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
		String extension = fileExtension.toLowerCase();
		if (extension.startsWith("."))
			extension = extension.substring(1);
		if ("xml".equalsIgnoreCase(extension))
			return new XsltDynamicDocumentFormat();
		else return null;
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
		return this.availableXsltNames.toStringArray();
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormatProvider#getFormatForName(java.lang.String)
	 */
	public DocumentFormat getFormatForName(String formatName) {
		Transformer transformer = this.getTransformer(formatName);
		if (transformer == null)
			return null;
		else return new XsltDynamicDocumentFormat(formatName, transformer);
	}
	
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
		DocumentFormat[] formats = {new XsltDynamicDocumentFormat()};
		return formats;
	}
	
	private Transformer getTransformer(String name) {
		if (name == null)
			return null;
		
		try {
			Transformer transformer = XsltUtils.getTransformer(("XsltOutputFormat:" + name), this.dataProvider.getInputStream(name), !this.validXslts.add(name));
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			return transformer;
		}
		catch (IOException ioe) {
			System.out.println(ioe.getClass().getName() + " (" + ioe.getMessage() + ") while obtaining Transformer.");
			ioe.printStackTrace(System.out);
		}
		
		return null;
	}
	
	private class XsltDynamicDocumentFormat extends DocumentFormat {
		
		private String name;
		private Transformer transformer = null;
		
		XsltDynamicDocumentFormat() {
			this(null, null);
		}
		
		XsltDynamicDocumentFormat(String name, Transformer transformer) {
			this.name = name;
			this.transformer = transformer;
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat#isExportFormat()
		 */
		public boolean isExportFormat() {
			return true;
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat#getDefaultSaveFileExtension()
		 */
		public String getDefaultSaveFileExtension() {
			return "xml";
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat#loadDocument(java.io.Reader)
		 */
		public MutableAnnotation loadDocument(Reader source) throws IOException {
			return null;
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat#saveDocument(de.goldenGate.DocumentEditor, java.io.OutputStream)
		 */
		public boolean saveDocument(DocumentEditor data, OutputStream out) throws IOException {
			return this.saveDocument(data, this.getOutputStreamWriter(out, UTF_8_ENCODING_NAME));
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat#saveDocument(de.uka.ipd.idaho.gamta.QueriableAnnotation, java.io.OutputStream)
		 */
		public boolean saveDocument(QueriableAnnotation data, OutputStream out) throws IOException {
			return this.saveDocument(data, this.getOutputStreamWriter(out, UTF_8_ENCODING_NAME));
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat#saveDocument(de.goldenGate.DocumentEditor, de.uka.ipd.idaho.gamta.QueriableAnnotation, java.io.OutputStream)
		 */
		public boolean saveDocument(DocumentEditor data, QueriableAnnotation doc, OutputStream out) throws IOException {
			return this.saveDocument(data, doc, this.getOutputStreamWriter(out, UTF_8_ENCODING_NAME));
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat#saveDocument(de.uka.ipd.idaho.gamta.QueriableAnnotation, java.io.Writer)
		 */
		public boolean saveDocument(QueriableAnnotation data, Writer out) throws IOException {
			return this.saveDocument(null, data, out);
		}
		
		/* 
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat#saveDocument(de.goldenGate.DocumentEditor, java.io.Writer)
		 */
		public boolean saveDocument(DocumentEditor data, Writer out) throws IOException {
			return this.saveDocument(data, data.getContent(), out);
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat#saveDocument(de.goldenGate.DocumentEditor, de.uka.ipd.idaho.gamta.MutableAnnotation, java.io.Writer)
		 */
		public boolean saveDocument(DocumentEditor data, QueriableAnnotation doc, Writer out) throws IOException {
			if (this.transformer == null) {
				ResourceDialog rd = ResourceDialog.getResourceDialog(getSaveFormatNames(), "Select XSLT Stylesheet", "Select");
				rd.setVisible(true);
				if (rd.isCommitted()) {
					this.name = rd.getSelectedResourceName();
					this.transformer = getTransformer(this.name);
				}
				else return false;
			}
			try {
				this.transformer.transform(new StreamSource(new AnnotationInputStream(doc, "  ", "utf-8")), new StreamResult(out));
				out.flush();
				return true;
			}
			catch (TransformerException e) {
				throw new IOException(e.getMessageAndLocation());
			}
		}
		
		/* 
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat#accept(java.lang.String)
		 */
		public boolean accept(String fileName) {
			return fileName.toLowerCase().endsWith(".xml");
		}
		
		/*
		 * @see javax.swing.filechooser.FileFilter#getDescription()
		 */
		public String getDescription() {
			return "XML file (XSLT " + ((this.name == null) ? "transformed" : this.name) + ")";
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat#equals(de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat)
		 */
		public boolean equals(DocumentFormat format) {
			if (format == null)
				return false;
			if (!(format instanceof XsltDynamicDocumentFormat))
				return false;
			XsltDynamicDocumentFormat dwdf = ((XsltDynamicDocumentFormat) format);
			if (this.name == null)
				return true; // allow an undetermined instance to be pre-selected if the previous selection was another instance that has been determined in the meantime 
			else return this.name.equals(dwdf.name);
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
			return XsltOutputFormatter.class.getName();
		}
	}
}