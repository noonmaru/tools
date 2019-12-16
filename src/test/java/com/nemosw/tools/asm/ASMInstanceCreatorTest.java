package com.nemosw.tools.asm;

import org.junit.Test;

import static org.junit.Assert.*;

public class ASMInstanceCreatorTest
{

    @Test
    public void create()
    {
        ASMInstanceCreator.create(ASMInstanceCreatorTest.class).get();
    }
}