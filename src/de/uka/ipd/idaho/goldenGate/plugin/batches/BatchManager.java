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
package de.uka.ipd.idaho.goldenGate.plugin.batches;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Properties;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import de.uka.ipd.idaho.easyIO.settings.Settings;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.goldenGate.DocumentEditorDialog;
import de.uka.ipd.idaho.goldenGate.GoldenGATE;
import de.uka.ipd.idaho.goldenGate.GoldenGateConstants;
import de.uka.ipd.idaho.goldenGate.configuration.ConfigurationUtils;
import de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager;
import de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat;
import de.uka.ipd.idaho.goldenGate.plugins.DocumentProcessor;
import de.uka.ipd.idaho.goldenGate.plugins.DocumentProcessorManager;
import de.uka.ipd.idaho.goldenGate.plugins.Resource;
import de.uka.ipd.idaho.goldenGate.util.DataListListener;
import de.uka.ipd.idaho.goldenGate.util.DialogPanel;
import de.uka.ipd.idaho.goldenGate.util.ResourceDialog;
import de.uka.ipd.idaho.stringUtils.StringVector;


/**
 * Resource manager for Batch jobs. A Batch is basically a (set of) document(s)
 * and a DocumentProcessor. All the documents will be run through the
 * DocumentProcessor one after the other (optionally without any user
 * interaction, e.g. for some overnight job), and then be saved to a different
 * location. The latter is in order to avoid modification of the input data,
 * thus preventing data loss in any case. All configuration can be done in the
 * 'Edit Batches' dialog in the GoldenGATE Editor.
 * 
 * @author sautter
 */
public class BatchManager extends AbstractResourceManager {
	
	private static final String FILE_EXTENSION = ".batch";
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.ResourceManager#getResourceTypeLabel()
	 */
	public String getResourceTypeLabel() {
		return "Batch";
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
		ArrayList collector = new ArrayList();
		JMenuItem mi;
		
		mi = new JMenuItem("Create");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				if (createBatch())
					resourceNameList.refresh();
			}
		});
		collector.add(mi);
		mi = new JMenuItem("Edit");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				editBatches();
			}
		});
		collector.add(mi);
		collector.add(GoldenGateConstants.MENU_SEPARATOR_ITEM);
		
		mi = new JMenuItem("Ad-Hoc");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				runAdHocBatch();
			}
		});
		collector.add(mi);
		
		mi = new JMenuItem("Run");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				runBatch(null);
			}
		});
		collector.add(mi);
		
		return ((JMenuItem[]) collector.toArray(new JMenuItem[collector.size()]));
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#getToolsMenuFunctionItems(de.uka.ipd.idaho.goldenGate.GoldenGateConstants.InvokationTargetProvider)
	 */
	public JMenuItem[] getToolsMenuFunctionItems(InvokationTargetProvider targetProvider) {
		ArrayList collector = new ArrayList();
		JMenuItem mi;
		
		mi = new JMenuItem("Ad-Hoc Batch");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				runAdHocBatch();
			}
		});
		collector.add(mi);
		
		mi = new JMenuItem("Run Batch");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				runBatch(null);
			}
		});
		collector.add(mi);
		
		return ((JMenuItem[]) collector.toArray(new JMenuItem[collector.size()]));
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractDocumentProcessorManager#getMainMenuTitle()
	 */
	public String getMainMenuTitle() {
		return "Batches";
	}
	
	private boolean createBatch() {
		return this.createBatch(new Settings(), null);
	}
	
	private boolean cloneBatch() {
		String selectedName = this.resourceNameList.getSelectedName();
		if (selectedName == null)
			return this.createBatch();
		else {
			String name = "New " + selectedName;
			return this.createBatch(this.loadSettingsResource(selectedName), name);
		}
	}
	
	private boolean createBatch(Settings modelBatch, String name) {
		CreateBatchDialog cbd = new CreateBatchDialog(modelBatch, name);
		cbd.setVisible(true);
		
		if (cbd.isCommitted()) {
			Settings batch = cbd.getBatch();
			String batchName = cbd.getBatchName();
			if (!batchName.endsWith(FILE_EXTENSION)) batchName += FILE_EXTENSION;
			try {
				return this.storeSettingsResource(batchName, batch);
			} catch (IOException e) {}
		}
		return false;
	}
	
	private void editBatches() {
		final BatchEditorPanel[] editor = new BatchEditorPanel[1];
		editor[0] = null;
		
		final DialogPanel editDialog = new DialogPanel("Edit Batchs", true);
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
						storeSettingsResource(editor[0].batchName, editor[0].getSettings());
					} catch (IOException ioe) {}
				}
				if (editDialog.isVisible()) {
					editDialog.setVisible(false);
					editDialog.dispose();
				}
			}
		});
		
		JPanel editButtons = new JPanel(new FlowLayout(FlowLayout.CENTER));
		JButton button;
		button = new JButton("Create");
		button.setBorder(BorderFactory.createRaisedBevelBorder());
		button.setPreferredSize(new Dimension(100, 21));
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (createBatch())
					resourceNameList.refresh();
			}
		});
		editButtons.add(button);
		button = new JButton("Clone");
		button.setBorder(BorderFactory.createRaisedBevelBorder());
		button.setPreferredSize(new Dimension(100, 21));
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (cloneBatch())
					resourceNameList.refresh();
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
		button = new JButton("Run");
		button.setBorder(BorderFactory.createRaisedBevelBorder());
		button.setPreferredSize(new Dimension(100, 21));
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				runBatch();
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
				editor[0] = new BatchEditorPanel(selectedName, set);
				editorPanel.add(editor[0], BorderLayout.CENTER);
			}
		}
		editDialog.add(editorPanel, BorderLayout.CENTER);
		
		editDialog.add(this.resourceNameList, BorderLayout.EAST);
		DataListListener dll = new DataListListener() {
			public void selected(String dataName) {
				if ((editor[0] != null) && editor[0].isDirty()) {
					try {
						storeSettingsResource(editor[0].batchName, editor[0].getSettings());
					}
					catch (IOException ioe) {
						if (JOptionPane.showConfirmDialog(editDialog, (ioe.getClass().getName() + " (" + ioe.getMessage() + ")\nwhile saving file to " + editor[0].batchName + "\nProceed?"), "Could Not Save Analyzer", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
							resourceNameList.setSelectedName(editor[0].batchName);
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
						editor[0] = new BatchEditorPanel(dataName, set);
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
	
	private void runBatch() {
		this.runBatch(this.resourceNameList.getSelectedName());
	}
	
	private void runBatch(String batchName) {
		String name = batchName;
		if (name == null) {
			ResourceDialog rbd = ResourceDialog.getResourceDialog(this, "Select Batch to Execute", "Run");
			rbd.setLocationRelativeTo(DialogPanel.getTopWindow());
			rbd.setVisible(true);
			name = rbd.getSelectedResourceName();
		}
		if (name != null) {
			Settings batch = this.loadSettingsResource(name);
			if (batch != null)
				this.runBatch(("Run Batch '" + name + "'"), name, batch);
		}
	}
	
	private void runAdHocBatch() {
		this.runBatch("Run Ad-Hoc Batch", "AdHocBatch", new Settings());
	}
	
	private void runBatch(String title, String name, Settings batch) {
		BatchRunDialog batchDialog = new BatchRunDialog(title, name, batch);
		batchDialog.setSize(new Dimension(600, 400));
		batchDialog.setLocationRelativeTo(batchDialog.getOwner());
		batchDialog.setVisible(true);
	}
	
	private class BatchRunDialog extends DialogPanel {
		
		private BatchDataPanel dataPanel;
		
		private JLabel statusLabel = new JLabel("", JLabel.CENTER) {
			public void setText(String text) {
				super.setText("<HTML><B>" + text + "</B></HTML>");
			}
		};
		
		private JCheckBox interactive = new JCheckBox("Interactive", true);
		private JCheckBox inspectDocument = new JCheckBox("Inspect Document", false);
		
		private JButton startButton = new JButton("Start");
		private JButton pauseButton = new JButton("Pause");
		private JButton stopButton = new JButton("Stop");
		private JButton resetButton = new JButton("Reset");
		private JButton closeButton = new JButton("Close");
		
		JProgressBar progressBar = new JProgressBar(0, 100);
		private StringVector logLines = new StringVector();
		private JTextArea logDisplay = new JTextArea();
		
		private String batchName;
		private Batch batch = null;
		
		BatchRunDialog(String title, String batchName, Settings batch) {
			super(title, true);
			this.batchName = batchName;
			this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
			
			this.dataPanel = new BatchDataPanel(batch);
			this.dataPanel.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent ce) {
					updateBatchData();
				}
			});
			
			
			this.interactive.setBorder(BorderFactory.createEtchedBorder());
			this.inspectDocument.setBorder(BorderFactory.createEtchedBorder());
			
			this.statusLabel.setBorder(BorderFactory.createLineBorder(Color.RED));
			
			this.startButton.setBorder(BorderFactory.createRaisedBevelBorder());
			this.startButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					start();
				}
			});
			
			this.pauseButton.setBorder(BorderFactory.createRaisedBevelBorder());
			this.pauseButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					pause();
				}
			});
			
			this.stopButton.setBorder(BorderFactory.createRaisedBevelBorder());
			this.stopButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					stop();
				}
			});
			
			this.resetButton.setBorder(BorderFactory.createRaisedBevelBorder());
			this.resetButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					reset();
				}
			});
			
			this.closeButton.setBorder(BorderFactory.createRaisedBevelBorder());
			this.closeButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					dispose();
				}
			});
			
			JPanel buttonPanel = new JPanel(new GridLayout(2, 4, 3, 3), true);
			buttonPanel.add(this.interactive);
			buttonPanel.add(this.inspectDocument);
			buttonPanel.add(this.statusLabel);
			buttonPanel.add(this.closeButton);
			buttonPanel.add(this.startButton);
			buttonPanel.add(this.pauseButton);
			buttonPanel.add(this.stopButton);
			buttonPanel.add(this.resetButton);
			
			
			this.progressBar.setStringPainted(true);
			
			JPanel progressPanel = new JPanel(new BorderLayout(), true);
			progressPanel.add(new JScrollPane(this.logDisplay), BorderLayout.CENTER);
			progressPanel.add(this.progressBar, BorderLayout.SOUTH);
			
			
			this.add(this.dataPanel, BorderLayout.NORTH);
			this.add(progressPanel, BorderLayout.CENTER);
			this.add(buttonPanel, BorderLayout.SOUTH);
			
			
			this.updateBatchData();
			this.updateStatus();
		}
		
		private DocumentProcessor processor;
		
		private File inputFolder;
		private DocumentFormat inputFormat = null;
		
		private File outputFolder;
		private DocumentFormat outputFormat = null;
		
		private TreeSet allFileNames = new TreeSet();
		private TreeSet todoFileNames = new TreeSet();
		private TreeSet doneFileNames = new TreeSet();
		
		private String doneLogFileName;
		
		private void updateBatchData() {
			
			//	got all parameters, load data objects
			if (this.dataPanel.isComplete()) {
				this.inputFolder = new File(this.dataPanel.inputSelector.getFolder());
				this.inputFormat = parent.getDocumentFormatForName(this.dataPanel.inputSelector.getFormat());
				this.outputFolder = new File(this.dataPanel.outputSelector.getFolder());
				this.outputFormat = parent.getDocumentFormatForName(this.dataPanel.outputSelector.getFormat());
				this.processor = parent.getDocumentProcessorForName(this.dataPanel.processorSelector.getProcessorName());
			}
			
			//	check if all objects valid
			if (this.isBatchRunnable()) {
				
				//	list files to process
				File[] docFiles = this.inputFolder.listFiles(new java.io.FileFilter() {
					public boolean accept(File name) {
						return (inputFormat.accept(name));
					}
				});
				for (int f = 0; f < docFiles.length; f++) {
					if (docFiles[f].isFile())
						this.allFileNames.add(docFiles[f].getName());
				}
				
				//	list files already processed
				this.doneLogFileName = ((this.dataPanel.inputSelector.getFormat() + "-" + this.processor.getName() + "-" + this.dataPanel.outputSelector.getFormat()).replaceAll("[^a-zA-Z0-9\\-\\_\\.]", "_") + ".done.log");
				this.doneFileNames.clear();
				File doneLogFile = new File(this.outputFolder, this.doneLogFileName);
				if (doneLogFile.exists()) try {
					BufferedReader br = new BufferedReader(new FileReader(doneLogFile));
					String doneFileName;
					while ((doneFileName = br.readLine()) != null) {
						if (this.allFileNames.contains(doneFileName))
							this.doneFileNames.add(doneFileName);
					}
					br.close();
				}
				catch (IOException ioe) {}
				
				//	diff file lists
				this.todoFileNames.clear();
				for (Iterator fit = this.allFileNames.iterator(); fit.hasNext();) {
					String fileName = ((String) fit.next());
					if (!this.doneFileNames.contains(fileName))
						this.todoFileNames.add(fileName);
				}
				
				//	show progress
				this.progressBar.setMaximum(this.allFileNames.size());
				this.updateProgress();
			}
			
			//	something's missing, clean up
			else {
				this.inputFolder = null;
				this.inputFormat = null;
				this.outputFolder = null;
				this.outputFormat = null;
				this.processor = null;
				this.allFileNames.clear();
				this.todoFileNames.clear();
				this.doneFileNames.clear();
				this.doneLogFileName = null;
				
				this.progressBar.setMaximum(100);
				this.progressBar.setValue(0);
				this.progressBar.setString("Progress will be indicated when all data is present");
			}
			
			//	reflect state in buttons
			this.updateStatus();
		}
		
		private boolean isBatchRunnable() {
			return (true
					&& (this.inputFolder != null)
					&& this.inputFolder.exists() 
					&& this.inputFolder.isDirectory()
					&& (this.inputFormat != null)
					
					&& (this.outputFolder != null)
					&& this.outputFolder.exists()
					&& this.outputFolder.isDirectory()
					&& (this.outputFormat != null)
					
					&& (this.processor != null)
					);
		}
		
		private StringVector getErrorReport() {
			StringVector errorMessages = new StringVector();
			
			if (this.inputFolder == null)
				errorMessages.addElement("No input folder selected.");
			else if (!this.inputFolder.exists() || !this.inputFolder.isDirectory())
				errorMessages.addElement("The selected input folder does not exist or is not a folder.");
			if (this.inputFormat == null)
				errorMessages.addElement("The selected input format does not exist or could not be loaded.");
			
			if (this.outputFolder == null)
				errorMessages.addElement("No output folder selected.");
			else if (!this.outputFolder.exists() || !this.outputFolder.isDirectory())
				errorMessages.addElement("The selected output folder does not exist or is not a folder.");
			if (this.outputFormat == null)
				errorMessages.addElement("The selected output format does not exist or could not be loaded.");
			
			if (this.processor == null)
				errorMessages.addElement("The selected document processor does not exist or could not be loaded.");
			
			return errorMessages;
		}
		
		void fileFinished(String fileName) {
			this.todoFileNames.remove(fileName);
			this.doneFileNames.add(fileName);
			this.updateProgress();
		}
		
		private void updateProgress() {
			this.progressBar.setValue(this.doneFileNames.size());
			this.progressBar.setString(this.doneFileNames.size() + " of " + this.allFileNames.size() + " files processed");
		}
		
		String getNextFileName() {
			return (this.todoFileNames.isEmpty() ? null : ((String) this.todoFileNames.first()));
		}
		
		private void start() {
			
			//	start after shutdown
			if (this.batch == null) {
				
				//	check parameters
				if (!this.dataPanel.isComplete()) {
					JOptionPane.showMessageDialog(this, "The batch cannot be executed in its current state.\nMake sure you specified all the parameters required:\n - An input folder and input format\n - An output folder and output format\n - The document processor to run on the documents", "Cannot Execute Batch", JOptionPane.ERROR_MESSAGE);
					return;
				}
				
				//	gather data and check if complete
				if (this.isBatchRunnable()) {
					
					//	initialize batch
					this.batch = new Batch(parent, this, this.batchName);
					
					//	start batch and wait until it's running
					synchronized (this.batch) {
						this.batch.start();
						try {
							this.batch.wait();
						} catch (InterruptedException ie) {}
					}
				}
				
				//	something's missing
				else {
					StringVector errorReport = this.getErrorReport();
					JOptionPane.showMessageDialog(this, ("The batch cannot be executed in its current state:\n - " + errorReport.concatStrings("\n - ")), "Cannot Execute Batch", JOptionPane.ERROR_MESSAGE);
				}
			}
			
			//	start after pause
			else if (this.batch.paused)
				synchronized (this.batch.pause) {
					this.batch.paused = false;
					this.batch.pause.notify();
				}
			
			//	make sure buttons reflect current state
			this.updateStatus();
		}
		
		private void pause() {
			
			//	check state
			if ((this.batch == null) || !this.batch.isActive())
				return;
			
			//	use new thread to pause batch in order to keep EDT available for processor dialogs
			Thread pauser = new Thread() {
				private Batch pBatch = batch;
				public void run() {
					synchronized (this.pBatch.pause) {
						this.pBatch.paused = true;
						try {
							this.pBatch.pause.wait();
						} catch (InterruptedException ie) {}
					}
					
					//	make sure buttons reflect current state
					updateStatus();
				}
			};
			
			//	make sure buttons reflect current state
			this.statusLabel.setText("Pausing After Document");
			this.interactive.setEnabled(false);
			this.inspectDocument.setEnabled(false);
			this.startButton.setEnabled(false);
			this.pauseButton.setEnabled(false);
			this.stopButton.setEnabled(false);
			this.resetButton.setEnabled(false);
			this.closeButton.setEnabled(false);
			this.dataPanel.setEnabled(false);
			
			//	trigger pausing
			pauser.start();
		}
		
		private void stop() {
			
			//	check state
			if (this.batch == null)
				return;
			
			//	use new thread in order to keep EDT available for processor dialogs
			Thread stopper = new Thread() {
				private Batch sBatch = batch;
				public void run() {
					this.sBatch.stop = true;
					if (this.sBatch.paused)
						synchronized (this.sBatch.pause) {
							this.sBatch.paused = false;
							this.sBatch.pause.notify();
						}
					try {
						this.sBatch.join();
					} catch (InterruptedException ie) {}
					
					//	make sure buttons reflect current state
					updateStatus();
					
					//	reset progress bar
					updateProgress();
				}
			};
			
			//	make sure buttons reflect current state
			this.statusLabel.setText("Stopping After Document");
			this.interactive.setEnabled(false);
			this.inspectDocument.setEnabled(false);
			this.startButton.setEnabled(false);
			this.pauseButton.setEnabled(false);
			this.stopButton.setEnabled(false);
			this.resetButton.setEnabled(false);
			this.closeButton.setEnabled(false);
			this.dataPanel.setEnabled(false);
			
			//	trigger stopping
			stopper.start();
		}
		
		private void reset() {
			
			//	check state
			if (this.batch != null)
				return;
			
			//	delete log file
			File doneLogFile = new File(this.outputFolder, this.doneLogFileName);
			if (doneLogFile.exists())
				doneLogFile.delete();
			
			//	clear local logs
			this.doneFileNames.clear();
			this.todoFileNames.addAll(this.allFileNames);
			
			//	show new status
			this.updateProgress();
		}
		
		private void updateStatus() {
			boolean runnable = this.isBatchRunnable();
			this.statusLabel.setText(runnable ? ((this.batch == null) ? "Stopped" : (this.batch.paused ? "Paused" : "Running")) : "Not Runnable");
			this.interactive.setEnabled((this.batch == null) || this.batch.paused);
			this.inspectDocument.setEnabled((this.batch == null) || this.batch.paused);
			this.startButton.setEnabled(runnable && ((this.batch == null) || this.batch.paused));
			this.pauseButton.setEnabled((this.batch != null) && !this.batch.paused);
			this.stopButton.setEnabled(this.batch != null);
			this.resetButton.setEnabled(runnable && (this.batch == null));
			this.closeButton.setEnabled(this.batch == null);
			this.dataPanel.setEnabled(this.batch == null);
		}
		
		void batchRunTerminated() {
			this.batch = null;
			try {
				File doneLogFile = new File(this.outputFolder, this.doneLogFileName);
				if (doneLogFile.exists()) {
					doneLogFile.delete();
					doneLogFile = new File(this.outputFolder, this.doneLogFileName);
				}
				doneLogFile.createNewFile();
				
				BufferedWriter bw = new BufferedWriter(new FileWriter(doneLogFile));
				for (Iterator dfit = this.doneFileNames.iterator(); dfit.hasNext();) {
					String doneFileName = ((String) dfit.next());
					bw.write(doneFileName);
					bw.newLine();
				}
				bw.flush();
				bw.close();
			}
			catch (IOException ioe) {}
			this.updateStatus();
		}
		
		void log(String entry) {
			System.out.println(entry);
			this.logLines.addElement(entry);
			while (this.logLines.size() > 100)
				this.logLines.remove(0);
			this.logDisplay.setText(this.logLines.concatStrings("\n"));
			this.logDisplay.validate();
		}
	}
	
	private static class Batch extends Thread {
		
		static final String PROCESSOR_NAME_ATTRIBUTE = "PROCESSOR_NAME";
		
		static final String INPUT_DIRECTORY_ATTRIBUTE = "INPUT_DIRECTORY";
		static final String INPUT_FORMAT_ATTRIBUTE = "INPUT_FORMAT";
		
		static final String STORAGE_DIRECTORY_ATTRIBUTE = "STORAGE_DIRECTORY";
		static final String STORAGE_FORMAT_ATTRIBUTE = "STORAGE_FORMAT";
		
		private GoldenGATE host;
		private BatchRunDialog parent;
		
		private static final SimpleDateFormat timeFormat = new SimpleDateFormat("yyyyMMdd-HHmmss");
		private BufferedWriter logWriter;
		
		boolean stop = false;
		boolean paused = false;
		Object pause = new Object();
		
		private Batch(GoldenGATE host, BatchRunDialog parent, String batchName) {
			this.host = host;
			this.parent = parent;
			
			File logFile = new File(this.parent.outputFolder, (batchName + "." + timeFormat.format(new Date()) + ".log"));
			try {
				logFile.createNewFile();
				this.logWriter = new BufferedWriter(new FileWriter(logFile));
			} catch (IOException ioe) {}
		}
		
		public void run() {
			
			//	notify any objects that might be waiting for the start to complete
			synchronized (this) {
				this.notify();
			}
			
			//	run until told to stop
			String docFileName;
			while (!this.stop && ((docFileName = this.parent.getNextFileName()) != null)) {
				
				//	process next file in line if there is one
				try {
					this.log("Processing " + docFileName);
					
					//	load document
					File docFile = new File(this.parent.inputFolder, docFileName);
					FileInputStream fis = new FileInputStream(docFile);
					MutableAnnotation doc = this.parent.inputFormat.loadDocument(fis);
					fis.close();
					this.log(" - " + docFileName + " opened, size is " + doc.size());
					
					//	run document throught processor
					Properties parameters = new Properties();
					if (this.parent.interactive.isSelected())
						parameters.setProperty(Resource.INTERACTIVE_PARAMETER, Resource.INTERACTIVE_PARAMETER);
					this.parent.processor.process(doc, parameters);
					this.log(" - " + docFileName + " processed, size is " + doc.size());
					
					//	inspect document if option selected
					boolean save = true;
					if (this.parent.inspectDocument.isSelected()) {
						DocumentEditDialog ded = new DocumentEditDialog(this.host, docFileName, doc);
						ded.setLocationRelativeTo(this.parent);
						ded.setVisible(true);
						if (!ded.save())
							save = false;
						if (ded.pause.isSelected())
							this.parent.pause();
						else if (ded.stop.isSelected())
							this.parent.stop();
					}
					
					//	save document
					if (save) {
						
						//	prepare file name
						String saveFileExtension = this.parent.outputFormat.getDefaultSaveFileExtension();
						if ((saveFileExtension.length() != 0) && !saveFileExtension.startsWith("."))
							saveFileExtension = ("." + saveFileExtension);
						String saveFileName = (docFileName + (docFileName.endsWith(saveFileExtension) ? "" : saveFileExtension));
						
						//	create target file (make way if necessary)
						File target = new File(this.parent.outputFolder, saveFileName);
						if (target.exists()) {
							target.renameTo(new File(this.parent.outputFolder, (saveFileName + "." + System.currentTimeMillis() + ".old")));
							target = new File(this.parent.outputFolder, saveFileName);
						}
						
						//	write document to file
						OutputStream fos = new FilterOutputStream(new FileOutputStream(target)) {
							private int written = 0;
							public void close() throws IOException {
								super.close();
								log(" - " + this.written + " bytes written");
							}
							public void write(int b) throws IOException {
								super.write(b);
								this.written++;
							}
						};
						this.parent.outputFormat.saveDocument(doc, fos);
						fos.flush();
						fos.close();
						
						this.log(" - " + docFileName + " stored to " + target.getAbsolutePath() + ", size is " + doc.size());
					}
					
					//	give the others a little time ...
					Thread.yield();
				}
				catch (Exception e) {
					this.log(e, docFileName);
				}
				
				//	make progress visible
				this.parent.fileFinished(docFileName);
				
				//	pause if sheduled to pause
				if (this.paused)
					synchronized (this.pause) {
						this.pause.notify();
						try {
							this.pause.wait();
						} catch (InterruptedException ie) {}
					}
			}
			
			//	close log writer
			if (this.logWriter != null) try {
				this.logWriter.flush();
				this.logWriter.close();
			} catch (IOException ioe) {}
			
			//	notify parent
			this.parent.batchRunTerminated();
		}
		
		boolean isActive() {
			return (!this.stop && !this.paused && this.isAlive());
		}
		
		private void log(String entry) {
			this.parent.log(entry);
			if (this.logWriter != null) try {
				this.logWriter.write(entry);
				this.logWriter.newLine();
			} catch (IOException ioe) {}
		}
		
		private void log(Exception e, String docFileName) {
			this.log(e.getClass().getName() + " (" + e.getMessage() + ") while processing " + docFileName);
			if (this.logWriter != null) try {
				Throwable tr = e;
				do {
					this.logWriter.write(((tr == e) ? "" : "Caused by: ") + tr.toString());
					this.logWriter.newLine();
		            StackTraceElement[] trace = tr.getStackTrace();
					for (int t = 0; t < trace.length; t++)
						this.log("\tat " + trace[t]);
				}
				while ((tr = tr.getCause()) != null);
			} catch (IOException ioe) {}
		}
		
		private class DocumentEditDialog extends DocumentEditorDialog {
			
			boolean save = true;
			
			JCheckBox pause = new JCheckBox("Pause After Document", false);
			JCheckBox stop = new JCheckBox("Stop After Document", false);
			
			DocumentEditDialog(GoldenGATE host, String title, MutableAnnotation doc) {
				super(host, null, title, doc);
				
				//	initialize main buttons
				JButton commitButton = new JButton("Store Document");
				commitButton.setBorder(BorderFactory.createRaisedBevelBorder());
				commitButton.setPreferredSize(new Dimension(150, 21));
				commitButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						documentEditor.writeChanges();
						dispose();
					}
				});
				JButton abortButton = new JButton("Discart Document");
				abortButton.setBorder(BorderFactory.createRaisedBevelBorder());
				abortButton.setPreferredSize(new Dimension(150, 21));
				abortButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						if (JOptionPane.showConfirmDialog(DocumentEditDialog.this, "Really discart Document?", "Confirm Discarting Document", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
							save = false;
							dispose();
						}
					}
				});
				
				this.mainButtonPanel.add(abortButton);
				this.mainButtonPanel.add(commitButton);
				this.mainButtonPanel.add(this.pause);
				this.mainButtonPanel.add(this.stop);
				
				this.setResizable(true);
				this.setSize(new Dimension(800, 600));
				this.setLocationRelativeTo(DialogPanel.getTopWindow());
			}
			
			boolean save() {
				return this.save;
			}
		}
	}
	
	private class CreateBatchDialog extends DialogPanel {
		
		private JTextField nameField;
		
		private BatchEditorPanel editor;
		private String batchName = null;
		
		CreateBatchDialog(Settings set, String name) {
			super("Create Batch", true);
			
			this.nameField = new JTextField((name == null) ? "New Batch" : name);
			this.nameField.setBorder(BorderFactory.createLoweredBevelBorder());
			
			//	initialize main buttons
			JButton commitButton = new JButton("Create");
			commitButton.setBorder(BorderFactory.createRaisedBevelBorder());
			commitButton.setPreferredSize(new Dimension(100, 21));
			commitButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					batchName = nameField.getText();
					dispose();
				}
			});
			
			JButton abortButton = new JButton("Cancel");
			abortButton.setBorder(BorderFactory.createRaisedBevelBorder());
			abortButton.setPreferredSize(new Dimension(100, 21));
			abortButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					batchName = null;
					dispose();
				}
			});
			
			JPanel mainButtonPanel = new JPanel();
			mainButtonPanel.setLayout(new FlowLayout());
			mainButtonPanel.add(commitButton);
			mainButtonPanel.add(abortButton);
			
			//	initialize editor
			this.editor = new BatchEditorPanel(name, set);
			
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
			return (this.batchName != null);
		}
		
		Settings getBatch() {
			return this.editor.getSettings();
		}
		
		String getBatchName() {
			return this.batchName;
		}
	}
	
	private class BatchEditorPanel extends JPanel {
		
		private String batchName;
		private BatchDataPanel dataPanel;
		
		BatchEditorPanel(String name, Settings set) {
			super(new BorderLayout(), true);
			this.batchName = name;
			this.dataPanel = new BatchDataPanel(set);
			
			this.add(getExplanationLabel(), BorderLayout.CENTER);
			this.add(this.dataPanel, BorderLayout.SOUTH);
		}
		
		boolean isDirty() {
			return this.dataPanel.isDirty();
		}
		
		Settings getSettings() {
			Settings set = new Settings();
			
			String processorName = this.dataPanel.processorSelector.getProcessorName();
			if (processorName != null)
				set.setSetting(Batch.PROCESSOR_NAME_ATTRIBUTE, processorName);
			
			String inputFolder = this.dataPanel.inputSelector.getFolder();
			if (inputFolder != null)
				set.setSetting(Batch.INPUT_DIRECTORY_ATTRIBUTE, inputFolder);
			String inputFormat = this.dataPanel.inputSelector.getFormat();
			if (inputFormat != null)
				set.setSetting(Batch.INPUT_FORMAT_ATTRIBUTE, inputFormat);
			
			String outputFolder = this.dataPanel.outputSelector.getFolder();
			if (outputFolder != null)
				set.setSetting(Batch.STORAGE_DIRECTORY_ATTRIBUTE, outputFolder);
			String outputFormat = this.dataPanel.outputSelector.getFormat();
			if (outputFormat != null)
				set.setSetting(Batch.STORAGE_FORMAT_ATTRIBUTE, outputFormat);
			
			return set;
		}
	}
	
	private class BatchDataPanel extends JPanel {
		FolderSelectorPanel inputSelector;
		FolderSelectorPanel outputSelector;
		ProcessorSelectorPanel processorSelector;
		
		BatchDataPanel(Settings set) {
			super(new GridLayout(3, 1, 0, 3), true);
			
			this.inputSelector = new FolderSelectorPanel(set.getSetting(Batch.INPUT_DIRECTORY_ATTRIBUTE), set.getSetting(Batch.INPUT_FORMAT_ATTRIBUTE), true);
			this.outputSelector = new FolderSelectorPanel(set.getSetting(Batch.STORAGE_DIRECTORY_ATTRIBUTE), set.getSetting(Batch.STORAGE_FORMAT_ATTRIBUTE), false);
			this.processorSelector = new ProcessorSelectorPanel(set.getSetting(Batch.PROCESSOR_NAME_ATTRIBUTE));
			
			this.add(this.inputSelector);
			this.add(this.outputSelector);
			this.add(this.processorSelector);
		}
		
		public void setEnabled(boolean enabled) {
			super.setEnabled(enabled);
			this.inputSelector.setEnabled(enabled);
			this.outputSelector.setEnabled(enabled);
			this.processorSelector.setEnabled(enabled);
		}
		
		boolean isDirty() {
			return (this.inputSelector.isDirty() || this.outputSelector.isDirty() || this.processorSelector.isDirty());
		}
		
		boolean isComplete() {
			return (this.inputSelector.isComplete() && this.outputSelector.isComplete() && this.processorSelector.isComplete());
		}
		
		private ArrayList cls = null;
		void addChangeListener(ChangeListener cl) {
			if (cl == null)
				return;
			if (this.cls == null) {
				this.cls = new ArrayList();
				this.inputSelector.addChangeListener(new ChangeListener() {
					public void stateChanged(ChangeEvent ce) {
						BatchDataPanel.this.stateChanged(ce);
					}
				});
				this.outputSelector.addChangeListener(new ChangeListener() {
					public void stateChanged(ChangeEvent ce) {
						BatchDataPanel.this.stateChanged(ce);
					}
				});
				this.processorSelector.addChangeListener(new ChangeListener() {
					public void stateChanged(ChangeEvent ce) {
						BatchDataPanel.this.stateChanged(ce);
					}
				});
			}
			this.cls.add(cl);
		}
		private void stateChanged(ChangeEvent ce) {
			if (this.cls == null)
				return;
			ChangeEvent lce = new ChangeEvent(this);
			for (Iterator lit = this.cls.iterator(); lit.hasNext();)
				((ChangeListener) lit.next()).stateChanged(lce);
		}
	}
	
	private class FolderSelectorPanel extends JPanel {
		private boolean input;
		private String label;
		
		private JButton folderButton;
		private JFileChooser folderSelector = new JFileChooser();
		private String folderName = null;
		private JButton formatButton;
		private String formatName = null;
		
		private boolean dirty = false;
		
		FolderSelectorPanel(String folderName, String formatName, boolean input) {
			super(new GridLayout(2, 1, 0, 3), true);
			this.input = input;
			this.label = (this.input ? "Input" : "Output");
			
			this.folderSelector.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			this.folderName = folderName;
			String folderLabel;
			if (this.folderName == null)
				folderLabel = (this.label + " Folder: " + "<No " + this.label + " Folder Selected>");
			else {
				File folder = new File(this.folderName);
				if (input) {
					if (folder.isDirectory()) {
						folderLabel = (this.label + " Folder: " + this.folderName);
						this.folderSelector.setSelectedFile(new File(this.folderName));
					}
					else {
						this.folderName = null;
						folderLabel = (this.label + " Folder: " + "<No " + this.label + " Folder Selected>");
					}
				}
				else {
					if (folder.exists() && !folder.isDirectory()) {
						this.folderName = null;
						folderLabel = (this.label + " Folder: " + "<No " + this.label + " Folder Selected>");
					}
					else {
						folderLabel = (this.label + " Folder: " + this.folderName);
						this.folderSelector.setSelectedFile(new File(this.folderName));
					}
				}
			}
			this.folderButton = new JButton(folderLabel);
			this.folderButton.setBorder(BorderFactory.createRaisedBevelBorder());
			this.folderButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					setFolder();
				}
			});
			
			this.formatName = formatName;
			this.formatButton = new JButton((this.formatName == null) ? "<All Files>" : this.formatName);
			this.formatButton.setBorder(BorderFactory.createRaisedBevelBorder());
			this.formatButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					selectFormat();
				}
			});
			
			this.add(this.folderButton);
			this.add(this.formatButton);
			this.setBorder(BorderFactory.createEtchedBorder());
		}
		
		void setFolder() {
			if (this.folderSelector.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
				File file = this.folderSelector.getSelectedFile();
				if (file == null) {
					this.folderName = null;
					this.folderButton.setText(this.label + " Folder: " + "<No " + this.label + " Folder Selected>");
				}
				else {
					this.folderName = ConfigurationUtils.normalizePath(file.getAbsolutePath());
					this.folderButton.setText(this.label + " Folder: " + this.folderName);
				}
				this.stateChanged();
			}
		}
		
		void selectFormat() {
			String newFormatName = (this.input ? parent.selectLoadFormat() : parent.selectSaveFormat());
			if (newFormatName != null) {
				this.formatName = newFormatName;
				this.formatButton.setText(this.label + " Format: " + this.formatName);
				this.stateChanged();
			}
		}
		
		public void setEnabled(boolean enabled) {
			super.setEnabled(enabled);
			this.folderButton.setEnabled(enabled);
			this.formatButton.setEnabled(enabled);
		}
		
		boolean isDirty() {
			return this.dirty;
		}
		
		boolean isComplete() {
			return ((this.folderName != null) && (this.formatName != null));
		}
		
		String getFolder() {
			return this.folderName;
		}
		
		String getFormat() {
			return this.formatName;
		}
		
		private ArrayList cls = null;
		void addChangeListener(ChangeListener cl) {
			if (cl == null)
				return;
			if (this.cls == null)
				this.cls = new ArrayList();
			this.cls.add(cl);
		}
		private void stateChanged() {
			this.dirty = true;
			if (this.cls == null)
				return;
			ChangeEvent ce = new ChangeEvent(this);
			for (Iterator lit = this.cls.iterator(); lit.hasNext();)
				((ChangeListener) lit.next()).stateChanged(ce);
		}
	}
	
	private class ProcessorSelectorPanel extends JPanel {
		
		private String processorName = null;
		private String processorProviderClassName = null;
		
		private JButton useDpButton;
		private JButton createDpButton;
		private JLabel processorLabel = new JLabel("<No Document Processor Selected>", JLabel.LEFT);
		
		private boolean dirty = false;
		
		ProcessorSelectorPanel(String dpName) {
			super(new BorderLayout(), true);
			
			if (dpName != null) {
				DocumentProcessor dp = parent.getDocumentProcessorForName(dpName);
				if (dp != null) {
					this.processorName = dp.getName();
					this.processorProviderClassName = dp.getProviderClassName();
					this.processorLabel.setText(dp.getTypeLabel() + ": " + dp.getName() + " (double click to edit)");
				}
			}
			this.processorLabel.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent me) {
					if (ProcessorSelectorPanel.this.isEnabled() && (me.getClickCount() > 1)) {
						DocumentProcessorManager dpm = parent.getDocumentProcessorProvider(processorProviderClassName);
						if (dpm != null)
							dpm.editDocumentProcessor(processorName);
					}
				}
			});
			
			final JPopupMenu useDpMenu = new JPopupMenu();
			final JPopupMenu createDpMenu = new JPopupMenu();
			
			DocumentProcessorManager[] dpms = parent.getDocumentProcessorProviders();
			for (int p = 0; p < dpms.length; p++) {
				final String className = dpms[p].getClass().getName();
				
				JMenuItem useDpMi = new JMenuItem("Use " + dpms[p].getResourceTypeLabel());
				useDpMi.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						selectProcessor(className);
					}
				});
				useDpMenu.add(useDpMi);
				
				JMenuItem createDpMi = new JMenuItem("Create " + dpms[p].getResourceTypeLabel());
				createDpMi.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						createProcessor(className);
					}
				});
				createDpMenu.add(createDpMi);
			}
			
			this.useDpButton = new JButton("Use ...");
			this.useDpButton.setBorder(BorderFactory.createRaisedBevelBorder());
			this.useDpButton.setSize(new Dimension(120, 21));
			this.useDpButton.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent me) {
					if (ProcessorSelectorPanel.this.isEnabled())
						useDpMenu.show(useDpButton, me.getX(), me.getY());
				}
			});
			
			this.createDpButton = new JButton("Create ...");
			this.createDpButton.setBorder(BorderFactory.createRaisedBevelBorder());
			this.createDpButton.setSize(new Dimension(120, 21));
			this.createDpButton.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent me) {
					if (ProcessorSelectorPanel.this.isEnabled())
						createDpMenu.show(createDpButton, me.getX(), me.getY());
				}
			});
			
			JPanel setDpPanel = new JPanel(new GridLayout(1, 2, 3, 0), true);
			setDpPanel.add(this.createDpButton);
			setDpPanel.add(this.useDpButton);
			
			this.add(setDpPanel, BorderLayout.WEST);
			this.add(this.processorLabel, BorderLayout.CENTER);
			this.setBorder(BorderFactory.createEtchedBorder());
		}
		
		void selectProcessor(String providerClassName) {
			DocumentProcessorManager dpm = parent.getDocumentProcessorProvider(providerClassName);
			if (dpm != null) {
				ResourceDialog rd = ResourceDialog.getResourceDialog(dpm, ("Select " + dpm.getResourceTypeLabel()), "Select");
				rd.setLocationRelativeTo(DialogPanel.getTopWindow());
				rd.setVisible(true);
				DocumentProcessor dp = dpm.getDocumentProcessor(rd.getSelectedResourceName());
				if (dp != null) {
					this.processorName = dp.getName();
					this.processorProviderClassName = dp.getProviderClassName();
					this.processorLabel.setText(dp.getTypeLabel() + ": " + dp.getName() + " (double click to edit)");
					this.stateChanged();
				}
			}
		}
		
		void createProcessor(String providerClassName) {
			DocumentProcessorManager dpm = parent.getDocumentProcessorProvider(providerClassName);
			if (dpm != null) {
				String dpName = dpm.createDocumentProcessor();
				DocumentProcessor dp = dpm.getDocumentProcessor(dpName);
				if (dp != null) {
					this.processorName = dp.getName();
					this.processorProviderClassName = dp.getProviderClassName();
					this.processorLabel.setText(dp.getTypeLabel() + ": " + dp.getName() + " (double click to edit)");
					this.stateChanged();
				}
			}
		}
		
		public void setEnabled(boolean enabled) {
			super.setEnabled(enabled);
			this.createDpButton.setEnabled(enabled);
			this.useDpButton.setEnabled(enabled);
		}
		
		boolean isDirty() {
			return this.dirty;
		}
		
		boolean isComplete() {
			return ((this.processorName != null) && (this.processorProviderClassName != null));
		}
		
		String getProcessorName() {
			return (this.isComplete() ? (this.processorName + "@" + this.processorProviderClassName) : null);
		}
		
		private ArrayList cls = null;
		void addChangeListener(ChangeListener cl) {
			if (cl == null)
				return;
			if (this.cls == null)
				this.cls = new ArrayList();
			this.cls.add(cl);
		}
		private void stateChanged() {
			this.dirty = true;
			if (this.cls == null)
				return;
			ChangeEvent ce = new ChangeEvent(this);
			for (Iterator lit = this.cls.iterator(); lit.hasNext();)
				((ChangeListener) lit.next()).stateChanged(ce);
		}
	}
}
