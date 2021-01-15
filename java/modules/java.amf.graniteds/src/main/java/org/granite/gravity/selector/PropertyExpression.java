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

import javax.jms.JMSException;

import flex.messaging.messages.Message;

/**
 * Represents a property  expression
 *
 * @version $Revision: 1.5 $
 */
public class PropertyExpression implements Expression {

    interface SubExpression {
        public Object evaluate( Message message );
    }

    private final String name;

    public PropertyExpression(String name) {
        this.name = name;
    }

    public Object evaluate(MessageEvaluationContext message) throws JMSException {
        return message.getMessage().getHeader(name);
    }

    public Object evaluate(Message message) {
        return message.getHeader(name);
    }

    public String getName() {
        return name;
    }


    /**
     * @see Object#toString()
     */
    @Override
    public String toString() {
        return name;
    }

    /**
     * @see Object#hashCode()
     */
    @Override
    public int hashCode() {
        return name.hashCode();
    }

    /**
     * @see Object#equals(Object)
     */
    @Override
    public boolean equals(Object o) {

        if (o == null || !this.getClass().equals(o.getClass())) {
            return false;
        }
        return name.equals(((PropertyExpression) o).name);

    }

}
