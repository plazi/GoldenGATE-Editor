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
package de.uka.ipd.idaho.goldenGate.plugin.lists;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import de.uka.ipd.idaho.easyIO.settings.Settings;
import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.DocumentRoot;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.TokenSequenceUtils;
import de.uka.ipd.idaho.goldenGate.plugins.AbstractAnnotationSourceManager;
import de.uka.ipd.idaho.goldenGate.plugins.AnnotationSource;
import de.uka.ipd.idaho.goldenGate.plugins.AnnotationSourceParameterPanel;
import de.uka.ipd.idaho.goldenGate.util.DataListListener;
import de.uka.ipd.idaho.goldenGate.util.DialogPanel;
import de.uka.ipd.idaho.goldenGate.util.EditFontsButton;
import de.uka.ipd.idaho.goldenGate.util.FontEditable;
import de.uka.ipd.idaho.goldenGate.util.FontEditorDialog;
import de.uka.ipd.idaho.goldenGate.util.FontEditorPanel;
import de.uka.ipd.idaho.goldenGate.util.ResourceDialog;
import de.uka.ipd.idaho.stringUtils.StringUtils;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * Annotation source manager for gazetteer lists. Lists are useful for
 * extracting or annotating words and phrases in document text, e.g. country
 * names, chemical elements, or stop words.<br>
 * <br>
 * All configuration can be done in the 'Edit Lists' dialog in the GoldenGATE
 * Editor. This includes combining lists with union, intersection, and
 * subtraction. In addition, there are functions for creating lists from the
 * annotations of a given document, and for adding the annotations of a given
 * document to an existing list.
 * 
 * @author sautter
 */
public class ListManager extends AbstractAnnotationSourceManager {
	
	private static final String FILE_EXTENSION = ".list";
	
	private static final String[] FIX_LIST_NAMES = {"<Months>", "<Noise Words>", "<Person Titles>"};
	private static final StringVector[] FIX_LISTS = {StringUtils.getMonthNames(), StringUtils.getNoiseWords(), StringUtils.getTitles()};
	
	private JFileChooser fileChooser = null;
	
	public ListManager() {}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager#getFileExtension()
	 */
	protected String getFileExtension() {
		return FILE_EXTENSION;
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#getMainMenuItems()
	 */
	public JMenuItem[] getMainMenuItems() {
		if (!this.dataProvider.isDataEditable())
			return new JMenuItem[0];
		
		ArrayList collector = new ArrayList();
		JMenuItem mi;
		
		mi = new JMenuItem("Create");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				createList();
			}
		});
		collector.add(mi);
		mi = new JMenuItem("Load");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				loadList();
			}
		});
		collector.add(mi);
		mi = new JMenuItem("Load URL");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				loadListFromURL();
			}
		});
		collector.add(mi);
		mi = new JMenuItem("Extract");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				extractList();
			}
		});
		collector.add(mi);
		mi = new JMenuItem("Extend");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				extendList();
			}
		});
		collector.add(mi);
		mi = new JMenuItem("Edit");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				editLists();
			}
		});
		collector.add(mi);
		return ((JMenuItem[]) collector.toArray(new JMenuItem[collector.size()]));
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AnnotationSourceManager#getMainMenuTitle()
	 */
	public String getMainMenuTitle() {
		return "Lists";
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager#getToolsMenuLabel()
	 */
	public String getToolsMenuLabel() {
		return "Apply";
	}
	
	/* 
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AnnotationSourceManager#createAnnotationSource()
	 */
	public String createAnnotationSource() {
		return this.createList(null, null);
	}
	
	/* 
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AnnotationSourceManager#editAnnotationSource(java.lang.String)
	 */
	public void editAnnotationSource(String name) {
		StringVector list = this.loadListResource(name);
		if ((list == null) || list.isEmpty()) return;
		
		EditListDialog eld = new EditListDialog(name, list);
		eld.setVisible(true);
		
		if (eld.isCommitted()) try {
			this.storeListResource(name, eld.getList());
		} catch (IOException ioe) {}
	}
	
	/* 
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AnnotationSourceManager#editAnnotationSources()
	 */
	public void editAnnotationSources() {
		this.editLists();
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AnnotationSourceManager#getAnnotationSource(java.lang.String)
	 */
	public AnnotationSource getAnnotationSource(String name) {
		StringVector list = this.getList(name);
		return ((list == null) ? null : new ListAnnotationSource(name, list));
	}
	
	private class ListAnnotationSource implements AnnotationSource {
		
		private static final String ALLOW_OVERLAP_ATTRIBUTE_NAME = "ALLOW_OVERLAP";
		private static final String CASE_SENSITIVE_ATTRIBUTE_NAME = "CASE_SENSITIVE";
		
		private String name;
		private StringVector list;
//		private int maxTokens;
		
		ListAnnotationSource(String name, StringVector list) {
			this.name = name;
			this.list = list;
			
//			int maxWhitespace = 0;
//			for (int e = 0; e < list.size(); e++) {
//				String element = list.get(e);
//				int elementWhitespace = 0;
//				boolean lastWasWhitespace = true;
//				for (int c = 0; c < element.length(); c++) {
//					char ch = element.charAt(c);
//					if (ch < 33) {
//						if (!lastWasWhitespace) elementWhitespace ++;
//						lastWasWhitespace = true;
//					} else lastWasWhitespace = false;
//				}
//				if (elementWhitespace > maxWhitespace) maxWhitespace = elementWhitespace;
//			}
//			this.maxTokens = maxWhitespace + 1;
//
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
			return "List";
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.AnnotationSource#getProviderClassName()
		 */
		public String getProviderClassName() {
			return ListManager.class.getName();
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.AnnotationSource#process(de.uka.ipd.idaho.gamta.MutableAnnotation, boolean)
		 */
		public Annotation[] annotate(MutableAnnotation data) {
			return this.annotate(data, null);
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.AnnotationSource#annotate(de.uka.ipd.idaho.gamta.MutableAnnotation, java.util.Properties)
		 */
		public Annotation[] annotate(MutableAnnotation data, Properties parameters) {
			if (parameters == null) return Gamta.extractAllContained(data, this.list, 0);
			else {
				boolean allowOverlap = TRUE.equals(parameters.getProperty(ALLOW_OVERLAP_ATTRIBUTE_NAME, FALSE));
				boolean caseSensitive = TRUE.equals(parameters.getProperty(CASE_SENSITIVE_ATTRIBUTE_NAME, FALSE));
				return Gamta.extractAllContained(data, this.list, 0, caseSensitive, allowOverlap);
			}
//			if (parameters == null) return Gamta.extractAllContained(data, this.list, this.maxTokens);
//			else {
//				boolean allowOverlap = TRUE.equals(parameters.getProperty(ALLOW_OVERLAP_ATTRIBUTE_NAME, FALSE));
//				boolean caseSensitive = TRUE.equals(parameters.getProperty(CASE_SENSITIVE_ATTRIBUTE_NAME, FALSE));
//				return Gamta.extractAllContained(data, this.list, this.maxTokens, caseSensitive, allowOverlap);
//			}
		}
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AnnotationSourceManager#getAnnotatorParameterPanel()
	 */
	public AnnotationSourceParameterPanel getAnnotatorParameterPanel() {
		return new ListParameterPanel();
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AnnotationSourceManager#getAnnotatorParameterPanel(de.uka.ipd.idaho.easyIO.settings.Settings)
	 */
	public AnnotationSourceParameterPanel getAnnotatorParameterPanel(Settings settings) {
		return new ListParameterPanel(settings);
	}
	
	private class ListParameterPanel extends AnnotationSourceParameterPanel {
		
		private JCheckBox caseSensitive;
		private JCheckBox allowOverlap;
		private boolean dirty = false;
		
		ListParameterPanel() {
			this(null);
		}
		
		ListParameterPanel(Settings settings) {
			super(new GridBagLayout());
			
			this.caseSensitive = new JCheckBox("Case Sensitive", true);
			if (settings != null) this.caseSensitive.setSelected(ListAnnotationSource.TRUE.equals(settings.getSetting(ListAnnotationSource.CASE_SENSITIVE_ATTRIBUTE_NAME, ListAnnotationSource.FALSE)));
			this.caseSensitive.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent ie) {
					dirty = true;
				}
			});
			
			this.allowOverlap = new JCheckBox("Overlapping Matches", false);
			if (settings != null) this.allowOverlap.setSelected(ListAnnotationSource.TRUE.equals(settings.getSetting(ListAnnotationSource.ALLOW_OVERLAP_ATTRIBUTE_NAME, ListAnnotationSource.FALSE)));
			this.allowOverlap.addItemListener(new ItemListener() {
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
			this.add(this.caseSensitive, gbc.clone());
			
			gbc.gridx = 1;
			this.add(this.allowOverlap, gbc.clone());
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.AnnotationSourceParameterPanel#isDirty()
		 */
		public boolean isDirty() {
			return this.dirty;
		}
		
		/*
		 * @see de.uka.ipd.idaho.goldenGate.plugins.AnnotationSourceParameterPanel#getSettings()
		 */
		public Settings getSettings() {
			Settings set = new Settings();
			set.setSetting(ListAnnotationSource.CASE_SENSITIVE_ATTRIBUTE_NAME, (this.caseSensitive.isSelected() ? ListAnnotationSource.TRUE : ListAnnotationSource.FALSE));
			set.setSetting(ListAnnotationSource.ALLOW_OVERLAP_ATTRIBUTE_NAME, (this.allowOverlap.isSelected() ? ListAnnotationSource.TRUE: ListAnnotationSource.FALSE));
			return set;
		}
	}

	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AnnotationSourceManager#getResourceNames()
	 */
	public String[] getResourceNames() {
		StringVector lists = new StringVector();
		lists.addContentIgnoreDuplicates(FIX_LIST_NAMES);
		lists.addContentIgnoreDuplicates(super.getResourceNames());
		return lists.toStringArray();
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.ResourceManager#getResourceTypeLabel()
	 */
	public String getResourceTypeLabel() {
		return "List";
	}
	
	/**
	 * Retrieve a plain list by its name
	 * @param name the name of the reqired list
	 * @return the list with the required name, or null, if there is no such
	 *         list
	 */
	public StringVector getList(String name) {
		if (name == null) return null;
		
		for (int l = 0; l < FIX_LIST_NAMES.length; l++) 
			if (name.equals(FIX_LIST_NAMES[l])) return FIX_LISTS[l];
		
		return this.loadListResource(name);
	}
	
	private boolean createList() {
		return (this.createList(null, null) != null);
	}
	
	private boolean loadList() {
		if (this.fileChooser == null) try {
			this.fileChooser = new JFileChooser();
		} catch (SecurityException se) {}
		
		if ((this.fileChooser != null) && (this.fileChooser.showOpenDialog(DialogPanel.getTopWindow()) == JFileChooser.APPROVE_OPTION)) {
			File file = this.fileChooser.getSelectedFile();
			if ((file != null) && file.isFile()) {
				try {
					String fileName = file.toString();
					fileName = fileName.substring(fileName.lastIndexOf(File.separatorChar) + 1);
					return this.loadList(new FileInputStream(file), fileName);
				}
				catch (IOException ioe) {
					JOptionPane.showMessageDialog(DialogPanel.getTopWindow(), (ioe.getClass().getName() + " (" + ioe.getMessage() + ")\nwhile trying to load " + file.toString()), "Could Not Read File", JOptionPane.ERROR_MESSAGE);
				}
			}
		}
		return false;
	}
	
	private boolean loadListFromURL() {
		if (!this.dataProvider.allowWebAccess() && (JOptionPane.showConfirmDialog(null, "You are working in offline mode, allow loading list from URL anyway?", "Allow Web Access", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) != JOptionPane.YES_OPTION))
			return false;
		
		Object o = JOptionPane.showInputDialog(DialogPanel.getTopWindow(), "Please enter URL to load", "Enter URL", JOptionPane.QUESTION_MESSAGE, null, null, "http://");
		if ((o != null) && (o instanceof String)) {
			try {
//				URL url = new URL(o.toString());
				URL url = this.dataProvider.getURL(o.toString());
				String fileName = StringUtils.replaceAll(url.getHost() + url.getPath(), "/", "_");
				return this.loadList(url.openStream(), fileName);
			}
			catch (IOException ioe) {
				JOptionPane.showMessageDialog(DialogPanel.getTopWindow(), (ioe.getClass().getName() + " (" + ioe.getMessage() + ")\nwhile trying to load " + o.toString()), "Could Not Load URL", JOptionPane.ERROR_MESSAGE);
			}
		}
		return false;
	}
	
	private boolean loadList(InputStream source, String name) {
		StringVector list = new StringVector();
		if (source != null) list = this.readList(source);
		return (this.createList(list, name) != null);
	}
	
	private StringVector readList(InputStream source) {
		try {
			InputStreamReader reader = new InputStreamReader(source);
			StringVector list = StringVector.loadList(reader);
			reader.close();
			return list;
		}
		catch (IOException ioe) {
			return null;
		}
	}
	
	private boolean extractList() {
		QueriableAnnotation data = this.parent.getActiveDocument();
		if (data != null) {
			
			//	select Annotation type and mode
			ExtractListDialog eld = new ExtractListDialog(data);
			eld.setVisible(true);
			
			//	create list
			if (eld.isCommitted()) {
				String type = eld.getType();
				StringVector list = this.extractData(type, eld.useTokens(), eld.getAttribute(), eld.caseSensitive());
				if (list != null) {
					String listName = ("New " + ((type == null) ? "" : StringUtils.capitalize(type)) + "List");
					list.sortLexicographically(false, eld.caseSensitive());
					return (this.createList(list, listName) != null);
				}
				else return false;
			}
			else return false;
		}
		else return false;
	}
	
	private void extendList() {
		this.extendList(null);
	}
	
	private void extendList(String listName) {
		QueriableAnnotation data = this.parent.getActiveDocument();
		if (data != null) {
			
			//	select Annotation type and mode
			ExtractListDialog eld = new ExtractListDialog(data);
			eld.setVisible(true);
			
			//	create list
			if (eld.isCommitted()) {
				String type = eld.getType();
				StringVector extensionList = this.extractData(type, eld.useTokens(), eld.getAttribute(), eld.caseSensitive());
				if (extensionList != null) {
					
					if (listName == null) {
						ResourceDialog rd = ResourceDialog.getResourceDialog(this, "Select List To Extend", "Extend List");
						rd.setLocationRelativeTo(DialogPanel.getTopWindow());
						rd.setVisible(true);
						if (rd.isCommitted())
							listName = rd.getSelectedResourceName();
					}
					
					if (listName != null) {
						StringVector list = this.getList(listName);
						int originalSize = list.size();
						list.addContentIgnoreDuplicates(extensionList, eld.caseSensitive());
						if (list.size() > originalSize) try {
							this.storeListResource(listName, list);
						} catch (IOException ioe) {}
					}
				}
			}
		}
	}
	
	private StringVector extractData(String type, boolean useTokens, String attributeName, boolean caseSensitive) {
		QueriableAnnotation data = this.parent.getActiveDocument();
		if (data != null) {
			StringVector list = new StringVector(caseSensitive);
			Annotation[] annotations = data.getAnnotations(type);
			
			for (int a = 0; a < annotations.length; a++) {
				if (!DocumentRoot.DOCUMENT_TYPE.equals(annotations[a].getType())) {
					if (attributeName == null) {
						String[] attributeNames = annotations[a].getAttributeNames();
						for (int n = 0; n < attributeNames.length; n++) {
							Object o = annotations[a].getAttribute(attributeNames[n]);
							if ((o != null) && (o instanceof String)) {
								if (useTokens) list.addContentIgnoreDuplicates(TokenSequenceUtils.getTextTokens(data.getTokenizer().tokenize(o.toString())));
								else list.addElementIgnoreDuplicates(o.toString());
							}
						}
					}
					else if (Annotation.ANNOTATION_VALUE_ATTRIBUTE.equals(attributeName)) {
						if (useTokens) list.addContentIgnoreDuplicates(TokenSequenceUtils.getTextTokens(annotations[a]));
						else list.addElementIgnoreDuplicates(annotations[a].getValue());
					}
					else {
						Object o = annotations[a].getAttribute(attributeName);
						if ((o != null) && (o instanceof String)) {
							if (useTokens) list.addContentIgnoreDuplicates(TokenSequenceUtils.getTextTokens(data.getTokenizer().tokenize(o.toString())));
							else list.addElementIgnoreDuplicates(o.toString());
						}
					}
				}
			}
			return list;
		}
		else return null;
	}
	
	private boolean cloneList() {
		String selectedName = this.resourceNameList.getSelectedName();
		if (selectedName == null)
			return this.createList();
		else {
			String name = "New " + selectedName;
			StringVector list = new StringVector();
			StringVector model = this.loadListResource(selectedName);
			if (model != null) list.addContent(model);
			return (this.createList(list, name) != null);
		}
	}
	
	private String createList(StringVector list, String name) {
		CreateListDialog cld = new CreateListDialog(name, list);
		cld.setVisible(true);
		
		if (cld.isCommitted()) {
			StringVector newList = cld.getList();
			String listName = cld.getListName();
			if (!listName.endsWith(FILE_EXTENSION)) listName += FILE_EXTENSION;
			try {
				if (this.storeListResource(listName, newList)) {
					this.parent.notifyResourcesChanged(this.getClass().getName());
					this.resourceNameList.refresh();
					return listName;
				}
			} catch (IOException e) {}
		}
		return null;
	}
	
	private void editLists() {
		final ListEditorPanel[] editor = new ListEditorPanel[1];
		editor[0] = null;
		
		final DialogPanel editDialog = new DialogPanel("Edit Lists", true);
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
						storeListResource(editor[0].listName, editor[0].getContent());
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
				createList();
			}
		});
		editButtons.add(button);
		
		button = new JButton("Clone");
		button.setBorder(BorderFactory.createRaisedBevelBorder());
		button.setPreferredSize(new Dimension(80, 21));
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				cloneList();
			}
		});
		editButtons.add(button);
		
		button = new JButton("Load");
		button.setBorder(BorderFactory.createRaisedBevelBorder());
		button.setPreferredSize(new Dimension(80, 21));
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				loadList();
			}
		});
		editButtons.add(button);
		
		button = new JButton("Load URL");
		button.setBorder(BorderFactory.createRaisedBevelBorder());
		button.setPreferredSize(new Dimension(80, 21));
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				loadListFromURL();
			}
		});
		editButtons.add(button);
		
		button = new JButton("Extract");
		button.setBorder(BorderFactory.createRaisedBevelBorder());
		button.setPreferredSize(new Dimension(80, 21));
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				extractList();
			}
		});
		editButtons.add(button);
		
		button = new JButton("Extend");
		button.setBorder(BorderFactory.createRaisedBevelBorder());
		button.setPreferredSize(new Dimension(80, 21));
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				extendList(resourceNameList.getSelectedName());
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
		
		editDialog.add(editButtons, BorderLayout.NORTH);
		
		final JPanel editorPanel = new JPanel(new BorderLayout());
		String selectedName = this.resourceNameList.getSelectedName();
		if (selectedName == null)
			editorPanel.add(this.getExplanationLabel(), BorderLayout.CENTER);
		else {
			StringVector list = this.loadListResource(selectedName);
			if (list == null)
				editorPanel.add(this.getExplanationLabel(), BorderLayout.CENTER);
			else {
				editor[0] = new ListEditorPanel(selectedName, list);
				editorPanel.add(editor[0], BorderLayout.CENTER);
			}
		}
		editDialog.add(editorPanel, BorderLayout.CENTER);
		
		editDialog.add(this.resourceNameList, BorderLayout.EAST);
		DataListListener dll = new DataListListener() {
			public void selected(String dataName) {
				if ((editor[0] != null) && editor[0].isDirty()) {
					try {
						storeListResource(editor[0].listName, editor[0].getContent());
					}
					catch (IOException ioe) {
						if (JOptionPane.showConfirmDialog(editDialog, (ioe.getClass().getName() + " (" + ioe.getMessage() + ")\nwhile saving file to " + editor[0].listName + "\nProceed?"), "Could Not Save Analyzer", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
							resourceNameList.setSelectedName(editor[0].listName);
							editorPanel.validate();
							return;
						}
					}
				}
				editorPanel.removeAll();
				
				if (dataName == null)
					editorPanel.add(getExplanationLabel(), BorderLayout.CENTER);
				else {
					StringVector list = loadListResource(dataName);
					if (list == null)
						editorPanel.add(getExplanationLabel(), BorderLayout.CENTER);
					else {
						editor[0] = new ListEditorPanel(dataName, list);
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
	
	private class CreateListDialog extends DialogPanel {
		
		private JTextField nameField;
		
		private ListEditorPanel editor;
		private StringVector list = null;
		private String listName = null;
		
		CreateListDialog(String name, StringVector list) {
			super("Create List", true);
			
			this.nameField = new JTextField((name == null) ? "New List" : name);
			this.nameField.setBorder(BorderFactory.createLoweredBevelBorder());
			
			//	initialize main buttons
			JButton commitButton = new JButton("Create");
			commitButton.setBorder(BorderFactory.createRaisedBevelBorder());
			commitButton.setPreferredSize(new Dimension(100, 21));
			commitButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					CreateListDialog.this.list = editor.getContent();
					listName = nameField.getText();
					dispose();
				}
			});
			
			JButton abortButton = new JButton("Cancel");
			abortButton.setBorder(BorderFactory.createRaisedBevelBorder());
			abortButton.setPreferredSize(new Dimension(100, 21));
			abortButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					CreateListDialog.this.list = null;
					listName = null;
					dispose();
				}
			});
			
			JPanel mainButtonPanel = new JPanel();
			mainButtonPanel.setLayout(new FlowLayout());
			mainButtonPanel.add(commitButton);
			mainButtonPanel.add(abortButton);
			
			//	initialize editor
			this.editor = new ListEditorPanel(name, list);
			
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
			return (this.list != null);
		}
		
		StringVector getList() {
			return this.list;
		}
		
		String getListName() {
			return this.listName;
		}
	}
	
	private class EditListDialog extends DialogPanel {
		
		private ListEditorPanel editor;
		private StringVector list = null;
		
		EditListDialog(String name, StringVector list) {
			super(("Edit List '" + name + "'"), true);
			
			//	initialize main buttons
			JButton commitButton = new JButton("OK");
			commitButton.setBorder(BorderFactory.createRaisedBevelBorder());
			commitButton.setPreferredSize(new Dimension(100, 21));
			commitButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					EditListDialog.this.list = editor.getContent();
					dispose();
				}
			});
			
			JButton abortButton = new JButton("Cancel");
			abortButton.setBorder(BorderFactory.createRaisedBevelBorder());
			abortButton.setPreferredSize(new Dimension(100, 21));
			abortButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					EditListDialog.this.list = null;
					dispose();
				}
			});
			
			JPanel mainButtonPanel = new JPanel();
			mainButtonPanel.setLayout(new FlowLayout());
			mainButtonPanel.add(commitButton);
			mainButtonPanel.add(abortButton);
			
			//	initialize editor
			this.editor = new ListEditorPanel(name, list);
			
			//	put the whole stuff together
			this.setLayout(new BorderLayout());
			this.add(this.editor, BorderLayout.CENTER);
			this.add(mainButtonPanel, BorderLayout.SOUTH);
			
			this.setResizable(true);
			this.setSize(new Dimension(600, 400));
			this.setLocationRelativeTo(DialogPanel.getTopWindow());
		}
		
		boolean isCommitted() {
			return (this.list != null);
		}
		
		StringVector getList() {
			return this.list;
		}
	}

	private class ListEditorPanel extends JPanel implements FontEditable, DocumentListener {
		
		private JTextArea editor;
		private JScrollPane editorBox;
		
		private StringVector content = new StringVector();
		
		private String fontName = "Verdana";
		private int fontSize = 12;
		private Color fontColor = Color.BLACK;
		
		private boolean dirty = false;
		private String listName;
		
		ListEditorPanel(String name, StringVector list) {
			super(new BorderLayout(), true);
			this.listName = name;
			
			//	initialize editor
			this.editor = new JTextArea();
			this.editor.setEditable(true);
			
			//	wrap editor in scroll pane
			this.editorBox = new JScrollPane(this.editor);
			
			//	initialize buttons
			JButton unionButton = new JButton("Union");
			unionButton.setBorder(BorderFactory.createRaisedBevelBorder());
			unionButton.setPreferredSize(new Dimension(70, 21));
			unionButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					unionList();
				}
			});
			
			JButton intersectButton = new JButton("Intersect");
			intersectButton.setBorder(BorderFactory.createRaisedBevelBorder());
			intersectButton.setPreferredSize(new Dimension(70, 21));
			intersectButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					intersectList();
				}
			});
			
			JButton subtractButton = new JButton("Subtract");
			subtractButton.setBorder(BorderFactory.createRaisedBevelBorder());
			subtractButton.setPreferredSize(new Dimension(70, 21));
			subtractButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					subtractList();
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
			gbc.gridx = 1;
			buttonPanel.add(unionButton, gbc.clone());
			gbc.gridx = 2;
			buttonPanel.add(intersectButton, gbc.clone());
			gbc.gridx = 3;
			buttonPanel.add(subtractButton, gbc.clone());
			
			
			//	put the whole stuff together
			this.add(this.editorBox, BorderLayout.CENTER);
			this.add(buttonPanel, BorderLayout.SOUTH);
			this.setContent((list == null) ? new StringVector() : list);
		}
		
		StringVector getContent() {
			if (this.isDirty()) {
				this.content.clear();
				this.content.parseAndAddElements(this.editor.getText(), "\n");
			}
			return this.content;
		}
		
		void setContent(StringVector list) {
			this.content = list;
			this.refreshDisplay();
			this.dirty = false;
		}
		
		boolean isDirty() {
			return this.dirty;
		}
		
		void refreshDisplay() {
			this.editor.setFont(new Font(this.fontName, Font.PLAIN, this.fontSize));
			this.editor.setText(this.content.concatStrings("\n"));
			this.editor.getDocument().addDocumentListener(this);
		}
		
		void unionList() {
			StringVector list = this.getContent();
			CaseSensitivePanel csp = new CaseSensitivePanel();
			ResourceDialog rd = ResourceDialog.getResourceDialog(ListManager.this, "Union with List", "Union", csp);
			rd.setLocationRelativeTo(DialogPanel.getTopWindow());
			rd.setVisible(true);
			StringVector unionList = getList(rd.getSelectedResourceName());
			if (unionList != null) this.setContent(list.union(unionList, csp.caseSensitive.isSelected()));
			this.dirty = true;
		}
		
		void intersectList() {
			StringVector list = this.getContent();
			CaseSensitivePanel csp = new CaseSensitivePanel();
			ResourceDialog rd = ResourceDialog.getResourceDialog(ListManager.this, "Intersect with List", "Intersect", csp);
			rd.setLocationRelativeTo(DialogPanel.getTopWindow());
			rd.setVisible(true);
			StringVector intersectList = getList(rd.getSelectedResourceName());
			if (intersectList != null) this.setContent(list.intersect(intersectList, csp.caseSensitive.isSelected()));
			this.dirty = true;
		}
		
		void subtractList() {
			StringVector list = this.getContent();
			CaseSensitivePanel csp = new CaseSensitivePanel();
			ResourceDialog rd = ResourceDialog.getResourceDialog(ListManager.this, "Subtract List", "Subtract", csp);
			rd.setLocationRelativeTo(DialogPanel.getTopWindow());
			rd.setVisible(true);
			StringVector subtractList = getList(rd.getSelectedResourceName());
			if (subtractList != null) this.setContent(list.without(subtractList, csp.caseSensitive.isSelected()));
			this.dirty = true;
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

	private class CaseSensitivePanel extends JPanel {
		private JCheckBox caseSensitive = new JCheckBox("Case Sensitive");
		private CaseSensitivePanel() {
			this(false);
		}
		private CaseSensitivePanel(boolean caseSensitive) {
			super(new FlowLayout(FlowLayout.LEFT), true);
			this.caseSensitive.setSelected(caseSensitive);
			this.add(this.caseSensitive);
		}
	}

	private class ExtractListDialog extends DialogPanel {
		
		private static final String ALL_TYPES_TYPE = "<All Annotations>";
		private static final String ANNOTATION_VALUE_ATTRIBUTE = "<Annotation Value>";
		private static final String ALL_ATTRIBUTES_ATTRIBUTE = "<All Attributes>";
		
		private QueriableAnnotation data;
		
		private JComboBox typeSelector;
		private JComboBox attributeSelector;
		private JCheckBox useTokens = new JCheckBox("Individual Tokens", false);
		private JCheckBox caseSensitive = new JCheckBox("Case Sensitive", false);
		
		private boolean isCommitted = false;
		
		ExtractListDialog(QueriableAnnotation data) {
			super("Extract List From Annotations", true);
			this.data = data;
			
			String[] dataTypes = this.data.getAnnotationTypes();
			String[] types = new String[dataTypes.length + 1];
			types[0] = ALL_TYPES_TYPE;
			for (int t = 0; t < dataTypes.length; t++) types[t+1] = dataTypes[t];
			
			//	initialize selector
			this.typeSelector = new JComboBox(types);
			this.typeSelector.setBorder(BorderFactory.createLoweredBevelBorder());
			this.typeSelector.setPreferredSize(new Dimension(200, 25));
			this.typeSelector.setEditable(false);
			this.typeSelector.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent e) {
					sourceTypeChanged();
				}
			});
			
			this.attributeSelector = new JComboBox();
			this.attributeSelector.setBorder(BorderFactory.createLoweredBevelBorder());
			this.attributeSelector.setPreferredSize(new Dimension(200, 25));
			this.attributeSelector.setEditable(false);
			
			Annotation[] annotations = this.data.getAnnotations(null);
			StringVector attributeNameCollector = new StringVector();
			for (int a = 0; a < annotations.length; a++)
				attributeNameCollector.addContentIgnoreDuplicates(annotations[a].getAttributeNames());
			this.attributeSelector.removeAllItems();
			this.attributeSelector.addItem(ANNOTATION_VALUE_ATTRIBUTE);
			this.attributeSelector.addItem(ALL_ATTRIBUTES_ATTRIBUTE);
			for (int i = 0; i < attributeNameCollector.size(); i++)
				this.attributeSelector.addItem(attributeNameCollector.get(i));
			
			JPanel selectorPanel = new JPanel(new GridBagLayout());
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.weighty = 0;
			gbc.gridwidth = 1;
			gbc.gridheight = 1;
			gbc.insets.top = 5;
			gbc.insets.left = 5;
			gbc.insets.right = 5;
			gbc.insets.bottom = 5;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			
			gbc.gridy = 0;
			gbc.gridx = 0;
			gbc.weightx = 0;
			selectorPanel.add(new JLabel("Annotation Type"), gbc.clone());
			gbc.gridx = 1;
			gbc.weightx = 1;
			selectorPanel.add(this.typeSelector, gbc.clone());
			
			gbc.gridy = 1;
			gbc.gridx = 0;
			gbc.weightx = 0;
			selectorPanel.add(new JLabel("Attribute Name"), gbc.clone());
			gbc.gridx = 1;
			gbc.weightx = 1;
			selectorPanel.add(this.attributeSelector, gbc.clone());
			
			gbc.gridy = 2;
			gbc.gridx = 0;
			gbc.weightx = 0;
			selectorPanel.add(this.useTokens, gbc.clone());
			gbc.gridx = 1;
			gbc.weightx = 1;
			selectorPanel.add(this.caseSensitive, gbc.clone());
			
			//	initialize main buttons
			JButton commitButton = new JButton("Extract");
			commitButton.setBorder(BorderFactory.createRaisedBevelBorder());
			commitButton.setPreferredSize(new Dimension(100, 21));
			commitButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					isCommitted = true;
					dispose();
				}
			});
			
			JButton abortButton = new JButton("Cancel");
			abortButton.setBorder(BorderFactory.createRaisedBevelBorder());
			abortButton.setPreferredSize(new Dimension(100, 21));
			abortButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					dispose();
				}
			});
			
			JPanel mainButtonPanel = new JPanel();
			mainButtonPanel.setLayout(new FlowLayout());
			mainButtonPanel.add(commitButton);
			mainButtonPanel.add(abortButton);
			
			//	put the whole stuff together
			this.setLayout(new BorderLayout());
			this.add(selectorPanel, BorderLayout.CENTER);
			this.add(mainButtonPanel, BorderLayout.SOUTH);
			
			this.setResizable(true);
			this.setSize(new Dimension(400, 150));
			this.setLocationRelativeTo(DialogPanel.getTopWindow());
		}
		
		void sourceTypeChanged() {
			Object item = this.typeSelector.getSelectedItem();
			Annotation[] annotations = this.data.getAnnotations((ALL_TYPES_TYPE.equals(item) || (item == null)) ? null : item.toString());
			StringVector attributeNameCollector = new StringVector();
			for (int a = 0; a < annotations.length; a++)
				attributeNameCollector.addContentIgnoreDuplicates(annotations[a].getAttributeNames());
			this.attributeSelector.removeAllItems();
			this.attributeSelector.addItem(ANNOTATION_VALUE_ATTRIBUTE);
			this.attributeSelector.addItem(ALL_ATTRIBUTES_ATTRIBUTE);
			for (int i = 0; i < attributeNameCollector.size(); i++)
				this.attributeSelector.addItem(attributeNameCollector.get(i));
		}
		
		boolean isCommitted() {
			return this.isCommitted;
		}
		
		String getType() {
			Object item = this.typeSelector.getSelectedItem();
			return (((item == null) || ALL_TYPES_TYPE.equals(item)) ? null : item.toString());
		}
		
		String getAttribute() {
			Object item = this.attributeSelector.getSelectedItem();
			return (((item == null) || ALL_ATTRIBUTES_ATTRIBUTE.equals(item)) ? null : (ANNOTATION_VALUE_ATTRIBUTE.equals(item) ? Annotation.ANNOTATION_VALUE_ATTRIBUTE : item.toString()));
		}
		
		boolean useTokens() {
			return this.useTokens.isSelected();
		}
		
		boolean caseSensitive() {
			return this.caseSensitive.isSelected();
		}
	}
}
