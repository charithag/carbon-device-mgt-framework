package org.wso2.carbon.device.mgt.group.core.dao;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.device.mgt.core.config.datasource.DataSourceConfig;
import org.wso2.carbon.device.mgt.core.config.datasource.JNDILookupDefinition;
import org.wso2.carbon.device.mgt.core.dao.DeviceDAO;
import org.wso2.carbon.device.mgt.core.dao.DeviceTypeDAO;
import org.wso2.carbon.device.mgt.core.dao.impl.DeviceDAOImpl;
import org.wso2.carbon.device.mgt.core.dao.impl.DeviceTypeDAOImpl;
import org.wso2.carbon.device.mgt.core.dao.util.DeviceManagementDAOUtil;

import javax.sql.DataSource;
import java.util.Hashtable;
import java.util.List;

public class GroupManagementDAOFactory {


    private static final Log log = LogFactory.getLog(GroupManagementDAOFactory.class);
    private static DataSource dataSource;

    public static DeviceDAO getDeviceDAO() {
        return new DeviceDAOImpl(dataSource);
    }

    public static GroupDAO getGroupDAO() {
        return new GroupDAOImpl(dataSource);
    }

    public static DeviceTypeDAO getDeviceTypeDAO() {
        return new DeviceTypeDAOImpl(dataSource);
    }

    public static void init(DataSourceConfig config) {
        dataSource = resolveDataSource(config);
    }

    public static void init(DataSource dtSource) {
        dataSource = dtSource;
    }

    /**
     * Resolve data source from the data source definition
     *
     * @param config data source configuration
     * @return data source resolved from the data source definition
     */
    private static DataSource resolveDataSource(DataSourceConfig config) {
        DataSource dataSource = null;
        if (config == null) {
            throw new RuntimeException(
                    "Device Management Repository data source configuration " + "is null and " +
                            "thus, is not initialized");
        }
        JNDILookupDefinition jndiConfig = config.getJndiLookupDefinition();
        if (jndiConfig != null) {
            if (log.isDebugEnabled()) {
                log.debug("Initializing Device Management Repository data source using the JNDI " +
                        "Lookup Definition");
            }
            List<JNDILookupDefinition.JNDIProperty> jndiPropertyList =
                    jndiConfig.getJndiProperties();
            if (jndiPropertyList != null) {
                Hashtable<Object, Object> jndiProperties = new Hashtable<Object, Object>();
                for (JNDILookupDefinition.JNDIProperty prop : jndiPropertyList) {
                    jndiProperties.put(prop.getName(), prop.getValue());
                }
                dataSource = DeviceManagementDAOUtil
                        .lookupDataSource(jndiConfig.getJndiName(), jndiProperties);
            } else {
                dataSource =
                        DeviceManagementDAOUtil.lookupDataSource(jndiConfig.getJndiName(), null);
            }
        }
        return dataSource;
    }

    public static DataSource getDataSource() {
        return dataSource;
    }


}
