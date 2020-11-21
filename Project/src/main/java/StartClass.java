import auxClass.AuxClass;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.cha.CHACallGraph;
import com.ibm.wala.ipa.callgraph.impl.AllApplicationEntrypoints;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.config.AnalysisScopeReader;

import java.io.File;
import java.io.IOException;

/**
 * 该类将用于初始化
 */
public class StartClass {
    public static AnalysisScope scope;
    public static CHACallGraph cg;
    private static int selectType; //0表示类级，1表示方法级
    private static String target,change_info;//加载目标和变更信息

    /**
     * 域的初始化
     * @throws IOException
     * @throws InvalidClassFileException
     */
    public static void loadClass() throws IOException, InvalidClassFileException {
        ClassLoader classLoader = StartClass.class.getClassLoader();

        scope = AnalysisScopeReader.readJavaScope(
                "scope.txt",
                new File("exclusion.txt"),
                classLoader
        );
//        File file = new File("scope.txt");
//        scope.addClassFileToScope(ClassLoaderReference.Application, file);
    }

    /**
     * 将target目录中的class文件递归地加入域中
     * @param path
     * @throws InvalidClassFileException
     */
    public static void initScope(String path) throws InvalidClassFileException{
        File file = new File(path);
        if(file.exists()){
            File[] files = file.listFiles();
            if(!(files == null || files.length == 0)){
                for(File sub : files){
                    if(sub.isDirectory()){
                        //如果是文件夹，就递归调用
                        initScope(sub.getAbsolutePath());
                    }else if(sub.getName().endsWith(".class")){ //挑选出类文件
                        scope.addClassFileToScope(ClassLoaderReference.Application,sub);
                    }
                }
            }
        }else {
            System.out.println("Can't find the file");
        }
    }

    /**
     * 生成进入点并初始化图
     * @throws ClassHierarchyException
     * @throws CancelException
     */
    public static void initGraph() throws ClassHierarchyException,CancelException{
        // 1.生成类层次关系对象
        //AnalysisScope scope = null;
        ClassHierarchy cha = ClassHierarchyFactory.makeWithRoot(scope);

        // 2.生成进入点
        Iterable<Entrypoint> eps = new AllApplicationEntrypoints(scope,cha);

        // 3.利用CHA算法构建调用图
        //README更新
        //CallGraph cg = new CHACallGraph(cha);
        //((CHACallGraph) cg).init(eps);
        cg = new CHACallGraph(cha);
        cg.init(eps);
    }

    /**
     * 解析输入的参数
     * @param args 命令行 -m/c <project_target> <change_info>
     */
    public static void parseArg(String[] args){
        //根据输入m/c选择方法级或者类级
        if(args[0].equals("-c"))
            selectType = 0;
        else if(args[0].equals("-m"))
            selectType = 1;
        //加载对应的路径参数
        target = args[1];
        change_info = args[2];
    }

    public static void main(String args[]) throws IOException, InvalidClassFileException, ClassHierarchyException, CancelException {

        //生成分析域对象scope
        loadClass();
        //处理输入的参数args
        if(args.length == 0){//用于调试的情况，选择一个数据进行测试
            String[] temps = new String[]{"-c",
                    "C:\\_18SE\\_学习\\大三\\自动化测试\\Final大作业\\实践\\_Git提交部分\\AutomationTesting\\Project\\ClassicAutomatedTesting\\1-ALU\\target",
                    "C:\\_18SE\\_学习\\大三\\自动化测试\\Final大作业\\实践\\_Git提交部分\\AutomationTesting\\Project\\ClassicAutomatedTesting\\1-ALU\\data\\change_info.txt"};
            parseArg(temps);
        }else
            parseArg(args);
        initScope(target);
        initGraph();
        //System.out.println(scope);
        //System.out.println(cg);
        if(selectType == 0){//类级别
            new AuxClass(cg,change_info).classLevelSelect();
        }else if(selectType == 1){//方法级别
            new AuxClass(cg,change_info).methodLevelSelect();
        }
    }
}