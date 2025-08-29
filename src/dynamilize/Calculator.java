package dynamilize;

/**
 * 变量计算器，函数式接口，用于对变量的当前值进行一系列处理后提供回调
 *
 * @author EBwilson
 */
public interface Calculator<T> {
	T calculate(T input);

	interface BoolCalculator {
		boolean calculate(boolean input);
	}

	interface ByteCalculator {
		byte calculate(byte input);
	}

	interface ShortCalculator {
		short calculate(short input);
	}

	interface IntCalculator {
		int calculate(int input);
	}

	interface LongCalculator {
		long calculate(long input);
	}

	interface FloatCalculator {
		float calculate(float input);
	}

	interface DoubleCalculator {
		double calculate(double input);
	}
}
