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
package de.uka.ipd.idaho.goldenGate.plugin.analyzers.builtin;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.TreeSet;

import de.uka.ipd.idaho.gamta.util.AnalyzerDataProvider;
import de.uka.ipd.idaho.stringUtils.StringUtils;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * Central data holder for character normalization matters.
 * 
 * @author sautter
 */
public class CharacterNormalization {
	
	static final String NORMALIZED_TOKEN_ANNOTATION_TYPE = "normalizedToken";
	static final String ORIGINAL_VALUE_ATTRIBUTE = "originalValue";
	
	private static final String staticMappingFileName = "characterMappings.static.txt";
	private static final String customMappingFileName = "characterMappings.custom.txt";
	private static final String unAnnotatedMappingFileName = "characterMappings.unannotated.txt";
	private static final String unmappedCharactersFileName = "characterMappings.unmapped.txt";
	
	private static final HashMap instances = new HashMap();
	public static CharacterNormalization getInstance(AnalyzerDataProvider adp) {
		CharacterNormalization cn = ((CharacterNormalization) instances.get(adp));
		if (cn == null) {
			if (adp == null)
				return null;
			cn = new CharacterNormalization(adp);
			instances.put(adp, cn);
		}
		return cn;
	}
	
	StringVector unAnnotatedMappings = new StringVector();
	StringVector unmappedCharacters = new StringVector();
	
//	private int maxMappingLength = 0; // TODO consider mapping multiple characters at once
	private Properties mappings = new Properties();
	private boolean mappingsDirty = false;
	
	private AnalyzerDataProvider adp;
	
	private CharacterNormalization(AnalyzerDataProvider adp) {
		this.adp = adp;
		
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(this.adp.getInputStream(staticMappingFileName), "UTF-8"));
			String line;
			while ((line = br.readLine()) != null) {
				if (line.startsWith("//"))
					continue;
				int split = line.indexOf(' ');
				if (split == -1)
					continue;
				String original = line.substring(0, split);
				String substitute = line.substring(split+1).trim();
//				if ((original.length() != 0) && (substitute.length() != 0)) {
				if (original.length() != 0) { // we have to accept empty transcripts, as they occur with Russian 'escape characters'
					this.mappings.setProperty(original, substitute);
//					maxMappingLength = Math.max(maxMappingLength, original.length()); // TODO use this line once multi-character mappings are enabled
				}
			}
			br.close();
		}
		catch (Exception e) {
			System.out.println(e.getClass().getName() + " (" + e.getMessage() + ") while initializing Character Normalization.");
		}
		
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(this.adp.getInputStream(customMappingFileName), "UTF-8"));
			String line;
			while ((line = br.readLine()) != null) {
				if (line.startsWith("//"))
					continue;
				int split = line.indexOf(' ');
				if (split == -1)
					continue;
				String original = line.substring(0, split);
				String substitute = line.substring(split+1).trim();
//				if ((original.length() != 0) && (substitute.length() != 0)) {
				if (original.length() != 0) { // we have to accept empty transcripts, as they occur with Russian 'escape characters'
					this.mappings.setProperty(original, substitute);
//					maxMappingLength = Math.max(maxMappingLength, original.length()); // TODO use this line once multi-character mappings are enabled
				}
			}
			br.close();
		}
		catch (Exception e) {
			System.out.println(e.getClass().getName() + " (" + e.getMessage() + ") while initializing Character Normalization.");
		}
		
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(this.adp.getInputStream(unAnnotatedMappingFileName), "UTF-8"));
			while (br.ready()) {
				String line = br.readLine().trim();
				if (line.startsWith("//"))
					continue;
				this.unAnnotatedMappings.addElementIgnoreDuplicates(line.trim());
			}
			br.close();
		}
		catch (Exception e) {
			System.out.println(e.getClass().getName() + " (" + e.getMessage() + ") while initializing Character Normalization.");
		}
		
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(this.adp.getInputStream(unmappedCharactersFileName), "UTF-8"));
			while (br.ready()) {
				String line = br.readLine().trim();
				if (line.startsWith("//"))
					continue;
				this.unmappedCharacters.addElementIgnoreDuplicates(line.trim());
			}
			br.close();
		}
		catch (Exception e) {
			System.out.println(e.getClass().getName() + " (" + e.getMessage() + ") while initializing Character Normalization.");
		}
	}
	
	synchronized void exit() {
		if (this.adp == null)
			return;
		this.storeMappings();
		this.adp = null;
	}
	
	private void storeMappings() {
		if (!this.mappingsDirty)
			return;
		
		try {
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(this.adp.getOutputStream(customMappingFileName), "UTF-8"));
			TreeSet keys = new TreeSet(this.mappings.keySet());
			for (Iterator kit = keys.iterator(); kit.hasNext();) {
				String original = ((String) kit.next());
				String substitute = this.mappings.getProperty(original);
				if ((original.length() != 0) && (substitute != null) && (substitute.length() != 0)) {
					bw.write(original + " " + substitute);
					bw.newLine();
				}
			}
			bw.flush();
			bw.close();
			this.mappingsDirty = false;
		}
		catch (Exception e) {
			System.out.println(e.getClass().getName() + " (" + e.getMessage() + ") while shutting down Character Normalization.");
		}
	}
	
	boolean learnMappings(String originalValue, String normalizedValue) {
		System.out.println("Learning mappings: " + originalValue + " to " + normalizedValue);
		boolean learned = false;
		
		//	find way back
		int[] es = StringUtils.getLevenshteinEditSequence(originalValue, normalizedValue, true);
		StringBuffer normalized = new StringBuffer();
		StringBuffer original = new StringBuffer();
		int vIndex = 0;
		int ovIndex = 0;
		for (int e = 0; e < es.length; e++) {
			
			//	matching point
			if (es[e] == StringUtils.LEVENSHTEIN_KEEP) {
				
				//	we're in safe territory, finish this mapping and decode original
				if ((original.length() + normalized.length()) != 0) {
					if (this.learnMapping(original.toString(), normalized.toString()))
						learned = true;
					original = new StringBuffer();
					normalized = new StringBuffer();
				}
				
				//	keep current character
				vIndex++;
				ovIndex++;
				continue;
			}
			
			//	deletion, extend original
			else if (es[e] == StringUtils.LEVENSHTEIN_DELETE) {
				original.append(originalValue.charAt(ovIndex));
				ovIndex++;
			}
			
			//	insertion, extend mapped
			else if (es[e] == StringUtils.LEVENSHTEIN_INSERT) {
				normalized.append(normalizedValue.charAt(vIndex));
				vIndex++;
			}
			
			//	replacement, extend both original and mapped
			else if (es[e] == StringUtils.LEVENSHTEIN_REPLACE) {
				original.append(originalValue.charAt(ovIndex));
				normalized.append(normalizedValue.charAt(vIndex));
				vIndex++;
				ovIndex++;
			}
		}
		
		//	finish any open mapping and decode original
		if ((original.length() + normalized.length()) != 0) {
			if (this.learnMapping(original.toString(), normalized.toString()))
				learned = true;
		}
		
		//	... finally
		return learned;
	}
	
	boolean learnMapping(String original, String normalized) {
//		if ((original == null) || (original.length() == 0)) // TODO use this condition once multi-character mappings are enabled
		System.out.println("Learning mapping: " + original + " to " + normalized);
		if ((original == null) || (original.length() != 1) || (normalized == null) || (normalized.length() == 0)) {
			System.out.println("==> original too long or short, or normalized too short");
			return false;
		}
		if (original.equals(normalized)) {
			System.out.println("==> original and normalized equal");
			return false;
		}
		if (!original.toLowerCase().equals(original) && !normalized.toLowerCase().equals(normalized) && (normalized.length() > 1))
			normalized = StringUtils.capitalize(normalized);
		String knownMapping = this.mappings.getProperty(original);
		if ((knownMapping != null) && knownMapping.equals(normalized)) {
			System.out.println("==> mapping already known");
			return false;
		}
		this.mappings.setProperty(original, normalized);
		this.mappingsDirty = true;
		System.out.println("==> learned mapping: " + original + " to " + normalized);
		return true;
	}
	
	boolean hasUnNormalizedLetters(String str) {
		boolean unNormalizedLetter = false;
		for (int c = 0; c < str.length(); c++) {
			char ch = str.charAt(c);
			unNormalizedLetter = (unNormalizedLetter || this.isUnNormalizedLetter(ch));
		}
		return unNormalizedLetter;
	}
	
	boolean isUnNormalizedLetter(char ch) {
		return ((ch > 127) && Character.isLetter(ch) && !this.unmappedCharacters.contains("" + ch));
	}
	
	String getUnmappedLetters() {
		return this.unmappedCharacters.concatStrings("");
	}
	
	String getNormalizedValue(String originalValue) {
		StringBuffer normalizedValue = new StringBuffer();
		for (int c = 0; c < originalValue.length(); c++) {
			String str = originalValue.substring(c, (c+1));
			String normalizedStr = this.mappings.getProperty(str);
			
			//	current letter not normalized to anything but itself, simply append it
			if ((normalizedStr == null) || str.equals(normalizedStr)) {
				normalizedValue.append(str);
				continue;
			}
			
			//	normalized value has upper case letters, and is longer than 1, take care of capitalization
			if (!normalizedStr.equals(normalizedStr.toLowerCase()) && (normalizedStr.length() > 1)) {
				boolean upper = true;
				
				//	check character before (if any)
				if (c != 0) {
					String lastNormalizedStr = normalizedValue.substring(normalizedValue.length() - 1);
					
					//	character before in lower case, keep camel case
					if (lastNormalizedStr.equals(lastNormalizedStr.toLowerCase()))
						upper = false;
				}
				
				//	check character after (if any)
				if ((c+1) < originalValue.length()) {
					String nextStr = originalValue.substring((c+1), (c+2));
					
					//	character before in lower case, keep camel case
					if (nextStr.equals(nextStr.toLowerCase()))
						upper = false;
				}
				
				//	we're in all upper case, follow suite
				if (upper)
					normalizedStr = normalizedStr.toUpperCase();
			}
			
			//	append what we've got
			normalizedValue.append(normalizedStr);
		}
		
		//	... finally
		return normalizedValue.toString();
	}
}