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
package de.uka.ipd.idaho.goldenGate.plugin.regExes;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.regex.PatternSyntaxException;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import de.uka.ipd.idaho.easyIO.EasyIO;
import de.uka.ipd.idaho.easyIO.settings.Settings;
import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.util.swing.AnnotationDisplayDialog;
import de.uka.ipd.idaho.goldenGate.DocumentEditor;
import de.uka.ipd.idaho.goldenGate.GoldenGateConstants;
import de.uka.ipd.idaho.goldenGate.plugin.lists.ListManager;
import de.uka.ipd.idaho.goldenGate.plugins.AbstractAnnotationSourceManager;
import de.uka.ipd.idaho.goldenGate.plugins.AnnotationSource;
import de.uka.ipd.idaho.goldenGate.plugins.AnnotationSourceParameterPanel;
import de.uka.ipd.idaho.goldenGate.util.DataListListener;
import de.uka.ipd.idaho.goldenGate.util.DialogPanel;
import de.uka.ipd.idaho.goldenGate.util.EditFontsButton;
import de.uka.ipd.idaho.goldenGate.util.FontEditable;
import de.uka.ipd.idaho.goldenGate.util.FontEditorDialog;
import de.uka.ipd.idaho.goldenGate.util.FontEditorPanel;
import de.uka.ipd.idaho.goldenGate.util.ResourceSelector;
import de.uka.ipd.idaho.stringUtils.StringVector;
import de.uka.ipd.idaho.stringUtils.regExUtils.RegExUtils;

/**
 * Manager for regular expressions as annotation sources. Regular expression
 * patterns can be used to find and annotate matching character sequences in a
 * document. To facilitate using regular expression patterns on dirty text,
 * whitespaces are normalized on matching, into a single space or line break at
 * most.<br>
 * <br>
 * All configuration can be done in the 'Edit Reg Exes' dialog in the GoldenGATE
 * Editor, which provides an editor for Java regular expression patterns. To
 * facilitate mastering highly complex regular expression patterns, this editor
 * allows for including existing regular expression patterns into others, using
 * '&lt;&lt;regExName&gt;&gt;' (name of the referenced pattern in double angle
 * brackets) to reference these imported patterns. Imports are resolved
 * recursively, and it is no problem to reference the same sub pattern more than
 * one in the same pattern. Only circular references are prohibited. This way,
 * highly complex patterns can be built from smaller partial patterns, reducing
 * complexity.<br>
 * <br>
 * This plugin requires the ListManager plugin to be present in the GoldenGATE
 * Editor as well.
 * 
 * @author sautter
 */
public class RegExManager extends AbstractAnnotationSourceManager {
	
	private static final String AD_HOC_REGEX_NAME = "AdHoc";
	
	private static final String FILE_EXTENSION = ".regEx";
	
	private static final String[] FIX_REGEX_NAMES = {
			"<Word>", 
			"<Lower Case Word>", 
			"<Capitalized Word>", 
			"<Upper Case Word>", 
			"<Abbreviation>", 
			"<Long Abbreviation>", 
			"<Lower Case Abbreviation>", 
			"<Lower Case Long Abbreviation>", 
			"<Capitalized Abbreviation>",  
			"<Capitalized Long Abbreviation>", 
			"<First Name>", 
			"<Last Name>", 
			"<Person Name>",
		};
	private static final String[] FIX_REGEXES = {
			RegExUtils.WORD, 
			RegExUtils.LOWER_CASE_WORD, 
			RegExUtils.CAPITALIZED_WORD, 
			RegExUtils.UPPER_CASE_WORD, 
			RegExUtils.ABBREVIATION, 
			RegExUtils.LONG_ABBREVIATION, 
			RegExUtils.LOWER_CASE_ABBREVIATION, 
			RegExUtils.LOWER_CASE_LONG_ABBREVIATION, 
			RegExUtils.CAPITALIZED_ABBREVIATION, 
			RegExUtils.CAPITALIZED_LONG_ABBREVIATION, 
			RegExUtils.FIRST_NAME, 
			RegExUtils.LAST_NAME, 
			RegExUtils.PERSON_NAME,
		};
	
	private StringVector adHocRegExHistory = new StringVector();
	private int adHocRegExHistorySize = 20;
	
	private ListManager listProvider;
	private JFileChooser fileChooser = null;
	
	public RegExManager() {}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#init()
	 */
	public void init() {
		this.listProvider = ((ListManager) this.parent.getAnnotationSourceProvider(ListManager.class.getName()));
		StringVector adHocRegExHistory = this.loadListResource("adHocRegExHistory.cnfg");
		if (adHocRegExHistory != null)
			this.adHocRegExHistory.addContent(adHocRegExHistory);
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#exit()
	 */
	public void exit() {
		try {
			this.storeListResource("adHocRegExHistory.cnfg", this.adHocRegExHistory);
		} catch (IOException ioe) {}
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.ResourceManager#isOperational()
	 */
	public boolean isOperational() {
		return (super.isOperational() && (this.listProvider != null));
	}
	
	/* test a regular expression
	 * @param	regEx	the regular expression to test
	 * @return the Annotations extracted from the currently selected Document by the specified regular expression
	 */
	private Annotation[] testRegEx(String regEx) {
		QueriableAnnotation data = this.parent.getActiveDocument();
		return ((data == null) ? null : Gamta.extractAllMatches(data, regEx, 20));
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#getToolsMenuFunctionItems(de.uka.ipd.idaho.goldenGate.GoldenGateConstants.InvokationTargetProvider)
	 */
	public JMenuItem[] getToolsMenuFunctionItems(final InvokationTargetProvider targetProvider) {
		ArrayList collector = new ArrayList();
		JMenuItem mi;
		
		mi = new JMenuItem("Ad Hoc (enter and apply)");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				applyAdHocRegEx(targetProvider.getFunctionTarget());
			}
		});
		collector.add(mi);
		return ((JMenuItem[]) collector.toArray(new JMenuItem[collector.size()]));
	}
	
	private void applyAdHocRegEx(DocumentEditor target) {
		if (target == null)
			target = this.parent.getActivePanel();
		if (target != null)
			this.applyAnnotationSource(AD_HOC_REGEX_NAME, target, new Properties());
	}
	
	/* 
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AnnotationSourceManager#createAnnotationSource()
	 */
	public String createAnnotationSource() {
		return this.createRegEx(null, null);
	}
	
	/* 
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AnnotationSourceManager#editAnnotationSource(java.lang.String)
	 */
	public void editAnnotationSource(String name) {
		String regEx = this.loadStringResource(name);
		if ((regEx == null) || (regEx.length() == 0)) return;
		
		EditRegExDialog ered = new EditRegExDialog(name, regEx);
		ered.setVisible(true);
		
		if (ered.isCommitted()) try {
			this.storeStringResource(name, ered.getRegEx());
		} catch (IOException ioe) {}
	}
	
	/* 
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AnnotationSourceManager#editAnnotationSources()
	 */
	public void editAnnotationSources() {
		this.editRegExes();
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AnnotationSourceManager#getAnnotationSource(java.lang.String)
	 */
	public AnnotationSource getAnnotationSource(String name) {
		if (AD_HOC_REGEX_NAME.equals(name))
			return this.getAdHocRegExAnnotationSource();
		String regEx = this.getRegEx(name);
		return ((regEx == null) ? null : new RegExAnnotationSource(name, regEx, this.listProvider));
	}
	
	private RegExAnnotationSource getAdHocRegExAnnotationSource() {
		AdHocRegExDialog ahred = new AdHocRegExDialog();
		ahred.setLocationRelativeTo(DialogPanel.getTopWindow());
		ahred.setVisible(true);
		if (ahred.isCommitted()) {
			String regEx = ahred.getRegEx();
			return ((regEx.trim().length() == 0) ? null : new RegExAnnotationSource("Ad-Hoc RegEx", regEx, this.listProvider));
		}
		else return null;
	}
	
	private class AdHocRegExDialog extends DialogPanel {
		private RegExEditorPanel editor;
		private String regEx = null;
		
		AdHocRegExDialog() {
			super("Enter Ad-Hoc RegEx", true);
			
			final JComboBox adHocRegExSelector = new JComboBox();
			adHocRegExSelector.setModel(new DefaultComboBoxModel(adHocRegExHistory.toStringArray()));
			adHocRegExSelector.setEditable(false);
			adHocRegExSelector.setSelectedItem(adHocRegExHistory.isEmpty() ? "" : adHocRegExHistory.get(0));
			adHocRegExSelector.setBorder(BorderFactory.createLoweredBevelBorder());
			adHocRegExSelector.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent ie) {
					editor.setContent(adHocRegExSelector.getSelectedItem().toString());
				}
			});
			
			JPanel adHocRegExPanel = new JPanel(new BorderLayout());
			adHocRegExPanel.add(new JLabel("Recently Used: "), BorderLayout.WEST);
			adHocRegExPanel.add(adHocRegExSelector, BorderLayout.CENTER);
			
			//	initialize main buttons
			JButton commitButton = new JButton("Apply");
			commitButton.setBorder(BorderFactory.createRaisedBevelBorder());
			commitButton.setPreferredSize(new Dimension(100, 21));
			commitButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					commit();
				}
			});
			
			JButton abortButton = new JButton("Cancel");
			abortButton.setBorder(BorderFactory.createRaisedBevelBorder());
			abortButton.setPreferredSize(new Dimension(100, 21));
			abortButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					abort();
				}
			});
			
			JPanel mainButtonPanel = new JPanel(new FlowLayout());
			mainButtonPanel.add(commitButton);
			mainButtonPanel.add(abortButton);
			
			//	initialize editor
			this.editor = new RegExEditorPanel("AdHoc", "", this);
			
			//	put the whole stuff together
			this.setLayout(new BorderLayout());
			this.add(adHocRegExPanel, BorderLayout.NORTH);
			this.add(this.editor, BorderLayout.CENTER);
			this.add(mainButtonPanel, BorderLayout.SOUTH);
			
			this.setResizable(true);
			this.setSize(new Dimension(600, 400));
			this.setLocationRelativeTo(DialogPanel.getTopWindow());
		}
		
		boolean isCommitted() {
			return (this.regEx != null);
		}
		
		String getRegEx() {
			return this.regEx;
		}
		
		void abort() {
			this.regEx = null;
			this.dispose();
		}

		void commit() {
			this.regEx = this.editor.getRegEx();
			if (this.regEx.trim().length() == 0)
				this.regEx = null;
			else {
				try {
					"".matches(RegExUtils.normalizeRegEx(this.regEx));
				} catch (PatternSyntaxException pse) {
					JOptionPane.showMessageDialog(this, ("The regulare expression you entered is not a valid pattern:\n" + pse.getMessage()), "RegEx Validation Failed", JOptionPane.ERROR_MESSAGE);
					return;
				}
				adHocRegExHistory.removeAll(this.regEx);
				adHocRegExHistory.insertElementAt(this.regEx, 0);
				while (adHocRegExHistory.size() > adHocRegExHistorySize)
					adHocRegExHistory.removeElementAt(adHocRegExHistorySize);
			}
			this.dispose();
		}
	}
	
	private class RegExAnnotationSource implements AnnotationSource {
		
		private static final String MAX_TOKENS_ATTRIBUTE_NAME = "MAX_TOKENS";
		private static final String START_EXCLUDE_LIST_ATTRIBUTE_NAME = "START_EXCLUDE";
		private static final String EXCLUDE_LIST_ATTRIBUTE_NAME = "EXCLUDE_LIST";
		private static final String IGNORE_LINEBREAKS_ATTRIBUTE_NAME = "IGNORE_LINE_BREAKS";
		private static final String ALLOW_OVERLAP_ATTRIBUTE_NAME = "ALLOW_OVERLAP";
		
		private String name;
		private String regEx;
		private ListManager listProvider;
		
		RegExAnnotationSource(String name, String regEx, ListManager listProvider) {
			this.name = name;
			this.regEx = regEx;
			this.listProvider = listProvider;
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.AnnotationSource#getName()
		 */
		public String getName() {
			return this.name;
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.AnnotationSource#getTypeLabel()
		 */
		public String getTypeLabel() {
			return "Regular Expression";
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.AnnotationSource#getProviderClassName()
		 */
		public String getProviderClassName() {
			return RegExManager.class.getName();
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.AnnotationSource#annotate(de.uka.ipd.idaho.gamta.MutableAnnotation)
		 */
		public Annotation[] annotate(MutableAnnotation data) {
			return this.annotate(data, null);
		}

		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.AnnotationSource#annotate(de.uka.ipd.idaho.gamta.MutableAnnotation, java.util.Properties)
		 */
		public Annotation[] annotate(MutableAnnotation data, Properties parameters) {
			if (parameters == null) return Gamta.extractAllMatches(data, this.regEx);
			else {
				int maxTokens = 0;
				try {
					maxTokens = Integer.parseInt(parameters.getProperty(MAX_TOKENS_ATTRIBUTE_NAME, "0"));
				} catch (Exception e) {}
				boolean allowOverlap = TRUE.equals(parameters.getProperty(ALLOW_OVERLAP_ATTRIBUTE_NAME, FALSE));
				boolean ignoreLineBreaks = TRUE.equals(parameters.getProperty(IGNORE_LINEBREAKS_ATTRIBUTE_NAME, FALSE));
				StringVector startExclude = this.listProvider.getList(parameters.getProperty(START_EXCLUDE_LIST_ATTRIBUTE_NAME));
				if (startExclude == null) startExclude = new StringVector();
				StringVector exclude = this.listProvider.getList(parameters.getProperty(EXCLUDE_LIST_ATTRIBUTE_NAME));
				if (exclude == null) exclude = new StringVector();
				return Gamta.extractAllMatches(data, this.regEx, maxTokens, startExclude, exclude, allowOverlap, ignoreLineBreaks);
			}
		}
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AnnotationSourceManager#getAnnotatorParameterPanel()
	 */
	public AnnotationSourceParameterPanel getAnnotatorParameterPanel() {
		return new RegExParameterPanel(this.listProvider);
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AnnotationSourceManager#getAnnotatorParameterPanel(de.uka.ipd.idaho.easyIO.settings.Settings)
	 */
	public AnnotationSourceParameterPanel getAnnotatorParameterPanel(Settings settings) {
		return new RegExParameterPanel(this.listProvider, settings);
	}
	
	private class RegExParameterPanel extends AnnotationSourceParameterPanel {
		
		private JTextField maxTokensInput;
		private ResourceSelector startExclude;
		private String startExcludeInit = null; 
		private ResourceSelector exclude;
		private String excludeInit = null; 
		private JCheckBox allowOverlap;
		private JCheckBox ignoreLinebreaks;
		private boolean dirty = false;
		
		RegExParameterPanel(ListManager listProvider) {
			this(listProvider, null);
		}
		
		RegExParameterPanel(ListManager listProvider, Settings settings) {
			super(new GridBagLayout());
			
			this.maxTokensInput = new JTextField("0");
			if (settings != null) this.maxTokensInput.setText(settings.getSetting(RegExAnnotationSource.MAX_TOKENS_ATTRIBUTE_NAME, "0"));
			this.maxTokensInput.getDocument().addDocumentListener(new DocumentListener() {
				public void changedUpdate(DocumentEvent de) {}
				public void insertUpdate(DocumentEvent de) {
					dirty = true;
				}
				public void removeUpdate(DocumentEvent de) {
					dirty = true;
				}
			});
			
			this.startExcludeInit = ((settings == null) ? null : settings.getSetting(RegExAnnotationSource.START_EXCLUDE_LIST_ATTRIBUTE_NAME));
			this.startExclude = listProvider.getSelector("Start Exclude", this.startExcludeInit);
			
			this.excludeInit = ((settings == null) ? null : settings.getSetting(RegExAnnotationSource.EXCLUDE_LIST_ATTRIBUTE_NAME));
			this.exclude = listProvider.getSelector("Exclude", this.excludeInit);
			
			this.allowOverlap = new JCheckBox("Overlapping Matches", false);
			if (settings != null) this.allowOverlap.setSelected(RegExAnnotationSource.TRUE.equals(settings.getSetting(RegExAnnotationSource.ALLOW_OVERLAP_ATTRIBUTE_NAME, RegExAnnotationSource.FALSE)));
			this.allowOverlap.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent ie) {
					dirty = true;
				}
			});
			
			this.ignoreLinebreaks = new JCheckBox("Ignore Linebreaks", false);
			if (settings != null) this.ignoreLinebreaks.setSelected(RegExAnnotationSource.TRUE.equals(settings.getSetting(RegExAnnotationSource.IGNORE_LINEBREAKS_ATTRIBUTE_NAME, RegExAnnotationSource.FALSE)));
			this.ignoreLinebreaks.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent ie) {
					dirty = true;
				}
			});
			
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridy = 0;
			gbc.weighty = 0;
			gbc.gridheight = 1;
			gbc.insets.top = 3;
			gbc.insets.left = 5;
			gbc.insets.right = 5;
			gbc.insets.bottom = 3;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			
			gbc.gridx = 0;
			gbc.gridwidth = 1;
			gbc.weightx = 0;
			this.add(new JLabel("Max Tokens"), gbc.clone());
			
			gbc.gridx = 1;
			gbc.weightx = 1;
			this.add(this.maxTokensInput, gbc.clone());
			
			gbc.gridx = 0;
			gbc.gridwidth = 2;
			gbc.weightx = 1;
			
			gbc.gridy ++;
			this.add(this.startExclude, gbc.clone());
			
			gbc.gridy ++;
			this.add(this.exclude, gbc.clone());
			
			gbc.gridy ++;
			gbc.gridwidth = 1;
			this.add(this.allowOverlap, gbc.clone());
			gbc.gridx = 1;
			this.add(this.ignoreLinebreaks, gbc.clone());
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.AnnotationSourceParameterPanel#isDirty()
		 */
		public boolean isDirty() {
			String startExcludeListName = this.startExclude.getSelectedResourceName();
			if (this.startExcludeInit == null) {
				if (startExcludeListName != null) return true; 
			} else if (!this.startExcludeInit.equals(startExcludeListName)) return true;
			
			String excludeListName = this.exclude.getSelectedResourceName();
			if (this.excludeInit == null) {
				if (excludeListName != null) return true; 
			} else if (!this.excludeInit.equals(excludeListName)) return true;
			
			return this.dirty;
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.AnnotationSourceParameterPanel#getSetting(java.lang.String)
		 */
		public Settings getSettings() {
			Settings set = new Settings();
			
			set.setSetting(RegExAnnotationSource.MAX_TOKENS_ATTRIBUTE_NAME, this.maxTokensInput.getText());
			
			String startExcludeListName = this.startExclude.getSelectedResourceName();
			if (startExcludeListName != null) set.setSetting(RegExAnnotationSource.START_EXCLUDE_LIST_ATTRIBUTE_NAME, startExcludeListName);
			
			String excludeListName = this.exclude.getSelectedResourceName();
			if (excludeListName != null) set.setSetting(RegExAnnotationSource.EXCLUDE_LIST_ATTRIBUTE_NAME, excludeListName);
			
			set.setSetting(RegExAnnotationSource.ALLOW_OVERLAP_ATTRIBUTE_NAME, (this.allowOverlap.isSelected() ? AnnotationSource.TRUE : AnnotationSource.FALSE));
			
			set.setSetting(RegExAnnotationSource.IGNORE_LINEBREAKS_ATTRIBUTE_NAME, (this.ignoreLinebreaks.isSelected() ? AnnotationSource.TRUE : AnnotationSource.FALSE));
			
			return set;
		}
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AnnotationSourceManager#getResourceNames()
	 */
	public String[] getResourceNames() {
		StringVector regExes = new StringVector();
		regExes.addContentIgnoreDuplicates(FIX_REGEX_NAMES);
		regExes.addContentIgnoreDuplicates(super.getResourceNames());
		return regExes.toStringArray();
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.ResourceManager#getResourceTypeLabel()
	 */
	public String getResourceTypeLabel() {
		return "Regular Expression";
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager#getFileExtension()
	 */
	protected String getFileExtension() {
		return FILE_EXTENSION;
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AnnotationSourceManager#getMainMenuItems()
	 */
	public JMenuItem[] getMainMenuItems() {
		ArrayList collector = new ArrayList();
		JMenuItem mi;
		
		if (this.dataProvider.isDataEditable()) {
			mi = new JMenuItem("Create");
			mi.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					createRegEx();
				}
			});
			collector.add(mi);
			mi = new JMenuItem("Load");
			mi.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					loadRegEx();
				}
			});
			collector.add(mi);
			mi = new JMenuItem("Edit");
			mi.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					editRegExes();
				}
			});
			collector.add(mi);
			collector.add(GoldenGateConstants.MENU_SEPARATOR_ITEM);
		}
		
		mi = new JMenuItem("Ad Hoc (enter and apply)");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				applyAdHocRegEx(null);
			}
		});
		collector.add(mi);
		
		return ((JMenuItem[]) collector.toArray(new JMenuItem[collector.size()]));
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#getMainMenuTitle()
	 */
	public String getMainMenuTitle() {
		return "Regular Expressions";
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager#getToolsMenuLabel()
	 */
	public String getToolsMenuLabel() {
		return "Apply";
	}
	
	private String getRegEx(String name) {
		if (name == null) return null;
		
		for (int r = 0; r < FIX_REGEX_NAMES.length; r++) 
			if (name.equals(FIX_REGEX_NAMES[r])) return FIX_REGEXES[r];
		
		return this.loadStringResource(name);
	}
	
	private boolean createRegEx() {
		return (this.createRegEx(null, null) != null);
	}
	
	private boolean loadRegEx() {
		if (this.fileChooser == null) try {
			this.fileChooser = new JFileChooser();
		} catch (SecurityException se) {}
		
		if ((this.fileChooser != null) && (this.fileChooser.showOpenDialog(DialogPanel.getTopWindow()) == JFileChooser.APPROVE_OPTION)) {
			File file = this.fileChooser.getSelectedFile();
			if ((file != null) && file.isFile()) {
				try {
					String fileName = file.toString();
					fileName = fileName.substring(fileName.lastIndexOf(File.separatorChar) + 1);
					return (this.createRegEx(EasyIO.readFile(file), fileName) != null);
				} catch (IOException ioe) {
					JOptionPane.showMessageDialog(DialogPanel.getTopWindow(), (ioe.getClass().getName() + " (" + ioe.getMessage() + ")\nwhile trying to load " + file.toString()), "Could Not Read File", JOptionPane.ERROR_MESSAGE);
				}
			}
		}
		return false;
	}
	
	private boolean cloneRegEx() {
		String selectedName = this.resourceNameList.getSelectedName();
		if (selectedName == null)
			return this.createRegEx();
		else {
			String name = "New " + selectedName;
			return (this.createRegEx(this.loadStringResource(selectedName), name) != null);
		}
	}
	
	private String createRegEx(String modelRegEx, String name) {
		CreateRegExDialog cred = new CreateRegExDialog(name, modelRegEx);
		cred.setVisible(true);
		if (cred.isCommitted()) {
			String regEx = cred.getRegEx();
			String regExName = cred.getRegExName();
			if (!regExName.endsWith(FILE_EXTENSION)) regExName += FILE_EXTENSION;
			try {
				if (this.storeStringResource(regExName, regEx)) {
					this.resourceNameList.refresh();
					return regExName;
				}
			} catch (IOException e) {}
		}
		return null;
	}
	
	private void editRegExes() {
		final RegExEditorPanel[] editor = new RegExEditorPanel[1];
		editor[0] = null;
		
		final DialogPanel editDialog = new DialogPanel("Edit Regular Expressions", true);
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
						storeStringResource(editor[0].regExName, editor[0].getRegEx());
					} catch (IOException ioe) {}
				}
				if (editDialog.isVisible()) editDialog.dispose();
			}
		});
		
		JPanel editButtons = new JPanel(new FlowLayout(FlowLayout.CENTER));
		JButton button;
		button = new JButton("Create");
		button.setBorder(BorderFactory.createRaisedBevelBorder());
		button.setPreferredSize(new Dimension(100, 21));
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				createRegEx();
			}
		});
		editButtons.add(button);
		button = new JButton("Load");
		button.setBorder(BorderFactory.createRaisedBevelBorder());
		button.setPreferredSize(new Dimension(100, 21));
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				loadRegEx();
			}
		});
		editButtons.add(button);
		button = new JButton("Clone");
		button.setBorder(BorderFactory.createRaisedBevelBorder());
		button.setPreferredSize(new Dimension(100, 21));
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				cloneRegEx();
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
			String regEx = this.loadStringResource(selectedName);
			if (regEx == null)
				editorPanel.add(this.getExplanationLabel(), BorderLayout.CENTER);
			else {
				editor[0] = new RegExEditorPanel(selectedName, regEx, editDialog);
				editorPanel.add(editor[0], BorderLayout.CENTER);
			}
		}
		editDialog.add(editorPanel, BorderLayout.CENTER);
		
		editDialog.add(this.resourceNameList, BorderLayout.EAST);
		DataListListener dll = new DataListListener() {
			public void selected(String dataName) {
				if ((editor[0] != null) && editor[0].isDirty()) {
					try {
						storeStringResource(editor[0].regExName, editor[0].getRegEx());
					}
					catch (IOException ioe) {
						if (JOptionPane.showConfirmDialog(editDialog, (ioe.getClass().getName() + " (" + ioe.getMessage() + ")\nwhile saving file to " + editor[0].regExName + "\nProceed?"), "Could Not Save Analyzer", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
							resourceNameList.setSelectedName(editor[0].regExName);
							editorPanel.validate();
							return;
						}
					}
				}
				editorPanel.removeAll();
				
				if (dataName == null)
					editorPanel.add(getExplanationLabel(), BorderLayout.CENTER);
				else {
					String regEx = loadStringResource(dataName);
					if (regEx == null)
						editorPanel.add(getExplanationLabel(), BorderLayout.CENTER);
					else {
						editor[0] = new RegExEditorPanel(dataName, regEx, editDialog);
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
	
	private class CreateRegExDialog extends DialogPanel {
		
		private JTextField nameField;
		
		private RegExEditorPanel editor;
		private String regEx = null;
		private String regExName = null;
		
		CreateRegExDialog(String name, String regEx) {
			super("Create RegEx", true);
			
			this.nameField = new JTextField((name == null) ? "New RegEx" : name);
			this.nameField.setBorder(BorderFactory.createLoweredBevelBorder());
			
			//	initialize main buttons
			JButton commitButton = new JButton("Create");
			commitButton.setBorder(BorderFactory.createRaisedBevelBorder());
			commitButton.setPreferredSize(new Dimension(100, 21));
			commitButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					commit();
				}
			});
			
			JButton abortButton = new JButton("Cancel");
			abortButton.setBorder(BorderFactory.createRaisedBevelBorder());
			abortButton.setPreferredSize(new Dimension(100, 21));
			abortButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					abort();
				}
			});
			
			JPanel mainButtonPanel = new JPanel(new FlowLayout());
			mainButtonPanel.add(commitButton);
			mainButtonPanel.add(abortButton);
			
			//	initialize editor
			this.editor = new RegExEditorPanel(name, regEx, this);
			
			//	put the whole stuff together
			this.setLayout(new BorderLayout());
			this.add(this.nameField, BorderLayout.NORTH);
			this.add(this.editor, BorderLayout.CENTER);
			this.add(mainButtonPanel, BorderLayout.SOUTH);
			
			this.setResizable(true);
			this.setSize(new Dimension(600, 400));
			this.setLocationRelativeTo(DialogPanel.getTopWindow());
		}
		
		boolean isCommitted() {
			return (this.regEx != null);
		}
		
		String getRegEx() {
			return this.regEx;
		}
		
		String getRegExName() {
			return this.regExName;
		}
		
		void abort() {
			this.regEx = null;
			this.regExName = null;
			this.dispose();
		}

		void commit() {
			this.regEx = this.editor.getRegEx();
			this.regExName = this.nameField.getText();
			this.dispose();
		}
	}

	private class EditRegExDialog extends DialogPanel {
		
		private RegExEditorPanel editor;
		private String regEx = null;
		
		EditRegExDialog(String name, String regEx) {
			super(("Edit RegEx '" + name + "'"), true);
			
			//	initialize main buttons
			JButton commitButton = new JButton("OK");
			commitButton.setBorder(BorderFactory.createRaisedBevelBorder());
			commitButton.setPreferredSize(new Dimension(100, 21));
			commitButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					EditRegExDialog.this.regEx = editor.getRegEx();
					dispose();
				}
			});
			
			JButton abortButton = new JButton("Cancel");
			abortButton.setBorder(BorderFactory.createRaisedBevelBorder());
			abortButton.setPreferredSize(new Dimension(100, 21));
			abortButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					EditRegExDialog.this.regEx = null;
					dispose();
				}
			});
			
			JPanel mainButtonPanel = new JPanel(new FlowLayout());
			mainButtonPanel.add(commitButton);
			mainButtonPanel.add(abortButton);
			
			//	initialize editor
			this.editor = new RegExEditorPanel(name, regEx, this);
			
			//	put the whole stuff together
			this.setLayout(new BorderLayout());
			this.add(this.editor, BorderLayout.CENTER);
			this.add(mainButtonPanel, BorderLayout.SOUTH);
			
			this.setResizable(true);
			this.setSize(new Dimension(600, 400));
			this.setLocationRelativeTo(DialogPanel.getTopWindow());
		}
		
		boolean isCommitted() {
			return (this.regEx != null);
		}
		
		String getRegEx() {
			return this.regEx;
		}
	}
	
	private class RegExEditorPanel extends JPanel implements FontEditable, DocumentListener {
		
		private static final String VALIDATOR = "";
		private static final int MAX_SCROLLBAR_WAIT = 200;
		
		private JTextArea editor;
		private JScrollPane editorBox;
		
		private String content = "";
		
		private String fontName = "Verdana";
		private int fontSize = 12;
		private Color fontColor = Color.BLACK;
		
		private boolean dirty = false;
		private String regExName;
		
		private DialogPanel frame;
		
		RegExEditorPanel(String name, String regEx, DialogPanel frame) {
			super(new BorderLayout(), true);
			this.regExName = name;
			this.frame = frame;
			
			//	initialize editor
			this.editor = new JTextArea();
			this.editor.setEditable(true);
			
			//	wrap editor in scroll pane
			this.editorBox = new JScrollPane(this.editor);
			
			//	initialize buttons
			JButton refreshButton = new JButton("Refresh");
			refreshButton.setBorder(BorderFactory.createRaisedBevelBorder());
			refreshButton.setPreferredSize(new Dimension(115, 21));
			refreshButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					refreshRegEx();
				}
			});
			
			JButton validateButton = new JButton("Validate");
			validateButton.setBorder(BorderFactory.createRaisedBevelBorder());
			validateButton.setPreferredSize(new Dimension(115, 21));
			validateButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					validateRegEx();
				}
			});
			
			JButton testButton = new JButton("Test");
			testButton.setBorder(BorderFactory.createRaisedBevelBorder());
			testButton.setPreferredSize(new Dimension(115, 21));
			testButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					testRegEx();
				}
			});
			
			JButton enumerationButton = new JButton("Enumeration");
			enumerationButton.setBorder(BorderFactory.createRaisedBevelBorder());
			enumerationButton.setPreferredSize(new Dimension(115, 21));
			enumerationButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					produceEnumeration();
				}
			});
			
			JPanel buttonPanel = new JPanel(new GridBagLayout(), true);
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.insets.top = 3;
			gbc.insets.bottom = 3;
			gbc.insets.left = 3;
			gbc.insets.right = 3;
			gbc.weighty = 0;
			gbc.weightx = 1;
			gbc.gridheight = 1;
			gbc.gridwidth = 1;
			gbc.fill = GridBagConstraints.BOTH;
			gbc.gridy = 0;
			gbc.gridx = 0;
			buttonPanel.add(this.getEditFontsButton(new Dimension(100, 21)), gbc.clone());
			gbc.gridx ++;
			buttonPanel.add(refreshButton, gbc.clone());
			gbc.gridx ++;
			buttonPanel.add(validateButton, gbc.clone());
			gbc.gridx ++;
			buttonPanel.add(testButton, gbc.clone());
			gbc.gridx ++;
			buttonPanel.add(enumerationButton, gbc.clone());
			
			//	put the whole stuff together
			this.add(this.editorBox, BorderLayout.CENTER);
			this.add(buttonPanel, BorderLayout.SOUTH);
			this.setContent((regEx == null) ? "" : regEx);
		}
		
		String getRegEx() {
			if (this.isDirty()) this.content = RegExUtils.normalizeRegEx(this.editor.getText());
			return this.content;
		}
		
		void setContent(String regEx) {
			this.content = RegExUtils.normalizeRegEx(regEx);
			this.refreshDisplay();
			this.dirty = false;
		}
		
		boolean isDirty() {
			return this.dirty;
		}
		
		void refreshRegEx() {
			String regEx = this.editor.getText();
			if ((regEx != null) && (regEx.length() != 0)) {
				
				final Point viewPosition = this.editorBox.getViewport().getViewPosition();
				
				String normalizedRegEx = RegExUtils.normalizeRegEx(regEx);
				this.editor.getDocument().removeDocumentListener(this);
				this.editor.setText(RegExUtils.explodeRegEx(normalizedRegEx, "  "));
				this.editor.getDocument().addDocumentListener(this);
				
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						try {
							editorBox.getViewport().setViewPosition(viewPosition);
							validate();
						} catch (RuntimeException re) {}
					}
				});
			}
		}
		
		void validateRegEx() {
			boolean selected = true;
			String regEx = this.editor.getSelectedText();
			if ((regEx == null) || (regEx.length() == 0)) {
				regEx = this.editor.getText();
				selected = false;
			}
			if (!this.validateRegEx(regEx)) JOptionPane.showMessageDialog(this, "The " + (selected ? "selected pattern part" : "pattern") + " is not a valid pattern.", "RegEx Validation", JOptionPane.ERROR_MESSAGE);
			else JOptionPane.showMessageDialog(this, "The " + (selected ? "selected pattern part" : "pattern") + " is a valid pattern.", "RegEx Validation", JOptionPane.INFORMATION_MESSAGE);
		}
		
		void testRegEx() {
			boolean selected = true;
			String regEx = this.editor.getSelectedText();
			if ((regEx == null) || (regEx.length() == 0)) {
				regEx = this.editor.getText();
				selected = false;
			}
			regEx = regEx.trim();
			regEx = RegExUtils.normalizeRegEx(regEx);
			if (!this.validateRegEx(regEx))
				JOptionPane.showMessageDialog(this, "The " + (selected ? "selected pattern part" : "pattern") + " is not a valid pattern.", "RegEx Validation", JOptionPane.ERROR_MESSAGE);
			else {
				Annotation[] annotations = RegExManager.this.testRegEx(regEx);
				if (annotations != null) {
					AnnotationDisplayDialog add = new AnnotationDisplayDialog(this.frame.getDialog(), "Matches of RegEx", annotations, true);
					add.setLocationRelativeTo(this);
					add.setVisible(true);
				}
			}
		}
		
		void produceEnumeration() {
			boolean selected = true;
			String regEx = this.editor.getSelectedText();
			if ((regEx == null) || (regEx.length() == 0)) {
				regEx = this.editor.getText();
				selected = false;
			}
			if (this.validateRegEx(regEx) || (JOptionPane.showConfirmDialog(this, "The " + (selected ? "selected pattern part" : "pattern") + " is not a valid pattern.\nProduce enumeration expression anyway?", "RegEx Validation", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)) {
				boolean allowCommaOnly = (JOptionPane.showConfirmDialog(this, "Should the produced expression match enumerations whose elements are separated by commas only?\n(Selecting NO will produce an expression requiring the last two elements separated by a preposition)", "Allow Comma Only", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION);
				String enumEx = RegExUtils.produceEnumerationGroup(RegExUtils.normalizeRegEx(regEx), allowCommaOnly);
				if (selected) {
					this.editor.replaceSelection(RegExUtils.explodeRegEx(enumEx));
					this.content = RegExUtils.normalizeRegEx(this.editor.getText());
				} else {
					this.content = enumEx;
				}
				this.refreshDisplay();
			}
		}
		
		boolean validateRegEx(String regEx) {
			try {
				VALIDATOR.matches(RegExUtils.normalizeRegEx(regEx));
				return true;
			} catch (PatternSyntaxException pse) {
				return false;
			}
		}
		
		void refreshDisplay() {
			final JScrollBar scroller = this.editorBox.getVerticalScrollBar();
			final int scrollPosition = scroller.getValue();
			
			this.editor.getDocument().removeDocumentListener(this);
			this.editor.setFont(new Font(this.fontName, Font.PLAIN, this.fontSize));
			this.editor.setText(RegExUtils.explodeRegEx(this.content, "  "));
			this.editor.getDocument().addDocumentListener(this);
			
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					int scrollbarWaitCounter = 0;
					while (scroller.getValueIsAdjusting() && (scrollbarWaitCounter < MAX_SCROLLBAR_WAIT)) try {
						Thread.sleep(10);
						scrollbarWaitCounter ++;
					} catch (Exception e) {}
					
					if (scrollbarWaitCounter < MAX_SCROLLBAR_WAIT) {
						scroller.setValueIsAdjusting(true);
						scroller.setValue(scrollPosition);
						scroller.setValueIsAdjusting(false);
					}
					validate();
				}
			});
		}
		
		/*
		 * @see javax.swing.event.DocumentListener#changedUpdate(javax.swing.event.DocumentEvent)
		 */
		public void changedUpdate(DocumentEvent e) {
			//	attribute changes are not of interest for now
		}
		
		/*
		 * @see javax.swing.event.DocumentListener#insertUpdate(javax.swing.event.DocumentEvent)
		 */
		public void insertUpdate(DocumentEvent e) {
			this.dirty = true;
		}
		
		/*
		 * @see javax.swing.event.DocumentListener#removeUpdate(javax.swing.event.DocumentEvent)
		 */
		public void removeUpdate(DocumentEvent e) {
			this.dirty = true;
		}
		
		/*
		 * @see de.goldenGate.util.FontEditable#getEditFontsButton()
		 */
		public JButton getEditFontsButton() {
			return this.getEditFontsButton(null, null, null);
		}
		
		/*
		 * @see de.goldenGate.util.FontEditable#getEditFontsButton(String)
		 */
		public JButton getEditFontsButton(String text) {
			return this.getEditFontsButton(text, null, null);
		}
		
		/*
		 * @see de.goldenGate.util.FontEditable#getEditFontsButton(Dimension)
		 */
		public JButton getEditFontsButton(Dimension dimension) {
			return this.getEditFontsButton(null, dimension, null);
		}
		
		/*
		 * @see de.goldenGate.util.FontEditable#getEditFontsButton(Border)
		 */
		public JButton getEditFontsButton(Border border) {
			return this.getEditFontsButton(null, null, border);
		}
		
		/*
		 * @see de.goldenGate.util.FontEditable#getEditFontsButton(String, Dimension, Border)
		 */
		public JButton getEditFontsButton(String text, Dimension dimension, Border border) {
			return new EditFontsButton(this, text, dimension, border);
		}
		
		/*
		 * @see de.goldenGate.util.FontEditable#editFonts()
		 */
		public boolean editFonts() {
			FontEditorDialog fed = new FontEditorDialog(((JFrame) null), this.fontName, this.fontSize, this.fontColor);
			fed.setVisible(true);
			if (fed.isCommitted()) {
				FontEditorPanel font = fed.getFontEditor();
				if (font.isDirty()) {
					this.fontName = font.getFontName();
					this.fontSize = font.getFontSize();
					this.fontColor = font.getFontColor();
					dirty = true;
				}
				this.refreshDisplay();
				return true;
			}
			return false;
		}
	}
}
