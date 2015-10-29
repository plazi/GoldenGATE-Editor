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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
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
import de.uka.ipd.idaho.gamta.util.constants.LiteratureConstants;
import de.uka.ipd.idaho.gamta.util.gPath.GPath;
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
 * can be selected for removal or deletion, and annotations of the same value
 * can be selected in one action, regardless of their actual number. This view
 * is helpful for sorting out false positives after a document has automatically
 * been annotated by an NLP component, for instance, and for locating and
 * deleting artifacts in a document text.<br>
 * Invoking the SelectAnnotationsViewer with one of the showDocument() methods
 * that takes a Properties object as an argument allows for programmatically
 * specifying the AnnotationFilter used for determining which Annotations to
 * list for selection. In particular, there are three parameters, any one of
 * which can be used:
 * <ul>
 * <li><b>annotationFilterType</b>: a specific annotation type; all
 * annotations of this type will be shown for selection</li>
 * <li><b>annotationFilterPath</b>: a GPath expression; all annotations
 * selected by the GPath expression will be shown for selection</li>
 * <li><b>annotationFilterName</b>: the fully qualified name of an
 * AnnotationFilter, i.e. &lt;filterName&gt;@&lt;filterProviderClassName&gt;,
 * with &lt;filterName&gt; being the provider-internal name of the
 * AnnotationFilter, and &lt;filterProviderClassName&gt; being the class name of
 * respective the AnnotationFilterProvider; all annotations matching the filter
 * will be shown for selection</li>
 * </ul>
 * If none of these parameters is specified, the user will be prompted to select
 * a filter manually, e.g. if the SelectAnnotationsViewer is invoked through
 * GoldenGATE's View menu.
 * 
 * @author sautter
 */
public class SelectAnnotationsViewer extends AbstractDocumentViewer implements LiteratureConstants {
	
	/** the parameter to use for specifying a specific annotation type to show for selection */
	public static final String ANNOTATION_FILTER_TYPE_PARAMETER = "annotationFilterType";
	
	/** the parameter to use for specifying a specific GPath expression whose matches to show for selection */
	public static final String ANNOTATION_FILTER_PATH_PARAMETER = "annotationFilterPath";
	
	/** the parameter to use for specifying the name and provider name of an AnnotationFilter whose matches to show for selection */
	public static final String ANNOTATION_FILTER_NAME_PARAMETER = "annotationFilterName";
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentViewer#getViewMenuName()
	 */
	public String getViewMenuName() {
		return "Select Annotations";
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractDocumentViewer#showDocument(de.uka.ipd.idaho.gamta.MutableAnnotation, de.uka.ipd.idaho.goldenGate.DocumentEditor, java.util.Properties)
	 */
	protected void showDocument(MutableAnnotation doc, DocumentEditor editor, Properties parameters) {
		AnnotationFilter filter = this.getFilter(parameters);
		
		if (filter == null) {
			AnnotationSelectorParameterDialog sad = new AnnotationSelectorParameterDialog(editor.getAnnotationFilters());
			sad.setLocationRelativeTo(DialogPanel.getTopWindow());
			sad.setVisible(true);
			if (sad.wasCommitted())
				filter = sad.getSelectedFilter();
		}
		
		if (filter != null) {
			AnnotationSelectorDialog asd = new AnnotationSelectorDialog("Select Annotations", filter, doc, editor);
			asd.setLocationRelativeTo(DialogPanel.getTopWindow());
			asd.setVisible(true);
		}
	}
	
	private AnnotationFilter getFilter(Properties parameters) {
		
		//	check filter type parameter
		final String filterType = parameters.getProperty(ANNOTATION_FILTER_TYPE_PARAMETER);
		if (filterType != null) {
			System.out.println("SelectAnnotationsViewer: filter type is " + filterType);
			return new AnnotationFilter() {
				public boolean accept(Annotation annotation) {
					return filterType.equals(annotation.getType());
				}
				public QueriableAnnotation[] getMatches(QueriableAnnotation data) {
					return data.getAnnotations(filterType);
				}
				public MutableAnnotation[] getMutableMatches(MutableAnnotation data) {
					return data.getMutableAnnotations(filterType);
				}
				public String getName() {
					return filterType;
				}
				public String getProviderClassName() {
					return null;
				}
				public String getTypeLabel() {
					return "AnnotationTypeFilter";
				}
			};
		}
		
		//	check GPath filter parameter
		final String filterPath = parameters.getProperty(ANNOTATION_FILTER_PATH_PARAMETER);
		if (filterPath != null) try {
			System.out.println("SelectAnnotationsViewer: filter path is " + filterPath);
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
		if (filterName != null) {
			System.out.println("SelectAnnotationsViewer: filter name is " + filterName);
			return this.parent.getAnnotationFilterForName(filterName);
		}
		
		//	filter not found
		return null;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractDocumentViewer#getDocumentViewParameterPanel(de.uka.ipd.idaho.easyIO.settings.Settings)
	 */
	public DocumentViewParameterPanel getDocumentViewParameterPanel(Settings settings) {
		return new AnnotationSelectorParameterPanel(settings);
	}
	
	private class AnnotationSelectorParameterPanel extends DocumentViewParameterPanel {
		
		private JRadioButton useTypeFilter = new JRadioButton("Use Annotation Type Filter");
		private JComboBox filterTypeSelector;
		private JRadioButton usePathFilter = new JRadioButton("Use GPath Filter");
		private JTextField filterPathField = new JTextField();
		private JRadioButton useFilter = new JRadioButton("Use Annotation Filter");
		private JComboBox filterSelector;
		
		private boolean dirty = false;
		
		AnnotationSelectorParameterPanel(Settings settings) {
			super(new GridLayout(3,2));
			
			//	interconnect buttons
			ButtonGroup bg = new ButtonGroup();
			bg.add(this.useTypeFilter);
			bg.add(this.usePathFilter);
			bg.add(this.useFilter);
			
			//	listen to mode selection changes
			this.useTypeFilter.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent ie) {
					filterModeChanged();
				}
			});
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
			
			//	initialize filter type selector
			StringVector typeCollector = new StringVector();
			typeCollector.addContentIgnoreDuplicates(DocumentEditor.getActiveAnnotationTypes());
			typeCollector.addContentIgnoreDuplicates(DocumentEditor.getAnnotationTypeSuggestions());
			String[] types = typeCollector.toStringArray();
			Arrays.sort(types, ANNOTATION_TYPE_ORDER);
			this.filterTypeSelector = new JComboBox(types);
			this.filterTypeSelector.setEditable(true);
			this.filterTypeSelector.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent ie) {
					dirty = true;
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
				this.useTypeFilter.setSelected(true);
			
			else if (settings.containsKey(ANNOTATION_FILTER_TYPE_PARAMETER)) {
				this.useTypeFilter.setSelected(true);
				this.filterTypeSelector.setSelectedItem(settings.getSetting(ANNOTATION_FILTER_TYPE_PARAMETER));
			}
			else if (settings.containsKey(ANNOTATION_FILTER_PATH_PARAMETER)) {
				this.usePathFilter.setSelected(true);
				this.filterPathField.setText(settings.getSetting(ANNOTATION_FILTER_PATH_PARAMETER));
			}
			else if (settings.containsKey(ANNOTATION_FILTER_NAME_PARAMETER)) {
				AnnotationFilter af = parent.getAnnotationFilterForName(settings.getSetting(ANNOTATION_FILTER_NAME_PARAMETER));
				if (af == null)
					this.useTypeFilter.setSelected(true);
				else {
					this.useFilter.setSelected(true);
					this.filterSelector.setSelectedItem(new SelectableAnnotationFilter(af));
				}
			}
			else this.useTypeFilter.setSelected(true);
			
			//	changes count from now on
			this.dirty = false;
			
			//	put the whole stuff together
			this.add(this.useTypeFilter);
			this.add(this.filterTypeSelector);
			this.add(this.usePathFilter);
			this.add(this.filterPathField);
			this.add(this.useFilter);
			this.add(this.filterSelector);
		}
		
		private void filterModeChanged() {
			this.filterTypeSelector.setEnabled(this.useTypeFilter.isSelected());
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
			if (this.useTypeFilter.isSelected()) {
				Object filterTypeObject = this.filterTypeSelector.getSelectedItem();
				if (filterTypeObject != null)
					set.setSetting(ANNOTATION_FILTER_TYPE_PARAMETER, filterTypeObject.toString());
			}
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
	
	private class AnnotationSelectorParameterDialog extends DialogPanel {
//		private JComboBox selector = new JComboBox();
//		private boolean committed = false;
		AnnotationFilter filter = null;
		
		AnnotationSelectorParameterDialog(AnnotationFilter[] filters) {
			super("Display Annotations for Selection", true);
			
//			this.selector = new JComboBox(filters);
//			this.selector.setBorder(BorderFactory.createLoweredBevelBorder());
//			this.selector.setEditable(false);
//			
//			JPanel selectorPanel = new JPanel(new GridBagLayout());
//			GridBagConstraints gbc = new GridBagConstraints();
//			gbc.insets.top = 2;
//			gbc.insets.bottom = 2;
//			gbc.insets.left = 3;
//			gbc.insets.right = 3;
//			gbc.weighty = 0;
//			gbc.weightx = 1;
//			gbc.gridheight = 1;
//			gbc.gridwidth = 1;
//			gbc.fill = GridBagConstraints.BOTH;
//			gbc.gridy = 0;
//			
//			gbc.gridx = 0;
//			selectorPanel.add(new JLabel("Annotation Type"), gbc.clone());
//			gbc.gridx = 1;
//			selectorPanel.add(this.selector, gbc.clone());	
			
			final AnnotationSelectorParameterPanel parameterPanel = new AnnotationSelectorParameterPanel(null);
			
			JButton commitButton = new JButton("Select Annotations");
			commitButton.setPreferredSize(new Dimension(100, 21));
			commitButton.setBorder(BorderFactory.createRaisedBevelBorder());
			commitButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
//					committed = true;
					filter = getFilter(parameterPanel.getSettings().toProperties());
					dispose();
				}
			});
			JButton abortButton = new JButton("Cancel");
			abortButton.setPreferredSize(new Dimension(100, 21));
			abortButton.setBorder(BorderFactory.createRaisedBevelBorder());
			abortButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					dispose();
				}
			});
			
			JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER), true);
			buttonPanel.add(commitButton);
			buttonPanel.add(abortButton);
			
			this.setLayout(new BorderLayout());
//			this.add(selectorPanel, BorderLayout.CENTER);
			this.add(parameterPanel, BorderLayout.CENTER);
			this.add(buttonPanel, BorderLayout.SOUTH);
			
//			this.setSize(new Dimension(400, 85));
			this.setSize(400, 135);
			this.setResizable(true);
			this.setLocationRelativeTo(this.getOwner());
		}
		
		boolean wasCommitted() {
//			return this.committed;
			return (this.filter != null);
		}
		
		AnnotationFilter getSelectedFilter() {
//			Object o = this.selector.getSelectedItem();
//			if ((o != null) && (o instanceof AnnotationFilter)) return ((AnnotationFilter) o);
//			else return null;
			return this.filter;
		}
	}
	
	private class AnnotationSelectorDialog extends DialogPanel {
		
		private AnnotationSelectorPanel annotationDisplay;
		private JLabel statusLabel = new JLabel(" ");
		
		private String originalTitle;
		private MutableAnnotation data;
		
		AnnotationSelectorDialog(String title, AnnotationFilter filter, MutableAnnotation data, DocumentEditor target) {
			super(title, true);
			this.originalTitle = title;
			this.data = data;
			
			JPanel buttonPanel = new JPanel();
			buttonPanel.setLayout(new FlowLayout());
			
			//	initialize main buttons
			JButton writeButton = new JButton("Write & Refresh");
			writeButton.setBorder(BorderFactory.createRaisedBevelBorder());
			writeButton.setPreferredSize(new Dimension(100, 21));
			writeButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					writeChanges();
					annotationDisplay.refreshAnnotations();
				}
			});
			buttonPanel.add(writeButton);
			
			JButton commitButton = new JButton("Write & Close");
			commitButton.setBorder(BorderFactory.createRaisedBevelBorder());
			commitButton.setPreferredSize(new Dimension(100, 21));
			commitButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					writeChanges();
					AnnotationSelectorDialog.this.dispose();
				}
			});
			buttonPanel.add(commitButton);
			
			JButton abortButton = new JButton("Close");
			abortButton.setBorder(BorderFactory.createRaisedBevelBorder());
			abortButton.setPreferredSize(new Dimension(100, 21));
			abortButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					AnnotationSelectorDialog.this.dispose();
				}
			});
			buttonPanel.add(abortButton);
			
			this.statusLabel.setBorder(BorderFactory.createLoweredBevelBorder());
			
			JPanel functionPanel = new JPanel(new BorderLayout());
			functionPanel.add(buttonPanel, BorderLayout.CENTER);
			functionPanel.add(this.statusLabel, BorderLayout.SOUTH);
			
			//	put the whole stuff together
			this.setLayout(new BorderLayout());
			
			this.annotationDisplay = new AnnotationSelectorPanel(filter, data, target);
			this.add(this.annotationDisplay, BorderLayout.CENTER);
			this.add(functionPanel, BorderLayout.SOUTH);
			
			this.setResizable(true);
			this.setSize(new Dimension(500, Math.min(Math.max(200, (150 + (25 * this.annotationDisplay.annotationTraysByID.size()))), 500)));
		}
		
		void writeChanges() {
			AnnotationTray[] aTrays = this.annotationDisplay.getAnnotationTrays();
			ArrayList delete = new ArrayList();
			for (int a = 0; a < aTrays.length; a++) {
				if (aTrays[a].delete) delete.add(aTrays[a].annotation);
				else if (aTrays[a].remove) this.data.removeAnnotation(aTrays[a].annotation);
			}
			AnnotationTools.deleteAnnotations(data, ((Annotation[]) delete.toArray(new Annotation[delete.size()])));
		}
		
		private class AnnotationTray {
			MutableAnnotation annotation;
			boolean remove = false;
			boolean delete = false;
			ArrayList subTrays = new ArrayList();
			AnnotationTray(MutableAnnotation annotation) {
				this.annotation = annotation;
			}
		}
	
		/**
		 * the panel doing the actual displaying
		 * 
		 * @author sautter
		 */
		private class AnnotationSelectorPanel extends JPanel {
			
			private Dimension editDialogSize = new Dimension(800, 600);
			private Point editDialogLocation = null;
			
			private String[] taggedTypes = {};
			private String[] highlightTypes = {};
			
			private Dimension attributeDialogSize = new Dimension(400, 300);
			private Point attributeDialogLocation = null;
			
			private JTable annotationTable;
			
			private AnnotationTray[] annotationTrays;
			private HashMap annotationTraysByID = new HashMap();
			private AnnotationTray[] allAnnotationTrays;
			
			private MutableAnnotation data;
			private DocumentEditor target;
			private AnnotationFilter filter;
			
			private JCheckBox distinctValues = new JCheckBox("Distinct Values", false);
			
			private int sortColumn = -1;
			private boolean sortDescending = false;
			
			AnnotationSelectorPanel(AnnotationFilter filter, MutableAnnotation data, DocumentEditor target) {
				super(new BorderLayout(), true);
				this.setBorder(BorderFactory.createEtchedBorder());
				
				this.filter = filter;
				this.data = data;
				this.target = target;
				
				//	read base layout settings
				this.taggedTypes = ((this.target == null) ? new String[0] : this.target.getTaggedAnnotationTypes());
				this.highlightTypes = ((this.target == null) ? new String[0] : this.target.getHighlightAnnotationTypes());
				
				//	initialize distinct mode
				this.distinctValues.addItemListener(new ItemListener() {
					public void itemStateChanged(ItemEvent ie) {
						arrangeAnnotations();
					}
				});
				this.add(this.distinctValues, BorderLayout.NORTH);
				
				//	initialize display
				this.annotationTable = new JTable();
				this.annotationTable.setDefaultRenderer(Object.class, new TooltipAwareComponentRenderer(5, data.getTokenizer()));
				this.annotationTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
				this.annotationTable.addMouseListener(new MouseAdapter() {
					public void mouseClicked(MouseEvent me) {
						if (me.getButton() != MouseEvent.BUTTON1) {
							int clickRowIndex = annotationTable.rowAtPoint(me.getPoint());
							int rowIndex = annotationTable.getSelectedRow();
							if ((clickRowIndex != rowIndex) && (clickRowIndex != -1)) {
								ListSelectionModel lsm = annotationTable.getSelectionModel();
								if (lsm != null) lsm.setSelectionInterval(clickRowIndex, clickRowIndex);
							}
							showContextMenu(me);
						}
						else if (me.getClickCount() > 1)
							markSelectedValueForRemove();
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
				this.add(annotationTableBox, BorderLayout.CENTER);
				
				JButton removeAllButton = new JButton("Remove All");
				removeAllButton.setBorder(BorderFactory.createRaisedBevelBorder());
				removeAllButton.setPreferredSize(new Dimension(100, 21));
				removeAllButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						markAllForRemove();
					}
				});
				
				JButton removeSelectedValueButton = new JButton("Remove Selected Value");
				removeSelectedValueButton.setBorder(BorderFactory.createRaisedBevelBorder());
				removeSelectedValueButton.setPreferredSize(new Dimension(100, 21));
				removeSelectedValueButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						markSelectedValueForRemove();
					}
				});
				
				JButton dontRemoveSelectedValueButton = new JButton("Don't Remove Selected Value");
				dontRemoveSelectedValueButton.setBorder(BorderFactory.createRaisedBevelBorder());
				dontRemoveSelectedValueButton.setPreferredSize(new Dimension(100, 21));
				dontRemoveSelectedValueButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						unmarkSelectedValueForRemove();
					}
				});
				
				JButton removeNoneButton = new JButton("Remove None");
				removeNoneButton.setBorder(BorderFactory.createRaisedBevelBorder());
				removeNoneButton.setPreferredSize(new Dimension(100, 21));
				removeNoneButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						unmarkAllForRemove();
					}
				});
				
				JButton deleteAllButton = new JButton("Delete All");
				deleteAllButton.setBorder(BorderFactory.createRaisedBevelBorder());
				deleteAllButton.setPreferredSize(new Dimension(100, 21));
				deleteAllButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						markAllForDelete();
					}
				});
				
				JButton deleteSelectedValueButton = new JButton("Delete Selected Value");
				deleteSelectedValueButton.setBorder(BorderFactory.createRaisedBevelBorder());
				deleteSelectedValueButton.setPreferredSize(new Dimension(100, 21));
				deleteSelectedValueButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						markSelectedValueForDelete();
					}
				});
				
				JButton dontDeleteSelectedValueButton = new JButton("Don't Delete Selected Value");
				dontDeleteSelectedValueButton.setBorder(BorderFactory.createRaisedBevelBorder());
				dontDeleteSelectedValueButton.setPreferredSize(new Dimension(100, 21));
				dontDeleteSelectedValueButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						unmarkSelectedValueForDelete();
					}
				});
				
				JButton deleteNoneButton = new JButton("Delete None");
				deleteNoneButton.setBorder(BorderFactory.createRaisedBevelBorder());
				deleteNoneButton.setPreferredSize(new Dimension(100, 21));
				deleteNoneButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						unmarkAllForDelete();
					}
				});
				
				JPanel buttonPanel = new JPanel(new GridBagLayout());
				GridBagConstraints gbc = new GridBagConstraints();
				gbc.insets.top = 2;
				gbc.insets.bottom = 2;
				gbc.insets.left = 5;
				gbc.insets.right = 5;
				gbc.weighty = 0;
				gbc.weightx = 1;
				gbc.gridheight = 1;
				gbc.gridwidth = 1;
				gbc.fill = GridBagConstraints.HORIZONTAL;
				
				gbc.gridy = 0;
				gbc.gridx = 0;
				buttonPanel.add(removeAllButton, gbc.clone());
				gbc.gridx++;
				buttonPanel.add(removeSelectedValueButton, gbc.clone());
				gbc.gridx++;
				buttonPanel.add(dontRemoveSelectedValueButton, gbc.clone());
				gbc.gridx++;
				buttonPanel.add(removeNoneButton, gbc.clone());
				
				gbc.gridy = 1;
				gbc.gridx = 0;
				buttonPanel.add(deleteAllButton, gbc.clone());
				gbc.gridx++;
				buttonPanel.add(deleteSelectedValueButton, gbc.clone());
				gbc.gridx++;
				buttonPanel.add(dontDeleteSelectedValueButton, gbc.clone());
				gbc.gridx++;
				buttonPanel.add(deleteNoneButton, gbc.clone());
				
				this.add(buttonPanel, BorderLayout.SOUTH);
			}
			
			void refreshAnnotations() {
				MutableAnnotation[] annotations = this.filter.getMutableMatches(this.data);
				this.allAnnotationTrays = new AnnotationTray[annotations.length];
				for (int a = 0; a < annotations.length; a++) {
					if (this.annotationTraysByID.containsKey(annotations[a].getAnnotationID()))
						this.allAnnotationTrays[a] = ((AnnotationTray) this.annotationTraysByID.get(annotations[a].getAnnotationID()));
					else {
						this.allAnnotationTrays[a] = new AnnotationTray(annotations[a]);
						this.annotationTraysByID.put(annotations[a].getAnnotationID(), this.allAnnotationTrays[a]);
					}
				}
				this.arrangeAnnotations();
			}
			
			void arrangeAnnotations() {
				if (this.distinctValues.isSelected() && (this.allAnnotationTrays.length != 0)) {
					Arrays.sort(this.allAnnotationTrays, new Comparator() {
						public int compare(Object o1, Object o2) {
							AnnotationTray at1 = ((AnnotationTray) o1);
							AnnotationTray at2 = ((AnnotationTray) o2);
							int c = String.CASE_INSENSITIVE_ORDER.compare(at1.annotation.getValue(), at2.annotation.getValue());
							return ((c == 0) ? AnnotationUtils.compare(at1.annotation, at2.annotation) : c);
						}
					});
					ArrayList annotationTrays = new ArrayList();
					AnnotationTray lastTray;
					lastTray = this.allAnnotationTrays[0];
					lastTray.subTrays.clear();
					annotationTrays.add(lastTray);
					for (int a = 1; a < this.allAnnotationTrays.length; a++) {
						if (lastTray.annotation.getValue().equals(this.allAnnotationTrays[a].annotation.getValue()))
							lastTray.subTrays.add(this.allAnnotationTrays[a]);
						else {
							lastTray = this.allAnnotationTrays[a];
							lastTray.subTrays.clear();
							annotationTrays.add(lastTray);
						}
					}
					this.annotationTrays = ((AnnotationTray[]) annotationTrays.toArray(new AnnotationTray[annotationTrays.size()]));
					Arrays.sort(this.annotationTrays, new Comparator() {
						public int compare(Object o1, Object o2) {
							AnnotationTray at1 = ((AnnotationTray) o1);
							AnnotationTray at2 = ((AnnotationTray) o2);
							return AnnotationUtils.compare(at1.annotation, at2.annotation);
						}
					});
					setTitle(originalTitle + " - " + this.allAnnotationTrays.length + " Annotations, " + this.annotationTrays.length + " Distinct Values");
				}
				else {
					this.annotationTrays = this.allAnnotationTrays;
					setTitle(originalTitle + " - " + this.allAnnotationTrays.length + " Annotations");
				}
				
				this.annotationTable.setModel(new AnnotationSelectionTableModel(this.annotationTrays, this.data, 3, this.distinctValues.isSelected()));
				
				this.annotationTable.getColumnModel().getColumn(0).setMaxWidth(60);
				this.annotationTable.getColumnModel().getColumn(1).setMaxWidth(60);
				this.annotationTable.getColumnModel().getColumn(2).setMaxWidth(120);
				this.annotationTable.getColumnModel().getColumn(3).setMaxWidth(60);
				this.annotationTable.getColumnModel().getColumn(4).setMaxWidth(60);
				if (this.annotationTable.getModel().getColumnCount() > 6)
					this.annotationTable.getColumnModel().getColumn(6).setMaxWidth(60);
				
				this.refreshDisplay();
			}
			
			void sortAnnotations() {
//				System.out.println("Sorting annotations by " + this.sortColumn + " " + (this.sortDescending ? "d" : "a"));
				Arrays.sort(this.annotationTrays, new Comparator() {
					public int compare(Object o1, Object o2) {
						AnnotationTray at1 = ((AnnotationTray) o1);
						AnnotationTray at2 = ((AnnotationTray) o2);
						int c;
						if (sortColumn == 2)
							c = at1.annotation.getType().compareToIgnoreCase(at2.annotation.getType());
						else if (sortColumn == 3)
							c = (at1.annotation.getStartIndex() - at2.annotation.getStartIndex());
						else if (sortColumn == 4)
							c = (at1.annotation.size() - at2.annotation.size());
						else if (sortColumn == 5)
							c = String.CASE_INSENSITIVE_ORDER.compare(at1.annotation.getValue(), at2.annotation.getValue());
						else if (sortColumn == 6)
							c = (at1.subTrays.size() - at2.subTrays.size());
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
				AnnotationSelectorDialog.this.validate();
			}
			
			void editAnnotation(MutableAnnotation annotation) {
				
				//	create dialog & show
				DocumentEditDialog ded = new DocumentEditDialog(AnnotationSelectorDialog.this.getDialog(), SelectAnnotationsViewer.this.parent, target, "Edit Annotation", annotation);
				ded.setLocationRelativeTo(AnnotationSelectorPanel.this);
				ded.setVisible(true);
				
				//	finish
				if (ded.committed && ded.isContentModified()) this.refreshAnnotations();
			}
			
			private class DocumentEditDialog extends DocumentEditorDialog {
				private boolean committed = false;
				DocumentEditDialog(JDialog owner, GoldenGATE host, DocumentEditor parent, String title, MutableAnnotation docPart) {
					super(owner, host, parent, title, docPart);
					
					JButton okButton = new JButton("OK");
					okButton.setBorder(BorderFactory.createRaisedBevelBorder());
					okButton.setPreferredSize(new Dimension(100, 21));
					okButton.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
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
						public void actionPerformed(ActionEvent e) {
							DocumentEditDialog.this.dispose();
						}
					});
					this.mainButtonPanel.add(cancelButton);
					
					for (int t = 0; t < taggedTypes.length; t++)
						this.documentEditor.setAnnotationTagVisible(taggedTypes[t], true);
					for (int t = 0; t < highlightTypes.length; t++)
						this.documentEditor.setAnnotationValueHighlightVisible(highlightTypes[t], true);
					
					this.setSize(editDialogSize);
					if (editDialogLocation == null) this.setLocationRelativeTo((parent == null) ? ((Component) this.getOwner()) : ((Component) parent));
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
				AttributeEditorDialog aed = new AttributeEditorDialog(AnnotationSelectorDialog.this.getDialog(), "Edit Annotation Attributes", annotation, data) {
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
			
			void showContext(AnnotationTray tray) {
				Object pageNumber = tray.annotation.getAttribute(PAGE_NUMBER_ATTRIBUTE);
				String message = ("<HTML>" + buildLabel(data, tray.annotation, 10) + " (at " + tray.annotation.getStartIndex() + ((pageNumber == null) ? "" : (" on page " + pageNumber)) + ")" + "</HTML>");
				JOptionPane.showMessageDialog(this, message, ("Context of \"" + buildValue(tray.annotation, 10) + "\""), JOptionPane.INFORMATION_MESSAGE);
			}
			
			void showValueContext(AnnotationTray tray) {
				StringVector messageLines = new StringVector();
				if (this.distinctValues.isSelected()) {
					Object pageNumber = tray.annotation.getAttribute(PAGE_NUMBER_ATTRIBUTE);
					messageLines.addElement(buildLabel(data, tray.annotation, 10) + " (at " + tray.annotation.getStartIndex() + ((pageNumber == null) ? "" : (" on page " + pageNumber)) + ")");
					for (int t = 0; t < tray.subTrays.size(); t++) {
						AnnotationTray subTray = ((AnnotationTray) tray.subTrays.get(t));
						pageNumber = subTray.annotation.getAttribute(PAGE_NUMBER_ATTRIBUTE);
						messageLines.addElement(buildLabel(data, subTray.annotation, 10) + " (at " + subTray.annotation.getStartIndex() + ((pageNumber == null) ? "" : (" on page " + pageNumber)) + ")");
					}
				}
				else {
					String value = tray.annotation.getValue();
					for (int a = 0; a < this.annotationTrays.length; a++)
						if (this.annotationTrays[a].annotation.getValue().equals(value)) {
							Object pageNumber = this.annotationTrays[a].annotation.getAttribute(PAGE_NUMBER_ATTRIBUTE);
							messageLines.addElement(buildLabel(data, this.annotationTrays[a].annotation, 10) + " (at " + this.annotationTrays[a].annotation.getStartIndex() + ((pageNumber == null) ? "" : (" on page " + pageNumber)) + ")");
						}
				}
				String message = ("<HTML>" + messageLines.concatStrings("<BR>") + "</HTML>");
				JOptionPane.showMessageDialog(this, message, ("Context of \"" + buildValue(tray.annotation, 10) + "\""), JOptionPane.INFORMATION_MESSAGE);
			}
			
			String buildLabel(TokenSequence text, Annotation annot, int envSize) {
				int aStart = annot.getStartIndex();
				int aEnd = annot.getEndIndex();
				int start = Math.max(0, (aStart - envSize));
				int end = Math.min(text.size(), (aEnd + envSize));
				StringBuffer sb = new StringBuffer("... ");
				Token lastToken = null;
				Token token = null;
				
				for (int t = start; t < aStart; t++) {
					lastToken = token;
					token = text.tokenAt(t);
					
					//	add spacer
					if ((lastToken != null) && Gamta.insertSpace(lastToken, token)) sb.append(" ");
					
					//	append token
					sb.append(token);
				}
				
				//	add highlighted value
				if ((lastToken != null) && Gamta.insertSpace(lastToken, annot.firstToken())) sb.append(" ");
				sb.append("<B>");
				sb.append(buildValue(annot, envSize));
				sb.append("</B>");
				lastToken = annot.lastToken();
				
				for (int t = aEnd; t < end; t++) {
					lastToken = token;
					token = text.tokenAt(t);
					
					//	add spacer
					if ((lastToken != null) && Gamta.insertSpace(lastToken, token)) sb.append(" ");
					
					//	append token
					sb.append(token);
				}
				
				return sb.append(" ...").toString();
			}
			
			String buildValue(Annotation annot, int size) {
				if (annot.size() <= size) return annot.getValue();
				
				else {
					StringBuffer sb = new StringBuffer("");
					Token lastToken = null;
					Token token = null;
					
					for (int t = 0; t < (size / 2); t++) {
						lastToken = token;
						token = annot.tokenAt(t);
						
						//	add spacer
						if ((lastToken != null) && Gamta.insertSpace(lastToken, token)) sb.append(" ");
						
						//	append token
						sb.append(token);
					}
					
					sb.append(" ... ");
					lastToken = null;
					
					for (int t = (annot.size() - (size / 2)); t < annot.size(); t++) {
						lastToken = token;
						token = annot.tokenAt(t);
						
						//	add spacer
						if ((lastToken != null) && Gamta.insertSpace(lastToken, token)) sb.append(" ");
						
						//	append token
						sb.append(token);
					}
					
					return sb.toString();
				}
			}
			
			void markAllForRemove() {
				int annotationCount = 0;
				for (int a = 0; a < this.annotationTrays.length; a++) {
					if (!this.annotationTrays[a].remove) annotationCount ++;
					this.annotationTrays[a].remove = true;
					if (this.distinctValues.isSelected())
						for (int t = 0; t < this.annotationTrays[a].subTrays.size(); t++)
							((AnnotationTray) this.annotationTrays[a].subTrays.get(t)).remove = true;
				}
				this.annotationTable.repaint();
				statusLabel.setText("Selected " + annotationCount + " further Annotations for Removal");
				this.validate();
			}
			
			void markSelectedValueForRemove() {
				int si = this.annotationTable.getSelectedRow();
				if (si != -1) {
					if (this.distinctValues.isSelected()) {
						this.annotationTrays[si].remove = true;
						for (int t = 0; t < this.annotationTrays[si].subTrays.size(); t++)
							((AnnotationTray) this.annotationTrays[si].subTrays.get(t)).remove = true;
						this.annotationTable.repaint();
					}
					else {
						String value = this.annotationTrays[si].annotation.getValue();
						int annotationCount = 0;
						for (int a = 0; a < this.annotationTrays.length; a++)
							if (this.annotationTrays[a].annotation.getValue().equals(value)) {
								if (!this.annotationTrays[a].remove) annotationCount ++;
								this.annotationTrays[a].remove = true;
							}
						this.annotationTable.repaint();
						statusLabel.setText("Selected " + annotationCount + " further '" + value + "'-Annotations for Removal");
					}
					this.validate();
				}
			}
			
			void unmarkSelectedValueForRemove() {
				int si = this.annotationTable.getSelectedRow();
				if (si != -1) {
					if (this.distinctValues.isSelected()) {
						this.annotationTrays[si].remove = false;
						this.annotationTrays[si].delete = false;
						for (int t = 0; t < this.annotationTrays[si].subTrays.size(); t++) {
							AnnotationTray subTray = ((AnnotationTray) this.annotationTrays[si].subTrays.get(t));
							subTray.delete = false;
							subTray.remove = false;
						}
						this.annotationTable.repaint();
					}
					else {
						String value = this.annotationTrays[si].annotation.getValue();
						int annotationCount = 0;
						for (int a = 0; a < this.annotationTrays.length; a++)
							if (this.annotationTrays[a].annotation.getValue().equals(value)) {
								if (this.annotationTrays[a].remove) annotationCount ++;
								this.annotationTrays[a].remove = false;
								this.annotationTrays[a].delete = false;
							}
						this.annotationTable.repaint();
						statusLabel.setText("Deselected " + annotationCount + " '" + value + "'-Annotations for Removal");
					}
					this.validate();
				}
			}
			
			void unmarkAllForRemove() {
				for (int a = 0; a < this.annotationTrays.length; a++) {
					this.annotationTrays[a].remove = false;
					this.annotationTrays[a].delete = false;
					if (this.distinctValues.isSelected())
						for (int t = 0; t < this.annotationTrays[a].subTrays.size(); t++) {
							AnnotationTray subTray = ((AnnotationTray) this.annotationTrays[a].subTrays.get(t));
							subTray.delete = false;
							subTray.remove = false;
						}
				}
				this.annotationTable.repaint();
				statusLabel.setText("Deselected all Annotations for Removal");
				this.validate();
			}
			
			void markAllForDelete() {
				int annotationCount = 0;
				for (int a = 0; a < this.annotationTrays.length; a++) {
					this.annotationTrays[a].remove = true;
					if (!this.annotationTrays[a].delete) annotationCount++;
					this.annotationTrays[a].delete = true;
					if (this.distinctValues.isSelected())
						for (int t = 0; t < this.annotationTrays[a].subTrays.size(); t++) {
							AnnotationTray subTray = ((AnnotationTray) this.annotationTrays[a].subTrays.get(t));
							subTray.delete = true;
							subTray.remove = true;
						}
				}
				this.annotationTable.repaint();
				statusLabel.setText("Selected " + annotationCount + " further Annotations for Deletion");
				this.validate();
			}
			
			void markSelectedValueForDelete() {
				int si = this.annotationTable.getSelectedRow();
				if (si != -1) {
					if (this.distinctValues.isSelected()) {
						this.annotationTrays[si].remove = true;
						this.annotationTrays[si].delete = true;
						for (int t = 0; t < this.annotationTrays[si].subTrays.size(); t++) {
							AnnotationTray subTray = ((AnnotationTray) this.annotationTrays[si].subTrays.get(t));
							subTray.delete = true;
							subTray.remove = true;
						}
						this.annotationTable.repaint();
					}
					else {
						String value = this.annotationTrays[si].annotation.getValue();
						int annotationCount = 0;
						for (int a = 0; a < this.annotationTrays.length; a++)
							if (this.annotationTrays[a].annotation.getValue().equals(value)) {
								this.annotationTrays[a].remove = true;
								if (!this.annotationTrays[a].delete) annotationCount++;
								this.annotationTrays[a].delete = true;
							}
						this.annotationTable.repaint();
						statusLabel.setText("Selected " + annotationCount + " further '" + value + "'-Annotations for Deletion");
					}
					this.validate();
				}
			}
			
			void unmarkSelectedValueForDelete() {
				int si = this.annotationTable.getSelectedRow();
				if (si != -1) {
					if (this.distinctValues.isSelected()) {
						this.annotationTrays[si].delete = false;
						for (int t = 0; t < this.annotationTrays[si].subTrays.size(); t++)
							((AnnotationTray) this.annotationTrays[si].subTrays.get(t)).delete = false;
						this.annotationTable.repaint();
					}
					else {
						String value = this.annotationTrays[si].annotation.getValue();
						int annotationCount = 0;
						for (int a = 0; a < this.annotationTrays.length; a++)
							if (this.annotationTrays[a].annotation.getValue().equals(value)) {
								if (this.annotationTrays[a].delete) annotationCount++;
								this.annotationTrays[a].delete = false;
							}
						this.annotationTable.repaint();
						statusLabel.setText("Deselected " + annotationCount + " '" + value + "'-Annotations for Deletion");
					}
					this.validate();
				}
			}
			
			void unmarkAllForDelete() {
				for (int a = 0; a < this.annotationTrays.length; a++) {
					this.annotationTrays[a].delete = false;
					if (this.distinctValues.isSelected())
						for (int t = 0; t < this.annotationTrays[a].subTrays.size(); t++)
							((AnnotationTray) this.annotationTrays[a].subTrays.get(t)).delete = false;
				}
				this.annotationTable.repaint();
				statusLabel.setText("Deselected all Annotations for Deletion");
				this.validate();
			}
			
			AnnotationTray[] getAnnotationTrays() {
				return this.allAnnotationTrays;
			}
			
			void showContextMenu(MouseEvent me) {
				int row = this.annotationTable.getSelectedRow();
				if (row == -1) return;
				
				final AnnotationTray tray = this.annotationTrays[row];
				
				JMenu menu = new JMenu();
				JMenuItem mi;
				
				if (tray.subTrays.isEmpty()) {
					mi = new JMenuItem("Edit");
					mi.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							editAnnotation(tray.annotation);
						}
					});
					menu.add(mi);
					
					mi = new JMenuItem("Edit Attributes");
					mi.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							editAnnotationAttributes(tray.annotation);
						}
					});
					menu.add(mi);
					menu.addSeparator();
				}
				
				if (this.distinctValues.isSelected()) {
					mi = new JMenuItem("Show Wider Context");
					mi.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							showValueContext(tray);
						}
					});
					menu.add(mi);
				}
				else {
					mi = new JMenuItem("Show Wider Context");
					mi.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							showContext(tray);
						}
					});
					menu.add(mi);
					mi = new JMenuItem("Show Context of Value");
					mi.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							showValueContext(tray);
						}
					});
					menu.add(mi);
				}
				menu.addSeparator();
				
				if (tray.remove) {
					mi = new JMenuItem("Don't Remove");
					mi.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							tray.delete = false;
							tray.remove = false;
							for (int t = 0; t < tray.subTrays.size(); t++) {
								AnnotationTray subTray = ((AnnotationTray) tray.subTrays.get(t));
								subTray.delete = false;
								subTray.remove = false;
							}
							annotationTable.repaint();
							validate();
						}
					});
					menu.add(mi);
				}
				else {
					mi = new JMenuItem("Remove");
					mi.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							tray.remove = true;
							for (int t = 0; t < tray.subTrays.size(); t++)
								((AnnotationTray) tray.subTrays.get(t)).remove = true;
							annotationTable.repaint();
							validate();
						}
					});
					menu.add(mi);
				}
				
				mi = new JMenuItem("Remove Value");
				mi.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						markSelectedValueForRemove();
					}
				});
				menu.add(mi);
				
				mi = new JMenuItem("Don't Remove Value");
				mi.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						unmarkSelectedValueForRemove();
					}
				});
				menu.add(mi);
				menu.addSeparator();
				
				mi = new JMenuItem("Rename");
				mi.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						String[] types = ((target == null) ? data.getAnnotationTypes() : target.getAnnotationTypes());
						Arrays.sort(types, ANNOTATION_TYPE_ORDER);
						
						RenameAnnotationDialog rad = new RenameAnnotationDialog(tray.annotation.getType(), types);
						rad.setLocationRelativeTo(AnnotationSelectorPanel.this);
						rad.setVisible(true);
						if (rad.targetType == null) return;
						
						String newType = rad.targetType.trim();
						if ((newType.length() != 0) && !newType.equals(tray.annotation.getType())) {
							tray.annotation.changeTypeTo(newType);
							refreshAnnotations();
						}
					}
				});
				menu.add(mi);
				menu.addSeparator();
				
				if (tray.delete) {
					mi = new JMenuItem("Don't Delete");
					mi.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							tray.delete = false;
							for (int t = 0; t < tray.subTrays.size(); t++)
								((AnnotationTray) tray.subTrays.get(t)).delete = false;
							annotationTable.repaint();
							validate();
						}
					});
					menu.add(mi);
				}
				else {
					mi = new JMenuItem("Delete");
					mi.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							tray.remove = true;
							tray.delete = true;
							for (int t = 0; t < tray.subTrays.size(); t++) {
								AnnotationTray subTray = ((AnnotationTray) tray.subTrays.get(t));
								subTray.delete = true;
								subTray.remove = true;
							}
							annotationTable.repaint();
							validate();
						}
					});
					menu.add(mi);
				}
				
				mi = new JMenuItem("Delete Value");
				mi.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						markSelectedValueForDelete();
					}
				});
				menu.add(mi);
				
				mi = new JMenuItem("Don't Delete Value");
				mi.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						unmarkSelectedValueForDelete();
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
				}
				
				void commit() {
					Object item = targetTypeSelector.getSelectedItem();
					this.targetType = ((item == null) ? "" : item.toString());
					this.dispose();
				}
			}
			
			private class AnnotationSelectionTableModel implements TableModel {
				private AnnotationTray[] annotations;
				private TokenSequence text;
				private int contextSize;
				private boolean distinct;
				AnnotationSelectionTableModel(AnnotationTray[] annotations, TokenSequence text, int contextSize, boolean distinct) {
					this.annotations = annotations;
					this.text = text;
					this.contextSize = contextSize;
					this.distinct = distinct;
				}
				
				/*
				 * @see javax.swing.table.TableModel#addTableModelListener(javax.swing.event.TableModelListener)
				 */
				public void addTableModelListener(TableModelListener l) {}
				
				/*
				 * @see javax.swing.table.TableModel#getColumnClass(int)
				 */
				public Class getColumnClass(int columnIndex) {
					if (columnIndex == 0) return Boolean.class;
					else if (columnIndex == 1) return Boolean.class;
					else return String.class;
				}
				
				/*
				 * @see javax.swing.table.TableModel#getColumnCount()
				 */
				public int getColumnCount() {
					return (this.distinct ? 7 : 6);
				}
				
				/*
				 * @see javax.swing.table.TableModel#getColumnName(int)
				 */
				public String getColumnName(int columnIndex) {
					String sortExtension = ((columnIndex == sortColumn) ? (sortDescending ? " (d)" : " (a)") : "");
					if (columnIndex == 0) return "Remove";
					if (columnIndex == 1) return "Delete";
					if (columnIndex == 2) return ("Type" + sortExtension);
					if (columnIndex == 3) return ("Start" + sortExtension);
					if (columnIndex == 4) return ("Size" + sortExtension);
					if (columnIndex == 5) return ("Value" + sortExtension);
					if (columnIndex == 6) return ("Count" + sortExtension);
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
					if (columnIndex == 0) return new Boolean(this.annotations[rowIndex].remove);
					if (columnIndex == 1) return new Boolean(this.annotations[rowIndex].delete);
					Annotation a = this.annotations[rowIndex].annotation;
					if (columnIndex == 2) return a.getType();
					if (columnIndex == 3) return "" + a.getStartIndex();
					if (columnIndex == 4) return "" + a.size();
					if (columnIndex == 5) {
						if (this.contextSize < 1) return a.getValue();
						
						int aStart = a.getStartIndex();
						int aEnd = a.getEndIndex();
						int start = Math.max(0, (aStart - contextSize));
						int end = Math.min(this.text.size(), (aEnd + contextSize));
						StringBuffer value = new StringBuffer("<HTML>... ");
						Token lastToken = null;
						Token token = null;
						for (int t = start; t < end; t++) {
							lastToken = token;
							token = this.text.tokenAt(t);
							
							//	end highlighting value
							if (t == aEnd) value.append("</B>");
							
							//	add spacer
							if ((lastToken != null) && Gamta.insertSpace(lastToken, token)) value.append(" ");
							
							//	start highlighting value
							if (t == aStart) value.append("<B>");
							
							//	append token
							value.append(token);
						}
						
						return value.append(" ...</HTML>").toString();
					}
					if (columnIndex == 6) return ("" + (1 + this.annotations[rowIndex].subTrays.size()));
					return null;
				}
				
				/*
				 * @see javax.swing.table.TableModel#isCellEditable(int, int)
				 */
				public boolean isCellEditable(int rowIndex, int columnIndex) {
					return (((columnIndex == 0) && !this.annotations[rowIndex].delete) || ((columnIndex == 1) && this.annotations[rowIndex].remove));
				}
				
				/*
				 * @see javax.swing.table.TableModel#removeTableModelListener(javax.swing.event.TableModelListener)
				 */
				public void removeTableModelListener(TableModelListener l) {}
				
				/*
				 * @see javax.swing.table.TableModel#setValueAt(java.lang.Object, int, int)
				 */
				public void setValueAt(Object newValue, int rowIndex, int columnIndex) {
					if (columnIndex == 0) {
						this.annotations[rowIndex].remove = ((Boolean) newValue).booleanValue();
						for (int t = 0; t < this.annotations[rowIndex].subTrays.size(); t++)
							((AnnotationTray) this.annotations[rowIndex].subTrays.get(t)).remove = ((Boolean) newValue).booleanValue();
					}
					if (columnIndex == 1) {
						this.annotations[rowIndex].delete = ((Boolean) newValue).booleanValue();
						for (int t = 0; t < this.annotations[rowIndex].subTrays.size(); t++)
							((AnnotationTray) this.annotations[rowIndex].subTrays.get(t)).delete = ((Boolean) newValue).booleanValue();
					}
				}
			}
		
			private class TooltipAwareComponentRenderer extends DefaultTableCellRenderer {
				private HashSet tooltipColumns = new HashSet();
				private Tokenizer tokenizer;
				TooltipAwareComponentRenderer(int tooltipColumn, Tokenizer tokenizer) {
					this.tooltipColumns.add("" + tooltipColumn);
					this.tokenizer = tokenizer;
				}
//				TooltipAwareComponentRenderer(int[] tooltipColumns) {
//					for (int c = 0; c < tooltipColumns.length; c++)
//						this.tooltipColumns.add("" + tooltipColumns[c]);
//				}
				public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
					JComponent component = ((value instanceof JComponent) ? ((JComponent) value) : (JComponent) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column));
					if (this.tooltipColumns.contains("" + row) && (component instanceof JComponent))
						((JComponent) component).setToolTipText(this.produceTooltipText(new PlainTokenSequence(value.toString(), this.tokenizer)));
					return component;
				}
				String produceTooltipText(TokenSequence tokens) {
					if (tokens.length() < 100) return TokenSequenceUtils.concatTokens(tokens);
					
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
