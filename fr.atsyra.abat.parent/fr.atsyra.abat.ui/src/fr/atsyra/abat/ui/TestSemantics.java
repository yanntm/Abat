package fr.atsyra.abat.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


import org.eclipse.core.resources.IFile;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;

import fr.atsyra.abat.BasicAbatSerializer;
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
import fr.atsyra.abat.abat.Wand;
import fr.atsyra.abat.abat.Wsand;

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
			HashSet<Tree> must = new HashSet<>();
			HashSet<Leaf> may = new HashSet<>();
			gatherInitial (cont.getRoot(), may);			
			
			if (check(cont, trace.getActs().stream().map(l->l.getName()).collect(Collectors.toList()), must, may, new HashSet<>())) {
				log.append("Trace \""+ trace.getName() + "\" accepted \n");
			} else {
				log.append("Trace \""+ trace.getName() + "\" rejected \n");
			}
		}
	}
	
	
	
	private boolean check(AttackTree cont, List<String> trace, Set<Tree> must, Set<Leaf> may, Set<Tree> marked) {
		if (marked.contains(cont.getRoot())) {
			if (trace.size() == 0) {
				getLog().info("Reached the end of the trace, we are accepted.");
				return true;
			} else {
				getLog().fine("Reached the goal, but trace is not finished. Currently not accepting longer suffix of other attacks.");
				return false;
			}
		} else if (trace.size() == 0) {
			getLog().fine("Reached the end of the trace but we have not validated the goal.");
			return false;
		} else if (may.isEmpty()) {
			getLog().fine("Still elements in the trace, but nowhere to match them in the tree.");
			return false;
		} else {
			String label = trace.get(0);
			List<List<Leaf>> N = new ArrayList<>();
			List<Leaf> possible = may.stream().filter(l ->l.getName().equals(label)).collect(Collectors.toList());
			if (possible.size() == 0) {
				return false;
			} else if (possible.size() == 1) {
				N.add(possible);
			} else {
				if (possible.size() >= 32) {
					throw new RuntimeException("Unsupported too large operands ");
				}
				int totalSubsetCount = (int) Math.pow(2, possible.size()) - 1;
				for (int i = 1; i < totalSubsetCount +1 ; i++) {
					List<Leaf> toadd = new ArrayList<Leaf>(Integer.bitCount(i));
					//List<Integer> test = new ArrayList<Integer>();
					for (int j = 0 ; j < possible.size()+1 ; j++) {
						if (  ((i  >> j) & 1)  == 1 ) {
							toadd.add(possible.get(j));
							// test.add(j);
						}
					}
					//System.out.println("Test i="+i+" set="+test);
					N.add(toadd);					
				}
			}
			getLog().info("Starting recursion for action "+ label +" with " + N.size() + " branches to explore.");
			
			
			for (List<Leaf> chosenN :  N) {
				
				getLog().fine("Executing label :" + label + " with chosen size :" + chosenN.size());
				getLog().fine("chosen N :" + getMarkedString(cont.getRoot(), new HashSet<Tree>(chosenN), "N" ));
				boolean mustOk = true;
				Set<Tree> newMarked = new HashSet<Tree>(chosenN);
				for (Tree t : must) {					
					if (! hasProgressed(t,newMarked)) {
						mustOk = false;
						break;
					}
				}
				if (! mustOk) {
					getLog().fine("The N that was chosen did not cover MUST criterion.");
					// bad choice of N
					continue;
				}
				
				Set<Tree> nextMust = new HashSet<>();
				Set<Leaf> nextMay = new HashSet<>(may);
				nextMay.removeAll(chosenN);
				Set<Tree> nextMarked = new HashSet<>(marked);
				nextMarked.addAll(chosenN);
				for (Leaf child : chosenN) {
					propagate (cont, child, nextMust, nextMay, nextMarked);
				}
				
				getLog().info("Obtained after executing "+label + " state =" + getMarkedString(cont.getRoot(), nextMarked, "OK"));
				getLog().info("Must =" + getMarkedString(cont.getRoot(), nextMust, "REQ"));
				getLog().info("May =" + getMarkedString(cont.getRoot(), nextMay, "MAY"));
				
				if ( check(cont,new ArrayList<>(trace.subList(1, trace.size())), nextMust, nextMay , nextMarked) ) {
					return true;
				}
			}
			getLog().info("Backtracking action "+ label +" all " + N.size() + " branches to explore proved to be dead ends.");
			return false;
		}
	}



	private void propagate(AttackTree cont, Tree newlyValid, Set<Tree> must, Set<Leaf> may, Set<Tree> marked) {
		if (newlyValid == cont.getRoot()) {
			return;
		}
		Tree par = (Tree) newlyValid.eContainer();
		
		if (par instanceof Or) {
			Or or = (Or) par;			
			removeLeaves(or, may);
			marked.add(or);
			propagate(cont, or, must, may, marked);
		} else if (par instanceof Sand) {
			Sand sand = (Sand) par;
			int rs = -1;
			for (int i =0 ; i < sand.getOps().size() - 1 ; i ++) {
				if (sand.getOps().get(i)==newlyValid) {
					rs = i+1 ;
					break;
				}
			}
			if (rs > 0) {
				Tree RS = sand.getOps().get(rs);
				must.add(RS);
				gatherInitial(RS, may);
			} else {
				marked.add(sand);
				propagate(cont, sand, must, may, marked);
			}
		} else if (par instanceof And) {
			And and = (And) par;
			if (and.getOps().stream().allMatch(t -> marked.contains(t))) {
				marked.add(and);
				propagate(cont, and, must, may, marked);
			} else {
				must.add(and);
			}						
		}				
	}



	private void removeLeaves(Tree node, Set<Leaf> may) {
		if (node instanceof Leaf) {
			may.remove(node);
		} else if (node instanceof Operator) {
			Operator op = (Operator) node;
			for (Tree t: op.getOps()) {
				removeLeaves(t, may);
			}
		}
	}

	
	private String getMarkedString (Tree t, Collection<? extends Tree> marked, String text) {
		CustomSerialiser cs = new CustomSerialiser(marked, text);
		return cs.getTextM(t);
	}
	

/*
	{
			
			Set<Leaf> maySee = new HashSet<>();
			gatherInitial (cont.getRoot(), maySee);			
			
			for (int i =0 ; i < trace.getActs().size() ; i++) {
				String label = trace.getActs().get(i);
								
				// replace initials with the right letter by True
				
				for (Leaf l : maySee) {
					if (l.getName().equals(label)) {
						EcoreUtil.replace(l, AbatFactory.eINSTANCE.createTrue());
					}
				}
				
				System.out.println("In trace "+ trace.getName()+ " after executing label " + label + " new tree state " + getText(cont.getRoot()));
				
				propagateTrue(cont.getRoot());
				
				System.out.println("After propagating True new tree state " + getText(cont.getRoot()));

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
	
	*/
	private void propagateTrue (Tree t) {
		if (t instanceof Operator) {
			Operator op = (Operator) t;
			for (Tree child : op.getOps()) {
				propagateTrue(child);
			}
			
			if (op instanceof Or) {
				Or or = (Or) op;
				if (or.getOps().stream().anyMatch(c -> c instanceof True)) {
					EcoreUtil.replace(or, AbatFactory.eINSTANCE.createTrue());
				}				
			} else if (op instanceof Wand || op instanceof And) {
				if (op.getOps().stream().allMatch(c -> c instanceof True)) {
					EcoreUtil.replace(op, AbatFactory.eINSTANCE.createTrue());
				}
			} else if (op instanceof Wsand || op instanceof Sand) {
				if (op.getOps().stream().allMatch(c -> c instanceof True)) {
					EcoreUtil.replace(op, AbatFactory.eINSTANCE.createTrue());
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
			} else if (op instanceof Wand) {
				Wand and = (Wand) op;
				and.getOps().removeIf(c -> c instanceof True);
				if (and.getOps().stream().anyMatch(c -> c instanceof False)) {
					EcoreUtil.replace(and, AbatFactory.eINSTANCE.createFalse());
				} else if (and.getOps().size() == 1) {
					EcoreUtil.replace(and, and.getOps().get(0));
				}
			} else if (op instanceof Wsand) {
				Wsand sand = (Wsand) op;
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
			
		} else if (tree instanceof Wand) {
			Wand and = (Wand) tree;
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
		} else if (tree instanceof Wsand) {
			Wsand sand = (Wsand) tree;
			boolean hasLT = hasTrueDescendant(sand.getOps().get(0));
			if (! hasLT) {
				EcoreUtil.replace(sand, AbatFactory.eINSTANCE.createFalse());
			} else {
				updateFalse(sand.getOps().get(0));
			}
		}
	}
	private boolean hasProgressed(Tree t, Set<Tree> marked) {
		if (marked.contains(t)) {
			return true;
		}
		if (t instanceof Operator) {
			Operator op = (Operator) t;
			return op.getOps().stream().anyMatch(c -> hasProgressed(c, marked));			
		}
		return false;
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

class CustomSerialiser extends BasicAbatSerializer {
	
	Collection<? extends Tree> toMark;
	private String mark;
	
	public CustomSerialiser(Collection<? extends Tree> marked, String mark) {
		this.toMark = marked;
		this.mark = mark;
	}

	@Override
	public Boolean doSwitch(EObject eObject) {
		if (toMark.contains(eObject)) {
			pw.append(mark + ":");
		}
		return super.doSwitch(eObject);
	}
	
}
