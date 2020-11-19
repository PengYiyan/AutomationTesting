package org.example;

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
import org.example.aug.AugUtil;

import java.io.File;
import java.io.IOException;

/**
 * @program: lys_classic_task
 * @description: 处理路径与域的构建
 * @author: Li Yongshao
 * @create: 2020-11-16 23:09
 */
public class InitUtil {
    public static AnalysisScope scope;
    public static CHACallGraph cg;
    private static int selectType; //0 class; 1 method
    private static String target, change_info;


    /**
     * @Description: 初始化域
     * @Author: Li Yongshao
     * @date: 2020/11/16
     */
    private static void init() throws IOException {
        File exclusion = new File("exclusion.txt");
        ClassLoader classLoader = InitUtil.class.getClassLoader();
        scope = AnalysisScopeReader.readJavaScope("scope.txt", exclusion, classLoader);
        //System.out.println(scope);
    }

    /**
     * @Description: 解析输入的参数
     * @Params: 命令行后的 -m|c <project_target> <change_info>
     * @Author: Li Yongshao
     * @date: 2020/11/16
     */
    private static void parseArgs(String[] args) {
        //根据参数选择类级或方法级
        if (args[0].equals("-c"))
            selectType = 0;
        else if (args[0].equals("-m"))
            selectType = 1;
        //加载路径参数
        target = args[1];
        change_info = args[2];
    }

    /**
     * @Description: 递归地将target目录中的class文件加入域
     * @Author: Li Yongshao
     * @date: 2020/11/16
     */
    private static void initScope(String path) throws InvalidClassFileException {
        File file = new File(path);
        if (file.exists()) {
            File[] files = file.listFiles();
            if (!(files == null || files.length == 0)) {
                for (File subFile : files) {
                    if (subFile.isDirectory()) {
                        initScope(subFile.getAbsolutePath());
                    } else if (subFile.getName().endsWith(".class")) {  //将class文件加入域
                        scope.addClassFileToScope(ClassLoaderReference.Application, subFile);
                    }
                }
            }
        } else {
            System.out.println("File not exists!");
        }
    }


    /**
     * @Description: 初始化载入点，初始化图
     * @Author: Li Yongshao
     * @date: 2020/11/16
     */
    private static void initGraph() throws ClassHierarchyException, CancelException {
        ClassHierarchy cha = ClassHierarchyFactory.makeWithRoot(scope);
        Iterable<Entrypoint> eps = new AllApplicationEntrypoints(scope, cha);
        cg = new CHACallGraph(cha);
        cg.init(eps);
    }

    public static void main(String[] args) throws IOException, InvalidClassFileException, ClassHierarchyException, CancelException {
        init();
        if (args.length == 0) {
            parseArgs(new String[]{"-c",
                    "C:\\_18SE\\_学习\\大三\\自动化测试\\Final大作业\\实践\\Test\\ClassicAutomatedTesting\\0-CMD\\target",
                    "C:\\_18SE\\_学习\\大三\\自动化测试\\Final大作业\\实践\\Test\\ClassicAutomatedTesting\\0-CMD\\data\\change_info.txt"});
        } else parseArgs(args);
        initScope(target);
        initGraph();
        if (selectType == 0) {
            new AugUtil(cg, change_info).classLevelSelect();
            //new AugUtil(cg,change_info).outputDotFile();
        }
        else if (selectType == 1) {
            new AugUtil(cg, change_info).methodLevelSelect();
            //new AugUtil(cg,change_info).outputDotFile();
        }
    }
}