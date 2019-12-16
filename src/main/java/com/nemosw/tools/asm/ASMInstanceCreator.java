/*
 * Copyright (c) 2019 Noonmaru
 *
 * Licensed under the General Public License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/gpl-2.0.php
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.nemosw.tools.asm;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import static org.objectweb.asm.Opcodes.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.function.Supplier;


public final class ASMInstanceCreator
{
	private static final String[] SUPPLIER_DESC = new String[]{Type.getInternalName(Supplier.class)};
	private static final String GET_DESC = Type.getMethodDescriptor(Supplier.class.getMethods()[0]);
	private static int ids;

	private static final HashMap<Class<?>, Supplier<?>> CACHE = new HashMap<>();

	@SuppressWarnings("unchecked")
	public synchronized static <T> Supplier<T> create(Class<T> clazz)
	{
		Supplier<T> creator = (Supplier<T>) CACHE.get(clazz);
		
		if (creator != null)
			return creator;

		int mod = clazz.getModifiers();

		if (!Modifier.isPublic(mod))
			throw new IllegalArgumentException("Not a public class: " + clazz);

		if (Modifier.isAbstract(mod))
			throw new IllegalArgumentException("Abstract class: " + clazz);

		String name = generateClassName(clazz);

		try
		{
			byte[] classData = generateClass(name, clazz);
			creator = (Supplier<T>) ClassDefiner.defineClass(name, classData, clazz.getClassLoader()).newInstance();
			CACHE.put(clazz, creator);
		}
		catch (InstantiationException | IllegalAccessException e)
		{
			e.printStackTrace();
		}

		return creator;
	}

	private static <T> byte[] generateClass(String name, Class<T> clazz)
	{
		Constructor<T> con;

		try
		{
			con = clazz.getConstructor();
		}
		catch (NoSuchMethodException e)
		{
			throw new IllegalArgumentException("No constructor", e);
		}
		catch (SecurityException e)
		{
			throw new IllegalArgumentException("Security Error", e);
		}

		int conMod = con.getModifiers();

		if (!Modifier.isPublic(conMod))
			throw new IllegalArgumentException("No public constructor: " + clazz);

		String desc = name.replace('.', '/');
		String instType = Type.getInternalName(clazz);

		ClassWriter cw = new ClassWriter(0);

		cw.visit(V1_6, ACC_PUBLIC | ACC_SUPER, desc, "Ljava/lang/Object;L " + SUPPLIER_DESC[0] + "<L" + instType + ";>;", "java/lang/Object", SUPPLIER_DESC);
		cw.visitSource(".dynamic", null);

		{
			MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
			mv.visitCode();
			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
			mv.visitInsn(RETURN);
			mv.visitMaxs(1, 1);
			mv.visitEnd();
		}

		{
			MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "get", GET_DESC, null, null);
			mv.visitCode();
			mv.visitTypeInsn(NEW, instType);
			mv.visitInsn(DUP);
			mv.visitMethodInsn(INVOKESPECIAL, instType, "<init>", "()V", false);
			mv.visitInsn(ARETURN);
			mv.visitMaxs(2, 1);
			mv.visitEnd();
		}

		return cw.toByteArray();
	}

	private static String generateClassName(Class<?> clazz)
	{
		return String.format("%s_%d_%s", Supplier.class, ids++, clazz.getSimpleName());
	}

	private ASMInstanceCreator()
	{}
}
