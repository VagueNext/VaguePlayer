package com.vagueplayer.music.utils

/**
 * Embedded Pinyin Data with Unicode Escapes to prevent encoding corruption.
 * Guarantees correct key mapping regardless of source file charset.
 */
object PinyinData {
    // Keys are Unicode Escaped: '再' -> '\u518d'
    val map: Map<Char, Char> = mapOf(
        // Z Group
        '\u518d' to 'Z', // 再
        '\u5468' to 'Z', // 周
        '\u5f20' to 'Z', // 张
        '\u8d75' to 'Z', // 赵
        '\u90d1' to 'Z', // 郑
        '\u6731' to 'Z', // 朱
        '\u5e84' to 'Z', // 庄
        '\u90b9' to 'Z', // 邹
        '\u66fe' to 'Z', // 曾
        '\u7ae0' to 'Z', // 章
        '\u949f' to 'Z', // 钟
        '\u81ea' to 'Z', // 自
        '\u505a' to 'Z', // 做
        '\u8d70' to 'Z', // 走
        '\u6700' to 'Z', // 最
        '\u771f' to 'Z', // 真
        '\u6b63' to 'Z', // 正
        '\u4e2d' to 'Z', // 中
        '\u77e5' to 'Z', // 知
        '\u53ea' to 'Z', // 只

        // B Group
        '\u51b0' to 'B', // 冰
        '\u522b' to 'B', // 别
        '\u534a' to 'B', // 半
        '\u4e0d' to 'B', // 不
        '\u767d' to 'B', // 白
        '\u767e' to 'B', // 百
        '\u5305' to 'B', // 包
        '\u5317' to 'B', // 北
        '\u672c' to 'B', // 本
        '\u5fc5' to 'B', // 必
        '\u5e73' to 'B', // 平
        '\u53d8' to 'B', // 变
        '\u8868' to 'B', // 表
        '\u5175' to 'B', // 兵
        '\u5e76' to 'B', // 并
        '\u8865' to 'B', // 补
        '\u6b65' to 'B', // 步

        // N Group
        '\u51dd' to 'N', // 凝
        '\u4f60' to 'N', // 你
        '\u90a3' to 'N', // 那
        '\u5357' to 'N', // 南
        '\u5185' to 'N', // 内
        '\u80fd' to 'N', // 能
        '\u5e74' to 'N', // 年
        '\u725b' to 'N', // 牛
        '\u5973' to 'N', // 女
        '\u96be' to 'N', // 难
        '\u5462' to 'N', // 呢

        // G Group
        '\u521a' to 'G', // 刚
        '\u9ad8' to 'G', // 高
        '\u4e2a' to 'G', // 个
        '\u5404' to 'G', // 各
        '\u7ed9' to 'G', // 给
        '\u66f4' to 'G', // 更
        '\u5de5' to 'G', // 工
        '\u516c' to 'G', // 公
        '\u5171' to 'G', // 共
        '\u53e4' to 'G', // 古
        '\u5173' to 'G', // 关
        '\u5149' to 'G', // 光
        '\u5e7f' to 'G', // 广
        '\u56fd' to 'G', // 国
        '\u679c' to 'G', // 果
        '\u8fc7' to 'G', // 过

        // S Group
        '\u8bf4' to 'S', // 说
        '\u662f' to 'S', // 是
        '\u4e0a' to 'S', // 上
        '\u5c71' to 'S', // 山
        '\u5c11' to 'S', // 少
        '\u8c01' to 'S', // 谁
        '\u8eab' to 'S', // 身
        '\u6df1' to 'S', // 深
        '\u58f0' to 'S', // 声
        '\u751f' to 'S', // 生
        '\u5e08' to 'S', // 师
        '\u5341' to 'S', // 十
        '\u65f6' to 'S', // 时
        '\u5b9e' to 'S', // 实
        '\u8bd5' to 'S', // 试
        '\u89c6' to 'S', // 视
        '\u6536' to 'S', // 收
        '\u624b' to 'S', // 手
        '\u9996' to 'S', // 首
        '\u4e66' to 'S', // 书
        '\u672f' to 'S', // 术
        '\u6c34' to 'S', // 水
        '\u56db' to 'S', // 四
        '\u601d' to 'S', // 思
        '\u6b7b' to 'S', // 死
        '\u65af' to 'S', // 斯
        '\u4f3c' to 'S', // 似
        '\u79c1' to 'S', // 私
        '\u5b59' to 'S', // 孙
        '\u6c88' to 'S', // 沈
        '\u5b8b' to 'S', // 宋
        '\u77f3' to 'S', // 石
        '\u53f2' to 'S', // 史
        '\u90b5' to 'S', // 邵

        // Q Group
        '\u4e03' to 'Q', // 七
        '\u94b1' to 'Q', // 钱
        '\u8d77' to 'Q', // 起
        '\u6c14' to 'Q', // 气
        '\u5343' to 'Q', // 千
        '\u524d' to 'Q', // 前
        '\u5f3a' to 'Q', // 强
        '\u4eb2' to 'Q', // 亲
        '\u6e05' to 'Q', // 清
        '\u60c5' to 'Q', // 情
        '\u8bf7' to 'Q', // 请
        '\u5168' to 'Q', // 全
        '\u786e' to 'Q', // 确
        '\u53bb' to 'Q', // 去
        '\u90b1' to 'Q', // 邱
        '\u79e6' to 'Q', // 秦
        '\u4e54' to 'Q', // 乔

        // Y Group
        '\u4e00' to 'Y', // 一
        '\u6709' to 'Y', // 有
        '\u4e5f' to 'Y', // 也
        '\u8981' to 'Y', // 要
        '\u4ee5' to 'Y', // 以
        '\u5df2' to 'Y', // 已
        '\u610f' to 'Y', // 意
        '\u56e0' to 'Y', // 因
        '\u97f3' to 'Y', // 音
        '\u82f1' to 'Y', // 英
        '\u7528' to 'Y', // 用
        '\u7531' to 'Y', // 由
        '\u4e0e' to 'Y', // 与
        '\u5143' to 'Y', // 元
        '\u5458' to 'Y', // 员
        '\u539f' to 'Y', // 原
        '\u8fdc' to 'Y', // 远
        '\u9662' to 'Y', // 院
        '\u6708' to 'Y', // 月
        '\u6837' to 'Y', // 样
        '\u773c' to 'Y', // 眼
        '\u6768' to 'Y', // 杨
        '\u53f6' to 'Y', // 叶
        '\u960e' to 'Y', // 阎
        '\u4f59' to 'Y', // 余
        '\u59da' to 'Y', // 姚
        '\u5c39' to 'Y', // 尹
        '\u6613' to 'Y', // 易

        // Other Surnames & Commons
        '\u674e' to 'L', // 李
        '\u738b' to 'W', // 王
        '\u5218' to 'L', // 刘
        '\u9648' to 'C', // 陈
        '\u9ec4' to 'H', // 黄
        '\u5434' to 'W', // 吴
        '\u5f90' to 'X', // Xu
        '\u80e1' to 'H', // Hu
        '\u6797' to 'L', // Lin
        '\u4f55' to 'H', // He
        '\u90ed' to 'G', // Guo
        '\u9a6c' to 'M', // Ma
        '\u7f57' to 'L', // Luo
        '\u6881' to 'L', // Liang
        '\u8c22' to 'X', // Xie
        '\u97e9' to 'H', // Han
        '\u5510' to 'T', // Tang
        '\u51af' to 'F', // Feng
        '\u4e8e' to 'Y', // Yu
        '\u8463' to 'D', // Dong
        '\u8427' to 'X', // Xiao
        '\u7a0b' to 'C', // Cheng
        '\u66f9' to 'C', // Cao
        '\u8881' to 'Y', // Yuan
        '\u9093' to 'D', // Deng
        '\u8bb8' to 'X', // Xu
        '\u5085' to 'F', // Fu
        '\u5f6d' to 'P', // Peng
        '\u5415' to 'L', // Lu
        '\u82cf' to 'S', // Su
        '\u5362' to 'L', // Lu
        '\u848b' to 'J', // Jiang
        '\u8521' to 'C', // Cai
        '\u8d3e' to 'J', // Jia
        '\u4e01' to 'D', // Ding
        '\u9b4f' to 'W', // Wei
        '\u859b' to 'X', // Xue
        '\u6f58' to 'P', // Pan
        '\u675c' to 'D', // Du
        '\u6234' to 'D', // Dai
        '\u590f' to 'X', // Xia
        '\u6c6a' to 'W', // Wang
        '\u7530' to 'T', // Tian
        '\u4efb' to 'R', // Ren (Ren)
        '\u59dc' to 'J', // Jiang
        '\u8303' to 'F', // Fan
        '\u65b9' to 'F', // Fang
        '\u8c2d' to 'T', // Tan
        '\u5ed6' to 'L', // Liao
        '\u718a' to 'X', // Xiong
        '\u91d1' to 'J', // Jin
        '\u9646' to 'L', // Lu
        '\u90dd' to 'H', // Hao
        '\u5b54' to 'K', // Kong
        '\u5d14' to 'C', // Cui
        '\u5eb7' to 'K', // Kang
        '\u6bdb' to 'M', // Mao
        '\u6c5f' to 'J', // Jiang
        '\u987e' to 'G', // Gu
        '\u4faf' to 'H', // Hou
        '\u5b5f' to 'M', // Meng
        '\u9f99' to 'L', // Long
        '\u4e07' to 'W', // Wan
        '\u6bb5' to 'D', // Duan
        '\u96f7' to 'L', // Lei
        '\u6c64' to 'T', // Tang
        '\u9ece' to 'L', // Li
        '\u5e38' to 'C', // Chang
        '\u6b66' to 'W', // Wu
        '\u8d3a' to 'H', // He
        '\u8d56' to 'L', // Lai
        '\u9f9a' to 'G', // Gong
        '\u6587' to 'W'  // Wen
    )
    
    fun getPinyin(c: Char): Char? {
        return map[c]
    }
}
