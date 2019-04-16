package fr.atsyra.abat;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

import org.eclipse.emf.ecore.EObject;

import fr.atsyra.abat.abat.And;
import fr.atsyra.abat.abat.AttackTree;
import fr.atsyra.abat.abat.False;
import fr.atsyra.abat.abat.Leaf;
import fr.atsyra.abat.abat.Or;
import fr.atsyra.abat.abat.Sand;
import fr.atsyra.abat.abat.True;
import fr.atsyra.abat.abat.util.AbatSwitch;


public class BasicAbatSerializer extends AbatSwitch<Boolean>{

	protected PrintWriter pw;


	public void serialize (EObject modelElement, OutputStream stream) {
		setStream(stream);

		doSwitch(modelElement);		
		close();
	}

	public void close() {
		pw.flush();
		pw.close();
		pw = null;
	}

	public static String getText (EObject modelElement) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		BasicAbatSerializer bgs = new BasicAbatSerializer();
		bgs.serialize(modelElement, bos);
		return bos.toString();
	}
	
	public void setStream(OutputStream stream) {
		pw = new PrintWriter(stream);
	}
	
	@Override
	public Boolean caseAttackTree(AttackTree tree) {
		doSwitch(tree.getRoot());
		return true;
	}
	
	@Override
	public Boolean caseAnd(And node) {
		pw.print("AND(");
		doSwitch(node.getLeft());
		pw.print(",");
		doSwitch(node.getRight());
		pw.print(")");
		return true;
	}
	
	@Override
	public Boolean caseOr(Or node) {
		pw.print("OR(");
		doSwitch(node.getLeft());
		pw.print(",");
		doSwitch(node.getRight());
		pw.print(")");
		return true;
	}
	
	@Override
	public Boolean caseSand(Sand node) {
		pw.print("SAND(");
		doSwitch(node.getLeft());
		pw.print(",");
		doSwitch(node.getRight());
		pw.print(")");
		return true;
	}
	
	@Override
	public Boolean caseLeaf(Leaf l) {
		pw.print("\"" + l.getName() + "\"");
		return true;
	}
	
	@Override
	public Boolean caseTrue(True object) {
		pw.print("true");
		return true;
	}

	@Override
	public Boolean caseFalse(False object) {
		pw.print("false");
		return true;
	}

	
}
