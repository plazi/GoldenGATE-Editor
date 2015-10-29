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
package de.uka.ipd.idaho.goldenGate.plugin.pdf;

import java.awt.GraphicsEnvironment;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.icepdf.core.exceptions.PDFException;
import org.icepdf.core.exceptions.PDFSecurityException;
import org.icepdf.core.pobjects.Document;

import de.uka.ipd.idaho.easyIO.util.RandomByteSource;
import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.DocumentRoot;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.util.ProgressMonitor;
import de.uka.ipd.idaho.gamta.util.SgmlDocumentReader;
import de.uka.ipd.idaho.gamta.util.imaging.ImagingConstants;
import de.uka.ipd.idaho.gamta.util.imaging.PageImage;
import de.uka.ipd.idaho.gamta.util.imaging.PageImageInputStream;
import de.uka.ipd.idaho.gamta.util.imaging.PageImageStore;
import de.uka.ipd.idaho.gamta.util.imaging.PageImageStore.AbstractPageImageStore;
import de.uka.ipd.idaho.gamta.util.swing.ProgressMonitorDialog;
import de.uka.ipd.idaho.goldenGate.DocumentEditor;
import de.uka.ipd.idaho.goldenGate.plugins.AbstractDocumentFormatProvider;
import de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat;
import de.uka.ipd.idaho.goldenGate.plugins.MonitorableDocumentFormat;
import de.uka.ipd.idaho.goldenGate.util.DialogPanel;
import de.uka.ipd.idaho.im.ImDocument;
import de.uka.ipd.idaho.im.gamta.ImDocumentRoot;
import de.uka.ipd.idaho.im.pdf.PdfExtractor;

/**
 * Document format provider for PDF documents, both text and image based. This
 * implementation is based on the IcePDF library and uses ImageMagick.
 * 
 * @author sautter
 */
public class PdfDocumentFormatter extends AbstractDocumentFormatProvider implements ImagingConstants {
	private static final String genericPdfDocumentFormatName = "<Generic PDF Document Format>";
	private static final String textPdfDocumentFormatName = "<Textual PDF Document Format>";
	private static final String imagePdfDocumentFormatName = "<OCR Image PDF Document Format>";
	private static final String pageOnlyImagePdfDocumentFormatName = "<Page-Only OCR Image PDF Document Format>";
	private static final String blocksOnlyImagePdfDocumentFormatName = "<Text-Block-Only OCR Image PDF Document Format>";
	
	private DocumentFormat genericPdfDocumentFormat = new GenericPdfDocumentFormat();
	private DocumentFormat textPdfDocumentFormat = new TextPdfDocumentFormat();
	private DocumentFormat imagePdfDocumentFormat = new ImagePdfDocumentFormat();
	private DocumentFormat pageOnlyImagePdfDocumentFormat = new PageOnlyImagePdfDocumentFormat();
	private DocumentFormat blocksOnlyImagePdfDocumentFormat = new BlocksOnlyImagePdfDocumentFormat();
	
	private PdfExtractor pdfExtractor;
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormatProvider#getFileExtensions()
	 */
	public String[] getFileExtensions() {
		String[] fes = {".pdf"};
		return fes;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#init()
	 */
	public void init() {
		
		//	register page image source
		PageImageStore pis = new AbstractPageImageStore() {
			public boolean isPageImageAvailable(String name) {
				if (!name.endsWith(IMAGE_FORMAT))
					name += ("." + IMAGE_FORMAT);
				return dataProvider.isDataAvailable("cache/" + name);
			}
			public PageImageInputStream getPageImageAsStream(String name) throws IOException {
				if (!name.endsWith(IMAGE_FORMAT))
					name += ("." + IMAGE_FORMAT);
				if (!dataProvider.isDataAvailable("cache/" + name))
					return null;
				return new PageImageInputStream(dataProvider.getInputStream("cache/" + name), this);
			}
			public boolean storePageImage(String name, PageImage pageImage) throws IOException {
				if (!name.endsWith(IMAGE_FORMAT))
					name += ("." + IMAGE_FORMAT);
				try {
					OutputStream imageOut = dataProvider.getOutputStream("cache/" + name);
					pageImage.write(imageOut);
					imageOut.close();
					return true;
				}
				catch (IOException ioe) {
					ioe.printStackTrace(System.out);
					return false;
				}
			}
			public int getPriority() {
				return 0; // we're storing each and every page image, so yield to more specific stores
			}
		};
		PageImage.addPageImageSource(pis);
		
		//	this is a breach of the data provider principle,
		//	but that's impossible to avoid if we want to use ImageMagick
		File pdfPath = new File(this.dataProvider.getAbsolutePath());
		this.pdfExtractor = new PdfExtractor(pdfPath, pis, !GraphicsEnvironment.isHeadless()); // use all available cores only in a desktop application
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager#getPluginName()
	 */
	public String getPluginName() {
		return "PDF Document Format";
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#getAboutBoxExtension()
	 */
	public String getAboutBoxExtension() {
		return  "IcePDF is open source software by Icesoft Technologies Inc.\n" +
				"The Tesseract OCR engine is open source software by Google Inc.\n" +
				"   formerly by Hewlett-Packard Company\n" +
				"ImageMagick is open source software by ImageMagick Studio LLC";
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormatProvider#getFormatForFileExtension(java.lang.String)
	 */
	public DocumentFormat getFormatForFileExtension(String fileExtension) {
		return ("pdf".equalsIgnoreCase(fileExtension) ? this.genericPdfDocumentFormat : null);
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormatProvider#getFormatForName(java.lang.String)
	 */
	public DocumentFormat getFormatForName(String formatName) {
		if (genericPdfDocumentFormatName.equals(formatName))
			return this.genericPdfDocumentFormat;
		else if (textPdfDocumentFormatName.equals(formatName))
			return this.textPdfDocumentFormat;
		else if (imagePdfDocumentFormatName.equals(formatName))
			return this.imagePdfDocumentFormat;
		else if (pageOnlyImagePdfDocumentFormatName.equals(formatName))
			return this.pageOnlyImagePdfDocumentFormat;
		else if (blocksOnlyImagePdfDocumentFormatName.equals(formatName))
			return this.blocksOnlyImagePdfDocumentFormat;
		else return null;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormatProvider#getLoadFormatNames()
	 */
	public String[] getLoadFormatNames() {
		if (this.pdfExtractor.isOcrAvailable()) {
			String[] lfns = {genericPdfDocumentFormatName, textPdfDocumentFormatName, imagePdfDocumentFormatName, blocksOnlyImagePdfDocumentFormatName, pageOnlyImagePdfDocumentFormatName};
			return lfns;
		}
		else {
			String[] lfns = {textPdfDocumentFormatName};
			return lfns;
		}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormatProvider#getLoadFileFilters()
	 */
	public DocumentFormat[] getLoadFileFilters() {
		if (this.pdfExtractor.isOcrAvailable()) {
			DocumentFormat[] lfs = {this.genericPdfDocumentFormat, this.textPdfDocumentFormat, this.imagePdfDocumentFormat, this.blocksOnlyImagePdfDocumentFormat, this.pageOnlyImagePdfDocumentFormat};
			return lfs;
		}
		else {
			DocumentFormat[] lfs = {this.textPdfDocumentFormat};
			return lfs;
		}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormatProvider#getSaveFormatNames()
	 */
	public String[] getSaveFormatNames() {
		return new String[0]; // we're only loading PDFs
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormatProvider#getSaveFileFilters()
	 */
	public DocumentFormat[] getSaveFileFilters() {
		return new DocumentFormat[0]; // we're only loading PDFs
	}
	
	private abstract class PdfDocumentFormat extends MonitorableDocumentFormat {
		private String name;
		private String description;
		PdfDocumentFormat(String name, String description) {
			this.name = name;
			this.description = description;
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat#isExportFormat()
		 */
		public boolean isExportFormat() {
			return false; // we're not saving anything anyway
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGate.plugins.Resource#getName()
		 */
		public String getName() {
			return this.name;
		}
		
		/* (non-Javadoc)
		 * @see javax.swing.filechooser.FileFilter#getDescription()
		 */
		public String getDescription() {
			return this.description;
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGate.plugins.Resource#getProviderClassName()
		 */
		public String getProviderClassName() {
			return PdfDocumentFormatter.class.getName();
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGate.plugins.MonitorableDocumentFormat#saveDocument(de.uka.ipd.idaho.gamta.QueriableAnnotation, java.io.Writer, de.uka.ipd.idaho.gamta.util.ProgressMonitor)
		 */
		public boolean saveDocument(QueriableAnnotation data, Writer out, ProgressMonitor pm) throws IOException {
			return false; // we're only loading documents
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGate.plugins.MonitorableDocumentFormat#saveDocument(de.uka.ipd.idaho.goldenGate.DocumentEditor, java.io.Writer, de.uka.ipd.idaho.gamta.util.ProgressMonitor)
		 */
		public boolean saveDocument(DocumentEditor data, Writer out, ProgressMonitor pm) throws IOException {
			return false; // we're only loading documents
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGate.plugins.MonitorableDocumentFormat#saveDocument(de.uka.ipd.idaho.goldenGate.DocumentEditor, de.uka.ipd.idaho.gamta.QueriableAnnotation, java.io.Writer, de.uka.ipd.idaho.gamta.util.ProgressMonitor)
		 */
		public boolean saveDocument(DocumentEditor data, QueriableAnnotation doc, Writer out, ProgressMonitor pm) throws IOException {
			return false; // we're only loading documents
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGate.plugins.MonitorableDocumentFormat#loadDocument(java.io.InputStream, de.uka.ipd.idaho.gamta.util.ProgressMonitor)
		 */
		public MutableAnnotation loadDocument(final InputStream source, ProgressMonitor pm) throws IOException {
			if (pm == null)
				pm = ProgressMonitor.dummy;
			
			//	TODO remove this once GG core itself starts monitoring
			ProgressMonitorDialog pmd = null;
			if (pm == ProgressMonitor.dummy) try {
				 pmd = new ProgressMonitorDialog(true, true, DialogPanel.getTopWindow(), "Loading PDF");
			} catch (Exception e) {}
			
			//	we're in some headless environment, load document in current thread
			if (pmd == null)
				return this.doLoadDocument(source, pm);
			
			//	we're in a GUI environment, load with status dialog
			pmd.setSize(500, 170);
			pmd.setLocationRelativeTo(DialogPanel.getTopWindow());
			final ProgressMonitorDialog fpmd = pmd;
			final MutableAnnotation[] doc = {null};
			final IOException[] ioe = {null};
			Thread dlt = new Thread() {
				public void run() {
					while (!fpmd.getWindow().isVisible()) try {
						Thread.sleep(50);
					} catch (InterruptedException ie) {}
					try {
						doc[0] = doLoadDocument(source, fpmd);
					}
					catch (IOException io) {
						ioe[0] = io;
					}
					finally {
						fpmd.close();
					}
				}
			};
			dlt.start();
			fpmd.popUp(true);
			
			//	throw exception or return result
			if (ioe[0] != null)
				throw ioe[0];
			return doc[0];
		}
		
		private MutableAnnotation doLoadDocument(InputStream source, ProgressMonitor pm) throws IOException {
			DocumentRoot doc;
			String docId;
			
			//	 try and load PDF
			try {
				pm.setStep("Loading PDF Document");
				BufferedInputStream bis = ((source instanceof BufferedInputStream) ? ((BufferedInputStream) source) : new BufferedInputStream(source));
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				byte[] buffer = new byte[1024];
				int read;
				while ((read = bis.read(buffer, 0, buffer.length)) != -1) {
					baos.write(buffer, 0, read);
					pm.setInfo(" - " + baos.size() + " bytes read");
				}
				bis.close();
				byte[] bytes = baos.toByteArray();
				pm.setInfo(" - " + bytes.length + " bytes read in total");
				
				docId = getChecksum(bytes);
				
				//	read cached document
				if (dataProvider.isDataAvailable("cache/" + this.getCachePrefix() + docId + ".xml")) {
					pm.setStep("Loading Document From Cache ...");
					InputStream docIn = dataProvider.getInputStream("cache/" + this.getCachePrefix() + docId + ".xml");
					BufferedReader docReader = new BufferedReader(new InputStreamReader(docIn, "UTF-8"));
					doc = SgmlDocumentReader.readDocument(docReader);
					doc.setDocumentProperty(DOCUMENT_ID_ATTRIBUTE, docId);
					pm.setStep("Document Loaded From Cache");
					pm.setProgress(100);
					return doc;
				}
				
				//	generate base document
				ImDocument imDoc = new ImDocument(docId);
				imDoc.setAttribute(ImDocument.TOKENIZER_ATTRIBUTE, ((parent == null) ? Gamta.INNER_PUNCTUATION_TOKENIZER : parent.getTokenizer()));
				pm.setInfo(" - document ID generated");
				
				//	parse PDF
				pm.setStep("Parsing PDF Document");
				Document pdfDoc = new Document();
				pdfDoc.setInputStream(new ByteArrayInputStream(bytes), "");
				
				//	extract content
				pm.setStep("Extracting Document Content ...");
				imDoc = this.loadDocument(imDoc, pdfDoc, bytes, pm);
				
				//	wrap document
				ImDocumentRoot wDoc = new ImDocumentRoot(imDoc, ImDocumentRoot.NORMALIZATION_LEVEL_RAW);
				wDoc.setShowTokensAsWordsAnnotations(true);
				wDoc.setUseRandomAnnotationIDs(false);
				doc = Gamta.copyDocument(wDoc);
				
				//	clean up
				imDoc.dispose();
			}
			catch (PDFException pdfe) {
				pdfe.printStackTrace(System.out);
				throw new IOException("Could not open PDF: " + pdfe.getMessage());
			}
			catch (PDFSecurityException pdfse) {
				pdfse.printStackTrace(System.out);
				throw new IOException("Could not open PDF: " + pdfse.getMessage());
			}
			
			//	try to cache document (still return it if caching fails)
			try {
				OutputStream docOut = dataProvider.getOutputStream("cache/" + this.getCachePrefix() + docId + ".xml");
				BufferedWriter docWriter = new BufferedWriter(new OutputStreamWriter(docOut, "UTF-8"));
				AnnotationUtils.writeXML(doc, docWriter);
				docWriter.flush();
				docWriter.close();
			}
			catch (Exception e) {
				e.printStackTrace(System.out);
			}
			
			//	finally ...
			return doc;
		}
		
		abstract ImDocument loadDocument(ImDocument imDoc, Document pdfDoc, byte[] bytes, ProgressMonitor pm) throws IOException;
		
		abstract String getCachePrefix();
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGate.plugins.MonitorableDocumentFormat#loadDocument(java.io.Reader, de.uka.ipd.idaho.gamta.util.ProgressMonitor)
		 */
		public MutableAnnotation loadDocument(Reader source, ProgressMonitor pm) throws IOException {
			throw new IOException("Cannot load binary file format through char-based Reader, please notify your system developer.");
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat#accept(java.lang.String)
		 */
		public boolean accept(String fileName) {
			return ((fileName != null) && fileName.toLowerCase().endsWith(".pdf"));
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat#getDefaultSaveFileExtension()
		 */
		public String getDefaultSaveFileExtension() {
			return "pdf";
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat#equals(de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat)
		 */
		public boolean equals(DocumentFormat format) {
			return ((format != null) && this.getClass().getName().equals(format.getClass().getName()));
		}
	}
	
	private class GenericPdfDocumentFormat extends PdfDocumentFormat {
		GenericPdfDocumentFormat() {
			super(genericPdfDocumentFormatName, "PDF Documents");
		}
		String getCachePrefix() {
			return "";
		}
		ImDocument loadDocument(ImDocument imDoc, Document pdfDoc, byte[] bytes, ProgressMonitor pm) throws IOException {
			return pdfExtractor.loadGenericPdf(imDoc, pdfDoc, bytes, 1, pm);
		}
	}
	
	private class TextPdfDocumentFormat extends PdfDocumentFormat {
		TextPdfDocumentFormat() {
			super(textPdfDocumentFormatName, "PDF Documents (Text Based)");
		}
		String getCachePrefix() {
			return "T/";
		}
		ImDocument loadDocument(ImDocument imDoc, Document pdfDoc, byte[] bytes, ProgressMonitor pm) throws IOException {
			return pdfExtractor.loadTextPdf(imDoc, pdfDoc, bytes, pm);
		}
	}
	
	private class ImagePdfDocumentFormat extends PdfDocumentFormat {
		ImagePdfDocumentFormat() {
			super(imagePdfDocumentFormatName, "PDF Documents (Image Based)");
		}
		String getCachePrefix() {
			return "I/";
		}
		ImDocument loadDocument(ImDocument imDoc, Document pdfDoc, byte[] bytes, ProgressMonitor pm) throws IOException {
			return pdfExtractor.loadImagePdf(imDoc, pdfDoc, bytes, 1, pm);
		}
	}
	
	private class PageOnlyImagePdfDocumentFormat extends PdfDocumentFormat {
		PageOnlyImagePdfDocumentFormat() {
			super(pageOnlyImagePdfDocumentFormatName, "PDF Documents (Image Based), pages only");
		}
		String getCachePrefix() {
			return "P/";
		}
		ImDocument loadDocument(ImDocument imDoc, Document pdfDoc, byte[] bytes, ProgressMonitor pm) throws IOException {
			return pdfExtractor.loadImagePdfPages(imDoc, pdfDoc, bytes, 1, pm);
		}
	}
	
	private class BlocksOnlyImagePdfDocumentFormat extends PdfDocumentFormat {
		BlocksOnlyImagePdfDocumentFormat() {
			super(blocksOnlyImagePdfDocumentFormatName, "PDF Documents (Image Based), text blocks only");
		}
		String getCachePrefix() {
			return "B/";
		}
		ImDocument loadDocument(ImDocument imDoc, Document pdfDoc, byte[] bytes, ProgressMonitor pm) throws IOException {
			return pdfExtractor.loadImagePdfBlocks(imDoc, pdfDoc, bytes, 1, pm);
		}
	}
	
	//	TODO to facilitate rendering preview with IcePDF, remove any JPEG2000 images and, if given, replace them with their masks
	
//	private String getPageImageName(String docId, int pageId) {
//		return (docId + "." + getPageIdString(pageId, 4) + "." + IMAGE_FORMAT);
//	}
//	private static String getPageIdString(int pn, int length) {
//		String pns = ("" + pn);
//		while (pns.length() < length)
//			pns = ("0" + pns);
//		return pns;
//	}
//	
	private static MessageDigest checksumDigester = null;
	private static String getChecksum(byte[] pdfBytes) {
		if (checksumDigester == null) {
			try {
				checksumDigester = MessageDigest.getInstance("MD5");
			}
			catch (NoSuchAlgorithmException nsae) {
				System.out.println(nsae.getClass().getName() + " (" + nsae.getMessage() + ") while creating checksum digester.");
				nsae.printStackTrace(System.out); // should not happen, but Java don't know ...
				return Gamta.getAnnotationID(); // use random value so a document is regarded as new
			}
		}
		checksumDigester.reset();
		InputStream is = new ByteArrayInputStream(pdfBytes);
		try {
			byte[] buffer = new byte[1024];
			int read;
			while ((read = is.read(buffer)) != -1)
				checksumDigester.update(buffer, 0, read);
		}
		catch (IOException ioe) {
			System.out.println(ioe.getClass().getName() + " (" + ioe.getMessage() + ") while computing document checksum.");
			ioe.printStackTrace(System.out); // should not happen, but Java don't know ...
			return Gamta.getAnnotationID(); // use random value so a document is regarded as new
		}
		byte[] checksumBytes = checksumDigester.digest();
		String checksum = new String(RandomByteSource.getHexCode(checksumBytes));
		return checksum;
	}
//	
//	/**
//	 * @param args
//	 */
//	public static void main(String[] args) throws Exception {
//		try {
//			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
//		} catch (Exception e) {}
//		
//		File dataPath = new File("E:/Testdaten/PdfExtract/");
//		PdfDocumentFormatter pdf = new PdfDocumentFormatter();
//		pdf.setDataProvider(new PluginDataProviderFileBased(dataPath));
//		pdf.init();
//		DocumentFormat df = pdf.getFormatForName(genericPdfDocumentFormatName);
//		
//		String pdfName;
//		pdfName = "abcofevolution00mcca.pdf"; // JPX, JBIG2
//		pdfName = "ants_02732.pdf"; // Flate
//		pdfName = "SCZ634_Cairns_web_FINAL.pdf"; // more Flate
////		pdfName = "ObjectTest.pdf";
//		pdfName = "5834.pdf"; // CCITTFaxDecode, multi-line dictionaries
//		pdfName = "21330.pdf"; // CCITTFaxDecode
////		pdfName = "zt02879p040.pdf";
////		pdfName = "23416.pdf";
////		pdfName = "Test600DPI.pdf";
//		File pdfFile = new File(dataPath, pdfName);
//		FileInputStream pdfIn = new FileInputStream(pdfFile);
//		MutableAnnotation doc = df.loadDocument(pdfIn);
//		pdfIn.close();
//		AnnotationUtils.writeXML(doc, new OutputStreamWriter(System.out));
//	}
}
