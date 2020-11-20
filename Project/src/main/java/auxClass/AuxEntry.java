package auxClass;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.ShrikeBTMethod;

import java.util.ArrayList;

/**
 * 用于记录类和类下的方法
 */
public class AuxEntry {
    public IClass iClass;
    public ArrayList<ShrikeBTMethod> methods;

    public AuxEntry(IClass iClass){
        this.iClass = iClass;
        this.methods = new ArrayList<ShrikeBTMethod>();
    }

    /**
     * 增添方法
     * @param method
     */
    public void tryAddMethod(ShrikeBTMethod method){
        if(this.methods.indexOf(method) == -1){//如果该方法没有被增加过，就添加该方法
            this.methods.add(method);
        }
    }
}
