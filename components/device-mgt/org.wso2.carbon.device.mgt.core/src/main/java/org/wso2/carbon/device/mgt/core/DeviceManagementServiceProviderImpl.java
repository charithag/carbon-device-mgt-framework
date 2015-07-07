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
package org.wso2.carbon.device.mgt.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.device.mgt.common.*;
import org.wso2.carbon.device.mgt.common.license.mgt.License;
import org.wso2.carbon.device.mgt.common.license.mgt.LicenseManagementException;
import org.wso2.carbon.device.mgt.common.operation.mgt.Operation;
import org.wso2.carbon.device.mgt.common.operation.mgt.OperationManagementException;
import org.wso2.carbon.device.mgt.common.spi.DeviceManager;
import org.wso2.carbon.device.mgt.core.config.DeviceConfigurationManager;
import org.wso2.carbon.device.mgt.core.config.email.NotificationMessages;
import org.wso2.carbon.device.mgt.core.dao.*;
import org.wso2.carbon.device.mgt.core.dao.util.DeviceManagementDAOUtil;
import org.wso2.carbon.device.mgt.core.dto.DeviceType;
import org.wso2.carbon.device.mgt.core.dto.Status;
import org.wso2.carbon.device.mgt.core.email.EmailConstants;
import org.wso2.carbon.device.mgt.core.internal.DeviceManagementDataHolder;
import org.wso2.carbon.device.mgt.core.internal.EmailServiceDataHolder;
import org.wso2.carbon.device.mgt.core.service.DeviceManagementService;
import org.wso2.carbon.device.mgt.core.util.DeviceManagerUtil;

import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class DeviceManagementServiceProviderImpl implements DeviceManagementService {

    private static Log log = LogFactory.getLog(DeviceManagementServiceProviderImpl.class);
    private DeviceDAO deviceDAO;
    private GroupDAO groupDAO;
    private DeviceTypeDAO deviceTypeDAO;
    private DeviceManagementRepository pluginRepository;

    public DeviceManagementServiceProviderImpl(DeviceManagementRepository pluginRepository) {
        this.pluginRepository = pluginRepository;
        this.deviceDAO = DeviceManagementDAOFactory.getDeviceDAO();
        this.groupDAO = DeviceManagementDAOFactory.getGroupDAO();
        this.deviceTypeDAO = DeviceManagementDAOFactory.getDeviceTypeDAO();
    }

    public DeviceManagementServiceProviderImpl() {
        this.deviceDAO = DeviceManagementDAOFactory.getDeviceDAO();
        this.groupDAO = DeviceManagementDAOFactory.getGroupDAO();
        this.deviceTypeDAO = DeviceManagementDAOFactory.getDeviceTypeDAO();
    }

    @Override
    public String getProviderType() {
        return null;
    }

    @Override
    public FeatureManager getFeatureManager() {
        return null;
    }

    @Override
    public void addGroup(Group group) throws GroupManagementException {
        try {
            this.groupDAO.addGroup(group);
        } catch (GroupManagementDAOException e) {
            throw new GroupManagementException("Error occurred while adding group " +
                    "'" + group.getName() + "'", e);
        }
    }

    @Override
    public void modifyGroup(Group group) throws GroupManagementException {
        try {
            this.groupDAO.updateGroup(group);
        } catch (GroupManagementDAOException e) {
            throw new GroupManagementException("Error occurred while modifying group " +
                    "'" + group.getName() + "'", e);
        }
    }

    @Override
    public void removeGroup(int groupId) throws GroupManagementException {
        try {
            this.groupDAO.deleteGroup(groupId);
        } catch (GroupManagementDAOException e) {
            throw new GroupManagementException("Error occurred while removing group " +
                    "'" + groupId + "'", e);
        }
    }

    @Override
    public FeatureManager getFeatureManager(String type) {
        DeviceManager dms =
                this.getPluginRepository().getDeviceManagementProvider(type);
        return dms.getFeatureManager();
    }

    @Override
    public Device getCoreDevice(DeviceIdentifier deviceId) throws DeviceManagementException {

        Device convertedDevice = null;
        try {
            DeviceType deviceType = this.getDeviceTypeDAO().getDeviceType(deviceId.getType());
            org.wso2.carbon.device.mgt.core.dto.Device device = this.getDeviceDAO().getDevice(deviceId);
            if (device != null) {
                convertedDevice = DeviceManagementDAOUtil.convertDevice(device,
                        this.getDeviceTypeDAO().getDeviceType(deviceType.getId()));
            }
        } catch (DeviceManagementDAOException e) {
            throw new DeviceManagementException("Error occurred while obtaining the device for id " +
                    "'" + deviceId.getId() + "' and type:" + deviceId.getType(), e);
        }
        return convertedDevice;
    }

    @Override
    public boolean enrollDevice(Device device) throws DeviceManagementException {
        DeviceManager dms =
                this.getPluginRepository().getDeviceManagementProvider(device.getType());
        boolean status = dms.enrollDevice(device);
        try {
            org.wso2.carbon.device.mgt.core.dto.Device deviceDto = DeviceManagementDAOUtil.convertDevice(device);
            DeviceType deviceType = this.getDeviceTypeDAO().getDeviceType(device.getType());
            if (dms.isClaimable(new DeviceIdentifier(device.getDeviceIdentifier(), deviceType.getName()))) {
                deviceDto.setStatus(Status.INACTIVE);
            } else {
                deviceDto.setStatus(Status.ACTIVE);
            }
            deviceDto.setDeviceTypeId(deviceType.getId());
            this.getDeviceDAO().addDevice(deviceDto);
        } catch (DeviceManagementDAOException e) {
            throw new DeviceManagementException("Error occurred while enrolling the device " +
                    "'" + device.getId() + "'", e);
        }
        return status;
    }

    @Override
    public boolean modifyEnrollment(Device device) throws DeviceManagementException {
        DeviceManager dms =
                this.getPluginRepository().getDeviceManagementProvider(device.getType());
        boolean status = dms.modifyEnrollment(device);
        try {
            this.getDeviceDAO().updateDevice(DeviceManagementDAOUtil.convertDevice(device));
        } catch (DeviceManagementDAOException e) {
            throw new DeviceManagementException("Error occurred while modifying the device " +
                    "'" + device.getId() + "'", e);
        }
        return status;
    }

    @Override
    public boolean disenrollDevice(DeviceIdentifier deviceId) throws DeviceManagementException {
        DeviceManager dms =
                this.getPluginRepository().getDeviceManagementProvider(deviceId.getType());
        return dms.disenrollDevice(deviceId);
    }

    @Override
    public boolean isEnrolled(DeviceIdentifier deviceId) throws DeviceManagementException {
        DeviceManager dms =
                this.getPluginRepository().getDeviceManagementProvider(deviceId.getType());
        return dms.isEnrolled(deviceId);
    }

    @Override
    public boolean isActive(DeviceIdentifier deviceId) throws DeviceManagementException {
        DeviceManager dms =
                this.getPluginRepository().getDeviceManagementProvider(deviceId.getType());
        return dms.isActive(deviceId);
    }

    @Override
    public boolean setActive(DeviceIdentifier deviceId, boolean status)
            throws DeviceManagementException {
        DeviceManager dms =
                this.getPluginRepository().getDeviceManagementProvider(deviceId.getType());
        return dms.setActive(deviceId, status);
    }

    @Override
    public List<Device> getAllDevices() throws DeviceManagementException {
        List<Device> convertedDevicesList = new ArrayList<Device>();
        try {
            List<org.wso2.carbon.device.mgt.core.dto.Device> devicesList = this.deviceDAO.getDevices();
            for (int x = 0; x < devicesList.size(); x++) {
                org.wso2.carbon.device.mgt.core.dto.Device device = devicesList.get(x);
                device.setDeviceType(deviceTypeDAO.getDeviceType(device.getDeviceTypeId()));
                DeviceManager dms =
                        this.getPluginRepository().getDeviceManagementProvider(device.getDeviceType().getName());
                DeviceType deviceType = this.deviceTypeDAO.getDeviceType(device.getDeviceTypeId());
                Device convertedDevice = DeviceManagementDAOUtil.convertDevice(device, deviceType);
                DeviceIdentifier deviceIdentifier =
                        DeviceManagementDAOUtil.createDeviceIdentifier(device, deviceType);
                Device dmsDevice = dms.getDevice(deviceIdentifier);
                if (dmsDevice != null) {
                    convertedDevice.setProperties(dmsDevice.getProperties());
                    convertedDevice.setFeatures(dmsDevice.getFeatures());
                }
                convertedDevicesList.add(convertedDevice);
            }
        } catch (DeviceManagementDAOException e) {
            throw new DeviceManagementException("Error occurred while obtaining devices all devices", e);
        }
        return convertedDevicesList;
    }

    @Override
    public List<Device> getAllDevices(String type) throws DeviceManagementException {
        DeviceManager dms = this.getPluginRepository().getDeviceManagementProvider(type);
        List<Device> devicesList = new ArrayList<Device>();
        try {
            DeviceType dt = this.getDeviceTypeDAO().getDeviceType(type);
            List<org.wso2.carbon.device.mgt.core.dto.Device> devices =
                    this.getDeviceDAO().getDevices(dt.getId());

            for (org.wso2.carbon.device.mgt.core.dto.Device device : devices) {
                DeviceType deviceType = this.deviceTypeDAO.getDeviceType(device.getDeviceTypeId());
                Device convertedDevice = DeviceManagementDAOUtil.convertDevice(device, deviceType);
                DeviceIdentifier deviceIdentifier =
                        DeviceManagementDAOUtil.createDeviceIdentifier(device, deviceType);
                Device dmsDevice = dms.getDevice(deviceIdentifier);
                if (dmsDevice != null) {
                    convertedDevice.setProperties(dmsDevice.getProperties());
                    convertedDevice.setFeatures(dmsDevice.getFeatures());
                }
                devicesList.add(convertedDevice);
            }
        } catch (DeviceManagementDAOException e) {
            throw new DeviceManagementException("Error occurred while obtaining the device for type " +
                    "'" + type + "'", e);
        }
        return devicesList;
    }

    @Override
    public List<Device> getDeviceListOfUser(String username) throws DeviceManagementException {
        List<Device> devicesOfUser = new ArrayList<Device>();
        try {
            int tenantId = DeviceManagerUtil.getTenantId();
            List<org.wso2.carbon.device.mgt.core.dto.Device> devicesList = this.deviceDAO
                    .getDeviceListOfUser(username, tenantId);
            for (int x = 0; x < devicesList.size(); x++) {
                org.wso2.carbon.device.mgt.core.dto.Device device = devicesList.get(x);
                device.setDeviceType(deviceTypeDAO.getDeviceType(device.getDeviceTypeId()));
                DeviceManager dms =
                        this.getPluginRepository().getDeviceManagementProvider(device.getDeviceType().getName());
                Device convertedDevice = DeviceManagementDAOUtil.convertDevice(device, device.getDeviceType());
                DeviceIdentifier deviceIdentifier = new DeviceIdentifier();
                deviceIdentifier.setId(device.getDeviceIdentificationId());
                deviceIdentifier.setType(device.getDeviceType().getName());
                Device dmsDevice = dms.getDevice(deviceIdentifier);
                if (dmsDevice != null) {
                    convertedDevice.setProperties(dmsDevice.getProperties());
                    convertedDevice.setFeatures(dmsDevice.getFeatures());
                }
                devicesOfUser.add(convertedDevice);
            }
        } catch (DeviceManagementDAOException e) {
            throw new DeviceManagementException("Error occurred while obtaining devices for user " +
                    "'" + username + "'", e);
        }
        return devicesOfUser;
    }

    @Override
    public List<Device> getDevicesByGroup(int groupId) throws DeviceManagementException {
        List<Device> devicesInGroup = new ArrayList<Device>();
        try {
            int tenantId = DeviceManagerUtil.getTenantId();
            List<org.wso2.carbon.device.mgt.core.dto.Device> devicesList = this.deviceDAO.getDevicesByGroup(groupId, tenantId);
            for (int x = 0; x < devicesList.size(); x++) {
                org.wso2.carbon.device.mgt.core.dto.Device device = devicesList.get(x);
                device.setDeviceType(deviceTypeDAO.getDeviceType(device.getDeviceTypeId()));
                DeviceManager dms =
                        this.getPluginRepository().getDeviceManagementProvider(device.getDeviceType().getName());
                Device convertedDevice = DeviceManagementDAOUtil.convertDevice(device, device.getDeviceType());
                DeviceIdentifier deviceIdentifier = new DeviceIdentifier();
                deviceIdentifier.setId(device.getDeviceIdentificationId());
                deviceIdentifier.setType(device.getDeviceType().getName());
                Device dmsDevice = dms.getDevice(deviceIdentifier);
                if (dmsDevice != null) {
                    convertedDevice.setProperties(dmsDevice.getProperties());
                    convertedDevice.setFeatures(dmsDevice.getFeatures());
                }
                devicesInGroup.add(convertedDevice);
            }
        } catch (DeviceManagementDAOException e) {
            throw new DeviceManagementException("Error occurred while obtaining devices for group " +
                    "'" + groupId + "'", e);
        }
        return devicesInGroup;
    }

    @Override
    public List<Group> getAllGroups() throws GroupManagementException {
        try {
            return this.groupDAO.getGroups();
        } catch (GroupManagementDAOException e) {
            throw new GroupManagementException("Error occurred while obtaining groups", e);
        }
    }

    @Override
    public Group getGroup(int groupId) throws GroupManagementException {
        try {
            return this.groupDAO.getGroup(groupId);
        } catch (GroupManagementDAOException e) {
            throw new GroupManagementException("Error occurred while obtaining group " + groupId, e);
        }
    }

    @Override
    public List<Group> getGroupListOfUser(String username) throws GroupManagementException {
        int tenantId = DeviceManagerUtil.getTenantId();
        try {
            return this.groupDAO.getGroupListOfUser(username, tenantId);
        } catch (GroupManagementDAOException e) {
            throw new GroupManagementException("Error occurred while obtaining groups for user " + username, e);
        }
    }

    @Override
    public void sendEnrolmentInvitation(EmailMessageProperties emailMessageProperties)
            throws DeviceManagementException {

        List<NotificationMessages> notificationMessages = DeviceConfigurationManager.getInstance()
                .getNotificationMessagesConfig().getNotificationMessagesList();

        String messageHeader = "";
        String messageBody = "";
        String messageFooter1 = "";
        String messageFooter2 = "";
        String messageFooter3 = "";
        String url = "";
        String subject = "";

        for (NotificationMessages notificationMessage : notificationMessages) {
            if (DeviceManagementConstants.EmailNotifications.ENROL_NOTIFICATION_TYPE.
                    equals(notificationMessage.getType())) {
                messageHeader = notificationMessage.getHeader();
                messageBody = notificationMessage.getBody();
                messageFooter1 = notificationMessage.getFooterLine1();
                messageFooter2 = notificationMessage.getFooterLine2();
                messageFooter3  = notificationMessage.getFooterLine3();
                url = notificationMessage.getUrl();
                subject = notificationMessage.getSubject();
                break;
            }
        }

        StringBuilder messageBuilder = new StringBuilder();

        try {
            messageHeader = messageHeader.replaceAll("\\{" + EmailConstants.EnrolmentEmailConstants.FIRST_NAME + "\\}",
                    URLEncoder.encode(emailMessageProperties.getFirstName(),
                            EmailConstants.EnrolmentEmailConstants.ENCODED_SCHEME));
            messageBody = messageBody.trim() + System.getProperty("line.separator") +
                    System.getProperty("line.separator") + url.replaceAll("\\{"
                            + EmailConstants.EnrolmentEmailConstants.DOWNLOAD_URL + "\\}",
                    URLDecoder.decode(emailMessageProperties.getEnrolmentUrl(),
                            EmailConstants.EnrolmentEmailConstants.ENCODED_SCHEME));

            messageBuilder.append(messageHeader).append(System.getProperty("line.separator"))
                    .append(System.getProperty("line.separator"));
            messageBuilder.append(messageBody);
            messageBuilder.append(System.getProperty("line.separator")).append(System.getProperty("line.separator"));
            messageBuilder.append(messageFooter1.trim())
                    .append(System.getProperty("line.separator")).append(messageFooter2.trim()).append(System
                    .getProperty("line.separator")).append(messageFooter3.trim());

        } catch (IOException e) {
            log.error("IO error in processing enrol email message " + emailMessageProperties);
            throw new DeviceManagementException("Error replacing tags in email template '" +
                    emailMessageProperties.getSubject() + "'", e);
        }
        emailMessageProperties.setMessageBody(messageBuilder.toString());
        emailMessageProperties.setSubject(subject);
        EmailServiceDataHolder.getInstance().getEmailServiceProvider().sendEmail(emailMessageProperties);
    }

    @Override
    public void sendRegistrationEmail(EmailMessageProperties emailMessageProperties) throws DeviceManagementException {
        List<NotificationMessages> notificationMessages = DeviceConfigurationManager.getInstance()
                .getNotificationMessagesConfig().getNotificationMessagesList();

        String messageHeader = "";
        String messageBody = "";
        String messageFooter1 = "";
        String messageFooter2 = "";
        String messageFooter3 = "";
        String url = "";
        String subject = "";

        for (NotificationMessages notificationMessage : notificationMessages) {
            if (DeviceManagementConstants.EmailNotifications.USER_REGISTRATION_NOTIFICATION_TYPE.
                    equals(notificationMessage.getType())) {
                messageHeader = notificationMessage.getHeader();
                messageBody = notificationMessage.getBody();
                messageFooter1 = notificationMessage.getFooterLine1();
                messageFooter2 = notificationMessage.getFooterLine2();
                messageFooter3 = notificationMessage.getFooterLine3();
                url = notificationMessage.getUrl();
                subject = notificationMessage.getSubject();
                break;
            }
        }

        StringBuilder messageBuilder = new StringBuilder();

        try {
            messageHeader = messageHeader.replaceAll("\\{" + EmailConstants.EnrolmentEmailConstants.FIRST_NAME + "\\}",
                    URLEncoder.encode(emailMessageProperties.getFirstName(),
                            EmailConstants.EnrolmentEmailConstants.ENCODED_SCHEME));

            messageBody = messageBody.trim().replaceAll("\\{" + EmailConstants.EnrolmentEmailConstants
                            .USERNAME
                            + "\\}",
                    URLEncoder.encode(emailMessageProperties.getUserName(), EmailConstants.EnrolmentEmailConstants
                            .ENCODED_SCHEME));

            messageBody = messageBody.replaceAll("\\{" + EmailConstants.EnrolmentEmailConstants.PASSWORD + "\\}",
                    URLEncoder.encode(emailMessageProperties.getPassword(), EmailConstants.EnrolmentEmailConstants
                            .ENCODED_SCHEME));

            messageBody = messageBody + System.getProperty("line.separator") + url.replaceAll("\\{"
                            + EmailConstants.EnrolmentEmailConstants.DOWNLOAD_URL + "\\}",
                    URLDecoder.decode(emailMessageProperties.getEnrolmentUrl(),
                            EmailConstants.EnrolmentEmailConstants.ENCODED_SCHEME));

            messageBuilder.append(messageHeader).append(System.getProperty("line.separator"));
            messageBuilder.append(messageBody).append(System.getProperty("line.separator")).append(messageFooter1.trim());
            messageBuilder.append(System.getProperty("line.separator")).append(messageFooter2.trim());
            messageBuilder.append(System.getProperty("line.separator")).append(messageFooter3.trim());

        } catch (IOException e) {
            log.error("IO error in processing enrol email message " + emailMessageProperties);
            throw new DeviceManagementException("Error replacing tags in email template '" +
                    emailMessageProperties.getSubject() + "'", e);
        }
        emailMessageProperties.setMessageBody(messageBuilder.toString());
        emailMessageProperties.setSubject(subject);
        EmailServiceDataHolder.getInstance().getEmailServiceProvider().sendEmail(emailMessageProperties);
    }

    @Override
    public Device getDevice(DeviceIdentifier deviceId) throws DeviceManagementException {

        DeviceManager dms = this.getPluginRepository().getDeviceManagementProvider(deviceId.getType());
        Device convertedDevice = null;
        try {
            DeviceType deviceType =
                    this.getDeviceTypeDAO().getDeviceType(deviceId.getType());
            org.wso2.carbon.device.mgt.core.dto.Device device =
                    this.getDeviceDAO().getDevice(deviceId);
            if (device != null) {
                convertedDevice = DeviceManagementDAOUtil
                        .convertDevice(device, this.getDeviceTypeDAO().getDeviceType(deviceType.getId()));
                Device dmsDevice = dms.getDevice(deviceId);
                if (dmsDevice != null) {
                    convertedDevice.setProperties(dmsDevice.getProperties());
                    convertedDevice.setFeatures(dmsDevice.getFeatures());
                }
            }
        } catch (DeviceManagementDAOException e) {
            throw new DeviceManagementException("Error occurred while obtaining the device for id " +
                    "'" + deviceId.getId() + "'", e);
        }
        return convertedDevice;
    }

    @Override
    public boolean updateDeviceInfo(DeviceIdentifier deviceIdentifier, Device device) throws DeviceManagementException {
        DeviceManager dms =
                this.getPluginRepository().getDeviceManagementProvider(device.getType());
        return dms.updateDeviceInfo(deviceIdentifier, device);
    }

    @Override
    public boolean setOwnership(DeviceIdentifier deviceId, String ownershipType)
            throws DeviceManagementException {
        DeviceManager dms =
                this.getPluginRepository().getDeviceManagementProvider(deviceId.getType());
        return dms.setOwnership(deviceId, ownershipType);
    }

    @Override
    public boolean isClaimable(DeviceIdentifier deviceId) throws DeviceManagementException {
        DeviceManager dms =
                this.getPluginRepository().getDeviceManagementProvider(deviceId.getType());
        return dms.isClaimable(deviceId);
    }

    @Override
    public License getLicense(String deviceType, String languageCode) throws LicenseManagementException {
        return DeviceManagementDataHolder.getInstance().getLicenseManager().getLicense(deviceType, languageCode);
    }

    @Override
    public boolean addLicense(String type, License license) throws LicenseManagementException {
        return DeviceManagementDataHolder.getInstance().getLicenseManager().addLicense(type, license);
    }

    public DeviceDAO getDeviceDAO() {
        return deviceDAO;
    }

    public DeviceTypeDAO getDeviceTypeDAO() {
        return deviceTypeDAO;
    }

    public DeviceManagementRepository getPluginRepository() {
        return pluginRepository;
    }

    @Override
    public boolean addOperation(Operation operation, List<DeviceIdentifier> devices) throws
            OperationManagementException {
        return DeviceManagementDataHolder.getInstance().getOperationManager().addOperation(operation, devices);
    }

    @Override
    public List<? extends Operation> getOperations(DeviceIdentifier deviceId) throws OperationManagementException {
        return DeviceManagementDataHolder.getInstance().getOperationManager().getOperations(deviceId);
    }

    @Override
    public List<? extends Operation> getPendingOperations(DeviceIdentifier deviceId)
            throws OperationManagementException {
        return DeviceManagementDataHolder.getInstance().getOperationManager().getPendingOperations(deviceId);
    }

    @Override
    public Operation getNextPendingOperation(DeviceIdentifier deviceId) throws OperationManagementException {
        return DeviceManagementDataHolder.getInstance().getOperationManager().getNextPendingOperation(deviceId);
    }

    @Override
    public void updateOperation(int operationId, Operation.Status operationStatus)
            throws OperationManagementException {
        DeviceManagementDataHolder.getInstance().getOperationManager().updateOperation(operationId, operationStatus);
    }

    @Override
    public void deleteOperation(int operationId) throws OperationManagementException {
        DeviceManagementDataHolder.getInstance().getOperationManager().deleteOperation(operationId);
    }

    @Override
    public Operation getOperationByDeviceAndOperationId(DeviceIdentifier deviceId, int operationId)
            throws OperationManagementException {
        return DeviceManagementDataHolder.getInstance().getOperationManager().getOperationByDeviceAndOperationId(
                deviceId, operationId);
    }

    @Override
    public List<? extends Operation> getOperationsByDeviceAndStatus(DeviceIdentifier identifier,
            Operation.Status status) throws OperationManagementException, DeviceManagementException {
        return DeviceManagementDataHolder.getInstance().getOperationManager().getOperationsByDeviceAndStatus(identifier,
                status);
    }

    @Override
    public Operation getOperation(int operationId) throws OperationManagementException {
        return DeviceManagementDataHolder.getInstance().getOperationManager().getOperation(operationId);
    }

    @Override
    public List<? extends Operation> getOperationsForStatus(Operation.Status status)
            throws OperationManagementException {
        return DeviceManagementDataHolder.getInstance().getOperationManager().getOperationsForStatus(status);
    }

    @Override
    public List<Device> getAllDevicesOfUser(String userName)
            throws DeviceManagementException {
        List<Device> devicesOfUser = new ArrayList<Device>();
        List<org.wso2.carbon.device.mgt.core.dto.Device> devicesList;
        Device convertedDevice;
        DeviceIdentifier deviceIdentifier;
        DeviceManager dms;
        Device dmsDevice;
        org.wso2.carbon.device.mgt.core.dto.Device device;
        int tenantId = DeviceManagerUtil.getTenantId();
        //Fetch the DeviceList from Core
        try {
            devicesList = this.getDeviceDAO().getDeviceListOfUser(userName, tenantId);
        } catch (DeviceManagementDAOException e) {
            throw new DeviceManagementException("Error occurred while obtaining the devices of user '"
                    + userName + "'", e);
        }

        //Fetch the DeviceList from device plugin dbs & append the properties
        for (int x = 0; x < devicesList.size(); x++) {
            device = devicesList.get(x);
            try {
                //TODO : Possible improvement if DeviceTypes have been cached
                device.setDeviceType(deviceTypeDAO.getDeviceType(device.getDeviceTypeId()));
                dms = this.getPluginRepository().getDeviceManagementProvider(device.getDeviceType().getName());
                convertedDevice = DeviceManagementDAOUtil.convertDevice(device, device.getDeviceType());
                deviceIdentifier = new DeviceIdentifier();
                deviceIdentifier.setId(device.getDeviceIdentificationId());
                deviceIdentifier.setType(device.getDeviceType().getName());
                dmsDevice = dms.getDevice(deviceIdentifier);
                if (dmsDevice != null) {
                    convertedDevice.setProperties(dmsDevice.getProperties());
                    convertedDevice.setFeatures(dmsDevice.getFeatures());
                }
                devicesOfUser.add(convertedDevice);
            } catch (DeviceManagementDAOException e) {
                log.error("Error occurred while obtaining the device type of DeviceTypeId '" +
                        device.getDeviceTypeId() + "'", e);
            }
        }
        return devicesOfUser;
    }

    @Override
    public List<Device> getAllDevicesOfRole(String roleName)
            throws DeviceManagementException {
        List<Device> devicesOfRole = new ArrayList<Device>();
        List<org.wso2.carbon.device.mgt.core.dto.Device> devicesList;
        List<org.wso2.carbon.device.mgt.user.common.User> users;
        Device convertedDevice;
        DeviceIdentifier deviceIdentifier;
        DeviceManager dms;
        Device dmsDevice;
        org.wso2.carbon.device.mgt.core.dto.Device device;
        String userName = "";
        int tenantId = DeviceManagerUtil.getTenantId();
        //Obtaining the list of users of role
        try {
            users = DeviceManagementDataHolder.getInstance().getUserManager().getUsersForTenantAndRole(
                    tenantId, roleName);
        } catch (org.wso2.carbon.device.mgt.user.common.UserManagementException e) {
            throw new DeviceManagementException("Error occurred while obtaining the users of role '"
                    + roleName + "'", e);
        }

        //Obtaining the devices per user
        for (org.wso2.carbon.device.mgt.user.common.User user : users) {
            try {
                userName = user.getUserName();
                devicesList = this.getDeviceDAO().getDeviceListOfUser(userName, tenantId);
                for (int x = 0; x < devicesList.size(); x++) {
                    device = devicesList.get(x);
                    try {
                        //TODO : Possible improvement if DeviceTypes have been cached
                        device.setDeviceType(deviceTypeDAO.getDeviceType(device.getDeviceTypeId()));
                        dms = this.getPluginRepository().getDeviceManagementProvider(device.getDeviceType().getName());
                        convertedDevice = DeviceManagementDAOUtil.convertDevice(device, device.getDeviceType());
                        deviceIdentifier = new DeviceIdentifier();
                        deviceIdentifier.setId(device.getDeviceIdentificationId());
                        deviceIdentifier.setType(device.getDeviceType().getName());
                        dmsDevice = dms.getDevice(deviceIdentifier);
                        if (dmsDevice != null) {
                            convertedDevice.setProperties(dmsDevice.getProperties());
                            convertedDevice.setFeatures(dmsDevice.getFeatures());
                        }
                        devicesOfRole.add(convertedDevice);
                    } catch (DeviceManagementDAOException e) {
                        log.error("Error occurred while obtaining the device type of DeviceTypeId '" +
                                device.getDeviceTypeId() + "'", e);
                    }
                }
            } catch (DeviceManagementDAOException e) {
                log.error("Error occurred while obtaining the devices of user '"
                        + userName + "'", e);
            }
        }
        return devicesOfRole;
    }

    @Override
    public int getDeviceCount() throws DeviceManagementException {
        try {
            int deviceCount = this.deviceDAO.getDeviceCount();
            return deviceCount;
        } catch (DeviceManagementDAOException e) {
            log.error("Error occurred while counting devices", e);
            throw new DeviceManagementException("Error occurred while counting devices", e);
        }
    }

    @Override
    public int getGroupCount() throws GroupManagementException {
        return this.getGroupCount();
    }

    @Override
    public List<Device> getDevicesByName(String deviceName, int tenantId) throws DeviceManagementException {
        List<Device> devicesOfUser = new ArrayList<Device>();
        List<org.wso2.carbon.device.mgt.core.dto.Device> devicesList;
        Device convertedDevice;
        DeviceIdentifier deviceIdentifier;
        DeviceManager dms;
        Device dmsDevice;
        org.wso2.carbon.device.mgt.core.dto.Device device;

        try {
            devicesList = this.getDeviceDAO().getDevicesByName(deviceName, tenantId);
        } catch (DeviceManagementDAOException e) {
            throw new DeviceManagementException("Error occurred while fetching the list of devices that matches to '"
                                                + deviceName + "'", e);
        }

        for (int x = 0; x < devicesList.size(); x++) {
            device = devicesList.get(x);
            try {
                device.setDeviceType(deviceTypeDAO.getDeviceType(device.getDeviceTypeId()));
                dms = this.getPluginRepository().getDeviceManagementProvider(device.getDeviceType().getName());
                convertedDevice = DeviceManagementDAOUtil.convertDevice(device, device.getDeviceType());
                deviceIdentifier = new DeviceIdentifier();
                deviceIdentifier.setId(device.getDeviceIdentificationId());
                deviceIdentifier.setType(device.getDeviceType().getName());
                dmsDevice = dms.getDevice(deviceIdentifier);
                if (dmsDevice != null) {
                    convertedDevice.setProperties(dmsDevice.getProperties());
                    convertedDevice.setFeatures(dmsDevice.getFeatures());
                }
                devicesOfUser.add(convertedDevice);
            } catch (DeviceManagementDAOException e) {
                log.error("Error occurred while obtaining the device type of DeviceTypeId '" +
                          device.getDeviceTypeId() + "'", e);
            }
        }
        return devicesOfUser;
    }

    @Override
    public List<Group> getGroupsByName(String groupName, int tenantId) throws GroupManagementException {
        return this.getGroupsByName(groupName, tenantId);
    }

    public GroupDAO getGroupDAO() {
        return groupDAO;
    }

}
