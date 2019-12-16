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

package com.github.noonmaru.tools.gson.config;

import com.github.noonmaru.tools.gson.JsonIO;
import com.google.gson.*;

import java.io.*;
import java.util.*;

public final class JsonConfiguration
{
	public static JsonConfiguration load(String s)
	{
		return new JsonConfiguration((JsonObject) new JsonParser().parse(new StringReader(s)));
	}
	
	public static JsonConfiguration load(File file) throws IOException
	{
		return new JsonConfiguration(JsonIO.load(file));
	}
	
	public static JsonConfiguration load(Reader reader)
    {
		return new JsonConfiguration(JsonIO.load(reader));
	}

	@SuppressWarnings("rawtypes")
	private static JsonElement toJson(Object o)
	{
		if (o instanceof Boolean)
			return new JsonPrimitive((Boolean) o);
		if (o instanceof Number)
			return new JsonPrimitive((Number) o);
		if (o instanceof Character)
			return new JsonPrimitive((Character) o);
		if (o instanceof String)
			return new JsonPrimitive((String) o);
		if (o instanceof List)
		{
			List list = (List) o;
			JsonArray array = new JsonArray();

			for (Object obj : list)
				array.add(toJson(obj));

			return array;
		}
		if (o instanceof JsonConfiguration)
			return ((JsonConfiguration) o).json;

		throw new IllegalArgumentException("Unsupport type " + o.getClass());
	}

	private final JsonObject json;
	private JsonConfiguration parent;
	private HashMap<String, JsonConfiguration> children;

	public JsonConfiguration()
	{
		this.json = new JsonObject();
	}

	public JsonConfiguration(JsonObject json)
	{
		this.json = json;
	}

	private JsonConfiguration(JsonObject json, JsonConfiguration parent)
	{
		this.json = json;
		this.parent = parent;
	}

	private void addChild(String name, JsonConfiguration child)
	{
		if (this.children == null)
			this.children = new HashMap<>();

		this.children.put(name, child);
	}

	public JsonConfiguration createConfig(String name)
	{
		JsonObject json = new JsonObject();
		JsonConfiguration config = new JsonConfiguration(json, this);

		this.json.add(name, json);
		addChild(name, config);

		return config;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private Object fromJson(JsonElement element)
	{
		if (element.isJsonObject())
			return new JsonConfiguration(element.getAsJsonObject(), this);
		else if (element.isJsonArray())
		{
			JsonArray array = element.getAsJsonArray();
			int size = array.size();
			ArrayList list = new ArrayList(size);

			for (int i = 0; i < size; i++)
				list.add(fromJson(array.get(i)));

			return list;
		}
		else if (element.isJsonPrimitive())
		{
			JsonPrimitive primitive = element.getAsJsonPrimitive();

			if (primitive.isBoolean())
				return primitive.getAsBoolean();
			else if (primitive.isNumber())
				return primitive.getAsNumber();
			else
				return primitive.getAsString();
		}

		return null;
	}

	@SuppressWarnings("unchecked")
	public <T> T get(String name)
	{
		if (this.children != null && this.children.size() > 0)
		{
			JsonConfiguration config = this.children.get(name);

			if (config != null)
				return (T) config;
		}

		JsonElement value = this.json.get(name);

		if (value == null)
			return null;

		if (value.isJsonObject())
		{
			JsonObject json = value.getAsJsonObject();
			JsonConfiguration config = new JsonConfiguration(json, this);
			addChild(name, config);

			return (T) config;
		}

		return (T) fromJson(value);
	}

	public Boolean getBoolean(String name)
	{
		JsonPrimitive value = getPrimitive(name);

		return value == null ? null : value.getAsBoolean();
	}

	public byte getByte(String name)
	{
		Number number = getNumber(name);

		return number == null ? 0 : number.byteValue();
	}

	public char getChar(String name)
	{
		Character value = getCharacter(name);

		return value == null ? 0 : value.charValue();
	}

	public Character getCharacter(String name)
	{
		JsonPrimitive value = getPrimitive(name);

		return value == null ? null : value.getAsCharacter();
	}

	public JsonConfiguration getConfig(String name)
	{
		JsonConfiguration config = this.children == null || this.children.isEmpty() ? null : this.children.get(name);

		if (config == null)
		{
			JsonElement value = this.json.get(name);

			if (value != null && value.isJsonObject())
			{
				JsonObject json = value.getAsJsonObject();
				config = new JsonConfiguration(json, this);
				addChild(name, config);
			}
		}

		return config;
	}

	public double getDouble(String name)
	{
		Number number = getNumber(name);

		return number == null ? 0D : number.doubleValue();
	}

	public float getFloat(String name)
	{
		Number number = getNumber(name);

		return number == null ? 0F : number.floatValue();
	}

	public int getInt(String name)
	{
		Number number = getNumber(name);

		return number == null ? 0 : number.intValue();
	}

	@SuppressWarnings("unchecked")
	public <T> List<T> getList(String name)
	{
		JsonElement value = this.json.get(name);

		if (value == null)
			return null;

		JsonArray array = value.getAsJsonArray();
		int size = array.size();
		ArrayList<T> list = new ArrayList<>(size);

		for (int i = 0; i < size; i++)
			list.add((T) fromJson(array.get(i)));

		return list;
	}

	public long getLong(String name)
	{
		Number number = getNumber(name);

		return number == null ? 0L : number.longValue();
	}

	public Number getNumber(String name)
	{
		JsonPrimitive value = getPrimitive(name);

		return value == null ? null : value.getAsNumber();
	}

	public JsonConfiguration getParent()
	{
		return this.parent;
	}

	private JsonPrimitive getPrimitive(String name)
	{
		JsonElement value = this.json.get(name);

		return value != null && value.isJsonPrimitive() ? value.getAsJsonPrimitive() : null;
	}

	public short getShort(String name)
	{
		Number number = getNumber(name);

		return number == null ? 0 : number.shortValue();
	}

	public String getString(String name)
	{
		JsonPrimitive value = getPrimitive(name);

		return value == null ? null : value.getAsString();
	}

	public List<String> getStringList(String name)
	{
		JsonElement value = this.json.get(name);

		if (value == null)
			return null;

		JsonArray array = value.getAsJsonArray();
		int size = array.size();
		ArrayList<String> list = new ArrayList<>(size);

		for (int i = 0; i < size; i++)
			list.add(array.get(i).getAsString());

		return list;
	}

	public boolean has(String name)
	{
		return this.json.has(name);
	}

	public void save(File file) throws IOException
	{
		JsonIO.save(this.json, file);
	}

	public void save(Writer writer)
    {
		JsonIO.save(this.json, writer);
	}

	private void set(String name, JsonElement value)
	{
		if (this.children != null)
		{
			JsonConfiguration child = this.children.remove(name);

			if (child != null)
				child.parent = null;
		}

		this.json.add(name, value);
	}

	@SuppressWarnings("rawtypes")
	public void set(String name, Object value)
	{
		if (value instanceof Boolean)
			setBoolean(name, (Boolean) value);
		else if (value instanceof Number)
			setNumber(name, (Number) value);
		else if (value instanceof Character)
			setCharacter(name, (Character) value);
		else if (value instanceof String)
			setString(name, (String) value);
		else if (value instanceof List)
			setList(name, (List) value);
		else if (value instanceof JsonConfiguration)
			setConfig(name, (JsonConfiguration) value);
	}

	public void setBoolean(String name, Boolean value)
	{
		set(name, new JsonPrimitive(value));
	}

	public void setCharacter(String name, Character value)
	{
		set(name, new JsonPrimitive(value));
	}

	public void setConfig(String name, JsonConfiguration config)
	{
		JsonConfiguration configParent = config.parent;

		if (configParent != null)
			throw new IllegalArgumentException("Config already has parent");

		JsonConfiguration parent = this.parent;

		while (parent != null)
		{
			if (parent == configParent)
				throw new IllegalArgumentException("Config cannot be ancestor");

			parent = parent.parent;
		}

		this.json.add(name, config.json);
		addChild(name, config);
	}

	public void setList(String name, List<?> value)
	{
		JsonArray array = new JsonArray();

		for (Object o : value)
			array.add(toJson(o));

		set(name, array);
	}

	public void setNumber(String name, Number value)
	{
		set(name, new JsonPrimitive(value));
	}

	public void setNumberList(String name, List<? extends Number> value)
	{
		JsonArray array = new JsonArray();

		for (Number number : value)
			array.add(new JsonPrimitive(number));

		set(name, array);
	}

	public void setString(String name, String value)
	{
		set(name, new JsonPrimitive(value));
	}

	public void setStringList(String name, List<String> value)
	{
		JsonArray array = new JsonArray();

		for (String string : value)
			array.add(new JsonPrimitive(string));

		set(name, array);
	}

	public JsonObject getJson()
	{
		return this.json;
	}

	public Map<String, Object> toRaw()
	{
		LinkedHashMap<String, Object> map = new LinkedHashMap<>();

		for (Map.Entry<String, JsonElement> entry : this.json.entrySet())
			map.put(entry.getKey(), fromJson(entry.getValue()));

		return map;
	}

	@Override
	public String toString()
	{
		return JsonIO.save(this.json);
	}
}
