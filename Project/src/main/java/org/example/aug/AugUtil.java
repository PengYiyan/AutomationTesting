package org.example.aug;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.ShrikeBTMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.cha.CHACallGraph;
import com.ibm.wala.types.annotations.Annotation;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * @program: lys_classic_task
 * @description: 辅助工具类
 * @author: Li Yongshao
 * @create: 2020-11-17 00:29
 */
public class AugUtil {
    private String path;
    private String[] changeInfo;
    private CHACallGraph cg;
    private ArrayList<MethodEdge> methodEdgePairs;
    private ArrayList<ClassEdge> classEdgePairs;
    private ArrayList<AugEntry> classRecord;
    private ArrayList<ShrikeBTMethod> selectedMethods;

    /**
     * @Description: 获取非自带类的图信息
     * @Author: Li Yongshao
     * @date: 2020/11/17
     */
    public void getDAG() {
        for (CGNode node : cg) {
            if (node.getMethod() instanceof ShrikeBTMethod) {
                ShrikeBTMethod method = (ShrikeBTMethod) node.getMethod();
                if ("Application".equals(method.getDeclaringClass().getClassLoader().toString())) {
/*                    String classInnerName = method.getDeclaringClass().getName().toString(); //类的内部表示
                    String signature = method.getSignature(); //方法签名*/

                    //记录所有类与各自的方法
                    boolean existFlag = false;
                    for (AugEntry augEntry : classRecord) {
                        if (augEntry.iClass.equals(method.getDeclaringClass())) {
                            existFlag = true;
                            augEntry.tryAddMethod(method);
                        }
                    }
                    if (!existFlag) {
                        AugEntry augEntry = new AugEntry(method.getDeclaringClass());
                        augEntry.tryAddMethod(method);
                        classRecord.add(augEntry);
                    }

//                  System.out.println(classInnerName+" "+signature);

                    //分别记录类、方法等级的边
                    Iterator<CGNode> pred = cg.getPredNodes(node); //pred记录了当前方法调用了哪些方法
                    while (pred.hasNext()) {
                        CGNode nd = pred.next();
                        if (nd.getMethod() instanceof ShrikeBTMethod) {
                            ShrikeBTMethod methodEnd = (ShrikeBTMethod) nd.getMethod();
                            if ("Application".equals(methodEnd.getDeclaringClass().getClassLoader().toString())) {
                                //记录类级的边
                                boolean classEdgeExistFlag = false;
                                for (ClassEdge classEdge : classEdgePairs) {
                                    if (classEdge.begin.equals(method.getDeclaringClass()) &&
                                            classEdge.end.equals(methodEnd.getDeclaringClass()))
                                        classEdgeExistFlag = true;
                                }
                                if (!classEdgeExistFlag) {
                                    classEdgePairs.add(new ClassEdge(method.getDeclaringClass(), methodEnd.getDeclaringClass()));
                                }
                                //记录方法级的边
                                boolean methodEdgeExistFlag = false;
                                for (MethodEdge methodEdge : methodEdgePairs) {
                                    if (methodEdge.begin.equals(method) && methodEdge.end.equals(methodEnd))
                                        methodEdgeExistFlag = true;
                                }
                                if (!methodEdgeExistFlag) {
                                    methodEdgePairs.add(new MethodEdge(method, methodEnd));
                                }
                            }
                        }
                    }

                }
            }
        }
    }

    /**
     * @Description: 从路径解析变更文件，存储到changeInfo数组中
     * @Author: Li Yongshao
     * @date: 2020/11/17
     */
    private void parseChangeInfo() throws IOException {
        File changeInfoFile = new File(path);
        assert changeInfoFile.exists();
        BufferedReader bufferedReader = new BufferedReader(new FileReader(changeInfoFile));
        String temp = null;
        StringBuffer sb = new StringBuffer();
        temp = bufferedReader.readLine();
        while (temp != null) {
            sb.append(temp + "\n");
            temp = bufferedReader.readLine();
        }
        this.changeInfo = sb.toString().split("\n");
    }

    /**
     * @Description: 判断一个方法是否属于测试方法
     * @Author: Li Yongshao
     * @date: 2020/11/17
     */
    private boolean judTestMethod(ShrikeBTMethod method) {
        //找到方法的所有注解，比较是否有junit下的单元测试
        Collection<Annotation> annotations = method.getAnnotations();
        boolean isTestMethodFlag = false;
        for (Annotation annotation : annotations) {
            String test = annotation.getType().getName().toString();
            if (annotation.getType().getName().toString().equals("Lorg/junit/Test")) {
                isTestMethodFlag = true;
                break;
            }
        }
        return isTestMethodFlag;
    }

    /**
     * @Description: 从selectedMethods输出被选择的测试方法
     * @Author: Li Yongshao
     * @date: 2020/11/17
     */
    private void outputSelectedMethodFile(String typeOfLevel) throws IOException {
        File selectedMethodFile = new File("./selection-" + typeOfLevel + ".txt");
        if (!selectedMethodFile.exists()) {
            selectedMethodFile.createNewFile();
        }
        FileOutputStream fileOutputStream = new FileOutputStream(selectedMethodFile);
        PrintStream printStream = new PrintStream(fileOutputStream);
        for (ShrikeBTMethod method : selectedMethods) {
            String classInnerName = method.getDeclaringClass().getName().toString();
            String signature = method.getSignature();
            printStream.println(classInnerName + " " + signature);
        }
    }

    /**
     * @Description: 根据变更记录类级选取变更生产类调用的测试类下所有方法
     * @Author: Li Yongshao
     * @date: 2020/11/17
     */
    public void classLevelSelect() throws IOException {
        ArrayList<String> changeClasses = new ArrayList<String>();
        ArrayList<IClass> affectedClasses = new ArrayList<IClass>();
        for (String info : changeInfo) {
            String[] infos = info.split("\\s");
            assert infos.length == 2;
            if (changeClasses.indexOf(infos[0]) == -1) {
                changeClasses.add(infos[0]);
            }
        }
        //找到变更的类调用的测试类
        for (String classBegin : changeClasses) {
            for (ClassEdge classEdge : classEdgePairs) {
                //匹配类名
                if (classEdge.begin.getName().toString().equals(classBegin)) {
                    affectedClasses.add(classEdge.end);
                }
            }
        }
        //将受影响的测试类下的所有方法加入选择结果中
        for (IClass iClass : affectedClasses) {
            AugEntry affectedClass = null;
            for (AugEntry augEntry : classRecord) {
                if (augEntry.iClass.equals(iClass)) {
                    affectedClass = augEntry;
                    break;
                }
            }
            assert affectedClass != null;
            for (ShrikeBTMethod method : affectedClass.methods) {
                if (judTestMethod(method)) {
                    if (selectedMethods.indexOf(method) == -1) {
                        selectedMethods.add(method);
                    }
                }
            }
        }
        //输出
        outputSelectedMethodFile("class");
    }

    /**
     * @Description: 根据变更记录直接选取变更生产方法调用的属于测试类的方法
     * @Author: Li Yongshao
     * @date: 2020/11/17
     */
    public void methodLevelSelect() throws IOException {
        for (String info : changeInfo) {
            String[] infos = info.split("\\s");
            assert infos.length == 2;
            for (MethodEdge methodEdge : methodEdgePairs) {
                //匹配类名与方法签名
                if (methodEdge.begin.getDeclaringClass().getName().toString().equals(infos[0]) &&
                        methodEdge.begin.getSignature().equals(infos[1])) {
                    if (judTestMethod(methodEdge.end)) {
                        if (selectedMethods.indexOf(methodEdge.end) == -1) {
                            selectedMethods.add(methodEdge.end);
                        }
                    }
                }
            }
        }
        //输出
        outputSelectedMethodFile("method");
    }

    /**
     * @Description: 将两个等级记录edge的ArrayList的内容遍历输出成dot文件
     * @Author: Li Yongshao
     * @date: 2020/11/17
     */
    public void outputDotFile() throws IOException {
        String dotName = "MoreTriangle";
        String dotClassFileBegin = "digraph " + dotName + "_class {\n";
        String dotMethodFileBegin = "digraph " + dotName + "_method {\n";
        String dotClassFileContent = "";
        String dotMethodFileContent = "";
        String dotFileEnd = "}";
        //遍历类级
        for (ClassEdge classEdge : classEdgePairs) {
            //获取类的内部表示
            dotClassFileContent += "\t\"" + classEdge.begin.getName().toString() + "\" -> \"" +
                    classEdge.end.getName().toString() + "\";\n";
        }
        File dotClassFile = new File("./dotFiles/class-" + dotName.toUpperCase() + ".dot");
        if (!dotClassFile.exists()) {
            dotClassFile.createNewFile();
        }
        FileOutputStream outputStream = new FileOutputStream(dotClassFile);
        PrintStream printStream = new PrintStream(outputStream);
        printStream.print(dotClassFileBegin + dotClassFileContent + dotFileEnd);

        //遍历方法级
        for (MethodEdge methodEdge : methodEdgePairs) {
            //获取方法签名
            dotMethodFileContent += "\t\"" + methodEdge.begin.getSignature() + "\" -> \"" +
                    methodEdge.end.getSignature() + "\";\n";
        }
        File dotMethodFile = new File("./dotFiles/method-" + dotName.toUpperCase() + ".dot");
        if (!dotMethodFile.exists()) {
            dotMethodFile.createNewFile();
        }
        outputStream = new FileOutputStream(dotMethodFile);
        printStream = new PrintStream(outputStream);
        printStream.print(dotMethodFileBegin + dotMethodFileContent + dotFileEnd);
    }

    /**
     * @Description: 初始化ArrayList并负责调用初始化的DAG信息获取、路径解析
     * @Author: Li Yongshao
     * @date: 2020/11/17
     */
    public AugUtil(CHACallGraph cg, String change_info) throws IOException {
        this.cg = cg;
        this.path = change_info;
        this.classEdgePairs = new ArrayList<ClassEdge>();
        this.methodEdgePairs = new ArrayList<MethodEdge>();
        this.classRecord = new ArrayList<AugEntry>();
        this.selectedMethods = new ArrayList<ShrikeBTMethod>();
        getDAG();
        //dotFile输出完成后弃用该方法
        outputDotFile();
        parseChangeInfo();
    }
}