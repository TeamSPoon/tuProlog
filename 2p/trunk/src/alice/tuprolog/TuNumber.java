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
package alice.tuprolog;

import java.util.*;

import nu.xom.xslt.XSLException;

/**
 *
 * Number abstract class represents numbers prolog data type
 *
 * @see TuInt
 * @see TuLong
 * @see TuFloat
 * @see TuDouble
 *
 * Reviewed by Paolo Contessi: implements Comparable<Number>
 */
public abstract class TuNumber extends TuTerm implements Comparable<TuNumber> {
    private static final long serialVersionUID = 1L;

    /**
     *  Returns the value of the number as int
     */
    public abstract int intValue();

    /**
     *  Returns the value of the number as float
     */
    public abstract float floatValue();

    /**
     *  Returns the value of the number as long
     */
    public abstract long longValue();

    /**
     *  Returns the value of the number as double
     */
    public abstract double doubleValue();

    /** is this term a prolog integer term? */
    public abstract boolean isInteger();

    /** is this term a prolog real term? */
    public abstract boolean isReal();

    //

    /** is an int Integer number? 
     * Was <tt>instanceof Int</tt> instead. */
    public abstract boolean isTypeInt();

    /** is an int Integer number?
     * Was <tt>instanceof Int</tt> instead. */
    public abstract boolean isInt();

    /** is a float Real number? 
     * Was <tt>instanceof alice.tuprolog.Float</tt> instead. */
    public abstract boolean isTypeFloat();

    /** is a float Real number?
     * Was <tt>instanceof alice.tuprolog.Float</tt> instead. */
    public abstract boolean isFloat();

    /** is a double Real number? 
     * Was <tt>instanceof alice.tuprolog.Double</tt> instead.*/
    @Deprecated
    public abstract boolean isTypeDouble();

    /** is a double Real number?
     * Was <tt>instanceof alice.tuprolog.Double</tt> instead. */
    public abstract boolean isDouble();

    /** is a long Integer number? 
     * Was <tt>instanceof alice.tuprolog.Long</tt> instead. */
    public abstract boolean isTypeLong();

    /** is a long Integer number?
     * Was <tt>instanceof alice.tuprolog.Long</tt> instead. */
    public abstract boolean isLong();

    public static TuNumber createNumber(String s) {
        Term t = Term.createTerm(s);
        if (t .isNumber())
            return (TuNumber) t;
        throw new InvalidTermException("Term " + t + " is not a number.");
    }

    /**
     * Gets the actual term referred by this Term.
     */
    @Override
    public Term getTerm() {
        return this;
    }

    // checking type and properties of the Term

    /** is this term a prolog numeric term? */
    @Override
    final public boolean isNumber() {
        return true;
    }

    /** is this term a struct  */
    @Override
    final public boolean isCallable() {
        return false;
    }

    /** is this term a variable  */
    @Override
    final public boolean isVar() {
        return false;
    }

    @Override
    final public boolean isEmptyList() {
        return false;
    }

    //

    /** is this term a constant prolog term? */
    @Override
    final public boolean isAtomic() {
        return true;
    }

    /** is this term a prolog compound term? */
    @Override
    final public boolean isCompound() {
        return false;
    }

    /** is this term a prolog (alphanumeric) atom? */
    @Override
    final public boolean isAtomSymbol() {
        return false;
    }

    /** is this term a prolog list? */
    @Override
    final public boolean isList() {
        return false;
    }

    /** is this term a ground term? */
    @Override
    final public boolean isGround() {
        return true;
    }

    //

    /**
     * gets a copy of this term.
     */
    public Term copy(int idExecCtx) {
        return this;
    }

    /**
     * gets a copy (with renamed variables) of the term.
     * <p>
     * the list argument passed contains the list of variables to be renamed
     * (if empty list then no renaming)
     */
    @Override
    public Term copy(int idExecCtx, AbstractMap<TuVar, TuVar> vMap) {
        return this;
    }

    /**
     * gets a copy of the term.
     */
    @Override
    public Term copy(AbstractMap<TuVar, TuVar> vMap, AbstractMap<Term, TuVar> substMap) {
        return this;
    }

    @Override
    public Term copyAndRetainFreeVar(AbstractMap<TuVar, TuVar> vMap, int idExecCtx) {
        // TODO Auto-generated method stub
        return this;
    }

    @Override
    public long resolveTerm(long count) {
        return count;
    }

    /**
     *
     */
    @Override
    public void free() {
    }

    void restoreVariables() {
    }

    /*Castagna 06/2011*/
    @Override
    public void accept(TuTermVisitor tv) {
        tv.visit(this);
    }
    /**/
}