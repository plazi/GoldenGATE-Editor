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
package de.uka.ipd.idaho.goldenGate.plugin.markupConverters;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.text.JTextComponent;

import de.uka.ipd.idaho.easyIO.settings.Settings;
import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.DocumentRoot;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.util.gPath.GPath;
import de.uka.ipd.idaho.gamta.util.gPath.exceptions.GPathException;
import de.uka.ipd.idaho.gamta.util.swing.AnnotationDisplayDialog;
import de.uka.ipd.idaho.goldenGate.DocumentEditor;
import de.uka.ipd.idaho.goldenGate.GoldenGateConstants;
import de.uka.ipd.idaho.goldenGate.observers.ResourceObserver;
import de.uka.ipd.idaho.goldenGate.plugins.AbstractDocumentProcessorManager;
import de.uka.ipd.idaho.goldenGate.plugins.AnnotationFilter;
import de.uka.ipd.idaho.goldenGate.plugins.AnnotationFilterManager;
import de.uka.ipd.idaho.goldenGate.plugins.DocumentProcessor;
import de.uka.ipd.idaho.goldenGate.plugins.Resource;
import de.uka.ipd.idaho.goldenGate.plugins.ResourceSplashScreen;
import de.uka.ipd.idaho.goldenGate.util.AnnotationTools;
import de.uka.ipd.idaho.goldenGate.util.DataListListener;
import de.uka.ipd.idaho.goldenGate.util.DialogPanel;
import de.uka.ipd.idaho.htmlXmlUtil.accessories.IoTools;
import de.uka.ipd.idaho.stringUtils.StringVector;
import de.uka.ipd.idaho.stringUtils.accessories.StringSelector;

/**
 * Manager for markup converters. A markup converter is essentially a set of
 * mappings from a source annotation type or attribute name to a target type or
 * name. In addition, the source types can be specified in GPath.<br>
 * <br>
 * Running a markup converter applies the mappings in top-down order. The main
 * purpose is renaming, but some special mapping target values also allow for
 * removing or deleting annotations and attributes. Markup converters are
 * especially useful for cleaning up unnecessary markup, e.g. in-line layout filters
 * from an HTML document that is about to be transformed into a semantically
 * annotated XML document.<br>
 * <br>
 * All configuration can be done in the 'Edit Markup Converters' dialog in the
 * GoldenGATE Editor.<br>
 * <br>
 * In addition to providing markup converters as resources, this plug-in also
 * adds a set of function items to the Tools menu, which provide functionality
 * to manipulate markup in an ad-hoc fashion, with the same functionality as
 * offered by markup converters stored as resources.
 * 
 * @see de.uka.ipd.idaho.gamta.util.AnnotationFilter
 * @see de.uka.ipd.idaho.goldenGate.util.AnnotationTools
 * 
 * @author sautter
 */
public class MarkupConverterManager extends AbstractDocumentProcessorManager {
	
	private static final String FILE_EXTENSION = ".markupConverter";
	
	private TreeMap externalFilters = new TreeMap();
	
	public MarkupConverterManager() {}
	
	/* 
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#init()
	 */
	public void init() {
		this.parent.registerResourceObserver(new ResourceObserver() {
			public void resourcesChanged(String resourceProviderClassName) {
				if (parent.getAnnotationFilterProvider(resourceProviderClassName) != null)
					refreshExternalFilters();
			}
		});
		this.refreshExternalFilters();
	}
	
	private void refreshExternalFilters() {
		this.externalFilters.clear();
		
		AnnotationFilterManager[] afms = parent.getAnnotationFilterProviders();
		for (int m = 0; m < afms.length; m++) {
			String afl = afms[m].getResourceTypeLabel();
			String[] afns = afms[m].getResourceNames();
			for (int f = 0; f < afns.length; f++) {
				String name = (afns[f] + "@" + afms[m].getClass().getName());
				String label = ("<" + afl + ": " + afns[f] + ">");
				this.externalFilters.put(label, name);
			}
		}
	}
	
	private McAnnotationFilter getAnnotationFilter(String filterString, boolean reportError) {
		if (filterString == null)
			return null;
		
		//	split attribute name
		int attributeSplit;
		
		//	external filter, attribute must be appended to nice name
		if (filterString.startsWith("<")) 
			attributeSplit = filterString.indexOf(">@");
		
		//	internal filter
		else {
			attributeSplit = filterString.lastIndexOf('@');
			
			//	test if attribute separator '@' belongs to predicate inside GPath
			if (!filterString.substring(attributeSplit + 1).matches("[^\\]\\/\\)]++"))
				attributeSplit = -1;
		}
		
		//	split attribute (if any)
		String attribute = null;
		if (attributeSplit != -1) {
			attribute = filterString.substring(attributeSplit + 1);
			filterString = filterString.substring(0, attributeSplit);
			if (filterString.endsWith("/"))
				filterString = filterString.substring(0, (filterString.length() - 1));
		}
		
		//	get external filter
		if (this.externalFilters.containsKey(filterString)) {
			String fullName = ((String) this.externalFilters.get(filterString));
			String providerClassName = fullName.substring(fullName.indexOf('@') + 1);
			AnnotationFilterManager afm = parent.getAnnotationFilterProvider(providerClassName);
			if (afm != null) {
				String filterName = fullName.substring(0, fullName.indexOf('@'));
				return new McAnnotationFilter(afm.getAnnotationFilter(filterName), attribute);
			}
			else return null;
		}
		
		//	produce type filter (string is not a complex GPath)
		else if (filterString.matches("[^\\[\\]\\/\\(\\)]++")) {
			
			//	handle wildcard types
			if ((filterString.length() == 0) || "*".equals(filterString))
				filterString = null;
			
			final String type = filterString;
			return new McAnnotationFilter(new AnnotationFilter() {
				public boolean accept(Annotation annotation) {
					return ((type == null) || type.equals(annotation.getType()));
				}
				public QueriableAnnotation[] getMatches(QueriableAnnotation data) {
					return data.getAnnotations(type);
				}
				public MutableAnnotation[] getMutableMatches(MutableAnnotation data) {
					return data.getMutableAnnotations(type);
				}
				public String getName() {
					return ((type == null) ? "<All Annotations>" : type);
				}
				public String getProviderClassName() {
					return null;
				}
				public String getTypeLabel() {
					return "Annotation Filter";
				}
				public boolean equals(Object obj) {
					return ((obj != null) && this.toString().equals(obj.toString()));
				}
				public String toString() {
					return ((type == null) ? "<All Annotations>" : type);
				}
			}, attribute);
		}
		
		//	produce GPath filter
		else {
			final GPath gPath;
			try {
				gPath = new GPath(filterString);
			}
			catch (Exception e) {
				if (reportError)
					JOptionPane.showMessageDialog(DialogPanel.getTopWindow(), ("'" + filterString + "' is not a valid GPath expression:\n" + e.getMessage()), "GPath Validation", JOptionPane.ERROR_MESSAGE);
				return null;
			}
			return new McAnnotationFilter(new AnnotationFilter() {
				public boolean accept(Annotation annotation) {
					return false;
				}
				public QueriableAnnotation[] getMatches(QueriableAnnotation data) {
					try {
						return gPath.evaluate(data, null);
					}
					catch (GPathException gpe) {
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
					return gPath.toString();
				}
				public String getProviderClassName() {
					return null;
				}
				public String getTypeLabel() {
					return "Annotation Filter";
				}
				public boolean equals(Object obj) {
					return ((obj != null) && this.toString().equals(obj.toString()));
				}
				public String toString() {
					return gPath.toString();
				}
			}, attribute);
		}
	}
	
	private class McAnnotationFilter implements AnnotationFilter {
		AnnotationFilter filter;
		String attribute;
		McAnnotationFilter(AnnotationFilter filter, String attribute) {
			this.filter = filter;
			this.attribute = attribute;
		}
		public boolean accept(Annotation annotation) {
			return this.filter.accept(annotation);
		}
		public QueriableAnnotation[] getMatches(QueriableAnnotation data) {
			return this.filter.getMatches(data);
		}
		public MutableAnnotation[] getMutableMatches(MutableAnnotation data) {
			return this.filter.getMutableMatches(data);
		}
		public String getName() {
			return this.filter.getName();
		}
		public String getProviderClassName() {
			return this.filter.getProviderClassName();
		}
		public String getTypeLabel() {
			return this.filter.getTypeLabel();
		}
		public boolean equals(Object obj) {
			return ((obj != null) && this.toString().equals(obj.toString()));
		}
		public String toString() {
			return (this.filter.getTypeLabel() + ": " + this.filter.getName());
		}
	}
	
	/* 
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentProcessorManager#createDocumentProcessor()
	 */
	public String createDocumentProcessor() {
		return this.createMarkupConverter(new Settings(), null);
	}
	
	/* 
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentProcessorManager#editDocumentProcessor(java.lang.String)
	 */
	public void editDocumentProcessor(String name) {
		Settings set = this.loadSettingsResource(name);
		if ((set == null) || set.isEmpty()) return;
		
		EditMarkupConverterDialog emcd = new EditMarkupConverterDialog(name, set, ((String[]) this.externalFilters.keySet().toArray(new String[this.externalFilters.size()])));
		emcd.setVisible(true);
		
		if (emcd.isCommitted()) try {
			this.storeSettingsResource(name, emcd.getMarkupConverter());
		} catch (IOException ioe) {}
	}
	
	/* 
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentProcessorManager#editDocumentProcessors()
	 */
	public void editDocumentProcessors() {
		this.editMarkupConverters();
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractDocumentProcessorManager#getDocumentProcessor(java.lang.String)
	 */
	public DocumentProcessor getDocumentProcessor(String name) {
		return this.getMarkupConverter(name);
	}
	
	private class MarkupConverter implements DocumentProcessor {
		
		static final String CONVERSION_FILTER_ATTRIBUTE_NAME = "FILTER";
		static final String CONVERSION_TARGET_ATTRIBUTE_NAME = "TARGET";
		
		static final String CLONE_CONVERSION = "#C ";
		static final String REMOVE_DUPLICATES_CONVERSION = "#R";
		static final String DELETE_CONVERSION = "#D";
		
		
		private StringVector filterList;
		private Properties conversion;
		
		private MarkupConverterManager parent;
		private String name;
		
		MarkupConverter(MarkupConverterManager parent, String name, Settings settings) {
			this.parent = parent;
			this.name = name;
			this.filterList = new StringVector();
			this.conversion = new Properties();
			for (int f = 0; f < (settings.size() / 2); f++) {
				String filter = settings.getSetting(CONVERSION_FILTER_ATTRIBUTE_NAME + f);
				if (filter != null) {
					this.filterList.addElement(filter);
					this.conversion.setProperty(filter, settings.getSetting((CONVERSION_TARGET_ATTRIBUTE_NAME + f), filter));
				}
			}
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentProcessor#getName()
		 */
		public String getName() {
			return this.name;
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentProcessor#getTypeLabel()
		 */
		public String getTypeLabel() {
			return "Markup Converter";
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentProcessor#getProviderClassName()
		 */
		public String getProviderClassName() {
			return MarkupConverterManager.class.getName();
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentProcessor#process(de.uka.ipd.idaho.gamta.MutableAnnotation, java.util.Properties)
		 */
		public void process(MutableAnnotation data, Properties parameters) {
			for (int t = 0; t < this.filterList.size(); t++) {
				String filter = this.filterList.get(t);
				String newFilter = this.conversion.getProperty(filter, filter);
				this.parent.convertMarkup(data, filter, newFilter);
			}
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentProcessor#process(de.uka.ipd.idaho.gamta.MutableAnnotation)
		 */
		public void process(MutableAnnotation data) {
			this.process(data, new Properties());
		}
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.ResourceManager#getResourceTypeLabel()
	 */
	public String getResourceTypeLabel() {
		return "Markup Converter";
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractDocumentProcessorManager#getFileExtension()
	 */
	protected String getFileExtension() {
		return FILE_EXTENSION;
	}
	
	private static final String CLONE_ANNOTATIONS_CONVERSION = "CLONE_ANNOTATIONS_CONVERSION";
	
	private static final String RENAME_ANNOTATIONS_CONVERSION = "RENAME_ANNOTATIONS_CONVERSION";
	
	private static final String REMOVE_ANNOTATIONS_CONVERSION = "REMOVE_ANNOTATIONS_CONVERSION";
	private static final String REMOVE_DUPLICATE_ANNOTATIONS_CONVERSION = "REMOVE_DUPLICATE_ANNOTATIONS_CONVERSION";
	private static final String REMOVE_INNER_ANNOTATIONS_CONVERSION = "REMOVE_INNER_ANNOTATIONS_CONVERSION";
	private static final String REMOVE_OUTER_ANNOTATIONS_CONVERSION = "REMOVE_OUTER_ANNOTATIONS_CONVERSION";
	private static final String REMOVE_CONTAINED_ANNOTATIONS_CONVERSION = "REMOVE_CONTAINED_ANNOTATIONS_CONVERSION";
	private static final String REMOVE_CONTAINING_ANNOTATIONS_CONVERSION = "REMOVE_CONTAINING_ANNOTATIONS_CONVERSION";
	private static final String REMOVE_NOT_CONTAINED_ANNOTATIONS_CONVERSION = "REMOVE_NOT_CONTAINED_ANNOTATIONS_CONVERSION";
	private static final String REMOVE_NOT_CONTAINING_ANNOTATIONS_CONVERSION = "REMOVE_NOT_CONTAINING_ANNOTATIONS_CONVERSION";
	
	private static final String DELETE_ANNOTATIONS_CONVERSION = "DELETE_ANNOTATIONS_CONVERSION";
	
	private static final String RENAME_ATTRIBUTE_CONVERSION = "RENAME_ATTRIBUTE_CONVERSION";
	private static final String REMOVE_ATTRIBUTE_CONVERSION = "REMOVE_ATTRIBUTE_CONVERSION";
	
	private static final String ALL_ANNOTATIONS_TYPE = "<All Annotations>";
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractDocumentProcessorManager#getMainMenuItems()
	 */
	public JMenuItem[] getMainMenuItems() {
		ArrayList collector = new ArrayList();
		JMenuItem mi;
		
		mi = new JMenuItem("Clone Annotations");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				applyDocumentProcessor(CLONE_ANNOTATIONS_CONVERSION);
			}
		});
		collector.add(mi);
		mi = new JMenuItem("Rename Annotations");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				applyDocumentProcessor(RENAME_ANNOTATIONS_CONVERSION);
			}
		});
		collector.add(mi);
		mi = new JMenuItem("Remove Annotations");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				applyDocumentProcessor(REMOVE_ANNOTATIONS_CONVERSION);
			}
		});
		collector.add(mi);
		mi = new JMenuItem("Remove Duplicate Annotations");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				applyDocumentProcessor(REMOVE_DUPLICATE_ANNOTATIONS_CONVERSION);
			}
		});
		collector.add(mi);
		mi = new JMenuItem("Remove Self-Contained Annotations");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				applyDocumentProcessor(REMOVE_INNER_ANNOTATIONS_CONVERSION);
			}
		});
		collector.add(mi);
		mi = new JMenuItem("Remove Self-Containing Annotations");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				applyDocumentProcessor(REMOVE_OUTER_ANNOTATIONS_CONVERSION);
			}
		});
		collector.add(mi);
		mi = new JMenuItem("Remove Annotations Contained in ...");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				applyDocumentProcessor(REMOVE_CONTAINED_ANNOTATIONS_CONVERSION);
			}
		});
		collector.add(mi);
		mi = new JMenuItem("Remove Annotations Containing ...");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				applyDocumentProcessor(REMOVE_CONTAINING_ANNOTATIONS_CONVERSION);
			}
		});
		collector.add(mi);
		mi = new JMenuItem("Remove Annotations Not Contained in ...");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				applyDocumentProcessor(REMOVE_NOT_CONTAINED_ANNOTATIONS_CONVERSION);
			}
		});
		collector.add(mi);
		mi = new JMenuItem("Remove Annotations Not Containing ...");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				applyDocumentProcessor(REMOVE_NOT_CONTAINING_ANNOTATIONS_CONVERSION);
			}
		});
		collector.add(mi);
		mi = new JMenuItem("Delete Annotations");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				applyDocumentProcessor(DELETE_ANNOTATIONS_CONVERSION);
			}
		});
		collector.add(mi);
		collector.add(GoldenGateConstants.MENU_SEPARATOR_ITEM);
		
		mi = new JMenuItem("Rename Annotation Attribute");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				applyDocumentProcessor(RENAME_ATTRIBUTE_CONVERSION);
			}
		});
		collector.add(mi);
		mi = new JMenuItem("Remove Annotation Attribute");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				applyDocumentProcessor(REMOVE_ATTRIBUTE_CONVERSION);
			}
		});
		collector.add(mi);
		
		if (this.dataProvider.isDataEditable()) {
			collector.add(GoldenGateConstants.MENU_SEPARATOR_ITEM);
			
			mi = new JMenuItem("Create");
			mi.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					createMarkupConverter();
				}
			});
			collector.add(mi);
			mi = new JMenuItem("Edit");
			mi.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					editMarkupConverters();
				}
			});
			collector.add(mi);
		}
		
		return ((JMenuItem[]) collector.toArray(new JMenuItem[collector.size()]));
	}
	
	private void applyDocumentProcessor(String processorName) {
		DocumentEditor data = this.parent.getActivePanel();
		if (data != null) data.applyDocumentProcessor(this.getClass().getName(), processorName);
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractDocumentProcessorManager#getMainMenuTitle()
	 */
	public String getMainMenuTitle() {
		return "Markup Converters";
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractDocumentProcessorManager#getToolsMenuLabel()
	 */
	public String getToolsMenuLabel() {
		return "Apply";
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager#getToolsMenuFunctionItems(de.uka.ipd.idaho.goldenGate.GoldenGateConstants.InvokationTargetProvider)
	 */
	public JMenuItem[] getToolsMenuFunctionItems(final InvokationTargetProvider targetProvider) {
		final String className = this.getClass().getName();
		ArrayList collector = new ArrayList();
		JMenuItem mi;
		
		mi = new JMenuItem("Clone Annotations");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				DocumentEditor target = targetProvider.getFunctionTarget();
				if (target != null)
					target.applyDocumentProcessor(className, CLONE_ANNOTATIONS_CONVERSION);
			}
		});
		collector.add(mi);
		mi = new JMenuItem("Rename Annotations");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				DocumentEditor target = targetProvider.getFunctionTarget();
				if (target != null)
					target.applyDocumentProcessor(className, RENAME_ANNOTATIONS_CONVERSION);
			}
		});
		collector.add(mi);
		mi = new JMenuItem("Remove Annotations");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				DocumentEditor target = targetProvider.getFunctionTarget();
				if (target != null)
					target.applyDocumentProcessor(className, REMOVE_ANNOTATIONS_CONVERSION);
			}
		});
		collector.add(mi);
		mi = new JMenuItem("Remove Duplicate Annotations");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				DocumentEditor target = targetProvider.getFunctionTarget();
				if (target != null)
					target.applyDocumentProcessor(className, REMOVE_DUPLICATE_ANNOTATIONS_CONVERSION);
			}
		});
		collector.add(mi);
		mi = new JMenuItem("Remove Self-Contained Annotations");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				DocumentEditor target = targetProvider.getFunctionTarget();
				if (target != null)
					target.applyDocumentProcessor(className, REMOVE_INNER_ANNOTATIONS_CONVERSION);
			}
		});
		collector.add(mi);
		mi = new JMenuItem("Remove Self-Containing Annotations");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				DocumentEditor target = targetProvider.getFunctionTarget();
				if (target != null)
					target.applyDocumentProcessor(className, REMOVE_OUTER_ANNOTATIONS_CONVERSION);
			}
		});
		collector.add(mi);
		mi = new JMenuItem("Remove Annotations Contained in ...");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				DocumentEditor target = targetProvider.getFunctionTarget();
				if (target != null)
					target.applyDocumentProcessor(className, REMOVE_CONTAINED_ANNOTATIONS_CONVERSION);
			}
		});
		collector.add(mi);
		mi = new JMenuItem("Remove Annotations Containing ...");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				DocumentEditor target = targetProvider.getFunctionTarget();
				if (target != null)
					target.applyDocumentProcessor(className, REMOVE_CONTAINING_ANNOTATIONS_CONVERSION);
			}
		});
		collector.add(mi);
		mi = new JMenuItem("Remove Annotations Not Contained in ...");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				DocumentEditor target = targetProvider.getFunctionTarget();
				if (target != null)
					target.applyDocumentProcessor(className, REMOVE_NOT_CONTAINED_ANNOTATIONS_CONVERSION);
			}
		});
		collector.add(mi);
		mi = new JMenuItem("Remove Annotations Not Containing ...");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				DocumentEditor target = targetProvider.getFunctionTarget();
				if (target != null)
					target.applyDocumentProcessor(className, REMOVE_NOT_CONTAINING_ANNOTATIONS_CONVERSION);
			}
		});
		collector.add(mi);
		mi = new JMenuItem("Delete Annotations");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				DocumentEditor target = targetProvider.getFunctionTarget();
				if (target != null)
					target.applyDocumentProcessor(className, DELETE_ANNOTATIONS_CONVERSION);
			}
		});
		collector.add(mi);
		collector.add(GoldenGateConstants.MENU_SEPARATOR_ITEM);
		
		mi = new JMenuItem("Rename Annotation Attribute");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				DocumentEditor target = targetProvider.getFunctionTarget();
				if (target != null)
					target.applyDocumentProcessor(className, RENAME_ATTRIBUTE_CONVERSION);
			}
		});
		collector.add(mi);
		mi = new JMenuItem("Remove Annotation Attribute");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				DocumentEditor target = targetProvider.getFunctionTarget();
				if (target != null)
					target.applyDocumentProcessor(className, REMOVE_ATTRIBUTE_CONVERSION);
			}
		});
		collector.add(mi);
		return ((JMenuItem[]) collector.toArray(new JMenuItem[collector.size()]));
	}

	/* retrieve a MarkupConverter by its name
	 * @param	name	the name of the required MarkupConverter
	 * @return the MarkupConverter with the required name, or null, if there is no such MarkupConverter
	 */
	private DocumentProcessor getMarkupConverter(String name) {
		if (name == null) return null;
		return this.getMarkupConverter(name, this.loadSettingsResource(name));
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractDocumentProcessorManager#applyDocumentProcessor(java.lang.String, de.uka.ipd.idaho.goldenGate.DocumentEditor, java.util.Properties)
	 */
	public void applyDocumentProcessor(String processorName, DocumentEditor data, Properties parameters) {
		if ((data != null) && parameters.containsKey(Resource.INTERACTIVE_PARAMETER)) {
			final QueriableAnnotation content = data.getContent();
			
			if (CLONE_ANNOTATIONS_CONVERSION.equals(processorName)) {
				String[] types = data.getAnnotationTypes();
				
				final StringSelector typeSelector = new StringSelector("Select Annotation Type", types, false);
				final StringSelector newTypeSelector = new StringSelector("Select Clone Type", types, true);
				typeSelector.addItemListener(new ItemListener() {
					public void itemStateChanged(ItemEvent ie) {
						newTypeSelector.setSelectedString(typeSelector.getSelectedString());
					}
				});
				
				StringSelector[] selectors = {typeSelector, newTypeSelector};
				MarkupConversionDialog mcd = new MarkupConversionDialog("Clone Annotations", "Clone", selectors, "Confirm Clone Annotations", "Really clone Annotations?");
				mcd.setVisible(true);
				
				if (mcd.wasCommitted()) {
					String type = typeSelector.getSelectedString();
					String cloneType = newTypeSelector.getSelectedString();
					McAnnotationFilter filter = this.getAnnotationFilter(type, false);
					ResourceSplashScreen splashScreen = new ResourceSplashScreen("Cloning Annotations", ("Cloning " + type + " as " + cloneType));
					data.applyDocumentProcessor(new McFilterDocumentProcessor(filter, (MarkupConverter.CLONE_CONVERSION + cloneType), ("Clone " + type + " as " + cloneType)), splashScreen, parameters);
				}
			}
			
			else if (RENAME_ANNOTATIONS_CONVERSION.equals(processorName)) {
				String[] types = data.getAnnotationTypes();
				
				final StringSelector typeSelector = new StringSelector("Select Annotation Type", types, false);
				final StringSelector newTypeSelector = new StringSelector("Rename Type to", types, true);
				typeSelector.addItemListener(new ItemListener() {
					public void itemStateChanged(ItemEvent ie) {
						newTypeSelector.setSelectedString(typeSelector.getSelectedString());
					}
				});
				
				StringSelector[] selectors = {typeSelector, newTypeSelector};
				MarkupConversionDialog mcd = new MarkupConversionDialog("Rename Annotations", "Rename", selectors, "Confirm Rename Annotations", "Really rename Annotations?");
				mcd.setVisible(true);
				
				if (mcd.wasCommitted()) {
					String type = typeSelector.getSelectedString();
					String newType = newTypeSelector.getSelectedString();
					McAnnotationFilter filter = this.getAnnotationFilter(type, false);
					ResourceSplashScreen splashScreen = new ResourceSplashScreen("Renaming Annotations", ("Renaming " + type + " to " + newType));
					data.applyDocumentProcessor(new McFilterDocumentProcessor(filter, newType, ("Rename " + type + " to " + newType)), splashScreen, parameters);
				}
			}
			
			else if (REMOVE_ANNOTATIONS_CONVERSION.equals(processorName)) {
				String[] types = data.getAnnotationTypes();
				
				StringVector typeList = new StringVector();
				typeList.addElement(ALL_ANNOTATIONS_TYPE);
				typeList.addContent(types);
				types = typeList.toStringArray();
				
				StringSelector typeSelector = new StringSelector("Select Annotation Type", types, false);
				
				StringSelector[] selectors = {typeSelector};
				MarkupConversionDialog mcd = new MarkupConversionDialog("Remove Annotations", "Remove", selectors, "Confirm Remove Annotations", "Really remove Annotations?");
				mcd.setVisible(true);
				
				if (mcd.wasCommitted()) {
					String type = typeSelector.getSelectedString();
					if (ALL_ANNOTATIONS_TYPE.equals(type)) type = "*";
					McAnnotationFilter filter = this.getAnnotationFilter(type, false);
					ResourceSplashScreen splashScreen = new ResourceSplashScreen("Removing Annotations", ("Removing " + type + " annotations"));
					data.applyDocumentProcessor(new McFilterDocumentProcessor(filter, "", ("Remove " + type)), splashScreen, parameters);
				}
			}
			
			else if (REMOVE_DUPLICATE_ANNOTATIONS_CONVERSION.equals(processorName)) {
				String[] types = data.getAnnotationTypes();
				
				StringVector typeList = new StringVector();
				typeList.addElement(ALL_ANNOTATIONS_TYPE);
				typeList.addContent(types);
				types = typeList.toStringArray();
				
				StringSelector typeSelector = new StringSelector("Select Annotation Type", types, false);
				
				StringSelector[] selectors = {typeSelector};
				MarkupConversionDialog mcd = new MarkupConversionDialog("Remove Duplicate Annotations", "Remove", selectors, null, null);
				mcd.setVisible(true);
				
				if (mcd.wasCommitted()) {
					String type = typeSelector.getSelectedString();
					if (ALL_ANNOTATIONS_TYPE.equals(type)) type = "*";
					McAnnotationFilter filter = this.getAnnotationFilter(type, false);
					ResourceSplashScreen splashScreen = new ResourceSplashScreen("Removing Duplicate Annotations", ("Removing duplicate " + type + " annotations"));
					data.applyDocumentProcessor(new McFilterDocumentProcessor(filter, MarkupConverter.REMOVE_DUPLICATES_CONVERSION, ("Remove duplicate " + type + "s")), splashScreen, parameters);
				}
			}
			
			else if (REMOVE_INNER_ANNOTATIONS_CONVERSION.equals(processorName)) {
				String[] types = data.getAnnotationTypes();
				
				StringSelector typeSelector = new StringSelector("Select Annotation Type", types, false);
				
				StringSelector[] selectors = {typeSelector};
				MarkupConversionDialog mcd = new MarkupConversionDialog("Remove Inner Annotations", "Remove Inner", selectors, "Confirm Remove Annotations", "Really remove inner Annotations?");
				mcd.setVisible(true);
				
				if (mcd.wasCommitted()) {
					String type = typeSelector.getSelectedString();
					McAnnotationFilter filter = this.getAnnotationFilter((type + "/" + type), false);
					ResourceSplashScreen splashScreen = new ResourceSplashScreen("Removing Self-Nested Annotations", ("Removing " + type + " annotations nested in others"));
					data.applyDocumentProcessor(new McFilterDocumentProcessor(filter, "", ("Remove self-nested " + type + "s")), splashScreen, parameters);
				}
			}
			
			else if (REMOVE_OUTER_ANNOTATIONS_CONVERSION.equals(processorName)) {
				String[] types = data.getAnnotationTypes();
				
				StringSelector typeSelector = new StringSelector("Select Annotation Type", types, false);
				
				StringSelector[] selectors = {typeSelector};
				MarkupConversionDialog mcd = new MarkupConversionDialog("Remove Outer Annotations", "Remove Outer", selectors, "Confirm Remove Annotations", "Really remove outer Annotations?");
				mcd.setVisible(true);
				
				if (mcd.wasCommitted()) {
					String type = typeSelector.getSelectedString();
					McAnnotationFilter filter = this.getAnnotationFilter((type + "[./" + type + "]"), false);
					ResourceSplashScreen splashScreen = new ResourceSplashScreen("Removing Self-Containing Annotations", ("Removing " + type + " annotations containing others"));
					data.applyDocumentProcessor(new McFilterDocumentProcessor(filter, "", ("Remove self-containing " + type + "s")), splashScreen, parameters);
				}
			}
			
			else if (REMOVE_CONTAINED_ANNOTATIONS_CONVERSION.equals(processorName)) {
				String[] types = data.getAnnotationTypes();
				
				StringSelector outerTypeSelector = new StringSelector("Select Containing Type", types, false);
				StringSelector innerTypeSelector = new StringSelector("Select Contained Type", types, false);
				
				StringSelector[] selectors = {outerTypeSelector, innerTypeSelector};
				MarkupConversionDialog mcd = new MarkupConversionDialog("Remove Contained Annotations", "Remove Contained", selectors, "Confirm Remove Annotations", "Really remove contained Annotations?");
				mcd.setVisible(true);
				
				if (mcd.wasCommitted()) {
					String outerType = outerTypeSelector.getSelectedString();
					String innerType = innerTypeSelector.getSelectedString();
					McAnnotationFilter filter = this.getAnnotationFilter((outerType + "/" + innerType), false);
					ResourceSplashScreen splashScreen = new ResourceSplashScreen("Removing Nested Annotations", ("Removing " + innerType + " annotations nested in " + outerType));
					data.applyDocumentProcessor(new McFilterDocumentProcessor(filter, "", ("Remove " + innerType + " nested in " + outerType)), splashScreen, parameters);
				}
			}
			
			else if (REMOVE_CONTAINING_ANNOTATIONS_CONVERSION.equals(processorName)) {
				String[] types = data.getAnnotationTypes();
				
				StringSelector innerTypeSelector = new StringSelector("Select Contained Type", types, false);
				StringSelector outerTypeSelector = new StringSelector("Select Containing Type", types, false);
				
				StringSelector[] selectors = {outerTypeSelector, innerTypeSelector};
				MarkupConversionDialog mcd = new MarkupConversionDialog("Remove Containing Annotations", "Remove Containing", selectors, "Confirm Remove Annotations", "Really remove containing Annotations?");
				mcd.setVisible(true);
				
				if (mcd.wasCommitted()) {
					String outerType = outerTypeSelector.getSelectedString();
					String innerType = innerTypeSelector.getSelectedString();
					McAnnotationFilter filter = this.getAnnotationFilter((outerType + "[./" + innerType + "]"), false);
					ResourceSplashScreen splashScreen = new ResourceSplashScreen("Removing Containing Annotations", ("Removing " + outerType + " annotations containing " + innerType));
					data.applyDocumentProcessor(new McFilterDocumentProcessor(filter, "", ("Remove " + outerType + " containing " + innerType)), splashScreen, parameters);
				}
			}
			
			else if (REMOVE_NOT_CONTAINED_ANNOTATIONS_CONVERSION.equals(processorName)) {
				String[] types = data.getAnnotationTypes();
				
				StringSelector outerTypeSelector = new StringSelector("Select Containing Type", types, false);
				StringSelector innerTypeSelector = new StringSelector("Select Contained Type", types, false);
				
				StringSelector[] selectors = {outerTypeSelector, innerTypeSelector};
				MarkupConversionDialog mcd = new MarkupConversionDialog("Remove Not Contained Annotations", "Remove Not Contained", selectors, "Confirm Remove Annotations", "Really remove uncontained Annotations?");
				mcd.setVisible(true);
				
				if (mcd.wasCommitted()) {
					String outerType = outerTypeSelector.getSelectedString();
					String innerType = innerTypeSelector.getSelectedString();
					McAnnotationFilter filter = this.getAnnotationFilter((innerType + "[not(./@" + Annotation.ANNOTATION_ID_ATTRIBUTE + " = " + DocumentRoot.DOCUMENT_TYPE + "/" + outerType + "/" + innerType + "/@" + Annotation.ANNOTATION_ID_ATTRIBUTE + ")]"), false);
					ResourceSplashScreen splashScreen = new ResourceSplashScreen("Removing Non-Nested Annotations", ("Removing " + innerType + " annotations not contained in " + outerType));
					data.applyDocumentProcessor(new McFilterDocumentProcessor(filter, "", ("Remove " + innerType + " not nested in " + outerType)), splashScreen, parameters);
				}
			}
			
			else if (REMOVE_NOT_CONTAINING_ANNOTATIONS_CONVERSION.equals(processorName)) {
				String[] types = data.getAnnotationTypes();
				
				StringSelector innerTypeSelector = new StringSelector("Select Contained Type", types, true);
				StringSelector outerTypeSelector = new StringSelector("Select Containing Type", types, false);
				
				StringSelector[] selectors = {outerTypeSelector, innerTypeSelector};
				MarkupConversionDialog mcd = new MarkupConversionDialog("Remove Not Containing Annotations", "Remove Not Containing", selectors, "Confirm Remove Annotations", "Really remove non-containing Annotations?");
				mcd.setVisible(true);
				
				if (mcd.wasCommitted()) {
					String outerType = outerTypeSelector.getSelectedString();
					String innerType = innerTypeSelector.getSelectedString();
					McAnnotationFilter filter = this.getAnnotationFilter((outerType + "[not(./" + innerType + ")]"), false);
					ResourceSplashScreen splashScreen = new ResourceSplashScreen("Removing Non-Containing Annotations", ("Removing " + outerType + " annotations not containing " + innerType));
					data.applyDocumentProcessor(new McFilterDocumentProcessor(filter, "", ("Remove " + outerType + " not containing " + innerType)), splashScreen, parameters);
				}
			}
			
			else if (DELETE_ANNOTATIONS_CONVERSION.equals(processorName)) {
				String[] types = data.getAnnotationTypes();
				
				StringSelector typeSelector = new StringSelector("Select Annotation Type", types, false);
				
				StringSelector[] selectors = {typeSelector};
				MarkupConversionDialog mcd = new MarkupConversionDialog("Delete Annotations", "Delete", selectors, "Confirm Delete Annotations", "Really delete Annotations\nand all Tokens in them?");
				mcd.setVisible(true);
				
				if (mcd.wasCommitted()) {
					String type = typeSelector.getSelectedString();
					McAnnotationFilter filter = this.getAnnotationFilter(type, false);
					ResourceSplashScreen splashScreen = new ResourceSplashScreen("Deleting Annotations", ("Deleting " + type + " annotations"));
					data.applyDocumentProcessor(new McFilterDocumentProcessor(filter, MarkupConverter.DELETE_CONVERSION, ("Delete " + type + "s")), splashScreen, parameters);
				}
			}
			
			else if (RENAME_ATTRIBUTE_CONVERSION.equals(processorName)) {
				String[] types = data.getAnnotationTypes();
				
				StringVector typeList = new StringVector();
				typeList.addElement(ALL_ANNOTATIONS_TYPE);
				typeList.addContent(types);
				types = typeList.toStringArray();
				
				StringVector attributeList = new StringVector();
				Annotation[] annotations = content.getAnnotations(null);
				for (int a = 0; a < annotations.length; a++)
					attributeList.addContentIgnoreDuplicates(annotations[a].getAttributeNames());
				
				final StringSelector typeSelector = new StringSelector("Select Annotation Type", types, false);
				final StringSelector attributeSelector = new StringSelector("Select Attribute", attributeList.toStringArray(), false);
				final StringSelector newAttributeSelector = new StringSelector("Rename Attribute to", attributeList.toStringArray(), true);
				typeSelector.addItemListener(new ItemListener() {
					public void itemStateChanged(ItemEvent ie) {
						String type = typeSelector.getSelectedString();
						if (ALL_ANNOTATIONS_TYPE.equals(type)) type = null;
						
						String attribute = attributeSelector.getSelectedString();
						String newAttribute = newAttributeSelector.getSelectedString();
						
						StringVector attributeList = new StringVector();
						Annotation[] annotations = content.getAnnotations(type);
						for (int a = 0; a < annotations.length; a++)
							attributeList.addContentIgnoreDuplicates(annotations[a].getAttributeNames());
						
						attributeSelector.setStrings(attributeList.toStringArray());
						if ((attribute != null) && (attribute.trim().length() != 0)) attributeSelector.setSelectedString(attribute);
						else if (attributeList.size() != 0) attributeSelector.setSelectedString(attributeList.firstElement());
						
						newAttributeSelector.setStrings(attributeList.toStringArray());
						if ((newAttribute != null) && (newAttribute.trim().length() != 0)) newAttributeSelector.setSelectedString(newAttribute);
						else if ((attribute != null) && (attribute.trim().length() != 0)) newAttributeSelector.setSelectedString(attribute);
						else if (attributeList.size() != 0) newAttributeSelector.setSelectedString(attributeList.firstElement());
					}
				});
				attributeSelector.addItemListener(new ItemListener() {
					public void itemStateChanged(ItemEvent ie) {
						newAttributeSelector.setSelectedString(attributeSelector.getSelectedString());
					}
				});
				
				StringSelector[] selectors = {typeSelector, attributeSelector, newAttributeSelector};
				MarkupConversionDialog mcd = new MarkupConversionDialog("Rename Attribute", "Rename", selectors, "Confirm Rename Attribute", "Really rename Annotation attribute?");
				mcd.setVisible(true);
				
				if (mcd.wasCommitted()) {
					String type = typeSelector.getSelectedString();
					String attribute = attributeSelector.getSelectedString();
					String newAttribute = newAttributeSelector.getSelectedString();
					McAnnotationFilter filter = this.getAnnotationFilter((type + "@" + attribute), false);
					ResourceSplashScreen splashScreen = new ResourceSplashScreen("Renaming Annotation Attribute", ("Renaming " + attribute + " attribute of " + type + " annotations to " + newAttribute));
					data.applyDocumentProcessor(new McFilterDocumentProcessor(filter, newAttribute, ("Rename attribute " + attribute + " to " + newAttribute)), splashScreen, parameters);
				}
			}
			
			else if (REMOVE_ATTRIBUTE_CONVERSION.equals(processorName)) {
				String[] types = data.getAnnotationTypes();
				
				StringVector typeList = new StringVector();
				typeList.addElement(ALL_ANNOTATIONS_TYPE);
				typeList.addContent(types);
				types = typeList.toStringArray();
				
				StringVector attributeList = new StringVector();
				Annotation[] annotations = content.getAnnotations(null);
				for (int a = 0; a < annotations.length; a++)
					attributeList.addContentIgnoreDuplicates(annotations[a].getAttributeNames());
				
				final StringSelector typeSelector = new StringSelector("Select Annotation Type", types, false);
				final StringSelector attributeSelector = new StringSelector("Select Attribute", attributeList.toStringArray(), false);
				typeSelector.addItemListener(new ItemListener() {
					public void itemStateChanged(ItemEvent ie) {
						String type = typeSelector.getSelectedString();
						if (ALL_ANNOTATIONS_TYPE.equals(type)) type = null;
						
						String attribute = attributeSelector.getSelectedString();
						
						StringVector attributeList = new StringVector();
						Annotation[] annotations = content.getAnnotations(type);
						for (int a = 0; a < annotations.length; a++)
							attributeList.addContentIgnoreDuplicates(annotations[a].getAttributeNames());
						
						attributeSelector.setStrings(attributeList.toStringArray());
						if ((attribute != null) && (attribute.trim().length() != 0)) attributeSelector.setSelectedString(attribute);
						else if (attributeList.size() != 0) attributeSelector.setSelectedString(attributeList.firstElement());
					}
				});
				
				StringSelector[] selectors = {typeSelector, attributeSelector};
				MarkupConversionDialog mcd = new MarkupConversionDialog("Remove Attribute", "Remove", selectors, "Confirm Remove Attribute", "Really remove attribute from Annotations?");
				mcd.setVisible(true);
				
				if (mcd.wasCommitted()) {
					String type = typeSelector.getSelectedString();
					String attribute = attributeSelector.getSelectedString();
					McAnnotationFilter filter = this.getAnnotationFilter((type + "@" + attribute), false);
					ResourceSplashScreen splashScreen = new ResourceSplashScreen("Removing Annotation Attribute", ("Removing " + attribute + " from " + type + " annotations"));
					data.applyDocumentProcessor(new McFilterDocumentProcessor(filter, "", ("Remove attribute " + attribute)), splashScreen, parameters);
				}
			}
			else super.applyDocumentProcessor(processorName, data, parameters);
		}
		else super.applyDocumentProcessor(processorName, data, parameters);
	}
	
	private class MarkupConversionDialog extends DialogPanel {
		private boolean committed = false;
		
		MarkupConversionDialog(String title, String commitText, StringSelector[] selectors, String confirmTitle, String confirmText) {
			super(title, true);
			
			JPanel selectorPanel = new JPanel(new GridLayout(0, 1));
			for (int s = 0; s < selectors.length; s++)
				selectorPanel.add(selectors[s]);
			
			JButton commitButton = new JButton(commitText);
			commitButton.setPreferredSize(new Dimension(100, 21));
			commitButton.setBorder(BorderFactory.createRaisedBevelBorder());
			commitButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					committed = true;
					dispose();
				}
			});
			JButton abortButton = new JButton("Cancel");
			abortButton.setPreferredSize(new Dimension(100, 21));
			abortButton.setBorder(BorderFactory.createRaisedBevelBorder());
			abortButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					dispose();
				}
			});
			
			JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER), true);
			buttonPanel.add(commitButton);
			buttonPanel.add(abortButton);
			
			this.setLayout(new BorderLayout());
			this.add(selectorPanel, BorderLayout.CENTER);
			this.add(buttonPanel, BorderLayout.SOUTH);
			
			this.setSize(new Dimension(250, ((selectors.length * 30) + 55)));
			this.setResizable(true);
			this.setLocationRelativeTo(this.getOwner());
		}
		
		boolean wasCommitted() {
			return this.committed;
		}
	}
	
	private class McFilterDocumentProcessor implements DocumentProcessor {
		private McAnnotationFilter filter;
		private String target;
		private String name;
		McFilterDocumentProcessor(McAnnotationFilter filter, String target, String name) {
			this.filter = filter;
			this.target = target;
			this.name = name;
		}
		public void process(MutableAnnotation data) {
			Properties parameters = new Properties();
			parameters.setProperty(Resource.INTERACTIVE_PARAMETER, Resource.INTERACTIVE_PARAMETER);
			this.process(data, parameters);
		}
		public void process(MutableAnnotation data, Properties parameters) {
			convertMarkup(data, this.filter, this.target);
		}
		public String getProviderClassName() {
			return MarkupConverterManager.class.getName();
		}
		public String getName() {
			return this.name;
		}
		public String getTypeLabel() {
			return "Markup Converter";
		}
	}
	
	private void convertMarkup(MutableAnnotation data, String source, String target) {
		if (data != null)
			this.convertMarkup(data, this.getAnnotationFilter(source, false), target);
	}
	
	private void convertMarkup(MutableAnnotation data, McAnnotationFilter filter, String target) {
		if ((data != null) && (filter != null)) {
			
			//	convert specific filter
			if (filter.attribute == null) {
				
				//	remove duplicate Annotations
				if (MarkupConverter.REMOVE_DUPLICATES_CONVERSION.equals(target))
					AnnotationTools.removeDuplicates(data, filter);
				
				//	remove Annotations containing an Annotation of the same type
				else if (MarkupConverter.DELETE_CONVERSION.equals(target))
					AnnotationTools.deleteAnnotations(data, filter);
				
				//	remove Annotations
				else if ((target == null) || (target.length() == 0))
					AnnotationTools.removeAnnotations(data, filter);
				
				//	clone Annotations
				else if (target.startsWith(MarkupConverter.CLONE_CONVERSION)) {
					String cloneType = target.substring(MarkupConverter.CLONE_CONVERSION.length()).trim();
					AnnotationTools.cloneAnnotations(data, filter, cloneType);
				}
				
				//	rename Annotations
				else AnnotationTools.renameAnnotations(data, filter, target);
			}
			
			//	convert attribute
			else {
				String attribute = filter.attribute;
				if ((attribute.length() == 0) || attribute.equals("*"))
					attribute = null;
				
				//	remove attribute
				if ((target == null) || (target.length() == 0))
					AnnotationTools.removeAnnotationAttribute(data, filter, attribute);
				
				//	rename attribute
				else AnnotationTools.renameAnnotationAttribute(data, filter, attribute, target);
			}
		}
	}
	
	private DocumentProcessor getMarkupConverter(String name, Settings settings) {
		if (settings == null) return null;
		return new MarkupConverter(this, name, settings);
	}
	
	private boolean createMarkupConverter() {
		return (this.createMarkupConverter(new Settings(), null) != null);
	}
	
	private boolean cloneMarkupConverter() {
		String selectedName = this.resourceNameList.getSelectedName();
		if (selectedName == null)
			return this.createMarkupConverter();
		else {
			String name = "New " + selectedName;
			return (this.createMarkupConverter(this.loadSettingsResource(selectedName), name) != null);
		}
	}
	
	private String createMarkupConverter(Settings set, String name) {
		CreateMarkupConverterDialog cfd = new CreateMarkupConverterDialog(name, set, ((String[]) this.externalFilters.keySet().toArray(new String[this.externalFilters.size()])));
		cfd.setVisible(true);
		if (cfd.isCommitted()) {
			Settings markupConverter = cfd.getMarkupConverter();
			String markupConverterName = cfd.getMarkupConverterName();
			if (!markupConverterName.endsWith(FILE_EXTENSION)) markupConverterName += FILE_EXTENSION;
			try {
				if (this.storeSettingsResource(markupConverterName, markupConverter)) {
					this.resourceNameList.refresh();
					return markupConverterName;
				}
			} catch (IOException e) {}
		}
		return null;
	}
	
	private void editMarkupConverters() {
		final MarkupConverterEditorPanel[] editor = new MarkupConverterEditorPanel[1];
		editor[0] = null;
		
		final DialogPanel editDialog = new DialogPanel("Edit MarkupConverters", true);
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
				createMarkupConverter();
			}
		});
		editButtons.add(button);
		button = new JButton("Clone");
		button.setBorder(BorderFactory.createRaisedBevelBorder());
		button.setPreferredSize(new Dimension(100, 21));
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				cloneMarkupConverter();
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
		final String[] externalFilterNames = ((String[]) this.externalFilters.keySet().toArray(new String[this.externalFilters.size()]));
		String selectedName = this.resourceNameList.getSelectedName();
		if (selectedName == null)
			editorPanel.add(this.getExplanationLabel(), BorderLayout.CENTER);
		else {
			Settings set = this.loadSettingsResource(selectedName);
			if (set == null)
				editorPanel.add(this.getExplanationLabel(), BorderLayout.CENTER);
			else {
				editor[0] = new MarkupConverterEditorPanel(selectedName, set, externalFilterNames);
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
						editor[0] = new MarkupConverterEditorPanel(dataName, set, externalFilterNames);
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
	
	private class CreateMarkupConverterDialog extends DialogPanel {
		
		private JTextField nameField;
		
		private MarkupConverterEditorPanel editor;
		private Settings markupConverter = null;
		private String markupConverterName = null;
		
		CreateMarkupConverterDialog(String name, Settings settings, String[] externalFilters) {
			super("Create Markup Converter", true);
			
			this.nameField = new JTextField((name == null) ? "New MarkupConverter" : name);
			this.nameField.setBorder(BorderFactory.createLoweredBevelBorder());
			
			//	initialize main buttons
			JButton commitButton = new JButton("Create");
			commitButton.setBorder(BorderFactory.createRaisedBevelBorder());
			commitButton.setPreferredSize(new Dimension(100, 21));
			commitButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					markupConverter = editor.getSettings();
					markupConverterName = nameField.getText();
					dispose();
				}
			});
			
			JButton abortButton = new JButton("Cancel");
			abortButton.setBorder(BorderFactory.createRaisedBevelBorder());
			abortButton.setPreferredSize(new Dimension(100, 21));
			abortButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					markupConverter = null;
					markupConverterName = null;
					dispose();
				}
			});
			
			JPanel mainButtonPanel = new JPanel();
			mainButtonPanel.setLayout(new FlowLayout());
			mainButtonPanel.add(commitButton);
			mainButtonPanel.add(abortButton);
			
			//	initialize editor
			this.editor = new MarkupConverterEditorPanel(name, settings, externalFilters);
			
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
			return (this.markupConverter != null);
		}
		
		Settings getMarkupConverter() {
			return this.markupConverter;
		}
		
		String getMarkupConverterName() {
			return this.markupConverterName;
		}
	}

	private class EditMarkupConverterDialog extends DialogPanel {
		
		private MarkupConverterEditorPanel editor;
		private Settings markupConverter = null;
		
		EditMarkupConverterDialog(String name, Settings settings, String[] externalFilters) {
			super(("Edit Markup Converter '" + name + "'"), true);
			
			//	initialize main buttons
			JButton commitButton = new JButton("OK");
			commitButton.setBorder(BorderFactory.createRaisedBevelBorder());
			commitButton.setPreferredSize(new Dimension(100, 21));
			commitButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					markupConverter = editor.getSettings();
					dispose();
				}
			});
			
			JButton abortButton = new JButton("Cancel");
			abortButton.setBorder(BorderFactory.createRaisedBevelBorder());
			abortButton.setPreferredSize(new Dimension(100, 21));
			abortButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					markupConverter = null;
					dispose();
				}
			});
			
			JPanel mainButtonPanel = new JPanel();
			mainButtonPanel.setLayout(new FlowLayout());
			mainButtonPanel.add(commitButton);
			mainButtonPanel.add(abortButton);
			
			//	initialize editor
			this.editor = new MarkupConverterEditorPanel(name, settings, externalFilters);
			
			//	put the whole stuff together
			this.setLayout(new BorderLayout());
			this.add(this.editor, BorderLayout.CENTER);
			this.add(mainButtonPanel, BorderLayout.SOUTH);
			
			this.setResizable(true);
			this.setSize(new Dimension(600, 400));
			this.setLocationRelativeTo(this.getOwner());
		}
		
		boolean isCommitted() {
			return (this.markupConverter != null);
		}
		
		Settings getMarkupConverter() {
			return this.markupConverter;
		}
	}
	
	private class MarkupConverterEditorPanel extends JPanel {
		static final String REMOVE_DUPLICATES = "<remove duplicates>";
		static final String REMOVE = "<remove>";
		static final String DELETE = "<delete>";
		static final String CLONE_PREFIX = "<clone as ...>";
		
		private ArrayList conversions = new ArrayList();
		private int selectedConversion = -1;
		private boolean dirty = false;
		
		private JLabel filterLabel = new JLabel("Filter", JLabel.CENTER);
		private JLabel targetLabel = new JLabel("Target", JLabel.CENTER);
		private JPanel linePanelSpacer = new JPanel();
		private JPanel linePanel = new JPanel(new GridBagLayout());
		
		private String[] targets;
		
		private String name;
		private String[] externalFilters = new String[0];
		
		MarkupConverterEditorPanel(String name, Settings set, String[] externalFilters) {
			super(new BorderLayout(), true);
			this.name = name;
			this.externalFilters = externalFilters;
			
			StringVector targetCollector = new StringVector();
			targetCollector.addElement(REMOVE_DUPLICATES);
			targetCollector.addElement(REMOVE);
			targetCollector.addElement(DELETE);
			targetCollector.addElement(CLONE_PREFIX);
			targetCollector.addContent(DocumentEditor.getAnnotationTypeSuggestions());
			this.targets = targetCollector.toStringArray();
			
			this.filterLabel.setBorder(BorderFactory.createRaisedBevelBorder());
			this.filterLabel.setPreferredSize(new Dimension(160, 21));
			this.filterLabel.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent me) {
					selectConversion(-1);
					linePanel.requestFocusInWindow();
				}
			});
			
			this.targetLabel.setBorder(BorderFactory.createRaisedBevelBorder());
			this.targetLabel.setPreferredSize(new Dimension(160, 21));
			this.targetLabel.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent me) {
					selectConversion(-1);
					linePanel.requestFocusInWindow();
				}
			});
			
			this.linePanelSpacer.setBackground(Color.WHITE);
			this.linePanelSpacer.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent me) {
					selectConversion(-1);
					linePanel.requestFocusInWindow();
				}
			});
			this.linePanel.setBorder(BorderFactory.createLineBorder(this.getBackground(), 3));
			this.linePanel.setFocusable(true);
			this.linePanel.addFocusListener(new FocusAdapter() {
//				public void focusGained(FocusEvent fe) {
//					System.out.println("focusGained");
//				}
//				public void focusLost(FocusEvent fe) {
//					System.out.println("focusLost");
//				}
			});
			
	        final String upKey = "GO_UP";
	        this.linePanel.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0, true), upKey);
	        this.linePanel.getActionMap().put(upKey, new AbstractAction() {
	            public void actionPerformed(ActionEvent ae) {
	                System.out.println(upKey);
	                if (selectedConversion > 0)
	                	selectConversion(selectedConversion - 1);
	                else if (selectedConversion == -1)
                		selectConversion(conversions.size() - 1);
	            }
	        });
	        final String moveUpKey = "MOVE_UP";
	        this.linePanel.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.CTRL_DOWN_MASK, true), moveUpKey);
	        this.linePanel.getActionMap().put(moveUpKey, new AbstractAction() {
	            public void actionPerformed(ActionEvent ae) {
	                System.out.println(moveUpKey);
					moveUp();
	            }
	        });
	        
	        final String downKey = "GO_DOWN";
	        this.linePanel.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0, true), downKey);
	        this.linePanel.getActionMap().put(downKey, new AbstractAction() {
	            public void actionPerformed(ActionEvent ae) {
	                System.out.println(downKey);
                	if (selectedConversion == -1)
                		selectConversion(0);
                	else if ((selectedConversion + 1) < conversions.size())
	                	selectConversion(selectedConversion + 1);
	            }
	        });
	        final String moveDownKey = "MOVE_DOWN";
	        this.linePanel.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.CTRL_DOWN_MASK, true), moveDownKey);
	        this.linePanel.getActionMap().put(moveDownKey, new AbstractAction() {
	            public void actionPerformed(ActionEvent ae) {
	                System.out.println(moveDownKey);
					moveDown();
	            }
	        });

			JScrollPane linePanelBox = new JScrollPane(this.linePanel);
			
			JButton addConversionButton = new JButton("Add Conversion");
			addConversionButton.setBorder(BorderFactory.createRaisedBevelBorder());
			addConversionButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					addConversion();
				}
			});
			
			JButton testConversionButton = new JButton("Test Filter");
			testConversionButton.setBorder(BorderFactory.createRaisedBevelBorder());
			testConversionButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					testFilter();
				}
			});
			
			JButton removeConversionButton = new JButton("Remove Conversion");
			removeConversionButton.setBorder(BorderFactory.createRaisedBevelBorder());
			removeConversionButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					removeConversion();
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
			buttonPanel.add(addConversionButton, gbc.clone());
			gbc.gridx = 1;
			buttonPanel.add(testConversionButton, gbc.clone());
			gbc.gridx = 2;
			buttonPanel.add(removeConversionButton, gbc.clone());
			
			JButton upButton = new JButton("Up");
			upButton.setBorder(BorderFactory.createRaisedBevelBorder());
			upButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					moveUp();
				}
			});
			JButton downButton = new JButton("Down");
			downButton.setBorder(BorderFactory.createRaisedBevelBorder());
			downButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					moveDown();
				}
			});
			
			JPanel reorderButtonPanel = new JPanel(new GridBagLayout());
			gbc = new GridBagConstraints();
			gbc.insets.top = 2;
			gbc.insets.bottom = 2;
			gbc.insets.left = 3;
			gbc.insets.right = 3;
			gbc.weightx = 1;
			gbc.weighty = 1;
			gbc.gridheight = 1;
			gbc.fill = GridBagConstraints.BOTH;
			gbc.gridx = 0;
			gbc.gridwidth = 1;
			
			gbc.gridy = 0;
			reorderButtonPanel.add(upButton, gbc.clone());
			gbc.gridy = 1;
			reorderButtonPanel.add(downButton, gbc.clone());
			
			this.add(reorderButtonPanel, BorderLayout.WEST);
			this.add(linePanelBox, BorderLayout.CENTER);
			this.add(buttonPanel, BorderLayout.SOUTH);
			
			this.setContent(set);
		}
		
		void layoutConversions() {
			this.linePanel.removeAll();
			
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.insets.top = 0;
			gbc.insets.bottom = 0;
			gbc.insets.left = 0;
			gbc.insets.right = 0;
			gbc.weighty = 0;
			gbc.gridy = 0;
			gbc.gridheight = 1;
			gbc.gridwidth = 1;
			gbc.fill = GridBagConstraints.BOTH;
			
			gbc.gridx = 0;
			gbc.weightx = 1;
			this.linePanel.add(this.filterLabel, gbc.clone());
			gbc.gridx = 1;
			gbc.weightx = 0;
			this.linePanel.add(this.targetLabel, gbc.clone());
			gbc.gridy++;
			
			for (int l = 0; l < this.conversions.size(); l++) {
				MarkupConversion line = ((MarkupConversion) this.conversions.get(l));
				line.index = l;
				
				gbc.gridx = 0;
				gbc.weightx = 1;
				this.linePanel.add(line.filterPanel, gbc.clone());
				gbc.gridx = 1;
				gbc.weightx = 0;
				this.linePanel.add(line.targetPanel, gbc.clone());
				gbc.gridy++;
			}
			
			gbc.gridx = 0;
			gbc.weightx = 1;
			gbc.weighty = 1;
			gbc.gridwidth = 2;
			this.linePanel.add(this.linePanelSpacer, gbc.clone());
			
			this.validate();
			this.repaint();
		}
		
		void selectConversion(int index) {
			if (this.selectedConversion == index) return;
			this.selectedConversion = index;
			
			for (int l = 0; l < this.conversions.size(); l++) {
				MarkupConversion line = ((MarkupConversion) this.conversions.get(l));
				if (l == this.selectedConversion) {
					line.filterPanel.setBorder(BorderFactory.createLineBorder(Color.BLUE));
					line.targetPanel.setBorder(BorderFactory.createLineBorder(Color.BLUE));
				}
				else {
					line.setEditing(false);
					line.filterPanel.setBorder(BorderFactory.createLineBorder(this.getBackground()));
					line.targetPanel.setBorder(BorderFactory.createLineBorder(this.getBackground()));
				}
			}
			
			this.linePanel.validate();
			this.linePanel.repaint();
		}
		
		void moveUp() {
			if (this.selectedConversion < 1)
				return;
			this.conversions.add(this.selectedConversion, this.conversions.remove(this.selectedConversion - 1));
			this.selectedConversion--;
			this.layoutConversions();
			this.dirty = true;
		}
		
		void moveDown() {
			if ((this.selectedConversion == -1) || ((this.selectedConversion + 1) == this.conversions.size()))
				return;
			this.conversions.add(this.selectedConversion, this.conversions.remove(this.selectedConversion + 1));
			this.selectedConversion++;
			this.layoutConversions();
			this.dirty = true;
		}
		
		void addConversion() {
			AddConversionDialog acd = new AddConversionDialog(this.externalFilters, this.targets);
			acd.setVisible(true);
			String filter = acd.getFilter();
			if (filter != null)
				this.addConversion(filter, acd.getTarget());
		}
		
		void addConversion(String filter, String target) {
			MarkupConversion line = new MarkupConversion(filter, target);
			line.filterPanel.setBorder(BorderFactory.createLineBorder(this.getBackground()));
			line.targetPanel.setBorder(BorderFactory.createLineBorder(this.getBackground()));
			this.conversions.add(line);
			this.dirty = true;
			this.layoutConversions();
			this.selectConversion(this.conversions.size() - 1);
		}
		
		void removeConversion() {
			if (this.selectedConversion == -1) return;
			int newSelectedConversion = this.selectedConversion;
			this.conversions.remove(this.selectedConversion);
			this.dirty = true;
			this.selectedConversion = -1;
			this.layoutConversions();
			if (newSelectedConversion == this.conversions.size())
				newSelectedConversion--;
			this.selectConversion(newSelectedConversion);
		}
		
		boolean validateFilter(String filter) {
			if (filter.length() == 0)
				return false;
			return (getAnnotationFilter(filter, true) != null);
		}
		
		boolean validateTarget(String target) {
			if (target.length() == 0)
				return true;
			else if (target.startsWith(CLONE_PREFIX)) {
				String cTarget = target.substring(CLONE_PREFIX.length()).trim();
				return AnnotationUtils.isValidAnnotationType(cTarget);
			}
			else if (REMOVE_DUPLICATES.equals(target) || REMOVE.equals(target) || DELETE.equals(target))
				return true;
			else return AnnotationUtils.isValidAnnotationType(target);
		}
		
		void testFilter() {
			if (this.selectedConversion == -1)
				return;
			this.testFilter(((MarkupConversion) this.conversions.get(this.selectedConversion)).getFilter());
		}
		void testFilter(String filter) {
			QueriableAnnotation testDoc = Gamta.getTestDocument();
			if (testDoc == null)
				return;
			
			McAnnotationFilter af = getAnnotationFilter(filter, true);
			if (af == null)
				return;
			
			Annotation[] data = af.getMatches(testDoc);
			
			AnnotationDisplayDialog add;
			Window top = DialogPanel.getTopWindow();
			if (top instanceof JDialog)
				add = new AnnotationDisplayDialog(((JDialog) top), "Matches of Filter", data, true);
			else if (top instanceof JFrame)
				add = new AnnotationDisplayDialog(((JFrame) top), "Matches of Filter", data, true);
			else add = new AnnotationDisplayDialog(((JFrame) null), "Matches of Filter", data, true);
			add.setLocationRelativeTo(top);
			add.setVisible(true);
		}
		
		private class MarkupConversion {
			int index = 0;
			private boolean isEditing = false;
			
			private String filter;
			private boolean filterDirty = false;
			private int filterInputPressedKey = -1;
			private String target;
			private boolean targetDirty = false;
			private int targetSelectorPressedKey = -1;
			
			JPanel filterPanel = new JPanel(new BorderLayout(), true);
			private JLabel filterDisplay = new JLabel("", JLabel.LEFT);
			private JPanel filterEditor = new JPanel(new BorderLayout(), true);
			private JTextField filterInput = new JTextField("");
			private JButton filterTest = new JButton("Test");
			
			JPanel targetPanel = new JPanel(new BorderLayout(), true);
			private JLabel targetDisplay = new JLabel("", JLabel.LEFT);
			private JComboBox targetSelector;
			
			MarkupConversion(String filter, String target) {
				this.filter = filter;
				this.target = target;
				
				this.filterDisplay.setText(this.filter);
				this.filterDisplay.setOpaque(true);
				this.filterDisplay.setBackground(Color.WHITE);
				this.filterDisplay.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
				this.filterDisplay.addMouseListener(new MouseAdapter() {
					public void mouseClicked(MouseEvent me) {
						if (me.getButton() != MouseEvent.BUTTON1) return;
						if (me.getClickCount() > 1)
							setEditing(true);
						select();
					}
				});
				
				this.filterInput.setText(this.filter);
				this.filterInput.setBorder(BorderFactory.createLoweredBevelBorder());
				this.filterInput.addKeyListener(new KeyAdapter() {
					public void keyPressed(KeyEvent ke) {
						filterInputPressedKey = ke.getKeyCode();
					}
					public void keyReleased(KeyEvent ke) {
						filterInputPressedKey = -1;
					}
					public void keyTyped(KeyEvent ke) {
						if (filterInputPressedKey == KeyEvent.VK_ESCAPE) {
							revertFilter();
							setEditing(false);
						}
						filterDirty = true;
					}
				});
				this.filterInput.addFocusListener(new FocusAdapter() {
					public void focusLost(FocusEvent fe) {
						updateFilter();
					}
				});
				this.filterInput.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						setEditing(false);
					}
				});
				
				this.filterTest.setBorder(BorderFactory.createRaisedBevelBorder());
				this.filterTest.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						testFilter(getFilter());
					}
				});
				
				this.filterEditor.add(this.filterInput, BorderLayout.CENTER);
				this.filterEditor.add(this.filterTest, BorderLayout.EAST);
				
				this.targetDisplay.setText(this.target);
				this.targetDisplay.setOpaque(true);
				this.targetDisplay.setBackground(Color.WHITE);
				this.targetDisplay.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
				this.targetDisplay.addMouseListener(new MouseAdapter() {
					public void mouseClicked(MouseEvent me) {
						if (me.getButton() != MouseEvent.BUTTON1)
							return;
						if (me.getClickCount() > 1)
							setEditing(true);
						select();
					}
				});
				
				this.targetSelector = new JComboBox(targets);
				this.targetSelector.setEditable(true);
				this.targetSelector.setSelectedItem(this.target);
				this.targetSelector.setBorder(BorderFactory.createLoweredBevelBorder());
				this.targetSelector.addItemListener(new ItemListener() {
					public void itemStateChanged(ItemEvent e) {
						targetDirty = true;
					}
				});
				this.targetSelector.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						if (!targetSelector.isVisible()) return;
						else if ((targetSelectorPressedKey != -1) && !targetSelector.isPopupVisible()) {
							if (targetSelectorPressedKey == KeyEvent.VK_ESCAPE) {
								revertTarget();
								setEditing(false);
							}
							else updateTarget();
							if (targetSelectorPressedKey == KeyEvent.VK_ENTER)
								setEditing(false);
						}
					}
				});
				this.targetSelector.addPopupMenuListener(new PopupMenuListener() {
					public void popupMenuCanceled(PopupMenuEvent pme) {}
					public void popupMenuWillBecomeVisible(PopupMenuEvent pme) {}
					public void popupMenuWillBecomeInvisible(PopupMenuEvent pme) {
						targetSelectorPressedKey = -1;
					}
				});
				((JTextComponent) this.targetSelector.getEditor().getEditorComponent()).addKeyListener(new KeyAdapter() {
					public void keyPressed(KeyEvent ke) {
						targetSelectorPressedKey = ke.getKeyCode();
					}
					public void keyReleased(KeyEvent ke) {
						targetSelectorPressedKey = -1;
					}
					public void keyTyped(KeyEvent ke) {
						if (targetSelectorPressedKey == KeyEvent.VK_ESCAPE) {
							revertTarget();
							setEditing(false);
						}
						else targetDirty = true;
					}
				});
				
				this.filterPanel.setPreferredSize(new Dimension(160, 21));
				this.targetPanel.setPreferredSize(new Dimension(160, 21));
				
				this.layoutParts(false);
			}
			String getFilter() {
				this.updateFilter();
				return this.filter;
			}
			private void updateFilter() {
				if (!this.filterDirty)
					return;
				
				this.filter = this.filterInput.getText().trim();
				this.filterDisplay.setText(this.filter);
				if (validateFilter(this.filter)) {
					this.filterDisplay.setBackground(Color.WHITE);
					this.filterInput.setBackground(Color.WHITE);
				}
				else {
					this.filterDisplay.setBackground(Color.ORANGE);
					this.filterInput.setBackground(Color.ORANGE);
				}
				
				dirty = true;
				this.filterDirty = false;
			}
			private void revertFilter() {
				this.filterInput.setText(this.filter);
				this.filterDirty = true;
				this.updateFilter();
			}
			String getTarget() {
				this.updateTarget();
				return this.target;
			}
			private void updateTarget() {
				if (!this.targetDirty) return;
				String target = this.targetSelector.getSelectedItem().toString().trim();
				if (target.length() == 0)
					target = REMOVE;
				
				this.target = target;
				this.targetDisplay.setText(this.target);
				if (validateTarget(this.target)) {
					this.targetDisplay.setBackground(Color.WHITE);
					this.targetSelector.setBackground(Color.WHITE);
					((JTextComponent) this.targetSelector.getEditor().getEditorComponent()).setBackground(Color.WHITE);
				}
				else {
					this.targetDisplay.setBackground(Color.ORANGE);
					this.targetSelector.setBackground(Color.ORANGE);
					((JTextComponent) this.targetSelector.getEditor().getEditorComponent()).setBackground(Color.ORANGE);
				}
				
				dirty = true;
				this.targetDirty = false;
			}
			private void revertTarget() {
				this.targetSelector.setSelectedItem(this.target);
				this.targetDirty = true;
				this.updateTarget();
			}
			void setEditing(boolean editing) {
				if (this.isEditing == editing)
					return;
				if (this.isEditing) {
					this.updateFilter();
					this.updateTarget();
				}
				this.isEditing = editing;
				this.layoutParts(this.isEditing);
				if (!this.isEditing)
					linePanel.requestFocusInWindow();
			}
			void layoutParts(boolean editing) {
				this.filterPanel.removeAll();
				this.targetPanel.removeAll();
				if (editing) {
					this.filterPanel.add(this.filterEditor, BorderLayout.CENTER);
					this.targetPanel.add(this.targetSelector, BorderLayout.CENTER);
				}
				else {
					this.filterPanel.add(this.filterDisplay, BorderLayout.CENTER);
					this.targetPanel.add(this.targetDisplay, BorderLayout.CENTER);
				}
				this.filterPanel.validate();
				this.filterPanel.repaint();
				this.targetPanel.validate();
				this.targetPanel.repaint();
			}
			void select() {
				selectConversion(this.index);
			}
		}
		
		private class AddConversionDialog extends DialogPanel {
			private boolean committed = true;
			private JComboBox filterSelector;
			private JComboBox targetSelector;
			
			AddConversionDialog(String[] externalFilters, String[] targets) {
				super("Add Conversion", true);
				
				this.filterSelector = new JComboBox(externalFilters);
				this.filterSelector.setBorder(BorderFactory.createLoweredBevelBorder());
				this.filterSelector.setEditable(true);
				
				this.targetSelector = new JComboBox(targets);
				this.targetSelector.setEditable(true);
				this.targetSelector.setSelectedItem(REMOVE);
				this.targetSelector.setBorder(BorderFactory.createLoweredBevelBorder());
				
				JPanel inputPanel = new JPanel(new BorderLayout(), true);
				inputPanel.add(this.filterSelector, BorderLayout.CENTER);
				inputPanel.add(this.targetSelector, BorderLayout.EAST);
				
				JButton addConversionButton = new JButton("Add Conversion");
				addConversionButton.setPreferredSize(new Dimension(100, 21));
				addConversionButton.setBorder(BorderFactory.createRaisedBevelBorder());
				addConversionButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						String filter = getFilter();
						if (!validateFilter(filter)) {
							JOptionPane.showMessageDialog(DialogPanel.getTopWindow(), ("'" + filter + "' is not a valid GPath expression or filter name."), "Invalid Filter", JOptionPane.ERROR_MESSAGE);
							return;
						}
						String target = getTarget();
						if (!validateTarget(target)) {
							JOptionPane.showMessageDialog(DialogPanel.getTopWindow(), ("'" + target + "' is not a valid mapping target."), "Invalid Target", JOptionPane.ERROR_MESSAGE);
							return;
						}
						dispose();
					}
				});
				
				JButton testFilterButton = new JButton("Test Filter");
				testFilterButton.setPreferredSize(new Dimension(100, 21));
				testFilterButton.setBorder(BorderFactory.createRaisedBevelBorder());
				testFilterButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						testFilter(getFilter());
					}
				});
				
				JButton cancelButton = new JButton("Cancel");
				cancelButton.setPreferredSize(new Dimension(100, 21));
				cancelButton.setBorder(BorderFactory.createRaisedBevelBorder());
				cancelButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						committed = false;
						dispose();
					}
				});
				
				JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER), true);
				buttonPanel.add(addConversionButton);
				buttonPanel.add(testFilterButton);
				buttonPanel.add(cancelButton);
				
				this.setLayout(new BorderLayout());
				this.add(inputPanel, BorderLayout.NORTH);
				this.add(new JLabel("<HTML>" +
						"Enter the filter to map. The conversion can be changed in the table.<BR>" +
						"Mapping a filter to " + IoTools.prepareForHtml(CLONE_PREFIX) + " &lt;type&gt; will cause annotations being cloned as &lt;type&gt;.<BR>" +
						"Mapping a filter to " + IoTools.prepareForHtml(REMOVE_DUPLICATES) + " will cause duplicate annotation to be removed.<BR>" +
						"Mapping a filter to " + IoTools.prepareForHtml(REMOVE) + " or the empty String will cause theses annotations/attributes to be removed.<BR>" +
						"Mapping a filter to " + IoTools.prepareForHtml(DELETE) + " will cause annotations matching the filter to be deleted." +
						"</HTML>"
						), BorderLayout.CENTER);
				this.add(buttonPanel, BorderLayout.SOUTH);
				this.setSize(500, 180);
				this.setResizable(true);
				this.setLocationRelativeTo(this.getOwner());
			}
			
			String getFilter() {
				return (this.committed ? this.filterSelector.getSelectedItem().toString() : null);
			}
			
			String getTarget() {
				return (this.committed ? this.targetSelector.getSelectedItem().toString() : null);
			}
		}
		
		void setContent(Settings settings) {
			this.conversions.clear();
			this.selectedConversion = -1;
			for (int f = 0; f < settings.size(); f++) {
				String filter = settings.getSetting(MarkupConverter.CONVERSION_FILTER_ATTRIBUTE_NAME + f);
				if (filter != null) {
					String target = settings.getSetting((MarkupConverter.CONVERSION_TARGET_ATTRIBUTE_NAME + f), "");
					if (MarkupConverter.REMOVE_DUPLICATES_CONVERSION.equals(target))
						target = REMOVE_DUPLICATES;
					else if (target == null)
						target = REMOVE;
					else if ("".equals(target))
						target = REMOVE;
					else if (target.startsWith(MarkupConverter.CLONE_CONVERSION))
						target = CLONE_PREFIX + target.substring(MarkupConverter.CLONE_CONVERSION.length()).trim();
					else if (MarkupConverter.DELETE_CONVERSION.equals(target))
						target = DELETE;
					this.addConversion(filter, target);
				}
			}
			this.layoutConversions();
			this.selectConversion(0);
			this.dirty = false;
		}
		
		boolean isDirty() {
			return this.dirty;
		}
		
		Settings getSettings() {
			Settings set = new Settings();
			
			for (int c = 0; c < this.conversions.size(); c++) {
				MarkupConversion mc = ((MarkupConversion) this.conversions.get(c));
				String filter = mc.getFilter();
				String target = mc.getTarget();
				if (this.validateFilter(filter) && this.validateTarget(target)) {
					if (REMOVE_DUPLICATES.equals(target))
						target = MarkupConverter.REMOVE_DUPLICATES_CONVERSION;
					else if (REMOVE.equals(target))
						target = "";
					else if (DELETE.equals(target))
						target = MarkupConverter.DELETE_CONVERSION;
					else if (target.startsWith(CLONE_PREFIX))
						target = MarkupConverter.CLONE_CONVERSION + target.substring(CLONE_PREFIX.length()).trim();
					set.setSetting((MarkupConverter.CONVERSION_FILTER_ATTRIBUTE_NAME + c), filter);
					set.setSetting((MarkupConverter.CONVERSION_TARGET_ATTRIBUTE_NAME + c), target);
				}
			}
			
			return set;
		}
	}
}
