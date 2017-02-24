/* Soot - a J*va Optimization Framework
 * Copyright (C) 1999 Patrice Pominville, Raja Vallee-Rai
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 */

/*
 * Modified by the Sable Research Group and others 1997-2003.  
 * See the 'credits' file distributed with Soot for the complete list of
 * contributors.  (Soot is distributed at http://www.sable.mcgill.ca/soot)
 */

package soot.toolkits.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import nu.NUSootConfig;
import soot.Body;
import soot.BooleanType;
import soot.G;
import soot.Local;
import soot.RefType;
import soot.SootClass;
import soot.SootMethod;
import soot.SootMethodRef;
import soot.Unit;
import soot.UnitBox;
import soot.Value;
import soot.VoidType;
import soot.dava.internal.javaRep.DIntConstant;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.NullConstant;
import soot.jimple.Stmt;
import soot.jimple.VirtualInvokeExpr;
import soot.jimple.internal.JInvokeStmt;
import soot.jimple.internal.JVirtualInvokeExpr;
import soot.jimple.internal.JimpleLocal;
import soot.options.Options;
import soot.util.Chain;
import soot.SootMethodRefImpl;
import soot.Type;

/**
 * <p>
 * Represents a CFG where the nodes are {@link Unit} instances and edges
 * represent unexceptional and (possibly) exceptional control flow between
 * <tt>Unit</tt>s.
 * </p>
 *
 * <p>
 * This is an abstract class, providing the facilities used to build CFGs for
 * specific purposes.
 * </p>
 */
public abstract class UnitGraph implements DirectedGraph<Unit> {
	protected List<Unit> heads;
	protected List<Unit> tails;

	protected Map<Unit, List<Unit>> unitToSuccs;
	protected Map<Unit, List<Unit>> unitToPreds;
	protected SootMethod method;
	protected Body body;
	protected Chain<Unit> unitChain;
	static int newLocalCount = 177;

	/**
	 * Performs the work that is required to construct any sort of
	 * <tt>UnitGraph</tt>.
	 *
	 * @param body
	 *            The body of the method for which to construct a control flow
	 *            graph.
	 */
	protected UnitGraph(Body body) {
		this.body = body;
		unitChain = body.getUnits();
		method = body.getMethod();
		if (Options.v().verbose())
			G.v().out.println("[" + method.getName() + "]     Constructing "
					+ this.getClass().getName() + "...");
		
		NUSootConfig config = NUSootConfig.getInstance();
		if(!config.isEnableGraphEnhance())
			return ;
		//XIANG 
		//TODO: deal with myltiple parameter's class
		//TODO: remove dummymain class.
		Iterator<Unit> unitIt = unitChain.snapshotIterator();
		List<String> eventList = new ArrayList<String>();
		eventList.add("onClick");
		eventList.add("onLongClick");
		eventList.add("onFocusChange"); //View v, boolean hasFocus
		eventList.add("onFocusChanged"); //View v, boolean hasFocus
		eventList.add("onKey"); //View v, int keyCode, KeyEvent event
		eventList.add("onKeyDown"); //int keyCode, KeyEvent event
		eventList.add("onKeyUp");  //int keyCode, KeyEvent event
		eventList.add("onKeyLongPress"); //int keyCode, KeyEvent event
		eventList.add("onTouchEvent");  //MotionEvent event
		eventList.add("onTouch"); //View v, MotionEvent event
		
		
		Unit currentUnit = null;
		Unit nextUnit = unitIt.hasNext() ? (Unit) unitIt.next() : null;
		Value savedListener = null;
		SootClass listenerCls = null;
		int count = 0;
		while (nextUnit != null) {
			currentUnit = nextUnit;
			nextUnit = unitIt.hasNext() ? (Unit) unitIt.next() : null;
			//XIANG
			//TODO: add other event callbacks.
			if(currentUnit instanceof Stmt){
				Stmt s = (Stmt)currentUnit;
				if(s.containsInvokeExpr() && s.getInvokeExpr() instanceof InstanceInvokeExpr){
					InstanceInvokeExpr ie = (InstanceInvokeExpr)s.getInvokeExpr();
					SootMethod sm = s.getInvokeExpr().getMethod();
					//if(sm.getName().equals("<init>") && sm.getDeclaringClass().toString().contains("$")){
					if(sm.getName().equals("<init>")){
						savedListener = ie.getBase(); 
						listenerCls = sm.getDeclaringClass();
						
//						if(!listenerCls.isApplicationClass())
//							continue;
//						if(listenerCls.isJavaLibraryClass() || listenerCls.isLibraryClass() || listenerCls.isPhantom())
//							continue;
						
						for(String methodName : eventList){
							//extract method from class
							SootMethod eventMethod = null;
							try{
								eventMethod = listenerCls.getMethodByName(methodName);
							}
							catch(Exception e){}
							if(eventMethod == null) continue;
							//TODO: it could be multiple parameters
							//if(eventMethod.getParameterCount() != 1) continue;
							
							//create method ref for building instrument
							SootMethodRef eventMethodRef = new SootMethodRefImpl(listenerCls,eventMethod.getName(), 
									eventMethod.getParameterTypes(), eventMethod.getReturnType(), false);
							List<Value> args = new ArrayList<Value>();
							boolean doContinue = false;
							for(int i=0; i<eventMethod.getParameterCount(); i++){
								//args.add(NullConstant.v());
								Type t = eventMethod.getParameterTypes().get(i);
								//System.out.println("TYPE: "+t.getEscapedName());
								if(t.getEscapedName().equals("int")){
									args.add(IntConstant.v(0));
								}
								else if(t.getEscapedName().equals("boolean")){
									DIntConstant d = DIntConstant.v(0, BooleanType.v());
									args.add(d);
								}
								else{
									if(savedListener.equals(NullConstant.v())){
										doContinue = true;
										break;
									}
									args.add(savedListener);
								}
							}
							if(doContinue) continue;
							
							InstanceInvokeExpr iie = new JVirtualInvokeExpr(savedListener, eventMethodRef, args);
							JInvokeStmt jis = new JInvokeStmt(iie);
//							System.out.println("ADDED NEW INSTR:"+listenerCls.isApplicationClass()+" "+jis+" "+body.getMethod().getName()+"@"+listenerCls);
//							System.out.println("  ApplicationCls:"+listenerCls.isApplicationClass()+" JAVA:"+listenerCls.isJavaLibraryClass()+"  L:"+listenerCls.isLibraryClass()+" Phantom:"+listenerCls.isPhantom());
							if(nextUnit!=null && nextUnit instanceof Stmt){
								Stmt ns = (Stmt)nextUnit;
								if(!ns.containsInvokeExpr() || !ns.getInvokeExpr().getMethod().getName().equals(methodName)){
									unitChain.insertAfter(jis, currentUnit);
									count++;
								}
							}
							else{
								unitChain.insertAfter(jis, currentUnit);
								count++;
							}
						}
					}
					
					/*if(savedListener!=null && sm.getName().equals("setOnClickListener")){
						//InstanceInvokeExpr iie = new JVirtualInvokeExpr();
//						System.out.println("TEST2:"+s+" ||"+ s.getInvokeExpr().getClass().toString());
//						System.out.println("  Details:BTN:"+ie.getBase()+" VIEW:"+savedView+" LN:"+listenerCls.getMethodByName("onClick"));
						SootMethod onClickMethod = listenerCls.getMethodByName("onClick");
						SootMethodRef onClickMethodRef = new SootMethodRefImpl(listenerCls,onClickMethod.getName(), 
								onClickMethod.getParameterTypes(), onClickMethod.getReturnType(), false);
						List<Value> args = new ArrayList<Value>();
						args.add(savedListener);
						InstanceInvokeExpr iie = new JVirtualInvokeExpr(savedListener, onClickMethodRef, args);
						JInvokeStmt jis = new JInvokeStmt(iie);
						System.out.println("XXYYZZ:"+savedListener+" VS "+ie.getArg(0));
						System.out.println("   [" + method.getName() + "]     Constructing "
								+ method.getDeclaringClass().getName() + "...");
//						System.out.println("  NewInvokExpr:"+jis+" HASBODY:"+onClickMethod.hasActiveBody());
//						if(iie.getMethod().hasActiveBody())
//							System.out.println("  SIZE:"+iie.getMethod().getActiveBody().getUnits().size());
						if(nextUnit!=null && nextUnit instanceof Stmt){
							Stmt ns = (Stmt)nextUnit;
							if(ns.containsInvokeExpr() && ns.getInvokeExpr().getMethod().getName().equals("onClick")){
								
							}
							else{
								unitChain.insertAfter(jis, currentUnit);
								count++;
							}
						}
						//break;
						//TODO
						//sm.getDeclaringClass().getNa
					}
					*/
//					if(sm.getName().equals("onClick")){
//						System.out.println("TEST3: Base:"+ie.getBase()+" Arg:"+s.getInvokeExpr().getArg(0));
//						System.out.println(sm.getReturnType());
//						System.out.println(sm.getParameterType(0));
//						
//					}
				}
			}
		}
		if(count >= 2){
			System.out.println("Added more than 2 onClicks");
			System.out.println("   [" + method.getName() + "]     Constructing "
					+ method.getDeclaringClass().getName() + "...");
		}
		//==============================================================
		

	}

	/**
	 * Utility method for <tt>UnitGraph</tt> constructors. It computes the edges
	 * corresponding to unexceptional control flow.
	 *
	 * @param unitToSuccs
	 *            A {@link Map} from {@link Unit}s to {@link List}s of
	 *            {@link Unit}s. This is an ``out parameter''; callers must pass
	 *            an empty {@link Map}. <tt>buildUnexceptionalEdges</tt> will
	 *            add a mapping for every <tt>Unit</tt> in the body to a list of
	 *            its unexceptional successors.
	 *
	 * @param unitToPreds
	 *            A {@link Map} from {@link Unit}s to {@link List}s of
	 *            {@link Unit}s. This is an ``out parameter''; callers must pass
	 *            an empty {@link Map}. <tt>buildUnexceptionalEdges</tt> will
	 *            add a mapping for every <tt>Unit</tt> in the body to a list of
	 *            its unexceptional predecessors.
	 */
	protected void buildUnexceptionalEdges(Map<Unit, List<Unit>> unitToSuccs,
			Map<Unit, List<Unit>> unitToPreds) {
		Iterator<Unit> unitIt = unitChain.iterator();
		Unit currentUnit, nextUnit;
		nextUnit = unitIt.hasNext() ? (Unit) unitIt.next() : null;
		
		while (nextUnit != null) {
			currentUnit = nextUnit;
			nextUnit = unitIt.hasNext() ? (Unit) unitIt.next() : null;
			
			ArrayList<Unit> successors = new ArrayList<Unit>();

			if (currentUnit.fallsThrough()) {
				// Add the next unit as the successor
				if (nextUnit != null) {
					successors.add(nextUnit);
					
					List<Unit> preds = unitToPreds.get(nextUnit);
					if (preds == null) {
						preds = new ArrayList<Unit>();
						unitToPreds.put(nextUnit, preds);
					}
					preds.add(currentUnit);
				}
			}

			if (currentUnit.branches()) {
				for (UnitBox targetBox : currentUnit.getUnitBoxes()) {
					Unit target = targetBox.getUnit();
					// Arbitrary bytecode can branch to the same
					// target it falls through to, so we screen for duplicates:
					if (!successors.contains(target)) {
						successors.add(target);
						
						List<Unit> preds = unitToPreds.get(target);
						if (preds == null) {
							preds = new ArrayList<Unit>();
							unitToPreds.put(target, preds);
						}
						preds.add(currentUnit);
					}
				}
			}

			// Store away successors
			if (!successors.isEmpty()) {
				successors.trimToSize();
				unitToSuccs.put(currentUnit, successors);
			}
			
		}
		
	}

	/**
	 * <p>
	 * Utility method used in the construction of {@link UnitGraph}s, to be
	 * called only after the unitToPreds and unitToSuccs maps have been built.
	 * </p>
	 *
	 * <p>
	 * <code>UnitGraph</code> provides an implementation of
	 * <code>buildHeadsAndTails()</code> which defines the graph's set of heads
	 * to include the first {@link Unit} in the graph's body, together with any
	 * other <tt>Unit</tt> which has no predecessors. It defines the graph's set
	 * of tails to include all <tt>Unit</tt>s with no successors. Subclasses of
	 * <code>UnitGraph</code> may override this method to change the criteria
	 * for classifying a node as a head or tail.
	 * </p>
	 */
	protected void buildHeadsAndTails() {
		tails = new ArrayList<Unit>();
		heads = new ArrayList<Unit>();

		for (Unit s : unitChain) {
			List<Unit> succs = unitToSuccs.get(s);
			if (succs == null || succs.isEmpty()) {
				tails.add(s);
			}
			List<Unit> preds = unitToPreds.get(s);
			if (preds == null || preds.isEmpty()) {
				heads.add(s);
			}
		}

		// Add the first Unit, even if it is the target of
		// a branch.
		if (!unitChain.isEmpty()) {
			Unit entryPoint = unitChain.getFirst();
			if (!heads.contains(entryPoint)) {
				heads.add(entryPoint);
			}
		}

	}

	/**
	 * Utility method that produces a new map from the {@link Unit}s of this
	 * graph's body to the union of the values stored in the two argument
	 * {@link Map}s, used to combine the maps of exceptional and unexceptional
	 * predecessors and successors into maps of all predecessors and successors.
	 * The values stored in both argument maps must be {@link List}s of
	 * {@link Unit}s, which are assumed not to contain any duplicate
	 * <tt>Unit</tt>s.
	 * 
	 * @param mapA
	 *            The first map to be combined.
	 *
	 * @param mapB
	 *            The second map to be combined.
	 */
	protected Map<Unit, List<Unit>> combineMapValues(
			Map<Unit, List<Unit>> mapA, Map<Unit, List<Unit>> mapB) {
		// The duplicate screen
		Map<Unit, List<Unit>> result = new HashMap<Unit, List<Unit>>(
				mapA.size() * 2 + 1, 0.7f);
		for (Unit unit : unitChain) {
			List<Unit> listA = mapA.get(unit);
			if (listA == null) {
				listA = Collections.emptyList();
			}
			List<Unit> listB = mapB.get(unit);
			if (listB == null) {
				listB = Collections.emptyList();
			}

			int resultSize = listA.size() + listB.size();
			if (resultSize == 0) {
				result.put(unit, Collections.<Unit> emptyList());
			} else {
				List<Unit> resultList = new ArrayList<Unit>(resultSize);
				List<Unit> list = null;
				// As a minor optimization of the duplicate screening,
				// copy the longer list first.
				if (listA.size() >= listB.size()) {
					resultList.addAll(listA);
					list = listB;
				} else {
					resultList.addAll(listB);
					list = listA;
				}
				for (Unit element : list) {
					// It is possible for there to be both an exceptional
					// and an unexceptional edge connecting two Units
					// (though probably not in a class generated by
					// javac), so we need to screen for duplicates. On the
					// other hand, we expect most of these lists to have
					// only one or two elements, so it doesn't seem worth
					// the cost to build a Set to do the screening.
					if (!resultList.contains(element)) {
						resultList.add(element);
					}
				}
				result.put(unit, resultList);
			}
		}
		return result;
	}

	/**
	 * Utility method for adding an edge to maps representing the CFG.
	 * 
	 * @param unitToSuccs
	 *            The {@link Map} from {@link Unit}s to {@link List}s of their
	 *            successors.
	 *
	 * @param unitToPreds
	 *            The {@link Map} from {@link Unit}s to {@link List}s of their
	 *            successors.
	 *
	 * @param head
	 *            The {@link Unit} from which the edge starts.
	 *
	 * @param tail
	 *            The {@link Unit} to which the edge flows.
	 */
	protected void addEdge(Map<Unit, List<Unit>> unitToSuccs,
			Map<Unit, List<Unit>> unitToPreds, Unit head, Unit tail) {
		List<Unit> headsSuccs = unitToSuccs.get(head);
		if (headsSuccs == null) {
			headsSuccs = new ArrayList<Unit>(3); // We expect this list to
			// remain short.
			unitToSuccs.put(head, headsSuccs);
		}
		if (!headsSuccs.contains(tail)) {
			headsSuccs.add(tail);
			List<Unit> tailsPreds = unitToPreds.get(tail);
			if (tailsPreds == null) {
				tailsPreds = new ArrayList<Unit>();
				unitToPreds.put(tail, tailsPreds);
			}
			tailsPreds.add(head);
		}
	}

	/**
	 * @return The body from which this UnitGraph was built.
	 *
	 * @see Body
	 */
	public Body getBody() {
		return body;
	}

	/**
	 * Look for a path in graph, from def to use. This path has to lie inside an
	 * extended basic block (and this property implies uniqueness.). The path
	 * returned includes from and to.
	 *
	 * @param from
	 *            start point for the path.
	 * @param to
	 *            end point for the path.
	 * @return null if there is no such path.
	 */
	public List<Unit> getExtendedBasicBlockPathBetween(Unit from, Unit to) {
		UnitGraph g = this;

		// if this holds, we're doomed to failure!!!
		if (g.getPredsOf(to).size() > 1)
			return null;

		// pathStack := list of succs lists
		// pathStackIndex := last visited index in pathStack
		LinkedList<Unit> pathStack = new LinkedList<Unit>();
		LinkedList<Integer> pathStackIndex = new LinkedList<Integer>();

		pathStack.add(from);
		pathStackIndex.add(new Integer(0));

		int psiMax = (g.getSuccsOf(pathStack.get(0))).size();
		int level = 0;
		while (pathStackIndex.get(0).intValue() != psiMax) {
			int p = (pathStackIndex.get(level)).intValue();

			List<Unit> succs = g.getSuccsOf((pathStack.get(level)));
			if (p >= succs.size()) {
				// no more succs - backtrack to previous level.

				pathStack.remove(level);
				pathStackIndex.remove(level);

				level--;
				int q = pathStackIndex.get(level).intValue();
				pathStackIndex.set(level, new Integer(q + 1));
				continue;
			}

			Unit betweenUnit = (Unit) (succs.get(p));

			// we win!
			if (betweenUnit == to) {
				pathStack.add(to);
				return pathStack;
			}

			// check preds of betweenUnit to see if we should visit its kids.
			if (g.getPredsOf(betweenUnit).size() > 1) {
				pathStackIndex.set(level, new Integer(p + 1));
				continue;
			}

			// visit kids of betweenUnit.
			level++;
			pathStackIndex.add(new Integer(0));
			pathStack.add(betweenUnit);
		}
		return null;
	}

	/* DirectedGraph implementation */
	public List<Unit> getHeads() {
		return heads;
	}

	public List<Unit> getTails() {
		return tails;
	}

	public List<Unit> getPredsOf(Unit u) {
		List<Unit> l = unitToPreds.get(u);
		if (l == null)
			return Collections.emptyList();

		return l;
	}

	public List<Unit> getSuccsOf(Unit u) {
		List<Unit> l = unitToSuccs.get(u);
		if (l == null)
			return Collections.emptyList();

		return l;
	}

	public int size() {
		return unitChain.size();
	}

	public Iterator<Unit> iterator() {
		return unitChain.iterator();
	}

	public String toString() {
		StringBuilder buf = new StringBuilder();
		for (Unit u : unitChain) {
			buf.append("// preds: ").append(getPredsOf(u)).append('\n');
			buf.append(u).append('\n');
			buf.append("// succs ").append(getSuccsOf(u)).append('\n');
		}
		return buf.toString();
	}
}