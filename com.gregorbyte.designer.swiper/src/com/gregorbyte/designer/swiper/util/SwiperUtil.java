package com.gregorbyte.designer.swiper.util;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;

import com.gregorbyte.designer.swiper.builder.SwiperNature;
import com.gregorbyte.designer.swiper.pref.DoraPreferenceManager;
import com.ibm.commons.log.Log;
import com.ibm.commons.log.LogMgr;
import com.ibm.commons.util.StringUtil;
import com.ibm.designer.domino.ide.resources.DominoResourcesPlugin;
import com.ibm.designer.domino.ide.resources.jni.NotesDesignElement;
import com.ibm.designer.domino.ide.resources.metamodel.IMetaModelConstants;
import com.ibm.designer.domino.ide.resources.project.IDominoDesignerProject;
import com.ibm.designer.domino.team.util.SyncUtil;
import com.ibm.designer.prj.resources.commons.IMetaModelDescriptor;

public class SwiperUtil {

	public static LogMgr SWIPER_LOG = Log.load("com.gregorbyte.designer.swiper", "Logger used for Swiper");
	private static final String MARKER_TYPE = "com.gregorbyte.designer.swiper.xmlProblem";

	public static Set<String> getCanFilterIds() {

		HashSet<String> things = new HashSet<String>();

		things.add(IMetaModelConstants.AGENTS);
		things.add(IMetaModelConstants.SHARED_ELEMENTS);
		things.add(IMetaModelConstants.DATACONNS);
		things.add(IMetaModelConstants.FOLDERS);
		things.add(IMetaModelConstants.FIELDS);
		things.add(IMetaModelConstants.FRAMESET);
		things.add(IMetaModelConstants.JAVAJARS);
		things.add(IMetaModelConstants.DBSCRIPT);
		// Metadata
		things.add(IMetaModelConstants.XSPPAGES);
		things.add(IMetaModelConstants.XSPCCS);
		things.add(IMetaModelConstants.NAVIGATORS);
		things.add(IMetaModelConstants.OUTLINES);
		things.add(IMetaModelConstants.PAGES);
		things.add(IMetaModelConstants.SUBFORMS);
		things.add(IMetaModelConstants.VIEWS);
		things.add(IMetaModelConstants.FORMS);

		things.add(IMetaModelConstants.ABOUTDOC);
		things.add(IMetaModelConstants.DBPROPS);
		things.add(IMetaModelConstants.ICONNOTE);
		things.add(IMetaModelConstants.ACTIONS);
		things.add(IMetaModelConstants.USINGDOC);

		things.add(IMetaModelConstants.SCRIPTLIB);

		return things;
	}

	public static String getPreferenceKey(IMetaModelDescriptor mmd) {
		return "swiper.filter." + mmd.getID();
	}

	public static String getPreferenceKey(String id) {
		return "swiper.filter." + id;
	}

	public static boolean isSetToFilter(IMetaModelDescriptor mmd) {

		String prefKey = getPreferenceKey(mmd);

		logInfo("Checking preference for " + mmd.getName());

		boolean isset = DoraPreferenceManager.getInstance().getBooleanValue(prefKey, false);

		if (isset) {
			logInfo(prefKey + " is currently set to True");
		} else {
			logInfo(prefKey + " is currently set to False");
		}

		return isset;
	}

	public static boolean shouldFilter(IResource resource) {

		NotesDesignElement element = DominoResourcesPlugin.getNotesDesignElement(resource);

		if (element == null) {
			return false;
		}

		boolean hasMetadata = SyncUtil.hasMetadataFile(element);

		if (hasMetadata) {
			logInfo("Design Element Name: " + element.getName() + "Has Metadata");
		} else {
			logInfo("Design Element Name: " + element.getName() + "Does not have metadata");
		}

		IMetaModelDescriptor mmd = element.getMetaModel();

		if (mmd == null) {
			return false;
		}

		String id = mmd.getID();

		if (getCanFilterIds().contains(id)) {

			logInfo("Yes we can filter" + mmd.getName());
			return isSetToFilter(mmd);

		} else {

			logInfo("No we don't filter" + mmd.getName() + " (" + id + ")");
			return false;

		}

	}

	public static boolean isUseDefaultFilter() {
		String key = getPreferenceKey("useDefaultFilter");
		return DoraPreferenceManager.getInstance().getBooleanValue(key, false);
	}

	public static String getDefaultFilterFilePath() {
		return DoraPreferenceManager.getInstance().getValue("defaultFilter", false);
	}

	public static IFile getRelevantDiskFile(IDominoDesignerProject designerProject, IResource designerFile)
			throws CoreException {

		NotesDesignElement designElement = DominoResourcesPlugin.getNotesDesignElement(designerFile);

		IProject diskProject = SyncUtil.getAssociatedDiskProject(designerProject, false);

		IFile diskFile = null;

		if (SyncUtil.hasMetadataFile(designElement)) {

			SwiperUtil.logInfo("Metadata file needed " + designerFile.getName());

			IPath localPath = designerFile.getProjectRelativePath().addFileExtension("metadata");
			diskFile = diskProject.getFile(localPath);

		} else {

			SwiperUtil.logInfo("No Metadata file needed for " + designerFile.getName());
			diskFile = SyncUtil.getPhysicalFile(designerProject, designerFile);

		}

		return diskFile;

	}

	public static QualifiedName getSyncModifiedQualifiedName(IResource paramIResource) {
		QualifiedName localQualifiedName = new QualifiedName("com.gregorbyte.designer.swiper",
				paramIResource.getProjectRelativePath().toString());
		return localQualifiedName;

	}

	public static boolean isModifiedBySync(IResource resource) {

		if (resource.exists()) {
			try {
				resource.refreshLocal(1, new NullProgressMonitor());

				long l1 = resource.getLocalTimeStamp();
				String str = getPersistentSyncTimestamp(resource);
				if (StringUtil.equals(str, String.valueOf(l1))) {
					return false;
				}
				// if (StringUtil.isNotEmpty(str)) {
				// try {
				// long l2 = Long.parseLong(str);
				// long l3 = l2 - l1;
				// if ((l3 > 500L) && (l3 < 2000L)) {
				// return true;
				// }
				// } catch (NumberFormatException localNumberFormatException) {
				// }
				// }
				return true;
			} catch (CoreException localCoreException) {
				localCoreException.printStackTrace();
			}
		}
		return false;
	}

	public static String getPersistentSyncTimestamp(IResource paramIResource) throws CoreException {
		QualifiedName localQualifiedName = getSyncModifiedQualifiedName(paramIResource);
		return paramIResource.getProject().getPersistentProperty(localQualifiedName);
	}

	public static void setSyncTimestamp(IResource paramIResource) {

		if ((paramIResource == null) || (!paramIResource.exists())) {
			return;
		}
		try {
			if (paramIResource.getType() == 1) {
				paramIResource.refreshLocal(1, new NullProgressMonitor());
			}
			if (paramIResource.exists()) {
				long l = paramIResource.getLocalTimeStamp();
				QualifiedName localQualifiedName = getSyncModifiedQualifiedName(paramIResource);
				paramIResource.getProject().setPersistentProperty(localQualifiedName, String.valueOf(l));
			}
		} catch (CoreException localCoreException) {
			localCoreException.printStackTrace();
		}

	}

	public static void logInfo(String message) {
		if (SWIPER_LOG.isInfoEnabled()) {
			SWIPER_LOG.infop("SwiperUtil", "", "Swiper: " + message, new Object[0]);
		}
	}

	public static void addNature(IProject project) {

		SwiperUtil.logInfo("Attempt to Add Nature");

		try {
			IProjectDescription description = project.getDescription();
			String[] natures = description.getNatureIds();

			for (int i = 0; i < natures.length; ++i) {

				if (SwiperNature.NATURE_ID.equals(natures[i])) {
					SwiperUtil.logInfo("Swiper Nature already exists");
					return;
				}
			}

			// Add the nature
			String[] newNatures = new String[natures.length + 1];
			System.arraycopy(natures, 0, newNatures, 0, natures.length);
			newNatures[natures.length] = SwiperNature.NATURE_ID;
			description.setNatureIds(newNatures);
			project.setDescription(description, null);

		} catch (CoreException e) {

			SwiperUtil.logInfo(e.getMessage());
			e.printStackTrace();

		} catch (Exception e) {

			SwiperUtil.logInfo(e.getMessage());
			e.printStackTrace();

		}

	}

	public static void removeNature(IProject project) {

		SwiperUtil.logInfo("Attempt to Remove Nature");

		try {
			IProjectDescription description = project.getDescription();
			String[] natures = description.getNatureIds();

			for (int i = 0; i < natures.length; ++i) {
				if (SwiperNature.NATURE_ID.equals(natures[i])) {
					// Remove the nature
					String[] newNatures = new String[natures.length - 1];
					System.arraycopy(natures, 0, newNatures, 0, i);
					System.arraycopy(natures, i + 1, newNatures, i, natures.length - i - 1);
					description.setNatureIds(newNatures);
					project.setDescription(description, null);
					return;
				}
			}

		} catch (CoreException e) {

			SwiperUtil.logInfo(e.getMessage());
			e.printStackTrace();

		} catch (Exception e) {

			SwiperUtil.logInfo(e.getMessage());
			e.printStackTrace();

		}
	}

	public static void addMarker(IProject project, String message, int severity) {

		try {
			IMarker marker = project.createMarker(MARKER_TYPE);
			marker.setAttribute(IMarker.MESSAGE, message);
			marker.setAttribute(IMarker.SEVERITY, severity);
		} catch (CoreException e) {

		}

	}
	
	public static void addMarker(IFile file, String message, int lineNumber, int severity) {
		try {
			IMarker marker = file.createMarker(MARKER_TYPE);
			marker.setAttribute(IMarker.MESSAGE, message);
			marker.setAttribute(IMarker.SEVERITY, severity);
			if (lineNumber == -1) {
				lineNumber = 1;
			}
			marker.setAttribute(IMarker.LINE_NUMBER, lineNumber);
		} catch (CoreException e) {
		}
	}

	public static void cleanMarkers(IProject project) throws CoreException {

		project.deleteMarkers(MARKER_TYPE, true, IResource.DEPTH_INFINITE);

	}
	
	public static void deleteMarkers(IFile file) {
		try {
			file.deleteMarkers(MARKER_TYPE, false, IResource.DEPTH_ZERO);
		} catch (CoreException ce) {
		}
	}

	
}
