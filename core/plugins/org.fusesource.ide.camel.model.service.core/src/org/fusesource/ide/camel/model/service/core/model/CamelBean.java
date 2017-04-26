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
package org.fusesource.ide.camel.model.service.core.model;

import org.w3c.dom.Node;

/**
 * @author bfitzpat
 */
public class CamelBean extends GlobalDefinitionCamelModelElement {

	public static final String PROP_ID = "id"; //$NON-NLS-1$
	public static final String PROP_CLASS = "class"; //$NON-NLS-1$
	public static final String PROP_SCOPE = "scope"; //$NON-NLS-1$
	public static final String PROP_DEPENDS_ON = "depends-on"; //$NON-NLS-1$
	public static final String PROP_INIT_METHOD = "init-method"; //$NON-NLS-1$
	public static final String PROP_DESTROY_METHOD = "destroy-method"; //$NON-NLS-1$
	public static final String PROP_FACTORY_METHOD = "factory-method"; //$NON-NLS-1$
	public static final String PROP_FACTORY_BEAN = "factory-bean"; //$NON-NLS-1$
	public static final String ARG_TYPE = "type"; //$NON-NLS-1$
	public static final String ARG_VALUE = "value"; //$NON-NLS-1$
	public static final String PROP_NAME = "name"; //$NON-NLS-1$
	public static final String PROP_VALUE = "value"; //$NON-NLS-1$
	public static final String TAG_PROPERTY = "property"; //$NON-NLS-1$
	public static final String TAG_ARGUMENT = "argument"; //$NON-NLS-1$
	public static final String TAG_CONSTRUCTOR_ARG = "constructor-arg"; //$NON-NLS-1$
	
	/**
	 * @param parent
	 * @param underlyingNode
	 */
	public CamelBean() {
		super(null, null);
	}

	/**
	 * @param parent
	 * @param underlyingXmlNode
	 */
	public CamelBean(AbstractCamelModelElement parent, Node underlyingXmlNode) {
		super(parent, underlyingXmlNode);
	}
	
	public CamelBean(String name) {
		super(null, null);
		setParameter(PROP_CLASS, name);
	}
	public String getClassName() {
		return (String)getParameter(PROP_CLASS);
	}
	public void setClassName(String name) {
		setParameter(PROP_CLASS, name);
	}
	public String getScope() {
		return (String)getParameter(PROP_SCOPE);
	}
	public void setScope(String value) {
		setParameter(PROP_SCOPE, value);
	}
	public String getDependsOn() {
		return (String)getParameter(PROP_DEPENDS_ON);
	}
	public void setDependsOn(String value) {
		setParameter(PROP_DEPENDS_ON, value);
	}
	public String getInitMethod() {
		return (String)getParameter(PROP_INIT_METHOD);
	}
	public void setInitMethod(String value) {
		setParameter(PROP_INIT_METHOD, value);
	}
	public String getDestroyMethod() {
		return (String)getParameter(PROP_DESTROY_METHOD);
	}
	public void setDestroyMethod(String value) {
		setParameter(PROP_DESTROY_METHOD, value);
	}
	

	/* (non-Javadoc)
	 * @see org.fusesource.ide.camel.model.service.core.model.CamelModelElement#setParent(org.fusesource.ide.camel.model.service.core.model.CamelModelElement)
	 */
	@Override
	public void setParent(AbstractCamelModelElement parent) {
		super.setParent(parent);
		if (parent != null && parent.getXmlNode() != null && getXmlNode() != null) {
			boolean alreadyChild = false;
			for (int i = 0; i < parent.getXmlNode().getChildNodes().getLength(); i++) {
				if (parent.getXmlNode().getChildNodes().item(i).isEqualNode(getXmlNode())) {
					alreadyChild = true;
					break;
				}
			}
			if (!alreadyChild) {				
				parent.getXmlNode().appendChild(getXmlNode());	
			}
		}
	}
}