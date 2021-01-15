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

import javax.persistence.PostPersist;
import javax.persistence.PostRemove;
import javax.persistence.PostUpdate;

import org.granite.tide.data.DataContext.EntityUpdateType;
import org.granite.tide.data.ExcludeFromDataPublish.ExcludeMode;


public class DataPublishListener {
    
    @PostPersist
    public void onPostPersist(Object entity) {
    	if (handleExclude(entity))
    		return;
    	DataContext.addUpdate(EntityUpdateType.PERSIST, entity, entity);
    }
    
    @PostRemove
    public void onPostRemove(Object entity) {
    	if (handleExclude(entity))
    		return;
    	DataContext.addUpdate(EntityUpdateType.REMOVE, entity, entity);
    }
    
    @PostUpdate
    public void onPostUpdate(Object entity) {
    	if (handleExclude(entity))
    		return;
    	DataContext.addUpdate(EntityUpdateType.UPDATE, entity, entity);
    }
    
    public static boolean handleExclude(Object entity) {
    	if (entity == null || DataContext.get() == null)
    		return true;
    	if (!entity.getClass().isAnnotationPresent(ExcludeFromDataPublish.class))
    		return false;
    	ExcludeFromDataPublish exclude = entity.getClass().getAnnotation(ExcludeFromDataPublish.class);
    	if (exclude.value() == ExcludeMode.CHANGES)
        	DataContext.addUpdate(EntityUpdateType.REFRESH, entity.getClass(), entity.getClass());
    	return true;
    }
}
