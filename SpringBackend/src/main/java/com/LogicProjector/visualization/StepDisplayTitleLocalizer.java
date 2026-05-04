package com.LogicProjector.visualization;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class StepDisplayTitleLocalizer {

    private static final Map<String, String> EXACT_TITLES = Map.ofEntries(
            Map.entry("Check middle", "检查中点"),
            Map.entry("Choose pivot", "选择基准"),
            Map.entry("Compare to pivot", "与基准比较"),
            Map.entry("Move left of pivot", "移到基准左侧"),
            Map.entry("Place pivot", "放置基准"),
            Map.entry("Quick sort complete", "快速排序完成"),
            Map.entry("Merge sort complete", "归并排序完成"),
            Map.entry("Bubble sort complete", "冒泡排序完成"),
            Map.entry("Selection sort complete", "选择排序完成"),
            Map.entry("Insertion sort complete", "插入排序完成"),
            Map.entry("Heap sort complete", "堆排序完成"),
            Map.entry("Scan for minimum", "扫描最小值"),
            Map.entry("Update minimum", "更新最小值"),
            Map.entry("Swap into place", "交换到位"),
            Map.entry("Pick key", "取出待插入值"),
            Map.entry("Shift right", "右移元素"),
            Map.entry("Insert key", "插入到位"),
            Map.entry("Move max to sorted suffix", "最大值移入有序区"),
            Map.entry("Restore heap order", "恢复堆顺序"),
            Map.entry("Split range", "拆分区间"),
            Map.entry("Merge next value", "合并下一个值"),
            Map.entry("Append remaining left", "追加左侧剩余值"),
            Map.entry("Append remaining right", "追加右侧剩余值"));

    private StepDisplayTitleLocalizer() {
    }

    public static VisualizationPayload apply(VisualizationPayload payload) {
        List<VisualizationStep> localizedSteps = new ArrayList<>();
        for (VisualizationStep step : payload.steps()) {
            localizedSteps.add(new VisualizationStep(
                    step.title(),
                    step.narration(),
                    step.arrayState(),
                    step.activeIndices(),
                    step.highlightedLines(),
                    displayTitle(step.title())));
        }
        return new VisualizationPayload(payload.algorithm(), List.copyOf(localizedSteps));
    }

    public static String displayTitle(String title) {
        String exact = EXACT_TITLES.get(title);
        if (exact != null) {
            return exact;
        }
        String normalized = title.toLowerCase(Locale.ROOT);
        if (normalized.startsWith("compare ")) {
            return "比较元素";
        }
        if (normalized.startsWith("swap ")) {
            return "交换元素";
        }
        if (normalized.startsWith("heapify node")) {
            return "维护堆结构";
        }
        if (normalized.startsWith("visit node")) {
            return "访问节点";
        }
        if (normalized.startsWith("explore node")) {
            return "探索节点";
        }
        return title;
    }
}
