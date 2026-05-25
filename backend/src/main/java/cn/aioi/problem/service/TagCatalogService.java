package cn.aioi.problem.service;

import cn.aioi.problem.api.dto.TagDtos;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class TagCatalogService {
    private final List<TagDtos.TagCategory> categories;
    private final Set<String> standardTags;
    private final Set<String> categoryNames;
    private final Map<String, String> aliases;
    private final String promptText;

    public TagCatalogService() {
        this.categories = buildCategories();
        this.standardTags = categories.stream()
                .flatMap(category -> category.tags().stream())
                .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);
        this.categoryNames = categories.stream()
                .map(TagDtos.TagCategory::name)
                .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);
        this.aliases = buildAliases();
        this.promptText = buildPromptText();
    }

    public List<TagDtos.TagCategory> categories() {
        return categories;
    }

    public Set<String> standardTags() {
        return standardTags;
    }

    public boolean isCategory(String value) {
        return categoryNames.contains(cleanDisplay(value));
    }

    public boolean isStandardTag(String value) {
        return standardTags.contains(cleanDisplay(value));
    }

    public List<String> normalizeTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String tag : tags) {
            normalizeOne(tag).ifPresent(normalized::add);
        }
        return normalized.stream().limit(12).toList();
    }

    public List<String> normalizeAiTags(List<String> tags) {
        List<String> normalized = normalizeTags(tags);
        return normalized.isEmpty() ? List.of("模拟") : normalized;
    }

    public java.util.Optional<String> normalizeOne(String raw) {
        String display = cleanDisplay(raw);
        if (display.isBlank() || categoryNames.contains(display)) {
            return java.util.Optional.empty();
        }
        if (standardTags.contains(display)) {
            return java.util.Optional.of(display);
        }
        String aliased = aliases.get(aliasKey(display));
        return aliased == null ? java.util.Optional.empty() : java.util.Optional.of(aliased);
    }

    public String promptText() {
        return promptText;
    }

    private List<TagDtos.TagCategory> buildCategories() {
        return List.of(
                category("语言入门", "顺序结构", "分支结构", "循环结构", "数组", "字符串（入门）", "结构体", "函数与递归"),
                category("字符串", "后缀自动机 SAM", "字典树 Trie", "AC 自动机", "KMP 算法", "后缀数组 SA", "后缀树", "有限状态自动机", "回文自动机 PAM", "Manacher 算法", "Lyndon 分解", "Z 函数", "后缀平衡树"),
                category("动态规划 DP", "背包 DP", "数位 DP", "区间 DP", "树形 DP", "轮廓线 DP", "线性 DP", "状压 DP"),
                category("搜索", "广度优先搜索 BFS", "深度优先搜索 DFS", "剪枝", "记忆化搜索", "启发式搜索", "迭代加深搜索", "启发式迭代加深搜索 IDA*", "Dancing Links", "爬山算法 Local search", "模拟退火", "随机调整", "遗传算法", "A* 算法", "折半搜索 meet in the middle", "梯度下降法"),
                category("数学", "信息论", "拉格朗日乘数法", "拉格朗日插值法", "单位根反演"),
                category("图论", "Kruskal 重构树", "网络流", "图论建模", "图遍历", "拓扑排序", "最短路", "生成树", "平面图", "最小环", "负权环", "连通块", "2-SAT", "平面图欧拉公式", "强连通分量", "Tarjan", "双连通分量", "欧拉回路", "差分约束", "仙人掌", "二分图", "一般图的最大匹配", "上下界网络流", "最小割", "费用流", "圆方树", "弦图", "Floyd 算法", "广义串并联图"),
                category("计算几何", "三维计算几何", "向量", "凸包", "叉积", "线段相交", "半平面交", "旋转卡壳", "极角排序", "平面几何", "闵可夫斯基和 Minkowski sum"),
                category("树形数据结构", "线段树", "并查集", "平衡树", "堆", "树状数组", "cdq 分治", "可并堆", "动态树 LCT", "树套树", "可持久化线段树", "可持久化", "整体二分", "K-D Tree", "李超线段树", "吉司机线段树 segment tree beats", "线段树合并", "二区间合并（猫树分治）", "KTT / Kinetic Tournament Tree"),
                category("博弈论", "博弈树", "Nim 积", "SG 函数"),
                category("线性数据结构", "单调队列", "颜色段均摊（珂朵莉树 ODT）", "前缀和", "栈", "队列", "分块", "ST 表", "差分", "链表", "单调栈", "哈希表"),
                category("多项式", "快速傅里叶变换 FFT", "快速数论变换 NTT", "快速沃尔什变换 FWT", "快速莫比乌斯变换 FMT", "Berlekamp-Massey(BM) 算法", "集合幂级数，子集卷积"),
                category("数论", "原根", "素数判断,质数,筛法", "最大公约数 gcd", "扩展欧几里德算法", "不定方程", "进制", "中国剩余定理 CRT", "莫比乌斯反演", "逆元", "Lucas 定理", "类欧几里得算法", "调和级数", "欧拉降幂", "Stern-Brocot 树", "整除分块", "Dirichlet 卷积", "大步小步算法 BSGS", "二次剩余", "Bézout 定理", "杜教筛", "欧拉函数", "筛法"),
                category("基础算法", "模拟", "贪心", "递推", "倍增", "二分", "递归", "枚举", "分治", "排序", "STL"),
                category("动态规划优化", "优先队列", "矩阵加速", "斜率优化", "状态合并", "凸完全单调性（wqs 二分）", "四边形不等式", "DP 套 DP", "动态 DP", "决策单调性", "整体转移", "斜率维护技巧 slope trick"),
                category("树论", "点分治", "树上启发式合并", "树的遍历", "最近公共祖先 LCA", "树的直径", "树链剖分", "虚树", "基环树", "动态树分治", "Prüfer 序列", "全局平衡二叉树", "树的重心"),
                category("群论", "置换", "Pólya 定理"),
                category("组合数学", "排列组合", "二项式定理", "康托展开", "鸽笼原理", "容斥原理", "Fibonacci 数列", "Catalan 数", "Stirling 数", "生成函数", "Dilworth 定理", "拉格朗日反演", "杨表"),
                category("概率论", "期望", "概率生成函数", "随机游走 Markov Chain", "鞅的停时定理")
        );
    }

    private TagDtos.TagCategory category(String name, String... tags) {
        return new TagDtos.TagCategory(name, List.of(tags));
    }

    private Map<String, String> buildAliases() {
        Map<String, String> map = new LinkedHashMap<>();
        standardTags.forEach(tag -> map.put(aliasKey(tag), tag));
        alias(map, "广度优先搜索 BFS", "BFS", "广搜", "宽搜");
        alias(map, "深度优先搜索 DFS", "DFS", "深搜");
        alias(map, "启发式迭代加深搜索 IDA*", "IDA", "IDA Star", "IDA*");
        alias(map, "A* 算法", "A Star", "A*");
        alias(map, "折半搜索 meet in the middle", "meet in middle", "MITM", "折半枚举");
        alias(map, "背包 DP", "背包", "背包问题");
        alias(map, "数位 DP", "数位动态规划");
        alias(map, "区间 DP", "区间动态规划");
        alias(map, "树形 DP", "树形动态规划");
        alias(map, "状压 DP", "状压", "状态压缩 DP");
        alias(map, "字典树 Trie", "Trie", "前缀树", "字典树");
        alias(map, "KMP 算法", "KMP");
        alias(map, "后缀自动机 SAM", "SAM");
        alias(map, "后缀数组 SA", "SA");
        alias(map, "回文自动机 PAM", "PAM", "回文树");
        alias(map, "Manacher 算法", "Manacher", "马拉车");
        alias(map, "Z 函数", "Z算法", "Z Algorithm");
        alias(map, "强连通分量", "SCC", "强连通", "强联通分量");
        alias(map, "双连通分量", "BCC", "双连通", "双联通分量");
        alias(map, "2-SAT", "2sat");
        alias(map, "最短路", "最短路径");
        alias(map, "生成树", "MST", "最小生成树");
        alias(map, "网络流", "最大流", "Dinic", "ISAP");
        alias(map, "费用流", "MCMF", "最小费用最大流");
        alias(map, "最近公共祖先 LCA", "LCA");
        alias(map, "树链剖分", "HLD", "重链剖分", "树剖");
        alias(map, "树上启发式合并", "DSU on tree");
        alias(map, "树状数组", "BIT", "Fenwick Tree");
        alias(map, "并查集", "DSU", "Union Find");
        alias(map, "线段树", "Segment Tree");
        alias(map, "动态树 LCT", "LCT", "Link-Cut Tree");
        alias(map, "K-D Tree", "KD Tree", "K-D 树");
        alias(map, "吉司机线段树 segment tree beats", "Segment Tree Beats", "线段树 Beats");
        alias(map, "颜色段均摊（珂朵莉树 ODT）", "ODT", "珂朵莉树", "老司机树");
        alias(map, "ST 表", "RMQ", "Sparse Table");
        alias(map, "哈希表", "Hash", "哈希", "散列表");
        alias(map, "快速傅里叶变换 FFT", "FFT");
        alias(map, "快速数论变换 NTT", "NTT");
        alias(map, "快速沃尔什变换 FWT", "FWT");
        alias(map, "快速莫比乌斯变换 FMT", "FMT");
        alias(map, "Berlekamp-Massey(BM) 算法", "BM", "Berlekamp Massey");
        alias(map, "最大公约数 gcd", "gcd", "GCD", "最大公因数");
        alias(map, "扩展欧几里德算法", "exgcd", "扩欧");
        alias(map, "中国剩余定理 CRT", "CRT", "中国剩余", "孙子定理");
        alias(map, "Lucas 定理", "Lucas", "卢卡斯定理");
        alias(map, "大步小步算法 BSGS", "BSGS");
        alias(map, "欧拉函数", "phi", "φ");
        alias(map, "莫比乌斯反演", "莫反");
        alias(map, "Bézout 定理", "Bézout", "贝祖定理", "裴蜀定理");
        alias(map, "二分", "二分查找");
        alias(map, "STL", "STL 容器", "C++ STL");
        alias(map, "排序", "排序算法");
        alias(map, "排列组合", "组合数", "排列与组合");
        alias(map, "鸽笼原理", "鸽巢原理", "抽屉原理");
        alias(map, "Catalan 数", "Catalan", "卡特兰数");
        alias(map, "Stirling 数", "Stirling", "斯特林数");
        alias(map, "Fibonacci 数列", "Fibonacci", "斐波那契");
        alias(map, "随机游走 Markov Chain", "Markov Chain", "马尔可夫链");
        return map;
    }

    private void alias(Map<String, String> map, String standard, String... values) {
        for (String value : values) {
            map.put(aliasKey(value), standard);
        }
    }

    private String cleanDisplay(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("\\s+", " ");
    }

    private String aliasKey(String value) {
        String normalized = Normalizer.normalize(cleanDisplay(value), Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT)
                .replace('，', ',')
                .replace('：', ':')
                .replace('（', '(')
                .replace('）', ')');
        return normalized.replaceAll("[\\s_\\-:/,，、()（）]+", "");
    }

    private String buildPromptText() {
        List<String> lines = new ArrayList<>();
        lines.add("tags 只能从以下标准二级标签中选择；一级分类不可作为标签；无法确定具体二级标签时不要输出该标签。");
        for (TagDtos.TagCategory category : categories) {
            lines.add(category.name() + "：" + String.join("、", category.tags()));
        }
        return String.join("\n", lines);
    }
}
