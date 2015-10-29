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
package de.uka.ipd.idaho.goldenGate.plugin.pipelines;


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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.event.ListDataListener;

import de.uka.ipd.idaho.easyIO.settings.Settings;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.util.ProgressMonitor;
import de.uka.ipd.idaho.gamta.util.swing.ProgressMonitorWindow;
import de.uka.ipd.idaho.goldenGate.DocumentEditor;
import de.uka.ipd.idaho.goldenGate.DocumentEditorDialog;
import de.uka.ipd.idaho.goldenGate.GoldenGATE;
import de.uka.ipd.idaho.goldenGate.plugins.AbstractDocumentProcessorManager;
import de.uka.ipd.idaho.goldenGate.plugins.DocumentProcessor;
import de.uka.ipd.idaho.goldenGate.plugins.DocumentProcessorManager;
import de.uka.ipd.idaho.goldenGate.plugins.MonitorableDocumentProcessor;
import de.uka.ipd.idaho.goldenGate.plugins.Resource;
import de.uka.ipd.idaho.goldenGate.plugins.ResourceManager;
import de.uka.ipd.idaho.goldenGate.plugins.ResourceSplashScreen;
import de.uka.ipd.idaho.goldenGate.util.DataListListener;
import de.uka.ipd.idaho.goldenGate.util.DialogPanel;
import de.uka.ipd.idaho.goldenGate.util.ResourceDialog;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * Manager for pipeline document processors. A pipeline wraps other document
 * processors, making them accessible as one, always executing in the same
 * order, one after another. Pipelines can have different interactivity levels,
 * specifying if and in which states of the execution user interaction is
 * permitted:
 * <ul>
 * <li><b>Feedback / Editing after each step</b>: wrapped document processors
 * may prompt users with dialogs, and the document is opened in an editing
 * dialog after each wrapped document processor has been executed. Good mode for
 * testing.</li>
 * <li><b>Feedback / Editing of result</b>: wrapped document processors may
 * prompt users with dialogs, and the document is opened in an editing dialog
 * after all the wrapped document processor have been executed.</li>
 * <li><b>Feedback only</b>: wrapped document processors may prompt users with
 * dialogs, but the document is not opened in an editing dialog any time.</li>
 * <li><b>Editing of result</b>: wrapped document processors must not prompt
 * users with dialogs, but the document is opened in an editing dialog after all
 * the wrapped document processor have been executed.</li>
 * <li><b>No iteractivity</b>: wrapped document processors must not prompt
 * users with dialogs, and the document is never opened in an editing dialog.</li>
 * </ul>
 * All configuration can be done in the 'Edit Pipelines' dialog in the
 * GoldenGATE Editor. This includes adding and removing document processors to
 * and from the pipeline, as well as changing the wrapped document processors'
 * order.
 * 
 * @author sautter
 */
public class PipelineManager extends AbstractDocumentProcessorManager {
	
	private static final String FILE_EXTENSION = ".pipeline";
	
	public PipelineManager() {}
	
	/**
	 * Overwrites the implementation from AbstractDocumentProcessorManager in a
	 * way that provides a specialized ResourceSplashScreen. This specialized
	 * splash screen is connected to the pipeline, and displays the name of the
	 * document processor wrapped in the pipeline and currently executing,
	 * instead of the name of the pipeline itself.
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractDocumentProcessorManager#applyDocumentProcessor(java.lang.String, de.uka.ipd.idaho.goldenGate.DocumentEditor, java.util.Properties)
	 */
	public void applyDocumentProcessor(String processorName, DocumentEditor data, Properties parameters) {
		String pn = processorName;
		
		//	select processor if not specified
		if (pn == null) {
			ResourceDialog rd = ResourceDialog.getResourceDialog(this, (this.getToolsMenuLabel() + " " + this.getResourceTypeLabel()), this.getToolsMenuLabel());
			rd.setLocationRelativeTo(DialogPanel.getTopWindow());
			rd.setVisible(true);
			if (rd.isCommitted()) pn = rd.getSelectedResourceName();
		}
		
		//	get pipeline
		Pipeline pipeline = this.getPipeline(pn);
		if (pipeline != null) {
			
			//	apply processor
			ResourceSplashScreen splashScreen = new ResourceSplashScreen((this.getResourceTypeLabel() + " Running ..."), ("Please wait while '" + this.getResourceTypeLabel() + ": " + pn + "' is processing the Document ..."));
			DocumentProcessor dp = new PipelineDocumentProcessor(splashScreen, pn, pipeline);
			data.applyDocumentProcessor(dp, splashScreen, parameters);
		}
	}
	
	/* 
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentProcessorManager#createDocumentProcessor()
	 */
	public String createDocumentProcessor() {
		return this.createPipeline(new Settings(), null);
	}
	
	/* 
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentProcessorManager#editDocumentProcessor(java.lang.String)
	 */
	public void editDocumentProcessor(String name) {
		Pipeline pipeline = this.getPipeline(name);
		if (pipeline == null) return;
		
		EditPipelineDialog epd = new EditPipelineDialog(pipeline, name);
		epd.setVisible(true);
		
		if (epd.isCommitted()) try {
			this.storeSettingsResource(name, epd.getPipeline());
		}
		catch (IOException ioe) {
			System.out.println("Exception storing pipeline '" + name + "': " + ioe.getMessage());
			ioe.printStackTrace(System.out);
		}
	}
	
	/* 
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentProcessorManager#editDocumentProcessors()
	 */
	public void editDocumentProcessors() {
		this.editPipelines();
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractDocumentProcessorManager#getDocumentProcessor(java.lang.String)
	 */
	public DocumentProcessor getDocumentProcessor(String name) {
		Pipeline pipeline = this.getPipeline(name);
		return ((pipeline == null) ? null : new PipelineDocumentProcessor(null, name, pipeline));
	}
	
	private class Pipeline {
		
		private static final String PART_NAME_ATTRIBUTE = "PART_NAME";
		private static final String PART_PROVIDER_CLASS_NAME_ATTRIBUTE = "PROVIDER_CLASS";
		
		private static final String INTERACTIVITY_LEVEL_ATTRIBUTE = "INTERACTIVITY_LEVEL";
		
		private static final String FULL_INTERACTIVITY_LEVEL = "Feedback / Editing after each step";
		private static final String FEEDBACK_END_INTERACTIVITY_LEVEL = "Feedback / Editing of result";
		private static final String FEEDBACK_INTERACTIVITY_LEVEL = "Feedback only";
		private static final String END_INTERACTIVITY_LEVEL = "Editing of result only";
		private static final String NO_INTERACTIVITY_LEVEL = "No interactivity";
		
		private static final String DEFAULT_INTERACTIVITY_LEVEL = FEEDBACK_END_INTERACTIVITY_LEVEL;
		
		private Vector parts = new Vector();
		private String interactivityLevel = FEEDBACK_END_INTERACTIVITY_LEVEL;
		
		Pipeline(String interactivityLevel) {
			this.interactivityLevel = interactivityLevel;
		}
		
		DocumentProcessor[] getParts() {
			return ((DocumentProcessor[]) this.parts.toArray(new DocumentProcessor[this.parts.size()]));
		}
		
		String getInteractivityLevel() {
			return this.interactivityLevel;
		}
		
		boolean allowFeedback() {
			return (FULL_INTERACTIVITY_LEVEL.equals(this.interactivityLevel) || FEEDBACK_END_INTERACTIVITY_LEVEL.equals(this.interactivityLevel) || FEEDBACK_INTERACTIVITY_LEVEL.equals(this.interactivityLevel));
		}
		
		boolean editAfterStep() {
			return FULL_INTERACTIVITY_LEVEL.equals(this.interactivityLevel);
		}
		
		boolean editResult() {
			return (FULL_INTERACTIVITY_LEVEL.equals(this.interactivityLevel) || FEEDBACK_END_INTERACTIVITY_LEVEL.equals(this.interactivityLevel) || END_INTERACTIVITY_LEVEL.equals(this.interactivityLevel));
		}
		
		private void addPart(DocumentProcessor part) {
			this.parts.add(part);
		}
	}

	private class PipelineDocumentProcessor implements MonitorableDocumentProcessor {
		
		private String name;
		private Pipeline pipeline;
		
		private ProgressMonitor splashScreen;
		
		PipelineDocumentProcessor(ResourceSplashScreen splashScreen, String name, Pipeline pipeline) {
			this.splashScreen = splashScreen;
			this.name = name;
			this.pipeline = pipeline;
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
			return "Pipeline";
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentProcessor#getProviderClassName()
		 */
		public String getProviderClassName() {
			return PipelineManager.class.getName();
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentProcessor#process(de.uka.ipd.idaho.gamta.MutableAnnotation)
		 */
		public void process(MutableAnnotation data) {
			Properties parameters = new Properties();
			parameters.setProperty(Resource.INTERACTIVE_PARAMETER, Resource.INTERACTIVE_PARAMETER);
			this.process(data, parameters, this.splashScreen);
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentProcessor#process(de.uka.ipd.idaho.gamta.MutableAnnotation, boolean)
		 */
		public void process(MutableAnnotation data, Properties parameters) {
			this.process(data, parameters, this.splashScreen);
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGate.plugins.MonitorableDocumentProcessor#process(de.uka.ipd.idaho.gamta.MutableAnnotation, java.util.Properties, de.uka.ipd.idaho.gamta.util.ProgressMonitor)
		 */
		public void process(MutableAnnotation data, Properties parameters, ProgressMonitor pm) {
			ProgressMonitorWindow pmw = null;
			if (pm instanceof ProgressMonitorWindow)
				pmw = ((ProgressMonitorWindow) pm);
			else if (this.splashScreen instanceof ProgressMonitorWindow)
				pmw = ((ProgressMonitorWindow) this.splashScreen);
			this.process(data, (parameters.containsKey(INTERACTIVE_PARAMETER) && this.pipeline.allowFeedback()), (parameters.containsKey(INTERACTIVE_PARAMETER) && this.pipeline.editAfterStep()), (parameters.containsKey(INTERACTIVE_PARAMETER) && this.pipeline.editResult()), ((pmw == null) ? pm : pmw), new HashSet(), parameters, true);
		}
		
		private void process(MutableAnnotation data, boolean allowFeedback, boolean editAfterStep, boolean editResult, ProgressMonitor splashScreen, HashSet activePipelines, Properties parameters, boolean isTopInvocation) {
			DocumentProcessor[] parts = this.pipeline.getParts();
			activePipelines.add(this.getName().toLowerCase());
			
			for (int p = 0; p < parts.length; p++) {
				DocumentProcessor part = parts[p];
				if (splashScreen != null) {
					if (splashScreen instanceof ResourceSplashScreen)
						((ResourceSplashScreen) splashScreen).setText("Current Processor is " + part.getName());
					if (isTopInvocation) {
						splashScreen.setBaseProgress((100 * p) / parts.length);
						splashScreen.setMaxProgress((100 * (p+1)) / parts.length);
						splashScreen.setProgress(0);
					}
					else splashScreen.setProgress((100 * p) / parts.length);
				}
				
				if (part instanceof PipelineDocumentProcessor) {
					if (!activePipelines.contains(part.getName().toLowerCase())) //	catch cycles
						((PipelineDocumentProcessor) part).process(data, allowFeedback, false, false, splashScreen, activePipelines, parameters, false);
				}
				else {
					Properties subParameters = new Properties();
					Iterator it = parameters.keySet().iterator();
					while (it.hasNext()) {
						String key = it.next().toString();
						if ((allowFeedback && this.pipeline.allowFeedback()) || !INTERACTIVE_PARAMETER.equals(key))
							subParameters.setProperty(key, parameters.getProperty(key));
					}
					if (part instanceof MonitorableDocumentProcessor)
						((MonitorableDocumentProcessor) part).process(data, subParameters, splashScreen);
					else part.process(data, subParameters);
				}
				
				if ((editAfterStep && this.pipeline.editAfterStep()) || (editResult && ((p + 1) == parts.length) && this.pipeline.editResult())) {
					DocumentEditDialog ded;
					if (splashScreen instanceof ProgressMonitorWindow)
						ded = new DocumentEditDialog(((ProgressMonitorWindow) splashScreen).getWindow(), parent, "Edit Document", data, ((p + 1) == parts.length));
					else ded = new DocumentEditDialog("Edit Document", data, ((p + 1) == parts.length));
					ded.setVisible(true);
					if (ded.stopPipeline)
						return;
				}
				
				if (splashScreen != null)
					splashScreen.setProgress((100 * (p+1)) / parts.length);
			}
			
			activePipelines.remove(this.getName().toLowerCase());
		}
		
		private class DocumentEditDialog extends DocumentEditorDialog {
			
			private boolean stopPipeline = false;
			
			DocumentEditDialog(String title, MutableAnnotation docPart, boolean isFinalDialog) {
				super(PipelineManager.this.parent, title, docPart);
				this.init(isFinalDialog);
			}
			
			DocumentEditDialog(Window owner, GoldenGATE host, String title, MutableAnnotation docPart, boolean isFinalDialog) {
				super(owner, host, title, docPart);
				this.host = host;
				this.init(isFinalDialog);
			}
			
			void init(boolean isFinalDialog) {
				
				//	initialize main buttons
				JButton commitButton = new JButton(isFinalDialog ? "Finished" : "Proceed");
				commitButton.setBorder(BorderFactory.createRaisedBevelBorder());
				commitButton.setPreferredSize(new Dimension(100, 21));
				commitButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						commit();
					}
				});
				this.mainButtonPanel.add(commitButton);
				
				if (!isFinalDialog) {
					JButton abortButton = new JButton("Stop");
					abortButton.setBorder(BorderFactory.createRaisedBevelBorder());
					abortButton.setPreferredSize(new Dimension(100, 21));
					abortButton.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							abort();
						}
					});
					this.mainButtonPanel.add(abortButton);
				}
				
				this.setResizable(true);
				this.setSize(new Dimension(800, 600));
				this.setLocationRelativeTo(DialogPanel.getTopWindow());
			}
			
			void abort() {
				if (JOptionPane.showConfirmDialog(this, "Really interrupt Pipeline?", "Confirm Interrupt Pipeline", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
					this.stopPipeline = true;
					this.dispose();
				}
			}
			
			void commit() {
				this.documentEditor.writeChanges();
				this.dispose();
			}
		}
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager#getDataNamesForResource(java.lang.String)
	 */
	public String[] getDataNamesForResource(String name) {
		StringVector names = new StringVector();
		names.addContentIgnoreDuplicates(super.getDataNamesForResource(name));
		
		Pipeline pipeline = this.getPipeline(name);
		if (pipeline == null) return names.toStringArray();
		
		DocumentProcessor[] parts = pipeline.getParts();
		for (int p = 0; p < parts.length; p++) {
			DocumentProcessorManager dpm = this.parent.getDocumentProcessorProvider(parts[p].getProviderClassName());
			if (dpm != null) names.addContentIgnoreDuplicates(dpm.getDataNamesForResource(parts[p].getName()));
		}
		
		return names.toStringArray();
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.ResourceManager#getRequiredResourceNames(java.lang.String, boolean)
	 */
	public String[] getRequiredResourceNames(String name, boolean recourse) {
		Pipeline pipeline = this.getPipeline(name);
		if (pipeline == null) return new String[0];
		
		StringVector nameCollector = new StringVector();
		
		DocumentProcessor[] parts = pipeline.getParts();
		for (int p = 0; p < parts.length; p++)
			nameCollector.addElementIgnoreDuplicates(parts[p].getName() + "@" + parts[p].getProviderClassName());
		
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
		return "Pipeline";
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
				createPipeline();
			}
		});
		collector.add(mi);
		mi = new JMenuItem("Edit");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				editPipelines();
			}
		});
		collector.add(mi);
		return ((JMenuItem[]) collector.toArray(new JMenuItem[collector.size()]));
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#getMainMenuTitle()
	 */
	public String getMainMenuTitle() {
		return "Pipelines";
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager#getToolsMenuLabel()
	 */
	public String getToolsMenuLabel() {
		return "Run";
	}
	
	/* retrieve a Pipeline by its name
	 * @param	name	the name of the reqired Pipeline
	 * @return the Pipeline with the required name, or null, if there is no such Pipeline
	 */
	private Pipeline getPipeline(String name) {
		if (name == null) return null;
		return this.getPipeline(this.loadSettingsResource(name));
	}
	
	private Pipeline getPipeline(Settings settings) {
		if (settings == null)
			return null;
		try {
			String interactivityLevel = settings.getSetting(Pipeline.INTERACTIVITY_LEVEL_ATTRIBUTE, Pipeline.DEFAULT_INTERACTIVITY_LEVEL);
			Pipeline pipeline = new Pipeline(interactivityLevel);
			for (int s = 0; s < settings.size(); s++) {
				Settings partSettings = settings.getSubset("PART_" + s);
				String partName = partSettings.getSetting(Pipeline.PART_NAME_ATTRIBUTE);
				String providerClassName = partSettings.getSetting(Pipeline.PART_PROVIDER_CLASS_NAME_ATTRIBUTE);
				DocumentProcessorManager dpmp = this.parent.getDocumentProcessorProvider(providerClassName);
				if (dpmp != null) {
					DocumentProcessor dp = dpmp.getDocumentProcessor(partName);
					if (dp != null) pipeline.addPart(dp);
				}
			}
			return pipeline;
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	private boolean createPipeline() {
		return (this.createPipeline(new Settings(), null) != null);
	}
	
	private boolean clonePipeline() {
		String selectedName = this.resourceNameList.getSelectedName();
		if (selectedName == null)
			return this.createPipeline();
		else {
			String name = "New " + selectedName;
			return (this.createPipeline(this.loadSettingsResource(selectedName), name) != null);
		}
	}
	
	private String createPipeline(Settings modelPipeline, String name) {
		CreatePipelineDialog cpd = new CreatePipelineDialog(this.getPipeline(modelPipeline), name);
		cpd.setVisible(true);
		
		if (cpd.isCommitted()) {
			Settings pipeline = cpd.getPipeline();
			String pipelineName = cpd.getPipelineName();
			if (!pipelineName.endsWith(FILE_EXTENSION)) pipelineName += FILE_EXTENSION;
			try {
				if (this.storeSettingsResource(pipelineName, pipeline)) {
					this.resourceNameList.refresh();
					return pipelineName;
				}
			} catch (IOException e) {}
		}
		return null;
	}
	
	private void editPipelines() {
		final PipelineEditorPanel[] editor = new PipelineEditorPanel[1];
		editor[0] = null;
		
		final DialogPanel editDialog = new DialogPanel("Edit Pipelines", true);
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
			public void actionPerformed(ActionEvent ae) {
				createPipeline();
			}
		});
		editButtons.add(button);
		button = new JButton("Clone");
		button.setBorder(BorderFactory.createRaisedBevelBorder());
		button.setPreferredSize(new Dimension(100, 21));
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				clonePipeline();
			}
		});
		editButtons.add(button);
		button = new JButton("Delete");
		button.setBorder(BorderFactory.createRaisedBevelBorder());
		button.setPreferredSize(new Dimension(100, 21));
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
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
				editor[0] = new PipelineEditorPanel(selectedName, this.getPipeline(set));
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
						editor[0] = new PipelineEditorPanel(dataName, getPipeline(set));
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
	
	private boolean hasCycle(String pipelineName, DocumentProcessor newPart) {
		if ((newPart == null) || !(newPart instanceof PipelineDocumentProcessor)) return false;
		if ((pipelineName != null) && pipelineName.equals(newPart.getName())) return true;
		Pipeline pipeline = this.getPipeline(newPart.getName());
		if (pipeline == null) return false;
		
		DocumentProcessor[] parts = pipeline.getParts();
		for (int p = 0; p < parts.length; p++)
			if (hasCycle(pipelineName, parts[p])) return true;
		
		return false;
	}
	
	private class CreatePipelineDialog extends DialogPanel {
		
		private JTextField nameField;
		
		private PipelineEditorPanel editor;
		private String pipelineName = null;
		
		CreatePipelineDialog(Pipeline pipeline, String name) {
			super("Create Pipeline", true);
			
			this.nameField = new JTextField((name == null) ? "New Pipeline" : name);
			this.nameField.setBorder(BorderFactory.createLoweredBevelBorder());
			
			//	initialize main buttons
			JButton commitButton = new JButton("Create");
			commitButton.setBorder(BorderFactory.createRaisedBevelBorder());
			commitButton.setPreferredSize(new Dimension(100, 21));
			commitButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					pipelineName = nameField.getText();
					dispose();
				}
			});
			
			JButton abortButton = new JButton("Cancel");
			abortButton.setBorder(BorderFactory.createRaisedBevelBorder());
			abortButton.setPreferredSize(new Dimension(100, 21));
			abortButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					pipelineName = null;
					dispose();
				}
			});
			
			JPanel mainButtonPanel = new JPanel();
			mainButtonPanel.setLayout(new FlowLayout());
			mainButtonPanel.add(commitButton);
			mainButtonPanel.add(abortButton);
			
			//	initialize editor
			this.editor = new PipelineEditorPanel(name, pipeline);
			
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
			return (this.pipelineName != null);
		}
		
		Settings getPipeline() {
			return this.editor.getSettings();
		}
		
		String getPipelineName() {
			return this.pipelineName;
		}
	}

	private class EditPipelineDialog extends DialogPanel {
		
		private PipelineEditorPanel editor;
		private String pipelineName = null;
		
		EditPipelineDialog(Pipeline pipeline, String name) {
			super(("Edit Pipeline '" + name + "'"), true);
			this.pipelineName = name;
			
			//	initialize main buttons
			JButton commitButton = new JButton("OK");
			commitButton.setBorder(BorderFactory.createRaisedBevelBorder());
			commitButton.setPreferredSize(new Dimension(100, 21));
			commitButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					dispose();
				}
			});
			
			JButton abortButton = new JButton("Cancel");
			abortButton.setBorder(BorderFactory.createRaisedBevelBorder());
			abortButton.setPreferredSize(new Dimension(100, 21));
			abortButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					pipelineName = null;
					dispose();
				}
			});
			
			JPanel mainButtonPanel = new JPanel();
			mainButtonPanel.setLayout(new FlowLayout());
			mainButtonPanel.add(commitButton);
			mainButtonPanel.add(abortButton);
			
			//	initialize editor
			this.editor = new PipelineEditorPanel(name, pipeline);
			
			//	put the whole stuff together
			this.setLayout(new BorderLayout());
			this.add(this.editor, BorderLayout.CENTER);
			this.add(mainButtonPanel, BorderLayout.SOUTH);
			
			this.setResizable(true);
			this.setSize(new Dimension(600, 400));
			this.setLocationRelativeTo(DialogPanel.getTopWindow());
		}
		
		boolean isCommitted() {
			return (this.pipelineName != null);
		}
		
		Settings getPipeline() {
			return this.editor.getSettings();
		}
	}

	private class PipelineEditorPanel extends JPanel {
		
		private String name;
		
		private Vector parts = new Vector();
		private JList partList;
		
		private JComboBox interactivityLevel = new JComboBox();
		
		private boolean dirty = false;
		
//		PipelineEditorPanel(String name) {
//			this(name, null);
//		}
//		
		PipelineEditorPanel(String name, Pipeline pipeline) {
			super(new BorderLayout(), true);
			this.name = name;
			
			if (pipeline != null) {
				DocumentProcessor[] parts = pipeline.getParts();
				for (int p = 0; p < parts.length; p++)
					this.parts.add(parts[p]);
			}
			
			this.partList = new JList(new PipelinePartListModel(this.parts));
			JScrollPane partListBox = new JScrollPane(this.partList);
			this.add(partListBox, BorderLayout.CENTER);
			
			JButton upButton = new JButton("Up");
			upButton.setBorder(BorderFactory.createRaisedBevelBorder());
			upButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					moveUp();
				}
			});
			JButton editButton = new JButton("Edit");
			editButton.setBorder(BorderFactory.createRaisedBevelBorder());
			editButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					editPart();
				}
			});
			JButton downButton = new JButton("Down");
			downButton.setBorder(BorderFactory.createRaisedBevelBorder());
			downButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					moveDown();
				}
			});
			JPanel reorderButtonPanel = new JPanel(new GridBagLayout());
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.insets.top = 2;
			gbc.insets.bottom = 2;
			gbc.insets.left = 5;
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
			reorderButtonPanel.add(editButton, gbc.clone());
			gbc.gridy = 2;
			reorderButtonPanel.add(downButton, gbc.clone());
			
			this.add(reorderButtonPanel, BorderLayout.WEST);
			
			JButton removePartButton = new JButton("Remove");
			removePartButton.setBorder(BorderFactory.createRaisedBevelBorder());
			removePartButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					removePart();
				}
			});
			
			this.interactivityLevel.addItem(Pipeline.FULL_INTERACTIVITY_LEVEL);
			this.interactivityLevel.addItem(Pipeline.FEEDBACK_END_INTERACTIVITY_LEVEL);
			this.interactivityLevel.addItem(Pipeline.FEEDBACK_INTERACTIVITY_LEVEL);
			this.interactivityLevel.addItem(Pipeline.END_INTERACTIVITY_LEVEL);
			this.interactivityLevel.addItem(Pipeline.NO_INTERACTIVITY_LEVEL);
			this.interactivityLevel.setEditable(false);
			this.interactivityLevel.setSelectedItem((pipeline == null) ? Pipeline.DEFAULT_INTERACTIVITY_LEVEL : pipeline.getInteractivityLevel());
			this.interactivityLevel.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent ie) {
					dirty = true;
				}
			});
			
			JPanel managerButtonPanel = new JPanel(new GridBagLayout());
			gbc.insets.top = 3;
			gbc.insets.bottom = 2;
			gbc.insets.left = 5;
			gbc.insets.right = 5;
			gbc.weighty = 0;
			gbc.gridheight = 1;
			gbc.fill = GridBagConstraints.BOTH;
			gbc.gridx = 0;
			gbc.gridwidth = 3;
			
			gbc.gridy = 0;
			gbc.gridx = 0;
			managerButtonPanel.add(removePartButton, gbc.clone());
			
			gbc.insets.top = 2;
			DocumentProcessorManager[] dpms = parent.getDocumentProcessorProviders();
			JButton button;
			for (int a = 0; a < dpms.length; a++) {
				final String className = dpms[a].getClass().getName();
				gbc.gridy++;
				
				button = new JButton("Add " + dpms[a].getResourceTypeLabel());
				button.setBorder(BorderFactory.createRaisedBevelBorder());
				button.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						addProcessor(className);
					}
				});
				
				gbc.gridx = 0;
				gbc.weightx = 1;
				gbc.gridwidth = 2;
				managerButtonPanel.add(button, gbc.clone());
				
				button = new JButton("Create " + dpms[a].getResourceTypeLabel());
				button.setBorder(BorderFactory.createRaisedBevelBorder());
				button.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						createProcessor(className);
					}
				});
				
				gbc.gridx = 2;
				gbc.weightx = 0;
				gbc.gridwidth = 1;
				managerButtonPanel.add(button, gbc.clone());
			}
			
			gbc.gridy ++;
			gbc.gridx = 0;
			gbc.weightx = 0;
			gbc.gridwidth = 1;
			managerButtonPanel.add(new JLabel("Interactivity Level", JLabel.LEFT), gbc.clone());
			gbc.gridx = 1;
			gbc.weightx = 1;
			gbc.gridwidth = 2;
			managerButtonPanel.add(this.interactivityLevel, gbc.clone());
			
			this.add(managerButtonPanel, BorderLayout.SOUTH);
		}
		
		void addProcessor(String providerClassName) {
			DocumentProcessorManager dpmp = parent.getDocumentProcessorProvider(providerClassName);
			if (dpmp != null) {
				ResourceDialog rd = ResourceDialog.getResourceDialog(dpmp, ("Select " + dpmp.getResourceTypeLabel()), "Select");
				rd.setLocationRelativeTo(DialogPanel.getTopWindow());
				rd.setVisible(true);
				
				//	get annotator
				DocumentProcessor dp = dpmp.getDocumentProcessor(rd.getSelectedResourceName());
				if (dp != null) {
					if (hasCycle(this.name, dp)) {
						JOptionPane.showMessageDialog(this, "The selected Pipeline cannot be part of this Pipeline due to a circular reference.", "Cycle Detected", JOptionPane.ERROR_MESSAGE);
					}
					else {
						this.parts.add(dp);
						this.partList.setModel(new PipelinePartListModel(this.parts));
						this.dirty = true;
					}
				}
			}
		}
		
		void createProcessor(String providerClassName) {
			DocumentProcessorManager dpm = parent.getDocumentProcessorProvider(providerClassName);
			if (dpm != null) {
				
				String dpName = dpm.createDocumentProcessor();
				
				//	get annotator
				DocumentProcessor dp = dpm.getDocumentProcessor(dpName);
				if (dp != null) {
					if (hasCycle(this.name, dp)) {
						JOptionPane.showMessageDialog(this, "The selected Pipeline cannot be part of this Pipeline due to a circular reference.", "Cycle Detected", JOptionPane.ERROR_MESSAGE);
					}
					else {
						this.parts.add(dp);
						this.partList.setModel(new PipelinePartListModel(this.parts));
						this.dirty = true;
					}
				}
			}
		}
		
		boolean isDirty() {
			return this.dirty;
		}
		
		void removePart() {
			int index = this.partList.getSelectedIndex();
			if (index != -1) {
				this.parts.remove(index);
				this.partList.setModel(new PipelinePartListModel(this.parts));
				this.dirty = true;
			}
		}
		
		void moveUp() {
			int index = this.partList.getSelectedIndex();
			if (index > 0) {
				this.parts.insertElementAt(this.parts.remove(index - 1), index);
				this.partList.setModel(new PipelinePartListModel(this.parts));
				this.partList.setSelectedIndex(index - 1);
				this.dirty = true;
			}
		}
		
		void editPart() {
			int index = this.partList.getSelectedIndex();
			if (index > -1) {
				DocumentProcessor dp = ((DocumentProcessor) this.parts.get(index));
				DocumentProcessorManager dpm = parent.getDocumentProcessorProvider(dp.getProviderClassName());
				if (dpm != null) dpm.editDocumentProcessor(dp.getName());
			}
		}
		
		void moveDown() {
			int index = this.partList.getSelectedIndex();
			if ((index != -1) && ((index + 1) != this.parts.size())) {
				this.parts.insertElementAt(this.parts.remove(index), (index + 1));
				this.partList.setModel(new PipelinePartListModel(this.parts));
				this.partList.setSelectedIndex(index + 1);
				this.dirty = true;
			}
		}
		
		Settings getSettings() {
			Settings set = new Settings();
			set.setSetting(Pipeline.INTERACTIVITY_LEVEL_ATTRIBUTE, this.interactivityLevel.getSelectedItem().toString());
			for (int p = 0; p < this.parts.size(); p++) {
				Settings partSettings = set.getSubset("PART_" + p);
				DocumentProcessor part = ((DocumentProcessor) this.parts.get(p));
				partSettings.setSetting(Pipeline.PART_NAME_ATTRIBUTE, part.getName());
				partSettings.setSetting(Pipeline.PART_PROVIDER_CLASS_NAME_ATTRIBUTE, part.getProviderClassName());

			}
			return set;
		}
		
		private class PipelinePartListModel implements ListModel {
			
			private Vector data;
			
			PipelinePartListModel(Vector data) {
				this.data = data;
			}
			
			public Object getElementAt(int index) {
				DocumentProcessor dp = ((DocumentProcessor) this.data.get(index));
				return (dp.getTypeLabel() + ": " + dp.getName());
			}
			
			public int getSize() {
				return this.data.size();
			}
			
			public void addListDataListener(ListDataListener l) {}
			
			public void removeListDataListener(ListDataListener l) {}
		}
	}
}
