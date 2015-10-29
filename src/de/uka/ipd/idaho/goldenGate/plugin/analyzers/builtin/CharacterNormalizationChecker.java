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
 *     * Neither the name of the Universit�t Karlsruhe (TH) nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY UNIVERSIT�T KARLSRUHE (TH) / KIT AND CONTRIBUTORS 
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
package de.uka.ipd.idaho.goldenGate.plugin.analyzers.builtin;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Properties;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.MutableAnnotation;

/**
 * This analyzer checks the original value attribute of normalized tokens. In
 * particular, it restores special characters that have been broken by encoding
 * problems (storing as UTF-8 and loading as Cp1252), and it completes original
 * values of tokens normalized before being de-hyphenated.
 * 
 * @author sautter
 */
public class CharacterNormalizationChecker extends CharacterNormalizationAnalyzer {
	private static final String bustedRegEx = ".*[\\Â\\Ã\\Å].*"; // TODO extend this
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.Analyzer#process(de.uka.ipd.idaho.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		
		//	get normalized tokens
		Annotation[] normalizedTokens = data.getAnnotations(CharacterNormalization.NORMALIZED_TOKEN_ANNOTATION_TYPE);
		
		//	check original values for UTF-8 vs. Cp1252 errors
		for (int t = 0; t < normalizedTokens.length; t++) {
			String value = normalizedTokens[t].getValue();
			String originalValue = ((String) normalizedTokens[t].getAttribute(CharacterNormalization.ORIGINAL_VALUE_ATTRIBUTE));
			if (originalValue == null)
				continue;
			if (!originalValue.matches(bustedRegEx))
				continue;
			System.out.println("Busted original value in '" + value + "': " + originalValue);
			String restoredOriginal = decode(originalValue, value);
			System.out.println("==> restored original value of " + value + " as " + restoredOriginal);
			normalizedTokens[t].setAttribute(CharacterNormalization.ORIGINAL_VALUE_ATTRIBUTE, restoredOriginal);
		}
		
		//	check de-hyphenation errors
		for (int t = 0; t < normalizedTokens.length; t++) {
			String value = normalizedTokens[t].getValue();
			String originalValue = ((String) normalizedTokens[t].getAttribute(CharacterNormalization.ORIGINAL_VALUE_ATTRIBUTE));
			if (originalValue == null)
				continue;
			if (!originalValue.endsWith("-"))
				continue;
			String normalizedOriginal = this.characterNormalization.getNormalizedValue(originalValue.substring(0, (originalValue.length()-1)));
			if (value.equals(normalizedOriginal))
				continue;
			System.out.println("Broken original value in '" + value + "': " + originalValue);
			if (value.startsWith(normalizedOriginal)) {
				String restoredOriginal = (originalValue.substring(0, (originalValue.length() - 1)) + value.substring(normalizedOriginal.length()));
				System.out.println("==> restored original value of " + value + " as " + restoredOriginal);
				normalizedTokens[t].setAttribute(CharacterNormalization.ORIGINAL_VALUE_ATTRIBUTE, restoredOriginal.toString());
			}
		}
	}
	
	private static String decode(String code, String mapped) {
		if (!code.matches(bustedRegEx))
			return code;
		if ((code.length() < mapped.length()) && !code.endsWith("-"))
			return code;
		if (mapped.equals(code))
			return code;
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			Writer w = new OutputStreamWriter(baos, "Cp1252");
			w.write(code);
			w.flush();
			w.close();
			ByteArrayInputStream bios = new ByteArrayInputStream(baos.toByteArray());
			BufferedReader br = new BufferedReader(new InputStreamReader(bios, "UTF-8"));
			String decoded = br.readLine();
			br.close();
			System.out.println("Decoded '" + code + "' to '" + decoded + "'");
			return decode(decoded, mapped);
		}
		catch (IOException ioe) {
			System.out.println("Error in decoding '" + code + "': " + ioe.getMessage());
			return mapped;
		}
	}
//	
//	//	TEST ONLY
//	public static void main(String[] args) throws Exception {
////		for (int c = 0; c < Character.MAX_VALUE / 2; c++) {
////			if (Character.isLetter((char) c)) {
//////				String cs;
//////				if (Character.isLowerCase(((char) c)))
//////					cs = "LC";
//////				else if (Character.isUpperCase(((char) c)))
//////					cs = "UC";
//////				else cs = "OC";
//////				System.out.println("letter (" + cs + "): " + ((char) c) + " (" + c + ")");
////			}
////			else if (Character.isDigit((char) c)) {
//////				System.out.println("digit: " + ((char) c) + " (" + c + ")");
////			}
////			else if (Character.isDefined((char) c))
////				System.out.println("other: " + ((char) c) + " (" + c + ")");
////		}
////		if (true)
////			return;
//		
//		
//		MutableAnnotation doc = Gamta.newDocument(Gamta.newTokenSequence("verhältniß—\nmäßig", Gamta.INNER_PUNCTUATION_TOKENIZER));
//		doc.addAnnotation(MutableAnnotation.PARAGRAPH_TYPE, 0, doc.size());
//		
//		AnnotationUtils.writeXML(doc, new OutputStreamWriter(System.out));
//		System.out.println();
////		
////		CharacterNormalizer cn = new CharacterNormalizer();
////		cn.setDataProvider(new AnalyzerDataProviderFileBased(new File("E:/GoldenGATEv3/Plugins/AnalyzerData/Analyzer.builtinData/")));
////		cn.process(doc, new Properties());
////		
////		AnnotationUtils.writeXML(doc, new OutputStreamWriter(System.out));
////		System.out.println();
//		
//		Gamta.normalizeParagraphStructure(doc);
//		
//		AnnotationUtils.writeXML(doc, new OutputStreamWriter(System.out));
//		System.out.println();
//		
//		CharacterNormalizer cn = new CharacterNormalizer();
//		cn.setDataProvider(new AnalyzerDataProviderFileBased(new File("E:/GoldenGATEv3/Plugins/AnalyzerData/Analyzer.builtinData/")));
//		cn.process(doc, new Properties());
//		
//		AnnotationUtils.writeXML(doc, new OutputStreamWriter(System.out));
//		System.out.println();
//		
//		CharacterNormalizationChecker cnc = new CharacterNormalizationChecker();
//		cnc.setDataProvider(new AnalyzerDataProviderFileBased(new File("E:/GoldenGATEv3/Plugins/AnalyzerData/Analyzer.builtinData/")));
//		cnc.process(doc, new Properties());
//		
//		AnnotationUtils.writeXML(doc, new OutputStreamWriter(System.out));
//		System.out.println();
//	}
//	//	TEST ONLY
//	public static void main(String[] args) throws Exception {
//		MutableAnnotation doc = SgmlDocumentReader.readDocument(new InputStreamReader(new URL("http://plazi.cs.umb.edu/GgServer/xslt/794CFC45B589F52AF3F79EA2FA4DEBC3").openStream(), "UTF-8"));
////		
////		//	break document for testing
////		Annotation[] normalizedTokens = doc.getAnnotations(CharacterNormalization.NORMALIZED_TOKEN_ANNOTATION_TYPE);
////		for (int t = 0; t < normalizedTokens.length; t++) {
////			String originalValue = ((String) normalizedTokens[t].getAttribute(CharacterNormalization.ORIGINAL_VALUE_ATTRIBUTE));
////			if (originalValue.matches(bustedRegEx))
////				continue;
////			normalizedTokens[t].setAttribute(CharacterNormalization.ORIGINAL_VALUE_ATTRIBUTE, encode(originalValue));
////		}
////		
//		CharacterNormalizationChecker cnc = new CharacterNormalizationChecker();
//		cnc.setDataProvider(new AnalyzerDataProviderFileBased(new File("E:/GoldenGATEv3/Plugins/AnalyzerData/Analyzer.builtinData/")));
//		cnc.process(doc, new Properties());
//	}
//	private static String encode(String plain) {
//		try {
//			ByteArrayOutputStream baos = new ByteArrayOutputStream();
//			Writer w = new OutputStreamWriter(baos, "UTF-8");
//			w.write(plain);
//			w.flush();
//			w.close();
//			ByteArrayInputStream bios = new ByteArrayInputStream(baos.toByteArray());
//			BufferedReader br = new BufferedReader(new InputStreamReader(bios, "Cp1252"));
//			String encoded = br.readLine();
//			br.close();
//			return encoded;
//		}
//		catch (IOException ioe) {
//			return plain;
//		}
//	}
}
