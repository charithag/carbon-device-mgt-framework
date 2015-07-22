package org.wso2.carbon.device.mgt.group.core.service;

import org.wso2.carbon.device.mgt.common.Device;
import org.wso2.carbon.device.mgt.common.DeviceIdentifier;
import org.wso2.carbon.device.mgt.group.common.Group;
import org.wso2.carbon.device.mgt.group.common.GroupManagementException;
import org.wso2.carbon.device.mgt.user.common.User;
import org.wso2.carbon.user.core.Permission;

import java.util.List;

public interface GroupManagementService {

    boolean createGroup(Group group) throws GroupManagementException;

    void updateGroup(Group group) throws GroupManagementException;

    boolean deleteGroup(int groupId) throws GroupManagementException;

    Group getGroupById(int groupId) throws GroupManagementException;

    Group getGroupByName(String groupName) throws GroupManagementException;

    List<Group> getGroupListOfUser(String username) throws GroupManagementException;

    int getGroupCountOfUser(String username) throws GroupManagementException;

    boolean shareGroup(String username, int groupId, String sharingRole) throws GroupManagementException;

    boolean unShareGroup(String userName, int groupId, String sharingRole) throws GroupManagementException;

    boolean addNewSharingRoleForGroup(String userName, int groupId, String roleName, Permission[] permissions) throws GroupManagementException;

    boolean removeSharingRoleForGroup(int groupId, String roleName) throws GroupManagementException;

    List<String> getAllRolesForGroup(int groupId) throws GroupManagementException;

    List<String> getGroupRolesForUser(String userName, int groupId) throws GroupManagementException;

    List<User> getUsersForGroup(int groupId) throws GroupManagementException;

    String[] getPermissionsForGroupRole(int groupId, String sharingRole) throws GroupManagementException;

    List<Device> getAllDevicesInGroup(int groupId) throws GroupManagementException;

    boolean addDeviceToGroup(DeviceIdentifier deviceId, int groupId) throws GroupManagementException;

}
