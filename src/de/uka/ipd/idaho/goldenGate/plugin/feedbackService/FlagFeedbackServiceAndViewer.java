///*
// * Copyright (c) 2006-, IPD Boehm, Universitaet Karlsruhe (TH) / KIT, by Guido Sautter
// * All rights reserved.
// *
// * Redistribution and use in source and binary forms, with or without
// * modification, are permitted provided that the following conditions are met:
// *
// *     * Redistributions of source code must retain the above copyright
// *       notice, this list of conditions and the following disclaimer.
// *     * Redistributions in binary form must reproduce the above copyright
// *       notice, this list of conditions and the following disclaimer in the
// *       documentation and/or other materials provided with the distribution.
// *     * Neither the name of the Universität Karlsruhe (TH) / KIT nor the
// *       names of its contributors may be used to endorse or promote products
// *       derived from this software without specific prior written permission.
// *
// * THIS SOFTWARE IS PROVIDED BY UNIVERSITÄT KARLSRUHE (TH) / KIT AND CONTRIBUTORS 
// * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
// * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// * ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY
// * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
// * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
// * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
// */
//package de.uka.ipd.idaho.goldenGate.plugin.feedbackService;
//
//import java.awt.BorderLayout;
//import java.awt.Color;
//import java.awt.Component;
//import java.awt.Container;
//import java.awt.Dialog;
//import java.awt.Dimension;
//import java.awt.FlowLayout;
//import java.awt.Font;
//import java.awt.Frame;
//import java.awt.GridLayout;
//import java.awt.KeyboardFocusManager;
//import java.awt.LayoutManager;
//import java.awt.Point;
//import java.awt.Toolkit;
//import java.awt.Window;
//import java.awt.event.ActionEvent;
//import java.awt.event.ActionListener;
//import java.awt.event.ComponentAdapter;
//import java.awt.event.ComponentEvent;
//import java.awt.event.ContainerAdapter;
//import java.awt.event.ContainerEvent;
//import java.awt.event.ContainerListener;
//import java.awt.event.ItemEvent;
//import java.awt.event.ItemListener;
//import java.awt.event.MouseAdapter;
//import java.awt.event.MouseEvent;
//import java.io.BufferedReader;
//import java.io.IOException;
//import java.io.InputStreamReader;
//import java.io.OutputStreamWriter;
//import java.io.StringReader;
//import java.io.Writer;
//import java.util.Arrays;
//import java.util.Collections;
//import java.util.Comparator;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.Iterator;
//import java.util.LinkedHashMap;
//import java.util.LinkedList;
//import java.util.Map;
//import java.util.Properties;
//import java.util.Set;
//import java.util.TreeSet;
//
//import javax.swing.AbstractButton;
//import javax.swing.BorderFactory;
//import javax.swing.ButtonGroup;
//import javax.swing.DefaultComboBoxModel;
//import javax.swing.JButton;
//import javax.swing.JCheckBox;
//import javax.swing.JComboBox;
//import javax.swing.JComponent;
//import javax.swing.JDialog;
//import javax.swing.JLabel;
//import javax.swing.JMenu;
//import javax.swing.JMenuItem;
//import javax.swing.JPanel;
//import javax.swing.JProgressBar;
//import javax.swing.JRadioButton;
//import javax.swing.JScrollPane;
//import javax.swing.JTabbedPane;
//import javax.swing.JTable;
//import javax.swing.JTextArea;
//import javax.swing.JTextPane;
//import javax.swing.ListSelectionModel;
//import javax.swing.SwingUtilities;
//import javax.swing.event.TableModelListener;
//import javax.swing.table.DefaultTableCellRenderer;
//import javax.swing.table.JTableHeader;
//import javax.swing.table.TableModel;
//import javax.swing.text.AttributeSet;
//import javax.swing.text.Element;
//import javax.swing.text.JTextComponent;
//import javax.swing.text.SimpleAttributeSet;
//import javax.swing.text.StyleConstants;
//import javax.swing.text.StyledDocument;
//
//import de.uka.ipd.idaho.gamta.Annotation;
//import de.uka.ipd.idaho.gamta.AnnotationUtils;
//import de.uka.ipd.idaho.gamta.Gamta;
//import de.uka.ipd.idaho.gamta.MutableAnnotation;
//import de.uka.ipd.idaho.gamta.QueriableAnnotation;
//import de.uka.ipd.idaho.gamta.Token;
//import de.uka.ipd.idaho.gamta.TokenSequence;
//import de.uka.ipd.idaho.gamta.TokenSequenceUtils;
//import de.uka.ipd.idaho.gamta.Tokenizer;
//import de.uka.ipd.idaho.gamta.defaultImplementation.PlainTokenSequence;
//import de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel;
//import de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel.FeedbackService;
//import de.uka.ipd.idaho.goldenGate.DocumentEditor;
//import de.uka.ipd.idaho.goldenGate.DocumentEditorDialog;
//import de.uka.ipd.idaho.goldenGate.GoldenGATE;
//import de.uka.ipd.idaho.goldenGate.observers.AnnotationObserver;
//import de.uka.ipd.idaho.goldenGate.plugins.AbstractDocumentViewer;
//import de.uka.ipd.idaho.goldenGate.plugins.AnnotationFilter;
//import de.uka.ipd.idaho.goldenGate.plugins.Resource;
//import de.uka.ipd.idaho.goldenGate.plugins.SettingsPanel;
//import de.uka.ipd.idaho.goldenGate.util.DialogPanel;
//import de.uka.ipd.idaho.htmlXmlUtil.accessories.IoTools;
//import de.uka.ipd.idaho.stringUtils.StringVector;
//
///**
// * This feedback service implementation opens the argument feedback panel in a
// * JDialog, wrapping it in a JScrollPane first. All the button labels feedback
// * panels have show up as buttons at the bottom of the dialog; clicking either
// * of the buttons closes the dialog immediately. The label of the clicked
// * button becomes the status code of the displayed feedback panel. The label of
// * the feedback panel is  displayed above the feedback panel's actual content.
// * <br>
// * Feedback dialogs include functionality for zooming feedback panels to adjust
// * the content size to both user vision and screen size.
// * <br>
// * Annotations displaying in feedback panels can be flagged for further
// * inspection. Namely, flagged annotations show up in the list dialogs provided
// * by FlaggedAnnotationViewer, which is bundled with this plugin.
// * 
// * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel.FeedbackService#getFeedback(de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel)
// * 
// * @author sautter
// */
//public class FlagFeedbackServiceAndViewer extends AbstractDocumentViewer implements FeedbackService {
//	
//	private static final int defaultMaxDialogWidth = 700;
//	private static final int defaultMaxDialogHeight = 700;
//	
//	private static final String noFlag = "<Do not flag>";
//	
//	private int maxDialogWidth;
//	private int maxDialogHeight;
//	
//	private StringVector flagList = new StringVector();;
//	private TreeSet flagSet = new TreeSet();
//	
//	private Properties annotationFlags = new Properties();
//	
//	/** public default constructor to enable class loading */
//	public FlagFeedbackServiceAndViewer() {
//		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
//		this.maxDialogWidth = Math.max(defaultMaxDialogWidth, ((screenSize.width * 2) / 3));
//		this.maxDialogHeight = Math.max(defaultMaxDialogHeight, ((screenSize.height * 2) / 3));
//		if (DEBUG) System.out.println("FlaggingFeedbackService: max dialog size set to " + this.maxDialogWidth + " x " + this.maxDialogHeight);
//	}
//	
//	/* (non-Javadoc)
//	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#getPluginName()
//	 */
//	public String getPluginName() {
//		return "Feedback Service";
//	}
//	
//	/* (non-Javadoc)
//	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#init()
//	 */
//	public void init() {
//		
//		//	load feedback panel flags from config file
//		try {
//			StringVector flagList = StringVector.loadList(new InputStreamReader(this.dataProvider.getInputStream("flags.cnfg"), "UTF-8"));
//			this.flagList = flagList;
//			this.fillFlagSet();
//		} catch (IOException ioe) {}
//		
//		//	listen to annotations in parent
//		this.parent.registerAnnotationObserver(new AnnotationObserver() {
//			public void annotationAdded(QueriableAnnotation doc, Annotation annotation, Resource source) {}
//			public void annotationRemoved(QueriableAnnotation doc, Annotation annotation, Resource source) {
//				annotationFlags.remove(annotation.getAnnotationID());
//			}
//			public void annotationTypeChanged(QueriableAnnotation doc, Annotation annotation, String oldType, Resource source) {}
//			public void annotationAttributeChanged(QueriableAnnotation doc, Annotation annotation, String attributeName, Object oldValue, Resource source) {}
//		});
//		
//		//	register feedback service
//		FeedbackPanel.addFeedbackService(this);
//	}
//	
//	private void fillFlagSet() {
//		this.flagSet.clear();
//		for (int f = 0; f < this.flagList.size(); f++) {
//			String flag = this.flagList.get(f).trim();
//			if ((flag.length() != 0) && !flag.startsWith("//"))
//					this.flagSet.add(flag);
//		}
//	}
//	
//	/* (non-Javadoc)
//	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#exit()
//	 */
//	public void exit() {
//		FeedbackPanel.removeFeedbackService(this);
//	}
//	
//	/* (non-Javadoc)
//	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#getSettingsPanel()
//	 */
//	public SettingsPanel getSettingsPanel() {
//		return new FeedbackServiceSettingsPanel();
//	}
//	
//	private class FeedbackServiceSettingsPanel extends SettingsPanel {
//		private JTextArea flagEditor = new JTextArea();
//		public FeedbackServiceSettingsPanel() {
//			super("Feedback Service", "Edit the flags the feedback service can mark annotations with.");
//			this.setLayout(new BorderLayout());
//			
//			this.flagEditor.setLineWrap(false);
//			this.flagEditor.setWrapStyleWord(false);
//			this.flagEditor.setText(flagList.concatStrings("\r\n"));
//			
//			JScrollPane flagEditorBox = new JScrollPane(this.flagEditor);
//			flagEditorBox.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
//			flagEditorBox.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
//			flagEditorBox.getVerticalScrollBar().setUnitIncrement(25);
//			flagEditorBox.getVerticalScrollBar().setBlockIncrement(25);
//			
//			this.add(flagEditorBox, BorderLayout.CENTER);
//		}
//		
//		public void commitChanges() {
//			try {
//				flagList.clear();
//				BufferedReader flagReader = new BufferedReader(new StringReader(this.flagEditor.getText()));
//				for (String flag; (flag = flagReader.readLine()) != null;)
//					flagList.addElement(flag);
//				Writer flagWriter = new OutputStreamWriter(dataProvider.getOutputStream("flags.cnfg"), "UTF-8");
//				flagList.storeContent(flagWriter);
//				flagWriter.flush();
//				flagWriter.close();
//				fillFlagSet();
//			} catch (IOException ioe) {}
//		}
//	}
//	
//	/* (non-Javadoc)
//	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentViewer#getViewMenuName()
//	 */
//	public String getViewMenuName() {
//		return "List Flagged Annotations";
//	}
//	
//	/* (non-Javadoc)
//	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractDocumentViewer#showDocument(de.uka.ipd.idaho.gamta.MutableAnnotation, de.uka.ipd.idaho.goldenGate.DocumentEditor, java.util.Properties)
//	 */
//	protected void showDocument(MutableAnnotation doc, DocumentEditor editor, Properties parameters) {
//		FlaggedAnnotationDialog fad = new FlaggedAnnotationDialog("Flagged Annotations", doc, editor);
//		fad.setVisible(true);
//	}
//	
//	private class FlaggedAnnotationDialog extends DialogPanel {
//		
//		private FlaggedAnnotationPanel annotationDisplay;
//		
//		private String originalTitle;
//		
//		private JComboBox flagFilterSelector = null;
//		
//		FlaggedAnnotationDialog(String title, MutableAnnotation data, DocumentEditor target) {
//			super(title, true);
//			this.originalTitle = title;
//			
//			JButton closeButton = new JButton("Close");
//			closeButton.setBorder(BorderFactory.createRaisedBevelBorder());
//			closeButton.setPreferredSize(new Dimension(100, 21));
//			closeButton.addActionListener(new ActionListener() {
//				public void actionPerformed(ActionEvent ae) {
//					FlaggedAnnotationDialog.this.dispose();
//				}
//			});
//			
//			//	create editor
//			this.annotationDisplay = new FlaggedAnnotationPanel(data, target);
//			
//			//	create filter selector
//			JPanel flagFilterPanel = null;
//			if (flagSet.size() > 0) {
//				String[] flags = new String[flagSet.size() + 1];
//				flags[0] = "<All Flags>";
//				int fi = 1;
//				for (Iterator fit = flagSet.iterator(); fit.hasNext();)
//					flags[fi++] = ((String) fit.next());
//				this.flagFilterSelector.setModel(new DefaultComboBoxModel(flags));
//				this.flagFilterSelector.setEditable(false);
//				this.flagFilterSelector.setBorder(BorderFactory.createLoweredBevelBorder());
//				this.flagFilterSelector.addItemListener(new ItemListener() {
//					public void itemStateChanged(ItemEvent ie) {
//						applyFlagFilter();
//					}
//				});
//				flagFilterPanel = new JPanel(new BorderLayout());
//				flagFilterPanel.add(new JLabel("Select flag to show annotations for "), BorderLayout.WEST);
//				flagFilterPanel.add(this.flagFilterSelector, BorderLayout.CENTER);
//			}
//			
//			//	put the whole stuff together
//			if (flagFilterPanel != null)
//				this.add(flagFilterPanel, BorderLayout.NORTH);
//			this.add(this.annotationDisplay, BorderLayout.CENTER);
//			this.add(closeButton, BorderLayout.SOUTH);
//			
//			this.setResizable(true);
//			this.setSize(new Dimension(500, 650));
//			this.setLocationRelativeTo((target == null) ? ((Component) this.getOwner()) : ((Component) target));
//		}
//		
//		void applyFlagFilter() {
//			String filterFlagString = this.flagFilterSelector.getSelectedItem().toString();
//			final String filterFlagPrefix = ("<All Flags>".equals(filterFlagString) ? "" : filterFlagString);
//			
//			//	create filter
//			AnnotationFilter filter = new AnnotationFilter() {
//				public boolean accept(Annotation annotation) {
//					return false;
//				}
//				public QueriableAnnotation[] getMatches(QueriableAnnotation data) {
//					QueriableAnnotation[] dataAnnotations = data.getAnnotations();
//					LinkedList matches = new LinkedList();
//					for (int a = 0; a < dataAnnotations.length; a++) {
//						String flag = annotationFlags.getProperty(dataAnnotations[a].getAnnotationID());
//						if ((flag != null) && flag.startsWith(filterFlagPrefix))
//							matches.add(dataAnnotations[a]);
//					}
//					return ((QueriableAnnotation[]) matches.toArray(new QueriableAnnotation[matches.size()]));
//				}
//				public MutableAnnotation[] getMutableMatches(MutableAnnotation data) {
//					MutableAnnotation[] dataAnnotations = data.getMutableAnnotations();
//					LinkedList matches = new LinkedList();
//					for (int a = 0; a < dataAnnotations.length; a++) {
//						String flag = annotationFlags.getProperty(dataAnnotations[a].getAnnotationID());
//						if ((flag != null) && flag.startsWith(filterFlagPrefix))
//							matches.add(dataAnnotations[a]);
//					}
//					return ((MutableAnnotation[]) matches.toArray(new MutableAnnotation[matches.size()]));
//				}
//				public String getName() {
//					return ((filterFlagPrefix.length() == 0) ? "Flagged Annotations" : ("Annotations flagged '" + filterFlagPrefix + "'"));
//				}
//				public String getProviderClassName() {
//					return "Homegrown";
//				}
//				public String getTypeLabel() {
//					return "Custom Filter";
//				}
//				public boolean equals(Object obj) {
//					return ((obj != null) && filterFlagPrefix.equals(obj.toString()));
//				}
//				public String toString() {
//					return filterFlagPrefix;
//				}
//			};
//			
//			//	apply filter
//			this.annotationDisplay.setFilter(filter);
//		}
//		
//		/**
//		 * the panel doing the actual displaying
//		 * 
//		 * @author sautter
//		 */
//		private class FlaggedAnnotationPanel extends JPanel {
//			
//			private Dimension editDialogSize = new Dimension(800, 600);
//			private Point editDialogLocation = null;
//			
//			private String[] taggedTypes = {};
//			private String[] highlightTypes = {};
//			
//			private JTable annotationTable;
//			
//			private AnnotationTray[] annotationTrays;
//			private HashMap annotationTraysByID = new HashMap();
//			
//			private MutableAnnotation data;
//			private DocumentEditor target;
//			private AnnotationFilter filter = null;
//			
//			private JRadioButton showMatches = new JRadioButton("Show Matches Only", true);
//			private JRadioButton highlightMatches = new JRadioButton("Highlight Matches", false);
//			
//			private int sortColumn = -1;
//			private boolean sortDescending = false;
//			
//			FlaggedAnnotationPanel(MutableAnnotation data, DocumentEditor target) {
//				super(new BorderLayout(), true);
//				this.setBorder(BorderFactory.createEtchedBorder());
//				
//				this.data = data;
//				this.target = target;
//				
//				//	read base layout settings
//				this.taggedTypes = ((this.target == null) ? new String[0] : this.target.getTaggedAnnotationTypes());
//				this.highlightTypes = ((this.target == null) ? new String[0] : this.target.getHighlightAnnotationTypes());
//				
//				//	initialize display
//				this.annotationTable = new JTable();
//				this.annotationTable.setDefaultRenderer(Object.class, new TooltipAwareComponentRenderer(5, data.getTokenizer()));
//				this.annotationTable.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
//				this.annotationTable.addMouseListener(new MouseAdapter() {
//					public void mouseClicked(MouseEvent me) {
//						
//						int clickRowIndex = annotationTable.rowAtPoint(me.getPoint());
//						int rowIndex = annotationTable.getSelectedRow();
//						if ((clickRowIndex != -1) && ((clickRowIndex < rowIndex) || (clickRowIndex >= (rowIndex + annotationTable.getSelectedRowCount())))) {
//							ListSelectionModel lsm = annotationTable.getSelectionModel();
//							if (lsm != null) lsm.setSelectionInterval(clickRowIndex, clickRowIndex);
//						}
//						
//						if (me.getButton() != MouseEvent.BUTTON1)
//							showContextMenu(me);
//						
//						else if ((me.getClickCount() > 1) && (annotationTable.getSelectedRowCount() == 1))
//							editAnnotation(annotationTrays[clickRowIndex].annotation);
//					}
//				});
//				
//				final JTableHeader header = this.annotationTable.getTableHeader();
//				header.setReorderingAllowed(false);
//				header.addMouseListener(new MouseAdapter() {
//					public void mouseClicked(MouseEvent me) {
//		                int newSortColumn = header.columnAtPoint(me.getPoint());
//		                if (newSortColumn == sortColumn)
//		                	sortDescending = !sortDescending;
//		                else {
//		                	sortDescending = false;
//		                	sortColumn = newSortColumn;
//		                }
//		                sortAnnotations();
//					}
//				});
//				
//				this.refreshAnnotations();
//				
//				JScrollPane annotationTableBox = new JScrollPane(this.annotationTable);
//				
//				this.showMatches.addItemListener(new ItemListener() {
//					public void itemStateChanged(ItemEvent ie) {
//						if (showMatches.isSelected()) {
//							refreshAnnotations();
//						}
//					}
//				});
//				this.highlightMatches.addItemListener(new ItemListener() {
//					public void itemStateChanged(ItemEvent ie) {
//						if (highlightMatches.isSelected()) {
//							refreshAnnotations();
//						}
//					}
//				});
//				
//				ButtonGroup displayModeButtonGroup = new ButtonGroup();
//				displayModeButtonGroup.add(this.showMatches);
//				displayModeButtonGroup.add(this.highlightMatches);
//				
//				JPanel displayModePanel = new JPanel(new GridLayout(1, 2));
//				displayModePanel.add(this.showMatches);
//				displayModePanel.add(this.highlightMatches);
//				
//				this.add(displayModePanel, BorderLayout.NORTH);
//				this.add(annotationTableBox, BorderLayout.CENTER);
//			}
//			
//			void setFilter(AnnotationFilter filter) {
//				this.filter = filter;
//				this.refreshAnnotations();
//			}
//			
//			void refreshAnnotations() {
//				
//				//	apply filter
//				MutableAnnotation[] annotations = ((this.filter == null) ? new MutableAnnotation[0] : this.filter.getMutableMatches(this.data));
//				this.annotationTrays = new AnnotationTray[annotations.length];
//				
//				//	set up statistics
//				String type = ((annotations.length == 0) ? "" : annotations[0].getType());
//				Set matchIDs = new HashSet();
//				
//				//	check matching annotations
//				for (int a = 0; a < annotations.length; a++) {
//					if (!type.equals(annotations[a].getType()))
//						type = "";
//					
//					matchIDs.add(annotations[a].getAnnotationID());
//					
//					if (this.annotationTraysByID.containsKey(annotations[a].getAnnotationID()))
//						this.annotationTrays[a] = ((AnnotationTray) this.annotationTraysByID.get(annotations[a].getAnnotationID()));
//					
//					else {
//						this.annotationTrays[a] = new AnnotationTray(annotations[a]);
//						this.annotationTraysByID.put(annotations[a].getAnnotationID(), this.annotationTrays[a]);
//					}
//					
//					this.annotationTrays[a].isMatch = true;
//				}
//				
//				//	more than one type
//				if (type.length() == 0) {
//					this.showMatches.setSelected(true);
//					this.showMatches.setEnabled(false);
//					this.highlightMatches.setEnabled(false);
//					
//					setTitle(originalTitle + " - " + annotations.length + " Annotations");
//				}
//				
//				//	all of same type, do match highlight display if required
//				else {
//					this.showMatches.setEnabled(true);
//					this.highlightMatches.setEnabled(true);
//					
//					//	highlight matches
//					if (this.highlightMatches.isSelected()) {
//						annotations = this.data.getMutableAnnotations(type);
//						this.annotationTrays = new AnnotationTray[annotations.length];
//						
//						for (int a = 0; a < annotations.length; a++) {
//							
//							if (this.annotationTraysByID.containsKey(annotations[a].getAnnotationID()))
//								this.annotationTrays[a] = ((AnnotationTray) this.annotationTraysByID.get(annotations[a].getAnnotationID()));
//							
//							else {
//								this.annotationTrays[a] = new AnnotationTray(annotations[a]);
//								this.annotationTraysByID.put(annotations[a].getAnnotationID(), this.annotationTrays[a]);
//							}
//							
//							this.annotationTrays[a].isMatch = matchIDs.contains(annotations[a].getAnnotationID());
//						}
//						
//						setTitle(originalTitle + " - " + annotations.length + " Annotations, " + matchIDs.size() + " matches");
//					}
//					
//					else setTitle(originalTitle + " - " + annotations.length + " Annotations");
//				}
//				
//				this.annotationTable.setModel(new AnnotationNavigatorTableModel(this.annotationTrays));
//				this.annotationTable.getColumnModel().getColumn(0).setMaxWidth(120);
//				this.annotationTable.getColumnModel().getColumn(1).setMaxWidth(50);
//				this.annotationTable.getColumnModel().getColumn(2).setMaxWidth(50);
//				
//				this.sortColumn = -1;
//				this.sortDescending = false;
//				this.refreshDisplay();
//			}
//			
//			void sortAnnotations() {
//				Arrays.sort(this.annotationTrays, new Comparator() {
//					public int compare(Object o1, Object o2) {
//						AnnotationTray at1 = ((AnnotationTray) o1);
//						AnnotationTray at2 = ((AnnotationTray) o2);
//						int c;
//						if (sortColumn == 0)
//							c = at1.annotation.getType().compareToIgnoreCase(at2.annotation.getType());
//						else if (sortColumn == 1)
//							c = (at1.annotation.getStartIndex() - at2.annotation.getStartIndex());
//						else if (sortColumn == 2)
//							c = (at1.annotation.size() - at2.annotation.size());
//						else if (sortColumn == 3)
//							c = String.CASE_INSENSITIVE_ORDER.compare(at1.annotation.getValue(), at2.annotation.getValue());
//						else c = 0;
//						
//						return ((sortDescending ? -1 : 1) * ((c == 0) ? AnnotationUtils.compare(at1.annotation, at2.annotation) : c));
//					}
//				});
//				this.refreshDisplay();
//			}
//			
//			void refreshDisplay() {
//				for(int i = 0; i < this.annotationTable.getColumnCount();i++)
//					this.annotationTable.getColumnModel().getColumn(i).setHeaderValue(this.annotationTable.getModel().getColumnName(i));
//				this.annotationTable.getTableHeader().revalidate();
//				this.annotationTable.getTableHeader().repaint();
//				this.annotationTable.revalidate();
//				this.annotationTable.repaint();
//				FlaggedAnnotationDialog.this.validate();
//			}
//			
//			void editAnnotation(MutableAnnotation annotation) {
//				
//				//	create dialog & show
//				DocumentEditDialog ded = new DocumentEditDialog(FlaggedAnnotationDialog.this.getDialog(), FlagFeedbackServiceAndViewer.this.parent, target, "Edit Annotation", annotation);
//				ded.setVisible(true);
//				
//				//	finish
//				if (ded.committed && ded.isContentModified()) this.refreshAnnotations();
//			}
//			
//			class DocumentEditDialog extends DocumentEditorDialog {
//				private boolean committed = false;
//				private DocumentEditDialog(JDialog owner, GoldenGATE host, DocumentEditor parent, String title, MutableAnnotation docPart) {
//					super(owner, host, parent, title, docPart);
//					
//					JButton okButton = new JButton("OK");
//					okButton.setBorder(BorderFactory.createRaisedBevelBorder());
//					okButton.setPreferredSize(new Dimension(100, 21));
//					okButton.addActionListener(new ActionListener() {
//						public void actionPerformed(ActionEvent ae) {
//							DocumentEditDialog.this.committed = true;
//							DocumentEditDialog.this.documentEditor.writeChanges();
//							DocumentEditDialog.this.dispose();
//						}
//					});
//					this.mainButtonPanel.add(okButton);
//					
//					JButton cancelButton = new JButton("Cancel");
//					cancelButton.setBorder(BorderFactory.createRaisedBevelBorder());
//					cancelButton.setPreferredSize(new Dimension(100, 21));
//					cancelButton.addActionListener(new ActionListener() {
//						public void actionPerformed(ActionEvent ae) {
//							DocumentEditDialog.this.dispose();
//						}
//					});
//					this.mainButtonPanel.add(cancelButton);
//					
//					for (int t = 0; t < taggedTypes.length; t++)
//						this.documentEditor.setAnnotationTagVisible(taggedTypes[t], true);
//					for (int t = 0; t < highlightTypes.length; t++)
//						this.documentEditor.setAnnotationValueHighlightVisible(highlightTypes[t], true);
//					
//					this.setSize(editDialogSize);
//					if (editDialogLocation == null) this.setLocationRelativeTo(FlaggedAnnotationDialog.this);
//					else this.setLocation(editDialogLocation);
//				}
//				
//				public void dispose() {
//					editDialogSize = this.getSize();
//					editDialogLocation = this.getLocation(editDialogLocation);
//					taggedTypes = this.documentEditor.getTaggedAnnotationTypes();
//					highlightTypes = this.documentEditor.getHighlightAnnotationTypes();
//					
//					super.dispose();
//				}
//			}
//			
//			void showContextMenu(MouseEvent me) {
//				int row = this.annotationTable.getSelectedRow();
//				if (row == -1) return;
//				
//				int rows = this.annotationTable.getSelectedRowCount();
//				final AnnotationTray tray = this.annotationTrays[row];
//				final AnnotationTray[] trays = new AnnotationTray[rows];
//				System.arraycopy(annotationTrays, row, trays, 0, rows);
//				Arrays.sort(trays, new Comparator() {
//					private Comparator comp = AnnotationUtils.getComparator(data.getAnnotationNestingOrder());
//					public int compare(Object o1, Object o2) {
//						return this.comp.compare(((AnnotationTray) o1).annotation, ((AnnotationTray) o1).annotation);
//					}
//				});
//				
//				JMenu menu = new JMenu();
//				JMenuItem mi;
//				
//				mi = new JMenuItem("Edit");
//				mi.addActionListener(new ActionListener() {
//					public void actionPerformed(ActionEvent ae) {
//						if (trays.length == 1)
//							editAnnotation(tray.annotation);
//						
//						else {
//							MutableAnnotation temp = data.addAnnotation(TEMP_ANNOTATION_TYPE, trays[0].annotation.getStartIndex(), (trays[trays.length - 1].annotation.getEndIndex() - trays[0].annotation.getStartIndex()));
//							editAnnotation(temp);
//							data.removeAnnotation(temp);
//						}
//					}
//				});
//				menu.add(mi);
//				
//				mi = new JMenuItem("Un-Flag");
//				mi.addActionListener(new ActionListener() {
//					public void actionPerformed(ActionEvent ae) {
//						for (int r = 0; r < trays.length; r++)
//							annotationFlags.remove(trays[r].annotation.getAnnotationID());
//						refreshAnnotations();
//					}
//				});
//				menu.add(mi);
//				
//				mi = new JMenuItem("Remove");
//				mi.addActionListener(new ActionListener() {
//					public void actionPerformed(ActionEvent ae) {
//						for (int r = 0; r < trays.length; r++)
//							data.removeAnnotation(trays[r].annotation);
//						refreshAnnotations();
//					}
//				});
//				menu.add(mi);
//				
//				if (rows == 1) {
//					mi = new JMenuItem("Remove All");
//					mi.addActionListener(new ActionListener() {
//						public void actionPerformed(ActionEvent ae) {
//							String value = tray.annotation.getValue();
//							int annotationCount = 0;
//							for (int a = 0; a < annotationTrays.length; a++)
//								if (annotationTrays[a].annotation.getValue().equals(value)) {
//									annotationCount ++;
//									data.removeAnnotation(annotationTrays[a].annotation);
//								}
//							if (annotationCount > 0) refreshAnnotations();
//						}
//					});
//					menu.add(mi);
//				}
//				
//				mi = new JMenuItem("Delete");
//				mi.addActionListener(new ActionListener() {
//					public void actionPerformed(ActionEvent ae) {
//						for (int r = trays.length; r > 0; r--)
//							data.removeTokens(trays[r - 1].annotation);
//						refreshAnnotations();
//					}
//				});
//				menu.add(mi);
//				
//				menu.getPopupMenu().show(this.annotationTable, me.getX(), me.getY());
//			}
//			
//			private class AnnotationTray {
//				MutableAnnotation annotation;
//				boolean isMatch = false;
//				AnnotationTray(MutableAnnotation annotation) {
//					this.annotation = annotation;
//				}
//			}
//		
//			private class AnnotationNavigatorTableModel implements TableModel {
//				private AnnotationTray[] annotations;
//				private boolean isMatchesOnly = true;
//				AnnotationNavigatorTableModel(AnnotationTray[] annotations) {
//					this.annotations = annotations;
//					for (int a = 0; a < this.annotations.length; a++)
//						this.isMatchesOnly = (this.isMatchesOnly && this.annotations[a].isMatch);
//				}
//				public void addTableModelListener(TableModelListener l) {}
//				public Class getColumnClass(int columnIndex) {
//					return String.class;
//				}
//				public int getColumnCount() {
//					return 5;
//				}
//				public String getColumnName(int columnIndex) {
//					String sortExtension = ((columnIndex == sortColumn) ? (sortDescending ? " (d)" : " (a)") : "");
//					if (columnIndex == 0)
//						return ("Flag" + sortExtension);
//					else if (columnIndex == 1)
//						return ("Type" + sortExtension);
//					else if (columnIndex == 2)
//						return ("Start" + sortExtension);
//					else if (columnIndex == 3)
//						return ("Size" + sortExtension);
//					else if (columnIndex == 4)
//						return ("Value" + sortExtension);
//					else return null;
//				}
//				public int getRowCount() {
//					return this.annotations.length;
//				}
//				public Object getValueAt(int rowIndex, int columnIndex) {
//					Annotation annotation = this.annotations[rowIndex].annotation;
//					if (this.isMatchesOnly || !this.annotations[rowIndex].isMatch) {
//						if (columnIndex == 0)
//							return annotationFlags.getProperty(annotation.getAnnotationID(), "");
//						else if (columnIndex == 1)
//							return annotation.getType();
//						else if (columnIndex == 2)
//							return "" + annotation.getStartIndex();
//						else if (columnIndex == 3)
//							return "" + annotation.size();
//						else if (columnIndex == 4)
//							return annotation.getValue();
//						else return null;
//					}
//					else {
//						String value = null;
//						if (columnIndex == 0)
//							value = annotationFlags.getProperty(annotation.getAnnotationID(), "");
//						else if (columnIndex == 1)
//							value = annotation.getType();
//						else if (columnIndex == 2)
//							value = "" + annotation.getStartIndex();
//						else if (columnIndex == 3)
//							value = "" + annotation.size();
//						else if (columnIndex == 4)
//							value = annotation.getValue();
//						return ((value == null) ? null : ("<HTML><B>" + value + "</B></HTML>"));
//					}
//				}
//				public boolean isCellEditable(int rowIndex, int columnIndex) {
//					return false;
//				}
//				public void removeTableModelListener(TableModelListener l) {}
//				public void setValueAt(Object newValue, int rowIndex, int columnIndex) {}
//			}
//			
//			private class TooltipAwareComponentRenderer extends DefaultTableCellRenderer {
//				private HashSet tooltipColumns = new HashSet();
//				private Tokenizer tokenizer;
//				TooltipAwareComponentRenderer(int tooltipColumn, Tokenizer tokenizer) {
//					this.tooltipColumns.add("" + tooltipColumn);
//					this.tokenizer = tokenizer;
//				}
//				public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
//					JComponent component = ((value instanceof JComponent) ? ((JComponent) value) : (JComponent) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column));
//					if (this.tooltipColumns.contains("" + row) && (component instanceof JComponent))
//						((JComponent) component).setToolTipText(this.produceTooltipText(new PlainTokenSequence(value.toString(), this.tokenizer)));
//					return component;
//				}
//				private String produceTooltipText(TokenSequence tokens) {
//					if (tokens.size() < 100) return TokenSequenceUtils.concatTokens(tokens);
//					
//					StringVector lines = new StringVector();
//					int startToken = 0;
//					int lineLength = 0;
//					Token lastToken = null;
//					
//					for (int t = 0; t < tokens.size(); t++) {
//						Token token = tokens.tokenAt(t);
//						lineLength += token.length();
//						if (lineLength > 100) {
//							lines.addElement(TokenSequenceUtils.concatTokens(tokens, startToken, (t - startToken + 1)));
//							startToken = (t + 1);
//							lineLength = 0;
//						} else if (Gamta.insertSpace(lastToken, token)) lineLength++;
//					}
//					if (startToken < tokens.size())
//						lines.addElement(TokenSequenceUtils.concatTokens(tokens, startToken, (tokens.size() - startToken)));
//					
//					return ("<HTML>" + lines.concatStrings("<BR>") + "</HTML>");
//				}
//			}
//		}
//	}
//	
//	private static final boolean DEBUG = false;
//	
//	private Map typeDimensions = Collections.synchronizedMap(new HashMap());
//	
//	private ZoomPanel zoomPanel = null;
//	
//	private boolean showFullExplanation = true;
//	
//	private static class ZoomPanel extends JPanel {
//		private float zoomFactor = 1;
//		private JLabel zoomFactorLabel = new JLabel("1", JLabel.CENTER);
//		
//		private static Dimension buttonSize = new Dimension(21, 21);
//		private JButton larger = new JButton("<HTML><B>+</B></HTML>");
//		private JButton smaller = new JButton("<HTML><B>-</B></HTML>");
//		
//		private JDialog dialog = null;
//		
//		private HashMap componentFonts = new HashMap();
//		private boolean newComponents = false;
//		private LinkedHashMap containerListeners = new LinkedHashMap();
//		
//		ZoomPanel() {
//			super(new BorderLayout(), true);
//			
//			this.larger.setBorder(BorderFactory.createRaisedBevelBorder());
//			this.larger.setPreferredSize(buttonSize);
//			this.larger.addActionListener(new ActionListener() {
//				public void actionPerformed(ActionEvent ae) {
//					larger();
//				}
//			});
//			
//			this.smaller.setBorder(BorderFactory.createRaisedBevelBorder());
//			this.smaller.setPreferredSize(buttonSize);
//			this.smaller.addActionListener(new ActionListener() {
//				public void actionPerformed(ActionEvent ae) {
//					smaller();
//				}
//			});
//			
//			this.zoomFactorLabel.addMouseListener(new MouseAdapter() {
//				public void mouseClicked(MouseEvent me) {
//					if (me.getClickCount() > 1)
//						doZoom();
//				}
//			});
//			
//			this.zoomFactorLabel.setBorder(BorderFactory.createLoweredBevelBorder());
//			this.zoomFactorLabel.setOpaque(true);
//			this.zoomFactorLabel.setBackground(Color.WHITE);
//			
//			this.add(new JLabel("<HTML><B>Zoom Control</B></HTML>", JLabel.CENTER), BorderLayout.NORTH);
//			this.add(this.smaller, BorderLayout.WEST);
//			this.add(this.zoomFactorLabel, BorderLayout.CENTER);
//			this.add(this.larger, BorderLayout.EAST);
//		}
//		
//		void larger() {
//			
//			//	adjust zoom factor
//			this.zoomFactor += 0.25f;
//			
//			//	adjust buttons
//			if (this.zoomFactor >= 4)
//				this.larger.setEnabled(false);
//			this.smaller.setEnabled(true);
//			
//			//	update labels
//			this.doZoom();
//		}
//		
//		void smaller() {
//			
//			//	adjust zoom factor
//			this.zoomFactor -= 0.25f;
//			
//			//	adjust buttons
//			if (this.zoomFactor <= 0.5)
//				this.smaller.setEnabled(false);
//			this.larger.setEnabled(true);
//			
//			//	update labels
//			this.doZoom();
//		}
//		
//		void doZoom() {
//			
//			//	refresh component registers
//			if (this.newComponents)
//				this.refreshRegisters();
//			
//			//	adjust zoom display
//			this.zoomFactorLabel.setText("" + this.zoomFactor);
//			
//			//	adjust labels based on current zoom factor
//			for (Iterator cit = this.componentFonts.keySet().iterator(); cit.hasNext();) {
//				JComponent comp = ((JComponent) cit.next());
//				Font font = ((Font) this.componentFonts.get(comp));
//				if (font != null)
//					setComponentFont(comp, font.deriveFont(this.zoomFactor * font.getSize()));
//			}
//			
//			//	re-layout panels
//			waitForSwingEventQueue();
//			for (Iterator cit = this.containerListeners.keySet().iterator(); cit.hasNext();) {
//				Container cont = ((Container) cit.next());
//				LayoutManager lm = cont.getLayout();
//				if (lm != null)
//					lm.layoutContainer(cont);
//				cont.validate();
//				cont.repaint();
//			}
//			
//			//	make changes visible
//			this.dialog.getContentPane().invalidate();
//			this.dialog.getContentPane().validate();
//			this.dialog.getContentPane().repaint();
//		}
//		
//		void setFeedbackDialog(JDialog dialog) {
//			if (this.dialog != null)
//				return;
//			this.dialog = dialog;
//			
//			//	refresh component registers
//			this.refreshRegisters();
//			
//			//	adjust labels based on current zoom factor
//			this.doZoom();
//		}
//		
//		private void refreshRegisters() {
//			LinkedList fontedComponents = new LinkedList();
//			LinkedList containers = new LinkedList();
//			LinkedList componentQueue = new LinkedList();
//			componentQueue.addLast(dialog.getContentPane());
//			while (componentQueue.size() != 0) {
//				Component comp = ((Component) componentQueue.removeFirst());
//				
//				if (comp instanceof JLabel)
//					fontedComponents.addLast(comp);
//				else if (comp instanceof JMenu) {
//					fontedComponents.addLast(comp);
//					componentQueue.addLast(((JMenu) comp).getPopupMenu());
//				}
//				else if (comp instanceof AbstractButton)
//					fontedComponents.addLast(comp);
//				else if (comp instanceof JComboBox)
//					fontedComponents.addLast(comp);
//				else if (comp instanceof JProgressBar)
//					fontedComponents.addLast(comp);
//				else if (comp instanceof JTextComponent)
//					fontedComponents.addLast(comp);
//				
//				else if (comp instanceof JScrollPane) {
//					Component viewComp = ((JScrollPane) comp).getViewport().getView();
//					if (viewComp != null)
//						componentQueue.addLast(viewComp);
//				}
//				else if (comp instanceof JTabbedPane) {
//					for (int t = 0; t < ((JTabbedPane) comp).getTabCount(); t++)
//						componentQueue.addLast(((JTabbedPane) comp).getComponentAt(t));
//				}
//				else if (comp instanceof Container) {
//					containers.addFirst(comp);
//					Component[] subComps = ((Container) comp).getComponents();
//					for (int c = 0; c < subComps.length; c++)
//						componentQueue.addLast(subComps[c]);
//				}
//			}
//			
//			for (Iterator cit = fontedComponents.iterator(); cit.hasNext();) {
//				JComponent comp = ((JComponent) cit.next());
//				if (!this.componentFonts.containsKey(comp)) {
//					Font font = getComponentFont(comp);
//					this.componentFonts.put(comp, font);
//				}
//			}
//			
//			for (Iterator cit = containers.iterator(); cit.hasNext();) {
//				Container cont = ((Container) cit.next());
//				if (!this.containerListeners.containsKey(cont)) {
//					ContainerListener cl = new ContainerAdapter() {
//						public void componentAdded(ContainerEvent ce) {
//							if (!componentFonts.containsKey(ce.getChild()) && !containerListeners.containsKey(ce.getChild()))
//								newComponents = true;
//						}
//					};
//					cont.addContainerListener(cl);
//					this.containerListeners.put(cont, cl);
//				}
//			}
//			
//			this.newComponents = false;
//		}
//		
//		void releaseFeedbackPanel() {
//			if (this.dialog == null)
//				return;
//			
//			//	reset labels' font to original size
//			for (Iterator cit = this.componentFonts.keySet().iterator(); cit.hasNext();) {
//				JComponent comp = ((JComponent) cit.next());
//				Font font = ((Font) this.componentFonts.get(comp));
//				if (font != null)
//					setComponentFont(comp, font);
//			}
//			
//			//	remove container listeners
//			for (Iterator cit = this.containerListeners.keySet().iterator(); cit.hasNext();) {
//				Container cont = ((Container) cit.next());
//				ContainerListener cl = ((ContainerListener) this.containerListeners.get(cont));
//				if (cl != null)
//					cont.removeContainerListener(cl);
//
//			}
//			
//			//	clear register
//			this.dialog = null;
//			this.componentFonts.clear();
//			this.containerListeners.clear();
//		}
//		
//		private static Font getComponentFont(JComponent comp) {
//			if (comp instanceof JTextPane) {
//				StyledDocument sd = ((JTextPane) comp).getStyledDocument();
//				Element e = sd.getCharacterElement(0);
//				if (e != null) {
//					AttributeSet as = e.getAttributes();
//					SimpleAttributeSet fas = new SimpleAttributeSet();
//					if (as.isDefined(StyleConstants.FontConstants.Size))
//						fas.addAttribute(StyleConstants.FontConstants.Size, as.getAttribute(StyleConstants.FontConstants.Size));
//					else fas.addAttribute(StyleConstants.FontConstants.Size, new Integer(comp.getFont().getSize()));
//					return sd.getFont(fas);
//				}
//			}
//			return comp.getFont();
//		}
//		
//		private static void setComponentFont(JComponent comp, Font font) {
//			if (comp instanceof JTextPane) {
//				StyledDocument sd = ((JTextPane) comp).getStyledDocument();
//				SimpleAttributeSet as = new SimpleAttributeSet();
//				as.addAttribute(StyleConstants.FontConstants.Size, new Integer(font.getSize()));
//				sd.setCharacterAttributes(0, sd.getLength(), as, false);
//			}
//			else comp.setFont(font);
//			
//			if (comp instanceof JComboBox)
//				comp.updateUI();
//			else {
//				comp.validate();
//				comp.repaint();
//			}
//		}
//	}
//	
//	/* (non-Javadoc)
//	 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel.FeedbackService#canGetFeedback(de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel)
//	 */
//	public boolean canGetFeedback(FeedbackPanel fp) {
//		return true; // we handle all requests
//	}
//	
//	/* (non-Javadoc)
//	 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel.FeedbackService#getPriority()
//	 */
//	public int getPriority() {
//		return 1;
//	}
//	
//	/* (non-Javadoc)
//	 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel.FeedbackService#getFeedback(de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel)
//	 */
//	public void getFeedback(final FeedbackPanel fp) {
//		Window topWindow = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
////		Disabled due to Java VM bug (InternalError in sun.awt.windows.WToolkit.eventLoop(Native Method) because of JNI flaw)
////		TODO: re-enable this once VM bug is fixed
////		Frame[] frames = Frame.getFrames();
////		LinkedList windows = new LinkedList();
////		for (int f = 0; f < frames.length; f++)
////			windows.addLast(frames[f]);
////		while (windows.size() != 0) {
////			topWindow = ((Window) windows.removeFirst());
////			Window[] subWindows = topWindow.getOwnedWindows();
////			for (int w = 0; w < subWindows.length; w++)
////				windows.add(subWindows[w]);
////		}
//		final JDialog dialog;
//		
//		if (topWindow instanceof Frame)
//			dialog = new JDialog(((Frame) topWindow), fp.getTitle(), true);
//		else if (topWindow instanceof Dialog)
//			dialog = new JDialog(((Dialog) topWindow), fp.getTitle(), true);
//		else dialog = new JDialog(((Frame) null), fp.getTitle(), true);
//		
//		dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
//		dialog.getContentPane().setLayout(new BorderLayout());
//		if (DEBUG) System.out.println("  - dialog produced");
//		
//		final JLabel label;
//		String labelString = fp.getLabel();
//		if (labelString != null) {
//			
//			//	add title to label
//			if ((labelString.length() > 6) && "<html>".equals(labelString.substring(0, 6).toLowerCase()))
//				labelString = "<HTML>" + "<B>What to do in this dialog?</B>" + " (click to collapse) " + "<BR>" + labelString.substring(6);
//			else labelString = "<HTML>" + "<B>What to do in this dialog?</B>" + " (click to collapse) " + "<BR>" + IoTools.prepareForHtml(labelString) + "</HTML>";
//			
//			//	store full and short (collapsed) explanation
//			final String fullLabelString = labelString;
//			final String shortLabelString = "<HTML>" + "<B>What to do in this dialog?</B>" + " (click to expand) " + "</HTML>";
//			
//			//	build and configure label
//			label = new JLabel((this.showFullExplanation ? fullLabelString : shortLabelString), JLabel.LEFT);
//			label.setOpaque(true);
//			label.setBackground(Color.WHITE);
//			label.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.WHITE, 2), BorderFactory.createLineBorder(Color.RED, 1)), BorderFactory.createLineBorder(Color.WHITE, 5)));
//			
//			//	facilitate showing/hiding explanation
//			label.addMouseListener(new MouseAdapter() {
//				public void mouseClicked(MouseEvent me) {
//					showFullExplanation = !showFullExplanation;
//					label.setText(showFullExplanation ? fullLabelString : shortLabelString);
//				}
//			});
//			
//			//	add label to dialog
//			dialog.getContentPane().add(label, BorderLayout.NORTH);
//		}
//		else label = null;
//		if (DEBUG) System.out.println("  - dialog label produced");
//		
//		if (this.zoomPanel == null)
//			this.zoomPanel = new ZoomPanel();
//		if (DEBUG) System.out.println("  - zoom panel produced");
//		
//		//	initialize flagging
//		final String feedbackAnnotationId = fp.getProperty(FeedbackPanel.TARGET_ANNOTATION_ID_PROPERTY);
//		String feedbackAnnotationType = fp.getProperty(FeedbackPanel.TARGET_ANNOTATION_TYPE_PROPERTY);
//		String flagLabel = ((feedbackAnnotationType == null) ? "" : (" " + feedbackAnnotationType.substring(0, 1).toUpperCase() + feedbackAnnotationType.substring(1)));
//		String flagTooltip = ("Flag the content of this feedback dialog " + (flagSet.isEmpty() ? "for further inspection" : "for one of the available reasons"));
//		final JCheckBox simpleFlag;
//		final JLabel detailFlagLabel;
//		final JComboBox detailFlagSelector;
//		final JPanel detailFlagPanel;
//		if (feedbackAnnotationId == null) {
//			simpleFlag = null;
//			detailFlagLabel = null;
//			detailFlagSelector = null;
//			detailFlagPanel = null;
//		}
//		else if (this.flagSet.isEmpty()) {
//			simpleFlag = new JCheckBox(("Flag" + flagLabel), (this.annotationFlags.containsKey(feedbackAnnotationId)));
//			simpleFlag.setToolTipText(flagTooltip);
//			detailFlagLabel = null;
//			detailFlagSelector = null;
//			detailFlagPanel = null;
//		}
//		else {
//			simpleFlag = null;
//			detailFlagLabel = new JLabel("Flag" + flagLabel + " as ");
//			detailFlagLabel.setToolTipText(flagTooltip);
//			detailFlagSelector = new JComboBox((String[]) this.flagSet.toArray(new String[this.flagSet.size()]));
//			detailFlagSelector.insertItemAt(noFlag, 0);
//			detailFlagSelector.setSelectedItem(this.annotationFlags.getProperty(feedbackAnnotationId, noFlag));
//			detailFlagPanel = new JPanel(new BorderLayout(), true);
//			detailFlagPanel.add(detailFlagLabel, BorderLayout.WEST);
//			detailFlagPanel.add(detailFlagSelector, BorderLayout.CENTER);
//		}
//		if (DEBUG) System.out.println("  - flagging prepared");
//		
//		//	add buttons
//		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
//		final String[] buttons = fp.getButtons();
//		if (buttons.length == 0) {
//			JButton button = new JButton("OK");
//			button.addActionListener(new ActionListener() {
//				public void actionPerformed(ActionEvent ae) {
//					String[] error = fp.checkFeedback("OK");
//					if (error == null) {
//						fp.setStatusCode("OK");
//						if (simpleFlag != null) {
//							if (simpleFlag.isSelected())
//								annotationFlags.setProperty(feedbackAnnotationId, "");
//							else annotationFlags.remove(feedbackAnnotationId);
//						}
//						else if (detailFlagSelector != null) {
//							String flag = ((String) detailFlagSelector.getSelectedItem());
//							if ((flag != null) && !noFlag.equals(flag))
//								annotationFlags.setProperty(feedbackAnnotationId, flag);
//							else annotationFlags.remove(feedbackAnnotationId);
//						}
//						dialog.dispose();
//					}
//					else displayError(error, dialog);
//				}
//			});
//			buttonPanel.add(button);
//		}
//		else for (int b = 0; b < buttons.length; b++) {
//			final String buttonLabel = buttons[b];
//			JButton button = new JButton(buttonLabel);
//			button.addActionListener(new ActionListener() {
//				public void actionPerformed(ActionEvent ae) {
//					String[] error = fp.checkFeedback(buttonLabel);
//					if (error == null) {
//						fp.setStatusCode(buttonLabel);
//						if (simpleFlag != null) {
//							if (simpleFlag.isSelected())
//								annotationFlags.setProperty(feedbackAnnotationId, "");
//							else annotationFlags.remove(feedbackAnnotationId);
//						}
//						else if (detailFlagSelector != null) {
//							String flag = ((String) detailFlagSelector.getSelectedItem());
//							if ((flag != null) && !noFlag.equals(flag))
//								annotationFlags.setProperty(feedbackAnnotationId, flag);
//							else annotationFlags.remove(feedbackAnnotationId);
//						}
//						dialog.dispose();
//					}
//					else displayError(error, dialog);
//				}
//			});
//			buttonPanel.add(button);
//		}
//		if (DEBUG) System.out.println("  - buttons produced");
//		
//		final Properties fieldStates = fp.getFieldStates();
//		if (fieldStates != null) {
//			JButton resetButton = new JButton("Reset");
//			resetButton.addActionListener(new ActionListener() {
//				public void actionPerformed(ActionEvent ae) {
//					fp.setFieldStates(fieldStates);
//					dialog.validate();
//					dialog.repaint();
//				}
//			});
//			buttonPanel.add(resetButton);
//			if (DEBUG) System.out.println("  - reset mechanism prepared");
//		}
//		
//		JPanel functionPanel = new JPanel(new BorderLayout(), true);
//		if (simpleFlag != null)
//			functionPanel.add(simpleFlag, BorderLayout.WEST);
//		else functionPanel.add(detailFlagPanel, BorderLayout.WEST);
//		functionPanel.add(buttonPanel, BorderLayout.CENTER);
//		functionPanel.add(this.zoomPanel, BorderLayout.EAST);
//		dialog.getContentPane().add(functionPanel, BorderLayout.SOUTH);
//		if (DEBUG) System.out.println("  - functions added");
//		
//		//	compute type specific max size
//		int maxWidth = this.maxDialogWidth;
//		int maxHeight = this.maxDialogHeight;
//		final String requestType = fp.getProperty(FeedbackPanel.REQUESTER_CLASS_NAME_PROPERTY);
//		Dimension maxSize = ((Dimension) this.typeDimensions.get(requestType));
//		if (maxSize != null) {
//			maxWidth = Math.max(maxWidth, maxSize.width);
//			maxHeight = Math.max(maxHeight, maxSize.height);
//		}
//		if (DEBUG) System.out.println("  - dialog max size computed: " + maxWidth + " x " + maxHeight);
//		
//		//	prepare recording resizing actions
//		dialog.addComponentListener(new ComponentAdapter() {
//			public void componentResized(ComponentEvent ce) {
//				if (dialog.isVisible()) {
//					Dimension dialogSize = dialog.getSize();
//					Dimension maxSize = ((Dimension) typeDimensions.get(requestType));
//					if (maxSize == null)
//						maxSize = dialogSize;
//					maxSize = new Dimension(Math.max(dialogSize.width, maxSize.width), Math.max(dialogSize.height, maxSize.height));
//					typeDimensions.put(requestType, maxSize);
//					if (DEBUG) System.out.println("  - dialog max size for '" + requestType + "' set to " + maxSize.width + " x " + maxSize.height);
//				}
//				else if (DEBUG) System.out.println("  - dialog max size for '" + requestType + "' remains, dialog not visible yet");
//			}
//		});
//		
//		//	put panel in scroll pane (no more if necessary, but always, since FeedbackPanel implements Scrollable now)
//		JScrollPane fpBox = new JScrollPane(fp);
//		fpBox.getVerticalScrollBar().setUnitIncrement(Math.max(50, fp.getScrollableUnitIncrement(null, 0, 0)));
//		fpBox.getVerticalScrollBar().setBlockIncrement(200); // we need to set this explicitly here, as the feedback panel returns 1 for the block increment to make scrolling position flexible
//		dialog.getContentPane().add(fpBox, BorderLayout.CENTER);
//		if (DEBUG) System.out.println("  - dialog content boxed");
//		
//		/*
//		 * we have to catch exceptions from here on in order to make sure we
//		 * remove the dialog from the zoom controller even in case of layout
//		 * exceptions like the '512' ArrayIndexOutOfBounds exception in
//		 * GridBagLayout ...
//		 */
//		try {
//			
//			//	apply zoom (has to be done before setting dialog size)
//			this.zoomPanel.setFeedbackDialog(dialog);
//			if (DEBUG) System.out.println("  - zoom applied");
//			
//			//	compute size
//			Dimension lPs = ((label == null) ? new Dimension(0, 0) : label.getPreferredSize());
//			Dimension fpPs = fp.getPreferredSize();
//			Dimension bpPs = buttonPanel.getPreferredSize();
//			
//			int width = maxWidth;
//			int height = maxHeight;
//			
//			if ((fpPs != null) && (bpPs != null) && (lPs != null)) {
//				width = (Math.max(lPs.width, Math.max(fpPs.width, bpPs.width)) + 10); // add for window borders
//				height = (lPs.height + fpPs.height + bpPs.height + 50); // add for window title bar and borders
//				if (DEBUG) System.out.println("  - dialog size pre-computed: " + width + " x " + height);
//			}
//			
//			//	adjust size
//			width = Math.min(width + 50, maxWidth); // add for scroll bar
//			height = Math.min(height, maxHeight);
//			if (DEBUG) System.out.println("  - dialog content produced");
//			
//			//	set dialog size
//			dialog.pack();
//			dialog.setSize(width, height);
//			
//			//	wait for pack() to complete
//			waitForSwingEventQueue();
//		
//			//	validate zoom (has to be done after initially packing the dialog)
//			this.zoomPanel.doZoom();
//			if (DEBUG) System.out.println("  - zoom validated");
//			
//			//	re-compute dialog size (feedback panels with line wrapping content, for instance, may change their preferred height if width is reduced)
//			if (fp.getScrollableTracksViewportWidth() && (width == maxWidth)) {
//				if (DEBUG) System.out.println("  - resizing dialog content");
//				fpPs = fp.getPreferredSize();
//				height = (lPs.height + ((fpPs.height * fpPs.width) / width) + bpPs.height + 50); // add for window title bar and borders
//				if (DEBUG) System.out.println("  - dialog size re-computed: " + width + " x " + height);
//				height = Math.min(height, maxHeight);
//				dialog.setSize(width, height);
//			}
//			else if (fp.getScrollableTracksViewportHeight() && (height == maxHeight)) {
//				if (DEBUG) System.out.println("  - resizing dialog content");
//				fpPs = fp.getPreferredSize();
//				width = (Math.max(lPs.width, Math.max(((fpPs.width * fpPs.height) / height), bpPs.width)) + 10); // add for window borders
//				if (DEBUG) System.out.println("  - dialog size re-computed: " + width + " x " + height);
//				width = Math.min(width, maxWidth);
//				dialog.setSize(width, height);
//			}
//			
//			//	set dialog location
//			dialog.setLocationRelativeTo(topWindow);
//			if (DEBUG) System.out.println("  - dialog configured");
//			
//			if (DEBUG) System.out.println("  - showing dialog");
//			waitForSwingEventQueue();
//			dialog.setVisible(true);
//			if (DEBUG) System.out.println("  - dialog closed");
//		}
//		finally {
//			this.zoomPanel.releaseFeedbackPanel();
//			if (DEBUG) System.out.println("  - zoom removed");
//		}
//	}
//	
//	private static void waitForSwingEventQueue() {
//		if (SwingUtilities.isEventDispatchThread()) return;
//		
//		/*
//		 * If we're not on the event dispatch thread, we have to wait until
//		 * the latter finishes laying out the dialog. Otherwise, there might
//		 * be deadlocks inside synchronized Swing components.
//		 */
//		final Object layoutLock = new Object();
//		synchronized (layoutLock) {
//			
//			//	enqueue wakeup for main thread at end of event dispatch thread
//			SwingUtilities.invokeLater(new Runnable() {
//				public void run() {
//					/*
//					 * this block can only be entered if the main thread has
//					 * reached the wait() command
//					 */
//					synchronized (layoutLock) {
//						if (DEBUG) System.out.println("    - releasing dialog layout lock");
//						layoutLock.notify();
//						if (DEBUG) System.out.println("    - dialog layout lock released");
//					}
//				}
//			});
//			
//			//	wait until Swing event dispatcher has completed dialog layout
//			try {
//				if (DEBUG) System.out.println("  - waiting for Swing event queue ...");
//				if (DEBUG) System.out.println("    - waiting on dialog layout lock");
//				layoutLock.wait();
//			}
//			catch (InterruptedException ie) {
//				if (DEBUG) System.out.println("    - interrupted while waiting on dialog layout lock");
//			}
//		}
//	}
//	
//	private void displayError(String[] errorLines, JDialog dialog) {
//		final JDialog errorDialog = new JDialog(dialog, "Incomplete or Incorrect Feedback", true);
//		errorDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
//		errorDialog.getContentPane().setLayout(new BorderLayout());
//		if (DEBUG) System.out.println("  - error dialog produced");
//		
//		//	build dialog label
//		JLabel label = new JLabel("<HTML><B>The feedback data you have entered in this feedback panel is incomplete or contains errors.<BR>Please see the error message below for details.</B></HTML>", JLabel.LEFT);
//		label.setOpaque(true);
//		label.setBackground(Color.WHITE);
//		label.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.WHITE, 2), BorderFactory.createLineBorder(Color.RED, 1)), BorderFactory.createLineBorder(Color.WHITE, 5)));
//		Font labelFont = label.getFont();
//		label.setFont(labelFont.deriveFont(this.zoomPanel.zoomFactor * labelFont.getSize()));
//		errorDialog.getContentPane().add(label, BorderLayout.NORTH);
//		
//		//	build message
//		StringBuffer errorLabelString = new StringBuffer("<HTML>");
//		for (int e = 0; e < errorLines.length; e++) {
//			if (e != 0)
//				errorLabelString.append("<BR>");
//			String errorLine = errorLines[e];
//			if ((errorLine.length() > 6) && "<html>".equals(errorLine.substring(0, 6).toLowerCase())) {
//				errorLine = errorLine.substring(6);
//				if ((errorLine.length() > 7) && "</html>".equals(errorLine.substring(errorLine.length() - 7).toLowerCase()))
//					errorLine = errorLine.substring(errorLine.length() - 7);
//				errorLabelString.append(errorLine);
//			}
//			else errorLabelString.append(IoTools.prepareForHtml(errorLine));
//		}
//		errorLabelString.append("</HTML>");
//		JLabel errorLabel = new JLabel(errorLabelString.toString(), JLabel.LEFT);
//		errorLabel.setOpaque(true);
//		errorLabel.setBackground(Color.WHITE);
//		errorLabel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.WHITE, 2), BorderFactory.createLineBorder(Color.RED, 1)), BorderFactory.createLineBorder(Color.WHITE, 5)));
//		Font errorLabelFont = errorLabel.getFont();
//		errorLabel.setFont(errorLabelFont.deriveFont(this.zoomPanel.zoomFactor * errorLabelFont.getSize()));
//		errorDialog.getContentPane().add(errorLabel, BorderLayout.CENTER);
//		
//		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
//		JButton button = new JButton("OK");
//		button.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent ae) {
//				errorDialog.dispose();
//			}
//		});
//		Font buttonFont = button.getFont();
//		button.setFont(buttonFont.deriveFont(this.zoomPanel.zoomFactor * buttonFont.getSize()));
//		buttonPanel.add(button);
//		errorDialog.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
//		if (DEBUG) System.out.println("  - buttons produced");
//		
//		//	compute size
//		Dimension lPs = label.getPreferredSize();
//		Dimension elPs = errorLabel.getPreferredSize();
//		Dimension bpPs = buttonPanel.getPreferredSize();
//		int width = (Math.max(lPs.width, Math.max(elPs.width, bpPs.width)) + 10); // add for window borders
//		int height = (lPs.height + elPs.height + bpPs.height + 50); // add for window title bar and borders
//		if (DEBUG) System.out.println("  - error dialog size pre-computed: " + width + " x " + height);
//		
//		//	adjust size
//		width = Math.min(width, this.maxDialogWidth);
//		height = Math.min(height, this.maxDialogHeight);
//		if (DEBUG) System.out.println("  - error dialog content produced");
//		
//		//	set dialog size
//		errorDialog.pack();
//		errorDialog.setSize(width, height);
//		
//		//	wait for pack() to complete
//		waitForSwingEventQueue();
//		
//		//	set dialog location
//		errorDialog.setLocationRelativeTo(dialog);
//		if (DEBUG) System.out.println("  - error dialog configured");
//		
//		if (DEBUG) System.out.println("  - showing error dialog");
//		errorDialog.setVisible(true);
//		if (DEBUG) System.out.println("  - error dialog closed");
//	}
//	
//	/* (non-Javadoc)
//	 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel.FeedbackService#isLocal()
//	 */
//	public boolean isLocal() {
//		return true;
//	}
//	
//	/* (non-Javadoc)
//	 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel.FeedbackService#isMultiRequest()
//	 */
//	public boolean isMultiFeedbackSupported() {
//		return false;
//	}
//
//	/* (non-Javadoc)
//	 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel.FeedbackService#getMultiFeedback(de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel[])
//	 */
//	public void getMultiFeedback(FeedbackPanel[] fps) {
//		throw new UnsupportedOperationException("This feedback service can handle only one feedback request at a time.");
//	}
//	
//	/* (non-Javadoc)
//	 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel.FeedbackService#shutdown()
//	 */
//	public void shutdown() {}
//}