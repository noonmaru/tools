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

import java.util.IdentityHashMap;

public final class ClassDefiner
{
	
	private static final IdentityHashMap<ClassLoader, ASMClassLoader> CACHE = new IdentityHashMap<>();

	static
	{
		CACHE.put(null, new ASMClassLoader());
	}

	public synchronized static Class<?> defineClass(String name, byte[] data, ClassLoader loader)
	{
		ASMClassLoader asmLoader = CACHE.get(loader);

		if (asmLoader == null)
			CACHE.put(loader, asmLoader = new ASMClassLoader(loader));
		
		return asmLoader.defineClass(name, data);
	}

	private static class ASMClassLoader extends ClassLoader
	{
		private ASMClassLoader()
		{}

		private ASMClassLoader(ClassLoader classLoader)
		{
			super(classLoader);
		}

		public Class<?> defineClass(String name, byte[] data)
		{
			return defineClass(name, data, 0, data.length);
		}
	}
	
	private ClassDefiner() {}
	
}
