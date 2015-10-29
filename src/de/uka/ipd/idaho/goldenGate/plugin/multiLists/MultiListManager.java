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
package de.uka.ipd.idaho.goldenGate.plugin.multiLists;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.TableModel;

import de.uka.ipd.idaho.easyIO.settings.Settings;
import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.TokenSequence;
import de.uka.ipd.idaho.gamta.TokenSequenceUtils;
import de.uka.ipd.idaho.gamta.Tokenizer;
import de.uka.ipd.idaho.goldenGate.plugin.lists.ListManager;
import de.uka.ipd.idaho.goldenGate.plugins.AbstractDocumentProcessorManager;
import de.uka.ipd.idaho.goldenGate.plugins.DocumentProcessor;
import de.uka.ipd.idaho.goldenGate.plugins.GoldenGatePluginDataProvider;
import de.uka.ipd.idaho.goldenGate.util.DataListListener;
import de.uka.ipd.idaho.goldenGate.util.DialogPanel;
import de.uka.ipd.idaho.goldenGate.util.ResourceDialog;
import de.uka.ipd.idaho.stringUtils.StringUtils;
import de.uka.ipd.idaho.stringUtils.StringVector;
import de.uka.ipd.idaho.stringUtils.csvHandler.StringRelation;
import de.uka.ipd.idaho.stringUtils.csvHandler.StringTupel;

/**
 * Manager for document processors that create annotations by means of lookups
 * in CSV files. Lookups can use exact match, or different levels of fuzzieness,
 * each mode case sensitive or case insensitive. Different columns in the same
 * CSV file can use different modes. In case of multiple matching rows, the row
 * matched for the column with the strictest matching mode is considered the
 * matching one. If multiple rows match with this mode, the first one is used.
 * Column entries from matching rows can be attached as attributes to the
 * generated annotations. CSV files can be shared among different multi list
 * document processors.
 * 
 * @author sautter
 */
public class MultiListManager extends AbstractDocumentProcessorManager {
	
	//	TODO reduce size of lookup map
	
	//	TODO speed up lookup map generation
	
	private static final String FILE_EXTENSION = ".multiList";
	
	private static final String TOKEN_SEQUENCE_LOOKUP_MODE = "Token, Full Sequence (Exact Match)";
	private static final String TOKEN_SUB_SEQUENCE_LOOKUP_MODE = "Token, Sub Sequence";
	private static final String TOKEN_SPARSE_SUB_SEQUENCE_LOOKUP_MODE = "Token, Sparse Sub Sequence";
	private static final String TOKEN_SUFFIX_LOOKUP_MODE = "Token, Suffix";
	private static final String TOKEN_SPARSE_SUFFIX_LOOKUP_MODE = "Token, Sparse Suffix";
	private static final String TOKEN_PREFIX_LOOKUP_MODE = "Token, Prefix";
	private static final String TOKEN_SPARSE_PREFIX_LOOKUP_MODE = "Token, Sparse Prefix";
	private static final String TOKEN_LOOKUP_MODE = "Token, single";
	private static final String TOKEN_SET_LOOKUP_MODE = "Token, Set";
	private static final String TOKEN_SUB_SET_LOOKUP_MODE = "Token, Sub Set";
	
	private static final String BLOCK_SEQUENCE_LOOKUP_MODE = "Block, Full Sequence";
	private static final String BLOCK_SUB_SEQUENCE_LOOKUP_MODE = "Block, Sub Sequence";
	private static final String BLOCK_SPARSE_SUB_SEQUENCE_LOOKUP_MODE = "Block, Sparse Sub Sequence";
	private static final String BLOCK_SET_LOOKUP_MODE = "Block, Set";
	private static final String BLOCK_SUB_SET_LOOKUP_MODE = "Block, Sub Set";
	
	private static final String TOKEN_MATCH_MODE_EXACT = "Exact Match";
	private static final String TOKEN_MATCH_MODE_EDIT_DISTANCE_1 = "Edit Distance <= 1";
	private static final String TOKEN_MATCH_MODE_EDIT_DISTANCE_2 = "Edit Distance <= 2";
	private static final String TOKEN_MATCH_MODE_EDIT_DISTANCE_3 = "Edit Distance <= 3";
	private static final String TOKEN_MATCH_MODE_TOKEN_IS_PREFIX = "Token Prefix of Lookup";
	private static final String TOKEN_MATCH_MODE_TOKEN_IS_INFIX = "Token Infix of Lookup";
	private static final String TOKEN_MATCH_MODE_TOKEN_IS_SUFFIX = "Token Suffix of Lookup";
	private static final String TOKEN_MATCH_MODE_LOOKUP_IS_PREFIX = "Lookup Prefix of Token";
	private static final String TOKEN_MATCH_MODE_LOOKUP_IS_INFIX = "Lookup Infix of Token";
	private static final String TOKEN_MATCH_MODE_LOOKUP_IS_SUFFIX = "Lookup Suffix of Token";
	
	private static final String CSV_DATA_NAME_PARAMETER = "csvDataName";
	private static final String ANNOTATION_TYPE_PARAMETER = "annotationType";
	
	private static final String LOOKUP_MODE_PARAMETER = "lookupMode";
	private static final String CASE_SENSITIVE_PARAMETER = "caseSensitive";
	private static final String CASE_SENSITIVE_TRUE = "true";
	private static final String TOKEN_MATCH_MODE_PARAMETER = "tokenMatchMode";
	private static final String BRIDGEABLE_LIST_PARAMETER = "bridgeableList";
	private static final String ATTRIBUTE_NAME_PARAMETER = "attributeName";
	
	private ListManager listProvider;
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#init()
	 */
	public void init() {
		this.listProvider = ((ListManager) this.parent.getAnnotationSourceProvider(ListManager.class.getName()));
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.ResourceManager#isOperational()
	 */
	public boolean isOperational() {
		return (super.isOperational() && (this.listProvider != null));
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager#getFileExtension()
	 */
	protected String getFileExtension() {
		return FILE_EXTENSION;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.ResourceManager#getResourceTypeLabel()
	 */
	public String getResourceTypeLabel() {
		return "Multi List";
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager#getToolsMenuLabel()
	 */
	public String getToolsMenuLabel() {
		return "Apply";
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#getMainMenuTitle()
	 */
	public String getMainMenuTitle() {
		return "Multi Lists";
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager#getRequiredResourceNames(java.lang.String, boolean)
	 */
	public String[] getRequiredResourceNames(String name, boolean recourse) {
		StringVector dataNames = new StringVector();
		Settings multiList = this.loadSettingsResource(name);
		String[] columnNames = multiList.getSubsetPrefixes();
		for (int c = 0; c < columnNames.length; c++) {
			Settings columnSettings = multiList.getSubset(columnNames[c]);
			String bridgeableList = columnSettings.getSetting(BRIDGEABLE_LIST_PARAMETER);
			if (bridgeableList != null)
				dataNames.addElementIgnoreDuplicates(bridgeableList + "@" + this.listProvider.getClass().getName());
		}
		return dataNames.toStringArray();
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager#getDataNamesForResource(java.lang.String)
	 */
	public String[] getDataNamesForResource(String name) {
		StringVector dataNames = new StringVector();
		dataNames.addContentIgnoreDuplicates(super.getDataNamesForResource(name));
		Settings multiList = this.loadSettingsResource(name);
		String csvDataName = multiList.getSetting(CSV_DATA_NAME_PARAMETER);
		dataNames.addElementIgnoreDuplicates(csvDataName + "@" + this.getClass().getName());
		String[] columnNames = multiList.getSubsetPrefixes();
		for (int c = 0; c < columnNames.length; c++) {
			Settings columnSettings = multiList.getSubset(columnNames[c]);
			String bridgeableList = columnSettings.getSetting(BRIDGEABLE_LIST_PARAMETER);
			if (bridgeableList != null)
				dataNames.addContentIgnoreDuplicates(this.listProvider.getDataNamesForResource(bridgeableList));
		}
		return dataNames.toStringArray();
	}
	
	private class ColumnLookup {
		final String columnName;
		final String lookupMode;
		final boolean caseSensitive;
		final String tokenMatchMode;
		final StringVector bridgeableList;
		final String attributeName;
		ColumnLookup(String columnName, String lookupMode, boolean caseSensitive, String tokenMatchMode, StringVector bridgeableList, String attributeName) {
			this.columnName = columnName;
			this.lookupMode = lookupMode;
			this.caseSensitive = caseSensitive;
			this.tokenMatchMode = tokenMatchMode;
			this.bridgeableList = bridgeableList;
			this.attributeName = attributeName;
		}
	}
	
	private class MultiListDocumentProcessor implements DocumentProcessor {
		private String name;
		private ColumnLookup[] lookups;
		private StringRelation lookupData;
		private String annotationType;
		MultiListDocumentProcessor(String name, ColumnLookup[] lookups, StringRelation lookupData, String annotationType) {
			this.name = name;
			this.lookups = lookups;
			this.lookupData = lookupData;
			this.annotationType = annotationType;
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGate.plugins.Resource#getName()
		 */
		public String getName() {
			return this.name;
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGate.plugins.Resource#getProviderClassName()
		 */
		public String getProviderClassName() {
			return MultiListManager.class.getName();
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGate.plugins.Resource#getTypeLabel()
		 */
		public String getTypeLabel() {
			return "Multi List";
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentProcessor#process(de.uka.ipd.idaho.gamta.MutableAnnotation)
		 */
		public void process(MutableAnnotation data) {
			applyMultiList(data, this.lookups, this.lookupData, this.annotationType);
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentProcessor#process(de.uka.ipd.idaho.gamta.MutableAnnotation, java.util.Properties)
		 */
		public void process(MutableAnnotation data, Properties parameters) {
			this.process(data);
		}
	}
	
	/* 
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentProcessorManager#createDocumentProcessor()
	 */
	public String createDocumentProcessor() {
		return this.createMultiList(new Settings(), null);
	}
	
	/* 
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentProcessorManager#editDocumentProcessor(java.lang.String)
	 */
	public void editDocumentProcessor(String name) {
		Settings set = this.loadSettingsResource(name);
		if ((set == null) || set.isEmpty()) return;
		
		EditMultiListDialog emcd = new EditMultiListDialog(name, set);
		emcd.setVisible(true);
		
		if (emcd.isCommitted()) try {
			this.storeSettingsResource(name, emcd.getMultiList());
		} catch (IOException ioe) {}
	}
	
	/* 
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentProcessorManager#editDocumentProcessors()
	 */
	public void editDocumentProcessors() {
		this.editMultiLists();
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractDocumentProcessorManager#getDocumentProcessor(java.lang.String)
	 */
	public DocumentProcessor getDocumentProcessor(String name) {
		return this.getMultiList(name);
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractDocumentProcessorManager#getMainMenuItems()
	 */
	public JMenuItem[] getMainMenuItems() {
		ArrayList collector = new ArrayList();
		JMenuItem mi;
		
		if (this.dataProvider.isDataEditable()) {
			mi = new JMenuItem("Create");
			mi.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					createMultiList();
				}
			});
			collector.add(mi);
			mi = new JMenuItem("Edit");
			mi.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					editMultiLists();
				}
			});
			collector.add(mi);
		}
		
		return ((JMenuItem[]) collector.toArray(new JMenuItem[collector.size()]));
	}
	
	private static final Comparator annotationOrder = AnnotationUtils.getComparator("");
	private static final String lookupAttribute = "lookup";
	private static final String preBridgedAttribute = "preBridged";
	
	private static void applyMultiList(MutableAnnotation data, ColumnLookup[] lookups, StringRelation lookupData, String annotationType) {
		System.out.println("MultiListManager: analyzing document with " + data.size() + " tokens ...");
		for (int l = 0; l < lookups.length; l++) {
			System.out.println(" - doing lookup in column " + lookups[l].columnName);
			long start = System.currentTimeMillis();
			if (lookups[l].lookupMode == null)
				continue;
			
			MultiListLookupMap lookupMap = getLookupMap(lookupData, lookups[l].columnName, lookups[l].lookupMode, lookups[l].caseSensitive, lookups[l].tokenMatchMode, lookups[l].bridgeableList, data.getTokenizer());
			System.out.println("   - got lookup map with " + lookupMap.size() + " entries and " + lookupMap.baseBlocksDict.size() + " possible base blocks after " + (System.currentTimeMillis() - start) + " ms");
//			Annotation[] baseBlocks = Gamta.extractAllContained(data, lookupMap.baseBlocks, lookups[l].caseSensitive, true, true);
			Annotation[] baseBlocks = getBaseBlocks(data, lookupMap.baseBlocksDict, lookups[l].caseSensitive, lookups[l].tokenMatchMode);
			System.out.println("   - got " + baseBlocks.length + " base blocks after " + (System.currentTimeMillis() - start) + " ms");
			Annotation[] bridgeables = ((lookups[l].bridgeableList == null) ? new Annotation[0] : Gamta.extractAllContained(data, lookups[l].bridgeableList, lookups[l].caseSensitive, true, true));
			System.out.println("   - got " + bridgeables.length + " bridgeables after " + (System.currentTimeMillis() - start) + " ms");
			if (bridgeables.length != 0) {
				ArrayList baseBlockList = new ArrayList();
				int fbi = 0;
				for (int b = 0; b < baseBlocks.length; b++) {
					baseBlockList.add(baseBlocks[b]);
					while ((fbi < bridgeables.length) && (bridgeables[fbi].getEndIndex() < baseBlocks[b].getStartIndex()))
						fbi++;
					int bi = fbi;
					while ((bi < bridgeables.length) && (bridgeables[bi].getStartIndex() <= baseBlocks[b].getStartIndex())) {
						if (bridgeables[bi].getEndIndex() == baseBlocks[b].getStartIndex()) {
							Annotation baseBlock = Gamta.newAnnotation(data, null, bridgeables[bi].getStartIndex(), (bridgeables[bi].size() + baseBlocks[b].size()));
							Annotation[] lookup = {baseBlocks[b]};
							baseBlock.setAttribute(lookupAttribute, lookup);
							baseBlock.setAttribute(preBridgedAttribute, "");
							baseBlockList.add(baseBlock);
						}
						bi++;
					}
				}
				baseBlocks = ((Annotation[]) baseBlockList.toArray(new Annotation[baseBlockList.size()]));
				Arrays.sort(baseBlocks, annotationOrder);
				System.out.println("   - base blocks joint over bridgeables after " + (System.currentTimeMillis() - start) + " ms");
			}
			
			ArrayList annotationList = new ArrayList(Arrays.asList(baseBlocks));
			HashSet deDuplicator = new HashSet();
			for (int b = 0; b < baseBlocks.length; b++)
				deDuplicator.add(baseBlocks[b].getStartIndex() + "-" + baseBlocks[b].getEndIndex());
			Annotation[] annotations;
			do {
				annotations = ((Annotation[]) annotationList.toArray(new Annotation[annotationList.size()]));
				Arrays.sort(annotations, annotationOrder);
				for (int a = 0; a < annotations.length; a++) {
					if (annotations[a].hasAttribute(preBridgedAttribute))
						continue;
					int b = a+1;
					while ((b < annotations.length) && (annotations[b].getStartIndex() <= annotations[a].getEndIndex())) {
						if (annotations[a].getEndIndex() == annotations[b].getStartIndex()) {
							if (deDuplicator.add(annotations[a].getStartIndex() + "-" + annotations[b].getEndIndex()))
								annotationList.add(getCombinedAnnotation(data, annotations[a], annotations[b]));
						}
						b++;
					}
				}
			} while (annotationList.size() > annotations.length);
			annotations = ((Annotation[]) annotationList.toArray(new Annotation[annotationList.size()]));
			annotationList.clear();
			System.out.println("   - got " + annotations.length + " annotations after " + (System.currentTimeMillis() - start) + " ms");
			
			for (int a = 0; a < annotations.length; a++) {
				if (annotations[a].hasAttribute(preBridgedAttribute))
					continue;
				Annotation[] lookup = ((Annotation[]) annotations[a].getAttribute(lookupAttribute));
				if (lookup == null) {
					lookup = new Annotation[1];
					lookup[0] = annotations[a];
				}
				String lookupString = getLookupString(lookup, lookups[l].lookupMode, lookups[l].caseSensitive);
				StringTupel st = ((StringTupel) lookupMap.get(lookupString));
				if (st == null)
					continue;
				for (int al = 0; al < lookups.length; al++) {
					if (lookups[al].attributeName == null)
						continue;
					String aValue = st.getValue(lookups[al].columnName);
					if (aValue != null)
						annotations[a].setAttribute(lookups[al].attributeName, aValue);
				}
				annotationList.add(annotations[a]);
			}
			annotations = ((Annotation[]) annotationList.toArray(new Annotation[annotationList.size()]));
			Arrays.sort(annotations, annotationOrder);
			annotationList.clear();
			System.out.println("   - got " + annotations.length + " post-filtering annotations after " + (System.currentTimeMillis() - start) + " ms");
			
			int lastEndIndex = 0;
			for (int a = 0; a < annotations.length; a++) {
				if (annotations[a].getStartIndex() >= lastEndIndex) {
					annotations[a].changeTypeTo(annotationType);
					annotations[a].removeAttribute(lookupAttribute);
					data.addAnnotation(annotations[a]);
					lastEndIndex = annotations[a].getEndIndex();
				}
			}
			System.out.println("   - annotations added to document after " + (System.currentTimeMillis() - start) + " ms");
		}
	}
	
	private static Annotation getCombinedAnnotation(TokenSequence data, Annotation firstAnnotation, Annotation secondAnnotation) {
		Annotation annotation = Gamta.newAnnotation(data, null, firstAnnotation.getStartIndex(), (firstAnnotation.size() + secondAnnotation.size()));
		Annotation[] faLookup = {firstAnnotation};
		faLookup = ((Annotation[]) firstAnnotation.getAttribute(lookupAttribute, faLookup));
		Annotation[] saLookup = {secondAnnotation};
		saLookup = ((Annotation[]) secondAnnotation.getAttribute(lookupAttribute, saLookup));
		Annotation[] aLookup = new Annotation[faLookup.length + saLookup.length];
		System.arraycopy(faLookup, 0, aLookup, 0, faLookup.length);
		System.arraycopy(saLookup, 0, aLookup, faLookup.length, saLookup.length);
		annotation.setAttribute(lookupAttribute, aLookup);
		return annotation;
	}
	
	private static String separator = (" -" + Math.random() + "- ");
	
	private static String getLookupString(Annotation[] annotations, String lookupMode, boolean caseSensitive) {
		
		//	should never happen, but for testing a safety net helps (maybe remove later)
		if (annotations.length == 0)
			return "";
		
//		//	in exact match or token mode, the argument array should always have exactly one element (this cannot be guaranteed any more with token fuzzy matching)
//		if (TOKEN_SEQUENCE_LOOKUP_MODE.equals(lookupMode)
//				|| TOKEN_LOOKUP_MODE.equals(lookupMode)) {
//			String lookupString = TokenSequenceUtils.concatTokens(annotations[0], true, true);
//			return (caseSensitive ? lookupString : lookupString.toLowerCase());
//		}
//		
		//	in exact match or token mode, the argument array should always have exactly one element (this cannot be guaranteed any more with token fuzzy matching)
		if (TOKEN_LOOKUP_MODE.equals(lookupMode)) {
			String lookupString = TokenSequenceUtils.concatTokens(annotations[0], true, true);
			return (caseSensitive ? lookupString : lookupString.toLowerCase());
		}
		
		//	in exact match or token mode, the argument array should always have exactly one element (this cannot be guaranteed any more with token fuzzy matching)
		else if (TOKEN_SEQUENCE_LOOKUP_MODE.equals(lookupMode)) {
			StringBuffer lookupString = new StringBuffer(TokenSequenceUtils.concatTokens(annotations[0], true, true));
			for (int a = 1; a < annotations.length; a++) {
				if (Gamta.insertSpace(annotations[a-1].lastValue(), annotations[a].firstValue()))
					lookupString.append(" ");
				lookupString.append(TokenSequenceUtils.concatTokens(annotations[a], true, true));
			}
			return (caseSensitive ? lookupString.toString() : lookupString.toString().toLowerCase());
		}
		
		//	concatenate values of argument annotations (for sequence and sub sequence alike)
		else if (BLOCK_SEQUENCE_LOOKUP_MODE.equals(lookupMode)
				|| BLOCK_SUB_SEQUENCE_LOOKUP_MODE.equals(lookupMode)
				|| BLOCK_SPARSE_SUB_SEQUENCE_LOOKUP_MODE.equals(lookupMode)
				|| TOKEN_SUB_SEQUENCE_LOOKUP_MODE.equals(lookupMode)
				|| TOKEN_SPARSE_SUB_SEQUENCE_LOOKUP_MODE.equals(lookupMode)
				|| TOKEN_PREFIX_LOOKUP_MODE.equals(lookupMode)
				|| TOKEN_SPARSE_PREFIX_LOOKUP_MODE.equals(lookupMode)
				|| TOKEN_SUFFIX_LOOKUP_MODE.equals(lookupMode)
				|| TOKEN_SPARSE_SUFFIX_LOOKUP_MODE.equals(lookupMode)) {
			StringBuffer lookupString = new StringBuffer(TokenSequenceUtils.concatTokens(annotations[0], true, true));
			for (int a = 1; a < annotations.length; a++) {
				lookupString.append(separator);
				lookupString.append(TokenSequenceUtils.concatTokens(annotations[a], true, true));
			}
			return (caseSensitive ? lookupString.toString() : lookupString.toString().toLowerCase());
		}
		
		//	concatenate sorted values of argument annotations (for set and sub set alike)
		else if (BLOCK_SET_LOOKUP_MODE.equals(lookupMode)
				|| BLOCK_SUB_SET_LOOKUP_MODE.equals(lookupMode)
				|| TOKEN_SET_LOOKUP_MODE.equals(lookupMode)
				|| TOKEN_SUB_SET_LOOKUP_MODE.equals(lookupMode)) {
			StringVector lookupStrings = new StringVector();
			for (int a = 0; a < annotations.length; a++) {
				String lookupString = TokenSequenceUtils.concatTokens(annotations[a], true, true);
				lookupStrings.addElement(caseSensitive ? lookupString : lookupString.toLowerCase());
			}
			lookupStrings.sortLexicographically(false, false); // in case insensitive mode, all elements are already in lower case anyways
			return lookupStrings.concatStrings(separator);
		}
		
		//	we should never get here, but Java don't know
		else return "";
	}
	
	private static Annotation[] getBaseBlocks(TokenSequence data, StringVector baseBlocks, boolean caseSensitive, String tokenMatchMode) {
		if ((tokenMatchMode == null) || TOKEN_MATCH_MODE_EXACT.equals(tokenMatchMode))
			return Gamta.extractAllContained(data, baseBlocks, caseSensitive, true, true);
		
		ArrayList baseBlockList = new ArrayList();
		
//token pre-/suf-/infix of lookup:
//- produce all infixes of lookup using for loop over start and length:
//  - decrement length
//  - increment start for each length
//  ==> longest match preferred
//- matched string trivially equals matched infix
		if (TOKEN_MATCH_MODE_TOKEN_IS_PREFIX.equals(tokenMatchMode) || TOKEN_MATCH_MODE_TOKEN_IS_SUFFIX.equals(tokenMatchMode) || TOKEN_MATCH_MODE_TOKEN_IS_INFIX.equals(tokenMatchMode)) {
			HashMap lookupCache = new HashMap();
			HashSet negativeCache = new HashSet();
			for (int t = 0; t < data.size(); t++) {
				String token = data.valueAt(t);
				if (lookupCache.containsKey(token)) {
					Annotation baseBlock = Gamta.newAnnotation(data, null, t, 1);
					baseBlock.setAttribute(lookupAttribute, ((Annotation[]) lookupCache.get(token)));
					baseBlockList.add(baseBlock);
				}
				else if (negativeCache.contains(token))
					continue;
				else {
					for (int s = 0; s < (TOKEN_MATCH_MODE_TOKEN_IS_PREFIX.equals(tokenMatchMode) ? 1 : token.length()); s++) {
						for (int l = (token.length() - s); l >= (TOKEN_MATCH_MODE_TOKEN_IS_SUFFIX.equals(tokenMatchMode) ? (token.length() - s) : 1); l--) {
							String infix = token.substring(s, (s+l));
							if (caseSensitive ? baseBlocks.contains(infix) : baseBlocks.containsIgnoreCase(infix)) {
								Annotation baseBlock = Gamta.newAnnotation(data, null, t, 1);
								if (!caseSensitive)
									infix = infix.toLowerCase();
								TokenSequence lookupTokens = Gamta.newTokenSequence(infix, data.getTokenizer());
								Annotation[] lookup = {Gamta.newAnnotation(lookupTokens, null, 0, lookupTokens.size())};
								baseBlock.setAttribute(lookupAttribute, lookup);
								lookupCache.put(token, lookup);
								baseBlockList.add(baseBlock);
								s = token.length();
								l = 0;
							}
						}
					}
					if (!lookupCache.containsKey(token))
						negativeCache.add(token);
				}
			}
		}
		
//		lookup pre-/suf-/infix of token:
//		- create Properties mapping infixes to tekens they are derived from
//		- start with whole tokens
//		- then add infixes:
//		  - increment length difference
//		  - increment start for each length
//		  ==> best overlap preferred
//		- Properties provides matched token
		else if (TOKEN_MATCH_MODE_LOOKUP_IS_PREFIX.equals(tokenMatchMode) || TOKEN_MATCH_MODE_LOOKUP_IS_SUFFIX.equals(tokenMatchMode) || TOKEN_MATCH_MODE_LOOKUP_IS_INFIX.equals(tokenMatchMode)) {
			baseBlocks.sortByLength(true);
			HashMap lookupCache = new HashMap();
			int cutoff = 0;
			while (cutoff > -1) {
				boolean newMappings = false;
				for (int b = 0; b < baseBlocks.size(); b++) {
					String baseBlock = baseBlocks.get(b);
					if (baseBlock.length() <= cutoff)
						break;
					else if (cutoff == 0) {
						TokenSequence lookupTokens = Gamta.newTokenSequence(baseBlock, data.getTokenizer());
						Annotation[] lookup = {Gamta.newAnnotation(lookupTokens, null, 0, lookupTokens.size())};
						lookupCache.put((caseSensitive ? baseBlock : baseBlock.toLowerCase()), lookup);
						newMappings = true;
					}
					else if (TOKEN_MATCH_MODE_LOOKUP_IS_PREFIX.equals(tokenMatchMode)) {
						String prefix = baseBlock.substring(0, (baseBlock.length() - cutoff));
						if (!caseSensitive)
							prefix = prefix.toLowerCase();
						if (!lookupCache.containsKey(prefix)) {
							TokenSequence lookupTokens = Gamta.newTokenSequence(baseBlock, data.getTokenizer());
							Annotation[] lookup = {Gamta.newAnnotation(lookupTokens, null, 0, lookupTokens.size())};
							lookupCache.put(prefix, lookup);
							newMappings = true;
						}
					}
					else if (TOKEN_MATCH_MODE_LOOKUP_IS_SUFFIX.equals(tokenMatchMode)) {
						String suffix = baseBlock.substring(cutoff);
						if (!caseSensitive)
							suffix = suffix.toLowerCase();
						if (!lookupCache.containsKey(suffix)) {
							TokenSequence lookupTokens = Gamta.newTokenSequence(baseBlock, data.getTokenizer());
							Annotation[] lookup = {Gamta.newAnnotation(lookupTokens, null, 0, lookupTokens.size())};
							lookupCache.put(suffix, lookup);
							newMappings = true;
						}
					}
					else if (TOKEN_MATCH_MODE_LOOKUP_IS_INFIX.equals(tokenMatchMode)) {
						TokenSequence lookupTokens = Gamta.newTokenSequence(baseBlock, data.getTokenizer());
						Annotation[] lookup = {Gamta.newAnnotation(lookupTokens, null, 0, lookupTokens.size())};
						for (int s = 0; s <= cutoff; s++) {
							String infix = baseBlock.substring(s, (baseBlock.length() - s));
							if (!caseSensitive)
								infix = infix.toLowerCase();
							if (!lookupCache.containsKey(infix)) {
								lookupCache.put(infix, lookup);
								newMappings = true;
							}
						}
					}
				}
				if (newMappings)
					cutoff++;
				else cutoff = -1;
			}
			
			for (int t = 0; t < data.size(); t++) {
				String token = data.valueAt(t);
				if (!caseSensitive)
					token = token.toLowerCase();
				if (lookupCache.containsKey(token)) {
					Annotation baseBlock = Gamta.newAnnotation(data, null, t, 1);
					baseBlock.setAttribute(lookupAttribute, ((Annotation[]) lookupCache.get(token)));
					baseBlockList.add(baseBlock);
				}
			}
		}
		
//		edit distance match:
//		- use list of tokens sorted by length and lexicographically
//		- binary search first token to test
//		- check all possibly matching tokens to find best match
//		  - observe theoretical bounds of edit distance while doing so
		else if (TOKEN_MATCH_MODE_EDIT_DISTANCE_1.equals(tokenMatchMode) || TOKEN_MATCH_MODE_EDIT_DISTANCE_2.equals(tokenMatchMode) || TOKEN_MATCH_MODE_EDIT_DISTANCE_3.equals(tokenMatchMode)) {
			baseBlocks.sortByLength(true);
			HashMap lookupCache = new HashMap();
			HashSet negativeCache = new HashSet();
			
			int maxEditDistance = (TOKEN_MATCH_MODE_EDIT_DISTANCE_1.equals(tokenMatchMode) ? 1 : (TOKEN_MATCH_MODE_EDIT_DISTANCE_2.equals(tokenMatchMode) ? 2 : 3));
			byte[] tokenCrossSumData = new byte[27];
			byte[] baseBlockCrossSumData = new byte[27];
			
			for (int t = 0; t < data.size(); t++) {
				String token = data.valueAt(t);
				if (!caseSensitive)
					token = token.toLowerCase();
				if (lookupCache.containsKey(token)) {
					Annotation baseBlock = Gamta.newAnnotation(data, null, t, 1);
					baseBlock.setAttribute(lookupAttribute, ((Annotation[]) lookupCache.get(token)));
					baseBlockList.add(baseBlock);
				}
				else if (negativeCache.contains(token))
					continue;
				else {
					/*
					 * caching cross sum arrays in a map makes no sense:
					 * - lookup cost in hash map:
					 *   - hashing: |string| operations
					 *   - equals comparison: |string| operations
					 *   - sum: 2*|string| operations
					 * - lookup cost in tree map: 
					 *   - multiple equals comparisons: x*|string| operations
					 *   - sum: x*|string| operations
					 * - recomputation cost:
					 *   - filling array: |string| operations (getting chars), plus (at most) 5*|string| primitive operations (comparisons & counter increment), plus 27 primitive operations for initializing array
					 *   - NO multiplications at all, much less power computations (as when hashing a string) 
					 */
					
					String bestMatchBaseBlock = null;
					int bestMatchEditDistance = maxEditDistance + 1;
					
					Arrays.fill(tokenCrossSumData, 0, tokenCrossSumData.length, ((byte) 0));
					for (int tc = 0; tc < token.length(); tc++) {
						char ch = token.charAt(tc);
						if (('a' <= ch) && (ch <= 'z')) tokenCrossSumData[ch - 'a']++;
						else if (('A' <= ch) && (ch <= 'Z')) tokenCrossSumData[ch - 'A']++;
						else tokenCrossSumData[26]++;
					}
					
					int crossSumDistance = 0;
					int levenshteinDistance;
					for (int b = 0; b < baseBlocks.size(); b++) {
						String baseBlock = baseBlocks.get(b);
						if (baseBlock.length() > (token.length() + maxEditDistance))
							continue;
						else if (baseBlock.length() < (token.length() - maxEditDistance))
							break;
						
						if (!caseSensitive)
							baseBlock = baseBlock.toLowerCase();
						
						Arrays.fill(baseBlockCrossSumData, 0, baseBlockCrossSumData.length, ((byte) 0));
						for (int cc = 0; cc < baseBlock.length(); cc++) {
							char ch = baseBlock.charAt(cc);
							if (('a' <= ch) && (ch <= 'z')) baseBlockCrossSumData[ch - 'a']++;
							else if (('A' <= ch) && (ch <= 'Z')) baseBlockCrossSumData[ch - 'A']++;
							else baseBlockCrossSumData[26]++;
						}
						
						crossSumDistance = 0;
						for (int csc = 0; csc < tokenCrossSumData.length; csc++)
							crossSumDistance += Math.abs(tokenCrossSumData[csc] - baseBlockCrossSumData[csc]);
						
						if (crossSumDistance <= (2 * maxEditDistance)) {
							levenshteinDistance = StringUtils.getLevenshteinDistance(token, baseBlock, maxEditDistance, caseSensitive);
							if ((levenshteinDistance <= maxEditDistance) && (levenshteinDistance < bestMatchEditDistance)) {// compute similarity to word in question
								bestMatchBaseBlock = baseBlock;
								bestMatchEditDistance = levenshteinDistance;
							}
						}
					}
					
					if (bestMatchBaseBlock == null)
						negativeCache.add(token);
					else {
						Annotation baseBlock = Gamta.newAnnotation(data, null, t, 1);
						TokenSequence lookupTokens = Gamta.newTokenSequence(bestMatchBaseBlock, data.getTokenizer());
						Annotation[] lookup = {Gamta.newAnnotation(lookupTokens, null, 0, lookupTokens.size())};
						baseBlock.setAttribute(lookupAttribute, lookup);
						lookupCache.put(token, lookup);
						baseBlockList.add(baseBlock);
					}
				}
			}
		}
		
		return ((Annotation[]) baseBlockList.toArray(new Annotation[baseBlockList.size()]));
	}
	
	private static class MultiListLookupMap extends TreeMap {
		StringVector baseBlocksDict;
//		int maxBlocks = 1;
		MultiListLookupMap(boolean caseSensitive) {
			super(caseSensitive ? null : String.CASE_INSENSITIVE_ORDER);
			this.baseBlocksDict = new StringVector(caseSensitive);
		}
		void addBaseBlock(String baseBlock) {
			if ((baseBlock.length() > 1) || Character.isLetter(baseBlock.charAt(0)))
				this.baseBlocksDict.addElementIgnoreDuplicates(baseBlock);
		}
//		void setMaxBlocks(int maxBlocks) {
//			this.maxBlocks = Math.max(this.maxBlocks, maxBlocks);
//		}
	}
	
	private static MultiListLookupMap getLookupMap(StringRelation data, String columnName, String lookupMode, boolean caseSensitive, String tokenMatchMode, StringVector bridgeableList, Tokenizer tokenizer) {
		HashMap lookupStringTokens = new HashMap();
		ArrayList lookupTupelList = new ArrayList();
		final HashMap lookupStringParts = new HashMap();
		for (int d = 0; d < data.size(); d++) {
			StringTupel st = data.get(d);
			String columnValue = st.getValue(columnName);
			if ((columnValue == null) || (columnValue.trim().length() == 0))
				continue;
			
			lookupTupelList.add(st);
			
			TokenSequence columnTokens = tokenizer.tokenize(columnValue);
			lookupStringTokens.put(st, columnTokens);
			
			if (lookupMode.startsWith("Token"))
				lookupStringParts.put(st, new Integer(columnTokens.size()));
			
			else if (bridgeableList == null)
				lookupStringParts.put(st, new Integer(1));
			
			else {
				Annotation[] bridgeables = Gamta.extractAllContained(columnTokens, bridgeableList, caseSensitive);
				int parts = 0;
				int startIndex = 0;
				for (int b = 0; b < bridgeables.length; b++) {
					if (startIndex < bridgeables[b].getStartIndex())
						parts++;
					startIndex = bridgeables[b].getEndIndex();
				}
				if (startIndex < columnTokens.size())
					parts++;
				lookupStringParts.put(st, new Integer(parts));
			}
		}
		
		Collections.sort(lookupTupelList, new Comparator() {
			public int compare(Object st1, Object st2) {
				Integer i1 = ((Integer) lookupStringParts.get(st1));
				Integer i2 = ((Integer) lookupStringParts.get(st2));
				return i1.compareTo(i2);
			}
		});
		
		MultiListLookupMap lookupMap = new MultiListLookupMap(caseSensitive);
		for (int d = 0; d < lookupTupelList.size(); d++) {
			StringTupel st = ((StringTupel) lookupTupelList.get(d));
			TokenSequence columnTokens = ((TokenSequence) lookupStringTokens.get(st));
			
			if (TOKEN_SEQUENCE_LOOKUP_MODE.equals(lookupMode)) {
				String lookupString = TokenSequenceUtils.concatTokens(columnTokens, true, true);
				if (!caseSensitive)
					lookupString = lookupString.toLowerCase();
				if (!lookupMap.containsKey(lookupString)) {
					lookupMap.put(lookupString, st);
					lookupMap.addBaseBlock(lookupString);
				}
			}
			
			else if (TOKEN_PREFIX_LOOKUP_MODE.equals(lookupMode)) {
				for (int s = 1; s <= columnTokens.size(); s++) {
					String lookupString = TokenSequenceUtils.concatTokens(columnTokens, 0, s, true, true);
					if (!caseSensitive)
						lookupString = lookupString.toLowerCase();
					if (!lookupMap.containsKey(lookupString)) {
						lookupMap.put(lookupString, st);
						lookupMap.addBaseBlock(lookupString);
					}
				}
			}
			
			else if (TOKEN_SUFFIX_LOOKUP_MODE.equals(lookupMode)) {
				for (int s = 0; s < columnTokens.size(); s++) {
					String lookupString = TokenSequenceUtils.concatTokens(columnTokens, s, (columnTokens.size() - s), true, true);
					if (!caseSensitive)
						lookupString = lookupString.toLowerCase();
					if (!lookupMap.containsKey(lookupString)) {
						lookupMap.put(lookupString, st);
						lookupMap.addBaseBlock(lookupString);
					}
				}
			}
			
			else if (TOKEN_SUB_SEQUENCE_LOOKUP_MODE.equals(lookupMode)) {
				for (int i = 0; i < columnTokens.size(); i++)
					for (int s = 1; s <= (columnTokens.size()-i); s++) {
						String lookupString = TokenSequenceUtils.concatTokens(columnTokens, i, s, true, true);
						if (!caseSensitive)
							lookupString = lookupString.toLowerCase();
						if (!lookupMap.containsKey(lookupString)) {
							lookupMap.put(lookupString, st);
							lookupMap.addBaseBlock(lookupString);
						}
					}
			}
			
			else if (TOKEN_LOOKUP_MODE.equals(lookupMode)) {
				for (int t = 0; t < columnTokens.size(); t++) {
					String lookupString = columnTokens.valueAt(t);
					if (!caseSensitive)
						lookupString = lookupString.toLowerCase();
					if (!lookupMap.containsKey(lookupString)) {
						lookupMap.put(lookupString, st);
						lookupMap.addBaseBlock(lookupString);
					}
				}
			}
			
			else {
				Annotation[] annotations;
				if (lookupMode.startsWith("Token")) {
					annotations = new Annotation[columnTokens.size()];
					for (int t = 0; t < columnTokens.size(); t++) {
						annotations[t] = Gamta.newAnnotation(columnTokens, null, t, 1);
						lookupMap.addBaseBlock(columnTokens.valueAt(t));
					}
//					lookupMap.setMaxBlocks(columnTokens.size());
				}
				else if (bridgeableList == null) {
					annotations = new Annotation[1];
					annotations[0] = Gamta.newAnnotation(columnTokens, null, 0, columnTokens.size());
					lookupMap.addBaseBlock(TokenSequenceUtils.concatTokens(annotations[0], true, true));
				}
				else {
					Annotation[] bridgeables = Gamta.extractAllContained(columnTokens, bridgeableList, caseSensitive);
					ArrayList annotationList = new ArrayList();
					int startIndex = 0;
					for (int b = 0; b < bridgeables.length; b++) {
						if (startIndex < bridgeables[b].getStartIndex()) {
							Annotation annotation = Gamta.newAnnotation(columnTokens, null, startIndex, (bridgeables[b].getStartIndex() - startIndex));
							annotationList.add(annotation);
							lookupMap.addBaseBlock(TokenSequenceUtils.concatTokens(annotation, true, true));
						}
						startIndex = bridgeables[b].getEndIndex();
					}
					if (startIndex < columnTokens.size()) {
						Annotation annotation = Gamta.newAnnotation(columnTokens, null, startIndex, (columnTokens.size() - startIndex));
						annotationList.add(annotation);
						lookupMap.addBaseBlock(TokenSequenceUtils.concatTokens(annotation, true, true));
					}
					annotations = ((Annotation[]) annotationList.toArray(new Annotation[annotationList.size()]));
//					lookupMap.setMaxBlocks(annotations.length);
				}
				
				if (BLOCK_SEQUENCE_LOOKUP_MODE.equals(lookupMode)
						|| BLOCK_SET_LOOKUP_MODE.equals(lookupMode)
						|| TOKEN_SET_LOOKUP_MODE.equals(lookupMode)) {
					String lookupString = getLookupString(annotations, lookupMode, caseSensitive);
					if (!lookupMap.containsKey(lookupString))
						lookupMap.put(lookupString, st);
				}
				
				else if (BLOCK_SUB_SEQUENCE_LOOKUP_MODE.equals(lookupMode)) {
					Annotation[] subAnnotations;
					ArrayList subAnnotationList = new ArrayList();
					for (int s = 0; s < annotations.length; s++) {
						subAnnotationList.clear();
						for (int a = s; a < annotations.length; a++) {
							subAnnotationList.add(annotations[a]);
							subAnnotations = ((Annotation[]) subAnnotationList.toArray(new Annotation[subAnnotationList.size()]));
							String lookupString = getLookupString(subAnnotations, lookupMode, caseSensitive);
							if (!lookupMap.containsKey(lookupString))
								lookupMap.put(lookupString, st);
						}
					}
				}
				
				else if (TOKEN_SPARSE_PREFIX_LOOKUP_MODE.equals(lookupMode)) {
					Annotation[] subAnnotations;
					ArrayList subAnnotationList = new ArrayList();
					for (int s = 1; s < (1 << annotations.length); s++) {
						if ((s & 1) != 0) // no need to run both options, first annotation is mandatory
							continue;
						subAnnotationList.clear();
						subAnnotationList.add(annotations[0]);
						for (int a = 1; a < annotations.length; a++) {
							if ((s & (1 << a)) != 0)
								subAnnotationList.add(annotations[a]);
						}
						subAnnotations = ((Annotation[]) subAnnotationList.toArray(new Annotation[subAnnotationList.size()]));
						String lookupString = getLookupString(subAnnotations, lookupMode, caseSensitive);
						if (!lookupMap.containsKey(lookupString))
							lookupMap.put(lookupString, st);
					}
				}
				
				else if (TOKEN_SPARSE_SUFFIX_LOOKUP_MODE.equals(lookupMode)) {
					Annotation[] subAnnotations;
					ArrayList subAnnotationList = new ArrayList();
					for (int s = 1; s < (1 << (annotations.length-1)); s++) {  // no need to run all the way, last annotation is mandatory
						subAnnotationList.clear();
						for (int a = 0; a < (annotations.length-1); a++) {
							if ((s & (1 << a)) != 0)
								subAnnotationList.add(annotations[a]);
						}
						subAnnotationList.add(annotations[annotations.length-1]);
						subAnnotations = ((Annotation[]) subAnnotationList.toArray(new Annotation[subAnnotationList.size()]));
						String lookupString = getLookupString(subAnnotations, lookupMode, caseSensitive);
						if (!lookupMap.containsKey(lookupString))
							lookupMap.put(lookupString, st);
					}
				}
				
				else if (BLOCK_SPARSE_SUB_SEQUENCE_LOOKUP_MODE.equals(lookupMode)
						|| BLOCK_SUB_SET_LOOKUP_MODE.equals(lookupMode)
						|| TOKEN_SPARSE_SUB_SEQUENCE_LOOKUP_MODE.equals(lookupMode)
						|| TOKEN_SUB_SET_LOOKUP_MODE.equals(lookupMode)) {
					Annotation[] subAnnotations;
					ArrayList subAnnotationList = new ArrayList();
					for (int s = 1; s < (1 << annotations.length); s++) {
						subAnnotationList.clear();
						for (int a = 0; a < annotations.length; a++) {
							if ((s & (1 << a)) != 0)
								subAnnotationList.add(annotations[a]);
						}
						subAnnotations = ((Annotation[]) subAnnotationList.toArray(new Annotation[subAnnotationList.size()]));
						String lookupString = getLookupString(subAnnotations, lookupMode, caseSensitive);
						if (!lookupMap.containsKey(lookupString))
							lookupMap.put(lookupString, st);
					}
				}
			}
		}
		
		lookupMap.baseBlocksDict.sortLexicographically(false);
		return lookupMap;
	}
	
	private DocumentProcessor getMultiList(String name) {
		if (name == null) return null;
		return this.getMultiList(name, this.loadSettingsResource(name));
	}
	
//	private static class ExcelCsvConverterReader extends Reader {
//		private Reader source;
//		private StringReader sr = new StringReader("");
//		ExcelCsvConverterReader(Reader source) {
//			this.source = source;
//		}
//		public void close() throws IOException {
//			this.source.close();
//		}
//		public int read(char[] cbuf, int off, int len) throws IOException {
//			int read = this.sr.read(cbuf, off, len);
//			if (read != -1) {
////				System.out.println("Read " + read + " characters");
//				return read;
//			}
//			else {
//				int next = this.source.read();
//				if (next == -1)
//					return -1;
//				
//				char lastChar = '\u0000';
//				char currentChar;
//				char nextChar = ((char) next);
//				
//				boolean quoted = false;
//				boolean escaped = false;
//				
//				StringVector tupelValues = new StringVector();
//				StringBuffer valueAssembler = new StringBuffer();
//				
//				while (next != -1) {
//					currentChar = nextChar;
//					next = this.source.read();
//					nextChar = ((char) next);
//					
//					//	escaped character
//					if (escaped) {
//						escaped = false;
//						valueAssembler.append(currentChar);
//					}
//					
//					//	start or end of quoted value
//					else if (currentChar == '"') {
//						if (quoted) {
//							if (nextChar == '"') escaped = true;
//							else if ((nextChar == ';') || (nextChar == '\n') || (nextChar == '\r')) quoted = false;
//						}
//						else quoted = true;
//					}
//					
//					//	in quoted value
//					else if (quoted) valueAssembler.append(currentChar);
//					
//					//	end of value
//					else if ((currentChar == ';')) {
//						tupelValues.addElement((lastChar == '"') ? valueAssembler.toString() : valueAssembler.toString().trim());
//						valueAssembler = new StringBuffer();
//					}
//					
//					//	end of tupel
//					else if ((currentChar == '\n') || (currentChar == '\r') || (next == -1)) {
//						if (valueAssembler.length() != 0) {
//							tupelValues.addElement((lastChar == '"') ? valueAssembler.toString() : valueAssembler.toString().trim());
//							valueAssembler = new StringBuffer();
//						}
//						for (int v = 0; v < tupelValues.size(); v++)
//							tupelValues.setElementAt(("\"" + StringUtils.replaceAll(tupelValues.get(v), "\\\"", ("\"\"")) + "\""), v);
////						System.out.println("Next Chunk of Data: " + tupelValues.concatStrings(","));
//						this.sr = new StringReader(tupelValues.concatStrings(",") + "\n");
//						return this.read(cbuf, off, len);
//					}
//					
//					//	other char
//					else valueAssembler.append(currentChar);
//					
//					//	remember char
//					lastChar = currentChar;
//				}
//				
//				//	check if data left in buffers
//				if (valueAssembler.length() != 0)
//					tupelValues.addElement(valueAssembler.toString());
//				if (tupelValues.size() != 0) {
//					for (int v = 0; v < tupelValues.size(); v++)
//						tupelValues.setElementAt(("\"" + StringUtils.replaceAll(tupelValues.get(v), "\\\"", ("\"\"")) + "\""), v);
////					System.out.println("Next Chunk of Data: " + tupelValues.concatStrings(","));
//					this.sr = new StringReader(tupelValues.concatStrings(",") + "\n");
//					return this.read(cbuf, off, len);
//				}
//			}
//			return -1;
//		}
//	}
//	
	private DocumentProcessor getMultiList(String name, Settings settings) {
		if (settings == null)
			return null;
		String lookupDataName = settings.getSetting(CSV_DATA_NAME_PARAMETER);
		if (lookupDataName == null)
			return null;
		String annotationType = settings.getSetting(ANNOTATION_TYPE_PARAMETER);
		if (annotationType == null)
			return null;
		StringRelation lookupData;
		try {
			lookupData = this.readCsvData(lookupDataName);
//			Reader csvIn = new InputStreamReader(this.dataProvider.getInputStream(lookupDataName), "UTF-8");
//			lookupData = StringRelation.readCsvData(csvIn, '"');
//			csvIn.close();
//			BufferedReader csvIn = new BufferedReader(new InputStreamReader(this.dataProvider.getInputStream(lookupDataName), "UTF-8"));
//			lookupData = StringRelation.readCsvData(csvIn, StringRelation.GUESS_SEPARATOR, StringRelation.DEFAULT_VALUE_DELIMITER);
//			csvIn.close();
//			csvIn.mark(8192); // buffered reader's default buffer size
//			String testLine = csvIn.readLine();
//			csvIn.reset();
//			int commaCount = 0;
//			int semicolonCount = 0;
//			for (int c = 0; c < testLine.length(); c++) {
//				char ch = testLine.charAt(c);
//				if (ch == ',')
//					commaCount++;
//				else if (ch == ';')
//					semicolonCount++;
//			}
//			if (semicolonCount >= commaCount)// convert MS Excel 'CSV'
//				csvIn = new BufferedReader(new ExcelCsvConverterReader(csvIn));
//			lookupData = StringRelation.readCsvData(csvIn, '"');
//			csvIn.close();
		}
		catch (IOException ioe) {
			return null;
		}
		StringVector columnNames = lookupData.getKeys();
		columnNames.sortLexicographically(false, false);
		ArrayList columnLookups = new ArrayList();
		for (int c = 0; c < columnNames.size(); c++) {
			String columnName = columnNames.get(c);
			Settings columnSettings = settings.getSubset(columnName);
			String lookupMode = columnSettings.getSetting(LOOKUP_MODE_PARAMETER);
			boolean caseSensitive = CASE_SENSITIVE_TRUE.equals(columnSettings.getSetting(CASE_SENSITIVE_PARAMETER));
			String tokenMatchMode = columnSettings.getSetting(TOKEN_MATCH_MODE_PARAMETER);
			String bridgeableList = columnSettings.getSetting(BRIDGEABLE_LIST_PARAMETER);
			String attributeName = columnSettings.getSetting(ATTRIBUTE_NAME_PARAMETER);
			if ((attributeName != null) || (lookupMode != null))
				columnLookups.add(new ColumnLookup(columnName, lookupMode, caseSensitive, tokenMatchMode, this.listProvider.getList(bridgeableList), attributeName));
		}
		return new MultiListDocumentProcessor(name, ((ColumnLookup[]) columnLookups.toArray(new ColumnLookup[columnLookups.size()])), lookupData, annotationType);
	}
	
	private StringRelation readCsvData(String dataName) throws IOException {
		BufferedInputStream csvIn = new BufferedInputStream(this.dataProvider.getInputStream(dataName));
		
		csvIn.mark(16);
		String encoding = "UTF-8";
		int bomLength = 0;
		byte[] bomCheck = new byte[16];
		int bomRead = csvIn.read(bomCheck, 0, bomCheck.length);
		if (bomRead >= 2) {
			if ((bomCheck[0] == -2) && (bomCheck[1] == -1)) {
				System.out.println("RECOGNIZED ENCODING FROM BOM: UTF-16BE");
				bomLength = 2;
				encoding = "UTF-16BE";
			}
			if ((bomCheck[0] == -1) && (bomCheck[1] == -2)) {
				System.out.println("RECOGNIZED ENCODING FROM BOM: UTF-16LE");
				bomLength = 2;
				encoding = "UTF-16LE";
			}
		}
		if ((bomRead >= 3) && (bomCheck[0] == -17) && (bomCheck[1] == -69) && (bomCheck[2] == -65)) {
			System.out.println("RECOGNIZED ENCODING FROM BOM: UTF-8");
			bomLength = 3;
			encoding = "UTF-8";
		}
		csvIn.reset();
		csvIn.skip(bomLength);
		
		BufferedReader csvBr = new BufferedReader(new InputStreamReader(csvIn, encoding));
		StringRelation data = StringRelation.readCsvData(csvBr, StringRelation.GUESS_SEPARATOR, StringRelation.DEFAULT_VALUE_DELIMITER);
		csvBr.close();
		
		return data;
	}
	
	private boolean createMultiList() {
		return (this.createMultiList(new Settings(), null) != null);
	}
	
	private boolean cloneMultiList() {
		String selectedName = this.resourceNameList.getSelectedName();
		if (selectedName == null)
			return this.createMultiList();
		else {
			String name = "New " + selectedName;
			return (this.createMultiList(this.loadSettingsResource(selectedName), name) != null);
		}
	}
	
	private String createMultiList(Settings set, String name) {
		CreateMultiListDialog cfd = new CreateMultiListDialog(name, set);
		cfd.setVisible(true);
		if (cfd.isCommitted()) {
			Settings multiList = cfd.getMultiList();
			String multiListName = cfd.getMultiListName();
			if (!multiListName.endsWith(FILE_EXTENSION)) multiListName += FILE_EXTENSION;
			try {
				if (this.storeSettingsResource(multiListName, multiList)) {
					this.resourceNameList.refresh();
					return multiListName;
				}
			} catch (IOException e) {}
		}
		return null;
	}
	
	private void editMultiLists() {
		final MultiListEditorPanel[] editor = new MultiListEditorPanel[1];
		editor[0] = null;
		
		final DialogPanel editDialog = new DialogPanel("Edit MultiLists", true);
		editDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		editDialog.addWindowListener(new WindowAdapter() {
			public void windowClosed(WindowEvent we) {
				this.closeDialog();
			}
			public void windowClosing(WindowEvent we) {
				this.closeDialog();
			}
			private void closeDialog() {
				if ((editor[0] != null) && editor[0].isDirty()) {
					try {
						storeSettingsResource(editor[0].name, editor[0].getSettings());
					} catch (IOException ioe) {}
				}
				if (editDialog.isVisible()) editDialog.dispose();
			}
		});
		
		editDialog.setLayout(new BorderLayout());
		
		JPanel editButtons = new JPanel(new FlowLayout(FlowLayout.CENTER));
		JButton button;
		button = new JButton("Create");
		button.setBorder(BorderFactory.createRaisedBevelBorder());
		button.setPreferredSize(new Dimension(100, 21));
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				createMultiList();
			}
		});
		editButtons.add(button);
		button = new JButton("Clone");
		button.setBorder(BorderFactory.createRaisedBevelBorder());
		button.setPreferredSize(new Dimension(100, 21));
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				cloneMultiList();
			}
		});
		editButtons.add(button);
		button = new JButton("Delete");
		button.setBorder(BorderFactory.createRaisedBevelBorder());
		button.setPreferredSize(new Dimension(100, 21));
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (deleteResource(resourceNameList.getSelectedName()))
					resourceNameList.refresh();
			}
		});
		editButtons.add(button);
		
		editDialog.add(editButtons, BorderLayout.NORTH);
		
		final JPanel editorPanel = new JPanel(new BorderLayout());
		String selectedName = this.resourceNameList.getSelectedName();
		if (selectedName == null)
			editorPanel.add(this.getExplanationLabel(), BorderLayout.CENTER);
		else {
			Settings set = this.loadSettingsResource(selectedName);
			if (set == null)
				editorPanel.add(this.getExplanationLabel(), BorderLayout.CENTER);
			else {
				editor[0] = new MultiListEditorPanel(selectedName, set, this.listProvider.getResourceNames(), this.dataProvider);
				editorPanel.add(editor[0], BorderLayout.CENTER);
			}
		}
		editDialog.add(editorPanel, BorderLayout.CENTER);
		
		editDialog.add(this.resourceNameList, BorderLayout.EAST);
		DataListListener dll = new DataListListener() {
			public void selected(String dataName) {
				if ((editor[0] != null) && editor[0].isDirty()) {
					try {
						storeSettingsResource(editor[0].name, editor[0].getSettings());
					}
					catch (IOException ioe) {
						if (JOptionPane.showConfirmDialog(editDialog, (ioe.getClass().getName() + " (" + ioe.getMessage() + ")\nwhile saving file to " + editor[0].name + "\nProceed?"), "Could Not Save Analyzer", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
							resourceNameList.setSelectedName(editor[0].name);
							editorPanel.validate();
							return;
						}
					}
				}
				editorPanel.removeAll();
				
				if (dataName == null)
					editorPanel.add(getExplanationLabel(), BorderLayout.CENTER);
				else {
					Settings set = loadSettingsResource(dataName);
					if (set == null)
						editorPanel.add(getExplanationLabel(), BorderLayout.CENTER);
					else {
						editor[0] = new MultiListEditorPanel(dataName, set, listProvider.getResourceNames(), dataProvider);
						editorPanel.add(editor[0], BorderLayout.CENTER);
					}
				}
				editorPanel.validate();
			}
		};
		this.resourceNameList.addDataListListener(dll);
		
		editDialog.setSize(DEFAULT_EDIT_DIALOG_SIZE);
		editDialog.setLocationRelativeTo(editDialog.getOwner());
		editDialog.setVisible(true);
		
		this.resourceNameList.removeDataListListener(dll);
	}
	
	private class CreateMultiListDialog extends DialogPanel {
		
		private JTextField nameField;
		
		private MultiListEditorPanel editor;
		private Settings multiList = null;
		private String multiListName = null;
		
		CreateMultiListDialog(String name, Settings settings) {
			super("Create Markup Converter", true);
			
			this.nameField = new JTextField((name == null) ? "New MultiList" : name);
			this.nameField.setBorder(BorderFactory.createLoweredBevelBorder());
			
			//	initialize main buttons
			JButton commitButton = new JButton("Create");
			commitButton.setBorder(BorderFactory.createRaisedBevelBorder());
			commitButton.setPreferredSize(new Dimension(100, 21));
			commitButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					multiList = editor.getSettings();
					multiListName = nameField.getText();
					dispose();
				}
			});
			
			JButton abortButton = new JButton("Cancel");
			abortButton.setBorder(BorderFactory.createRaisedBevelBorder());
			abortButton.setPreferredSize(new Dimension(100, 21));
			abortButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					multiList = null;
					multiListName = null;
					dispose();
				}
			});
			
			JPanel mainButtonPanel = new JPanel();
			mainButtonPanel.setLayout(new FlowLayout());
			mainButtonPanel.add(commitButton);
			mainButtonPanel.add(abortButton);
			
			//	initialize editor
			this.editor = new MultiListEditorPanel(name, settings, listProvider.getResourceNames(), dataProvider);
			
			//	put the whole stuff together
			this.setLayout(new BorderLayout());
			this.add(this.nameField, BorderLayout.NORTH);
			this.add(this.editor, BorderLayout.CENTER);
			this.add(mainButtonPanel, BorderLayout.SOUTH);
			
			this.setResizable(true);
			this.setSize(new Dimension(600, 400));
			this.setLocationRelativeTo(this.getOwner());
		}
		
		boolean isCommitted() {
			return (this.multiList != null);
		}
		
		Settings getMultiList() {
			return this.multiList;
		}
		
		String getMultiListName() {
			return this.multiListName;
		}
	}

	private class EditMultiListDialog extends DialogPanel {
		
		private MultiListEditorPanel editor;
		private Settings multiList = null;
		
		EditMultiListDialog(String name, Settings settings) {
			super(("Edit Markup Converter '" + name + "'"), true);
			
			//	initialize main buttons
			JButton commitButton = new JButton("OK");
			commitButton.setBorder(BorderFactory.createRaisedBevelBorder());
			commitButton.setPreferredSize(new Dimension(100, 21));
			commitButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					multiList = editor.getSettings();
					dispose();
				}
			});
			
			JButton abortButton = new JButton("Cancel");
			abortButton.setBorder(BorderFactory.createRaisedBevelBorder());
			abortButton.setPreferredSize(new Dimension(100, 21));
			abortButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					multiList = null;
					dispose();
				}
			});
			
			JPanel mainButtonPanel = new JPanel();
			mainButtonPanel.setLayout(new FlowLayout());
			mainButtonPanel.add(commitButton);
			mainButtonPanel.add(abortButton);
			
			//	initialize editor
			this.editor = new MultiListEditorPanel(name, settings, listProvider.getResourceNames(), dataProvider);
			
			//	put the whole stuff together
			this.setLayout(new BorderLayout());
			this.add(this.editor, BorderLayout.CENTER);
			this.add(mainButtonPanel, BorderLayout.SOUTH);
			
			this.setResizable(true);
			this.setSize(new Dimension(600, 400));
			this.setLocationRelativeTo(this.getOwner());
		}
		
		boolean isCommitted() {
			return (this.multiList != null);
		}
		
		Settings getMultiList() {
			return this.multiList;
		}
	}
	
	private class ColumnLookupPanel extends JPanel {
		private static final String NO_LOOKUP = "<No Lookup>";
		private static final String NO_LIST = "<No List>";
		String columnName;
		boolean dirty = false;
		JComboBox lookupModeSelector;
		JCheckBox caseSensitiveFlag;
		JComboBox tokenMatchModeSelector;
		JButton bridgeableListButton;
		String bridgeableListName;
		JTextField attributeName;
		ColumnLookupPanel(String columnName, String lookupMode, boolean caseSensitive, String tokenMatchMode, String bListName, String[] bListNames, String attributeName) {
			super(new GridBagLayout(), true);
			this.columnName = columnName;
			this.bridgeableListName = bListName;
			
			this.lookupModeSelector = new JComboBox();
			this.lookupModeSelector.addItem(NO_LOOKUP);
			this.lookupModeSelector.addItem(TOKEN_SEQUENCE_LOOKUP_MODE);
			this.lookupModeSelector.addItem(TOKEN_LOOKUP_MODE);
			this.lookupModeSelector.addItem(TOKEN_SUB_SEQUENCE_LOOKUP_MODE);
			this.lookupModeSelector.addItem(TOKEN_SPARSE_SUB_SEQUENCE_LOOKUP_MODE);
			this.lookupModeSelector.addItem(TOKEN_SUFFIX_LOOKUP_MODE);
			this.lookupModeSelector.addItem(TOKEN_SPARSE_SUFFIX_LOOKUP_MODE);
			this.lookupModeSelector.addItem(TOKEN_PREFIX_LOOKUP_MODE);
			this.lookupModeSelector.addItem(TOKEN_SPARSE_PREFIX_LOOKUP_MODE);
			this.lookupModeSelector.addItem(TOKEN_SET_LOOKUP_MODE);
			this.lookupModeSelector.addItem(TOKEN_SUB_SET_LOOKUP_MODE);
			this.lookupModeSelector.addItem(BLOCK_SEQUENCE_LOOKUP_MODE);
			this.lookupModeSelector.addItem(BLOCK_SUB_SEQUENCE_LOOKUP_MODE);
			this.lookupModeSelector.addItem(BLOCK_SPARSE_SUB_SEQUENCE_LOOKUP_MODE);
			this.lookupModeSelector.addItem(BLOCK_SET_LOOKUP_MODE);
			this.lookupModeSelector.addItem(BLOCK_SUB_SET_LOOKUP_MODE);
			this.lookupModeSelector.setEditable(false);
			this.lookupModeSelector.setSelectedItem((lookupMode == null) ? NO_LOOKUP : lookupMode);
			this.lookupModeSelector.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent ie) {
					String lookupMode = ((String) lookupModeSelector.getSelectedItem());
					bridgeableListButton.setEnabled(!NO_LOOKUP.equals(lookupMode) && !lookupMode.startsWith("Token"));
					caseSensitiveFlag.setEnabled(!NO_LOOKUP.equals(lookupMode));
					if (lookupMode.startsWith("Token"))
						tokenMatchModeSelector.setEnabled(true);
					else {
						tokenMatchModeSelector.setSelectedItem(TOKEN_MATCH_MODE_EXACT);
						tokenMatchModeSelector.setEnabled(false);
					}
					dirty = true;
				}
			});
			
			this.tokenMatchModeSelector = new JComboBox();
			this.tokenMatchModeSelector.addItem(TOKEN_MATCH_MODE_EXACT);
			this.tokenMatchModeSelector.addItem(TOKEN_MATCH_MODE_EDIT_DISTANCE_1);
			this.tokenMatchModeSelector.addItem(TOKEN_MATCH_MODE_EDIT_DISTANCE_2);
			this.tokenMatchModeSelector.addItem(TOKEN_MATCH_MODE_EDIT_DISTANCE_3);
			this.tokenMatchModeSelector.addItem(TOKEN_MATCH_MODE_TOKEN_IS_PREFIX);
			this.tokenMatchModeSelector.addItem(TOKEN_MATCH_MODE_TOKEN_IS_INFIX);
			this.tokenMatchModeSelector.addItem(TOKEN_MATCH_MODE_TOKEN_IS_SUFFIX);
			this.tokenMatchModeSelector.addItem(TOKEN_MATCH_MODE_LOOKUP_IS_PREFIX);
			this.tokenMatchModeSelector.addItem(TOKEN_MATCH_MODE_LOOKUP_IS_INFIX);
			this.tokenMatchModeSelector.addItem(TOKEN_MATCH_MODE_LOOKUP_IS_SUFFIX);
			this.tokenMatchModeSelector.setEditable(false);
			this.tokenMatchModeSelector.setSelectedItem(((tokenMatchMode == null) || ((lookupMode != null) && !lookupMode.startsWith("Token"))) ? TOKEN_MATCH_MODE_EXACT : tokenMatchMode);
			this.tokenMatchModeSelector.setEnabled((lookupMode != null) && lookupMode.startsWith("Token"));
			this.tokenMatchModeSelector.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent ie) {
					dirty = true;
				}
			});
			
			this.caseSensitiveFlag = new JCheckBox();
			this.caseSensitiveFlag.setSelected(caseSensitive);
			this.caseSensitiveFlag.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent ie) {
					dirty = true;
				}
			});
			this.caseSensitiveFlag.setEnabled(lookupMode != null);
			
			this.bridgeableListButton = new JButton(((this.bridgeableListName == null) ? NO_LIST : this.bridgeableListName) + " (click to change)");
			final String[] brListNames = new String[bListNames.length + 1];
			brListNames[0] = NO_LIST;
			System.arraycopy(bListNames, 0, brListNames, 1, bListNames.length);
			this.bridgeableListButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					ResourceDialog rd = ResourceDialog.getResourceDialog(brListNames, "Select Bridgeable List", "Select");
					rd.setVisible(true);
					if (rd.isCommitted()) {
						String bListName = rd.getSelectedResourceName();
						bridgeableListName = (NO_LIST.equals(bListName) ? null : bListName);
						bridgeableListButton.setText(((bridgeableListName == null) ? NO_LIST : bridgeableListName) + " (click to change)");
						dirty = true;
					}
				}
			});
			this.bridgeableListButton.setEnabled((lookupMode != null) && !lookupMode.startsWith("Token"));
			
			this.attributeName = new JTextField();
			this.attributeName.setText((attributeName == null) ? "" : attributeName);
			this.attributeName.getDocument().addDocumentListener(new DocumentListener() {
				public void changedUpdate(DocumentEvent de) {}
				public void insertUpdate(DocumentEvent de) {
					dirty = true;
				}
				public void removeUpdate(DocumentEvent de) {
					dirty = true;
				}
			});
			
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.insets.top = 3;
			gbc.insets.bottom = 3;
			gbc.insets.left = 3;
			gbc.insets.right = 3;
			gbc.weighty = 0;
			gbc.gridheight = 1;
			gbc.gridwidth = 1;
			gbc.fill = GridBagConstraints.BOTH;
			gbc.gridy = 0;
			
			gbc.gridx = 0;
			gbc.weightx = 0;
			this.add(new JLabel("Lookup Mode:", JLabel.LEFT), gbc.clone());
			gbc.gridx = 1;
			gbc.weightx = 1;
			this.add(this.lookupModeSelector, gbc.clone());
			gbc.gridy++;
			
			gbc.gridx = 0;
			gbc.weightx = 0;
			this.add(new JLabel("Lookup Case Sensitive?", JLabel.LEFT), gbc.clone());
			gbc.gridx = 1;
			gbc.weightx = 1;
			this.add(this.caseSensitiveFlag, gbc.clone());
			gbc.gridy++;
			
			gbc.gridx = 0;
			gbc.weightx = 0;
			this.add(new JLabel("Token Match Mode", JLabel.LEFT), gbc.clone());
			gbc.gridx = 1;
			gbc.weightx = 1;
			this.add(this.tokenMatchModeSelector, gbc.clone());
			gbc.gridy++;
			
			gbc.gridx = 0;
			gbc.weightx = 0;
			this.add(new JLabel("Bridgeable List:", JLabel.LEFT), gbc.clone());
			gbc.gridx = 1;
			gbc.weightx = 1;
			this.add(this.bridgeableListButton, gbc.clone());
			gbc.gridy++;
			
			gbc.gridx = 0;
			gbc.weightx = 0;
			this.add(new JLabel("Attribute Name:", JLabel.LEFT), gbc.clone());
			gbc.gridx = 1;
			gbc.weightx = 1;
			this.add(this.attributeName, gbc.clone());
		}
		
		void addSettings(Settings settings) {
			String lookupMode = ((String) this.lookupModeSelector.getSelectedItem());
			String attributeName = this.attributeName.getText().trim();
			Settings set = settings.getSubset(this.columnName);
			if (!NO_LOOKUP.equals(lookupMode)) {
				set.setSetting(LOOKUP_MODE_PARAMETER, lookupMode);
				if (this.caseSensitiveFlag.isSelected())
					set.setSetting(CASE_SENSITIVE_PARAMETER, CASE_SENSITIVE_TRUE);
				if (!TOKEN_MATCH_MODE_EXACT.equals(this.tokenMatchModeSelector.getSelectedItem()))
					set.setSetting(TOKEN_MATCH_MODE_PARAMETER, ((String) this.tokenMatchModeSelector.getSelectedItem()));
				if ((this.bridgeableListName != null) && !lookupMode.startsWith("Token"))
					set.setSetting(BRIDGEABLE_LIST_PARAMETER, this.bridgeableListName);
			}
			if (!"".equals(attributeName))
				set.setSetting(ATTRIBUTE_NAME_PARAMETER, attributeName);
		}
	}
	
	private class MultiListEditorPanel extends JPanel {
		private JComboBox csvDataName;
		private JFileChooser csvDataChooser;
		private JTabbedPane columnLookupPanel = new JTabbedPane();
		private JTextField annotationType;
		private JPanel multiListPanel = new JPanel(new BorderLayout(), true);
		
		private boolean dirty = false;
		private String name;
		private Settings multiList;
		private String[] listNames;
		private GoldenGatePluginDataProvider dataProvider;
		
		private JTable csvDataTable = new JTable();
		private JScrollPane csvDataTableBox = new JScrollPane(this.csvDataTable);
		
		MultiListEditorPanel(String name, Settings set, String[] listNames, GoldenGatePluginDataProvider dataProvider) {
			super(new BorderLayout(), true);
			this.name = name;
			this.multiList = set;
			this.listNames = listNames;
			this.dataProvider = dataProvider;
			
			this.csvDataName = new JComboBox();
			this.csvDataName.setEditable(false);
			this.refreshCsvDataNames();
			this.csvDataName.setSelectedItem(this.multiList.getSetting(CSV_DATA_NAME_PARAMETER));
			this.csvDataName.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent ie) {
					setCsvDataName((String) csvDataName.getSelectedItem());
					dirty = true;
				}
			});
			JButton importCsvData = new JButton("Import");
			importCsvData.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					importCsvData();
				}
			});
//			String[] dataNames = this.dataProvider.getDataNames();
//			StringVector csvDataNames = new StringVector();
//			for (int d = 0; d < dataNames.length; d++) {
//				if (dataNames[d].endsWith(".csv"))
//					csvDataNames.addElementIgnoreDuplicates(dataNames[d]);
//			}
//			csvDataNames.sortLexicographically(false, false);
//			this.csvDataName = new JComboBox(csvDataNames.toStringArray());
//			this.csvDataName.setEditable(false);
//			this.csvDataName.setSelectedItem(this.multiList.getSetting(CSV_DATA_NAME_PARAMETER, ((dataNames.length == 0) ? "" : dataNames[0])));
//			this.csvDataName.addItemListener(new ItemListener() {
//				public void itemStateChanged(ItemEvent ie) {
//					setCsvDataName((String) csvDataName.getSelectedItem());
//					dirty = true;
//				}
//			});
			JPanel csvDataNamePanel = new JPanel(new BorderLayout(), true);
			csvDataNamePanel.add(new JLabel("CSV File To Use:", JLabel.LEFT), BorderLayout.WEST);
			csvDataNamePanel.add(this.csvDataName, BorderLayout.CENTER);
			csvDataNamePanel.add(importCsvData, BorderLayout.EAST);
			
			this.annotationType = new JTextField(set.getSetting(ANNOTATION_TYPE_PARAMETER, ""));
			this.annotationType.getDocument().addDocumentListener(new DocumentListener() {
				public void changedUpdate(DocumentEvent de) {}
				public void insertUpdate(DocumentEvent de) {
					dirty = true;
				}
				public void removeUpdate(DocumentEvent de) {
					dirty = true;
				}
			});
			JPanel annotationTypePanel = new JPanel(new BorderLayout(), true);
			annotationTypePanel.add(new JLabel("Annotation Type:", JLabel.LEFT), BorderLayout.WEST);
			annotationTypePanel.add(this.annotationType, BorderLayout.CENTER);
			
			this.multiListPanel.add(csvDataNamePanel, BorderLayout.NORTH);
			this.multiListPanel.add(this.columnLookupPanel, BorderLayout.CENTER);
			this.multiListPanel.add(annotationTypePanel, BorderLayout.SOUTH);
			
			this.add(this.multiListPanel, BorderLayout.NORTH);
			this.add(this.csvDataTableBox, BorderLayout.CENTER);
			
			String csvDataName = this.multiList.getSetting(CSV_DATA_NAME_PARAMETER);
			if (csvDataName != null) {
				if ((this.csvDataName.getItemCount() != 0) && csvDataName.equals(this.csvDataName.getSelectedItem()))
					this.setCsvDataName(csvDataName);
				else this.csvDataName.setSelectedItem(csvDataName);
			}
		}
		
		private void refreshCsvDataNames() {
			String[] dataNames = this.dataProvider.getDataNames();
			StringVector csvDataNames = new StringVector();
			for (int d = 0; d < dataNames.length; d++) {
				if (dataNames[d].endsWith(".csv"))
					csvDataNames.addElementIgnoreDuplicates(dataNames[d]);
			}
			csvDataNames.sortLexicographically(false, false);
			this.csvDataName.setModel(new DefaultComboBoxModel(csvDataNames.toStringArray()));
		}
		
		private void importCsvData() {
			if (this.csvDataChooser == null) {
				this.csvDataChooser = new JFileChooser();
				this.csvDataChooser.setAcceptAllFileFilterUsed(false);
				this.csvDataChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
				this.csvDataChooser.addChoosableFileFilter(new FileFilter() {
					public boolean accept(File file) {
						if (file.isDirectory())
							return true;
						String lcfn = file.getName().toLowerCase();
						return (lcfn.endsWith(".csv") || lcfn.endsWith(".tab") || lcfn.endsWith(".txt"));
					}
					public String getDescription() {
						return "Tabular Files (.csv, .tab, .txt)";
					}
				});
			}
			if (this.csvDataChooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
				return;
			
			File csvFile = this.csvDataChooser.getSelectedFile();
			if (csvFile == null)
				return;
			if (csvFile.getAbsolutePath().replaceAll("\\/\\\\\\.\\:", "").startsWith(this.dataProvider.getAbsolutePath().replaceAll("\\/\\\\\\.\\:", "")))
				return;
			String csvDataName = csvFile.getName();
			if (!csvDataName.toLowerCase().endsWith(".csv"))
				csvDataName += ".csv";
			if (this.dataProvider.isDataAvailable(csvDataName) && (JOptionPane.showConfirmDialog(this, ("The file '" + csvDataName + "' already exists - Replace it?"), "Replace File?", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE) != JOptionPane.OK_OPTION))
				return;
			
			try {
				InputStream csvIn = new BufferedInputStream(new FileInputStream(csvFile));
				OutputStream csvOut = new BufferedOutputStream(this.dataProvider.getOutputStream(csvDataName));
				byte[] csvBuffer = new byte[1024];
				for (int r; (r = csvIn.read(csvBuffer, 0, csvBuffer.length)) != -1;)
					csvOut.write(csvBuffer, 0, r);
				csvOut.flush();
				csvOut.close();
				csvIn.close();
				this.refreshCsvDataNames();
				this.csvDataName.setSelectedItem(csvDataName);
			}
			catch (IOException ioe) {
				JOptionPane.showMessageDialog(this, ("An error occurred while impoting file '" + csvFile.getAbsolutePath() + "':\n" + ioe.getMessage() + "\nSee log files for error details."), "Error Importing File", JOptionPane.ERROR_MESSAGE);
				System.out.println("Error importin CSV file '" + csvFile.getAbsolutePath() + "': " + ioe.getMessage());
				ioe.printStackTrace(System.out);
			}
		}
		
		private void setCsvDataName(String csvDataName) {
			final StringRelation lookupData;
			try {
				lookupData = readCsvData(csvDataName);
//				Reader csvIn = new InputStreamReader(this.dataProvider.getInputStream(csvDataName), "UTF-8");
//				lookupData = StringRelation.readCsvData(csvIn, '"');
//				csvIn.close();
//				BufferedReader csvIn = new BufferedReader(new InputStreamReader(this.dataProvider.getInputStream(csvDataName), "UTF-8"));
//				lookupData = StringRelation.readCsvData(csvIn, StringRelation.GUESS_SEPARATOR, StringRelation.DEFAULT_VALUE_DELIMITER);
//				csvIn.close();
//				csvIn.mark(8192); // buffered reader's default buffer size
//				String testLine = csvIn.readLine();
//				csvIn.reset();
//				int commaCount = 0;
//				int semicolonCount = 0;
//				for (int c = 0; c < testLine.length(); c++) {
//					char ch = testLine.charAt(c);
//					if (ch == ',')
//						commaCount++;
//					else if (ch == ';')
//						semicolonCount++;
//				}
//				if (semicolonCount >= commaCount)// convert MS Excel 'CSV'
//					csvIn = new BufferedReader(new ExcelCsvConverterReader(csvIn));
//				lookupData = StringRelation.readCsvData(csvIn, '"');
//				csvIn.close();
			}
			catch (IOException ioe) {
				JOptionPane.showMessageDialog(this, (csvDataName + " could not be read: " + ioe.getMessage()), "Error Loading CSV Data", JOptionPane.ERROR_MESSAGE);
				ioe.printStackTrace(System.out);
				return;
			}
			final StringVector columnNames = lookupData.getKeys();
			columnNames.sortLexicographically(false, false);
			
			this.columnLookupPanel.removeAll();
			for (int c = 0; c < columnNames.size(); c++) {
				String columnName = columnNames.get(c);
				Settings columnSettings = this.multiList.getSubset(columnName);
				String lookupMode = columnSettings.getSetting(LOOKUP_MODE_PARAMETER);
				boolean caseSensitive = CASE_SENSITIVE_TRUE.equals(columnSettings.getSetting(CASE_SENSITIVE_PARAMETER));
				String tokenMatchMode = columnSettings.getSetting(TOKEN_MATCH_MODE_PARAMETER, TOKEN_MATCH_MODE_EXACT);
				String attributeName = columnSettings.getSetting(ATTRIBUTE_NAME_PARAMETER);
				this.columnLookupPanel.addTab(columnName, new ColumnLookupPanel(columnName, lookupMode, caseSensitive, tokenMatchMode, columnSettings.getSetting(BRIDGEABLE_LIST_PARAMETER), this.listNames, attributeName));
			}
			
			if (this.annotationType.getText().trim().length() == 0)
				this.annotationType.setText(csvDataName.substring(0, (csvDataName.length() - ".csv".length())));
			
			this.csvDataTable.setModel(new TableModel() {
				public Class getColumnClass(int columnIndex) {
					return String.class;
				}
				public int getColumnCount() {
					return columnNames.size();
				}
				public String getColumnName(int columnIndex) {
					return columnNames.get(columnIndex);
				}
				public int getRowCount() {
					return lookupData.size();
				}
				public Object getValueAt(int rowIndex, int columnIndex) {
					StringTupel st = lookupData.get(rowIndex);
					return st.getValue(columnNames.get(columnIndex), "");
				}
				public boolean isCellEditable(int rowIndex, int columnIndex) {
					return false;
				}
				public void setValueAt(Object aValue, int rowIndex, int columnIndex) {}
				public void addTableModelListener(TableModelListener tml) {}
				public void removeTableModelListener(TableModelListener tml) {}
			});
			
			this.validate();
			this.repaint();
		}
		
		boolean isDirty() {
			for (int c = 0; c < this.columnLookupPanel.getTabCount(); c++) {
				ColumnLookupPanel clp = ((ColumnLookupPanel) this.columnLookupPanel.getComponentAt(c));
				if (clp.dirty)
					return true;
			}
			return this.dirty;
		}
		
		Settings getSettings() {
			Settings set = new Settings();
			String annotationType = this.annotationType.getText().trim();
			if (annotationType.length() != 0)
				set.setSetting(ANNOTATION_TYPE_PARAMETER, annotationType);
			set.setSetting(CSV_DATA_NAME_PARAMETER, ((String) this.csvDataName.getSelectedItem()));
			for (int c = 0; c < this.columnLookupPanel.getTabCount(); c++) {
				ColumnLookupPanel clp = ((ColumnLookupPanel) this.columnLookupPanel.getComponentAt(c));
				clp.addSettings(set);
			}
			return set;
		}
	}
}