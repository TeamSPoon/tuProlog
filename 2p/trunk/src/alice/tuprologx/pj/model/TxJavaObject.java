/*
 * JavaObject.java
 *
 * Created on July 19, 2007, 11:23 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package alice.tuprologx.pj.model;

import alice.tuprologx.pj.engine.PJ;

/**
 *
 * @author maurizio
 */
public class TxJavaObject<O> extends TxTerm<TxJavaObject<O>> {
    
    O _theObject;
    
    /** Creates a new instance of JavaObject */
    public TxJavaObject(O o) {
        _theObject = o;
    }

    @Override
	public alice.tuprolog.Term marshal() {
        return PJ.registerJavaObject(_theObject);
    }

    @Override
	public <Z> Z toJava() {
        //return (Z)_theObject;
        return uncheckedCast(_theObject);
    }
    
    static boolean matches(alice.tuprolog.Term t) {        
        return (t instanceof alice.tuprolog.TuStruct && PJ.getRegisteredJavaObject((alice.tuprolog.TuStruct)t) != null);        
    }
    
    static <Z> TxJavaObject<Z> unmarshalObject(alice.tuprolog.TuStruct s) {
        if (matches(s)) {           
            //return new JavaObject<Z>((Z)(PJ.getRegisteredJavaObject(s)));
        	Z auxJavaObject = uncheckedCast(PJ.getRegisteredJavaObject(s));
            return new TxJavaObject<Z>( auxJavaObject );
        }
        else
            throw new UnsupportedOperationException();
    }

    @Override
	public String toString() {        
        return "JavaObject{"+_theObject+"}";
    }
    
    
    
}
