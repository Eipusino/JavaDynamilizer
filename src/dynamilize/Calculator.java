package dynamilize;

/**
 * Variable calculator, a functional interface used to process the current value of a variable and provide
 * callbacks.
 *
 * @author EBwilson
 */
@FunctionalInterface
public interface Calculator<T> {
	T calculate(T input);

	@FunctionalInterface
	interface BoolCalculator {
		boolean calculate(boolean input);
	}

	@FunctionalInterface
	interface ByteCalculator {
		byte calculate(byte input);
	}

	@FunctionalInterface
	interface ShortCalculator {
		short calculate(short input);
	}

	@FunctionalInterface
	interface IntCalculator {
		int calculate(int input);
	}

	@FunctionalInterface
	interface LongCalculator {
		long calculate(long input);
	}

	@FunctionalInterface
	interface FloatCalculator {
		float calculate(float input);
	}

	@FunctionalInterface
	interface DoubleCalculator {
		double calculate(double input);
	}
}
