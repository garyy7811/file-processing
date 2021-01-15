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
package org.granite.tide.data;

import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Transaction;

import org.granite.tide.TideTransactionManager;

/**
 * Responsible for attaching a entity with the entity mangager
 * @author William DRAI
 *
 */
public class JDOPersistenceManager extends AbstractTidePersistenceManager implements TideTransactionPersistenceManager {
	
	protected PersistenceManager pm;

	
	public JDOPersistenceManager() {
		super(new JDOTransactionManager());
	}

	public JDOPersistenceManager(PersistenceManagerFactory pmf) {
		this(pmf, null);
	}
	
	public JDOPersistenceManager(PersistenceManagerFactory pmf, TideTransactionManager tm) {
		super(tm != null ? tm : new JDOTransactionManager());
		pm = pmf.getPersistenceManager();
	}
	
	public Object getCurrentTransaction() {
        Transaction t = pm.currentTransaction();
        t.begin();
        return t;
	}
	
	@Override
	public void close() {
		if (pm != null && !pm.isClosed())
			pm.close();
	}
	
	
    /**
     * Finds the entity with the JDO persistence manager.
     * @return the entity
     */
	@Override
	public Object fetchEntity(Object entity, String[] fetch) {
		Object id = pm.getObjectId(entity);
		if (id == null)
			return null;
		
		return pm.getObjectById(id);
	}

	
	public static class JDOTransactionManager implements TideTransactionManager {
			
		public Object begin(TideTransactionPersistenceManager pm) {
			if (pm != null)
				return pm.getCurrentTransaction();
			return null;
		}
		
		public void commit(Object t) throws Exception {
	        ((Transaction)t).commit();
		}
		
		public void rollback(Object t) {
	        ((Transaction)t).rollback();
		}
	}
}
