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
			
			AttackTree cont = AbatFactory.eINSTANCE.createAttackTree();
			cont.setRoot(tree);
			 
			for (int i =0 ; i < trace.getActs().size() ; i++) {
				String label = trace.getActs().get(i);
				Set<Leaf> initialNodes = new HashSet<>();
				gatherInitial (cont.getRoot(), initialNodes);
				
				// replace initials with the right letter by True
				for (Leaf l : initialNodes) {
					if (l.getName().equals(label)) {
						EcoreUtil.replace(l, AbatFactory.eINSTANCE.createTrue());
					}
				}
				
				System.out.println("In trace "+ trace.getName()+ " after executing label " + label + " new tree state " + getText(cont.getRoot()));
				
				// now iterate downwards, looking for goals that have not been progressing 
				// but should have
				updateFalse(cont.getRoot());
				
				
				System.out.println("After propagating false new tree state " + getText(cont.getRoot()));

				reduceBoolean(cont.getRoot());
				
				
				
				System.out.println("After reducing boolean leaves new tree state " + getText(cont.getRoot()));
				
				if (cont.getRoot() instanceof True) {
					System.out.println("Trace " + trace.getActs().subList(0, i+1) + " is an accepted attack.");
					if (i != trace.getActs().size()-1) {
						System.out.println("Note that the suffix of this trace : " + trace.getActs().subList(i+1, trace.getActs().size()) + "  is not useful.");
					}
					break;
				} else if (cont.getRoot() instanceof False) {
					System.out.println("Trace " + trace.getActs().subList(0, i+1) + " is rejected.");
					if (i != trace.getActs().size()-1) {
						System.out.println("Note that the suffix of this trace : " + trace.getActs().subList(i+1, trace.getActs().size()) + " was not even tested.");
					}
					break;
				}
			}
		}
	}

	private void reduceBoolean(Tree t) {
		if (t instanceof Operator) {
			Operator op = (Operator) t;
			for (Tree child : op.getOps()) {
				reduceBoolean(child);
			}
			
			if (op instanceof Or) {
				Or or = (Or) op;
				or.getOps().removeIf(c -> c instanceof False);
				if (or.getOps().stream().anyMatch(c -> c instanceof True)) {
					EcoreUtil.replace(or, AbatFactory.eINSTANCE.createTrue());
				} else if (or.getOps().size() == 1) {
					EcoreUtil.replace(or, or.getOps().get(0));
				}
			} else if (op instanceof And) {
				And and = (And) op;
				and.getOps().removeIf(c -> c instanceof True);
				if (and.getOps().stream().anyMatch(c -> c instanceof False)) {
					EcoreUtil.replace(and, AbatFactory.eINSTANCE.createFalse());
				} else if (and.getOps().size() == 1) {
					EcoreUtil.replace(and, and.getOps().get(0));
				}
			} else if (op instanceof Sand) {
				Sand sand = (Sand) op;
				if (sand.getOps().get(0) instanceof True) {
					sand.getOps().remove(0);
				} else if (sand.getOps().get(0) instanceof False) {
					EcoreUtil.replace(sand, AbatFactory.eINSTANCE.createFalse());
				} 
				if (sand.getOps().size() == 1) {
					EcoreUtil.replace(sand, sand.getOps().get(0));
				}
			}
		}
	}



	private void updateFalse(Tree tree) {
		if (tree instanceof Or) {
			Or or = (Or) tree;
			
			for (int i = or.getOps().size() - 1 ; i >= 0 ; i--) {
				Tree op = or.getOps().get(i);
				if (! hasTrueDescendant(op)) {
					EcoreUtil.replace(op, AbatFactory.eINSTANCE.createFalse());
				} else {
					updateFalse(op);
				}
			}
			
		} else if (tree instanceof And) {
			And and = (And) tree;
			boolean hasT = false;
			
			for (Tree op : and.getOps()) {
				if (hasTrueDescendant(op)) {
					updateFalse(op);
					hasT = true;
				} else {
					EcoreUtil.replace(op, AbatFactory.eINSTANCE.createFalse());
				}
			}

			if (! hasT) {
				EcoreUtil.replace(and, AbatFactory.eINSTANCE.createFalse());
			}
		} else if (tree instanceof Sand) {
			Sand sand = (Sand) tree;
			boolean hasLT = hasTrueDescendant(sand.getOps().get(0));
			if (! hasLT) {
				EcoreUtil.replace(sand, AbatFactory.eINSTANCE.createFalse());
			} else {
				updateFalse(sand.getOps().get(0));
			}
		}
	}



	private boolean hasTrueDescendant(Tree t) {
		if (t instanceof Operator) {
			Operator op = (Operator) t;
			return op.getOps().stream().anyMatch(c -> hasTrueDescendant(c));			
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
			gatherInitial(sand.getOps().get(0), initialNodes);
		} else if (tree instanceof Operator) {
			Operator op = (Operator) tree;
			for (Tree c : op.getOps()) {
				gatherInitial(c, initialNodes);
			}
		} else {
			System.out.println("Error with unexpected tree node type : " + tree.getClass().getName());
		}
	}

	@Override
	protected String getServiceName() {		
		return "Test Semantics";
	}


}
