package cn.aioi.problem.ai;

import cn.aioi.problem.domain.DifficultyLevel;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class RuleBasedAiProvider implements AiProvider {
    @Override
    public AiAssessment assess(ProblemInput input) {
        String text = (input.title() + "\n" + input.text()).toLowerCase(Locale.ROOT);
        List<String> tags = new ArrayList<>();
        int score = 1;

        score += tagIf(text, tags, "动态规划", "dp", "dynamic programming", "状态转移") ? 2 : 0;
        score += tagIf(text, tags, "图论", "最短路", "树", "拓扑", "连通") ? 1 : 0;
        score += tagIf(text, tags, "数据结构", "线段树", "树状数组", "堆", "并查集") ? 2 : 0;
        score += tagIf(text, tags, "数学", "组合", "概率", "数论", "同余") ? 1 : 0;
        score += tagIf(text, tags, "搜索", "dfs", "bfs", "回溯") ? 1 : 0;
        score += tagIf(text, tags, "贪心", "greedy", "排序") ? 1 : 0;

        if (text.contains("10^5") || text.contains("100000") || text.contains("1e5")) {
            score += 1;
        }
        if (text.contains("10^6") || text.contains("1000000") || text.contains("1e6")) {
            score += 2;
        }
        if (text.contains("10^9") || text.contains("1e9")) {
            score += 1;
        }
        if (tags.isEmpty()) {
            tags.add("模拟");
        }

        DifficultyLevel difficulty = switch (Math.min(score, 6)) {
            case 1 -> DifficultyLevel.ENTRY;
            case 2 -> DifficultyLevel.EASY;
            case 3 -> DifficultyLevel.CSPJ_MEDIUM;
            case 4 -> DifficultyLevel.CSPS_ADVANCED;
            case 5 -> DifficultyLevel.NOIP_HARD;
            default -> DifficultyLevel.NOI_HELL;
        };
        List<String> hints = List.of(
                "先提取输入规模和需要维护的核心状态。",
                "把题目转成标签对应的经典模型，再考虑朴素做法是否超时。",
                "若朴素算法过慢，优先优化状态表示、转移或数据结构维护方式。"
        );
        return new AiAssessment(difficulty, 0.58 + Math.min(score, 5) * 0.06, tags, hints, "规则模型根据关键词、数据范围和算法标签给出初判。");
    }

    private boolean tagIf(String text, List<String> tags, String tag, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword.toLowerCase(Locale.ROOT))) {
                tags.add(tag);
                return true;
            }
        }
        return false;
    }
}

