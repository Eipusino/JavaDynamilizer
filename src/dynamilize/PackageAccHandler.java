package dynamilize;

import dynamilize.classmaker.ClassInfo;
import dynamilize.classmaker.CodeBlock;
import dynamilize.classmaker.Parameter;
import dynamilize.classmaker.code.IClass;
import dynamilize.classmaker.code.ILocal;
import dynamilize.classmaker.code.IMethod;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * The utility class used to access and elevate package private methods is used to create a string of
 * inheritance subtrees for elevating package private methods on a target class, in order to achieve the
 * purpose of rewriting package private methods.
 * <p>Specifically, after passing the target base class into the {@link PackageAccHandler#handle(Class)} method, it will sequentially check
 * whether there are package private methods in each parent type of this type. If there are package private
 * methods, the package name of this class will be used to inherit from the previous level of the type. By
 * loading this class into the same protected domain through {@link PackageAccHandler#loadClass(ClassInfo, Class)}, the
 * inherited type can access the target class's package private methods. Rewrite and raise its access modifier
 * to {@code protected}. After completing the traversal of all superclass in the base class, all package private
 * methods in the hierarchical structure of this class can be accessed.
 * <p>Intuitively speaking, this class does the following:
 * <pre>{@code
 * //Assuming there are several classes as follows:
 *
 * //package pac1;
 * public class A {
 *     void method1() { //package-private method
 *
 *     }
 *
 *     public void test() {
 *         method1();
 *     }
 * }
 *
 * //package pac2;
 * import pac1.A;
 *
 * public class B extends A {
 *     void method2() { //package-private method
 *
 *     }
 *
 *     @Override
 *     public void test() {
 *         super.test();
 *         method2();
 *     }
 * }
 *
 * //If the handle method is passed into class B, the following two types will be created:
 *
 * //package pac2:
 * public class B$packageAccess$0 extends B {
 *     @Override
 *     protected void method2() {
 *         super.method2();
 *     }
 * }
 *
 * //package pac1:
 * import pac2.B$packageAccess$0;
 *
 * public class A$packageAccess$0 extends B$packageAccess$0 {
 *     @Override
 *     protected void method1() {
 *         super.method2();
 *     }
 * }
 *
 * //Finally, the handle method will return the class object pac1.A$packageAccess$0.
 * //At this point, all package private methods have been promoted to protected,
 * //and the subclass's rewriting of both package private methods will take effect when calling the test method.
 * }</pre>
 * <strong>Note:<ul>
 * <li>The method of cross package inheritance and class rewriting shown above is actually illegal in Java compilers, but this logic is valid in
 * JVM. The above code is only for logical illustration.</li>
 * <li>Due to the limitation of Java package namespace, it is not possible to load Java open package
 * name types from external sources. Therefore, open package private methods do not apply to
 * classes whose package names start with, but are not limited to, {@code java.}, {@code javax.}, {@code jdk.}, {@code sun.}.</li>
 * </ul></strong>
 *
 * @author EBwilson
 * @since 1.6
 */
@SuppressWarnings("unchecked")
public abstract class PackageAccHandler {
	public static final int PAC_PRI_FLAGS = Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE | Modifier.STATIC | Modifier.FINAL;
	@SuppressWarnings("rawtypes")
	public static final ILocal[] A = new ILocal[0];
	private final Map<Class<?>, Class<?>> classMap = new HashMap<>();

	public <T> Class<? extends T> handle(Class<T> baseClass) {
		return (Class<? extends T>) classMap.computeIfAbsent(baseClass, c -> {
			Class<?> curr = c;
			Class<?> opening = null;

			while (curr != null) {
				if (shouldOpen(curr)) {
					ClassInfo<?> ci = makeClassInfo(opening == null ? ClassInfo.asType(curr) : ClassInfo.asType(opening), curr);
					opening = loadClass(ci, curr);
				}

				curr = curr.getSuperclass();
			}

			return opening == null ? c : opening;
		});
	}

	protected boolean shouldOpen(Class<?> checking) {
		if (checking.getPackage().getName().startsWith("java.")) return false;

		for (Method method : checking.getDeclaredMethods()) {
			if ((method.getModifiers() & PAC_PRI_FLAGS) == 0) {
				return true;
			}
		}
		return false;
	}

	@SuppressWarnings("rawtypes")
	protected <T> ClassInfo<? extends T> makeClassInfo(ClassInfo<?> superClass, Class<?> base) {
		ClassInfo<?> res = new ClassInfo<>(
				Modifier.PUBLIC,
				base.getName() + "$packageAccess$" + superClass.name().hashCode(),
				superClass
		);

		ClassInfo<?> baseC = ClassInfo.asType(base);

		for (Method method : base.getDeclaredMethods()) {
			if ((method.getModifiers() & PAC_PRI_FLAGS) == 0) {
				IMethod<?, ?> sup = baseC.getMethod(
						ClassInfo.asType(method.getReturnType()),
						method.getName(),
						Arrays.stream(method.getParameterTypes()).map(ClassInfo::asType).toArray(ClassInfo[]::new)
				);

				CodeBlock<?> code = res.declareMethod(
						Modifier.PROTECTED,
						method.getName(),
						ClassInfo.asType(method.getReturnType()),
						Parameter.asParameter(method.getParameters())
				);

				ILocal<T> self = code.getThis();
				if (method.getReturnType() == void.class) {
					code.invokeSuper(
							self,
							sup,
							null,
							code.getParamList().toArray(A)
					);
				} else {
					ILocal ret = code.local(ClassInfo.asType(method.getReturnType()));

					code.invokeSuper(
							self,
							sup,
							ret,
							code.getParamList().toArray(A)
					);
					code.returnValue(ret);
				}
			}
		}

		for (Constructor<?> constructor : superClass.getTypeClass().getDeclaredConstructors()) {
			if ((constructor.getModifiers() & PAC_PRI_FLAGS) == 0 && !(base.getPackage().equals(superClass.getTypeClass().getPackage())))
				continue;
			if ((constructor.getModifiers() & (Modifier.PRIVATE | Modifier.FINAL)) != 0) continue;

			IMethod<?, ?> su = superClass.getConstructor(Arrays.stream(constructor.getParameterTypes()).map(ClassInfo::asType).toArray(IClass[]::new));

			CodeBlock<?> code = res.declareConstructor(Modifier.PUBLIC, Parameter.asParameter(constructor.getParameters()));
			code.invoke(code.getThis(), su, null, code.getParamList().toArray(A));
		}

		return (ClassInfo<? extends T>) res;
	}

	/**
	 * Load the target {@linkplain ClassInfo class information} as an object with the same protection domain as the base class,
	 * and implement this method based on the target platform.
	 */
	protected abstract <T> Class<? extends T> loadClass(ClassInfo<?> clazz, Class<T> baseClass);
}
