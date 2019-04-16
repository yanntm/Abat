package fr.atsyra.abat.ui;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.emf.ecore.util.EcoreUtil;

import static fr.atsyra.abat.BasicAbatSerializer.getText;
import fr.atsyra.abat.SerializationUtil;
import fr.atsyra.abat.abat.AbatFactory;
import fr.atsyra.abat.abat.And;
import fr.atsyra.abat.abat.AttackTree;
import fr.atsyra.abat.abat.False;
import fr.atsyra.abat.abat.Leaf;
import fr.atsyra.abat.abat.Operator;
import fr.atsyra.abat.abat.Or;
import fr.atsyra.abat.abat.Sand;
import fr.atsyra.abat.abat.Trace;
import fr.atsyra.abat.abat.Tree;
import fr.atsyra.abat.abat.True;

public class TestSemantics extends FileAction {

	@Override
	protected void workWithFile(IFile file, StringBuilder log) {
		String path = file.getRawLocationURI().getPath();
		AttackTree model = SerializationUtil.fileToAbatSystem(path);
		
		Tree treeOri = model.getRoot();
		for (Trace trace :model.getTraces()) {
			Tree tree = EcoreUtil.copy(treeOri);
			Or cont = AbatFactory.eINSTANCE.createOr();
			cont.setLeft(tree);
			 
			for (int i =0 ; i < trace.getActs().size() ; i++) {
				String label = trace.getActs().get(i);
				Set<Leaf> initialNodes = new HashSet<>();
				gatherInitial (tree, initialNodes);
				
				// replace initials with the right letter by True
				for (Leaf l : initialNodes) {
					if (l.getName().equals(label)) {
						EcoreUtil.replace(l, AbatFactory.eINSTANCE.createTrue());
					}
				}
				
				System.out.println("In trace "+ trace.getName()+ " after executing label " + label + " new tree state " + getText(tree));
				
				// now iterate downwards, looking for goals that have not been progressing 
				// but should have
				updateFalse(tree);
				tree = cont.getLeft();
				
				System.out.println("After propagating false new tree state " + getText(tree));

				reduceBoolean(tree);
				tree = cont.getLeft();
				
				
				System.out.println("After reducing boolean leaves new tree state " + getText(tree));
				
				if (tree instanceof True) {
					System.out.println("Trace " + trace.getActs().subList(0, i+1) + " is an accepted attack.");
					if (i != trace.getActs().size()-1) {
						System.out.println("Note that the suffix of this trace : " + trace.getActs().subList(i+1, trace.getActs().size()-1) + "  is not useful.");
					}
					break;
				} else if (tree instanceof False) {
					System.out.println("Trace " + trace.getActs().subList(0, i+1) + " is rejected.");
					if (i != trace.getActs().size()-1) {
						System.out.println("Note that the suffix of this trace : " + trace.getActs().subList(i+1, trace.getActs().size()-1) + " was not even tested.");
					}
					break;				
				}
			}
		}
	}



	private void reduceBoolean(Tree t) {
		if (t instanceof Operator) {
			Operator op = (Operator) t;
			reduceBoolean(op.getLeft());
			reduceBoolean(op.getRight());
			
			if (op instanceof Or) {
				Or or = (Or) op;
				if (or.getLeft() instanceof False) {
					EcoreUtil.replace(or, or.getRight());
				} else if (or.getRight() instanceof False) {
					EcoreUtil.replace(or, or.getLeft());
				} else if (or.getLeft() instanceof True || or.getRight() instanceof True) {					
					EcoreUtil.replace(or, AbatFactory.eINSTANCE.createTrue());
				}
			} else if (op instanceof And) {
				And and = (And) op;
				if (and.getLeft() instanceof False || and.getRight() instanceof False) {
					EcoreUtil.replace(and, AbatFactory.eINSTANCE.createFalse());
				} else if (and.getLeft() instanceof True) {
					EcoreUtil.replace(and, and.getRight());
				} else if (and.getRight() instanceof True) {
					EcoreUtil.replace(and, and.getLeft());
				}								
			} else if (op instanceof Sand) {
				Sand sand = (Sand) op;
				if (sand.getLeft() instanceof True) {
					EcoreUtil.replace(sand, sand.getRight());
				} else if (sand.getLeft() instanceof False) {
					EcoreUtil.replace(sand, sand.getLeft());
				}
			}
		}
	}



	private void updateFalse(Tree tree) {
		if (tree instanceof Or) {
			Or or = (Or) tree;
			if (! hasTrueDescendant(or.getLeft())) {
				or.setLeft(AbatFactory.eINSTANCE.createFalse());
			} else {
				updateFalse(or.getLeft());
			}
			if (! hasTrueDescendant(or.getRight())) {
				or.setRight(AbatFactory.eINSTANCE.createFalse());
			} else {
				updateFalse(or.getRight());
			}
		} else if (tree instanceof And) {
			And and = (And) tree;
			boolean hasLT = hasTrueDescendant(and.getLeft());
			boolean hasRT = hasTrueDescendant(and.getRight());
			if ( (!hasLT) && (!hasRT) ) {
				EcoreUtil.replace(and, AbatFactory.eINSTANCE.createFalse());
				
			} else {
				if (hasLT){
					updateFalse(and.getLeft());
				}
				if (hasRT) {
					updateFalse(and.getRight());
				}
			}
		} else if (tree instanceof Sand) {
			Sand sand = (Sand) tree;
			boolean hasLT = hasTrueDescendant(sand.getLeft());
			if (! hasLT) {
				EcoreUtil.replace(sand, AbatFactory.eINSTANCE.createFalse());
			} else {
				updateFalse(sand.getLeft());
			}
		}
	}



	private boolean hasTrueDescendant(Tree t) {
		if (t instanceof Operator) {
			Operator op = (Operator) t;
			return hasTrueDescendant(op.getLeft()) || hasTrueDescendant(op.getRight());
		} else if (t instanceof True) {
			return true;
		}
		return false;
	}



	private void gatherInitial(Tree tree, Set<Leaf> initialNodes) {
		if (tree instanceof Leaf) {
			Leaf leaf = (Leaf) tree;
			initialNodes.add(leaf);			
		} else if (tree instanceof Sand) {
			Sand sand = (Sand) tree;
			gatherInitial(sand.getLeft(), initialNodes);
		} else if (tree instanceof Operator) {
			Operator op = (Operator) tree;
			gatherInitial(op.getLeft(), initialNodes);
			gatherInitial(op.getRight(), initialNodes);
		} else {
			System.out.println("Error with unexpected tree node type : " + tree.getClass().getName());
		}
	}

	@Override
	protected String getServiceName() {		
		return "Test Semantics";
	}


}
