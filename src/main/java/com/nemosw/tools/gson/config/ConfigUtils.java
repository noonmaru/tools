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

package com.nemosw.tools.gson.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.nemosw.tools.gson.JsonIO;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;

public final class ConfigUtils
{
	private static final IdentityHashMap<Class<?>, ConfigAdapter<?>> ADAPTERS = new IdentityHashMap<>();
	private static final EnumAdapter ENUM_ADAPTER = new EnumAdapter();
	private static final ListAdapter LIST_ADAPTER = new ListAdapter();

	static
	{
		registerAdapter(Boolean.TYPE, Boolean.class, new BooleanAdapter());
		registerAdapter(Character.TYPE, Character.class, new CharacterAdapter());
		registerAdapter(Byte.TYPE, Byte.class, new ByteAdapter());
		registerAdapter(Short.TYPE, Short.class, new ShortAdapter());
		registerAdapter(Integer.TYPE, Integer.class, new IntegerAdapter());
		registerAdapter(Long.TYPE, Long.class, new LongAdapter());
		registerAdapter(Float.TYPE, Float.class, new FloatAdapter());
		registerAdapter(Double.TYPE, Double.class, new DoubleAdapter());
		registerAdapter(Number.class, new NumberAdapter<>());
		registerAdapter(String.class, new StringAdapter());
	}

	public static void registerAdapter(Class<?> type, ConfigAdapter<?> adapter)
	{
		ADAPTERS.put(type, adapter);
	}

	private static <T> void registerAdapter(Class<T> primitiveType, Class<T> wrapperType, ConfigAdapter<?> adapter)
	{
		registerAdapter(primitiveType, adapter);
		registerAdapter(wrapperType, adapter);
	}

	public static boolean load(Object o, JsonObject json)
	{
		return load(o, getConfigFields(o.getClass(), -1), json);
	}

	public static boolean load(Object o, File file) throws IOException
	{
		JsonObject json = file.exists() ? JsonIO.load(file) : new JsonObject();

		if (load(o, json))
			return true;

		JsonIO.save(json, file);
		return false;
	}

	public static boolean load(Class<?> clazz, JsonObject json)
	{
		return load(null, getConfigFields(clazz, Modifier.STATIC), json);
	}

	public static boolean load(Class<?> clazz, File file) throws IOException
	{
		JsonObject json = file.exists() ? JsonIO.load(file) : new JsonObject();

		if (load(clazz, json))
			return true;

		JsonIO.save(json, file);
		return false;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static boolean load(Object o, List<Field> fields, JsonObject json)
	{
		boolean result = true;

		for (Field field : fields)
		{
			Config c = field.getAnnotation(Config.class);

			String name = c.name();

			if (name.isEmpty())
				name = field.getName();

			Class<?> fieldType = field.getType();

			ConfigAdapter configAdapter;

			if (Enum.class.isAssignableFrom(fieldType))
			{
				configAdapter = ENUM_ADAPTER;
				ENUM_ADAPTER.enumType = (Class<? extends Enum>) fieldType;
			}
			else if (List.class == fieldType)
			{
				configAdapter = LIST_ADAPTER;
				
				LIST_ADAPTER.genericType = (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
			}
			else
			{
				configAdapter = getAdapter(fieldType);

				if (configAdapter == null)
					throw new NullPointerException("Unsupported type " + fieldType.getName());
			}

			try
			{
				if (json.has(name))
				{
					Object value = configAdapter.fromJson(json.get(name));

					if (value != null)
					{
						field.set(o, value);
						continue;
					}
				}

				Object value = field.get(o);

				if (value != null && !(field.getAnnotation(Config.class).skippable() && configAdapter.isNullValue(value)))
				{
					json.add(name, configAdapter.toJson(value));
					result = false;
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}

		return result;
	}

	private static List<Field> getConfigFields(Class<?> clazz, int modifierFilter)
	{
		ArrayList<Class<?>> supers = new ArrayList<>();

		do
			supers.add(clazz);
		while ((clazz = clazz.getSuperclass()) != null);

		EnumMap<ConfigPriority, ArrayList<Field>> fields = new EnumMap<>(ConfigPriority.class);
		int count = 0;

		for (int i = supers.size() - 1; i >= 0; i--)
			for (Field field : supers.get(i).getDeclaredFields())
			{
				Config config = field.getAnnotation(Config.class);

				if (config != null && (modifierFilter == -1 || (field.getModifiers() & modifierFilter) != 0))
				{
					ConfigPriority priority = config.priority();
					ArrayList<Field> list = fields.get(priority);

					if (list == null)
						fields.put(priority, list = new ArrayList<>());

					field.setAccessible(true);
					list.add(field);
					count++;
				}
			}

		ArrayList<Field> list = new ArrayList<>(count);

		for (ArrayList<Field> arrayList : fields.values())
			list.addAll(arrayList);

		return list;
	}

	static ConfigAdapter<?> getAdapter(Class<?> clazz)
	{
		ConfigAdapter<?> configAdapter = ADAPTERS.get(clazz);

		if (configAdapter == null)
		{
			Adapter adapter = clazz.getAnnotation(Adapter.class);

			if (adapter != null)
				try
				{
					registerAdapter(clazz, configAdapter = adapter.value().asSubclass(ConfigAdapter.class).newInstance());
				}
				catch (ClassCastException | InstantiationException | IllegalAccessException e)
				{
					e.printStackTrace();
				}
		}

		return configAdapter;
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static class EnumAdapter extends ConfigAdapter<Enum>
	{
		Class<? extends Enum> enumType;

		@Override
		public Enum fromJson(JsonElement json)
		{
			String value = json.getAsString();
			Enum<?> en = null;

			if (value != null)
				try
				{
					en = Enum.valueOf(this.enumType, value);
				}
				catch (IllegalArgumentException e)
				{}

			return en;
		}

		@Override
		public JsonElement toJson(Enum o)
		{
			return new JsonPrimitive(o.name());
		}
	}

	private static class BooleanAdapter extends ConfigAdapter<Boolean>
	{
		@Override
		public Boolean fromJson(JsonElement json)
		{
			return json.getAsBoolean();
		}

		@Override
		public JsonElement toJson(Boolean o)
		{
			return new JsonPrimitive(o);
		}

		@Override
		public boolean isNullValue(Boolean value)
		{
			return !value;
		}
	}

	private static class CharacterAdapter extends ConfigAdapter<Character>
	{
		@Override
		public Character fromJson(JsonElement json)
		{
			return json.getAsCharacter();
		}

		@Override
		public JsonElement toJson(Character o)
		{
			return new JsonPrimitive(o);
		}
	}

	private static class NumberAdapter<T extends Number> extends ConfigAdapter<T>
	{
		@Override
		public final T fromJson(JsonElement json)
		{
			return convert(json.getAsNumber());
		}

		@SuppressWarnings("unchecked")
		public T convert(Number number)
		{
			return (T) number;
		}

		@Override
		public final JsonElement toJson(T o)
		{
			return new JsonPrimitive(o);
		}
	}

	private static class ByteAdapter extends NumberAdapter<Byte>
	{
		@Override
		public Byte convert(Number number)
		{
			return number.byteValue();
		}

		@Override
		public boolean isNullValue(Byte value)
		{
			return 0 == value.byteValue();
		}
	}

	private static class ShortAdapter extends NumberAdapter<Short>
	{
		@Override
		public Short convert(Number number)
		{
			return number.shortValue();
		}

		@Override
		public boolean isNullValue(Short value)
		{
			return 0 == value.shortValue();
		}
	}

	private static class IntegerAdapter extends NumberAdapter<Integer>
	{
		@Override
		public Integer convert(Number number)
		{
			return number.intValue();
		}

		@Override
		public boolean isNullValue(Integer value)
		{
			return 0 == value;
		}
	}

	private static class LongAdapter extends NumberAdapter<Long>
	{
		@Override
		public Long convert(Number number)
		{
			return number.longValue();
		}

		@Override
		public boolean isNullValue(Long value)
		{
			return 0L == value;
		}
	}

	private static class FloatAdapter extends NumberAdapter<Float>
	{
		@Override
		public Float convert(Number number)
		{
			return number.floatValue();
		}

		@Override
		public boolean isNullValue(Float value)
		{
			return 0.0F == value.floatValue();
		}
	}

	private static class DoubleAdapter extends NumberAdapter<Double>
	{
		@Override
		public Double convert(Number number)
		{
			return number.doubleValue();
		}

		@Override
		public boolean isNullValue(Double value)
		{
			return 0.0D == value.doubleValue();
		}
	}

	private static class StringAdapter extends ConfigAdapter<String>
	{
		@Override
		public String fromJson(JsonElement json)
		{
			return json.getAsString();
		}

		@Override
		public JsonElement toJson(String o)
		{
			return new JsonPrimitive(o);
		}

		@Override
		public boolean isNullValue(String value)
		{
			return value.isEmpty();
		}
	}

	private static class ListAdapter extends ConfigAdapter<List<?>>
	{
		Class<?> genericType;

		ListAdapter()
		{}

		private ListAdapter(Class<?> genericType)
		{
			this.genericType = genericType;
		}

		@SuppressWarnings({"unchecked", "rawtypes"})
		@Override
		public List<?> fromJson(JsonElement json)
		{
			Class<?> genericType = this.genericType;
			ConfigAdapter<?> configAdapter;
			JsonArray array = json.getAsJsonArray();
			int size = array.size();
			List list = new ArrayList<>(size);

			if (Enum.class.isAssignableFrom(genericType))
			{
				configAdapter = ENUM_ADAPTER;
				ENUM_ADAPTER.enumType = (Class<? extends Enum>) genericType;
			}
			else if (List.class == genericType)
				configAdapter = new ListAdapter((Class<?>) ((ParameterizedType) genericType.getGenericSuperclass()).getActualTypeArguments()[0]);
			else
			{
				configAdapter = getAdapter(genericType);

				if (configAdapter == null)
					throw new NullPointerException("Unsupported type " + genericType.getName());
			}

			for (int i = 0; i < size; i++)
				list.add(configAdapter.fromJson(array.get(i)));

			return list;
		}

		@SuppressWarnings({"unchecked", "rawtypes"})
		@Override
		public JsonElement toJson(List<?> o)
		{
			int size = o.size();
			JsonArray array = new JsonArray();
			Class<?> genericType = this.genericType;
			ConfigAdapter configAdapter;

			if (Enum.class.isAssignableFrom(genericType))
			{
				configAdapter = ENUM_ADAPTER;
				ENUM_ADAPTER.enumType = (Class<? extends Enum>) genericType;
			}
			else if (List.class == genericType)
				configAdapter = new ListAdapter((Class<?>) ((ParameterizedType) genericType.getGenericSuperclass()).getActualTypeArguments()[0]);
			else
			{
				configAdapter = getAdapter(genericType);

				if (configAdapter == null)
					throw new NullPointerException("Unsupported type " + genericType.getName());
			}

			for (int i = 0; i < size; i++)
				array.add(configAdapter.toJson(o.get(i)));

			return array;
		}

		@Override
		public boolean isNullValue(List<?> value)
		{
			return value.isEmpty();
		}
	}
}
