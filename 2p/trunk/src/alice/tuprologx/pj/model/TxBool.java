/*
 * Bool.java
 *
 * Created on March 8, 2007, 5:24 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package alice.tuprologx.pj.model;

/**
 *
 * @author maurizio
 */
public class TxBool extends TxTerm<TxBool> {
	Boolean _theBool;        
        
	// public Boolean toJava() { return _theBool; } // ED 2013-05-12
	@Override
	public <Z> Z toJava() { return uncheckedCast (_theBool); }
	
	public TxBool (Boolean b) {_theBool = b;}
        
        @Override
		public alice.tuprolog.Term marshal() {
            return _theBool.booleanValue() ? alice.tuprolog.Term.TRUE : alice.tuprolog.Term.FALSE;
        }
        
        static TxBool unmarshal(alice.tuprolog.TuStruct b) {
            if (!matches(b))
                throw new UnsupportedOperationException();
            if (b.isEqual(alice.tuprolog.Term.TRUE))
                return new TxBool(Boolean.TRUE);
            else 
                return new TxBool(Boolean.FALSE);
        }
        
        static boolean matches(alice.tuprolog.Term t) {            
            return (!(t instanceof alice.tuprolog.TuVar) && (t.isEqual(alice.tuprolog.Term.TRUE) || t.isEqual(alice.tuprolog.Term.FALSE)));
        }
        
	@Override
	public String toString() {
		return "Bool("+_theBool+")";
	}

}