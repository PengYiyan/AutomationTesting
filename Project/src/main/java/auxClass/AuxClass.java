package auxClass;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.ShrikeBTMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.cha.CHACallGraph;
import com.ibm.wala.types.annotations.Annotation;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

/**
 * 用于完成后续的处理
 */
public class AuxClass {
    private String path;
    private String[] changeInfo;
    private CHACallGraph cg;
    private ArrayList<MethodEdge> methodEdges;
    private ArrayList<ClassEdge> classEdges;
    private ArrayList<AuxEntry> classRecord;
    private ArrayList<ShrikeBTMethod> selectedMethods;

    /**
     * 获取非自带类的图信息
     */
    public void getDAG(){
        // 4.遍历cg中所有的节点
        for(CGNode node:cg){
            //node中包含了很多信息，包括类加载器、方法信息等
            if(node.getMethod() instanceof ShrikeBTMethod){
                // node.getMethod()返回一个比较泛化的IMethod实例，不能获取到我们想要的信息
                // 一般地，本项目中所有和业务逻辑相关的方法都是ShrikeBTMethod对象
                ShrikeBTMethod method = (ShrikeBTMethod)node.getMethod();
                // 使用Primordial类加载器加载的类都属于Java原生类，一般不关心
                if("Application".equals(method.getDeclaringClass().getClassLoader().toString())){

                    //记录所有类与各自的方法
                    boolean existFlag = false;
                    for (AuxEntry auxEntry : classRecord){
                        if(auxEntry.iClass.equals(method.getDeclaringClass())){
                            existFlag = true;
                            auxEntry.tryAddMethod(method);
                        }
                    }
                    if(!existFlag){
                        AuxEntry auxEntry = new AuxEntry(method.getDeclaringClass());
                        auxEntry.tryAddMethod(method);
                        classRecord.add(auxEntry);
                    }

                    //记录类和方法的边
                    Iterator<CGNode> pred = cg.getPredNodes(node); //找前驱节点
                    while(pred.hasNext()){
                        CGNode nextNode = pred.next();
                        if(nextNode.getMethod() instanceof ShrikeBTMethod){
                            ShrikeBTMethod methodEnd = (ShrikeBTMethod)nextNode.getMethod();
                            //这里不小心把methodEnd敲成method了，导致后续null的报错
                            if("Application".equals(methodEnd.getDeclaringClass().getClassLoader().toString())){
                                //记录类级的边
                                boolean classEdgeExists = false;
                                for(ClassEdge classEdge:classEdges){
                                    //进行头尾对比
                                    if(classEdge.begin.equals(method.getDeclaringClass()) && classEdge.end.equals(methodEnd.getDeclaringClass())){
                                        classEdgeExists = true;
                                    }
                                }
                                if(!classEdgeExists){
                                    classEdges.add(new ClassEdge(method.getDeclaringClass(),methodEnd.getDeclaringClass()));
                                }
                                //记录方法级的边
                                boolean methodEdgeExists = false;
                                for(MethodEdge methodEdge:methodEdges){
                                    if(methodEdge.begin.equals(method) && methodEdge.end.equals(methodEnd)){
                                        methodEdgeExists = true;
                                    }
                                }
                                if(!methodEdgeExists){
                                    methodEdges.add(new MethodEdge(method,methodEnd));
                                }
                            }
                        }
                    }
                }
            }else {
                //System.out.println(String.format("'%s'不是一个ShrikeBTMethod：%s",node.getMethod(),node.getMethod().getClass()));
            }
        }
    }

    /**
     * 将变更信息存储到changeInfo中
     * @throws IOException
     */
    public void parseChangeInfo() throws IOException{
        File changeInfoFile = new File(path);
        assert changeInfoFile.exists();
        BufferedReader bufferedReader = new BufferedReader(new FileReader(changeInfoFile));
        String temp = null;
        StringBuffer stringBuffer = new StringBuffer();
        temp = bufferedReader.readLine();
        while(temp!=null){
            stringBuffer.append(temp + "\n");
            temp = bufferedReader.readLine();
        }
        this.changeInfo = stringBuffer.toString().split("\n");
    }

    /**
     * 输出被选择的测试方法
     * @param type
     * @throws IOException
     */

    public void outputSelectedRes(String type) throws IOException{
        File selectedMethodFile = new File("./selection-" + type + ".txt");
        if(!selectedMethodFile.exists()){
            selectedMethodFile.createNewFile();
        }
        FileOutputStream fileOutputStream = new FileOutputStream(selectedMethodFile);
        PrintStream printStream = new PrintStream(fileOutputStream);
        for(ShrikeBTMethod method:selectedMethods){
            // 获取声明该方法的类的内部标识
            String classInnerName = method.getDeclaringClass().getName().toString();
            // 获取方法签名
            String signature = method.getSignature();
            printStream.println(classInnerName + " " + signature);
        }
    }

    /**
     * 判断一个方法是否属于测试方法
     * @param method
     * @你
     */
    public boolean judgeTestMethod(ShrikeBTMethod method){
        //找到方法的注解，比较是否存在junit单元下的单元测试
        Collection<Annotation> annotations = method.getAnnotations();
        boolean isTestMethod = false;
        for (Annotation annotation : annotations){
            String test = annotation.getType().getName().toString();
            if(test.equals("Lorg/junit/Test")){
                isTestMethod = true;
                break;
            }
        }
        return isTestMethod;
    }

    /**
     * 选取变更类调用的测试类下所有方法
     * @throws IOException
     */

    public void classLevelSelect() throws IOException{
        ArrayList<String> changeClasses = new ArrayList<String>();
        ArrayList<IClass> affectedClasses = new ArrayList<IClass>();
        for(String info: changeInfo){
            String[] infos = info.split("\\s");
            assert infos.length == 2;
            if(changeClasses.indexOf(infos[0]) == -1){
                changeClasses.add(infos[0]);
            }
        }
        //找到变更的类调用的测试类
        for (String classCaller: changeClasses){
            for (ClassEdge classEdge:classEdges){
                //匹配类名
                if(classEdge.begin.getName().toString().equals(classCaller)){
                    affectedClasses.add(classEdge.end);//添加被调用的测试类
                }
            }
        }
        //将受影响的测试类下所有的方法都添加到选择的结果集中
        for(IClass iClass:affectedClasses){
            AuxEntry affectedClass = null;
            for(AuxEntry auxEntry:classRecord){
                if(auxEntry.iClass.equals(iClass)){
                    affectedClass = auxEntry;
                    break;
                }
            }
            //System.out.println(affectedClass);
            //System.out.println(affectedClass == null);
            assert affectedClass != null;
            for(ShrikeBTMethod method : affectedClass.methods){
                if(judgeTestMethod(method)){
                    if(selectedMethods.indexOf(method) == -1) //如果还未添加过该方法，添加
                        selectedMethods.add(method);
                }
            }
        }
        outputSelectedRes("class");
    }

    public void getClosure(ArrayList<String> changeInfoArray){
        ArrayList<String> closure = new ArrayList<String>();
        for(String info:changeInfoArray){
            String[] infos = info.split("\\s");
            assert infos.length == 2;
            for(MethodEdge methodEdge:methodEdges){
                if(methodEdge.begin.getDeclaringClass().getName().toString().equals(infos[0]) && methodEdge.begin.getSignature().equals(infos[1])){
                    if(selectedMethods.indexOf(methodEdge.end) == -1){
                        selectedMethods.add(methodEdge.end);
                        String methodString = methodEdge.end.getDeclaringClass().getName().toString() + " " + methodEdge.end.getSignature();
                        if(changeInfoArray.indexOf(methodString)==-1){
                            closure.add(methodString);
                        }
                    }
                }
            }
        }
        //如果没有新增方法，说明闭包生成已经结束
        if(closure.size() == 0){

        }else {//否则将已经有的闭包加入变更信息中，继续递归
            changeInfoArray.addAll(closure);
            getClosure(changeInfoArray);
        }
    }

    /**
     * 选取变更方法调用的属于测试类的所有方法
     * @throws IOException
     */
    public void methodLevelSelect() throws IOException{
        //考虑到方法中的依赖传递问题
        ArrayList<String> changeInfoArray = new ArrayList<String>(Arrays.asList(changeInfo));
        getClosure(changeInfoArray);

//        for (String info:changeInfo){
//            String[] infos = info.split("\\s");
//            assert infos.length == 2;
//            for(MethodEdge methodEdge:methodEdges){
//                if(methodEdge.begin.getDeclaringClass().getName().toString().equals(infos[0]) && methodEdge.begin.getSignature().equals(infos[1])){
//                    if(judgeTestMethod(methodEdge.end)){
//                        if(selectedMethods.indexOf(methodEdge.end) == -1){
//                            selectedMethods.add(methodEdge.end);
//                        }
//                    }
//                }
//            }
//        }
        //更新测试方法
        ArrayList<ShrikeBTMethod> tempMethods = new ArrayList<ShrikeBTMethod>();
        for(ShrikeBTMethod method:selectedMethods){
            if(judgeTestMethod(method)){
                tempMethods.add(method);
            }
        }
        selectedMethods = tempMethods;

        outputSelectedRes("method");
    }

    public void outputDotFile() throws IOException{
        String dotName = "MoreTriangle";
        String dotClassBegin = "digraph" + " " + dotName + "_class {\n";
        String dotMethodBegin = "digraph" + " " + dotName + "_method {\n";
        String dotClassContent = "";
        String dotMethodContent = "";
        String dotEnd = "}";

        //遍历类级
        for(ClassEdge classEdge:classEdges){
            //获取类的内部表示，caller指向callee
            dotClassContent += "\t\"" + classEdge.begin.getName().toString() + "\" -> \"" +
                    classEdge.end.getName().toString() + "\";\n";
        }
        //生成点图
        File dotClassFile = new File("./dotFiles/class-" + dotName.toUpperCase() + ".dot");
        if(!dotClassFile.exists()){
            dotClassFile.createNewFile();
        }
        FileOutputStream outputStream = new FileOutputStream(dotClassFile);
        PrintStream printStream = new PrintStream(outputStream);
        printStream.print(dotClassBegin + dotClassContent + dotEnd);

        //遍历方法级
        for(MethodEdge methodEdge:methodEdges){
            //获取方法签名
            dotMethodContent += "\t\"" + methodEdge.begin.getSignature().toString() + "\" -> \"" +
                    methodEdge.end.getSignature().toString() + "\";\n";
        }
        File dotMethodFile = new File("./dotFiles/method-" + dotName.toUpperCase() + ".dot");
        if(!dotMethodFile.exists()){
            dotMethodFile.createNewFile();
        }
        outputStream = new FileOutputStream(dotMethodFile);
        printStream = new PrintStream(outputStream);
        printStream.println(dotMethodBegin + dotMethodContent + dotEnd);
    }

    public AuxClass(CHACallGraph cg,String change_info) throws IOException{
        this.cg = cg;
        this.path = change_info;
        this.classEdges = new ArrayList<ClassEdge>();
        this.methodEdges = new ArrayList<MethodEdge>();
        this.classRecord = new ArrayList<AuxEntry>();
        this.selectedMethods = new ArrayList<ShrikeBTMethod>();
        getDAG();
        //输出.dot文件，要根据具体使用情况修改参数
        //outputDotFile();
        parseChangeInfo();
    }
}
