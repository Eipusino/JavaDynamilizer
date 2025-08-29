package dynamilize.classmaker;

import dynamilize.classmaker.code.Element;
import dynamilize.classmaker.code.IArrayGet;
import dynamilize.classmaker.code.IArrayPut;
import dynamilize.classmaker.code.ICast;
import dynamilize.classmaker.code.IClass;
import dynamilize.classmaker.code.ICodeBlock;
import dynamilize.classmaker.code.ICompare;
import dynamilize.classmaker.code.ICondition;
import dynamilize.classmaker.code.IField;
import dynamilize.classmaker.code.IGetField;
import dynamilize.classmaker.code.IGoto;
import dynamilize.classmaker.code.IInstanceOf;
import dynamilize.classmaker.code.IInvoke;
import dynamilize.classmaker.code.ILoadConstant;
import dynamilize.classmaker.code.ILocal;
import dynamilize.classmaker.code.ILocalAssign;
import dynamilize.classmaker.code.IMarkLabel;
import dynamilize.classmaker.code.IMethod;
import dynamilize.classmaker.code.INewArray;
import dynamilize.classmaker.code.INewInstance;
import dynamilize.classmaker.code.IOddOperate;
import dynamilize.classmaker.code.IOperate;
import dynamilize.classmaker.code.IPutField;
import dynamilize.classmaker.code.IReturn;
import dynamilize.classmaker.code.ISwitch;
import dynamilize.classmaker.code.IThrow;

public class DefaultReadVisitor implements ElementVisitor {
	@Override
	public void visitClass(IClass<?> clazz) {
		for (Element element : clazz.elements()) {
			element.accept(this);
		}
	}

	@Override
	public void visitMethod(IMethod<?, ?> method) {
		method.block().accept(this);
	}

	@Override
	public void visitField(IField<?> field) {
	}

	@Override
	public void visitLocal(ILocal<?> local) {
	}

	@Override
	public void visitInvoke(IInvoke<?> invoke) {
	}

	@Override
	public void visitGetField(IGetField<?, ?> getField) {
	}

	@Override
	public void visitPutField(IPutField<?, ?> putField) {
	}

	@Override
	public void visitLocalSet(ILocalAssign<?, ?> localSet) {
	}

	@Override
	public void visitOperate(IOperate<?> operate) {
	}

	@Override
	public void visitCast(ICast cast) {
	}

	@Override
	public void visitGoto(IGoto iGoto) {
	}

	@Override
	public void visitLabel(IMarkLabel label) {
	}

	@Override
	public void visitCompare(ICompare<?> compare) {
	}

	@Override
	public void visitCodeBlock(ICodeBlock<?> codeBlock) {
		for (Element element : codeBlock.codes()) {
			element.accept(this);
		}
	}

	@Override
	public void visitReturn(IReturn<?> iReturn) {
	}

	@Override
	public void visitInstanceOf(IInstanceOf instanceOf) {
	}

	@Override
	public void visitNewInstance(INewInstance<?> newInstance) {
	}

	@Override
	public void visitOddOperate(IOddOperate<?> operate) {
	}

	@Override
	public void visitConstant(ILoadConstant<?> loadConstant) {
	}

	@Override
	public void visitNewArray(INewArray<?> newArray) {
	}

	@Override
	public void visitCondition(ICondition condition) {
	}

	@Override
	public void visitArrayGet(IArrayGet<?> arrayGet) {
	}

	@Override
	public void visitArrayPut(IArrayPut<?> arrayPut) {
	}

	@Override
	public void visitSwitch(ISwitch<?> zwitch) {
	}

	@Override
	public void visitThrow(IThrow<?> thr) {
	}
}
