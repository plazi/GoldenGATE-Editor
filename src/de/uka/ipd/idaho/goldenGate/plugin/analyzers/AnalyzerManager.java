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
package de.uka.ipd.idaho.goldenGate.plugin.analyzers;


import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import de.uka.ipd.idaho.easyIO.settings.Settings;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.util.AbstractAnalyzerDataProvider;
import de.uka.ipd.idaho.gamta.util.Analyzer;
import de.uka.ipd.idaho.gamta.util.GamtaClassLoader;
import de.uka.ipd.idaho.gamta.util.MonitorableAnalyzer;
import de.uka.ipd.idaho.gamta.util.ProgressMonitor;
import de.uka.ipd.idaho.gamta.util.analyzers.LineEndMarker;
import de.uka.ipd.idaho.gamta.util.analyzers.ParagraphStructureNormalizer;
import de.uka.ipd.idaho.gamta.util.analyzers.ParagraphTagger;
import de.uka.ipd.idaho.gamta.util.analyzers.SectionTagger;
import de.uka.ipd.idaho.gamta.util.analyzers.SentenceTagger;
import de.uka.ipd.idaho.gamta.util.analyzers.WhitespaceNormalizer;
import de.uka.ipd.idaho.goldenGate.DocumentEditor;
import de.uka.ipd.idaho.goldenGate.GoldenGateConstants;
import de.uka.ipd.idaho.goldenGate.plugins.AbstractDocumentProcessorManager;
import de.uka.ipd.idaho.goldenGate.plugins.DocumentProcessor;
import de.uka.ipd.idaho.goldenGate.plugins.MonitorableDocumentProcessor;
import de.uka.ipd.idaho.goldenGate.plugins.ResourceSplashScreen;
import de.uka.ipd.idaho.goldenGate.util.DataListListener;
import de.uka.ipd.idaho.goldenGate.util.DialogPanel;
import de.uka.ipd.idaho.stringUtils.StringUtils;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * Document processor manager for wrapping GAMTA Analyzers in
 * DocumentProcessors. The intention is to make NLP logics implemented in
 * Analyzers available in the GoldenGATE Editor<br>
 * <br>
 * For deploying an Analyzer (or a group of Analyzers that belong together), do
 * the following:
 * <ul>
 * <li>Export the Analyzer(s) to a jar file and deposit this jar file in the
 * AnalyzerManager's data path. Let's now assume the Analyzer(s) reside in
 * 'MyAnalyzers.jar'.</li>
 * <li>Then, deposit all data required by the Analyzer(s) in a sub folder to
 * the AnalyzerManager's data path, named just as the jar containing the
 * Analyzer(s), with the '.jar' suffix replaced by 'Data'. In our example, this
 * would be the folder 'MyAnalyzersData'.</li>
 * <li>If the Analyzer(s) require any third party jar files on the class path
 * (beside those containing the StringUtils, HtmlXmlUtil, GAMTA, and EasyIO
 * classes, which are natively included in GoldenGATE's class path), deposit
 * these jar files in a sub folder to the AnalyzerManager's data path, named
 * just as the jar containing the Analyzer(s), with the '.jar' suffix replaced
 * by 'Bin'. In our example, this would be 'MyAnalyzersBin'.</li>
 * <li>Finally, open the GoldenGATE Editor (with a Master Configuration), go to
 * the 'Edit Analyzers' dialog, and click the 'Search Analyzers' button. The
 * AnalyzerManager should automatically find the newly deployed Analyzer(s) and
 * create the respective descriptor files. These files are named after the last
 * part of the Analyzers' class names, prefixed with the upper case letters from
 * the jar file's name. Let's assume we have an Analyzer named
 * 'com.myUrl.myPackage.MyFirstAnalyzer' deployed inside 'MyAnalyzers.jar'.
 * Then, the AnalyzerManager creates a descriptor file named
 * 'MA.MyFirstAnalyzer'. The prefix generated from the jar name is intended to
 * keep Analyzers together that originate from the same jar file and therefore
 * can be assumed to have some sort of semantic relation.</li>
 * </ul>
 * For deploying third party components as Anaylzers, e.g. NLP components
 * available from the web, you have two options:
 * <ul>
 * <li>Create a specialized wrapper for each of the third party components and
 * deploy these wrappers as individual Analyzers, as described above.</li>
 * <li>Create a generic wrapper factory for the common super class of the third
 * party components, implementing the AnalyzerFactory interface. Deploy the
 * factory to a jar file, depositing data and third party jar files just as
 * described above, and click the 'Search Types' button in the 'Edit Analyzers'
 * dialog. Afterward, click the 'Search Analyzers' button. The AnalyzerManager
 * should now find the respective components and have the newly fond
 * AnalyzerFactory wrap them in Analyzers.</li>
 * </ul>
 * 
 * @see de.uka.ipd.idaho.gamta.util.Analyzer
 * @see de.uka.ipd.idaho.gamta.util.AbstractAnalyzer
 * @see de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer
 * 
 * @author sautter
 */
public class AnalyzerManager extends AbstractDocumentProcessorManager {
	
	private static final String FILE_EXTENSION = ".analyzer";
	
	private static final String PARAGRAPH_ANNOTATOR_NAME = "<Paragraph Tagger>";
	private static final String PARAGRAPH_STRUCTURE_NORMALIZER_NAME = "<Paragraph Structure Normalizer>";
	private static final String LINE_END_MARKER_NAME = "<Line End Marker>";
	private static final String WHITESPACE_NORMALIZER_NAME = "<Whitespace Normalizer>";
	private static final String SENTENCE_ANNOTATOR_NAME = "<Sentence Tagger>";
	private static final String SECTION_ANNOTATOR_NAME = "<Section Tagger>";
	
	private static final String[] FIX_ANALYZER_NAMES = {PARAGRAPH_ANNOTATOR_NAME, PARAGRAPH_STRUCTURE_NORMALIZER_NAME, WHITESPACE_NORMALIZER_NAME, LINE_END_MARKER_NAME, SENTENCE_ANNOTATOR_NAME, SECTION_ANNOTATOR_NAME};
	private static final Analyzer[] FIX_ANALYZERS = {new ParagraphTagger(), new ParagraphStructureNormalizer(), new WhitespaceNormalizer(), new LineEndMarker(), new SentenceTagger(), new SectionTagger()};
	
	private HashMap analyzers = new HashMap();
	private AnalyzerClassLoader analyzerLoader;
	private boolean analyzerDataWritable = true;
	
	public AnalyzerManager() {}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#init()
	 */
	public void init() {
		try {
			this.analyzerLoader = new AnalyzerClassLoader();
		}
		catch (SecurityException se) {
			System.out.println("AnalyzerManager: could not create class loader - " + se.getMessage());
			se.printStackTrace(System.out);
		}
	}
	
	private class AnalyzerDocumentProcessor implements DocumentProcessor {
		static final String CLASS_ATTRIBUTE_NAME = "CLASS";
		static final String CLASS_PATH_ATTRIBUTE_NAME = "CLASS_PATH";
		static final String LIB_PATH_ATTRIBUTE_NAME = "LIB_PATH";
		static final String DATA_PATH_ATTRIBUTE_NAME = "DATA_PATH";
		
		final String name;
		final Analyzer analyzer;
		
		AnalyzerDocumentProcessor(String name, Analyzer analyzer) {
			this.name = name;
			this.analyzer = analyzer;
		}
		public String getName() {
			return this.name;
		}
		public String getTypeLabel() {
			return "Analyzer";
		}
		public String getProviderClassName() {
			return AnalyzerManager.class.getName();
		}
		public void process(MutableAnnotation data) {
			Properties parameters = new Properties();
			parameters.setProperty(INTERACTIVE_PARAMETER, INTERACTIVE_PARAMETER);
			this.process(data, parameters);
		}
		public void process(MutableAnnotation data, Properties parameters) {
			Properties prop = ((parameters == null) ? new Properties() : new Properties(parameters));
			if (dataProvider.allowWebAccess())
				prop.setProperty(Analyzer.ONLINE_PARAMETER, Analyzer.ONLINE_PARAMETER);
			if ((parameters != null) && parameters.containsKey(INTERACTIVE_PARAMETER))
				prop.setProperty(Analyzer.INTERACTIVE_PARAMETER, Analyzer.INTERACTIVE_PARAMETER);
			this.analyzer.process(data, prop);
		}
	}
	
	private class MonitorableAnalyzerDocumentProcessor extends AnalyzerDocumentProcessor implements MonitorableDocumentProcessor {
		MonitorableAnalyzerDocumentProcessor(String name, MonitorableAnalyzer analyzer) {
			super(name, analyzer);
		}
		public void process(MutableAnnotation data) {
			Properties parameters = new Properties();
			parameters.setProperty(INTERACTIVE_PARAMETER, INTERACTIVE_PARAMETER);
			this.process(data, parameters);
		}
		public void process(MutableAnnotation data, Properties parameters) {
			this.process(data, parameters, ProgressMonitor.dummy);
		}
		public void process(MutableAnnotation data, Properties parameters, ProgressMonitor pm) {
			Properties prop = ((parameters == null) ? new Properties() : new Properties(parameters));
			if (dataProvider.allowWebAccess())
				prop.setProperty(Analyzer.ONLINE_PARAMETER, Analyzer.ONLINE_PARAMETER);
			if ((parameters != null) && parameters.containsKey(INTERACTIVE_PARAMETER))
				prop.setProperty(Analyzer.INTERACTIVE_PARAMETER, Analyzer.INTERACTIVE_PARAMETER);
			((MonitorableAnalyzer) this.analyzer).process(data, prop, pm);
		}
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractDocumentProcessorManager#exit()
	 */
	public void exit() {
		String[] analyzerNames = this.getResourceNames();
		for (int a = 0; a < analyzerNames.length; a++)
			if (this.analyzers.containsKey(analyzerNames[a])) try {
				((Analyzer) this.analyzers.get(analyzerNames[a])).exit();
			} catch (Throwable t) {
				System.out.println(t.getClass().getName() + " (" + t.getMessage() + ") while exiting Analyzer '" + analyzerNames[a] + "':");
				t.printStackTrace(System.out);
			}
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractDocumentProcessorManager#getDocumentProcessor(java.lang.String)
	 */
	public DocumentProcessor getDocumentProcessor(String name) {
		Analyzer analyzer = this.getAnalyzer(name);
		if (analyzer == null)
			return null;
		if (analyzer instanceof MonitorableAnalyzer)
			return new MonitorableAnalyzerDocumentProcessor(name, ((MonitorableAnalyzer) analyzer));
		return new AnalyzerDocumentProcessor(name, analyzer);
	}
	
	/* 
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentProcessorManager#createDocumentProcessor()
	 */
	public String createDocumentProcessor() {
		return this.createAnalyzer(new Settings(), null);
	}
	
	/* 
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentProcessorManager#editDocumentProcessor(java.lang.String)
	 */
	public void editDocumentProcessor(String name) {
		Settings set = this.loadSettingsResource(name);
		if ((set == null) || set.isEmpty()) return;
		
		EditAnalyzerDialog ead = new EditAnalyzerDialog(set, name);
		ead.setVisible(true);
		
		if (ead.isCommitted()) try {
			this.storeSettingsResource(name, ead.getAnalyzer());
		} catch (IOException ioe) {}
	}
	
	/* 
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentProcessorManager#editDocumentProcessors()
	 */
	public void editDocumentProcessors() {
		this.editAnalyzers();
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager#getDataNamesForResource(java.lang.String)
	 */
	public String[] getDataNamesForResource(String name) {
		if (name.startsWith("<")) return new String[0]; // catch built-in resources
		
		StringVector names = new StringVector();
		names.addContentIgnoreDuplicates(super.getDataNamesForResource(name));
		
		Settings settings = this.loadSettingsResource(name);
		if ((settings == null) || settings.isEmpty()) return names.toStringArray();
		
		String analyzerClassPath = settings.getSetting(AnalyzerDocumentProcessor.CLASS_PATH_ATTRIBUTE_NAME);
		names.addElementIgnoreDuplicates(analyzerClassPath + "@" + this.getClass().getName());
		
		String analyzerDataPrefix = analyzerClassPath.substring(0, (analyzerClassPath.length() - 4));
		names.addElementIgnoreDuplicates(analyzerDataPrefix + JAR_BIN_FOLDER_SUFFIX + "/" + "@" + this.getClass().getName());
		
		String analyzerDataPath = settings.getSetting(AnalyzerDocumentProcessor.DATA_PATH_ATTRIBUTE_NAME, (analyzerDataPrefix + JAR_DATA_FOLDER_SUFFIX));
		names.addElementIgnoreDuplicates(analyzerDataPath + (analyzerDataPath.endsWith("/") ? "" : "/") + "@" + this.getClass().getName());
		
		names.addElementIgnoreDuplicates("Bin/@" + this.getClass().getName());
		
		return names.toStringArray();
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractDocumentProcessorManager#getResourceNames()
	 */
	public String[] getResourceNames() {
		StringVector analyzers = new StringVector();
		analyzers.addContentIgnoreDuplicates(FIX_ANALYZER_NAMES);
		analyzers.addContentIgnoreDuplicates(super.getResourceNames());
		return analyzers.toStringArray();
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.ResourceManager#getResourceTypeLabel()
	 */
	public String getResourceTypeLabel() {
		return "Analyzer";
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
		
		mi = new JMenuItem("Annotate Sentences");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				applyDocumentProcessor(SENTENCE_ANNOTATOR_NAME);
			}
		});
		collector.add(mi);
		mi = new JMenuItem("Annotate Paragraphs");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				applyDocumentProcessor(PARAGRAPH_ANNOTATOR_NAME);
			}
		});
		collector.add(mi);
		mi = new JMenuItem("Annotate Sections");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				applyDocumentProcessor(SECTION_ANNOTATOR_NAME);
			}
		});
		collector.add(mi);
		
		collector.add(GoldenGateConstants.MENU_SEPARATOR_ITEM);
		
		mi = new JMenuItem("Normalize Paragraphs");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				applyDocumentProcessor(PARAGRAPH_STRUCTURE_NORMALIZER_NAME);
			}
		});
		collector.add(mi);
		mi = new JMenuItem("Normalize Whitespace");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				applyDocumentProcessor(WHITESPACE_NORMALIZER_NAME);
			}
		});
		collector.add(mi);
		mi = new JMenuItem("Mark Line Ends");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				applyDocumentProcessor(LINE_END_MARKER_NAME);
			}
		});
		collector.add(mi);
		
		if (this.dataProvider.isDataEditable()) {
			collector.add(GoldenGateConstants.MENU_SEPARATOR_ITEM);
			
			mi = new JMenuItem("Create");
			mi.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					createAnalyzer();
				}
			});
			collector.add(mi);
			mi = new JMenuItem("Edit");
			mi.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					editAnalyzers();
				}
			});
			collector.add(mi);
			mi = new JMenuItem("Unload (save)");
			mi.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					Thread unloader = new Thread() {
						public void run() {
							ResourceSplashScreen rss = new ResourceSplashScreen("Analyzer Manager: Unloading Analyzers ...", "Please wait while Analyzer Manager is unloading Analyzers ...");
							rss.popUp();
							while (!rss.isVisible()) try {
								sleep(25);
							} catch (InterruptedException ie) {}
							unloadAnalyzers(rss, true);
							rss.dispose();
						}
					};
					unloader.start();
				}
			});
			collector.add(mi);
			mi = new JMenuItem("Unload (no save)");
			mi.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					Thread unloader = new Thread() {
						public void run() {
							ResourceSplashScreen rss = new ResourceSplashScreen("Analyzer Manager: Unloading Analyzers ...", "Please wait while Analyzer Manager is unloading Analyzers ...");
							rss.popUp();
							while (!rss.isVisible()) try {
								sleep(25);
							} catch (InterruptedException ie) {}
							unloadAnalyzers(rss, false);
							rss.dispose();
						}
					};
					unloader.start();
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
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#getMainMenuTitle()
	 */
	public String getMainMenuTitle() {
		return "Analyzers";
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager#getToolsMenuLabel()
	 */
	public String getToolsMenuLabel() {
		return "Run";
	}
	
	/* retrieve an Analyzer by its name
	 * @param	name	the name of the reqired Analyzer
	 * @return the Analyzer with the required name, or null, if there is no such Analyzer
	 */
	private Analyzer getAnalyzer(String name) {
		if (name == null) return null;
		for (int a = 0; a < FIX_ANALYZER_NAMES.length; a++) 
			if (name.equals(FIX_ANALYZER_NAMES[a])) return FIX_ANALYZERS[a];
		return this.getAnalyzer(name, this.loadSettingsResource(name));
	}
	
	private Analyzer getAnalyzer(String name, Settings settings) {
		try {
			if (this.analyzers.containsKey(name)) {
				return ((Analyzer) this.analyzers.get(name));
			} else {
				if (settings == null)
					return null;
				Analyzer analyzer = this.produceAnalyzer(name, settings);
				if (analyzer != null)
					this.analyzers.put(name, analyzer);
				return analyzer;
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	private boolean createAnalyzer() {
		return (this.createAnalyzer(new Settings(), null) != null);
	}
	
	private boolean cloneAnalyzer() {
		String selectedName = this.resourceNameList.getSelectedName();
		if (selectedName == null)
			return this.createAnalyzer();
		else {
			String name = "New " + selectedName;
			return (this.createAnalyzer(this.loadSettingsResource(selectedName), name) != null);
		}
	}
	
	private String createAnalyzer(Settings modelAnalyzer, String name) {
		CreateAnalyzerDialog cad = new CreateAnalyzerDialog(modelAnalyzer, name);
		cad.setVisible(true);
		
		if (cad.isCommitted()) {
			Settings analyzer = cad.getAnalyzer();
			String analyzerName = cad.getAnalyzerName();
			if (!analyzerName.endsWith(FILE_EXTENSION)) analyzerName += FILE_EXTENSION;
			try {
				if (this.storeSettingsResource(analyzerName, analyzer)) {
					this.resourceNameList.refresh();
					return analyzerName;
				}
			} catch (IOException e) {}
		}
		return null;
	}
	
	private void editAnalyzers() {
		final AnalyzerEditorPanel[] editor = new AnalyzerEditorPanel[1];
		editor[0] = null;
		
		final DialogPanel editDialog = new DialogPanel("Edit Analyzers", true);
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
						storeSettingsResource(editor[0].analyzerName, editor[0].getSettings());
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
				createAnalyzer();
			}
		});
		editButtons.add(button);
		button = new JButton("Clone");
		button.setBorder(BorderFactory.createRaisedBevelBorder());
		button.setPreferredSize(new Dimension(100, 21));
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				cloneAnalyzer();
			}
		});
		editButtons.add(button);
		button = new JButton("Search Analyzers");
		button.setBorder(BorderFactory.createRaisedBevelBorder());
		button.setPreferredSize(new Dimension(100, 21));
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				Thread searcher = new Thread() {
					public void run() {
						ResourceSplashScreen rss = new ResourceSplashScreen("Analyzer Manager: Searching Analyzers ...", "Please wait while Analyzer Manager is searching for new Analyzers ...");
						rss.popUp();
						while (!rss.isVisible()) try {
							sleep(25);
						} catch (InterruptedException ie) {}
						if (searchAnalyzers(rss))
							resourceNameList.refresh();
						rss.dispose();
					}
				};
				searcher.start();
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
		button = new JButton("Unload All (save)");
		button.setBorder(BorderFactory.createRaisedBevelBorder());
		button.setPreferredSize(new Dimension(100, 21));
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				Thread unloader = new Thread() {
					public void run() {
						ResourceSplashScreen rss = new ResourceSplashScreen("Analyzer Manager: Unloading Analyzers ...", "Please wait while Analyzer Manager is unloading Analyzers ...");
						rss.popUp();
						while (!rss.isVisible()) try {
							sleep(25);
						} catch (InterruptedException ie) {}
						unloadAnalyzers(rss, true);
						rss.dispose();
					}
				};
				unloader.start();
			}
		});
		editButtons.add(button);
		button = new JButton("Unload All (no save)");
		button.setBorder(BorderFactory.createRaisedBevelBorder());
		button.setPreferredSize(new Dimension(100, 21));
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				Thread unloader = new Thread() {
					public void run() {
						ResourceSplashScreen rss = new ResourceSplashScreen("Analyzer Manager: Unloading Analyzers ...", "Please wait while Analyzer Manager is unloading Analyzers ...");
						rss.popUp();
						while (!rss.isVisible()) try {
							sleep(25);
						} catch (InterruptedException ie) {}
						unloadAnalyzers(rss, false);
						rss.dispose();
					}
				};
				unloader.start();
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
				editor[0] = new AnalyzerEditorPanel(selectedName, set);
				editorPanel.add(editor[0], BorderLayout.CENTER);
			}
		}
		editDialog.add(editorPanel, BorderLayout.CENTER);
		
		editDialog.add(this.resourceNameList, BorderLayout.EAST);
		DataListListener dll = new DataListListener() {
			public void selected(String dataName) {
				if ((editor[0] != null) && editor[0].isDirty()) {
					try {
						storeSettingsResource(editor[0].analyzerName, editor[0].getSettings());
					}
					catch (IOException ioe) {
						if (JOptionPane.showConfirmDialog(editDialog, (ioe.getClass().getName() + " (" + ioe.getMessage() + ")\nwhile saving file to " + editor[0].analyzerName + "\nProceed?"), "Could Not Save Analyzer", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
							resourceNameList.setSelectedName(editor[0].analyzerName);
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
						editor[0] = new AnalyzerEditorPanel(dataName, set);
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
	
	private void configureAnalyzer(String name) {
		final Analyzer analyzer = this.getAnalyzer(name);
		if (analyzer != null) SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				analyzer.configureProcessor();
			}
		});
	}
	
	private void unloadAnalyzers(ResourceSplashScreen rss, boolean allowSave) {
		String[] analyzerNames = this.getResourceNames();
		
		//	disable writing data to disc if required
		this.analyzerDataWritable = allowSave;
		
		//	exit analyzer instances
		rss.setStep("Exitting Analyzers ...");
		for (int a = 0; a < analyzerNames.length; a++) {
			if (this.analyzers.containsKey(analyzerNames[a])) try {
				rss.setInfo(" - " + analyzerNames[a]);
				((Analyzer) this.analyzers.get(analyzerNames[a])).exit();
			}
			catch (Throwable t) {
				System.out.println(t.getClass().getName() + " (" + t.getMessage() + ") while exiting Analyzer '" + analyzerNames[a] + "':");
				t.printStackTrace(System.out);
			}
			rss.setProgress((100 * (a+1)) / analyzerNames.length);
		}
		rss.setStep("Analyzers exitted");
		
		//	clear class loader cache
		this.analyzers.clear();
		try {
			this.analyzerLoader = new AnalyzerClassLoader();
		} catch (SecurityException se) {}
		rss.setStep("Analyzer class loader cleared");
		
		//	re-enable writing (at least from our point of view)
		this.analyzerDataWritable = true;
	}
	
	private boolean searchAnalyzers(ResourceSplashScreen rss) {
		
		String[] jarNames = this.getJarNames("");
		rss.setText("Got " + jarNames.length + " JARs to search");
		
		StringVector existingNames = new StringVector();
		existingNames.addContent(this.getResourceNames());
		boolean foundNew = false;
		
		for (int j = 0; j < jarNames.length; j++) {
			StringVector jarClassNames = new StringVector();
			Properties jarNamesByClassNames = new Properties();
			
			String jarName = jarNames[j];
			rss.setStep("Investigating " + jarName + " (" + (j+1) + "/" + jarNames.length + ")");
			try {
				JarInputStream jis = new JarInputStream(this.dataProvider.getInputStream(jarName));
				JarEntry je;
				while ((je = jis.getNextJarEntry()) != null) {
					String jarEntryName = je.getName();
					
					//	new class file
					if (jarEntryName.endsWith(".class")) {
						String className = StringUtils.replaceAll(jarEntryName.substring(0, (jarEntryName.length() - 6)), "/", ".");
						
						//	collect names of all non-nested classes
						if (className.indexOf('$') == -1) {
							jarClassNames.addElementIgnoreDuplicates(className);
							jarNamesByClassNames.setProperty(className, jarName);
						}
					}
				}
				jis.close();
				rss.setInfo("- class names collected");
				
				//	create class loader
				AnalyzerClassLoader jarLoader = new AnalyzerClassLoader();
				jarLoader.addJar(jarName);
				
				//	check for binary folder
				String jarLibBasePath = (jarName.substring(0, (jarName.length() - 4)) + JAR_BIN_FOLDER_SUFFIX);
				String[] jarBinNames = this.getJarNames(jarLibBasePath);
				String jarLibPathString = "";
				for (int b = 0; b < jarBinNames.length; b++) {
					jarLoader.addJar(jarBinNames[b], this.dataProvider.getInputStream(jarLibBasePath + "/" + jarBinNames[b]));
					jarLibPathString += (" " + jarBinNames[b]);
				}
				jarLibPathString = jarLibPathString.trim();
				rss.setInfo("- extra binaries collected");
				
				//	generate prefix to identify origin of analyzers
				String analyzerNamePrefix = jarName;
				StringBuffer anp = new StringBuffer();
				for (int c = 0; c < analyzerNamePrefix.length(); c++) {
					char ch = analyzerNamePrefix.charAt(c);
					if ((c == 0) || (StringUtils.UPPER_CASE_LETTERS.indexOf(ch) != -1))
						anp.append(Character.toUpperCase(ch));
				}
				analyzerNamePrefix = anp.toString();
				
				
				//	iterate over jar entries
				for (int jcn = 0; jcn < jarClassNames.size(); jcn++) {
					
					String className = jarClassNames.get(jcn);
					Class analyzerClass = null;
					
					//	try to load class
					try {
						analyzerClass = jarLoader.loadClass(className);
					}
					catch (ClassNotFoundException cnfe) {}
					catch (NoClassDefFoundError ncdfe) {}
					catch (SecurityException se) {} // may happen due to jar signatures
					
					//	Analyzer class loaded successfully
					if ((analyzerClass != null) && !Modifier.isAbstract(analyzerClass.getModifiers()) && Modifier.isPublic(analyzerClass.getModifiers()) && !Modifier.isInterface(analyzerClass.getModifiers()) && Analyzer.class.isAssignableFrom(analyzerClass)) {
						System.out.println("  Got Analyzer class in " + jarNames[j] + ": " + className);
						rss.setInfo("- found Analyzer class: '" + className + "'");
						
						String analyzerName = analyzerNamePrefix + "." + className.substring(className.lastIndexOf('.') + 1);
						if (!existingNames.contains(analyzerName + FILE_EXTENSION)) {
							rss.setInfo("==> found new Analyzer: '" + className + "'");
							
							Settings analyzerSettings = new Settings();
							analyzerSettings.setSetting(AnalyzerDocumentProcessor.CLASS_ATTRIBUTE_NAME, className);
							
							String classPath = jarNames[j];
							analyzerSettings.setSetting(AnalyzerDocumentProcessor.CLASS_PATH_ATTRIBUTE_NAME, classPath);
							
							if (jarLibPathString.length() != 0)
								analyzerSettings.setSetting(AnalyzerDocumentProcessor.LIB_PATH_ATTRIBUTE_NAME, jarLibPathString);
							
							String dataPath = (jarName.substring(0, (jarName.length() - 4)) + JAR_DATA_FOLDER_SUFFIX);
							analyzerSettings.setSetting(AnalyzerDocumentProcessor.DATA_PATH_ATTRIBUTE_NAME, (dataPath + "/"));
							
							if (this.storeSettingsResource((analyzerName + FILE_EXTENSION), analyzerSettings)) {
								foundNew = true;
//								this.analyzerNamesByClassNames.setProperty((analyzerName + FILE_EXTENSION), className);
								existingNames.addElementIgnoreDuplicates(analyzerName + FILE_EXTENSION);
							}
						}
					}
				}
			} catch (IOException ioe) {}
			rss.setProgress((100 * (j+1)) / jarNames.length);
		}
		
		return foundNew;
	}
	
	private String[] getJarNames(String prefix) {
		StringVector dataNameList = new StringVector();
		String[] dataNames = this.dataProvider.getDataNames();
		for (int n = 0; n < dataNames.length; n++)
			if (dataNames[n].endsWith(".jar") && ((dataNames[n].startsWith(prefix) && (dataNames[n].lastIndexOf('/') == prefix.length())) || ((prefix.length() == 0) && (dataNames[n].indexOf('/') == -1))))
				dataNameList.addElement(dataNames[n].substring(dataNames[n].indexOf('/') + 1));
		return dataNameList.toStringArray();
	}
	
	private Analyzer produceAnalyzer(String name, Settings settings) {
		if (settings.size() == 0) return null;
		
		System.out.println("AnalyzerManager: loading " + name + " ...");
		
		String analyzerClassName = settings.getSetting(AnalyzerDocumentProcessor.CLASS_ATTRIBUTE_NAME);
		System.out.println("  class is " + analyzerClassName);
		
		String analyzerClassPath = settings.getSetting(AnalyzerDocumentProcessor.CLASS_PATH_ATTRIBUTE_NAME);
		System.out.println("  class path is " + analyzerClassPath);
		
		String analyzerDataPrefix = analyzerClassPath.substring(0, (analyzerClassPath.length() - 4));
		
		String analyzerDataPath = settings.getSetting(AnalyzerDocumentProcessor.DATA_PATH_ATTRIBUTE_NAME, (analyzerDataPrefix + JAR_DATA_FOLDER_SUFFIX));
		System.out.println("  data path is " + analyzerDataPath);
		
		StringVector libPathParser = new StringVector();
		libPathParser.parseAndAddElements(settings.getSetting(AnalyzerDocumentProcessor.LIB_PATH_ATTRIBUTE_NAME, ""), " ");
		libPathParser.removeAll("");
		for (int l = 0; l < libPathParser.size(); l++)
			System.out.println("  lib: " + libPathParser.get(l));
		
		
		//	extends class loader (this makes sure every jar is loaded only once)
		if (this.analyzerLoader != null) try {
			this.analyzerLoader.addJar(analyzerClassPath);
			for (int l = 0; l < libPathParser.size(); l++)
				this.analyzerLoader.addJar(libPathParser.get(l), this.dataProvider.getInputStream(analyzerDataPrefix + JAR_BIN_FOLDER_SUFFIX + "/" + libPathParser.get(l)));
		}
		catch (IOException ioe) {
			System.out.println("Exception extending analyzer class loader for Analyzer '" + name + "':\n  " + ioe.getMessage());
			ioe.printStackTrace(System.out);
		}
		System.out.println("  source jars added to class loader");
		
		
		//	load analyzer class
		Class analyzerClass = null;
		try {
			
			//	try system class path if analyzer loader could not be created
			if (this.analyzerLoader == null)
				analyzerClass = Class.forName(analyzerClassName);
			
			//	use class loader otherwise
			else analyzerClass = this.analyzerLoader.loadClass(analyzerClassName);
		}
		catch (ClassNotFoundException cnfe) {
			System.out.println("  could not load class \"" + analyzerClassName + "\" from \"" + analyzerClassPath + "\": " + cnfe.getMessage() + ".");
			this.printClassPath(this.analyzerLoader);
		}
		catch (NoClassDefFoundError ncdfe) {
			System.out.println("  could not find some parts of " + analyzerClassName + ":\n " + ncdfe.getMessage());
			this.printClassPath(this.analyzerLoader);
		}
		catch (SecurityException se) { // may happen due to jar signatures
			System.out.println("  not allowed to load class - " + se.getMessage());
		}
		System.out.println("  class loaded");
		
		
		//	Analyzer class loaded successfully
		if ((analyzerClass != null) && !Modifier.isAbstract(analyzerClass.getModifiers()) && Modifier.isPublic(analyzerClass.getModifiers()) && !Modifier.isInterface(analyzerClass.getModifiers()) && Analyzer.class.isAssignableFrom(analyzerClass)) {
			
			//	instantiate Analyzer and return it
			try {
				Object rawAnalyzer = analyzerClass.newInstance();
				Analyzer analyzer = ((Analyzer) rawAnalyzer);
				System.out.println("  Analyzer class instantiated");
				analyzer.setDataProvider(new PrefixAnalyzerDataProvider(analyzerDataPath));
				System.out.println("  Analyzer initialized");
				return analyzer;
			}
			catch (InstantiationException ie) {
				System.out.println("  could not instantiate class \"" + analyzerClassName + "\": " + ie.getMessage() + ".");
			}
			catch (IllegalAccessException iae) {
				System.out.println("  illegal acces to class \"" + analyzerClassName + "\": " + iae.getMessage() + ".");
			}
			catch (SecurityException se) { // may happen due to jar signatures
				System.out.println("  not allowed to instantiate Analzyer class - " + se.getMessage());
			}
			catch (NoClassDefFoundError ncdfe) {
				System.out.println("  could not find some part of \"" + analyzerClassName + "\": " + ncdfe.getMessage() + ".");
				this.printClassPath(this.analyzerLoader);
			}
		}
		
		System.out.println("  could not load Analyzer class \"" + analyzerClassName + "\".");
		return null;
	}
	
	private void printClassPath(AnalyzerClassLoader acl) {
		System.out.println("  URLs in analyzer class loader:");
		if (acl != null) {
			String[] jarNames = acl.getJarNames();
			for (int u = 0; u < jarNames.length; u++)
				System.out.println("    " + jarNames[u]);
		}
	}
	
	private class AnalyzerClassLoader {
		GamtaClassLoader cl;
		TreeSet jarNameSet = new TreeSet();
		AnalyzerClassLoader() {
			this.cl = GamtaClassLoader.createClassLoader(AnalyzerManager.class);
			String[] dataNames = AnalyzerManager.this.dataProvider.getDataNames();
			for (int d = 0; d < dataNames.length; d++) {
				if (!dataNames[d].startsWith("Bin/"))
					continue;
				if (!dataNames[d].endsWith(".jar"))
					continue;
				if (dataNames[d].indexOf('/') != dataNames[d].lastIndexOf('/'))
					continue;
				System.out.println("Adding shared jar: " + dataNames[d]);
				try {
					this.addJar(dataNames[d]);
				}
				catch (IOException ioe) {
					System.out.println("Exception adding shared jar: " + ioe.getMessage());
					ioe.printStackTrace(System.out);
				}
			}
		}
		
		public Class loadClass(String name) throws ClassNotFoundException {
			return this.cl.loadClass(name);
		}
		
		boolean addJar(String jarName) throws IOException {
			if (this.jarNameSet.add(jarName)) {
				InputStream jarSource = dataProvider.getInputStream(jarName);
				this.addJar(jarSource);
				jarSource.close();
				return true;
			}
			else return false;
		}
		boolean addJar(String jarName, InputStream jarSource) throws IOException {
			if (this.jarNameSet.add(jarName)) {
				this.addJar(jarSource);
				return true;
			}
			else return false;
		}
		void addJar(InputStream jarSource) throws IOException {
			this.cl.addJar(jarSource);
		}
		String[] getJarNames() {
			return ((String[]) this.jarNameSet.toArray(new String[this.jarNameSet.size()]));
		}
	}
	
	private class PrefixAnalyzerDataProvider extends AbstractAnalyzerDataProvider {
		private String pathPrefix;
		PrefixAnalyzerDataProvider(String pathPrefix) {
			this.pathPrefix = (pathPrefix.endsWith("/") ? pathPrefix : (pathPrefix + "/"));
		}
		public boolean deleteData(String name) {
			if (analyzerDataWritable)
				return dataProvider.deleteData(this.addPrefix(name));
			else return false;
		}
		public String[] getDataNames() {
			String[] names = dataProvider.getDataNames();
			StringVector list = new StringVector();
			for (int n = 0; n < names.length; n++)
				if (names[n].startsWith(this.pathPrefix))
					list.addElementIgnoreDuplicates(names[n].substring(this.pathPrefix.length()));
			return list.toStringArray();
		}
		public InputStream getInputStream(String dataName) throws IOException {
			return dataProvider.getInputStream(this.addPrefix(dataName));
		}
		public OutputStream getOutputStream(String dataName) throws IOException {
			if (analyzerDataWritable)
				return dataProvider.getOutputStream(this.addPrefix(dataName));
			else return null;
		}
		public URL getURL(String dataName) throws IOException {
			return dataProvider.getURL((dataName.indexOf("://") == -1) ? this.addPrefix(dataName) : dataName);
		}
		public boolean isDataAvailable(String dataName) {
			return dataProvider.isDataAvailable(this.addPrefix(dataName));
		}
		public boolean isDataEditable() {
			return (analyzerDataWritable && dataProvider.isDataEditable());
		}
		public boolean isDataEditable(String dataName) {
			return (analyzerDataWritable && dataProvider.isDataEditable(this.addPrefix(dataName)));
		}
		private String addPrefix(String name) {
			return (this.pathPrefix + (name.startsWith("/") ? name.substring(1) : name));
		}
		public String getAbsolutePath() {
			return dataProvider.getAbsolutePath() + "/" + this.pathPrefix.substring(0, (this.pathPrefix.length() - 1));
		}
	}
	
	private class CreateAnalyzerDialog extends DialogPanel {
		
		private JTextField nameField;
		
		private AnalyzerEditorPanel editor;
		private String analyzerName = null;
		
		CreateAnalyzerDialog(Settings analyzer, String name) {
			super("Create Analyzer", true);
			
			this.nameField = new JTextField((name == null) ? "New Analyzer" : name);
			this.nameField.setBorder(BorderFactory.createLoweredBevelBorder());
			
			//	initialize main buttons
			JButton commitButton = new JButton("Create");
			commitButton.setBorder(BorderFactory.createRaisedBevelBorder());
			commitButton.setPreferredSize(new Dimension(100, 21));
			commitButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					analyzerName = nameField.getText();
					dispose();
				}
			});
			
			JButton abortButton = new JButton("Cancel");
			abortButton.setBorder(BorderFactory.createRaisedBevelBorder());
			abortButton.setPreferredSize(new Dimension(100, 21));
			abortButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					analyzerName = null;
					dispose();
				}
			});
			
			JPanel mainButtonPanel = new JPanel();
			mainButtonPanel.setLayout(new FlowLayout());
			mainButtonPanel.add(commitButton);
			mainButtonPanel.add(abortButton);
			
			//	initialize editor
			this.editor = new AnalyzerEditorPanel(name, analyzer);
			
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
			return (this.analyzerName != null);
		}
		
		Settings getAnalyzer() {
			return this.editor.getSettings();
		}
		
		String getAnalyzerName() {
			return this.analyzerName;
		}
	}
	
	private class EditAnalyzerDialog extends DialogPanel {
		
		private AnalyzerEditorPanel editor;
		private String analyzerName = null;
		
		EditAnalyzerDialog(Settings analyzer, String name) {
			super(("Edit Analyzer '" + name + "'"), true);
			this.analyzerName = name;
			
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
					analyzerName = null;
					dispose();
				}
			});
			
			JPanel mainButtonPanel = new JPanel();
			mainButtonPanel.setLayout(new FlowLayout());
			mainButtonPanel.add(commitButton);
			mainButtonPanel.add(abortButton);
			
			//	initialize editor
			this.editor = new AnalyzerEditorPanel(name, analyzer);
			
			//	put the whole stuff together
			this.setLayout(new BorderLayout());
			this.add(this.editor, BorderLayout.CENTER);
			this.add(mainButtonPanel, BorderLayout.SOUTH);
			
			this.setResizable(true);
			this.setSize(new Dimension(600, 400));
			this.setLocationRelativeTo(this.getOwner());
		}
		
		boolean isCommitted() {
			return (this.analyzerName != null);
		}
		
		Settings getAnalyzer() {
			return this.editor.getSettings();
		}
	}
	
	private class AnalyzerEditorPanel extends JPanel {
		
		private String analyzerName;
		private JTextField classField;
		private JTextField classPathField;
		private JTextField libPathField;
		private JTextField dataPathField;
		
		private boolean dirty = false;
		
		AnalyzerEditorPanel(String name, Settings settings) {
			super(new BorderLayout(), true);
			this.analyzerName = name;
			this.add(getExplanationLabel(), BorderLayout.CENTER);
			
			JPanel functionPanel = new JPanel(new GridBagLayout(), true);
			functionPanel.setBorder(BorderFactory.createEtchedBorder());
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.insets.top = 2;
			gbc.insets.bottom = 2;
			gbc.insets.left = 5;
			gbc.insets.right = 5;
			gbc.weighty = 0;
			gbc.gridheight = 1;
			gbc.fill = GridBagConstraints.BOTH;
			
			this.classField = new JTextField(settings.getSetting(AnalyzerDocumentProcessor.CLASS_ATTRIBUTE_NAME, ""));
			this.classField.addKeyListener(new KeyAdapter() {
				public void keyTyped(KeyEvent e) {
					dirty = true;
				}
			});
			JLabel classLabel = new JLabel("Analyzer Class");
			
			gbc.gridy = 0;
			gbc.gridx = 0;
			gbc.gridwidth = 1;
			gbc.weightx = 0;
			functionPanel.add(classLabel, gbc.clone());
			gbc.gridx = 1;
			gbc.gridwidth = 2;
			gbc.weightx = 1;
			functionPanel.add(this.classField, gbc.clone());
			
			this.classPathField = new JTextField(settings.getSetting(AnalyzerDocumentProcessor.CLASS_PATH_ATTRIBUTE_NAME, ""));
			this.classPathField.addKeyListener(new KeyAdapter() {
				public void keyTyped(KeyEvent e) {
					dirty = true;
				}
			});
			
			gbc.gridy = 1;
			gbc.gridx = 0;
			gbc.gridwidth = 1;
			gbc.weightx = 0;
			functionPanel.add(new JLabel("Analyzer Class Path"), gbc.clone());
			gbc.gridx = 1;
			gbc.gridwidth = 2;
			gbc.weightx = 1;
			functionPanel.add(this.classPathField, gbc.clone());
			
			
			this.dataPathField = new JTextField(settings.getSetting(AnalyzerDocumentProcessor.DATA_PATH_ATTRIBUTE_NAME, ""));
			this.dataPathField.addKeyListener(new KeyAdapter() {
				public void keyTyped(KeyEvent e) {
					dirty = true;
				}
			});
			
			gbc.gridy = 2;
			gbc.gridx = 0;
			gbc.gridwidth = 1;
			gbc.weightx = 0;
			functionPanel.add(new JLabel("Analyzer Data Path"), gbc.clone());
			gbc.gridx = 1;
			gbc.gridwidth = 2;
			gbc.weightx = 1;
			functionPanel.add(this.dataPathField, gbc.clone());
			
			
			this.libPathField = new JTextField(settings.getSetting(AnalyzerDocumentProcessor.LIB_PATH_ATTRIBUTE_NAME, ""));
			this.libPathField.addKeyListener(new KeyAdapter() {
				public void keyTyped(KeyEvent e) {
					dirty = true;
				}
			});
			
			gbc.gridy = 3;
			gbc.gridx = 0;
			gbc.gridwidth = 1;
			gbc.weightx = 0;
			functionPanel.add(new JLabel("Analyzer Lib Path"), gbc.clone());
			gbc.gridx = 1;
			gbc.gridwidth = 2;
			gbc.weightx = 1;
			functionPanel.add(this.libPathField, gbc.clone());
			
			
			JButton configButton = new JButton("Configure Analyzer Processor");
			configButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					configureAnalyzer(analyzerName);
				}
			});
			
			gbc.gridy = 4;
			gbc.gridx = 0;
			gbc.gridwidth = 3;
			gbc.weightx = 1;
			functionPanel.add(configButton, gbc.clone());
			this.add(functionPanel, BorderLayout.SOUTH);
		}
		
		boolean isDirty() {
			return this.dirty;
		}
		
		Settings getSettings() {
			Settings set = new Settings();
			
			set.setSetting(AnalyzerDocumentProcessor.CLASS_ATTRIBUTE_NAME, this.classField.getText());
			set.setSetting(AnalyzerDocumentProcessor.CLASS_PATH_ATTRIBUTE_NAME, this.classPathField.getText());
			
			String dataPath = (StringUtils.replaceAll(this.dataPathField.getText(), "\\", "/"));
			if (!dataPath.endsWith("/")) dataPath += "/";
			while (dataPath.endsWith("//")) dataPath = dataPath.substring(0, (dataPath.length() - 1));
			set.setSetting(AnalyzerDocumentProcessor.DATA_PATH_ATTRIBUTE_NAME, dataPath);
			
			String libPath = (StringUtils.replaceAll(this.libPathField.getText(), "\\", "/"));
			while (libPath.endsWith("//")) libPath = libPath.substring(0, (libPath.length() - 1));
			set.setSetting(AnalyzerDocumentProcessor.LIB_PATH_ATTRIBUTE_NAME, libPath);
			
			return set;
		}
	}
}
