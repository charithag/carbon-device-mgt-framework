/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.device.mgt.group.core.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.device.mgt.core.config.DeviceConfigurationManager;
import org.wso2.carbon.device.mgt.core.config.DeviceManagementConfig;
import org.wso2.carbon.device.mgt.core.config.datasource.DataSourceConfig;
import org.wso2.carbon.device.mgt.core.service.DeviceManagementService;
import org.wso2.carbon.device.mgt.group.core.dao.GroupManagementDAOFactory;
import org.wso2.carbon.device.mgt.group.core.providers.GroupManagementServiceProvider;
import org.wso2.carbon.device.mgt.group.core.providers.GroupManagementServiceProviderImpl;
import org.wso2.carbon.device.mgt.group.core.service.GroupManagementService;
import org.wso2.carbon.device.mgt.group.core.service.GroupManagementServiceImpl;
import org.wso2.carbon.device.mgt.user.core.UserManager;
import org.wso2.carbon.user.core.service.RealmService;

/**
 * @scr.component name="org.wso2.carbon.device.groupmanager" immediate="true"
 * @scr.reference name="user.realmservice.default"
 * interface="org.wso2.carbon.user.core.service.RealmService"
 * cardinality="1..1"
 * policy="dynamic"
 * bind="setRealmService"
 * unbind="unsetRealmService"
 * @scr.reference name="org.wso2.carbon.device.manager"
 * interface="org.wso2.carbon.device.mgt.core.service.DeviceManagementService"
 * cardinality="1..1"
 * policy="dynamic"
 * bind="setDeviceManagementService"
 * unbind="unsetDeviceManagementService"
 * @scr.reference name="org.wso2.carbon.device.mgt.user.core.usermanager"
 * interface="org.wso2.carbon.device.mgt.user.core.UserManager"
 * cardinality="1..1
 * policy="dynamic"
 * bind="setUserManager"
 * unbind="unsetUserManager"
 */

public class DeviceMgtGroupServiceComponent {

    private static Log log = LogFactory.getLog(DeviceMgtGroupServiceComponent.class);

    protected void activate(ComponentContext componentContext) {
        try {

            if (log.isDebugEnabled()) {
                log.debug("Initializing group management core bundle");
            }

            DeviceManagementConfig config =
                    DeviceConfigurationManager.getInstance().getDeviceManagementConfig();

            DataSourceConfig dsConfig = config.getDeviceManagementConfigRepository().getDataSourceConfig();
            GroupManagementDAOFactory.init(dsConfig);

            GroupManagementServiceProvider groupManagementServiceProvider = new GroupManagementServiceProviderImpl();
            DeviceMgtGroupDataHolder.getInstance().setGroupManagementServiceProvider(groupManagementServiceProvider);

            if (log.isDebugEnabled()) {
                log.debug("Registering OSGi service Group Management Service");
            }
            /* Registering Group Management service */
            BundleContext bundleContext = componentContext.getBundleContext();
            bundleContext.registerService(GroupManagementService.class,
                    new GroupManagementServiceImpl(), null);
            if (log.isDebugEnabled()) {
                log.debug("Group management core bundle has been successfully initialized");
            }
        } catch (Throwable e) {
            String msg = "Error occurred while initializing group management core bundle";
            log.error(msg, e);
        }
    }

    /**
     * Sets Realm Service.
     *
     * @param realmService An instance of RealmService
     */
    protected void setRealmService(RealmService realmService) {
        DeviceMgtGroupDataHolder.getInstance().setRealmService(realmService);
    }

    /**
     * Un sets Realm Service.
     *
     * @param realmService An instance of RealmService
     */
    protected void unsetRealmService(RealmService realmService) {
        DeviceMgtGroupDataHolder.getInstance().setRealmService(null);
    }

    /**
     * Sets GroupManagementService.
     *
     * @param deviceMgtService An instance of GroupManagementService
     */
    protected void setDeviceManagementService(DeviceManagementService deviceMgtService) {
        if (log.isDebugEnabled()) {
            log.debug("Setting DeviceManager Service");
        }
        DeviceMgtGroupDataHolder.getInstance().setDeviceManagementService(deviceMgtService);
    }

    /**
     * Unsets GroupManagementService.
     *
     * @param deviceMgtService An instance of UserManager
     */
    protected void unsetDeviceManagementService(DeviceManagementService deviceMgtService) {
        if (log.isDebugEnabled()) {
            log.debug("Unsetting DeviceManager Service");
        }
        DeviceMgtGroupDataHolder.getInstance().setDeviceManagementService(null);
    }

    /**
     * Sets UserManager Service.
     *
     * @param userMgtService An instance of UserManager
     */
    protected void setUserManager(UserManager userMgtService) {
        if (log.isDebugEnabled()) {
            log.debug("Setting UserManager Service");
        }
        DeviceMgtGroupDataHolder.getInstance().setUserManager(userMgtService);
    }

    /**
     * Unsets UserManager Service.
     *
     * @param userMgtService An instance of UserManager
     */
    protected void unsetUserManager(UserManager userMgtService) {
        if (log.isDebugEnabled()) {
            log.debug("Unsetting UserManager Service");
        }
        DeviceMgtGroupDataHolder.getInstance().setUserManager(null);
    }

}
