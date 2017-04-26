/*******************************************************************************
 * Copyright (c) 2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.fusesource.ide.camel.editor.properties;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.observable.Observables;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.swt.ISWTObservableValue;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.internal.forms.widgets.FormsResources;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;
import org.fusesource.ide.camel.editor.globalconfiguration.beans.BeanConfigUtil;
import org.fusesource.ide.camel.editor.globalconfiguration.beans.wizards.pages.BeanClassExistsValidator;
import org.fusesource.ide.camel.editor.properties.bean.AttributeTextFieldPropertyUICreator;
import org.fusesource.ide.camel.editor.properties.bean.AttributeTextFieldPropertyUICreatorWithBrowse;
import org.fusesource.ide.camel.editor.properties.bean.NewBeanIdPropertyValidator;
import org.fusesource.ide.camel.editor.properties.bean.PropertyMethodValidator;
import org.fusesource.ide.camel.editor.properties.bean.PropertyRequiredValidator;
import org.fusesource.ide.camel.editor.properties.creators.AbstractTextFieldParameterPropertyUICreator;
import org.fusesource.ide.camel.editor.properties.creators.advanced.UnsupportedParameterPropertyUICreatorForAdvanced;
import org.fusesource.ide.camel.editor.utils.CamelUtils;
import org.fusesource.ide.camel.model.service.core.catalog.Parameter;
import org.fusesource.ide.camel.model.service.core.model.CamelBean;
import org.fusesource.ide.camel.model.service.core.util.CamelComponentUtils;
import org.fusesource.ide.foundation.core.util.Strings;

/**
 * @author bfitzpat
 */
public class AdvancedBeanPropertiesSection extends FusePropertySection {

	private BeanConfigUtil beanConfigUtil = new BeanConfigUtil();

	/**
	 * 
	 * @param folder
	 */
	@Override
	protected void createContentTabs(CTabFolder folder) {
		List<Parameter> props = new ArrayList<>();
		List<String> tabsToCreate = new ArrayList<>();
		tabsToCreate.add(GROUP_COMMON);
		
		// define the properties we're handling here
		Parameter idParam = beanConfigUtil.createParameter(CamelBean.PROP_ID, String.class.getName());
		idParam.setRequired("true");
		props.add(idParam);
		Parameter classParam = beanConfigUtil.createParameter(CamelBean.PROP_CLASS, String.class.getName());
		classParam.setRequired("true");
		props.add(classParam);
		props.add(beanConfigUtil.createParameter(CamelBean.PROP_SCOPE, String.class.getName()));
		props.add(beanConfigUtil.createParameter(CamelBean.PROP_DEPENDS_ON, String.class.getName()));
		props.add(beanConfigUtil.createParameter(CamelBean.PROP_INIT_METHOD, String.class.getName()));
		props.add(beanConfigUtil.createParameter(CamelBean.PROP_DESTROY_METHOD, String.class.getName()));
		
		String factoryAttribute = beanConfigUtil.getFactoryMethodAttribute(selectedEP.getXmlNode());
		props.add(beanConfigUtil.createParameter(factoryAttribute, String.class.getName()));

		props.sort(new ParameterPriorityComparator());

		for (String group : tabsToCreate) {
			CTabItem contentTab = new CTabItem(this.tabFolder, SWT.NONE);
			contentTab.setText(Strings.humanize(group));

			Composite page = this.toolkit.createComposite(folder);
			page.setLayout(new GridLayout(4, false));

			generateTabContents(props, page);

			contentTab.setControl(page);

			this.tabs.add(contentTab);
		}
	}

	private AbstractTextFieldParameterPropertyUICreator createTextFieldWithClassBrowseAndNew(final Parameter p, final Composite page) {
		AbstractTextFieldParameterPropertyUICreator txtFieldCreator = 
				new AttributeTextFieldPropertyUICreatorWithBrowse(dbc, modelMap, eip, selectedEP, p, page, getWidgetFactory());
		txtFieldCreator.setColumnSpan(1);
		txtFieldCreator.create();
		createClassBrowseButton(page, txtFieldCreator.getControl());
		createClassNewButton(page, txtFieldCreator.getControl());
		return txtFieldCreator;
	}
	
	private AbstractTextFieldParameterPropertyUICreator createTextFieldWithMethodBrowse(final Parameter p, final Composite page) {
		AbstractTextFieldParameterPropertyUICreator txtFieldCreator = 
				new AttributeTextFieldPropertyUICreatorWithBrowse(dbc, modelMap, eip, selectedEP, p, page, getWidgetFactory());
		txtFieldCreator.setColumnSpan(2);
		txtFieldCreator.create();
		createMethodBrowseButton(page, txtFieldCreator.getControl());
		return txtFieldCreator;
	}

	private AbstractTextFieldParameterPropertyUICreator createTextFieldWithNoArgMethodBrowse(final Parameter p, final Composite page) {
		AbstractTextFieldParameterPropertyUICreator txtFieldCreator = 
				new AttributeTextFieldPropertyUICreatorWithBrowse(dbc, modelMap, eip, selectedEP, p, page, getWidgetFactory());
		txtFieldCreator.setColumnSpan(2);
		txtFieldCreator.create();
		createNoArgMethodBrowseButton(page, txtFieldCreator.getControl());
		return txtFieldCreator;
	}

	private AbstractTextFieldParameterPropertyUICreator createTextField(final Parameter p, final Composite page) {
		AbstractTextFieldParameterPropertyUICreator txtFieldCreator = new AttributeTextFieldPropertyUICreator(dbc, modelMap, eip, selectedEP, p, page,
				getWidgetFactory());
		txtFieldCreator.create();
		return txtFieldCreator;
	}

	/**
	 * 
	 * @param props
	 * @param page
	 * @param ignorePathProperties
	 * @param group
	 */
	protected void generateTabContents(List<Parameter> props, final Composite page) {
		props.sort(new ParameterPriorityComparator());
		for (Parameter p : props) {
			createPropertyLabel(toolkit, page, p);
			createPropertyFieldEditor(page, p);
		}
	}

	private void createPropertyFieldEditor(final Composite page, Parameter p) {
		IValidator validator = null;
		AbstractTextFieldParameterPropertyUICreator txtFieldCreator = null;
		String propName = p.getName();
		
		if (propName.equals(CamelBean.PROP_CLASS)) {
			txtFieldCreator = createTextFieldWithClassBrowseAndNew(p, page);
			validator = new BeanClassExistsValidator();
		} else if (propName.equals(CamelBean.PROP_INIT_METHOD)
				|| propName.equals(CamelBean.PROP_DESTROY_METHOD)) {
			txtFieldCreator = createTextFieldWithNoArgMethodBrowse(p, page);
			validator = new PropertyMethodValidator(modelMap);
		} else if (propName.equals(CamelBean.PROP_FACTORY_METHOD)
				|| propName.equals(CamelBean.PROP_FACTORY_BEAN)) {
			txtFieldCreator = createTextFieldWithMethodBrowse(p, page);
			validator = new PropertyMethodValidator(modelMap);
		} else if (propName.equals(CamelBean.PROP_ID)) {
			txtFieldCreator = createTextField(p, page);
			validator = new NewBeanIdPropertyValidator(p, selectedEP);
		} else if (CamelComponentUtils.isTextProperty(p) || CamelComponentUtils.isCharProperty(p)) {
			txtFieldCreator = createTextField(p, page);
			if (p.getRequired() != null && "true".contentEquals(p.getRequired())) {
				validator = new PropertyRequiredValidator(p);
			}
		} else if (CamelComponentUtils.isUnsupportedProperty(p)) {
			// handle unsupported props
			new UnsupportedParameterPropertyUICreatorForAdvanced(dbc, modelMap, eip, selectedEP, p, page,
					getWidgetFactory()).create();
		}

		if (txtFieldCreator != null) {
			ISWTObservableValue uiObservable = handleObservable(txtFieldCreator, p);
			if (uiObservable != null) {
				bindField(validator, uiObservable, p);
			}
		}
	}	
	
	private ISWTObservableValue handleObservable(AbstractTextFieldParameterPropertyUICreator txtFieldCreator, Parameter p) {
		// initialize the map entry
		modelMap.put(p.getName(), txtFieldCreator.getControl().getText());
		// create observables for the control
		return txtFieldCreator.getUiObservable();
	}
	
	private void bindField(IValidator validator, ISWTObservableValue uiObservable, Parameter p) {
		// create UpdateValueStrategy and assign to the binding
		UpdateValueStrategy strategy = new UpdateValueStrategy();
		strategy.setBeforeSetValidator(validator);

		// create observables for the Map entries
		IObservableValue<Object> modelObservable = Observables.observeMapEntry(modelMap, p.getName());
		// bind the observables
		Binding bindValue = dbc.bindValue(uiObservable, modelObservable, strategy, null);
		ControlDecorationSupport.create(bindValue, SWT.TOP | SWT.LEFT);
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.views.properties.tabbed.AbstractPropertySection#
	 * createControls (org.eclipse.swt.widgets.Composite,
	 * org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage)
	 */
	@Override
	public void createControls(Composite parent, TabbedPropertySheetPage aTabbedPropertySheetPage) {
		this.toolkit = new FormToolkit(parent.getDisplay());
		super.createControls(parent, aTabbedPropertySheetPage);

		// now setup the file binding properties page
		parent.setLayout(new GridLayout());
		parent.setLayoutData(new GridData(GridData.FILL_BOTH));

		form = toolkit.createForm(parent);
		form.setLayoutData(new GridData(GridData.FILL_BOTH));
		form.getBody().setLayout(new GridLayout(1, false));

		Composite sbody = form.getBody();

		tabFolder = new CTabFolder(sbody, SWT.TOP | SWT.FLAT);
		toolkit.adapt(tabFolder, true, true);
		tabFolder.setLayoutData(new GridData(GridData.FILL_BOTH));

		Color selectedColor = toolkit.getColors().getColor(IFormColors.SEPARATOR);
		tabFolder.setSelectionBackground(new Color[] { selectedColor, toolkit.getColors().getBackground() },
				new int[] { 20 }, true);
		tabFolder.setCursor(FormsResources.getHandCursor());
		toolkit.paintBordersFor(tabFolder);

		form.setText("Advanced Properties");
		toolkit.decorateFormHeading(form);

		form.layout();
		tabFolder.setSelection(0);
	}

	private void createClassBrowseButton(Composite composite, Text field) {
		Button browseBeanButton = new Button(composite, SWT.PUSH);
		browseBeanButton.setText("...");
		browseBeanButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				final IProject project = CamelUtils.project();
				String className = beanConfigUtil.handleClassBrowse(project, getDisplay().getActiveShell());
				if (className != null) {
					field.setText(className);
				}
			}

		});
	}

	private void createClassNewButton(Composite composite, Text field) {
		Button newBeanButton = new Button(composite, SWT.PUSH);
		newBeanButton.setText("+");
		newBeanButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				final IProject project = CamelUtils.project();
				String value = beanConfigUtil.handleNewClassWizard(project, getDisplay().getActiveShell());
				if (value != null) {
					field.setText(value);
				}
			}

		});
	}

	private void createNoArgMethodBrowseButton(Composite composite, Text field) {
		Button browseBeanButton = new Button(composite, SWT.PUSH);
		browseBeanButton.setText("...");
		browseBeanButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				Object control = modelMap.get(CamelBean.PROP_CLASS);
				if (control != null) {
					final IProject project = CamelUtils.project();
					String className = (String) control;
					String methodName = beanConfigUtil.handleNoArgMethodBrowse(project, className,
							getDisplay().getActiveShell());
					if (methodName != null) {
						field.setText(methodName);
					}
				}
			}

		});
	}

	private void createMethodBrowseButton(Composite composite, Text field) {
		Button browseBeanButton = new Button(composite, SWT.PUSH);
		browseBeanButton.setText("...");
		browseBeanButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				Object control = modelMap.get(CamelBean.PROP_CLASS);
				if (control != null) {
					final IProject project = CamelUtils.project();
					String className = (String) control;
					String methodName = beanConfigUtil.handleMethodBrowse(project, className,
							getDisplay().getActiveShell());
					if (methodName != null) {
						field.setText(methodName);
					}
				}
			}

		});
	}
}