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
package org.fusesource.ide.camel.validation.model;

import java.util.Arrays;
import java.util.List;

import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.osgi.util.NLS;
import org.fusesource.ide.camel.model.service.core.catalog.Parameter;
import org.fusesource.ide.camel.model.service.core.catalog.components.Component;
import org.fusesource.ide.camel.model.service.core.model.AbstractCamelModelElement;
import org.fusesource.ide.camel.model.service.core.model.CamelRouteContainerElement;
import org.fusesource.ide.camel.model.service.core.util.CamelComponentUtils;
import org.fusesource.ide.camel.model.service.core.util.PropertiesUtils;
import org.fusesource.ide.camel.validation.l10n.Messages;
import org.fusesource.ide.foundation.core.util.Strings;

/**
 * @author Aurelien Pupier
 *
 */
public final class TextParameterValidator implements IValidator {

	private AbstractCamelModelElement camelModelElement;
	private Parameter parameter;
	private Object uriValue;
	private Object refValue;
	private CamelRouteContainerElement routeContainer;

	public TextParameterValidator(AbstractCamelModelElement camelModelElement, Parameter parameter) {
		this.camelModelElement = camelModelElement;
		this.parameter = parameter;
		this.uriValue = camelModelElement.getParameter("uri");
		this.refValue = camelModelElement.getParameter("ref");
		this.routeContainer = camelModelElement.getRouteContainer();
	}

	@Override
	public IStatus validate(Object value) {
		IStatus validationStatus = ValidationStatus.ok();
		
		if (isValidationRequired()) {
			if (isUriParameter()) {
				validationStatus = validateUri();
			} else if (isRefParameter()) {
				validationStatus = validateRef(value);
			} else if (isIdParameter()) {
				validationStatus = validateId(value);
			} else {
				// by default we only check for a value != null and length > 0
				if (value == null || value instanceof String && value.toString().trim().length() < 1) {
					validationStatus =  ValidationStatus.warning("Parameter " + parameter.getName() + " is a mandatory field and cannot be empty.");
				}
			}
		}

		return validationStatus;
	}
	
	private boolean isValidationRequired() {
		return PropertiesUtils.isRequired(parameter) || "id".equalsIgnoreCase(parameter.getName());
	}
	
	private boolean isUriParameter() {
		return "uri".equalsIgnoreCase(parameter.getName());
	}
	
	private boolean isRefParameter() {
		return "ref".equalsIgnoreCase(parameter.getName());
	}
	
	private boolean isIdParameter() {
		return "id".equalsIgnoreCase(parameter.getName());
	}
	
	private boolean isRefAndUriEmpty() {
		return Strings.isBlank((String) uriValue) && Strings.isBlank((String) refValue);
	}
	
	private boolean isRefSet() {
		return Strings.isBlank((String) uriValue) && !Strings.isBlank((String) refValue);
	}
	
	private IStatus validateUri() {
		// one of uri and ref has to be filled
		if (isRefAndUriEmpty()) {
			return ValidationStatus.warning("One of Ref and Uri values have to be filled!");
		}
		
		// if ref then we need to check if uri defined
		if (isRefSet()) {
			// ref found - now check if REF has URI defined
			AbstractCamelModelElement cme = routeContainer.findNode((String) refValue);
			if (cme == null || cme.getParameter("uri") == null || ((String) cme.getParameter("uri")).trim().length() < 1) {
				// no uri defined on ref
				return ValidationStatus.warning("The referenced endpoint has no URI defined or does not exist.");
			}
		}

		// check for broken refs
		if (uriValue != null && ((String) uriValue).startsWith("ref:")) {
			String refId = ((String) uriValue).trim().length() > "ref:".length() ? ((String) uriValue).substring("ref:".length()) : null;
			List<String> refs = Arrays.asList(CamelComponentUtils.getRefs(camelModelElement.getCamelFile()));
			if (refId == null || refId.trim().length() < 1 || !refs.contains(refId)) {
				return ValidationStatus.warning("The entered reference does not exist in your context!");
			}
		}

		// warn user if he set both ref and uri
		if (uriValue != null && ((String) uriValue).trim().length() > 0	&& refValue != null && ((String) refValue).trim().length() > 0) {
			return ValidationStatus.error("Please choose either URI or Ref but do not enter both values.");
		}
		
		return ValidationStatus.ok();
	}
	
	private IStatus validateRef(Object value) {
		if (value instanceof String && ((String)value).trim().length() > 0) {
			String refId = (String) value;
			AbstractCamelModelElement cme = routeContainer.findNode(refId);
			if (cme == null && !camelModelElement.getCamelFile().getGlobalDefinitions().containsKey(refId)) {
				// the ref doesn't exist
				return ValidationStatus.warning("The entered reference does not exist in your context!");
			} else if (cme != null && cme.getParameter("uri") == null || ((String) cme.getParameter("uri")).trim().length() < 1) {
				// but has no URI defined
				return ValidationStatus.error("The referenced endpoint does not define a valid URI!");
			}
		}

		// warn user if he set both ref and uri
		if (uriValue != null && ((String) uriValue).trim().length() > 0 && refValue != null && ((String) refValue).trim().length() > 0) {
			return ValidationStatus.warning("Please choose only ONE of Uri and Ref.");
		}
		
		return ValidationStatus.ok();
	}
	
	private IStatus validateId(Object value) {
		// check if ID is unique
		if (value == null || value instanceof String == false || value.toString().trim().length() < 1) {
			return ValidationStatus.warning("Parameter " + parameter.getName() + " is a mandatory field and cannot be empty.");
		} else if (routeContainer != null && !routeContainer.isIDUnique((String) value)) {
			return ValidationStatus.warning("Parameter " + parameter.getName() + " does not contain a unique value.");
		} else {
			Component component = PropertiesUtils.getComponentFor(camelModelElement);
			if (component != null && value.equals(component.getScheme())) {
				return ValidationStatus.error(NLS.bind(Messages.validationSameComponentIdAndComponentDefinitionId, parameter.getName(), value));
			}
		}
		
		return ValidationStatus.ok();
	}
}