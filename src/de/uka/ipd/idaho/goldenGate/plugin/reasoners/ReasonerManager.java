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
package de.uka.ipd.idaho.goldenGate.plugin.reasoners;


import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import de.uka.ipd.idaho.easyIO.settings.Settings;
import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.util.AnnotationReasoner;
import de.uka.ipd.idaho.gamta.util.swing.AnnotationDisplayDialog;
import de.uka.ipd.idaho.goldenGate.DocumentEditor;
import de.uka.ipd.idaho.goldenGate.GoldenGateConstants;
import de.uka.ipd.idaho.goldenGate.plugin.lists.ListManager;
import de.uka.ipd.idaho.goldenGate.plugins.AbstractAnnotationSourceManager;
import de.uka.ipd.idaho.goldenGate.plugins.AnnotationSource;
import de.uka.ipd.idaho.goldenGate.plugins.AnnotationSourceParameterPanel;
import de.uka.ipd.idaho.goldenGate.plugins.Resource;
import de.uka.ipd.idaho.goldenGate.plugins.ResourceSplashScreen;
import de.uka.ipd.idaho.goldenGate.util.DataListListener;
import de.uka.ipd.idaho.goldenGate.util.DialogPanel;
import de.uka.ipd.idaho.goldenGate.util.ResourceSelector;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * Manager for reasoner annotation sources. A reasoner creates new annotations
 * from ones already existing in a document. This can be done in a series of
 * way:
 * <ul>
 * <li><b>Clone Reasoning</b>: clone existing annottaions of a specific type,
 * creating duplicates from the originals. If the clones are added to the
 * document with a different type, they easily distinguish from the originals.
 * This is useful for preserving the status quo prior to a filtering action.</li>
 * <li><b>Value Reasoning</b>: extract the values of annotations of a given
 * type, and then annotate all other occurrences of the these values throughout
 * the document with the same type. This is useful for automatically annotating
 * occurrences of words and phrases which have already been identified,
 * preventing that any occurrences have been missed.</li>
 * <li><b>Environment Reasoning</b>: extract the environment (tokens to the
 * left and right, number selectable) of annotations of a given source type, and
 * annotate annotations of a given target type with the source type if they have
 * the same environment. This is useful for judging candidate annotations based
 * on their context.</li>
 * <li><b>Value / Environment Reasoning</b>: Combination of Value Reasoning
 * and Environment Reasoning, which are applied alternatingly until no new
 * annotations are created any more by either mode, thus creating something like
 * the transitive hull of existing markup. This is a powerful tool for creating
 * annotations, sort of an ad-hoc bootstrapping procedure that can create many
 * annottaions from a few seeds if (a) the document's text is sufficiently
 * redundant and (b) the document's structure is sufficiently regular.</li>
 * <li><b>Token Fraction Reasoning</b>: extract the tokens of annotations of a
 * given source type, and annotate annotations of a given target type with the
 * source type if their tokens are contained in the source type annotations in
 * to sufficiently high fraction. This is useful for judging candidate
 * annotations based on existing annotations, a little more fuzzy than Value
 * Reasoning.</li>
 * <li><b>Token Sequence Reasoning</b>: extract all sub token sequences
 * (prefixes, infixes, and suffixes) fron the annotations of a given source
 * type, and then annotate all other occurrences of the these token sequences
 * throughout the document with the same type. This is useful for automatically
 * annotating occurrences of words and phrases that are sub sequences of phrases
 * already extracted, e.g. standalone last names if full person names (with
 * first and last name) have already been annotated.</li>
 * <li><b>Annotation Joining</b>: join annotations of a given type to larger
 * annotations of the same type if there is only a maximum number of tokens in
 * between them. This is useful, for instance, for spanning person names over so
 * far unknown middle initials if the first and last name are already annotated.</li>
 * </ul>
 * All configuration can be done ad-hoc in the GoldenGATE Editor. This plugin
 * requires the ListManager plugin to be present in the GoldenGATE Editor as
 * well.<br>
 * <br>
 * In addition to providing reasoners as resources, this plugin also adds a set
 * of function items to the Tools menu, which provide functionality to
 * manipulate markup in an ad-hoc fashion, with the same functionality as
 * offered by reasoners stored as resources.
 * 
 * @author sautter
 */
public class ReasonerManager extends AbstractAnnotationSourceManager {
	
	private static final String FILE_EXTENSION = ".reasoner";
	
	private ListManager listProvider;
	
	public ReasonerManager() {}
	
	/*
	 *  @see de.uka.ipd.idaho.goldenGate.plugins.ResourceManager#init()
	 */
	public void init() {
		this.listProvider = ((ListManager) this.parent.getAnnotationSourceProvider(ListManager.class.getName()));
	}
	
	/*
	 *  @see de.uka.ipd.idaho.goldenGate.plugins.ResourceManager#isOperational()
	 */
	public boolean isOperational() {
		return (super.isOperational() && (this.listProvider != null));
	}
	
	/* 
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AnnotationSourceManager#createAnnotationSource()
	 */
	public String createAnnotationSource() {
		return this.createReasoner(new Settings(), null);
	}
	
	/* 
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AnnotationSourceManager#editAnnotationSource(java.lang.String)
	 */
	public void editAnnotationSource(String name) {
		Settings set = this.loadSettingsResource(name);
		if ((set == null) || set.isEmpty()) return;
		
		EditReasonerDialog erd = new EditReasonerDialog(name, set, this.listProvider, null);
		erd.setVisible(true);
		
		if (erd.isCommitted()) try {
			this.storeSettingsResource(name, erd.getReasoner());
		} catch (IOException ioe) {}
	}
	
	/* 
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AnnotationSourceManager#editAnnotationSources()
	 */
	public void editAnnotationSources() {
		this.editReasoners();
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AnnotationSourceManager#getAnnotationSource(java.lang.String)
	 */
	public AnnotationSource getAnnotationSource(String name) {
		return ((name == null) ? null : new Reasoner(name, this, new Settings()));
	}

	private class Reasoner implements AnnotationSource {
		
		static final String REASONING_MODE_ATTRIBUTE = "REASONING_MODE";
		
		static final String ENVIRONMENT_REASONING_MODE = "ENVIRONMENT_REASONING";
		static final String VALUE_REASONING_MODE = "VALUE_REASONING";
		static final String CLONE_REASONING_MODE = "CLONE_REASONING";
		static final String VALUE_ENVIRONMENT_REASONING_MODE = "VALUE_ENVIRONMENT_REASONING";
		static final String TOKEN_FRACTION_REASONING_MODE = "TOKEN_FRACTION_REASONING";
		static final String TOKEN_SEQUENCE_REASONING_MODE = "TOKEN_SEQUENCE_REASONING";
		static final String JOIN_ANNOTATIONS_REASONING_MODE = "JOIN_ANNOTATION";
		
		static final String SOURCE_ANNOTATION_TYPE_ATTRIBUTE = "SOURCE_TYPE";
		static final String TARGET_TYPE_ATTRIBUTE = "TARGET_TYPE";
		
		static final String LIST_NAME_ATTRIBUTE = "LIST";
		static final String INCLUDE_ATTRIBUTE = "INCLUDE";
		
		static final String MAX_SPAN_TOKENS_ATTRIBUTE = "MAX_SPAN";
		static final String MIN_MATCH_FACTOR_ATTRIBUTE = "MIN_MATCH_FACTOR";
		
		static final String ENVIRONMENT_WIDTH_ATTRIBUTE = "ENVIRONMENT_WIDTH";
		static final String LEFT_ENVIRONMENT_WIDTH_ATTRIBUTE = "LEFT_ENVIRONMENT_WIDTH";
		static final String RIGHT_ENVIRONMENT_WIDTH_ATTRIBUTE = "RIGHT_ENVIRONMENT_WIDTH";
		
		static final String MAX_ROUNDS_ATTRIBUTE = "MAX_ROUNDS";
		
		static final String CASE_SENSITIVE_ATTRIBUTE = "CASE_SENSITIVE";
		
		private String name;
		private ReasonerManager parent;
		private Settings parameters;
		
		Reasoner(String name, ReasonerManager parent, Settings parameters) {
			this.name = name;
			this.parent = parent;
			this.parameters = parameters;
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
			return "Annotation Reasoner";
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.AnnotationSource#getProviderClassName()
		 */
		public String getProviderClassName() {
			return ReasonerManager.class.getName();
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
			Properties theParameters = new Properties(this.parameters.toProperties());
			theParameters.putAll(parameters);
			return this.parent.doReasoning(data, theParameters);
		}
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AnnotationSourceManager#getAnnotatorParameterPanel()
	 */
	public AnnotationSourceParameterPanel getAnnotatorParameterPanel() {
		return new ReasonerParameterPanel(this.listProvider, null);
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AnnotationSourceManager#getAnnotatorParameterPanel(de.uka.ipd.idaho.easyIO.settings.Settings)
	 */
	public AnnotationSourceParameterPanel getAnnotatorParameterPanel(Settings settings) {
		return new ReasonerParameterPanel(this.listProvider, null, settings);
	}
	
	private class ReasonerParameterPanel extends AnnotationSourceParameterPanel {
		
		private JTabbedPane reasoningModeTabs = new JTabbedPane();
		
		//	main parameter panel
		private ReasonerMainParameterPanel mainPanel;
		
		//	additional fields for environmental reasoning
		private EnvironmentReasonerParameterPanel environmentPanel;
		
		//	panel for value reasoning
		private ValueReasonerParameterPanel valuePanel;
		
		//	panel for cloning
		private CloneReasonerParameterPanel clonePanel;
		
		//	dummy panel for value reasoning
		private ValueEnvironmentReasonerParameterPanel valueEnvironmentPanel;
		
		//	additional fields for Token fraction reasoning
		private TokenFractionReasonerParameterPanel tokenFractionPanel;
		
		//	additional fields for Token sequence reasoning
		private TokenSequenceReasonerParameterPanel tokenSequencePanel;
		
		//	additional fields for Annotation joining
		private JoinAnnotationsParameterPanel joinPanel;
		
		private boolean dirty = false;
		
		ReasonerParameterPanel(ListManager listProvider, String[] annotationTypes) {
			this(listProvider, annotationTypes, null);
		}
		
		ReasonerParameterPanel(ListManager listProvider, String[] annotationTypes, Settings settings) {
			super(new BorderLayout());
			
			String[] types = ((annotationTypes == null) ? new String[0] : annotationTypes);
			
			String reasoningMode = ((settings == null) ? Reasoner.VALUE_REASONING_MODE : settings.getSetting(Reasoner.REASONING_MODE_ATTRIBUTE, Reasoner.VALUE_REASONING_MODE));
			
			this.mainPanel = new ReasonerMainParameterPanel(types, settings);
			
			this.environmentPanel = new EnvironmentReasonerParameterPanel(types, settings);
			this.valuePanel = new ValueReasonerParameterPanel();
			this.clonePanel = new CloneReasonerParameterPanel();
			this.valueEnvironmentPanel = new ValueEnvironmentReasonerParameterPanel(types, settings);
			this.tokenFractionPanel = new TokenFractionReasonerParameterPanel(types, settings);
			this.tokenSequencePanel = new TokenSequenceReasonerParameterPanel(listProvider, settings);
			this.joinPanel = new JoinAnnotationsParameterPanel(listProvider, settings);
			
			
			this.reasoningModeTabs.addTab("Environment", null, this.environmentPanel, "Do environmental reasoning (sort of SVM using surrounding Tokens as the features)");
			this.reasoningModeTabs.addTab("Value", null, this.valuePanel, "Do value reasoning (mark all occurrences of values already annotated)");
			this.reasoningModeTabs.addTab("Clone", null, this.clonePanel, "Clone Annotations");
			this.reasoningModeTabs.addTab("Value & Environment", null, this.valueEnvironmentPanel, "Do iterative value / environment reasoning (alternating value and environment reasoning)");
			this.reasoningModeTabs.addTab("Token Fraction", null, this.tokenFractionPanel, "Do Token Fraction reasoning (mark all Annotations of some target type that contain a minimum fraction of Tokens already contained in an Annotation of the base type)");
			this.reasoningModeTabs.addTab("Token Sequence", null, this.tokenSequencePanel, "Do Token Sequence reasoning (mark all sequences of Tokens already contained in an Annotation)");
			this.reasoningModeTabs.addTab("Join Annotations", null, this.joinPanel, "Join neighboring Annotations of the same type");
			
			if (Reasoner.ENVIRONMENT_REASONING_MODE.equals(reasoningMode))
				this.reasoningModeTabs.setSelectedComponent(this.environmentPanel);
			else if (Reasoner.VALUE_REASONING_MODE.equals(reasoningMode))
				this.reasoningModeTabs.setSelectedComponent(this.valuePanel);
			else if (Reasoner.CLONE_REASONING_MODE.equals(reasoningMode))
				this.reasoningModeTabs.setSelectedComponent(this.clonePanel);
			else if (Reasoner.VALUE_ENVIRONMENT_REASONING_MODE.equals(reasoningMode))
				this.reasoningModeTabs.setSelectedComponent(this.valueEnvironmentPanel);
			else if (Reasoner.TOKEN_FRACTION_REASONING_MODE.equals(reasoningMode))
				this.reasoningModeTabs.setSelectedComponent(this.tokenFractionPanel);
			else if (Reasoner.TOKEN_SEQUENCE_REASONING_MODE.equals(reasoningMode))
				this.reasoningModeTabs.setSelectedComponent(this.tokenSequencePanel);
			else if (Reasoner.JOIN_ANNOTATIONS_REASONING_MODE.equals(reasoningMode))
				this.reasoningModeTabs.setSelectedComponent(this.joinPanel);
			
			this.reasoningModeTabs.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent ce) {
					dirty = true;
				}
			});
			
			this.add(this.mainPanel, BorderLayout.NORTH);
			this.add(this.reasoningModeTabs, BorderLayout.CENTER);
		}
		public boolean isDirty() {
			if (this.mainPanel.isDirty()) return true;
			
			Component comp = this.reasoningModeTabs.getSelectedComponent();
			if ((comp != null) && (comp instanceof AnnotationSourceParameterPanel) && ((AnnotationSourceParameterPanel) comp).isDirty()) return true; 
			
			return this.dirty;
		}
		public Settings getSettings() {
			Settings set = this.mainPanel.getSettings();
			
			Component comp = this.reasoningModeTabs.getSelectedComponent();
			if ((comp != null) && (comp instanceof AnnotationSourceParameterPanel))
				set.setSettings(((AnnotationSourceParameterPanel) comp).getSettings()); 
			
			return set;
		}
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AnnotationSourceManager#getResourceNames()
	 */
	public String[] getResourceNames() {
		StringVector regExes = new StringVector();
		regExes.addElementIgnoreDuplicates("<Annotation Reasoner>");
		regExes.addContentIgnoreDuplicates(super.getResourceNames());
		return regExes.toStringArray();
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.ResourceManager#getResourceTypeLabel()
	 */
	public String getResourceTypeLabel() {
		return "Reasoner";
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager#getFileExtension()
	 */
	protected String getFileExtension() {
		return FILE_EXTENSION;
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager#getMainMenuItems()
	 */
	public JMenuItem[] getMainMenuItems() {
		ArrayList collector = new ArrayList();
		collector.addAll(Arrays.asList(this.getMenuItems(new InvokationTargetProvider() {
			public DocumentEditor getFunctionTarget() {
				return parent.getActivePanel();
			}
		})));
		
		if (this.dataProvider.isDataEditable()) {
			collector.add(GoldenGateConstants.MENU_SEPARATOR_ITEM);
			JMenuItem mi;
			
			mi = new JMenuItem("Create");
			mi.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					createReasoner();
				}
			});
			collector.add(mi);
			mi = new JMenuItem("Edit");
			mi.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					editReasoners();
				}
			});
			collector.add(mi);
		}
		
		return ((JMenuItem[]) collector.toArray(new JMenuItem[collector.size()]));
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager#getMainMenuTitle()
	 */
	public String getMainMenuTitle() {
		return "Reasoners";
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager#getToolsMenuLabel()
	 */
	public String getToolsMenuLabel() {
		return "Apply";
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager#getToolsMenuFunctionItems(de.uka.ipd.idaho.goldenGate.GoldenGateConstants.InvokationTargetProvider)
	 */
	public JMenuItem[] getToolsMenuFunctionItems(InvokationTargetProvider targetProvider) {
		return this.getMenuItems(targetProvider);
	}
	
	private JMenuItem[] getMenuItems(final InvokationTargetProvider targetProvider) {
		final String className = this.getClass().getName();
		ArrayList collector = new ArrayList();
		JMenuItem mi;
		
		mi = new JMenuItem("Environment Reasoning");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				DocumentEditor target = targetProvider.getFunctionTarget();
				if (target != null)
					target.applyAnnotationSource(className, Reasoner.ENVIRONMENT_REASONING_MODE);
			}
		});
		collector.add(mi);
		mi = new JMenuItem("Value Reasoning");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				DocumentEditor target = targetProvider.getFunctionTarget();
				if (target != null)
					target.applyAnnotationSource(className, Reasoner.VALUE_REASONING_MODE);
			}
		});
		collector.add(mi);
		mi = new JMenuItem("Clone Annotations");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				DocumentEditor target = targetProvider.getFunctionTarget();
				if (target != null)
					target.applyAnnotationSource(className, Reasoner.CLONE_REASONING_MODE);
			}
		});
		collector.add(mi);
		mi = new JMenuItem("Value / Environment Reasoning");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				DocumentEditor target = targetProvider.getFunctionTarget();
				if (target != null)
					target.applyAnnotationSource(className, Reasoner.VALUE_ENVIRONMENT_REASONING_MODE);
			}
		});
		collector.add(mi);
		mi = new JMenuItem("Token Fraction Reasoning");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				DocumentEditor target = targetProvider.getFunctionTarget();
				if (target != null)
					target.applyAnnotationSource(className, Reasoner.TOKEN_FRACTION_REASONING_MODE);
			}
		});
		collector.add(mi);
		mi = new JMenuItem("Token Sequence Reasoning");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				DocumentEditor target = targetProvider.getFunctionTarget();
				if (target != null)
					target.applyAnnotationSource(className, Reasoner.TOKEN_SEQUENCE_REASONING_MODE);
			}
		});
		collector.add(mi);
		mi = new JMenuItem("Join Annotations");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				DocumentEditor target = targetProvider.getFunctionTarget();
				if (target != null)
					target.applyAnnotationSource(className, Reasoner.JOIN_ANNOTATIONS_REASONING_MODE);
			}
		});
		collector.add(mi);
		
		return ((JMenuItem[]) collector.toArray(new JMenuItem[collector.size()]));
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractAnnotationSourceManager#applyAnnotationSource(java.lang.String, de.uka.ipd.idaho.goldenGate.DocumentEditor, java.util.Properties)
	 */
	public void applyAnnotationSource(String annotatorName, DocumentEditor data, Properties parameters) {
		
		//	check parameters
		if ((data == null) || (parameters == null) || !parameters.containsKey(Resource.INTERACTIVE_PARAMETER)) return;
		
		String[] types = data.getAnnotationTypes();
		Arrays.sort(types, ANNOTATION_TYPE_ORDER);
		
		//	build parameter panel
		AnnotationSourceParameterPanel aspp;
		String reasonerName;
		if (Reasoner.ENVIRONMENT_REASONING_MODE.equals(annotatorName)) {
			aspp = new SpecializedReasonerParameterPanel(new EnvironmentReasonerParameterPanel(types), types);
			reasonerName = "Environment Reasoner";
		}
		else if (Reasoner.VALUE_REASONING_MODE.equals(annotatorName)) {
			aspp = new SpecializedReasonerParameterPanel(new ValueReasonerParameterPanel(), types);
			reasonerName = "Value Reasoner";
		}
		else if (Reasoner.CLONE_REASONING_MODE.equals(annotatorName)) {
			aspp = new SpecializedReasonerParameterPanel(new CloneReasonerParameterPanel(), types);
			reasonerName = "Annotation Cloner";
		}
		else if (Reasoner.VALUE_ENVIRONMENT_REASONING_MODE.equals(annotatorName)) {
			aspp = new SpecializedReasonerParameterPanel(new ValueEnvironmentReasonerParameterPanel(types), types);
			reasonerName = "Value / Environment Reasoner";
		}
		else if (Reasoner.TOKEN_FRACTION_REASONING_MODE.equals(annotatorName)) {
			aspp = new SpecializedReasonerParameterPanel(new TokenFractionReasonerParameterPanel(types), types);
			reasonerName = "Token Fraction Reasoner";
		}
		else if (Reasoner.TOKEN_SEQUENCE_REASONING_MODE.equals(annotatorName)) {
			aspp = new SpecializedReasonerParameterPanel(new TokenSequenceReasonerParameterPanel(this.listProvider), types);
			reasonerName = "Token Sequence Reasoner";
		}
		else if (Reasoner.JOIN_ANNOTATIONS_REASONING_MODE.equals(annotatorName)) {
			aspp = new SpecializedReasonerParameterPanel(new JoinAnnotationsParameterPanel(this.listProvider), types);
			reasonerName = "Annotation Joiner";
		}
		else {
			aspp = new ReasonerParameterPanel(this.listProvider, types);
			reasonerName = "Annotation Reasoner";
		}
		
		//	prompt for details
		ApplyReasonerDialog ard = new ApplyReasonerDialog(("Apply " + reasonerName), aspp);
		ard.setLocationRelativeTo(DialogPanel.getTopWindow());
		ard.setVisible(true);
		
		if (ard.isCommitted()) {
			Settings set = aspp.getSettings();
			Reasoner ras = new Reasoner(reasonerName, this, set);
			ResourceSplashScreen splashScreen = new ResourceSplashScreen((this.getResourceTypeLabel() + " Running ..."), ("Please wait while '" + this.getResourceTypeLabel() + ": " + ras.getName() + "' is annotating the Document ..."));
			
			data.applyAnnotationSource(ras, splashScreen, parameters);
		}
	}
	
	private class ApplyReasonerDialog extends DialogPanel {
		
		private boolean isCommitted = false;
		
		ApplyReasonerDialog(String title, AnnotationSourceParameterPanel parameterPanel) {
			super(title, true);
			
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
			
			JPanel mainButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
			mainButtonPanel.add(commitButton);
			mainButtonPanel.add(abortButton);
			
			//	put the whole stuff together
			this.setLayout(new BorderLayout());
			this.add(parameterPanel, BorderLayout.CENTER);
			this.add(mainButtonPanel, BorderLayout.SOUTH);
			
			this.setResizable(true);
			this.setSize(new Dimension(420, 250));
		}
		
		boolean isCommitted() {
			return this.isCommitted;
		}
		
		void abort() {
			this.dispose();
		}

		void commit() {
			this.isCommitted = true;
			this.dispose();
		}
	}

	private boolean createReasoner() {
		return (this.createReasoner(new Settings(), null) != null);
	}
	
	private boolean cloneReasoner() {
		String selectedName = this.resourceNameList.getSelectedName();
		if (selectedName == null)
			return this.createReasoner();
		else {
			String name = "New " + selectedName;
			return (this.createReasoner(this.loadSettingsResource(selectedName), name) != null);
		}
	}
	
	private String createReasoner(Settings set, String name) {
		CreateReasonerDialog crd = new CreateReasonerDialog(name, set, this.listProvider, null);
		crd.setVisible(true);
		if (crd.isCommitted()) {
			Settings reasoner = crd.getReasoner();
			String reasonerName = crd.getReasonerName();
			if (!reasonerName.endsWith(FILE_EXTENSION)) reasonerName += FILE_EXTENSION;
			try {
				if (this.storeSettingsResource(reasonerName, reasoner)) {
					this.resourceNameList.refresh();
					return reasonerName;
				}
			} catch (IOException e) {}
		}
		return null;
	}
	
	private void editReasoners() {
		final ReasonerEditorPanel[] editor = new ReasonerEditorPanel[1];
		editor[0] = null;
		
		final DialogPanel editDialog = new DialogPanel("Edit Reasoners", true);
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
				createReasoner();
			}
		});
		editButtons.add(button);
		button = new JButton("Clone");
		button.setBorder(BorderFactory.createRaisedBevelBorder());
		button.setPreferredSize(new Dimension(100, 21));
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				cloneReasoner();
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
				editor[0] = new ReasonerEditorPanel(selectedName, set, this.listProvider, null);
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
						editor[0] = new ReasonerEditorPanel(dataName, set, listProvider, null);
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
	
	private class CreateReasonerDialog extends DialogPanel {
		
		private JTextField nameField;
		
		private ReasonerEditorPanel editor;
		private Settings reasoner = null;
		private String reasonerName = null;
		
		CreateReasonerDialog(String name, Settings settings, ListManager listProvider, String[] annotationTypes) {
			super("Create Reasoner", true);
			
			this.nameField = new JTextField((name == null) ? "New Reasoner" : name);
			this.nameField.setBorder(BorderFactory.createLoweredBevelBorder());
			
			//	initialize main buttons
			JButton commitButton = new JButton("Create");
			commitButton.setBorder(BorderFactory.createRaisedBevelBorder());
			commitButton.setPreferredSize(new Dimension(100, 21));
			commitButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					reasoner = editor.getSettings();
					reasonerName = nameField.getText();
					dispose();
				}
			});
			
			JButton testButton = new JButton("Test");
			testButton.setBorder(BorderFactory.createRaisedBevelBorder());
			testButton.setPreferredSize(new Dimension(100, 21));
			testButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					testReasoner(editor.getSettings());
				}
			});
			
			JButton abortButton = new JButton("Cancel");
			abortButton.setBorder(BorderFactory.createRaisedBevelBorder());
			abortButton.setPreferredSize(new Dimension(100, 21));
			abortButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					reasoner = null;
					reasonerName = null;
					dispose();
				}
			});
			
			JPanel mainButtonPanel = new JPanel();
			mainButtonPanel.setLayout(new FlowLayout());
			mainButtonPanel.add(commitButton);
			mainButtonPanel.add(testButton);
			mainButtonPanel.add(abortButton);
			
			//	initialize editor
			this.editor = new ReasonerEditorPanel(name, settings, listProvider, annotationTypes);
			
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
			return (this.reasoner != null);
		}
		
		Settings getReasoner() {
			return this.reasoner;
		}
		
		String getReasonerName() {
			return this.reasonerName;
		}
	}

	private class EditReasonerDialog extends DialogPanel {
		
		private ReasonerEditorPanel editor;
		private Settings reasoner = null;
		
		EditReasonerDialog(String name, Settings settings, ListManager listProvider, String[] annotationTypes) {
			super(("Edit Reasoner '" + name + "'"), true);
			
			//	initialize main buttons
			JButton commitButton = new JButton("OK");
			commitButton.setBorder(BorderFactory.createRaisedBevelBorder());
			commitButton.setPreferredSize(new Dimension(100, 21));
			commitButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					reasoner = editor.getSettings();
					dispose();
				}
			});
			
			JButton testButton = new JButton("Test");
			testButton.setBorder(BorderFactory.createRaisedBevelBorder());
			testButton.setPreferredSize(new Dimension(100, 21));
			testButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					testReasoner(editor.getSettings());
				}
			});
			
			JButton abortButton = new JButton("Cancel");
			abortButton.setBorder(BorderFactory.createRaisedBevelBorder());
			abortButton.setPreferredSize(new Dimension(100, 21));
			abortButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					reasoner = null;
					dispose();
				}
			});
			
			JPanel mainButtonPanel = new JPanel();
			mainButtonPanel.setLayout(new FlowLayout());
			mainButtonPanel.add(commitButton);
			mainButtonPanel.add(testButton);
			mainButtonPanel.add(abortButton);
			
			//	initialize editor
			this.editor = new ReasonerEditorPanel(name, settings, listProvider, annotationTypes);
			
			//	put the whole stuff together
			this.setLayout(new BorderLayout());
			this.add(this.editor, BorderLayout.CENTER);
			this.add(mainButtonPanel, BorderLayout.SOUTH);
			
			this.setResizable(true);
			this.setSize(new Dimension(600, 400));
			this.setLocationRelativeTo(this.getOwner());
		}
		
		boolean isCommitted() {
			return (this.reasoner != null);
		}
		
		Settings getReasoner() {
			return this.reasoner;
		}
	}
	
	private class ReasonerEditorPanel extends JPanel {
		
		private String name;
		
		private ReasonerParameterPanel editor;
		
		ReasonerEditorPanel(String name, Settings reasoner, ListManager listProvider, String[] annotationTypes) {
			super(new BorderLayout(), true);
			this.name = name;
			this.editor = new ReasonerParameterPanel(listProvider, annotationTypes, reasoner);
			this.add(this.editor, BorderLayout.CENTER);
		}

		boolean isDirty() {
			return this.editor.isDirty();
		}
		
		Settings getSettings() {
			return this.editor.getSettings();
		}
	}
	
	/**
	 * composit panel of main parameter panel (source type and case sensitivity)
	 * and reasoning mode specific parameter panel
	 * 
	 * @author sautter
	 */
	private class SpecializedReasonerParameterPanel extends AnnotationSourceParameterPanel {
		
		//	main parameter panel
		private ReasonerMainParameterPanel mainPanel;
		
		//	additional fields
		private AnnotationSourceParameterPanel specialPanel;
		
		SpecializedReasonerParameterPanel(AnnotationSourceParameterPanel specialPanel, String[] annotationTypes) {
			this(specialPanel, annotationTypes, null);
		}
		
		SpecializedReasonerParameterPanel(AnnotationSourceParameterPanel specialPanel, String[] annotationTypes, Settings settings) {
			super(new BorderLayout());
			this.specialPanel = specialPanel;
			
			String[] types = ((annotationTypes == null) ? new String[0] : annotationTypes);
			
			this.mainPanel = new ReasonerMainParameterPanel(types, settings);
			
			this.add(this.mainPanel, BorderLayout.NORTH);
			this.add(this.specialPanel, BorderLayout.CENTER);
		}
		public boolean isDirty() {
			return (this.mainPanel.isDirty() || this.specialPanel.isDirty());
		}
		public Settings getSettings() {
			Settings set = this.mainPanel.getSettings();
			set.setSettings(this.specialPanel.getSettings()); 
			return set;
		}
	}
	
	/**
	 * panel for source type and case sensitivity
	 * 
	 * @author sautter
	 */
	private class ReasonerMainParameterPanel extends AnnotationSourceParameterPanel {
		
		private JComboBox annotationType;
		private JCheckBox caseSensitive;
		
		private boolean dirty = false;
		
//		ReasonerMainParameterPanel(String[] annotationTypes) {
//			this(annotationTypes, null);
//		}
//		
		ReasonerMainParameterPanel(String[] annotationTypes, Settings settings) {
			super(new GridBagLayout());
			
			String[] types = ((annotationTypes == null) ? new String[0] : annotationTypes);
			this.annotationType = new JComboBox(types);
			this.annotationType.setBorder(BorderFactory.createLoweredBevelBorder());
			this.annotationType.setEditable(true);
			if (settings != null) this.annotationType.setSelectedItem(settings.getSetting(Reasoner.SOURCE_ANNOTATION_TYPE_ATTRIBUTE, ""));
			this.annotationType.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent e) {
					dirty = true;
				}
			});
			
			this.caseSensitive = new JCheckBox("Case Sensitive", false);
			if (settings != null) this.caseSensitive.setSelected(Reasoner.CASE_SENSITIVE_ATTRIBUTE.equals(settings.getSetting(Reasoner.CASE_SENSITIVE_ATTRIBUTE, null)));
			this.caseSensitive.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent ie) {
					dirty = true;
				}
			});
			
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridy = 0;
			gbc.weightx = 1;
			gbc.weighty = 0;
			gbc.gridwidth = 1;
			gbc.gridheight = 1;
			gbc.insets.top = 3;
			gbc.insets.left = 5;
			gbc.insets.right = 5;
			gbc.insets.bottom = 3;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			
			gbc.gridx = 0;
			this.add(new JLabel("Source Type"), gbc.clone());
			gbc.gridx = 1;
			this.add(this.annotationType, gbc.clone());
			
			gbc.gridy ++;
			gbc.gridx = 0;
			gbc.gridwidth = 2;
			this.add(this.caseSensitive, gbc.clone());
		}
		public boolean isDirty() {
			return this.dirty;
		}
		public Settings getSettings() {
			Settings set = new Settings();
			
			Object o = this.annotationType.getSelectedItem();
			if ((o != null) && (o instanceof String))
				set.setSetting(Reasoner.SOURCE_ANNOTATION_TYPE_ATTRIBUTE, o.toString());
			
			if (this.caseSensitive.isSelected())
				set.setSetting(Reasoner.CASE_SENSITIVE_ATTRIBUTE, Reasoner.CASE_SENSITIVE_ATTRIBUTE);
			
			return set;
		}
	}
	
	private class ValueReasonerParameterPanel extends AnnotationSourceParameterPanel {
		ValueReasonerParameterPanel() {
			super(new BorderLayout());
		}
		public boolean isDirty() {
			return false;
		}
		public Settings getSettings() {
			Settings set = new Settings();
			set.setSetting(Reasoner.REASONING_MODE_ATTRIBUTE, Reasoner.VALUE_REASONING_MODE);
			return set;
		}
	}

	private class CloneReasonerParameterPanel extends AnnotationSourceParameterPanel {
		CloneReasonerParameterPanel() {
			super(new BorderLayout());
		}
		public boolean isDirty() {
			return false;
		}
		public Settings getSettings() {
			Settings set = new Settings();
			set.setSetting(Reasoner.REASONING_MODE_ATTRIBUTE, Reasoner.CLONE_REASONING_MODE);
			return set;
		}
	}
	
	private static final String SYMMETRIC_ENVIRONMENT = "Symmetric Environment";
	private static final String ASYMMETRIC_ENVIRONMENT = "Asymmetric Environment";
	private static final String SHIFTING_ENVIRONMENT = "Shifting Environment";
	private static final String[] ENVIRONMENT_TYPES = {SYMMETRIC_ENVIRONMENT, ASYMMETRIC_ENVIRONMENT, SHIFTING_ENVIRONMENT};
	
	private static final String[] ENVIRONMENT_WIDTHS = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "10"};
	private static final String[] SIDE_ENVIRONMENT_WIDTHS = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10"};
	
	private class EnvironmentReasonerParameterPanel extends AnnotationSourceParameterPanel {
		
		private JComboBox environmentType;
		
		private JComboBox environmentWidthInput = new JComboBox(ENVIRONMENT_WIDTHS);
		private JComboBox leftEnvironmentWidthInput = new JComboBox(SIDE_ENVIRONMENT_WIDTHS);
		private JComboBox rightEnvironmentWidthInput = new JComboBox(SIDE_ENVIRONMENT_WIDTHS);
		
		private JComboBox targetAnnotationType;
		
		private boolean dirty = false;
		
		EnvironmentReasonerParameterPanel(String[] annotationTypes) {
			this(annotationTypes, null);
		}
		
		EnvironmentReasonerParameterPanel(String[] annotationTypes, Settings settings) {
			super(new GridBagLayout());
			
			String[] types = ((annotationTypes == null) ? new String[0] : annotationTypes);
			
			//	input fields for environmental reasoning
			if (settings == null)
				this.environmentWidthInput.setSelectedItem("3");
			else this.environmentWidthInput.setSelectedItem(settings.getSetting(Reasoner.ENVIRONMENT_WIDTH_ATTRIBUTE, "3"));
			this.environmentWidthInput.setEditable(false);
			this.environmentWidthInput.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent ie) {
					dirty = true;
				}
			});
			
			if (settings == null)
				this.leftEnvironmentWidthInput.setSelectedItem("3");
			else this.leftEnvironmentWidthInput.setSelectedItem(settings.getSetting(Reasoner.LEFT_ENVIRONMENT_WIDTH_ATTRIBUTE, "3"));
			this.leftEnvironmentWidthInput.setEditable(false);
			this.leftEnvironmentWidthInput.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent ie) {
					dirty = true;
				}
			});
			
			if (settings == null)
				this.rightEnvironmentWidthInput.setSelectedItem("3");
			else this.rightEnvironmentWidthInput.setSelectedItem(settings.getSetting(Reasoner.RIGHT_ENVIRONMENT_WIDTH_ATTRIBUTE, "3"));
			this.rightEnvironmentWidthInput.setEditable(false);
			this.rightEnvironmentWidthInput.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent ie) {
					dirty = true;
				}
			});
			
			this.environmentType = new JComboBox(ENVIRONMENT_TYPES);
			if (settings == null)
				this.environmentType.setSelectedItem(SYMMETRIC_ENVIRONMENT);
			else {
				if (settings.containsKey(Reasoner.LEFT_ENVIRONMENT_WIDTH_ATTRIBUTE) && settings.containsKey(Reasoner.RIGHT_ENVIRONMENT_WIDTH_ATTRIBUTE)) {
					if (settings.containsKey(Reasoner.ENVIRONMENT_WIDTH_ATTRIBUTE))
						this.environmentType.setSelectedItem(SHIFTING_ENVIRONMENT);
					else this.environmentType.setSelectedItem(ASYMMETRIC_ENVIRONMENT);
				}
				else this.environmentType.setSelectedItem(SYMMETRIC_ENVIRONMENT);
			}
			this.environmentTypeChanged(this.environmentType.getSelectedItem());
			this.environmentType.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent ie) {
					dirty = true;
					environmentTypeChanged(environmentType.getSelectedItem());
				}
			});
			
			this.targetAnnotationType = new JComboBox(types);
			this.targetAnnotationType.setBorder(BorderFactory.createLoweredBevelBorder());
			this.targetAnnotationType.setEditable(true);
			if (settings != null) this.targetAnnotationType.setSelectedItem(settings.getSetting(Reasoner.TARGET_TYPE_ATTRIBUTE, ""));
			this.targetAnnotationType.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent e) {
					dirty = true;
				}
			});
			
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.weightx = 1;
			gbc.weighty = 0;
			gbc.gridwidth = 1;
			gbc.gridheight = 1;
			gbc.insets.top = 3;
			gbc.insets.left = 5;
			gbc.insets.right = 5;
			gbc.insets.bottom = 3;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			
			gbc.gridy = 0;
			gbc.gridx = 0;
			gbc.gridwidth = 2;
			this.add(this.environmentType, gbc.clone());
			gbc.gridx = 2;
			gbc.gridwidth = 1;
			this.add(new JLabel("Env Tokens"), gbc.clone());
			gbc.gridx = 3;
			this.add(this.environmentWidthInput, gbc.clone());
			
			gbc.gridy = 1;
			gbc.gridx = 0;
			this.add(new JLabel("Left Tokens"), gbc.clone());
			gbc.gridx = 1;
			this.add(this.leftEnvironmentWidthInput, gbc.clone());
			gbc.gridx = 2;
			this.add(new JLabel("Right Tokens"), gbc.clone());
			gbc.gridx = 3;
			this.add(this.rightEnvironmentWidthInput, gbc.clone());
			
			gbc.gridy = 2;
			gbc.gridwidth = 2;
			gbc.gridx = 0;
			this.add(new JLabel("Target Type"), gbc.clone());
			gbc.gridx = 2;
			this.add(this.targetAnnotationType, gbc.clone());
		}
		void environmentTypeChanged(Object envType) {
			if (ASYMMETRIC_ENVIRONMENT.equals(envType)) {
				this.environmentWidthInput.setEnabled(false);
				this.leftEnvironmentWidthInput.setEnabled(true);
				this.rightEnvironmentWidthInput.setEnabled(true);
			}
			else if (SHIFTING_ENVIRONMENT.equals(envType)) {
				this.environmentWidthInput.setEnabled(true);
				this.leftEnvironmentWidthInput.setEnabled(true);
				this.rightEnvironmentWidthInput.setEnabled(true);
			}
			else {
				this.environmentWidthInput.setEnabled(true);
				this.leftEnvironmentWidthInput.setEnabled(false);
				this.rightEnvironmentWidthInput.setEnabled(false);
			}
		}
		public boolean isDirty() {
			return this.dirty;
		}
		public Settings getSettings() {
			Settings set = new Settings();
			
			set.setSetting(Reasoner.REASONING_MODE_ATTRIBUTE, Reasoner.ENVIRONMENT_REASONING_MODE);
			
			Object o = this.targetAnnotationType.getSelectedItem();
			if ((o != null) && (o instanceof String))
				set.setSetting(Reasoner.TARGET_TYPE_ATTRIBUTE, o.toString());
			
			Object envType = this.environmentType.getSelectedItem();
			if (ASYMMETRIC_ENVIRONMENT.equals(envType)) {
				set.setSetting(Reasoner.LEFT_ENVIRONMENT_WIDTH_ATTRIBUTE, this.leftEnvironmentWidthInput.getSelectedItem().toString());
				set.setSetting(Reasoner.RIGHT_ENVIRONMENT_WIDTH_ATTRIBUTE, this.rightEnvironmentWidthInput.getSelectedItem().toString());
			}
			else if (SHIFTING_ENVIRONMENT.equals(envType)) {
				set.setSetting(Reasoner.ENVIRONMENT_WIDTH_ATTRIBUTE, this.environmentWidthInput.getSelectedItem().toString());
				set.setSetting(Reasoner.LEFT_ENVIRONMENT_WIDTH_ATTRIBUTE, this.leftEnvironmentWidthInput.getSelectedItem().toString());
				set.setSetting(Reasoner.RIGHT_ENVIRONMENT_WIDTH_ATTRIBUTE, this.rightEnvironmentWidthInput.getSelectedItem().toString());
			}
			else set.setSetting(Reasoner.ENVIRONMENT_WIDTH_ATTRIBUTE, this.environmentWidthInput.getSelectedItem().toString());
			
			return set;
		}
	}

	private class ValueEnvironmentReasonerParameterPanel extends AnnotationSourceParameterPanel {
		
		private JComboBox environmentType;
		
		private JComboBox environmentWidthInput = new JComboBox(ENVIRONMENT_WIDTHS);
		private JComboBox leftEnvironmentWidthInput = new JComboBox(SIDE_ENVIRONMENT_WIDTHS);
		private JComboBox rightEnvironmentWidthInput = new JComboBox(SIDE_ENVIRONMENT_WIDTHS);
		
		private JTextField maxRoundsInput;
		
		private JComboBox targetAnnotationType;
		
		private boolean dirty = false;
		
		ValueEnvironmentReasonerParameterPanel(String[] annotationTypes) {
			this(annotationTypes, null);
		}
		
		ValueEnvironmentReasonerParameterPanel(String[] annotationTypes, Settings settings) {
			super(new GridBagLayout());
			
			String[] types = ((annotationTypes == null) ? new String[0] : annotationTypes);
			
			//	input fields for environmental reasoning
			if (settings == null)
				this.environmentWidthInput.setSelectedItem("3");
			else this.environmentWidthInput.setSelectedItem(settings.getSetting(Reasoner.ENVIRONMENT_WIDTH_ATTRIBUTE, "3"));
			this.environmentWidthInput.setEditable(false);
			this.environmentWidthInput.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent ie) {
					dirty = true;
				}
			});
			
			if (settings == null)
				this.leftEnvironmentWidthInput.setSelectedItem("3");
			else this.leftEnvironmentWidthInput.setSelectedItem(settings.getSetting(Reasoner.LEFT_ENVIRONMENT_WIDTH_ATTRIBUTE, "3"));
			this.leftEnvironmentWidthInput.setEditable(false);
			this.leftEnvironmentWidthInput.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent ie) {
					dirty = true;
				}
			});
			
			if (settings == null)
				this.rightEnvironmentWidthInput.setSelectedItem("3");
			else this.rightEnvironmentWidthInput.setSelectedItem(settings.getSetting(Reasoner.RIGHT_ENVIRONMENT_WIDTH_ATTRIBUTE, "3"));
			this.rightEnvironmentWidthInput.setEditable(false);
			this.rightEnvironmentWidthInput.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent ie) {
					dirty = true;
				}
			});
			
			this.maxRoundsInput = new JTextField("0");
			if (settings != null) this.maxRoundsInput.setText(settings.getSetting(Reasoner.MAX_ROUNDS_ATTRIBUTE, "0"));
			this.maxRoundsInput.getDocument().addDocumentListener(new DocumentListener() {
				public void changedUpdate(DocumentEvent de) {}
				public void insertUpdate(DocumentEvent de) {
					dirty = true;
				}
				public void removeUpdate(DocumentEvent de) {
					dirty = true;
				}
			});
			
			this.environmentType = new JComboBox(ENVIRONMENT_TYPES);
			if (settings == null)
				this.environmentType.setSelectedItem(SYMMETRIC_ENVIRONMENT);
			else {
				if (settings.containsKey(Reasoner.LEFT_ENVIRONMENT_WIDTH_ATTRIBUTE) && settings.containsKey(Reasoner.RIGHT_ENVIRONMENT_WIDTH_ATTRIBUTE)) {
					if (settings.containsKey(Reasoner.ENVIRONMENT_WIDTH_ATTRIBUTE))
						this.environmentType.setSelectedItem(SHIFTING_ENVIRONMENT);
					else this.environmentType.setSelectedItem(ASYMMETRIC_ENVIRONMENT);
				}
				else this.environmentType.setSelectedItem(SYMMETRIC_ENVIRONMENT);
			}
			this.environmentTypeChanged(this.environmentType.getSelectedItem());
			this.environmentType.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent ie) {
					dirty = true;
					environmentTypeChanged(environmentType.getSelectedItem());
				}
			});
			
			this.targetAnnotationType = new JComboBox(types);
			this.targetAnnotationType.setBorder(BorderFactory.createLoweredBevelBorder());
			this.targetAnnotationType.setEditable(true);
			if (settings != null) this.targetAnnotationType.setSelectedItem(settings.getSetting(Reasoner.TARGET_TYPE_ATTRIBUTE, ""));
			this.targetAnnotationType.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent e) {
					dirty = true;
				}
			});
			
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.weightx = 1;
			gbc.weighty = 0;
			gbc.gridwidth = 1;
			gbc.gridheight = 1;
			gbc.insets.top = 3;
			gbc.insets.left = 5;
			gbc.insets.right = 5;
			gbc.insets.bottom = 3;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			
			gbc.gridy = 0;
			gbc.gridx = 0;
			gbc.gridwidth = 2;
			this.add(this.environmentType, gbc.clone());
			gbc.gridx = 2;
			gbc.gridwidth = 1;
			this.add(new JLabel("Env Tokens"), gbc.clone());
			gbc.gridx = 3;
			this.add(this.environmentWidthInput, gbc.clone());
			
			gbc.gridy = 1;
			gbc.gridx = 0;
			gbc.gridwidth = 1;
			this.add(new JLabel("Left Tokens"), gbc.clone());
			gbc.gridx = 1;
			this.add(this.leftEnvironmentWidthInput, gbc.clone());
			gbc.gridx = 2;
			this.add(new JLabel("Right Tokens"), gbc.clone());
			gbc.gridx = 3;
			this.add(this.rightEnvironmentWidthInput, gbc.clone());
			
			gbc.gridy = 2;
			gbc.gridx = 0;
			gbc.gridwidth = 1;
			this.add(new JLabel("Rounds"), gbc.clone());
			gbc.gridx = 1;
			this.add(this.maxRoundsInput, gbc.clone());
			gbc.gridx = 2;
			this.add(new JLabel("Target Type"), gbc.clone());
			gbc.gridx = 3;
			this.add(this.targetAnnotationType, gbc.clone());
		}
		void environmentTypeChanged(Object envType) {
			if (ASYMMETRIC_ENVIRONMENT.equals(envType)) {
				this.environmentWidthInput.setEnabled(false);
				this.leftEnvironmentWidthInput.setEnabled(true);
				this.rightEnvironmentWidthInput.setEnabled(true);
			}
			else if (SHIFTING_ENVIRONMENT.equals(envType)) {
				this.environmentWidthInput.setEnabled(true);
				this.leftEnvironmentWidthInput.setEnabled(true);
				this.rightEnvironmentWidthInput.setEnabled(true);
			}
			else {
				this.environmentWidthInput.setEnabled(true);
				this.leftEnvironmentWidthInput.setEnabled(false);
				this.rightEnvironmentWidthInput.setEnabled(false);
			}
		}
		public boolean isDirty() {
			return this.dirty;
		}
		public Settings getSettings() {
			Settings set = new Settings();
			
			set.setSetting(Reasoner.REASONING_MODE_ATTRIBUTE, Reasoner.VALUE_ENVIRONMENT_REASONING_MODE);
			
			Object o = this.targetAnnotationType.getSelectedItem();
			if ((o != null) && (o instanceof String))
				set.setSetting(Reasoner.TARGET_TYPE_ATTRIBUTE, o.toString());
			
			Object envType = this.environmentType.getSelectedItem();
			if (ASYMMETRIC_ENVIRONMENT.equals(envType)) {
				set.setSetting(Reasoner.LEFT_ENVIRONMENT_WIDTH_ATTRIBUTE, this.leftEnvironmentWidthInput.getSelectedItem().toString());
				set.setSetting(Reasoner.RIGHT_ENVIRONMENT_WIDTH_ATTRIBUTE, this.rightEnvironmentWidthInput.getSelectedItem().toString());
			}
			else if (SHIFTING_ENVIRONMENT.equals(envType)) {
				set.setSetting(Reasoner.ENVIRONMENT_WIDTH_ATTRIBUTE, this.environmentWidthInput.getSelectedItem().toString());
				set.setSetting(Reasoner.LEFT_ENVIRONMENT_WIDTH_ATTRIBUTE, this.leftEnvironmentWidthInput.getSelectedItem().toString());
				set.setSetting(Reasoner.RIGHT_ENVIRONMENT_WIDTH_ATTRIBUTE, this.rightEnvironmentWidthInput.getSelectedItem().toString());
			}
			else set.setSetting(Reasoner.ENVIRONMENT_WIDTH_ATTRIBUTE, this.environmentWidthInput.getSelectedItem().toString());
			
			set.setSetting(Reasoner.MAX_ROUNDS_ATTRIBUTE, this.maxRoundsInput.getText());
			
			return set;
		}
	}

	private class TokenFractionReasonerParameterPanel extends AnnotationSourceParameterPanel {
		
		private JComboBox targetAnnotationType;
		private JTextField minMatchFactorInput;
		
		private boolean dirty = false;
		
		TokenFractionReasonerParameterPanel(String[] annotationTypes) {
			this(annotationTypes, null);
		}
		
		TokenFractionReasonerParameterPanel(String[] annotationTypes, Settings settings) {
			super(new GridBagLayout());
			
			String[] types = ((annotationTypes == null) ? new String[0] : annotationTypes);
			
			//	additional input fields for factor Token based reasoning
			this.targetAnnotationType = new JComboBox(types);
			this.targetAnnotationType.setBorder(BorderFactory.createLoweredBevelBorder());
			this.targetAnnotationType.setEditable(true);
			if (settings != null) this.targetAnnotationType.setSelectedItem(settings.getSetting(Reasoner.TARGET_TYPE_ATTRIBUTE, ""));
			this.targetAnnotationType.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent e) {
					dirty = true;
				}
			});
			
			this.minMatchFactorInput = new JTextField("100");
			if (settings != null) {
				float factor = 1;
				try {
					factor = Float.parseFloat(settings.getSetting(Reasoner.MIN_MATCH_FACTOR_ATTRIBUTE, "1"));
				} catch (Exception e) {}
				this.minMatchFactorInput.setText("" + ((int) (factor * 100)));
			}
			this.minMatchFactorInput.getDocument().addDocumentListener(new DocumentListener() {
				public void changedUpdate(DocumentEvent de) {}
				public void insertUpdate(DocumentEvent de) {
					dirty = true;
				}
				public void removeUpdate(DocumentEvent de) {
					dirty = true;
				}
			});
			
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.weightx = 1;
			gbc.weighty = 0;
			gbc.gridwidth = 1;
			gbc.gridheight = 1;
			gbc.insets.top = 3;
			gbc.insets.left = 5;
			gbc.insets.right = 5;
			gbc.insets.bottom = 3;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			
			gbc.gridy = 0;
			gbc.gridx = 0;
			this.add(new JLabel("Min % Known Tokens"), gbc.clone());
			gbc.gridx = 1;
			this.add(this.minMatchFactorInput, gbc.clone());
			
			gbc.gridy = 1;
			gbc.gridx = 0;
			this.add(new JLabel("Target Type"), gbc.clone());
			gbc.gridx = 1;
			this.add(this.targetAnnotationType, gbc.clone());
		}
		public boolean isDirty() {
			return this.dirty;
		}
		public Settings getSettings() {
			Settings set = new Settings();
			
			set.setSetting(Reasoner.REASONING_MODE_ATTRIBUTE, Reasoner.TOKEN_FRACTION_REASONING_MODE);
			
			Object o = this.targetAnnotationType.getSelectedItem();
			if ((o != null) && (o instanceof String))
				set.setSetting(Reasoner.TARGET_TYPE_ATTRIBUTE, o.toString());
			
			int factor = 100;
			try {
				factor = Integer.parseInt(this.minMatchFactorInput.getText());
			} catch (Exception e) {}
			set.setSetting(Reasoner.MIN_MATCH_FACTOR_ATTRIBUTE, ("" + (((float) factor) / 100)));
			
			return set;
		}
	}

	private class TokenSequenceReasonerParameterPanel extends AnnotationSourceParameterPanel {
		
		private JTextField maxSpanTokensInput;
		private ResourceSelector inExclude;
		private String inExcludeInit = null;
		private JCheckBox include;
		
		private boolean dirty = false;
		
		TokenSequenceReasonerParameterPanel(ListManager listProvider) {
			this(listProvider, null);
		}
		
		TokenSequenceReasonerParameterPanel(ListManager listProvider, Settings settings) {
			super(new GridBagLayout());
			
			this.maxSpanTokensInput = new JTextField("0");
			if (settings != null) this.maxSpanTokensInput.setText(settings.getSetting(Reasoner.MAX_SPAN_TOKENS_ATTRIBUTE, "0"));
			this.maxSpanTokensInput.getDocument().addDocumentListener(new DocumentListener() {
				public void changedUpdate(DocumentEvent de) {}
				public void insertUpdate(DocumentEvent de) {
					dirty = true;
				}
				public void removeUpdate(DocumentEvent de) {
					dirty = true;
				}
			});
			
			this.inExcludeInit = ((settings == null) ? null : settings.getSetting(Reasoner.LIST_NAME_ATTRIBUTE));
			this.inExclude = listProvider.getSelector("Token List", this.inExcludeInit);
			
			this.include = new JCheckBox("Use List for Inclusion", false);
			if (settings != null) this.include.setSelected(Reasoner.INCLUDE_ATTRIBUTE.equals(settings.getSetting(Reasoner.INCLUDE_ATTRIBUTE, null)));
			this.include.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent ie) {
					dirty = true;
				}
			});
			
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.weightx = 1;
			gbc.weighty = 0;
			gbc.gridwidth = 1;
			gbc.gridheight = 1;
			gbc.insets.top = 3;
			gbc.insets.left = 5;
			gbc.insets.right = 5;
			gbc.insets.bottom = 3;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			
			gbc.gridy = 0;
			gbc.gridx = 0;
			this.add(new JLabel("Max Tokens to Span"), gbc.clone());
			gbc.gridx = 1;
			this.add(this.maxSpanTokensInput, gbc.clone());
			
			gbc.gridy = 1;
			gbc.gridx = 0;
			gbc.gridwidth = 2;
			this.add(this.inExclude, gbc.clone());
			
			gbc.gridy = 2;
			this.add(this.include, gbc.clone());
		}
		public boolean isDirty() {
			String tokenInExcludeListName = this.inExclude.getSelectedResourceName();
			if (this.inExcludeInit == null) {
				if (tokenInExcludeListName != null) return true; 
			} else if (!this.inExcludeInit.equals(tokenInExcludeListName)) return true;
			
			return this.dirty;
		}
		public Settings getSettings() {
			Settings set = new Settings();
			
			set.setSetting(Reasoner.REASONING_MODE_ATTRIBUTE, Reasoner.TOKEN_SEQUENCE_REASONING_MODE);
			
			int spanTokens = 0;
			try {
				spanTokens = Integer.parseInt(this.maxSpanTokensInput.getText());
			} catch (Exception e) {}
			
			if (spanTokens != 0) {
				set.setSetting(Reasoner.MAX_SPAN_TOKENS_ATTRIBUTE, ("" + spanTokens));
				
				String listName = this.inExclude.getSelectedResourceName();
				if (listName != null) {
					set.setSetting(Reasoner.LIST_NAME_ATTRIBUTE, listName);
					
					if (this.include.isSelected())
						set.setSetting(Reasoner.INCLUDE_ATTRIBUTE, Reasoner.INCLUDE_ATTRIBUTE);
				}
			}
			
			return set;
		}
	}

	private class JoinAnnotationsParameterPanel extends AnnotationSourceParameterPanel {
		
		private JTextField maxSpanTokensInput;
		private ResourceSelector inExclude;
		private String inExcludeInit = null;
		private JCheckBox include;
		
		private boolean dirty = false;
		
		JoinAnnotationsParameterPanel(ListManager listProvider) {
			this(listProvider, null);
		}
		
		JoinAnnotationsParameterPanel(ListManager listProvider, Settings settings) {
			super(new GridBagLayout());
			
			this.maxSpanTokensInput = new JTextField("0");
			if (settings != null) this.maxSpanTokensInput.setText(settings.getSetting(Reasoner.MAX_SPAN_TOKENS_ATTRIBUTE, "0"));
			this.maxSpanTokensInput.getDocument().addDocumentListener(new DocumentListener() {
				public void changedUpdate(DocumentEvent de) {}
				public void insertUpdate(DocumentEvent de) {
					dirty = true;
				}
				public void removeUpdate(DocumentEvent de) {
					dirty = true;
				}
			});
			
			this.inExcludeInit = ((settings == null) ? null : settings.getSetting(Reasoner.LIST_NAME_ATTRIBUTE));
			this.inExclude = listProvider.getSelector("Token List", this.inExcludeInit);
			
			this.include = new JCheckBox("Use List for Inclusion", false);
			if (settings != null) this.include.setSelected(Reasoner.INCLUDE_ATTRIBUTE.equals(settings.getSetting(Reasoner.INCLUDE_ATTRIBUTE, null)));
			this.include.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent ie) {
					dirty = true;
				}
			});
			
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.weightx = 1;
			gbc.weighty = 0;
			gbc.gridwidth = 1;
			gbc.gridheight = 1;
			gbc.insets.top = 3;
			gbc.insets.left = 5;
			gbc.insets.right = 5;
			gbc.insets.bottom = 3;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			
			gbc.gridy = 0;
			gbc.gridx = 0;
			this.add(new JLabel("Max Tokens to Span"), gbc.clone());
			gbc.gridx = 1;
			this.add(this.maxSpanTokensInput, gbc.clone());
			
			gbc.gridy = 1;
			gbc.gridx = 0;
			gbc.gridwidth = 2;
			this.add(this.inExclude, gbc.clone());
			gbc.gridy = 2;
			this.add(this.include, gbc.clone());
		}
		public boolean isDirty() {
			String joinInExcludeListName = this.inExclude.getSelectedResourceName();
			if (this.inExcludeInit == null) {
				if (joinInExcludeListName != null) return true; 
			}
			else if (!this.inExcludeInit.equals(joinInExcludeListName)) return true;
			
			return this.dirty;
		}
		public Settings getSettings() {
			Settings set = new Settings();
			
			set.setSetting(Reasoner.REASONING_MODE_ATTRIBUTE, Reasoner.JOIN_ANNOTATIONS_REASONING_MODE);
			
			int spanTokens = 0;
			try {
				spanTokens = Integer.parseInt(this.maxSpanTokensInput.getText());
			} catch (Exception e) {}
			
			if (spanTokens != 0) {
				set.setSetting(Reasoner.MAX_SPAN_TOKENS_ATTRIBUTE, ("" + spanTokens));
				
				String listName = this.inExclude.getSelectedResourceName();
				if (listName != null) {
					set.setSetting(Reasoner.LIST_NAME_ATTRIBUTE, listName);
					
					if (this.include.isSelected())
						set.setSetting(Reasoner.INCLUDE_ATTRIBUTE, Reasoner.INCLUDE_ATTRIBUTE);
				}
			}
			
			return set;
		}
	}
	
	private void testReasoner(Settings reasoner) {
		QueriableAnnotation testDoc = Gamta.getTestDocument();
		if (testDoc == null) return;
		
		Annotation[] data = this.doReasoning(testDoc, reasoner.toProperties());
		
		AnnotationDisplayDialog add;
		Window top = DialogPanel.getTopWindow();
		if (top instanceof JDialog)
			add = new AnnotationDisplayDialog(((JDialog) top), "Matches of Conversion", data, true);
		else if (top instanceof JFrame)
			add = new AnnotationDisplayDialog(((JFrame) top), "Matches of Conversion", data, true);
		else add = new AnnotationDisplayDialog(((JFrame) null), "Matches of Conversion", data, true);
		add.setLocationRelativeTo(top);
		add.setVisible(true);
	}
	
	private Annotation[] doReasoning(QueriableAnnotation data, Properties parameters) {
		
		//	check parameters
		if ((data == null) || (parameters == null)) return null;
		
		//	get reasoning mode
		String reasoningMode = parameters.getProperty(Reasoner.REASONING_MODE_ATTRIBUTE, null);
		if (reasoningMode == null) return null;
		
		//	get (source) Annotation type
		String annotationType = parameters.getProperty(Reasoner.SOURCE_ANNOTATION_TYPE_ATTRIBUTE, null);
		if (annotationType == null) return null;
		
		//	get case sensitivity
		boolean caseSensitive = Reasoner.CASE_SENSITIVE_ATTRIBUTE.equals(parameters.getProperty(Reasoner.CASE_SENSITIVE_ATTRIBUTE, ""));
		
		if (Reasoner.VALUE_REASONING_MODE.equals(reasoningMode))
			return AnnotationReasoner.doValueReasoning(data, annotationType, caseSensitive);
			
		else if (Reasoner.CLONE_REASONING_MODE.equals(reasoningMode))
			return AnnotationReasoner.cloneAnnotations(data, annotationType);
			
		else if (Reasoner.ENVIRONMENT_REASONING_MODE.equals(reasoningMode)) {
			String targetType = parameters.getProperty(Reasoner.TARGET_TYPE_ATTRIBUTE, null);
			
			int envWidth = -1;
			try {
				envWidth = Integer.parseInt(parameters.getProperty(Reasoner.ENVIRONMENT_WIDTH_ATTRIBUTE, "-1"));
			} catch (Exception e) {}
			
			int leftEnvWidth = -1;
			try {
				leftEnvWidth = Integer.parseInt(parameters.getProperty(Reasoner.LEFT_ENVIRONMENT_WIDTH_ATTRIBUTE, "-1"));
			} catch (Exception e) {}
			int rightEnvWidth = -1;
			try {
				rightEnvWidth = Integer.parseInt(parameters.getProperty(Reasoner.RIGHT_ENVIRONMENT_WIDTH_ATTRIBUTE, "-1"));
			} catch (Exception e) {}
			
			if (envWidth == -1) {
				if ((leftEnvWidth >= 0) && (rightEnvWidth >= 0) && ((leftEnvWidth + rightEnvWidth) > 0))
					return AnnotationReasoner.doEnvironmentReasoning(data, annotationType, targetType, leftEnvWidth, rightEnvWidth, caseSensitive);
				else return null;
			}
			else {
				if ((leftEnvWidth >= 0) && (rightEnvWidth >= 0))
					return AnnotationReasoner.doEnvironmentReasoning(data, annotationType, targetType, envWidth, leftEnvWidth, rightEnvWidth, caseSensitive);
				else return AnnotationReasoner.doEnvironmentReasoning(data, annotationType, targetType, envWidth, caseSensitive);
			}
		}
		
		else if (Reasoner.VALUE_ENVIRONMENT_REASONING_MODE.equals(reasoningMode)) {
			String targetType = parameters.getProperty(Reasoner.TARGET_TYPE_ATTRIBUTE, null);
			
			int maxRounds = 0;
			try {
				maxRounds = Integer.parseInt(parameters.getProperty(Reasoner.MAX_ROUNDS_ATTRIBUTE, "0"));
			} catch (Exception e) {}
			
			int envWidth = -1;
			try {
				envWidth = Integer.parseInt(parameters.getProperty(Reasoner.ENVIRONMENT_WIDTH_ATTRIBUTE, "-1"));
			} catch (Exception e) {}
			
			int leftEnvWidth = -1;
			try {
				leftEnvWidth = Integer.parseInt(parameters.getProperty(Reasoner.LEFT_ENVIRONMENT_WIDTH_ATTRIBUTE, "-1"));
			} catch (Exception e) {}
			int rightEnvWidth = -1;
			try {
				rightEnvWidth = Integer.parseInt(parameters.getProperty(Reasoner.RIGHT_ENVIRONMENT_WIDTH_ATTRIBUTE, "-1"));
			} catch (Exception e) {}
			
			if (envWidth == -1) {
				if ((leftEnvWidth >= 0) && (rightEnvWidth >= 0) && ((leftEnvWidth + rightEnvWidth) > 0))
					return AnnotationReasoner.doValueEnvironmentReasoning(data, annotationType, targetType, leftEnvWidth, rightEnvWidth, caseSensitive, maxRounds);
				else return null;
			}
			else {
				if ((leftEnvWidth >= 0) && (rightEnvWidth >= 0))
					return AnnotationReasoner.doValueEnvironmentReasoning(data, annotationType, targetType, envWidth, leftEnvWidth, rightEnvWidth, caseSensitive, maxRounds);
				else  return AnnotationReasoner.doValueEnvironmentReasoning(data, annotationType, targetType, envWidth, caseSensitive, maxRounds);
			}
		}
		
		else if (Reasoner.TOKEN_FRACTION_REASONING_MODE.equals(reasoningMode)) {
			String targetType = parameters.getProperty(Reasoner.TARGET_TYPE_ATTRIBUTE, null);
			
			float minMatchFactor = 1;
			try {
				minMatchFactor = Float.parseFloat(parameters.getProperty(Reasoner.MIN_MATCH_FACTOR_ATTRIBUTE, "1"));
			} catch (Exception e) {}
			
			return AnnotationReasoner.doTokenReasoning(data, annotationType, targetType, minMatchFactor, caseSensitive);
		}
		
		else if (Reasoner.TOKEN_SEQUENCE_REASONING_MODE.equals(reasoningMode)) {
			int maxSpanTokens = 0;
			try {
				maxSpanTokens = Integer.parseInt(parameters.getProperty(Reasoner.MAX_SPAN_TOKENS_ATTRIBUTE, "0"));
			} catch (Exception e) {}
			
			String listName = parameters.getProperty(Reasoner.LIST_NAME_ATTRIBUTE, null);
			StringVector list = ((listName == null) ? new StringVector() : this.listProvider.getList(listName));
			
			boolean include = Reasoner.INCLUDE_ATTRIBUTE.equals(parameters.getProperty(Reasoner.INCLUDE_ATTRIBUTE, ""));
			
			return AnnotationReasoner.doTokenReasoning(data, annotationType, maxSpanTokens, list, include, caseSensitive);
		}
		
		else if (Reasoner.JOIN_ANNOTATIONS_REASONING_MODE.equals(reasoningMode)) {
			int maxSpanTokens = 0;
			try {
				maxSpanTokens = Integer.parseInt(parameters.getProperty(Reasoner.MAX_SPAN_TOKENS_ATTRIBUTE, "0"));
			} catch (Exception e) {}
			
			String listName = parameters.getProperty(Reasoner.LIST_NAME_ATTRIBUTE, null);
			StringVector list = ((listName == null) ? new StringVector() : this.listProvider.getList(listName));
			
			boolean include = Reasoner.INCLUDE_ATTRIBUTE.equals(parameters.getProperty(Reasoner.INCLUDE_ATTRIBUTE, ""));
			
			return AnnotationReasoner.joinAnnotations(data, annotationType, maxSpanTokens, list, include, caseSensitive);
		}
		
		//	return empty result by default
		return null;
	}
}
