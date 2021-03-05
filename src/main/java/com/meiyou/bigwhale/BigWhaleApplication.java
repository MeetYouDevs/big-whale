package com.meiyou.bigwhale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.quartz.SchedulerFactoryBeanCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
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

        @Autowired
        private Environment environment;

        @Override
        public void customize(SchedulerFactoryBean schedulerFactoryBean) {
            boolean pro = false;
            for (String activeProfile : environment.getActiveProfiles()) {
                if ("pro".equals(activeProfile)) {
                    pro = true;
                    break;
                }
            }
            if (pro) {
                schedulerFactoryBean.setAutoStartup(false);
            } else {
                schedulerFactoryBean.setAutoStartup(true);
            }
            schedulerFactoryBean.setBeanName("BigWhaleScheduler");
        }

    }
}
