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
package org.granite.gravity.adapters;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.granite.config.flex.Adapter;
import org.granite.config.flex.Destination;
import org.granite.config.flex.ServicesConfig;
import org.granite.context.GraniteContext;
import org.granite.gravity.GravityInternal;
import org.granite.logging.Logger;
import org.granite.messaging.service.ServiceException;
import org.granite.util.TypeUtil;

import flex.messaging.messages.AsyncMessage;
import flex.messaging.messages.CommandMessage;
import flex.messaging.messages.Message;

/**
 * @author William DRAI
 */
public class AdapterFactory implements Serializable {

    private static final long serialVersionUID = 1L;


    private static final Logger log = Logger.getLogger(AdapterFactory.class);
    private static final ReentrantLock lock = new ReentrantLock();

    private GravityInternal gravity;
    private Map<String, ServiceAdapter> adaptersCache = new ConcurrentHashMap<String, ServiceAdapter>();
    private List<ServiceAdapter> adapters = new ArrayList<ServiceAdapter>();
    private static Class<SimpleServiceAdapter> defaultAdapterClass = SimpleServiceAdapter.class;


    public AdapterFactory(GravityInternal gravity) {
        this.gravity = gravity;
    }


    public ServiceAdapter getServiceAdapter(Message request) throws ServiceException {

        String messageType = request.getClass().getName();
        if (request instanceof CommandMessage)
            messageType = ((CommandMessage)request).getMessageRefType();
        if (messageType == null)
            messageType = AsyncMessage.class.getName();
        String destinationId = request.getDestination();

        return getServiceAdapter(messageType, destinationId);
    }

    public ServiceAdapter getServiceAdapter(String messageType, String destinationId) throws ServiceException {
        GraniteContext context = GraniteContext.getCurrentInstance();

        log.debug(">> Finding serviceAdapter for messageType: %s and destinationId: %s", messageType, destinationId);

        ServicesConfig servicesConfig = context.getServicesConfig();
        Destination destination = servicesConfig.findDestinationById(messageType, destinationId);
        if (destination == null) {
            log.debug(">> No destination found: %s", destinationId);
            return null;
        }
        Adapter adapter = destination.getAdapter();

        String key = null;

        if (adapter != null) {
            log.debug(">> Found adapterRef: %s", adapter.getId());
            key = AdapterFactory.class.getName() + '@' + destination.getId() + '.' + adapter.getId();
        }
        else
            key = defaultAdapterClass.getName() + '@' + destination.getId();

        return getServiceAdapter(adaptersCache, context, destination, key, adapter != null ? adapter.getId() : null);
    }

    private ServiceAdapter getServiceAdapter(Map<String, ServiceAdapter> cache, GraniteContext context, Destination destination, String key, String adapterId) {
        lock.lock();
        try {
            ServiceAdapter serviceAdapter = cache.get(key);
            if (serviceAdapter == null) {
                log.debug(">> No cached factory for: %s", adapterId);

                Adapter config = destination.getAdapter();
                try {
                    Class<? extends ServiceAdapter> clazz = (adapterId != null)
                        ? TypeUtil.forName(config.getClassName(), ServiceAdapter.class)
                        : defaultAdapterClass;
                    serviceAdapter = clazz.newInstance();
                    serviceAdapter.setId(adapterId);
                    serviceAdapter.setGravity(gravity);
                    serviceAdapter.configure(config.getProperties(), destination.getProperties());
                    serviceAdapter.start();

                    adapters.add(serviceAdapter);
                }
                catch (ServiceException e) {
                	throw e;
                }
                catch (Exception e) {
                    throw new ServiceException("Could not instantiate serviceAdapter: " + config, e);
                }
                cache.put(key, serviceAdapter);
            }
            else
                log.debug(">> Found a cached serviceAdapter for ref: %s", destination.getAdapter());

            log.debug("<< Returning serviceAdapter: %s", serviceAdapter);

            serviceAdapter.setDestination(destination);
            return serviceAdapter;
        } finally {
            lock.unlock();
        }
    }


    public void stopAll() {
        for (ServiceAdapter adapter : adapters) {
            adapter.stop();
        }
    }


    @Override
    public String toString() {
        return toString(null);
    }

    public String toString(String append) {
        return super.toString() + " {" +
            (append != null ? append : "") +
        "\n}";
    }
}
