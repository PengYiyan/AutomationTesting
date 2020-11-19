package org.example.aug;

import com.ibm.wala.classLoader.ShrikeBTMethod;

/**
 * @program: lys_classic_task
 * @description: 辅助存储方法等级的边
 * @author: Li Yongshao
 * @create: 2020-11-17 01:13
 */
public class MethodEdge {
    public ShrikeBTMethod begin, end;

    public MethodEdge(ShrikeBTMethod begin, ShrikeBTMethod end) {
        this.begin = begin;
        this.end = end;
    }
}