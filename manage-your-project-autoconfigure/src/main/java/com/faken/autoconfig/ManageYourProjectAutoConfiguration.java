package com.faken.autoconfig;

import com.faken.properties.ServerProperties;
import com.faken.util.DataSender;
import com.faken.util.ManageRegister;
import com.faken.util.RequestManageProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ServerProperties.class)
@ConditionalOnBean(ManageRegister.class)
public class ManageYourProjectAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ServerProperties serverProperties() {
        return new ServerProperties();
    }

    @Bean
    @ConditionalOnMissingBean
    public DataSender dataSender(ServerProperties serverProperties) {
        return new DataSender(
                serverProperties,
                serverProperties.isEnabledAccessLog(),
                serverProperties.isEnableErrorLog(),
                serverProperties.getInitialDelay(),
                serverProperties.getPeriod(),
                serverProperties.getTimeUnit()
        );
    }

    @Bean
    @ConditionalOnBean(ManageRegister.class)
    public RequestManageProcessor requestManageProcessor(ManageRegister manageRegister, DataSender dataSender) {
        return new RequestManageProcessor(manageRegister, dataSender);
    }
}
