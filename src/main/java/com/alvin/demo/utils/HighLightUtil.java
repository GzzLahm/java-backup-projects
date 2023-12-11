package com.alvin.demo.utils;

import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.FlowNode;
import org.activiti.bpmn.model.SequenceFlow;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.image.ProcessDiagramGenerator;
import org.activiti.image.impl.DefaultProcessDiagramGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

@Component
public class HighLightUtil {

	@Autowired
	private RepositoryService repositoryService ;

	@Autowired
	private HistoryService historyService ;

	/**
	 * 根据流程实例Id,获取实时流程图片
	 * @param processInstanceId 流程实例Id
	 */
	public InputStream getFlowImgByInstanceId(String processInstanceId) {
		try {
			if (processInstanceId == null || processInstanceId.trim().length() == 0) {
				System.out.println("processInstanceId is null");
				return null;
			}
			// 获取历史流程实例
			HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
					.processInstanceId(processInstanceId).singleResult();
			// 获取流程中已经执行的节点，按照执行先后顺序排序
			List<HistoricActivityInstance> historicActivityInstances = historyService
					.createHistoricActivityInstanceQuery().processInstanceId(processInstanceId)
					.orderByHistoricActivityInstanceId().asc().list();
			// 高亮已经执行流程节点ID集合
			List<String> highLightedActivitiIds = new ArrayList<>();
			for (HistoricActivityInstance historicActivityInstance : historicActivityInstances) {
				highLightedActivitiIds.add(historicActivityInstance.getActivityId());
			}

			ProcessDiagramGenerator processDiagramGenerator = null;
			
			processDiagramGenerator = new DefaultProcessDiagramGenerator();

			BpmnModel bpmnModel = repositoryService.getBpmnModel(historicProcessInstance.getProcessDefinitionId());
			
			
			// 高亮流程已发生流转的线id集合
			List<String> highLightedFlowIds = getHighLightedFlows(bpmnModel, historicActivityInstances);
			
			
			
			// 使用默认配置获得流程图表生成器，并生成追踪图片字符流
			return processDiagramGenerator.generateDiagram(
					bpmnModel,
					highLightedActivitiIds,
					highLightedFlowIds,
					"宋体", "宋体", "宋体");

		} catch (Exception e) {
			System.out.println(
					"processInstanceId" + processInstanceId
							+ "生成流程图失败，原因：" + e.getMessage());

			return null;
		}

	}

	/**
	 * 获取已经流转的线
	 */
	private List<String> getHighLightedFlows(BpmnModel bpmnModel,
			List<HistoricActivityInstance> historicActivityInstances) {

		// 流转线ID集合
		List<String> flowIdList = new ArrayList<>();
		// 全部活动实例
		List<FlowNode> historicFlowNodeList = new LinkedList<>();
		// 已完成的历史活动节点
		List<HistoricActivityInstance> finishedActivityInstanceList = new LinkedList<>();
		
		for (HistoricActivityInstance historicActivityInstance : historicActivityInstances) {
			
			historicFlowNodeList.add((FlowNode) bpmnModel.getMainProcess()
					.getFlowElement(historicActivityInstance.getActivityId(), true));
			
			if (historicActivityInstance.getEndTime() != null) {
				finishedActivityInstanceList.add(historicActivityInstance);
			}
		}

		// 遍历已完成的活动实例，从每个实例的outgoingFlows中找到已执行的
		FlowNode currentFlowNode = null;
		for (HistoricActivityInstance currentActivityInstance : finishedActivityInstanceList) {
			// 获得当前活动对应的节点信息及outgoingFlows信息
			currentFlowNode = (FlowNode) bpmnModel.getMainProcess()
					.getFlowElement(currentActivityInstance.getActivityId(), true);
			List<SequenceFlow> sequenceFlowList = currentFlowNode.getOutgoingFlows();

			/**
			 * 遍历outgoingFlows并找到已已流转的 满足如下条件认为已已流转：
			 * 1.当前节点是并行网关或包含网关，则通过outgoingFlows能够在历史活动中找到的全部节点均为已流转
			 * 2.当前节点是以上两种类型之外的，通过outgoingFlows查找到的时间最近的流转节点视为有效流转
			 */
			FlowNode targetFlowNode = null;
			if ("parallelGateway".equals(currentActivityInstance.getActivityType())
					|| "inclusiveGateway".equals(currentActivityInstance.getActivityType())) {
				// 遍历历史活动节点，找到匹配Flow目标节点的
				for (SequenceFlow sequenceFlow : sequenceFlowList) {
					targetFlowNode = (FlowNode) bpmnModel.getMainProcess().getFlowElement(sequenceFlow.getTargetRef(),
							true);
					if (historicFlowNodeList.contains(targetFlowNode)
							&& (sequenceFlowList.size() > 1 || Objects.nonNull(currentActivityInstance.getEndTime()))) {
						flowIdList.add(sequenceFlow.getId());
					}
				}
			} else {
				// 遍历历史活动节点，找到匹配Flow目标节点的
				for (SequenceFlow sequenceFlow : sequenceFlowList) {
					for (HistoricActivityInstance historicActivityInstance : historicActivityInstances) {
						if (historicActivityInstance.getActivityId().equals(sequenceFlow.getTargetRef())) {
							if (Objects.nonNull(currentActivityInstance.getEndTime())
									&& Objects.nonNull(historicActivityInstance.getStartTime())) {
								
								SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
								
								String endTime = sdf.format(currentActivityInstance.getEndTime());
								
								String startTime = sdf.format(historicActivityInstance.getStartTime());
								

								if (startTime.equals(endTime)) {
									flowIdList.add(sequenceFlow.getId());
								}
							}
						}
					}
				}
			}
		}
		return flowIdList;
	}

}
