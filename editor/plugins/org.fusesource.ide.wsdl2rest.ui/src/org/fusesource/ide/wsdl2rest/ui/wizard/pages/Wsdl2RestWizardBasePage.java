/*******************************************************************************
 * Copyright (c) 2018 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/ 
package org.fusesource.ide.wsdl2rest.ui.wizard.pages;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.ui.IJavaElementSearchConstants;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.databinding.swt.DisplayRealm;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.dialogs.FilteredResourcesSelectionDialog;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.eclipse.ui.model.BaseWorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.fusesource.ide.foundation.core.util.Strings;
import org.fusesource.ide.wsdl2rest.ui.internal.UIMessages;
import org.fusesource.ide.wsdl2rest.ui.internal.Wsdl2RestUIActivator;
import org.fusesource.ide.wsdl2rest.ui.wizard.Wsdl2RestOptions;
import org.fusesource.ide.wsdl2rest.ui.wizard.Wsdl2RestWizard;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Base page for the wsdl2rest wizard.
 * @author brianf
 *
 */
public abstract class Wsdl2RestWizardBasePage extends WizardPage {

	/**
	 * Shared Databinding Context for the page.
	 */
	protected final DataBindingContext dbc = new DataBindingContext(DisplayRealm.getRealm(Display.getCurrent()));

	/**
	 * Simple Constructor
	 * @param pageName
	 */
	protected Wsdl2RestWizardBasePage(String pageName) {
		this(pageName, null, null);
	}

	/**
	 * Constructor
	 * @param pageName
	 * @param title
	 * @param titleImage
	 */
	protected Wsdl2RestWizardBasePage(String pageName, String title, ImageDescriptor titleImage) {
		super(pageName, title, titleImage);
	}

	/**
	 * Creates a label control beside a Text control with a default column span.
	 * @param composite
	 * @param labelText
	 * @return
	 */
	protected Text createLabelAndText(Composite composite, String labelText) {
		return createLabelAndText(composite, labelText, 3);
	}

	/**
	 * Creates a label control beside a Text control with a specific column span.
	 * @param composite
	 * @param labelText
	 * @param span
	 * @return
	 */
	protected Text createLabelAndText(Composite composite, String labelText, int span) {
		Label label = new Label(composite, SWT.NONE);
		label.setText(labelText);
		Text textControl = new Text(composite, SWT.BORDER);
		textControl.setLayoutData(GridDataFactory.fillDefaults().indent(10, 0).grab(true, false).span(span, 1).create());
		return textControl;
	}

	/**
	 * Creates a button control.
	 * @param composite
	 * @param labelText
	 * @return
	 */
	protected Button createButton(Composite composite, String labelText) {
		Button buttonControl = new Button(composite, SWT.PUSH);
		buttonControl.setText(labelText);
		return buttonControl;
	}

	/**
	 * Utility method to retrieve the shared options object.
	 * @return
	 */
	protected Wsdl2RestOptions getOptionsFromWizard() {
		return ((Wsdl2RestWizard)getWizard()).getOptions();
	}

	/**
	 * Creates a databinding Binding between a Text control, a model ID, and validator.
	 * @param control
	 * @param modelID
	 * @param validator
	 * @return
	 */
	protected Binding createBinding(Text control, String modelID, IValidator validator) {
		IObservableValue<?> wsdlTarget = WidgetProperties.text(SWT.Modify).observe(control);
		@SuppressWarnings("unchecked")
		IObservableValue<?> wsdlModel = BeanProperties.value(Wsdl2RestOptions.class, modelID).observe(getOptionsFromWizard());		
		return dbc.bindValue(
				wsdlTarget,
				wsdlModel,
				new UpdateValueStrategy().setBeforeSetValidator(validator),
				null);
	}

	/**
	 * Opens a simple dialog to allow selection of a project.
	 * @return
	 */
	protected IProject selectProject() {
		ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(
				getShell(),
				new WorkbenchLabelProvider(),
				new BaseWorkbenchContentProvider());

		dialog.setInput(ResourcesPlugin.getWorkspace().getRoot());
		dialog.setAllowMultiple(false);

		if (dialog.open() == Window.OK) {
			IResource resource = (IResource) dialog.getFirstResult();
			if (resource instanceof IProject) {
				return (IProject) resource;
			}
		}
		return null;
	}

	/**
	 * Opens a simple dialog to allow selection of a WSDL file.
	 */
	protected void selectWSDL() {
		FilteredResourcesSelectionDialog dialog = new FilteredResourcesSelectionDialog(getShell(), false,
				ResourcesPlugin.getWorkspace().getRoot(), IResource.FILE) {
			@Override
			protected ItemsFilter createFilter() {
				return new ResourceFilter() {
					@Override
					public boolean matchItem(Object item) {
						IResource resource = (IResource) item;
						return super.matchItem(item) && "wsdl".equals(resource.getFileExtension()); //$NON-NLS-1$
					}
				};
			}
		};
		dialog.setInitialPattern("*"); //$NON-NLS-1$
		if (dialog.open() == FilteredResourcesSelectionDialog.OK) {
			Object[] result = dialog.getResult();
			if (result == null || result.length != 1 || !(result[0] instanceof IResource)) {
				return;
			}
			try {
				IResource resultFile = (IResource) result[0];
				File actualFile = resultFile.getLocation().toFile();
				getOptionsFromWizard().setWsdlURL(actualFile.toURI().toURL().toExternalForm());
			} catch (MalformedURLException e) {
				Wsdl2RestUIActivator.pluginLog().logError(e);
			}
		}
	}

	/**
	 * Opens a simple dialog to select a folder.
	 * @param project
	 * @return
	 */
	protected String selectFolder(IProject project) {
		ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(
				Display.getDefault().getActiveShell(), 
				new WorkbenchLabelProvider(), 
				new BaseWorkbenchContentProvider());
		dialog.addFilter(new ViewerFilter() {
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				if (element instanceof IProject) {
					IProject project= (IProject) element;
					return project.isAccessible();
				} else if (element instanceof IFolder) {
					return true;
				}
				return false;
			}
		});
		dialog.setTitle(UIMessages.wsdl2RestWizardSecondPageContainerSelectionDialogTitle);
		dialog.setMessage(UIMessages.wsdl2RestWizardSecondPageContainerSelectionDialogMessage);
		dialog.setInput(project);
		if (dialog.open() == ElementTreeSelectionDialog.OK) {
			Object[] result = dialog.getResult();
			if (result.length == 1) {
				return (((IFolder) result[0]).getFullPath().toPortableString());
			}
		}
		return null;
	}

	/**
	 * Opens a dialog to allow selection of a Java class in the workbench.
	 * @param project
	 * @param shell
	 * @return
	 */
	protected String handleClassBrowse(IProject project, Shell shell) {
		IJavaSearchScope scope = null;
		if (project != null) {
			IJavaProject jproject = JavaCore.create(project);
			if (jproject == null) {
				scope = SearchEngine.createWorkspaceScope();
			} else {
				scope = SearchEngine.createJavaSearchScope(new IJavaElement[] { jproject });
			}
		}

		try {
			SelectionDialog dialog = JavaUI.createTypeDialog(shell, null, scope,
					IJavaElementSearchConstants.CONSIDER_CLASSES_AND_INTERFACES, false, ""); //$NON-NLS-1$
			if (dialog.open() == SelectionDialog.OK) {
				Object[] result = dialog.getResult();
				if (result.length > 0 && result[0] instanceof IType) {
					return ((IType) result[0]).getFullyQualifiedName();
				}
			}
		} catch (JavaModelException e) {
			Wsdl2RestUIActivator.pluginLog().logError(e);
		}
		return null;
	}

	/**
	 * Initialize text control if incoming String is not null or empty.
	 * @param textControl
	 * @param value
	 */
	protected void initIfNotEmpty (Text textControl, String value) {
		if (!Strings.isEmpty(value)) {
			textControl.setText(value);
		}
	}

	/**
	 * Utility method to find the target service URL for the WSDL if it is available. 
	 * @return String
	 */
	protected String getLocationFromWSDL() {
		String locationURL = null;
		if (!Strings.isEmpty(getOptionsFromWizard().getWsdlURL())) {
			URL url;
			InputStream testStream = null;
			try {
				url = new URL(getOptionsFromWizard().getWsdlURL());
				testStream = url.openStream();

				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				DocumentBuilder builder = factory.newDocumentBuilder();

				// Load the input XML document, parse it and return an instance of the
				// Document class.
				Document document = builder.parse(testStream);

				// now use XPath to find the particular element we need
				XPathFactory xpf = XPathFactory.newInstance();
				XPath xpath = xpf.newXPath();
				Object rawLocation = 
						xpath.evaluate("/definitions/service/port/address/@location", document, XPathConstants.STRING);

				// if we found it, stash it!
				if (rawLocation != null) {
					locationURL = (String) rawLocation;
				}
			} catch (IOException | ParserConfigurationException | SAXException | XPathExpressionException e) {
				// should be valid at this point
				Wsdl2RestUIActivator.pluginLog().logError(e);
			} finally {
				// make sure we close the stream
				if (testStream != null) {
					try {
						testStream.close();
					} catch (IOException e) {
						Wsdl2RestUIActivator.pluginLog().logError(e);
					}
				}
			}
		}
		return locationURL;
	}
	
	protected Wsdl2RestWizard getWsdl2RestWizard() {
		return (Wsdl2RestWizard) this.getWizard();
	}
}
