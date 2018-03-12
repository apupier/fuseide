/******************************************************************************* 
 * Copyright (c) 2016 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/ 
package org.fusesource.ide.projecttemplates.wizards.pages;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.FilteredTree;
import org.fusesource.ide.foundation.ui.util.Selections;
import org.fusesource.ide.projecttemplates.internal.Messages;
import org.fusesource.ide.projecttemplates.internal.ProjectTemplatesActivator;
import org.fusesource.ide.projecttemplates.wizards.pages.filter.CompatibleCamelVersionFilter;
import org.fusesource.ide.projecttemplates.wizards.pages.filter.ExcludeEmptyCategoriesFilter;
import org.fusesource.ide.projecttemplates.wizards.pages.filter.TemplateNameAndKeywordPatternFilter;
import org.fusesource.ide.projecttemplates.wizards.pages.model.CategoryItem;
import org.fusesource.ide.projecttemplates.wizards.pages.model.TemplateItem;
import org.fusesource.ide.projecttemplates.wizards.pages.model.TemplateModel;
import org.fusesource.ide.projecttemplates.wizards.pages.provider.TemplateContentProvider;
import org.fusesource.ide.projecttemplates.wizards.pages.provider.TemplateLabelProvider;

/**
 * @author lhein
 */
public class FuseIntegrationProjectWizardTemplatePage extends WizardPage {

	private FilteredTree listTemplates;
	private Text templateInfoText;
	private FuseIntegrationProjectWizardRuntimeAndCamelPage runtimeAndCamelVersionPage;
	private CompatibleCamelVersionFilter compatibleCamelVersionFilter;
	private Label filteredTemplatesInformationMessage;
	private Label filteredTemplatesInformationIcon;
	
	public FuseIntegrationProjectWizardTemplatePage(FuseIntegrationProjectWizardRuntimeAndCamelPage runtimeAndCamelVersionPage) {
		super(Messages.newProjectWizardTemplatePageName);
		this.runtimeAndCamelVersionPage = runtimeAndCamelVersionPage;
		setTitle(Messages.newProjectWizardTemplatePageTitle);
		setDescription(Messages.newProjectWizardTemplatePageDescription);
		setImageDescriptor(ProjectTemplatesActivator.imageDescriptorFromPlugin(ProjectTemplatesActivator.PLUGIN_ID, ProjectTemplatesActivator.IMAGE_CAMEL_PROJECT_ICON));
		setPageComplete(false);
	}
	
	@Override
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		container.setLayout(new GridLayout(3, false));

		Label lblHeadline = new Label(container, SWT.None);
		lblHeadline.setLayoutData(GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).grab(true, false).span(3, 1).create());
		lblHeadline.setText(Messages.newProjectWizardTemplatePageHeadlineLabel);

		Composite grpEmptyVsTemplate = new Composite(container, SWT.None);
		GridLayout gridLayout = new GridLayout(1, false);
		grpEmptyVsTemplate.setLayout(gridLayout);
		grpEmptyVsTemplate.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).span(3, 1).hint(SWT.DEFAULT, 400).create());

		createTemplatesPanel(grpEmptyVsTemplate);

		setControl(container);

		validate();
	}

	protected void createTemplatesPanel(Composite grpEmptyVsTemplate) {
		Composite templates = new Composite(grpEmptyVsTemplate, SWT.None);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1);
		gd.horizontalIndent = 20;
		templates.setLayoutData(gd);
		templates.setLayout(new GridLayout(2, true));

		listTemplates = createFilteredTree(templates);

		templateInfoText = new Text(templates, SWT.MULTI | SWT.READ_ONLY | SWT.WRAP | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		templateInfoText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		createTemplatesBottomInformation(templates);
	}

	protected void createTemplatesBottomInformation(Composite templates) {
		Composite infoComposite = new Composite(templates, SWT.NONE);
		infoComposite.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).span(2, 1).create());
		infoComposite.setLayout(GridLayoutFactory.fillDefaults().numColumns(2).equalWidth(false).create());
		
		filteredTemplatesInformationIcon = new Label(infoComposite, SWT.NONE);
		filteredTemplatesInformationIcon.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_INFO_TSK));
		
		filteredTemplatesInformationMessage = new Label(infoComposite, SWT.NONE);
		filteredTemplatesInformationMessage.setText(Messages.newProjectWizardTemplatePageTemplateFilterMessageInformation);
		filteredTemplatesInformationMessage.setFont(JFaceResources.getFontRegistry().getItalic(JFaceResources.DEFAULT_FONT));
	}

	/**
	 * @param parent
	 * @return
	 */
	private FilteredTree createFilteredTree(Composite parent) {
		final int treeStyle = SWT.SINGLE | SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER;
		listTemplates = new FilteredTree(parent, treeStyle, new TemplateNameAndKeywordPatternFilter(), true);
		listTemplates.getFilterControl().setMessage(Messages.newProjectWizardTemplatePageFilterBoxText);
		listTemplates.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());
		listTemplates.getViewer().setContentProvider(new TemplateContentProvider());
		listTemplates.getViewer().setLabelProvider(new TemplateLabelProvider());
		compatibleCamelVersionFilter = new CompatibleCamelVersionFilter(runtimeAndCamelVersionPage.getSelectedCamelVersion());
		listTemplates.getViewer().setFilters(
				new ExcludeEmptyCategoriesFilter(),
				compatibleCamelVersionFilter);
		listTemplates.getViewer().setInput(getTemplates());
		listTemplates.getViewer().addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				if (!event.getSelection().isEmpty()) {
					Object selObj = Selections.getFirstSelection(event.getSelection());
					if (selObj instanceof TemplateItem) {
						updateTemplateInfo((TemplateItem)selObj);
						validate();
						return;
					}
				} 
				updateTemplateInfo(null);
			}
		});
		return listTemplates;
	}
	
	private void updateTemplateInfo(TemplateItem template) {
		if (template == null) {
			templateInfoText.setText("");
		} else {
			templateInfoText.setText(template.getDescription());
		}
		validate();
	}
	
	/**
	 * /!\ Public for test purpose
	 */
	public TemplateModel getTemplates() {
		return new TemplateModel();
	}
	
	private void validate() {
		if ((listTemplates.getViewer().getSelection().isEmpty() || 
			 Selections.getFirstSelection(listTemplates.getViewer().getSelection()) instanceof CategoryItem)) {
			setPageComplete(false);
		} else {
			setPageComplete(true);
		}
	}
	
	/**
	 * returns the selected template or null if none selected
	 * 
	 * @return
	 */
	public TemplateItem getSelectedTemplate() {
		if (!listTemplates.getViewer().getSelection().isEmpty()) {
			Object o = Selections.getFirstSelection(listTemplates.getViewer().getSelection());
			if (o instanceof TemplateItem) {
				return (TemplateItem)o;
			}
		}
		return null;
	}
	
	/**
	 * /!\ Public for test purpose
	 */
	public FilteredTree getListTemplates() {
		return this.listTemplates;
	}

	public void refresh(String camelVersion) {
		compatibleCamelVersionFilter.setCamelVersion(camelVersion);
		listTemplates.getViewer().refresh();
	}
}
