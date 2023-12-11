package com.alvin.demo.controller;

import com.alvin.demo.utils.HighLightUtil;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.DeploymentBuilder;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipInputStream;

@RestController
@RequestMapping("/")
public class TestController {

    @Autowired
    private ProcessEngine processEngine ;

    @GetMapping("/test")
    public Map<String,String> deployment(){

        //2.得到RepositoryService实例
        RepositoryService repositoryService = processEngine.getRepositoryService();

        //3.进行部署
        Deployment deployment = repositoryService.createDeployment()
                .addClasspathResource("process/test1.bpmn")  //添加bpmn资源
                .addClasspathResource("process/test1.png")
                .name("请假申请单流程005")
                .deploy();

        Map<String, String> map = new HashMap<>() ;

        map.put("id", deployment.getId()) ;
        map.put("name", deployment.getName()) ;
        map.put("key" ,deployment.getKey());
        return map ;
    }

    @PostMapping("/test1")
    public Map<String,String> deployment1(
            @RequestParam("file") MultipartFile file
    ){

        DeploymentBuilder deploymentBuilder = processEngine
                .getRepositoryService().createDeployment();
        ZipInputStream zipInputStream = null;
        try {
            zipInputStream = new ZipInputStream(file.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        deploymentBuilder.addZipInputStream(zipInputStream).name("请假流程001");
        Deployment deployment = deploymentBuilder.deploy();

        Map<String, String> map = new HashMap<>() ;

        map.put("id", deployment.getId()) ;
        map.put("name", deployment.getName()) ;

        return map ;
    }

    @Autowired
    private RuntimeService runtimeService ;

    // definitionKey 为 bxlc 在设计 流程图的时候写死的
    @GetMapping("startInstances")
    public Map<String, String> startInstances(
            @RequestParam("definitionKey") String definitionKey
    ) {
        ProcessInstance processInstance = runtimeService
                .startProcessInstanceByKey(definitionKey);

        runtimeService.setProcessInstanceName(
                processInstance.getProcessInstanceId(),
                "张三请10天假");

        Map<String, String> map = new HashMap<>() ;

        map.put("id", processInstance.getId()) ;
        map.put("name", processInstance.getName()) ;

        return map ;
    }


    @Autowired
    private TaskService taskService ;

    @GetMapping("listTask")
    public List<Map<String, String>> listTask(
            @RequestParam(name = "userName") String userName
    ){
        //3.查询当前用户的任务
        List<Task> tasks = taskService.createTaskQuery()
                .orderByTaskCreateTime().desc()
                .taskAssignee(userName).list() ;

        List<Map<String, String>> list = new ArrayList<>();

        tasks.forEach(task -> {
            Map<String, String> map = new HashMap<>();
            map.put("processInstanceId", task.getProcessInstanceId()) ;
            map.put("taskId", task.getId()) ;

            list.add(map);
        });

        return list ;
    }

    @GetMapping("completeTask/{taskId}")
    public String completeTask(@PathVariable String taskId){
        taskService.complete(taskId) ;
        return taskId + "任务完成" ;
    }

    @Autowired
    private HighLightUtil highLightUtil ;

    @GetMapping("getImage/{instanceId}")
    public void getImage(
            @PathVariable String instanceId ,
            HttpServletResponse response
    ) throws IOException {
        InputStream inputStream = highLightUtil.getFlowImgByInstanceId(instanceId);

        // 输出图片内容
        int byteSize = 1024;
        byte[] b = new byte[byteSize];
        int len;
        while ((len = inputStream.read(b, 0, byteSize)) != -1) {
            response.getOutputStream().write(b, 0, len);
        }
    }
}
