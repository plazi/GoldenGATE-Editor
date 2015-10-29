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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.goldenGate.DocumentEditor;
import de.uka.ipd.idaho.goldenGate.plugins.AbstractDocumentFormatProvider;
import de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat;
import de.uka.ipd.idaho.goldenGate.plugins.DocumentFormatProvider;
import de.uka.ipd.idaho.goldenGate.util.DialogPanel;

/**
 * Document format provider enabeling GoldenGATE to handle different character
 * encodings. This document format acts as a decorator to another document
 * format that actually decodes/encodes a GAMTA document from/to any character
 * stream. It makes available all code pages available from the respective host
 * platform.
 * 
 * @author sautter
 */
public class CustomEncodingDocumentFormatter extends AbstractDocumentFormatProvider {
	
	private DocumentFormat[] dfs = {new CustomEncodingDocumentFormat()};
	
	private static final String[] encodingNotSelectable = {"<The Encoding Is Not Selectable>"};
	private String[] encodingNames = null;
	
	/* 
	 * @see de.uka.ipd.idaho.goldenGate.plugins.GoldenGatePlugin#getPluginName()
	 */
	public String getPluginName() {
		return "Custom Encoding Document Format Decorator";
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormatProvider#getFileExtensions()
	 */
	public String[] getFileExtensions() {
		return new String[0];
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractDocumentFormatProvider#init()
	 */
	public void init() {
		this.encodingNames = DocumentFormat.getEncodingNames();
	}
	
	private DocumentFormat[] getLoadDocumentFormats() {
		DocumentFormatProvider[] formatters = this.parent.getDocumentFormatProviders();
		ArrayList loadFormatList = new ArrayList();
		for (int d = 0; d < formatters.length; d++) if (formatters[d] != this) {
			DocumentFormat[] loadFormats = formatters[d].getLoadFileFilters();
			for (int f = 0; f < loadFormats.length; f++)
				loadFormatList.add(loadFormats[f]);
		}
		return ((DocumentFormat[]) loadFormatList.toArray(new DocumentFormat[loadFormatList.size()]));
	}
	
	private DocumentFormat[] getSaveDocumentFormats() {
		DocumentFormatProvider[] formatters = this.parent.getDocumentFormatProviders();
		ArrayList saveFormatList = new ArrayList();
		for (int d = 0; d < formatters.length; d++) if (formatters[d] != this) {
			DocumentFormat[] saveFormats = formatters[d].getSaveFileFilters();
			for (int f = 0; f < saveFormats.length; f++)
				saveFormatList.add(saveFormats[f]);
		}
		return ((DocumentFormat[]) saveFormatList.toArray(new DocumentFormat[saveFormatList.size()]));
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormatProvider#getFormatForFileExtension(java.lang.String)
	 */
	public DocumentFormat getFormatForFileExtension(String fileExtension) {
		return null; // Custom Encodiong is not for automated loading
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormatProvider#getFormatForName(java.lang.String)
	 */
	public DocumentFormat getFormatForName(String formatName) {
		return null; // Custom Encodiong is not for automated saving
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormatProvider#getLoadFileFilters()
	 */
	public DocumentFormat[] getLoadFileFilters() {
		return this.dfs;
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormatProvider#getLoadFormatNames()
	 */
	public String[] getLoadFormatNames() {
		return new String[0];
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormatProvider#getSaveFileFilters()
	 */
	public DocumentFormat[] getSaveFileFilters() {
		return this.dfs;
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormatProvider#getSaveFormatNames()
	 */
	public String[] getSaveFormatNames() {
		return new String[0];
	}
	
	private class CustomEncodingDocumentFormat extends DocumentFormat {
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat#isExportFormat()
		 */
		public boolean isExportFormat() {
			return false;
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat#equals(de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat)
		 */
		public boolean equals(DocumentFormat format) {
			return (format instanceof CustomEncodingDocumentFormat);
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat#getDefaultSaveFileExtension()
		 */
		public String getDefaultSaveFileExtension() {
			return null; // there is no default extension
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat#loadDocument(java.io.InputStream)
		 */
		public MutableAnnotation loadDocument(InputStream source) throws IOException {
			if (encodingNames == null) CustomEncodingDocumentFormatter.this.init();
			
			CustomEncodingDialog ced = new CustomEncodingDialog(true, "Please Select Encoding & Format", "Load", encodingNames);
			ced.encodingChooser.setSelectedItem(getDefaultEncodingName());
			ced.setVisible(true);
			
			if (ced.isCommitted) {
				String encoding = ced.getEncoding();
				DocumentFormat df = ced.getFormat();
				MutableAnnotation doc;
				if (UTF_8_ENCODING_NAME.equals(encoding) || UTF_16_LE_ENCODING_NAME.equals(encoding) || UTF_16_BE_ENCODING_NAME.equals(encoding))
					doc = df.loadDocument(new InputStreamReader(source, encoding));
				else doc = df.loadDocument(new InputStreamReader(source, encoding));
				doc.setAttribute(ENCODING_ATTRIBUTE, encoding);
				return doc;
			}
			else return null;
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat#loadDocument(java.io.Reader)
		 */
		public MutableAnnotation loadDocument(Reader source) throws IOException {
			if (encodingNames == null) CustomEncodingDocumentFormatter.this.init();
			
			CustomEncodingDialog ced = new CustomEncodingDialog(true, "Please Select Format", "Load", encodingNotSelectable);
			ced.setVisible(true);
			
			if (ced.isCommitted)
				return ced.getFormat().loadDocument(source);
			else return null;
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat#saveDocument(de.goldenGate.DocumentEditor, de.uka.ipd.idaho.gamta.MutableAnnotation, java.io.Writer)
		 */
		public boolean saveDocument(DocumentEditor data, QueriableAnnotation doc, Writer out) throws IOException {
			if (encodingNames == null) CustomEncodingDocumentFormatter.this.init();
			
			CustomEncodingDialog ced = new CustomEncodingDialog(false, "Please Select Format", "Save", encodingNotSelectable);
			ced.setVisible(true);
			
			if (ced.isCommitted)
				return ced.getFormat().saveDocument(data, doc, out);
			else return false;
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat#saveDocument(de.goldenGate.DocumentEditor, de.uka.ipd.idaho.gamta.MutableAnnotation, java.io.OutputStream)
		 */
		public boolean saveDocument(DocumentEditor data, QueriableAnnotation doc, OutputStream out) throws IOException {
			if (encodingNames == null) CustomEncodingDocumentFormatter.this.init();
			
			CustomEncodingDialog ced = new CustomEncodingDialog(false, "Please Select Encoding & Format", "Save", encodingNames);
			ced.encodingChooser.setSelectedItem(doc.getAttribute(ENCODING_ATTRIBUTE, data.getContent().getAttribute(ENCODING_ATTRIBUTE, getDefaultEncodingName())));
			ced.setVisible(true);
			
			if (ced.isCommitted) {
				String encoding = ced.getEncoding();
				DocumentFormat df = ced.getFormat();
				return df.saveDocument(data, doc, this.getOutputStreamWriter(out, encoding));
//				if (UTF_8_ENCODING_NAME.equals(encoding) || UTF_16_LE_ENCODING_NAME.equals(encoding) || UTF_16_BE_ENCODING_NAME.equals(encoding))
//					return df.saveDocument(data, doc, this.getOutputStreamWriter(out, encoding));
//				else return df.saveDocument(data, doc, new OutputStreamWriter(out, encoding));
			} else return false;
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat#saveDocument(de.goldenGate.DocumentEditor, java.io.Writer)
		 */
		public boolean saveDocument(DocumentEditor data, Writer out) throws IOException {
			if (encodingNames == null) CustomEncodingDocumentFormatter.this.init();
			
			CustomEncodingDialog ced = new CustomEncodingDialog(false, "Please Select Format", "Save", encodingNotSelectable);
			ced.setVisible(true);
			
			if (ced.isCommitted)
				return ced.getFormat().saveDocument(data, out);
			else return false;
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat#saveDocument(de.goldenGate.DocumentEditor, java.io.OutputStream)
		 */
		public boolean saveDocument(DocumentEditor data, OutputStream out) throws IOException {
			if (encodingNames == null) CustomEncodingDocumentFormatter.this.init();
			
			CustomEncodingDialog ced = new CustomEncodingDialog(false, "Please Select Encoding & Format", "Save", encodingNames);
			ced.encodingChooser.setSelectedItem(data.getContent().getAttribute(ENCODING_ATTRIBUTE, getDefaultEncodingName()));
			ced.setVisible(true);
			
			if (ced.isCommitted) {
				String encoding = ced.getEncoding();
				DocumentFormat df = ced.getFormat();
				return df.saveDocument(data, this.getOutputStreamWriter(out, encoding));
//				if (UTF_8_ENCODING_NAME.equals(encoding) || UTF_16_LE_ENCODING_NAME.equals(encoding) || UTF_16_BE_ENCODING_NAME.equals(encoding))
//					return df.saveDocument(data, this.getOutputStreamWriter(out, encoding));
//				else return df.saveDocument(data, new OutputStreamWriter(out, encoding));
			} else return false;
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat#saveDocument(de.uka.ipd.idaho.gamta.MutableAnnotation, java.io.Writer)
		 */
		public boolean saveDocument(QueriableAnnotation data, Writer out) throws IOException {
			if (encodingNames == null) CustomEncodingDocumentFormatter.this.init();
			
			CustomEncodingDialog ced = new CustomEncodingDialog(false, "Please Select Format", "Save", encodingNotSelectable);
			ced.setVisible(true);
			if (ced.isCommitted)
				return ced.getFormat().saveDocument(data, out);
			else return false;
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat#saveDocument(de.uka.ipd.idaho.gamta.MutableAnnotation, java.io.OutputStream)
		 */
		public boolean saveDocument(QueriableAnnotation data, OutputStream out) throws IOException {
			if (encodingNames == null) CustomEncodingDocumentFormatter.this.init();
			
			CustomEncodingDialog ced = new CustomEncodingDialog(false, "Please Select Encoding & Format", "Save", encodingNames);
			ced.encodingChooser.setSelectedItem(data.getAttribute(ENCODING_ATTRIBUTE, getDefaultEncodingName()));
			ced.setVisible(true);
			if (ced.isCommitted) {
				String encoding = ced.getEncoding();
				DocumentFormat df = ced.getFormat();
				return df.saveDocument(data, this.getOutputStreamWriter(out, encoding));
//				if (UTF_8_ENCODING_NAME.equals(encoding) || UTF_16_LE_ENCODING_NAME.equals(encoding) || UTF_16_BE_ENCODING_NAME.equals(encoding))
//					return df.saveDocument(data, this.getOutputStreamWriter(out, encoding));
//				else return df.saveDocument(data, new OutputStreamWriter(out, encoding));
			} else return false;
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
			return "All Files (Custom Encoding)";
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGate.plugins.Resource#getName()
		 */
		public String getName() {
			return "<Custom Encoding Document Format>";
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGate.plugins.Resource#getProviderClassName()
		 */
		public String getProviderClassName() {
			return CustomEncodingDocumentFormatter.class.getName();
		}
	}
	
	private class CustomEncodingDialog extends DialogPanel {
		
		private JComboBox encodingChooser;
		private JComboBox formatChooser;
		private boolean isCommitted = false;
		
		private boolean isDefaultEncoding = false;
		
		CustomEncodingDialog(boolean isLoadDialog, String title, String commitText, String[] encodingNames/*, DocumentFormat[] formats*/) {
			super(title, true);
			DocumentFormat[] formats = (isLoadDialog ? getLoadDocumentFormats() : getSaveDocumentFormats());
			
			this.encodingChooser = new JComboBox(encodingNames);
			this.encodingChooser.setBorder(BorderFactory.createLoweredBevelBorder());
			this.encodingChooser.setEditable(false);
			if (encodingNames == encodingNotSelectable) this.encodingChooser.setEnabled(false);
			else this.encodingChooser.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent ie) {
					DocumentFormat selectedFormat = getFormat();
					if (selectedFormat != null)
						isDefaultEncoding = selectedFormat.getFormatDefaultEncodingName().equals(encodingChooser.getSelectedItem());
				}
			});
			
			this.formatChooser = new JComboBox(formats);
			this.formatChooser.setBorder(BorderFactory.createLoweredBevelBorder());
			this.formatChooser.setEditable(false);
			this.formatChooser.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent ie) {
					if (isDefaultEncoding) {
						DocumentFormat selected = getFormat();
						if (selected != null)
							encodingChooser.setSelectedItem(selected.getFormatDefaultEncodingName());
					}
				}
			});
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
			gbc.weightx = 0;
			selectorPanel.add(new JLabel("Encoding"), gbc.clone());
			gbc.gridx = 1;
			gbc.weightx = 1;
			selectorPanel.add(this.encodingChooser, gbc.clone());
			
			gbc.gridy = 1;
			gbc.gridx = 0;
			gbc.weightx = 0;
			selectorPanel.add(new JLabel("Format"), gbc.clone());
			gbc.gridx = 1;
			gbc.weightx = 1;
			selectorPanel.add(this.formatChooser, gbc.clone());
			
			//	initialize main buttons
			JButton commitButton = new JButton(commitText);
			commitButton.setBorder(BorderFactory.createRaisedBevelBorder());
			commitButton.setPreferredSize(new Dimension(100, 21));
			commitButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					isCommitted = true;
					dispose();
				}
			});
			
			JButton abortButton = new JButton("Cancel");
			abortButton.setBorder(BorderFactory.createRaisedBevelBorder());
			abortButton.setPreferredSize(new Dimension(100, 21));
			abortButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					dispose();
				}
			});
			
			JPanel mainButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
			mainButtonPanel.add(commitButton);
			mainButtonPanel.add(abortButton);
			
			//	put the whole stuff together
			this.setLayout(new BorderLayout());
			this.add(selectorPanel, BorderLayout.CENTER);
			this.add(mainButtonPanel, BorderLayout.SOUTH);
			
			this.setResizable(true);
			this.setSize(new Dimension(400, 120));
			this.setLocationRelativeTo(this.getOwner());
		}
		
		String getEncoding() {
			return this.encodingChooser.getSelectedItem().toString();
		}
		
		DocumentFormat getFormat() {
			return ((DocumentFormat) this.formatChooser.getSelectedItem());
		}
	}
}
