package com.alvin.demo.config;

import org.activiti.engine.*;
import org.activiti.engine.impl.cfg.StandaloneProcessEngineConfiguration;
import org.activiti.engine.impl.history.HistoryLevel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class ActivitiConfig {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private ProcessEngineConfiguration processEngineConfiguration ;

    @Autowired
    private ProcessEngine processEngine ;

    @Bean(name = "processEngine")
    public ProcessEngine processEngine(){
        ProcessEngine processEngine =  processEngineConfiguration.buildProcessEngine();
        return processEngine;
    }
    @Bean(name = "processEngineConfiguration")
    public ProcessEngineConfiguration processEngineConfiguration(){

        ProcessEngineConfiguration processEngineConfiguration =
                new StandaloneProcessEngineConfiguration();
        processEngineConfiguration.setDataSource(dataSource);
        // 自动建表
        processEngineConfiguration.setDatabaseSchemaUpdate("true");
        // 保存完整历史
        processEngineConfiguration.setHistoryLevel(HistoryLevel.FULL) ;
        processEngineConfiguration.setDbHistoryUsed(true);
        return processEngineConfiguration;
    }

    @Bean(name = "repositoryService")
    public RepositoryService repositoryService(){
        return processEngine.getRepositoryService();
    }

    @Bean(name = "runtimeService")
    public RuntimeService runtimeService(){
        return processEngine.getRuntimeService();
    }

    @Bean(name = "taskService")
    public TaskService taskService(){
        return processEngine.getTaskService();
    }

    @Bean(name = "historyService")
    public HistoryService historyService(){
        return processEngine.getHistoryService();
    }
}
