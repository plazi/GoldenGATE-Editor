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
package de.uka.ipd.idaho.goldenGate.plugin.gPaths;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import de.uka.ipd.idaho.easyIO.EasyIO;
import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.Attributed;
import de.uka.ipd.idaho.gamta.DocumentRoot;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.Token;
import de.uka.ipd.idaho.gamta.TokenSequence;
import de.uka.ipd.idaho.gamta.TokenSequenceUtils;
import de.uka.ipd.idaho.gamta.Tokenizer;
import de.uka.ipd.idaho.gamta.defaultImplementation.PlainTokenSequence;
import de.uka.ipd.idaho.gamta.util.GamtaClassLoader;
import de.uka.ipd.idaho.gamta.util.GamtaClassLoader.InputStreamProvider;
import de.uka.ipd.idaho.gamta.util.gPath.GPath;
import de.uka.ipd.idaho.gamta.util.gPath.GPathEngine;
import de.uka.ipd.idaho.gamta.util.gPath.GPathFunction;
import de.uka.ipd.idaho.gamta.util.gPath.GPathParser;
import de.uka.ipd.idaho.gamta.util.gPath.GPathVariableResolver;
import de.uka.ipd.idaho.gamta.util.gPath.exceptions.GPathException;
import de.uka.ipd.idaho.gamta.util.swing.AnnotationDisplayDialog;
import de.uka.ipd.idaho.goldenGate.AnnotationEditorPanel;
import de.uka.ipd.idaho.goldenGate.DocumentEditor;
import de.uka.ipd.idaho.goldenGate.DocumentEditorDialog;
import de.uka.ipd.idaho.goldenGate.GoldenGATE;
import de.uka.ipd.idaho.goldenGate.GoldenGateConstants;
import de.uka.ipd.idaho.goldenGate.plugins.AbstractAnnotationFilterManager;
import de.uka.ipd.idaho.goldenGate.plugins.AnnotationFilter;
import de.uka.ipd.idaho.goldenGate.plugins.DocumentProcessor;
import de.uka.ipd.idaho.goldenGate.plugins.Resource;
import de.uka.ipd.idaho.goldenGate.util.DataListListener;
import de.uka.ipd.idaho.goldenGate.util.DialogPanel;
import de.uka.ipd.idaho.goldenGate.util.EditFontsButton;
import de.uka.ipd.idaho.goldenGate.util.FontEditable;
import de.uka.ipd.idaho.goldenGate.util.FontEditorDialog;
import de.uka.ipd.idaho.goldenGate.util.FontEditorPanel;
import de.uka.ipd.idaho.goldenGate.util.ResourceDialog;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * Manager for GPath based AnnotationFilters. Note that GPaths selecting
 * individual tokens as results are not of much use, since tokens are not
 * annotation in the GAMTA data model. Consequently, the filter will nor return
 * any 'real' annotations if the backing GPaths selects individual tokens.<br>
 * <br>
 * All configuration can be done in the 'Edit GPaths' dialog in the GoldenGATE
 * Editor, which provides an editor for GPath expressions.<br>
 * <br>
 * For deploying custom GPathFunctions, export the respective classes to jar
 * files and deposit these jar files in the 'Functions' sub folder of the
 * GPathManager's data path.
 * 
 * @author sautter
 */
public class GPathManager extends AbstractAnnotationFilterManager {
	
	private static final String FUNCTION_FOLDER_NAME = "Functions";
	private static final String FILE_EXTENSION = ".gPath";
	
	private JFileChooser fileChooser = null;
	
	private GPathEngine gPathEngine;
	private String[] customFunctionNames;
	
	public GPathManager() {}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.ResourceManager#init()
	 */
	public void init() {
		
		//	instantiate GPath engine
		this.gPathEngine = new GPathEngine();
		
//		//	load custom functions
//		StringVector functionJarNames = new StringVector();
//		String[] dataNames = this.dataProvider.getDataNames();
//		for (int n = 0; n < dataNames.length; n++)
//			if (dataNames[n].startsWith(FUNCTION_FOLDER_NAME) && (dataNames[n].lastIndexOf('/') == FUNCTION_FOLDER_NAME.length()) && dataNames[n].endsWith(".jar"))
//				functionJarNames.addElement(dataNames[n]);
//		
//		URL[] jarURLs = new URL[functionJarNames.size()];
//		StringVector jarClassNames = new StringVector();
//		
//		for (int j = 0; j < functionJarNames.size(); j++) {
//			try {
//				jarURLs[j] = this.dataProvider.getURL(functionJarNames.get(j));
//				JarInputStream jis = new JarInputStream(jarURLs[j].openStream());
//				JarEntry je;
//				while ((je = jis.getNextJarEntry()) != null) {
//					String jarEntryName = je.getName();
//					
//					//	new class file
//					if (jarEntryName.endsWith(".class")) {
//						String className = StringUtils.replaceAll(jarEntryName.substring(0, (jarEntryName.length() - 6)), "/", ".");
//						
//						//	collect names of all non-nested classes
//						if (className.indexOf('$') == -1)
//							jarClassNames.addElementIgnoreDuplicates(className);
//					}
//				}
//			}
//			catch (IOException ioe) {}
//		}
//		
//		ClassLoader functionLoader = null;
//		try {
//			functionLoader = new URLClassLoader(jarURLs, GPathFunction.class.getClassLoader());
//		}
//		
//		//	catch security exceptions that may occur in contexts where creating class loaders is not allowed, eg in Applets
//		catch (SecurityException se) {}
//		
//		ArrayList functions = new ArrayList();
//		
//		//	iterate over jar entries
//		for (int jcn = 0; jcn < jarClassNames.size(); jcn++) {
//			
//			String className = jarClassNames.get(jcn);
//			Class functionClass = null;
//			
//			//	try to load class
//			try {
//				//	could not create class loader, try system class path
//				if (functionLoader == null)
//					functionClass = Class.forName(className);
//				
//				//	load function class through specific class loader if given
//				else functionClass = functionLoader.loadClass(className);
//				
//			}
//			catch (ClassNotFoundException cnfe) {}
//			catch (NoClassDefFoundError ncdfe) {}
//			catch (SecurityException se) {} // may happen due to jar signatures
//			
//			//	Analyzer class loaded successfully
//			if ((functionClass != null) && !Modifier.isAbstract(functionClass.getModifiers()) && Modifier.isPublic(functionClass.getModifiers()) && !Modifier.isInterface(functionClass.getModifiers()) && GPathFunction.class.isAssignableFrom(functionClass)) {
//				System.out.println("GPathManager: got custom function class - " + className);
//				try {
//					Object o = functionClass.newInstance();
//					functions.add((GPathFunction) o);
//					System.out.println("  custom function successfully instantiated.");
//				}
//				catch (InstantiationException e) {
//					System.out.println("  could not instantiate function class.");
//				}
//				catch (IllegalAccessException e) {
//					System.out.println("  illegal acces to function class.");
//				}
//			}
//		}
//		
//		GPathFunction[] customFunctions = ((GPathFunction[]) functions.toArray(new GPathFunction[functions.size()]));
//		this.customFunctionNames = new String[customFunctions.length];
//		for (int f = 0; f < customFunctions.length; f++) {
//			String functionName = customFunctions[f].getClass().getName();
//			functionName = functionName.substring(functionName.lastIndexOf('.') + 1);
//			functionName = functionName.substring(0, 1).toLowerCase() + functionName.substring(1);
//			this.customFunctionNames[f] = functionName;
//			this.gPathEngine.addFunction(functionName, customFunctions[f]);
//		}
		
		//	load functions
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
				GPathFunction.class, 
				null);
		
		//	register functions
		this.customFunctionNames = new String[functionObjects.length];
		for (int f = 0; f < functionObjects.length; f++) {
			GPathFunction function = ((GPathFunction) functionObjects[f]);
			String functionName = function.getClass().getName();
			functionName = functionName.substring(functionName.lastIndexOf('.') + 1);
			functionName = functionName.substring(0, 1).toLowerCase() + functionName.substring(1);
			this.customFunctionNames[f] = functionName;
			this.gPathEngine.addFunction(functionName, function);
		}
	}
	
	/* 
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AnnotationFilterManager#createAnnotationFilter()
	 */
	public String createAnnotationFilter() {
		return this.createGPath(null, null);
	}

	/* 
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AnnotationFilterManager#editAnnotationFilter(java.lang.String)
	 */
	public void editAnnotationFilter(String name) {
		String gPath = this.loadStringResource(name);
		if (gPath == null) return;
		
		EditGPathDialog egpd = new EditGPathDialog(gPath, name);
		egpd.setVisible(true);
		
		if (egpd.isCommitted()) try {
			this.storeStringResource(name, egpd.getGPath());
			this.parent.notifyResourcesChanged(this.getClass().getName());
		} catch (IOException ioe) {}
	}

	/* 
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AnnotationFilterManager#editAnnotationFilters()
	 */
	public void editAnnotationFilters() {
		this.editGPaths();
	}

	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AnnotationFilterManager#getAnnotationFilter(java.lang.String)
	 */
	public AnnotationFilter getAnnotationFilter(String name) {
		String path = this.getGPath(name);
		if (path == null)
			return null;
		else return new GPathAnnotationFilter(name, new GPath(path));
	}
	
	private class GPathAnnotationFilter implements AnnotationFilter {
		
		private String name;
		private GPath gPath;
		
		private GPathAnnotationFilter(String name, GPath gPath) {
			this.name = name;
			this.gPath = gPath;
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.Resource#getName()
		 */
		public String getName() {
			return this.name;
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.Resource#getProviderClassName()
		 */
		public String getProviderClassName() {
			return GPathManager.class.getName();
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.Resource#getTypeLabel()
		 */
		public String getTypeLabel() {
			return "GPath";
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.AnnotationFilter#accept(de.uka.ipd.idaho.gamta.Annotation)
		 */
		public boolean accept(Annotation annotation) {
			QueriableAnnotation qa = ((annotation instanceof QueriableAnnotation) ? ((QueriableAnnotation) annotation) : new FilterableAnnotation(annotation));
			try {
				Annotation[] result = gPathEngine.evaluatePath(qa, this.gPath, null);
				return (result.length != 0);
			}
			catch (GPathException gpe) {
				return false;
			}
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGate.plugins.AnnotationFilter#getMatches(de.uka.ipd.idaho.gamta.QueriableAnnotation)
		 */
		public QueriableAnnotation[] getMatches(QueriableAnnotation data) {
			try {
				return gPathEngine.evaluatePath(data, this.gPath, null);
			}
			catch (GPathException gpe) {
				return new QueriableAnnotation[0];
			}
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.AnnotationFilter#getMutableMatches(de.uka.ipd.idaho.gamta.MutableAnnotation)
		 */
		public MutableAnnotation[] getMutableMatches(MutableAnnotation data) {
			QueriableAnnotation[] matches = this.getMatches(data);
			Set matchIDs = new HashSet();
			for (int m = 0; m < matches.length; m++)
				matchIDs.add(matches[m].getAnnotationID());
			ArrayList mutableMatches = new ArrayList();
			if (matchIDs.remove(data.getAnnotationID()))
				mutableMatches.add(data);
			MutableAnnotation[] mutableAnnotations = data.getMutableAnnotations();
			for (int m = 0; m < mutableAnnotations.length; m++)
				if (matchIDs.contains(mutableAnnotations[m].getAnnotationID()))
					mutableMatches.add(mutableAnnotations[m]);
			Collections.sort(mutableMatches, AnnotationUtils.getComparator(data.getAnnotationNestingOrder()));
			return ((MutableAnnotation[]) mutableMatches.toArray(new MutableAnnotation[mutableMatches.size()]));
		}
		
		/** wrapper for making an arbitrary Annotation mimic a QueriableAnnotation */
		private class FilterableAnnotation implements QueriableAnnotation {
			
			private Annotation data;
			
			private FilterableAnnotation(Annotation data) {
				this.data = data;
			}
			public int getAbsoluteStartOffset() {
				return this.data.getStartOffset();
			}
			public String getDocumentProperty(String propertyName, String defaultValue) {
				return this.data.getDocumentProperty(propertyName, defaultValue);
			}
			public String getDocumentProperty(String propertyName) {
				return this.data.getDocumentProperty(propertyName);
			}
			public String[] getDocumentPropertyNames() {
				return this.data.getDocumentPropertyNames();
			}
			public int getStartOffset() {
				return this.data.getStartOffset();
			}
			public int getEndOffset() {
				return this.data.getEndOffset();
			}
			public String getWhitespaceAfter(int index) {
				return this.data.getWhitespaceAfter(index);
			}
			public char charAt(int index) {
				return this.data.charAt(index);
			}
			public CharSequence subSequence(int start, int end) {
				return this.data.subSequence(start, end);
			}
			public void copyAttributes(Attributed source) {}
			public int getAbsoluteStartIndex() {
				return this.data.getStartIndex();
			}
			public int getEndIndex() {
				return this.data.getEndIndex();
			}
			public int getStartIndex() {
				return this.data.getStartIndex();
			}
			public int size() {
				return this.data.size();
			}
			public String getType() {
				return this.data.getType();
			}
			public String getAnnotationID() {
				return this.data.getAnnotationID();
			}
			public String getValue() {
				return this.data.getValue();
			}
			public QueriableAnnotation[] getAnnotations() {
				return new QueriableAnnotation[0];
			}
			public QueriableAnnotation[] getAnnotations(String type) {
				return new QueriableAnnotation[0];
			}
			public String[] getAnnotationTypes() {
				String[] types = {this.data.getType()};
				return types;
			}
			public String getAnnotationNestingOrder() {
				return DocumentRoot.DEFAULT_ANNOTATION_NESTING_ORDER;
			}
			public String toXML() {
				return this.data.toXML();
			}
			public String changeTypeTo(String newType) {
				return this.data.getType();
			}
			public boolean hasAttribute(String name) {
				return this.data.hasAttribute(name);
			}
			public Object getAttribute(String name, Object def) {
				return this.data.getAttribute(name, def);
			}
			public Object getAttribute(String name) {
				return this.data.getAttribute(name);
			}
			public String[] getAttributeNames() {
				return this.data.getAttributeNames();
			}
			public Object removeAttribute(String name) {
				return this.data.removeAttribute(name);
			}
			public void setAttribute(String name) {
				this.data.setAttribute(name);
			}
			public Object setAttribute(String name, Object value) {
				return this.data.setAttribute(name, value);
			}
			public void clearAttributes() {
				this.data.clearAttributes();
			}
			public int compareTo(Object o) {
				return this.data.compareTo(o);
			}
			public String getLeadingWhitespace() {
				return this.data.getLeadingWhitespace();
			}
			public Token firstToken() {
				return this.data.firstToken();
			}
			public String firstValue() {
				return this.data.firstValue();
			}
			public Token lastToken() {
				return this.data.lastToken();
			}
			public String lastValue() {
				return this.data.lastValue();
			}
			public Token tokenAt(int index) {
				return this.data.tokenAt(index);
			}
			public String valueAt(int index) {
				return this.data.valueAt(index);
			}
			public TokenSequence getSubsequence(int start, int size) {
				return this.data.getSubsequence(start, size);
			}
			public Tokenizer getTokenizer() {
				return this.data.getTokenizer();
			}
			public int length() {
				return this.data.length();
			}
		}
	}
	
	/**
	 * Test a gPath expression
	 * @param gPath the GPath expression to test
	 * @return the Annotations in the currently selected document that match the
	 *         specified GPath expression
	 */
	public Annotation[] testPath(String gPath) throws GPathException {
		QueriableAnnotation data = this.parent.getActiveDocument();
		return ((data == null) ? null : this.gPathEngine.evaluatePath(data, gPath, null));
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.ResourceManager#getResourceTypeLabel()
	 */
	public String getResourceTypeLabel() {
		return "GPath Expression";
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
		
		if (this.dataProvider.isDataEditable()) {
			mi = new JMenuItem("Create");
			mi.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					createGPath();
				}
			});
			collector.add(mi);
			mi = new JMenuItem("Load");
			mi.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					loadGPath();
				}
			});
			collector.add(mi);
			mi = new JMenuItem("Edit");
			mi.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					editGPaths();
				}
			});
			collector.add(mi);
			collector.add(GoldenGateConstants.MENU_SEPARATOR_ITEM);
		}
		
		mi = new JMenuItem("Evaluate");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				evaluatePath(null, parent.getActivePanel());
			}
		});
		collector.add(mi);
		return ((JMenuItem[]) collector.toArray(new JMenuItem[collector.size()]));
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager#getToolsMenuFunctionItems(de.uka.ipd.idaho.goldenGate.GoldenGateConstants.InvokationTargetProvider)
	 */
	public JMenuItem[] getToolsMenuFunctionItems(final InvokationTargetProvider tragetProvider) {
		ArrayList collector = new ArrayList();
		JMenuItem mi;
		
		mi = new JMenuItem("Evaluate GPath");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				evaluatePath(null, tragetProvider.getFunctionTarget());
			}
		});
		collector.add(mi);
		
		return ((JMenuItem[]) collector.toArray(new JMenuItem[collector.size()]));
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#getMainMenuTitle()
	 */
	public String getMainMenuTitle() {
		return "GPath Expressions";
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager#getToolsMenuLabel()
	 */
	public String getToolsMenuLabel() {
		return "Evaluate";
	}
	
	/**
	 * Retrieve a plain String representation of a GPath expression by its name
	 * @param name the name of the reqired GPath expression
	 * @return the String representation of the GPath expression with the
	 *         required name, or null, if there is no such GPath expression
	 */
	public String getGPath(String name) {
		if (name == null) return null;
		return this.loadStringResource(name);
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager#getDataNamesForResource(java.lang.String)
	 */
	public String[] getDataNamesForResource(String name) {
		String[] names = {(name + "@" + this.getClass().getName()), (FUNCTION_FOLDER_NAME + "/" + "@" + this.getClass().getName())};
		return names;
	}
	
	/**
	 * Evaluate a GPath query on a MutableAnnotation
	 * @param data the MutableAnnotation to evaluate the query on
	 * @param path the GPath to evaluate
	 * @return an array containing the Annotations resulting form the evaluation
	 */
	public Annotation[] evaluatePath(MutableAnnotation data, String path) throws GPathException {
		return this.gPathEngine.evaluatePath(data, path, null);
	}

	/**
	 * Evaluate a GPath query on a MutableAnnotation
	 * @param data the MutableAnnotation to evaluate the query on
	 * @param path the GPath to evaluate
	 * @param variableBindings the variable bindings which are currently valid
	 * @return an array containing the Annotations resulting form the evaluation
	 */
	public Annotation[] evaluatePath(MutableAnnotation data, String path, GPathVariableResolver variableBindings) throws GPathException {
		return this.gPathEngine.evaluatePath(data, path, variableBindings);
	}

	/**
	 * Evaluate a GPath query on a MutableAnnotation
	 * @param data the MutableAnnotation to evaluate the query on
	 * @param pathName the name of the GPath to evaluate
	 * @return an array containing the Annotations resulting form the
	 *         evaluation, or null, if there is no GPath with the specified name
	 */
	public Annotation[] evaluatePathByName(MutableAnnotation data, String pathName) throws GPathException {
		String gPath = this.getGPath(pathName);
		return ((gPath == null) ? null : this.gPathEngine.evaluatePath(data, gPath, null));
	}

	/**
	 * Evaluate a GPath query on a MutableAnnotation
	 * @param data the MutableAnnotation to evaluate the query on
	 * @param pathName the name of the GPath to evaluate
	 * @param variableBindings the variable bindings which are currently valid
	 * @return an array containing the Annotations resulting form the
	 *         evaluation, or null, if there is no GPath with the specified name
	 */
	public Annotation[] evaluatePathByName(MutableAnnotation data, String pathName, GPathVariableResolver variableBindings) throws GPathException {
		String gPath = this.getGPath(pathName);
		return ((gPath == null) ? null : this.gPathEngine.evaluatePath(data, gPath, variableBindings));
	}
	
	private boolean createGPath() {
		return (this.createGPath(null, null) != null);
	}
	
	private boolean loadGPath() {
		if (this.fileChooser == null) try {
			this.fileChooser = new JFileChooser();
		} catch (SecurityException se) {}
		
		if ((this.fileChooser != null) && (this.fileChooser.showOpenDialog(DialogPanel.getTopWindow()) == JFileChooser.APPROVE_OPTION)) {
			File file = this.fileChooser.getSelectedFile();
			if ((file != null) && file.isFile()) {
				try {
					String fileName = file.toString();
					fileName = fileName.substring(fileName.lastIndexOf(File.separatorChar) + 1);
					return (this.createGPath(EasyIO.readFile(file), fileName) != null);
				} catch (IOException ioe) {
					JOptionPane.showMessageDialog(DialogPanel.getTopWindow(), (ioe.getClass().getName() + " (" + ioe.getMessage() + ")\nwhile trying to load " + file.toString()), "Could Not Read File", JOptionPane.ERROR_MESSAGE);
				}
			}
		}
		return false;
	}
	
	private boolean cloneGPath() {
		String selectedName = this.resourceNameList.getSelectedName();
		if (selectedName == null)
			return this.createGPath();
		else {
			String name = "New " + selectedName;
			return (this.createGPath(this.loadStringResource(selectedName), name) != null);
		}
	}
	
	private String createGPath(String modelGPath, String name) {
		CreateGPathDialog cred = new CreateGPathDialog(modelGPath, name);
		cred.setVisible(true);
		if (cred.isCommitted()) {
			String gPath = cred.getGPath();
			String gPathName = cred.getGPathName();
			if (!gPathName.endsWith(FILE_EXTENSION)) gPathName += FILE_EXTENSION;
			try {
				if (this.storeStringResource(gPathName, gPath)) {
					this.parent.notifyResourcesChanged(this.getClass().getName());
					this.resourceNameList.refresh();
					return gPathName;
				}
			} catch (IOException e) {}
		}
		return null;
	}
	
	private void editGPaths() {
		final GPathEditorPanel[] editor = new GPathEditorPanel[1];
		editor[0] = null;
		
		final DialogPanel editDialog = new DialogPanel("Edit GPath Expressions", true);
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
						storeStringResource(editor[0].gPathName, editor[0].getGPath());
					} catch (IOException ioe) {}
				}
				if (editDialog.isVisible()) editDialog.dispose();
			}
		});
		
		JPanel editButtons = new JPanel(new FlowLayout(FlowLayout.CENTER));
		JButton button;
		button = new JButton("Create");
		button.setBorder(BorderFactory.createRaisedBevelBorder());
		button.setPreferredSize(new Dimension(80, 21));
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				createGPath();
			}
		});
		editButtons.add(button);
		button = new JButton("Load");
		button.setBorder(BorderFactory.createRaisedBevelBorder());
		button.setPreferredSize(new Dimension(80, 21));
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				loadGPath();
			}
		});
		editButtons.add(button);
		button = new JButton("Clone");
		button.setBorder(BorderFactory.createRaisedBevelBorder());
		button.setPreferredSize(new Dimension(80, 21));
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				cloneGPath();
			}
		});
		editButtons.add(button);
		button = new JButton("Delete");
		button.setBorder(BorderFactory.createRaisedBevelBorder());
		button.setPreferredSize(new Dimension(80, 21));
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (deleteResource(resourceNameList.getSelectedName()))
					resourceNameList.refresh();
			}
		});
		editButtons.add(button);
		
		button = new JButton("Evaluate");
		button.setBorder(BorderFactory.createRaisedBevelBorder());
		button.setPreferredSize(new Dimension(80, 21));
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				evaluatePath();
			}
		});
		editButtons.add(button);
		button = new JButton("Show Custom Functions");
		button.setBorder(BorderFactory.createRaisedBevelBorder());
		button.setPreferredSize(new Dimension(150, 21));
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				showCustomFunctions();
			}
		});
		editButtons.add(button);
		
		editDialog.add(editButtons, BorderLayout.NORTH);
		
		final JPanel editorPanel = new JPanel(new BorderLayout());
		String selectedName = this.resourceNameList.getSelectedName();
		if (selectedName == null)
			editorPanel.add(this.getExplanationLabel(), BorderLayout.CENTER);
		else {
			String gPath = this.loadStringResource(selectedName);
			if (gPath == null)
				editorPanel.add(this.getExplanationLabel(), BorderLayout.CENTER);
			else {
				editor[0] = new GPathEditorPanel(selectedName, gPath, editDialog);
				editorPanel.add(editor[0], BorderLayout.CENTER);
			}
		}
		editDialog.add(editorPanel, BorderLayout.CENTER);
		
		editDialog.add(this.resourceNameList, BorderLayout.EAST);
		DataListListener dll = new DataListListener() {
			public void selected(String dataName) {
				if ((editor[0] != null) && editor[0].isDirty()) {
					try {
						storeStringResource(editor[0].gPathName, editor[0].getGPath());
					}
					catch (IOException ioe) {
						if (JOptionPane.showConfirmDialog(editDialog, (ioe.getClass().getName() + " (" + ioe.getMessage() + ")\nwhile saving file to " + editor[0].gPathName + "\nProceed?"), "Could Not Save Analyzer", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
							resourceNameList.setSelectedName(editor[0].gPathName);
							editorPanel.validate();
							return;
						}
					}
				}
				editorPanel.removeAll();
				
				if (dataName == null)
					editorPanel.add(getExplanationLabel(), BorderLayout.CENTER);
				else {
					String gPath = loadStringResource(dataName);
					if (gPath == null)
						editorPanel.add(getExplanationLabel(), BorderLayout.CENTER);
					else {
						editor[0] = new GPathEditorPanel(dataName, gPath, editDialog);
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
	
	private class CreateGPathDialog extends DialogPanel {
		
		private JTextField nameField;
		
		private GPathEditorPanel editor;
		private String gPath = null;
		private String gPathName = null;
		
		CreateGPathDialog(String gPath, String name) {
			super("Create GPath", true);
			
			this.nameField = new JTextField((name == null) ? "New GPath" : name);
			this.nameField.setBorder(BorderFactory.createLoweredBevelBorder());
			
			//	initialize main buttons
			JButton commitButton = new JButton("Create");
			commitButton.setBorder(BorderFactory.createRaisedBevelBorder());
			commitButton.setPreferredSize(new Dimension(100, 21));
			commitButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					CreateGPathDialog.this.gPath = editor.getGPath();
					gPathName = nameField.getText();
					dispose();
				}
			});
			
			JButton abortButton = new JButton("Cancel");
			abortButton.setBorder(BorderFactory.createRaisedBevelBorder());
			abortButton.setPreferredSize(new Dimension(100, 21));
			abortButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					CreateGPathDialog.this.gPath = null;
					gPathName = null;
					dispose();
				}
			});
			
			JPanel mainButtonPanel = new JPanel(new FlowLayout());
			mainButtonPanel.add(commitButton);
			mainButtonPanel.add(abortButton);
			
			//	initialize editor
			this.editor = new GPathEditorPanel(name, gPath, this);
			
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
			return (this.gPath != null);
		}
		
		String getGPath() {
			return this.gPath;
		}
		
		String getGPathName() {
			return this.gPathName;
		}
	}

	private class EditGPathDialog extends DialogPanel {
		
		private GPathEditorPanel editor;
		private String gPath = null;
		
		EditGPathDialog(String gPath, String name) {
			super(("Edit GPath '" + name + "'"), true);
			
			//	initialize main buttons
			JButton commitButton = new JButton("OK");
			commitButton.setBorder(BorderFactory.createRaisedBevelBorder());
			commitButton.setPreferredSize(new Dimension(100, 21));
			commitButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					EditGPathDialog.this.gPath = editor.getGPath();
					dispose();
				}
			});
			
			JButton abortButton = new JButton("Cancel");
			abortButton.setBorder(BorderFactory.createRaisedBevelBorder());
			abortButton.setPreferredSize(new Dimension(100, 21));
			abortButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					EditGPathDialog.this.gPath = null;
					dispose();
				}
			});
			
			JPanel mainButtonPanel = new JPanel(new FlowLayout());
			mainButtonPanel.add(commitButton);
			mainButtonPanel.add(abortButton);
			
			//	initialize editor
			this.editor = new GPathEditorPanel(name, gPath, this);
			
			//	put the whole stuff together
			this.setLayout(new BorderLayout());
			this.add(this.editor, BorderLayout.CENTER);
			this.add(mainButtonPanel, BorderLayout.SOUTH);
			
			this.setResizable(true);
			this.setSize(new Dimension(600, 400));
			this.setLocationRelativeTo(DialogPanel.getTopWindow());
		}
		
		boolean isCommitted() {
			return (this.gPath != null);
		}
		
		String getGPath() {
			return this.gPath;
		}
	}

	private class GPathEditorPanel extends JPanel implements FontEditable, DocumentListener {
		
		private static final int MAX_SCROLLBAR_WAIT = 200;
		
		private JTextArea editor;
		private JScrollPane editorBox;
		
		private String content = "";
		
		private String fontName = "Verdana";
		private int fontSize = 12;
		private Color fontColor = Color.BLACK;
		
		private boolean dirty = false;
		private String gPathName;
		
		private DialogPanel frame;
		
		GPathEditorPanel(String name, String gPath, DialogPanel frame) {
			super(new BorderLayout(), true);
			this.frame = frame;
			
			//	initialize editor
			this.editor = new JTextArea();
			this.editor.setEditable(true);
			this.editor.getDocument().addDocumentListener(this);
			
			//	wrap editor in scroll pane
			this.editorBox = new JScrollPane(this.editor);
			
			//	initialize buttons
			JButton refreshButton = new JButton("Refresh");
			refreshButton.setBorder(BorderFactory.createRaisedBevelBorder());
			refreshButton.setPreferredSize(new Dimension(115, 21));
			refreshButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					refreshGPath();
				}
			});
			
			JButton validateButton = new JButton("Validate");
			validateButton.setBorder(BorderFactory.createRaisedBevelBorder());
			validateButton.setPreferredSize(new Dimension(115, 21));
			validateButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					validateGPath();
				}
			});
			
			JButton testButton = new JButton("Test");
			testButton.setBorder(BorderFactory.createRaisedBevelBorder());
			testButton.setPreferredSize(new Dimension(115, 21));
			testButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					testGPath();
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
			this.setContent((gPath == null) ? "" : gPath);
		}
		
		String getGPath() {
			if (this.isDirty()) this.content = GPath.normalizePath(this.editor.getText());
			return this.content;
		}
		
		void setContent(String gPath) {
			this.content = GPath.normalizePath(gPath);
			this.refreshDisplay();
			this.dirty = false;
		}
		
		boolean isDirty() {
			return this.dirty;
		}
		
		void refreshGPath() {
			String gPath = this.editor.getText();
			if ((gPath != null) && (gPath.length() != 0)) {
				
				final JScrollBar scroller = this.editorBox.getVerticalScrollBar();
				final int scrollPosition = scroller.getValue();
				
				String normalizedGPath = GPath.normalizePath(gPath);
				this.editor.getDocument().removeDocumentListener(this);
				this.editor.setText(GPath.explodePath(normalizedGPath, "  "));
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
		}
		
		void validateGPath() {
			boolean selected = true;
			String gPath = this.editor.getSelectedText();
			if ((gPath == null) || (gPath.length() == 0)) {
				gPath = this.editor.getText();
				selected = false;
			}
			String error = GPathParser.validatePath(gPath);
			if (error == null) JOptionPane.showMessageDialog(this, ("The " + (selected ? "selected expression part" : "expression") + " is valid."), "GPath Validation", JOptionPane.INFORMATION_MESSAGE);
			else JOptionPane.showMessageDialog(this, ("The " + (selected ? "selected expression part" : "expression") + " is not valid:\n" + error), "GPath Validation", JOptionPane.ERROR_MESSAGE);
		}
		
		void testGPath() {
			boolean selected = true;
			String gPath = this.editor.getSelectedText();
			if ((gPath == null) || (gPath.length() == 0)) {
				gPath = this.editor.getText();
				selected = false;
			}
			String error = GPathParser.validatePath(gPath);
			if (error != null) JOptionPane.showMessageDialog(this, ("The " + (selected ? "selected expression part" : "expression") + " is not valid:\n" + error), "GPath Validation", JOptionPane.ERROR_MESSAGE);
			else try {
				Annotation[] annotations = testPath(gPath);
				if (annotations != null) {
					AnnotationDisplayDialog add = new AnnotationDisplayDialog(this.frame.getDialog(), "Matches of GPath", annotations, true);
					add.setLocationRelativeTo(this);
					add.setVisible(true);
				}
			}
			catch (GPathException gpe) {
				JOptionPane.showMessageDialog(this, gpe, "GPath Error", JOptionPane.ERROR_MESSAGE);
			}
		}
		
		void refreshDisplay() {
			final JScrollBar scroller = this.editorBox.getVerticalScrollBar();
			final int scrollPosition = scroller.getValue();
			
			this.editor.getDocument().removeDocumentListener(this);
			this.editor.setFont(new Font(this.fontName, Font.PLAIN, this.fontSize));
			this.editor.setText(GPath.explodePath(this.content, "  "));
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
		public void changedUpdate(DocumentEvent de) {
			//	attribute changes are not of interest for now
		}
		
		/*
		 * @see javax.swing.event.DocumentListener#insertUpdate(javax.swing.event.DocumentEvent)
		 */
		public void insertUpdate(DocumentEvent de) {
			this.dirty = true;
		}
		
		/*
		 * @see javax.swing.event.DocumentListener#removeUpdate(javax.swing.event.DocumentEvent)
		 */
		public void removeUpdate(DocumentEvent de) {
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
	
	private void evaluatePath() {
		this.evaluatePath(this.resourceNameList.getSelectedName(), this.parent.getActivePanel());
	}
	
	private void evaluatePath(String pathName, DocumentEditor de) {
		String name = pathName;
		if (name == null) {
			ResourceDialog rd = ResourceDialog.getResourceDialog(this, "Select GPath to Evaluate", "Evaluate");
			rd.setLocationRelativeTo(DialogPanel.getTopWindow());
			rd.setVisible(true);
			name = rd.getSelectedResourceName();
		}
		if (name != null) {
			String rawPath = this.getGPath(name);
			if (rawPath != null) {
				GPath gPath = null;
				try {
					gPath = new GPath(rawPath);
				} catch (Exception e) {}
//				DocumentEditor de = this.parent.getActivePanel();
				if ((gPath != null) && (de != null)) {
					GPathDocumentProcessor gpdp = new GPathDocumentProcessor(this.parent, de, this, gPath, this.gPathEngine);
					Properties parameters = new Properties();
					parameters.setProperty(Resource.INTERACTIVE_PARAMETER, Resource.INTERACTIVE_PARAMETER);
					de.applyDocumentProcessor(gpdp, null, parameters);
				}
			}
		}
	}
	
	private class GPathDocumentProcessor implements DocumentProcessor {
		
		private DocumentEditor target;
		private GPath gPath;
		
		GPathDocumentProcessor(GoldenGATE host, DocumentEditor target, GPathManager parent, GPath gPath, GPathEngine gPathEngine) {
			this.target = target;
			this.gPath = gPath;
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentProcessor#process(de.uka.ipd.idaho.gamta.MutableAnnotation, java.util.Properties)
		 */
		public void process(MutableAnnotation data, Properties parameters) {
			if (parameters.containsKey(INTERACTIVE_PARAMETER)) {
				GPathResultDialog gprd = new GPathResultDialog("Matches of GPath", gPath, this.target, data);
				gprd.setLocationRelativeTo(DialogPanel.getTopWindow());
				gprd.setVisible(true);
				if (gprd.isCommitted()) {
					AnnotationTray[] aTrays = gprd.getAnnotationTrays();
					for (int a = 0; a < aTrays.length; a++) {
						if (aTrays[a].remove) {
							Annotation annotation = data.removeAnnotation(aTrays[a].annotation);
							if (aTrays[a].delete) {
								int start = annotation.getStartIndex();
								int size = annotation.size();
								data.removeTokensAt(start, size);
							}
						}
					}
				}
			}
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentProcessor#process(de.uka.ipd.idaho.gamta.MutableAnnotation)
		 */
		public void process(MutableAnnotation data) {
			Properties parameters = new Properties();
			parameters.setProperty(Resource.INTERACTIVE_PARAMETER, Resource.INTERACTIVE_PARAMETER);
			this.process(data, parameters);
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.Resource#getName()
		 */
		public String getName() {
			return "GPath Annotation Selector";
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.Resource#getProviderClassName()
		 */
		public String getProviderClassName() {
			return GPathManager.class.getName();
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.Resource#getTypeLabel()
		 */
		public String getTypeLabel() {
			return "GPath Annotation Selector";
		}
		
		private class AnnotationTray {
			private Annotation annotation;
			private boolean remove = false;
			private boolean delete = false;
			private JButton editButton = new JButton("Edit");
			
			private AnnotationTray(Annotation annotation) {
				this.annotation = annotation;
			}
		}
		
		private class GPathResultDialog extends DialogPanel {
			
			private GPathResultPanel annotationDisplay;
			private boolean isCommitted = false;
			
			GPathResultDialog(String title, GPath gPath, DocumentEditor target, MutableAnnotation data) {
				super(title, true);
				
				JPanel mainButtonPanel = new JPanel();
				mainButtonPanel.setLayout(new FlowLayout());
				
				//	initialize main buttons
				JButton commitButton = new JButton("Write Selection");
				commitButton.setBorder(BorderFactory.createRaisedBevelBorder());
				commitButton.setPreferredSize(new Dimension(100, 21));
				commitButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						isCommitted = true;
						dispose();
					}
				});
				mainButtonPanel.add(commitButton);
				
				JButton abortButton = new JButton("Cancel");
				abortButton.setBorder(BorderFactory.createRaisedBevelBorder());
				abortButton.setPreferredSize(new Dimension(100, 21));
				abortButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						dispose();
					}
				});
				
				mainButtonPanel.add(abortButton);
				
				//	put the whole stuff together
				this.setLayout(new BorderLayout());
				
				this.annotationDisplay = new GPathResultPanel(gPath, target, data);
				this.add(this.annotationDisplay, BorderLayout.CENTER);
				this.add(mainButtonPanel, BorderLayout.SOUTH);
				
				this.setResizable(true);
				//	TODO: compute size dependent on annotations.length
				this.setSize(new Dimension(500, 500));
			}
			
			/*
			 * @return true if and only if the dialog was committed
			 */
			public boolean isCommitted() {
				return this.isCommitted;
			}
			
			private AnnotationTray[] getAnnotationTrays() {
				return this.annotationDisplay.getAnnotationTrays();
			}
			
			private Dimension partEditDialogSize = new Dimension(800, 600);
			private Point partEditDialogLocation = null;
			
			private class GPathResultPanel extends JPanel {
				
				private JTable annotationTable;
				
				private AnnotationTray[] annotationTrays;
				private HashMap annotationTraysByID = new HashMap();
				
				private DocumentEditor target;
				private MutableAnnotation data;
				
				private GPath gPath;
				
				GPathResultPanel(GPath gPath, DocumentEditor target, MutableAnnotation data) {
					super(new BorderLayout(), true);
					this.setBorder(BorderFactory.createEtchedBorder());
					
					this.gPath = gPath;
					
					this.target = target;
					this.data = data;
					
					this.produceAnnotationTrays();
					
					this.annotationTable = new JTable();
					this.annotationTable.setDefaultRenderer(Object.class, new TooltipAwareComponentRenderer(5, data.getTokenizer()));
					this.annotationTable.addMouseListener(new JTableButtonMouseListener(this.annotationTable));
					this.annotationTable.setModel(new GPathResultTableModel(this.annotationTrays));
					this.annotationTable.getColumnModel().getColumn(0).setMaxWidth(60);
					this.annotationTable.getColumnModel().getColumn(1).setMaxWidth(60);
					this.annotationTable.getColumnModel().getColumn(2).setMaxWidth(120);
					this.annotationTable.getColumnModel().getColumn(3).setMaxWidth(60);
					this.annotationTable.getColumnModel().getColumn(4).setMaxWidth(60);
					this.annotationTable.getColumnModel().getColumn(6).setMaxWidth(60);
					
					JPanel buttonPanel = new JPanel(new GridBagLayout());
					
					JButton removeAllButton = new JButton("Remove All");
					removeAllButton.setBorder(BorderFactory.createRaisedBevelBorder());
					removeAllButton.setPreferredSize(new Dimension(100, 21));
					removeAllButton.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							selectAllForRemove();
						}
					});
					
					JButton removeNoneButton = new JButton("Remove None");
					removeNoneButton.setBorder(BorderFactory.createRaisedBevelBorder());
					removeNoneButton.setPreferredSize(new Dimension(100, 21));
					removeNoneButton.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							selectNoneForRemove();
						}
					});
					
					JButton deleteAllButton = new JButton("Delete All");
					deleteAllButton.setBorder(BorderFactory.createRaisedBevelBorder());
					deleteAllButton.setPreferredSize(new Dimension(100, 21));
					deleteAllButton.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							selectAllForDelete();
						}
					});
					
					JButton deleteNoneButton = new JButton("Delete None");
					deleteNoneButton.setBorder(BorderFactory.createRaisedBevelBorder());
					deleteNoneButton.setPreferredSize(new Dimension(100, 21));
					deleteNoneButton.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							selectNoneForDelete();
						}
					});
					
					GridBagConstraints gbc = new GridBagConstraints();
					gbc.insets.top = 2;
					gbc.insets.bottom = 2;
					gbc.insets.left = 5;
					gbc.insets.right = 5;
					gbc.weighty = 0;
					gbc.weightx = 1;
					gbc.gridheight = 1;
					gbc.gridwidth = 1;
					gbc.fill = GridBagConstraints.HORIZONTAL;
					
					gbc.gridy = 0;
					gbc.gridx = 0;
					buttonPanel.add(removeAllButton, gbc.clone());
					gbc.gridx = 1;
					buttonPanel.add(removeNoneButton, gbc.clone());
					
					gbc.gridy = 1;
					gbc.gridx = 0;
					buttonPanel.add(deleteNoneButton, gbc.clone());
					gbc.gridx = 1;
					buttonPanel.add(deleteAllButton, gbc.clone());
					
					this.add(buttonPanel, BorderLayout.SOUTH);
					
					JScrollPane annotationTableBox = new JScrollPane(this.annotationTable);
					this.add(annotationTableBox, BorderLayout.CENTER);
				}
				
				void produceAnnotationTrays() {
					Annotation[] annotations;
					try {
						annotations = gPathEngine.evaluatePath(this.data, this.gPath, null);
					}
					catch (GPathException gpe) {
						JOptionPane.showMessageDialog(this, gpe.getMessage(), "GPath Error", JOptionPane.ERROR_MESSAGE);
						annotations = new Annotation[0];
					}
					this.annotationTrays = new AnnotationTray[annotations.length];
					for (int a = 0; a < annotations.length; a++) {
						if (this.annotationTraysByID.containsKey(annotations[a].getAnnotationID()))
							this.annotationTrays[a] = ((AnnotationTray) this.annotationTraysByID.get(annotations[a].getAnnotationID()));
						else {
							this.annotationTrays[a] = new AnnotationTray(annotations[a]);
							final Annotation annotation = annotations[a];
							this.annotationTrays[a].editButton.addMouseListener(new MouseAdapter() {
								public void mouseClicked(MouseEvent e) {
									editAnnotation(annotation);
								}
							});
							this.annotationTraysByID.put(annotation.getAnnotationID(), this.annotationTrays[a]);
						}
					}
				}
				
				void editAnnotation(Annotation annotation) {
					MutableAnnotation editAnnotation;
					if (annotation instanceof MutableAnnotation) editAnnotation = ((MutableAnnotation) annotation);
					else editAnnotation = data.addAnnotation(AnnotationEditorPanel.TEMP_ANNOTATION_TYPE, annotation.getStartIndex(), annotation.size());
					
					//	create dialog
					DocumentEditDialog ded = new DocumentEditDialog(GPathResultDialog.this.getDialog(), target, "Edit Annotation", editAnnotation) {
						public void dispose() {
							partEditDialogSize = this.getSize();
							partEditDialogLocation = this.getLocation(partEditDialogLocation);
							super.dispose();
						}
					};
					
					//	position and show dialog
					ded.setSize(partEditDialogSize);
					if (partEditDialogLocation == null) ded.setLocationRelativeTo(GPathResultDialog.this);
					else ded.setLocation(partEditDialogLocation);
					ded.setVisible(true);
					
					//	finish
					if (!(annotation instanceof MutableAnnotation)) data.removeAnnotation(editAnnotation);
					if (ded.isContentModified()) {
						this.produceAnnotationTrays();
						this.annotationTable.setModel(new GPathResultTableModel(this.annotationTrays));
						this.annotationTable.getColumnModel().getColumn(0).setMaxWidth(60);
						this.annotationTable.getColumnModel().getColumn(1).setMaxWidth(60);
						this.annotationTable.getColumnModel().getColumn(2).setMaxWidth(120);
						this.annotationTable.getColumnModel().getColumn(3).setMaxWidth(60);
						this.annotationTable.getColumnModel().getColumn(4).setMaxWidth(60);
						this.annotationTable.getColumnModel().getColumn(6).setMaxWidth(60);
						this.annotationTable.repaint();
						this.validate();
					}
				}
				
				private class DocumentEditDialog extends DocumentEditorDialog {
					DocumentEditDialog(JDialog owner, DocumentEditor parent, String title, MutableAnnotation docPart) {
						super(owner, GPathManager.this.parent, parent, title, docPart);
						
						JButton okButton = new JButton("OK");
						okButton.setBorder(BorderFactory.createRaisedBevelBorder());
						okButton.setPreferredSize(new Dimension(100, 21));
						okButton.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent e) {
								DocumentEditDialog.this.documentEditor.writeChanges();
								DocumentEditDialog.this.dispose();
							}
						});
						this.mainButtonPanel.add(okButton);
						
						JButton cancelButton = new JButton("Cancel");
						cancelButton.setBorder(BorderFactory.createRaisedBevelBorder());
						cancelButton.setPreferredSize(new Dimension(100, 21));
						cancelButton.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent e) {
								DocumentEditDialog.this.dispose();
							}
						});
						this.mainButtonPanel.add(cancelButton);
					}
				}
				
				void selectAllForRemove() {
					for (int a = 0; a < this.annotationTrays.length; a++)
						this.annotationTrays[a].remove = true;
					this.annotationTable.repaint();
					this.validate();
				}
				
				void selectNoneForRemove() {
					for (int a = 0; a < this.annotationTrays.length; a++) {
						this.annotationTrays[a].remove = false;
						this.annotationTrays[a].delete = false;
					}
					this.annotationTable.repaint();
					this.validate();
				}
				
				void selectAllForDelete() {
					for (int a = 0; a < this.annotationTrays.length; a++) {
						this.annotationTrays[a].remove = true;
						this.annotationTrays[a].delete = true;
					}
					this.annotationTable.repaint();
					this.validate();
				}
				
				void selectNoneForDelete() {
					for (int a = 0; a < this.annotationTrays.length; a++)
						this.annotationTrays[a].delete = false;
					this.annotationTable.repaint();
					this.validate();
				}
				
				AnnotationTray[] getAnnotationTrays() {
					return this.annotationTrays;
				}
			}
			
			private class GPathResultTableModel implements TableModel {
				private AnnotationTray[] annotations;
				
				GPathResultTableModel(AnnotationTray[] annotations) {
					this.annotations = annotations;
				}
				
				/*
				 * @see javax.swing.table.TableModel#addTableModelListener(javax.swing.event.TableModelListener)
				 */
				public void addTableModelListener(TableModelListener l) {}
				
				/*
				 * @see javax.swing.table.TableModel#getColumnClass(int)
				 */
				public Class getColumnClass(int columnIndex) {
					if (columnIndex == 0) return Boolean.class;
					else if (columnIndex == 1) return Boolean.class;
					else if (columnIndex == 6) return JButton.class;
					else return String.class;
				}
				
				/*
				 * @see javax.swing.table.TableModel#getColumnCount()
				 */
				public int getColumnCount() {
					return 7;
				}
				
				/*
				 * @see javax.swing.table.TableModel#getColumnName(int)
				 */
				public String getColumnName(int columnIndex) {
					if (columnIndex == 0) return "Remove";
					if (columnIndex == 1) return "Delete";
					if (columnIndex == 2) return "Type";
					if (columnIndex == 3) return "Start";
					if (columnIndex == 4) return "Size";
					if (columnIndex == 5) return "Value";
					if (columnIndex == 6) return "Edit";
					return null;
				}
				
				/*
				 * @see javax.swing.table.TableModel#getRowCount()
				 */
				public int getRowCount() {
					return this.annotations.length;
				}
				
				/*
				 * @see javax.swing.table.TableModel#getValueAt(int, int)
				 */
				public Object getValueAt(int rowIndex, int columnIndex) {
					if (columnIndex == 0) return new Boolean(this.annotations[rowIndex].remove);
					if (columnIndex == 1) return new Boolean(this.annotations[rowIndex].delete);
					Annotation a = this.annotations[rowIndex].annotation;
					if (columnIndex == 2) return a.getType();
					if (columnIndex == 3) return "" + a.getStartIndex();
					if (columnIndex == 4) return "" + a.size();
					if (columnIndex == 5) return a.getValue();
					if (columnIndex == 6) return this.annotations[rowIndex].editButton;
					return null;
				}
				
				/*
				 * @see javax.swing.table.TableModel#isCellEditable(int, int)
				 */
				public boolean isCellEditable(int rowIndex, int columnIndex) {
					return (((columnIndex == 0) && !this.annotations[rowIndex].delete) || ((columnIndex == 1) && this.annotations[rowIndex].remove));
				}
				
				/*
				 * @see javax.swing.table.TableModel#removeTableModelListener(javax.swing.event.TableModelListener)
				 */
				public void removeTableModelListener(TableModelListener l) {}
				
				/*
				 * @see javax.swing.table.TableModel#setValueAt(java.lang.Object, int, int)
				 */
				public void setValueAt(Object newValue, int rowIndex, int columnIndex) {
					if (columnIndex == 0) this.annotations[rowIndex].remove = ((Boolean) newValue).booleanValue();
					if (columnIndex == 1) this.annotations[rowIndex].delete = ((Boolean) newValue).booleanValue();
				}
			}

			private class TooltipAwareComponentRenderer extends DefaultTableCellRenderer {
				private HashSet tooltipColumns = new HashSet();
				private Tokenizer tokenizer;
				 TooltipAwareComponentRenderer(int tooltipColumn, Tokenizer tokenizer) {
					this.tooltipColumns.add("" + tooltipColumn);
					this.tokenizer = tokenizer;
				}
//				TooltipAwareComponentRenderer(int[] tooltipColumns) {
//					for (int c = 0; c < tooltipColumns.length; c++)
//						this.tooltipColumns.add("" + tooltipColumns[c]);
//				}
				public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
					JComponent component = ((value instanceof JComponent) ? ((JComponent) value) : (JComponent) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column));
					if (this.tooltipColumns.contains("" + row) && (component instanceof JComponent))
						((JComponent) component).setToolTipText(this.produceTooltipText(new PlainTokenSequence(value.toString(), this.tokenizer)));
					return component;
				}
				private String produceTooltipText(TokenSequence tokens) {
					if (tokens.length() < 100) return TokenSequenceUtils.concatTokens(tokens);
					
					StringVector lines = new StringVector();
					int startToken = 0;
					int lineLength = 0;
					Token lastToken = null;
					
					for (int t = 0; t < tokens.size(); t++) {
						Token token = tokens.tokenAt(t);
						lineLength += token.length();
						if (lineLength > 100) {
							lines.addElement(TokenSequenceUtils.concatTokens(tokens, startToken, (t - startToken + 1)));
							startToken = (t + 1);
							lineLength = 0;
						} else if (Gamta.insertSpace(lastToken, token)) lineLength++;
					}
					if (startToken < tokens.size())
						lines.addElement(TokenSequenceUtils.concatTokens(tokens, startToken, (tokens.size() - startToken)));
					
					return ("<HTML>" + lines.concatStrings("<BR>") + "</HTML>");
				}
			}

			private class JTableButtonMouseListener implements MouseListener {
				private JTable table;
				void forwardEventToButton(MouseEvent e) {
					TableColumnModel columnModel = this.table.getColumnModel();
					int column = columnModel.getColumnIndexAtX(e.getX());
					int row = e.getY() / this.table.getRowHeight();
					if (row >= this.table.getRowCount() || row < 0 || column >= this.table.getColumnCount() || column < 0) return;
					Object value = this.table.getValueAt(row, column);
					if (value instanceof JButton) {
						JButton button = ((JButton) value);
						MouseEvent buttonEvent = SwingUtilities.convertMouseEvent(this.table, e, button);
						button.dispatchEvent(buttonEvent);
						this.table.repaint();
					}
				}
				private JTableButtonMouseListener(JTable table) {
					this.table = table;
				}
				public void mouseClicked(MouseEvent e) {
					this.forwardEventToButton(e);
				}
				public void mouseEntered(MouseEvent e) {
					this.forwardEventToButton(e);
				}
				public void mouseExited(MouseEvent e) {
					this.forwardEventToButton(e);
				}
				public void mousePressed(MouseEvent e) {
					this.forwardEventToButton(e);
				}
				public void mouseReleased(MouseEvent e) {
					this.forwardEventToButton(e);
				}
			}
		}
	}
	
	private void showCustomFunctions() {
		final DialogPanel functionDialog = new DialogPanel("Custom GPath Functions", true);
		functionDialog.setLayout(new BorderLayout());
		
		JList functionList = new JList(this.customFunctionNames);
		JScrollPane functionListBox = new JScrollPane(functionList);
		functionListBox.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		functionListBox.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		functionDialog.add(functionListBox, BorderLayout.CENTER);
		
		JButton closeButton = new JButton("Close");
		closeButton.setBorder(BorderFactory.createRaisedBevelBorder());
		closeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				functionDialog.dispose();
			}
		});
		functionDialog.add(closeButton, BorderLayout.SOUTH);
		
		functionDialog.setSize(300, 500);
		functionDialog.setLocationRelativeTo(DialogPanel.getTopWindow());
		functionDialog.setVisible(true);
	}
}
