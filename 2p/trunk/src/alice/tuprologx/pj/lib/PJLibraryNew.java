/*
 * tuProlog - Copyright (C) 2001-2002  aliCE team at deis.unibo.it
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
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package alice.tuprologx.pj.lib;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Iterator;

import alice.tuprolog.TuInt;
import alice.tuprolog.TuNumber;
import alice.tuprolog.TuStruct;
import alice.tuprolog.Term;
import alice.tuprolog.TuFactory;
import alice.tuprolog.TuVar;

import alice.tuprologx.pj.annotations.*;

import alice.tuprolog.lib.*;
import static alice.tuprolog.TuPrologError.*;
import static alice.tuprolog.TuFactory.*;


/**
 *
 * This class represents a tuProlog library enabling the interaction
 * with the Java environment from tuProlog.
 *
 * Works only with JDK 1.2 (because of setAccessible method)
 *
 * The most specific method algorithm used to find constructors / methods
 * has been inspired by the article
 *     "What Is Interactive Scripting?", by Michael Travers
 *     Dr. Dobb's -- Software Tools for the Professional Programmer
 *     January 2000
 *     CMP  Media Inc., a United News and Media Company
 *
 * Library/Theory Dependency:  BasicLibrary
 *
 *
 *
 */
@SuppressWarnings("serial")
public class PJLibraryNew extends OOLibrary {
	
	/**
	 * library theory
	 */
	@Override
	public String getTheory() {
		return
		//
		// operators defined by the JavaLibrary theory
		//
		":- op(800,xfx,'<-').\n" +
        ":- op(800,xfx,':=').\n" +
        ":- op(850,fy,'returns').\n" +
        ":- op(200,xfx,'as').\n" +
		":- op(600,xfx,'.'). \n" +
		//
		// flags defined by the JavaLibrary theory
		//
		//":- flag(java_object_backtrackable,[true,false],false,true).\n" +
		//
		"java_object_bt(ClassName,Args,Id):- java_object(ClassName,Args,Id).\n" +
		"java_object_bt(ClassName,Args,Id):- destroy_object(Id).\n" +
        "new_object(ClassName,Args,Id):- prolog_class(ClassName), java_object_prolog(ClassName, Args, Id).\n" +
        "new_object(ClassName,Args,Id):- !, java_object_std(ClassName, Args, Id).\n" +
		"Obj <- What :- java_call1(Obj,What,Res), Res \\== false.\n" +
		"Obj <- What returns Res :- java_call1(Obj,What,Res).\n" +
        "java_call1('.'(C,F), set(X), Res):-lookup_field(C, F, Field), java_access(C, F, Field, set(X), Res).\n" +
        "java_call1('.'(C,F), get(X), Res):-lookup_field(C, F, Field), java_access(C, F, Field, get(X), Res).\n" +
        "java_call1(Obj, What, Res):-java_call2(Obj, What, Res).\n" +
        "java_call2(Obj, What, Res):-lookup_method(Obj, What, Meth), not prolog_method(Meth), !, java_method_call(Obj, Meth, What, Res, false, false).\n" +
        "java_call2(Obj, What, Res):-unmarshal_method(What, M2), lookup_method(Obj, M2, Meth), prolog_call_rest(Obj, Meth, M2, Res).\n" +
        "prolog_call_rest(Obj, Meth, What, Res):-is_iterable(Meth), !, java_method_call(Obj, Meth, What, R2, true, true), R2 <- iterator returns I, next(I, E), marshal(E, Res).\n" +
        "prolog_call_rest(Obj, Meth, What, Res):-!, java_method_call(Obj, Meth, What, R2, true, false), marshal(R2, Res).\n" +
        "java_access(C, F, Field, get(X), Res):-prolog_field(Field), !, java_get(C, F, Y), marshal(Y, X).\n" +
        "java_access(C, F, Field, set(X), Res):-prolog_field(Field), !, unmarshal(X, Y), java_set(C, F, Y).\n" +
        "java_access(C, F, Field, get(X), Res):-java_get(C, F, X).\n" +
        "java_access(C, F, Field, set(X), Res):-java_set(C, F, X).\n" +
		"java_array_set(Array,Index,Object):-           class('java.lang.reflect.Array') <- set(Array as 'java.lang.Object',Index,Object as 'java.lang.Object'),!.\n" +
		"java_array_set(Array,Index,Object):-			java_array_set_primitive(Array,Index,Object).\n"+
		"java_array_get(Array,Index,Object):-           class('java.lang.reflect.Array') <- get(Array as 'java.lang.Object',Index) returns Object,!.\n" +
		"java_array_get(Array,Index,Object):-       java_array_get_primitive(Array,Index,Object).\n"+
        "add_rule(Clause):-this(Obj), pj_assert(Obj, Clause), assert(Clause).\n"+
        "remove_rule(Clause):-this(Obj), pj_retract(Obj, Clause), retract(Clause).\n"+
        "remove_rules(Clause):-this(Obj), pj_retract_all(Obj, Clause), retractAll(Clause).\n"+
        "next(Iterable, Element) :-Iterable <- hasNext, next2(Iterable, Element).\n" +
        "next2(Iterable, Element) :- Iterable <- next returns Element.\n" +
        "next2(Iterable, Element) :- Iterable <- hasNext, next(Iterable, Element).\n" +

		"java_array_length(Array,Length):-              class('java.lang.reflect.Array') <- getLength(Array as 'java.lang.Object') returns Length.\n" +
		"java_object_string(Object,String):-    Object <- toString returns String.    \n";
	}
	
	//----------------------------------------------------------------------------

    public boolean prolog_class_1(Term classname) {
        if (!classname.isAtomSymbol())
            return false;
        Class<?> clazz = null;
        try {
             clazz = Class.forName(((TuStruct)classname.dref()).fname());
        }
        catch (Throwable ex) {
            return false;
        }
        if (clazz == null)
            return false;
        else {
            return clazz.isAnnotationPresent(PrologClass.class);
        }
    }

    public boolean prolog_method_1(Term method) {
        if (!method.isAtomSymbol())
            return false;
        Object o = null;
        try {
             o = getRegisteredDynamicObject((TuStruct)method.dref());
        }
        catch (InvalidObjectIdException ex) {
            return false;
        }
        if (o == null || !(o instanceof Method))
            return false;
        else {
            Method m = (Method)o;
            try {
            return m.getDeclaringClass().getSuperclass().getDeclaredMethod(m.getName(),m.getParameterTypes()).isAnnotationPresent(PrologMethod.class);
            }
            catch (Exception e) {
                return false;
            }
        }
    }

    public boolean prolog_field_1(Term method) {
        if (!method.isAtomSymbol())
            return false;
        Object o = null;
        try {
             o = getRegisteredDynamicObject((TuStruct)method.dref());
        }
        catch (InvalidObjectIdException ex) {
            return false;
        }
        if (o == null || !(o instanceof Field))
            return false;
        else {
            Field f = (Field)o;
            boolean b = f.isAnnotationPresent(PrologField.class);
            return b;
        }
    }

    public boolean java_method_1(Term method) {
        if (!method.dref().isAtomSymbol())
            return false;
        Object o = null;
        try {
             o = getRegisteredDynamicObject((TuStruct)method.dref());
        }
        catch (InvalidObjectIdException ex) {
            return false;
        }
        if (o == null || !(o instanceof Method))
            return false;
        else {
            return true;
        }
    }

    public boolean is_iterable_1(Term method) {
        if (!method.dref().isAtomSymbol())
            return false;
        Object o = null;
        try {
             o = getRegisteredDynamicObject((TuStruct)method.dref());
        }
        catch (InvalidObjectIdException ex) {
            return false;
        }
        if (o == null || !(o instanceof Method))
            return false;
        else {
            Method m = (Method)o;
            return m.getReturnType() == Iterable.class;
        }
    }

    public boolean java_field_1(Term method) {
        if (!method.dref().isAtomSymbol())
            return false;
        Object o = null;
        try {
             o = getRegisteredDynamicObject((TuStruct)method.dref());
        }
        catch (InvalidObjectIdException ex) {
            return false;
        }
        if (o == null || !(o instanceof Field))
            return false;
        else {
            return true;
        }
    }
    
    public boolean marshal_2(Term term, Term marshalledTerm) {
        if (!term.isAtomSymbol())
            return false;
        Object o = null;
        try {
             o = getRegisteredDynamicObject((TuStruct)term.dref());
        }
        catch (InvalidObjectIdException ex) {
            return false;
        }
        if (o == null || !(o instanceof alice.tuprologx.pj.model.TxTerm<?>))
            return unify(marshalledTerm, term);
        else {
            alice.tuprologx.pj.model.TxTerm<?> t = (alice.tuprologx.pj.model.TxTerm<?>)o;
            boolean res =  unify(marshalledTerm, t.marshal());
            return res;
        }            
    }
    
    public boolean unmarshal_2(Term term, Term unmarshalledTerm) {
        Object o = alice.tuprologx.pj.model.TxTerm.unmarshal(term.dref());
        return unify(unmarshalledTerm, registerDynamic(o));
    }

    public boolean unmarshal_method_2(Term term, Term unmarshalledTerm) {
        if (! (term.dref() .isTuStruct()) )
            return false;
        TuStruct methodInfo = (TuStruct)term.dref();
        Term[] terms = new Term[methodInfo.getPlArity()];
        for (int i = 0 ; i < methodInfo.getPlArity() ; i ++) {
            terms[i] = registerDynamic(alice.tuprologx.pj.model.TxTerm.unmarshal(methodInfo.getDerefArg(i).dref()));
        }
        return unify(unmarshalledTerm, createTuStructA(methodInfo.fname(), terms));
    }

    

    public boolean lookup_field_3(Term receiver, Term name, Term result) {
        if (!receiver.isAtomSymbol())
            return false;
        Object o = null;
        try {
             o = getRegisteredDynamicObject((TuStruct)receiver.dref());
        }
        catch (InvalidObjectIdException ex) {
            return false;
        }
        if (o == null)
            return false;
        else {
            String fname = ((TuStruct)name.dref()).fname();
            try {
                return bindDynamicObject(result, o.getClass().getField(fname));
            }
            catch (Exception e) {
                return false;
            }
        }
    }

    public boolean lookup_method_3(Term receiver, Term method, Term result) {
        if (!receiver.isAtomSymbol())
            return false;
        Object o = null;
        try {
             o = getRegisteredDynamicObject((TuStruct)receiver.dref());
        }
        catch (InvalidObjectIdException ex) {
            return false;
        }
        if (o == null)
            return false;
        else {
            TuStruct methodInfo = (TuStruct)method.dref();
            String mname = methodInfo.fname();
            Signature sig = parseArg(methodInfo);
            Method m = null;
            try {
                m = new MethodFinder(o.getClass()).findMethod(mname, sig.getTypes());
            }
            catch (Exception e2) {
                return false;
            }
            return bindDynamicObject(result, m);
        }
    }

    private Object[] getArrayFromList(TuStruct list) {
		Object args[] = new Object[list.listSize()];
		Iterator<? extends Term> it = list.listIteratorProlog();
		int count = 0;
		while (it.hasNext()) {
			args[count++] = it.next();
		}
		return args;
	}


    public boolean java_object_std_3(Term className, Term args, Term id) {
        if (!className.isAtomSymbol() && !args.isPlList())
            return false;
        String clazz = ((TuStruct)className.dref()).fname();
        Signature sig = parseArg(getArrayFromList((TuStruct)args.dref()));
        Constructor<?> c = null;
        Object o = null;
        try {
            c = new MethodFinder(Class.forName(clazz)).findConstructor(sig.getTypes());
            o = c.newInstance(sig.values);
        }
        catch (Exception e2) {
            return false;
        }
        return bindDynamicObject(id, o);
    }

    public boolean java_object_prolog_3(Term className, Term args, Term id) {
        if (!className.isAtomSymbol() && !args.isPlList())
            return false;
        Signature sig = parseArg(getArrayFromList((TuStruct)args.dref()));
        assert sig.types.length == 0;
        Class<?> clazz = null;
        Object o = null;
        try {
            clazz = Class.forName(((TuStruct)className.dref()).fname());
            o = alice.tuprologx.pj.engine.PJ.newInstance(clazz);
        }
        catch (Exception e2) {
            return false;
        }
        return bindDynamicObject(id, o);
    }

    public boolean java_method_call_6(Term objId, Term method, Term method_info, Term idResult, Term isProlog, Term isReentrant) {
        if (!method.isAtomSymbol())
            return false;
        Object o = null;
        try {
             o = getRegisteredDynamicObject((TuStruct)method.dref());
        }
        catch (InvalidObjectIdException ex) {
            return false;
        }
        if (o == null || !(o instanceof Method)) {
            getEngine().warn("Method not found: " + method);
            return false;
        }
        else {
            Method m = (Method)o;
            Object res = null;
            Object receiver = null;
            try {
                receiver = getRegisteredDynamicObject((TuStruct)objId.dref());
            }
            catch (Exception e) {}
            try {
                Object[] args = parseArg((TuStruct)method_info.dref()).getValues();
                if (isProlog.isAtomSymbol() && ((TuStruct)isProlog).fname().equals("true")) {
                    boolean reentrant = isProlog.isAtomSymbol() && ((TuStruct)isReentrant).fname().equals("true");
                    res = alice.tuprologx.pj.engine.PJ.call(receiver, m, args, reentrant);
                }
                else {
                    res = m.invoke(receiver, args);
                }
            } catch (Throwable ex) {
                getEngine().warn("Method invocation failed: " + method);
                ex.printStackTrace();
                return false;
            }
            return parseResult(idResult, res);
        }
    }
	
	/*
	 * set the field value of an object
	 */
	public boolean java_set_3(Term objId, Term fieldTerm, Term what) {
		//System.out.println("SET "+objId+" "+fieldTerm+" "+what);
		what = what.dref();
		if (!fieldTerm.isAtomSymbol() || what .isVar())
			return false;
        fieldTerm = fieldTerm.dref();
        objId = objId.dref();
		String fieldName = ((TuStruct) fieldTerm).fname();
		Object obj = null;
		try {
			Class<?> cl = null;
			if (objId.isCompound() &&
					((TuStruct) objId).getPlArity() == 1 && ((TuStruct) objId).fname().equals("class")) {
				String clName = alice.util.Tools.removeApices(((TuStruct) objId).getPlainArg(0).toString());
				try {
					cl = Class.forName(clName);
				} catch (ClassNotFoundException ex) {
					getEngine().warn("Java class not found: " + clName);
					return false;
				} catch (Exception ex) {
					getEngine().warn("Static field " + fieldName + " not found in class " + alice.util.Tools.removeApices(((TuStruct) objId).getPlainArg(0).toString()));
					return false;
				}
			} else {				
				obj = getRegisteredDynamicObject((TuStruct)objId.dref());
				if (obj != null) {
					cl = obj.getClass();
				} else {
					return false;
				}
			}
			
			// first check for primitive data field
			Field field = cl.getField(fieldName);
			if (what .isNumber()) {
				TuNumber wn = (TuNumber) what;
				if (wn .isInt()) {
					field.setInt(obj, wn.intValue());
				} else if (wn instanceof alice.tuprolog.TuDouble) {
					field.setDouble(obj, wn.doubleValue());
				} else if (wn instanceof alice.tuprolog.TuLong) {
					field.setLong(obj, wn.longValue());
				} else if (wn instanceof alice.tuprolog.TuFloat) {
					field.setFloat(obj, wn.floatValue());
				} else {
					return false;
				}
			} 
            else {                    
                    Object obj2 = getRegisteredDynamicObject((TuStruct)what.dref());
                    if (obj2 != null) {
                        field.set(obj, obj2);
                    } else {
                        // consider value as a simple string
                        field.set(obj, what.toString());
                    }
                }
			
			return true;
		} catch (NoSuchFieldException ex) {
			getEngine().warn("Field " + fieldName + " not found in class " + objId);
			return false;
		} catch (Exception ex) {
			ex.printStackTrace();
			return false;
		}
	}

    public boolean pj_assert_2(Term obj, Term clause) {
        if (!obj.isAtomSymbol())
            return false;
        Object o = null;
        try {
             o = getRegisteredDynamicObject((TuStruct)obj.dref());
        }
        catch (InvalidObjectIdException ex) {
            return false;
        }
        if (o == null || !(o instanceof alice.tuprologx.pj.engine.PrologObject))
            return false;
        else {
            alice.tuprologx.pj.engine.PJ.assertClause((alice.tuprologx.pj.engine.PrologObject)o, clause);
            return true;
        }
    }

    public boolean pj_retract_2(Term obj, Term clause) {
        if (!obj.isAtomSymbol())
            return false;
        Object o = null;
        try {
             o = getRegisteredDynamicObject((TuStruct)obj.dref());
        }
        catch (InvalidObjectIdException ex) {
            return false;
        }
        if (o == null || !(o instanceof alice.tuprologx.pj.engine.PrologObject))
            return false;
        else {
            alice.tuprologx.pj.engine.PJ.retractClause((alice.tuprologx.pj.engine.PrologObject)o, clause);
            return true;
        }
    }

    public boolean pj_retract_all_2(Term obj, Term clause) {
        if (!obj.isAtomSymbol())
            return false;
        Object o = null;
        try {
             o = getRegisteredDynamicObject((TuStruct)obj.dref());
        }
        catch (InvalidObjectIdException ex) {
            return false;
        }
        if (o == null || !(o instanceof alice.tuprologx.pj.engine.PrologObject))
            return false;
        else {
            alice.tuprologx.pj.engine.PJ.retractAllClauses((alice.tuprologx.pj.engine.PrologObject)o, clause);
            return true;
        }
    }
	
	/*
	 * get the value of the field
	 */
	public boolean java_get_3(Term objId, Term fieldTerm, Term what) {
		//System.out.println("GET "+objId+" "+fieldTerm+" "+what);
		if (!fieldTerm.isAtomSymbol()) {
			return false;
		}
        fieldTerm = fieldTerm.dref();
        objId = objId.dref();

		String fieldName = ((TuStruct) fieldTerm).fname();
		Object obj = null;
		try {
			Class<?> cl = null;
			if (objId.isCompound() &&
					((TuStruct) objId).getPlArity() == 1 && ((TuStruct) objId).fname().equals("class")) {
				String clName = alice.util.Tools.removeApices(((TuStruct) objId).getPlainArg(0).toString());
				try {
					cl = Class.forName(clName);
				} catch (ClassNotFoundException ex) {
					getEngine().warn("Java class not found: " + clName);
					return false;
				} catch (Exception ex) {
					getEngine().warn("Static field " + fieldName + " not found in class " + alice.util.Tools.removeApices(((TuStruct) objId).getPlainArg(0).toString()));
					return false;
				}
			} else {
				obj = getRegisteredDynamicObject((TuStruct)objId.dref());
				if (obj == null) {
					return false;
				}
				cl = obj.getClass();
			}
			
			Field field = cl.getField(fieldName);
			Class<?> fc = field.getType();
			// work only with JDK 1.2
			field.setAccessible(true);
			
			// first check for primitive types
			if (fc.equals(Integer.TYPE) || fc.equals(Byte.TYPE)) {
				int value = field.getInt(obj);
				return unify(what, createTuInt(value));
			} else if (fc.equals(java.lang.Long.TYPE)) {
				long value = field.getLong(obj);
				return unify(what, createTuLong(value));
			} else if (fc.equals(java.lang.Float.TYPE)) {
				float value = field.getFloat(obj);
				return unify(what, createTuFloat(value));
			} else if (fc.equals(java.lang.Double.TYPE)) {
				double value = field.getDouble(obj);
				return unify(what, createTuDouble(value));
			} else {
				// the field value is an object
				Object res = field.get(obj);
				return bindDynamicObject(what, res);
			}
			//} catch (ClassNotFoundException ex){
			//    getEngine().warn("object of unknown class "+objId);
			//ex.printStackTrace();
			//    return false;
		} catch (NoSuchFieldException ex) {
			getEngine().warn("Field " + fieldName + " not found in class " + objId);
			return false;
		} catch (Exception ex) {
			getEngine().warn("Generic error in accessing the field");
			//ex.printStackTrace();
			return false;
		}
	}	
	
	/**
	 * creation of method signature from prolog data
	 */
	private Signature parseArg(TuStruct method) {
		Object[] values = new Object[method.getPlArity()];
		Class<?>[] types = new Class[method.getPlArity()];
		for (int i = 0; i < method.getPlArity(); i++) {
			if (!parse_arg(values, types, i, method.getDerefArg(i)))
				return null;
		}
		return new Signature(values, types);
	}

    private Signature parseArg(Object[] objs) {
		Object[] values = new Object[objs.length];
		Class<?>[] types = new Class[objs.length];
		for (int i = 0; i < objs.length; i++) {
			if (!parse_arg(values, types, i, (Term) objs[i]))
				return null;
		}
		return new Signature(values, types);
	}
	
	private boolean parse_arg(Object[] values, Class<?>[] types, int i, Term term) {
		try {
			if (term == null) {
				values[i] = null;
				types[i] = null;
			} else if (term.isAtomSymbol()) {
				String name = alice.util.Tools.removeApices(term.toString());
				if (name.equals("true")){
					values[i]=Boolean.TRUE;
					types[i] = Boolean.TYPE;
				} else if (name.equals("false")){
					values[i]=Boolean.FALSE;
					types[i] = Boolean.TYPE;
				} else {
					Object obj = getRegisteredDynamicObject((TuStruct)term.dref());
					if (obj == null) {
						values[i] = name;
					} else {
						values[i] = obj;
					}
					types[i] = values[i].getClass();
				}
			} else if (term .isNumber()) {
				TuNumber t = (TuNumber) term;
				if (t .isInt()) {
					values[i] = new java.lang.Integer(t.intValue());
					types[i] = java.lang.Integer.TYPE;
				} else if (t instanceof alice.tuprolog.TuDouble) {
					values[i] = new java.lang.Double(t.doubleValue());
					types[i] = java.lang.Double.TYPE;
				} else if (t instanceof alice.tuprolog.TuLong) {
					values[i] = new java.lang.Long(t.longValue());
					types[i] = java.lang.Long.TYPE;
				} else if (t instanceof alice.tuprolog.TuFloat) {
					values[i] = new java.lang.Float(t.floatValue());
					types[i] = java.lang.Float.TYPE;
				}
			} else if (term .isTuStruct()) {
				// argument descriptors
				TuStruct tc = (TuStruct) term;
				if (tc.fname().equals("as")) {
					return parse_as(values, types, i, tc.getDerefArg(0), tc.getDerefArg(1));
				} else {
					Object obj = getRegisteredDynamicObject((TuStruct)tc.dref());
					if (obj == null) {
						values[i] = alice.util.Tools.removeApices(tc.toString());
					} else {
						values[i] = obj;
					}
					types[i] = values[i].getClass();
				}
			} else if (term .isVar() && !((TuVar) term).isBound()) {
				values[i] = null;
				types[i] = Object.class;
			} else {
				return false;
			}
		} catch (Exception ex) {
			return false;
		}
		return true;
	}
	
	/**
	 *  parses return value
	 *  of a method invokation
	 */
	private boolean parseResult(Term id, Object obj) {
		if (obj == null) {
			//return unify(id,Term.TRUE);
			return unify(id, createTuVar());
		}
		try {
			if (Boolean.class.isInstance(obj)) {
				if (((Boolean) obj).booleanValue()) {
					return unify(id, Term.TRUE);
				} else {
					return unify(id, Term.FALSE);
				}
			} else if (Byte.class.isInstance(obj)) {
				return unify(id, createTuInt(((Byte) obj).intValue()));
			} else if (Short.class.isInstance(obj)) {
				return unify(id, createTuInt(((Short) obj).intValue()));
			} else if (Integer.class.isInstance(obj)) {
				return unify(id, createTuInt(((Integer) obj).intValue()));
			} else if (java.lang.Long.class.isInstance(obj)) {
				return unify(id, createTuLong(((java.lang.Long) obj).longValue()));
			} else if (java.lang.Float.class.isInstance(obj)) {
				return unify(id, createTuFloat(((java.lang.Float) obj).floatValue()));
			} else if (java.lang.Double.class.isInstance(obj)) {
				return unify(id, createTuDouble(((java.lang.Double) obj).doubleValue()));
			} else if (String.class.isInstance(obj)) {
				return unify(id, createTuAtom((String) obj));
			} else if (Character.class.isInstance(obj)) {
				return unify(id, createTuAtom(((Character) obj).toString()));
			} else {
				return bindDynamicObject(id, obj);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			return false;
		}
	}

    private boolean parse_as(Object[] values, Class<?>[] types, int i, Term castWhat, Term castTo) {
		try {
			if (!(castWhat .isNumber())) {
				String castTo_name = alice.util.Tools.removeApices(((TuStruct) castTo).fname());
				String castWhat_name = alice.util.Tools.removeApices(castWhat.dref().toString());
				//System.out.println(castWhat_name+" "+castTo_name);
				if (castTo_name.equals("java.lang.String") &&
						castWhat_name.equals("true")){
					values[i]="true";
					types[i]=String.class;
					return true;
				} else if (castTo_name.equals("java.lang.String") &&
						castWhat_name.equals("false")){
					values[i]="false";
					types[i]=String.class;
					return true;
				} else if (castTo_name.endsWith("[]")) {
					if (castTo_name.equals("boolean[]")) {
						castTo_name = "[Z";
					} else if (castTo_name.equals("byte[]")) {
						castTo_name = "[B";
					} else if (castTo_name.equals("short[]")) {
						castTo_name = "[S";
					} else if (castTo_name.equals("char[]")) {
						castTo_name = "[C";
					} else if (castTo_name.equals("int[]")) {
						castTo_name = "[I";
					} else if (castTo_name.equals("long[]")) {
						castTo_name = "[L";
					} else if (castTo_name.equals("float[]")) {
						castTo_name = "[F";
					} else if (castTo_name.equals("double[]")) {
						castTo_name = "[D";
					} else {
						castTo_name = "[L" + castTo_name.substring(0, castTo_name.length() - 2) + ";";
					}
				}
				if (!castWhat_name.equals("null")) {
					Object obj_to_cast = getRegisteredDynamicObject((TuStruct)castWhat.dref());
					if (obj_to_cast == null) {
						if (castTo_name.equals("boolean")) {
							if (castWhat_name.equals("true")) {
								values[i] = new Boolean(true);
							} else if (castWhat_name.equals("false")) {
								values[i] = new Boolean(false);
							} else {
								return false;
							}
							types[i] = Boolean.TYPE;
						} else {
							// conversion to array
							return false;
						}
					} else {
						values[i] = obj_to_cast;
						try {
							types[i] = (Class.forName(castTo_name));
						} catch (ClassNotFoundException ex) {
							getEngine().warn("Java class not found: " + castTo_name);
							return false;
						}
					}
				} else {
					values[i] = null;
					if (castTo_name.equals("byte")) {
						types[i] = Byte.TYPE;
					} else if (castTo_name.equals("short")) {
						types[i] = Short.TYPE;
					} else if (castTo_name.equals("char")) {
						types[i] = Character.TYPE;
					} else if (castTo_name.equals("int")) {
						types[i] = java.lang.Integer.TYPE;
					} else if (castTo_name.equals("long")) {
						types[i] = java.lang.Long.TYPE;
					} else if (castTo_name.equals("float")) {
						types[i] = java.lang.Float.TYPE;
					} else if (castTo_name.equals("double")) {
						types[i] = java.lang.Double.TYPE;
					} else if (castTo_name.equals("boolean")) {
						types[i] = java.lang.Boolean.TYPE;
					} else {
						try {
							types[i] = (Class.forName(castTo_name));
						} catch (ClassNotFoundException ex) {
							getEngine().warn("Java class not found: " + castTo_name);
							return false;
						}
					}
				}
			} else {
				TuNumber num = (TuNumber) castWhat;
				String castTo_name = ((TuStruct) castTo).fname();
				if (castTo_name.equals("byte")) {
					values[i] = new Byte((byte) num.intValue());
					types[i] = Byte.TYPE;
				} else if (castTo_name.equals("short")) {
					values[i] = new Short((short) num.intValue());
					types[i] = Short.TYPE;
				} else if (castTo_name.equals("int")) {
					values[i] = new Integer(num.intValue());
					types[i] = Integer.TYPE;
				} else if (castTo_name.equals("long")) {
					values[i] = new java.lang.Long(num.longValue());
					types[i] = java.lang.Long.TYPE;
				} else if (castTo_name.equals("float")) {
					values[i] = new java.lang.Float(num.floatValue());
					types[i] = java.lang.Float.TYPE;
				} else if (castTo_name.equals("double")) {
					values[i] = new java.lang.Double(num.doubleValue());
					types[i] = java.lang.Double.TYPE;
				} else {
					return false;
				}
			}
		} catch (Exception ex) {
			getEngine().warn("Casting " + castWhat + " to " + castTo + " failed");
			return false;
		}
		return true;
	}
}

