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
 *     * Neither the name of the Universität Karlsruhe (TH) / KIT nor the
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
package de.uka.ipd.idaho.goldenGate.plugin.feedbackService;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.TableModelListener;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.goldenGate.DocumentEditor;
import de.uka.ipd.idaho.goldenGate.DocumentEditorDialog;
import de.uka.ipd.idaho.goldenGate.GoldenGATE;
import de.uka.ipd.idaho.goldenGate.plugins.AbstractDocumentViewer;
import de.uka.ipd.idaho.goldenGate.plugins.AnnotationFilter;
import de.uka.ipd.idaho.goldenGate.util.DialogPanel;

/**
 * List view provider showing annotations that have been flagged in in some way
 * in the specialized feedback service that comes with the same package.
 * 
 * @author sautter
 */
public class FlaggedAnnotationViewer extends AbstractDocumentViewer {
	
	private static final String ALL_FLAGS = "<All Flags>";
	
	private FlaggingFeedbackService ffs;
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentViewer#getViewMenuName()
	 */
	public String getViewMenuName() {
		return "List Flagged Annotations";
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#init()
	 */
	public void init() {
		this.ffs = ((FlaggingFeedbackService) this.parent.getPlugin(FlaggingFeedbackService.class.getName()));
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#isOperational()
	 */
	public boolean isOperational() {
		return (super.isOperational() && (this.ffs != null));
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractDocumentViewer#showDocument(de.uka.ipd.idaho.gamta.MutableAnnotation, de.uka.ipd.idaho.goldenGate.DocumentEditor, java.util.Properties)
	 */
	protected void showDocument(MutableAnnotation doc, DocumentEditor editor, Properties parameters) {
		FlaggedAnnotationDialog fad = new FlaggedAnnotationDialog("Flagged Annotations", doc, editor);
		fad.setVisible(true);
	}
	
	private class FlaggedAnnotationDialog extends DialogPanel {
		
		private FlaggedAnnotationPanel annotationDisplay;
		
		private String originalTitle;
		
		private JComboBox flagFilterSelector = null;
		private JCheckBox showParagraphs = new JCheckBox("Show Paragraphs", false);
		
		FlaggedAnnotationDialog(String title, MutableAnnotation data, DocumentEditor target) {
			super(title, true);
			this.originalTitle = title;
			
			JButton closeButton = new JButton("Close");
			closeButton.setBorder(BorderFactory.createRaisedBevelBorder());
			closeButton.setPreferredSize(new Dimension(100, 21));
			closeButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					FlaggedAnnotationDialog.this.dispose();
				}
			});
			
			//	create editor
			this.annotationDisplay = new FlaggedAnnotationPanel(data, target);
			
			//	create filter selector
			JPanel flagFilterPanel = new JPanel(new BorderLayout());
			JLabel flagFilterLabel = new JLabel("Select flag to show annotations for ");
			String[] flags = ffs.getFlags();
			if (flags.length == 0) {
				flags = new String[1];
				flags[0] = ALL_FLAGS;
			}
			this.flagFilterSelector = new JComboBox(new DefaultComboBoxModel(flags));
			if (flags.length > 1)
				this.flagFilterSelector.insertItemAt(ALL_FLAGS, 0);
			this.flagFilterSelector.setSelectedIndex(0);
			this.flagFilterSelector.setEditable(false);
			this.flagFilterSelector.setBorder(BorderFactory.createLoweredBevelBorder());
			if (flags.length > 1)
				this.flagFilterSelector.addItemListener(new ItemListener() {
					public void itemStateChanged(ItemEvent ie) {
						applyFlagFilter();
					}
				});
			else this.flagFilterSelector.setEnabled(false);
			
			//	initialize display mode selection
			this.showParagraphs.setToolTipText("Select to list paragraphs containing flagged annotations rather than flagged annotations proper");
			this.showParagraphs.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent ie) {
					applyFlagFilter();
				}
			});
			
			flagFilterPanel.add(flagFilterLabel, BorderLayout.WEST);
			flagFilterPanel.add(this.flagFilterSelector, BorderLayout.CENTER);
			flagFilterPanel.add(this.showParagraphs, BorderLayout.EAST);
			
			//	put the whole stuff together
			this.add(flagFilterPanel, BorderLayout.NORTH);
			this.add(this.annotationDisplay, BorderLayout.CENTER);
			this.add(closeButton, BorderLayout.SOUTH);
			
			this.setResizable(true);
			this.setSize(new Dimension(700, 650));
			this.setLocationRelativeTo((target == null) ? ((Component) this.getOwner()) : ((Component) target));
			
			this.applyFlagFilter();
		}
		
		void applyFlagFilter() {
			final String filterFlag = ((this.flagFilterSelector == null) ? "" : (ALL_FLAGS.equals(this.flagFilterSelector.getSelectedItem()) ? "" : this.flagFilterSelector.getSelectedItem().toString()));
			
			//	create filter
			AnnotationFilter filter = new AnnotationFilter() {
				public boolean accept(Annotation annotation) {
					return false;
				}
				public QueriableAnnotation[] getMatches(QueriableAnnotation data) {
					LinkedList matches = new LinkedList();
					if (showParagraphs.isSelected()) {
						QueriableAnnotation[] dataParagraphs = data.getAnnotations(MutableAnnotation.PARAGRAPH_TYPE);
						for (int p = 0; p < dataParagraphs.length; p++) {
							String flag = ffs.getFlag(dataParagraphs[p].getAnnotationID());
							if ((flag != null) && flag.startsWith(filterFlag)) {
								matches.add(dataParagraphs[p]);
								continue;
							}
							QueriableAnnotation[] dataAnnotations = dataParagraphs[p].getAnnotations();
							for (int a = 0; a < dataAnnotations.length; a++) {
								flag = ffs.getFlag(dataAnnotations[a].getAnnotationID());
								if ((flag != null) && flag.startsWith(filterFlag)) {
									matches.add(dataParagraphs[p]);
									break;
								}
							}
						}
					}
					else {
						QueriableAnnotation[] dataAnnotations = data.getAnnotations();
						for (int a = 0; a < dataAnnotations.length; a++) {
							String flag = ffs.getFlag(dataAnnotations[a].getAnnotationID());
							if ((flag != null) && flag.startsWith(filterFlag))
								matches.add(dataAnnotations[a]);
						}
					}
					return ((QueriableAnnotation[]) matches.toArray(new QueriableAnnotation[matches.size()]));
				}
				public MutableAnnotation[] getMutableMatches(MutableAnnotation data) {
					LinkedList matches = new LinkedList();
					if (showParagraphs.isSelected()) {
						MutableAnnotation[] dataParagraphs = data.getMutableAnnotations(MutableAnnotation.PARAGRAPH_TYPE);
						for (int p = 0; p < dataParagraphs.length; p++) {
							String flag = ffs.getFlag(dataParagraphs[p].getAnnotationID());
							if ((flag != null) && flag.startsWith(filterFlag)) {
								matches.add(dataParagraphs[p]);
								continue;
							}
							QueriableAnnotation[] dataAnnotations = dataParagraphs[p].getAnnotations();
							for (int a = 0; a < dataAnnotations.length; a++) {
								flag = ffs.getFlag(dataAnnotations[a].getAnnotationID());
								if ((flag != null) && flag.startsWith(filterFlag)) {
									matches.add(dataParagraphs[p]);
									break;
								}
							}
						}
					}
					else {
						MutableAnnotation[] dataAnnotations = data.getMutableAnnotations();
						for (int a = 0; a < dataAnnotations.length; a++) {
							String flag = ffs.getFlag(dataAnnotations[a].getAnnotationID());
							if ((flag != null) && flag.startsWith(filterFlag))
								matches.add(dataAnnotations[a]);
						}
					}
					return ((MutableAnnotation[]) matches.toArray(new MutableAnnotation[matches.size()]));
				}
				public String getName() {
					return ((filterFlag.length() == 0) ? "Flagged Annotations" : ("Annotations flagged '" + filterFlag + "'"));
				}
				public String getProviderClassName() {
					return "Homegrown";
				}
				public String getTypeLabel() {
					return "Custom Filter";
				}
				public boolean equals(Object obj) {
					return ((obj != null) && filterFlag.equals(obj.toString()));
				}
				public String toString() {
					return filterFlag;
				}
			};
			
			//	apply filter
			this.annotationDisplay.setFilter(filter);
		}
		
		/**
		 * the panel doing the actual displaying
		 * 
		 * @author sautter
		 */
		private class FlaggedAnnotationPanel extends JPanel {
			
			private Dimension editDialogSize = new Dimension(800, 600);
			private Point editDialogLocation = null;
			
			private String[] taggedTypes = {};
			private String[] highlightTypes = {};
			
			private JTable annotationTable;
			
			private AnnotationTray[] annotationTrays;
			private HashMap annotationTraysByID = new HashMap();
			
			private MutableAnnotation data;
			private DocumentEditor target;
			private AnnotationFilter filter = null;
			
			private JRadioButton showMatches = new JRadioButton("Show Matches Only", true);
			private JRadioButton highlightMatches = new JRadioButton("Highlight Matches", false);
			
			private int sortColumn = -1;
			private boolean sortDescending = false;
			
			FlaggedAnnotationPanel(MutableAnnotation data, DocumentEditor target) {
				super(new BorderLayout(), true);
				this.setBorder(BorderFactory.createEtchedBorder());
				
				this.data = data;
				this.target = target;
				
				//	read base layout settings
				this.taggedTypes = ((this.target == null) ? new String[0] : this.target.getTaggedAnnotationTypes());
				this.highlightTypes = ((this.target == null) ? new String[0] : this.target.getHighlightAnnotationTypes());
				
				//	initialize display
				this.annotationTable = new JTable();
				this.annotationTable.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
				this.annotationTable.addMouseListener(new MouseAdapter() {
					public void mouseClicked(MouseEvent me) {
						int clickRowIndex = annotationTable.rowAtPoint(me.getPoint());
						int rowIndex = annotationTable.getSelectedRow();
						if ((clickRowIndex != -1) && ((clickRowIndex < rowIndex) || (clickRowIndex >= (rowIndex + annotationTable.getSelectedRowCount())))) {
							ListSelectionModel lsm = annotationTable.getSelectionModel();
							if (lsm != null) lsm.setSelectionInterval(clickRowIndex, clickRowIndex);
						}
						
						if (me.getButton() != MouseEvent.BUTTON1)
							showContextMenu(me);
						
						else if ((me.getClickCount() > 1) && (annotationTable.getSelectedRowCount() == 1))
							editAnnotation(annotationTrays[clickRowIndex].annotation);
					}
				});
				
				final JTableHeader header = this.annotationTable.getTableHeader();
				header.setReorderingAllowed(false);
				header.addMouseListener(new MouseAdapter() {
					public void mouseClicked(MouseEvent me) {
		                int newSortColumn = header.columnAtPoint(me.getPoint());
		                if (newSortColumn == sortColumn)
		                	sortDescending = !sortDescending;
		                else {
		                	sortDescending = false;
		                	sortColumn = newSortColumn;
		                }
		                sortAnnotations();
					}
				});
				
				this.refreshAnnotations();
				
				JScrollPane annotationTableBox = new JScrollPane(this.annotationTable);
				
				this.showMatches.addItemListener(new ItemListener() {
					public void itemStateChanged(ItemEvent ie) {
						if (showMatches.isSelected()) {
							refreshAnnotations();
						}
					}
				});
				this.highlightMatches.addItemListener(new ItemListener() {
					public void itemStateChanged(ItemEvent ie) {
						if (highlightMatches.isSelected()) {
							refreshAnnotations();
						}
					}
				});
				
				ButtonGroup displayModeButtonGroup = new ButtonGroup();
				displayModeButtonGroup.add(this.showMatches);
				displayModeButtonGroup.add(this.highlightMatches);
				
				JPanel displayModePanel = new JPanel(new GridLayout(1, 2));
				displayModePanel.add(this.showMatches);
				displayModePanel.add(this.highlightMatches);
				
				this.add(displayModePanel, BorderLayout.NORTH);
				this.add(annotationTableBox, BorderLayout.CENTER);
			}
			
			void setFilter(AnnotationFilter filter) {
				this.filter = filter;
				this.refreshAnnotations();
			}
			
			void refreshAnnotations() {
				
				//	apply filter
				MutableAnnotation[] annotations = ((this.filter == null) ? new MutableAnnotation[0] : this.filter.getMutableMatches(this.data));
				this.annotationTrays = new AnnotationTray[annotations.length];
				
				//	set up statistics
				String type = ((annotations.length == 0) ? "" : annotations[0].getType());
				Set matchIDs = new HashSet();
				
				//	check matching annotations
				for (int a = 0; a < annotations.length; a++) {
					if (!type.equals(annotations[a].getType()))
						type = "";
					
					matchIDs.add(annotations[a].getAnnotationID());
					
					if (this.annotationTraysByID.containsKey(annotations[a].getAnnotationID()))
						this.annotationTrays[a] = ((AnnotationTray) this.annotationTraysByID.get(annotations[a].getAnnotationID()));
					
					else {
						this.annotationTrays[a] = new AnnotationTray(annotations[a]);
						this.annotationTraysByID.put(annotations[a].getAnnotationID(), this.annotationTrays[a]);
					}
					
					this.annotationTrays[a].isMatch = true;
				}
				
				//	more than one type
				if (type.length() == 0) {
					this.showMatches.setSelected(true);
					this.showMatches.setEnabled(false);
					this.highlightMatches.setEnabled(false);
					
					setTitle(originalTitle + " - " + annotations.length + " Annotations");
				}
				
				//	all of same type, do match highlight display if required
				else {
					this.showMatches.setEnabled(true);
					this.highlightMatches.setEnabled(true);
					
					//	highlight matches
					if (this.highlightMatches.isSelected()) {
						annotations = this.data.getMutableAnnotations(type);
						this.annotationTrays = new AnnotationTray[annotations.length];
						
						for (int a = 0; a < annotations.length; a++) {
							
							if (this.annotationTraysByID.containsKey(annotations[a].getAnnotationID()))
								this.annotationTrays[a] = ((AnnotationTray) this.annotationTraysByID.get(annotations[a].getAnnotationID()));
							
							else {
								this.annotationTrays[a] = new AnnotationTray(annotations[a]);
								this.annotationTraysByID.put(annotations[a].getAnnotationID(), this.annotationTrays[a]);
							}
							
							this.annotationTrays[a].isMatch = matchIDs.contains(annotations[a].getAnnotationID());
						}
						
						setTitle(originalTitle + " - " + annotations.length + " Annotations, " + matchIDs.size() + " matches");
					}
					
					else setTitle(originalTitle + " - " + annotations.length + " Annotations");
				}
				
				this.annotationTable.setModel(new AnnotationNavigatorTableModel(this.annotationTrays));
				this.annotationTable.getColumnModel().getColumn(0).setPreferredWidth(150);
				this.annotationTable.getColumnModel().getColumn(0).setMaxWidth(150);
				this.annotationTable.getColumnModel().getColumn(1).setPreferredWidth(150);
				this.annotationTable.getColumnModel().getColumn(1).setMaxWidth(150);
				this.annotationTable.getColumnModel().getColumn(2).setPreferredWidth(50);
				this.annotationTable.getColumnModel().getColumn(2).setMaxWidth(50);
				this.annotationTable.getColumnModel().getColumn(3).setPreferredWidth(50);
				this.annotationTable.getColumnModel().getColumn(3).setMaxWidth(50);
				
				this.sortColumn = -1;
				this.sortDescending = false;
				this.refreshDisplay();
			}
			
			void sortAnnotations() {
				Arrays.sort(this.annotationTrays, new Comparator() {
					public int compare(Object o1, Object o2) {
						AnnotationTray at1 = ((AnnotationTray) o1);
						AnnotationTray at2 = ((AnnotationTray) o2);
						int c;
						if (sortColumn == 0)
							c = at1.annotation.getType().compareToIgnoreCase(at2.annotation.getType());
						else if (sortColumn == 1)
							c = (at1.annotation.getStartIndex() - at2.annotation.getStartIndex());
						else if (sortColumn == 2)
							c = (at1.annotation.size() - at2.annotation.size());
						else if (sortColumn == 3)
							c = String.CASE_INSENSITIVE_ORDER.compare(at1.annotation.getValue(), at2.annotation.getValue());
						else c = 0;
						
						return ((sortDescending ? -1 : 1) * ((c == 0) ? AnnotationUtils.compare(at1.annotation, at2.annotation) : c));
					}
				});
				this.refreshDisplay();
			}
			
			void refreshDisplay() {
				for(int i = 0; i < this.annotationTable.getColumnCount();i++)
					this.annotationTable.getColumnModel().getColumn(i).setHeaderValue(this.annotationTable.getModel().getColumnName(i));
				this.annotationTable.getTableHeader().revalidate();
				this.annotationTable.getTableHeader().repaint();
				this.annotationTable.revalidate();
				this.annotationTable.repaint();
				FlaggedAnnotationDialog.this.validate();
			}
			
			void editAnnotation(MutableAnnotation annotation) {
				
				//	create dialog & show
				DocumentEditDialog ded = new DocumentEditDialog(FlaggedAnnotationDialog.this.getDialog(), FlaggedAnnotationViewer.this.parent, target, "Edit Annotation", annotation);
				ded.setVisible(true);
				
				//	finish
				if (ded.committed && (ded.isContentModified() || ded.unFlagged))
					this.refreshAnnotations();
			}
			
			class DocumentEditDialog extends DocumentEditorDialog {
				private boolean committed = false;
				private boolean unFlagged = false;
				private DocumentEditDialog(JDialog owner, GoldenGATE host, DocumentEditor parent, String title, final MutableAnnotation data) {
					super(owner, host, parent, title, data);
					
					JButton okUnFlagButton = new JButton("OK & Un-Flag");
					okUnFlagButton.setBorder(BorderFactory.createRaisedBevelBorder());
					okUnFlagButton.setPreferredSize(new Dimension(100, 21));
					okUnFlagButton.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							DocumentEditDialog.this.committed = true;
							DocumentEditDialog.this.unFlagged = true;
							DocumentEditDialog.this.documentEditor.writeChanges();
							ffs.removeFlag(data.getAnnotationID());
							if (showParagraphs.isSelected()) {
								QueriableAnnotation[] dataAnnotations = data.getAnnotations();
								for (int a = 0; a < dataAnnotations.length; a++)
									ffs.removeFlag(dataAnnotations[a].getAnnotationID());
							}
							DocumentEditDialog.this.dispose();
						}
					});
					this.mainButtonPanel.add(okUnFlagButton);
					
					JButton okButton = new JButton("OK");
					okButton.setBorder(BorderFactory.createRaisedBevelBorder());
					okButton.setPreferredSize(new Dimension(100, 21));
					okButton.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							DocumentEditDialog.this.committed = true;
							DocumentEditDialog.this.documentEditor.writeChanges();
							DocumentEditDialog.this.dispose();
						}
					});
					this.mainButtonPanel.add(okButton);
					
					JButton cancelButton = new JButton("Cancel");
					cancelButton.setBorder(BorderFactory.createRaisedBevelBorder());
					cancelButton.setPreferredSize(new Dimension(100, 21));
					cancelButton.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							DocumentEditDialog.this.dispose();
						}
					});
					this.mainButtonPanel.add(cancelButton);
					
					for (int t = 0; t < taggedTypes.length; t++)
						this.documentEditor.setAnnotationTagVisible(taggedTypes[t], true);
					for (int t = 0; t < highlightTypes.length; t++)
						this.documentEditor.setAnnotationValueHighlightVisible(highlightTypes[t], true);
					
					this.setSize(editDialogSize);
					if (editDialogLocation == null) this.setLocationRelativeTo(FlaggedAnnotationDialog.this);
					else this.setLocation(editDialogLocation);
				}
				
				public void dispose() {
					editDialogSize = this.getSize();
					editDialogLocation = this.getLocation(editDialogLocation);
					taggedTypes = this.documentEditor.getTaggedAnnotationTypes();
					highlightTypes = this.documentEditor.getHighlightAnnotationTypes();
					
					super.dispose();
				}
			}
			
			void showContextMenu(MouseEvent me) {
				int row = this.annotationTable.getSelectedRow();
				if (row == -1) return;
				
				int rows = this.annotationTable.getSelectedRowCount();
				final AnnotationTray tray = this.annotationTrays[row];
				final AnnotationTray[] trays = new AnnotationTray[rows];
				System.arraycopy(annotationTrays, row, trays, 0, rows);
				Arrays.sort(trays, new Comparator() {
					private Comparator comp = AnnotationUtils.getComparator(data.getAnnotationNestingOrder());
					public int compare(Object o1, Object o2) {
						return this.comp.compare(((AnnotationTray) o1).annotation, ((AnnotationTray) o1).annotation);
					}
				});
				
				JMenu menu = new JMenu();
				JMenuItem mi;
				
				mi = new JMenuItem("Edit");
				mi.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						if (trays.length == 1)
							editAnnotation(tray.annotation);
						
						else {
							MutableAnnotation temp = data.addAnnotation(TEMP_ANNOTATION_TYPE, trays[0].annotation.getStartIndex(), (trays[trays.length - 1].annotation.getEndIndex() - trays[0].annotation.getStartIndex()));
							editAnnotation(temp);
							data.removeAnnotation(temp);
						}
					}
				});
				menu.add(mi);
				
				mi = new JMenuItem("Un-Flag");
				mi.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						for (int r = 0; r < trays.length; r++) {
							ffs.removeFlag(trays[r].annotation.getAnnotationID());
							if (showParagraphs.isSelected()) {
								QueriableAnnotation[] dataAnnotations = trays[r].annotation.getAnnotations();
								for (int a = 0; a < dataAnnotations.length; a++)
									ffs.removeFlag(dataAnnotations[a].getAnnotationID());
							}
						}
						refreshAnnotations();
					}
				});
				menu.add(mi);
				
				if (!showParagraphs.isSelected()) {
					mi = new JMenuItem("Remove");
					mi.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							for (int r = 0; r < trays.length; r++)
								data.removeAnnotation(trays[r].annotation);
							refreshAnnotations();
						}
					});
					menu.add(mi);
					
					if (rows == 1) {
						mi = new JMenuItem("Remove All");
						mi.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								String value = tray.annotation.getValue();
								int annotationCount = 0;
								for (int a = 0; a < annotationTrays.length; a++)
									if (annotationTrays[a].annotation.getValue().equals(value)) {
										annotationCount ++;
										data.removeAnnotation(annotationTrays[a].annotation);
									}
								if (annotationCount > 0) refreshAnnotations();
							}
						});
						menu.add(mi);
					}
					
					mi = new JMenuItem("Delete");
					mi.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							for (int r = trays.length; r > 0; r--)
								data.removeTokens(trays[r - 1].annotation);
							refreshAnnotations();
						}
					});
					menu.add(mi);
				}
				
				menu.getPopupMenu().show(this.annotationTable, me.getX(), me.getY());
			}
			
			private class AnnotationTray {
				MutableAnnotation annotation;
				boolean isMatch = false;
				AnnotationTray(MutableAnnotation annotation) {
					this.annotation = annotation;
				}
			}
			
			private class AnnotationNavigatorTableModel implements TableModel {
				private AnnotationTray[] annotations;
				private boolean isMatchesOnly = true;
				AnnotationNavigatorTableModel(AnnotationTray[] annotations) {
					this.annotations = annotations;
					for (int a = 0; a < this.annotations.length; a++)
						this.isMatchesOnly = (this.isMatchesOnly && this.annotations[a].isMatch);
				}
				public void addTableModelListener(TableModelListener l) {}
				public Class getColumnClass(int columnIndex) {
					return String.class;
				}
				public int getColumnCount() {
					return 5;
				}
				public String getColumnName(int columnIndex) {
					String sortExtension = ((columnIndex == sortColumn) ? (sortDescending ? " (d)" : " (a)") : "");
					if (columnIndex == 0)
						return ("Flag" + sortExtension);
					else if (columnIndex == 1)
						return ("Type" + sortExtension);
					else if (columnIndex == 2)
						return ("Start" + sortExtension);
					else if (columnIndex == 3)
						return ("Size" + sortExtension);
					else if (columnIndex == 4)
						return ("Value" + sortExtension);
					else return null;
				}
				public int getRowCount() {
					return this.annotations.length;
				}
				public Object getValueAt(int rowIndex, int columnIndex) {
					Annotation annotation = this.annotations[rowIndex].annotation;
					if (this.isMatchesOnly || !this.annotations[rowIndex].isMatch) {
						if (columnIndex == 0)
							return ffs.getFlag(annotation.getAnnotationID());
						else if (columnIndex == 1)
							return annotation.getType();
						else if (columnIndex == 2)
							return "" + annotation.getStartIndex();
						else if (columnIndex == 3)
							return "" + annotation.size();
						else if (columnIndex == 4)
							return annotation.getValue();
						else return null;
					}
					else {
						String value = null;
						if (columnIndex == 0)
							value = ffs.getFlag(annotation.getAnnotationID());
						else if (columnIndex == 1)
							value = annotation.getType();
						else if (columnIndex == 2)
							value = "" + annotation.getStartIndex();
						else if (columnIndex == 3)
							value = "" + annotation.size();
						else if (columnIndex == 4)
							value = annotation.getValue();
						return ((value == null) ? null : ("<HTML><B>" + value + "</B></HTML>"));
					}
				}
				public boolean isCellEditable(int rowIndex, int columnIndex) {
					return false;
				}
				public void removeTableModelListener(TableModelListener l) {}
				public void setValueAt(Object newValue, int rowIndex, int columnIndex) {}
			}
		}
	}
}