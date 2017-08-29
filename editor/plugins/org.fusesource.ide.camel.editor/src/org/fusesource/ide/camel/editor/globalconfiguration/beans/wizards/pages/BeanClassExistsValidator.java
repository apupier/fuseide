/*******************************************************************************
 * Copyright (c) 2017 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.fusesource.ide.camel.editor.globalconfiguration.beans.wizards.pages;

import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.swt.widgets.Combo;
import org.fusesource.ide.camel.editor.globalconfiguration.beans.BeanConfigUtil;
import org.fusesource.ide.camel.editor.internal.UIMessages;
import org.fusesource.ide.camel.model.service.core.model.AbstractCamelModelElement;
import org.fusesource.ide.foundation.core.util.Strings;

/**
 * @author brianf
 *
 */
public class BeanClassExistsValidator implements IValidator {
	
	private IProject project;
	private AbstractCamelModelElement parent;
	private Combo beanRefIdCombo;
	protected BeanConfigUtil beanConfigUtil = new BeanConfigUtil();

	public BeanClassExistsValidator(IProject project) {
		this(project, null, null);
	}

	public BeanClassExistsValidator(IProject project, AbstractCamelModelElement element) {
		this.project = project;
		this.parent = element;
	}
	
	public BeanClassExistsValidator(IProject project, AbstractCamelModelElement element, Combo refCombo) {
		this.project = project;
		this.parent = element;
		this.beanRefIdCombo = refCombo;
	}

	public void setControl(Combo control) {
		this.beanRefIdCombo = control;
	}
	
	private IStatus classExistsInProject(String className) {
		if (className == null || className.isEmpty()) {
			return ValidationStatus.error(UIMessages.beanClassExistsValidatorErrorBeanClassMandatory);
		}
		IJavaProject javaProject = JavaCore.create(this.project);
        IType javaClass;
		try {
			javaClass = javaProject == null ? null : javaProject.findType(className);
			if (javaClass == null) {
				return ValidationStatus.error(UIMessages.beanClassExistsValidatorErrorBeanClassMustExist);
			}
		} catch (JavaModelException e) {
			return ValidationStatus.error(UIMessages.beanClassExistsValidatorErrorBeanClassMustExist, e);
		}
		return ValidationStatus.ok();
	}
	
	@Override
	public IStatus validate(Object value) {
		String className = (String) value;
		String beanRefId = null;
		if (beanRefIdCombo != null && !beanRefIdCombo.isDisposed()) {
			beanRefId = beanRefIdCombo.getText();
		}
		if (Strings.isEmpty(className) && Strings.isEmpty(beanRefId)) {
			return ValidationStatus.error("Must specify either an explicit class name in the project or a reference to a global bean that exposes one.");
		}
		if (!Strings.isEmpty(className) && !Strings.isEmpty(beanRefId)) {
			return ValidationStatus.error("Must specify either an explicit class name in the project or a reference to a global bean that exposes one, not both.");
		}
		IStatus firstStatus = classExistsInProject(className);
		if (firstStatus != ValidationStatus.ok() && !Strings.isEmpty(beanRefId)) {
			String referencedClassName = beanConfigUtil.getClassNameFromReferencedCamelBean(parent, beanRefId);
			return classExistsInProject(referencedClassName);
		}
		return ValidationStatus.ok();
	}
}