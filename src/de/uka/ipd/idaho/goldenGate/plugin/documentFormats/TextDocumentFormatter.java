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


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.MutableTokenSequence;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.Token;
import de.uka.ipd.idaho.goldenGate.DocumentEditor;
import de.uka.ipd.idaho.goldenGate.plugins.AbstractDocumentFormatProvider;
import de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat;

/**
 * Document format provider enabeling GoldenGATE to read and write plain text
 * documents.
 * 
 * @author sautter
 */
public class TextDocumentFormatter extends AbstractDocumentFormatProvider {
	
	private static final String[] FILE_EXTENSIONS = {"txt"};
	private static final String TEXT_FORMAT_NAME = "<TXT Document Format>";
	private static final String LS_TEXT_FORMAT_NAME = "<LS TXT Document Format>";
	
	/* 
	 * @see de.uka.ipd.idaho.goldenGate.plugins.GoldenGatePlugin#getPluginName()
	 */
	public String getPluginName() {
		return "Text Document Format";
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
		if ("txt".equalsIgnoreCase(extension))
			return new TextDocumentFormat();
		else return null;
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormatProvider#getLoadFormatNames()
	 */
	public String[] getLoadFormatNames() {
//		String[] formatNames = {TEXT_FORMAT_NAME};
		String[] formatNames = {TEXT_FORMAT_NAME, LS_TEXT_FORMAT_NAME};
		return formatNames;
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormatProvider#getFormatForName(java.lang.String)
	 */
	public DocumentFormat getFormatForName(String formatName) {
		if (TEXT_FORMAT_NAME.equalsIgnoreCase(formatName))
			return new TextDocumentFormat();
		else if (LS_TEXT_FORMAT_NAME.equalsIgnoreCase(formatName))
			return new LsTextDocumentFormat();
		else return null;
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormatProvider#getLoadFileFilters()
	 */
	public DocumentFormat[] getLoadFileFilters() {
//		DocumentFormat[] formats = {new TextDocumentFormat()};
		DocumentFormat[] formats = {new TextDocumentFormat(), new LsTextDocumentFormat()};
		return formats;
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormatProvider#getSaveFormatNames()
	 */
	public String[] getSaveFormatNames() {
		String[] formatNames = {TEXT_FORMAT_NAME};
		return formatNames;
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormatProvider#getSaveFileFilters()
	 */
	public DocumentFormat[] getSaveFileFilters() {
		DocumentFormat[] formats = {new TextDocumentFormat()};
		return formats;
	}
	
	private class TextDocumentFormat extends DocumentFormat {
		
		TextDocumentFormat() {}
		
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
			return "txt";
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat#loadDocument(java.io.Reader)
		 */
		public MutableAnnotation loadDocument(Reader source) throws IOException {
			ArrayList paragraphs = new ArrayList();
			int paragraphStart = 0;
			
			//	read data
			MutableTokenSequence mts = Gamta.newTokenSequence("", parent.getTokenizer());
			BufferedReader br = new BufferedReader(source);
			String line;
			while ((line = br.readLine()) != null) {
				
				//	add characters
				mts.addChars(line);
				mts.addChar('\n');
				if (mts.size() != 0)
					mts.lastToken().setAttribute(Token.PARAGRAPH_END_ATTRIBUTE, Token.PARAGRAPH_END_ATTRIBUTE);
				
				//	mark paragraph
				if (mts.size() > paragraphStart) {
					paragraphs.add(Gamta.newAnnotation(mts, MutableAnnotation.PARAGRAPH_TYPE, paragraphStart, (mts.size() - paragraphStart)));
					paragraphStart = mts.size();
				}
			}
			
			//	create markup overlay
			MutableAnnotation data = Gamta.newDocument(mts);
			
			//	mark paragraphs
			for (int p = 0; p < paragraphs.size(); p++)
				data.addAnnotation((Annotation) paragraphs.get(p));
			
			//	finally ...
			return data;
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
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat#saveDocument(de.goldenGate.DocumentEditor, de.uka.ipd.idaho.gamta.QueriableAnnotation, java.io.Writer)
		 */
		public boolean saveDocument(DocumentEditor data, QueriableAnnotation doc, Writer out) throws IOException {
			BufferedWriter buf = new BufferedWriter(out);
			Token token = null;
			Token lastToken;
			
			//	write Tokens only
			for (int t = 0; t < doc.size(); t++) {
				lastToken = token;
				token = doc.tokenAt(t);
				
				//	retrieve normalized whitespace from Token otherwise
				if ((lastToken != null) && lastToken.hasAttribute(Token.PARAGRAPH_END_ATTRIBUTE))
					buf.newLine();
				else if ((t != 0) && (doc.getWhitespaceAfter(t-1).length() != 0))
					buf.write(' ');
				buf.write(token.getValue());
			}
			
			buf.flush();
			return true;
		}
		
		/* 
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat#accept(java.lang.String)
		 */
		public boolean accept(String fileName) {
			return fileName.toLowerCase().endsWith(".txt");
		}
		
		/*
		 * @see javax.swing.filechooser.FileFilter#getDescription()
		 */
		public String getDescription() {
			return "Text files";
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat#equals(de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat)
		 */
		public boolean equals(DocumentFormat format) {
			return ((format != null) && (format instanceof TextDocumentFormat));
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGate.plugins.Resource#getName()
		 */
		public String getName() {
			return TEXT_FORMAT_NAME;
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGate.plugins.Resource#getProviderClassName()
		 */
		public String getProviderClassName() {
			return TextDocumentFormatter.class.getName();
		}
	}
	
	private class LsTextDocumentFormat extends DocumentFormat {
		
		LsTextDocumentFormat() {}
		
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
			return "txt";
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat#loadDocument(java.io.Reader)
		 */
		public MutableAnnotation loadDocument(Reader source) throws IOException {
			ArrayList paragraphs = new ArrayList();
			int paragraphStart = 0;
			
			//	read data
			MutableTokenSequence mts = Gamta.newTokenSequence("", parent.getTokenizer());
			BufferedReader br = new BufferedReader(source);
			String line;
			while ((line = br.readLine()) != null) {
				
				//	blank line
				if (line.trim().length() == 0) {
					
					//	mark paragraph
					if (mts.size() > paragraphStart) {
						paragraphs.add(Gamta.newAnnotation(mts, MutableAnnotation.PARAGRAPH_TYPE, paragraphStart, (mts.size() - paragraphStart)));
						paragraphStart = mts.size();
					}
					
					//	we're done
					continue;
				}
				
				//	add characters
				mts.addChars(line);
				mts.addChar('\n');
				if (mts.size() != 0)
					mts.lastToken().setAttribute(Token.PARAGRAPH_END_ATTRIBUTE, Token.PARAGRAPH_END_ATTRIBUTE);
			}
			
			//	mark last paragraph (if any)
			if (mts.size() > paragraphStart) {
				paragraphs.add(Gamta.newAnnotation(mts, MutableAnnotation.PARAGRAPH_TYPE, paragraphStart, (mts.size() - paragraphStart)));
				paragraphStart = mts.size();
			}
			
			//	create markup overlay
			MutableAnnotation data = Gamta.newDocument(mts);
			
			//	mark paragraphs
			for (int p = 0; p < paragraphs.size(); p++)
				data.addAnnotation((Annotation) paragraphs.get(p));
			
			//	TODO consider running paragraph normalizer
			
			//	finally ...
			return data;
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat#saveDocument(de.uka.ipd.idaho.gamta.QueriableAnnotation, java.io.Writer)
		 */
		public boolean saveDocument(QueriableAnnotation data, Writer out) throws IOException {
			return false;
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat#saveDocument(de.goldenGate.DocumentEditor, java.io.Writer)
		 */
		public boolean saveDocument(DocumentEditor data, Writer out) throws IOException {
			return false;
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat#saveDocument(de.goldenGate.DocumentEditor, de.uka.ipd.idaho.gamta.QueriableAnnotation, java.io.Writer)
		 */
		public boolean saveDocument(DocumentEditor data, QueriableAnnotation doc, Writer out) throws IOException {
			return false;
		}
		
		/* 
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat#accept(java.lang.String)
		 */
		public boolean accept(String fileName) {
			return fileName.toLowerCase().endsWith(".txt");
		}
		
		/*
		 * @see javax.swing.filechooser.FileFilter#getDescription()
		 */
		public String getDescription() {
			return "Text files (blank line separated)";
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat#equals(de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat)
		 */
		public boolean equals(DocumentFormat format) {
			return ((format != null) && (format instanceof LsTextDocumentFormat));
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGate.plugins.Resource#getName()
		 */
		public String getName() {
			return LS_TEXT_FORMAT_NAME;
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGate.plugins.Resource#getProviderClassName()
		 */
		public String getProviderClassName() {
			return TextDocumentFormatter.class.getName();
		}
	}
}
