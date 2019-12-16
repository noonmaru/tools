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

package com.github.noonmaru.tools.reflection;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

public final class ReflectionUtils
{
	public static Field findPrivateField(Class<?> target, String fieldName)
	{
		try
		{
			Field field = target.getDeclaredField(fieldName);
			field.setAccessible(true);
			
			return field;
		}
		catch (Exception e)
		{
			throw new AssertionError(e);
		}
	}
	
	public static Set<Class<?>> getSuperClasses(Class<?> c)
	{
		HashSet<Class<?>> classes = new HashSet<>();
		
		do
		{
			classes.add(c);
			getSuperIntefaces(classes, c);
			c = c.getSuperclass();
		}
		while (c != null);
		
		return classes;
	}
	
	private static void getSuperIntefaces(HashSet<Class<?>> classes, Class<?> c)
	{
		Class<?>[] interfaces = c.getInterfaces();
		
		for (Class<?> inter : interfaces)
		{
			if (classes.add(inter))
				getSuperIntefaces(classes, inter);
		}
	}
	
	private ReflectionUtils() {}
}
