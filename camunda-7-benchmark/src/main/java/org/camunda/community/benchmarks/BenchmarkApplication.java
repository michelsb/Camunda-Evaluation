package org.camunda.community.benchmarks;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.spring.boot.starter.annotation.EnableProcessApplication;
import org.camunda.bpm.spring.boot.starter.event.PostDeployEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import org.springframework.util.Assert;

@SpringBootApplication
@EnableProcessApplication
@EnableScheduling
@EnableAsync
@ComponentScan
class BenchmarkApplication  {


    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(BenchmarkApplication.class, args);

        // Trigger here to make sure it happens AFTER the ApplicationContext is ready
        //context.getBean(ProcessDeployer.class).autoDeploy();
        /*for (String beanName : context.getBeanDefinitionNames()) {
            System.out.println(beanName);
        }*/

        context.getBean(JobWorker.class).startWorkers();
    }

    private final Logger logger = LoggerFactory.getLogger(BenchmarkApplication.class);

    @Autowired
    private RepositoryService repositoryService;

    @Bean
    public JavaDelegate sayHelloDelegate() {
        return execution -> logger.info("Hello!");
    }

    @EventListener
    public void notify(final PostDeployEvent unused) {
        final ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionKey("Sample").singleResult();

        logger.info("Found deployed process: {}", processDefinition);
        Assert.notNull(processDefinition, "process 'Sample' should be deployed!");
    }

}