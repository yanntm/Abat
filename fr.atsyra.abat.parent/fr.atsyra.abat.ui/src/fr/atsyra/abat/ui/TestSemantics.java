package fr.atsyra.abat.ui;

import org.eclipse.core.resources.IFile;

import fr.atsyra.abat.SerializationUtil;
import fr.atsyra.abat.abat.AttackTree;

public class TestSemantics extends FileAction {

	@Override
	protected void workWithFile(IFile file, StringBuilder log) {
		String path = file.getRawLocationURI().getPath();
		AttackTree tree = SerializationUtil.fileToAbatSystem(path);
		
		System.out.println("A.B.B.A");
		
	}

	@Override
	protected String getServiceName() {		
		return "Test Semantics";
	}


}
