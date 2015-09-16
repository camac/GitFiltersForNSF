package com.gregorbyte.designer.swiper.builder.pre;

import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import com.gregorbyte.designer.swiper.action.FilterMetadataAction;
import com.gregorbyte.designer.swiper.util.SwiperUtil;
import com.ibm.designer.domino.ide.resources.DominoResourcesPlugin;
import com.ibm.designer.domino.ide.resources.NsfException;
import com.ibm.designer.domino.ide.resources.project.IDominoDesignerProject;
import com.ibm.designer.domino.team.util.SyncUtil;

public class SwiperPreSyncBuilder extends IncrementalProjectBuilder {

	public static final String BUILDER_ID = "com.gregorbyte.designer.swiper.SwiperPreSyncBuilder";

	IDominoDesignerProject designerProject = null;
	IProject diskProject = null;
	FilterMetadataAction filterAction = null;

	public SwiperPreSyncBuilder() {

	}

	public SwiperPreSyncBuilder(IDominoDesignerProject designerProject) {
		this.designerProject = designerProject;
	}

	public IDominoDesignerProject getDesignerProject() {
		return this.designerProject;
	}

	public void initialize() {

		if (this.designerProject != null) {
			try {
				this.diskProject = SyncUtil.getAssociatedDiskProject(
						this.designerProject, false);

				if (this.diskProject != null) {
					this.filterAction = new FilterMetadataAction();
					this.filterAction.setSyncProjects(this.designerProject,
							this.diskProject);
				}
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}

	}

	@SuppressWarnings("rawtypes")
	protected IProject[] build(int kind, Map args, IProgressMonitor monitor)
			throws CoreException {

		SwiperUtil.logInfo("Swiper: PreSyncBuilder");

		try {
			this.designerProject = DominoResourcesPlugin
					.getDominoDesignerProject(getProject());
		} catch (NsfException e) {
			e.printStackTrace();
		}

		if ((this.designerProject == null)
				|| (!this.designerProject.isProjectInitialized())
				|| (!SyncUtil
						.isConfiguredForSourceControl(this.designerProject))) {
			return null;
		}

		if ((!SyncUtil.isSourceControlEnabled())
				|| (!SwiperUtil.isAutoExportEnabled())) {
			return null;
		}

		initialize();

		try {
			IResourceDelta delta = getDelta(getProject());

			if (delta != null) {
				boolean isRelevant = isRelevant(delta);

				if (!isRelevant) {
					return null;
				}

				SyncUtil.logToConsole("--GO Swiper---");
				SyncUtil.logToConsole("Starting MetaData filter");

				delta.accept(new SwiperPreVisitor(monitor, this));

				ResourcesPlugin.getWorkspace().save(false, monitor);

			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;

	}

	private boolean isRelevant(IResourceDelta delta) throws CoreException {
		final boolean[] arrayOfBoolean = new boolean[1];
		delta.accept(new IResourceDeltaVisitor() {
			public boolean visit(IResourceDelta paramAnonymousIResourceDelta) {
				switch (paramAnonymousIResourceDelta.getKind()) {
				case 1:
					if ((paramAnonymousIResourceDelta.getResource() instanceof IFile)) {
						arrayOfBoolean[0] = true;
						return false;
					}
					break;
				case 4:
					if ((paramAnonymousIResourceDelta.getResource() instanceof IFile)) {
						arrayOfBoolean[0] = true;
						return false;
					}
					break;
				case 2:
					IResource localIResource = paramAnonymousIResourceDelta
							.getResource();
					if (localIResource.getType() == 1) {
						arrayOfBoolean[0] = true;
						return false;
					}
					break;
				}
				return true;
			}
		});
		return arrayOfBoolean[0];
	}

}
