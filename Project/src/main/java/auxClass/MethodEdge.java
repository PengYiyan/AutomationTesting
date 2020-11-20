package auxClass;

import com.ibm.wala.classLoader.ShrikeBTMethod;

/**
 * 记录方法的边
 */
public class MethodEdge {
    public ShrikeBTMethod begin,end;

    public MethodEdge(ShrikeBTMethod begin,ShrikeBTMethod end){
        this.begin = begin;
        this.end = end;
    }
}
