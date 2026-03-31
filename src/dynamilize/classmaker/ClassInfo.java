package dynamilize.classmaker;

import dynamilize.IllegalHandleException;
import dynamilize.classmaker.code.Element;
import dynamilize.classmaker.code.IClass;
import dynamilize.classmaker.code.IField;
import dynamilize.classmaker.code.IMethod;
import dynamilize.classmaker.code.annotation.AnnotatedElement;
import dynamilize.classmaker.code.annotation.AnnotationType;
import dynamilize.classmaker.code.annotation.IAnnotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Type identification, used to mark/generate a class object, usually has the following two types:
 * <ul>
 *   <li><strong>Generate type identifier</strong> is mutable by default (before type generation is completed), used to declare
 *   and describe a new type and load it, and is the descriptive type used to generate a new class.
 *   <li><strong>The type identifier</strong> created for an existing type is immutable and is only used to mark an existing type
 *   for allocation and use.
 * </ul>
 * The default constructed type tag is the generated type identifier, which allows declaring methods and
 * fields, constructors, and static code (cinit block). If the generated type identifier is completed, it will be
 * transformed into an existing type identifier for the generated type.
 * <p>You will need to use an assembly language like approach to describe behavior, such as using goto instead
 * of {@code for} and {@code if}.
 *
 * <p>Give a simple example:
 * <pre>{@code
 * // There is a class:
 * import java.util.concurrent.ThreadLocalRandom;
 *
 * public class Demo {
 *     public static String INFO = "HelloWorld";
 *
 *     public static main(String[] args) {
 *         if (ThreadLocalRandom.current().nextInt() < 123456789) {
 *             System.out.println(INFO);
 *         } else {
 *             System.out.println("Late")
 *         }
 *     }
 * }
 *
 * // To generate an equivalent type, the following description should be given:
 *
 * ClassInfo<?> demoInfo = new ClassInfo<>(
 *     Modifier.PUBLIC,
 *     "Demo",
 *     ClassInfo.OBJECT_TYPE //Or null, default points to java.lang.Object
 * );
 *
 * IField<String> INFO = demoInfo.declareField(
 *     Modifier.PUBLIC | Modifier.STATIC,
 *     "INFO",
 *     ClassInfo.STRING_TYPE,
 *     "HelloWorld"
 * );
 * IField<PrintWriter> out = ClassInfo.asType(System.class).getField(ClassInfo.asType(PrintWriter.class), "out");
 *
 * IMethod<?, Long> nanoTime = ClassInfo.asType(System.class).getMethod(ClassInfo.LONG_TYPE, "nanoTime");
 * IMethod<?, Void> println = ClassInfo.asType(PrintWriter.class).getMethod(ClassInfo.VOID_TYPE, "println", ClassInfo.STRING_TYPE);
 *
 * CodeBlock<Void> code = demoInfo.declareMethod(
 *     Modifier.PUBLIC | Modifier.STATIC,
 *     "main",
 *     ClassInfo.VOID_TYPE,
 *     Parameter.trans(ClassInfo.STRING_TYPE.asArray())
 * );
 * ILocal<Long> time = code.local(ClassInfo.LONG_TYPE);
 * code.invokeStatic(nanoTime, time);
 *
 * ILocal<String> lo = code.local(ClassInfo.STRING_TYPE);
 * ILocal<PrintWriter> ou = code.local(ClassInfo.asType(PrintWriter.class));
 * code.assignStatic(out, ou);
 *
 * Label label = code.label();
 * ILocal<Long> cons = code.local(ClassInfo.LONG_TYPE);
 * code.loadConstant(cons, 123456789L);
 * code.compare(time, ">=", cons, label);
 *
 * code.assignStatic(INFO, lo);
 * code.invoke(ou, println, null, lo);
 * code.returnVoid();
 *
 * code.markLabel(label);
 * code.loadConstant(lo, "Late");
 * code.invoke(ou, println, null, lo);
 *
 * //Generate this demoInfo to obtain an equivalent class
 * }</pre>
 * This process is tedious, but also fast. Skipping the compiler to generate class files sacrifices operability in
 * exchange for class generation speed. It is recommended to describe the behavior as a template and then
 * make changes based on the template to improve development efficiency.
 */
@SuppressWarnings("rawtypes")
public class ClassInfo<T> extends AnnotatedMember implements IClass<T> {
	public static final LinkedList<IClass<?>> QUEUE = new LinkedList<>();
	public static final HashSet<IClass<?>> EXCLUDE = new HashSet<>();

	private static final Map<Class<?>, ClassInfo<?>> classMap = new HashMap<>();

	private static final String OBJECT_TYPE_MARK = "Ljava/lang/Object;";
	private static final String INIT = "<init>";
	private static final String CLINIT = "<clinit>";

	private static final int CLASS_ACCESS_MODIFIERS =
			Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE |
					Modifier.FINAL | Modifier.STATIC | Modifier.INTERFACE |
					Modifier.ABSTRACT | 4096/*synthetic*/ | 8192/*annotation*/ | 16384/*enum*/;

	private static final int METHOD_ACCESS_MODIFIERS =
			Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE |
					Modifier.FINAL | Modifier.STATIC | Modifier.NATIVE |
					Modifier.SYNCHRONIZED | Modifier.STRICT | Modifier.ABSTRACT |
					128/*varargs*/ | 4096/*synthetic*/;

	private static final int FIELD_ACCESS_MODIFIERS =
			Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE |
					Modifier.FINAL | Modifier.STATIC |
					Modifier.TRANSIENT | Modifier.VOLATILE;

	public static final String PRIMITIVE_REAL = "IFJZBCDV";

	@SuppressWarnings("unchecked")
	private static final IClass<? extends Throwable>[] EMP_ARR = new IClass[0];

	/**
	 * For non-existing type identifiers, this field is usually null and should be correctly set to the generated
	 * class after completing the creation and loading of the class.
	 * <p>As an identifier of an existing type, it must not be null.
	 */
	private Class<T> clazz;

	private CodeBlock<Void> clinit;

	private String realName;

	ClassInfo<? super T> superClass;
	List<ClassInfo<?>> interfaces;
	List<Element> elements;

	Map<String, IField<?>> fieldMap;
	Map<String, IMethod<?, ?>> methodMap;

	private ClassInfo<T[]> arrayType;
	private final ClassInfo<?> componentType;

	private AnnotationType<? extends Annotation> annotationType;


	/**
	 * For the type identification of {@code int}, generic references encapsulate data types while still referencing
	 * basic data types.
	 */
	public static final ClassInfo<Integer> INT_TYPE = new ClassInfo<>(int.class);


	/**
	 * For the type identification of {@code float}, generic references encapsulate data types while still referencing
	 * basic data types.
	 */
	public static final ClassInfo<Float> FLOAT_TYPE = new ClassInfo<>(float.class);


	/**
	 * For the type identification of {@code boolean}, generic references encapsulate data types while still referencing
	 * basic data types.
	 */
	public static final ClassInfo<Boolean> BOOLEAN_TYPE = new ClassInfo<>(boolean.class);


	/**
	 * For the type identification of {@code byte}, generic references encapsulate data types while still referencing
	 * basic data types.
	 */
	public static final ClassInfo<Byte> BYTE_TYPE = new ClassInfo<>(byte.class);


	/**
	 * For the type identification of {@code short}, generic references encapsulate data types while still referencing
	 * basic data types.
	 */
	public static final ClassInfo<Short> SHORT_TYPE = new ClassInfo<>(short.class);


	/**
	 * For the type identification of {@code long}, generic references encapsulate data types while still referencing
	 * basic data types.
	 */
	public static final ClassInfo<Long> LONG_TYPE = new ClassInfo<>(long.class);


	/**
	 * For the type identification of {@code double}, generic references encapsulate data types while still referencing
	 * basic data types.
	 */
	public static final ClassInfo<Double> DOUBLE_TYPE = new ClassInfo<>(double.class);

	/**
	 * For the type identification of {@code char}, generic references encapsulate data types while still referencing
	 * basic data types.
	 */
	public static final ClassInfo<Character> CHAR_TYPE = new ClassInfo<>(char.class);

	/**
	 * For the type identification of {@code void}, generic references encapsulate data types while still referencing
	 * basic data types.
	 */
	public static final ClassInfo<Void> VOID_TYPE = new ClassInfo<>(void.class);

	/** Type identification for {@link Object}. */
	public static final ClassInfo<Object> OBJECT_TYPE = new ClassInfo<>(Object.class);

	/** Type identification for {@link String}. */
	public static final ClassInfo<String> STRING_TYPE = asType(String.class);

	/** Type identification for {@link Class}. */
	@SuppressWarnings("rawtypes")
	public static final ClassInfo<Class> CLASS_TYPE = asType(Class.class);

	boolean initialized;

	final boolean isPrimitive;

	/**
	 * Create a type identifier to mark the type, and if the target type has already been marked, return the
	 * existing object identifier.
	 *
	 * @param clazz Class object to be used for tagging
	 */
	@SuppressWarnings("unchecked")
	public static <T> ClassInfo<T> asType(Class<T> clazz) {
		ClassInfo<T> res = (ClassInfo<T>) classMap.get(clazz);

		if (res == null) {
			res = clazz.isArray() ? new ClassInfo<>(asType(clazz.getComponentType())) : new ClassInfo<>(
					clazz.getModifiers(),
					clazz.getName(),
					clazz.getSuperclass() == null ? null : clazz.getSuperclass().equals(Object.class) ? OBJECT_TYPE : asType(clazz.getSuperclass()),
					Arrays.stream(clazz.getInterfaces()).map(ClassInfo::asType).toArray(ClassInfo[]::new)
			);
			res.clazz = clazz;

			classMap.put(clazz, res);

			if (clazz.isAnnotation())
				res.asAnnotation(null);

			res.initAnnotations();
		}

		return res;
	}

	/**
	 * This method should not be called externally. It is only used to pass a Java primitive type class object
	 * to obtain its type identifier. If the passed type is not a primitive Java type or {@link Object}, an exception
	 * will be thrown.
	 *
	 * @param primitive Marked basic type objects
	 */
	private ClassInfo(Class<T> primitive) {
		super(primitive.getName());
		if (!primitive.isPrimitive() && primitive != Object.class)
			throw new IllegalArgumentException(primitive + " was not a primitive class");

		interfaces = new ArrayList<>();
		elements = new ArrayList<>();

		clazz = primitive;

		if (primitive == Object.class) {
			setModifiers(Modifier.PUBLIC);

			realName = OBJECT_TYPE_MARK;

			fieldMap = new HashMap<>();
			methodMap = new HashMap<>();

			isPrimitive = false;
		} else {
			fieldMap = new HashMap<>();
			methodMap = new HashMap<>();

			setModifiers(Modifier.PUBLIC | Modifier.FINAL);

			if (primitive == int.class) realName = "I";
			else if (primitive == float.class) realName = "F";
			else if (primitive == boolean.class) realName = "Z";
			else if (primitive == byte.class) realName = "B";
			else if (primitive == short.class) realName = "S";
			else if (primitive == long.class) realName = "J";
			else if (primitive == char.class) realName = "C";
			else if (primitive == double.class) realName = "D";
			else if (primitive == void.class) realName = "V";

			isPrimitive = true;
		}

		componentType = null;

		classMap.put(primitive, this);
	}

	/**
	 * Build an instance that generates type identifiers for dynamically generating classes.
	 *
	 * @param modifiers  The modifier flags of the class describe the bit set
	 * @param name       The fully qualified name of the class
	 * @param superClass The superclass of class extension should be inheritable
	 * @param interfaces List of interfaces to be extended for this type
	 * @throws IllegalArgumentException If the extended supertype is {@code final} or non-inheritable, or if there are non interface types in the implemented interface
	 */
	public ClassInfo(int modifiers, String name, ClassInfo<? super T> superClass, ClassInfo<?>... interfaces) {
		super(name);
		checkModifiers(modifiers, CLASS_ACCESS_MODIFIERS);

		if (superClass == null) superClass = OBJECT_TYPE;
		if (Modifier.isFinal(superClass.modifiers()))
			throw new IllegalArgumentException(superClass + " was a final class, cannot extend a final class");

		for (ClassInfo<?> inter : interfaces) {
			if (!Modifier.isInterface(inter.modifiers()))
				throw new IllegalArgumentException("cannot implement a class " + inter + ", it must be a interface");
		}

		setModifiers(modifiers);
		this.superClass = superClass;
		this.interfaces = Arrays.asList(interfaces);

		elements = new ArrayList<>();
		fieldMap = new HashMap<>();
		methodMap = new HashMap<>();

		realName = "L" + name.replace(".", "/") + ";";

		isPrimitive = false;
		componentType = null;
	}

	private ClassInfo(ClassInfo<?> comp) {
		super(comp.name() + "[]");

		superClass = OBJECT_TYPE;
		elements = new ArrayList<>();
		methodMap = new HashMap<>();
		fieldMap = new HashMap<>();

		realName = "[" + comp.realName;

		isPrimitive = false;
		componentType = comp;
	}

	@Override
	@SuppressWarnings("unchecked")
	public ClassInfo<T[]> asArray() {
		ClassInfo<T[]> res = arrayType;
		if (res == null) {
			if (clazz != null) {
				res = arrayType = (ClassInfo<T[]>) asType(Array.newInstance(clazz, 0).getClass());
			} else res = arrayType = new ClassInfo<>(this);
		}

		res.isExistedClass(); // Attempt to initialize array type

		return res;
	}

	@Override
	@SuppressWarnings({"rawtypes", "unchecked"})
	public <A extends Annotation> AnnotationType<A> asAnnotation(Map<String, Object> defaultAttributes) {
		if (defaultAttributes == null) defaultAttributes = new HashMap<>();
		checkAnnotation(defaultAttributes);

		Map<String, Object> map = defaultAttributes;
		return annotationType != null ? (AnnotationType<A>) annotationType : (AnnotationType<A>) (annotationType = new AnnotationType<A>() {
			final Map<String, Object> def = new HashMap<>(map);

			@Override
			public IClass<A> typeClass() {
				return (IClass<A>) ClassInfo.this;
			}

			@Override
			public Map<String, Object> defaultValues() {
				return def;
			}

			@Override
			public IAnnotation<A> annotateTo(AnnotatedElement element, Map<String, Object> attributes) {
				IAnnotation<A> anno = new AnnotationDef(this, element, attributes);
				element.addAnnotation(anno);
				return anno;
			}
		});
	}

	@Override
	public boolean isArray() {
		return componentType != null;
	}

	@Override
	public IClass<?> componentType() {
		return componentType;
	}

	@Override
	public Class<T> getTypeClass() {
		return isExistedClass() ? clazz : null;
	}

	@Override
	public boolean isAnnotation() {
		return annotationType != null;
	}

	@Override
	public String realName() {
		return realName;
	}

	public String internalName() {
		return (realName.startsWith("L") ? realName.replaceFirst("L", "") : realName).replace(";", "");
	}

	/**
	 * Use the provided class generator to construct the class object declared with this type identifier,
	 * which should be available. Please refer to {@link ClassInfo#checkGen()}
	 *
	 * @param generator A class generator used for building types
	 * @return Build the generated class object
	 * @throws IllegalHandleException If the current status of the type identifier is unavailable
	 */
	public Class<T> generate(AbstractClassGenerator generator) {
		checkGen();

		try {
			return clazz = generator.generateClass(this);
		} catch (ClassNotFoundException e) {
			throw new IllegalHandleException(e);
		}
	}

	public void initAnnotations() {
		if (!initialized) {
			for (Annotation annotation : clazz.getAnnotations()) {
				addAnnotation(new AnnotationDef<>(annotation));
			}
			initialized = true;
		}

		for (IField<?> field : fieldMap.values()) {
			field.initAnnotations();
		}

		for (IMethod<?, ?> method : methodMap.values()) {
			method.initAnnotations();
		}
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public boolean isExistedClass() {
		if (clazz != null) return true;

		if (componentType != null && componentType.isExistedClass()) {
			clazz = (Class<T>) Array.newInstance(componentType.clazz, 0).getClass();
			return true;
		}

		return false;
	}

	@Override
	public boolean isPrimitive() {
		return isPrimitive;
	}

	public CodeBlock<Void> getClinitBlock() {
		if (clinit == null) {
			clinit = declareCinit();
		}

		return clinit;
	}

	@Override
	@SuppressWarnings("unchecked")
	//utilMethods
	public <R> MethodInfo<T, R> getMethod(IClass<R> returnType, String name, IClass<?>... args) {
		return (MethodInfo<T, R>) methodMap.computeIfAbsent(pack(name, args), e -> {
			if (!isExistedClass())
				throw new IllegalHandleException("this class info is not a existed type mark, you have to declare method then get it");

			Class<?>[] paramClass = new Class[args.length];
			boolean stat = true;

			for (int i = 0; i < args.length; i++) {
				if ((paramClass[i] = args[i].getTypeClass()) == null) {
					stat = false;
					break;
				}
			}

			Method met = null;
			if (stat) {
				try {
					met = clazz.getDeclaredMethod(name, paramClass);
				} catch (NoSuchMethodException ex) {
					throw new IllegalHandleException(ex);
				}
			}

			MethodInfo<T, R> method;
			if (met == null) {
				method = new MethodInfo<>(this, Modifier.PUBLIC, name, returnType, EMP_ARR, ParameterInfo.trans(args));
			} else {
				Class<?>[] lis = met.getExceptionTypes();
				IClass<? extends Throwable>[] thrs = new IClass[lis.length];

				for (int i = 0; i < lis.length; i++) {
					thrs[i] = (IClass<? extends Throwable>) ClassInfo.asType(lis[i]);
				}

				method = new MethodInfo<>(this, met.getModifiers(), name, returnType, thrs, ParameterInfo.asParameter(met.getParameters()));
				method.initAnnotations();
			}

			return method;
		});
	}

	//utilMethods
	@Override
	@SuppressWarnings("unchecked")
	public MethodInfo<T, Void> getConstructor(IClass<?>... args) {
		return (MethodInfo<T, Void>) methodMap.computeIfAbsent(pack(INIT, args), e -> {
			if (!isExistedClass())
				throw new IllegalHandleException("this class info is not a existed type mark, you have to declare method then get it");

			Class<?>[] paramClass = new Class[args.length];
			boolean stat = true;

			for (int i = 0; i < args.length; i++) {
				if ((paramClass[i] = args[i].getTypeClass()) == null) {
					stat = false;
					break;
				}
			}

			Constructor<?> cstr = null;
			if (stat) {
				try {
					cstr = clazz.getDeclaredConstructor(paramClass);
				} catch (NoSuchMethodException ex) {
					throw new IllegalHandleException(ex);
				}
			}

			MethodInfo<?, ?> res;
			if (cstr == null) {
				res = new MethodInfo<>(this, Modifier.PUBLIC, INIT, VOID_TYPE, EMP_ARR, ParameterInfo.trans(args));
			} else {
				Class<?>[] lis = cstr.getExceptionTypes();
				IClass<? extends Throwable>[] thrs = new IClass[lis.length];

				for (int i = 0; i < lis.length; i++) {
					thrs[i] = (IClass<? extends Throwable>) ClassInfo.asType(lis[i]);
				}

				res = new MethodInfo<>(this, cstr.getModifiers(), INIT, VOID_TYPE, thrs, ParameterInfo.asParameter(cstr.getParameters()));
				res.initAnnotations();
			}

			return res;
		});
	}

	@Override
	@SuppressWarnings("unchecked")
	public <TY> FieldInfo<TY> getField(IClass<TY> type, String name) {
		return (FieldInfo<TY>) fieldMap.computeIfAbsent(name, e -> {
			if (!isExistedClass())
				throw new IllegalHandleException("this class info is not a existed type mark, you have to declare field then get it");

			int flags;
			try {
				flags = clazz.getDeclaredField(name).getModifiers();
			} catch (NoSuchFieldException ex) {
				throw new IllegalHandleException(ex);
			}

			FieldInfo<TY> field = new FieldInfo<>(this, flags, name, type, null);
			type.initAnnotations();
			return field;
		});
	}

	@Override
	public boolean isAssignableFrom(IClass<?> target) {
		if (isExistedClass() && target.isExistedClass() && clazz.isAssignableFrom(target.getTypeClass())) {
			return true;
		}

		IClass<?> ty = target;
		if (!Modifier.isInterface(modifiers())) {
			while (ty != null) {
				if (equals(ty)) return true;

				ty = ty.superClass();
			}
		} else {
			QUEUE.clear();
			EXCLUDE.clear();

			while (ty != null) {
				for (IClass<?> iClass : ty.interfaces()) {
					if (EXCLUDE.add(iClass)) QUEUE.addFirst(iClass);
				}
				while (!QUEUE.isEmpty()) {
					IClass<?> c = QUEUE.removeFirst();
					if (equals(c)) return true;

					for (IClass<?> iClass : c.interfaces()) {
						if (EXCLUDE.add(iClass)) QUEUE.addFirst(iClass);
					}
				}

				ty = ty.superClass();
			}
		}

		return false;
	}

	private static String pack(String name, IClass<?>... args) {
		StringBuilder builder = new StringBuilder(name);
		builder.append("(");
		for (IClass<?> arg : args) {
			builder.append(arg.realName());
			if (arg.realName().length() != 1 || !PRIMITIVE_REAL.contains(arg.realName())) {
				builder.append(",");
			}
		}
		String type = builder.toString();
		if (type.endsWith(",")) type = type.substring(0, type.length() - 1);

		return type + ")";
	}

	/**
	 * Declare a {@code <clinit>} block and return the block body declaration object. If the block already exists, return the existing block body
	 *
	 * @return The method body of a static block declares an object, and if the block already exists, returns its block
	 * @throws IllegalHandleException If this type declaration has already been generated as a class or type identifier
	 */
	public CodeBlock<Void> declareCinit() {
		return clinit != null ? clinit : (clinit = declareMethod(Modifier.STATIC, CLINIT, VOID_TYPE));
	}

	/**
	 * Declare a constructor and return the constructor body declaration object.
	 *
	 * @param modifiers  The modifier flags of this constructor cannot be {@code static}
	 * @param parameters Parameter type identification of constructor
	 * @return Declaration object of constructor method body
	 * @throws IllegalArgumentException If modifiers contain {@code static} or are illegal
	 * @throws IllegalHandleException   If this type declaration has already been generated as a class or type identifier
	 */
	public CodeBlock<Void> declareConstructor(int modifiers, ParameterInfo<?>... parameters) {
		if (Modifier.isStatic(modifiers))
			throw new IllegalArgumentException("constructor cannot be static");

		return declareMethod(modifiers, INIT, VOID_TYPE, parameters);
	}

	/**
	 * Declare a method and return the declared object of the method block.
	 *
	 * @see ClassInfo#declareMethod(int, String, ClassInfo, ClassInfo[], ParameterInfo[])
	 */
	@SuppressWarnings("unchecked")
	public <R> CodeBlock<R> declareMethod(int modifiers, String name, ClassInfo<R> returnType, ParameterInfo<?>... parameters) {
		return declareMethod(modifiers, name, returnType, new ClassInfo[0], parameters);
	}

	/**
	 * Declare a method and return the declared object of the method block.
	 *
	 * @param modifiers  The flags of the method
	 * @param name       Name of Method
	 * @param returnType The return value type of the method
	 * @param parameters Method parameter type list
	 * @param throwsList List of thrown exceptions for methods
	 * @throws IllegalArgumentException If modifiers are illegal
	 * @throws IllegalHandleException   If this type declaration has already been generated as a class or type identifier
	 */
	@SuppressWarnings("unchecked")
	public <R> CodeBlock<R> declareMethod(int modifiers, String name, ClassInfo<R> returnType, ClassInfo<? extends Throwable>[] throwsList, ParameterInfo<?>... parameters) {
		checkGen();
		checkModifiers(modifiers, METHOD_ACCESS_MODIFIERS);

		if (Modifier.isAbstract(modifiers) && Modifier.isStatic(modifiers))
			throw new IllegalArgumentException("conflicted modifiers " + Modifier.toString(modifiers));

		MethodInfo<T, R> method = (MethodInfo<T, R>) methodMap.computeIfAbsent(
				pack(name, Arrays.stream(parameters).map(ParameterInfo::getType).toArray(IClass[]::new)),
				e -> new MethodInfo<>(this, modifiers, name, returnType, throwsList, parameters));
		elements.add(method);

		return method.block();
	}

	/**
	 * Declare a field. If you need to assign a default constant value to the field, it should be static.
	 * Otherwise, you should initialize the default value of the object's member field in the constructor of
	 * this type.
	 * <p>The default values assigned can only be literal constants of the following basic types or array types
	 * composed of them
	 * <ul>
	 *     <li>{@code Class<?>} -> {@code AnyClass}
	 *     <li>{@code Enum<?>} -> {@code AnyEnum.object}
	 *     <li>{@code String} -> {@code "anythings"}
	 *     <li>{@code int} -> {@code -2147483648} ~ {@code 2147483647}
	 *     <li>{@code float} -> {@code -3.4028235E38F} ~ {@code 3.4028235E38F}
	 *     <li>{@code boolean} -> {@code true} or {@code false}
	 *     <li>{@code byte} -> {@code -128} ~ {@code 127}
	 *     <li>{@code short} -> {@code -32768} ~ {@code 32767}
	 *     <li>{@code long} -> {@code -9223372036854775808L} ~ {@code 9223372036854775807L}
	 *     <li>{@code double} -> {@code -1.7976931348623157E308D} ~ {@code 1.7976931348623157E308D}
	 *     <li>{@code char} -> {@code 'u0000'} ~ {@code 'uFFFF'}
	 * <ul/>
	 *
	 * @param modifiers Field modifier flags identification
	 * @param name      Field name
	 * @param type      Type of field
	 * @param initial   The default initial value of a field can be empty and can only be assigned according to rules
	 * @throws IllegalArgumentException If modifiers are invalid or the given initialization constant
	 *                                  value is invalid, please refer to {@link ClassInfo#checkModifiers(int, int)}
	 * @throws IllegalHandleException   If this type declaration has already been generated as a class
	 *                                  or type identifier
	 */
	@SuppressWarnings("unchecked")
	public <F> FieldInfo<F> declareField(int modifiers, String name, ClassInfo<F> type, F initial) {
		checkGen();
		checkModifiers(modifiers, FIELD_ACCESS_MODIFIERS);
		FieldInfo<F> field = (FieldInfo<F>) fieldMap.computeIfAbsent(name, e -> new FieldInfo<>(this, modifiers, name, type, initial));
		elements.add(field);

		if (initial != null && clinit == null
				&& (field.initial() instanceof Array || field.initial() instanceof Enum<?>)) {
			clinit = declareCinit();
		}

		return field;
	}

	/**
	 * Check for conflicts between modifiers and whether modifiers are available, such as public, protected, and private, which can only have one of them.
	 *
	 * @param modifiers Modifiers to be checked
	 * @param access    Acceptable modifier bit sets
	 * @throws IllegalArgumentException If there are conflicts or unacceptable modifiers
	 */
	private static void checkModifiers(int modifiers, int access) {
		if (Modifier.isPublic(modifiers) && (modifiers & (Modifier.PROTECTED | Modifier.PRIVATE)) != 0
				|| Modifier.isProtected(modifiers) && (modifiers & (Modifier.PUBLIC | Modifier.PRIVATE)) != 0
				|| Modifier.isPrivate(modifiers) && (modifiers & (Modifier.PUBLIC | Modifier.PROTECTED)) != 0
				|| Modifier.isAbstract(modifiers) && (modifiers & (Modifier.FINAL | Modifier.NATIVE)) != 0
				|| Modifier.isInterface(modifiers) && (modifiers & Modifier.FINAL) != 0
				|| Modifier.isFinal(modifiers) && Modifier.isVolatile(modifiers))
			throw new IllegalArgumentException("modifiers was conflicted， modifiers: " + Modifier.toString(modifiers));

		if ((modifiers & ~access) != 0)
			throw new IllegalArgumentException("unexpected modifiers with " + Modifier.toString(modifiers));
	}

	/**
	 * Check the current class generation status. If it is not in a modifiable state, throw an exception. The modifiable state should meet the following conditions:
	 * <ul>
	 *   <li><strong>This type identifier is not an existing type identifier</strong>
	 *   <li><strong>This type of identifier has not yet completed class generation and class object loading</strong>
	 * </ul>
	 *
	 * @throws IllegalHandleException If the identification status of this type is incorrect
	 */
	public void checkGen() {
		if (isExistedClass())
			throw new IllegalHandleException(this + " was a generated object or type mark, can not handle it");
	}

	private void checkAnnotation(Map<String, Object> defAttributes) {
		if ((isExistedClass() && !clazz.isAnnotation()))
			throw new IllegalHandleException("clazz " + this + " was not a annotation type");

		Map<String, Object> map = new HashMap<>(defAttributes);
		for (Element element : elements()) {
			if (!(element instanceof IMethod<?, ?> met) || !Modifier.isAbstract(met.modifiers()))
				throw new IllegalHandleException("clazz " + this + " was not a annotation type");

			IClass<?> type = met.returnType();
			if ((!isReferenceType(type) && !type.equals(STRING_TYPE))
					|| (type.isArray() && (!isReferenceType(type.componentType()) && !type.componentType().equals(STRING_TYPE))))
				throw new IllegalHandleException("unsupported return type in annotation: " + met);

			Object val = map.remove(met.name());
			if (!asType(val.getClass()).equals(type))
				throw new IllegalHandleException("attribute \"" + met.name() + "\" type was " + type + ", but given default value is " + val.getClass() + ", they should be same");
		}
		if (!map.isEmpty()) throw new IllegalArgumentException("unknown default attribute declaring: " + map);
	}

	private boolean isReferenceType(IClass<?> type) {
		return type.realName().startsWith("L");
	}

	@Override
	public final IClass<? super T> superClass() {
		return superClass;
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	@Override
	public List<IClass<?>> interfaces() {
		return (List) interfaces;
	}

	@Override
	public List<Element> elements() {
		return elements;
	}

	@Override
	public String toString() {
		return isExistedClass() ? clazz.toString() : name();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof ClassInfo<?> classInfo)) return false;
		return Objects.equals(clazz, classInfo.clazz) && realName.equals(classInfo.realName);
	}

	@Override
	public int hashCode() {
		return Objects.hash(clazz, realName);
	}

	@Override
	public boolean isType(ElementType type) {
		return type == (isAnnotation() ? ElementType.ANNOTATION_TYPE : ElementType.TYPE);
	}

	@Override
	public <A extends Annotation> A getAnnotation(Class<A> annoClass) {
		Class<?> clazz = getTypeClass();
		if (clazz == null)
			throw new IllegalHandleException("only get annotation object in existed type info");

		return clazz.getAnnotation(annoClass);
	}
}
