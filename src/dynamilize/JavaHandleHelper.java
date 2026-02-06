package dynamilize;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * For the further upgrade of JDK16+in terms of modular management and cross platform and cross version
 * considerations, a platform support tool for Java basic behavior is provided, which includes some behavior
 * interfaces related to version/platform.
 * <p>The implementation should be carried out according to the functional description of the method for the
 * operating platform.
 *
 * @author EBwilson
 */
public interface JavaHandleHelper {
	default void makeAccess(Object object) {
		if (object instanceof AccessibleObject obj) {
			obj.setAccessible(true);
		}
	}

	IVariable getJavaVariableReference(Field field);

	IFunctionEntry getJavaMethodReference(Method method);
}
