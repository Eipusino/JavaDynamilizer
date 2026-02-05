package dynamilize;

import java.io.Serializable;

public class Variable implements IVariable {
	private final String name;
	private final Initializer<?> init;

	public Variable(String name) {
		this.name = name;
		this.init = null;
	}

	public Variable(String name, Initializer<?> init) {
		this.name = name;
		this.init = init;
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public void init(DynamicObject<?> obj) {
		Object value = init == null ? null : init.getInit();
		if (value == null) return;
		if (value.getClass().isPrimitive()) {
			if (value instanceof Byte b) set(obj, b.byteValue());
			else if (value instanceof Short b) set(obj, b.shortValue());
			else if (value instanceof Integer b) set(obj, b.intValue());
			else if (value instanceof Character b) set(obj, b.charValue());
			else if (value instanceof Long b) set(obj, b.longValue());
			else if (value instanceof Float b) set(obj, b.floatValue());
			else if (value instanceof Double b) set(obj, b.doubleValue());
			else if (value instanceof Boolean b) set(obj, b.booleanValue());
			else throw new IllegalHandleException("how do you cased this?");
		} else set(obj, value);
	}

	@Override
	public <T> T get(DynamicObject<?> obj) {
		return obj.varValueGet(name);
	}

	@Override
	public void set(DynamicObject<?> obj, Object value) {
		obj.varValueSet(name, value);
	}

	@Override
	public boolean get(DynamicObject<?> obj, boolean def) {
		Object b = get(obj);
		boolean res = def;
		if (b instanceof BooleanReference ref) {
			res = ref.value;
		} else if (b != null)
			throw new IllegalHandleException("variable " + name + " in object " + obj + " was not a boolean");

		return res;
	}

	@Override
	public byte get(DynamicObject<?> obj, byte def) {
		Object b = get(obj);
		byte res = def;
		if (b instanceof ByteReference ref) {
			res = ref.value;
		} else if (b != null)
			throw new IllegalHandleException("variable " + name + " in object " + obj + " was not a byte");

		return res;
	}

	@Override
	public short get(DynamicObject<?> obj, short def) {
		Object b = get(obj);
		short res = def;
		if (b instanceof ShortReference ref) {
			res = ref.value;
		} else if (b != null)
			throw new IllegalHandleException("variable " + name + " in object " + obj + " was not a short");

		return res;
	}

	@Override
	public int get(DynamicObject<?> obj, int def) {
		Object b = get(obj);
		int res = def;
		if (b instanceof IntReference ref) {
			res = ref.value;
		} else if (b != null)
			throw new IllegalHandleException("variable " + name + " in object " + obj + " was not a integer");

		return res;
	}

	@Override
	public long get(DynamicObject<?> obj, long def) {
		Object b = get(obj);
		long res = def;
		if (b instanceof LongReference ref) {
			res = ref.value;
		} else if (b != null)
			throw new IllegalHandleException("variable " + name + " in object " + obj + " was not a long");

		return res;
	}

	@Override
	public float get(DynamicObject<?> obj, float def) {
		Object b = get(obj);
		float res = def;
		if (b instanceof FloatReference ref) {
			res = ref.value;
		} else if (b != null)
			throw new IllegalHandleException("variable " + name + " in object " + obj + " was not a float");

		return res;
	}

	@Override
	public double get(DynamicObject<?> obj, double def) {
		Object b = get(obj);
		double res = def;
		if (b instanceof DoubleReference ref) {
			res = ref.value;
		} else if (b != null)
			throw new IllegalHandleException("variable " + name + " in object " + obj + " was not a double");

		return res;
	}

	@Override
	public char get(DynamicObject<?> obj, char def) {
		Object b = get(obj);
		char res = def;
		if (b instanceof CharReference ref) {
			res = ref.value;
		} else if (b != null)
			throw new IllegalHandleException("variable " + name + " in object " + obj + " was not a char");

		return res;
	}

	@Override
	public void set(DynamicObject<?> obj, boolean value) {
		if (get(obj) instanceof BooleanReference ref) {
			ref.value = value;
		} else {
			BooleanReference ref = new BooleanReference();
			ref.value = value;
			set(obj, ref);
		}
	}

	@Override
	public void set(DynamicObject<?> obj, byte value) {
		if (get(obj) instanceof ByteReference ref) {
			ref.value = value;
		} else {
			ByteReference ref = new ByteReference();
			ref.value = value;
			set(obj, ref);
		}
	}

	@Override
	public void set(DynamicObject<?> obj, short value) {
		if (get(obj) instanceof ShortReference ref) {
			ref.value = value;
		} else {
			ShortReference ref = new ShortReference();
			ref.value = value;
			set(obj, ref);
		}
	}

	@Override
	public void set(DynamicObject<?> obj, int value) {
		if (get(obj) instanceof IntReference ref) {
			ref.value = value;
		} else {
			IntReference ref = new IntReference();
			ref.value = value;
			set(obj, ref);
		}
	}

	@Override
	public void set(DynamicObject<?> obj, long value) {
		if (get(obj) instanceof LongReference ref) {
			ref.value = value;
		} else {
			LongReference ref = new LongReference();
			ref.value = value;
			set(obj, ref);
		}
	}

	@Override
	public void set(DynamicObject<?> obj, float value) {
		if (get(obj) instanceof FloatReference ref) {
			ref.value = value;
		} else {
			FloatReference ref = new FloatReference();
			ref.value = value;
			set(obj, ref);
		}
	}

	@Override
	public void set(DynamicObject<?> obj, double value) {
		if (get(obj) instanceof DoubleReference ref) {
			ref.value = value;
		} else {
			DoubleReference ref = new DoubleReference();
			ref.value = value;
			set(obj, ref);
		}
	}

	@Override
	public void set(DynamicObject<?> obj, char value) {
		if (get(obj) instanceof CharReference ref) {
			ref.value = value;
		} else {
			CharReference ref = new CharReference();
			ref.value = value;
			set(obj, ref);
		}
	}

	public static class BooleanReference implements Serializable {
		private static final long serialVersionUID = -7120385114040352042l;

		public boolean value;

		public BooleanReference() {}

		public BooleanReference(boolean initialValue) {
			value = initialValue;
		}

		@Override
		public String toString() {
			return String.valueOf(value);
		}
	}

	public static class ByteReference extends Number implements Serializable {
		private static final long serialVersionUID = -5505364929950384247l;

		public byte value;

		public ByteReference() {}

		public ByteReference(byte initialValue) {
			value = initialValue;
		}

		@Override
		public String toString() {
			return String.valueOf(value);
		}

		@Override
		public byte byteValue() {
			return value;
		}

		@Override
		public short shortValue() {
			return value;
		}

		@Override
		public int intValue() {
			return value;
		}

		@Override
		public long longValue() {
			return value;
		}

		@Override
		public float floatValue() {
			return value;
		}

		@Override
		public double doubleValue() {
			return value;
		}
	}

	public static class CharReference implements Serializable {
		private static final long serialVersionUID = -6890675570138946042l;

		public char value;

		public CharReference() {}

		public CharReference(char initialValue) {
			value = initialValue;
		}

		@Override
		public String toString() {
			return String.valueOf(value);
		}
	}

	public static class DoubleReference extends Number implements Serializable {
		private static final long serialVersionUID = 3872914250117543122l;

		public double value;

		public DoubleReference() {}

		public DoubleReference(double initialValue) {
			value = initialValue;
		}

		@Override
		public String toString() {
			return String.valueOf(value);
		}

		@Override
		public int intValue() {
			return (int) value;
		}

		@Override
		public long longValue() {
			return (long) value;
		}

		@Override
		public float floatValue() {
			return (float) value;
		}

		@Override
		public double doubleValue() {
			return value;
		}
	}

	public static class FloatReference extends Number implements Serializable {
		private static final long serialVersionUID = 2272494129790516325l;

		public float value;

		public FloatReference() {}

		public FloatReference(float initialValue) {
			value = initialValue;
		}

		@Override
		public String toString() {
			return String.valueOf(value);
		}

		@Override
		public int intValue() {
			return (int) value;
		}

		@Override
		public long longValue() {
			return (long) value;
		}

		@Override
		public float floatValue() {
			return value;
		}

		@Override
		public double doubleValue() {
			return value;
		}
	}

	public static class IntReference extends Number implements Serializable {
		private static final long serialVersionUID = -2015042737234032560l;

		public int value;

		public IntReference() {}

		public IntReference(int initialValue) {
			value = initialValue;
		}

		@Override
		public String toString() {
			return String.valueOf(value);
		}

		@Override
		public int intValue() {
			return value;
		}

		@Override
		public long longValue() {
			return value;
		}

		@Override
		public float floatValue() {
			return value;
		}

		@Override
		public double doubleValue() {
			return value;
		}
	}

	public static class LongReference extends Number implements Serializable {
		private static final long serialVersionUID = 6421798427509969426l;

		public long value;

		public LongReference() {}

		public LongReference(long initialValue) {
			value = initialValue;
		}

		@Override
		public String toString() {
			return String.valueOf(value);
		}

		@Override
		public int intValue() {
			return (int) value;
		}

		@Override
		public long longValue() {
			return value;
		}

		@Override
		public float floatValue() {
			return value;
		}

		@Override
		public double doubleValue() {
			return value;
		}
	}

	public static class ObjectReference<T> implements Serializable {
		private static final long serialVersionUID = -9054478421223311650l;

		public T value;

		public ObjectReference() {}

		public ObjectReference(T initialValue) {
			value = initialValue;
		}

		@Override
		public String toString() {
			return String.valueOf(value);
		}
	}

	public static class ShortReference extends Number implements Serializable {
		private static final long serialVersionUID = -6456250884875681558l;

		public short value;

		public ShortReference() {}

		public ShortReference(short initialValue) {
			value = initialValue;
		}

		@Override
		public String toString() {
			return String.valueOf(value);
		}

		@Override
		public byte byteValue() {
			return (byte) value;
		}

		@Override
		public short shortValue() {
			return value;
		}

		@Override
		public int intValue() {
			return value;
		}

		@Override
		public long longValue() {
			return value;
		}

		@Override
		public float floatValue() {
			return value;
		}

		@Override
		public double doubleValue() {
			return value;
		}
	}
}
