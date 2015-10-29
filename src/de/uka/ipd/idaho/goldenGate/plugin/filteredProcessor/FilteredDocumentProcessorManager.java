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
package de.uka.ipd.idaho.goldenGate.plugin.filteredProcessor;


import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import de.uka.ipd.idaho.easyIO.settings.Settings;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.goldenGate.plugins.AbstractDocumentProcessorManager;
import de.uka.ipd.idaho.goldenGate.plugins.AnnotationFilter;
import de.uka.ipd.idaho.goldenGate.plugins.AnnotationFilterManager;
import de.uka.ipd.idaho.goldenGate.plugins.DocumentProcessor;
import de.uka.ipd.idaho.goldenGate.plugins.DocumentProcessorManager;
import de.uka.ipd.idaho.goldenGate.plugins.ResourceManager;
import de.uka.ipd.idaho.goldenGate.util.DataListListener;
import de.uka.ipd.idaho.goldenGate.util.DialogPanel;
import de.uka.ipd.idaho.goldenGate.util.ResourceDialog;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * Manager for filtered document processors. A filtered document processor wraps
 * another document processor in a way that the wrapped processor does not
 * process a document as a whole, but only selected parts of it.<br>
 * <br>
 * All configuration can be done in the 'Edit Filtered Document Processors'
 * dialog in the GoldenGATE Editor. Since this class makes use of the
 * AnnotationFilterManagers present in GoldenGATE, it will be of no use if there
 * are no AnnotationFilterManagers present in GoldenGATE.
 * 
 * @author sautter
 */
public class FilteredDocumentProcessorManager extends AbstractDocumentProcessorManager {
	
	private static final String FILE_EXTENSION = ".filteredDp";
	
	public FilteredDocumentProcessorManager() {}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractDocumentProcessorManager#getDocumentProcessor(java.lang.String)
	 */
	public DocumentProcessor getDocumentProcessor(String name) {
		Settings settings = this.getFilteredProcessor(name);
		return ((settings == null) ? null : this.getFilteredProcessor(name, settings));
	}
	
	private class FilteredDocumentProcessor implements DocumentProcessor {
		
		private static final String FILTER_NAME_ATTRIBUTE = "FILTER_NAME";
		private static final String FILTER_PROVIDER_CLASS_NAME_ATTRIBUTE = "FILTER_PROVIDER_CLASS";
		private static final String PROCESSOR_NAME_ATTRIBUTE = "PROCESSOR_NAME";
		private static final String PROCESSOR_PROVIDER_CLASS_NAME_ATTRIBUTE = "PROCESSOR_PROVIDER_CLASS";
		
		private String name;
		private AnnotationFilter filter;
		private DocumentProcessor processor;
		
		FilteredDocumentProcessor(String name, AnnotationFilter filter, DocumentProcessor processor) {
			this.name = name;
			this.processor = processor;
			this.filter = filter;
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
			return "Filtered DP";
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentProcessor#getProviderClassName()
		 */
		public String getProviderClassName() {
			return FilteredDocumentProcessorManager.class.getName();
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
			MutableAnnotation[] dataToProcess;
			if (this.filter == null) {
				dataToProcess = new MutableAnnotation[1];
				dataToProcess[0] = data;
			} else dataToProcess = this.filter.getMutableMatches(data);
			
			for (int d = 0; d < dataToProcess.length; d++)
				this.processor.process(dataToProcess[d], parameters);
		}
	}

	/* 
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentProcessorManager#createDocumentProcessor()
	 */
	public String createDocumentProcessor() {
		return this.createFilteredProcessor(new Settings(), null);
	}
	
	/* 
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentProcessorManager#editDocumentProcessor(java.lang.String)
	 */
	public void editDocumentProcessor(String name) {
		Settings set = this.loadSettingsResource(name);
		if ((set == null) || set.isEmpty()) return;
		
		EditFilteredProcessorDialog ead = new EditFilteredProcessorDialog(name, set);
		ead.setVisible(true);
		
		if (ead.isCommitted()) try {
			this.storeSettingsResource(name, ead.getFilteredProcessor());
		} catch (IOException ioe) {}
	}
	
	/* 
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentProcessorManager#editDocumentProcessors()
	 */
	public void editDocumentProcessors() {
		this.editFilteredProcessors();
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager#getDataNamesForResource(java.lang.String)
	 */
	public String[] getDataNamesForResource(String name) {
		StringVector names = new StringVector();
		names.addContentIgnoreDuplicates(super.getDataNamesForResource(name));
		Settings settings = this.getFilteredProcessor(name);
		if ((settings == null) || settings.isEmpty()) return names.toStringArray();
		AnnotationFilterManager afp = this.parent.getAnnotationFilterProvider(settings.getSetting(FilteredDocumentProcessor.FILTER_PROVIDER_CLASS_NAME_ATTRIBUTE));
		if (afp != null) // fetch data names for annotation source
			names.addContentIgnoreDuplicates(afp.getDataNamesForResource(settings.getSetting(FilteredDocumentProcessor.FILTER_NAME_ATTRIBUTE)));
		DocumentProcessorManager dpp = this.parent.getDocumentProcessorProvider(settings.getSetting(FilteredDocumentProcessor.PROCESSOR_PROVIDER_CLASS_NAME_ATTRIBUTE));
		if (dpp != null) // fetch data names for annotation source
			names.addContentIgnoreDuplicates(dpp.getDataNamesForResource(settings.getSetting(FilteredDocumentProcessor.PROCESSOR_NAME_ATTRIBUTE)));
		return names.toStringArray();
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.ResourceManager#getRequiredResourceNames(java.lang.String, boolean)
	 */
	public String[] getRequiredResourceNames(String name, boolean recourse) {
		StringVector nameCollector = new StringVector();
		Settings settings = this.getFilteredProcessor(name);
		
		String filterName = settings.getSetting(FilteredDocumentProcessor.FILTER_NAME_ATTRIBUTE);
		if (filterName != null)
			nameCollector.addElement(filterName + "@" + settings.getSetting(FilteredDocumentProcessor.FILTER_PROVIDER_CLASS_NAME_ATTRIBUTE));
		
		String processorName = settings.getSetting(FilteredDocumentProcessor.PROCESSOR_NAME_ATTRIBUTE);
		if (processorName != null)
			nameCollector.addElement(processorName + "@" + settings.getSetting(FilteredDocumentProcessor.PROCESSOR_PROVIDER_CLASS_NAME_ATTRIBUTE));
		
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
		return "Filtered DP";
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
				createFilteredProcessor();
			}
		});
		collector.add(mi);
		mi = new JMenuItem("Edit");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				editFilteredProcessors();
			}
		});
		collector.add(mi);
		return ((JMenuItem[]) collector.toArray(new JMenuItem[collector.size()]));
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#getMainMenuTitle()
	 */
	public String getMainMenuTitle() {
		return "Filtered DPs";
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager#getToolsMenuLabel()
	 */
	public String getToolsMenuLabel() {
		return "Run";
	}
	
	/* retrieve a FilteredProcessor by its name
	 * @param	name	the name of the reqired FilteredProcessor
	 * @return the FilteredProcessor with the required name, or null, if there is no such FilteredProcessor
	 */
	private Settings getFilteredProcessor(String name) {
		if (name == null) return null;
		return this.loadSettingsResource(name);
	}
	
	private FilteredDocumentProcessor getFilteredProcessor(String name, Settings settings) {
		if (settings == null) return null;
		AnnotationFilterManager afp = this.parent.getAnnotationFilterProvider(settings.getSetting(FilteredDocumentProcessor.FILTER_PROVIDER_CLASS_NAME_ATTRIBUTE));
		AnnotationFilter af = null;
		if (afp != null)
			af = afp.getAnnotationFilter(settings.getSetting(FilteredDocumentProcessor.FILTER_NAME_ATTRIBUTE));
		
		DocumentProcessorManager damp = this.parent.getDocumentProcessorProvider(settings.getSetting(FilteredDocumentProcessor.PROCESSOR_PROVIDER_CLASS_NAME_ATTRIBUTE));
		if (damp != null) {
			DocumentProcessor dp = damp.getDocumentProcessor(settings.getSetting(FilteredDocumentProcessor.PROCESSOR_NAME_ATTRIBUTE));
			if (dp != null) {
				return new FilteredDocumentProcessor(name, af, dp);
			} else return null;
		} else return null;
	}
	
	private boolean createFilteredProcessor() {
		return (this.createFilteredProcessor(new Settings(), "New FilteredDocumentProcessor") != null);
	}
	
	private boolean cloneFilteredProcessor() {
		String selectedName = this.resourceNameList.getSelectedName();
		if (selectedName == null)
			return this.createFilteredProcessor();
		else {
			String name = "New " + selectedName;
			return (this.createFilteredProcessor(this.loadSettingsResource(selectedName), name) != null);
		}
	}
	
	private String createFilteredProcessor(Settings set, String name) {
		CreateFilteredProcessorDialog cfd = new CreateFilteredProcessorDialog(name, set);
		cfd.setVisible(true);
		if (cfd.isCommitted()) {
			Settings filteredProcessor = cfd.getFilteredProcessor();
			String filteredProcessorName = cfd.getFilteredProcessorName();
			if (!filteredProcessorName.endsWith(FILE_EXTENSION)) filteredProcessorName += FILE_EXTENSION;
			try {
				if (this.storeSettingsResource(filteredProcessorName, filteredProcessor)) {
					this.resourceNameList.refresh();
					return filteredProcessorName;
				}
			} catch (IOException e) {}
		}
		return null;
	}
	
	private void editFilteredProcessors() {
		final FilteredProcessorEditorPanel[] editor = new FilteredProcessorEditorPanel[1];
		editor[0] = null;
		
		final DialogPanel editDialog = new DialogPanel("Edit Filtered Document Processors", true);
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
						storeSettingsResource(editor[0].fdpName, editor[0].getSettings());
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
				createFilteredProcessor();
			}
		});
		editButtons.add(button);
		button = new JButton("Clone");
		button.setBorder(BorderFactory.createRaisedBevelBorder());
		button.setPreferredSize(new Dimension(100, 21));
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				cloneFilteredProcessor();
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
				editor[0] = new FilteredProcessorEditorPanel(selectedName, set);
				editorPanel.add(editor[0], BorderLayout.CENTER);
			}
		}
		editDialog.add(editorPanel, BorderLayout.CENTER);
		
		editDialog.add(this.resourceNameList, BorderLayout.EAST);
		DataListListener dll = new DataListListener() {
			public void selected(String dataName) {
				if ((editor[0] != null) && editor[0].isDirty()) {
					try {
						storeSettingsResource(editor[0].fdpName, editor[0].getSettings());
					}
					catch (IOException ioe) {
						if (JOptionPane.showConfirmDialog(editDialog, (ioe.getClass().getName() + " (" + ioe.getMessage() + ")\nwhile saving file to " + editor[0].fdpName + "\nProceed?"), "Could Not Save Analyzer", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
							resourceNameList.setSelectedName(editor[0].fdpName);
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
						editor[0] = new FilteredProcessorEditorPanel(dataName, set);
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
	
	private class CreateFilteredProcessorDialog extends DialogPanel {
		
		private JTextField nameField;
		
		private FilteredProcessorEditorPanel editor;
		private Settings filteredProcessor = null;
		private String filteredProcessorName = null;
		
		CreateFilteredProcessorDialog(String name, Settings filteredProcessor) {
			super("Create Filtered Document Processor", true);
			
			this.nameField = new JTextField((name == null) ? "New FilteredDocumentProcessor" : name);
			this.nameField.setBorder(BorderFactory.createLoweredBevelBorder());
			
			//	initialize main buttons
			JButton commitButton = new JButton("Create");
			commitButton.setBorder(BorderFactory.createRaisedBevelBorder());
			commitButton.setPreferredSize(new Dimension(100, 21));
			commitButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					CreateFilteredProcessorDialog.this.filteredProcessor = editor.getSettings();
					filteredProcessorName = nameField.getText();
					dispose();
				}
			});
			
			JButton abortButton = new JButton("Cancel");
			abortButton.setBorder(BorderFactory.createRaisedBevelBorder());
			abortButton.setPreferredSize(new Dimension(100, 21));
			abortButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					CreateFilteredProcessorDialog.this.filteredProcessor = null;
					filteredProcessorName = null;
					dispose();
				}
			});
			
			JPanel mainButtonPanel = new JPanel();
			mainButtonPanel.setLayout(new FlowLayout());
			mainButtonPanel.add(commitButton);
			mainButtonPanel.add(abortButton);
			
			//	initialize editor
			this.editor = new FilteredProcessorEditorPanel(name, filteredProcessor);
			
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
			return (this.filteredProcessor != null);
		}
		
		Settings getFilteredProcessor() {
			return this.filteredProcessor;
		}
		
		String getFilteredProcessorName() {
			return this.filteredProcessorName;
		}
	}

	private class EditFilteredProcessorDialog extends DialogPanel {
		
		private FilteredProcessorEditorPanel editor;
		private Settings filteredProcessor = null;
		
		EditFilteredProcessorDialog(String name, Settings filteredProcessor) {
			super(("Edit Filtered Document Processor '" + name + "'"), true);
			
			//	initialize main buttons
			JButton commitButton = new JButton("OK");
			commitButton.setBorder(BorderFactory.createRaisedBevelBorder());
			commitButton.setPreferredSize(new Dimension(100, 21));
			commitButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					EditFilteredProcessorDialog.this.filteredProcessor = editor.getSettings();
					dispose();
				}
			});
			
			JButton abortButton = new JButton("Cancel");
			abortButton.setBorder(BorderFactory.createRaisedBevelBorder());
			abortButton.setPreferredSize(new Dimension(100, 21));
			abortButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					EditFilteredProcessorDialog.this.filteredProcessor = null;
					dispose();
				}
			});
			
			JPanel mainButtonPanel = new JPanel();
			mainButtonPanel.setLayout(new FlowLayout());
			mainButtonPanel.add(commitButton);
			mainButtonPanel.add(abortButton);
			
			//	initialize editor
			this.editor = new FilteredProcessorEditorPanel(name, filteredProcessor);
			
			//	put the whole stuff together
			this.setLayout(new BorderLayout());
			this.add(this.editor, BorderLayout.CENTER);
			this.add(mainButtonPanel, BorderLayout.SOUTH);
			
			this.setResizable(true);
			this.setSize(new Dimension(600, 400));
			this.setLocationRelativeTo(this.getOwner());
		}
		
		boolean isCommitted() {
			return (this.filteredProcessor != null);
		}
		
		Settings getFilteredProcessor() {
			return this.filteredProcessor;
		}
	}

	private class FilteredProcessorEditorPanel extends JPanel {
		
		private String filterName = null;
		private String filterProviderClassName = null;
		private JLabel filterLabel = new JLabel("<No Annotation Filter Selected>", JLabel.LEFT);
		
		private String processorName = null;
		private String processorProviderClassName = null;
		private JLabel processorLabel = new JLabel("<No Document Processor Selected>", JLabel.LEFT);
		
		private boolean dirty = false;
		
		private String fdpName;
		
		FilteredProcessorEditorPanel(String name, Settings settings) {
			super(new BorderLayout(), true);
			this.fdpName = name;
			this.add(getExplanationLabel(), BorderLayout.CENTER);
			
			this.filterName = settings.getSetting(FilteredDocumentProcessor.FILTER_NAME_ATTRIBUTE);
			this.filterProviderClassName = settings.getSetting(FilteredDocumentProcessor.FILTER_PROVIDER_CLASS_NAME_ATTRIBUTE);
			AnnotationFilterManager afm = parent.getAnnotationFilterProvider(this.filterProviderClassName);
			if (afm != null) {
				AnnotationFilter af = afm.getAnnotationFilter(this.filterName);
				if (af != null)
					this.filterLabel.setText(af.getTypeLabel() + ": " + af.getName() + " (double click to edit)");
			}
			this.filterLabel.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent me) {
					if (me.getClickCount() > 1) {
						AnnotationFilterManager afm = parent.getAnnotationFilterProvider(filterProviderClassName);
						if (afm != null) afm.editAnnotationFilter(filterName);
					}
				}
			});
			
			this.processorName = settings.getSetting(FilteredDocumentProcessor.PROCESSOR_NAME_ATTRIBUTE);
			this.processorProviderClassName = settings.getSetting(FilteredDocumentProcessor.PROCESSOR_PROVIDER_CLASS_NAME_ATTRIBUTE);
			DocumentProcessorManager dpm = parent.getDocumentProcessorProvider(this.processorProviderClassName);
			if (dpm != null) {
				DocumentProcessor dp = dpm.getDocumentProcessor(this.processorName);
				if (dp != null)
					this.processorLabel.setText(dp.getTypeLabel() + ": " + dp.getName() + " (double click to edit)");
			}
			this.processorLabel.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent me) {
					if (me.getClickCount() > 1) {
						DocumentProcessorManager dpm = parent.getDocumentProcessorProvider(processorProviderClassName);
						if (dpm != null) dpm.editDocumentProcessor(processorName);
					}
				}
			});
			
			JPanel functionPanel = new JPanel(new GridBagLayout(), true);
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
			functionPanel.add(this.filterLabel, gbc.clone());
			
			gbc.gridy++;
			AnnotationFilterManager[] afms = parent.getAnnotationFilterProviders();
			JButton button;
			for (int a = 0; a < afms.length; a++) {
				final String className = afms[a].getClass().getName();
				
				button = new JButton("Use " + afms[a].getResourceTypeLabel() + " Filter");
				button.setBorder(BorderFactory.createRaisedBevelBorder());
				button.setSize(new Dimension(120, 21));
				button.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						selectFilter(className);
					}
				});
				
				gbc.gridx = 0;
				gbc.weightx = 1;
				gbc.gridwidth = 2;
				functionPanel.add(button, gbc.clone());
				
				button = new JButton("Create " + afms[a].getResourceTypeLabel() + " Filter");
				button.setBorder(BorderFactory.createRaisedBevelBorder());
				button.setSize(new Dimension(120, 21));
				button.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						createFilter(className);
					}
				});
				
				gbc.gridx = 2;
				gbc.weightx = 0;
				gbc.gridwidth = 1;
				functionPanel.add(button, gbc.clone());
				
				gbc.gridy++;
			}
			
			gbc.gridx = 0;
			gbc.weightx = 3;
			gbc.gridwidth = 3;
			functionPanel.add(this.processorLabel, gbc.clone());
			
			DocumentProcessorManager[] dpms = parent.getDocumentProcessorProviders();
			for (int p = 0; p < dpms.length; p++) {
				final String className = dpms[p].getClass().getName();
				gbc.gridy++;
				
				button = new JButton("Use " + dpms[p].getResourceTypeLabel());
				button.setBorder(BorderFactory.createRaisedBevelBorder());
				button.setSize(new Dimension(120, 21));
				button.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						selectProcessor(className);
					}
				});
				
				gbc.gridx = 0;
				gbc.weightx = 1;
				gbc.gridwidth = 2;
				functionPanel.add(button, gbc.clone());
				
				button = new JButton("Create " + dpms[p].getResourceTypeLabel());
				button.setBorder(BorderFactory.createRaisedBevelBorder());
				button.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						createProcessor(className);
					}
				});
				
				gbc.gridx = 2;
				gbc.weightx = 0;
				gbc.gridwidth = 1;
				functionPanel.add(button, gbc.clone());
			}
			
			this.add(functionPanel, BorderLayout.SOUTH);
		}
		
		void selectFilter(String providerClassName) {
			AnnotationFilterManager afm = parent.getAnnotationFilterProvider(providerClassName);
			if (afm != null) {
				ResourceDialog rd = ResourceDialog.getResourceDialog(afm, ("Select " + afm.getResourceTypeLabel()), "Select");
				rd.setLocationRelativeTo(DialogPanel.getTopWindow());
				rd.setVisible(true);
				
				//	get filteredProcessor
				AnnotationFilter af = afm.getAnnotationFilter(rd.getSelectedResourceName());
				if (af != null) {
					this.filterName = af.getName();
					this.filterProviderClassName = af.getProviderClassName();
					this.filterLabel.setText(af.getTypeLabel() + ": " + af.getName() + " (double click to edit)");
					this.dirty = true;
				}
			}
		}
		
		void createFilter(String providerClassName) {
			AnnotationFilterManager afm = parent.getAnnotationFilterProvider(providerClassName);
			if (afm != null) {
				String afName = afm.createAnnotationFilter();
				
				//	get filteredProcessor
				AnnotationFilter af = afm.getAnnotationFilter(afName);
				if (af != null) {
					this.filterName = af.getName();
					this.filterProviderClassName = af.getProviderClassName();
					this.filterLabel.setText(af.getTypeLabel() + ": " + af.getName() + " (double click to edit)");
					this.dirty = true;
				}
			}
		}
		
		void selectProcessor(String providerClassName) {
			DocumentProcessorManager dpm = parent.getDocumentProcessorProvider(providerClassName);
			if (dpm != null) {
				ResourceDialog rd = ResourceDialog.getResourceDialog(dpm, ("Select " + dpm.getResourceTypeLabel()), "Select");
				rd.setLocationRelativeTo(DialogPanel.getTopWindow());
				rd.setVisible(true);
				
				//	get filteredProcessor
				DocumentProcessor dp = dpm.getDocumentProcessor(rd.getSelectedResourceName());
				if (dp != null) {
					this.processorName = dp.getName();
					this.processorProviderClassName = dp.getProviderClassName();
					this.processorLabel.setText(dp.getTypeLabel() + ": " + dp.getName() + " (double click to edit)");
					this.dirty = true;
				}
			}
		}
		
		void createProcessor(String providerClassName) {
			DocumentProcessorManager dpm = parent.getDocumentProcessorProvider(providerClassName);
			if (dpm != null) {
				String dpName = dpm.createDocumentProcessor();
				
				//	get filteredProcessor
				DocumentProcessor dp = dpm.getDocumentProcessor(dpName);
				if (dp != null) {
					this.processorName = dp.getName();
					this.processorProviderClassName = dp.getProviderClassName();
					this.processorLabel.setText(dp.getTypeLabel() + ": " + dp.getName() + " (double click to edit)");
					this.dirty = true;
				}
			}
		}
		
		boolean isDirty() {
			return (this.dirty);
		}
		
		Settings getSettings() {
			if (this.processorName == null) return null;
			if (this.processorProviderClassName == null) return null;
			
			Settings set = new Settings();
			set.setSetting(FilteredDocumentProcessor.PROCESSOR_NAME_ATTRIBUTE, this.processorName);
			set.setSetting(FilteredDocumentProcessor.PROCESSOR_PROVIDER_CLASS_NAME_ATTRIBUTE, this.processorProviderClassName);
			
			if ((this.filterName != null) && (this.filterProviderClassName != null)){
				set.setSetting(FilteredDocumentProcessor.FILTER_NAME_ATTRIBUTE, this.filterName);
				set.setSetting(FilteredDocumentProcessor.FILTER_PROVIDER_CLASS_NAME_ATTRIBUTE, this.filterProviderClassName);
			}
			return set;
		}
	}
}
