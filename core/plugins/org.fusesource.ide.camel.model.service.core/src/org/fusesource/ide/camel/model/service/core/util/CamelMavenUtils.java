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
package org.fusesource.ide.camel.model.service.core.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Repository;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.fusesource.ide.camel.model.service.core.internal.CamelModelServiceCoreActivator;

public class CamelMavenUtils {

	private CamelMavenUtils() {
		// util class
	}
	
	public static List<Repository> getRepositories(IProject project) {
		IMavenProjectFacade projectFacade = getMavenProjectFacade(project);
		List<Repository> reps = new ArrayList<>();
		if (projectFacade != null) {
			try {
				MavenProject mavenProject = projectFacade.getMavenProject(new NullProgressMonitor());
				reps.addAll(getRepositories(mavenProject));
			} catch (CoreException e) {
				CamelModelServiceCoreActivator.pluginLog().logError(
						"Maven project has not been found (not imported?). Repositories won't be resolved.", e);
			}
		}
		return reps;
	}
	
	public static List<Repository> getRepositories(MavenProject project) {
		if (project != null) {
			String pomPath = project.getFile().getPath(); // TODO: check if we need to append pom.xml
			final File pomFile = new File(pomPath);
			if (!pomFile.exists() || !pomFile.isFile()) {
				return Collections.emptyList();
			}
			try {
				final Model model = MavenPlugin.getMaven().readModel(pomFile);
				List<Repository> repos = new ArrayList<>();
				if (model.getRepositories() != null) {
					repos.addAll(model.getRepositories());
				}
				if (model.getPluginRepositories() != null) {
					repos.addAll(model.getPluginRepositories());
				}
				return repos;
			} catch (Exception ex) {
				CamelModelServiceCoreActivator.pluginLog().logError(ex);
			}
		}
		return Collections.emptyList();
	}

	private static void translateVariables(List<Dependency> deps, Model model) {
		for (Dependency dep : deps) {
			if (dep.getVersion() != null && dep.getVersion().startsWith("${")) {
				String propName = dep.getVersion().substring(2, dep.getVersion().length()-1);
				if (model.getProperties() != null) {
					String version = (String)model.getProperties().get(propName);
					dep.setVersion(version);
				}
			}
		}
	}

	/**
	 * /!\ public for test purpose
	 * 
	 * @param project
	 * @return the Maven project facade corresponding to the supplied project
	 */
	public static IMavenProjectFacade getMavenProjectFacade(IProject project) {
		final IMavenProjectRegistry projectRegistry = MavenPlugin.getMavenProjectRegistry();
		final IFile pomIFile = project.getFile(new Path(IMavenConstants.POM_FILE_NAME));
		return projectRegistry.create(pomIFile, true, new NullProgressMonitor());
	}

	/**
	 * checks for the camel version in the dependencies of the pom.xml
	 * 
	 * @param project
	 * @return
	 */
	public static String getCamelVersionFromMaven(IProject project) {
		List<Dependency> deps = getDependencyList(project);
		return getCamelVersionFromDependencies(deps);
	}

	public static Model getMavenModel(IProject project) {
		return getMavenModel(project, false);
	}
	
	public static Model getMavenModel(IProject project, boolean resolveFully) {
		if (resolveFully) {
			IMavenProjectFacade m2facade = getMavenProjectFacade(project);
			try {
				MavenProject m2Project = m2facade.getMavenProject(new NullProgressMonitor());
				return m2Project.getModel();
			} catch (CoreException ex) {
				CamelModelServiceCoreActivator.pluginLog().logError(ex);
			}
		} else {
			try {
				return MavenPlugin.getMaven().readModel(project.getFile(IMavenConstants.POM_FILE_NAME).getContents());
			} catch (CoreException ex) {
				CamelModelServiceCoreActivator.pluginLog().logError(ex);
			}
		}
		return null;
	}

	public static List<Dependency> getDependencyList(IProject project) {
		return getDependencyList(project, false);
	}
	
	public static List<Dependency> getDependencyList(IProject project, boolean includeManagedDependencies) {
		IMavenProjectFacade m2facade = getMavenProjectFacade(project);
		
		List<Dependency> deps = new ArrayList<>();
		if(m2facade != null) {
			try {
				MavenProject m2Project = m2facade.getMavenProject(new NullProgressMonitor());
				deps.addAll(m2Project.getCompileDependencies());
				deps.addAll(m2Project.getDependencies());
				deps.addAll(m2Project.getRuntimeDependencies());
				deps.addAll(m2Project.getSystemDependencies());
				deps.addAll(m2Project.getTestDependencies());
				if (m2Project.getDependencyManagement() != null && includeManagedDependencies) {
					deps.addAll(m2Project.getDependencyManagement().getDependencies());
				}
				translateVariables(deps, m2Project.getModel());
			} catch (CoreException ex) {
				CamelModelServiceCoreActivator.pluginLog().logError(ex);
			}
		}
		
		return deps;
	}
	
	private static String getCamelVersionFromDependencies(List<Dependency> deps) {
		for (Dependency pomDep : deps) {
			System.err.println(String.format("%s:%s:%s", pomDep.getGroupId(), pomDep.getArtifactId(), pomDep.getVersion()));
			if (pomDep.getGroupId().equalsIgnoreCase(CamelCatalogUtils.CATALOG_KARAF_GROUPID) && 
				pomDep.getArtifactId().startsWith("camel-")) {
				return pomDep.getVersion();
			}
		}
		return null;//CamelCatalogUtils.DEFAULT_CAMEL_VERSION;
	}

	/**
	 * checks for the camel version in the dependencies of the pom.xml
	 * 
	 * @param project
	 * @return
	 */
	public static String getWildFlyCamelVersionFromMaven(IProject project) {
		// get any wildfly camel dep
		List<Dependency> deps = getDependencyList(project);
		for (Dependency pomDep : deps) {
			if (pomDep.getGroupId().equalsIgnoreCase(CamelCatalogUtils.CATALOG_WILDFLY_GROUPID)) {
				return pomDep.getVersion();
			}
		}
		return null;
	}
	
	public static List<List<String>> getAdditionalRepos() {
		List<List<String>> repoList = new ArrayList<>();
		// add the ASF snapshot repo for cutting edge camel version access
		repoList.add(Arrays.asList("asf-snapshots", "https://repository.apache.org/content/groups/snapshots"));
		// public asf repo
		repoList.add(Arrays.asList("asf-public", "https://repo.maven.apache.org/maven2"));
		// add the JBoss Products GA repo
		repoList.add(Arrays.asList("jboss-products-ga", "https://repository.jboss.org/nexus/content/groups/product-ga/"));
		repoList.add(Arrays.asList("redhat-ga", "https://maven.repository.redhat.com/ga/"));
		IPreferenceStore s = new ScopedPreferenceStore(InstanceScope.INSTANCE, "org.fusesource.ide.projecttemplates");
		boolean enabled = s.getBoolean("enableStagingRepositories");
		if (enabled) {
			String repos = s.getString("stagingRepositories");
			repoList.addAll(Arrays.asList(repos.split(";"))
					.stream()
					.map(repoName -> Arrays.asList(repoName.split(",")))
					.collect(Collectors.toList()));
		}
		return repoList;
	}
}
