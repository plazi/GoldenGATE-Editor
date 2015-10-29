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
package de.uka.ipd.idaho.goldenGate.plugin.guiTranslator;


import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ContainerEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin;
import de.uka.ipd.idaho.goldenGate.plugins.SettingsPanel;
import de.uka.ipd.idaho.goldenGate.util.DialogPanel;
import de.uka.ipd.idaho.stringUtils.StringVector;
import de.uka.ipd.idaho.stringUtils.csvHandler.StringRelation;
import de.uka.ipd.idaho.stringUtils.csvHandler.StringTupel;
import de.uka.ipd.idaho.stringUtils.regExUtils.RegExUtils;

/**
 * Plugin for localizing the GoldenGATE Editor GUI. This plugin hooks up to the
 * AWT event queue and listens for components being created. If the newly
 * created component bears some sort of text, this plugin tries to translate the
 * text. In particular, this applies to
 * <ul>
 * <li>the text of JLabels</li>
 * <li>the text of sub classes of AbstractButton, e.g. JButton or JMenuItem</li>
 * <li>the tab and tooltip texts in JTabbelPanes</li>
 * <li>the title text of JFrames and JDialogs</li>
 * </ul>
 * Translation works based on rules. Every rule consists of a <b>match</b>
 * String and a <b>translation</b> String. There are two types of rules:
 * <ul>
 * <li>Simple rules are rules whose match and translation Strings are plain
 * Strings. They represent a one to one mapping of original text and translated
 * text.</li>
 * <li>Complex rules are rules whose match and translation Strings involve
 * variables (see below). They represent a mapping of any String matching the
 * match part to the respective translation. Variables are parts that (a) match
 * any String and (b) are literally transferred to the translation. The
 * intention of complex rules is to simplify the translation of texts that have
 * common patterns.<br>
 * Example: assume there are two DocumentIO plugins that provide an item to the
 * file menu for loading documents. Be the text of the first menu item 'Load
 * Document from Server', and be that of the second 'Load Document from URL'.
 * Instead of translating these texts individually, a complex rule facilitates
 * doing the translation in one rule: The match String 'Load Document
 * from&nbsp;@docSource' will match both menu item texts, the
 * variable&nbsp;@docSource matching 'Server' and 'URL', respectively. The
 * translation (into German) could be 'Dokument von&nbsp;@docSource Laden', with
 * the value matched by the variable automatically filled in. This rule results
 * in 'Load Document from <i>URL</i>' being translated to 'Dokument von <i>URL</i>
 * Laden', and 'Load Document from <i>Server</i>' being translated to 'Dokument
 * von <i>Server</i> Laden'</li>
 * </ul>
 * On initialization, the language is set to the default locale. If the latter
 * is English, this plugin does nothing. For any other language, it tries to
 * find a translation for any text encountered. If no translation is found for
 * the current language, this plugin remembers the untranslated String. Missing
 * translations can be filled in in the plugin's settings panel in the
 * GoldenGATE Editor's Preferences dialog.
 * 
 * @author sautter
 */
public class GuiTranslator extends AbstractGoldenGatePlugin {
	
	private static final String TRANSLATION_DATA_PREFIX = "Translations.";
	private static final String CSV_DATA_SUFFIX = ".csv";
	
	private TranslationDataSet translationData = null;
	
	//	all strings we've been asked for a translation for
	private TreeSet allStringsToTranslate = new TreeSet();
	private boolean newStringsToTranslate = false; // any new ones among them?
	
	/**
	 * Container for the translation data for a specific locale
	 * 
	 * @author sautter
	 */
	private class TranslationDataSet {
		
		//	the locale this data set belongs to
		Locale locale;
		
		//	data structures used for translation
		TreeMap translations = new TreeMap();
		ArrayList translationRules = new ArrayList();
		boolean translationDataDirty = false;
		
		//	strings we didn't have a translation for in this data set
		TreeSet untranslated = new TreeSet();
		
		TranslationDataSet(Locale locale) {
			this.locale = locale;
		}
		
		void addTranslationRule(String match, String translation) {
			this.addTranslationRule(new TranslationRule(match, translation));
		}
		
		void addTranslationRule(TranslationRule rule) {
			
			//	simple match (plain literal), use properties
			if (rule.isSimple)
				this.translations.put(rule.match, rule.translation);
			
			//	create rule object from complex rules
			else this.translationRules.add(rule);
			
			//	remember we've been modified
			this.translationDataDirty = true;
		}
	}
	
	/* 
	 * @see de.goldenGate.resourceManagement.AbstractGoldenGatePlugin#init()
	 */
	public void init() {
		
		//	load translation file for system language
		this.translationData = this.getTranslationData(new Locale(Locale.getDefault().getLanguage()));
		
		//	catch components as they are created, and translate the label texts
		try {
			Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener() {
				public void eventDispatched(AWTEvent event) {
			        if (event instanceof ContainerEvent) {
						ContainerEvent cEvent = (ContainerEvent) event;
						if (cEvent.getID() == ContainerEvent.COMPONENT_ADDED) {
							Component pComp = cEvent.getContainer();
							Component cComp = cEvent.getChild();
							if ((pComp != null) && (pComp instanceof JTabbedPane)) {
								JTabbedPane tp = ((JTabbedPane) pComp);
								int ci = tp.indexOfComponent(cComp);
								if (ci != -1) {
									tp.setTitleAt(ci, translate(tp.getTitleAt(ci)));
									tp.setToolTipTextAt(ci, translate(tp.getToolTipTextAt(ci)));
								}
							}
							else translateComponent(cComp);
						}
					}
			        else if (event instanceof WindowEvent) {
			        	WindowEvent wEvent = (WindowEvent) event;
						if (wEvent.getID() == WindowEvent.WINDOW_OPENED)
							translateComponent(wEvent.getWindow());
			        }
				}
			},(
				AWTEvent.CONTAINER_EVENT_MASK
				|
				AWTEvent.WINDOW_EVENT_MASK
			));
		}
		
		//	this might happen in an applet
		catch (SecurityException se) {}
		
		//	load all strings needing translation
		try {
			InputStream is = this.dataProvider.getInputStream(TRANSLATION_DATA_PREFIX + "all.txt");
			BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
			
			String line;
			while ((line = br.readLine()) != null)
				this.allStringsToTranslate.add(line);
			
			br.close();
		}
		catch (FileNotFoundException fnfe) {
			System.out.println("Could not find list of strings to translate.");
			
			//	create empty translation file in order to remember language
			if (this.dataProvider.isDataEditable()) {
				
				//	store dummy translation file
				try {
					OutputStream os = this.dataProvider.getOutputStream(TRANSLATION_DATA_PREFIX + "all.txt");
					os.flush();
					os.close();
				}
				catch (IOException ioe) {
					System.out.println(ioe.getClass().getName() + " while storing strings to translate: " + ioe.getMessage());
					ioe.printStackTrace(System.out);
				}
			}
		}
		catch (IOException ioe) {
			System.out.println(ioe.getClass().getName() + " while loading strings to translate: " + ioe.getMessage());
			ioe.printStackTrace(System.out);
		}

	}
	
	/* 
	 * @see de.goldenGate.resourceManagement.AbstractGoldenGatePlugin#getSettingsPanel()
	 */
	public SettingsPanel getSettingsPanel() {
		return new GtSettingsPanel();
	}
	
	private class GtSettingsPanel extends SettingsPanel {
		private JComboBox languages;
		
		private TreeMap languagePanels = new TreeMap();
		private JTabbedPane languageTabs = new JTabbedPane();
		
		private final String[] stringsToTranslate;
		
		GtSettingsPanel() {
			super("Gui Translation", "Edit translation of texts in GUI");
			
			//	freeze strings to translate (shiled from dynamic extension of string set due to components created from this panel)
			this.stringsToTranslate = ((String[]) allStringsToTranslate.toArray(new String[allStringsToTranslate.size()]));
			
			//	add tab for each known language
			String[] languageData = dataProvider.getDataNames();
			for (int l = 0; l < languageData.length; l++) {
				if (languageData[l].startsWith(TRANSLATION_DATA_PREFIX) && languageData[l].endsWith(CSV_DATA_SUFFIX)) {
					String language = languageData[l].substring(TRANSLATION_DATA_PREFIX.length());
					language = language.substring(0, (language.length() - CSV_DATA_SUFFIX.length()));
					TranslationDataSet tds = getTranslationData(new Locale(language));
					if (tds != null) {
						LanguagePanel lp = new LanguagePanel(tds);
						this.languagePanels.put(language, lp);
						this.languageTabs.addTab(this.getTabTitle(language), lp);
					}
				}
			}
			
			//	add drop-down to select current language
			Locale[] locales = Locale.getAvailableLocales();
			ArrayList wrappedLocales = new ArrayList();
			for (int l = 0; l < locales.length; l++)
				if (locales[l].getCountry().length() == 0)
					wrappedLocales.add(new LocaleWrapper(locales[l]));
			Collections.sort(wrappedLocales);
			this.languages = new JComboBox(wrappedLocales.toArray());
			this.languages.setEditable(false);
			this.languages.setSelectedItem(new LocaleWrapper(new Locale((translationData == null) ? "en" : translationData.locale.getLanguage())));
			this.languages.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent ie) {
					Object newLocaleObject = languages.getSelectedItem();
					if (newLocaleObject != null)
						setSelectedLocale(((LocaleWrapper) newLocaleObject).locale);
				}
			});
			
			//	put the whole stuff together
			this.setLayout(new BorderLayout());
			this.add(this.languages, BorderLayout.NORTH);
			this.add(this.languageTabs, BorderLayout.CENTER);
		}
		
		//	wrapper for overriding toString() method (which is final in Locale) in favor of readable display values
		private class LocaleWrapper implements Comparable {
			private final Locale locale;
			private LocaleWrapper(Locale locale) {
				this.locale = locale;
			}
			public String toString() {
				return (this.locale.getDisplayName(this.locale) + " (" + this.locale.getDisplayName(new Locale("en")) + ")");
			}
			public boolean equals(Object obj) {
				return ((obj != null) && (obj instanceof LocaleWrapper) && ((LocaleWrapper) obj).locale.equals(this.locale));
			}
//			public int compareTo(LocaleWrapper lw) {
//				return this.toString().compareToIgnoreCase(lw.toString());
//			}
			public int compareTo(Object obj) {
				LocaleWrapper lw = ((LocaleWrapper) obj);
				return this.toString().compareToIgnoreCase(lw.toString());
			}
		}
		
		private void setSelectedLocale(Locale locale) {
			LanguagePanel lp = ((LanguagePanel) this.languagePanels.get(locale.getLanguage()));
			if (lp == null) {
				TranslationDataSet tds = getTranslationData(locale);
				if (tds != null) {
					lp = new LanguagePanel(tds);
					this.languagePanels.put(locale.getLanguage(), lp);
					this.languageTabs.addTab(this.getTabTitle(locale.getLanguage()), lp);
				}
			}
			if (lp != null) this.languageTabs.setSelectedComponent(lp);
		}
		
		private String getTabTitle(String language) {
			return (language + " (" + (new Locale(language)).getDisplayLanguage(new Locale((translationData == null) ? "en" : translationData.locale.getLanguage())) + ")");
		}
		
		public void commitChanges() {
			boolean languageDirty = false;
			
			//	store modified languages
			for (Iterator lit = this.languagePanels.values().iterator(); lit.hasNext();) {
				LanguagePanel lp = ((LanguagePanel) lit.next());
				System.out.println("Checking language panel for " + lp.data.locale.getLanguage());
				if (lp.data.translationDataDirty) {
					System.out.println("  data dirty");
					storeTranslationData(lp.data);
					if (lp.data == translationData) {
						System.out.println("  dirty GUI language: " + lp.data.locale.getLanguage());
						languageDirty = true;
					}
				}
			}
			
			//	adjust GUI
			Object newLocaleObject = this.languages.getSelectedItem();
			if (newLocaleObject != null) {
				Locale newLocale = ((LocaleWrapper) newLocaleObject).locale;
				System.out.println("New locale is " + newLocaleObject.toString());
				System.out.println("Translation data is " + ((translationData == null) ? "null" : (new LocaleWrapper(translationData.locale)).toString()));
				if ("en".equals(newLocale.getLanguage()) ? (translationData != null) : ((translationData == null) || !newLocale.getLanguage().equals(translationData.locale.getLanguage()))) {
					translationData = getTranslationData(newLocale);
					translateGui();
				}
				else if (languageDirty) translateGui();
			}
		}
		
		/**
		 * Editor panel for the translation data set of a spcific language
		 * 
		 * @author sautter
		 */
		private class LanguagePanel extends JPanel {
			private TranslationDataSet data;
			
			private TreeMap translations = new TreeMap();
			private TreeMap translationRules = new TreeMap();
			
			private JTable translationTable = new JTable();
			
			LanguagePanel(TranslationDataSet tds) {
				super(new BorderLayout(), true);
				this.data = tds;
				
				//	create table
				this.translationTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
				this.translationTable.addMouseListener(new MouseAdapter() {
					public void mouseClicked(MouseEvent me) {
						if (me.getClickCount() > 1) {
							int row = translationTable.getSelectedRow();
							if (row != -1) {
								String string = stringsToTranslate[row];
								String translation = ((String) translations.get(string));
								TranslationRule tr = ((TranslationRule) translationRules.get(string));
								editRule(string, translation, tr);
							}
						}
					}
				});
				
				//	add data
				this.refreshTable();
				
				//	make table scrollable
				JScrollPane translationTableBox = new JScrollPane(this.translationTable);
				translationTableBox.getVerticalScrollBar().setUnitIncrement(50);
				
				JButton storeButton = new JButton("Store Language");
				storeButton.setBorder(BorderFactory.createRaisedBevelBorder());
				storeButton.setPreferredSize(new Dimension(150, 21));
				storeButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						storeTranslationData(data);
					}
				});
				
				JButton discardButton = new JButton("Discard Language");
				discardButton.setBorder(BorderFactory.createRaisedBevelBorder());
				discardButton.setPreferredSize(new Dimension(150, 21));
				discardButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						discardLanguage();
					}
				});
				
				JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER), true);
				buttonPanel.add(storeButton);
				buttonPanel.add(discardButton);
				
				this.add(translationTableBox, BorderLayout.CENTER);
				this.add(buttonPanel, BorderLayout.SOUTH);
			}
			
			void discardLanguage() {
				languagePanels.remove(this.data.locale.getLanguage());
				languageTabs.remove(this);
				dataProvider.deleteData(TRANSLATION_DATA_PREFIX + this.data.locale.getLanguage() + CSV_DATA_SUFFIX);
			}
			
			void refreshTable() {
				
				//	clean up
				this.translations.clear();
				this.translationRules.clear();
				
				//	fill data structures
				for (int s = 0; s < stringsToTranslate.length; s++) {
					String string = stringsToTranslate[s];
					String translation = ((String) this.data.translations.get(string));
					
					if (translation != null)
						this.translations.put(string, translation);
					
					for (int ri = 0; ri < this.data.translationRules.size(); ri++) {
						TranslationRule tr = ((TranslationRule) this.data.translationRules.get(ri));
						translation = tr.translate(string);
						if (translation != null) {
							this.translations.put(string, translation);
							this.translationRules.put(string, tr);
						}
					}
				}
				
				//	make changes visible
				this.translationTable.setModel(new TranslationTableModel());
				this.validate();
			}
			
			private class TranslationTableModel implements TableModel  {
				public Class getColumnClass(int columnIndex) {
					return String.class;
				}
				
				public int getColumnCount() {
					return 3;
				}
				public String getColumnName(int columnIndex) {
					if (columnIndex == 0) return "String";
					else if (columnIndex == 1) return "Translation";
					else if (columnIndex == 2) return "Rule Applied";
					else return null;
				}
				
				public int getRowCount() {
					return stringsToTranslate.length;
				}
				
				public Object getValueAt(int rowIndex, int columnIndex) {
					String string = stringsToTranslate[rowIndex];
					if (columnIndex == 0) return string;
					else if (columnIndex == 1) {
						String translation = ((String) translations.get(string));
						return ((translation == null) ? "" : translation);
					}
					else if (columnIndex == 2) {
						String translation = ((String) translations.get(string));
						if (translation == null) return "";
						else {
							TranslationRule tr = ((TranslationRule) translationRules.get(string));
							return (
									(tr == null)
									? 
									("Mapping: " + string + " ==> " + translation)
									: 
									("Rule: " + tr.match + " ==> " + tr.translation)
									);
						}
					}
					else return null;
				}

				public boolean isCellEditable(int rowIndex, int columnIndex) {
					return false;
				}
				public void setValueAt(Object aValue, int rowIndex, int columnIndex) {}

				public void removeTableModelListener(TableModelListener l) {}
				public void addTableModelListener(TableModelListener l) {}
			}
			
			void editRule(String string, String translation, TranslationRule tr) {
				TranslationRuleEditor tre = new TranslationRuleEditor(string, translation, tr);
				tre.setVisible(true);
				
				if (tre.isCommitted()) {
					this.data.translations.remove(string);
					this.data.translationRules.remove(tr);
					this.data.addTranslationRule(tre.translationRule);
					this.refreshTable();
				}
				else if (tre.isDeleted()) {
					this.data.translations.remove(string);
					this.data.translationRules.remove(tr);
					this.data.translationDataDirty = true;
					this.refreshTable();
				}
			}
			
			class TranslationRuleEditor extends DialogPanel {
				private JTextField matchField = new JTextField();
				private JTextField translationField = new JTextField();
				
				private TranslationRule translationRule;
				private boolean delete = false;
				
				TranslationRuleEditor(String string, String translation, TranslationRule tr) {
					super("Edit Translation Rule", true);
					
					this.matchField.setBorder(BorderFactory.createLoweredBevelBorder());
					this.translationField.setBorder(BorderFactory.createLoweredBevelBorder());
					
					//	initialize fields
					if (tr == null) {
						this.matchField.setText(string);
						this.translationField.setText((translation == null) ? "" : translation);
					}
					else {
						this.matchField.setText(tr.match);
						this.translationField.setText(tr.translation);
					}
					
					JPanel fieldPanel = new JPanel(new GridBagLayout());
					final GridBagConstraints gbc = new GridBagConstraints();
					gbc.insets.top = 2;
					gbc.insets.bottom = 2;
					gbc.insets.left = 5;
					gbc.insets.right = 5;
					gbc.fill = GridBagConstraints.HORIZONTAL;
					gbc.weightx = 1;
					gbc.weighty = 0;
					gbc.gridwidth = 1;
					gbc.gridheight = 1;
					gbc.gridx = 0;
					gbc.gridy = 0;
					
					gbc.gridwidth = 6;
					gbc.weightx = 4;
					fieldPanel.add(new JLabel("Please edit the translation rule for '" + string + "'"), gbc.clone());
					
					gbc.gridy++;
					gbc.gridx = 0;
					gbc.weightx = 0;
					gbc.gridwidth = 1;
					fieldPanel.add(new JLabel("Match", JLabel.LEFT), gbc.clone());
					gbc.gridx = 1;
					gbc.weightx = 2;
					gbc.gridwidth = 2;
					fieldPanel.add(this.matchField, gbc.clone());
					gbc.gridx = 3;
					gbc.weightx = 0;
					gbc.gridwidth = 1;
					fieldPanel.add(new JLabel("Translation", JLabel.LEFT), gbc.clone());
					gbc.gridx = 4;
					gbc.weightx = 2;
					gbc.gridwidth = 2;
					fieldPanel.add(this.translationField, gbc.clone());
					
					JButton okButton = new JButton("OK");
					okButton.setBorder(BorderFactory.createRaisedBevelBorder());
					okButton.setPreferredSize(new Dimension(80, 21));
					okButton.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							commit();
						}
					});
					
					JButton cancelButton = new JButton("Cancel");
					cancelButton.setBorder(BorderFactory.createRaisedBevelBorder());
					cancelButton.setPreferredSize(new Dimension(80, 21));
					cancelButton.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							dispose();
						}
					});
					
					JButton deleteButton = new JButton("Delete");
					deleteButton.setBorder(BorderFactory.createRaisedBevelBorder());
					deleteButton.setPreferredSize(new Dimension(80, 21));
					deleteButton.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							delete = true;
							dispose();
						}
					});
					
					JButton testButton = new JButton("Test");
					testButton.setBorder(BorderFactory.createRaisedBevelBorder());
					testButton.setPreferredSize(new Dimension(80, 21));
					testButton.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							testRule();
						}
					});
					
					JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
					buttonPanel.add(okButton);
					buttonPanel.add(cancelButton);
					buttonPanel.add(deleteButton);
					buttonPanel.add(testButton);
					
					this.add(fieldPanel, BorderLayout.CENTER);
					this.add(buttonPanel, BorderLayout.SOUTH);
					
					this.setSize(500, 100);
					this.setLocationRelativeTo(LanguagePanel.this);
				}
				
				TranslationRule getTranslationRule() {
					String match = this.matchField.getText().trim();
					if (match.length() == 0)
						JOptionPane.showMessageDialog(this, "Please specify a match string.", "Rule Edit Error", JOptionPane.ERROR_MESSAGE);
					
					else try {
							return new TranslationRule(match, this.translationField.getText());
						}
						catch (RuntimeException re) { // catch and report pattern syntax exception
							JOptionPane.showMessageDialog(this, ("The specified match string is invalid, or variables in match string and translation don't match:\n" + re.getMessage()), "Rule Edit Error", JOptionPane.ERROR_MESSAGE);
						}
					
					return null;
				}
				
				void commit() {
					TranslationRule tr = this.getTranslationRule();
					if (tr == null) return;
					else {
						this.translationRule = tr;
						dispose();
					}
				}
				
				void testRule() {
					TranslationRule tr = this.getTranslationRule();
					if (tr == null) return;
					else {
						TreeMap result = new TreeMap();
						for (int s = 0; s < stringsToTranslate.length; s++) {
							String string = stringsToTranslate[s];
							String translation = tr.translate(string);
							if (translation != null)
								result.put(string, translation);
						}
						TestResultDialog trd = new TestResultDialog(result);
						trd.setVisible(true);
					}
				}
				
				class TestResultDialog extends DialogPanel {
					TestResultDialog(final TreeMap result) {
						super("Rule Test Result", true);
						
						final String[] strings = ((String[]) result.keySet().toArray(new String[result.size()]));
						JTable resultTable = new JTable(new TableModel() {
							public Class getColumnClass(int columnIndex) {
								return String.class;
							}
							
							public int getColumnCount() {
								return 2;
							}
							public String getColumnName(int columnIndex) {
								if (columnIndex == 0) return "String";
								else if (columnIndex == 1) return "Translation";
								else return null;
							}
							
							public int getRowCount() {
								return result.size();
							}
							
							public Object getValueAt(int rowIndex, int columnIndex) {
								String string = strings[rowIndex];
								if (columnIndex == 0) return string;
								else if (columnIndex == 1) return result.get(string);
								else return null;
							}

							public boolean isCellEditable(int rowIndex, int columnIndex) {
								return false;
							}
							public void setValueAt(Object aValue, int rowIndex, int columnIndex) {}

							public void removeTableModelListener(TableModelListener l) {}
							public void addTableModelListener(TableModelListener l) {}
						});
						
						JButton okButton = new JButton("OK");
						okButton.setBorder(BorderFactory.createRaisedBevelBorder());
						okButton.setPreferredSize(new Dimension(80, 21));
						okButton.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								TestResultDialog.this.dispose();
							}
						});
						
						this.add(new JScrollPane(resultTable), BorderLayout.CENTER);
						this.add(okButton, BorderLayout.SOUTH);
						
						this.setSize(300, 500);
						this.setLocationRelativeTo(TranslationRuleEditor.this);
					}
				}
				
				boolean isCommitted() {
					return (this.translationRule != null);
				}
				
				boolean isDeleted() {
					return this.delete;
				}
			}
		}
	}
	
	private TranslationDataSet getTranslationData(Locale locale) {
		
		//	for English, we don't need translation data
		if ("en".equals(locale.getLanguage())) return null;
		
		//	we already got this one
		if ((this.translationData != null) && this.translationData.locale.equals(locale))
			return this.translationData;
		
		//	load translation file
		TranslationDataSet tds = new TranslationDataSet(locale);
		try {
			InputStream is = this.dataProvider.getInputStream(TRANSLATION_DATA_PREFIX + locale.getLanguage() + CSV_DATA_SUFFIX);
			StringRelation load = StringRelation.readCsvData(new InputStreamReader(is, "UTF-8"), '"', true, null);
			is.close();
			for (int t = 0; t < load.size(); t++) {
				String match = load.get(t).getValue("match");
				String translation = load.get(t).getValue("translation");
				if ((match != null) && (translation != null)) try {
					tds.addTranslationRule(match, translation);
				}
				catch (RuntimeException re) { // catch and report pattern syntax exception
					System.out.println(re.getClass().getName() + " while loading translation table for '" + locale.getLanguage() + "': " + re.getMessage());
					re.printStackTrace(System.out);
				}
			}
			tds.translationDataDirty = false;
		}
		catch (FileNotFoundException fnfe) {
			System.out.println("Could not find translation data for '" + locale.getLanguage() + "'");
			
			//	create empty translation file in order to remember language
			if (this.dataProvider.isDataEditable()) {
				StringRelation store = new StringRelation();
				StringVector keys = new StringVector();
				keys.addElement("match");
				keys.addElement("translation");
				
				//	store dummy translation file
				try {
					OutputStream os = this.dataProvider.getOutputStream(TRANSLATION_DATA_PREFIX + tds.locale.getLanguage() + CSV_DATA_SUFFIX);
					StringRelation.writeCsvData(new OutputStreamWriter(os, "UTF-8"), store, keys);
					os.flush();
					os.close();
				}
				catch (IOException ioe) {
					System.out.println(ioe.getClass().getName() + " while storing translation table for '" + tds.locale.getLanguage() + "': " + ioe.getMessage());
					ioe.printStackTrace(System.out);
				}
			}
		}
		catch (IOException ioe) {
			System.out.println(ioe.getClass().getName() + " while loading translation table for '" + locale.getLanguage() + "': " + ioe.getMessage());
			ioe.printStackTrace(System.out);
		}
		return tds;
	}
	
	private void storeTranslationData(TranslationDataSet tsd) {
		
		//	test if storing necessary and possible & check language
		if ((tsd != null) && tsd.translationDataDirty && this.dataProvider.isDataEditable()) {
			
			//	collect data
			StringRelation store = new StringRelation();
			ArrayList simpleMatches = new ArrayList(tsd.translations.keySet());
			Collections.sort(simpleMatches, String.CASE_INSENSITIVE_ORDER);
			for (int m = 0; m < simpleMatches.size(); m++) {
				String match = simpleMatches.get(m).toString();
				StringTupel st = new StringTupel();
				st.setValue("match", match);
				st.setValue("translation", ((String) tsd.translations.get(match)));
				store.addElement(st);
			}
			for (int r = 0; r < tsd.translationRules.size(); r++) {
				TranslationRule tr = ((TranslationRule) tsd.translationRules.get(r));
				StringTupel st = new StringTupel();
				st.setValue("match", tr.match);
				st.setValue("translation", tr.translation);
				store.addElement(st);
			}
			StringVector keys = new StringVector();
			keys.addElement("match");
			keys.addElement("translation");
			
			//	store translation file
			try {
				OutputStream os = this.dataProvider.getOutputStream(TRANSLATION_DATA_PREFIX + tsd.locale.getLanguage() + CSV_DATA_SUFFIX);
				StringRelation.writeCsvData(new OutputStreamWriter(os, "UTF-8"), store, keys);
				os.flush();
				os.close();
			}
			catch (IOException ioe) {
				System.out.println(ioe.getClass().getName() + " while storing translation table for '" + tsd.locale.getLanguage() + "': " + ioe.getMessage());
				ioe.printStackTrace(System.out);
			}
			
			//	remember persistent data is in sync
			tsd.translationDataDirty = false;
		}
	}
	
	/* 
	 * @see de.goldenGate.resourceManagement.AbstractGoldenGatePlugin#exit()
	 */
	public void exit() {
		
		//	store current data set if dirty
		this.storeTranslationData(this.translationData);
		
		//	store full data set if dirty
		if (this.newStringsToTranslate && this.dataProvider.isDataEditable()) {
			try {
				OutputStream os = this.dataProvider.getOutputStream(TRANSLATION_DATA_PREFIX + "all.txt");
				BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
				
				for (Iterator it = this.allStringsToTranslate.iterator(); it.hasNext();) {
					bw.write((String)it.next());
					bw.newLine();
				}
				
				bw.flush();
				bw.close();
			}
			catch (IOException ioe) {
				System.out.println(ioe.getClass().getName() + " while storing strings to translate: " + ioe.getMessage());
				ioe.printStackTrace(System.out);
			}
		}
	}
	
	private static class TranslationRule {
		private final String match;
		private final Pattern matcher;
		private final boolean isSimple;
		
		private final String translation;
		private final String[] translationParts;
		private final int[] translationPartGroupNumbers;
		
		TranslationRule(String match, String translation) {
			this.match = match;
			
			String[] matchParts = parse(this.match);
			StringBuffer matchPattern = new StringBuffer();
			HashMap variablePositions = new HashMap();
			for (int p = 0; p < matchParts.length; p++)
				//	variable
				if (matchParts[p].startsWith("@")) {
					variablePositions.put(matchParts[p], new Integer(variablePositions.size() + 1));
					matchPattern.append("(.+?)"); // use reluctant capturing group so match goes only up to next non-variable part
				}
				//	literal
				else matchPattern.append(RegExUtils.escapeForRegEx(unescape(matchParts[p])));
			
			this.matcher = Pattern.compile(matchPattern.toString());
			this.isSimple = variablePositions.isEmpty();
			
			this.translation = translation;
			this.translationParts = parse(this.translation);
			
			this.translationPartGroupNumbers = new int[this.translationParts.length];
			for (int p = 0; p < this.translationParts.length; p++)
				//	variable
				if (this.translationParts[p].startsWith("@")) {
					Integer positionInMatch = ((Integer) variablePositions.get(this.translationParts[p]));
					
					//	variable not defined in match
					if (positionInMatch == null)
						throw new IllegalArgumentException("Variable '" + this.translationParts[p] + "' is referenced in translation, but not defined in match string.");
					
					//	store position
					else this.translationPartGroupNumbers[p] = positionInMatch.intValue();
				}
				//	literal
				else {
					this.translationParts[p] = unescape(this.translationParts[p]);
					this.translationPartGroupNumbers[p] = -1;
				}
		}
		
		String translate(String text) {
			Matcher matcher = this.matcher.matcher(text);
			
			//	rule applicable to text
			if (matcher.matches()) {
				StringBuffer translation = new StringBuffer();
				for (int p = 0; p < this.translationParts.length; p++) {
					
					//	literal
					if (this.translationPartGroupNumbers[p] == -1)
						translation.append(this.translationParts[p]);
					
					//	variable in range
					else if (this.translationPartGroupNumbers[p] <= matcher.groupCount())
						translation.append(matcher.group(this.translationPartGroupNumbers[p]));
					
					//	variable out of range, ignore it
				}
				return translation.toString();
			}
			
			//	rule not applicable
			else return null;
		}
		
		static String unescape(String string) {
			StringBuffer unescaped = new StringBuffer(string);
			int escape = -1;
			for (int c = 0; c < unescaped.length(); c++) {
				if ((c != escape) && (unescaped.charAt(c) == '\\'))
					unescaped.deleteCharAt(escape = c--); // store position, and keep it for next run of loop
			}
			return unescaped.toString();
		}
		
		private static final Pattern parsePattern = Pattern.compile("\\@[a-zA-Z0-9\\_]++");
		static String[] parse(String string) {
			StringVector parts = new StringVector();
			int startOfRemainder = 0;
			Matcher parser = parsePattern.matcher(string);
			while (parser.find()) {
				if ((parser.start() == 0) || (string.charAt(parser.start() - 1) != '\\')) {
					if (startOfRemainder < parser.start())
						parts.addElement(string.substring(startOfRemainder, parser.start()));
					parts.addElement(parser.group());
					startOfRemainder = parser.end();
				}
			}
			if (startOfRemainder < string.length())
				parts.addElement(string.substring(startOfRemainder));
			return parts.toStringArray();
		}
	}
	
	private String translate(String text) {
		if (text == null) return ""; //null; do not return null, for this may cause exceptions with window titles
		else if (text.length() < 3) return text;
		
		this.newStringsToTranslate = (this.allStringsToTranslate.add(text) || this.newStringsToTranslate);
		
		if (this.translationData == null) return text;
		
		String translation = ((String) this.translationData.translations.get(text));
		if (translation != null) return translation;
		
		for (int ri = 0; (translation == null) && (ri < this.translationData.translationRules.size()); ri++)
			translation = ((TranslationRule) this.translationData.translationRules.get(ri)).translate(text);
		
		if (translation == null) {
			this.translationData.untranslated.add(text);
			return text;
		}
		else return translation;
	}
	
	private static final boolean DEBUG = false;
	
	private void translateGui() {
		if (DEBUG) System.out.println("Translating GUI");
		Frame[] frames = Frame.getFrames();
		
		for (int f = 0; f < frames.length; f++) {
			this.translateContainer(frames[f]);
			if (frames[f] instanceof JFrame)
				this.translateContainer(((JFrame) frames[f]).getContentPane());
			this.translateComponent(frames[f]);
			
			Window[] windows = frames[f].getOwnedWindows();
			for (int w = 0; w < windows.length; w++) {
				this.translateContainer(windows[w]);
				if (windows[w] instanceof JFrame)
					this.translateContainer(((JFrame) windows[w]).getContentPane());
				else if (windows[w] instanceof JDialog)
					this.translateContainer(((JDialog) windows[w]).getContentPane());
				this.translateComponent(windows[w]);
			}
		}
	}
	
	private void translateContainer(Container cont) {
		if (DEBUG) System.out.println("Translating Container " + cont.getClass().getName());
		Component[] comps = cont.getComponents();
		for (int c = 0; c < comps.length; c++) {
			this.translateComponent(comps[c]);
			if (comps[c] instanceof Container)
				this.translateContainer((Container) comps[c]);
			if (comps[c] instanceof JMenu)
				this.translateContainer(((JMenu) comps[c]).getPopupMenu());
		}
	}
	
	private void translateComponent(Component comp) {
		if (comp.getClass().getName().indexOf("$UI") != -1) return;
		if (comp.getClass().getName().indexOf(".plaf.") != -1) return;
		
		if (DEBUG) System.out.println("Translating Component " + comp.getClass().getName());
		
		if (comp instanceof JLabel) {
			JLabel jl = ((JLabel) comp);
			if (DEBUG) System.out.println(" ==> " + jl.getText());
			jl.setText(translate(jl.getText()));
		}
		else if (comp instanceof AbstractButton) {
			AbstractButton ab = ((AbstractButton) comp);
			if (DEBUG) System.out.println(" ==> " + ab.getText());
			ab.setText(translate(ab.getText()));
		}
		else if (comp instanceof JTabbedPane) {
			JTabbedPane tp = ((JTabbedPane) comp);
			for (int t = 0; t < tp.getTabCount(); t++) {
				if (DEBUG) System.out.println(" ==> " + tp.getTitleAt(t));
				tp.setTitleAt(t, translate(tp.getTitleAt(t)));
				if (DEBUG) System.out.println(" ==> " + tp.getToolTipTextAt(t));
				tp.setToolTipTextAt(t, translate(tp.getToolTipTextAt(t)));
			}
		}
		else if (comp instanceof Frame) {
			Frame f = ((Frame) comp);
			if (DEBUG) System.out.println(" ==> " + f.getTitle());
			f.setTitle(translate(f.getTitle()));
		}
		else if (comp instanceof Dialog) {
			Dialog d = ((Dialog) comp);
			if (DEBUG) System.out.println(" ==> " + d.getTitle());
			d.setTitle(translate(d.getTitle()));
		}
	}
	
	//	!!! for test purposes only !!!
	public static void main(String[] args) throws Exception {
		TranslationRule tr = new TranslationRule("Test with @name by @doc", "@doc's Versuch mit @name");
		System.out.println(tr.isSimple ? "simple" : "complex");
		System.out.println(tr.matcher.pattern());
		System.out.println(tr.matcher.matcher("").groupCount());
		System.out.println(tr.translate("Test with Bob & Terry by Donat"));
	}
}