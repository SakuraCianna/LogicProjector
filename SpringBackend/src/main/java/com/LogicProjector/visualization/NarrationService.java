package com.LogicProjector.visualization;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.LogicProjector.analysis.AiChatClient;
import com.LogicProjector.analysis.DetectedAlgorithm;
import com.fasterxml.jackson.databind.JsonNode;

@Service
public class NarrationService {

    private static final String SYSTEM_PROMPT = """
            你负责为 Java 算法演示生成中文课堂讲解。
            必须返回严格 JSON，字段为 summary 和 stepNarrations。
            summary 用 1 句中文概括算法过程，表达具体、适合课堂讲解。
            stepNarrations 必须是中文数组，按输入步骤顺序逐步对应，每一步只写 1 句简短中文。
            保留每一步确定性的状态含义，可以提升表达清晰度，但不能编造不存在的状态变化。
            不要提到不支持、无法确定、模型限制或实现不确定性。
            """;

    private final AiChatClient aiChatClient;

    public NarrationService(AiChatClient aiChatClient) {
        this.aiChatClient = aiChatClient;
    }

    public NarrationResult createNarration(DetectedAlgorithm algorithm, VisualizationPayload payload, String sourceCode) {
        try {
            JsonNode response = aiChatClient.createStructuredResponse(
                    SYSTEM_PROMPT,
                    buildNarrationPrompt(algorithm, payload, sourceCode));
            String summary = response.path("summary").asText("").trim();
            List<String> stepNarrations = extractStepNarrations(response, payload.steps().size());
            if (!summary.isEmpty()) {
                return new NarrationResult(summary, stepNarrations);
            }
        } catch (RuntimeException ignored) {
            // Fall back to deterministic copy if narration generation fails.
        }

        String summary = switch (algorithm) {
            case QUICK_SORT -> "快速排序选择基准值完成分区，再递归排序左右两侧。";
            case BINARY_SEARCH -> "二分查找不断缩小搜索区间，直到定位目标位置。";
            case MERGE_SORT -> "归并排序先拆分区间，再按顺序合并已排序的子区间。";
            case BUBBLE_SORT -> "冒泡排序反复比较相邻元素，把较大的值逐步交换到右侧。";
            case SELECTION_SORT -> "选择排序每轮找到剩余区间最小值，并放入已排序前缀。";
            case INSERTION_SORT -> "插入排序把每个新元素插入左侧已排序区间的正确位置。";
            default -> "算法会通过数据结构变化逐步讲解。";
        };
        return new NarrationResult(summary, List.of());
    }

    private List<String> extractStepNarrations(JsonNode response, int stepCount) {
        JsonNode stepNarrationsNode = response.path("stepNarrations");
        if (!stepNarrationsNode.isArray() || stepNarrationsNode.size() != stepCount) {
            return List.of();
        }

        List<String> stepNarrations = new ArrayList<>();
        for (JsonNode stepNarrationNode : stepNarrationsNode) {
            String narration = stepNarrationNode.asText("").trim();
            if (narration.isEmpty()) {
                return List.of();
            }
            stepNarrations.add(narration);
        }
        return List.copyOf(stepNarrations);
    }

    private String buildNarrationPrompt(DetectedAlgorithm algorithm, VisualizationPayload payload, String sourceCode) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("算法：").append(algorithm.name()).append("\n");
        prompt.append("步骤数量：").append(payload.steps().size()).append("\n");
        prompt.append("步骤：\n");

        for (int index = 0; index < payload.steps().size(); index++) {
            VisualizationStep step = payload.steps().get(index);
            prompt.append(index + 1)
                    .append(". 标题=")
                    .append(step.title())
                    .append("；当前讲解=")
                    .append(step.narration())
                    .append("\n");
        }

        prompt.append("源代码：\n").append(sourceCode);
        return prompt.toString();
    }
}
