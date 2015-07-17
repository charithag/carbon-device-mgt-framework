/*
 *  Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.wso2.carbon.device.mgt.group.core.internal;

import org.wso2.carbon.device.mgt.core.service.DeviceManagementService;
import org.wso2.carbon.device.mgt.group.core.GroupManagementServiceProvider;
import org.wso2.carbon.device.mgt.user.core.UserManager;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.user.core.tenant.TenantManager;

public class DeviceMgtGroupDataHolder {

    private RealmService realmService;
    private TenantManager tenantManager;
    private static DeviceMgtGroupDataHolder thisInstance = new DeviceMgtGroupDataHolder();
    private UserManager userManager;
    private GroupManagementServiceProvider groupManagementServiceProvider;
    private DeviceManagementService deviceManagementService;

    private DeviceMgtGroupDataHolder() {
    }

    public static DeviceMgtGroupDataHolder getInstance() {
        return thisInstance;
    }

    public TenantManager getTenantManager() {
        return tenantManager;
    }

    private void setTenantManager(RealmService realmService) {
        if (realmService == null) {
            throw new IllegalStateException("Realm service is not initialized properly");
        }
        this.tenantManager = realmService.getTenantManager();
    }

    public RealmService getRealmService() {
        return realmService;
    }

    public void setRealmService(RealmService realmService) {
        this.realmService = realmService;
        this.setTenantManager(realmService);
    }

    public UserManager getUserManager() {
        return userManager;
    }

    public void setUserManager(UserManager userManager) {
        this.userManager = userManager;
    }

    public GroupManagementServiceProvider getGroupManagementServiceProvider() {
        return groupManagementServiceProvider;
    }

    public void setGroupManagementServiceProvider(GroupManagementServiceProvider groupManagementServiceProvider) {
        this.groupManagementServiceProvider = groupManagementServiceProvider;
    }

    public DeviceManagementService getDeviceManagementService() {
        return deviceManagementService;
    }

    public void setDeviceManagementService(DeviceManagementService deviceManagementService) {
        this.deviceManagementService = deviceManagementService;
    }
}