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


import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;

import de.uka.ipd.idaho.gamta.DocumentRoot;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.util.GenericGamtaXML;
import de.uka.ipd.idaho.goldenGate.DocumentEditor;
import de.uka.ipd.idaho.goldenGate.plugins.AbstractDocumentFormatProvider;
import de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat;

/**
 * Document format provider enabeling GoldenGATE to read and write documents in
 * the generic GAMTA data format, a form of XML representation capable of
 * encoding annotations that together would not result in a well-formed document
 * in normal XML output.
 * 
 * @author sautter
 */
public class GamtaDocumentFormatter extends AbstractDocumentFormatProvider {
	
	private static final String[] FILE_EXTENSIONS = {"gamta"};
	private static final String GAMTA_FORMAT_NAME = "<GAMTA Document Format>";
	
	public GamtaDocumentFormatter() {}
	
	/* 
	 * @see de.uka.ipd.idaho.goldenGate.plugins.GoldenGatePlugin#getPluginName()
	 */
	public String getPluginName() {
		return "GAMTA Document Format";
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
		if ("gamta".equalsIgnoreCase(extension))
			return new GamtaDocumentFormat();
		else return null;
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormatProvider#getLoadFormatNames()
	 */
	public String[] getLoadFormatNames() {
		String[] formatNames = {GAMTA_FORMAT_NAME};
		return formatNames;
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormatProvider#getSaveFormatNames()
	 */
	public String[] getSaveFormatNames() {
		String[] formatNames = {GAMTA_FORMAT_NAME};
		return formatNames;
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormatProvider#getFormatForName(java.lang.String)
	 */
	public DocumentFormat getFormatForName(String formatName) {
		if (GAMTA_FORMAT_NAME.equalsIgnoreCase(formatName))
			return new GamtaDocumentFormat();
		else return null;
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormatProvider#getLoadFileFilters()
	 */
	public DocumentFormat[] getLoadFileFilters() {
		DocumentFormat[] formats = {new GamtaDocumentFormat()};
		return formats;
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormatProvider#getSaveFileFilters()
	 */
	public DocumentFormat[] getSaveFileFilters() {
		DocumentFormat[] formats = {new GamtaDocumentFormat()};
		return formats;
	}
	
	private class GamtaDocumentFormat extends DocumentFormat {
		
		GamtaDocumentFormat() {}
		
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
			return "gamta";
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat#getInputStreamReader(java.io.InputStream)
		 */
		protected InputStreamReader getInputStreamReader(InputStream source) throws IOException {
			if (DEBUG_CHAR_ENCODING) System.out.println("Determining character encoding ...");
			BufferedInputStream bis = new BufferedInputStream(source);
			
			//	read some bytes to guess encoding
			bis.mark(CHARSET_GUESS_LOOKAHEAD);
			
			//	read lookahead bytes
			int lookahead = Math.min(CHARSET_GUESS_LOOKAHEAD, bis.available());
			if (DEBUG_CHAR_ENCODING) System.out.println("  - lookahead is " + lookahead);
			int[] lookaheads = new int[lookahead];
			for (int l = 0; l < lookahead; l++)
				lookaheads[l] = bis.read();
			bis.reset();
			if (DEBUG_CHAR_ENCODING) System.out.println("  - input stream reset, available bytes: " + bis.available());
			
			//	check for SGML encoding specifications
			int firstChar = 0;
			while ((firstChar < lookaheads.length) && ((lookaheads[firstChar] < 32) || (lookaheads[firstChar] > 126)))
				firstChar++;
			
			//	tag does not start with first character, probably BOM
			if (firstChar != 0)
				return super.getInputStreamReader(bis);
			
			//	parse start tag
			if ((firstChar < lookaheads.length) && (lookaheads[firstChar] == '<')) {
				StringBuffer tagBuffer = new StringBuffer();
				int i = 0;
				while ((firstChar + i) < lookaheads.length) {
					char c = ((char) lookaheads[firstChar + i]);
					tagBuffer.append(c);
					if (c == '>') {
						firstChar = (firstChar + i + 1);
						i = lookaheads.length;
					}
					else i++;
				}
				String tag = tagBuffer.toString();
				if (DEBUG_CHAR_ENCODING) System.out.println("  - read first tag: " + tag);
				
				//	generic GAMTA XML file <sAnnot_-1 t_y_p_e="document" ENCODING="UTF-8" ...>
				if (tag.startsWith("<sAnnot_")) {
					int split = tag.indexOf(ENCODING_ATTRIBUTE);
					if (split != -1) {
						String encoding = tag.substring(split + ENCODING_ATTRIBUTE.length());
						split = encoding.indexOf('"');
						if (split != -1) {
							encoding = encoding.substring(split + 1);
							split = encoding.indexOf('"');
							if (split != -1) {
								encoding = encoding.substring(0, encoding.indexOf('"'));
								if (DEBUG_CHAR_ENCODING) System.out.println("  - parsed encoding: " + encoding);
								return new InputStreamReader(bis, encoding);
							}
						}
					}
				}
			}
			
			//	could not parse encoding, let default method do the rest
			return super.getInputStreamReader(bis);
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat#loadDocument(java.io.Reader)
		 */
		public MutableAnnotation loadDocument(Reader source) throws IOException {
			DocumentRoot document = Gamta.newDocument(parent.getTokenizer());
			GenericGamtaXML.readDocument(source, document);
			return document;
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat#saveDocument(de.uka.ipd.idaho.gamta.MutableAnnotation, java.io.Writer)
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
			GenericGamtaXML.storeDocument(doc, out);
			return true;
		}
		
		/* 
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat#accept(java.lang.String)
		 */
		public boolean accept(String fileName) {
			return fileName.toLowerCase().endsWith(".gamta");
		}
		
		/*
		 * @see javax.swing.filechooser.FileFilter#getDescription()
		 */
		public String getDescription() {
			return "GAMTA files";
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat#equals(de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat)
		 */
		public boolean equals(DocumentFormat format) {
			return ((format != null) && (format instanceof GamtaDocumentFormat));
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGate.plugins.Resource#getName()
		 */
		public String getName() {
			return GAMTA_FORMAT_NAME;
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGate.plugins.Resource#getProviderClassName()
		 */
		public String getProviderClassName() {
			return GamtaDocumentFormatter.class.getName();
		}
	}
}
