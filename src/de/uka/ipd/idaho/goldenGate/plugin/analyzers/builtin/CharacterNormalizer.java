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


import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Iterator;
import java.util.Properties;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.TokenSequence;
import de.uka.ipd.idaho.gamta.TokenSequenceUtils;
import de.uka.ipd.idaho.gamta.util.constants.LiteratureConstants;
import de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * This analyzer normalized characters to their ASCII-7 base form to simplify
 * using and managing regular expression patterns. It annotates the tokens it
 * has normalized; the un-normalized original values are preserved as attributes
 * to these annotations.
 * 
 * @author sautter
 */
public class CharacterNormalizer extends CharacterNormalizationAnalyzer implements LiteratureConstants {
	
	//	NO NEED FOR CARING ABOUT PAGE BREAKS & CO, AS CHARACTER NORMALIZATION AT MAX DRAGS MORE CHARACTERS INTO THE TOKEN ALREADY ANNOTATED AS A PAGE BREAK
	
	/** @see de.uka.ipd.idaho.gamta.util.Analyzer#process(de.uka.ipd.idaho.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		this.normalize(data, parameters, null);
	}
	private void normalize(MutableAnnotation data, Properties parameters, Properties userInput) {
		TreeSet forUserInput = new TreeSet();
		
		//	do normalization
		for (int v = 0; v < data.size(); v++) {
			String originalValue = data.valueAt(v);
			String normalizedValue = this.characterNormalization.getNormalizedValue(originalValue);
			
			//	nothing normalized or to normalize, we're done here
			if (originalValue.equals(normalizedValue) && !this.characterNormalization.hasUnNormalizedLetters(normalizedValue))
				continue;
			
			//	extend to whitespace-bounded block
			int s = v;
			while ((s != 0) && (data.getWhitespaceAfter(s-1).length() == 0))
				s--;
			int e = v + 1;
			while ((e < data.size()) && (data.getWhitespaceAfter(e-1).length() == 0))
				e++;
			
			//	normalize whole block
			String originalBlock = TokenSequenceUtils.concatTokens(data, s, (e - s), false, false);
			String normalizedBlock;
			
			//	no user input so far, try automatic normalization
			if (userInput == null) {
				normalizedBlock = this.characterNormalization.getNormalizedValue(originalBlock);
				
				//	check for unknown special characters
				if (this.characterNormalization.hasUnNormalizedLetters(normalizedBlock)) {
					
					//	ask user for transcript if allowed to
					if (parameters.containsKey(INTERACTIVE_PARAMETER)) {
						forUserInput.add(originalBlock);
						continue;
					}
				}
			}
			
			//	got user input
			else {
				normalizedBlock = userInput.getProperty(originalBlock);
				
				//	... but not for this one
				if (normalizedBlock == null)
					continue;
			}
			
			//	tokenize normalized block to change only what requires changing
			TokenSequence normalizedTokens = data.getTokenizer().tokenize(normalizedBlock);
			
			//	remember offsets
			int so = data.tokenAt(s).getStartOffset();
			int eo = data.tokenAt(e-1).getEndOffset();
			
			//	narrow boundaries, and store token attributes
			int nts = 0;
			while ((s < v) && (nts < normalizedTokens.size()) && (data.valueAt(s).equals(normalizedTokens.valueAt(nts)))) {
				normalizedTokens.tokenAt(nts).copyAttributes(data.tokenAt(s));
				s++;
				nts++;
			}
			int nte = normalizedTokens.size();
			while ((e > v) && (nte > 1) && (data.valueAt(e-1).equals(normalizedTokens.valueAt(nte-1)))) {
				normalizedTokens.tokenAt(nte-1).copyAttributes(data.tokenAt(e-1));
				e--;
				nte--;
			}
			
			//	catch empty change input
			if ((nte <= nts) || (e <= s))
				continue;
			
			//	store attributes of tokens we actually have to change (if normalization mapping is expansive (ASCII fractions !!!), use last original token (usually a single token anyway))
			for (int t = nts; t < nte; t++)
				normalizedTokens.tokenAt(t).copyAttributes(data.tokenAt(Math.min((s-nts+t), (e-1))));
			
			//	remember original
			String original = TokenSequenceUtils.concatTokens(data, s, (e-s), false, true);
			System.out.println("Original tokens are: " + TokenSequenceUtils.concatTokens(data, s, (e-s), true, true));
			
			//	replace what's necessary
			try {
				data.setChars(TokenSequenceUtils.concatTokens(normalizedTokens, false, false), so, (eo-so));
			}
			//	 catch runtime exceptions that might be thrown by data model implementations if number of tokens changes
			catch (RuntimeException re) {
				re.printStackTrace(System.out);
				continue;
			}
			
			//	mark what we just did
			Annotation normalized = data.addAnnotation(CharacterNormalization.NORMALIZED_TOKEN_ANNOTATION_TYPE, s, (nte-nts));
			normalized.setAttribute(CharacterNormalization.ORIGINAL_VALUE_ATTRIBUTE, original);
			
			//	restore token attributes
			for (int t = 0; t < normalizedTokens.size(); t++)
				data.tokenAt(s-nts+t).copyAttributes(normalizedTokens.tokenAt(t));
			
			//	learn
			if (userInput != null)
				this.characterNormalization.learnMappings(original, normalized.getValue());
		}
		
		//	get user input and recurse if allowed and required
		if (forUserInput.size() != 0) {
			Properties theUserInput = getTranscripts(forUserInput);
			if (theUserInput != null)
				this.normalize(data, parameters, theUserInput);
		}
		
		//	we're in the recursive invocation, don't do the cleanup here
		if (userInput != null)
			return;
		
		//	filter specific normalized token annotations
		Annotation[] normalizedTokens = data.getAnnotations(CharacterNormalization.NORMALIZED_TOKEN_ANNOTATION_TYPE);
		for (int n = 0; n < normalizedTokens.length; n++) {
			if (this.characterNormalization.unAnnotatedMappings.contains(normalizedTokens[n].getValue()))
				data.removeAnnotation(normalizedTokens[n]);
		}
	}
	
	private Properties getTranscripts(TreeSet forUserInput) {
		
		//	collect special characters and their occurrences
		TreeMap specialChars = new TreeMap();
		for (Iterator oit = forUserInput.iterator(); oit.hasNext();) {
			String original = ((String) oit.next());
			for (int c = 0; c < original.length(); c++) {
				char ch = original.charAt(c);
				if (this.characterNormalization.isUnNormalizedLetter(ch)) {
					String sc = ("" + ch);
					TreeSet occurrences = ((TreeSet) specialChars.get(sc));
					if (occurrences == null) {
						occurrences = new TreeSet();
						specialChars.put(sc, occurrences);
					}
					occurrences.add(original);
				}
			}
		}
		
		//	build feedback panel
		TranscriptFeedbackPanel tfp = new TranscriptFeedbackPanel(this.characterNormalization.getUnmappedLetters());
		for (Iterator scit = specialChars.keySet().iterator(); scit.hasNext();) {
			String sc = ((String) scit.next());
			TreeSet occurrences = ((TreeSet) specialChars.get(sc));
			tfp.addLine(sc, occurrences);
		}
		tfp.addButton("OK");
		tfp.addButton("Cancel");
		
		//	get feedback
		String f = tfp.getFeedback();
		if ("Cancel".equalsIgnoreCase(f))
			return null;
		
		//	process feedback
		Properties userInput = new Properties();
		for (Iterator oit = forUserInput.iterator(); oit.hasNext();) {
			String original = ((String) oit.next());
			StringBuffer transcript = new StringBuffer();
			boolean transcriptDiffers = false;
			for (int c = 0; c < original.length(); c++) {
				char ch = original.charAt(c);
				if (this.characterNormalization.isUnNormalizedLetter(ch)) {
					String tCh = tfp.getTranscript("" + ch);
					if (tCh == null)
						transcript.append(ch);
					else {
						transcript.append(tCh);
						if ((tCh.length() != 1) || (ch != tCh.charAt(0)))
							transcriptDiffers = true;
					}
				}
				else transcript.append(ch);
			}
			if (transcriptDiffers)
				userInput.setProperty(original, transcript.toString());
		}
		return userInput;
	}
	
	/** this class is public only to facilitate remote class loading */
	public static class TranscriptFeedbackPanel extends FeedbackPanel {
		private TreeMap lines = new TreeMap();
		private JPanel spacer = new JPanel();
		private String unmappedCharacters;
		TranscriptFeedbackPanel() {
			this("");
		}
		TranscriptFeedbackPanel(String unmappedCharacters) {
			super("Enter ASCII-7 Transcript");
			this.unmappedCharacters = unmappedCharacters;
			this.setLayout(new GridBagLayout());
			this.setLabel("<HTML>Please provide ASCII-7 transcripts for the special characters listed below." +
							"<BR>This can be simply the base character of a diacritic, or the two or more" +
							"<BR>characters a ligature is formed from, or a two-character transcript of a German" +
							"<BR>umlaut, or something totally different." +
							"<BR>To leave a character unnormalized, check the 'Leave as is' box." +
							"<BR>Hover the mouse over the characters on the left to see words they occur in." +
						"</HTML>");
		}
		
		void addLine(String original, TreeSet occurrences) {
			this.lines.put(original, new TranscriptLine(original, occurrences));
			this.layoutLines();
		}
		
		private void layoutLines() {
			this.removeAll();
			
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.insets.top = 2;
			gbc.insets.bottom = 2;
			gbc.insets.left = 3;
			gbc.insets.right = 3;
			gbc.weighty = 0;
			gbc.gridy = 0;
			gbc.gridheight = 1;
			gbc.gridwidth = 1;
			gbc.fill = GridBagConstraints.BOTH;
			
			for (Iterator lit = this.lines.values().iterator(); lit.hasNext();) {
				TranscriptLine tl = ((TranscriptLine) lit.next());
				gbc.gridx = 0;
				gbc.weightx = 0;
				this.add(tl.original, gbc.clone());
				gbc.gridx = 1;
				gbc.weightx = 1;
				this.add(tl.transcript, gbc.clone());
				gbc.gridx = 2;
				gbc.weightx = 0;
				this.add(tl.noTransscript, gbc.clone());
				gbc.gridy++;
			}
			
			gbc.gridx = 0;
			gbc.weightx = 1;
			gbc.weighty = 1;
			gbc.gridwidth = 3;
			this.add(this.spacer, gbc.clone());
			
			this.validate();
			this.repaint();
		}
		
		String getTranscript(String original) {
			TranscriptLine tl = ((TranscriptLine) this.lines.get(original));
			return (((tl == null) || tl.noTransscript.isSelected()) ? null : tl.transcript.getText());
		}
		
		private class TranscriptLine {
			JLabel original;
			String originalString;
			JTextField transcript;
			JCheckBox noTransscript;
			boolean transcriptError = true;
			TranscriptLine(String original, TreeSet occurrences) {
				this.originalString = original;
				this.original = new JLabel("<HTML>&nbsp;<B>" + original + "</B>&nbsp;</HTML>");
				StringBuffer toolTip = new StringBuffer("<HTML>");
				for (Iterator oit = occurrences.iterator(); oit.hasNext();) {
					String occurrence = ((String) oit.next());
					if (toolTip.length() > 6)
						toolTip.append("<BR>");
					toolTip.append(occurrence);
				}
				toolTip.append("</HTML>");
				this.original.setToolTipText(toolTip.toString());
				
				this.transcript = new JTextField(original);
				this.transcript.addFocusListener(new FocusAdapter() {
					public void focusGained(FocusEvent fe) {
						transcript.getCaret().setDot(0);
						transcript.getCaret().moveDot(transcript.getText().length());
					}
					public void focusLost(FocusEvent fe) {
						validateTranscript();
					}
				});
				
				this.noTransscript = new JCheckBox("Leave as is");
				this.noTransscript.addItemListener(new ItemListener() {
					public void itemStateChanged(ItemEvent ie) {
						validateTranscript();
					}
				});
				this.validateTranscript();
			}
			private void validateTranscript() {
				String t = this.transcript.getText();
				this.transcriptError = (hasUnNormalizedLetters(t) && !this.noTransscript.isSelected()); // we have to accept empty transcripts, as they occur with Russian 'escape characters'
				this.transcript.setBorder(
						this.transcriptError
						?
						BorderFactory.createLineBorder(Color.ORANGE)
						:
						BorderFactory.createLoweredBevelBorder()
					);
			}
		}
		private boolean hasUnNormalizedLetters(String str) {
			boolean unNormalizedLetter = false;
			for (int c = 0; c < str.length(); c++) {
				char ch = str.charAt(c);
				unNormalizedLetter = (unNormalizedLetter || this.isUnNormalizedLetter(ch));
			}
			return unNormalizedLetter;
		}
		
		private boolean isUnNormalizedLetter(char ch) {
			return ((ch > 127) && Character.isLetter(ch) && (this.unmappedCharacters.indexOf(ch) == -1));
		}
		public String[] checkFeedback(String status) {
			if (!status.startsWith("OK"))
				return null;
			
			StringVector errors = new StringVector();
			for (Iterator lit = this.lines.values().iterator(); lit.hasNext();) {
				TranscriptLine tl = ((TranscriptLine) lit.next());
				if (tl.transcriptError)
					errors.addElement("'" + tl.transcript.getText() + "' is not a valid transcript for '" + tl.originalString + "'");
			}
			return (errors.isEmpty() ? null : errors.toStringArray());
		}
		public int getComplexity() {
			return (this.getDecisionCount() * this.getDecisionComplexity());
		}
		public int getDecisionComplexity() {
			return 5;
		}
		public int getDecisionCount() {
			return this.lines.size();
		}
		
		//	TODOne DO NOT implement remoting methods
		//	THIS IS ONLY FOR TRAINING, AND CONFIG FILES ARE UNAVAILABLE ON REMOTE MACHINE
	}
}