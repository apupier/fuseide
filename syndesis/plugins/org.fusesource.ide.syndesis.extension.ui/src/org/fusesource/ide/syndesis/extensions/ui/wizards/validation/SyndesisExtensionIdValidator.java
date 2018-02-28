/*******************************************************************************
 * Copyright (c) 2018 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.fusesource.ide.syndesis.extensions.ui.wizards.validation;

import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.fusesource.ide.foundation.core.util.Strings;
import org.fusesource.ide.syndesis.extensions.ui.internal.Messages;

/**
 * @author lheinema
 *
 */
public class SyndesisExtensionIdValidator implements IValidator {

	/* (non-Javadoc)
	 * @see org.eclipse.core.databinding.validation.IValidator#validate(java.lang.Object)
	 */
	@Override
	public IStatus validate(Object value) {
		String id = (String) value;
		// validate the extension id
		if (Strings.isBlank(id)) {
			return ValidationStatus.error(Messages.newProjectWizardExtensionDetailsPageErrorMissingExtensionId);
		} else if (id.indexOf(' ') != -1) {
			return ValidationStatus.error(Messages.newProjectWizardExtensionDetailsPageErrorInvalidExtensionId);
		} 
		return ValidationStatus.ok();
	}
}
