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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import de.uka.ipd.idaho.easyIO.settings.Settings;
import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.TokenSequence;
import de.uka.ipd.idaho.gamta.util.GenericMutableAnnotationWrapper;
import de.uka.ipd.idaho.goldenGate.DocumentEditor;
import de.uka.ipd.idaho.goldenGate.DocumentEditorDialog;
import de.uka.ipd.idaho.goldenGate.GoldenGATE;
import de.uka.ipd.idaho.goldenGate.plugins.AbstractDocumentViewer;
import de.uka.ipd.idaho.goldenGate.plugins.DocumentViewParameterPanel;
import de.uka.ipd.idaho.goldenGate.util.DialogPanel;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * This viewer goes through a document like a slide show. It shows a small set
 * of annotations at the same time, moving up and down the document.<br>
 * Invoking the SlideAnnotationsViewer with one of the showDocument() methods
 * that takes a Properties object as an argument allows for programmatically
 * specifying the AnnotationFilter used for determining which Annotations to
 * slide over. In particular, there are three parameters:
 * <ul>
 * <li><b>annotationType</b>: the type of the annotations to slide</li>
 * <li><b>windowSize</b>: the number of annotations of the selected type to
 * show at the same time in the sliding window (this value has to be between 1
 * and 5, inclusive)</li>
 * <li><b>environmentSize</b>: the number of tokens to show before the first
 * and after the last annotation in the sliding window (has to be between 0 and
 * 40, inclusive; has an effect only for windowSize=1)</li>
 * </ul>
 * If no annotation type is specified, the user will be prompted to select a
 * type manually, e.g. if the SlideAnnotationsViewer is invoked through
 * GoldenGATE's View menu.
 * 
 * @author sautter
 */
public class SlideAnnotationsViewer extends AbstractDocumentViewer {
	
	/** the parameter to use for specifyingthe annotation type to slide */
	public static final String ANNOTATION_TYPE_PARAMETER = "annotationType";
	
	/** the number of annotations of the selected type to show at the same time in the sliding window (this value has to be between 1 and 5, inclusive) */
	public static final String SLIDING_WINDOW_SIZE_PARAMETER = "windowSize";
	
	/** the number of tokens to show before the first and after the last annotation in the sliding window (has to be between 0 and 25, inclusive; has an effect only for windowSize=1) */
	public static final String ENVIRONMENT_SIZE_PARAMETER = "environmentSize";
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentViewer#getViewMenuName()
	 */
	public String getViewMenuName() {
		return "Slide Annotations";
	}
	
	private class SlideShowData {
		Dimension dialogSize = new Dimension(800, 600);
		Point dialogLocation = null;
		
		String[] taggedTypes = {};
		String[] highlightTypes = {};
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractDocumentViewer#showDocument(de.uka.ipd.idaho.gamta.MutableAnnotation, de.uka.ipd.idaho.goldenGate.DocumentEditor, java.util.Properties)
	 */
	protected void showDocument(MutableAnnotation doc, DocumentEditor editor, Properties parameters) {
		
		//	read parameters
		String type = parameters.getProperty(ANNOTATION_TYPE_PARAMETER);
		int windowSize = -1;
		try {
			windowSize = Integer.parseInt(parameters.getProperty(SLIDING_WINDOW_SIZE_PARAMETER, "-1"));
		} catch (NumberFormatException nfe) {}
		int environmentSize = -1;
		try {
			environmentSize = Integer.parseInt(parameters.getProperty(ENVIRONMENT_SIZE_PARAMETER, "0"));
		} catch (NumberFormatException nfe) {}
		
		//	check if all parameters given
		boolean showDocument = ((type != null) && (windowSize > 0) && (environmentSize >= 0));
		
		//	prompt for missing parameters
		if (!showDocument) {
//			SlidingWindowParameterDialog sad = new SlidingWindowParameterDialog(doc, type, windowSize, environmentSize);
			SlidingWindowParameterDialog sad = new SlidingWindowParameterDialog(doc, parameters);
			sad.setLocationRelativeTo(DialogPanel.getTopWindow());
			sad.setVisible(true);
			if (sad.isCommitted()) {
				type = sad.getType();
				windowSize = sad.getWindowSize();
				environmentSize = sad.getEnvironmentSize();
				showDocument = true;
			}
		}
		
		//	show document if possible
		if (showDocument) {
			
			Annotation[] annotations = doc.getAnnotations(type);
			if (annotations.length < windowSize) {
				JOptionPane.showMessageDialog(DialogPanel.getTopWindow(), ("Cannot slide " + annotations.length + " Annotations with window size " + windowSize), "Cannot Slide", JOptionPane.INFORMATION_MESSAGE);
				return;
			}
			
			try {
				
				//	initialize layout settings
				SlideShowData ssd = new SlideShowData();
				ssd.taggedTypes = ((editor == null) ? new String[0] : editor.getTaggedAnnotationTypes());
				ssd.highlightTypes = ((editor == null) ? new String[0] : editor.getHighlightAnnotationTypes());
				
				//	slide annotations
				int index = 0;
				while ((index + windowSize - 1) < annotations.length) {
					System.gc();
					
					//	set up sliding window dialog editor
					int startIndex = annotations[index].getStartIndex() - environmentSize;
					if (startIndex < 0) startIndex = 0;
					int endIndex = annotations[index + windowSize - 1].getEndIndex() + environmentSize;
					if (endIndex > doc.size()) endIndex = doc.size();
					MutableAnnotation slidingWindowDocPart = doc.addAnnotation(SlidingWindowMask.SLIDING_WINDOW_DOCPART_TYPE, startIndex, (endIndex - startIndex));
					SlidingWindowMask slidingWindow = new SlidingWindowMask(slidingWindowDocPart, type);
					
					//	display sliding window for editing
					SlidingWindowDialog swd = new SlidingWindowDialog(this.parent, editor, ("Slide Edit '" + type + "' Annotations, " + (index + 1) + ((windowSize == 1) ? "" : (" to " + (index + windowSize))) + " of " + annotations.length), slidingWindow, index, (annotations.length - (index + windowSize)), ssd);
					
					//	pop up dialog
					swd.setVisible(true);
					
					//	slide editing cancelled
					int step = swd.getStep();
					if (step == 0) {
						index = annotations.length;
					}
					else {
						Annotation[] windowAnnotations = slidingWindow.getAnnotations(type);
						if (slidingWindow.contentTypeAnnotationsModified || (windowAnnotations.length != windowSize))
							annotations = doc.getAnnotations(type);
						if (windowAnnotations.length < windowSize) {
							if (step < 0)
								index--;
						}
						else if (windowAnnotations.length == windowSize)
							index += step;
						else if (step > 0) index++;
					}
					
					//	remove slide mask document part
					doc.removeAnnotation(slidingWindowDocPart);
				} 
			}
			catch (Exception e) {
				System.out.println(e.getClass().getName() + " (" + e.getMessage() + ") while sliding annotations.");
				e.printStackTrace();
			}
			
			//	remove any remaining sliding window annotations (if any)
			MutableAnnotation[] windows = doc.getMutableAnnotations(SlidingWindowMask.SLIDING_WINDOW_DOCPART_TYPE);
			for (int w = 0; w < windows.length; w++)
				doc.removeAnnotation(windows[w]);
		}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractDocumentViewer#getDocumentViewParameterPanel(de.uka.ipd.idaho.easyIO.settings.Settings)
	 */
	public DocumentViewParameterPanel getDocumentViewParameterPanel(Settings settings) {
		return new SlidingWindowParameterPanel(settings, DocumentEditor.getAnnotationTypeSuggestions(), true);
	}
	
	private static final Integer[] WINDOW_SIZES = {new Integer(1), new Integer(2), new Integer(3), new Integer(4), new Integer(5)};
	private static final Integer DEFAULT_WINDOW_SIZE = new Integer(1);
	
	private static final Integer[] ENVIRONMENT_SIZES = {new Integer(0), new Integer(5), new Integer(10), new Integer(20), new Integer(40)};
	private static final Integer ZERO_ENVIRONMENT_SIZE = new Integer(0);
	private static final Integer DEFAULT_ENVIRONMENT_SIZE = new Integer(10);
	
	private class SlidingWindowParameterPanel extends DocumentViewParameterPanel {
		
		private JComboBox typeSelector;
		private JComboBox windowSizeSelector;
		private JComboBox environmentSizeSelector;
		
		private boolean dirty = false;
		
		SlidingWindowParameterPanel(Settings settings, String[] types, boolean typeEditable) {
			this(((settings == null) ? new Properties() : settings.toProperties()), types, typeEditable);
		}
		
		SlidingWindowParameterPanel(Properties parameters, String[] types, boolean typeEditable) {
			super(new GridBagLayout());
			
			//	initialize window size selector
			Integer environmentSize = ZERO_ENVIRONMENT_SIZE;
			try {
				environmentSize = new Integer(parameters.getProperty(ENVIRONMENT_SIZE_PARAMETER, "0"));
			} catch (NumberFormatException nfe) {}
			this.environmentSizeSelector = new JComboBox(ENVIRONMENT_SIZES);
			this.environmentSizeSelector.setBorder(BorderFactory.createLoweredBevelBorder());
			this.environmentSizeSelector.setEditable(false);
			this.environmentSizeSelector.setSelectedItem((environmentSize.intValue() >= 0) ? environmentSize : DEFAULT_ENVIRONMENT_SIZE);
			this.environmentSizeSelector.setEnabled(true);
			this.environmentSizeSelector.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent ie) {
					dirty = true;
				}
			});
			
			//	initialize window size selector
			Integer windowSize = DEFAULT_WINDOW_SIZE;
			try {
				windowSize = new Integer(parameters.getProperty(SLIDING_WINDOW_SIZE_PARAMETER, "-1"));
			} catch (NumberFormatException nfe) {}
			this.windowSizeSelector = new JComboBox(WINDOW_SIZES);
			this.windowSizeSelector.setBorder(BorderFactory.createLoweredBevelBorder());
			this.windowSizeSelector.setEditable(false);
			this.windowSizeSelector.setSelectedItem((windowSize.intValue() > 0) ? windowSize : DEFAULT_WINDOW_SIZE);
			this.windowSizeSelector.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent ie) {
					int windowSize = getWindowSize();
					if (windowSize == 1) {
						environmentSizeSelector.setSelectedItem(DEFAULT_ENVIRONMENT_SIZE);
						environmentSizeSelector.setEnabled(true);
					}
					else {
						environmentSizeSelector.setSelectedItem(ZERO_ENVIRONMENT_SIZE);
						environmentSizeSelector.setEnabled(false);
					}
					dirty = true;
				}
			});
			
			//	initialize type selector
			Arrays.sort(types, ANNOTATION_TYPE_ORDER);
			String type = parameters.getProperty(ANNOTATION_TYPE_PARAMETER);
			type = ((type == null) ? ((types.length == 0) ? "" : types[0]) : type);
			
			this.typeSelector = new JComboBox(types);
			this.typeSelector.setBorder(BorderFactory.createLoweredBevelBorder());
			this.typeSelector.setEditable(typeEditable);
			this.typeSelector.setSelectedItem(type);
			this.typeSelector.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent ie) {
					dirty = true;
				}
			});
			
			//	put the whole stuff together
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.weighty = 0;
			gbc.gridwidth = 1;
			gbc.gridheight = 1;
//			gbc.insets.top = 5;
//			gbc.insets.left = 5;
//			gbc.insets.right = 5;
//			gbc.insets.bottom = 5;
			gbc.insets.top = 0;
			gbc.insets.left = 0;
			gbc.insets.right = 0;
			gbc.insets.bottom = 0;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			
			gbc.gridy = 0;
			gbc.gridx = 0;
			gbc.weightx = 0;
			this.add(new JLabel("Annotation Type"), gbc.clone());
			gbc.gridx = 1;
			gbc.weightx = 1;
			this.add(this.typeSelector, gbc.clone());
			
			gbc.gridy = 1;
			gbc.gridx = 0;
			gbc.weightx = 0;
			this.add(new JLabel("Sliding Window Size"), gbc.clone());
			gbc.gridx = 1;
			gbc.weightx = 1;
			this.add(this.windowSizeSelector, gbc.clone());
			
			gbc.gridy = 2;
			gbc.gridx = 0;
			gbc.weightx = 0;
			this.add(new JLabel("Environment Size"), gbc.clone());
			gbc.gridx = 1;
			gbc.weightx = 1;
			this.add(this.environmentSizeSelector, gbc.clone());
		}
		
		String getType() {
			Object typeObject = this.typeSelector.getSelectedItem();
			return ((typeObject == null) ? null : typeObject.toString());
		}
		
		int getWindowSize() {
			Object windowSizeObject = this.windowSizeSelector.getSelectedItem();
			return ((windowSizeObject == null) ? 1 : ((Integer) windowSizeObject).intValue());
		}
		
		int getEnvironmentSize() {
			Object environmentSizeObject = this.environmentSizeSelector.getSelectedItem();
			return ((environmentSizeObject == null) ? 0 : ((Integer) environmentSizeObject).intValue());
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentViewParameterPanel#isDirty()
		 */
		public boolean isDirty() {
			return this.dirty;
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentViewParameterPanel#getSettings()
		 */
		public Settings getSettings() {
			Settings set = new Settings();
			Object typeObject = this.typeSelector.getSelectedItem();
			if (typeObject != null)
				set.setSetting(ANNOTATION_TYPE_PARAMETER, typeObject.toString());
			Object windowSizeObject = this.windowSizeSelector.getSelectedItem();
			if (windowSizeObject != null)
				set.setSetting(SLIDING_WINDOW_SIZE_PARAMETER, windowSizeObject.toString());
			Object environmentSizeObject = this.environmentSizeSelector.getSelectedItem();
			if (environmentSizeObject != null)
				set.setSetting(ENVIRONMENT_SIZE_PARAMETER, environmentSizeObject.toString());
			return set;
		}
	}
	
	private class SlidingWindowParameterDialog extends DialogPanel {
		
//		private MutableAnnotation data;
//		
//		private JComboBox typeSelector;
//		private JComboBox windowSizeSelector;
//		private JComboBox environmentSizeSelector;
//		
//		private boolean isCommitted = false;
//		
//		private StringVector parquettingTypes = new StringVector();
//		private StringVector nonParquettingTypes = new StringVector();
		
		private SlidingWindowParameterPanel parameterPanel;
		
//		SlidingWindowParameterDialog(MutableAnnotation data, String type, int windowSize, int environmentSize) {
		SlidingWindowParameterDialog(MutableAnnotation data, Properties parameters) {
			super("Slide Annotations", true);
			
//			this.data = data;
//			String[] annotationTypes = this.data.getAnnotationTypes();
//			Arrays.sort(annotationTypes, ANNOTATION_TYPE_ORDER);
//			
//			//	initialize window size selector
//			this.environmentSizeSelector = new JComboBox(ENVIRONMENT_SIZES);
//			this.environmentSizeSelector.setBorder(BorderFactory.createLoweredBevelBorder());
//			this.environmentSizeSelector.setEditable(false);
//			this.environmentSizeSelector.setSelectedItem(((environmentSize >= 0) && (environmentSize <= 25)) ? ("" + environmentSize) : "10");
//			this.environmentSizeSelector.setEnabled(true);
//			
//			//	initialize window size selector
//			this.windowSizeSelector = new JComboBox(WINDOW_SIZES);
//			this.windowSizeSelector.setBorder(BorderFactory.createLoweredBevelBorder());
//			this.windowSizeSelector.setEditable(false);
//			this.windowSizeSelector.setSelectedItem(((windowSize > 0) && (windowSize <= 5)) ? ("" + windowSize) : "1");
//			String initialType = ((type == null) ? ((annotationTypes.length == 0) ? "" : annotationTypes[0]) : type);
//			if (!this.isParquettingType(initialType)) this.windowSizeSelector.setEnabled(false);
//			this.windowSizeSelector.addItemListener(new ItemListener() {
//				public void itemStateChanged(ItemEvent ie) {
//					int windowSize = getWindowSize();
//					if (windowSize == 1) {
//						environmentSizeSelector.setSelectedItem("10");
//						environmentSizeSelector.setEnabled(true);
//					} else {
//						environmentSizeSelector.setSelectedItem("0");
//						environmentSizeSelector.setEnabled(false);
//					}
//				}
//			});
//			
//			//	initialize type selector
//			this.typeSelector = new JComboBox(annotationTypes);
//			this.typeSelector.setBorder(BorderFactory.createLoweredBevelBorder());
//			this.typeSelector.setEditable(false);
//			this.typeSelector.setSelectedItem(initialType);
//			this.typeSelector.addItemListener(new ItemListener() {
//				public void itemStateChanged(ItemEvent ie) {
//					String type = typeSelector.getSelectedItem().toString();
//					if (isParquettingType(type))
//						windowSizeSelector.setEnabled(true);
//					else {
//						windowSizeSelector.setSelectedItem("1");
//						windowSizeSelector.setEnabled(false);
//					}
//				}
//			});
//			
//			JPanel selectorPanel = new JPanel(new GridBagLayout());
//			GridBagConstraints gbc = new GridBagConstraints();
//			gbc.weighty = 0;
//			gbc.gridwidth = 1;
//			gbc.gridheight = 1;
//			gbc.insets.top = 5;
//			gbc.insets.left = 5;
//			gbc.insets.right = 5;
//			gbc.insets.bottom = 5;
//			gbc.fill = GridBagConstraints.HORIZONTAL;
//			
//			gbc.gridy = 0;
//			gbc.gridx = 0;
//			gbc.weightx = 0;
//			selectorPanel.add(new JLabel("Annotation Type"), gbc.clone());
//			gbc.gridx = 1;
//			gbc.weightx = 1;
//			selectorPanel.add(this.typeSelector, gbc.clone());
//			
//			gbc.gridy = 1;
//			gbc.gridx = 0;
//			gbc.weightx = 0;
//			selectorPanel.add(new JLabel("Sliding Window Size"), gbc.clone());
//			gbc.gridx = 1;
//			gbc.weightx = 1;
//			selectorPanel.add(this.windowSizeSelector, gbc.clone());
//			
//			gbc.gridy = 2;
//			gbc.gridx = 0;
//			gbc.weightx = 0;
//			selectorPanel.add(new JLabel("Environment Size"), gbc.clone());
//			gbc.gridx = 1;
//			gbc.weightx = 1;
//			selectorPanel.add(this.environmentSizeSelector, gbc.clone());
//			
			//	initialize parameter panel
			String[] types = data.getAnnotationTypes();
			Arrays.sort(types, ANNOTATION_TYPE_ORDER);
			this.parameterPanel = new SlidingWindowParameterPanel(parameters, types, false);
			
			//	initialize main buttons
			JButton commitButton = new JButton("Slide Annotations");
			commitButton.setBorder(BorderFactory.createRaisedBevelBorder());
			commitButton.setPreferredSize(new Dimension(100, 21));
			commitButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
//					isCommitted = true;
					dispose();
				}
			});
			
			JButton abortButton = new JButton("Cancel");
			abortButton.setBorder(BorderFactory.createRaisedBevelBorder());
			abortButton.setPreferredSize(new Dimension(100, 21));
			abortButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					parameterPanel = null;
					dispose();
				}
			});
			
			JPanel mainButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
			mainButtonPanel.add(commitButton);
			mainButtonPanel.add(abortButton);
			
			//	put the whole stuff together
			this.setLayout(new BorderLayout());
//			this.add(selectorPanel, BorderLayout.CENTER);
			this.add(this.parameterPanel, BorderLayout.CENTER);
			this.add(mainButtonPanel, BorderLayout.SOUTH);
			
			this.setResizable(true);
			this.setSize(new Dimension(400, 145));
		}
		
//		boolean isParquettingType(String annotationType) {
//			if (this.nonParquettingTypes.containsIgnoreCase(annotationType)) return false;
//			if (this.parquettingTypes.containsIgnoreCase(annotationType)) return true;
//			Annotation[] annotations = this.data.getAnnotations(annotationType);
//			int nextStartIndex = ((annotations.length == 0) ? 0 : annotations[0].getEndIndex());
//			for (int a = 1; a < annotations.length; a++) {
//				if (annotations[a].getStartIndex() == nextStartIndex)
//					nextStartIndex = annotations[a].getEndIndex();
//				else {
//					this.nonParquettingTypes.addElementIgnoreDuplicates(annotationType, false);
//					return false;
//				}
//			}
//			this.parquettingTypes.addElementIgnoreDuplicates(annotationType, false);
//			return true;
//		}
		
		boolean isCommitted() {
			return (this.parameterPanel != null);
//			return this.isCommitted;
		}
		
		String getType() {
//			Object item = this.typeSelector.getSelectedItem();
//			return ((item == null) ? null : item.toString());
			return ((this.parameterPanel == null) ? null : this.parameterPanel.getType());
		}
		
		int getWindowSize() {
//			Object item = this.windowSizeSelector.getSelectedItem();
//			if (item == null) return 1;
//			try {
//				return Integer.parseInt(item.toString());
//			} catch (Exception e) {
//				return 1;
//			}
			return ((this.parameterPanel == null) ? 1 : this.parameterPanel.getWindowSize());
		}
		
		int getEnvironmentSize() {
//			Object item = this.environmentSizeSelector.getSelectedItem();
//			if (item == null) return 1;
//			try {
//				return Integer.parseInt(item.toString());
//			} catch (Exception e) {
//				return 0;
//			}
			return ((this.parameterPanel == null) ? 0 : this.parameterPanel.getEnvironmentSize());
		}
	}
	
	private class SlidingWindowDialog extends DocumentEditorDialog {
		private int step = 0;
		private SlideShowData ssd;
		SlidingWindowDialog(GoldenGATE host, DocumentEditor parent, String title, SlidingWindowMask data, int numPrevious, int numNext, SlideShowData ssd) {
			super(host, parent, title, data);
			this.ssd = ssd;
			
			//	make slide edited Annotations visible
			this.documentEditor.setAnnotationTagVisible(data.contentType, true);
			
			//	transfer display settings from previous dialog
			for (int t = 0; t < this.ssd.taggedTypes.length; t++)
				this.documentEditor.setAnnotationTagVisible(this.ssd.taggedTypes[t], true);
			for (int t = 0; t < this.ssd.highlightTypes.length; t++)
				this.documentEditor.setAnnotationValueHighlightVisible(this.ssd.highlightTypes[t], true);
			
			//	we don't need a file menu here
			this.mainMenu.remove(this.fileMenu);
			
			//	remove own item from view menu
			Component[] vme = this.viewMenu.getComponents();
			for (int c = 0; c < vme.length; c++) {
				if (vme[c] instanceof JMenuItem) {
					JMenuItem me = ((JMenuItem) vme[c]);
					if (me.getText().equals(getViewMenuName()))
						this.viewMenu.remove(me);
				}
			}
			
			//	assemble button panel
			if (numPrevious != 0) {
				final JComboBox stepChooser;
				if (numPrevious > 1) {
					stepChooser = new JComboBox();
					int step1 = 1;
					int step2 = 2;
					stepChooser.addItem("1");
					while (step2 < numPrevious) {
						stepChooser.addItem("" + step2);
						int i = step1;
						step1 = step2;
						step2 += i;
					}
					stepChooser.setBorder(BorderFactory.createLoweredBevelBorder());
					stepChooser.setSelectedItem("1");
					stepChooser.setEditable(false);
				}
				else stepChooser = null;
				
				JButton previousButton = new JButton("Previous");
				previousButton.setBorder(BorderFactory.createRaisedBevelBorder());
				previousButton.setPreferredSize(new Dimension(80, 21));
				previousButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						if (stepChooser == null) step = -1;
						else step = -Integer.parseInt(stepChooser.getSelectedItem().toString());
						dispose();
					}
				});
				if (stepChooser != null) this.mainButtonPanel.add(stepChooser);
				this.mainButtonPanel.add(previousButton);
			}
			JButton cancelButton = new JButton("Stop");
			cancelButton.setBorder(BorderFactory.createRaisedBevelBorder());
			cancelButton.setPreferredSize(new Dimension(80, 21));
			cancelButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					step = 0;
					dispose();
				}
			});
			this.mainButtonPanel.add(cancelButton);
			if (numNext != 0) {
				final JComboBox stepChooser;
				if (numNext > 1) {
					stepChooser = new JComboBox();
					int step1 = 1;
					int step2 = 2;
					stepChooser.addItem("1");
					while (step2 < numNext) {
						stepChooser.addItem("" + step2);
						int i = step1;
						step1 = step2;
						step2 += i;
					}
					stepChooser.setBorder(BorderFactory.createLoweredBevelBorder());
					stepChooser.setSelectedItem("1");
					stepChooser.setEditable(false);
				}
				else stepChooser = null;
				
				JButton nextButton = new JButton("Next");
				nextButton.setBorder(BorderFactory.createRaisedBevelBorder());
				nextButton.setPreferredSize(new Dimension(80, 21));
				nextButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						if (stepChooser == null) step = 1;
						else step = Integer.parseInt(stepChooser.getSelectedItem().toString());
						dispose();
					}
				});
				this.mainButtonPanel.add(nextButton);
				if (stepChooser != null) this.mainButtonPanel.add(stepChooser);
			}
			
			//	configure window
			this.setResizable(true);
			this.setSize(this.ssd.dialogSize);
			if (this.ssd.dialogLocation == null) this.setLocationRelativeTo((parent == null) ? ((Component) this.getOwner()) : ((Component) parent));
			else this.setLocation(this.ssd.dialogLocation);
		}
		
		int getStep() {
			return this.step;
		}
		
		public void dispose() {
			this.ssd.dialogSize = this.getSize();
			this.ssd.dialogLocation = this.getLocation(this.ssd.dialogLocation);
			this.ssd.taggedTypes = this.documentEditor.getTaggedAnnotationTypes();
			this.ssd.highlightTypes = this.documentEditor.getHighlightAnnotationTypes();
			this.documentEditor.writeChanges();
			super.dispose();
		}
	}
	
	private class SlidingWindowMask extends GenericMutableAnnotationWrapper {
		
		static final String SLIDING_WINDOW_DOCPART_TYPE = "SlidingWindow";
		
		private String contentType;
		
		boolean contentTypeAnnotationsModified = false;
		
		SlidingWindowMask(MutableAnnotation data, String contentType) {
			super(data);
			this.contentType = contentType;
		}
		
		public MutableAnnotation addAnnotation(Annotation annotation) {
			if (this.contentType.equals(annotation.getType())) this.contentTypeAnnotationsModified = true;
			return this.mutableAnnotationData.addAnnotation(annotation);
		}
		public MutableAnnotation addAnnotation(String type, int startIndex, int size) {
			if (this.contentType.equals(type)) this.contentTypeAnnotationsModified = true;
			return this.mutableAnnotationData.addAnnotation(type, startIndex, size);
		}
		public QueriableAnnotation[] getAnnotations() {
			Annotation[] annotations = this.mutableAnnotationData.getAnnotations();
			ArrayList aList = new ArrayList();
			for (int a = 0; a < annotations.length; a++)
				if (!SLIDING_WINDOW_DOCPART_TYPE.equals(annotations[a].getType()))
					aList.add(annotations[a]);
			return ((QueriableAnnotation[]) aList.toArray(new QueriableAnnotation[aList.size()]));
		}
		public QueriableAnnotation[] getAnnotations(String type) {
			if (SLIDING_WINDOW_DOCPART_TYPE.equals(type)) return new QueriableAnnotation[0];
			return this.mutableAnnotationData.getAnnotations(type);
		}
		public String[] getAnnotationTypes() {
			StringVector types = new StringVector();
			types.addContentIgnoreDuplicates(this.mutableAnnotationData.getAnnotationTypes());
			types.removeAll(SLIDING_WINDOW_DOCPART_TYPE);
			return types.toStringArray();
		}
		public Annotation removeAnnotation(Annotation annotation) {
			if (this.contentType.equals(annotation.getType())) this.contentTypeAnnotationsModified = true;
			return this.mutableAnnotationData.removeAnnotation(annotation);
		}
		public TokenSequence removeTokens(Annotation annotation) {
			if (this.contentType.equals(annotation.getType())) this.contentTypeAnnotationsModified = true;
			return this.mutableAnnotationData.removeTokens(annotation);
		}
		public String toXML() {
			StringVector types = new StringVector();
			types.addContentIgnoreDuplicates(this.getAnnotationTypes());
			types.removeAll(SLIDING_WINDOW_DOCPART_TYPE);
			return AnnotationUtils.toXML(this, types.toSet());
		}
		public String changeTypeTo(String newType) {
			return SLIDING_WINDOW_DOCPART_TYPE;
		}
		public String getType() {
			return SLIDING_WINDOW_DOCPART_TYPE;
		}
		public void clear() {
			this.contentTypeAnnotationsModified = true;
			this.mutableAnnotationData.clear();
		}
		protected MutableAnnotation wrapMutableAnnotation(MutableAnnotation annotation) {
			return new MutableAnnotationWrapper(annotation);
		}
		
		private class MutableAnnotationWrapper extends GenericMutableAnnotationWrapper {
			MutableAnnotationWrapper(MutableAnnotation data) {
				super(data);
			}
			public MutableAnnotation addAnnotation(Annotation annotation) {
				if (contentType.equals(annotation.getType())) contentTypeAnnotationsModified = true;
				return this.mutableAnnotationData.addAnnotation(annotation);
			}
			public MutableAnnotation addAnnotation(String type, int startIndex, int size) {
				if (contentType.equals(type)) contentTypeAnnotationsModified = true;
				return this.mutableAnnotationData.addAnnotation(type, startIndex, size);
			}
			public QueriableAnnotation[] getAnnotations() {
				Annotation[] annotations = this.mutableAnnotationData.getAnnotations();
				ArrayList aList = new ArrayList();
				for (int a = 0; a < annotations.length; a++)
					if (!SLIDING_WINDOW_DOCPART_TYPE.equals(annotations[a].getType()))
						aList.add(annotations[a]);
				return ((QueriableAnnotation[]) aList.toArray(new QueriableAnnotation[aList.size()]));
			}
			public QueriableAnnotation[] getAnnotations(String type) {
				if (SLIDING_WINDOW_DOCPART_TYPE.equals(type)) return new QueriableAnnotation[0];
				return this.mutableAnnotationData.getAnnotations(type);
			}
			public String[] getAnnotationTypes() {
				StringVector types = new StringVector();
				types.addContentIgnoreDuplicates(this.mutableAnnotationData.getAnnotationTypes());
				types.removeAll(SLIDING_WINDOW_DOCPART_TYPE);
				return types.toStringArray();
			}
			public Annotation removeAnnotation(Annotation annotation) {
				if (contentType.equals(annotation.getType())) contentTypeAnnotationsModified = true;
				return this.mutableAnnotationData.removeAnnotation(annotation);
			}
			public TokenSequence removeTokens(Annotation annotation) {
				if (contentType.equals(annotation.getType())) contentTypeAnnotationsModified = true;
				return this.mutableAnnotationData.removeTokens(annotation);
			}
			public String toXML() {
				StringVector types = new StringVector();
				types.addContentIgnoreDuplicates(this.getAnnotationTypes());
				types.removeAll(SLIDING_WINDOW_DOCPART_TYPE);
				return AnnotationUtils.toXML(this, types.toSet());
			}
			public void clear() {
				contentTypeAnnotationsModified = true;
				this.mutableAnnotationData.clear();
			}
			protected MutableAnnotation wrapMutableAnnotation(MutableAnnotation annotation) {
				return new MutableAnnotationWrapper(annotation);
			}
		}
	}
//	private class SlidingWindowMask implements MutableAnnotation {
//		
//		static final String SLIDING_WINDOW_DOCPART_TYPE = "SlidingWindow";
//		
//		private MutableAnnotation data;
//		private String contentType;
//		
//		boolean contentTypeAnnotationsModified = false;
//		
//		SlidingWindowMask(MutableAnnotation data, String contentType) {
//			this.data = data;
//			this.contentType = contentType;
//		}
//		
//		public String getDocumentProperty(String propertyName, String defaultValue) {
//			return this.data.getDocumentProperty(propertyName, defaultValue);
//		}
//		public String getDocumentProperty(String propertyName) {
//			return this.data.getDocumentProperty(propertyName);
//		}
//		public String[] getDocumentPropertyNames() {
//			return this.data.getDocumentPropertyNames();
//		}
//		public MutableAnnotation addAnnotation(Annotation annotation) {
//			if (this.contentType.equals(annotation.getType())) contentTypeAnnotationsModified = true;
//			return this.data.addAnnotation(annotation);
//		}
//		public MutableAnnotation addAnnotation(String type, int startIndex, int size) {
//			if (this.contentType.equals(type)) contentTypeAnnotationsModified = true;
//			return this.data.addAnnotation(type, startIndex, size);
//		}
//		public int getAbsoluteStartIndex() {
//			return this.data.getAbsoluteStartIndex();
//		}
//		public QueriableAnnotation[] getAnnotations() {
//			Annotation[] annotations = this.data.getAnnotations();
//			ArrayList aList = new ArrayList();
//			for (int a = 0; a < annotations.length; a++)
//				if (!SLIDING_WINDOW_DOCPART_TYPE.equals(annotations[a].getType()))
//					aList.add(annotations[a]);
//			return ((QueriableAnnotation[]) aList.toArray(new QueriableAnnotation[aList.size()]));
//		}
//		public QueriableAnnotation[] getAnnotations(String type) {
//			if (SLIDING_WINDOW_DOCPART_TYPE.equals(type)) return new QueriableAnnotation[0];
//			return this.data.getAnnotations(type);
//		}
//		public MutableAnnotation[] getMutableAnnotations() {
//			return this.data.getMutableAnnotations();
//		}
//		public MutableAnnotation[] getMutableAnnotations(String type) {
//			return this.data.getMutableAnnotations(type);
//		}
//		public void copyAttributes(Attributed source) {
//			this.data.copyAttributes(source);
//		}
//		public String[] getAnnotationTypes() {
//			StringVector types = new StringVector();
//			types.addContentIgnoreDuplicates(this.data.getAnnotationTypes()); //TODO: Use AnnotationFilters
//			types.removeAll(SLIDING_WINDOW_DOCPART_TYPE);
//			return types.toStringArray();
//		}
//		public String getAnnotationNestingOrder() {
//			return this.data.getAnnotationNestingOrder();
//		}
//		public Annotation removeAnnotation(Annotation annotation) {
//			if (this.contentType.equals(annotation.getType())) contentTypeAnnotationsModified = true;
//			return this.data.removeAnnotation(annotation);
//		}
//		public TokenSequence removeTokens(Annotation annotation) {
//			if (this.contentType.equals(annotation.getType())) contentTypeAnnotationsModified = true;
//			return this.data.removeTokens(annotation);
//		}
//		public String toXML() {
//			StringVector types = new StringVector();
//			types.addContentIgnoreDuplicates(this.getAnnotationTypes());
//			types.removeAll(SLIDING_WINDOW_DOCPART_TYPE);
//			return AnnotationUtils.toXML(this, types.toSet());
//		}
//		public String changeTypeTo(String newType) {
//			return SLIDING_WINDOW_DOCPART_TYPE;
//		}
//		public void clearAttributes() {
//			this.data.clearAttributes();
//		}
//		public Object getAttribute(String name, Object def) {
//			return this.data.getAttribute(name, def);
//		}
//		public Object getAttribute(String name) {
//			return this.data.getAttribute(name);
//		}
//		public String[] getAttributeNames() {
//			return this.data.getAttributeNames();
//		}
//		public int getEndIndex() {
//			return this.data.getEndIndex();
//		}
//		public int getStartIndex() {
//			return this.data.getStartIndex();
//		}
//		public String getType() {
//			return SLIDING_WINDOW_DOCPART_TYPE;
//		}
//		public String getAnnotationID() {
//			return this.data.getAnnotationID();
//		}
//		public String getValue() {
//			return this.data.getValue();
//		}
//		public boolean hasAttribute(String name) {
//			return this.data.hasAttribute(name);
//		}
//		public Object removeAttribute(String name) {
//			return this.data.removeAttribute(name);
//		}
//		public Object setAttribute(String name, Object value) {
//			return this.data.setAttribute(name, value);
//		}
//		public int compareTo(Object o) {
//			return this.data.compareTo(o);
//		}
//		public CharSequence addTokens(CharSequence tokens) {
//			return this.data.addTokens(tokens);
//		}
//		public void addTokenSequenceListener(TokenSequenceListener tsl) {
//			this.data.addTokenSequenceListener(tsl);
//		}
//		public void clear() {
//			this.contentTypeAnnotationsModified = true;
//			this.data.clear();
//		}
//		public CharSequence insertTokensAt(CharSequence tokens, int index) {
//			return this.data.insertTokensAt(tokens, index);
//		}
//		public TokenSequence removeTokensAt(int index, int size) {
//			return this.data.removeTokensAt(index, size);
//		}
//		public void removeTokenSequenceListener(TokenSequenceListener tsl) {
//			this.data.removeTokenSequenceListener(tsl);
//		}
//		public CharSequence setLeadingWhitespace(CharSequence whitespace) {
//			return this.data.setLeadingWhitespace(whitespace);
//		}
//		public CharSequence setValueAt(String value, int index) {
//			return this.data.setValueAt(value, index);
//		}
//		public Token firstToken() {
//			return this.data.firstToken();
//		}
//		public String firstValue() {
//			return this.data.firstValue();
//		}
//		public String getLeadingWhitespace() {
//			return this.data.getLeadingWhitespace();
//		}
//		public TokenSequence getSubsequence(int start, int size) {
//			return this.data.getSubsequence(start, size);
//		}
//		public Tokenizer getTokenizer() {
//			return this.data.getTokenizer();
//		}
//		public Token lastToken() {
//			return this.data.lastToken();
//		}
//		public String lastValue() {
//			return this.data.lastValue();
//		}
//		public int getStartOffset() {
//			return this.data.getStartOffset();
//		}
//		public int getAbsoluteStartOffset() {
//			return this.data.getAbsoluteStartOffset();
//		}
//		public int getEndOffset() {
//			return this.data.getEndOffset();
//		}
//		public int length() {
//			return this.data.length();
//		}
//		public int size() {
//			return this.data.size();
//		}
//		public Token tokenAt(int index) {
//			return this.data.tokenAt(index);
//		}
//		public String valueAt(int index) {
//			return this.data.valueAt(index);
//		}
//		public MutableTokenSequence getMutableSubsequence(int start, int size) {
//			return this.data.getMutableSubsequence(start, size);
//		}
//		public CharSequence setValueAt(CharSequence value, int index) throws IllegalArgumentException {
//			return this.data.setValueAt(value, index);
//		}
//		public CharSequence setWhitespaceAfter(CharSequence whitespace, int index) throws IllegalArgumentException {
//			return this.data.setWhitespaceAfter(whitespace, index);
//		}
//		public String getWhitespaceAfter(int index) {
//			return this.data.getWhitespaceAfter(index);
//		}
//		public void addChar(char ch) {
//			this.data.addChar(ch);
//		}
//		public void addChars(CharSequence chars) {
//			this.data.addChars(chars);
//		}
//		public void addCharSequenceListener(CharSequenceListener csl) {
//			this.data.addCharSequenceListener(csl);
//		}
//		public void insertChar(char ch, int offset) {
//			this.data.insertChar(ch, offset);
//		}
//		public void insertChars(CharSequence chars, int offset) {
//			this.data.insertChars(chars, offset);
//		}
//		public MutableCharSequence mutableSubSequence(int start, int end) {
//			return this.data.getMutableSubsequence(start, end);
//		}
//		public char removeChar(int offset) {
//			return this.data.removeChar(offset);
//		}
//		public CharSequence removeChars(int offset, int length) {
//			return this.data.removeChars(offset, length);
//		}
//		public void removeCharSequenceListener(CharSequenceListener csl) {
//			this.data.removeCharSequenceListener(csl);
//		}
//		public char setChar(char ch, int offset) {
//			return this.data.setChar(ch, offset);
//		}
//		public CharSequence setChars(CharSequence chars, int offset, int length) {
//			return this.data.setChars(chars, offset, length);
//		}
//		public char charAt(int index) {
//			return this.data.charAt(index);
//		}
//		public CharSequence subSequence(int start, int end) {
//			return this.data.subSequence(start, end);
//		}
//		public void addAnnotationListener(AnnotationListener al) {
//			this.data.addAnnotationListener(al);
//		}
//		public void removeAnnotationListener(AnnotationListener al) {
//			this.data.addAnnotationListener(al);
//		}
//	}
}
