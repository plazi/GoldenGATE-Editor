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
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.ToolTipManager;
import javax.swing.text.JTextComponent;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.DocumentRoot;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.TokenSequenceUtils;
import de.uka.ipd.idaho.goldenGate.DocumentEditor;
import de.uka.ipd.idaho.goldenGate.DocumentEditorDialog;
import de.uka.ipd.idaho.goldenGate.GoldenGATE;
import de.uka.ipd.idaho.goldenGate.plugins.AbstractDocumentViewer;
import de.uka.ipd.idaho.goldenGate.util.AttributeEditorDialog;
import de.uka.ipd.idaho.goldenGate.util.DialogPanel;

/**
 * Document viewer showing the document's structure as a tree of annotations,
 * layn out according to their nesting order. This view allows for merging and
 * editing annotations, among others.
 * 
 * @author sautter
 */
public class AnnotationTreeViewer extends AbstractDocumentViewer {
	
	//	root icon is constant, can be stored in ImageIcon
	private static final String ROOT_ICON_FILE_NAME = "root.gif";
	private ImageIcon rootIcon;
	
	//	need to store other icons as BufferedImages so background can be added dynamically
	private static final String CLOSED_IMAGE_FILE_NAME = "closed.bmp";
	private BufferedImage closedImage;
	private static final String OPEN_IMAGE_FILE_NAME = "open.bmp";
	private BufferedImage openImage;
	private static final String LEAF_IMAGE_FILE_NAME = "leaf.bmp";
	private BufferedImage leafImage;
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractDocumentViewer#init()
	 */
	public void init() {
		try {
			ImageIO.setUseCache(false);
			InputStream is;
			
			is = this.dataProvider.getInputStream(CLOSED_IMAGE_FILE_NAME);
			this.closedImage = ImageIO.read(is);
			is.close();
			
			is = this.dataProvider.getInputStream(OPEN_IMAGE_FILE_NAME);
			this.openImage = ImageIO.read(is);
			is.close();
			
			is = this.dataProvider.getInputStream(LEAF_IMAGE_FILE_NAME);
			this.leafImage = ImageIO.read(is);
			is.close();
			
			is = this.dataProvider.getInputStream(ROOT_ICON_FILE_NAME);
			this.rootIcon = new ImageIcon(ImageIO.read(is));
			is.close();
			
		} catch (IOException e) {}
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractDocumentViewer#getViewMenuName()
	 */
	public String getViewMenuName() {
		return "Tree View Annotations";
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractDocumentViewer#showDocument(de.uka.ipd.idaho.gamta.MutableAnnotation, de.uka.ipd.idaho.goldenGate.DocumentEditor, java.util.Properties)
	 */
	protected void showDocument(MutableAnnotation doc, DocumentEditor editor, Properties parameters) {
		AnnotationTreeViewDialog atvd = new AnnotationTreeViewDialog(editor, doc);
		atvd.setSize(600, 600);
		atvd.setLocationRelativeTo(DialogPanel.getTopWindow());
		atvd.setVisible(true);
	}
	
	private class AnnotationTreeViewDialog extends DialogPanel {
		
		private Dimension attributeDialogSize = new Dimension(400, 300);
		private Point attributeDialogLocation = null;
		
		private Dimension editDialogSize = new Dimension(800, 600);
		private Point editDialogLocation = null;
		
		private String[] taggedTypes = {};
		private String[] highlightTypes = {};
		
		private JTree annotationTreeView = new JTree();
		private AnnotationTreeNode annotationTreeRoot;
		
		private DocumentEditor target;
		private MutableAnnotation data;
		
		AnnotationTreeViewDialog(DocumentEditor target, MutableAnnotation data) {
			super("Tree View Annotations", true);
			this.target = target;
			this.data = data;
			
			//	read base layout settings
			this.taggedTypes = ((this.target == null) ? new String[0] : this.target.getTaggedAnnotationTypes());
			this.highlightTypes = ((this.target == null) ? new String[0] : this.target.getHighlightAnnotationTypes());
			
			//	initialize main buttons
			JButton closeButton = new JButton("Close");
			closeButton.setBorder(BorderFactory.createRaisedBevelBorder());
			closeButton.setPreferredSize(new Dimension(100, 21));
			closeButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					close();
				}
			});
			
			//	initialize tree view
			this.annotationTreeView.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
			this.annotationTreeView.setCellRenderer(new AnnotationTreeCellRenderer(this.target));
			this.annotationTreeRoot = produceRoot(this.data);
			this.annotationTreeView.setModel(new DefaultTreeModel(this.annotationTreeRoot));
			ToolTipManager.sharedInstance().registerComponent(this.annotationTreeView);
			
			//	add context menu
			this.annotationTreeView.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent me) {
					if (me.getButton() != MouseEvent.BUTTON1)
						showContextMenu(me);
				}
			});
			
			//	put tree view in scroll pane
			JScrollPane treeBox = new JScrollPane(this.annotationTreeView);
			this.add(treeBox, BorderLayout.CENTER);
			this.add(closeButton, BorderLayout.SOUTH);
			this.validate();
		}
		
		void showContextMenu(MouseEvent me) {
			TreePath[] selectedPaths = this.annotationTreeView.getSelectionPaths();
			if ((selectedPaths == null) || (selectedPaths.length == 0)) return;
			final MutableAnnotation annotation = ((AnnotationTreeNode) selectedPaths[0].getLastPathComponent()).data;
			
			JMenu menu = new JMenu();
			JMenuItem mi;
			
			mi = new JMenuItem("Edit");
			mi.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					
					//	create dialog
					DocumentEditDialog ded = new DocumentEditDialog(AnnotationTreeViewDialog.this.getDialog(), AnnotationTreeViewer.this.parent, target, "Edit Annotation", annotation);
					
					//	position and show dialog
					ded.setSize(editDialogSize);
					if (editDialogLocation == null) ded.setLocationRelativeTo(DialogPanel.getTopWindow());
					else ded.setLocation(editDialogLocation);
					ded.setVisible(true);
					
					//	finish
					if (ded.isContentModified()) buildTree();
				}
			});
			menu.add(mi);
			
			mi = new JMenuItem("Edit Attributes");
			mi.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					AttributeEditorDialog aed = new AttributeEditorDialog(AnnotationTreeViewDialog.this.getDialog(), "Edit Annotation Attributes", annotation, data) {
						public void dispose() {
							attributeDialogSize = this.getSize();
							attributeDialogLocation = this.getLocation(attributeDialogLocation);
							super.dispose();
						}
					};
					
					//	position and show dialog
					aed.setSize(attributeDialogSize);
					if (attributeDialogLocation == null) aed.setLocationRelativeTo(DialogPanel.getTopWindow());
					else aed.setLocation(attributeDialogLocation);
					aed.setVisible(true);
				}
			});
			menu.add(mi);
			menu.addSeparator();
			
			mi = new JMenuItem("Rename");
			mi.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					String[] types = ((target == null) ? data.getAnnotationTypes() : target.getAnnotationTypes());
					Arrays.sort(types, ANNOTATION_TYPE_ORDER);
					
					RenameAnnotationDialog rad = new RenameAnnotationDialog(annotation.getType(), types);
					rad.setVisible(true);
					if (rad.targetType == null) return;
					
					String newType = rad.targetType.trim();
					if ((newType.length() != 0) && !newType.equals(annotation.getType())) {
						annotation.changeTypeTo(newType);
						buildTree();
					}
				}
			});
			menu.add(mi);
			
			mi = new JMenuItem("Remove");
			mi.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					data.removeAnnotation(annotation);
					buildTree();
				}
			});
			menu.add(mi);
			
			mi = new JMenuItem("Delete");
			mi.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					data.removeTokens(annotation);
					buildTree();
				}
			});
			menu.add(mi);
			
			if (selectedPaths.length > 1) {
				final Annotation[] selectedAnnotations = new Annotation[selectedPaths.length];
				String lastType = null;
				boolean canMerge = true;
				for (int s = 0; s < selectedPaths.length; s++) {
					selectedAnnotations[s] = ((AnnotationTreeNode) selectedPaths[s].getLastPathComponent()).data;
					if (lastType != null) canMerge = (canMerge && selectedAnnotations[s].getType().equals(lastType));
					lastType = selectedAnnotations[s].getType();
				}
				if (canMerge) {
					menu.addSeparator();
					mi = new JMenuItem("Merge Annotations");
					mi.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							int start = data.size();
							int end = 0;
							
							for (int s = 0; s < selectedAnnotations.length; s++) {
								if (start > selectedAnnotations[s].getStartIndex()) start = selectedAnnotations[s].getStartIndex();
								if (end < selectedAnnotations[s].getEndIndex()) end = selectedAnnotations[s].getEndIndex();
							}
							
							Annotation mergedAnnotation = data.addAnnotation(selectedAnnotations[0].getType(), start, (end - start));
							
							for (int s = 0; s < selectedAnnotations.length; s++) {
								mergedAnnotation.copyAttributes(selectedAnnotations[s]);
								data.removeAnnotation(selectedAnnotations[s]);
							}
							buildTree();
						}
					});
					menu.add(mi);
				}
			}
			
			menu.getPopupMenu().show(this.annotationTreeView, me.getX(), me.getY());
		}
		
		void close() {
			this.dispose();
		}
		
		void buildTree() {
			ArrayList expandedPathsList = new ArrayList();
			Enumeration expandedPathEnumeration = this.annotationTreeView.getExpandedDescendants(new TreePath(this.annotationTreeRoot));
			while ((expandedPathEnumeration != null) && expandedPathEnumeration.hasMoreElements())
				expandedPathsList.add(expandedPathEnumeration.nextElement());
			TreePath[] expandedPaths = ((TreePath[]) expandedPathsList.toArray(new TreePath[expandedPathsList.size()]));
			
//			this.annotationTreeRoot = produceRoot(this.data, this.target);
			this.annotationTreeRoot = produceRoot(this.data);
			this.annotationTreeView.setModel(new DefaultTreeModel(this.annotationTreeRoot));
			
			for (int e = 0; e < expandedPaths.length; e++)
				this.annotationTreeView.expandPath(expandedPaths[e]);
		}
		
		private class DocumentEditDialog extends DocumentEditorDialog {
			DocumentEditDialog(JDialog owner, GoldenGATE host, DocumentEditor parent, String title, MutableAnnotation docPart) {
				super(owner, host, parent, title, docPart);
				
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
				
				for (int t = 0; t < taggedTypes.length; t++)
					this.documentEditor.setAnnotationTagVisible(taggedTypes[t], true);
				for (int t = 0; t < highlightTypes.length; t++)
					this.documentEditor.setAnnotationValueHighlightVisible(highlightTypes[t], true);
				
				this.setSize(editDialogSize);
				if (editDialogLocation == null) this.setLocationRelativeTo((parent == null) ? ((Component) this.getOwner()) : ((Component) parent));
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
		
		private class RenameAnnotationDialog extends DialogPanel {
			
			private String targetType;
			private JComboBox targetTypeSelector;
			
			private boolean keyPressed = false;
			
			RenameAnnotationDialog(String sourceType, String[] existingTypes) {
				super("Rename Annotation", true);
				
				JLabel sourceTypeLabel = new JLabel(" " + sourceType);
				sourceTypeLabel.setBorder(BorderFactory.createLoweredBevelBorder());
				
				this.targetTypeSelector = new JComboBox(existingTypes);
				this.targetTypeSelector.setBorder(BorderFactory.createLoweredBevelBorder());
				this.targetTypeSelector.setEditable(true);
				this.targetTypeSelector.setSelectedItem(sourceType);
				this.targetTypeSelector.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						if (keyPressed && isVisible() && !targetTypeSelector.isPopupVisible()) commit();
					}
				});
				((JTextComponent) this.targetTypeSelector.getEditor().getEditorComponent()).addKeyListener(new KeyAdapter() {
					public void keyPressed(KeyEvent ke) {
						keyPressed = true;
					}
					public void keyReleased(KeyEvent ke) {
						keyPressed = false;
					}
				});
				
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
				selectorPanel.add(new JLabel("Current Annotation Type"), gbc.clone());
				gbc.gridx = 1;
				gbc.weightx = 1;
				selectorPanel.add(sourceTypeLabel, gbc.clone());
				
				gbc.gridy = 1;
				gbc.gridx = 0;
				gbc.weightx = 0;
				selectorPanel.add(new JLabel("Rename Annotation To"), gbc.clone());
				gbc.gridx = 1;
				gbc.weightx = 1;
				selectorPanel.add(this.targetTypeSelector, gbc.clone());
				
				//	initialize main buttons
				JButton commitButton = new JButton("Rename");
				commitButton.setBorder(BorderFactory.createRaisedBevelBorder());
				commitButton.setPreferredSize(new Dimension(100, 21));
				commitButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						commit();
					}
				});
				
				JButton abortButton = new JButton("Cancel");
				abortButton.setBorder(BorderFactory.createRaisedBevelBorder());
				abortButton.setPreferredSize(new Dimension(100, 21));
				abortButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						RenameAnnotationDialog.this.dispose();
					}
				});
				
				JPanel mainButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
				mainButtonPanel.add(commitButton);
				mainButtonPanel.add(abortButton);
				
				//	put the whole stuff together
				this.setLayout(new BorderLayout());
				this.add(selectorPanel, BorderLayout.CENTER);
				this.add(mainButtonPanel, BorderLayout.SOUTH);
				
				this.setResizable(true);
				this.setSize(new Dimension(400, 120));
			}
			
			void commit() {
				Object item = targetTypeSelector.getSelectedItem();
				this.targetType = ((item == null) ? "" : item.toString());
				this.dispose();
			}
		}
	}
	
	private AnnotationTreeNode produceRoot(MutableAnnotation data/*, DocumentEditor editor*/) {
		AnnotationTreeNode root = new AnnotationTreeNode(null, data);
		
		MutableAnnotation[] annotations = data.getMutableAnnotations();
		Arrays.sort(annotations, AnnotationUtils.getComparator(data.getAnnotationNestingOrder()));
		
		//	hide first Annotation if equal to data
		if ((annotations.length != 0) && (annotations[0].size() == data.size()) && DocumentRoot.DOCUMENT_TYPE.equals(annotations[0].getType())) {
			MutableAnnotation[] containedAnnotations = new MutableAnnotation[annotations.length - 1];
			if (containedAnnotations.length != 0)
				System.arraycopy(annotations, 1, containedAnnotations, 0, containedAnnotations.length);
			root.children = produceChildren(root, data, containedAnnotations);
		}
		
		//	show all Annotations otherwise
		else root.children = produceChildren(root, data, annotations);
		
		return root;
	}
	
	private AnnotationTreeNode[] produceChildren(AnnotationTreeNode node, MutableAnnotation annotation, MutableAnnotation[] containedAnnotations) {
		ArrayList childList = new ArrayList();
		int lastEndIndex = 0;
		for (int a = 0; a < containedAnnotations.length; a++) {
			if (containedAnnotations[a].getStartIndex() >= lastEndIndex) {
				
				//	remember document position reached so far
				lastEndIndex = containedAnnotations[a].getEndIndex();
				
				//	produce node for current Annotation
				AnnotationTreeNode atn = new AnnotationTreeNode(node, containedAnnotations[a]);
				childList.add(atn);
				
				//	find end of Annotations contained in current one
				int containedEndIndex = (a+1);
				while ((containedEndIndex < containedAnnotations.length) && (containedAnnotations[containedEndIndex].getStartIndex() < lastEndIndex))
					containedEndIndex++;
				
				//	copy Annotations contained in current one 
				MutableAnnotation[] childContainedAnnotations = new MutableAnnotation[containedEndIndex - (a+1)];
				if (childContainedAnnotations.length != 0)
					System.arraycopy(containedAnnotations, (a+1), childContainedAnnotations, 0, childContainedAnnotations.length);
				
				//	produce children for current Annotation
				atn.children = produceChildren(atn, containedAnnotations[a], childContainedAnnotations);
			}
		}
		return ((AnnotationTreeNode[]) childList.toArray(new AnnotationTreeNode[childList.size()]));
	}
	
	private class AnnotationTreeNode implements TreeNode {
		
		private MutableAnnotation data;
		
		private AnnotationTreeNode parentNode;
		private AnnotationTreeNode[] children;
		
		private AnnotationTreeNode(AnnotationTreeNode parentNode, MutableAnnotation data) {
			this.parentNode = parentNode;
			this.data = data;
		}
		
		/*
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		public boolean equals(Object obj) {
			return ((obj != null) && (obj instanceof AnnotationTreeNode) && ((AnnotationTreeNode) obj).data.getAnnotationID().equals(this.data.getAnnotationID()));
		}
		
		/*
		 * @see java.lang.Object#hashCode()
		 */
		public int hashCode() {
			return this.data.getAnnotationID().hashCode();
		}
		
		/*
		 * @see javax.swing.tree.TreeNode#children()
		 */
		public Enumeration children() {
			return new Vector(Arrays.asList(this.children)).elements();
		}
		
		/*
		 * @see javax.swing.tree.TreeNode#getAllowsChildren()
		 */
		public boolean getAllowsChildren() {
			return true;
		}
		
		/*
		 * @see javax.swing.tree.TreeNode#getChildAt(int)
		 */
		public TreeNode getChildAt(int childIndex) {
			return this.children[childIndex];
		}
		
		/*
		 * @see javax.swing.tree.TreeNode#getChildCount()
		 */
		public int getChildCount() {
			return this.children.length;
		}
		
		/*
		 * @see javax.swing.tree.TreeNode#getIndex(javax.swing.tree.TreeNode)
		 */
		public int getIndex(TreeNode node) {
			if ((node != null) && (node instanceof AnnotationTreeNode)) {
				String nodeAnnotationID = ((AnnotationTreeNode) node).data.getAnnotationID();
				for (int c = 0; c < this.children.length; c++)
					if (this.children[c].data.getAnnotationID().equals(nodeAnnotationID)) return c;
			}
			return -1;
		}
		
		/*
		 * @see javax.swing.tree.TreeNode#getParent()
		 */
		public TreeNode getParent() {
			return this.parentNode;
		}
		
		/*
		 * @see javax.swing.tree.TreeNode#isLeaf()
		 */
		public boolean isLeaf() {
			return (this.children.length == 0);
		}
		
		/*
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			return (this.data.getType() + " (" + this.data.getStartIndex() + ", " + this.data.getEndIndex() + ") " + ((this.data.size() < 11) ? this.data.getValue() : (TokenSequenceUtils.concatTokens(this.data, 0, 10) + " ...")));
		}
	}
	
	private class AnnotationTreeCellRenderer extends DefaultTreeCellRenderer {
		
		private DocumentEditor formatProvider;
		
		AnnotationTreeCellRenderer(DocumentEditor formatProvider) {
			this.formatProvider = formatProvider;
		}
		
		/*
		 * @see javax.swing.tree.DefaultTreeCellRenderer#getTreeCellRendererComponent(javax.swing.JTree, java.lang.Object, boolean, boolean, boolean, int, boolean)
		 */
		public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
			super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
			
			//	set root icon
			if ((AnnotationTreeViewer.this.rootIcon != null) && (value != null) && (value instanceof TreeNode) && (((TreeNode) value).getParent() == null))
				this.setIcon(AnnotationTreeViewer.this.rootIcon);
			
			//	set tooltip
			if ((value != null) && (value instanceof AnnotationTreeNode)) {
				
				//	get color
				int color = ((this.formatProvider == null) ? DocumentEditor.getActiveAnnotationColor(((AnnotationTreeNode) value).data.getType()) : this.formatProvider.getAnnotationColor(((AnnotationTreeNode) value).data.getType())).getRGB();
				
				//	set icon
				if (((AnnotationTreeNode) value).getParent() == null) this.setIcon(AnnotationTreeViewer.this.rootIcon);
				else if (leaf) this.setIcon(produceIcon(AnnotationTreeViewer.this.leafImage, color));
				else if (expanded) this.setIcon(produceIcon(AnnotationTreeViewer.this.openImage, color));
				else this.setIcon(produceIcon(AnnotationTreeViewer.this.closedImage, color));
				
				//	add type and range
				StringBuffer tooltip = new StringBuffer("<HTML><B>" + value.toString() + "</B>");
				
				//	add attributes
				String[] aNames = ((AnnotationTreeNode) value).data.getAttributeNames();
				for (int a = 0; a < aNames.length; a++) {
					Object aValue = ((AnnotationTreeNode) value).data.getAttribute(aNames[a]);
					if (aValue != null) tooltip.append("<BR>&nbsp;&nbsp;- " + aNames[a] + " = " + aValue.toString());
				}
				
				//	add value
				tooltip.append("<BR>");
				if (((AnnotationTreeNode) value).data.size() <= 25)
					tooltip.append(((AnnotationTreeNode) value).data.getValue());
				else {
					int length = Math.min((((AnnotationTreeNode) value).data.size() / 2), 25);
					tooltip.append(TokenSequenceUtils.concatTokens(((AnnotationTreeNode) value).data, 0, length));
					tooltip.append("<BR> ... <BR>");
					tooltip.append(TokenSequenceUtils.concatTokens(((AnnotationTreeNode) value).data, (((AnnotationTreeNode) value).data.size() - length), length));
				}
				
				//	set tooltip
				tooltip.append("</HTML>");
				this.setToolTipText(tooltip.toString());
			}
			
			return this;
		}
		
		private final int white = Color.WHITE.getRGB();
		private ImageIcon produceIcon(BufferedImage source, int color) {
			BufferedImage iconImage = new BufferedImage(source.getWidth(), source.getHeight(), source.getType());
			for (int x = 0; x < source.getWidth(); x++)
				for (int y = 0; y < source.getHeight(); y++) {
					int sourceColor = source.getRGB(x, y);
					if (white == sourceColor) 
						iconImage.setRGB(x, y, color);
					else iconImage.setRGB(x, y, sourceColor);
				}
			return new ImageIcon(iconImage);
		}
	}
}
