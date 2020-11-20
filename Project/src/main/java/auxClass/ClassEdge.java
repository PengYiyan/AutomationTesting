package auxClass;

import com.ibm.wala.classLoader.IClass;

/**
 * 记录类的边
 */
public class ClassEdge {
    public IClass begin,end;

    public ClassEdge(IClass begin,IClass end){
        this.begin = begin;
        this.end = end;
    }
}
