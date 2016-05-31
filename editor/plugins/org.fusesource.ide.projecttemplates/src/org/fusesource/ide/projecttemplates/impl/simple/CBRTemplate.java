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
package org.fusesource.ide.projecttemplates.impl.simple;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.zip.ZipInputStream;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
import org.fusesource.ide.projecttemplates.adopters.AbstractProjectTemplate;
import org.fusesource.ide.projecttemplates.adopters.configurators.MavenTemplateConfigurator;
import org.fusesource.ide.projecttemplates.adopters.configurators.TemplateConfiguratorSupport;
import org.fusesource.ide.projecttemplates.adopters.creators.TemplateCreatorSupport;
import org.fusesource.ide.projecttemplates.adopters.creators.UnzipStreamCreator;
import org.fusesource.ide.projecttemplates.adopters.util.CamelDSLType;
import org.fusesource.ide.projecttemplates.internal.ProjectTemplatesActivator;
import org.fusesource.ide.projecttemplates.util.NewProjectMetaData;
import org.fusesource.ide.projecttemplates.util.camel.ICamelFacetDataModelProperties;

/**
 * @author lhein
 */
public class CBRTemplate extends AbstractProjectTemplate {

	/* (non-Javadoc)
	 * @see org.fusesource.ide.projecttemplates.adopters.AbstractProjectTemplate#supportsDSL(org.fusesource.ide.projecttemplates.adopters.util.CamelDSLType)
	 */
	@Override
	public boolean supportsDSL(CamelDSLType type) {
		switch (type) {
			case BLUEPRINT:	return true;
			case SPRING:	return false;
			case JAVA:		return true;
			case ROUTES:	return false;
			default:		return false;
		}	
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.fusesource.ide.projecttemplates.adopters.AbstractProjectTemplate#getCreator(org.fusesource.ide.projecttemplates.util.NewProjectMetaData)
	 */
	@Override
	public TemplateCreatorSupport getCreator(NewProjectMetaData projectMetaData) {
		return new CBRUnzipTemplateCreator();
	}
	
	/* (non-Javadoc)
	 * @see org.fusesource.ide.projecttemplates.adopters.AbstractProjectTemplate#getConfigurator()
	 */
	@Override
	public TemplateConfiguratorSupport getConfigurator() {
		return new CBRTemplateConfigurator();
	}

	/**
	 * creator class for the CBR simple template 
	 */
	private class CBRUnzipTemplateCreator extends UnzipStreamCreator {

		private static final String TEMPLATE_FOLDER = "templates/simple/cbr/";
		private static final String TEMPLATE_BLUEPRINT = "simple-fuse-cbr-blueprint.zip";
		private static final String TEMPLATE_SPRING = "simple-fuse-cbr-spring.zip";
		private static final String TEMPLATE_JAVA = "simple-fuse-cbr-java.zip";
		
		/* (non-Javadoc)
		 * @see org.fusesource.ide.projecttemplates.adopters.creators.InputStreamCreator#getTemplateStream(org.fusesource.ide.projecttemplates.util.NewProjectMetaData)
		 */
		@Override
		public InputStream getTemplateStream(NewProjectMetaData metadata) throws IOException {
			String bundleEntry = null;
			switch (metadata.getDslType()) {
				case BLUEPRINT:	bundleEntry = String.format("%s%s", TEMPLATE_FOLDER, TEMPLATE_BLUEPRINT);
								break;
				case SPRING:	bundleEntry = String.format("%s%s", TEMPLATE_FOLDER, TEMPLATE_SPRING);
								break;
				case JAVA:		bundleEntry = String.format("%s%s", TEMPLATE_FOLDER, TEMPLATE_JAVA);
								break;
				default:
			}
			URL archiveUrl = ProjectTemplatesActivator.getBundleContext().getBundle().getEntry(bundleEntry);
			if (archiveUrl != null) {
				InputStream is = null;
				try {
					is = archiveUrl.openStream();
					return new ZipInputStream(is, Charset.forName("UTF-8"));
				} catch (IOException ex) {
					ProjectTemplatesActivator.pluginLog().logError(ex);
				}			
			}
			return null;
		}
	}
	
	/**
	 * configurator class for the CBR simple template 
	 */
	private class CBRTemplateConfigurator extends MavenTemplateConfigurator {
		
		/* (non-Javadoc)
		 * @see org.fusesource.ide.projecttemplates.adopters.configurators.MavenTemplateConfigurator#configure(org.eclipse.core.resources.IProject, org.fusesource.ide.projecttemplates.util.NewProjectMetaData, org.eclipse.core.runtime.IProgressMonitor)
		 */
		@Override
		public boolean configure(IProject project, NewProjectMetaData metadata, IProgressMonitor monitor) {
			boolean ok = super.configure(project, metadata, monitor);
			
			if (ok) {
				try {
					// now add camel facet
					String[] camelVersionParts = metadata.getCamelVersion().split("\\.");
					if (camelVersionParts.length>1) {
						String camelFacetVersion = String.format("%s.%s", camelVersionParts[0], camelVersionParts[1]); 
						IDataModel dm = getCamelFacetDataModel(metadata);
						installFacet(project, ICamelFacetDataModelProperties.CAMEL_PROJECT_FACET, camelFacetVersion, dm, monitor);
					}
				} catch (CoreException ex) {
					ProjectTemplatesActivator.pluginLog().logError(ex);
					ok = false;
				}
			}
			
			return ok;
		}
	}
}
