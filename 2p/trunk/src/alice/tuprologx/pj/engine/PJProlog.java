/*
 * Prolog.java
 *
 * Created on March 12, 2007, 2:41 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package alice.tuprologx.pj.engine;


import alice.tuprologx.pj.model.*;

import java.util.*;

/**
 *
 * @author maurizio
 */
public class PJProlog /*extends alice.tuprolog.Prolog*/ {
    
    protected alice.tuprolog.TuProlog engine;
    
    public PJProlog() {
        engine = new alice.tuprolog.TuProlog();        
        try {
            engine.unloadLibrary("alice.tuprolog.lib.OOLibrary");
            engine.loadLibrary("alice.tuprologx.pj.lib.PJLibraryNew");
            engine.loadLibrary("alice.tuprolog.lib.DCGLibrary");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public <G extends TxTerm<?>, S extends TxTerm<?>> Iterable<PrologSolution<G,S>>  solveAll(final G query) {            
            class SolutionProxy implements Iterable<PrologSolution<G,S>> {
                @Override
				public Iterator<PrologSolution<G,S>> iterator() {                
                    PrologSolution<G,S> first = PJProlog.this.solve(query);
                    return new SolutionIterator<G,S>(first);
                }
            };
            return new SolutionProxy();
        }

    public <G extends TxTerm<?>, S extends TxTerm<?>> PrologSolution<G,S> solve(G g) {
        alice.tuprolog.SolveInfo retValue;        
        retValue = engine.solve(g.marshal());        
        return new PrologSolution<G,S>(retValue);
    }
    
    public <G extends TxTerm<?>, S extends TxTerm<?>> PrologSolution<G,S> solveNext() throws NoSolutionException {
        alice.tuprolog.SolveInfo retValue;        
        try {
            retValue = engine.solveNext();
        }
        catch (Exception e) {
            throw new NoSolutionException();
        }
        return new PrologSolution<G,S>(retValue);
    }

    public void setTheory(TxTheory theory) throws alice.tuprolog.InvalidTheoryException {
        engine.setTheory(new alice.tuprolog.TuTheory(theory.marshal()));
    }

    public void addTheory(TxTheory theory) throws alice.tuprolog.InvalidTheoryException {
        engine.addTheory(new alice.tuprolog.TuTheory(theory.marshal()));
    }
    
    public TxTheory getTheory() throws alice.tuprolog.InvalidTheoryException {
        return TxTheory.unmarshal(engine.getTheory());
    }
        
    /**
	 * @author  ale
	 */
    class SolutionIterator<G extends TxTerm<?>, S extends TxTerm<?>> implements Iterator<PrologSolution<G,S>> {

        PrologSolution<G,S> current = null;
        PrologSolution<G,S> next = null;

        SolutionIterator(PrologSolution<G,S> first) {            
            this.current = first;
        }

        @Override
		public void remove() {
            throw new UnsupportedOperationException();
        }

        @Override
		public PrologSolution<G,S> next() {
            if (current != null) {
                hasNext();
                PrologSolution<G,S> temp = current;
                current = (next != null && next.isSuccess()) ? next : null;
                next = null;
                return temp;
            }
            else {
                throw new NoSuchElementException();
            }
        }

        @Override
		public boolean hasNext() {
            if (next == null) {
                try {
                    next = new PrologSolution<G,S>(engine.solveNext());                    
                }
                catch (alice.tuprolog.NoMoreSolutionException e) {
                    next = null;
                }
            }
            return current != null && current.isSuccess();// && next.isSuccess();            
        }
    }
    
     public alice.tuprolog.TuTerm registerJavaObject(Object o) {
        return ((alice.tuprolog.lib.OOLibrary)engine.getLibrary("alice.tuprologx.pj.lib.PJLibraryNew")).register(o);
     }
     
     public Object getJavaObject(alice.tuprolog.TuStruct t) { 
        try {
            return ((alice.tuprolog.lib.OOLibrary)engine.getLibrary("alice.tuprologx.pj.lib.PJLibraryNew")).getRegisteredObject(t);
        }
        catch (Exception e) {
            return null;
        }        
     }
     
     public void loadLibrary(alice.tuprolog.TuLibrary library) {
         try {
            engine.loadLibrary(library);
         }
         catch (Exception e) {
             throw new UnsupportedOperationException(e);
         }
     }

     public alice.tuprologx.pj.lib.PJLibraryNew getPJLibrary() {
         try {
            return (alice.tuprologx.pj.lib.PJLibraryNew)engine.getLibrary("alice.tuprologx.pj.lib.PJLibraryNew");
         }
         catch (Exception e) {
             return null;
         }
     }
}
