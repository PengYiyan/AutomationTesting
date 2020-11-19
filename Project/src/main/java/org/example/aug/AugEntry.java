package org.example.aug;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.ShrikeBTMethod;

import java.util.ArrayList;

/**
 * @program: lys_classic_task
 * @description: 辅助类，记录类名和类下的方法
 * @author: Li Yongshao
 * @create: 2020-11-17 00:53
 */
public class AugEntry {
    public IClass iClass;
    public ArrayList<ShrikeBTMethod> methods;

    public void tryAddMethod(ShrikeBTMethod method) {
        if (this.methods.indexOf(method) == -1)
            this.methods.add(method);
    }

    public AugEntry(IClass iClass) {
        this.iClass = iClass;
        this.methods = new ArrayList<ShrikeBTMethod>();
    }
}