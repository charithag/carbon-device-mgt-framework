/*
 * Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.device.mgt.core.service;

import org.wso2.carbon.device.mgt.common.*;
import org.wso2.carbon.device.mgt.common.Device;
import org.wso2.carbon.device.mgt.common.license.mgt.LicenseManager;
import org.wso2.carbon.device.mgt.common.operation.mgt.OperationManager;
import org.wso2.carbon.device.mgt.common.spi.DeviceManager;
import org.wso2.carbon.device.mgt.common.spi.GroupManager;
import org.wso2.carbon.device.mgt.core.dao.DeviceManagementDAOException;

import java.util.List;

/**
 * Proxy class for all Device Management related operations that take the corresponding plugin type in
 * and resolve the appropriate plugin implementation
 */
public interface DeviceManagementService extends DeviceManager, GroupManager, LicenseManager, OperationManager {

    List<Device> getAllDevices(String type) throws DeviceManagementException;

    List<Device> getAllDevices() throws DeviceManagementException;

    List<Device> getDeviceListOfUser(String username) throws DeviceManagementException;

    List<Device> getDevicesByGroup(int groupId) throws DeviceManagementException;

    /**
     * @param username of the user
     * @return List of all available groups
     * @throws GroupManagementException
     */
    List<Group> getGroupListOfUser(String username) throws GroupManagementException;

    void sendEnrolmentInvitation(EmailMessageProperties config) throws DeviceManagementException;

    void sendRegistrationEmail(EmailMessageProperties config) throws DeviceManagementException;

    FeatureManager getFeatureManager(String type) throws DeviceManagementException;

    /**
     * This method returns core device details.
     * @param deviceId
     * @return
     * @throws DeviceManagementException
     */
    Device getCoreDevice(DeviceIdentifier deviceId) throws DeviceManagementException;

    /**
     * Method to get the list of devices owned by an user.
     *
     * @param userName          Username of the user
     * @return List of devices owned by a particular user
     * @throws DeviceManagementException If some unusual behaviour is observed while fetching the
     * device list
     */
    List<Device> getAllDevicesOfUser(String userName) throws DeviceManagementException;

    /**
     * Method to get the list of devices owned by users of a particular user-role.
     *
     * @param roleName          Role name of the users
     * @return List of devices owned by users of a particular role
     * @throws DeviceManagementException If some unusual behaviour is observed while fetching the
     * device list
     */
    List<Device> getAllDevicesOfRole(String roleName) throws DeviceManagementException;

    /**
     * Method to get the count of all types of devices.
     * @return device count
     * @throws DeviceManagementException If some unusual behaviour is observed while counting
     * the devices
     */
    int getDeviceCount() throws DeviceManagementException;

    /**
     * Method to get the count of all groups.
     * @return group count
     * @throws GroupManagementException If some unusual behaviour is observed while counting
     * the devices
     */
    int getGroupCount() throws GroupManagementException;

    /**
     * Method to get the list of devices that matches with the given device name.
     *
     * @param deviceName    name of the device
     * @return List of devices that matches with the given device name.
     * @throws DeviceManagementException If some unusual behaviour is observed while fetching the
     * device list
     */
    List<Device> getDevicesByName(String deviceName, int tenantId) throws DeviceManagementException;

    /**
     * Method to get the list of groups that matches with the given group name.
     *
     * @param groupName name of the group
     * @return List of groups that matches with the given group name.
     * @throws GroupManagementException If some unusual behaviour is observed while fetching the
     *                                  device list
     */
    List<Group> getGroupsByName(String groupName, int tenantId) throws GroupManagementException;
}
