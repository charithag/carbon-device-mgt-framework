/*
 *   Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.wso2.carbon.device.mgt.core.operation.mgt.dao.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.device.mgt.common.DeviceIdentifier;
import org.wso2.carbon.device.mgt.core.dto.operation.mgt.Operation;
import org.wso2.carbon.device.mgt.core.dto.operation.mgt.ProfileOperation;
import org.wso2.carbon.device.mgt.core.operation.mgt.dao.OperationManagementDAOException;
import org.wso2.carbon.device.mgt.core.operation.mgt.dao.OperationManagementDAOFactory;
import org.wso2.carbon.device.mgt.core.operation.mgt.dao.OperationManagementDAOUtil;

import java.io.*;
import java.sql.*;

public class ProfileOperationDAOImpl extends OperationDAOImpl {

    private static final Log log = LogFactory.getLog(ProfileOperationDAOImpl.class);

    public int addOperation(Operation operation) throws OperationManagementDAOException {

        int operationId = super.addOperation(operation);
        operation.setCreatedTimeStamp(new Timestamp(new java.util.Date().getTime()).toString());
        operation.setId(operationId);
        operation.setEnabled(true);
        ProfileOperation profileOp = (ProfileOperation) operation;
        Connection conn = OperationManagementDAOFactory.getConnection();

        PreparedStatement stmt = null;

        try {
            stmt = conn.prepareStatement("INSERT INTO DM_PROFILE_OPERATION(OPERATION_ID, OPERATION_DETAILS) " +
                    "VALUES(?, ?)");
            stmt.setInt(1, operationId);
            stmt.setObject(2, profileOp);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new OperationManagementDAOException("Error occurred while adding profile operation", e);
        } finally {
            OperationManagementDAOUtil.cleanupResources(stmt);
        }
        return operationId;
    }

    public Operation getNextOperation(DeviceIdentifier deviceId) throws OperationManagementDAOException {

        PreparedStatement stmt = null;
        ResultSet rs = null;
        ByteArrayInputStream bais = null;
        ObjectInputStream ois = null;

        try {
            Connection connection = OperationManagementDAOFactory.getConnection();
            stmt = connection.prepareStatement(
                    "SELECT po.OPERATION_DETAILS AS OPERATION_DETAILS " +
                            "FROM DM_OPERATION o " +
                            "INNER JOIN DM_PROFILE_OPERATION po ON o.ID = po.OPERATION_ID AND o.STATUS =? AND o.ID IN ("
                            +
                            "SELECT dom.OPERATION_ID FROM (SELECT d.ID FROM DM_DEVICE d INNER JOIN " +
                            "DM_DEVICE_TYPE dm ON d.DEVICE_TYPE_ID = dm.ID AND dm.NAME = ? AND " +
                            "d.DEVICE_IDENTIFICATION = ?) d1 INNER JOIN DM_DEVICE_OPERATION_MAPPING dom " +
                            "ON d1.ID = dom.DEVICE_ID) ORDER BY o.CREATED_TIMESTAMP ASC LIMIT 1");

            stmt.setString(1, Operation.Status.PENDING.toString());
            stmt.setString(2, deviceId.getType());
            stmt.setString(3, deviceId.getId());
            rs = stmt.executeQuery();

            byte[] operationDetails = new byte[0];
            if (rs.next()) {
                operationDetails = rs.getBytes("OPERATION_DETAILS");
            }
            bais = new ByteArrayInputStream(operationDetails);
            ois = new ObjectInputStream(bais);
            return (ProfileOperation) ois.readObject();
        } catch (SQLException e) {
            log.error("SQL error occurred while retrieving profile operation", e);
            throw new OperationManagementDAOException("Error occurred while adding operation metadata", e);
        } catch (ClassNotFoundException e) {
            log.error("Class not found error occurred while retrieving profile operation", e);
            throw new OperationManagementDAOException("Error occurred while casting retrieved payload as a " +
                    "ProfileOperation object", e);
        } catch (IOException e) {
            log.error("IO error occurred while de serialize profile operation", e);
            throw new OperationManagementDAOException("Error occurred while serializing profile operation object", e);
        } finally {
            if (bais != null) {
                try {
                    bais.close();
                } catch (IOException e) {
                    log.warn("Error occurred while closing ByteArrayOutputStream", e);
                }
            }
            if (ois != null) {
                try {
                    ois.close();
                } catch (IOException e) {
                    log.warn("Error occurred while closing ObjectOutputStream", e);
                }
            }
            OperationManagementDAOUtil.cleanupResources(stmt, rs);
            OperationManagementDAOFactory.closeConnection();
        }
    }

    @Override
    public void updateOperation(Operation operation) throws OperationManagementDAOException {

        PreparedStatement stmt = null;
        ByteArrayOutputStream bao = null;
        ObjectOutputStream oos = null;
        super.updateOperation(operation);

        try {
            Connection connection = OperationManagementDAOFactory.getConnection();
            stmt = connection.prepareStatement("UPDATE DM_PROFILE_OPERATION O SET O.OPERATION_DETAILS=? " +
                    "WHERE O.OPERATION_ID=?");

            bao = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(bao);
            oos.writeObject(operation);

            stmt.setBytes(1, bao.toByteArray());
            stmt.setInt(2, operation.getId());
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new OperationManagementDAOException("Error occurred while update operation metadata", e);
        } catch (IOException e) {
            throw new OperationManagementDAOException("Error occurred while serializing profile operation object", e);
        } finally {
            if (bao != null) {
                try {
                    bao.close();
                } catch (IOException e) {
                    log.warn("Error occurred while closing ByteArrayOutputStream", e);
                }
            }
            if (oos != null) {
                try {
                    oos.close();
                } catch (IOException e) {
                    log.warn("Error occurred while closing ObjectOutputStream", e);
                }
            }
            OperationManagementDAOUtil.cleanupResources(stmt);
        }
    }

    @Override
    public void deleteOperation(int id) throws OperationManagementDAOException {

        super.deleteOperation(id);
        PreparedStatement stmt = null;
        try {
            Connection connection = OperationManagementDAOFactory.getConnection();
            stmt = connection.prepareStatement("DELETE DM_PROFILE_OPERATION WHERE OPERATION_ID=?");
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new OperationManagementDAOException("Error occurred while deleting operation metadata", e);
        } finally {
            OperationManagementDAOUtil.cleanupResources(stmt);
        }
    }
}
