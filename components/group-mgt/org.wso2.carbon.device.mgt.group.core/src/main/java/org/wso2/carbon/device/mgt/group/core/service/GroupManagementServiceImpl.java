package org.wso2.carbon.device.mgt.group.core.service;

import org.wso2.carbon.device.mgt.common.Device;
import org.wso2.carbon.device.mgt.common.DeviceIdentifier;
import org.wso2.carbon.device.mgt.group.common.Group;
import org.wso2.carbon.device.mgt.group.common.GroupManagementException;
import org.wso2.carbon.device.mgt.group.core.internal.DeviceMgtGroupDataHolder;
import org.wso2.carbon.device.mgt.user.common.User;
import org.wso2.carbon.user.core.Permission;

import java.util.List;

public class GroupManagementServiceImpl implements GroupManagementService {

    @Override
    public boolean createGroup(Group group) throws GroupManagementException {
        return DeviceMgtGroupDataHolder.getInstance().getGroupManagementServiceProvider().createGroup(group);
    }

    @Override
    public void updateGroup(Group group) throws GroupManagementException {
        DeviceMgtGroupDataHolder.getInstance().getGroupManagementServiceProvider().updateGroup(group);
    }

    @Override
    public boolean deleteGroup(int groupId) throws GroupManagementException {
        return DeviceMgtGroupDataHolder.getInstance().getGroupManagementServiceProvider().deleteGroup(groupId);
    }

    @Override
    public Group getGroupById(int groupId) throws GroupManagementException {
        return DeviceMgtGroupDataHolder.getInstance().getGroupManagementServiceProvider().getGroupById(groupId);
    }

    @Override
    public Group getGroupByName(String groupName) throws GroupManagementException {
        return DeviceMgtGroupDataHolder.getInstance().getGroupManagementServiceProvider().getGroupByName(groupName);
    }

    @Override
    public List<Group> getGroupListOfUser(String username) throws GroupManagementException {
        return DeviceMgtGroupDataHolder.getInstance().getGroupManagementServiceProvider().getGroupListOfUser(username);
    }

    @Override
    public int getGroupCountOfUser(String username) throws GroupManagementException {
        return DeviceMgtGroupDataHolder.getInstance().getGroupManagementServiceProvider().getGroupCountOfUser(username);
    }

    @Override
    public boolean shareGroup(String username, int groupId, String sharingRole) throws GroupManagementException {
        return DeviceMgtGroupDataHolder.getInstance().getGroupManagementServiceProvider().shareGroup(username, groupId, sharingRole);
    }

    @Override
    public boolean unShareGroup(String username, int groupId, String sharingRole) throws GroupManagementException {
        return DeviceMgtGroupDataHolder.getInstance().getGroupManagementServiceProvider().unShareGroup(username, groupId, sharingRole);
    }

    @Override
    public boolean addNewSharingRoleForGroup(String username, int groupId, String roleName, Permission[] permissions) throws GroupManagementException {
        return DeviceMgtGroupDataHolder.getInstance().getGroupManagementServiceProvider().addNewSharingRoleForGroup(username, groupId, roleName, permissions);
    }

    @Override
    public boolean removeSharingRoleForGroup(int groupId, String roleName) throws GroupManagementException {
        return DeviceMgtGroupDataHolder.getInstance().getGroupManagementServiceProvider().removeSharingRoleForGroup(groupId, roleName);
    }

    @Override
    public List<String> getAllRolesForGroup(int groupId) throws GroupManagementException {
        return DeviceMgtGroupDataHolder.getInstance().getGroupManagementServiceProvider().getAllRolesForGroup(groupId);
    }

    @Override
    public List<String> getGroupRolesForUser(String username, int groupId) throws GroupManagementException {
        return DeviceMgtGroupDataHolder.getInstance().getGroupManagementServiceProvider().getGroupRolesForUser(username, groupId);
    }

    @Override
    public List<User> getUsersForGroup(int groupId) throws GroupManagementException {
        return DeviceMgtGroupDataHolder.getInstance().getGroupManagementServiceProvider().getUsersForGroup(groupId);
    }

    @Override
    public String[] getPermissionsForGroupRole(int groupId, String sharingRole) throws GroupManagementException {
        return DeviceMgtGroupDataHolder.getInstance().getGroupManagementServiceProvider().getPermissionsForGroupRole(groupId, sharingRole);
    }

    @Override
    public List<Device> getAllDevicesInGroup(int groupId) throws GroupManagementException {
        return DeviceMgtGroupDataHolder.getInstance().getGroupManagementServiceProvider().getAllDevicesInGroup(groupId);
    }

    @Override
    public boolean addDeviceToGroup(DeviceIdentifier deviceId, int groupId) throws GroupManagementException {
        return DeviceMgtGroupDataHolder.getInstance().getGroupManagementServiceProvider().addDeviceToGroup(deviceId, groupId);
    }
}
