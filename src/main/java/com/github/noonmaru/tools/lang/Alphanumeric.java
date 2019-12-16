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

package com.github.noonmaru.tools.lang;

public final class Alphanumeric
{
    public static int compare(String o1, String o2)
    {
        int thisMarker = 0;
        int thatMarker = 0;
        int s1Length = o1.length();
        int s2Length = o2.length();

        while (thisMarker < s1Length && thatMarker < s2Length)
        {
            String thisChunk = getChunk(o1, s1Length, thisMarker);
            thisMarker += thisChunk.length();

            String thatChunk = getChunk(o2, s2Length, thatMarker);
            thatMarker += thatChunk.length();

            int result = 0;
            if (isDigit(thisChunk.charAt(0)) && isDigit(thatChunk.charAt(0)))
            {
                int thisChunkLength = thisChunk.length();
                result = thisChunkLength - thatChunk.length();
                if (result == 0)
                    for (int i = 0; i < thisChunkLength; i++)
                    {
                        result = thisChunk.charAt(i) - thatChunk.charAt(i);
                        if (result != 0)
                            return result;
                    }
            }
            else
                result = thisChunk.compareTo(thatChunk);

            if (result != 0)
                return result;
        }

        return s1Length - s2Length;
    }

    private static String getChunk(String s, int slength, int marker)
    {
        StringBuilder chunk = new StringBuilder();
        char c = s.charAt(marker);
        chunk.append(c);
        marker++;
        if (isDigit(c))
            while (marker < slength)
            {
                c = s.charAt(marker);
                if (!isDigit(c))
                    break;
                chunk.append(c);
                marker++;
            }
        else
            while (marker < slength)
            {
                c = s.charAt(marker);
                if (isDigit(c))
                    break;
                chunk.append(c);
                marker++;
            }
        return chunk.toString();
    }

    private static boolean isDigit(char ch)
    {
        return ch >= 48 && ch <= 57;
    }

    private Alphanumeric() {}
}
