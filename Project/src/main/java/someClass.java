import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.config.AnalysisScopeReader;

import java.io.File;
import java.io.IOException;

public class someClass {

//    public void loadClass() throws IOException, InvalidClassFileException {
//        ClassLoader classLoader = someClass.class.getClassLoader();
//        /*
//            Other code...
//         */
//        AnalysisScope scope = AnalysisScopeReader.readJavaScope(
//                "scope.txt",
//                new File("exclusion.txt"),
//                classLoader
//        );
//        File file = new File("scope.txt");
//        scope.addClassFileToScope(ClassLoaderReference.Application, file);
//    }
}
