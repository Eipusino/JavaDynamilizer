package dynamilize.classmaker;

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

public interface ElementVisitor {
	void visitClass(IClass<?> clazz);

	void visitMethod(IMethod<?, ?> method);

	void visitField(IField<?> field);

	void visitLocal(ILocal<?> local);

	void visitInvoke(IInvoke<?> invoke);

	void visitGetField(IGetField<?, ?> getField);

	void visitPutField(IPutField<?, ?> putField);

	void visitLocalSet(ILocalAssign<?, ?> localSet);

	void visitOperate(IOperate<?> operate);

	void visitCast(ICast cast);

	void visitGoto(IGoto iGoto);

	void visitLabel(IMarkLabel label);

	void visitCompare(ICompare<?> compare);

	void visitCodeBlock(ICodeBlock<?> codeBlock);

	void visitReturn(IReturn<?> iReturn);

	void visitInstanceOf(IInstanceOf instanceOf);

	void visitNewInstance(INewInstance<?> newInstance);

	void visitOddOperate(IOddOperate<?> operate);

	void visitConstant(ILoadConstant<?> loadConstant);

	void visitNewArray(INewArray<?> newArray);

	void visitCondition(ICondition condition);

	void visitArrayGet(IArrayGet<?> arrayGet);

	void visitArrayPut(IArrayPut<?> arrayPut);

	void visitSwitch(ISwitch<?> zwitch);

	void visitThrow(IThrow<?> thr);
}
