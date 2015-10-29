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
package de.uka.ipd.idaho.goldenGate.plugin.editorExtensions;


import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListDataListener;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.TokenSequenceUtils;
import de.uka.ipd.idaho.gamta.util.constants.LiteratureConstants;
import de.uka.ipd.idaho.goldenGate.DocumentEditor;
import de.uka.ipd.idaho.goldenGate.observers.AnnotationObserver;
import de.uka.ipd.idaho.goldenGate.observers.DisplayObserver;
import de.uka.ipd.idaho.goldenGate.plugins.AbstractDocumentEditorExtension;
import de.uka.ipd.idaho.goldenGate.plugins.Resource;

/**
 * This plugin adds a navigation bar to the document editor. The navigation bar
 * is essentially a list of annotations of a given type, e.g. paragraphs. The
 * listed annotations currently in the visible area of the editing window are
 * highlighted. The list automatically scrolls along with the editing window. A
 * click on an annotation in the list, in turn, will cause the editing window to
 * scroll to the clicked annotation, leaving the cursor behind.
 * 
 * @author sautter
 */
public class DocumentNavigator extends AbstractDocumentEditorExtension {
	
	/* 
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentEditorExtension#getExtensionPanel(de.goldenGate.DocumentEditor)
	 */
	public JPanel getExtensionPanel(DocumentEditor editor) {
		return new DocumentNavigatorPanel(editor);
	}
	
	private class DocumentNavigatorPanel extends JPanel implements LiteratureConstants {
		private DocumentEditor editor;
		private QueriableAnnotation doc;
		
		private Annotation[] annotations = new Annotation[0];
		private boolean[] highlighted = new boolean[0];
		private String type = PARAGRAPH_TYPE;
		
		private JComboBox typeSelector = new JComboBox();
		private JList list = new JList();
		private JScrollPane listBox = new JScrollPane(this.list);
		
		private boolean refreshImmediately = true;
		private boolean refreshRequired = false;
		
		DocumentNavigatorPanel(DocumentEditor target) {
			super(new BorderLayout(), true);
			this.setPreferredSize(new Dimension(200, 100));
			
			this.editor = target;
			this.doc = this.editor.getContent();
			
			target.addAnnotationObserver(new AnnotationObserver() {
				public void annotationAdded(QueriableAnnotation doc, Annotation annotation, Resource source) {
					if (refreshImmediately) {
						if (type.equals(annotation.getType()))
							refreshList();
						refreshSelector();
					}
					else refreshRequired = true;
				}
				public void annotationRemoved(QueriableAnnotation doc, Annotation annotation, Resource source) {
					if (refreshImmediately) {
						if (type.equals(annotation.getType()))
							refreshList();
						refreshSelector();
					}
					else refreshRequired = true;
				}
				public void annotationTypeChanged(QueriableAnnotation doc, Annotation annotation, String oldType, Resource source) {
					if (refreshImmediately) {
						if (type.equals(oldType) || type.equals(annotation.getType()))
							refreshList();
						refreshSelector();
					}
					else refreshRequired = true;
				}
				public void annotationAttributeChanged(QueriableAnnotation doc, Annotation annotation, String attributeName, Object oldValue, Resource source) {}
			});
			
			this.refreshList();
			this.list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			this.list.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent me) {
					int index = list.getSelectedIndex();
					if (index != -1)
						editor.scrollToToken(annotations[index].getStartIndex(), true, false);
				}
			});
			this.list.setModel(new AnnotationListModel());
			
			this.refreshSelector();
			this.typeSelector.setEditable(false);
			this.typeSelector.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent ie) {
					Object selected = typeSelector.getSelectedItem();
					if ((selected != null) && !type.equals(selected)) {
						type = selected.toString();
						refreshList();
					}
				}
			});
			
			target.addDisplayObserver(new DisplayObserver() {
				public void displayPositionChanged(int topIndex, int bottomIndex) {
					refreshHighlights(topIndex, bottomIndex);
				}
				public void displayLocked() {
					refreshImmediately = false;
				}
				public void displayUnlocked() {
					refreshImmediately = true;
					if (refreshRequired) {
						refreshList();
						refreshSelector();
						refreshRequired = false;
					}
				}
			});
			
			this.add(this.typeSelector, BorderLayout.NORTH);
			this.add(this.listBox, BorderLayout.CENTER);
		}
		
		void refreshSelector() {
			String[] types = this.doc.getAnnotationTypes();
			Arrays.sort(types, ANNOTATION_TYPE_ORDER);
			this.typeSelector.setModel(new DefaultComboBoxModel(types));
			this.typeSelector.setSelectedItem(this.type);
		}
		
		void refreshList() {
			this.annotations = this.doc.getAnnotations(this.type);
			this.list.setModel(new AnnotationListModel());
			this.highlighted = new boolean[this.annotations.length];
			this.refreshHighlights();
		}
		
		void refreshHighlights() {
			int top = this.editor.getTopTokenIndex();
			int bottom = this.editor.getBottomTokenIndex();
			this.refreshHighlights(top, bottom);
		}
		
		void refreshHighlights(int top, int bottom) {
			int listTop = -1;
			int listBottom = -1;
			
			for (int a = 0; a < this.annotations.length; a++) {
				this.highlighted[a] = ((this.annotations[a].getStartIndex() < bottom) && (this.annotations[a].getEndIndex() > top));
				if (this.highlighted[a] && (listTop == -1))
					listTop = Math.max(0, (a - 2));
				else if (!this.highlighted[a] && (listTop != -1) && (listBottom == -1))
					listBottom = Math.min((a + 2), (this.annotations.length - 1));
			}
			
			JViewport listViewPort = this.listBox.getViewport();
			
			Point listBottomPoint = this.list.indexToLocation(listBottom);
			int bottomVisible = this.list.locationToIndex(new Point(listViewPort.getViewPosition().x, (listViewPort.getViewPosition().y + listViewPort.getExtentSize().height)));
			if ((bottomVisible < listBottom) && (listBottomPoint != null))
				listViewPort.setViewPosition(new Point(listViewPort.getViewPosition().x, (listBottomPoint.y - listViewPort.getExtentSize().height)));
			
			Point listTopPoint = this.list.indexToLocation(listTop);
			int topVisible = this.list.locationToIndex(listViewPort.getViewPosition());
			if (topVisible > listTop)
				listViewPort.setViewPosition(new Point(listViewPort.getViewPosition().x, (listTopPoint == null) ? 0 : listTopPoint.y));
			
			this.list.revalidate();
			this.list.repaint();
		}
		
		class AnnotationListModel implements ListModel {
			private int listMaxTokens = 5;
			AnnotationListModel() {}
			public Object getElementAt(int index) {
				String text = TokenSequenceUtils.concatTokens(annotations[index], 0, Math.min(annotations[index].size(), listMaxTokens));
				if (this.listMaxTokens < annotations[index].size()) text += " ...";
				text =  (type + " \"" + text + "\"");
				return ("<HTML>" + (((index < highlighted.length) && highlighted[index]) ? ("<B>" + text + "</B>") : text) + "</HTML>");
			}
			public int getSize() {
				return annotations.length;
			}
			public void addListDataListener(ListDataListener l) {}
			public void removeListDataListener(ListDataListener l) {}
		}
	}
	
	/* 
	 * @see de.uka.ipd.idaho.goldenGate.plugins.GoldenGatePlugin#getPluginName()
	 */
	public String getPluginName() {
		return "Document Navigator";
	}
}
