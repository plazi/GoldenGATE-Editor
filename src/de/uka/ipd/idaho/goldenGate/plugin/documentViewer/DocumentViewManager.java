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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import de.uka.ipd.idaho.easyIO.settings.Settings;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.goldenGate.plugins.AbstractDocumentProcessorManager;
import de.uka.ipd.idaho.goldenGate.plugins.DocumentProcessor;
import de.uka.ipd.idaho.goldenGate.plugins.DocumentViewParameterPanel;
import de.uka.ipd.idaho.goldenGate.plugins.DocumentViewer;
import de.uka.ipd.idaho.goldenGate.util.DataListListener;
import de.uka.ipd.idaho.goldenGate.util.DialogPanel;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * Document processor manager for Document Views. Document views are basically
 * wrappers for DocumentViewers, consisting of the name of the wrapped
 * DocumentViewers and a series of parameters. The rationale is to render
 * DocumentViewers capable of running as DocumentProcessors. All configuration
 * can be done in the 'Edit DocumentViews' dialog in the GoldenGATE Editor.
 * 
 * @author sautter
 */
public class DocumentViewManager extends AbstractDocumentProcessorManager {
	
	private static final String FILE_EXTENSION = ".documentView";
	
	public DocumentViewManager() {}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractDocumentProcessorManager#getDocumentProcessor(java.lang.String)
	 */
	public DocumentProcessor getDocumentProcessor(String name) {
		Settings settings = this.getDocumentView(name);
		return ((settings == null) ? null : this.getDocumentView(name, settings));
	}
	
	/* 
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentProcessorManager#createDocumentProcessor()
	 */
	public String createDocumentProcessor() {
		return this.createDocumentView(new Settings(), null);
	}
	
	/* 
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentProcessorManager#editDocumentProcessor(java.lang.String)
	 */
	public void editDocumentProcessor(String name) {
		Settings set = this.loadSettingsResource(name);
		if ((set == null) || set.isEmpty()) return;
		
		EditDocumentViewDialog ead = new EditDocumentViewDialog(name, set);
		ead.setVisible(true);
		
		if (ead.isCommitted()) try {
			this.storeSettingsResource(name, ead.getDocumentView());
		} catch (IOException ioe) {}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager#getRequiredResourceNames(java.lang.String, boolean)
	 */
	public String[] getRequiredResourceNames(String name, boolean recourse) {
		StringVector nameCollector = new StringVector();
		
		Settings docView = this.getDocumentView(name);
		if (docView == null) return new String[0];
		
		String viewerClassName = docView.getSetting(DocumentViewDocumentProcessor.VIEWER_CLASS_NAME_ATTRIBUTE);
		if (viewerClassName != null)
			nameCollector.addElementIgnoreDuplicates("<DocView>" + "@" + viewerClassName);
		
		return nameCollector.toStringArray();
	}
	
	/* 
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentProcessorManager#editDocumentProcessors()
	 */
	public void editDocumentProcessors() {
		this.editDocumentViews();
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.ResourceManager#getResourceTypeLabel()
	 */
	public String getResourceTypeLabel() {
		return "Document View";
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
				createDocumentView();
			}
		});
		collector.add(mi);
		mi = new JMenuItem("Edit");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				editDocumentViews();
			}
		});
		collector.add(mi);
		return ((JMenuItem[]) collector.toArray(new JMenuItem[collector.size()]));
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractDocumentProcessorManager#getMainMenuTitle()
	 */
	public String getMainMenuTitle() {
		return "Document Views";
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager#getToolsMenuLabel()
	 */
	public String getToolsMenuLabel() {
		return "Show";
	}
	
	/* retrieve a DocumentView by its name
	 * @param	name	the name of the reqired DocumentView
	 * @return the DocumentView with the required name, or null, if there is no such DocumentView
	 */
	private Settings getDocumentView(String name) {
		if (name == null) return null;
		return this.loadSettingsResource(name);
	}
	
	private DocumentViewDocumentProcessor getDocumentView(String name, Settings settings) {
		if (settings == null) return null;
		DocumentViewer dv = this.parent.getDocumentViewer(settings.getSetting(DocumentViewDocumentProcessor.VIEWER_CLASS_NAME_ATTRIBUTE));
		if (dv != null) {
			return new DocumentViewDocumentProcessor(name, dv, settings);
		} else return null;
	}
	
	private class DocumentViewDocumentProcessor implements DocumentProcessor {
		
		static final String VIEWER_CLASS_NAME_ATTRIBUTE = "DOCUMENT_VIEWER_CLASS_NAME";
		
		private String name;
		private DocumentViewer documentViewer;
		private Settings parameters;
		
		DocumentViewDocumentProcessor(String name, DocumentViewer documentViewer, Settings parameters) {
			this.name = name;
			this.documentViewer = documentViewer;
			this.parameters = parameters;
			System.out.println("DocumentViewDocumentProcessor: got settings for " + this.documentViewer.getClass().getName());
			System.out.println(this.parameters.toString());
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
			return "DocumentView";
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentProcessor#getProviderClassName()
		 */
		public String getProviderClassName() {
			return DocumentViewManager.class.getName();
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
			if (!parameters.containsKey(INTERACTIVE_PARAMETER)) return;
			
			parameters = new Properties(parameters);
			String[] pns = this.parameters.getKeys();
			for (int p = 0; p < pns.length; p++)
				parameters.setProperty(pns[p], this.parameters.getSetting(pns[p]));
			this.documentViewer.showDocument(data, parameters);
		}
	}
	
	private boolean createDocumentView() {
		return (this.createDocumentView(new Settings(), "New DocumentView") != null);
	}
	
	private boolean cloneDocumentView() {
		String selectedName = this.resourceNameList.getSelectedName();
		if (selectedName == null)
			return this.createDocumentView();
		else {
			String name = "New " + selectedName;
			return (this.createDocumentView(this.loadSettingsResource(selectedName), name) != null);
		}
	}
	
	private String createDocumentView(Settings set, String name) {
		CreateDocumentViewDialog cfd = new CreateDocumentViewDialog(name, set);
		cfd.setVisible(true);
		if (cfd.isCommitted()) {
			Settings documentView = cfd.getDocumentView();
			String documentViewName = cfd.getDocumentViewName();
			if (!documentViewName.endsWith(FILE_EXTENSION)) documentViewName += FILE_EXTENSION;
			try {
				if (this.storeSettingsResource(documentViewName, documentView)) {
					this.resourceNameList.refresh();
					return documentViewName;
				}
			} catch (IOException e) {}
		}
		return null;
	}
	
	private void editDocumentViews() {
		final DocumentViewEditorPanel[] editor = new DocumentViewEditorPanel[1];
		editor[0] = null;
		
		final DialogPanel editDialog = new DialogPanel("Edit Document Views", true);
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
						storeSettingsResource(editor[0].documentViewName, editor[0].getSettings());
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
				createDocumentView();
			}
		});
		editButtons.add(button);
		button = new JButton("Clone");
		button.setBorder(BorderFactory.createRaisedBevelBorder());
		button.setPreferredSize(new Dimension(100, 21));
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				cloneDocumentView();
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
				editor[0] = new DocumentViewEditorPanel(selectedName, set);
				editorPanel.add(editor[0], BorderLayout.CENTER);
			}
		}
		editDialog.add(editorPanel, BorderLayout.CENTER);
		
		editDialog.add(this.resourceNameList, BorderLayout.EAST);
		DataListListener dll = new DataListListener() {
			public void selected(String dataName) {
				if ((editor[0] != null) && editor[0].isDirty()) {
					try {
						storeSettingsResource(editor[0].documentViewName, editor[0].getSettings());
					}
					catch (IOException ioe) {
						if (JOptionPane.showConfirmDialog(editDialog, (ioe.getClass().getName() + " (" + ioe.getMessage() + ")\nwhile saving file to " + editor[0].documentViewName + "\nProceed?"), "Could Not Save Analyzer", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
							resourceNameList.setSelectedName(editor[0].documentViewName);
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
						editor[0] = new DocumentViewEditorPanel(dataName, set);
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
	
	private class CreateDocumentViewDialog extends DialogPanel {
		
		private JTextField nameField;
		
		private DocumentViewEditorPanel editor;
		private Settings documentView = null;
		private String documentViewName = null;
		
		CreateDocumentViewDialog(String name, Settings documentView) {
			super("Create DocumentView", true);
			
			this.nameField = new JTextField((name == null) ? "New DocumentView" : name);
			this.nameField.setBorder(BorderFactory.createLoweredBevelBorder());
			
			//	initialize main buttons
			JButton commitButton = new JButton("Create");
			commitButton.setBorder(BorderFactory.createRaisedBevelBorder());
			commitButton.setPreferredSize(new Dimension(100, 21));
			commitButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					CreateDocumentViewDialog.this.documentView = editor.getSettings();
					documentViewName = nameField.getText();
					dispose();
				}
			});
			
			JButton abortButton = new JButton("Cancel");
			abortButton.setBorder(BorderFactory.createRaisedBevelBorder());
			abortButton.setPreferredSize(new Dimension(100, 21));
			abortButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					CreateDocumentViewDialog.this.documentView = null;
					documentViewName = null;
					dispose();
				}
			});
			
			JPanel mainButtonPanel = new JPanel();
			mainButtonPanel.setLayout(new FlowLayout());
			mainButtonPanel.add(commitButton);
			mainButtonPanel.add(abortButton);
			
			//	initialize editor
			this.editor = new DocumentViewEditorPanel(name, documentView);
			
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
			return (this.documentView != null);
		}
		
		Settings getDocumentView() {
			return this.documentView;
		}
		
		String getDocumentViewName() {
			return this.documentViewName;
		}
	}

	private class EditDocumentViewDialog extends DialogPanel {
		
		private DocumentViewEditorPanel editor;
		private String documentViewName = null;
		
		EditDocumentViewDialog(String name, Settings documentView) {
			super(("Edit Document View '" + name + "'"), true);
			this.documentViewName = name;
			
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
					documentViewName = null;
					dispose();
				}
			});
			
			JPanel mainButtonPanel = new JPanel();
			mainButtonPanel.setLayout(new FlowLayout());
			mainButtonPanel.add(commitButton);
			mainButtonPanel.add(abortButton);
			
			//	initialize editor
			this.editor = new DocumentViewEditorPanel(name, documentView);
			
			//	put the whole stuff together
			this.setLayout(new BorderLayout());
			this.add(this.editor, BorderLayout.CENTER);
			this.add(mainButtonPanel, BorderLayout.SOUTH);
			
			this.setResizable(true);
			this.setSize(new Dimension(600, 400));
			this.setLocationRelativeTo(this.getOwner());
		}
		
		boolean isCommitted() {
			return (this.documentViewName != null);
		}
		
		Settings getDocumentView() {
			return this.editor.getSettings();
		}
	}

	private class DocumentViewEditorPanel extends JPanel {
		
		private String documentViewName = null;
		private String documentViewerClassName = null;
		private DocumentViewParameterPanel documentViewerParameterPanel;
		
		private JPanel functionPanel = new JPanel(new BorderLayout(), true);
		private JPanel contentPanel = new JPanel(new BorderLayout(), true);
		
		private Settings modelSettings;
		private boolean dirty = false;
		
		DocumentViewEditorPanel(String name, Settings settings) {
			super(new BorderLayout(), true);
			this.documentViewName = name;
			this.modelSettings = settings;
			
			this.add(getExplanationLabel(), BorderLayout.CENTER);
			
			this.documentViewerClassName = settings.getSetting(DocumentViewDocumentProcessor.VIEWER_CLASS_NAME_ATTRIBUTE);
			
			final DocumentViewer[] dvs = parent.getDocumentViewers();
			if (dvs.length == 0)
				this.functionPanel.add(new JLabel("<Sorry, no Document Viewers available>", JLabel.CENTER), BorderLayout.CENTER);
			
			else {
				final String[] dvNames = new String[dvs.length];
				final String[] dvClassNames = new String[dvs.length];
				int selectedDvIndex = 0;
				for (int v = 0; v < dvs.length; v++) {
					dvNames[v] = dvs[v].getPluginName();
					dvClassNames[v] = dvs[v].getClass().getName();
					if (dvClassNames[v].equals(this.documentViewerClassName))
						selectedDvIndex = v;
				}
				
				final JComboBox documentViewerSelector = new JComboBox(dvNames);
				documentViewerSelector.setEditable(false);
				documentViewerSelector.addItemListener(new ItemListener() {
					public void itemStateChanged(ItemEvent ie) {
						int v = documentViewerSelector.getSelectedIndex();
						if (v != -1)
							selectDocumentViewer(dvClassNames[v]);
					}
				});
				
				documentViewerSelector.setSelectedIndex(selectedDvIndex);
				this.selectDocumentViewer(dvClassNames[selectedDvIndex]);
				
				this.functionPanel.add(new JLabel("Document Viewer to use:", JLabel.LEFT), BorderLayout.WEST);
				this.functionPanel.add(documentViewerSelector, BorderLayout.CENTER);
			}
			
			this.layoutPanels();
			this.dirty = false;
		}
		
		void layoutPanels() {
			this.remove(this.contentPanel);
			this.remove(this.functionPanel);
			if (this.documentViewerParameterPanel == null)
				this.add(this.functionPanel, BorderLayout.SOUTH); 
			else {
				this.contentPanel.removeAll();
				this.contentPanel.add(this.documentViewerParameterPanel, BorderLayout.CENTER);
				this.contentPanel.add(this.functionPanel, BorderLayout.SOUTH);
				this.add(this.contentPanel, BorderLayout.SOUTH);
			}
			this.validate();
		}
		
		void selectDocumentViewer(String documentViewerClassName) {
			DocumentViewer dv = parent.getDocumentViewer(documentViewerClassName);
			if (dv != null) {
				this.documentViewerParameterPanel = dv.getDocumentViewParameterPanel(this.modelSettings);
				this.documentViewerClassName = documentViewerClassName;
				this.layoutPanels();
				this.dirty = true;
			}
		}
		
		boolean isDirty() {
			return (this.dirty || ((this.documentViewerParameterPanel != null) && this.documentViewerParameterPanel.isDirty()));
		}
		
		Settings getSettings() {
			if (this.documentViewerClassName == null) return null;
			
			Settings set = new Settings();
			set.setSetting(DocumentViewDocumentProcessor.VIEWER_CLASS_NAME_ATTRIBUTE, this.documentViewerClassName);
			if (this.documentViewerParameterPanel != null)
				set.setSettings(this.documentViewerParameterPanel.getSettings());
			return set;
		}
	}
}
