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

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


public final class JsonMacro
{
    private static final ScriptEngine JAVA_SCRIPT_ENGINE = new ScriptEngineManager().getEngineByName("JavaScript");

    public static Object eval(String script)
    {
        try
        {
            return JAVA_SCRIPT_ENGINE.eval(script);
        }
        catch (ScriptException e)
        {
            e.printStackTrace();
        }

        return null;
    }

    private static DecimalFormat formatter;

    private static DecimalFormat formatter()
    {
        if (formatter == null)
            formatter = new DecimalFormat("#.##");

        return formatter;
    }

    public static String process(String s, JsonConfiguration config)
    {
        StringBuilder builder = new StringBuilder(s);

        int index = 0;
        int length = 0;

        while (index < (length = builder.length()))
        {
            if ('[' == builder.charAt(index++))
            {
                int macroStart = index - 1;

                find:
                while (index < length)
                {
                    char c = builder.charAt(index++);

                    if (']' == c)
                    {
                        String macro = builder.substring(macroStart + 1, index - 1);

                        if (macro.isEmpty())
                            continue;

                        int lastIndex = macro.lastIndexOf('/');
                        JsonConfiguration current = config;
                        String name;

                        if (lastIndex >= 0)
                        {
                            name = macro.substring(lastIndex + 1);

                            if (name.isEmpty())
                                continue;

                            String[] pathes = macro.substring(0, lastIndex).split("/");

                            for (String path : pathes)
                            {
                                if (path.isEmpty())
                                    continue;

                                if ("..".equals(path))
                                {
                                    JsonConfiguration parent = current.getParent();

                                    if (parent != null)
                                        current = parent;
                                }
                                else
                                {
                                    JsonConfiguration sub = current.getConfig(path);

                                    if (sub == null)
                                        continue find;

                                    current = sub;
                                }
                            }
                        }
                        else
                        {
                            name = macro;
                        }

                        Object value = current.get(name);

                        if (value != null)
                        {
                            String replace = value.toString();
                            builder.replace(macroStart, index, replace);
                            index = macroStart + replace.length();
                        }

                        break;
                    }
                }
            }
        }

        index = 0;
        find:
        while ((index = builder.indexOf("EVAL(", index)) > -1)
        {
            length = builder.length();
            int depth = 0;

            for (int i = index + 6; i < length; i++)
            {
                char c = builder.charAt(i);

                if (c == '(')
                    depth++;
                else if (c == ')')
                {
                    if (depth-- == 0)
                    {
                        String script = builder.substring(index + 5, i);
                        String result = String.valueOf(eval(script));

                        builder.replace(index, i + 1, result);
                        index += result.length();
                        continue find;
                    }
                }
            }

            break;
        }

        index = 0;
        find:
        while ((index = builder.indexOf("FORMAT(", index)) > -1)
        {
            length = builder.length();
            int depth = 0;

            for (int i = index + 8; i < length; i++)
            {
                char c = builder.charAt(i);

                if (c == '(')
                    depth++;
                else if (c == ')')
                {
                    if (depth-- == 0)
                    {
                        String value = builder.substring(index + 7, i);
                        String[] split = value.split(",");

                        DecimalFormat format = split.length == 2 ? new DecimalFormat(split[1].trim()) : formatter();

                        value = format.format(Double.parseDouble(split[0]));

                        builder.replace(index, i + 1, value);
                        index += value.length();
                        continue find;
                    }
                }
            }

            break;
        }

        return builder.toString();
    }

    public static List<String> processAll(Collection<String> src, JsonConfiguration config)
    {
        ArrayList<String> list = new ArrayList<>(src.size());

        for (String s : src)
            list.add(process(s, config));

        return list;
    }

    private JsonMacro()
    {}
}
