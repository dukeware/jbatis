/*
 * Copyright 2003,2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 */
package org.ibatis.asm;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * MethodEmitter
 * <p>
 * Date: 2011-09-30, 12:46:43 +0800
 * 
 * @author suns
 * @version 1.0
 */
public class MethodEmitter extends MethodVisitor implements Opcodes {

    List<Label> labels;
    final boolean trace;

    public MethodEmitter(boolean trace, MethodVisitor mv) {
        super(ASM4, mv);
        this.trace = trace;
        if (trace) {
            labels = new ArrayList<Label>();
        }
    }

    public void start_method() {
        mv.visitCode();
    }

    public void end_method() {
        if (trace) {
            for (Label l : labels) {
                if (!l.marked) {
                    throw l.trace;
                }
            }
        }
        visitMaxs(0, 0);
        mv.visitEnd();
    }

    public void catch_exception(Label start, Label end, Type exception) {
        mv.visitTryCatchBlock(start, end, mark(), exception.getInternalName());
    }

    public void catch_finally(Label start, Label end) {
        mv.visitTryCatchBlock(start, end, mark(), null);
    }

    public void jump(Label label) {
        mv.visitJumpInsn(GOTO, label);
    }

    public void ifnull(Label label) {
        mv.visitJumpInsn(IFNULL, label);
    }

    public void ifnonnull(Label label) {
        mv.visitJumpInsn(IFNONNULL, label);
    }

    public void if_jump(int mode, Label label) {
        mv.visitJumpInsn(mode, label);
    }

    public void if_icmp(int mode, Label label) {
        if_cmp(T_int, mode, label);
    }

    public void if_cmp(Type type, int mode, Label label) {
        int intOp = -1;
        int jumpmode = mode;
        switch (mode) {
        case GE:
            jumpmode = LT;
            break;
        case LE:
            jumpmode = GT;
            break;
        }
        switch (type.getSort()) {
        case Type.LONG:
            mv.visitInsn(LCMP);
            break;
        case Type.DOUBLE:
            mv.visitInsn(DCMPG);
            break;
        case Type.FLOAT:
            mv.visitInsn(FCMPG);
            break;
        case Type.ARRAY:
        case Type.OBJECT:
            switch (mode) {
            case EQ:
                mv.visitJumpInsn(IF_ACMPEQ, label);
                return;
            case NE:
                mv.visitJumpInsn(IF_ACMPNE, label);
                return;
            }
            throw new IllegalArgumentException("Bad comparison for type " + type);
        default:
            switch (mode) {
            case EQ:
                intOp = IF_ICMPEQ;
                break;
            case NE:
                intOp = IF_ICMPNE;
                break;
            case GE:
                swap(); /* fall through */
            case LT:
                intOp = IF_ICMPLT;
                break;
            case LE:
                swap(); /* fall through */
            case GT:
                intOp = IF_ICMPGT;
                break;
            }
            mv.visitJumpInsn(intOp, label);
            return;
        }
        if_jump(jumpmode, label);
    }

    public void pop() {
        mv.visitInsn(POP);
    }

    public void pop2() {
        mv.visitInsn(POP2);
    }

    public void dup() {
        mv.visitInsn(DUP);
    }

    public void dup2() {
        mv.visitInsn(DUP2);
    }

    public void dup_x1() {
        mv.visitInsn(DUP_X1);
    }

    public void dup_x2() {
        mv.visitInsn(DUP_X2);
    }

    public void dup2_x1() {
        mv.visitInsn(DUP2_X1);
    }

    public void dup2_x2() {
        mv.visitInsn(DUP2_X2);
    }

    public void swap() {
        mv.visitInsn(SWAP);
    }

    public void aconst_null() {
        mv.visitInsn(ACONST_NULL);
    }

    public void swap(Type prev, Type type) {
        if (type.getSize() == 1) {
            if (prev.getSize() == 1) {
                swap(); // same as dup_x1(), pop();
            } else {
                dup_x2();
                pop();
            }
        } else {
            if (prev.getSize() == 1) {
                dup2_x1();
                pop2();
            } else {
                dup2_x2();
                pop2();
            }
        }
    }

    public void monitorenter() {
        mv.visitInsn(MONITORENTER);
    }

    public void monitorexit() {
        mv.visitInsn(MONITOREXIT);
    }

    public void math(int op, Type type) {
        mv.visitInsn(type.getOpcode(op));
    }

    public void array_load(Type type) {
        mv.visitInsn(type.getOpcode(IALOAD));
    }

    public void array_store(Type type) {
        mv.visitInsn(type.getOpcode(IASTORE));
    }

    /**
     * Casts from one primitive numeric type to another
     */
    public void cast_numeric(Type from, Type to) {
        if (from != to) {
            if (from == T_double) {
                if (to == T_float) {
                    mv.visitInsn(D2F);
                } else if (to == T_long) {
                    mv.visitInsn(D2L);
                } else {
                    mv.visitInsn(D2I);
                    cast_numeric(T_int, to);
                }
            } else if (from == T_float) {
                if (to == T_double) {
                    mv.visitInsn(F2D);
                } else if (to == T_long) {
                    mv.visitInsn(F2L);
                } else {
                    mv.visitInsn(F2I);
                    cast_numeric(T_int, to);
                }
            } else if (from == T_long) {
                if (to == T_double) {
                    mv.visitInsn(L2D);
                } else if (to == T_float) {
                    mv.visitInsn(L2F);
                } else {
                    mv.visitInsn(L2I);
                    cast_numeric(T_int, to);
                }
            } else {
                if (to == T_byte) {
                    mv.visitInsn(I2B);
                } else if (to == T_char) {
                    mv.visitInsn(I2C);
                } else if (to == T_double) {
                    mv.visitInsn(I2D);
                } else if (to == T_float) {
                    mv.visitInsn(I2F);
                } else if (to == T_long) {
                    mv.visitInsn(I2L);
                } else if (to == T_short) {
                    mv.visitInsn(I2S);
                }
            }
        }
    }

    public void push(int i) {
        if (i < -1) {
            mv.visitLdcInsn(new Integer(i));
        } else if (i <= 5) {
            mv.visitInsn(ICONST(i));
        } else if (i <= Byte.MAX_VALUE) {
            mv.visitIntInsn(BIPUSH, i);
        } else if (i <= Short.MAX_VALUE) {
            mv.visitIntInsn(SIPUSH, i);
        } else {
            mv.visitLdcInsn(new Integer(i));
        }
    }

    public void push(long value) {
        if (value == 0L || value == 1L) {
            mv.visitInsn(LCONST(value));
        } else {
            mv.visitLdcInsn(new Long(value));
        }
    }

    public void push(float value) {
        if (value == 0f || value == 1f || value == 2f) {
            mv.visitInsn(FCONST(value));
        } else {
            mv.visitLdcInsn(new Float(value));
        }
    }

    public void push(double value) {
        if (value == 0d || value == 1d) {
            mv.visitInsn(DCONST(value));
        } else {
            mv.visitLdcInsn(new Double(value));
        }
    }

    public void push(String value) {
        mv.visitLdcInsn(value);
    }

    public void push(Class<?> value) {
        push(Type.getType(value));
    }

    public void push(Type value) {
        if (value.isPrimitive()) {
            Type t = value.toReferenceType();
            mv.visitFieldInsn(GETSTATIC, t.getInternalName(), "TYPE", "Ljava/lang/Class;");
        } else {
            mv.visitLdcInsn(value);
        }
    }

    public void push_default(Type type) {
        switch (type.getSort()) {
        case Type.VOID:
            break;
        case Type.BOOLEAN:
        case Type.CHAR:
        case Type.BYTE:
        case Type.SHORT:
        case Type.INT:
            mv.visitInsn(ICONST_0);
            break;
        case Type.FLOAT:
            mv.visitInsn(FCONST_0);
            break;
        case Type.LONG:
            mv.visitInsn(LCONST_0);
            break;
        case Type.DOUBLE:
            mv.visitInsn(DCONST_0);
            break;
        default:
            mv.visitInsn(ACONST_NULL);
            break;
        }
    }

    public void newarray() {
        newarray(T_Object);
    }

    public void newarray(Type type) {
        if (type.isPrimitive()) {
            mv.visitIntInsn(NEWARRAY, NEWARRAY(type));
        } else {
            emit_type(ANEWARRAY, type);
        }
    }

    public void arraylength() {
        mv.visitInsn(ARRAYLENGTH);
    }

    public void load_this() {
        mv.visitVarInsn(ALOAD, 0);
    }

    public void load_local(Type t, int pos) {
        mv.visitVarInsn(t.getOpcode(ILOAD), pos);
    }

    public void store_local(Type t, int pos) {
        mv.visitVarInsn(t.getOpcode(ISTORE), pos);
    }

    public void iinc(int index, int amount) {
        mv.visitIincInsn(index, amount);
    }

    public void return_value(Type type) {
        mv.visitInsn(type.getOpcode(IRETURN));
    }

    public void return_void() {
        mv.visitInsn(RETURN);
    }

    public void super_getfield(Type superType, String name, Type type) {
        emit_field(GETFIELD, superType, name, type);
    }

    public void super_putfield(Type superType, String name, Type type) {
        emit_field(PUTFIELD, superType, name, type);
    }

    public void super_getstatic(Type superType, String name, Type type) {
        emit_field(GETSTATIC, superType, name, type);
    }

    public void super_putstatic(Type superType, String name, Type type) {
        emit_field(PUTSTATIC, superType, name, type);
    }

    public void getfield(Type owner, String name, Type type) {
        emit_field(GETFIELD, owner, name, type);
    }

    public void putfield(Type owner, String name, Type type) {
        emit_field(PUTFIELD, owner, name, type);
    }

    public void getstatic(Type owner, String name, Type type) {
        emit_field(GETSTATIC, owner, name, type);
    }

    public void putstatic(Type owner, String name, Type type) {
        emit_field(PUTSTATIC, owner, name, type);
    }

    // package-protected for EmitUtils, try to fix
    void emit_field(int opcode, Type ctype, String name, Type ftype) {
        mv.visitFieldInsn(opcode, ctype.getInternalName(), name, ftype.getDescriptor());
    }

    public void super_invoke(Type superType, String name, String desc) {
        mv.visitMethodInsn(INVOKESPECIAL, superType.getInternalName(), name, desc, false);
    }

    public void invoke_constructor(Type type) {
        invoke_constructor(type, "()V");
    }

    public void super_invoke_constructor(Type superType) {
        invoke_constructor(superType);
    }

    public void invoke_constructor_this(Type thisType) {
        invoke_constructor(thisType);
    }

    public void invoke_method(Method m) {
        Type type = Type.getType(m.getDeclaringClass());
        if (m.getName().equals(CONSTRUCTOR_NAME)) {
            invoke_constructor(type, Type.getMethodDescriptor(m));
        } else if ((ACC_INTERFACE & m.getDeclaringClass().getModifiers()) != 0) {
            invoke_interface(type, m.getName(), Type.getMethodDescriptor(m));
        } else if ((ACC_STATIC & m.getModifiers()) != 0) {
            invoke_static(type, m.getName(), Type.getMethodDescriptor(m));
        } else {
            invoke_virtual(type, m.getName(), Type.getMethodDescriptor(m));
        }
    }

    public void invoke_interface(Type owner, String name, String desc) {
        mv.visitMethodInsn(INVOKEINTERFACE, owner.getInternalName(), name, desc, true);
    }

    public void invoke_virtual(Type owner, String name, String desc) {
        mv.visitMethodInsn(INVOKEVIRTUAL, owner.getInternalName(), name, desc, false);
    }

    public void invoke_static(Type owner, String name, String desc) {
        mv.visitMethodInsn(INVOKESTATIC, owner.getInternalName(), name, desc, false);
    }

    public void invoke_virtual_this(Type thisType, String name, String desc) {
        invoke_virtual(thisType, name, desc);
    }

    public void invoke_constructor(Type type, String desc) {
        mv.visitMethodInsn(INVOKESPECIAL, type.getInternalName(), CONSTRUCTOR_NAME, desc, false);
    }

    public void invoke_constructor_this(Type thisType, String desc) {
        invoke_constructor(thisType, desc);
    }

    public void super_invoke_constructor(Type thisType, String desc) {
        invoke_constructor(thisType, desc);
    }

    public void new_instance_this(Type thisType) {
        new_instance(thisType);
    }

    public void new_instance(Type type) {
        emit_type(NEW, type);
    }

    private void emit_type(int opcode, Type type) {
        String desc;
        if (type.isArray()) {
            desc = type.getDescriptor();
        } else {
            desc = type.getInternalName();
        }
        mv.visitTypeInsn(opcode, desc);
    }

    public void aaload(int index) {
        push(index);
        aaload();
    }

    public void aaload() {
        mv.visitInsn(AALOAD);
    }

    public void aastore() {
        mv.visitInsn(AASTORE);
    }

    public void athrow() {
        mv.visitInsn(ATHROW);
    }

    public Label make_label() {
        Label l = new Label();
        if (trace) {
            l.trace = new RuntimeException();
            labels.add(l);
        }
        return l;
    }

    public void checkcast_this(Type thisType) {
        checkcast(thisType);
    }

    public void checkcast(Type type) {
        if (!type.equals(T_Object)) {
            emit_type(CHECKCAST, type);
        }
    }

    public void instance_of(Type type) {
        emit_type(INSTANCEOF, type);
    }

    public void instance_of_this(Type thisType) {
        instance_of(thisType);
    }

    public void process_switch(int[] keys, Case callback) {
        float density;
        if (keys.length == 0) {
            density = 0;
        } else {
            density = (float) keys.length / (keys[keys.length - 1] - keys[0] + 1);
        }
        process_switch(keys, callback, density >= 0.5f);
    }

    public void process_switch(int[] keys, Case callback, boolean table) {
        if (!isSorted(keys)) {
            throw new IllegalArgumentException("keys to switch must be sorted ascending");
        }
        Label def = make_label();
        Label end = make_label();

        if (keys.length > 0) {
            int len = keys.length;
            int min = keys[0];
            int max = keys[len - 1];
            int range = max - min + 1;

            if (table) {
                Label[] labels = new Label[range];
                Arrays.fill(labels, def);
                for (int i = 0; i < len; i++) {
                    labels[keys[i] - min] = make_label();
                }
                mv.visitTableSwitchInsn(min, max, def, labels);
                for (int i = 0; i < range; i++) {
                    Label label = labels[i];
                    if (label != def) {
                        mark(label);
                        callback.processCase(this, i + min, end);
                    }
                }
            } else {
                Label[] labels = new Label[len];
                for (int i = 0; i < len; i++) {
                    labels[i] = make_label();
                }
                mv.visitLookupSwitchInsn(def, keys, labels);
                for (int i = 0; i < len; i++) {
                    mark(labels[i]);
                    callback.processCase(this, keys[i], end);
                }
            }
        }

        mark(def);
        callback.processDefault(this);
        mark(end);
    }

    static boolean isSorted(int[] keys) {
        for (int i = 1; i < keys.length; i++) {
            if (keys[i] < keys[i - 1])
                return false;
        }
        return true;
    }

    public void mark_local(String name, Type type, Label start, Label end, int index) {
        mv.visitLocalVariable(name, type.getDescriptor(), null, start, end, index);
    }

    public void mark(Label label) {
        if (label.marked) {
            throw new IllegalStateException();
        }
        label.marked = true;

        mv.visitLabel(label);
    }

    public Label mark() {
        Label label = make_label();
        if (label.marked) {
            throw new IllegalStateException();
        }
        label.marked = true;

        mv.visitLabel(label);
        return label;
    }

    public void push(boolean value) {
        push(value ? 1 : 0);
    }

    /**
     * Toggles the integer on the top of the stack from 1 to 0 or vice versa
     */
    public void not() {
        push(1);
        math(XOR, T_int);
    }

    public void throw_exception(Type type, String msg) {
        new_instance(type);
        dup();
        push(msg);
        invoke_constructor(type, Type.getMethodDescriptor(T_void, T_String));
        athrow();
    }

    /**
     * If the argument is a primitive class, replaces the primitive value on the top of the stack with the wrapped
     * (Object) equivalent. For example, char -> Character. If the class is Void, a null is pushed onto the stack
     * instead.
     * 
     * @param type
     *            the class indicating the current type of the top stack value
     */
    public void box(Type type) {
        if (type.isPrimitive()) {
            Type boxed = type.toReferenceType();
            /*-
            new_instance(boxed);
            if (type.getSize() == 2) {
                // Pp -> Ppo -> oPpo -> ooPpo -> ooPp -> o
                dup_x2();
                dup_x2();
                pop();
            } else {
                // p -> po -> opo -> oop -> o
                dup_x1();
                swap();
            }
            invoke_constructor(boxed,
                Type.getMethodDescriptor(T_void, type));
             */
            invoke_static(boxed, "valueOf", Type.getMethodDescriptor(boxed, type));
        }
    }

    /**
     * If the argument is a primitive class, replaces the object on the top of the stack with the unwrapped (primitive)
     * equivalent. For example, Character -> char.
     * 
     * @param type
     *            the class indicating the desired type of the top stack value
     * @return true if the value was unboxed
     */
    public void unbox(Type type) {
        if (type.getSort() < Type.BOOLEAN || type.getSort() > Type.DOUBLE) {
            throw new IllegalArgumentException();
        }
        String sig = null;
        Type t = T_Number;
        switch (type.getSort()) {
        case Type.CHAR:
            t = T_Character;
            sig = "charValue";
            break;
        case Type.BOOLEAN:
            t = T_Boolean;
            sig = "booleanValue";
            break;
        case Type.DOUBLE:
            sig = "doubleValue";
            break;
        case Type.FLOAT:
            sig = "floatValue";
            break;
        case Type.LONG:
            sig = "longValue";
            break;
        case Type.INT:
        case Type.SHORT:
        case Type.BYTE:
            sig = "intValue";
        }

        invoke_virtual(t, sig, Type.getMethodDescriptor(type));
    }

    /**
     * Unboxes the object on the top of the stack. If the object is null, the unboxed primitive value becomes zero.
     */
    public void unbox_or_zero(Type type) {
        if (type.isPrimitive()) {
            if (type != T_void) {
                Label nonNull = make_label();
                Label end = make_label();
                dup();
                ifnonnull(nonNull);
                pop();
                zero_or_null(type);
                jump(end);
                mark(nonNull);
                unbox(type);
                mark(end);
            }
        } else {
            checkcast(type);
        }
    }

    /**
     * Pushes a zero onto the stack if the argument is a primitive class, or a null otherwise.
     */
    public void zero_or_null(Type type) {
        if (type.isPrimitive()) {
            switch (type.getSort()) {
            case Type.DOUBLE:
                push(0d);
                break;
            case Type.LONG:
                push(0L);
                break;
            case Type.FLOAT:
                push(0f);
                break;
            case Type.VOID:
                aconst_null();
            default:
                push(0);
            }
        } else {
            aconst_null();
        }
    }

    static int ICONST(int value) {
        switch (value) {
        case -1:
            return ICONST_M1;
        case 0:
            return ICONST_0;
        case 1:
            return ICONST_1;
        case 2:
            return ICONST_2;
        case 3:
            return ICONST_3;
        case 4:
            return ICONST_4;
        case 5:
            return ICONST_5;
        }
        return -1; // error
    }

    static int LCONST(long value) {
        if (value == 0L) {
            return LCONST_0;
        } else if (value == 1L) {
            return LCONST_1;
        } else {
            return -1; // error
        }
    }

    static int FCONST(float value) {
        if (value == 0f) {
            return FCONST_0;
        } else if (value == 1f) {
            return FCONST_1;
        } else if (value == 2f) {
            return FCONST_2;
        } else {
            return -1; // error
        }
    }

    static int DCONST(double value) {
        if (value == 0d) {
            return DCONST_0;
        } else if (value == 1d) {
            return DCONST_1;
        } else {
            return -1; // error
        }
    }

    static int NEWARRAY(Type type) {
        switch (type.getSort()) {
        case Type.BYTE:
            return T_BYTE;
        case Type.CHAR:
            return T_CHAR;
        case Type.DOUBLE:
            return T_DOUBLE;
        case Type.FLOAT:
            return T_FLOAT;
        case Type.INT:
            return T_INT;
        case Type.LONG:
            return T_LONG;
        case Type.SHORT:
            return T_SHORT;
        case Type.BOOLEAN:
            return T_BOOLEAN;
        default:
            return -1; // error
        }
    }
}
