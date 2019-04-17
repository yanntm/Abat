package fr.atsyra.abat;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

import org.eclipse.emf.ecore.EObject;

import fr.atsyra.abat.abat.And;
import fr.atsyra.abat.abat.AttackTree;
import fr.atsyra.abat.abat.False;
import fr.atsyra.abat.abat.Leaf;
import fr.atsyra.abat.abat.Operator;
import fr.atsyra.abat.abat.Or;
import fr.atsyra.abat.abat.Sand;
import fr.atsyra.abat.abat.Tree;
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
		pw.print("AND");
		printOperands(node);
		return true;
	}

	private void printOperands(Operator node) {
		boolean first = true;
		pw.print("(");
		for (Tree t : node.getOps()) {
			if (first) {
				first = false;
			} else {
				pw.print(", ");
			}
			doSwitch(t);			
		}		
		pw.print(")");
	}
	
	@Override
	public Boolean caseOr(Or node) {
		pw.print("OR");
		printOperands(node);
		return true;
	}
	
	@Override
	public Boolean caseSand(Sand node) {
		pw.print("SAND");
		printOperands(node);
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
