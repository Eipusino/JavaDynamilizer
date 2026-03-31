package dynamilize.classmaker.code;

import dynamilize.classmaker.ElementVisitor;
import dynamilize.classmaker.ParameterInfo;
import dynamilize.classmaker.code.annotation.AnnotatedElement;

import java.util.List;

public interface IMethod<S, R> extends Element, AnnotatedElement {
	@Override
	default void accept(ElementVisitor visitor) {
		visitor.visitMethod(this);
	}

	@Override
	default ElementKind kind() {
		return ElementKind.METHOD;
	}

	String name();

	int modifiers();

	String typeDescription();

	List<ParameterInfo<?>> parameters();

	List<IClass<? extends Throwable>> throwTypes();

	IClass<S> owner();

	IClass<R> returnType();

	ICodeBlock<R> block();
}
