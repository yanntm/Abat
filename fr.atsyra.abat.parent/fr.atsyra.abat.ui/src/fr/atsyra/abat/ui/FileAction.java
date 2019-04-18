package fr.atsyra.abat.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;


public abstract class FileAction implements IObjectActionDelegate {

		private Shell shell;
		private List<IFile> files = new ArrayList<IFile>();
		private Logger log =Logger.getLogger("fr.atsyra.abat");

		public void setShell(Shell shell) {
			this.shell = shell;
		}
		/**
		 * @see IObjectActionDelegate#setActivePart(IAction, IWorkbenchPart)
		 */
		public void setActivePart(IAction action, IWorkbenchPart targetPart) {
			shell = targetPart.getSite().getShell();
		}

		
		public Logger getLog() {
			return log;
		}
		/**
		 * @see IActionDelegate#run(IAction)
		 */
		public void run(IAction action) {
			ConsoleAdder.startConsole();
			StringBuilder sb = new StringBuilder();
			for (IFile file : files) {
				if (file != null) {
					workWithFile(file,sb);
				}
				log.info(getServiceName() + " was executed on " + file.getName());
				java.lang.System.err.println(getServiceName() + " was executed on " + file.getName());
			}
			InputDialog id = new InputDialog(shell, "Results", "Test of membership for traces reported :", sb.toString(), null);			
			id.open();
			id.getReturnCode();
			MessageDialog.openInformation(
					shell,
					"Verification result",
					getServiceName() + "  :\n" + sb.toString());

			//log.info(getServiceName() + " operation successfully produced files : " + sb.toString());
			
			files.clear();
		}


		
		/**
		 * Work on current file; log contents whown at end of operation
		 * @param file
		 * @param log
		 */
		protected abstract void workWithFile(IFile file, StringBuilder log);

		protected abstract String getServiceName() ;

		
		/**
		 * @see IActionDelegate#selectionChanged(IAction, ISelection)
		 */
		public void selectionChanged(IAction action, ISelection selection) {
			files.clear();
			if (selection instanceof IStructuredSelection) {
				IStructuredSelection ts = (IStructuredSelection) selection;
				for (Object s : ts.toList()) {
					if (s instanceof IResource) {
						
						try {
							((IResource) s).accept(new IResourceVisitor() {
								
								@Override
								public boolean visit(IResource resource) throws CoreException {
									if (resource instanceof IFile) {
										IFile file = (IFile) resource;
										if (file.getFileExtension()!=null && "abat".contains(file.getFileExtension()) ) {
											String fname = file.getFullPath().toPortableString();
											files.add(file);
										}							
									}
									// descend into subfolders
									return true;
								}
							});
						} catch (CoreException e) {
							e.printStackTrace();
						}
						
					}
					
					
				}
			}
		}

		
		
		protected void warn(Exception e) {
			MessageDialog.openWarning(
					shell,
					getServiceName(),
					getServiceName() + " operation raised an exception " + e.getMessage() + " while analyzing \n Please make sure your plugins"
					+ "are up to date. If you can reproduce this error, please mail us your model (ddd@lip6.fr) and we will try to correct the problem. " );
			e.printStackTrace();
		}
}
