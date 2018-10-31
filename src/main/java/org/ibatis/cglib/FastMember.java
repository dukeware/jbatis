package org.ibatis.cglib;

import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

public abstract class FastMember {
    static final Class<?>[] Empty = new Class<?>[0];
    protected Member member;

    protected FastMember(Member member) {
        this.member = member;
    }

    public Class<?>[] getParameterTypes() {
        if (member instanceof Constructor<?>) {
            return ((Constructor<?>) member).getParameterTypes();
        } else if (member instanceof Method) {
            return ((Method) member).getParameterTypes();
        }
        return Empty;
    }

    public Class<?>[] getExceptionTypes() {
        if (member instanceof Constructor<?>) {
            return ((Constructor<?>) member).getExceptionTypes();
        } else if (member instanceof Method) {
            return ((Method) member).getExceptionTypes();
        }

        return Empty;
    }

    public String getName() {
        return member.getName();
    }

    public Class<?> getDeclaringClass() {
        return member.getDeclaringClass();
    }

    public int getModifiers() {
        return member.getModifiers();
    }

    public String toString() {
        return member.toString();
    }

    public int hashCode() {
        return member.hashCode();
    }

    public boolean equals(Object o) {
        if (o == null || !(o instanceof FastMember)) {
            return false;
        }
        return member.equals(((FastMember) o).member);
    }
}
