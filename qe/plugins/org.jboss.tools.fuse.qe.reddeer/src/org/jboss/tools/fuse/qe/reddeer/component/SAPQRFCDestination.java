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
package org.jboss.tools.fuse.qe.reddeer.component;

import static org.jboss.tools.fuse.qe.reddeer.component.SAPLabels.DESTINATION;
import static org.jboss.tools.fuse.qe.reddeer.component.SAPLabels.QUEUE;
import static org.jboss.tools.fuse.qe.reddeer.component.SAPLabels.RFC;

/**
 * 
 * @author apodhrad
 *
 */
public class SAPQRFCDestination extends AbstractURICamelComponent {

	public SAPQRFCDestination() {
		super("sap-qrfc-destination");
		addProperty(DESTINATION, "destination");
		addProperty(QUEUE, "queue");
		addProperty(RFC, "rfc");
	}

	@Override
	public String getPaletteEntry() {
		return "SAP qRFC Destination";
	}

}
