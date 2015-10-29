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
package de.uka.ipd.idaho.goldenGate.plugin.documentViewer;


import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;
import javax.swing.text.JTextComponent;

import de.uka.ipd.idaho.easyIO.settings.Settings;
import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.Token;
import de.uka.ipd.idaho.gamta.TokenSequence;
import de.uka.ipd.idaho.gamta.TokenSequenceUtils;
import de.uka.ipd.idaho.gamta.Tokenizer;
import de.uka.ipd.idaho.gamta.defaultImplementation.PlainTokenSequence;
import de.uka.ipd.idaho.gamta.util.gPath.GPath;
import de.uka.ipd.idaho.gamta.util.gPath.GPathExpression;
import de.uka.ipd.idaho.gamta.util.gPath.GPathParser;
import de.uka.ipd.idaho.gamta.util.gPath.exceptions.GPathException;
import de.uka.ipd.idaho.goldenGate.DocumentEditor;
import de.uka.ipd.idaho.goldenGate.DocumentEditorDialog;
import de.uka.ipd.idaho.goldenGate.GoldenGATE;
import de.uka.ipd.idaho.goldenGate.plugins.AbstractDocumentViewer;
import de.uka.ipd.idaho.goldenGate.plugins.AnnotationFilter;
import de.uka.ipd.idaho.goldenGate.plugins.AnnotationFilterManager;
import de.uka.ipd.idaho.goldenGate.plugins.DocumentViewParameterPanel;
import de.uka.ipd.idaho.goldenGate.util.AnnotationTools;
import de.uka.ipd.idaho.goldenGate.util.AttributeEditorDialog;
import de.uka.ipd.idaho.goldenGate.util.DialogPanel;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * Annotation viewer listing annotations in a table. The displayed annotations
 * can be filtered through custom filters that may be changed in an ad-hoc
 * fashion. The listing allows for merging and editing annotations, among
 * others. This view is both helpful for sorting out annotations, and for
 * finding ones with specific error conditions, e.g. a lacking attribute.<br>
 * Invoking the SelectAnnotationsViewer with one of the showDocument() methods
 * that takes a Properties object as an argument allows for programmatically
 * specifying the AnnotationFilter used for determining which Annotations to
 * list for selection. In particular, there are two parameters, any one of which
 * can be used:
 * <ul>
 * <li><b>annotationFilterPath</b>: a GPath expression; all annotations
 * selected by the GPath expression will be shown/highlighted</li>
 * <li><b>annotationFilterName</b>: the fully qualified name of an
 * AnnotationFilter, i.e. &lt;filterName&gt;@&lt;filterProviderClassName&gt;,
 * with &lt;filterName&gt; being the provider-internal name of the
 * AnnotationFilter, and &lt;filterProviderClassName&gt; being the class name of
 * respective the AnnotationFilterProvider; all annotations matching the filter
 * will be shown/highlighted</li>
 * </ul>
 * If none of these parameters is specified, the user can select or enter
 * filters manually in an extra panel at the top of the dialog. This happens,
 * for instance, if the NavigateAnnotationsViewer is invoked through
 * GoldenGATE's View menu.
 * 
 * @author sautter
 */
public class NavigateAnnotationsViewer extends AbstractDocumentViewer {
	
	/** the parameter to use for specifying a specific GPath expression whose matches to show/highlight */
	public static final String ANNOTATION_FILTER_PATH_PARAMETER = "annotationFilterPath";
	
	/** the parameter to use for specifying the name and provider name of an AnnotationFilter whose matches to show/highlight */
	public static final String ANNOTATION_FILTER_NAME_PARAMETER = "annotationFilterName";
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentViewer#getViewMenuName()
	 */
	public String getViewMenuName() {
		return "Navigate Annotations";
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractDocumentViewer#showDocument(de.uka.ipd.idaho.gamta.MutableAnnotation, de.uka.ipd.idaho.goldenGate.DocumentEditor, java.util.Properties)
	 */
	protected void showDocument(MutableAnnotation doc, DocumentEditor editor, Properties parameters) {
		AnnotationNavigatorDialog and = new AnnotationNavigatorDialog("Navigate Annotations", doc, editor, this.getFilter(parameters));
		and.setVisible(true);
	}
	
	private AnnotationFilter getFilter(Properties parameters) {
		
		//	check GPath filter parameter
		final String filterPath = parameters.getProperty(ANNOTATION_FILTER_PATH_PARAMETER);
		if (filterPath != null) try {
			return new AnnotationFilter() {
				private GPath path = new GPath(filterPath);
				public boolean accept(Annotation annotation) {
					return false; // never used anyway ...
				}
				public QueriableAnnotation[] getMatches(QueriableAnnotation data) {
					return this.path.evaluate(data, null);
				}
				public MutableAnnotation[] getMutableMatches(MutableAnnotation data) {
					QueriableAnnotation[] matches = this.getMatches(data);
					Set matchIDs = new HashSet();
					for (int m = 0; m < matches.length; m++)
						matchIDs.add(matches[m].getAnnotationID());
					MutableAnnotation[] mutableAnnotations = data.getMutableAnnotations();
					ArrayList mutableMatches = new ArrayList();
					for (int m = 0; m < mutableAnnotations.length; m++)
						if (matchIDs.contains(mutableAnnotations[m].getAnnotationID()))
							mutableMatches.add(mutableAnnotations[m]);
					return ((MutableAnnotation[]) mutableMatches.toArray(new MutableAnnotation[mutableMatches.size()]));
				}
				public String getName() {
					return filterPath;
				}
				public String getProviderClassName() {
					return null;
				}
				public String getTypeLabel() {
					return "GPathAnnotationFilter";
				}
			};
		}
		catch (GPathException gpe) {
			gpe.printStackTrace(System.out);
		}
		
		//	check filter name parameter
		final String filterName = parameters.getProperty(ANNOTATION_FILTER_NAME_PARAMETER);
		if (filterName != null)
			return this.parent.getAnnotationFilterForName(filterName);
		
		//	filter not found
		return null;
	}
	
	private StringVector customFilterHistory = new StringVector();
	private HashMap customFiltersByName = new HashMap();
	private int customFilterHistorySize = 10;
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#init()
	 */
	public void init() {
		//	load custom filter history
		try {
			InputStream is = this.dataProvider.getInputStream("customFilterHistory.cnfg");
			this.customFilterHistory.addContent(StringVector.loadList(is));
			is.close();
		}
		catch (IOException ioe) {
			ioe.printStackTrace(System.out);
		}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#exit()
	 */
	public void exit() {
		//	store custom filter history
		try {
			OutputStream os = this.dataProvider.getOutputStream("customFilterHistory.cnfg");
			this.customFilterHistory.storeContent(os);
			os.flush();
			os.close();
		}
		catch (IOException ioe) {
			ioe.printStackTrace(System.out);
		}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractDocumentViewer#getDocumentViewParameterPanel(de.uka.ipd.idaho.easyIO.settings.Settings)
	 */
	public DocumentViewParameterPanel getDocumentViewParameterPanel(Settings settings) {
		return new AnnotationNavigatorParameterPanel(settings);
	}
	
	private class AnnotationNavigatorParameterPanel extends DocumentViewParameterPanel {
		
		private JRadioButton usePathFilter = new JRadioButton("Use GPath Filter");
		private JTextField filterPathField = new JTextField();
		private JRadioButton useFilter = new JRadioButton("Use Annotation Filter");
		private JComboBox filterSelector;
		
		private boolean dirty = false;
		
		AnnotationNavigatorParameterPanel(Settings settings) {
			super(new GridLayout(2,2));
			
			//	interconnect buttons
			ButtonGroup bg = new ButtonGroup();
			bg.add(this.usePathFilter);
			bg.add(this.useFilter);
			
			//	listen to mode selection changes
			this.usePathFilter.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent ie) {
					filterModeChanged();
				}
			});
			this.useFilter.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent ie) {
					filterModeChanged();
				}
			});
			
			//	initialize path filter
			this.filterPathField.setBorder(BorderFactory.createLoweredBevelBorder());
			this.filterPathField.addKeyListener(new KeyAdapter() {
				public void keyTyped(KeyEvent e) {
					dirty = true;
				}
			});
			
			//	initialize filter selector
			ArrayList filterList = new ArrayList();
			AnnotationFilterManager[] afms = parent.getAnnotationFilterProviders();
			for (int m = 0; m < afms.length; m++) {
				String[] afns = afms[m].getResourceNames();
				for (int n = 0; n < afns.length; n++) {
					AnnotationFilter af = afms[m].getAnnotationFilter(afns[n]);
					if (af != null)
						filterList.add(new SelectableAnnotationFilter(af));
				}
			}
			SelectableAnnotationFilter[] filters = ((SelectableAnnotationFilter[]) filterList.toArray(new SelectableAnnotationFilter[filterList.size()]));
			Arrays.sort(filters, new Comparator() {
				public int compare(Object o1, Object o2) {
					return o1.toString().compareToIgnoreCase(o2.toString());
				}
			});
			this.filterSelector = new JComboBox(filters);
			this.filterSelector.setEditable(false);
			this.filterSelector.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent ie) {
					dirty = true;
				}
			});
			
			//	reflect model settings
			if (settings == null)
				this.usePathFilter.setSelected(true);
			
			else if (settings.containsKey(ANNOTATION_FILTER_PATH_PARAMETER)) {
				this.usePathFilter.setSelected(true);
				this.filterPathField.setText(settings.getSetting(ANNOTATION_FILTER_PATH_PARAMETER));
			}
			else if (settings.containsKey(ANNOTATION_FILTER_NAME_PARAMETER)) {
				AnnotationFilter af = parent.getAnnotationFilterForName(settings.getSetting(ANNOTATION_FILTER_NAME_PARAMETER));
				if (af == null)
					this.usePathFilter.setSelected(true);
				else {
					this.useFilter.setSelected(true);
					this.filterSelector.setSelectedItem(new SelectableAnnotationFilter(af));
				}
			}
			else this.usePathFilter.setSelected(true);
			
			//	changes count from now on
			this.dirty = false;
			
			//	put the whole stuff together
			this.add(this.usePathFilter);
			this.add(this.filterPathField);
			this.add(this.useFilter);
			this.add(this.filterSelector);
		}
		
		private void filterModeChanged() {
			this.filterPathField.setEnabled(this.usePathFilter.isSelected());
			this.filterSelector.setEnabled(this.useFilter.isSelected());
			this.dirty = true;
		}
		
		private class SelectableAnnotationFilter {
			String filterName;
			String filterTypeLabel;
			String providerClassName;
			SelectableAnnotationFilter(AnnotationFilter af) {
				this.filterName = af.getName();
				this.filterTypeLabel = af.getTypeLabel();
				this.providerClassName = af.getProviderClassName();
			}
			public String toString() {
				return (this.filterTypeLabel + ": " + this.filterName);
			}
			String toDataString() {
				return (this.filterName + "@" + this.providerClassName);
			}
			public boolean equals(Object obj) {
				return this.toString().equals(obj.toString());
			}
			public int hashCode() {
				return this.toString().hashCode();
			}
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentViewParameterPanel#isDirty()
		 */
		public boolean isDirty() {
			return this.dirty;
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentViewParameterPanel#getSettings()
		 */
		public Settings getSettings() {
			Settings set = new Settings();
			if (this.usePathFilter.isSelected()) {
				String filterPath = this.filterPathField.getText().trim();
				if (filterPath.length() != 0)
					set.setSetting(ANNOTATION_FILTER_PATH_PARAMETER, filterPath.toString());
			}
			if (this.useFilter.isSelected()) {
				Object filterObject = this.filterSelector.getSelectedItem();
				if (filterObject != null)
					set.setSetting(ANNOTATION_FILTER_NAME_PARAMETER, ((SelectableAnnotationFilter) filterObject).toDataString());
			}
			return set;
		}
	}
	
	private class AnnotationNavigatorDialog extends DialogPanel {
		
		private AnnotationNavigatorPanel annotationDisplay;
		
		private String originalTitle;
		
		private JPanel customFilterPanel = new JPanel(new BorderLayout());
		
		private StringVector customFilterNames = new StringVector();
		private StringVector annotationTypeFilterNames = new StringVector();
		
		private JComboBox customFilterSelector = new JComboBox();
		private boolean customFilterSelectorKeyPressed = false;
		
		
		private JPanel typePredicateFilterPanel = new JPanel(new BorderLayout());
		
		private JComboBox typeSelector = new JComboBox();
		private StringVector predicateHistory = new StringVector();
		private HashMap tpFiltersByName = new HashMap();
		
		private JComboBox predicateSelector = new JComboBox();
		private boolean predicateSelectorKeyPressed = false;
		
		AnnotationNavigatorDialog(String title, MutableAnnotation data, DocumentEditor target, AnnotationFilter filter) {
			super(title, true);
			this.originalTitle = title;
			
			JButton closeButton = new JButton("Close");
			closeButton.setBorder(BorderFactory.createRaisedBevelBorder());
			closeButton.setPreferredSize(new Dimension(100, 21));
			closeButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					AnnotationNavigatorDialog.this.dispose();
				}
			});
			
			//	create editor
			this.annotationDisplay = new AnnotationNavigatorPanel(data, target);
			
			//	set filter if given
			if (filter != null)
				this.annotationDisplay.setFilter(filter);
			
			//	put the whole stuff together
			if (filter == null)
				this.add(this.buildFilterTabs(data, target), BorderLayout.NORTH);
			this.add(this.annotationDisplay, BorderLayout.CENTER);
			this.add(closeButton, BorderLayout.SOUTH);
			
			this.setResizable(true);
			this.setSize(new Dimension(500, 650));
			this.setLocationRelativeTo((target == null) ? ((Component) this.getOwner()) : ((Component) target));
		}
		
		private JTabbedPane buildFilterTabs(MutableAnnotation data, DocumentEditor target) {
			JTabbedPane filterTabs = new JTabbedPane();
			
			//	build custom filter panel
			AnnotationFilter[] filters = ((target == null) ? DocumentEditor.getActiveAnnotationFilters() : target.getAnnotationFilters());
			for (int f = 0; f < filters.length; f++) {
				String filterName = filters[f].toString();
				this.customFilterNames.addElement(filterName);
				customFiltersByName.put(filterName, filters[f]);
			}
			
			//	remember which filters are type based
			this.annotationTypeFilterNames.addContent(data.getAnnotationTypes());
			
			StringVector filterNames = new StringVector();
			filterNames.addContentIgnoreDuplicates(customFilterHistory);
			filterNames.addContent(this.customFilterNames);
			
			this.customFilterSelector.setModel(new DefaultComboBoxModel(filterNames.toStringArray()));
			this.customFilterSelector.setEditable(true);
			this.customFilterSelector.setSelectedItem("");
			this.customFilterSelector.setBorder(BorderFactory.createLoweredBevelBorder());
			this.customFilterSelector.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					if (!customFilterSelector.isVisible()) return;
					
					else if (customFilterSelectorKeyPressed && !customFilterSelector.isPopupVisible())
						AnnotationNavigatorDialog.this.applyCustomFilter();
					
					else if (customFilterNames.contains(customFilterSelector.getSelectedItem().toString()))
						AnnotationNavigatorDialog.this.applyCustomFilter();
				}
			});
			((JTextComponent) this.customFilterSelector.getEditor().getEditorComponent()).addKeyListener(new KeyAdapter() {
				public void keyPressed(KeyEvent ke) {
					customFilterSelectorKeyPressed = true;
				}
				public void keyReleased(KeyEvent ke) {
					customFilterSelectorKeyPressed = false;
				}
			});
			
			JButton applyCustomFilterButton = new JButton("Apply");
			applyCustomFilterButton.setBorder(BorderFactory.createRaisedBevelBorder());
			applyCustomFilterButton.setPreferredSize(new Dimension(50, 21));
			applyCustomFilterButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					AnnotationNavigatorDialog.this.applyCustomFilter();
				}
			});
			
			this.customFilterPanel.add(this.customFilterSelector, BorderLayout.CENTER);
			this.customFilterPanel.add(applyCustomFilterButton, BorderLayout.EAST);
			
			//	build type & predicate filter panel
			String[] types = data.getAnnotationTypes();
			Arrays.sort(types, ANNOTATION_TYPE_ORDER);
			
			this.typeSelector.setModel(new DefaultComboBoxModel(types));
			this.typeSelector.setEditable(false);
			this.typeSelector.setBorder(BorderFactory.createLoweredBevelBorder());
			this.typeSelector.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					if (typeSelector.isVisible())
						AnnotationNavigatorDialog.this.applyTpFilter();
				}
			});
			
			this.predicateSelector.setModel(new DefaultComboBoxModel(types));
			this.predicateSelector.setEditable(true);
			this.predicateSelector.setBorder(BorderFactory.createLoweredBevelBorder());
			this.predicateSelector.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					if (predicateSelectorKeyPressed && predicateSelector.isVisible() && !predicateSelector.isPopupVisible())
						AnnotationNavigatorDialog.this.applyTpFilter();
				}
			});
			((JTextComponent) this.predicateSelector.getEditor().getEditorComponent()).addKeyListener(new KeyAdapter() {
				public void keyPressed(KeyEvent ke) {
					predicateSelectorKeyPressed = true;
				}
				public void keyReleased(KeyEvent ke) {
					predicateSelectorKeyPressed = false;
				}
			});
			
			JButton applyTpFilterButton = new JButton("Apply");
			applyTpFilterButton.setBorder(BorderFactory.createRaisedBevelBorder());
			applyTpFilterButton.setPreferredSize(new Dimension(50, 21));
			applyTpFilterButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					AnnotationNavigatorDialog.this.applyTpFilter();
				}
			});
			
			this.typePredicateFilterPanel.add(this.typeSelector, BorderLayout.WEST);
			this.typePredicateFilterPanel.add(this.predicateSelector, BorderLayout.CENTER);
			this.typePredicateFilterPanel.add(applyTpFilterButton, BorderLayout.EAST);
			
			//	build filter panel
			filterTabs.addTab("Custom Filters", null, this.customFilterPanel, "Pre-configured & custom filters.");
			filterTabs.addTab("Type & Predicate Filters", null, this.typePredicateFilterPanel, "Filter with annotation type and custom predicate.");
			return filterTabs;
		}
		
		void applyCustomFilter() {
			Object filterObject = this.customFilterSelector.getSelectedItem();
			if (filterObject != null) {
				final String filterString = filterObject.toString().trim();
				
				//	filter from provider selected
				if (this.customFilterNames.contains(filterString)) {
					this.annotationDisplay.setFilter((AnnotationFilter) customFiltersByName.get(filterString));
					this.customFilterSelector.setEditable(this.annotationTypeFilterNames.contains(filterString));
				}
				
				//	filter entered manually or selected from history
				else {
					this.customFilterSelector.setEditable(true);
					
					//	filter from history selected
					/*
					 * if (customFiltersHistory.contains(filterString))
					 * 
					 * because we load recently used filters from file now, we
					 * have to check is we have to re-generate the filter object
					 * dirctly on the map
					 */
					if (customFiltersByName.containsKey(filterString))
						this.annotationDisplay.setFilter((AnnotationFilter) customFiltersByName.get(filterString));
					
					//	new filter entered
					else {
						
						//	validate & compile path expression
						String error = GPathParser.validatePath(filterString);
						
						GPath filterPath = null;
						if (error == null) try {
							filterPath = new GPath(filterString);
						}
						catch (Exception e) {
							error = e.getMessage();
						}
						
						//	validation successful
						if (error == null) {
							
							//	create & cache filter
							final GPath fFilterPath = filterPath;
							AnnotationFilter filter = new AnnotationFilter() {
								public boolean accept(Annotation annotation) {
									return false;
								}
								public QueriableAnnotation[] getMatches(QueriableAnnotation data) {
									try {
										return fFilterPath.evaluate(data, GPath.getDummyVariableResolver());
									}
									catch (GPathException gpe) {
										JOptionPane.showMessageDialog(AnnotationNavigatorDialog.this, gpe.getMessage(), "GPath Error", JOptionPane.ERROR_MESSAGE);
										return new QueriableAnnotation[0];
									}
								}
								public MutableAnnotation[] getMutableMatches(MutableAnnotation data) {
									QueriableAnnotation[] matches = this.getMatches(data);
									Set matchIDs = new HashSet();
									for (int m = 0; m < matches.length; m++)
										matchIDs.add(matches[m].getAnnotationID());
									MutableAnnotation[] mutableAnnotations = data.getMutableAnnotations();
									ArrayList mutableMatches = new ArrayList();
									for (int m = 0; m < mutableAnnotations.length; m++)
										if (matchIDs.contains(mutableAnnotations[m].getAnnotationID()))
											mutableMatches.add(mutableAnnotations[m]);
									return ((MutableAnnotation[]) mutableMatches.toArray(new MutableAnnotation[mutableMatches.size()]));
								}
								public String getName() {
									return filterString;
								}
								public String getProviderClassName() {
									return "Homegrown";
								}
								public String getTypeLabel() {
									return "Custom Filter";
								}
								public boolean equals(Object obj) {
									return ((obj != null) && filterString.equals(obj.toString()));
								}
								public String toString() {
									return filterString;
								}
							};
							customFiltersByName.put(filterString, filter);
							
							//	make way
							customFilterHistory.removeAll(filterString);
							this.customFilterSelector.removeItem(filterString);
							
							//	store new filter in history
							customFilterHistory.insertElementAt(filterString, 0);
							this.customFilterSelector.insertItemAt(filterString, 0);
							this.customFilterSelector.setSelectedIndex(0);
							
							//	shrink history
							while (customFilterHistory.size() > customFilterHistorySize) {
								customFiltersByName.remove(customFilterHistory.get(customFilterHistorySize));
								customFilterHistory.remove(customFilterHistorySize);
								customFilterSelector.removeItemAt(customFilterHistorySize);
							}
							
							//	apply filter
							this.annotationDisplay.setFilter(filter);
						}
						
						//	path validation error
						else {
							JOptionPane.showMessageDialog(this, ("The expression is not valid:\n" + error), "GPath Validation", JOptionPane.ERROR_MESSAGE);
							this.customFilterSelector.requestFocusInWindow();
						}
					}
				}
			}
		}
		
		void applyTpFilter() {
			Object typeObject = this.typeSelector.getSelectedItem();
			Object predicateObject = this.predicateSelector.getSelectedItem();
			
			if ((typeObject != null) && (predicateObject != null)) {
				
				final String typeString = typeObject.toString().trim();
				String predicateString = predicateObject.toString().trim();
				if (predicateString.startsWith("["))
					predicateString = predicateString.substring(1);
				if (predicateString.endsWith("]"))
					predicateString = predicateString.substring(0, (predicateString.length() - 1));
				
				final String filterString = ("/" + typeString + "[" + predicateString + "]");
				
				//	filter entered manually or selected from history
				this.customFilterSelector.setEditable(true);
				
				//	filter from history selected
				if (this.tpFiltersByName.containsKey(filterString))
					this.annotationDisplay.setFilter((AnnotationFilter) this.tpFiltersByName.get(filterString));
				
				//	new filter entered
				else {
					
					//	validate path expression
					String error = GPathParser.validatePath(predicateString);
					
					//	validation successful
					if (error == null) {
						
						//	compile path
						final GPathExpression predicateExpression = GPathParser.parseExpression(predicateString);
						
						//	create & cache filter
						AnnotationFilter filter = new AnnotationFilter() {
							public boolean accept(Annotation annotation) {
								return false;
							}
							public QueriableAnnotation[] getMatches(QueriableAnnotation data) {
								QueriableAnnotation[] annotations = data.getAnnotations(typeString);
								ArrayList matches = new ArrayList();
								for (int m = 0; m < annotations.length; m++)
									try {
										if (GPath.evaluateExpression(predicateExpression, annotations[m], GPath.getDummyVariableResolver()).asBoolean().value)
											matches.add(annotations[m]);
									}
									catch (GPathException gpe) {
										JOptionPane.showMessageDialog(AnnotationNavigatorDialog.this, gpe.getMessage(), "GPath Error", JOptionPane.ERROR_MESSAGE);
									}
								return ((QueriableAnnotation[]) matches.toArray(new QueriableAnnotation[matches.size()]));
							}
							public MutableAnnotation[] getMutableMatches(MutableAnnotation data) {
								MutableAnnotation[] annotations = data.getMutableAnnotations(typeString);
								ArrayList mutableMatches = new ArrayList();
								for (int m = 0; m < annotations.length; m++)
									try {
										if (GPath.evaluateExpression(predicateExpression, annotations[m], GPath.getDummyVariableResolver()).asBoolean().value)
											mutableMatches.add(annotations[m]);
									}
									catch (GPathException gpe) {
										JOptionPane.showMessageDialog(AnnotationNavigatorDialog.this, gpe.getMessage(), "GPath Error", JOptionPane.ERROR_MESSAGE);
									}
								return ((MutableAnnotation[]) mutableMatches.toArray(new MutableAnnotation[mutableMatches.size()]));
							}
							public String getName() {
								return filterString;
							}
							public String getProviderClassName() {
								return "Homegrown";
							}
							public String getTypeLabel() {
								return "Type & Predicate Filter";
							}
							public boolean equals(Object obj) {
								return ((obj != null) && filterString.equals(obj.toString()));
							}
							public String toString() {
								return filterString;
							}
						};
						this.tpFiltersByName.put(filterString, filter);
						
						//	store new filter in history
						this.predicateHistory.insertElementAt(predicateString, 0);
						this.predicateSelector.insertItemAt(predicateString, 0);
						this.predicateSelector.setSelectedIndex(0);
						
						//	shrink history
						while (this.predicateHistory.size() > 10) {
							this.predicateHistory.remove(10);
							this.predicateSelector.removeItemAt(10);
						}
						
						//	apply filter
						this.annotationDisplay.setFilter(filter);
					}
					
					//	path validation error
					else {
						JOptionPane.showMessageDialog(this, ("The predicate is not valid:\n" + error), "GPath Validation", JOptionPane.ERROR_MESSAGE);
						this.customFilterSelector.requestFocusInWindow();
					}
				}
			}
		}
		
		/**
		 * the panel doing the actual displaying
		 * 
		 * @author sautter
		 */
		private class AnnotationNavigatorPanel extends JPanel {
			
			private Dimension editDialogSize = new Dimension(800, 600);
			private Point editDialogLocation = null;
			
			private String[] taggedTypes = {};
			private String[] highlightTypes = {};
			
			private Dimension attributeDialogSize = new Dimension(400, 300);
			private Point attributeDialogLocation = null;
			
			private JTable annotationTable;
			
			private AnnotationTray[] annotationTrays;
			private HashMap annotationTraysByID = new HashMap();
			
			private MutableAnnotation data;
			private DocumentEditor target;
			private AnnotationFilter filter = null;
			
			private JRadioButton showMatches = new JRadioButton("Show Matches Only", true);
			private JRadioButton highlightMatches = new JRadioButton("Highlight Matches", false);
			
			private int sortColumn = -1;
			private boolean sortDescending = false;
			
			AnnotationNavigatorPanel(MutableAnnotation data, DocumentEditor target) {
				super(new BorderLayout(), true);
				this.setBorder(BorderFactory.createEtchedBorder());
				
				this.data = data;
				this.target = target;
				
				//	read base layout settings
				this.taggedTypes = ((this.target == null) ? new String[0] : this.target.getTaggedAnnotationTypes());
				this.highlightTypes = ((this.target == null) ? new String[0] : this.target.getHighlightAnnotationTypes());
				
				//	initialize display
				this.annotationTable = new JTable();
				this.annotationTable.setDefaultRenderer(Object.class, new TooltipAwareComponentRenderer(5, data.getTokenizer()));
				this.annotationTable.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
				this.annotationTable.addMouseListener(new MouseAdapter() {
					public void mouseClicked(MouseEvent me) {
						
						int clickRowIndex = annotationTable.rowAtPoint(me.getPoint());
						int rowIndex = annotationTable.getSelectedRow();
						if ((clickRowIndex != -1) && ((clickRowIndex < rowIndex) || (clickRowIndex >= (rowIndex + annotationTable.getSelectedRowCount())))) {
							ListSelectionModel lsm = annotationTable.getSelectionModel();
							if (lsm != null) lsm.setSelectionInterval(clickRowIndex, clickRowIndex);
						}
						
						if (me.getButton() != MouseEvent.BUTTON1)
							showContextMenu(me);
						
						else if ((me.getClickCount() > 1) && (annotationTable.getSelectedRowCount() == 1))
							editAnnotationAttributes(annotationTrays[clickRowIndex].annotation);
					}
				});
				
				final JTableHeader header = this.annotationTable.getTableHeader();
				header.setReorderingAllowed(false);
				header.addMouseListener(new MouseAdapter() {
					public void mouseClicked(MouseEvent me) {
		                int newSortColumn = header.columnAtPoint(me.getPoint());
		                if (newSortColumn == sortColumn)
		                	sortDescending = !sortDescending;
		                else {
		                	sortDescending = false;
		                	sortColumn = newSortColumn;
		                }
		                sortAnnotations();
					}
				});
				
				this.refreshAnnotations();
				
				JScrollPane annotationTableBox = new JScrollPane(this.annotationTable);
				
				
				this.showMatches.addItemListener(new ItemListener() {
					public void itemStateChanged(ItemEvent ie) {
						if (showMatches.isSelected()) {
							refreshAnnotations();
						}
					}
				});
				this.highlightMatches.addItemListener(new ItemListener() {
					public void itemStateChanged(ItemEvent ie) {
						if (highlightMatches.isSelected()) {
							refreshAnnotations();
						}
					}
				});
				
				ButtonGroup displayModeButtonGroup = new ButtonGroup();
				displayModeButtonGroup.add(this.showMatches);
				displayModeButtonGroup.add(this.highlightMatches);
				
				JPanel displayModePanel = new JPanel(new GridLayout(1, 2));
				displayModePanel.add(this.showMatches);
				displayModePanel.add(this.highlightMatches);
				
				this.add(displayModePanel, BorderLayout.NORTH);
				this.add(annotationTableBox, BorderLayout.CENTER);
			}
			
			void setFilter(AnnotationFilter filter) {
				this.filter = filter;
				this.refreshAnnotations();
			}
			
			void refreshAnnotations() {
				
				//	apply filter
				MutableAnnotation[] annotations = ((this.filter == null) ? new MutableAnnotation[0] : this.filter.getMutableMatches(this.data));
				this.annotationTrays = new AnnotationTray[annotations.length];
				
				//	set up statistics
				String type = ((annotations.length == 0) ? "" : annotations[0].getType());
				Set matchIDs = new HashSet();
				
				//	check matching annotations
				for (int a = 0; a < annotations.length; a++) {
					if (!type.equals(annotations[a].getType()))
						type = "";
					
					matchIDs.add(annotations[a].getAnnotationID());
					
					if (this.annotationTraysByID.containsKey(annotations[a].getAnnotationID()))
						this.annotationTrays[a] = ((AnnotationTray) this.annotationTraysByID.get(annotations[a].getAnnotationID()));
					
					else {
						this.annotationTrays[a] = new AnnotationTray(annotations[a]);
						this.annotationTraysByID.put(annotations[a].getAnnotationID(), this.annotationTrays[a]);
					}
					
					this.annotationTrays[a].isMatch = true;
				}
				
				//	more than one type
				if (type.length() == 0) {
					this.showMatches.setSelected(true);
					this.showMatches.setEnabled(false);
					this.highlightMatches.setEnabled(false);
					
					setTitle(originalTitle + " - " + annotations.length + " Annotations");
				}
				
				//	all of same type, do match highlight display if required
				else {
					this.showMatches.setEnabled(true);
					this.highlightMatches.setEnabled(true);
					
					//	highlight matches
					if (this.highlightMatches.isSelected()) {
						annotations = this.data.getMutableAnnotations(type);
						this.annotationTrays = new AnnotationTray[annotations.length];
						
						for (int a = 0; a < annotations.length; a++) {
							
							if (this.annotationTraysByID.containsKey(annotations[a].getAnnotationID()))
								this.annotationTrays[a] = ((AnnotationTray) this.annotationTraysByID.get(annotations[a].getAnnotationID()));
							
							else {
								this.annotationTrays[a] = new AnnotationTray(annotations[a]);
								this.annotationTraysByID.put(annotations[a].getAnnotationID(), this.annotationTrays[a]);
							}
							
							this.annotationTrays[a].isMatch = matchIDs.contains(annotations[a].getAnnotationID());
						}
						
						setTitle(originalTitle + " - " + annotations.length + " Annotations, " + matchIDs.size() + " matches");
					}
					
					else setTitle(originalTitle + " - " + annotations.length + " Annotations");
				}
				
				this.annotationTable.setModel(new AnnotationNavigatorTableModel(this.annotationTrays));
				this.annotationTable.getColumnModel().getColumn(0).setMaxWidth(120);
				this.annotationTable.getColumnModel().getColumn(1).setMaxWidth(50);
				this.annotationTable.getColumnModel().getColumn(2).setMaxWidth(50);
				
				this.sortColumn = -1;
				this.sortDescending = false;
				this.refreshDisplay();
			}
			
			void sortAnnotations() {
				Arrays.sort(this.annotationTrays, new Comparator() {
					public int compare(Object o1, Object o2) {
						AnnotationTray at1 = ((AnnotationTray) o1);
						AnnotationTray at2 = ((AnnotationTray) o2);
						int c;
						if (sortColumn == 0)
							c = at1.annotation.getType().compareToIgnoreCase(at2.annotation.getType());
						else if (sortColumn == 1)
							c = (at1.annotation.getStartIndex() - at2.annotation.getStartIndex());
						else if (sortColumn == 2)
							c = (at1.annotation.size() - at2.annotation.size());
						else if (sortColumn == 3)
							c = String.CASE_INSENSITIVE_ORDER.compare(at1.annotation.getValue(), at2.annotation.getValue());
						else c = 0;
						
						return ((sortDescending ? -1 : 1) * ((c == 0) ? AnnotationUtils.compare(at1.annotation, at2.annotation) : c));
					}
				});
				this.refreshDisplay();
			}
			
			void refreshDisplay() {
				for(int i = 0; i < this.annotationTable.getColumnCount();i++)
					this.annotationTable.getColumnModel().getColumn(i).setHeaderValue(this.annotationTable.getModel().getColumnName(i));
				this.annotationTable.getTableHeader().revalidate();
				this.annotationTable.getTableHeader().repaint();
				this.annotationTable.revalidate();
				this.annotationTable.repaint();
				AnnotationNavigatorDialog.this.validate();
			}
			
			void editAnnotation(MutableAnnotation annotation) {
				
				//	create dialog & show
				DocumentEditDialog ded = new DocumentEditDialog(AnnotationNavigatorDialog.this.getDialog(), NavigateAnnotationsViewer.this.parent, target, "Edit Annotation", annotation);
				ded.setVisible(true);
				
				//	finish
				if (ded.committed && ded.isContentModified()) this.refreshAnnotations();
			}
			
			class DocumentEditDialog extends DocumentEditorDialog {
				private boolean committed = false;
				private DocumentEditDialog(JDialog owner, GoldenGATE host, DocumentEditor parent, String title, MutableAnnotation data) {
					super(owner, host, parent, title, data);
					
					JButton okButton = new JButton("OK");
					okButton.setBorder(BorderFactory.createRaisedBevelBorder());
					okButton.setPreferredSize(new Dimension(100, 21));
					okButton.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							DocumentEditDialog.this.committed = true;
							DocumentEditDialog.this.documentEditor.writeChanges();
							DocumentEditDialog.this.dispose();
						}
					});
					this.mainButtonPanel.add(okButton);
					
					JButton cancelButton = new JButton("Cancel");
					cancelButton.setBorder(BorderFactory.createRaisedBevelBorder());
					cancelButton.setPreferredSize(new Dimension(100, 21));
					cancelButton.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							DocumentEditDialog.this.dispose();
						}
					});
					this.mainButtonPanel.add(cancelButton);
					
					for (int t = 0; t < taggedTypes.length; t++)
						this.documentEditor.setAnnotationTagVisible(taggedTypes[t], true);
					for (int t = 0; t < highlightTypes.length; t++)
						this.documentEditor.setAnnotationValueHighlightVisible(highlightTypes[t], true);
					
					this.setSize(editDialogSize);
					if (editDialogLocation == null) this.setLocationRelativeTo(AnnotationNavigatorDialog.this);
					else this.setLocation(editDialogLocation);
				}
				
				public void dispose() {
					editDialogSize = this.getSize();
					editDialogLocation = this.getLocation(editDialogLocation);
					taggedTypes = this.documentEditor.getTaggedAnnotationTypes();
					highlightTypes = this.documentEditor.getHighlightAnnotationTypes();
					
					super.dispose();
				}
			}
			
			void editAnnotationAttributes(MutableAnnotation annotation) {
				
				//	create dialog
				AttributeEditorDialog aed = new AttributeEditorDialog(AnnotationNavigatorDialog.this.getDialog(), "Edit Annotation Attributes", annotation, data) {
					public void dispose() {
						attributeDialogSize = this.getSize();
						attributeDialogLocation = this.getLocation(attributeDialogLocation);
						super.dispose();
					}
				};
				
				//	position and show dialog
				aed.setSize(attributeDialogSize);
				if (attributeDialogLocation == null) aed.setLocationRelativeTo(DialogPanel.getTopWindow());
				else aed.setLocation(attributeDialogLocation);
				aed.setVisible(true);
				
				//	finish
				if (aed.isDirty()) refreshAnnotations();
			}
			
			void modifyAnnotationAttribute(int[] rows) {
				Annotation[] annotations = new Annotation[rows.length];
				for (int r = 0; r < rows.length; r++)
					annotations[r] = this.annotationTrays[rows[r]].annotation;
				if (annotations.length == 0) return;
				
				ModifyAttributeDialog mad = new ModifyAttributeDialog(annotations);
				mad.setVisible(true);
				
				if (mad.isModified()) refreshAnnotations();
			}
			
			void removeAnnotationAttribute(int[] rows) {
				Annotation[] annotations = new Annotation[rows.length];
				for (int r = 0; r < rows.length; r++)
					annotations[r] = this.annotationTrays[rows[r]].annotation;
				if (annotations.length == 0) return;
				
				RemoveAttributeDialog rad = new RemoveAttributeDialog(annotations);
				rad.setVisible(true);
				
				if (rad.isModified()) refreshAnnotations();
			}
			
			void showContextMenu(MouseEvent me) {
				int row = this.annotationTable.getSelectedRow();
				if (row == -1) return;
				
				int rows = this.annotationTable.getSelectedRowCount();
				final AnnotationTray tray = this.annotationTrays[row];
				final AnnotationTray[] trays = new AnnotationTray[rows];
				System.arraycopy(annotationTrays, row, trays, 0, rows);
				Arrays.sort(trays, new Comparator() {
					private Comparator comp = AnnotationUtils.getComparator(data.getAnnotationNestingOrder());
					public int compare(Object o1, Object o2) {
						return this.comp.compare(((AnnotationTray) o1).annotation, ((AnnotationTray) o1).annotation);
					}
				});
				
				JMenu menu = new JMenu();
				JMenuItem mi;
				
				mi = new JMenuItem("Edit");
				mi.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						if (trays.length == 1)
							editAnnotation(tray.annotation);
						
						else {
							MutableAnnotation temp = data.addAnnotation(TEMP_ANNOTATION_TYPE, trays[0].annotation.getStartIndex(), (trays[trays.length - 1].annotation.getEndIndex() - trays[0].annotation.getStartIndex()));
							editAnnotation(temp);
							data.removeAnnotation(temp);
						}
					}
				});
				menu.add(mi);
				
				if (rows == 1) {
					mi = new JMenuItem("Edit Attributes");
					mi.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							editAnnotationAttributes(tray.annotation);
						}
					});
					menu.add(mi);
				}
				
				else if (rows != 0) {
					mi = new JMenuItem("Modify Attribute");
					mi.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							modifyAnnotationAttribute(annotationTable.getSelectedRows());
						}
					});
					menu.add(mi);
					mi = new JMenuItem("Remove Attribute");
					mi.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							removeAnnotationAttribute(annotationTable.getSelectedRows());
						}
					});
					menu.add(mi);
				}
				menu.addSeparator();
				
				boolean sameType = true;
				final String type = tray.annotation.getType();
				for (int r = 1; r < rows; r++)
					sameType = (sameType && trays[r].annotation.getType().equals(type));
				
				if (sameType) {
					mi = new JMenuItem("Rename");
					mi.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							String[] types = ((target == null) ? data.getAnnotationTypes() : target.getAnnotationTypes());
							Arrays.sort(types, ANNOTATION_TYPE_ORDER);
							
							RenameAnnotationDialog rad = new RenameAnnotationDialog(type, types);
							rad.setVisible(true);
							if (rad.targetType == null) return;
							
							String newType = rad.targetType.trim();
							if ((newType.length() != 0) && !newType.equals(type)) {
								for (int r = 0; r < trays.length; r++)
									trays[r].annotation.changeTypeTo(newType);
								refreshAnnotations();
							}
						}
					});
					menu.add(mi);
					menu.addSeparator();
				}
				
				mi = new JMenuItem("Remove");
				mi.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						for (int r = 0; r < trays.length; r++)
							data.removeAnnotation(trays[r].annotation);
						refreshAnnotations();
					}
				});
				menu.add(mi);
				
				if (rows == 1) {
					mi = new JMenuItem("Remove All");
					mi.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							String value = tray.annotation.getValue();
							int annotationCount = 0;
							for (int a = 0; a < annotationTrays.length; a++)
								if (annotationTrays[a].annotation.getValue().equals(value)) {
									annotationCount ++;
									data.removeAnnotation(annotationTrays[a].annotation);
								}
							if (annotationCount > 0) refreshAnnotations();
						}
					});
					menu.add(mi);
				}
				
				mi = new JMenuItem("Delete");
				mi.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						for (int r = trays.length; r > 0; r--)
							data.removeTokens(trays[r - 1].annotation);
						refreshAnnotations();
					}
				});
				menu.add(mi);
				
				menu.getPopupMenu().show(this.annotationTable, me.getX(), me.getY());
			}
			
			private class RenameAnnotationDialog extends DialogPanel {
				
				private String targetType;
				private JComboBox targetTypeSelector;
				
				private boolean keyPressed = false;
				
				RenameAnnotationDialog(String sourceType, String[] existingTypes) {
					super("Rename Annotation", true);
					
					JLabel sourceTypeLabel = new JLabel(" " + sourceType);
					sourceTypeLabel.setBorder(BorderFactory.createLoweredBevelBorder());
					
					this.targetTypeSelector = new JComboBox(existingTypes);
					this.targetTypeSelector.setBorder(BorderFactory.createLoweredBevelBorder());
					this.targetTypeSelector.setEditable(true);
					this.targetTypeSelector.setSelectedItem(sourceType);
					this.targetTypeSelector.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							if (keyPressed && isVisible() && !targetTypeSelector.isPopupVisible()) commit();
						}
					});
					((JTextComponent) this.targetTypeSelector.getEditor().getEditorComponent()).addKeyListener(new KeyAdapter() {
						public void keyPressed(KeyEvent ke) {
							keyPressed = true;
						}
						public void keyReleased(KeyEvent ke) {
							keyPressed = false;
						}
					});
					
					JPanel selectorPanel = new JPanel(new GridBagLayout());
					GridBagConstraints gbc = new GridBagConstraints();
					gbc.weighty = 0;
					gbc.gridwidth = 1;
					gbc.gridheight = 1;
					gbc.insets.top = 5;
					gbc.insets.left = 5;
					gbc.insets.right = 5;
					gbc.insets.bottom = 5;
					gbc.fill = GridBagConstraints.HORIZONTAL;
					
					gbc.gridy = 0;
					gbc.gridx = 0;
					gbc.weightx = 0;
					selectorPanel.add(new JLabel("Current Annotation Type"), gbc.clone());
					gbc.gridx = 1;
					gbc.weightx = 1;
					selectorPanel.add(sourceTypeLabel, gbc.clone());
					
					gbc.gridy = 1;
					gbc.gridx = 0;
					gbc.weightx = 0;
					selectorPanel.add(new JLabel("Rename Annotation To"), gbc.clone());
					gbc.gridx = 1;
					gbc.weightx = 1;
					selectorPanel.add(this.targetTypeSelector, gbc.clone());
					
					//	initialize main buttons
					JButton commitButton = new JButton("Rename");
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
							RenameAnnotationDialog.this.dispose();
						}
					});
					
					JPanel mainButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
					mainButtonPanel.add(commitButton);
					mainButtonPanel.add(abortButton);
					
					//	put the whole stuff together
					this.setLayout(new BorderLayout());
					this.add(selectorPanel, BorderLayout.CENTER);
					this.add(mainButtonPanel, BorderLayout.SOUTH);
					
					this.setResizable(true);
					this.setSize(new Dimension(400, 120));
					this.setLocationRelativeTo(AnnotationNavigatorPanel.this);
				}
				
				private void commit() {
					Object item = targetTypeSelector.getSelectedItem();
					this.targetType = ((item == null) ? "" : item.toString());
					this.dispose();
				}
			}
			
			/**
			 * dialog for entering the parameters of an attribute modification operation
			 * 
			 * @author sautter
			 */
			private class ModifyAttributeDialog extends DialogPanel {
				
				private static final String allValues = "<All Values>";
				
				private Annotation[] annotations;
				
				private JComboBox attributeSelector;
				private JComboBox oldValueSelector;
				private JComboBox newValueSelector;
				
				private JRadioButton addButton = new JRadioButton("Add where not set", true);
				private JRadioButton setButton = new JRadioButton("Set everywhere");
				private JRadioButton changeButton = new JRadioButton("Change value above");
				
				private boolean isModified = false;
				private boolean keyPressed = false;
				
				ModifyAttributeDialog(Annotation[] annotations) {
					super("Modify Annotation Attributes", true);
					this.annotations = annotations;
					
					//	initialize selector
					this.attributeSelector = new JComboBox();
					this.attributeSelector.setBorder(BorderFactory.createLoweredBevelBorder());
					this.attributeSelector.setEditable(true);
					this.attributeSelector.addItemListener(new ItemListener() {
						public void itemStateChanged(ItemEvent e) {
							attributeChanged();
						}
					});
					
					this.oldValueSelector = new JComboBox();
					this.oldValueSelector.setBorder(BorderFactory.createLoweredBevelBorder());
					this.oldValueSelector.setEditable(false);
					this.oldValueSelector.addItemListener(new ItemListener() {
						public void itemStateChanged(ItemEvent e) {
							oldValueChanged();
						}
					});
					
					this.newValueSelector = new JComboBox();
					this.newValueSelector.setBorder(BorderFactory.createLoweredBevelBorder());
					this.newValueSelector.setEditable(true);
					this.newValueSelector.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							if (keyPressed && isVisible() && !newValueSelector.isPopupVisible()) modifyAttribute();
						}
					});
					((JTextComponent) this.newValueSelector.getEditor().getEditorComponent()).addKeyListener(new KeyAdapter() {
						public void keyPressed(KeyEvent ke) {
							keyPressed = true;
						}
						public void keyReleased(KeyEvent ke) {
							keyPressed = false;
						}
					});
					
					
					StringVector attributeNameCollector = new StringVector();
					for (int a = 0; a < annotations.length; a++)
						attributeNameCollector.addContentIgnoreDuplicates(annotations[a].getAttributeNames());
					attributeNameCollector.sortLexicographically(false, false);
					
					this.attributeSelector.removeAllItems();
					for (int i = 0; i < attributeNameCollector.size(); i++)
						this.attributeSelector.addItem(attributeNameCollector.get(i));
					
					JPanel selectorPanel = new JPanel(new GridBagLayout());
					GridBagConstraints gbc = new GridBagConstraints();
					gbc.weighty = 1;
					gbc.gridwidth = 1;
					gbc.gridheight = 1;
					gbc.insets.top = 5;
					gbc.insets.left = 5;
					gbc.insets.right = 5;
					gbc.insets.bottom = 5;
					gbc.fill = GridBagConstraints.BOTH;
					
					gbc.gridy = 0;
					gbc.gridx = 0;
					gbc.weightx = 0;
					selectorPanel.add(new JLabel("Attribute To Modify"), gbc.clone());
					gbc.gridx = 1;
					gbc.weightx = 1;
					selectorPanel.add(this.attributeSelector, gbc.clone());
					
					gbc.gridy = 1;
					gbc.gridx = 0;
					gbc.weightx = 0;
					selectorPanel.add(new JLabel("Old Attribute Value"), gbc.clone());
					gbc.gridx = 1;
					gbc.weightx = 1;
					selectorPanel.add(this.oldValueSelector, gbc.clone());
					
					gbc.gridy = 2;
					gbc.gridx = 0;
					gbc.weightx = 0;
					selectorPanel.add(new JLabel("New Attribute Value"), gbc.clone());
					gbc.gridx = 1;
					gbc.weightx = 1;
					selectorPanel.add(this.newValueSelector, gbc.clone());
					
					ButtonGroup bg = new ButtonGroup();
					bg.add(this.addButton);
					bg.add(this.setButton);
					bg.add(this.changeButton);
					JPanel modePanel = new JPanel(new GridLayout(1,3));
					modePanel.add(this.addButton);
					modePanel.add(this.setButton);
					modePanel.add(this.changeButton);
					gbc.gridy = 3;
					gbc.gridx = 0;
					gbc.weightx = 1;
					gbc.gridwidth = 2;
					selectorPanel.add(modePanel, gbc.clone());
					
					//	initialize main buttons
					JButton commitButton = new JButton("Modify");
					commitButton.setBorder(BorderFactory.createRaisedBevelBorder());
					commitButton.setPreferredSize(new Dimension(100, 21));
					commitButton.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							modifyAttribute();
						}
					});
					
					JButton abortButton = new JButton("Cancel");
					abortButton.setBorder(BorderFactory.createRaisedBevelBorder());
					abortButton.setPreferredSize(new Dimension(100, 21));
					abortButton.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							ModifyAttributeDialog.this.dispose();
						}
					});
					
					JPanel mainButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
					mainButtonPanel.add(commitButton);
					mainButtonPanel.add(abortButton);
					
					//	put the whole stuff together
					this.setLayout(new BorderLayout());
					this.add(selectorPanel, BorderLayout.CENTER);
					this.add(mainButtonPanel, BorderLayout.SOUTH);
					
					this.setResizable(true);
					this.setSize(new Dimension(400, 180));
					this.setLocationRelativeTo(AnnotationNavigatorDialog.this);
				}
				
				void attributeChanged() {
					Object attributeItem = this.attributeSelector.getSelectedItem();
					if (attributeItem != null) {
						String attribute = attributeItem.toString();
						StringVector matchTypes = new StringVector();
						StringVector attributeNames = new StringVector();
						StringVector attributeValues = new StringVector();
						
						for (int a = 0; a < this.annotations.length; a++) {
							if (this.annotations[a].hasAttribute(attribute))
								attributeValues.addElementIgnoreDuplicates(this.annotations[a].getAttribute(attribute).toString());
						}
						
						attributeValues.sortLexicographically(false, false);
						this.oldValueSelector.removeAllItems();
						this.oldValueSelector.addItem(allValues);
						for (int i = 0; i < attributeValues.size(); i++)
							this.oldValueSelector.addItem(attributeValues.get(i));
						
						for (int t = 0; t < matchTypes.size(); t++) {
							String type = matchTypes.get(t);
							for (int a = 0; a < attributeNames.size(); a++)
								attributeValues.addContentIgnoreDuplicates(DocumentEditor.getAttributeValueSuggestions(type, attributeNames.get(a)));
						}
						
						attributeValues.sortLexicographically(false, false);
						this.newValueSelector.removeAllItems();
						for (int i = 0; i < attributeValues.size(); i++) {
							this.newValueSelector.addItem(attributeValues.get(i));
						}
					}
				}
				
				void oldValueChanged() {
					Object item = this.oldValueSelector.getSelectedItem();
					if (item != null) this.newValueSelector.setSelectedItem(item);
				}
				
				boolean isModified() {
					return this.isModified;
				}
				
				void modifyAttribute() {
					String attribute = this.attributeSelector.getSelectedItem().toString();
					Object oldValueObject = this.oldValueSelector.getSelectedItem();
					String oldValue = (allValues.equals(oldValueObject) ? null : oldValueObject.toString());
					Object newValueObject = this.newValueSelector.getSelectedItem();
					String newValue = ((newValueObject == null) ? "" : newValueObject.toString());
					
					if (this.addButton.isSelected())
						this.isModified = AnnotationTools.addAnnotationAttribute(this.annotations, attribute, newValue);
					else if (this.setButton.isSelected())
						this.isModified = AnnotationTools.setAnnotationAttribute(this.annotations, attribute, newValue);
					else if (this.changeButton.isSelected())
						this.isModified = AnnotationTools.changeAnnotationAttribute(this.annotations, attribute, oldValue, newValue);
					this.dispose();
				}
			}
			
			/**
			 * dialog for entering the parameters of an attribute removal operation
			 * 
			 * @author sautter
			 */
			private class RemoveAttributeDialog extends DialogPanel {
				
				private static final String ALL_ATTRIBUTES_ATTRIBUTE = "<All Attributes>";
				
				private Annotation[] annotations;
				
				private JComboBox attributeSelector;
				
				private boolean isModified = false;
				
				RemoveAttributeDialog(Annotation[] annotations) {
					super("Remove Annotation Attributes", true);
					this.annotations = annotations;
					
					this.attributeSelector = new JComboBox();
					this.attributeSelector.setBorder(BorderFactory.createLoweredBevelBorder());
					this.attributeSelector.setEditable(false);
					
					StringVector attributeNameCollector = new StringVector();
					for (int a = 0; a < this.annotations.length; a++)
						attributeNameCollector.addContentIgnoreDuplicates(this.annotations[a].getAttributeNames());
					attributeNameCollector.sortLexicographically(false, false);
					this.attributeSelector.removeAllItems();
					this.attributeSelector.addItem(ALL_ATTRIBUTES_ATTRIBUTE);
					for (int i = 0; i < attributeNameCollector.size(); i++)
						this.attributeSelector.addItem(attributeNameCollector.get(i));
					
					JPanel selectorPanel = new JPanel(new GridBagLayout());
					GridBagConstraints gbc = new GridBagConstraints();
					gbc.weighty = 1;
					gbc.gridwidth = 1;
					gbc.gridheight = 1;
					gbc.insets.top = 5;
					gbc.insets.left = 5;
					gbc.insets.right = 5;
					gbc.insets.bottom = 5;
					gbc.fill = GridBagConstraints.BOTH;
					
					gbc.gridy = 0;
					gbc.gridx = 0;
					gbc.weightx = 0;
					selectorPanel.add(new JLabel("Attribute Name"), gbc.clone());
					gbc.gridx = 1;
					gbc.weightx = 1;
					selectorPanel.add(this.attributeSelector, gbc.clone());
					
					//	initialize main buttons
					JButton commitButton = new JButton("Remove");
					commitButton.setBorder(BorderFactory.createRaisedBevelBorder());
					commitButton.setPreferredSize(new Dimension(100, 21));
					commitButton.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							removeAttribute();
						}
					});
					
					JButton abortButton = new JButton("Cancel");
					abortButton.setBorder(BorderFactory.createRaisedBevelBorder());
					abortButton.setPreferredSize(new Dimension(100, 21));
					abortButton.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							RemoveAttributeDialog.this.dispose();
						}
					});
					
					JPanel mainButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
					mainButtonPanel.add(commitButton);
					mainButtonPanel.add(abortButton);
					
					//	put the whole stuff together
					this.setLayout(new BorderLayout());
					this.add(selectorPanel, BorderLayout.CENTER);
					this.add(mainButtonPanel, BorderLayout.SOUTH);
					
					this.setResizable(true);
					this.setSize(new Dimension(400, 100));
					this.setLocationRelativeTo(AnnotationNavigatorDialog.this);
				}
				
				boolean isModified() {
					return this.isModified;
				}
				
				void removeAttribute() {
					Object attributeObject = this.attributeSelector.getSelectedItem();
					String attribute = (((attributeObject == null) || ALL_ATTRIBUTES_ATTRIBUTE.equals(attributeObject)) ? null : attributeObject.toString());
					this.isModified = AnnotationTools.removeAnnotationAttribute(this.annotations, attribute);
					this.dispose();
				}
			}
	
			private class AnnotationTray {
				MutableAnnotation annotation;
				boolean isMatch = false;
				AnnotationTray(MutableAnnotation annotation) {
					this.annotation = annotation;
				}
			}
		
			private class AnnotationNavigatorTableModel implements TableModel {
				private AnnotationTray[] annotations;
				private boolean isMatchesOnly = true;
				AnnotationNavigatorTableModel(AnnotationTray[] annotations) {
					this.annotations = annotations;
					for (int a = 0; a < this.annotations.length; a++)
						this.isMatchesOnly = (this.isMatchesOnly && this.annotations[a].isMatch);
				}
				
				/*
				 * @see javax.swing.table.TableModel#addTableModelListener(javax.swing.event.TableModelListener)
				 */
				public void addTableModelListener(TableModelListener l) {}
				
				/*
				 * @see javax.swing.table.TableModel#getColumnClass(int)
				 */
				public Class getColumnClass(int columnIndex) {
					return String.class;
				}
				
				/*
				 * @see javax.swing.table.TableModel#getColumnCount()
				 */
				public int getColumnCount() {
					return 4;
				}
				
				/*
				 * @see javax.swing.table.TableModel#getColumnName(int)
				 */
				public String getColumnName(int columnIndex) {
					String sortExtension = ((columnIndex == sortColumn) ? (sortDescending ? " (d)" : " (a)") : "");
					if (columnIndex == 0) return ("Type" + sortExtension);
					if (columnIndex == 1) return ("Start" + sortExtension);
					if (columnIndex == 2) return ("Size" + sortExtension);
					if (columnIndex == 3) return ("Value" + sortExtension);
					return null;
				}
				
				/*
				 * @see javax.swing.table.TableModel#getRowCount()
				 */
				public int getRowCount() {
					return this.annotations.length;
				}
				
				/*
				 * @see javax.swing.table.TableModel#getValueAt(int, int)
				 */
				public Object getValueAt(int rowIndex, int columnIndex) {
					Annotation annotation = this.annotations[rowIndex].annotation;
					if (this.isMatchesOnly || !this.annotations[rowIndex].isMatch) {
						if (columnIndex == 0) return annotation.getType();
						if (columnIndex == 1) return "" + annotation.getStartIndex();
						if (columnIndex == 2) return "" + annotation.size();
						if (columnIndex == 3) return annotation.getValue();
						return null;
					}
					else {
						String value = null;
						if (columnIndex == 0) value = annotation.getType();
						if (columnIndex == 1) value = "" + annotation.getStartIndex();
						if (columnIndex == 2) value = "" + annotation.size();
						if (columnIndex == 3) value = annotation.getValue();
						return ((value == null) ? null : ("<HTML><B>" + value + "</B></HTML>"));
					}
				}
				
				/*
				 * @see javax.swing.table.TableModel#isCellEditable(int, int)
				 */
				public boolean isCellEditable(int rowIndex, int columnIndex) {
					return false;
				}
				
				/*
				 * @see javax.swing.table.TableModel#removeTableModelListener(javax.swing.event.TableModelListener)
				 */
				public void removeTableModelListener(TableModelListener l) {}
				
				/*
				 * @see javax.swing.table.TableModel#setValueAt(java.lang.Object, int, int)
				 */
				public void setValueAt(Object newValue, int rowIndex, int columnIndex) {}
			}
		
			private class TooltipAwareComponentRenderer extends DefaultTableCellRenderer {
				private HashSet tooltipColumns = new HashSet();
				private Tokenizer tokenizer;
				TooltipAwareComponentRenderer(int tooltipColumn, Tokenizer tokenizer) {
					this.tooltipColumns.add("" + tooltipColumn);
					this.tokenizer = tokenizer;
				}
				public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
					JComponent component = ((value instanceof JComponent) ? ((JComponent) value) : (JComponent) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column));
					if (this.tooltipColumns.contains("" + row) && (component instanceof JComponent))
						((JComponent) component).setToolTipText(this.produceTooltipText(new PlainTokenSequence(value.toString(), this.tokenizer)));
					return component;
				}
				private String produceTooltipText(TokenSequence tokens) {
					if (tokens.size() < 100) return TokenSequenceUtils.concatTokens(tokens);
					
					StringVector lines = new StringVector();
					int startToken = 0;
					int lineLength = 0;
					Token lastToken = null;
					
					for (int t = 0; t < tokens.size(); t++) {
						Token token = tokens.tokenAt(t);
						lineLength += token.length();
						if (lineLength > 100) {
							lines.addElement(TokenSequenceUtils.concatTokens(tokens, startToken, (t - startToken + 1)));
							startToken = (t + 1);
							lineLength = 0;
						} else if (Gamta.insertSpace(lastToken, token)) lineLength++;
					}
					if (startToken < tokens.size())
						lines.addElement(TokenSequenceUtils.concatTokens(tokens, startToken, (tokens.size() - startToken)));
					
					return ("<HTML>" + lines.concatStrings("<BR>") + "</HTML>");
				}
			}
		}
	}
}
