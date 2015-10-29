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
import java.io.IOException;
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
 * Document format provider enabeling GoldenGATE to validate XML data against
 * arbitrary XML Schema definitions on output. This document format acts as a
 * decorator to another document format that actually encodes a GAMTA document
 * to some XML representation and outputs the resulting data to a character
 * stream.
 * 
 * @author sautter
 */
public class ValidatingXmlOutputFormatter extends AbstractDocumentFormatProvider {
	
	private DocumentFormat[] dfs = {new ValidatingDocumentFormat()};
	private DocumentFormat[] saveFormats = null;
	
	private static final String[] encodingNotSectable = {"<The Encoding Is Not Selectable>"};
	private String[] encodingNames = null;
	
	/* 
	 * @see de.uka.ipd.idaho.goldenGate.plugins.GoldenGatePlugin#getPluginName()
	 */
	public String getPluginName() {
		return "XML Validating Document Format";
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormatProvider#getFileExtensions()
	 */
	public String[] getFileExtensions() {
		return new String[0];
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#init()
	 */
	public void init() {
		DocumentFormatProvider[] formatters = this.parent.getDocumentFormatProviders();
		ArrayList saveFormatList = new ArrayList();
		for (int d = 0; d < formatters.length; d++) if (formatters[d] != this) {
			DocumentFormat[] saveFormats = formatters[d].getSaveFileFilters();
			for (int f = 0; f < saveFormats.length; f++)
				if ("xml".equalsIgnoreCase(saveFormats[f].getDefaultSaveFileExtension()))
					saveFormatList.add(saveFormats[f]);
		}
		this.saveFormats = ((DocumentFormat[]) saveFormatList.toArray(new DocumentFormat[saveFormatList.size()]));
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormatProvider#getFormatForFileExtension(java.lang.String)
	 */
	public DocumentFormat getFormatForFileExtension(String fileExtension) {
		return null; // Validation is not for automated loading
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormatProvider#getFormatForName(java.lang.String)
	 */
	public DocumentFormat getFormatForName(String formatName) {
		return null; // Validation is not for automated saving now
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormatProvider#getLoadFileFilters()
	 */
	public DocumentFormat[] getLoadFileFilters() {
		return new DocumentFormat[0];
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
	
	private class ValidatingDocumentFormat extends DocumentFormat {
		
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
			return (format instanceof ValidatingDocumentFormat);
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
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat#saveDocument(de.goldenGate.DocumentEditor, de.uka.ipd.idaho.gamta.MutableAnnotation, java.io.Writer)
		 */
		public boolean saveDocument(DocumentEditor data, QueriableAnnotation doc, Writer out) throws IOException {
			if ((saveFormats == null) || (encodingNames == null))
				ValidatingXmlOutputFormatter.this.init();
			
			ValidatedSavingDialog ced = new ValidatedSavingDialog("Please Select Format", "Save", encodingNotSectable, saveFormats);
			ced.setVisible(true);
			
			if (ced.isCommitted)
				return ced.getFormat().saveDocument(data, doc, out);
			else return false;
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat#saveDocument(de.goldenGate.DocumentEditor, java.io.Writer)
		 */
		public boolean saveDocument(DocumentEditor data, Writer out) throws IOException {
			if ((saveFormats == null) || (encodingNames == null))
				ValidatingXmlOutputFormatter.this.init();
			
			ValidatedSavingDialog ced = new ValidatedSavingDialog("Please Select Format", "Save", encodingNotSectable, saveFormats);
			ced.setVisible(true);
			
			if (ced.isCommitted)
				return ced.getFormat().saveDocument(data, out);
			else return false;
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat#saveDocument(de.uka.ipd.idaho.gamta.MutableAnnotation, java.io.Writer)
		 */
		public boolean saveDocument(QueriableAnnotation data, Writer out) throws IOException {
			if ((saveFormats == null) || (encodingNames == null))
				ValidatingXmlOutputFormatter.this.init();
			
			ValidatedSavingDialog ced = new ValidatedSavingDialog("Please Select Format", "Save", encodingNotSectable, saveFormats);
			ced.setVisible(true);
			if (ced.isCommitted)
				return ced.getFormat().saveDocument(data, out);
			else return false;
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
			return "XML Files (Schema Validation)";
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGate.plugins.Resource#getName()
		 */
		public String getName() {
			return "<XML Validating Document Format>";
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGate.plugins.Resource#getProviderClassName()
		 */
		public String getProviderClassName() {
			return ValidatingXmlOutputFormatter.class.getName();
		}
	}
	
	private class ValidatedSavingDialog extends DialogPanel {
		
		JComboBox formatChooser;
		boolean isCommitted = false;
		
		ValidatedSavingDialog(String title, String commitText, String[] encodingNames, DocumentFormat[] formats) {
			super(title, true);
			
			this.formatChooser = new JComboBox(formats);
			this.formatChooser.setBorder(BorderFactory.createLoweredBevelBorder());
			this.formatChooser.setEditable(false);
			
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
			this.add(selectorPanel, BorderLayout.CENTER);
			this.add(mainButtonPanel, BorderLayout.SOUTH);
			
			this.setResizable(true);
			this.setSize(new Dimension(400, 120));
			this.setLocationRelativeTo(this.getOwner());
		}
		
		DocumentFormat getFormat() {
			return ((DocumentFormat) this.formatChooser.getSelectedItem());
		}
	}
}
