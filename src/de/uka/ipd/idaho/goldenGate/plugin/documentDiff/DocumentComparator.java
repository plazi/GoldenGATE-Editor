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
 *     * Neither the name of the Universität Karlsruhe (TH) / KIT nor the
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
package de.uka.ipd.idaho.goldenGate.plugin.documentDiff;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.Attributed;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.TokenSequence;
import de.uka.ipd.idaho.gamta.TokenSequenceUtils;
import de.uka.ipd.idaho.gamta.util.SgmlDocumentReader;
import de.uka.ipd.idaho.goldenGate.DocumentEditor;
import de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin;
import de.uka.ipd.idaho.goldenGate.util.DialogPanel;
import de.uka.ipd.idaho.stringUtils.StringUtils;

/**
 * @author sautter
 *
 */
public class DocumentComparator extends AbstractGoldenGatePlugin {
	
	public DocumentComparator() {}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#getToolsMenuFunctionItems(de.uka.ipd.idaho.goldenGate.GoldenGateConstants.InvokationTargetProvider)
	 */
	public JMenuItem[] getToolsMenuFunctionItems(final InvokationTargetProvider targetProvider) {
		ArrayList collector = new ArrayList();
		JMenuItem mi;
		
		mi = new JMenuItem("Compare Document ...");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				DocumentEditor target = targetProvider.getFunctionTarget();
				if (target != null)
					doDiff(target);
			}
		});
		collector.add(mi);
		
		return ((JMenuItem[]) collector.toArray(new JMenuItem[collector.size()]));
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#getMainMenuTitle()
	 */
	public String getMainMenuTitle() {
		return "Document Comparator";
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#getMainMenuItems()
	 */
	public JMenuItem[] getMainMenuItems() {
		ArrayList collector = new ArrayList();
		JMenuItem mi;
		
		mi = new JMenuItem("Compare Document ...");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				doDiff(parent.getActivePanel());
			}
		});
		collector.add(mi);
		
		return ((JMenuItem[]) collector.toArray(new JMenuItem[collector.size()]));
	}
	
	void doDiff(DocumentEditor target) {
		if (target == null)
			return;
		
		//	select document to compare to
		DocumentEditor comparison;
		DocumentEditor[] editors = this.parent.getPanels();
		if (editors.length < 2) {
			JOptionPane.showMessageDialog(DialogPanel.getTopWindow(), "This function is avaliable only if two or more documents are open.", "No Document for Comparison", JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		else if (editors.length == 2)
			comparison = ((target == editors[0]) ? editors[1] : editors[0]);
		else {
			final DocumentEditor[] selectedComparison = {null};
			final DialogPanel selectorDialog = new DialogPanel("Select Document for Comparison", true);
			
			//	put non-active documents in tabs for selection
			final JTabbedPane tabSelector = new JTabbedPane();
			final HashMap selectorTabsToEditors = new HashMap();
			for (int e = 0; e < editors.length; e++) {
				if (editors[e] == target)
					continue;
				StringBuffer tabText = new StringBuffer("<html><table>");
				QueriableAnnotation doc = editors[e].getContent();
				String[] attributeNames = doc.getAttributeNames();
				for (int a = 0; a < attributeNames.length; a++) {
					Object attributeValue = doc.getAttribute(attributeNames[a]);
					if (attributeValue != null) {
						tabText.append("<tr>");
						tabText.append("<td>" + AnnotationUtils.escapeForXml(attributeNames[a]) + "</td>");
						tabText.append("<td>" + AnnotationUtils.escapeForXml(attributeValue.toString()) + "</td>");
						tabText.append("</tr>");
					}
				}
				tabText.append("</table></html>");
				JLabel selectorTab = new JLabel(tabText.toString());
				selectorTabsToEditors.put(selectorTab, editors[e]);
				tabSelector.addTab(editors[e].getTitle(), selectorTab);
			}
			
			//	add buttons
			JButton ok = new JButton("Compare");
			ok.setBorder(BorderFactory.createRaisedBevelBorder());
			ok.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					selectedComparison[0] = ((DocumentEditor) selectorTabsToEditors.get(tabSelector.getSelectedComponent()));
					selectorDialog.dispose();
				}
			});
			JButton cancel = new JButton("Cancel");
			cancel.setBorder(BorderFactory.createRaisedBevelBorder());
			cancel.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					selectorDialog.dispose();
				}
			});
			JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER));
			buttons.add(ok);
			buttons.add(cancel);
			
			//	build dialog
			selectorDialog.add(tabSelector, BorderLayout.CENTER);
			selectorDialog.add(buttons, BorderLayout.SOUTH);
			selectorDialog.setSize(800, 600);
			selectorDialog.setLocationRelativeTo(selectorDialog.getOwner());
			
			//	selection cancelled or not?
			if (selectedComparison[0] == null)
				return;
			else comparison = selectedComparison[0];
		}
		
		//	actually do comparison
		this.doDiff(target, comparison);
	}
	
	private void doDiff(DocumentEditor target, DocumentEditor comparison) {
		
		//	get documents
		QueriableAnnotation tDoc = target.getContent();
		QueriableAnnotation cDoc = comparison.getContent();
		
		//	compute differences
		doDiff(tDoc, cDoc);
		
		//	TODO display differences
		
		//	TODO inject document processor into target to write through selected changes
	}
	
	//	TODO return differences list after tests
	private static void doDiff(QueriableAnnotation tDoc, QueriableAnnotation cDoc) {
		
		//	initialize token index mappings, and index tokens, as well as hyphenations
		int[] tDocTokenMappings = new int[tDoc.size()];
		TokenIndexMap tDocTokenIndex = new TokenIndexMap();
		TreeSet tDocHyphenated = new TreeSet();
		for (int t = 0; t < tDocTokenMappings.length; t++) {
			tDocTokenMappings[t] = -1;
			String token = tDoc.valueAt(t);
			tDocTokenIndex.indexAt(token, t);
			String nToken = normalize(token);
			tDocTokenIndex.indexAt(nToken, t);
			if (((t+1) < tDoc.size()) && nToken.matches("[a-zA-Z\\-\\']+\\-")) {
				tDocHyphenated.add(new Integer(t));
				String nextToken = tDoc.valueAt(t+1);
				tDocTokenIndex.indexAt((token.substring(0, (token.length()-1)) + nextToken), t);
				tDocTokenIndex.indexAt((nToken.substring(0, (token.length()-1)) + normalize(nextToken)), t);
			}
		}
		int[] cDocTokenMappings = new int[cDoc.size()];
		TokenIndexMap cDocTokenIndex = new TokenIndexMap();
		TreeSet cDocHyphenated = new TreeSet();
		for (int t = 0; t < cDocTokenMappings.length; t++) {
			cDocTokenMappings[t] = -1;
			String token = cDoc.valueAt(t);
			cDocTokenIndex.indexAt(token, t);
			String nToken = normalize(token);
			cDocTokenIndex.indexAt(nToken, t);
			if (((t+1) < cDoc.size()) && nToken.matches("[a-zA-Z\\-\\']+\\-")) {
				cDocHyphenated.add(new Integer(t));
				String nextToken = cDoc.valueAt(t+1);
				cDocTokenIndex.indexAt((token.substring(0, (token.length()-1)) + nextToken), t);
				cDocTokenIndex.indexAt((nToken.substring(0, (token.length()-1)) + normalize(nextToken)), t);
			}
		}
		
		//	TODO also index token bigrams and trigrams
		
		//	TODO also index token quatrograms and maybe even quintagrams
		
		//	TODO use n-grams occurring exactly once in each document as anchors for partitioning
		
		//	TODO compute mapping between these, using divide and conquer
		
		LinkedList differences = new LinkedList();
		
		//	TODO compute (approximate) Levenshtein edit sequence
		int tIndex = 0;
		int cIndex = 0;
		while ((tIndex < tDoc.size()) || (cIndex < cDoc.size())) {
			
			//	end of target document reached, must be insertion
			if (tIndex == tDoc.size()) {
				differences.add(new TokenDifference(-1, cIndex, null, cDoc.valueAt(cIndex)));
				cIndex++;
				continue;
			}
			
			//	end of comparison document reached, must be deletion
			if (cIndex == cDoc.size()) {
				differences.add(new TokenDifference(tIndex, -1, tDoc.valueAt(tIndex), null));
				tIndex++;
				continue;
			}
			
			//	compute distance to next matching token
			int tMatchIndex = tDocTokenIndex.indexOf(cDoc.valueAt(cIndex), tIndex);
			int cMatchIndex = cDocTokenIndex.indexOf(tDoc.valueAt(tIndex), cIndex);
			
			//	tokens match
			if (tDoc.valueAt(tIndex).equals(cDoc.valueAt(cIndex))) {
				tDocTokenMappings[tIndex] = cIndex;
				cDocTokenMappings[cIndex] = tIndex;
				tIndex++;
				cIndex++;
				continue;
			}
			
			//	no (further) matches in either document, must be replacement
			if ((tMatchIndex == -1) && (cMatchIndex == -1)) {
				differences.add(new TokenDifference(tIndex, cIndex, tDoc.valueAt(tIndex), cDoc.valueAt(cIndex)));
				cIndex++;
				continue;
			}
			
			//	no (further) matches in target document, must be insertion
			else if (tMatchIndex == -1) {
				differences.add(new TokenDifference(-1, cIndex, null, cDoc.valueAt(cIndex)));
				cIndex++;
				continue;
			}
			
			//	no (further) matches in comparison document, must be deletion
			else if (cMatchIndex == -1) {
				differences.add(new TokenDifference(tIndex, -1, tDoc.valueAt(tIndex), null));
				tIndex++;
				continue;
			}
			
			//	suppose we jump forward in comparison document, inserting all tokens in between, what would we have to delete from target document later 
			TreeSet deletions = new TreeSet();
			for (int t = (cIndex+1); t < cMatchIndex; t++) {
				int tLookaheadIndex = tDocTokenIndex.indexOf(cDoc.valueAt(t), (tIndex+1));
				if (tLookaheadIndex != -1)
					deletions.add(new Integer(tLookaheadIndex));
			}
			int deletionDistance = (deletions.isEmpty() ? 0 : (((Integer) deletions.first()).intValue() - tIndex));
			float deletionDensity;
			if (deletions.size() == 0)
				deletionDensity = 0;
			else if (deletions.size() == 1)
				deletionDensity = 1;
			else deletionDensity = (((float) (((Integer) deletions.last()).intValue() - ((Integer) deletions.first()).intValue())) / deletions.size());
			
			//	suppose we jump forward in target document, deleting all tokens in between, what would we have to insert into target document later
			TreeSet insertions = new TreeSet();
			for (int t = (tIndex+1); t < tMatchIndex; t++) {
				int cLookaheadIndex = cDocTokenIndex.indexOf(tDoc.valueAt(t), (cIndex+1));
				if (cLookaheadIndex != -1)
					insertions.add(new Integer(cLookaheadIndex));
			}
			int insertionDistance = (insertions.isEmpty() ? 0 : (((Integer) insertions.first()).intValue() - cIndex));
			float insertionDensity;
			if (insertions.size() == 0)
				insertionDensity = 0;
			else if (insertions.size() == 1)
				insertionDensity = 1;
			else insertionDensity = (((float) (((Integer) insertions.last()).intValue() - ((Integer) insertions.first()).intValue())) / deletions.size());
			
			//	TODO check which displacement is further
			//	both very dense, likely blocks swapped ==> work on smaller one
			if ((deletionDensity > 0.7) && (insertionDensity > 0.7)) {
				
				//	block to insert and later delete closer, and thus likely larger, rather delete smaller block
				if (deletionDistance < insertionDistance) {
					
				}
				
				//	block to delete and later insert closer, and thus likely larger, rather insert smaller block
				else {
					
				}
			}
			
			//	block to insert and later delete very dense, rather delete the in-between
			else if (deletionDensity > 0.7) {
				
			}
			
			//	block to delete and later insert very dense, rather insert the in-between
			else if (insertionDensity > 0.7) {
				
			}
			
			//	neither block very dense, replace
			else {
				
			}
		}
		
		//	TODO diff annotations, figuring in token offsets
		
		//	TODO allow for moving differences left and right
		//	- token changes
		//	- annotations
		//	- annotation attributes
		
		//	TODO use document processor injected into target to make changes
		
		//	TODO use copy of comparison document to ignore changes
		//	BETTER
		//	simply remove objects representing ignored changes
		//	TODO generate Runnable actions from transfered changes on 'Merge'
	}
	
	private static String normalize(String token) {
		StringBuffer nToken = new StringBuffer();
		for (int c = 0; c < token.length(); c++)
			nToken.append(StringUtils.getBaseChar(token.charAt(c)));
		return nToken.toString();
	}
	
	private static class TokenDiffStep implements Comparable {
		final int tIndex;
		final int cIndex;
		final Difference step;
		final int matched;
		final int insertCount;
		final int inserted;
		final int deleteCount;
		final int deleted;
		final int replaced;
		private double score = -1;
		//	for match, replace, insert, and delete
		TokenDiffStep(TokenDifference step, int tIndex, int cIndex) {
			this.step = step;
			//	deletion
			if (step.cToken == null) {
				this.tIndex = this.step.tIndex;
				this.cIndex = cIndex;
				this.matched = 0;
				this.insertCount = 0;
				this.inserted = 0;
				this.deleteCount = 1;
				this.deleted = 1;
				this.replaced = 0;
			}
			//	insertion
			else if (step.tToken == null) {
				this.tIndex = tIndex;
				this.cIndex = this.step.cIndex;
				this.matched = 0;
				this.insertCount = 1;
				this.inserted = 1;
				this.deleteCount = 0;
				this.deleted = 0;
				this.replaced = 0;
			}
			//	match
			else if (step.tToken.matches(step.cToken)) {
				this.tIndex = this.step.tIndex;
				this.cIndex = this.step.cIndex;
				this.matched = 1;
				this.insertCount = 0;
				this.inserted = 0;
				this.deleteCount = 0;
				this.deleted = 0;
				this.replaced = 0;
			}
			//	replacement
			else {
				this.tIndex = this.step.tIndex;
				this.cIndex = this.step.cIndex;
				this.matched = 0;
				this.insertCount = 0;
				this.inserted = 0;
				this.deleteCount = 0;
				this.deleted = 0;
				this.replaced = 1;
			}
		}
		//	for forward matching, either way
		TokenDiffStep(TokenDifference step, int tIndex, int cIndex, int tIndexJump, int cIndexJump) {
			this.step = step;
			this.tIndex = this.step.tIndex;
			this.cIndex = this.step.cIndex;
			this.matched = 1;
			this.insertCount = ((cIndexJump < 1) ? 0 : 1);
			this.inserted = cIndexJump;
			this.deleteCount = ((tIndexJump < 1) ? 0 : 1);
			this.deleted = tIndexJump;
			this.replaced = 0;
		}
		//	for cloning
		private TokenDiffStep(int tIndex, int cIndex, Difference step, int matched, int insertCount, int inserted, int deleteCount, int deleted, int replaced) {
			this.tIndex = tIndex;
			this.cIndex = cIndex;
			this.step = step;
			this.matched = matched;
			this.insertCount = insertCount;
			this.inserted = inserted;
			this.deleteCount = deleteCount;
			this.deleted = deleted;
			this.replaced = replaced;
		}
		TokenDiffStep nextStepMatch() {
			return new TokenDiffStep((this.tIndex+1), (this.cIndex+1), step, (this.matched+1), this.insertCount, this.inserted, this.deleteCount, this.deleted, this.replaced);
		}
		TokenDiffStep nextStepReplace() {
			return new TokenDiffStep((this.tIndex+1), (this.cIndex+1), step, this.matched, this.insertCount, this.inserted, this.deleteCount, this.deleted, (this.replaced+1));
		}
		TokenDiffStep nextStepInsert() {
			return new TokenDiffStep(this.tIndex, (this.cIndex+1), step, this.matched, (this.insertCount+1), (this.inserted+1), this.deleteCount, this.deleted, this.replaced);
		}
		TokenDiffStep nextStepDelete() {
			return new TokenDiffStep((this.tIndex+1), this.cIndex, step, this.matched, this.insertCount, this.inserted, (this.deleteCount+1), (this.deleted+1), this.replaced);
		}
		TokenDiffStep nextStepInsertMatch(int cMatchIndex) {
			return new TokenDiffStep((this.tIndex+1), (cMatchIndex+1), step, (this.matched+1), (this.insertCount+1), (this.inserted + (cMatchIndex - this.cIndex)), this.deleteCount, this.deleted, this.replaced);
		}
		TokenDiffStep nextStepDeleteMatch(int tMatchIndex) {
			return new TokenDiffStep((tMatchIndex+1), (this.cIndex+1), step, (this.matched+1), this.insertCount, this.inserted, (this.deleteCount+1), (this.deleted + (tMatchIndex - this.tIndex)), this.replaced);
		}
		public int compareTo(Object obj) {
			return ((obj instanceof TokenDiffStep) ? Double.compare(((TokenDiffStep) obj).getScore(), this.getScore()) : -1);
		}
		double getScore() {
			if (this.score == -1) {
				//	TODO compute score
			}
			return this.score;
		}
	}
	
	private static TokenIndexMap getTokenNGramIndex(TokenSequence tokens, int n) {
		TokenIndexMap index = new TokenIndexMap();
		for (int t = 0; t <= (tokens.size() - n); t++)
			index.indexAt(TokenSequenceUtils.concatTokens(tokens, t, n, true, true), t);
		return index;
	}
	
	private static class TokenIndexMap extends TreeMap {
		void indexAt(String term, int index) {
			TreeSet termIndexes = ((TreeSet) this.get(term));
			if (termIndexes == null) {
				termIndexes = new TreeSet();
				this.put(term, termIndexes);
			}
			termIndexes.add(new Integer(index));
		}
		int indexOf(String term, int from) {
			TreeSet termIndexes = ((TreeSet) this.get(term));
			if (termIndexes != null)
				for (Iterator tiit = termIndexes.iterator(); tiit.hasNext();) {
					Integer termIndex = ((Integer) tiit.next());
					if (from <= termIndex.intValue())
						return termIndex.intValue();
				}
			return -1;
		}
		int[] indexesOf(String term, int from) {
			TreeSet termIndexes = ((TreeSet) this.get(term));
			if (termIndexes == null)
				return null;
			LinkedList fromTermIndexes = null;
			for (Iterator tiit = termIndexes.iterator(); tiit.hasNext();) {
				Integer termIndex = ((Integer) tiit.next());
				if (from <= termIndex.intValue()) {
					if (fromTermIndexes == null)
						fromTermIndexes = new LinkedList();
					fromTermIndexes.add(termIndex);
				}
			}
			if (fromTermIndexes == null)
				return null;
			int[] indexesOfTerm = new int[fromTermIndexes.size()];
			for (int i = 0; 0 < fromTermIndexes.size(); i++)
				indexesOfTerm[i] = ((Integer) fromTermIndexes.removeFirst()).intValue();
			return indexesOfTerm;
		}
		int occurrenceCountOf(String term, int from) {
			TreeSet termIndexes = ((TreeSet) this.get(term));
			int occurrenceCount = 0;
			if (termIndexes != null)
				for (Iterator tiit = termIndexes.iterator(); tiit.hasNext();) {
					Integer termIndex = ((Integer) tiit.next());
					if (from <= termIndex.intValue())
						occurrenceCount++;
				}
			return occurrenceCount;
		}
		void unIndexAt(String term, int index) {
			TreeSet termIndexes = ((TreeSet) this.get(term));
			if (termIndexes != null)
				termIndexes.remove(new Integer(index));
		}
	}
	
	private static abstract class Difference implements Comparable {
		final int tIndex;
		final int cIndex;
		Difference(int tIndex, int cIndex) {
			this.tIndex = tIndex;
			this.cIndex = cIndex;
		}
		public int compareTo(Object obj) {
			if (!(obj instanceof Difference))
				return -1;
			else if (((Difference) obj).tIndex == this.tIndex)
				return (this.cIndex - ((Difference) obj).cIndex);
			else return (this.tIndex - ((Difference) obj).tIndex);
		}
	}
	
	private static class TokenDifference extends Difference{
		final String tToken;
		final String cToken;
		TokenDifference(int tIndex, int cIndex, String tToken, String cToken) {
			super(tIndex, cIndex);
			this.tToken = tToken;
			this.cToken = cToken;
		}
	}
	
	private static class AnnotationDifference extends Difference{
		final Annotation tAnnot;
		final Annotation cAnnot;
		AnnotationDifference(int tIndex, int cIndex, Annotation tAnnot, Annotation cAnnot) {
			super(tIndex, cIndex);
			this.tAnnot = tAnnot;
			this.cAnnot = cAnnot;
		}
	}
	
	private static class AttributeDifference extends Difference{
		final String attribName;
		final Attributed tAttrib;
		final Attributed cAttrib;
		AttributeDifference(int tIndex, int cIndex, String attribName, Attributed tAttrib, Attributed cAttrib) {
			super(tIndex, cIndex);
			this.attribName = attribName;
			this.tAttrib = tAttrib;
			this.cAttrib = cAttrib;
		}
	}
	
	public static void main(String[] args) throws Exception {
//		MutableAnnotation doc = SgmlDocumentReader.readDocument(new BufferedReader(new InputStreamReader(new FileInputStream(new File("E:/Projektdaten/TaxonxTest/zt03305p052.xml")))));
//		for (int n = 1; n < 8; n++) {
//			TokenIndexMap index = getTokenNGramIndex(doc, n);
//			System.out.println("Got " + index.size() + " distinct " + n + "-grams:");
//			TokenIndexMap uniqueIndex = new TokenIndexMap();
//			for (Iterator tngit = index.keySet().iterator(); tngit.hasNext();) {
//				String nGram = ((String) tngit.next());
//				int[] nGramIndexes = index.indexesOf(nGram, 0);
////				System.out.println(" - " + nGram + " at " + Arrays.toString(nGramIndexes));
//				if (nGramIndexes.length == 1)
//					uniqueIndex.indexAt(nGram, nGramIndexes[0]);
//			}
//			System.out.println("Got " + uniqueIndex.size() + " unique " + n + "-grams:");
//			for (Iterator tngit = uniqueIndex.keySet().iterator(); tngit.hasNext();) {
//				String nGram = ((String) tngit.next());
//				System.out.println(" - '" + nGram + "' at " + uniqueIndex.indexOf(nGram, 0));
//			}
//			System.out.println("Got " + (index.size() - uniqueIndex.size()) + " non-unique " + n + "-grams:");
//			for (Iterator tngit = index.keySet().iterator(); tngit.hasNext();) {
//				String nGram = ((String) tngit.next());
//				if (uniqueIndex.containsKey(nGram))
//					continue;
//				int[] nGramIndexes = index.indexesOf(nGram, 0);
//				System.out.println(" - '" + nGram + "' " + nGramIndexes.length + " times, at " + Arrays.toString(nGramIndexes));
//			}
//		}
		int maxN = 8;
		MutableAnnotation tDoc = SgmlDocumentReader.readDocument(new BufferedReader(new InputStreamReader(new FileInputStream(new File("E:/Projektdaten/TaxonxTest/21330.htm")), "UTF-8")));
		TokenIndexMap[] tDocTokenIndexes = new TokenIndexMap[maxN];
		TokenIndexMap[] tDocUniqueTokenIndexes = new TokenIndexMap[maxN];
		for (int n = 1; n <= tDocTokenIndexes.length; n++) {
			tDocTokenIndexes[n-1] = getTokenNGramIndex(tDoc, n);
			tDocUniqueTokenIndexes[n-1] = new TokenIndexMap();
			for (Iterator tngit = tDocTokenIndexes[n-1].keySet().iterator(); tngit.hasNext();) {
				String nGram = ((String) tngit.next());
				int[] nGramIndexes = tDocTokenIndexes[n-1].indexesOf(nGram, 0);
				if (nGramIndexes.length == 1)
					tDocUniqueTokenIndexes[n-1].indexAt(nGram, nGramIndexes[0]);
			}
		}
		MutableAnnotation cDoc = SgmlDocumentReader.readDocument(new BufferedReader(new InputStreamReader(new FileInputStream(new File("E:/Projektdaten/TaxonxTest/21330.complete.xml")), "UTF-8")));
		TokenIndexMap[] cDocTokenIndexes = new TokenIndexMap[maxN];
		TokenIndexMap[] cDocUniqueTokenIndexes = new TokenIndexMap[maxN];
		for (int n = 1; n <= cDocTokenIndexes.length; n++) {
			cDocTokenIndexes[n-1] = getTokenNGramIndex(cDoc, n);
			cDocUniqueTokenIndexes[n-1] = new TokenIndexMap();
			for (Iterator tngit = cDocTokenIndexes[n-1].keySet().iterator(); tngit.hasNext();) {
				String nGram = ((String) tngit.next());
				int[] nGramIndexes = cDocTokenIndexes[n-1].indexesOf(nGram, 0);
				if (nGramIndexes.length == 1)
					cDocUniqueTokenIndexes[n-1].indexAt(nGram, nGramIndexes[0]);
			}
		}
		for (int n = 1; n <= maxN; n++) {
			System.out.println("Unique " + n + "-grams shared between docs:");
			int sUniqueCount = 0;
			for (Iterator tngit = tDocUniqueTokenIndexes[n-1].keySet().iterator(); tngit.hasNext();) {
				String nGram = ((String) tngit.next());
				if (!cDocUniqueTokenIndexes[n-1].containsKey(nGram))
					continue;
				System.out.println(" - '" + nGram + "' at t." + tDocUniqueTokenIndexes[n-1].indexOf(nGram, 0) + " / c." + cDocUniqueTokenIndexes[n-1].indexOf(nGram, 0));
				sUniqueCount++;
			}
			System.out.println(" ==> " + sUniqueCount + " in total");
			System.out.println("Unique " + n + "-grams exclusive in tDoc:");
			int tUniqueCount = 0;
			for (Iterator tngit = tDocUniqueTokenIndexes[n-1].keySet().iterator(); tngit.hasNext();) {
				String nGram = ((String) tngit.next());
				if (cDocTokenIndexes[n-1].containsKey(nGram))
					continue;
				System.out.println(" - '" + nGram + "' at " + tDocUniqueTokenIndexes[n-1].indexOf(nGram, 0));
				tUniqueCount++;
			}
			System.out.println(" ==> " + tUniqueCount + " in total");
			System.out.println("Unique " + n + "-grams exclusive in cDoc:");
			int cUniqueCount = 0;
			for (Iterator tngit = cDocUniqueTokenIndexes[n-1].keySet().iterator(); tngit.hasNext();) {
				String nGram = ((String) tngit.next());
				if (tDocTokenIndexes[n-1].containsKey(nGram))
					continue;
				System.out.println(" - '" + nGram + "' at " + cDocUniqueTokenIndexes[n-1].indexOf(nGram, 0));
				cUniqueCount++;
			}
			System.out.println(" ==> " + cUniqueCount + " in total");
		}
		
		int[] tDocTokenMappings = new int[tDoc.size()];
		Arrays.fill(tDocTokenMappings, -1);
		int[] cDocTokenMappings = new int[cDoc.size()];
		Arrays.fill(cDocTokenMappings, -1);
		
		for (int n = maxN; n >= 2; n--) {
			for (Iterator tngit = tDocUniqueTokenIndexes[n-1].keySet().iterator(); tngit.hasNext();) {
				String nGram = ((String) tngit.next());
				if (!cDocUniqueTokenIndexes[n-1].containsKey(nGram))
					continue;
				int tIndex = tDocUniqueTokenIndexes[n-1].indexOf(nGram, 0);
				int cIndex = cDocUniqueTokenIndexes[n-1].indexOf(nGram, 0);
				if ((tDocTokenMappings[tIndex] != -1) && (cDocTokenMappings[cIndex] != -1))
					continue;
				for (int t = 0; t < n; t++) {
					tDocTokenMappings[tIndex+t] = (cIndex+t);
					cDocTokenMappings[cIndex+t] = (tIndex+t);
				}
			}
		}
		
		int tMapped = 0;
		int tForwardJumps = 0;
		int tBackwardJumps = 0;
		int tLastMapping = -1;
		for (int t = 0; t < tDocTokenMappings.length; t++) {
			if (tDocTokenMappings[t] == -1)
				continue;
			tMapped++;
			if (tLastMapping != -1) {
				if (tDocTokenMappings[t] < tLastMapping) {
					System.out.println("Backward jump from " + tLastMapping + " (" + cDoc.tokenAt(tLastMapping) + ") to " + tDocTokenMappings[t] + " (" + cDoc.tokenAt(tDocTokenMappings[t]) + ")");
					tBackwardJumps++;
				}
				else if ((tDocTokenMappings[t] - tLastMapping) > 1) {
					System.out.println("Forward jump from " + tLastMapping + " (" + cDoc.tokenAt(tLastMapping) + ") to " + tDocTokenMappings[t] + " (" + cDoc.tokenAt(tDocTokenMappings[t]) + ")");
					tForwardJumps++;
				}
			}
			tLastMapping = tDocTokenMappings[t];
		}
		System.out.println("N-gram mapped " + tMapped + " of " + tDocTokenMappings.length + " tDoc tokens");
		System.out.println("  " + tForwardJumps + " forward jumps, " + tBackwardJumps + " backward jumps");
		int cMapped = 0;
		int cForwardJumps = 0;
		int cBackwardJumps = 0;
		int cLastMapping = -1;
		for (int t = 0; t < cDocTokenMappings.length; t++) {
			if (cDocTokenMappings[t] == -1)
				continue;
			cMapped++;
			if (cLastMapping != -1) {
				if (cDocTokenMappings[t] < cLastMapping) {
					System.out.println("Backward jump from " + cLastMapping + " (" + tDoc.tokenAt(cLastMapping) + ") to " + cDocTokenMappings[t] + " (" + tDoc.tokenAt(cDocTokenMappings[t]) + ")");
					cBackwardJumps++;
				}
				else if ((cDocTokenMappings[t] - cLastMapping) > 1) {
					System.out.println("Forward jump from " + cLastMapping + " (" + tDoc.tokenAt(cLastMapping) + ") to " + cDocTokenMappings[t] + " (" + tDoc.tokenAt(cDocTokenMappings[t]) + ")");
					cForwardJumps++;
				}
			}
			cLastMapping = cDocTokenMappings[t];
		}
		System.out.println("N-gram mapped " + cMapped + " of " + cDocTokenMappings.length + " cDoc tokens");
		System.out.println("  " + cForwardJumps + " forward jumps, " + cBackwardJumps + " backward jumps");
//		doDiff(tDoc, cDoc);
	}
}