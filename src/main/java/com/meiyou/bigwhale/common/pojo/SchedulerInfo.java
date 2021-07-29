package com.meiyou.bigwhale.common.pojo;

import com.meiyou.bigwhale.common.Constant.YarnResourcemanagerScheduler;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * YARN resource manager scheduler info
 *
 * <a href="https://hadoop.apache.org/docs/current/hadoop-yarn/hadoop-yarn-site/ResourceManagerRest.html#Cluster_Scheduler_API">schedulerInfo</a>
 *
 * Elements of the schedulerInfo object
 *
 * @author yangjie
 */
@Data
public class SchedulerInfo implements Serializable {

    /**
     * @see YarnResourcemanagerScheduler
     */
    private String type;
    private Float capacity;
    private Float usedCapacity;
    private Float maxCapacity;
    private String queueName;
    private List<Queue> queues;
    private Health health;

    /**
     * Elements of the queues object for a Parent queue
     *
     * Elements of the queues object for a Leaf queue - contains all the elements in parent except ‘queues’ plus the following
     */
    @Data
    public static class Queue {
        /**
         * Elements of the queues object for a Parent queue
         */
        private Float capacity;
        private Float usedCapacity;
        private Float maxCapacity;
        private Float absoluteCapacity;
        private Float absoluteMaxCapacity;
        private Float absoluteUsedCapacity;
        private Integer numApplications;
        private String usedResources;
        private String queueName;
        private String state;
        private List<Queue> queues;
        private Resource resourcesUsed;
        /**
         * Elements of the queues object for a Leaf queue
         * contains all the elements in parent except ‘queues’ plus the following
         */
        private String type;
        private Integer numActiveApplications;
        private Integer numPendingApplications;
        private Integer numContainers;
        private Integer allocatedContainers;
        private Integer reservedContainers;
        private Integer pendingContainers;
        private Integer maxApplications;
        private Integer maxApplicationsPerUser;
        private Integer maxActiveApplications;
        private Integer maxActiveApplicationsPerUser;
        private Integer userLimit;
        private Float userLimitFactor;
        private List<User> users;
    }

    /**
     * Elements of the user object for users
     */
    @Data
    public static class User {
        private String username;
        private Resource resourcesUsed;
        private Integer numActiveApplications;
        private Integer numPendingApplications;
    }

    /**
     * Elements of the resource object for resourcesUsed in user and queues
     */
    @Data
    public static class Resource {
        private Integer memory;
        private Integer vCores;
    }

    /**
     * Elements of the health object in schedulerInfo
     */
    @Data
    public static class Health {
        private Long lastrun;
        private List<Operation> operationsInfo;
        private List<LastRunDetail> lastRunDetails;
    }

    /**
     * Elements of the operation object in health
     */
    @Data
    public static class Operation {
        private String operation;
        private String nodeId;
        private String containerId;
        private String queue;
    }

    /**
     * Elements of the lastRunDetail object in health
     */
    @Data
    public static class LastRunDetail {
        private String operation;
        private Long count;
        private Resource resources;
    }
}
