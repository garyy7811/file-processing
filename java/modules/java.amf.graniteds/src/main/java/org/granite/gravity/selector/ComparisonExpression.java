/**
 *   GRANITE DATA SERVICES
 *   Copyright (C) 2006-2015 GRANITE DATA SERVICES S.A.S.
 *
 *   This file is part of the Granite Data Services Platform.
 *
 *   Granite Data Services is free software; you can redistribute it and/or
 *   modify it under the terms of the GNU Lesser General Public
 *   License as published by the Free Software Foundation; either
 *   version 2.1 of the License, or (at your option) any later version.
 *
 *   Granite Data Services is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser
 *   General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public
 *   License along with this library; if not, write to the Free Software
 *   Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301,
 *   USA, or see <http://www.gnu.org/licenses/>.
 */
package org.granite.gravity.selector;

import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

import javax.jms.JMSException;

/**
 * A filter performing a comparison of two objects
 *
 * @version $Revision: 1.2 $
 */
public abstract class ComparisonExpression extends BinaryExpression implements BooleanExpression {

    public static BooleanExpression createBetween(Expression value, Expression left, Expression right) {
        return LogicExpression.createAND(createGreaterThanEqual(value, left), createLessThanEqual(value, right));
    }

    public static BooleanExpression createNotBetween(Expression value, Expression left, Expression right) {
        return LogicExpression.createOR(createLessThan(value, left), createGreaterThan(value, right));
    }

    static final private HashSet<Character> REGEXP_CONTROL_CHARS = new HashSet<Character>();

    static {
        REGEXP_CONTROL_CHARS.add(new Character('.'));
        REGEXP_CONTROL_CHARS.add(new Character('\\'));
        REGEXP_CONTROL_CHARS.add(new Character('['));
        REGEXP_CONTROL_CHARS.add(new Character(']'));
        REGEXP_CONTROL_CHARS.add(new Character('^'));
        REGEXP_CONTROL_CHARS.add(new Character('$'));
        REGEXP_CONTROL_CHARS.add(new Character('?'));
        REGEXP_CONTROL_CHARS.add(new Character('*'));
        REGEXP_CONTROL_CHARS.add(new Character('+'));
        REGEXP_CONTROL_CHARS.add(new Character('{'));
        REGEXP_CONTROL_CHARS.add(new Character('}'));
        REGEXP_CONTROL_CHARS.add(new Character('|'));
        REGEXP_CONTROL_CHARS.add(new Character('('));
        REGEXP_CONTROL_CHARS.add(new Character(')'));
        REGEXP_CONTROL_CHARS.add(new Character(':'));
        REGEXP_CONTROL_CHARS.add(new Character('&'));
        REGEXP_CONTROL_CHARS.add(new Character('<'));
        REGEXP_CONTROL_CHARS.add(new Character('>'));
        REGEXP_CONTROL_CHARS.add(new Character('='));
        REGEXP_CONTROL_CHARS.add(new Character('!'));
    }

    static class LikeExpression extends UnaryExpression implements BooleanExpression {

        Pattern likePattern;

        /**
         * @param left
         */
        public LikeExpression(Expression right, String like, int escape) {
            super(right);

            StringBuffer regexp = new StringBuffer(like.length() * 2);
            regexp.append("\\A"); // The beginning of the input
            for (int i = 0; i < like.length(); i++) {
                char c = like.charAt(i);
                if (escape == (0xFFFF & c)) {
                    i++;
                    if (i >= like.length()) {
                        // nothing left to escape...
                        break;
                    }

                    char t = like.charAt(i);
                    regexp.append("\\x");
                    regexp.append(Integer.toHexString(0xFFFF & t));
                }
                else if (c == '%') {
                    regexp.append(".*?"); // Do a non-greedy match
                }
                else if (c == '_') {
                    regexp.append("."); // match one
                }
                else if (REGEXP_CONTROL_CHARS.contains(new Character(c))) {
                    regexp.append("\\x");
                    regexp.append(Integer.toHexString(0xFFFF & c));
                }
                else {
                    regexp.append(c);
                }
            }
            regexp.append("\\z"); // The end of the input

            likePattern = Pattern.compile(regexp.toString(), Pattern.DOTALL);
        }

        @Override
        public String getExpressionSymbol() {
            return "LIKE";
        }

        public Object evaluate(MessageEvaluationContext message) throws JMSException {

            Object rv = this.getRight().evaluate(message);

            if (rv == null) {
                return null;
            }

            if (!(rv instanceof String)) {
                return Boolean.FALSE;
                //throw new RuntimeException("LIKE can only operate on String identifiers.  LIKE attemped on: '" + rv.getClass());
            }

            return likePattern.matcher((String) rv).matches() ? Boolean.TRUE : Boolean.FALSE;
        }

        public boolean matches(MessageEvaluationContext message) throws JMSException {
            Object object = evaluate(message);
            return object!=null && object==Boolean.TRUE;
        }
    }

    public static BooleanExpression createLike(Expression left, String right, String escape) {
        if (escape != null && escape.length() != 1) {
            throw new RuntimeException("The ESCAPE string litteral is invalid.  It can only be one character.  Litteral used: " + escape);
        }
        int c = -1;
        if (escape != null) {
            c = 0xFFFF & escape.charAt(0);
        }

        return new LikeExpression(left, right, c);
    }

    public static BooleanExpression createNotLike(Expression left, String right, String escape) {
        return UnaryExpression.createNOT(createLike(left, right, escape));
    }

    public static BooleanExpression createInFilter(Expression left, List<?> elements) {

        if( !(left instanceof PropertyExpression) )
            throw new RuntimeException("Expected a property for In expression, got: "+left);
        return UnaryExpression.createInExpression((PropertyExpression)left, elements, false);

    }

    public static BooleanExpression createNotInFilter(Expression left, List<?> elements) {

        if( !(left instanceof PropertyExpression) )
            throw new RuntimeException("Expected a property for In expression, got: "+left);
        return UnaryExpression.createInExpression((PropertyExpression)left, elements, true);

    }

    public static BooleanExpression createIsNull(Expression left) {
        return doCreateEqual(left, ConstantExpression.NULL);
    }

    public static BooleanExpression createIsNotNull(Expression left) {
        return UnaryExpression.createNOT(doCreateEqual(left, ConstantExpression.NULL));
    }

    public static BooleanExpression createNotEqual(Expression left, Expression right) {
        return UnaryExpression.createNOT(createEqual(left, right));
    }

    public static BooleanExpression createEqual(Expression left, Expression right) {
        checkEqualOperand(left);
        checkEqualOperand(right);
        checkEqualOperandCompatability(left, right);
        return doCreateEqual(left, right);
    }

    private static BooleanExpression doCreateEqual(Expression left, Expression right) {
        return new ComparisonExpression(left, right) {
            @Override
            public Object evaluate(MessageEvaluationContext message) throws JMSException {
                Object lv = left.evaluate(message);
                Object rv = right.evaluate(message);

                // Iff one of the values is null
                if (lv == null ^ rv == null) {
                    return Boolean.FALSE;
                }
                if (lv == rv || (lv != null && lv.equals(rv))) {
                    return Boolean.TRUE;
                }
                if( lv instanceof Comparable<?> && rv instanceof Comparable<?> ) {
                    return compare((Comparable<?>)lv, (Comparable<?>)rv);
                }
                return Boolean.FALSE;
            }

            @Override
            protected boolean asBoolean(int answer) {
                return answer == 0;
            }

            @Override
            public String getExpressionSymbol() {
                return "=";
            }
        };
    }

    public static BooleanExpression createGreaterThan(final Expression left, final Expression right) {
        checkLessThanOperand(left);
        checkLessThanOperand(right);
        return new ComparisonExpression(left, right) {
            @Override
            protected boolean asBoolean(int answer) {
                return answer > 0;
            }

            @Override
            public String getExpressionSymbol() {
                return ">";
            }
        };
    }

    public static BooleanExpression createGreaterThanEqual(final Expression left, final Expression right) {
        checkLessThanOperand(left);
        checkLessThanOperand(right);
        return new ComparisonExpression(left, right) {
            @Override
            protected boolean asBoolean(int answer) {
                return answer >= 0;
            }

            @Override
            public String getExpressionSymbol() {
                return ">=";
            }
        };
    }

    public static BooleanExpression createLessThan(final Expression left, final Expression right) {
        checkLessThanOperand(left);
        checkLessThanOperand(right);
        return new ComparisonExpression(left, right) {
            @Override
            protected boolean asBoolean(int answer) {
                return answer < 0;
            }

            @Override
            public String getExpressionSymbol() {
                return "<";
            }

        };
    }

    public static BooleanExpression createLessThanEqual(final Expression left, final Expression right) {
        checkLessThanOperand(left);
        checkLessThanOperand(right);
        return new ComparisonExpression(left, right) {
            @Override
            protected boolean asBoolean(int answer) {
                return answer <= 0;
            }

            @Override
            public String getExpressionSymbol() {
                return "<=";
            }
        };
    }

    /**
     * Only Numeric expressions can be used in >, >=, < or <= expressions.s
     *
     * @param expr
     */
    public static void checkLessThanOperand(Expression expr ) {
        if( expr instanceof ConstantExpression ) {
            Object value = ((ConstantExpression)expr).getValue();
            if( value instanceof Number )
                return;

            // Else it's boolean or a String..
            throw new RuntimeException("Value '"+expr+"' cannot be compared.");
        }
        if( expr instanceof BooleanExpression ) {
            throw new RuntimeException("Value '"+expr+"' cannot be compared.");
        }
    }

    /**
     * Validates that the expression can be used in == or <> expression.
     * Cannot not be NULL TRUE or FALSE litterals.
     *
     * @param expr
     */
    public static void checkEqualOperand(Expression expr ) {
        if( expr instanceof ConstantExpression ) {
            Object value = ((ConstantExpression)expr).getValue();
            if( value == null )
                throw new RuntimeException("'"+expr+"' cannot be compared.");
        }
    }

    /**
     *
     * @param left
     * @param right
     */
    private static void checkEqualOperandCompatability(Expression left, Expression right) {
        if( left instanceof ConstantExpression && right instanceof ConstantExpression ) {
            if( left instanceof BooleanExpression && !(right instanceof BooleanExpression) )
                throw new RuntimeException("'"+left+"' cannot be compared with '"+right+"'");
        }
    }



    /**
     * @param left
     * @param right
     */
    public ComparisonExpression(Expression left, Expression right) {
        super(left, right);
    }

    public Object evaluate(MessageEvaluationContext message) throws JMSException {
        Comparable<?> lv = (Comparable<?>)left.evaluate(message);
        if (lv == null) {
            return null;
        }
        Comparable<?> rv = (Comparable<?>)right.evaluate(message);
        if (rv == null) {
            return null;
        }
        return compare(lv, rv);
    }

    @SuppressWarnings({ "unchecked", "rawtypes", "boxing" })
    protected Boolean compare(Comparable lv, Comparable rv) {
        Class<?> lc = lv.getClass();
        Class<?> rc = rv.getClass();
        // If the the objects are not of the same type,
        // try to convert up to allow the comparison.
        if (lc != rc) {
            if (lc == Byte.class) {
                if (rc == Short.class) {
                    return ((Number)lv).shortValue() == ((Short)rv).shortValue();
                }
                else if (rc == Integer.class) {
                    return ((Number)lv).intValue() == ((Integer)rv).intValue();
                }
                else if (rc == Long.class) {
                    return ((Number)lv).longValue() == ((Long)rv).longValue();
                }
                else if (rc == Float.class) {
                    return ((Number)lv).floatValue() == ((Float)rv).floatValue();
                }
                else if (rc == Double.class) {
                    return ((Double)lv).doubleValue() == ((Double)rv).doubleValue();
                }
                else {
                    return Boolean.FALSE;
                }
             } else if (lc == Short.class) {
                if (rc == Integer.class) {
                    lv = new Integer(((Number) lv).intValue());
                }
                else if (rc == Long.class) {
                    lv = new Long(((Number) lv).longValue());
                }
                else if (rc == Float.class) {
                    lv = new Float(((Number) lv).floatValue());
                }
                else if (rc == Double.class) {
                    lv = new Double(((Number) lv).doubleValue());
                }
                else {
                    return Boolean.FALSE;
                }
            } else if (lc == Integer.class) {
                if (rc == Long.class) {
                    lv = new Long(((Number)lv).longValue());
                }
                else if (rc == Float.class) {
                    lv = new Float(((Number)lv).floatValue());
                }
                else if (rc == Double.class) {
                    lv = new Double(((Number)lv).doubleValue());
                }
                else {
                    return Boolean.FALSE;
                }
            }
            else if (lc == Long.class) {
                if (rc == Integer.class) {
                    rv = new Long(((Number)rv).longValue());
                }
                else if (rc == Float.class) {
                    lv = new Float(((Number)lv).floatValue());
                }
                else if (rc == Double.class) {
                    lv = new Double(((Number)lv).doubleValue());
                }
                else {
                    return Boolean.FALSE;
                }
            }
            else if (lc == Float.class) {
                if (rc == Integer.class) {
                    rv = new Float(((Number)rv).floatValue());
                }
                else if (rc == Long.class) {
                    rv = new Float(((Number)rv).floatValue());
                }
                else if (rc == Double.class) {
                    lv = new Double(((Number)lv).doubleValue());
                }
                else {
                    return Boolean.FALSE;
                }
            }
            else if (lc == Double.class) {
                if (rc == Integer.class) {
                    rv = new Double(((Number)rv).doubleValue());
                }
                else if (rc == Long.class) {
                    rv = new Double(((Number)rv).doubleValue());
                }
                else if (rc == Float.class) {
                    rv = new Float(((Number)rv).doubleValue());
                }
                else {
                    return Boolean.FALSE;
                }
            }
            else
                return Boolean.FALSE;
        }

        return asBoolean(lv.compareTo(rv)) ? Boolean.TRUE : Boolean.FALSE;
    }

    protected abstract boolean asBoolean(int answer);

    public boolean matches(MessageEvaluationContext message) throws JMSException {
        Object object = evaluate(message);
        return object != null && object == Boolean.TRUE;
    }

}
