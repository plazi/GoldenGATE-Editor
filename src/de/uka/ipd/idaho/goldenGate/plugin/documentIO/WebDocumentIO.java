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
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.goldenGate.DocumentEditor;
import de.uka.ipd.idaho.goldenGate.plugins.AbstractDocumentIO;
import de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat;
import de.uka.ipd.idaho.goldenGate.plugins.DocumentFormatProvider;
import de.uka.ipd.idaho.goldenGate.plugins.DocumentSaveOperation;
import de.uka.ipd.idaho.goldenGate.util.DialogPanel;
import de.uka.ipd.idaho.stringUtils.StringUtils;
import de.uka.ipd.idaho.stringUtils.accessories.StringSelector;

/**
 * Document IO implementation enabling the GoldenGATE Editor to load documents
 * from URLs. Writing data to a URL is a hard thing to do, for there are many
 * ways web servers can receive data, and it's close to impossible to implement
 * them all. If the protocol of a URL to save a document to is HTTP, this class
 * uses the PUT method. For the other supported protocols, namely FTP and FILE,
 * no request method is set. This class does not support HTTPS.
 * 
 * @author sautter
 */
public class WebDocumentIO extends AbstractDocumentIO {
	
	private static final String testFileName = "index.html";
	
	private String lastLoadUrl = "http://";
	private DocumentFormat lastLoadFormat = null;
	
	private String lastSaveUrl = "http://";
	private DocumentFormat lastSaveFormat = null;
	
	/* 
	 * @see de.uka.ipd.idaho.goldenGate.plugins.GoldenGatePlugin#getPluginName()
	 */
	public String getPluginName() {
		return "Web Document IO";
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentLoader#getLoadDocumentMenuItem()
	 */
	public JMenuItem getLoadDocumentMenuItem() {
		JMenuItem mi = new JMenuItem("Load Document from URL");
		return mi;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentLoader#loadDocument()
	 */
	public DocumentData loadDocument() throws Exception {
		
		//	ask if web access allowed if in offline mode
		if (!this.dataProvider.allowWebAccess() && (JOptionPane.showConfirmDialog(DialogPanel.getTopWindow(), "GoldenGATE is in offline mode, allow loading document from URL anyway?", "Allow Web Access", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) != JOptionPane.YES_OPTION))
			return null;
		
		//	get document formats
		DocumentFormatProvider[] formatters = this.parent.getDocumentFormatProviders();
		ArrayList formatList = new ArrayList();
		String defaultFormatName = DocumentFormat.getDefaultLoadFormatName();
		boolean enforceDefaultFormat = DocumentFormat.enforceDefaultSaveFormat();
		DocumentFormat defaultFormat = null;
		DocumentFormat chosenFormat = null;
		DocumentFormat htmlFormat = null;
		for (int d = 0; d < formatters.length; d++) {
			DocumentFormat[] formats = formatters[d].getLoadFileFilters();
			for (int f = 0; f < formats.length; f++) {
				formatList.add(formats[f]);
				if ((defaultFormatName != null) && defaultFormatName.equals(formats[f].getDescription()))
					defaultFormat = formats[f]; 
				if (formats[f].equals(this.lastLoadFormat))
					chosenFormat = formats[f];
				if ((htmlFormat == null) && formats[f].accept(testFileName))
					htmlFormat = formats[f];
			}
		}
		
		//	prompt for URL to load document form
		UrlIoDialog uid = new UrlIoDialog("Load Document From URL", this.lastLoadUrl, "Load", ((DocumentFormat[]) formatList.toArray(new DocumentFormat[formatList.size()])), null);
		if ((defaultFormat != null) && ((chosenFormat == null) || enforceDefaultFormat))
			uid.formatChooser.setSelectedItem(defaultFormat);
		else if (chosenFormat != null)
			uid.formatChooser.setSelectedItem(chosenFormat);
		else if (htmlFormat != null)
			uid.formatChooser.setSelectedItem(htmlFormat);
		
		uid.setLocationRelativeTo(DialogPanel.getTopWindow());
		uid.setVisible(true);
		
		//	load document
		String urlString = uid.getUrl();
		DocumentFormat format = uid.getFormat(); 
		if (urlString != null) {
			try {
				URL url = this.dataProvider.getURL(urlString);
				LoadInputStream lis = new LoadInputStream(url.openStream());
				LoadTimeoutWatchdog ltw = new LoadTimeoutWatchdog(2000, urlString, lis);
				ltw.start();
				
				DocumentData dd = new DocumentData(format.loadDocument(lis), StringUtils.replaceAll((url.getHost() + url.getPath()), "/", "."), format);
				
				lis.close();
				
				this.lastLoadUrl = urlString;
				this.lastLoadFormat = format;
				
				return dd;
			}
			catch (IOException ioe) {
				throw new Exception((ioe.getClass().getName() + " (" + ioe.getMessage() + ")\nwhile trying to load " + urlString), ioe);
			}
		}
		else return null;
	}
	
	private class LoadInputStream extends FilterInputStream {
		long lastRead = System.currentTimeMillis();
		boolean open = true;
		LoadInputStream(InputStream in) {
			super(in);
		}
		public int read() throws IOException {
			int r = super.read();
			this.lastRead = System.currentTimeMillis();
			return r;
		}
		public int read(byte[] b, int off, int len) throws IOException {
			int r = super.read(b, off, len);
			this.lastRead = System.currentTimeMillis();
			return r;
		}
		public void close() throws IOException {
			super.close();
			this.open = false;
		}
	}
	
	private class LoadTimeoutWatchdog extends Thread {
		private long timeout;
		private String url;
		private LoadInputStream toWatch;
		LoadTimeoutWatchdog(long timeout, String url, LoadInputStream toWatch) {
			this.timeout = timeout;
			this.url = url;
			this.toWatch = toWatch;
		}
		public void run() {
			while (this.toWatch.open) {
				try {
					Thread.sleep(this.timeout / 2);
				} catch (InterruptedException ie) {}
				if (this.toWatch.lastRead < (System.currentTimeMillis() - this.timeout)) try {
					System.out.println("WebDocumentIO.LoadTimer: Loading document from " + this.url + " timed out, closing connection.");
					this.toWatch.close();
				} catch (Exception e) {}
			}
		}
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentSaver#getSaveDocumentMenuItem()
	 */
	public JMenuItem getSaveDocumentMenuItem() {
		JMenuItem mi = new JMenuItem("Save Document to URL");
		return mi;
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentSaver#getSaveDocumentPartsMenuItem()
	 */
	public JMenuItem getSaveDocumentPartsMenuItem() {
		JMenuItem mi = new JMenuItem("Save Document Parts to URL");
		return mi;
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentSaver#getSaveOperation(de.uka.ipd.idaho.goldenGate.plugins.DocumentSaveOperation)
	 */
	public DocumentSaveOperation getSaveOperation(DocumentSaveOperation model) {
		return this.produceSaveOperation((model == null) ? null : model.getDocumentFormat());
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentSaver#getSaveOperation(java.lang.String, de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat)
	 */
	public DocumentSaveOperation getSaveOperation(String documentName, DocumentFormat format) {
		return this.produceSaveOperation(format);
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentSaver#saveDocumentParts(de.uka.ipd.idaho.goldenGate.DocumentEditor, de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat, java.lang.String)
	 */
	public String saveDocumentParts(DocumentEditor data, DocumentFormat modelFormat, String modelType) {
		
		//	ask if web access allowed if in offline mode
		if (!this.dataProvider.allowWebAccess() && (JOptionPane.showConfirmDialog(DialogPanel.getTopWindow(), "GoldenGATE is in offline mode, allow saving document parts to URL anyway?", "Allow Web Access", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) != JOptionPane.YES_OPTION))
			return null;
		
		//	get actual document
		QueriableAnnotation doc = data.getContent();
		
		//	select part type
		StringSelector typeSelector = new StringSelector("Parts to Save", doc.getAnnotationTypes(), false);
		if (modelType != null) typeSelector.setSelectedString(modelType);
		
		//	select format
		DocumentFormatProvider[] formatters = this.parent.getDocumentFormatProviders();
		ArrayList formatList = new ArrayList();
		String defaultFormatName = DocumentFormat.getDefaultSaveFormatName();
		boolean enforceDefaultFormat = DocumentFormat.enforceDefaultSaveFormat();
		DocumentFormat defaultFormat = null;
		DocumentFormat chosenFormat = null;
		for (int d = 0; d < formatters.length; d++) {
			DocumentFormat[] formats = formatters[d].getSaveFileFilters();
			for (int f = 0; f < formats.length; f++) {
				formatList.add(formats[f]);
				if ((defaultFormatName != null) && defaultFormatName.equals(formats[f].getDescription()))
					defaultFormat = formats[f]; 
				if (formats[f].equals(this.lastSaveFormat))
					chosenFormat = formats[f];
			}
		}
		
		//	prompt for url to save parts to
		UrlIoDialog uid = new UrlIoDialog("Save Document Parts To URL", this.lastSaveUrl, "Save", ((DocumentFormat[]) formatList.toArray(new DocumentFormat[formatList.size()])), typeSelector);
		if ((defaultFormat != null) && ((modelFormat == null) || enforceDefaultFormat))
			uid.formatChooser.setSelectedItem(defaultFormat);
		else if (modelFormat != null)
			uid.formatChooser.setSelectedItem(modelFormat);
		else if (chosenFormat != null)
			uid.formatChooser.setSelectedItem(chosenFormat);
		
		uid.setLocationRelativeTo(DialogPanel.getTopWindow());
		uid.setVisible(true);
		
		String urlString = uid.getUrl();
		if (urlString != null) {
			try {
//				URL url = new URL(urlString);
				URL url = this.dataProvider.getURL(urlString);
				DocumentFormat format = uid.getFormat(); 
				this.lastSaveFormat = format;
				
				String partType = typeSelector.getSelectedString();
				QueriableAnnotation[] parts = doc.getAnnotations(partType);
				
				for (int p = 0; p < parts.length; p++) {
					URLConnection uc = url.openConnection();
					uc.setDoOutput(true);
					if ("http".equals(url.getProtocol().toLowerCase()))
						((HttpURLConnection) uc).setRequestMethod("PUT");
					
					//	write document
					OutputStream out = uc.getOutputStream();
					format.saveDocument(data, parts[p], out);
					out.flush(); out.close();
					
					//	read response
					try {
						InputStreamReader isr = new InputStreamReader(uc.getInputStream());
						int c = isr.read();
						while (c != -1) c = isr.read();
					} catch (Exception e) {}
				}
				
				return partType;
			} catch (IOException ioe) {
				JOptionPane.showMessageDialog(DialogPanel.getTopWindow(), "The specified URL is invalid.", "Invalid URL", JOptionPane.ERROR_MESSAGE);
				return null;
			}
		} else return null;
	}
	
	private DocumentSaveOperation produceSaveOperation(DocumentFormat modelFormat) {
		DocumentFormatProvider[] formatters = this.parent.getDocumentFormatProviders();
		ArrayList formatList = new ArrayList();
		String defaultFormatName = DocumentFormat.getDefaultSaveFormatName();
		boolean enforceDefaultFormat = DocumentFormat.enforceDefaultSaveFormat();
		DocumentFormat defaultFormat = null;
		DocumentFormat chosenFormat = null;
		for (int d = 0; d < formatters.length; d++) {
			DocumentFormat[] formats = formatters[d].getSaveFileFilters();
			for (int f = 0; f < formats.length; f++) {
				formatList.add(formats[f]);
				if ((defaultFormatName != null) && defaultFormatName.equals(formats[f].getDescription()))
					defaultFormat = formats[f]; 
				if (formats[f].equals(this.lastSaveFormat))
					chosenFormat = formats[f];
			}
		}
		
		UrlIoDialog uid = new UrlIoDialog("Save Document To URL", this.lastSaveUrl, "Save", ((DocumentFormat[]) formatList.toArray(new DocumentFormat[formatList.size()])), null);
		if ((defaultFormat != null) && ((modelFormat == null) || enforceDefaultFormat))
			uid.formatChooser.setSelectedItem(defaultFormat);
		else if (modelFormat != null)
			uid.formatChooser.setSelectedItem(modelFormat);
		else if (chosenFormat != null)
			uid.formatChooser.setSelectedItem(chosenFormat);
		
		uid.setLocationRelativeTo(DialogPanel.getTopWindow());
		uid.setVisible(true);
		
		String urlString = uid.getUrl();
		DocumentFormat format = uid.getFormat(); 
		
		if (urlString != null) {
			try {
				URL url = this.dataProvider.getURL(urlString);
				this.lastSaveUrl = urlString;
				this.lastSaveFormat = format;
				return new UrlSaveOperation(url, format);
			}
			catch (IOException ioe) {
				JOptionPane.showMessageDialog(DialogPanel.getTopWindow(), "The specified URL is invalid.", "Invalid URL", JOptionPane.ERROR_MESSAGE);
				return null;
			}
		} else return null;
	}
	
	private class UrlSaveOperation implements DocumentSaveOperation {
		
		private URL url;
		private DocumentFormat format;
		
		UrlSaveOperation(URL url, DocumentFormat format) {
			this.url = url;
			this.format = format;
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentSaveOperation#keepAsDefault()
		 */
		public boolean keepAsDefault() {
			return false;
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentSaveOperation#saveDocument(de.uka.ipd.idaho.gamta.QueriableAnnotation)
		 */
		public String saveDocument(QueriableAnnotation data) {
			
			//	ask if web access allowed if in offline mode
			if (!dataProvider.allowWebAccess() && (JOptionPane.showConfirmDialog(DialogPanel.getTopWindow(), "GoldenGATE is in offline mode, allow saving document parts to URL anyway?", "Allow Web Access", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) != JOptionPane.YES_OPTION))
				return null;
			
			try {
				//  open connection & save data
				URLConnection uc = this.url.openConnection();
				uc.setDoOutput(true);
				if ("http".equals(this.url.getProtocol().toLowerCase()))
					((HttpURLConnection) uc).setRequestMethod("PUT");
				OutputStream out = uc.getOutputStream();
				
				//	save document
				if (!this.format.saveDocument(data, out)) {
					
					//	save document in new format
					this.format = this.selectDocumentFormat(this.format);
					this.format.saveDocument(data, out);
				}
				out.flush();
				out.close();
				
				try {
					InputStreamReader isr = new InputStreamReader(uc.getInputStream());
					StringBuffer response = new StringBuffer("Response from server:\n");
					int c = isr.read();
					while (c != -1) {
						response.append((char) c);
						c = isr.read();
					}
					JOptionPane.showMessageDialog(DialogPanel.getTopWindow(), response.toString(), "Document Saved To URL", JOptionPane.INFORMATION_MESSAGE);
				}
				catch (Exception e) {}
			}
			catch (IOException ioe) {
				JOptionPane.showMessageDialog(DialogPanel.getTopWindow(), (ioe.getClass().getName() + " (" + ioe.getMessage() + ")\nwhile trying to save document to " + this.url.toString()), "Could Not Save Document To URL", JOptionPane.ERROR_MESSAGE);
			}
			
			//	return null so save is not repeatable
			return null;
		}
		
		/**
		 * save a document using the settings of this SaveOperation
		 * @param data the document to save
		 * @return a SaveOperation to save the document again with the same
		 *         configuration, or null, if this SaveOperation cannot be used
		 *         again (for instance if a session can be used only once)
		 */
		public String saveDocument(DocumentEditor data) {
			
			//	ask if web access allowed if in offline mode
			if (!dataProvider.allowWebAccess() && (JOptionPane.showConfirmDialog(DialogPanel.getTopWindow(), "GoldenGATE is in offline mode, allow saving document parts to URL anyway?", "Allow Web Access", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) != JOptionPane.YES_OPTION))
				return null;
			
			try {
				//  open connection & save data
				URLConnection uc = this.url.openConnection();
				uc.setDoOutput(true);
				if ("http".equals(this.url.getProtocol().toLowerCase()))
					((HttpURLConnection) uc).setRequestMethod("PUT");
				OutputStream out = uc.getOutputStream();
				
				//	save document
				if (!this.format.saveDocument(data, out)) {
					
					//	save document in new format
					this.format = this.selectDocumentFormat(this.format);
					this.format.saveDocument(data, out);
				}
				out.flush();
				out.close();
				
				try {
					InputStreamReader isr = new InputStreamReader(uc.getInputStream());
					StringBuffer response = new StringBuffer("Response from server:\n");
					int c = isr.read();
					while (c != -1) {
						response.append((char) c);
						c = isr.read();
					}
					JOptionPane.showMessageDialog(data, response.toString(), "Document Saved To URL", JOptionPane.INFORMATION_MESSAGE);
				}
				catch (Exception e) {}
			}
			catch (IOException ioe) {
				JOptionPane.showMessageDialog(data, (ioe.getClass().getName() + " (" + ioe.getMessage() + ")\nwhile trying to save document to " + this.url.toString()), "Could Not Save Document To URL", JOptionPane.ERROR_MESSAGE);
			}
			
			//	return null so save is not repeatable
			return null;
		}
		
		private DocumentFormat selectDocumentFormat(DocumentFormat format) {
			DocumentFormatProvider[] formatters = parent.getDocumentFormatProviders();
			ArrayList formatList = new ArrayList();
			for (int d = 0; d < formatters.length; d++) {
				DocumentFormat[] formats = formatters[d].getSaveFileFilters();
				for (int f = 0; f < formats.length; f++) formatList.add(formats[f]);
			}
			SelectFormatDialog sfd = new SelectFormatDialog(("'" + format.getDescription() + "' cannot save the document, please select a different format."), ((DocumentFormat[]) formatList.toArray(new DocumentFormat[formatList.size()])));
			sfd.setVisible(true);
			return sfd.getFormat();
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentSaveOperation#getDocumentFormat()
		 */
		public DocumentFormat getDocumentFormat() {
			return this.format;
		}
		
		/*
		 * @return the title of a document after it has been saved through this SaveOperation
		 */
		public String getDocumentName() {
			return (StringUtils.replaceAll((this.url.getHost() + this.url.getPath()), "/", "."));
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentSaveOperation#documentClosed()
		 */
		public void documentClosed() {
			// nothing to do here
		}
	}
	
	private class UrlIoDialog extends DialogPanel {
		
		private JTextField urlInput = new JTextField("http://");
		private JComboBox formatChooser;
		private boolean isCommitted = false;
		
		UrlIoDialog(String title, String url, String commitText, DocumentFormat[] formats, StringSelector typeSelector) {
			super(title, true);
			
			if (url != null) this.urlInput.setText(url);
			this.urlInput.setBorder(BorderFactory.createLoweredBevelBorder());
			this.urlInput.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					isCommitted = true;
					dispose();
				}
			});
			
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
			selectorPanel.add(new JLabel("URL"), gbc.clone());
			gbc.gridx = 1;
			gbc.weightx = 1;
			selectorPanel.add(this.urlInput, gbc.clone());
			
			gbc.gridy = 1;
			gbc.gridx = 0;
			gbc.weightx = 0;
			selectorPanel.add(new JLabel("Format"), gbc.clone());
			gbc.gridx = 1;
			gbc.weightx = 1;
			selectorPanel.add(this.formatChooser, gbc.clone());
			
			if (typeSelector != null) {
				gbc.gridy = 2;
				gbc.gridx = 0;
				gbc.weightx = 1;
				gbc.gridwidth = 2;
				selectorPanel.add(typeSelector, gbc.clone());
			}
			
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
			
			JPanel mainButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
			mainButtonPanel.add(commitButton);
			
			//	put the whole stuff together
			this.add(selectorPanel, BorderLayout.CENTER);
			this.add(mainButtonPanel, BorderLayout.SOUTH);
			
			this.setResizable(true);
			this.setSize(new Dimension(400, ((typeSelector == null) ? 120 : 150)));
		}
		
		String getUrl() {
			return (this.isCommitted ? this.urlInput.getText() : null);
		}
		
		DocumentFormat getFormat() {
			return ((DocumentFormat) this.formatChooser.getSelectedItem());
		}
	}
	
	private class SelectFormatDialog extends DialogPanel {
		
		private JComboBox formatChooser;
		
		SelectFormatDialog(String text, DocumentFormat[] formats) {
			super("Select Document Format", true);
			
			this.formatChooser = new JComboBox(formats);
			this.formatChooser.setBorder(BorderFactory.createLoweredBevelBorder());
			this.formatChooser.setEditable(false);
			String defaultFormatName = DocumentFormat.getDefaultSaveFormatName();
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
			JButton commitButton = new JButton("OK");
			commitButton.setBorder(BorderFactory.createRaisedBevelBorder());
			commitButton.setPreferredSize(new Dimension(100, 21));
			commitButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					dispose();
				}
			});
			
			JPanel mainButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
			mainButtonPanel.add(commitButton);
			
			//	put the whole stuff together
			this.add(selectorPanel, BorderLayout.CENTER);
			this.add(mainButtonPanel, BorderLayout.SOUTH);
			
			this.setResizable(true);
			this.setSize(new Dimension(400, 120));
			this.setLocationRelativeTo(DialogPanel.getTopWindow());
		}
		
		DocumentFormat getFormat() {
			return ((DocumentFormat) this.formatChooser.getSelectedItem());
		}
	}
}