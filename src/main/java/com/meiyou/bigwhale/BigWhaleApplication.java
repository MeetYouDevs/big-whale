package com.meiyou.bigwhale;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.quartz.SchedulerFactoryBeanCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.transaction.annotation.EnableTransactionManagement;


@EnableTransactionManagement
@EnableConfigurationProperties
@SpringBootApplication
@PropertySource("big-whale.properties")
public class BigWhaleApplication {

    public static void main(String[] args) {
        SpringApplication.run(BigWhaleApplication.class, args);
    }

    @Configuration
    public class SchedulerFactoryBeanConfig implements SchedulerFactoryBeanCustomizer {

        @Override
        public void customize(SchedulerFactoryBean schedulerFactoryBean) {
            schedulerFactoryBean.setBeanName("BigWhaleScheduler");
        }

    }
}
