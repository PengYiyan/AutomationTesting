package org.example.aug;

import com.ibm.wala.classLoader.IClass;

/**
 * @program: lys_classic_task
 * @description: 辅助记录类等级的边
 * @author: Li Yongshao
 * @create: 2020-11-17 01:08
 */
public class ClassEdge {
    public IClass begin, end;

    public ClassEdge(IClass begin, IClass end) {
        this.begin = begin;
        this.end = end;
    }
}