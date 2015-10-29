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
package de.uka.ipd.idaho.goldenGate.plugin.gScript;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Properties;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.border.Border;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.util.GamtaClassLoader;
import de.uka.ipd.idaho.gamta.util.GamtaClassLoader.InputStreamProvider;
import de.uka.ipd.idaho.gamta.util.gPath.GPathVariableResolver;
import de.uka.ipd.idaho.gamta.util.gPath.exceptions.GPathException;
import de.uka.ipd.idaho.gamta.util.gPath.types.GPathAnnotationSet;
import de.uka.ipd.idaho.gamta.util.gPath.types.GPathObject;
import de.uka.ipd.idaho.gamta.util.gPath.types.GPathString;
import de.uka.ipd.idaho.gamta.util.gScript.GScript;
import de.uka.ipd.idaho.gamta.util.gScript.GScriptFunction;
import de.uka.ipd.idaho.gamta.util.gScript.exceptions.GScriptException;
import de.uka.ipd.idaho.goldenGate.DocumentEditor;
import de.uka.ipd.idaho.goldenGate.DocumentEditorDialog;
import de.uka.ipd.idaho.goldenGate.GoldenGATE;
import de.uka.ipd.idaho.goldenGate.GoldenGateConstants;
import de.uka.ipd.idaho.goldenGate.plugins.AbstractDocumentProcessorManager;
import de.uka.ipd.idaho.goldenGate.plugins.AnnotationSourceManager;
import de.uka.ipd.idaho.goldenGate.plugins.DocumentProcessor;
import de.uka.ipd.idaho.goldenGate.plugins.DocumentProcessorManager;
import de.uka.ipd.idaho.goldenGate.plugins.ResourceSplashScreen;
import de.uka.ipd.idaho.goldenGate.util.DataListListener;
import de.uka.ipd.idaho.goldenGate.util.DialogPanel;
import de.uka.ipd.idaho.goldenGate.util.EditFontsButton;
import de.uka.ipd.idaho.goldenGate.util.FontEditable;
import de.uka.ipd.idaho.goldenGate.util.FontEditorDialog;
import de.uka.ipd.idaho.goldenGate.util.FontEditorPanel;
import de.uka.ipd.idaho.goldenGate.util.ResourceDialog;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * Manager for GAMTA Scripts (GScripts). GScripts are powerful document
 * processors. They can create and modify markup, and invoce other document
 * processors. In addition, this plugin provides a command console for executing
 * ad-hoc commands. This is especially useful for fixing uncommon markup
 * problems.<br>
 * <br>
 * All configuration can be done in the 'Edit GScripts' dialog in the GoldenGATE
 * Editor, which provides an editor for GScripts.<br>
 * <br>
 * For deploying custom GScriptFunctions, export the respective classes to jar
 * files and deposit these jar files in the 'Functions' sub folder of the
 * GScriptManager's data path.
 * 
 * @see de.uka.ipd.idaho.gamta.util.gScript.GScript
 * 
 * @author sautter
 */
public class GScriptManager extends AbstractDocumentProcessorManager implements GScript.ScriptNameResolver {
	
	private static final String FUNCTION_FOLDER_NAME = "Functions";
	private static final String FILE_EXTENSION = ".gScript";
	
	private static final String CONSOLE_SCRIPT_NAME = "<Gamta Script Console>";
	private static final String CONSOLE_SCRIPT = "START_CONSOLE";
	private static final String[] FIX_SCRIPT_NAMES = {CONSOLE_SCRIPT_NAME};
	private static final String[] FIX_SCRIPTS = {CONSOLE_SCRIPT};
	
	private JFileChooser fileChooser = null;
	
	private String[] functionNames;
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager#init()
	 */
	public void init() {
		
		//	load custom functions
		Object[] functionObjects = GamtaClassLoader.loadComponents(
				this.dataProvider.getDataNames(), 
				FUNCTION_FOLDER_NAME, 
//				new UrlProvider() {
//					public URL getURL(String dataName) throws IOException {
//						return dataProvider.getURL(dataName);
//					}
//				}, 
				new InputStreamProvider() {
					public InputStream getInputStream(String dataName) throws IOException {
						return dataProvider.getInputStream(dataName);
					}
				}, 
				GScriptFunction.class, 
				null);
		
		//	collect custom functions
		ArrayList functionList = new ArrayList();
		for (int f = 0; f < functionObjects.length; f++)
			functionList.add((GScriptFunction) functionObjects[f]);
		
		//	add standard functions
		functionList.add(new RunFunction(this));
		functionList.add(new ListAnnotationSourcesFunction(this.parent));
		functionList.add(new ListDocumentProcessorsFunction(this.parent));
		functionList.add(new ListFunctionsFunction());
		
		//	register functions with GScript
		GScriptFunction[] functions = ((GScriptFunction[]) functionList.toArray(new GScriptFunction[functionList.size()]));
		this.functionNames = new String[functions.length];
		for (int f = 0; f < functions.length; f++) {
			String functionName = functions[f].getName();
			this.functionNames[f] = functionName;
			GScript.addFunction(functions[f]);
		}
		
		//	self-register as script name resolver
		GScript.addScriptNameResolver(this);
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager#getResourceNames()
	 */
	public String[] getResourceNames() {
		StringVector scripts = new StringVector();
		scripts.addContentIgnoreDuplicates(FIX_SCRIPT_NAMES);
		scripts.addContentIgnoreDuplicates(super.getResourceNames());
		return scripts.toStringArray();
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager#getRequiredResourceNames(java.lang.String, boolean)
	 */
	public String[] getRequiredResourceNames(String name, boolean recourse) {
		StringVector names = new StringVector();
		
		//	add document processors called via the run-function
		String gScriptString = this.getScript(name);
		if (gScriptString != null) {
			String[] gScriptLines = gScriptString.split("\\s*\\n\\s*");
			for (int l = 0; l < gScriptLines.length; l++) try {
				String gScriptLine = gScriptLines[l].trim();
				if ((gScriptLine.length() == 0) || gScriptLine.startsWith(GScript.COMMENT_LINE_START))
					continue;
				String[] gScriptTokens = GScript.tokenizeCommand(gScriptLines[l]);
				if (gScriptTokens.length < 2)
					continue;
				if (!RunFunction.RUN_FUNCTION_NAME.equals(gScriptTokens[0]))
					continue;
				
				DocumentProcessor dp = this.parent.getDocumentProcessorForName(gScriptTokens[1]);
				if (dp == null)
					continue;
				names.addElementIgnoreDuplicates(dp.getName() + "@" + dp.getProviderClassName());
				
				if (!recourse)
					continue;
				
				DocumentProcessorManager dpm = this.parent.getDocumentProcessorProvider(dp.getProviderClassName());
				if (dpm == null)
					continue;
				names.addContentIgnoreDuplicates(dpm.getRequiredResourceNames(dp.getName(), recourse));
			}
			catch (GScriptException gse) {}
		}
		
		//	finally ...
		return names.toStringArray();
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager#getDataNamesForResource(java.lang.String)
	 */
	public String[] getDataNamesForResource(String name) {
		StringVector names = new StringVector();
		names.addContentIgnoreDuplicates(super.getDataNamesForResource(name));
		
		//	add functions
		names.addElementIgnoreDuplicates(FUNCTION_FOLDER_NAME + "/" + "@" + this.getClass().getName());
		
		//	add document processors called via the run-function
		String gScriptString = this.getScript(name);
		if (gScriptString != null) {
			String[] gScriptLines = gScriptString.split("\\s*\\n\\s*");
			for (int l = 0; l < gScriptLines.length; l++) try {
				String gScriptLine = gScriptLines[l].trim();
				if ((gScriptLine.length() == 0) || gScriptLine.startsWith(GScript.COMMENT_LINE_START))
					continue;
				String[] gScriptTokens = GScript.tokenizeCommand(gScriptLines[l]);
				if (gScriptTokens.length < 2)
					continue;
				if (!RunFunction.RUN_FUNCTION_NAME.equals(gScriptTokens[0]))
					continue;
				
				DocumentProcessor dp = this.parent.getDocumentProcessorForName(gScriptTokens[1]);
				if (dp == null)
					continue;
				
				DocumentProcessorManager dpm = this.parent.getDocumentProcessorProvider(dp.getProviderClassName());
				if (dpm == null)
					continue;
				names.addContentIgnoreDuplicates(dpm.getDataNamesForResource(dp.getName()));
			}
			catch (GScriptException gse) {}
		}
		
		//	finally ...
		return names.toStringArray();
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager#getResourceTypeLabel()
	 */
	public String getResourceTypeLabel() {
		return "Gamta Script";
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractDocumentProcessorManager#getDocumentProcessor(java.lang.String)
	 */
	public DocumentProcessor getDocumentProcessor(String name) {
		if (CONSOLE_SCRIPT_NAME.equals(name)) return new GScriptConsoleDocumentProcessor(null);
		String script = this.getScript(name);
		if (CONSOLE_SCRIPT.equals(script)) return new GScriptConsoleDocumentProcessor(null);
		return ((script == null) ? null : new GScriptDocumentProcessor(name, script));
	}
	
	private class GScriptDocumentProcessor implements DocumentProcessor {
		
		private String name;
		private String scriptText;
		private GScript script;
		
		GScriptDocumentProcessor(String name, String script) {
			this.name = name;
			this.scriptText = script;
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
			return "Gamta Script";
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentProcessor#getProviderClassName()
		 */
		public String getProviderClassName() {
			return GScriptManager.class.getName();
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentProcessor#process(de.uka.ipd.idaho.gamta.MutableAnnotation)
		 */
		public void process(MutableAnnotation data) {
			this.process(data, new Properties());
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentProcessor#process(de.uka.ipd.idaho.gamta.MutableAnnotation, java.util.Properties)
		 */
		public void process(MutableAnnotation data, Properties parameters) {
			try {
				if (this.script == null)
					this.script = GScript.compile(this.scriptText);
				this.script.execute(data);
//				GScript.executeScript(data, this.script);
			}
			catch (GScriptException gse) {
				throw new RuntimeException(gse);
			}
		}
	}

	private class GScriptConsoleDocumentProcessor implements DocumentProcessor {
		
		private String name = "Gamta Script Console";
		
		private ResourceSplashScreen splashScreen;
		
		GScriptConsoleDocumentProcessor(ResourceSplashScreen splashScreen) {
			this.splashScreen = splashScreen;
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
			return "Gamta Script";
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentProcessor#getProviderClassName()
		 */
		public String getProviderClassName() {
			return GScriptManager.class.getName();
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentProcessor#process(de.uka.ipd.idaho.gamta.MutableAnnotation)
		 */
		public void process(MutableAnnotation data) {
			this.process(data, new Properties());
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentProcessor#process(de.uka.ipd.idaho.gamta.MutableAnnotation, java.util.Properties)
		 */
		public void process(MutableAnnotation data, Properties parameters) {
			if (parameters.containsKey(INTERACTIVE_PARAMETER)) {
				final DialogPanel dialog = new DialogPanel("GolcenGATE Script Console", true);
				dialog.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
				JButton closeButton = new JButton("Close");
				closeButton.setBorder(BorderFactory.createRaisedBevelBorder());
				closeButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						dialog.dispose();
					}
				});
				
				dialog.setLayout(new BorderLayout());
				dialog.add(new GScriptConsole(data), BorderLayout.CENTER);
				dialog.add(closeButton, BorderLayout.SOUTH);
				
				dialog.setSize(800, 600);
				dialog.setLocationRelativeTo(this.splashScreen);
				dialog.setVisible(true);
			}
		}
	}

	private class GScriptConsole extends JPanel {
		
		private JTextArea consoleInputField = new JTextArea();
		private JTextArea consoleInputHistoryView = new JTextArea();
		
		private StringVector consoleInputHistory = new StringVector();
		private int consoleInputHistoryPosition = 0;
		private String lastTypedConsoleInput = "";
		
		private JTextArea outputView = new JTextArea();
		
		private MutableAnnotation data;
		
		GScriptConsole(MutableAnnotation data) {
			super(new BorderLayout(), true);
			
			this.data = data;
			
			this.consoleInputHistoryView.setEditable(false);
			this.consoleInputHistoryView.setLineWrap(true);
			this.consoleInputHistoryView.setWrapStyleWord(true);
			
			this.consoleInputField.setEditable(true);
			this.consoleInputField.setLineWrap(true);
			this.consoleInputField.setWrapStyleWord(true);
			this.consoleInputField.addKeyListener(new KeyAdapter() {
				boolean returnDown = false;
				public void keyPressed(KeyEvent ke) {
					if (ke.getKeyChar() == '\n') {
						this.returnDown = true;
						ke.consume();
					}
				}
				public void keyReleased(KeyEvent ke) {
					if ((ke.getKeyChar() == '\n') && this.returnDown) {
						ke.consume();
						executeCommand();
					}
					this.returnDown = false;
				}
			});
			this.consoleInputField.getKeymap().addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_UP, KeyEvent.CTRL_DOWN_MASK), new AbstractAction() {
				public void actionPerformed(ActionEvent ae) {
					if (consoleInputHistoryPosition == consoleInputHistory.size())
						lastTypedConsoleInput = consoleInputField.getText().trim();
					if (consoleInputHistoryPosition > 0) {
						consoleInputHistoryPosition --;
						consoleInputField.setText(consoleInputHistory.get(consoleInputHistoryPosition));
					}
				}
			});
			this.consoleInputField.getKeymap().addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, KeyEvent.CTRL_DOWN_MASK), new AbstractAction() {
				public void actionPerformed(ActionEvent ae) {
					if ((consoleInputHistoryPosition + 1) < consoleInputHistory.size()) {
						consoleInputHistoryPosition ++;
						consoleInputField.setText(consoleInputHistory.get(consoleInputHistoryPosition));
					} else consoleInputField.setText(lastTypedConsoleInput);
				}
			});
			
			JPanel consoleInputPanel = new JPanel(new BorderLayout());
			JScrollPane consoleInputHistoryBox = new JScrollPane(this.consoleInputHistoryView);
			consoleInputPanel.add(consoleInputHistoryBox, BorderLayout.CENTER);
			JScrollPane consoleInputBox = new JScrollPane(this.consoleInputField);
			consoleInputBox.setPreferredSize(new Dimension(60, 60));
			consoleInputPanel.add(consoleInputBox, BorderLayout.SOUTH);
			
			// catch output
			GScript.setOutputDestination(new PrintWriter(System.out) {
				public void println(String line) {
					output(line);
				}
			});
			
			JScrollPane outputBox = new JScrollPane(this.outputView);
			JSplitPane inOutSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, consoleInputPanel, outputBox);
			inOutSplit.setDividerLocation(0.5);
			inOutSplit.setResizeWeight(0.5);
			
			this.add(inOutSplit, BorderLayout.CENTER);
		}
		
		void executeCommand() {
			String commandInput = consoleInputField.getText().trim();
			consoleInputField.setText("");
			if (commandInput.trim().length() == 0) return;
			else if ("?".equals(commandInput)) commandInput = "listF";
			
			consoleInputHistory.addElement(commandInput);
			consoleInputHistoryPosition = consoleInputHistory.size();
			consoleInputHistoryView.append("\n ---------- \n" + commandInput);
			
			output("");
			try {
				String result = GScript.executeCommand(this.data, commandInput);
				output(result);
			} catch (Exception e) {
				output("Exception executing command:");
				output(e.getMessage());
			}
		}
		
		void output(String outLine) {
			this.outputView.append(outLine + "\n");
			this.outputView.setCaretPosition(outputView.getText().length());
		}
	}
	
	/*
	 * @see de.uka.ipd.idaho.gamta.util.gScript.GScript.ScriptNameResolver#getScriptForName(java.lang.String)
	 */
	public String getScriptForName(String scriptName) {
		return this.getScript(scriptName);
	}
	
	/* retrieve a plain String representation of a regular expression by its name
	 * @param	name	the name of the reqired regular expression
	 * @return the String representation of the regular expression with the required name, or null, if there is no such regular expression
	 */
	private String getScript(String name) {
		if (name == null) return null;
		
		for (int r = 0; r < FIX_SCRIPT_NAMES.length; r++) 
			if (name.equals(FIX_SCRIPT_NAMES[r])) return FIX_SCRIPTS[r];
		
		return this.loadStringResource(name);
	}
	
	/**
	 * Retrieve a DocumentProcessor provided by an arbitrary
	 * DocumentProcessorManager present in GoldenGATE
	 * @param name the name of the desired DocumentProcessor
	 * @return the DocumentProcessor with the specified name, or null if there
	 *         is no such DocumentProcessor in any of the Managers available
	 */
	DocumentProcessor getProcessor(String name) {
		DocumentProcessorManager[] dpm = this.parent.getDocumentProcessorProviders();
		for (int m = 0; m < dpm.length; m++) {
			DocumentProcessor dp = dpm[m].getDocumentProcessor(name);
			if (dp != null) return dp;
		}
		return null;
	}
	
	/* 
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentProcessorManager#createDocumentProcessor()
	 */
	public String createDocumentProcessor() {
		return this.createScript("", null);
	}
	
	/* 
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentProcessorManager#editDocumentProcessor(java.lang.String)
	 */
	public void editDocumentProcessor(String name) {
		String script = this.loadStringResource(name);
		if ((script == null) || (script.length() == 0)) return;
		
		EditGScriptDialog esd = new EditGScriptDialog(name, script);
		esd.setVisible(true);
		
		if (esd.isCommitted()) try {
			this.storeStringResource(name, esd.getScript());
		} catch (IOException ioe) {}
	}
	
	/* 
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentProcessorManager#editDocumentProcessors()
	 */
	public void editDocumentProcessors() {
		this.editScripts();
	}
	
	/*
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
		
		//	get script
		if (CONSOLE_SCRIPT_NAME.equals(pn)) {
			
			//	apply processor
			ResourceSplashScreen splashScreen = new ResourceSplashScreen((this.getResourceTypeLabel() + " Running ..."), ("Please wait while '" + this.getResourceTypeLabel() + ": " + pn + "' is processing the Document ..."));
			DocumentProcessor dp = new GScriptConsoleDocumentProcessor(splashScreen);
			data.applyDocumentProcessor(dp, splashScreen, parameters);
		}
		else {
			String script = this.getScript(pn);
			if (script != null) {
				
				//	apply processor
				ResourceSplashScreen splashScreen = new ResourceSplashScreen((this.getResourceTypeLabel() + " Running ..."), ("Please wait while '" + this.getResourceTypeLabel() + ": " + pn + "' is processing the Document ..."));
				DocumentProcessor dp = new GScriptDocumentProcessor(pn, script);
				data.applyDocumentProcessor(dp, splashScreen, parameters);
			}
		}
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager#getFileExtension()
	 */
	protected String getFileExtension() {
		return FILE_EXTENSION;
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager#getMainMenuTitle()
	 */
	public String getMainMenuTitle() {
		return "Gamta Scripts";
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager#getToolsMenuLabel()
	 */
	public String getToolsMenuLabel() {
		return "Execute";
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager#getToolsMenuFunctionItems(de.uka.ipd.idaho.goldenGate.GoldenGateConstants.InvokationTargetProvider)
	 */
	public JMenuItem[] getToolsMenuFunctionItems(final InvokationTargetProvider targetProvider) {
		ArrayList collector = new ArrayList();
		JMenuItem mi;
		
		mi = new JMenuItem("Open Console");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				DocumentEditor target = targetProvider.getFunctionTarget();
				if (target != null)
					openConsole(target);
			}
		});
		collector.add(mi);
		
		return ((JMenuItem[]) collector.toArray(new JMenuItem[collector.size()]));
	}

	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager#getMainMenuItems()
	 */
	public JMenuItem[] getMainMenuItems() {
		if (!this.dataProvider.isDataEditable())
			return new JMenuItem[0];
		
		ArrayList collector = new ArrayList();
		JMenuItem mi;
		
		mi = new JMenuItem("Create");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				createScript();
			}
		});
		collector.add(mi);
		mi = new JMenuItem("Load");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				loadScript();
			}
		});
		collector.add(mi);
		mi = new JMenuItem("Edit");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				editScripts();
			}
		});
		collector.add(mi);
		collector.add(GoldenGateConstants.MENU_SEPARATOR_ITEM);
		
		mi = new JMenuItem("Open Console");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				openConsole(parent.getActivePanel());
			}
		});
		collector.add(mi);
		
		return ((JMenuItem[]) collector.toArray(new JMenuItem[collector.size()]));
	}
	
	private void openConsole(DocumentEditor documentEditor) {
		if (documentEditor != null)
			documentEditor.applyDocumentProcessor(this.getClass().getName(), CONSOLE_SCRIPT_NAME);
	}
	
	private boolean createScript() {
		return (this.createScript(null, null) != null);
	}
	
	private boolean loadScript() {
		if (this.fileChooser == null) try {
			this.fileChooser = new JFileChooser();
		} catch (SecurityException se) {}
		
		if ((this.fileChooser != null) && (this.fileChooser.showOpenDialog(DialogPanel.getTopWindow()) == JFileChooser.APPROVE_OPTION)) {
			File file = this.fileChooser.getSelectedFile();
			if ((file != null) && file.isFile()) {
				try {
					String fileName = file.toString();
					fileName = fileName.substring(fileName.lastIndexOf(File.separatorChar) + 1);
					StringVector scriptLines = StringVector.loadList(file);
					return (this.createScript(scriptLines.concatStrings("\n"), fileName) != null);
				}
				catch (IOException ioe) {
					JOptionPane.showMessageDialog(DialogPanel.getTopWindow(), (ioe.getClass().getName() + " (" + ioe.getMessage() + ")\nwhile trying to load " + file.toString()), "Could Not Read File", JOptionPane.ERROR_MESSAGE);
				}
			}
		}
		return false;
	}
	
	private boolean cloneScript() {
		String selectedName = this.resourceNameList.getSelectedName();
		if (selectedName == null)
			return this.createScript();
		else {
			String name = "New " + selectedName;
			return (this.createScript(this.loadStringResource(selectedName), name) != null);
		}
	}
	
	private String createScript(String modelScript, String name) {
		CreateGScriptDialog cred = new CreateGScriptDialog(name, modelScript);
		cred.setVisible(true);
		if (cred.isCommitted()) {
			String script = cred.getScript();
			String scriptName = cred.getScriptName();
			if (!scriptName.endsWith(FILE_EXTENSION)) scriptName += FILE_EXTENSION;
			try {
				if (this.storeStringResource(scriptName, script)) {
					this.resourceNameList.refresh();
					return scriptName;
				}
			} catch (IOException e) {}
		}
		return null;
	}
	
	private void editScripts() {
		final GScriptEditorPanel[] editor = new GScriptEditorPanel[1];
		editor[0] = null;
		
		final DialogPanel editDialog = new DialogPanel("Edit Gamta Scripts", true);
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
						storeStringResource(editor[0].scriptName, editor[0].getScript());
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
				createScript();
			}
		});
		editButtons.add(button);
		button = new JButton("Load");
		button.setBorder(BorderFactory.createRaisedBevelBorder());
		button.setPreferredSize(new Dimension(100, 21));
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				loadScript();
			}
		});
		editButtons.add(button);
		button = new JButton("Clone");
		button.setBorder(BorderFactory.createRaisedBevelBorder());
		button.setPreferredSize(new Dimension(100, 21));
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				cloneScript();
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
			String script = this.loadStringResource(selectedName);
			if (script == null)
				editorPanel.add(this.getExplanationLabel(), BorderLayout.CENTER);
			else {
				editor[0] = new GScriptEditorPanel(selectedName, script);
				editorPanel.add(editor[0], BorderLayout.CENTER);
			}
		}
		editDialog.add(editorPanel, BorderLayout.CENTER);
		
		editDialog.add(this.resourceNameList, BorderLayout.EAST);
		DataListListener dll = new DataListListener() {
			public void selected(String dataName) {
				if ((editor[0] != null) && editor[0].isDirty()) {
					try {
						storeStringResource(editor[0].scriptName, editor[0].getScript());
					}
					catch (IOException ioe) {
						if (JOptionPane.showConfirmDialog(editDialog, (ioe.getClass().getName() + " (" + ioe.getMessage() + ")\nwhile saving file to " + editor[0].scriptName + "\nProceed?"), "Could Not Save Analyzer", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
							resourceNameList.setSelectedName(editor[0].scriptName);
							editorPanel.validate();
							return;
						}
					}
				}
				editorPanel.removeAll();
				
				if (dataName == null)
					editorPanel.add(getExplanationLabel(), BorderLayout.CENTER);
				else {
					String script = loadStringResource(dataName);
					if (script == null)
						editorPanel.add(getExplanationLabel(), BorderLayout.CENTER);
					else {
						editor[0] = new GScriptEditorPanel(dataName, script);
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
	
	private class CreateGScriptDialog extends DialogPanel {
		
		private JTextField nameField;
		
		private GScriptEditorPanel editor;
		private String script = null;
		private String scriptName = null;
		
		CreateGScriptDialog(String name, String script) {
			super("Create Gamta Script", true);
			
			this.nameField = new JTextField((name == null) ? "New GScript" : name);
			this.nameField.setBorder(BorderFactory.createLoweredBevelBorder());
			
			//	initialize main buttons
			JButton commitButton = new JButton("Create");
			commitButton.setBorder(BorderFactory.createRaisedBevelBorder());
			commitButton.setPreferredSize(new Dimension(100, 21));
			commitButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					CreateGScriptDialog.this.script = editor.getScript();
					scriptName = nameField.getText();
					dispose();
				}
			});
			
			JButton abortButton = new JButton("Cancel");
			abortButton.setBorder(BorderFactory.createRaisedBevelBorder());
			abortButton.setPreferredSize(new Dimension(100, 21));
			abortButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					CreateGScriptDialog.this.script = null;
					scriptName = null;
					dispose();
				}
			});
			
			JPanel mainButtonPanel = new JPanel(new FlowLayout());
			mainButtonPanel.add(commitButton);
			mainButtonPanel.add(abortButton);
			
			//	initialize editor
			this.editor = new GScriptEditorPanel(name, script);
			
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
			return (this.script != null);
		}
		
		String getScript() {
			return this.script;
		}
		
		String getScriptName() {
			return this.scriptName;
		}
	}

	private class EditGScriptDialog extends DialogPanel {
		
		private GScriptEditorPanel editor;
		private String script = null;
		
		EditGScriptDialog(String name, String gScript) {
			super(("Edit Gamta Script '" + name + "'"), true);
			
			//	initialize main buttons
			JButton commitButton = new JButton("OK");
			commitButton.setBorder(BorderFactory.createRaisedBevelBorder());
			commitButton.setPreferredSize(new Dimension(100, 21));
			commitButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					EditGScriptDialog.this.script = editor.getScript();
					dispose();
				}
			});
			
			JButton abortButton = new JButton("Cancel");
			abortButton.setBorder(BorderFactory.createRaisedBevelBorder());
			abortButton.setPreferredSize(new Dimension(100, 21));
			abortButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					EditGScriptDialog.this.script = null;
					dispose();
				}
			});
			
			JPanel mainButtonPanel = new JPanel(new FlowLayout());
			mainButtonPanel.add(commitButton);
			mainButtonPanel.add(abortButton);
			
			//	initialize editor
			this.editor = new GScriptEditorPanel(name, gScript);
			
			//	put the whole stuff together
			this.setLayout(new BorderLayout());
			this.add(this.editor, BorderLayout.CENTER);
			this.add(mainButtonPanel, BorderLayout.SOUTH);
			
			this.setResizable(true);
			this.setSize(new Dimension(600, 400));
			this.setLocationRelativeTo(DialogPanel.getTopWindow());
		}
		
		boolean isCommitted() {
			return (this.script != null);
		}
		
		String getScript() {
			return this.script;
		}
	}

	private static final String[] OPTIONS = {GScript.INCLUDE_COMMAND, GScript.CALL_COMMAND};
	
	private class GScriptEditorPanel extends JPanel implements FontEditable, DocumentListener {
		
		private JPanel functionQuickPanel = new JPanel(new GridBagLayout());
		private JScrollPane functionQuickPanelBox = new JScrollPane(this.functionQuickPanel);
		
		private JPanel documentProcessorQuickPanel = new JPanel(new GridBagLayout());
		private JScrollPane documentProcessorQuickPanelBox = new JScrollPane(this.documentProcessorQuickPanel);
		
		private JPanel scriptQuickPanel = new JPanel(new GridBagLayout());
		private JScrollPane scriptQuickPanelBox = new JScrollPane(this.scriptQuickPanel);
		
		private JTabbedPane quickPanelTabs = new JTabbedPane();
		
		private JTextArea editor;
		
		private String content = "";
		
		private String fontName = "Verdana";
		private int fontSize = 12;
		private Color fontColor = Color.BLACK;
		
		private boolean dirty = false;
		private String scriptName;
		
		GScriptEditorPanel(String name, String gScript) {
			super(new BorderLayout(), true);
			this.scriptName = name;
			
			//	initialize editor
			this.editor = new JTextArea();
			this.editor.setEditable(true);
			this.editor.setLineWrap(false);
			this.editor.setWrapStyleWord(false);
			
			//	wrap editor in scroll pane
			JScrollPane editorBox = new JScrollPane(this.editor);
			
			//	initialize buttons
			JButton validateButton = new JButton("Validate");
			validateButton.setBorder(BorderFactory.createRaisedBevelBorder());
			validateButton.setPreferredSize(new Dimension(115, 21));
			validateButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					validateScript();
				}
			});
			
			JButton testButton = new JButton("Test");
			testButton.setBorder(BorderFactory.createRaisedBevelBorder());
			testButton.setPreferredSize(new Dimension(115, 21));
			testButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					testScript();
				}
			});
			
			JButton debugButton = new JButton("Debug");
			debugButton.setBorder(BorderFactory.createRaisedBevelBorder());
			debugButton.setPreferredSize(new Dimension(100, 21));
			debugButton.addActionListener(new  ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					debugScript();
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
			buttonPanel.add(validateButton, gbc.clone());
			gbc.gridx ++;
			buttonPanel.add(testButton, gbc.clone());
			gbc.gridx ++;
			buttonPanel.add(debugButton, gbc.clone());
			
			//	put editor together
			JPanel editorPanel = new JPanel(new BorderLayout());
			editorPanel.add(editorBox, BorderLayout.CENTER);
			editorPanel.add(buttonPanel, BorderLayout.SOUTH);
			
			//	put quick panels together
			this.functionQuickPanel.addAncestorListener(new AncestorListener() {
				public void ancestorAdded(AncestorEvent event) {
					buildFunctionQuickPanel();
					validate();
				}
				public void ancestorMoved(AncestorEvent event) {}
				public void ancestorRemoved(AncestorEvent event) {}
			});
			this.quickPanelTabs.addTab("Functions", null, this.functionQuickPanelBox, "Available Functions");
			
			this.buildDocumentProcessorQuickPanel(null);
			this.quickPanelTabs.addTab("Document Processors", null, this.documentProcessorQuickPanelBox, "Available DocumentProcessors");
			
			this.scriptQuickPanel.addAncestorListener(new AncestorListener() {
				public void ancestorAdded(AncestorEvent event) {
					buildScriptQuickPanel();
					validate();
				}
				public void ancestorMoved(AncestorEvent event) {}
				public void ancestorRemoved(AncestorEvent event) {}
			});
			this.quickPanelTabs.addTab("Scripts", null, this.scriptQuickPanelBox, "Available Gamta Scripts for Inclusion");
			
			//	put the whole stuff together
			this.add(editorPanel, BorderLayout.CENTER);
			this.add(this.quickPanelTabs, BorderLayout.WEST);
			
			this.setContent((gScript == null) ? "" : gScript);
		}
		
		void buildFunctionQuickPanel() {
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.weightx = 1;
			gbc.weighty = 0;
			gbc.gridwidth = 1;
			gbc.gridheight = 1;
			gbc.gridx = 0;
			gbc.gridy = 0;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			
			String[] functionNames = GScript.getFunctionNames();
			for (int n = 0; n < functionNames.length; n++) {
				
				String[][] functionDescriptions = GScript.getFunctionDescriptions(functionNames[n]);
				for (int f = 0; f < functionDescriptions.length; f++) {
					final String functionName = functionNames[n];
					final int paramCount = (functionDescriptions[f].length);
					
					StringBuffer tooltip = new StringBuffer("<HTML><B>" + functionDescriptions[f][0] + "</B>");
					for (int d = 1; d < functionDescriptions[f].length; d++)
						tooltip.append("<BR>" + functionDescriptions[f][d]);
					tooltip.append("</HTML>");
					
					JLabel fLabel = new JLabel(functionNames[n], JLabel.LEFT);
					fLabel.setToolTipText(tooltip.toString());
					fLabel.addMouseListener(new MouseAdapter() {
						public void mouseClicked(MouseEvent me) {
							if (me.getClickCount() > 1) { // double click only
								GPathString[] params = new GPathString[paramCount];
								GScriptFunction function = GScript.getFunction(functionName, params);
								insertFunction(function);
							}
						}
					});
					this.functionQuickPanel.add(fLabel, gbc.clone());
					gbc.gridy++;
				}
			}
			
			gbc.weighty = 1;
			this.functionQuickPanel.add(new JPanel(), gbc.clone());
		}
		
		void insertFunction(GScriptFunction function) {
			if (function == null) return;
			StringBuffer sb = new StringBuffer(function.getName());
			String[] paramNames = function.getParameterNames();
			String[] paramTypes = function.getParameterTypes();
			for (int p = 0; p < paramNames.length; p++) {
				sb.append(" ");
				if (GScript.GPATH_ANNOTATION_SET_TYPE.equals(paramTypes[p]))
					sb.append(GScript.ANNOTATION_SET_CONSTRUCTOR_START + GScript.GPATH_ANNOTATION_SET_TYPE + " " + paramNames[p] + GScript.ANNOTATION_SET_CONSTRUCTOR_END);
				else if (GScript.GPATH_NUMBER_TYPE.equals(paramTypes[p]))
					sb.append(GScript.EXPRESSION_START + GScript.GPATH_NUMBER_TYPE + " " + paramNames[p] + GScript.EXPRESSION_END);
				else if (GScript.GPATH_BOOLEAN_TYPE.equals(paramTypes[p]))
					sb.append(GScript.EXPRESSION_START + GScript.GPATH_BOOLEAN_TYPE + " " + paramNames[p] + GScript.EXPRESSION_END);
				else if (GScript.GPATH_STRING_TYPE.equals(paramTypes[p]))
					sb.append('"' + GScript.GPATH_STRING_TYPE + " " + paramNames[p] + '"');
				else sb.append(GScript.ANNOTATION_SET_CONSTRUCTOR_START + GScript.GPATH_OBJECT_TYPE + " " + paramNames[p] + GScript.ANNOTATION_SET_CONSTRUCTOR_END);
			}
			this.insertStringInScript(sb.toString());
		}
		
		void buildDocumentProcessorQuickPanel(String expandProviderClassName) {
			this.documentProcessorQuickPanel.removeAll();
			
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.weightx = 1;
			gbc.weighty = 0;
			gbc.gridwidth = 1;
			gbc.gridheight = 1;
			gbc.gridx = 0;
			gbc.gridy = 0;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			
			DocumentProcessorManager[] dpm = parent.getDocumentProcessorProviders();
			for (int m = 0; m < dpm.length; m++) {
				
				//	scripts are in special panel
				if (dpm[m] instanceof GScriptManager) continue;
				
				final String providerClassName = dpm[m].getClass().getName();
				
				JLabel dpTypeLabel = new JLabel(("----- " + dpm[m].getMainMenuTitle() + " -----"), JLabel.LEFT);
				dpTypeLabel.addMouseListener(new MouseAdapter() {
					public void mouseClicked(MouseEvent me) {
						buildDocumentProcessorQuickPanel(providerClassName);
						validate();
					}
				});
				this.documentProcessorQuickPanel.add(dpTypeLabel, gbc.clone());
				gbc.gridy++;
				
				if (providerClassName.equals(expandProviderClassName)) {
					
					String[] dpNames = dpm[m].getResourceNames();
					for (int d = 0; d < dpNames.length; d++) {
						final String processorName = dpNames[d];
						
						JLabel dpLabel = new JLabel(dpNames[d], JLabel.LEFT);
						dpLabel.setToolTipText("Insert 'run " + dpNames[d] + "'");
						dpLabel.addMouseListener(new MouseAdapter() {
							public void mouseClicked(MouseEvent me) {
								if (me.getClickCount() > 1) { // double click only
									insertDocumentProcessor(providerClassName, processorName);
								}
							}
						});
						this.documentProcessorQuickPanel.add(dpLabel, gbc.clone());
						gbc.gridy++;
					}
				}
			}
			
			gbc.weighty = 1;
			this.documentProcessorQuickPanel.add(new JPanel(), gbc.clone());
		}
		
		void insertDocumentProcessor(String providerClassName, String processorName) {
			DocumentProcessorManager dpm = parent.getDocumentProcessorProvider(providerClassName);
			if (dpm == null) return;
			DocumentProcessor dp = dpm.getDocumentProcessor(processorName);
			if (dp == null) return;
			this.insertStringInScript(RunFunction.RUN_FUNCTION_NAME + " '" + processorName + "' " + GScript.ANNOTATION_SET_CONSTRUCTOR_START + GScript.GPATH_ANNOTATION_SET_TYPE + " annotationSet (optional)" + GScript.ANNOTATION_SET_CONSTRUCTOR_END);
		}
		
		void buildScriptQuickPanel() {
			this.scriptQuickPanel.removeAll();
			
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.weightx = 1;
			gbc.weighty = 0;
			gbc.gridwidth = 1;
			gbc.gridheight = 1;
			gbc.gridx = 0;
			gbc.gridy = 0;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			
			String[] scriptNames = getResourceNames();
			for (int s = 0; s < scriptNames.length; s++) {
				final String scriptName = scriptNames[s];
				
				JLabel sLabel = new JLabel(scriptNames[s], JLabel.LEFT);
				sLabel.setToolTipText("Insert call to '" + scriptNames[s] + "'");
				sLabel.addMouseListener(new MouseAdapter() {
					public void mouseClicked(MouseEvent me) {
						if (me.getClickCount() > 1) { // double click only
							insertScript(scriptName);
						}
					}
				});
				this.scriptQuickPanel.add(sLabel, gbc.clone());
				gbc.gridy++;
			}
			
			gbc.weighty = 1;
			this.scriptQuickPanel.add(new JPanel(), gbc.clone());
		}
		
		void insertScript(String scriptName) {
			String script = getScriptForName(scriptName);
			if (script == null) return;
			Object option = JOptionPane.showInputDialog(this, ("Select how to include '" + scriptName + "'"), "Select Mode", JOptionPane.QUESTION_MESSAGE, null, OPTIONS, GScript.INCLUDE_COMMAND);
			if (option == null) return;
			this.insertStringInScript(option.toString() + " '" + scriptName + "' " + GScript.ANNOTATION_SET_CONSTRUCTOR_START + GScript.GPATH_ANNOTATION_SET_TYPE + " annotationSet (optional)" + GScript.ANNOTATION_SET_CONSTRUCTOR_END);
		}
		
		void insertStringInScript(String insert) {
			if (this.isDirty()) this.content = this.editor.getText();
			StringVector lines = new StringVector();
			lines.parseAndAddElements(this.content, "\n");
			int caretPos = this.editor.getCaretPosition();
			int insertPos = 0;
			for (int l = 0; l < lines.size(); l++) {
				if (caretPos > insertPos) insertPos += lines.get(l).length() + 1;
				else {
					this.editor.insert((insert + "\n"), insertPos);
					return;
				}
			}
			this.editor.append(((insertPos == 0) ? "" : "\n") + insert);
		}
		
		String getScript() {
			if (this.isDirty()) this.content = this.editor.getText();
			return this.content;
		}
		
		void setContent(String script) {
			this.content = script;
			this.refreshDisplay();
			this.dirty = false;
		}
		
		boolean isDirty() {
			return this.dirty;
		}
		
		void validateScript() {
			String script = this.editor.getText().trim();
			if (script.length() == 0) return;
			
			StringVector messages = new StringVector();
			String validationError;
			try {
				validationError = GScript.validateScript(script, messages);
			}
			catch (GScriptException e) {
				validationError = e.getMessage();
			}
			if (validationError == null)
				this.showMessageDialog(("The script is valid." + (messages.isEmpty() ? "" : ("  \n" + messages.concatStrings("\n  ")))), "Script Validation", JOptionPane.INFORMATION_MESSAGE);
			else this.showMessageDialog(("The script is not valid:\n" + validationError + (messages.isEmpty() ? "" : ("  \n" + messages.concatStrings("\n  ")))), "Script Validation", JOptionPane.ERROR_MESSAGE);
		}
		
		void testScript() {
			
			//	get & validate script
			String script = this.editor.getText().trim();
			if (script.length() == 0) return;
			
			//	if script is valid, apply it to copy of current document
			StringVector messages = new StringVector();
			String validationError;
			try {
				validationError = GScript.validateScript(script, messages);
			}
			catch (GScriptException e) {
				validationError = e.getMessage();
			}
			if (validationError == null) {
				try {
					//	fetch test document
					DocumentEditor documentEditor = parent.getActivePanel();
					if (documentEditor == null) return;
					QueriableAnnotation readOnlyData = documentEditor.getContent();
					MutableAnnotation data = Gamta.copyDocument(readOnlyData);
					
					//	execute script
					GScript.executeScript(data, script);
					
					//	show result
					DocumentDisplayDialog ddd = new DocumentDisplayDialog(parent, documentEditor, "Script finished", data, true);
					ddd.setVisible(true);
					
				}
				catch (GScriptException gse) {
					JOptionPane.showMessageDialog(this, ("The script produced an exception:\n" + gse.getMessage()), "Script Test", JOptionPane.ERROR_MESSAGE);
				}
				catch (GPathException gpe) {
					JOptionPane.showMessageDialog(this, ("The script produced an exception:\n" + gpe.getMessage()), "Script Test", JOptionPane.ERROR_MESSAGE);
				}
				catch (Exception e) {
					JOptionPane.showMessageDialog(this, ("An exception occurred while executing the script:\n" + e.getMessage()), "Script Test", JOptionPane.ERROR_MESSAGE);
				}
			}
			else this.showMessageDialog(("The script is not valid:\n" + validationError + (messages.isEmpty() ? "" : ("  \n" + messages.concatStrings("\n  ")))), "Script Validation", JOptionPane.ERROR_MESSAGE);
		}
		
		void debugScript() {
			
			//	get & validate script
			String script = this.editor.getText().trim();
			if (script.length() == 0) return;
			
			//	if script is valid, apply it to copy of current document
			StringVector messages = new StringVector();
			String validationError;
			try {
				validationError = GScript.validateScript(script, messages);
			}
			catch (GScriptException e) {
				validationError = e.getMessage();
			}
			if (validationError == null) {
				
				//	fetch test document
				DocumentEditor documentEditor = parent.getActivePanel();
				if (documentEditor == null) return;
				QueriableAnnotation readOnlyData = documentEditor.getContent();
				MutableAnnotation data = Gamta.copyDocument(readOnlyData);
				
				//	parse script
				StringVector lines = new StringVector();
				lines.parseAndAddElements(script, "\n");
				
				//	extract executable lines
				StringVector commands = new StringVector();
				for (int l = 0; l < lines.size(); l++) {
					String line = lines.get(l).trim();
					
					//	it's an executable line
					if ((line.length() != 0) && !line.startsWith(GScript.COMMENT_LINE_START)) commands.addElement(line);
				}
				
				//	build and show debug dialog
				DebugDialog dd = new DebugDialog(documentEditor, data, commands.toStringArray());
				dd.setVisible(true);
			}
			
			//	report syntax error
			else this.showMessageDialog(("The script is not valid:\n" + validationError + (messages.isEmpty() ? "" : ("  \n" + messages.concatStrings("\n  ")))), "Script Validation", JOptionPane.ERROR_MESSAGE);
		}
		
		private void showMessageDialog(String message, String title, int messageType) {
			String[] messageLines = message.split("\\n");
			JPanel messageDisplay = new JPanel(new GridLayout(messageLines.length, 1));
			for (int l = 0; l < messageLines.length; l++) {
				JLabel messageLine = new JLabel(messageLines[l]);
				messageLine.setBorder(BorderFactory.createLineBorder(messageDisplay.getBackground()));
				messageDisplay.add(messageLine);
			}
			JScrollPane messageBox = new JScrollPane(messageDisplay);
			messageBox.setBorder(BorderFactory.createEmptyBorder());
			messageBox.setViewportBorder(BorderFactory.createEmptyBorder());
			messageBox.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
			messageBox.getVerticalScrollBar().setUnitIncrement(50);
			messageBox.getVerticalScrollBar().setBlockIncrement(250);
			messageBox.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			messageBox.setPreferredSize(new Dimension(Math.min(messageDisplay.getPreferredSize().width, 600), Math.min(messageDisplay.getPreferredSize().height, 700)));
			JOptionPane.showMessageDialog(this, messageBox, title, messageType);
		}
		
		private class DebugDialog extends DialogPanel implements GScript.DebugLogger {
			
			private DocumentEditor editor;
			private MutableAnnotation data;
			
			private String[] commands;
			
			private StringVector variableNames = new StringVector();
			private Properties variableValues = new Properties();
			
			private JTable variableView = new JTable();
			private JTextArea scriptView = new JTextArea();
			private JTextArea outputView = new JTextArea();
			
			private JButton runButton = new JButton("Run Script");
			
			DebugDialog(DocumentEditor editor, MutableAnnotation data, String[] commands) {
				super("Debugging Gamta Script ...", true);
				
				this.editor = editor;
				this.data = data;
				
				this.commands = commands;
				
				this.scriptView.setEditable(false);
				JScrollPane scriptViewBox = new JScrollPane(this.scriptView);
				for (int c = 0; c < this.commands.length; c++)
					this.scriptView.append(this.commands[c] + "\n");
				
				JScrollPane variableViewBox = new JScrollPane(this.variableView);
				
				JSplitPane topSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scriptViewBox, variableViewBox);
				topSplit.setDividerLocation(0.5);
				topSplit.setResizeWeight(0.5);
				
				this.outputView.setEditable(false);
				JScrollPane outputViewBox = new JScrollPane(this.outputView);
				JSplitPane mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topSplit, outputViewBox);
				mainSplit.setDividerLocation(0.5);
				mainSplit.setResizeWeight(0.5);
				
				this.runButton.setBorder(BorderFactory.createRaisedBevelBorder());
				this.runButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						runScript();
					}
				});
				
				this.setLayout(new BorderLayout());
				this.add(mainSplit, BorderLayout.CENTER);
				this.add(runButton, BorderLayout.SOUTH);
				
				GScript.setOutputDestination(new PrintWriter(System.out) {
					public void println(String line) {
						output(line);
					}
				});
				
				this.setResizable(true);
				this.setSize(new Dimension(400, 600));
				this.setLocationRelativeTo(this.getOwner());
			}
			
			void runScript() {
				
				//	run script in own thread
				new Thread(new Runnable() {
					public void run() {
						try {
							
							//	execute script with DebugLogger
							GPathVariableResolver globalBindings = new GPathVariableResolver();
							String[] results = GScript.executeScript(data, commands, new GPathVariableResolver(globalBindings), DebugDialog.this);
							output("");
							output(" ----- Script finished ----- ");
							for (int r = 0; r < results.length; r++) output(results[r]);
							
							//	show result
							DocumentDisplayDialog ddd = new DocumentDisplayDialog(parent, editor, "Script finished", data, true);
							ddd.setVisible(true);
						}
						catch (GScriptException gse) {
							gse.printStackTrace(System.out);
							JOptionPane.showMessageDialog(DebugDialog.this, ("The script produced an exception:\n" + gse.getMessage()), "Script Debugging", JOptionPane.ERROR_MESSAGE);
						}
						catch (Exception e) {
							e.printStackTrace(System.out);
							JOptionPane.showMessageDialog(DebugDialog.this, ("Exception while executing script:\n" + e.getMessage()), "Script Debugging", JOptionPane.ERROR_MESSAGE);
						}
						finally {
							runButton.setEnabled(false);
						}
					}
				}).start();
			}
			
			/*
			 * @see de.uka.ipd.idaho.gamta.util.gScript.GScript.DebugLogger#expressionEvaluated(java.lang.String, java.lang.String)
			 */
			public void expressionEvaluated(String expression, String result) {
				this.output("expression '" + expression + "' evaluated to '" + result + "'");
			}
			
			/*
			 * @see de.uka.ipd.idaho.gamta.util.gScript.GScript.DebugLogger#variableAssigned(java.lang.String, java.lang.String)
			 */
			public void variableAssigned(String name, String value) {
				this.output("variable '" + name + "' set to '" + value + "'");
				this.setVariable(name, value);
			}
			
			/*
			 * @see de.uka.ipd.idaho.gamta.util.gScript.GScript.DebugLogger#forLoopVariableAssigned(java.lang.String, java.lang.String, java.lang.String)
			 */
			public void forLoopVariableAssigned(String name, String forLoopExpression, String value) {
				this.output("variable '" + name + "' in loop over '" + forLoopExpression + "' set to '" + value + "'");
				
				String oldValue = this.variableValues.getProperty(name);
				this.setVariable(name, value);
				
				//	end of loop run
				if (value == null) this.showDocument("Run over '" + forLoopExpression + "' for '" + name + "'='" + oldValue + "' finished");
			}
			
			/*
			 * @see de.uka.ipd.idaho.gamta.util.gScript.GScript.DebugLogger#functionInvoking(java.lang.String, java.lang.String[])
			 */
			public void functionInvoking(String name, String[] parameters) {
				this.output("Invoking function '" + name + "'");
				for (int p = 0; p < parameters.length; p++)
					this.output(" - " + parameters[p]);
			}
			
			/*
			 * @see de.uka.ipd.idaho.gamta.util.gScript.GScript.DebugLogger#functionInvoked(java.lang.String, java.lang.String[])
			 */
			public void functionInvoked(String name, String[] results) {
				this.output("function '" + name + "' returned:");
				for (int p = 0; p < results.length; p++)
					this.output(" - " + results[p]);
				
				this.showDocument("Function '" + name + "' finished.");
			}
			
			void setVariable(String name, String value) {
				if (value == null) {
					this.variableNames.remove(name);
					this.variableValues.remove(name);
				}
				else {
					this.variableNames.addElementIgnoreDuplicates(name);
					this.variableNames.sortLexicographically(false, false);
					this.variableValues.setProperty(name, value);
				}
				variableView.setModel(new VariableTableModel(this.variableNames, this.variableValues));
				variableView.validate();
			}
			
			private class VariableTableModel implements TableModel {
				private StringVector variableNames;
				private Properties variableValues;
				VariableTableModel(StringVector variableNames, Properties variableValues) {
					this.variableNames = variableNames;
					this.variableValues = variableValues;
				}
				public int getColumnCount() {
					return 2;
				}
				public Class getColumnClass(int columnIndex) {
					return String.class;
				}
				public String getColumnName(int columnIndex) {
					return ((columnIndex == 0) ? "Name" : "Value");
				}
				public int getRowCount() {
					return this.variableNames.size();
				}
				public boolean isCellEditable(int rowIndex, int columnIndex) {
					return false;
				}
				public Object getValueAt(int rowIndex, int columnIndex) {
					String name = this.variableNames.get(rowIndex);
					return ((columnIndex == 0) ? name : this.variableValues.getProperty(name));
				}
				public void setValueAt(Object aValue, int rowIndex, int columnIndex) {}
				public void addTableModelListener(TableModelListener l) {}
				public void removeTableModelListener(TableModelListener l) {}
			}
			
			//	show document TODO: highlight changes made in last step
			void showDocument(String title) {
				DocumentDisplayDialog ddd = new DocumentDisplayDialog(this.getDialog(), parent, this.editor, title, this.data, false);
				ddd.setVisible(true);
				if (ddd.stop) throw new RuntimeException("Script interrupted by user.");
			}
			
			//	print a line to the output view
			void output(String outLine) {
				this.outputView.append(outLine + "\n");
				this.outputView.setCaretPosition(outputView.getText().length());
			}
		}
		
		private class DocumentDisplayDialog extends DocumentEditorDialog {
			
			private boolean stop = false;
			
			DocumentDisplayDialog(JDialog owner, GoldenGATE host, DocumentEditor parent, String title, MutableAnnotation docPart, boolean isFinalDialog) {
				super(owner, host, parent, title, docPart);
				this.host = host;
				this.init(isFinalDialog);
			}
			
			DocumentDisplayDialog(GoldenGATE host, DocumentEditor parent, String title, MutableAnnotation docPart, boolean isFinalDialog) {
				super(host, parent, title, docPart);
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
					JButton abortButton = new JButton("Interrupt");
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
				this.stop = true;
				this.dispose();
			}
			
			void commit() {
				this.documentEditor.writeChanges();
				this.dispose();
			}
		}
		
		void refreshDisplay() {
			this.editor.getDocument().removeDocumentListener(this);
			this.editor.setFont(new Font(this.fontName, Font.PLAIN, this.fontSize));
			this.editor.setText(this.content);
			this.editor.getDocument().addDocumentListener(this);
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

class AnnotateFunction implements GScriptFunction {
	
	static final String ANNOTATE_FUNCTION_NAME = "annotate";
	
	GScriptManager parent;
	
	AnnotateFunction(GScriptManager parent) {
		this.parent = parent;
	}
	
	private static final String[] parameterNames = {
		"annotatorName",
		"annotationSet (optional)"
	};
	
	private static final String[] parameterTypes = {
		GScript.GPATH_STRING_TYPE,
		GScript.GPATH_ANNOTATION_SET_TYPE
	};
	
	private static final String[] parameterDescriptions = {
		"the name of the DocumentProcessor to run",
		"the annotations to process (optional, will process context document if not specified)"
	};
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.gScript.GScriptFunction#process(de.uka.ipd.idaho.gamta.MutableAnnotation, de.uka.ipd.idaho.gamta.util.gPath.types.GPathObject[], de.uka.ipd.idaho.gamta.util.gPath.GPathVariableResolver)
	 */
	public String process(MutableAnnotation data, GPathObject[] parameters, GPathVariableResolver variables) {
		if ((parameters.length < 1) || (parameters.length > 2)) {
			StringVector parameterDescriptions = new StringVector();
			parameterDescriptions.addContent(this.getParameterDescriptions());
			throw new IllegalArgumentException("'" + this.getName() + "' expects the following parameters:\n  - " + parameterDescriptions.concatStrings("\n  - "));
		}
		
		String processorName = (parameters[0].asString().value);
		
		DocumentProcessor dp = this.parent.getProcessor(processorName);
		if (dp == null) return ("Invalid processor name: " + processorName);
		
		if (parameters.length == 1) {
			dp.process(data, new Properties());
			return ("Document processed.");
		}
		else {
			GPathAnnotationSet gpas = ((GPathAnnotationSet) parameters[1]);
			for (int a = 0; a < gpas.size(); a++) {
				Annotation an = gpas.get(a);
				if (an instanceof MutableAnnotation)
					dp.process((MutableAnnotation) an);
				else {
					MutableAnnotation temp = data.addAnnotation(an);
					dp.process((MutableAnnotation) temp);
					data.removeAnnotation(temp);
				}
			}
			return (gpas.size() + " MutableAnnotations processed.");
		}
	}
	
	/*
	 * @see de.goldenGate.plugin.gScript.GScriptFunctionOld#getName()
	 */
	public String getName() {
		return ANNOTATE_FUNCTION_NAME;
	}
	
	/*
	 * @see de.goldenGate.plugin.gScript.GScriptFunctionOld#getParameterNames()
	 */
	public String[] getParameterNames() {
		return parameterNames;
	}
	
	/*
	 * @see de.goldenGate.plugin.gScript.GScriptFunction#getParameterTypes()
	 */
	public String[] getParameterTypes() {
		return parameterTypes;
	}
	
	/*
	 * @see de.goldenGate.plugin.gScript.GScriptFunctionOld#getParameterDescriptions()
	 */
	public String[] getParameterDescriptions() {
		return parameterDescriptions;
	}
}

class RunFunction implements GScriptFunction {
	
	static final String RUN_FUNCTION_NAME = "run";
	
	GScriptManager parent;
	
	RunFunction(GScriptManager parent) {
		this.parent = parent;
	}
	
	private static final String[] parameterNames = {
		"processorName",
		"annotationSet (optional)"
	};
	
	private static final String[] parameterTypes = {
		GScript.GPATH_STRING_TYPE,
		GScript.GPATH_ANNOTATION_SET_TYPE
	};
	
	private static final String[] parameterDescriptions = {
		"the name of the DocumentProcessor to run",
		"the annotations to process (optional, will process context document if not specified)"
	};
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.gScript.GScriptFunction#process(de.uka.ipd.idaho.gamta.MutableAnnotation, de.uka.ipd.idaho.gamta.util.gPath.types.GPathObject[], de.uka.ipd.idaho.gamta.util.gPath.GPathVariableResolver)
	 */
	public String process(MutableAnnotation data, GPathObject[] parameters, GPathVariableResolver variables) {
		if ((parameters.length < 1) || (parameters.length > 2)) {
			StringVector parameterDescriptions = new StringVector();
			parameterDescriptions.addContent(this.getParameterDescriptions());
			throw new IllegalArgumentException("'" + this.getName() + "' expects the following parameters:\n  - " + parameterDescriptions.concatStrings("\n  - "));
		}
		
		String processorName = (parameters[0].asString().value);
		
		DocumentProcessor dp = this.parent.getProcessor(processorName);
		if (dp == null) return ("Invalid processor name: " + processorName);
		
		if (parameters.length == 1) {
			dp.process(data, new Properties());
			return ("Document processed.");
		}
		else {
			GPathAnnotationSet gpas = ((GPathAnnotationSet) parameters[1]);
			for (int a = 0; a < gpas.size(); a++) {
				Annotation an = gpas.get(a);
				if (an instanceof MutableAnnotation)
					dp.process((MutableAnnotation) an);
				else {
					MutableAnnotation temp = data.addAnnotation(an);
					dp.process((MutableAnnotation) temp);
					data.removeAnnotation(temp);
				}
			}
			return (gpas.size() + " MutableAnnotations processed.");
		}
	}
	
	/*
	 * @see de.goldenGate.plugin.gScript.GScriptFunctionOld#getName()
	 */
	public String getName() {
		return RUN_FUNCTION_NAME;
	}
	
	/*
	 * @see de.goldenGate.plugin.gScript.GScriptFunctionOld#getParameterNames()
	 */
	public String[] getParameterNames() {
		return parameterNames;
	}
	
	/*
	 * @see de.goldenGate.plugin.gScript.GScriptFunction#getParameterTypes()
	 */
	public String[] getParameterTypes() {
		return parameterTypes;
	}
	
	/*
	 * @see de.goldenGate.plugin.gScript.GScriptFunctionOld#getParameterDescriptions()
	 */
	public String[] getParameterDescriptions() {
		return parameterDescriptions;
	}
}

class ListFunctionsFunction implements GScriptFunction {
	
	ListFunctionsFunction() {}
	
	private static final String[] parameterNames = {};
	
	private static final String[] parameterTypes = {};
	
	private static final String[] parameterDescriptions = {};
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.gScript.GScriptFunction#process(de.uka.ipd.idaho.gamta.MutableAnnotation, de.uka.ipd.idaho.gamta.util.gPath.types.GPathObject[], de.uka.ipd.idaho.gamta.util.gPath.GPathVariableResolver)
	 */
	public String process(MutableAnnotation data, GPathObject[] parameters, GPathVariableResolver variables) {
		if (parameters.length != 0) {
			StringVector parameterDescriptions = new StringVector();
			parameterDescriptions.addContent(this.getParameterDescriptions());
			throw new IllegalArgumentException("'" + this.getName() + "' expects no parameters.");
		}
		
		String[][] functionDescriptions = GScript.getFunctionDescriptions();
		for (int f = 0; f < functionDescriptions.length; f++) {
			for (int d = 0; d < functionDescriptions[f].length; d++)
				GScript.output(functionDescriptions[f][d]);
		}
		
		return ("Got " + functionDescriptions.length + " functions.");
	}
	
	/*
	 * @see de.goldenGate.plugin.gScript.GScriptFunctionOld#getName()
	 */
	public String getName() {
		return "listF";
	}
	
	/*
	 * @see de.goldenGate.plugin.gScript.GScriptFunctionOld#getParameterNames()
	 */
	public String[] getParameterNames() {
		return parameterNames;
	}
	
	/*
	 * @see de.goldenGate.plugin.gScript.GScriptFunction#getParameterTypes()
	 */
	public String[] getParameterTypes() {
		return parameterTypes;
	}
	
	/*
	 * @see de.goldenGate.plugin.gScript.GScriptFunctionOld#getParameterDescriptions()
	 */
	public String[] getParameterDescriptions() {
		return parameterDescriptions;
	}
}

class ListAnnotationSourcesFunction implements GScriptFunction {
	
	GoldenGATE host;
	
	ListAnnotationSourcesFunction(GoldenGATE host) {
		this.host = host;
	}
	
	private static final String[] parameterNames = {};
	
	private static final String[] parameterTypes = {};
	
	private static final String[] parameterDescriptions = {};
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.gScript.GScriptFunction#process(de.uka.ipd.idaho.gamta.MutableAnnotation, de.uka.ipd.idaho.gamta.util.gPath.types.GPathObject[], de.uka.ipd.idaho.gamta.util.gPath.GPathVariableResolver)
	 */
	public String process(MutableAnnotation data, GPathObject[] parameters, GPathVariableResolver variables) {
		if (parameters.length != 0) {
			StringVector parameterDescriptions = new StringVector();
			parameterDescriptions.addContent(this.getParameterDescriptions());
			throw new IllegalArgumentException("'" + this.getName() + "' expects no parameters.");
		}
		
		AnnotationSourceManager[] asm = this.host.getAnnotationSourceProviders();
		int dpCount = 0;
		for (int m = 0; m < asm.length; m++) {
			String[] dpNames = asm[m].getResourceNames();
			GScript.output("'" + asm[m].getResourceTypeLabel() + "' type AnnotationSource (" + dpNames.length + "):");
			dpCount += dpNames.length;
			for (int d = 0; d < dpNames.length; d++)
				GScript.output(" - " + dpNames[d]);
		}
		return ("Got " + dpCount + " AnnotationSource from " + asm.length + " Managers.");
	}
	
	/*
	 * @see de.goldenGate.plugin.gScript.GScriptFunctionOld#getName()
	 */
	public String getName() {
		return "listAS";
	}
	
	/*
	 * @see de.goldenGate.plugin.gScript.GScriptFunctionOld#getParameterNames()
	 */
	public String[] getParameterNames() {
		return parameterNames;
	}
	
	/*
	 * @see de.goldenGate.plugin.gScript.GScriptFunction#getParameterTypes()
	 */
	public String[] getParameterTypes() {
		return parameterTypes;
	}
	
	/*
	 * @see de.goldenGate.plugin.gScript.GScriptFunctionOld#getParameterDescriptions()
	 */
	public String[] getParameterDescriptions() {
		return parameterDescriptions;
	}
}

class ListDocumentProcessorsFunction implements GScriptFunction {
	
	GoldenGATE host;
	
	ListDocumentProcessorsFunction(GoldenGATE host) {
		this.host = host;
	}
	
	private static final String[] parameterNames = {};
	
	private static final String[] parameterTypes = {};
	
	private static final String[] parameterDescriptions = {};
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.gScript.GScriptFunction#process(de.uka.ipd.idaho.gamta.MutableAnnotation, de.uka.ipd.idaho.gamta.util.gPath.types.GPathObject[], de.uka.ipd.idaho.gamta.util.gPath.GPathVariableResolver)
	 */
	public String process(MutableAnnotation data, GPathObject[] parameters, GPathVariableResolver variables) {
		if (parameters.length != 0) {
			StringVector parameterDescriptions = new StringVector();
			parameterDescriptions.addContent(this.getParameterDescriptions());
			throw new IllegalArgumentException("'" + this.getName() + "' expects no parameters.");
		}
		
		DocumentProcessorManager[] dpm = this.host.getDocumentProcessorProviders();
		int dpCount = 0;
		for (int m = 0; m < dpm.length; m++) {
			String[] dpNames = dpm[m].getResourceNames();
			GScript.output("'" + dpm[m].getResourceTypeLabel() + "' type DocumentProcessors (" + dpNames.length + "):");
			dpCount += dpNames.length;
			for (int d = 0; d < dpNames.length; d++)
				GScript.output(" - " + dpNames[d]);
		}
		return ("Got " + dpCount + " DocumentProcessors from " + dpm.length + " Managers.");
	}
	
	/*
	 * @see de.goldenGate.plugin.gScript.GScriptFunctionOld#getName()
	 */
	public String getName() {
		return "listDP";
	}
	
	/*
	 * @see de.goldenGate.plugin.gScript.GScriptFunctionOld#getParameterNames()
	 */
	public String[] getParameterNames() {
		return parameterNames;
	}
	
	/*
	 * @see de.goldenGate.plugin.gScript.GScriptFunction#getParameterTypes()
	 */
	public String[] getParameterTypes() {
		return parameterTypes;
	}
	
	/*
	 * @see de.goldenGate.plugin.gScript.GScriptFunctionOld#getParameterDescriptions()
	 */
	public String[] getParameterDescriptions() {
		return parameterDescriptions;
	}
}

class EditDocumentFunction implements GScriptFunction {
	
	GoldenGATE host;
	
	EditDocumentFunction(GoldenGATE host) {
		this.host = host;
	}
	
	private static final String[] parameterNames = {};
	
	private static final String[] parameterTypes = {};
	
	private static final String[] parameterDescriptions = {};
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.gScript.GScriptFunction#process(de.uka.ipd.idaho.gamta.MutableAnnotation, de.uka.ipd.idaho.gamta.util.gPath.types.GPathObject[], de.uka.ipd.idaho.gamta.util.gPath.GPathVariableResolver)
	 */
	public String process(MutableAnnotation data, GPathObject[] parameters, GPathVariableResolver variables) {
		if (parameters.length != 0) {
			StringVector parameterDescriptions = new StringVector();
			parameterDescriptions.addContent(this.getParameterDescriptions());
			throw new IllegalArgumentException("'" + this.getName() + "' expects no parameters.");
		}
		
		DocumentEditor parent = this.host.getActivePanel();
		DocumentEditDialog ded = new DocumentEditDialog(this.host, parent, ((parent == null) ? "Edit Document" : ("Edit " + parent.getContentName())), data);
		ded.setVisible(true);
		if (ded.stop) throw new RuntimeException("GScript execution interrupted by user.");
		
		return ("Document edited.");
	}
	
	/*
	 * @see de.goldenGate.plugin.gScript.GScriptFunctionOld#getName()
	 */
	public String getName() {
		return "editDoc";
	}
	
	/*
	 * @see de.goldenGate.plugin.gScript.GScriptFunctionOld#getParameterNames()
	 */
	public String[] getParameterNames() {
		return parameterNames;
	}
	
	/*
	 * @see de.goldenGate.plugin.gScript.GScriptFunction#getParameterTypes()
	 */
	public String[] getParameterTypes() {
		return parameterTypes;
	}
	
	/*
	 * @see de.goldenGate.plugin.gScript.GScriptFunctionOld#getParameterDescriptions()
	 */
	public String[] getParameterDescriptions() {
		return parameterDescriptions;
	}
	
	private class DocumentEditDialog extends DocumentEditorDialog {
		
		boolean stop = false;
		
		DocumentEditDialog(GoldenGATE host, DocumentEditor parent, String title, MutableAnnotation docPart) {
			super(host, parent, title, docPart);
			this.host = host;
			
			//	initialize main buttons
			JButton commitButton = new JButton("Proceed");
			commitButton.setBorder(BorderFactory.createRaisedBevelBorder());
			commitButton.setPreferredSize(new Dimension(100, 21));
			commitButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					commit();
				}
			});
			this.mainButtonPanel.add(commitButton);
			
			JButton abortButton = new JButton("Stop");
			abortButton.setBorder(BorderFactory.createRaisedBevelBorder());
			abortButton.setPreferredSize(new Dimension(100, 21));
			abortButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					abort();
				}
			});
			this.mainButtonPanel.add(abortButton);
			
			this.setResizable(true);
			this.setSize(new Dimension(800, 600));
			this.setLocationRelativeTo(DialogPanel.getTopWindow());
		}
		
		void abort() {
			if (JOptionPane.showConfirmDialog(this, "Really interrupt Script?", "Confirm Interrupt Script", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
				this.stop = true;
				this.dispose();
			}
		}
		
		void commit() {
			this.documentEditor.writeChanges();
			this.dispose();
		}
	}
}
