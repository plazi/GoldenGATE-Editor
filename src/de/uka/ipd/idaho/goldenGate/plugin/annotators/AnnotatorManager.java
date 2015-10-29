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
package de.uka.ipd.idaho.goldenGate.plugin.annotators;


import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import de.uka.ipd.idaho.easyIO.settings.Settings;
import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.util.swing.AnnotationDisplayDialog;
import de.uka.ipd.idaho.goldenGate.plugins.AbstractDocumentProcessorManager;
import de.uka.ipd.idaho.goldenGate.plugins.AnnotationSource;
import de.uka.ipd.idaho.goldenGate.plugins.AnnotationSourceManager;
import de.uka.ipd.idaho.goldenGate.plugins.AnnotationSourceParameterPanel;
import de.uka.ipd.idaho.goldenGate.plugins.DocumentProcessor;
import de.uka.ipd.idaho.goldenGate.plugins.ResourceManager;
import de.uka.ipd.idaho.goldenGate.util.DataListListener;
import de.uka.ipd.idaho.goldenGate.util.DialogPanel;
import de.uka.ipd.idaho.goldenGate.util.ResourceDialog;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * Document processor manager for Annotators. Annotators are basically wrappers
 * for AnnotationSources, consisting of the name and provider name of the
 * wrapped AnnotationSource and a series of parameters for the AnnotationSource.
 * The rationale is to render AnnotationSources capable of running as
 * DocumentProcessors. All configuration can be done in the 'Edit Annotators'
 * dialog in the GoldenGATE Editor.
 * 
 * @author sautter
 */
public class AnnotatorManager extends AbstractDocumentProcessorManager {
	
	private static final String FILE_EXTENSION = ".annotator";
	
	public AnnotatorManager() {}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractDocumentProcessorManager#getDocumentProcessor(java.lang.String)
	 */
	public DocumentProcessor getDocumentProcessor(String name) {
		Settings settings = this.getAnnotator(name);
		return ((settings == null) ? null : this.getAnnotator(name, settings));
	}
	
	/* 
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentProcessorManager#createDocumentProcessor()
	 */
	public String createDocumentProcessor() {
		return this.createAnnotator(new Settings(), null);
	}
	
	/* 
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentProcessorManager#editDocumentProcessor(java.lang.String)
	 */
	public void editDocumentProcessor(String name) {
		Settings set = this.loadSettingsResource(name);
		if ((set == null) || set.isEmpty()) return;
		
		EditAnnotatorDialog ead = new EditAnnotatorDialog(name, set);
		ead.setVisible(true);
		
		if (ead.isCommitted()) try {
			this.storeSettingsResource(name, ead.getAnnotator());
		} catch (IOException ioe) {}
	}
	
	/* 
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentProcessorManager#editDocumentProcessors()
	 */
	public void editDocumentProcessors() {
		this.editAnnotators();
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager#getDataNamesForResource(java.lang.String)
	 */
	public String[] getDataNamesForResource(String name) {
		StringVector names = new StringVector();
		names.addContentIgnoreDuplicates(super.getDataNamesForResource(name));
		Settings settings = this.getAnnotator(name);
		if ((settings == null) || settings.isEmpty()) return names.toStringArray();
		AnnotationSourceManager asp = this.parent.getAnnotationSourceProvider(settings.getSetting(AnnotatorDocumentProcessor.PROVIDER_CLASS_NAME_ATTRIBUTE));
		if (asp != null) // fetch data names for annotation source
			names.addContentIgnoreDuplicates(asp.getDataNamesForResource(settings.getSetting(AnnotatorDocumentProcessor.ANNOTATOR_NAME_ATTRIBUTE)));
		return names.toStringArray();
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.ResourceManager#getRequiredResourceNames(java.lang.String, boolean)
	 */
	public String[] getRequiredResourceNames(String name, boolean recourse) {
		StringVector nameCollector = new StringVector();
		Settings settings = this.getAnnotator(name);
		
		String annotatorName = settings.getSetting(AnnotatorDocumentProcessor.ANNOTATOR_NAME_ATTRIBUTE);
		if (annotatorName != null)
			nameCollector.addElement(annotatorName + "@" + settings.getSetting(AnnotatorDocumentProcessor.PROVIDER_CLASS_NAME_ATTRIBUTE));
		
		int nameIndex = 0;
		while (recourse && (nameIndex < nameCollector.size())) {
			String resName = nameCollector.get(nameIndex);
			int split = resName.indexOf('@');
			if (split != -1) {
				String plainResName = resName.substring(0, split);
				String resProviderClassName = resName.substring(split + 1);
				
				ResourceManager rm = this.parent.getResourceProvider(resProviderClassName);
				if (rm != null)
					nameCollector.addContentIgnoreDuplicates(rm.getRequiredResourceNames(plainResName, recourse));
			}
			nameIndex++;
		}
		
		return nameCollector.toStringArray();
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.ResourceManager#getResourceTypeLabel()
	 */
	public String getResourceTypeLabel() {
		return "Annotator";
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractDocumentProcessorManager#getFileExtension()
	 */
	protected String getFileExtension() {
		return FILE_EXTENSION;
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractDocumentProcessorManager#getMainMenuItems()
	 */
	public JMenuItem[] getMainMenuItems() {
		if (!this.dataProvider.isDataEditable())
			return new JMenuItem[0];
		
		ArrayList collector = new ArrayList();
		JMenuItem mi;
		
		mi = new JMenuItem("Create");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				createAnnotator();
			}
		});
		collector.add(mi);
		mi = new JMenuItem("Edit");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				editAnnotators();
			}
		});
		collector.add(mi);
		return ((JMenuItem[]) collector.toArray(new JMenuItem[collector.size()]));
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractDocumentProcessorManager#getMainMenuTitle()
	 */
	public String getMainMenuTitle() {
		return "Annotators";
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager#getToolsMenuLabel()
	 */
	public String getToolsMenuLabel() {
		return "Apply";
	}
	
	/* retrieve a Annotator by its name
	 * @param	name	the name of the reqired Annotator
	 * @return the Annotator with the required name, or null, if there is no such Annotator
	 */
	private Settings getAnnotator(String name) {
		if (name == null) return null;
		return this.loadSettingsResource(name);
	}
	
	private AnnotatorDocumentProcessor getAnnotator(String name, Settings settings) {
		if (settings == null) return null;
		String annotationType = settings.getSetting(AnnotatorDocumentProcessor.ANNOTATION_TYPE_ATTRIBUTE);
		if (annotationType == null) return null;
		AnnotationSourceManager dam = this.parent.getAnnotationSourceProvider(settings.getSetting(AnnotatorDocumentProcessor.PROVIDER_CLASS_NAME_ATTRIBUTE));
		if (dam != null) {
			AnnotationSource as = dam.getAnnotationSource(settings.getSetting(AnnotatorDocumentProcessor.ANNOTATOR_NAME_ATTRIBUTE));
			if (as != null)
				return new AnnotatorDocumentProcessor(name, annotationType, settings.containsKey(AnnotatorDocumentProcessor.INTERACTIVE_ATTRIBUTE), as, settings);
			else return null;
		} else return null;
	}
	
	private class AnnotatorDocumentProcessor implements DocumentProcessor {
		
		private static final String ANNOTATOR_NAME_ATTRIBUTE = "ANNOTATOR_NAME";
		private static final String PROVIDER_CLASS_NAME_ATTRIBUTE = "PROVIDER_CLASS";
		private static final String ANNOTATION_TYPE_ATTRIBUTE = "ANNOTATION_TYPE";
		private static final String INTERACTIVE_ATTRIBUTE = "INTERACTIVE";
		
		private String name;
		private String annotationType;
		private boolean interactive;
		private AnnotationSource annotator;
		private Settings parameters;
		
		AnnotatorDocumentProcessor(String name, String annotationType, boolean interactive, AnnotationSource annotator, Settings parameters) {
			this.name = name;
			this.annotator = annotator;
			this.interactive = interactive;
			this.parameters = parameters;
			this.annotationType = annotationType;
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
			return "Annotator";
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentProcessor#getProviderClassName()
		 */
		public String getProviderClassName() {
			return AnnotatorManager.class.getName();
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentProcessor#process(de.uka.ipd.idaho.gamta.MutableAnnotation)
		 */
		public void process(MutableAnnotation data) {
			Properties parameters = new Properties();
			parameters.setProperty(INTERACTIVE_PARAMETER, INTERACTIVE_PARAMETER);
			this.process(data, parameters);
		}
		
		/* 
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentProcessor#process(de.uka.ipd.idaho.gamta.MutableAnnotation, java.util.Properties)
		 */
		public void process(MutableAnnotation data, Properties parameters) {
			Annotation[] annotations = this.annotator.annotate(data, this.parameters.toProperties());
			if (annotations != null) {
				
				//	name Annotations
				for (int a = 0; a < annotations.length; a++)
					annotations[a].changeTypeTo(this.annotationType);
				
				//	show Annotations to user
				boolean annotate = true;
				if (this.interactive && parameters.containsKey(INTERACTIVE_PARAMETER)) {
					Window top = DialogPanel.getTopWindow();
					AnnotationDisplayDialog add;
					
					if (top instanceof JDialog)
						add = new AnnotationDisplayDialog(((JDialog) top), "Matches of Annotator", annotations);
					else if (top instanceof JFrame)
						add = new AnnotationDisplayDialog(((JFrame) top), "Matches of Annotator", annotations);
					else add = new AnnotationDisplayDialog(((JFrame) null), "Matches of Annotator", annotations);
					
					add.setLocationRelativeTo(top);
					add.setVisible(true);
					if (!add.isCommitted()) annotate = false;
				}
				
				//	add Annotations to document
				if (annotate) for (int a = 0; a < annotations.length; a++)
					annotations[a] = data.addAnnotation(annotations[a]);
			}
		}
	}
	
	private boolean createAnnotator() {
		return (this.createAnnotator(new Settings(), "New Annotator") != null);
	}
	
	private boolean cloneAnnotator() {
		String selectedName = this.resourceNameList.getSelectedName();
		if (selectedName == null)
			return this.createAnnotator();
		else {
			String name = "New " + selectedName;
			return (this.createAnnotator(this.loadSettingsResource(selectedName), name) != null);
		}
	}
	
	private String createAnnotator(Settings set, String name) {
		CreateAnnotatorDialog cfd = new CreateAnnotatorDialog(name, set);
		cfd.setVisible(true);
		if (cfd.isCommitted()) {
			Settings annotator = cfd.getAnnotator();
			String annotatorName = cfd.getAnnotatorName();
			if (!annotatorName.endsWith(FILE_EXTENSION)) annotatorName += FILE_EXTENSION;
			try {
				if (this.storeSettingsResource(annotatorName, annotator)) {
					this.resourceNameList.refresh();
					return annotatorName;
				}
			} catch (IOException e) {}
		}
		return null;
	}
	
	private void editAnnotators() {
		final AnnotatorEditorPanel[] editor = new AnnotatorEditorPanel[1];
		editor[0] = null;
		
		final DialogPanel editDialog = new DialogPanel("Edit Annotators", true);
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
						storeSettingsResource(editor[0].annotatorName, editor[0].getSettings());
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
				createAnnotator();
			}
		});
		editButtons.add(button);
		button = new JButton("Clone");
		button.setBorder(BorderFactory.createRaisedBevelBorder());
		button.setPreferredSize(new Dimension(100, 21));
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				cloneAnnotator();
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
				editor[0] = new AnnotatorEditorPanel(selectedName, set);
				editorPanel.add(editor[0], BorderLayout.CENTER);
			}
		}
		editDialog.add(editorPanel, BorderLayout.CENTER);
		
		editDialog.add(this.resourceNameList, BorderLayout.EAST);
		DataListListener dll = new DataListListener() {
			public void selected(String dataName) {
				if ((editor[0] != null) && editor[0].isDirty()) {
					try {
						storeSettingsResource(editor[0].annotatorName, editor[0].getSettings());
					}
					catch (IOException ioe) {
						if (JOptionPane.showConfirmDialog(editDialog, (ioe.getClass().getName() + " (" + ioe.getMessage() + ")\nwhile saving file to " + editor[0].annotatorName + "\nProceed?"), "Could Not Save Analyzer", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
							resourceNameList.setSelectedName(editor[0].annotatorName);
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
						editor[0] = new AnnotatorEditorPanel(dataName, set);
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
	
	private class CreateAnnotatorDialog extends DialogPanel {
		
		private JTextField nameField;
		
		private AnnotatorEditorPanel editor;
		private Settings annotator = null;
		private String annotatorName = null;
		
		CreateAnnotatorDialog(String name, Settings annotator) {
			super("Create Annotator", true);
			
			this.nameField = new JTextField((name == null) ? "New Annotator" : name);
			this.nameField.setBorder(BorderFactory.createLoweredBevelBorder());
			
			//	initialize main buttons
			JButton commitButton = new JButton("Create");
			commitButton.setBorder(BorderFactory.createRaisedBevelBorder());
			commitButton.setPreferredSize(new Dimension(100, 21));
			commitButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					CreateAnnotatorDialog.this.annotator = editor.getSettings();
					annotatorName = nameField.getText();
					dispose();
				}
			});
			
			JButton abortButton = new JButton("Cancel");
			abortButton.setBorder(BorderFactory.createRaisedBevelBorder());
			abortButton.setPreferredSize(new Dimension(100, 21));
			abortButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					CreateAnnotatorDialog.this.annotator = null;
					annotatorName = null;
					dispose();
				}
			});
			
			JPanel mainButtonPanel = new JPanel();
			mainButtonPanel.setLayout(new FlowLayout());
			mainButtonPanel.add(commitButton);
			mainButtonPanel.add(abortButton);
			
			//	initialize editor
			this.editor = new AnnotatorEditorPanel(name, annotator);
			
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
			return (this.annotator != null);
		}
		
		Settings getAnnotator() {
			return this.annotator;
		}
		
		String getAnnotatorName() {
			return this.annotatorName;
		}
	}

	private class EditAnnotatorDialog extends DialogPanel {
		
		private AnnotatorEditorPanel editor;
		private String annotatorName = null;
		
		EditAnnotatorDialog(String name, Settings annotator) {
			super(("Edit Annotator '" + name + "'"), true);
			this.annotatorName = name;
			
			//	initialize main buttons
			JButton commitButton = new JButton("OK");
			commitButton.setBorder(BorderFactory.createRaisedBevelBorder());
			commitButton.setPreferredSize(new Dimension(100, 21));
			commitButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					dispose();
				}
			});
			
			JButton abortButton = new JButton("Cancel");
			abortButton.setBorder(BorderFactory.createRaisedBevelBorder());
			abortButton.setPreferredSize(new Dimension(100, 21));
			abortButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					annotatorName = null;
					dispose();
				}
			});
			
			JPanel mainButtonPanel = new JPanel();
			mainButtonPanel.setLayout(new FlowLayout());
			mainButtonPanel.add(commitButton);
			mainButtonPanel.add(abortButton);
			
			//	initialize editor
			this.editor = new AnnotatorEditorPanel(name, annotator);
			
			//	put the whole stuff together
			this.setLayout(new BorderLayout());
			this.add(this.editor, BorderLayout.CENTER);
			this.add(mainButtonPanel, BorderLayout.SOUTH);
			
			this.setResizable(true);
			this.setSize(new Dimension(600, 400));
			this.setLocationRelativeTo(this.getOwner());
		}
		
		boolean isCommitted() {
			return (this.annotatorName != null);
		}
		
		Settings getAnnotator() {
			return this.editor.getSettings();
		}
	}

	private class AnnotatorEditorPanel extends JPanel {
		
		private String annotatorName = null;
		
		private String annotationSourceName = null;
		private String annotationSourceProviderClassName = null;
		private JLabel annotationSourceLabel = new JLabel("<No Annotation Source Selected>", JLabel.LEFT);
		
		private JPanel functionPanel = new JPanel(new GridBagLayout(), true);
		private JPanel contentPanel = new JPanel(new BorderLayout(), true);
		private AnnotationSourceParameterPanel annotationSourcePanel;
		
		private JTextField annotationType = new JTextField();
		private JCheckBox interactive = new JCheckBox("Interactive", true);
		
		private boolean dirty = false;
		
		AnnotatorEditorPanel(String name, Settings settings) {
			super(new BorderLayout(), true);
			this.annotatorName = name;
			
			this.add(getExplanationLabel(), BorderLayout.CENTER);
			
			this.annotationSourceName = settings.getSetting(AnnotatorDocumentProcessor.ANNOTATOR_NAME_ATTRIBUTE);
			this.annotationSourceProviderClassName = settings.getSetting(AnnotatorDocumentProcessor.PROVIDER_CLASS_NAME_ATTRIBUTE);
			
			this.annotationType.setText(settings.getSetting(AnnotatorDocumentProcessor.ANNOTATION_TYPE_ATTRIBUTE, ""));
			this.annotationType.getDocument().addDocumentListener(new DocumentListener() {
				public void changedUpdate(DocumentEvent de) {}
				public void insertUpdate(DocumentEvent de) {
					dirty = true;
				}
				public void removeUpdate(DocumentEvent de) {
					dirty = true;
				}
			});
			
			this.interactive.setSelected(AnnotatorDocumentProcessor.INTERACTIVE_ATTRIBUTE.equals(settings.getSetting(AnnotatorDocumentProcessor.INTERACTIVE_ATTRIBUTE, "")));
			this.interactive.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent ie) {
					dirty = true;
				}
			});
			
			AnnotationSourceManager dam = parent.getAnnotationSourceProvider(this.annotationSourceProviderClassName);
			if (dam != null) {
				this.annotationSourcePanel = dam.getAnnotatorParameterPanel(settings);
				AnnotationSource da = dam.getAnnotationSource(this.annotationSourceName);
				if (da != null) this.annotationSourceLabel.setText(da.getTypeLabel() + ": " + da.getName() + " (double click to edit)");
			}
			this.annotationSourceLabel.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent me) {
					if (me.getClickCount() > 1) {
						AnnotationSourceManager asm = parent.getAnnotationSourceProvider(annotationSourceProviderClassName);
						if (asm != null) asm.editAnnotationSource(annotationSourceName);
					}
				}
			});
			
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.insets.top = 2;
			gbc.insets.bottom = 2;
			gbc.insets.left = 5;
			gbc.insets.right = 5;
			gbc.weighty = 1;
			gbc.weightx = 1;
			gbc.gridheight = 1;
			gbc.gridwidth = 3;
			gbc.gridx = 0;
			gbc.fill = GridBagConstraints.BOTH;
			
			gbc.gridy = 0;
			this.functionPanel.add(this.annotationSourceLabel, gbc.clone());
			
			gbc.gridy++;
			AnnotationSourceManager[] damps = parent.getAnnotationSourceProviders();
			for (int a = 0; a < damps.length; a++) {
				JButton button = new JButton("Use " + damps[a].getResourceTypeLabel());
				button.setBorder(BorderFactory.createRaisedBevelBorder());
				button.setSize(new Dimension(120, 21));
				final String className = damps[a].getClass().getName();
				button.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						selectAnnotator(className);
					}
				});
				
				gbc.gridx = 0;
				gbc.weightx = 1;
				gbc.gridwidth = 2;
				this.functionPanel.add(button, gbc.clone());
				
				button = new JButton("Create " + damps[a].getResourceTypeLabel());
				button.setBorder(BorderFactory.createRaisedBevelBorder());
				button.setSize(new Dimension(120, 21));
				button.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						createAnnotator(className);
					}
				});
				
				gbc.gridx = 2;
				gbc.weightx = 0;
				gbc.gridwidth = 1;
				functionPanel.add(button, gbc.clone());
				gbc.gridy++;
			}
			
			gbc.gridx = 0;
			gbc.gridwidth = 1;
			gbc.weightx = 0;
			this.functionPanel.add(new JLabel("Annotation Type:", JLabel.LEFT), gbc.clone());
			gbc.weightx = 1;
			gbc.gridx = 1;
			this.functionPanel.add(this.annotationType, gbc.clone());
			gbc.weightx = 0;
			gbc.gridx = 2;
			this.functionPanel.add(this.interactive, gbc.clone());
			
			this.layoutPanels();
		}
		
		void layoutPanels() {
			this.remove(this.contentPanel);
			this.remove(this.functionPanel);
			if (this.annotationSourcePanel != null) {
				this.contentPanel.add(this.annotationSourcePanel, BorderLayout.CENTER);
				this.contentPanel.add(this.functionPanel, BorderLayout.SOUTH);
				this.add(this.contentPanel, BorderLayout.SOUTH);
			} else {
				this.add(this.functionPanel, BorderLayout.SOUTH);
			}
			this.validate();
		}
		
		void selectAnnotator(String providerClassName) {
			AnnotationSourceManager asm = parent.getAnnotationSourceProvider(providerClassName);
			if (asm != null) {
				ResourceDialog rd = ResourceDialog.getResourceDialog(asm, ("Select " + asm.getResourceTypeLabel()), "Select");
				rd.setLocationRelativeTo(this);
				rd.setVisible(true);
				
				//	get annotator
				AnnotationSource as = asm.getAnnotationSource(rd.getSelectedResourceName());
				if (as != null) {
					this.annotationSourceName = as.getName();
					this.annotationSourceProviderClassName = as.getProviderClassName();
					this.annotationSourcePanel = asm.getAnnotatorParameterPanel();
					this.annotationSourceLabel.setText(as.getTypeLabel() + ": " + as.getName() + " (double click to edit)");
					this.layoutPanels();
					this.dirty = true;
				}
			}
		}
		
		void createAnnotator(String providerClassName) {
			AnnotationSourceManager asm = parent.getAnnotationSourceProvider(providerClassName);
			if (asm != null) {
				String asName = asm.createAnnotationSource();
				
				//	get filteredProcessor
				AnnotationSource as = asm.getAnnotationSource(asName);
				if (as != null) {
					this.annotationSourceName = as.getName();
					this.annotationSourceProviderClassName = as.getProviderClassName();
					this.annotationSourcePanel = asm.getAnnotatorParameterPanel();
					this.annotationSourceLabel.setText(as.getTypeLabel() + ": " + as.getName() + " (double click to edit)");
					this.layoutPanels();
					this.dirty = true;
				}
			}
		}
		
		boolean isDirty() {
			return (this.dirty || ((this.annotationSourcePanel != null) && this.annotationSourcePanel.isDirty()));
		}
		
		Settings getSettings() {
			if (this.annotationSourceName == null) return null;
			if (this.annotationSourceProviderClassName == null) return null;
			
			Settings set = new Settings();
			set.setSetting(AnnotatorDocumentProcessor.ANNOTATOR_NAME_ATTRIBUTE, this.annotationSourceName);
			set.setSetting(AnnotatorDocumentProcessor.PROVIDER_CLASS_NAME_ATTRIBUTE, this.annotationSourceProviderClassName);
			set.setSetting(AnnotatorDocumentProcessor.ANNOTATION_TYPE_ATTRIBUTE, this.annotationType.getText());
			if (this.interactive.isSelected()) set.setSetting(AnnotatorDocumentProcessor.INTERACTIVE_ATTRIBUTE, AnnotatorDocumentProcessor.INTERACTIVE_ATTRIBUTE);
			if (this.annotationSourcePanel != null) set.setSettings(this.annotationSourcePanel.getSettings());
			return set;
		}
	}
}
