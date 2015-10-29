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
package de.uka.ipd.idaho.goldenGate.plugin.annotationPatterns;

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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Properties;
import java.util.regex.PatternSyntaxException;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
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

import de.uka.ipd.idaho.easyIO.settings.Settings;
import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.util.AnnotationPatternMatcher;
import de.uka.ipd.idaho.gamta.util.AnnotationPatternMatcher.MatchTree;
import de.uka.ipd.idaho.gamta.util.swing.AnnotationDisplayDialog;
import de.uka.ipd.idaho.goldenGate.DocumentEditor;
import de.uka.ipd.idaho.goldenGate.GoldenGateConstants;
import de.uka.ipd.idaho.goldenGate.plugins.AbstractAnnotationSourceManager;
import de.uka.ipd.idaho.goldenGate.plugins.AnnotationSource;
import de.uka.ipd.idaho.goldenGate.plugins.AnnotationSourceParameterPanel;
import de.uka.ipd.idaho.goldenGate.util.DataListListener;
import de.uka.ipd.idaho.goldenGate.util.DialogPanel;
import de.uka.ipd.idaho.goldenGate.util.EditFontsButton;
import de.uka.ipd.idaho.goldenGate.util.FontEditable;
import de.uka.ipd.idaho.goldenGate.util.FontEditorDialog;
import de.uka.ipd.idaho.goldenGate.util.FontEditorPanel;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * Manager for annotation patterns as annotation sources. Annotation patterns
 * can be used to find and annotate matching sequences of tokens and annotations
 * in a document.<br>
 * <br>
 * All configuration can be done in the 'Edit Annotation Patterns' dialog in
 * the GoldenGATE Editor, which provides an editor for annotation patterns.
 * 
 * @author sautter
 */
public class AnnotationPatternManager extends AbstractAnnotationSourceManager {
	
	private static final String AD_HOC_ANNOTATION_PATTERN_NAME = "AdHoc";
	
	private static final String FILE_EXTENSION = ".annotationPattern";
	
	private StringVector adHocAnnotationPatternHistory = new StringVector();
	private int adHocAnnotationPatternHistorySize = 20;
	
	public AnnotationPatternManager() {}
	
	/* test an annotation pattern
	 * @param pattern the annotation pattern to test
	 * @return the Annotations extracted from the currently selected Document by the specified annotation pattern
	 */
	private MatchTree[] testAnnotationPattern(String pattern) {
		QueriableAnnotation data = this.parent.getActiveDocument();
		return ((data == null) ? null : AnnotationPatternMatcher.getMatchTrees(data, pattern));
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
				applyAdHocAnnotationPattern(targetProvider.getFunctionTarget());
			}
		});
		collector.add(mi);
		return ((JMenuItem[]) collector.toArray(new JMenuItem[collector.size()]));
	}
	
	private void applyAdHocAnnotationPattern(DocumentEditor target) {
		if (target == null)
			target = this.parent.getActivePanel();
		if (target != null)
			this.applyAnnotationSource(AD_HOC_ANNOTATION_PATTERN_NAME, target, new Properties());
	}
	
	/* 
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AnnotationSourceManager#createAnnotationSource()
	 */
	public String createAnnotationSource() {
		return this.createAnnotationPattern(null, null);
	}
	
	/* 
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AnnotationSourceManager#editAnnotationSource(java.lang.String)
	 */
	public void editAnnotationSource(String name) {
		String annotationPattern = this.loadStringResource(name);
		if ((annotationPattern == null) || (annotationPattern.length() == 0)) return;
		
		EditAnnotationPatternDialog eapd = new EditAnnotationPatternDialog(name, annotationPattern);
		eapd.setVisible(true);
		
		if (eapd.isCommitted()) try {
			this.storeStringResource(name, eapd.getAnnotationPattern());
		} catch (IOException ioe) {}
	}
	
	/* 
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AnnotationSourceManager#editAnnotationSources()
	 */
	public void editAnnotationSources() {
		this.editAnnotationPatternes();
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AnnotationSourceManager#getAnnotationSource(java.lang.String)
	 */
	public AnnotationSource getAnnotationSource(String name) {
		if (AD_HOC_ANNOTATION_PATTERN_NAME.equals(name))
			return this.getAdHocAnnotationPatternAnnotationSource();
		String annotationPattern = this.getAnnotationPattern(name);
		return ((annotationPattern == null) ? null : new AnnotationPatternAnnotationSource(name, annotationPattern));
	}
	
	
	private AnnotationPatternAnnotationSource getAdHocAnnotationPatternAnnotationSource() {
		AdHocAnnotationPatternDialog ahred = new AdHocAnnotationPatternDialog();
		ahred.setLocationRelativeTo(DialogPanel.getTopWindow());
		ahred.setVisible(true);
		if (ahred.isCommitted()) {
			String annotationPattern = ahred.getAnnotationPattern();
			return ((annotationPattern.trim().length() == 0) ? null : new AnnotationPatternAnnotationSource("Ad-Hoc Annotation Pattern", annotationPattern));
		}
		else return null;
	}
	
	private class AdHocAnnotationPatternDialog extends DialogPanel {
		private AnnotationPatternEditorPanel editor;
		private String annotationPattern = null;
		
		AdHocAnnotationPatternDialog() {
			super("Enter Ad-Hoc Annotation Pattern", true);
			
			final JComboBox adHocAnnotationPatternSelector = new JComboBox();
			adHocAnnotationPatternSelector.setModel(new DefaultComboBoxModel(adHocAnnotationPatternHistory.toStringArray()));
			adHocAnnotationPatternSelector.setEditable(false);
			adHocAnnotationPatternSelector.setSelectedItem(adHocAnnotationPatternHistory.isEmpty() ? "" : adHocAnnotationPatternHistory.get(0));
			adHocAnnotationPatternSelector.setBorder(BorderFactory.createLoweredBevelBorder());
			adHocAnnotationPatternSelector.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent ie) {
					editor.setContent(adHocAnnotationPatternSelector.getSelectedItem().toString());
				}
			});
			
			JPanel adHocAnnotationPatternPanel = new JPanel(new BorderLayout());
			adHocAnnotationPatternPanel.add(new JLabel("Recently Used: "), BorderLayout.WEST);
			adHocAnnotationPatternPanel.add(adHocAnnotationPatternSelector, BorderLayout.CENTER);
			
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
			this.editor = new AnnotationPatternEditorPanel("AdHoc", "", this);
			
			//	put the whole stuff together
			this.setLayout(new BorderLayout());
			this.add(adHocAnnotationPatternPanel, BorderLayout.NORTH);
			this.add(this.editor, BorderLayout.CENTER);
			this.add(mainButtonPanel, BorderLayout.SOUTH);
			
			this.setResizable(true);
			this.setSize(new Dimension(600, 400));
			this.setLocationRelativeTo(DialogPanel.getTopWindow());
		}
		
		boolean isCommitted() {
			return (this.annotationPattern != null);
		}
		
		String getAnnotationPattern() {
			return this.annotationPattern;
		}
		
		void abort() {
			this.annotationPattern = null;
			this.dispose();
		}

		void commit() {
			this.annotationPattern = this.editor.getAnnotationPattern();
			if (this.annotationPattern.trim().length() == 0)
				this.annotationPattern = null;
			else {
				try {
					AnnotationPatternMatcher.getMatches(Gamta.newDocument(Gamta.INNER_PUNCTUATION_TOKENIZER), AnnotationPatternMatcher.normalizePattern(this.annotationPattern));
				}
				catch (PatternSyntaxException pse) {
					JOptionPane.showMessageDialog(this, ("The annotation pattern you entered is not valid:\n" + pse.getMessage()), "Annotation Pattern Validation Failed", JOptionPane.ERROR_MESSAGE);
					return;
				}
				adHocAnnotationPatternHistory.removeAll(this.annotationPattern);
				adHocAnnotationPatternHistory.insertElementAt(this.annotationPattern, 0);
				while (adHocAnnotationPatternHistory.size() > adHocAnnotationPatternHistorySize)
					adHocAnnotationPatternHistory.removeElementAt(adHocAnnotationPatternHistorySize);
			}
			this.dispose();
		}
	}
	
	private class AnnotationPatternAnnotationSource implements AnnotationSource {
		
		private String name;
		private String annotationPattern;
		
		AnnotationPatternAnnotationSource(String name, String annotationPattern) {
			this.name = name;
			this.annotationPattern = annotationPattern;
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
			return "Annotation Pattern";
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.AnnotationSource#getProviderClassName()
		 */
		public String getProviderClassName() {
			return AnnotationPatternManager.class.getName();
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
			return AnnotationPatternMatcher.getMatches(data, this.annotationPattern);
		}
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AnnotationSourceManager#getAnnotatorParameterPanel()
	 */
	public AnnotationSourceParameterPanel getAnnotatorParameterPanel() {
		return new AnnotationPatternParameterPanel();
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AnnotationSourceManager#getAnnotatorParameterPanel(de.uka.ipd.idaho.easyIO.settings.Settings)
	 */
	public AnnotationSourceParameterPanel getAnnotatorParameterPanel(Settings settings) {
		return new AnnotationPatternParameterPanel(settings);
	}
	
	private class AnnotationPatternParameterPanel extends AnnotationSourceParameterPanel {
		AnnotationPatternParameterPanel() {
			this(null);
		}
		AnnotationPatternParameterPanel(Settings settings) {
			super(new BorderLayout());
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.AnnotationSourceParameterPanel#isDirty()
		 */
		public boolean isDirty() {
			return false;
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.AnnotationSourceParameterPanel#getSetting(java.lang.String)
		 */
		public Settings getSettings() {
			return new Settings();
		}
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.ResourceManager#getResourceTypeLabel()
	 */
	public String getResourceTypeLabel() {
		return "Annotation Pattern";
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
					createAnnotationPattern();
				}
			});
			collector.add(mi);
			mi = new JMenuItem("Edit");
			mi.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					editAnnotationPatternes();
				}
			});
			collector.add(mi);
			collector.add(GoldenGateConstants.MENU_SEPARATOR_ITEM);
		}
		
		mi = new JMenuItem("Ad Hoc (enter and apply)");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				applyAdHocAnnotationPattern(null);
			}
		});
		collector.add(mi);
		
		return ((JMenuItem[]) collector.toArray(new JMenuItem[collector.size()]));
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#getMainMenuTitle()
	 */
	public String getMainMenuTitle() {
		return "Annotation Patterns";
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager#getToolsMenuLabel()
	 */
	public String getToolsMenuLabel() {
		return "Apply";
	}
	
	/* retrieve a plain String representation of a regular expression by its name
	 * @param	name	the name of the reqired regular expression
	 * @return the String representation of the regular expression with the required name, or null, if there is no such regular expression
	 */
	private String getAnnotationPattern(String name) {
		if (name == null)
			return null;
		return this.loadStringResource(name);
	}
	
	private boolean createAnnotationPattern() {
		return (this.createAnnotationPattern(null, null) != null);
	}
	
	private boolean cloneAnnotationPattern() {
		String selectedName = this.resourceNameList.getSelectedName();
		if (selectedName == null)
			return this.createAnnotationPattern();
		else {
			String name = "New " + selectedName;
			return (this.createAnnotationPattern(this.loadStringResource(selectedName), name) != null);
		}
	}
	
	private String createAnnotationPattern(String modelAnnotationPattern, String name) {
		CreateAnnotationPatternDialog cred = new CreateAnnotationPatternDialog(name, modelAnnotationPattern);
		cred.setVisible(true);
		if (cred.isCommitted()) {
			String annotationPattern = cred.getAnnotationPattern();
			String annotationPatternName = cred.getAnnotationPatternName();
			if (!annotationPatternName.endsWith(FILE_EXTENSION)) annotationPatternName += FILE_EXTENSION;
			try {
				if (this.storeStringResource(annotationPatternName, annotationPattern)) {
					this.resourceNameList.refresh();
					return annotationPatternName;
				}
			} catch (IOException e) {}
		}
		return null;
	}
	
	private void editAnnotationPatternes() {
		final AnnotationPatternEditorPanel[] editor = new AnnotationPatternEditorPanel[1];
		editor[0] = null;
		
		final DialogPanel editDialog = new DialogPanel("Edit Annotation Patterns", true);
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
						storeStringResource(editor[0].annotationPatternName, editor[0].getAnnotationPattern());
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
				createAnnotationPattern();
			}
		});
		editButtons.add(button);
		button = new JButton("Clone");
		button.setBorder(BorderFactory.createRaisedBevelBorder());
		button.setPreferredSize(new Dimension(100, 21));
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				cloneAnnotationPattern();
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
			String annotationPattern = this.loadStringResource(selectedName);
			if (annotationPattern == null)
				editorPanel.add(this.getExplanationLabel(), BorderLayout.CENTER);
			else {
				editor[0] = new AnnotationPatternEditorPanel(selectedName, annotationPattern, editDialog);
				editorPanel.add(editor[0], BorderLayout.CENTER);
			}
		}
		editDialog.add(editorPanel, BorderLayout.CENTER);
		
		editDialog.add(this.resourceNameList, BorderLayout.EAST);
		DataListListener dll = new DataListListener() {
			public void selected(String dataName) {
				if ((editor[0] != null) && editor[0].isDirty()) {
					try {
						storeStringResource(editor[0].annotationPatternName, editor[0].getAnnotationPattern());
					}
					catch (IOException ioe) {
						if (JOptionPane.showConfirmDialog(editDialog, (ioe.getClass().getName() + " (" + ioe.getMessage() + ")\nwhile saving file to " + editor[0].annotationPatternName + "\nProceed?"), "Could Not Save Analyzer", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
							resourceNameList.setSelectedName(editor[0].annotationPatternName);
							editorPanel.validate();
							return;
						}
					}
				}
				editorPanel.removeAll();
				
				if (dataName == null)
					editorPanel.add(getExplanationLabel(), BorderLayout.CENTER);
				else {
					String annotationPattern = loadStringResource(dataName);
					if (annotationPattern == null)
						editorPanel.add(getExplanationLabel(), BorderLayout.CENTER);
					else {
						editor[0] = new AnnotationPatternEditorPanel(dataName, annotationPattern, editDialog);
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
	
	private class CreateAnnotationPatternDialog extends DialogPanel {
		
		private JTextField nameField;
		
		private AnnotationPatternEditorPanel editor;
		private String annotationPattern = null;
		private String annotationPatternName = null;
		
		CreateAnnotationPatternDialog(String name, String annotationPattern) {
			super("Create AnnotationPattern", true);
			
			this.nameField = new JTextField((name == null) ? "New AnnotationPattern" : name);
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
			this.editor = new AnnotationPatternEditorPanel(name, annotationPattern, this);
			
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
			return (this.annotationPattern != null);
		}
		
		String getAnnotationPattern() {
			return this.annotationPattern;
		}
		
		String getAnnotationPatternName() {
			return this.annotationPatternName;
		}
		
		void abort() {
			this.annotationPattern = null;
			this.annotationPatternName = null;
			this.dispose();
		}

		void commit() {
			this.annotationPattern = this.editor.getAnnotationPattern();
			this.annotationPatternName = this.nameField.getText();
			this.dispose();
		}
	}

	private class EditAnnotationPatternDialog extends DialogPanel {
		
		private AnnotationPatternEditorPanel editor;
		private String annotationPattern = null;
		
		EditAnnotationPatternDialog(String name, String annotationPattern) {
			super(("Edit AnnotationPattern '" + name + "'"), true);
			
			//	initialize main buttons
			JButton commitButton = new JButton("OK");
			commitButton.setBorder(BorderFactory.createRaisedBevelBorder());
			commitButton.setPreferredSize(new Dimension(100, 21));
			commitButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					EditAnnotationPatternDialog.this.annotationPattern = editor.getAnnotationPattern();
					dispose();
				}
			});
			
			JButton abortButton = new JButton("Cancel");
			abortButton.setBorder(BorderFactory.createRaisedBevelBorder());
			abortButton.setPreferredSize(new Dimension(100, 21));
			abortButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					EditAnnotationPatternDialog.this.annotationPattern = null;
					dispose();
				}
			});
			
			JPanel mainButtonPanel = new JPanel(new FlowLayout());
			mainButtonPanel.add(commitButton);
			mainButtonPanel.add(abortButton);
			
			//	initialize editor
			this.editor = new AnnotationPatternEditorPanel(name, annotationPattern, this);
			
			//	put the whole stuff together
			this.setLayout(new BorderLayout());
			this.add(this.editor, BorderLayout.CENTER);
			this.add(mainButtonPanel, BorderLayout.SOUTH);
			
			this.setResizable(true);
			this.setSize(new Dimension(600, 400));
			this.setLocationRelativeTo(DialogPanel.getTopWindow());
		}
		
		boolean isCommitted() {
			return (this.annotationPattern != null);
		}
		
		String getAnnotationPattern() {
			return this.annotationPattern;
		}
	}
	
	private class AnnotationPatternEditorPanel extends JPanel implements FontEditable, DocumentListener {
		
		private static final int MAX_SCROLLBAR_WAIT = 200;
		
		private JTextArea editor;
		private JScrollPane editorBox;
		
		private String content = "";
		
		private String fontName = "Verdana";
		private int fontSize = 12;
		private Color fontColor = Color.BLACK;
		
		private boolean dirty = false;
		private String annotationPatternName;
		
		private DialogPanel frame;
		
		AnnotationPatternEditorPanel(String name, String annotationPattern, DialogPanel frame) {
			super(new BorderLayout(), true);
			this.annotationPatternName = name;
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
				public void actionPerformed(ActionEvent e) {
					refreshAnnotationPattern();
				}
			});
			
			JButton validateButton = new JButton("Validate");
			validateButton.setBorder(BorderFactory.createRaisedBevelBorder());
			validateButton.setPreferredSize(new Dimension(115, 21));
			validateButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					validateAnnotationPattern();
				}
			});
			
			JButton testButton = new JButton("Test");
			testButton.setBorder(BorderFactory.createRaisedBevelBorder());
			testButton.setPreferredSize(new Dimension(115, 21));
			testButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					testAnnotationPattern();
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
			
			//	put the whole stuff together
			this.add(this.editorBox, BorderLayout.CENTER);
			this.add(buttonPanel, BorderLayout.SOUTH);
			this.setContent((annotationPattern == null) ? "" : annotationPattern);
		}
		
		String getAnnotationPattern() {
			if (this.isDirty())
				this.content = AnnotationPatternMatcher.normalizePattern(this.editor.getText());
			return this.content;
		}
		
		void setContent(String annotationPattern) {
			this.content = AnnotationPatternMatcher.normalizePattern(annotationPattern);
			this.refreshDisplay();
			this.dirty = false;
		}
		
		boolean isDirty() {
			return this.dirty;
		}
		
		void refreshAnnotationPattern() {
			String annotationPattern = this.editor.getText();
			if ((annotationPattern != null) && (annotationPattern.length() != 0)) {
				
				final Point viewPosition = this.editorBox.getViewport().getViewPosition();
				
				String normalizedAnnotationPattern = AnnotationPatternMatcher.normalizePattern(annotationPattern);
				this.editor.getDocument().removeDocumentListener(this);
				this.editor.setText(AnnotationPatternMatcher.explodePattern(normalizedAnnotationPattern));
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
		
		void validateAnnotationPattern() {
			boolean selected = true;
			String annotationPattern = this.editor.getSelectedText();
			if ((annotationPattern == null) || (annotationPattern.length() == 0)) {
				annotationPattern = this.editor.getText();
				selected = false;
			}
			if (!this.validateAnnotationPattern(annotationPattern)) JOptionPane.showMessageDialog(this, "The " + (selected ? "selected pattern part" : "pattern") + " is not a valid pattern.", "AnnotationPattern Validation", JOptionPane.ERROR_MESSAGE);
			else JOptionPane.showMessageDialog(this, "The " + (selected ? "selected pattern part" : "pattern") + " is a valid pattern.", "AnnotationPattern Validation", JOptionPane.INFORMATION_MESSAGE);
		}
		
		void testAnnotationPattern() {
			boolean selected = true;
			String annotationPattern = this.editor.getSelectedText();
			if ((annotationPattern == null) || (annotationPattern.length() == 0)) {
				annotationPattern = this.editor.getText();
				selected = false;
			}
			annotationPattern = annotationPattern.trim();
			annotationPattern = AnnotationPatternMatcher.normalizePattern(annotationPattern);
			if (!this.validateAnnotationPattern(annotationPattern))
				JOptionPane.showMessageDialog(this, "The " + (selected ? "selected pattern part" : "pattern") + " is not a valid pattern.", "AnnotationPattern Validation", JOptionPane.ERROR_MESSAGE);
			else {
				final MatchTree[] matches = AnnotationPatternManager.this.testAnnotationPattern(annotationPattern);
				if (matches == null)
					return;
				final Annotation[] annotations = new Annotation[matches.length];
				for (int m = 0; m < matches.length; m++)
					annotations[m] = matches[m].getMatch();
				AnnotationDisplayDialog add = new AnnotationDisplayDialog(this.frame.getDialog(), "Matches of Annotation Pattern (double-click for details)", annotations, true) {
					protected void annotationClicked(int rowIndex, int clickCount) {
						if (clickCount < 2)
							super.annotationClicked(rowIndex, clickCount);
						else try {
							StringBuffer matchDetails = new StringBuffer("<HTML>");
							BufferedReader matchDetailReader = new BufferedReader(new StringReader(matches[rowIndex].toString()));
							for (String line; (line = matchDetailReader.readLine()) != null;) {
								if (line.trim().length() == 0)
									continue;
								if (matchDetails.length() > "<HTML>".length())
									matchDetails.append("<BR>");
								int lineStart = 0;
								while ((lineStart < line.length()) && (line.charAt(lineStart) < 33)) {
									matchDetails.append("&nbsp;");
									lineStart++;
								}
								matchDetails.append(AnnotationUtils.escapeForXml(line.substring(lineStart)));
							}
							matchDetails.append("</HTML>");
							JLabel matchDetailDisplay = new JLabel(matchDetails.toString(), JLabel.LEFT);
							JScrollPane matchDetailBox = new JScrollPane(matchDetailDisplay);
							Dimension matchDetailDisplaySize = matchDetailDisplay.getPreferredSize();
							matchDetailBox.setPreferredSize(new Dimension(Math.min(500, (matchDetailDisplaySize.width + 10)), Math.min(500, (matchDetailDisplaySize.height + 50))));
							JOptionPane.showMessageDialog(frame.getDialog(), matchDetailBox, "Match Details", JOptionPane.PLAIN_MESSAGE);
						} catch (IOException ioe) { /* never gonna happen, but Java don't know */ }
					}
				};
				add.setLocationRelativeTo(this);
				add.setVisible(true);
			}
		}
		
		boolean validateAnnotationPattern(String annotationPattern) {
			try {
				annotationPattern = AnnotationPatternMatcher.normalizePattern(annotationPattern);
				AnnotationPatternMatcher.getMatches(Gamta.newDocument(Gamta.INNER_PUNCTUATION_TOKENIZER), annotationPattern);
				return true;
			}
			catch (PatternSyntaxException pse) {
				return false;
			}
		}
		
		void refreshDisplay() {
			final JScrollBar scroller = this.editorBox.getVerticalScrollBar();
			final int scrollPosition = scroller.getValue();
			
			this.editor.getDocument().removeDocumentListener(this);
			this.editor.setFont(new Font(this.fontName, Font.PLAIN, this.fontSize));
			this.editor.setText(AnnotationPatternMatcher.explodePattern(this.content));
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